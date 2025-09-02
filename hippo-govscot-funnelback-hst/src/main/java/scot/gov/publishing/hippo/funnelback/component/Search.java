package scot.gov.publishing.hippo.funnelback.component;

import org.hippoecm.hst.core.component.HstRequest;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulate the params needed to perform a search.
 */
public class Search {
    private String query = "";

    private LocalDate fromDate;

    private LocalDate toDate;

    private Sort sort = null;

    private Map<String, String> topics = new HashMap<>();

    private Map<String, String> publicationTypes = new HashMap<>();

    private Map<String, String> languages = new HashMap<>();

    private int page;

    private boolean enableSuplimentaryQueries = true;

    private String requestUrl = "";

    private HstRequest request;

    public Search() {
    }

    public Search(Search search) {
        this.query = search.getQuery();
        this.fromDate = search.getFromDate();
        this.toDate = search.getToDate();
        this.sort = search.getSort();
        this.topics.putAll(search.getTopics());
        this.publicationTypes.putAll(search.getPublicationTypes());
        this.languages.putAll(search.getLanguages());
        this.page = search.getPage();
        this.enableSuplimentaryQueries = search.isEnableSuplimentaryQueries();
        this.requestUrl = search.getRequestUrl();
        this.request = search.getRequest();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public Map<String, String> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, String> topics) {
        this.topics = topics;
    }

    public Map<String, String> getPublicationTypes() {
        return publicationTypes;
    }

    public void setPublicationTypes(Map<String, String> publicationTypes) {
        this.publicationTypes = publicationTypes;
    }

    public Map<String, String> getLanguages() {
        return languages;
    }

    public void setLanguages(Map<String, String> languages) {
        this.languages = languages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean isEnableSuplimentaryQueries() {
        return enableSuplimentaryQueries;
    }

    public void setEnableSuplimentaryQueries(boolean enableSuplimentaryQueries) {
        this.enableSuplimentaryQueries = enableSuplimentaryQueries;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public HstRequest getRequest() {
        return request;
    }

    public void setRequest(HstRequest request) {
        this.request = request;
    }
}
