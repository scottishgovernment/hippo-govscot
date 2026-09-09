package scot.gov.publishing.hippo.funnelback.client;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static scot.gov.publishing.hippo.funnelback.client.LoggingHttpInterceptor.REQUEST_LINE;
import static scot.gov.publishing.hippo.funnelback.client.LoggingHttpInterceptor.STOPWATCH;

class LoggingHttpInterceptorTest {

    LoggingHttpInterceptor sut = new LoggingHttpInterceptor();

    @Test
    void requestStartsStopwatchAndRecordsRequestLine() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getRequestUri()).thenReturn("/s/search.json?query=test");
        HttpContext context = new BasicHttpContext();

        sut.process(request, null, context);

        assertThat(context.getAttribute(REQUEST_LINE)).isEqualTo("/s/search.json?query=test");
        assertThat(context.getAttribute(STOPWATCH)).isInstanceOfSatisfying(StopWatch.class,
                stopwatch -> assertThat(stopwatch.isStarted()).isTrue());
    }

    @Test
    void responseStopsStopwatch() {
        HttpContext context = new BasicHttpContext();
        StopWatch stopwatch = StopWatch.createStarted();
        context.setAttribute(STOPWATCH, stopwatch);
        context.setAttribute(REQUEST_LINE, "/s/search.json");

        sut.process(mock(HttpResponse.class), null, context);

        assertThat(stopwatch.isStopped()).isTrue();
    }

    @Test
    void responseWithoutStopwatchIsIgnored() {
        HttpContext context = new BasicHttpContext();

        assertThatCode(() -> sut.process(mock(HttpResponse.class), null, context)).doesNotThrowAnyException();
    }
}
