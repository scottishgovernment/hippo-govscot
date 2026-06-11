package scot.gov.publishing.hippo.funnelback.client;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;

public class LoggingHttpRequestInterceptor implements HttpRequestInterceptor {

    static final String STOPWATCH = "stopwatch";

    static final String REQUEST_LINE = "requestLine";

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
        httpContext.setAttribute(STOPWATCH, StopWatch.createStarted());
        httpContext.setAttribute(REQUEST_LINE, httpRequest.getRequestUri());
    }
}
