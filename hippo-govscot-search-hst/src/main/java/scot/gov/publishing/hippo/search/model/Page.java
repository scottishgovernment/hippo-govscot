package scot.gov.publishing.hippo.search.model;

public class Page extends Link{

    private boolean selected;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
