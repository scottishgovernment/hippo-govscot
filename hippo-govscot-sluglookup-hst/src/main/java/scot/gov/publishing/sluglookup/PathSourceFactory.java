package scot.gov.publishing.sluglookup;
public class PathSourceFactory {
    private PathSourceFactory() {
        // private constructor
    }
    public static PathForSlugSource lookup() {
        PathForSlugSource lookupSource = new LookupPathSource();
        return new TimingPathSource(lookupSource);
    }

    public static PathForSlugSource withFallback(PathForSlugSource fallback) {
        PathForSlugSource lookupSource = new LookupPathSource();
        PathForSlugSource fallbackSource = new FallbackSlugSource(lookupSource, fallback);
        PathForSlugSource cachingPathSource = new CachingPathSource(fallbackSource);
        return new TimingPathSource(cachingPathSource);
    }

}
