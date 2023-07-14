package run.acloud.framework.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import run.acloud.commons.util.AntPathMatcherUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.framework.enums.AuthExcludePathPattern;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

@Slf4j
public class LoggerFilter implements Filter {

//    @Resource(name = "cocktailSession")
//    private SqlSessionTemplate sqlSession;

//    private static Logger log = LoggerFactory.getLogger(LoggerFilter.class);
    private static Logger inquireAuditLogger = LoggerFactory.getLogger("inquire.audit.logger");

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException{
		HttpServletRequest request = (HttpServletRequest) req;

        log.debug(request.getRequestURI());

        if (request.getRequestURI().endsWith("/xhr_streaming") ||
                request.getRequestURI().endsWith("/xhr") ||
                request.getRequestURI().endsWith("/jsonp") ||
                request.getRequestURI().endsWith("/eventsource") ||
                request.getRequestURI().endsWith("/xhr_send") ||
                request.getRequestURI().endsWith("/terraform"))
        {
            return;
        }

        if( ( (inquireAuditLogger.isInfoEnabled() || inquireAuditLogger.isDebugEnabled()) || (log.isDebugEnabled() || log.isTraceEnabled()) )
                && !(request.getRequestURI().endsWith("/check/liveness")
                        || request.getRequestURI().endsWith("/check/readiness")
                        || request.getRequestURI().endsWith("/signature/validate")
                        || AntPathMatcherUtils.getPathMatcher().match("/api/cluster/*/RUNNING", request.getRequestURI())
                        || AntPathMatcherUtils.getPathMatcher().match("/api/cluster/*/STOPPED", request.getRequestURI())
                        || AntPathMatcherUtils.getPathMatcher().match("/api/cluster/id/*/RUNNING", request.getRequestURI())
                        || AntPathMatcherUtils.getPathMatcher().match("/api/cluster/id/*/STOPPED", request.getRequestURI())
                    )
        ){

            Map<String, String> requestHeaderMap = ExceptionMessageUtils.getHeadersInfo(request);

            StringBuffer sb = new StringBuffer();
            sb.append("\n####################################################################################################\n");
            sb.append(String.format("# - Request URi : %s", request.getRequestURI())).append("\n");
            sb.append(String.format("# - RemoteHost : %s", request.getRemoteHost())).append("\n");
            sb.append(String.format("# - ClientIp : %s", Utils.getClientIp(request))).append("\n");
            sb.append(String.format("# - Request Headers : %s", (MapUtils.isNotEmpty(requestHeaderMap) ? JsonUtils.toGson(requestHeaderMap) : ""))).append("\n");
            sb.append(String.format("# - Rquest Query String : %s", request.getQueryString())).append("\n");
            sb.append(String.format("# - Method : %s", request.getMethod())).append("\n");
            //2018.11.22 파일 업로드시 본문이 보이는 이슈로 인해 제외
            if (!StringUtils.startsWith(request.getContentType(), "multipart/") && !request.getRequestURI().endsWith("/file/upload")) {
                sb.append(String.format("# - RequestBody : %s", LoggerFilter.getBody(request))).append("\n");
            }
            sb.append("####################################################################################################\n\n");
            log.debug(sb.toString());

            // 조회 관련 audit 로그 수집
            // INFO, DEBUG 레벨일 시에만 저장
            if (HttpMethod.GET.name().equalsIgnoreCase(request.getMethod())) {
                if (inquireAuditLogger.isInfoEnabled() || inquireAuditLogger.isDebugEnabled()) {

                    // authInterceptor에서 제외하는 path는 제외
                    boolean isExcludePath = false;
                    List<String> excludePaths = AuthExcludePathPattern.toList();
                    if (CollectionUtils.isNotEmpty(excludePaths)) {
                        for (String pathPattern : excludePaths) {
                            if (AntPathMatcherUtils.getPathMatcher().match(pathPattern, request.getRequestURI())) {
                                isExcludePath = true;
                                break;
                            }
                        }
                    }

                    if (!isExcludePath) {
                        if (inquireAuditLogger.isInfoEnabled()) {
                            inquireAuditLogger.info(sb.toString());
                        } else if (inquireAuditLogger.isDebugEnabled()) {
                            inquireAuditLogger.debug(sb.toString());
                        }
                    }
                }
            }
        }

		chain.doFilter(req, res);
	}
 
	@Override
	public void destroy()
	{
	}
 
	@Override
	public void init(FilterConfig fc) throws ServletException
	{
	}
	
	public static String getBody(HttpServletRequest request) throws IOException {
		 
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            log.error("Error reading the request body...");
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    log.error("Error closing bufferedReader...");
                }
            }
        }
 
        return stringBuilder.toString();
    }
}
