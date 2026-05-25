package scot.gov.publishing.hippo.redirects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedirectNodePathTest {

    // -----------------------------------------------------.  --------------------
    // normalisePath
    // -------------------------------------------------------------------------

    @Test
    void normalisePath_returnsNullForNull() {
        assertNull(RedirectNodePath.normalisePath(null));
    }

    @Test
    void normalisePath_returnsPathUnchangedWhenNoPercentEncoding() {
        assertEquals("/foo/bar", RedirectNodePath.normalisePath("/foo/bar"));
    }

    @Test
    void normalisePath_decodesUpperCasePercentEncodedColon() {
        assertEquals("/foo:bar", RedirectNodePath.normalisePath("/foo%3Abar"));
    }

    @Test
    void normalisePath_decodesLowerCasePercentEncodedColon() {
        assertEquals("/foo:bar", RedirectNodePath.normalisePath("/foo%3abar"));
    }

    @Test
    void normalisePath_decodesPercentEncodedSpace() {
        assertEquals("/foo bar", RedirectNodePath.normalisePath("/foo%20bar"));
    }

    @Test
    void normalisePath_decodesPercentEncodedParentheses() {
        assertEquals("/foo(bar)", RedirectNodePath.normalisePath("/foo%28bar%29"));
    }

    @Test
    void normalisePath_decodesPercentEncodedSquareBrackets() {
        assertEquals("/foo[bar]", RedirectNodePath.normalisePath("/foo%5Bbar%5D"));
    }

    @Test
    void normalisePath_preservesLiteralPlusSign() {
        assertEquals("/foo+bar", RedirectNodePath.normalisePath("/foo+bar"));
    }

    @Test
    void normalisePath_decodesEncodedPlusSign() {
        assertEquals("/foo+bar", RedirectNodePath.normalisePath("/foo%2Bbar"));
    }

    @Test
    void normalisePath_preservesPathStructure() {
        // Slashes that are path delimiters must not be affected by decoding
        assertEquals("/a/b/c", RedirectNodePath.normalisePath("/a/b/c"));
    }

    @Test
    void normalisePath_decodesMultipleSegmentsIndependently() {
        assertEquals("/govscot:foo/govscot:bar",
                RedirectNodePath.normalisePath("/govscot%3Afoo/govscot%3Abar"));
    }

    @Test
    void normalisePath_handlesRealWorldBinariesUrl() {
        String encoded = "/binaries/content/gallery/featureditems/cabinet2026-2-jpg/cabinet2026-2-jpg/govscot%3Axlargeeightcolumnsdoubled";
        String expected = "/binaries/content/gallery/featureditems/cabinet2026-2-jpg/cabinet2026-2-jpg/govscot:xlargeeightcolumnsdoubled";
        assertEquals(expected, RedirectNodePath.normalisePath(encoded));
    }

    @Test
    void normalisePath_leavesInvalidPercentSequenceUnchanged() {
        // %ZZ is not a valid hex sequence — return original segment rather than throwing
        assertEquals("/foo%ZZbar", RedirectNodePath.normalisePath("/foo%ZZbar"));
    }

    // -------------------------------------------------------------------------
    // path — encoded and decoded forms must map to the same JCR node path
    // -------------------------------------------------------------------------

    @Test
    void path_encodedAndDecodedColonProduceSameNodePath() {
        String encoded = "/binaries/content/gallery/govscot%3Axlarge";
        String decoded  = "/binaries/content/gallery/govscot:xlarge";
        assertEquals(RedirectNodePath.path("govscot", encoded),
                     RedirectNodePath.path("govscot", decoded));
    }

    @Test
    void path_encodedAndDecodedSpaceProduceSameNodePath() {
        assertEquals(RedirectNodePath.path("govscot", "/foo%20bar"),
                     RedirectNodePath.path("govscot", "/foo bar"));
    }

    @Test
    void path_encodedAndDecodedBracketsProduceSameNodePath() {
        assertEquals(RedirectNodePath.path("govscot", "/foo%28bar%29"),
                     RedirectNodePath.path("govscot", "/foo(bar)"));
    }

    @Test
    void path_mixedCaseEncodingProduceSameNodePath() {
        assertEquals(RedirectNodePath.path("govscot", "/foo%3Abar"),
                     RedirectNodePath.path("govscot", "/foo%3abar"));
    }
}
