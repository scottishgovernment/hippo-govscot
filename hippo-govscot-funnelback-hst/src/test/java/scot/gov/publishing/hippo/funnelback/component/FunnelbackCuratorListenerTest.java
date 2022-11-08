package scot.gov.publishing.hippo.funnelback.component;


import org.junit.Test;
import org.mockito.Mockito;
import org.onehippo.cms7.event.HippoEvent;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static scot.gov.publishing.hippo.funnelback.component.FunnelbackCuratorListener.CURATOR_DOC_PATH;

public class FunnelbackCuratorListenerTest {

    @Test
    public void cacheClearedForRelevantEvent() {
        // ARRANGE
        FunnelbackCuratorListener sut = new FunnelbackCuratorListener();
        sut.cacheCleaner = Mockito.mock(FunnelbackCuratorListener.CacheCleaner.class);
        HippoEvent event = event("commitEditableInstance", CURATOR_DOC_PATH);

        // ACT
        sut.onHippoEvent(event);

        // ASSERT
        verify(sut.cacheCleaner).clean();
    }


    @Test
    public void cacheNotClearedForOtherPaths() {
        // ARRANGE
        FunnelbackCuratorListener sut = new FunnelbackCuratorListener();
        sut.cacheCleaner = Mockito.mock(FunnelbackCuratorListener.CacheCleaner.class);
        HippoEvent event = event("commitEditableInstance", "anotherpath");

        // ACT
        sut.onHippoEvent(event);

        // ASSERT
        verify(sut.cacheCleaner, never()).clean();
    }

    @Test
    public void cacheNotClearedForOtherActions() {
        // ARRANGE
        FunnelbackCuratorListener sut = new FunnelbackCuratorListener();
        sut.cacheCleaner = Mockito.mock(FunnelbackCuratorListener.CacheCleaner.class);
        HippoEvent event = event("anotherevent", CURATOR_DOC_PATH);

        // ACT
        sut.onHippoEvent(event);

        // ASSERT
        verify(sut.cacheCleaner, never()).clean();
    }

    HippoEvent event(String action, String path) {
        HippoEvent event = new HippoEvent("");
        event.action(action);
        event.set("subjectPath", path);
        return event;
    }
}
