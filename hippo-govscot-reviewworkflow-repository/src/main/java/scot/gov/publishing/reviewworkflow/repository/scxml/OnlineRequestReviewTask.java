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

public class OnlineRequestReviewTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private DocumentVariant contextVariant;
    private String email;
    private String uuid;

    public void setContextVariant(DocumentVariant contextVariant) {
        this.contextVariant = contextVariant;
    }
    public void setEmail(final String email) {
        this.email = email;
    }
    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public Object doExecute() throws RepositoryException {

        final Node node = contextVariant.getNode(getWorkflowContext().getInternalWorkflowSession());
        ReviewRequest reviewRequest = new ReviewRequest(node, contextVariant, getWorkflowContext().getUserIdentity(), email, uuid);
        final Session internalWorkflowSession = getWorkflowContext().getInternalWorkflowSession();
        getWorkflowContext().getInternalWorkflowSession().getWorkspace().getVersionManager().checkin(contextVariant.getNode(internalWorkflowSession).getPath());
        internalWorkflowSession.save();

        return reviewRequest;
    }

}
