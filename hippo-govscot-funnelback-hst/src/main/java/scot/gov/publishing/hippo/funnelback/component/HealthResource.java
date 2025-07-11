package scot.gov.publishing.hippo.funnelback.component;


import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static com.netflix.hystrix.HystrixEventType.FAILURE;
import static com.netflix.hystrix.HystrixEventType.TIMEOUT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static scot.gov.publishing.hippo.funnelback.component.ResilientSearchService.FUNNELBACK_SEARCH_COMMAND_KEY;
import static scot.gov.publishing.hippo.funnelback.component.ResilientSearchService.FUNNELBACK_SUGGESTIONS_COMMAND_KEY;

@Path("/_search/health")
public class HealthResource {

    @Produces(APPLICATION_JSON)
    @GET
    public Response getHealth() {
        Health health = healthForCommandKey(FUNNELBACK_SEARCH_COMMAND_KEY);
        int status =  health.isOk() ? 200 : 503;
        return Response.status(status).entity(health).build();
    }

    @Produces(APPLICATION_JSON)
    @Path("suggestions")
    @GET
    public Response getSuggestionsHealth() {
        Health health = healthForCommandKey(FUNNELBACK_SUGGESTIONS_COMMAND_KEY);
        int status =  health.isOk() ? 200 : 503;
        return Response.status(status).entity(health).build();
    }

    Health healthForCommandKey(HystrixCommandKey key) {
        Health health = new Health();

        HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory.getInstance(key);
        if (circuitBreaker != null) {
            health.setOk(!circuitBreaker.isOpen());
        }

        HystrixCommandMetrics commandMetrics = HystrixCommandMetrics.getInstance(key);
        if (commandMetrics != null) {
            health.setFailures(commandMetrics.getRollingCount(FAILURE));
            health.setTimeouts(commandMetrics.getRollingCount(TIMEOUT));
            health.setLatency75(commandMetrics.getExecutionTimePercentile(75));
        }
        return health;
    }

    class Health {

        long failures = 0;

        long timeouts = 0;

        long latency75 = 0;

        boolean ok = true;

        public long getFailures() {
            return failures;
        }

        public void setFailures(long failures) {
            this.failures = failures;
        }

        public long getTimeouts() {
            return timeouts;
        }

        public void setTimeouts(long timeouts) {
            this.timeouts = timeouts;
        }

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }

        public long getLatency75() {
            return latency75;
        }

        public void setLatency75(long latency75) {
            this.latency75 = latency75;
        }
    }
}