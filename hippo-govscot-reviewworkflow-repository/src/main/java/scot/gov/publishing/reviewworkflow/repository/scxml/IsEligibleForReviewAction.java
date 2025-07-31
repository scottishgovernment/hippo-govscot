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

public class IsEligibleForReviewAction extends AbstractDocumentTaskAction<IsEeligibleForReviewTask> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    public void setId(String id) {
        setParameter("id", id);
    }

    public String getId() {
        return getParameter("id");
    }

    @Override
    protected IsEeligibleForReviewTask createWorkflowTask() {
        return new IsEeligibleForReviewTask();
    }

    @Override
    protected void initTask(IsEeligibleForReviewTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setId(eval(getId()));
    }
}
