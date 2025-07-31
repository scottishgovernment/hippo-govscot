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
package scot.gov.publishing.reviewworkflow.repository.documentworkflow;

import java.util.Calendar;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.bloomreach.forge.reviewworkflow.ReviewWorkflowNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.Request;
import org.onehippo.repository.util.JcrConstants;

public class ReviewRequest extends Request {

    public ReviewRequest(Node node) throws RepositoryException {
        super(node);
    }

    public static Node newRequestNode(Node parent) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(parent);
        Node requestNode = parent.addNode(HippoStdPubWfNodeType.HIPPO_REQUEST, ReviewWorkflowNodeType.REVIEWWORKFLOW_REQUEST);
        requestNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        return requestNode;
    }

    public ReviewRequest(Node sibling, DocumentVariant document, String owner, String assignTo) throws RepositoryException {
        super(newRequestNode(sibling.getParent()));

        setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_OWNER, owner);
        if (assignTo.startsWith("auto_")) {
            setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_ASSIGNTO, assignTo.replace("auto_", ""));
        } else {
            setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_ASSIGNTO, assignTo);
        }

        setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_UUID, UUID.randomUUID().toString());
        setBooleanProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_CHECKED, false);
        setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_STATE, "request");
        setDateProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_CREATIONDATE, Calendar.getInstance().getTime());
        if (document != null) {
            getCheckedOutNode().setProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_DOCUMENT, document.getNode());
        }
    }

    public ReviewRequest(final Node sibling, final DocumentVariant document, final String owner, final String assignTo, final String uuid) throws RepositoryException {
        this(sibling, document, owner, assignTo);
        setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_UUID, uuid);
        setBooleanProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_CHECKED, false);
    }


    public Boolean getIsStateRequest() throws RepositoryException {
        return "request".equals(getStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_STATE));
    }

    public Boolean getIsStateRejected() throws RepositoryException {
        return "rejected".equals(getStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_STATE));
    }

    public String getOwner() throws RepositoryException {
        return getStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_OWNER);
    }

    public String getAssignTo() throws RepositoryException {
        return getStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_ASSIGNTO);
    }

    public void setRejected(String reviewedBy, String reason) throws RepositoryException {
        setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_STATE, HippoStdPubWfNodeType.REJECTED);
        setNodeProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_DOCUMENT, null);
        setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_REVIEWEDBY, reviewedBy);
        setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_ASSIGNTO, getStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_OWNER));

        if (reason != null && reason.isEmpty()) {
            // empty reason should not be stored.
            reason = null;
        }
        setStringProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_REASON, reason);
    }


}