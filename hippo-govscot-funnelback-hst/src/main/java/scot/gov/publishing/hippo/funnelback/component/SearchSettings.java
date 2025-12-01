package scot.gov.publishing.hippo.funnelback.component;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * represents the search settings stored in the adminstration folder of the site.
 */
public class SearchSettings {

    static final String SEARCH_TYPE_RESILIENT = "resilient";

    static final String SEARCH_TYPE_SEARCH_TYPE_RESILIENT_DXP = "resilient-dxp";

    static final String SEARCH_TYPE_FUNNELBACK = "funnelback";

    static final String SEARCH_TYPE_FUNNELBACK_DXP = "funnelback-dxp";

    static final String SEARCH_TYPE_BLOOMREACH = "bloomreach";

    private String searchType;

    private long timeoutMillis;

    private long sugestTimeoutMillis;

    private boolean enabled;

    private boolean showFilters;

    double funnelbackErrorRate = 0;

    double bloomreachErrorRate = 0;

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public long getSugestTimeoutMillis() {
        return sugestTimeoutMillis;
    }

    public void setSugestTimeoutMillis(long sugestTimeoutMillis) {
        this.sugestTimeoutMillis = sugestTimeoutMillis;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isShowFilters() {
        return showFilters;
    }

    public void setShowFilters(boolean showFilters) {
        this.showFilters = showFilters;
    }

    public double getFunnelbackErrorRate() {
        return funnelbackErrorRate;
    }

    public void setFunnelbackErrorRate(double funnelbackErrorRate) {
        this.funnelbackErrorRate = funnelbackErrorRate;
    }

    public double getBloomreachErrorRate() {
        return bloomreachErrorRate;
    }

    public void setBloomreachErrorRate(double bloomreachErrorRate) {
        this.bloomreachErrorRate = bloomreachErrorRate;
    }

    public static SearchSettings searchSettings() {
        HippoBean global = getGlobalSearchSettingsBean();
        HippoBean site = getSiteSpecificSearchSettingsBean();
        SearchSettings searchsettings = new SearchSettings();
        searchsettings.setSearchType(getValue("search:searchtype", global, site, SEARCH_TYPE_RESILIENT));
        searchsettings.setEnabled(getValue("search:enabled", global, site, true));
        searchsettings.setShowFilters(getValue("search:showFilters", global, site, false));
        searchsettings.setTimeoutMillis(getValue("search:timeoutMillis", global, site, 4000L));
        searchsettings.setSugestTimeoutMillis(getValue("search:suggestTimeoutMillis", global, site, 300L));
        searchsettings.setBloomreachErrorRate(getValue("search:bloomreachErrorRate", global, site,0.0));
        searchsettings.setFunnelbackErrorRate(getValue("search:funnelbackErrorRate", global, site, 0.0));
        return searchsettings;
    }

    static <T> T getValue(String property, HippoBean global, HippoBean site, T defaultValue) {
        T globalValue = global.getSingleProperty(property);
        T siteValue = site != null ? site.getSingleProperty(property) : null;

        // if there is a site specific value then use that
        if (siteValue != null) {
            return siteValue;
        }

        // if there is a global value then use that
        if (globalValue != null) {
            return globalValue;
        }

        // fallback to a default value
        return defaultValue;
    }

    /**
     * In publishing each site has its own search settings document, and for gov it lives under the root
     * administration folder
     */
    static HippoBean getGlobalSearchSettingsBean() {
        HippoBean siteBaseBean = RequestContextProvider.get().getSiteContentBaseBean();
        HippoBean root = siteBaseBean.getParentBean();
        return root.getBean("administration/search-settings");
    }

    static HippoBean getSiteSpecificSearchSettingsBean() {
        HippoBean siteBaseBean = RequestContextProvider.get().getSiteContentBaseBean();
        return siteBaseBean.getBean("administration/search-settings");
    }

}