package scot.gov.publishing.hippo.redirects;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import scot.gov.publishing.jcr.SessionSaver;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.apache.jackrabbit.commons.JcrUtils.getOrCreateByPath;

/**
 * JCR-backed implementation of {@link RedirectRepository} using a hash-bucketed node layout.
 *
 * <p>Redirects are stored as {@code redirects:redirect} leaf nodes under a 2-level hex-character
 * bucket derived from the SHA-1 hash of the redirect path. This ensures no single JCR node
 * accumulates more than 16 structural children regardless of data volume.
 *
 * <p>Each leaf node carries:
 * <ul>
 *   <li>{@code redirects:url} — the redirect target</li>
 *   <li>{@code redirects:from} — the original source path (for verification)</li>
 *   <li>{@code redirects:description} — optional description</li>
 *   <li>{@code redirects:historical} — {@code true} when this is a historical URL entry</li>
 * </ul>
 *
 * <p>The {@code redirects} namespace and {@code redirects:redirect} node type are registered
 * via {@code hippo-govscot-redirects-repository}.
 */
public class JcrRedirectRepository implements RedirectRepository {

    public static final String PROP_URL = "redirects:url";
    public static final String PROP_FROM = "redirects:from";
    public static final String PROP_HISTORICAL = "redirects:historical";
    public static final String PROP_DESCRIPTION = "redirects:description";

    /**
     * Site identifier used as the first path segment under {@code /content/redirects/}.
     * This scopes the hash-bucketed tree to a single site and allows multiple sites to
     * coexist under the same root in future (e.g. {@code /content/redirects/mygov/...}).
     */
    static final String SITE = "govscot";

    private static final String NODE_TYPE        = "redirects:redirect";
    private static final String BUCKET_NODE_TYPE = "redirects:bucket";

    private final Session session;

    private final SessionSaver saver;

    public JcrRedirectRepository(Session session) {
        this(session, null);
    }

    public JcrRedirectRepository(Session session, SessionSaver saver) {
        this.session = session;
        this.saver = saver;
    }

    @Override
    public Optional<Redirect> lookup(String path) throws RepositoryException {
        String nodePath = RedirectNodePath.path(SITE, path);
        if (!session.nodeExists(nodePath)) {
            return Optional.empty();
        }
        Node node = session.getNode(nodePath);
        Redirect redirect = result(node, path);
        return Optional.of(redirect);
    }

    @Override
    public void save(Collection<Redirect> redirects) throws RepositoryException {
        for (Redirect redirect : redirects) {
            doSave(redirect);
        }
    }

    @Override
    public boolean delete(String path) throws RepositoryException {
        String nodePath = RedirectNodePath.path(SITE, path);
        if (!session.nodeExists(nodePath)) {
            return false;
        }
        Node node = session.getNode(nodePath);
        if (!node.hasProperty(PROP_URL)) {
            return false;
        }
        node.remove();
        sessionSave();
        return true;
    }

    @Override
    public RedirectResult list(String path) throws RepositoryException {
        String nodePath = RedirectNodePath.path(SITE, path);
        if (!session.nodeExists(nodePath)) {
            return null;
        }
        return result(session.getNode(nodePath), path);

    }

    RedirectResult result(Node node, String path) throws RepositoryException {
        RedirectResult result = new RedirectResult();
        result.setFrom(path);
        result.setTo(JcrUtils.getStringProperty(node, PROP_URL, ""));
        result.setHistoricalUrl(JcrUtils.getBooleanProperty(node, PROP_HISTORICAL, false));
        result.setDescription(node.hasProperty(PROP_DESCRIPTION) ? node.getProperty(PROP_DESCRIPTION).getString() : "");
        result.setChildren(Collections.emptyList());
        return result;
    }

    void doSave(Redirect redirect) throws RepositoryException {
        Node leaf = ensureRedirectNode(redirect.getFrom());
        if (redirect.isHistoricalUrl()) {
            leaf.setProperty(PROP_HISTORICAL, true);
        } else {
            leaf.setProperty(PROP_URL, redirect.getTo());
        }
        leaf.setProperty(PROP_FROM, redirect.getFrom());
        leaf.setProperty(PROP_DESCRIPTION, StringUtils.defaultString(redirect.getDescription()));
        sessionSave();
    }

    /**
     * Saves the session via the {@link SessionSaver} if one was supplied (batched, throttled),
     * or directly via {@link Session#save()} otherwise.
     */
    private void sessionSave() throws RepositoryException {
        if (saver != null) {
            saver.save();
        } else {
            session.save();
        }
    }

    /**
     * Finds or creates the {@code redirects:redirect} leaf node for the given path,
     * placing it under the hash-bucketed tree rooted at
     * {@code /content/redirects/govscot/&hellip;}.
     *
     * <p>Each intermediate node (the site node and the 4 hex-digit bucket levels) is created
     * as {@code redirects:bucket} so it can be distinguished from redirect leaves in JCR
     * queries (e.g. {@code SELECT * FROM [redirects:redirect]} returns only leaf nodes).
     */
    Node ensureRedirectNode(String path) throws RepositoryException {
        String redirectPath = RedirectNodePath.path(SITE, path);
        String parentPath = redirectPath.substring(0, redirectPath.lastIndexOf('/'));
        String leafName   = redirectPath.substring(redirectPath.lastIndexOf('/') + 1);
        Node parent = getOrCreateByPath(parentPath, BUCKET_NODE_TYPE, session);
        return parent.hasNode(leafName) ? parent.getNode(leafName) : parent.addNode(leafName, NODE_TYPE);
    }
}
