package scot.gov.publishing.journal.funnelback;

/**
 * Configuration for connecting to Funnelback / Squiz push API.
 * The accountName and positionKey are site-specific values that should
 * be set by the site's FunnelbackIndexerFactory (e.g. via CMS job attributes).
 */
public class FunnelbackConfiguration {

    private String apiUrl;

    private String clientId;

    private String apiKey;

    /** The Funnelback collection used to store the journal position (e.g. "ds-journal-push"). */
    private String positionCollection;

    /** The URL key used within the position collection to store the journal position. */
    private String positionKey;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPositionCollection() {
        return positionCollection;
    }

    public void setPositionCollection(String positionCollection) {
        this.positionCollection = positionCollection;
    }

    public String getPositionKey() {
        return positionKey;
    }

    public void setPositionKey(String positionKey) {
        this.positionKey = positionKey;
    }

    @Override
    public String toString() {
        return "FunnelbackConfiguration{" +
                ", apiUrl='" + apiUrl + '\'' +
                ", clientId='" + clientId + '\'' +
                ", apiKey='" + (apiKey == null ? null : "***") + '\'' +
                ", positionCollection='" + positionCollection + '\'' +
                ", positionKey='" + positionKey + '\'' +
                '}';
    }
}
