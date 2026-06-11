package scot.gov.publishing.searchjournal;

import scot.gov.publishing.searchjournal.funnelback.JournalPosition;

/**
 * Abstraction over where the journal position is stored and retrieved.
 * Decoupled from content indexing so that the position can be persisted
 * independently of the search engine being used.
 */
public interface JournalPositionSource {

    /**
     * Returns the position the reconciliation loop should resume from,
     * or {@code null} if no position has been stored yet.
     */
    JournalPosition getJournalPosition() throws JournalConsumerException;

    /**
     * Persists the given position so the next run can resume from here.
     */
    void storeJournalPosition(JournalPosition position) throws JournalConsumerException;

}
