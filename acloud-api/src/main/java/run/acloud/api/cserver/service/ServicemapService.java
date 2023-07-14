package run.acloud.api.cserver.service;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.enums.GateWayNameType;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.service.ClusterVolumeService;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.dao.IServicemapGroupMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.dao.IWorkloadGroupMapper;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.event.service.EventService;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.ClusterType;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.KubeServiceTypes;
import run.acloud.api.resource.enums.SecretType;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 1. 12.
 */
@Slf4j
@Service
public class ServicemapService {

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private ServiceSpecService serviceSpecService;

	@Autowired
	private PersistentVolumeService persistentVolumeService;

	@Autowired
    private EventService eventService;

    @Autowired
    private RegistryPropertyService registry;

	@Autowired
	private ClusterStateService clusterStateService;

	@Autowired
	private ClusterVolumeService clusterVolumeService;

	@Autowired
	private ServerStateService serverStateService;

	@Autowired
	private LimitRangeService limitRangeService;

	@Autowired
	private ResourceQuotaService resourceQuotaService;

	@Autowired
	private NetworkPolicyService networkPolicyService;

	@Autowired
	private IngressSpecService ingressSpecService;

	@Autowired
	private SecretService secretService;

	@Autowired
	private NamespaceService namespaceService;

	@Autowired
	private WorkloadResourceService workloadResourceService;

	@Autowired
	private CRDResourceService crdService;

	@Autowired
	private AccountService accountService;

	public List<ServicemapSummaryAdditionalVO> getServicemapSummaries(List<Integer> serviceSeqs, boolean useStorage, boolean useGateWay, boolean useWorkload, boolean useNamespace) throws Exception {
		return this.getServicemapSummaries(null, serviceSeqs, useStorage, useGateWay, useGateWay, false, useWorkload, useNamespace);
	}

	public List<ServicemapSummaryAdditionalVO> getServicemapSummaries(Integer accountSeq, boolean useStorage, boolean useGateWay, boolean useWorkload, boolean useNamespace) throws Exception {
		return this.getServicemapSummaries(accountSeq, null, useStorage, useGateWay, useGateWay, false, useWorkload, useNamespace);
	}

    public List<ServicemapSummaryAdditionalVO> getServicemapSummaries(Integer accountSeq, List<Integer> serviceSeqs, boolean useStorage, boolean useService, boolean useIngress, boolean useIngressHost, boolean useWorkload, boolean useNamespace) throws Exception {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

		List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, null, null, serviceSeqs, null, "Y");
		List<ServicemapSummaryVO> servicemapSummaries = servicemapDao.getServicemapSummaries(accountSeq, null, serviceSeqs, null);
		List<ServicemapSummaryAdditionalVO> servicemapSummaryAdditionals = Lists.newArrayList();
		// appmap(namespace) 별로 loadBalancer 유형의 서비스 수 셋팅
		if (CollectionUtils.isNotEmpty(servicemapSummaries) && CollectionUtils.isNotEmpty(clusters)){
			// Map<clusterSeq, List<ServicemapSummaryVO>>
			Map<Integer, List<ServicemapSummaryVO>> servicemapSummariesMap = Maps.newHashMap();
			for (ServicemapSummaryVO smsRow : servicemapSummaries) {
				if (!servicemapSummariesMap.containsKey(smsRow.getClusterSeq())) {
					servicemapSummariesMap.put(smsRow.getClusterSeq(), Lists.newArrayList());
				}
				servicemapSummariesMap.get(smsRow.getClusterSeq()).add(smsRow);

				// ServicemapSummaryVO -> ServicemapSummaryAdditionalVO
				ServicemapSummaryAdditionalVO smsaTarget = new ServicemapSummaryAdditionalVO();
				BeanUtils.copyProperties(smsRow, smsaTarget);
				servicemapSummaryAdditionals.add(smsaTarget);
			}

			List<ClusterVolumeVO> clusterVolumes = null;
			// Map<clusterSeq, List<ClusterVolumeVO>>
			Map<Integer, List<ClusterVolumeVO>> clusterVolumesMap = Maps.newHashMap();
			if (useStorage) {
				clusterVolumes = clusterVolumeService.getStorageVolumes(accountSeq, serviceSeqs, null, null, null, false, false);
				if (CollectionUtils.isNotEmpty(clusterVolumes)) {
					for (ClusterVolumeVO cvRow : clusterVolumes) {
						if (!clusterVolumesMap.containsKey(cvRow.getClusterSeq())) {
							clusterVolumesMap.put(cvRow.getClusterSeq(), Lists.newArrayList());
						}
						clusterVolumesMap.get(cvRow.getClusterSeq()).add(cvRow);
					}
				}
			}

			Map<Integer, ClusterDetailVO> clusterMap = new HashMap();
			/** Map<clusterSeq, Map<namespace, Map<GateWayNameType, GateWayCount>>> 형태로 GateWay 서비스를 셋팅 */
			Map<Integer, Map<String, Map<GateWayNameType, Integer>>> k8sServicesMap = new HashMap<>();
			/** Map<clusterSeq, Map<namespace, Map<clusterVolumeName, ClusterVolumeCapacityVO>> 형태로 PVC 의 총 요청량을 셋팅 */
			Map<Integer, Map<String, Map<String, ClusterVolumeCapacityVO>>> k8sVolumeRequestMap = new HashMap<>();
			/** Map<clusterSeq, Map<namespace, List<K8sIngressVO>> 형태로 Ingress host 정보 셋팅 */
			Map<Integer, Map<String, List<K8sIngressVO>>> k8sIngressMap = new HashMap<>();
			/** Map<clusterSeq, Map<namespace, Set<workload>>> */
            Map<Integer, Map<String, Set<String>>> workloadMap = new HashMap<>();
			/** Map<clusterSeq, Set<namespace>> */
            Map<Integer, Set<String>> namespaceMap = new HashMap<>();

			for (ClusterVO clusterRow : clusters){
				// ClusterVO -> ClusterDetailVO
				ClusterDetailVO clusterDetail = new ClusterDetailVO();
				BeanUtils.copyProperties(clusterRow, clusterDetail);
				clusterMap.put(clusterRow.getClusterSeq(), clusterDetail);

				if (clusterStateService.isClusterRunning(clusterRow)){
					// Servicemap에서 해당 클러스터로 생성된 것이 존재하는 지 확인
					List<ServicemapSummaryVO> servicemapSummaryOfCluster = servicemapSummariesMap.get(clusterRow.getClusterSeq());

					// 존재한다면
					if (CollectionUtils.isNotEmpty(servicemapSummaryOfCluster)){
						if (useService || useIngress || useIngressHost) {
							Set<String> namespaces = new HashSet<>();
							for (ServicemapSummaryVO smsRow : servicemapSummaryOfCluster) {
								namespaces.add(smsRow.getNamespaceName());
							}
							/**
							 * GateWay(Service(LB, NodePort, ClusterIP), Ingress) 서비스
							 */
							if (useService || useIngress) {
								try {
									Map<String, Map<GateWayNameType, Integer>> namespacedGateWayMap = this.getNamespacedClusterGateWays(clusterRow, namespaces, useService, useIngress, ContextHolder.exeContext());
									if (MapUtils.isNotEmpty(namespacedGateWayMap)) {
										k8sServicesMap.put(clusterRow.getClusterSeq(), namespacedGateWayMap);
									}
								} catch (Exception e) {
									log.error("appmap summary - GateWay fail!!", e);
								}
							}
							/**
							 * Ingress Host 정보
							 */
							if (useIngressHost) {
								try {
									// Ingress 조회
									List<K8sIngressVO> ingressList = ingressSpecService.getIngresses(clusterRow, null, null, ContextHolder.exeContext());

									if(CollectionUtils.isNotEmpty(ingressList)) {
										Map<String, List<K8sIngressVO>> k8sNamespaceIngressMap = new HashMap<>();
										for (K8sIngressVO ingressRow : ingressList) {
											if (MapUtils.getObject(k8sNamespaceIngressMap, ingressRow.getNamespace(), null) == null) {
												k8sNamespaceIngressMap.put(ingressRow.getNamespace(), new ArrayList<>());
											}
											k8sNamespaceIngressMap.get(ingressRow.getNamespace()).add(ingressRow);
										}
										k8sIngressMap.put(clusterRow.getClusterSeq(), k8sNamespaceIngressMap);
									}
								} catch (Exception e) {
									log.error("appmap summary - Ingresses fail!!", e);
								}
							}
						}

						/**
						 * PVC 조회
						 */
						if (useStorage) {
							if (CollectionUtils.isNotEmpty(clusterVolumes)){
								List<ClusterVolumeVO> volumesOfCluster = clusterVolumesMap.get(clusterRow.getClusterSeq());

								if(CollectionUtils.isNotEmpty(volumesOfCluster)){
									try {
										List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(clusterRow, "", null, null, ContextHolder.exeContext());
										Map<String, K8sPersistentVolumeVO> persistentVolumeMap = persistentVolumeService.convertPersistentVolumeDataMap(clusterRow, null, null, null);

										Map<String, Map<String, ClusterVolumeCapacityVO>> clusterVolumeRequestMap = clusterVolumeService.getNamespacedTotalCapacityByStorage(volumesOfCluster, persistentVolumeClaims, persistentVolumeMap, ContextHolder.exeContext());
										k8sVolumeRequestMap.put(clusterRow.getClusterSeq(), clusterVolumeRequestMap);
									} catch (Exception e) {
										log.error("appmap summary - pvc, pv fail!!", e);
									}
								}
							}
						}

						/**
						 * Workload 조회
						 */
						if (useWorkload) {
                            try {
								ComponentDetailsVO componentDetails = serverStateService.getComponentDetails(clusterRow, null, null, false, ContextHolder.exeContext());

								if (componentDetails != null && CollectionUtils.isNotEmpty(componentDetails.getComponents())) {
									workloadMap.put(clusterRow.getClusterSeq(), Maps.newHashMap());
									for (ComponentVO componentRow : componentDetails.getComponents()) {
										if (MapUtils.getObject(workloadMap.get(clusterRow.getClusterSeq()), componentRow.getNamespaceName(), null) == null) {
											workloadMap.get(clusterRow.getClusterSeq()).put(componentRow.getNamespaceName(), Sets.newHashSet());
										}
										workloadMap.get(clusterRow.getClusterSeq()).get(componentRow.getNamespaceName()).add(componentRow.getComponentName());
									}
								}

                            } catch (Exception e) {
                                log.error("appmap summary - workload fail!!", e);
                            }
                        }

						/**
						 * Namespace 조회
						 */
						if (useNamespace) {
							try {
								List<K8sNamespaceVO> k8sNamespaces = namespaceService.getNamespacesToList(clusterRow, null, null, ContextHolder.exeContext());

								if (CollectionUtils.isNotEmpty(k8sNamespaces)) {
									namespaceMap.put(clusterRow.getClusterSeq(), Sets.newHashSet());
									for (K8sNamespaceVO namespaceRow : k8sNamespaces) {
										namespaceMap.get(clusterRow.getClusterSeq()).add(namespaceRow.getName());
									}
								}
 							} catch (Exception e) {
								log.error("appmap summary - namespace fail!!", e);
							}
						}
					}
				}
			}
			// appmap 별로 loadBalancer 유형의 서비스 수 셋팅
			int gateWayCount;
			int volumeRequestCapacity;
			for (ServicemapSummaryAdditionalVO sscRow : servicemapSummaryAdditionals) {
				gateWayCount = 0;
				volumeRequestCapacity = 0;

				if (useService || useIngress) {
					if (MapUtils.isNotEmpty(k8sServicesMap)
							&& MapUtils.getObject(k8sServicesMap, sscRow.getClusterSeq(), null) != null
							&& MapUtils.getObject(k8sServicesMap.get(sscRow.getClusterSeq()), sscRow.getNamespaceName(), null) != null
					) {
						for (Map.Entry<GateWayNameType, Integer> gateWayNameTypeRow : k8sServicesMap.get(sscRow.getClusterSeq()).get(sscRow.getNamespaceName()).entrySet()) {
							if (gateWayNameTypeRow.getKey() != GateWayNameType.CLUSTER_IP) {
								gateWayCount += gateWayNameTypeRow.getValue().intValue();
							}
							switch (gateWayNameTypeRow.getKey()) {
								case LOAD_BALANCER:
									sscRow.setLoadBalancerCount(gateWayNameTypeRow.getValue().intValue());
									break;
								case NODE_PORT:
									sscRow.setNodePortCount(gateWayNameTypeRow.getValue().intValue());
									break;
								case INGRESS:
									if (useIngress) {
										sscRow.setIngressCount(gateWayNameTypeRow.getValue().intValue());
									}
									break;
								case CLUSTER_IP:
									sscRow.setClusterIpCount(gateWayNameTypeRow.getValue().intValue());
									break;
							}
						}
					}
				}
				sscRow.setGateWayCount(gateWayCount);

				if (useIngressHost) {
					if (MapUtils.isNotEmpty(k8sIngressMap)
							&& MapUtils.getObject(k8sIngressMap, sscRow.getClusterSeq(), null) != null
							&& MapUtils.getObject(k8sIngressMap.get(sscRow.getClusterSeq()), sscRow.getNamespaceName(), null) != null
					) {
						sscRow.setIngressHostInfos(new ArrayList<>());
						for (K8sIngressVO ingressRow : k8sIngressMap.get(sscRow.getClusterSeq()).get(sscRow.getNamespaceName())) {
							if (CollectionUtils.isNotEmpty(ingressRow.getIngressSpec().getIngressRules())) {
								for (IngressRuleVO irRow : ingressRow.getIngressSpec().getIngressRules()) {
									IngressHostInfoVO ingressHostInfo = new IngressHostInfoVO();
									ingressHostInfo.setClusterSeq(sscRow.getClusterSeq());
									ingressHostInfo.setClusterName(sscRow.getClusterName());
									ingressHostInfo.setClusterId(sscRow.getClusterId());
									ingressHostInfo.setServicemapSeq(sscRow.getServicemapSeq());
									ingressHostInfo.setServicemapName(sscRow.getServicemapName());
									ingressHostInfo.setNamaespaceName(sscRow.getNamespaceName());
//									ingressHostInfo.setHostName(StringUtils.isBlank(irRow.getHostName()) ? StringUtils.replaceAll(clusterMap.get(sscRow.getClusterSeq()).getIngressHost(), "https?://", "") : irRow.getHostName());
									ingressHostInfo.setHostName(irRow.getHostName());
									ingressHostInfo.setIngressName(ingressRow.getName());

									sscRow.getIngressHostInfos().add(ingressHostInfo);
								}
							}
						}
					}
				}

				if (useStorage) {
					if (MapUtils.isNotEmpty(k8sVolumeRequestMap)
							&& MapUtils.getObject(k8sVolumeRequestMap, sscRow.getClusterSeq(), null) != null
							&& MapUtils.getObject(k8sVolumeRequestMap.get(sscRow.getClusterSeq()), sscRow.getNamespaceName(), null) != null) {

						for (Map.Entry<String, ClusterVolumeCapacityVO> volumeCapacityRow : k8sVolumeRequestMap.get(sscRow.getClusterSeq()).get(sscRow.getNamespaceName()).entrySet()) {
							if (volumeCapacityRow.getValue() != null) {
								volumeRequestCapacity += volumeCapacityRow.getValue().getAllocatedCapacity();
							}
						}
					}
				}
				sscRow.setVolumeRequestCapacity(volumeRequestCapacity);

				if (useWorkload) {
					if (MapUtils.isNotEmpty(workloadMap)
							&& MapUtils.getObject(workloadMap, sscRow.getClusterSeq(), null) != null
							&& MapUtils.getObject(workloadMap.get(sscRow.getClusterSeq()), sscRow.getNamespaceName(), null) != null
					) {
						sscRow.setServerCount(workloadMap.get(sscRow.getClusterSeq()).get(sscRow.getNamespaceName()).size());
					}
				}

				if (useNamespace) {
					if (MapUtils.isNotEmpty(namespaceMap)
							&& MapUtils.getObject(namespaceMap, sscRow.getClusterSeq(), null) != null
					) {
						if (namespaceMap.get(sscRow.getClusterSeq()).contains(sscRow.getNamespaceName())) {
							sscRow.setK8sResourceExists(true);
						} else {
							sscRow.setK8sResourceExists(false);
						}
					}
				}
			}
		}

