package scot.gov.publishing.hippo.funnelback.client;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static scot.gov.publishing.hippo.funnelback.client.LoggingHttpRequestInterceptor.REQUEST_LINE;
import static scot.gov.publishing.hippo.funnelback.client.LoggingHttpRequestInterceptor.STOPWATCH;

public class LoggingHttpResponseInterceptor implements HttpResponseInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingHttpResponseInterceptor.class);

    @Override
    public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
        StopWatch stopwatch = (StopWatch) httpContext.getAttribute(STOPWATCH);
        stopwatch.stop();
        RequestLine requestLine = (RequestLine) httpContext.getAttribute(REQUEST_LINE);
        LOG.info("funnelback-http-request {} {}, took {}", requestLine.getUri(), httpResponse.getStatusLine().getStatusCode(), stopwatch.getTime());
    }
}
