package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.junit.Test;
import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;
import scot.gov.publishing.hippo.funnelback.model.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by z418868 on 20/06/2022.
 */
public class ResultLinkRewriterTest {

    private final Map<String, String> sitesByAlias = new HashMap<>();

    private final Map<String, String> aliasesBySite = new HashMap<>();

    @Test
    public void rewritesDevToExp() {
        sitesByAlias.put("mygov", "exp.www.mygov.scot");
        aliasesBySite.put("dev.www.mygov.scot", "mygov");
        aliasesBySite.put("exp.www.mygov.scot", "mygov");
        ResultLinkRewriter sut = new ResultLinkRewriter(sitesByAlias, aliasesBySite);
        FunnelbackSearchResponse response = new FunnelbackSearchResponse();
        List<Result> results = new ArrayList<>();
        results.add(result("https://dev.www.mygov.scot/"));
        results.add(result("https://dev.www.mygov.scot/with/path"));
        results.add(result("https://dev.www.mygov.scot/with/path?param=value"));
        results.add(result("https://dev.www.mygov.scot/with/path?param=value#anchor"));
        results.add(result("https://exp.www.mygov.scot/"));
        response.getResponse().getResultPacket().setResults(results);

        // ACT
        sut.process(response);

        // ASSERT
        assertEquals("https://exp.www.mygov.scot/", response.getResponse().getResultPacket().getResults().get(0).getLiveUrl());
        assertEquals("https://exp.www.mygov.scot/with/path", response.getResponse().getResultPacket().getResults().get(1).getLiveUrl());
        assertEquals("https://exp.www.mygov.scot/with/path?param=value", response.getResponse().getResultPacket().getResults().get(2).getLiveUrl());
        assertEquals("https://exp.www.mygov.scot/with/path?param=value#anchor", response.getResponse().getResultPacket().getResults().get(3).getLiveUrl());
        assertEquals("https://exp.www.mygov.scot/", response.getResponse().getResultPacket().getResults().get(4).getLiveUrl());
    }
    
    @Test
    public void doesNotRewriteUrlsFromOtherSites() {
        sitesByAlias.put("mygov", "exp.www.mygov.scot");
        aliasesBySite.put("dev.www.mygov.scot", "mygov");
        aliasesBySite.put("exp.www.mygov.scot", "mygov");
        ResultLinkRewriter sut = new ResultLinkRewriter(sitesByAlias, aliasesBySite);
        FunnelbackSearchResponse response = new FunnelbackSearchResponse();
        List<Result> results = new ArrayList<>();
        results.add(result("https://digital.jobs.gov.scot/"));
        results.add(result("https://www.gov.scot/"));
        response.getResponse().getResultPacket().setResults(results);

        // ACT
        sut.process(response);

        // ASSERT
        assertEquals("https://digital.jobs.gov.scot/", response.getResponse().getResultPacket().getResults().get(0).getLiveUrl());
        assertEquals("https://www.gov.scot/", response.getResponse().getResultPacket().getResults().get(1).getLiveUrl());

    }

    Result result(String url) {
        Result result = new Result();
        result.setLiveUrl(url);
        result.setTitle(anyTitle());
        return result;
    }

    String anyTitle() {
        return "title";
    }

}
