package run.acloud.api.resource.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PolicyRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.models.ExtensionsV1beta1PodSecurityPolicy;
import run.acloud.api.k8sextended.models.V1beta1PodSecurityPolicy;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PodSecurityPolicyService {

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private RBACResourceService rbacResourceService;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;


    /**
     * K8S PodSecurityPolicy 생성
     *
     * @param cluster
     * @param pspGui
     * @param context
     * @return
     * @throws Exception
     */
    public K8sPodSecurityPolicyVO createPodSecurityPolicy(ClusterVO cluster, PodSecurityPolicyGuiVO pspGui, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        // k8s 1.14 부터 추가된 Policy group의 PodSecurityPolicy를 기본으로 생성함
        if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {

            V1beta1PodSecurityPolicy psp = K8sSpecFactory.buildPspPolicyV1beta1(pspGui);
            k8sWorker.createPodSecurityPolicyPolicyV1beat1(cluster, psp);
            Thread.sleep(100);

        } else {
            ExtensionsV1beta1PodSecurityPolicy psp = K8sSpecFactory.buildPspExtensionsV1beta1(pspGui);
            k8sWorker.createPodSecurityPolicyExtensionsV1beat1(cluster, psp);
            Thread.sleep(100);

        }

        return this.getPodSecurityPolicy(cluster, pspGui.getName(), context);
    }

    /**
     * PodSecurityPolicy 생성 (yaml)
     *
     * @param cluster
     * @param yamlStr
     * @param context
     * @throws Exception
     */
    public void createPodSecurityPolicy(ClusterVO cluster, String yamlStr, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        Map<String, Object> k8sObjectToMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(k8sObjectToMap);
        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
        K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
        if (apiKindType == K8sApiKindType.POD_SECURITY_POLICY) {
            if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
                V1beta1PodSecurityPolicy createPsp = Yaml.loadAs(yamlStr, V1beta1PodSecurityPolicy.class);
                createPsp.getMetadata().setResourceVersion(null);
                createPsp.getMetadata().setSelfLink(null);
                this.createPodSecurityPolicy(cluster, createPsp, ContextHolder.exeContext());
            } else {
                ExtensionsV1beta1PodSecurityPolicy createPsp = Yaml.loadAs(yamlStr, ExtensionsV1beta1PodSecurityPolicy.class);
                createPsp.getMetadata().setResourceVersion(null);
                createPsp.getMetadata().setSelfLink(null);
                this.createPodSecurityPolicy(cluster, createPsp, ContextHolder.exeContext());
            }
        }
        else {
            log.error(String.format("Invalid API Kind Type : createPodSecurityPolicy : %s\n%s", Optional.ofNullable(apiKindType).map(K8sApiKindType::getCode), yamlStr));
        }

    }

    /**
     * Extensions PodSecurityPolicy 생성
     *
     * @param cluster
     * @param podSecurityPolicySpec
     * @param context
     * @return
     * @throws Exception
     */
    public ExtensionsV1beta1PodSecurityPolicy createPodSecurityPolicy(ClusterVO cluster, ExtensionsV1beta1PodSecurityPolicy podSecurityPolicySpec, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        // 현재 PodSecurityPolicy 조회
        ExtensionsV1beta1PodSecurityPolicy currentPsp = k8sWorker.getPodSecurityPolicyExtensionsV1beta1(cluster, podSecurityPolicySpec.getMetadata().getName());
        if(currentPsp != null){
            throw new CocktailException("PodSecurityPolicy already exists!!", ExceptionType.PspNameAlreadyExists);
        }

        return k8sWorker.createPodSecurityPolicyExtensionsV1beat1(cluster, podSecurityPolicySpec);
    }

    /**
     * Policy PodSecurityPolicy 생성
     *
     * @param cluster
     * @param podSecurityPolicySpec
     * @param context
     * @return
     * @throws Exception
     */
    public V1beta1PodSecurityPolicy createPodSecurityPolicy(ClusterVO cluster, V1beta1PodSecurityPolicy podSecurityPolicySpec, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        // 현재 PodSecurityPolicy 조회
        V1beta1PodSecurityPolicy currentPsp = k8sWorker.getPodSecurityPolicyPolicyV1beta1(cluster, podSecurityPolicySpec.getMetadata().getName());
        if(currentPsp != null){
            throw new CocktailException("PodSecurityPolicy already exists!!", ExceptionType.PspNameAlreadyExists);
        }

        return k8sWorker.createPodSecurityPolicyPolicyV1beat1(cluster, podSecurityPolicySpec);
    }

    /**
     * K8S PodSecurityPolicy Patch
     *
     * @param cluster
     * @param pspGui
     * @param context
     * @return
     * @throws Exception
     */
    public K8sPodSecurityPolicyVO patchPodSecurityPolicy(ClusterVO cluster, PodSecurityPolicyGuiVO pspGui, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        K8sPodSecurityPolicyVO k8sPsps = null;

        ExtensionsV1beta1PodSecurityPolicy currExtensionsPsp = null;
        V1beta1PodSecurityPolicy currPolicyPsp = null;

        if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
            currPolicyPsp = k8sWorker.getPodSecurityPolicyPolicyV1beta1(cluster, pspGui.getName());
        } else {
            currExtensionsPsp = k8sWorker.getPodSecurityPolicyExtensionsV1beta1(cluster, pspGui.getName());
        }

        if (currExtensionsPsp != null) {

            ExtensionsV1beta1PodSecurityPolicy updatedPsp = K8sSpecFactory.buildPspExtensionsV1beta1(pspGui);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currExtensionsPsp, updatedPsp);

            updatedPsp = k8sWorker.patchPodSecurityPolicyExtensionsV1beat1(cluster, updatedPsp.getMetadata().getName(), patchBody);
            Thread.sleep(100);

            k8sPsps = this.getPodSecurityPolicy(cluster, updatedPsp.getMetadata().getName(), context);

        } else if (currPolicyPsp != null) {

            V1beta1PodSecurityPolicy updatedPsp = K8sSpecFactory.buildPspPolicyV1beta1(pspGui);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currPolicyPsp, updatedPsp);

            updatedPsp = k8sWorker.patchPodSecurityPolicyPolicyV1beat1(cluster, updatedPsp.getMetadata().getName(), patchBody);
            Thread.sleep(100);

            k8sPsps = this.getPodSecurityPolicy(cluster, updatedPsp.getMetadata().getName(), context);
        }

        return k8sPsps;
    }

    public K8sPodSecurityPolicyVO patchPodSecurityPolicy(ClusterVO cluster, ExtensionsV1beta1PodSecurityPolicy updatedPsp, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        // 현재 Ingress 조회
        ExtensionsV1beta1PodSecurityPolicy currentPsp = k8sWorker.getPodSecurityPolicyExtensionsV1beta1(cluster, updatedPsp.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentPsp, updatedPsp);
        log.debug("########## PodSecurityPolicy patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchPodSecurityPolicyExtensionsV1beat1(cluster, updatedPsp.getMetadata().getName(), patchBody);
        Thread.sleep(100);

        return this.getPodSecurityPolicy(cluster, updatedPsp.getMetadata().getName(), context);
    }

    public K8sPodSecurityPolicyVO patchPodSecurityPolicy(ClusterVO cluster, V1beta1PodSecurityPolicy updatedPsp, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        // 현재 Ingress 조회
        V1beta1PodSecurityPolicy currentPsp = k8sWorker.getPodSecurityPolicyPolicyV1beta1(cluster, updatedPsp.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentPsp, updatedPsp);
        log.debug("########## PodSecurityPolicy patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchPodSecurityPolicyPolicyV1beat1(cluster, updatedPsp.getMetadata().getName(), patchBody);
        Thread.sleep(100);

        return this.getPodSecurityPolicy(cluster, updatedPsp.getMetadata().getName(), context);
    }

    public K8sPodSecurityPolicyVO patchPodSecurityPolicy(ClusterVO cluster, String yamlStr, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        Map<String, Object> pspObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(pspObjMap);
        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(pspObjMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(pspObjMap);

        if (apiKindType == K8sApiKindType.POD_SECURITY_POLICY) {
            if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
                V1beta1PodSecurityPolicy updatedPsp = Yaml.loadAs(yamlStr, V1beta1PodSecurityPolicy.class);
                return this.patchPodSecurityPolicy(cluster, updatedPsp, ContextHolder.exeContext());
            } else {
                ExtensionsV1beta1PodSecurityPolicy updatedPsp = Yaml.loadAs(yamlStr, ExtensionsV1beta1PodSecurityPolicy.class);
                return this.patchPodSecurityPolicy(cluster, updatedPsp, ContextHolder.exeContext());
            }
        }

        return null;
    }

    public K8sPodSecurityPolicyVO patchPodSecurityPolicy(ClusterVO cluster, String name, List<JsonObject> patchBody, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
            // patch
            k8sWorker.patchPodSecurityPolicyPolicyV1beat1(cluster, name, patchBody);
            Thread.sleep(100);

            return this.getPodSecurityPolicy(cluster, name, context);
        } else {
            // patch
            k8sWorker.patchPodSecurityPolicyExtensionsV1beat1(cluster, name, patchBody);
            Thread.sleep(100);

            return this.getPodSecurityPolicy(cluster, name, context);
        }
    }

    /**
     * display Default를 제외한 psp에서 라벨 제거
     *
     * @param cluster
     * @param displayDefaultPspName
     * @throws Exception
     */
    public void removeOtherDisplayDefaultPsp(ClusterVO cluster, String displayDefaultPspName) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        List<K8sPodSecurityPolicyVO> psps = this.getPodSecurityPolicies(cluster, String.format("metadata.name!=%s", displayDefaultPspName), KubeConstants.LABELS_ACORNSOFT_PSP_DISPLAY_DEFALUT, ContextHolder.exeContext());

        if (CollectionUtils.isNotEmpty(psps)) {
            Map<String, Object> patchMap = new HashMap<>();
            patchMap.put("op", JsonPatchOp.REMOVE.getValue());
            patchMap.put("path", String.format("/metadata/labels/%s", KubeConstants.LABELS_ACORNSOFT_PSP_DISPLAY_DEFALUT));

            List<JsonObject> patchBody = new ArrayList<>();
            patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());

            for (K8sPodSecurityPolicyVO pspRow : psps) {
                this.patchPodSecurityPolicy(cluster, pspRow.getName(), patchBody, ContextHolder.exeContext());
            }
        }
    }


