package scot.gov.publishing.jcr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class ScheduledJobUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobUtils.class);

    private static final String SCHEDULER_BASE =
            "/hippo:configuration/hippo:modules/scheduler/hippo:moduleconfig/system/";

    private ScheduledJobUtils() {
        // hide constructor
    }

    /**
     * Disables all triggers and the job node itself for the named scheduler job so it never
     * fires again.
     *
     * @param session  a live JCR session (caller is responsible for saving)
     * @param jobName  the simple name of the job as it appears under the scheduler base path
     */
    public static void unscheduleJob(Session session, String jobName) throws RepositoryException {
        setJobEnabled(session, jobName, false);
    }

    /**
     * Enables all triggers and the job node itself for the named scheduler job.
     *
     * @param session  a live JCR session (caller is responsible for saving)
     * @param jobName  the simple name of the job as it appears under the scheduler base path
     */
    public static void enableJob(Session session, String jobName) throws RepositoryException {
        setJobEnabled(session, jobName, true);
    }

    private static void setJobEnabled(Session session, String jobName, boolean enabled) throws RepositoryException {
        String jobPath = SCHEDULER_BASE + jobName;
        if (!session.nodeExists(jobPath)) {
            LOG.warn("ScheduledJobUtils: scheduler node not found at '{}'; cannot set enabled={}", jobPath, enabled);
            return;
        }
        Node jobNode = session.getNode(jobPath);
        jobNode.setProperty("hipposched:enabled", enabled);
        if (jobNode.hasNode("hipposched:triggers")) {
            NodeIterator triggers = jobNode.getNode("hipposched:triggers").getNodes();
            while (triggers.hasNext()) {
                Node trigger = triggers.nextNode();
                trigger.setProperty("hipposched:enabled", enabled);
                LOG.info("ScheduledJobUtils: set trigger '{}' enabled={} on job '{}'", trigger.getName(), enabled, jobName);
            }
        }
        LOG.info("ScheduledJobUtils: job '{}' set enabled={}", jobName, enabled);
    }
}
