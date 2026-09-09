package scot.gov.publishing.hippo.funnelback.client;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the request line and elapsed time of each HTTP exchange.
 *
 * Register the same instance as both a request and a response interceptor: the request side starts a stopwatch
 * in the HttpContext, the response side stops it and logs.
 */
public class LoggingHttpInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingHttpInterceptor.class);

    static final String STOPWATCH = "stopwatch";

    static final String REQUEST_LINE = "requestLine";

    private final String requestPrefix;

    public LoggingHttpInterceptor() {
        this("funnelback-http-request");
    }

    public LoggingHttpInterceptor(String requestPrefix) {
        this.requestPrefix = requestPrefix;
    }

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context) {
        context.setAttribute(STOPWATCH, StopWatch.createStarted());
        context.setAttribute(REQUEST_LINE, request.getRequestUri());
    }

    @Override
    public void process(HttpResponse response, EntityDetails entity, HttpContext context) {
        StopWatch stopwatch = (StopWatch) context.getAttribute(STOPWATCH);
        if (stopwatch == null) {
            return;
        }
        stopwatch.stop();
        LOG.info("{} {}, took {}", requestPrefix, context.getAttribute(REQUEST_LINE), stopwatch.getTime());
    }
}
