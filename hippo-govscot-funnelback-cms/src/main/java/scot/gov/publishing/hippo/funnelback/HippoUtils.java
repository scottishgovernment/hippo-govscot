package scot.gov.publishing.hippo.funnelback;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class HippoUtils {

    public static Node findPublished(Node handle) throws RepositoryException {
        NodeIterator it = handle.getNodes(handle.getName());
        while (it.hasNext()) {
            Node variant = it.nextNode();
            if (isPublished(variant)) {
                return variant;
            }
        }
        return null;
    }

    static boolean isPublished(Node node) throws RepositoryException {
        return "published".equals(node.getProperty("hippostd:state").getString());
    }
}
