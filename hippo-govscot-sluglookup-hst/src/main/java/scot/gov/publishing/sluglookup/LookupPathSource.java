package scot.gov.publishing.sluglookup;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static scot.gov.publishing.sluglookup.SlugLookupPaths.slugLookupPath;

public class LookupPathSource implements PathForSlugSource {

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
            return null;
        }
        return lookupNode.getProperty("sluglookup:path").getString();
    }

}