//    public V1ClusterRole patchClusterRoleV1ForPsp(ClusterVO cluster, String name, String pspName) throws Exception{
//        if(cluster == null) {
//            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
//        }
//
//        String clusterRoleName = "";
//        if (cluster.getCubeType() == CubeType.EKS) {
//            // EKS 1.13 부터 Policy group의 PodSecurityPolicy를 기본으로 생성함. 이하 버전은 지원안함.
//            if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_13)) {
//                clusterRoleName = KubeConstants.POD_SECURITY_POLICY_EKS_CLUSTER_ROLE_DEFAULT_NAME;
//            } else {
//                this.raiseExceptionNotSuppored(cluster.getK8sVersion());
//            }
//        } else {
//            clusterRoleName = KubeConstants.POD_SECURITY_POLICY_CLUSTER_ROLE_DEFAULT_NAME;
//        }
//
//        V1ClusterRole currClusterRole = rbacResourceService.getClusterRoleV1(cluster, clusterRoleName);
//        V1ClusterRole updatedClusterRole = k8sPatchSpecFactory.copyObject(currClusterRole, new TypeReference<V1ClusterRole>(){});
//
//        for (V1PolicyRule rule : updatedClusterRole.getRules()) {
//            if (rule.getApiGroups().contains(K8sApiGroupType.POLICY.getValue())
//                    && rule.getResources().contains(KubeConstants.POD_SECURITY_POLICY_RESOURCES)
//                    && rule.getVerbs().contains("use")
//            ) {
//                rule.setResourceNames(Arrays.asList(pspName));
//            }
//        }
//
//        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currClusterRole, updatedClusterRole);
//
//        return rbacResourceService.patchClusterRoleV1(cluster, clusterRoleName, patchBody);
//
//    }

    /**
     * K8S PodSecurityPolicy 삭제
     *
     * @param cluster
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public void deletePodSecurityPolicy(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception {

        this.deletePodSecurityPolicy(cluster, name, false, context);

    }

    public void deletePodSecurityPolicy(ClusterVO cluster, String name, boolean cascade, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        K8sPodSecurityPolicyVO psp = this.getPodSecurityPolicy(cluster, name, context);

        if (psp != null) {
            if (cascade) {
                IntegrateRBACVO integrate = this.getIntegrateRBACByPsp(cluster, name);
                if (integrate != null) {
                    // delete roleBinding
                    for (K8sRoleBindingVO item : Optional.ofNullable(integrate.getRoleBindings()).orElseGet(() ->Lists.newArrayList())) {
                        rbacResourceService.deleteRoleBinding(cluster, item.getNamespace(), item.getName());
                    }
                    // delete clusterRoleBinding
                    for (K8sClusterRoleBindingVO item : Optional.ofNullable(integrate.getClusterRoleBindings()).orElseGet(() ->Lists.newArrayList())) {
                        rbacResourceService.deleteClusterRoleBindingV1(cluster, item.getName());
                    }
                    // binding 삭제 대기
                    Thread.sleep(500);
                    // delete role
                    for (K8sRoleVO item : Optional.ofNullable(integrate.getRoles()).orElseGet(() ->Lists.newArrayList())) {
                        rbacResourceService.deleteRole(cluster, item.getNamespace(), item.getName());
                    }
                    // delete clusterRole
                    for (K8sClusterRoleVO item : Optional.ofNullable(integrate.getClusterRoles()).orElseGet(() ->Lists.newArrayList())) {
                        rbacResourceService.deleteClusterRole(cluster, item.getName());
                    }
                }
            } else {
                IntegrateRBACVO integrate = this.getIntegrateRBACByPsp(cluster, name);
                if (integrate != null
                        && (
                                CollectionUtils.isNotEmpty(integrate.getClusterRoles())
                                    || CollectionUtils.isNotEmpty(integrate.getRoles())
                                    || CollectionUtils.isNotEmpty(integrate.getClusterRoleBindings())
                                    || CollectionUtils.isNotEmpty(integrate.getRoleBindings())
                        )
                ) {
                    throw new CocktailException("There is a resource binding that Pod security setting and cannot be deleted.", ExceptionType.PspCanNotDeleteHasBindingResource, "There is a resource binding that Pod security setting and cannot be deleted.");
                }
            }

            if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
                k8sWorker.deletePodSecurityPolicyPolicyV1beta1(cluster, name);
                Thread.sleep(500);
            } else {
                k8sWorker.deletePodSecurityPolicyExtensionsV1beta1(cluster, name);
                Thread.sleep(500);
            }
        }
    }

    /**
     * K8S PodSecurityPolicy 정보 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @param context
     * @return List<K8sPodSecurityPolicyVO>
     * @throws Exception
     */
    public List<K8sPodSecurityPolicyVO> getPodSecurityPolicies(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){
                /**
                 * k8s 1.25부터 psp가 remove되어 기능 제거됨
                 */
                this.checkSupportedVersionForPsp(cluster.getK8sVersion());

                return this.convertPodSecurityPolicyDataList(cluster, field, label, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getPodSecurityPolicies fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S PodSecurityPolicy 정보 조회
     *
     * @param cluster
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sPodSecurityPolicyVO getPodSecurityPolicy(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(name)){
            /**
             * k8s 1.25부터 psp가 remove되어 기능 제거됨
             */
            this.checkSupportedVersionForPsp(cluster.getK8sVersion());

            ExtensionsV1beta1PodSecurityPolicy extensionsPsp = null;
            V1beta1PodSecurityPolicy policyPsp = null;

            if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
                policyPsp = k8sWorker.getPodSecurityPolicyPolicyV1beta1(cluster, name);
            } else {
                extensionsPsp = k8sWorker.getPodSecurityPolicyExtensionsV1beta1(cluster, name);
            }


            if (extensionsPsp != null) {
                return this.convertPodSecurityPolicyData(extensionsPsp, new JSON());
            } else if (policyPsp != null) {
                return this.convertPodSecurityPolicyData(policyPsp, new JSON());
            } else {
                return null;
            }
        }else{
            throw new CocktailException("cluster/name is null.", ExceptionType.InvalidParameter, "cluster/name is null.");
        }
    }

    public K8sPodSecurityPolicyVO getPodSecurityPolicyWithRBAC(ClusterVO cluster, String name, boolean useRBAC, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        // psp 상세 조회
        K8sPodSecurityPolicyVO psp = this.getPodSecurityPolicy(cluster, name, context);

        // psp 관련 clusterRole 조회
        if (psp != null && useRBAC) {
            psp.setRBAC(this.getIntegrateRBACByPsp(cluster, name));
        }

        return psp;
    }

    /**
     * psp와 관련된 ClusterRole, Role, ClusterRoleBinding, RoleBinding 목록 조회
     *
     * @param cluster
     * @param pspName
     * @return
     * @throws Exception
     */
    public IntegrateRBACVO getIntegrateRBACByPsp(ClusterVO cluster, String pspName) throws Exception {
        IntegrateRBACVO integrate = new IntegrateRBACVO();
        // clusterRole
        integrate.setClusterRoles(this.getClusterRoleByPsp(cluster, pspName));
        List<String> clusterRoleNames = this.getClusterRoleNamesByPsp(integrate.getClusterRoles());

        // role
        integrate.setRoles(this.getRoleByPsp(cluster, pspName));
        Map<String, List<String>> roleNameMap = this.getRoleNameMap(integrate.getRoles());

        // clusterRoleBinding
        integrate.setClusterRoleBindings(rbacResourceService.getClusterRoleBindingsByRole(cluster, clusterRoleNames));

        // roleBinding
        integrate.setRoleBindings(rbacResourceService.getRoleBindingsByRole(cluster, clusterRoleNames, roleNameMap));

        return integrate;
    }

    public List<K8sClusterRoleVO> getClusterRoleByPsp(ClusterVO cluster, String pspName) throws Exception {
        List<K8sClusterRoleVO> clusterRoles = rbacResourceService.getClusterRoles(cluster, null, null);
        List<K8sClusterRoleVO> clusterRolesByPsp = Lists.newArrayList();

        for (K8sClusterRoleVO crRow : Optional.ofNullable(clusterRoles).orElseGet(() ->Lists.newArrayList())) {
            for (PolicyRuleVO ruleRow : Optional.ofNullable(crRow.getRules()).orElseGet(() ->Lists.newArrayList())) {
                if (this.isPspRole(ruleRow, pspName)) {
                    clusterRolesByPsp.add(crRow);
                    break;
                }
            }
        }

        return clusterRolesByPsp;
    }

    public List<String> getClusterRoleNamesByPsp(List<K8sClusterRoleVO> clusterRolesByPsp) throws Exception {
        return Optional.ofNullable(clusterRolesByPsp).orElseGet(() ->Lists.newArrayList()).stream().map(K8sClusterRoleVO::getName).collect(Collectors.toList());
    }

    public List<K8sRoleVO> getRoleByPsp(ClusterVO cluster, String pspName) throws Exception {
        List<K8sRoleVO> roles = rbacResourceService.getRoles(cluster, null, null, null);
        List<K8sRoleVO> rolesByPsp = Lists.newArrayList();

        for (K8sRoleVO rRow : Optional.ofNullable(roles).orElseGet(() ->Lists.newArrayList())) {
            for (PolicyRuleVO ruleRow : Optional.ofNullable(rRow.getRules()).orElseGet(() ->Lists.newArrayList())) {
                if (this.isPspRole(ruleRow, pspName)) {
                    rolesByPsp.add(rRow);
                    break;
                }
            }
        }

        return rolesByPsp;
    }

    public Map<String, List<String>> getRoleNameMap(List<K8sRoleVO> rolesByPsp) throws Exception {
        Map<String, List<String>> roleNameMap = Maps.newHashMap();
        for (K8sRoleVO rRow : Optional.ofNullable(rolesByPsp).orElseGet(() ->Lists.newArrayList())) {
            if (MapUtils.getObject(roleNameMap, rRow.getNamespace(), null) == null) {
                roleNameMap.put(rRow.getNamespace(), Lists.newArrayList());
            }
            roleNameMap.get(rRow.getNamespace()).add(rRow.getName());
        }

        return roleNameMap;
    }

    public boolean isPspRole(PolicyRuleVO policyRule, String pspName) throws Exception {
        if (Optional.ofNullable(policyRule.getApiGroups()).orElseGet(() ->Lists.newArrayList()).contains(K8sApiGroupType.POLICY.getValue())
                && Optional.ofNullable(policyRule.getResources()).orElseGet(() ->Lists.newArrayList()).contains(KubeConstants.POD_SECURITY_POLICY_RESOURCES)
                && Optional.ofNullable(policyRule.getResourceNames()).orElseGet(() ->Lists.newArrayList()).contains(pspName)
                && Optional.ofNullable(policyRule.getVerbs()).orElseGet(() ->Lists.newArrayList()).contains(K8sRoleVerbType.use.getCode())
        ) {
            return true;
        }

        return false;
    }

    public List<K8sRoleBindingVO> getRoleBindingsByPsp(ClusterVO cluster, String pspName) throws Exception {

        // clusterRole - psp
        List<K8sClusterRoleVO> clusterRolesByPsp = this.getClusterRoleByPsp(cluster, pspName);

        // role - psp
        List<K8sRoleVO> rolesByPsp = this.getRoleByPsp(cluster, pspName);

        return this.getRoleBindingsByPsp(cluster, clusterRolesByPsp, rolesByPsp);
    }

    public List<K8sRoleBindingVO> getRoleBindingsByPsp(ClusterVO cluster, List<K8sClusterRoleVO> clusterRolesByPsp, List<K8sRoleVO> rolesByPsp) throws Exception {
        return rbacResourceService.getRoleBindingsByRole(cluster, this.getClusterRoleNamesByPsp(clusterRolesByPsp), this.getRoleNameMap(rolesByPsp));
    }

    public List<K8sClusterRoleBindingVO> getClusterRoleBindingsByPsp(ClusterVO cluster, String pspName) throws Exception {
        List<K8sClusterRoleVO> clusterRolesByPsp = this.getClusterRoleByPsp(cluster, pspName);

        return rbacResourceService.getClusterRoleBindingsByRole(cluster, this.getClusterRoleNamesByPsp(clusterRolesByPsp));
    }


    /**
     * K8s PodSecurityPolicy 정보 조회. (K8s API Version 1.14 미만)
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<ExtensionsV1beta1PodSecurityPolicy> getPodSecurityPolicyExtensionsV1Beta1(ClusterVO cluster, String field, String label) throws Exception {
        if(cluster == null){
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        try {
            return k8sWorker.getPodSecurityPoliciesExtensionsV1Beta1(cluster, field, label);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getPodSecurityPolicyExtensionsV1Beta1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8s PodSecurityPolicy 정보 조회.  (K8s API Version 1.14 이상)
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1beta1PodSecurityPolicy> getPodSecurityPolicyPolicyV1Beta1(ClusterVO cluster, String field, String label) throws Exception {
        if(cluster == null){
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        try {
            return k8sWorker.getPodSecurityPoliciesPolicyV1Beta1(cluster, field, label);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getPodSecurityPolicyPolicyV1Beta1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S PodSecurityPolicy 정보 조회 후 V1beta1PodSecurityPolicy -> K8sPodSecurityPolicyVO 변환
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sPodSecurityPolicyVO> convertPodSecurityPolicyDataList(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        List<K8sPodSecurityPolicyVO> psps = new ArrayList<>();
        List<ExtensionsV1beta1PodSecurityPolicy> extensionsPsps = null;
        List<V1beta1PodSecurityPolicy> policyPsps = null;

        if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
            policyPsps = this.getPodSecurityPolicyPolicyV1Beta1(cluster, field, label);
        } else {
            extensionsPsps = this.getPodSecurityPolicyExtensionsV1Beta1(cluster, field, label);
        }

        // joda.datetime Serialization
        JSON k8sJson = new JSON();

        if(CollectionUtils.isNotEmpty(extensionsPsps)){

            for(ExtensionsV1beta1PodSecurityPolicy row : extensionsPsps){
                K8sPodSecurityPolicyVO psp = this.convertPodSecurityPolicyData(row, k8sJson);

                psps.add(psp);

            }

        } else if (CollectionUtils.isNotEmpty(policyPsps)) {

            for(V1beta1PodSecurityPolicy row : policyPsps){
                K8sPodSecurityPolicyVO psp = this.convertPodSecurityPolicyData(row, k8sJson);

                psps.add(psp);

            }
        }

        return psps;
    }

    /**
     * K8S PodSecurityPolicy 정보 조회 후 ExtensionsV1beta1PodSecurityPolicy -> K8sPodSecurityPolicyVO 변환
     *
     * @param v1beta1PodSecurityPolicy
     * @throws Exception
     */
    public K8sPodSecurityPolicyVO convertPodSecurityPolicyData(ExtensionsV1beta1PodSecurityPolicy v1beta1PodSecurityPolicy, JSON k8sJson) throws Exception {

        K8sPodSecurityPolicyVO podSecurityPolicy = new K8sPodSecurityPolicyVO();
        if(v1beta1PodSecurityPolicy != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            podSecurityPolicy = JsonUtils.fromGson(k8sJson.serialize(v1beta1PodSecurityPolicy.getSpec()), K8sPodSecurityPolicyVO.class);

            // description
            podSecurityPolicy.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1beta1PodSecurityPolicy.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // 기본 노출 psp 여부
            podSecurityPolicy.setDisplayDefault(ResourceUtil.isDisplayDefaultPsp(Optional.ofNullable(v1beta1PodSecurityPolicy.getMetadata().getLabels()).orElseGet(() ->Maps.newHashMap())));

            podSecurityPolicy.setName(v1beta1PodSecurityPolicy.getMetadata().getName());
            podSecurityPolicy.setNewName(v1beta1PodSecurityPolicy.getMetadata().getName());
            podSecurityPolicy.setLabels(v1beta1PodSecurityPolicy.getMetadata().getLabels());
            podSecurityPolicy.setAnnotations(v1beta1PodSecurityPolicy.getMetadata().getAnnotations());
            podSecurityPolicy.setCreationTimestamp(v1beta1PodSecurityPolicy.getMetadata().getCreationTimestamp());

            podSecurityPolicy.setDeployment(k8sJson.serialize(v1beta1PodSecurityPolicy));
            podSecurityPolicy.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1beta1PodSecurityPolicy));
        }

        return podSecurityPolicy;
    }

    /**
     * K8S PodSecurityPolicy 정보 조회 후 PolicyV1beta1PodSecurityPolicy -> K8sPodSecurityPolicyVO 변환
     *
     * @param v1beta1PodSecurityPolicy
     * @throws Exception
     */
    public K8sPodSecurityPolicyVO convertPodSecurityPolicyData(V1beta1PodSecurityPolicy v1beta1PodSecurityPolicy, JSON k8sJson) throws Exception {

        K8sPodSecurityPolicyVO podSecurityPolicy = new K8sPodSecurityPolicyVO();
        if(v1beta1PodSecurityPolicy != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            podSecurityPolicy = JsonUtils.fromGson(k8sJson.serialize(v1beta1PodSecurityPolicy.getSpec()), K8sPodSecurityPolicyVO.class);

            // description
            podSecurityPolicy.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1beta1PodSecurityPolicy.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // 기본 노출 psp 여부
            podSecurityPolicy.setDisplayDefault(ResourceUtil.isDisplayDefaultPsp(Optional.ofNullable(v1beta1PodSecurityPolicy.getMetadata().getLabels()).orElseGet(() ->Maps.newHashMap())));

            podSecurityPolicy.setName(v1beta1PodSecurityPolicy.getMetadata().getName());
            podSecurityPolicy.setNewName(v1beta1PodSecurityPolicy.getMetadata().getName());
            podSecurityPolicy.setLabels(v1beta1PodSecurityPolicy.getMetadata().getLabels());
            podSecurityPolicy.setAnnotations(v1beta1PodSecurityPolicy.getMetadata().getAnnotations());
            podSecurityPolicy.setCreationTimestamp(v1beta1PodSecurityPolicy.getMetadata().getCreationTimestamp());

            podSecurityPolicy.setDeployment(k8sJson.serialize(v1beta1PodSecurityPolicy));
            podSecurityPolicy.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1beta1PodSecurityPolicy));
        }

        return podSecurityPolicy;
    }

    public PodSecurityPolicyGuiVO generatePspTemplate() throws Exception {
        PodSecurityPolicyGuiVO gui = new PodSecurityPolicyGuiVO();
        gui.setName("");
        gui.setDisplayDefault(false);
        gui.putLabelsItem(KubeConstants.LABELS_ACORNSOFT_PSP_RESOURCE, KubeConstants.LABELS_COCKTAIL_KEY);
        gui.putAnnotationsItem(KubeConstants.POD_SECURITY_POLICY_ANNOTATIONS_ALLOWED_PROFILE_NAMES, "*");

        gui.setAllowPrivilegeEscalation(Boolean.TRUE);
        gui.setAllowedCapabilities(Arrays.asList("*"));
        gui.setFsGroup(new FSGroupStrategyOptionsVO(FSGroupRule.RunAsAny.getCode()));
        gui.setHostIPC(Boolean.TRUE);
        gui.setHostNetwork(Boolean.TRUE);
        gui.setHostPID(Boolean.TRUE);
        gui.setHostPorts(Arrays.asList(new HostPortRangeVO(65535L, 0L)));
        gui.setPrivileged(Boolean.TRUE);
        gui.setRunAsUser(new RunAsUserStrategyOptionsVO(SupplementalGroupsRule.RunAsAny.getCode()));
        gui.setSeLinux(new SELinuxStrategyOptionsVO(SELinuxRule.RunAsAny.getCode()));
        gui.setSupplementalGroups(new SupplementalGroupsStrategyOptionsVO(SupplementalGroupsRule.RunAsAny.getCode()));
        gui.setVolumes(Arrays.asList("*"));

        return gui;
    }

