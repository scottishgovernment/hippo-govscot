package scot.gov.publishing.hippo.funnelback.component;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import org.onehippo.cms7.crisp.hst.module.CrispHstServices;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunnelbackCuratorListener implements PersistedHippoEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(FunnelbackCuratorListener.class);

    static final String CURATOR_DOC_PATH = "/content/documents/administration/funnelback-curator-changes";

    interface CacheCleaner {
        void clean();
    }

    CacheCleaner cacheCleaner = () -> {
        clearPageCache();
        clearCrispCache();
    };

    @Override
    public String getEventCategory() {
        return HippoEventConstants.CATEGORY_WORKFLOW;
    }

    @Override
    public String getChannelName() {
        return "funnelback-curator-listener";
    }

    @Override
    public boolean onlyNewEvents() {
        return true;
    }

    @Override
    public void onHippoEvent(HippoEvent event) {

        if (isFunnelbackCuratorChange(event)) {
            LOG.info("funnelback curator change detected ... clearing page cache and CRISP cache");
            cacheCleaner.clean();
        }
    }

    void clearPageCache() {
        HstCache pageCache = HstServices.getComponentManager().getComponent("pageCache");
        pageCache.clear();
    }

    void clearCrispCache() {
        ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker(HstServices.getComponentManager());
        ResourceDataCache crispCache = broker.getResourceDataCache("funnelback-dxp");
        crispCache.clear();
    }

    boolean isFunnelbackCuratorChange(HippoEvent event) {
        return isCommitEvent(event) && isCuratorChangeDocument(event);
    }

    boolean isCommitEvent(HippoEvent event) {
        String action = event.action();
        return "commitEditableInstance".equals(action);
    }

    boolean isCuratorChangeDocument(HippoEvent event) {
        String path = event.getValues().get("subjectPath").toString();
        return StringUtils.equals(path, CURATOR_DOC_PATH);
    }
}