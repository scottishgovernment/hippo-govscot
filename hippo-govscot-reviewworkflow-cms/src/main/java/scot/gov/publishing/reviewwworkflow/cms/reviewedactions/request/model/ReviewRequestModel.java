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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.bloomreach.forge.reviewworkflow.ReviewWorkflowNodeType;
import org.hippoecm.frontend.plugins.reviewedactions.model.Request;
import org.hippoecm.frontend.plugins.reviewedactions.model.RequestModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviewRequestModel extends RequestModel {

    private static final Logger log = LoggerFactory.getLogger(ReviewRequestModel.class);

    public ReviewRequestModel(final String id, final Map<String, ?> info) {
        super(id, info);
    }

    @Override
    protected Request load() {
        Request request = super.load();
        try {
            Session session = UserSession.get().getJcrSession();
            Node node = session.getNodeByIdentifier(request.getId());
            if (node.isNodeType(ReviewWorkflowNodeType.REVIEWWORKFLOW_REQUEST)) {
                request = new ReviewRequestWrapper(request, node);
            }
        } catch (RepositoryException e) {
            log.error("Error loading the request in the model",e);
        }
        return request;
    }
}
