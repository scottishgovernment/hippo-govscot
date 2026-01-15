package scot.gov.publishing.hippo.funnelback;

import org.apache.commons.lang3.StringUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static scot.gov.publishing.hippo.funnelback.HippoUtils.findPublished;
import static scot.gov.publishing.hippo.funnelback.scheduler.PollFunnelbackCurator.FUNNELBACK;

public class SearchType {

    private SearchType() {
        // hide default constructor
    }

    public static String getSearchType(Session session) throws RepositoryException {
        Node handle = session.getNode("/content/documents/administration/search-settings");
        Node published = findPublished(handle);
        if (!published.getProperty("search:enabled").getBoolean()) {
            return "";
        }
        String searchType = published.getProperty("search:searchtype").getString();
        if (StringUtils.equalsAny(searchType, "funnelback-dxp", "resilient")) {
            return "funnelback-dxp";
        }

        if (StringUtils.equalsAny(searchType, FUNNELBACK, "resilient")) {
            return FUNNELBACK;
        }

        return searchType;
    }
}
