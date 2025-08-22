package scot.gov.publishing.hippo.funnelback.component;

import scot.gov.publishing.hippo.funnelback.component.postprocess.SearchQueryBuilder;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterButtonGroups {

    List<FilterButton> types = new ArrayList<>();

    List<FilterButton> topics = new ArrayList<>();

    List<FilterButton> languages = new ArrayList<>();

    Map<String, FilterButton> dates = new HashMap<>();

    public List<FilterButton> getTypes() {
        return types;
    }

    public List<FilterButton> getTopics() {
        return topics;
    }

    public List<FilterButton> getLanguages() {
        return languages;
    }

    public Map<String, FilterButton> getDates() {
        return dates;
    }

    public static FilterButtonGroups filterButtonGroups(Search search) {
        return filterButtonGroups(search, "q");
    }

    public static FilterButtonGroups filterButtonGroups(Search search, String searchParam) {
        FilterButtonGroups groups = new FilterButtonGroups();

        if (!search.getPublicationTypes().isEmpty()) {
            groups.types = publicationTypesButtonGroup(search, searchParam);
        }

        if (!search.getTopics().isEmpty()) {
            groups.topics = topicsButtonGroup(search, searchParam);
        }

        if (!search.getLanguages().isEmpty()) {
            groups.languages = languagesButtonGroup(search, searchParam);
        }

        if (search.getFromDate() != null || search.getToDate() != null) {
            groups.dates = datesGroup(search, searchParam);
        }

        return groups;
    }

    static Map<String, FilterButton> datesGroup(Search search, String searchParam) {
        Map<String, FilterButton> buttons = new HashMap<>();

        SearchQueryBuilder searchQueryBuilder = new SearchQueryBuilder(searchParam);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (search.getFromDate() != null) {
            FilterButton from = new FilterButton();
            from.setLabel(search.getFromDate().format(formatter));
            from.setUrl(searchQueryBuilder.queryParamsNoFromDate(search));
            buttons.put("begin", from);
        }

        if (search.getToDate() != null) {
            FilterButton end = new FilterButton();
            end.setLabel(search.getToDate().format(formatter));
            end.setUrl(searchQueryBuilder.queryParamsNoToDate(search));
            buttons.put("end", end);
        }
        return buttons;
    }

    static List<FilterButton> topicsButtonGroup(Search search, String searchParam) {
        List<FilterButton> buttons = new ArrayList<>();
        SearchQueryBuilder searchQueryBuilder = new SearchQueryBuilder(searchParam);
        search.getTopics().entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
            FilterButton button = new FilterButton();
            button.setLabel(e.getValue());
            button.setUrl(searchQueryBuilder.queryParamsWithoutTopic(search, e.getKey()));
            button.setId(e.getKey());
            buttons.add(button);
        });
        return buttons;
    }

    static List<FilterButton> publicationTypesButtonGroup(Search search, String searchParam) {
        List<FilterButton> buttons = new ArrayList<>();
        SearchQueryBuilder searchQueryBuilder = new SearchQueryBuilder(searchParam);
        search.getPublicationTypes().entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
            FilterButton button = new FilterButton();
            button.setLabel(e.getValue());
            button.setUrl(searchQueryBuilder.queryParamsWithoutPublicationType(search, e.getKey()));
            button.setId(e.getKey());
            buttons.add(button);
        });
        return buttons;
    }

    static List<FilterButton> languagesButtonGroup(Search search, String searchParam) {
        List<FilterButton> buttons = new ArrayList<>();
        SearchQueryBuilder searchQueryBuilder = new SearchQueryBuilder(searchParam);
        search.getLanguages().entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
            FilterButton button = new FilterButton();
            button.setLabel(e.getValue());
            button.setUrl(searchQueryBuilder.queryParamsWithoutLanguage(search, e.getKey()));
            button.setId(e.getKey());
            buttons.add(button);
        });
        return buttons;
    }
}
