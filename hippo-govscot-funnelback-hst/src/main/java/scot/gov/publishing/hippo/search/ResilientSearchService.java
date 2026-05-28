package scot.gov.publishing.hippo.search;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import scot.gov.publishing.hippo.search.model.Search;
import scot.gov.publishing.hippo.search.model.SearchResponse;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

@Service
@Component("scot.gov.publishing.hippo.search.ResilientSearchService")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ResilientSearchService implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(ResilientSearchService.class);

    private static final CircuitBreakerConfig CIRCUIT_BREAKER_CONFIG = CircuitBreakerConfig.custom()
            // use a 5-minute time-based sliding window
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
            .slidingWindowSize(300)
            // do not trip the circuit unless we get at least 5 requests in the window
            .minimumNumberOfCalls(5)
            // 50 percent error rate will cause circuit to trip
            .failureRateThreshold(50)
            // wait 30 seconds before retrying a tripped circuit
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .build();

    static final CircuitBreaker SEARCH_CIRCUIT_BREAKER = CircuitBreaker.of("search", CIRCUIT_BREAKER_CONFIG);

    static final CircuitBreaker SUGGESTIONS_CIRCUIT_BREAKER = CircuitBreaker.of("suggestions", CIRCUIT_BREAKER_CONFIG);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private SearchService primarySearchService;

    private SearchService backSearchService;

    Supplier<Double> randomNumberSource = Math::random;

    @Override
    public SearchResponse performSearch(Search search, SearchSettings searchSettings) {
        long timeoutMillis = searchSettings.getTimeoutMillis();
        LOG.info("performSearch {}, {}", timeoutMillis, search.getQuery());
        TimeLimiter timeLimiter = timeLimiter(timeoutMillis);
        try {
            Supplier<CompletableFuture<SearchResponse>> futureSupplier = () ->
                    CompletableFuture.supplyAsync(() -> {
                        throwExceptionAtSpecifiedRate("primary", searchSettings.getFunnelbackErrorRate());
                        return primarySearchService.performSearch(search, searchSettings);
                    }, EXECUTOR);
            return CircuitBreaker.decorateCallable(SEARCH_CIRCUIT_BREAKER,
                    TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier)).call();
        } catch (Exception e) {
            LOG.warn("Primary search failed, falling back to back search service", e);
            throwExceptionAtSpecifiedRate("back", searchSettings.getBloomreachErrorRate());
            return backSearchService.performSearch(search, searchSettings);
        }
    }

    @Override
    public List<String> getSuggestions(String query, String mount, SearchSettings searchSettings) {
        long timeoutMillis = searchSettings.getSugestTimeoutMillis();
        LOG.info("getSuggestions {}, {}", query, timeoutMillis);
        TimeLimiter timeLimiter = timeLimiter(timeoutMillis);
        try {
            Supplier<CompletableFuture<List<String>>> futureSupplier = () ->
                    CompletableFuture.supplyAsync(() -> {
                        throwExceptionAtSpecifiedRate("primary", searchSettings.getFunnelbackErrorRate());
                        return primarySearchService.getSuggestions(query, mount, searchSettings);
                    }, EXECUTOR);
            return CircuitBreaker.decorateCallable(SUGGESTIONS_CIRCUIT_BREAKER,
                    TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier)).call();
        } catch (Exception e) {
            LOG.warn("Primary suggestions failed, returning empty list", e);
            return emptyList();
        }
    }

    private static TimeLimiter timeLimiter(long timeoutMillis) {
        return TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(timeoutMillis))
                .build());
    }

    public SearchService getPrimarySearchService() {
        return primarySearchService;
    }

    public void setPrimarySearchService(SearchService primarySearchService) {
        this.primarySearchService = primarySearchService;
    }

    public SearchService getBackSearchService() {
        return backSearchService;
    }

    public void setBackSearchService(SearchService backSearchService) {
        this.backSearchService = backSearchService;
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

    static class ManafacturedException extends RuntimeException {
        public ManafacturedException(String msg) {
            super(msg);
        }
    }
}
