package scot.gov.publishing.hippo.sso.security;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.value.StringValue;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.AbstractSecurityProvider;
import org.hippoecm.repository.security.DelegatingSecurityProvider;
import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.security.SecurityProviderContext;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.group.RepositoryGroupManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.hippo.sso.SsoAttributes;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * Custom SecurityProvider that wraps the repository user manager with an
 * SSO-aware user manager, copies OIDC claims to the user node on login,
 * and logs successful logins.
 *
 * <p>Extends {@link AbstractSecurityProvider} rather than
 * {@link DelegatingSecurityProvider} in order to override {@link #syncUser}.
 * This allows OpenID Connect claims, such as names and email address, to be
 * synchronized to the repository user, when {@link #synchronizeOnLogin} in
 * the superclass is called.
 */
@SuppressWarnings("unused")
public class SsoSecurityProvider extends AbstractSecurityProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SsoSecurityProvider.class);

    private SecurityProviderContext context;

    @Override
    public void init(SecurityProviderContext context) throws RepositoryException {
        this.context = context;
        Session session = context.getSession();

        ManagerContext userContext = userContext(session);
        this.userManager = new SsoUserManager(userContext);

        this.groupManager = new RepositoryGroupManager();
        this.groupManager.init(groupContext(session));
    }

    @Override
    public UserManager getUserManager(Session session) throws RepositoryException {
        // Log calls to instantiate a use SsoUserManager.
        // Typically, this method would be called for internal use on the built-in
        // RepositorySecurityProvider. However, logging here allows verification
        // that this method isn't being called too frequently. Frequent calls
        // would suggest that the SSO ID -> repository ID cache in SsoUserManager
        // isn't being reused between logins.
        LOG.info("Creating new SsoUserManager");
        return new SsoUserManager(userContext(session));
    }

    @Override
    public GroupManager getGroupManager(Session session) throws RepositoryException {
        RepositoryGroupManager groupManager = new RepositoryGroupManager();
        groupManager.init(groupContext(session));
        return groupManager;
    }

    /**
     * Synchronizes the user on login, and logs successful logins.
     * The superclass runs the sync only once per credentials object; the same
     * check is reused here so each login is logged once, since a single CMS
     * login logs in several JCR sessions with the same credentials object
     * (e.g. the channel manager preview session).
     */
    @Override
    public void synchronizeOnLogin(SimpleCredentials creds) throws RepositoryException {
        boolean firstLogin = !Boolean.TRUE.equals(creds.getAttribute(SYNCED_ATTR_NAME));
        super.synchronizeOnLogin(creds);
        if (firstLogin) {
            logLogin(creds);
        }
    }

    @Override
    protected void syncUser(SimpleCredentials creds, HippoUserManager userMgr) throws RepositoryException {
        if (creds.getAttribute(SsoAttributes.SSO_ID) != null) {
            Node user = userMgr.getUser(creds.getUserID());
            copyAttribute(creds, SsoAttributes.SSO_EMAIL, user, User.PROP_EMAIL);
        }
        super.syncUser(creds, userMgr);
    }

    /**
     * Log a successful login, except those by system users (hipposys:system).
     * On instances where the site is served from the same instance as the CMS,
     * system users includes HST logins required to serve the site.
     */
    private void logLogin(SimpleCredentials creds) throws RepositoryException {
        HippoUserManager userMgr = (HippoUserManager) getUserManager();
        Node user = userMgr.getUser(creds.getUserID());
        if (user == null || JcrUtils.getBooleanProperty(user, HippoNodeType.HIPPO_SYSTEM, false)) {
            return;
        }

        // The node name is the resolved repository user ID.
        // creds.getUserID() is the ID as presented,
        // i.e. the username form field value, or the IdP claim for SSO.
        String userId = NodeNameCodec.decode(user.getName());
        boolean isSsoLogin = creds.getAttribute(SsoAttributes.SSO_ID) != null;
        LOG.atInfo()
                .addKeyValue("login.user", userId)
                .addKeyValue("login.claim", creds.getUserID())
                .addKeyValue("login.method", isSsoLogin ? "sso" : "password")
                .log("Successful login for user: {}", userId);
    }

    private void copyAttribute(SimpleCredentials creds, String attributeName, Node node, String propertyName)
            throws RepositoryException {
        String attribute = (String) creds.getAttribute(attributeName);
        if (attribute != null) {
            node.setProperty(propertyName, new StringValue(attribute));
        }
    }

    private ManagerContext userContext(Session session) throws RepositoryException {
        return new ManagerContext(
                session, context.getProviderPath(),
                context.getUsersPath(), context.isMaintenanceMode());
    }

    private ManagerContext groupContext(Session session) throws RepositoryException {
        return new ManagerContext(
                session, context.getProviderPath(),
                context.getGroupsPath(), context.isMaintenanceMode());
    }

}
