package scot.gov.publishing.searchjournal;

import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Lookup feature flags for event listeners and scheduled tasks.  Featured flags are stored as boolean properties of the
 * node /content/featureflags/
 *
 * For local development the flags can be overridden at the command line
 *
 * If the featured flags node does not exist or the flag in question does not exist then the flag will default to false.
 */
public class FeatureFlag {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureFlag.class);

    private static final String FEATURE_FLAGS_PATH = "/content/featureflags/";

    private final Session session;

    private final String flag;

    public FeatureFlag(Session session, String flag) {
        this.session = session;
        this.flag = flag;
    }

    public void setEnabled(boolean enabled) throws RepositoryException {
        Node node = getFlagsNode();
        node.setProperty(flag, enabled);
    }

    public boolean isEnabled() {
        // allow the flag to be overridden at the command line
        ContainerConfiguration containerConfiguration = HstServices.getComponentManager().getContainerConfiguration();
        if (containerConfiguration.containsKey(flag)) {
            String flagValue = containerConfiguration.getString(flag);
            if ("true".equals(flagValue)) {
                return true;
            }

            if ("false".equals(flagValue)) {
                return false;
            }

            LOG.warn("featureFlag {} appears as a property but does not have a valid value (true | false).  Value is {}", flag, flagValue);
        }

        try {
            Node featureFlags = getFlagsNode();
            if (!featureFlags.hasProperty(flag)) {
                LOG.info("feature flag {} does not exist, defaulting to enabled = false", flag);
                return false;
            }

            return featureFlags.getProperty(flag).getBoolean();
        } catch (RepositoryException e) {
            LOG.warn("unexpected exception getting feature flag {}, defaulting to enabled = false", flag, e);
            return false;
        }
    }

    Node getFlagsNode() throws RepositoryException {
        return session.nodeExists(FEATURE_FLAGS_PATH)
                ? session.getNode(FEATURE_FLAGS_PATH)
                : createFlagsNode();
    }

    Node createFlagsNode() throws RepositoryException {
        return session.getNode("/content").addNode("featureflags", "nt:unstructured");
    }
}
