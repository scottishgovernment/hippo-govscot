package scot.gov.publishing.search.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by z418868 on 01/07/2022.
 */
public class SearchResponse {

    public enum Type { BLOOMREACH, FUNNELBACK }

    boolean hasResults;

    Type type;

    Question question = new Question();

    ResultsSummary resultsSummary = new ResultsSummary();

    Pagination pagination = new Pagination();

    String spellSuggestionSuppressedQuery;

    String spellSuggestionQuery;

    String queryHighlightRegex;

    List<SupplementaryQuery> supplementaryQueries = new ArrayList<>();

    List<Result> results = new ArrayList<>();

    List<String> htmlMessages = new ArrayList<>();

    List<PromotedResult> adverts = new ArrayList<>();

    List<Link> relatedResults;

    FilterButtonGroups filterButtons = new FilterButtonGroups();

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public ResultsSummary getResultsSummary() {
        return resultsSummary;
    }

    public void setResultsSummary(ResultsSummary resultsSummary) {
        this.resultsSummary = resultsSummary;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public String getSpellSuggestionSuppressedQuery() {
        return spellSuggestionSuppressedQuery;
    }

    public void setSpellSuggestionSuppressedQuery(String spellSuggestionSuppressedQuery) {
        this.spellSuggestionSuppressedQuery = spellSuggestionSuppressedQuery;
    }

    public String getSpellSuggestionQuery() {
        return spellSuggestionQuery;
    }

    public void setSpellSuggestionQuery(String spellSuggestionQuery) {
        this.spellSuggestionQuery = spellSuggestionQuery;
    }

    public String getQueryHighlightRegex() {
        return queryHighlightRegex;
    }

    public void setQueryHighlightRegex(String queryHighlightRegex) {
        this.queryHighlightRegex = queryHighlightRegex;
    }

    public List<SupplementaryQuery> getSupplementaryQueries() {
        return supplementaryQueries;
    }

    public void setSupplementaryQueries(List<SupplementaryQuery> supplementaryQueries) {
        this.supplementaryQueries = supplementaryQueries;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public List<String> getHtmlMessages() {
        return htmlMessages;
    }

    public void setHtmlMessages(List<String> htmlMessages) {
        this.htmlMessages = htmlMessages;
    }

    public List<PromotedResult> getAdverts() {
        return adverts;
    }

    public void setAdverts(List<PromotedResult> adverts) {
        this.adverts = adverts;
    }

    public List<Link> getRelatedResults() {
        return relatedResults;
    }

    public void setRelatedResults(List<Link> relatedResults) {
        this.relatedResults = relatedResults;
    }

    public FilterButtonGroups getFilterButtons() {
        return filterButtons;
    }

    public void setFilterButtons(FilterButtonGroups filterButtons) {
        this.filterButtons = filterButtons;
    }

    public boolean getHasResults() {
        return hasResults;
    }

    public void setHasResults(boolean hasResults) {
        this.hasResults = hasResults;
    }

    public static SearchResponse blankSearchResponse() {
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setType(SearchResponse.Type.BLOOMREACH);
        Question question = new Question();
        question.setOriginalQuery("");
        question.setQuery("");
        searchResponse.setQuestion(question);
        return searchResponse;
    }
}
