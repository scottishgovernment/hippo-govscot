package scot.gov.publishing.hippo.sso;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link OidcConfig}. {@code getConfigValue} is stubbed directly
 * (matching {@link RedirectHandler#getCmsBaseUrl}) so no HST infrastructure or
 * network discovery call is needed, and endpoint values are supplied explicitly.
 *
 * <p>{@code getConfigValue} and {@code instance} are static so this test class
 * claims an exclusive lock for the duration of each test, ensuring that tests
 * do not interfere with each other even if parallel test execution is enabled.
 */
@ResourceLock(value = "scot.gov.publishing.hippo.sso.OidcConfig", mode = ResourceAccessMode.READ_WRITE)
public class OidcConfigTest {

    @TempDir
    static Path tempDir;

    static Path keyFile;

    // getConfigValue is a static seam shared with production code, so its real
    // default is captured once and restored after every test.
    private static final Function<String, String> DEFAULT_GET_CONFIG_VALUE = OidcConfig.getConfigValue;

    private final Map<String, String> configValues = new HashMap<>();

    @BeforeAll
    static void generateKeyFile() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        keyFile = tempDir.resolve("test-client-key.pem");
        Files.writeString(keyFile, toPem(keyPair.getPrivate().getEncoded()));
    }

    private static String toPem(byte[] derEncoded) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(derEncoded);
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }

    @BeforeEach
    void setUp() {
        configValues.put("oidc.issuer", "https://idp.example.com");
        configValues.put("oidc.authorization.endpoint", "https://idp.example.com/auth");
        configValues.put("oidc.jwks.uri", "https://idp.example.com/jwks");
        configValues.put("oidc.token.endpoint", "https://idp.example.com/token");
        configValues.put("oidc.userinfo.endpoint", "https://idp.example.com/userinfo");
        configValues.put("oidc.client.id", "test-client");
        OidcConfig.getConfigValue = configValues::get;
    }

    @AfterEach
    void tearDown() {
        OidcConfig.getConfigValue = DEFAULT_GET_CONFIG_VALUE;
        OidcConfig.instance = null;
    }

    @Test
    void keyBranchCreatesDistinctClientAuthenticationPerCall() throws Exception {
        configValues.put("oidc.client.key.file", keyFile.toString());

        OidcConfig config = OidcConfig.loadConfig();

        ClientAuthentication first = config.newClientAuthentication();
        ClientAuthentication second = config.newClientAuthentication();

        assertInstanceOf(PrivateKeyJWT.class, first);
        assertInstanceOf(PrivateKeyJWT.class, second);
        assertNotEquals(
                ((PrivateKeyJWT) first).getJWTAuthenticationClaimsSet().getJWTID(),
                ((PrivateKeyJWT) second).getJWTAuthenticationClaimsSet().getJWTID());
    }

    @Test
    void keyBranchPublicJwksHasNoPrivateKeyMaterial() throws Exception {
        configValues.put("oidc.client.key.file", keyFile.toString());

        OidcConfig config = OidcConfig.loadConfig();
        JWK jwk = config.publicJwks().getKeys().get(0);

        assertEquals(1, config.publicJwks().getKeys().size());
        assertInstanceOf(RSAKey.class, jwk);
        assertFalse(jwk.isPrivate());
    }

    @Test
    void secretBranchUsesClientSecretBasicAndEmptyJwks() throws Exception {
        configValues.put("oidc.client.secret", "shared-secret");

        OidcConfig config = OidcConfig.loadConfig();

        assertInstanceOf(ClientSecretBasic.class, config.newClientAuthentication());
        assertEquals("{\"keys\":[]}", config.publicJwks().toString());
    }

    @Test
    void getCachesConfigAndReadsKeyFileOnce() throws IOException {
        // A private copy of keyFile, not the @BeforeAll-shared one, since this test
        // deletes it to prove get() does not re-read it on a second call.
        Path disposableKeyFile = tempDir.resolve("cache-test-key.pem");
        Files.copy(keyFile, disposableKeyFile);
        configValues.put("oidc.client.key.file", disposableKeyFile.toString());

        OidcConfig first = OidcConfig.get();
        Files.delete(disposableKeyFile);

        OidcConfig second = OidcConfig.get();

        assertSame(first, second);
        assertSame(first.publicJwks(), second.publicJwks());
    }
}
