package scot.gov.publishing.hippo.funnelback.component;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContext;

import static org.apache.commons.lang3.StringUtils.*;
import static scot.gov.publishing.hippo.funnelback.component.SearchResponse.blankSearchResponse;

@Service
@Component("scot.gov.publishing.hippo.funnelback.component.ResilientSearchComponent")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ResilientSearchComponent extends EssentialsContentComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ResilientSearchComponent.class);

    @Autowired
    private FunnelbackSearchService funnelbackSearchService;

    @Autowired
    private SearchService bloomreachSearchService;

    private ResilientSearchService resilientSearchService;

    private static final String SEARCH_TYPE_RESILIENT = "resilient";

    private static final String SEARCH_TYPE_FUNNELBACK = "funnelback";

    private static final String SEARCH_TYPE_BLOOMREACH = "bloomreach";

    // the type of search that this component is configured to provide in the sitemap
    private String searchType;

    @Override
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) {
        super.init(servletContext, componentConfig);

        searchType = componentConfig.getRawParameters().getOrDefault("searchtype", SEARCH_TYPE_RESILIENT);
        resilientSearchService = new ResilientSearchService();
        resilientSearchService.setFunnelbackSearchService(funnelbackSearchService);
        resilientSearchService.setBloomreachSearchService(bloomreachSearchService);
    }

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);

        SearchSettings searchsettings = searchSettings();
        LOG.info("doBeforeRender funnelbackErrorRate: {}, bloomreachErrorRate: {}",
                searchsettings.getFunnelbackErrorRate(), searchsettings.getBloomreachErrorRate());
        if (isEnabled(searchsettings) ) {
            Search search = search(request);

            String useSearchType = searchType(searchsettings);
            SearchService searchService = searchService(useSearchType);
            request.setAttribute("enabled", searchsettings.isEnabled());
            request.setAttribute("searchType", useSearchType);

            SearchResponse searchResponse =
                    isBlank(search.getQuery())
                    ? blankSearchResponse()
                    : searchService.performSearch(search, searchsettings);
            populateRequestAttributes(request, searchResponse, searchsettings);
        } else {
            request.setAttribute("enabled", false);
            request.setAttribute("autoCompleteEnabled", false);
        }
    }

    boolean isEnabled(SearchSettings searchsettings) {
        return  searchsettings != null && searchsettings.isEnabled();
    }

    Search search(HstRequest request) {
        String query = getAnyParameter(request, "q");
        String qsup = getAnyParameter(request, "qsup");
        int page = getAnyIntParameter(request, "page", 1);
        boolean qsupOff = "off".equals(qsup);
        return new SearchBuilder()
                .query(query)
                .page(page)
                .enableSuplimentaryQueries(qsupOff)
                .request(request).build();
    }

    public static SearchSettings searchSettings() {
        HippoBean bean = getSearchSettingsBean();
        SearchSettings searchsettings = new SearchSettings();
        if (bean != null) {
            searchsettings.setSearchType(bean.getSingleProperty("search:searchtype"));
            searchsettings.setEnabled(bean.getSingleProperty("search:enabled"));
            searchsettings.setTimeoutMillis(bean.getSingleProperty("search:timeoutMillis"));
            searchsettings.setSugestTimeoutMillis(bean.getSingleProperty("search:suggestTimeoutMillis", 300L));
            searchsettings.setBloomreachErrorRate(bean.getSingleProperty("search:bloomreachErrorRate", 0.0));
            searchsettings.setFunnelbackErrorRate(bean.getSingleProperty("search:funnelbackErrorRate", 0.0));
        } else {
            LOG.warn("unable to find search settings document");
        }

        return searchsettings;
    }

    /**
     * In publishing each site has its own search settings document, and for gov it lives under the root
     * administration folder
     */
    static HippoBean getSearchSettingsBean() {
        HippoBean siteBaseBean = RequestContextProvider.get().getSiteContentBaseBean();
        HippoBean searchSettingsBean = siteBaseBean.getBean("administration/search-settings");
        if (searchSettingsBean != null) {
            return searchSettingsBean;
        }

        HippoBean root = siteBaseBean.getParentBean();
        searchSettingsBean = root.getBean("administration/search-settings");
        return searchSettingsBean;
    }

    String searchType(SearchSettings searchsettings) {
        // if this component is configured to provide a 'resilient' search then the search type should be the one
        // specified in the searchsettings - this allows us to provide an override.
        return SEARCH_TYPE_RESILIENT.equals(searchType)
                ? searchsettings.getSearchType()
                : searchType;
    }

    SearchService searchService(String searchtype) {
        switch (searchtype) {
            case SEARCH_TYPE_FUNNELBACK: return funnelbackSearchService;
            case SEARCH_TYPE_BLOOMREACH: return bloomreachSearchService;
            case SEARCH_TYPE_RESILIENT :
            default:
                return resilientSearchService;
        }
    }

    boolean autoCompleteEnabled(SearchSettings searchSettings) {
        return searchSettings.isEnabled()
                && !equalsAny(SEARCH_TYPE_BLOOMREACH, this.searchType, searchSettings.getSearchType());
    }

    void populateRequestAttributes(HstRequest request, SearchResponse searchResponse, SearchSettings searchSettings) {
        request.setAttribute("queryString", defaultString(request.getQueryString()));
        request.setAttribute("searchType", searchResponse.getType().toString());
        request.setAttribute("question", searchResponse.getQuestion());
        request.setAttribute("response", searchResponse.getResponse());
        request.setAttribute("bloomreachresults", searchResponse.getBloomreachResults());
        request.setAttribute("pagination", searchResponse.getPagination());
        request.setAttribute("autoCompleteEnabled", autoCompleteEnabled(searchSettings));
    }
}
