package scot.gov.publishing.hippo.sso;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Arrays;
import java.util.Optional;

public final class SsoCookies {

    public static final String SSO_COOKIE_NAME = "sso";

    /**
     * Cookie name used to indicate that the user has logged out.
     * This allows the redirect filter to avoid immediately redirecting the user
     * back to the IdP when not required (sso.redirect=ONCE).
     */
    public static final String LOGGED_OUT_COOKIE_NAME = "logged_out";

    private SsoCookies() {
        // Utility class - do not instantiate
    }

    public static boolean isSsoLoginRequested(HttpServletRequest request) {
        return getBooleanCookie(request, SSO_COOKIE_NAME).orElse(false);
    }

    public static boolean isSsoLoginSuppressed(HttpServletRequest request) {
        return !getBooleanCookie(request, SSO_COOKIE_NAME).orElse(true);
    }

    public static boolean isLogoutRequested(HttpServletRequest request) {
        return getBooleanCookie(request, LOGGED_OUT_COOKIE_NAME).orElse(false);
    }

    static Optional<Boolean> getBooleanCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (ArrayUtils.isEmpty(cookies)) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equalsIgnoreCase(c.getName()))
                .map(Cookie::getValue)
                .map(BooleanUtils::toBoolean)
                .findFirst();
    }

}
