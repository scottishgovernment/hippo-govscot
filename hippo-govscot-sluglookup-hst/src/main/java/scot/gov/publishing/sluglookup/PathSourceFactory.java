package scot.gov.publishing.sluglookup;

public class PathSourceFactory {

    private PathSourceFactory() {
        // private constructor
    }
    public static PathForSlugSource lookup() {
        return new LookupPathSource();
    }

    public static PathForSlugSource withFallback(PathForSlugSource fallback) {
        return new FallbackSlugSource(new LookupPathSource(), fallback);
    }

}
