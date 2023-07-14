package run.acloud.framework.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.HandlerMapping;
import run.acloud.api.auth.enums.ServiceMode;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.catalog.vo.TemplateLaunchVO;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.cserver.vo.ServerYamlVO;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.AuditAdditionalType;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.AuditAccessLogger;
import run.acloud.framework.util.AuditLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
public class AuditLoggingAdvisor {
    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private AuditAccessLogger auditAccessLogger;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    // 실행중 임시로 사용할 데이터 저장을 위한 hashmap, 동시성 문제가 없도록 concurrentHashMap 사용.
    private ConcurrentHashMap<String, Object> execTempMap = new ConcurrentHashMap<String, Object>();

    private static Logger aLog = LoggerFactory.getLogger("audit.logger");

    /*****************************************************************
     * AOP : Acloud API의.모든 컨트롤러 && Acloud Build의 모든 컨트롤러 호출시
     *****************************************************************/
    @Pointcut("execution(public * run.acloud.api.*.controller.*Controller.*(..))")
    public void onApiAuditController() {}

    /**
     * 모든 API와 Build 컨트롤러에 진입전 공통적으로 호출..
     * @param joinPoint
     * @throws Throwable
     */
    @Before("onApiAuditController()")
    public void beforeAuditController(JoinPoint joinPoint) {
        aLog.debug("@@@ Start beforeAuditController");
        /**
         * Audit Logging에서 사용하기 위해 모든 Controller 실행시에 ClassName과 MethodName을 ThreadLocal 변수에 보관
         */

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_CLASS_NAME, signature.getMethod().getDeclaringClass().getSimpleName());
        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_METHOD_NAME, signature.getMethod().getName());
        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_USER_SEQ, ContextHolder.exeContext().getUserSeq());
        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_USER_SERVICE_SEQ, ContextHolder.exeContext().getUserServiceSeq());
        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_USER_ROLE, ContextHolder.exeContext().getUserRole());

        // audit_access_logs 테이블 존재여부 값 셋팅, 2021-11-08, coolingi
//        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_EXISTS_AUDIT_ACCESS_LOG_TABLE, Boolean.valueOf(auditAccessLogger.existTable()));

        StopWatch sw = new StopWatch();
        sw.start(); // Controller 진입전 Start
        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_STOPWATCH_OBJECT, sw); // For Audit Logging : (Controller ExecutionTime)


        aLog.debug("@@@ Ended beforeAuditController");
    }

    /**
     * API와 Build 컨트롤러의 처리가 완료되면 공통적으로 호출..
     * @param joinPoint
     * @throws Throwable
     */
    @After("onApiAuditController()")
    public void afterAuditController(JoinPoint joinPoint) {

        aLog.debug("@@@ Start afterAuditController");
        StopWatch sw = (StopWatch) ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_STOPWATCH_OBJECT);
        sw.stop(); // Controller out 시 stop.
        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_DURATION, sw.getTotalTimeSeconds());
        aLog.debug("@@@ Ended afterAuditController");

    }



    /*****************************************************************
     * AOP : TemplateService.launchTemplateDeployment
     *****************************************************************/
