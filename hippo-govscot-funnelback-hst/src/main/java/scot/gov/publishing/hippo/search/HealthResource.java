package scot.gov.publishing.hippo.search;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/_search/health")
public class HealthResource {

    @Produces(APPLICATION_JSON)
    @GET
    public Response getHealth() {
        Health health = healthForCircuitBreaker(ResilientSearchService.SEARCH_CIRCUIT_BREAKER);
        int status = health.isOk() ? 200 : 503;
        return Response.status(status).entity(health).build();
    }

    @Produces(APPLICATION_JSON)
    @Path("suggestions")
    @GET
    public Response getSuggestionsHealth() {
        Health health = healthForCircuitBreaker(ResilientSearchService.SUGGESTIONS_CIRCUIT_BREAKER);
        int status = health.isOk() ? 200 : 503;
        return Response.status(status).entity(health).build();
    }

    Health healthForCircuitBreaker(CircuitBreaker circuitBreaker) {
        Health health = new Health();
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        health.setOk(circuitBreaker.getState() != CircuitBreaker.State.OPEN);
        health.setFailures(metrics.getNumberOfFailedCalls());
        health.setTimeouts(metrics.getNumberOfSlowCalls());
        return health;
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
