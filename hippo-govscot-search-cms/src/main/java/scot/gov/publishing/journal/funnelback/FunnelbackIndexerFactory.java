package scot.gov.publishing.journal.funnelback;

import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunnelbackIndexerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FunnelbackIndexerFactory.class);

    private FunnelbackIndexerFactory() {
        // hide implicit constructor
    }

    public static FunnelbackIndexer newFunnelback(String positionCollection, String positionKey, double errorRate) {

        FunnelbackConfiguration funnelbackConfiguration = configuration();
        if (funnelbackConfiguration == null) {
            return null;
        }
        FunnelbackIndexingConfiguration indexingConfiguration =
                new FunnelbackIndexingConfiguration(positionCollection, positionKey);
        FunnelbackIndexer funnelback = new FunnelbackIndexerImpl(funnelbackConfiguration, indexingConfiguration);
        if (errorRate > 0.0) {
            LOG.warn("Using FlakyFunnelback to simulate errors.  Error rate is {}", errorRate);
            funnelback = new FlakyFunnelback(funnelback, errorRate);
        }

        return new MetricsCollectingFunnelbackImpl(funnelback);
    }

    static FunnelbackConfiguration configuration() {

        ContainerConfiguration containerConfiguration = HstServices.getComponentManager().getContainerConfiguration();
        if (!containerConfiguration.containsKey("squiz.admin.token")) {
            return null;
        }
        FunnelbackConfiguration configuration = new FunnelbackConfiguration(
                containerConfiguration.getString("squiz.admin.url"),
                containerConfiguration.getString("squiz.clientId"),
                containerConfiguration.getString("squiz.admin.token"));
        LOG.debug("{}", configuration);
        return configuration;
    }
}
