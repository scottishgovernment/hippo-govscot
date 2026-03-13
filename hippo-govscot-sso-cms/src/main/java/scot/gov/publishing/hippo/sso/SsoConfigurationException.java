package scot.gov.publishing.hippo.sso;

/**
 * Exception thrown when SSO integration cannot be configured or initialised.
 */
public class SsoConfigurationException extends RuntimeException {

    public SsoConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
