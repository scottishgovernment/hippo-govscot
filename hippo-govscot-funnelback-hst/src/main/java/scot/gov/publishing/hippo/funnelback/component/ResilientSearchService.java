package scot.gov.publishing.hippo.funnelback.component;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Component("scot.gov.publishing.hippo.funnelback.component.ResilientSearchService")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ResilientSearchService implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(ResilientSearchService.class);

    public static final HystrixCommandGroupKey FUNNELBACK_COMMAND_GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("Funnelback");

    static final HystrixCommandKey FUNNELBACK_COMMAND_KEY = HystrixCommandKey.Factory.asKey("searchWithTimout");

    private SearchService funnelbackSearchService;

    private SearchService bloomreachSearchService;

    Supplier<Double> randomNumberSource = () -> Math.random();

    double bloomreachErrorRate = 0;

    double funnelbackErrorRate = 0;

    @Override
    public SearchResponse performSearch(Search search, SearchSettings searchsettings) {
        int timeoutMilis = (int) searchsettings.getTimeoutMillis();
        HystrixCommandProperties.Setter commandPropertiesSetter = HystrixCommandProperties.Setter()
                .withExecutionTimeoutInMilliseconds(timeoutMilis)

                // there are 10 buckets, so each bucket is 30 seconds
                .withMetricsRollingStatisticalWindowInMilliseconds((int) TimeUnit.MINUTES.toMillis(5))

                // wait 5 minute before retrying tripped circuit
                .withCircuitBreakerSleepWindowInMilliseconds((int) TimeUnit.SECONDS.toMillis(30))

                // do not trip the circuit unless we get at least 5 requests in the statistical window
                .withCircuitBreakerRequestVolumeThreshold(5)

                // 50 percent error rate will cause circuit to trip
                .withCircuitBreakerErrorThresholdPercentage(50);
        SearchCommand command = new SearchCommand(search, searchsettings, commandPropertiesSetter);
        return command.execute();
    }

    class SearchCommand extends HystrixCommand<SearchResponse> {

        Search search;

        SearchSettings searchsettings;

        public SearchCommand(Search search, SearchSettings searchsettings, HystrixCommandProperties.Setter commandPropertiesSetter) {
            super(Setter
                    .withGroupKey(FUNNELBACK_COMMAND_GROUP_KEY)
                    .andCommandKey(FUNNELBACK_COMMAND_KEY)
                    .andCommandPropertiesDefaults(commandPropertiesSetter));
            this.search = search;
            this.searchsettings = searchsettings;
        }

        @Override
        protected SearchResponse run() {
            throwExceptionAtSpecifiedRate("funnelback", funnelbackErrorRate);
            return funnelbackSearchService.performSearch(search, searchsettings);
        }

        @Override
        protected SearchResponse getFallback() {
            throwExceptionAtSpecifiedRate("bloomreach", bloomreachErrorRate);
            return bloomreachSearchService.performSearch(search, searchsettings);
        }
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

    public double getBloomreachErrorRate() {
        return bloomreachErrorRate;
    }

    public void setBloomreachErrorRate(double bloomreachErrorRate) {
        this.bloomreachErrorRate = bloomreachErrorRate;
    }

    public double getFunnelbackErrorRate() {
        return funnelbackErrorRate;
    }

    public void setFunnelbackErrorRate(double funnelbackErrorRate) {
        this.funnelbackErrorRate = funnelbackErrorRate;
    }

    class ManafacturedException extends RuntimeException {

        public ManafacturedException(String msg) {
            super(msg);
        }
    }
}
