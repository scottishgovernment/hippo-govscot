package scot.gov.publishing.hippo.funnelback.component;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.*;

public class SearchBuilder {

    private String query = "";

    private LocalDate fromDate;

    private LocalDate toDate;

    private Sort sort = null;

    private Map<String, String> topics = new HashMap<>();

    private Map<String, String> publicationTypes = new HashMap<>();

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

    public SearchBuilder topics(String topicsString, Map<String, String> topicsLookup) {
        // topics string is a ; separated list of either codes or strings
        if (isBlank(topicsString)) {
            return this;
        }
        String [] types = split(topicsString, ";");
        for (String type : types) {
            if (topicsLookup.containsKey(type)) {
                topics.put(type, topicsLookup.get(type));
            } else {
                // find the topic by its value and convert it to the key
                Optional<Map.Entry<String, String>> entry = topicsLookup.entrySet().stream()
                        .filter(e -> StringUtils.equals(e.getValue(), type))
                        .findFirst();
                if (entry.isPresent()) {
                    topics.put(entry.get().getKey(), type);
                }
            }
        }
        return this;
    }

    public SearchBuilder publicationTypes(String typesString, Map<String, String> typeLookups) {
        return this.publicationTypes(typesString, ";", typeLookups);
    }

    public SearchBuilder publicationTypes(String typesString, String separator, Map<String, String> typeLookups) {

        if (isNotBlank(typesString)) {
            String [] types = split(typesString, separator);
            for (String type : types) {
                if (typeLookups.containsKey(type)) {
                    publicationTypes.put(type, typeLookups.get(type));
                }
            }
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

    public boolean hasPublicationTypes() {
        return !publicationTypes.isEmpty();
    }

    public Search build() {
        Search search = new Search();
        search.setQuery(query);
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
