package scot.gov.publishing.journal;

/**
 * Abstraction over a search engine (or any other consumer) that can accept
 * journal entries processed by the reconciliation loop.
 *
 * <p>Implementations decide how to fetch content (e.g. HTML vs JSON) and what has to be done with the content — this is
 * not imposed by the interface.
 */
public interface JournalConsumer {

    /**
     * Process {@code entry}, publishing or removing the content it describes from the
     * appropriate index depending on {@link JournalEntry#getAction()}.
     */
    void consume(JournalEntry entry) throws JournalConsumerException;

    void close();

}
