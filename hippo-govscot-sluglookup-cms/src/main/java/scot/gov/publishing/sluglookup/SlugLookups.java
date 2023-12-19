package scot.gov.publishing.sluglookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.Query;

import static scot.gov.publishing.sluglookup.SlugLookupPaths.slugLookupPath;

/**
 * Contains logic used by SlugMaintenanceListener to update and remove lookups.
 */
public class SlugLookups {

    private static final Logger LOG = LoggerFactory.getLogger(SlugLookups.class);

    private static final String PATH = "sluglookup:path";

    Session session;

    static int LETTER_DEPTH = 10;

    public SlugLookups(Session session) {
        this.session = session;
    }

    public void updateLookup(String slug, String path, String site, String type, String mountType, boolean clearLookup) throws RepositoryException {
        // check if there is an existing lookup for this path
        Node existingLookup = findLookupForPath(site, type, mountType, path);
        if (existingLookup != null) {
            // check if there is an existing lookup that is the same as the one we would expect
            // if so then no action is required.
            if (isExpectedPath(existingLookup.getPath(), site, type, mountType, path)) {
                return;
            }

            // clear the existing lookup
            if (clearLookup) {
                clearLookup(existingLookup);
            }
        }

        // ensure that the lookup has been created for the current slug
        ensureLookupPath(slug, path, site, type, mountType);
        session.save();
    }

    public void removeLookup(String path, String site, String type, String mountType) throws RepositoryException {
        Node existingLookup = findLookupForPath(site, type, mountType, path);
        clearLookup(existingLookup);
    }

    public Node ensureLookupPath(String slug, String path, String site, String type, String mountType) throws RepositoryException {
        Node parent = ensureLookupRoot(site, type, mountType);
        String [] slugHashPath = SlugLookupPaths.slugHashPath(slug);
        Node one = ensureFolder(parent, slugHashPath[0]);
        Node two = ensureFolder(one, slugHashPath[1]);
        Node three = ensureFolder(two, slugHashPath[2]);
        Node four = ensureFolder(three, slugHashPath[3]);
        Node node = ensureFolder(four, slug);
        node.setProperty(PATH, path);
        return node;
    }

    private Node findLookupForPath(String site, String type, String mountType, String path) throws RepositoryException {
        String xpath = xpathFindLookupByPath(site, type, mountType, path);
        NodeIterator it = execute(xpath);

        if (it.getSize() == 0) {
            return null;
        }

        if (it.getSize() > 1) {
            LOG.warn("More than one entry for {}", path);
        }

        return it.nextNode();
    }

    private String xpathFindLookupByPath(String site, String type, String mountType, String path) {
        String template = "/jcr:root/content/urls/%s/%s/%s//element(*, sluglookup:lookup)[sluglookup:path = '%s']";
        return String.format(template, site, type, mountType, path);
    }

    private boolean isExpectedPath(String existingPath, String site, String type, String mountType, String slug) {
        String slugLookupPath = slugLookupPath(site, type, mountType, slug);
        return slugLookupPath.equals(existingPath);
    }

    private NodeIterator execute(String xpath) throws RepositoryException {
        return session
                .getWorkspace()
                .getQueryManager()
                .createQuery(xpath, Query.XPATH)
                .execute()
                .getNodes();
    }

    Node ensureLookupRoot(String site, String type, String mountType) throws RepositoryException {
        Node root = ensureFolder(session.getNode("/content"), "urls");
        Node siteRoot = ensureFolder(root, site);
        Node typeRoot = ensureFolder(siteRoot, type);
        return ensureFolder(typeRoot, mountType);
    }

    Node ensureFolder(Node parent, String name) throws RepositoryException {
        return parent.hasNode(name)
                ? parent.getNode(name)
                : parent.addNode(name, "sluglookup:lookup");
    }

    private Node ensureLookupPath(Node parent, int pos, String path) throws RepositoryException {

        if (pos == path.length()) {
            return parent;
        }

        if (pos >= LETTER_DEPTH) {
            return redirectNode(parent, path.substring(pos));
        }

        String element = Character.toString(escapeSlash(path.charAt(pos)));
        Node next = parent.hasNode(element)
                ? parent.getNode(element)
                : redirectNode(parent, element);
        int newPos = pos + 1;
        return ensureLookupPath(next, newPos, path);
    }

    private Node redirectNode(Node parent, String name) throws RepositoryException {
        return parent.addNode(name, "sluglookup:lookup");
    }

    void clearLookup(Node node) throws RepositoryException {
        node.getProperty(PATH).remove();
    }

    static Character escapeSlash(char c) {
        return c == '/' ? '\\' : c;
    }
}
