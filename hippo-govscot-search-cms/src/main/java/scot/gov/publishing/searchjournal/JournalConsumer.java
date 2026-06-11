package scot.gov.publishing.searchjournal;

/**
 * Abstraction over a search engine (or any other consumer) that can accept
 * journal entries processed by the reconciliation loop.
 *
 * <p>Implementations decide how to fetch content (e.g. HTML vs JSON) and what has to be done with the content — this is
 * not imposed by the interface.
 */
public interface JournalConsumer {

    /**
     * Returns {@code true} if the consumer is ready to process entries.
     * The reconciliation loop calls this before starting a run and skips if not ready.
     */
    boolean isReady();

    /**
     * Publish the content described by {@code entry} into the appropriate index.
     */
    void publish(SearchJournalEntry entry) throws JournalConsumerException;

    /**
     * Remove the content described by {@code entry} from the appropriate index.
     */
    void depublish(SearchJournalEntry entry) throws JournalConsumerException;

    void close();

}
