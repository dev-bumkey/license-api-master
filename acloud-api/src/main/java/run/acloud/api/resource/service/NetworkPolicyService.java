package run.acloud.api.resource.service;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.NetworkPolicyCreationType;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiGroupType;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class NetworkPolicyService {

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;


    /**
     * K8S NetworkPolicy 생성
     *
     * @param cluster
     * @param npGui
     * @param context
     * @return
     * @throws Exception
     */
    public K8sNetworkPolicyVO createNetworkPolicy(ClusterVO cluster, NetworkPolicyGuiVO npGui, ExecutingContextVO context) throws Exception {

        this.createNetworkPolicy(cluster, npGui, false, context);
        Thread.sleep(100);

        return this.getNetworkPolicy(cluster, npGui.getNamespace(), npGui.getName(), context);
    }

    public void createNetworkPolicy(ClusterVO cluster, NetworkPolicyGuiVO npGui, boolean dryRun, ExecutingContextVO context) throws Exception {
        if (npGui != null) {
            V1NetworkPolicy v1bNetworkPolicy = K8sSpecFactory.buildNetworkPolicyV1(npGui);
            k8sWorker.createNetworkPolicyV1(cluster, npGui.getNamespace(), v1bNetworkPolicy, dryRun);
        } else {
            throw new CocktailException("Invalid request NetworkPolicyGui info", ExceptionType.InvalidParameter);
        }
    }

    /**
     * NetworkPolicy 생성 (yaml)
     *
     * @param cluster
     * @param namespace
     * @param yamlStr
     * @param context
     * @throws Exception
     */
    public K8sNetworkPolicyVO createNetworkPolicy(ClusterVO cluster, String namespace, String yamlStr, ExecutingContextVO context) throws Exception {
        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

        if (apiKindType == K8sApiKindType.NETWORK_POLICY && apiGroupType == K8sApiGroupType.NETWORKING && apiVerType == K8sApiType.V1) {
            V1NetworkPolicy createNp = Yaml.loadAs(yamlStr, V1NetworkPolicy.class);

            // 현재 PodSecurityPolicy 조회
            V1NetworkPolicy currentNp = k8sWorker.getNetworkPolicyV1(cluster, namespace, createNp.getMetadata().getName());
            if(currentNp != null){
                throw new CocktailException("NetworkPolicy already exists!!", ExceptionType.NetworkPolicyNameAlreadyExists);
            }

            // 생성
            k8sWorker.createNetworkPolicyV1(cluster, namespace, createNp, false);
            Thread.sleep(100);

            return this.getNetworkPolicy(cluster, createNp.getMetadata().getNamespace(), createNp.getMetadata().getName(), context);
        } else {
            throw new CocktailException("Invalid API Kind, Group, Version,", ExceptionType.InvalidYamlData
                    , String.format("%s: %s, %s: %s", KubeConstants.APIVSERION, MapUtils.getString(objMap, KubeConstants.APIVSERION), KubeConstants.KIND, MapUtils.getString(objMap, KubeConstants.KIND)));
        }
    }

    /**
     * K8S NetworkPolicy Patch
     *
     * @param cluster
     * @param npGui
     * @param context
     * @return
     * @throws Exception
     */
    public K8sNetworkPolicyVO patchNetworkPolicy(ClusterVO cluster, NetworkPolicyGuiVO npGui, boolean dryRun, ExecutingContextVO context) throws Exception {

        K8sNetworkPolicyVO k8sNp = null;

        V1NetworkPolicy currNp = k8sWorker.getNetworkPolicyV1(cluster, npGui.getNamespace(), npGui.getName());

        if (currNp != null) {

            V1NetworkPolicy updatedNp = K8sSpecFactory.buildNetworkPolicyV1(npGui);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currNp, updatedNp);

            updatedNp = k8sWorker.patchNetworkPolicyV1(cluster, updatedNp.getMetadata().getNamespace(), updatedNp.getMetadata().getName(), patchBody, dryRun);
            Thread.sleep(100);

            k8sNp = this.getNetworkPolicy(cluster, updatedNp.getMetadata().getNamespace(), updatedNp.getMetadata().getName(), context);

        }

        return k8sNp;
    }

    public K8sNetworkPolicyVO patchNetworkPolicy(ClusterVO cluster, V1NetworkPolicy updatedNp, boolean dryRun, ExecutingContextVO context) throws Exception {
        // 현재 NetworkPolicy 조회
        V1NetworkPolicy currNp = k8sWorker.getNetworkPolicyV1(cluster, updatedNp.getMetadata().getNamespace(), updatedNp.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currNp, updatedNp);
        log.debug("########## NetworkPolicy patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchNetworkPolicyV1(cluster, updatedNp.getMetadata().getNamespace(), updatedNp.getMetadata().getName(), patchBody, dryRun);
        Thread.sleep(100);

        return this.getNetworkPolicy(cluster, updatedNp.getMetadata().getNamespace(), updatedNp.getMetadata().getName(), context);
    }

    public K8sNetworkPolicyVO patchNetworkPolicy(ClusterVO cluster, String yamlStr, boolean dryRun, ExecutingContextVO context) throws Exception {

        Map<String, Object> npObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(npObjMap);
        if (apiKindType == K8sApiKindType.NETWORK_POLICY) {
            V1NetworkPolicy updatedNp = Yaml.loadAs(yamlStr, V1NetworkPolicy.class);
            return this.patchNetworkPolicy(cluster, updatedNp, dryRun, ContextHolder.exeContext());
        }

        return null;
    }

    public K8sNetworkPolicyVO patchNetworkPolicy(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun, ExecutingContextVO context) throws Exception {
        // patch
        k8sWorker.patchNetworkPolicyV1(cluster, namespace, name, patchBody, dryRun);
        Thread.sleep(100);

        return this.getNetworkPolicy(cluster, namespace, name, context);
    }

    /**
     * K8S NetworkPolicy 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @throws Exception
     */
    public void deleteNetworkPolicy(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception {

        V1NetworkPolicy v1NetworkPolicy = k8sWorker.getNetworkPolicyV1(cluster, namespace, name);

        if (v1NetworkPolicy != null) {
            k8sWorker.deleteNetworkPolicyV1(cluster, namespace, name);
            Thread.sleep(500);
        }

    }

    /**
     * K8S NetworkPolicy 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return List<K8sNetworkPolicyVO>
     * @throws Exception
     */
    public List<K8sNetworkPolicyVO> getNetworkPolicies(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){
                return this.convertNetworkPolicyDataList(cluster, namespace, field, label, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getNetworkPolicies fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S NetworkPolicy 정보 조회
     *
     * @param cluster
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sNetworkPolicyVO getNetworkPolicy(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(name)){

            V1NetworkPolicy v1NetworkPolicy = k8sWorker.getNetworkPolicyV1(cluster, namespace, name);

            if (v1NetworkPolicy != null) {
                return this.convertNetworkPolicyData(v1NetworkPolicy, new JSON());
            } else {
                return null;
            }

        }else{
            throw new CocktailException("cluster/name is null.", ExceptionType.InvalidParameter, "cluster/name is null.");
        }
    }

    /**
     * K8S NetworkPolicy 정보 조회 후 V1NetworkPolicy -> K8sNetworkPolicyVO 변환
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sNetworkPolicyVO> convertNetworkPolicyDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sNetworkPolicyVO> nps = new ArrayList<>();

        List<V1NetworkPolicy> v1NetworkPolicies = k8sWorker.getNetworkPoliciesV1(cluster, namespace, field, label);

        if (CollectionUtils.isNotEmpty(v1NetworkPolicies)) {

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for (V1NetworkPolicy item : v1NetworkPolicies) {
                nps.add(this.convertNetworkPolicyData(item, k8sJson));
            }

        }

        return nps;
    }

    /**
     * K8S NetworkPolicy 정보 조회 후 V1NetworkPolicy -> K8sNetworkPolicyVO 변환
     *
     * @param v1NetworkPolicy
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sNetworkPolicyVO convertNetworkPolicyData(V1NetworkPolicy v1NetworkPolicy, JSON k8sJson) throws Exception {

        K8sNetworkPolicyVO networkPolicy = new K8sNetworkPolicyVO();
        if(v1NetworkPolicy != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            networkPolicy.setNamespace(v1NetworkPolicy.getMetadata().getNamespace());
            networkPolicy.setName(v1NetworkPolicy.getMetadata().getName());
            networkPolicy.setLabels(v1NetworkPolicy.getMetadata().getLabels());
            networkPolicy.setAnnotations(v1NetworkPolicy.getMetadata().getAnnotations());
            networkPolicy.setCreationTimestamp(v1NetworkPolicy.getMetadata().getCreationTimestamp());

            // description
            networkPolicy.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1NetworkPolicy.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // 기본 여부
            networkPolicy.setDefault(ResourceUtil.isDefault(Optional.ofNullable(v1NetworkPolicy.getMetadata().getLabels()).orElseGet(() ->Maps.newHashMap())));

            // egress
            if (CollectionUtils.isNotEmpty(v1NetworkPolicy.getSpec().getEgress())) {
                for (V1NetworkPolicyEgressRule ruleRow : v1NetworkPolicy.getSpec().getEgress()) {
                    NetworkPolicyEgressRuleVO rule = new NetworkPolicyEgressRuleVO();

                    // egress.ports
                    if (CollectionUtils.isNotEmpty(ruleRow.getPorts())) {
                        for (V1NetworkPolicyPort portRow : ruleRow.getPorts()) {
                            NetworkPolicyPortVO port = new NetworkPolicyPortVO();
                            if (portRow.getPort() != null) {
                                port.setPort(portRow.getPort().toString());
                            }
                            port.setProtocol(portRow.getProtocol());
                            rule.addPortsItem(port);
                        }
                    }

                    // egress.to
                    String toJson = k8sJson.serialize(ruleRow.getTo());
                    rule.setTo(k8sJson.getGson().fromJson(toJson, new TypeToken<List<NetworkPolicyPeerVO>>(){}.getType()));

                    networkPolicy.addEgressItem(rule);
                }
            }

            // ingress
            if (CollectionUtils.isNotEmpty(v1NetworkPolicy.getSpec().getIngress())) {
                for (V1NetworkPolicyIngressRule ruleRow : v1NetworkPolicy.getSpec().getIngress()) {
                    NetworkPolicyIngressRuleVO rule = new NetworkPolicyIngressRuleVO();

                    // ingress.ports
                    if (CollectionUtils.isNotEmpty(ruleRow.getPorts())) {
                        for (V1NetworkPolicyPort portRow : ruleRow.getPorts()) {
                            NetworkPolicyPortVO port = new NetworkPolicyPortVO();
                            if (portRow.getPort() != null) {
                                port.setPort(portRow.getPort().toString());
                            }
                            port.setProtocol(portRow.getProtocol());
                            rule.addPortsItem(port);
                        }
                    }

                    // ingress.from
                    String fromJson = k8sJson.serialize(ruleRow.getFrom());
                    rule.setFrom(k8sJson.getGson().fromJson(fromJson, new TypeToken<List<NetworkPolicyPeerVO>>(){}.getType()));

                    networkPolicy.addIngressItem(rule);
                }
            }

            // podSelector
            networkPolicy.setPodSelector(JsonUtils.fromGson(k8sJson.serialize(v1NetworkPolicy.getSpec().getPodSelector()), K8sLabelSelectorVO.class));

            // policyTypes
            networkPolicy.setPolicyTypes(v1NetworkPolicy.getSpec().getPolicyTypes());

            networkPolicy.setDeployment(k8sJson.serialize(v1NetworkPolicy));
            networkPolicy.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1NetworkPolicy));
        }

        return networkPolicy;
    }

    public void createDefaultNetworkPolicy(ClusterVO cluster, String namespaceName) throws Exception {
        this.createDefaultNetworkPolicy(cluster, namespaceName, null, false);
    }

    public void createDefaultNetworkPolicy(ClusterVO cluster, String namespaceName, NetworkPolicyGuiVO gui, boolean dryRun) throws Exception {

        if (gui == null) {
            gui = new NetworkPolicyGuiVO();
            gui.setName(KubeConstants.NETWORK_POLICY_DEFAULT_NAME);
            gui.setNamespace(namespaceName);
            gui.setDescription("The network policy is managed by a cocktail.");
            gui.putLabelsItem(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
            gui.setDefault(true);

            // Ingress, egress
            gui.addPolicyTypesItem("Ingress");
            gui.addPolicyTypesItem("Egress");
            // egress 허용
            gui.addEgressItem(new NetworkPolicyEgressRuleVO());
        }

        K8sNetworkPolicyVO k8sNp = this.getNetworkPolicy(cluster, gui.getNamespace(), gui.getName(), ContextHolder.exeContext());
        if (k8sNp == null) {
            // 생성
            this.createNetworkPolicy(cluster, gui, ContextHolder.exeContext());
        } else {
            // 이미 존재한다면 관리 라벨만 추가
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(k8sNp.getLabels());
            this.patchNetworkPolicy(cluster, gui.getNamespace(), gui.getName(), patchBody, dryRun, ContextHolder.exeContext());
        }
    }

    public String generateNetworkPolicyTemplate(String namespaceName) {
        V1NetworkPolicy networkPolicy = new V1NetworkPolicy();
        networkPolicy.setApiVersion(String.format("%s/%s", K8sApiGroupType.NETWORKING.getValue(), K8sApiType.V1.getValue()));
        networkPolicy.setKind(K8sApiKindType.NETWORK_POLICY.getValue());

        // meta
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName("");
        meta.setNamespace(namespaceName);
        networkPolicy.setMetadata(meta);

        // spec
        V1NetworkPolicySpec spec = new V1NetworkPolicySpec();
        spec.addPolicyTypesItem("Ingress");
        spec.addPolicyTypesItem("Egress");
        spec.addEgressItem(new V1NetworkPolicyEgressRule());
        networkPolicy.setSpec(spec);

        return Yaml.getSnakeYaml().dumpAsMap(networkPolicy);
    }

    /**
     * 서비스맵 생성시 생성 유형에 따른 기본 NetworkPolicy Spec 생성
     *
     * @param creationType
     * @param namespaceName
     * @return
     */
    public NetworkPolicyGuiVO generateDefaultNetworkPolicyTemplateWithType(NetworkPolicyCreationType creationType, String namespaceName) {
        if (creationType != null) {
            NetworkPolicyGuiVO gui = new NetworkPolicyGuiVO();
            gui.setName(KubeConstants.NETWORK_POLICY_DEFAULT_NAME);
            gui.setNamespace(namespaceName);
            gui.setDefault(true);

            gui.addPolicyTypesItem("Ingress");
            gui.addPolicyTypesItem("Egress");

            switch (creationType) {
                case INGRESS_ALLOW_EGRESS_ALLOW:
                    gui.addIngressItem(new NetworkPolicyIngressRuleVO());
                    gui.addEgressItem(new NetworkPolicyEgressRuleVO());
                    break;
                case INGRESS_BLOCK_EGRESS_BLOCK:
                    break;
                case INGRESS_ALLOW_EGRESS_BLOCK:
                    gui.addIngressItem(new NetworkPolicyIngressRuleVO());
                    break;
                case INGRESS_BLOCK_EGRESS_ALLOW:
                    gui.addEgressItem(new NetworkPolicyEgressRuleVO());
                    break;
            }

            return gui;
        }

        return null;
    }

    /**
     * NetworkPolicy 체크
     *
     * @param cluster
     * @param namespaceName
     * @param isAdd
     * @param gui
     * @throws Exception
     */
    public void checkNetworkPolicy(ClusterVO cluster, String namespaceName, boolean isAdd, NetworkPolicyGuiVO gui) throws Exception {
        K8sNetworkPolicyVO k8sNp = null;
        if (isAdd) {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("NetworkPolicy name is invalid", ExceptionType.K8sNetworkPolicyNameInvalid, ResourceUtil.getInvalidNameMsg("NetworkPolicy name is invalid"));
            } else {
                k8sNp = this.getNetworkPolicy(cluster, namespaceName, gui.getName(), ContextHolder.exeContext());
                if (k8sNp != null) {
                    throw new CocktailException("NetworkPolicy already exists!!", ExceptionType.NetworkPolicyNameAlreadyExists);
                }
            }
        } else {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("NetworkPolicy name is invalid", ExceptionType.K8sNetworkPolicyNameInvalid, ResourceUtil.getInvalidNameMsg("NetworkPolicy name is invalid"));
            } else {
                k8sNp = this.getNetworkPolicy(cluster, namespaceName, gui.getName(), ContextHolder.exeContext());
                if (k8sNp == null) {
                    throw new CocktailException("NetworkPolicy not found!!", ExceptionType.K8sNetworkPolicyNotFound);
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
    public NetworkPolicyGuiVO convertYamlToGui(ClusterVO cluster, String yamlStr) throws Exception {

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

            if (apiKindType == K8sApiKindType.NETWORK_POLICY && apiGroupType == K8sApiGroupType.NETWORKING && apiVerType == K8sApiType.V1) {
                V1NetworkPolicy updatedNp = Yaml.loadAs(yamlStr, V1NetworkPolicy.class);
                return this.convertNetworkPolicyData(updatedNp, new JSON());
            } else {
                throw new CocktailException("Yaml is invalid.(it is not NetworkPolicy).", ExceptionType.InvalidYamlData);
            }
        }

        return null;
    }
}
