package scot.gov.publishing.hippo.funnelback.component;


import java.util.List;

public interface SearchService {

    SearchResponse performSearch(Search search, SearchSettings searchSettings);

    List<String> getSuggestions(String query, String mount, SearchSettings searchSettings);
}