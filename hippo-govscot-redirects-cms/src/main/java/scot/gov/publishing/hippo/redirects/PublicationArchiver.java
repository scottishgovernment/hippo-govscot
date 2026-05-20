package scot.gov.publishing.hippo.redirects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Builds and saves the set of redirects needed to archive a publication.
 *
 * <p>A publication lives at {@code /publications/{slug}} and may also have:
 * <ul>
 *   <li>page URLs at {@code /publications/{slug}/pages/{page-name}}</li>
 *   <li>a documents index at {@code /publications/{slug}/documents} (only when
 *       the publication has both a {@code pages} folder and a {@code documents} folder)</li>
 *   <li>an ISBN URL at {@code /isbn/{isbn}} (only when the publication carries a
 *       {@code govscot:isbn} property)</li>
 * </ul>
 *
 * <p>The publication is located in the JCR content tree by querying for a
 * {@code govscot:Publication} node whose {@code govscot:slug} matches the slug
 * extracted from the supplied URL.
 */
public class PublicationArchiver {

    private static final Logger LOG = LoggerFactory.getLogger(PublicationArchiver.class);

    private static final String PUBLICATIONS_PREFIX = "/publications/";
    private static final String SLUG_QUERY_TEMPLATE =
            "//element(*, govscot:Publication)[@govscot:slug='%s']";

    private final Session session;
    private final RedirectRepository redirectRepository;

    public PublicationArchiver(Session session, RedirectRepository redirectRepository) {
        this.session = session;
        this.redirectRepository = redirectRepository;
    }

    /**
     * Validates the request fields before any JCR access.
     *
     * @return a list of violation messages; empty if the request is valid
     */
    public List<String> validate(ArchivePublicationRequest request) {
        List<String> violations = new ArrayList<>();
        if (isBlank(request.getUrl())) {
            violations.add("url is required");
        } else if (extractSlug(request.getUrl()) == null) {
            violations.add("url must include a /publications/<slug> path: " + request.getUrl());
        }
        if (isBlank(request.getRedirectUrl())) {
            violations.add("redirectUrl is required");
        }
        return violations;
    }

    /**
     * Expands a single redirect into the full set of redirects needed to archive a publication.
     *
     * <p>If {@code redirect.getFrom()} is a publication URL (contains a {@code /publications/<slug>}
     * segment), the publication is looked up in JCR and the redirect is expanded to cover
     * the publication root, all its pages, the documents index (when applicable), and the
     * ISBN URL (when the publication carries a {@code govscot:isbn} property) — all pointing
     * to the same {@code redirect.getTo()} target.
     *
     * <p>If the URL is not a publication URL, or the publication cannot be found in JCR, the
     * original redirect is returned unchanged as a singleton list.
     *
     * @return one or more redirects derived from the input; never empty
     */
    public List<Redirect> expand(Redirect redirect) throws RepositoryException {
        String slug = extractSlug(redirect.getFrom());
        if (slug == null) {
            return Collections.singletonList(redirect);
        }
        // Only expand the publication root URL. Sub-paths (pages, documents, etc.) are
        // explicit entries and should be kept as-is.
        if (!redirect.getFrom().equals(PUBLICATIONS_PREFIX + slug)) {
            return Collections.singletonList(redirect);
        }
        Node variant = findPublicationVariant(slug);
        if (variant == null) {
            LOG.info("No publication found for slug '{}', using redirect as-is", slug);
            return Collections.singletonList(redirect);
        }
        return buildRedirects(slug, variant, redirect.getTo());
    }

    /**
     * Finds the publication, builds its redirects, saves them and returns the list.
     *
     * @return the saved redirects, or {@code null} if no publication was found for the slug
     */
    public List<Redirect> archive(ArchivePublicationRequest request) throws RepositoryException {
        String slug = extractSlug(request.getUrl());
        String to = normalizeRedirectUrl(request.getRedirectUrl());

        Node variant = findPublicationVariant(slug);
        if (variant == null) {
            LOG.info("No publication found for slug '{}'", slug);
            return Collections.emptyList();
        }

        List<Redirect> redirects = buildRedirects(slug, variant, to);
        redirectRepository.save(redirects);
        return redirects;
    }

