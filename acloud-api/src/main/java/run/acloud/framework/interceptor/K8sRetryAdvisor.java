package run.acloud.framework.interceptor;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

@Slf4j
@Aspect
@Component
public class K8sRetryAdvisor {

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    @Around("execution(public * run.acloud.api.resource.task.K8sWorker.*(..)) " +
            "&& !execution(* run.acloud.api.resource.task.K8sWorker.convertReasourceMap(..)) " +
            "&& !execution(* run.acloud.api.resource.task.K8sWorker.convertQuantityToLong(..)) " +
            "&& !execution(* run.acloud.api.resource.task.K8sWorker.convertQuantityToLongForCpuMilliCore(..))")
    public Object loggerAop(ProceedingJoinPoint joinpoint) throws Throwable{

        String signatureStr = joinpoint.getSignature().toShortString();
        log.debug(String.format("Start K8sRetryAdvisor %s!!", signatureStr));

        int retryCounter = 0;
        int maxRetries = cocktailServiceProperties.getK8sRetryMaxCount();
        Exception lastException = null;

        while (retryCounter <= maxRetries) {
            try {
                return joinpoint.proceed();
            } catch (ApiException e) {
                boolean isRetry = false;

                lastException = e;
                log.error(String.format("FAILED K8sRetryAdvisor %s, Exception Msg : [%s: %s] - retry %d of %d !!", signatureStr, e.getClass().getName(), e.getMessage(), retryCounter, maxRetries));
                // ApiException 오류가 Tag mismatch 인 경우 재시도
                if(StringUtils.equalsIgnoreCase("javax.net.ssl.SSLException: Tag mismatch!", e.getMessage())){
                    isRetry = true;
                }
                // ApiException 오류가 Unsupported record version Unknown 인 경우 재시도
                else if(StringUtils.contains(e.getMessage(), "javax.net.ssl.SSLException: Unsupported record version Unknown")){
                    isRetry = true;
                }
                // ApiException 오류가 timeout인 경우 rootCause가 Socket closed면 재시도
                else if(StringUtils.contains(e.getMessage(), "java.net.SocketTimeoutException: timeout")){
                    Throwable rootCause = ExceptionUtils.getRootCause(e);
                    String rootCauseMsg = ExceptionMessageUtils.getExceptionName(rootCause);
                    if (StringUtils.contains(rootCauseMsg, "java.net.SocketException: Socket closed")){
                        isRetry = true;
                    }
                }
                // ApiException 오류가 connect timed out 인 경우 재시도
                else if(StringUtils.contains(e.getMessage(), "java.net.SocketTimeoutException: connect timed out")){
                    isRetry = true;
                }
                else {
                    break;
                }

                if(isRetry){
                    if (retryCounter >= maxRetries) {
                        break;
                    }
                }
            } catch (CocktailException ce){
                lastException = ce;
            } finally {
                log.debug(String.format("Finish K8sRetryAdvisor %s - retry %d of %d !!", signatureStr, retryCounter, maxRetries));
                retryCounter++;
            }
        }

        throw lastException;
    }

}
