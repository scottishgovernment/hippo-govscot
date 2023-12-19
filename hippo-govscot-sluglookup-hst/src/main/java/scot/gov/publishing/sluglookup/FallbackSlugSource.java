package scot.gov.publishing.sluglookup;

import org.hippoecm.hst.container.RequestContextProvider;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class FallbackSlugSource implements PathForSlugSource {

    PathForSlugSource primary;

    PathForSlugSource fallback;

    public FallbackSlugSource(PathForSlugSource primary, PathForSlugSource fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public String get(String slug, String site, String type, String mount) throws RepositoryException {
        String path = primary.get(slug, site, type, mount);

        if (path != null) {
            return path;
        }

        path = fallback.get(slug, site, type, mount);
        return path;
    }

    void createLookup(String slug, String path, String site, String type, String mount) throws RepositoryException {
        Session session = RequestContextProvider.get().getSession();
        SlugLookups slugLookups = new SlugLookups(session);
        slugLookups.ensureLookupPath(slug, path, site, type, mount);
        session.save();
    }

}
