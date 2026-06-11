package scot.gov.publishing.hippo.funnelback.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListMetadata {

    List<String> c = new ArrayList<>();

    List<String> d= new ArrayList<>();

    List<String> dcTitle= new ArrayList<>();

    String displayDate;

    String displayDateTime;

    List<String> f= new ArrayList<>();

    List<String> t= new ArrayList<>();

    List<String> image= new ArrayList<>();

    List<String> personRole = new ArrayList<>();

    List<String> personName=  new ArrayList<>();

    List<String> publicationDate = new ArrayList<>();

    List<String> publicationType = new ArrayList<>();

    List<String> publicationCollection = new ArrayList<>();

    List<String> publicationCollectionLink = new ArrayList<>();

    List<String> titleSeries = new ArrayList<>();

    List<String> titleSeriesLink = new ArrayList<>();

    public List<String> getC() {
        return c;
    }

    public void setC(List<String> c) {
        this.c = c;
    }

    public List<String> getD() {
        return d;
    }

    public List<String> getDcTitle() {
        return dcTitle;
    }

    public void setDcTitle(List<String> dcTitle) {
        this.dcTitle = dcTitle;
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

    public List<String> getF() {
        return f;
    }

    public void setF(List<String> f) {
        this.f = f;
    }

    public List<String> getT() {
        return t;
    }

    public void setT(List<String> t) {
        this.t = t;
    }

    public List<String> getImage() {
        return image;
    }

    public void setImage(List<String> image) {
        this.image = image;
    }

    public List<String> getPersonRole() {
        return personRole;
    }

    public void setPersonRole(List<String> personRole) {
        this.personRole = personRole;
    }

    public List<String> getPersonName() {
        return personName;
    }

    public void setPersonName(List<String> personName) {
        this.personName = personName;
    }

    public List<String> getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(List<String> publicationDate) {
        this.publicationDate = publicationDate;
    }

    public List<String> getPublicationType() {
        return publicationType;
    }

    public void setPublicationType(List<String> publicationType) {
        this.publicationType = publicationType;
    }

    public List<String> getPublicationCollection() {
        return publicationCollection;
    }

    public void setPublicationCollection(List<String> publicationCollection) {
        this.publicationCollection = publicationCollection;
    }

    public List<String> getPublicationCollectionLink() {
        return publicationCollectionLink;
    }

    public void setPublicationCollectionLink(List<String> publicationCollectionLink) {
        this.publicationCollectionLink = publicationCollectionLink;
    }

    public List<String> getTitleSeries() {
        return titleSeries;
    }

    public void setTitleSeries(List<String> titleSeries) {
        this.titleSeries = titleSeries;
    }

    public List<String> getTitleSeriesLink() {
        return titleSeriesLink;
    }

    public void setTitleSeriesLink(List<String> titleSeriesLink) {
        this.titleSeriesLink = titleSeriesLink;
    }

    public void setD(List<String> d) {
        this.d = d;
    }
}