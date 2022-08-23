package scot.gov.publishing.hippo.funnelback.component;

public interface SearchService {

    SearchResponse performSearch(Search search, SearchSettings searchSettings);

}
