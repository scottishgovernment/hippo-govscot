package scot.gov.publishing.sluglookup;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

public class TimingPathSource implements PathForSlugSource {

    private static final Logger LOG = LoggerFactory.getLogger(TimingPathSource.class);

    PathForSlugSource pathSource;
    public TimingPathSource(PathForSlugSource pathSource) {
        this.pathSource = pathSource;
    }
    @Override
    public String get(String slug, String site, String type, String mount) throws RepositoryException {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        String path = pathSource.get(slug, site, type, mount);
        LOG.info("get {} took {}", slug, stopwatch.getTime());
        return path;
    }
}
