package run.acloud.commons.util;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.catalog.dao.ITemplateMapper;
import run.acloud.api.catalog.enums.TemplateShareType;
import run.acloud.api.catalog.enums.TemplateType;
import run.acloud.api.catalog.vo.TemplateVO;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.commons.enums.AccessibleResources;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.filter.LoggerFilter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ResourcesAccessibleChecker {

    private final static String BUILD_URI_PREFIX = "/builder/";
    private final static String BUILD_KEY_PREFIX = "build";

    // for logging
    private final static String DEBUG_AUTH_ERROR_PREFIX = "@CATCH : "; //"@AUTHOCHECK : ";
    private final static String DEBUG_LOG_PREFIX = " AUTHOCHECK : ";
    private final static boolean IS_PRINTING_DEBUG_DATA = true;
    private final static boolean IS_PRINTING_DEBUG_LOG = true;

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    private static Logger rLog = LoggerFactory.getLogger("resource.access.checker.logger");

    public boolean checkPermission(HttpServletRequest request, HttpServletResponse response, Object handler) throws CocktailException {

        String requestURI = request.getRequestURI();

        this.printDebugLog(String.format("############################### REQUEST URI : %s", requestURI));
        IServiceMapper serviceDao = null;
        /**
         * 00. User Role 및 API 유형에 따른 예외 처리 선행.
         */
        if (ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.ADMIN.getCode())) {
            this.printDebugLog(String.format("=== Skip Admin : %s", requestURI));
            return true;
        } else if (this.isRequestURIMatched("/api/service/seqs/**", requestURI)          // Node Server에서 직접 호출되는 경우 세션 정보가 올바르게 넘어오지 않는 경우가 빈번.. 내부 로직이 로그인 세션 기준 처리하도록 되어 있어 퍼미션 체크 제외하여도 큰 문제 없어 보임..
        ) {
            this.printDebugLog(String.format("=== Skip API (StartsWith) : %s", requestURI));
            return true;
        } else if ((requestURI.endsWith("/api/service") && "GET".equalsIgnoreCase(request.getMethod()))   // DEVOPS 사용자가 serviceSeq 없이 호출하는 일이 빈번 => 사용자계정으로 필터링 되므로 퍼미션 체크 제외하여도 큰 문제 없어 보임..
        ) {
            this.printDebugLog(String.format("=== Skip API (EndsWith) : %s", requestURI));
            return true;
        } else if (!(ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSUSER.getCode()) || ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSDEMO.getCode()) || ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSTEM.getCode()))) {
            // SYSTEM, ADMIN 계정이 아닌 경우 Header에 워크스페이스 정보가 없으면 오류!
            if (Optional.ofNullable(ContextHolder.exeContext().getUserServiceSeq()).orElseGet(() ->0) < 1) {
                if (
                        !(
                                requestURI.endsWith("changePassword") ||
                                        requestURI.endsWith("extendPassword")
                        )
                ) { // DEVOPS 사용자가 로그인 전에 사용하는 API로 Workspace 없이 사용 가능. 이후 처리에서 입력된 userSeq가 본인 것인지 확인 절차 있음..
                    //다음 릴리즈에서 제외 (2020.02.26 제외 검토 완료 - 2020.02.28 제외) : 사용자가 접근 가능한 Workspace를 기준으로 처리하므로 굳이 체크하지 않아도 됨.
//                    throw new CocktailException(DEBUG_AUTH_ERROR_PREFIX + "The DEVOPS role user must have a serviceKey" + makeDebugData(request), ExceptionType.NotAuthorizedToResource);
                }
            } else {
                //DEVOPS 사용자의 Workspace 정보가 올바른지 확인..
                serviceDao = sqlSession.getMapper(IServiceMapper.class);
                Map<String, Object> checkMap = new HashMap<>();
                checkMap.put("loginUserSeq", ContextHolder.exeContext().getUserSeq());
                checkMap.put("loginUserRole", ContextHolder.exeContext().getUserRole());
                checkMap.put(AccessibleResources.SERVICE.getResourceKey(), ContextHolder.exeContext().getUserServiceSeq());
                checkMap.put(AccessibleResources.ACCOUNT.getResourceKey(), ContextHolder.exeContext().getUserAccountSeq());
                int count = serviceDao.getAccessibleServicesCount(checkMap);
                if (count < 1) {
                    throw new CocktailException(this.getErrorLog(String.format("Do not have permission to the requested workspace. : %s", checkMap), request), ExceptionType.NotAuthorizedToResource);
                }
            }
        } else if (StringUtils.startsWith(request.getContentType(), "multipart/")) {
            this.printDebugLog(String.format("=== Skip multipart request : %s", request.getContentType()));
            return true;
        }

        /** Ignore Set 설정. : 2019.10.24 : R3.5
         * 해당 키가 아직 존재하지 않거나(NamespaceName은 생성요청 Param으로 요청되는 경우가 있음..)
         * 아직 DB Relation 생성전 이거나.(서비스 생성시 ClusterSeq, Id 목록 : 서비스와 클러스터 연결)
         * 기타 사유에 의해 체크에 포함될 수 없는 경우..
         *
         * 1. 워크스페이스 생성 / 수정시 clusterSeq는 연결되지 않은 클러스터가 올 수 있으므로 clusterSeq에 대한 Ignore 필요.
         *    2020.01.14 : clusterId로 연결되는 케이스 추가.
         * 2. 카탈로그 배포시 Namespace를 생성하면서 배포할 경우 Namespace에 대한 Ignore 필요.
         * 3. 카탈로그 유효성 체크시 신규 배포이면 appmap이 생성전이라 appmapSeq가 0이 들어올 수 있음..
         * 4. 빌드 재실행시마다 실행 이력을 만드는데 buildRunSeq가 0으로 들어올때가 있음 (자세한건 @coolingi)
         * 5. 빌드 재실행시 연결된 클러스터/서비스맵등이 존재하지 않을 수 있음 (Json형태로 보관되는 정보로 체크 불가)
         * 6. 빌드 재실행시 pipelineContainerSeq만 유효함... (/api/pipelineflow/rerun) : requestBody의 파라메터들 무시..
         * 7. ClusterID를 Resource 권한 체크에 추가. : Cluster 생성시 clusterId에 대한 권한 예외 처리 필요..
         * N. 추가시 로직 추가..
         */
        Set<String> ignoreSet = new HashSet<>();
        if (requestURI.startsWith("/api/service")) { // 1. 워크스페이스 생성 수정 시 CLUSTER 매핑 정보 예외 처리..
            if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
                ignoreSet.add(AccessibleResources.CLUSTER.getResourceKey());
                ignoreSet.add(AccessibleResources.CLUSTERID.getResourceKey());
                /**
                 * 클러스터 권한 체크가 누락되면서 다른 사용자의 클러스터를 연결할 수 있음.. 개선 필요..
                 * 본 서비스가 아닌 해당 API에서 직접 체크하는 것으로 진행..
                 **/
                ignoreSet.add(AccessibleResources.USER.getResourceKey()); // 워크스페이스 생성 / 수정시 사용자 체크 예외 처리. (매니저가 수정시 오류 회피)
                ignoreSet.add(AccessibleResources.SERVICEMAP.getResourceKey()); // 워크스페이스 생성 / 수정시 서비스맵 체크 예외 처리. (매니저가 수정시 오류 회피)
            }
        }
        if (this.isRequestURIMatched("/api/template/**/launch", requestURI)) { // 2. 카탈로그 배포시 타겟 Namespace가 생선 전인 케이스가 있음..
            ignoreSet.add(AccessibleResources.NAMESPACE.getResourceKey());
        }
        if (this.isRequestURIMatched("/api/template/**/valid", requestURI)) { // 3. 카탈로그 유효성 체크시 appmapSeq가 0이 셋팅되는 경우가 있어 appmapSeq 체크는 하지 않음 (ServiceSeq가 함께 들어오므로 권한 체크는 정상 동작)
            ignoreSet.add(AccessibleResources.SERVICEMAP.getResourceKey());
        }
        if (this.isRequestURIMatched("/api/build/run/**", requestURI) && "POST".equalsIgnoreCase(request.getMethod())) { // 4. 빌드 재실행시 buildRunSeq가 0일 때가 있음..
            ignoreSet.add(AccessibleResources.BUILD_RUN.getResourceKey());
        }
        if (this.isRequestURIMatched("/api/build/run/**", requestURI) && "POST".equalsIgnoreCase(request.getMethod())) { // 5. 빌드 재실행시 연결된 클러스터/서비스맵등이 존재하지 않을 수 있음 (Json형태로 보관되는 정보로 체크 불가)
            ignoreSet.add(AccessibleResources.CLUSTERID.getResourceKey());
            ignoreSet.add(AccessibleResources.PIPELINE_CONTAINER.getResourceKey());
            ignoreSet.add(AccessibleResources.PIPELINE_WORKLOAD.getResourceKey());
            ignoreSet.add(AccessibleResources.SERVICEMAP.getResourceKey());
            ignoreSet.add(AccessibleResources.SERVICE.getResourceKey());
            ignoreSet.add(AccessibleResources.NAMESPACE.getResourceKey());
        }
        if (this.isRequestURIMatched("/api/pipelineflow/rerun/**", requestURI) && "POST".equalsIgnoreCase(request.getMethod())) { // 6. 빌드 재실행시 pipelineContainerSeq만 유효함...
            ignoreSet.add(AccessibleResources.CLUSTERID.getResourceKey());
            ignoreSet.add(AccessibleResources.PIPELINE_WORKLOAD.getResourceKey());
            ignoreSet.add(AccessibleResources.SERVICEMAP.getResourceKey());
            ignoreSet.add(AccessibleResources.SERVICE.getResourceKey());
            ignoreSet.add(AccessibleResources.NAMESPACE.getResourceKey());
            ignoreSet.add(AccessibleResources.ACCOUNT.getResourceKey());
            ignoreSet.add(AccessibleResources.BUILD_RUN.getResourceKey());
            ignoreSet.add(AccessibleResources.BUILD_STEP_RUN.getResourceKey());
            ignoreSet.add(AccessibleResources.BUILD.getResourceKey());
        }
        if (requestURI.equalsIgnoreCase("/api/cluster") && "POST".equalsIgnoreCase(request.getMethod())) { // 7. 클러스터 생성시 cluster-id에 대한 권한 체크 예외..
            ignoreSet.add(AccessibleResources.CLUSTERID.getResourceKey());
        }
        if ((this.isRequestURIMatched("/api/service/**/tenant/soft", requestURI)
                || this.isRequestURIMatched("/api/service/**/tenant/hard", requestURI))
                && "PUT".equalsIgnoreCase(request.getMethod())
        ) { // 8. 워크스페이스 소프트/하드 테넌트 리소스 관리시에는 appmap 및 Cluster 대한 소유권한이 아직 없으므로 예외 처리.
            ignoreSet.add(AccessibleResources.SERVICEMAP.getResourceKey());
            ignoreSet.add(AccessibleResources.CLUSTER.getResourceKey());
            ignoreSet.add(AccessibleResources.CLUSTERID.getResourceKey());
        }

        if ((requestURI.equalsIgnoreCase("/api/servicemap/v2") || requestURI.equalsIgnoreCase("/api/servicemap/v2/existnamespace"))
                && "POST".equalsIgnoreCase(request.getMethod())) { // 9. Servicemap 생성시 클러스터의 권한이 없이 생성하는 케이스 (소프트 테넌시로 추가하는 경우)
            ignoreSet.add(AccessibleResources.CLUSTERID.getResourceKey());
            ignoreSet.add(AccessibleResources.SERVICE.getResourceKey());
            ignoreSet.add(AccessibleResources.SERVICEMAP.getResourceKey());
            if (!requestURI.equalsIgnoreCase("/api/servicemap/v2/existnamespace")) {
                ignoreSet.add(AccessibleResources.NAMESPACE.getResourceKey());
            }
        }


        /**
         * 00. Request Parameter List를 Setting할 변수 선언..
         *     이후 처리에서 params에 존재하는 키에 대해서만 리소스 권한 확인을 진행함..
         */
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> ignoreParams = new HashMap<>(); // ignore 처리한 Parameter중 단독으로 체크가 필요한 경우 사용..
        for (String resourceKey : AccessibleResources.getResourceKeyList()) {
            // Resource Key Setting for Search
            if (!ignoreSet.contains(resourceKey)) { // ignoreSet에 포함되어 있지 않을때만 params에 put..
                params.put(resourceKey, null);
            } else {
                ignoreParams.put(resourceKey, null);
            }
        }

        /**
         * 01. POST와 PUT 요청시 RequestBody로 들어오는 객체에서 필요한 리소스의 Key값을 추출..
         * 리소스 추가시 AccessibleResources.java & Service.xml파일의 getAccessibleResourcesCount Query 에 내용 추가...
         */
        String requestBody = null;
        try {
            if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
                requestBody = LoggerFilter.getBody(request); // UTF-8로 인코딩된 requestBody를 읽는다..
                if (StringUtils.isNotBlank(requestBody) && StringUtils.startsWith(requestBody, "{")) {
                    JSONObject jsonObject = new JSONObject(requestBody);
                    this.printDebugLog("=================== RequestBody");
                    this.setValueToKeyMapFromJson(jsonObject, params, ignoreParams, request);
                    this.printDebugLog("===============================");
                } else if (StringUtils.isNotBlank(requestBody) && StringUtils.startsWith(requestBody, "[")) {
                    JSONArray jsonArray = new JSONArray(requestBody);
                    this.printDebugLog("=================== RequestBody");
                    this.setValueToKeyMapFromJson(jsonArray, params, ignoreParams, request);
                    this.printDebugLog("===============================");
                }
            }
        } catch (IOException | JSONException me) {
            if (rLog.isDebugEnabled()) {
                throw new CocktailException(this.getErrorLog("Exception : RequestBody Parsing Error : ", request), me, ExceptionType.NotAuthorizedToResource);
            }
            else {
                // 운영 모드에서는 서비스에 영향이 없도록 에러 로그를 남기고 다음 처리로 넘어감..
                this.printErrorLog("Exception : RequestBody Parsing Error : ", request, me);
            }
        }


        /**
         * 02. Query Parameter List에서 리소스의 Key값을 추출..
         */
        if(!(request.getHeader("Content-Type") != null && request.getHeader("Content-Type").startsWith("multipart/form-data"))) {
            Map<String, String[]> queryParameters = request.getParameterMap();
            if (queryParameters != null && queryParameters.size() > 0) {
                this.printDebugLog("============== Query Parameters");

                for (String gKey : queryParameters.keySet()) {

                    if (params.containsKey(gKey)) {
                        if (params.get(gKey) != null && queryParameters.get(gKey).length > 0 && StringUtils.isNotBlank(queryParameters.get(gKey)[0])) {
                            // 해당 케이스 존재시 처리방안을 확인하기 위해 error 로그 남김.. (확인되는 케이스 아직 없음..)
                            this.printErrorLog(String.format("Query Parameters Setting Error (Already Exist in POST Parameters) : %s : %s", gKey, JsonUtils.toGson(queryParameters)), request);
                        }
                        else {
                            if (gKey.equals(AccessibleResources.SERVICES.getResourceKey())) {
                                // serviceSeqs는 List Object로 구성한다..
                                this.setStringOrInteger(params, gKey, queryParameters.get(gKey), request);
                            }
                            else if (queryParameters.get(gKey).length == 1) {
                                this.setStringOrInteger(params, gKey, queryParameters.get(gKey)[0], request);
                            }
                            else if (queryParameters.get(gKey).length > 1) {
                                this.setStringOrInteger(params, gKey, queryParameters.get(gKey)[0], request);
                                // 리소스가 Array로 유입시 첫번째 값만 사용함. 해당 케이스 존재시 처리방안을 확인하기 위해 error 로그 남김.. (현재 식별하고 있는 리소스에서는 serviceSeq만 케이스에 해당하며 이는 위에서 예외 처리함. 이 외에 해당 케이스 아직 없음..)
                                this.printErrorLog(String.format("Query Parameters Setting Error (This is List Object but only use first Object) : %s : %s", gKey, JsonUtils.toGson(queryParameters)), request);
                            }
                        }
                    }
                }

                this.printDebugLog("===============================");
            }
            else {
                this.printDebugLog(String.format("=== Query Parameters is null : %s", queryParameters.size()) );
            }
        }

        /**
         * 03. Path Variables로 수집되는 Parameter들을 추출..
         */
        Map<String, Object> uriTemplateVariables = Collections.emptyMap();
        if (request != null) {
            uriTemplateVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        }
        this.printDebugLog("======== URI TEMPLATE VARIABLES");
        params.put("loginUserSeq", ContextHolder.exeContext().getUserSeq());
        params.put("loginUserRole", ContextHolder.exeContext().getUserRole());
        // RequestBody에서 설정된 워크스페이스 정보가 없으면 워크스페이스 정보(serviceSeq)를 Setting
        if(params.get(AccessibleResources.SERVICE.getResourceKey()) == null || StringUtils.isBlank(params.get(AccessibleResources.SERVICE.getResourceKey()).toString())) {
            /**
             * SYSTEM 계정은 워크스페이스 세션 정보를 사용하지 않음..
             * 01. Input Parameter로 serviceSeq가 없으면 계정이 접근 가능한 모든 워크스페이스 기준으로 접근 권한 판단.
             * 02. Input Parameter로 serviceSeq가 들어오면 해당 값은 사용함... => 해당 서비스에 대한 명시적 요청이 분명하므로..
             *    (그리고, serviceSeq만을 Key로 한 데이터 요청이 있을 수 있으므로(서비스내 리소스 조회 등) Parameter로 들어온 serviceSeq에 대한 판단은 필요..)
             *
             * ## 2019.12.02 : DEVOPS 사용자에게 추가 권한 부여 : 클러스터 할당유형이 "CLUSTER"인 경우 클러스터 메뉴에서 현재 세션과 다른 워크스페이스의 리소스 접근 가능해짐..
             *                 이로 인해 Default로 serviceSeq를 설정하는 아래 로직을 제거함..
             */
//                if(!ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSTEM.getCode())) {
//                    params.put(AccessibleResources.SERVICE.getResourceKey(), ContextHolder.exeContext().getUserServiceSeq());
//                }
        }
        for (String k : uriTemplateVariables.keySet()) {
            if(uriTemplateVariables.get(k) instanceof String || uriTemplateVariables.get(k) instanceof Integer) {
                String val = uriTemplateVariables.get(k).toString();
                if (!(ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSUSER.getCode()) || ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSDEMO.getCode()) || ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSTEM.getCode())) && // SYSTEM 계정이 아닐 경우만..
                    params.containsKey(k) &&
                    params.get(k) != null &&
                    StringUtils.isNotBlank(params.get(k).toString()) &&
                    !params.get(k).toString().equals(val)) {
                    // Resource의 Key가 여러개일 경우 서로 값이 다르면 문제가 있는 것으로 판단.. => 오류 로그 남김 (Debug mode에서는 Throw 시켜 데이터 확인)
                    if (rLog.isDebugEnabled()) {
                        this.printErrorLog(String.format("Resource Keys are Different : key(%s), 1st Value(%s), 2nd Value(%s)", k, params.get(k), val), request);
                    }
                    else {
                        // 서비스에 영향이 없도록 에러 로그를 남기고 Path Variables에 대한 처리는 계속 함..
                        this.printErrorLog(String.format("Resource Keys are Different : key(%s), 1st Value(%s), 2nd Value(%s)", k, params.get(k), val), request);
                    }
                }
                if(params.containsKey(k)) {
                    this.setStringOrInteger(params, k, val, request);
                }
            }
        }
        this.printDebugLog("===============================");

        /** 2019.12.02 : Null data Remove **/
        for (String resourceKey : AccessibleResources.getResourceKeyList()) {
            // Remove null data
            params.remove(resourceKey, null);
        }

        /**
         * 04. templateSeq에 대한 요청이 있을 경우 공유 템플릿 판단을 위해 추가 로직 처리..
         * 처리 후 templateSeq는 제거..
         */
        if (params.get(AccessibleResources.TEMPLATE.getResourceKey()) != null) {
            if (this.checkTemplatePermission(params, params.get(AccessibleResources.TEMPLATE.getResourceKey()).toString()) < 1) {
                throw new CocktailException(this.getErrorLog(String.format("This call does not have permission for the template. : %s", params), request), ExceptionType.NotAuthorizedToResource);
            }
            else {
                params.remove(AccessibleResources.TEMPLATE.getResourceKey());
            }
        }


        /**
         * 수집된 파라미터에 대한 체크 예외 처리 추가 : 2019.11.05
         * Namespace에 대한 권한 체크는 아래 명기한 파라미터들이 존재하지 않을때에만 진행..
         **/
        if(params.containsKey(AccessibleResources.CLUSTER.getResourceKey()) ||
            params.containsKey(AccessibleResources.CLUSTERID.getResourceKey()) ||
            params.containsKey(AccessibleResources.SERVICE.getResourceKey()) ||
            params.containsKey(AccessibleResources.SERVICEMAP.getResourceKey()) ||
            params.containsKey(AccessibleResources.SERVICES.getResourceKey())
//            || params.containsKey(AccessibleResources.COMPONENT.getResourceKey())
            )
        {
            // Namespace에 대한 권한 체크는 독립적인 요청이 있을 경우에만 처리
            // 현재 전체 API목록 검토 결과 Namespace는 반드시 CLUSTER와 함께 사용되고 있으므로 권한 체크가 동작하지 않으나 향 후 개발중 문제 발생 체크를 위해 로직은 남겨둠..
            params.remove(AccessibleResources.NAMESPACE.getResourceKey());
        }

        /**
         * 05. Check Authorization.
         */


        // 05-01 : Check Pre Conditions.
        if(!(ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSUSER.getCode()) || ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSDEMO.getCode()) || ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSTEM.getCode()))) { // if DEVOPS role

            boolean isUserAPI = false;
            boolean isOwner = false;
            boolean isUserAPILastService = this.isRequestURIMatched("/api/user/lastServiceSeq/**", requestURI);

            if(this.isRequestURIMatched("/api/user/**", requestURI)) {
                isUserAPI = true;

                // DEVOPS 권한으로 User Service에 접근 했을때 userSeq가 본인의 정보일때만 접근 가능 처리..
                if(params.get(AccessibleResources.USER.getResourceKey()) != null && StringUtils.isNotBlank(params.get(AccessibleResources.USER.getResourceKey()).toString())) {
                    if(!params.get(AccessibleResources.USER.getResourceKey()).toString().equalsIgnoreCase(ContextHolder.exeContext().getUserSeq().toString())) {
                        throw new CocktailException(this.getErrorLog(String.format("Can't access other users : %s, %s",
                                params.get(AccessibleResources.USER.getResourceKey()).toString(),
                                ContextHolder.exeContext().getUserSeq().toString()), request), ExceptionType.NotAuthorizedToResource);
                    } else {
                        isOwner = true;
                    }
                }
                else {
                    // INPUT에 userSeq가 없는 경우 POST / PUT Method는 권한을 주지 않음.. (자기 정보만 Access 가능)
                    if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
                        // 예외 처리..
                        if(!isUserAPILastService) {
                            throw new CocktailException(this.getErrorLog("The DEVOPS role user doesn't have Permission.", request), ExceptionType.NotAuthorizedToResource);
                        }
                    }
                }
            }

            /** 해당 서비스의 사용자 권한이 VIEW 권한인데, POST|PUT|DELETE 로 호출한 경우는 권한오류로 처리 **/
            Integer userSeq = ContextHolder.exeContext().getUserSeq();
            Integer serviceSeq = ContextHolder.exeContext().getUserServiceSeq();

            // request method 가 POST|PUT|DELETE 때 체크
            if ( "POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()) || "DELETE".equalsIgnoreCase(request.getMethod()) ) {
                boolean isDeleteMethod = "DELETE".equalsIgnoreCase(request.getMethod());

                if (
                        // 예외처리
                        (isUserAPI && !isOwner)    // 사용자 API 호출시 본인 resource인 경우
                        && (isUserAPI && isOwner && isDeleteMethod) // 사용자 API 호출시 본인 resource이나 DELETE 메서드인 경우
                        && !isUserAPILastService  // lastServiceSeq 처리 API
                ) {
                    // 시스템 데모 사용자는
                    if (UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isSysdemo()) {
                        throw new CocktailException(this.getErrorLog("The SYSDEMO grant user doesn't have Permission.", request), ExceptionType.NotAuthorizedToResource);
                    }

                    // userSeq 와 serviceSeq 가 존재한다면
                    if ( Optional.ofNullable(userSeq).orElseGet(() ->0) > 0 && Optional.ofNullable(serviceSeq).orElseGet(() ->0) > 0 ) {
                        IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
                        UserVO accessUser = userDao.selectByUserSeq(userSeq, "Y");

                        if(ListUtils.emptyIfNull(accessUser.getUserRelations()).size() > 0){
                            Optional<ServiceRelationVO> relationOptional = accessUser.getUserRelations().stream().filter(rel -> rel.getServiceSeq().equals(serviceSeq)).findFirst();
                            if ( relationOptional.isPresent()){
                                // VIEWER 사용자
                                if (relationOptional.get().getUserGrant().isViewer()) {
                                    throw new CocktailException(this.getErrorLog("The VIEWER grant user doesn't have Permission.", request), ExceptionType.NotAuthorizedToResource);
                                }
                                // DEVOPS or DEV 사용자 - 파이프라인 API 제외
                                if (!this.isRequestURIMatched("/api/pl/**", requestURI) && relationOptional.get().getUserGrant().isPipeline()) {
                                    throw new CocktailException(this.getErrorLog("The DEVOPS or DEV grant user doesn't have Permission.", request), ExceptionType.NotAuthorizedToResource);
                                }
                            }
                        }
                    }
                }

            }
        }

        // 05-02 : Check Params (데이터가 복합적으로 들어오는 경우 CLUSTER만 기준으로 처리하기 위함 / 칵테일은 우선 클러스터에 대한 권한 부여 후 하위 권한들이 부여되므로 클러스터 권한만으로도 어느정도 체크가 가능함)
        if(params.containsKey(AccessibleResources.CLUSTER.getResourceKey())) {
            params.remove(AccessibleResources.CLUSTERID.getResourceKey());
            params.remove(AccessibleResources.SERVICE.getResourceKey());
            params.remove(AccessibleResources.SERVICES.getResourceKey());
            params.remove(AccessibleResources.SERVICEMAP.getResourceKey());
        }
        if(params.containsKey(AccessibleResources.CLUSTERID.getResourceKey())) {
            params.remove(AccessibleResources.SERVICE.getResourceKey());
            params.remove(AccessibleResources.SERVICES.getResourceKey());
            params.remove(AccessibleResources.SERVICEMAP.getResourceKey());
        }

        // 05-03 : Check Authorization.
        IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
        serviceDao = serviceDao == null ? sqlSession.getMapper(IServiceMapper.class) : serviceDao;
        // SYSTEM계정이고 AND 워크스페이스 정보 (serviceSeq)가 존재하지 않는 경우
        if( // ContextHolder.exeContext().getUserRole().equalsIgnoreCase(UserRole.SYSTEM.getCode()) &&
            ( params.get(AccessibleResources.SERVICE.getResourceKey()) == null || StringUtils.isBlank(params.get(AccessibleResources.SERVICE.getResourceKey()).toString()) ))
        {
            this.printDebugLog(String.format("==== No Workspace. : %s", JsonUtils.toGson(params)));
            /** =====================================================================================
             * SYSTEM 사용자는 service(워크스페이스)에 종속적으로 동작하지 않으므로 개별 리소스에 대해 권한 체크가 필요함..
             * 01. 관리 메뉴에 접근하여 API 호출시 세션의 워크스페이스 기준으로 동작하면 안되며,
             * 02. 워크스페이스가 0개일때도 API 호출이 가능.
             * 따라서 리소스별로 세분화 하여 권한 체크가 필요함.
             ======================================================================================== */
            /**
             * 01. Input Parameter로 요청된 리소스가 워크스페이스 반드시 필요한 경우 계정의 워크스페이스 정보를 기준으로 권한 체크.
             *     아래 리소스들은 모두 워크스페이스가 생성된 이후에 생성되는 리소스들임 (mustHaveWorkspace)
             *   - serviceSeqs, serviceSeq, appmapSeq, groupSeq, componentSeq, pipelineWorkloadSeq, pipelineContainerSeq
             *
             * 02. Input Parameter로 요청된 리소스가 워크스페이스 없이 존재 가능한 경우 계정의 Account 정보를 기준으로 권한 체크.
             *     아래 리소스들은 워크스페이스 없이 존개 가능. (noWorkspaceRequired)
             *   - accountSeq, clusterSeq, volumeSeq, userSeq, templateSeq, buildSeq, buildRunSeq, buildStepRunSeq
             */
            Map<String, Object> mustHaveWorkspace = new HashMap<>();
            boolean isFindMust = false;
            Map<String, Object> noWorkspaceRequired = new HashMap<>();
            boolean isFindNo = false;
            for(AccessibleResources ar : AccessibleResources.values()) {
                if(ar.getMustHaveService()) {
                    if(params.containsKey(ar.getResourceKey())) { // params에 해당 키가 있을때만 put
                        isFindMust = true;
                        mustHaveWorkspace.put(ar.getResourceKey(), params.get(ar.getResourceKey()));
                    }
                }
                else {
                    if(params.containsKey(ar.getResourceKey())) { // params에 해당 키가 있을때만 put
                        isFindNo = true;
                        noWorkspaceRequired.put(ar.getResourceKey(), params.get(ar.getResourceKey()));
                    }
                }
            }
            /**
             * 각 케이스별 Parameter 존재시 권한을 체크함..
             */
            // Workspace 정보가 있어야만 존재할 수 있는 리소스에 대한 권한 체크
            if(isFindMust) {
                mustHaveWorkspace.put("loginUserSeq", ContextHolder.exeContext().getUserSeq());
                mustHaveWorkspace.put("loginUserRole", ContextHolder.exeContext().getUserRole());
                this.printDebugLog(String.format("=========== Must Have Workspace : %s", JsonUtils.toGson(mustHaveWorkspace)));
                // 서비스가 여러개 요청되었을 경우 따로 체크..
                if(mustHaveWorkspace.get(AccessibleResources.SERVICES.getResourceKey()) != null) {
                    Map<String, Object> services = new HashMap<>();
                    services.put("loginUserSeq", ContextHolder.exeContext().getUserSeq());
                    services.put("loginUserRole", ContextHolder.exeContext().getUserRole());
                    services.put(AccessibleResources.SERVICES.getResourceKey(), mustHaveWorkspace.get(AccessibleResources.SERVICES.getResourceKey()));
                    int countsvc = serviceDao.getAccessibleResourcesCount(services);
//                        this.printDebugLog(String.format(DEBUG_LOG_PREFIX + "Services Count Check : %s : %s", countsvc, ((List)mustHaveWorkspace.get(AccessibleResources.SERVICES.getResourceKey())).size()));
                    if(countsvc < ((List)mustHaveWorkspace.get(AccessibleResources.SERVICES.getResourceKey())).size()) {
                        throw new CocktailException(this.getErrorLog(String.format("This call does not have permission for the Service List (case : system account & no have workspace) : %s : %s", countsvc, ((List)mustHaveWorkspace.get(AccessibleResources.SERVICES.getResourceKey())).size()), request), ExceptionType.NotAuthorizedToResource);
                    }

                    mustHaveWorkspace.remove(AccessibleResources.SERVICES.getResourceKey());
                }

                // 아래 Query 조회시 workspace 없음. userSeq 기준으로 해당 사용자가 접근 가능한 서비스를 가져와 권한 조회
                int count = serviceDao.getAccessibleResourcesCount(mustHaveWorkspace);
                if (count < 1) {
                    throw new CocktailException(this.getErrorLog(String.format("This call does not have permission for the resource. (case : must have workspace) : %s", mustHaveWorkspace), request), ExceptionType.NotAuthorizedToResource);
                }
            }

            // Workspace 정보가 없어도 존재 가능한 리소스에 대한 권한 체크 (accountSeq, clusterSeq, userSeq)
            if(isFindNo) {
                noWorkspaceRequired.put("loginUserSeq", ContextHolder.exeContext().getUserSeq());
                noWorkspaceRequired.put("loginUserRole", ContextHolder.exeContext().getUserRole());
                this.printDebugLog(String.format("======= No Workspace Required : %s", JsonUtils.toGson(noWorkspaceRequired)));
                int count = accountDao.getAccessibleResourcesCountWithoutWorkspace(noWorkspaceRequired);
                if (count < 1) {
                    throw new CocktailException(this.getErrorLog(String.format("This call does not have permission for the resource. (case : no workspace required) : %s", noWorkspaceRequired), request), ExceptionType.NotAuthorizedToResource);
                }

                // 사용자 테이블이 account_user_mapping과 account_system_mapping으로 나뉘어져 있어 UNION Query로 처리 필요 => userSeq는 별도 로직으로 처리함..
                if(noWorkspaceRequired.containsKey(AccessibleResources.USER.getResourceKey()) && noWorkspaceRequired.get(AccessibleResources.USER.getResourceKey()) != null) {
                    int countuser = accountDao.getAccessibleUserCountWithoutWorkspace(noWorkspaceRequired);
                    if (countuser < 1) {
                        throw new CocktailException(this.getErrorLog(String.format("This call does not have permission for the user. : %s", noWorkspaceRequired), request), ExceptionType.NotAuthorizedToResource);
                    }
                }
            }
        }
        else { // 워크스페이스 정보 (serviceSeq)가 존재
            this.printDebugLog(String.format("==== Service has a Workspace. : %s", JsonUtils.toGson(params)));
            // 서비스가 여러개 요청되었을 경우 따로 체크..
            if(params.get(AccessibleResources.SERVICES.getResourceKey()) != null) {
                Map<String, Object> services = new HashMap<>();
                services.put(AccessibleResources.SERVICES.getResourceKey(), params.get(AccessibleResources.SERVICES.getResourceKey()));
                services.put("loginUserSeq", ContextHolder.exeContext().getUserSeq());
                services.put("loginUserRole", ContextHolder.exeContext().getUserRole());
                int countsvc = serviceDao.getAccessibleResourcesCount(services);

                this.printDebugLog(String.format("Services Count Check : %s : %s", countsvc, ((List)params.get(AccessibleResources.SERVICES.getResourceKey())).size()));
                if(countsvc < ((List)params.get(AccessibleResources.SERVICES.getResourceKey())).size()) {
                    throw new CocktailException(this.getErrorLog(String.format("This call does not have permission for the Service List (case : have a workspace) : %s : %s", countsvc, ((List)params.get(AccessibleResources.SERVICES.getResourceKey())).size()), request), ExceptionType.NotAuthorizedToResource);
                }

                params.remove(AccessibleResources.SERVICES.getResourceKey());
            }
            int count = serviceDao.getAccessibleResourcesCount(params);
            if (count < 1) {
                throw new CocktailException(this.getErrorLog(String.format("This call does not have permission for the resource. (case : have a workspace) : %s", params), request), ExceptionType.NotAuthorizedToResource);
            }

            // 일반적인 케이스에서는 UserService API에 serviceSeq가 존재하지 않아 이곳으로 흐를 수 없으나.
            // Input Data에 강제로 "serviceSeq"를 파라미터 추가하여 본 로직으로 흘러올 수 있으며, 이로인해 SYSTEM 사용자가 다른 어카운트의 계정에 접근할 수 있는 홀이 존재함..
            // => 사용자 접근 권한 체크 추가.. 2019.12.03
            if(params.containsKey(AccessibleResources.USER.getResourceKey()) && params.get(AccessibleResources.USER.getResourceKey()) != null) {
                int countuser = accountDao.getAccessibleUserCountWithoutWorkspace(params);
                if (countuser < 1) {
                    throw new CocktailException(this.getErrorLog(String.format("This call does not have permission for the user. : %s", params), request), ExceptionType.NotAuthorizedToResource);
                }
            }
        }
        this.printDebugLog("############################### Resource permission check succeeded ###############################");

        return true;
    }

    /**
     * Template에 대한 접근 권한 판단..
     *
     * @param params
     * @param templateSeq
     * @return
     * @throws Exception
     */
    private int checkTemplatePermission(Map<String, Object> params, String templateSeq) {
        // templateSeq가 숫자가 아니면 오류.
        if (!Pattern.matches("^[0-9]+$", templateSeq)) {
            return 0;
        }

        ITemplateMapper templateDao = sqlSession.getMapper(ITemplateMapper.class);
        TemplateVO template = templateDao.getTemplate(Integer.valueOf(templateSeq), "Y");
        if(template == null || template.getTemplateType() == null) {
            // Template이 존재하지 않음... 메인 비즈니스 로직에서 처리하도록 Skip 함..
            return 1;
        }
        else if(template.getTemplateType().equalsIgnoreCase(TemplateType.COCKTAIL.getCode())) {
            this.printDebugLog(String.format(">>>> COCKTAIL Shared Template : %s : %s : %s", template.getTemplateType(), template.getTemplateShareType(), templateSeq));
            return 1;
        }
        else if(template.getTemplateShareType().equalsIgnoreCase(TemplateShareType.SYSTEM_SHARE.getCode())) {
            this.printDebugLog(String.format(">>>> SYSTEM Shared Template : %s : %s : %s", template.getTemplateType(), template.getTemplateShareType(), templateSeq));
            params.put("systemTemplateSeq", Integer.valueOf(templateSeq));
            return templateDao.getSystemTemplatePermission(params);
        }
        else { // WORKSPACE_SHARE
            this.printDebugLog(String.format(">>>> WORKSPACE Shared Template : %s : %s : %s", template.getTemplateType(), template.getTemplateShareType(), templateSeq));
            params.put("workspaceTemplateSeq", Integer.valueOf(templateSeq));
            return templateDao.getWorkspaceTemplatePermission(params);
        }
    }

    /**
     * Map에 값 셋팅.
     * @param params
     * @param key
     * @param val
     */
    private void setStringOrInteger(Map<String, Object> params, String key, String val) {
        setStringOrInteger(params, key, val, null);
    }

    private void setStringOrInteger(Map<String, Object> params, String key, String val, HttpServletRequest request) {
        // value가 숫자로만 구성되어 있고 Key에 "Seq"가 포함되어 있으면 Map에 Integer Type으로 Set
        if(StringUtils.isNotBlank(val)) {
            String buildKey;
            if (Pattern.matches("^[0-9]*$", val) && key.contains("Seq")) {
                if (request != null && request.getRequestURI().startsWith(BUILD_URI_PREFIX)) {
                    buildKey = String.format("%s_%s", BUILD_KEY_PREFIX, key);
                    params.put(buildKey, Integer.valueOf(val));
                    this.printDebugLog(String.format(">>>>> Integer (builder) : %s : %s", buildKey, params.get(key)));
                }
                else {
                    params.put(key, Integer.valueOf(val));
                    this.printDebugLog(String.format(">>>>> Integer : %s : %s", key, params.get(key)));
                }
            }
            else {
                if (request != null && request.getRequestURI().startsWith(BUILD_URI_PREFIX)) {
                    buildKey = String.format("%s_%s", BUILD_KEY_PREFIX, key);
                    params.put(buildKey, val);
                    this.printDebugLog(String.format(">>>>>> String (builder) : %s : %s", buildKey, params.get(key)));
                }
                else {
                    params.put(key, val);
                    this.printDebugLog(String.format(">>>>>> String : %s : %s", key, params.get(key)));
                }
            }
        }
    }

    private void setStringOrInteger(Map<String, Object> params, String key, String[] arrayVal, HttpServletRequest request) {
        Set<String> strList = new HashSet<>();
        Set<Integer> intList = new HashSet<>();
        for(int i = 0; i < arrayVal.length; i++) {
            if(StringUtils.isNotBlank(arrayVal[i])) {
                // value가 숫자로만 구성되어 있고 Key에 "Seq"가 포함되어 있으면 Map에 Integer Type으로 Set
                if (Pattern.matches("^[0-9]+$", arrayVal[i]) && key.contains("Seq")) {
                    intList.add(Integer.valueOf(arrayVal[i]));
                    this.printDebugLog(String.format(">>>>> Integer(l) : %s : %s", key, arrayVal[i]));
                }
                else {
                    strList.add(arrayVal[i]);
                    this.printDebugLog(String.format(">>>>> String(l) : %s : %s", key, arrayVal[i]));
                }
            }
        }
        if (request != null && request.getRequestURI().startsWith(BUILD_URI_PREFIX)) {
            String buildKey = String.format("%s_%s", BUILD_KEY_PREFIX, key);
            if(strList.size() > 0) {

                this.printDebugLog(String.format(">>>>>> String(l) (builder) : %s : %s", buildKey, JsonUtils.toGson(strList)));
                params.put(buildKey, new ArrayList(strList));
            }
            else {
                this.printDebugLog(String.format(">>>>>> Integer(l) (builder) : %s : %s", buildKey, JsonUtils.toGson(intList)));
                params.put(buildKey, new ArrayList(intList));
            }
        }
        else {
            if(strList.size() > 0) {

                this.printDebugLog(String.format(">>>>>> String(l) (acloud): %s : %s", key, JsonUtils.toGson(strList)));
                params.put(key, new ArrayList(strList));
            }
            else {
                this.printDebugLog(String.format(">>>>> Integer(l) (acloud) : %s : %s", key, JsonUtils.toGson(intList)));
                params.put(key, new ArrayList(intList));
            }
        }
    }

    public String makeDebugData(HttpServletRequest request) {
        return makeDebugData(request, true);
    }
    /**
     * 디버깅을 위한 상세 데이터 로깅..
     * @param request
     * @return
     */
    private String makeDebugData(HttpServletRequest request, boolean lineDelimiter) {
        if(!IS_PRINTING_DEBUG_DATA) {
            return "";
        }

        Map<String, Object> params = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder();
        try {
            SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
            Date time = new Date();
            String loggingTime = format1.format(time);
            params.put("loggingTime", loggingTime);
            if (rLog.isDebugEnabled()) {
                params.put("requestURL", request.getRequestURL());
                params.put("userSeq", ContextHolder.exeContext().getUserSeq());
                params.put("userRole", ContextHolder.exeContext().getUserRole());
                params.put("userWorkspace", ContextHolder.exeContext().getUserServiceSeq());
                // Query Parameters
                params.put("queryParams", request.getParameterMap());

                // URI Path Variables
                Map<String, Object> uriTemplateVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                params.put("pathParams", uriTemplateVariables);

                // Request Body
                if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
                    String requestBody = LoggerFilter.getBody(request); // UTF-8로 인코딩된 requestBody를 읽는다..
                    if (StringUtils.isNotBlank(requestBody) && StringUtils.startsWith(requestBody, "{")) {
                        params.put("requestBody", JsonUtils.toJsonObject(requestBody));
                    }
                    else if (StringUtils.isNotBlank(requestBody) && StringUtils.startsWith(requestBody, "[")) {
                        params.put("requestBody", JsonUtils.toJsonArray(requestBody));
                    }
                }

                if (lineDelimiter) {
                    return stringBuilder.append("\n====================\n").append(DEBUG_AUTH_ERROR_PREFIX).append(JsonUtils.toGson(params)).append("\n====================\n").toString();
                }
            }
        }
        catch (Exception ex) {
            params.put("parsingError", request.getRequestURL());
        }

        return stringBuilder.append(DEBUG_AUTH_ERROR_PREFIX).append(JsonUtils.toGson(params)).toString();
    }

    /**
     * JSONObject로 부터 map에 입력한 key에 해당하는 value를 찾아서 응답..
     * 1. 요청한 Key에 해당하는 값이 JSONObject이거나 JSONArray일 경우에는 무시하며 String, Number, Boolean, null 일 경우만 셋팅.
     * 2. key가 여러개일 경우 가장 마지막에 찾은 값을 셋팅
     *
     * @param jsonObj
     * @param map
     */
    private void setValueToKeyMapFromJson(Object jsonObj, Map<String, Object> map, Map<String, Object> ignoreMap, HttpServletRequest request) {
        if(jsonObj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject)jsonObj;
            Iterator<String> jsonlist = jsonObject.keys();
            String buildKey;
            while(jsonlist.hasNext()) {
                String key = jsonlist.next();
                if(jsonObject.get(key) instanceof JSONArray) {
                    this.setValueToKeyMapFromJson(jsonObject.get(key), map, ignoreMap, request);
                } else if(jsonObject.get(key) instanceof JSONObject) {
//                    this.printDebugLog("====== JSONObject : " + key);
                    this.setValueToKeyMapFromJson(jsonObject.get(key), map, ignoreMap, request);
                } else {
                    if(map.containsKey(key)) {
                        if(request != null && request.getRequestURI().startsWith(BUILD_URI_PREFIX)) {
                            buildKey = String.format("%s_%s", BUILD_KEY_PREFIX, key);
                            this.printDebugLog(String.format("found key (builder) ::::::::: %s : %s", buildKey, jsonObject.get(key)));
                            map.put(buildKey, jsonObject.get(key));
                        }
                        else {
                            this.printDebugLog(String.format("found key ::::::::: %s : %s", key, jsonObject.get(key)));
                            map.put(key, jsonObject.get(key));
                        }
                    }
                    else {
                        if(MapUtils.isNotEmpty(ignoreMap)) {

                            this.printDebugLog(String.format("found ignore key ::::::::: %s : %s", key, jsonObject.get(key)));
                            ignoreMap.put(key, jsonObject.get(key));
                        }
                    }
                }
            }
        }
        else if(jsonObj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) jsonObj;
            for(int i = 0; i < jsonArray.length(); i++) {
                if(jsonArray.get(i) instanceof JSONArray) {
                    this.setValueToKeyMapFromJson(jsonArray.get(i), map, ignoreMap, request);
                } else if(jsonArray.get(i) instanceof JSONObject) {
                    this.setValueToKeyMapFromJson(jsonArray.get(i), map, ignoreMap, request);
                }
            }
        }
        else  {
            throw new CocktailException(this.getErrorLog("RequestBody Parsing Error!!", request), ExceptionType.InvalidInputData);
        }
    }

    private boolean isRequestURIMatched(String pattern, String requestURI) {
        return AntPathMatcherUtils.getPathMatcher().match(pattern, requestURI);
    }

    private void printDebugLog(String logStr) {
        if(IS_PRINTING_DEBUG_LOG) {
            rLog.debug("{} {}", DEBUG_LOG_PREFIX, logStr);
        }
    }

    private String getErrorLog(String errMsg, HttpServletRequest request) {
        return String.format("%s %s %s", DEBUG_AUTH_ERROR_PREFIX, StringUtils.defaultString(errMsg), this.makeDebugData(request));
    }

    private void printErrorLog(String errMsg, HttpServletRequest request) {
        this.printErrorLog(errMsg, request, null);
    }

    private void printErrorLog(String errMsg, HttpServletRequest request, Exception e) {
        if (e != null) {
            rLog.error(this.getErrorLog(errMsg, request), e);
        } else {
            rLog.error(this.getErrorLog(errMsg, request));
        }
    }
}
