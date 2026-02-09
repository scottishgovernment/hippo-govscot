package scot.gov.publishing.search;

import jakarta.servlet.ServletContext;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;

import static scot.gov.publishing.search.SearchSettings.searchSettings;

/**
 * Component suitable for the base of a search page.
 */
public class SearchComponent extends EssentialsContentComponent {

    private static final String AUTO_COMPLETE_ENABLED = "autoCompleteEnabled";

    private String searchType;

    @Override
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) {
        super.init(servletContext, componentConfig);
        this.searchType = componentConfig.getRawParameters().getOrDefault("searchtype", "resilient");
    }

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);
        HippoBean bean = request.getRequestContext().getContentBean();
        // handle for gov ...
        request.setAttribute("displayFilters", bean.getSingleProperty("publishing:displayFilters", true));
        switch (searchType) {
            case "bloomreach":
                request.setAttribute(AUTO_COMPLETE_ENABLED, false);
                break;
            case "funnelback":
                request.setAttribute(AUTO_COMPLETE_ENABLED, true);
                break;
            default:
                // defer to the type specific in search settings
                request.setAttribute(AUTO_COMPLETE_ENABLED, searchSettings().isEnabled());
        }
    }

}