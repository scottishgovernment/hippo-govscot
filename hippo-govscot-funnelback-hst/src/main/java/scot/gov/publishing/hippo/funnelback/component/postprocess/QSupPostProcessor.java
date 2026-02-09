package scot.gov.publishing.hippo.funnelback.component.postprocess;

import scot.gov.publishing.search.SearchQueryBuilder;
import scot.gov.publishing.search.model.Search;
import scot.gov.publishing.search.SearchBuilder;
import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;
import scot.gov.publishing.hippo.funnelback.model.QSup;

public class QSupPostProcessor implements PostProcessor {

    private Search search;

    private SearchQueryBuilder queryBuilder = new SearchQueryBuilder();

    public QSupPostProcessor(Search search) {
        this.search = search;
    }

    @Override
    public void process(FunnelbackSearchResponse response) {
        response.getResponse().getResultPacket().getQsups().stream().forEach(this::rewrite);
    }

    void rewrite(QSup qsup) {
        // set query to be the original search but with qsup=false, do not include any filters or pagination in the query
        qsup.setQsupSuppressedQuery(queryBuilder.queryParamsSuppressQSup(searchWithoutFilters(search.getQuery())));

        // the url with the corrected term but with no filters or pagination
        qsup.setSpellSugestionQuery(queryBuilder.queryParams(searchWithoutFilters(qsup.getQuery())));
    }

    Search searchWithoutFilters(String query) {
         return new SearchBuilder().query(query).request(search.getRequest()).build();
    }
}