package scot.gov.publishing.hippo.funnelback.component;


import com.netflix.hystrix.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static com.netflix.hystrix.HystrixEventType.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static scot.gov.publishing.hippo.funnelback.component.ResilientSearchService.FUNNELBACK_COMMAND_KEY;

@Path("/_search/health")
public class HealthResource {

    @Produces(APPLICATION_JSON)
    @GET
    public Response getHealth() {
        Health health = new Health();

        HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory.getInstance(FUNNELBACK_COMMAND_KEY);
        if (circuitBreaker != null) {
            health.setOk(!circuitBreaker.isOpen());
        }

        HystrixCommandMetrics commandMetrics = HystrixCommandMetrics.getInstance(FUNNELBACK_COMMAND_KEY);
        if (commandMetrics != null) {
            health.setFailures(commandMetrics.getRollingCount(FAILURE));
            health.setTimeouts(commandMetrics.getRollingCount(TIMEOUT));
        }

        int status =  health.isOk() ? 200 : 503;
        return Response.status(status).entity(health).build();
    }

    class Health {

        long failures = 0;

        long timeouts = 0;

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
    }
}