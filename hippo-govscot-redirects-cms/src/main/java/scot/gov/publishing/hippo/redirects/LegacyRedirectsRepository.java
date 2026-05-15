package scot.gov.publishing.hippo.redirects;

import org.hippoecm.repository.util.JcrUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static javax.jcr.nodetype.NodeType.NT_UNSTRUCTURED;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static scot.gov.publishing.hippo.redirects.JcrRedirectRepository.*;

/**
 * Legacy path-mirror implementation of {@link RedirectRepository}.
 *
 * <p>Stores redirects as JCR nodes whose path mirrors the URL:
 * {@code /content/redirects/Aliases/old/page → govscot:url = "/new/target"}.
 *
 * <p>Used by {@link SwitchingRedirectRepository} during the migration period.
 * Retire once all data has been moved to the hash-bucketed layout.
 */
public class LegacyRedirectsRepository implements RedirectRepository {

    private static final String REDIRECTS = "redirects";
    private static final String ALIASES = "Aliases";
    private static final String ALIASES_ROOT = "/content/redirects/" + ALIASES;

    private final Session session;

    public LegacyRedirectsRepository(Session session) {
        this.session = session;
    }

    @Override
    public Optional<Redirect> lookup(String path) throws RepositoryException {
        Node root = aliasesRoot();
        String relativePath = removeLeadingSlash(path);
        if (!root.hasNode(relativePath)) {
            return Optional.empty();
        }
        Node node = root.getNode(relativePath);
        if (!node.hasProperty(PROP_URL)) {
            return Optional.empty();
        }
        Redirect redirect = new Redirect();
        redirect.setDescription(JcrUtils.getStringProperty(node, PROP_DESCRIPTION, ""));
        redirect.setTo(JcrUtils.getStringProperty(node, PROP_URL, ""));
        redirect.setFrom(JcrUtils.getStringProperty(node, PROP_FROM, ""));
        redirect.setHistoricalUrl(JcrUtils.getBooleanProperty(node, PROP_HISTORICAL, false));
        return Optional.of(redirect);
    }

    @Override
    public void save(Collection<Redirect> redirects) throws RepositoryException {
        for (Redirect redirect : redirects) {
            doSave(redirect);
        }
        session.save();
    }

    @Override
    public boolean delete(String path) throws RepositoryException {
        Node root = aliasesRoot();
        String relativePath = removeLeadingSlash(path);
        if (!root.hasNode(relativePath)) {
            return false;
        }
        Node node = root.getNode(relativePath);
        if (!node.hasProperty(PROP_URL)) {
            return false;
        }
        if (!node.getNodes().hasNext()) {
            node.remove();
        } else {
            // node has children — remove just the URL rather than the whole node
            node.getProperty(PROP_URL).remove();
            node.setProperty(PROP_DESCRIPTION, "");
        }
        session.save();
        return true;
    }

    @Override
    public RedirectResult list(String path) throws RepositoryException {
        Node node = getNode(path);
        if (node == null) {
            return null;
        }
        NodeIterator it = node.getNodes();
        List<Redirect> children = new ArrayList<>();
        while (it.hasNext()) {
            children.add(toRedirect(it.nextNode()));
        }
        RedirectResult result = new RedirectResult();
        result.setChildren(children);
        populateRedirect(result, node);
        return result;
    }

    private Node getNode(String path) throws RepositoryException {
        Node root = aliasesRoot();
        if ("/".equals(path)) {
            return root;
        }
        String relativePath = removeLeadingSlash(path);
        return root.hasNode(relativePath) ? root.getNode(relativePath) : null;
    }

    private void doSave(Redirect redirect) throws RepositoryException {
        Node node = ensureRedirectPath(redirect.getFrom());
        node.setProperty(PROP_URL, redirect.getTo());
        node.setProperty(PROP_DESCRIPTION, isBlank(redirect.getDescription()) ? "" : redirect.getDescription());
    }

    private Redirect toRedirect(Node node) throws RepositoryException {
        Redirect redirect = new Redirect();
        populateRedirect(redirect, node);
        return redirect;
    }

    private void populateRedirect(Redirect redirect, Node node) throws RepositoryException {
        redirect.setFrom(substringAfter(node.getPath(), ALIASES_ROOT));
        redirect.setTo(node.hasProperty(PROP_URL) ? node.getProperty(PROP_URL).getString() : "");
        redirect.setDescription(node.hasProperty(PROP_DESCRIPTION) ? node.getProperty(PROP_DESCRIPTION).getString() : "");
    }

    private Node ensureRedirectPath(String from) throws RepositoryException {
        return ensureRedirectPath(aliasesRoot(), 0, asList(removeLeadingSlash(from).split("/")));
    }

    private Node ensureRedirectPath(Node parent, int pos, List<String> parts) throws RepositoryException {
        if (pos == parts.size()) {
            return parent;
        }
        String name = parts.get(pos);
        Node next = parent.hasNode(name) ? parent.getNode(name) : parent.addNode(name, NT_UNSTRUCTURED);
        return ensureRedirectPath(next, pos + 1, parts);
    }

    private Node aliasesRoot() throws RepositoryException {
        Node content = session.getNode("/content");
        Node redirectRoot = content.hasNode(REDIRECTS)
                ? content.getNode(REDIRECTS) : content.addNode(REDIRECTS, NT_UNSTRUCTURED);
        return redirectRoot.hasNode(ALIASES)
                ? redirectRoot.getNode(ALIASES) : redirectRoot.addNode(ALIASES, NT_UNSTRUCTURED);
    }

    private String removeLeadingSlash(String path) {
        return path.startsWith("/") ? substringAfter(path, "/") : path;
    }
}
