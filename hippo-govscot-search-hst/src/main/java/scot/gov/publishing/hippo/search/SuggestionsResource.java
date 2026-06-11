package scot.gov.publishing.hippo.search;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.hippoecm.hst.container.RequestContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import scot.gov.publishing.hippo.funnelback.component.FunnelbackSearchService;

import java.util.List;
import java.util.function.Supplier;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Collections.emptyList;

@Service
@Component("scot.gov.publishing.search.SuggestionsResource")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SuggestionsResource {

    @Autowired
    @Qualifier("funnelbackSearchServiceDXP")
    FunnelbackSearchService funnelbackSearchServiceDXP;

    ResilientSearchService resilientSearchService;

    Supplier<SearchSettings> searchSettingSource = SearchSettings::searchSettings;

    Supplier<String> mountSupplier = () -> FunnelbackSearchService.mountName(RequestContextProvider.get());

    @Context
    private UriInfo uriInfo;

    public SuggestionsResource() {
        resilientSearchService = new ResilientSearchService();
        resilientSearchService.setFunnelbackSearchServiceDXP(funnelbackSearchServiceDXP);
    }

    @Path("search/suggestions")
    @Produces(APPLICATION_JSON)
    @GET
    public List<String> getSuggestions(@QueryParam("q") String partialQuery) {
        SearchSettings searchSettings = searchSettingSource.get();
        String mount = mountSupplier.get();
        resilientSearchService.setFunnelbackSearchServiceDXP(funnelbackSearchServiceDXP);
        if (!searchSettings.isEnabled() || "bloomreach".equals(searchSettings.getSearchType())) {
            return emptyList();
        } else {
            return resilientSearchService.getSuggestions(partialQuery, mount, searchSettings);
        }
    }
}