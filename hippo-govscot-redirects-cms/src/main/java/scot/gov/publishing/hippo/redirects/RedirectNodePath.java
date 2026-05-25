package scot.gov.publishing.hippo.redirects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates stable, hierarchical JCR node paths for redirect storage using a
 * bucket tree to distribute nodes evenly across the repository.
 *
 * <h2>Path Structure</h2>
 * <pre>
 * /content/redirects/{site}/{b0}/{b1}/{b2}/{b3}/{fullHash}
 * </pre>
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li><b>Hashing</b> — the site and URL path are SHA-1 hashed to produce a
 *       40-character hex string, giving a uniform, collision-resistant key.
 *       SHA-1 is appropriate here as this is not a security use case.</li>
 *   <li><b>Bucket splitting</b> — the first {@value #BUCKET_DEPTH} hex characters
 *       are each used as a single path segment, creating intermediate nodes with
 *       at most 16 children (one per hex digit {@code 0–f}). This keeps the tree
 *       shallow and balanced.</li>
 *   <li><b>Leaf node</b> — the full hash is appended as the final
 *       segment, guaranteeing uniqueness with no collision risk.</li>
 * </ol>
 */
public class RedirectNodePath {

    private static final Logger LOG = LoggerFactory.getLogger(RedirectNodePath.class);

    public static final String ALIASES_ROOT = "/content/redirects";

    // 4 levels gives us 13 million nodes with a comfortable ceiling of 200 children at the leaf level, and the
    // intermediate nodes never exceed 16 children (one per hex digit)
    private static final int BUCKET_DEPTH = 4;

    private RedirectNodePath() {
        // hide default constructor
    }

    /**
     * Returns a stable, unique JCR node path for the given site + URL path.
     * Structure: {ALIASES_ROOT}/{site}/{b0}/{b1}/{b2}/{b3}/{fullHash}
     *
     * <p>The path is normalised before hashing so that percent-encoded and
     * decoded forms of the same URL always map to the same node
     * (e.g. {@code /foo%3Abar} and {@code /foo:bar} are equivalent).
     */
    public static String path(String site, String urlPath) {

        String hash = sha1Hex(normalisePath(urlPath));
        String[] buckets = bucketParts(hash);
        StringBuilder b = new StringBuilder(ALIASES_ROOT).append('/').append(site);
        for (String bucket : buckets) {
            b.append('/').append(bucket);
        }

        // Leaf node = full hash. Completely unique, no collision risk.
        b.append('/').append(hash);
        return b.toString();
    }

    /**
     * Splits the first BUCKET_DEPTH characters of the hash into individual
     * path segments, each being a single hex digit (0–f), so no parent node
     * ever has more than 16 children from bucketing alone.
     */
    static String[] bucketParts(String hash) {
        String[] parts = new String[BUCKET_DEPTH];
        for (int i = 0; i < BUCKET_DEPTH; i++) {
            parts[i] = hash.substring(i, i + 1);
        }
        return parts;
    }

    /**
     * Normalises a URL path to a canonical decoded form so that
     * percent-encoded and literal forms of the same character produce the
     * same hash.  For example, {@code /foo%3Abar}, {@code /foo%3abar}, and
     * {@code /foo:bar} all normalise to {@code /foo:bar}.
     *
     * <p>Each path segment is decoded independently so that literal slash
     * delimiters ({@code /}) are never altered.  The {@code +} character is
     * preserved as-is (in URL paths it is a literal plus, not an encoded
     * space as in query strings).  Malformed {@code %xx} sequences are left
     * unchanged rather than causing an error.
     */
    static String normalisePath(String urlPath) {
        if (urlPath == null || !urlPath.contains("%")) {
            return urlPath;
        }
        String[] segments = urlPath.split("/", -1);
        StringBuilder sb = new StringBuilder(urlPath.length());
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(decodeSegment(segments[i], urlPath));
        }
        return sb.toString();
    }

    private static String decodeSegment(String segment, String urlPath) {
        if (!segment.contains("%")) {
            return segment;
        }
        try {
            // Replace '+' before decoding so URLDecoder does not convert it to a space
            // (URLDecoder is designed for application/x-www-form-urlencoded, not URL paths)
            return URLDecoder.decode(segment.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            LOG.error("malformed url sequence, {} in {}", segment, urlPath, e);
            // malformed %xx sequence — keep original
            return segment;
        }
    }

    /**
     * SHA-1 hex of the input. 40 hex chars, astronomically low collision rate
     * for URL paths. SHA-1 is fine here — this is not a security use case.
     */
    static String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(40);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
