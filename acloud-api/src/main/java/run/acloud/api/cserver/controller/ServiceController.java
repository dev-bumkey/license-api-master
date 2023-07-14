package run.acloud.api.cserver.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.build.service.PipelineBuildService;
import run.acloud.api.build.vo.BuildVO;
import run.acloud.api.catalog.service.TemplateService;
import run.acloud.api.configuration.enums.ServiceRegistryType;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.service.*;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.service.ServiceValidService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.pipelineflow.service.PipelineFlowService;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.K8sDeployYamlVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.CRUDCommand;
import run.acloud.commons.enums.HarborRegistryProjectMemberRole;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.*;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io on 2017. 1. 10.
 */
@Tag(name = "Workspace", description = "칵테일 워크스페이스 관련 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/service")
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ServiceValidService serviceValidService;

    @Autowired
    private ServicemapService servicemapService;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private ClusterStateService clusterStateService;

	@Autowired
    private HarborRegistryFactoryService harborRegistryFactory;

    @Autowired
    private RegistryPropertyService registryProperties;

    @Autowired
    private PipelineBuildService pipelineBuildService;

    @Autowired
    private PipelineFlowService pipelineFlowService;

    /** build개선, adding serivce refer to Build */
    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountGradeService accountGradeService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ExternalRegistryService externalRegistryService;

    private enum Step {
        Start,
        DockerUser,
        DockerProject,
        DockerUserToProject,
        DockerUserToAnotherProject,
        Builder
    }

    @InHouse
    @PostMapping(value = "/{apiVersion}")
    @Operation(summary = "워크스페이스 생성", description = "칵테일 워크스페이스를 생성한다.")
    public ServiceAddVO addService(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(description = "서비스생성 모델", required = true) @RequestBody ServiceAddVO service
    		) throws Exception {
        /** less than R4.4.0 **/
        return this.addServiceCommon(apiVersion, service);
    }

    @PostMapping(value = "")
    @Operation(summary = "워크스페이스 생성", description = "칵테일 워크스페이스를 생성한다.")
    public ServiceAddVO addService(
        @Parameter(description = "서비스생성 모델", required = true) @RequestBody ServiceAddVO service
    ) throws Exception {
        /** greater than R4.4.0 **/
        return this.addServiceCommon("v2", service);
    }

    public ServiceAddVO addServiceCommon(String apiVersion, ServiceAddVO service) throws Exception {
        log.debug("[BEGIN] addService");
        if(service.getServiceType() == null) {
            service.setServiceType(ServiceType.NORMAL); // default NORMAL
        }
//        if(service.getClusterTenancy() == null) {
//            throw new CocktailException("clusterTenancy is null.", ExceptionType.InvalidParameter_Empty);
//        }

        /**
         * header 정보로 요청 사용자 권한 체크, DevOps는 권한 없음
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        // check parameter
        serviceValidService.checkParameter(service);

        // check workspace name
        serviceValidService.checkWorkspaceNameUsed(service);

        AccountVO account = null;
        if(service.getAccountSeq() != null){
            boolean isValidAccount = true;
            account = accountService.getAccount(service.getAccountSeq());

            if(account == null){
                isValidAccount = false;
            }

            if(!isValidAccount){
                throw new CocktailException("account seq is invalid.", ExceptionType.InvalidParameter, ExceptionMessageUtils.setParameterDataInvalid("accountSeq", service.getAccountSeq()));
            }

            // account 에 설정된 workspace 갯수를 초과 하는지 체크
            boolean isPossibleRegisterWorkspace = accountGradeService.isPossibleRegisterWorkspace(service.getAccountSeq());
            if(!isPossibleRegisterWorkspace){ // 등록 불가능하면 Exception 발생
                String errMsg = "Account Has Exceeded the Maximum Number of Allowed Workspaces.";
                throw new CocktailException(errMsg, ExceptionType.ExceededMaximumWorkspaceInAccount, ExceptionMessageUtils.setParameterData("accountSeq", service.getAccountSeq(), errMsg));
            }

        }else{
            throw new CocktailException("account seq is null.", ExceptionType.InvalidParameter_Empty, ExceptionMessageUtils.setParameterDataEmpty("accountSeq", service.getAccountSeq()));
        }

        /** Harbor Registry 사용자 정보 구성 **/
        HarborUserReqVO registryReqUser = new HarborUserReqVO();
        // 생성되는 워크스페이스 (서비스)에 생성한 사용자 정보 Setting => DB Insert시에는 암호화 처리해서 넣음.
        service.setRegistryUserId(ResourceUtil.makeRegistryUserId());
        service.setRegistryUserPassword(ResourceUtil.makeRegistryUserPassword());
        // 해당 워크스페이스의 registry 사용자 등록 정보
        registryReqUser.setUsername(service.getRegistryUserId());
        registryReqUser.setPassword(service.getRegistryUserPassword());
        registryReqUser.setRealname("default devops user");

        HarborUserRespVO registryUser = new HarborUserRespVO();

        /**
         * AccountType not in (CubeEngine) 일 경우에만 registry 기능 지원
         */
        if (!account.getAccountType().isCubeEngine()) {
            // harbor api client
//            ProductsApi productsApi = registryService.getProductsApiByAccountSeq(service.getAccountSeq());
            IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(service.getAccountSeq());

            // 해당 account에 registry의 type이 'SERVICE'인 registry를 조회 (SERVICE는 해당 service에서 생성한 것)
            List<ServiceDetailVO> services = Optional.ofNullable(serviceService.getServiceOfAccount(service.getAccountSeq(), ServiceRegistryType.SERVICE.getCode())).orElseGet(() ->Lists.newArrayList());
            // projectId 별로 Map에 셋팅
            Map<Integer, ServiceRegistryVO> anotherServiceProjectMap =
                Optional.ofNullable(
                    services.stream()
                        .filter(s -> (CollectionUtils.isNotEmpty(s.getProjects())))
                        .flatMap(s -> (s.getProjects().stream()))
                        .collect(Collectors.toMap(ServiceRegistryVO::getProjectId, Function.identity())))
                    .orElseGet(() ->Maps.newHashMap());

            /**
             * 01. Registry 유효성 체크.
             */
            if (CollectionUtils.isNotEmpty(service.getProjects())) {
                // 'SERVICE' type은 1개이상 생성 불가..
                long serviceRegistryCount = service.getProjects().stream().filter(p -> (p.getProjectType() == ServiceRegistryType.SERVICE)).count();
                if (serviceRegistryCount > 1) {
                    throw new CocktailException("Only one registry can be created.", ExceptionType.OnlyOneRegistryCanBeCreated);
                }

                if(ApiVersionType.V1.getType().equalsIgnoreCase(apiVersion)) { // R4.4.0 이전에는 기본 Registry가 Mandatory
                    if (serviceRegistryCount == 0) {
                        throw new CocktailException("There must be at least one registry.", ExceptionType.MustBeAtLeastOneRegistry);
                    }
                }
                for (ServiceRegistryVO srRow : service.getProjects()) {
                    // 'SERVICE' Type
                    if (srRow.getProjectType() == ServiceRegistryType.SERVICE) {
                        ExceptionMessageUtils.checkParameter("projectName", srRow.getProjectName(), 30, true);

                        // projectName명 중복 체크
                        List<RegistryProjectVO> harborProjects = harborRegistryService.getProjectList(srRow.getProjectName());
                        if (CollectionUtils.isNotEmpty(harborProjects)) {
                            Optional<RegistryProjectVO> harborProjectsOptional = harborProjects.stream().filter(hp -> (StringUtils.equals(hp.getName(), srRow.getProjectName()))).findFirst();
                            if (harborProjectsOptional.isPresent()) {
                                throw new CocktailException("Already exists registry project!!", ExceptionType.RegistryProjectAlreadyExists);
                            }
                        }
                    }
                    // 'SHARE' Type
                    else {
                        // 'SHARE' Type은 실제 account에 속한 'SERVICE' type의 registry 정보와 맞는 지 체크
                        boolean isValid = true;
                        if (!anotherServiceProjectMap.containsKey(srRow.getProjectId())) {
                            isValid = false;
                        } else {
                            if (MapUtils.getObject(anotherServiceProjectMap, srRow.getProjectId(), null) != null) {
                                if (!StringUtils.equals(anotherServiceProjectMap.get(srRow.getProjectId()).getProjectName(), srRow.getProjectName())) {
                                    isValid = false;
                                }
                            } else {
                                isValid = false;
                            }
                        }

                        if (!isValid) {
                            throw new CocktailException(String.format("The registry(%d : %s) to share does not exist on the system", srRow.getProjectId(), srRow.getProjectName()), ExceptionType.RegistryToShareDoesNotExistOnTheSystem);
                        }
                    }
                }
            } else {
                if(ApiVersionType.V1.getType().equalsIgnoreCase(apiVersion)) { // R4.4.0 이전에는 Registry가 필수 입력...
                    throw new CocktailException("There must be at least one registry.", ExceptionType.MustBeAtLeastOneRegistry);
                }
            }

            Step step = Step.Start;

            /**
             * 02. Registry 등록 시작
             */
            try {
                /** 02.01 harbor에 사용자 등록**/
                registryUser = harborRegistryService.addUser(registryReqUser);
                step = Step.DockerUser;

                if (registryUser != null) {
                    HarborProjectMemberVO projectMember = new HarborProjectMemberVO();
                    projectMember.setEntityName(registryUser.getUsername());
                    // DEVELOPER(pull, push) 권한 부여
                    projectMember.setRoleId(HarborRegistryProjectMemberRole.DEVELOPER.getValue());

                    /** 02.02 Harbor에 Project생성(if SERVICE type) / 위에서 생성한 사용자를 Project Member로 등록 **/
                    List<Integer> shareProjectIds = new ArrayList<>();
                    // 'SERVICE' type의 registry는 생성하고 위에서 생성한 service registry 사용자를 할당함.
                    // 'SHARE' type의 registry는 위에서 생성한 service registry 사용자만 할당함.
                    if (CollectionUtils.isNotEmpty(service.getProjects())) {
                        service.setProjectIds(new ArrayList<>());
                        HarborProjectReqVO projectReq;
                        for (ServiceRegistryVO srRow : service.getProjects()) {
                            if (srRow.getProjectType() == ServiceRegistryType.SERVICE) {
                                projectReq = new HarborProjectReqVO();
                                projectReq.setPublic(false);
                                projectReq.setProjectName(srRow.getProjectName());
                                RegistryProjectVO addProject = harborRegistryService.addProject(projectReq);

                                if (addProject != null) { // 등록 성공
                                    service.getProjectIds().add(addProject.getProjectId());
                                    srRow.setProjectId(addProject.getProjectId());

                                    // add member ( role : developer )
                                    harborRegistryService.addMemberToProject(addProject.getProjectId(), projectMember, false);
                                }
                            } else {
                                shareProjectIds.add(srRow.getProjectId());
                                // add member ( role : developer )
                                harborRegistryService.addMemberToProject(srRow.getProjectId(), projectMember, false);
                            }
                        }
                    }

                    step = Step.DockerProject;

                    /** 02.03 위에서 생성한 사용사를 account 안의 다른 service registry에 멤버(GUEST)로 사용자 등록 처리  (ImagePull은 어디서든 가능하도록 처리..) **/
                    List<ServiceDetailVO> servicesOfAccount = serviceService.getServiceOfAccount(service.getAccountSeq(), ServiceRegistryType.SERVICE.getCode());
                    if (CollectionUtils.isNotEmpty(servicesOfAccount)) {
                        projectMember.setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());

                        HarborProjectMemberVO anotherProjectMember = new HarborProjectMemberVO();
                        anotherProjectMember.setEntityName(registryUser.getUsername());
                        anotherProjectMember.setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());

                        for (ServiceDetailVO serviceRow : servicesOfAccount) {
                            if (CollectionUtils.isNotEmpty(serviceRow.getProjects())) {
                                for (ServiceRegistryVO srRow : serviceRow.getProjects()) {
                                    if (!shareProjectIds.contains(srRow.getProjectId())) {
                                        // add member ( role : guest )
                                        harborRegistryService.addMemberToProject(srRow.getProjectId(), projectMember, false);
                                    }
                                }
                            }
                            if (CollectionUtils.isNotEmpty(service.getProjectIds())) {
                                anotherProjectMember.setEntityName(CryptoUtils.decryptAES(serviceRow.getRegistryUserId()));
                                for (Integer projectIdRow : service.getProjectIds()) {
                                    // add member ( role : guest )
                                    harborRegistryService.addMemberToProject(projectIdRow, anotherProjectMember, false);
                                }
                            }
                        }
                    }
                    step = Step.DockerUserToAnotherProject;

                    service.setCreator(ContextHolder.exeContext().getUserSeq());

                    // service 등록
                    serviceService.addService(service, apiVersion);
                } else {
                    throw new CocktailException("add registry user fail!!", ExceptionType.RegistryAddUserFail);
                }

            } catch (Exception eo) { // roll back as possible
                log.error("fail addService!!", eo);
                try {
                    switch (step) {
                        case DockerUserToAnotherProject:
                        case DockerUserToProject:
                        case DockerProject:
                            if (CollectionUtils.isNotEmpty(service.getProjects())) {
                                for (ServiceRegistryVO srRow : service.getProjects()) {
                                    if (srRow.getProjectType() == ServiceRegistryType.SERVICE && srRow.getProjectId() != null) {
                                        harborRegistryService.deleteProject(srRow.getProjectId());
                                    }
                                }
                            }
                        case DockerUser:
                            if (registryUser != null && registryUser.getUserId() != null) {
                                harborRegistryService.deleteUser(registryUser.getUserId());
                            }
                    }
                } catch (Exception eo2) {
                    log.error("fail rollback addService!!", eo2);
                }

                throw eo;
            }
        } else {
            if (CollectionUtils.isNotEmpty(service.getProjects())) {
                CocktailException ce = new CocktailException(String.format("Registry is not supported.[%s]", account.getAccountType().getCode()), ExceptionType.InvalidParameter, service.getProjects());
                log.warn("Registry is not supported.", ce);
                service.setProjects(null);
            }
            // harbor api client
            IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(service.getAccountSeq());

            Step step = Step.Start;

            try {
                /** CubeEngine -> Cocktail 서비스로 Upgrade가 가능하므로, 사용자는 미리 생성 해 둔다 **/
                // harbor에 사용자 등록
                registryUser = harborRegistryService.addUser(registryReqUser);
                step = Step.DockerUser;

                service.setCreator(ContextHolder.exeContext().getUserSeq());

                // service 등록
                service = serviceService.addService(service, apiVersion);
            } catch (Exception eo) { // roll back as possible
                log.error("fail addService!!", eo);
                try {
                    switch (step) {
                        case DockerUser:
                            if (registryUser != null && registryUser.getUserId() != null) {
                                harborRegistryService.deleteUser(registryUser.getUserId());
                            }
                    }
                } catch (Exception eo2) {
                    log.error("fail rollback addService!!", eo2);
                }

                throw eo;
            }
        }

        log.debug("[END  ] addService");

        return service;
    }

    @InHouse
    @PutMapping(value = "/{apiVersion}/{serviceSeq}")
    @Operation(summary = "워크스페이스 수정", description = "칵테일 워크스페이스를 수정한다.")
    public ServiceAddVO updateService(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(description = "서비스번호", required = true) @PathVariable Integer serviceSeq,
    		@Parameter(description = "서비스생성 모델", required = true) @RequestBody ServiceAddVO service
    		) throws Exception {

        /** less than R4.4.0 **/
        return this.updateServiceCommon(apiVersion, serviceSeq, service);
    }

    @PutMapping(value = "/{serviceSeq}")
    @Operation(summary = "워크스페이스 수정", description = "칵테일 워크스페이스를 수정한다.")
    public ServiceAddVO updateService(
        @Parameter(description = "서비스번호", required = true) @PathVariable Integer serviceSeq,
        @Parameter(description = "서비스생성 모델", required = true) @RequestBody ServiceAddVO service
    ) throws Exception {
        /** Greater than R4.4.0 **/
        return this.updateServiceCommon(ApiVersionType.V2.getType(), serviceSeq, service);
    }

    public ServiceAddVO updateServiceCommon(String apiVersion, Integer serviceSeq, ServiceAddVO service) throws Exception {
    	log.debug("[BEGIN] updateService");
        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        // check parameter
        serviceValidService.checkParameter(service);
        if(service.getClusterTenancy() == null) {
            throw new CocktailException("clusterTenancy is null.", ExceptionType.InvalidParameter_Empty);
        }

        /**
         * Service 정보 조회 (Docker Registry 사용자 정보 포함)
         * docker registry 접속 사용자를 읽어
         * 1. 기본 Registry 등록일 경우 (기본 Registry가 없는 케이스) Registry를 등록 해 주고
         * 2. 사용하지 않는 공유 Registry Project에서는 제거해주며,
         * 3. 새로 사용할 공유 Registry Project에는 신규 등록 한다.
         */
        ServiceDetailVO currService = serviceService.getService(serviceSeq);
        service.setAccountSeq(currService.getAccount().getAccountSeq());

        // 워크스페이스명이 변경 되었을 경우 이름 중복을 체크한다. check workspace name
        if ( !StringUtils.equalsIgnoreCase(currService.getServiceName(), service.getServiceName()) ) {
            serviceValidService.checkWorkspaceNameUsed(service);
        }

        /** Cluster Tenancy가 달라지면 연결된 서비스맵 or 클러스터가 없어야 함 **/
        /**
         * 2022.05.31 hjchoi - clusterTenancy 기능 제거, 기본 SOFT 로 값만 셋팅
         */
//        if(currService.getClusterTenancy() != service.getClusterTenancy()) {
//            if(currService.getClusterTenancy() == ClusterTenancy.HARD) {
//                if(CollectionUtils.isNotEmpty(currService.getClusters())) {
//                    throw new CocktailException(ExceptionType.TenancyCannotBeChangedToSoft.getExceptionPolicy().getMessage(), ExceptionType.TenancyCannotBeChangedToSoft);
//                }
//            }
//            else if(currService.getClusterTenancy() == ClusterTenancy.SOFT) {
//                if(CollectionUtils.isNotEmpty(currService.getServicemaps())) {
//                    throw new CocktailException(ExceptionType.TenancyCannotBeChangedToHard.getExceptionPolicy().getMessage(), ExceptionType.TenancyCannotBeChangedToHard);
//                }
//            }
//        }

        /**
         * AccountType not in (CubeEngine) 일 경우에만 registry를 지원
         */
        if (!currService.getAccount().getAccountType().isCubeEngine()) {
            /** 현재 서비스(워크스페이스)에 등록되어 있는 전체 Project(Registry) 목록 조회 **/
            List<ServiceRegistryVO> currProjects = Optional.ofNullable(currService).map(ServiceDetailVO::getProjects).orElseGet(() ->new ArrayList<>());
            Map<Integer, ServiceRegistryVO> currProjectIdMap = currProjects.stream().collect(Collectors.toMap(ServiceRegistryVO::getProjectId, Function.identity()));
            // get Harbor API Client
//            ProductsApi productsApi = harborRegistryFactory.getProductsApiByAccountSeq(currService.getAccount().getAccountSeq());
            IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(currService.getAccount().getAccountSeq());
            /** 현재 서비스를 소유한 account내에서 registry의 type이 'SERVICE'인 registry를 조회 (SERVICE는 해당 service에서 생성한 것)
                (아래에서 공유 Registry가 유효한 Registry 정보 인지 확인하기 위해 사용됨) **/
            List<ServiceDetailVO> services = Optional.ofNullable(serviceService.getServiceOfAccount(currService.getAccount().getAccountSeq(), ServiceRegistryType.SERVICE.getCode())).orElseGet(() ->Lists.newArrayList());
            // projectId 별로 Map에 셋팅
            Map<Integer, ServiceRegistryVO> anotherServiceProjectMap =
                    Optional.ofNullable(
                            services.stream()
                                    .filter(s -> (CollectionUtils.isNotEmpty(s.getProjects())))
                                    .flatMap(s -> (s.getProjects().stream()))
                                    .collect(Collectors.toMap(ServiceRegistryVO::getProjectId, Function.identity())))
                            .orElseGet(() ->Maps.newHashMap());

            CRUDCommand command = CRUDCommand.N;
            /**
             * 01. Registry 유효성 체크. SERVICE type의 신규 등록이 있는지 확인. 있다면 command를 Create로 설정.
             */
            if (CollectionUtils.isNotEmpty(service.getProjects())) {
                // 'SERVICE' type은 1개이상 생성 불가..
                long serviceRegistryCount = service.getProjects().stream().filter(p -> (p.getProjectType() == ServiceRegistryType.SERVICE)).count();
                if (serviceRegistryCount > 1) {
                    throw new CocktailException("Only one registry can be created.", ExceptionType.OnlyOneRegistryCanBeCreated);
                }

                if(ApiVersionType.V1.getType().equalsIgnoreCase(apiVersion)) { // R4.4.0 이전에는 기본 Registry가 Mandatory
                   if (serviceRegistryCount == 0) {
                        throw new CocktailException("There must be at least one registry.", ExceptionType.MustBeAtLeastOneRegistry);
                    }
                }

                long currStandardRegistryCount = currProjects.stream().filter(p -> (p.getProjectType() == ServiceRegistryType.SERVICE)).count();
                for (ServiceRegistryVO srRow : service.getProjects()) {
                    if (srRow.getProjectId() == null) {
                        if (srRow.getProjectType() != ServiceRegistryType.SERVICE) {
                            throw new CocktailException("The projectid does not exist in shared registry", ExceptionType.InvalidInputData);
                        }
                    }

                    if (srRow.getProjectType() == ServiceRegistryType.SERVICE) {
                        if (MapUtils.isNotEmpty(currProjectIdMap)) {
                            if (!currProjectIdMap.containsKey(srRow.getProjectId())) {
                                // 기존 Registry 목록에 현재 입력된 Project가 없다.
                                if(ApiVersionType.V1.getType().equalsIgnoreCase(apiVersion)) {
                                    throw new CocktailException("Only one registry can be created.", ExceptionType.OnlyOneRegistryCanBeCreated);
                                }
                                else {
                                    if(currStandardRegistryCount > 0) { // 기존에도 있고 입력받은 값도 있는데 ProjectId가 다르면 오류 입력
                                        throw new CocktailException("Only one registry can be created.", ExceptionType.OnlyOneRegistryCanBeCreated);
                                    }
                                    else { // 기존에는 없는데 새로 입력받은 내용이 있다면 추가.
                                        command = CRUDCommand.C;
                                    }
                                }
                            } else {
                                // 기존 Registry 목록에 현재 입력된 Project가 있다.
                                if (MapUtils.getObject(currProjectIdMap, srRow.getProjectId(), null) != null) {
                                    // 그런데 Registry 이름이 같지 않다면 오류.
                                    if (!(currProjectIdMap.get(srRow.getProjectId()).getProjectType() == srRow.getProjectType()
                                            && StringUtils.equals(currProjectIdMap.get(srRow.getProjectId()).getProjectName(), srRow.getProjectName()))) {
                                        throw new CocktailException("Only one registry can be created.", ExceptionType.OnlyOneRegistryCanBeCreated);
                                    }
                                }
                            }
                        }
                    }
                    // 'SHARE' Type
                    else {
                        // 'SHARE' Type은 실제 account에 속한 'SERVICE' type의 registry 정보와 맞는 지 체크
                        boolean isValid = true;
                        if (!anotherServiceProjectMap.containsKey(srRow.getProjectId())) {
                            isValid = false;
                        } else {
                            if (MapUtils.getObject(anotherServiceProjectMap, srRow.getProjectId(), null) != null) {
                                if (!StringUtils.equals(anotherServiceProjectMap.get(srRow.getProjectId()).getProjectName(), srRow.getProjectName())) {
                                    isValid = false;
                                }
                            } else {
                                isValid = false;
                            }
                        }

                        if (!isValid) {
                            throw new CocktailException(String.format("The registry(%d : %s) to share does not exist on the system", srRow.getProjectId(), srRow.getProjectName()), ExceptionType.RegistryToShareDoesNotExistOnTheSystem);
                        }
                    }
                }
            } else {
                if(ApiVersionType.V1.getType().equalsIgnoreCase(apiVersion)) { // R4.4.0 이전에는 Registry가 필수 입력...
                    throw new CocktailException("There must be at least one registry.", ExceptionType.MustBeAtLeastOneRegistry);
                }
            }

            /** R4.4.0 부터 적용. : 기본 레지스트리가 없다가 신규로 등록되거나 삭제가 될 수 있음 : Validation 체크만 **/
            List<Integer> projectIdList = new ArrayList<>();
            ServiceRegistryVO currStdPrj = currProjects.stream().filter(cp -> cp.getProjectType() == ServiceRegistryType.SERVICE).findFirst().orElseGet(() ->null);
            ServiceRegistryVO reqStdPrj = CollectionUtils.isEmpty(service.getProjects()) ? null : service.getProjects().stream().filter(cp -> cp.getProjectType() == ServiceRegistryType.SERVICE).findFirst().orElseGet(() ->null);
            if(!ApiVersionType.V1.getType().equalsIgnoreCase(apiVersion)) {
                // Create 여부는 위에서 판단되어서 내려옴.
                if (currStdPrj != null && reqStdPrj == null) { // 기존에 SERVICE type이 있는데 새로 입력된 내용이 없으면 삭제
                    command = CRUDCommand.D;
                }

                switch (command) {
                    /**
                     * 01. 기본 Registry 삭제 Case.
                     **/
                    case D: {// 삭제
                        /** 01-01. image registry 를 사용하는 워크로드가 존재하는 지 체크 */
                        // 기본 레지스트리 삭제시에는 워크로드 존재 여부 체크 안함, 아래 build 정보 유무 체크에서 걸러짐..

                        /** 01-02. build 정보 유무 체크 */
                        // 'SERVICE'유형으로 생성된 project는 빌드를 삭제 후 삭제 가능
                        projectIdList = currProjects.stream().filter(cp -> (cp.getProjectType() == ServiceRegistryType.SERVICE)).map(ServiceRegistryVO::getProjectId).collect(Collectors.toList());
                        List<BuildVO> builds = pipelineBuildService.getBuildList(currService.getAccount().getAccountSeq(), projectIdList);

                        if (CollectionUtils.isNotEmpty(builds)) {
                            throw new CocktailException("이 워크스페이스는 빌드를 포함하고 있어 삭제될 수 없습니다. 빌드를 먼저 삭제해 주세요.", ExceptionType.ServiceContainsBuild);
                        }
                    }
                    break;
                    /**
                     * 02. 기본 Registry 생성 Case.
                     **/
                    case C: { // 생성
                        /** 02.01. Validation Check **/
                        ExceptionMessageUtils.checkParameter("projectName", reqStdPrj.getProjectName(), 30, true);

                        // projectName명 중복 체크
                        List<RegistryProjectVO> harborProjects = harborRegistryService.getProjectList(reqStdPrj.getProjectName());
                        if (CollectionUtils.isNotEmpty(harborProjects)) {
                            Optional<RegistryProjectVO> harborProjectsOptional = harborProjects.stream().filter(hp -> (StringUtils.equals(hp.getName(), reqStdPrj.getProjectName()))).findFirst();
                            if (harborProjectsOptional.isPresent()) {
                                throw new CocktailException("Already exists registry project!!", ExceptionType.RegistryProjectAlreadyExists);
                            }
                        }
                    }
                    break;
                    default: // No change
                        break;

                }
            }

            /**
             * 02. 공유 Registry의 추가 / 삭제에 따라서 사용자 권한 설정 : 신규 추가된 Project는(ProjectId가 Null인것) 제외함
             */
            Map<Integer, ServiceRegistryVO> reqProjectIdMap = service.getProjects().stream().filter(sp -> sp.getProjectId() != null).collect(Collectors.toMap(ServiceRegistryVO::getProjectId, Function.identity()));
            List<Integer> delProjectIds = ListUtils.subtract(Lists.newArrayList(currProjectIdMap.keySet()), Lists.newArrayList(reqProjectIdMap.keySet()));
            List<Integer> addProjectIds = ListUtils.subtract(Lists.newArrayList(reqProjectIdMap.keySet()), Lists.newArrayList(currProjectIdMap.keySet()));

            HarborUserRespVO registryUser = harborRegistryService.getUser(CryptoUtils.decryptAES(currService.getRegistryUserId()));
            HarborProjectMemberVO projectMember = new HarborProjectMemberVO();
            if (registryUser != null) {
                projectMember.setEntityName(registryUser.getUsername());

                // project member 추가 - GUEST, 존재하면 role만 변경
                if (CollectionUtils.isNotEmpty(delProjectIds)) {
                    // 워크스페이스에서 해당 레지스트리를 사용하는 지 확인
                    boolean isExistsPipeline = pipelineFlowService.checkPipelineUsingServiceSeqAndRegistryIds(serviceSeq, delProjectIds, null);

                    if (isExistsPipeline) {
                        throw new CocktailException("이 워크스페이스의 이미지 레지스트리를 사용하는 워크로드가 존재하여 삭제될 수 없습니다. 워크로드를 먼저 중지 후, 삭제하거나 이미지를 다른 이미지로 교체해 주세요.", ExceptionType.WorkloadUsingImageRegistryExists);
                    }
                    projectMember.setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());
                    for (Integer projectIdRow : delProjectIds) {
                        HarborProjectMemberVO projectMemberEntity = harborRegistryService.getMemberOfProject(projectIdRow, registryUser.getUsername());
                        if (projectMemberEntity != null) {
                            harborRegistryService.deleteMemberOfProject(projectIdRow, projectMemberEntity.getId());
                        }
                    }
                }
                // project member 추가 - DEVELOPER, 존재하면 role만 변경
                if (CollectionUtils.isNotEmpty(addProjectIds)) {
                    projectMember.setRoleId(HarborRegistryProjectMemberRole.DEVELOPER.getValue());
                    for (Integer projectIdRow : addProjectIds) {
                        HarborProjectMemberVO projectMemberEntity = harborRegistryService.getMemberOfProject(projectIdRow, registryUser.getUsername());
                        if (projectMemberEntity != null) {
                            harborRegistryService.updateMemberOfProject(projectIdRow, projectMemberEntity.getId(), HarborRegistryProjectMemberRole.DEVELOPER.getValue());
                        } else {
                            harborRegistryService.addMemberToProject(projectIdRow, projectMember, false);
                        }
                    }
                }

                /**
                 * 03. Registry 추가 / 삭제 처리.
                 */
                /** R4.4.0 부터 적용. : 기본 레지스트리가 없다가 신규로 등록되거나 삭제가 될 수 있음 : 실제 추가/삭제 처리 **/
                if(!ApiVersionType.V1.getType().equalsIgnoreCase(apiVersion)) {
                    switch (command) {
                        /**
                         * 01. 기본 Registry 삭제 Case.
                         **/
                        case D: {// 삭제
                            /** 01.03. Registry 삭제 : 01. Registry 내의 사용자 삭제 **/
                            try {
                                /** 사용자는 워크스페이스가 삭제될때만 지워주어야 함... 사용자 정보는 해당 워크스페이스의 서비스맵에서 계속 사용... **/
//                            if (StringUtils.isNotBlank(currService.getRegistryUserId())) {
//                                // registry의 사용자 삭제
//                                registryService.deleteUser(productsApi, CryptoUtils.decryptAES(currService.getRegistryUserId()));
//                            }
                            }
                            catch (Exception eo) {
                                log.error("trace log ", eo); // 오류시 에러 로깅 후 진행.
                            }

                            /** 01.04. Registry 삭제 : 02. Registry 삭제 **/
                            try {
                                for (Integer id : projectIdList) { // registry 에서 project 삭제, 현재는 service(workspace)별 아이디가 1개씩만 발급되므로 실제로는 한번만 looping 됨.
                                    // registry server에서 project 삭제
                                    harborRegistryService.deleteProject(id);
                                    // 공유된 service registry도 모두 제거해줌
                                    List<ServiceRegistryVO> sharedRegistryInfos = serviceService.getServiceRegistryOfAccount(currService.getAccount().getAccountSeq(), ServiceRegistryType.SHARE.getCode(), id);
                                    if (CollectionUtils.isNotEmpty(sharedRegistryInfos)) {
                                        for (ServiceRegistryVO serviceRegistryRow : sharedRegistryInfos) {
                                            serviceService.deleteProjectsOfService(serviceRegistryRow.getServiceSeq(), Arrays.asList(id));
                                        }
                                    }
                                }
                                // service registry 삭제 : updateService에서 삭제 해줌...
//                            serviceService.deleteProjectsOfService(serviceSeq, null);
                            }
                            catch (Exception eo) {
                                log.error("trace log ", eo); // 오류시 에러 로깅 후 진행.
                            }
                        }
                        break;
                        /**
                         * 02. 기본 Registry 생성 Case.
                         **/
                        case C: { // 생성이 있음...
                            /** 02.02 Harbor에 Project생성(if SERVICE type) / 기존에 생성된 사용자를 Project Member로 등록 **/
                            if (CollectionUtils.isNotEmpty(service.getProjects())) {
                                for (ServiceRegistryVO srRow : service.getProjects()) {
                                    if (srRow.getProjectType() == ServiceRegistryType.SERVICE) {
                                        service.setProjectIds(new ArrayList<>());
                                        HarborProjectReqVO projectReq = new HarborProjectReqVO();
                                        projectReq.setPublic(false);
                                        projectReq.setProjectName(srRow.getProjectName());
                                        RegistryProjectVO addProject = harborRegistryService.addProject(projectReq);

                                        if (addProject != null) { // 등록 성공
                                            service.getProjectIds().add(addProject.getProjectId());
                                            srRow.setProjectId(addProject.getProjectId());
                                            // add member ( role : developer )
                                            projectMember.setRoleId(HarborRegistryProjectMemberRole.DEVELOPER.getValue());
                                            harborRegistryService.addMemberToProject(addProject.getProjectId(), projectMember, false);
                                        }
                                        break; // 1개만 있음.
                                    }
                                }
                            }
                        }
                        break;
                        default: // No change
                            break;

                    }
                }
            }

        } else {
            /** CubeEngine은 Registry Project 지원 안함 **/
            if (CollectionUtils.isNotEmpty(service.getProjects())) {
                CocktailException ce = new CocktailException(String.format("Registry is not supported.[%s]", currService.getAccount().getAccountType().getCode()), ExceptionType.InvalidParameter, service.getProjects());
                log.warn("Registry is not supported.", ce);
                service.setProjects(null);
            }
        }

    	service.setServiceSeq(serviceSeq);
    	service.setUpdater(ContextHolder.exeContext().getUserSeq());
        serviceService.updateService(service, currService, apiVersion);

        if (StringUtils.isNotBlank(service.getUseYn()) && service.getUseYn().equals("N")) {
            serviceService.markServiceUnused(serviceSeq, ContextHolder.exeContext().getUserSeq());
            this.removeService(serviceSeq);
        }

        log.debug("[END  ] updateService");
        return service;
    }

    @PutMapping(value = "/{serviceSeq}/user")
    @Operation(summary = "워크스페이스안에 사용자 등록/수정/삭제", description = "칵테일 워크스페이스안에 사용자를 등록/수정/삭제")
    public ResultVO updateUsersOfService(
        @Parameter(description = "서비스 번호", required = true) @PathVariable Integer serviceSeq,
        @Parameter(description = "사용자 정보 모델", required = true) @RequestBody List<ServiceUserVO> serviceUsers
    ) throws Exception {
        log.debug("[BEGIN] updateUsersOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        serviceService.updateUsersOfService(serviceSeq, serviceUsers, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] updateUsersOfService");

        return new ResultVO();
    }

    @PostMapping(value = "/{serviceSeq}/user")
    @Operation(summary = "워크스페이스안에 사용자 등록", description = "칵테일 워크스페이스안에 사용자를 등록")
    public ResultVO addUserOfService(
            @Parameter(description = "서비스 번호", required = true) @PathVariable Integer serviceSeq,
            @Parameter(description = "사용자 정보 모델", required = true) @RequestBody ServiceUserVO serviceUser
    ) throws Exception {
        log.debug("[BEGIN] addUserOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        List<ServiceUserVO> userList = serviceService.getServiceUsers(serviceSeq);
        if(CollectionUtils.isNotEmpty(userList)){
            List<Integer> userSeqList = userList.stream().map(ServiceUserVO::getUserSeq).collect(Collectors.toList());
            if(userSeqList.contains(serviceUser.getUserSeq())){
                throw new CocktailException("User already exist", ExceptionType.UserAlreadyExists);
            }
            userList.add(serviceUser);

        } else {
            userList = new ArrayList<>();
            userList.add(serviceUser);
        }

        serviceService.updateUsersOfService(serviceSeq, userList, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] addUserOfService");

        return new ResultVO();
    }

    @PutMapping(value = "/{serviceSeq}/clusters")
    @Operation(summary = "워크스페이스에 클러스터를 할당", description = "워크스페이스안에 클러스터를 할당 처리한다.")
    public ResultVO updateClustersOfService(
        @Parameter(description = "서비스 번호", required = true) @PathVariable Integer serviceSeq,
        @Parameter(description = "클러스터 목록", required = true) @RequestBody List<Integer> clusterSeqs
    ) throws Exception {
        log.debug("[BEGIN] updateClustersOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        serviceService.updateClustersOfService(serviceSeq, clusterSeqs, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] updateClustersOfService");

        return new ResultVO();
    }

    @PutMapping(value = "/{serviceSeq}/servicemaps")
    @Operation(summary = "워크스페이스에 서비스맵 할당", description = "워크스페이스안에 서비스맵을 할당 처리한다.")
    public void updateServicemapsOfService(
        @Parameter(description = "서비스 번호", required = true) @PathVariable Integer serviceSeq,
        @Parameter(description = "서비스맵 목록", required = true) @RequestBody List<ServicemapGroupMappingAddVO> servicemaps
    ) throws Exception {
        log.debug("[BEGIN] updateServicemapsOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        serviceService.updateServicemapssOfService(serviceSeq, servicemaps, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] updateServicemapsOfService");
    }

    @PutMapping(value = "/{serviceSeq}/buildservers")
    @Operation(summary = "워크스페이스에 빌드서버 할당", description = "워크스페이스안에 빌드서버를 할당 처리한다.")
    public ResultVO updateBuildserversOfService(
        @Parameter(description = "서비스 번호", required = true) @PathVariable Integer serviceSeq,
        @Parameter(description = "빌드서버 목록", required = true) @RequestBody List<Integer> buildserverSeqs
    ) throws Exception {
        log.debug("[BEGIN] updateBuildserversOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        serviceService.updateBuildserversOfService(serviceSeq, buildserverSeqs, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] updateBuildserversOfService");

        return new ResultVO();
    }

	@DeleteMapping(value = "/{serviceSeq}")
    @Operation(summary = "워크스페이스 삭제", description = "칵테일 워크스페이스를 삭제한다.")
    public ServiceDetailVO removeService(
            @Parameter(description = "서비스번호", required = true) @PathVariable Integer serviceSeq
    		) throws Exception {
    	log.debug("[BEGIN] removeService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        //////////// START : 빌드 존재여부 체크, 클러스터 존재 유무 체크 및 registry 정보 삭제 추가, 2019.05.08, coolingi

        /** 서비스 정보 조회 */
        ServiceDetailVO currService = serviceService.getService(serviceSeq);

        /** 클러스터 정보 유무 체크, serviceService.removeService 내에 존재하는 클러스터 체크 로직을 여기로 옮김 */
        List<ClusterVO> clusters = currService.getClusters();
        List<ServicemapDetailResourceVO> servicemaps = currService.getServicemaps();

        if (CollectionUtils.isNotEmpty(servicemaps)) {
            String servicemapsName = servicemaps.stream().map(ServicemapDetailResourceVO::getServicemapName).collect(Collectors.joining(", "));
            throw new CocktailException(String.format("in use service map [%s]", servicemapsName), ExceptionType.ServiceHasAppmaps);
        }
        if (CollectionUtils.isNotEmpty(clusters)) {
            String clustersName = clusters.stream().map(ClusterVO::getClusterName).collect(Collectors.joining(", "));
            throw new CocktailException(String.format("in use clusters [%s]", clustersName), ExceptionType.ServiceHasClusters);
        }

        // harbor api client
//        ProductsApi productsApi = harborRegistryFactory.getProductsApiByAccountSeq(currService.getAccount().getAccountSeq());
        IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(currService.getAccount().getAccountSeq());

        /**
         * AccountType in (Cocktail, Apps) 일 경우에만 registry를 생성하도록 함
         */
        if (!currService.getAccount().getAccountType().isCubeEngine()) {

            List<ServiceRegistryVO> currProjects = Optional.ofNullable(currService).map(ServiceDetailVO::getProjects).orElseGet(() ->new ArrayList<>());
            Map<Integer, ServiceRegistryVO> currProjectIdMap = currProjects.stream().collect(Collectors.toMap(ServiceRegistryVO::getProjectId, Function.identity()));

            /**  image registry 를 사용하는 워크로드가 존재하는 지 체크 */
            if (CollectionUtils.isNotEmpty(currService.getProjects())) {
                boolean isExistsPipeline = pipelineFlowService.checkPipelineUsingServiceSeqAndRegistryIds(serviceSeq, Lists.newArrayList(currProjectIdMap.keySet()), null);

                if (isExistsPipeline) {
                    throw new CocktailException("이 워크스페이스의 이미지 레지스트리를 사용하는 워크로드가 존재하여 삭제될 수 없습니다. 워크로드를 먼저 중지 후, 삭제하거나 이미지를 다른 이미지로 교체해 주세요.", ExceptionType.WorkloadUsingImageRegistryExists);
                }
            }

            /**  build 정보 유무 체크 */
            // 'SERVICE'유형으로 생성된 project는 빌드를 삭제 후 삭제 가능
            List<Integer> projectIdList = currProjects.stream().filter(cp -> (cp.getProjectType() == ServiceRegistryType.SERVICE)).map(ServiceRegistryVO::getProjectId).collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(projectIdList)) { /** 빌드 체크는 ProjectIdList가 있을때만 하도록 함 (ProjectIdList가 null 이면 전체 Build가 조회됨) : 2020.06.17 **/
                List<BuildVO> builds = pipelineBuildService.getBuildList(currService.getAccount().getAccountSeq(), projectIdList);
                if (CollectionUtils.isNotEmpty(builds)) {
//            log.error("이 워크스페이스는 빌드를 포함하고 있어 삭제될 수 없습니다. 빌드를 먼저 삭제해 주세요."); // This workspace contains builds, can not be deleted. Please remove the builds first.
                throw new CocktailException("이 워크스페이스는 빌드를 포함하고 있어 삭제될 수 없습니다. 빌드를 먼저 삭제해 주세요.", ExceptionType.ServiceContainsBuild);
                }
            }

            //////////// END : 빌드 존재여부 체크 및 registry 정보 삭제 추가, 2019.05.08, coolingi



            /**
             *  Docker registry 사용자 & 프로젝트 삭제, 오류가 발생해도 사용자가 액션을 취할수 없음으로 로그만 찍는다.
             */
            // docker registry 접속 사용자 삭제, project 삭제
            try {
                if (StringUtils.isNotBlank(currService.getRegistryUserId())) {
                    // registry의 사용자 삭제
                    harborRegistryService.deleteUser(CryptoUtils.decryptAES(currService.getRegistryUserId()));
                }
            } catch (Exception eo) {
                log.error(eo.getMessage());
            }

            //////////// START : 빌드 존재여부 체크, 클러스터 존재 유무 체크 및 registry 정보 삭제 추가, 2019.05.08, coolingi

            // registry 에서 project 삭제, 현재는 service(workspace)별 아이디가 1개씩만 발급되므로 실제로는 한번만 looping 됨.
            try {
                for( Integer id : projectIdList ){
                    // registry server에서 project 삭제
                    harborRegistryService.deleteProject(id);
                    // 공유된 service registry 정보 삭제
                    List<ServiceRegistryVO> sharedRegistryInfos = serviceService.getServiceRegistryOfAccount(currService.getAccount().getAccountSeq(), ServiceRegistryType.SHARE.getCode(), id);
                    if (CollectionUtils.isNotEmpty(sharedRegistryInfos)) {
                        for (ServiceRegistryVO serviceRegistryRow : sharedRegistryInfos) {
                            serviceService.deleteProjectsOfService(serviceRegistryRow.getServiceSeq(), Arrays.asList(id));
                        }
                    }
                }
                // service registry 삭제
                serviceService.deleteProjectsOfService(serviceSeq, null);
            } catch (Exception eo) {
                log.error(eo.getMessage());
            }
            //////////// END : 빌드 존재여부 체크 및 registry 정보 삭제 추가, 2019.05.08, coolingi
        } else {
            /**
             *  Docker registry 사용자 & 프로젝트 삭제, 오류가 발생해도 사용자가 액션을 취할수 없음으로 로그만 찍는다.
             */
            // docker registry 접속 사용자 삭제, project 삭제
            try {
                if (StringUtils.isNotBlank(currService.getRegistryUserId())) {
                    // registry의 사용자 삭제
                    harborRegistryService.deleteUser(CryptoUtils.decryptAES(currService.getRegistryUserId()));
                }
            } catch (Exception eo) {
                log.error(eo.getMessage());
            }
        }


        // 워크스페이스 user 삭제
        serviceService.deleteUsersOfService(serviceSeq, null);
        // 워크스페이스 and Mapping 삭제
        int cnt = serviceService.removeServiceAndMapping(currService.getAccount().getAccountSeq(), serviceSeq, ContextHolder.exeContext().getUserSeq());
        // 외부 레지스트리 Mapping 삭제
        externalRegistryService.deleteExternalRegistryServiceMappingOfService(serviceSeq);

        // 해당 워크스페이스 유형의 스냅샷 삭제
        templateService.removeTemplateByService(currService.getAccount().getAccountSeq(), serviceSeq, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] removeService[{}]", cnt);

        ServiceDetailVO result = new ServiceDetailVO();
        result.setAccount(currService.getAccount());
        result.setServiceSeq(currService.getServiceSeq());
        result.setServiceName(currService.getServiceName());
        return result;
    }

	@GetMapping(value = "")
    @Operation(summary = "칵테일 워크스페이스 목록", description = "칵테일 워크스페이스의 목록을 반환한다.")
    public List<ServiceListVO> getServices(
            @Parameter(name = "includePlatform", description = "플랫폼 워크스페이스 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "includePlatform", required = false, defaultValue = "false") boolean includePlatform,
            @Parameter(name = "accountSeq", description = "플랫폼 번호") @RequestParam(name = "accountSeq", required = false) Integer accountSeq,
            @Parameter(name = "serviceSeq", description = "서비스 번호") @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq
    ) throws Exception {
    	log.debug("[BEGIN] getServices");

        /**
         * ADMIN이 아니면 Header로 수신되는 인증된 Workspace 값을 사용하여 조회하도록 처리.
         */
//        if(!AuthUtils.isAdminUser(_request)) {
//            serviceSeq = ctx.getUserServiceSeq();
//        }
//        log.debug("############### serviceSeq : " + serviceSeq);

        if (!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin() && accountSeq == null) {
            accountSeq = ContextHolder.exeContext().getUserAccountSeq();
        }
        List<ServiceListVO> result = serviceService.getServices(accountSeq, serviceSeq, ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserRole(), "Y", null);

        if(includePlatform) {
            log.debug("[END  ] getServices");
            return result;
        }
        List<ServiceListVO> excludePlatform = Optional.ofNullable(result).orElseGet(() ->Lists.newArrayList()).stream().filter(svc -> ServiceType.NORMAL.getCode().equals(Optional.ofNullable(svc.getServiceType()).orElseGet(() ->ServiceType.PLATFORM).getCode())).collect(Collectors.toList());
        log.debug("[END  ] getServices");
        return excludePlatform;
    }

	@GetMapping(value = "/{serviceSeq}")
    @Operation(summary = "워크스페이스 상세 정보", description = "칵테일 워크스페이스의 상세정보를 반환한다.")
    public ServiceVO getService (
    		@Parameter(description = "서비스번호", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "useNamespace", description = "서비스맵 :: k8s Namespace 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useNamespace", required = false, defaultValue = "false") boolean useNamespace,
            @Parameter(name = "useLimitRange", description = "서비스맵 :: k8s LimitRange 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useLimitRange", required = false, defaultValue = "false") boolean useLimitRange,
            @Parameter(name = "useResourceQuota", description = "서비스맵 :: k8s ResourceQuota 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useResourceQuota", required = false, defaultValue = "false") boolean useResourceQuota,
            @Parameter(name = "useNetworkPolicy", description = "서비스맵 :: k8s NetworkPolicy 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useNetworkPolicy", required = false, defaultValue = "false") boolean useNetworkPolicy
    ) throws Exception {
    	log.debug("[BEGIN] getService");

        ServiceVO service = serviceService.getService(serviceSeq, useNamespace, useLimitRange, useResourceQuota, useNetworkPolicy);

        log.debug("[END  ] getService");
        return service;
    }

    @GetMapping(value = "/summaries")
    @Operation(summary = "워크스페이스 요약 목록", description = "칵테일 워크스페이스의 목록(요약 정보)을 반환한다.")
    public List<ServiceSummaryVO> getServiceSummaries(
            HttpServletRequest _request,
            @Parameter(description = "사용여부", schema = @Schema(defaultValue = "Y")) @RequestParam(required = false, defaultValue = "Y") String useYn,
            @Parameter(name = "clusterState", description = "cluster state", schema = @Schema(allowableValues = {"RUNNING","STOPPED"})) @RequestParam(required = false) String clusterState,
            @Parameter(name = "useService", description = "useService 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useService", required = false, defaultValue = "false") boolean useService,
            @Parameter(name = "useIngressHost", description = "useIngressHost 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useIngressHost", required = false, defaultValue = "false") boolean useIngressHost,
            @Parameter(name = "useWorkload", description = "workload 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useWorkload", required = false, defaultValue = "false") boolean useWorkload
    ) throws Exception {
    	log.debug("[BEGIN] getServiceSummaries");

        ExecutingContextVO ctx = AuthUtils.getContext(_request);

//    	UserVO user = this.userService.getByUserSeq(_userSeq);
        Map<String, Object> params = new HashMap<>();
        params.put("user_seq", ContextHolder.exeContext().getUserSeq());
//        params.put("user_role", user.getRoles().get(0));
        params.put("user_role", ContextHolder.exeContext().getUserRole()); // Header에 존재하는 User-Role을 사용하도록 변경 2018.11.14
        params.put("useYn", useYn);
        params.put("clusterState", clusterState);
        params.put("useService", useService);
        params.put("useIngressHost", useIngressHost);
        params.put("useWorkload", useWorkload);
        if(ctx.getUserServiceSeq() != null && ctx.getUserServiceSeq() > 0) {
            params.put("serviceSeq", ctx.getUserServiceSeq());
        }

        List<ServiceSummaryVO> result = serviceService.getServiceSummaries(params);
        try {
            log.debug("Service summary:\n {}", JsonUtils.toJsonObject(result).getAsJsonArray().toString());
        } catch (Exception eo) {
            log.error("ServiceSummary result print error", eo);
        }

        log.debug("[END  ] getServiceSummaries");
        return result;
    }

	@GetMapping(value = "/{serviceSeq}/clusters")
	@Operation(summary = "워크스페이스에 속한 클러스터 목록", description = "지정한 워크스페이스에 속한 클러스터 목록을 반환한다.")
	public List<ClusterVO> getClustersOfService(
			@Parameter(description = "서비스번호", required = true) @PathVariable Integer serviceSeq) throws Exception {
		log.debug("[BEGIN] getClustersOfService");

		List<ClusterVO> clusters = clusterService.getClusters(null, serviceSeq);

        if (CollectionUtils.isNotEmpty(clusters)) {
            clusters.forEach(c -> {
                c.setApiSecret(null);
                c.setServerAuthData(null);
                c.setClientAuthData(null);
                c.setClientKeyData(null);
            });
        }

		log.debug("[END  ] getClustersOfService");
		return clusters;
	}

//    @PutMapping(value = "/{serviceSeq}/registry-user/password")
//    @Operation(summary = "레지스트리 사용자 암호 수정", description = "레지스트리 사용자 암호를 수정한다.")
//	public ResultVO changeRegistryUserPassword(
//            @RequestHeader(name = "user-id" ) Integer _userSeq,
//            @RequestHeader(name = "user-role" ) String _userRole,
//            @Parameter(description = "서비스번호", required = true) @PathVariable Integer serviceSeq,
//            @Parameter(description = "서비스생성 모델", required = true) @RequestBody ServiceAddVO service) throws Exception {
//	    ResultVO r = new ResultVO();
//        ServiceVO currService = this.getService(_userSeq, _userRole, serviceSeq);
//
//        this.registryService.changeUserPassword(CryptoUtils.decryptAES(currService.getRegistryUserId()), CryptoUtils.decryptAES(currService.getRegistryUserPassword()),
//                service.getRegistryUserPassword());
//
//        this.serviceService.changeRegistryUserPassword(service);
//        r.setMessage("password changed: ok");
//        return r;
//    }

//    @PutMapping(value = "/{serviceSeq}/registry-user/init")
//    @Operation(summary = "레지스트리 사용자 생성", description = "레지스트리 사용자가 없는 서비스에 레지스리 사용자 생성(임시).")
//    public ResultVO resetRegistryUser(
//            @RequestHeader(name = "user-id" ) Integer _userSeq,
//            @RequestHeader(name = "user-role" ) String _userRole,
//            @Parameter(description = "서비스번호", required = true) @PathVariable Integer serviceSeq,
//            @Parameter(description = "서비스생성 모델", required = true) @RequestBody ServiceAddVO service
//    ) throws Exception {
//        ResultVO r = new ResultVO();
//
//        ServiceVO currService = this.getService(_userSeq, _userRole, serviceSeq);
//        List<UserVO> users = new ArrayList<>();
//        UserVO user = new UserVO();
//        users.add(user);
//        if (StringUtils.isNoneEmpty(currService.getRegistryUserId())) {
//            user.setUserId(CryptoUtils.decryptAES(currService.getRegistryUserId()));
//
//            this.registryService.deleteUsersFromProjects(users, this.extractId(currService.getProjects()), false);
//            this.registryService.deleteUser(user.getUserId());
//        }
//
//        String regUserId = String.format("user-%s", Utils.shortUUID().toLowerCase());
//        String password = String.format("%s%s%s", RandomStringUtils.randomAlphabetic(3).toLowerCase(),
//                RandomStringUtils.randomAlphabetic(3).toUpperCase(), RandomStringUtils.randomNumeric(2));
//        service.setRegistryUserId(regUserId);
//        service.setRegistryUserPassword(password);
//
//        user.setUserId(regUserId);
//        user.setPassword(password);
//        user.setRoles(Arrays.asList("DEVOPS"));
//        user.setUserName(String.format("%s user", service.getServiceName()));
//
//        this.registryService.addUser(user);
//
//        users = new ArrayList<>();
//        user.setUserRole(HarborRegistryProjectMemberRole.DEVELOPER.getValueToString());
//        users.add(user);
//        this.registryService.addUsersToProjects(users, service.getProjectIds(), false);
//
//        service.setServiceSeq(serviceSeq);
//        service.setUpdater(_userSeq);
//        serviceService.updateService(service);
//
//        r.setMessage("reset registry user: ok");
//        return r;
//    }

//    /**
//     * build개선, removeBuildJob 메서드
//     * 중복된 코드가 존재해 메서드로 분리
//     *
//     * @since 2018.11.27
//     * @param buildJobSeq
//     * @param ctx
//     * @throws Exception
//     */
//    private void removeBuildJob(Integer buildJobSeq, ExecutingContextVO ctx) throws Exception {
//        // build개선, build service call START
//        BuildJobVO builds = buildService.getBuildJobs(buildJobSeq);
//        if(builds != null){
//            BuildParamVO buildParam = new BuildParamVO(BuildState.WILL_REMOVE, null, null, ctx);
//
//            buildJobService.runJob(JobRunType.REMOVE_ALL_BUILD, buildJobSeq, TaskType.BUILD, buildParam);
//        }else{
//            buildJobService.removeBuildJob(buildJobSeq, ctx.getUserSeq());
//        }
//        // build개선, build service call END
//    }

    /**
     * 접속한 세션 사용자의 워크스페이스(서비스)시퀀스 리스트를 반환<br/>
     * user-role에 따라 조회 쿼리를 달리 한다.
     09121-=`
     * @param _userSeq
     * @param _userRole
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/seqs")
    @Operation(summary = "사용자의 워크스페이스 Sequence 리스트", description = "접속한 세션 사용자의 워크스페이스 Sequence 목록을 반환한다.")
    public List<Integer> getServiceSeqsOfUser (
            @RequestHeader(name = "user-id" ) Integer _userSeq,
            @RequestHeader(name = "user-role" ) String _userRole) throws Exception {
        log.debug("[BEGIN] getServiceSeqsOfUser");

        List<Integer> serviceSeqs = null;

        // system, sysuser 권한일 경우와 그외 사용자일 경우 다르게 표시
        if (UserRole.valueOf(_userRole).isUserOfSystem()){
            serviceSeqs = serviceService.getServiceSeqsOfSystem(_userSeq);
        }else{
            serviceSeqs = serviceService.getServiceSeqsOfUser(_userSeq);
        }

        log.debug("[END  ] getServiceSeqsOfUser");
        return serviceSeqs;
    }


    @PostMapping("/{serviceSeq}/servicemap")
    @Operation(summary = "서비스맵 생성", description = "워크스페이스 안에 서비스맵을 생성한다")
    public ServicemapVO addServicemapOfService(
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemap", description = "servicemap 생성 모델") @Validated @RequestBody ServicemapAddVO servicemap
    ) throws Exception {

        log.debug("[BEGIN] addServicemapOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(servicemap.getClusterSeq());

        // check validation
        if(StringUtils.isNotBlank(servicemap.getNamespaceName())){
            if(!ResourceUtil.validNamespaceName(servicemap.getNamespaceName())){
                throw new CocktailException("Invalid namespaceName!!", ExceptionType.NamespaceNameInvalid);
            }
        }

        ServicemapVO servicemapReturn = serviceService.addServicemapOfService(serviceSeq, servicemap);

        log.debug("[END  ] addServicemapOfService");

        return servicemapReturn;
    }

    @PostMapping("/{serviceSeq}/servicemap/existnamespace")
    @Operation(summary = "Namespace를 서비스맵으로 등록", description = "기존에 존재하는 Namespace를 워크스페이스안에 서비스맵으로 등록")
    public ServicemapVO addServicemapOfServiceExistNamespace(
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemap", description = "servicemap 추가 모델") @Validated @RequestBody ServicemapAddVO servicemap
    ) throws Exception {

        log.debug("[BEGIN] addServicemapOfServiceExistNamespace");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(servicemap.getClusterSeq());

        // check validation
        if(StringUtils.isNotBlank(servicemap.getNamespaceName())){
            if(!ResourceUtil.validNamespaceName(servicemap.getNamespaceName())){
                throw new CocktailException("Invalid namespaceName!!", ExceptionType.NamespaceNameInvalid);
            }
        }

        ServicemapVO servicemapReturn = serviceService.addServicemapExistNamespaceOfService(serviceSeq, servicemap);

        log.debug("[END  ] addServicemapOfServiceExistNamespace");

        return servicemapReturn;
    }

    @PutMapping("/{serviceSeq}/servicemap/{servicemapSeq}")
    @Operation(summary = "워크스페이스에서 서비스맵 수정", description = "워크스페이스에서 서비스맵 수정한다.")
    public ServicemapModVO updateServicemapOfService(
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "servicemap", description = "서비스맵 수정 모델") @RequestBody ServicemapModVO servicemap
    ) throws Exception {

        log.debug("[BEGIN] updateServicemapOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        serviceService.updateServicemapOfService(serviceSeq, servicemap);

        log.debug("[END  ] updateServicemapOfService");

        return servicemap;
    }

    @PutMapping(value = "/{serviceSeq}/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
    @Operation(summary = "Cluster > namespace 정보 yaml 수정", description = "Cluster > namespace 정보 yaml로 수정한다.")
    public void udpateNamespaceOfServicemapByYaml(
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "clusterSeq", description = "Cluster 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(description = "deployYaml", required = true) @RequestBody K8sDeployYamlVO deployYaml
    ) throws Exception {

        log.debug("[BEGIN] udpateNamespaceOfServicemapByYaml");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        serviceService.udpateNamespaceOfServicemapByYaml(serviceSeq, clusterSeq, namespaceName, deployYaml);

        log.debug("[END  ] udpateNamespaceOfServicemapByYaml");

    }

    @DeleteMapping("/{serviceSeq}/servicemap/{servicemapSeq}")
    @Operation(summary = "워크스페이스에서 서비스맵 삭제", description = "워크스페이스에서 서비스맵 삭제")
    public ServicemapVO removeServicemapOfService(
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq
    ) throws Exception {

        log.debug("[BEGIN] removeServicemapOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        ServicemapVO servicemapReturn = serviceService.removeServicemapMappingOfService(serviceSeq, servicemapSeq, ContextHolder.exeContext());

        log.debug("[END  ] removeServicemapOfService");

        return servicemapReturn;
    }


    @PutMapping("/{serviceSeq}/servicemap/{servicemapSeq}/rename")
    @Operation(summary = "서비스맵 이름변경", description = "서비스맵의 이름을 변경한다.")
    public ResultVO renameServicemapOfService(
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "name", description = "name", required = true) @Size(min = 1, max = 50) @RequestParam(name = "name") String name
    ) throws Exception {
        log.debug("[BEGIN] renameServicemapOfService");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(serviceSeq));

        if ( StringUtils.isBlank(name) || (StringUtils.isNotBlank(name) && name.length() > 50) ) {
            throw new CocktailException("Servicemap name is invalid", ExceptionType.AppmapNameInvalid);
        }

        servicemapService.renameServicemap(servicemapSeq, name, ContextHolder.exeContext());

        log.debug("[END  ] renameServicemapOfService");

        return new ResultVO();
    }
}