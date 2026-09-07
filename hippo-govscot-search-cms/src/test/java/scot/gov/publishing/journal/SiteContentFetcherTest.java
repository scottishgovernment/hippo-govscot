package scot.gov.publishing.journal;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class SiteContentFetcherTest {

    private final SiteContentFetcher fetcher = new SiteContentFetcher();

    @Test
    void createsGetRequest() throws IOException {
        var uri = URI.create("https://www.gov.scot/about/how-government-is-run");
        var request = SiteContentFetcher.getRequest(uri);
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getURI().getPath()).isEqualTo("/site/about/how-government-is-run");

        Header[] hostHeaders = request.getHeaders("X-Forwarded-Host");
        assertThat(hostHeaders).hasSize(1);
        assertThat(hostHeaders[0].getValue()).isEqualTo("www.gov.scot");

        Header[] schemeHeaders = request.getHeaders("X-Forwarded-Scheme");
        assertThat(schemeHeaders).hasSize(1);
        assertThat(schemeHeaders[0].getValue()).isEqualTo("https");
    }

}
