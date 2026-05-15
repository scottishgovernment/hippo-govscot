package scot.gov.publishing.hippo.redirects;

import org.apache.jackrabbit.util.Text;

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
 *       shallow and balanced across ~65,000 leaf buckets.</li>
 *   <li><b>Leaf node</b> — the full 40-character hash is appended as the final
 *       segment, guaranteeing uniqueness with no collision risk.</li>
 *   <li><b>JCR encoding</b> — the site name is sanitised with
 *       {@link org.apache.jackrabbit.util.Text#escapeIllegalJcrChars} to produce
 *       a valid, reversible JCR node name.</li>
 * </ol>
 *
 * <h2>Capacity</h2>
 * With 4 bucket levels and a ceiling of ~200 redirects per leaf node, the
 * structure comfortably supports approximately 13 million redirects before
 * any performance degradation.
 *
 * <h2>Lookup</h2>
 * Because the same site and URL path always produce the same node path, lookups
 * are O(1) — hash the input and navigate directly, no searching required.
 */
public class RedirectNodePath {

    public static final String ALIASES_ROOT = "/content/redirects";

    // 4 levels gives us 13 million nodes with a comfortable ceiling of 200 children at the leaf level, and the
    // intermediate nodes never exceed 16 children (one per hex digit)
    private static final int BUCKET_DEPTH = 4;

    private RedirectNodePath() {
        // hide default constructor
    }

    /**
     * Returns a stable, unique JCR node path for the given site + URL path.
     *
     * Structure: {ALIASES_ROOT}/{site}/{b0}/{b1}/{b2}/{fullHash}
     *
     * - Bucket levels (b0..b2) spread nodes across 4096 intermediate buckets.
     * - The leaf node is the full 16-char SHA-1 hex prefix, giving uniqueness.
     */
    public static String path(String site, String urlPath) {

        String hash = sha1Hex(urlPath);
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