    /**
     * Extracts the publication slug from a full URL or a path.
     *
     * <p>Accepts either {@code https://www.gov.scot/publications/mypub} or
     * {@code /publications/mypub}. Returns {@code null} if the path does not
     * contain a {@code /publications/} segment.
     */
    String extractSlug(String url) {
        String path = url;
        if (url.contains("://")) {
            int schemeEnd = url.indexOf("://");
            int pathStart = url.indexOf('/', schemeEnd + 3);
            if (pathStart < 0) {
                return null;
            }
            path = url.substring(pathStart);
        }
        if (!path.startsWith(PUBLICATIONS_PREFIX)) {
            return null;
        }
        String afterPrefix = path.substring(PUBLICATIONS_PREFIX.length());
        if (afterPrefix.isEmpty()) {
            return null;
        }
        int slash = afterPrefix.indexOf('/');
        String slug = slash < 0 ? afterPrefix : afterPrefix.substring(0, slash);
        return slug.isEmpty() ? null : slug;
    }

    /**
     * Strips any nested {@code http://} or {@code https://} that appears in the
     * path portion of archive URLs, e.g.:
     * <pre>
     * https://webarchive.nrscotland.gov.uk/.../https://www.gov.scot/publications/mypub/
     * →
     * https://webarchive.nrscotland.gov.uk/.../www.gov.scot/publications/mypub/
     * </pre>
     */
    String normalizeRedirectUrl(String url) {
        return url.replaceAll("(?<=/)(https?://)", "");
    }

    /**
     * Queries for the {@code govscot:Publication} variant node with the given slug.
     *
     * @return the variant node, or {@code null} if not found
     */
    Node findPublicationVariant(String slug) throws RepositoryException {
        String xpath = String.format(SLUG_QUERY_TEMPLATE, slug);
        NodeIterator nodes = session.getWorkspace().getQueryManager()
                .createQuery(xpath, Query.XPATH)
                .execute()
                .getNodes();
        return nodes.hasNext() ? nodes.nextNode() : null;
    }

    /**
     * Builds the full set of redirects for the publication:
     * <ol>
     *   <li>The publication URL itself</li>
     *   <li>Each page URL (if a {@code pages} folder is present and non-empty)</li>
     *   <li>The {@code /documents} URL (only when both a {@code pages} and a
     *       {@code documents} folder are present)</li>
     *   <li>The {@code /isbn/{isbn}} URL (only when the variant carries a
     *       {@code govscot:isbn} property)</li>
     * </ol>
     *
     * @param variant the {@code govscot:Publication} variant node
     */
    List<Redirect> buildRedirects(String slug, Node variant, String to)
            throws RepositoryException {
        // variant → handle → publication folder
        Node publicationFolder = variant.getParent().getParent();

        List<Redirect> redirects = new ArrayList<>();
        String pubPath = PUBLICATIONS_PREFIX + slug;

        redirects.add(redirect(pubPath, to));

        if (publicationFolder.hasNode("pages")) {
            Node pagesFolder = publicationFolder.getNode("pages");
            NodeIterator pageHandles = pagesFolder.getNodes();
            while (pageHandles.hasNext()) {
                Node pageHandle = pageHandles.nextNode();
                redirects.add(redirect(pubPath + "/pages/" + pageHandle.getName(), to));
            }

            if (publicationFolder.hasNode("documents")) {
                redirects.add(redirect(pubPath + "/documents", to));
            }
        }

        if (variant.hasProperty("govscot:isbn")) {
            String isbn = variant.getProperty("govscot:isbn").getString();
            redirects.add(redirect("/isbn/" + isbn, to));
        }

        return redirects;
    }

    private Redirect redirect(String from, String to) {
        Redirect r = new Redirect();
        r.setFrom(from);
        r.setTo(to);
        return r;
    }
}
