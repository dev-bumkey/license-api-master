/**
 * Copyright ⓒ 2018 Acornsoft. All rights reserved
 * @project     : cocktail-java
 * @category    : run.acloud.framework.filter
 * @class       : TransactionLoggingFilter.java
 * @author      : Gun Kim (gun@acornsoft.io)
 * @date        : 2018. 8. 28 오후 07:30:38
 * @description :
 */
package run.acloud.framework.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import run.acloud.commons.util.AntPathMatcherUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.io.IOException;
import java.util.Map;

/**
 * 내부 통신 인증 처리
 */
@Slf4j
public class InternalAuthFilter implements Filter {
    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String sRequestURI = "";
        if(req.getRequestURI() != null) {
            sRequestURI = req.getRequestURI();
        }

        Map<String, String> requestHeaderMap = ExceptionMessageUtils.getHeadersInfo(req);

        // 플랫폼 신청현황
        if (AntPathMatcherUtils.getPathMatcher().match("/internal/account/applications/**", sRequestURI)) {
            log.debug("call applications {}} => {}", sRequestURI, JsonUtils.toPrettyString(requestHeaderMap));
        }
        // 사용자 OTP
        else if (AntPathMatcherUtils.getPathMatcher().match("/internal/otp/**", sRequestURI)) {
            log.debug("call otp {}} => {}", sRequestURI, JsonUtils.toPrettyString(requestHeaderMap));
        }
        // 사용자 OTP
        else if (AntPathMatcherUtils.getPathMatcher().match("/internal/cluster/**", sRequestURI)) {
            log.debug("call cluster {}} => {}", sRequestURI, JsonUtils.toPrettyString(requestHeaderMap));
        }

        chain.doFilter( request, response );

    }

    @Override
    public void init( FilterConfig config )
        throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
