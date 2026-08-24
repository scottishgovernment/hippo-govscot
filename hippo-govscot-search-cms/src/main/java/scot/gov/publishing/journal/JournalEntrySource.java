package scot.gov.publishing.journal;

import org.onehippo.repository.events.HippoWorkflowEvent;

import javax.jcr.RepositoryException;
import java.util.List;

/**
 * Determines which journal entries should be recorded for a given workflow event.
 * Implementations are responsible for all site-specific logic: which events are relevant,
 * which content types are indexed, which collection they belong to, and what URL they are
 * reachable at.  Return an empty list to indicate that an event should not be recorded.
 *
 * One event might result in several jounral entries, for example gov publications or publishing guides, manuals.
 */
public interface JournalEntrySource {

    List<JournalEntry> entriesForEvent(HippoWorkflowEvent event) throws RepositoryException;
}
