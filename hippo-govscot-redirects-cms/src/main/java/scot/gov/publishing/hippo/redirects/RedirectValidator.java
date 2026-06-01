package scot.gov.publishing.hippo.redirects;

import org.apache.commons.validator.routines.UrlValidator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class RedirectValidator extends UrlValidator {

    private final Set<String> allowedOrigins;

    public RedirectValidator( Set<String> allowedOrigins) {
        super(new String[]{"http", "https"}, UrlValidator.ALLOW_2_SLASHES);
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> validateRedirects(List<Redirect> redirects) {
        List<String> violations = new ArrayList<>();
        for (Redirect redirect : redirects) {
            validateRedirect(redirect, violations);
        }
        return violations;
    }

    void validateRedirect(Redirect redirect, List<String> violations) {
        if (!validFrom(redirect)) {
            violations.add("Invalid From url: " + redirect.getFrom());
        }
        if (!validTo(redirect)) {
            violations.add("Invalid To url: " + redirect.getTo());
        }
    }

    boolean validFrom(Redirect redirect) {
        if (isBlank(redirect.getFrom())) {
            return false;
        }
        if (!super.isValid(redirect.getFrom()) && !isValidPath(redirect.getFrom())) {
            return false;
        }

        String host = extractHost(redirect.getFrom());
        // null host means it's a relative path — accept it
        return host == null || allowedOrigins.contains(host);
    }

    private String extractHost(String url) {
        try {
            return new URI(url).getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    boolean validTo(Redirect redirect) {
        if (isBlank(redirect.getTo())) {
            return false;
        }
        if (isValid(redirect.getTo())) {
            return true;
        }
        String[] pathAndAnchor = redirect.getTo().split("#");
        return pathAndAnchor.length == 1
                ? isValidPath(pathAndAnchor[0])
                : isValidFragment(pathAndAnchor[1]);
    }
}
