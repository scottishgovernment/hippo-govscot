package scot.gov.publishing.hippo.funnelback.component.postprocess;

import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;
import scot.gov.publishing.hippo.funnelback.model.ListMetadata;
import scot.gov.publishing.hippo.funnelback.model.Result;

import static java.util.Collections.singletonList;

public class DefaultsPostProcessor implements PostProcessor {

    @Override
    public void process(FunnelbackSearchResponse response) {
        response.getResponse().getResultPacket().getResults().stream().forEach(this::addDefaults);
    }

    void addDefaults(Result result) {
        ListMetadata listMetadata = result.getListMetadata();
        // providing a default value of F makes the templates simpler since they do not have to reason about missing or null
        // values.
        if (listMetadata.getF() == null || listMetadata.getF().isEmpty()) {
            listMetadata.setF(singletonList(""));
        }
    }
}
