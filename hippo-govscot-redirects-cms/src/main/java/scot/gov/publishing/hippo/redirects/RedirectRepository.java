package scot.gov.publishing.hippo.redirects;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.Optional;

public interface RedirectRepository {

    Optional<Redirect> lookup(String path) throws RepositoryException;

    void save(Collection<Redirect> redirects) throws RepositoryException;

    boolean delete(String path) throws RepositoryException;

    RedirectResult list(String path) throws RepositoryException;
}
