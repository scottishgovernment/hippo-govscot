package scot.gov.publishing.hippo.funnelback.client;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class LoggingHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

    public LoggingHttpComponentsClientHttpRequestFactory() {
        HttpClient client = HttpClients.custom()
                .addInterceptorFirst(new LoggingHttpRequestInterceptor())
                .addInterceptorFirst(new LoggingHttpResponseInterceptor())
                .build();
        setHttpClient(client);
    }
}
