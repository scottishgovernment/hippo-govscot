package scot.gov.publishing.hippo.redirects;

import org.apache.commons.validator.routines.UrlValidator;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class RedirectValidator extends UrlValidator {

    public RedirectValidator() {
        super(new String[]{"http", "https"}, UrlValidator.ALLOW_2_SLASHES);
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
        return super.isValid(redirect.getFrom()) || isValidPath(redirect.getFrom());
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
