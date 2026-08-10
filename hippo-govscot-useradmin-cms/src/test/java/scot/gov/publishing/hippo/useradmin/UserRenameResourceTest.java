package scot.gov.publishing.hippo.useradmin;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRenameResourceTest {

    private static final String USERS_PATH = "/hippo:configuration/hippo:users";

    private static final String GROUPS_PATH = "/hippo:configuration/hippo:groups";

    @Mock
    private Session session;

    @Mock
    private SecurityService securityService;

    @Mock
    private HttpHeaders httpHeaders;

    private UserRenameResource resource;

    @BeforeEach
    void setUp() {
        resource = new UserRenameResource(session, "cmsadmin", () -> securityService);
    }

    private void asCmsAdmin(String username) throws Exception {
        basicAuthAs(username);
        stubMembershipsFor(username, Collections.singleton("cmsadmin"));
    }

    private void basicAuthAs(String username) {
        String credentials = Base64.getEncoder().encodeToString((username + ":password").getBytes(StandardCharsets.UTF_8));
        when(httpHeaders.getRequestHeader("Authorization")).thenReturn(Collections.singletonList("Basic " + credentials));
    }

    private void stubMembershipsFor(String username, Set<String> memberships) throws Exception {
        User user = mock(User.class);
        when(user.getMemberships()).thenReturn(memberships);
        when(securityService.hasUser(username)).thenReturn(true);
        when(securityService.getUser(username)).thenReturn(user);
    }

    private Node groupWithMembers(String... members) throws Exception {
        Node group = mock(Node.class);
        Property property = mock(Property.class);
        Value[] values = new Value[members.length];
        for (int i = 0; i < members.length; i++) {
            Value value = mock(Value.class);
            when(value.getString()).thenReturn(members[i]);
            values[i] = value;
        }
        when(group.hasProperty("hipposys:members")).thenReturn(members.length > 0);
        when(group.getProperty("hipposys:members")).thenReturn(property);
        when(property.getValues()).thenReturn(values);
        return group;
    }

    private NodeIterator iteratorOf(Node... nodes) {
        Iterator<Node> delegate = List.of(nodes).iterator();
        NodeIterator iterator = mock(NodeIterator.class);
        when(iterator.hasNext()).thenAnswer(invocation -> delegate.hasNext());
        when(iterator.nextNode()).thenAnswer(invocation -> delegate.next());
        return iterator;
    }

    @Test
    void rejectsCallerNotInCmsAdmin() throws Exception {
        basicAuthAs("someuser");
        stubMembershipsFor("someuser", Collections.singleton("editors"));

        RenameRequest request = new RenameRequest();
        request.setFrom("old@example.com");
        request.setTo("new@example.com");

        Response response = resource.rename(request, httpHeaders);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        verify(session, never()).move(eq(USERS_PATH + "/old@example.com"), eq(USERS_PATH + "/new@example.com"));
    }

    @Test
    void usesConfiguredAdminGroupName() throws Exception {
        UserRenameResource customGroupResource = new UserRenameResource(session, "siteadmins", () -> securityService);
        basicAuthAs("admin@example.com");
        stubMembershipsFor("admin@example.com", Collections.singleton("siteadmins"));
        when(securityService.hasUser("old@example.com")).thenReturn(true);
        when(securityService.hasUser("new@example.com")).thenReturn(false);
        when(session.nodeExists(GROUPS_PATH)).thenReturn(false);
        when(session.getNode(USERS_PATH + "/new@example.com")).thenReturn(mock(Node.class));

        RenameRequest request = new RenameRequest();
        request.setFrom("old@example.com");
        request.setTo("new@example.com");

        Response response = customGroupResource.rename(request, httpHeaders);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(session).move(USERS_PATH + "/old@example.com", USERS_PATH + "/new@example.com");
    }

    @Test
    void rejectsCallerInDifferentConfiguredGroup() throws Exception {
        UserRenameResource customGroupResource = new UserRenameResource(session, "siteadmins", () -> securityService);
        basicAuthAs("admin@example.com");
        stubMembershipsFor("admin@example.com", Collections.singleton("cmsadmin"));

        RenameRequest request = new RenameRequest();
        request.setFrom("old@example.com");
        request.setTo("new@example.com");

        Response response = customGroupResource.rename(request, httpHeaders);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void rejectsUnknownCaller() throws Exception {
        basicAuthAs("someuser");
        when(securityService.hasUser("someuser")).thenReturn(false);

        RenameRequest request = new RenameRequest();
        request.setFrom("old@example.com");
        request.setTo("new@example.com");

        Response response = resource.rename(request, httpHeaders);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void rejectsMissingCredentials() {
        when(httpHeaders.getRequestHeader("Authorization")).thenReturn(Collections.emptyList());

        RenameRequest request = new RenameRequest();
        request.setFrom("old@example.com");
        request.setTo("new@example.com");

        Response response = resource.rename(request, httpHeaders);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void rejectsBlankFromOrTo() throws Exception {
        asCmsAdmin("admin@example.com");

        RenameRequest request = new RenameRequest();
        request.setFrom("");
        request.setTo("new@example.com");

        Response response = resource.rename(request, httpHeaders);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void rejectsSameFromAndTo() throws Exception {
        asCmsAdmin("admin@example.com");

        RenameRequest request = new RenameRequest();
        request.setFrom("same@example.com");
        request.setTo("same@example.com");

        Response response = resource.rename(request, httpHeaders);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void rejectsUnknownFromUser() throws Exception {
        asCmsAdmin("admin@example.com");
        when(securityService.hasUser("old@example.com")).thenReturn(false);

        RenameRequest request = new RenameRequest();
        request.setFrom("old@example.com");
        request.setTo("new@example.com");

        Response response = resource.rename(request, httpHeaders);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void rejectsExistingToUser() throws Exception {
        asCmsAdmin("admin@example.com");
        when(securityService.hasUser("old@example.com")).thenReturn(true);
        when(securityService.hasUser("new@example.com")).thenReturn(true);

        RenameRequest request = new RenameRequest();
        request.setFrom("old@example.com");
        request.setTo("new@example.com");

        Response response = resource.rename(request, httpHeaders);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    void movesNodeUpdatesEmailAndGroupMemberships() throws Exception {
        asCmsAdmin("admin@example.com");
        when(securityService.hasUser("old@example.com")).thenReturn(true);
        when(securityService.hasUser("new@example.com")).thenReturn(false);

        String fromPath = USERS_PATH + "/old@example.com";
        String toPath = USERS_PATH + "/new@example.com";

        Node renamedUser = mock(Node.class);
        when(session.getNode(toPath)).thenReturn(renamedUser);

        Node editorsGroup = groupWithMembers("someoneelse@example.com", "old@example.com");
        when(session.nodeExists(GROUPS_PATH)).thenReturn(true);
        Node groupsNode = mock(Node.class);
        NodeIterator groupsIterator = iteratorOf(editorsGroup);
        when(session.getNode(GROUPS_PATH)).thenReturn(groupsNode);
        when(groupsNode.getNodes()).thenReturn(groupsIterator);

        RenameRequest request = new RenameRequest();
        request.setFrom("old@example.com");
        request.setTo("new@example.com");

        Response response = resource.rename(request, httpHeaders);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(session).move(fromPath, toPath);
        verify(renamedUser).setProperty("hipposys:email", "new@example.com");
        verify(editorsGroup).setProperty(eq("hipposys:members"),
                eq(new String[]{"someoneelse@example.com", "new@example.com"}));
        verify(session).save();
    }
}
