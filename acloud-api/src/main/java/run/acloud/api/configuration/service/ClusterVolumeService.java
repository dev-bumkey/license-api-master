package run.acloud.api.configuration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ClusterVolumeCapacityVO;
import run.acloud.api.configuration.vo.ClusterVolumeParamterVO;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.enums.*;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiVerType;
import run.acloud.api.resource.service.ConfigMapService;
import run.acloud.api.resource.service.PersistentVolumeService;
import run.acloud.api.resource.service.StorageClassService;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.ConfigMapGuiVO;
import run.acloud.api.resource.vo.K8sPersistentVolumeClaimVO;
import run.acloud.api.resource.vo.K8sPersistentVolumeVO;
import run.acloud.api.resource.vo.K8sStorageClassVO;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 5.
 */
@Slf4j
@Service
public class ClusterVolumeService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ServicemapService servicemapService;

    @Autowired
    private ConfigMapService configMapService;

    @Autowired
    private PersistentVolumeService persistentVolumeService;

    @Autowired
    private StorageClassService storageClassService;

//    @Autowired
//    private WorkloadResourceService workloadResourceService;
    @Transactional(transactionManager = "transactionManager")
    public ClusterVolumeVO addStorageVolume(ClusterVolumeVO clusterVolume, boolean makeStorageClass) throws Exception {

        this.checkRequest(clusterVolume, true);

        if (clusterVolume.getType() == VolumeType.PERSISTENT_VOLUME) {

            EnumSet<VolumePlugIn> provisionerPlugins = EnumSet.of(VolumePlugIn.AWSEBS, VolumePlugIn.GCE, VolumePlugIn.AZUREDISK, VolumePlugIn.AZUREFILE, VolumePlugIn.VSPHEREVOLUME
                                                        , VolumePlugIn.AWSEBS_CSI, VolumePlugIn.AWSEFS_CSI, VolumePlugIn.GCE_CSI, VolumePlugIn.AZUREDISK_CSI
                                                        , VolumePlugIn.AZUREFILE_CSI, VolumePlugIn.VSPHEREVOLUME_CSI, VolumePlugIn.NCPBLOCK_CSI, VolumePlugIn.NCPNAS_CSI);

            if(provisionerPlugins.contains(clusterVolume.getPlugin())){
                clusterVolume.setTotalCapacity(0);
                clusterVolume.setProvisionerName(clusterVolume.getPlugin().getProvisionerName());
            }
            else{
                if(clusterVolume.getPlugin().haveTotalCapacity()){
                    if(makeStorageClass){
                        if (clusterVolume.getPlugin() == VolumePlugIn.NFS_CSI) {
                            if (StringUtils.isBlank(clusterVolume.getProvisionerName())) {
                                clusterVolume.setProvisionerName(clusterVolume.getPlugin().getProvisionerName());
                            }
                        }
                    }
                }
            }
            List<ClusterVolumeVO> volumes = this.getStorageVolumes(null, null, clusterVolume.getClusterSeq(), clusterVolume.getStorageType().getCode(), VolumeType.PERSISTENT_VOLUME.getCode(), false, false);
            this.checkDynamicVolumeDuplication(clusterVolume, volumes,false);

        } else {
            // server valid regex
            // ^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)+([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$
            List<ClusterVolumeVO> volumes = this.getStorageVolumes(null, null, clusterVolume.getClusterSeq(), clusterVolume.getStorageType().getCode(), VolumeType.PERSISTENT_VOLUME_STATIC.getCode(), false, false);
            for (ClusterVolumeVO v : volumes) {
                if (v.getPhase() != VolumePhase.AVAILABLE) {
                    continue;
                }

                if (clusterVolume.getName().equals(v.getName())) {
                    throw new CocktailException("Shared Cluster Volume already exists",
                            ExceptionType.SharedClusterVolumeAlreadyExists);
                }

                boolean path = false, server = false;
                for (ClusterVolumeParamterVO p1 : v.getParameters()) {
                    if (p1.getName().equals("server")) {
                        for (ClusterVolumeParamterVO p2 : clusterVolume.getParameters()) {
                            if (p2.getName().equals("server")) {
                                server = p1.getValue().equals(p2.getValue());
                                break;
                            }
                        }
                    } else if (p1.getName().equals("path")) {
                        for (ClusterVolumeParamterVO p2 : clusterVolume.getParameters()) {
                            if (p2.getName().equals("path")) {
                                Path path1 = Paths.get(p1.getValue());
                                Path path2 = Paths.get(p2.getValue());
                                path = path1.compareTo(path2) == 0;
                                break;
                            }
                        }
                    }
                }

                if (server && path) {
                    throw new CocktailException("Shared Cluster Volume already exists",
                            ExceptionType.SharedClusterVolumeAlreadyExists);
                }
            }
        }

        clusterVolume.setReadWriteOnceYn("Y");
        VolumePlugIn plugIn = clusterVolume.getPlugin();
        clusterVolume.setReadOnlyManyYn(plugIn.canReadOnlyMany() ? "Y" : "N");
        clusterVolume.setReadWriteManyYn(plugIn.canReadWriteMany() ? "Y" : "N");
        clusterVolume.setPhase(VolumePhase.AVAILABLE);
        clusterVolume.setUseYn("Y");

        if (clusterVolume.getType() == VolumeType.PERSISTENT_VOLUME) {
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            ClusterVO cluster = clusterDao.getCluster(clusterVolume.getClusterSeq());

            if(makeStorageClass){

                clusterVolume.setCluster(cluster);

                storageClassService.createStorageClass(cluster, clusterVolume);
            } else {
                // 기 생성된 storageClass를 등록할 시에는 다른 storageClass를 is-default : false로 변경
                if (BooleanUtils.toBoolean(clusterVolume.getBaseStorageYn())) {
                    storageClassService.patchDefaultStorageClass(cluster, clusterVolume);
                }
            }
        } else {
            // create Static Storage ConfigMap
            ConfigMapGuiVO configMap = new ConfigMapGuiVO();
            configMap.setNamespace(KubeConstants.COCKTAIL_ADDON_NAMESPACE);
            configMap.setName(ResourceUtil.makeStaticStorageConfigMapName(clusterVolume.getName()));
            configMap.setLabels(K8sSpecFactory.buildStorageClassLabel(clusterVolume));
            configMap.setAnnotations(K8sSpecFactory.buildStorageClassAnno(clusterVolume));
            configMap.setData(Maps.newHashMap());
            configMap.getData().put(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_RECLAIM_POLICY, clusterVolume.getReclaimPolicy().getCode());
            if (CollectionUtils.isNotEmpty(clusterVolume.getParameters())) {
                for (ClusterVolumeParamterVO paramterRow : clusterVolume.getParameters()) {
                    if (StringUtils.equalsIgnoreCase(paramterRow.getName(), KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_SERVER)) {
                        configMap.getData().put(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_SERVER, paramterRow.getValue());
                    } else if (StringUtils.equalsIgnoreCase(paramterRow.getName(), KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_PATH)) {
                        configMap.getData().put(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_PATH, paramterRow.getValue());
                    }
                }
            }
            configMapService.createConfigMap(clusterVolume.getClusterSeq(), KubeConstants.COCKTAIL_ADDON_NAMESPACE, configMap, null);
        }

        return clusterVolume;
    }

    private void checkDynamicVolumeDuplication(ClusterVolumeVO clusterVolume, List<ClusterVolumeVO> volumes, boolean isUpdate) throws Exception {

        if(CollectionUtils.isNotEmpty(volumes)){
            for (ClusterVolumeVO v : volumes) {
                if (isUpdate && v.getName().equals(clusterVolume.getName())) { // 수정 시 자가 자신과의 비교를 제외
                    continue;
                }
                // 이름이 다르다면 똑같은 스토리지도 만들 수 있도록 주석 ( hjchoi. 20201020 )
//                if (v.getName().equals(clusterVolume.getName()) ||
//                        (StringUtils.equals(v.getProvisionerName(), clusterVolume.getProvisionerName()) &&
//                                v.getPlugin() == clusterVolume.getPlugin() &&
//                                v.getReclaimPolicy() == clusterVolume.getReclaimPolicy() &&
//                                CollectionUtils.isEqualCollection(Optional.ofNullable(v.getParameters()).orElseGet(() ->Lists.newArrayList()), Optional.ofNullable(clusterVolume.getParameters()).orElseGet(() ->Lists.newArrayList())))) {
//                    throw new CocktailException(String.format("Volume[%s] in cluster '%d' with storageClass[%s]-provisioner[%s] already exists",
//                            v.getName(), v.getClusterSeq(), v.getName(), v.getProvisionerName()), ExceptionType.DynamicClusterVolumeAlreadyExists);
//                }

                if (v.getName().equals(clusterVolume.getName())) {
                    throw new CocktailException(String.format("Volume[%s] in cluster '%d' with storageClass[%s]-provisioner[%s] already exists",
                            v.getName(), v.getClusterSeq(), v.getName(), v.getProvisionerName()), ExceptionType.DynamicClusterVolumeAlreadyExists);
                }
            }
        }
    }

    public void checkRequest(ClusterVolumeVO volume, boolean isAdd) throws Exception {
        if (isAdd && volume.getClusterSeq() == null) {
            throw new CocktailException("Cluster seq is null", ExceptionType.ClusterSequenceEmpty);
        }

        if (StringUtils.isBlank(volume.getName()) || volume.getName().length() > 50) {
            throw new CocktailException("Cluster volume name is empty", ExceptionType.ClusterVolumeNameInvalid);
        } else {
            if (!volume.getName().matches(KubeConstants.RULE_SERVICE_NAME)) {
                throw new CocktailException("Cluster volume name is invalid", ExceptionType.ClusterVolumeNameInvalid);
            }
        }

//        if(volume.getStorageType() == null){
//            throw new CocktailException("StorageType is null", ExceptionType.InvalidClusterStorageType);
//        }else{
//            if(volume.getStorageType() != StorageType.SHARED && volume.getStorageType() != StorageType.SINGLE){
//                throw new CocktailException("StorageType is invalid", ExceptionType.InvalidClusterStorageType);
//            }
//        }

        if(volume.getPlugin() == null) {
            throw new CocktailException("Volume plugin is null", ExceptionType.VolumePluginInvalid);
        }else{
            if(EnumSet.of(VolumePlugIn.NFSDYNAMIC, VolumePlugIn.AWSEFS).contains(volume.getPlugin())){
                if(StringUtils.isBlank(volume.getProvisionerName())){
                    throw new CocktailException("Provisioner name is null", ExceptionType.ProvisionerNameInvalid);
                }
            }

            if(volume.getPlugin().canBindingMode()){
                if(volume.getVolumeBindingMode() == null){
                    throw new CocktailException("VolumeBindingMode is null", ExceptionType.InvalidParameter);
                }else {
                    IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                    ClusterVO cluster = clusterDao.getCluster(volume.getClusterSeq());

                    this.k8sVolumeBindingModeSupported(cluster, volume);
                }
            }else{
                volume.setVolumeBindingMode(VolumeBindingMode.IMMEDIATE);
            }
        }

        if(volume.getType() == null){
            throw new CocktailException("Type is null", ExceptionType.InvalidClusterVolumeType);
        }else{
            if(volume.getType() != VolumeType.PERSISTENT_VOLUME && volume.getType() != VolumeType.PERSISTENT_VOLUME_STATIC){
                throw new CocktailException("Type is invalid", ExceptionType.InvalidClusterVolumeType);
            }else{
//                if(volume.getType() == VolumeType.PERSISTENT_VOLUME){
//                    if(StringUtils.isBlank(volume.getStorageClassName()) || !volume.getStorageClassName().matches(KubeConstants.RULE_RESOURCE_NAME)){
//                        throw new CocktailException("Storage class name is null or invalid", ExceptionType.StorageClassNameInvalid);
//                    }
//                }

                if (volume.getType() == VolumeType.PERSISTENT_VOLUME_STATIC) {
                    if (CollectionUtils.isEmpty(volume.getParameters())) {
                        throw new CocktailException("Static Volume needs parameter(s)", ExceptionType.VolumeParameterNotExists);
                    }
                    if (isAdd) {
                        ConfigMapGuiVO existConfigMap = configMapService.getConfigMap(volume.getClusterSeq(), KubeConstants.COCKTAIL_ADDON_NAMESPACE, ResourceUtil.makeStaticStorageConfigMapName(volume.getName()));
                        if(existConfigMap != null){
                            throw new CocktailException("Static Storage ConfigMap already exists!!", ExceptionType.DynamicClusterVolumeAlreadyExists);
                        }
                    }
                }
            }
        }

        if (volume.getReclaimPolicy() == null) {
            throw new CocktailException("Reclaim policy is null", ExceptionType.ReclaimPolicyEmpty);
        }

    }

    private boolean k8sVolumeBindingModeSupported(ClusterVO cluster, ClusterVolumeVO clusterVolume) throws Exception{
        if(StringUtils.isNotBlank(cluster.getK8sVersion())) {

            if(clusterVolume != null && clusterVolume.getPlugin() != null && clusterVolume.getPlugin().canBindingMode()){
                EnumSet<K8sApiVerType> supportedVersionEnumSet = clusterVolume.getVolumeBindingMode().getSupportedVersion();
                List<String> apiVersionList = supportedVersionEnumSet.stream().map(K8sApiVerType::getVersion).collect(Collectors.toList());
                if (ResourceUtil.getK8sSupported(cluster.getK8sVersion(), apiVersionList)) {
                    return true;
                }else{
                    throw new CocktailException(String.format("Cube Cluster version[%s] is not support.", cluster.getK8sVersion()), ExceptionType.K8sNotSupported);
                }
            }
        }else{
            throw new CocktailException("k8s version is null.", ExceptionType.InternalError);
        }

        return false;
    }

    /**
     * 조건에 맞는 storageClass, PersistentVolumeClaim, PersistentVolume 조회하여 clusterSeq를 키로 Map에 셋팅
     *
     * @param accountSeq
     * @param serviceSeq
     * @param clusterSeq
     * @param storageType
     * @param type
     * @param useCapacity
     * @param useRequest
     * @param k8sStorageClassesMap
     * @param k8sPersistentVolumeClaimsMap
     * @param k8sPersistentVolumesMap
     * @param context
     * @throws Exception
     */
    public void getStorageClassesWithPvc(
            Integer accountSeq, Integer serviceSeq, Integer clusterSeq,
            String storageType, String type, boolean useCapacity, boolean useRequest,
            Map<Integer, List<K8sStorageClassVO>> k8sStorageClassesMap,
            Map<Integer, List<ConfigMapGuiVO>> k8sStaticStoragesMap,
            Map<Integer, List<K8sPersistentVolumeClaimVO>> k8sPersistentVolumeClaimsMap,
            Map<Integer, Map<String, K8sPersistentVolumeVO>> k8sPersistentVolumesMap,
            ExecutingContextVO context
    ) throws Exception{
        this.getStorageClassesWithPvc(
                accountSeq, serviceSeq, clusterSeq, null,
                storageType, type, useCapacity, useRequest,
                k8sStorageClassesMap, k8sStaticStoragesMap, k8sPersistentVolumeClaimsMap, k8sPersistentVolumesMap,
                context
        );
    }

    /**
     * 조건에 맞는 storageClass, PersistentVolumeClaim, PersistentVolume 조회하여 clusterSeq를 키로 Map에 셋팅
     *
     * @param accountSeq
     * @param serviceSeq
     * @param clusterSeq
     * @param storageType
     * @param type
     * @param useCapacity
     * @param useRequest
     * @param k8sStorageClassesMap
     * @param k8sPersistentVolumeClaimsMap
     * @param k8sPersistentVolumesMap
     * @param context
     * @throws Exception
     */
    public void getStorageClassesWithPvc(
            Integer accountSeq, Integer serviceSeq, Integer clusterSeq, List<ClusterVO> clusters,
            String storageType, String type, boolean useCapacity, boolean useRequest,
            Map<Integer, List<K8sStorageClassVO>> k8sStorageClassesMap,
            Map<Integer, List<ConfigMapGuiVO>> k8sStaticStoragesMap,
            Map<Integer, List<K8sPersistentVolumeClaimVO>> k8sPersistentVolumeClaimsMap,
            Map<Integer, Map<String, K8sPersistentVolumeVO>> k8sPersistentVolumesMap,
            ExecutingContextVO context
    ) throws Exception{
        HttpServletRequest request = Utils.getCurrentRequest();
        try {
            String labelSelector = null;
            String storageTypeLable = null;
            String pluginTypeLable = null;
            if (StringUtils.isNotBlank(storageType)) {
                storageTypeLable = String.format("%s=%s", KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE, storageType);
                labelSelector = storageTypeLable;
            }
            if (StringUtils.isNotBlank(type)) {
                pluginTypeLable = String.format("%s=%s", KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_VOLUME_TYPE, type);
                if (StringUtils.isNotBlank(labelSelector)) {
                    labelSelector += ",";
                } else {
                    labelSelector = "";
                }

                labelSelector += pluginTypeLable;
            }

            if(clusterSeq != null){
                if(clusterStateService.isClusterRunning(clusterSeq)){
                    IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                    ClusterVO cluster = null;
                    if (CollectionUtils.isNotEmpty(clusters)) {
                        cluster = clusters.get(0);
                    } else {
                        cluster = clusterDao.getCluster(clusterSeq);
                    }

                    // StorageClass
                    if (StringUtils.isBlank(type) || (StringUtils.isNotBlank(type) && VolumeType.valueOf(type) == VolumeType.PERSISTENT_VOLUME)) {
                        List<K8sStorageClassVO> k8sStorageClasses = storageClassService.getStorageClasses(cluster, null, labelSelector, context);
                        if(CollectionUtils.isNotEmpty(k8sStorageClasses)){
                            k8sStorageClassesMap.put(clusterSeq, k8sStorageClasses);
                        }
                    }

                    // Static Storage ConfigMap 조회
                    if (StringUtils.isBlank(type) || (StringUtils.isNotBlank(type) && VolumeType.valueOf(type) == VolumeType.PERSISTENT_VOLUME_STATIC)) {
                        List<ConfigMapGuiVO> k8sConfigMaps = configMapService.getConfigMaps(cluster, KubeConstants.COCKTAIL_ADDON_NAMESPACE, null, String.format("%s=%s", KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_VOLUME_TYPE, VolumeType.PERSISTENT_VOLUME_STATIC.getCode()));
                        if (CollectionUtils.isNotEmpty(k8sConfigMaps)) {
                            k8sStaticStoragesMap.put(clusterSeq, k8sConfigMaps);
                        }
                    }

                    if (useCapacity || useRequest){
                        // PersistentVolumeClaim
                        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(cluster, "", null, null, context);
                        if(CollectionUtils.isNotEmpty(persistentVolumeClaims)){
                            if (serviceSeq != null) {
                                Set<String> namespaces = servicemapService.getNamespaceNamesByServiceInCluster(serviceSeq, clusterSeq);
                                List<K8sPersistentVolumeClaimVO> persistentVolumeClaimsByNamespace = new ArrayList<>();
                                for (K8sPersistentVolumeClaimVO k8sPersistentVolumeClaimRow : persistentVolumeClaims) {
                                    if (CollectionUtils.isNotEmpty(namespaces) && namespaces.contains(k8sPersistentVolumeClaimRow.getNamespace())) {
                                        persistentVolumeClaimsByNamespace.add(k8sPersistentVolumeClaimRow);
                                    }
                                }
                                k8sPersistentVolumeClaimsMap.put(clusterSeq, persistentVolumeClaimsByNamespace);
                            } else {
                                k8sPersistentVolumeClaimsMap.put(clusterSeq, persistentVolumeClaims);
                            }
                        }
                        // PersistentVolume
                        Map<String, K8sPersistentVolumeVO> persistentVolumeMap = persistentVolumeService.convertPersistentVolumeDataMap(cluster, null, null, null);
                        if(MapUtils.isNotEmpty(persistentVolumeMap)){
                            k8sPersistentVolumesMap.put(clusterSeq, persistentVolumeMap);
                        }
                    }
                }
            }else {
                if(accountSeq != null || serviceSeq != null){
                    if (CollectionUtils.isEmpty(clusters)) {
                        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                        clusters = clusterDao.getClusters(accountSeq, serviceSeq, null, null, null, "Y");
                    }

                    if(CollectionUtils.isNotEmpty(clusters)){
                        for (ClusterVO clusterRow : clusters){
                            if(clusterStateService.isClusterRunning(clusterRow)){
                                // StorageClass
                                if (StringUtils.isBlank(type) || (StringUtils.isNotBlank(type) && VolumeType.valueOf(type) == VolumeType.PERSISTENT_VOLUME)) {
                                    List<K8sStorageClassVO> k8sStorageClasses = storageClassService.getStorageClasses(clusterRow, null, labelSelector, context);
                                    if(CollectionUtils.isNotEmpty(k8sStorageClasses)){
                                        k8sStorageClassesMap.put(clusterRow.getClusterSeq(), k8sStorageClasses);
                                    }
                                }

                                // Static Storage ConfigMap 조회
                                if (StringUtils.isBlank(type) || (StringUtils.isNotBlank(type) && VolumeType.valueOf(type) == VolumeType.PERSISTENT_VOLUME_STATIC)) {
                                    List<ConfigMapGuiVO> k8sConfigMaps = configMapService.getConfigMaps(clusterRow, KubeConstants.COCKTAIL_ADDON_NAMESPACE, null, String.format("%s=%s", KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_VOLUME_TYPE, VolumeType.PERSISTENT_VOLUME_STATIC.getCode()));
                                    if (CollectionUtils.isNotEmpty(k8sConfigMaps)) {
                                        k8sStaticStoragesMap.put(clusterRow.getClusterSeq(), k8sConfigMaps);
                                    }
                                }

                                if (useCapacity || useRequest){
                                    // PersistentVolumeClaim
                                    List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(clusterRow, "", null, null, context);
                                    if(CollectionUtils.isNotEmpty(persistentVolumeClaims)){
                                        k8sPersistentVolumeClaimsMap.put(clusterRow.getClusterSeq(), persistentVolumeClaims);
                                    }
                                    // PersistentVolume
                                    Map<String, K8sPersistentVolumeVO> persistentVolumeMap = persistentVolumeService.convertPersistentVolumeDataMap(clusterRow, null, null, null);
                                    if(MapUtils.isNotEmpty(persistentVolumeMap)){
                                        k8sPersistentVolumesMap.put(clusterRow.getClusterSeq(), persistentVolumeMap);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CocktailException ce = new CocktailException("getClusterVolumes - fail to storage class info!!", e, ExceptionType.K8sCocktailCloudInquireFail);
            log.error(ExceptionMessageUtils.setCommonResult(request, null, ce, false), e);
        }
    }

    private List<ClusterVolumeVO> genStorageVolumeData(ClusterVO cluster,
                                                       Map<Integer, List<K8sStorageClassVO>> k8sStorageClassesMap,
                                                       Map<Integer, List<ConfigMapGuiVO>> k8sStaticStoragesMap,
                                                       Map<Integer, List<K8sPersistentVolumeClaimVO>> k8sPersistentVolumeClaimsMap,
                                                       Map<Integer, Map<String, K8sPersistentVolumeVO>> k8sPersistentVolumesMap,
                                                       boolean useCapacity, boolean useRequest,
                                                       List<ClusterVolumeVO> storageVolumes,
                                                       ExecutingContextVO context
    ) throws Exception {
        if (storageVolumes == null) {
            storageVolumes = Lists.newArrayList();
        }

        // Dynamic Storage (StorageClass)
        if (MapUtils.isNotEmpty(k8sStorageClassesMap) && MapUtils.getObject(k8sStorageClassesMap, cluster.getClusterSeq(), null) != null) {
            for (K8sStorageClassVO k8sStorageClassRow : k8sStorageClassesMap.get(cluster.getClusterSeq())) {

                ClusterVolumeVO storageVolume = new ClusterVolumeVO();
                storageVolume.setClusterSeq(cluster.getClusterSeq());
                storageVolume.setName(k8sStorageClassRow.getName());
                storageVolume.setLabels(k8sStorageClassRow.getDetail().getLabels());
                storageVolume.setAnnotations(k8sStorageClassRow.getDetail().getAnnotations());
                storageVolume.setType(VolumeType.PERSISTENT_VOLUME);
                storageVolume.setReclaimPolicy(ReclaimPolicy.getReclaimPolicyOfValue(k8sStorageClassRow.getDetail().getReclaimPolicy()));
                storageVolume.setProvisionerName(k8sStorageClassRow.getProvisioner());
                storageVolume.setVolumeBindingMode(VolumeBindingMode.getCodeByValue(k8sStorageClassRow.getDetail().getVolumeBindingMode()));
                storageVolume.setPhase(VolumePhase.AVAILABLE);
                storageVolume.setStorageClass(k8sStorageClassRow);

                // provider 별로 provisioner를 확인하여 plugin, storageType 셋팅
                switch (cluster.getProviderAccount().getProviderCode()) {
                    case AWS:
                        // EBS
                        if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.AWS_EBS)) {
                            storageVolume.setPlugin(VolumePlugIn.AWSEBS);
                            storageVolume.setStorageType(VolumePlugIn.AWSEBS.getStorageType());
                        }
                        // EBS CSI
                        else if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.AWS_EBS_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.AWSEBS_CSI);
                            storageVolume.setStorageType(VolumePlugIn.AWSEBS_CSI.getStorageType());
                        }
                        // EFS CSI
                        else if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.AWS_EFS_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.AWSEFS_CSI);
                            storageVolume.setStorageType(VolumePlugIn.AWSEFS_CSI.getStorageType());
                        }
                        break;
                    case GCP:
                        // GCE PD
                        if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.GCE_PD)) {
                            storageVolume.setPlugin(VolumePlugIn.GCE);
                            storageVolume.setStorageType(VolumePlugIn.GCE.getStorageType());
                        }
                        // GCE PD CSI
                        else if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.GCE_PD_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.GCE_CSI);
                            storageVolume.setStorageType(VolumePlugIn.GCE_CSI.getStorageType());
                        }
                        break;
                    case AZR:
                        // Azure Disk
                        if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.AZURE_DISK)) {
                            storageVolume.setPlugin(VolumePlugIn.AZUREDISK);
                            storageVolume.setStorageType(VolumePlugIn.AZUREDISK.getStorageType());
                        }
                        // Azure File
                        else if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.AZURE_FILE)) {
                            storageVolume.setPlugin(VolumePlugIn.AZUREFILE);
                            storageVolume.setStorageType(VolumePlugIn.AZUREFILE.getStorageType());
                        }
                        // Azure Disk CSI
                        else if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.AZURE_DISK_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.AZUREDISK_CSI);
                            storageVolume.setStorageType(VolumePlugIn.AZUREDISK_CSI.getStorageType());
                        }
                        // Azure File CSI
                        else if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.AZURE_FILE_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.AZUREFILE_CSI);
                            storageVolume.setStorageType(VolumePlugIn.AZUREFILE_CSI.getStorageType());
                        }
                        break;
                    case VMW:
                        // vSphere volume CSI
                        if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.VSPHERE_VOLUME_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.VSPHEREVOLUME_CSI);
                            storageVolume.setStorageType(VolumePlugIn.VSPHEREVOLUME_CSI.getStorageType());
                        }
                        break;
                    case NCP:
                        // NCP Block Storage CSI
                        if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.NCP_BLOCK_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.NCPBLOCK_CSI);
                            storageVolume.setStorageType(VolumePlugIn.NCPBLOCK_CSI.getStorageType());
                        }
                        // NCP NAS Volume CSI
                        else if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.NCP_NAS_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.NCPNAS_CSI);
                            storageVolume.setStorageType(VolumePlugIn.NCPNAS_CSI.getStorageType());
                        }
                        break;
                }

                // plugIn
                if (storageVolume.getPlugin() == null) {
                    if (MapUtils.getObject(k8sStorageClassRow.getDetail().getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_PROVISIONER_TYPE, null) != null) {
                        storageVolume.setPlugin(VolumePlugIn.valueOf(k8sStorageClassRow.getDetail().getLabels().get(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_PROVISIONER_TYPE)));
                    } else {
                        if (StringUtils.equals(k8sStorageClassRow.getProvisioner(), VolumePlugIn.ProvisionerNames.NFS_CSI)) {
                            storageVolume.setPlugin(VolumePlugIn.NFS_CSI);
                            storageVolume.setStorageType(VolumePlugIn.NFS_CSI.getStorageType());
                        } else {
                            // default로 NFS로 셋팅
                            storageVolume.setPlugin(VolumePlugIn.NFSDYNAMIC);
                        }
                    }
                }
                // storageType
                if (storageVolume.getStorageType() == null) {
                    if (MapUtils.getObject(k8sStorageClassRow.getDetail().getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE, null) != null) {
                        storageVolume.setStorageType(StorageType.valueOf(k8sStorageClassRow.getDetail().getLabels().get(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE)));
                    } else {
                        storageVolume.setStorageType(VolumePlugIn.NFSDYNAMIC.getStorageType());
                    }
                }
                // Total Capacity
                if (storageVolume.getPlugin().haveTotalCapacity()) {
                    if (MapUtils.getObject(k8sStorageClassRow.getDetail().getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_TOTAL_CAPACITY, null) != null) {
                        storageVolume.setTotalCapacity(Integer.parseInt(k8sStorageClassRow.getDetail().getLabels().get(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_TOTAL_CAPACITY)));
                    } else {
                        // defalut로 100GB로 셋팅
                        storageVolume.setTotalCapacity(1024);
                    }
                }

                if (MapUtils.isNotEmpty(k8sStorageClassRow.getDetail().getAnnotations())) {
                    // default-storage 셋팅
                    storageVolume.setBaseStorageYn(BooleanUtils.toString(storageClassService.isDefaultStorageClass(k8sStorageClassRow.getDetail().getAnnotations()), "Y", "N", "N"));
                    // description
                    storageVolume.setDescription(new String(Base64Utils.decodeFromString(MapUtils.getString(k8sStorageClassRow.getDetail().getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "")), "UTF-8"));
                }

                // Access Modes
                storageVolume.setReadWriteOnceYn("Y");
                storageVolume.setReadOnlyManyYn(storageVolume.getPlugin().canReadOnlyMany() ? "Y" : "N");
                storageVolume.setReadWriteManyYn(storageVolume.getPlugin().canReadWriteMany() ? "Y" : "N");

                // Parameters
                if (MapUtils.isNotEmpty(k8sStorageClassRow.getParameters())) {
                    List<ClusterVolumeParamterVO> parameters = Lists.newArrayList();
                    for (Map.Entry<String, String> paramEntry : k8sStorageClassRow.getParameters().entrySet()) {
                        ClusterVolumeParamterVO paramter = new ClusterVolumeParamterVO();
                        paramter.setName(paramEntry.getKey());
                        paramter.setValue(paramEntry.getValue());
                        parameters.add(paramter);
                    }
                    storageVolume.setParameters(parameters);
                }

                // MountOptions
                if (CollectionUtils.isNotEmpty(k8sStorageClassRow.getDetail().getMountOptions())) {
                    storageVolume.setMountOptions(k8sStorageClassRow.getDetail().getMountOptions());
                }


                if(useCapacity || useRequest){
                    ClusterVolumeCapacityVO clusterVolumeCapacity = this.getTotalCapacityByStorage(storageVolume
                            , k8sPersistentVolumeClaimsMap.get(storageVolume.getClusterSeq())
                            , k8sPersistentVolumesMap.get(storageVolume.getClusterSeq())
                            , context);

                    /**
                     * NFSSTATIC, NFSDYNAMIC, AWSEFS, GFS 는 Total Capacity 값이 존재하므로
                     * 위 plugin을 제외하고 Total Capacity 값 셋팅
                     */
                    if (useCapacity){
                        if(!storageVolume.getPlugin().haveTotalCapacity()){
                            if(storageVolume.getTotalCapacity() == null ||
                                    (storageVolume.getTotalCapacity() != null && storageVolume.getTotalCapacity().intValue() == 0)) {
                                storageVolume.setTotalCapacity(Integer.parseInt(String.valueOf(clusterVolumeCapacity.getAllocatedCapacity())));
                            }
                        }
                    }

                    /**
                     * 해당 storage class를 이용하여 생성된 PVC의 요청량 값 셋팅
                     */
                    if (useRequest){
                        storageVolume.setTotalRequest(Integer.parseInt(String.valueOf(clusterVolumeCapacity.getAllocatedCapacity())));
                        storageVolume.setTotalVolumeCount(Integer.parseInt(String.valueOf(clusterVolumeCapacity.getVolumeCount())));
                    }

                }

                storageVolumes.add(storageVolume);
            }
        }

        // Static Storage (StorageClass)
        if (MapUtils.isNotEmpty(k8sStaticStoragesMap) && MapUtils.getObject(k8sStaticStoragesMap, cluster.getClusterSeq(), null) != null) {
            for (ConfigMapGuiVO storageRow : k8sStaticStoragesMap.get(cluster.getClusterSeq())) {
                ClusterVolumeVO storageVolume = new ClusterVolumeVO();
                storageVolume.setClusterSeq(cluster.getClusterSeq());
                storageVolume.setName(storageRow.getName());
                storageVolume.setLabels(storageRow.getLabels());
                storageVolume.setAnnotations(storageRow.getAnnotations());
                storageVolume.setType(VolumeType.PERSISTENT_VOLUME_STATIC);
                storageVolume.setReclaimPolicy(ReclaimPolicy.valueOf(storageRow.getData().get(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_RECLAIM_POLICY)));
                storageVolume.setPhase(VolumePhase.AVAILABLE);
                storageVolume.setStorageClass(null);

                if (MapUtils.isNotEmpty(storageRow.getLabels())) {
                    // plugIn
                    if (MapUtils.getObject(storageRow.getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_PROVISIONER_TYPE, null) != null) {
                        storageVolume.setPlugin(VolumePlugIn.valueOf(storageRow.getLabels().get(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_PROVISIONER_TYPE)));
                    } else {
                        // default로 NFS로 셋팅
                        storageVolume.setPlugin(VolumePlugIn.NFSSTATIC);
                    }
                    // storageType
                    if (MapUtils.getObject(storageRow.getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE, null) != null) {
                        storageVolume.setStorageType(StorageType.valueOf(storageRow.getLabels().get(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE)));
                    } else {
                        storageVolume.setStorageType(VolumePlugIn.NFSSTATIC.getStorageType());
                    }
                    if (MapUtils.getObject(storageRow.getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_TOTAL_CAPACITY, null) != null) {
                        storageVolume.setTotalCapacity(Integer.parseInt(storageRow.getLabels().get(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_TOTAL_CAPACITY)));
                    } else {
                        // defalut로 100GB로 셋팅
                        if (storageVolume.getPlugin().haveTotalCapacity()) {
                            storageVolume.setTotalCapacity(100);
                        }
                    }
                }
                if (MapUtils.isNotEmpty(storageRow.getAnnotations())) {
                    // description
                    storageVolume.setDescription(new String(Base64Utils.decodeFromString(MapUtils.getString(storageRow.getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "")), "UTF-8"));
                }
                // Access Modes
                storageVolume.setReadWriteOnceYn("Y");
                storageVolume.setReadOnlyManyYn(storageVolume.getPlugin().canReadOnlyMany() ? "Y" : "N");
                storageVolume.setReadWriteManyYn(storageVolume.getPlugin().canReadWriteMany() ? "Y" : "N");

                List<ClusterVolumeParamterVO> parameters = Lists.newArrayList();
                ClusterVolumeParamterVO paramter = new ClusterVolumeParamterVO();
                paramter.setName(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_SERVER);
                paramter.setValue(storageRow.getData().get(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_SERVER));
                parameters.add(paramter);
                paramter = new ClusterVolumeParamterVO();
                paramter.setName(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_PATH);
                paramter.setValue(storageRow.getData().get(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_PATH));
                parameters.add(paramter);
                storageVolume.setParameters(parameters);

                if(useCapacity || useRequest){
                    ClusterVolumeCapacityVO clusterVolumeCapacity = this.getTotalCapacityByStorage(storageVolume
                            , k8sPersistentVolumeClaimsMap.get(storageVolume.getClusterSeq())
                            , k8sPersistentVolumesMap.get(storageVolume.getClusterSeq())
                            , context);

                    /**
                     * NFSSTATIC, NFSDYNAMIC, AWSEFS, GFS 는 Total Capacity 값이 존재하므로
                     * 위 plugin을 제외하고 Total Capacity 값 셋팅
                     */
                    if (useCapacity){
                        if(!storageVolume.getPlugin().haveTotalCapacity()){
                            if(storageVolume.getTotalCapacity() == null ||
                                    (storageVolume.getTotalCapacity() != null && storageVolume.getTotalCapacity().intValue() == 0)) {
                                storageVolume.setTotalCapacity(Integer.parseInt(String.valueOf(clusterVolumeCapacity.getAllocatedCapacity())));
                            }
                        }
                    }

                    /**
                     * 해당 storage class를 이용하여 생성된 PVC의 요청량 값 셋팅
                     */
                    if (useRequest){
                        storageVolume.setTotalRequest(Integer.parseInt(String.valueOf(clusterVolumeCapacity.getAllocatedCapacity())));
                        storageVolume.setTotalVolumeCount(Integer.parseInt(String.valueOf(clusterVolumeCapacity.getVolumeCount())));
                    }

                }

                storageVolumes.add(storageVolume);
            }
        }

        return storageVolumes;
    }

    public List<ClusterVolumeVO> getStorageVolumes(Integer accountSeq, List<Integer> serviceSeqs, Integer clusterSeq, String storageType, String type, boolean useCapacity, boolean useRequest) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, null, clusterSeq, serviceSeqs, null, "Y");

        List<ClusterVolumeVO> storageVolumes = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(clusters)) {

            ExecutingContextVO context = new ExecutingContextVO();

            /**
             * Cluster 별 StorageClass 목록, PVC 목록, PV 맵을 ClusterSeq를 Key로 Map에 셋팅
             */
            Map<Integer, List<K8sStorageClassVO>> k8sStorageClassesMap = new HashMap<>();
            Map<Integer, List<ConfigMapGuiVO>> k8sStaticStoragesMap = new HashMap<>();
            Map<Integer, List<K8sPersistentVolumeClaimVO>> k8sPersistentVolumeClaimsMap = new HashMap<>();
            Map<Integer, Map<String, K8sPersistentVolumeVO>> k8sPersistentVolumesMap = new HashMap<>();
            this.getStorageClassesWithPvc(accountSeq, null, clusterSeq, clusters, storageType, type, useCapacity, useRequest, k8sStorageClassesMap, k8sStaticStoragesMap, k8sPersistentVolumeClaimsMap, k8sPersistentVolumesMap, context);


            for (ClusterVO clusterRow : clusters) {
                this.genStorageVolumeData(clusterRow, k8sStorageClassesMap, k8sStaticStoragesMap, k8sPersistentVolumeClaimsMap, k8sPersistentVolumesMap, useCapacity, useRequest, storageVolumes, context);
            }

        }

        return storageVolumes;
    }

    public ClusterVolumeVO getStorageVolume(Integer clusterSeq, String storageName) throws Exception {
        return this.getStorageVolume(clusterSeq, storageName, false, false);
    }

    public ClusterVolumeVO getStorageVolume(Integer clusterSeq, String storageName, boolean useCapacity, boolean useRequest) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        ClusterVolumeVO storageVolume = null;
        if (clusterStateService.isClusterRunning(cluster)) {

            ExecutingContextVO context = new ExecutingContextVO();

            Map<Integer, List<K8sStorageClassVO>> k8sStorageClassesMap = new HashMap<>();
            Map<Integer, List<ConfigMapGuiVO>> k8sStaticStoragesMap = new HashMap<>();
            Map<Integer, List<K8sPersistentVolumeClaimVO>> k8sPersistentVolumeClaimsMap = new HashMap<>();
            Map<Integer, Map<String, K8sPersistentVolumeVO>> k8sPersistentVolumesMap = new HashMap<>();

            // StorageClass
            List<K8sStorageClassVO> k8sStorageClasses = storageClassService.getStorageClasses(cluster, String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, storageName), null, context);
            if(CollectionUtils.isNotEmpty(k8sStorageClasses)){
                k8sStorageClassesMap.put(clusterSeq, k8sStorageClasses);
            }
            // Static Storage ConfigMap 조회
            List<ConfigMapGuiVO> k8sConfigMaps = configMapService.getConfigMaps(
                    cluster,
                    KubeConstants.COCKTAIL_ADDON_NAMESPACE,
                    String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, storageName),
                    String.format("%s=%s", KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_VOLUME_TYPE, VolumeType.PERSISTENT_VOLUME_STATIC.getCode()));
            if (CollectionUtils.isNotEmpty(k8sConfigMaps)) {
                k8sStaticStoragesMap.put(clusterSeq, k8sConfigMaps);
            }
            if (useCapacity || useRequest){
                // PersistentVolumeClaim
                List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(cluster, "", null, null, context);
                if(CollectionUtils.isNotEmpty(persistentVolumeClaims)){
                    k8sPersistentVolumeClaimsMap.put(clusterSeq, persistentVolumeClaims);
                }
                // PersistentVolume
                Map<String, K8sPersistentVolumeVO> persistentVolumeMap = persistentVolumeService.convertPersistentVolumeDataMap(cluster, null, null, null);
                if(MapUtils.isNotEmpty(persistentVolumeMap)){
                    k8sPersistentVolumesMap.put(clusterSeq, persistentVolumeMap);
                }
            }

            List<ClusterVolumeVO> storageVolumes = this.genStorageVolumeData(cluster, k8sStorageClassesMap, k8sStaticStoragesMap, k8sPersistentVolumeClaimsMap, k8sPersistentVolumesMap, useCapacity, useRequest, null, context);
            if (CollectionUtils.isNotEmpty(storageVolumes)) {
                storageVolume = storageVolumes.get(0);
            }
        }

        return storageVolume;
    }

    public Boolean canDeleteStorageVolume(Integer clusterSeq, String storageName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVolumeVO cv = this.getStorageVolume(clusterSeq, storageName);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        if (cv == null) {
            throw new CocktailException(String.format("Can't find cluster volume: %d, %s", clusterSeq, storageName), ExceptionType.ClusterVolumeNotFound);
        }else{
            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(cluster);
        }

        /**
         * 해당 StorageClass로 생성한 PVC가 있다면 삭제 금지
         */
        List<K8sPersistentVolumeClaimVO> k8sPersistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(cv.getClusterSeq(), "", null, null, ContextHolder.exeContext());

        if(cv.getType() == VolumeType.PERSISTENT_VOLUME){

            if(CollectionUtils.isNotEmpty(k8sPersistentVolumeClaims)){
                Optional<K8sPersistentVolumeClaimVO> k8sPersistentVolumeClaimsOptional = k8sPersistentVolumeClaims.stream()
                        .filter(pvc -> (StringUtils.equals(cv.getName(), pvc.getStorageClassName())))
                        .findFirst();

                if(k8sPersistentVolumeClaimsOptional.isPresent()){
                    return false;
                }
            }

            // 기본 default storage는 삭제할 수 없습니다.
            if (BooleanUtils.toBoolean(cv.getBaseStorageYn())) {
                throw new CocktailException("You can not delete default storage. Please change the default assignment to another storage and try again.", ExceptionType.CanNotDeleteDefaultStorage);
            }
        }else if(cv.getType() == VolumeType.PERSISTENT_VOLUME_STATIC){
            Map<String, String> sourceParams = Maps.newHashMap();
            Map<String, String> targetParams = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(cv.getParameters())) {
                for (ClusterVolumeParamterVO paramterVO : cv.getParameters()) {
                    sourceParams.put(paramterVO.getName(), paramterVO.getValue());
                }
            }
            if(CollectionUtils.isNotEmpty(k8sPersistentVolumeClaims)){
                // storageClass로 만든 pvc 제외
                List<K8sPersistentVolumeClaimVO> k8sPersistentVolumes = k8sPersistentVolumeClaims.stream().filter(pvc -> (StringUtils.isBlank(pvc.getStorageClassName()))).collect(Collectors.toList());
                // PV에서 nfs.volumeSource 의 server, path 정보를 가지고 사용 중인 pvc 조회
                if (CollectionUtils.isNotEmpty(k8sPersistentVolumes)) {
                    for (K8sPersistentVolumeClaimVO claimRow : k8sPersistentVolumes) {
                        if (claimRow.getPersistentVolume() != null) {
                            V1PersistentVolume v1PersistentVolume = ServerUtils.unmarshalYaml(claimRow.getPersistentVolume().getDeploymentYaml());
                            if (v1PersistentVolume != null && v1PersistentVolume.getSpec().getNfs() != null) {
                                targetParams.put(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_SERVER, v1PersistentVolume.getSpec().getNfs().getServer());
                                targetParams.put(KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_PATH, v1PersistentVolume.getSpec().getNfs().getPath());

                                if (sourceParams.equals(targetParams)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }


    public void updateStorageVolume(ClusterVolumeVO clusterVolume) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVolumeVO volume = this.getStorageVolume(clusterVolume.getClusterSeq(), clusterVolume.getName(), true, false);
        if (volume == null) {
            throw new CocktailException(String.format("Can't find cluster volume: %d, %s", clusterVolume.getClusterSeq(), clusterVolume.getName()),
                    ExceptionType.ClusterVolumeNotFound);
        }

        // 총용량을 수정 가능하다. value check
        boolean capacityUpdate = false;
        if(clusterVolume.getPlugin().haveTotalCapacity()){
            int prevValue = volume.getTotalCapacity();
            int currentValue = clusterVolume.getTotalCapacity();
            if(prevValue > currentValue){
                throw new CocktailException(String.format("The requested capacity is greater than the total capacity of the cluster volume[%s].", clusterVolume.getName()),
                        ExceptionType.ClusterVolumeTotalCapacitySmall);
            } else {
                if (prevValue != currentValue) {
                    capacityUpdate = true;
                }
            }
        }else{
            // NFS 가 아닌 경우는 용량 수정이 안되게 값을 이전값으로 넣는다.
            clusterVolume.setTotalCapacity(volume.getTotalCapacity());
        }

        // 기본 스토리지는 지정만 가능, 지정 해제 불가
        boolean baseStorageFlagByAnno = storageClassService.isDefaultStorageClass(clusterVolume.getAnnotations());
        if (BooleanUtils.toBoolean(volume.getBaseStorageYn())) {
            if (!BooleanUtils.toBoolean(clusterVolume.getBaseStorageYn()) || !baseStorageFlagByAnno) {
                throw new CocktailException("Primary storage cannot be released.", ExceptionType.CanNotReleasedDefaultStorage);
            }
        } else {
            // annotation을 직접 수정시 변경되도록 셋팅
            if (baseStorageFlagByAnno) {
                clusterVolume.setBaseStorageYn("Y");
            }
        }

        ClusterVO cluster = clusterDao.getCluster(volume.getClusterSeq());
        if (clusterVolume.getType() == VolumeType.PERSISTENT_VOLUME) {
            // patch label, annotation(기본 지정 포함)
            storageClassService.patchDefaultStorageClass(cluster, clusterVolume);
        } else {
            if (capacityUpdate) {
                V1ConfigMap currConfigMap = k8sWorker.getConfigMapV1(cluster, KubeConstants.COCKTAIL_ADDON_NAMESPACE, volume.getName());
                V1ConfigMap updatedConfigMap = k8sPatchSpecFactory.copyObject(currConfigMap, new TypeReference<V1ConfigMap>(){});

                updatedConfigMap.getMetadata().getLabels().put(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_TOTAL_CAPACITY, String.valueOf(clusterVolume.getTotalCapacity().intValue()));

                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currConfigMap, updatedConfigMap);
                k8sWorker.patchConfigMapV1(cluster, KubeConstants.COCKTAIL_ADDON_NAMESPACE, volume.getName(), patchBody, false);
            }
        }

    }

    public void deleteStorageVolume(Integer clusterSeq, String storageName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVolumeVO volume = this.getStorageVolume(clusterSeq, storageName);
        if (volume == null) {
            throw new CocktailException(String.format("Can't find cluster volume: %d, %s", clusterSeq, storageName),
                    ExceptionType.ClusterVolumeNotFound);
        }

        if(volume.getType() == VolumeType.PERSISTENT_VOLUME){
            // 기본 default storage는 삭제할 수 없습니다.
            if (BooleanUtils.toBoolean(volume.getBaseStorageYn())) {
                throw new CocktailException("You can not delete default storage. Please change the default assignment to another storage and try again.", ExceptionType.CanNotDeleteDefaultStorage);
            }
            storageClassService.deleteStorageClass(volume.getClusterSeq(), volume.getName(), null);
        } else {
            ClusterVO cluster = clusterDao.getCluster(volume.getClusterSeq());
            configMapService.deleteConfigMap(cluster, KubeConstants.COCKTAIL_ADDON_NAMESPACE, volume.getName());
        }
    }

    public void checkStorageResource(ClusterVolumeVO clusterVolume, long requestCapacity, ExecutingContextVO context) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterVolume.getClusterSeq());

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(cluster, "", null, null, context);
        Map<String, K8sPersistentVolumeVO> persistentVolumeMap = persistentVolumeService.convertPersistentVolumeDataMap(cluster, null, null, null);

        ClusterVolumeCapacityVO clusterVolumeCapacity = this.getTotalCapacityByStorage(clusterVolume, persistentVolumeClaims, persistentVolumeMap, context);

        log.debug("Check Storage Resource => volumeSeq : {}, totalCapacity : {} GB, requestCapacity : {} GB, allocatedCapacity : {} GB", clusterVolume.getVolumeSeq(), clusterVolume.getTotalCapacity(), requestCapacity, clusterVolumeCapacity.getAllocatedCapacity());
        if(clusterVolume.getTotalCapacity().intValue() < (clusterVolumeCapacity.getAllocatedCapacity() + requestCapacity)){
            context.getParams().put("isValid", Boolean.FALSE);
            context.getParams().put("totalCapacity", clusterVolume.getTotalCapacity());
            context.getParams().put("requestCapacity", requestCapacity);
            context.getParams().put("allocatedCapacity", clusterVolumeCapacity.getAllocatedCapacity());
        }
    }

    private ClusterVolumeCapacityVO getTotalCapacityByStorage(ClusterVolumeVO clusterVolume, List<K8sPersistentVolumeClaimVO> persistentVolumeClaims, Map<String, K8sPersistentVolumeVO> persistentVolumeMap, ExecutingContextVO context) throws Exception {
        ClusterVolumeCapacityVO clusterVolumeCapacity = new ClusterVolumeCapacityVO();
        clusterVolumeCapacity.setClusterSeq(clusterVolume.getClusterSeq());
        clusterVolumeCapacity.setVolumeSeq(clusterVolume.getVolumeSeq());

        long allocatedCapacity = 0L;
        long volumeCount = 0L;

        if(clusterStateService.isClusterRunning(clusterVolume.getClusterSeq())){
            try {
                String server = null;
                String path = null;
                if(clusterVolume.getPlugin() == VolumePlugIn.NFSSTATIC){
                    server = clusterVolume.getParameters().stream().filter(cp -> (StringUtils.equalsIgnoreCase("server", cp.getName()))).map(cp -> (cp.getValue())).findFirst().orElseGet(() ->null);
                    path = clusterVolume.getParameters().stream().filter(cp -> (StringUtils.equalsIgnoreCase("path", cp.getName()))).map(cp -> (cp.getValue())).findFirst().orElseGet(() ->null);
                }

                if(CollectionUtils.isNotEmpty(persistentVolumeClaims)){
                    for(K8sPersistentVolumeClaimVO persistentVolumeClaimRow : persistentVolumeClaims){
                        if(clusterVolume.getPlugin() == VolumePlugIn.NFSSTATIC){
                            if (StringUtils.isNotBlank(persistentVolumeClaimRow.getVolumeName())){
                                if (MapUtils.isNotEmpty(persistentVolumeMap)){
                                    K8sPersistentVolumeVO persistentVolume = persistentVolumeMap.get(persistentVolumeClaimRow.getVolumeName());
                                    if( persistentVolume != null && persistentVolume.getDetail() != null && StringUtils.isNotBlank(persistentVolume.getDetail().getVolumeSource()) ){
                                        Map<String, Object> volumeSource = ObjectMapperUtils.getMapper().readValue(persistentVolume.getDetail().getVolumeSource(), new TypeReference<Map<String, Object>>(){});
                                        if (volumeSource.get("nfs") != null){
                                            Map<String, String> nfsMap = (Map<String, String>)volumeSource.get(KubeConstants.VOLUMES_PLUGINS_NFS);
                                            if(StringUtils.equals(nfsMap.get("server"), server) && StringUtils.equals(nfsMap.get("path"), path)){
                                                log.debug("{}({} - server : [{}], path : [{}]) - {}", persistentVolumeClaimRow.getName(), persistentVolumeClaimRow.getVolumeName(), server, path, persistentVolumeClaimRow.getCapacity().get(KubeConstants.VOLUMES_STORAGE));
                                                allocatedCapacity += persistentVolumeClaimRow.getCapacityByte()/1024/1024/1024;
                                                volumeCount += 1;
                                            }
                                        }
                                    }
                                }
                            }
                        }else {
                            if (StringUtils.isNotBlank(clusterVolume.getName())){
                                if ( StringUtils.equals(clusterVolume.getName(), persistentVolumeClaimRow.getStorageClassName())
                                        || StringUtils.equals(clusterVolume.getName(), MapUtils.getString(persistentVolumeClaimRow.getDetail().getAnnotations(), KubeConstants.VOLUMES_CLASS_NAME_BETA)) ){
                                    log.debug("{}({}) - {}", persistentVolumeClaimRow.getName(), persistentVolumeClaimRow.getStorageClassName(), persistentVolumeClaimRow.getCapacity().get("storage"));
                                    allocatedCapacity += persistentVolumeClaimRow.getCapacityByte()/1024/1024/1024;
                                    volumeCount += 1;
                                }
                            }
                        }
                    }

                    clusterVolumeCapacity.setAllocatedCapacity(allocatedCapacity);
                    clusterVolumeCapacity.setVolumeCount(volumeCount);
                }
            } catch (Exception e) {
                HttpServletRequest request = Utils.getCurrentRequest();
                CocktailException ce = new CocktailException("fail getTotalCapacityByStorage info!!", e, ExceptionType.K8sCocktailCloudInquireFail);
                log.error(ExceptionMessageUtils.setCommonResult(request, null, ce, false), e);
            }
        }

        return clusterVolumeCapacity;
    }

    /**
     * Namespace 별 PVC Total capacity
     *
     * @param clusterVolumes - Cluster 에 생성된 스토리지 목록
     * @param persistentVolumeClaims
     * @param persistentVolumeMap
     * @param context
     * @return Map<namespace, Map<clusterVolumeName, ClusterVolumeCapacityVO>
     * @throws Exception
     */
    public Map<String, Map<String, ClusterVolumeCapacityVO>> getNamespacedTotalCapacityByStorage(List<ClusterVolumeVO> clusterVolumes, List<K8sPersistentVolumeClaimVO> persistentVolumeClaims, Map<String, K8sPersistentVolumeVO> persistentVolumeMap, ExecutingContextVO context) throws Exception {

        // Map<namespace, Map<clusterVolumeName, ClusterVolumeCapacityVO>
        Map<String, Map<String, ClusterVolumeCapacityVO>> clusterVolumeRequestMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(clusterVolumes)) {
            for (ClusterVolumeVO clusterVolumeRow : clusterVolumes) {
                if(clusterStateService.isClusterRunning(clusterVolumeRow.getClusterSeq())){
                    try {
                        String server = null;
                        String path = null;
                        if(clusterVolumeRow.getPlugin() == VolumePlugIn.NFSSTATIC){
                            server = clusterVolumeRow.getParameters().stream().filter(cp -> (StringUtils.equalsIgnoreCase("server", cp.getName()))).map(cp -> (cp.getValue())).findFirst().orElseGet(() ->null);
                            path = clusterVolumeRow.getParameters().stream().filter(cp -> (StringUtils.equalsIgnoreCase("path", cp.getName()))).map(cp -> (cp.getValue())).findFirst().orElseGet(() ->null);
                        }

                        if(CollectionUtils.isNotEmpty(persistentVolumeClaims)){
                            for(K8sPersistentVolumeClaimVO persistentVolumeClaimRow : persistentVolumeClaims){
                                long allocatedCapacity = 0L;
                                long volumeCount = 0L;

                                if(clusterVolumeRow.getPlugin() == VolumePlugIn.NFSSTATIC){
                                    if (StringUtils.isNotBlank(persistentVolumeClaimRow.getVolumeName())){
                                        if (MapUtils.isNotEmpty(persistentVolumeMap)){
                                            K8sPersistentVolumeVO persistentVolume = persistentVolumeMap.get(persistentVolumeClaimRow.getVolumeName());
                                            if( persistentVolume != null && persistentVolume.getDetail() != null && StringUtils.isNotBlank(persistentVolume.getDetail().getVolumeSource()) ){
                                                Map<String, Object> volumeSource = ObjectMapperUtils.getMapper().readValue(persistentVolume.getDetail().getVolumeSource(), new TypeReference<Map<String, Object>>(){});
                                                if (volumeSource.get("nfs") != null){
                                                    Map<String, String> nfsMap = (Map<String, String>)volumeSource.get(KubeConstants.VOLUMES_PLUGINS_NFS);
                                                    if(StringUtils.equals(nfsMap.get("server"), server) && StringUtils.equals(nfsMap.get("path"), path)){
                                                        log.debug("{}({} - server : [{}], path : [{}]) - {}", persistentVolumeClaimRow.getName(), persistentVolumeClaimRow.getVolumeName(), server, path, persistentVolumeClaimRow.getCapacity().get(KubeConstants.VOLUMES_STORAGE));
                                                        allocatedCapacity = persistentVolumeClaimRow.getCapacityByte()/1024/1024/1024;
                                                        volumeCount = 1;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }else {
                                    if (StringUtils.isNotBlank(clusterVolumeRow.getName())){
                                        if ( StringUtils.equals(clusterVolumeRow.getName(), persistentVolumeClaimRow.getStorageClassName())
                                                || StringUtils.equals(clusterVolumeRow.getName(), MapUtils.getString(persistentVolumeClaimRow.getDetail().getAnnotations(), KubeConstants.VOLUMES_CLASS_NAME_BETA)) ){
                                            log.debug("{}({}) - {}", persistentVolumeClaimRow.getName(), persistentVolumeClaimRow.getStorageClassName(), persistentVolumeClaimRow.getCapacity().get("storage"));
                                            allocatedCapacity = persistentVolumeClaimRow.getCapacityByte()/1024/1024/1024;
                                            volumeCount = 1;
                                        }
                                    }
                                }

                                if (MapUtils.getObject(clusterVolumeRequestMap, persistentVolumeClaimRow.getNamespace(), null) == null) {

                                    ClusterVolumeCapacityVO clusterVolumeCapacity = new ClusterVolumeCapacityVO();
                                    clusterVolumeCapacity.setClusterSeq(clusterVolumeRow.getClusterSeq());
                                    clusterVolumeCapacity.setStorageName(clusterVolumeRow.getName());

                                    Map<String, ClusterVolumeCapacityVO> clusterVolumeCapacityMap = new HashMap<>();
                                    clusterVolumeCapacityMap.put(clusterVolumeRow.getName(), clusterVolumeCapacity);

                                    clusterVolumeRequestMap.put(persistentVolumeClaimRow.getNamespace(), clusterVolumeCapacityMap);
                                }else {
                                    if (MapUtils.getObject(clusterVolumeRequestMap.get(persistentVolumeClaimRow.getNamespace()), clusterVolumeRow.getName(), null) == null) {
                                        ClusterVolumeCapacityVO clusterVolumeCapacity = new ClusterVolumeCapacityVO();
                                        clusterVolumeCapacity.setClusterSeq(clusterVolumeRow.getClusterSeq());
                                        clusterVolumeCapacity.setStorageName(clusterVolumeRow.getName());

                                        clusterVolumeRequestMap.get(persistentVolumeClaimRow.getNamespace()).put(clusterVolumeRow.getName(), clusterVolumeCapacity);
                                    }
                                }
                                ClusterVolumeCapacityVO clusterVolumeCapacity = clusterVolumeRequestMap.get(persistentVolumeClaimRow.getNamespace()).get(clusterVolumeRow.getName());
                                clusterVolumeRequestMap.get(persistentVolumeClaimRow.getNamespace()).get(clusterVolumeRow.getName()).setAllocatedCapacity(clusterVolumeCapacity.getAllocatedCapacity() + allocatedCapacity);
                                clusterVolumeRequestMap.get(persistentVolumeClaimRow.getNamespace()).get(clusterVolumeRow.getName()).setVolumeCount(clusterVolumeCapacity.getVolumeCount() + volumeCount);
                            }
                        }
                    } catch (Exception e) {
                        HttpServletRequest request = Utils.getCurrentRequest();
                        CocktailException ce = new CocktailException("fail getNamespacedTotalCapacityByStorage info!!", e, ExceptionType.K8sCocktailCloudInquireFail);
                        log.error(ExceptionMessageUtils.setCommonResult(request, null, ce, false), e);
                    }
                }
            }
        }

        return clusterVolumeRequestMap;
    }
}

