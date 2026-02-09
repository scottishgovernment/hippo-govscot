package scot.gov.publishing.hippo.funnelback.component.postprocess;

import scot.gov.publishing.search.SearchQueryBuilder;
import scot.gov.publishing.search.model.Search;
import scot.gov.publishing.hippo.funnelback.model.ContextualNavigationCategory;
import scot.gov.publishing.hippo.funnelback.model.ContextualNavigationCluster;
import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;

public class RelatedSearchesPostProcessor implements PostProcessor {

    private Search search;

    private SearchQueryBuilder queryBuilder = new SearchQueryBuilder();

    public RelatedSearchesPostProcessor(Search search) {
        this.search = search;
    }

    @Override
    public void process(FunnelbackSearchResponse response) {
        if (response.getResponse().getResultPacket().getContextualNavigation() != null) {
            response.getResponse().getResultPacket().getContextualNavigation().getCategories().forEach(this::rewrite);
        }
    }

    void rewrite(ContextualNavigationCategory category) {
        category.getClusters().stream().forEach(this::rewrite);
    }

    void rewrite(ContextualNavigationCluster cluster) {
        // we want to use the query as the label (funnelback label introduces ellipses)
        cluster.setLabel(cluster.getQuery());

        // rewrite the query to contain the query string including paging, filters etc. from the search
        Search searchCopy = new Search(search);
        searchCopy.setQuery("`" + cluster.getQuery() + "`");

        // this is a new search so it does not make sense to stay on the same page
        searchCopy.setPage(1);

        String queryparams = queryBuilder.queryParams(searchCopy);
        cluster.setQuery(queryparams);
    }
}