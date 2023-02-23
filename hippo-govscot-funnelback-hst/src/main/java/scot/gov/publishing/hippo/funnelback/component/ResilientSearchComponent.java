package scot.gov.publishing.hippo.funnelback.component;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.apache.commons.lang.StringUtils;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    private SearchTab searchTab;

    private static Boolean hystrixPropertiesStrategySet = false;

    @Override
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) {
        super.init(servletContext, componentConfig);

        searchType = componentConfig.getRawParameters().getOrDefault("searchtype", SEARCH_TYPE_RESILIENT);
        searchTab = searchTab(componentConfig);
        resilientSearchService = new ResilientSearchService();
        resilientSearchService.setFunnelbackSearchService(funnelbackSearchService);
        resilientSearchService.setBloomreachSearchService(bloomreachSearchService);
        ensueHystrixPropertiesStrategy();
    }

    SearchTab searchTab(ComponentConfiguration componentConfig) {
        String tabString = componentConfig.getRawParameters().get("tab");
        return tabString == null
                ? null
                : parseTabString(tabString);
    }

    private void ensueHystrixPropertiesStrategy() {
        // override the HystrixPropertiesStrategy so that we can set the timeout value at runtime from a value stored
        // in the repo without it being cached
        synchronized (hystrixPropertiesStrategySet) {
            if (!hystrixPropertiesStrategySet.booleanValue()) {
                hystrixPropertiesStrategySet = true;
                HystrixPropertiesStrategy newStrategy = new HystrixPropertiesStrategyWithReloadableCache();
                HystrixPlugins.getInstance().registerPropertiesStrategy(newStrategy);
            }
        }
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
        String qsup = getAnyParameter(request, "qsup");
        boolean qsupOff = "off".equals(qsup);

        return new SearchBuilder()
                .query(getAnyParameter(request, "q"))
                .tab(searchTab(request))
                .fromDate(date(request, "begin"))
                .toDate(date(request, "end"))
                .sort(sort(request))
                .topics(getAnyParameter(request, "topics"))
                .publicationTypes(getAnyParameter(request, "publicationTypes"))
                .page(getAnyIntParameter(request, "page", 1))
                .enableSuplimentaryQueries(qsupOff)
                .request(request)
                .build();
    }

    SearchTab searchTab(HstRequest request) {
        // if ths search tab is configured as a part of the component then always use that regardless of amy params
        if (searchTab != null) {
            return searchTab;
        }

        // since no tab is configured, allow the type param to control it
        String param = request.getParameter("type");
        return param != null ? parseTabString(param) : null;
    }

    SearchTab parseTabString(String tabString) {
        try {
            return SearchTab.valueOf(tabString);
        } catch (IllegalArgumentException e) {
            return searchTab;
        }
    }

    LocalDate date(HstRequest request, String dateParam) {
        String dateValue = getAnyParameter(request, dateParam);

        if (StringUtils.isBlank(dateValue)) {
            return null;
        }
        return LocalDate.parse(dateValue, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    Sort sort(HstRequest request) {
        String sortParam = request.getParameter("sort");
        if (isBlank(sortParam)) {
            return Sort.RELEVANCE;
        }

        try {
            return Sort.valueOf(sortParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid sort value {}, defaulting to relevance", sortParam);
            return Sort.RELEVANCE;
        }
    }

    public static SearchSettings searchSettings() {
        HippoBean bean = getSearchSettingsBean();
        SearchSettings searchsettings = new SearchSettings();
        if (bean != null) {
            LOG.info("Loaded search settings from {}", bean.getPath());
            searchsettings.setSearchType(bean.getSingleProperty("search:searchtype"));
            searchsettings.setEnabled(bean.getSingleProperty("search:enabled"));
            searchsettings.setTimeoutMillis(bean.getSingleProperty("search:timeoutMillis"));
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
