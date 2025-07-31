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
package scot.gov.publishing.reviewworkflow.repository.scxml;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.bloomreach.forge.reviewworkflow.ReviewWorkflowNodeType;
import org.bloomreach.forge.reviewworkflow.cms.workflow.ReviewWorkflowUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsEeligibleForReviewTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(IsEeligibleForReviewTask.class);

    private String id;

    public String getId() {
        return id;
    }

    @Override
    public Object doExecute() {
        try {
            final Session session = getWorkflowContext().getInternalWorkflowSession();
            Node requestNode = ReviewWorkflowUtils.getRequestNodeFromWorkflowId(getId(), session);
            if( requestNode != null) {
                JcrUtils.ensureIsCheckedOut(requestNode.getParent());
                if (requestNode.isNodeType(ReviewWorkflowNodeType.REVIEWWORKFLOW_REQUEST) && StringUtils.isNotEmpty(id)) {
                    requestNode.setProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_CHECKED, true);
                    session.save();
                    return requestNode.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_UUID).getString().equals(getId());
                }
            }
        } catch (Exception e) {
            log.error("error while trying to receive if document is eligable for review ", e);
        }
        return false;
    }

    public void setId(String id) {
        this.id = id;
    }
}
