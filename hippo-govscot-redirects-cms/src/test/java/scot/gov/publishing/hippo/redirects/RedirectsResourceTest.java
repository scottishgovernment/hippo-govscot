package scot.gov.publishing.hippo.redirects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RedirectsResourceTest {

    // -------------------------------------------------------------------------
    // normalizeFromUrl
    // -------------------------------------------------------------------------

    @Test
    void normalizeFromUrl_leavesSimplePathUnchanged() {
        assertEquals("/publications/mypub", RedirectsResource.normalizeFromUrl("/publications/mypub"));
    }

    @Test
    void normalizeFromUrl_stripsGovScotOrigin() {
        assertEquals("/publications/mypub",
                RedirectsResource.normalizeFromUrl("https://www.gov.scot/publications/mypub"));
    }

    @Test
    void normalizeFromUrl_stripsGovScotOriginWithTrailingSlash() {
        assertEquals("/publications/mypub/",
                RedirectsResource.normalizeFromUrl("https://www.gov.scot/publications/mypub/"));
    }

    @Test
    void normalizeFromUrl_decodesPercentEncodedColon() {
        // %3A in path segments must be decoded so the stored path matches what the
        // servlet container gives the lookup service (Tomcat URL-decodes the request URI).
        assertEquals("/binaries/content/gallery/govscot:xlarge",
                RedirectsResource.normalizeFromUrl("/binaries/content/gallery/govscot%3Axlarge"));
    }

    @Test
    void normalizeFromUrl_decodesPercentEncodedPlusSign() {
        assertEquals("/binaries/EIR+202600513699+-++Information+Released.pdf",
                RedirectsResource.normalizeFromUrl("/binaries/EIR%2B202600513699%2B-%2B%2BInformation%2BReleased.pdf"));
    }

    @Test
    void normalizeFromUrl_stripsOriginAndDecodesEncoding() {
        // The combined case: full gov.scot URL with percent-encoded segments
        assertEquals(
                "/binaries/content/documents/govscot/publications/govscot:document/EIR+202600513699.pdf",
                RedirectsResource.normalizeFromUrl(
                        "https://www.gov.scot/binaries/content/documents/govscot/publications/govscot%3Adocument/EIR%2B202600513699.pdf"));
    }

    @Test
    void normalizeFromUrl_leavesOtherFullUrlUnchanged() {
        String other = "https://www.example.com/publications/mypub";
        assertEquals(other, RedirectsResource.normalizeFromUrl(other));
    }

    @Test
    void normalizeFromUrl_handlesNull() {
        assertNull(RedirectsResource.normalizeFromUrl(null));
    }
}
