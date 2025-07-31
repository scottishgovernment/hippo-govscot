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

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringEscapeUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleReviewWorkflowComponent extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(ExampleReviewWorkflowComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        request.setAttribute("isPreview", request.getRequestContext().isPreview());
        final String workflowId = getPublicRequestParameter(request, "workflowId");
        final HippoBean contentBean = request.getRequestContext().getContentBean();
        ReviewWorkflowActions reviewWorkflowActions = HstServices.getComponentManager().getComponent(ReviewWorkflowActions.class, "org.bloomreach.forge.reviewworkflow");
        boolean eligibleForReview = false;
        try {
            eligibleForReview = reviewWorkflowActions.isEligibleForReview(contentBean, workflowId);
        } catch (RepositoryException ex) {
            log.error("Error checking if is eligible for review", ex);
        }
        request.setAttribute("eligibleForReview", eligibleForReview);
    }

    @Override
    public void doAction(final HstRequest request, final HstResponse response) throws HstComponentException {
        super.doAction(request, response);

        try {
            final String workflowId = getPublicRequestParameter(request, "workflowId");
            final String workflow = getEscapedParameter(request, "workflow");
            final String reason = getEscapedParameter(request, "reason");
            final HippoBean contentBean = request.getRequestContext().getContentBean();
            final ReviewWorkflowActions reviewWorkflowActions = HstServices.getComponentManager().getComponent(ReviewWorkflowActions.class, "org.bloomreach.forge.reviewworkflow");
            reviewWorkflowActions.execute(workflowId, contentBean, "accept".equals(workflow) ? ReviewWorkflowActions.Type.ACCEPT : ReviewWorkflowActions.Type.REJECT, reason);
        } catch (Exception ex) {
            log.error("Error executing the accept/reject wf", ex);
        }
    }

    public static String getEscapedParameter(final HstRequest request, final String parameterName) {
        final String value = request.getParameter(parameterName);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return StringEscapeUtils.escapeHtml(value);
    }

}
