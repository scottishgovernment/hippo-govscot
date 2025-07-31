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

import scot.gov.publishing.reviewworkflow.repository.documentworkflow.ReviewRequest;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;

public class RequestReviewTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private String assignTo;
    private DocumentVariant contextVariant;

    public void setAssignTo(final String assignTo) {
        this.assignTo = assignTo;
    }

    public void setContextVariant(DocumentVariant contextVariant) {
        this.contextVariant = contextVariant;
    }

    @Override
    public Object doExecute() throws RepositoryException {

        final Node node = contextVariant.getNode(getWorkflowContext().getInternalWorkflowSession());
        ReviewRequest reviewRequest = new ReviewRequest(node, contextVariant, getWorkflowContext().getUserIdentity(), assignTo);
        final Session internalWorkflowSession = getWorkflowContext().getInternalWorkflowSession();
        internalWorkflowSession.getWorkspace().getVersionManager().checkin(contextVariant.getNode(internalWorkflowSession).getPath());
        internalWorkflowSession.save();
        return reviewRequest;
    }
}
