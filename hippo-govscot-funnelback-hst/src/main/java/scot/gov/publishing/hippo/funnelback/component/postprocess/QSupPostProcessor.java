package scot.gov.publishing.hippo.funnelback.component.postprocess;

import scot.gov.publishing.hippo.funnelback.component.Search;
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
        qsup.setUrl(queryBuilder.queryParams(search, search.getPage()));
    }
}