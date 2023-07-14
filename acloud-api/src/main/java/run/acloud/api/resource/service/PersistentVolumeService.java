package run.acloud.api.resource.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.TypeRef;
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
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.service.ClusterVolumeService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ClusterVolumeParamterVO;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.enums.*;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.K8sMapperUtils;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 20.
 */
@Slf4j
@Service
public class PersistentVolumeService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ClusterVolumeService clusterVolumeService;

    @Autowired
    private ServicemapService servicemapService;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    @Autowired
    private WorkloadResourceService workloadResourceService;


    /**
     * PersistentVolumeClaim 생성 (Invoke From Snapshot Deployment)
     *
     * @param cluster
     * @param pvcs
     * @param context
     * @throws Exception
     */
    public void createMultiplePersistentVolumeClaim(ClusterVO cluster, List<PersistentVolumeClaimIntegrateVO> pvcs, ExecutingContextVO context) throws Exception {
        if(CollectionUtils.isNotEmpty(pvcs)){
            for(PersistentVolumeClaimIntegrateVO pvc : pvcs){
                if(DeployType.valueOf(pvc.getDeployType()) == DeployType.GUI) {
                    PersistentVolumeClaimGuiVO pvcGui = null;
                    try {
                        pvcGui = (PersistentVolumeClaimGuiVO) pvc;
                        this.createPersistentVolumeClaim(cluster, cluster.getNamespaceName(), pvcGui, context);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("PersistentVolumeClaim Deployment Failure : createMultiplePersistentVolumeClaim : %s\n%s", ex.getMessage(), JsonUtils.toGson(pvcGui)));
                    }
                }
                else if(DeployType.valueOf(pvc.getDeployType()) == DeployType.YAML) {
                    PersistentVolumeClaimYamlVO pvcYaml = null;
                    try {
                        pvcYaml = (PersistentVolumeClaimYamlVO) pvc;
                        V1PersistentVolumeClaim v1PersistentVolumeClaim = ServerUtils.unmarshalYaml(pvcYaml.getYaml(), K8sApiKindType.PERSISTENT_VOLUME_CLAIM);
                        k8sWorker.createPersistentVolumeClaimV1(cluster, cluster.getNamespaceName(), v1PersistentVolumeClaim);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("PersistentVolumeClaim Deployment Failure : createMultiplePersistentVolumeClaim : %s\n%s", ex.getMessage(), JsonUtils.toGson(pvcYaml)));
                    }
                }
                else {
                    log.error(String.format("Invalid PersistentVolumeClaim DeployType : createMultiplePersistentVolumeClaim : %s", JsonUtils.toGson(pvc)));
                }
            }
        }
    }

    /**
     * PersistentVolumeClaim, PersistentVolume 생성
     *
     * @param servicemapSeq
     * @param persistentVolumeClaim
     * @return
     * @throws Exception
     */
    public void createPersistentVolumeClaim(Integer servicemapSeq, PersistentVolumeClaimGuiVO persistentVolumeClaim, ExecutingContextVO context) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        this.createPersistentVolumeClaim(cluster, cluster.getNamespaceName(), persistentVolumeClaim, context);
    }
    public void createPersistentVolumeClaim(ClusterVO cluster, String namespace, PersistentVolumeClaimGuiVO persistentVolumeClaim, ExecutingContextVO context) throws Exception {
        try {

            if(persistentVolumeClaim != null){
                if(StringUtils.isNotBlank(persistentVolumeClaim.getStorageVolumeName())){
                    /**
                     * cluster volume storage 정보 조회
                     */
                    ClusterVolumeVO clusterVolume = clusterVolumeService.getStorageVolume(cluster.getClusterSeq(), persistentVolumeClaim.getStorageVolumeName());

                    if(clusterVolume != null){
                        /**
                         * cluster volume storage parameter 가 있다면 셋팅
                         * ClusterVolumeParamterVO -> PersistentVolumeParamterVO
                         */
                        List<PersistentVolumeParamterVO> persistentVolumeParamters = new ArrayList<>();
                        if(CollectionUtils.isNotEmpty(clusterVolume.getParameters())){
                            for(ClusterVolumeParamterVO clusterVolumeParamterRow : clusterVolume.getParameters()){
                                PersistentVolumeParamterVO persistentVolumeParamter = new PersistentVolumeParamterVO();
                                BeanUtils.copyProperties(persistentVolumeParamter, clusterVolumeParamterRow);
                                persistentVolumeParamters.add(persistentVolumeParamter);
                            }
                        }

                        // 해당 pvc명으로 생성된 pvc가 존재하는 지 조회
                        V1PersistentVolumeClaim v1PersistentVolumeClaim = k8sWorker.getPersistentVolumeClaimV1WithName(cluster, namespace, persistentVolumeClaim.getName());
                        if(v1PersistentVolumeClaim != null){
                            throw new CocktailException("PVC is exists!!", ExceptionType.K8sVolumeAlreadyExists_PVC);
                        }

                        /**
                         * PERSISTENT_VOLUME_STATIC 이라면 먼저 PV를 생성
                         */
                        if(clusterVolume.getType() == VolumeType.PERSISTENT_VOLUME_STATIC){

                            PersistentVolumeVO persistentVolume = new PersistentVolumeVO();
                            BeanUtils.copyProperties(persistentVolume, persistentVolumeClaim);

                            // pv명은 아래 format으로 변경
                            persistentVolume.setName(ResourceUtil.makePersistentVolumeName(ResourceUtil.COCKTAIL_CUSTOM_PV_NAME_FORMAT, persistentVolumeClaim.getName()));
                            persistentVolume.setReclaimPolicy(clusterVolume.getReclaimPolicy());
                            persistentVolume.setPlugin(clusterVolume.getPlugin());
                            persistentVolume.setStorageVolumeName(clusterVolume.getName());
                            persistentVolume.setParameters(persistentVolumeParamters);

                            // 해당 pv명으로 생성된 pv가 존재하는 지 조회
                            V1PersistentVolume v1PersistentVolume = k8sWorker.getPersistentVolumeV1WithName(cluster, persistentVolume.getName());

                            if(v1PersistentVolume == null){
                                try {
                                    /**
                                     * PV 생성
                                     */
                                    V1PersistentVolume v1PersistentVolumeParam = K8sSpecFactory.buildPersistentVolumeV1(persistentVolume);
                                    k8sWorker.createPersistentVolumeV1(cluster, v1PersistentVolumeParam);
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                    throw new CocktailException("PV creation fail!!", e, ExceptionType.K8sVolumeCreationFail);
                                }

                                // pvc생성시 사용할 pv 명 셋팅
                                persistentVolumeClaim.setVolumeName(persistentVolume.getName());

                            }else{
                                throw new CocktailException("PV is exists!!", ExceptionType.K8sVolumeAlreadyExists_PV);
                            }

                        }

                        try {
                            /**
                             * PVC 생성
                             */
                            if (clusterVolume.getType() == VolumeType.PERSISTENT_VOLUME) {
                                persistentVolumeClaim.setStorageClassName(clusterVolume.getName());
                            } else {
                                persistentVolumeClaim.setStorageClassName("");
                            }
                            persistentVolumeClaim.setReclaimPolicy(clusterVolume.getReclaimPolicy());
                            persistentVolumeClaim.setParameters(persistentVolumeParamters);
                            V1PersistentVolumeClaim v1PersistentVolumeClaimParam = K8sSpecFactory.buildPersistentVolumeClaimV1(persistentVolumeClaim);
                            k8sWorker.createPersistentVolumeClaimV1(cluster, namespace, v1PersistentVolumeClaimParam);
                        } catch (Exception e) {
                            // pvc 생성 실패시 만약 pv가 생성되어 있다면 삭제하여 롤백 처리
                            if(StringUtils.isNotBlank(persistentVolumeClaim.getVolumeName())){
                                k8sWorker.deletePersistentVolumeV1(cluster, persistentVolumeClaim.getVolumeName());
                                Thread.sleep(100);
                            }
                            if (e instanceof CocktailException) {
                                throw e;
                            } else {
                                throw new CocktailException("PVC creation fail!!", e, ExceptionType.K8sVolumeClaimCreationFail);
                            }
                        }

                        Thread.sleep(1000);
                    }else{
                        throw new CocktailException("cluster volume is null!!", ExceptionType.InvalidParameter);
                    }
                }else{
                    throw new CocktailException("StorageVolumeName parameter is null!!", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("persistentVolumeClaim parameter is null!!", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            if (e instanceof CocktailException) {
                throw e;
            } else {
                throw new CocktailException("createPersistentVolumeClaim fail!!", e, ExceptionType.K8sCocktailCloudCreateFail);
            }
        }
    }

    /**
     * PersistentVolumeClaim 수정
     * 다른값은 변경할 수 없고 실제 변경되는 값은 storage 값만 변경됨
     *
     * @param servicemapSeq
     * @param persistentVolumeClaimName
     * @param persistentVolumeClaim
     * @param context
     * @throws Exception
     */
    public void updatePersistentVolumeClaim(Integer servicemapSeq, String persistentVolumeClaimName, PersistentVolumeClaimGuiVO persistentVolumeClaim, ExecutingContextVO context) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        this.updatePersistentVolumeClaim(cluster, cluster.getNamespaceName(), persistentVolumeClaimName, persistentVolumeClaim, context);
    }

    public void updatePersistentVolumeClaim(ClusterVO cluster, String namespace, String persistentVolumeClaimName, PersistentVolumeClaimGuiVO persistentVolumeClaim, ExecutingContextVO context) throws Exception {
        try {
            if(persistentVolumeClaim != null){
                if(StringUtils.isNotBlank(persistentVolumeClaim.getStorageVolumeName())){
                    /**
                     * cluster volume storage 정보 조회
                     */
                    ClusterVolumeVO clusterVolume = clusterVolumeService.getStorageVolume(cluster.getClusterSeq(), persistentVolumeClaim.getStorageVolumeName());

                    if(clusterVolume != null){
                        /**
                         * cluster volume storage parameter 가 있다면 셋팅
                         * ClusterVolumeParamterVO -> PersistentVolumeParamterVO
                         */
                        List<PersistentVolumeParamterVO> persistentVolumeParamters = new ArrayList<>();
                        if(CollectionUtils.isNotEmpty(clusterVolume.getParameters())){
                            for(ClusterVolumeParamterVO clusterVolumeParamterRow : clusterVolume.getParameters()){
                                PersistentVolumeParamterVO persistentVolumeParamter = new PersistentVolumeParamterVO();
                                BeanUtils.copyProperties(persistentVolumeParamter, clusterVolumeParamterRow);
                                persistentVolumeParamters.add(persistentVolumeParamter);
                            }
                        }

                        // 해당 pvc명으로 생성된 pvc가 존재하는 지 조회
//                        V1PersistentVolumeClaim currentPersistentVolumeClaim = k8sWorker.getPersistentVolumeClaimV1WithName(cluster, cluster.getNamespaceName(), persistentVolumeClaimName);
                        K8sPersistentVolumeClaimVO currentK8sPersistentVolumeClaim = this.getPersistentVolumeClaimDetail(cluster.getClusterSeq(), namespace, persistentVolumeClaimName, context);
                        V1PersistentVolumeClaim currentPVC = null;
                        V1PersistentVolumeClaim updatePVC = null;
                        if(currentK8sPersistentVolumeClaim == null){
                            throw new CocktailException("PVC is Not exists!!", ExceptionType.K8sVolumeNotExists_PVC);
                        } else {
                            currentPVC = Yaml.loadAs(currentK8sPersistentVolumeClaim.getDeploymentYaml(), V1PersistentVolumeClaim.class);

                            /**
                             * PVC 수정 - label, annotation, capacity 만 수정 가능
                             **/
                            // 1. 현재 PVC를 조회 업데이트할 updatePVC 객체에 셋팅
                            updatePVC = Yaml.loadAs(currentK8sPersistentVolumeClaim.getDeploymentYaml(), V1PersistentVolumeClaim.class);
                            // 2. 업데이트된 label, annotation, capacity를 반영하여 updateGenPVC 객체에 생성
                            if (clusterVolume.getType() == VolumeType.PERSISTENT_VOLUME) {
                                persistentVolumeClaim.setStorageClassName(clusterVolume.getName());
                            }
                            persistentVolumeClaim.setReclaimPolicy(clusterVolume.getReclaimPolicy());
                            persistentVolumeClaim.setParameters(persistentVolumeParamters);
                            V1PersistentVolumeClaim updateGenPVC = K8sSpecFactory.buildPersistentVolumeClaimV1(persistentVolumeClaim);

                            /** patchData를 만들기 전 Reserved Annotations and Labels 가 삭제되었을 경우 다시 넣어주어 제거할 수 없도록 함.. */
                            updatePVC.getMetadata().setLabels(updateGenPVC.getMetadata().getLabels());
                            k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(currentPVC.getMetadata().getLabels(), updatePVC.getMetadata().getLabels());
                            updatePVC.getMetadata().setAnnotations(updateGenPVC.getMetadata().getAnnotations());
                            k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(currentPVC.getMetadata().getAnnotations(), updatePVC.getMetadata().getAnnotations());

                            if (!currentPVC.getSpec().getResources().getRequests().get(KubeConstants.VOLUMES_STORAGE).equals(updateGenPVC.getSpec().getResources().getRequests().get(KubeConstants.VOLUMES_STORAGE))) {
                                /**
                                 * validation check
                                 */
                                if(!clusterVolume.getPlugin().canUpdatePVC()){
                                    throw new CocktailException(clusterVolume.getPlugin()+" PlugIn can not be modified dynamic.", ExceptionType.K8sVolumeClaimUpdateFail);
                                }

                                // PVC 이름이 변경되었는지 체크
                                if(!persistentVolumeClaimName.equals(persistentVolumeClaim.getName())){
                                    throw new CocktailException("PVC Name is modified.", ExceptionType.InvalidParameter);
                                }

                                // AsureDisk 일 경우 사용하는 Deployments 조회, 사용하는 deployments 존재시 수정 불가
                                if(clusterVolume.getPlugin() == VolumePlugIn.AZUREDISK){
//                            String serchLabel = String.format("%s=%s", persistentVolumeClaimName, KubeConstants.LABELS_VALUE_STORAGE);
//                            List<V1Deployment> deployments = k8sWorker.getDeploymentsV1(cluster, cluster.getNamespaceName(), null, serchLabel);
                                    if(CollectionUtils.isNotEmpty(currentK8sPersistentVolumeClaim.getServerParams())){
                                        throw new CocktailException(clusterVolume.getPlugin()+" PlugIn can not be modified when the server mounted is exist.", ExceptionType.K8sVolumeClaimUpdateFail);
                                    }
                                }

                                // 변경된 storage용량이 이전 용량 보다 작은지 체크
                                BigDecimal storage =  currentPVC.getSpec().getResources().getRequests().get(KubeConstants.VOLUMES_STORAGE).getNumber();
                                BigDecimal compareValueBig = updateGenPVC.getSpec().getResources().getRequests().get(KubeConstants.VOLUMES_STORAGE).getNumber();

                                // 이전 storage 값 보다 변경된 storage 값이 작을 경우
                                if(storage.compareTo(compareValueBig) == 1){
                                    throw new CocktailException("Capacity is small than the previous value.", ExceptionType.InvalidParameter);
                                }

                                // 용향 변경값 셋팅
                                updatePVC.getSpec().setResources(updateGenPVC.getSpec().getResources());
                            }
                        }

                        try {

                            // diff
                            List<JsonObject> body = k8sPatchSpecFactory.buildPatch(currentPVC, updatePVC);

                            // 수정사항이 있을경우
                            if(CollectionUtils.isNotEmpty(body)){

                                List<JsonObject> patchBody = new ArrayList<>();
                                Map<String, Object> patchMap = new HashMap<>();

                                // PV 수정 필요한 경우, PV 부터 수정
                                if(clusterVolume.getPlugin().canUpdatePV()){
                                    // 볼륨 정보 조회
                                    String volumeName = currentPVC.getSpec().getVolumeName();
                                    V1PersistentVolume currentPersistentVolume = k8sWorker.getPersistentVolumeV1WithName(cluster, volumeName);
                                    // 용량 수정 내용 생성
                                    patchMap.put("op", "replace");
                                    patchMap.put("path", String.format("/spec/capacity/%s", KubeConstants.VOLUMES_STORAGE));
                                    patchMap.put("value", String.format("%dGi", persistentVolumeClaim.getCapacity().intValue()));
                                    JsonObject updatePVBody = (JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject();
                                    patchBody.add(updatePVBody);
                                    V1PersistentVolume pvResult = k8sWorker.patchPersistentVolumeV1(cluster, currentPersistentVolume, patchBody);
                                }

                                // PVC 수정
                                V1PersistentVolumeClaim pvcResult = k8sWorker.patchPersistentVolumeClaimV1(cluster, namespace, updatePVC, body);
                            }
                        } catch (Exception e) {
                            if (e instanceof CocktailException) {
                                throw e;
                            } else {
                                throw new CocktailException("PVC update fail!!", e, ExceptionType.K8sVolumeClaimUpdateFail);
                            }
                        }

                    }else{
                        throw new CocktailException("cluster volume is null!!", ExceptionType.InvalidParameter);
                    }
                }else{
                    throw new CocktailException("StorageVolumeName parameter is null!!", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("persistentVolumeClaim parameter is null!!", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            if (e instanceof CocktailException) {
                throw e;
            } else {
                throw new CocktailException("updatePersistentVolumeClaim fail!!", e, ExceptionType.K8sCocktailCloudUpdateFail);
            }
        }
    }

    public void updatePersistentVolumeClaimByYaml(ClusterVO cluster, PersistentVolumeClaimYamlVO persistentVolumeClaimYaml, ExecutingContextVO context) throws Exception {
        if (persistentVolumeClaimYaml != null) {
            List<Object> objs = ServerUtils.getYamlObjects(persistentVolumeClaimYaml.getYaml());
            if (CollectionUtils.isNotEmpty(objs) && objs.size() == 1) {
                K8sApiKindType k8sApiKindType = ServerUtils.getK8sKindInObject(objs.get(0), new JSON());
                if (k8sApiKindType == K8sApiKindType.PERSISTENT_VOLUME_CLAIM) {
                    // update pvc
                    V1PersistentVolumeClaim updatePVC = (V1PersistentVolumeClaim) objs.get(0);
                    updatePVC.setStatus(null);

                    // check yaml namespace, name
                    if (StringUtils.equals(persistentVolumeClaimYaml.getNamespace(), updatePVC.getMetadata().getNamespace()) && StringUtils.equals(persistentVolumeClaimYaml.getName(), updatePVC.getMetadata().getName())) {
                        // current pvc
                        V1PersistentVolumeClaim currentPVC = k8sWorker.getPersistentVolumeClaimV1WithName(cluster, persistentVolumeClaimYaml.getNamespace(), persistentVolumeClaimYaml.getName());
                        currentPVC.setStatus(null);

                        // diff
                        List<JsonObject> body = k8sPatchSpecFactory.buildPatch(currentPVC, updatePVC);

                        // PVC 수정
                        V1PersistentVolumeClaim pvcResult = k8sWorker.patchPersistentVolumeClaimV1(cluster, persistentVolumeClaimYaml.getNamespace(),  updatePVC, body);
                    } else {
                        throw new CocktailException("namespace and name parameter is invalid.", ExceptionType.InvalidYamlData);
                    }

                } else {
                    throw new CocktailException("Yaml is invalid.(it is not PersistentVolumeClaim).", ExceptionType.InvalidYamlData);
                }
            } else {
                throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
            }
        } else {
            throw new CocktailException("persistentVolumeClaim parameter is null.", ExceptionType.InvalidParameter);
        }
    }

    public void deletePersistentVolumeClaim(ClusterVO cluster, String namespaceName, String persistentVolumeClaimName, ExecutingContextVO context) throws Exception {
        if(cluster != null){
            k8sWorker.deletePersistentVolumeClaimV1(cluster, namespaceName, persistentVolumeClaimName);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

//    public void deletePersistentVolumeClaim(Integer serviceSeq, Integer appmapSeq, String persistentVolumeClaimName, ExecutingContextVO context) throws Exception {
//        try {
//            ClusterVO cluster = this.setupCluster(serviceSeq, appmapSeq);
//
//            this.deletePersistentVolumeClaim(cluster, cluster.getNamespaceName(), persistentVolumeClaimName, context);
//        } catch (Exception e) {
//            throw new CocktailException("deletePersistentVolumeClaim fail!!", e, ExceptionType.K8sCocktailCloudDeleteFail);
//        }
//    }

    public void deletePersistentVolume(ClusterVO cluster, String persistentVolumeName, ExecutingContextVO context) throws Exception {
        if(cluster != null){
            k8sWorker.deletePersistentVolumeV1(cluster, persistentVolumeName);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    public void deletePersistentVolume(Integer servicemapSeq, String persistentVolumeName, ExecutingContextVO context) throws Exception {

        ClusterVO cluster = this.setupCluster(servicemapSeq);

        this.deletePersistentVolume(cluster, persistentVolumeName, context);

    }

    public void setPersistentVolumeInWorkload(List<K8sPersistentVolumeClaimVO> persistentVolumeClaims, String deployment_config, String volume_jsonPath, Map<String, K8sPersistentVolumeClaimVO> persistentVolumeClaimMap) throws Exception {

        List<V1Volume> volumes = ServerUtils.getObjectsInWorkload(deployment_config, volume_jsonPath, new TypeRef<List<V1Volume>>() {}, true);

        this.setPersistentVolumeInWorkload(persistentVolumeClaims, volumes, persistentVolumeClaimMap);
    }

    public void setPersistentVolumeInWorkload(List<K8sPersistentVolumeClaimVO> persistentVolumeClaims, List<V1Volume> volumes, Map<String, K8sPersistentVolumeClaimVO> persistentVolumeClaimMap) throws Exception {

        if(CollectionUtils.isNotEmpty(volumes)){
            String claimName = "";
            for(V1Volume volumeRow : volumes){
                if(volumeRow.getPersistentVolumeClaim() != null){
                    claimName = volumeRow.getPersistentVolumeClaim().getClaimName();
                    if (MapUtils.getObject(persistentVolumeClaimMap, claimName, null) != null) {
                        persistentVolumeClaims.add(persistentVolumeClaimMap.get(claimName));
                    }
                }
            }
        }
    }

    /**
     * K8S PersistentVolumeClaim 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaims(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception{
        return this.getPersistentVolumeClaims(clusterSeq, namespaceName, null ,null, context);
    }

    /**
     * K8S PersistentVolumeClaim 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaims(Integer clusterSeq, String namespaceName, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                return this.getPersistentVolumeClaims(cluster, namespaceName, field, label, context);
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumeClaims fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaims(ClusterVO cluster, String namespaceName, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){
                return this.convertPersistentVolumeClaimDataList(cluster, namespaceName, field, label);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumeClaims fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaims(Integer servicemapSeq, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            ClusterVO cluster = this.setupCluster(servicemapSeq);

            if(cluster != null){
                return this.getPersistentVolumeClaims(cluster, cluster.getNamespaceName(), field, label, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumeClaims fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<V1PersistentVolumeClaim> getPersistentVolumeClaimsV1(ClusterVO cluster, String namespaceName, String field, String label) throws Exception{
        try {
            if(cluster != null){
                return k8sWorker.getPersistentVolumeClaimsV1(cluster, namespaceName, field, label);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumeClaimsV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public K8sPersistentVolumeClaimVO getPersistentVolumeClaim(ClusterVO cluster, String namespaceName, String name, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){
                String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);
                List<K8sPersistentVolumeClaimVO> results = this.convertPersistentVolumeClaimDataList(cluster, namespaceName, field, null);

                if(CollectionUtils.isNotEmpty(results)){
                    return results.get(0);
                }else{
                    return null;
                }
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumeClaims fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S PersistentVolumeClaim 정보 조회 후 V1PersistentVolumeClaim -> K8sPersistentVolumeClaimVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sPersistentVolumeClaimVO> convertPersistentVolumeClaimDataList(ClusterVO cluster, String namespace, String field, String label) throws Exception {

        List<K8sPersistentVolumeClaimVO> pvcs = new ArrayList<>();

        try {
            List<V1PersistentVolumeClaim> v1PersistentVolumeClaims = k8sWorker.getPersistentVolumeClaimsV1(cluster, namespace, field, label);

            if(CollectionUtils.isNotEmpty(v1PersistentVolumeClaims)){

                this.genPersistentVolumeClaimData(cluster, pvcs, null, v1PersistentVolumeClaims);
            }
        } catch (Exception e) {
            throw new CocktailException("convertPersistentVolumeClaimDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return pvcs;
    }

    /**
     * K8S PersistentVolumeClaim 정보 조회
     * (cluster 모든 PVC)
     *
     * @param clusterSeq
     * @return
     * @throws Exception
     */
    public Map<String, K8sPersistentVolumeClaimVO> getPersistentVolumeClaimsByCluster(Integer clusterSeq, ExecutingContextVO context) throws Exception{

        return this.getPersistentVolumeClaimsByCluster(clusterSeq, null, null, context);
    }

    public Map<String, K8sPersistentVolumeClaimVO> getPersistentVolumeClaimsByCluster(Integer clusterSeq, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){

                    return this.convertPersistentVolumeClaimDataMap(cluster, null, field, label);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumeClaimsByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S PersistentVolumeClaim 정보 조회 후 V1PersistentVolumeClaim -> K8sPersistentVolumeClaimVO 변환
     * PV name 이 Key
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public Map<String, K8sPersistentVolumeClaimVO> convertPersistentVolumeClaimDataMap(ClusterVO cluster, String namespace, String field, String label) throws Exception {
        return this.convertPersistentVolumeClaimDataMap(cluster, namespace, field, label, false);
    }

    /**
     * K8S PersistentVolumeClaim 정보 조회 후 V1PersistentVolumeClaim -> K8sPersistentVolumeClaimVO 변환
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param useMapPvcKey
     * @return
     * @throws Exception
     */
    public Map<String, K8sPersistentVolumeClaimVO> convertPersistentVolumeClaimDataMap(ClusterVO cluster, String namespace, String field, String label, boolean useMapPvcKey) throws Exception {

        Map<String, K8sPersistentVolumeClaimVO> pvcMap = null;
        try {
            pvcMap = new HashMap<>();

            List<V1PersistentVolumeClaim> v1PersistentVolumeClaims = k8sWorker.getPersistentVolumeClaimsV1(cluster, namespace, field, label);

            if(CollectionUtils.isNotEmpty(v1PersistentVolumeClaims)){

                this.genPersistentVolumeClaimData(cluster, null, pvcMap, v1PersistentVolumeClaims, useMapPvcKey);
            }
        } catch (Exception e) {
            throw new CocktailException("convertPersistentVolumeClaimDataMap fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return pvcMap;
    }

    public void genPersistentVolumeClaimData(ClusterVO cluster, List<K8sPersistentVolumeClaimVO> pvcs, Map<String, K8sPersistentVolumeClaimVO> pvcMap, List<V1PersistentVolumeClaim> v1PersistentVolumeClaims) throws Exception {
        this.genPersistentVolumeClaimData(cluster, pvcs, pvcMap, v1PersistentVolumeClaims, false);
    }

    public void genPersistentVolumeClaimData(ClusterVO cluster, List<K8sPersistentVolumeClaimVO> pvcs, Map<String, K8sPersistentVolumeClaimVO> pvcMap, List<V1PersistentVolumeClaim> v1PersistentVolumeClaims, boolean useMapPvcKey) throws Exception {
        if(CollectionUtils.isNotEmpty(v1PersistentVolumeClaims)){
            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            for(V1PersistentVolumeClaim v1PersistentVolumeClaimRow : v1PersistentVolumeClaims){
                K8sPersistentVolumeClaimVO persistentVolumeClaim = this.convertPersistentVolumeClaim(v1PersistentVolumeClaimRow, k8sJson);

                if (cluster != null) {
                    persistentVolumeClaim.setClusterSeq(cluster.getClusterSeq());
                    persistentVolumeClaim.setClusterName(cluster.getClusterName());
                }

                if(pvcs != null){
                    pvcs.add(persistentVolumeClaim);
                }
                if(pvcMap != null){
                    if (useMapPvcKey) {
                        pvcMap.put(v1PersistentVolumeClaimRow.getMetadata().getName(), persistentVolumeClaim);
                    } else {
                        if (v1PersistentVolumeClaimRow.getSpec().getVolumeName() != null) {
                            pvcMap.put(v1PersistentVolumeClaimRow.getSpec().getVolumeName(), persistentVolumeClaim);
                        }
                    }
                }
            }
        }
    }

    public K8sPersistentVolumeClaimVO convertPersistentVolumeClaim(V1PersistentVolumeClaim v1PersistentVolumeClaim) throws Exception {
        return this.convertPersistentVolumeClaim(v1PersistentVolumeClaim, null);
    }
    private K8sPersistentVolumeClaimVO convertPersistentVolumeClaim(V1PersistentVolumeClaim v1PersistentVolumeClaim, JSON k8sJson) throws Exception {
        if(k8sJson == null) {
            k8sJson = new JSON();
        }
        K8sPersistentVolumeClaimVO persistentVolumeClaim = new K8sPersistentVolumeClaimVO();

        Map<String, String> labels = Optional.ofNullable(v1PersistentVolumeClaim.getMetadata().getLabels()).orElseGet(() ->Maps.newHashMap());

        // Persistent Volume Claim 목록
        persistentVolumeClaim.setLabel(null);
        persistentVolumeClaim.setNamespace(v1PersistentVolumeClaim.getMetadata().getNamespace());
        persistentVolumeClaim.setName(v1PersistentVolumeClaim.getMetadata().getName());
        persistentVolumeClaim.setPersistentVolumeType(v1PersistentVolumeClaim.getSpec().getAccessModes().contains(AccessMode.RWO.getValue()) ? PersistentVolumeType.SINGLE.getCode() : PersistentVolumeType.SHARED.getCode());

        /**
         * Phase
         *  A volume will be in one of the following phases:
         *
         *  Available – a free resource that is not yet bound to a claim
         *  Bound – the volume is bound to a claim
         *  Released – the claim has been deleted, but the resource is not yet reclaimed by the cluster
         *  Failed – the volume has failed its automatic reclamation
         */
        if(v1PersistentVolumeClaim.getStatus() != null) { // 조회가 아닌 배포용 YAML Parsing할때는 Status가 없을 수 있음.. 2020.03.03
            persistentVolumeClaim.setStatus(v1PersistentVolumeClaim.getStatus().getPhase());
        }
        persistentVolumeClaim.setVolumeName(v1PersistentVolumeClaim.getSpec().getVolumeName());
        persistentVolumeClaim.setCapacity(k8sWorker.convertReasourceMap(v1PersistentVolumeClaim.getSpec().getResources().getRequests()));
        persistentVolumeClaim.setCapacityByte(k8sWorker.convertQuantityToLong(v1PersistentVolumeClaim.getSpec().getResources().getRequests().get(KubeConstants.VOLUMES_STORAGE)));
        persistentVolumeClaim.setAccessModes(v1PersistentVolumeClaim.getSpec().getAccessModes());
        persistentVolumeClaim.setStorageClassName(v1PersistentVolumeClaim.getSpec().getStorageClassName());
        persistentVolumeClaim.setCreationTimestamp(v1PersistentVolumeClaim.getMetadata().getCreationTimestamp());
        persistentVolumeClaim.setDeployment(k8sJson.serialize(v1PersistentVolumeClaim));
        persistentVolumeClaim.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1PersistentVolumeClaim));

        // PersistentVolumeClaim Detail
        K8sPersistentVolumeClaimDetailVO persistentVolumeClaimDetail = new K8sPersistentVolumeClaimDetailVO();
        persistentVolumeClaimDetail.setName(v1PersistentVolumeClaim.getMetadata().getName());
        persistentVolumeClaimDetail.setNamespace(v1PersistentVolumeClaim.getMetadata().getNamespace());
        persistentVolumeClaimDetail.setLabels(v1PersistentVolumeClaim.getMetadata().getLabels());
        persistentVolumeClaimDetail.setAnnotations(v1PersistentVolumeClaim.getMetadata().getAnnotations());
        persistentVolumeClaimDetail.setCreationTime(v1PersistentVolumeClaim.getMetadata().getCreationTimestamp());
        String selectorJson = k8sJson.serialize(v1PersistentVolumeClaim.getSpec().getSelector());
        persistentVolumeClaimDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
        if(v1PersistentVolumeClaim.getStatus() != null) { // 조회가 아닌 배포용 YAML Parsing할때는 Status가 없을 수 있음.. 2020.03.03
            persistentVolumeClaimDetail.setStatus(v1PersistentVolumeClaim.getStatus().getPhase());
        }
        persistentVolumeClaimDetail.setVolumeName(v1PersistentVolumeClaim.getSpec().getVolumeName());
        persistentVolumeClaimDetail.setAccessModes(v1PersistentVolumeClaim.getSpec().getAccessModes());
        persistentVolumeClaimDetail.setStorageClassName(v1PersistentVolumeClaim.getSpec().getStorageClassName());
        persistentVolumeClaimDetail.setCapacity(k8sWorker.convertReasourceMap(v1PersistentVolumeClaim.getSpec().getResources().getRequests()));
        persistentVolumeClaim.setDetail(persistentVolumeClaimDetail);

        return persistentVolumeClaim;
    }

    /**
     * K8S PersistentVolume 정보 조회
     *
     * @param clusterSeq
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
//    public K8sPersistentVolumeDetailVO getPersistentVolumeByCluster(Integer clusterSeq, String name, ExecutingContextVO context) throws Exception{
//
//        try {
//            if(clusterSeq != null){
//                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
//
//                ClusterVO cluster = clusterDao.getCluster(clusterSeq);
//
//                if(cluster != null){
//
//                    return this.convertPersistentVolumeData(cluster, name);
//                }else{
//                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
//                }
//            }else{
//                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
//            }
//        } catch (Exception e) {
//            throw new CocktailException("getPersistentVolumeByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
//        }
//    }

    /**
     * K8S PersistentVolumeClaim 정보 조회 후 V1PersistentVolumeClaim -> K8sPersistentVolumeClaimVO 변환
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public K8sPersistentVolumeDetailVO convertPersistentVolumeData(ClusterVO cluster, String name) throws Exception {

        V1PersistentVolume v1PersistentVolume = k8sWorker.getPersistentVolumeV1WithName(cluster, name);

        return this.convertPersistentVolumeData(v1PersistentVolume);
    }

    private K8sPersistentVolumeDetailVO convertPersistentVolumeData(V1PersistentVolume v1PersistentVolume) throws Exception {

        K8sPersistentVolumeDetailVO persistentVolume = new K8sPersistentVolumeDetailVO();

        if(v1PersistentVolume != null){

            ObjectMapper mapper = K8sMapperUtils.getMapper();

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            persistentVolume.setName(v1PersistentVolume.getMetadata().getName());
            persistentVolume.setLabels(v1PersistentVolume.getMetadata().getLabels());
            persistentVolume.setAnnotations(v1PersistentVolume.getMetadata().getAnnotations());
            persistentVolume.setCreationTime(v1PersistentVolume.getMetadata().getCreationTimestamp());
            persistentVolume.setStatus(v1PersistentVolume.getStatus().getPhase());
            if(v1PersistentVolume.getSpec().getClaimRef() != null){
                persistentVolume.setClaimName(String.format("%s/%s", v1PersistentVolume.getSpec().getClaimRef().getNamespace(), v1PersistentVolume.getSpec().getClaimRef().getName()));
            }
            persistentVolume.setPersistentVolumeReclaimPolicy(v1PersistentVolume.getSpec().getPersistentVolumeReclaimPolicy());
            persistentVolume.setAccessModes(v1PersistentVolume.getSpec().getAccessModes());
            persistentVolume.setStorageClassName(v1PersistentVolume.getSpec().getStorageClassName());
            persistentVolume.setStatusReason(v1PersistentVolume.getStatus().getReason());
            persistentVolume.setStatusMessage(v1PersistentVolume.getStatus().getMessage());
            persistentVolume.setCapacity(k8sWorker.convertReasourceMap(v1PersistentVolume.getSpec().getCapacity()));
//            persistentVolume.setVolumeMode(v1PersistentVolume.getSpec().getVolumeMode());

            // Volume Source
            // Volume별로 spec이 상이하여 Json으로 내려줌
            String volumeSourceJson = k8sJson.serialize(v1PersistentVolume.getSpec());
            Map<String, Object> volumeSourceMap = mapper.readValue(volumeSourceJson, new TypeReference<Map<String, Object>>(){});
            if(volumeSourceMap != null && !volumeSourceMap.isEmpty()){
                volumeSourceMap.remove("accessModes");
                volumeSourceMap.remove("capacity");
                volumeSourceMap.remove("claimRef");
                volumeSourceMap.remove("mountOptions");
                volumeSourceMap.remove("persistentVolumeReclaimPolicy");
                volumeSourceMap.remove("storageClassName");
                volumeSourceMap.remove("volumeMode");
                volumeSourceMap.remove("nodeAffinity");

                persistentVolume.setVolumeSource(k8sJson.serialize(volumeSourceMap));
            }
        }

        return persistentVolume;
    }

    /**
     * K8S PersistentVolume 정보 조회
     *
     * @param clusterSeq
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sPersistentVolumeVO> getPersistentVolumes(Integer clusterSeq, ExecutingContextVO context) throws Exception{

        return this.getPersistentVolumes(clusterSeq, null, null, context);
    }

    /**
     * K8S PersistentVolume 정보 조회
     *
     * @param clusterSeq
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sPersistentVolumeVO> getPersistentVolumes(Integer clusterSeq, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){

                    return this.convertPersistentVolumeDataList(cluster, field, label);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumes fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S PersistentVolume 정보 조회 후 V1PersistentVolume -> K8sPersistentVolumeVO 변환
     * to List
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sPersistentVolumeVO> convertPersistentVolumeDataList(ClusterVO cluster, String field, String label) throws Exception {

        List<K8sPersistentVolumeVO> persistentVolumes = new ArrayList<>();
        List<V1PersistentVolume> v1PersistentVolumes = k8sWorker.getPersistentVolumesV1(cluster, field, label);

        if(CollectionUtils.isNotEmpty(v1PersistentVolumes)){

            this.genPersistentVolumesData(persistentVolumes, null, v1PersistentVolumes, null);

        }

        return persistentVolumes;
    }

    /**
     * K8S PersistentVolume 정보 조회
     *
     * @param clusterSeq
     * @param context
     * @return
     * @throws Exception
     */
//    public Map<String, K8sPersistentVolumeVO> getPersistentVolumesMap(Integer clusterSeq, String namespace, String field, String label, ExecutingContextVO context) throws Exception{
//
//        try {
//            if(clusterSeq != null){
//                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
//
//                ClusterVO cluster = clusterDao.getCluster(clusterSeq);
//
//                if(cluster != null){
//
//                    return this.getPersistentVolumesMap(cluster, namespace, field, label, context);
//                }else{
//                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
//                }
//            }else{
//                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
//            }
//        } catch (Exception e) {
//            throw new CocktailException("getPersistentVolumesMap fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
//        }
//    }

    /**
     * K8S PersistentVolume 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public Map<String, K8sPersistentVolumeVO> getPersistentVolumesMap(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){

                return this.convertPersistentVolumeDataMap(cluster, namespace, field, label);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumesMap fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S PersistentVolume 정보 조회
     *
     * @param servicemapSeq
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public Map<String, K8sPersistentVolumeVO> getPersistentVolumesMap(Integer servicemapSeq, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(servicemapSeq != null){
                ClusterVO cluster = this.setupCluster(servicemapSeq);

                if(cluster != null){

                    return this.getPersistentVolumesMap(cluster, cluster.getNamespaceName(), field, label, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("servicemapSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getPersistentVolumesMap fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S PersistentVolume 정보 조회 후 V1PersistentVolume -> K8sPersistentVolumeVO 변환
     * to Map
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public Map<String, K8sPersistentVolumeVO> convertPersistentVolumeDataMap(ClusterVO cluster, String namespace, String field, String label) throws Exception {

        Map<String, K8sPersistentVolumeVO> persistentVolumesMap = new HashMap<>();
        List<V1PersistentVolume> v1PersistentVolumes = k8sWorker.getPersistentVolumesV1(cluster, field, label);

        if(CollectionUtils.isNotEmpty(v1PersistentVolumes)){

            this.genPersistentVolumesData(null, persistentVolumesMap, v1PersistentVolumes, namespace);

        }

        return persistentVolumesMap;
    }

    public void genPersistentVolumesData(List<K8sPersistentVolumeVO> pvs, Map<String, K8sPersistentVolumeVO> pvMap, List<V1PersistentVolume> v1PersistentVolumes, String namespace) throws Exception {
        if(CollectionUtils.isNotEmpty(v1PersistentVolumes)){

            ObjectMapper mapper = K8sMapperUtils.getMapper();

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            String claimNameWithNamespace = "";

            for(V1PersistentVolume v1PersistentVolumeRow : v1PersistentVolumes){
                K8sPersistentVolumeVO persistentVolume = new K8sPersistentVolumeVO();
                persistentVolume.setName(v1PersistentVolumeRow.getMetadata().getName());
                persistentVolume.setCapacity(v1PersistentVolumeRow.getSpec().getCapacity().get("storage") != null ? v1PersistentVolumeRow.getSpec().getCapacity().get("storage").toSuffixedString() : null);
                persistentVolume.setAccessModes(v1PersistentVolumeRow.getSpec().getAccessModes());
                persistentVolume.setPersistentVolumeReclaimPolicy(v1PersistentVolumeRow.getSpec().getPersistentVolumeReclaimPolicy());
                persistentVolume.setStatus(v1PersistentVolumeRow.getStatus().getPhase());
                if(v1PersistentVolumeRow.getSpec().getClaimRef() != null){
                    if(StringUtils.isNotBlank(v1PersistentVolumeRow.getSpec().getClaimRef().getNamespace())){
                        if(StringUtils.isNotBlank(namespace) && !StringUtils.equals(namespace, v1PersistentVolumeRow.getSpec().getClaimRef().getNamespace())){
                            continue;
                        }
                        claimNameWithNamespace = String.format("%s/%s", v1PersistentVolumeRow.getSpec().getClaimRef().getNamespace(), v1PersistentVolumeRow.getSpec().getClaimRef().getName());
                    }else{
                        claimNameWithNamespace = v1PersistentVolumeRow.getSpec().getClaimRef().getName();
                    }
                    persistentVolume.setClaimNameWithNamespace(claimNameWithNamespace);
                    persistentVolume.setClaimName(v1PersistentVolumeRow.getSpec().getClaimRef().getName());
                }
                persistentVolume.setStorageClassName(v1PersistentVolumeRow.getSpec().getStorageClassName());
                persistentVolume.setStatusReason(v1PersistentVolumeRow.getStatus().getReason());
                persistentVolume.setCreationTimestamp(v1PersistentVolumeRow.getMetadata().getCreationTimestamp());
                persistentVolume.setDeployment(k8sJson.serialize(v1PersistentVolumeRow));
                persistentVolume.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1PersistentVolumeRow));

                K8sPersistentVolumeDetailVO persistentVolumeDetail = this.convertPersistentVolumeData(v1PersistentVolumeRow);
//                persistentVolumeDetail.setName(v1PersistentVolumeRow.getMetadata().getName());
//                persistentVolumeDetail.setLabels(v1PersistentVolumeRow.getMetadata().getLabels());
//                persistentVolumeDetail.setAnnotations(v1PersistentVolumeRow.getMetadata().getAnnotations());
//                persistentVolumeDetail.setCreationTime(v1PersistentVolumeRow.getMetadata().getCreationTimestamp());
//                persistentVolumeDetail.setStatus(v1PersistentVolumeRow.getStatus().getPhase());
//                if(v1PersistentVolumeRow.getSpec().getClaimRef() != null){
//                    persistentVolumeDetail.setClaimName(String.format("%s/%s", v1PersistentVolumeRow.getSpec().getClaimRef().getNamespace(), v1PersistentVolumeRow.getSpec().getClaimRef().getName()));
//                }
//                persistentVolumeDetail.setPersistentVolumeReclaimPolicy(v1PersistentVolumeRow.getSpec().getPersistentVolumeReclaimPolicy());
//                persistentVolumeDetail.setAccessModes(v1PersistentVolumeRow.getSpec().getAccessModes());
//                persistentVolumeDetail.setStorageClassName(v1PersistentVolumeRow.getSpec().getStorageClassName());
//                persistentVolumeDetail.setStatusReason(v1PersistentVolumeRow.getStatus().getReason());
//                persistentVolumeDetail.setStatusMessage(v1PersistentVolumeRow.getStatus().getMessage());
//                persistentVolumeDetail.setCapacity(k8sWorker.convertReasourceMap(v1PersistentVolumeRow.getSpec().getCapacity()));
//
//                // Volume Source
//                // Volume별로 spec이 상이하여 Json으로 내려줌
//                String volumeSourceJson = k8sJson.serialize(v1PersistentVolumeRow.getSpec());
//                Map<String, Object> volumeSourceMap = mapper.readValue(volumeSourceJson, new TypeReference<Map<String, Object>>(){});
//                if(volumeSourceMap != null && !volumeSourceMap.isEmpty()){
//                    volumeSourceMap.remove("accessModes");
//                    volumeSourceMap.remove("capacity");
//                    volumeSourceMap.remove("claimRef");
//                    volumeSourceMap.remove("mountOptions");
//                    volumeSourceMap.remove("persistentVolumeReclaimPolicy");
//                    volumeSourceMap.remove("storageClassName");
//                    volumeSourceMap.remove("volumeMode");
//
//                    persistentVolumeDetail.setVolumeSource(k8sJson.serialize(volumeSourceMap));
//                }

                persistentVolume.setDetail(persistentVolumeDetail);

                if(pvs != null){
                    pvs.add(persistentVolume);
                }
                if(pvMap != null){
                    pvMap.put(persistentVolume.getName(), persistentVolume);
                }

            }

        }
    }


    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimsInServicemap(Integer servicemapSeq, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        return this.getPersistentVolumeClaimsInServicemap(cluster, cluster.getNamespaceName(), fieldSelector, labelSelector, context);
    }

    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimsInServicemap(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception {

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = this.getPersistentVolumeClaims(cluster, namespace, fieldSelector, labelSelector, context);
        Map<String, K8sPersistentVolumeVO> persistentVolumesMap = this.getPersistentVolumesMap(cluster, namespace, null, null, context);

        String eventFieldSelector = String.format("%s=%s,%s=%s", "involvedObject.namespace", namespace, "involvedObject.kind", KubeConstants.KIND_PERSISTENT_VOLUME_CLAIM);
        List<K8sEventVO> persistentVolumeClaimEvents = k8sResourceService.convertEventDataList(cluster, null, eventFieldSelector, null);

        eventFieldSelector = String.format("%s=%s", "involvedObject.kind", KubeConstants.KIND_PERSISTENT_VOLUME);
        List<K8sEventVO> persistentVolumeEvents = k8sResourceService.convertEventDataList(cluster, null, eventFieldSelector, null);


        if(CollectionUtils.isNotEmpty(persistentVolumeClaims)){
            List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, null, cluster.getClusterSeq(), null, null, false, false);

            for(K8sPersistentVolumeClaimVO persistentVolumeClaimRow : persistentVolumeClaims) {
                this.setPersistentVolumeClaims(
                        cluster,
                        persistentVolumeClaimRow, persistentVolumesMap,
                        persistentVolumeClaimEvents, persistentVolumeEvents,
                        clusterVolumes,
                        null, null, null
                );
            }
        }

        return persistentVolumeClaims;
    }

    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimsInCluster(Integer clusterSeq, Integer serviceSeq, String fieldSelector, String labelSelector, Boolean acloudOnly, ExecutingContextVO context) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaimsTgt = new ArrayList<>();

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaimsSrc = this.getPersistentVolumeClaims(cluster, null, fieldSelector, labelSelector, context);
        Map<String, K8sPersistentVolumeVO> persistentVolumesMap = this.getPersistentVolumesMap(cluster, null, null, null, context);

        String eventFieldSelector = String.format("%s=%s", "involvedObject.kind", KubeConstants.KIND_PERSISTENT_VOLUME_CLAIM);
        List<K8sEventVO> persistentVolumeClaimEvents = k8sResourceService.convertEventDataList(cluster, null, eventFieldSelector, null);

        eventFieldSelector = String.format("%s=%s", "involvedObject.kind", KubeConstants.KIND_PERSISTENT_VOLUME);
        List<K8sEventVO> persistentVolumeEvents = k8sResourceService.convertEventDataList(cluster, null, eventFieldSelector, null);


        if(CollectionUtils.isNotEmpty(persistentVolumeClaimsSrc)){
            String resourcePrefixSource = ResourceUtil.getResourcePrefix();
            String resourcePrefixTarget = "";

            List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, null, cluster.getClusterSeq(), null, null, false, false);

            Set<String> namespaces = new HashSet<>();
            if (serviceSeq != null) {
                namespaces = servicemapService.getNamespaceNamesByServiceInCluster(serviceSeq, clusterSeq);
            } else {
                if (acloudOnly) {
                    namespaces = Sets.newHashSet(Optional.ofNullable(servicemapService.getNamespaceListOfCluster(clusterSeq)).orElseGet(() ->Lists.newArrayList()));
                }
            }

            for(K8sPersistentVolumeClaimVO persistentVolumeClaimRow : persistentVolumeClaimsSrc) {
                if (serviceSeq != null || acloudOnly) {
                    if (CollectionUtils.isNotEmpty(namespaces) && namespaces.contains(persistentVolumeClaimRow.getNamespace())) {

                    } else {
                        continue;
                    }
                }

                this.setPersistentVolumeClaims(
                        cluster,
                        persistentVolumeClaimRow, persistentVolumesMap,
                        persistentVolumeClaimEvents, persistentVolumeEvents,
                        clusterVolumes,
                        null, null, null
                );

                persistentVolumeClaimsTgt.add(persistentVolumeClaimRow);
            }

        }

        return persistentVolumeClaimsTgt;
    }

    public K8sPersistentVolumeClaimVO getPersistentVolumeClaimDetail(Integer clusterSeq, String namespaceName, String name, ExecutingContextVO context) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        return this.getPersistentVolumeClaimDetail(cluster, namespaceName, name, context);
    }

    public K8sPersistentVolumeClaimVO getPersistentVolumeClaimDetail(ClusterVO cluster, String namespaceName, String name, ExecutingContextVO context) throws Exception {

        K8sPersistentVolumeClaimVO persistentVolumeClaim = this.getPersistentVolumeClaim(cluster, namespaceName, name, context);

        if(persistentVolumeClaim != null){
            List<K8sPodVO> allPods = workloadResourceService.getPods(cluster, null, namespaceName, null, context);
            List<K8sJobVO> allJobs = workloadResourceService.getJobs(cluster, namespaceName, null, null, context);
            List<K8sReplicaSetVO> allRelicaSets = workloadResourceService.convertReplicaSetDataList(cluster, namespaceName, null, null);

            Map<String, K8sPersistentVolumeVO> persistentVolumesMap = this.convertPersistentVolumeDataMap(cluster, namespaceName, null, null);

            String eventFieldSelector = String.format("%s=%s,%s=%s", "involvedObject.namespace", namespaceName, "involvedObject.kind", KubeConstants.KIND_PERSISTENT_VOLUME_CLAIM);
            List<K8sEventVO> persistentVolumeClaimEvents = k8sResourceService.convertEventDataList(cluster, namespaceName, eventFieldSelector, null);

            eventFieldSelector = String.format("%s=%s", "involvedObject.kind", KubeConstants.KIND_PERSISTENT_VOLUME);
            List<K8sEventVO> persistentVolumeEvents = k8sResourceService.convertEventDataList(cluster, namespaceName, eventFieldSelector, null);

            List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, null, cluster.getClusterSeq(), null, null, false, false);
            Map<String, K8sJobVO> jobByCronJobMap = null;
            if (CollectionUtils.isNotEmpty(allJobs)) {
                jobByCronJobMap = allJobs.stream().filter(j -> (CollectionUtils.isNotEmpty(j.getDetail().getOwnerReferences()))).collect(Collectors.toMap(K8sJobVO::getName, Function.identity()));
            }
            Map<String, K8sReplicaSetVO> replicaSetMap = null;
            if (CollectionUtils.isNotEmpty(allRelicaSets)) {
                replicaSetMap = allRelicaSets.stream().filter(j -> (CollectionUtils.isNotEmpty(j.getDetail().getOwnerReferences()))).collect(Collectors.toMap(K8sReplicaSetVO::getName, Function.identity()));
            }

            this.setPersistentVolumeClaims(
                    cluster,
                    persistentVolumeClaim, persistentVolumesMap,
                    persistentVolumeClaimEvents, persistentVolumeEvents,
                    clusterVolumes,
                    allPods, replicaSetMap, jobByCronJobMap
            );
        }

        return persistentVolumeClaim;
    }

    public List<K8sPersistentVolumeClaimVO> getStorageVolumesInCluster(ClusterVO cluster, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception {
        /** PVC List Response Object **/
        List<K8sPersistentVolumeClaimVO> persistentVolumeClaimsTgt = new ArrayList<>();

        /** PVC List from K8s **/
        List<K8sPersistentVolumeClaimVO> persistentVolumeClaimsSrc = this.getPersistentVolumeClaims(cluster, null, fieldSelector, labelSelector, context);

        if(CollectionUtils.isNotEmpty(persistentVolumeClaimsSrc)) {
            /** PV Map from K8s **/
            Map<String, K8sPersistentVolumeVO> persistentVolumesMap = this.getPersistentVolumesMap(cluster, null, null, null, context);

            /** PVC Event List from K8s **/
            String eventFieldSelector = String.format("%s=%s", "involvedObject.kind", KubeConstants.KIND_PERSISTENT_VOLUME_CLAIM);
            List<K8sEventVO> persistentVolumeClaimEvents = k8sResourceService.convertEventDataList(cluster, null, eventFieldSelector, null);

            /** PV Event List from K8s **/
            eventFieldSelector = String.format("%s=%s", "involvedObject.kind", KubeConstants.KIND_PERSISTENT_VOLUME);
            List<K8sEventVO> persistentVolumeEvents = k8sResourceService.convertEventDataList(cluster, null, eventFieldSelector, null);

            String resourcePrefixSource = ResourceUtil.getResourcePrefix();
            String resourcePrefixTarget = "";

            /** 클러스터 내의 전체 Appmap 연결 정보를 조회 (할당 유형 CLUSTER Type을 처리하기 위함)**/
            Map<String, ServicemapSummaryVO> servicemapInfoMap = servicemapService.getServicemapSummaryMap(cluster.getClusterSeq());

            List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, null, cluster.getClusterSeq(), null, null, false, false);

            /**
             * 조회된 전체 PVC 루프 처리
             */
            for (K8sPersistentVolumeClaimVO pvcRow : persistentVolumeClaimsSrc) {
                pvcRow.setClusterSeq(cluster.getClusterSeq());
                pvcRow.setClusterName(cluster.getClusterName());

                /** 조회한 PVC의 워크스페이스 클러스터 네임스페이스등 연결 정보를 설정 **/
                if (MapUtils.isNotEmpty(servicemapInfoMap) && servicemapInfoMap.containsKey(pvcRow.getNamespace())) {
                    pvcRow.setServicemapInfo(servicemapInfoMap.get(pvcRow.getNamespace()));
                }

                this.setPersistentVolumeClaims(
                        cluster,
                        pvcRow, persistentVolumesMap,
                        persistentVolumeClaimEvents, persistentVolumeEvents,
                        clusterVolumes,
                        null, null, null
                );
                persistentVolumeClaimsTgt.add(pvcRow);
            }
        }
        persistentVolumeClaimsTgt.sort(Comparator.comparing(s -> s.getCreationTimestamp()));
        Collections.reverse(persistentVolumeClaimsTgt);

        int i = 1;
        if (log.isDebugEnabled() || log.isTraceEnabled()) {
//            SimpleDateFormat format = new SimpleDateFormat("YYYY MM dd HH:mm:ss", Locale.UK);
            for(K8sPersistentVolumeClaimVO pvc : persistentVolumeClaimsTgt) {
                log.debug(String.format("##### %d : %s : %s : %s : %s", i++, pvc.getCreationTimestamp().toString(), pvc.getName(), pvc.getNamespace(), JsonUtils.toGson(pvc.getServicemapInfo())));
            }
            log.debug("::::::::::::::::::::::::::::::::::::::::::::: {}", Optional.ofNullable(persistentVolumeClaimsTgt).map(List::size).orElseGet(() ->0));
        }


        return persistentVolumeClaimsTgt;
    }

    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimsInAccount(Integer accountSeq, Integer serviceSeq, String fieldSelector, String labelSelector, Boolean acloudOnly, ExecutingContextVO context) throws Exception {
        if (accountSeq != null) {
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, serviceSeq, null, null, null, "Y");
//            List<ClusterVO> clusters = clusterService.getClusterCondition(accountSeq, serviceSeq, false, false, false, false, false, false, false, false, ContextHolder.exeContext());

            if (CollectionUtils.isNotEmpty(clusters)) {
                List<K8sPersistentVolumeClaimVO> allPersistentVolumeClaims = new ArrayList<>();
                for (ClusterVO clusterRow : clusters) {
                    if (clusterStateService.isClusterRunning(clusterRow)) {
                        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = this.getPersistentVolumeClaimsInCluster(clusterRow.getClusterSeq(), serviceSeq, fieldSelector, labelSelector, acloudOnly, context);
                        if (CollectionUtils.isNotEmpty(persistentVolumeClaims)) {
                            allPersistentVolumeClaims.addAll(persistentVolumeClaims);
                        }
                    }
                }
                return allPersistentVolumeClaims;
            }
        }

        return new ArrayList<>();
    }

    public void setPersistentVolumeClaims(ClusterVO cluster,
                                           K8sPersistentVolumeClaimVO persistentVolumeClaimRow, Map<String, K8sPersistentVolumeVO> persistentVolumesMap,
                                           List<K8sEventVO> persistentVolumeClaimEvents, List<K8sEventVO> persistentVolumeEvents,
                                           List<ClusterVolumeVO> clusterVolumes,
                                           List<K8sPodVO> allPods, Map<String, K8sReplicaSetVO> replicaSetMap, Map<String, K8sJobVO> jobByCronJobMap
    ) throws Exception {

        // Set PersistentVolume
        persistentVolumeClaimRow.setPersistentVolume(persistentVolumesMap.get(persistentVolumeClaimRow.getVolumeName()));

        // Set Event
        if(CollectionUtils.isNotEmpty(persistentVolumeClaimEvents)){
            persistentVolumeClaimRow.getDetail().setEvents(persistentVolumeClaimEvents.stream().filter(pvce -> (StringUtils.equals(pvce.getName(), persistentVolumeClaimRow.getName()))).collect(Collectors.toList()));
        }
        if(CollectionUtils.isNotEmpty(persistentVolumeEvents)){
            if (persistentVolumeClaimRow.getPersistentVolume() != null){
                persistentVolumeClaimRow.getPersistentVolume().getDetail().setEvents(persistentVolumeEvents.stream().filter(pve -> (StringUtils.equals(pve.getName(), persistentVolumeClaimRow.getPersistentVolume().getName()))).collect(Collectors.toList()));
            }
        }

        if(persistentVolumeClaimRow != null){
            // label 처리
            persistentVolumeClaimRow.setPersistentVolumeType(Optional.ofNullable(persistentVolumeClaimRow.getDetail().getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.CUSTOM_PERSISTENT_VOLUME_TYPE));
            if (CollectionUtils.isNotEmpty(clusterVolumes)) {
                boolean isExists = false;
                for(ClusterVolumeVO clusterVolumeRow : clusterVolumes) {
                    // dynamic 일 경우 처리 (storageClass 존재 여부로 판단)
                    if (StringUtils.isNotBlank(persistentVolumeClaimRow.getStorageClassName())) {
                        if (StringUtils.equals(persistentVolumeClaimRow.getStorageClassName(), clusterVolumeRow.getName())) {
                            isExists = true;
                        }
                    } else {
                        // default storageClass로 배포시 'volume.beta.kubernetes.io/storage-class' annotation을 확인하여 storageClass를 확인하여 있다면 셋팅
                        if (persistentVolumeClaimRow.getStorageClassName() == null) {
                            if (MapUtils.isNotEmpty(persistentVolumeClaimRow.getDetail().getAnnotations())
                                    && MapUtils.getString(persistentVolumeClaimRow.getDetail().getAnnotations(), KubeConstants.VOLUMES_CLASS_NAME_BETA, null) != null) {
                                if (StringUtils.equals(persistentVolumeClaimRow.getDetail().getAnnotations().get(KubeConstants.VOLUMES_CLASS_NAME_BETA), clusterVolumeRow.getName())) {
                                    isExists = true;
                                    persistentVolumeClaimRow.setStorageClassName(persistentVolumeClaimRow.getDetail().getAnnotations().get(KubeConstants.VOLUMES_CLASS_NAME_BETA));
                                    persistentVolumeClaimRow.getDetail().setStorageClassName(persistentVolumeClaimRow.getDetail().getAnnotations().get(KubeConstants.VOLUMES_CLASS_NAME_BETA));
                                }
                            }
                        }

                        // static 일 경우 처리 (현재 nfs만 가능)
                        if (!isExists && StringUtils.isBlank(clusterVolumeRow.getProvisionerName()) && CollectionUtils.isNotEmpty(clusterVolumeRow.getParameters())) {
                            if (persistentVolumeClaimRow.getPersistentVolume() != null && StringUtils.isNotBlank(persistentVolumeClaimRow.getPersistentVolume().getDetail().getVolumeSource())) {
                                Map<String, Object> tempVolumeSourceMap = K8sMapperUtils.getMapper().readValue(persistentVolumeClaimRow.getPersistentVolume().getDetail().getVolumeSource(), new TypeReference<Map<String, Object>>() {});
                                if (MapUtils.getObject(tempVolumeSourceMap, "nfs", null) != null) {
                                    Map<String, String> targetVolumeSourceMap = (Map<String, String>)tempVolumeSourceMap.get("nfs");
                                    Map<String, String> sourceVolumeSourceMap = Maps.newHashMap();
                                    for (ClusterVolumeParamterVO paramterRow : clusterVolumeRow.getParameters()) {
                                        sourceVolumeSourceMap.put(paramterRow.getName(), paramterRow.getValue());
                                    }
                                    if (sourceVolumeSourceMap.equals(targetVolumeSourceMap)) {
                                        isExists = true;
                                    }
                                }
                            }
                        }
                    }

                    if (isExists) {
                        persistentVolumeClaimRow.setClusterVolume(clusterVolumeRow);
                        if (StringUtils.isBlank(persistentVolumeClaimRow.getPersistentVolumeType())) {
                            if (persistentVolumeClaimRow.getAccessModes().contains(AccessMode.RWO.getValue())) {
                                persistentVolumeClaimRow.setPersistentVolumeType(PersistentVolumeType.SINGLE.getCode());
                            } else {
                                persistentVolumeClaimRow.setPersistentVolumeType(PersistentVolumeType.SHARED.getCode());
                            }
                        }
                        break;
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(allPods)) {
//                    if (persistentVolumeClaimRow.getClusterVolume() != null && StringUtils.isNotBlank(persistentVolumeClaimRow.getClusterVolume().getStorageClassName())) {
//                        K8sStorageClassVO k8sStorageClass = k8sResourceService.getStorageClass(cluster, persistentVolumeClaimRow.getClusterVolume().getStorageClassName(), ContextHolder.exeContext());
//
//                        if(k8sStorageClass != null){
//                            clusterVolumeService.setStorageClassInClusterVolume(persistentVolumeClaimRow.getClusterVolume(), k8sStorageClass);
//                        }
//                    }
                // Mount된 owner 정보
                Map<String, String> ownerResourceNameMap = Maps.newHashMap();
                // deployment 여부
                boolean isDeployment;
                // cronJob 여부
                boolean isCronJob;
                // mount 정보
                List<ServerDetailParamForPVVO> serverParams = Lists.newArrayList();

                String workloadName = null;

                for (K8sPodVO k8sPodRow : allPods) {
                    isDeployment = false;
                    isCronJob = false;
                    if (CollectionUtils.isNotEmpty(k8sPodRow.getDetail().getOwnerReferences())) {
                        K8sOwnerReferenceVO ownerReference = k8sPodRow.getDetail().getOwnerReferences().get(0);

                        if (ownerResourceNameMap.containsKey(ownerReference.getName())) {
                            continue;
                        } else {
                            // workload명을 얻기위해 체크
                            // pod의 owner가 또 owner를 가질 경우 ( Deployment(ReplicaSe), CronJob(Job) )
                            if (K8sApiKindType.findKindTypeByValue(ownerReference.getKind()) == K8sApiKindType.REPLICA_SET) {
                                if (MapUtils.getObject(replicaSetMap, ownerReference.getName(), null) != null) {
                                    isDeployment = true;
                                }
                            }
                            if (K8sApiKindType.findKindTypeByValue(ownerReference.getKind()) == K8sApiKindType.JOB) {
                                if (MapUtils.getObject(jobByCronJobMap, ownerReference.getName(), null) != null) {
                                    isCronJob = true;
                                }
                            }
                        }

                        List<V1Volume> volumes = ServerUtils.getObjectsInWorkload(k8sPodRow.getPodDeployment(), "$.spec.volumes", new TypeRef<List<V1Volume>>() {},  true);
                        if (CollectionUtils.isNotEmpty(volumes)) {

                            Map<String, V1Volume> v1VolumeMap = volumes.stream()
                                    .filter(v -> (v.getPersistentVolumeClaim() != null && StringUtils.equals(persistentVolumeClaimRow.getName(), v.getPersistentVolumeClaim().getClaimName())))
                                    .collect(Collectors.toMap(V1Volume::getName, Function.identity()));

                            if (MapUtils.isNotEmpty(v1VolumeMap)) {
                                List<V1Container> allContainers = Lists.newArrayList();
                                List<V1Container> initContainers = ServerUtils.getObjectsInWorkload(k8sPodRow.getPodDeployment(), "$.spec.initContainers", new TypeRef<List<V1Container>>() {}, true);
                                List<V1Container> containers = ServerUtils.getObjectsInWorkload(k8sPodRow.getPodDeployment(), "$.spec.containers", new TypeRef<List<V1Container>>() {}, true);

                                if (CollectionUtils.isNotEmpty(initContainers)) {
                                    allContainers.addAll(initContainers);
                                }
                                if (CollectionUtils.isNotEmpty(containers)) {
                                    allContainers.addAll(containers);
                                }

                                if (CollectionUtils.isNotEmpty(allContainers)) {
                                    boolean isMount = false;
                                    List<ContainerVolumeVO> volumeMounts = Lists.newArrayList();
                                    for (V1Container containerRow : allContainers) {
                                        if (CollectionUtils.isNotEmpty(containerRow.getVolumeMounts())) {
                                            for (V1VolumeMount volumeMountRow : containerRow.getVolumeMounts()) {
                                                if (v1VolumeMap.containsKey(volumeMountRow.getName())) {

                                                    ContainerVolumeVO volumeMount = new ContainerVolumeVO();
                                                    volumeMount.setContainerName(containerRow.getName());
                                                    volumeMount.setVolumeName(volumeMountRow.getName());
                                                    volumeMount.setContainerPath(volumeMountRow.getMountPath());
                                                    volumeMount.setSubPath(volumeMountRow.getSubPath());
                                                    volumeMount.setReadOnly(volumeMountRow.getReadOnly());

                                                    volumeMounts.add(volumeMount);

                                                    isMount = true;
                                                }
                                            }
                                        }
                                    }

                                    // 같은 controller의 pod 중복 체크 방지
                                    if (isMount) {
                                        workloadName = null;
                                        if (isDeployment) {
                                            if (CollectionUtils.isNotEmpty(replicaSetMap.get(ownerReference.getName()).getDetail().getOwnerReferences())) {
                                                // 중복 체크용 셋팅
                                                K8sOwnerReferenceVO ownerReferenceTemp = replicaSetMap.get(ownerReference.getName()).getDetail().getOwnerReferences().get(0);
                                                ownerResourceNameMap.put(ownerReferenceTemp.getName(), ownerReferenceTemp.getKind());

                                                workloadName = ownerReferenceTemp.getName();

                                            }
                                        } else if (isCronJob) {
                                            if (CollectionUtils.isNotEmpty(jobByCronJobMap.get(ownerReference.getName()).getDetail().getOwnerReferences())) {
                                                K8sOwnerReferenceVO ownerReferenceTemp = jobByCronJobMap.get(ownerReference.getName()).getDetail().getOwnerReferences().get(0);
                                                ownerResourceNameMap.put(ownerReferenceTemp.getName(), ownerReferenceTemp.getKind());

                                                workloadName = ownerReferenceTemp.getName();
                                            }
                                        } else {
                                            ownerResourceNameMap.put(ownerReference.getName(), ownerReference.getKind());

                                            workloadName = ownerReference.getName();
                                        }

                                        // serverParam 셋팅
                                        ServerDetailParamForPVVO serverParam = new ServerDetailParamForPVVO();
                                        serverParam.setNamespaceName(k8sPodRow.getNamespace());
                                        serverParam.setServerName(workloadName);
                                        serverParam.setContainerVolumes(volumeMounts);
                                        serverParams.add(serverParam);
                                    }
                                }
                            }
                        }
                    }

                }

                persistentVolumeClaimRow.setServerParams(serverParams);
            }

        }
    }

    public void deletePersistentVolumeClaimsInCluster(Integer servicemapSeq, String persistentVolumeClaimName, boolean checkMount, ExecutingContextVO context) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        this.deletePersistentVolumeClaimsInCluster(cluster, cluster.getNamespaceName(), persistentVolumeClaimName, checkMount, context);
    }

    public void deletePersistentVolumeClaimsInCluster(ClusterVO cluster, String namespace, String persistentVolumeClaimName, boolean checkMount, ExecutingContextVO context) throws Exception {

        K8sPersistentVolumeClaimVO k8sPersistentVolumeClaim = this.getPersistentVolumeClaim(cluster, namespace, persistentVolumeClaimName, context);

        if(k8sPersistentVolumeClaim != null){
            if(checkMount && CollectionUtils.isNotEmpty(k8sPersistentVolumeClaim.getServerParams())){
                throw new CocktailException("PVC is using mount!!", ExceptionType.K8sVolumeClaimIsUsingMount);
            }else{
//                Integer volumeSeq = 0;
//                // statefulSet volumeClaimTemplate 으로 생성한 PVC일 경우 CUSTOM_VOLUME_STORAGE 라벨이 없으므로 storageClass나 스토리지 파라미터(NFS Named)로 체크
//                if (k8sPersistentVolumeClaim.getDetail().getLabels().get(KubeConstants.CUSTOM_VOLUME_STORAGE) != null) {
//                    volumeSeq = Integer.valueOf(k8sPersistentVolumeClaim.getDetail().getLabels().get(KubeConstants.CUSTOM_VOLUME_STORAGE));
//                }
//
//                ClusterVolumeVO clusterVolume = null;
//                if (volumeSeq != null && volumeSeq.intValue() != 0) {
//                    clusterVolume = clusterVolumeService.getClusterVolume(volumeSeq);
//                } else {
//                    List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, null, cluster.getClusterSeq(), null, null, false, false);
//
//                    if (StringUtils.isNotBlank(k8sPersistentVolumeClaim.getStorageClassName())) {
//                        if (CollectionUtils.isNotEmpty(clusterVolumes)) {
//                            for(ClusterVolumeVO clusterVolumeRow : clusterVolumes) {
//                                if (StringUtils.equals(k8sPersistentVolumeClaim.getStorageClassName(), clusterVolumeRow.getName())) {
//                                    clusterVolume = clusterVolumeRow;
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }

                VolumeType volumeType = VolumeType.PERSISTENT_VOLUME_STATIC;
                Map<String, String> targetParams = Maps.newHashMap();
                if (StringUtils.isNotBlank(k8sPersistentVolumeClaim.getStorageClassName())) {
                    volumeType = VolumeType.PERSISTENT_VOLUME;
                } else {
                    if (k8sPersistentVolumeClaim.getPersistentVolume() != null) {
                        V1PersistentVolume v1PersistentVolume = ServerUtils.unmarshalYaml(k8sPersistentVolumeClaim.getPersistentVolume().getDeploymentYaml());
                        if (v1PersistentVolume != null && v1PersistentVolume.getSpec().getNfs() != null) {
                            targetParams.put(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_SERVER, v1PersistentVolume.getSpec().getNfs().getServer());
                            targetParams.put(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_PATH, v1PersistentVolume.getSpec().getNfs().getPath());
                        }
                    }
                }

                List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, null, cluster.getClusterSeq(), null, volumeType.getCode(), false, false);
                ClusterVolumeVO clusterVolume = null;

                if (CollectionUtils.isNotEmpty(clusterVolumes)) {
                    for(ClusterVolumeVO clusterVolumeRow : clusterVolumes) {
                        if (volumeType == VolumeType.PERSISTENT_VOLUME) {
                            if (StringUtils.equals(k8sPersistentVolumeClaim.getStorageClassName(), clusterVolumeRow.getName())) {
                                clusterVolume = clusterVolumeRow;
                                break;
                            }
                        } else {
                            if (CollectionUtils.isNotEmpty(clusterVolumeRow.getParameters())) {
                                Map<String, String> sourceParams = Maps.newHashMap();
                                for (ClusterVolumeParamterVO paramterVO : clusterVolumeRow.getParameters()) {
                                    sourceParams.put(paramterVO.getName(), paramterVO.getValue());
                                }

                                if (sourceParams.equals(targetParams)) {
                                    clusterVolume = clusterVolumeRow;
                                    break;
                                }
                            }
                        }
                    }
                }

                this.deletePersistentVolumeClaim(cluster, namespace, persistentVolumeClaimName, context);

                if(clusterVolume != null && clusterVolume.getType() == VolumeType.PERSISTENT_VOLUME_STATIC){
                    this.deletePersistentVolume(cluster, k8sPersistentVolumeClaim.getPersistentVolume().getName(), context);
                }
            }
        }
    }

    public boolean checkDuplicatePersistentVolume(Integer servicemapSeq, String persistentVolumeClaimName, ExecutingContextVO context) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        return this.checkDuplicatePersistentVolume(cluster, cluster.getNamespaceName(), persistentVolumeClaimName, context);
    }

    public boolean checkDuplicatePersistentVolume(ClusterVO cluster, String namespace, String persistentVolumeClaimName, ExecutingContextVO context) throws Exception {


        boolean isDuplicated = false;
        V1PersistentVolumeClaim v1PersistentVolumeClaim = k8sWorker.getPersistentVolumeClaimV1WithName(cluster, namespace, persistentVolumeClaimName);
        if(v1PersistentVolumeClaim != null){
            isDuplicated = true;
        }

        return isDuplicated;
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
