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
package scot.gov.publishing.reviewwworkflow.cms.dashboard.todo;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.bloomreach.forge.reviewworkflow.ReviewWorkflowNodeType;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLinkTarget;
import org.hippoecm.frontend.plugins.cms.dashboard.todo.TodoLink;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoPlugin extends org.hippoecm.frontend.plugins.cms.dashboard.todo.TodoPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TodoPlugin.class);

    public TodoPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        addOrReplace(new TodoView("view", getDefaultModel()));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
    }

    private final RequestFactory requestFactory = new RequestFactory();

    private class TodoView extends RefreshingView {

        public TodoView(String id, IModel model) {
            super(id, model);
        }

        @Override
        protected Iterator getItemModels() {
            final IDataProvider dataProvider = (IDataProvider) getDefaultModel();
            return dataProvider.iterator(0, 0);
        }

        @Override
        protected void populateItem(final Item item) {
            try {
                @SuppressWarnings("unchecked") final IModel<Node> nodeModel = item.getModel();
                final BrowseLinkTarget target = new BrowseLinkTarget(nodeModel.getObject().getPath());

                final TodoRequest request = requestFactory.createRequest(nodeModel);
                item.add(new TodoLink(getPluginContext(), getPluginConfig(), "link", target,
                        new PropertyModel<>(request, "username"),
                        new PropertyModel<>(request, "localType"),
                        new PropertyModel<>(request, "creationDate")));
            } catch (RepositoryException e) {
                log.error("Failed to create todo item from request node", e);
            }
        }
    }

    public class RequestFactory {

        public TodoRequest createRequest(IModel<Node> model) throws RepositoryException {
            if (model.getObject().isNodeType(ReviewWorkflowNodeType.REVIEWWORKFLOW_REQUEST)) {
                return new ReviewRequest(model, TodoPlugin.this);
            } else {
                return new Request(model, TodoPlugin.this);
            }
        }

    }

    private interface TodoRequest {

        public String getLocalType();

        public String getUsername();

        public Calendar getCreationDate();
    }

    private static class ReviewRequest implements TodoRequest, Serializable {

        private static final long serialVersionUID = 1L;

        private final IModel<Node> nodeModel;
        private final Component container;

        private ReviewRequest(IModel<Node> nodeModel, Component container) {
            this.nodeModel = nodeModel;
            this.container = container;
        }


        @SuppressWarnings("unused")
        public String getLocalType() {
            try {
                Node node = nodeModel.getObject();
                String type = node.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_STATE).getString();
                return container.getString(type);
            } catch (RepositoryException ignored) {
            }
            return null;
        }

        public String getUsername() {
            try {
                Node node = nodeModel.getObject();
                return node.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_ASSIGNTO).getString();
            } catch (RepositoryException ignored) {
            }
            return null;
        }

        /**
         * Get the creation date of the request. Returns a {@link Calendar} object when a date is available, null
         * otherwise.
         *
         * @return the creation date or null
         */
        public Calendar getCreationDate() {
            try {
                Node node = nodeModel.getObject();
                if (node.hasProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_CREATIONDATE)) {
                    return node.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_CREATIONDATE).getDate();
                }
            } catch (RepositoryException e) {
                log.warn("Error while retrieving creation date", e);
            }
            return null;
        }

    }

    private static class Request implements TodoRequest, Serializable {

        private static final long serialVersionUID = 1L;

        private final IModel<Node> nodeModel;
        private final Component container;

        private Request(IModel<Node> nodeModel, Component container) {
            this.nodeModel = nodeModel;
            this.container = container;
        }

        @SuppressWarnings("unused")
        public String getLocalType() {
            try {
                Node node = nodeModel.getObject();
                String type = node.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE).getString();
                return container.getString(type);
            } catch (RepositoryException ignored) {
            }
            return null;
        }

        public String getUsername() {
            try {
                Node node = nodeModel.getObject();
                return node.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME).getString();
            } catch (RepositoryException ignored) {
            }
            return null;
        }

        /**
         * Get the creation date of the request. Returns a {@link Calendar} object when a date is available, null
         * otherwise.
         *
         * @return the creation date or null
         */
        public Calendar getCreationDate() {
            try {
                Node node = nodeModel.getObject();
                if (node.hasProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE)) {
                    return node.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE).getDate();
                }
            } catch (RepositoryException e) {
                log.warn("Error while retrieving creation date", e);
            }
            return null;
        }
    }
}
