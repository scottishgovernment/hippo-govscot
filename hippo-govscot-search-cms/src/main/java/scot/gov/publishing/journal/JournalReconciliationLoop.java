package scot.gov.publishing.journal;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.commons.lang3.time.StopWatch;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.jcr.FeatureFlag;
import scot.gov.publishing.journal.funnelback.FunnelbackMetricRegistry;
import scot.gov.publishing.journal.funnelback.JournalPosition;
import scot.gov.publishing.journal.funnelback.MetricName;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Reconciliation loop that reads pending entries from the search journal and
 * forwards them to a {@link JournalConsumer} (e.g. Funnelback).
 *
 * <p>The consumer implementation is looked up at runtime via {@code HippoServiceRegistry},
 * so different sites or search engines can plug in their own implementation
 * by registering a {@link JournalConsumerFactory}.
 *
 * <p>The following job attributes are optional:
 * <ul>
 *   <li>{@code maxJournalEntriesToFetch} - maximum entries per run (default 2000)</li>
 *   <li>{@code interRequestPause} - milliseconds to pause between requests (default 100)</li>
 * </ul>
 */
public class JournalReconciliationLoop implements RepositoryJob {

    private static final Logger LOG = LoggerFactory.getLogger(JournalReconciliationLoop.class);

    private static final String BATCH_SIZE_ATTRIBUTE = "maxJournalEntriesToFetch";

    private static final String REQUEST_PAUSE_ATTRIBUTE = "interRequestPause";

    // length of time to pause between requests
    private long interRequestPause = 100;

    // the maximum number of journal entries to fetch each time the job runs
    private int maxJournalEntriesToFetch = 2000;

    private final RetryPolicy retryPolicy = new RetryPolicy();

    private final Counter failureCounter = FunnelbackMetricRegistry.getInstance().counter(MetricName.FAILURES.getName());

    private final Meter failureMeter = FunnelbackMetricRegistry.getInstance().meter(MetricName.FAILURE_RATE.getName());

    private final Timer jobTimer = FunnelbackMetricRegistry.getInstance().timer(MetricName.JOB_TIMES.getName());

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {

        configure(context);

        Session session = context.createSystemSession();
        try {
            FeatureFlag featureFlag = new FeatureFlag(session, "JournalReconciliationLoop");
            if (featureFlag.isEnabled()) {
                doExecute(context, session, featureFlag);
            } else {
                LOG.info("JournalReconciliationLoop is disabled");
            }
        } catch (RepositoryException e) {
            LOG.error("RepositoryException during journal reconciliation", e);
            throw e;
        } finally {
            session.logout();
        }
    }

    void configure(RepositoryJobExecutionContext context) {
        if (context.getAttributeNames().contains(BATCH_SIZE_ATTRIBUTE)) {
            String maxJournalEntriesToFetchString = context.getAttribute(BATCH_SIZE_ATTRIBUTE);
            try {
                maxJournalEntriesToFetch = Integer.parseInt(maxJournalEntriesToFetchString);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid value of {}: \"{}\", defaulting to 2000", BATCH_SIZE_ATTRIBUTE, maxJournalEntriesToFetchString);
            }
        }

