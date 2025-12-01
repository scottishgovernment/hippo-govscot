package scot.gov.publishing.searchjournal;

import static org.apache.commons.lang3.StringUtils.equalsAny;

public enum FunnelbackCollection {
    NEWS("ds-news-push"),
    POLICY("ds-policies-push"),
    PUBLICATIONS("ds-publications-push"),
    PUBLICATIONS_OTHER("ds-publications-other-push"),
    JOURNAL("ds-journal-push");

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