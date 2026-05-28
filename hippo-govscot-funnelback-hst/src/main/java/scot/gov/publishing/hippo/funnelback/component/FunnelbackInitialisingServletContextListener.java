package scot.gov.publishing.hippo.funnelback.component;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.onehippo.repository.events.PersistedHippoEventListenerRegistry;

public class FunnelbackInitialisingServletContextListener implements ServletContextListener {

    FunnelbackCuratorListener funnelbackCuratorListener;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // register cluster wide event listener to clear the page cache and the CRISP cache when curator change are made in funnelback
        funnelbackCuratorListener = new FunnelbackCuratorListener();
        PersistedHippoEventListenerRegistry.get().register(funnelbackCuratorListener);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        PersistedHippoEventListenerRegistry.get().unregister(funnelbackCuratorListener);
    }
}
