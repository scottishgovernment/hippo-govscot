package scot.gov.publishing.hippo.funnelback.component;

import jakarta.servlet.ServletContext;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;
import org.onehippo.forge.selection.hst.contentbean.ValueList;
import org.onehippo.forge.selection.hst.util.SelectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Class.*;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.*;
import static scot.gov.publishing.hippo.funnelback.component.SearchResponse.blankSearchResponse;
import static scot.gov.publishing.hippo.funnelback.component.SearchSettings.*;

@Service
@Component("scot.gov.publishing.hippo.funnelback.component.ResilientSearchComponent")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ResilientSearchComponent extends EssentialsContentComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ResilientSearchComponent.class);

    @Autowired
    @Qualifier("funnelbackSearchService")
    private FunnelbackSearchService funnelbackSearchService;

    @Autowired
    @Qualifier("funnelbackSearchServiceDXP")
    private FunnelbackSearchService funnelbackSearchServiceDXP;

    @Autowired
    private SearchService bloomreachSearchService;

    private ResilientSearchService resilientSearchService;

    private static final String TOPICS = "topics";

    // the type of search that this component is configured to provide in the sitemap
    private String searchType;

    private Set<String> supportedParams = new HashSet<>();

    MapProvider topicsProvider = request -> emptyMap();
    MapProvider publicationTypesProvider = request -> emptyMap();

    @Override
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) {
        super.init(servletContext, componentConfig);

        searchType = componentConfig.getRawParameters().getOrDefault("searchtype", SEARCH_TYPE_RESILIENT);
        createProviders(componentConfig);
        resilientSearchService = new ResilientSearchService();
        resilientSearchService.setFunnelbackSearchService(funnelbackSearchService);
        resilientSearchService.setFunnelbackSearchServiceDXP(funnelbackSearchServiceDXP);
        resilientSearchService.setBloomreachSearchService(bloomreachSearchService);

        Collections.addAll(supportedParams,
            componentConfig.getRawParameters().getOrDefault("supportedparams", "q,qsup,page").split(","));
    }

    void createProviders(ComponentConfiguration componentConfig) {
        topicsProvider = createProvider(componentConfig, "topicsProvider");
        publicationTypesProvider = createProvider(componentConfig, "publicationTypesProvider");
    }

    MapProvider createProvider(ComponentConfiguration componentConfig, String param) {
        String topicProviderClass = componentConfig.getRawParameters().get(param);
        try {
            Class<?> clazz = Class.forName(topicProviderClass);
            return (MapProvider) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            LOG.error("Unable to create map provider {}", topicProviderClass, e);
            return request -> Map.of();
        }
    }

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);
        SearchSettings searchsettings = searchSettings();
        if (isEnabled(searchsettings)) {
            Search search = search(request);
            String useSearchType = searchType(searchsettings);
            request.setAttribute("enabled", searchsettings.isEnabled());
            request.setAttribute("showFilters", searchsettings.isShowFilters());
            request.setAttribute("searchType", useSearchType);

            SearchService searchService = searchService(useSearchType);
            SearchResponse searchResponse =
                    isBlank(search.getQuery())
                            ? blankSearchResponse()
                            : searchService.performSearch(search, searchsettings);
            populateRequestAttributes(request, search, searchResponse, searchsettings);
        } else {
            request.setAttribute("enabled", false);
            request.setAttribute("autoCompleteEnabled", false);
        }
    }

    boolean isEnabled(SearchSettings searchsettings) {
        return  searchsettings != null && searchsettings.isEnabled();
    }

    Search search(HstRequest request) {
        String query = getRequestParam(request, "q");
        String qsup = getRequestParam(request, "qsup");
        boolean qsupOff = "off".equals(qsup);
        int page = getAnyIntParameter(request, "page", 1);

        // we only want to use paramaters that are supported
        LocalDate begin = date(request, "begin");
        LocalDate end = date(request, "end");
        Sort sort = sort(request);
        SearchBuilder searchBuilder = new SearchBuilder()
                .query(query)
                .enableSuplimentaryQueries(!qsupOff)
                .page(page)
                .fromDate(begin)
                .toDate(end)
                .sort(sort)
                .request(request);
        addPublicationTypes(request, searchBuilder);
        addTopics(request, searchBuilder);
        return searchBuilder.build();
    }

    void addTopics(HstRequest request, SearchBuilder searchBuilder) {

        //  - topics: a ; separated list of topics
        //  - topic: multiple topic params can be supplied and each one will be added
        Map<String, String> topicsMap = topicsProvider.get(request.getRequestContext());
        searchBuilder.topics(getRequestParam(request, TOPICS), topicsMap);

        String [] topics = request.getParameterMap().get("topic");
        if (topics != null) {
            for (String topic : topics) {
                searchBuilder.topics(topic, topicsMap);
            }
        }
    }

    void addPublicationTypes(HstRequest request, SearchBuilder searchBuilder) {
        // we support type publication types paramaters:
        //  - publicationsTypes: a ; separated list of publications types
        //  - type: multiple type params can be supplied and each one will be added
        Map<String, String> typesMap = publicationTypesProvider.get(request.getRequestContext());
        String publicationTypes = getRequestParam(request, "publicationTypes");
        searchBuilder.publicationTypes(publicationTypes, typesMap);
        String [] types = request.getParameterMap().get("type");
        if (types != null) {
            for (String type : types) {
                searchBuilder.publicationTypes(type, typesMap);
            }
        }
    }

    public Map<String, String> publicationTypes(HstRequest request) {
        ValueList valueList = SelectionUtil.getValueListByIdentifier("publicationTypes", RequestContextProvider.get());
        if (valueList == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = SelectionUtil.valueListAsMap(valueList);
        map.put("news", "News");
        map.put("policy", "Policy");
        return map;
    }

    public Map<String, String> topics(HstRequest request) {
        HippoBean siteBean = request.getRequestContext().getSiteContentBaseBean();
        if (StringUtils.equals(siteBean.getName(), "govscot")) {
            return govscotTopics(request);
        }
         return topicsMap(request.getRequestContext());
    }

    Map<String, String> topicsMap(HstRequestContext context) {
        HippoBean baseBean = context.getSiteContentBaseBean();
        HippoBean topicsList = getTopicsList(baseBean);
        if (topicsList == null) {
            return emptyMap();
        }
        return SelectionUtil.valueListAsMap((ValueList) topicsList);
    }

    HippoBean getTopicsList(HippoBean baseBean) {
        HippoFolder administration = baseBean.getBean("administration");
        if (administration == null) {
            return null;
        }

        return administration.getBean(TOPICS);
    }

    Map<String, String> govscotTopics(HstRequest request) {
        HashMap<String, String> topics = new HashMap<>();

        try {
            HstRequestContext context = RequestContextProvider.get();
            Session session = context.getSession();
            if (session.nodeExists("/content/documents/govscot")) {
                HippoBean baseBean = context.getSiteContentBaseBean();
                HippoFolderBean topicsFolder = baseBean.getBean(TOPICS, HippoFolderBean.class);

                String xpath = String.format("//*[(@hippo:paths='%s') and (@hippo:availability='live') and not(@jcr:primaryType='nt:frozenNode') and ((@jcr:primaryType='govscot:Issue' or @jcr:primaryType='govscot:AboutTopic' or @jcr:primaryType='govscot:Topic' or @jcr:primaryType='govscot:DynamicIssue'))]", topicsFolder.getIdentifier());
                Query queryObj = session
                        .getWorkspace()
                        .getQueryManager()
                        .createQuery(xpath, Query.XPATH);
                QueryResult result = queryObj.execute();
                NodeIterator it = result.getNodes();
                while (it.hasNext()) {
                    Node topic = it.nextNode();
                    topics.put(topic.getName(), topic.getProperty("govscot:title").getString());
                }
            }
            return topics;
        } catch (RepositoryException e) {
            LOG.error("Failed to get list of topics and issues", e);
            return Collections.emptyMap();
        }
    }


    String getRequestParam(HstRequest request, String param) {
        return supportedParams.contains(param) ? getAnyParameter(request, param) : null;
    }

    LocalDate date(HstRequest request, String dateParam) {
        String dateValue = getRequestParam(request, dateParam);
        if (isBlank(dateValue)) {
            return null;
        }
        return LocalDate.parse(dateValue, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    Sort sort(HstRequest request) {
        String sortParam = getRequestParam(request,"sort");
        if (isBlank(sortParam)) {
            return null;
        }
        try {
            return Sort.valueOf(sortParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid sort value {}, defaulting to relevance", sortParam, e);
            return Sort.RELEVANCE;
        }
    }



    SearchService searchService(String searchtype) {
        switch (searchtype) {
            case SEARCH_TYPE_FUNNELBACK_DXP: return funnelbackSearchServiceDXP;
            case SEARCH_TYPE_FUNNELBACK: return funnelbackSearchService;
            case SEARCH_TYPE_BLOOMREACH: return bloomreachSearchService;
            case SEARCH_TYPE_RESILIENT :
            default:
                return resilientSearchService;
        }
    }

    public String searchType(SearchSettings searchsettings) {
        // if this component is configured to provide a 'resilient' search then the search type should be the one
        // specified in the searchsettings - this allows us to provide an override.
        return SEARCH_TYPE_RESILIENT.equals(searchType)
                ? searchsettings.getSearchType()
                : searchType;
    }

    boolean autoCompleteEnabled(SearchSettings searchSettings) {
        return searchSettings.isEnabled()
                && !equalsAny(SEARCH_TYPE_BLOOMREACH, this.searchType, searchSettings.getSearchType());
    }

    void populateRequestAttributes(HstRequest request, Search search, SearchResponse searchResponse, SearchSettings searchSettings) {
        request.setAttribute("search", search);
        request.setAttribute("queryString", defaultString(request.getQueryString()));
        request.setAttribute("searchType", searchResponse.getType().toString());
        request.setAttribute("question", searchResponse.getQuestion());
        request.setAttribute("response", searchResponse.getResponse());
        request.setAttribute("bloomreachresults", searchResponse.getBloomreachResults());
        request.setAttribute("pagination", searchResponse.getPagination());
        request.setAttribute("autoCompleteEnabled", autoCompleteEnabled(searchSettings));
        request.setAttribute("filterButtons", FilterButtonGroups.filterButtonGroups(search));
    }
}
