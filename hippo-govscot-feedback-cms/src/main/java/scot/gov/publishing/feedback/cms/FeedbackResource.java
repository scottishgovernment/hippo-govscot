package scot.gov.publishing.feedback.cms;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * REST endpoints for feedback UI
 */
public class FeedbackResource {

    private static final Logger LOG = LoggerFactory.getLogger(FeedbackResource.class);

    private static final Pattern VALID_DOMAIN = Pattern.compile("([a-z]+)(\\.[a-z]+)+");

    /**
     * The name of the host group used for sites in production.
     */
    private final String hostGroup;

    private final String hstNode;

    public FeedbackResource(String hostGroup, String hstNode) {
        this.hostGroup = hostGroup;
        this.hstNode = hstNode;
    }

    /**
     * Returns 200 OK if the user is authorised to access feedback for a site,
     * and 403 Forbidden if they are not.
     */
    @GET
    @Path("site")
    public Response authorised(@QueryParam("site") String site) {
        if (site == null || !isValidDomainName(site)) {
            LOG.info("Invalid/unspecified domain name: {}", site);
            return Response.status(Status.BAD_REQUEST).build();
        }
        try {
            HippoSession session = UserSession.get().getJcrSession();
            SessionUser user = session.getUser();
            return authResponse(user, site);
        } catch (RepositoryException ex) {
            LOG.error("Failed to authenticate user for feedback", ex);
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    private Response authResponse(SessionUser user, String site) throws RepositoryException {
        HippoSession session = UserSession.get().getJcrSession();
        String alias = getSiteAlias(session, site);
        if (alias == null) {
            LOG.warn("Unknown site for feedback: {}", site);
            return Response.status(Status.BAD_REQUEST).build();
        }

        boolean authorised = authorised(user, alias);
        if (authorised) {
            return Response.ok().build();
        } else {
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

    private boolean authorised(SessionUser user, String alias) {
        HippoSession session = UserSession.get().getJcrSession();

        Set<String> groups = user.getMemberships();
        Set<String> roles = user.getUserRoles();
        String role = "scotgov.feedback.viewer";
        boolean inRole = session.isUserInRole(role);

        boolean inGroup = user.getMemberships().contains(alias + "-feedback");
        LOG.debug("Feedback access to {} for user {} with groups {} and roles {}: {}",
                alias,
                user.getId(),
                groups,
                roles,
                inRole);
        return inRole && inGroup;
    }

    private String getSiteAlias(Session session, String site) throws RepositoryException {
        String[] segments = StringUtils.split(site, '.');
        ArrayUtils.reverse(segments);
        String path = StringUtils.join(segments, "/");
        String jcrPath = String.format(
                "/%s/hst:hosts/%s/%s/hst:root",
                hstNode,
                hostGroup,
                path);
        try {
            Node root = session.getNode(jcrPath);
            Property aliasProperty = root.getProperty("hst:alias");
            return aliasProperty.getString();
        } catch (PathNotFoundException ex) {
            LOG.info("Could not find site {} using path {}", site, jcrPath);
            LOG.trace("Caught exception", ex);
            return null;
        }
    }

    private boolean isValidDomainName(String name) {
        return VALID_DOMAIN.matcher(name).matches();
    }

    /**
     * Endpoint to return a list of sites for which the user has permission
     * to read feedback.
     */
    @GET
    @Path("sites")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sites() {
        try {
            HippoSession session = UserSession.get().getJcrSession();
            Map<String, String> sites = getSites(session);
            return Response.ok().entity(sites.values()).build();
        } catch (RepositoryException ex) {
            LOG.error("Failed to list sites with feedback", ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, String> getSites(HippoSession session) throws RepositoryException {
        String prefix = String.format("/%s/hst:hosts/%s", hstNode, hostGroup);
        String sql = String.format("SELECT [hst:alias] FROM [hst:mount] " +
                "WHERE ISDESCENDANTNODE('%s') " +
                "AND [hst:type] IS NULL " +
                "AND [hst:mountpoint] IS NOT NULL",
                prefix);
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        Map<String, String> sites = new LinkedHashMap<>();
        for (RowIterator it = result.getRows(); it.hasNext();) {
            Row row = it.nextRow();
            String alias = row.getValues()[0].getString();
            if (authorised(session.getUser(), alias)) {
                String domain = hstRootPathToDomainName(row.getPath());
                sites.put(alias, domain);
            }
        }
        return sites;
    }

    /**
     * Returns the fully-qualified domain name for an HST mount given its path.
     * e.g. The FQDN for /hst:hst/hst:hosts/www/scot/gov/www/hst:root is www.gov.scot.
     */
    String hstRootPathToDomainName(String path) {
        // domain segments span indices 3 to -1 in
        // /hst:hst/hst:hosts/group/scot/.../www/hst:root
        String[] pathSegments = StringUtils.split(path, '/');
        int start = 3;
        int end = pathSegments.length - 1;
        String[] segments = Arrays.copyOfRange(pathSegments, start, end);
        ArrayUtils.reverse(segments);
        return StringUtils.join(segments, '.');
    }

}
