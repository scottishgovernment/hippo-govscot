package scot.gov.publishing.hippo.funnelback.component;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.ContainerConfiguration;
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
import scot.gov.publishing.hippo.funnelback.component.postprocess.*;

import scot.gov.publishing.hippo.funnelback.model.*;
import scot.gov.publishing.hippo.hst.request.UserTypeValve;
import scot.gov.publishing.hippo.search.PaginationBuilder;
import scot.gov.publishing.hippo.search.SearchService;
import scot.gov.publishing.hippo.search.SearchSettings;
import scot.gov.publishing.hippo.search.model.*;
import scot.gov.publishing.hippo.search.model.Question;
import scot.gov.publishing.hippo.search.model.Result;
import scot.gov.publishing.hippo.search.model.ResultsSummary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.*;

@Service
@Component("scot.gov.publishing.hippo.funnelback.component.FunnelbackService")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FunnelbackSearchService implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(FunnelbackSearchService.class);

    private static final String INTERNAL = "internal";

    private static final String GOVSCOT = "govscot";

    private static final String PERSON = "Person";

    private static final String FEATURED_ROLE = "Featured role";

    private static final CuratorPostProcessor CURATOR_POST_PROCESSOR = new CuratorPostProcessor();

    private static final RelativeImagesPostProcessor RELATIVE_IMAGES_POST_PROCESSOR = new RelativeImagesPostProcessor();

    private static final DatePostProcessor DATE_POST_PROCESSOR = new DatePostProcessor();

    private static final DefaultsPostProcessor DEFAULTS_POST_PROCESSOR = new DefaultsPostProcessor();

    private static final RelatedSearchLogger RELATED_SEARCH_LOGGER = new RelatedSearchLogger();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("ddMMMyyyy");

    private static final Set<String> IMAGE_TYPES = Set.of("News", FEATURED_ROLE, "Role");

    // on gov we suppress the label for some types
    private static final Set<String> NO_LABEL_TYPES = Set.of(FEATURED_ROLE, "Role", PERSON);

    private static final Set<String> NO_DATE_TYPES = Set.of("", "Group", "Directorate", "Policy", "Role", FEATURED_ROLE, PERSON, "Collection");

    String urlTemplate;

    String suggestTemplate;

    String tokenProperty;

    String resourceResolver;

    private Map<String, String> aliasesBySite;

    private Map<String, String> collections;

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getSuggestTemplate() {
        return suggestTemplate;
    }

    public void setSuggestTemplate(String suggestTemplate) {
        this.suggestTemplate = suggestTemplate;
    }

    public String getTokenProperty() {
        return tokenProperty;
    }

    public void setTokenProperty(String tokenProperty) {
        this.tokenProperty = tokenProperty;
    }

    public String getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(String resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public SearchResponse performSearch(Search search, SearchSettings searchsettings) {
        try {
            return doPerformSearch(search, searchsettings);
        } catch (ResourceException e) {
            LOG.error("performSearch failed {}", search.getQuery(), e);
            throw e;
        } catch (Throwable t) {
            LOG.error("performSearch failed {}", search.getQuery(), t);
            throw t;
        }
    }

    /**
     * Used to send a scheduled ping request to funnelback. This is scheduled in spring code, see
     * /site/components/src/main/resources/META-INF/hst-assembly/overrides/spring-managed-components.xml
     */
    void ping(String mount) {
        if (!HstServices.isAvailable()) {
            return;
        }

        if (!isFunnelbackTokenAvailable()) {
            return;
        }

        String query = "funnelback-ping-" + RandomStringUtils.randomAlphabetic(4);
        ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker(HstServices.getComponentManager());
        if (broker != null) {
            Resource results = broker.resolve(resourceResolver, suggestTemplate, suggestionsParamMap(query, mount));
            ResourceBeanMapper resourceBeanMapper = broker.getResourceBeanMapper(resourceResolver);
            resourceBeanMapper.mapCollection(results.getChildren(), Suggestion.class);
        }
    }

    public boolean isFunnelbackTokenAvailable() {
        String token = HstServices.getComponentManager().getContainerConfiguration().getString(tokenProperty);
        return isNotBlank(token);
    }

    SearchResponse doPerformSearch(Search search, SearchSettings searchsettings) {
        Map<String, Object> params = searchParamMap(search);
        ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker(HstServices.getComponentManager());
        Resource results = broker.resolve(resourceResolver, getUrlTemplate(search), params);
        ResourceBeanMapper resourceBeanMapper = broker.getResourceBeanMapper(resourceResolver);
        FunnelbackSearchResponse response = resourceBeanMapper.map(results, FunnelbackSearchResponse.class);
        postProcessSearchresponse(search, response);

        return convert(search, response);
    }

    SearchResponse convert(Search search, FunnelbackSearchResponse fbresponse) {
        SearchResponse response = new SearchResponse();
        response.getQuestion().setOriginalQuery(search.getQuery());
        response.getQuestion().setQuery(search.getQuery());
        response.setType(SearchResponse.Type.FUNNELBACK);
        response.setQueryHighlightRegex(fbresponse.getResponse().getResultPacket().getQueryHighlightRegex());
        response.setSupplementaryQueries(fbresponse.getResponse().getResultPacket().getQsups().stream().map(this::convert).collect(toList()));
        response.setResultsSummary(resultsSummary(fbresponse));
        response.setPagination(new PaginationBuilder().getPagination(response.getResultsSummary(), search));
        response.setQuestion(question(fbresponse));
        response.setRelatedResults(convertRelated(fbresponse));
        response.setResults(fbresponse.getResponse().getResultPacket().getResults().stream().map(this::toResult).collect(toList()));
        response.setHtmlMessages(fbresponse.getResponse().getCurator().getSimpleHtmlExhibits().stream().map(Exhibit::getMessageHtml).collect(toList()));
        response.setAdverts(fbresponse.getResponse().getCurator().getAdvertExhibits().stream().map(this::convert).collect(toList()));
        response.setHasResults(!response.getResults().isEmpty() || !response.getAdverts().isEmpty() || !response.getHtmlMessages().isEmpty());
        return response;
    }

    ResultsSummary resultsSummary(FunnelbackSearchResponse fbrespone) {
        ResultsSummary summary = new ResultsSummary();
        summary.setCurrStart(fbrespone.getResponse().getResultPacket().getResultsSummary().getCurrStart());
        summary.setCurrEnd(fbrespone.getResponse().getResultPacket().getResultsSummary().getCurrEnd());
        summary.setNumRanks(fbrespone.getResponse().getResultPacket().getResultsSummary().getNumRanks());
        summary.setTotalMatching(fbrespone.getResponse().getResultPacket().getResultsSummary().getTotalMatching());
        return summary;
    }

    PromotedResult convert(Exhibit exhibit) {
        PromotedResult promotedResult = new PromotedResult();
        promotedResult.setTitleHtml(exhibit.getTitleHtml());
        promotedResult.setMessageHtml(exhibit.getMessageHtml());
        promotedResult.setCategory(exhibit.getCategory());
        promotedResult.setDisplayUrl(exhibit.getDisplayUrl());
        promotedResult.setLinkUrl(exhibit.getLinkUrl());
        promotedResult.setDescriptionHtml(exhibit.getDescriptionHtml());
        return promotedResult;
    }

    Result toResult(scot.gov.publishing.hippo.funnelback.model.Result fbresult) {
        Result result = new Result();
        String type = type(fbresult);
        result.setLabel(label(type));
        result.setSummary(summary(fbresult));
        if (equalsAny(type, "Role", FEATURED_ROLE)) {
            result.setSubtitle(first(fbresult.getListMetadata().getPersonName()));
        }
        if (equalsAny(type, PERSON)) {
            result.setSubtitle(first(fbresult.getListMetadata().getPersonRole()));
        }
        if (collections.containsKey(GOVSCOT) && "News".equals(type)) {
            result.setDisplayDate(fbresult.getListMetadata().getDisplayDateTime());
        } else if (!NO_DATE_TYPES.contains(type)) {
            result.setDisplayDate(fbresult.getListMetadata().getDisplayDate());
        }

        result.setLink(link(fbresult));
        setImages(type, fbresult, result);

        for (int i = 0; i < fbresult.getListMetadata().getTitleSeries().size(); i++) {
            String titleSeries = fbresult.getListMetadata().getTitleSeries().get(i);
            String titleSeriesLink = fbresult.getListMetadata().getTitleSeriesLink().get(i);
            result.getPartOf().add(link(titleSeries, titleSeriesLink));
        }
        for (int i = 0; i < fbresult.getListMetadata().getPublicationCollection().size(); i++) {
            String titleSeries = fbresult.getListMetadata().getPublicationCollection().get(i);
            String titleSeriesLink = fbresult.getListMetadata().getPublicationCollectionLink().get(i);
            result.getPartOf().add(link(titleSeries, titleSeriesLink));
        }
        return result;
    }

    void setImages(String type, scot.gov.publishing.hippo.funnelback.model.Result fbresult, Result result) {

        if (IMAGE_TYPES.contains(type) && !fbresult.getListMetadata().getImage().isEmpty()) {
            String prefix = collections.containsKey(GOVSCOT) ? GOVSCOT : "publishing";
            result.setImage(Image.createImage(fbresult.getListMetadata().getImage().get(0), prefix));
        }
    }

    Link link(String label, String url) {
        Link link = new Link();
        link.setLabel(label);
        link.setUrl(url);
        return link;
    }

    String first(List<String> vals) {
        return vals.isEmpty() ? "" : vals.get(0);
    }

    Link link(scot.gov.publishing.hippo.funnelback.model.Result fbresult) {
        Link link = new Link();
        link.setUrl(fbresult.getLiveUrl());
        link.setLabel(title(fbresult));
        return link;
    }

    String type(scot.gov.publishing.hippo.funnelback.model.Result fbresult) {
        String format = first(fbresult.getListMetadata().getF(), "");
        if ("Publication".equals(format)) {
            return first(fbresult.getListMetadata().getPublicationType(), "Publication");
        }
        return format;
    }

    String label(String type) {
        return NO_LABEL_TYPES.contains(type) ? "" : type;
    }

    String first(List<String> values, String defaultValue) {
        return values.isEmpty() ? defaultValue : values.get(0);
    }

    String title(scot.gov.publishing.hippo.funnelback.model.Result fbresult) {
        String dcTitle = fbresult.getListMetadata().getDcTitle().isEmpty()
                ? ""
                : fbresult.getListMetadata().getDcTitle().get(0);
        String t = fbresult.getListMetadata().getT().isEmpty()
                ? ""
                : fbresult.getListMetadata().getT().get(0);
        return StringUtils.firstNonBlank(dcTitle, t);
    }

    String summary(scot.gov.publishing.hippo.funnelback.model.Result fbresult) {
        return fbresult.getListMetadata().getC().isEmpty()
                ? ""
                : fbresult.getListMetadata().getC().get(0);
    }

    Link toLink(ContextualNavigationCluster cluster) {
        Link link = new Link();
        link.setLabel(cluster.getLabel());
        link.setUrl(cluster.getQuery());
        return link;
    }

    Question question(FunnelbackSearchResponse in) {
        Question q = new Question();
        q.setQuery(in.getQuestion().getQuery());
        q.setOriginalQuery(in.getQuestion().getOriginalQuery());
        return q;
    }

    int getRank(int page) {
        return ((page - 1) * 10) + 1;
    }

    List<Link> convertRelated(FunnelbackSearchResponse fbresponse) {
        if (fbresponse.getResponse().getResultPacket().getContextualNavigation() == null) {
            return Collections.emptyList();
        }

        return fbresponse.getResponse().getResultPacket().getContextualNavigation().getCategories().stream()
                .flatMap(category -> category.getClusters().stream())
                .map(this::toLink)
                .collect(toList());
    }

    SupplementaryQuery convert(QSup qSup) {
        SupplementaryQuery supplementaryQuery = new SupplementaryQuery();
        supplementaryQuery.setQuery(qSup.getQuery());
        supplementaryQuery.setQsupSuppressedQuery(qSup.getQsupSuppressedQuery());
        supplementaryQuery.setSpellSugestionQuery(qSup.getSpellSugestionQuery());
        supplementaryQuery.setSrc(qSup.getSrc());
        supplementaryQuery.setUrl(qSup.getUrl());
        return supplementaryQuery;
    }

    @Override
    public List<String> getSuggestions(String partialQuery, String mount, SearchSettings searchSettings) {
        try {
            return doGetSuggestions(partialQuery, mount);
        } catch (ResourceException e) {
            LOG.error("getSuggestions failed", e);
            return Collections.emptyList();
        }
    }

    List<String> doGetSuggestions(String partialQuery, String mount) {
        Map<String, Object> params = suggestionsParamMap(partialQuery, mount);
        ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker(HstServices.getComponentManager());
        Resource results = broker.findResources(resourceResolver, suggestTemplate, params);
        ResourceBeanMapper resourceBeanMapper = broker.getResourceBeanMapper(resourceResolver);
        Collection<Suggestion> suggestions = resourceBeanMapper.mapCollection(results.getChildren(), Suggestion.class);
        return suggestions.stream().map(Suggestion::getDisp).collect(toList());
    }

    String getUrlTemplate(Search search) {

        List<String> params = new ArrayList<>();
        if (!search.isEnableSuplimentaryQueries()) {
            params.add("qsup=off");
        }

        if (usePreview(search.getRequest())) {
            params.add("profile=search_preview");
        } else {
            params.add("profile=search");
        }

        if (search.getFromDate() != null || search.getToDate() != null) {
            params.add(dateRangeParam(search));
        }

        if (search.getSort() != null && search.getSort() != Sort.RELEVANCE) {
            params.add(sortParam(search.getSort()));
        }

        if (!search.getTopics().isEmpty()) {
            search.getTopics().values()
                    .stream()
                    .map(this::topicParam)
                    .forEach(params::add);
        }

        if (!search.getPublicationTypes().isEmpty()) {
            search.getPublicationTypes().values()
                    .stream()
                    .map(this::publicationTypeParam)
                    .forEach(params::add);
        }

        return params.isEmpty() ? urlTemplate : urlTemplate + "&" + params.stream().collect(joining("&"));
    }

    String publicationTypeParam(String publicationType) {
        return "f.Content type|publicationType=" + publicationType;
    }


    String dateRangeParam(Search search) {
        // the date range param looks like:
        // f.Date|d=d>1Feb2023<3Feb2023
        StringBuilder dateParam = new StringBuilder("f.Date|d=d");
        if (search.getFromDate() != null) {
            LocalDate fromMinusOneDay = search.getFromDate().minusDays(1);
            dateParam.append(">").append(DATE_TIME_FORMATTER.format(fromMinusOneDay));
        }
        if (search.getToDate() != null) {
            LocalDate toPlusOneDaye = search.getToDate().plusDays(1);
            dateParam.append("<").append(DATE_TIME_FORMATTER.format(toPlusOneDaye));
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

    /**
     * Only pass on the _default_preview parameter for internal users. This version of the URL can then be used in
     * the marketing dashboard to allow users to preview changes.
     */
    boolean usePreview(HstRequest request) {
        String userType = userType(request);
        String previewParam = request.getParameter("profile");
        return INTERNAL.equals(userType) && "search_preview".equals(previewParam);
    }

    String userType(HstRequest request) {
        String headerUserType = request != null
                ? (String) request.getAttribute(UserTypeValve.USERTYPE_REQUEST_ATTR_NAME)
                : INTERNAL;
        return defaultString(headerUserType, INTERNAL);
    }

    void postProcessSearchresponse(Search search, FunnelbackSearchResponse response) {
        rewriteLinks(response, search.getRequest());
        removeDuplucateQSups(response);
        CURATOR_POST_PROCESSOR.process(response);
        RELATIVE_IMAGES_POST_PROCESSOR.process(response);
        DATE_POST_PROCESSOR.process(response);
        DEFAULTS_POST_PROCESSOR.process(response);
        new RelatedSearchesPostProcessor(search).process(response);
        new QSupPostProcessor(search).process(response);
        RELATED_SEARCH_LOGGER.process(response);
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
        VirtualHost host = context.getVirtualHost();
        String hostGroupName = host.getHostGroupName();
        if (!useRewriter(hostGroupName)) {
            return;
        }

        VirtualHosts hosts = host.getVirtualHosts();
        // fqdn -> alias map can be reused across multiple requests
        Map<String, String> aliases = aliasesBySite(hosts);

        // alias -> fqdn map is environment specific and depends on host in current request
        Map<String, String> sitesByAlias = hosts.getMountsByHostGroup(hostGroupName).stream()
                .filter(m -> Objects.nonNull(m.getAlias()))
                .collect(toMap(Mount::getAlias, m -> m.getVirtualHost().getHostName()));

        // prefer host name in current request if it is an alias for a site
        Mount mount = context.getResolvedMount().getMount();
        String publishingAlias = mount.getProperty("publishing:alias");
        if (publishingAlias != null) {
            String type = mount.getType();
            Mount aliased = hosts.getMountByGroupAliasAndType(hostGroupName, publishingAlias, type);
            sitesByAlias.put(aliased.getAlias(), mount.getVirtualHost().getHostName());
        }

        PostProcessor postProcessor = new ResultLinkRewriter(sitesByAlias, aliases);
        postProcessor.process(response);
    }

    synchronized Map<String, String> aliasesBySite(VirtualHosts hosts) {
        if (aliasesBySite != null) {
            return aliasesBySite;
        }
        aliasesBySite = hosts.getHostGroupNames().stream()
                .map(hosts::getMountsByHostGroup)
                .flatMap(Collection::stream)
                .filter(m -> m.getMountPath().isEmpty())
                .filter(m -> "live".equals(m.getType()))
                .filter(m -> !"localhost".equals(m.getVirtualHost().getHostName()))
                .filter(m -> Objects.nonNull(m.getAlias()))
                .collect(toMap(m -> m.getVirtualHost().getHostName(), Mount::getAlias));
        LOG.debug("Aliases by site: {}", aliasesBySite);
        return aliasesBySite;
    }

    boolean useRewriter(String hostGroupName) {
        return !equalsAny(hostGroupName, "production", "dev-localhost", "www");
    }

    Map<String, Object> searchParamMap(Search search) {
        int rank = getRank(search.getPage());
        String mountname = mountName(search.getRequest().getRequestContext());
        String collection = collections.get(mountname);
        Map<String, Object> params = new HashMap<>();
        params.put("query", defaultIfBlank(search.getQuery(), ""));
        params.put("rank", rank);
        params.put("collection", collection);
        params.put("clientId", clientId());
        return params;
    }

    String clientId() {
        ContainerConfiguration containerConfiguration = HstServices.getComponentManager().getContainerConfiguration();
        return containerConfiguration.getString("squiz.clientId");
    }
    public static String mountName(HstRequestContext context) {
        return context.getResolvedMount().getMount().getHstSite().getName();
    }
    public Map<String, String> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, String> collections) {
        this.collections = collections;
    }

    Map<String, Object> suggestionsParamMap(String partialQuery, String mount) {
        Map<String, Object> params = new HashMap<>();
        String collection = collections.get(mount);
        params.put("partial_query", defaultIfBlank(partialQuery, ""));
        params.put("collection", collection);
        params.put("clientId", clientId());
        params.put("show", 6);
        params.put("sort", 0);
        params.put("fmt", "json++");
        return params;
    }

}
