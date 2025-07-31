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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.bloomreach.forge.reviewworkflow.ReviewWorkflowNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.Request;

public class ReviewWorkflowDocumentHandle extends DocumentHandle {

    private Set<String> groups = new HashSet<>(); //will hold current user's groups

    public ReviewWorkflowDocumentHandle(final Node handle) {
        super(handle);
    }

    @Override
    protected void initializeRequestStatus() throws RepositoryException {
        super.initializeRequestStatus();
        for (Node requestNode : new NodeIterable(getHandle().getNodes(HippoStdPubWfNodeType.HIPPO_REQUEST))) {
            Request request = createRequest(requestNode);
            if (request instanceof final ReviewRequest reviewRequest && ReviewWorkflowNodeType.REVIEWWORKFLOW_REQUEST.equals(request.getRequestType())) {
                if (reviewRequest.getIsStateRequest()) {
                    getRequests().put(request.getIdentity(), request);
                    setRequestPending(true);
                }
                if (reviewRequest.getIsStateRejected()) {
                    getRequests().put(request.getIdentity(), request);
                    setRequestPending(true);
                }
            }
        }
    }

    @Override
    protected Request createRequest(final Node node) throws RepositoryException {
        if (node.isNodeType(ReviewWorkflowNodeType.REVIEWWORKFLOW_REQUEST)) {
            return new ReviewRequest(node);
        }
        return super.createRequest(node);
    }

    public void setGroups(final Set<String> groups) {
        this.groups = groups;
    }

    @SuppressWarnings("unused")
    public Set<String> getGroups() {
        return groups;
    }

    @Override
    public void reset() {
        super.reset();
    }
}
