package scot.gov.publishing.journal;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * Fetches HTML content from the local CMS site for indexing purposes.
 * Translates public URLs to their local equivalents and optionally sets the
 * {@code X-Forwarded-Host} header so the site renders as if serving from the public domain.
 */
public class SiteContentFetcher implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SiteContentFetcher.class);

    private final String localSiteUrl;

    private final String siteBaseUrl;

    /** Value for {@code X-Forwarded-Host} header; {@code null} to omit the header. */
    private final String forwardedHost;

    private final CloseableHttpClient httpClient;

    public SiteContentFetcher(String localSiteUrl, String siteBaseUrl, String forwardedHost) {
        this.localSiteUrl = localSiteUrl;
        this.siteBaseUrl = siteBaseUrl;
        this.forwardedHost = forwardedHost;
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Fetches the HTML content at the given public URL via the local site.
     * Returns {@code null} if the server returns a non-200 status; the error is logged.
     *
     * @throws IOException if the HTTP request itself fails
     */
    public String getHtml(String url) throws IOException {
        String localUrl = localUrl(url);
        HttpGet request = new HttpGet(localUrl);
        if (forwardedHost != null) {
            request.setHeader("X-Forwarded-Host", forwardedHost);
        }
        CloseableHttpResponse response = httpClient.execute(request);
        try {
            if (response.getStatusLine().getStatusCode() != 200) {
                LOG.error("Status was {} fetching {}", response.getStatusLine().getStatusCode(), localUrl);
                return null;
            }
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } finally {
            response.close();
        }
    }

    /**
     * Returns {@code true} if the local site's {@code /ping} endpoint responds with HTTP 200.
     */
    public boolean isPingResponding() {
        String pingUrl = localSiteUrl + "ping";
        HttpGet request = new HttpGet(pingUrl);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return response.getStatusLine().getStatusCode() == 200;
        } catch (IOException e) {
            LOG.warn("Failed to fetch ping url {}", pingUrl, e);
        }
        return false;
    }

    String localUrl(String url) {
        return StringUtils.replace(url, siteBaseUrl, localSiteUrl);
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

}
