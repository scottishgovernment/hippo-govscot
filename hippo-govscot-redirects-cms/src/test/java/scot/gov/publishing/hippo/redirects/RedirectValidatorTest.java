package scot.gov.publishing.hippo.redirects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static scot.gov.publishing.hippo.redirects.RedirectsCsvParser.ORIGINS;

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
    // validateRedirects — line numbers in messages
    // -------------------------------------------------------------------------

    @Test
    void validateRedirects_includesLineNumberInViolation() {
        List<Redirect> redirects = List.of(
                redirect("/valid/from", "https://example.com"),
                redirect("", "/to"),                          // line 2: bad from
                redirect("/from", "NOT_A_URL")                // line 3: bad to
        );
        List<String> violations = validator.validateRedirects(redirects);
        assertEquals(2, violations.size());
        assertTrue(violations.get(0).startsWith("Line 2:"), "expected line 2 prefix, got: " + violations.get(0));
        assertTrue(violations.get(1).startsWith("Line 3:"), "expected line 3 prefix, got: " + violations.get(1));
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
