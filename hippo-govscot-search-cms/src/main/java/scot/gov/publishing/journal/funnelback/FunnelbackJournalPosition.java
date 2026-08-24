package scot.gov.publishing.journal.funnelback;

import scot.gov.publishing.journal.JournalConsumerException;
import scot.gov.publishing.journal.JournalPositionSource;

/**
 * {@link JournalPositionSource} that stores the journal position as a push document
 * in Funnelback's push API.
 */
public class FunnelbackJournalPosition implements JournalPositionSource {

    private final FunnelbackIndexer funnelback;

    public FunnelbackJournalPosition(FunnelbackIndexer funnelback) {
        this.funnelback = funnelback;
    }

    @Override
    public JournalPosition getJournalPosition() throws JournalConsumerException {
        try {
            return funnelback.getJournalPosition();
        } catch (FunnelbackException e) {
            throw new JournalConsumerException("Failed to get journal position", e);
        }
    }

    @Override
    public void storeJournalPosition(JournalPosition position) throws JournalConsumerException {
        try {
            funnelback.storeJournalPosition(position);
        } catch (FunnelbackException e) {
            throw new JournalConsumerException("Failed to store journal position", e);
        }
    }

}
