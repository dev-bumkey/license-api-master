package run.acloud.api.resource.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.service.ComponentService;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.models.V1beta1CronJob;
import run.acloud.api.k8sextended.models.V1beta1CronJobSpec;
import run.acloud.api.k8sextended.models.V1beta1JobTemplateSpec;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.api.resource.enums.K8sApiGroupType;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiType;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.ConfigMapGuiVO;
import run.acloud.api.resource.vo.ConfigMapIntegrateVO;
import run.acloud.api.resource.vo.ConfigMapYamlVO;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.UnsupportedEncodingException;
import java.util.*;

@Slf4j
@Service
public class ConfigMapService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    /**
     * ConfigMap 생성
     *
     * @param servicemapSeq
     * @param configMap
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO createConfigMap(Integer servicemapSeq, ConfigMapGuiVO configMap) throws Exception {
        return this.createConfigMap(servicemapSeq, configMap, null);
    }

    /**
     * ConfigMap 생성, additional label
     *
     * @param clusterSeq
     * @param namespaceName
     * @param configMap
     * @param additionalLabels
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO createConfigMap(Integer clusterSeq, String namespaceName, ConfigMapGuiVO configMap, Map<String, String> additionalLabels) throws Exception {

        ClusterVO cluster = this.setupCluster(clusterSeq, namespaceName);
        V1ConfigMap v1ConfigMap = k8sWorker.getConfigMapV1(cluster, namespaceName, configMap.getName());
        if(v1ConfigMap != null){
            throw new CocktailException("ConfigMap already exists!!", ExceptionType.K8sConfigMapAlreadyExists);
        }

        V1ConfigMap configMapParam = K8sSpecFactory.buildConfigMapV1(configMap);
        if (MapUtils.isNotEmpty(additionalLabels)) {
            for (Map.Entry<String, String> entryRow : additionalLabels.entrySet()) {
                configMapParam.getMetadata().putLabelsItem(entryRow.getKey(), entryRow.getValue());
            }
        }

        k8sWorker.createConfigMapV1(cluster, namespaceName, configMapParam, false);

        return configMap;
    }

    /**
     * ConfigMap 생성
     *
     * @param servicemapSeq
     * @param configMap
     * @param cluster
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO createConfigMap(Integer servicemapSeq, ConfigMapGuiVO configMap, ClusterVO cluster) throws Exception {

        if(cluster == null){
            cluster = this.setupCluster(servicemapSeq);
        }
        V1ConfigMap configMapParam = K8sSpecFactory.buildConfigMapV1(configMap);

        V1ConfigMap existConfigMap = k8sWorker.getConfigMapV1(cluster, cluster.getNamespaceName(), configMap.getName());
        if(existConfigMap != null){
            throw new CocktailException("ConfigMap already exists!!", ExceptionType.K8sConfigMapAlreadyExists);
        }
        k8sWorker.createConfigMapV1(cluster, cluster.getNamespaceName(), configMapParam, false);

        return configMap;
    }

    /**
     * ConfigMap 생성 (Invoke From Snapshot Deployment)
     *
     * @param servicemapSeq
     * @param configMaps
     * @return
     * @throws Exception
     */
    public void createConfigMaps(Integer servicemapSeq, List<ConfigMapGuiVO> configMaps) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        if(CollectionUtils.isNotEmpty(configMaps)){
            for(ConfigMapGuiVO configMapRow : configMaps){

                this.createConfigMap(servicemapSeq, configMapRow, cluster);

                Thread.sleep(100);
            }
        }
    }

    /**
     * ConfigMap 생성 (Invoke From Snapshot Deployment)
     *
     * @param servicemapSeq
     * @param configMaps
     * @return
     * @throws Exception
     */
    public void createMultipleConfigMap(Integer servicemapSeq, List<ConfigMapIntegrateVO> configMaps) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        if(CollectionUtils.isNotEmpty(configMaps)){
            for(ConfigMapIntegrateVO configMapRow : configMaps){
                if(DeployType.valueOf(configMapRow.getDeployType()) == DeployType.GUI) {
                    ConfigMapGuiVO configMapGui = null;
                    try {
                        configMapGui = (ConfigMapGuiVO) configMapRow;
                        this.createConfigMap(servicemapSeq, configMapGui, cluster);
                        Thread.sleep(100);
                    }
                    catch (CocktailException ce) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("ConfigMap Deployment Failure : createMultipleConfigMap : %s\n%s", ce.getMessage(), JsonUtils.toGson(configMapGui)));
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("ConfigMap Deployment Failure : createMultipleConfigMap : %s\n%s", ex.getMessage(), JsonUtils.toGson(configMapGui)));
                    }
                }
                else if(DeployType.valueOf(configMapRow.getDeployType()) == DeployType.YAML) {
                    ConfigMapYamlVO configMapYaml = null;
                    try {
                        configMapYaml = (ConfigMapYamlVO) configMapRow;
                        V1ConfigMap v1ConfigMap = ServerUtils.unmarshalYaml(configMapYaml.getYaml(), K8sApiKindType.CONFIG_MAP);
                        k8sWorker.createConfigMapV1(cluster, cluster.getNamespaceName(), v1ConfigMap, false);
                        Thread.sleep(100);
                    }
                    catch (CocktailException ce) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("ConfigMap Deployment Failure : createMultipleConfigMap : %s\n%s", ce.getMessage(), JsonUtils.toGson(configMapYaml)));
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("ConfigMap Deployment Failure : createMultipleConfigMap : %s\n%s", ex.getMessage(), JsonUtils.toGson(configMapYaml)));
                    }
                }
                else {
                    log.error(String.format("Invalid DeployType : createMultipleConfigMap : %s", JsonUtils.toGson(configMapRow)));
                }
            }
        }
    }

    /**
     * ConfigMap 정보 조회
     *
     * @param servicemapSeq
     * @param configMapName
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO getConfigMap(Integer servicemapSeq, String configMapName) throws Exception {

        ClusterVO cluster = this.setupCluster(servicemapSeq);

        return this.getConfigMap(cluster, cluster.getNamespaceName(), configMapName);
    }

    /**
     * ConfigMap 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @param configMapName
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO getConfigMap(Integer clusterSeq, String namespaceName, String configMapName) throws Exception {

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getConfigMap(cluster, namespaceName, configMapName);
    }

    /**
     * ConfigMap 정보 조회
     *
     * @param cluster
     * @param namespaceName
     * @param configMapName
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO getConfigMap(ClusterVO cluster, String namespaceName, String configMapName) throws Exception {

        V1ConfigMap configMap = this.getConfigMapV1(cluster, namespaceName, configMapName);

        return this.convertConfigMapData(configMap);
    }

    /**
     * ConfigMap 정보 목록 조회
     *
     * @param servicemapSeq
     * @return
     * @throws Exception
     */
    public List<ConfigMapGuiVO> getConfigMaps(Integer servicemapSeq) throws Exception {

        return this.getConfigMaps(servicemapSeq, null, null);
    }

    /**
     * ConfigMap 정보 목록 조회
     *
     * @param servicemapSeq
     * @return
     * @throws Exception
     */
    public List<ConfigMapGuiVO> getConfigMaps(Integer servicemapSeq, String field, String label) throws Exception {

        ClusterVO cluster = this.setupCluster(servicemapSeq);

        return this.getConfigMaps(cluster, cluster.getNamespaceName(), field, label);
    }

    /**
     * ConfigMap 정보 목록 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<ConfigMapGuiVO> getConfigMaps(Integer clusterSeq, String namespaceName, String field, String label) throws Exception {

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getConfigMaps(cluster, namespaceName, field, label);
    }

    /**
     * ConfigMap 정보 목록 조회
     *
     * @param clusterId
     * @param namespaceName
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<ConfigMapGuiVO> getConfigMaps(String clusterId, String namespaceName, String field, String label) throws Exception {

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByClusterId(clusterId, "Y");

        return this.getConfigMaps(cluster, namespaceName, field, label);
    }

    /**
     * ConfigMap 정보 목록 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<ConfigMapGuiVO> getConfigMaps(ClusterVO cluster, String namespaceName, String field, String label) throws Exception {

        List<V1ConfigMap> configMaps = k8sWorker.getConfigMapsV1(cluster, namespaceName, field, label);

        return this.convertConfigMapDataList(configMaps);
    }

    /**
     * ConfigMap 정보 목록 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1ConfigMap> getConfigMapsV1(ClusterVO cluster, String namespaceName, String field, String label) throws Exception {
        return k8sWorker.getConfigMapsV1(cluster, namespaceName, field, label);
    }

    /**
     * ConfigMap 정보 조회.
     * @param cluster
     * @param namespaceName
     * @param configMapName
     * @return
     * @throws Exception
     */
    public V1ConfigMap getConfigMapV1(ClusterVO cluster, String namespaceName, String configMapName) throws Exception {
        return k8sWorker.getConfigMapV1(cluster, namespaceName, configMapName);
    }

    /**
     * K8S ConfigMap 정보 조회 후 List<V1ConfigMap> -> List<ConfigMapGuiVO> 변환
     *
     * @param configMaps
     * @return
     */
    private List<ConfigMapGuiVO> convertConfigMapDataList(List<V1ConfigMap> configMaps) {
        List<ConfigMapGuiVO> configMapGuiVOS = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(configMaps)){
            for(V1ConfigMap v1ConfigMapRow : configMaps){
                configMapGuiVOS.add(this.convertConfigMapData(v1ConfigMapRow));
            }
        }

        return configMapGuiVOS;
    }

    /**
     * K8S ConfigMap 정보 조회 후 V1ConfigMap -> ConfigMapGuiVO 변환
     *
     * @param configMap
     * @return
     */
    public ConfigMapGuiVO convertConfigMapData(V1ConfigMap configMap) {
        ConfigMapGuiVO cm = new ConfigMapGuiVO();

        if(configMap != null){
            cm.setName(configMap.getMetadata().getName());
            cm.setNamespace(configMap.getMetadata().getNamespace());
            cm.setCreationTimestamp(configMap.getMetadata().getCreationTimestamp());
            // description
            cm.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(configMap.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));
            cm.setData(configMap.getData());

            if(configMap.getMetadata().getLabels() != null) {
                cm.setLabels(configMap.getMetadata().getLabels());
            }

            /** R3.5 : 추가 **/
            if(configMap.getMetadata().getAnnotations() != null) {
                cm.setAnnotations(configMap.getMetadata().getAnnotations());
            }

            JSON k8sJson = new JSON();
            cm.setDeployment(k8sJson.serialize(configMap));
            cm.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(configMap));

        }else {
            return null;
        }

        return cm;
    }

    /**
     * Patch ConfigMap
     * @param servicemapSeq
     * @param configMapParam
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO patchConfigMap(Integer servicemapSeq, ConfigMapGuiVO configMapParam) throws Exception {
        return patchConfigMap(servicemapSeq, null, configMapParam);
    }

    /**
     * Patch ConfigMap
     * @param servicemapSeq
     * @param cluster
     * @param configMapParam
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO patchConfigMap(Integer servicemapSeq, ClusterVO cluster, ConfigMapGuiVO configMapParam) throws Exception{
        ConfigMapGuiVO configMapGuiVO = null;
        try {
            if(cluster == null || StringUtils.isBlank(cluster.getNamespaceName())) {
                if(servicemapSeq != null) {
                    cluster = this.setupCluster(servicemapSeq);
                }
                else {
                    throw new CocktailException("patchConfigMap fail!! (there is no key to check)", ExceptionType.InvalidParameter_Empty);
                }
            }

            if(!this.isUsedConfigMap(servicemapSeq, cluster, configMapParam, false, true, true)){
                throw new CocktailException("ConfigMap is used.", ExceptionType.ConfigMapUsed);
            }

            /** R3.5 : 2019.10.18 : 기존 Configmap을 조회하여 Reserved Label, Annotation을 유지할 수 있도록 처리 : buildPatchConfigMapV1().makePatchMap()**/
            V1ConfigMap asisConfigmap = k8sWorker.getConfigMapV1(cluster, cluster.getNamespaceName(), configMapParam.getName()); // Get
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchConfigMapV1(configMapParam, asisConfigmap); // Set
            V1ConfigMap configMap = k8sWorker.patchConfigMapV1(cluster, cluster.getNamespaceName(), configMapParam.getName(), patchBody, false); // Go

            configMapGuiVO = this.convertConfigMapData(configMap);
        }
        catch (CocktailException ce) {
            boolean isSpecial = false;
            if(ce.getType() == ExceptionType.ConfigMapUsed){
                isSpecial = true;
            }

            if (isSpecial){
                throw ce;
            }else {
                throw new CocktailException("patchConfigMap fail!!", ce, ExceptionType.K8sCocktailCloudUpdateFail);
            }
        }
        catch (Exception e) {
            throw new CocktailException("patchConfigMap fail!!", e, ExceptionType.K8sCocktailCloudUpdateFail);
        }

        return configMapGuiVO;
    }

    /**
     * Patch ConfigMap With Yaml
     * @param servicemapSeq
     * @param configMapYaml
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO patchConfigMapWithYaml(Integer servicemapSeq, ConfigMapYamlVO configMapYaml) throws Exception{
        return patchConfigMapWithYaml(servicemapSeq, null, configMapYaml);
    }

    /**
     * Patch ConfigMap With Yaml
     * @param servicemapSeq
     * @param cluster
     * @param configMapYaml
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO patchConfigMapWithYaml(Integer servicemapSeq, ClusterVO cluster, ConfigMapYamlVO configMapYaml) throws Exception{
        try {
            if(cluster == null || StringUtils.isBlank(cluster.getNamespaceName())) {
                if(servicemapSeq != null) {
                    cluster = this.setupCluster(servicemapSeq);
                }
                else {
                    throw new CocktailException("patchConfigMapWithYaml fail!! (there is no key to check)", ExceptionType.InvalidParameter_Empty);
                }
            }

            // Valid check 하기 위행 GUI로 변환
            ConfigMapGuiVO configMapGui = this.convertYamlToConfigMap(cluster, configMapYaml);
            if(!cluster.getNamespaceName().equals(configMapGui.getNamespace())) {
                throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
            }
            if(!configMapYaml.getName().equals(configMapGui.getName())) { // Controller에서 URI path Param과 configMapYaml Param이 다른지는 이미 비교함..
                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sConfigMapNameInvalid);
            }

            // configMap Valid Check.
            if(!this.isUsedConfigMap(servicemapSeq, cluster, configMapGui, false, true, true)){
                throw new CocktailException("ConfigMap is used.", ExceptionType.ConfigMapUsed);
            }

            Map<String, Object> configMapObjMap = ServerUtils.getK8sYamlToMap(configMapYaml.getYaml());

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(configMapObjMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(configMapObjMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(configMapObjMap);

            if (apiKindType == K8sApiKindType.CONFIG_MAP) {
                if (apiVerType == K8sApiType.V1) {
                    V1ConfigMap updatedConfigMap = Yaml.loadAs(configMapYaml.getYaml(), V1ConfigMap.class);
                    return this.patchConfigMapV1WithYaml(cluster, cluster.getNamespaceName(), updatedConfigMap, ContextHolder.exeContext());
                }
            }
        }
        catch (CocktailException ce) {
            boolean isSpecial = false;
            if(ce.getType() == ExceptionType.ConfigMapUsed ||
                    ce.getType() == ExceptionType.K8sConfigMapNameInvalid){
                isSpecial = true;
            }
            if (isSpecial){
                throw ce;
            }else {
                throw new CocktailException("patchConfigMap fail!!", ce, ExceptionType.K8sCocktailCloudUpdateFail);
            }
        }
        catch (Exception e) {
            throw new CocktailException("patchConfigMap fail!!", e, ExceptionType.K8sCocktailCloudUpdateFail);
        }

        return null;
    }

    public ConfigMapGuiVO patchConfigMapV1WithYaml(ClusterVO cluster, String namespace, V1ConfigMap updatedConfigMap, ExecutingContextVO context) throws Exception {
        // 현재 ConfigMap 조회
        V1ConfigMap currentConfigMap = k8sWorker.getConfigMapV1(cluster, namespace, updatedConfigMap.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentConfigMap, updatedConfigMap);
        log.debug("########## ConfigMap patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchConfigMapV1(cluster, namespace, updatedConfigMap.getMetadata().getName(), patchBody, false);
        Thread.sleep(100);

        return this.getConfigMap(cluster.getClusterSeq(), namespace, updatedConfigMap.getMetadata().getName());
    }

    public Object patchConfigMapV1WithYaml(ClusterVO cluster, String namespace, String name, Object currentConfigMap, Object updatedConfigMap, boolean dryRun, ExecutingContextVO context) throws Exception {

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentConfigMap, updatedConfigMap);
        log.debug("########## ConfigMap patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        return k8sWorker.patchConfigMapV1(cluster, namespace, name, patchBody, dryRun);
    }

    /**
     * Patch ConfigMap
     *
     * @param clusterSeq
     * @param namespaceName
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO patchConfigMap(Integer clusterSeq, String namespaceName, String name, List<JsonObject> patchBody) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.patchConfigMap(cluster, namespaceName, name, patchBody);
    }

    public ConfigMapGuiVO patchConfigMap(ClusterVO cluster, String namespaceName, String name, List<JsonObject> patchBody) throws Exception{
        ConfigMapGuiVO configMapGuiVO = null;
        try {
            V1ConfigMap currConfigMap = k8sWorker.getConfigMapV1(cluster, namespaceName, name);
            if (currConfigMap != null) {
                V1ConfigMap configMap = k8sWorker.patchConfigMapV1(cluster, namespaceName, name, patchBody, false);

                configMapGuiVO = this.convertConfigMapData(configMap);
            }
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("patchConfigMap fail!!", e, ExceptionType.K8sCocktailCloudUpdateFail);
        }

        return configMapGuiVO;
    }

    /**
     * ConfigMap 삭제
     * @param servicemapSeq
     * @param configMapName
     * @throws Exception
     */
    public void deleteConfigMap(Integer servicemapSeq, String configMapName) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        this.deleteConfigMap(cluster, configMapName);
    }

    /**
     * ConfigMap 삭제
     *
     * @param configMapName
     * @return
     * @throws Exception
     */
    public void deleteConfigMap(ClusterVO cluster, String configMapName) throws Exception {
        V1ConfigMap configMap = k8sWorker.getConfigMapV1(cluster, cluster.getNamespaceName(), configMapName);
        ConfigMapGuiVO configMapGuiVO = this.convertConfigMapData(configMap);

        if(!this.isUsedConfigMap(cluster, cluster.getNamespaceName(), configMapGuiVO, true, true, false)){
            throw new CocktailException("ConfigMap is used.", ExceptionType.ConfigMapUsed);
        }

        k8sWorker.deleteConfigMapV1(cluster, cluster.getNamespaceName(), configMapName);

    }

    /**
     * ConfigMap 삭제
     *
     * @param cluster
     * @param namespaceName
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteConfigMap(ClusterVO cluster, String namespaceName, String name) throws Exception {

        V1ConfigMap configMap = k8sWorker.getConfigMapV1(cluster, namespaceName, name);
        if (configMap != null) {
            k8sWorker.deleteConfigMapV1(cluster, namespaceName, name);
        }

    }

    /**
     * Config-Map 사용유무 체크
     *
     * @param servicemapSeq
     * @param configMapParam
     * @param checkVolume - Volume에서 Config-Map 사용유무
     * @param checkEnv - 환경변수에서 Config-Map 사용유무
     * @param checkEnvKey - 환경변수에서 Config-Map Key 사용유무
     * @return
     * @throws Exception
     */
    public boolean isUsedConfigMap(Integer servicemapSeq, ConfigMapGuiVO configMapParam, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return this.isUsedConfigMap(servicemapSeq, cluster, configMapParam, checkVolume, checkEnv, checkEnvKey);
    }

    /**
     * Config-Map 사용유무 체크
     *
     * @param clusterSeq
     * @param namespaceName
     * @param configMapParam
     * @param checkVolume
     * @param checkEnv
     * @param checkEnvKey
     * @return
     * @throws Exception
     */
    public boolean isUsedConfigMap(Integer clusterSeq, String namespaceName, ConfigMapGuiVO configMapParam, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception {
        ClusterVO cluster = this.setupCluster(clusterSeq, namespaceName);
        return this.isUsedConfigMap(null, cluster, configMapParam, checkVolume, checkEnv, checkEnvKey);
    }

    /**
     * Config-Map 사용유무 체크 (false가 사용중.. isUsedConfigMap() == false == 사용중이다..)
     * @param servicemapSeq
     * @param configMapParam
     * @param checkVolume - Volume에서 Config-Map 사용유무
     * @param checkEnv - 환경변수에서 Config-Map 사용유무
     * @param checkEnvKey - 환경변수에서 Config-Map Key 사용유무
     * @param cluster
     * @return
     * @throws Exception
     */
    public boolean isUsedConfigMap(Integer servicemapSeq, ClusterVO cluster, ConfigMapGuiVO configMapParam, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception{
        boolean isNotUsed = true;
        try {
            if(cluster == null || StringUtils.isBlank(cluster.getNamespaceName())) {
                if(servicemapSeq != null) {
                    cluster = this.setupCluster(servicemapSeq);
                }
                else {
                    throw new CocktailException("isUsedConfigMap fail!! (there is no key to check)", ExceptionType.InvalidParameter_Empty);
                }
            }
            V1ConfigMap configMap = k8sWorker.getConfigMapV1(cluster, cluster.getNamespaceName(), configMapParam.getName());

            if(configMap != null){
                Map<String, JsonPatchOp> patchOp = new HashMap<>();
                Map<String, String> removeKeyMap = new HashMap<>();
                Map<String, JsonPatchOp> labelPatchOp;
                Map<String, JsonPatchOp> annotationPatchOp;
                Map<String, JsonPatchOp> patchDescOp;

                /**
                 * 20220510, hjchoi
                 * - value가 빈값이 일시 추가를 안하던것을 가능하도록 빈값체크 제거
                 */
                if(MapUtils.isNotEmpty(configMap.getData()) && MapUtils.isNotEmpty(configMapParam.getData())){
                    for(Map.Entry<String, String> dataEntry : configMapParam.getData().entrySet()){
                        if(configMap.getData().containsKey(dataEntry.getKey())){
                            if(!StringUtils.equals(dataEntry.getValue(), configMap.getData().get(dataEntry.getKey()))){
                                patchOp.put(dataEntry.getKey(), JsonPatchOp.REPLACE);
                            }
                        }else{
                            patchOp.put(dataEntry.getKey(), JsonPatchOp.ADD);
                        }
                    }

                    for(Map.Entry<String, String> dataEntry : configMap.getData().entrySet()){
                        if(!configMapParam.getData().containsKey(dataEntry.getKey())){
                            patchOp.put(dataEntry.getKey(), JsonPatchOp.REMOVE);
                            removeKeyMap.put(dataEntry.getKey(), "remove");
                        }
                    }
                }

                // 키 삭제시 서버에서 사용중인지 체크
                if(checkEnvKey){
                    if(MapUtils.isNotEmpty(removeKeyMap)){
                        ConfigMapGuiVO configMapRemove = new ConfigMapGuiVO();
                        BeanUtils.copyProperties(configMapRemove, configMapParam);
                        configMapRemove.setData(removeKeyMap);
//                        if(serviceSeq != null && appmapSeq != null) {
//                            isNotUsed = this.isUsedConfigMap(serviceSeq, appmapSeq, configMapRemove, checkVolume, checkEnv, checkEnvKey, null);
//                        }
                        isNotUsed = this.isUsedConfigMap(cluster, cluster.getNamespaceName(), configMapRemove, checkVolume, checkEnv, checkEnvKey);
                    }
                }else{
//                    if(serviceSeq != null && appmapSeq != null) {
//                        isNotUsed = this.isUsedConfigMap(serviceSeq, appmapSeq, configMapParam, checkVolume, checkEnv, checkEnvKey, null);
//                    }
                    isNotUsed = this.isUsedConfigMap(cluster, cluster.getNamespaceName(), configMapParam, checkVolume, checkEnv, checkEnvKey);
                }
                configMapParam.setPatchOp(patchOp);

                /** R3.5 : 2019.10.18 : Add Check Labels **/
                labelPatchOp = k8sPatchSpecFactory.makePatchOp(configMap.getMetadata().getLabels(), configMapParam.getLabels());
                configMapParam.setLabelPatchOp(labelPatchOp);

                /** R3.5 : 2019.10.18 : Add Check Annotations **/
                annotationPatchOp = k8sPatchSpecFactory.makePatchOp(configMap.getMetadata().getAnnotations(), configMapParam.getAnnotations());
                configMapParam.setAnnotationPatchOp(annotationPatchOp);

                // description // R3.5 : 2019.10.18 : 중복 소스 리팩토링.
                patchDescOp = k8sPatchSpecFactory.makePatchOpForDescription(configMap.getMetadata().getAnnotations(), configMapParam.getDescription());
                configMapParam.setPatchDescOp(patchDescOp);

            }else{
                throw new CocktailException(String.format("ConfigMap not found: %s", configMapParam.getName()), ExceptionType.K8sConfigMapNotFound);
            }
        }
        catch (CocktailException ce) {
            boolean isSpecial = false;
            if(ce.getType() == ExceptionType.K8sConfigMapNotFound){
                isSpecial = true;
            }

            if (isSpecial){
                throw ce;
            }else {
                throw new CocktailException("isUsedConfigMap fail!!", ce, ExceptionType.K8sCocktailCloudInquireFail);
            }
        }
        catch (Exception e) {
            throw new CocktailException("isUsedConfigMap fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return isNotUsed;
    }

    /**
     * Config-Map 사용유무 체크 (false가 사용중이다. 헷갈리지 말자)
     *
     * @param cluster
     * @param namespace
     * @param configMap
     * @param checkVolume - Volume에서 Config-Map 사용유무
     * @param checkEnv - 환경변수에서 Config-Map 사용유무
     * @param checkEnvKey - 환경변수에서 Config-Map Key 사용유무
     * @return
     * @throws Exception
     */
    public boolean isUsedConfigMap(ClusterVO cluster, String namespace, ConfigMapGuiVO configMap, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception {
        boolean isNotUsed = true;

        if (configMap == null) {
            return false;
        }

        List<V1Deployment> deployments = k8sWorker.getDeploymentsV1(cluster, namespace, null, null);
        for (V1Deployment deploymentRow : Optional.ofNullable(deployments).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(deploymentRow).map(V1Deployment::getSpec).map(V1DeploymentSpec::getTemplate).orElseGet(() ->null);
            if (checkVolume && !this.isUsedConfigMapOfVolume(v1PodTemplateSpec, configMap)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedConfigMapOfContainer(v1PodTemplateSpec, configMap, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }
        if(!isNotUsed) return isNotUsed;

        List<V1StatefulSet> statefulSets = k8sWorker.getStatefulSetsV1(cluster, namespace, null, null);
        for (V1StatefulSet statefulSetRow : Optional.ofNullable(statefulSets).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(statefulSetRow).map(V1StatefulSet::getSpec).map(V1StatefulSetSpec::getTemplate).orElseGet(() ->null);
            if (checkVolume && !this.isUsedConfigMapOfVolume(v1PodTemplateSpec, configMap)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedConfigMapOfContainer(v1PodTemplateSpec, configMap, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }
        if(!isNotUsed) return isNotUsed;

        List<V1DaemonSet> daemonSets = k8sWorker.getDaemonSetsV1(cluster, namespace, null, null);
        for (V1DaemonSet daemonSetRow : Optional.ofNullable(daemonSets).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(daemonSetRow).map(V1DaemonSet::getSpec).map(V1DaemonSetSpec::getTemplate).orElseGet(() ->null);
            if (checkVolume && !this.isUsedConfigMapOfVolume(v1PodTemplateSpec, configMap)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedConfigMapOfContainer(v1PodTemplateSpec, configMap, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }
        if(!isNotUsed) return isNotUsed;

        List<V1Job> jobs = k8sWorker.getJobsV1(cluster, namespace, null, null);
        for (V1Job jobRow : Optional.ofNullable(jobs).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(jobRow).map(V1Job::getSpec).map(V1JobSpec::getTemplate).orElseGet(() ->null);
            if (checkVolume && !this.isUsedConfigMapOfVolume(v1PodTemplateSpec, configMap)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedConfigMapOfContainer(v1PodTemplateSpec, configMap, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }
        if(!isNotUsed) return isNotUsed;

        List<V1beta1CronJob> cronJobs = k8sWorker.getCronJobsV1beta1(cluster, namespace, null, null);
        for (V1beta1CronJob cronJobRow : Optional.ofNullable(cronJobs).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(cronJobRow).map(V1beta1CronJob::getSpec)
                            .map(V1beta1CronJobSpec::getJobTemplate)
                            .map(V1beta1JobTemplateSpec::getSpec)
                            .map(V1JobSpec::getTemplate)
                            .orElseGet(() ->null);
            if (checkVolume && !this.isUsedConfigMapOfVolume(v1PodTemplateSpec, configMap)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedConfigMapOfContainer(v1PodTemplateSpec, configMap, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }

        return isNotUsed;
    }

/* 2019.11.26 : 전체 워크로드에서 체크가 필요 & Kubernetes 1.14 Spec 기준으로 작성을 위해 주석 처리
    public boolean isUsedConfigMap(ClusterVO cluster, String namespace, ConfigMapGuiVO configMap, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception{
        boolean isNotUsed = true;

        if(configMap != null){
            K8sApiType apiType = k8sWorker.getApiType(cluster, K8sApiKindType.DEPLOYMENT);

            if(K8sApiType.V1BETA1 == apiType) {
                // Deployment 조회
                List<AppsV1beta1Deployment> v1beta1Deployments = k8sWorker.getDeploymentsV1beta1(cluster, namespace, null, null);

                if(CollectionUtils.isNotEmpty(v1beta1Deployments)){
                    for(AppsV1beta1Deployment deploymentRow : v1beta1Deployments){
                        if (checkVolume) {
                            // volume 체크
                            if (CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getVolumes())) {
                                if (!this.isUsedConfigMapOfVolume(deploymentRow.getSpec().getTemplate().getSpec().getVolumes(), configMap)) {
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                        if (checkEnv){
                            // env 체크
                            if(CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getContainers())){
                                if(!this.isUsedConfigMapOfContainer(deploymentRow.getSpec().getTemplate().getSpec().getContainers(), configMap, checkEnvKey)){
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                    }
                }
            }else if(K8sApiType.V1BETA2 == apiType) {
                // Deployment 조회
                List<V1beta2Deployment> v1beta2Deployments = k8sWorker.getDeploymentsV1beta2(cluster, namespace, null, null);

                if(CollectionUtils.isNotEmpty(v1beta2Deployments)){
                    for(V1beta2Deployment deploymentRow : v1beta2Deployments){
                        if (checkVolume) {
                            // volume 체크
                            if (CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getVolumes())) {
                                if (!this.isUsedConfigMapOfVolume(deploymentRow.getSpec().getTemplate().getSpec().getVolumes(), configMap)) {
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                        if (checkEnv){
                            // env 체크
                            if(CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getContainers())){
                                if(!this.isUsedConfigMapOfContainer(deploymentRow.getSpec().getTemplate().getSpec().getContainers(), configMap, checkEnvKey)){
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                    }
                }
            }else if(K8sApiType.V1 == apiType) {
                // Deployment 조회
                List<V1Deployment> v1Deployments = k8sWorker.getDeploymentsV1(cluster, namespace, null, null);

                if(CollectionUtils.isNotEmpty(v1Deployments)){
                    for(V1Deployment deploymentRow : v1Deployments){
                        if (checkVolume) {
                            // volume 체크
                            if (CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getVolumes())) {
                                if (!this.isUsedConfigMapOfVolume(deploymentRow.getSpec().getTemplate().getSpec().getVolumes(), configMap)) {
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                        if (checkEnv){
                            // env 체크
                            if(CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getContainers())){
                                if(!this.isUsedConfigMapOfContainer(deploymentRow.getSpec().getTemplate().getSpec().getContainers(), configMap, checkEnvKey)){
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                    }
                }
            }
        }
        else{
            isNotUsed = false;
        }

        return isNotUsed;
    }
*/

    /**
     * 볼륨에서 configMap 사용 유무 (false가 사용중이다. 헷갈리지 말자;; )
     * @param v1PodTemplateSpec
     * @param configMap
     * @return
     * @throws Exception
     */
    private boolean isUsedConfigMapOfVolume(V1PodTemplateSpec v1PodTemplateSpec, ConfigMapGuiVO configMap) throws Exception{
        if(v1PodTemplateSpec == null || configMap == null) return true;

        List<V1Volume> v1Volumes =
                Optional.of(v1PodTemplateSpec).map(V1PodTemplateSpec::getSpec).map(V1PodSpec::getVolumes).orElseGet(() ->Lists.newArrayList());

        for (V1Volume volumeRow : v1Volumes) {
            if (volumeRow.getConfigMap() != null && StringUtils.equals(volumeRow.getConfigMap().getName(), configMap.getName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 볼륨에서 configMap 사용 유무 (false가 사용중이다. 헷갈리지 말자;; )
     * @param v1Volumes
     * @param configMap
     * @return
     * @throws Exception
     */
    private boolean isUsedConfigMapOfVolume(List<V1Volume> v1Volumes, ConfigMapGuiVO configMap) throws Exception{
        if (CollectionUtils.isNotEmpty(v1Volumes)) {
            for (V1Volume volumeRow : v1Volumes) {
                if (volumeRow.getConfigMap() != null && StringUtils.equals(volumeRow.getConfigMap().getName(), configMap.getName())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 환경변수 중 해당 configMap key 사용 유무 (false가 사용중이다 헷갈리지 말자;; )
     * @param v1PodTemplateSpec
     * @param configMap
     * @param checkEnvKey
     * @return
     * @throws Exception
     */
    private boolean isUsedConfigMapOfContainer(V1PodTemplateSpec v1PodTemplateSpec, ConfigMapGuiVO configMap, boolean checkEnvKey) throws Exception{
        if(v1PodTemplateSpec == null || configMap == null) return true;

        List<V1Container> v1Containers = Optional.of(v1PodTemplateSpec)
                .map(V1PodTemplateSpec::getSpec).map(V1PodSpec::getContainers).orElseGet(() ->Lists.newArrayList());

        List<V1Container> v1InitContainers = Optional.of(v1PodTemplateSpec)
                .map(V1PodTemplateSpec::getSpec).map(V1PodSpec::getInitContainers).orElseGet(() ->null);

        // container + initcontainer Merge...
        Optional.ofNullable(v1InitContainers).ifPresent(v1Containers::addAll);

        for (V1Container containerRow : Optional.of(v1Containers).orElseGet(() ->Lists.newArrayList())) {
            for (V1EnvVar envVarRow : Optional.ofNullable(containerRow.getEnv()).orElseGet(() ->Lists.newArrayList())) {
                V1ConfigMapKeySelector keySelector = Optional.ofNullable(envVarRow)
                        .map(V1EnvVar::getValueFrom)
                        .map(V1EnvVarSource::getConfigMapKeyRef)
                        .orElseGet(() ->null);
                if (Optional.ofNullable(keySelector).map(V1ConfigMapKeySelector::getName).orElseGet(() ->"").equals(configMap.getName())) {
                    if (checkEnvKey && Optional.of(configMap).map(ConfigMapGuiVO::getData).orElseGet(() ->Maps.newHashMap()).containsKey(keySelector.getKey())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 환경변수 중 해당 configMap key 사용 유무 (false가 사용중이다 헷갈리지 말자;; )
     * @param v1Containers
     * @param configMap
     * @return
     * @throws Exception
     */
    private boolean isUsedConfigMapOfContainer(List<V1Container> v1Containers, ConfigMapGuiVO configMap, boolean checkEnvKey) throws Exception{
        if (CollectionUtils.isNotEmpty(v1Containers)) {
            for (V1Container containerRow : v1Containers) {
                if (CollectionUtils.isNotEmpty(containerRow.getEnv())) {
                    for (V1EnvVar envVarRow : containerRow.getEnv()) {
                        V1ConfigMapKeySelector keySelector = Optional.ofNullable(envVarRow)
                                .map(V1EnvVar::getValueFrom)
                                .map(V1EnvVarSource::getConfigMapKeyRef)
                                .orElseGet(() ->null);
                        if (keySelector != null && StringUtils.isNotBlank(keySelector.getName()) && keySelector.getName().equals(configMap.getName())) {
                            Map<String, String> configmapData = Optional.ofNullable(configMap).map(ConfigMapGuiVO::getData).orElseGet(() ->Maps.newHashMap());
                            if (checkEnvKey && configmapData.containsKey(keySelector.getKey())) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public ConfigMapGuiVO convertYamlToConfigMap(ClusterVO cluster, ConfigMapYamlVO configMapYaml) throws Exception{
        return this.convertYamlToConfigMap(configMapYaml);
    }

    public ConfigMapGuiVO convertYamlToConfigMap(ConfigMapYamlVO configMapYaml) throws Exception{
//        V1ConfigMap v1ConfigMap = this.convertYamlToV1ConfigMap(cluster, configMapYaml.getYaml());
        V1ConfigMap v1ConfigMap = ServerUtils.unmarshalYaml(configMapYaml.getYaml());
        if(v1ConfigMap == null) {
            throw new CocktailException("Can not found Configmap spec (Invalid YAML)", ExceptionType.K8sConfigMapNotFound);
        }
        ConfigMapGuiVO configMap = this.convertConfigMap(configMapYaml, v1ConfigMap);
        return configMap;
    }

    /**
     * V1ConfigMap 모델을 ConfigMapGuiVO 모델로 Convert
     * @param configMapYaml
     * @param v1ConfigMap
     * @throws Exception
     */
    public ConfigMapGuiVO convertConfigMap(ConfigMapYamlVO configMapYaml, V1ConfigMap v1ConfigMap) throws Exception {
        ConfigMapGuiVO configMap = new ConfigMapGuiVO();
        if(v1ConfigMap != null){
            if(!StringUtils.equals(configMapYaml.getName(), v1ConfigMap.getMetadata().getName())) {
                // Yaml에 입력된 ConfigMap Name과 path에 입력한 Name 정보가 틀리면 오류..
                throw new CocktailException("ConfigMap name is different.2", ExceptionType.K8sConfigMapNameInvalid);
            }
            configMap.setName(configMapYaml.getName());
            configMap.setDeployType(DeployType.GUI.getCode());
            configMap.setNamespace(v1ConfigMap.getMetadata().getNamespace());
            configMap.setCreationTimestamp(v1ConfigMap.getMetadata().getCreationTimestamp());
            configMap.setLabels(Optional.of(v1ConfigMap).map(V1ConfigMap::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->Maps.newHashMap()));
            configMap.setAnnotations(Optional.of(v1ConfigMap).map(V1ConfigMap::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap()));
            configMap.setData(v1ConfigMap.getData());
            JSON k8sJson = new JSON();
            configMap.setDeployment(k8sJson.serialize(v1ConfigMap));
            configMap.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1ConfigMap));
            // description
            try {
                Map<String, String> annotations = Optional.ofNullable(v1ConfigMap.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                if(StringUtils.isNotBlank(annotations.get(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION))) {
                    boolean isBase64 = Utils.isBase64Encoded(MapUtils.getString(annotations, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, ""));
                    if(isBase64) {
                        configMap.setDescription(new String(Base64Utils.decodeFromString(MapUtils.getString(annotations, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "")), "UTF-8"));
                    }
                    else {
                        configMap.setDescription(MapUtils.getString(annotations, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, ""));
                    }
                }
                else {
                    configMap.setDescription("");
                }
            } catch (UnsupportedEncodingException e) {
                CocktailException ce = new CocktailException("getConfigMaps desc fail!!", e, ExceptionType.K8sConfigMapDescInvalid);
                log.error(ce.getMessage(), ce);
            }
        }
        else {
            return null;
        }

        return configMap;
    }

    /**
     * ConfigMap Validation Check
     * @param configMap
     * @throws Exception
     */
    public void validConfigMapData(ConfigMapGuiVO configMap) throws Exception{
        if (MapUtils.isEmpty(configMap.getData())){
            throw new CocktailException("ConfigMap data is invalid", ExceptionType.K8sConfigMapDataInvalid);
        }else{
            for(Map.Entry<String, String> dataEntry : configMap.getData().entrySet()){
                // value는 체크하지 않고 key만 체크하도록 수정 hjchoi.20200107
                if(StringUtils.isBlank(dataEntry.getKey())){
                    throw new CocktailException("ConfigMap data(key) is invalid", ExceptionType.K8sConfigMapDataInvalid);
                }else{

                    if (!dataEntry.getKey().matches(KubeConstants.RULE_CONFIGMAP_NAME)) {
                        throw new CocktailException(String.format("ConfigMap data key '%s' is invalid", dataEntry.getKey()),
                            ExceptionType.K8sConfigMapKeyInvalid);
                    }
                }
            }
        }
    }

    /**
     * Name, Description 유효성 체크.
     * @param name
     * @param postName
     * @param description
     */
    public void checkNameAndDescription(String name, String postName, String description) {
        if (StringUtils.isBlank(postName)) {
            throw new CocktailException("ConfigMap name is empty", ExceptionType.K8sConfigMapNameInvalid);
        }
        if (!StringUtils.equals(name, postName)) {
            throw new CocktailException("ConfigMap name is different", ExceptionType.K8sConfigMapNameInvalid);
        }
//        if (StringUtils.length(description) > 50) {
//            throw new CocktailException("ConfigMap description more than 50 characters.", ExceptionType.K8sConfigMapDescInvalid_MaxLengthLimit);
//        }
    }

    public ClusterVO setupCluster(Integer servicemapSeq) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);
        return cluster;
    }

    public ClusterVO setupCluster(Integer clusterSeq, String namespaceName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        return cluster;
    }
}
