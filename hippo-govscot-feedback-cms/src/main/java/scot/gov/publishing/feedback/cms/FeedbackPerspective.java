package scot.gov.publishing.feedback.cms;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnEventHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Bloomreach perspective for showing on-site feedback.
 */
@SuppressWarnings("unused")
public class FeedbackPerspective extends Perspective {

    private static final Logger LOG = LoggerFactory.getLogger(FeedbackPerspective.class);

    private static final ResourceReference PERSPECTIVE_CSS =
        new CssResourceReference(FeedbackPerspective.class, "FeedbackPerspective.css");

    private static final ResourceReference PERSPECTIVE_JS =
        new JavaScriptResourceReference(FeedbackPerspective.class, "FeedbackPerspective.js");

    private final WebMarkupContainer iframe;

    @SuppressWarnings("unused")
    public FeedbackPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setOutputMarkupId(true);
        context.registerService(this, "feedback-perspective");

        iframe = new WebMarkupContainer("feedback-perspective-iframe");
        iframe.setOutputMarkupId(true);

        add(iframe);

        AjaxLink<?> feedbackLink = new AjaxLink<>("feedbackLink") {
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getDynamicExtraParameters()
                        .add("return {'uuid' : jQuery('#' + attrs.c).attr('data-uuid'), 'path' : jQuery('#' + attrs.c).attr('data-path') };");
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                onFeedbackLinkClick();
            }
        };
        add(feedbackLink);
    }

    public void onFeedbackLinkClick() {
        IRequestParameters parameters = RequestCycle.get()
                .getRequest()
                .getRequestParameters();
        String uuid = parameters.getParameterValue("uuid").toString();
        String path = parameters.getParameterValue("path").toString();

        String nodePath;
        if (!uuid.isEmpty()) {
            nodePath = getNodePathFromId(uuid);
        } else {
            String location = getPluginConfig().getString("option.location");
            nodePath = location + path;
        }
        browseToPath(nodePath);
    }

    void browseToPath(String nodePath) {
        IPluginContext context = getPluginContext();
        IPluginConfig config = getPluginConfig();
        String browserId = config.getString( "browser.id", "service.browse");
        @SuppressWarnings("unchecked")
        IBrowseService<JcrNodeModel> browseService = context.getService(browserId, IBrowseService.class);
        if (browseService == null) {
            LOG.warn("no browse service found with id '{}', cannot browse to '{}'", browserId, nodePath);
            return;
        }
        browseService.browse(new JcrNodeModel(nodePath));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(PERSPECTIVE_CSS));
        response.render(JavaScriptHeaderItem.forReference(PERSPECTIVE_JS));
        response.render(OnEventHeaderItem.forScript(
                "'" + getMarkupId() + "'",
                "readystatechange",
                "FeedbackPerspective.showIFrame(\"" + iframe.getMarkupId() + "\");"));
    }

    private String getNodePathFromId(String uuid) {
        try {
            HippoSession session = UserSession.get().getJcrSession();
            Node thisNode = session.getNodeByIdentifier(uuid);
            return thisNode.getPath();
        } catch (RepositoryException e) {
            LOG.error("Failed to get node for ID {}", uuid, e);
            return null;
        }
    }

}