//    public void createDefaultPodSecurityPolicyWithRBAC(ClusterVO cluster) throws Exception{
//
//        PodSecurityPolicyGuiVO gui = new PodSecurityPolicyGuiVO();
//        gui.setName(KubeConstants.POD_SECURITY_POLICY_DEFAULT_NAME);
////        gui.setDescription("The pod security policy is managed by a cocktail.");
//        gui.putLabelsItem(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
//        gui.putAnnotationsItem(KubeConstants.POD_SECURITY_POLICY_ANNOTATIONS_ALLOWED_PROFILE_NAMES, "*");
//
//        gui.setAllowPrivilegeEscalation(Boolean.TRUE);
//        gui.setAllowedCapabilities(Arrays.asList("*"));
//        gui.setFsGroup(new FSGroupStrategyOptionsVO(FSGroupRule.RunAsAny.getCode()));
//        gui.setHostIPC(Boolean.TRUE);
//        gui.setHostNetwork(Boolean.TRUE);
//        gui.setHostPID(Boolean.TRUE);
//        gui.setHostPorts(Arrays.asList(new HostPortRangeVO(65535L, 0L)));
//        gui.setPrivileged(Boolean.TRUE);
//        gui.setRunAsUser(new RunAsUserStrategyOptionsVO(SupplementalGroupsRule.RunAsAny.getCode()));
//        gui.setSeLinux(new SELinuxStrategyOptionsVO(SELinuxRule.RunAsAny.getCode()));
//        gui.setSupplementalGroups(new SupplementalGroupsStrategyOptionsVO(SupplementalGroupsRule.RunAsAny.getCode()));
//        gui.setVolumes(Arrays.asList("*"));
//
//        this.createDefaultPodSecurityPolicyWithRBAC(cluster, gui);
//    }

