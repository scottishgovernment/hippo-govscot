package scot.gov.publishing.hippo.sso;

/**
 * Attributes set on a UserCredentials object by the callback handler.
 * These attributes are used to pass user attributes from the IdP to the CMS.
 */
public final class SsoAttributes {

    public static final String SSO_ID = SsoAttributes.class.getName() + ".id";

    public static final String SSO_EMAIL = SsoAttributes.class.getName() + ".email";

    static final String SSO_NAME = SsoAttributes.class.getName() + ".name";

    public static final String SSO_GIVEN_NAME = SsoAttributes.class.getName() + ".givenName";

    public static final String SSO_FAMILY_NAME = SsoAttributes.class.getName() + ".familyName";

    private SsoAttributes() {
        // Private constructor
    }
}
