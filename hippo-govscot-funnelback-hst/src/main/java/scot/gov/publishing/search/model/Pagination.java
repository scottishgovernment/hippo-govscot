package scot.gov.publishing.search.model;

import java.util.ArrayList;
import java.util.List;

public class Pagination {
    public Page first;

    public Page last;

    public Page previous;

    public Page next;

    public List<Page> pages = new ArrayList<>();

    public int currentPageIndex = 1;

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
