package scot.gov.publishing.search.model;

import java.util.ArrayList;
import java.util.List;

public class Image {

    private String image;

    List<String> sizes = new ArrayList<>();

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getSizes() {
        return sizes;
    }

    public void setSizes(List<String> sizes) {
        this.sizes = sizes;
    }

    public static Image createImage(String img, String prefix) {
        Image image = new Image();
        image.setImage(img + "/" + prefix + "%3Amediumtwocolumnssquare");
        image.getSizes().add(img + "/" + prefix + "%3Amediumtwocolumnssquare 96w");
        image.getSizes().add(img + "/" + prefix + "%3Alargetwocolumnssquare 128w");
        image.getSizes().add(img + "/" + prefix + "%3Amediumtwocolumnsdoubledsquare 192w");
        image.getSizes().add(img + "/" + prefix + "%3Alargetwocolumnsdoubledsquare 256w");
        return image;
    }
}
