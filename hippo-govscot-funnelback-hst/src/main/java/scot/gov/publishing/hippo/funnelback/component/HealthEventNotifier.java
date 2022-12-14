package scot.gov.publishing.hippo.funnelback.component;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Listen to events from Hystrix to record the timestamp of the last short circuit event.
 */
public class HealthEventNotifier extends HystrixEventNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(HealthEventNotifier.class);

    private static long lastShortCurcuitTimestamp = 0;

    public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
        if (eventType == HystrixEventType.SHORT_CIRCUITED) {
            LOG.info("Short circuit event for {}", key.name());
            lastShortCurcuitTimestamp = System.currentTimeMillis();
        }
    }

    public void markCommandExecution(HystrixCommandKey key, HystrixCommandProperties.ExecutionIsolationStrategy isolationStrategy, int duration, List<HystrixEventType> eventsDuringExecution) {
        // nothing required
    }

    public static long getLastShortCurcuitTimestamp() {
        return lastShortCurcuitTimestamp;
    }
}
