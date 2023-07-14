package run.acloud.api.resource.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.QuantityFormatter;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1LimitRange;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
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
import run.acloud.api.resource.enums.LimitRangeType;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.K8sLimitRangeVO;
import run.acloud.api.resource.vo.LimitRangeGuiVO;
import run.acloud.api.resource.vo.LimitRangeItemVO;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class LimitRangeService {

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;


    /**
     * K8S LimitRange 생성
     *
     * @param cluster
     * @param lrGui
     * @param context
     * @return
     * @throws Exception
     */
    public K8sLimitRangeVO createLimitRange(ClusterVO cluster, LimitRangeGuiVO lrGui, ExecutingContextVO context) throws Exception {

        this.createLimitRange(cluster, lrGui, false, context);
        Thread.sleep(100);

        return this.getLimitRange(cluster, lrGui.getNamespace(), lrGui.getName(), context);
    }

    public void createLimitRange(ClusterVO cluster, LimitRangeGuiVO lrGui, boolean dryRun, ExecutingContextVO context) throws Exception {
        V1LimitRange v1LimitRange = K8sSpecFactory.buildLimitRangeV1(lrGui);
        k8sWorker.createLimitRangeV1(cluster, lrGui.getNamespace(), v1LimitRange, dryRun);
    }

    /**
     * LimitRange 생성 (yaml)
     *
     * @param cluster
     * @param namespace
     * @param yamlStr
     * @param context
     * @throws Exception
     */
    public K8sLimitRangeVO createLimitRange(ClusterVO cluster, String namespace, String yamlStr, ExecutingContextVO context) throws Exception {
        Map<String, Object> objMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(objMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(objMap);

        if (apiKindType == K8sApiKindType.LIMIT_RANGE && apiVerType == K8sApiType.V1) {
            V1LimitRange createLr = Yaml.loadAs(yamlStr, V1LimitRange.class);

            // 현재 LimitRange 조회
            V1LimitRange currentLr = k8sWorker.getLimitRangeV1(cluster, namespace, createLr.getMetadata().getName());
            if(currentLr != null){
                throw new CocktailException("LimitRange already exists!!", ExceptionType.LimitRangeNameAlreadyExists);
            }

            // 생성
            k8sWorker.createLimitRangeV1(cluster, namespace, createLr, false);

            return this.getLimitRange(cluster, createLr.getMetadata().getNamespace(), createLr.getMetadata().getName(), context);
        } else {
            throw new CocktailException("Invalid API Kind, Group, Version,", ExceptionType.InvalidYamlData
                    , String.format("%s: %s, %s: %s", KubeConstants.APIVSERION, MapUtils.getString(objMap, KubeConstants.APIVSERION), KubeConstants.KIND, MapUtils.getString(objMap, KubeConstants.KIND)));
        }

    }

    /**
     * K8S LimitRange Patch
     *
     * @param cluster
     * @param lrGui
     * @param context
     * @return
     * @throws Exception
     */
    public K8sLimitRangeVO patchLimitRange(ClusterVO cluster, LimitRangeGuiVO lrGui, boolean dryRun, ExecutingContextVO context) throws Exception {

        K8sLimitRangeVO k8sNp = null;

        V1LimitRange currLr = k8sWorker.getLimitRangeV1(cluster, lrGui.getNamespace(), lrGui.getName());

        if (currLr != null) {

            V1LimitRange updatedLr = K8sSpecFactory.buildLimitRangeV1(lrGui);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currLr, updatedLr);

            updatedLr = k8sWorker.patchLimitRangeV1(cluster, updatedLr.getMetadata().getNamespace(), updatedLr.getMetadata().getName(), patchBody, dryRun);
            Thread.sleep(100);

            k8sNp = this.getLimitRange(cluster, updatedLr.getMetadata().getNamespace(), updatedLr.getMetadata().getName(), context);

        }

        return k8sNp;
    }

    public K8sLimitRangeVO patchLimitRange(ClusterVO cluster, V1LimitRange updatedLr, boolean dryRun, ExecutingContextVO context) throws Exception {
        // 현재 LimitRange 조회
        V1LimitRange currLr = k8sWorker.getLimitRangeV1(cluster, updatedLr.getMetadata().getNamespace(), updatedLr.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currLr, updatedLr);
        log.debug("########## LimitRange patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchLimitRangeV1(cluster, updatedLr.getMetadata().getNamespace(), updatedLr.getMetadata().getName(), patchBody, dryRun);
        Thread.sleep(100);

        return this.getLimitRange(cluster, updatedLr.getMetadata().getNamespace(), updatedLr.getMetadata().getName(), context);
    }

    public K8sLimitRangeVO patchLimitRange(ClusterVO cluster, String yamlStr, boolean dryRun, ExecutingContextVO context) throws Exception {
        Map<String, Object> lrObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(lrObjMap);
        if (apiKindType == K8sApiKindType.LIMIT_RANGE) {
            V1LimitRange updatedLr = Yaml.loadAs(yamlStr, V1LimitRange.class);
            return this.patchLimitRange(cluster, updatedLr, dryRun, ContextHolder.exeContext());
        }

        return null;

    }

    public K8sLimitRangeVO patchLimitRange(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun, ExecutingContextVO context) throws Exception {
        // patch
        k8sWorker.patchLimitRangeV1(cluster, namespace, name, patchBody, dryRun);
        Thread.sleep(100);

        return this.getLimitRange(cluster, namespace, name, context);
    }

    /**
     * K8S LimitRange 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @throws Exception
     */
    public void deleteLimitRange(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception {

        V1LimitRange v1LimitRange = k8sWorker.getLimitRangeV1(cluster, namespace, name);

        if (v1LimitRange != null) {
            k8sWorker.deleteLimitRangeV1(cluster, namespace, name);
            Thread.sleep(500);
        }

    }

    /**
     * K8S LimitRange 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return List<K8sLimitRangeVO>
     * @throws Exception
     */
    public List<K8sLimitRangeVO> getLimitRanges(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){
                return this.convertLimitRangeDataList(cluster, namespace, field, label, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getLimitRanges fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S LimitRange 정보 조회
     *
     * @param cluster
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sLimitRangeVO getLimitRange(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(name)){

            V1LimitRange v1LimitRange = k8sWorker.getLimitRangeV1(cluster, namespace, name);

            if (v1LimitRange != null) {
                return this.convertLimitRangeData(v1LimitRange, new JSON());
            } else {
                return null;
            }

        }else{
            throw new CocktailException("cluster/name is null.", ExceptionType.InvalidParameter, "cluster/name is null.");
        }
    }

    /**
     * K8S LimitRange 정보 조회 후 V1LimitRange -> K8sLimitRangeVO 변환
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sLimitRangeVO> convertLimitRangeDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sLimitRangeVO> lrs = new ArrayList<>();

        List<V1LimitRange> v1LimitRanges = k8sWorker.getLimitRangesV1(cluster, namespace, field, label);

        if (CollectionUtils.isNotEmpty(v1LimitRanges)) {

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for (V1LimitRange item : v1LimitRanges) {
                lrs.add(this.convertLimitRangeData(item, k8sJson));
            }

        }

        return lrs;
    }

    /**
     * K8S LimitRange 정보 조회 후 V1LimitRange -> K8sLimitRangeVO 변환
     *
     * @param v1LimitRange
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sLimitRangeVO convertLimitRangeData(V1LimitRange v1LimitRange, JSON k8sJson) throws Exception {

        K8sLimitRangeVO limitRange = new K8sLimitRangeVO();
        if(v1LimitRange != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            limitRange.setNamespace(v1LimitRange.getMetadata().getNamespace());
            limitRange.setName(v1LimitRange.getMetadata().getName());
            limitRange.setLabels(v1LimitRange.getMetadata().getLabels());
            limitRange.setAnnotations(v1LimitRange.getMetadata().getAnnotations());
            limitRange.setCreationTimestamp(v1LimitRange.getMetadata().getCreationTimestamp());

            // description
            limitRange.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(v1LimitRange.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            // 기본 여부
            limitRange.setDefault(ResourceUtil.isDefault(Optional.ofNullable(v1LimitRange.getMetadata().getLabels()).orElseGet(() ->Maps.newHashMap())));

            // limits
            if (CollectionUtils.isNotEmpty(v1LimitRange.getSpec().getLimits())) {
                limitRange.setLimits(JsonUtils.fromGson(k8sJson.serialize(v1LimitRange.getSpec().getLimits()), new TypeToken<List<LimitRangeItemVO>>(){}.getType()));
            }

            limitRange.setDeployment(k8sJson.serialize(v1LimitRange));
            limitRange.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1LimitRange));
        }

        return limitRange;
    }

    public void createDefaultLimitRange(ClusterVO cluster, String namespaceName) throws Exception{
        this.createDefaultLimitRange(cluster, namespaceName, null, false);
    }

    public void createDefaultLimitRange(ClusterVO cluster, String namespaceName, LimitRangeGuiVO gui, boolean dryRun) throws Exception{

        if (gui == null) {
            gui = new LimitRangeGuiVO();
            gui.setName(KubeConstants.LIMIT_RANGE_DEFAULT_NAME);
            gui.setNamespace(namespaceName);
            gui.setDefault(true);
            gui.setDescription("The limit range is managed by a cocktail.");
            gui.putLabelsItem(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);

            LimitRangeItemVO limit = new LimitRangeItemVO();
            limit.setType(LimitRangeType.Container.getCode());
            limit.putDefaultRequestItem(KubeConstants.RESOURCES_CPU, "100m");
            limit.putDefaultRequestItem(KubeConstants.RESOURCES_MEMORY, "100Mi");
            limit.putDefaultItem(KubeConstants.RESOURCES_CPU, "200m");
            limit.putDefaultItem(KubeConstants.RESOURCES_MEMORY, "200Mi");
            gui.addLimitsItem(limit);
        }

        K8sLimitRangeVO k8sLr = this.getDefaultLimitRange(cluster, namespaceName);
        if (k8sLr == null) {
            // 생성
            this.createLimitRange(cluster, gui, dryRun, ContextHolder.exeContext());
        } else {
            // 이미 존재한다면 관리 라벨만 추가
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchManagedByCocktailLabel(k8sLr.getLabels());
            this.patchLimitRange(cluster, gui.getNamespace(), gui.getName(), patchBody, dryRun, ContextHolder.exeContext());
        }
    }

    /**
     * 기본 LimitRange 상세 조회
     *
     * @param cluster
     * @param namespacee
     * @return
     * @throws Exception
     */
    public K8sLimitRangeVO getDefaultLimitRange(ClusterVO cluster, String namespacee) throws Exception {
        String label = String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
        List<K8sLimitRangeVO> list = this.getLimitRanges(cluster, namespacee, null, label, ContextHolder.exeContext());
        K8sLimitRangeVO result = null;
        if (CollectionUtils.isNotEmpty(list)) {
            result = list.get(0);
        }

        return result;
    }

    /**
     * LimitRange 체크
     *
     * @param cluster
     * @param namespaceName
     * @param isAdd
     * @param gui
     * @throws Exception
     */
    public void checkLimitRange(ClusterVO cluster, String namespaceName, boolean isAdd, LimitRangeGuiVO gui) throws Exception {
        K8sLimitRangeVO k8sLr = null;
        QuantityFormatter quantityFormatter = new QuantityFormatter();
        if (isAdd) {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("LimitRange name is invalid", ExceptionType.K8sLimitRangeNameInvalid, ResourceUtil.getInvalidNameMsg("LimitRange name is invalid"));
            } else {
                /**
                 * 기본 LimitRange 생성일 경우 '기본' 라벨로 조회하고
                 * 조회된 값이 있을 경우 이름도 같은 지 체크함.
                 */
                if (gui.isDefault()) {
                    k8sLr = this.getDefaultLimitRange(cluster, namespaceName);
                } else {
                    k8sLr = this.getLimitRange(cluster, namespaceName, gui.getName(), ContextHolder.exeContext());
                }
                if (k8sLr != null) {
                    boolean isAlready = false;
                    if (gui.isDefault()) {
                        if (StringUtils.equals(gui.getName(), k8sLr.getName())) {
                            isAlready = true;
                        }
                    } else {
                        isAlready = true;
                    }
                    if (isAlready) {
                        throw new CocktailException("LimitRange already exists!!", ExceptionType.LimitRangeNameAlreadyExists);
                    }
                }
            }
        } else {
            if (StringUtils.isBlank(gui.getName()) || !gui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("LimitRange name is invalid", ExceptionType.K8sLimitRangeNameInvalid, ResourceUtil.getInvalidNameMsg("LimitRange name is invalid"));
            } else {
                /**
                 * 기본 LimitRange 수정일 경우 '기본' 라벨로 조회하고
                 * 조회된 값이 있을 경우 이름도 같은 지 체크함.
                 */
                if (gui.isDefault()) {
                    k8sLr = this.getDefaultLimitRange(cluster, namespaceName);
                } else {
                    k8sLr = this.getLimitRange(cluster, namespaceName, gui.getName(), ContextHolder.exeContext());
                }
                boolean notFound = false;
                if (k8sLr == null) {
                    notFound = true;
                } else {
                    if (gui.isDefault()) {
                        if (!StringUtils.equals(gui.getName(), k8sLr.getName())) {
                            notFound = true;
                        }
                    }
                }

                if (notFound) {
                    throw new CocktailException("LimitRange not found!!", ExceptionType.K8sLimitRangeNotFound);
                }
            }
        }

        // valid Quantity Resource Value
        if (CollectionUtils.isNotEmpty(gui.getLimits())) {
            String type = null;
            String errMsg = null;
            for (LimitRangeItemVO item : gui.getLimits()) {

                // set LimitRange type
                switch (LimitRangeType.valueOf(item.getType())) {
                    case Container:
                    case Pod:
                        type = item.getType();
                        break;
                    case PersistentVolumeClaim:
                        type = "Storage";
                        break;
                }

                // Pod 유형의 필수 처리
                if (LimitRangeType.valueOf(item.getType()) == LimitRangeType.Pod) {
                    errMsg = "may not be specified when `type` is 'Pod'";
                    if (MapUtils.isNotEmpty(item.get_default())) {
                        errMsg = String.format("'default' field %s", errMsg);
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                    if (MapUtils.isNotEmpty(item.getDefaultRequest())) {
                        errMsg = String.format("'default request' field %s", errMsg);
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                }

                // PersistentVolumeClaim 유형의 필수 처리
                if (LimitRangeType.valueOf(item.getType()) == LimitRangeType.PersistentVolumeClaim) {
                    if (MapUtils.getString(item.getMin(), KubeConstants.RESOURCES_STORAGE, null) == null
                            && MapUtils.getString(item.getMax(), KubeConstants.RESOURCES_STORAGE, null) == null) {
                        errMsg = "When `type` is 'Storage', either minimum or maximum storage value is required, but neither was provided.";
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                }

                // Set the resource name, the resource name means the map key.
                Set<String> keySet = Sets.newHashSet();
                keySet.addAll(MapUtils.emptyIfNull(item.getMin()).keySet());
                keySet.addAll(MapUtils.emptyIfNull(item.getMax()).keySet());
                keySet.addAll(MapUtils.emptyIfNull(item.get_default()).keySet());
                keySet.addAll(MapUtils.emptyIfNull(item.getDefaultRequest()).keySet());
                keySet.addAll(MapUtils.emptyIfNull(item.getMaxLimitRequestRatio()).keySet());

                // 각 리소스의 상관관계에 따른 validation
                for (String key : keySet) {
                    Quantity minQuantity = ResourceUtil.getQuantityResourceValue(key, MapUtils.emptyIfNull(item.getMin()).get(key), quantityFormatter);
                    Quantity maxQuantity = ResourceUtil.getQuantityResourceValue(key, MapUtils.emptyIfNull(item.getMax()).get(key), quantityFormatter);
                    Quantity defaultQuantity = ResourceUtil.getQuantityResourceValue(key, MapUtils.emptyIfNull(item.get_default()).get(key), quantityFormatter);
                    Quantity defaultRequestQuantity = ResourceUtil.getQuantityResourceValue(key, MapUtils.emptyIfNull(item.getDefaultRequest()).get(key), quantityFormatter);
                    Quantity maxRatio = ResourceUtil.getQuantityResourceValue(key, MapUtils.emptyIfNull(item.getMaxLimitRequestRatio()).get(key), quantityFormatter);

                    if (minQuantity != null && maxQuantity != null && minQuantity.getNumber().compareTo(maxQuantity.getNumber()) > 0) {
                        errMsg = String.format("When `type` is '%s', min value %s is greater than max value %s", type, MapUtils.emptyIfNull(item.getMin()).get(key), MapUtils.emptyIfNull(item.getMax()).get(key));
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                    if (defaultRequestQuantity != null && minQuantity != null && minQuantity.getNumber().compareTo(defaultRequestQuantity.getNumber()) > 0) {
                        errMsg = String.format("When `type` is '%s', min value %s is greater than default request value %s", type, MapUtils.emptyIfNull(item.getMin()).get(key), MapUtils.emptyIfNull(item.getDefaultRequest()).get(key));
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                    if (defaultRequestQuantity != null && maxQuantity != null && defaultRequestQuantity.getNumber().compareTo(maxQuantity.getNumber()) > 0) {
                        errMsg = String.format("When `type` is '%s', default request value %s is greater than max value %s", type, MapUtils.emptyIfNull(item.getDefaultRequest()).get(key), MapUtils.emptyIfNull(item.getMax()).get(key));
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                    if (defaultRequestQuantity != null && defaultQuantity != null && defaultRequestQuantity.getNumber().compareTo(defaultQuantity.getNumber()) > 0) {
                        errMsg = String.format("When `type` is '%s', default request value %s is greater than default limit value %s", type, MapUtils.emptyIfNull(item.getDefaultRequest()).get(key), MapUtils.emptyIfNull(item.get_default()).get(key));
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                    if (defaultQuantity != null && minQuantity != null && minQuantity.getNumber().compareTo(defaultQuantity.getNumber()) > 0) {
                        errMsg = String.format("When `type` is '%s', min value %s is greater than default value %s", type, MapUtils.emptyIfNull(item.getMin()).get(key), MapUtils.emptyIfNull(item.get_default()).get(key));
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                    if (defaultQuantity != null && maxQuantity != null && defaultQuantity.getNumber().compareTo(maxQuantity.getNumber()) > 0) {
                        errMsg = String.format("When `type` is '%s', default value %s is greater than max value %s", type, MapUtils.emptyIfNull(item.get_default()).get(key), MapUtils.emptyIfNull(item.getMax()).get(key));
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                    if (maxRatio != null && maxQuantity.getNumber().compareTo(new Quantity(new BigDecimal(1), Quantity.Format.DECIMAL_SI).getNumber()) > 0) {
                        errMsg = String.format("When `type` is '%s', ratio %s is less than 1", type, MapUtils.emptyIfNull(item.getMaxLimitRequestRatio()).get(key));
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
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
    public LimitRangeGuiVO convertYamlToGui(ClusterVO cluster, String yamlStr) throws Exception {

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> pspObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(pspObjMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(pspObjMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(pspObjMap);

            if (apiKindType == K8sApiKindType.LIMIT_RANGE) {
                V1LimitRange updatedLr = Yaml.loadAs(yamlStr, V1LimitRange.class);
                return this.convertLimitRangeData(updatedLr, new JSON());
            } else {
                throw new CocktailException("Yaml is invalid.(it is not LimitRange).", ExceptionType.InvalidYamlData);
            }
        }

        return null;
    }
}
