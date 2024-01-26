package scot.gov.publishing.hippo.hst.request;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Apply alowlist to url params
 */
public class ParamAllowlistFilter extends HttpFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ParamAllowlistFilter.class);

    private Set<String> allowlist = new HashSet<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        String whitelistString = filterConfig.getInitParameter("allowlist");
        allowlist = new HashSet<>(Arrays.asList(whitelistString.split(",")));
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (StringUtils.isBlank(request.getQueryString())) {
            super.doFilter(request, response, chain);
            return;
        }

        HttpServletRequest req = new LimitedParamsRequest(request, allowlist);
        super.doFilter(req, response, chain);
    }

    static class LimitedParamsRequest extends HttpServletRequestWrapper {

        String queryString;

        public LimitedParamsRequest(HttpServletRequest request, Set<String> allowlist) {
            super(request);
            this.queryString = filterQueryString(request.getQueryString(), allowlist);
        }

        String filterQueryString(String queryString, Set<String> allowlist) {
            StringBuilder filteredQueryString = new StringBuilder();
            String[] params = queryString.split("&");
            for (String param : params) {
                if (include(param, allowlist)) {
                    LOG.error("including param {}", param);
                    filteredQueryString.append(param);
                    filteredQueryString.append('&');
                } else {
                    LOG.error("stripping param {}", param);
                }
            }

            return filteredQueryString.toString();
        }

        boolean include(String param, Set<String> allowlist) {
            String[] keyValue = param.split("=", 2);
            return allowlist.contains(keyValue[0]);
        }

        @Override
        public String getQueryString() {
            return queryString;
        }

    }
}