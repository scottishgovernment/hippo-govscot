package scot.gov.publishing.hippo.funnelback.client;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.message.RequestLine;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static scot.gov.publishing.hippo.funnelback.client.LoggingHttpRequestInterceptor.REQUEST_LINE;
import static scot.gov.publishing.hippo.funnelback.client.LoggingHttpRequestInterceptor.STOPWATCH;

public class LoggingHttpResponseInterceptor implements HttpResponseInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingHttpResponseInterceptor.class);

    @Override
    public void process(HttpResponse response, EntityDetails entity, HttpContext context) throws HttpException, IOException {
        StopWatch stopwatch = (StopWatch) context.getAttribute(STOPWATCH);
        stopwatch.stop();
        String requestLine = (String)context .getAttribute("requestLine");
        LOG.info("funnelback-http-request {}, took {}", new Object[]{requestLine, stopwatch.getTime()});
    }

}
