package scot.gov.publishing.hippo.redirects;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Path("/")
public class RedirectsResource {

    private static final Logger LOG = LoggerFactory.getLogger(RedirectsResource.class);

    private static final String GOV_SCOT_ORIGIN = "https://www.gov.scot";

    private final RedirectRepository redirectRepository;

    private final RedirectValidator redirectValidator = new RedirectValidator();

    private final PublicationArchiver publicationArchiver;

    @Context
    UriInfo uriInfo;

    public RedirectsResource(RedirectRepository redirectRepository, PublicationArchiver publicationArchiver) {
        this.redirectRepository = redirectRepository;
        this.publicationArchiver = publicationArchiver;
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response upload(List<Redirect> redirects) {
        LOG.info("upload redirects {}", redirects.size());
        redirects.forEach(r -> r.setFrom(normalizeFromUrl(r.getFrom())));
        List<String> violations = redirectValidator.validateRedirects(redirects);
        if (!violations.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(violations).build();
        }
        try {
            redirectRepository.save(redirects);
            logRedirects(redirects);
            return Response.status(Response.Status.OK).entity(redirects).build();
        } catch (RepositoryException e) {
            LOG.error("Unexpected exception uploading redirects", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected exception uploading redirects").build();
        }
    }

    @POST
    @Path("csv")
    @Consumes("text/csv")
    @Produces({MediaType.APPLICATION_JSON})
    public Response uploadCsv(@Multipart("file") File file) throws IOException {
        List<Redirect> parsed;
        try (Reader in = new FileReader(file);
             CSVParser csvParser = new CSVParser(in, CSVFormat.DEFAULT)) {
            parsed = csvParser.getRecords().stream().map(this::toRedirect).collect(Collectors.toList());
        }

        List<String> violations = redirectValidator.validateRedirects(parsed);
        if (!violations.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(violations).build();
        }

        try {
            List<Redirect> redirects = expandRedirects(parsed);
            redirectRepository.save(redirects);
            logRedirects(redirects);
            return Response.status(Response.Status.OK).entity(redirects).build();
        } catch (RepositoryException e) {
            LOG.error("Unexpected exception uploading csv", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected exception uploading csv").build();
        }
    }

    private List<Redirect> expandRedirects(List<Redirect> parsed) throws RepositoryException {
        Set<String> explicitPaths = parsed.stream().map(Redirect::getFrom).collect(Collectors.toSet());
        List<Redirect> redirects = new ArrayList<>();
        for (Redirect r : parsed) {
            for (Redirect expanded : publicationArchiver.expand(r)) {
                boolean overriddenByExplicitRow = !expanded.getFrom().equals(r.getFrom())
                        && explicitPaths.contains(expanded.getFrom());
                if (!overriddenByExplicitRow) {
                    redirects.add(expanded);
                }
            }
        }
        return redirects;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getRoot() {
        return doGet("/");
    }

    @GET
    @Path("{path: .+}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get() {
        String path = uriInfo.getPathParameters().getFirst("path");
        return doGet(path);
    }

    @POST
    @Path("archive-publication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response archivePublication(ArchivePublicationRequest request) {
        request.setUrl(normalizeFromUrl(request.getUrl()));
        LOG.info("archive publication: {}", request.getUrl());
        List<String> violations = publicationArchiver.validate(request);
        if (!violations.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(violations).build();
        }
        try {
            List<Redirect> redirects = publicationArchiver.archive(request);
            if (redirects == null) {
                String slug = publicationArchiver.extractSlug(request.getUrl());
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Collections.singletonList("No publication found with slug: " + slug))
                        .build();
            }
            logRedirects(redirects);
            return Response.status(Response.Status.OK).entity(redirects).build();
        } catch (RepositoryException e) {
            LOG.error("Unexpected exception archiving publication", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Unexpected exception archiving publication").build();
        }
    }

    @DELETE
    @Path("{path: .+}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response delete() {
        String path = uriInfo.getPathParameters().getFirst("path");
        try {
            boolean deleted = redirectRepository.delete(path);
            if (deleted) {
                LOG.info("deleted redirect '{}'", path);
                return Response.status(Response.Status.OK).entity("deleted").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("no redirect found").build();
            }
        } catch (RepositoryException e) {
            LOG.error("Unexpected exception deleting redirect", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected exception deleting redirect").build();
        }
    }

    private Response doGet(String path) {
        try {
            RedirectResult result = redirectRepository.list(path);
            if (result == null) {
                RedirectResult notFound = new RedirectResult();
                notFound.setDescription("No redirects found");
                return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
            }
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (RepositoryException e) {
            LOG.error("Unexpected exception fetching redirects", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected exception fetching redirects").build();
        }
    }

    private Redirect toRedirect(CSVRecord record) {
        Redirect redirect = new Redirect();
        redirect.setFrom(normalizeFromUrl(record.get(0)));
        redirect.setTo(record.get(1));
        if (record.size() > 2) {
            redirect.setDescription(record.get(2));
        }
        return redirect;
    }

    /**
     * Normalises a {@code from} URL for storage:
     * <ol>
     *   <li>If the value is a fully-qualified {@code https://www.gov.scot/} URL, the origin is
     *       stripped so only the path is retained.</li>
     *   <li>Any percent-encoded characters in the path are decoded to their literal form (e.g.
     *       {@code %3A} → {@code :}, {@code %2B} → {@code +}), using the same logic as
     *       {@link RedirectNodePath#normalisePath}.  This ensures the stored path matches what
     *       the servlet container hands to the lookup service after URL-decoding the request
     *       URI.</li>
     * </ol>
     */
    static String normalizeFromUrl(String url) {
        if (url == null) {
            return null;
        }
        String path = url.startsWith(GOV_SCOT_ORIGIN + "/")
                ? url.substring(GOV_SCOT_ORIGIN.length())
                : url;
        return RedirectNodePath.normalisePath(path);
    }

    private void logRedirects(List<Redirect> redirects) {
        for (Redirect redirect : redirects) {
            LOG.info("adding redirect '{}' -> '{}' ({})", redirect.getFrom(), redirect.getTo(), description(redirect));
        }
    }

    private String description(Redirect redirect) {
        return isBlank(redirect.getDescription()) ? "no description" : redirect.getDescription();
    }
}
