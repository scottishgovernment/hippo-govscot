package scot.gov.publishing.hippo.funnelback.component;

import org.hippoecm.hst.core.component.HstRequest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ValueListUtil {

    private ValueListUtil() {
        // hide public constructor
    }

    /**
     * return a map version of a value list.  Returns an empty map of the value list does not exist.
     */
    public static Map<String, String> toMap(HstRequest request, String path) throws RepositoryException {
        Session session = request.getRequestContext().getSession();
        if (!session.nodeExists("/content/documents/govscot/valuelists/publicationTypes/publicationTypes")) {
            return Collections.emptyMap();
        }

        Node valueListNode = session.getNode(path);
        NodeIterator it = valueListNode.getNodes();
        Map<String, String> map = new HashMap<>();
        while (it.hasNext()) {
            Node next = it.nextNode();
            String key = next.getProperty("selection:key").getString();
            String value = next.getProperty("selection:label").getString();
            map.put(key, value);
        }
        return map;
    }
}