package scot.gov.publishing.hippo.redirects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.jcr.FeatureFlag;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collection;
import java.util.Optional;

/**
 * {@link RedirectRepository} decorator that routes writes to one or both of a legacy and a new
 * repository, controlled by {@link FeatureFlag} instance at runtime.
 *
 * <p>The feature flag is cached for {@value FLAG_CACHE_TTL_MS} ms to avoid a JCR lookup on every
 * request.
 */
public class SwitchingRedirectRepository implements RedirectRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchingRedirectRepository.class);

    static final long FLAG_CACHE_TTL_MS = 5 * 60 * 1000L;

    private final RedirectRepository legacyRepository;
    private final RedirectRepository newRepository;
    private final FeatureFlag newFlag;

    private static volatile Boolean cachedFlagValue;
    private static volatile long flagLastChecked;

    public SwitchingRedirectRepository(Session session) {
        this.legacyRepository = new LegacyRedirectsRepository(session);
        this.newRepository = new JcrRedirectRepository(session);
        newFlag = new FeatureFlag(session, JcrRedirectRepository.class.getSimpleName());
    }

    @Override
    public Optional<Redirect> lookup(String path) throws RepositoryException {
        return repository().lookup(path);
    }

    @Override
    public void save(Collection<Redirect> redirects) throws RepositoryException {
        repository().save(redirects);
    }

    @Override
    public boolean delete(String path) throws RepositoryException {
        return repository().delete(path);
    }

    @Override
    public RedirectResult list(String path) throws RepositoryException {
        return repository().list(path);
    }

    private RedirectRepository repository() {
        long now = System.currentTimeMillis();
        if (cachedFlagValue == null || now - flagLastChecked > FLAG_CACHE_TTL_MS) {
            cachedFlagValue = newFlag.isEnabled();
            flagLastChecked = now;
            LOG.info("feature flag refreshed: using {}", cachedFlagValue ? newRepository.getClass().getName() : legacyRepository.getClass().getName());
        }
        return cachedFlagValue ? newRepository : legacyRepository;
    }
}
