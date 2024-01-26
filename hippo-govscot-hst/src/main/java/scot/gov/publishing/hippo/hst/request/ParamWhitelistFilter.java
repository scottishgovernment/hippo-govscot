package scot.gov.publishing.hippo.hst.request;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Whitelist url params allowed on a site by site basis.
 */
public class ParamWhitelistFilter extends HttpFilter {

    Set<String> whitelist = new HashSet<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        String whitelistString = filterConfig.getInitParameter("whitelist");
        whitelist = new HashSet<>(Arrays.asList(whitelistString.split(",")));
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (StringUtils.isBlank(request.getQueryString())) {
            super.doFilter(request, response, chain);
            return;
        }

        HttpServletRequest req = new LimitedParamsRequest(request, whitelist);
        super.doFilter(req, response, chain);
    }

    static class LimitedParamsRequest extends HttpServletRequestWrapper {

        String queryString;

        public LimitedParamsRequest(HttpServletRequest request, Set<String> whitelist) {
            super(request);
            this.queryString = filterQueryString(request.getQueryString(), whitelist);
        }

        String filterQueryString(String queryString, Set<String> whitelist) {
            StringBuilder filteredQueryString = new StringBuilder();
            String[] params = queryString.split("&");
            for (String param : params) {
                if (includeParam(param, whitelist)) {
                    filteredQueryString.append(param);
                    filteredQueryString.append('&');
                }
            }

            return filteredQueryString.toString();
        }

        boolean includeParam(String param, Set<String> whitelist) {
            String[] keyValue = param.split("=", 2);
            String paramName = keyValue[0];
            if (!whitelist.contains(paramName)) {
                return false;
            }

            return true;
        }

        @Override
        public String getQueryString() {
            return queryString;
        }

    }
}