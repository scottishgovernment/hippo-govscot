package scot.gov.publishing.hippo.funnelback.client;


import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class LoggingHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

    public LoggingHttpComponentsClientHttpRequestFactory() {
        HttpClient client = HttpClients.custom()
                .addRequestInterceptorFirst(new LoggingHttpRequestInterceptor())
                .addResponseInterceptorFirst(new LoggingHttpResponseInterceptor())
                .build();
        setHttpClient(client);
    }
}
