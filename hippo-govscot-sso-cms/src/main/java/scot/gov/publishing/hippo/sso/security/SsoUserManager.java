package scot.gov.publishing.hippo.sso.security;

import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.security.user.DelegatingHippoUserManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.hippoecm.repository.security.user.RepositoryUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.hippo.sso.SsoAttributes;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSO-aware user manager that adds case-insensitive user ID resolution.
 *
 * <p>Uses delegation rather than extending {@code AbstractUserManager} because
 * {@code AbstractUserManager} declares {@code hasUser} and {@code getUser} as
 * {@code final}. Delegation is the only way to intercept these methods to add
 * case-insensitive lookup via the resolved user ID cache.
 */
public class SsoUserManager extends DelegatingHippoUserManager {

    private static final Logger LOG = LoggerFactory.getLogger(SsoUserManager.class);

    private final Session session;

    List<UserIdResolver> userIdResolvers = List.of(
            new UserIdResolver("lower case match", this::resolveByLowerCase),
            new UserIdResolver("email query", this::resolveByEmailQuery),
            new UserIdResolver("linear scan", this::resolveByScan)
    );

    /**
     * Maps user IDs (as provided by the IdP) to the actual user IDs in the repository.
     * This cache is never evicted but its size has an upper bound determined by the
     * number of user IDs provided by the IdP.
     */
    private final ConcurrentMap<String, String> resolvedUserIds = new ConcurrentHashMap<>();

    public SsoUserManager(ManagerContext context) throws RepositoryException {
        this(createRepositoryUserManager(context), context.getSession());
    }

    SsoUserManager(HippoUserManager delegate, Session session) {
        super(delegate);
        this.session = session;
    }

    private static HippoUserManager createRepositoryUserManager(ManagerContext context) throws RepositoryException {
        RepositoryUserManager manager = new RepositoryUserManager();
        manager.init(context);
        return manager;
    }

    @Override
    public boolean hasUser(String userId) throws RepositoryException {
        return super.hasUser(resolveFromCache(userId));
    }

    @Override
    public boolean isActive(String userId) throws RepositoryException {
        return super.isActive(resolveFromCache(userId));
    }

    @Override
    public boolean isPasswordExpired(String userId) throws RepositoryException {
        return super.isPasswordExpired(resolveFromCache(userId));
    }

    @Override
    public Node getUser(String userId) throws RepositoryException {
        return super.getUser(resolveFromCache(userId));
    }

    private String resolveFromCache(String userId) {
        String resolved = resolvedUserIds.get(userId);
        return resolved != null ? resolved : userId;
    }

    @Override
    public boolean authenticate(SimpleCredentials creds) throws RepositoryException {
        if (creds.getAttribute(SsoAttributes.SSO_ID) != null) {
            return hasUser(resolveUserId(creds.getUserID()));
        }
        return super.authenticate(creds);
    }

    /**
     * Returns the user ID to use for authentication.
     * For SSO logins, this is either the user ID in the credentials, or if that doesn't
     * exist, it may return an existing user ID that is a case-insensitive match.
     * For local logins, this returns the user ID in the given credentials, and no
     * case-insensitive matching takes place.
     * In either case, if no such user exists, the user ID in the given credentials is
     * returned as-is. The caller must check the user exists with {@link #hasUser(String)}.
     */
    String resolveUserId(SimpleCredentials creds) throws RepositoryException {
        if (creds.getAttribute(SsoAttributes.SSO_ID) == null) {
            return creds.getUserID();
        }
        return resolveUserId(creds.getUserID());
    }

    /**
     * Resolve a user ID case-insensitively. If an exact match exists, returns it directly.
     * Otherwise searches all users for a case-insensitive match, caches the mapping, and
     * returns the actual mixed-case user ID. Returns the given user ID unchanged if no match
     * is found - this does not guarantee the returned ID exists; check with {@link #hasUser}.
     */
    String resolveUserId(String userId) throws RepositoryException {
        if (super.hasUser(userId)) {
            LOG.debug("resolveUserId: {} found by exact match", userId);
            return userId;
        }

        String cached = resolvedUserIds.get(userId);
        if (cached != null) {
            LOG.debug("resolveUserId: {} to {} from cache", userId, cached);
            return cached;
        }

        for (UserIdResolver resolver: userIdResolvers) {
            String resolvedUserId = resolver.resolve(userId);
            if (resolvedUserId != null) {
                String resolverName = resolver.name();
                LOG.info("resolveUserId: {} to {} by {}", userId, resolvedUserId, resolverName);
                resolvedUserIds.put(userId, resolvedUserId);
                return resolvedUserId;
            }
        }

        LOG.info("resolveUserId: {} not found", userId);
        return userId;
    }

    record UserIdResolver(String name, Strategy strategy) {
        String resolve(String userId) throws RepositoryException {
            return strategy.resolve(userId);
        }

        @FunctionalInterface
        interface Strategy {
            String resolve(String userId) throws RepositoryException;
        }
    }

    /**
     * Looks for a matching user ID that is lower case
     */
    String resolveByLowerCase(String userId) throws RepositoryException {
        String idLowerCase = userId.toLowerCase(Locale.ENGLISH);
        if (!userId.equals(idLowerCase) && hasUser(idLowerCase)) {
            return idLowerCase;
        }
        return null;
    }

    /**
     * Query for a user node whose hipposys:email matches the given email address
     * and whose name is a case-insensitive match for the email address.
     * The email property is set during SSO login by synchronizeOnLogin, so this
     * avoids a linear scan on subsequent logins (e.g. after a restart clears the cache).
     */
    String resolveByEmailQuery(String userId) throws RepositoryException {
        String sql = "SELECT * FROM [hipposys:user] WHERE [hipposys:email] = $email";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.bindValue("email", session.getValueFactory().createValue(userId));
        NodeIterator nodes = query.execute().getNodes();
        while (nodes.hasNext()) {
            String nodeName = NodeNameCodec.decode(nodes.nextNode().getName());
            if (nodeName.equalsIgnoreCase(userId)) {
                return nodeName;
            }
        }
        return null;
    }

    /**
     * Scan all users in the repository, looking for a case-insensitive match.
     */
    String resolveByScan(String userId) throws RepositoryException {
        NodeIterator nodes = listUsers(0, 0);
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            String nodeName = NodeNameCodec.decode(node.getName());
            if (nodeName.equalsIgnoreCase(userId)) {
                return nodeName;
            }
        }
        return null;
    }

}
