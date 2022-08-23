package scot.gov.publishing.hippo.funnelback.component;

/**
 * Created by z418868 on 19/08/2022.
 */
public class SearchSettings {

    private String searchType;

    private long timeoutMillis;

    private boolean enabled;


    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
