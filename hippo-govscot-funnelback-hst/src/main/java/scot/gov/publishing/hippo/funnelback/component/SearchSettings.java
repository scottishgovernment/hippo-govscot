package scot.gov.publishing.hippo.funnelback.component;

/**
 * represents the search settings stored in the adminstration folder of the site.
 */
public class SearchSettings {

    private String searchType;

    private long timeoutMillis;

    private boolean enabled;

    double funnelbackErrorRate = 0;

    double bloomreachErrorRate = 0;

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

    public double getFunnelbackErrorRate() {
        return funnelbackErrorRate;
    }

    public void setFunnelbackErrorRate(double funnelbackErrorRate) {
        this.funnelbackErrorRate = funnelbackErrorRate;
    }

    public double getBloomreachErrorRate() {
        return bloomreachErrorRate;
    }

    public void setBloomreachErrorRate(double bloomreachErrorRate) {
        this.bloomreachErrorRate = bloomreachErrorRate;
    }
}