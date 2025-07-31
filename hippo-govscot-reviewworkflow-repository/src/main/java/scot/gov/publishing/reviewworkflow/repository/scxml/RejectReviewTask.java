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

import javax.jcr.RepositoryException;

import scot.gov.publishing.reviewworkflow.repository.documentworkflow.ReviewRequest;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;

/**
 * Custom workflow task for rejecting a request
 */
public class RejectReviewTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private ReviewRequest request;
    private String reason;
    private String reviewer;

    public ReviewRequest getRequest() {
        return request;
    }

    public void setRequest(final ReviewRequest request) {
        this.request = request;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    @Override
    public Object doExecute() throws RepositoryException {
        getRequest().setRejected(reviewer, getReason());
        return null;
    }

    public void setReviewer(final String reviewer) {
        this.reviewer = reviewer;
    }

    public String getReviewer() {
        return reviewer;
    }
}
