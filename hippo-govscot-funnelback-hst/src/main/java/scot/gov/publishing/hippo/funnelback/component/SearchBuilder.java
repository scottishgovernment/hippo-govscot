package scot.gov.publishing.hippo.funnelback.component;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.split;

public class SearchBuilder {

    private String query = "";

    private SearchTab tab;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Sort sort = Sort.RELEVANCE;

    private List<String> topics = new ArrayList<>();

    private List<String> publicationTypes = new ArrayList<>();

    private int page = 1;

    private boolean enableSuplimentaryQueries = true;

    private String requestUrl;

    private HstRequest request;

    public SearchBuilder query(String query) {
        this.query = query;
        return this;
    }

    public SearchBuilder page(int page) {
        this.page = page;
        return this;
    }

    public SearchBuilder tab(SearchTab tab) {
        this.tab = tab;
        return this;
    }

    public SearchBuilder fromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public SearchBuilder toDate(LocalDate toDate) {
        this.toDate = toDate;
        return this;
    }

    public SearchBuilder sort(Sort sort) {
        this.sort = sort;
        return this;
    }

    public SearchBuilder topics(String topicsString) {
        if (isNotBlank(topicsString)) {
            this.topics = asList(split(topicsString, ";"));
        }
        return this;
    }

    public SearchBuilder publicationTypes(String publicationTypesString) {
        if (isNotBlank(publicationTypesString)) {
            publicationTypes = asList(split(publicationTypesString, ";"));
        }
        return this;
    }

    public SearchBuilder enableSuplimentaryQueries(boolean enableSuplimentaryQueries) {
        this.enableSuplimentaryQueries = enableSuplimentaryQueries;
        return this;
    }

    public SearchBuilder request(HstRequest request) {
        this.request = request;
        this.requestUrl = requestUrl(request);
        return this;
    }

    private String requestUrl(HstRequest request) {
        HstRequestContext context = request.getRequestContext();
        HttpServletRequest servletRequest = context.getServletRequest();
        return HstRequestUtils.getExternalRequestUrl(servletRequest, false);
    }

    public Search build() {
        Search search = new Search();
        search.setQuery(query);
        search.setTab(tab);
        search.setFromDate(fromDate);
        search.setToDate(toDate);
        search.setSort(sort);
        search.setTopics(topics);
        search.setPublicationTypes(publicationTypes);
        search.setPage(page);
        search.setRequest(request);
        search.setRequestUrl(requestUrl);
        search.setEnableSuplimentaryQueries(enableSuplimentaryQueries);
        return search;
    }
}
