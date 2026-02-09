package scot.gov.publishing.search.model;

import java.util.ArrayList;
import java.util.List;

public class Result {
    private Link link;

    private String summary;

    private Image image;

    private String label;

    private String subtitle;

    private String displayDate;

    private String displayDateTime;

    private List<Link> partOf = new ArrayList<>();

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDisplayDate() {
        return displayDate;
    }

    public void setDisplayDate(String displayDate) {
        this.displayDate = displayDate;
    }

    public String getDisplayDateTime() {
        return displayDateTime;
    }

    public void setDisplayDateTime(String displayDateTime) {
        this.displayDateTime = displayDateTime;
    }

    public List<Link> getPartOf() {
        return partOf;
    }

    public void setPartOf(List<Link> partOf) {
        this.partOf = partOf;
    }
}
