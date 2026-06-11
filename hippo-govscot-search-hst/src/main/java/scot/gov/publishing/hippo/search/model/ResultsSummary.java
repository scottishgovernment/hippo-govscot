package scot.gov.publishing.hippo.search.model;

public class ResultsSummary {

    int totalMatching;

    int numRanks;

    int currStart;

    int currEnd;

    public int getTotalMatching() {
        return totalMatching;
    }

    public void setTotalMatching(int totalMatching) {
        this.totalMatching = totalMatching;
    }

    public int getNumRanks() {
        return numRanks;
    }

    public void setNumRanks(int numRanks) {
        this.numRanks = numRanks;
    }

    public int getCurrStart() {
        return currStart;
    }

    public void setCurrStart(int currStart) {
        this.currStart = currStart;
    }

    public int getCurrEnd() {
        return currEnd;
    }

    public void setCurrEnd(int currEnd) {
        this.currEnd = currEnd;
    }
}
