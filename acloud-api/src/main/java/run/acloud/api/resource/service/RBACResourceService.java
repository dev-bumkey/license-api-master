package run.acloud.api.resource.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RBACResourceService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;


    /**
     * Service Account 생성 (Invoke from Snapshot Deployment)
     * @param cluster
     * @param namespace
     * @param serviceAccounts
     * @throws Exception
     */
    public void createMultipleServiceAccount(ClusterVO cluster, String namespace, List<CommonYamlVO> serviceAccounts) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        if(CollectionUtils.isNotEmpty(serviceAccounts)){
            for(CommonYamlVO serviceAccount : serviceAccounts){
                if(DeployType.valueOf(serviceAccount.getDeployType()) == DeployType.GUI) {
                    log.error(String.format("DeployType.GUI not yet supported : createMultipleServiceAccount : %s", JsonUtils.toGson(serviceAccount)));
                }
                else if(DeployType.valueOf(serviceAccount.getDeployType()) == DeployType.YAML) {
                    try {
                        V1ServiceAccount v1ServiceAccount = ServerUtils.unmarshalYaml(serviceAccount.getYaml(), K8sApiKindType.SERVICE_ACCOUNT);
                        this.createServiceAccountV1(cluster, namespace, v1ServiceAccount);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Service Account Deployment Failure : createMultipleServiceAccount : %s\n%s", ex.getMessage(), JsonUtils.toGson(serviceAccount)));
                    }
                }
                else {
                    log.error(String.format("Invalid DeployType : createMultipleServiceAccount : %s", JsonUtils.toGson(serviceAccount)));
                }
            }
        }
    }

    /**
     * Service Account 생성
     * @param cluster
     * @param namespace
     * @param serviceAccount
     * @return
     * @throws Exception
     */
    public V1ServiceAccount createServiceAccountV1(ClusterVO cluster, String namespace, V1ServiceAccount serviceAccount) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        V1ServiceAccount v1ServiceAccount = k8sWorker.getServiceAccountV1(cluster, namespace, serviceAccount.getMetadata().getName());
        if(v1ServiceAccount != null){
            throw new CocktailException("Service Account already exists!!", ExceptionType.SecretNameAlreadyExists);
        }

        serviceAccount = k8sWorker.createServiceAccountV1(cluster, namespace, serviceAccount);

        return serviceAccount;
    }

    /**
     * K8S ServiceAccount 리스트 정보 조회
     * (cluster > namespace)
     *
     * @param clusterSeq
     * @param namespace
     * @param field ex) metadata.name=default
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sServiceAccountVO> getServiceAccounts(Integer clusterSeq, String namespace, String field, String label) throws Exception{

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getServiceAccounts(cluster, namespace, field, label);
    }

    public List<K8sServiceAccountVO> getServiceAccounts(ClusterVO cluster, String namespace, String field, String label) throws Exception{

        if(cluster != null){
            // field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);
            return this.convertServiceAccountDataList(cluster, namespace, field, label);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    public List<K8sServiceAccountVO> convertServiceAccountDataList(ClusterVO cluster, String namespaceName, String field, String label) throws Exception{

        List<K8sServiceAccountVO> serviceAccounts = new ArrayList<>();

        try {
            // get service account
            List<V1ServiceAccount> v1ServiceAccounts = k8sWorker.getServiceAccountsV1(cluster, namespaceName, field, label);

            // convert data
            if(CollectionUtils.isNotEmpty(v1ServiceAccounts)){
                for(V1ServiceAccount serviceAccount : v1ServiceAccounts){
                    K8sServiceAccountVO k8sServiceAccount = new K8sServiceAccountVO();
                    k8sServiceAccount.setNamespace(serviceAccount.getMetadata().getNamespace());
                    k8sServiceAccount.setServiceAccountName(serviceAccount.getMetadata().getName());
                    k8sServiceAccount.setCreationTimestamp(serviceAccount.getMetadata().getCreationTimestamp());
                    if (CollectionUtils.isNotEmpty(serviceAccount.getSecrets())) {
                        k8sServiceAccount.setSecrets(serviceAccount.getSecrets().stream().map(ref -> ref.getName()).collect(Collectors.toList()));
                    }
                    serviceAccounts.add(k8sServiceAccount);
                }
            }

        } catch (Exception e) {
            throw new CocktailException("convertServiceAccountDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return serviceAccounts;
    }

    /**
     * Service Account List 조회
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1ServiceAccount> getServiceAccountsV1(ClusterVO cluster, String namespace, String field, String label) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getServiceAccountsV1(cluster, namespace, field, label);
        } catch (Exception e) {
            throw new CocktailException("getServiceAccounts fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }




    /**
     * K8S ClusterRole 생성
     *
     * @param cluster
     * @param gui
     * @return
     * @throws Exception
     */
    public K8sClusterRoleVO createClusterRole(ClusterVO cluster, ClusterRoleGuiVO gui) throws Exception {

        this.createClusterRole(cluster, gui, false);
        Thread.sleep(100);

        return this.getClusterRole(cluster, gui.getName());
    }

    public void createClusterRole(ClusterVO cluster, ClusterRoleGuiVO gui, boolean dryRun) throws Exception {
        V1ClusterRole v1ClusterRole = K8sSpecFactory.buildClusterRoleV1(gui);
        k8sWorker.createClusterRoleV1(cluster, v1ClusterRole, dryRun);
    }

    /**
     * ClusterRole 생성 (yaml)
     *
     * @param cluster
     * @param yamlStr
     * @throws Exception
     */
    public K8sClusterRoleVO createClusterRole(ClusterVO cluster, String yamlStr) throws Exception {
        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

        if (apiKindType == K8sApiKindType.CLUSTER_ROLE && apiGroupType == K8sApiGroupType.RBAC_AUTHORIZATION && apiVerType == K8sApiType.V1) {
            V1ClusterRole createCr = Yaml.loadAs(yamlStr, V1ClusterRole.class);

            // 현재 ClusterRole 조회
            V1ClusterRole currentCr = k8sWorker.getClusterRoleV1(cluster, createCr.getMetadata().getName());
            if(currentCr != null){
                throw new CocktailException("ClusterRole already exists!!", ExceptionType.ClusterRoleNameAlreadyExists);
            }

            // 생성
            k8sWorker.createClusterRoleV1(cluster, createCr, false);
            Thread.sleep(100);

            return this.getClusterRole(cluster, createCr.getMetadata().getName());
        } else {
            throw new CocktailException("Invalid API Kind, Group, Version,", ExceptionType.InvalidYamlData
                    , String.format("%s: %s, %s: %s", KubeConstants.APIVSERION, MapUtils.getString(objMap, KubeConstants.APIVSERION), KubeConstants.KIND, MapUtils.getString(objMap, KubeConstants.KIND)));
        }
    }

    /**
     * ClusterRole 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1ClusterRole createClusterRoleV1(ClusterVO cluster, V1ClusterRole param) throws Exception{
        return this.createClusterRoleV1(cluster, param, false);
    }

    /**
     * ClusterRole 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1ClusterRole createClusterRoleV1(ClusterVO cluster, V1ClusterRole param, boolean dryRun) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        K8sClusterRoleVO k8sClusterRole = this.getClusterRole(cluster, param.getMetadata().getName());
        if(k8sClusterRole != null){
            throw new CocktailException("ClusterRole already exists!!", ExceptionType.ClusterRoleNameAlreadyExists);
        }

        V1ClusterRole result = k8sWorker.createClusterRoleV1(cluster, param, dryRun);

        return result;
    }


    /**
     * ClusterRole 체크
     *
     * @param cluster
     * @param isAdd
     * @param gui
     * @throws Exception
     */
    public void checkClusterRole(ClusterVO cluster, boolean isAdd, ClusterRoleGuiVO gui) throws Exception {
        K8sClusterRoleVO k8sCr = null;
        if (isAdd) {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("ClusterRole name is invalid", ExceptionType.K8sClusterRoleNameInvalid, ResourceUtil.getInvalidNameMsg("ClusterRole name is invalid"));
            }
        } else {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("ClusterRole name is invalid", ExceptionType.K8sClusterRoleNameInvalid, ResourceUtil.getInvalidNameMsg("ClusterRole name is invalid"));
            } else {
                k8sCr = this.getClusterRole(cluster, gui.getName());
                if (k8sCr == null) {
                    throw new CocktailException("ClusterRole not found!!", ExceptionType.K8sClusterRoleNotFound);
                }
            }
        }
    }

    public void checkClusterRoleName(ClusterVO cluster, boolean isAdd, String name) throws Exception {
        ClusterRoleGuiVO gui = new ClusterRoleGuiVO();
        gui.setDeployType("GUI");
        gui.setName(name);

        this.checkClusterRole(cluster, isAdd, gui);
    }

    /**
     * ClusterRole Yaml to GUI
     *
     * @param cluster
     * @param yamlStr
     * @return
     * @throws Exception
     */
    public ClusterRoleGuiVO convertClusterRoleYamlToGui(ClusterVO cluster, String yamlStr) throws Exception {

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

            if (apiKindType == K8sApiKindType.CLUSTER_ROLE && apiGroupType == K8sApiGroupType.RBAC_AUTHORIZATION && apiVerType == K8sApiType.V1) {
                V1ClusterRole updatedCr = Yaml.loadAs(yamlStr, V1ClusterRole.class);
                return this.convertClusterRoleData(updatedCr, new JSON());
            } else {
                throw new CocktailException("Yaml is invalid.(it is not ClusterRole).", ExceptionType.InvalidYamlData);
            }
        }

        return null;
    }

    /**
     * K8S ClusterRole 정보 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @return List<K8sClusterRoleVO>
     * @throws Exception
     */
    public List<K8sClusterRoleVO> getClusterRoles(ClusterVO cluster, String field, String label) throws Exception{

        try {
            if(cluster != null){
                return this.convertClusterRoleDataList(cluster, field, label);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getClusterRoles fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<V1ClusterRole> getClusterRolesV1(Integer clusterSeq, String field, String label) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getClusterRolesV1(cluster, field, label);
    }

    /**
     * ClusterRole List 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1ClusterRole> getClusterRolesV1(ClusterVO cluster, String field, String label) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getClusterRolesV1(cluster, field, label);
        } catch (Exception e) {
            throw new CocktailException("getClusterRoleV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S ClusterRole 정보 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public K8sClusterRoleVO getClusterRole(ClusterVO cluster, String name) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(name)){

            V1ClusterRole v1ClusterRole = this.getClusterRoleV1(cluster, name);

            if (v1ClusterRole != null) {
                return this.convertClusterRoleData(v1ClusterRole, new JSON());
            } else {
                return null;
            }

        }else{
            throw new CocktailException("cluster/name is null.", ExceptionType.InvalidParameter, "cluster/name is null.");
        }
    }

    /**
     * ClusterRole 상세 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1ClusterRole getClusterRoleV1(ClusterVO cluster, String name) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getClusterRoleV1(cluster, name);
        } catch (Exception e) {
            throw new CocktailException("getClusterRoleV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }


    /**
     * K8S ClusterRole 정보 조회 후 V1ClusterRole -> K8sClusterRoleVO 변환 (목록)
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sClusterRoleVO> convertClusterRoleDataList(ClusterVO cluster, String field, String label) throws Exception {

        List<K8sClusterRoleVO> crs = new ArrayList<>();

        List<V1ClusterRole> v1ClusterRoles = this.getClusterRolesV1(cluster, field, label);

        if (CollectionUtils.isNotEmpty(v1ClusterRoles)) {

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for (V1ClusterRole item : v1ClusterRoles) {
                crs.add(this.convertClusterRoleData(item, k8sJson));
            }

        }

        return crs;
    }

    /**
     * K8S ClusterRole 정보 조회 후 V1ClusterRole -> K8sClusterRoleVO 변환
     *
     * @param v1ClusterRole
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sClusterRoleVO convertClusterRoleData(V1ClusterRole v1ClusterRole, JSON k8sJson) throws Exception {

        K8sClusterRoleVO clusterRole = new K8sClusterRoleVO();
        if(v1ClusterRole != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            clusterRole.setName(v1ClusterRole.getMetadata().getName());
            clusterRole.setLabels(v1ClusterRole.getMetadata().getLabels());
            clusterRole.setAnnotations(v1ClusterRole.getMetadata().getAnnotations());
            clusterRole.setCreationTimestamp(v1ClusterRole.getMetadata().getCreationTimestamp());

            // description
            clusterRole.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1ClusterRole.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // AggregationRule
            if (v1ClusterRole.getAggregationRule() != null) {

                clusterRole.setAggregationRule(JsonUtils.fromGson(k8sJson.serialize(v1ClusterRole.getAggregationRule()), AggregationRuleVO.class));
            }

            // Rules
            if (CollectionUtils.isNotEmpty(v1ClusterRole.getRules())) {
                String toJson = k8sJson.serialize(v1ClusterRole.getRules());
                clusterRole.setRules(k8sJson.getGson().fromJson(toJson, new TypeToken<List<PolicyRuleVO>>(){}.getType()));
            }

            clusterRole.setDeployment(k8sJson.serialize(v1ClusterRole));
            clusterRole.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1ClusterRole));
        }

        return clusterRole;
    }

    /**
     * K8S ClusterRole Patch
     *
     * @param cluster
     * @param gui
     * @return
     * @throws Exception
     */
    public K8sClusterRoleVO patchClusterRole(ClusterVO cluster, ClusterRoleGuiVO gui, boolean dryRun) throws Exception {

        K8sClusterRoleVO k8sCr = null;

        V1ClusterRole currCr = k8sWorker.getClusterRoleV1(cluster, gui.getName());

        if (currCr != null) {

            V1ClusterRole updatedCr = K8sSpecFactory.buildClusterRoleV1(gui);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currCr, updatedCr);

            String updatedCrName = "";
            if (updatedCr != null && updatedCr.getMetadata() != null){
                updatedCrName = updatedCr.getMetadata().getName();
            }

            updatedCr = this.patchClusterRoleV1(cluster, updatedCrName, patchBody, dryRun);
            Thread.sleep(100);

            k8sCr = this.getClusterRole(cluster, updatedCrName);

        }

        return k8sCr;
    }

    public V1ClusterRole patchClusterRoleV1(ClusterVO cluster, String name, List<JsonObject> patchBody) throws Exception{
        return this.patchClusterRoleV1(cluster, name, patchBody, false);
    }

    public K8sClusterRoleVO patchClusterRole(ClusterVO cluster, V1ClusterRole updated, boolean dryRun) throws Exception {
        // 현재 ClusterRole 조회
        V1ClusterRole curr = k8sWorker.getClusterRoleV1(cluster, updated.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(curr, updated);
        log.debug("########## ClusterRole patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchClusterRoleV1(cluster, updated.getMetadata().getName(), patchBody, dryRun);
        Thread.sleep(100);

        return this.getClusterRole(cluster, updated.getMetadata().getName());
    }

    public K8sClusterRoleVO patchClusterRole(ClusterVO cluster, String yamlStr, boolean dryRun) throws Exception {

        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        if (apiKindType == K8sApiKindType.CLUSTER_ROLE) {
            V1ClusterRole updated = Yaml.loadAs(yamlStr, V1ClusterRole.class);
            return this.patchClusterRole(cluster, updated, dryRun);
        }

        return null;
    }

    /**
     * ClusterRole patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @param dryRun
     * @return
     * @throws Exception
     */
    public V1ClusterRole patchClusterRoleV1(ClusterVO cluster, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        return k8sWorker.patchClusterRoleV1(cluster, name, patchBody, dryRun);

    }

    /**
     * ClusterRole 삭제
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteClusterRole(ClusterVO cluster, String name) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        k8sWorker.deleteClusterRoleV1(cluster, name);

    }

    /**
     * ClusterRole Template
     *
     * @return
     */
    public String generateClusterRoleTemplate() {
        V1ClusterRole clusterRole = new V1ClusterRole();
        clusterRole.setApiVersion(String.format("%s/%s", K8sApiGroupType.RBAC_AUTHORIZATION.getValue(), K8sApiType.V1.getValue()));
        clusterRole.setKind(K8sApiKindType.CLUSTER_ROLE.getValue());

        // meta
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName("");
        meta.putLabelsItem(KubeConstants.LABELS_ACORNSOFT_CLUSTER_ROLE, "true");
        clusterRole.setMetadata(meta);

        List<String> verbSet1 = Lists.newArrayList();
        verbSet1.add(K8sRoleVerbType.get.getCode());
        verbSet1.add(K8sRoleVerbType.list.getCode());
        verbSet1.add(K8sRoleVerbType.watch.getCode());

        List<String> verbSet2 = Lists.newArrayList();
        verbSet2.add(K8sRoleVerbType.create.getCode());
        verbSet2.add(K8sRoleVerbType.delete.getCode());
        verbSet2.add(K8sRoleVerbType.deletecollection.getCode());
        verbSet2.add(K8sRoleVerbType.patch.getCode());
        verbSet2.add(K8sRoleVerbType.update.getCode());

        List<String> verbSet3 = Lists.newArrayList();
        verbSet3.addAll(verbSet1);
        verbSet3.addAll(verbSet2);

        V1PolicyRule pr1 = new V1PolicyRule();
        pr1.addApiGroupsItem("");
        pr1.addResourcesItem(K8sRoleResourceType.PODS_ATTACH.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS_EXEC.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS_PORTFORWARD.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS_PROXY.getValue())
                .addResourcesItem(K8sRoleResourceType.SECRETS.getValue())
                .addResourcesItem(K8sRoleResourceType.SERVICES_PROXY.getValue());
        pr1.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr1);

        V1PolicyRule pr2 = new V1PolicyRule();
        pr2.addApiGroupsItem("");
        pr2.addResourcesItem(K8sRoleResourceType.SERVICEACCOUNTS.getValue());
        pr2.addVerbsItem(K8sRoleVerbType.impersonate.getCode());
        clusterRole.addRulesItem(pr2);

        V1PolicyRule pr3 = new V1PolicyRule();
        pr3.addApiGroupsItem("");
        pr3.addResourcesItem(K8sRoleResourceType.PODS.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS_EXEC.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS_PORTFORWARD.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS_PROXY.getValue())
                .addResourcesItem(K8sRoleResourceType.SECRETS.getValue());
        pr3.getVerbs().addAll(verbSet2);
        clusterRole.addRulesItem(pr3);

        V1PolicyRule pr4 = new V1PolicyRule();
        pr4.addApiGroupsItem("");
        pr4.addResourcesItem(K8sRoleResourceType.CONFIGMAPS.getValue())
                .addResourcesItem(K8sRoleResourceType.ENDPOINTS.getValue())
                .addResourcesItem(K8sRoleResourceType.PERSISTENTVOLUMECLAIMS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICATIONCONTROLLERS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICATIONCONTROLLERS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.SECRETS.getValue())
                .addResourcesItem(K8sRoleResourceType.SERVICEACCOUNTS.getValue())
                .addResourcesItem(K8sRoleResourceType.SERVICES.getValue())
                .addResourcesItem(K8sRoleResourceType.SERVICES_PROXY.getValue());
        pr4.getVerbs().addAll(verbSet2);
        clusterRole.addRulesItem(pr4);

        V1PolicyRule pr5 = new V1PolicyRule();
        pr5.addApiGroupsItem(K8sApiGroupType.APPS.getValue());
        pr5.addResourcesItem(K8sRoleResourceType.DAEMONSETS.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS_ROLLBACK.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.STATEFULSETS.getValue())
                .addResourcesItem(K8sRoleResourceType.STATEFULSETS_SCALE.getValue());
        pr5.getVerbs().addAll(verbSet2);
        clusterRole.addRulesItem(pr5);

        V1PolicyRule pr6 = new V1PolicyRule();
        pr6.addApiGroupsItem(K8sApiGroupType.AUTOSCALING.getValue());
        pr6.addResourcesItem(K8sRoleResourceType.HORIZONTALPODAUTOSCALERS.getValue());
        pr6.setVerbs(verbSet2);
        clusterRole.addRulesItem(pr6);

        V1PolicyRule pr7 = new V1PolicyRule();
        pr7.addApiGroupsItem(K8sApiGroupType.BATCH.getValue());
        pr7.addResourcesItem(K8sRoleResourceType.CRONJOBS.getValue())
                .addResourcesItem(K8sRoleResourceType.JOBS.getValue());
        pr7.getVerbs().addAll(verbSet2);
        clusterRole.addRulesItem(pr7);

        V1PolicyRule pr8 = new V1PolicyRule();
        pr8.addApiGroupsItem(K8sApiGroupType.EXTENSIONS.getValue());
        pr8.addResourcesItem(K8sRoleResourceType.DAEMONSETS.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS_ROLLBACK.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.INGRESSES.getValue())
                .addResourcesItem(K8sRoleResourceType.NETWORKPOLICIES.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICATIONCONTROLLERS_SCALE.getValue());
        pr8.getVerbs().addAll(verbSet2);
        clusterRole.addRulesItem(pr8);

        V1PolicyRule pr9 = new V1PolicyRule();
        pr9.addApiGroupsItem(K8sApiGroupType.POLICY.getValue());
        pr9.addResourcesItem(K8sRoleResourceType.PODDISRUPTIONBUDGETS.getValue());
        pr9.getVerbs().addAll(verbSet2);
        clusterRole.addRulesItem(pr9);

        V1PolicyRule pr10 = new V1PolicyRule();
        pr10.addApiGroupsItem(K8sApiGroupType.NETWORKING.getValue());
        pr10.addResourcesItem(K8sRoleResourceType.INGRESSES.getValue())
                .addResourcesItem(K8sRoleResourceType.NETWORKPOLICIES.getValue());
        pr10.getVerbs().addAll(verbSet2);
        clusterRole.addRulesItem(pr10);

        V1PolicyRule pr11 = new V1PolicyRule();
        pr11.addApiGroupsItem(K8sApiGroupType.METRICS.getValue());
        pr11.addResourcesItem(K8sRoleResourceType.PODS.getValue())
                .addResourcesItem(K8sRoleResourceType.NODES.getValue());
        pr11.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr11);

        V1PolicyRule pr12 = new V1PolicyRule();
        pr12.addApiGroupsItem("");
        pr12.addResourcesItem(K8sRoleResourceType.CONFIGMAPS.getValue())
                .addResourcesItem(K8sRoleResourceType.ENDPOINTS.getValue())
                .addResourcesItem(K8sRoleResourceType.PERSISTENTVOLUMECLAIMS.getValue())
                .addResourcesItem(K8sRoleResourceType.PERSISTENTVOLUMECLAIMS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICATIONCONTROLLERS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICATIONCONTROLLERS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.SERVICEACCOUNTS.getValue())
                .addResourcesItem(K8sRoleResourceType.SERVICES.getValue())
                .addResourcesItem(K8sRoleResourceType.SERVICES_STATUS.getValue());
        pr12.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr12);

        V1PolicyRule pr13 = new V1PolicyRule();
        pr13.addApiGroupsItem("");
        pr13.addResourcesItem(K8sRoleResourceType.BINDINGS.getValue())
                .addResourcesItem(K8sRoleResourceType.EVENTS.getValue())
                .addResourcesItem(K8sRoleResourceType.LIMITRANGES.getValue())
                .addResourcesItem(K8sRoleResourceType.NAMESPACES_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS_LOG.getValue())
                .addResourcesItem(K8sRoleResourceType.PODS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICATIONCONTROLLERS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.RESOURCEQUOTAS.getValue())
                .addResourcesItem(K8sRoleResourceType.RESOURCEQUOTAS_STATUS.getValue());
        pr13.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr13);

        V1PolicyRule pr14 = new V1PolicyRule();
        pr14.addApiGroupsItem("");
        pr14.addResourcesItem(K8sRoleResourceType.NAMESPACES.getValue());
        pr14.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr14);

        V1PolicyRule pr15 = new V1PolicyRule();
        pr15.addApiGroupsItem(K8sApiGroupType.APPS.getValue());
        pr15.addResourcesItem(K8sRoleResourceType.CONTROLLERREVISIONS.getValue())
                .addResourcesItem(K8sRoleResourceType.DAEMONSETS.getValue())
                .addResourcesItem(K8sRoleResourceType.DAEMONSETS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.STATEFULSETS.getValue())
                .addResourcesItem(K8sRoleResourceType.STATEFULSETS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.STATEFULSETS_STATUS.getValue());
        pr15.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr15);

        V1PolicyRule pr16 = new V1PolicyRule();
        pr16.addApiGroupsItem(K8sApiGroupType.AUTOSCALING.getValue());
        pr16.addResourcesItem(K8sRoleResourceType.HORIZONTALPODAUTOSCALERS.getValue())
                .addResourcesItem(K8sRoleResourceType.HORIZONTALPODAUTOSCALERS_STATUS.getValue());
        pr16.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr16);

        V1PolicyRule pr17 = new V1PolicyRule();
        pr17.addApiGroupsItem(K8sApiGroupType.BATCH.getValue());
        pr17.addResourcesItem(K8sRoleResourceType.CRONJOBS.getValue())
                .addResourcesItem(K8sRoleResourceType.CRONJOBS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.JOBS.getValue())
                .addResourcesItem(K8sRoleResourceType.JOBS_STATUS.getValue());
        pr17.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr17);

        V1PolicyRule pr18 = new V1PolicyRule();
        pr18.addApiGroupsItem(K8sApiGroupType.EXTENSIONS.getValue());
        pr18.addResourcesItem(K8sRoleResourceType.DAEMONSETS.getValue())
                .addResourcesItem(K8sRoleResourceType.DAEMONSETS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.DEPLOYMENTS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.INGRESSES.getValue())
                .addResourcesItem(K8sRoleResourceType.INGRESSES_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.NETWORKPOLICIES.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS_SCALE.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICASETS_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.REPLICATIONCONTROLLERS_SCALE.getValue());
        pr18.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr18);

        V1PolicyRule pr19 = new V1PolicyRule();
        pr19.addApiGroupsItem(K8sApiGroupType.POLICY.getValue());
        pr19.addResourcesItem(K8sRoleResourceType.PODDISRUPTIONBUDGETS.getValue())
                .addResourcesItem(K8sRoleResourceType.PODDISRUPTIONBUDGETS_STATUS.getValue());
        pr19.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr19);

        V1PolicyRule pr20 = new V1PolicyRule();
        pr20.addApiGroupsItem(K8sApiGroupType.NETWORKING.getValue());
        pr20.addResourcesItem(K8sRoleResourceType.INGRESSES.getValue())
                .addResourcesItem(K8sRoleResourceType.INGRESSES_STATUS.getValue())
                .addResourcesItem(K8sRoleResourceType.NETWORKPOLICIES.getValue());
        pr20.getVerbs().addAll(verbSet1);
        clusterRole.addRulesItem(pr20);

        V1PolicyRule pr21 = new V1PolicyRule();
        pr21.addApiGroupsItem(K8sApiGroupType.AUTHORIZATION.getValue());
        pr21.addResourcesItem(K8sRoleResourceType.LOCALSUBJECTACCESSREVIEWS.getValue());
        pr21.addVerbsItem(K8sRoleVerbType.create.getCode());
        clusterRole.addRulesItem(pr21);

        V1PolicyRule pr22 = new V1PolicyRule();
        pr22.addApiGroupsItem(K8sApiGroupType.RBAC_AUTHORIZATION.getValue());
        pr22.addResourcesItem(K8sRoleResourceType.ROLEBINDINGS.getValue())
                .addResourcesItem(K8sRoleResourceType.ROLES.getValue());
        pr22.getVerbs().addAll(verbSet1);
        pr22.getVerbs().addAll(verbSet2);
        clusterRole.addRulesItem(pr22);

        return Yaml.getSnakeYaml().dumpAsMap(clusterRole);
    }


    /*************************************** ClusteRoleBinding ***************************************/
    /**
     * K8S ClusterRoleBinding 정보 조회 후 V1ClusterRoleBinding -> K8sClusterRoleBindingVO 변환 (목록)
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sClusterRoleBindingVO> convertClusterRoleBindingDataList(ClusterVO cluster, String field, String label) throws Exception {

        List<K8sClusterRoleBindingVO> crbs = new ArrayList<>();

        List<V1ClusterRoleBinding> v1ClusterRoleBindings = this.getClusterRoleBindingsV1(cluster, field, label);

        if (CollectionUtils.isNotEmpty(v1ClusterRoleBindings)) {

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for (V1ClusterRoleBinding item : v1ClusterRoleBindings) {
                crbs.add(this.convertClusterRoleBindingData(item, k8sJson));
            }

        }

        return crbs;
    }

    /**
     * K8S ClusterRoleBinding 정보 조회 후 V1ClusterRoleBinding -> K8sClusterRoleBindingVO 변환
     *
     * @param v1ClusterRoleBinding
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sClusterRoleBindingVO convertClusterRoleBindingData(V1ClusterRoleBinding v1ClusterRoleBinding, JSON k8sJson) throws Exception {

        K8sClusterRoleBindingVO roleBinding = new K8sClusterRoleBindingVO();
        if(v1ClusterRoleBinding != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            roleBinding.setName(v1ClusterRoleBinding.getMetadata().getName());
            roleBinding.setLabels(v1ClusterRoleBinding.getMetadata().getLabels());
            roleBinding.setAnnotations(v1ClusterRoleBinding.getMetadata().getAnnotations());
            roleBinding.setCreationTimestamp(v1ClusterRoleBinding.getMetadata().getCreationTimestamp());

            // description
            roleBinding.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1ClusterRoleBinding.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // RoleRef
            if (v1ClusterRoleBinding.getRoleRef() != null) {
                roleBinding.setRoleRef(JsonUtils.fromGson(k8sJson.serialize(v1ClusterRoleBinding.getRoleRef()), RoleRefVO.class));
            }

            // Subjects
            if (CollectionUtils.isNotEmpty(v1ClusterRoleBinding.getSubjects())) {
                String toJson = k8sJson.serialize(v1ClusterRoleBinding.getSubjects());
                roleBinding.setSubjects(k8sJson.getGson().fromJson(toJson, new TypeToken<List<SubjectVO>>(){}.getType()));
            }

            roleBinding.setDeployment(k8sJson.serialize(v1ClusterRoleBinding));
            roleBinding.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1ClusterRoleBinding));
        }

        return roleBinding;
    }

    /**
     * K8S ClusterRoleBinding 정보 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @return List<K8sClusterRoleBindingVO>
     * @throws Exception
     */
    public List<K8sClusterRoleBindingVO> getClusterRoleBindings(ClusterVO cluster, String field, String label) throws Exception{

        try {
            if(cluster != null){
                return this.convertClusterRoleBindingDataList(cluster, field, label);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getClusterRoleBindings fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<V1ClusterRoleBinding> getClusterRoleBindingsV1(Integer clusterSeq, String field, String label) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getClusterRoleBindingsV1(cluster, field, label);
    }

    /**
     * ClusterRoleBinding List 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1ClusterRoleBinding> getClusterRoleBindingsV1(ClusterVO cluster, String field, String label) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getClusterRoleBindingsV1(cluster, field, label);
        } catch (Exception e) {
            throw new CocktailException("getClusterRoleBindingV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * ClusterRoleBinding 상세 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1ClusterRoleBinding getClusterRoleBindingV1(ClusterVO cluster, String name) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getClusterRoleBindingV1(cluster, name);
        } catch (Exception e) {
            throw new CocktailException("getClusterRoleBindingV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S ClusterRoleBinding Patch GUI
     *
     * @param cluster
     * @param gui
     * @return
     * @throws Exception
     */
    public K8sClusterRoleBindingVO patchClusterRoleBinding(ClusterVO cluster, ClusterRoleBindingGuiVO gui, boolean dryRun) throws Exception {

        K8sClusterRoleBindingVO k8sCrb = null;

        V1ClusterRoleBinding currCrb = k8sWorker.getClusterRoleBindingV1(cluster, gui.getName());

        if (currCrb != null) {

            V1ClusterRoleBinding updatedCrb = K8sSpecFactory.buildClusterRoleBindingV1(gui);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currCrb, updatedCrb);

            String updatedCrbName = "";
            if (updatedCrb != null && updatedCrb.getMetadata() != null){
                updatedCrbName = updatedCrb.getMetadata().getName();
            }

            updatedCrb = this.patchClusterRoleBindingV1(cluster, updatedCrbName, patchBody, dryRun);
            Thread.sleep(100);

            k8sCrb = this.getClusterRoleBinding(cluster, updatedCrbName);

        }

        return k8sCrb;
    }

    /**
     * K8S ClusterRoleBinding Patch YAML
     *
     * @param cluster
     * @param yamlStr
     * @param dryRun
     * @return
     * @throws Exception
     */
    public K8sClusterRoleBindingVO patchClusterRoleBinding(ClusterVO cluster, String yamlStr, boolean dryRun) throws Exception {

        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        if (apiKindType == K8sApiKindType.CLUSTER_ROLE_BINDING) {
            V1ClusterRoleBinding updated = Yaml.loadAs(yamlStr, V1ClusterRoleBinding.class);
            return this.patchClusterRoleBinding(cluster, updated, dryRun);
        }
        else {
            throw new CocktailException("Yaml is invalid.(it is not ClusterRoleBinding).", ExceptionType.InvalidYamlData);
        }
    }

    public K8sClusterRoleBindingVO patchClusterRoleBinding(ClusterVO cluster, V1ClusterRoleBinding updated, boolean dryRun) throws Exception {
        // 현재 ClusterRoleBinding 조회
        V1ClusterRoleBinding curr = k8sWorker.getClusterRoleBindingV1(cluster, updated.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(curr, updated);
        log.debug("########## ClusterRoleBinding patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchClusterRoleBindingV1(cluster, updated.getMetadata().getName(), patchBody, dryRun);
        Thread.sleep(100);

        return this.getClusterRoleBinding(cluster, updated.getMetadata().getName());
    }


    /**
     * ClusterRoleBinding patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1ClusterRoleBinding patchClusterRoleBindingV1(ClusterVO cluster, String name, List<JsonObject> patchBody) throws Exception{
        return this.patchClusterRoleBindingV1(cluster, name, patchBody, false);
    }

    public V1ClusterRoleBinding patchClusterRoleBindingV1(ClusterVO cluster, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        return k8sWorker.patchClusterRoleBindingV1(cluster, name, patchBody, dryRun);

    }

    /**
     * ClusterRoleBinding 삭제
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteClusterRoleBindingV1(ClusterVO cluster, String name) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        k8sWorker.deleteClusterRoleBindingV1(cluster, name);

    }

    /**
     * K8S ClusterRoleBinding 정보 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public K8sClusterRoleBindingVO getClusterRoleBinding(ClusterVO cluster, String name) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(name)){

            V1ClusterRoleBinding v1ClusterRoleBinding = this.getClusterRoleBindingV1(cluster, name);

            if (v1ClusterRoleBinding != null) {
                return this.convertClusterRoleBindingData(v1ClusterRoleBinding, new JSON());
            } else {
                return null;
            }

        }else{
            throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter, "cluster/namespace/name is null.");
        }
    }

    public List<K8sClusterRoleBindingVO> getClusterRoleBindingsByRole(ClusterVO cluster, List<String> clusterRoleNames) throws Exception {

        List<K8sClusterRoleBindingVO> clusterRoleBindingsByRole = Lists.newArrayList();

        // clusterRoleBinding
        if (cluster != null && CollectionUtils.isNotEmpty(clusterRoleNames)) {
            List<K8sClusterRoleBindingVO> clusterRoleBindings = this.getClusterRoleBindings(cluster, null, null);

            for (K8sClusterRoleBindingVO crbRow : Optional.ofNullable(clusterRoleBindings).orElseGet(() ->Lists.newArrayList())) {
                // roleRef - ClusterRole
                if (K8sApiKindType.findKindTypeByValue(crbRow.getRoleRef().getKind()) == K8sApiKindType.CLUSTER_ROLE
                        && clusterRoleNames.contains(crbRow.getRoleRef().getName())
                ) {
                    clusterRoleBindingsByRole.add(crbRow);
                }
            }
        }

        return clusterRoleBindingsByRole;
    }

    /**
     * K8S ClusterRoleBinding 생성
     *
     * @param cluster
     * @param gui
     * @return
     * @throws Exception
     */
    public K8sClusterRoleBindingVO createClusterRoleBinding(ClusterVO cluster, ClusterRoleBindingGuiVO gui) throws Exception {

        this.createClusterRoleBinding(cluster, gui, false);
        Thread.sleep(100);

        return this.getClusterRoleBinding(cluster, gui.getName());
    }

    public void createClusterRoleBinding(ClusterVO cluster, ClusterRoleBindingGuiVO gui, boolean dryRun) throws Exception {
        V1ClusterRoleBinding v1ClusterRoleBinding = K8sSpecFactory.buildClusterRoleBindingV1(gui);
        k8sWorker.createClusterRoleBindingV1(cluster, v1ClusterRoleBinding, dryRun);
    }

    /**
     * ClusterRoleBinding 생성 (yaml)
     *
     * @param cluster
     * @param yamlStr
     * @throws Exception
     */
    public K8sClusterRoleBindingVO createClusterRoleBinding(ClusterVO cluster, String yamlStr) throws Exception {
        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

        if (apiKindType == K8sApiKindType.CLUSTER_ROLE_BINDING && apiGroupType == K8sApiGroupType.RBAC_AUTHORIZATION && apiVerType == K8sApiType.V1) {
            V1ClusterRoleBinding createCrb = Yaml.loadAs(yamlStr, V1ClusterRoleBinding.class);

            // 현재 ClusterRoleBinding 조회
            V1ClusterRoleBinding currentCrb = k8sWorker.getClusterRoleBindingV1(cluster, createCrb.getMetadata().getName());
            if(currentCrb != null){
                throw new CocktailException("ClusterRoleBinding already exists!!", ExceptionType.ClusterRoleBindingNameAlreadyExists);
            }

            // 생성
            k8sWorker.createClusterRoleBindingV1(cluster, createCrb, false);
            Thread.sleep(100);

            return this.getClusterRoleBinding(cluster, createCrb.getMetadata().getName());
        } else {
            throw new CocktailException("Invalid API Kind, Group, Version,", ExceptionType.InvalidYamlData
                , String.format("%s: %s, %s: %s", KubeConstants.APIVSERION, MapUtils.getString(objMap, KubeConstants.APIVSERION), KubeConstants.KIND, MapUtils.getString(objMap, KubeConstants.KIND)));
        }
    }

    /**
     * ClusterRoleBinding 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1ClusterRoleBinding createClusterRoleBindingV1(ClusterVO cluster, V1ClusterRoleBinding param) throws Exception{
        return this.createClusterRoleBindingV1(cluster, param, false);
    }

    /**
     * ClusterRoleBinding 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1ClusterRoleBinding createClusterRoleBindingV1(ClusterVO cluster, V1ClusterRoleBinding param, boolean dryRun) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        K8sClusterRoleBindingVO k8sClusterRoleBinding = this.getClusterRoleBinding(cluster, param.getMetadata().getName());
        if(k8sClusterRoleBinding != null){
            throw new CocktailException("ClusterRoleBinding already exists!!", ExceptionType.ClusterRoleBindingNameAlreadyExists);
        }

        V1ClusterRoleBinding result = k8sWorker.createClusterRoleBindingV1(cluster, param, dryRun);

        return result;
    }

    /**
     * ClusterRoleBinding 체크
     *
     * @param cluster
     * @param isAdd
     * @param gui
     * @throws Exception
     */
    public void checkClusterRoleBinding(ClusterVO cluster, boolean isAdd, ClusterRoleBindingGuiVO gui) throws Exception {
        K8sClusterRoleBindingVO k8sCr = null;
        if (isAdd) {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("ClusterRoleBinding name is invalid", ExceptionType.K8sClusterRoleBindingNameInvalid, ResourceUtil.getInvalidNameMsg("ClusterRoleBinding name is invalid"));
            }
        } else {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("ClusterRoleBinding name is invalid", ExceptionType.K8sClusterRoleBindingNameInvalid, ResourceUtil.getInvalidNameMsg("ClusterRoleBinding name is invalid"));
            } else {
                k8sCr = this.getClusterRoleBinding(cluster, gui.getName());
                if (k8sCr == null) {
                    throw new CocktailException("ClusterRoleBinding not found!!", ExceptionType.K8sClusterRoleBindingNotFound);
                }
            }
        }
    }

    public void checkClusterRoleBindingName(ClusterVO cluster, boolean isAdd, String name) throws Exception {
        ClusterRoleBindingGuiVO gui = new ClusterRoleBindingGuiVO();
        gui.setDeployType("GUI");
        gui.setName(name);

        this.checkClusterRoleBinding(cluster, isAdd, gui);
    }

    /**
     * ClusterRoleBinding Yaml to GUI
     *
     * @param cluster
     * @param yamlStr
     * @return
     * @throws Exception
     */
    public ClusterRoleBindingGuiVO convertClusterRoleBindingYamlToGui(ClusterVO cluster, String yamlStr) throws Exception {

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

            if (apiKindType == K8sApiKindType.CLUSTER_ROLE_BINDING && apiGroupType == K8sApiGroupType.RBAC_AUTHORIZATION && apiVerType == K8sApiType.V1) {
                V1ClusterRoleBinding updatedCrb = Yaml.loadAs(yamlStr, V1ClusterRoleBinding.class);
                return this.convertClusterRoleBindingData(updatedCrb, new JSON());
            } else {
                throw new CocktailException("Yaml is invalid.(it is not ClusterRoleBinding).", ExceptionType.InvalidYamlData);
            }
        }

        return null;
    }

    /*************************************** Role ***************************************/
    /**
     * K8S Role 생성
     *
     * @param cluster
     * @param gui
     * @return
     * @throws Exception
     */
    public K8sRoleVO createRole(ClusterVO cluster, RoleGuiVO gui) throws Exception {

        this.createRole(cluster, gui, false);
        Thread.sleep(100);

        return this.getRole(cluster, gui.getNamespace(), gui.getName());
    }

    public void createRole(ClusterVO cluster, RoleGuiVO gui, boolean dryRun) throws Exception {
        V1Role v1Role = K8sSpecFactory.buildRoleV1(gui);
        k8sWorker.createRoleV1(cluster, v1Role.getMetadata().getNamespace(), v1Role, dryRun);
    }

    /**
     * Role 생성 (yaml)
     *
     * @param cluster
     * @param yamlStr
     * @throws Exception
     */
    public K8sRoleVO createRole(ClusterVO cluster, String namespace, String yamlStr) throws Exception {
        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

        if (apiKindType == K8sApiKindType.ROLE && apiGroupType == K8sApiGroupType.RBAC_AUTHORIZATION && apiVerType == K8sApiType.V1) {
            V1Role createCr = Yaml.loadAs(yamlStr, V1Role.class);

            // 현재 Role 조회
            V1Role currentCr = k8sWorker.getRoleV1(cluster, namespace, createCr.getMetadata().getName());
            if(currentCr != null){
                throw new CocktailException("Role already exists!!", ExceptionType.RoleNameAlreadyExists);
            }

            // 생성
            k8sWorker.createRoleV1(cluster, namespace, createCr, false);
            Thread.sleep(100);

            return this.getRole(cluster, namespace, createCr.getMetadata().getName());
        } else {
            throw new CocktailException("Invalid API Kind, Group, Version,", ExceptionType.InvalidYamlData
                    , String.format("%s: %s, %s: %s", KubeConstants.APIVSERION, MapUtils.getString(objMap, KubeConstants.APIVSERION), KubeConstants.KIND, MapUtils.getString(objMap, KubeConstants.KIND)));
        }
    }

    /**
     * Role 체크
     *
     * @param cluster
     * @param isAdd
     * @param gui
     * @throws Exception
     */
    public void checkRole(ClusterVO cluster, boolean isAdd, RoleGuiVO gui) throws Exception {
        K8sRoleVO k8sCr = null;
        if (isAdd) {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Role name is invalid", ExceptionType.K8sRoleNameInvalid, ResourceUtil.getInvalidNameMsg("Role name is invalid"));
            }
        } else {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Role name is invalid", ExceptionType.K8sRoleNameInvalid, ResourceUtil.getInvalidNameMsg("Role name is invalid"));
            } else {
                k8sCr = this.getRole(cluster, gui.getNamespace(), gui.getName());
                if (k8sCr == null) {
                    throw new CocktailException("Role not found!!", ExceptionType.K8sRoleNotFound);
                }
            }
        }
    }

    public void checkRoleName(ClusterVO cluster, boolean isAdd, String namespace, String name) throws Exception {
        RoleGuiVO gui = new RoleGuiVO();
        gui.setDeployType("GUI");
        gui.setNamespace(namespace);
        gui.setName(name);

        this.checkRole(cluster, isAdd, gui);
    }

    /**
     * Role Yaml to GUI
     *
     * @param cluster
     * @param namespace
     * @param yamlStr
     * @return
     * @throws Exception
     */
    public RoleGuiVO convertRoleYamlToGui(ClusterVO cluster, String namespace, String yamlStr) throws Exception {

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

            if (apiKindType == K8sApiKindType.ROLE && apiGroupType == K8sApiGroupType.RBAC_AUTHORIZATION && apiVerType == K8sApiType.V1) {
                V1Role updatedCr = Yaml.loadAs(yamlStr, V1Role.class);
                return this.convertRoleData(updatedCr, new JSON());
            } else {
                throw new CocktailException("Yaml is invalid.(it is not Role).", ExceptionType.InvalidYamlData);
            }
        }

        return null;
    }

    /**
     * Role 생성 (Invoke from Snapshot Deployment)
     * @param cluster
     * @param namespace
     * @param roles
     * @throws Exception
     */
    public void createMultipleRole(ClusterVO cluster, String namespace, List<CommonYamlVO> roles) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        if(CollectionUtils.isNotEmpty(roles)){
            for(CommonYamlVO role : roles){
                if(DeployType.valueOf(role.getDeployType()) == DeployType.GUI) {
                    log.error(String.format("DeployType.GUI not yet supported : createMultipleRole : %s", JsonUtils.toGson(role)));
                }
                else if(DeployType.valueOf(role.getDeployType()) == DeployType.YAML) {
                    try {
                        V1Role v1Role = ServerUtils.unmarshalYaml(role.getYaml(), K8sApiKindType.ROLE);
                        this.createRoleV1(cluster, namespace, v1Role);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Role Deployment Failure : createMultipleRole : %s\n%s", ex.getMessage(), JsonUtils.toGson(role)));
                    }
                }
                else {
                    log.error(String.format("Invalid DeployType : createMultipleRole : %s", JsonUtils.toGson(role)));
                }
            }
        }
    }

    public V1Role createRoleV1(ClusterVO cluster, String namespace, V1Role role) throws Exception{
        return this.createRoleV1(cluster, namespace, role, false);
    }

    /**
     * Role 생성
     * @param cluster
     * @param namespace
     * @param role
     * @return
     * @throws Exception
     */
    public V1Role createRoleV1(ClusterVO cluster, String namespace, V1Role role, boolean dryRun) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        V1Role v1Role = k8sWorker.getRoleV1(cluster, namespace, role.getMetadata().getName());
        if(v1Role != null){
            throw new CocktailException("Role already exists!!", ExceptionType.RoleNameAlreadyExists);
        }

        role = k8sWorker.createRoleV1(cluster, namespace, role, dryRun);

        return role;
    }

    /**
     * K8S Role 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return List<K8sRoleVO>
     * @throws Exception
     */
    public List<K8sRoleVO> getRoles(ClusterVO cluster, String namespace, String field, String label) throws Exception{

        try {
            if(cluster != null){
                return this.convertRoleDataList(cluster, namespace, field, label);
            }else{
                throw new CocktailException("cluster or namespace is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getRoles fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<V1Role> getRolesV1(Integer clusterSeq, String namespace, String field, String label) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getRolesV1(cluster, namespace, field, label);
    }

    /**
     * Role List 조회
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1Role> getRolesV1(ClusterVO cluster, String namespace, String field, String label) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster or namespace is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getRolesV1(cluster, namespace, field, label);
        } catch (Exception e) {
            throw new CocktailException("getRolesV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Role 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public K8sRoleVO getRole(ClusterVO cluster, String namespace, String name) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){

            V1Role v1Role = this.getRoleV1(cluster, namespace, name);

            if (v1Role != null) {
                return this.convertRoleData(v1Role, new JSON());
            } else {
                return null;
            }

        }else{
            throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter, "cluster/namespace/name is null.");
        }
    }

    /**
     * Role 상세 조회
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Role getRoleV1(ClusterVO cluster, String namespace, String name) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getRoleV1(cluster, namespace, name);
        } catch (Exception e) {
            throw new CocktailException("getRoleV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }


    /**
     * K8S Role 정보 조회 후 V1Role -> K8sRoleVO 변환 (목록)
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sRoleVO> convertRoleDataList(ClusterVO cluster, String namespace, String field, String label) throws Exception {

        List<K8sRoleVO> crs = new ArrayList<>();

        List<V1Role> v1Roles = this.getRolesV1(cluster, namespace, field, label);

        if (CollectionUtils.isNotEmpty(v1Roles)) {

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for (V1Role item : v1Roles) {
                crs.add(this.convertRoleData(item, k8sJson));
            }

        }

        return crs;
    }

    /**
     * K8S Role 정보 조회 후 V1Role -> K8sRoleVO 변환
     *
     * @param v1Role
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sRoleVO convertRoleData(V1Role v1Role, JSON k8sJson) throws Exception {

        K8sRoleVO role = new K8sRoleVO();
        if(v1Role != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            role.setName(v1Role.getMetadata().getName());
            role.setNamespace(v1Role.getMetadata().getNamespace());
            role.setLabels(v1Role.getMetadata().getLabels());
            role.setAnnotations(v1Role.getMetadata().getAnnotations());
            role.setCreationTimestamp(v1Role.getMetadata().getCreationTimestamp());

            // description
            role.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1Role.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // Rules
            if (CollectionUtils.isNotEmpty(v1Role.getRules())) {
                String toJson = k8sJson.serialize(v1Role.getRules());
                role.setRules(k8sJson.getGson().fromJson(toJson, new TypeToken<List<PolicyRuleVO>>(){}.getType()));
            }

            role.setDeployment(k8sJson.serialize(v1Role));
            role.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1Role));
        }

        return role;
    }

    /**
     * K8S Role Patch
     *
     * @param cluster
     * @param gui
     * @return
     * @throws Exception
     */
    public K8sRoleVO patchRole(ClusterVO cluster, RoleGuiVO gui, boolean dryRun) throws Exception {

        K8sRoleVO k8sCr = null;

        V1Role currCr = k8sWorker.getRoleV1(cluster, gui.getNamespace(), gui.getName());

        if (currCr != null) {

            V1Role updatedCr = K8sSpecFactory.buildRoleV1(gui);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currCr, updatedCr);

            String updateCrName = "";
            String updateCrNamespace = "";
            if (updatedCr != null && updatedCr.getMetadata() != null ){
                updateCrName = updatedCr.getMetadata().getName();
                updateCrNamespace = updatedCr.getMetadata().getNamespace();
            }

            updatedCr = this.patchRoleV1(cluster, updateCrNamespace, updateCrName, patchBody, dryRun);
            Thread.sleep(100);

            k8sCr = this.getRole(cluster, updateCrNamespace, updateCrName);

        }

        return k8sCr;
    }

    public V1Role patchRoleV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody) throws Exception{
        return this.patchRoleV1(cluster, namespace, name, patchBody, false);
    }

    public K8sRoleVO patchRole(ClusterVO cluster, String namespace, V1Role updated, boolean dryRun) throws Exception {
        // 현재 Role 조회
        V1Role curr = k8sWorker.getRoleV1(cluster, updated.getMetadata().getNamespace(), updated.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(curr, updated);
        log.debug("########## ClusterRole patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchRoleV1(cluster, updated.getMetadata().getNamespace(), updated.getMetadata().getName(), patchBody, dryRun);
        Thread.sleep(100);

        return this.getRole(cluster, updated.getMetadata().getNamespace(), updated.getMetadata().getName());
    }

    public K8sRoleVO patchRole(ClusterVO cluster, String namespace, String yamlStr, boolean dryRun) throws Exception {

        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        if (apiKindType == K8sApiKindType.ROLE) {
            V1Role updated = Yaml.loadAs(yamlStr, V1Role.class);
            return this.patchRole(cluster, namespace, updated, dryRun);
        }

        return null;
    }

    /**
     * Role patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @param dryRun
     * @return
     * @throws Exception
     */
    public V1Role patchRoleV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        return k8sWorker.patchRoleV1(cluster, namespace, name, patchBody, dryRun);

    }

    /**
     * Role 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteRole(ClusterVO cluster, String namespace, String name) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        k8sWorker.deleteRoleV1(cluster, namespace, name);

    }

    /**
     * 해당 ClusterRole, Role의 바인딩 사용 여부
     *
     * @param roleType
     * @param cluster
     * @param roleName
     * @param namespace
     * @param isThrow
     * @return
     * @throws Exception
     */
    public boolean canDeleteRoleWithBinding(K8sApiKindType roleType, ClusterVO cluster, String roleName, String namespace, boolean isThrow) throws Exception {

        boolean canDelete = true;

        if(roleType == null) {
            throw new CocktailException("kind is null.", ExceptionType.InvalidParameter);
        }
        if (!EnumSet.of(K8sApiKindType.CLUSTER_ROLE, K8sApiKindType.ROLE).contains(roleType)) {
            throw new CocktailException("kind is invalid. must be clusterRole, Role.", ExceptionType.InvalidParameter);
        }
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        if(K8sApiKindType.ROLE == roleType && StringUtils.isBlank(namespace)) {
            throw new CocktailException("namespace is null. if kind is Role, must be namespace.", ExceptionType.InvalidParameter);
        }
        if (StringUtils.isBlank(roleName)) {
            throw new CocktailException("roleName is null.", ExceptionType.InvalidParameter);
        }

        List<K8sClusterRoleBindingVO> clusterRoleBindings = Lists.newArrayList();
        List<K8sRoleBindingVO> roleBindings = Lists.newArrayList();
        if (K8sApiKindType.CLUSTER_ROLE == roleType) {
            clusterRoleBindings.addAll(Optional.ofNullable(this.getClusterRoleBindingsByRole(cluster, Arrays.asList(roleName))).orElseGet(() ->Lists.newArrayList()));
            roleBindings.addAll(Optional.ofNullable(this.getRoleBindingsByRole(cluster, Arrays.asList(roleName), null)).orElseGet(() ->Lists.newArrayList()));
        } else {
            Map<String, List<String>> roleNameMap = Maps.newHashMap();
            roleNameMap.put(namespace, Arrays.asList(roleName));
            roleBindings.addAll(Optional.ofNullable(this.getRoleBindingsByRole(cluster, null, roleNameMap)).orElseGet(() ->Lists.newArrayList()));
        }

        if (CollectionUtils.isNotEmpty(clusterRoleBindings) || CollectionUtils.isNotEmpty(roleBindings)) {
            List<Map<String, String>> msgs = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(clusterRoleBindings)) {
                Map<String, String> map = Maps.newHashMap();
                map.put("ClusterRoleBindings", clusterRoleBindings.stream().map(K8sClusterRoleBindingVO::getName).collect(Collectors.joining(",")));
                msgs.add(map);
            }
            if (CollectionUtils.isNotEmpty(roleBindings)) {
                Map<String, String> map = Maps.newHashMap();
                map.put("RoleBindings", roleBindings.stream().map(r -> (String.format("%s:%s", r.getNamespace(), r.getName()))).collect(Collectors.joining(",")));
                msgs.add(map);
            }
            if (isThrow) {
                ExceptionType exceptionType = K8sApiKindType.CLUSTER_ROLE == roleType ? ExceptionType.CanNotDeleteClusterRoleWithBinding : ExceptionType.CanNotDeleteRoleWithBinding;
                throw new CocktailException("Please delete the role binding first.", exceptionType, String.format("Please delete the role binding first.\n%s", JsonUtils.toGson(msgs)));
            } else {
                canDelete = false;
            }

        }

        return canDelete;
    }

    /*************************************** RoleBinding ***************************************/
    /**
     * K8S RoleBinding 정보 조회 후 V1RoleBinding -> K8sRoleBindingVO 변환 (목록)
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sRoleBindingVO> convertRoleBindingDataList(ClusterVO cluster, String namespace, String field, String label) throws Exception {

        List<K8sRoleBindingVO> rbs = new ArrayList<>();

        List<V1RoleBinding> v1RoleBindings = this.getRoleBindingsV1(cluster, namespace, field, label);

        if (CollectionUtils.isNotEmpty(v1RoleBindings)) {

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for (V1RoleBinding item : v1RoleBindings) {
                rbs.add(this.convertRoleBindingData(item, k8sJson));
            }

        }

        return rbs;
    }

    /**
     * K8S RoleBinding 정보 조회 후 V1RoleBinding -> K8sRoleBindingVO 변환
     *
     * @param v1RoleBinding
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sRoleBindingVO convertRoleBindingData(V1RoleBinding v1RoleBinding, JSON k8sJson) throws Exception {

        K8sRoleBindingVO roleBinding = new K8sRoleBindingVO();
        if(v1RoleBinding != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            roleBinding.setName(v1RoleBinding.getMetadata().getName());
            roleBinding.setNamespace(v1RoleBinding.getMetadata().getNamespace());
            roleBinding.setLabels(v1RoleBinding.getMetadata().getLabels());
            roleBinding.setAnnotations(v1RoleBinding.getMetadata().getAnnotations());
            roleBinding.setCreationTimestamp(v1RoleBinding.getMetadata().getCreationTimestamp());

            // description
            roleBinding.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1RoleBinding.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // RoleRef
            if (v1RoleBinding.getRoleRef() != null) {
                roleBinding.setRoleRef(JsonUtils.fromGson(k8sJson.serialize(v1RoleBinding.getRoleRef()), RoleRefVO.class));
            }

            // Subjects
            if (CollectionUtils.isNotEmpty(v1RoleBinding.getSubjects())) {
                String toJson = k8sJson.serialize(v1RoleBinding.getSubjects());
                roleBinding.setSubjects(k8sJson.getGson().fromJson(toJson, new TypeToken<List<SubjectVO>>(){}.getType()));
            }

            roleBinding.setDeployment(k8sJson.serialize(v1RoleBinding));
            roleBinding.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1RoleBinding));
        }

        return roleBinding;
    }

    /**
     * K8S RoleBinding 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return List<K8sRoleBindingVO>
     * @throws Exception
     */
    public List<K8sRoleBindingVO> getRoleBindings(ClusterVO cluster, String namespace, String field, String label) throws Exception{
        try {
            if(cluster != null){
                return this.convertRoleBindingDataList(cluster, namespace, field, label);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getRoleBindings fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * Role Binding List 조회
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1RoleBinding> getRoleBindingsV1(ClusterVO cluster, String namespace, String field, String label) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getRoleBindingsV1(cluster, namespace, field, label);
        } catch (Exception e) {
            throw new CocktailException("getRoleBindingV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<V1RoleBinding> getRoleBindingsV1(Integer clusterSeq, String namespace, String field, String label) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getRoleBindingsV1(cluster, namespace, field, label);
    }

    public List<K8sRoleBindingVO> getRoleBindingsByRole(ClusterVO cluster, List<String> clusterRoleNames, Map<String, List<String>> roleNameMap) throws Exception {

        List<K8sRoleBindingVO> roleBindingsByRole = Lists.newArrayList();

        // roleBinding
        if (CollectionUtils.isNotEmpty(clusterRoleNames) || MapUtils.isNotEmpty(roleNameMap)) {
            List<K8sRoleBindingVO> roleBindings = this.getRoleBindings(cluster, null, null, null);

            for (K8sRoleBindingVO rbRow : Optional.ofNullable(roleBindings).orElseGet(() ->Lists.newArrayList())) {
                // roleRef - Role
                if (MapUtils.isNotEmpty(roleNameMap)) {
                    if (K8sApiKindType.findKindTypeByValue(rbRow.getRoleRef().getKind()) == K8sApiKindType.ROLE
                            && (MapUtils.getObject(roleNameMap, rbRow.getNamespace(), null) != null
                            && roleNameMap.get(rbRow.getNamespace()).contains(rbRow.getRoleRef().getName()))
                    ) {
                        roleBindingsByRole.add(rbRow);
                    }
                }
                // roleRef - ClusterRole
                if (CollectionUtils.isNotEmpty(clusterRoleNames)) {
                    if (K8sApiKindType.findKindTypeByValue(rbRow.getRoleRef().getKind()) == K8sApiKindType.CLUSTER_ROLE
                                && clusterRoleNames.contains(rbRow.getRoleRef().getName())
                    ) {
                        roleBindingsByRole.add(rbRow);
                    }
                }
            }
        }

        return roleBindingsByRole;
    }

    /**
     * RoleBinding 생성 (Invoke from Snapshot Deployment)
     *
     * @param cluster
     * @param namespace
     * @param roleBindings
     * @throws Exception
     */
    public void createMultipleRoleBinding(ClusterVO cluster, String namespace, List<CommonYamlVO> roleBindings) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        if(CollectionUtils.isNotEmpty(roleBindings)){
            for(CommonYamlVO roleBinding : roleBindings){
                if(DeployType.valueOf(roleBinding.getDeployType()) == DeployType.GUI) {
                    log.error(String.format("DeployType.GUI not yet supported : createMultipleRoleBinding : %s", JsonUtils.toGson(roleBinding)));
                }
                else if(DeployType.valueOf(roleBinding.getDeployType()) == DeployType.YAML) {
                    try {
                        V1RoleBinding v1RoleBinding = ServerUtils.unmarshalYaml(roleBinding.getYaml(), K8sApiKindType.ROLE_BINDING);
                        this.createRoleBindingV1(cluster, namespace, v1RoleBinding);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("RoleBinding Deployment Failure : createMultipleRoleBinding : %s\n%s", ex.getMessage(), JsonUtils.toGson(roleBinding)));
                    }
                }
                else {
                    log.error(String.format("Invalid DeployType : createMultipleRoleBinding : %s", JsonUtils.toGson(roleBinding)));
                }
            }
        }
    }

    /**
     * K8S RoleBinding 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public K8sRoleBindingVO getRoleBinding(ClusterVO cluster, String namespace, String name) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){

            V1RoleBinding v1RoleBinding = this.getRoleBindingV1(cluster, namespace, name);

            if (v1RoleBinding != null) {
                return this.convertRoleBindingData(v1RoleBinding, new JSON());
            } else {
                return null;
            }

        }else{
            throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter, "cluster/namespace/name is null.");
        }
    }

    /**
     * RoleBinding 상세 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1RoleBinding getRoleBindingV1(ClusterVO cluster, String namespace, String name) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getRoleBindingV1(cluster, namespace, name);
        } catch (Exception e) {
            throw new CocktailException("getRoleBindingV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * RoleBinding 삭
     * @param cluster
     * @param namespace
     * @param name
     * @throws Exception
     */
    public void deleteRoleBinding(ClusterVO cluster, String namespace, String name) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        k8sWorker.deleteRoleBindingV1(cluster, namespace, name);

    }

    /**
     * K8S RoleBinding 생성
     *
     * @param cluster
     * @param gui
     * @return
     * @throws Exception
     */
    public K8sRoleBindingVO createRoleBinding(ClusterVO cluster, String namespace, RoleBindingGuiVO gui) throws Exception {
        if(StringUtils.isNotBlank(namespace)) {
            gui.setNamespace(namespace);
        }
        this.createRoleBinding(cluster, gui.getNamespace(), gui, false);
        Thread.sleep(100);

        return this.getRoleBinding(cluster, gui.getNamespace(), gui.getName());
    }

    public void createRoleBinding(ClusterVO cluster, String namespace, RoleBindingGuiVO gui, boolean dryRun) throws Exception {
        if(StringUtils.isNotBlank(namespace)) {
            gui.setNamespace(namespace);
        }
        V1RoleBinding v1RoleBinding = K8sSpecFactory.buildRoleBindingV1(gui);
        k8sWorker.createRoleBindingV1(cluster, namespace, v1RoleBinding, dryRun);
    }

    /**
     * RoleBinding 생성 (yaml)
     *
     * @param cluster
     * @param yamlStr
     * @throws Exception
     */
    public K8sRoleBindingVO createRoleBinding(ClusterVO cluster, String namespace, String yamlStr) throws Exception {
        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

        if (apiKindType == K8sApiKindType.ROLE_BINDING && apiGroupType == K8sApiGroupType.RBAC_AUTHORIZATION && apiVerType == K8sApiType.V1) {
            V1RoleBinding createRb = Yaml.loadAs(yamlStr, V1RoleBinding.class);

            // 현재 RoleBinding 조회
            V1RoleBinding currentRb = k8sWorker.getRoleBindingV1(cluster, namespace, createRb.getMetadata().getName());
            if(currentRb != null){
                throw new CocktailException("RoleBinding already exists!!", ExceptionType.RoleBindingNameAlreadyExists);
            }

            // 생성
            k8sWorker.createRoleBindingV1(cluster, namespace, createRb, false);
            Thread.sleep(100);

            return this.getRoleBinding(cluster, namespace, createRb.getMetadata().getName());
        } else {
            throw new CocktailException("Invalid API Kind, Group, Version,", ExceptionType.InvalidYamlData
                , String.format("%s: %s, %s: %s", KubeConstants.APIVSERION, MapUtils.getString(objMap, KubeConstants.APIVSERION), KubeConstants.KIND, MapUtils.getString(objMap, KubeConstants.KIND)));
        }
    }

    /**
     * RoleBinding 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1RoleBinding createRoleBindingV1(ClusterVO cluster, String namespace, V1RoleBinding param) throws Exception{
        return this.createRoleBindingV1(cluster, namespace, param, false);
    }

    /**
     * RoleBinding 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1RoleBinding createRoleBindingV1(ClusterVO cluster, String namespace, V1RoleBinding param, boolean dryRun) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        K8sRoleBindingVO k8sRoleBinding = this.getRoleBinding(cluster, namespace, param.getMetadata().getName());
        if(k8sRoleBinding != null){
            throw new CocktailException("RoleBinding already exists!!", ExceptionType.RoleBindingNameAlreadyExists);
        }

        V1RoleBinding result = k8sWorker.createRoleBindingV1(cluster, namespace, param, dryRun);

        return result;
    }

    /**
     * K8S RoleBinding Patch GUI
     *
     * @param cluster
     * @param gui
     * @return
     * @throws Exception
     */
    public K8sRoleBindingVO patchRoleBinding(ClusterVO cluster, String namespace, RoleBindingGuiVO gui, boolean dryRun) throws Exception {

        K8sRoleBindingVO k8sRb = null;

        V1RoleBinding currRb = k8sWorker.getRoleBindingV1(cluster, namespace, gui.getName());

        if (currRb != null) {

            V1RoleBinding updatedRb = K8sSpecFactory.buildRoleBindingV1(gui);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currRb, updatedRb);

            String updatedRbName = "";
            if (updatedRb != null && updatedRb.getMetadata() != null){
                updatedRbName = updatedRb.getMetadata().getName();
            }
            updatedRb = this.patchRoleBindingV1(cluster, namespace, updatedRbName, patchBody, dryRun);
            Thread.sleep(100);

            k8sRb = this.getRoleBinding(cluster, namespace, updatedRbName);

        }

        return k8sRb;
    }

    /**
     * K8S RoleBinding Patch YAML
     *
     * @param cluster
     * @param yamlStr
     * @param dryRun
     * @return
     * @throws Exception
     */
    public K8sRoleBindingVO patchRoleBinding(ClusterVO cluster, String namespace, String yamlStr, boolean dryRun) throws Exception {

        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        if (apiKindType == K8sApiKindType.ROLE_BINDING) {
            V1RoleBinding updated = Yaml.loadAs(yamlStr, V1RoleBinding.class);
            return this.patchRoleBinding(cluster, namespace, updated, dryRun);
        }
        else {
            throw new CocktailException("Yaml is invalid.(it is not RoleBinding).", ExceptionType.InvalidYamlData);
        }
    }

    public K8sRoleBindingVO patchRoleBinding(ClusterVO cluster, String namespace, V1RoleBinding updated, boolean dryRun) throws Exception {
        // 현재 RoleBinding 조회
        V1RoleBinding curr = k8sWorker.getRoleBindingV1(cluster, namespace, updated.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(curr, updated);
        log.debug("########## RoleBinding patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchRoleBindingV1(cluster, namespace, updated.getMetadata().getName(), patchBody, dryRun);
        Thread.sleep(100);

        return this.getRoleBinding(cluster, namespace, updated.getMetadata().getName());
    }


    /**
     * RoleBinding patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1RoleBinding patchRoleBindingV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody) throws Exception{
        return this.patchRoleBindingV1(cluster, namespace, name, patchBody, false);
    }

    public V1RoleBinding patchRoleBindingV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        return k8sWorker.patchRoleBindingV1(cluster, namespace, name, patchBody, dryRun);

    }


    /**
     * RoleBinding 체크
     */
    public void checkRoleBindingValidator(ClusterVO cluster, String namespace, boolean isAdd, RoleBindingGuiVO gui) throws Exception {
        if(!StringUtils.equalsIgnoreCase(namespace, gui.getNamespace())) { // Namespace 입력이 서로 다르면 오류.
            log.error(String.format("Different Namespace [ %s : %s ]", namespace, gui.getNamespace()), ExceptionType.NamespaceNameInvalid);
        }
        this.checkRoleBinding(cluster, isAdd, gui);
    }
    /**
     * RoleBinding 체크
     *
     * @param cluster
     * @param isAdd
     * @param gui
     * @throws Exception
     */
    public void checkRoleBinding(ClusterVO cluster, boolean isAdd, RoleBindingGuiVO gui) throws Exception {
        K8sRoleBindingVO k8sCr = null;
        if (isAdd) {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("RoleBinding name is invalid", ExceptionType.K8sRoleBindingNameInvalid, ResourceUtil.getInvalidNameMsg("RoleBinding name is invalid"));
            }
        } else {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("RoleBinding name is invalid", ExceptionType.K8sRoleBindingNameInvalid, ResourceUtil.getInvalidNameMsg("RoleBinding name is invalid"));
            } else {
                k8sCr = this.getRoleBinding(cluster, gui.getNamespace(), gui.getName());
                if (k8sCr == null) {
                    throw new CocktailException("RoleBinding not found!!", ExceptionType.K8sRoleBindingNotFound);
                }
            }
        }
    }

    public void checkRoleBindingName(ClusterVO cluster, boolean isAdd, String namespace, String name) throws Exception {
        RoleBindingGuiVO gui = new RoleBindingGuiVO();
        gui.setDeployType("GUI");
        gui.setNamespace(namespace);
        gui.setName(name);

        this.checkRoleBinding(cluster, isAdd, gui);
    }

    /**
     * RoleBinding Yaml to GUI
     *
     * @param yamlStr
     * @return
     * @throws Exception
     */
    public RoleBindingGuiVO convertRoleBindingYamlToGui(String yamlStr) throws Exception {

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(objMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

            if (apiKindType == K8sApiKindType.ROLE_BINDING && apiGroupType == K8sApiGroupType.RBAC_AUTHORIZATION && apiVerType == K8sApiType.V1) {
                V1RoleBinding updatedRb = Yaml.loadAs(yamlStr, V1RoleBinding.class);
                return this.convertRoleBindingData(updatedRb, new JSON());
            } else {
                throw new CocktailException("Yaml is invalid.(it is not RoleBinding).", ExceptionType.InvalidYamlData);
            }
        }

        return null;
    }

}
