package scot.gov.publishing.search;

import org.hippoecm.hst.core.request.HstRequestContext;

import java.util.Map;

/**
 * Interface use by ResilientSearchComponent to make providsion of topics and publiction types pipeline specific.
 */
public interface MapProvider {

    /**
     * Get the map of key value pairs
     */
    Map<String, String> get(HstRequestContext context);

}
