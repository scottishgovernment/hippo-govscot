package scot.gov.publishing.bulkpublish;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.*;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.ArrayUtils.*;
import static org.hippoecm.frontend.plugins.reviewedactions.model.Request.CANCEL_REQUEST;
import static org.hippoecm.repository.HippoStdNodeType.NT_DIRECTORY;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;
import static org.hippoecm.repository.HippoStdPubWfNodeType.DEPUBLISH;
import static org.hippoecm.repository.HippoStdPubWfNodeType.PUBLISH;
import static org.hippoecm.repository.api.HippoNodeType.*;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_REQUEST;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB;

public class ScheduledWorkflowPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledWorkflowPlugin.class);

    private static final String PRIVILEGE_BULKSCHEDULE_USER = "bulkschedule:user";

    private static final String REQUESTS = "requests";

    private static final String WORKFLOW_CATEGORY = "default";

    private String folderName;

    public ScheduledWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        if(userHasAdvancedFolderPrivileges()) {
            add(new SchedulePublishWorkflow(context));
            add(new ScheduleDepublishWorkflow(context));
            add(new CancelAllWorkflow(context));
        }
    }

    private void collectValidDocumentIDs(Node folder, String action, Set<String> nodeIDs) throws RepositoryException, WorkflowException, RemoteException {
        for (Node child : new NodeIterable(folder.getNodes())) {
            collectValidDocumentIDsForChild(child, action, nodeIDs);
        }
    }

    private void collectValidDocumentIDsForChild(Node child, String action, Set<String> nodeIDs) throws RepositoryException, WorkflowException, RemoteException {
        if (child.isNodeType(NT_FOLDER) || child.isNodeType(NT_DIRECTORY)) {
            collectValidDocumentIDs(child, action, nodeIDs);
            return;
        }

        if (!child.isNodeType(NT_HANDLE)) {
            return;
        }

        if (!hasWorkflowPermission(child, action)) {
            return;
        }

        doCollect(child, action, nodeIDs);
    }

    void doCollect(Node child, String action, Set<String> nodeIDs) throws RepositoryException {
        if(PUBLISH.equals(action) && !isDocumentHandleLive(child)) {
            nodeIDs.add(child.getIdentifier());
        } else if (DEPUBLISH.equals(action) && isDocumentHandleLive(child)){
            nodeIDs.add(child.getIdentifier());
        } else if (CANCEL_REQUEST.equals(action) && child.hasNode(HIPPO_REQUEST)){
            collectValidDocumentIDsForCancel(child, nodeIDs);
        }
    }

    boolean hasWorkflowPermission(Node node, String action) throws RepositoryException, WorkflowException, RemoteException {
        WorkflowManager workflowManager = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
        Workflow workflow = workflowManager.getWorkflow(WORKFLOW_CATEGORY, node);
        if (workflow == null) {
            return false;
        }
        if (StringUtils.equals(action, "cancelRequest")) {
            return isPermittedCancelRequest(workflow);
        }

        Serializable hint = workflow.hints().get(action);
        return hint instanceof Boolean && (Boolean) hint;
    }

    boolean isPermittedCancelRequest(Workflow workflow) throws RepositoryException, WorkflowException, RemoteException {
        Serializable hint = workflow.hints().get(REQUESTS);
        if (hint instanceof Map) {
            return !((Map) hint).isEmpty();
        }
        return false;
    }

    private void collectValidDocumentIDsForCancel(Node child, Set<String> nodeIDs) throws RepositoryException {
        NodeIterator requestsNodeIterator = child.getNodes(HIPPO_REQUEST);
        while (requestsNodeIterator.hasNext()) {
            Node requestNode = requestsNodeIterator.nextNode();
            if (requestNode.isNodeType(HIPPOSCHED_WORKFLOW_JOB)) {
                nodeIDs.add(child.getIdentifier());
            }
        }
    }

    private void bulkExecuteDocumentWorkflow(String action, Set<String> nodeIDs, Date date) throws RepositoryException {
        Session session = UserSession.get().getJcrSession();
        for (String uuid : nodeIDs) {
            try {
                Node handle = session.getNodeByIdentifier(uuid);
                if (handle.isNodeType(NT_HANDLE)) {
                    executeWorkflow(handle, action, date);
                }
            } catch (RepositoryException | RemoteException | WorkflowException e) {
                LOG.error("Execution of action {} on {} failed: {}", action, uuid, e);
            }
            session.refresh(true);
        }
    }

    private void executeWorkflow(Node handle, String action, Date date) throws RepositoryException, RemoteException, WorkflowException {
        WorkflowManager wfMgr = ((HippoWorkspace) handle.getSession().getWorkspace()).getWorkflowManager();
        Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, handle);
        if (workflow instanceof DocumentWorkflow) {
            DocumentWorkflow docWorkflow = (DocumentWorkflow) workflow;
            switch (action) {
                case PUBLISH:
                    publish(docWorkflow, date);
                    break;
                case DEPUBLISH:
                    depublish(docWorkflow, date);
                    break;
                case CANCEL_REQUEST:
                    cancel(docWorkflow);
                    break;
                default:
                    LOG.error("unhandled action {}", action);
            }

        }
    }

    private void publish(DocumentWorkflow docWorkflow, Date date) throws WorkflowException, RepositoryException, RemoteException {
        if (date != null) {
            docWorkflow.publish(date);
        } else {
            docWorkflow.publish();
        }
    }

    private void depublish(DocumentWorkflow docWorkflow, Date date) throws WorkflowException, RepositoryException, RemoteException {
        if (date != null) {
            docWorkflow.depublish(date);
        } else {
            docWorkflow.depublish();
        }
    }

    private void cancel(DocumentWorkflow docWorkflow) throws WorkflowException, RepositoryException, RemoteException {
        Map<String, Serializable> info = docWorkflow.hints();
        if (info.containsKey(REQUESTS)) {
            Map<String, Map<String, ?>> infoRequests = (Map<String, Map<String, ?>>) info.get(REQUESTS);
            for (Map.Entry<String, Map<String, ?>> entry : infoRequests.entrySet()) {
                if (entry.getValue().get(CANCEL_REQUEST) != null) {
                    docWorkflow.cancelRequest(entry.getKey());
                }
            }
        }
    }
    private boolean isDocumentHandleLive(Node handle) throws RepositoryException {
        Node liveVariant = getDocumentVariantByHippoStdState(handle, HippoStdNodeType.PUBLISHED);
        if (liveVariant == null) {
            return false;
        }

        String[] availabilities = JcrUtils.getMultipleStringProperty(liveVariant, HIPPO_AVAILABILITY, EMPTY_STRING_ARRAY);
        return ArrayUtils.contains(availabilities, "live");
    }

    private Node getDocumentVariantByHippoStdState(Node handle, String hippoStdState)
            throws RepositoryException {
        Node variantNode = null;
        String state;

        for (NodeIterator nodeIt = handle.getNodes(handle.getName()); nodeIt.hasNext();) {
            variantNode = nodeIt.nextNode();

            if (variantNode.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                state = variantNode.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                if (StringUtils.equals(hippoStdState, state)) {
                    return variantNode;
                }
            }
        }

        return null;
    }

    private boolean userHasAdvancedFolderPrivileges() {
        try {
            String path = ((WorkflowDescriptorModel) getDefaultModel()).getNode().getPath();
            HippoSession hippoSession = UserSession.get().getJcrSession();
            AccessControlManager accessControlManager = hippoSession.getAccessControlManager();
            return accessControlManager.hasPrivileges(path, new Privilege[]{accessControlManager.privilegeFromName(PRIVILEGE_BULKSCHEDULE_USER)});
        } catch (RepositoryException e) {
            LOG.error("Error checking privileges", e);
            return false;
        }
    }

    private class SchedulePublishWorkflow extends StdWorkflow<FolderWorkflow> {

        private Date date;
        private Set<String> nodeIDs;

        public SchedulePublishWorkflow(IPluginContext context) {
            super("schedulePublish",
                    new StringResourceModel("schedule-publish-all-label", ScheduledWorkflowPlugin.this),
                    context, (WorkflowDescriptorModel) ScheduledWorkflowPlugin.this.getModel());
            date = new Date();
            nodeIDs = new HashSet<>();
        }

        @Override
        protected Component getIcon(String id) {
            return HippoIcon.fromSprite(id, Icon.CHECK_CIRCLE);
        }

        @Override
        protected IDialogService.Dialog createRequestDialog() {
            try {
                folderName = ((HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode()).getDisplayName();
                collectValidDocumentIDs(getModel().getNode(), PUBLISH, nodeIDs);
            } catch (RepositoryException | RemoteException | WorkflowException e) {
                LOG.error("Exception while trying to retrieve node.", e);
                folderName = "";
            }

            IModel<String> titleModel = new StringResourceModel("schedule-publish-all-title", ScheduledWorkflowPlugin.this);
            IModel<String> notification = new StringResourceModel("schedule-publish-all-notification", ScheduledWorkflowPlugin.this).setParameters(folderName, nodeIDs.size());
            IModel<String> action = new StringResourceModel("schedule-all-action", ScheduledWorkflowPlugin.this);
            IModel<String> dateText = new StringResourceModel("schedule-publish-all-text", ScheduledWorkflowPlugin.this);
            return new ScheduleBulkDialog(this, ScheduledWorkflowPlugin.super.getModel(),
                    PropertyModel.of(this, "date"), titleModel, notification, action, dateText);
        }

        @Override
        protected String execute(FolderWorkflow workflow) throws Exception {
            bulkExecuteDocumentWorkflow(PUBLISH, nodeIDs, date);
            return null;
        }
    }

    private class ScheduleDepublishWorkflow extends StdWorkflow<FolderWorkflow> {

        private Date date;
        private Set<String> nodeIDs;

        public ScheduleDepublishWorkflow(IPluginContext context) {
            super("scheduleDepublish", new StringResourceModel("schedule-depublish-all-label", ScheduledWorkflowPlugin.this), context, (WorkflowDescriptorModel) ScheduledWorkflowPlugin.this.getModel());
            date = new Date();
            nodeIDs = new HashSet<>();
        }

        @Override
        protected Component getIcon(String id) {
            return HippoIcon.fromSprite(id, Icon.MINUS_CIRCLE);
        }


        @Override
        protected ScheduleBulkDialog createRequestDialog() {
            try {
                folderName = ((HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode()).getDisplayName();
                collectValidDocumentIDs(getModel().getNode(), DEPUBLISH, nodeIDs);
            } catch (RepositoryException | RemoteException | WorkflowException e) {
                LOG.error("Exception while trying to retrieve node.", e);
                folderName = "";
            }

            IModel<String> titleModel = new StringResourceModel("schedule-depublish-all-title", ScheduledWorkflowPlugin.this);
            IModel<String> notification = new StringResourceModel("schedule-depublish-all-notification", ScheduledWorkflowPlugin.this).setParameters(folderName, nodeIDs.size());
            IModel<String> action = new StringResourceModel("schedule-all-action", ScheduledWorkflowPlugin.this);
            IModel<String> dateText = new StringResourceModel("schedule-depublish-all-text", ScheduledWorkflowPlugin.this);
            return new ScheduleBulkDialog(this, ScheduledWorkflowPlugin.super.getModel(),
                    PropertyModel.of(this, "date"), titleModel, notification, action, dateText);
        }

        @Override
        protected String execute(FolderWorkflow workflow) throws Exception {
            bulkExecuteDocumentWorkflow(DEPUBLISH, nodeIDs, date);
            return null;
        }
    }

    private class CancelAllWorkflow extends StdWorkflow<FolderWorkflow> {

        public CancelAllWorkflow(IPluginContext context) {
            super("cancelAll", new StringResourceModel("cancel-all-label", ScheduledWorkflowPlugin.this), context, (WorkflowDescriptorModel) ScheduledWorkflowPlugin.this.getModel());
        }

        @Override
        protected Component getIcon(String id) {
            return HippoIcon.fromSprite(id, Icon.TIMES);
        }

        @Override
        protected String execute(FolderWorkflow wf) throws Exception {
            Set<String> nodeIDs = new HashSet<>();
            collectValidDocumentIDs(getModel().getNode(), CANCEL_REQUEST, nodeIDs);
            bulkExecuteDocumentWorkflow(CANCEL_REQUEST, nodeIDs, null);
            return null;
        }

    }
}