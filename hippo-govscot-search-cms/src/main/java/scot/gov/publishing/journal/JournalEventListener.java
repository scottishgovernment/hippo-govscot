package scot.gov.publishing.journal;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.modules.ConfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.jcr.FeatureFlag;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;
import java.util.List;

/**
 * Listen to workflow events and record relevant ones in the search journal.
 * All site-specific logic (which events matter, what URLs to use) is delegated
 * to a {@link JournalEntrySource} registered via {@link HippoServiceRegistry}.
 *
 * <p>The {@code site} recorded on each entry is derived from the content path convention
 * {@code /content/documents/<site>/...}, so that deployments serving multiple sites from one
 * repository (e.g. a publishing platform) end up with entries routed to the right site. The
 * {@code rootContentPath} module config property names the content path segment that is this
 * deployment's own primary site (e.g. {@code govscot}); it is recorded as {@code rootSite}
 * instead of the raw path segment (e.g. {@code gov}), or as blank if {@code rootSite} isn't set.
 */
public class JournalEventListener implements ConfigurableDaemonModule {

    private static final Logger LOG = LoggerFactory.getLogger(JournalEventListener.class);

    private static final String CONTENT_ROOT = "/content/documents/";

    private Session session;

    private FeatureFlag featureFlag;

    private Journal searchJournal;

    private String rootContentPath = "";

    private String rootSite = "";

    @Override
    public void configure(Node moduleConfig) throws RepositoryException {
        if (moduleConfig.hasProperty("rootContentPath")) {
            rootContentPath = moduleConfig.getProperty("rootContentPath").getString();
        }
        if (moduleConfig.hasProperty("rootSite")) {
            rootSite = moduleConfig.getProperty("rootSite").getString();
        }
    }

    @Override
    public void initialize(Session session) throws RepositoryException {
        this.session = session;
        featureFlag = new FeatureFlag(session, "JournalEventListener");
        searchJournal = new Journal(session);
        HippoEventListenerRegistry.get().register(this);
        LOG.info("JournalEventListener initialised, enabled={}", featureFlag.isEnabled());
    }

    @Override
    public void shutdown() {
        HippoEventListenerRegistry.get().unregister(this);
    }

    @Subscribe
    public void handleEvent(HippoWorkflowEvent event) {
        if (!featureFlag.isEnabled()) {
            LOG.debug("JournalEventListener is disabled, skipping event interaction={} subject={}",
                    event.interaction(), event.subjectId());
            return;
        }

        LOG.debug("Handling event interaction={} action={} subject={} path={}",
                event.interaction(), event.action(), event.subjectId(), event.subjectPath());

        JournalEntrySource source = HippoServiceRegistry.getService(JournalEntrySource.class);
        if (source == null) {
            LOG.warn("No JournalEntrySource registered, skipping event for {}", event.subjectId());
            return;
        }

        try {
            doHandleEvent(event, source);
        } catch (RepositoryException e) {
            LOG.error("RepositoryException trying to index {}", event.subjectId(), e);
        } catch (RuntimeException e) {
            LOG.error("Exception trying to index {}", event.subjectId(), e);
            throw e;
        }
    }

    void doHandleEvent(HippoWorkflowEvent event, JournalEntrySource source) throws RepositoryException {
        List<JournalEntry> entries = source.entriesForEvent(event);
        if (entries.isEmpty()) {
            LOG.debug("No journal entries produced for interaction={} subject={}",
                    event.interaction(), event.subjectId());
            return;
        }
        LOG.debug("Recording {} journal {} for interaction={} subject={}",
                entries.size(), entries.size() == 1 ? "entry" : "entries",
                event.interaction(), event.subjectId());
        Calendar timestamp = Calendar.getInstance();
        String site = siteFor(event.subjectPath());
        long sequence = 1;
        for (JournalEntry entry : entries) {
            entry.setAttempt(0);
            entry.setTimestamp(timestamp);
            entry.setContentId(event.subjectId());
            entry.setSequence(sequence++);
            entry.setSite(site);
            searchJournal.record(entry);
        }
        session.save();
    }

    /**
     * Derives the site an entry belongs to from the content path convention
     * {@code /content/documents/<site>/...}. Returns {@link #rootSite} for the configured
     * {@link #rootContentPath}, or blank if the path doesn't follow that convention.
     */
    String siteFor(String path) {
        if (path == null || !path.startsWith(CONTENT_ROOT)) {
            return "";
        }
        String remainder = path.substring(CONTENT_ROOT.length());
        String site = StringUtils.substringBefore(remainder, "/");
        return site.equals(rootContentPath) ? rootSite : site;
    }
}
