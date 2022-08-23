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

@Service
@Component("scot.gov.publishing.hippo.funnelback.component.ResilientSearchService")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ResilientSearchService implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(ResilientSearchService.class);

    private static final String FUNNELBACK_COMMAND_KEY = "FunnelbackCommandKey";

    private SearchService funnelbackSearchService;

    private SearchService bloomreachSearchService;

    @Override
    public SearchResponse performSearch(Search search, SearchSettings searchsettings) {
        LOG.info("performSearch {}, {}, {}", searchsettings.isEnabled(), searchsettings.getSearchType(), searchsettings.getTimeoutMillis());

        HystrixCommandProperties.Setter commandPropertiesSetter = HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds((int) searchsettings.getTimeoutMillis());
        SearchCommand command = new SearchCommand(search, searchsettings, commandPropertiesSetter);
        return command.execute();
    }

    class SearchCommand extends HystrixCommand<SearchResponse> {

        Search search;

        SearchSettings searchsettings;

        public SearchCommand(Search search, SearchSettings searchsettings, HystrixCommandProperties.Setter commandPropertiesSetter) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(FUNNELBACK_COMMAND_KEY))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("searchWithTimeout"))
                    .andCommandPropertiesDefaults(commandPropertiesSetter));

            this.search = search;
            this.searchsettings = searchsettings;
        }

        @Override
        protected SearchResponse run() {
            LOG.info("Using funnelback search ... {}", search.getQuery());
            return funnelbackSearchService.performSearch(search, searchsettings);
        }

        @Override
        protected SearchResponse getFallback() {
            LOG.info("Using fallback search ... {}", search.getQuery());
            return bloomreachSearchService.performSearch(search, searchsettings);
        }
    }

    public SearchService getFunnelbackSearchService() {
        return funnelbackSearchService;
    }

    public void setFunnelbackSearchService(SearchService funnelbackSearchService) {
        this.funnelbackSearchService = funnelbackSearchService;
    }

    public SearchService getBloomreachSearchService() {
        return bloomreachSearchService;
    }

    public void setBloomreachSearchService(SearchService bloomreachSearchService) {
        this.bloomreachSearchService = bloomreachSearchService;
    }

}
