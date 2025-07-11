package scot.gov.publishing.bulkpublish;

import org.apache.wicket.model.IModel;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.DatePickerComponent;
import org.hippoecm.repository.api.WorkflowDescriptor;

import java.util.Date;

public class ScheduleBulkDialog extends WorkflowDialog<WorkflowDescriptor> {

    public ScheduleBulkDialog(IWorkflowInvoker invoker, IModel<WorkflowDescriptor> nodeModel,
                              IModel<Date> dateModel, IModel<String> titleModel,
                              IModel<String> notification, IModel<String> action,
                              IModel<String> dateText) {
        super(invoker, nodeModel, titleModel);

        setCssClass("hippo-workflow-dialog");
        setNotification(notification);
        setOkLabel(action);
        setFocusOnOk();

        addOrReplace(new DatePickerComponent(Dialog.BOTTOM_LEFT_ID, dateModel, dateText));
    }
}
