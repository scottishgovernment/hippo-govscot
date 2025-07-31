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
package scot.gov.publishing.reviewwworkflow.cms.reviewedactions.request.model;

import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.bloomreach.forge.reviewworkflow.ReviewWorkflowNodeType;
import org.hippoecm.frontend.plugins.reviewedactions.model.Request;

public class ReviewRequestWrapper extends Request {

    private final Node requestNode;

    public ReviewRequestWrapper(final Request request, final Node requestNode) throws RepositoryException {
        super(
                request.getId(),
                request.getSchedule(),
                "review-" + requestNode.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_STATE).getString(),
                Collections.singletonMap("infoRequest", request.getInfo())
        );
        this.requestNode = requestNode;
    }

    public String getOwner() throws RepositoryException {
        return requestNode.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_OWNER).getString();
    }

    public String getAssignTo() throws RepositoryException {
        return requestNode.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_ASSIGNTO).getString();
    }

    public String getReviewedBy() throws RepositoryException {
        return requestNode.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_REVIEWEDBY).getString();
    }

    public String getReason() throws RepositoryException {
        return requestNode.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_REASON).getString();
    }

    @Override
    public Boolean getCancel() {
        return false;
    }
}
