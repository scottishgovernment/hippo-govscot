package scot.gov.publishing.journal;

public class JournalConsumerException extends Exception {

    public JournalConsumerException(String message, Exception e) {
        super(message, e);
    }
}
