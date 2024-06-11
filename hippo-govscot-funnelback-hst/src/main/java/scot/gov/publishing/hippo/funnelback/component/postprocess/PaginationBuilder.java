package scot.gov.publishing.hippo.funnelback.component.postprocess;

import scot.gov.publishing.hippo.funnelback.component.Search;
import scot.gov.publishing.hippo.funnelback.model.Page;
import scot.gov.publishing.hippo.funnelback.model.Pagination;
import scot.gov.publishing.hippo.funnelback.model.ResultsSummary;

import java.util.HashSet;
import java.util.Set;

public class PaginationBuilder {

    private static final int PAGES = 3;

    private SearchQueryBuilder queryBuilder = new SearchQueryBuilder();

    public Pagination getPagination(ResultsSummary resultsSummary, Search search) {

        if (resultsSummary.getTotalMatching() <= resultsSummary.getNumRanks()) {
            return new Pagination();
        }

        Pagination pagination = new Pagination();
        int currentPage = rankToPage(resultsSummary.getCurrStart(), resultsSummary.getNumRanks());
        pagination.setCurrentPageIndex(currentPage);
        int maxPage = rankToPage(resultsSummary.getTotalMatching(), resultsSummary.getNumRanks());

        int firstPage = firstPage(currentPage, maxPage);
        int lastPage = lastPage(firstPage, maxPage);
        Set<Integer> included = new HashSet<>();
        for (int pageIndex = firstPage; pageIndex <= lastPage; pageIndex++) {
            Page page = page(search, pageIndex);
            included.add(pageIndex);
            page.setSelected(pageIndex == currentPage);
            pagination.getPages().add(page);
        }

        if (!included.contains(1)) {
            Page first = page(search, 1);
            pagination.setFirst(first);
        }

        if (!included.contains(maxPage)) {
            Page last = page(search, maxPage);
            pagination.setLast(last);
        }

        if (currentPage != 1) {
            Page previous = page(search, currentPage - 1);
            previous.setLabel("Previous");
            pagination.setPrevious(previous);
        }

        if (currentPage != maxPage) {
            Page next = page(search, currentPage + 1);
            next.setLabel("Next");
            pagination.setNext(next);
        }

        return pagination;
    }

    int rankToPage(int rank, int pageSize) {
        return (rank - 1) / pageSize + 1;
    }

    int firstPage(int currentPage, int maxPage) {
        int minBeforeCount = PAGES / 2;

        if (currentPage <= minBeforeCount) {
            return 1;
        }

        if (currentPage + minBeforeCount >= maxPage) {
            return Math.max(1, maxPage - PAGES);
        }

        // if the first page is 2 then just make it 1
        int page = currentPage - minBeforeCount;
        return page == 2 ? 1 : page;
    }

    int lastPage(int firstPage, int maxPage) {

        int page = Math.min(firstPage + PAGES - 1, maxPage);

        if (page <= PAGES) {
            return Math.min(maxPage, firstPage + PAGES);
        }

        // if the first page the second to last then make it the last one
        if (page == maxPage - 1) {
            return maxPage;
        }

        return page;
    }

    Page page(Search search, int index) {
        Page page = new Page();
        String queryString = queryBuilder.queryParams(search, index);
        String url = new StringBuffer(search.getRequestUrl()).append('?').append(queryString).toString();
        page.setLabel(Integer.toString(index));
        page.setUrl(url);
        page.setSelected(false);
        return page;
    }

}
