package scot.gov.publishing.hippo.redirects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MigrateAliasRedirectsJobTest {

    // -------------------------------------------------------------------------
    // fixArchiveUrl
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        // www2 host — the canonical example from the migration spec
        "https://webarchive.nrscotland.gov.uk/20200119101657/www2.gov.scot/Resource/Doc/264771/0079288.pdf,"
            + "https://webarchive.nrscotland.gov.uk/20200119101657/https://www2.gov.scot/Resource/Doc/264771/0079288.pdf",

        // plain www host
        "https://webarchive.nrscotland.gov.uk/20210301120000/www.gov.scot/Topics/foo/bar,"
            + "https://webarchive.nrscotland.gov.uk/20210301120000/https://www.gov.scot/Topics/foo/bar",

        // different archive host — should still be fixed
        "https://webarchive.example.com/20200119101657/www2.gov.scot/Resource/Doc/1/x.pdf,"
            + "https://webarchive.example.com/20200119101657/https://www2.gov.scot/Resource/Doc/1/x.pdf",

        // already has https:// — must be left unchanged
        "https://webarchive.nrscotland.gov.uk/20200119101657/https://www2.gov.scot/Resource/Doc/1/x.pdf,"
            + "https://webarchive.nrscotland.gov.uk/20200119101657/https://www2.gov.scot/Resource/Doc/1/x.pdf",

        // non-archive URL — must be left unchanged
        "https://www.gov.scot/some/path,https://www.gov.scot/some/path",

        // relative path — must be left unchanged
        "/some/relative/path,/some/relative/path",
    })
    void fixArchiveUrl_transformsCorrectly(String input, String expected) {
        assertEquals(expected, MigrateAliasRedirectsJob.fixArchiveUrl(input));
    }

    @Test
    void fixArchiveUrl_nullInput_returnsNull() {
        assertNull(MigrateAliasRedirectsJob.fixArchiveUrl(null));
    }
}
