package scot.gov.publishing.hippo.search.model;

import java.util.ArrayList;
import java.util.List;

public class Pagination {
    private Page first;

    private Page last;

    private Page previous;

    private Page next;

    private List<Page> pages = new ArrayList<>();

    private int currentPageIndex = 1;

    public Page getFirst() {
        return first;
    }

    public void setFirst(Page first) {
        this.first = first;
    }

    public Page getLast() {
        return last;
    }

    public void setLast(Page last) {
        this.last = last;
    }

    public Page getPrevious() {
        return previous;
    }

    public void setPrevious(Page previous) {
        this.previous = previous;
    }

    public Page getNext() {
        return next;
    }

    public void setNext(Page next) {
        this.next = next;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public void setCurrentPageIndex(int currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
    }
}
