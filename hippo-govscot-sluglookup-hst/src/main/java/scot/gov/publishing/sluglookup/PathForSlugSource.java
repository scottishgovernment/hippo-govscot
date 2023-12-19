package scot.gov.publishing.sluglookup;

import javax.jcr.RepositoryException;

/**
 * Interface for class that can perform the task of determining the path to use for a given slug.
 */
public interface PathForSlugSource {
    String get(String slug, String site, String type, String mount) throws RepositoryException;

 }