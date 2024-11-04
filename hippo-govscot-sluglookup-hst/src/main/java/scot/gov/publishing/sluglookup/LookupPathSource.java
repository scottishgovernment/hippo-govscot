package scot.gov.publishing.sluglookup;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static scot.gov.publishing.sluglookup.SlugLookupPaths.slugLookupPath;

public class LookupPathSource implements PathForSlugSource {

    PathForSlugSource queryBackup = (slug, site, type, mount) -> null;

    public LookupPathSource() {
    }

    public LookupPathSource(PathForSlugSource queryBackup) {
        this.queryBackup = queryBackup;
    }

    @Override
    public String get(String slug, String site, String type, String mount) throws RepositoryException {
        String lookupPath = lookupPath(slug, site, type, mount);
        if (lookupPath == null) {
            return null;
        }
        return  lookupPath;
    }

    String lookupPath(String slug, String site, String type, String mount) throws RepositoryException {
        HstRequestContext req = RequestContextProvider.get();
        Session session = req.getSession();
        String slugLookupPath = slugLookupPath(slug, site, type, mount);
        if (!session.nodeExists(slugLookupPath)) {
            return null;
        }
        Node lookupNode = session.getNode(slugLookupPath);
        if (!lookupNode.hasProperty("sluglookup:path")) {
            // this is the case that is failing, use the queryBackup if one exists
            return queryBackup.get(slug, site, type, mount);
        }
        return lookupNode.getProperty("sluglookup:path").getString();
    }

}
