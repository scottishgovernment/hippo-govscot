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

import static javax.jcr.nodetype.NodeType.NT_UNSTRUCTURED;
import static org.apache.jackrabbit.commons.JcrUtils.getOrCreateByPath;

/**
 * JCR-backed implementation of {@link RedirectRepository} using a hash-bucketed node layout.
 *
 * <p>Redirects are stored as {@code nt:unstructured} leaf nodes under a 2-level hex-character
 * bucket derived from the SHA-1 hash of the redirect path. This ensures no single JCR node
 * accumulates more than 16 structural children regardless of data volume.
 *
 * <p>Each leaf node carries:
 * <ul>
 *   <li>{@code govscot:url} — the redirect target</li>
 *   <li>{@code govscot:from} — the original source path (for verification)</li>
 *   <li>{@code govscot:description} — optional description</li>
 * </ul>
 */
public class JcrRedirectRepository implements RedirectRepository {

    public static final String PROP_URL = "govscot:url";
    public static final String PROP_FROM = "govscot:from";
    public static final String PROP_HISTORICAL = "govscot:historical";
    public static final String PROP_DESCRIPTION = "govscot:description";
    static final String GOVSCOT = "govscot";
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
        String nodePath = RedirectNodePath.path(GOVSCOT, path);
        if (!session.nodeExists(nodePath)) {
            return Optional.empty();
        }
        Node node = session.getNode(nodePath);
        if (!node.hasProperty(PROP_URL)) {
            return Optional.empty();
        }
        return Optional.of(result(node, path));
    }

    @Override
    public void save(Collection<Redirect> redirects) throws RepositoryException {
        for (Redirect redirect : redirects) {
            doSave(redirect);
        }
    }

    @Override
    public boolean delete(String path) throws RepositoryException {
        String nodePath = RedirectNodePath.path(GOVSCOT, path);
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
        String nodePath = RedirectNodePath.path(GOVSCOT, path);
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

    Node ensureRedirectNode(String path) throws RepositoryException {
        String redirectPath = RedirectNodePath.path(GOVSCOT, path);
        return getOrCreateByPath(redirectPath, NT_UNSTRUCTURED, session);
    }
}
