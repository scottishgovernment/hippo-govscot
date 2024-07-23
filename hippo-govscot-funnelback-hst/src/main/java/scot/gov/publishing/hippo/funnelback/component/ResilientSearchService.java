package scot.gov.publishing.hippo.funnelback.component;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

@Service
@Component("scot.gov.publishing.hippo.funnelback.component.ResilientSearchService")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ResilientSearchService implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(ResilientSearchService.class);

    public static final HystrixCommandGroupKey FUNNELBACK_COMMAND_GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("Funnelback");

    static final HystrixCommandKey FUNNELBACK_SEARCH_COMMAND_KEY = HystrixCommandKey.Factory.asKey("searchWithTimeout");

    static final HystrixCommandKey FUNNELBACK_SUGGESTIONS_COMMAND_KEY = HystrixCommandKey.Factory.asKey("suggestionsWithTimeout");

    private SearchService funnelbackSearchService;

    private SearchService bloomreachSearchService;

    Supplier<Double> randomNumberSource = () -> Math.random();

    private static Boolean hystrixPropertiesStrategySet = false;

    public ResilientSearchService() {
        ensueHystrixPropertiesStrategy();
    }

    @Override
    public SearchResponse performSearch(Search search, SearchSettings searchsettings) {
        int timeoutMillis = (int) searchsettings.getTimeoutMillis();
        HystrixCommandProperties.Setter properties = searchProperties(timeoutMillis);
        SearchCommand command = new SearchCommand(search, searchsettings, properties);
        return command.execute();
    }

    @Override
    public List<String> getSuggestions(String query, String mount, SearchSettings searchsettings) {
        int timeoutMillis = (int) searchsettings.getSugestTimeoutMillis();
        HystrixCommandProperties.Setter properties = searchProperties(timeoutMillis);
        LOG.info("getSuggestions {}, {}", query, timeoutMillis);
        SuggestionsCommand command = new SuggestionsCommand(query, mount, searchsettings, properties);
        return command.execute();
    }

    public static void ensueHystrixPropertiesStrategy() {
        synchronized (hystrixPropertiesStrategySet) {
            if (!hystrixPropertiesStrategySet.booleanValue()) {
                hystrixPropertiesStrategySet = true;
                HystrixPropertiesStrategy newStrategy = new HystrixPropertiesStrategyWithReloadableCache();
                HystrixPlugins.getInstance().registerPropertiesStrategy(newStrategy);
            }
        }
    }

    HystrixCommandProperties.Setter searchProperties(int timeoutMillis) {
        return HystrixCommandProperties.Setter()
                .withExecutionTimeoutInMilliseconds(timeoutMillis)

                // there are 10 buckets, so each bucket is 30 seconds
                .withMetricsRollingStatisticalWindowInMilliseconds((int) TimeUnit.MINUTES.toMillis(5))

                // wait 5 minute before retrying tripped circuit
                .withCircuitBreakerSleepWindowInMilliseconds((int) TimeUnit.SECONDS.toMillis(30))

                // do not trip the circuit unless we get at least 5 requests in the statistical window
                .withCircuitBreakerRequestVolumeThreshold(5)

                // 50 percent error rate will cause circuit to trip
                .withCircuitBreakerErrorThresholdPercentage(50);
    }

    public SearchService getFunnelbackSearchService() {
        return funnelbackSearchService;
    }

    public void setFunnelbackSearchService(FunnelbackSearchService funnelbackSearchService) {
        this.funnelbackSearchService = funnelbackSearchService;
    }

    public SearchService getBloomreachSearchService() {
        return bloomreachSearchService;
    }

    public void setBloomreachSearchService(SearchService bloomreachSearchService) {
        this.bloomreachSearchService = bloomreachSearchService;
    }

    class SearchCommand extends HystrixCommand<SearchResponse> {

        Search search;

        SearchSettings searchsettings;

        public SearchCommand(Search search, SearchSettings searchsettings, HystrixCommandProperties.Setter commandPropertiesSetter) {
            super(Setter
                    .withGroupKey(FUNNELBACK_COMMAND_GROUP_KEY)
                    .andCommandKey(FUNNELBACK_SEARCH_COMMAND_KEY)
                    .andCommandPropertiesDefaults(commandPropertiesSetter));
            this.search = search;
            this.searchsettings = searchsettings;
        }

        @Override
        protected SearchResponse run() {
            throwExceptionAtSpecifiedRate("funnelback", searchsettings.getFunnelbackErrorRate());
            return funnelbackSearchService.performSearch(search, searchsettings);
        }

        @Override
        protected SearchResponse getFallback() {
            throwExceptionAtSpecifiedRate("bloomreach", searchsettings.getBloomreachErrorRate());
            return bloomreachSearchService.performSearch(search, searchsettings);
        }
    }

    class SuggestionsCommand extends HystrixCommand<List<String>> {

        String query;

        String mount;

        SearchSettings searchsettings;

        public SuggestionsCommand(String query, String mount, SearchSettings searchsettings, HystrixCommandProperties.Setter commandPropertiesSetter) {
            super(Setter
                    .withGroupKey(FUNNELBACK_COMMAND_GROUP_KEY)
                    .andCommandKey(FUNNELBACK_SUGGESTIONS_COMMAND_KEY)
                    .andCommandPropertiesDefaults(commandPropertiesSetter));
            this.query = query;
            this.mount = mount;
            this.searchsettings = searchsettings;
        }

        @Override
        protected List<String> run() {
            throwExceptionAtSpecifiedRate("funnelback", searchsettings.getFunnelbackErrorRate());
            return funnelbackSearchService.getSuggestions(query, mount, searchsettings);
        }

        @Override
        protected List<String> getFallback() {
            return emptyList();
        }
    }

    /**
     * Used for testing error behaviour.  The rate can be set in the component in the sitemap.
     */
    void throwExceptionAtSpecifiedRate(String label, double rate) {

        if (rate == 0) {
            return;
        }

        double random = randomNumberSource.get();
        if (random < rate) {
            LOG.warn("Generating manufactured exception, label is {}, rate is {}", label, rate);
            throw new ManafacturedException(label);
        }
    }

    class ManafacturedException extends RuntimeException {
        public ManafacturedException(String msg) {
            super(msg);
        }
    }
}
