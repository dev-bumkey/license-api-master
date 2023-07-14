package run.acloud.api.catalog.service;

import com.google.api.client.util.Sets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.utils.PackageUtils;
import run.acloud.api.catalog.vo.*;
import run.acloud.api.configuration.service.AddonCommonService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.protobuf.chart.Package;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author gun@acornsoft.io
 * Created on 2019. 1. 29.
 */
@Slf4j
@Service
public class PackageK8sService {

    @Autowired
    private HelmService helmService;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private AddonCommonService addonCommonService;

    @Autowired
    private IngressSpecService ingressSpecService;

    @Autowired
    private ServiceSpecService serviceSpecService;

    @Autowired
    private ConfigMapService configMapService;

    @Autowired
    private SecretService secretService;

    @Autowired
    private PersistentVolumeService persistentVolumeService;

    @Autowired
    private StorageClassService storageClassService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private PackageInfoService packageInfoService;

    @Autowired
    private PackageValidService packageValidService;

    @Autowired
    private PackageHistoryService packageHistoryService;


    /**
     * Print Cluster Access Info
     * @param clusterSeq
     * @throws Exception
     */
    public void printClusterAccessInfo(Integer clusterSeq) throws Exception {
        log.debug(JsonUtils.toPrettyString(packageInfoService.getClusterAccessInfo(clusterSeq)));

    }

