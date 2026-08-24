package scot.gov.publishing.journal;

/**
 * The action a {@link JournalEntry} represents.
 */
public enum JournalAction {

    PUBLISH,
    UNPUBLISH;

    /**
     * Converts a Bloomreach/Hippo workflow action string (e.g. {@code "publish"}, {@code "depublish"})
     * into a {@link JournalAction}.
     */
    public static JournalAction fromBloomreachAction(String bloomreachAction) {
        if ("publish".equals(bloomreachAction)) {
            return PUBLISH;
        }
        if ("depublish".equals(bloomreachAction)) {
            return UNPUBLISH;
        }
        return valueOf(bloomreachAction);
    }
}
