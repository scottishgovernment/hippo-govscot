package scot.gov.publishing.publications.cms;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnEventHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;

/**
 * Bloomreach perspective for the publications importer.
 */
@SuppressWarnings("unused")
public class PublicationsPerspective extends Perspective {

    private static final ResourceReference PERSPECTIVE_CSS =
        new CssResourceReference(PublicationsPerspective.class, "PublicationsPerspective.css");

    private static final ResourceReference PERSPECTIVE_JS =
        new JavaScriptResourceReference(PublicationsPerspective.class, "PublicationsPerspective.js");

    private final WebMarkupContainer iframe;

    @SuppressWarnings("unused")
    public PublicationsPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setOutputMarkupId(true);
        context.registerService(this, "publications-perspective");

        iframe = new WebMarkupContainer("publications-perspective-iframe");
        iframe.setOutputMarkupId(true);
        add(iframe);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(PERSPECTIVE_CSS));
        response.render(JavaScriptHeaderItem.forReference(PERSPECTIVE_JS));
        response.render(OnEventHeaderItem.forScript(
                "'" + getMarkupId() + "'",
                "readystatechange",
                "PublicationsPerspective.showIFrame(\"" + iframe.getMarkupId() + "\");"));
    }

}