        if (context.getAttributeNames().contains(REQUEST_PAUSE_ATTRIBUTE)) {
            String interRequestPauseString = context.getAttribute(REQUEST_PAUSE_ATTRIBUTE);
            try {
                interRequestPause = Long.parseLong(interRequestPauseString);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid value of {}: \"{}\", defaulting to {}", REQUEST_PAUSE_ATTRIBUTE, interRequestPauseString, interRequestPause);
            }
        }
    }

    void doExecute(RepositoryJobExecutionContext context, Session session, FeatureFlag featureFlag) throws RepositoryException {
        JournalConsumerFactory journalConsumerFactory = HippoServiceRegistry.getService(JournalConsumerFactory.class);
        if (journalConsumerFactory == null) {
            LOG.warn("No JournalConsumerFactory registered, skipping");
            return;
        }

        JournalConsumer journalConsumer = null;
        try {
            journalConsumer = journalConsumerFactory.newConsumer(context, session);
            JournalPositionSource positionSource = journalConsumerFactory.newPositionSource(context, session);
            fetchAndProcessPendingJournalEntries(journalConsumer, positionSource, session, featureFlag);
        } catch (JournalConsumerException e) {
            LOG.error("JournalConsumerException during journal reconciliation", e);
        } catch (JournalConsumerFactoryException e) {
            LOG.warn("JournalConsumerFactory could not create a journal consumer or position source, skipping", e);
        } finally {
            if (journalConsumer != null) {
                journalConsumer.close();
            }
        }
    }

    void fetchAndProcessPendingJournalEntries(
            JournalConsumer consumer,
            JournalPositionSource positionSource,
            Session session,
            FeatureFlag featureFlag) throws RepositoryException, JournalConsumerException {

        Journal journal = new Journal(session);
        JournalPosition journalPosition = positionSource.getJournalPosition();
        if (journalPosition == null) {
            LOG.info("No journal position found ... skipping this run.");
            return;
        }

        List<JournalEntry> pendingEntries =
                journal.getPendingEntries(journalPosition.getPosition(), journalPosition.getSequence(), maxJournalEntriesToFetch);
        if (pendingEntries.isEmpty()) {
            LOG.info("No journal entries to process");
            return;
        }

        LOG.info("Pending journal entries: {}  to process, journal position is {}", pendingEntries.size(), journalPosition);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Timer.Context timerContext = jobTimer.time();
        int count = processPendingEntries(pendingEntries, consumer, positionSource, journal, featureFlag);
        timerContext.stop();
        stopWatch.stop();
        LOG.info("Reconciliation loop took {} to process {} journal entries", stopWatch.getTime(), count);
    }

    int processPendingEntries(
            List<JournalEntry> pendingEntries,
            JournalConsumer consumer,
            JournalPositionSource positionSource,
            Journal journal,
            FeatureFlag featureFlag) throws JournalConsumerException {

        Map<String, List<JournalEntry>> pendingEntriesByUrl = new HashMap<>();
        for (JournalEntry entry : pendingEntries) {
            pendingEntriesByUrl.putIfAbsent(entry.getUrl(), new ArrayList<>());
            pendingEntriesByUrl.get(entry.getUrl()).add(entry);
        }

        int count = 0;
        JournalEntry lastEntry = null;
        for (JournalEntry entry : pendingEntries) {

            if (!featureFlag.isEnabled()) {
                LOG.info("Job has been disabled, finishing early");
                break;
            }

            if (moreRecentEntryForUrl(entry, pendingEntriesByUrl)) {
                LOG.info("More recent entries exits for {}, skipping", entry.getUrl());
            } else {
                processEntry(consumer, journal, entry);
                count++;
                periodicSave(positionSource, entry, count);
            }
            lastEntry = entry;
        }
        if (lastEntry != null) {
            JournalPosition position = new JournalPosition();
            position.setPosition(lastEntry.getTimestamp());
            position.setSequence(lastEntry.getSequence());
            positionSource.storeJournalPosition(position);
        }
        return count;
    }

    boolean moreRecentEntryForUrl(JournalEntry entry, Map<String, List<JournalEntry>> pendingEntriesByUrl) {
        List<JournalEntry> entriesForUrl = pendingEntriesByUrl.get(entry.getUrl());
        JournalEntry mostRecentEntry = entriesForUrl.get(entriesForUrl.size() - 1);
        return entriesForUrl.size() > 1 && entry.getTimestamp().before(mostRecentEntry.getTimestamp());
    }

    void processEntry(JournalConsumer consumer, Journal journal, JournalEntry entry) {
        try {
            doProcessEntry(consumer, entry);
            TimeUnit.MILLISECONDS.sleep(interRequestPause);
        } catch (JournalConsumerException e) {
            LOG.error("Failed to process journal entry {}", entry.getUrl(), e);
            handleFailure(entry, journal);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while pausing after {}", entry.getUrl(), e);
        }
    }

    void doProcessEntry(JournalConsumer consumer, JournalEntry entry) throws JournalConsumerException {
        LOG.info("Processing {} {} {} attempt {}", ((GregorianCalendar) entry.getTimestamp()).toZonedDateTime(), entry.getAction(), entry.getUrl(), entry.getAttempt());
        consumer.consume(entry);
    }

    void periodicSave(JournalPositionSource positionSource, JournalEntry entry, int count) throws JournalConsumerException {
        // dictates how often the journal position is saved
        int saveInterval = 100;
        if (count % saveInterval == 0) {
            JournalPosition journalPosition = new JournalPosition();
            journalPosition.setPosition(entry.getTimestamp());
            journalPosition.setSequence(entry.getSequence());
            positionSource.storeJournalPosition(journalPosition);
        }
    }

    void handleFailure(JournalEntry entry, Journal journal) {
        try {
            doHandleFailure(entry, journal);
        } catch (RepositoryException e) {
            LOG.error("RepositoryException trying to create a new journal entry for a failure.", e);
        }
    }

    void doHandleFailure(JournalEntry entry, Journal journal) throws RepositoryException {
        if (!retryPolicy.shouldRetry(entry)) {
            failureCounter.inc();
            failureMeter.mark();
            LOG.error("Entry has reached max attempts: {}, {}", entry.getAction(), entry.getUrl());
            return;
        }

        LOG.warn("Reconciliation failed {} {} {}, attempt {}",
                entry.getAction(), entry.getCollection(), entry.getUrl(), entry.getAttempt());
        long newAttempt = entry.getAttempt() + 1;
        JournalEntry newEntry = new JournalEntry();
        newEntry.setUrl(entry.getUrl());
        newEntry.setAction(entry.getAction());
        newEntry.setCollection(entry.getCollection());
        newEntry.setAttempt(newAttempt);
        newEntry.setTimestamp(getNewTimestamp(entry));
        journal.record(newEntry);
    }

    Calendar getNewTimestamp(JournalEntry entry) {
        long backoff = retryPolicy.getBackoffPeriodInMillis(entry);
        long newTime = System.currentTimeMillis() + backoff;
        Calendar newTimestamp = Calendar.getInstance();
        newTimestamp.setTimeInMillis(newTime);
        return newTimestamp;
    }

}
