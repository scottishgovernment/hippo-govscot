/*
 * Copyright 2024 Bloomreach (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scot.gov.publishing.reviewworkflow.hst;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("/preview/")
public class ExampleDocumentPreviewUrlService extends AbstractResource {

    private static Logger log = LoggerFactory.getLogger(ExampleDocumentPreviewUrlService.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/uuid/{uuid}")
    public String getDocumentResource(@Context HttpServletRequest servletRequest,
                                      @Context HttpServletResponse servletResponse,
                                      @Context UriInfo uriInfo,
                                      @PathParam("uuid") String uuid) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final HstLinkCreator hstLinkCreator = requestContext.getHstLinkCreator();
        final Mount mount = requestContext.getMount("reviewworkflow", "preview");

        Node nodeByIdentifier = null;
        try {
            nodeByIdentifier = requestContext.getSession().getNodeByIdentifier(uuid);
        } catch (RepositoryException e) {
            log.error("error while trying to retrieve node identifier {}", e.getMessage(), e);
        }

        final HstLink hstLink = hstLinkCreator.create(nodeByIdentifier, mount);
        return hstLink.toUrlForm(requestContext, true);
    }

}
