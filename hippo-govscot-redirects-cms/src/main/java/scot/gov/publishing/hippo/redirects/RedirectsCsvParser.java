package scot.gov.publishing.hippo.redirects;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses a CSV text string into a list of {@link Redirect} objects, applying
 * URL normalisation to the {@code from} field.
 *
 * <p>Expected CSV columns: {@code from}, {@code to}, {@code description} (optional).
 * No header row.
 *
 * <p>This class factors out the common parsing and normalisation logic shared by
 * {@link RedirectsResource} and the CMS Redirects perspective, so that both entry
 * points process CSV identically.
 */
public class RedirectsCsvParser {

    public static final String GOV_SCOT_ORIGIN = "https://www.gov.scot";
    public static final String GOV_SCOT_ORIGIN_WWW2 = "https://www2.gov.scot";
    public static final Set<String> ORIGINS = Set.of(GOV_SCOT_ORIGIN, GOV_SCOT_ORIGIN_WWW2);

    /**
     * Parses the given CSV text and returns a list of normalised redirects.
     *
     * @param csvText raw CSV text; no header row expected
     * @return list of redirects with normalised {@code from} URLs
     * @throws IOException if the CSV cannot be parsed
     */
    public List<Redirect> parse(String csvText) throws IOException {
        try (CSVParser csvParser = new CSVParser(new StringReader(csvText), CSVFormat.DEFAULT)) {
            List<CSVRecord> records = csvParser.getRecords();
            List<Redirect> redirects = new ArrayList<>(records.size());
            for (CSVRecord record : records) {
                if (record.size() != 2) {
                    throw new IOException("Line " + record.getRecordNumber()
                            + ": expected 2 columns (from, to) but found " + record.size());
                }
                redirects.add(toRedirect(record));
            }
            return redirects;
        }
    }

    private Redirect toRedirect(CSVRecord record) {
        Redirect redirect = new Redirect();
        redirect.setFrom(normalizeFromUrl(record.get(0)));
        redirect.setTo(record.get(1));
        return redirect;
    }

    /**
     * Normalises a {@code from} URL for storage:
     * <ol>
     *   <li>If the value is a fully-qualified {@code https://www.gov.scot/} URL
     *       (or www2), the origin is stripped so only the path is retained.</li>
     *   <li>Any percent-encoded characters are decoded to their literal form.</li>
     *   <li>Any trailing slash is removed.</li>
     * </ol>
     */
    public static String normalizeFromUrl(String url) {
        if (url == null) {
            return null;
        }
        String path = ORIGINS.stream()
                .map(origin -> StringUtils.stripEnd(origin, "/"))
                .filter(url::startsWith)
                .findFirst()
                .map(origin -> url.substring(origin.length()))
                .orElse(url);
        String normalised = RedirectNodePath.normalisePath(path);
        return StringUtils.stripEnd(normalised, "/");
    }
}
