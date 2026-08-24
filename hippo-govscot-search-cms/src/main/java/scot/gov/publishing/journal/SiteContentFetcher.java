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
 * Translates public URLs to their local equivalents and sets the
 * {@code X-Forwarded-Host} header (derived from the public URL being fetched) so the site
 * renders as if serving from the public domain.
 *
 * <p>Deployments that serve multiple sites from the same local Hippo instance (e.g. a publishing
 * platform) can pass a site name to {@link #getHtml(String, String)}; it is inserted as a path
 * segment right after {@code LOCAL_SITE_URL}, e.g. {@code http://localhost:8080/site/} becomes
 * {@code http://localhost:8080/site/&lt;site&gt;/}. A {@code null} or blank site, or one matching
 * {@code rootSite} (the site mounted at the HST root with no path segment), leaves the local URL
 * unchanged.
 */
public class SiteContentFetcher implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SiteContentFetcher.class);

    private static final String LOCAL_SITE_URL = "http://localhost:8080/site/";

    private final String rootSite;

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    /**
     * @param rootSite the site value that is mounted at the local HST root with no path segment
     *                 (e.g. {@code "gov"} for a single-site deployment); blank for deployments
     *                 where every site is a named path segment
     */
    public SiteContentFetcher(String rootSite) {
        this.rootSite = rootSite;
    }

    /**
     * Fetches the HTML content at the given public URL via the local site.
     * Returns {@code null} if the server returns a non-200 status; the error is logged.
     *
     * @param site the site to fetch from, for multi-site deployments; {@code null} or blank for
     *             the single default site
     * @throws IOException if the HTTP request itself fails
     */
    public String getHtml(String site, String url) throws IOException {
        String localUrl = localUrl(site, url);
        HttpGet request = new HttpGet(localUrl);
        String forwardedHost = hostOf(url);
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
        String pingUrl = LOCAL_SITE_URL + "ping";
        HttpGet request = new HttpGet(pingUrl);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                LOG.warn("Ping url {} returned status {}", pingUrl, status);
                return false;
            }
            return true;
        } catch (IOException e) {
            LOG.warn("Failed to fetch ping url {}", pingUrl, e);
        }
        return false;
    }

    String localUrl(String site, String url) {
        boolean atRoot = StringUtils.isBlank(site) || site.equals(rootSite);
        String localBase = atRoot ? StringUtils.removeEnd(LOCAL_SITE_URL, "/") : LOCAL_SITE_URL + site;
        return localBase + pathOf(url);
    }

    /**
     * Derives the {@code X-Forwarded-Host} value from the public URL being fetched, so that a
     * multi-site pipeline sends the correct host for each site without needing a separate
     * site-to-host mapping.
     */
    private String hostOf(String url) {
        return StringUtils.substringBefore(StringUtils.substringAfter(url, "://"), "/");
    }

    /**
     * Returns everything after the scheme and hostname of {@code url} (path plus query string, if
     * any), so it can be appended to the local base URL in place of the public one.
     */
    private String pathOf(String url) {
        String afterHost = StringUtils.substringAfter(url, "://");
        int pathStart = afterHost.indexOf('/');
        return pathStart == -1 ? "" : afterHost.substring(pathStart);
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

}
