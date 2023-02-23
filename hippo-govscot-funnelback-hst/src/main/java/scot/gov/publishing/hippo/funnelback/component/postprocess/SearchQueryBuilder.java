package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.hippo.funnelback.component.Search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class SearchQueryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SearchQueryBuilder.class);

    String queryParams(Search search, int page) {

        List<String> params = new ArrayList<>();
        params.add(param("q", encodeParam(search.getQuery())));
        params.add(param("page", Integer.toString(page)));

        if (search.getSort() != null) {
            params.add(param("sort", search.getSort().toString().toLowerCase()));
        }

        if (search.getFromDate() != null) {
            params.add(param("from", dateString(search.getFromDate())));
        }

        if (search.getToDate() != null) {
            params.add(param("to", dateString(search.getToDate())));
        }

        if (!search.getPublicationTypes().isEmpty()) {
            params.add(param("publicationTypes", search.getPublicationTypes().stream().collect(joining(";"))));
        }

        if (!search.getTopics().isEmpty()) {
            String topicString = search.getTopics().stream().collect(joining(";"));
            params.add(param("topics", topicString));
        }

        if (search.getTab() != null) {
            params.add(param("type", search.getTab().name().toLowerCase()));
        }

        return params.stream().collect(Collectors.joining("&"));
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
