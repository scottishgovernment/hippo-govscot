package scot.gov.publishing.search;

import scot.gov.publishing.search.model.Search;
import scot.gov.publishing.search.model.SearchResponse;

import java.util.List;

public interface SearchService {

    SearchResponse performSearch(Search search, SearchSettings searchSettings);

    List<String> getSuggestions(String query, String mount, SearchSettings searchSettings);
}