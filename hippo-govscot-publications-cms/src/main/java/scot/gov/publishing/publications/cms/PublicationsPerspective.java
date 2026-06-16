package scot.gov.publishing.publications.cms;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnEventHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.jcr.FeatureFlag;

/**
 * Bloomreach perspective for the publications importer.
 */
@SuppressWarnings("unused")
public class PublicationsPerspective extends Perspective {

    private static final Logger LOG = LoggerFactory.getLogger(PublicationsPerspective.class);

    private static final ResourceReference PERSPECTIVE_CSS =
        new CssResourceReference(PublicationsPerspective.class, "PublicationsPerspective.css");

    private static final ResourceReference PERSPECTIVE_JS =
        new JavaScriptResourceReference(PublicationsPerspective.class, "PublicationsPerspective.js");

    // mirrors the flag checked by the publications-rest API - kept in step so that when the API
    // is refusing to respond, the perspective shows a clear disabled message rather than a blank
    // or broken iframe. The userrole grant that makes the perspective's nav item appear at all is
    // static CMS config, so this flag cannot hide the nav item itself - only what it shows.
    private static final String FEATURE_FLAG_NAME = "PublicationImporterPerspective";

    private final WebMarkupContainer iframe;

    private final boolean enabled;

    @SuppressWarnings("unused")
    public PublicationsPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setOutputMarkupId(true);
        context.registerService(this, "publications-perspective");

        enabled = isFeatureEnabled();

        iframe = new WebMarkupContainer("publications-perspective-iframe");
        iframe.setOutputMarkupId(true);
        iframe.setVisible(enabled);
        add(iframe);

        Label disabledMessage = new Label("publications-perspective-disabled",
                new StringResourceModel("disabled-message", this, null));
        disabledMessage.setVisible(!enabled);
        add(disabledMessage);
    }

    private boolean isFeatureEnabled() {
        try {
            HippoSession session = UserSession.get().getJcrSession();
            return new FeatureFlag(session, FEATURE_FLAG_NAME).isEnabled();
        } catch (Exception e) {
            LOG.warn("Failed to check {} feature flag, defaulting to disabled", FEATURE_FLAG_NAME, e);
            return false;
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(PERSPECTIVE_CSS));
        if (!enabled) {
            return;
        }
        response.render(JavaScriptHeaderItem.forReference(PERSPECTIVE_JS));
        response.render(OnEventHeaderItem.forScript(
                "'" + getMarkupId() + "'",
                "readystatechange",
                "PublicationsPerspective.showIFrame(\"" + iframe.getMarkupId() + "\");"));
    }

}