        return servicemapSummaryAdditionals;
    }

	public ServicemapVO addServicemap(ServicemapAddVO servicemapAdd, ExecutingContextVO context) throws Exception {
    	return this.addServicemap(servicemapAdd, null, context);
	}

    @Transactional(transactionManager = "transactionManager")
    public ServicemapVO addServicemap(ServicemapAddVO servicemapAdd, List<String> workloadGroupNames, ExecutingContextVO context) throws Exception {

		// check validation
		ExceptionMessageUtils.checkParameterRequired("clusterSeq", servicemapAdd.getClusterSeq());
		ExceptionMessageUtils.checkParameter("namespaceName", servicemapAdd.getNamespaceName(), 50, true);
		ExceptionMessageUtils.checkParameter("servicemapName", servicemapAdd.getServicemapName(), 50, true);

		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);

		ClusterVO cluster = clusterDao.getCluster(servicemapAdd.getClusterSeq());

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(cluster);

		try {
			if(StringUtils.isNotBlank(servicemapAdd.getNamespaceName())){
				String fieldSelector = String.format("metadata.name=%s", servicemapAdd.getNamespaceName());
				List<K8sNamespaceVO> namespaces = namespaceService.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, context);

				if(CollectionUtils.isNotEmpty(namespaces)){
					throw new CocktailException(String.format("Already exists Namespace - [%s]!!", servicemapAdd.getNamespaceName()), ExceptionType.NamespaceAlreadyExists);
				}
			}else{
				String namespaceName = ResourceUtil.makeNamespaceName(servicemapAdd.getServicemapSeq(), cluster.getClusterSeq());
				servicemapAdd.setNamespaceName(namespaceName);
			}
			log.debug("namespace: {}", servicemapAdd.getNamespaceName());
			namespaceService.createNamespace(cluster, servicemapAdd.getNamespaceName(), context);

			/**
			 * Nemespace > LimitRange, ResourceQuota, NetworkPolicy 생성 (dryRun)
			 */
			if (ResourceUtil.isSupportedDryRun(cluster.getK8sVersion())) {
				this.createNemespacedResource(cluster, servicemapAdd, true, context);
			}

			// create servicemap
			servicemapAdd.setCreator(context.getUserSeq());
			servicemapAdd.setUseYn("Y");
			servicemapDao.addServicemap(servicemapAdd);

			if (servicemapAdd.getServiceSeq() != null) {
				// service(workspace), servicemap Mapping
				ServicemapMappingVO smm = new ServicemapMappingVO();
				smm.setServiceSeq(servicemapAdd.getServiceSeq());
				smm.setServicemapSeq(servicemapAdd.getServicemapSeq());
				smm.setCreator(context.getUserSeq());
				servicemapDao.addServiceServicemapMapping(smm);

				// servicemapgroup, service Mapping
				ServicemapGroupMappingVO smgm = new ServicemapGroupMappingVO();
				if (servicemapAdd.getServicemapGroupSeq() != null) {
					smgm.setServicemapGroupSeq(servicemapAdd.getServicemapGroupSeq());
				} else {
					IServicemapGroupMapper servicemapGroupDao = sqlSession.getMapper(IServicemapGroupMapper.class);
					List<ServicemapGroupVO> servicemapGroups = Optional.ofNullable(servicemapGroupDao.getServicemapGroupsOfService(servicemapAdd.getServiceSeq())).orElseGet(Lists::newArrayList);
					smgm.setServicemapGroupSeq(servicemapGroups.get(0).getServicemapGroupSeq());
				}
				smgm.setServicemapSeq(servicemapAdd.getServicemapSeq());
				smgm.setCreator(context.getUserSeq());
				servicemapDao.addServicemapgroupServicemapMapping(smgm);
			}

			/**
			 * imagePullSecret 생성 (플랫폼 레지스트리 pull 사용자로 생성)
			 */
			// 레지스트리 pull user 생성 및 존재하는 레지스트리에 pull user member 추가
			accountService.createAccountRegistryPullUser(cluster.getAccount());

			String registryPullUserId = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId());
			String registryPullUserPassword = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserPassword());

			if (secretService.getSecret(cluster, servicemapAdd.getNamespaceName(), registryPullUserId) == null) {

				DockerRegistrySecretVO secret = new DockerRegistrySecretVO();
				secret.setMakeCocktail(false);
				secret.setType(SecretType.DockerRegistry);
				secret.setName(registryPullUserId);
				secret.setUserName(registryPullUserId);
				secret.setPassword(registryPullUserPassword);
				secret.setEmail(String.format("%s@%s", registryPullUserId, CommonConstants.DEFAULT_USER_DOMAIN));
				secret.setServerUrl(this.registry.getUrl());
				secretService.createDockerRegistrySecret(cluster, servicemapAdd.getNamespaceName(), secret);
			}

			if(CollectionUtils.isEmpty(workloadGroupNames)){
				workloadGroupNames = Arrays.asList(CommonConstants.DEFAULT_GROUP_NAME);
			}

			for(int i = 0, ie = workloadGroupNames.size(); i < ie; i++){
				WorkloadGroupAddVO workloadGroupAdd = new WorkloadGroupAddVO();
				workloadGroupAdd.setServicemapSeq(servicemapAdd.getServicemapSeq());
				workloadGroupAdd.setWorkloadGroupName(workloadGroupNames.get(i));
				workloadGroupAdd.setColumnCount(1);
				workloadGroupAdd.setSortOrder(i + 1);
				workloadGroupAdd.setUseYn("Y");
				workloadGroupAdd.setCreator(context.getUserSeq());

				workloadGroupDao.addWorkloadGroup(workloadGroupAdd);
			}

			/**
			 * Nemespace > LimitRange, ResourceQuota, NetworkPolicy 생성
			 */
			this.createNemespacedResource(cluster, servicemapAdd, false, context);

			if (servicemapAdd.getServiceSeq() != null) {
				try {
					this.eventService.getInstance().sendServices(servicemapAdd.getServiceSeq(), servicemapAdd.getServicemapSeq(), context);
				} catch (Exception e) {
					log.error("Error! event sendServices on addAppmap", e);
				}
			}
		} catch (Exception e) {
			boolean isExist = false;
			if (e instanceof CocktailException) {
				if (((CocktailException) e).getType() == ExceptionType.NamespaceAlreadyExists) {
					isExist = true;
				}
			}

			if (!isExist) {
				// rollback
				namespaceService.deleteNamespace(cluster, servicemapAdd.getNamespaceName(), context);
			}

			throw e;
		}

		return servicemapDao.getServicemap(servicemapAdd.getServicemapSeq(), servicemapAdd.getServiceSeq());
    }

	@Transactional(transactionManager = "transactionManager")
	public ServicemapVO addExistServicemap(ServicemapAddVO servicemapAdd, List<String> workloadGroupNames, ExecutingContextVO context) throws Exception {

		// check validation
		ExceptionMessageUtils.checkParameterRequired("clusterSeq", servicemapAdd.getClusterSeq());
		ExceptionMessageUtils.checkParameter("namespaceName", servicemapAdd.getNamespaceName(), 50, true);
		ExceptionMessageUtils.checkParameter("servicemapName", servicemapAdd.getServicemapName(), 50, true);

		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);

		List<WorkloadGroupVO> workloadGroups = new ArrayList<>();

		ClusterVO cluster = clusterDao.getCluster(servicemapAdd.getClusterSeq());

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(cluster);

		/**
		 * 존재하는 Namespace인지 확인.
		 */
		if(StringUtils.isNotBlank(servicemapAdd.getNamespaceName())){
			String fieldSelector = String.format("metadata.name=%s", servicemapAdd.getNamespaceName());
			List<K8sNamespaceVO> namespaces = namespaceService.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, context);

			if(CollectionUtils.isEmpty(namespaces)){
				throw new CocktailException(String.format("Namespace Not Found- [%s]!!", servicemapAdd.getNamespaceName()), ExceptionType.K8sNamespaceNotFound);
			} else {
				K8sNamespaceVO k8sNamespace = namespaces.get(0);
				this.patchNamespaceIstioLabel(cluster, k8sNamespace);
			}

			/**
			 * Nemespace > LimitRange, ResourceQuota, NetworkPolicy 생성 (dryRun)
			 */
			if (ResourceUtil.isSupportedDryRun(cluster.getK8sVersion())) {
				this.createNemespacedResource(cluster, servicemapAdd, true, context);
			}
		}

		ServicemapVO existServicemap = servicemapDao.getServicemapByClusterAndName(servicemapAdd.getClusterSeq(), servicemapAdd.getNamespaceName());
		if (existServicemap != null) {
			throw new CocktailException("Already exists servicemap!!", ExceptionType.ResourceAlreadyExists, ExceptionBiz.SERVICEMAP);
		}

		// create servicemap
		servicemapAdd.setCreator(context.getUserSeq());
		servicemapAdd.setUseYn("Y");
		servicemapDao.addServicemap(servicemapAdd);

		if (servicemapAdd.getServiceSeq() != null) {
			// service(workspace), servicemap Mapping
			ServicemapMappingVO smm = new ServicemapMappingVO();
			smm.setServiceSeq(servicemapAdd.getServiceSeq());
			smm.setServicemapSeq(servicemapAdd.getServicemapSeq());
			smm.setCreator(context.getUserSeq());
			servicemapDao.addServiceServicemapMapping(smm);

			if (servicemapAdd.getServicemapGroupSeq() != null) {
				// servicemapgroup, service Mapping
				ServicemapGroupMappingVO smgm = new ServicemapGroupMappingVO();
				smgm.setServicemapGroupSeq(servicemapAdd.getServicemapGroupSeq());
				smgm.setServicemapSeq(servicemapAdd.getServicemapSeq());
				smgm.setCreator(context.getUserSeq());
				servicemapDao.addServicemapgroupServicemapMapping(smgm);
			}
		}

		/**
		 * imagePullSecret 생성 (플랫폼 레지스트리 pull 사용자로 생성)
		 */
		// 레지스트리 pull user 생성 및 존재하는 레지스트리에 pull user member 추가
		accountService.createAccountRegistryPullUser(cluster.getAccount());

		String registryPullUserId = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId());
		String registryPullUserPassword = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserPassword());

		if (secretService.getSecret(cluster, servicemapAdd.getNamespaceName(), registryPullUserId) == null) {

			DockerRegistrySecretVO secret = new DockerRegistrySecretVO();
			secret.setMakeCocktail(false);
			secret.setType(SecretType.DockerRegistry);
			secret.setName(registryPullUserId);
			secret.setUserName(registryPullUserId);
			secret.setPassword(registryPullUserPassword);
			secret.setEmail(String.format("%s@%s", registryPullUserId, CommonConstants.DEFAULT_USER_DOMAIN));
			secret.setServerUrl(this.registry.getUrl());
			secretService.createDockerRegistrySecret(cluster, servicemapAdd.getNamespaceName(), secret);
		}

		/**
		 * Servicemap 내의 Default Group을 추가..
		 * 이걸 어떻게 사용할지는 고민 필요.. 만들지만 사용하지 않을 것 같으므로 제거해야할 수 있음.. (위 Docker Registry Secret도 마찬가지..)
		 */
		if(CollectionUtils.isEmpty(workloadGroupNames)){
			workloadGroupNames = Arrays.asList(CommonConstants.DEFAULT_GROUP_NAME);
		}

		for(int i = 0, ie = workloadGroupNames.size(); i < ie; i++){
			WorkloadGroupAddVO workloadGroupAdd = new WorkloadGroupAddVO();
			workloadGroupAdd.setServicemapSeq(servicemapAdd.getServicemapSeq());
			workloadGroupAdd.setWorkloadGroupName(workloadGroupNames.get(i));
			workloadGroupAdd.setColumnCount(1);
			workloadGroupAdd.setSortOrder(i + 1);
			workloadGroupAdd.setUseYn("Y");
			workloadGroupAdd.setCreator(context.getUserSeq());

			workloadGroupDao.addWorkloadGroup(workloadGroupAdd);
			workloadGroups.add(workloadGroupAdd);
		}

		/**
		 * Nemespace > LimitRange, ResourceQuota, NetworkPolicy 생성
		 */
		try {
			this.createNemespacedResource(cluster, servicemapAdd, false, context);
		} catch (Exception e) {
			// LimitRange
			if (servicemapAdd.getLimitRange() != null && CollectionUtils.isNotEmpty(servicemapAdd.getLimitRange().getLimits())) {
				// rollback
				limitRangeService.deleteLimitRange(cluster, servicemapAdd.getNamespaceName(), servicemapAdd.getLimitRange().getName(), context);
			}
			// ResourceQuota
			if (servicemapAdd.getResourceQuota() != null && MapUtils.isNotEmpty(servicemapAdd.getResourceQuota().getHard())) {
				// rollback
				resourceQuotaService.deleteResourceQuota(cluster, servicemapAdd.getNamespaceName(), servicemapAdd.getResourceQuota().getName(), context);
			}
			// NetworkPolicy
			if (servicemapAdd.getNetworkPolicyCreationType() != null && servicemapAdd.getNetworkPolicy() != null) {
				// rollback
				networkPolicyService.deleteNetworkPolicy(cluster, servicemapAdd.getNamespaceName(), servicemapAdd.getNetworkPolicy().getName(), context);
			}
			throw e;
		}

		if (servicemapAdd.getServiceSeq() != null) {
			try {
				this.eventService.getInstance().sendServices(servicemapAdd.getServiceSeq(), servicemapAdd.getServicemapSeq(), context);
			} catch (Exception e) {
				log.error("Error! event sendServices on addAppmap", e);
			}
		}

		return servicemapDao.getServicemap(servicemapAdd.getServicemapSeq(), servicemapAdd.getServiceSeq());
	}

	/**
	 * Labels에 istio-injection 키가 없다면 셋팅하도록 처리
	 *
	 * @param cluster
	 * @param k8sNamespace
	 * @throws Exception
	 */
	public void patchNamespaceIstioLabel(ClusterVO cluster, K8sNamespaceVO k8sNamespace) throws Exception {
		if (MapUtils.isEmpty(k8sNamespace.getLabels())) {
			k8sNamespace.setLabels(Maps.newHashMap());
		}

		if (MapUtils.getString(k8sNamespace.getLabels(), KubeConstants.LABELS_ISTIO_INJECTION_KEY, null) == null) {
			k8sNamespace.getLabels().put(KubeConstants.LABELS_ISTIO_INJECTION_KEY, KubeConstants.LABELS_ISTIO_INJECTION_VALUE_DISABLED);
			k8sNamespace.getDetail().setLabels(k8sNamespace.getLabels());

			// patch namespace (labels, annotations)
			namespaceService.patchNamespace(cluster, k8sNamespace.getName(), k8sNamespace.getLabels(), k8sNamespace.getAnnotations(), ContextHolder.exeContext());
		}
	}

	/**
	 * Nemespace > LimitRange, ResourceQuota, NetworkPolicy 생성
	 *
	 * @param cluster
	 * @param servicemapAdd
	 * @param dryRun
	 * @param context
	 * @throws Exception
	 */
	public void createNemespacedResource(ClusterVO cluster, ServicemapAddVO servicemapAdd, boolean dryRun, ExecutingContextVO context) throws Exception {
		try {
			boolean isExist = false;
			// LimitRange
			if (servicemapAdd.getLimitRange() != null && CollectionUtils.isNotEmpty(servicemapAdd.getLimitRange().getLimits())) {
				isExist = false;

				servicemapAdd.getLimitRange().setName(KubeConstants.LIMIT_RANGE_DEFAULT_NAME);
				servicemapAdd.getLimitRange().setNamespace(servicemapAdd.getNamespaceName());
				servicemapAdd.getLimitRange().setDefault(true);

				try {
					// check
					limitRangeService.checkLimitRange(cluster, servicemapAdd.getNamespaceName(), true, servicemapAdd.getLimitRange());
				} catch (Exception e) {
					if ( e instanceof CocktailException ) {
						if (((CocktailException) e).getType() == ExceptionType.LimitRangeNameAlreadyExists) {
							isExist = true;
						}
					}

					if (!isExist) {
						throw e;
					}
				}

				if (!isExist) {
					limitRangeService.createLimitRange(cluster, servicemapAdd.getLimitRange(), dryRun, context);
				}

			}

			// ResourceQuota
			if (servicemapAdd.getResourceQuota() != null && MapUtils.isNotEmpty(servicemapAdd.getResourceQuota().getHard())) {
				isExist = false;

				servicemapAdd.getResourceQuota().setName(KubeConstants.RESOURCE_QUOTA_DEFAULT_NAME);
				servicemapAdd.getResourceQuota().setNamespace(servicemapAdd.getNamespaceName());
				servicemapAdd.getResourceQuota().setDefault(true);

				try {
					// check
					resourceQuotaService.checkResourceQuota(cluster, servicemapAdd.getNamespaceName(), true, servicemapAdd.getResourceQuota());
				} catch (Exception e) {
					if ( e instanceof CocktailException ) {
						if (((CocktailException) e).getType() == ExceptionType.ResourceQuotaNameAlreadyExists) {
							isExist = true;
						}
					}

					if (!isExist) {
						throw e;
					}
				}

				if (!isExist) {
					resourceQuotaService.createResourceQuota(cluster, servicemapAdd.getResourceQuota(), dryRun, context);
				}
			}

			// NetworkPolicy
			if (servicemapAdd.getNetworkPolicyCreationType() != null) {
				isExist = false;

				// type에 맞게 NetworkPolicy spec 생성
				NetworkPolicyGuiVO gui = networkPolicyService.generateDefaultNetworkPolicyTemplateWithType(servicemapAdd.getNetworkPolicyCreationType(), servicemapAdd.getNamespaceName());
				servicemapAdd.setNetworkPolicy(gui);

				try {
					// check
					networkPolicyService.checkNetworkPolicy(cluster, servicemapAdd.getNamespaceName(), true, gui);
				} catch (Exception e) {
					if ( e instanceof CocktailException ) {
						if (((CocktailException) e).getType() == ExceptionType.NetworkPolicyNameAlreadyExists) {
							isExist = true;
						}
					}

					if (!isExist) {
						throw e;
					}
				}

				if (!isExist) {
					networkPolicyService.createNetworkPolicy(cluster, servicemapAdd.getNetworkPolicy(), dryRun, context);
				}

			}
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Namespace에 해당 resource가 존재하는 지 여부를 조회하여 리턴
	 *
	 * @param cluster
	 * @param namespaceName
	 * @return Map<resourceKindName, 존재여부>
	 * @throws Exception
	 */
	public Map<String, Boolean> getNamespacedDefaultResourceExists(ClusterVO cluster, String namespaceName) throws Exception {

		Map<String, Boolean> result = Maps.newHashMap();
		result.put(K8sApiKindType.LIMIT_RANGE.getValue(), Boolean.FALSE);
		result.put(K8sApiKindType.RESOURCE_QUOTA.getValue(), Boolean.FALSE);
		result.put(K8sApiKindType.NETWORK_POLICY.getValue(), Boolean.FALSE);

		if (cluster != null && StringUtils.isNotBlank(namespaceName)) {
			String label = String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
			if (CollectionUtils.isNotEmpty(limitRangeService.getLimitRanges(cluster, namespaceName, null, label, ContextHolder.exeContext()))) {
				result.put(K8sApiKindType.LIMIT_RANGE.getValue(), Boolean.TRUE);
			}
			if (CollectionUtils.isNotEmpty(resourceQuotaService.getResourceQuotas(cluster, namespaceName, null, label, ContextHolder.exeContext()))) {
				result.put(K8sApiKindType.RESOURCE_QUOTA.getValue(), Boolean.TRUE);
			}
			if (CollectionUtils.isNotEmpty(networkPolicyService.getNetworkPolicies(cluster, namespaceName, null, label, ContextHolder.exeContext()))) {
				result.put(K8sApiKindType.NETWORK_POLICY.getValue(), Boolean.TRUE);
			}

		}

		return result;
	}

	public ServerStateVO getWorkloadsInNamespace(Integer servicemapSeq, ExecutingContextVO context) throws Exception {
    	return this.getWorkloadsInNamespace(servicemapSeq, null, true, false, false, context);
	}

	public ServerStateVO getWorkloadsInNamespace(Integer servicemapSeq, boolean useExistingWorkload, ExecutingContextVO context) throws Exception {
    	return this.getWorkloadsInNamespace(servicemapSeq, null, true, useExistingWorkload, false, context);
	}

	public ServerStateVO getWorkloadsInNamespace(Integer servicemapSeq, String workloadName, ExecutingContextVO context) throws Exception {
		return this.getWorkloadsInNamespace(servicemapSeq, workloadName, true, false, false, context);
	}

	public ServerStateVO getWorkloadsInNamespace(Integer servicemapSeq, String workloadName, boolean canActualizeState, boolean useExistingWorkload, boolean usePodEventInfo, ExecutingContextVO context) throws Exception {

		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

		/** Appmap에서 사용중인 Cluster 정보 조회 **/
		ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);

		return serverStateService.getWorkloadsStateInNamespace(cluster, cluster.getNamespaceName(), workloadName, canActualizeState, useExistingWorkload, usePodEventInfo, context);
	}

	/**
	 * Servicemap 제거
	 *
	 * @param servicemapSeq
	 * @param cascade - true : Servicemap에 관련된 설정 정보(칵테일 설정 정보(DB)와 실제 k8s namespace까지 삭제), false : Servicemap에 관련된 설정 정보(칵테일 설정 정보(DB)만 삭제
	 * @param context
	 * @throws Exception
	 */
    @Transactional(transactionManager = "transactionManager")
    public void removeServicemap(Integer servicemapSeq, boolean cascade, ExecutingContextVO context) throws Exception {
    	IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
    	ServicemapVO servicemap = servicemapDao.getServicemap(servicemapSeq, null);

		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);
		String namespaceNotFoundMsg = "";

		if (cluster != null
				&& StringUtils.isNotBlank(cluster.getNamespaceName()) && cluster.getClusterType() == ClusterType.CUBE
				&& clusterStateService.isClusterRunning(cluster)
				&& cascade
		) {
			List<K8sPodVO> k8sPods = workloadResourceService.getPods(cluster, null, cluster.getNamespaceName(), null, 1, context);
			if (CollectionUtils.isNotEmpty(k8sPods)) {
				throw new CocktailException("Workloads exist in the namespace.", ExceptionType.ClusterHasComponent);
			}

			try {
				String fieldSelector = String.format("metadata.name=%s", cluster.getNamespaceName());
				List<K8sNamespaceVO> namespaces = namespaceService.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, context);

				if(CollectionUtils.isEmpty(namespaces)){
					namespaceNotFoundMsg = String.format("REMOVE_SERVICEMAP ##### - Namespace not found - [%s]!!", cluster.getNamespaceName());
					log.debug("{} {} - cluster: [{}, {}], servicemap: {}, namespace: {}, updater: [{}, {}]", "###############################", namespaceNotFoundMsg, cluster.getClusterSeq(), cluster.getClusterId(), servicemapSeq, cluster.getNamespaceName(), ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserRole());
					throw new CocktailException(namespaceNotFoundMsg, ExceptionType.K8sNamespaceNotFound);
				}else{
					namespaceService.deleteNamespace(cluster, cluster.getNamespaceName(), context);
				}
			} catch (Exception e) {
				if (e instanceof CocktailException) {
					if(((CocktailException) e).getType() != ExceptionType.K8sNamespaceNotFound){
						throw e;
					}
				} else {
					throw e;
				}
			}
		}

		int result = 0;
		String baseLogFormat = String.format("############################### REMOVE_SERVICEMAP ##### - cluster: [%d, %s], servicemap: %d, namespace: %s, updater: [%d, %s]"
				, Optional.ofNullable(cluster).map(ClusterVO::getClusterSeq).orElseGet(() ->0)
				, Optional.ofNullable(cluster).map(ClusterVO::getClusterId).orElseGet(() ->"")
				, servicemapSeq
				, Optional.ofNullable(cluster).map(ClusterVO::getNamespaceName).orElseGet(() ->"")
				, ContextHolder.exeContext().getUserSeq()
				, ContextHolder.exeContext().getUserRole());

		result = servicemapDao.removeComponentsByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removeComponents", result);

		result = servicemapDao.removeWorkloadGroupsByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removeWorkloadGroups", result);

		result = servicemapDao.removePipelineRunByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removePipelineRun", result);

		result = servicemapDao.removePipelineContainerByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removePipelineContainer", result);

		result = servicemapDao.removePipelineWorkloadByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removePipelineWorkload", result);

		// remove 서비스맵
		servicemap.setUpdater(context.getUserSeq());
		servicemapDao.removeServicemap(servicemap);

		// 맵핑 삭제 및 서비스맵그룹 sort 초기화 처리
		if (CollectionUtils.isNotEmpty(servicemap.getServicemapMappings())) {
			for (ServicemapMappingVO smmRow : servicemap.getServicemapMappings()) {
				if (smmRow.getServicemapGroup() != null) {
					// servicemapgroup, servicemap 맵핑 삭제
					servicemapDao.deleteServicemapgroupServicemapMapping(smmRow.getServicemapGroup().getServicemapGroupSeq(), servicemapSeq);

					// 서비스맵그룹 sort 초기화 처리
					servicemapDao.updateServicemapInitSortOrder(smmRow.getServicemapGroup().getServicemapGroupSeq());
				}

				// service, servicemap 맵핑 제거
				servicemapDao.deleteServiceServicemapMapping(smmRow.getServiceSeq(), servicemapSeq);

				try {
					// 서비스 이벤트 처리
					this.eventService.getInstance().sendServices(smmRow.getServiceSeq(), servicemapSeq, context);
				} catch (Exception e) {
					log.error("Error! on removeServicemap, event sendServices", e);
				}
			}
		}


	    /**
	     * R4.4.0 클러스터 테넌시 : 2020.06.29 : 현재 삭제되는 서비스맵이 워크스페이스의 클러스터에서 생성한 마지막 서비스맵 이라면 클러스터와의 연결을 끊어줌...
	     * - 소프트 테넌시는 서비스맵이 연결되면서 자동으로 service_cluster 연결이 생성됨... (서비스맵 연결을 위해 반드시 필요하므로...)
	     * - 소프트 테넌시일때 service_cluster 연결은 서비스맵이 없을 경우에는 필요하지 않으므로 제거 해 주도록 한다.
	     */
