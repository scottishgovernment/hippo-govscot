package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.junit.Assert;
import org.junit.Test;
import scot.gov.publishing.hippo.funnelback.component.Search;
import scot.gov.publishing.hippo.funnelback.component.SearchBuilder;
import scot.gov.publishing.hippo.funnelback.component.Sort;

import java.time.LocalDate;
import java.util.Arrays;

public class SearchQueryBuilderTest {

    @Test
    public void expectedQueryWithNoFacets() {

        Search search = new SearchBuilder()
                .query("query")
                .page(1)
                .build();

        String expected = "q=query&page=1&sort=relevance";
        String actual = new SearchQueryBuilder().queryParams(search, 1);
        Assert.assertEquals(expected, actual);

    }
    @Test
    public void expectedQueryWithAllFacets() {

        Search search = new SearchBuilder()
                .query("query")
                .page(1)
                .fromDate(LocalDate.of(2001, 1, 1))
                .toDate(LocalDate.of(2002, 2, 2))
                .publicationTypes("type1;type2")
                .topics("topics1;topic2")
                .sort(Sort.ADATE)
                .build();

        String expected = "q=query&page=1&sort=adate&from=01/01/2001&to=02/02/2002&publicationTypes=type1;type2&topics=topics1;topic2";
        String actual = new SearchQueryBuilder().queryParams(search, 1);
        Assert.assertEquals(expected, actual);

    }
}
