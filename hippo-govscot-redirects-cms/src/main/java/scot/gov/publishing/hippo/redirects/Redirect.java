package scot.gov.publishing.hippo.redirects;

public class Redirect {

    private String from;

    private String to;

    private String description;

    private boolean historicalUrl;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHistoricalUrl() {
        return historicalUrl;
    }

    public void setHistoricalUrl(boolean historicalUrl) {
        this.historicalUrl = historicalUrl;
    }
}
