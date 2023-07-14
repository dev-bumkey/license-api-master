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
import run.acloud.api.resource.service.LimitRangeService;
import run.acloud.api.resource.vo.K8sLimitRangeVO;
import run.acloud.api.resource.vo.LimitRangeGuiVO;
import run.acloud.api.resource.vo.LimitRangeIntegrateVO;
import run.acloud.api.resource.vo.LimitRangeYamlVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Kubernetes LimitRange Management", description = "쿠버네티스 LimitRange에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/lr")
@RestController
@Validated
public class LimitRangeController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private LimitRangeService limitRangeService;



    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
    @Operation(summary = "LimitRange를 추가한다", description = "클러스터 속한 LimitRange를 추가한다.")
    public K8sLimitRangeVO addLimitRange(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "limitRangeSpec", description = "추가하려는 LimitRange", required = true) @RequestBody LimitRangeIntegrateVO limitRangeSpec
    ) throws Exception {

        log.debug("[BEGIN] addLimitRange");

        if (DeployType.valueOf(limitRangeSpec.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }

        try {
            if (DeployType.valueOf(limitRangeSpec.getDeployType()) == DeployType.GUI) {

                ClusterVO cluster = clusterService.getCluster(clusterSeq);

                /**
                 * cluster 상태 체크
                 */
                clusterStateService.checkClusterState(cluster);

                ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
                ContextHolder.exeContext().setApiVersionType(apiVersionType);

                LimitRangeGuiVO gui = (LimitRangeGuiVO) limitRangeSpec;

                // valid
                limitRangeService.checkLimitRange(cluster, namespaceName, true, gui);

                return limitRangeService.createLimitRange(cluster, gui, ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] addLimitRange");
        }

        return null;
    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/default")
    @Operation(summary = "기본 LimitRange를 추가한다", description = "cluster에 속한 기본 LimitRange를 추가한다.")
    public K8sLimitRangeVO addDefaultLimitRange(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] addDefaultLimitRange");

        K8sLimitRangeVO result = null;

        try {
            ClusterVO cluster = clusterService.getCluster(clusterSeq);

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(cluster);

            // 기본 limitRange 생성
            limitRangeService.createDefaultLimitRange(cluster, namespaceName);

            String label = String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
            List<K8sLimitRangeVO> list = limitRangeService.getLimitRanges(cluster, namespaceName, null, label, ContextHolder.exeContext());
            if (CollectionUtils.isNotEmpty(list)) {
                result = list.get(0);
            }
        } finally {
            log.debug("[END  ] addDefaultLimitRange");
        }

        return result;
    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/limitrange/{limitRangeName:.+}")
    @Operation(summary = "지정한 LimitRange 수정", description = "클러스터의 네임스페이스안에 지정된 LimitRange를 수정한다")
    public K8sLimitRangeVO updateLimitRange(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "limitRangeName", description = "limitRangeName", required = true) @PathVariable String limitRangeName,
            @Parameter(name = "limitRangeSpec", description = "수정하려는 limitRangeSpec", required = true) @RequestBody LimitRangeIntegrateVO limitRangeSpec
    ) throws Exception {

        log.debug("[BEGIN] updateLimitRange");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(limitRangeSpec.getDeployType());
        }
        catch (IllegalArgumentException iae) {
            throw new CocktailException("DeployType invalid.", iae, ExceptionType.InvalidParameter);
        }
        catch (Exception e) {
            throw new CocktailException("DeployType invalid.", e, ExceptionType.InvalidParameter);
        }

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        try {
            if (deployType == DeployType.GUI) {
                LimitRangeGuiVO gui = (LimitRangeGuiVO) limitRangeSpec;
                if(!limitRangeName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the LimitRange name. (LimitRange name is different)", ExceptionType.K8sLimitRangeNameInvalid);
                }

                // valid
                limitRangeService.checkLimitRange(cluster, namespaceName, false, gui);

                return limitRangeService.patchLimitRange(cluster, gui, false, ContextHolder.exeContext());
            } else {
                LimitRangeYamlVO yaml = (LimitRangeYamlVO) limitRangeSpec;
                if(!limitRangeName.equals(yaml.getName())) {
                    throw new CocktailException("Can't change the LimitRange name. (LimitRange name is different)", ExceptionType.K8sLimitRangeNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                LimitRangeGuiVO gui = limitRangeService.convertYamlToGui(cluster, yaml.getYaml());
                if (gui == null) {
                    throw new CocktailException("Can't convert the LimitRange. (It is null Gui Object to validate)", ExceptionType.K8sLimitRangeNotFound);
                }
                if(!limitRangeName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the LimitRange name. (Ingress LimitRange is different)", ExceptionType.K8sLimitRangeNameInvalid);
                }

                // valid
                limitRangeService.checkLimitRange(cluster, namespaceName, false, gui);

                return limitRangeService.patchLimitRange(cluster, yaml.getYaml(), false, ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] updateLimitRange");
        }

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/limitrange/{limitRangeName:.+}")
    @Operation(summary = "LimitRange 상세 반환", description = "클러스터의 네임스페이스안에 지정된 LimitRange의 상세 정보를 응답한다.")
    public K8sLimitRangeVO getLimitRange(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "limitRangeName", description = "limitRangeName", required = true) @PathVariable String limitRangeName
    ) throws Exception {

        log.debug("[BEGIN] getLimitRange");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sLimitRangeVO result = limitRangeService.getLimitRange(cluster, namespaceName, limitRangeName, ContextHolder.exeContext());

        log.debug("[END  ] getLimitRange");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/limitrange/default")
    @Operation(summary = "기본 LimitRange 상세 반환", description = "클러스터의 네임스페이스에 지정된 기본 LimitRange 정보를 찾아서 응답한다.")
    public K8sLimitRangeVO getDefaultLimitRange(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getDefaultLimitRange");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sLimitRangeVO result = limitRangeService.getDefaultLimitRange(cluster, namespaceName);

        log.debug("[END  ] getDefaultLimitRange");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/limitranges")
    @Operation(summary = "지정한 cluster에 속한 LimitRange 목록 반환", description = "클러스터의 네임스페이스안의 전체 LimitRange 목록을 응답한다.")
    public List<K8sLimitRangeVO> getLimitRanges(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getNetworkPolicies");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);


        List<K8sLimitRangeVO> list = limitRangeService.getLimitRanges(cluster, namespaceName, null, null, ContextHolder.exeContext());


        log.debug("[END  ] getNetworkPolicies");

        return list;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/limitrange/{limitRangeName:.+}")
    @Operation(summary = "지정한 LimitRange 삭제", description = "클러스터의 네임스페이스안에 지정된 LimitRange를 삭제 한다.")
    public void deleteLimitRange(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "limitRangeName", description = "limitRangeName", required = true) @PathVariable String limitRangeName
    ) throws Exception {

        log.debug("[BEGIN] deleteLimitRange");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        limitRangeService.deleteLimitRange(cluster, namespaceName, limitRangeName, ContextHolder.exeContext());

        log.debug("[END  ] deleteLimitRange");

    }

}
