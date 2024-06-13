package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.hippo.funnelback.component.Search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class SearchQueryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SearchQueryBuilder.class);

    private String queryParam = "q";

    public SearchQueryBuilder() {
        // nothing required, default tio using "q" as the query param
    }

    public SearchQueryBuilder(String queryParam) {
        this.queryParam = queryParam;
    }

    public String queryParams(Search search) {
        return toParamString(params(search));
    }

    public String queryParams(Search search, int page) {
        Search modifiedSearch = new Search(search);
        modifiedSearch.setPage(page);
        return toParamString(params(modifiedSearch));
    }

    public String queryParamsNoFromDate(Search search) {
        Search modifiedSearch = new Search(search);
        modifiedSearch.setFromDate(null);
        return toParamString(params(modifiedSearch));
    }

    public String queryParamsNoToDate(Search search) {
        Search modifiedSearch = new Search(search);
        modifiedSearch.setToDate(null);
        return toParamString(params(modifiedSearch));
    }

    public String queryParamsWithoutTopic(Search search, String excludeTopic) {
        Search modifiedSearch = new Search(search);
        modifiedSearch.getTopics().remove(excludeTopic);
        return toParamString(params(modifiedSearch));
    }

    public String queryParamsWithoutPublicationType(Search search, String excludeType) {
        Search modifiedSearch = new Search(search);
        modifiedSearch.getPublicationTypes().remove(excludeType);
        return toParamString(params(modifiedSearch));
    }

    String toParamString(List<String> params) {
        return params.stream().collect(joining("&"));
    }

    List<String> params(Search search) {
        List<String> params = new ArrayList<>();
        if (isNotBlank(search.getQuery())) {
            params.add(param(queryParam, encodeParam(search.getQuery())));
        }
        params.add(param("page", Integer.toString(search.getPage())));

        if (search.getSort() != null) {
            params.add(param("sort", search.getSort().toString().toLowerCase()));
        }

        if (search.getFromDate() != null) {
            params.add(param("begin", dateString(search.getFromDate())));
        }

        if (search.getToDate() != null) {
            params.add(param("end", dateString(search.getToDate())));
        }

        if (!search.getPublicationTypes().isEmpty()) {
            for (String key : search.getPublicationTypes().keySet()) {
                params.add(param("type", key));
            }
        }

        if (!search.getTopics().isEmpty()) {
            for (String key : search.getTopics().keySet()) {
                params.add(param("topic", key));
            }
        }

        String cat = search.getRequest().getParameter("cat");
        if (isNotBlank(cat)) {
            params.add(param("cat", cat));
        }
        return params;
    }

    String param(String name, String value) {
        return name + "=" + value;
    }

    String dateString(LocalDate date) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date);
    }

    String encodeParam(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to encode param: {}", param, e);
            return param;
        }

    }

}
