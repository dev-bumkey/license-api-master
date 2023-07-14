package run.acloud.api.cserver.controller;

import com.google.common.collect.Lists;
import io.kubernetes.client.openapi.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.*;
import run.acloud.api.cserver.service.*;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.resource.enums.K8sApiVerType;
import run.acloud.api.resource.service.WorkloadResourceService;
import run.acloud.api.resource.vo.ComponentVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Workload", description = "서비스맵의 워크로드를 관리할 수 있는 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/server")
public class ServerController {
    @Autowired
    private ServerService serverService;

    @Autowired
    private ServerValidService serverValidService;

    @Autowired
    private ServerStateService serverStateService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ServicemapService servicemapService;

    @Operation(summary = "워크로드 생성", description = "워크로드를 생성 한다. (GUI)")
    @PostMapping("/{apiVersion}/deploy/{deployType}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName}/create")
    public ServerIntegrateVO addWorkload(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI}), required = true) @PathVariable String deployType,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName,
            @Parameter(name = "deployMode", description = "deployMode", schema = @Schema(allowableValues = {"SYNC","ASYNC"}, defaultValue = "SYNC")) @RequestParam(required = false, defaultValue = "SYNC") String deployMode,
            @Parameter(name = "serverParam", description = "serverParam") @RequestBody @Validated ServerIntegrateVO serverParam
    ) throws Exception {

        ServerIntegrateVO serverData = null;
        try {
            log.info(String.format("[BEGIN] addWorkload (%s) : %s - %s - %s", serverParam.getDeployType(), clusterSeq, namespaceName, workloadName));

            if (DeployType.valueOf(deployType) != DeployType.GUI) {
                throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
            }

            ExecutingContextVO ctx = ContextHolder.exeContext();
            HttpServletRequest request = Utils.getCurrentRequest();
            ResultVO result = new ResultVO();
            ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
            ctx.setResult(result);

            log.info("[PARAM] IN addWorkload - \n{}", JsonUtils.toGson(serverParam));
            if (DeployType.valueOf(deployType) == DeployType.GUI) {
                ServerGuiVO serverGui = (ServerGuiVO)serverParam;

                if (clusterSeq.equals(serverGui.getComponent().getClusterSeq()) && namespaceName.equals(serverGui.getComponent().getNamespaceName()) && workloadName.equals(serverGui.getComponent().getComponentName())) {
                    ClusterVO cluster = clusterService.getCluster(serverGui.getComponent().getClusterSeq());

                    /**
                     * cluster 상태 체크
                     */
                    clusterStateService.checkClusterState(serverGui.getComponent().getClusterSeq());

//                    if (serverGui.getServer().getHpa() != null) {
//                        if (serverGui.getServer().getHpa().getMinReplicas() == null) {
//                            serverGui.getServer().getHpa().setMinReplicas(serverGui.getServer().getComputeTotal());
//                        }
//                    }

                    /**
                     * api version 체크
                     */
                    serverValidService.checkServerApiVersion(serverGui.getServer().getWorkloadType(), serverGui.getServer().getWorkloadVersion(), serverGui.getComponent().getClusterSeq(), ctx);

                    /**
                     * server 생성
                     */
                    serverData = serverService.addWorkload(clusterSeq, namespaceName, workloadName, serverGui, DeployMode.valueOf(deployMode), ctx);

                } else {
                    throw new CocktailException("cluster, namespace, workloadName are different from serverParam.", ExceptionType.InvalidParameter);
                }

            }

            log.debug("[PARAM] OUT - \n{}", JsonUtils.toGson(serverParam));

        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(ExceptionType.ServerCreationFailOnPreparation.getExceptionPolicy().getMessage(), e, ExceptionType.ServerCreationFailOnPreparation);
        }finally {
            log.info("[END  ] addWorkload");
        }

        return serverData;
    }

    @Operation(summary = "워크로드 수정", description = "워크로드를 수정한다. (GUI, YAML)")
    @PutMapping("/{apiVersion}/deploy/{deployType}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName}/update")
    public ServerIntegrateVO updateWorkload(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI , DeployType.Names.YAML}), required = true) @PathVariable String deployType,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName,
            @Parameter(name = "deployMode", description = "deployMode", schema = @Schema(allowableValues = {"SYNC","ASYNC"}, defaultValue = "SYNC")) @RequestParam(required = false, defaultValue = "SYNC") String deployMode,
            @Parameter(name = "serverParam", description = "serverParam") @RequestBody @Validated ServerIntegrateVO serverParam
    ) throws Exception {

        ServerIntegrateVO serverData = null;
        try {
            log.info(String.format("[BEGIN] updateWorkload (%s) : %s - %s - %s", serverParam.getDeployType(), clusterSeq, namespaceName, workloadName));

            ExecutingContextVO ctx = ContextHolder.exeContext();
            HttpServletRequest request = Utils.getCurrentRequest();
            ResultVO result = new ResultVO();
            ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
            ctx.setResult(result);

            log.debug("[PARAM] IN - \n{}", JsonUtils.toGson(serverParam));

            if (DeployType.valueOf(deployType) == DeployType.GUI) {
                ServerGuiVO serverGui = (ServerGuiVO)serverParam;

                if (clusterSeq.equals(serverGui.getComponent().getClusterSeq()) && namespaceName.equals(serverGui.getComponent().getNamespaceName()) && workloadName.equals(serverGui.getComponent().getComponentName())) {
                    /**
                     * cluster 상태 체크
                     */
                    clusterStateService.checkClusterState(serverGui.getComponent().getClusterSeq());

//                    if (serverGui.getServer().getHpa() != null) {
//                        if (serverGui.getServer().getHpa().getMinReplicas() == null) {
//                            serverGui.getServer().getHpa().setMinReplicas(serverGui.getServer().getComputeTotal());
//                        }
//                    }

                    /**
                     * api version 체크
                     */
                    serverValidService.checkServerApiVersion(serverGui.getServer().getWorkloadType(), serverGui.getServer().getWorkloadVersion(), serverGui.getComponent().getClusterSeq(), ctx);

                    /**
                     * server 수정
                     */
                    serverData = serverService.updateWorkload(serverGui.getComponent().getClusterSeq(), serverGui.getComponent().getNamespaceName(), workloadName, serverParam, DeployMode.valueOf(deployMode), ctx);
                } else {
                    throw new CocktailException("cluster, namespace, workloadName are different from serverParam.", ExceptionType.InvalidParameter);
                }


            } else {
                ServerYamlVO serverYaml = (ServerYamlVO)serverParam;

                if (clusterSeq.equals(serverYaml.getClusterSeq()) && namespaceName.equals(serverYaml.getNamespaceName()) && workloadName.equals(serverYaml.getWorkloadName())) {
                    /**
                     * yaml validation
                     */
                    if (!serverValidService.checkWorkloadYaml(serverYaml.getNamespaceName(), WorkloadType.valueOf(serverYaml.getWorkloadType()), serverYaml.getWorkloadName(), serverYaml.getYaml(), new JSON())) {
                        throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
                    }

                    /**
                     * cluster 상태 체크
                     */
                    clusterStateService.checkClusterState(serverYaml.getClusterSeq());

                    /**
                     * api version 체크
                     */
                    serverValidService.checkServerApiVersion(serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), serverYaml.getClusterSeq(), ctx);

                    /**
                     * server 수정
                     */
                    serverData = serverService.updateWorkload(serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), workloadName, serverParam, DeployMode.valueOf(deployMode), ctx);
                } else {
                    throw new CocktailException("cluster, namespace, workloadName are different from serverParam.", ExceptionType.InvalidParameter);
                }

            }

            log.debug("[PARAM] OUT - \n{}", JsonUtils.toGson(serverParam));

        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(ExceptionType.ServerCreationFailOnPreparation.getExceptionPolicy().getMessage(), e, ExceptionType.ServerCreationFailOnPreparation);
        }finally {
            log.info("[END  ] updateWorkload");
        }

        return serverData;
    }

    @Operation(summary = "워크로드 재생성", description = "중지된 워크로드를 다시 생성 한다.")
    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName}/redeploy")
    public ServerIntegrateVO redeployWorkload(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName,
            @Parameter(name = "deployMode", description = "deployMode", schema = @Schema(allowableValues = {"SYNC","ASYNC"}, defaultValue = "SYNC")) @RequestParam(required = false, defaultValue = "SYNC") String deployMode
    ) throws Exception {

        ServerIntegrateVO serverData = null;
        try {
            log.info(String.format("[BEGIN] redeployWorkload : %s - %s - %s", clusterSeq, namespaceName, workloadName));

            ExecutingContextVO ctx = ContextHolder.exeContext();
            HttpServletRequest request = Utils.getCurrentRequest();
            ResultVO result = new ResultVO();
            ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
            ctx.setResult(result);

            ClusterVO cluster = clusterService.getCluster(clusterSeq);

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(cluster);

            // 콤포넌트 정보 조회
            ComponentVO component = serverService.getComponent(clusterSeq, namespaceName, workloadName);

            /**
             * 실행 가능항 상태인지 체크
             */
            this.checkServerStateProcess(component, "start", ctx);

            /**
             * server 재생성
             */
            serverData = serverService.redeployWorkload(clusterSeq, namespaceName, workloadName, component, DeployMode.valueOf(deployMode), ctx);

        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(ExceptionType.ServerCreationFailOnPreparation.getExceptionPolicy().getMessage(), e, ExceptionType.ServerCreationFailOnPreparation);
        }finally {
            log.info("[END  ] redeployWorkload");
        }

        return serverData;
    }

    @Operation(summary = "워크로드 중지", description = "워크로드를 중지 한다.")
    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName:.+}/terminate")
    public ServerIntegrateVO terminateWorkload(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"})) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName,
            @Parameter(name = "deployMode", description = "deployMode", schema = @Schema(allowableValues = {"SYNC","ASYNC"}, defaultValue = "SYNC")) @RequestParam(required = false, defaultValue = "SYNC") String deployMode
    ) throws Exception {

        ServerIntegrateVO serverData = null;
        try {
            log.debug("[BEGIN] terminateWorkload");

            ExecutingContextVO ctx = ContextHolder.exeContext();
            HttpServletRequest request = Utils.getCurrentRequest();
            ResultVO result = new ResultVO();
            ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
            ctx.setResult(result);

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(clusterSeq);

            // 콤포넌트 정보 조회
            ComponentVO currComponent = null;

            try {
                currComponent = serverService.getComponent(clusterSeq, namespaceName, workloadName);

                if (currComponent != null) {
                    /**
                     * 실행 가능항 상태인지 체크
                     */
                    this.checkServerStateProcess(currComponent, "stop", ctx);

                    /**
                     * 파이프라인 실행 여부 체크
                     */
                    serverValidService.checkPipelineOnRunning(currComponent);

                    /**
                     * 서버 종류
                     */
                    serverData = serverService.terminateWorkload(clusterSeq, namespaceName, workloadName, currComponent, DeployMode.valueOf(deployMode), ctx);
                } else {
                    // 종료는 어떤 상태에서도 가능하므로 action 검사는 하지 않는다.
                    componentService.updateComponentState(ctx
                            , clusterSeq, namespaceName, workloadName
                            , StateCode.STOPPED
                            , null);
                }
            }
            catch (CocktailException ce) {
                log.error("server termination fail.", ce);
                // 종료는 어떤 상태에서도 가능하므로 action 검사는 하지 않는다.
                componentService.updateComponentState(ctx
                        , clusterSeq, namespaceName, workloadName
                        , StateCode.STOPPED
                        , null);
            }
            catch (Exception e) {
                log.error("server termination fail.", e);
                // 종료는 어떤 상태에서도 가능하므로 action 검사는 하지 않는다.
                componentService.updateComponentState(ctx
                        , clusterSeq, namespaceName, workloadName
                        , StateCode.STOPPED
                        , null);
            }

        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(ExceptionType.ServerTerminationFailOnPreparation.getExceptionPolicy().getMessage(), e, ExceptionType.ServerTerminationFailOnPreparation);
        }finally {
            log.debug("[END  ] terminateWorkload");
        }

        return serverData;
    }

    @Operation(summary = "워크로드 삭제", description = "워크로드를 삭제 한다.")
    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName}/remove")
    public void removeWorkload(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"})) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName
    ) throws Exception {

        ServerIntegrateVO serverData;
        try {
            log.debug("[BEGIN] removeWorkload");

            ExecutingContextVO ctx = ContextHolder.exeContext();
            HttpServletRequest request = Utils.getCurrentRequest();
            ResultVO result = new ResultVO();
            ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
            ctx.setResult(result);

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(clusterSeq);

            try {
                // 콤포넌트 정보 조회
                ComponentVO component = serverService.getComponent(clusterSeq, namespaceName, workloadName);

                /**
                 * 실행 가능항 상태인지 체크
                 */
                this.checkServerStateProcess(component, "remove", ctx);
            } catch (CocktailException ce) {
                if (ce.getType() == ExceptionType.InvalidYamlData) {
                    /**
                     * 서버 제거
                     */
                    serverService.removeWorkload(clusterSeq, namespaceName, workloadName, ctx);
                } else {
                    throw ce;
                }
            } catch (Exception e) {
                throw e;
            }

            /**
             * 서버 제거
             */
            serverService.removeWorkload(clusterSeq, namespaceName, workloadName, ctx);

        } catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        } catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        } catch (Exception e) {
            throw new CocktailException(ExceptionType.ServerRemoveFailOnPreparation.getExceptionPolicy().getMessage(), e, ExceptionType.ServerRemoveFailOnPreparation);
        } finally {
            log.debug("[END  ] removeWorkload");
        }

    }

    @Operation(summary = "중지된 상태의 워크로드 설정 정보 수정", description = "워크로드 설정 정보 수정 (중지된 상태의 워크로드에 대해서만 동작)")
    @PutMapping("/{apiVersion}/deploy/{deployType}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName}/edit")
    public ServerIntegrateVO editWorkloadManifest(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI , DeployType.Names.YAML}), required = true) @PathVariable String deployType,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName,
            @Parameter(name = "serverParam", description = "serverParam") @RequestBody @Validated ServerIntegrateVO serverParam
    ) throws Exception {

        ServerIntegrateVO serverData = null;
        try {
            log.debug("[BEGIN] editWorkloadManifest");

            ExecutingContextVO ctx = ContextHolder.exeContext();
            HttpServletRequest request = Utils.getCurrentRequest();
            ResultVO result = new ResultVO();
            ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
            ctx.setResult(result);

            log.info("[PARAM] IN - \n{}", JsonUtils.toGson(serverParam));

            if (DeployType.valueOf(deployType) == DeployType.GUI) {
                ServerGuiVO serverGui = (ServerGuiVO)serverParam;

                if (clusterSeq.equals(serverGui.getComponent().getClusterSeq()) && namespaceName.equals(serverGui.getComponent().getNamespaceName()) && workloadName.equals(serverGui.getComponent().getComponentName())) {
//                    if (serverGui.getServer().getHpa() != null) {
//                        if (serverGui.getServer().getHpa().getMinReplicas() == null) {
//                            serverGui.getServer().getHpa().setMinReplicas(serverGui.getServer().getComputeTotal());
//                        }
//                    }

                    /**
                     * api version 체크
                     */
                    serverValidService.checkServerApiVersion(serverGui.getServer().getWorkloadType(), serverGui.getServer().getWorkloadVersion(), serverGui.getComponent().getClusterSeq(), ctx);

                } else {
                    throw new CocktailException("cluster, namespace, workloadName are different from serverParam.", ExceptionType.InvalidParameter);
                }

            } else {
                ServerYamlVO serverYaml = (ServerYamlVO)serverParam;

                if (clusterSeq.equals(serverYaml.getClusterSeq()) && namespaceName.equals(serverYaml.getNamespaceName()) && workloadName.equals(serverYaml.getWorkloadName())) {
                    /**
                     * api version 체크
                     */
                    serverValidService.checkServerApiVersion(serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), serverYaml.getClusterSeq(), ctx);

                } else {
                    throw new CocktailException("cluster, namespace, workloadName are different from serverParam.", ExceptionType.InvalidParameter);
                }

            }

            /**
             * 실행 가능항 상태인지 체크
             */
            ComponentVO component = serverService.getComponent(clusterSeq, namespaceName, workloadName);
            this.checkServerStateProcess(component, "edit", ctx);

            /**
             * server 수정
             */
            serverData = serverService.editWorkloadManifest(clusterSeq, namespaceName, workloadName, serverParam, ctx);

            log.info("[PARAM] OUT - \n{}", JsonUtils.toGson(serverParam));

        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.SERVER);
        }finally {
            log.debug("[END  ] editWorkloadManifest");
        }

        return serverData;
    }


    @Operation(summary = "워크로드 재시작", description = "워크로드의 pod를 재시작 한다.")
    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName}/restart")
    public void restartWorkload(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"})) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName
    ) throws Exception {
        log.debug("[BEGIN] restartWorkload");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        HttpServletRequest request = Utils.getCurrentRequest();
        ResultVO result = new ResultVO();
        ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
        ctx.setResult(result);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        // 콤포넌트 정보 조회
        ComponentVO component = serverService.getComponent(clusterSeq, namespaceName, workloadName);

        // 실행 가능항 상태인지 체크
        this.checkServerStateProcess(component, "restart", ctx);

        // hjchoi. 20230623 워크로드 재시작시 파이프라인 동작여부 체크 로직 주석
//        /**
//         * 파이프라인 실행 여부 체크
//         */
//        serverValidService.checkPipelineOnRunning(component);

//        /**
//         * Pod 유무 확인
//         */
//        WorkloadVO workload = serverService.getWorkloadResource(cluster, namespaceName, workloadName, Boolean.FALSE);
//        if(!(workload != null && CollectionUtils.isNotEmpty(workload.getPods()))){
//            throw new CocktailException("Pod is now exists.", ExceptionType.K8sPodNotFound);
//        }
//
//        /**
//         * Pod restart(pod 삭제)
//         */
//        List<String> labels = new ArrayList<>();
//        for (Map.Entry<String, String> labelEntryRow : workload.getPods().get(0).getDetail().getLabels().entrySet()) {
//            labels.add(String.format("%s=%s", labelEntryRow.getKey(), labelEntryRow.getValue()));
//        }
//        workloadResourceService.deleteCollectionNamespacedPod(cluster, namespaceName, null, String.join(",", labels), ctx);

        // rollout 방식으로 변경
        workloadResourceService.rolloutRestartWorkload(WorkloadType.valueOf(component.getWorkloadType()), cluster, namespaceName, workloadName);

        log.debug("[END  ] restartWorkload");

    }


    @Operation(summary = "Namespace안의 워크로드 상세 정보 조회", description = "Namespace 안에 존재하는 워크로드의 상세 정보를 응답한다. (칵테일 워크로드 기반)")
    @GetMapping("/{apiVersion}/deploy/{deployType}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName:.+}")
    public ServerIntegrateVO getWorkloadDetailByNamespace(
            @RequestHeader(name = "user-id" ) Integer userSeq,
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI , DeployType.Names.YAML}), required = true) @PathVariable String deployType,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadDetailByNamespace");

        ServerIntegrateVO serverIntegrate = serverService.getWorkloadDetailByNamespace(DeployType.valueOf(deployType), clusterSeq, namespaceName, workloadName);
        if (serverIntegrate != null && DeployType.valueOf(serverIntegrate.getDeployType()) == DeployType.GUI) {
            ServerGuiVO serverGui = (ServerGuiVO) serverIntegrate;
            serverGui.getComponent().setWorkloadManifest(null);
        }

        log.debug("[END  ] getWorkloadDetailByNamespace");

        return serverIntegrate;
    }

    @Operation(summary = "Namespace안의 워크로드 상세 정보 조회", description = "Namespace 안에 존재하는 워크로드의 상세 정보를 응답한다. (클러스터의 정보를 직접 조회)")
    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName:.+}")
    public ServerIntegrateVO getWorkloadDetailByNamespaceForCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "deployType",
            description = "deployType",
            schema = @Schema(allowableValues = {DeployType.Names.GUI , DeployType.Names.YAML}, defaultValue = DeployType.Names.YAML), required = false)
            @RequestParam(value = "deployType", required = false, defaultValue = DeployType.Names.YAML) String deployType,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
        @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadDetailByNamespaceForCluster");

        if(StringUtils.isBlank(deployType)) {
            deployType = DeployType.Names.YAML;
        }

        log.debug(deployType);

        ServerIntegrateVO serverIntegrate = serverService.getWorkloadDetailByNamespaceForCluster(DeployType.valueOf(deployType), clusterSeq, namespaceName, workloadName);

        log.debug("[END  ] getWorkloadDetailByNamespaceForCluster");

        return serverIntegrate;
    }

    @Operation(summary = "Namespace안의 워크로드들의 상태 조회", description = "Namespace 안에 존재하는 모든 워크로드의 상태를 응답한다. (by Cluster Sequence)")
    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workloads/state")
    public ServerStateVO getWorkloadsStateInNamespace(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadsStateInNamespace");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

        ServerStateVO serverStates = serverStateService.getWorkloadsStateInNamespace(clusterSeq, namespaceName, null, true, ContextHolder.exeContext());

        log.debug("[END  ] getWorkloadsStateInNamespace");

        return serverStates;
    }

    @Operation(summary = "워크로드 상태 조회 ", description = "워크로드의 상태를 조회한다. (by Cluster Sequence)")
    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName}/state")
    public ServerStateVO getWorkloadState(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadState");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

        ServerStateVO serverStates = serverStateService.getWorkloadsStateInNamespace(clusterSeq, namespaceName, workloadName, true, ContextHolder.exeContext());

        log.debug("[END  ] getWorkloadState");

        return serverStates;
    }

    @Operation(summary = "Namespace안의 워크로드들의 상태 조회 ", description = "Namespace 안에 존재하는 모든 워크로드들의 상태를 응답한다. (by Cluster Id)")
    @GetMapping("/{apiVersion}/cluster/id/{clusterId}/namespace/{namespaceName}/workloads/state")
    public ServerStateVO getWorkloadsStateInNamespaceById(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterId", description = "clusterId", required = true) @PathVariable String clusterId,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadsStateInNamespaceById");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterId);

        ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

        ServerStateVO serverStates = serverStateService.getWorkloadsStateInNamespace(clusterId, namespaceName, null, true, ContextHolder.exeContext());

        log.debug("[END  ] getWorkloadsStateInNamespaceById");

        return serverStates;
    }

    @Operation(summary = "워크로드 상태 조회 ", description = "워크로드의 상태를 조회한다. (by Cluster Id)")
    @GetMapping("/{apiVersion}/cluster/id/{clusterId}/namespace/{namespaceName}/workload/{workloadName}/state")
    public ServerStateVO getWorkloadStateById(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterId", description = "clusterId", required = true) @PathVariable String clusterId,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadStateById");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterId);

        ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

        ServerStateVO serverStates = serverStateService.getWorkloadsStateInNamespace(clusterId, namespaceName, workloadName, true, ContextHolder.exeContext());

        log.debug("[END  ] getWorkloadStateById");

        return serverStates;
    }

    @Operation(summary = "워크로드 유형 조회", description = "워크로드 유형 목록을 조회한다.")
    @GetMapping("/workloadTypes")
    public List<Map<String, Object>> getWorkloadTypes() throws Exception {
    	log.debug("[BEGIN] getWorkloadTypes");

    	List<Map<String, Object>> workloadTypes = new ArrayList<>();

    	List<WorkloadType> workloadTypesSupported = WorkloadType.getSupoortWorkloadTypes();
    	for(WorkloadType workloadTypeRow : workloadTypesSupported){
            Map<String, Object> workloadTypeMap = new HashMap<>();
            workloadTypeMap.put("workloadType", workloadTypeRow.getCode());
            workloadTypeMap.put("order", workloadTypeRow.getOrder());

            // WorkloadType에 해당하는 WorkloadVersionSet 조회
            List<WorkloadVersionSet> workloadVersionSets = WorkloadVersionSet.getWorkloadVersionSetByType(workloadTypeRow);
            if(CollectionUtils.isNotEmpty(workloadVersionSets)){
                List<Map<String, Object>> workloadVersions = new ArrayList<>();
                for(WorkloadVersionSet workloadVersionSetRow : workloadVersionSets){
                    Map<String, Object> workloadVersionMap = new HashMap<>();
                    workloadVersionMap.put("version", workloadVersionSetRow.getWorkloadVersion());

                    EnumSet<K8sApiVerType> k8sApiVerTypes = workloadVersionSetRow.getClusterVersion();
                    workloadVersionMap.put("clusterVersion", k8sApiVerTypes.isEmpty() ? k8sApiVerTypes : k8sApiVerTypes.stream().map(K8sApiVerType::getVersion).collect(Collectors.toList()));

                    workloadVersions.add(workloadVersionMap);
                }
                workloadTypeMap.put("workloadVersion", workloadVersions);
            }

    	    workloadTypes.add(workloadTypeMap);
        }


        log.debug("[END  ] getWorkloadTypes");

        return workloadTypes;
    }

    @Operation(summary = "워크로드 이동", description = "워크로드를 지정한 그룹으로 이동시킨다.")
    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName}/move")
    public ResultVO moveBetweenGroup(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
        @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName,
        @Parameter(description = "컴포넌트 이동 정보") @RequestBody @Validated ComponentOrderVO componentOrder) throws Exception
    {
        log.debug("[BEGIN] moveBetweenGroup");

        serverService.moveWorkload(clusterSeq, namespaceName, workloadName, componentOrder);

        log.debug("[END  ] moveBetweenGroup");

        return new ResultVO();
    }

    @Operation(summary = "워크로드의 podTemplate 조회", description = "워크로드 podTemplate 정보를 조회한다.")
    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/workload/podtemplates")
    public List<WorkloadPodTemplateVO> getWorkloadPodTemplates(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadPodTemplates");

        List<WorkloadPodTemplateVO> workloadPodTemplates = serverService.getWorkloadPodTemplates(clusterSeq, namespaceName);

        log.debug("[END  ] getWorkloadPodTemplates");

        return workloadPodTemplates;
    }


    /**
     * 각 Server API 호출타입에 따른 처리 가능여부 체크.<br>
     * 처리 불가능 할 경우 각 API에 따른 Exception 발생.<br>
     * <br>
     *     * 서버 액션별 가능 상태 정리<br>
     *     - 서버중지 : CREATING, STOPPING, STOPPED 외 상태시 가능<br>
     *     - 서버시작 : STOPPED 만 가능<br>
     *     - 서버재시작 : RUNNING 만 가능<br>
     *     - 서버제거 : STOPPED 만 가능<br>
     *
     * @param component 요청된 서버(Component) 정보
     * @param action Controller 에 요청된 action 타입 ("stop", "start", "restart", "remove")
     *
     */
    private void checkServerStateProcess(ComponentVO component, String action, ExecutingContextVO ctx) throws Exception{
        EnumSet<StateCode> CAN_NOT_TERMINATE_SET = EnumSet.of(StateCode.CREATING, StateCode.STOPPING, StateCode.STOPPED);
        EnumSet<StateCode> CAN_START_SET = EnumSet.of(StateCode.STOPPED);
        EnumSet<StateCode> CAN_RESTART_SET = EnumSet.of(StateCode.RUNNING, StateCode.RUNNING_PREPARE);
        EnumSet<StateCode> CAN_RESTART_SET_FOR_CRONJOB = EnumSet.of(StateCode.RUNNING, StateCode.RUNNING_PREPARE, StateCode.READY, StateCode.COMPLETED);
        EnumSet<StateCode> CAN_REMOVE_SET = EnumSet.of(StateCode.STOPPED);

        // 콤포넌트 정보 조회
        StateCode serverStateCode = StateCode.codeOf(component.getStateCode());

        boolean result = false;
        switch (action) {
            case "stop":
                result = CAN_NOT_TERMINATE_SET.contains(serverStateCode);
                if(result) {
                    throw new CocktailException("The server can not stop due to server state.", ExceptionType.ServerStopInvalidState);
                }
                break;
            case "start":
                result = CAN_START_SET.contains(serverStateCode);
                if(!result) throw new CocktailException("The server can not start due to server state.", ExceptionType.ServerStartInvalidState);
                break;
            case "restart":
                if (WorkloadType.valueOf(component.getWorkloadType()) == WorkloadType.CRON_JOB_SERVER) {
                    result = CAN_RESTART_SET_FOR_CRONJOB.contains(serverStateCode);
                } else {
                    result = CAN_RESTART_SET.contains(serverStateCode);
                }
                if(!result) throw new CocktailException("The server can not restart due to server state.", ExceptionType.ServerRestartInvalidState);
                break;
            case "remove":
                result = CAN_REMOVE_SET.contains(serverStateCode);
                if(!result) throw new CocktailException("The server can not remove due to server state.", ExceptionType.ServerRemoveInvalidState);
                break;
            case "edit":
                result = CAN_START_SET.contains(serverStateCode);
                if(!result) throw new CocktailException("The server can not edit due to server state.", ExceptionType.ServerEditInvalidState);
                break;
            default:
                break;
        }
    }

    @InHouse
    @Operation(summary = "서비스맵 안의 모든 워크로드를 STOP or 재시작", description = "서비스맵 안의 모든 워크로드를 STOP or 재시작")
    @PostMapping("/z/do/not/touch/{servicemapSeq}/control")
    public ServerStateVO controlWorkloadsInServicemap(
        @PathVariable Integer servicemapSeq,
        @Parameter(name = "isRemove", description = "command가 STOP일 경우 워크로드 중지 후 삭제 처리 실행 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "isRemove", required = false, defaultValue = "false") boolean isRemove,
        @Parameter(name = "accessKey", description = "이중인증키 (짱1)", required = true) @RequestParam(value = "accessKey", required = true) String accessKey,
        @Parameter(name = "command", description = "command", schema = @Schema(allowableValues = {"ReRUN","STOP"}), required = true) @RequestParam(required = true) String command) throws Exception
    {
        log.debug("[BEGIN] allStopWorkloadsInNamespace");
        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setUserSeq(ContextHolder.exeContext().getUserSeq());
        ctx.setApiVersionType(ApiVersionType.V2);

        if(!AuthUtils.isAdminUser(ContextHolder.exeContext())) {
            throw new CocktailException("Admin role required.", ExceptionType.NotAuthorizedToRequest);
        }

        if(!"cocktailWkd1!".equals(accessKey)) {
            throw new CocktailException("Invalid Access Key.", ExceptionType.NotAuthorizedToRequest);
        }

        ServerStateVO serverStates = servicemapService.getWorkloadsInNamespace(servicemapSeq, ctx);
        ClusterVO cluster = clusterService.getClusterOfServicemap(servicemapSeq);
        List<String> jobLogs = new ArrayList<>();
        for(ComponentVO component : Optional.ofNullable(serverStates.getComponents()).orElseGet(() ->Lists.newArrayList())) {
            // 콤포넌트 정보 조회
            ComponentVO currComponent = serverService.getComponent(cluster.getClusterSeq(), cluster.getNamespaceName(), component.getComponentName());
            if("ReRUN".equals(command)) {
                if(currComponent != null) {
                    try {
                        /**
                         * 실행 가능한 상태인지 체크
                         */
                        this.checkServerStateProcess(currComponent, "start", ctx);
                    }
                    catch (CocktailException ce) {
                        jobLogs.add("Could not Restart (Invalid State) : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                        continue;
                    }
                    catch (Exception ex) {
                        jobLogs.add("Could not Restart (Invalid State) : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                        continue;
                    }
                    /**
                     * server 재생성
                     */
                    serverService.redeployWorkload(cluster.getClusterSeq(), cluster.getNamespaceName(), component.getComponentName(), currComponent, DeployMode.ASYNC, ctx);
                    jobLogs.add("ReStart : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                }
            }
            else if("STOP".equals(command)) {
                if (currComponent != null) {
                    try {
                        /**
                         * 실행 가능항 상태인지 체크
                         */
                        this.checkServerStateProcess(currComponent, "stop", ctx);
                    }
                    catch (CocktailException ce) {
                        jobLogs.add("Can not Stopped (Invalid State) : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                        /**
                         * 서버 삭제 처리
                         */
                        if (isRemove && StringUtils.equals(component.getStateCode(), StateCode.STOPPED.getCode())) {
                            serverService.removeWorkload(cluster.getClusterSeq(), cluster.getNamespaceName(), component.getComponentName(), ctx);
                            jobLogs.add("Removed1 : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                        }
                        continue;
                    }
                    catch (Exception ex) {
                        jobLogs.add("Can not Stopped (Invalid State) : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                        /**
                         * 서버 삭제 처리
                         */
                        if (isRemove && StringUtils.equals(component.getStateCode(), StateCode.STOPPED.getCode())) {
                            serverService.removeWorkload(cluster.getClusterSeq(), cluster.getNamespaceName(), component.getComponentName(), ctx);
                            jobLogs.add("Removed1 : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                        }
                        continue;
                    }
                    try {
                        /**
                         * 파이프라인 실행 여부 체크
                         */
                        serverValidService.checkPipelineOnRunning(currComponent);
                    }
                    catch (CocktailException ce) {
                        jobLogs.add("Can not Stopped (Pipeline On running) : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                        continue;
                    }
                    catch (Exception ex) {
                        jobLogs.add("Can not Stopped (Pipeline On running) : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                        continue;
                    }
                    /**
                     * 서버 종료 (SYNC 처리)
                     */
                    serverService.terminateWorkload(cluster.getClusterSeq(), cluster.getNamespaceName(), component.getComponentName(), currComponent, DeployMode.SYNC, ctx);
                    jobLogs.add("Stopped1 : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                }
                else {
                    // 종료는 어떤 상태에서도 가능하므로 action 검사는 하지 않는다.
                    componentService.updateComponentState(ctx
                            , cluster.getClusterSeq(), cluster.getNamespaceName(), component.getComponentName()
                            , StateCode.STOPPED
                            , null);
                    jobLogs.add("Stopped2 : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                }

                /**
                 * 서버 삭제 처리
                 */
                if (isRemove) {
                    serverService.removeWorkload(cluster.getClusterSeq(), cluster.getNamespaceName(), component.getComponentName(), ctx);
                    jobLogs.add("Removed : " + cluster.getNamespaceName() + " : " + component.getComponentName());
                }
            }
        }
        log.debug("=================================================================");
        log.debug("Processing Result. ----------------------------------------------");
        log.debug(JsonUtils.toPrettyString(jobLogs));
        log.debug("-----------------------------------------------------------------");
        log.debug("=================================================================");
        log.debug("[END  ] allStopWorkloadsInNamespace");

        return serverStates;
    }
}