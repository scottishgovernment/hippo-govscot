package scot.gov.publishing.hippo.redirects;

import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.jcr.FeatureFlag;
import scot.gov.publishing.jcr.ScheduledJobUtils;
import scot.gov.publishing.jcr.SessionSaver;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static scot.gov.publishing.hippo.redirects.JcrRedirectRepository.*;

/**
 * Scheduled job that migrates URL alias redirects from the legacy path-mirror JCR layout
 * to the new hash-bucketed layout used by {@link JcrRedirectRepository}.
 *
 * <p>Three source trees are processed, in this order:
 * <ol>
 *   <li>{@code /content/redirects/Aliases} — nodes with a {@code govscot:url} property are
 *       migrated as normal redirects.</li>
 *   <li>{@code /content/redirects/prgloo} — a character trie where the leaf-node path spells
 *       a news-article slug letter by letter (e.g. {@code /n/e/w/s} → slug {@code news}).
 *       Each leaf is migrated as a redirect to the NRS web-archive URL for that slug.
 *       A generated description is added when the source node carries none.</li>
 *   <li>{@code /content/redirects/HistoricalUrls} — every node in the tree is migrated as a
 *       historical URL ({@code govscot:historical = true}).</li>
 * </ol>
 *
 * <p><b>Control flags</b> (stored as booleans under {@code /content/featureflags/}):
 * <ul>
 *   <li>{@code MigrateAliasRedirectsJob} — set to {@code true} to enable / resume the job.
 *       The job self-disables this flag only on <em>full completion</em>, so a paused run is
 *       automatically resumed on the next scheduled firing without any operator action.</li>
 *   <li>{@code MigrateAliasRedirectsJobPaused} — set to {@code true} to pause the job at the
 *       next batch boundary.  The job commits the current partial batch before stopping.
 *       Set back to {@code false} to allow the next run to proceed.</li>
 * </ul>
 *
 * <p><b>Checkpoint / resume</b>: the job records its position after every committed batch as two
 * string properties on the {@code /content/featureflags/} node
 * ({@code MigrateAliasRedirectsJobCheckpointPhase} and
 * {@code MigrateAliasRedirectsJobCheckpointPath}).  On resume, phases that are fully behind
 * the checkpoint are skipped without any JCR traversal; within the checkpoint phase, nodes are
 * skipped by path comparison (no repository read) until the last committed path is found, at
 * which point normal processing resumes.  The checkpoint is cleared on full completion.
 *
 * <p>After full migration the job enables the {@code JcrRedirectRepository} feature flag and
 * disables {@code LegacyRedirectsRepository}, switching both reads and writes to the new
 * hash-bucketed structure.  Old tree deletion is handled separately by
 * {@link DeleteOldRedirectsJob}.
 *
 * <p>All saves are batched via {@link SessionSaver} with a throttle delay after each batch to
 * avoid overwhelming the JCR repository.  The batch size ({@value SAVE_BATCH_SIZE}) and delay
 * ({@value SAVE_DELAY_MS} ms) are deliberately conservative: 100-node batches keep per-transaction
 * memory low and reduce lock contention; the 1-second inter-batch pause yields CPU and lock time
 * to other sessions (Jackrabbit 2 indexes synchronously, so no async queue can fall behind).
 */
public class MigrateAliasRedirectsJob implements RepositoryJob {

    private static final Logger LOG = LoggerFactory.getLogger(MigrateAliasRedirectsJob.class);

    static final String ALIASES_ROOT       = "/content/redirects/Aliases";
    static final String HISTORICAL_ROOT    = "/content/redirects/HistoricalUrls";
    static final String PRGLOO_ROOT        = "/content/redirects/prgloo";
    static final String WEBARCHIVE_BASE    = "https://webarchive.nrscotland.gov.uk/300/news.gov.scot/news/";
    static final int    SAVE_BATCH_SIZE    = 100;
    static final long   SAVE_DELAY_MS      = 1_000L;

    /**
     * Feature flag name used to pause the job between batches.
     * Set to {@code true} in {@code /content/featureflags/} to request a pause;
     * set back to {@code false} before the next scheduled run to resume.
     */
    static final String PAUSE_FLAG = "MigrateAliasRedirectsJobPaused";

