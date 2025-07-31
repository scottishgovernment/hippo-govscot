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
package scot.gov.publishing.reviewwworkflow.cms.reviewedactions;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.model.ValueList;

public class OnlineRequestDialog extends AbstractAssignDialog {

    public OnlineRequestDialog(final IWorkflowInvoker invoker, final IModel<Node> nodeModel,
                               final IModel<String> assignToModel, final IModel<String> titleModel,
                               final String listLocation) {
        super(invoker, nodeModel, assignToModel, titleModel, listLocation);
    }

    @Override
    protected IModel<? extends List<String>> getDropDownModel(final IModel<Node> nodeModel, final ValueList valueList) throws RepositoryException {
        List<String> list = new ArrayList<>();
        for (ListItem next : valueList) {
            if (!next.getKey().equals(nodeModel.getObject().getSession().getUserID())) {
                list.add(next.getKey());
            }
        }
        return Model.ofList(list);
    }
}
