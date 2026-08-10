package scot.gov.publishing.hippo.sso;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.apache.commons.lang3.ObjectUtils;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

public record OidcConfig(
        String issuer,
        URI authorizationEndpoint,
        URI jwksUri,
        URI tokenEndpoint,
        URI userInfoEndpoint,
        ClientID clientId,
        Supplier<ClientAuthentication> clientAuthenticationFactory,
        JWKSet publicJwks
) {

    private static final Logger LOG = LoggerFactory.getLogger(OidcConfig.class);

    // Package-private so tests can substitute a stub, avoiding a dependency on the
    // HST component manager.
    static Function<String, String> getConfigValue = OidcConfig::containerConfigValue;

    // Package-private so tests can reset the cache between cases.
    static volatile OidcConfig instance;

    public ClientAuthentication newClientAuthentication() {
        return clientAuthenticationFactory.get();
    }

    public static OidcConfig get() {
        OidcConfig config = instance;
        if (config == null) {
            synchronized (OidcConfig.class) {
                config = instance;
                if (config == null) {
                    config = loadAndWrap();
                    instance = config;
                }
            }
        }
        return config;
    }

    private static OidcConfig loadAndWrap() {
        try {
            return loadConfig();
        } catch (IOException | GeneralException | JOSEException ex) {
            throw new SsoConfigurationException("Failed to load OIDC SSO configuration", ex);
        }
    }

    private static String containerConfigValue(String key) {
        ContainerConfiguration config = HstServices.getComponentManager().getContainerConfiguration();
        return config.getString(key);
    }

    static OidcConfig loadConfig() throws GeneralException, IOException, JOSEException {
        String issuer = getConfigValue.apply("oidc.issuer");
        String authorizationEndpoint = getConfigValue.apply("oidc.authorization.endpoint");
        String jwksUri = getConfigValue.apply("oidc.jwks.uri");
        String tokenEndpoint = getConfigValue.apply("oidc.token.endpoint");
        String userInfoEndpoint = getConfigValue.apply("oidc.userinfo.endpoint");

        if (ObjectUtils.anyNull(authorizationEndpoint, jwksUri, tokenEndpoint, userInfoEndpoint)) {
            OIDCProviderMetadata metadata = OIDCProviderMetadata.resolve(new Issuer(issuer));
            if (authorizationEndpoint == null) {
                authorizationEndpoint = metadata.getAuthorizationEndpointURI().toString();
            }
            if (jwksUri == null) {
                jwksUri = metadata.getJWKSetURI().toString();
            }
            if (tokenEndpoint == null) {
                tokenEndpoint = metadata.getTokenEndpointURI().toString();
            }
            if (userInfoEndpoint == null) {
                userInfoEndpoint = metadata.getUserInfoEndpointURI().toString();
            }
        }

        ClientID clientId = new ClientID(getConfigValue.apply("oidc.client.id"));
        URI tokenEndpointUri = URI.create(tokenEndpoint);

        String keyFile = getConfigValue.apply("oidc.client.key.file");
        Supplier<ClientAuthentication> clientAuthenticationFactory;
        JWKSet publicJwks;
        if (keyFile != null) {
            // PrivateKeyJWT signs a JWT at construction time, so create a fresh one per request.
            RSAKey rsaKey = loadRsaKey(keyFile);
            PrivateKey privateKey = rsaKey.toRSAPrivateKey();
            String keyID = rsaKey.getKeyID();
            clientAuthenticationFactory = () -> createClientAuthentication(
                    clientId, tokenEndpointUri, privateKey, keyID);
            publicJwks = new JWKSet(rsaKey.toPublicJWK());
        } else {
            // ClientSecretBasic is immutable, so a single instance can be reused.
            Secret secret = new Secret(getConfigValue.apply("oidc.client.secret"));
            ClientAuthentication clientSecretBasic = new ClientSecretBasic(clientId, secret);
            clientAuthenticationFactory = () -> clientSecretBasic;
            publicJwks = new JWKSet(Collections.emptyList());
        }

        return new OidcConfig(
                issuer,
                URI.create(authorizationEndpoint),
                URI.create(jwksUri),
                tokenEndpointUri,
                URI.create(userInfoEndpoint),
                clientId,
                clientAuthenticationFactory,
                publicJwks
        );
    }

    private static RSAKey loadRsaKey(String keyFile) throws IOException, JOSEException {
        String pem = Files.readString(Path.of(keyFile));
        JWK jwk = JWK.parseFromPEMEncodedObjects(pem);
        RSAKey rsaKey = new RSAKey.Builder(jwk.toRSAKey())
                .keyID(jwk.computeThumbprint().toString())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        LOG.info("Using private key JWT client authentication.");
        LOG.info("Public key JWK for IdP configuration: {}", rsaKey.toPublicJWK().toJSONString());
        return rsaKey;
    }

    private static ClientAuthentication createClientAuthentication(
            ClientID clientId, URI tokenEndpoint, PrivateKey privateKey, String keyID) {
        try {
            return new PrivateKeyJWT(
                    clientId, tokenEndpoint, JWSAlgorithm.RS256,
                    privateKey, keyID, null, null, null);
        } catch (JOSEException ex) {
            throw new SsoConfigurationException("Failed to create JWT", ex);
        }
    }

}