    static final String PHASE_ALIASES    = "aliases";
    static final String PHASE_PRGLOO     = "prgloo";
    static final String PHASE_HISTORICAL = "historical";

    /** Defines the execution order used for checkpoint phase comparisons. */
    static final List<String> PHASE_ORDER = Arrays.asList(PHASE_ALIASES, PHASE_PRGLOO, PHASE_HISTORICAL);

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {
        Session session = context.createSystemSession();
        try {
            FeatureFlag flag = new FeatureFlag(session, MigrateAliasRedirectsJob.class.getSimpleName());
            FeatureFlag pauseFlag = new FeatureFlag(session, PAUSE_FLAG);
            if (!flag.isEnabled()) {
                LOG.info("MigrateAliasRedirectsJob is not enabled, skipping");
                return;
            }
            if (pauseFlag.isEnabled()) {
                LOG.info("MigrateAliasRedirectsJob is paused, skipping");
                return;
            }

            doExecute(session, flag);
        } finally {
            session.logout();
        }
    }

    void doExecute(Session session, FeatureFlag mainFlag) throws RepositoryException {
        FeatureFlag pauseFlag = new FeatureFlag(session, PAUSE_FLAG);
        SessionSaver saver = new SessionSaver(session, SAVE_BATCH_SIZE, SAVE_DELAY_MS);
        JcrRedirectRepository repo = new JcrRedirectRepository(session, saver);
        Stats stats = new Stats();
        Checkpoint checkpoint = Checkpoint.load(session);
        if (checkpoint.hasCheckpoint()) {
            stats.migrated = checkpoint.totalMigrated;
            LOG.info("MigrateAliasRedirectsJob: resuming from checkpoint phase='{}', lastPath='{}', totalMigrated={}",
                    checkpoint.phase, checkpoint.lastPath, checkpoint.totalMigrated);
        }
        try {
            migrateAliases(session, repo, stats, pauseFlag, checkpoint);
            migratePrglooRedirects(session, repo, stats, pauseFlag, checkpoint);
            migrateHistoricalUrls(session, repo, stats, pauseFlag, checkpoint);
        } catch (MigrationPausedException e) {
            saver.forceSave();
            LOG.info("MigrateAliasRedirectsJob: paused by operator (flag '{}') after migrated={}, skipped={}; "
                    + "job remains enabled and will try again on next scheduled run",
                    PAUSE_FLAG, stats.migrated, stats.skipped, e);
            return;
        }
        saver.forceSave();
        LOG.info("MigrateAliasRedirectsJob: migration complete: migrated={}, skipped={}", stats.migrated, stats.skipped);
        completeMigration(session, mainFlag, checkpoint);
    }

    private void migrateAliases(Session session, JcrRedirectRepository repo,
                                Stats stats, FeatureFlag pauseFlag, Checkpoint checkpoint)
            throws RepositoryException {
        if (checkpoint.isPhaseComplete(PHASE_ALIASES)) {
            LOG.info("MigrateAliasRedirectsJob: aliases phase already complete per checkpoint, skipping");
            return;
        }
        if (!session.nodeExists(ALIASES_ROOT)) {
            LOG.info("MigrateAliasRedirectsJob: no legacy aliases root at {}, nothing to migrate", ALIASES_ROOT);
            return;
        }
        LOG.info("MigrateAliasRedirectsJob: starting alias migration from {}", ALIASES_ROOT);
        boolean[] skipping = { checkpoint.hasCheckpoint() && PHASE_ALIASES.equals(checkpoint.phase) };
        if (skipping[0]) {
            LOG.info("MigrateAliasRedirectsJob: fast-forwarding aliases to checkpoint path '{}'", checkpoint.lastPath);
        }
        Node root = session.getNode(ALIASES_ROOT);
        walkAliases(root, ALIASES_ROOT, session, repo, stats, pauseFlag, checkpoint, skipping);
        if (skipping[0]) {
            LOG.warn("MigrateAliasRedirectsJob: aliases checkpoint path '{}' was not found during traversal; the checkpoint may be stale", checkpoint.lastPath);
        }
        LOG.info("MigrateAliasRedirectsJob: alias migration complete, migrated={}", stats.migrated);
    }

