package scot.gov.publishing.searchjournal.funnelback;

import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.searchjournal.SiteContentFetcher;

public class FunnelbackFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FunnelbackFactory.class);

    private static final String ERROR_RATE_ATTRIBUTE = "errorRate";

    private static final String FILTERS_ATTRIBUTE = "filters";

    private static final String ACCOUNT_NAME_ATTRIBUTE = "accountName";

    private static final String POSITION_COLLECTION_ATTRIBUTE = "positionCollection";

    private static final String POSITION_KEY_ATTRIBUTE = "positionKey";

    private static final String LOCAL_SITE_URL_ATTRIBUTE = "localSiteUrl";

    private static final String SITE_BASE_URL_ATTRIBUTE = "siteBaseUrl";

    private static final String FORWARDED_HOST_ATTRIBUTE = "forwardedHost";

    private FunnelbackFactory() {
        // hide implicit constructor
    }

    public static Funnelback newFunnelback(RepositoryJobExecutionContext context) {

        String filters = filters(context);
        FunnelbackConfiguration funnelbackConfiguration = configuration(context);
        if (funnelbackConfiguration == null) {
            return null;
        }
        Funnelback funnelback = new FunnelbackImpl(funnelbackConfiguration, filters);

        LOG.info("filters: {}", filters);
        double errorRate = 0.0;
        if (context.getAttributeNames().contains(ERROR_RATE_ATTRIBUTE)) {
            String errorRateString = context.getAttribute(ERROR_RATE_ATTRIBUTE);
            errorRate = Double.parseDouble(errorRateString);
        }

        if (errorRate > 0.0) {
            LOG.warn("WARNING, using FlakyFunnelback to simulate errors.  Error rate is {}", errorRate);
            funnelback = new FlakyFunnelback(funnelback, errorRate);
        }

        return new MetricsCollectingFunnelbackImpl(funnelback);
    }

    public static SiteContentFetcher newFetcher(RepositoryJobExecutionContext context) {
        String localSiteUrl = context.getAttributeNames().contains(LOCAL_SITE_URL_ATTRIBUTE)
                ? context.getAttribute(LOCAL_SITE_URL_ATTRIBUTE) : "http://localhost:8080/site/";
        String siteBaseUrl = context.getAttributeNames().contains(SITE_BASE_URL_ATTRIBUTE)
                ? context.getAttribute(SITE_BASE_URL_ATTRIBUTE) : "https://www.gov.scot/";
        String forwardedHost = context.getAttributeNames().contains(FORWARDED_HOST_ATTRIBUTE)
                ? context.getAttribute(FORWARDED_HOST_ATTRIBUTE) : null;
        return new SiteContentFetcher(localSiteUrl, siteBaseUrl, forwardedHost);
    }

    static FunnelbackConfiguration configuration(RepositoryJobExecutionContext context) {

        FunnelbackConfiguration configuration = new FunnelbackConfiguration();
        ContainerConfiguration containerConfiguration = HstServices.getComponentManager().getContainerConfiguration();
        if (!containerConfiguration.containsKey("squiz.admin.token")) {
            return null;
        }
        configuration.setSearchType("funnelback-dxp");
        configuration.setAccountName(context.getAttribute(ACCOUNT_NAME_ATTRIBUTE));
        configuration.setPositionCollection(context.getAttribute(POSITION_COLLECTION_ATTRIBUTE));
        configuration.setPositionKey(context.getAttribute(POSITION_KEY_ATTRIBUTE));
        configuration.setApiUrl(containerConfiguration.getString("squiz.admin.url"));
        configuration.setClientId(containerConfiguration.getString("squiz.clientId"));
        configuration.setApiKey(containerConfiguration.getString("squiz.admin.token"));

        return configuration;
    }

    static String filters(RepositoryJobExecutionContext context) {
        return context.getAttributeNames().contains(FILTERS_ATTRIBUTE)
                ? context.getAttribute(FILTERS_ATTRIBUTE) : "";
    }
}
