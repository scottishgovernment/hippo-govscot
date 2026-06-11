package scot.gov.publishing.searchjournal.funnelback;

/**
 * Configuration for connecting to Funnelback / Squiz push API.
 * The accountName and positionKey are site-specific values that should
 * be set by the site's FunnelbackFactory (e.g. via CMS job attributes).
 */
public class FunnelbackConfiguration {

    private String searchType;

    private String apiUrl;

    private String clientId;

    private String apiKey;

    /** The account/namespace prefix used in Funnelback collection names (e.g. "govscot"). */
    private String accountName;

    /** The Funnelback collection used to store the journal position (e.g. "ds-journal-push"). */
    private String positionCollection;

    /** The URL key used within the position collection to store the journal position. */
    private String positionKey;

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

}
