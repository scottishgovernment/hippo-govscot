package scot.gov.publishing.hippo.redirects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static scot.gov.publishing.hippo.redirects.RedirectsResource.ORIGINS;

class RedirectValidatorTest {

    private final RedirectValidator validator = new RedirectValidator(ORIGINS);

    // -------------------------------------------------------------------------
    // validFrom
    // -------------------------------------------------------------------------

    @Test
    void validFrom_rejectsBlankFrom() {
        assertFalse(validator.validFrom(redirect("", "/to")));
    }

    @Test
    void validFrom_acceptsSimplePath() {
        assertTrue(validator.validFrom(redirect("/old/page", "/new/page")));
    }

    @Test
    void validFrom_acceptsPercentEncodedColon() {
        assertTrue(validator.validFrom(redirect(
                "/binaries/content/gallery/govscot%3Axlarge", "/new")));
    }

    @Test
    void validFrom_acceptsDecodedColonInPath() {
        // Colon is valid in a path segment per RFC 3986 (except as first segment)
        assertTrue(validator.validFrom(redirect("/foo/govscot:xlarge", "/new")));
    }

    @Test
    void validFrom_doesNotUseToFieldForFromValidation() {
        // Previously validFrom mistakenly checked isValidPath(redirect.getTo())
        // This test has a valid from but an invalid to; from validation must pass
        // independently of the to value.
        Redirect r = redirect("/valid/from/path", "NOT_A_URL_OR_PATH");
        assertTrue(validator.validFrom(r));
    }

    // -------------------------------------------------------------------------
    // validTo
    // -------------------------------------------------------------------------

    @Test
    void validTo_rejectsBlankTo() {
        assertFalse(validator.validTo(redirect("/from", "")));
    }

    @Test
    void validTo_acceptsAbsoluteUrl() {
        assertTrue(validator.validTo(redirect("/from", "https://example.com/page")));
    }

    @Test
    void validTo_acceptsRelativePath() {
        assertTrue(validator.validTo(redirect("/from", "/new/page")));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Redirect redirect(String from, String to) {
        Redirect r = new Redirect();
        r.setFrom(from);
        r.setTo(to);
        return r;
    }
}
