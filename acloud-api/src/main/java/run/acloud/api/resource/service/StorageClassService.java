package run.acloud.api.resource.service;

import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1StorageClass;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.vo.K8sDeployYamlVO;
import run.acloud.api.resource.vo.K8sStorageClassDetailVO;
import run.acloud.api.resource.vo.K8sStorageClassVO;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StorageClassService {
    private static final Integer MAX_TAIL_COUNT = 10000;
//    private static final String COCKTAIL_REGISTRY_SECRET = "cocktail-registry-secret";

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    /**
     * K8S StorageClass 생성
     *
     * @param clusterSeq
     * @param clusterVolume
     * @return
     * @throws Exception
     */
    public ClusterVolumeVO createStorageClass(Integer clusterSeq, ClusterVolumeVO clusterVolume) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.createStorageClass(cluster, clusterVolume);
    }

    public ClusterVolumeVO createStorageClass(ClusterVO cluster, ClusterVolumeVO clusterVolume) throws Exception {

        V1StorageClass v1StorageClass = k8sWorker.getStorageClassV1(cluster, clusterVolume.getName());
        if(v1StorageClass != null){
            throw new CocktailException("StorageClass already exists!!", ExceptionType.K8sStorageClassAlreadyExists);
        } else {
            // 기본 storageClass로 지정되었다면 기존 storageClass들의 기본 설정을 false로 변경하여 줌.
            if (BooleanUtils.toBoolean(clusterVolume.getBaseStorageYn())) {
                List<V1StorageClass> v1StorageClasses = k8sWorker.getStorageClassesV1(cluster, null, null);
                if (CollectionUtils.isNotEmpty(v1StorageClasses)) {
                    for (V1StorageClass currentV1StorageClassRow : v1StorageClasses) {
                        V1StorageClass updateV1StorageClass = k8sPatchSpecFactory.copyStorageClass(currentV1StorageClassRow);

                        if (MapUtils.getString(updateV1StorageClass.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT) != null) {
                            updateV1StorageClass.getMetadata().getAnnotations().put(KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT, "false");
                        } else {
                            updateV1StorageClass.getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RELEASE_STORAGE_CLASS_IS_DEFAULT, "false");
                        }
                        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentV1StorageClassRow, updateV1StorageClass);

                        // patch
                        k8sWorker.patchStorageClassV1(cluster, currentV1StorageClassRow.getMetadata().getName(), patchBody);
                    }
                }
            }
        }

        V1StorageClass param = K8sSpecFactory.buildStorageClassV1(clusterVolume);
        k8sWorker.createStorageClassV1(cluster, param);

        return clusterVolume;
    }

    /**
     * K8S StorageClass default 변경
     *
     * @param clusterSeq
     * @param clusterVolume
     * @return
     * @throws Exception
     */
    public ClusterVolumeVO patchDefaultStorageClass(Integer clusterSeq, ClusterVolumeVO clusterVolume) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.patchDefaultStorageClass(cluster, clusterVolume);
    }

    public ClusterVolumeVO patchDefaultStorageClass(ClusterVO cluster, ClusterVolumeVO clusterVolume) throws Exception {

        // 기본 storageClass로 지정되었다면 기존 storageClass들의 기본 설정을 false로 변경하여 줌.
        if (BooleanUtils.toBoolean(clusterVolume.getBaseStorageYn())) {
            List<V1StorageClass> v1StorageClasses = k8sWorker.getStorageClassesV1(cluster, null, null);

            if (CollectionUtils.isNotEmpty(v1StorageClasses)) {
                String isDefault = null;
                for (V1StorageClass currentV1StorageClassRow : v1StorageClasses) {
                    isDefault = StringUtils.equals(currentV1StorageClassRow.getMetadata().getName(), clusterVolume.getName()) ? "true" : "false";
                    V1StorageClass updateV1StorageClass = k8sPatchSpecFactory.copyStorageClass(currentV1StorageClassRow);

                    this.setStorageClassLabelAnno(clusterVolume, updateV1StorageClass, isDefault, BooleanUtils.toBoolean(isDefault));
                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentV1StorageClassRow, updateV1StorageClass);

                    // patch
                    k8sWorker.patchStorageClassV1(cluster, currentV1StorageClassRow.getMetadata().getName(), patchBody);
                }
            }
        } else {
            V1StorageClass currentV1StorageClass = k8sWorker.getStorageClassV1(cluster, clusterVolume.getName());
            V1StorageClass updateV1StorageClass = k8sPatchSpecFactory.copyStorageClass(currentV1StorageClass);

            this.setStorageClassLabelAnno(clusterVolume, updateV1StorageClass, "false", true);
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentV1StorageClass, updateV1StorageClass);

            // patch
            k8sWorker.patchStorageClassV1(cluster, currentV1StorageClass.getMetadata().getName(), patchBody);
        }

        return clusterVolume;
    }

    public void patchStorageClassByYaml(ClusterVO cluster, K8sDeployYamlVO deployYaml) throws Exception {

        if (deployYaml != null) {
            List<Object> objs = ServerUtils.getYamlObjects(deployYaml.getYaml());
            if (CollectionUtils.isNotEmpty(objs) && objs.size() == 1) {
                K8sApiKindType k8sApiKindType = ServerUtils.getK8sKindInObject(objs.get(0), new JSON());
                if (k8sApiKindType == K8sApiKindType.STORAGE_CLASS) {
                    // update storageClass
                    V1StorageClass updateStorageClass = (V1StorageClass) objs.get(0);

                    // check yaml name
                    if (StringUtils.equals(deployYaml.getName(), updateStorageClass.getMetadata().getName())) {
                        V1StorageClass currentStorageClass = k8sWorker.getStorageClassV1(cluster, updateStorageClass.getMetadata().getName());

                        boolean currentIsDefault = this.isDefaultStorageClass(currentStorageClass.getMetadata().getAnnotations());
                        boolean updateIsDefault = this.isDefaultStorageClass(updateStorageClass.getMetadata().getAnnotations());
                        // 기본 스토리지는 지정만 가능, 지정 해제 불가
                        if (currentIsDefault) {
                            if (!updateIsDefault) {
                                throw new CocktailException("Primary storage cannot be released.", ExceptionType.CanNotReleasedDefaultStorage);
                            }
                        }

                        if (updateIsDefault) {
                            List<V1StorageClass> v1StorageClasses = k8sWorker.getStorageClassesV1(cluster, String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, updateStorageClass.getMetadata().getName()), null);

                            if (CollectionUtils.isNotEmpty(v1StorageClasses)) {
                                for (V1StorageClass currentV1StorageClassRow : v1StorageClasses) {
                                    V1StorageClass updateV1StorageClass = k8sPatchSpecFactory.copyStorageClass(currentV1StorageClassRow);

                                    if (MapUtils.getString(updateV1StorageClass.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT) != null) {
                                        updateV1StorageClass.getMetadata().getAnnotations().put(KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT, "false");
                                    } else {
                                        updateV1StorageClass.getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RELEASE_STORAGE_CLASS_IS_DEFAULT, "false");
                                    }

                                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentV1StorageClassRow, updateV1StorageClass);

                                    // patch
                                    k8sWorker.patchStorageClassV1(cluster, currentV1StorageClassRow.getMetadata().getName(), patchBody);
                                }
                            }
                        }

                        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentStorageClass, updateStorageClass);
                        // patch
                        k8sWorker.patchStorageClassV1(cluster, updateStorageClass.getMetadata().getName(), patchBody);
                    } else {
                        throw new CocktailException("name parameter is invalid.", ExceptionType.InvalidYamlData);
                    }
                } else {
                    throw new CocktailException("Yaml is invalid.(it is not StorageClass).", ExceptionType.InvalidYamlData);
                }
            } else {
                throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
            }
        } else {
            throw new CocktailException("StorageClass parameter is null.", ExceptionType.InvalidParameter);
        }
    }

    private void setStorageClassLabelAnno(ClusterVolumeVO clusterVolume, V1StorageClass updateV1StorageClass, String isDefault, boolean isOwner) throws Exception {
        if (isOwner) {
            // labels
            Map<String, String> labels = K8sSpecFactory.buildStorageClassLabel(clusterVolume);
            k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(updateV1StorageClass.getMetadata().getLabels(), labels);
            updateV1StorageClass.getMetadata().setLabels(labels);

            // annotations
            Map<String, String> annotations = K8sSpecFactory.buildStorageClassAnno(clusterVolume);
            k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(updateV1StorageClass.getMetadata().getAnnotations(), annotations);
            updateV1StorageClass.getMetadata().setAnnotations(annotations);
        }

        if (MapUtils.getString(updateV1StorageClass.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT) != null) {
            updateV1StorageClass.getMetadata().getAnnotations().put(KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT, isDefault);
        } else {
            updateV1StorageClass.getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RELEASE_STORAGE_CLASS_IS_DEFAULT, isDefault);
        }
    }

    public Boolean isDefaultStorageClass(Map<String, String> annotations) throws Exception {
        // default storage class 셋팅
        if (MapUtils.isNotEmpty(annotations)) {
            if (MapUtils.getBoolean(annotations, KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT) != null) {
                return MapUtils.getBooleanValue(annotations, KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT, false);
            } else if (MapUtils.getBoolean(annotations, KubeConstants.META_ANNOTATIONS_RELEASE_STORAGE_CLASS_IS_DEFAULT) != null) {
                return MapUtils.getBooleanValue(annotations, KubeConstants.META_ANNOTATIONS_RELEASE_STORAGE_CLASS_IS_DEFAULT, false);
            } else {
                return false;
            }
        }

        return false;
    }

    /**
     * K8S StorageClass 삭제
     *
     * @param clusterSeq
     * @param storageClassName
     * @param cluster
     * @return
     * @throws Exception
     */
    public void deleteStorageClass(Integer clusterSeq, String storageClassName, ClusterVO cluster) throws Exception {

        if(cluster == null){
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            cluster = clusterDao.getCluster(clusterSeq);
        }

        K8sStorageClassVO k8sStorageClass = this.getStorageClass(cluster, storageClassName, new ExecutingContextVO());

        if(k8sStorageClass != null){
            k8sWorker.deleteStorageClassV1(cluster, storageClassName);
        }else {
            log.info("{}:{} is not found during delete!!", storageClassName, clusterSeq);
        }
    }

    /**
     * K8S StorageClass 목록 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sStorageClassVO> getStorageClasses(ClusterVO cluster, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){
                return this.convertStorageClassDataList(cluster, fieldSelector, labelSelector, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getStorageClasses fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S StorageClass 목록 조회
     *
     * @param clusterSeq
     * @param fieldSelector
     * @param labelSelector
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sStorageClassVO> getStorageClasses(Integer clusterSeq, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception{

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        if(cluster != null){
            return this.getStorageClasses(cluster, fieldSelector, labelSelector, context);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    /**
     * K8S StorageClass 정보 조회
     *
     * @param cluster
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sStorageClassVO getStorageClass(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(name)){

                V1StorageClass v1StorageClass = k8sWorker.getStorageClassV1(cluster, name);

                if(v1StorageClass != null){
                    return this.convertStorageClassData(new K8sStorageClassVO(), v1StorageClass, new JSON());
                }else {
                    return null;
                }
            }else{
                throw new CocktailException("cluster/name is null.", ExceptionType.InvalidParameter);
            }
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getStorageClass fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S StorageClass 정보 조회
     *
     * @param clusterSeq
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sStorageClassVO getStorageClass(Integer clusterSeq, String name, ExecutingContextVO context) throws Exception{

        try {
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            ClusterVO cluster = clusterDao.getCluster(clusterSeq);

            return this.getStorageClass(cluster, name, context);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getStorageClass fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S StorageClass 정보 조회 후 V1StorageClass -> K8sStorageClassVO 변환
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sStorageClassVO> convertStorageClassDataList(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sStorageClassVO> sStorageClasses = new ArrayList<>();
        List<V1StorageClass> v1StorageClasses = k8sWorker.getStorageClassesV1(cluster, field, label);

        if(CollectionUtils.isNotEmpty(v1StorageClasses)){

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for(V1StorageClass v1StorageClassRow : v1StorageClasses){
                K8sStorageClassVO storageClass = new K8sStorageClassVO();
                this.convertStorageClassData(storageClass, v1StorageClassRow, k8sJson);

                sStorageClasses.add(storageClass);

            }

        }

        return sStorageClasses;
    }

    /**
     * K8S StorageClass 정보 조회 후 V1StorageClass -> K8sStorageClassVO 변환
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public Map<String, K8sStorageClassVO> convertStorageClassDataMap(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception {

        Map<String, K8sStorageClassVO> storageClassMap = new HashMap<>();
        List<V1StorageClass> v1StorageClasses = k8sWorker.getStorageClassesV1(cluster, field, label);

        if(CollectionUtils.isNotEmpty(v1StorageClasses)){

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for(V1StorageClass v1StorageClassRow : v1StorageClasses){
                if (v1StorageClassRow != null && v1StorageClassRow.getMetadata() != null) {
                    K8sStorageClassVO storageClass = new K8sStorageClassVO();
                    this.convertStorageClassData(storageClass, v1StorageClassRow, k8sJson);

                    storageClassMap.put(v1StorageClassRow.getMetadata().getName(), storageClass);
                }
            }

        }

        return storageClassMap;
    }

    /**
     * K8S StorageClass 정보 조회 후 V1StorageClass -> K8sStorageClassVO 변환
     *
     * @param storageClass
     * @param v1StorageClass
     * @throws Exception
     */
    public K8sStorageClassVO convertStorageClassData(K8sStorageClassVO storageClass, V1StorageClass v1StorageClass, JSON k8sJson) throws Exception {

        if(v1StorageClass != null && v1StorageClass.getMetadata() != null){
            if(k8sJson == null){
                k8sJson = new JSON();
            }
            if(storageClass == null) {
                storageClass = new K8sStorageClassVO();
            }

            storageClass.setName(v1StorageClass.getMetadata().getName());
            storageClass.setLabels(v1StorageClass.getMetadata().getLabels());
            storageClass.setProvisioner(v1StorageClass.getProvisioner());
            storageClass.setParameters(v1StorageClass.getParameters());
            storageClass.setCreationTimestamp(v1StorageClass.getMetadata().getCreationTimestamp());
            storageClass.setDeployment(k8sJson.serialize(v1StorageClass));
            storageClass.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1StorageClass));

            K8sStorageClassDetailVO storageClassDetail = new K8sStorageClassDetailVO();
            storageClassDetail.setName(v1StorageClass.getMetadata().getName());
            storageClassDetail.setAnnotations(v1StorageClass.getMetadata().getAnnotations());
            storageClassDetail.setCreationTime(v1StorageClass.getMetadata().getCreationTimestamp());
            storageClassDetail.setLabels(v1StorageClass.getMetadata().getLabels());
            storageClassDetail.setProvisioner(v1StorageClass.getProvisioner());
            storageClassDetail.setParameters(v1StorageClass.getParameters());
            storageClassDetail.setAllowVolumeExpansion(v1StorageClass.getAllowVolumeExpansion());
            storageClassDetail.setMountOptions(v1StorageClass.getMountOptions());
            storageClassDetail.setReclaimPolicy(v1StorageClass.getReclaimPolicy());
            storageClassDetail.setVolumeBindingMode(v1StorageClass.getVolumeBindingMode());
            storageClass.setDetail(storageClassDetail);
        }

        return storageClass;
    }

}
