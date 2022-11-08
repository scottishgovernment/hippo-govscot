package scot.gov.publishing.hippo.funnelback.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Service
@Component("scot.gov.publishing.hippo.funnelback.component.SuggestionsResource")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SuggestionsResource {

    @Autowired
    FunnelbackSearchService funnelbackSearchService;

    Supplier<SearchSettings> searchSettingSource = () -> ResilientSearchComponent.searchSettings();

    @Path("search/suggestions")
    @Produces(APPLICATION_JSON)
    @GET
    public List<String> getSuggestions(@QueryParam("partial_query") String partialQuery) {
        SearchSettings searchSettings = searchSettingSource.get();

        if (!searchSettings.isEnabled() || "bloomreach".equals(searchSettings.getSearchType())) {
            return emptyList();
        } else {
            return funnelbackSearchService.getSuggestions(partialQuery);
        }
    }
}