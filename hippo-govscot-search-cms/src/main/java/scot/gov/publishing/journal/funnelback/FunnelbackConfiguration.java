package scot.gov.publishing.journal.funnelback;

/**
 * Connection settings for the Funnelback / Squiz push API, sourced from container configuration
 * (properties file).
 */
public record FunnelbackConfiguration(String apiUrl, String clientId, String apiKey) {

    @Override
    public String toString() {
        return "FunnelbackConfiguration{" +
                "apiUrl='" + apiUrl + '\'' +
                ", clientId='" + clientId + '\'' +
                ", apiKey='" + (apiKey == null ? null : "***") + '\'' +
                '}';
    }
}
