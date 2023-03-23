package scot.gov.publishing.searchjounal;

import static org.apache.commons.lang3.StringUtils.equalsAny;

public enum FunnelbackCollection {
    NEWS("govscot~ds-news-push"),
    POLICY("govscot~ds-policies-push"),
    PUBLICATIONS("govscot~ds-publications-push"),
    PUBLICATIONS_OTHER("govscot~ds-publications-other-push"),
    JOURNAL("govscot~ds-journal-push");

    private final String collectionName;

    FunnelbackCollection(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public static FunnelbackCollection getCollectionByPublicationType(String publicationType) {
        return equalsAny(publicationType, "minutes", "foi-eir-release")
                ? PUBLICATIONS_OTHER
                : PUBLICATIONS;
    }
}