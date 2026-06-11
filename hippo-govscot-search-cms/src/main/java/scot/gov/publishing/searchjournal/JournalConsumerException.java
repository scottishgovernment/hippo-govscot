package scot.gov.publishing.searchjournal;

public class JournalConsumerException extends Exception {

    public JournalConsumerException(String message, Exception e) {
        super(message, e);
    }
}
