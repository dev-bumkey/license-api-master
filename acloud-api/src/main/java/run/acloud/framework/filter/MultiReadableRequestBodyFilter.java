package run.acloud.framework.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import run.acloud.framework.wrapper.CustomHttpServletRequestWrapper;

import java.io.IOException;

@Component
public class MultiReadableRequestBodyFilter implements Filter {

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        if (servletRequest.getContentType() == null) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        if (servletRequest.getContentType().startsWith("multipart/")) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            CustomHttpServletRequestWrapper wrappedRequest = new CustomHttpServletRequestWrapper((HttpServletRequest) servletRequest);
            filterChain.doFilter(wrappedRequest, servletResponse);
        }
    }
}