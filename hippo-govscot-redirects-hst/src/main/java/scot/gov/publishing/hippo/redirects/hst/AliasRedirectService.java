package scot.gov.publishing.hippo.redirects.hst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.hippo.redirects.Redirect;
import scot.gov.publishing.hippo.redirects.RedirectRepository;
import scot.gov.publishing.hippo.redirects.SwitchingRedirectRepository;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Optional;

/**
 * Looks up URL alias redirects from JCR, with a bounded LRU cache to serve repeated lookups
 * without hitting JCR on every request.
 *
 * <p>The cache is read-through: on a miss the value is loaded from JCR, stored, and returned in
 * one atomic step. Both hits (a redirect exists) and misses (no redirect for this path) are
 * cached, so paths with no redirect do not cause repeated JCR reads.
 *
 * <p>The active lookup strategy is delegated to {@link SwitchingRedirectRepository}, which
 * consults feature flags to choose between the hash-bucketed and legacy path-mirror structures.
 */
public class AliasRedirectService {

    private static final Logger LOG = LoggerFactory.getLogger(AliasRedirectService.class);

    /**
     * Sentinel stored in the cache for paths that have no redirect, allowing misses to be
     * cached without using {@code null} (which Guava disallows).
     */
    private static final Redirect NO_REDIRECT = new Redirect();

    /**
     * Looks up the redirect target for the given path.
     *
     * @param session an active JCR session
     * @param path    the incoming request path (e.g. {@code /old/page})
     * @return the redirect, or empty if no alias redirect exists for this path
     */
    public Optional<Redirect> lookup(Session session, String path) {
        String normalisedPath = removeTrailingSlash(path);
        Redirect result = doLookup(session, normalisedPath);
        if (result == NO_REDIRECT) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private String removeTrailingSlash(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private Redirect doLookup(Session session, String path) {
        try {
            RedirectRepository repo = new SwitchingRedirectRepository(session);
            return repo.lookup(path).orElse(NO_REDIRECT);
        } catch (RepositoryException e) {
            LOG.error("Failed to lookup alias redirect for path '{}'", path, e);
            return NO_REDIRECT;
        }
    }
}
