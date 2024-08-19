package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.hippoecm.hst.core.component.HstRequest;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import scot.gov.publishing.hippo.funnelback.component.Search;
import scot.gov.publishing.hippo.funnelback.component.Sort;
import scot.gov.publishing.hippo.funnelback.model.Pagination;
import scot.gov.publishing.hippo.funnelback.model.ResultPacket;
import scot.gov.publishing.hippo.funnelback.model.ResultsSummary;

import java.time.LocalDate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Created by z418868 on 17/06/2022.
 */
public class PaginationBuilderTest {



    PaginationBuilder sut = new PaginationBuilder();

    @Test
    public void resultsLessThanPageSize() {

        // ARRANGE
        ResultsSummary resultsSummary = resultsSummary(5, 1);

        // ACT
        Pagination pagination = sut.getPagination(resultsSummary, anySearch());

        // ASSERT
        assertTrue("no pagination should be present for 5 results", pagination.getPages().isEmpty());
        assertNull(pagination.getFirst());
        assertNull(pagination.getLast());
    }

    @Test
    public void threePagesOfResultsWithCurrentPage1() {
        // ARRANGE
        ResultsSummary resultsSummary = resultsSummary(25, 1);

        // ACT
        Pagination pagination = sut.getPagination(resultsSummary, anySearch());

        // ASSERT
        assertEquals("wrong number of pages", 3, pagination.getPages().size());
        assertTrue("wrong item selected", pagination.getPages().get(0).isSelected());
        assertEquals("https://www.mygov.scot/search?q=anyQuery&page=1&cat=sitesearch", pagination.getPages().get(0).getUrl());
        assertNull(pagination.getFirst());
        assertNull(pagination.getLast());
    }

    @Test
    public void tenPagesOfResultsWithCurrentPage5() {
        // ARRANGE
        ResultsSummary resultsSummary = resultsSummary(95, 41);

        // ACT
        Pagination pagination = sut.getPagination(resultsSummary, anySearch());

        // ASSERT
        assertEquals("wrong number of pages", 3, pagination.getPages().size());
        assertTrue("wrong item selected", pagination.getPages().get(1).isSelected());
        assertEquals("https://www.mygov.scot/search?q=anyQuery&page=5&cat=sitesearch", pagination.getPages().get(1).getUrl());
        assertEquals(pagination.getFirst().getUrl(), "https://www.mygov.scot/search?q=anyQuery&page=1&cat=sitesearch");
        assertEquals(pagination.getLast().getUrl(), "https://www.mygov.scot/search?q=anyQuery&page=10&cat=sitesearch");
    }

    @Test
    public void tenPagesOfResultsWithCurrentPage3() {
        // ARRANGE
        ResultsSummary resultsSummary = resultsSummary(95, 21);

        // ACT
        Pagination pagination = sut.getPagination(resultsSummary, anySearch());

        // ASSERT
        assertEquals("wrong number of pages", 4, pagination.getPages().size());
        assertTrue("wrong item selected", pagination.getPages().get(2).isSelected());
        assertEquals("https://www.mygov.scot/search?q=anyQuery&page=3&cat=sitesearch", pagination.getPages().get(2).getUrl());
        assertNull("first should be null", pagination.getFirst());
        assertEquals("https://www.mygov.scot/search?q=anyQuery&page=10&cat=sitesearch", pagination.getLast().getUrl());
    }

    @Test
    public void tenPagesOfResultsWithCurrentPage7() {
        // ARRANGE
        ResultsSummary resultsSummary = resultsSummary(95, 71);

        // ACT
        Pagination pagination = sut.getPagination(resultsSummary, anySearch());

        // ASSERT
        assertEquals("wrong number of pages", 4, pagination.getPages().size());
        assertTrue("wrong item selected", pagination.getPages().get(1).isSelected());
        assertEquals("https://www.mygov.scot/search?q=anyQuery&page=8&cat=sitesearch", pagination.getPages().get(1).getUrl());
        assertNull("last should be null", pagination.getLast());
        assertEquals("https://www.mygov.scot/search?q=anyQuery&page=1&cat=sitesearch", pagination.getFirst().getUrl());
    }

    @Test
    public void trailingSlashUrlDoesNotAddExtraSlashes() {
        // ARRANGE
        ResultsSummary resultsSummary = resultsSummary(95, 21);

        // ACT
        Search search = anySearch();
        search.setRequestUrl("https://www.mygov.scot/search/");
        Pagination pagination = sut.getPagination(resultsSummary, search);

        // ASSERT
        assertEquals("wrong number of pages", 4, pagination.getPages().size());
        assertTrue("wrong item selected", pagination.getPages().get(2).isSelected());
        assertEquals("https://www.mygov.scot/search/?q=anyQuery&page=3&cat=sitesearch", pagination.getPages().get(2).getUrl());
        assertNull("first should be null", pagination.getFirst());
        assertEquals("https://www.mygov.scot/search/?q=anyQuery&page=10&cat=sitesearch", pagination.getLast().getUrl());
    }

    String anyQuery() {
        return "anyQuery";
    }

    Search anySearch() {
        Search search = new Search();
        search.setQuery(anyQuery());
        search.setRequestUrl("https://www.mygov.scot/search");

        search.setRequestUrl("https://www.mygov.scot/search");
        HstRequest request = Mockito.mock(HstRequest.class);
        Mockito.when(request.getParameter(eq("cat"))).thenReturn("sitesearch");
        search.setRequest(request);

        return search;
    }

    Search searchWithDates() {
        Search search = anySearch();
        search.setFromDate(LocalDate.now().minusDays(10));
        search.setToDate(LocalDate.now());
        return search;
    }

    ResultPacket resultPacket(int matches) {
        ResultPacket resultPacket = new ResultPacket();
        //resultPacket.setResults(results(matches));
        resultPacket.setResultsSummary(summary(matches));
        return resultPacket;
    }

    ResultsSummary summary(int totalMatching) {
        ResultsSummary summary = new ResultsSummary();
        summary.setTotalMatching(totalMatching);
        summary.setNumRanks(10);
        return summary;
    }

    ResultsSummary resultsSummary(int matches, int start) {
        ResultsSummary resultsSummary = new ResultsSummary();
        resultsSummary.setTotalMatching(matches);
        resultsSummary.setCurrStart(start);
        resultsSummary.setNumRanks(10);
        return resultsSummary;
    }

}
