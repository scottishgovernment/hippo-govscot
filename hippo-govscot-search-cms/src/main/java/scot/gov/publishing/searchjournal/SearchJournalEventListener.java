package scot.gov.publishing.searchjournal;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.modules.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.jcr.FeatureFlag;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;
import java.util.List;

/**
 * Listen to workflow events and record relevant ones in the search journal.
 * All site-specific logic (which events matter, what URLs to use) is delegated
 * to a {@link JournalEntrySource} registered via {@link HippoServiceRegistry}.
 */
public class SearchJournalEventListener implements DaemonModule {

    private static final Logger LOG = LoggerFactory.getLogger(SearchJournalEventListener.class);

    private Session session;

    private FeatureFlag featureFlag;

    private SearchJournal searchJournal;

    @Override
    public void initialize(Session session) throws RepositoryException {
        this.session = session;
        featureFlag = new FeatureFlag(session, "SearchJournalEventListener");
        searchJournal = new SearchJournal(session);
        HippoEventListenerRegistry.get().register(this);
        LOG.info("SearchJournalEventListener initialised, enabled={}", featureFlag.isEnabled());
    }

    @Override
    public void shutdown() {
        HippoEventListenerRegistry.get().unregister(this);
    }

    @Subscribe
    public void handleEvent(HippoWorkflowEvent event) {
        if (!featureFlag.isEnabled()) {
            LOG.debug("SearchJournalEventListener is disabled, skipping event interaction={} subject={}",
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
        List<SearchJournalEntry> entries = source.entriesForEvent(event);
        if (entries.isEmpty()) {
            LOG.debug("No journal entries produced for interaction={} subject={}",
                    event.interaction(), event.subjectId());
            return;
        }
        LOG.debug("Recording {} journal {} for interaction={} subject={}",
                entries.size(), entries.size() == 1 ? "entry" : "entries",
                event.interaction(), event.subjectId());
        Calendar timestamp = Calendar.getInstance();
        long sequence = 1;
        for (SearchJournalEntry entry : entries) {
            entry.setAttempt(0);
            entry.setTimestamp(timestamp);
            entry.setContentId(event.subjectId());
            entry.setSequence(sequence++);
            searchJournal.record(entry);
        }
        session.save();
    }
}
