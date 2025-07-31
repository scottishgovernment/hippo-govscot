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

public class OnlineRequestReviewAction extends AbstractDocumentTaskAction<OnlineRequestReviewTask> {

    private static final long serialVersionUID = 1L;


    public String getContextVariantExpr() {
        return getParameter("contextVariantExpr");
    }

    @SuppressWarnings("unused")
    public void setContextVariantExpr(String contextVariantExpr) {
        setParameter("contextVariantExpr", contextVariantExpr);
    }

    public String getEmail() {
        return getParameter("email");
    }

    @SuppressWarnings("unused")
    public void setEmail(String email) {
        setParameter("email", email);
    }

    public String getUuid() {
        return getParameter("uuid");
    }

    @SuppressWarnings("unused")
    public void setUuid(String uuid) {
        setParameter("uuid", uuid);
    }

    @Override
    protected OnlineRequestReviewTask createWorkflowTask() {
        return new OnlineRequestReviewTask();
    }

    @Override
    protected void initTask(OnlineRequestReviewTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setContextVariant(eval(getContextVariantExpr()));
        task.setEmail(eval(getEmail()));
        task.setUuid(eval(getUuid()));
    }
}

