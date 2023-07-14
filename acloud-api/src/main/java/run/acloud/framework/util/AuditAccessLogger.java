package run.acloud.framework.util;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import run.acloud.api.audit.service.AuditAccessService;
import run.acloud.api.audit.vo.AuditAccessLogVO;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.AuthVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.build.service.PipelineBuildRunService;
import run.acloud.api.build.service.PipelineBuildService;
import run.acloud.api.build.vo.*;
import run.acloud.api.catalog.service.TemplateService;
import run.acloud.api.catalog.vo.*;
import run.acloud.api.configuration.enums.IssueBindingType;
import run.acloud.api.configuration.service.AccountApplicationService;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.service.WorkloadGroupService;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.log.vo.LogAgentVO;
import run.acloud.api.log.vo.LogAgentViewVO;
import run.acloud.api.openapi.vo.ApiTokenIssueAddVO;
import run.acloud.api.openapi.vo.ApiTokenIssueEditVO;
import run.acloud.api.openapi.vo.ApiTokenVO;
import run.acloud.api.pipelineflow.service.PipelineFlowService;
import run.acloud.api.pipelineflow.vo.PipelineContainerVO;
import run.acloud.api.pipelineflow.vo.PipelineWorkloadVO;
import run.acloud.api.pl.vo.PlMasterVO;
import run.acloud.api.pl.vo.PlResBuildVO;
import run.acloud.api.pl.vo.PlResDeployVO;
import run.acloud.api.pl.vo.PlRunVO;
import run.acloud.api.resource.enums.CertIssuerScope;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.ResourceType;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.AuditAdditionalType;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.filter.LoggerFilter;
import run.acloud.framework.properties.CocktailAddonProperties;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AuditAccessLogger {

    @Autowired
    private AuditAccessService auditService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ServicemapService servicemapService;

    @Autowired
    private WorkloadGroupService workloadGroupService;

    @Autowired
    private PipelineBuildService pipelineBuildService;

    @Autowired
    private PipelineBuildRunService pipelineBuildRunService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private PipelineFlowService pipelineFlowService;

    @Autowired
    private CocktailAddonProperties cocktailAddonProperties;

    @Autowired
    private AccountApplicationService accountApplicationService;

    private static final Logger aLog = LoggerFactory.getLogger("audit.logger");

    public Map<String, Object> makeRequestDump(ServerHttpRequest request) throws IOException {
        return this.makeRequestDump(((ServletServerHttpRequest) request).getServletRequest());
    }

    public Map<String, Object> makeRequestDump(ServletRequest request) throws IOException {
        return this.makeRequestDump((HttpServletRequest)request);
    }

    public Map<String, Object> makeRequestDump(HttpServletRequest request) throws IOException {

        Map<String, Object> auditProcessingDatas = (Map<String, Object>) ((HashMap) ContextHolder.auditProcessingDatas()).clone();

        auditProcessingDatas.put(CommonConstants.AUDIT_REQUEST_URI, request.getRequestURI());
        auditProcessingDatas.put(CommonConstants.AUDIT_HTTP_METHOD, request.getMethod());
        auditProcessingDatas.put(CommonConstants.AUDIT_REQUEST_CLIENT_IP, Utils.getClientIp(request));
        auditProcessingDatas.put(CommonConstants.AUDIT_REQUEST_REFERER, request.getHeader(CommonConstants.AUDIT_REQUEST_REFERER));
        auditProcessingDatas.put(CommonConstants.AUDIT_REQUEST_UA, request.getHeader(CommonConstants.AUDIT_REQUEST_UA));

        /**
         * Set Request Data
         */
        Map<String, Object> requestData = new HashMap<>();

        // Path Variables로 수집된 Parameter 읽어서 셋팅
        Map<String, Object> uriTemplateVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        requestData.put(CommonConstants.AUDIT_REQUEST_DATA_PATH, uriTemplateVariables);

        // POST, PUT 요청시 Request Body 읽어서 셋팅
        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
            String requestBody = LoggerFilter.getBody(request); // UTF-8로 인코딩된 requestBody를 읽는다..
            if (StringUtils.isNotBlank(requestBody) && StringUtils.startsWith(requestBody, "{")) {
                JSONObject bodyJsonObject = new JSONObject(requestBody);
                requestData.put(CommonConstants.AUDIT_REQUEST_DATA_BODY, bodyJsonObject.toMap());
            }
            else if (StringUtils.isNotBlank(requestBody) && StringUtils.startsWith(requestBody, "[")) {
                JSONArray bodyJsonArray = new JSONArray(requestBody);
                requestData.put(CommonConstants.AUDIT_REQUEST_DATA_BODY, bodyJsonArray.toList());
            }
        }

        // multipart/form-data가 아닐 경우에만. Query Parameter 읽어서 셋팅
        if (!(request.getHeader("Content-Type") != null && request.getHeader("Content-Type").startsWith("multipart/"))) {
            Map<String, String[]> queryParameters = request.getParameterMap();
            requestData.put(CommonConstants.AUDIT_REQUEST_DATA_QUERY, queryParameters);
        }
        auditProcessingDatas.put(CommonConstants.AUDIT_REQUEST_DATAS, requestData);

        return auditProcessingDatas;
    }

    @Cacheable(value="isExistAuditAccessLogsTable")
    public boolean existTable(){
        return auditService.existAuditAccessLogsTable();
    }

    @Async
    public void write(ResultVO result, Map<String, Object> auditProcessingDatas) {
        try {
            aLog.debug("### Start Write");

            /**
             * Method에서 Parameter로 받을때는 JsonString 형태로 받은 후에
             * 1. ASync 처리중 Transaction 종료로 데이터 유실 방지
             * 2. AuditLog 처리중 result값을 저장하지 않기 위해 값을 조작하는 경우 서비스에 영향도가 없도록 함
             * ** JsonString 형태로 받아 Object로 변환시 캐스팅 오류 발생으로 주석 처리..
              */

            /**
             * Audit 처리를 위한 데이터 셋팅..
             */
            String requestUri = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI);
            String userAgent = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_UA);
            String method = (String) auditProcessingDatas.get(CommonConstants.AUDIT_HTTP_METHOD);
            String className = (String) auditProcessingDatas.get(CommonConstants.AUDIT_CLASS_NAME);
            String methodName = (String) auditProcessingDatas.get(CommonConstants.AUDIT_METHOD_NAME);
            Integer userSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SEQ);
            String userRole = (String) auditProcessingDatas.get(CommonConstants.AUDIT_USER_ROLE);
            Integer userServiceSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SERVICE_SEQ);
            double duration = (double)auditProcessingDatas.get(CommonConstants.AUDIT_DURATION);
            String clientIp = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_CLIENT_IP);
            String referer = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_REFERER);
            Map<String, Object> requestData = (Map<String, Object>) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS);

            aLog.debug("===== : requestUri : " + requestUri);

            /**
             * 예외 케이스 처리..
             */
            if ("GET".equalsIgnoreCase(method)
                    && !((className.equalsIgnoreCase("AccountController") && methodName.equalsIgnoreCase("getUsersOfAccount"))
                            || (className.equalsIgnoreCase("AccountController") && methodName.equalsIgnoreCase("getUserOfAccount"))
                            || (className.equalsIgnoreCase("UserController") && methodName.equalsIgnoreCase("getUsers"))
                            || (className.equalsIgnoreCase("UserController") && methodName.equalsIgnoreCase("getUser"))
                            || (className.equalsIgnoreCase("AccountApplicationController") && methodName.equalsIgnoreCase("detailAccountApplications"))
                        )
            ) {
                aLog.debug("### Skip Write ( Request with GET Method ) : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
                return;
            }

            if (Optional.ofNullable(userSeq).orElseGet(() ->0) == 0) { // = userSeq is null or 0
                // 사용자 정보 없이 audit logging까지 진입 => Audit Logging이 필요 없는 예외 요청일 가능성 99% 로 제외함..
                // (사용자 정보가 없는 경우 리소스 인가 필터에 의해 진입이 불가능하기 때문..)
                if(!methodName.equalsIgnoreCase("login") // 로그인 처리시에는 userSeq가 없음 => 예외
                    && !methodName.equalsIgnoreCase("editUserOtpInfo") // /internal url은 userSeq가 없음 => 예외
                    && !methodName.equalsIgnoreCase("loginCocktailAdmin") // 로그인 처리시에는 userSeq가 없음 => 예외
                    && !methodName.equalsIgnoreCase("loginPlatformAdmin") // 로그인 처리시에는 userSeq가 없음 => 예외
                    && !methodName.equalsIgnoreCase("loginPlatformUser") // 로그인 처리시에는 userSeq가 없음 => 예외
                ) {
                    aLog.debug("### Skip Write ( Request without User Information ) : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
                    return;
                }
            }

            String resultStatus = "";
            Object resultResult = null;
            if (result != null) {
                resultStatus = result.getStatus();
                resultResult = result.getResult();
            }

            /**
             * Audit Data 객체 생성..
             */
            AuditAccessLogVO auditLog = new AuditAccessLogVO();

            auditLog.setLogCode(className+"."+methodName);
            auditLog.setHttpMethod(method);
            auditLog.setUri(requestUri);
            auditLog.setUserAgent(userAgent);
            auditLog.setClientIp(clientIp);
            auditLog.setReferer(referer);
            auditLog.setControllerName(className);
            auditLog.setMethodName(methodName);
            auditLog.setServiceSeq(userServiceSeq);
            auditLog.setUserSeq(userSeq);
            auditLog.setUserRole(userRole);


            /** status code change **/
            if(result != null && "ok".equalsIgnoreCase(resultStatus)) {
                auditLog.setResultCode("SUCCESS");
            }
            else {
                auditLog.setResultCode("FAILURE");
            }

            /** 각 리소스 유형별 데이터 생성 1 : get Data*/
            Map<String, Object> auditResourceDatas = this.generateEachResourceData(auditProcessingDatas, requestData, result);
            if(StringUtils.equalsIgnoreCase("Y", MapUtils.getString(auditResourceDatas, "isSkip", ""))) {
                log.debug("### @CATCH : Skip Write ( login in progress ) : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
                return;
            }

            // 사용자 추가 정보 셋팅
            // generateEachResourceData 메서드내에서 로그인 일 경우는 auditProcessingDatas에 사용자 정보 셋팅함.
            if (Optional.ofNullable(userSeq).orElseGet(() ->0) == 0) {
                userSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SEQ);
                userRole = (String) auditProcessingDatas.get(CommonConstants.AUDIT_USER_ROLE);

                auditLog.setUserSeq(userSeq);
                auditLog.setUserRole(userRole);
            }

            UserVO userVO = userService.getByUserSeq(userSeq);
            if ( userVO != null ) {
                auditLog.setUserId(userVO.getUserId());
                auditLog.setUserName(userVO.getUserName());
            } else {
                auditLog.setUserId((String) auditProcessingDatas.get(CommonConstants.AUDIT_USER_ID));
                auditLog.setUserName((String) auditProcessingDatas.get(CommonConstants.AUDIT_USER_NAME));
            }

            /** 각 리소스 유형별 데이터 생성 2 : set Data.*/
            auditLog.setResourceData(JsonUtils.toPrettyString(auditResourceDatas));

            /** Set Request Body (로그인 / 비밀번호 변경시 RequestBody 제거하고 설정) **/
            if(methodName.equalsIgnoreCase("login") ||
                methodName.equalsIgnoreCase("loginCocktailAdmin") ||
                methodName.equalsIgnoreCase("loginPlatformAdmin") ||
                methodName.equalsIgnoreCase("loginPlatformUser") ||
                methodName.equalsIgnoreCase("logout") ||
                methodName.equalsIgnoreCase("editUserOtpInfo") ||
                methodName.equalsIgnoreCase("checkPassword") ||
                methodName.equalsIgnoreCase("changePassword")) { // 비밀번호등 주요 정보 요청시 Request Body를 Null로 재 설정.. (비밀번호 Null..)
                requestData.put(CommonConstants.AUDIT_REQUEST_DATA_BODY, "Not visible by security policy.");
                auditLog.setRequestData(JsonUtils.toPrettyString(requestData));
            }
            else {
                auditLog.setRequestData(JsonUtils.toPrettyString(requestData));
            }

            /** Set Resource Name */
            if(auditResourceDatas.get("resourceName") != null) {
                auditLog.setResourceName(auditResourceDatas.get("resourceName").toString());
            }
            else {
                aLog.debug("### @CATCH : Skip Write ( resourceName is null : Not a target for Audit ) : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
                return;
            }

            /** 각 리소스 유형별 데이터에서 획득한 서비스 연결 정보 기록 */
            if(auditResourceDatas.get("serviceRelation") != null) {
                if(auditResourceDatas.get("serviceRelation") instanceof ServiceRelationVO) {
                    auditLog = this.setServiceRelation(auditLog, (ServiceRelationVO)auditResourceDatas.get("serviceRelation"));
                }
            }

            /** Set Response Data */
            if(resultResult != null) { // ResultVO를 Response Data에 포함시킬 것인가.. (Response Status는 별도 필드로 표시중이므로 제외함..)
                if(methodName.equalsIgnoreCase("login")
                        || methodName.equalsIgnoreCase("loginCocktailAdmin")
                        || methodName.equalsIgnoreCase("loginPlatformAdmin")
                        || methodName.equalsIgnoreCase("loginPlatformUser")
                        || methodName.equalsIgnoreCase("logout")
                ) {
                    auditLog.setResponseData("{\"message\":\"Not visible by security policy.\"}");
                }
                else {
                    auditLog.setResponseData(JsonUtils.toPrettyString(resultResult));
                }
            }

            /** Set User Seq : Header에 UserSeq가 없을때 데이터에서 찾아서 입력하도록 함 **/
            if(methodName.equalsIgnoreCase("login") ||
                methodName.equalsIgnoreCase("loginCocktailAdmin") ||
                methodName.equalsIgnoreCase("loginPlatformAdmin") ||
                methodName.equalsIgnoreCase("loginPlatformUser") ||
                methodName.equalsIgnoreCase("logout") ||
                methodName.equalsIgnoreCase("editUserOtpInfo")) {
                if(auditLog.getUserSeq() == null || auditLog.getUserSeq() < 1) {
                    auditLog.setUserSeq(MapUtils.getInteger(auditResourceDatas, "userSeq", null)); // 로그인은 Header 정보가 없으므로, UserSeq 설정.
                }
            }

            /** Audit Log 등록용도인 api 실패 처리 로직 **/
            if (StringUtils.equalsAnyIgnoreCase(auditLog.getLogCode(), "TerminalController.addLogTerminalOnOpen")) {
                Map<String, String[]> queryParameters = (Map<String, String[]>) requestData.get(CommonConstants.AUDIT_REQUEST_DATA_QUERY);
                if (MapUtils.isNotEmpty(queryParameters)) {
                    if (MapUtils.getObject(queryParameters, "success", null) != null) {
                        String[] success = queryParameters.get("success");
                        if (!BooleanUtils.toBoolean(success[0])) {
                            auditLog.setResultCode("FAILURE");
                        }
                        // Request 정보에서 query string 삭제
                        requestData.remove(CommonConstants.AUDIT_REQUEST_DATA_QUERY);
                        auditLog.setRequestData(JsonUtils.toPrettyString(requestData));
                    }
                }
            }

            /** Set Duration */
            auditLog.setDuration(duration);

            /** insert Audit Log */
            int count = auditService.addAuditAccessLog(auditLog);

            /** 기본 요청이 실패하였을때는 추가 Audit Log를 기록하지 않음... (ex : snapshot 배포중 exception 발생... **/
            if("FAILURE".equals(auditLog.getResultCode())) {
                aLog.debug("### Skip Additional Log : because of main requst failure");
                return;
            }

            /** ================================
             * Additional Audit Log에 대한 처리...
             * - 현재 스냅샷(카탈로그 템플릿) 배포시에만 사용.
             */
            try {
                // Additional Audit Log 관련 데이터가 둘다 존재하지 않으면 종료..
                if (auditProcessingDatas.get(CommonConstants.AUDIT_ADDITIONAL_TYPE) == null ||
                    auditProcessingDatas.get(CommonConstants.AUDIT_ADDITIONAL_DATAS) == null) {
                    aLog.debug("### Skip Additional Log : Can't Found Additional Datas");
                    return;
                }
                String type = (String) auditProcessingDatas.get(CommonConstants.AUDIT_ADDITIONAL_TYPE);
                Optional<AuditAdditionalType> auditAdditionalTypeOptional = Arrays.stream(AuditAdditionalType.values()).filter(a -> a.getCode().equalsIgnoreCase(type)).findFirst();

                // Additional Audit Type이 없으면 종료..
                if (!auditAdditionalTypeOptional.isPresent()) {
                    aLog.debug("### Skip Additional Log : Additional Type Mismatch");
                    return;
                }

//                Thread.sleep(3000);
                aLog.debug("### Start Additional Log Write");
                switch (auditAdditionalTypeOptional.get()) {
                    case LAUNCH_TEMPLATE:
                        List<ConfigMapIntegrateVO> configMaps = null;
                        List<SecretIntegrateVO> secrets = null;
                        List<K8sCRDNetAttachDefIntegrateVO> netAttachDefs = null;
                        /** R4.1 **/
                        List<ServiceSpecIntegrateVO> services = null;
                        List<IngressSpecIntegrateVO> ingresses = null;
                        List<PersistentVolumeClaimIntegrateVO> pvcs = null;
                        List<CommonYamlVO> roles = null;
                        List<CommonYamlVO> roleBindings = null;
                        List<CommonYamlVO> serviceAccounts = null;
                        List<CommonYamlVO> customObjects = null;

                        ServicemapVO servicemap = null;

                        // Additional Log 적재를 위한 데이터 조회
                        Map<String, Object> templateDatas = (Map)auditProcessingDatas.get(CommonConstants.AUDIT_ADDITIONAL_DATAS);
                        if (templateDatas.get(ResourceType.SERVICEMAP.getCode()) != null) {
                            servicemap = (ServicemapVO)templateDatas.get(ResourceType.SERVICEMAP.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.CONFIG_MAP.getCode()) != null) {
                            configMaps = (List<ConfigMapIntegrateVO>)templateDatas.get(K8sApiKindType.CONFIG_MAP.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.SECRET.getCode()) != null) {
                            secrets = (List<SecretIntegrateVO>)templateDatas.get(K8sApiKindType.SECRET.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION.getCode()) != null) {
                            netAttachDefs = (List<K8sCRDNetAttachDefIntegrateVO>)templateDatas.get(K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.SERVICE.getCode()) != null) {
                            services = (List<ServiceSpecIntegrateVO>)templateDatas.get(K8sApiKindType.SERVICE.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.INGRESS.getCode()) != null) {
                            ingresses = (List<IngressSpecIntegrateVO>)templateDatas.get(K8sApiKindType.INGRESS.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.PERSISTENT_VOLUME_CLAIM.getCode()) != null) {
                            pvcs = (List<PersistentVolumeClaimIntegrateVO>)templateDatas.get(K8sApiKindType.PERSISTENT_VOLUME_CLAIM.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.ROLE.getCode()) != null) {
                            roles = (List<CommonYamlVO>)templateDatas.get(K8sApiKindType.ROLE.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.ROLE_BINDING.getCode()) != null) {
                            roleBindings = (List<CommonYamlVO>)templateDatas.get(K8sApiKindType.ROLE_BINDING.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.SERVICE_ACCOUNT.getCode()) != null) {
                            serviceAccounts = (List<CommonYamlVO>)templateDatas.get(K8sApiKindType.SERVICE_ACCOUNT.getCode());
                        }
                        if (templateDatas.get(K8sApiKindType.CUSTOM_OBJECT.getCode()) != null) {
                            customObjects = (List<CommonYamlVO>)templateDatas.get(K8sApiKindType.CUSTOM_OBJECT.getCode());
                        }

                        // 상위(카탈로그배포) Audit Log 데이터로부터 카탈로그 정보를 수집 및 재설정 후 Resource 정보 초기화.
                        String templateName = (String)auditResourceDatas.get("resourceName");
                        auditResourceDatas.put("catalogueName", templateName);
                        auditResourceDatas.remove("resourceName");
                        Integer templateSeq = (Integer)auditResourceDatas.get("resourceSeq");
                        auditResourceDatas.put("catalogueSeq", templateSeq);
                        auditResourceDatas.remove("resourceSeq");

                        if(servicemap != null) {
                            auditResourceDatas.put("resourceName", servicemap.getServicemapName());
                            auditResourceDatas.put("namespaceName", servicemap.getNamespaceName());
                            auditResourceDatas.put("resourceSeq", servicemap.getServicemapSeq());
                            auditLog.setResourceName(servicemap.getServicemapName());
                            auditLog.setResourceData(JsonUtils.toPrettyString(auditResourceDatas));
                            auditLog.setLogSeq(null);
                            auditLog.setLogCode(className+"."+methodName+"addServicemap");
                            count = auditService.addAuditAccessLog(auditLog);
                            aLog.debug("### Additional Audit Log (Servicemap) Write ("+count+") : " + requestUri);
                        }
                        if(CollectionUtils.isNotEmpty(configMaps)) {
                            // Configmap 입력
                            for(ConfigMapIntegrateVO configmap : configMaps) {
                                try {
                                    switch (DeployType.valueOf(configmap.getDeployType())) {
                                        case GUI:
                                            this.writeAdditionalResourceLog(((ConfigMapGuiVO) configmap).getName(), "addConfigMap", auditResourceDatas, auditLog);
                                            break;
                                        case YAML:
                                            auditResourceDatas.put("yaml", ((ConfigMapYamlVO) configmap).getYaml());
                                            this.writeAdditionalResourceLog(((ConfigMapYamlVO) configmap).getName(), "addConfigMap", auditResourceDatas, auditLog);
                                            break;
                                    }
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addConfigMap) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(secrets)) {
                            // Secret 입력
                            for(SecretIntegrateVO secret : secrets) {
                                try {
                                    switch (DeployType.valueOf(secret.getDeployType())) {
                                        case GUI:
                                            this.writeAdditionalResourceLog(((SecretGuiVO) secret).getName(), "addSecret", auditResourceDatas, auditLog);
                                            break;
                                        case YAML:
                                            auditResourceDatas.put("yaml", ((SecretYamlVO) secret).getYaml());
                                            this.writeAdditionalResourceLog(((SecretYamlVO) secret).getName(), "addSecret", auditResourceDatas, auditLog);
                                            break;
                                    }
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addSecret) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(netAttachDefs)) {
                            // Network 입력
                            for(K8sCRDNetAttachDefIntegrateVO network : netAttachDefs) {
                                try {
                                    switch (DeployType.valueOf(network.getDeployType())) {
                                        case GUI:
                                            auditResourceDatas.put("type", ((K8sCRDNetAttachDefGuiVO) network).getType());
                                            this.writeAdditionalResourceLog(((K8sCRDNetAttachDefGuiVO) network).getName(), "addNetAttachDef", auditResourceDatas, auditLog);
                                            break;
                                        case YAML:
                                            auditResourceDatas.put("type", ((K8sCRDNetAttachDefYamlVO) network).getType());
                                            auditResourceDatas.put("yaml", ((K8sCRDNetAttachDefYamlVO) network).getYaml());
                                            this.writeAdditionalResourceLog(((K8sCRDNetAttachDefYamlVO) network).getName(), "addNetAttachDef", auditResourceDatas, auditLog);
                                            break;
                                    }
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addNetAttachDef) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(services)) {
                            // SERVICE 입력
                            for(ServiceSpecIntegrateVO service : services) {
                                try {
                                    switch (DeployType.valueOf(service.getDeployType())) {
                                        case GUI:
                                            this.writeAdditionalResourceLog(((ServiceSpecGuiVO) service).getName(), "addServiceSpec", auditResourceDatas, auditLog);
                                            break;
                                        case YAML:
                                            auditResourceDatas.put("yaml", ((ServiceSpecYamlVO) service).getYaml());
                                            this.writeAdditionalResourceLog(((ServiceSpecYamlVO) service).getName(), "addServiceSpec", auditResourceDatas, auditLog);
                                            break;
                                    }
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addServiceSpec) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(ingresses)) {
                            // INGRESS 입력
                            for(IngressSpecIntegrateVO ingress : ingresses) {
                                try {
                                    switch (DeployType.valueOf(ingress.getDeployType())) {
                                        case GUI:
                                            this.writeAdditionalResourceLog(((IngressSpecGuiVO) ingress).getName(), "addIngressSpec", auditResourceDatas, auditLog);
                                            break;
                                        case YAML:
                                            auditResourceDatas.put("yaml", ((IngressSpecYamlVO) ingress).getYaml());
                                            this.writeAdditionalResourceLog(((IngressSpecYamlVO) ingress).getName(), "addIngressSpec", auditResourceDatas, auditLog);
                                            break;
                                    }
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addIngressSpec) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(pvcs)) {
                            // PERSISTENT_VOLUME_CLAIM 입력
                            for(PersistentVolumeClaimIntegrateVO pvc : pvcs) {
                                try {
                                    switch (DeployType.valueOf(pvc.getDeployType())) {
                                        case GUI:
                                            auditResourceDatas.put("capacity", ((PersistentVolumeClaimGuiVO) pvc).getCapacity().toString() + "G");
                                            auditResourceDatas.put("accessMode", ((PersistentVolumeClaimGuiVO) pvc).getAccessMode());
                                            auditResourceDatas.put("persistentVolumeType", ((PersistentVolumeClaimGuiVO) pvc).getPersistentVolumeType());
                                            this.writeAdditionalResourceLog(((PersistentVolumeClaimGuiVO) pvc).getName(), "addPersistentVolumeClaimeV2", auditResourceDatas, auditLog);
                                            break;
                                        case YAML:
                                            auditResourceDatas.put("yaml", ((PersistentVolumeClaimYamlVO) pvc).getYaml());
                                            this.writeAdditionalResourceLog(((PersistentVolumeClaimYamlVO) pvc).getName(), "addPersistentVolumeClaimeV2", auditResourceDatas, auditLog);
                                            break;
                                    }
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addPersistentVolumeClaimeV2) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(roles)) {
                            // ROLE 입력
                            for(CommonYamlVO role : roles) {
                                try {
                                    auditResourceDatas.put("yaml", role.getYaml());
                                    this.writeAdditionalResourceLog(role.getName(), "addRole", auditResourceDatas, auditLog);
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addRole) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(roleBindings)) {
                            // ROLE_BINDING 입력
                            for(CommonYamlVO roleBinding : roleBindings) {
                                try {
                                    auditResourceDatas.put("yaml", roleBinding.getYaml());
                                    this.writeAdditionalResourceLog(roleBinding.getName(), "addRoleBinding", auditResourceDatas, auditLog);
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addRoleBinding) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(serviceAccounts)) {
                            // SERVICE_ACCOUNT 입력
                            for(CommonYamlVO serviceAccount : serviceAccounts) {
                                try {
                                    auditResourceDatas.put("yaml", serviceAccount.getYaml());
                                    this.writeAdditionalResourceLog(serviceAccount.getName(), "addServiceAccount", auditResourceDatas, auditLog);
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addServiceAccount) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        if(CollectionUtils.isNotEmpty(customObjects)) {
                            // CUSTOM_OBJECT 입력
                            for(CommonYamlVO customObject : customObjects) {
                                try {
                                    auditResourceDatas.put("yaml", customObject.getYaml());
                                    this.writeAdditionalResourceLog(customObject.getName(), "addCustomObject", auditResourceDatas, auditLog);
                                }
                                catch (Exception ex) {
                                    // 에러 logging 후 다음 처리 계속...
                                    aLog.debug("### Additional Audit Log (addCustomObject) Write Failure (catch exception) : " + auditLog.getUri() + "\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                        break;
                    case PIPELINE_DEPLOY:
                        break;
                    default:
                        break;
                }
            }
            catch (Exception ex) {
                if(aLog.isDebugEnabled()) {
                    aLog.debug("trace log ", ex);
                    aLog.debug("### @CATCH : Additional Audit Log Write Running Failure : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
                }
                else {
                    aLog.warn("### @CATCH : Additional Audit Log Write Running Failure : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas), ex);
                }
            }

            aLog.debug(String.format("### status [ %s ] : ExecutionTime[ %ss ]", resultStatus, duration));
            aLog.debug("### Ended Write ("+count+") : " + requestUri);
        }
        catch (Exception ex) {
            if(aLog.isDebugEnabled()) {
                aLog.debug("trace log ", ex);
                aLog.debug("### @CATCH : Write Running Failure : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
            }
            else {
                aLog.warn("### @CATCH : Write Running Failure : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas), ex);
            }
        }
    }

    /**
     * Additional Audit Log 기록...
     * @param resourceName
     * @param auditResourceDatas
     * @param auditLog
     * @throws Exception
     */
    private void writeAdditionalResourceLog(String resourceName, String additionalLogCode, Map<String, Object> auditResourceDatas, AuditAccessLogVO auditLog) throws Exception {
        if(StringUtils.isNotBlank(resourceName)) {
            auditResourceDatas.put("resourceName", resourceName);
            auditLog.setResourceName(resourceName);
            auditLog.setResourceData(JsonUtils.toPrettyString(auditResourceDatas));
            auditLog.setLogSeq(null);
            auditLog.setLogCode(auditLog.getControllerName() + "." + auditLog.getMethodName() + additionalLogCode);
            int count = auditService.addAuditAccessLog(auditLog);
            aLog.debug("### Additional Audit Log (" + auditLog.getLogCode() + ") Write (" + count + ") : " + auditLog.getUri());
        }
        else {
            aLog.debug("### Additional Audit Log (" + auditLog.getLogCode() + ") Write Failure (name is null) : " + auditLog.getUri());
        }
    }

    /**
     * 비동기로 워크로드 생성시 워크로드 생성에 대한 Audit Log 기재..
     * @param auditProcessingDatas
     * @param isCatalogue
     * @throws Exception
     */
    @Async
    public void writeForWorkloadProcess(Map<String, Object> auditProcessingDatas, boolean isCatalogue) {
        try {
//            Thread.sleep(6000);
            aLog.debug("### Start Write for WorkloadProcess");

            /**
             * Audit 처리를 위한 데이터 셋팅..
             */

            String requestUri = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI);
            String userAgent = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_UA);
            String method = (String) auditProcessingDatas.get(CommonConstants.AUDIT_HTTP_METHOD);
            String clientIp = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_CLIENT_IP);
            String referer = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_REFERER);
            String className = (String) auditProcessingDatas.get(CommonConstants.AUDIT_CLASS_NAME);
            String methodName = (String) auditProcessingDatas.get(CommonConstants.AUDIT_METHOD_NAME);
            Integer userSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SEQ);
            Integer userServiceSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SERVICE_SEQ);
            String userRole = (String) auditProcessingDatas.get(CommonConstants.AUDIT_USER_ROLE);
            String auditResultCode = (String) auditProcessingDatas.get(CommonConstants.AUDIT_RESULT_CODE);
            double duration = (double) auditProcessingDatas.get(CommonConstants.AUDIT_DURATION);

            Map<String, Object> uriTemplateVariables = (Map) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATA_PATH);

            aLog.debug("===== : requestUri : " + requestUri);

            ServerGuiVO serverGui = null;
            ServerYamlVO serverYaml = null;
            if(auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS) != null) {
                DeployType deployType = DeployType.valueOf(((ServerIntegrateVO) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS)).getDeployType());
                switch (deployType) {
                    case GUI:
                        serverGui = (ServerGuiVO) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS);
                        break;
                    case YAML:
                        serverYaml = (ServerYamlVO) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS);
                        break;
                }
            }

            // Input 데이터가 없거나.. ServerGuiVO, ServerYamlVO 둘중 하나와도 매칭되지 못하면 종료..
            if(auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS) == null || (serverGui == null && serverYaml == null)) {
                if(aLog.isDebugEnabled()) {
                    aLog.debug("### @CATCH : WorkloadProcess Audit Logging Failed (Workload Data is null) : " + requestUri);
                }
                else {
                    aLog.error("### @CATCH : WorkloadProcess Audit Logging Failed (Workload Data is null) : " + requestUri);
                }
                return;
            }

            /**
             * Audit Data 객체 생성..
             */
            AuditAccessLogVO auditLog = new AuditAccessLogVO();

            if(isCatalogue) {
                auditLog.setLogCode(className + "." + methodName + "addWorkload");
            }
            else {
                auditLog.setLogCode(className + "." + methodName + "Process");
            }
            auditLog.setHttpMethod(method);
            auditLog.setUri(requestUri);
            auditLog.setUserAgent(userAgent);
            auditLog.setClientIp(clientIp);
            auditLog.setReferer(referer);
            auditLog.setControllerName(className);
            auditLog.setMethodName(methodName);
            auditLog.setUserSeq(userSeq);
            auditLog.setUserRole(userRole);
            auditLog.setServiceSeq(userServiceSeq);
            auditLog.setResultCode(auditResultCode);

            // 사용자 추가 정보 셋팅
            UserVO userVO = userService.getByUserSeq(userSeq);
            auditLog.setUserId(userVO.getUserId());
            auditLog.setUserName(userVO.getUserName());

            /** 리소스 데이터 생성 */
            Map<String, Object> auditResourceDatas = new HashMap<>();

            Integer relationKey = null;
            if(isCatalogue) {
                if (serverGui != null) {
                    relationKey = Optional.ofNullable(serverGui).map(ServerGuiVO::getComponent).map(ComponentVO::getServicemapInfo).map(ServicemapSummaryVO::getServicemapSeq).orElseGet(() ->null);
                }
                else if (serverYaml != null) {
                    String namespace = Optional.ofNullable(serverYaml).map(ServerYamlVO::getNamespaceName).orElseGet(() ->null);
                    Integer clusterSeq = Optional.ofNullable(serverYaml).map(ServerYamlVO::getClusterSeq).orElseGet(() ->null);
                    relationKey = servicemapService.getServicemapSeqByNamespace(clusterSeq, namespace);
                }
            }
            else {
                relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
            }
            String relationKeyName = "servicemapSeq";
            aLog.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            aLog.debug(relationKeyName + ":" + relationKey);
            aLog.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

            /** Set Request / Response Body **/
            if(serverGui != null) {
                auditLog.setRequestData(JsonUtils.toPrettyString(serverGui));
                auditLog.setResponseData(JsonUtils.toPrettyString(serverGui));
                auditResourceDatas.put("resourceName", serverGui.getComponent().getComponentName());
                auditResourceDatas.put("workloadType", serverGui.getServer().getWorkloadType());
                aLog.debug(JsonUtils.toGson(serverGui));
            }
            else if(serverYaml != null) {
                auditLog.setRequestData(JsonUtils.toPrettyString(serverYaml));
                auditLog.setResponseData(JsonUtils.toPrettyString(serverYaml));
                auditResourceDatas.put("resourceName", serverYaml.getWorkloadName());
                auditResourceDatas.put("workloadType", serverYaml.getWorkloadType());
                aLog.debug(JsonUtils.toGson(serverYaml));
            }

            if (relationKey != null && StringUtils.isNotBlank(relationKeyName)) { // serviceRelation 조회가 가능 = Service Relation 정보 입력
                ServiceRelationVO serviceRelation = this.getServiceRelation(relationKeyName, relationKey);
                auditResourceDatas.put("serviceRelation", serviceRelation);
            }

            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_REFERER, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_REFERER));
            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_CLIENT_IP, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_CLIENT_IP));
            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_UA, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_UA));

            // 카탈로그로 배포되었을 경우 나를 배포한(?) 카탈로그의 정보 기록..
            if(isCatalogue) {
                if (auditProcessingDatas.get("catalogueSeq") != null) { // TemplateController.launchTemplate 에서 셋팅..
                    Integer templateSeq = 0;
                    if(auditProcessingDatas.get("catalogueSeq") != null) {
                        templateSeq = (Integer) auditProcessingDatas.get("catalogueSeq");
                    }
                    if (templateSeq > 0) {
                        TemplateDetailVO templateDetail = templateService.getTemplateDetail(templateSeq, null, false);
                        auditResourceDatas.put("catalogueName", templateDetail.getTemplateName());
                        auditResourceDatas.put("catalogueSeq", templateSeq);
                    }
                }
            }

            // auditLog Data 기록.
            auditLog.setResourceData(JsonUtils.toPrettyString(auditResourceDatas));

            /** Set Resource Name */
            if(auditResourceDatas.get("resourceName") != null) {
                auditLog.setResourceName(auditResourceDatas.get("resourceName").toString());
            }
            else {
                aLog.debug("### @CATCH : Skip Write For WorkloadProcess ( resourceName is null : Not a target for Audit ) : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
                return;
            }

            /** 서비스 연결 정보 기록 */
            if(auditResourceDatas.get("serviceRelation") != null &&
                (auditResourceDatas.get("serviceRelation") instanceof ServiceRelationVO))
            {
                auditLog = this.setServiceRelation(auditLog, (ServiceRelationVO)auditResourceDatas.get("serviceRelation"));
            }

            /** Set Duration */
            auditLog.setDuration(duration);

            /** insert Audit Log */
            int count = auditService.addAuditAccessLog(auditLog);

            aLog.debug(String.format("### status [ %s ] : ExecutionTime[ %ss ]", auditResultCode, duration));
            aLog.debug("### Ended Write For WorkloadProcess ("+count+") : " + requestUri);
        }
        catch (Exception ex) {
            if(aLog.isDebugEnabled()) {
                aLog.debug("trace log ", ex);
                aLog.debug("### @CATCH : Write For WorkloadProcess Running Failure : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
            }
            else {
                aLog.error("### @CATCH : Write For WorkloadProcess Running Failure : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas), ex);
            }
        }
    }

    /**
     * Snapshot으로 패키지 Install시 Audit Log 기재..
     * @param auditProcessingDatas
     * @param isCatalogue
     * @throws Exception
     */
    @Async
    public void writeForInstallPackage(Map<String, Object> auditProcessingDatas, boolean isCatalogue) {
        try {
//            Thread.sleep(6000);
            aLog.debug("### Start Write for InstallPackage");

            /**
             * Audit 처리를 위한 기본 데이터 셋팅..
             */
            String requestUri = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI);
            String userAgent = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_UA);
            String clientIp = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_CLIENT_IP);
            String referer = (String) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_REFERER);
            String method = (String) auditProcessingDatas.get(CommonConstants.AUDIT_HTTP_METHOD);
            String className = (String) auditProcessingDatas.get(CommonConstants.AUDIT_CLASS_NAME);
            String methodName = (String) auditProcessingDatas.get(CommonConstants.AUDIT_METHOD_NAME);
            Integer userSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SEQ);
            Integer userServiceSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SERVICE_SEQ);
            String userRole = (String) auditProcessingDatas.get(CommonConstants.AUDIT_USER_ROLE);
            double duration = (double) auditProcessingDatas.get(CommonConstants.AUDIT_DURATION);
            String auditResultCode = (String) auditProcessingDatas.get(CommonConstants.AUDIT_RESULT_CODE);

