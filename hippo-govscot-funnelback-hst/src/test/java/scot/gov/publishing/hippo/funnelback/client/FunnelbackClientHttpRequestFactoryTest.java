package scot.gov.publishing.hippo.funnelback.client;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static scot.gov.publishing.hippo.funnelback.client.FunnelbackClientHttpRequestFactory.DEFAULT_USER_AGENT;
import static scot.gov.publishing.hippo.funnelback.client.FunnelbackClientHttpRequestFactory.SECURITY_TOKEN_HEADER;

class FunnelbackClientHttpRequestFactoryTest {

    @Test
    void defaultUserAgentUsedWhenNotSet() {
        FunnelbackClientHttpRequestFactory sut = new FunnelbackClientHttpRequestFactory();

        assertThat(sut.effectiveUserAgent()).isEqualTo(DEFAULT_USER_AGENT);
    }

    @Test
    void defaultUserAgentUsedWhenBlank() {
        FunnelbackClientHttpRequestFactory sut = new FunnelbackClientHttpRequestFactory();
        sut.setUserAgent("  ");

        assertThat(sut.effectiveUserAgent()).isEqualTo(DEFAULT_USER_AGENT);
    }

    @Test
    void configuredUserAgentUsedWhenSet() {
        FunnelbackClientHttpRequestFactory sut = new FunnelbackClientHttpRequestFactory();
        sut.setUserAgent("custom-agent");

        assertThat(sut.effectiveUserAgent()).isEqualTo("custom-agent");
    }

    /**
     * Sends real requests to a local HTTP server and inspects the headers received. Binds an ephemeral port on
     * loopback, so may not run in a sandboxed environment.
     */
    @Nested
    class RequestHeaders {

        HttpServer server;

        AtomicReference<Headers> received = new AtomicReference<>();

        @BeforeEach
        void startServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                received.set(exchange.getRequestHeaders());
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            server.start();
        }

        @AfterEach
        void stopServer() {
            server.stop(0);
        }

        @Test
        void sendsDefaultUserAgent() throws IOException {
            FunnelbackClientHttpRequestFactory sut = new FunnelbackClientHttpRequestFactory();
            sut.afterPropertiesSet();

            execute(sut);

            assertThat(received.get().getFirst("User-Agent")).isEqualTo(DEFAULT_USER_AGENT);
        }

        @Test
        void sendsConfiguredUserAgent() throws IOException {
            FunnelbackClientHttpRequestFactory sut = new FunnelbackClientHttpRequestFactory();
            sut.setUserAgent("custom-agent");
            sut.afterPropertiesSet();

            execute(sut);

            assertThat(received.get().getFirst("User-Agent")).isEqualTo("custom-agent");
        }

        @Test
        void sendsSecurityTokenWhenConfigured() throws IOException {
            FunnelbackClientHttpRequestFactory sut = new FunnelbackClientHttpRequestFactory();
            sut.setToken("secret");
            sut.afterPropertiesSet();

            execute(sut);

            assertThat(received.get().getFirst(SECURITY_TOKEN_HEADER)).isEqualTo("secret");
        }

        @Test
        void omitsSecurityTokenWhenBlank() throws IOException {
            FunnelbackClientHttpRequestFactory sut = new FunnelbackClientHttpRequestFactory();
            sut.setToken("");
            sut.afterPropertiesSet();

            execute(sut);

            assertThat(received.get().containsKey(SECURITY_TOKEN_HEADER)).isFalse();
        }

        void execute(FunnelbackClientHttpRequestFactory factory) throws IOException {
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/s/search.json");
            try (ClientHttpResponse response = factory.createRequest(uri, HttpMethod.GET).execute()) {
                assertThat(response.getStatusCode().value()).isEqualTo(200);
            }
        }
    }
}
