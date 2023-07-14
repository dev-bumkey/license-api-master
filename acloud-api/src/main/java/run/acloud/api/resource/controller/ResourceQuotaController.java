package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.service.ResourceQuotaService;
import run.acloud.api.resource.vo.K8sResourceQuotaVO;
import run.acloud.api.resource.vo.ResourceQuotaGuiVO;
import run.acloud.api.resource.vo.ResourceQuotaIntegrateVO;
import run.acloud.api.resource.vo.ResourceQuotaYamlVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Kubernetes ResourceQuota Management", description = "쿠버네티스 ResourceQuota에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/rq")
@RestController
@Validated
public class ResourceQuotaController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ResourceQuotaService resourceQuotaService;



    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
    @Operation(summary = "ResourceQuota를 추가한다", description = "클러스터의 네임스페이스에 ResourceQuota를 추가한다.")
    public K8sResourceQuotaVO addResourceQuota(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "resourceQuotaSpec", description = "추가하려는 ResourceQuota", required = true) @RequestBody ResourceQuotaIntegrateVO resourceQuotaSpec
    ) throws Exception {

        log.debug("[BEGIN] addResourceQuota");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(resourceQuotaSpec.getDeployType());
        }
        catch (IllegalArgumentException iae) {
            throw new CocktailException("DeployType invalid.", iae, ExceptionType.InvalidParameter);
        }
        catch (Exception e) {
            throw new CocktailException("DeployType invalid.", e, ExceptionType.InvalidParameter);
        }

        if (deployType != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        } else {
            try {
                ClusterVO cluster = clusterService.getCluster(clusterSeq);

                /**
                 * cluster 상태 체크
                 */
                clusterStateService.checkClusterState(cluster);

                ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
                ContextHolder.exeContext().setApiVersionType(apiVersionType);

                ResourceQuotaGuiVO gui = (ResourceQuotaGuiVO) resourceQuotaSpec;

                // valid
                resourceQuotaService.checkResourceQuota(cluster, namespaceName, true, gui);

                return resourceQuotaService.createResourceQuota(cluster, gui, ContextHolder.exeContext());
            } finally {
                log.debug("[END  ] addResourceQuota");
            }
        }
    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/default")
    @Operation(summary = "기본 ResourceQuota를 추가한다", description = "클러스터의 네임스페이스에 기본 ResourceQuota를 추가한다.")
    public K8sResourceQuotaVO addDefaultResourceQuota(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] addDefaultResourceQuota");

        K8sResourceQuotaVO result = null;

        try {
            ClusterVO cluster = clusterService.getCluster(clusterSeq);

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(cluster);

            // 기본 resourceQuota 생성
            resourceQuotaService.createDefaultResourceQuota(cluster, namespaceName);

            String label = String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
            List<K8sResourceQuotaVO> list = resourceQuotaService.getResourceQuotas(cluster, namespaceName, null, label, ContextHolder.exeContext());
            if (CollectionUtils.isNotEmpty(list)) {
                result = list.get(0);
            }
        } finally {
            log.debug("[END  ] addDefaultResourceQuota");
        }

        return result;
    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/resourcequota/{resourceQuotaName:.+}")
    @Operation(summary = "지정한 ResourceQuota 수정", description = "클러스터의 네임스페이스에 설정한 ResourceQuota를 수정한다.")
    public K8sResourceQuotaVO updateResourceQuota(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "resourceQuotaName", description = "resourceQuotaName", required = true) @PathVariable String resourceQuotaName,
            @Parameter(name = "resourceQuotaSpec", description = "수정하려는 resourceQuotaSpec", required = true) @RequestBody ResourceQuotaIntegrateVO resourceQuotaSpec
    ) throws Exception {

        log.debug("[BEGIN] updateResourceQuota");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(resourceQuotaSpec.getDeployType());
        }
        catch (IllegalArgumentException iae) {
            throw new CocktailException("DeployType invalid.", iae, ExceptionType.InvalidParameter);
        }
        catch (Exception e) {
            throw new CocktailException("DeployType invalid.", e, ExceptionType.InvalidParameter);
        }

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        if (cluster == null){
            throw new CocktailException("Cluster invalid.", ExceptionType.InvalidParameter);
        }

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        try {
            if (deployType == DeployType.GUI) {
                ResourceQuotaGuiVO gui = (ResourceQuotaGuiVO) resourceQuotaSpec;
                if (gui == null) {
                    throw new CocktailException("Can't convert the ResourceQuota. (It is null Gui Object to validate)", ExceptionType.K8sResourceQuotaNotFound);
                }
                if(!resourceQuotaName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the ResourceQuota name. (ResourceQuota name is different)", ExceptionType.K8sResourceQuotaNameInvalid);
                }

                // valid
                resourceQuotaService.checkResourceQuota(cluster, namespaceName, false, gui);

                return resourceQuotaService.patchResourceQuota(cluster, gui, false, ContextHolder.exeContext());
            } else {
                ResourceQuotaYamlVO yaml = (ResourceQuotaYamlVO) resourceQuotaSpec;
                if(!resourceQuotaName.equals(yaml.getName())) {
                    throw new CocktailException("Can't change the ResourceQuota name. (ResourceQuota name is different)", ExceptionType.K8sResourceQuotaNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                ResourceQuotaGuiVO gui = resourceQuotaService.convertYamlToGui(cluster, yaml.getYaml());
                if (gui == null) {
                    throw new CocktailException("Can't convert the ResourceQuota. (It is null Gui Object to validate)", ExceptionType.K8sResourceQuotaNotFound);
                }
                if(!resourceQuotaName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the ResourceQuota name. (Ingress ResourceQuota is different)", ExceptionType.K8sResourceQuotaNameInvalid);
                }

                // valid
                resourceQuotaService.checkResourceQuota(cluster, namespaceName, false, gui);

                return resourceQuotaService.patchResourceQuota(cluster, yaml.getYaml(), false, ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] updateResourceQuota");
        }

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/resourcequota/{resourceQuotaName:.+}")
    @Operation(summary = "지정한 ResourceQuota 상세 조회", description = "클러스터의 네임스페이스에 설정한 ResourceQuota의 상세 정보를 조회한다.")
    public K8sResourceQuotaVO getResourceQuota(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "resourceQuotaName", description = "resourceQuotaName", required = true) @PathVariable String resourceQuotaName
    ) throws Exception {

        log.debug("[BEGIN] getResourceQuota");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sResourceQuotaVO result = resourceQuotaService.getResourceQuota(cluster, namespaceName, resourceQuotaName, ContextHolder.exeContext());

        log.debug("[END  ] getResourceQuota");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/resourcequota/default")
    @Operation(summary = "기본 ResourceQuota 상세 조회", description = "클러스터의 네임스페이스에 설정한 기본 ResourceQuota의 상세 정보를 조회한다.")
    public K8sResourceQuotaVO getDefaultResourceQuota(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getDefaultResourceQuota");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sResourceQuotaVO result = resourceQuotaService.getDefaultResourceQuota(cluster, namespaceName);

        log.debug("[END  ] getDefaultResourceQuota");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/resourcequotas")
    @Operation(summary = "ResourceQuota 목록 조회", description = "클러스터의 네임스페이스에 설정한 ResourceQuota 목록을 조회한다.")
    public List<K8sResourceQuotaVO> getResourceQuotas(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getResourceQuotas");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);


        List<K8sResourceQuotaVO> list = resourceQuotaService.getResourceQuotas(cluster, namespaceName, null, null, ContextHolder.exeContext());


        log.debug("[END  ] getResourceQuotas");

        return list;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/resourcequota/{resourceQuotaName:.+}")
    @Operation(summary = "ResourceQuota 삭제", description = "지정한 ResourceQuota 를 삭제한다.")
    public void deleteResourceQuota(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "resourceQuotaName", description = "resourceQuotaName", required = true) @PathVariable String resourceQuotaName
    ) throws Exception {

        log.debug("[BEGIN] deleteResourceQuota");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        resourceQuotaService.deleteResourceQuota(cluster, namespaceName, resourceQuotaName, ContextHolder.exeContext());

        log.debug("[END  ] deleteResourceQuota");

    }

}
