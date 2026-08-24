package scot.gov.publishing.journal.funnelback;

public interface FunnelbackIndexer {

    void close();

    void publish(String collection, String key, String html) throws FunnelbackException;

    void unpublish(String collection, String key) throws FunnelbackException;

    JournalPosition getJournalPosition() throws FunnelbackException;

    void storeJournalPosition(JournalPosition position) throws FunnelbackException;

}
