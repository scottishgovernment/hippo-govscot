package scot.gov.publishing.journal;

import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;

import javax.jcr.Session;

/**
 * Factory for JournalConsumers & JournalPositionSource, registered via {@code HippoServiceRegistry}.
 *
 * JournalReconciliationLoop creates a fresh JournalConsumer and JournalPositionSource each time it runs.
 */
public interface JournalConsumerFactory {

    /**
     * Creates a new consumer for the current job execution.
     * Either returns a {@link JournalConsumer} that is ready to process entries, or throws a
     * {@link JournalConsumerFactoryException} (e.g. credentials not configured, or the consumer
     * is not ready), in which case the reconciliation loop will skip the run. Implementations must
     * close any consumer they created before throwing.
     */
    JournalConsumer newConsumer(RepositoryJobExecutionContext context, Session session) throws JournalConsumerFactoryException;

    /**
     * Creates the {@link JournalPositionSource} for the current job execution.
     * Either returns a usable {@link JournalPositionSource} or throws a {@link JournalConsumerFactoryException}
     * (e.g. position tracking unavailable), in which case the loop will skip the run.
     */
    JournalPositionSource newPositionSource(RepositoryJobExecutionContext context, Session session) throws JournalConsumerFactoryException;

}
