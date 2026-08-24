package scot.gov.publishing.journal;

import java.util.Calendar;

public class JournalEntry {

    private String contentId;

    /**
     * Identifies which site this entry belongs to, for deployments that serve multiple sites
     * from the same journal/reconciliation pipeline (e.g. a publishing platform). {@code null}
     * or blank means the single default site.
     */
    private String site;

    private String url;

    private Calendar timestamp;

    private JournalAction action;

    private long attempt = 0;

    private long sequence;

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    public JournalAction getAction() {
        return action;
    }

    public void setAction(JournalAction action) {
        this.action = action;
    }

    public long getAttempt() {
        return attempt;
    }

    public void setAttempt(long attempt) {
        this.attempt = attempt;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }
}