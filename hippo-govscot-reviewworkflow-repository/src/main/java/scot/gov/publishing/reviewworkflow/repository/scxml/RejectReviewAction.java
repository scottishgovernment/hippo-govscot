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

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.action.AbstractDocumentTaskAction;
import org.onehippo.repository.documentworkflow.task.RejectRequestTask;

/**
 * RejectRequestAction is a custom DocumentWorkflow SCXML state machine action for marking a specific document handle
 * request node as rejected, optionally together with a reason.
 * <p>
 * The execution of this task is delegated to its corresponding {@link RejectRequestTask}.
 * </p>
 */
public class RejectReviewAction extends AbstractDocumentTaskAction<RejectReviewTask> {

    private static final long serialVersionUID = 1L;

    public void setReviewer(String requestExpr) {
        setParameter("reviewer", requestExpr);
    }

    public String getReviewer() {
        return getParameter("reviewer");
    }

    @SuppressWarnings("unused")
    public void setRequestExpr(String requestExpr) {
        setParameter("requestExpr", requestExpr);
    }

    public String getRequestExpr() {
        return getParameter("requestExpr");
    }

    @SuppressWarnings("unused")
    public void setReasonExpr(String reasonExpr) {
        setParameter("reasonExpr", reasonExpr);
    }

    public String getReasonExpr() {
        return getParameter("reasonExpr");
    }

    @Override
    protected RejectReviewTask createWorkflowTask() {
        return new RejectReviewTask();
    }

    @Override
    protected void initTask(RejectReviewTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setRequest(eval(getRequestExpr()));
        task.setReason(eval(getReasonExpr()));
        task.setReviewer(eval(getReviewer()));
    }
}