    private void walkAliases(Node node, String rootPath, Session session, JcrRedirectRepository repo,
                             Stats stats, FeatureFlag pauseFlag, Checkpoint checkpoint, boolean[] skipping)
            throws RepositoryException {

        if (node.hasProperty(PROP_URL)) {
            String fromPath = node.getPath().substring(rootPath.length());
            processAlias(fromPath, node, session, repo, stats, pauseFlag, checkpoint, skipping);
        }

        NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            walkAliases(children.nextNode(), rootPath, session, repo, stats, pauseFlag, checkpoint, skipping);
        }
    }

    private void processAlias(String fromPath, Node node, Session session, JcrRedirectRepository repo,
                              Stats stats, FeatureFlag pauseFlag, Checkpoint checkpoint, boolean[] skipping)
            throws RepositoryException {

        if (skipping[0]) {
            stats.scannedToCheckpoint++;
            if (fromPath.equals(checkpoint.lastPath)) {
                skipping[0] = false;
                LOG.info("MigrateAliasRedirectsJob: aliases checkpoint path '{}' found after scanning {} items, resuming normal processing",
                        checkpoint.lastPath, stats.scannedToCheckpoint);
            } else if (stats.scannedToCheckpoint % 5_000 == 0) {
                LOG.info("MigrateAliasRedirectsJob: aliases fast-forward progress: scanned={}, target='{}'",
                        stats.scannedToCheckpoint, checkpoint.lastPath);
            }
            // already committed — skip without a nodeExists check
            return;
        }

        String redirectNodePath = RedirectNodePath.path("govscot", fromPath);
        if (session.nodeExists(redirectNodePath)) {
            LOG.info("skipping {} node already exists {}", fromPath, redirectNodePath);
            stats.skipped++;
            return;
        }

        Redirect redirect = new Redirect();
        redirect.setFrom(fromPath);
        redirect.setTo(node.getProperty(PROP_URL).getString());
        if (node.hasProperty(PROP_DESCRIPTION)) {
            redirect.setDescription(node.getProperty(PROP_DESCRIPTION).getString());
        }
        repo.doSave(redirect);
        stats.migrated++;
        checkpoint.update(PHASE_ALIASES, fromPath, stats.migrated);
        logProgress(stats);
        checkPauseFlag(pauseFlag, stats);
    }

    private void migratePrglooRedirects(Session session, JcrRedirectRepository repo,
                                        Stats stats, FeatureFlag pauseFlag, Checkpoint checkpoint)
            throws RepositoryException {
        if (checkpoint.isPhaseComplete(PHASE_PRGLOO)) {
            LOG.info("MigrateAliasRedirectsJob: prgloo phase already complete per checkpoint, skipping");
            return;
        }
        if (!session.nodeExists(PRGLOO_ROOT)) {
            LOG.info("MigrateAliasRedirectsJob: no prgloo tree at {}, nothing to migrate", PRGLOO_ROOT);
            return;
        }
        LOG.info("MigrateAliasRedirectsJob: starting prgloo migration from {}", PRGLOO_ROOT);
        boolean[] skipping = { checkpoint.hasCheckpoint() && PHASE_PRGLOO.equals(checkpoint.phase) };
        if (skipping[0]) {
            LOG.info("MigrateAliasRedirectsJob: fast-forwarding prgloo to checkpoint path '{}'", checkpoint.lastPath);
        }
        walkPrgloo(session.getNode(PRGLOO_ROOT), "", session, repo, stats, pauseFlag, checkpoint, skipping);
        if (skipping[0]) {
            LOG.warn("MigrateAliasRedirectsJob: prgloo checkpoint path '{}' was not found during traversal; "
                    + "the checkpoint may be stale", checkpoint.lastPath);
        }
        LOG.info("MigrateAliasRedirectsJob: prgloo migration complete, migrated={}", stats.migrated);
    }

    /**
     * Walks the prgloo character trie, building the slug by appending each node name.
     * Leaf nodes (no children) represent a complete slug and are migrated as redirects.
     */
    private void walkPrgloo(Node node, String slug, Session session, JcrRedirectRepository repo,
                            Stats stats, FeatureFlag pauseFlag, Checkpoint checkpoint, boolean[] skipping)
            throws RepositoryException {
        List<Node> children = new ArrayList<>();
        NodeIterator it = node.getNodes();
        while (it.hasNext()) {
            children.add(it.nextNode());
        }

        if (children.isEmpty()) {
            migratePrglooSlug(slug, node, session, repo, stats, pauseFlag, checkpoint, skipping);
        } else {
            for (Node child : children) {
                walkPrgloo(child, slug + child.getName(), session, repo, stats, pauseFlag, checkpoint, skipping);
            }
        }
    }

    private void migratePrglooSlug(String slug, Node sourceNode, Session session, JcrRedirectRepository repo,
                                   Stats stats, FeatureFlag pauseFlag, Checkpoint checkpoint, boolean[] skipping)
            throws RepositoryException {
        String fromPath = "/news/" + slug;

        if (skipping[0]) {
            stats.scannedToCheckpoint++;
            if (fromPath.equals(checkpoint.lastPath)) {
                skipping[0] = false;
                LOG.info("MigrateAliasRedirectsJob: prgloo checkpoint path '{}' found after scanning {} items, resuming normal processing",
                        checkpoint.lastPath, stats.scannedToCheckpoint);
            } else if (stats.scannedToCheckpoint % 5_000 == 0) {
                LOG.info("MigrateAliasRedirectsJob: prgloo fast-forward progress: scanned={}, target='{}'",
                        stats.scannedToCheckpoint, checkpoint.lastPath);
            }
            return;
        }

        if (session.nodeExists(RedirectNodePath.path(GOVSCOT, fromPath))) {
            stats.skipped++;
            return;
        }
        String description = sourceNode.hasProperty(PROP_DESCRIPTION)
                ? sourceNode.getProperty(PROP_DESCRIPTION).getString()
                : "Migrated from prgloo news archive (news.gov.scot/news/" + slug + ")";
        Redirect redirect = new Redirect();
        redirect.setFrom(fromPath);
        redirect.setTo(WEBARCHIVE_BASE + slug);
        redirect.setDescription(description);
        repo.doSave(redirect);
        stats.migrated++;
        checkpoint.update(PHASE_PRGLOO, fromPath, stats.migrated);
        logProgress(stats);
        checkPauseFlag(pauseFlag, stats);
    }

    private void migrateHistoricalUrls(Session session, JcrRedirectRepository repo,
                                       Stats stats, FeatureFlag pauseFlag, Checkpoint checkpoint)
            throws RepositoryException {
        if (checkpoint.isPhaseComplete(PHASE_HISTORICAL)) {
            LOG.info("MigrateAliasRedirectsJob: historical phase already complete per checkpoint, skipping");
            return;
        }
        if (!session.nodeExists(HISTORICAL_ROOT)) {
            LOG.info("MigrateAliasRedirectsJob: no historical URLs root at {}, nothing to migrate", HISTORICAL_ROOT);
            return;
        }
        LOG.info("MigrateAliasRedirectsJob: starting historical URL migration from {}", HISTORICAL_ROOT);
        boolean[] skipping = { checkpoint.hasCheckpoint() && PHASE_HISTORICAL.equals(checkpoint.phase) };
        if (skipping[0]) {
            LOG.info("MigrateAliasRedirectsJob: fast-forwarding historical to checkpoint path '{}'", checkpoint.lastPath);
        }
        walkHistoricalUrls(session.getNode(HISTORICAL_ROOT), HISTORICAL_ROOT, session, repo, stats, pauseFlag, checkpoint, skipping);
        if (skipping[0]) {
            LOG.warn("MigrateAliasRedirectsJob: historical checkpoint path '{}' was not found during traversal; "
                    + "the checkpoint may be stale", checkpoint.lastPath);
        }
        LOG.info("MigrateAliasRedirectsJob: historical URL migration complete, migrated={}", stats.migrated);
    }

    private void walkHistoricalUrls(Node node, String rootPath, Session session, JcrRedirectRepository repo,
                                    Stats stats, FeatureFlag pauseFlag, Checkpoint checkpoint, boolean[] skipping)
            throws RepositoryException {
        String fromPath = node.getPath().substring(rootPath.length());

        if (!fromPath.isEmpty()) {
            if (skipping[0]) {
                stats.scannedToCheckpoint++;
                if (fromPath.equals(checkpoint.lastPath)) {
                    skipping[0] = false;
                    LOG.info("MigrateAliasRedirectsJob: historical checkpoint path '{}' found after scanning {} items, resuming normal processing",
                            checkpoint.lastPath, stats.scannedToCheckpoint);
                } else if (stats.scannedToCheckpoint % 5_000 == 0) {
                    LOG.info("MigrateAliasRedirectsJob: historical fast-forward progress: scanned={}, target='{}'",
                            stats.scannedToCheckpoint, checkpoint.lastPath);
                }
                // already committed — skip without a nodeExists check
            } else {
                String redirectNodePath = RedirectNodePath.path("govscot", fromPath);
                if (session.nodeExists(redirectNodePath)) {
                    LOG.info("skipping {} node already exists {}", fromPath, redirectNodePath);
                    stats.skipped++;
                } else {
                    Redirect redirect = new Redirect();
                    redirect.setFrom(fromPath);
                    redirect.setHistoricalUrl(true);
                    repo.doSave(redirect);
                    stats.migrated++;
                    checkpoint.update(PHASE_HISTORICAL, fromPath, stats.migrated);
                    logProgress(stats);
                    checkPauseFlag(pauseFlag, stats);
                }
            }
        }

        NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            walkHistoricalUrls(children.nextNode(), rootPath, session, repo, stats, pauseFlag, checkpoint, skipping);
        }
    }

    /**
     * Checks the pause flag every {@value SAVE_BATCH_SIZE} migrated records (i.e. at the same
     * cadence as a batch JCR save) and throws {@link MigrationPausedException} if it is set.
     * The caller is responsible for force-saving any pending batch before propagating the
     * exception.
     */
    private void checkPauseFlag(FeatureFlag pauseFlag, Stats stats) {
        if (stats.migrated > 0 && stats.migrated % SAVE_BATCH_SIZE == 0 && pauseFlag.isEnabled()) {
            throw new MigrationPausedException();
        }
    }

    private void completeMigration(Session session, FeatureFlag mainFlag, Checkpoint checkpoint)
            throws RepositoryException {
        mainFlag.setEnabled(false);
        checkpoint.clear();
        new FeatureFlag(session, JcrRedirectRepository.class.getSimpleName()).setEnabled(true);
        new FeatureFlag(session, LegacyRedirectsRepository.class.getSimpleName()).setEnabled(false);
        unscheduleJob(session);
        enableDeleteOldRedirectsJob(session);
        session.save();
        LOG.info("MigrateAliasRedirectsJob: switched to new hash-bucketed redirect structure "
                + "(JcrRedirectRepository=true, LegacyRedirectsRepository=false, MigrateAliasRedirectsJob=false); "
                + "DeleteOldRedirectsJob scheduler enabled");
    }

    /**
     * Enables the scheduler job node and all its triggers for {@link DeleteOldRedirectsJob}
     * so that the deletion job begins firing on its configured schedule.  The job's own
     * feature flag is intentionally left off; an operator must enable it when ready to
     * allow actual deletions to proceed.
     */
    private void enableDeleteOldRedirectsJob(Session session) throws RepositoryException {
        ScheduledJobUtils.enableJob(session, DeleteOldRedirectsJob.class.getSimpleName());
    }

    /**
     * Disables all triggers on the scheduler node for this job so it never fires again.
     * The job is a one-shot migration; once complete it does not need to run a second time.
     */
    private void unscheduleJob(Session session) throws RepositoryException {
        ScheduledJobUtils.unscheduleJob(session, MigrateAliasRedirectsJob.class.getSimpleName());
    }

    private void logProgress(Stats stats) {
        if (stats.migrated % 500 == 0) {
            LOG.info("MigrateAliasRedirectsJob: migrated={}, skipped={}", stats.migrated, stats.skipped);
        }
    }

    private static class Stats {
        int migrated            = 0;
        int skipped             = 0;
        int scannedToCheckpoint = 0;
    }

    private static class MigrationPausedException extends RuntimeException {
        MigrationPausedException() {
            super("Migration paused by operator flag");
        }
    }

    /**
     * Persists the migration position as two string properties on the
     * {@code /content/featureflags/} node, committed atomically with each batch save.
     *
     * <p>The {@code phase} property names the current processing phase
     * ({@value MigrateAliasRedirectsJob#PHASE_ALIASES},
     *  {@value MigrateAliasRedirectsJob#PHASE_PRGLOO}, or
     *  {@value MigrateAliasRedirectsJob#PHASE_HISTORICAL}).
     * The {@code lastPath} property is the {@code fromPath} of the last item written in that
     * batch.  Both are absent when no migration has started or after full completion.
     */
    static class Checkpoint {

        static final String PROP_PHASE    = "MigrateAliasRedirectsJobCheckpointPhase";
        static final String PROP_PATH     = "MigrateAliasRedirectsJobCheckpointPath";
        static final String PROP_MIGRATED = "MigrateAliasRedirectsJobCheckpointMigrated";

        private final Node flagsNode;
        String phase         = "";
        String lastPath      = "";
        int    totalMigrated = 0;

        private Checkpoint(Node flagsNode) throws RepositoryException {
            this.flagsNode = flagsNode;
            if (flagsNode.hasProperty(PROP_PHASE)) {
                phase = flagsNode.getProperty(PROP_PHASE).getString();
            }
            if (flagsNode.hasProperty(PROP_PATH)) {
                lastPath = flagsNode.getProperty(PROP_PATH).getString();
            }
            if (flagsNode.hasProperty(PROP_MIGRATED)) {
                totalMigrated = (int) flagsNode.getProperty(PROP_MIGRATED).getLong();
            }
        }

        static Checkpoint load(Session session) throws RepositoryException {
            return new Checkpoint(session.getNode("/content/featureflags/"));
        }

        boolean hasCheckpoint() {
            return !phase.isEmpty();
        }

        /**
         * Returns {@code true} if {@code queryPhase} came entirely before the checkpoint phase,
         * meaning it was fully completed in a previous run.
         */
        boolean isPhaseComplete(String queryPhase) {
            if (!hasCheckpoint()) {
                return false;
            }
            return PHASE_ORDER.indexOf(queryPhase) < PHASE_ORDER.indexOf(phase);
        }

        /**
         * Updates the in-memory checkpoint and sets the pending JCR property change.
         * The change is committed to disk on the next {@link Session#save()} call, which
         * happens inside {@link SessionSaver} at every {@value MigrateAliasRedirectsJob#SAVE_BATCH_SIZE}
         * writes — so the committed checkpoint always reflects the last fully-saved batch.
         */
        void update(String newPhase, String newPath, int newTotalMigrated) throws RepositoryException {
            flagsNode.setProperty(PROP_PHASE, newPhase);
            flagsNode.setProperty(PROP_PATH, newPath);
            flagsNode.setProperty(PROP_MIGRATED, (long) newTotalMigrated);
            this.phase         = newPhase;
            this.lastPath      = newPath;
            this.totalMigrated = newTotalMigrated;
        }

        /** Removes the checkpoint properties; called on full migration completion. */
        void clear() throws RepositoryException {
            if (flagsNode.hasProperty(PROP_PHASE)) {
                flagsNode.getProperty(PROP_PHASE).remove();
            }
            if (flagsNode.hasProperty(PROP_PATH)) {
                flagsNode.getProperty(PROP_PATH).remove();
            }
            if (flagsNode.hasProperty(PROP_MIGRATED)) {
                flagsNode.getProperty(PROP_MIGRATED).remove();
            }
        }
    }
}
