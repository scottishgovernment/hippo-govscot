package scot.gov.publishing.hippo.sso.security;

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scot.gov.publishing.hippo.sso.SsoAttributes;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SsoUserManager}.
 *
 * <p>Top-level tests cover the public contract (authenticate/hasUser/getUser, the resolution
 * cache) and the {@link SsoUserManager#resolveUserId(String)} chain mechanics, using fake
 * {@link SsoUserManager.UserIdResolver}s injected via the package-private {@code userIdResolvers}
 * field so the chain logic can be tested without any JCR mocking. Each individual resolution
 * strategy (lower-case match, email query, linear scan) has its own {@link Nested} test class
 * that calls the corresponding {@code resolveByXxx} method directly and mocks only what that
 * strategy touches.
 */
public class SsoUserManagerTest {

    private HippoUserManager delegate;

    private Session session;

    private SsoUserManager sut;

    @BeforeEach
    void setUp() {
        delegate = mock(HippoUserManager.class);
        session = mock(Session.class);
        sut = new SsoUserManager(delegate, session);
    }

    // -- resolveUserId(SimpleCredentials) --

    /**
     * Local logins are passed through unchanged, with no lookup at all - this is what keeps
     * a plain-password login with an unknown username cheap: it never reaches
     * resolveUserId(String)'s resolver chain.
     */
    @Test
    void localLoginReturnsRawUserIdWithoutLookup() throws RepositoryException {
        SimpleCredentials creds = new SimpleCredentials("unknown.user", new char[0]);

        assertEquals("unknown.user", sut.resolveUserId(creds));
        verifyNoInteractions(delegate);
    }

    @Test
    void ssoLoginUsesFullResolutionChain() throws RepositoryException {
        // "Unknown.User" isn't an exact match, but the lower-cased form is a known account -
        // only reachable via the full resolveUserId(String) chain, not the passthrough used
        // for local logins.
        when(delegate.hasUser("Unknown.User")).thenReturn(false);
        when(delegate.hasUser("unknown.user")).thenReturn(true);
        SimpleCredentials creds = new SimpleCredentials("Unknown.User", new char[0]);
        creds.setAttribute(SsoAttributes.SSO_ID, "Unknown.User");

        assertEquals("unknown.user", sut.resolveUserId(creds));
    }

    /**
     * resolveUserId never signals "not found" with null - once the chain is exhausted it
     * echoes the given ID back unchanged. Callers must check existence separately via
     * hasUser().
     */
    @Test
    void ssoLoginWithNoMatchReturnsOriginalUserIdUnchanged() throws RepositoryException {
        when(delegate.hasUser("nobody")).thenReturn(false);
        sut.userIdResolvers = List.of();
        SimpleCredentials creds = new SimpleCredentials("nobody", new char[0]);
        creds.setAttribute(SsoAttributes.SSO_ID, "nobody");

        assertEquals("nobody", sut.resolveUserId(creds));
    }

    // -- resolveUserId(String) chain mechanics --

    @Nested
    class ResolveUserIdChainTest {

        @Test
        void resolversAreWiredWithDescriptiveNamesInOrder() {
            assertEquals(
                    List.of("lower case match", "email query", "linear scan"),
                    sut.userIdResolvers.stream().map(SsoUserManager.UserIdResolver::name).toList());
        }

        @Test
        void exactMatchIsReturnedWithoutConsultingResolvers() throws RepositoryException {
            when(delegate.hasUser("known.user")).thenReturn(true);
            sut.userIdResolvers = List.of(failIfCalledResolver());

            assertEquals("known.user", sut.resolveUserId("known.user"));
        }

        @Test
        void firstResolverToMatchWinsAndLaterResolversAreSkipped() throws RepositoryException {
            when(delegate.hasUser("Case.User")).thenReturn(false);
            sut.userIdResolvers = List.of(
                    stubResolver(null),
                    stubResolver("matched.user"),
                    failIfCalledResolver()
            );

            assertEquals("matched.user", sut.resolveUserId("Case.User"));
        }

        @Test
        void resolvedIdIsCachedSoResolversAreNotConsultedAgain() throws RepositoryException {
            when(delegate.hasUser("Case.User")).thenReturn(false);
            sut.userIdResolvers = List.of(stubResolver("matched.user"));
            sut.resolveUserId("Case.User");

            sut.userIdResolvers = List.of(failIfCalledResolver());

            assertEquals("matched.user", sut.resolveUserId("Case.User"));
        }

        @Test
        void noMatchingResolverReturnsOriginalUserId() throws RepositoryException {
            when(delegate.hasUser("nobody")).thenReturn(false);
            sut.userIdResolvers = List.of();

            assertEquals("nobody", sut.resolveUserId("nobody"));
        }
    }

    // -- resolveByLowerCase --

    @Nested
    class ResolveByLowerCaseTest {

        @Test
        void resolvesToExistingLowerCaseVariant() throws RepositoryException {
            when(delegate.hasUser("some.user")).thenReturn(true);

            assertEquals("some.user", sut.resolveByLowerCase("Some.User"));
        }

        @Test
        void returnsNullWhenLowerCaseVariantDoesNotExist() throws RepositoryException {
            when(delegate.hasUser("some.user")).thenReturn(false);

            assertNull(sut.resolveByLowerCase("Some.User"));
        }

        @Test
        void returnsNullWithoutLookupWhenUserIdIsAlreadyLowerCase() throws RepositoryException {
            assertNull(sut.resolveByLowerCase("some.user"));
            verifyNoInteractions(delegate);
        }
    }

    // -- resolveByEmailQuery --

    @Nested
    class ResolveByEmailQueryTest {

        private QueryResult queryResult;

        @BeforeEach
        void setUp() throws RepositoryException {
            Workspace workspace = mock(Workspace.class);
            QueryManager queryManager = mock(QueryManager.class);
            Query query = mock(Query.class);
            queryResult = mock(QueryResult.class);
            ValueFactory valueFactory = mock(ValueFactory.class);

            when(session.getWorkspace()).thenReturn(workspace);
            when(workspace.getQueryManager()).thenReturn(queryManager);
            when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(query);
            when(session.getValueFactory()).thenReturn(valueFactory);
            when(valueFactory.createValue(anyString())).thenReturn(mock(Value.class));
            when(query.execute()).thenReturn(queryResult);
        }

        @Test
        void resolvesToNodeWhoseNameIsACaseInsensitiveMatchForTheQueriedEmail() throws RepositoryException {
            NodeIterator nodes = nodesOf("Some.User");
            when(queryResult.getNodes()).thenReturn(nodes);

            assertEquals("Some.User", sut.resolveByEmailQuery("some.user"));
        }

        @Test
        void skipsNodesWhoseNameIsNotACaseInsensitiveMatch() throws RepositoryException {
            NodeIterator nodes = nodesOf("Other.User");
            when(queryResult.getNodes()).thenReturn(nodes);

            assertNull(sut.resolveByEmailQuery("some.user"));
        }

        @Test
        void returnsNullWhenQueryFindsNoNodes() throws RepositoryException {
            when(queryResult.getNodes()).thenReturn(NodeIteratorAdapter.EMPTY);

            assertNull(sut.resolveByEmailQuery("nobody"));
        }
    }

    // -- resolveByScan --

    @Nested
    class ResolveByScanTest {

        @Test
        void findsCaseInsensitiveMatchAmongAllUsers() throws RepositoryException {
            NodeIterator nodes = nodesOf("Other.User", "Some.User");
            when(delegate.listUsers(0, 0)).thenReturn(nodes);

            assertEquals("Some.User", sut.resolveByScan("some.user"));
        }

        @Test
        void returnsNullWhenNoUserMatches() throws RepositoryException {
            when(delegate.listUsers(0, 0)).thenReturn(NodeIteratorAdapter.EMPTY);

            assertNull(sut.resolveByScan("nobody"));
        }
    }

    // -- hasUser --

    @Test
    void hasUserReturnsTrueForExactMatch() throws RepositoryException {
        when(delegate.hasUser("known.user")).thenReturn(true);

        assertTrue(sut.hasUser("known.user"));
    }

    @Test
    void hasUserReturnsFalseForUnknownUser() throws RepositoryException {
        when(delegate.hasUser("unknown.user")).thenReturn(false);

        assertFalse(sut.hasUser("unknown.user"));
    }

    @Test
    void hasUserUsesResolutionCachedByAPriorSsoLogin() throws RepositoryException {
        when(delegate.hasUser("Some.User")).thenReturn(false);
        when(delegate.hasUser("some.user")).thenReturn(true);
        SimpleCredentials creds = new SimpleCredentials("Some.User", new char[0]);
        creds.setAttribute(SsoAttributes.SSO_ID, "Some.User");
        sut.resolveUserId(creds); // populates the resolvedUserIds cache: "Some.User" -> "some.user"

        assertTrue(sut.hasUser("Some.User"));
    }

    // -- getUser --

    @Test
    void getUserUsesResolutionCachedByAPriorSsoLogin() throws RepositoryException {
        when(delegate.hasUser("Some.User")).thenReturn(false);
        when(delegate.hasUser("some.user")).thenReturn(true);
        SimpleCredentials creds = new SimpleCredentials("Some.User", new char[0]);
        creds.setAttribute(SsoAttributes.SSO_ID, "Some.User");
        sut.resolveUserId(creds);

        Node expected = mock(Node.class);
        when(delegate.getUser("some.user")).thenReturn(expected);

        assertEquals(expected, sut.getUser("Some.User"));
    }

    // -- authenticate --

    /**
     * authenticate() must not treat "resolveUserId returned without throwing" as "the user
     * exists". A naive `resolveUserId(id) != null` check breaks once resolveUserId stops
     * signalling a miss with null - without the hasUser() check this would make every SSO
     * login succeed regardless of whether a matching account exists.
     */
    @Test
    void ssoLoginWithNoMatchingAccountFailsToAuthenticate() throws RepositoryException {
        when(delegate.hasUser("nobody")).thenReturn(false);
        sut.userIdResolvers = List.of();
        SimpleCredentials creds = new SimpleCredentials("nobody", new char[0]);
        creds.setAttribute(SsoAttributes.SSO_ID, "nobody");

        assertFalse(sut.authenticate(creds));
    }

    @Test
    void ssoLoginWithMatchingAccountAuthenticates() throws RepositoryException {
        when(delegate.hasUser("known.user")).thenReturn(true);
        SimpleCredentials creds = new SimpleCredentials("known.user", new char[0]);
        creds.setAttribute(SsoAttributes.SSO_ID, "known.user");

        assertTrue(sut.authenticate(creds));
    }

    // -- helpers --

    private static Node userNode(String name) {
        try {
            return when(mock(Node.class).getName()).thenReturn(name).getMock();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NodeIterator nodesOf(String... names) {
        return new NodeIteratorAdapter(Stream.of(names)
                .map(SsoUserManagerTest::userNode)
                .toList());
    }

    private static SsoUserManager.UserIdResolver stubResolver(String result) {
        return new SsoUserManager.UserIdResolver("stub", userId -> result);
    }

    private static SsoUserManager.UserIdResolver failIfCalledResolver() {
        return new SsoUserManager.UserIdResolver("should not be called", userId -> {
            throw new AssertionError("resolver should not have been called");
        });
    }

}
