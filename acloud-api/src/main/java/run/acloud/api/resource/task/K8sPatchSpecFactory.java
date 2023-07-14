package run.acloud.api.resource.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1StorageClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.api.resource.enums.ReservedLabelAndAnnotationKeys;
import run.acloud.api.resource.util.PatchObjectMapper;
import run.acloud.api.resource.vo.ConfigMapGuiVO;
import run.acloud.api.resource.vo.K8sNodeDetailVO;
import run.acloud.api.resource.vo.SecretGuiVO;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
public class K8sPatchSpecFactory {

    @Autowired
    private PatchObjectMapper patchObjectMapper;

    private static final Pattern ENCODED_TILDA_PATTERN = Pattern.compile("~");
    private static final Pattern ENCODED_SLASH_PATTERN = Pattern.compile("/");

    public static String encodePath(String path) {
        // see http://tools.ietf.org/html/rfc6901#section-4
        path = ENCODED_TILDA_PATTERN.matcher(path).replaceAll("~0");
        return ENCODED_SLASH_PATTERN.matcher(path).replaceAll("~1");
    }

    public List<JsonObject> buildPatch(Object current, Object updated) throws Exception {
        JsonNode diff = JsonDiff.asJson(patchObjectMapper.valueToTree(current), patchObjectMapper.valueToTree(updated));
        String diffStr = patchObjectMapper.writeValueAsString(diff);
        List<Map<String, Object>> diffMaps = patchObjectMapper.readValue(diffStr, new TypeReference<List<Map<String, Object>>>(){});

        List<JsonObject> patchBody = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(diffMaps)){
            for (Map<String, Object> diffMapRow : diffMaps){
                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(diffMapRow), JsonElement.class)).getAsJsonObject());
            }
        }

        log.debug("patchBody : {}", JsonUtils.toGson(patchBody));

        return patchBody;
    }

    public List<JsonObject> buildPatchSecretV1(SecretGuiVO patchParam, V1Secret asisSecret) throws Exception {

        List<JsonObject> patchBody = new ArrayList<>();

	    if(patchParam != null){
	        // Data
	        if (MapUtils.isNotEmpty(patchParam.getData()) && MapUtils.isNotEmpty(patchParam.getPatchOp())){
                for(Map.Entry<String, JsonPatchOp> patchOpEntry : patchParam.getPatchOp().entrySet()){
                    Map<String, Object> patchMap = new HashMap<>();
                    patchMap.put("op", patchOpEntry.getValue().getValue());
                    patchMap.put("path", String.format("/data/%s", encodePath(patchOpEntry.getKey())));

                    if(JsonPatchOp.ADD == patchOpEntry.getValue() || JsonPatchOp.REPLACE == patchOpEntry.getValue()){
                        patchMap.put("value", Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(patchParam.getData().get(patchOpEntry.getKey()))));
                    }

                    patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
                }
            }
            // Labels
            if (MapUtils.isNotEmpty(patchParam.getLabelPatchOp())){
                Map<String, Object> patchMap = this.makePatchMap(patchParam.getLabels(), asisSecret.getMetadata().getLabels(), "/metadata/labels", true);
                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
            }

            // Annotation1 : Description 입력
            patchParam.setAnnotations(Optional.ofNullable(patchParam.getAnnotations()).orElseGet(() ->new HashMap<>()));
            if (MapUtils.isNotEmpty(patchParam.getPatchDescOp())){
                String descriptionValue = Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(patchParam.getDescription())));
                patchParam.getAnnotations().put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, descriptionValue);
            }
            else {
                patchParam.getAnnotations().put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Optional.ofNullable(asisSecret.getMetadata().getAnnotations().get(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION)).orElseGet(() ->""));
            }
            // Annotation2
            if (MapUtils.isNotEmpty(patchParam.getPatchDescOp()) || MapUtils.isNotEmpty(patchParam.getAnnotationPatchOp())){
                Map<String, Object> patchMap = this.makePatchMap(patchParam.getAnnotations(), asisSecret.getMetadata().getAnnotations(), "/metadata/annotations", true);
                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
            }
        }

        return patchBody;
    }

    public List<JsonObject> buildPatchConfigMapV1(ConfigMapGuiVO patchParam, V1ConfigMap asisConfigMap) throws Exception {

        List<JsonObject> patchBody = new ArrayList<>();

	    if(patchParam != null){
            // Data
	        if (MapUtils.isNotEmpty(patchParam.getData()) && MapUtils.isNotEmpty(patchParam.getPatchOp())){
                for(Map.Entry<String, JsonPatchOp> patchOpEntry : patchParam.getPatchOp().entrySet()){
                    Map<String, Object> patchMap = new HashMap<>();
                    patchMap.put("op", patchOpEntry.getValue().getValue());
                    patchMap.put("path", String.format("/data/%s", encodePath(patchOpEntry.getKey())));

                    if(JsonPatchOp.ADD == patchOpEntry.getValue() || JsonPatchOp.REPLACE == patchOpEntry.getValue()){
                        patchMap.put("value", patchParam.getData().get(patchOpEntry.getKey()));
                    }

                    patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
                }
            }
            // Labels
            if (MapUtils.isNotEmpty(patchParam.getLabelPatchOp())){
                Map<String, Object> patchMap = this.makePatchMap(patchParam.getLabels(), asisConfigMap.getMetadata().getLabels(), "/metadata/labels", true);
                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
            }

            // Annotation1 : Description 입력
            patchParam.setAnnotations(Optional.ofNullable(patchParam.getAnnotations()).orElseGet(() ->new HashMap<>()));
            if (MapUtils.isNotEmpty(patchParam.getPatchDescOp())){
                String descriptionValue = Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(patchParam.getDescription())));
                patchParam.getAnnotations().put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, descriptionValue);
            }
            else {
                patchParam.getAnnotations().put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Optional.ofNullable(asisConfigMap.getMetadata().getAnnotations().get(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION)).orElseGet(() ->""));
            }
            // Annotation2
            if (MapUtils.isNotEmpty(patchParam.getPatchDescOp()) || MapUtils.isNotEmpty(patchParam.getAnnotationPatchOp())){
                Map<String, Object> patchMap = this.makePatchMap(patchParam.getAnnotations(), asisConfigMap.getMetadata().getAnnotations(), "/metadata/annotations", true);
                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
            }
        }

        return patchBody;
    }

    public List<JsonObject> buildPatchNodeV1(K8sNodeDetailVO target, V1Node source) throws Exception {
        List<JsonObject> patchBody = new ArrayList<>();
        if(target != null){
            /** Label과 Annotation의 Key에 "/"가 있을 경우 path에서 오류 발생.
             * => http://jsonpatch.com/에 해결 방법 존재 : "/" -> "~1"로 표기
             * => testsite : https://json-patch-builder-online.github.io/
             *
             * 입력된 Label과 Annotation에 대해 전체 Update로 처리 로직 변경.
             */
//            // Labels
//            if (MapUtils.isNotEmpty(patchParam.getLabslPatchOp())){
//                for(Map.Entry<String, JsonPatchOp> patchOpEntry : patchParam.getLabslPatchOp().entrySet()){
//                    Map<String, Object> patchMap = new HashMap<>();
//                    patchMap.put("op", patchOpEntry.getValue().getValue());
//                    patchMap.put("path", String.format("/metadata/labels/%s", patchOpEntry.getKey()));
//
//                    if(JsonPatchOp.ADD == patchOpEntry.getValue() || JsonPatchOp.REPLACE == patchOpEntry.getValue()){
//                        patchMap.put("value", patchParam.getLabels().get(patchOpEntry.getKey()));
//                    }
//
//                    patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
//                }
//            }
//
//            // Annotation
//            if (MapUtils.isNotEmpty(patchParam.getAnnotationPatchOp())){
//                for(Map.Entry<String, JsonPatchOp> patchOpEntry : patchParam.getAnnotationPatchOp().entrySet()){
//                    Map<String, Object> patchMap = new HashMap<>();
//                    patchMap.put("op", patchOpEntry.getValue().getValue());
//                    patchMap.put("path", String.format("/metadata/annotations/%s", patchOpEntry.getKey()));
//
//                    if(JsonPatchOp.ADD == patchOpEntry.getValue() || JsonPatchOp.REPLACE == patchOpEntry.getValue()){
//                        patchMap.put("value", patchParam.getAnnotations().get(patchOpEntry.getKey()));
//                    }
//
//                    patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
//                }
//            }
            // Labels
            if (MapUtils.isNotEmpty(target.getLabelPatchOp())){
                Map<String, Object> patchMap = this.makePatchMap(target.getLabels(), source.getMetadata().getLabels(), "/metadata/labels", true);
                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
            }

            // Annotation
            if (MapUtils.isNotEmpty(target.getAnnotationPatchOp())){
                Map<String, Object> patchMap = this.makePatchMap(target.getAnnotations(), source.getMetadata().getAnnotations(), "/metadata/annotations", true);
                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
            }

            // Taint
            if (MapUtils.isNotEmpty(target.getTaintsPatchOp())){
                // Taint는 수정 내역이 있을 경우 전체 update Taint 내용을 REPLACE 함..
                Map<String, Object> patchMap = new HashMap<>();
                if(CollectionUtils.isEmpty(target.getTaints())) { // Taint가 없으면 전체 삭제..
                    patchMap.put("op", JsonPatchOp.REMOVE.getValue());
                    patchMap.put("path", "/spec/taints");
                }
                else {
                    if (CollectionUtils.isEmpty(source.getSpec().getTaints())) {
                        patchMap.put("op", JsonPatchOp.ADD.getValue());
                    }
                    else {
                        patchMap.put("op", JsonPatchOp.REPLACE.getValue());
                    }
                    patchMap.put("path", "/spec/taints");
                    patchMap.put("value", target.getTaints());
                }
                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
            }
        }

        return patchBody;
    }

    @Deprecated
    private void buildPatchAnnotaionDescription(List<JsonObject> patchBody, Map<String, JsonPatchOp> patchDescOp, String description){
        if (MapUtils.isNotEmpty(patchDescOp)){
            Map<String, Object> patchMap = Maps.newHashMap();
            patchMap.put("op", patchDescOp.get(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION).getValue());

            if(patchDescOp.get(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION) == JsonPatchOp.ADD_ANNO){
                patchMap.put("path", "/metadata/annotations");
                Map<String, Object> patchDescMap = Maps.newHashMap();
                patchDescMap.put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(description))));
                patchMap.put("value", patchDescMap);
            }else{
                patchMap.put("path", String.format("/metadata/annotations/%s", KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION));
                patchMap.put("value", Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(description))));
            }

            patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
        }
    }

    public List<JsonObject> buildPatchDeploymentScaleV1(int computeTotal) {

        Map<String, Object> patchMap = new HashMap<>();
        patchMap.put("op", JsonPatchOp.REPLACE.getValue());
        patchMap.put("path", "/spec/replicas");
        patchMap.put("value", computeTotal == 0 ? 1 : computeTotal);

        return buildPatchObjectByMap(patchMap);
    }

    public List<JsonObject> buildPatchStatefulSetScaleV1(int computeTotal) {

        Map<String, Object> patchMap = new HashMap<>();
        patchMap.put("op", JsonPatchOp.REPLACE.getValue());
        patchMap.put("path", "/spec/replicas");
        patchMap.put("value", computeTotal);

        return buildPatchObjectByMap(patchMap);
    }

    public V1StorageClass copyStorageClass(V1StorageClass orig) throws Exception {
        String str = patchObjectMapper.writeValueAsString(orig);
        return patchObjectMapper.readValue(str, new TypeReference<V1StorageClass>(){});
    }

    public <T> T copyObject(Object orig, TypeReference<T> type) throws Exception {
        String str = patchObjectMapper.writeValueAsString(orig);
        return patchObjectMapper.readValue(str, type);
    }

    /**
     * 사전 정의한 Operation 정보를 기반으로 PatchMap을 생성
     * @param target
     * @param source
     * @return
     */
    private Map<String, Object> makePatchMap(Map<String, String> target, Map<String, String> source, String targetPath, boolean useReservedKeyRollback) throws Exception {
        Map<String, Object> patchMap = new HashMap<>();
        /** patchData를 만들기 전 Reserved Labels 가 삭제되었을 경우 다시 target에 넣어주어 제거 및 수정 할 수 없도록 함.. **/
        if(useReservedKeyRollback) {
            target = this.saveReservedLabelsAndAnnotations(source, target);
        }

        if(MapUtils.isEmpty(target)) { // Label이 없으면 전체 삭제..
            patchMap.put("op", JsonPatchOp.REMOVE.getValue());
            patchMap.put("path", targetPath);
        }
        else {
            if (MapUtils.isEmpty(source)) {
                patchMap.put("op", JsonPatchOp.ADD.getValue());
            }
            else {
                patchMap.put("op", JsonPatchOp.REPLACE.getValue());
            }

            patchMap.put("path", targetPath);
            patchMap.put("value", target);
        }

        return patchMap;
    }

    /**
     * Source Labels or Annotation중 예약된 Key에 해당하는 값이 있다면 변경할 수 없도록 target에 다시 넣어줌...
     * Source의 값을 target으로 복사.
     * @param source
     * @param target
     * @throws Exception
     */
    public Map<String, String> saveReservedLabelsAndAnnotations(Map<String, String> source, Map<String, String> target) throws Exception {
        target = Optional.ofNullable(target).orElseGet(() ->new HashMap<>());
        if(false) {
            if (MapUtils.isNotEmpty(source)) {
                for (String key : source.keySet()) {
                    // Reserved Label, Annotation도 상세하게 단어 전체를 관리하지 않으면 예외 케이스가 있음..;; 따로 목록 관리해야 할지는 구현하면서 판단하여 수정.
                    // (아래 예외의 경우는 cocktail이 포함되면 Reserved Key라 유지하지만 cocktail-user-description은 유지할 필요가 없는 케이스)
                    if (!StringUtils.equalsIgnoreCase(key, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION)) {
                        if (ReservedLabelAndAnnotationKeys.getKeyWhoesValueContainsKeys(key) != null) {
                            log.debug("========================== found Key : " + key);
                            target.put(key, source.get(key));
                        }
                    }
                }
            }
        }

        return target;
    }

    /**
     * app.kubernetes.io/managed-by: cocktail 라벨 patch 구문 생성
     *
     * @param source
     * @return
     */
    public List<JsonObject> buildPatchManagedByCocktailLabel(Map<String, String> source) {

        Map<String, Object> patchMap = Maps.newHashMap();
        JsonPatchOp patchOp = JsonPatchOp.ADD;
        if (MapUtils.isNotEmpty(source)){
            if (MapUtils.getString(source, KubeConstants.META_LABELS_APP_MANAGED_BY, null) != null) {
                patchOp = JsonPatchOp.REPLACE;
            }
        }

        patchMap.put("op", patchOp.getValue());
        if (patchOp == JsonPatchOp.ADD) {
            patchMap.put("path", "/metadata/labels");
            Map<String, Object> patchAddMap = Maps.newHashMap();
            patchAddMap.put(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
            patchMap.put("value", patchAddMap);
        } else {
            patchMap.put("path", String.format("/metadata/labels/%s", K8sPatchSpecFactory.encodePath(KubeConstants.META_LABELS_APP_MANAGED_BY)));
            patchMap.put("value", KubeConstants.LABELS_COCKTAIL_KEY);
        }

        return buildPatchObjectByMap(patchMap);
    }

    /**
     * 단건 patch object 생성
     *
     * @param patchMap
     * @return
     */
    public List<JsonObject> buildPatchObjectByMap(Map<String, Object> patchMap) {
        List<JsonObject> patchBody = Lists.newArrayList();
        if (MapUtils.isNotEmpty(patchMap)) {
            JsonObject body = (JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject();
            patchBody.add(body);
        }

        return patchBody;
    }

    /**
     * Source와 Target 맵을 비교하여 JsonPatchOp 맵을 생성.
     *
     * @param source
     * @param target
     * @return
     */
    public Map<String, JsonPatchOp> makePatchOp(Map<String, String> source, Map<String, String> target) {
        Map<String, JsonPatchOp> opMap = new HashMap<>();

        /**
         * 20220510, hjchoi
         * - value가 빈값이 일시 추가를 안하던것을 가능하도록 빈값체크 제거
         */
        if(MapUtils.isNotEmpty(target)) { // target에 데이터가 없으면 => 모두 Remove이므로 target에 데이터가 있을 때만 처리
            for (Map.Entry<String, String> dataEntry : target.entrySet()) {
                if (MapUtils.isNotEmpty(source) &&
                        source.containsKey(dataEntry.getKey())) {
                    if (!StringUtils.equals(dataEntry.getValue(), source.get(dataEntry.getKey()))) {
                        opMap.put(dataEntry.getKey(), JsonPatchOp.REPLACE);
                    }
                }
                else {
                    opMap.put(dataEntry.getKey(), JsonPatchOp.ADD);
                }
            }
        }
        if(MapUtils.isNotEmpty(source)) { // source에 데이터가 있을 경우에만 Remove가 존재 가능
            for(Map.Entry<String, String> dataEntry : source.entrySet()){
                if (MapUtils.isEmpty(target) ||
                        !target.containsKey(dataEntry.getKey())){
                    opMap.put(dataEntry.getKey(), JsonPatchOp.REMOVE);
                }
            }
        }

        return opMap;
    }

    /**
     * Source와 Target 맵을 비교하여 JsonPatchOp 맵을 생성.
     * @param source
     * @param targetDescription
     * @return
     */
    public Map<String, JsonPatchOp> makePatchOpForDescription(Map<String, String> source, String targetDescription) {
        Map<String, JsonPatchOp> opMap = new HashMap<>();

        if(MapUtils.isNotEmpty(source)){
            if (MapUtils.getString(source, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION) != null){
                String newDesc = StringUtils.defaultString(targetDescription);
                String oldDesc = "";
                boolean isBase64 = Utils.isBase64Encoded(MapUtils.getString(source, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, ""));
                if(isBase64) {
                    oldDesc = new String(Base64Utils.decodeFromString(MapUtils.getString(source, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "")));
                }
                else {
                    oldDesc = MapUtils.getString(source, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "");
                }

                if (!StringUtils.equals(newDesc, oldDesc)){
                    opMap.put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, JsonPatchOp.REPLACE);
                }
            }else{
                opMap.put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, JsonPatchOp.ADD);
            }
        }else{
            opMap.put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, JsonPatchOp.ADD_ANNO);
        }

        return opMap;
    }

    /**
     * rollout시 RESTARTED_AT 태그 patchJson으로 annotation에 추가하여 생성
     *
     * @param annotations
     * @param annoPath
     * @return
     * @throws Exception
     */
    public List<JsonObject> buildPatchRestartedAtAnno(Map<String, String> annotations, String annoPath) throws Exception {
        List<JsonObject> patchBody = new ArrayList<>();
        String dateStr = Utils.getNowDateTime(Utils.DEFAULT_DATE_TIME_ZONE_FORMAT);

        if (MapUtils.isNotEmpty(annotations)) {
            Map<String, Object> patchMap = new HashMap<>();
            patchMap.put("op", JsonPatchOp.ADD.getValue());
            if(annotations.containsKey(KubeConstants.META_ANNOTATIONS_RESTARTED_AT)) {
                patchMap.put("op", JsonPatchOp.REPLACE.getValue());
            }
            patchMap.put("path", String.format("%s/%s", annoPath, K8sPatchSpecFactory.encodePath(KubeConstants.META_ANNOTATIONS_RESTARTED_AT)));
            patchMap.put("value", dateStr);
            patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
        } else {
            Map<String, Object> patchMap = new HashMap<>();
            patchMap.put("op", JsonPatchOp.ADD.getValue());
            patchMap.put("path", annoPath);
            Map<String, String> restartedAtMap = Maps.newHashMap();
            restartedAtMap.put(KubeConstants.META_ANNOTATIONS_RESTARTED_AT, dateStr);
            patchMap.put("value", restartedAtMap);
            patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
        }

        return patchBody;
    }

    /**
     * rollout시 cocktail-deploy-datetime 태그 patchJson으로 annotation에 추가하여 생성
     *
     * @param annotations
     * @param annoPath
     * @return
     * @throws Exception
     */
    public List<JsonObject> buildPatchDeployDatetimeAnno(Map<String, String> annotations, String annoPath) throws Exception {
        List<JsonObject> patchBody = new ArrayList<>();
        String dateStr = Utils.getNowDateTime();

        if (MapUtils.isNotEmpty(annotations)) {
            Map<String, Object> patchMap = new HashMap<>();
            patchMap.put("op", JsonPatchOp.ADD.getValue());
            if(annotations.containsKey(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME)) {
                patchMap.put("op", JsonPatchOp.REPLACE.getValue());
            }
            patchMap.put("path", String.format("%s/%s", annoPath, K8sPatchSpecFactory.encodePath(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME)));
            patchMap.put("value", dateStr);
            patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
        } else {
            Map<String, Object> patchMap = new HashMap<>();
            patchMap.put("op", JsonPatchOp.ADD.getValue());
            patchMap.put("path", annoPath);
            Map<String, String> restartedAtMap = Maps.newHashMap();
            restartedAtMap.put(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
            patchMap.put("value", restartedAtMap);
            patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
        }

        return patchBody;
    }

    /**
     * Merge the YAML string
     * @param current Current YAML String
     * @param overwrite YAML string to overwrite
     * @return Merged YAML Map Object
     * @throws Exception
     */
    public Map<String, Object> mergeYamlToMap (String current, String overwrite) throws Exception {
        YamlMapFactoryBean factory = new YamlMapFactoryBean();
        factory.setResolutionMethod(YamlProcessor.ResolutionMethod.OVERRIDE_AND_IGNORE);
        factory.setResources(new ByteArrayResource(current.getBytes(StandardCharsets.UTF_8)),
            new ByteArrayResource(overwrite.getBytes(StandardCharsets.UTF_8)));
        Map<String, Object> yamlValueMap = factory.getObject();

        return yamlValueMap;
    }

    /**
     * Merge the YAML string
     * @param current Current YAML String
     * @param overwrite YAML string to overwrite
     * @return Merged YAML String
     * @throws Exception
     */
    public String mergeYamlToString (String current, String overwrite) throws Exception {
        Map<String, Object> yamlValueMap = this.mergeYamlToMap(current, overwrite);

        return Yaml.getSnakeYaml(null).dumpAsMap(yamlValueMap);
    }
}
