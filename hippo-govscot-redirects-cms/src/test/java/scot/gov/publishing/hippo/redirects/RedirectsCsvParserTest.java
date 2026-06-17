package scot.gov.publishing.hippo.redirects;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedirectsCsvParserTest {

    private final RedirectsCsvParser parser = new RedirectsCsvParser();

    @Test
    void parse_returnsTwoColumnRow() throws IOException {
        List<Redirect> redirects = parser.parse("/old,/new");
        assertEquals(1, redirects.size());
        assertEquals("/old", redirects.get(0).getFrom());
        assertEquals("/new", redirects.get(0).getTo());
    }

    @Test
    void parse_throwsOnSingleColumnRow() {
        IOException ex = assertThrows(IOException.class, () -> parser.parse("/only-one-column"));
        assertTrue(ex.getMessage().contains("Line 1"), "expected line number in message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("found 1"), "expected column count in message: " + ex.getMessage());
    }

    @Test
    void parse_throwsOnSecondLineWithMissingColumn() {
        IOException ex = assertThrows(IOException.class, () -> parser.parse("/one,/two\n/three"));
        assertTrue(ex.getMessage().contains("Line 2"), "expected line 2 in message: " + ex.getMessage());
    }

    @Test
    void parse_throwsOnTooManyColumns() {
        IOException ex = assertThrows(IOException.class, () -> parser.parse("/old,/new,extra"));
        assertTrue(ex.getMessage().contains("Line 1"), "expected line number in message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("found 3"), "expected column count in message: " + ex.getMessage());
    }
}