//            Map<String, Object> uriTemplateVariables = (Map) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATA_PATH);
            aLog.debug("===== : requestUri : " + requestUri);

            HelmInstallRequestVO helmInstallRequest = null;
            HelmReleaseBaseVO helmReleaseBase = null;
            if(auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS) != null) {
                helmInstallRequest = (HelmInstallRequestVO) auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS);
            }
            else {
                if(aLog.isDebugEnabled()) {
                    aLog.debug("### @CATCH : InstallPackage Audit Logging Failed (Package Install Data is null) : " + requestUri);
                }
                else {
                    aLog.error("### @CATCH : InstallPackage Audit Logging Failed (Package Install Data is null) : " + requestUri);
                }
                return;
            }

            /** 처리 결과가 성공일때만 Result Data를 Get **/
            if(StringUtils.equals("SUCCESS", auditResultCode)) {
                if(auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_DATAS) != null) {
                    helmReleaseBase = (HelmReleaseBaseVO) auditProcessingDatas.get(CommonConstants.AUDIT_RESULT_DATAS);
                }
                else {
                    if(aLog.isDebugEnabled()) {
                        aLog.debug("### @CATCH : InstallPackage Audit Logging Failed (Package Response Data is null) : " + requestUri);
                    }
                    else {
                        aLog.error("### @CATCH : InstallPackage Audit Logging Failed (Package Response Data is null) : " + requestUri);
                    }
                    return;
                }
            }

            /**
             * Audit Data 객체 생성..
             */
            AuditAccessLogVO auditLog = new AuditAccessLogVO();

            if(!isCatalogue) { // 카탈로그 배포가 아니면 Audit Log를 적재하지 않음...
                if(aLog.isDebugEnabled()) {
                    aLog.debug("### Skip Write Audit For InstallPackage (not catalogue) : " + requestUri);
                }
                return;
            }
            auditLog.setLogCode(className + "." + methodName + "installPackage");

            auditLog.setHttpMethod(method);
            auditLog.setUri(requestUri);
            auditLog.setUserAgent(userAgent);
            auditLog.setClientIp(clientIp);
            auditLog.setReferer(referer);
            auditLog.setControllerName(className);
            auditLog.setMethodName(methodName);
            auditLog.setUserSeq(userSeq);
            auditLog.setUserRole(userRole);
            auditLog.setServiceSeq(userServiceSeq);
            auditLog.setResultCode(auditResultCode);

            // 사용자 추가 정보 셋팅
            UserVO userVO = userService.getByUserSeq(userSeq);
            auditLog.setUserId(userVO.getUserId());
            auditLog.setUserName(userVO.getUserName());

            /** 리소스 데이터 생성 */
            Map<String, Object> auditResourceDatas = new HashMap<>();

            Integer relationKey = 0;
            String relationKeyName = "servicemapSeq";

            /** Set Request / Response Body **/
            if(helmInstallRequest != null) {
                helmInstallRequest.setClusterAccessInfo(null);
                auditLog.setRequestData(JsonUtils.toPrettyString(helmInstallRequest));
                auditResourceDatas.put("resourceName", helmInstallRequest.getReleaseName());
                auditResourceDatas.put("repository", helmInstallRequest.getRepo());
                auditResourceDatas.put("chart", helmInstallRequest.getChartName() + "-" + helmInstallRequest.getVersion());
                aLog.debug(JsonUtils.toGson(helmInstallRequest));
            }
            if(helmReleaseBase != null) {
                // TODO: Servicemap
                if (helmReleaseBase.getServicemapInfo() != null) {
                    relationKey = helmReleaseBase.getServicemapInfo().getServicemapSeq();
                }
                helmReleaseBase.setChart(null);
                auditLog.setResponseData(JsonUtils.toPrettyString(helmReleaseBase));
            }

            aLog.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            aLog.debug(relationKeyName + ":" + relationKey);
            aLog.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");


            if (relationKey != null && StringUtils.isNotBlank(relationKeyName)) { // serviceRelation 조회가 가능 = Service Relation 정보 입력
                ServiceRelationVO serviceRelation = this.getServiceRelation(relationKeyName, relationKey);
                auditResourceDatas.put("serviceRelation", serviceRelation);
            }

            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_CLIENT_IP, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_CLIENT_IP));
            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_REFERER, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_REFERER));
            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_UA, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_UA));

            // 카탈로그로 배포되었을 경우 나를 배포한(?) 카탈로그의 정보 기록..
            if(isCatalogue) {
                if (auditProcessingDatas.get("catalogueSeq") != null) { // TemplateController.deployTemplate(launchTemplate) 에서 셋팅..
                    Integer templateSeq = 0;
                    if(auditProcessingDatas.get("catalogueSeq") != null) {
                        templateSeq = (Integer) auditProcessingDatas.get("catalogueSeq");
                    }
                    if (templateSeq > 0) {
                        TemplateDetailVO templateDetail = templateService.getTemplateDetail(templateSeq, null, false);
                        auditResourceDatas.put("catalogueName", templateDetail.getTemplateName());
                        auditResourceDatas.put("catalogueSeq", templateSeq);
                    }
                }
            }

            // auditLog Data 기록.
            auditLog.setResourceData(JsonUtils.toPrettyString(auditResourceDatas));

            /** Set Resource Name */
            if(auditResourceDatas.get("resourceName") != null) {
                auditLog.setResourceName(auditResourceDatas.get("resourceName").toString());
            }
            else {
                aLog.debug("### @CATCH : Skip Write For InstallPackage ( resourceName is null : Not a target for Audit ) : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
                return;
            }

            /** 서비스 연결 정보 기록 */
            if(auditResourceDatas.get("serviceRelation") != null &&
                (auditResourceDatas.get("serviceRelation") instanceof ServiceRelationVO))
            {
                auditLog = this.setServiceRelation(auditLog, (ServiceRelationVO)auditResourceDatas.get("serviceRelation"));
            }

            /** Set Duration */
            auditLog.setDuration(duration);

            /** insert Audit Log */
            int count = auditService.addAuditAccessLog(auditLog);

            aLog.debug(String.format("### status [ %s ] : ExecutionTime[ %ss ]", auditResultCode, duration));
            aLog.debug("### Ended Write For InstallPackage ("+count+") : " + requestUri);
        }
        catch (Exception ex) {
            if(aLog.isDebugEnabled()) {
                aLog.debug("trace log ", ex);
                aLog.debug("### @CATCH : Write For InstallPackage Running Failure : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas));
            }
            else {
                aLog.error("### @CATCH : Write For InstallPackage Running Failure : " + auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_URI) + " : " + JsonUtils.toGson(auditProcessingDatas), ex);
            }
        }
    }

    /**
     * 각 리소스 유형별 Data 구성.
     * @param auditProcessingDatas
     * @param requestDatas
     * @param result
     * @return
     * @throws Exception
     */
    public Map<String, Object> generateEachResourceData(Map<String, Object> auditProcessingDatas, Map<String, Object> requestDatas, ResultVO result) throws Exception {
        Map<String, Object> auditResourceDatas = new HashMap<>();
        try {
            aLog.debug("### Start generateEachResourceData");
            String className = (String) auditProcessingDatas.get(CommonConstants.AUDIT_CLASS_NAME);
            String methodName = (String) auditProcessingDatas.get(CommonConstants.AUDIT_METHOD_NAME);
            Integer userServiceSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SERVICE_SEQ);

            // login 일때는 이 값은 존재하지 않음.
            String userRole = (String) auditProcessingDatas.get(CommonConstants.AUDIT_USER_ROLE);
            Integer userSeq = (Integer) auditProcessingDatas.get(CommonConstants.AUDIT_USER_SEQ);

            String requestBody = null;
            if(requestDatas.get(CommonConstants.AUDIT_REQUEST_DATA_BODY) != null) {
                Object requestBodyObj = requestDatas.get(CommonConstants.AUDIT_REQUEST_DATA_BODY);
                if (requestBodyObj instanceof Map) {
                    Map<String, Object> requestBodyMap = (Map<String, Object>) requestDatas.get(CommonConstants.AUDIT_REQUEST_DATA_BODY);
                    requestBody = JsonUtils.toGson(requestBodyMap);
                }
                else if(requestBodyObj instanceof List) {
                    List<Map<String, Object>> requestBodyList = (List<Map<String, Object>>) requestDatas.get(CommonConstants.AUDIT_REQUEST_DATA_BODY);
                    requestBody = JsonUtils.toGson(requestBodyList);
                }
            }
            else {
                requestBody = JsonUtils.toGson(null);
            }

            Map<String, Object> uriTemplateVariables = (Map<String, Object>) requestDatas.get(CommonConstants.AUDIT_REQUEST_DATA_PATH);
            Map<String, String[]> queryParameters = (Map<String, String[]>) requestDatas.get(CommonConstants.AUDIT_REQUEST_DATA_QUERY);

            /**
             * TODO : 무한정 If else로 할 수 없는 노릇이니 리팩토링 고민해보자.....
             */
            aLog.debug(String.format("### Entered %s : %s ", className, methodName));
            String auditLogCode = className + "." + methodName;

            String resultStatus = "";
            String resultMessage = "";
            Object resultResult = null;
            if (result != null) {
                resultStatus = result.getStatus();
                resultMessage = result.getMessage();
                resultResult = result.getResult();
            }

            // 서비스 연결정보 공통 로깅 처리를 위한 변수 선언.. relationKey, relationKeyName
            Integer relationKey = null;
            String relationKeyName = "";
            switch (className) {

                case "AuthController": { // AuthController에 대한 처리. (시스템 / 사용자계정)
                    if ("AuthController.login".equals(auditLogCode)
                            || "AuthController.loginCocktailAdmin".equals(auditLogCode)
                            || "AuthController.loginPlatformAdmin".equals(auditLogCode)
                            || "AuthController.loginPlatformUser".equals(auditLogCode)
                    ) { // 로그인
                        AuthVO auth = null; // login 요청 데이터
                        UserVO user = null; // login 완료시 로그인한 사용데이터

                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            auth = JsonUtils.fromJackson(requestBody, AuthVO.class);
                        }
                        // response로부터 user 정보 수집
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            user = (UserVO) resultResult;
                        }

                        /**
                         * 로그인 Audit Log 기록..
                         */
                        if (auth != null) {
                            // 어느 Account로 로그인 요청인지 기록.
                            AccountVO account = accountService.getAccountSimpleByCode(auth.getAccountId(), "Y");
                            if(account != null) {
                                ServiceRelationVO serviceRelationVO = new ServiceRelationVO();
                                serviceRelationVO.setAccountSeq(account.getAccountSeq());
                                serviceRelationVO.setAccountName(account.getAccountName());
                                auditResourceDatas.put("serviceRelation", serviceRelationVO);
                            }

                            auditResourceDatas.put("resourceId", auth.getUsername());
                            if(user != null) { /** 로그인 성공 **/
                                if(StringUtils.equalsIgnoreCase(user.getOtpUseYn(), "Y")) {
                                    /** OTP 추가 인증 필요 **/
                                    if(StringUtils.equalsIgnoreCase(auth.getCertified(), "S")) {
                                        // OTP 인증 성공
                                        auditResourceDatas.put("message", "login success with OTP");
                                    }
                                    else {
                                        // Audit Skip
                                        auditResourceDatas.put("isSkip", "Y");
                                        return auditResourceDatas;
                                    }
                                }
                                else {
                                    auditResourceDatas.put("message", "login success");
                                }
                                auditResourceDatas.put("resourceName", user.getUserName());
                                auditResourceDatas.put("userSeq", user.getUserSeq());

                                // login 시에는 header에 사용자 정보가 없기 때문에 audit log 에서 사용자 정보를 설정하기 위해 auditProcessingDatas에 값을 셋팅한다.
                                auditProcessingDatas.put(CommonConstants.AUDIT_USER_ROLE, user.getUserRole());
                                auditProcessingDatas.put(CommonConstants.AUDIT_USER_NAME, user.getUserName());
                                auditProcessingDatas.put(CommonConstants.AUDIT_USER_ID, user.getUserId());
                                auditProcessingDatas.put(CommonConstants.AUDIT_USER_SEQ, user.getUserSeq());
                            }
                            else {
                                /** 로그인 실패 **/
                                auditResourceDatas.put("resourceName", auth.getUsername());
                                auditResourceDatas.put("message", "login failure : " + resultMessage);

                                // login 시에는 header에 사용자 정보가 없기 때문에 audit log 에서 사용자 정보를 설정하기 위해 auditProcessingDatas에 값을 셋팅한다.
                                auditProcessingDatas.put(CommonConstants.AUDIT_USER_ROLE, auth.getRole());
                                auditProcessingDatas.put(CommonConstants.AUDIT_USER_ID, auth.getUsername());

                                // 사용자 정보 확보
                                UserVO checkUser = new UserVO();
                                checkUser.setUserId(auth.getUsername());
                                checkUser.setUserRole(auth.getRole());
                                List<UserVO> authUsers = userService.getUsersForCheck(checkUser);

                                if(CollectionUtils.isNotEmpty(authUsers)){
                                    if(StringUtils.equalsIgnoreCase(UserRole.DEVOPS.getCode(), auth.getRole())){
                                        for(UserVO us : authUsers) {
                                            if(StringUtils.equalsIgnoreCase(us.getUserId(), auth.getUsername())
                                                && StringUtils.equalsIgnoreCase(us.getAccount().getAccountCode(), auth.getAccountId())){
                                                user = this.userService.getUserById(auth.getUsername(), us.getRoles().get(0), auth.getAccountId());
                                                break;
                                            }
                                        }
                                    }
                                }

                                // 사용자 정보 확보가 가능한 경우에만 정보 Update. OTP Failure 포함.
                                if(user != null) {
                                    auditResourceDatas.put("resourceName", user.getUserName());
                                    auditResourceDatas.put("userSeq", user.getUserSeq());

                                    if(StringUtils.equalsIgnoreCase(user.getOtpUseYn(), "Y")) {
                                        if(StringUtils.equalsIgnoreCase(auth.getCertified(), "F")) {
                                            // OTP 인증 실패
                                            auditResourceDatas.put("message", "login failure with OTP : " + resultMessage);
                                        }
                                    }

                                    auditProcessingDatas.put(CommonConstants.AUDIT_USER_NAME, user.getUserName());
                                    auditProcessingDatas.put(CommonConstants.AUDIT_USER_SEQ, user.getUserSeq());
                                }
                            }
                        }
                    }
                    else if ("AuthController.logout".equals(auditLogCode)) { // 로그아웃
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            UserVO response = (UserVO) resultResult;
                            if(response.getAccount() != null) {
                                ServiceRelationVO serviceRelationVO = new ServiceRelationVO();
                                serviceRelationVO.setAccountSeq(response.getAccount().getAccountSeq());
                                serviceRelationVO.setAccountName(response.getAccount().getAccountName());
                                auditResourceDatas.put("serviceRelation", serviceRelationVO);
                            }

                            auditResourceDatas.put("resourceName", response.getUserName());
                            auditResourceDatas.put("resourceId", response.getUserId());
                            auditResourceDatas.put("userSeq", response.getUserSeq());

                            auditResourceDatas.put("message", "logout success.");
                        }
                    }
                }
                case "AccountController": // AccountController에 대한 처리. (시스템 / 사용자계정)
                case "UserController":    // UserController에 대한 처리 (관리자 계정)
                case "InternalOtpController": { // InternalOtpController에 대한 처리 (사용자 전체 OTP 정보)
                    AccountVO account = null;
                    if ("AccountController.addAccount".equals(auditLogCode)) { // Account 생성
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            account = JsonUtils.fromJackson(requestBody, AccountVO.class);
                        }
                        if (account != null) {
                            auditResourceDatas.put("resourceName", account.getAccountName());
                            auditResourceDatas.put("accountType", account.getAccountType().name());
                            auditResourceDatas.put("accountCode", account.getAccountCode());

                            if ("ok".equals(resultStatus) && resultResult != null) {
                                AccountVO response = (AccountVO) resultResult;
                                auditResourceDatas.put("resourceSeq", response.getAccountSeq());

                                ServiceRelationVO serviceRelationVO = new ServiceRelationVO();
                                serviceRelationVO.setAccountSeq(response.getAccountSeq());
                                serviceRelationVO.setAccountName(response.getAccountName());
                                auditResourceDatas.put("serviceRelation", serviceRelationVO);
                            }
                        }
                    }
                    else if ("AccountController.editAccount".equals(auditLogCode)) { // Account 수정
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            account = JsonUtils.fromJackson(requestBody, AccountVO.class);
                        }
                        if (account != null) {
                            Integer accountSeq = getIntegerSeq(uriTemplateVariables, "accountSeq");
                            auditResourceDatas.put("resourceName", account.getAccountName());
                            auditResourceDatas.put("resourceSeq", accountSeq);
                            auditResourceDatas.put("accountType", account.getAccountType().name());
                            auditResourceDatas.put("accountCode", account.getAccountCode());

                            ServiceRelationVO serviceRelationVO = new ServiceRelationVO();
                            serviceRelationVO.setAccountSeq(accountSeq);
                            serviceRelationVO.setAccountName(account.getAccountName());
                            auditResourceDatas.put("serviceRelation", serviceRelationVO);
                        }

                    }
                    else if ("AccountController.removeAccount".equals(auditLogCode) // Account 삭제
                            || "AccountController.getUsersOfAccount".equals(auditLogCode) // Account 사용자 목록 조회
                    ) {
                        Integer accountSeq = getIntegerSeq(uriTemplateVariables, "accountSeq");
                        if (accountSeq != null) {
                            auditResourceDatas.put("resourceSeq", accountSeq);
                            ServiceRelationVO serviceRelationVO = new ServiceRelationVO();
                            serviceRelationVO.setAccountSeq(accountSeq);

                            AccountVO accountSimple = accountService.getAccountSimple(accountSeq);

                            if ( accountSimple == null && "AccountController.removeAccount".equals(auditLogCode) && resultResult != null ) {
                                accountSimple = (AccountVO) resultResult;
                            }

                            if (accountSimple != null){
                                auditResourceDatas.put("resourceName", accountSimple.getAccountName());
                                auditResourceDatas.put("accountType", accountSimple.getAccountType().name());
                                auditResourceDatas.put("accountCode", accountSimple.getAccountCode());
                                serviceRelationVO.setAccountName(accountSimple.getAccountName());
                            }
                            auditResourceDatas.put("serviceRelation", serviceRelationVO);
                        }
                    }
                    else { // 사용자 계정 관리 / 관리자 관리 and other case
                        ServiceRelationVO serviceRelationVO = new ServiceRelationVO();
                        /** API 유형별 로깅 **/
                        if ("AccountController.addUserOfAccount".equals(auditLogCode) ||
                            "AccountController.editUserOfAccount".equals(auditLogCode) ||
                            "UserController.addUser".equals(auditLogCode) ||
                            "UserController.editUser".equals(auditLogCode)) { // 사용자 생성, 사용자 수정, 관리자 생성 관리자 수정
                            if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                                try {
                                    UserVO user = JsonUtils.fromJackson(requestBody, UserVO.class);
                                    auditResourceDatas.put("resourceName", user.getUserName());
                                    auditResourceDatas.put("resourceId", user.getUserId());
                                    auditResourceDatas.put("userRole", user.getUserRole());
                                    if(CollectionUtils.isNotEmpty(user.getShellRoles())) {
                                        List<Map<String, Object>> shellRoles = new ArrayList<>();
                                        for(UserClusterRoleIssueVO role : user.getShellRoles()) {
                                            Map<String, Object> shellRole = new HashMap<>();
                                            shellRole.put("clusterId", Optional.ofNullable(role).map(UserClusterRoleIssueVO::getCluster).map(ClusterVO::getClusterId).orElseGet(() -> null));
                                            if (StringUtils.equalsIgnoreCase(role.getBindingType(), IssueBindingType.CLUSTER.getCode())) {
                                                shellRole.put("Role", role.getIssueRole());
                                            } else {
                                                if (CollectionUtils.isNotEmpty(role.getBindings())) {
                                                    List<Map<String, String>> bindings = new ArrayList<>();
                                                    for (UserClusterRoleIssueBindingVO binding : role.getBindings()) {
                                                        Map<String, String> bindingMap = new HashMap<>();
                                                        bindingMap.put("namespace", binding.getNamespace());
                                                        bindingMap.put("Role", binding.getIssueRole());
                                                        bindings.add(bindingMap);
                                                    }
                                                    shellRole.put("bindings", bindings);
                                                }
                                            }
                                            shellRoles.add(shellRole);
                                        }
                                        auditResourceDatas.put("shellRoles", shellRoles);
                                    }
                                    if(CollectionUtils.isNotEmpty(user.getKubeconfigRoles())) {
                                        List<Map<String, Object>> kubeConfigRoles = new ArrayList<>();
                                        for(UserClusterRoleIssueVO role : user.getKubeconfigRoles()) {
                                            Map<String, Object> kubeConfigRole = new HashMap<>();
                                            kubeConfigRole.put("clusterId", Optional.ofNullable(role).map(UserClusterRoleIssueVO::getCluster).map(ClusterVO::getClusterId).orElseGet(() -> null));
                                            if (StringUtils.equalsIgnoreCase(role.getBindingType(), IssueBindingType.CLUSTER.getCode())) {
                                                kubeConfigRole.put("Role", role.getIssueRole());
                                            } else {
                                                if (CollectionUtils.isNotEmpty(role.getBindings())) {
                                                    List<Map<String, String>> bindings = new ArrayList<>();
                                                    for (UserClusterRoleIssueBindingVO binding : role.getBindings()) {
                                                        Map<String, String> bindingMap = new HashMap<>();
                                                        bindingMap.put("namespace", binding.getNamespace());
                                                        bindingMap.put("Role", binding.getIssueRole());
                                                        bindings.add(bindingMap);
                                                    }
                                                    kubeConfigRole.put("bindings", bindings);
                                                }
                                            }
                                            kubeConfigRoles.add(kubeConfigRole);
                                        }
                                        auditResourceDatas.put("kubeConfigRoles", kubeConfigRoles);
                                    }
                                }
                                catch (Exception ex) {
                                    if(aLog.isDebugEnabled()) {
                                        aLog.debug("### @CATCH : can't get requestBody (UserVO) : " + requestBody);
                                    }
                                    aLog.error("### @CATCH : can't get requestBody (UserVO) : " + requestBody, ex);
                                }
                            }

                            if ("ok".equals(resultStatus) && resultResult != null) {
                                UserVO response = (UserVO) resultResult;
                                if(auditResourceDatas.get("resourceName") == null) {
                                    auditResourceDatas.put("resourceName", response.getUserName());
                                }
                                if(auditResourceDatas.get("resourceId") == null) {
                                    auditResourceDatas.put("resourceId", response.getUserId());
                                }
                                if(auditResourceDatas.get("userRole") == null) {
                                    auditResourceDatas.put("userRole", response.getUserRole());
                                }
                                if(CollectionUtils.isNotEmpty(response.getKubeconfigRoles())) {
                                    List<Map<String,String>> kubeConfigRoles = new ArrayList<>();
                                    for(UserClusterRoleIssueVO role : response.getKubeconfigRoles()) {
                                        Map<String, String> kubeConfigRole = new HashMap<>();
                                        kubeConfigRole.put("clusterId", role.getCluster().getClusterId());
                                        kubeConfigRole.put("Role", role.getIssueRole());
                                        kubeConfigRoles.add(kubeConfigRole);
                                    }
                                    auditResourceDatas.put("kubeConfigRoles", kubeConfigRoles);
                                }
                                if(CollectionUtils.isNotEmpty(response.getShellRoles())) {
                                    List<Map<String,String>> shellRoles = new ArrayList<>();
                                    for(UserClusterRoleIssueVO role : response.getShellRoles()) {
                                        Map<String, String> shellRole = new HashMap<>();
                                        shellRole.put("clusterId", role.getCluster().getClusterId());
                                        shellRole.put("Role", role.getIssueRole());
                                        shellRoles.add(shellRole);
                                    }
                                    auditResourceDatas.put("shellRoles", shellRoles);
                                }

                                result.setResult("Not visible by security policy."); // result에 불필요 정보가 많아 null 처리. (Account 전체 정보가 보여짐..)
                            }
                        }
                        else if ("AccountController.removeUserOfAccount".equals(auditLogCode)
                                || "AccountController.resetPasswordUserOfAccount".equals(auditLogCode)
                                || "AccountController.resetOtpUserOfAccount".equals(auditLogCode)
                                || "AccountController.editUserInactiveStateOfAccount".equals(auditLogCode)
                                || "AccountController.getUserOfAccount".equals(auditLogCode)
                                || "UserController.removeUser".equals(auditLogCode)
                                || "UserController.changePassword".equals(auditLogCode)
                                || "UserController.checkPassword".equals(auditLogCode)
                                || "UserController.resetPassword".equals(auditLogCode)
                                || "UserController.resetOtpUser".equals(auditLogCode)
                                || "UserController.editUserInactiveState".equals(auditLogCode)
                                || "UserController.getUser".equals(auditLogCode)
                                || "InternalOtpController.editUserOtpInfo".equals(auditLogCode)
                        ) { // 사용자 삭제, 사용자 비밀번호 리셋, 관리자 삭제, 관리자 비밀번호 리셋
                            UserVO user = userService.getByUserSeq(this.getIntegerSeq(uriTemplateVariables, "userSeq"));
                            // 사용자가 삭제된 후에는 user == null... => response 데이터 사용함.
                            if(user == null &&
                                "AccountController.removeUserOfAccount".equals(auditLogCode) && "ok".equals(resultStatus) && resultResult != null) {
                                user = (UserVO) resultResult;
                            }

                            if(user != null) {
                                auditResourceDatas.put("resourceName", user.getUserName());
                                auditResourceDatas.put("resourceId", user.getUserId());
                                auditResourceDatas.put("userRole", user.getUserRole());
                                auditResourceDatas.put("userSeq", user.getUserSeq());

                                if (user.getAccount() != null) {
                                    serviceRelationVO.setAccountSeq(user.getAccount().getAccountSeq());
                                    serviceRelationVO.setAccountName(user.getAccount().getAccountName());
                                    auditResourceDatas.put("serviceRelation", serviceRelationVO);
                                }

                                // 인증 체크 제외 메서드이므로 별도 사용자 정보 셋팅
                                if ("InternalOtpController.editUserOtpInfo".equals(auditLogCode)) {
                                    auditResourceDatas.put("userSeq", user.getUserSeq());
                                }
                            }
                        }
                        // 레지스트리
                        else if (
                                "AccountController.addServiceRegistryOfAccount".equals(auditLogCode)
                                        || "AccountController.updateServiceRegistryOfAccount".equals(auditLogCode)
                                        || "AccountController.removeServiceRegistryOfAccount".equals(auditLogCode)
                        ) {
                            relationKey = this.getIntegerSeq(uriTemplateVariables, "accountSeq");
                            relationKeyName = "accountSeq";

                            if (StringUtils.equalsAny(methodName, "addServiceRegistryOfAccount", "updateServiceRegistryOfAccount")) {
                                ServiceRegistryVO reqBodyObj = null;
                                if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                                    reqBodyObj = JsonUtils.fromJackson(requestBody, ServiceRegistryVO.class);
                                }

                                if(reqBodyObj != null && reqBodyObj.getProjectName() != null) {
                                    auditResourceDatas.put("resourceName", reqBodyObj.getProjectName());
                                }
                            } else if ("removeServiceRegistryOfAccount".equals(methodName)) {

                                if ("ok".equals(resultStatus) && resultResult != null) {
                                    ServiceRegistryVO response = (ServiceRegistryVO) resultResult;
                                    auditResourceDatas.put("resourceName", response.getProjectName());
                                }
                            }
                        }
                        // 칵테일 관리자 조회
                        else if (
                                "UserController.getUsers".equals(auditLogCode)
                        ) {
                            auditResourceDatas.put("resourceName", "User List");
                        }

                        /** 공통 **/
                        Integer accountSeq = getIntegerSeq(uriTemplateVariables, "accountSeq");
                        if (accountSeq != null && auditResourceDatas.get("serviceRelation") == null) {
                            serviceRelationVO.setAccountSeq(accountSeq);

                            // 플랫폼 정보 조회시 값이 있을때만 serviceRelation 의 플랫폼 이름을 셋팅한다.
                            AccountVO accountSimple = accountService.getAccountSimple(accountSeq);
                            if(accountSimple != null) {
                                serviceRelationVO.setAccountName(accountSimple.getAccountName());
                            }
                            auditResourceDatas.put("serviceRelation", serviceRelationVO);
                        }
                    }
                    break;
                }
                case "AccountApplicationController": {

                    if ("AccountApplicationController.updateAccountApplicationStatus".equals(auditLogCode)
                            || "AccountApplicationController.detailAccountApplications".equals(auditLogCode)
                    ) {
                        Integer accountApplicationSeq = getIntegerSeq(uriTemplateVariables, "accountApplicationSeq");
                        AccountApplicationVO accountApplication = accountApplicationService.getDetailAccountApplicationByAdmin(accountApplicationSeq);
                        auditResourceDatas.put("resourceName", accountApplication.getUserName());
                        auditResourceDatas.put("resourceId", accountApplication.getAccountCode());

                    } else if ("AccountApplicationController.deleteAccountApplications".equals(auditLogCode)) {
                        AccountApplicationVO accountApplication = null;
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            accountApplication = (AccountApplicationVO) resultResult;
                            auditResourceDatas.put("resourceName", accountApplication.getUserName());
                            auditResourceDatas.put("resourceId", accountApplication.getAccountCode());
                        }
                    }

                }
                case "CubeController": { // CubeController에 대한 처리. (노드)
                    if ("patchNode".equals(methodName)) { // 노드 설정
                        relationKey = getIntegerSeq(uriTemplateVariables, "clusterSeq");

                        ClusterVO clusterVO = clusterService.getCluster(relationKey);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("nodeName"));
                        auditResourceDatas.put("clusterName", clusterVO.getClusterName());
                        auditResourceDatas.put("clusterId", clusterVO.getClusterId());
                        auditResourceDatas.put("clusterSeq", clusterVO.getClusterSeq());
                    }
                    relationKeyName = "clusterSeq"; // for 공통 로깅..
                    break;
                }
                case "ClusterController": { // ClusterController에 대한 처리. (클러스터 / 애드온)
                    /** API 유형별 로깅 **/
                    if ("addCluster".equals(methodName) ||
                        "updateCluster".equals(methodName)) { // 클러스터 생성 / 수정
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            ClusterAddVO cluster = JsonUtils.fromJackson(requestBody, ClusterAddVO.class);
                            if (cluster != null) {
                                auditResourceDatas.put("resourceName", cluster.getClusterName());
                                auditResourceDatas.put("resourceId", cluster.getClusterId());
                            }
                        }

                        if ("ok".equals(resultStatus) && resultResult != null) {
                            ClusterAddVO response = (ClusterAddVO) resultResult;
                            auditResourceDatas.put("resourceSeq", response.getClusterSeq());
                            relationKey = response.getClusterSeq();
                        }
                    }
                    else if ("removeCluster".equals(methodName)) { // 클러스터 삭제
                        relationKey = getIntegerSeq(uriTemplateVariables, "clusterSeq");
                        auditResourceDatas.put("resourceSeq", relationKey);

                        ClusterVO clusterVO = clusterService.getCluster(relationKey);
                        if(clusterVO != null) {
                            auditResourceDatas.put("resourceName", clusterVO.getClusterName());
                            auditResourceDatas.put("resourceId", clusterVO.getClusterId());
                        }
                    }
                    else if ("updateAddonInCluster".equals(methodName) ||
                        "deleteAddonInCluster".equals(methodName) ||
                        "redeployAddonInCluster".equals(methodName) ||
                        "rollbackAddonInCluster".equals(methodName)) { // 애드온 수정, 삭제, 재배포, 롤백
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "clusterSeq");
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("addonName"));
                    }
                    else if ("addonInstallInCluster".equals(methodName) ||
                        "upgradeAddonInCluster".equals(methodName)) { // 애드온 생성, 수정
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "clusterSeq");
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("addonName"));
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            AddonInstallVO addonInstall = JsonUtils.fromJackson(requestBody, AddonInstallVO.class);
                            if (addonInstall != null) {
                                /** Monitoring Agent는 자동생성된 이름을 사용... **/
                                if (StringUtils.equalsIgnoreCase(addonInstall.getName(), "monitoring-agent")) {
                                    String releaseName = String.format("%s-%s-%d", cocktailAddonProperties.getMonitoringAgentConfigmapPrefix(), ResourceUtil.getResourcePrefix(), relationKey);
                                    auditResourceDatas.put("resourceName", releaseName);
                                }
                                auditResourceDatas.put("repository", addonInstall.getRepo());
                                auditResourceDatas.put("chart", addonInstall.getName() + "-" + addonInstall.getVersion());
                            }
                        }

                    }
                    relationKeyName = "clusterSeq"; // for 공통 로깅..
                    break;
                }
                case "ClusterVolumeController": { // ClusterVolumeController에 대한 처리. (스토리지)
                    /** API 유형별 로깅 : R4.0 신규 API : 기존 API는 제거함.. (addClusterVolume, updateClusterVolume, removeClusterVolume **/
                    if ("addStorageVolume".equals(methodName) ||
                        "udpateStorageVolume".equals(methodName)) { // 스토리지 생성 / 수정
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            ClusterVolumeVO clusterVolume = JsonUtils.fromJackson(requestBody, ClusterVolumeVO.class);
                            if (clusterVolume != null) {
                                this.setClusterVolumeMap(auditResourceDatas, clusterVolume);
                                relationKey = clusterVolume.getClusterSeq();
                            }
                        }
                    }
                    else if ("udpateStroageClassByYaml".equals(methodName)) { // StorageClass Yaml 수정
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("storageClassName"));
                        relationKey = getIntegerSeq(uriTemplateVariables, "clusterSeq");
                    }
                    else if ("removeStorageVolume".equals(methodName)) { // 스토리지 삭제
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("storageName"));
                        relationKey = getIntegerSeq(uriTemplateVariables, "clusterSeq");
                    }
                    relationKeyName = "clusterSeq";  // for 공통 로깅..
                    break;
                }
                case "PodSecurityPolicyController": { // PodSecurityPolicyController에 대한 처리. (PodSecurityPolicy)
                    /** API 유형별 로깅 : R4.3 신규 API : 기존 API는 제거함.. (addPodSecurityPolicy, updatePodSecurityPolicy, deletePodSecurityPolicy **/
                    if ("addPodSecurityPolicy".equals(methodName)) { // PodSecurityPolicy 생성
                        relationKey = getIntegerSeq(uriTemplateVariables, "clusterSeq");
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            PodSecurityPolicyGuiVO pspGui = JsonUtils.fromJackson(requestBody, PodSecurityPolicyGuiVO.class);
                            if (pspGui != null) {
                                auditResourceDatas.put("resourceName", pspGui.getName());
                            }
                        }
                    }
                    else if ("addDefaultPodSecurityPolicy".equals(methodName)) { // 기본 PodSecurityPolicy 생
                        relationKey = getIntegerSeq(uriTemplateVariables, "clusterSeq");
                    }
                    else if (StringUtils.equalsAny(methodName, "updatePodSecurityPolicy", "deletePodSecurityPolicy")) { // PodSecurityPolicy 수정/삭제
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("pspName"));
                        relationKey = getIntegerSeq(uriTemplateVariables, "clusterSeq");
                    }
                    /** 공통 로깅 **/
                    if ("ok".equals(resultStatus)
                            && resultResult != null
                            && MapUtils.getString(auditResourceDatas, "resourceName", null) == null
                    ) {
                        K8sPodSecurityPolicyVO response = (K8sPodSecurityPolicyVO) resultResult;
                        auditResourceDatas.put("resourceName", response.getName());
                    }
                    relationKeyName = "clusterSeq";  // for 공통 로깅..
                    break;
                }
                case "ResourceQuotaController": { // ResourceQuotaController에 대한 처리. (ResourceQuota)
                    /** API 유형별 로깅 **/
                    // 생성
                    if ("addResourceQuota".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            ResourceQuotaIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ResourceQuotaIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    ResourceQuotaGuiVO gui = JsonUtils.fromJackson(requestBody, ResourceQuotaGuiVO.class);
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    ResourceQuotaYamlVO yaml = JsonUtils.fromJackson(requestBody, ResourceQuotaYamlVO.class);
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }
                    }
                    else if ("addDefaultResourceQuota".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                    }
                    // 수정 / 삭제
                    else if ("updateResourceQuota".equals(methodName) ||
                            "deleteResourceQuota".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("resourceQuotaName"));
                    }

                    /** 공통 로깅 **/
                    if ("ok".equals(resultStatus)
                            && resultResult != null
                            && MapUtils.getString(auditResourceDatas, "resourceName", null) == null
                    ) {
                        K8sResourceQuotaVO response = (K8sResourceQuotaVO) resultResult;
                        auditResourceDatas.put("resourceName", response.getName());
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "LimitRangeController": { // LimitRangeController에 대한 처리. (LimitRange)
                    /** API 유형별 로깅 **/
                    // 생성
                    if ("addLimitRange".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            LimitRangeIntegrateVO integrate = JsonUtils.fromJackson(requestBody, LimitRangeIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    LimitRangeGuiVO gui = JsonUtils.fromJackson(requestBody, LimitRangeGuiVO.class);
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    LimitRangeYamlVO yaml = JsonUtils.fromJackson(requestBody, LimitRangeYamlVO.class);
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }
                    }
                    else if ("addDefaultLimitRange".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                    }
                    // 수정 / 삭제
                    else if ("updateLimitRange".equals(methodName) ||
                            "deleteLimitRange".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("limitRangeName"));
                    }

                    /** 공통 로깅 **/
                    if ("ok".equals(resultStatus)
                            && resultResult != null
                            && MapUtils.getString(auditResourceDatas, "resourceName", null) == null
                    ) {
                        K8sLimitRangeVO response = (K8sLimitRangeVO) resultResult;
                        auditResourceDatas.put("resourceName", response.getName());
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "NetworkPolicyController": { // NetworkPolicyController에 대한 처리. (NetworkPolicy)
                    /** API 유형별 로깅 **/
                    // 생성
                    if ("addNetworkPolicy".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            NetworkPolicyIntegrateVO integrate = JsonUtils.fromJackson(requestBody, NetworkPolicyIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    NetworkPolicyGuiVO gui = JsonUtils.fromJackson(requestBody, NetworkPolicyGuiVO.class);
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    NetworkPolicyYamlVO yaml = JsonUtils.fromJackson(requestBody, NetworkPolicyYamlVO.class);
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }
                    }
                    else if ("addDefaultNetworkPolicy".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                    }
                    // 수정 / 삭제
                    else if ("updateNetworkPolicy".equals(methodName) ||
                            "deleteNetworkPolicy".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("networkPolicyName"));
                    }

                    /** 공통 로깅 **/
                    if ("ok".equals(resultStatus)
                            && resultResult != null
                            && MapUtils.getString(auditResourceDatas, "resourceName", null) == null
                    ) {
                        K8sNetworkPolicyVO response = (K8sNetworkPolicyVO) resultResult;
                        auditResourceDatas.put("resourceName", response.getName());
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "RBACResourceController": { // RBACResourceController에 대한 처리. (ClusterRole, ClusterRoleBinding, Role, RoleBinding)
                    /** API 유형별 로깅 **/
                    relationKey = getIntegerSeq(uriTemplateVariables, "clusterSeq");

                    // ClusterRole 생성
                    if ("addClusterRole".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            ClusterRoleIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ClusterRoleIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    ClusterRoleGuiVO gui = JsonUtils.fromJackson(requestBody, ClusterRoleGuiVO.class);
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    ClusterRoleYamlVO yaml = JsonUtils.fromJackson(requestBody, ClusterRoleYamlVO.class);
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }

                        if ("ok".equals(resultStatus)
                                && resultResult != null
                                && MapUtils.getString(auditResourceDatas, "resourceName", null) == null
                        ) {
                            K8sClusterRoleVO response = (K8sClusterRoleVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getName());
                        }
                    }
                    // ClusterRole 수정 / 삭제
                    else if ("updateClusterRole".equals(methodName) ||
                            "deleteClusterRole".equals(methodName)) {
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("clusterRoleName"));
                    }
                    // ClusterRoleBinding 생성
                    else if ("addClusterRoleBinding".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            ClusterRoleBindingIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ClusterRoleBindingIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    ClusterRoleBindingGuiVO gui = JsonUtils.fromJackson(requestBody, ClusterRoleBindingGuiVO.class);
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    ClusterRoleBindingYamlVO yaml = JsonUtils.fromJackson(requestBody, ClusterRoleBindingYamlVO.class);
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }

                        if ("ok".equals(resultStatus)
                                && resultResult != null
                                && MapUtils.getString(auditResourceDatas, "resourceName", null) == null
                        ) {
                            K8sClusterRoleBindingVO response = (K8sClusterRoleBindingVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getName());
                        }
                    }
                    // ClusterRoleBinding 수정 / 삭제
                    else if ("updateClusterRoleBinding".equals(methodName) ||
                            "deleteClusterRoleBinding".equals(methodName)) {
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("clusterRoleBindingName"));
                    }
                    // Role 생성
                    else if ("addRole".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            RoleIntegrateVO integrate = JsonUtils.fromJackson(requestBody, RoleIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    RoleGuiVO gui = JsonUtils.fromJackson(requestBody, RoleGuiVO.class);
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    RoleYamlVO yaml = JsonUtils.fromJackson(requestBody, RoleYamlVO.class);
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }

                        if ("ok".equals(resultStatus)
                                && resultResult != null
                                && MapUtils.getString(auditResourceDatas, "resourceName", null) == null
                        ) {
                            K8sRoleVO response = (K8sRoleVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getName());
                        }
                    }
                    // Role 수정 / 삭제
                    else if ("updateRole".equals(methodName) ||
                            "deleteRole".equals(methodName)) {
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("roleName"));
                    }
                    // RoleBinding 생성
                    else if ("addRoleBinding".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            RoleBindingIntegrateVO integrate = JsonUtils.fromJackson(requestBody, RoleBindingIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    RoleBindingGuiVO gui = JsonUtils.fromJackson(requestBody, RoleBindingGuiVO.class);
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    RoleBindingYamlVO yaml = JsonUtils.fromJackson(requestBody, RoleBindingYamlVO.class);
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }

                        if ("ok".equals(resultStatus)
                                && resultResult != null
                                && MapUtils.getString(auditResourceDatas, "resourceName", null) == null
                        ) {
                            K8sRoleBindingVO response = (K8sRoleBindingVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getName());
                        }
                    }
                    // RoleBinding 수정 / 삭제
                    else if ("updateRoleBinding".equals(methodName) ||
                            "deleteRoleBinding".equals(methodName)) {
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("roleBindingName"));
                    }

                    /** 공통 로깅 **/
                    relationKeyName = "clusterSeq"; // for 공통 로깅..
                    break;
                }
                case "PersistentVolumeController": { // PersistentVolumeController에 대한 처리. (볼륨)
                    /** API 유형별 로깅 **/
                    if ("addPersistentVolumeClaimeV2".equals(methodName) ||
                        "updatePersistentVolumeClaimeV2".equals(methodName) ||
                        "addPersistentVolumeClaime".equals(methodName) ||
                        "updatePersistentVolumeClaime".equals(methodName)) { // 클러스터볼륨 생성 / 수정
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            PersistentVolumeClaimIntegrateVO integrate = JsonUtils.fromJackson(requestBody, PersistentVolumeClaimIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    PersistentVolumeClaimGuiVO pvcGui = JsonUtils.fromJackson(requestBody, PersistentVolumeClaimGuiVO.class);
                                    if (pvcGui != null) {
                                        auditResourceDatas.put("resourceName", pvcGui.getName());
                                        auditResourceDatas.put("capacity", pvcGui.getCapacity().toString() + "G");
                                        auditResourceDatas.put("accessMode", pvcGui.getAccessMode());
                                        auditResourceDatas.put("persistentVolumeType", pvcGui.getPersistentVolumeType());
                                    }
                                    break;
                                case YAML:
                                    PersistentVolumeClaimYamlVO pvcYaml = JsonUtils.fromJackson(requestBody, PersistentVolumeClaimYamlVO.class);
                                    if (pvcYaml != null) {
                                        auditResourceDatas.put("resourceName", pvcYaml.getName());
                                        auditResourceDatas.put("yaml", pvcYaml.getYaml());
                                    }
                                    break;
                            }
                        }
                    }
                    else if ("deletePersistentVolumeClaimeV2".equals(methodName) ||
                        "deletePersistentVolumeClaime".equals(methodName)) { // 클러스터 삭제
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("persistentVolumeClaimName"));
                    }
                    relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                    relationKeyName = "servicemapSeq";  // for 공통 로깅..
                    break;
                }
                case "ServiceController": { // ServiceController에 대한 처리. (워크스페이스)
                    /** API 유형별 로깅 **/
                    if ("addService".equals(methodName)) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            ServiceAddVO response = (ServiceAddVO) resultResult;
                            relationKey = response.getServiceSeq();
                            relationKeyName = "serviceSeq";
                        }
                        else {
                            // Service(워크스페이스) 생성 실패시 Audit Logging...
                            ServiceAddVO service = null;
                            if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                                service = JsonUtils.fromJackson(requestBody, ServiceAddVO.class);
                            }

                            if(service != null) {
                                auditResourceDatas.put("resourceName", service.getServiceName());
                                ServiceRelationVO serviceRelation = this.getServiceRelation("accountSeq", service.getAccountSeq());
                                auditResourceDatas.put("serviceRelation", serviceRelation);
                            }
                        }
                    }
                    else if ("updateService".equals(methodName)) {
                        relationKey = getIntegerSeq(uriTemplateVariables, "serviceSeq");
                        relationKeyName = "serviceSeq";
                    }
                    else if ("removeService".equals(methodName)) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            ServiceVO response = (ServiceVO) resultResult;
                            auditResourceDatas.put("resourceSeq", response.getServiceSeq());
                            auditResourceDatas.put("resourceName", response.getServiceName());

                            ServiceRelationVO serviceRelationVO = new ServiceRelationVO();
                            serviceRelationVO.setAccountSeq(response.getAccountSeq());
                            serviceRelationVO.setServiceSeq(response.getServiceSeq());
                            serviceRelationVO.setServiceName(response.getServiceName());
                            auditResourceDatas.put("serviceRelation", serviceRelationVO);
                        }
                    }
                    else if ("updateUsersOfService".equals(methodName)) { // 워크스페이스 사용자 설정
                        relationKey = getIntegerSeq(uriTemplateVariables, "serviceSeq");
                        relationKeyName = "serviceSeq";

                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("[")) {
                            List<ServiceUserVO> reqBodyObj = null; // User List
                            if (StringUtils.isNotBlank(requestBody)) {
                                reqBodyObj = JsonUtils.fromJackson(requestBody, new TypeReference<List<ServiceUserVO>>(){});
                            }

                            if(CollectionUtils.isNotEmpty(reqBodyObj)) {
                                for(ServiceUserVO su : reqBodyObj) {
                                    UserVO user = userService.getUser(su.getUserSeq());
                                    su.setUserId(user.getUserId());
                                    su.setUserName(user.getUserName());
                                }
                                auditResourceDatas.put("userList", reqBodyObj);
                            }
                        }
                        ServiceRelationVO sr = this.getServiceRelation(relationKeyName, relationKey);
                        auditResourceDatas.put("resourceName", sr.getServiceName());
                    }
                    else if ("updateClustersOfService".equals(methodName)) { // 워크스페이스의 클러스터 할당
                        relationKey = getIntegerSeq(uriTemplateVariables, "serviceSeq");
                        relationKeyName = "serviceSeq";

                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("[")) {
                            List<Integer> reqBodyObj = null; // clusterSeq List
                            if (StringUtils.isNotBlank(requestBody)) {
                                reqBodyObj = JsonUtils.fromJackson(requestBody, new TypeReference<List<Integer>>(){});
                            }

                            if(CollectionUtils.isNotEmpty(reqBodyObj)) {
                                List<Map<String, Object>> clusterList = new ArrayList<>();
                                for(Integer clusterSeq : reqBodyObj) {
                                    ClusterVO cluster = clusterService.getCluster(clusterSeq);

                                    Map<String, Object> resultCluster = new HashMap<>();
                                    resultCluster.put("clusterSeq", cluster.getClusterSeq());
                                    resultCluster.put("clusterId", cluster.getClusterId());
                                    resultCluster.put("clusterName", cluster.getClusterName());
                                    clusterList.add(resultCluster);
                                }

                                auditResourceDatas.put("clusterList", clusterList);
                            }
                            else {
                                auditResourceDatas.put("clusterList", "EMPTY");
                            }
                        }
                        ServiceRelationVO sr = this.getServiceRelation(relationKeyName, relationKey);
                        auditResourceDatas.put("resourceName", sr.getServiceName());
                    }
                    else if ("updateBuildserversOfService".equals(methodName)) { // 워크스페이스의 빌드서버 할당
                        relationKey = getIntegerSeq(uriTemplateVariables, "serviceSeq");
                        relationKeyName = "serviceSeq";

                        ServiceRelationVO sr = this.getServiceRelation(relationKeyName, relationKey);
                        auditResourceDatas.put("resourceName", sr.getServiceName());
                    }
                    else if ("updateServicemapsOfService".equals(methodName)) { // 워크스페이스의 서비스맵 할당
                        relationKey = getIntegerSeq(uriTemplateVariables, "serviceSeq");
                        relationKeyName = "serviceSeq";

                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("[")) {
                            List<ServicemapGroupMappingAddVO> reqBodyObj = null; // clusterSeq List
                            if (StringUtils.isNotBlank(requestBody)) {
                                reqBodyObj = JsonUtils.fromJackson(requestBody, new TypeReference<List<ServicemapGroupMappingAddVO>>(){});
                            }

                            if(CollectionUtils.isNotEmpty(reqBodyObj)) {
                                List<Map<String, Object>> serviceMapList = new ArrayList<>();
                                for(ServicemapGroupMappingAddVO soft : reqBodyObj) {
                                    ServicemapVO servicemap = servicemapService.getServicemap(soft.getServicemapSeq(), relationKey);

                                    if( servicemap != null ) {
                                        Map<String, Object> resultServiceMap = new HashMap<>();
                                        resultServiceMap.put("serviceMapSeq", servicemap.getServicemapSeq());
                                        resultServiceMap.put("serviceMapName", servicemap.getServicemapName());

                                        if (CollectionUtils.isNotEmpty(servicemap.getServicemapMappings())) {
                                            ServicemapMappingVO servicemapMapping = servicemap.getServicemapMappings().get(0);
                                            resultServiceMap.put("serviceMapGroupSeq", servicemapMapping.getServicemapGroup().getServicemapGroupSeq());
                                            resultServiceMap.put("serviceMapGroupName", servicemapMapping.getServicemapGroup().getServicemapGroupName());
                                        }
                                        serviceMapList.add(resultServiceMap);
                                    }
                                }

                                auditResourceDatas.put("serviceMapList", serviceMapList);
                            }
                            else {
                                auditResourceDatas.put("serviceMapList", "EMPTY");
                            }
                        }
                        ServiceRelationVO sr = this.getServiceRelation(relationKeyName, relationKey);
                        auditResourceDatas.put("resourceName", sr.getServiceName());
                    }
                    else if ("addServicemapOfService".equals(methodName)) { // 워크스페이스에 서비스맵 할당
                        relationKey = getIntegerSeq(uriTemplateVariables, "serviceSeq");
                        relationKeyName = "serviceSeq";

                        ServicemapVO servicemap = null;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            servicemap = JsonUtils.fromJackson(requestBody, ServicemapVO.class);
                        }
                        if(servicemap != null) {
                            List<Map<String, Object>> addedServiceMap = new ArrayList<>();

                            Map<String, Object> resultServiceMap = new HashMap<>();
                            resultServiceMap.put("serviceMapName", servicemap.getServicemapName());
                            resultServiceMap.put("namespace", servicemap.getNamespaceName());
                            addedServiceMap.add(resultServiceMap);

                            auditResourceDatas.put("addedServiceMap", addedServiceMap);
                        }
                        ServiceRelationVO sr = this.getServiceRelation(relationKeyName, relationKey);
                        auditResourceDatas.put("resourceName", sr.getServiceName());
                    }


                    break;
                }
                case "ServicemapGroupController": { // ServicemapGroupController에 대한 처리. (서비스맵그룹)
                    /** API 유형별 로깅 **/
                    if ("addServicemapGroup".equals(methodName)) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            ServicemapGroupVO response = (ServicemapGroupVO) resultResult;
                            relationKey = response.getServicemapGroupSeq();
                        }
                        else {
                            // ServicemapGroup 생성 실패시 Audit Logging...
                            ServicemapGroupVO servicemapGroup = null;
                            if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                                servicemapGroup = JsonUtils.fromJackson(requestBody, ServicemapGroupVO.class);
                            }

                            if(servicemapGroup != null) {
                                auditResourceDatas.put("resourceName", servicemapGroup.getServicemapGroupName());
                                ServiceRelationVO serviceRelation = this.getServiceRelation("serviceSeq", servicemapGroup.getServiceSeq());
                                auditResourceDatas.put("serviceRelation", serviceRelation);
                            }
                        }
                    }
                    else if ("updateServicemapGroup".equals(methodName) || "removeServicemapGroup".equals(methodName)) {
                        relationKey = getIntegerSeq(uriTemplateVariables, "ServicemapGroupSeq");
                    }
                    relationKeyName = "servicemapGroupSeq"; // for 공통 로깅..
                    break;
                }
                case "ServicemapController": { // ServicemapController에 대한 처리. (서비스맵)
                    /** API 유형별 로깅 **/
                    if ("addServicemapV2".equals(methodName)) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            ServicemapVO response = (ServicemapVO) resultResult;
                            auditResourceDatas.put("namespaceName", response.getNamespaceName());
                            relationKey = response.getServicemapSeq();
                        }
                        else {
                            // Servicemap 생성 실패시 Audit Logging...
                            ServicemapAddVO servicemap = null;
                            if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                                servicemap = JsonUtils.fromJackson(requestBody, ServicemapAddVO.class);
                            }

                            if(servicemap != null) {
                                auditResourceDatas.put("resourceName", servicemap.getServicemapName());
                                ServiceRelationVO serviceRelation = this.getServiceRelation("servicemapGroupSeq", servicemap.getServicemapGroupSeq());
                                auditResourceDatas.put("serviceRelation", serviceRelation);
                            }
                        }
                    }
                    else if ("renameServicemap".equals(methodName) || "removeServicemap".equals(methodName) || "updateServicemap".equals(methodName)) {
                        relationKey = getIntegerSeq(uriTemplateVariables, "servicemapSeq");
                    }

                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "WorkloadGroupController": { // WorkloadGroupController에 대한 처리. (그룹)
                    /** API 유형별 로깅 **/
                    if ("addWorkloadGroup".equals(methodName)) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            WorkloadGroupVO response = (WorkloadGroupVO) resultResult;
                            relationKey = response.getWorkloadGroupSeq();
                        }
                    }
                    else if ("updateWorkloadGroup".equals(methodName) || "removeWorkloadGroup".equals(methodName)) {
                        relationKey = getIntegerSeq(uriTemplateVariables, "workloadGroupSeq");
                    }
                    relationKeyName = "workloadGroupSeq"; // for 공통 로깅..
                    break;
                }
                case "ServerController": { // ServerController에 대한 처리. (워크로드)
                    /** API 유형별 로깅 **/
                    // 생성 / 수정 (AS-IS)
                    if ("addServerV2".equals(methodName) ||
                        "deployV2".equals(methodName)) {
//                        ServerGuiVO server = null;
//                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
//                            server = JsonUtils.fromJackson(requestBody, ServerGuiVO.class);
//                            relationKey = Optional.ofNullable(server).map(ServerGuiVO::getComponent).map(ComponentVO::getGroupSeq).orElseGet(() ->null);
//                        }
                        aLog.debug("%s.%s - AS-IS(skip)", "ServerController", methodName);
                    }
                    // 중지 / 시작 / 재시작 / 삭제 (AS-IS)
                    else if ("terminateServerV2".equals(methodName) ||
                        "redeployV2".equals(methodName) ||
                        "restartV2".equals(methodName) ||
                        "removeServerV2".equals(methodName)) {
//                        relationKey = groupService.getGroupWithComponentSeq(this.getIntegerSeq(uriTemplateVariables, "componentSeq")).getGroupSeq();
                        aLog.debug("%s.%s - AS-IS(skip)", "ServerController", methodName);
                    }
                    // 생성 / 수정 / 내용수정 (3.5.0 워크로드 관리)
                    else if ("addWorkload".equals(methodName) ||
                        "updateWorkload".equals(methodName) ||
                        "editWorkloadManifest".equals(methodName)) {
                        ServerIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ServerIntegrateVO.class);
                        DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                        switch (deployType) {
                            case GUI:
                                ServerGuiVO serverGui = JsonUtils.fromJackson(requestBody, ServerGuiVO.class);
                                relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                                auditResourceDatas.put("resourceName", uriTemplateVariables.get("workloadName"));
                                break;
                            case YAML:
                                ServerYamlVO serverYaml = JsonUtils.fromJackson(requestBody, ServerYamlVO.class);
                                relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                                auditResourceDatas.put("resourceName", uriTemplateVariables.get("workloadName"));
                                if(serverYaml != null) {
                                    auditResourceDatas.put("yaml", serverYaml.getYaml());
                                }
                                break;
                        }
                    }
                    // 재생성 / 종료 / 제거 (3.5.0 워크로드 관리)
                    else if ("redeployWorkload".equals(methodName) ||
                        "terminateWorkload".equals(methodName) ||
                        "restartWorkload".equals(methodName) ||
                        "removeWorkload".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("workloadName"));
                    }

                    /** 공통 로깅 **/
                    if (!"controlWorkloadsInServicemap".equals(methodName)) { // ServerController.controlWorkloadsInServicemap Logging에서 제외.
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            if (resultResult instanceof ServerDetailVO) {
                                ServerDetailVO response = (ServerDetailVO) resultResult;

                                auditResourceDatas.put("resourceName", response.getComponent().getComponentName());
                                auditResourceDatas.put("workloadType", response.getServer().getWorkloadType());
                            }
                            else if (resultResult instanceof ServerIntegrateVO) {
                                ServerIntegrateVO integrate = (ServerIntegrateVO) resultResult;
                                DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                                switch (deployType) {
                                    case GUI:
                                        ServerGuiVO serverGui = (ServerGuiVO) resultResult;
                                        auditResourceDatas.put("resourceName", serverGui.getComponent().getComponentName());
                                        auditResourceDatas.put("workloadType", serverGui.getServer().getWorkloadType());
                                        break;
                                    case YAML:
                                        ServerYamlVO serverYaml = (ServerYamlVO) resultResult;
                                        auditResourceDatas.put("resourceName", serverYaml.getWorkloadName());
                                        auditResourceDatas.put("workloadType", serverYaml.getWorkloadType());
                                        auditResourceDatas.put("yaml", serverYaml.getYaml());
                                        break;
                                }
                            }
                        }
                    }
                    if(StringUtils.isBlank(relationKeyName)) {
                        relationKeyName = "servicemapSeq"; // for 공통 로깅.
                    }
                    break;
                }
                case "PackageController": { // PackageController에 대한 처리. (패키지)
                    /** API 유형별 로깅 **/
                    // Install / Upgrade Package
                    if ("installPackage".equals(methodName) ||
                        "upgradePackage".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        HelmInstallRequestVO pack = null;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            pack = JsonUtils.fromJackson(requestBody, HelmInstallRequestVO.class);
                            auditResourceDatas.put("resourceName", pack.getReleaseName());
                            auditResourceDatas.put("repository", pack.getRepo());
                            auditResourceDatas.put("chart", pack.getChartName() + "-" + pack.getVersion());
                        }
                    }
                    // Rollback / Uninstall Package
                    else if ("rollbackPackage".equals(methodName) ||
                        "unInstallPackage".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("releaseName"));
                    }

                    /** 공통 로깅 **/
                    if ("ok".equals(resultStatus) && resultResult != null) {
                        if (resultResult instanceof HelmReleaseBaseVO) {
                            HelmReleaseBaseVO response = (HelmReleaseBaseVO) resultResult;

                            auditResourceDatas.put("resourceName", response.getName());
                            auditResourceDatas.put("repository", response.getRepo());
                            auditResourceDatas.put("chart", response.getChartNameAndVersion());
                            auditResourceDatas.put("revision", response.getRevision());
                        }
                    }
                    if(StringUtils.isBlank(relationKeyName)) {
                        relationKeyName = "servicemapSeq"; // for 공통 로깅.
                    }
                    break;
                }
                case "ServiceSpecController": { // ServiceSpecController에 대한 처리. (서비스 노출)
                    /** API 유형별 로깅 **/
                    // 생성
                    if ("addServiceSpec".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);

                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            ServiceSpecIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ServiceSpecIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    ServiceSpecGuiVO serviceSpecGui = JsonUtils.fromJackson(requestBody, ServiceSpecGuiVO.class);
                                    auditResourceDatas.put("resourceName", serviceSpecGui.getName());
                                    break;
                                case YAML:
                                    ServiceSpecYamlVO serviceSpecYaml = JsonUtils.fromJackson(requestBody, ServiceSpecYamlVO.class);
                                    auditResourceDatas.put("resourceName", serviceSpecYaml.getName());
                                    auditResourceDatas.put("yaml", serviceSpecYaml.getYaml());
                                    break;
                            }
                        }
                    }
                    // 수정 / 삭제
                    else if ("updateService".equals(methodName) ||
                        "deleteService".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);

                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("serviceName"));
                    }

                    /** 공통 로깅 **/
                    if ("ok".equals(resultStatus) && resultResult != null) {
                        K8sServiceVO response = (K8sServiceVO) resultResult;
                        auditResourceDatas.put("labelSelector", response.getDetail().getLabelSelector());
                        auditResourceDatas.put("servicePorts", response.getServicePorts());
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "IngressSpecController": { // IngressSpecController에 대한 처리. (인그레스)
                    /** API 유형별 로깅 **/
                    // 생성
                    if ("addIngressSpec".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);

                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            IngressSpecIntegrateVO integrate = JsonUtils.fromJackson(requestBody, IngressSpecIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    IngressSpecGuiVO ingressSpecGui = JsonUtils.fromJackson(requestBody, IngressSpecGuiVO.class);
                                    auditResourceDatas.put("resourceName", ingressSpecGui.getName());
                                    break;
                                case YAML:
                                    IngressSpecYamlVO ingressSpecYaml = JsonUtils.fromJackson(requestBody, IngressSpecYamlVO.class);
                                    auditResourceDatas.put("resourceName", ingressSpecYaml.getName());
                                    auditResourceDatas.put("yaml", ingressSpecYaml.getYaml());
                                    break;
                            }
                        }
                    }
                    // 수정 / 삭제
                    else if ("updateIngress".equals(methodName) ||
                        "deleteIngress".equals(methodName)) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);

                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("ingressName"));
                    }

                    /** 공통 **/
                    if ("ok".equals(resultStatus) && resultResult != null) {
                        K8sIngressVO response = (K8sIngressVO) resultResult;
                        auditResourceDatas.put("IngressRules", response.getIngressSpec().getIngressRules());
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "ConfigMapController": { // ConfigMapController에 대한 처리. (컨피그맵)
                    /** API 유형별 로깅 **/
                    // 생성 / 수정
                    if ("addConfigMapV2".equals(methodName) ||
                        "updateConfigMapV2".equals(methodName) ||
                        "addConfigMapWithCluster".equals(methodName) ||
                        "updateConfigMapWithCluster".equals(methodName))
                    {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            try {
                                ConfigMapIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ConfigMapIntegrateVO.class);
                                DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                                switch (deployType) {
                                    case GUI:
                                        ConfigMapGuiVO configMapGui = JsonUtils.fromJackson(requestBody, ConfigMapGuiVO.class);
                                        auditResourceDatas.put("resourceName", configMapGui.getName());
                                        break;
                                    case YAML:
                                        ConfigMapYamlVO configMapYaml = JsonUtils.fromJackson(requestBody, ConfigMapYamlVO.class);
                                        auditResourceDatas.put("resourceName", configMapYaml.getName());
                                        auditResourceDatas.put("yaml", configMapYaml.getYaml());
                                        break;
                                }
                            }
                            catch (Exception ex) {
                                if (aLog.isDebugEnabled()) {
                                    aLog.error("requestBody to ConfigMapGuiVO Error...");
                                }
                            }
                        }
                    }
                    // 삭제
                    else if ("deleteConfigMapV2".equals(methodName) ||
                        "deleteConfigMapWithCluster".equals(methodName))
                    {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("configMapName"));
                    }

                    /** 공통 **/
                    if ("ok".equals(resultStatus) && resultResult != null) {
                        try {
                            ConfigMapGuiVO response = (ConfigMapGuiVO) resultResult;
                            auditResourceDatas.put("data", response.getData());
                            if(StringUtils.isBlank(Optional.ofNullable(auditResourceDatas.get("resourceName")).orElseGet(() ->"").toString())) {
                                auditResourceDatas.put("resourceName", response.getName());
                            }
                        }
                        catch (Exception ex) {
                            if(aLog.isDebugEnabled()) {
                                aLog.error("skip configmap data... result parsing error", ex);
                            }
                        }
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "SecretController": { // SecretController에 대한 처리. (시크릿)
                    /** API 유형별 로깅 **/
                    // 생성 / 수정
                    if ("addSecret".equals(methodName) ||
                        "updateSecret".equals(methodName) ||
                        "addSecretWithCluster".equals(methodName) ||
                        "updateSecretWithCluster".equals(methodName))
                    {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            try {
                                SecretIntegrateVO integrate = JsonUtils.fromJackson(requestBody, SecretIntegrateVO.class);
                                DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                                switch (deployType) {
                                    case GUI:
                                        SecretGuiVO secretGui = JsonUtils.fromJackson(requestBody, SecretGuiVO.class);
                                        auditResourceDatas.put("resourceName", secretGui.getName());
                                        auditResourceDatas.put("secretType", secretGui.getType().name());
                                        break;
                                    case YAML:
                                        SecretYamlVO secretYaml = JsonUtils.fromJackson(requestBody, SecretYamlVO.class);
                                        auditResourceDatas.put("resourceName", secretYaml.getName());
                                        auditResourceDatas.put("yaml", secretYaml.getYaml());
                                        break;
                                }
                            }
                            catch (Exception ex) {
                                if(aLog.isDebugEnabled()) {
                                    aLog.error("requestBody to SecretGuiVO Error...", ex);
                                }
                            }
                        }
                    }
                    // 삭제
                    else if ("deleteSecret".equals(methodName) ||
                        "deleteSecretWithCluster".equals(methodName))
                    {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("secretName"));
                    }

                    /** 공통 **/
                    if ("ok".equals(resultStatus) && resultResult != null) {
                        try {
                            SecretGuiVO response = (SecretGuiVO) resultResult;
                            auditResourceDatas.put("data", response.getData());
                            if (StringUtils.isBlank(Optional.ofNullable(auditResourceDatas.get("resourceName")).orElseGet(() ->"").toString())) {
                                auditResourceDatas.put("resourceName", response.getName());
                            }
                        }
                        catch (Exception ex) {
                            if (aLog.isDebugEnabled()) {
                                aLog.error("skip secret data... result parsing error", ex);
                            }
                        }
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "NetAttachDefController": { // NetAttachDefController에 대한 처리. (네트워크)
                    /** API 유형별 로깅 **/
                    // 생성 / 수정
                    if ("addNetAttachDef".equals(methodName) ||
                        "updateNetAttachDef".equals(methodName) ||
                        "addNetAttachDefWithCluster".equals(methodName) ||
                        "updateNetAttachDefWithCluster".equals(methodName))
                    {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            try {
                                K8sCRDNetAttachDefIntegrateVO integrate = JsonUtils.fromJackson(requestBody, K8sCRDNetAttachDefIntegrateVO.class);
                                DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                                switch (deployType) {
                                    case GUI:
                                        K8sCRDNetAttachDefGuiVO netGui = JsonUtils.fromJackson(requestBody, K8sCRDNetAttachDefGuiVO.class);
                                        auditResourceDatas.put("resourceName", netGui.getName());
                                        auditResourceDatas.put("type", netGui.getType());
                                        break;
                                    case YAML:
                                        K8sCRDNetAttachDefYamlVO netYaml = JsonUtils.fromJackson(requestBody, K8sCRDNetAttachDefYamlVO.class);
                                        auditResourceDatas.put("resourceName", netYaml.getName());
                                        auditResourceDatas.put("type", netYaml.getType());
                                        auditResourceDatas.put("yaml", netYaml.getYaml());
                                        break;
                                }
                            }
                            catch (Exception ex) {
                                if(aLog.isDebugEnabled()) {
                                    aLog.error("requestBody to K8sCRDNetAttachDefGuiVO Error...", ex);
                                }
                            }
                        }
                    }
                    // 삭제
                    else if ("deleteNetAttachDef".equals(methodName) ||
                        "deleteNetAttachDefWithCluster".equals(methodName))
                    {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("netAttachDefName"));
                    }

                    /** 공통 **/
                    if ("ok".equals(resultStatus) && resultResult != null) {
                        try {
                            K8sCRDNetAttachDefGuiVO response = (K8sCRDNetAttachDefGuiVO) resultResult;
                            auditResourceDatas.put("config", response.getConfig());
                            if (StringUtils.isBlank(Optional.ofNullable(auditResourceDatas.get("resourceName")).orElseGet(() ->"").toString())) {
                                auditResourceDatas.put("resourceName", response.getName());
                            }
                        }
                        catch (Exception ex) {
                            if (aLog.isDebugEnabled()) {
                                aLog.error("skip K8sCRDNetAttachDef config data... result parsing error", ex);
                            }
                        }
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "PipelineFlowController": {
                    // PipelineFlowController에 대한 처리. (파이프라인)
                    // 파이프라인 플로우 생성 / 수정
                    // 사용안함.. 워크로드 생성하면서 처리해주어야 함
                    if ("addPipeline".equals(methodName) ||
                        "editPipeline".equals(methodName)) {
                        PipelineWorkloadVO pipeline = null;
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            pipeline = (PipelineWorkloadVO)resultResult;
                        }
                        else if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            pipeline = JsonUtils.fromJackson(requestBody, PipelineWorkloadVO.class);
                        }
                        if (pipeline != null && StringUtils.isNotBlank(pipeline.getWorkloadName())) {
                            relationKey = servicemapService.getServicemapSeqByNamespace(pipeline.getClusterSeq(), pipeline.getNamespaceName());
                            auditResourceDatas.put("resourceName", pipeline.getWorkloadName());
                            auditResourceDatas.put("workloadType", pipeline.getWorkloadType());
                            auditResourceDatas.put("workloadState", pipeline.getWorkloadStateCode());
                            auditResourceDatas.put("numberOfPipelineContainers", Optional.ofNullable(pipeline.getPipelineContainers()).map(List::size).orElseGet(() ->0));
                        }
                    }
                    // 파이프라인 실행 / 재실행 / 이미지변경 / 롤백
                    if ("runPipelineContainerByBuildRun".equals(methodName) ||
                        "runPipelineContainerByBuildRunModify".equals(methodName) ||
                        "runChangeImage".equals(methodName) ||
                        "rollbackBuildImage".equals(methodName)) {
                        Integer pipelineContainerSeq = this.getIntegerSeq(uriTemplateVariables, "pipelineContainerSeq");
                        PipelineContainerVO pipelineContainer = pipelineFlowService.getPipelineContainer(pipelineContainerSeq);
                        if(pipelineContainer != null) {
                            PipelineWorkloadVO pipeline = pipelineFlowService.getPipelineWorkload(pipelineContainer.getPipelineWorkloadSeq());
                            auditResourceDatas.put("containerName", pipelineContainer.getContainerName());
                            relationKey = servicemapService.getServicemapSeqByNamespace(pipeline.getClusterSeq(), pipeline.getNamespaceName());
                            if(pipeline != null) {
                                auditResourceDatas.put("resourceName", pipeline.getWorkloadName());
                                auditResourceDatas.put("workloadType", pipeline.getWorkloadType());
                                auditResourceDatas.put("workloadState", pipeline.getWorkloadStateCode());
                                auditResourceDatas.put("deployImage", pipelineContainer.getDeployImageUrl());
                                auditResourceDatas.put("numberOfPipelineContainers", Optional.ofNullable(pipeline.getPipelineContainers()).map(List::size).orElseGet(() ->0));
                            }
                        }
                        if (pipelineContainer == null && "ok".equals(resultStatus) && resultResult != null) {
                            PipelineContainerVO response = (PipelineContainerVO) resultResult;
                            if(response != null) {
                                PipelineWorkloadVO pipeline = pipelineFlowService.getPipelineWorkload(pipelineContainer.getPipelineWorkloadSeq());
                                relationKey = servicemapService.getServicemapSeqByNamespace(pipeline.getClusterSeq(), pipeline.getNamespaceName());
                                auditResourceDatas.put("resourceName", pipeline.getWorkloadName());
                                auditResourceDatas.put("containerName", pipelineContainer.getContainerName());
                                auditResourceDatas.put("workloadType", pipeline.getWorkloadType());
                                auditResourceDatas.put("workloadState", pipeline.getWorkloadStateCode());
                                auditResourceDatas.put("workloadState", pipeline.getWorkloadStateCode());
                                auditResourceDatas.put("deployImage", pipelineContainer.getDeployImageUrl());
                                auditResourceDatas.put("numberOfPipelineContainers", Optional.ofNullable(pipeline.getPipelineContainers()).map(List::size).orElseGet(() ->0));
                            }
                        }
                    }
                    relationKeyName = "servicemapSeq"; // for 공통 로깅..
                    break;
                }
                case "BuildJobController": // BuildJobController에 대한 처리. (빌드)
                case "BuildTaskController": { // BuildTaskController에 대한 처리. (빌드)
                    break;
                }
                case "PipelineBuildController": { // PipelineBuildController에 대한 처리. (빌드)
                    if ("addBuild".equals(methodName) ||
                        "editBuild".equals(methodName)) {
                        BuildAddVO buildAddVo;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            buildAddVo = JsonUtils.fromJackson(requestBody, BuildAddVO.class);
                            if (buildAddVo != null) {
                                auditResourceDatas.put("resourceName", buildAddVo.getImageName());
                                auditResourceDatas.put("resourceSeq", buildAddVo.getBuildSeq());
                                auditResourceDatas.put("buildImages", buildAddVo.getImageName());
                            }
                        }
                        if ("ok".equals(resultStatus) && resultResult != null && "addBuild".equals(methodName)) {
                            BuildVO response = (BuildVO) resultResult;
                            auditResourceDatas.put("resourceSeq", response.getBuildSeq()); // 생성시 seq는 response에서 받아와야 함..
                        }
                    }
                    // 빌드 삭제
                    else if ("removeBuild".equals(methodName)) {
                        Integer buildSeq = this.getIntegerSeq(uriTemplateVariables, "buildSeq");
                        BuildVO build = pipelineBuildService.getBuildWithoutUseYn(buildSeq); // 삭제된 빌드이므로 useYn을 사용하지 않음..

                        auditResourceDatas.put("resourceName", build.getImageName());
                        auditResourceDatas.put("resourceSeq", buildSeq);
                    }

                    AccountVO account = accountService.getAccountByUser(userSeq, userRole);
                    relationKey = account.getAccountSeq();
                    relationKeyName = "accountSeq"; // for 공통 로깅..
                    break;
                }
                case "PipelineBuildRunController": {
                    // PipelineBuildRunController에 대한 처리. (빌드)
                    // 빌드 실행 / 빌드 이력에서 빌드 실행
                    if ("runBuildByBuild".equals(methodName) ||
                        "runBuildByBuildRun".equals(methodName)) {
                        Integer buildSeq = this.getIntegerSeq(uriTemplateVariables, "buildSeq");
                        BuildVO build = pipelineBuildService.getBuildWithoutUseYn(buildSeq);
                        if (build != null) {
                            auditResourceDatas.put("resourceName", build.getImageName());
                            auditResourceDatas.put("resourceSeq", build.getBuildSeq());
                            auditResourceDatas.put("buildImages", build.getImageName());
                        }

                        Integer buildRunSeq = this.getIntegerSeq(uriTemplateVariables, "buildRunSeq");
                        if(buildRunSeq != null) {
                            BuildRunVO buildRun = pipelineBuildRunService.getBuildRun(buildRunSeq, null, false);
                            if(Optional.ofNullable(buildRun).map(BuildRunVO::getBuildNo).orElseGet(() ->0) > 0) {
                                auditResourceDatas.put("sourceBuildNo", buildRun.getBuildNo());
                            }
                        }
                    }
                    // 빌드 실행 취소 / 빌드 실행 내역 삭제
//                    if ("cancelBuildRun".equals(methodName)) {
                    if ("cancelBuildRun".equals(methodName) ||
                        "removeBuildRun".equals(methodName)) {
                        Integer buildRunSeq = this.getIntegerSeq(uriTemplateVariables, "buildRunSeq");
                        if(buildRunSeq != null) {
                            BuildRunVO buildRun = pipelineBuildRunService.getBuildRun(buildRunSeq, null, false);
                            if(buildRun == null) { // buildRun을 찾을 수 없으면 Response 데이터를 사용하도록 함..
                                if ("ok".equals(resultStatus) && resultResult != null && "removeBuildRun".equals(methodName)) {
                                    buildRun = (BuildRunVO) resultResult;
                                }
                            }
                            if(buildRun != null) {
                                auditResourceDatas.put("resourceName", buildRun.getImageName());
                                auditResourceDatas.put("resourceSeq", buildRun.getBuildSeq());
                                auditResourceDatas.put("buildImages", buildRun.getImageName());
                                auditResourceDatas.put("buildNo", buildRun.getBuildNo());
                            }
                        }
                    }
                    // 빌드 태그 삭제
                    if ("removeImageTag".equals(methodName)) {
                        Integer buildSeq = this.getIntegerSeq(uriTemplateVariables, "buildSeq");
                        if(buildSeq != null) {
                            BuildVO build = pipelineBuildService.getBuildWithoutUseYn(buildSeq);
                            auditResourceDatas.put("resourceName", build.getImageName());
                            auditResourceDatas.put("resourceSeq", build.getBuildSeq());
                            auditResourceDatas.put("buildImages", build.getImageName());
                        }
                        String[] buildRunSeqs = queryParameters.get("buildRunSeqs");
                        if(ArrayUtils.isNotEmpty(buildRunSeqs)) {
                            Set<String> tagList = new HashSet<>();
                            for(String buildRunSeqString : buildRunSeqs) {
                                Integer buildRunSeq = this.getIntegerSeq(buildRunSeqString);
                                if(buildRunSeq != null) {
                                    BuildRunVO buildRun = pipelineBuildRunService.getBuildRun(buildRunSeq, null, false);
                                    tagList.add(buildRun.getTagName());
                                }
                            }
                            auditResourceDatas.put("tagList", tagList);
                        }
                    }


                    AccountVO account = accountService.getAccountByUser(userSeq, userRole);
                    relationKey = account.getAccountSeq();
                    relationKeyName = "accountSeq"; // for 공통 로깅..
                    break;
                }
                case "TemplateController": {
                    // TemplateController에 대한 처리. (카탈로그)
                    /** API 유형별 로깅 **/
                    if ("addTemplate".equals(methodName)) { // 생성
                        TemplateAddVO template;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            template = JsonUtils.fromJackson(requestBody, TemplateAddVO.class);
                            if (template != null) {
                                if (template.getTemplateSeq() > 0) { // 기존 카탈로그에 추가일때 templateName 조회 필요.. => isNew를 사용할 수 없음.. fromGson 처리중 무조건 false로 변환됨.. => 모델에 파라미터명이 isNew로 정의되어 있는데 이는 Lombok에서만 변환 가능..
                                    TemplateDetailVO templateDetail = templateService.getTemplateDetail(template.getTemplateSeq(), null, false);
                                    auditResourceDatas.put("resourceName", templateDetail.getTemplateName() + ":" + template.getVersion());
                                }
                                if (StringUtils.isBlank((String) auditResourceDatas.get("resourceName"))) {
                                    auditResourceDatas.put("resourceName", template.getTemplateName() + ":" + template.getVersion());
                                }
                                auditResourceDatas.put("resourceSeq", template.getTemplateSeq());
                                auditResourceDatas.put("catalogueVersion", template.getVersion());

                                // Service Relation 설정...
                                relationKeyName = "serviceSeq";
                                relationKey = template.getServiceSeq();
                            }
                            if ("ok".equals(resultStatus) && resultResult != null) {
                                TemplateAddVO response = (TemplateAddVO) resultResult;
                                if (StringUtils.isBlank((String) auditResourceDatas.get("resourceName"))) {
                                    if (template != null && StringUtils.isNotBlank(template.getVersion())) {
                                        auditResourceDatas.put("resourceName", response.getTemplateName() + ":" + template.getVersion());
                                    }
                                    auditResourceDatas.put("resourceName", response.getTemplateName());
                                }
                                if (StringUtils.isBlank((String) auditResourceDatas.get("catalogueVersion"))) {
                                    auditResourceDatas.put("catalogueVersion", template.getVersion());
                                }
                                if (auditResourceDatas.get("resourceSeq") == null) {
                                    auditResourceDatas.put("resourceSeq", response.getTemplateSeq());
                                }
                            }

                        }
                    }
                    else if ("editTemplate".equals(methodName) ||
                        "removeTemplate".equals(methodName)) { // 삭제
                        Integer templateSeq = this.getIntegerSeq(uriTemplateVariables, "templateSeq");
                        if(Optional.ofNullable(userServiceSeq).orElseGet(() ->0) > 0) {
                            relationKey = userServiceSeq; // 로그인된 serviceSeq가 있으면 해당 serviceSeq를 사용
                        }
                        else {
                            // serviceSeq가 존재하지 않으면 관리화면에서 진입 => 템플릿에 설정된 ServiceSeq를 조회하여 사용
                            TemplateVO tpl = templateService.getTemplate(templateSeq, null);
                            relationKey = tpl.getServiceSeq();
                        }
                        relationKeyName = "serviceSeq"; // for 공통 로깅..
                        Integer templateVersionSeq = this.getIntegerSeq(uriTemplateVariables, "templateVersionSeq");
                        if (Optional.ofNullable(templateVersionSeq).orElseGet(() ->0) == 0) {
                            templateVersionSeq = this.getIntegerSeq(queryParameters.get("templateVersionSeq"), 0);
                        }
                        if (Optional.ofNullable(templateSeq).orElseGet(() ->0) != 0 && Optional.ofNullable(templateVersionSeq).orElseGet(() ->0) != 0) {
                            TemplateDetailVO templateDetail = templateService.getTemplateDetail(templateSeq, templateVersionSeq, false);
                            if (templateDetail != null) {
                                auditResourceDatas.put("resourceName", templateDetail.getTemplateName() + ":" + templateDetail.getVersion());
                                auditResourceDatas.put("resourceSeq", templateDetail.getTemplateSeq());
                                auditResourceDatas.put("catalogueVersion", templateDetail.getVersion());
                            }
                        }
                    }
                    else if ("launchTemplateV2".equals(methodName) ||
                            "deployTemplate".equals(methodName)) { // 배포
                        Integer templateSeq = this.getIntegerSeq(uriTemplateVariables, "templateSeq");
                        TemplateDetailVO templateDetail = templateService.getTemplateDetail(templateSeq, null, false);
                        auditResourceDatas.put("resourceName", templateDetail.getTemplateName());
                        auditResourceDatas.put("resourceSeq", templateDetail.getTemplateSeq());
                        // auditResourceDatas.put("catalogueVersion",???????????) // Template 배포시에 TemplateVersion 정보가 없어서 알 수 없음..
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            ServicemapVO response = (ServicemapVO) resultResult;
                            auditResourceDatas.put("deployTargetServiceMap", response.getServicemapName());
                            relationKey = response.getServicemapSeq();
                            relationKeyName = "servicemapSeq"; // 카탈로그 배포시에는 서비스 릴레이션 계층을 배포대상 서비스맵까지 표시.
                        }
                    }
                    break;
                }
                case "PlController": { // PlController에 대한 처리. (파이프라인)
                    /** API 유형별 로깅 **/
                    if ("addPipelineByServicemap".equals(methodName)) { // 파이프라인 생성
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "servicemapSeq");
                        relationKeyName = "servicemapSeq";

                        PlMasterVO plMaster = null;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            plMaster = JsonUtils.fromJackson(requestBody, PlMasterVO.class);
                        }

                        if(plMaster != null) {
                            auditResourceDatas.put("resourceName", plMaster.getName());
                        }
                    }
                    else if ("addPipelineByNamespace".equals(methodName)) { // 파이프라인 생성
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "clusterSeq");
                        relationKeyName = "clusterSeq";

                        PlMasterVO plMaster = null;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            plMaster = JsonUtils.fromJackson(requestBody, PlMasterVO.class);
                        }

                        if(plMaster != null) {
                            auditResourceDatas.put("resourceName", plMaster.getName());
                        }
                    }
                    else if ("editPlMasterName".equals(methodName)) {
                        auditResourceDatas.put("resourceName", MapUtils.getString(uriTemplateVariables, "name", null));
                    }
                    else if ("deletePlMaster".equals(methodName)) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            PlMasterVO response = (PlMasterVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getName());
                        }
                    }
                    else if ("editWorkloadDeployResource".equals(methodName)) {
                        ServerIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ServerIntegrateVO.class);
                        DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                        switch (deployType) {
                            case GUI:
//                                ServerGuiVO serverGui = JsonUtils.fromJackson(requestBody, ServerGuiVO.class);
                                auditResourceDatas.put("resourceName", uriTemplateVariables.get("workloadName"));
                                break;
                            case YAML:
                                ServerYamlVO serverYaml = JsonUtils.fromJackson(requestBody, ServerYamlVO.class);
                                auditResourceDatas.put("resourceName", uriTemplateVariables.get("workloadName"));
                                if(serverYaml != null) {
                                    auditResourceDatas.put("yaml", serverYaml.getYaml());
                                }
                                break;
                        }
                    }
                    else if ("editConfigMapDeployResource".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            try {
                                ConfigMapIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ConfigMapIntegrateVO.class);
                                DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                                switch (deployType) {
                                    case GUI:
                                        ConfigMapGuiVO configMapGui = JsonUtils.fromJackson(requestBody, ConfigMapGuiVO.class);
                                        auditResourceDatas.put("resourceName", configMapGui.getName());
                                        break;
                                    case YAML:
                                        ConfigMapYamlVO configMapYaml = JsonUtils.fromJackson(requestBody, ConfigMapYamlVO.class);
                                        auditResourceDatas.put("resourceName", configMapYaml.getName());
                                        auditResourceDatas.put("yaml", configMapYaml.getYaml());
                                        break;
                                }
                            }
                            catch (Exception ex) {
                                if (aLog.isDebugEnabled()) {
                                    aLog.error("requestBody to ConfigMapIntegrateVO Error...", ex);
                                }
                            }
                        }
                    }
                    else if ("editSecretDeployResource".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            try {
                                SecretIntegrateVO integrate = JsonUtils.fromJackson(requestBody, SecretIntegrateVO.class);
                                DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                                switch (deployType) {
                                    case GUI:
                                        SecretGuiVO secretGui = JsonUtils.fromJackson(requestBody, SecretGuiVO.class);
                                        auditResourceDatas.put("resourceName", secretGui.getName());
                                        auditResourceDatas.put("secretType", secretGui.getType().name());
                                        break;
                                    case YAML:
                                        SecretYamlVO secretYaml = JsonUtils.fromJackson(requestBody, SecretYamlVO.class);
                                        auditResourceDatas.put("resourceName", secretYaml.getName());
                                        auditResourceDatas.put("yaml", secretYaml.getYaml());
                                        break;
                                }
                            }
                            catch (Exception ex) {
                                if(aLog.isDebugEnabled()) {
                                    aLog.error("requestBody to SecretIntegrateVO Error...", ex);
                                }
                            }
                        }
                    }
                    else if ("editServiceDeployResource".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            ServiceSpecIntegrateVO integrate = JsonUtils.fromJackson(requestBody, ServiceSpecIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    ServiceSpecGuiVO serviceSpecGui = JsonUtils.fromJackson(requestBody, ServiceSpecGuiVO.class);
                                    auditResourceDatas.put("resourceName", serviceSpecGui.getName());
                                    break;
                                case YAML:
                                    ServiceSpecYamlVO serviceSpecYaml = JsonUtils.fromJackson(requestBody, ServiceSpecYamlVO.class);
                                    auditResourceDatas.put("resourceName", serviceSpecYaml.getName());
                                    auditResourceDatas.put("yaml", serviceSpecYaml.getYaml());
                                    break;
                            }
                        }
                    }
                    else if ("editIngressDeployResource".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            IngressSpecIntegrateVO integrate = JsonUtils.fromJackson(requestBody, IngressSpecIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    IngressSpecGuiVO ingressSpecGui = JsonUtils.fromJackson(requestBody, IngressSpecGuiVO.class);
                                    auditResourceDatas.put("resourceName", ingressSpecGui.getName());
                                    break;
                                case YAML:
                                    IngressSpecYamlVO ingressSpecYaml = JsonUtils.fromJackson(requestBody, IngressSpecYamlVO.class);
                                    auditResourceDatas.put("resourceName", ingressSpecYaml.getName());
                                    auditResourceDatas.put("yaml", ingressSpecYaml.getYaml());
                                    break;
                            }
                        }
                    }
                    else if ("deletePlResDeploy".equals(methodName)
                            || "editPlResDeployWorkloadRunOrder".equals(methodName)
                            || "editPlResDeployRunYn".equals(methodName)
                    ) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            PlResDeployVO response = (PlResDeployVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getResName());
                        }
                    }
                    else if ("addPlResDeploy".equals(methodName)) {
                        PlResDeployVO reqBodyObj = null;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, PlResDeployVO.class);
                        }

                        if(reqBodyObj != null) {
                            auditResourceDatas.put("resourceName", reqBodyObj.getResName());
                        }
                    }
                    else if ("addPlResDeploies".equals(methodName)) {
                        List<PlResDeployVO> reqBodyObj = null;
                        if (StringUtils.isNotBlank(requestBody)) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, new TypeReference<List<PlResDeployVO>>(){});
                        }

                        if(reqBodyObj != null) {
                            auditResourceDatas.put("resourceName", reqBodyObj.stream().map(PlResDeployVO::getResName).collect(Collectors.joining(",")));
                        }
                    }
                    else if ("addPlResBuilds".equals(methodName)) {
                        List<Map<String,Object>> reqBodyObj = null;
                        if (StringUtils.isNotBlank(requestBody)) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, new TypeReference<List<Map<String,Object>>>(){});
                        }

                        if(reqBodyObj != null) {
                            auditResourceDatas.put("resourceName", reqBodyObj.stream().map(v -> JsonUtils.toGson(v)).collect(Collectors.joining(",")));
                        }
                    }
                    else if ("editPlResBuild".equals(methodName)) {
                        PlResBuildVO reqBodyObj = null;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, PlResBuildVO.class);
                        }

                        if(reqBodyObj != null && reqBodyObj.getBuildConfig() != null) {
                            auditResourceDatas.put("resourceName", reqBodyObj.getBuildConfig().getBuildName());
                        }
                    }
                    else if ("deletePlResBuild".equals(methodName)
                                || "editPlResBuildRunOrder".equals(methodName)
                                || "editPlResBuildRunYn".equals(methodName)
                    ) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            auditResourceDatas.put("resourceName", result.getResult());
                        }
                    }
                    else if ("runPlMaster".equals(methodName)
                            || "runBuildPlMaster".equals(methodName)
                            || "rollbackPlMaster".equals(methodName)
                            || "cancelPlMaster".equals(methodName)
                            || "cancelBuildPlMaster".equals(methodName)
                    ) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            PlRunVO response = (PlRunVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getPlName());
                        }
                    }
                    else if ("createVersion".equals(methodName)
                            || "deleteVersion".equals(methodName)
                    ) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            PlMasterVO response = (PlMasterVO) resultResult;
                            auditResourceDatas.put("resourceName", String.format("%s(%s)", response.getName(), response.getVer()));
                        }
                    }

                    if (!("addPipelineByServicemap".equals(methodName)
                            || "addPipelineByNamespace".equals(methodName)
                    )) {
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "plSeq");
                        relationKeyName = "plSeq";
                    }
                    break;
                }
                case "ExternalRegistryController": {
                    if ("addExternalRegistry".equals(methodName)) {
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "accountSeq");
                        relationKeyName = "accountSeq";

                        ExternalRegistryVO reqBodyObj = null;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, ExternalRegistryVO.class);
                        }

                        if(reqBodyObj != null && reqBodyObj.getName() != null) {
                            auditResourceDatas.put("resourceName", reqBodyObj.getName());
                        }
                    } else if ("editExternalRegistry".equals(methodName)) {
                        ExternalRegistryVO reqBodyObj = null;
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, ExternalRegistryVO.class);
                        }

                        if(reqBodyObj != null && reqBodyObj.getName() != null) {
                            auditResourceDatas.put("resourceName", reqBodyObj.getName());
                        }

                        if(reqBodyObj != null && reqBodyObj.getAccountSeq() != null) {
                            relationKey = reqBodyObj.getAccountSeq();
                            relationKeyName = "accountSeq";
                        } else {
                            if ("ok".equals(resultStatus) && resultResult != null) {
                                ExternalRegistryVO response = (ExternalRegistryVO) resultResult;
                                relationKey = response.getAccountSeq();
                                relationKeyName = "accountSeq";
                            }
                        }
                    } else if ("deleteExternalRegistry".equals(methodName)) {

                        if ("ok".equals(resultStatus) && resultResult != null) {
                            ExternalRegistryVO response = (ExternalRegistryVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getName());
                            relationKey = response.getAccountSeq();
                            relationKeyName = "accountSeq";
                        }
                    }
                    break;
                }
                case "OpenapiController": {
                    if ("issueApiToken".equals(methodName)) {
                        ApiTokenIssueAddVO reqBodyObj = null;
                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, ApiTokenIssueAddVO.class);
                        }
                        if (reqBodyObj != null) {
                            relationKey = reqBodyObj.getAccountSeq();
                            relationKeyName = "accountSeq";

                            if(reqBodyObj.getApiTokenName() != null) {
                                auditResourceDatas.put("resourceName", reqBodyObj.getApiTokenName());
                            }
                        }
                    } else if ("editApiTokenIssue".equals(methodName)) {
                        ApiTokenIssueEditVO reqBodyObj = null;
                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, ApiTokenIssueEditVO.class);
                        }
                        if (reqBodyObj != null) {
                            relationKey = reqBodyObj.getAccountSeq();
                            relationKeyName = "accountSeq";

                            if(reqBodyObj.getApiTokenName() != null) {
                                auditResourceDatas.put("resourceName", reqBodyObj.getApiTokenName());
                            }
                        }
                    } else if (
                            "revokeApiTokenIssue".equals(methodName)
                                    || "getApiToken".equals(methodName)

                    ) {
                        relationKey = this.getIntegerSeq(queryParameters.get("accountSeq"), 0);
                        relationKeyName = "accountSeq";

                        if ("ok".equals(resultStatus) && resultResult != null) {
                            ApiTokenVO response = (ApiTokenVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getApiTokenName());
                        }
                    }
                    break;
                }
                case "CertManagerController": { // CertManagerController에 대한 처리. (Cert-Manager)
                    /** API 유형별 로깅 **/
                    // 사설인증서 발급자 생성
                    if ("addCertPrivateIssuer".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            K8sCRDIssuerIntegrateVO integrate = JsonUtils.fromJackson(requestBody, K8sCRDIssuerIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    K8sCRDIssuerGuiVO gui = JsonUtils.fromJackson(requestBody, K8sCRDIssuerGuiVO.class);
                                    relationKey = gui.getClusterSeq();
                                    relationKeyName = "clusterSeq";
                                    auditResourceDatas.put("scope", gui.getScope());
                                    if (StringUtils.equals(CertIssuerScope.NAMESPACED.getCode(), gui.getScope())) {
                                        auditResourceDatas.put("namespace", gui.getNamespace());
                                    }
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    K8sCRDIssuerYamlVO yaml = JsonUtils.fromJackson(requestBody, K8sCRDIssuerYamlVO.class);
                                    relationKey = yaml.getClusterSeq();
                                    relationKeyName = "clusterSeq";
                                    auditResourceDatas.put("scope", yaml.getScope());
                                    if (StringUtils.equals(CertIssuerScope.NAMESPACED.getCode(), yaml.getScope())) {
                                        auditResourceDatas.put("namespace", yaml.getNamespace());
                                    }
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }
                    }
                    // 사설인증서 발급자 수정 / 삭제
                    else if (
                            StringUtils.equalsAny(methodName
                                    , "editCertPrivateClusterIssuer"
                                    , "deleteCertPrivateClusterIssuer"
                            )
                    ) {
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "clusterSeq");
                        relationKeyName = "clusterSeq";
                        auditResourceDatas.put("scope", CertIssuerScope.CLUSTER.getValue());
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("issuerName"));
                    }
                    else if (
                            StringUtils.equalsAny(methodName
                                    , "editCertPrivateIssuer"
                                    , "deleteCertPrivateIssuer"
                            )
                    ) {
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "clusterSeq");
                        relationKeyName = "clusterSeq";
                        auditResourceDatas.put("scope", CertIssuerScope.NAMESPACED.getValue());
                        auditResourceDatas.put("namespaceName", MapUtils.getString(uriTemplateVariables, "namespaceName", ""));
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("issuerName"));
                    }
                    // 사설인증서 인증서 생성
                    else if ("addCertPrivateCertificate".equals(methodName)) {
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            K8sCRDCertificateIntegrateVO integrate = JsonUtils.fromJackson(requestBody, K8sCRDCertificateIntegrateVO.class);
                            DeployType deployType = DeployType.valueOf(integrate.getDeployType());
                            switch (deployType) {
                                case GUI:
                                    K8sCRDCertificateGuiVO gui = JsonUtils.fromJackson(requestBody, K8sCRDCertificateGuiVO.class);
                                    relationKey = gui.getClusterSeq();
                                    relationKeyName = "clusterSeq";
                                    auditResourceDatas.put("namespace", gui.getNamespace());
                                    auditResourceDatas.put("resourceName", gui.getName());
                                    break;
                                case YAML:
                                    K8sCRDCertificateYamlVO yaml = JsonUtils.fromJackson(requestBody, K8sCRDCertificateYamlVO.class);
                                    relationKey = yaml.getClusterSeq();
                                    relationKeyName = "clusterSeq";
                                    auditResourceDatas.put("namespace", yaml.getNamespace());
                                    auditResourceDatas.put("resourceName", yaml.getName());
                                    auditResourceDatas.put("yaml", yaml.getYaml());
                                    break;
                            }
                        }
                    }
                    // 사설인증서 인증서 수정 / 삭제
                    else if (
                            StringUtils.equalsAny(methodName
                                    , "editCertPrivateCertificate"
                                    , "deleteCertPrivateCertificate"
                            )
                    ) {
                        relationKey = this.getIntegerSeq(uriTemplateVariables, "clusterSeq");
                        relationKeyName = "clusterSeq";
                        auditResourceDatas.put("namespaceName", MapUtils.getString(uriTemplateVariables, "namespaceName", ""));
                        auditResourceDatas.put("resourceName", uriTemplateVariables.get("certificateName"));
                    }
                    // 공인인증서 인증서 생성
                    else if ("addCertPublicCertificate".equals(methodName)) {
                        PublicCertificateAddVO reqBodyObj = null;
                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, PublicCertificateAddVO.class);
                        }
                        if (reqBodyObj != null) {
                            relationKey = reqBodyObj.getAccountSeq();
                            relationKeyName = "accountSeq";

                            if(reqBodyObj.getPublicCertificateName() != null) {
                                auditResourceDatas.put("resourceName", reqBodyObj.getPublicCertificateName());
                            }
                        }
                    }
                    // 공인인증서 인증서 수정
                    else if (StringUtils.equals(methodName, "editCertPublicCertificate")) {
                        relationKey = this.getIntegerSeq(queryParameters.get("accountSeq"), 0);
                        relationKeyName = "accountSeq";

                        PublicCertificateAddVO reqBodyObj = null;
                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, PublicCertificateAddVO.class);
                        }
                        if (reqBodyObj != null) {
                            if(reqBodyObj.getPublicCertificateName() != null) {
                                auditResourceDatas.put("resourceName", reqBodyObj.getPublicCertificateName());
                            }
                        }
                    }
                    // 공인인증서 인증서 삭제
                    else if (StringUtils.equals(methodName, "deleteCertPublicCertificate")) {
                        relationKey = this.getIntegerSeq(queryParameters.get("accountSeq"), 0);
                        relationKeyName = "accountSeq";

                        if ("ok".equals(resultStatus) && resultResult != null) {
                            PublicCertificateDetailVO response = (PublicCertificateDetailVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getPublicCertificateName());
                        }
                    }

                    break;
                }
                case "BuildServerController": {
                    if ("addBuildServer".equals(methodName)) {
                        BuildServerAddVO reqBodyObj = null;
                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, BuildServerAddVO.class);
                        }
                        if (reqBodyObj != null) {
                            relationKey = reqBodyObj.getClusterSeq();
                            relationKeyName = "clusterSeq";

                            if(reqBodyObj.getBuildServerName() != null) {
                                auditResourceDatas.put("resourceName", reqBodyObj.getBuildServerName());
                            }
                        }
                    } else if ("editBuildServer".equals(methodName)) {
                        BuildServerAddVO reqBodyObj = null;
                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, BuildServerAddVO.class);
                        }
                        if (reqBodyObj != null) {
                            relationKey = reqBodyObj.getClusterSeq();
                            relationKeyName = "clusterSeq";

                            if(reqBodyObj.getBuildServerName() != null) {
                                auditResourceDatas.put("resourceName", reqBodyObj.getBuildServerName());
                            }
                        }
                    } else if (
                            "removeBuildServer".equals(methodName)

                    ) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            BuildServerVO response = (BuildServerVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getBuildServerName());
                            relationKey = response.getClusterSeq();
                            relationKeyName = "clusterSeq";
                        }
                    }
                    break;
                }
                case "LogAgentController": {
                    if ("addLogAgent".equals(methodName)) {
                        LogAgentViewVO reqBodyObj = null;
                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, LogAgentViewVO.class);
                        }
                        if (reqBodyObj != null) {
                            relationKey = reqBodyObj.getClusterSeq();
                            relationKeyName = "clusterSeq";

                            if(reqBodyObj.getAgentName() != null) {
                                auditResourceDatas.put("resourceName", reqBodyObj.getAgentName());
                            }
                        }
                    } else if ("editLogAgent".equals(methodName)) {
                        LogAgentViewVO reqBodyObj = null;
                        // request body로부터 auth 정보 수집
                        if (StringUtils.isNotBlank(requestBody) && requestBody.startsWith("{")) {
                            reqBodyObj = JsonUtils.fromJackson(requestBody, LogAgentViewVO.class);
                        }
                        if (reqBodyObj != null) {
                            relationKey = reqBodyObj.getClusterSeq();
                            relationKeyName = "clusterSeq";

                            if(reqBodyObj.getAgentName() != null) {
                                auditResourceDatas.put("resourceName", reqBodyObj.getAgentName());
                            }
                        }
                    } else if (
                            "removeLogAgent".equals(methodName)

                    ) {
                        if ("ok".equals(resultStatus) && resultResult != null) {
                            LogAgentVO response = (LogAgentVO) resultResult;
                            auditResourceDatas.put("resourceName", response.getAgentName());
                            relationKey = response.getClusterSeq();
                            relationKeyName = "clusterSeq";
                        }
                    }
                    break;
                }
                case "TerminalController": {
                    if (StringUtils.equalsAny(methodName
                            , "addLogTerminalOnOpen"
                            , "addLogTerminalOnClose"
                    )) {
                        relationKey = this.setRelationAndGetServicemapSeq(auditResourceDatas, uriTemplateVariables);
                        relationKeyName = "servicemapSeq";

                        if (MapUtils.isNotEmpty(uriTemplateVariables)) {
                            String podName = MapUtils.getString(uriTemplateVariables, "podName", "");
                            String containerName = MapUtils.getString(uriTemplateVariables, "containerName", "");
                            if (StringUtils.isNotBlank(containerName)) {
                                podName = String.format("%s.%s", podName, containerName);
                            }
                            auditResourceDatas.put("resourceName", podName);
                        }
                    }
                    break;
                }
                default: {
                    aLog.debug("### This log is a case excluded from the main audit logging : " + auditLogCode);
                    break;
                }
            }

            /**
             * 공통 로깅 (서비스 Relation 정보)
             **/
            if (relationKey != null && StringUtils.isNotBlank(relationKeyName)) { // serviceRelation 조회가 가능 = 정상케이스 => 리소스 정보 입력.
                ServiceRelationVO serviceRelation = this.getServiceRelation(relationKeyName, relationKey);
                auditResourceDatas.put("serviceRelation", serviceRelation);

                switch (className) {
                    case "ServiceController" : {
                        auditResourceDatas.put("resourceName", serviceRelation.getServiceName());
                        auditResourceDatas.put("resourceSeq", serviceRelation.getServiceSeq());
                        break;
                    }
                    case "PipelineController": {
                        auditResourceDatas.put("resourceName", serviceRelation.getServicemapName());
                    }
                    case "ServicemapGroupController": {
                        auditResourceDatas.put("resourceName", serviceRelation.getServicemapGroupName());
                        auditResourceDatas.put("resourceSeq", serviceRelation.getServicemapGroupSeq());
                        break;
                    }
                    case "ServicemapController": {
                        auditResourceDatas.put("resourceName", serviceRelation.getServicemapName());
                        auditResourceDatas.put("resourceSeq", serviceRelation.getServicemapSeq());
                        break;
                    }
                    case "WorkloadGroupController": {
                        auditResourceDatas.put("resourceName", serviceRelation.getWorkloadGroupName());
                        auditResourceDatas.put("resourceSeq", serviceRelation.getWorkloadGroupSeq());
                        break;
                    }
                }
            }

            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_REFERER, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_REFERER));
            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_CLIENT_IP, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_CLIENT_IP));
            auditResourceDatas.put(CommonConstants.AUDIT_REQUEST_UA, auditProcessingDatas.get(CommonConstants.AUDIT_REQUEST_UA));

            aLog.debug("### Ended generateEachResourceData");
        }
        catch (Exception ex) {
            if(aLog.isDebugEnabled()) {
                aLog.debug("@CATCH : generateEachResourceData Running Failure # : " + ex.getMessage());
                aLog.debug("trace log", ex);
            }
            else {
                aLog.error("@CATCH : generateEachResourceData Running Failure # : " + ex.getMessage(), ex);
            }
            throw ex;
        }
        return auditResourceDatas;
    }

    /**
     * Map 에서 key에 해당하는 값을 꺼내어 숫자 형태이면 Integer로 변환하여 응답..
     *
     * @param params
     * @param key
     * @return
     */
    private Integer getIntegerSeq(Map<String, Object> params, String key) throws Exception {
        if(params.get(key) == null) {
            return null;
        }

        return getIntegerSeq(params.get(key).toString());
    }

    private Integer getIntegerSeq(String[] params, int num) throws Exception {
        if (params != null && params.length >= num + 1) {
            return getIntegerSeq(params[num]);
        }

        return null;
    }

    private Integer getIntegerSeq(String param) throws Exception {
        if(StringUtils.isBlank(param)) {
            return null;
        }

        Integer val = null;
        if (Pattern.matches("^[0-9]+$", param)) {
            val = Integer.valueOf(param);
        }

        return val;
    }

    private ServiceRelationVO getServiceRelation(String keyName, Integer key) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(keyName, key);

        return serviceService.getServiceRelation(params);
    }

    private void setClusterVolumeMap(Map<String, Object> auditResourceDatas, ClusterVolumeVO clusterVolume) {
        auditResourceDatas.put("resourceName", clusterVolume.getName());
        auditResourceDatas.put("totalCapacity", clusterVolume.getTotalCapacity());
        auditResourceDatas.put("reclaimPolicy", clusterVolume.getReclaimPolicy());
    }

    /**
     * Set Service Relation Information
     * @param auditLog
     * @param serviceRelation
     * @return
     */
    private AuditAccessLogVO setServiceRelation(AuditAccessLogVO auditLog, ServiceRelationVO serviceRelation) {
        auditLog.setAccountSeq(serviceRelation.getAccountSeq());
        auditLog.setAccountName(serviceRelation.getAccountName());
        auditLog.setServiceSeq(serviceRelation.getServiceSeq());
        auditLog.setServiceName(serviceRelation.getServiceName());
        auditLog.setClusterSeq(serviceRelation.getClusterSeq());
        auditLog.setClusterName(serviceRelation.getClusterName());
        auditLog.setServicemapGroupSeq(serviceRelation.getServicemapGroupSeq());
        auditLog.setServicemapGroupName(serviceRelation.getServicemapGroupName());
        auditLog.setServicemapSeq(serviceRelation.getServicemapSeq());
        auditLog.setServicemapName(serviceRelation.getServicemapName());
        auditLog.setWorkloadGroupSeq(serviceRelation.getWorkloadGroupSeq());
        auditLog.setWorkloadGroupName(serviceRelation.getWorkloadGroupName());

        return auditLog;
    }


    /**
     * 기존 Servicemap를 URI기반으로 사용하는 API를 Cluster, Namespace기반 API로 변경하면서 이를 체크하고 서비스 릴레이션 정보를 기록하기 위함.. 2022.04.12
     * @param auditResourceDatas
     * @param uriTemplateVariables
     * @return
     * @throws Exception
     */
    private Integer setRelationAndGetServicemapSeq(Map<String, Object> auditResourceDatas, Map<String, Object> uriTemplateVariables) throws Exception {
        Integer servicemapSeq = this.getIntegerSeq(uriTemplateVariables, "servicemapSeq");
        Integer clusterSeq = this.getIntegerSeq(uriTemplateVariables, "clusterSeq");

        Integer relationKey = null;
        if(servicemapSeq != null) {
            relationKey = servicemapSeq;
        }
        else if(clusterSeq != null) {
            /** cluster, namespace 기반 API에 대한 처리 **/
            ServiceRelationVO serviceRelationVO = this.getServiceRelation("clusterSeq", clusterSeq);
            serviceRelationVO.setNamespace(Optional.ofNullable(uriTemplateVariables.get("namespaceName")).orElseGet(() ->Optional.ofNullable(uriTemplateVariables.get("namespace")).orElseGet(() ->"")).toString());
            auditResourceDatas.put("serviceRelation", serviceRelationVO);
        }

        return relationKey;
    }

    /**
     * JSONObject로 부터 key에 해당하는 Value를 찾아서 응답..
     * 1. 요청한 Key에 해당하는 값이 JSONObject이거나 JSONArray일 경우에는 무시하며 String, Number, Boolean, null 일 경우만 응답.
     * 2. key가 여러개일 경우 가장 마지막에 찾은 값을 셋팅
     *
     * @param jsonObj
     * @param getKey
     * @return
     */
    private String findValueOfKeyFromJson(Object jsonObj, String getKey) throws Exception {
        return findValueOfKeyFromJson(jsonObj, getKey, null);
    }

    /**
     * JSONObject로 부터 key에 해당하는 Value를 찾아서 응답..
     * 1. 요청한 Key에 해당하는 값이 JSONObject이거나 JSONArray일 경우에는 무시하며 String, Number, Boolean, null 일 경우만 응답.
     * 2. key가 여러개일 경우 가장 마지막에 찾은 값을 셋팅
     *
     * @param jsonObj
     * @param getKey
     * @param defaultValue
     * @return
     */
    private String findValueOfKeyFromJson(Object jsonObj, String getKey, String defaultValue) throws Exception {
        String setValue = defaultValue;
        if(jsonObj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject)jsonObj;
            Iterator<String> jsonlist = jsonObject.keys();
            while(jsonlist.hasNext()) {
                String key = jsonlist.next();
                if(jsonObject.get(key) instanceof JSONArray) {
                    setValue = findValueOfKeyFromJson(jsonObject.get(key), getKey, setValue);
                } else if(jsonObject.get(key) instanceof JSONObject) {
                    setValue = findValueOfKeyFromJson(jsonObject.get(key), getKey, setValue);
                } else {
                    if(key.equals(getKey)) {
                        setValue = jsonObject.get(key).toString();
                    }
//                    this.printDebugLog(key + " : " + jsonObject.get(key) + " ::::: " + jsonObject.get(key).getClass().getName());
                }
            }
        }
        else if(jsonObj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) jsonObj;
            for(int i = 0; i < jsonArray.length(); i++) {
                if(jsonArray.get(i) instanceof JSONArray) {
                    setValue = findValueOfKeyFromJson(jsonArray.get(i), getKey, setValue);
                } else if(jsonArray.get(i) instanceof JSONObject) {
                    setValue = findValueOfKeyFromJson(jsonArray.get(i), getKey, setValue);
                } else {
//                    this.printDebugLog(i + " : " + jsonArray.get(i) + " ::::: " + jsonArray.get(i).getClass().getName());
                }
            }
        }
        else  {
            throw new CocktailException(String.format("Find Key from JSONObject Error!!"), ExceptionType.InvalidInputData);
        }

        return setValue;
    }

}
