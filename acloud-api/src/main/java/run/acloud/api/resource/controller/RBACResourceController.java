package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.service.RBACResourceService;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Kubernetes RBAC Management", description = "쿠버네티스 RBAC에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/rbac")
@RestController
@Validated
public class RBACResourceController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private RBACResourceService rbacResourceService;



    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrole")
    @Operation(summary = "ClusterRole를 추가한다", description = "cluster에 속한 ClusterRole을 추가한다.")
    public K8sClusterRoleVO addClusterRole(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "clusterRoleSpec", description = "추가하려는 ClusterRole", required = true) @RequestBody ClusterRoleIntegrateVO clusterRoleSpec
    ) throws Exception {

        log.debug("[BEGIN] addClusterRole");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(clusterRoleSpec.getDeployType());
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

                ClusterRoleGuiVO gui = (ClusterRoleGuiVO) clusterRoleSpec;

                // valid
                rbacResourceService.checkClusterRole(cluster, true, gui);

                return rbacResourceService.createClusterRole(cluster, gui);
            } else {
                ClusterRoleYamlVO yaml = (ClusterRoleYamlVO) clusterRoleSpec;

                // valid 하기 위행 GUI로 변환
                ClusterRoleGuiVO gui = rbacResourceService.convertClusterRoleYamlToGui(cluster, yaml.getYaml());

                // valid
                rbacResourceService.checkClusterRole(cluster, true, gui);

                return rbacResourceService.createClusterRole(cluster, yaml.getYaml());
            }
        } finally {
            log.debug("[END  ] addClusterRole");
        }

    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrole/{clusterRoleName:.+}")
    @Operation(summary = "지정한 ClusterRole 수정", description = "지정한 ClusterRole을 수정한다.")
    public K8sClusterRoleVO updateClusterRole(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "clusterRoleName", description = "clusterRoleName", required = true) @PathVariable String clusterRoleName,
            @Parameter(name = "clusterRoleSpec", description = "수정하려는 clusterRoleSpec", required = true) @RequestBody ClusterRoleIntegrateVO clusterRoleSpec
    ) throws Exception {

        log.debug("[BEGIN] updateClusterRole");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(clusterRoleSpec.getDeployType());
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

        if (StringUtils.startsWithIgnoreCase(clusterRoleName, "system:")) {
            throw new CocktailException("You cannot edit cluster system resources.", ExceptionType.K8sClusterCannotEditSystemResource, "You cannot edit cluster system resources.");
        }

        try {
            if (deployType == DeployType.GUI) {
                ClusterRoleGuiVO gui = (ClusterRoleGuiVO) clusterRoleSpec;
                if(!clusterRoleName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the ClusterRole name. (ClusterRole name is different)", ExceptionType.K8sClusterRoleNameInvalid);
                }

                // valid
                rbacResourceService.checkClusterRole(cluster, false, gui);

                return rbacResourceService.patchClusterRole(cluster, gui, false);
            } else {
                ClusterRoleYamlVO yaml = (ClusterRoleYamlVO) clusterRoleSpec;
                if(!clusterRoleName.equals(yaml.getName())) {
                    throw new CocktailException("Can't change the ClusterRole name. (ClusterRole name is different)", ExceptionType.K8sClusterRoleNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                ClusterRoleGuiVO gui = rbacResourceService.convertClusterRoleYamlToGui(cluster, yaml.getYaml());
                if(!clusterRoleName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the ClusterRole name. (ClusterRole name is different)", ExceptionType.K8sClusterRoleNameInvalid);
                }

                // valid
                rbacResourceService.checkClusterRole(cluster, false, gui);

                return rbacResourceService.patchClusterRole(cluster, yaml.getYaml(), false);
            }
        } finally {
            log.debug("[END  ] updateClusterRole");
        }

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrole/{clusterRoleName:.+}")
    @Operation(summary = "지정한 Cluster에 속한 ClusterRole 상세 반환", description = "클러스터 안에 지정한 ClusterRole에 대한 상세 정보를 응답한다")
    public K8sClusterRoleVO getClusterRole(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "clusterRoleName", description = "clusterRoleName", required = true) @PathVariable String clusterRoleName
    ) throws Exception {

        log.debug("[BEGIN] getClusterRole");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sClusterRoleVO result = rbacResourceService.getClusterRole(cluster, clusterRoleName);

        log.debug("[END  ] getClusterRole");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/clusterroles")
    @Operation(summary = "지정한 cluster에 속한 ClusterRole 목록 반환", description = "지정한 클러스터안의 모든 ClusterRole 목록을 응답한다.")
    public List<K8sClusterRoleVO> getClusterRoles(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
    ) throws Exception {

        log.debug("[BEGIN] getClusterRoles");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);


        List<K8sClusterRoleVO> list = rbacResourceService.getClusterRoles(cluster, null, null);


        log.debug("[END  ] getClusterRoles");

        return list;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrole/{clusterRoleName:.+}")
    @Operation(summary = "지정한 ClusterRole 삭제", description = "지정한 ClusterRole을 삭제한다.")
    public void deleteClusterRole(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "clusterRoleName", description = "clusterRoleName", required = true) @PathVariable String clusterRoleName
    ) throws Exception {

        log.debug("[BEGIN] deleteClusterRole");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        rbacResourceService.checkClusterRoleName(cluster, false, clusterRoleName);

        if (StringUtils.startsWithIgnoreCase(clusterRoleName, "system:")) {
            throw new CocktailException("You cannot delete cluster system resources.", ExceptionType.K8sClusterCannotDeleteSystemResource, "You cannot delete cluster system resources.");
        }

        // 해당 clusterRole을 사용하고 있는 바인딩이 있다면 삭제 불가
        rbacResourceService.canDeleteRoleWithBinding(K8sApiKindType.CLUSTER_ROLE, cluster, clusterRoleName, null, true);

        rbacResourceService.deleteClusterRole(cluster, clusterRoleName);

        log.debug("[END  ] deleteClusterRole");

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrole/template")
    @Operation(summary = "ClusterRole Yaml Template 반환", description = "Yaml 형태의 ClusterRole Base Template을 반환한다.")
    public String getClusterRoleTemplate(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
    ) throws Exception {

        log.debug("[BEGIN] getClusterRoleTemplate");

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);


        String template = rbacResourceService.generateClusterRoleTemplate();


        log.debug("[END  ] getClusterRoleTemplate");

        return template;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrolebindings")
    @Operation(summary = "지정한 cluster에 속한 ClusterRoleBinding 목록 반환", description = "지정한 cluster에 속한 ClusterRoleBinding 목록을 응답한다.")
    public List<K8sClusterRoleBindingVO> getClusterRoleBindings(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
    ) throws Exception {

        log.debug("[BEGIN] getClusterRoleBindings");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        List<K8sClusterRoleBindingVO> list = rbacResourceService.getClusterRoleBindings(cluster, null, null);

        log.debug("[END  ] getClusterRoleBindings");

        return list;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrolebinding/{clusterRoleBindingName:.+}")
    @Operation(summary = "ClusterRoleBinding 상세 조회", description = "ClusterRoleBinding의 상세 정보를 응답한다.")
    public K8sClusterRoleBindingVO getClusterRoleBinding(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "clusterRoleBindingName", description = "clusterRoleBindingName", required = true) @PathVariable String clusterRoleBindingName
    ) throws Exception {

        log.debug("[BEGIN] getClusterRoleBinding");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sClusterRoleBindingVO k8sClusterRoleBindingVO = rbacResourceService.getClusterRoleBinding(cluster, clusterRoleBindingName);

        log.debug("[END  ] getClusterRoleBinding");

        return k8sClusterRoleBindingVO;

    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrolebinding/{clusterRoleBindingName:.+}")
    @Operation(summary = "지정한 ClusterRoleBinding 삭제", description = "지정한 ClusterRoleBinding을 삭제한다.")
    public void deleteClusterRoleBinding(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "clusterRoleBindingName", description = "clusterRoleBindingName", required = true) @PathVariable String clusterRoleBindingName
    ) throws Exception {

        log.debug("[BEGIN] deleteClusterRoleBinding");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        rbacResourceService.checkClusterRoleBindingName(cluster, false, clusterRoleBindingName);

        if (StringUtils.startsWithIgnoreCase(clusterRoleBindingName, "system:") || StringUtils.startsWithIgnoreCase(clusterRoleBindingName, "kubeadm:")) {
            throw new CocktailException("You cannot delete cluster system resources.", ExceptionType.K8sClusterCannotDeleteSystemResource, "You cannot delete cluster system resources.");
        }

        rbacResourceService.deleteClusterRoleBindingV1(cluster, clusterRoleBindingName);

        log.debug("[END  ] deleteClusterRoleBinding");

    }



    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrolebinding")
    @Operation(summary = "ClusterRoleBinding을 추가한다", description = "cluster에 속한 ClusterRoleBinding을 추가한다.")
    public K8sClusterRoleBindingVO addClusterRoleBinding(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "clusterRoleBindingSpec", description = "추가하려는 ClusterRoleBinding", required = true) @RequestBody ClusterRoleBindingIntegrateVO clusterRoleBindingSpec
    ) throws Exception {

        log.debug("[BEGIN] addClusterRoleBinding");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(clusterRoleBindingSpec.getDeployType());
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

                ClusterRoleBindingGuiVO gui = (ClusterRoleBindingGuiVO) clusterRoleBindingSpec;

                // valid
                rbacResourceService.checkClusterRoleBinding(cluster, true, gui);

                return rbacResourceService.createClusterRoleBinding(cluster, gui);
            } else {
                ClusterRoleBindingYamlVO yaml = (ClusterRoleBindingYamlVO) clusterRoleBindingSpec;

                // valid 하기 위행 GUI로 변환
                ClusterRoleBindingGuiVO gui = rbacResourceService.convertClusterRoleBindingYamlToGui(cluster, yaml.getYaml());

                // valid
                rbacResourceService.checkClusterRoleBinding(cluster, true, gui);

                return rbacResourceService.createClusterRoleBinding(cluster, yaml.getYaml());
            }
        } finally {
            log.debug("[END  ] addClusterRoleBinding");
        }

    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/clusterrolebinding/{clusterRoleBindingName:.+}")
    @Operation(summary = "지정한 ClusterRoleBinding 수정", description = "지정한 ClusterRoleBinding을 수정한다")
    public K8sClusterRoleBindingVO updateClusterRoleBinding(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "clusterRoleBindingName", description = "clusterRoleBindingName", required = true) @PathVariable String clusterRoleBindingName,
        @Parameter(name = "clusterRoleBindingSpec", description = "수정하려는 clusterRoleBindingSpec", required = true) @RequestBody ClusterRoleBindingIntegrateVO clusterRoleBindingSpec
    ) throws Exception {

        log.debug("[BEGIN] updateClusterRoleBinding");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(clusterRoleBindingSpec.getDeployType());
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

        if (StringUtils.startsWithIgnoreCase(clusterRoleBindingName, "system:")) {
            throw new CocktailException("You cannot edit cluster system resources.", ExceptionType.K8sClusterCannotEditSystemResource, "You cannot edit cluster system resources.");
        }

        try {
            if (deployType == DeployType.GUI) {
                ClusterRoleBindingGuiVO gui = (ClusterRoleBindingGuiVO) clusterRoleBindingSpec;
                if(!clusterRoleBindingName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the ClusterRoleBinding name. (ClusterRoleBinding name is different)", ExceptionType.K8sClusterRoleBindingNameInvalid);
                }

                // valid
                rbacResourceService.checkClusterRoleBinding(cluster, false, gui);

                return rbacResourceService.patchClusterRoleBinding(cluster, gui, false);
            } else {
                ClusterRoleBindingYamlVO yaml = (ClusterRoleBindingYamlVO) clusterRoleBindingSpec;
                if(!clusterRoleBindingName.equals(yaml.getName())) {
                    throw new CocktailException("Can't change the ClusterRoleBinding name. (ClusterRoleBinding name is different)", ExceptionType.K8sClusterRoleBindingNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                ClusterRoleBindingGuiVO gui = rbacResourceService.convertClusterRoleBindingYamlToGui(cluster, yaml.getYaml());
                if(!clusterRoleBindingName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the ClusterRoleBinding name. (ClusterRoleBinding name is different)", ExceptionType.K8sClusterRoleBindingNameInvalid);
                }

                // valid
                rbacResourceService.checkClusterRoleBinding(cluster, false, gui);

                return rbacResourceService.patchClusterRoleBinding(cluster, yaml.getYaml(), false);
            }
        } finally {
            log.debug("[END  ] updateClusterRoleBinding");
        }

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/roles")
    @Operation(summary = "지정한 cluster의 Namespace에 속한 Role 목록 반환", description = "클러스터의 네임스페이스안의 모든 Role 목록을 응답한다.")
    public List<K8sRoleVO> getRoles(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
    ) throws Exception {

        log.debug("[BEGIN] getRoles");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        List<K8sRoleVO> list = rbacResourceService.getRoles(cluster, namespace, null, null);

        log.debug("[END  ] getRoles");

        return list;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/role/{roleName:.+}")
    @Operation(summary = "role 상세 조회", description = "지정한 Role의 상세 정보를 응답한다.")
    public K8sRoleVO getRole(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
            @Parameter(name = "roleName", description = "roleName", required = true) @PathVariable String roleName
    ) throws Exception {

        log.debug("[BEGIN] getRole");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sRoleVO result = rbacResourceService.getRole(cluster, namespace, roleName);

        log.debug("[END  ] getRole");

        return result;

    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/role/{roleName:.+}")
    @Operation(summary = "지정한 Role 삭제", description = "지정한 Role을 삭제 한다.")
    public void deleteRole(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
            @Parameter(name = "roleName", description = "roleName", required = true) @PathVariable String roleName
    ) throws Exception {

        log.debug("[BEGIN] deleteRole");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        rbacResourceService.checkRoleName(cluster,false, namespace, roleName);

        if (StringUtils.startsWithIgnoreCase(roleName, "system:") || StringUtils.startsWithIgnoreCase(roleName, "kubeadm:")) {
            throw new CocktailException("You cannot delete cluster system resources.", ExceptionType.K8sClusterCannotDeleteSystemResource, "You cannot delete cluster system resources.");
        }

        // 해당 clusterRole을 사용하고 있는 바인딩이 있다면 삭제 불가
        rbacResourceService.canDeleteRoleWithBinding(K8sApiKindType.ROLE, cluster, roleName, namespace, true);

        rbacResourceService.deleteRole(cluster, namespace, roleName);

        log.debug("[END  ] deleteRole");

    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/role")
    @Operation(summary = "Role을 추가한다", description = "cluster의 namespace에 속한 Role을 추가한다.")
    public K8sRoleVO addRole(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
            @Parameter(name = "roleSpec", description = "추가하려는 Role", required = true) @RequestBody RoleIntegrateVO roleSpec
    ) throws Exception {

        log.debug("[BEGIN] addRole");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(roleSpec.getDeployType());
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

                RoleGuiVO gui = (RoleGuiVO) roleSpec;

                // valid
                rbacResourceService.checkRole(cluster, true, gui);

                return rbacResourceService.createRole(cluster, gui);
            } else {
                RoleYamlVO yaml = (RoleYamlVO) roleSpec;

                // valid 하기 위행 GUI로 변환
                RoleGuiVO gui = rbacResourceService.convertRoleYamlToGui(cluster, namespace, yaml.getYaml());

                if (gui == null) {
                    throw new CocktailException("Can't convert the Role. (It is null Gui Object to validate)", ExceptionType.K8sRoleNotFound);
                }

                // valid
                rbacResourceService.checkRole(cluster, true, gui);

                return rbacResourceService.createRole(cluster, namespace, yaml.getYaml());
            }
        } finally {
            log.debug("[END  ] addRole");
        }

    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/role/{roleName:.+}")
    @Operation(summary = "지정한 Role 수정", description = "지정한 Role을 수정 한다.")
    public K8sRoleVO updateRole(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
            @Parameter(name = "roleName", description = "roleBindingName", required = true) @PathVariable String roleName,
            @Parameter(name = "roleSpec", description = "수정하려는 roleSpec", required = true) @RequestBody RoleIntegrateVO roleSpec
    ) throws Exception {

        log.debug("[BEGIN] updateRole");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(roleSpec.getDeployType());
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

        if (StringUtils.startsWithIgnoreCase(roleName, "system:")) {
            throw new CocktailException("You cannot edit cluster system resources.", ExceptionType.K8sClusterCannotEditSystemResource, "You cannot edit cluster system resources.");
        }

        try {
            if (deployType == DeployType.GUI) {
                RoleGuiVO gui = (RoleGuiVO) roleSpec;
                if(!roleName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the Role name. (Role name is different)", ExceptionType.K8sRoleNameInvalid);
                }

                // valid
                rbacResourceService.checkRole(cluster, false, gui);

                return rbacResourceService.patchRole(cluster, gui, false);
            } else {
                RoleYamlVO yaml = (RoleYamlVO) roleSpec;
                if(!roleName.equals(yaml.getName())) {
                    throw new CocktailException("Can't change the Role name. (Role name is different)", ExceptionType.K8sRoleNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                RoleGuiVO gui = rbacResourceService.convertRoleYamlToGui(cluster, namespace, yaml.getYaml());
                if (gui == null) {
                    throw new CocktailException("Can't convert the Gui of Role. (It is null Gui Object to validate)", ExceptionType.K8sRoleNotFound);
                }
                if(!roleName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the Role name. (Role name is different)", ExceptionType.K8sRoleNameInvalid);
                }

                // valid
                rbacResourceService.checkRole(cluster, false, gui);

                return rbacResourceService.patchRole(cluster, namespace, yaml.getYaml(), false);
            }
        } finally {
            log.debug("[END  ] updateRole");
        }
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/rolebindings")
    @Operation(summary = "지정한 cluster의 Namespace에 속한 RoleBinding 목록 반환", description = "클러스터의 네임스페이스에 속한 모든 RoleBinding 목록을 응답한다.")
    public List<K8sRoleBindingVO> getRoleBindings(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
    ) throws Exception {

        log.debug("[BEGIN] getRoleBindings");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        List<K8sRoleBindingVO> list = rbacResourceService.getRoleBindings(cluster, namespace, null, null);

        log.debug("[END  ] getRoleBindings");

        return list;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/rolebinding/{roleBindingName:.+}")
    @Operation(summary = "roleBinding 상세 조회", description = "지정한 RoleBinding의 상세 정보를 응답한다.")
    public K8sRoleBindingVO getRoleBinding(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
        @Parameter(name = "roleBindingName", description = "roleBindingName", required = true) @PathVariable String roleBindingName
    ) throws Exception {

        log.debug("[BEGIN] getRoleBinding");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sRoleBindingVO k8sRoleBindingVO = rbacResourceService.getRoleBinding(cluster, namespace, roleBindingName);

        log.debug("[END  ] getRoleBinding");

        return k8sRoleBindingVO;

    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/rolebinding/{roleBindingName:.+}")
    @Operation(summary = "지정한 RoleBinding 삭제", description = "지정한 RoleBinding을 삭제 한다.")
    public void deleteRoleBinding(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
        @Parameter(name = "roleBindingName", description = "roleBindingName", required = true) @PathVariable String roleBindingName
    ) throws Exception {

        log.debug("[BEGIN] deleteRoleBinding");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        rbacResourceService.checkRoleBindingName(cluster,false, namespace, roleBindingName);

        if (StringUtils.startsWithIgnoreCase(roleBindingName, "system:") || StringUtils.startsWithIgnoreCase(roleBindingName, "kubeadm:")) {
            throw new CocktailException("You cannot delete cluster system resources.", ExceptionType.K8sClusterCannotDeleteSystemResource, "You cannot delete cluster system resources.");
        }

        rbacResourceService.deleteRoleBinding(cluster, namespace, roleBindingName);

        log.debug("[END  ] deleteRoleBinding");

    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/rolebinding")
    @Operation(summary = "RoleBinding을 추가한다", description = "cluster의 namespace에 속한 RoleBinding을 추가한다.")
    public K8sRoleBindingVO addRoleBinding(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
        @Parameter(name = "roleBindingSpec", description = "추가하려는 RoleBinding", required = true) @RequestBody RoleBindingIntegrateVO roleBindingSpec
    ) throws Exception {

        log.debug("[BEGIN] addRoleBinding");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(roleBindingSpec.getDeployType());
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

                RoleBindingGuiVO gui = (RoleBindingGuiVO) roleBindingSpec;

                // valid
                rbacResourceService.checkRoleBindingValidator(cluster, namespace, true, gui);

                return rbacResourceService.createRoleBinding(cluster, namespace, gui);
            } else {
                RoleBindingYamlVO yaml = (RoleBindingYamlVO) roleBindingSpec;

                // valid 하기 위행 GUI로 변환
                RoleBindingGuiVO gui = rbacResourceService.convertRoleBindingYamlToGui(yaml.getYaml());
                if (gui == null) {
                    throw new CocktailException("Can't convert the RoleBinding. (It is null Gui Object to validate)", ExceptionType.K8sRoleBindingNotFound);
                }

                // valid
                rbacResourceService.checkRoleBindingValidator(cluster, namespace, true, gui);

                return rbacResourceService.createRoleBinding(cluster, namespace, yaml.getYaml());
            }
        } finally {
            log.debug("[END  ] addRoleBinding");
        }

    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespace}/rolebinding/{roleBindingName:.+}")
    @Operation(summary = "지정한 RoleBinding 수정", description = "지정한 RoleBinding을 수정한다.")
    public K8sRoleBindingVO updateRoleBinding(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
        @Parameter(name = "roleBindingName", description = "roleBindingName", required = true) @PathVariable String roleBindingName,
        @Parameter(name = "roleBindingSpec", description = "수정하려는 roleBindingSpec", required = true) @RequestBody RoleBindingIntegrateVO roleBindingSpec
    ) throws Exception {

        log.debug("[BEGIN] updateRoleBinding");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(roleBindingSpec.getDeployType());
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

        if (StringUtils.startsWithIgnoreCase(roleBindingName, "system:")) {
            throw new CocktailException("You cannot edit cluster system resources.", ExceptionType.K8sClusterCannotEditSystemResource, "You cannot edit cluster system resources.");
        }

        try {
            if (deployType == DeployType.GUI) {
                RoleBindingGuiVO gui = (RoleBindingGuiVO) roleBindingSpec;
                if (gui == null) {
                    throw new CocktailException("Can't convert the RoleBinding. (It is null Gui Object to validate)", ExceptionType.K8sRoleBindingNameInvalid);
                }
                if(!roleBindingName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the RoleBinding name. (RoleBinding name is different)", ExceptionType.K8sRoleBindingNameInvalid);
                }

                // valid
                rbacResourceService.checkRoleBindingValidator(cluster, namespace, false, gui);

                return rbacResourceService.patchRoleBinding(cluster, namespace, gui, false);
            } else {
                RoleBindingYamlVO yaml = (RoleBindingYamlVO) roleBindingSpec;
                if(!roleBindingName.equals(yaml.getName())) {
                    throw new CocktailException("Can't change the RoleBinding name. (RoleBinding name is different)", ExceptionType.K8sRoleBindingNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                RoleBindingGuiVO gui = rbacResourceService.convertRoleBindingYamlToGui(yaml.getYaml());
                if (gui == null) {
                    throw new CocktailException("Can't change the RoleBinding name. (It is null Gui Object to validate)", ExceptionType.K8sRoleBindingNameInvalid);
                }
                if(!roleBindingName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the RoleBinding name. (RoleBinding name is different)", ExceptionType.K8sRoleBindingNameInvalid);
                }

                // valid
                rbacResourceService.checkRoleBindingValidator(cluster, namespace, false, gui);

                return rbacResourceService.patchRoleBinding(cluster, namespace, yaml.getYaml(), false);
            }
        } finally {
            log.debug("[END  ] updateRoleBinding");
        }
    }



}
