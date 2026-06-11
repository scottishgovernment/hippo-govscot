package scot.gov.publishing.searchjournal.funnelback;

import scot.gov.publishing.searchjournal.JournalConsumerException;
import scot.gov.publishing.searchjournal.JournalPositionSource;

/**
 * {@link JournalPositionSource} that stores the journal position as a push document
 * in Funnelback's push API.
 */
public class FunnelbackJournalPosition implements JournalPositionSource {

    private final Funnelback funnelback;

    public FunnelbackJournalPosition(Funnelback funnelback) {
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
