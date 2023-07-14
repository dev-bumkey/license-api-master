package run.acloud.api.resource.service;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import io.kubernetes.client.custom.QuantityFormatter;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ResourceQuota;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.vo.ClusterVO;
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
import run.acloud.api.resource.vo.K8sResourceQuotaStatusVO;
import run.acloud.api.resource.vo.K8sResourceQuotaVO;
import run.acloud.api.resource.vo.ResourceQuotaGuiVO;
import run.acloud.api.resource.vo.ScopeSelectorVO;
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
public class ResourceQuotaService {

    public static final String LABEL = String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;



    /**
     * K8S ResourceQuota 생성
     *
     * @param cluster
     * @param rqGui
     * @param context
     * @return
     * @throws Exception
     */
    public K8sResourceQuotaVO createResourceQuota(ClusterVO cluster, ResourceQuotaGuiVO rqGui, ExecutingContextVO context) throws Exception {

        this.createResourceQuota(cluster, rqGui, false, context);
        Thread.sleep(100);

        return this.getResourceQuota(cluster, rqGui.getNamespace(), rqGui.getName(), context);
    }

    public void createResourceQuota(ClusterVO cluster, ResourceQuotaGuiVO rqGui, boolean dryRun, ExecutingContextVO context) throws Exception {
        if (rqGui != null) {
            V1ResourceQuota v1ResourceQuota = K8sSpecFactory.buildResourceQuotaV1(rqGui);
            k8sWorker.createResourceQuotaV1(cluster, rqGui.getNamespace(), v1ResourceQuota, dryRun);
        } else {
            throw new CocktailException("Invalid request ResourceQuotaGui info", ExceptionType.InvalidParameter);
        }

    }

    /**
     * ResourceQuota 생성 (yaml)
     *
     * @param cluster
     * @param namespace
     * @param yamlStr
     * @param context
     * @throws Exception
     */
    public K8sResourceQuotaVO createResourceQuota(ClusterVO cluster, String namespace, String yamlStr, ExecutingContextVO context) throws Exception {
        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

        if (apiKindType == K8sApiKindType.RESOURCE_QUOTA && apiVerType == K8sApiType.V1) {
            V1ResourceQuota createRq = Yaml.loadAs(yamlStr, V1ResourceQuota.class);

            // 현재 ResourceQuota 조회
            V1ResourceQuota currentRq = k8sWorker.getResourceQuotaV1(cluster, namespace, createRq.getMetadata().getName());
            if(currentRq != null){
                throw new CocktailException("ResourceQuota already exists!!", ExceptionType.ResourceQuotaNameAlreadyExists);
            }

            // 생성
            k8sWorker.createResourceQuotaV1(cluster, namespace, createRq, false);

            return this.getResourceQuota(cluster, createRq.getMetadata().getNamespace(), createRq.getMetadata().getName(), context);
        } else {
            throw new CocktailException("Invalid API Kind, Group, Version,", ExceptionType.InvalidYamlData
                    , String.format("%s: %s, %s: %s", KubeConstants.APIVSERION, MapUtils.getString(objMap, KubeConstants.APIVSERION), KubeConstants.KIND, MapUtils.getString(objMap, KubeConstants.KIND)));
        }

    }

    /**
     * K8S ResourceQuota Patch
     *
     * @param cluster
     * @param rqGui
     * @param context
     * @return
     * @throws Exception
     */
    public K8sResourceQuotaVO patchResourceQuota(ClusterVO cluster, ResourceQuotaGuiVO rqGui, boolean dryRun, ExecutingContextVO context) throws Exception {

        K8sResourceQuotaVO k8sRq = null;

        V1ResourceQuota currRq = k8sWorker.getResourceQuotaV1(cluster, rqGui.getNamespace(), rqGui.getName());

        if (currRq != null) {

            V1ResourceQuota updatedRq = K8sSpecFactory.buildResourceQuotaV1(rqGui);
            // 화면에서 안넘어오는 scopeSelector, scopes 을 현재 값으로 셋팅
            updatedRq.getSpec().setScopeSelector(currRq.getSpec().getScopeSelector());
            updatedRq.getSpec().setScopes(currRq.getSpec().getScopes());

            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currRq, updatedRq);

            updatedRq = k8sWorker.patchResourceQuotaV1(cluster, updatedRq.getMetadata().getNamespace(), updatedRq.getMetadata().getName(), patchBody, dryRun);
            Thread.sleep(100);

            k8sRq = this.getResourceQuota(cluster, updatedRq.getMetadata().getNamespace(), updatedRq.getMetadata().getName(), context);

        }

