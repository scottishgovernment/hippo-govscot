package scot.gov.publishing.jcr;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Batches JCR {@link Session#save()} calls to avoid overwhelming the repository.
 *
 * <p>Callers invoke {@link #save()} after each individual change. The session is only flushed
 * to the repository once every {@code saveLimit} calls (default {@value DEFAULT_SAVE_LIMIT}).
 * An optional inter-batch delay can be configured to yield CPU and lock time to other sessions
 * between flushes — useful when Jackrabbit indexes synchronously and a large bulk operation
 * would otherwise starve other writers.
 *
 * <p>Use {@link #forceSave()} at the end of a bulk operation to flush any remaining pending
 * changes that have not yet reached the batch threshold.
 */
public class SessionSaver {

    private static final Logger LOG = LoggerFactory.getLogger(SessionSaver.class);

    private static final int DEFAULT_SAVE_LIMIT = 50;

    private final Session session;

    private int saveLimit = DEFAULT_SAVE_LIMIT;

    private long delayMs = 0;

    private int count = 0;

    public SessionSaver(Session session) {
        this(session, DEFAULT_SAVE_LIMIT);
    }

    /**
     * @param session    the JCR session to flush
     * @param saveLimit  number of {@link #save()} calls between actual flushes
     */
    public SessionSaver(Session session, int saveLimit) {
        this(session, saveLimit, 0L);
    }

    /**
     * @param session    the JCR session to flush
     * @param saveLimit  number of {@link #save()} calls between actual flushes
     * @param delayMs    milliseconds to sleep after each flush; {@code 0} disables the delay
     */
    public SessionSaver(Session session, int saveLimit, long delayMs) {
        this.session = session;
        this.saveLimit = saveLimit;
        this.delayMs = delayMs;
    }

    /**
     * Records one pending change and flushes the session if the batch threshold has been reached.
     *
     * @return {@code true} if the session was actually saved, {@code false} if the change was
     *         only buffered
     */
    public boolean save() throws RepositoryException {
        if (count >= saveLimit) {
            forceSave();
            return true;
        }
        count++;
        return false;
    }

    /**
     * Unconditionally flushes all pending changes to the repository and resets the batch counter.
     * Should be called at the end of a bulk operation to commit any remainder below the threshold.
     */
    public void forceSave() throws RepositoryException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            session.save();
        } catch (RepositoryException e) {
            LOG.error("Failed to save session", e);
            throw e;
        }
        stopWatch.stop();
        LOG.info("Saving session done. Took {}", stopWatch.getTime());
        count = 0;
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