//    @Pointcut("execution(public * run.acloud.api.catalog.service.TemplateService.launchTemplateDeployment(..))")
//    public void onLaunchTemplateDeployment() {}

    @Pointcut("execution(public * run.acloud.api.catalog.service.TemplateService.deployTemplateV2(..))")
    public void onDeployTemplate() {}

    @Before("onDeployTemplate()")
    public void beforeLaunchTemplateDeployment(JoinPoint joinPoint) {

        aLog.debug("@@@ Start beforeLaunchTemplateDeployment");
        // Specific 처리가 필요한 Audit Type이 존재함을 기록 : Launch Template
        ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_ADDITIONAL_TYPE, AuditAdditionalType.LAUNCH_TEMPLATE.getCode());
        aLog.debug("@@@ Ended beforeLaunchTemplateDeployment");

    }

    @After("onDeployTemplate()")
    public void afterLaunchTemplateDeployment(JoinPoint joinPoint) {

        aLog.debug("@@@ Start afterLaunchTemplateDeployment");
        for (Object obj : joinPoint.getArgs()) {
            if(obj instanceof TemplateLaunchVO) {
                aLog.debug("############### TemplateLaunchVO");
                TemplateLaunchVO template = (TemplateLaunchVO)obj;
                aLog.debug(template.getLaunchType());
                aLog.debug(template.getServicemapName());
                aLog.debug(template.getNamespaceName());
                break;
            }
        }
        aLog.debug("@@@ Ended afterLaunchTemplateDeployment");

    }


    /*****************************************************************
     * AOP : ServerProcessService.addServerProcess
     *****************************************************************/
    @Pointcut("execution(public * run.acloud.api.cserver.service.ServerProcessService.addServerProcess(..))")
    public void onAddServerProcess() {}

    @Before("onAddServerProcess()")
    public void beforeAddServerProcess(JoinPoint joinPoint) throws Throwable {
        this.beforeServerProcessing(joinPoint);
    }

    @AfterReturning(value = "onAddServerProcess()", returning = "serverGuiVO")
    public void afterReturningAddServerProcess(JoinPoint joinPoint, ServerGuiVO serverGuiVO) {
        this.afterAddServerProcess(joinPoint, serverGuiVO, null, true);
    }

    @AfterThrowing(value = "onAddServerProcess()", throwing = "exception")
    public void afterThrowingAddServerProcess(JoinPoint joinPoint, Exception exception) {
        this.afterAddServerProcess(joinPoint, null, exception, false);
    }

    private void afterAddServerProcess(JoinPoint joinPoint, ServerGuiVO serverGuiVO, Exception exception, boolean isSuccess) {

        aLog.debug("@@@ Start afterAddServerProcess");
        ExecutingContextVO context = null;
        for (Object obj : joinPoint.getArgs()) {
            if(obj instanceof ExecutingContextVO) {
                aLog.debug("############### ExecutingContextVO");
                context = (ExecutingContextVO)obj;
            }
            if(serverGuiVO == null && (obj instanceof ServerGuiVO)) {
                aLog.debug("############### ServerGuiVO");
                serverGuiVO = (ServerGuiVO)obj;
            }
        }

        // 정보 누락시 종료
        if(context == null || serverGuiVO == null) {
            return;
        }
        // 카탈로그 배포 확인
        boolean isCatalogue = false;
        if(context.getCatalogYn().equalsIgnoreCase("Y")) {
            isCatalogue = true;
        }

        StopWatch sw = (StopWatch) context.getParams().get(CommonConstants.AUDIT_STOPWATCH_OBJECT);
        sw.stop(); // Controller out 시 stop.
        context.getParams().put(CommonConstants.AUDIT_DURATION, sw.getTotalTimeSeconds());

        /** 데이터 정상이면 Audit Log 적재 **/
        if(isSuccess) {
            context.getParams().put(CommonConstants.AUDIT_RESULT_CODE, "SUCCESS");
        }
        else {
            context.getParams().put(CommonConstants.AUDIT_RESULT_CODE, "FAILURE");
        }

        if(serverGuiVO != null) {
            context.getParams().put(CommonConstants.AUDIT_REQUEST_DATAS, serverGuiVO);
        }

        /** Audit Logging..... **/
        boolean existAuditAccessLogsTable = auditAccessLogger.existTable();

        ServiceMode serviceMode = ServiceMode.valueOf(cocktailServiceProperties.getMode());
        // secret online 모드 이면서 audit_access_logs 테이블 존재하면 audit_access_logs 테이블에 로그 적재, 아니면 기존 테이블에 로그 적재
        if (serviceMode == ServiceMode.SECURITY_ONLINE && existAuditAccessLogsTable){
            auditAccessLogger.writeForWorkloadProcess(context.getParams(), isCatalogue);
        }else{
            auditLogger.writeForWorkloadProcess(context.getParams(), isCatalogue);
        }



        aLog.debug("@@@ Ended afterAddServerProcess");

    }

    /*************************************************************************************************
     * AOP : ServerProcessService.udpateServerProcess / redeployServerProcess / terminateServerProcess
     *************************************************************************************************/
    @Pointcut("execution(public * run.acloud.api.cserver.service.ServerProcessService.udpateServerProcess(..))")
    public void onUpdpateServerProcess() {}
    @Pointcut("execution(public * run.acloud.api.cserver.service.ServerProcessService.redeployServerProcess(..))")
    public void onRedeployServerProcess() {}
    @Pointcut("execution(public * run.acloud.api.cserver.service.ServerProcessService.terminateServerProcess(..))")
    public void onTerminateServerProcess() {}

    @Before("onUpdpateServerProcess() || onRedeployServerProcess() || onTerminateServerProcess()")
    public void beforeServerProcess(JoinPoint joinPoint) throws Throwable {
        this.beforeServerProcessing(joinPoint);
    }

    @AfterReturning(value = "onUpdpateServerProcess() || onRedeployServerProcess() || onTerminateServerProcess()", returning = "serverYaml")
    public void afterReturningServerProcess(JoinPoint joinPoint, ServerYamlVO serverYaml)  {
        this.afterServerProcess(joinPoint, serverYaml, null, true);
    }

    @AfterThrowing(value = "onUpdpateServerProcess() || onRedeployServerProcess() || onTerminateServerProcess()", throwing = "exception")
    public void afterThrowingServerProcess(JoinPoint joinPoint, Exception exception) {
        this.afterServerProcess(joinPoint, null, exception, false);
    }

    private void afterServerProcess(JoinPoint joinPoint, ServerYamlVO serverYaml, Exception exception, boolean isSuccess) {

        aLog.debug("@@@ Start afterServerProcess");
        ExecutingContextVO context = null;
        for (Object obj : joinPoint.getArgs()) {
            if(obj instanceof ExecutingContextVO) {
                aLog.debug("############### ExecutingContextVO");
                context = (ExecutingContextVO)obj;
            }
            if(serverYaml == null && (obj instanceof ServerYamlVO)) {
                aLog.debug("############### ServerYamlVO");
                serverYaml = (ServerYamlVO)obj;
            }
        }

        // 정보 누락시 종료
        if(context == null || serverYaml == null) {
            return;
        }
        // 카탈로그 배포 확인
        boolean isCatalogue = false;
        if(context.getCatalogYn().equalsIgnoreCase("Y")) {
            isCatalogue = true;
        }

        StopWatch sw = (StopWatch) context.getParams().get(CommonConstants.AUDIT_STOPWATCH_OBJECT);
        sw.stop(); // Controller out 시 stop.
        context.getParams().put(CommonConstants.AUDIT_DURATION, sw.getTotalTimeSeconds());

        /** 데이터 정상이면 Audit Log 적재 **/
        if(isSuccess) {
            context.getParams().put(CommonConstants.AUDIT_RESULT_CODE, "SUCCESS");
        }
        else {
            context.getParams().put(CommonConstants.AUDIT_RESULT_CODE, "FAILURE");
        }

        if(serverYaml != null) {
            context.getParams().put(CommonConstants.AUDIT_REQUEST_DATAS, serverYaml);
        }

        /** Audit Logging..... **/
        boolean existAuditAccessLogsTable = auditAccessLogger.existTable();

        ServiceMode serviceMode = ServiceMode.valueOf(cocktailServiceProperties.getMode());
        // secret online 모드 이면서 audit_access_logs 테이블 존재하면 audit_access_logs 테이블에 로그 적재, 아니면 기존 테이블에 로그 적재
        if (serviceMode == ServiceMode.SECURITY_ONLINE && existAuditAccessLogsTable){
            auditAccessLogger.writeForWorkloadProcess(context.getParams(), isCatalogue);
        }else{
            auditLogger.writeForWorkloadProcess(context.getParams(), isCatalogue);
        }


        aLog.debug("@@@ Ended afterServerProcess");

    }

    /*************************************************************************************************
     * AOP : ServerService.addWorkload / updateWorkload / redeployWorkload / terminateWorkload
     * Snapshot 으로 배포했을 경우는 이곳을 타지 않는다...
     * => Snapshot 배포 완료시 deployTemplate 하단에서 Workload 배포전 (ASync 처리전) 미리 RequestInfo를 셋팅해줌...
     *************************************************************************************************/
    @Pointcut("execution(public * run.acloud.api.cserver.service.ServerService.addWorkload(..))")
    public void onAddWorkload() {}
    @Pointcut("execution(public * run.acloud.api.cserver.service.ServerService.updateWorkload(..))")
    public void onUpdateWorkload() {}
    @Pointcut("execution(public * run.acloud.api.cserver.service.ServerService.redeployWorkload(..))")
    public void onRedeployWorkload() {}
    @Pointcut("execution(public * run.acloud.api.cserver.service.ServerService.terminateWorkload(..))")
    public void onTerminateWorkload() {}

    @Before("onAddWorkload() || onUpdateWorkload() || onRedeployWorkload() || onTerminateWorkload()")
    public void beforeWorkloadProcess(JoinPoint joinPoint) throws Throwable {
        this.beforeServerProcessing(joinPoint, true);
    }


    /*************************************************************************************************
     * AOP : PackageCommonService.installPackage
     * Shapshot(Catalogue) 배포일 경우만 동작하도록 구현한다... (일반 배포시는 이미 적재되고 있음...)
     *************************************************************************************************/
    @Pointcut("execution(* run.acloud.api.catalog.service.PackageCommonService.installPackage(..))")
    public void onInstallPackage() {}

    @Before("onInstallPackage()")
    public void beforeInstallPackage(JoinPoint joinPoint) {
        this.beforeServerProcessing(joinPoint);
    }

    @AfterReturning(value = "onInstallPackage()", returning = "helmReleaseBase")
    public void afterReturningInstallPackage(JoinPoint joinPoint, HelmReleaseBaseVO helmReleaseBase) {
        this.afterInstallPackage(joinPoint, helmReleaseBase, null, true);
    }

    @AfterThrowing(value = "onInstallPackage()", throwing = "exception")
    public void afterThrowingInstallPackage(JoinPoint joinPoint, Exception exception) {
        this.afterInstallPackage(joinPoint, null, exception, false);
    }

    private void afterInstallPackage(JoinPoint joinPoint, HelmReleaseBaseVO helmReleaseBase, Exception exception, boolean isSuccess) {
        aLog.debug("@@@ Start afterInstallPackage");

        HelmInstallRequestVO helmInstallRequest = null;
        ExecutingContextVO context = null;
        for (Object obj : joinPoint.getArgs()) {
            if(obj instanceof ExecutingContextVO) {
                aLog.debug("############### ExecutingContextVO");
                context = (ExecutingContextVO)obj;
            }
            if(obj instanceof HelmInstallRequestVO) {
                aLog.debug("############### HelmInstallRequestVO");
                helmInstallRequest = (HelmInstallRequestVO)obj;
            }
        }

        // 정보 누락시 종료
        if(context == null || helmInstallRequest == null) {
            return;
        }

        // 카탈로그 배포 확인
        boolean isCatalogue = false;
        if(context.getCatalogYn().equalsIgnoreCase("Y")) {
            isCatalogue = true;
        }

        StopWatch sw = (StopWatch) context.getParams().get(CommonConstants.AUDIT_STOPWATCH_OBJECT);
        if(sw.isRunning()) {
            sw.stop(); // InstallPackage Out시 Stop..
        }
        else {
            if(aLog.isDebugEnabled()) {
                for (int i = 0; i < 10; i++) {
                    aLog.debug("@@@ Stopwatch is not running");
                }
            }
        }
        context.getParams().put(CommonConstants.AUDIT_DURATION, sw.getTotalTimeSeconds());

        /** 데이터 정상이면 Audit Log 적재 **/
        if(isSuccess) {
            context.getParams().put(CommonConstants.AUDIT_RESULT_CODE, "SUCCESS");
        }
        else {
            context.getParams().put(CommonConstants.AUDIT_RESULT_CODE, "FAILURE");
        }

        if(helmInstallRequest != null) {
            context.getParams().put(CommonConstants.AUDIT_REQUEST_DATAS, helmInstallRequest);
        }

        if(helmReleaseBase != null) {
            context.getParams().put(CommonConstants.AUDIT_RESULT_DATAS, helmReleaseBase);
        }

        /** Audit Logging..... **/
        if(isCatalogue) { // 카탈로그 배포일때만 AuditLog 적재를 실행..
            boolean existAuditAccessLogsTable = auditAccessLogger.existTable();

            ServiceMode serviceMode = ServiceMode.valueOf(cocktailServiceProperties.getMode());
            // secret online 모드 이면서 audit_access_logs 테이블 존재하면 audit_access_logs 테이블에 로그 적재, 아니면 기존 테이블에 로그 적재
            if (serviceMode == ServiceMode.SECURITY_ONLINE && existAuditAccessLogsTable){
                auditAccessLogger.writeForInstallPackage(context.getParams(), isCatalogue);
            }else{
                auditLogger.writeForInstallPackage(context.getParams(), isCatalogue);
            }

        }
        else {
            if(aLog.isDebugEnabled()) {
                for(int i = 0;i < 10;i++) {
                    aLog.debug("@@@ SKIP AuditLogging.... Request is not Snapshot Deploy..!!");
                }
            }
        }

        aLog.debug("@@@ Ended afterInstallPackage");
    }

    /*************************************************************************************************
     * AOP : Server(Workload) CRUD Common Processing
     * 2020.03.18 : Package Install 시에도 사용...
     *************************************************************************************************/
    private void beforeServerProcessing(JoinPoint joinPoint) {
        this.beforeServerProcessing(joinPoint, false);
    }

    private void beforeServerProcessing(JoinPoint joinPoint, boolean isAddRequestInfo) {
        aLog.debug("@@@ Start beforeServerProcess");
        ExecutingContextVO context = null;
        for (Object obj : joinPoint.getArgs()) {
            if(obj instanceof ExecutingContextVO) {
                aLog.debug("############### ExecutingContextVO");
                context = (ExecutingContextVO)obj;
                break;
            }
        }

        if(context != null) { // serverProcess 처리 시간 기록..
            StopWatch sw = new StopWatch();
            sw.start();
            context.getParams().put(CommonConstants.AUDIT_STOPWATCH_OBJECT, sw); // For Audit Logging : (serverProcess ExecutionTime)
            /**
             * Snapshot으로 배포시에는 아래 정보를 설정하지 않기 위한 옵션... (deployTemplate에서 Workload생성전 기입력...)
             * - 아래 정보는 ASync 호출되고 Request가 종료되면 ContextHolder(ThreadLocal)가 소멸되므로 입력이 불가능.. => Transaction이 종료되기 전에 ExecutingContextVO에 셋팅 필요...
             *   1. 워크로드는 위의 AOP : ServerService.addWorkload.... 에서 본 메소드를 호출해서 입력...  (아직 ASync 처리전...)
             *   2. Snapshot 배포일경우 deployTemplate에서 워크로드 생성을 호출하면이 이미 ASync 처리가 되므로, deployTemplate에서 미리 입력해줌...
             */
            if(isAddRequestInfo) {
                context.getParams().put(CommonConstants.AUDIT_REQUEST_URI, Utils.getCurrentRequest().getRequestURI());
                context.getParams().put(CommonConstants.AUDIT_HTTP_METHOD, Utils.getCurrentRequest().getMethod());
                context.getParams().put(CommonConstants.AUDIT_REQUEST_DATA_PATH, ((Map)Utils.getCurrentRequest().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)));
                context.getParams().put(CommonConstants.AUDIT_REQUEST_REFERER, Utils.getCurrentRequest().getHeader(CommonConstants.AUDIT_REQUEST_REFERER));
                context.getParams().put(CommonConstants.AUDIT_REQUEST_CLIENT_IP, Utils.getClientIp());
                context.getParams().put(CommonConstants.AUDIT_REQUEST_UA, Utils.getCurrentRequest().getHeader(CommonConstants.AUDIT_REQUEST_UA));
                context.getParams().put(CommonConstants.AUDIT_CLASS_NAME, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_CLASS_NAME));
                context.getParams().put(CommonConstants.AUDIT_METHOD_NAME, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_METHOD_NAME));
                context.getParams().put(CommonConstants.AUDIT_USER_SEQ, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_USER_SEQ));
                context.getParams().put(CommonConstants.AUDIT_USER_SERVICE_SEQ, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_USER_SERVICE_SEQ));
                context.getParams().put(CommonConstants.AUDIT_USER_ROLE, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_USER_ROLE));
            }
        }
        aLog.debug("@@@ Ended beforeServerProcess");

    }
}
