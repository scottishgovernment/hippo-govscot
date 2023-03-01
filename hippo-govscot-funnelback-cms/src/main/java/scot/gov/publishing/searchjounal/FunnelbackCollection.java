package scot.gov.publishing.searchjounal;

public enum FunnelbackCollection {
    NEWS("govscot~ds-news-push"),
    POLICY("govscot~ds-policies-push"),
    PUBLICATIONS("govscot~ds-publications-push"),
    PUBLICATIONS_OTHER("govscot~ds-publications-other-push"),
    STATS_AND_RESEARCH("govscot~ds-statistics-research-push"),
    JOURNAL("govscot~ds-journal-push");

    private final String collectionName;

    FunnelbackCollection(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public static FunnelbackCollection getCollectionByPublicationType(String publicationType) {
        switch (publicationType) {
            case "minutes":
            case "foi-eir-release":
                return PUBLICATIONS_OTHER;
            case "statistics":
            case "research-and-analysis":
                return STATS_AND_RESEARCH;
            default:
                return PUBLICATIONS;
        }
    }
}