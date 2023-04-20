package scot.gov.publishing.searchjounal;


import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class SessionSaver {

    private static final Logger LOG = LoggerFactory.getLogger(SessionSaver.class);

    private static final int DEFAULT_SAVE_LIMIT = 50;

    private final Session session;

    private int saveLimit = DEFAULT_SAVE_LIMIT;

    private int count = 0;

    public SessionSaver(Session session) {
        this(session, DEFAULT_SAVE_LIMIT);
    }

    public SessionSaver(Session session, int saveLimit) {
        this.session = session;
        this.saveLimit = saveLimit;
    }

    public  void save() throws RepositoryException {
        if (count >= saveLimit) {
            forceSave();
        }
        count++;
    }

    public void forceSave() throws RepositoryException {
        StopWatch stopWatch = new StopWatch();
        LOG.info("Saving session.");
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
    }
}