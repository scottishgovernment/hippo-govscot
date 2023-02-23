package scot.gov.publishing.hippo.funnelback.component;

import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.hst.module.CrispHstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import scot.gov.publishing.hippo.funnelback.component.postprocess.CuratorPostProcessor;
import scot.gov.publishing.hippo.funnelback.component.postprocess.PaginationBuilder;
import scot.gov.publishing.hippo.funnelback.component.postprocess.PostProcessor;
import scot.gov.publishing.hippo.funnelback.component.postprocess.ResultLinkRewriter;
import scot.gov.publishing.hippo.funnelback.model.*;
import scot.gov.publishing.hippo.hst.request.UserTypeValve;

import javax.jcr.RepositoryException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.*;

@Service
@Component("scot.gov.publishing.hippo.funnelback.component.FunnelbackService")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FunnelbackSearchService implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(FunnelbackSearchService.class);

    private static final String URL_TEMPLATE
            = "/search.json?query={query}&start_rank={rank}&collection={collection}";

    private static final String SUGGEST_URL
            = "/suggest.json?partial_query={partial_query}&show={show}&sort={sort}&fmt={fmt}&collection={collection}";

    private static final String FUNNELBACK_RESOURCE_SPACE = "funnelback";

    private static final CuratorPostProcessor CURATOR_POST_PROCESSOR = new CuratorPostProcessor();

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("ddMMMYYYY");

    private String collection;

    private List<String> sites;

    private Map<String, String> publicationTypeMappings = new HashMap<>();

    @Override
    public SearchResponse performSearch(Search search, SearchSettings searchsettings) {
        populatePublicationTypes(search);
        try {
            return  doPerformSearch(search, searchsettings);
        } catch (ResourceException e) {
            LOG.error("performSearch failed {}", search.getQuery(), e);
            throw e;
        }
    }

    void populatePublicationTypes(Search search) {

        if (!publicationTypeMappings.isEmpty()) {
            return;
        }

        try {
            publicationTypeMappings = ValueListUtil.toMap(search.getRequest(),
                    "/content/documents/govscot/valuelists/publicationTypes/publicationTypes");
        } catch (RepositoryException e) {
            LOG.error("Unexpected exception populating publication types");
        }
    }

    SearchResponse doPerformSearch(Search search, SearchSettings searchsettings) {
        int rank = getRank(search.getPage());
        Map<String, Object> params = searchParamMap(search.getQuery(), rank);
        ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker(HstServices.getComponentManager());
        String urlTemplate = getUrlTemplate(search);
        Resource results = broker.resolve(FUNNELBACK_RESOURCE_SPACE, urlTemplate, params);
        ResourceBeanMapper resourceBeanMapper = broker.getResourceBeanMapper(FUNNELBACK_RESOURCE_SPACE);
        FunnelbackSearchResponse response = resourceBeanMapper.map(results, FunnelbackSearchResponse.class);
        postProcessSearchresponse(search, response);
        Pagination pagination = createPagination(search, response);
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setType(SearchResponse.Type.FUNNELBACK);
        searchResponse.setQuestion(response.getQuestion());
        searchResponse.setResponse(response.getResponse());
        searchResponse.setPagination(pagination);
        return searchResponse;
    }

    int getRank(int page) {
        return ((page - 1) * 10) + 1;
    }

    public List<String> getSuggestions(String partialQuery) {
        try {
            return doGetSuggestions(partialQuery);
        } catch (ResourceException e) {
            LOG.error("getSuggestions failed", e);
            return Collections.emptyList();
        }
    }

    List<String> doGetSuggestions(String partialQuery) {
        Map<String, Object> params = suggestionsParamMap(partialQuery);
        ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker(HstServices.getComponentManager());
        Resource results = broker.findResources(FUNNELBACK_RESOURCE_SPACE, SUGGEST_URL, params);
        ResourceBeanMapper resourceBeanMapper = broker.getResourceBeanMapper(FUNNELBACK_RESOURCE_SPACE);
        Collection<Suggestion> suggestions = resourceBeanMapper.mapCollection(results.getChildren(), Suggestion.class);
        return suggestions.stream().map(Suggestion::getDisp).collect(toList());
    }

    String getUrlTemplate(Search search) {

        List<String> params = new ArrayList<>();
        if (search.isEnableSuplimentaryQueries()) {
            params.add("sup=off");
        }

        if (usePreview(search.getRequest())) {
            params.add("profile=_default_preview");
        }

        if (search.getTab() != null) {
            params.add(tabParam(search.getTab()));
        }

        if (search.getFromDate() != null || search.getToDate() != null) {
            params.add(dateRangeParam(search));
        }

        if (search.getSort() != Sort.RELEVANCE) {
            params.add(sortParam(search.getSort()));
        }

        if (!search.getTopics().isEmpty()) {
            search.getTopics()
                    .stream()
                    .map(this::topicParam)
                    .forEach(params::add);
        }

        if (!search.getPublicationTypes().isEmpty()) {
            search.getPublicationTypes()
                    .stream()
                    .filter(publicationTypeMappings::containsKey)
                    .map(publicationTypeMappings::get)
                    .map(this::publicationTypeParam)
                    .forEach(params::add);
        }

        return params.isEmpty() ? URL_TEMPLATE : URL_TEMPLATE + "&" + params.stream().collect(Collectors.joining("&"));
    }

    boolean usePreview(HstRequest request) {
        String userType = userType(request);
        String previewParam = request.getParameter("profile");
        return "internal".equals(userType) && "_default_preview".equals(previewParam);
    }

    String userType(HstRequest request) {
        String headerUserType = (String) request.getAttribute(UserTypeValve.USERTYPE_REQUEST_ATTR_NAME);
        return defaultString(headerUserType, "internal");
    }

    String publicationTypeParam(String publicationType) {
        return "f.Publication type|publicationType=" + publicationType;
    }

    String tabParam(SearchTab searchTab) {
        switch (searchTab) {
            case NEWS:
                return "f.Tabs|govscot~ds-news-push=News";

            case PUBLICATIONS:
                return "f.Tabs|govscot~ds-foi-eir-releases-push,govscot~ds-publications-push,govscot~ds-statistics-research-push=Publications";

            default:
                return "";
        }
    }

    String dateRangeParam(Search search) {
        // the date range param looks like:
        // f.Date|d=d>1Feb2023<3Feb2023
        StringBuilder dateParam = new StringBuilder("f.Date|d=d");
        if (search.getFromDate() != null) {
            LocalDate from = search.getFromDate().minusDays(1);
            dateParam.append(">").append(from.format(dateFormatter));
        }
        if (search.getToDate() != null) {
            LocalDate to = search.getToDate().plusDays(1);
            dateParam.append("<").append(to.format(dateFormatter));
        }
        return dateParam.toString();
    }

    String sortParam(Sort sort) {
        switch (sort) {
            case DATE:
                return "sort=date";

            case ADATE:
                return "sort=adate";

            default:
                return "";
        }
    }

    String topicParam(String topic) {
        return "f.Topic|topics=" + topic;
    }

    Pagination createPagination(Search search, FunnelbackSearchResponse response) {
        ResultsSummary summary = response.getResponse().getResultPacket().getResultsSummary();
        return new PaginationBuilder(search.getRequestUrl()).getPagination(summary, search);
    }

    void postProcessSearchresponse(Search search, FunnelbackSearchResponse response) {
        rewriteLinks(response, search.getRequest());
        removeDuplucateQSups(response);
        extractSimpleHtmlMessagesFromCurator(response);
    }

    void extractSimpleHtmlMessagesFromCurator(FunnelbackSearchResponse response) {
        CURATOR_POST_PROCESSOR.process(response);
    }

    void removeDuplucateQSups(FunnelbackSearchResponse response) {
        ResultPacket resultPacket = response.getResponse().getResultPacket();
        String query = resultPacket.getQuery();
        List<QSup> filteredQsups = resultPacket.getQsups()
                .stream()
                .filter(qsup -> !qsup.getQuery().equals(query))
                .collect(toList());
        resultPacket.setQsups(filteredQsups);
    }

    void rewriteLinks(FunnelbackSearchResponse response, HstRequest request) {
        HstRequestContext context = request.getRequestContext();
        VirtualHost virtualHost = context.getResolvedMount().getMount().getVirtualHost();
        String hostGroupName = virtualHost.getHostGroupName();
        if (useRewriter(hostGroupName)) {
            PostProcessor postProcessor = new ResultLinkRewriter(virtualHost.getName(), sites);
            postProcessor.process(response);
        }
    }

    boolean useRewriter(String hostGroupName) {
        return !equalsAny(hostGroupName, "production", "dev-localhost");
    }

    Map<String, Object> searchParamMap(String query, int rank) {

        Map<String, Object> params = new HashMap<>();
        params.put("query", defaultIfBlank(query, ""));
        params.put("rank", rank);
        params.put("collection", collection);
        return params;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public List<String> getSites() {
        return sites;
    }

    public void setSites(List<String> sites) {
        this.sites = sites;
    }

    Map<String, Object> suggestionsParamMap(String partialQuery) {
        Map<String, Object> params = new HashMap<>();
        params.put("partial_query", defaultIfBlank(partialQuery, ""));
        params.put("collection", collection);
        params.put("show", 6);
        params.put("sort", 0);
        params.put("fmt", "json++");
        return params;
    }

}