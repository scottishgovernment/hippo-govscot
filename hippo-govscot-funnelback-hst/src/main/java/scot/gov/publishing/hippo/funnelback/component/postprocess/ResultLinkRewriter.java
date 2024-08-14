package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;
import scot.gov.publishing.hippo.funnelback.model.Result;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Rewrite result links so that they link to pages on this environment
 */
public class ResultLinkRewriter implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ResultLinkRewriter.class);

    private final Map<String, String> sitesByAlias;

    private final Map<String, String> aliasesBySite;

    public ResultLinkRewriter(Map<String, String> sitesByAlias, Map<String, String> aliasesBySite) {
        this.sitesByAlias = sitesByAlias;
        this.aliasesBySite = aliasesBySite;
    }

    @Override
    public void process(FunnelbackSearchResponse response) {
        List<Result> results = response.getResponse().getResultPacket().getResults();
        for (Result result : results) {
            String rewritten = rewrite(result.getLiveUrl());
            result.setLiveUrl(rewritten);
        }
    }

    private String rewrite(String url) {
        URI uri = URI.create(url);
        String alias = aliasesBySite.get(uri.getHost());
        if (alias == null) {
            return url;
        }
        String linkedHost = sitesByAlias.get(alias);
        if (linkedHost == null) {
            return url;
        }
        try {
            URI newURI = new URI(uri.getScheme(), linkedHost, uri.getPath(), uri.getQuery(), uri.getFragment());
            return newURI.toString();
        } catch (URISyntaxException e) {
            LOG.error("Unable to rewrite result links. url: {}, scheme: {}, host: {}, path: {}",
                    uri, uri.getScheme(), linkedHost, uri.getPath(), e);
            return uri.toString();
        }
    }

}
