package scot.gov.publishing.staging.valves;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static scot.gov.publishing.staging.valves.PreviewKeyUtils.isPreviewMount;

public class PreviewValve extends AbstractOrderableValve {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewValve.class);

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HstRequestContext requestContext = context.getRequestContext();
        Mount resolvedMount = requestContext.getResolvedMount().getMount();

        boolean invokeNext = true;
        try {
            if (isPreviewMount(requestContext)) {
                invokeNext = doInvokeWithExceptionHandling(context, requestContext, resolvedMount);
            }
        } catch (IOException e) {
            LOG.error("IO IOException invoking preview valve for {}.", requestContext.getSiteContentBaseBean(), e);
        } finally {
            if (invokeNext) {
                context.invokeNext();
            }
        }
    }

    boolean doInvokeWithExceptionHandling(ValveContext valveContext, HstRequestContext requestContext, Mount resolvedMount) throws IOException {
        try {
            return doInvoke(valveContext, requestContext, resolvedMount);
        } catch (Exception e) {
            // if anything goes wring, default to not allowing access
            LOG.error("Exception while accessing this node {}.", requestContext.getSiteContentBaseBean(), e);
            return false;
        }
    }

    boolean doInvoke(ValveContext valveContext, HstRequestContext requestContext, Mount resolvedMount)
            throws RepositoryException, IOException {

        //fetching the previewkey
        Set<String> previewKeys = PreviewKeyUtils.getPreviewKeys(
                valveContext.getServletRequest(), valveContext.getServletResponse(), resolvedMount);

        // intercepting requests having the id in the url
        if (previewKeys.isEmpty()) {
            LOG.info("Preview request doesn't contain preview key");
            requestContext.getServletResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (isExempt(requestContext)) {
            return true;
        }

        HippoBean contentBean = getContentBean(requestContext);
        if (contentBean == null) {
            LOG.info("Preview request doesn't contain content bean");
            requestContext.getServletResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        if (!hasValidStagingKey(contentBean, previewKeys)) {
            LOG.info("Preview key {} for document {} is invalid or preview link has expired.", previewKeys, contentBean.getPath());
            requestContext.getServletResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    HippoBean getContentBean(HstRequestContext requestContext) {
        HippoBean contentBean = requestContext.getContentBean();

        // special case for /publication/slug/documents urls: content bean is a folder which does not have a
        // preview key so return the publication
        if (contentBean.isHippoFolderBean() && "documents".equals(contentBean.getName())) {
            HippoFolderBean documentFolder = (HippoFolderBean) contentBean;
            HippoFolderBean publicationFolder = (HippoFolderBean) documentFolder.getParentBean();
            List<HippoDocumentBean> documents = publicationFolder.getDocuments();
            Optional<HippoDocumentBean> publication = documents.stream().filter(this::isPublicaiton).findFirst();
            return publication.isPresent() ? publication.get() : null;
        }
        return contentBean;
    }

    boolean isPublicaiton(HippoDocumentBean bean) {
        try {
            return bean.getNode().isNodeType("govscot:Publication");
        }catch (RepositoryException e) {
            LOG.error("Unexpected exception trying to find publication", e);
            return false;
        }
    }
    /**
     * anything starting with /fragments is exempt as it is a dynamic endpoint and have to implement its
     * own logic determining visibility.
     */
    boolean isExempt(HstRequestContext context) {
        return StringUtils.startsWith(context.getBaseURL().getPathInfo(), "/fragments/");
    }

    boolean hasValidStagingKey(HippoBean contentBean, Set<String> previewKeys) throws RepositoryException {
        if (contentBean.isHippoFolderBean()) {
            return false;
        }

        Node unpublishedNode = getUnpublishedNode(contentBean);
        if (unpublishedNode == null) {
            return false;
        }

        NodeIterator iterator = unpublishedNode.getNodes("previewId");
        while (iterator.hasNext()) {
            Node node = iterator.nextNode();
            Calendar expirationCalendar = JcrUtils.getDateProperty(node, "staging:expirationdate", null);
            String key = JcrUtils.getStringProperty(node, "staging:key", "");
            if (previewKeys.contains(key) && isValid(expirationCalendar)) {
                return true;
            }
        }
        return false;
    }

    Node getUnpublishedNode(HippoBean contentBean) throws RepositoryException {
        Node handle = contentBean.getNode().getParent();
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            final String state = JcrUtils.getStringProperty(variant, HippoStdNodeType.HIPPOSTD_STATE, null);
            if (HippoStdNodeType.UNPUBLISHED.equals(state)) {
                return variant;
            }
        }
        return null;
    }

    private boolean isValid(final Calendar expirationCalendar) {
        return expirationCalendar == null || Calendar.getInstance().before(expirationCalendar);
    }

}