    /**
     * Chart 버전 정보 조회
     * @param repository
     * @param name
     * @return
     * @throws Exception
     */
    public List<ChartInfoBaseVO> getChartVersions(String repository, String name) throws Exception {
        try {

            ChartRequestBaseVO chartRequestBase = new ChartRequestBaseVO();
            chartRequestBase.setRepo(repository);
            chartRequestBase.setName(name);

            Package.ChartVersionsResponse chartVersionsResponse =  helmService.getChartVersions(chartRequestBase);

            return PackageUtils.convertChartVersions(chartVersionsResponse);
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.ChartInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.ChartInquireFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }


    /**
     * Package Status(개별) 조회
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @param revision
     * @return
     * @throws Exception
     */
    public HelmReleaseBaseVO getPackageStatus(Integer clusterSeq, String namespaceName, String releaseName, String revision) throws Exception {
        try {
            HelmStatusRequestVO helmStatusRequest = new HelmStatusRequestVO();
            helmStatusRequest.setNamespace(namespaceName);
            helmStatusRequest.setReleaseName(releaseName);
            helmStatusRequest.setRevision(revision);
            helmStatusRequest.setClusterAccessInfo(packageInfoService.getClusterAccessInfo(clusterSeq));

            Package.HelmStatusResponse response =  helmService.getPackageStatus(helmStatusRequest);

            HelmReleaseBaseVO helmRelease = PackageUtils.convertRelease(response.getRelease());
            helmRelease = packageInfoService.fillRelation(helmRelease, clusterSeq, namespaceName);

            PackageDeployHistoryVO history = packageHistoryService.getPackageDeployHistory(clusterSeq, namespaceName, releaseName, helmRelease.getChartName(),
                helmRelease.getChartVersion(), helmRelease.getRevision(), null);

            /** Values 정보 설정 **/
            if(history != null) {
                // 이력 데이터가 존재하면 이력의 values Yaml 파일을 이용하여 이력 적재
                if(log.isDebugEnabled()) log.debug("========================== YAML (found history) ===========================\n" + JsonUtils.toPrettyString(history));
                helmRelease.setValues(history.getChartValues());
                helmRelease.setRepo(history.getRepository());
            }
            else {
                // 이력 데이터가 존재하지 않으면, Package 배포 정보의 데이터를 Yaml로 컨버트 하여 이력 적재
                Map<String, Object> jsonMap = JsonUtils.fromGson(helmRelease.getChart(), HashMap.class);
                String yamlStr = Yaml.getSnakeYaml().dump(jsonMap.get("values"));
                if(log.isDebugEnabled()) log.debug("========================== YAML (not found history) ===========================\n" + yamlStr);
                helmRelease.setValues(yamlStr);
            }

            helmRelease.setChart(null);
            return helmRelease;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageStatusInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageStatusInquireFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }

    /**
     * Package Revision List 조회
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @return
     * @throws Exception
     */
    public List<HelmReleaseInfoVO> getPackageRevisions(Integer clusterSeq, String namespaceName, String releaseName) throws Exception {
        try {
            HelmStatusRequestVO helmStatusRequest = new HelmStatusRequestVO();
            helmStatusRequest.setNamespace(namespaceName);
            helmStatusRequest.setReleaseName(releaseName);
            helmStatusRequest.setClusterAccessInfo(packageInfoService.getClusterAccessInfo(clusterSeq));

            Package.HelmHistoryResponse response =  helmService.getPackageRevisions(helmStatusRequest);

            List<HelmReleaseInfoVO> helmReleaseInfoList = new ArrayList<>();
            for (Package.ReleaseInfo releaseInfo : response.getReleaseInfosList()) {
                HelmReleaseInfoVO helmReleaseInfo = new HelmReleaseInfoVO();
                Optional.ofNullable(releaseInfo.getRevision()).ifPresent(rl -> helmReleaseInfo.setRevision(rl));
                Optional.ofNullable(releaseInfo.getUpdated()).ifPresent(rl -> helmReleaseInfo.setUpdated(rl));
                Optional.ofNullable(releaseInfo.getChart()).ifPresent(rl -> helmReleaseInfo.setChart(rl));
                Optional.ofNullable(releaseInfo.getStatus()).ifPresent(rl -> helmReleaseInfo.setStatus(rl));
                Optional.ofNullable(releaseInfo.getAppVersion()).ifPresent(rl -> helmReleaseInfo.setAppVersion(rl));
                Optional.ofNullable(releaseInfo.getDescription()).ifPresent(rl -> helmReleaseInfo.setDescription(rl));

                helmReleaseInfoList.add(helmReleaseInfo);
            }

            try {
                helmReleaseInfoList.sort(Comparator.comparingInt(s -> Integer.parseInt(s.getRevision())));
                Collections.reverse(helmReleaseInfoList);
            }
            catch (Exception ex) {
                log.error("1. Can't Sort Revision. retry with string type. : PackageService.getPackageRevisions");
                try {
                    helmReleaseInfoList.sort(Comparator.comparing(s -> s.getRevision()));
                    Collections.reverse(helmReleaseInfoList);
                }
                catch (Exception ex2) {
                    log.error("2. Can't Sort Revision : PackageService.getPackageRevisions");
                }
            }

            return helmReleaseInfoList;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageHistoryInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageHistoryInquireFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }

    /**
     * Package 상세 정보 조회 (include Resources)
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @return
     * @throws Exception
     */
    public HelmResourcesVO getPackageResources(Integer clusterSeq, String namespaceName, String releaseName) throws Exception {
        try {
            /**
             * Cluster 유효성 체크
             * **/
            ClusterVO cluster = packageInfoService.setupCluster(clusterSeq, namespaceName);
            if(cluster == null) {
                throw new CocktailException("Fail to get cluster information", ExceptionType.ClusterNotFound);
            }

            /**
             * Helm Resource 조회
             * **/
            HelmReleaseBaseVO helmReleaseBase = this.getPackageStatus(clusterSeq, namespaceName, releaseName, null);
            if(helmReleaseBase == null) {
                throw new CocktailException("Fail to get package Resource", ExceptionType.PackageResourceInquireFail);
            }

            /**
             * Helm Resource 응답 객체 생성 및 기본 정보 설정
             * **/
            HelmResourcesVO helmResources = new HelmResourcesVO();
            BeanUtils.copyProperties(helmReleaseBase, helmResources);
            helmResources.setChart(null); // Chart Data 응답하지 않음.

            if(log.isDebugEnabled()) log.debug(JsonUtils.toPrettyString(helmResources));

            /**
             * 배포된 Helm Chart의 Manifest 파일을 Parsing 하여 조회할 리소스 추출
             * **/
            helmResources = getHelmResourcesFromManifest(helmReleaseBase.getManifest(), clusterSeq, cluster, namespaceName, helmResources);

            /** Resources 조회중 오류 발생시 기본 정보는 다시 넣어줌.. **/
            if(helmResources == null) {
                helmResources = new HelmResourcesVO();
                BeanUtils.copyProperties(helmReleaseBase, helmResources);
                helmResources.setChart(null); // Chart Data 응답하지 않음.
            }

            return helmResources;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("Can't get Resources", ExceptionType.PackageResourceInquireFail);
        }
    }

    /**
     * Manifest File로부터 HelmResource 데이터를 조회.
     * @param manifest
     * @param clusterSeq
     * @param cluster
     * @param defaultNamespace
     * @param helmResources
     * @return
     */
    public HelmResourcesVO getHelmResourcesFromManifest(String manifest, Integer clusterSeq, ClusterVO cluster, String defaultNamespace, HelmResourcesVO helmResources) throws Exception {
        try {
            if (cluster == null) {
                cluster = packageInfoService.setupCluster(clusterSeq, defaultNamespace);
                if (cluster == null) {
                    throw new CocktailException("Fail to get cluster information", ExceptionType.ClusterNotFound);
                }
            }

            /**
             * Helm Resource 응답 객체 생성
             * **/
            if (helmResources == null) {
                helmResources = new HelmResourcesVO();
            }

            /**
             * 배포된 Helm Chart의 Manifest 파일을 Parsing 하여 조회할 리소스 추출
             * **/
            List<K8sObjectMapVO> k8sObjectList = this.getK8sObjectList(manifest, defaultNamespace);
            /**
             * Define the Response Objects.
             */
            K8sControllersVO k8sControllers = new K8sControllersVO();
            List<K8sServiceVO> services = new ArrayList<>();
            List<K8sIngressVO> ingresses = new ArrayList<>();
            List<ConfigMapGuiVO> configMaps = new ArrayList<>();
            List<SecretGuiVO> secrets = new ArrayList<>();
            List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = new ArrayList<>();
            Map<String, K8sPersistentVolumeVO> persistentVolumesMap = null;
            Map<String, K8sStorageClassVO> storageClassMap = null;

            List<K8sEventVO> events;
            List<K8sPodVO> pods = new ArrayList<>();
            List<K8sPodVO> allPods;
            Map<String, List<K8sPodVO>> podMap = new HashMap<>();

            String field = null;
            String label = null;

            /** Event List 조회 **/
            events = k8sResourceService.getEventByCluster(clusterSeq, defaultNamespace, null, null, ContextHolder.exeContext());
            Map<String, Map<String, List<K8sEventVO>>> eventMap = new HashMap<>();
            for (K8sEventVO eventRow : Optional.ofNullable(events).orElseGet(() ->new ArrayList<>())) {
                if (!eventMap.containsKey(eventRow.getKind())) {
                    eventMap.put(eventRow.getKind(), Maps.newHashMap());
                }
                if (!eventMap.get(eventRow.getKind()).containsKey(eventRow.getName())) {
                    eventMap.get(eventRow.getKind()).put(eventRow.getName(), Lists.newArrayList());
                }
                eventMap.get(eventRow.getKind()).get(eventRow.getName()).add(eventRow);
            }
            /** Pod List 조회 **/
            allPods = workloadResourceService.getPods(clusterSeq, null, defaultNamespace, null, ContextHolder.exeContext());
            addonCommonService.getPodToMap(allPods, podMap, MapUtils.getObject(eventMap, K8sApiKindType.POD.getValue(), Maps.newHashMap()));

            if (CollectionUtils.isNotEmpty(k8sObjectList)) {
                Map<K8sApiKindType, Map<String, Map<String, ? extends Object>>> k8sObjMap = Maps.newHashMap();
                Set<String> namespaceSet = Sets.newHashSet();

                for (K8sObjectMapVO k8sObj : k8sObjectList) {
                    // set namespace
                    namespaceSet.add(k8sObj.getNamespace());
                    switch (k8sObj.getK8sApiKindType()) {
                        case DEPLOYMENT -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.DEPLOYMENT) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.DEPLOYMENT), k8sObj.getNamespace(), null) == null) {
                                List<K8sDeploymentVO> k8sDeployments = workloadResourceService.getDeployments(cluster, k8sObj.getNamespace(), field, label, ContextHolder.exeContext());
                                k8sObjMap.putIfAbsent(K8sApiKindType.DEPLOYMENT, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.DEPLOYMENT).put(k8sObj.getNamespace(), Optional.ofNullable(k8sDeployments).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sDeploymentVO::getName, Function.identity())));
                            }
                        }
                        case STATEFUL_SET -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.STATEFUL_SET) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.STATEFUL_SET), k8sObj.getNamespace(), null) == null) {
                                List<K8sStatefulSetVO> k8sStatefulSets = workloadResourceService.getStatefulSets(cluster, k8sObj.getNamespace(), field, label, ContextHolder.exeContext());
                                k8sObjMap.putIfAbsent(K8sApiKindType.STATEFUL_SET, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.STATEFUL_SET).put(k8sObj.getNamespace(), Optional.ofNullable(k8sStatefulSets).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sStatefulSetVO::getName, Function.identity())));
                            }
                        }
                        case DAEMON_SET -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.DAEMON_SET) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.DAEMON_SET), k8sObj.getNamespace(), null) == null) {
                                List<K8sDaemonSetVO> k8sDaemonSets = workloadResourceService.getDaemonSets(cluster, k8sObj.getNamespace(), field, label, ContextHolder.exeContext());
                                k8sObjMap.putIfAbsent(K8sApiKindType.DAEMON_SET, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.DAEMON_SET).put(k8sObj.getNamespace(), Optional.ofNullable(k8sDaemonSets).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sDaemonSetVO::getName, Function.identity())));
                            }
                        }
                        case CRON_JOB -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.CRON_JOB) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.CRON_JOB), k8sObj.getNamespace(), null) == null) {
                                List<K8sCronJobVO> k8sCronJobs = workloadResourceService.getCronJobs(cluster, k8sObj.getNamespace(), field, label, ContextHolder.exeContext());
                                k8sObjMap.putIfAbsent(K8sApiKindType.CRON_JOB, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.CRON_JOB).put(k8sObj.getNamespace(), Optional.ofNullable(k8sCronJobs).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sCronJobVO::getName, Function.identity())));
                            }
                        }
                        case JOB -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.JOB) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.JOB), k8sObj.getNamespace(), null) == null) {
                                List<K8sJobVO> k8sJobs = workloadResourceService.getJobs(cluster, k8sObj.getNamespace(), field, label, ContextHolder.exeContext());
                                k8sObjMap.putIfAbsent(K8sApiKindType.JOB, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.JOB).put(k8sObj.getNamespace(), Optional.ofNullable(k8sJobs).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sJobVO::getName, Function.identity())));
                            }
                        }
                        case SERVICE -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.SERVICE) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.SERVICE), k8sObj.getNamespace(), null) == null) {
                                List<K8sServiceVO> svcList = serviceSpecService.getServices(cluster, k8sObj.getNamespace(), field, label, ContextHolder.exeContext());
                                k8sObjMap.putIfAbsent(K8sApiKindType.SERVICE, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.SERVICE).put(k8sObj.getNamespace(), Optional.ofNullable(svcList).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sServiceVO::getServiceName, Function.identity())));
                            }
                        }
                        case INGRESS -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.INGRESS) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.INGRESS), k8sObj.getNamespace(), null) == null) {
                                List<K8sIngressVO> ingressList = ingressSpecService.getIngresses(cluster, field, label, ContextHolder.exeContext());
                                k8sObjMap.putIfAbsent(K8sApiKindType.INGRESS, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.INGRESS).put(k8sObj.getNamespace(), Optional.ofNullable(ingressList).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sIngressVO::getName, Function.identity())));
                            }
                        }
                        case CONFIG_MAP -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.CONFIG_MAP) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.CONFIG_MAP), k8sObj.getNamespace(), null) == null) {
                                List<ConfigMapGuiVO> configMapList = configMapService.getConfigMaps(cluster, k8sObj.getNamespace(), field, label);
                                k8sObjMap.putIfAbsent(K8sApiKindType.CONFIG_MAP, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.CONFIG_MAP).put(k8sObj.getNamespace(), Optional.ofNullable(configMapList).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(ConfigMapGuiVO::getName, Function.identity())));
                            }
                        }
                        case SECRET -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.SECRET) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.SECRET), k8sObj.getNamespace(), null) == null) {
                                List<SecretGuiVO> secretList = secretService.getSecrets(cluster, k8sObj.getNamespace(), field, label, true);
                                k8sObjMap.putIfAbsent(K8sApiKindType.SECRET, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.SECRET).put(k8sObj.getNamespace(), Optional.ofNullable(secretList).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(SecretGuiVO::getName, Function.identity())));
                            }
                        }
                        case PERSISTENT_VOLUME_CLAIM -> {
                            if (!k8sObjMap.containsKey(K8sApiKindType.PERSISTENT_VOLUME_CLAIM) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.PERSISTENT_VOLUME_CLAIM), k8sObj.getNamespace(), null) == null) {
                                List<K8sPersistentVolumeClaimVO> pvcList = this.genPersistentVolumeClaims(cluster, k8sObj.getNamespace(), field, label, persistentVolumesMap, storageClassMap);
                                k8sObjMap.putIfAbsent(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.PERSISTENT_VOLUME_CLAIM).put(k8sObj.getNamespace(), Optional.ofNullable(pvcList).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sPersistentVolumeClaimVO::getName, Function.identity())));
                            }
                        }
                    }
                }

                // manifest에 없는 volumeTemplate pvc를 위해 셋팅
                if (!namespaceSet.isEmpty() && k8sObjMap.containsKey(K8sApiKindType.STATEFUL_SET)) {
                    for (String ns : namespaceSet) {
                        // 해당 네임스페이스에 statefulset이 있다면 셋팅
                        if (MapUtils.getObject(k8sObjMap.get(K8sApiKindType.STATEFUL_SET), ns, null) != null) {
                            if (!k8sObjMap.containsKey(K8sApiKindType.PERSISTENT_VOLUME_CLAIM) || MapUtils.getObject(k8sObjMap.get(K8sApiKindType.PERSISTENT_VOLUME_CLAIM), ns, null) == null) {
                                List<K8sPersistentVolumeClaimVO> pvcList = this.genPersistentVolumeClaims(cluster, ns, field, label, persistentVolumesMap, storageClassMap);
                                k8sObjMap.putIfAbsent(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, Maps.newHashMap());
                                k8sObjMap.get(K8sApiKindType.PERSISTENT_VOLUME_CLAIM).put(ns, Optional.ofNullable(pvcList).orElseGet(() -> Lists.newArrayList()).stream().collect(Collectors.toMap(K8sPersistentVolumeClaimVO::getName, Function.identity())));
                            }
                        }
                    }
                }

                /**
                 * Manifest에서 추출한 Resource List를 K8s에서 조회하여 응답 객체 생성
                 * **/
                for (K8sObjectMapVO k8sObj : Optional.ofNullable(k8sObjectList).orElseGet(() -> Lists.newArrayList())) {
                    /* Resource List Logging.................... */
                    log.debug("====================================================\n" +
                            k8sObj.getApiVersion() + " : " +
                            k8sObj.getK8sApiKindType().getValue() + " : " +
                            k8sObj.getName() + " : " +
                            k8sObj.getNamespace() + "\n----------------------------------------------\n" +
                            JsonUtils.toPrettyString(k8sObj.getK8sObj())
                    );

                    field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, k8sObj.getName());
                    switch (k8sObj.getK8sApiKindType()) {
                        case DEPLOYMENT: {
                            /** 1. Get Deployments. **/
                            List<K8sDeploymentVO> k8sDeployments = new ArrayList<>(Collections.singletonList((K8sDeploymentVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));

                            /** 2. Get ReplicaSets in Deployments **/
                            Map<String, List<K8sReplicaSetVO>> replicaSetMapInDeployments = new HashMap<>();
                            List<K8sReplicaSetVO> replicaSetsInDeployments = new ArrayList<>();
                            for (K8sDeploymentVO k8sDeploymentRow : Optional.ofNullable(k8sDeployments).orElseGet(() -> Lists.newArrayList())) {
                                Optional.ofNullable(k8sDeploymentRow).map(K8sDeploymentVO::getOldReplicaSets).ifPresent(list -> replicaSetsInDeployments.addAll(list));
                                Optional.ofNullable(k8sDeploymentRow).map(K8sDeploymentVO::getNewReplicaSets).ifPresent(list -> replicaSetsInDeployments.addAll(list));
                            }
                            addonCommonService.getReplicaSetToMap(replicaSetsInDeployments, replicaSetMapInDeployments, MapUtils.getObject(eventMap, K8sApiKindType.REPLICA_SET.getValue(), Maps.newHashMap()));

                            /** 3. Set replicaSet owner pod **/
                            for (K8sDeploymentVO k8sDeploymentRow : Optional.ofNullable(k8sDeployments).orElseGet(() -> Lists.newArrayList())) {
                                Optional.ofNullable(k8sDeploymentRow.getNewReplicaSets()).ifPresent(replicas -> addonCommonService.addPod(pods, podMap, replicas.get(0).getName()));
                                Optional.ofNullable(k8sDeploymentRow.getOldReplicaSets()).ifPresent(replicas -> addonCommonService.addPod(pods, podMap, replicas.get(0).getName()));
                                // Set replicaSet
                                k8sControllers = this.setReplicaSets(k8sControllers, replicaSetMapInDeployments.get(k8sDeploymentRow.getName()));
                            }
                            /** 4. Set Deployments. **/
                            k8sControllers.addAllDeploymentsItem(k8sDeployments);
                            break;
                        }
                        case POD: {
                            /** 1. Get Pods **/
                            List<K8sPodVO> k8sPods = workloadResourceService.getPods(cluster, field, label);
                            /** 2. Set Events **/
                            for (K8sPodVO k8sPodRow : Optional.ofNullable(k8sPods).orElseGet(() -> Lists.newArrayList())) {
                                // set Events
                                k8sPodRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.POD.getValue(), Maps.newHashMap()).get(k8sPodRow.getPodName()));
                            }
                            /** 3. Add Pods **/
                            pods.addAll(k8sPods);
                            break;
                        }
                        case REPLICA_SET: {
                            /** 1. Get ReplicaSets **/
                            List<K8sReplicaSetVO> k8sReplicaSets = workloadResourceService.convertReplicaSetDataList(cluster, k8sObj.getNamespace(), field, label);
                            /** 2. Set Events and Add Pods**/
                            for (K8sReplicaSetVO k8sReplicaSetRow : Optional.ofNullable(k8sReplicaSets).orElseGet(() -> Lists.newArrayList())) {
                                // set Events
                                k8sReplicaSetRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.REPLICA_SET.getValue(), Maps.newHashMap()).get(k8sReplicaSetRow.getName()));
                                // set Pods in ReplicaSet
                                addonCommonService.addPod(pods, podMap, k8sReplicaSetRow.getName());
                            }
                            /** 3. Set ReplicaSets. **/
                            k8sControllers = this.setReplicaSets(k8sControllers, k8sReplicaSets);
                            break;
                        }
                        case STATEFUL_SET: {
                            /** 1. Get StatefulSets **/
                            List<K8sStatefulSetVO> k8sStatefulSets = new ArrayList<>(Collections.singletonList((K8sStatefulSetVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));
                            /** 2. Set Events and Add Pods**/
                            for (K8sStatefulSetVO k8sStatefulSetRow : Optional.ofNullable(k8sStatefulSets).orElseGet(() -> Lists.newArrayList())) {
                                k8sStatefulSetRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.STATEFUL_SET.getValue(), Maps.newHashMap()).get(k8sStatefulSetRow.getName()));
                                addonCommonService.addPod(pods, podMap, k8sStatefulSetRow.getName());
                            }
                            /** 3. Set StatefulSets. **/
                            k8sControllers.addAllStatefulSetsItem(k8sStatefulSets);

                            /** 4. volumeClaimTemplates -> PVC에 추가 **/
                            Map<String, ? extends Object> pvcMap = Optional.ofNullable(k8sObjMap.get(K8sApiKindType.PERSISTENT_VOLUME_CLAIM)).map(pvc -> pvc.get(k8sObj.getNamespace())).orElseGet(Maps::newHashMap);
                            for (K8sStatefulSetVO statefulSet : Optional.ofNullable(k8sStatefulSets).orElseGet(() -> Lists.newArrayList())) {
                                for (K8sPersistentVolumeClaimVO vctRow : Optional.ofNullable(statefulSet).map(K8sStatefulSetVO::getDetail).map(K8sStatefulSetDetailVO::getVolumeClaimTemplates).orElseGet(() -> Lists.newArrayList())) {
                                    String pvcName = String.format("%s-%s-", vctRow.getName(), statefulSet.getName());
                                    for (int idx = 0, idxe = statefulSet.getDesiredPodCnt(); idx < idxe; idx++) {
                                        // [VolumeClaimTemplate명]-[statefulSet명]-[idx] = PVC 이름 => 조회하여 PVC 목록에 넣어줌..
//                                        String volumeTemplateFieldSelector = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, String.format("%s%d", pvcName, idx));
//                                        List<K8sPersistentVolumeClaimVO> pvcList = this.genPersistentVolumeClaims(cluster, k8sObj.getNamespace(), volumeTemplateFieldSelector, null, persistentVolumesMap, storageClassMap);
//                                        persistentVolumeClaims.addAll(pvcList);
                                        String pvcNameIdx = String.format("%s%d", pvcName, idx);

                                        if (pvcMap.containsKey(pvcNameIdx)) {
                                            persistentVolumeClaims.add((K8sPersistentVolumeClaimVO)(k8sObjMap.get(K8sApiKindType.PERSISTENT_VOLUME_CLAIM).get(k8sObj.getNamespace())).get(pvcNameIdx));
                                        }
                                    }
                                }
                            }

                            break;
                        }
                        case DAEMON_SET: {
                            /** 1. Get DaemonSets **/
                            List<K8sDaemonSetVO> k8sDaemonSets = new ArrayList<>(Collections.singletonList((K8sDaemonSetVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));
                            /** 2. Set Events and Add Pods**/
                            for (K8sDaemonSetVO k8sDaemonSetRow : Optional.ofNullable(k8sDaemonSets).orElseGet(() -> Lists.newArrayList())) {
                                k8sDaemonSetRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.DAEMON_SET.getValue(), Maps.newHashMap()).get(k8sDaemonSetRow.getName()));
                                addonCommonService.addPod(pods, podMap, k8sDaemonSetRow.getName());
                            }
                            /** 3. Set DaemonSets. **/
                            k8sControllers.addAllDaemonSetsItem(k8sDaemonSets);
                            break;
                        }
                        case CRON_JOB: {
                            /** 1. Get CronJobs **/
                            List<K8sCronJobVO> k8sCronJobs = new ArrayList<>(Collections.singletonList((K8sCronJobVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));

                            /** 2. Set Jobs From CronJobs **/
                            List<K8sJobVO> currentJobs = new ArrayList<>();
                            for (K8sCronJobVO k8sCronJobRow : Optional.ofNullable(k8sCronJobs).orElseGet(() -> Lists.newArrayList())) {
                                // Set CronJob Events
                                k8sCronJobRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.CRON_JOB.getValue(), Maps.newHashMap()).get(k8sCronJobRow.getName()));
                                List<K8sJobVO> jobsInCronJob = Optional.ofNullable(k8sCronJobRow).map(K8sCronJobVO::getDetail).map(K8sCronJobDetailVO::getActiveJobs).orElseGet(() -> Lists.newArrayList());
                                for (K8sJobVO k8sJobRow : jobsInCronJob) {
                                    // Set Jobs Events in CronJob
                                    k8sJobRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.JOB.getValue(), Maps.newHashMap()).get(k8sJobRow.getName()));
                                }
                                // Set Jobs From Cronjob
                                k8sControllers.addAllJobsItem(jobsInCronJob);

                                currentJobs.addAll(jobsInCronJob);
                            }

                            /** 3. Add Pods **/
                            for (K8sJobVO k8sJobRow : Optional.ofNullable(currentJobs).orElseGet(() -> Lists.newArrayList())) {
                                addonCommonService.addPod(pods, podMap, k8sJobRow.getName());
                            }

                            /** 4. Set CronJobs. **/
                            k8sControllers.addAllCronJobsItem(k8sCronJobs);
                            break;
                        }
                        case JOB: {
                            /** 1. Get Jobs **/
                            List<K8sJobVO> k8sJobs = new ArrayList<>(Collections.singletonList((K8sJobVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));
                            /** 2. Set Events and Add Pods**/
                            for (K8sJobVO k8sJobRow : Optional.ofNullable(k8sJobs).orElseGet(() -> Lists.newArrayList())) {
                                k8sJobRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.JOB.getValue(), Maps.newHashMap()).get(k8sJobRow.getName()));
                                addonCommonService.addPod(pods, podMap, k8sJobRow.getName());
                            }
                            /** 3. Set Jobs **/
                            k8sControllers.addAllJobsItem(k8sJobs);
                            break;
                        }
                        case SERVICE: {
                            /** 1. Get Services **/
                            List<K8sServiceVO> svcList = new ArrayList<>(Collections.singletonList((K8sServiceVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));
                            if (CollectionUtils.isNotEmpty(svcList)) {
                                services.addAll(svcList);
                            }
                            break;
                        }
                        case INGRESS: {
                            /** 1. Get Ingresses **/
                            List<K8sIngressVO> ingressList = new ArrayList<>(Collections.singletonList((K8sIngressVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));
                            if (CollectionUtils.isNotEmpty(ingressList)) {
                                ingresses.addAll(ingressList);
                            }
                            break;
                        }
                        case CONFIG_MAP: {
                            /** 1. Get ConfigMaps **/
                            List<ConfigMapGuiVO> configMapList = new ArrayList<>(Collections.singletonList((ConfigMapGuiVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));
                            if (CollectionUtils.isNotEmpty(configMapList)) {
                                configMaps.addAll(configMapList);
                            }
                            break;
                        }
                        case SECRET: {
                            /** 1. Get Secrets **/
                            List<SecretGuiVO> secretList = new ArrayList<>(Collections.singletonList((SecretGuiVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));
                            if (CollectionUtils.isNotEmpty(secretList)) {
                                secrets.addAll(secretList);
                            }
                            break;
                        }
                        case PERSISTENT_VOLUME_CLAIM: {
                            List<K8sPersistentVolumeClaimVO> pvcList = new ArrayList<>(Collections.singletonList((K8sPersistentVolumeClaimVO)k8sObjMap.get(k8sObj.getK8sApiKindType()).get(k8sObj.getNamespace()).get(k8sObj.getName())));
                            persistentVolumeClaims.addAll(pvcList);
                            break;
                        }
//                  현재 화면에서 보여주는 곳이 없음.. 다음 스텝에서 지원..
//
//                    case PERSISTENT_VOLUME: {
//                        break;
//                    }
//                    case STORAGE_CLASS: {
//                        break;
//                    }
                        default: {
                            /** TODO: Support 되지 않는 Type은 Error Logging 처리하여 향후 지원하도록 한다. **/
                            if (log.isDebugEnabled()) {
                                log.warn("Unsupported Resources : " + k8sObj.getK8sApiKindType().getValue());
                            }
                            break;
                        }
                    } // end of switch(k8sObj.getK8sApiKindType()) {
                } // end of for(K8sObjectMapVO k8sObj : Optional.ofNullable(k8sObjectList).orElseGet(() ->Lists.newArrayList())) {
            }

            /**
             * 조회된 리소스별 셋팅
             */
            helmResources.setControllers(k8sControllers);
            helmResources.setConfigMaps(configMaps);
            helmResources.setIngresses(ingresses);
            helmResources.setPods(pods);
            helmResources.setSecrets(secrets);
            helmResources.setServices(services);
            helmResources.setVolumes(persistentVolumeClaims);

            /**
             * Response!!
             */
            return helmResources;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            return null;
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            return null;
        }
    }

    public List<K8sObjectMapVO> getK8sObjectList(String manifest, String defaultNamespace) throws Exception {
        List<K8sObjectMapVO> k8sObjectList = new ArrayList<>();
//            List<Object> objs = ServerUtils.getYamlObjects(deployedManifest);
        // Custom Resource가 포함되어 있으면 오류.. SnakeYaml로 직접 Parsing
        Iterable<Object> iterable = Yaml.getSnakeYaml().loadAll(manifest);
        List<Object> objs = new ArrayList();
        Iterator iterator = iterable.iterator();
        while(iterator.hasNext()) {
            Object object = iterator.next();
            if (object != null) {
                objs.add(object);
            }
        }

        if (CollectionUtils.isNotEmpty(objs)) {
            for (Object obj : objs) {
                Map<String, Object> k8sObjMap = ServerUtils.getK8sObjectToMap(obj, null);
                K8sApiKindType kind = K8sApiKindType.findKindTypeByValue(MapUtils.getString(k8sObjMap, "kind"));
                if (kind != null) { // Acloud에서 관리되는 리소스 유형..
                    K8sObjectMapVO k8sObjectMapVO = new K8sObjectMapVO();
                    Map<String, Object> meta = (Map<String, Object>) MapUtils.getMap(k8sObjMap, KubeConstants.META, null);
                    if (meta == null ||
                        MapUtils.getString(meta, KubeConstants.NAME, null) == null ||
//                            MapUtils.getString(meta, KubeConstants.META_NAMESPACE, null) == null ||   //Namespace가 존재하지 않는 케이스가 있음!!
                        MapUtils.getString(k8sObjMap, KubeConstants.APIVSERION) == null) {
                        continue; // 필수 데이터가 없으면 Skip...
                    }
                    k8sObjectMapVO.setK8sApiKindType(kind);
                    k8sObjectMapVO.setApiVersion(MapUtils.getString(k8sObjMap, KubeConstants.APIVSERION));
                    k8sObjectMapVO.setName(MapUtils.getString(meta, KubeConstants.NAME));

                    String namespaceFromMeta = MapUtils.getString(meta, KubeConstants.META_NAMESPACE);
                    if(StringUtils.isNotBlank(defaultNamespace)) {
                        if (StringUtils.isBlank(namespaceFromMeta)) {
                            k8sObjectMapVO.setNamespace(defaultNamespace);
                        }
                        else {
                            if (defaultNamespace.equals(namespaceFromMeta)) {
                                k8sObjectMapVO.setNamespace(defaultNamespace);
                            }
                            else {
                                if (log.isDebugEnabled()) {
                                    // Test를 위해 Debug 모드일때는 Manifest의 Namespace와 패키지가 설치된 Namespace가 서로 다를 경우 오류를 던짐..
                                    // cocktail-addon Namespace에 배포되는 addon중.. kube-system Namespace에 리소스를 생성하는 케이스가 존재하므로 error log만 남김...
                                    if(StringUtils.equals(defaultNamespace, KubeConstants.COCKTAIL_ADDON_NAMESPACE)) {
                                        log.error(String.format("Different Namespace [ %s : %s ]", namespaceFromMeta, defaultNamespace), ExceptionType.NamespaceNameInvalid);
                                    }
                                    else {
                                        log.error(String.format("Different Namespace [ %s : %s ]", namespaceFromMeta, defaultNamespace), ExceptionType.NamespaceNameInvalid);
//                                        throw new CocktailException(String.format("Different Namespace [ %s : %s ]", namespaceFromMeta, defaultNamespace), ExceptionType.NamespaceNameInvalid);
                                    }
                                }
                                k8sObjectMapVO.setNamespace(namespaceFromMeta);
                            }
                        }
                    } else {
                        k8sObjectMapVO.setNamespace(namespaceFromMeta);
                    }

                    k8sObjectMapVO.setK8sObj(obj);
                    k8sObjectMapVO.setK8sObjMap(k8sObjMap);
                    k8sObjectList.add(k8sObjectMapVO);
                }
            }
        }

        return k8sObjectList;
    }
    /**
     * Generate Persistent Volume Claim List
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param persistentVolumesMap
     * @param storageClassMap
     * @return
     * @throws Exception
     */
    private List<K8sPersistentVolumeClaimVO> genPersistentVolumeClaims(ClusterVO cluster, String namespace, String field, String label,
                                                                       Map<String, K8sPersistentVolumeVO> persistentVolumesMap,
                                                                       Map<String, K8sStorageClassVO> storageClassMap) throws Exception
    {
        /** 1. Get Persistent Volume Claims **/
        List<K8sPersistentVolumeClaimVO> pvcList = persistentVolumeService.getPersistentVolumeClaims(cluster, namespace, field, label, ContextHolder.exeContext());
        /** 1-1. Get Persistent Volume **/
        if (MapUtils.isEmpty(persistentVolumesMap)) {
            persistentVolumesMap = persistentVolumeService.getPersistentVolumesMap(cluster, namespace, null, null, ContextHolder.exeContext());
        }
        /** 1-2. Get StorageClass **/
        if (MapUtils.isEmpty(storageClassMap)) {
            storageClassMap = storageClassService.convertStorageClassDataMap(cluster, null, null, ContextHolder.exeContext());
        }

        /** 2. Set PersistentVolume And StorageClass **/
        for (K8sPersistentVolumeClaimVO k8sPersistentVolumeClaimRow : Optional.ofNullable(pvcList).orElseGet(() ->Lists.newArrayList())) {
            // Set PersistentVolume
            k8sPersistentVolumeClaimRow.setPersistentVolume(persistentVolumesMap.get(k8sPersistentVolumeClaimRow.getVolumeName()));
            // Set StorageClass
            k8sPersistentVolumeClaimRow.setStorageClass(storageClassMap.get(k8sPersistentVolumeClaimRow.getStorageClassName()));
        }

        return pvcList;
    }

    /**
     * Get K8sControllers Object
     * @param k8sControllers
     * @return
     * @throws Exception
     */
    private K8sControllersVO getK8sControllers(K8sControllersVO k8sControllers) throws Exception {
        if(k8sControllers == null) {
            k8sControllers = new K8sControllersVO();
        }

        return k8sControllers;
    }

    /**
     * Add Deployments To K8sControllers
     * @param k8sControllers
     * @param deployments
     * @return
     */
    private K8sControllersVO setDeployments(K8sControllersVO k8sControllers, List<K8sDeploymentVO> deployments) throws Exception {
        if(CollectionUtils.isNotEmpty(deployments)) {
            k8sControllers = this.getK8sControllers(k8sControllers);
            if (CollectionUtils.isEmpty(k8sControllers.getDeployments())) {
                k8sControllers.setDeployments(deployments);
            }
            else {
                k8sControllers.getDeployments().addAll(deployments);
            }
        }
        return k8sControllers;
    }

    /**
     * Add ReplicaSets To K8sControllers
     * @param k8sControllers
     * @param replicaSets
     * @return
     * @throws Exception
     */
    private K8sControllersVO setReplicaSets(K8sControllersVO k8sControllers, List<K8sReplicaSetVO> replicaSets) throws Exception {
        if(CollectionUtils.isNotEmpty(replicaSets)) {
            k8sControllers = this.getK8sControllers(k8sControllers);
            if (CollectionUtils.isEmpty(k8sControllers.getReplicaSets())) {
                k8sControllers.setReplicaSets(replicaSets);
            }
            else {
                k8sControllers.getReplicaSets().addAll(replicaSets);
            }
        }
        return k8sControllers;
    }

    /**
     * Add StatefulSets To K8sControllers
     * @param k8sControllers
     * @param statefulSets
     * @return
     * @throws Exception
     */
    private K8sControllersVO setStatefulSets(K8sControllersVO k8sControllers, List<K8sStatefulSetVO> statefulSets) throws Exception {
        if(CollectionUtils.isNotEmpty(statefulSets)) {
            k8sControllers = this.getK8sControllers(k8sControllers);
            if (CollectionUtils.isEmpty(k8sControllers.getStatefulSets())) {
                k8sControllers.setStatefulSets(statefulSets);
            }
            else {
                k8sControllers.getStatefulSets().addAll(statefulSets);
            }
        }
        return k8sControllers;
    }

    /**
     * Add DaemonSets To K8sControllers
     * @param k8sControllers
     * @param daemonSets
     * @return
     * @throws Exception
     */
    private K8sControllersVO setDaemonSets(K8sControllersVO k8sControllers, List<K8sDaemonSetVO> daemonSets) throws Exception {
        if(CollectionUtils.isNotEmpty(daemonSets)) {
            k8sControllers = this.getK8sControllers(k8sControllers);
            if (CollectionUtils.isEmpty(k8sControllers.getDaemonSets())) {
                k8sControllers.setDaemonSets(daemonSets);
            }
            else {
                k8sControllers.getDaemonSets().addAll(daemonSets);
            }
        }
        return k8sControllers;
    }

    /**
     * Add Cronjobs To K8sControllers
     * @param k8sControllers
     * @param cronJobs
     * @return
     * @throws Exception
     */
    private K8sControllersVO setCronJobs(K8sControllersVO k8sControllers, List<K8sCronJobVO> cronJobs) throws Exception {
        if(CollectionUtils.isNotEmpty(cronJobs)) {
            k8sControllers = this.getK8sControllers(k8sControllers);
            if (CollectionUtils.isEmpty(k8sControllers.getCronJobs())) {
                k8sControllers.setCronJobs(cronJobs);
            }
            else {
                k8sControllers.getCronJobs().addAll(cronJobs);
            }
        }
        return k8sControllers;
    }

    /**
     * Add Jobs To K8sControllers
     * @param k8sControllers
     * @param jobs
     * @return
     * @throws Exception
     */
    private K8sControllersVO setJobs(K8sControllersVO k8sControllers, List<K8sJobVO> jobs) throws Exception {
        if(CollectionUtils.isNotEmpty(jobs)) {
            k8sControllers = this.getK8sControllers(k8sControllers);
            if (CollectionUtils.isEmpty(k8sControllers.getJobs())) {
                k8sControllers.setJobs(jobs);
            }
            else {
                k8sControllers.getJobs().addAll(jobs);
            }
        }
        return k8sControllers;
    }

}
