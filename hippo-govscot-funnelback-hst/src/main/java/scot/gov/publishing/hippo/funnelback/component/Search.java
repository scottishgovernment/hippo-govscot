package scot.gov.publishing.hippo.funnelback.component;

import org.hippoecm.hst.core.component.HstRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulate the params needed toDate perform a search.
 */
public class Search {
    private String query;

    private SearchTab tab;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Sort sort;

    private List<String> topics = new ArrayList<>();

    private List<String> publicationTypes = new ArrayList<>();

    private int page;

    private boolean enableSuplimentaryQueries;

    private String requestUrl;

    private HstRequest request;

    public SearchTab getTab() {
        return tab;
    }

    public void setTab(SearchTab tab) {
        this.tab = tab;
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

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<String> getPublicationTypes() {
        return publicationTypes;
    }

    public void setPublicationTypes(List<String> publicationTypes) {
        this.publicationTypes = publicationTypes;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
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
