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
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StopWatch;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.framework.context.ContextHolder;

import java.io.IOException;

@Slf4j
public class TransactionLoggingFilter implements Filter {
    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException {
        StopWatch sw = new StopWatch();
        sw.start();
        Long tXID = (Long) request.getAttribute( CommonConstants.TXID );
        if ( tXID == null ) {
            tXID = ContextHolder.tXID( true );
        }
        String sRequestURI = "";
        if(((HttpServletRequest)request).getRequestURI() != null) {
            sRequestURI = ((HttpServletRequest) request).getRequestURI().toLowerCase();
        }

        /**
         * Logback 설정에서 사용하기 위한 MDC put
         */
        MDC.put( CommonConstants.TXID, tXID.toString() );
        MDC.put( CommonConstants.PRINCIPAL, "1233" );
        MDC.put( CommonConstants.REQUEST_URI_CONST, sRequestURI);

        request.setAttribute( CommonConstants.TXID, tXID );

        try {
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse)response).setHeader("X-TXID", tXID.toString());
            }
            if(!sRequestURI.startsWith(CommonConstants.API_CHECK_URI)) { // Liveness & Readiness Call일 경우에는 로그를 남기지 않음
                log.debug(String.format("### Request started : URI[ %s ]", ((HttpServletRequest) request).getRequestURI()));
            }
            chain.doFilter( request, response );
        } finally {
            sw.stop();
            if(!sRequestURI.startsWith(CommonConstants.API_CHECK_URI)) { // Liveness & Readiness Call일 경우에는 로그를 남기지 않음
                log.debug(String.format("### Request ended : URI[ %s ] : ExecutionTime[ %ss ]", ((HttpServletRequest) request).getRequestURI(), sw.getTotalTimeSeconds()));
            }
            ContextHolder.removeTXID();
            MDC.remove( CommonConstants.TXID );
            MDC.remove( CommonConstants.PRINCIPAL );

            ContextHolder.removeExeContext();
            ContextHolder.removeAuditProcessingDatas();

            // ThreadLocal<T> 제대로 초기화가 되지 않는 경우에 대비하여 강제 초기화
            ContextHolder.get().clear();
        }
    }

    @Override
    public void init( FilterConfig config )
        throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
