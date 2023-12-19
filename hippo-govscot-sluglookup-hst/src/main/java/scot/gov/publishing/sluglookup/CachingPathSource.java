package scot.gov.publishing.sluglookup;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.concurrent.TimeUnit;

public class CachingPathSource implements PathForSlugSource {

    private static final Logger LOG = LoggerFactory.getLogger(CachingPathSource.class);

    PathForSlugSource pathSource;

    CacheLoader<Key, String> cacheLoader = new CacheLoader<>() {
        @Override
        public String load(Key key) throws RepositoryException {
            return pathSource.get(key.path, key.site, key.type, key.mount);
        }
    };

    LoadingCache<Key, String> cache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(cacheLoader);

    public CachingPathSource(PathForSlugSource pathSource) {
        this.pathSource = pathSource;
    }

    @Override
    public String get(String path, String site, String type, String mount) throws RepositoryException {

        try {
            Key key = new Key(path, site, type, mount);
            return cache.get(key);
        } catch (Exception e) {
            LOG.info("failed to get slug for path {}", path, e);
            return null;
        }
    }

    class Key {

        String path;

        String site;

        String type;

        String mount;

        public Key(String path, String site, String type, String mount) {
            this.path = path;
            this.site = site;
            this.type = type;
            this.mount = mount;

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key key = (Key) o;

            return new EqualsBuilder().append(path, key.path).append(site, key.site).append(type, key.type).append(mount, key.mount).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(path).append(site).append(type).append(mount).toHashCode();
        }
    }


}
