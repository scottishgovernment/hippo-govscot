package scot.gov.publishing.journal.funnelback;

/**
 * Site-specific values needed to maintain the journal position within Funnelback, passed in by
 * the site's own code when it calls {@link FunnelbackIndexerFactory#newFunnelback}.
 *
 * @param positionCollection the Funnelback collection used to store the journal position (e.g. "ds-journal-push")
 * @param positionKey the URL key used within the position collection to store the journal position
 */
public record FunnelbackIndexingConfiguration(String positionCollection, String positionKey) {
}