//    /**
//     * 기본 psp 및 clusterRole, ClusterRoleBinding (system:authenticated) 생성
//     *
//     * @param cluster
//     * @param pspGui
//     * @throws Exception
//     */
//    public void createDefaultPodSecurityPolicyWithRBAC(ClusterVO cluster, PodSecurityPolicyGuiVO pspGui) throws Exception{
//        /**
//         * cluster 상태 체크
//         */
//        clusterStateService.checkClusterState(cluster);
//
//        if (pspGui != null) {
//            if (cluster.getCubeType() == CubeType.EKS) {
//                // EKS 1.13 부터 Policy group의 PodSecurityPolicy를 기본으로 생성함. 이하 버전은 지원안함.
//                if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_13)) {
//                    // EKS 1.13 부터는 기본적으로 PSP가 생성되므로 cocktail 관리 라벨만 생성해줌.
//                    K8sPodSecurityPolicyVO k8sPsp = this.getPodSecurityPolicy(cluster, KubeConstants.POD_SECURITY_POLICY_EKS_DEFAULT_NAME, ContextHolder.exeContext());
//                    if (k8sPsp != null) {
//                        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(k8sPsp.getLabels());
//                        this.patchPodSecurityPolicy(cluster, KubeConstants.POD_SECURITY_POLICY_EKS_DEFAULT_NAME, patchBody, ContextHolder.exeContext());
//                    }
//                    V1ClusterRole v1ClusterRole = rbacResourceService.getClusterRoleV1(cluster, KubeConstants.POD_SECURITY_POLICY_EKS_CLUSTER_ROLE_DEFAULT_NAME);
//                    if (v1ClusterRole != null) {
//                        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(v1ClusterRole.getMetadata().getLabels());
//                        rbacResourceService.patchClusterRoleV1(cluster, KubeConstants.POD_SECURITY_POLICY_EKS_CLUSTER_ROLE_DEFAULT_NAME, patchBody);
//                    }
//                    V1ClusterRoleBinding v1ClusterRoleBinding = rbacResourceService.getClusterRoleBindingV1(cluster, KubeConstants.POD_SECURITY_POLICY_EKS_CLUSTER_ROLE_BINDING_DEFAULT_NAME);
//                    if (v1ClusterRoleBinding != null) {
//                        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(v1ClusterRoleBinding.getMetadata().getLabels());
//                        rbacResourceService.patchClusterRoleBindingV1(cluster, KubeConstants.POD_SECURITY_POLICY_EKS_CLUSTER_ROLE_BINDING_DEFAULT_NAME, patchBody);
//                    }
//                } else {
//                    this.raiseExceptionNotSuppored(cluster.getK8sVersion());
//                }
//            } else {
//                K8sPodSecurityPolicyVO k8sPsp = this.getPodSecurityPolicy(cluster, pspGui.getName(), ContextHolder.exeContext());
//                if (k8sPsp == null) {
//                    // psp 생성
//                    this.createPodSecurityPolicy(cluster, pspGui, ContextHolder.exeContext());
//                } else {
//                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(k8sPsp.getLabels());
//                    this.patchPodSecurityPolicy(cluster, pspGui.getName(), patchBody, ContextHolder.exeContext());
//                }
//
//                V1ClusterRole v1ClusterRole = rbacResourceService.getClusterRoleV1(cluster, KubeConstants.POD_SECURITY_POLICY_CLUSTER_ROLE_DEFAULT_NAME);
//                if (v1ClusterRole == null) {
//                    // clusterRole 생성
//                    this.createClusterRolePsp(cluster, KubeConstants.POD_SECURITY_POLICY_CLUSTER_ROLE_DEFAULT_NAME, KubeConstants.POD_SECURITY_POLICY_DEFAULT_NAME, false);
//                } else {
//                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(v1ClusterRole.getMetadata().getLabels());
//                    rbacResourceService.patchClusterRoleV1(cluster, KubeConstants.POD_SECURITY_POLICY_CLUSTER_ROLE_DEFAULT_NAME, patchBody);
//                }
//
//                V1ClusterRoleBinding v1ClusterRoleBinding = rbacResourceService.getClusterRoleBindingV1(cluster, KubeConstants.POD_SECURITY_POLICY_CLUSTER_ROLE_BINDING_DEFAULT_NAME);
//                if (v1ClusterRoleBinding == null) {
//                    // clusterRoleBinding 생성
//                    V1ClusterRoleBinding clusterRoleBinding = new V1ClusterRoleBinding();
//                    clusterRoleBinding.metadata(new V1ObjectMeta());
//                    clusterRoleBinding.getMetadata().setName(KubeConstants.POD_SECURITY_POLICY_CLUSTER_ROLE_BINDING_DEFAULT_NAME);
//                    clusterRoleBinding.getMetadata().putLabelsItem(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
//                    V1RoleRef roleRef = new V1RoleRef();
//                    roleRef.setApiGroup(K8sApiGroupType.RBAC_AUTHORIZATION.getValue());
//                    roleRef.setKind(K8sApiKindType.CLUSTER_ROLE.getValue());
//                    roleRef.setName(KubeConstants.POD_SECURITY_POLICY_CLUSTER_ROLE_DEFAULT_NAME);
//                    clusterRoleBinding.setRoleRef(roleRef);
//                    V1Subject subject = new V1Subject();
//                    subject.setKind(K8sApiKindType.GROUP.getValue());
//                    subject.setApiGroup(K8sApiGroupType.RBAC_AUTHORIZATION.getValue());
//                    subject.setName(KubeConstants.RBAC_GROUP_SYSTEM_AUTHENTICATED);
//                    clusterRoleBinding.addSubjectsItem(subject);
//                    rbacResourceService.createClusterRoleBindingV1(cluster, clusterRoleBinding);
//                } else {
//                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(v1ClusterRoleBinding.getMetadata().getLabels());
//                    rbacResourceService.patchClusterRoleBindingV1(cluster, KubeConstants.POD_SECURITY_POLICY_CLUSTER_ROLE_BINDING_DEFAULT_NAME, patchBody);
//                }
//
//            }
//
//        }
//    }

    public void createClusterRolePsp(ClusterVO cluster, String roleName, String pspName, boolean dryRun) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        // clusterRole 생성
        V1ClusterRole clusterRole = new V1ClusterRole();
        clusterRole.metadata(new V1ObjectMeta());
        clusterRole.getMetadata().setName(roleName);
        clusterRole.getMetadata().putLabelsItem(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
        clusterRole.getMetadata().putLabelsItem(KubeConstants.LABELS_ACORNSOFT_PSP_RESOURCE, KubeConstants.LABELS_COCKTAIL_KEY);
        V1PolicyRule policyRule = new V1PolicyRule();
        policyRule.addApiGroupsItem(K8sApiGroupType.POLICY.getValue());
        policyRule.addResourceNamesItem(pspName);
        policyRule.addResourcesItem(KubeConstants.POD_SECURITY_POLICY_RESOURCES);
        policyRule.addVerbsItem(K8sRoleVerbType.use.getCode());
        clusterRole.addRulesItem(policyRule);
        rbacResourceService.createClusterRoleV1(cluster, clusterRole, dryRun);
    }

    /**
     * PodSecurityPolicy 체크
     *
     * @param cluster
     * @param isAdd
     * @param gui
     * @throws Exception
     */
    public void checkPodSecurityPolicy(ClusterVO cluster, boolean isAdd, PodSecurityPolicyGuiVO gui) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        K8sPodSecurityPolicyVO k8sPsp = null;
        if (isAdd) {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("PodSecurityPolicy name is invalid", ExceptionType.K8sPspNameInvalid, ResourceUtil.getInvalidNameMsg("PodSecurityPolicy name is invalid"));
            } else {
                k8sPsp = this.getPodSecurityPolicy(cluster, gui.getName(), ContextHolder.exeContext());
                if (k8sPsp != null) {
                    throw new CocktailException("PodSecurityPolicy already exists!!", ExceptionType.PspNameAlreadyExists);
                }
            }
        } else {
            /** 2020.08.28 : PSP 이름을 변경 가능하게 되면서 이름 유효성 체크는 NewName으로 하도록 하고, 기존 PSP 조회는 기존 Name으로 조회 하도록 함 **/
            /** 2020.10.20 : PSP 이름을 변경 가능하게 되면서 이름 유효성 체크는 다시 name으로 하도록 함 **/
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("PodSecurityPolicy name is invalid", ExceptionType.K8sPspNameInvalid, ResourceUtil.getInvalidNameMsg("PodSecurityPolicy name is invalid"));
            } else {
                k8sPsp = this.getPodSecurityPolicy(cluster, gui.getName(), ContextHolder.exeContext());
                if (k8sPsp == null) {
                    throw new CocktailException("PodSecurityPolicy not found!!", ExceptionType.K8sPspNotFound);
                }
            }
        }
    }

    /**
     * Yaml to GUI
     *
     * @param cluster
     * @param yamlStr
     * @return
     * @throws Exception
     */
    public PodSecurityPolicyGuiVO convertYamlToGui(ClusterVO cluster, String yamlStr) throws Exception {

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        this.checkSupportedVersionForPsp(cluster.getK8sVersion());

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> pspObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(pspObjMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(pspObjMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(pspObjMap);

            if (apiKindType == K8sApiKindType.POD_SECURITY_POLICY) {
                JSON k8sJson = new JSON();
                if (apiGroupType == K8sApiGroupType.EXTENSIONS && apiVerType == K8sApiType.V1BETA1) {
                    ExtensionsV1beta1PodSecurityPolicy updatedPsp = Yaml.loadAs(yamlStr, ExtensionsV1beta1PodSecurityPolicy.class);
//                    PodSecurityPolicyGuiVO pspGui = JsonUtils.fromJackson(k8sJson.serialize(updatedPsp.getSpec()), PodSecurityPolicyGuiVO.class);
//                    pspGui.setName(updatedPsp.getMetadata().getName());
//                    pspGui.setLabels(updatedPsp.getMetadata().getLabels());
//                    pspGui.setAnnotations(updatedPsp.getMetadata().getAnnotations());
//                    pspGui.setCreationTimestamp(updatedPsp.getMetadata().getCreationTimestamp());
                    return this.convertPodSecurityPolicyData(updatedPsp, new JSON());
                } else if (apiGroupType == K8sApiGroupType.POLICY && apiVerType == K8sApiType.V1BETA1) {
                    V1beta1PodSecurityPolicy updatedPsp = Yaml.loadAs(yamlStr, V1beta1PodSecurityPolicy.class);
//                    PodSecurityPolicyGuiVO pspGui = JsonUtils.fromJackson(k8sJson.serialize(updatedPsp.getSpec()), PodSecurityPolicyGuiVO.class);
//                    pspGui.setName(updatedPsp.getMetadata().getName());
//                    pspGui.setLabels(updatedPsp.getMetadata().getLabels());
//                    pspGui.setAnnotations(updatedPsp.getMetadata().getAnnotations());
//                    pspGui.setCreationTimestamp(updatedPsp.getMetadata().getCreationTimestamp());
                    return this.convertPodSecurityPolicyData(updatedPsp, new JSON());
                }
            }
        }

        return null;
    }

    private void raiseExceptionNotSuppored(String version) throws Exception {
        String errMsg = String.format("Version [%s] of the EKS cluster does not support PSP.", version);
        throw new CocktailException(errMsg, ExceptionType.K8sNotSupported, errMsg);
    }

    /**
     * k8s 1.25부터 psp가 remove되어 기능 제거됨
     *
     * @param k8sVersion
     * @throws Exception
     */
    public void checkSupportedVersionForPsp(String k8sVersion) throws Exception {
        if (!ResourceUtil.isSupportedPsp(k8sVersion)) {
            throw new CocktailException(String.format("Cube Cluster version[%s] is not support the pod security policy.", k8sVersion), ExceptionType.K8sNotSupported);
        }
    }
}
