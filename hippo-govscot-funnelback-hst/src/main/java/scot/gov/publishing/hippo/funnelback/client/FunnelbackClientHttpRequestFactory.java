package scot.gov.publishing.hippo.funnelback.client;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Request factory for outbound Funnelback requests.
 *
 * Configures the underlying HttpClient with a User-Agent, an X-Security-Token header when a token is configured,
 * and request timing logs. Both properties are optional: a blank user agent falls back to
 * {@link #DEFAULT_USER_AGENT}, and a blank token means no security header is sent.
 */
public class FunnelbackClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory implements InitializingBean {

    public static final String DEFAULT_USER_AGENT = "Bloomreach HST (digital-publishing@gov.scot)";

    static final String SECURITY_TOKEN_HEADER = "X-Security-Token";

    private String token;

    private String userAgent;

    @Override
    public void afterPropertiesSet() {
        LoggingHttpInterceptor logging = new LoggingHttpInterceptor();
        HttpClientBuilder builder = HttpClients.custom()
                .setUserAgent(effectiveUserAgent())
                .addRequestInterceptorFirst(logging)
                .addResponseInterceptorFirst(logging);

        if (!isBlank(token)) {
            builder.setDefaultHeaders(List.of(new BasicHeader(SECURITY_TOKEN_HEADER, token)));
        }

        setHttpClient(builder.build());
    }

    String effectiveUserAgent() {
        return isBlank(userAgent) ? DEFAULT_USER_AGENT : userAgent;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
