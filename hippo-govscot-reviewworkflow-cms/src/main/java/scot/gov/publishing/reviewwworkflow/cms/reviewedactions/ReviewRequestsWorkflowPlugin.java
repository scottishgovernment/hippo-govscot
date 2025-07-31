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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.bloomreach.forge.reviewworkflow.ReviewWorkflowNodeType;
import scot.gov.publishing.reviewwworkflow.cms.reviewedactions.request.model.ReviewRequestModel;
import scot.gov.publishing.reviewwworkflow.cms.reviewedactions.request.model.ReviewRequestWrapper;
import org.bloomreach.forge.reviewworkflow.cms.workflow.ReviewWorkflow;
import org.hippoecm.addon.workflow.ConfirmDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.TextDialog;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.AbstractDocumentWorkflowPlugin;
import org.hippoecm.frontend.plugins.reviewedactions.model.Request;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.util.DocumentUtils;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow menu plugin for the Review Workflow
 */
public class ReviewRequestsWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    private static final Logger log = LoggerFactory.getLogger(ReviewRequestsWorkflowPlugin.class);

    private final String internalAssignListLocation;
    private final String onlineRequestListLocation;

    public ReviewRequestsWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final boolean requestReviewEnabled = config.getAsBoolean("requestReview.enabled", false);
        final boolean onlineRequestReviewEnabled = config.getAsBoolean("onlineRequestReview.enabled", false);
        final boolean multipleReviewsEnabled = config.getAsBoolean("multipleReviewRequests.enabled", false);
        final int maxRequests = config.getAsInteger("multipleReviewRequests.limit", 0);
        final boolean acceptReviewEnabled = config.getAsBoolean("acceptReview.enabled", false);
        final boolean cancelReviewEnabled = config.getAsBoolean("cancelReview.enabled", false);
        final boolean rejectReviewEnabled = config.getAsBoolean("rejectReview.enabled", false);
        final boolean dropReviewEnabled = config.getAsBoolean("dropReview.enabled", false);

        this.internalAssignListLocation = config.getString("internal.assign.list.path");
        this.onlineRequestListLocation = config.getString("online.request.list.path");

        final Map<String, Serializable> info = getHints();

        WorkflowDescriptorModel model = getModel();
        Workflow workflow = model.getWorkflow();

        int numberOfRequests = 0;
        if (workflow != null && info != null) {
            if (info.containsKey("requests")) {
                Map<String, Map<String, ?>> infoRequests = (Map<String, Map<String, ?>>) info.get("requests");

                List<ReviewRequestWrapper> allReviewRequests = new ArrayList<>();
                for (Map.Entry<String, Map<String, ?>> entry : infoRequests.entrySet()) {
                    final ReviewRequestModel reviewRequestModel = new ReviewRequestModel(entry.getKey(), entry.getValue());
                    Request request = reviewRequestModel.getObject();
                    if (request instanceof final ReviewRequestWrapper reviewRequest) {
                        numberOfRequests++;
                        if (isStateReviewRejected(request) && dropReviewEnabled && isActionAllowed(info, "dropReview")) {
                            createDropReview(reviewRequest);
                        }
                        if (isStateReviewRequest(request)) {

                            if (acceptReviewEnabled && canCreateWorkflowAction(request, "acceptReview")) {
                                createAcceptReview(reviewRequest);
                            }

                            if (rejectReviewEnabled && canCreateWorkflowAction(request, "rejectReview")) {
                                createRejectReview(reviewRequest);
                            }

                            if (cancelReviewEnabled && canCreateWorkflowAction(request, "cancelReview")) {
                                createCancelReview(reviewRequest);
                                allReviewRequests.add(reviewRequest);
                            }
                        }
                    }
                }

                if (allReviewRequests.size() > 1) {
                    createCancelAllReviews(allReviewRequests);
                }
            }
            if (requestReviewEnabled && canCreateRequestReview(multipleReviewsEnabled, numberOfRequests, maxRequests) && isActionAllowed(info, "requestReview")) {
                createRequestReview();
            }

            if (onlineRequestReviewEnabled && canCreateRequestReview(multipleReviewsEnabled, numberOfRequests, maxRequests) && isActionAllowed(info, "onlineRequestReview")) {
                createRequestReviewOnline();
            }
        }
    }

    protected void createRequestReview() {
        add(new StdWorkflow("requestReview", new StringResourceModel("request-review", this, null), getPluginContext(), getModel()) {

            public String user = ""; //this variable should not be final as it is set later by the dialog

            @Override
            public String getSubMenu() {
                return new StringResourceModel("review", ReviewRequestsWorkflowPlugin.this).getString();
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.CHECK_CIRCLE);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                try {
                    Node handle = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    Node unpublished = getVariant(handle, WorkflowUtils.Variant.UNPUBLISHED);
                    final IModel<String> titleModel = new StringResourceModel("assign-to-title", ReviewRequestsWorkflowPlugin.this, getDocumentName());
                    return new InternalAssignDialog(this, new JcrNodeModel(unpublished),
                           PropertyModel.of(this, "user"), titleModel, internalAssignListLocation);
                } catch (RepositoryException e) {
                    log.error("Cannot create request dialog", e);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                ReviewWorkflow workflow = (ReviewWorkflow) wf;
                workflow.requestReview(user);
                return null;
            }
        });
    }

    protected void createRequestReviewOnline() {
        add(new StdWorkflow("onlineRequestReview", new StringResourceModel("request-review-online", this, null), getPluginContext(), getModel()) {

            public String user = ""; //this variable should not be final as it is set later by the dialog

            @Override
            public String getSubMenu() {
                return new StringResourceModel("review", ReviewRequestsWorkflowPlugin.this, null).getString();
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                try {
                    Node handle = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    Node unpublished = getVariant(handle, WorkflowUtils.Variant.UNPUBLISHED);
                    final IModel<String> titleModel = new StringResourceModel("email-to", ReviewRequestsWorkflowPlugin.this, getDocumentName());
                    return new OnlineRequestDialog(this, new JcrNodeModel(unpublished),
                            PropertyModel.of(this, "user"), titleModel, onlineRequestListLocation);
                } catch (RepositoryException e) {
                    log.error("cannot create request dialog", e);
                }
                return null;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.CHECK_CIRCLE);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                ReviewWorkflow workflow = (ReviewWorkflow) wf;
                workflow.requestReviewOnline(user, UUID.randomUUID().toString());
                return null;
            }
        });
    }

    protected void createCancelReview(final ReviewRequestWrapper request) {

        add(new StdWorkflow("cancelReview" + request.getId(), new StringResourceModel("cancel-review", this, Model.of(request)), getPluginContext(), getModel()) {

            @Override
            public String getSubMenu() {
                return new StringResourceModel("review", ReviewRequestsWorkflowPlugin.this).getString();
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TIMES);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                ReviewWorkflow workflow = (ReviewWorkflow) wf;
                workflow.cancelReview(request.getId());
                return null;
            }
        });
    }

    protected void createCancelAllReviews(final List<ReviewRequestWrapper> allReviewRequests) {
        add(new StdWorkflow("cancelAllReviewRequests", new StringResourceModel("cancel-all-review-requests", this, null), getPluginContext(), getModel()) {

            @Override
            public String getSubMenu() {
                return new StringResourceModel("review", this, null).getString();
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TIMES);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (wf instanceof final ReviewWorkflow workflow) {
                    for (Request request : allReviewRequests) {
                        workflow.cancelReview(request.getId());
                    }
                }
                return null;
            }
        });
    }

    protected void createRejectReview(final ReviewRequestWrapper request) {
        add(new StdWorkflow("rejectReview" + request.getId(), new StringResourceModel("reject-review", this, Model.of(request)), getPluginContext(), getModel()) {

            public String reason;

            @Override
            public String getSubMenu() {
                return new StringResourceModel("review", ReviewRequestsWorkflowPlugin.this).getString();
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.MINUS_CIRCLE);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                final StringResourceModel title = new StringResourceModel("rejected-review-title", ReviewRequestsWorkflowPlugin.this, null);
                final StringResourceModel text = new StringResourceModel("rejected-review-text", ReviewRequestsWorkflowPlugin.this, null);
                final StdWorkflow stdWorkflow = this;
                return new TextDialog(
                        title,
                        text,
                        new PropertyModel<>(this, "reason")) {
                    @Override
                    public void invokeWorkflow() throws Exception {
                        stdWorkflow.invokeWorkflow();
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (wf instanceof final ReviewWorkflow workflow) {
                    workflow.rejectReview(request.getId(), reason);
                }
                return null;
            }
        });
    }

    protected void createDropReview(final ReviewRequestWrapper request) {
        add(new StdWorkflow("dropReview" + request.getId(), new StringResourceModel("show-reject", this, Model.of(request)), getPluginContext(), getModel()) {

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                IModel<String> reason = null;
                try {
                    String id = request.getId();
                    Node node = UserSession.get().getJcrSession().getNodeByIdentifier(id);
                    if (node != null && node.hasProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_REASON)) {
                        reason = Model.of(node.getProperty(ReviewWorkflowNodeType.REVIEWWORKFLOW_REASON).getString());
                    }
                } catch (RepositoryException ex) {
                    log.warn(ex.getMessage(), ex);
                }
                if (reason == null) {
                    reason = new StringResourceModel("rejected-request-unavailable", ReviewRequestsWorkflowPlugin.this, null);
                }
                final StdWorkflow cancelAction = this;
                return new ConfirmDialog(
                        new StringResourceModel("rejected-request-title", ReviewRequestsWorkflowPlugin.this, null),
                        new StringResourceModel("rejected-request-text", ReviewRequestsWorkflowPlugin.this, null),
                        reason,
                        new StringResourceModel("rejected-request-question", ReviewRequestsWorkflowPlugin.this, null)) {

                    @Override
                    public void invokeWorkflow() throws Exception {
                        cancelAction.invokeWorkflow();
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (wf instanceof final ReviewWorkflow workflow) {
                    workflow.dropReview(request.getId());
                }
                return null;
            }

            @Override
            public String getSubMenu() {
                return new StringResourceModel("review", ReviewRequestsWorkflowPlugin.this, Model.of(request)).getString();
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TIMES);
            }
        });
    }

    protected void createAcceptReview(final ReviewRequestWrapper request) {
        add(new StdWorkflow("acceptReview" + request.getId(), new StringResourceModel("accept-review", this, Model.of(request)), getPluginContext(), getModel()) {

            @Override
            public String getSubMenu() {
                return new StringResourceModel("review", ReviewRequestsWorkflowPlugin.this).getString();
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.CHECK_CIRCLE);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (wf instanceof final ReviewWorkflow workflow) {
                    workflow.acceptReview(request.getId());
                }
                return null;
            }
        });
    }

    protected IModel<String> getDocumentName() {
        try {
            final IModel<String> result = DocumentUtils.getDocumentNameModel(((WorkflowDescriptorModel) getDefaultModel()).getNode());
            if (result != null) {
                return result;
            }
        } catch (RepositoryException ignored) {
        }
        return new StringResourceModel("unknown", this, null);
    }

    protected boolean canCreateWorkflowAction(final Request request, final String requestType) {
        return isActionAllowed(getHints(), requestType) &&
                Optional.of(getHints())
                        .filter(hints -> hints.containsKey("requests"))
                        .map(hints -> hints.get("requests"))
                        .map(requests -> ((Map<String, Map<String, Boolean>>) requests).get(request.getId()))
                        .map(requestId -> requestId.getOrDefault(requestType, false))
                        .orElse(false);
    }

    private boolean isStateReviewRejected(final Request request) {
        return "review-rejected".equals(request.getState());
    }

    private boolean isStateReviewRequest(final Request request) {
        return "review-request".equals(request.getState());
    }

    private boolean canCreateRequestReview(boolean multipleReviewsEnabled, int numberOfReviewRequests, int maxReviewRequests) {
        return numberOfReviewRequests == 0 || (multipleReviewsEnabled && (maxReviewRequests <= 0 || numberOfReviewRequests < maxReviewRequests));
    }
}
