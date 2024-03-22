package scot.gov.publishing.sluglookup;

public class PathSourceFactory {

    private PathSourceFactory() {
        // private constructor
    }
    public static PathForSlugSource lookup() {
        return new LookupPathSource();
    }

    /**
     * Used by gov during the swtichover when anot all lookups were present
     */
    public static PathForSlugSource withFallback(PathForSlugSource fallback) {
        return new FallbackSlugSource(new LookupPathSource(), fallback);
    }

    /**
     * Used as a temporary fix for the issue causing some slug lookup nodes to not have a path property
     * this will mean that the query backup is only used in this limited case and stops it being subject
     * to a dictionary attack
     */
    public static PathForSlugSource withMissingPropertyFallback(PathForSlugSource fallback) {
        return new LookupPathSource(fallback);
    }

}
