package scot.gov.publishing.searchjournal;

import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;

import javax.jcr.Session;

/**
 * Factory for per-execution journal resources, registered via {@code HippoServiceRegistry}.
 * Both the {@link JournalConsumer} (content indexing) and the {@link JournalPositionSource}
 * (position tracking) are created fresh for each job run so they can share an underlying
 * connection when needed, but remain independently swappable.
 */
public interface JournalConsumerSource {

    /**
     * Creates a new consumer for the current job execution.
     * Returns {@code null} if the consumer cannot be created (e.g. credentials not configured),
     * in which case the reconciliation loop will skip the run.
     */
    JournalConsumer newConsumer(RepositoryJobExecutionContext context, Session session);

    /**
     * Creates the {@link JournalPositionSource} for the current job execution.
     * Returns {@code null} if position tracking is unavailable, in which case the loop will skip the run.
     */
    JournalPositionSource newPositionSource(RepositoryJobExecutionContext context, Session session);

}
