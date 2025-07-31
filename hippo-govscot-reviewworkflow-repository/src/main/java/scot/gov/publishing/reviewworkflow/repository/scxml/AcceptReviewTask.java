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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.bloomreach.forge.reviewworkflow.ReviewWorkflowNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.Request;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;

public class AcceptReviewTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private Request request;
    private String reviewer;

    public Request getRequest() {
        return request;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setRequest(final Request request) {
        this.request = request;
    }

    @Override
    public Object doExecute() throws RepositoryException {
        final Session session = getWorkflowContext().getInternalWorkflowSession();
        Node requestNode = request.getCheckedOutNode(session);
        JcrUtils.ensureIsCheckedOut(requestNode.getParent());
        if (requestNode.isNodeType(ReviewWorkflowNodeType.REVIEWWORKFLOW_REQUEST)) {
            requestNode.setProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_STATE, "accepted");
            requestNode.setProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_ASSIGNTO, requestNode.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_OWNER).getString());
        }
        if (StringUtils.isNotEmpty(reviewer)) {
            requestNode.setProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_REVIEWEDBY, reviewer);
        }
        session.save();
        return null;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }
}
