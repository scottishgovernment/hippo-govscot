package scot.gov.publishing.sluglookup;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static scot.gov.publishing.sluglookup.SlugLookupPaths.slugLookupPath;

public class LookupPathSource implements PathForSlugSource {

    private static final Logger LOG = LoggerFactory.getLogger(LookupPathSource.class);

    @Override
    public String get(String slug, String site, String type, String mount) throws RepositoryException {
        String lookupPath = lookupPath(slug, site, type, mount);
        LOG.info("lookupPath {}", lookupPath);
        if (lookupPath == null) {
            return null;
        }
        return  lookupPath;
    }

    String lookupPath(String slug, String site, String type, String mount) throws RepositoryException {
        HstRequestContext req = RequestContextProvider.get();
        Session session = req.getSession();
        String slugLookupPath = slugLookupPath(slug, site, type, mount);
        LOG.info("slugLookupPath {}", slugLookupPath);
        if (!session.nodeExists(slugLookupPath)) {
            LOG.error("slugLookupPath does not exist {}", slugLookupPath);
            return null;
        }
        Node lookupNode = session.getNode(slugLookupPath);
        if (!lookupNode.hasProperty("sluglookup:path")) {
            LOG.error("no path {}", lookupNode.getPath());
            return null;
        }
        return lookupNode.getProperty("sluglookup:path").getString();
    }

}
