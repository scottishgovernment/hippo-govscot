package scot.gov.publishing.hippo.funnelback.model;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListMetadata {

    List<String> titleSeries;

    List<String> titleSeriesLink;

    List<String> c;

    List<String> d;

    List<String> f;

    List<String> t;

    List<String> dcTitle;

    List<String> publicationType;

    List<String> publicationCollection;

    List<String> publicationCollectionLink;

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

    public List<String> getC() {
        return c;
    }

    public void setC(List<String> c) {
        this.c = c;
    }

    public List<String> getD() {
        return d;
    }

    public void setD(List<String> d) {
        this.d = d;
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

    public List<String> getDcTitle() {
        return dcTitle;
    }

    public void setDcTitle(List<String> dcTitle) {
        this.dcTitle = dcTitle;
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

    // get the first date as a localdatetime
    public Calendar getDateTime() {
//        if (d.isEmpty()) {
//            return null;
//        }
//
//        String first = d.get(0);
//        return LocalDateTime.parse(first);
        // 2023-02-23T15:55:42.443
        return Calendar.getInstance();
    }

}