package scot.gov.publishing.hippo.useradmin;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class UserRenameResource {

    private static final Logger LOG = LoggerFactory.getLogger(UserRenameResource.class);

    private static final String USERS_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.USERS_PATH;

    private static final String GROUPS_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.GROUPS_PATH;

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BASIC_PREFIX = "Basic ";

    private final Session session;

    private final String adminGroup;

    private final Supplier<SecurityService> securityServiceSupplier;

    public UserRenameResource(Session session, String adminGroup) {
        this(session, adminGroup, () -> HippoServiceRegistry.getService(SecurityService.class));
    }

    UserRenameResource(Session session, String adminGroup, Supplier<SecurityService> securityServiceSupplier) {
        this.session = session;
        this.adminGroup = adminGroup;
        this.securityServiceSupplier = securityServiceSupplier;
    }

    @POST
    @Path("rename")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rename(RenameRequest request, @Context HttpHeaders httpHeaders) {
        try {
            String username = callingUser(httpHeaders);
            LOG.error("User rename by {}", username);
            if (!isCmsAdmin(username)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            String from = request == null ? null : request.getFrom();
            String to = request == null ? null : request.getTo();
            Response validationFailure = validate(from, to);
            if (validationFailure != null) {
                return validationFailure;
            }

            SecurityService securityService = securityServiceSupplier.get();
            if (!securityService.hasUser(from)) {
                return Response.status(Response.Status.NOT_FOUND).entity("No such user: " + from).build();
            }
            if (securityService.hasUser(to)) {
                return Response.status(Response.Status.CONFLICT).entity("User already exists: " + to).build();
            }

            String fromPath = userPath(from);
            String toPath = userPath(to);
            session.move(fromPath, toPath);
            session.getNode(toPath).setProperty(HippoNodeType.HIPPOSYS_EMAIL, to);
            updateGroupMemberships(from, to);
            session.save();

            return Response.ok().build();
        } catch (RepositoryException e) {
            LOG.error("Failed to rename user", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response validate(String from, String to) {
        if (isBlank(from) || isBlank(to)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("from and to are required").build();
        }
        if (from.equals(to)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("from and to must be different").build();
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String userPath(String username) {
        return USERS_PATH + "/" + NodeNameCodec.encode(username, true);
    }

    private void updateGroupMemberships(String from, String to) throws RepositoryException {
        if (!session.nodeExists(GROUPS_PATH)) {
            return;
        }
        NodeIterator groups = session.getNode(GROUPS_PATH).getNodes();
        while (groups.hasNext()) {
            renameMemberIfPresent(groups.nextNode(), from, to);
        }
    }

    private void renameMemberIfPresent(Node group, String from, String to) throws RepositoryException {
        if (!group.hasProperty(HippoNodeType.HIPPO_MEMBERS)) {
            return;
        }
        Value[] values = group.getProperty(HippoNodeType.HIPPO_MEMBERS).getValues();
        List<String> members = new ArrayList<>();
        boolean changed = false;
        for (Value value : values) {
            String member = value.getString();
            if (member.equals(from)) {
                members.add(to);
                changed = true;
            } else {
                members.add(member);
            }
        }
        if (changed) {
            group.setProperty(HippoNodeType.HIPPO_MEMBERS, members.toArray(new String[0]));
        }
    }

    private boolean isCmsAdmin(String username) throws RepositoryException {
        SecurityService securityService = securityServiceSupplier.get();
        if (username == null || securityService == null || !securityService.hasUser(username)) {
            return false;
        }
        Set<String> memberships = securityService.getUser(username).getMemberships();
        return memberships.contains(adminGroup);
    }

    private String callingUser(HttpHeaders httpHeaders) {
        List<String> authHeaders = httpHeaders.getRequestHeader(AUTHORIZATION_HEADER);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        String header = authHeaders.get(0);
        if (header == null || !header.startsWith(BASIC_PREFIX)) {
            return null;
        }
        byte[] credentials = Base64.getDecoder().decode(header.substring(BASIC_PREFIX.length()));
        int colon = ArrayUtils.indexOf(credentials, (byte) ':');
        byte[] userId = Arrays.copyOf(credentials, Math.max(colon, 0));
        Arrays.fill(credentials, (byte) 0);
        return new String(userId, StandardCharsets.UTF_8);
    }
}
