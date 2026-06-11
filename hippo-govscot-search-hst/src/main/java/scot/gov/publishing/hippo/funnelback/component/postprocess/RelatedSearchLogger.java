package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.hippo.funnelback.model.ContextualNavigation;
import scot.gov.publishing.hippo.funnelback.model.ContextualNavigationCategory;
import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;

import java.util.List;

/**
 * Temporary processor to log search reults that do not have related searches. This is aimed at accessing the impact of
 * https://sg-dtd.atlassian.net/browse/MGS-7692
 */
public class RelatedSearchLogger implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RelatedSearchLogger.class);

    @Override
    public void process(FunnelbackSearchResponse response) {
        ContextualNavigation contextualNavigation = response.getResponse().getResultPacket().getContextualNavigation();
        int relatedSearchCount = relatedSearchCount(contextualNavigation);
        LOG.info("{} related searches for {}", relatedSearchCount, response.getQuestion().getOriginalQuery());
    }

    int relatedSearchCount(ContextualNavigation contextualNavigation) {
        if (contextualNavigation == null) {
            return 0;
        }

        List<ContextualNavigationCategory> categories = contextualNavigation.getCategories();
        return categories.stream()
                .map(ContextualNavigationCategory::getClusters)
                .mapToInt(List::size)
                .sum();
    }
}
