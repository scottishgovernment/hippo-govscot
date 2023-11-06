package scot.gov.publishing.staging.dialog;

import org.apache.http.client.utils.URIBuilder;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.*;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.validation.validator.DateValidator;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.channelmanager.HstUtil;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.datetime.extensions.yui.calendar.DatePicker;

import javax.jcr.*;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static scot.gov.publishing.staging.plugins.PreviewPlugin.INTERNAL_PREVIEW_NODE_NAME;

public class PreviewDatePickerDialog extends AbstractDialog<String> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(PreviewDatePickerDialog.class);

    private static final String RESET_CONTAINER = "-reset-container";

    private static final CssResourceReference CSS = new CssResourceReference(PreviewDatePickerDialog.class, "PreviewDatePickerDialog.css");

    private static final ResourceReference DATE_PICKER_ICON = new PackageResourceReference(PreviewDatePickerDialog.class, "calendar.png");

    private static final ResourceReference RESET_ICON = new PackageResourceReference(PreviewDatePickerDialog.class, "delete.png");

    private String title;
    private Date expirationDate;
    private Set<String> nodeIDs;

    public PreviewDatePickerDialog(Set<String> nodeIDs) {
        this.title = getString("dialog-title", null, "Preview generation");
        this.nodeIDs = nodeIDs;

        //3 week default expiration date
        Calendar preselectedDate = Calendar.getInstance();
        preselectedDate.add(Calendar.DATE, 21);
        this.expirationDate =  preselectedDate.getTime();

        Form form = new Form<>("form", new CompoundPropertyModel<>(this));
        DateTextField expiryDate = new DateTextField("expiration-date",
                new PropertyModel<>(this, "expirationDate"),
               "dd/MM/yyyy") {

            @Override
            protected void onModelChanged() {
                super.onModelChanged();
                Optional<AjaxRequestTarget> target = getRequestCycle().find(AjaxRequestTarget.class);
                if (target.isPresent()) {
                    target.get().add(this.getParent().get(this.getId() + RESET_CONTAINER));
                }
            }
        };
        expiryDate.add(DateValidator.minimum(Calendar.getInstance().getTime()));
        expiryDate.add(createDatePicker());
        expiryDate.add(createSimpleAjaxChangeBehavior());
        form.add(expiryDate);
        form.add(createSimpleResetLink(expiryDate, this));
        add(form);

        Label counttext = new Label("counttext", MessageFormat.format(
                new StringResourceModel("publish-all-subtext",
                        PreviewDatePickerDialog.this).getString(), nodeIDs.size()
        ));
        add(counttext);
        counttext.setVisible(nodeIDs.size()>1);

        setOutputMarkupId(true);
        setCancelVisible(true);
    }


    private DatePicker createDatePicker() {
        DatePicker datePicker = new DatePicker() {
            @Override
            protected String getAdditionalJavaScript() {
                return "${calendar}.cfg.setProperty(\"navigator\",true,false); ${calendar}.render();";
            }

            @Override
            protected boolean alignWithIcon() {
                return false;
            }

            @Override
            protected CharSequence getIconUrl() {
                return RequestCycle.get().urlFor(new ResourceReferenceRequestHandler(DATE_PICKER_ICON));
            }
        };
        datePicker.setShowOnFieldClick(true);
        return datePicker;
    }

    private AjaxFormComponentUpdatingBehavior createSimpleAjaxChangeBehavior(Component... components) {
        return new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                setOkEnabled(true);
                if (components != null) {
                    for (Component component : components) {
                        target.add(component);
                        Component reset = component.getParent().get(component.getId() + RESET_CONTAINER);
                        if (reset != null) {
                            target.add(reset);
                        }
                    }
                }
            }
        };
    }



    private MarkupContainer createSimpleResetLink(Component component, PreviewDatePickerDialog dialog) {
        WebMarkupContainer container = new WebMarkupContainer(component.getId() + RESET_CONTAINER);
        container.setOutputMarkupId(true);
        Image resetImage = new Image(component.getId() + "-reset", RESET_ICON) {

            @Override
            public boolean isVisible() {
                return component.getDefaultModelObject() != null;
            }
        };
        resetImage.add(new AjaxEventBehavior("click") {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(new EventStoppingDecorator());
            }

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                dialog.setOkEnabled(false);
                component.setDefaultModelObject(null);
                target.add(component);
            }
        });
        container.add(resetImage);
        return container;
    }

    @Override
    protected void onOk() {
        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();
        Session session = UserSession.get().getJcrSession();
        nodeIDs.forEach(id->{
            try {
                Node node = session.getNodeByIdentifier(id);
                List<String> urls = getPreviewURLs(node);
                generatePreviewLinkNodes(node, urls, randomUUIDString);
            } catch (RepositoryException e){
                LOG.error("An exception occurred while generating a preview for node with uuid {}", id, e);
            }
        });
    }

    private List<String> getPreviewURLs(Node node) {
        HstRequestContext hstRequestContext = RequestContextProvider.get();
        HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        HstModel siteModel = hstModelRegistry.getHstModel("/site");
        HstLinkCreator hstLinkCreator = siteModel.getHstLinkCreator();
        return siteModel.getVirtualHosts().getMountsByHostGroup(HstUtil.getHostGroup())
                .stream()
                .filter(mount -> {
                    try {
                        return "preview".equals(mount.getType()) && mount.getAlias().endsWith("-staging") && node.getPath().contains(mount.getContentPath());
                    } catch (RepositoryException e) {
                        LOG.error("An exception occurred during preview link creation.", e);
                    }
                    return false;
                })
                .map(mount -> hstLinkCreator.create(node, mount).toUrlForm(hstRequestContext, true))
                .collect(Collectors.toList());
    }

    protected void generatePreviewLinkNodes(Node node, List<String> urls, String randomUUIDString) throws RepositoryException {
        LOG.info("generatePreviewLinkNodes {}, {}, {}", node.getPath(), urls, randomUUIDString);
        Node unpublishedVariant = getUnpublishedVariant(node);
        urls.stream().forEach(url -> {
            if (unpublishedVariant != null) {
                //storing the preview id in a separate child node
                try {
                    String fullURL = new URIBuilder(url).addParameter("previewkey", randomUUIDString).toString();
                    Node previewId = unpublishedVariant.addNode(INTERNAL_PREVIEW_NODE_NAME, "staging:preview");
                    previewId.setProperty("staging:key", randomUUIDString);
                    previewId.setProperty("staging:url", fullURL);
                    previewId.setProperty("staging:user", UserSession.get().getJcrSession().getUser().getId());
                    previewId.setProperty("staging:creationdate", Calendar.getInstance());
                    Calendar expiration = Calendar.getInstance();
                    expiration.setTime(expirationDate);
                    previewId.setProperty("staging:expirationdate", expiration);
                    previewId.getSession().save();
                } catch (RepositoryException e) {
                    LOG.error("Exception while generating preview link nodes.", e);
                } catch (URISyntaxException e){
                    LOG.error("Exception while constructing full URL.", e);
                }
            } else {
                LOG.info("unpublishedVariant is null");
            }
        });
    }

    private Node getUnpublishedVariant(Node handle) throws RepositoryException {
        Node unpublishedVariant = getVariantWithState(handle, HippoStdNodeType.UNPUBLISHED);
        if (unpublishedVariant != null) {
            return unpublishedVariant;
        }

        // there is no unpublished variant. This is due to code automatically creating content that has never ben edited
        // clone the published one.
        return createUnpublishedVariant(handle);
    }

    Node createUnpublishedVariant(Node handle) throws RepositoryException {
        Node publishedVariant = getVariantWithState(handle, HippoStdNodeType.PUBLISHED);
        if (publishedVariant == null) {
            return null;
        }

        Workspace workspace = handle.getSession().getWorkspace();
        workspace.copy(publishedVariant.getPath(), publishedVariant.getPath());
        Node newVariant = lastVariant(handle);
        newVariant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.UNPUBLISHED);
        newVariant.setProperty("hippo:availability", new String [] {"preview"});
        return newVariant;
    }

    Node lastVariant(Node handle) throws RepositoryException {
        NodeIterator it = handle.getNodes(handle.getName());
        Node last = null;
        while (it.hasNext()) {
            last = it.nextNode();
        }
        return last;
    }

    private Node getVariantWithState(Node handle, String state) throws RepositoryException {
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            String variantState = JcrUtils.getStringProperty(variant, HippoStdNodeType.HIPPOSTD_STATE, null);
            if (state.equals(variantState)) {
                return variant;
            }
        }
        return null;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
    }

    @Override
    public IModel getTitle() {
        return new IModel<String>() {

            @Override
            public String getObject() {
                return title;
            }
        };
    }
}