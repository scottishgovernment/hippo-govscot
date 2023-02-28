package scot.gov.publishing.hippo.funnelback.client;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class LoggingHttpRequestInterceptor implements HttpRequestInterceptor {

    static final String STOPWATCH = "stopwatch";

    static final String REQUEST_LINE = "requestLine";

    @Override
    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        httpContext.setAttribute(STOPWATCH, StopWatch.createStarted());
        httpContext.setAttribute(REQUEST_LINE, httpRequest.getRequestLine());
    }
}
