package scot.gov.publishing.hippo.funnelback.component;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;

import javax.servlet.ServletContext;

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

        // each page has a designated search type, and then there are the search settings for the site.
        // the specific funnelback and bloomreach pages are for testing and sholdhave their auto complete
        // value hard coded.
        //
        // otherwise the value is determined from the search settings document
        switch (searchType) {
            case "bloomreach":
                request.setAttribute(AUTO_COMPLETE_ENABLED, false);
                break;
            case "funnelback":
                request.setAttribute(AUTO_COMPLETE_ENABLED, true);
                break;
            default:
                // defer to the type specific in search settings
                request.setAttribute(AUTO_COMPLETE_ENABLED, ResilientSearchComponent.searchSettings().isEnabled());
        }
    }

}