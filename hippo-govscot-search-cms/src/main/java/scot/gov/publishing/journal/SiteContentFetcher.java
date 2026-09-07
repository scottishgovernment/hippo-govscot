package scot.gov.publishing.journal;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Fetches HTML content from the site context of the local instance.
 * Translates public URLs to their local equivalents and sets the {@code X-Forwarded-Host} header
 * (derived from the public URL being fetched) so the site renders as if serving from the public
 * domain name.
 */
public class SiteContentFetcher implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SiteContentFetcher.class);

    private static final URI SITE_URL = URI.create("http://localhost:8080/site/");

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public SiteContentFetcher() {
        // Default constructor
    }

    /**
     * Fetches the HTML content for the given URL from the local site webapp.
     *
     * @param uri the canonical URI of the content to fetch
     * @throws IOException if the content of the URL cannot be fetched
     */
    public String getHtml(URI uri) throws IOException {
        HttpGet request = getRequest(uri);
        LOG.info("Fetching {}", request.getURI());
        return httpClient.execute(request, new BasicResponseHandler());
    }

    /**
     * Verifies that {@code /ping} endpoint on the site web app responds with HTTP 2xx.
     * If the site is not available, the response exception is thrown.
     */
    public void ping() throws IOException {
        URI pingUri = SITE_URL.resolve("ping");
        HttpGet request = new HttpGet(pingUri);
        LOG.info("Pinging {}", request.getURI());
        httpClient.execute(request, new BasicResponseHandler());
    }

    static HttpGet getRequest(URI uri) throws IOException {
        URI localUrI = localURL(uri);
        HttpGet httpGet = new HttpGet(localUrI);
        httpGet.setHeader("X-Forwarded-Scheme", uri.getScheme());
        httpGet.setHeader("X-Forwarded-Host", uri.getHost());
        return httpGet;
    }

    static URI localURL(URI uri) throws IOException {
        String pathWithoutLeadingSlash = StringUtils.removeStart(uri.getPath(), '/');
        try {
            return new URI(
                    SITE_URL.getScheme(),
                    SITE_URL.getUserInfo(),
                    SITE_URL.getHost(),
                    SITE_URL.getPort(),
                    SITE_URL.resolve(pathWithoutLeadingSlash).getPath(),
                    uri.getQuery(),
                    null);
        } catch (URISyntaxException ex) {
            throw new IOException("Could not create local URI for " + uri, ex);
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

}