//	    // 위에서 appmap에 대한 처리(삭제)가 완료된 후 클러스터에 연결된 appmap이 더 있는지 다시 확인 후
//	    List<Integer> sourceAppmaps = appmapDao.getAppmapSeqs(cluster.getServiceSeq(), cluster.getClusterSeq());
//	    // 워크스페이스의 해당 클러스터에 더이상 서비스맵 연결이 없다면 service_cluster 연결 해
//	    if(CollectionUtils.isEmpty(sourceAppmaps)) {                                                        // 더이상 클러스터 내에 서비스맵이 없다면..
//		    IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
//		    ServiceVO service = serviceDao.getService(cluster.getServiceSeq());
//		    if(service.getClusterTenancy() == ClusterTenancy.SOFT                                           // 워크스페이스가 소프트 테넌시이면서,
//			    && service.getServiceType() == ServiceType.NORMAL) {                                        // 플랫폼 워크스페이스가 아닐 때만
//			    result = serviceDao.deleteServiceCluster(cluster.getClusterSeq(), cluster.getServiceSeq()); // service_cluster 연결을 제거한다.
//		    }
//	    }
    }

    @Transactional(transactionManager = "transactionManager")
    public void updateServicemap(ServicemapModVO servicemapMod, ExecutingContextVO context) throws Exception {
		IServicemapGroupMapper servicemapGroupDao = sqlSession.getMapper(IServicemapGroupMapper.class);
    	IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

		if (servicemapMod != null) {
			// check validation
			ExceptionMessageUtils.checkParameterRequired("servicemapSeq", servicemapMod.getServicemapSeq());
			ExceptionMessageUtils.checkParameter("servicemapName", servicemapMod.getServicemapName(), 50, true);

			// current servicemap
			ServicemapVO currServicemap = servicemapDao.getServicemap(servicemapMod.getServicemapSeq(), null);
			ServicemapMappingVO currServicemapMapping = null;
			if (currServicemap != null) {
				boolean isExistServicemapMappings = CollectionUtils.isNotEmpty(currServicemap.getServicemapMappings());

				if (isExistServicemapMappings) {

					if (servicemapMod.getServicemapGroupSeq() != null && servicemapMod.getServiceSeq() != null) {

						Optional<ServicemapMappingVO> servicemapMappingOptional = Optional.ofNullable(currServicemap.getServicemapMappings()).orElseGet(() ->Lists.newArrayList())
								.stream().filter(smm -> (servicemapMod.getServiceSeq().equals(smm.getServiceSeq())))
								.findFirst();

						if (servicemapMappingOptional.isPresent()) {

							currServicemapMapping = servicemapMappingOptional.get();

							// 2019.12.12 hjchoi : 서비스맵 그룹도 수정시 가능하도록 수정
							// 해당 워크스페이스내에 appmapGroupSeq가 존재하는 지 체크
							List<ServicemapGroupVO> servicemapGroups = servicemapGroupDao.getServicemapGroupsOfService(servicemapMod.getServiceSeq());
							Set<Integer> servicemapGroupSeqs = Optional.ofNullable(servicemapGroups).orElseGet(() ->Lists.newArrayList()).stream().map(ag -> ag.getServicemapGroupSeq()).collect(Collectors.toSet());
							if (!servicemapGroupSeqs.contains(servicemapMod.getServicemapGroupSeq())) {
								throw new CocktailException("ServicemapGroupSeq is not exists in workspace.", ExceptionType.InvalidInputData);
							} else {
								if (currServicemapMapping.getServicemapGroup() != null) {
									// delete before servicegroup mapping
									servicemapDao.deleteServicemapgroupServicemapMapping(currServicemapMapping.getServicemapGroup().getServicemapGroupSeq(), servicemapMod.getServicemapSeq());
									// init serviemap group mappint sort
									servicemapDao.updateServicemapInitSortOrder(currServicemapMapping.getServicemapGroup().getServicemapGroupSeq());
								}

								// add after servicegroup mapping
								ServicemapGroupMappingVO servicemapGroupMapping = new ServicemapGroupMappingVO();
								servicemapGroupMapping.setServicemapGroupSeq(servicemapMod.getServicemapGroupSeq());
								servicemapGroupMapping.setServicemapSeq(servicemapMod.getServicemapSeq());
								servicemapGroupMapping.setCreator(context.getUserSeq());
								servicemapDao.addServicemapgroupServicemapMapping(servicemapGroupMapping);
							}
						}
					}
				}


				// update servicemap
				currServicemap.setServicemapName(servicemapMod.getServicemapName());
				currServicemap.setUpdater(context.getUserSeq());
				servicemapDao.updateServicemap(currServicemap);

				ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapMod.getServicemapSeq());

				Map<String, String> labels = servicemapMod.getLabels();
				String namespaceName = cluster.getNamespaceName();
				if(MapUtils.isNotEmpty(labels) && labels.containsKey(KubeConstants.LABELS_ISTIO_INJECTION_KEY)) {

					String istioInjection = labels.get(KubeConstants.LABELS_ISTIO_INJECTION_KEY);
					if(StringUtils.isNotBlank(istioInjection)) {

						// OpenShift 유형과 Istio 설치 유무는 addServiceMeshMemberRollsMember 구현부 내용 참고
						this.addServiceMeshMemberRollsMember(cluster, (members) -> {
							if("enabled".equalsIgnoreCase(istioInjection)) {
								if (StringUtils.isNotBlank(namespaceName) || !members.contains(namespaceName)) {
									members.add(namespaceName);
								}
							}
							else // disabled
							{
								if (StringUtils.isNotBlank(namespaceName) ||  members.contains(namespaceName)) {
									members.remove(namespaceName);
								}
							}
						});

					}
				}

				// patch namespace (labels, annotations)
				namespaceService.patchNamespace(cluster, namespaceName, servicemapMod.getLabels(), servicemapMod.getAnnotations(), context);

				// 서비스 이벤트 처리
				if (isExistServicemapMappings) {
					for (ServicemapMappingVO smmRow : currServicemap.getServicemapMappings()) {
						try {
							this.eventService.getInstance().sendServices(smmRow.getServiceSeq(), servicemapMod.getServicemapSeq(), context);
						} catch (Exception e) {
							log.error("Error! on updateServicemap, event sendServices", e);
						}
					}
				}
			} else {
				throw new CocktailException("Servicemap is null.", ExceptionType.InvalidParameter);
			}

		} else {
			throw new CocktailException("ServicemapMod is null.", ExceptionType.InvalidParameter);
		}
    }

	public void udpateNamespaceOfServicemapByYaml(Integer clusterSeq, String namespaceName, K8sDeployYamlVO deployYaml) throws Exception {

		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getCluster(clusterSeq);

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(cluster);

		K8sNamespaceVO namespace = namespaceService.getNamespace(cluster, namespaceName, ContextHolder.exeContext());
		if (namespace == null) {
			throw new CocktailException(String.format("namespace not found: %d", namespaceName),
					ExceptionType.ResourceNotFound);
		} else {
			if (cluster != null) {
				if (StringUtils.equals(namespaceName, deployYaml.getName())) {
					namespaceService.patchNamespaceByYaml(cluster, deployYaml);
				} else {
					throw new CocktailException("name parameter is invalid.", ExceptionType.InvalidParameter);
				}
			}
		}
	}

    private void addServiceMeshMemberRollsMember(ClusterVO cluster, Consumer<List<String>> consumer) {
    	if(cluster == null) {
			return;
		}

		final String tmpNamespaceName = cluster.getNamespaceName();
		if(StringUtils.isBlank(tmpNamespaceName)) {
			return;
		}

		try {
			// OpenShift 유형과 Istio 설치 확인은 아래의 CRD(Custom Resource Definition) 존재 유무로 판단
			// - servicemeshmemberrolls.maistra.io
			final List<?> crds = crdService.readCustomResourceDefinitionRaw(cluster, "servicemeshmemberrolls.maistra.io");
			if (crds == null && crds.size() <= 0) {
				return;
			}

			// 2021년 10월 29일 (금)
			// Red Hat Service Mesh Operator를 istio-system 네임스페이스에 설치한다고 가정.
			// 추후 이 정보가 변경될 수 있다는 요구사항이 있을 경우 수정될 수 있음
			cluster.setNamespaceName("istio-system");

			Map<String, Object> serviceMeshMemberRolls = crdService.getCustomObject(cluster, "default", "maistra.io", "v1", "servicemeshmemberrolls");
			if (MapUtils.isNotEmpty(serviceMeshMemberRolls) && serviceMeshMemberRolls.containsKey("spec")) {
				Map<String, Object> spec = (Map<String, Object>) serviceMeshMemberRolls.get("spec");
				if (MapUtils.isNotEmpty(spec) && MapUtils.getObject(spec, "members", null) != null) {
					List<String> members = (List<String>) spec.get("members");
					if (members != null) {
						if (consumer != null) {
							consumer.accept(members);

							final String dumpAsMap = crdService.dumpAsMap(serviceMeshMemberRolls);
							if (StringUtils.isNotBlank(dumpAsMap)) {
								K8sCRDYamlVO crdYamlVO = new K8sCRDYamlVO();
								crdYamlVO.setYaml(dumpAsMap);
								crdService.replaceCustomObject(cluster, "default", "maistra.io", "v1", "servicemeshmemberrolls", crdYamlVO);
							}
						}
					}
				}
			}
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			cluster.setNamespaceName(tmpNamespaceName);
		}
	}

    public ServicemapVO getServicemap(Integer servicemapSeq, Integer serviceSeq) throws Exception {
    	IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);

		return servicemapDao.getServicemap(servicemapSeq, serviceSeq);
    }

    public ServicemapDetailVO getServicemapDetail(Integer servicemapSeq, Integer serviceSeq, ExecutingContextVO context) throws Exception {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

		ServicemapDetailVO servicemapDetail = servicemapDao.getServicemapDetail(servicemapSeq, serviceSeq);

		ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);

		K8sNamespaceVO namespace = namespaceService.getNamespace(cluster, servicemapDetail.getNamespaceName(), context);
		if (namespace != null) {
			this.patchNamespaceIstioLabel(cluster, namespace);
			servicemapDetail.setLabels(namespace.getLabels());
			servicemapDetail.setAnnotations(namespace.getAnnotations());
			servicemapDetail.setK8sNamespace(namespace);
			servicemapDetail.setK8sResourceExists(Boolean.TRUE);
		} else {
			servicemapDetail.setK8sResourceExists(Boolean.FALSE);
		}

		return servicemapDetail;
    }

	public ServicemapVO getServicemapByClusterAndName(Integer clusterSeq, String namespaceName) throws Exception {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);

		ServicemapVO servicemap = servicemapDao.getServicemapByClusterAndName(clusterSeq, namespaceName);

		return servicemap;
	}

	@Transactional(transactionManager = "transactionManager")
	public void renameServicemap(Integer servicemapSeq, String servicemapName, ExecutingContextVO context) {

		servicemapName = StringUtils.trim(servicemapName);
		ExceptionMessageUtils.checkParameter("servicemapName", servicemapName, 50, true);

		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);

		ServicemapVO currServicemap = servicemapDao.getServicemap(servicemapSeq, null);

		currServicemap.setServicemapName(servicemapName);
		currServicemap.setUpdater(context.getUserSeq());

		servicemapDao.updateServicemap(currServicemap);

		if (CollectionUtils.isNotEmpty(currServicemap.getServicemapMappings())) {
			for (ServicemapMappingVO smmRow : currServicemap.getServicemapMappings()) {
				try {
					// 서비스 이벤트 처리
					this.eventService.getInstance().sendServices(smmRow.getServiceSeq(), servicemapSeq, context);
				} catch (Exception e) {
					log.error("Error! event sendServices on renameServicemap", e);
				}
			}
		}
	}

	public String getNamespaceName(Integer servicemapSeq) {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
        return servicemapDao.getNamespaceName(servicemapSeq);
    }

	public Integer getServicemapSeqByNamespace(Integer clusterSeq, String namespaceName) {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		return servicemapDao.getServicemapSeqByNamespace(clusterSeq, namespaceName);
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateServicemapOrder(ServicemapOrderVO servicemapOrder) throws Exception{

		ExceptionMessageUtils.checkParameterRequired("servicemapSeq", servicemapOrder.getServicemapSeq());
		ExceptionMessageUtils.checkParameterRequired("serviceSeq", servicemapOrder.getServiceSeq());
		ExceptionMessageUtils.checkParameterRequired("servicemapGroupSeq", servicemapOrder.getServicemapGroupSeq());
		ExceptionMessageUtils.checkParameterRequired("sortOrder", servicemapOrder.getSortOrder());

		int servicemapSeq = servicemapOrder.getServicemapSeq();
		int serviceSeq = servicemapOrder.getServiceSeq();
		int nextGroupSeq = servicemapOrder.getServicemapGroupSeq();
		int nextSortOrder = servicemapOrder.getSortOrder();

		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		ServicemapVO currServicemap = servicemapDao.getServicemap(servicemapSeq, null);

		if (currServicemap != null) {
			boolean isExistServicemapMappings = CollectionUtils.isNotEmpty(currServicemap.getServicemapMappings());
			ServicemapMappingVO currServicemapMapping = null;

			if (isExistServicemapMappings) {
				Optional<ServicemapMappingVO> servicemapMappingOptional = Optional.ofNullable(currServicemap.getServicemapMappings()).orElseGet(() ->Lists.newArrayList())
						.stream().filter(smm -> (smm.getServiceSeq().equals(serviceSeq)))
						.findFirst();
				if (servicemapMappingOptional.isPresent()) {
					currServicemapMapping = servicemapMappingOptional.get();

					int currGroupSeq = currServicemapMapping.getServicemapGroup().getServicemapGroupSeq();
					int currSortOrder = currServicemapMapping.getServicemapGroup().getSortOrder();

					// init current group sort order
					servicemapDao.updateServicemapInitSortOrder(currGroupSeq);

					if (currGroupSeq != nextGroupSeq) {
						// init next group sort order
						servicemapDao.updateServicemapInitSortOrder(nextGroupSeq);

						servicemapDao.updateServicemapSortOrder(serviceSeq, currGroupSeq, currSortOrder + 1, null, -1);
						servicemapDao.updateServicemapSortOrder(serviceSeq, nextGroupSeq, nextSortOrder, null, 1);

						ServicemapGroupMappingVO nextServicemapGroupMapping = new ServicemapGroupMappingVO();
						nextServicemapGroupMapping.setServicemapGroupSeq(currGroupSeq);
						nextServicemapGroupMapping.setServicemapSeq(servicemapSeq);
						nextServicemapGroupMapping.setSortOrder(nextSortOrder);
						nextServicemapGroupMapping.setUpdater(ContextHolder.exeContext().getUserSeq());
						servicemapDao.updateServicemapSelfSortOrder(nextGroupSeq, nextServicemapGroupMapping);
					} else {
						if (currSortOrder < nextSortOrder) {
                            servicemapDao.updateServicemapSortOrder(serviceSeq, currGroupSeq, currSortOrder + 1, nextSortOrder, -1);
						} else if (currSortOrder > nextSortOrder) {
                            servicemapDao.updateServicemapSortOrder(serviceSeq, currGroupSeq, nextSortOrder, currSortOrder - 1, 1);
						}
                        ServicemapGroupMappingVO nextServicemapGroupMapping = new ServicemapGroupMappingVO();
                        nextServicemapGroupMapping.setServicemapGroupSeq(nextGroupSeq);
                        nextServicemapGroupMapping.setServicemapSeq(servicemapSeq);
                        nextServicemapGroupMapping.setSortOrder(nextSortOrder);
                        nextServicemapGroupMapping.setUpdater(ContextHolder.exeContext().getUserSeq());
                        servicemapDao.updateServicemapSelfSortOrder(null, nextServicemapGroupMapping);
					}

                    for (ServicemapMappingVO smmRow : currServicemap.getServicemapMappings()) {
                        try {
                            // 서비스 이벤트 처리
                            this.eventService.getInstance().sendServices(smmRow.getServiceSeq(), servicemapSeq, ContextHolder.exeContext());
                        } catch (Exception e) {
                            log.error("Error! event sendServices on updateServicemapOrder", e);
                        }
                    }
				}
			}

		}
	}

	public Set<String> getNamespaceNamesByServiceInCluster(Integer serviceSeq, Integer clusterSeq) throws Exception {
		List<ServicemapVO> servicemaps = this.getServicemapsByServiceClusters(serviceSeq, Collections.singletonList(clusterSeq), null);
		return servicemaps.stream().filter(sa -> (sa.getClusterSeq().equals(clusterSeq) && StringUtils.isNotBlank(sa.getNamespaceName()))).map(ServicemapVO::getNamespaceName).collect(Collectors.toSet());
	}

	public List<ServicemapVO> getServicemapsByServiceClusters(Integer serviceSeq, List<Integer> clusterSeqs, String serviceType) {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		return servicemapDao.getServicemapsByServiceClusters(serviceSeq, clusterSeqs, serviceType);
	}

    public List<String> getNamespaceListOfCluster(Integer clusterSeq) throws Exception {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
        return servicemapDao.getNamespaceListOfCluster(clusterSeq);
    }

    public List<ServicemapSummaryVO> getServicemapSummaries(Integer accountSeq, Integer clusterSeq, List<Integer> serviceSeqs, Integer servicemapSeq) {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		return servicemapDao.getServicemapSummaries(accountSeq, clusterSeq, serviceSeqs, servicemapSeq);
    }

    public ServicemapSummaryVO getServicemapSummary(Integer clusterSeq, String namespaceName, Integer servicemapSeq) {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		return servicemapDao.getServicemapSummary(clusterSeq, namespaceName, servicemapSeq);
    }

	/**
	 * 서비스맵 정보 map
	 *
	 * @param clusterSeq
	 * @return Map<namespaceName, ServicemapSummaryVO>
	 */
	public Map<String, ServicemapSummaryVO> getServicemapSummaryMap(Integer clusterSeq) throws Exception {
		List<ServicemapSummaryVO> servicemapInfos = this.getServicemapSummaries(null, clusterSeq, null, null);
		return Optional.ofNullable(servicemapInfos).orElseGet(() ->Lists.newArrayList())
				.stream().collect(Collectors.toMap(ServicemapSummaryVO::getNamespaceName, Function.identity()));
	}


	/**
	 * Namespace별 gateways
	 *
	 * @param cluster
	 * @param context
	 * @return Map<namespace, Map<GateWayNameType, GateWayCount>>
	 * @throws Exception
	 */
	public Map<String, Map<GateWayNameType, Integer>> getNamespacedClusterGateWays(ClusterVO cluster, Set<String> namespaces, boolean useService, boolean useIngress, ExecutingContextVO context) throws Exception {

		// 객체 생성  및 초기화
		Map<String, Map<GateWayNameType, Integer>> namespacedClusterGateWays = new HashMap<>();

		int ingressCnt = 0;
		int nodeportCnt = 0;
		int loadBalancerCnt = 0;
		int clusterIpCnt = 0;

		// count 조회
		if(cluster != null && clusterStateService.isClusterRunning(cluster)) {

			if (useIngress) {
				// Ingress 조회
				List<K8sIngressVO> ingressList = ingressSpecService.getIngresses(cluster, null, null, context);

				if(CollectionUtils.isNotEmpty(ingressList)){
					for(K8sIngressVO ingressRow: ingressList){
						if (CollectionUtils.isNotEmpty(namespaces) && !namespaces.contains(ingressRow.getNamespace())) {
							continue;
						}

						if (MapUtils.getObject(namespacedClusterGateWays, ingressRow.getNamespace(), null) == null
								|| MapUtils.getObject(namespacedClusterGateWays.get(ingressRow.getNamespace()), GateWayNameType.INGRESS, null) == null) {
							Map<GateWayNameType, Integer> gateWayMap = new HashMap<>();
							gateWayMap.put(GateWayNameType.INGRESS, 0);

							namespacedClusterGateWays.put(ingressRow.getNamespace(), gateWayMap);
						}
						ingressCnt = namespacedClusterGateWays.get(ingressRow.getNamespace()).get(GateWayNameType.INGRESS).intValue() + 1;
						namespacedClusterGateWays.get(ingressRow.getNamespace()).put(GateWayNameType.INGRESS, ingressCnt);
					}
				}
			}

			if (useService) {
				// Node Port, LoadBalancer, ClusterIP 조회
				List<K8sServiceVO> k8sServices = serviceSpecService.getServices(cluster.getClusterSeq(), null, null, null, context);

				if(CollectionUtils.isNotEmpty(k8sServices)){

					for(K8sServiceVO serviceRow : k8sServices) {
						if (CollectionUtils.isNotEmpty(namespaces) && !namespaces.contains(serviceRow.getNamespace())) {
							continue;
						}

						K8sServiceDetailVO detailRow = serviceRow.getDetail();
						if(detailRow != null) {

							if (MapUtils.getObject(namespacedClusterGateWays, serviceRow.getNamespace(), null) == null) {
								namespacedClusterGateWays.put(serviceRow.getNamespace(), Maps.newHashMap());
							}

							if( StringUtils.equals(detailRow.getType(), KubeServiceTypes.NodePort.name()) ){ // Node Port

								if( CollectionUtils.isNotEmpty(serviceRow.getServicePorts()) ) {
									for (K8sServicePortVO portRow : serviceRow.getServicePorts()) {
										if (MapUtils.getObject(namespacedClusterGateWays.get(serviceRow.getNamespace()), GateWayNameType.NODE_PORT, null) == null) {
											Map<GateWayNameType, Integer> gateWayMap = new HashMap<>();
											gateWayMap.put(GateWayNameType.NODE_PORT, 0);

											namespacedClusterGateWays.get(serviceRow.getNamespace()).putAll(gateWayMap);
										}
										nodeportCnt = namespacedClusterGateWays.get(serviceRow.getNamespace()).get(GateWayNameType.NODE_PORT).intValue() + 1;
										namespacedClusterGateWays.get(serviceRow.getNamespace()).put(GateWayNameType.NODE_PORT, nodeportCnt);
									}
								}

							} else if( StringUtils.equalsIgnoreCase(detailRow.getType(), KubeServiceTypes.LoadBalancer.name()) ){ // LoadBalancer

								if (MapUtils.getObject(namespacedClusterGateWays.get(serviceRow.getNamespace()), GateWayNameType.LOAD_BALANCER, null) == null) {
									Map<GateWayNameType, Integer> gateWayMap = new HashMap<>();
									gateWayMap.put(GateWayNameType.LOAD_BALANCER, 0);

									namespacedClusterGateWays.get(serviceRow.getNamespace()).putAll(gateWayMap);
								}
								loadBalancerCnt = namespacedClusterGateWays.get(serviceRow.getNamespace()).get(GateWayNameType.LOAD_BALANCER).intValue() + 1;
								namespacedClusterGateWays.get(serviceRow.getNamespace()).put(GateWayNameType.LOAD_BALANCER, loadBalancerCnt);

							} else if( StringUtils.equalsIgnoreCase(detailRow.getType(), KubeServiceTypes.ClusterIP.name()) ){ // ClusterIP

								if (MapUtils.getObject(namespacedClusterGateWays.get(serviceRow.getNamespace()), GateWayNameType.CLUSTER_IP, null) == null) {
									Map<GateWayNameType, Integer> gateWayMap = new HashMap<>();
									gateWayMap.put(GateWayNameType.CLUSTER_IP, 0);
									namespacedClusterGateWays.get(serviceRow.getNamespace()).putAll(gateWayMap);
								}
								clusterIpCnt = namespacedClusterGateWays.get(serviceRow.getNamespace()).get(GateWayNameType.CLUSTER_IP).intValue() + 1;
								namespacedClusterGateWays.get(serviceRow.getNamespace()).put(GateWayNameType.CLUSTER_IP, clusterIpCnt);

							}
						}
					}
				}
			}

		}

		return namespacedClusterGateWays;
	}

	public String getRegistryUrl() throws Exception {
		return this.registry.getUrl();
	}

//	@Transactional(transactionManager = "transactionManager")
//	public void moveAppmapWorkspace(Integer appmapSeq, Integer moveServiceSeq, Integer moveAppmapGroupSeq) throws Exception{
//
//		Integer userSeq = ContextHolder.exeContext().getUserSeq();
//
//		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
//		IAppmapMapper appmapDao = sqlSession.getMapper(IAppmapMapper.class);
//		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
//		IAppmapGroupMapper appmapGroupDao = sqlSession.getMapper(IAppmapGroupMapper.class);
//		IGroupMapper groupDao = sqlSession.getMapper(IGroupMapper.class);
//		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
//
//		/** ============================= DB 조회 & 체크 로직 ============================= **/
//		/// 기존 namespace 관련 정보 조회
//		AppmapVO currAppmapVO = this.getAppmap(appmapSeq, ContextHolder.exeContext());
//		Integer currClusterSeq = appmapDao.getClusterSeq(appmapSeq);
//		ClusterVO cluster = clusterDao.getCluster(currClusterSeq);
//		ServiceVO currServiceVO = serviceDao.getService(currAppmapVO.getServiceSeq()); // 기존 서비스 조회
//		List<ServiceClusterVO> currServiceClusters = serviceDao.getClusterOfService(currAppmapVO.getServiceSeq());
//
//		/// 이동할 서비스 관련 정보 조회
//		ServiceVO moveServiceVO = serviceDao.getService(moveServiceSeq); // 이동할 서비스 조회
//		AppmapGroupVO moveAppmapGroupVO = appmapGroupDao.getAppmapGroup(moveAppmapGroupSeq, moveServiceSeq);
//		List<ServiceClusterVO> moveServiceClusters = serviceDao.getClusterOfService(moveServiceSeq); // 이동할 service-cluster 정보 조회
//
//		/// validation
//		if(currAppmapVO == null){
//			throw new CocktailException("Appmap is not exists.", ExceptionType.InvalidInputData);
//		}
//
//		if(moveServiceVO == null){
//			throw new CocktailException("Workspace is not exists.", ExceptionType.InvalidInputData);
//		}
//
//		if(moveAppmapGroupVO == null){
//			throw new CocktailException("AppmapGroupSeq is not exists in workspace.", ExceptionType.InvalidInputData);
//		}
//
//		if(currAppmapVO.getServiceSeq().equals(moveServiceSeq)){
//			throw new CocktailException("it can not move to the same workspace.", ExceptionType.InvalidInputData);
//		}
//
//		if(!currServiceVO.getAccount().getAccountSeq().equals(moveServiceVO.getAccount().getAccountSeq())){
//			throw new CocktailException("it can not move to the other system.", ExceptionType.InvalidInputData);
//		}
//
//		/// service registry id/pw 복호화
//		currServiceVO.setRegistryUserId(CryptoUtils.decryptAES(currServiceVO.getRegistryUserId()));
//		currServiceVO.setRegistryUserPassword(CryptoUtils.decryptAES(currServiceVO.getRegistryUserPassword()));
//
//		moveServiceVO.setRegistryUserId(CryptoUtils.decryptAES(moveServiceVO.getRegistryUserId()));
//		moveServiceVO.setRegistryUserPassword(CryptoUtils.decryptAES(moveServiceVO.getRegistryUserPassword()));
//
//		/// namespace에서 사용하는 registry & 옮겨질 service에서 사용하는 registry 조회 후 이동할 서비스에서 포함하고 있는지 체크
//		List<PipelineContainerVO> registryList = pipelineDao.getUsedRegistryInfoByClusterAndNamespace(currClusterSeq, currAppmapVO.getNamespaceName()); // namespace에서 사용하는 registry 정보 조회
//		Map<Integer, String> currRegistry = Maps.newHashMap(); // namespace에서 사용하는 registry map 생성
//		if(CollectionUtils.isNotEmpty(registryList)){
//			currRegistry = registryList.stream().collect(Collectors.toMap(PipelineContainerVO::getDeployRegistrySeq, PipelineContainerVO::getDeployRegistryName));
//		}
//
//		List<ServiceRegistryVO> serviceRegitryList = serviceDao.getServiceRegistries(moveServiceSeq, null, null, null); // 옴겨질 서비스에서 사용하는 registry 조회
//		Map<Integer, String> moveRegistry = Maps.newHashMap(); // 이동할 서비스에서 사용하는 registry map 생성
//		if(CollectionUtils.isNotEmpty(serviceRegitryList)){
//			moveRegistry = serviceRegitryList.stream().collect(Collectors.toMap(ServiceRegistryVO::getProjectId, ServiceRegistryVO::getProjectName));
//		}
//		/// 옴겨질 namespace가 사용하는 registry를 옴길 서비스에서 모두 포함하는지 체크
//		boolean contain = ServerUtils.containMaps(moveRegistry, currRegistry);
//
//		/** ============================= DB update 로직 ============================= **/
//
//		/// 이동할 서비스에 appmap에서 사용하는 클러스터가 존재하지 않을 경우 클러스터 맵핑정보 등록
//		long containClusterCount = moveServiceClusters.stream().filter(vo -> vo.getClusterSeq().equals(currClusterSeq)).count();
//		if(containClusterCount == 0){
//			ServiceClusterVO currServiceCluster = currServiceClusters.stream().filter(vo -> vo.getClusterSeq().equals(currClusterSeq)).findFirst().get();
//			currServiceCluster.setServiceSeq(moveServiceSeq);
//			serviceDao.addClustersOfService(moveServiceSeq, Arrays.asList(currServiceCluster), userSeq);
//			log.debug("클러스터가 존재하지 않아 추가함 currServiceCluster : {} ", currServiceCluster);
//		}
//
//		/// update appmaps 테이블 변경, serviceSeq & appmapGroupSeq를 이동할 값으로 변경
//		AppmapVO moveAppmapVO = new AppmapVO();
//		BeanUtils.copyProperties(currAppmapVO, moveAppmapVO);
//		moveAppmapVO.setAppmapGroupSeq(moveAppmapGroupSeq);
//		moveAppmapVO.setServiceSeq(moveServiceSeq);
//		moveAppmapVO.setCreator(userSeq);
//		moveAppmapVO.setUpdater(userSeq);
//		appmapDao.updateAppmap(moveAppmapVO);
//
//		/// groups 테이블의 appmap_seq에 해당하는 service_seq값 변경
//		GroupVO moveGroup = new GroupVO();
//		moveGroup.setAppmapSeq(appmapSeq);
//		moveGroup.setServiceSeq(moveServiceSeq);
//		groupDao.updateGroupServiceSeqByAppmapSeq(moveGroup);
//
//		/// service_appmap_cluster 테이블 변경, 삭제 후 등록
//		appmapDao.removeAppmapCluster(appmapSeq); // service_appmap_cluster 테이블 데이터 삭제
//		appmapDao.addAppmapClusters(moveAppmapVO); // service_appmap_cluster 테이블 데이터 등록
//
//		/// 이동할 서비스에서 namespace에서 사용하는 registry를 포함하지 않을 경우, 옮겨질 서비스에 registry 등록
//		List<ServiceRegistryVO> moveServiceRegistryList = Lists.newArrayList();
//		if(!contain){
//			MapDifference<Integer, String> diff = Maps.difference(moveRegistry, currRegistry);
//			moveRegistry = diff.entriesOnlyOnRight(); // 포함하지 않는 registry 정보
//			for (Map.Entry<Integer, String> entry : moveRegistry.entrySet()) {
//				ServiceRegistryVO svcRegiVO = new ServiceRegistryVO();
//				svcRegiVO.setProjectId(entry.getKey());
//				svcRegiVO.setProjectName(entry.getValue());
//				svcRegiVO.setProjectType(ServiceRegistryType.SHARE);
//				moveServiceRegistryList.add(svcRegiVO);
//			}
//			// registry 저장
//			serviceDao.addProjectsOfService(moveServiceSeq, moveServiceRegistryList, userSeq);
//		}
//
//		/** ============================= Harbor 계정 정보 처리 ============================= **/
//		/// Harbor 계정 체크 및 등록, 같은 account 내에 registry라면 계정이 존재하지 않는경우가 없을 것 같음, 확인해서 필요 없으면 아래 로직 삭제 예정
//		/// 추가할 registry에 이동할 service의 계정 존재하는지 체크 및 등록, harbor 사용자 권한 수정(?) 권한수정이 필요할지 확인 필요, 현재는 GUEST로 등록
////		if(CollectionUtils.isNotEmpty(moveServiceRegistryList)) {
////			ProductsApi productsApi = registryService.getProductsApiByAccountSeq(currServiceVO.getAccount().getAccountSeq());
////			User serviceRegistryUser = registryService.getUser(productsApi, moveServiceVO.getRegistryUserId());
////
////			//등록할 사용자 정보 생성
////			ProjectMember projectMember = new ProjectMember();
////			UserEntity memberUser = new UserEntity();
////			memberUser.setUsername(serviceRegistryUser.getUsername());
////			projectMember.memberUser(memberUser).setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());
////
////			for(ServiceRegistryVO tmpServiceRegistry : moveServiceRegistryList) {
////				ProjectMemberEntity projectMemberEntity = registryService.getProjectMember(productsApi, tmpServiceRegistry.getProjectId(), serviceRegistryUser.getUsername());
////				// 사용자 정보가 없으면 등록
////				if(projectMemberEntity == null) {
////					registryService.addProjectMember(productsApi, tmpServiceRegistry.getProjectId(), projectMember);
////				}
////			}
////		}
//
//		/** ============================= K8s 리소스 정보 처리 ============================= **/
//		/// namespace에 존재하는 Docker ImagePull secret 삭제 및 이동할 서비스의 Docker ImagePull secret 생성
////		SecretGuiVO currSecretVO = secretService.getSecret(currAppmapVO.getServiceSeq(), currAppmapVO.getNamespaceName(), currServiceVO.getRegistryUserId());
////		if(currSecretVO != null) {
////			secretService.deleteSecret(currClusterSeq, currAppmapVO.getNamespaceName(), currSecretVO.getName());
////		}
////		DockerRegistrySecretVO secret = new DockerRegistrySecretVO();
////		secret.setMakeCocktail(false);
////		secret.setType(SecretType.DockerRegistry);
////		secret.setName(moveServiceVO.getRegistryUserId());
////		secret.setUserName(moveServiceVO.getRegistryUserId());
////		secret.setPassword(moveServiceVO.getRegistryUserPassword());
////		secret.setEmail(String.format("%s@%s", moveServiceVO.getRegistryUserId(), CommonConstants.DEFAULT_USER_DOMAIN));
////		secret.setServerUrl(this.registry.getUrl());
////		secretService.createDockerRegistrySecret(moveServiceVO.getServiceSeq(), currAppmapVO.getAppmapSeq(), secret);
//
//		/** ============================= workspace 이동처리 끝 ============================= **/
//
//		/**
//		 * 2022.03.16 hjchoi
//		 * imagePullSecret 생성 (플랫폼 레지스트리 pull 사용자로 생성)
//		 */
//		// 레지스트리 pull user 생성 및 존재하는 레지스트리에 pull user member 추가
//		accountService.createAccountRegistryPullUser(cluster.getAccount());
//
//		String registryPullUserId = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId());
//		String registryPullUserPassword = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserPassword());
//
//		if (secretService.getSecret(cluster, currAppmapVO.getNamespaceName(), registryPullUserId) == null) {
//
//			DockerRegistrySecretVO secret = new DockerRegistrySecretVO();
//			secret.setMakeCocktail(false);
//			secret.setType(SecretType.DockerRegistry);
//			secret.setName(registryPullUserId);
//			secret.setUserName(registryPullUserId);
//			secret.setPassword(registryPullUserPassword);
//			secret.setEmail(String.format("%s@%s", registryPullUserId, CommonConstants.DEFAULT_USER_DOMAIN));
//			secret.setServerUrl(this.registry.getUrl(cluster.getAccount().getAccountSeq()));
//			secretService.createDockerRegistrySecret(cluster, currAppmapVO.getNamespaceName(), secret);
//		}
//	}
}
