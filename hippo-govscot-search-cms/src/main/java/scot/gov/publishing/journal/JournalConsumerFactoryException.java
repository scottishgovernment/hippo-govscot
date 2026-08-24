package scot.gov.publishing.journal;

public class JournalConsumerFactoryException extends Exception {

    public JournalConsumerFactoryException(String message) {
        super(message);
    }

    public JournalConsumerFactoryException(String message, Exception e) {
        super(message, e);
    }
}
