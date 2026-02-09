package scot.gov.publishing.search.model;

public class SupplementaryQuery {
    String src;

    String query;

    String url;

    String qsupSuppressedQuery;

    String spellSugestionQuery;

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getQsupSuppressedQuery() {
        return qsupSuppressedQuery;
    }

    public void setQsupSuppressedQuery(String qsupSuppressedQuery) {
        this.qsupSuppressedQuery = qsupSuppressedQuery;
    }

    public String getSpellSugestionQuery() {
        return spellSugestionQuery;
    }

    public void setSpellSugestionQuery(String spellSugestionQuery) {
        this.spellSugestionQuery = spellSugestionQuery;
    }
}
