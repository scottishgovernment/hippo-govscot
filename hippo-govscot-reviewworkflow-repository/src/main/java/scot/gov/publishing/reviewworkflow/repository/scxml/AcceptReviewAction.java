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

public class AcceptReviewAction extends AbstractDocumentTaskAction<AcceptReviewTask> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    public void setRequestExpr(String requestExpr) {
        setParameter("requestExpr", requestExpr);
    }

    public String getRequestExpr() {
        return getParameter("requestExpr");
    }

    @SuppressWarnings("unused")
    public void setReviewer(String requestExpr) {
        setParameter("reviewer", requestExpr);
    }

    public String getReviewer() {
        return getParameter("reviewer");
    }

    @Override
    protected AcceptReviewTask createWorkflowTask() {
        return new AcceptReviewTask();
    }

    @Override
    protected void initTask(AcceptReviewTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setRequest(eval(getRequestExpr()));
        task.setReviewer(eval(getReviewer()));
    }
}
