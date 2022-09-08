package scot.gov.publishing.hippo.funnelback.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Curator {

    List<Exhibit> exhibits = new ArrayList<>();

    List<Exhibit> simpleHtmlExhibits = new ArrayList<>();

    List<Exhibit> advertExhibits = new ArrayList<>();

    public List<Exhibit> getExhibits() {
        return exhibits;
    }

    public void setExhibits(List<Exhibit> exhibits) {
        this.exhibits = exhibits;
    }

    public List<Exhibit> getSimpleHtmlExhibits() {
        return simpleHtmlExhibits;
    }

    public void setSimpleHtmlExhibits(List<Exhibit> simpleHtmlExhibits) {
        this.simpleHtmlExhibits = simpleHtmlExhibits;
    }

    public List<Exhibit> getAdvertExhibits() {
        return advertExhibits;
    }

    public void setAdvertExhibits(List<Exhibit> advertExhibits) {
        this.advertExhibits = advertExhibits;
    }
}