        return k8sRq;
    }

    public K8sResourceQuotaVO patchResourceQuota(ClusterVO cluster, V1ResourceQuota updatedRq, boolean dryRun, ExecutingContextVO context) throws Exception {
        // 현재 ResourceQuota 조회
        V1ResourceQuota currRq = k8sWorker.getResourceQuotaV1(cluster, updatedRq.getMetadata().getNamespace(), updatedRq.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currRq, updatedRq);
        log.debug("##########  ResourceQuota patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchResourceQuotaV1(cluster, updatedRq.getMetadata().getNamespace(), updatedRq.getMetadata().getName(), patchBody, dryRun);
        Thread.sleep(100);

        return this.getResourceQuota(cluster, updatedRq.getMetadata().getNamespace(), updatedRq.getMetadata().getName(), context);
    }

    public K8sResourceQuotaVO patchResourceQuota(ClusterVO cluster, String yamlStr, boolean dryRun, ExecutingContextVO context) throws Exception {
        Map<String, Object> lrObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(lrObjMap);
        if (apiKindType == K8sApiKindType.RESOURCE_QUOTA) {
            V1ResourceQuota updatedRq = Yaml.loadAs(yamlStr, V1ResourceQuota.class);
            return this.patchResourceQuota(cluster, updatedRq, dryRun, ContextHolder.exeContext());
        }

        return null;
    }

    public K8sResourceQuotaVO patchResourceQuota(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun, ExecutingContextVO context) throws Exception {
        // patch
        k8sWorker.patchResourceQuotaV1(cluster, namespace, name, patchBody, dryRun);
        Thread.sleep(100);

        return this.getResourceQuota(cluster, namespace, name, context);
    }

    /**
     * K8S ResourceQuota 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @throws Exception
     */
    public void deleteResourceQuota(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception {

        V1ResourceQuota v1ResourceQuota = k8sWorker.getResourceQuotaV1(cluster, namespace, name);

        if (v1ResourceQuota != null) {
            k8sWorker.deleteResourceQuotaV1(cluster, namespace, name);
            Thread.sleep(500);
        }

    }

    /**
     * K8S ResourceQuota 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return List<K8sResourceQuotaVO>
     * @throws Exception
     */
    public List<K8sResourceQuotaVO> getResourceQuotas(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){
                return this.convertResourceQuotaDataList(cluster, namespace, field, label, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getResourceQuotas fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S ResourceQuota 정보 조회
     *
     * @param cluster
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sResourceQuotaVO getResourceQuota(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(name)){

            V1ResourceQuota v1ResourceQuota = k8sWorker.getResourceQuotaV1(cluster, namespace, name);

            if (v1ResourceQuota != null) {
                return this.convertResourceQuotaData(v1ResourceQuota, new JSON());
            } else {
                return null;
            }

        }else{
            throw new CocktailException("cluster/name is null.", ExceptionType.InvalidParameter, "cluster/name is null.");
        }
    }

    /**
     * K8S ResourceQuota 정보 조회 후 V1ResourceQuota -> K8sResourceQuotaVO 변환
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sResourceQuotaVO> convertResourceQuotaDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sResourceQuotaVO> rqs = new ArrayList<>();

        List<V1ResourceQuota> v1ResourceQuotas = k8sWorker.getResourceQuotasV1(cluster, namespace, field, label);

        if (CollectionUtils.isNotEmpty(v1ResourceQuotas)) {

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for (V1ResourceQuota item : v1ResourceQuotas) {
                rqs.add(this.convertResourceQuotaData(item, k8sJson));
            }

        }

        return rqs;
    }

    /**
     * K8S ResourceQuota 정보 조회 후 V1ResourceQuota -> K8sResourceQuotaVO 변환
     *
     * @param v1ResourceQuota
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sResourceQuotaVO convertResourceQuotaData(V1ResourceQuota v1ResourceQuota, JSON k8sJson) throws Exception {

        K8sResourceQuotaVO resourceQuota = new K8sResourceQuotaVO();
        if(v1ResourceQuota != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            resourceQuota.setNamespace(v1ResourceQuota.getMetadata().getNamespace());
            resourceQuota.setName(v1ResourceQuota.getMetadata().getName());
            resourceQuota.setLabels(v1ResourceQuota.getMetadata().getLabels());
            resourceQuota.setAnnotations(v1ResourceQuota.getMetadata().getAnnotations());
            resourceQuota.setCreationTimestamp(v1ResourceQuota.getMetadata().getCreationTimestamp());

            // description
            resourceQuota.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1ResourceQuota.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // 기본 여부
            resourceQuota.setDefault(ResourceUtil.isDefault(Optional.ofNullable(v1ResourceQuota.getMetadata().getLabels()).orElseGet(() ->Maps.newHashMap())));

            // hard
            if (MapUtils.isNotEmpty(v1ResourceQuota.getSpec().getHard())) {
                resourceQuota.setHard(JsonUtils.fromGson(k8sJson.serialize(v1ResourceQuota.getSpec().getHard()), Map.class));
            }

            // scopeSelector
            resourceQuota.setScopeSelector(JsonUtils.fromGson(k8sJson.serialize(v1ResourceQuota.getSpec().getScopeSelector()), ScopeSelectorVO.class));

            // scopes
            resourceQuota.setScopes(v1ResourceQuota.getSpec().getScopes());

            // status
            resourceQuota.setStatus(JsonUtils.fromGson(k8sJson.serialize(v1ResourceQuota.getStatus()), K8sResourceQuotaStatusVO.class));

            resourceQuota.setDeployment(k8sJson.serialize(v1ResourceQuota));
            resourceQuota.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1ResourceQuota));
        }

        return resourceQuota;
    }

    /**
     * 기본 ResourceQuota 생성
     *
     * @param cluster
     * @param namespaceName
     * @throws Exception
     */
    public void createDefaultResourceQuota(ClusterVO cluster, String namespaceName) throws Exception{
        this.createDefaultResourceQuota(cluster, namespaceName, null, false);
    }

    public void createDefaultResourceQuota(ClusterVO cluster, String namespaceName, ResourceQuotaGuiVO gui, boolean dryRun) throws Exception{

        if (gui == null) {
            gui = new ResourceQuotaGuiVO();
            gui.setName(KubeConstants.RESOURCE_QUOTA_DEFAULT_NAME);
            gui.setNamespace(namespaceName);
            gui.setDescription("The resource quota is managed by a cocktail.");
            gui.putLabelsItem(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
            gui.setDefault(true);

            gui.putHardItem(String.format("%s.%s", KubeConstants.RESOURCES_LIMIT, KubeConstants.RESOURCES_CPU), "2");
            gui.putHardItem(String.format("%s.%s", KubeConstants.RESOURCES_LIMIT, KubeConstants.RESOURCES_MEMORY), "1Gi");
            gui.putHardItem("pods", "10");
        }

        K8sResourceQuotaVO k8sRq = this.getDefaultResourceQuota(cluster, gui.getNamespace());
        if (k8sRq == null) {
            // 생성
            this.createResourceQuota(cluster, gui, ContextHolder.exeContext());
        } else {
            // 이미 존재한다면 관리 라벨만 추가
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(k8sRq.getLabels());
            this.patchResourceQuota(cluster, gui.getNamespace(), gui.getName(), patchBody, dryRun, ContextHolder.exeContext());
        }
    }

    /**
     * 기본 ResourceQuota 상세 조회
     *
     * @param cluster
     * @param namespacee
     * @return
     * @throws Exception
     */
    public K8sResourceQuotaVO getDefaultResourceQuota(ClusterVO cluster, String namespacee) throws Exception {

        List<K8sResourceQuotaVO> list = this.getResourceQuotas(cluster, namespacee, null, LABEL, ContextHolder.exeContext());
        K8sResourceQuotaVO result = null;
        if (CollectionUtils.isNotEmpty(list)) {
            result = list.get(0);
        }

        return result;
    }

    /**
     * ResourceQuota 체크
     *
     * @param cluster
     * @param namespaceName
     * @param isAdd
     * @param gui
     * @throws Exception
     */
    public void checkResourceQuota(ClusterVO cluster, String namespaceName, boolean isAdd, ResourceQuotaGuiVO gui) throws Exception {
        K8sResourceQuotaVO k8sRq = null;
        QuantityFormatter quantityFormatter = new QuantityFormatter();
        if (isAdd) {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("ResourceQuota name is invalid", ExceptionType.K8sResourceQuotaNameInvalid, ResourceUtil.getInvalidNameMsg("ResourceQuota name is invalid"));
            } else {
                /**
                 * 기본 ResourceQuota 생성일 경우 '기본' 라벨로 조회하고
                 * 조회된 값이 있을 경우 이름도 같은 지 체크함.
                 */
                if (gui.isDefault()) {
                    k8sRq = this.getDefaultResourceQuota(cluster, namespaceName);
                } else {
                    k8sRq = this.getResourceQuota(cluster, namespaceName, gui.getName(), ContextHolder.exeContext());
                }
                if (k8sRq != null) {
                    boolean isAlready = false;
                    if (gui.isDefault()) {
                        if (StringUtils.equals(gui.getName(), k8sRq.getName())) {
                            isAlready = true;
                        }
                    } else {
                        isAlready = true;
                    }
                    if (isAlready) {
                        throw new CocktailException("ResourceQuota already exists!!", ExceptionType.ResourceQuotaNameAlreadyExists);
                    }
                }
            }
        } else {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("ResourceQuota name is invalid", ExceptionType.K8sResourceQuotaNameInvalid, ResourceUtil.getInvalidNameMsg("ResourceQuota name is invalid"));
            } else {
                /**
                 * 기본 ResourceQuota 수정일 경우 '기본' 라벨로 조회하고
                 * 조회된 값이 있을 경우 이름도 같은 지 체크함.
                 */
                if (gui.isDefault()) {
                    k8sRq = this.getDefaultResourceQuota(cluster, namespaceName);
                } else {
                    k8sRq = this.getResourceQuota(cluster, namespaceName, gui.getName(), ContextHolder.exeContext());
                }
                boolean notFound = false;
                if (k8sRq == null) {
                    notFound = true;
                } else {
                    if (gui.isDefault()) {
                        if (!StringUtils.equals(gui.getName(), k8sRq.getName())) {
                            notFound = true;
                        }
                    }
                }

                if (notFound) {
                    throw new CocktailException("ResourceQuota not found!!", ExceptionType.K8sResourceQuotaNotFound);
                }
            }
        }

        // valid Quantity Resource Value
        ResourceUtil.validQuantityResourceValue(gui.getHard(), quantityFormatter);
    }

    /**
     * Yaml to GUI
     *
     * @param cluster
     * @param yamlStr
     * @return
     * @throws Exception
     */
    public ResourceQuotaGuiVO convertYamlToGui(ClusterVO cluster, String yamlStr) throws Exception {

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> pspObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(pspObjMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(pspObjMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(pspObjMap);

            if (apiKindType == K8sApiKindType.RESOURCE_QUOTA) {
                V1ResourceQuota updatedRq = Yaml.loadAs(yamlStr, V1ResourceQuota.class);
                return this.convertResourceQuotaData(updatedRq, new JSON());
            } else {
                throw new CocktailException("Yaml is invalid.(it is not ResourceQuota).", ExceptionType.InvalidYamlData);
            }
        }

        return null;
    }
}
