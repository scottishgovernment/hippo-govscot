package scot.gov.publishing.search;

import org.junit.Test;
import scot.gov.publishing.hippo.funnelback.component.FunnelbackSearchService;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuggestionsResourceTest {

    @Test
    public void returnsEmptyListIfSearchIsDisabled() {
        // ARRANGE
        SuggestionsResource sut = new SuggestionsResource();
        sut.mountSupplier = () -> "mygov";
        SearchSettings searchSettings = new SearchSettings();
        searchSettings.setEnabled(false);
        sut.searchSettingSource = () -> searchSettings;
        sut.funnelbackSearchServiceDXP = mock(FunnelbackSearchService.class);

        // ACT
        List<String> actual = sut.getSuggestions("query");

        // ASSERT
        assertEquals(Collections.emptyList(), actual);
    }

    @Test
    public void returnsEmptyListIfSearchIsBloomreach() {
        // ARRANGE
        SuggestionsResource sut = new SuggestionsResource();
        sut.mountSupplier = () -> "mygov";
        SearchSettings searchSettings = new SearchSettings();
        searchSettings.setEnabled(true);
        searchSettings.setSearchType("bloomreach");
        sut.searchSettingSource = () -> searchSettings;

        // ACT
        List<String> actual = sut.getSuggestions("query");

        // ASSERT
        assertEquals(Collections.emptyList(), actual);
    }

    @Test
    public void returnsResultsIfSearchTypeIsResilient() {
        // ARRANGE
        SuggestionsResource sut = new SuggestionsResource();
        sut.mountSupplier = () -> "mygov";
        SearchSettings searchSettings = new SearchSettings();
        searchSettings.setEnabled(true);
        searchSettings.setSugestTimeoutMillis(1000);
        searchSettings.setSearchType("resilient");
        sut.searchSettingSource = () -> searchSettings;
        sut.funnelbackSearchServiceDXP = mock(FunnelbackSearchService.class);
        when(sut.funnelbackSearchServiceDXP.getSuggestions(anyString(), anyString(), any(SearchSettings.class))).thenReturn(singletonList("one"));

        // ACT
        List<String> actual = sut.getSuggestions("query");

        // ASSERT
        assertEquals(singletonList("one"), actual);
    }

    @Test
    public void returnsResultsIfSearchTypeIsFunnelback() {
        // ARRANGE
        SuggestionsResource sut = new SuggestionsResource();
        sut.mountSupplier = () -> "mygov";
        SearchSettings searchSettings = new SearchSettings();
        searchSettings.setEnabled(true);
        searchSettings.setSearchType("resilient");
        sut.searchSettingSource = () -> searchSettings;
        sut.funnelbackSearchServiceDXP = mock(FunnelbackSearchService.class);
        when(sut.funnelbackSearchServiceDXP.getSuggestions(anyString(), anyString(), any(SearchSettings.class))).thenReturn(singletonList("one"));

        // ACT
        List<String> actual = sut.getSuggestions("query");

        // ASSERT
        assertEquals(singletonList("one"), actual);
    }
}
