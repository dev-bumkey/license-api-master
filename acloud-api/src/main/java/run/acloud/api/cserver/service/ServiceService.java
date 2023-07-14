package run.acloud.api.cserver.service;

import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.build.dao.IBuildMapper;
import run.acloud.api.build.dao.IBuildServerMapper;
import run.acloud.api.build.vo.BuildServerVO;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.dao.IExternalRegistryMapper;
import run.acloud.api.configuration.enums.ClusterTenancy;
import run.acloud.api.configuration.enums.ServiceRegistryType;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.service.AccountRegistryService;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.service.ExternalRegistryService;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.dao.IServicemapGroupMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.event.service.EventService;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.HarborRegistryProjectMemberRole;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.*;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io Created on 2017. 1. 10.
 */
@Slf4j
@Service
public class ServiceService {

	private final static String DEFAULT_SERVICEMAP_GROUP_NAME = "Default";

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private ServicemapService servicemapService;

	@Autowired
	private HarborRegistryFactoryService harborRegistryFactory;

	@Autowired
	private SecretService secretService;

	@Autowired
	private EventService eventService;

	@Autowired
	private ClusterStateService clusterStateService;

	@Autowired
	private NamespaceService namespaceService;

	@Autowired
	private LimitRangeService limitRangeService;

	@Autowired
	private ResourceQuotaService resourceQuotaService;

	@Autowired
	private NetworkPolicyService networkPolicyService;

	@Autowired
	private ExternalRegistryService externalRegistryService;

	@Autowired
	private ServiceValidService serviceValidService;

	@Autowired
	private AccountService accountService;

	@Autowired
	private AccountRegistryService accountRegistryService;

	@Autowired
	private RegistryPropertyService registryProperties;

	public List<ServiceSummaryVO> getServiceSummaries(Map<String, Object> parameters) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		List<ServiceSummaryVO> serviceSummaries = dao.getServiceSummaries(parameters);

		if (CollectionUtils.isNotEmpty(serviceSummaries) && MapUtils.isNotEmpty(parameters)) {
			boolean useService = MapUtils.getBooleanValue(parameters, "useService", false);
			boolean useIngressHost = MapUtils.getBooleanValue(parameters, "useIngressHost", false);
			boolean useWorkload = MapUtils.getBooleanValue(parameters, "useWorkload", false);
			if (useService || useIngressHost || useWorkload) {
				List<Integer> serviceSeqs = serviceSummaries.stream().map(ServiceSummaryVO::getServiceSeq).collect(Collectors.toList());
				List<ServicemapSummaryAdditionalVO> servicemapSummaries = servicemapService.getServicemapSummaries(null, serviceSeqs, false, useService, false, useIngressHost, useWorkload, false);
				if (CollectionUtils.isNotEmpty(servicemapSummaries)) {
					// 서비스맵에 맵핑된 워크스페이스순번을 추림
					// Map<servicemapSeq, List<serviceSeq>>
					Map<Integer, List<Integer>> serviceOfServicemapMap = Maps.newHashMap();
					for (ServicemapSummaryAdditionalVO ssRow : servicemapSummaries) {
						serviceOfServicemapMap.computeIfAbsent(ssRow.getServicemapSeq(), (value) -> Lists.newArrayList());
						serviceOfServicemapMap.get(ssRow.getServicemapSeq()).addAll(
								Optional.ofNullable(ssRow.getServicemapMappings()).orElseGet(() ->Lists.newArrayList())
										.stream().map(ServicemapMappingVO::getServiceSeq).collect(Collectors.toList()));
					}
					// 워크스페이스별로 Map에 셋팅
					Map<Integer, ServiceSummaryVO> ssMap = new HashMap<>(); // service
					Map<Integer, List<IngressHostInfoVO>> ihiMap = new HashMap<>(); // ingress host
					Map<Integer, Integer> wMap = new HashMap<>(); // workload
					for (Integer serviceSeq : serviceSeqs) {
						for (ServicemapSummaryAdditionalVO ssRow : servicemapSummaries) {
							// 서비스맵이 해당 워크스페이스에 맵핑되었있는지 체크
							if (serviceOfServicemapMap.get(ssRow.getServicemapSeq()).contains(serviceSeq)) {
								if (useService) {
									if (MapUtils.getObject(ssMap, serviceSeq, null) == null) {
										ssMap.put(serviceSeq, new ServiceSummaryVO());
									}

									ssMap.get(serviceSeq).setGateWayCount(ssMap.get(serviceSeq).getGateWayCount() + ssRow.getGateWayCount());
									ssMap.get(serviceSeq).setLoadBalancerCount(ssMap.get(serviceSeq).getLoadBalancerCount() + ssRow.getLoadBalancerCount());
									ssMap.get(serviceSeq).setNodePortCount(ssMap.get(serviceSeq).getNodePortCount() + ssRow.getNodePortCount());
									ssMap.get(serviceSeq).setIngressCount(ssMap.get(serviceSeq).getIngressCount() + ssRow.getIngressCount());
									ssMap.get(serviceSeq).setClusterIpCount(ssMap.get(serviceSeq).getClusterIpCount() + ssRow.getClusterIpCount());
								}
								if (useIngressHost) {
									if (CollectionUtils.isNotEmpty(ssRow.getIngressHostInfos())) {
										if (MapUtils.getObject(ihiMap, serviceSeq, null) == null) {
											ihiMap.put(serviceSeq, new ArrayList<>());
										}
										ihiMap.get(serviceSeq).addAll(ssRow.getIngressHostInfos());
									}
								}
								if (useWorkload) {
									if (MapUtils.getObject(wMap, serviceSeq, null) == null) {
										wMap.put(serviceSeq, 0);
									}
									wMap.put(serviceSeq, wMap.get(serviceSeq).intValue() + ssRow.getServerCount());
								}
							}
						}
					}
					for (ServiceSummaryVO ssRow : serviceSummaries) {
						if (useService && MapUtils.getObject(ssMap, ssRow.getServiceSeq(), null) != null) {
							ssRow.setGateWayCount(ssMap.get(ssRow.getServiceSeq()).getGateWayCount());
							ssRow.setLoadBalancerCount(ssMap.get(ssRow.getServiceSeq()).getLoadBalancerCount());
							ssRow.setNodePortCount(ssMap.get(ssRow.getServiceSeq()).getNodePortCount());
							ssRow.setIngressCount(ssMap.get(ssRow.getServiceSeq()).getIngressCount());
							ssRow.setClusterIpCount(ssMap.get(ssRow.getServiceSeq()).getClusterIpCount());
						}
                        if (useIngressHost && MapUtils.getObject(ihiMap, ssRow.getServiceSeq(), null) != null) {
                            ssRow.setIngressHostInfos(ihiMap.get(ssRow.getServiceSeq()));
                        }
                        if (useWorkload && MapUtils.getObject(wMap, ssRow.getServiceSeq(), null) != null) {
                        	ssRow.setServerCount(wMap.get(ssRow.getServiceSeq()));
						}
					}
				}
			}
		}

		return serviceSummaries;
	}

	public ServiceAddVO addService(ServiceAddVO service) throws Exception {
		return this.addService(service, ApiVersionType.V2.getType());
	}

	@Transactional(transactionManager = "transactionManager")
	public ServiceAddVO addService(ServiceAddVO service, String apiVersion) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		IServicemapGroupMapper servicemapGroupDao = sqlSession.getMapper(IServicemapGroupMapper.class);

		Integer creator = ContextHolder.exeContext().getUserSeq();
		service.setUseYn("Y");

		// registry user id/pw encrypt
		service.setRegistryUserId(CryptoUtils.encryptAES(service.getRegistryUserId()));
		service.setRegistryUserPassword(CryptoUtils.encryptAES(service.getRegistryUserPassword()));

		/**
		 * 2022.05.31 hjchoi - clusterTenancy 기능 제거, 기본 SOFT 로 값만 셋팅
		 */
		service.setClusterTenancy(ClusterTenancy.SOFT);

		dao.addService(service);
		Integer serviceSeq = service.getServiceSeq();
		List<ServiceRegistryVO> projects = service.getProjects();

		if (CollectionUtils.isNotEmpty(projects)) {
		    dao.addProjectsOfService(serviceSeq, projects, creator);
        }

        if (service.getAccountSeq() != null){
			dao.addAccountServiceMapping(service.getAccountSeq(), serviceSeq, creator);
		}

		ServicemapGroupAddVO servicemapGroup = new ServicemapGroupAddVO();
        servicemapGroup.setServicemapGroupName(DEFAULT_SERVICEMAP_GROUP_NAME);
        servicemapGroup.setServiceSeq(serviceSeq);
        servicemapGroup.setColorCode(service.getColorCode());
        servicemapGroup.setSortOrder(1);
        servicemapGroup.setCreator(creator);
		servicemapGroup.setUpdater(creator);

		servicemapGroupDao.addServicemapGroup(servicemapGroup);

        return service;
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateService(ServiceAddVO service, ServiceDetailVO currService, String apiVersion) throws Exception {
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

		Integer serviceSeq = service.getServiceSeq();
		Integer updater = service.getUpdater();

		serviceDao.updateService(service);

		/**
		 * AccountType in (Cocktail, Apps) 일 경우에만 registry를 생성하도록 함
		 */
		if (currService != null && currService.getAccount() != null && !currService.getAccount().getAccountType().isCubeEngine()) {
			/**
			 * 워크스페이스 registry 수정
			 */
			this.updateProjectsOfService(serviceSeq, service.getProjects(), updater, apiVersion);
		}


		/**
		 * 외부 레지스트리 수정
		 */
		externalRegistryService.updateExternalRegistriesOfService(serviceSeq, service.getExternalRegistries(), updater);
	}

	public void updateService(ServiceAddVO service) throws Exception {
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		serviceDao.updateService(service);
	}

	public int removeService(Integer serviceSeq, Integer updater) throws Exception {
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		return serviceDao.removeService(serviceSeq, updater);
	}

	public int removeServiceAndMapping(Integer accountSeq, Integer serviceSeq, Integer updater) throws Exception {
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		serviceDao.deleteAccountServiceMapping(accountSeq, serviceSeq);
		serviceDao.deleteServiceCluster(null, serviceSeq);
		return serviceDao.removeService(serviceSeq, updater);
	}

    public void markServiceUnused(Integer serviceSeq, Integer updater) throws Exception {
        IServiceMapper serviceDao = this.sqlSession.getMapper(IServiceMapper.class);
		serviceDao.removeService(serviceSeq, updater);
	}

	public int deleteServiceClusterByCluster(Integer clusterSeq) throws Exception {
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

		return serviceDao.deleteServiceCluster(clusterSeq, null);
	}

	public List<ServiceListVO> getServices(Integer userSeq, String userRole, String useYn) {
		return this.getServices(null, userSeq, userRole, useYn);
	}

	public List<ServiceListVO> getServices(Integer serviceSeq, Integer userSeq, String userRole, String useYn) {
		return this.getServices(serviceSeq, userSeq, userRole, useYn, null);
	}

	public List<ServiceListVO> getServices(Integer serviceSeq, Integer userSeq, String userRole, String useYn, Integer projectId) {
		return this.getServices(ContextHolder.exeContext().getUserAccountSeq(), serviceSeq, userSeq, userRole, useYn, projectId);
	}

	public List<ServiceListVO> getServices(Integer accountSeq, Integer serviceSeq, Integer userSeq, String userRole, String useYn, Integer projectId) {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		List<ServiceListVO> services = dao.getServices(accountSeq, userSeq, userRole, useYn, projectId);

		// serviceSeq가 존재하면 해당 Service만 조회하도록 처리함.
		if(serviceSeq != null && serviceSeq > 0) {
			services.removeIf(service -> (!service.getServiceSeq().equals(serviceSeq)));
		}

		if (!UserRole.valueOf(userRole).isAdmin()) {
			// 본인이 Account User로 지정된 서비스에 대해서도 Remove 하지 않도록 처리를 추가합니다. 2018.11.16
			if(UserRole.valueOf(userRole).isUserOfSystem()){
				services.removeIf(service -> {
					if(service.getAccount() != null){
						if(CollectionUtils.isNotEmpty(service.getAccount().getAccountUsers())){
							Optional<UserVO> userVOOptional = service.getAccount().getAccountUsers().stream().filter(au -> (au.getUserSeq().equals(userSeq))).findFirst();

							return !userVOOptional.isPresent();
						}else{
							if(service.getAccount().getAccountUser() != null){
								return !userSeq.equals(service.getAccount().getAccountUser().getUserSeq());
							}
						}
					}

					return false;
				});
			}else{
				services.removeIf(service -> (service.getUserSeqs() != null && !service.getUserSeqs().contains(userSeq)));
			}

			/** KISA 보안인증 관련 로직 추가 : 사용자 정보 노출 취약점, 2022-11-01, coolingi **/
			// userSeq 이용한 service > account > accountusers, serviceUsers 정보 필터링, userSeq에 해당하는 정보 제외하고 사용자 정보 모두 제거
			for (ServiceListVO service : services){
				// account user 필터링
				if (service.getAccount() != null && CollectionUtils.isNotEmpty(service.getAccount().getAccountUsers())){
					List<UserVO> acountUsers = service.getAccount().getAccountUsers();
					acountUsers.removeIf(user -> !userSeq.equals(user.getUserSeq()));
				}

				// service user 필터링
				if (CollectionUtils.isNotEmpty(service.getServiceUsers())){
					List<ServiceUserVO> serviceUser = service.getServiceUsers();
					serviceUser.removeIf(user -> !userSeq.equals(user.getUserSeq()));
				}
			}
		}

		return services;
	}

	public List<ServiceCountVO> getServicesInRegistry(Integer projectId, String useYn) {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServicesInRegistry(ContextHolder.exeContext().getUserAccountSeq(), useYn, projectId);
	}

	public ServiceDetailVO getService(Integer serviceSeq, boolean useNamespace, boolean useLimitRange, boolean useResourceQuota, boolean useNetworkPolicy) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);

		ServiceDetailVO service = dao.getService(serviceSeq);

		// Servicemaps에 ResourceQuota 셋팅
		if (service != null && CollectionUtils.isNotEmpty(service.getServicemaps())) {
			if (useNamespace || useLimitRange || useResourceQuota || useNetworkPolicy) {
				// Map<clusterSeq, Map<namespaceName, namespace>>
				Map<Integer, Map<String, K8sNamespaceVO>> namespaceMap = Maps.newHashMap();
				// Map<clusterSeq, Map<namespaceName, List<limitRange>>>
				Map<Integer, Map<String, List<K8sLimitRangeVO>>> limitRangesMap = Maps.newHashMap();
				// Map<clusterSeq, Map<namespaceName, List<resourceQuota>>>
				Map<Integer, Map<String, List<K8sResourceQuotaVO>>> resourceQuotaMap = Maps.newHashMap();
				// Map<clusterSeq, Map<namespaceName, List<networkPolicy>>>
				Map<Integer, Map<String, List<K8sNetworkPolicyVO>>> networkPolicyMap = Maps.newHashMap();

				// cluster 정보 조회
				List<ClusterVO> clusters = clusterDao.getClusters(service.getAccount().getAccountSeq(), serviceSeq, null, null, Optional.ofNullable(service.getClusters()).orElseGet(() ->Lists.newArrayList()).stream().map(ClusterVO::getClusterSeq).collect(Collectors.toList()), "Y");

				// 각 cluster 별로 조회
				for (ClusterVO cluster : Optional.ofNullable(clusters).orElseGet(() ->Lists.newArrayList())) {
					if (clusterStateService.isClusterRunning(cluster)) {

						// Namespace
						if (useNamespace) {
							// 클러스터별로 초기값 셋팅
							namespaceMap.computeIfAbsent(cluster.getClusterSeq(), (value) -> Maps.newHashMap());

							List<K8sNamespaceVO> namespaces = Optional.ofNullable(namespaceService.getNamespacesToList(cluster, null, null, ContextHolder.exeContext())).orElseGet(() ->Lists.newArrayList());
							for (K8sNamespaceVO namespaceRow : namespaces) {
								// 네임스페이스별로 초기값 셋팅
								namespaceMap.get(cluster.getClusterSeq()).put(namespaceRow.getName(), namespaceRow);
							}
						}

						// LimitRange
						if (useLimitRange) {
							// 클러스터별로 초기값 셋팅
							limitRangesMap.computeIfAbsent(cluster.getClusterSeq(), (value) -> Maps.newHashMap());

							List<K8sLimitRangeVO> limitRanges = Optional.ofNullable(limitRangeService.getLimitRanges(cluster, null, null, null, ContextHolder.exeContext())).orElseGet(() ->Lists.newArrayList());
							for (K8sLimitRangeVO limitRangeRow : limitRanges) {
								// 네임스페이스별로 초기값 셋팅
								limitRangesMap.get(cluster.getClusterSeq()).computeIfAbsent(limitRangeRow.getNamespace(), (value) -> Lists.newArrayList());
								limitRangesMap.get(cluster.getClusterSeq()).get(limitRangeRow.getNamespace()).add(limitRangeRow);
							}
						}

						// ResourceOuota
						if (useResourceQuota) {
							// 클러스터별로 초기값 셋팅
							resourceQuotaMap.computeIfAbsent(cluster.getClusterSeq(), (value) -> Maps.newHashMap());

							List<K8sResourceQuotaVO> resourceQuotas = Optional.ofNullable(resourceQuotaService.getResourceQuotas(cluster, null, null, null, ContextHolder.exeContext())).orElseGet(() ->Lists.newArrayList());
							for (K8sResourceQuotaVO resourceQuotaRow : resourceQuotas) {
								// 네임스페이스별로 초기값 셋팅
								resourceQuotaMap.get(cluster.getClusterSeq()).computeIfAbsent(resourceQuotaRow.getNamespace(), (value) -> Lists.newArrayList());
								resourceQuotaMap.get(cluster.getClusterSeq()).get(resourceQuotaRow.getNamespace()).add(resourceQuotaRow);
							}
						}

						// NetworkPolicy
						if (useNetworkPolicy) {
							// 클러스터별로 초기값 셋팅
							networkPolicyMap.computeIfAbsent(cluster.getClusterSeq(), (value) -> Maps.newHashMap());

							List<K8sNetworkPolicyVO> networkPolicies = Optional.ofNullable(networkPolicyService.getNetworkPolicies(cluster, null, null, null, ContextHolder.exeContext())).orElseGet(() ->Lists.newArrayList());
							for (K8sNetworkPolicyVO networkPolicyRow : networkPolicies) {
								// 네임스페이스별로 초기값 셋팅
								networkPolicyMap.get(cluster.getClusterSeq()).computeIfAbsent(networkPolicyRow.getNamespace(), (value) -> Lists.newArrayList());
								networkPolicyMap.get(cluster.getClusterSeq()).get(networkPolicyRow.getNamespace()).add(networkPolicyRow);
							}
						}
					}
				}

				// 조회한 k8s resource 서비스맵 모델에 셋팅
				for (ServicemapDetailResourceVO servicemapRow : service.getServicemaps()) {
					// Namespace
					if (useNamespace) {
						if (MapUtils.getObject(namespaceMap, servicemapRow.getClusterSeq(), null) != null
								&& MapUtils.getObject(namespaceMap.get(servicemapRow.getClusterSeq()), servicemapRow.getNamespaceName(), null) != null
						) {
							K8sNamespaceVO k8sNamespace = namespaceMap.get(servicemapRow.getClusterSeq()).get(servicemapRow.getNamespaceName());
							servicemapRow.setLabels(k8sNamespace.getLabels());
							servicemapRow.setAnnotations(k8sNamespace.getAnnotations());
							servicemapRow.setK8sNamespace(k8sNamespace);
							servicemapRow.setK8sResourceExists(Boolean.TRUE);
						}
					}

					// LimitRange
					if (useLimitRange) {
						if (MapUtils.getObject(limitRangesMap, servicemapRow.getClusterSeq(), null) != null
								&& MapUtils.getObject(limitRangesMap.get(servicemapRow.getClusterSeq()), servicemapRow.getNamespaceName(), null) != null
						) {
							servicemapRow.setLimitRanges(limitRangesMap.get(servicemapRow.getClusterSeq()).get(servicemapRow.getNamespaceName()));
						}
					}

					// ResourceOuota
					if (useResourceQuota) {
						if (MapUtils.getObject(resourceQuotaMap, servicemapRow.getClusterSeq(), null) != null
								&& MapUtils.getObject(resourceQuotaMap.get(servicemapRow.getClusterSeq()), servicemapRow.getNamespaceName(), null) != null
						) {
							servicemapRow.setResourceQuotas(resourceQuotaMap.get(servicemapRow.getClusterSeq()).get(servicemapRow.getNamespaceName()));
						}
					}

					// NetworkPolicy
					if (useNetworkPolicy) {
						if (MapUtils.getObject(networkPolicyMap, servicemapRow.getClusterSeq(), null) != null
								&& MapUtils.getObject(networkPolicyMap.get(servicemapRow.getClusterSeq()), servicemapRow.getNamespaceName(), null) != null
						) {
							servicemapRow.setNetworkPolicies(networkPolicyMap.get(servicemapRow.getClusterSeq()).get(servicemapRow.getNamespaceName()));
						}
					}
				}
			}
		}

		/** KISA 보안인증 관련 로직 추가 Start : 사용자 정보 노출 취약점, 로그인한 사용자 외의 정보는 제거, 2022-12-01, coolingi **/
		Integer userSeq = ContextHolder.exeContext().getUserSeq();
		UserRole userRole = UserRole.valueOf(ContextHolder.exeContext().getUserRole());
		// account user 필터링
		if (service.getAccount() != null && CollectionUtils.isNotEmpty(service.getAccount().getAccountUsers())){
			List<UserVO> acountUsers = service.getAccount().getAccountUsers();
			acountUsers.removeIf(user -> !userSeq.equals(user.getUserSeq()));
		}

		// 접근 사용자가 devops 이면서 manager 가 아니면, 로그인한 사용자 외의 user 정보는 제거
		boolean isServiceManager = service.getUsers().stream().filter( user -> (userSeq.equals(user.getUserSeq()) && UserGrant.MANAGER.getCode().equals(user.getUserGrant())) ).findFirst().isPresent();
		if(userRole != null && userRole.isDevops() && !isServiceManager){
			List<UserVO> users = service.getUsers();
			users.removeIf(user -> !userSeq.equals(user.getUserSeq()));
		}
		/** KISA 보안인증 관련 로직 추가 End : 사용자 정보 노출 취약점, 로그인한 사용자 외의 정보는 제거, 2022-12-01, coolingi **/


		return service;
	}

	public ServiceDetailVO getService(Integer serviceSeq) throws Exception {
		return this.getService(serviceSeq, false, false, false, false);
	}

	public ServiceDetailVO getPlatformService(Integer serviceSeq) throws Exception {
		return this.getPlatformService(null, serviceSeq);
	}

	public ServiceDetailVO getPlatformService(Integer accountSeq, Integer serviceSeq) throws Exception {
		return this.getPlatformService(accountSeq, serviceSeq, "Y");
	}

	public ServiceDetailVO getPlatformService(Integer accountSeq, Integer serviceSeq, String useYn) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceByType(accountSeq, serviceSeq, ServiceType.PLATFORM.getCode(), Utils.getUseYn(useYn));
	}

	public ServiceDetailVO getNormalService(Integer serviceSeq) throws Exception {
		return this.getNormalService(null, serviceSeq);
	}

	public ServiceDetailVO getNormalService(Integer accountSeq, Integer serviceSeq) throws Exception {
		return this.getNormalService(accountSeq, serviceSeq, "Y");
	}

	public ServiceDetailVO getNormalService(Integer accountSeq, Integer serviceSeq, String useYn) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);

		return dao.getServiceByType(accountSeq, serviceSeq, ServiceType.NORMAL.getCode(), Utils.getUseYn(useYn));
	}

	public List<ServiceDetailVO> getNormalServices(Integer accountSeq, Integer serviceSeq, String useYn) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);

		return dao.getServicesByType(accountSeq, serviceSeq, ServiceType.NORMAL.getCode(), Utils.getUseYn(useYn));
	}

	public int getServicesCountByType(Integer accountSeq, Integer serviceSeq, String serviceType) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServicesCountByType(accountSeq, serviceSeq, serviceType);
	}

	public List<ServiceUserVO> getServiceUsers(Integer serviceSeq) {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		List<ServiceUserVO> currUsers = dao.getServiceUsers(serviceSeq);
		return currUsers;
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateUsersOfService(Integer serviceSeq, List<ServiceUserVO> reqUsers, Integer updater) throws Exception {
		if (reqUsers == null) {
			reqUsers = new ArrayList<>();
		}

		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);

//		ServiceVO service = dao.getService(serviceSeq);
//		List<ServiceClusterVO> currServiceClusters = dao.getClusterOfService(serviceSeq);
//		List<Integer> currClusterSeqs = Optional.ofNullable(currServiceClusters).orElseGet(() ->Lists.newArrayList()).stream().map(ServiceClusterVO::getClusterSeq).collect(Collectors.toList());

		List<ServiceUserVO> currUsers = dao.getServiceUsers(serviceSeq);
//		List<Integer> currUserSeqs = Optional.ofNullable(currUsers).orElseGet(() ->Lists.newArrayList()).stream().map(ServiceUserVO::getUserSeq).collect(Collectors.toList());

//		List<UserVO> userRelationInfos = null;
//		if (CollectionUtils.isNotEmpty(currUserSeqs)) {
//			userRelationInfos = userDao.selectByUserSeqs(currUserSeqs);
//		}

		this.initServiceUserGrantForCompare(currUsers);
		this.initServiceUserGrantForCompare(reqUsers);
		Map<Integer, ServiceUserVO> currUserMap = Optional.ofNullable(currUsers).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(ServiceUserVO::getUserSeq, Function.identity()));
		Map<Integer, ServiceUserVO> reqUserMap = Optional.ofNullable(reqUsers).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(ServiceUserVO::getUserSeq, Function.identity()));

		List<ServiceUserVO> addUsers = Lists.newArrayList();
		List<ServiceUserVO> deleteUsers = Lists.newArrayList();

		// 현재 사용자와 요청 사용자를 비교
		MapDifference<Integer, ServiceUserVO> diff = Maps.difference(currUserMap, reqUserMap);

		// 삭제 처리
		List<Integer> deleteUserSeqs = Lists.newArrayList();
		if (MapUtils.isNotEmpty(diff.entriesOnlyOnLeft())) {
			deleteUsers.addAll(new ArrayList<>(diff.entriesOnlyOnLeft().values()));
			deleteUserSeqs.addAll(deleteUsers.stream().map(ServiceUserVO::getUserSeq).collect(Collectors.toList()));

			// 삭제
			dao.deleteUsersOfService(serviceSeq, deleteUserSeqs);
		}

		// 추가 처리
		List<Integer> addUserSeqs = Lists.newArrayList();
		if (MapUtils.isNotEmpty(diff.entriesOnlyOnRight())) {
			addUsers.addAll(new ArrayList<>(diff.entriesOnlyOnRight().values()));
			addUserSeqs.addAll(addUsers.stream().map(ServiceUserVO::getUserSeq).collect(Collectors.toList()));

			// 추가
			dao.addUsersOfService(serviceSeq, addUsers, updater);

			// update 마지막 작업한 workspace
			userDao.updateUsersLastService(serviceSeq, addUserSeqs, updater);
		}

		// 수정 처리
		if (MapUtils.isNotEmpty(diff.entriesDiffering())) {
			Map<Integer, MapDifference.ValueDifference<ServiceUserVO>> differenceMap = diff.entriesDiffering();
			for (Map.Entry<Integer, MapDifference.ValueDifference<ServiceUserVO>> valueDifferenceEntry : differenceMap.entrySet()) {
				// 수정
				dao.updateUsersOfService(
						serviceSeq
						, Objects.requireNonNull(valueDifferenceEntry.getValue().rightValue()).getUserSeq()
						, Objects.requireNonNull(valueDifferenceEntry.getValue().rightValue()).getUserGrant().getCode()
						, updater);
			}
		}

		/**
		 * User Account 권한 user 삭제
		 */
//		if (CollectionUtils.isNotEmpty(userRelationInfos) && CollectionUtils.isNotEmpty(currClusterSeqs)) {
//
//			/**
//			 * cluster-tenancy가 SOFT일 경우 클러스터별로 네임스페이스 셋팅
//			 */
//			Map<Integer, List<String>> currAppmapMap = Maps.newHashMap();
//			if (service.getClusterTenancy() == ClusterTenancy.SOFT) {
//				if (CollectionUtils.isNotEmpty(service.getAppmaps())) {
//					for (AppmapVO appmap : service.getAppmaps()) {
//						if (!currAppmapMap.containsKey(appmap.getClusterSeq())) {
//							currAppmapMap.put(appmap.getClusterSeq(), Lists.newArrayList());
//						}
//
//						currAppmapMap.get(appmap.getClusterSeq()).add(appmap.getNamespaceName());
//					}
//				}
//			}
//
//
//			List<UserClusterRoleIssueVO> delShellRoles = Lists.newArrayList();
//			List<UserClusterRoleIssueVO> delKubeconfigRoles = Lists.newArrayList();
//
//			for (UserVO userRow : userRelationInfos) {
//				// 해당 사용자가 user account 권한이 있다면
//				if (CollectionUtils.isNotEmpty(userRow.getShellRoles()) || CollectionUtils.isNotEmpty(userRow.getKubeconfigRoles())) {
////					Map<Integer, UserClusterRoleIssueVO> shellRoleMap = Optional.ofNullable(userRow.getShellRoles()).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(UserClusterRoleIssueVO::getClusterSeq, Function.identity()));
////					Map<Integer, UserClusterRoleIssueVO> kubeconfigRoleMap = Optional.ofNullable(userRow.getKubeconfigRoles()).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(UserClusterRoleIssueVO::getClusterSeq, Function.identity()));
//					Map<Integer, List<UserClusterRoleIssueVO>> shellRoleMap = Maps.newHashMap();
//					Map<Integer, List<UserClusterRoleIssueVO>> kubeconfigRoleMap = Maps.newHashMap();
//					if (CollectionUtils.isNotEmpty(userRow.getShellRoles())) {
//						for (UserClusterRoleIssueVO roleIssue : userRow.getShellRoles()) {
//							if (MapUtils.getObject(shellRoleMap, roleIssue.getClusterSeq(), null) == null) {
//								shellRoleMap.put(roleIssue.getClusterSeq(), Lists.newArrayList());
//							}
//
//							shellRoleMap.get(roleIssue.getClusterSeq()).add(roleIssue);
//						}
//					}
//					if (CollectionUtils.isNotEmpty(userRow.getKubeconfigRoles())) {
//						for (UserClusterRoleIssueVO roleIssue : userRow.getKubeconfigRoles()) {
//							if (MapUtils.getObject(kubeconfigRoleMap, roleIssue.getClusterSeq(), null) == null) {
//								kubeconfigRoleMap.put(roleIssue.getClusterSeq(), Lists.newArrayList());
//							}
//
//							kubeconfigRoleMap.get(roleIssue.getClusterSeq()).add(roleIssue);
//						}
//					}
//
//					for (Integer clusterSeqRow : currClusterSeqs) {
//						/**
//						 * 삭제된 사용자
//						 *
//						 * - HARD
//						 *   : 다른 워크스페이스에 할당이 되지 않으므로 현재 할당된 클러스터 정보만으로 발급된 클러스터 계정 삭제
//						 *
//						 * - SOFT
//						 *   : 워크스페이스에 할당된 서비스맵 중 사용자에게 발급된 클러스터 계정의 네임스페이스 권한 삭제
//						 *
//						 */
//						if (service.getClusterTenancy() == ClusterTenancy.HARD) {
//							if (shellRoleMap.containsKey(clusterSeqRow)) {
//								delShellRoles.addAll(shellRoleMap.get(clusterSeqRow));
//							}
//							if (kubeconfigRoleMap.containsKey(clusterSeqRow)) {
//								delKubeconfigRoles.addAll(kubeconfigRoleMap.get(clusterSeqRow));
//							}
//						} else {
//							if (currAppmapMap.containsKey(clusterSeqRow)) {
//								if (shellRoleMap.containsKey(clusterSeqRow)) {
//									List<UserClusterRoleIssueVO> roleIssues = shellRoleMap.get(clusterSeqRow);
//
//									for (UserClusterRoleIssueVO roleIssue : roleIssues) {
//										if (IssueBindingType.valueOf(roleIssue.getBindingType()) == IssueBindingType.NAMESPACE && CollectionUtils.isNotEmpty(roleIssue.getBindings())) {
//											roleIssue.getBindings().removeIf(b -> (!currAppmapMap.get(clusterSeqRow).contains(b.getNamespace())));
//										}
//
//										delShellRoles.add(roleIssue);
//									}
//
//								}
//								if (kubeconfigRoleMap.containsKey(clusterSeqRow)) {
//									List<UserClusterRoleIssueVO> roleIssues = kubeconfigRoleMap.get(clusterSeqRow);
//
//									for (UserClusterRoleIssueVO roleIssue : roleIssues) {
//										if (IssueBindingType.valueOf(roleIssue.getBindingType()) == IssueBindingType.NAMESPACE && CollectionUtils.isNotEmpty(roleIssue.getBindings())) {
//											roleIssue.getBindings().removeIf(b -> (!currAppmapMap.get(clusterSeqRow).contains(b.getNamespace())));
//										}
//
//										delKubeconfigRoles.add(roleIssue);
//									}
//
//								}
//							}
//						}
//
//					}
//				}
//			}
//
//			// cluster-api 권한 삭제 요청
//			if (CollectionUtils.isNotEmpty(delShellRoles)) {
//				userClusterRoleIssueService.removeUserClusterRoleIssuesWithClusterApi(ContextHolder.exeContext().getUserAccountSeq(), null, IssueType.SHELL, delShellRoles, HistoryState.REVOKE, ContextHolder.exeContext().getUserSeq());
//			}
//			if (CollectionUtils.isNotEmpty(delKubeconfigRoles)) {
//				userClusterRoleIssueService.removeUserClusterRoleIssuesWithClusterApi(ContextHolder.exeContext().getUserAccountSeq(), null, IssueType.KUBECONFIG, delKubeconfigRoles, HistoryState.REVOKE, ContextHolder.exeContext().getUserSeq());
//			}
//		}

	}

	private void initServiceUserGrantForCompare(List<ServiceUserVO> serviceUsers) throws Exception {
		if (CollectionUtils.isNotEmpty(serviceUsers)) {
			for (ServiceUserVO serviceUser : serviceUsers) {
				serviceUser.setServiceSeq(null);
				serviceUser.setCreated(null);
				serviceUser.setCreator(null);
				serviceUser.setUpdated(null);
				serviceUser.setUpdater(null);
			}
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public void deleteUsersOfService(Integer serviceSeq, List<Integer> deleteUserSeqs) throws Exception {
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);

		List<Integer> currUserSeqs = serviceDao.getUserSeqsOfService(serviceSeq);
//		List<UserVO> userRelationInfos = null;
		if (CollectionUtils.isNotEmpty(currUserSeqs)) {
			deleteUserSeqs = currUserSeqs;
//			userRelationInfos = userDao.selectByUserSeqs(currUserSeqs);
		}
//		List<ServiceClusterVO> currServiceClusters = serviceDao.getClusterOfService(serviceSeq);
//		List<Integer> currClusterSeqs = Optional.ofNullable(currServiceClusters).orElseGet(() ->Lists.newArrayList()).stream().map(ServiceClusterVO::getClusterSeq).collect(Collectors.toList());
//
//		/**
//		 * User Account 권한 user 삭제
//		 */
//		if (CollectionUtils.isNotEmpty(userRelationInfos)) {
//			boolean isExistsDelUsers = false;
//			if (CollectionUtils.isNotEmpty(deleteUserSeqs)) {
//				isExistsDelUsers = true;
//			}
//			List<UserClusterRoleIssueVO> delShellRoles = Lists.newArrayList();
//			List<UserClusterRoleIssueVO> delKubeconfigRoles = Lists.newArrayList();
//
//			for (UserVO userRow : userRelationInfos) {
//
//				if (isExistsDelUsers) {
//					// 해당 사용자가 user account 권한이 있다면
//					if (CollectionUtils.isNotEmpty(userRow.getShellRoles()) || CollectionUtils.isNotEmpty(userRow.getKubeconfigRoles())) {
//						Map<Integer, UserClusterRoleIssueVO> shellRoleMap = Optional.ofNullable(userRow.getShellRoles()).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(UserClusterRoleIssueVO::getClusterSeq, Function.identity()));
//						Map<Integer, UserClusterRoleIssueVO> kubeconfigRoleMap = Optional.ofNullable(userRow.getKubeconfigRoles()).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(UserClusterRoleIssueVO::getClusterSeq, Function.identity()));
//						if (CollectionUtils.isNotEmpty(userRow.getUserRelations())) {
//							// 워크스페이스의 현재 부여된 클러스터가 해당 사용자가 속한 다른 워크스페이스에 부여된 클러스터 번호
//							Set<Integer> relationClusterSeqSet = userRow.getUserRelations().stream().filter(ur -> (ur.getServiceSeq() != null && !serviceSeq.equals(ur.getServiceSeq())) && !ur.getServiceSeq().equals(Integer.valueOf(0))).map(ServiceRelationVO::getClusterSeq).collect(Collectors.toSet());
//							List<Integer> relationClusterSeqs = Optional.ofNullable(relationClusterSeqSet).orElseGet(() ->Sets.newHashSet()).stream().collect(Collectors.toList());
//
//							if (CollectionUtils.isNotEmpty(relationClusterSeqs)) {
//								/**
//								 * 삭제된 사용자
//								 * - 현재 클러스터에서 사용자가 속한 다른 워크스페이스의 클러스터를 제외한 클러스터는 삭제
//								 * - currClusterSeqs(현재 클러스터) - relationClusterSeqs(사용자가 속한 다른 워크스페이스의 클러스터)
//								 */
//								List<Integer> delClusterSeqsForUser = ListUtils.subtract(currClusterSeqs, relationClusterSeqs);
//								if (isExistsDelUsers && deleteUserSeqs.contains(userRow.getUserSeq()) && CollectionUtils.isNotEmpty(delClusterSeqsForUser)) {
//									for (Integer clusterSeqRow : delClusterSeqsForUser) {
//										if (shellRoleMap.containsKey(clusterSeqRow)) {
//											delShellRoles.add(shellRoleMap.get(clusterSeqRow));
//										}
//										if (kubeconfigRoleMap.containsKey(clusterSeqRow)) {
//											delKubeconfigRoles.add(kubeconfigRoleMap.get(clusterSeqRow));
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//
//			// cluster-api 권한 삭제 요청
//			if (CollectionUtils.isNotEmpty(delShellRoles)) {
//				userClusterRoleIssueService.removeUserClusterRoleIssuesWithClusterApi(ContextHolder.exeContext().getUserAccountSeq(), null, IssueType.SHELL, delShellRoles, HistoryState.REVOKE, ContextHolder.exeContext().getUserSeq());
//			}
//			if (CollectionUtils.isNotEmpty(delKubeconfigRoles)) {
//				userClusterRoleIssueService.removeUserClusterRoleIssuesWithClusterApi(ContextHolder.exeContext().getUserAccountSeq(), null, IssueType.KUBECONFIG, delKubeconfigRoles, HistoryState.REVOKE, ContextHolder.exeContext().getUserSeq());
//			}
//
//		}

		// 워크스페이스 사용자 삭제
		serviceDao.deleteUsersOfService(serviceSeq, deleteUserSeqs);
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateClustersOfService(Integer serviceSeq, List<Integer> clusterSeqs, Integer updater) throws Exception {
		if (CollectionUtils.isEmpty(clusterSeqs)) {
			clusterSeqs = new ArrayList<>();
		}

		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

		// 클러스터 존재여부 체크
		if (CollectionUtils.isNotEmpty(clusterSeqs)) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

			List<ClusterVO> clusters = clusterDao.getClusters(ContextHolder.exeContext().getUserAccountSeq(), null, null, null, clusterSeqs, "Y");
			for (ClusterVO c : clusters) {
				if (!clusterSeqs.contains(c.getClusterSeq())) {
					throw new CocktailException("Some of the requested clusters do not exist.", ExceptionType.InvalidParameter, "Some of the requested clusters do not exist.");
				}
			}
		}

		List<Integer> currClusterSeqs = serviceDao.getClusterSeqsOfService(serviceSeq);
		@SuppressWarnings("unchecked")
		List<Integer> deleteClusterSeqs = ListUtils.subtract(currClusterSeqs, clusterSeqs);
		@SuppressWarnings("unchecked")
		List<Integer> addClusterSeqs = ListUtils.subtract(clusterSeqs, currClusterSeqs);

		if (CollectionUtils.isNotEmpty(deleteClusterSeqs)) {
			serviceValidService.canDeleteServiceClusters(serviceSeq, deleteClusterSeqs, ServiceType.Names.NORMAL);
			serviceDao.deleteClustersOfService(serviceSeq, deleteClusterSeqs);
		}

		if (CollectionUtils.isNotEmpty(addClusterSeqs)) {
			serviceDao.addClustersOfServiceV3(serviceSeq, addClusterSeqs, updater);
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateServicemapssOfService(Integer serviceSeq, List<ServicemapGroupMappingAddVO> servicemapsReq, Integer updater) throws Exception {
		if (CollectionUtils.isEmpty(servicemapsReq)) {
			servicemapsReq = new ArrayList<>();
		}

		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);

		// Map<servicemapSeq, ServicemapGroupMappingVO>
		Map<Integer, ServicemapGroupMappingVO> servicemapsReqMap = Maps.newHashMap();

		// 현재 워크스페이스
		ServiceDetailVO currService = serviceDao.getService(serviceSeq);

		if (currService != null) {
			// 현재 워크스페아스 > 서비스맵 그룹
			List<ServicemapGroupVO> currServicemapGroups = Optional.ofNullable(currService.getServicemapGroups()).orElseGet(Lists::newArrayList);
			Map<Integer, ServicemapGroupVO> currServicemapGroupsMap = currServicemapGroups.stream().collect(Collectors.toMap(ServicemapGroupVO::getServicemapGroupSeq, Function.identity()));
			// 현재 워크스페아스 > 서비스맵
			List<ServicemapDetailResourceVO> currServicemaps = Optional.ofNullable(currService.getServicemaps()).orElseGet(Lists::newArrayList);
			Map<Integer, ServicemapDetailResourceVO> currServicemapsMap = currServicemaps.stream().collect(Collectors.toMap(ServicemapDetailResourceVO::getServicemapSeq, Function.identity()));

			List<Integer> currServicemapGroupSeqs = Lists.newArrayList(currServicemapGroupsMap.keySet());
			List<Integer> currServicemapSeqs = Lists.newArrayList(currServicemapsMap.keySet());

			// validation
			if (CollectionUtils.isNotEmpty(servicemapsReq)) {

				// 플랫폼 > 서비스맵 조회
				List<ServicemapSummaryAdditionalVO> servicemapsOfAccount = servicemapService.getServicemapSummaries(ContextHolder.exeContext().getUserAccountSeq(), false, false, false, false);
				if (CollectionUtils.isNotEmpty(servicemapsOfAccount)) {
					Map<Integer, ServicemapSummaryAdditionalVO> servicemapSummariesMap = servicemapsOfAccount.stream().collect(Collectors.toMap(ServicemapSummaryAdditionalVO::getServicemapSeq, Function.identity()));

					for (ServicemapGroupMappingAddVO s : servicemapsReq) {
						// 플랫폼 > 서비스맵 존재여부 체크
						if (!servicemapSummariesMap.containsKey(s.getServicemapSeq())) {
							String errMsg = "Some of the requested servicemaps do not exist.";
							throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
						}
						// 플랫폼 > 워크스페이스(현재) > 서비스맵 그룹 존재여부 체크
						if (!currServicemapGroupSeqs.contains(s.getServicemapGroupSeq())) {
							String errMsg = "Some of the requested servicemap groups do not exist.";
							throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
						}
						ServicemapGroupMappingVO sgm = new ServicemapGroupMappingVO();
						sgm.setServicemapGroupSeq(s.getServicemapGroupSeq());
						sgm.setServicemapSeq(s.getServicemapSeq());
						sgm.setCreator(updater);
						servicemapsReqMap.put(s.getServicemapSeq(), sgm);
					}
				} else {
					String errMsg = "The service map does not exist on the platform.";
					throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
				}
			}

			boolean needSortInit = false;

			List<Integer> reqServicemapSeqs = Lists.newArrayList(servicemapsReqMap.keySet());
			@SuppressWarnings("unchecked")
			List<Integer> deleteServicemapSeqs = ListUtils.subtract(currServicemapSeqs, reqServicemapSeqs);
			@SuppressWarnings("unchecked")
			List<Integer> updateSerivcemapSeqs = ListUtils.intersection(reqServicemapSeqs, currServicemapSeqs);
			@SuppressWarnings("unchecked")
			List<Integer> addServicemapSeqs = ListUtils.subtract(reqServicemapSeqs, currServicemapSeqs);

			if (CollectionUtils.isNotEmpty(deleteServicemapSeqs)) {
				servicemapDao.deleteServiceServicemapMappings(serviceSeq, deleteServicemapSeqs);
				servicemapDao.deleteServicemapgroupServicemapMappings(serviceSeq, deleteServicemapSeqs);

				needSortInit = true;
			}

			// 수정은 서비스맵 그룹이 상이할 경우에 변경 처리
			if (CollectionUtils.isNotEmpty(updateSerivcemapSeqs)) {
				for (Integer servicemapSeq : updateSerivcemapSeqs) {
					Integer reqServicemapGroupSeq = servicemapsReqMap.get(servicemapSeq).getServicemapGroupSeq();
					Optional<ServicemapMappingVO> currServicemapMappingOptional = currServicemapsMap.get(servicemapSeq)
							.getServicemapMappings().stream()
							.filter(sm -> (serviceSeq.equals(sm.getServiceSeq()))).findFirst();

					if (currServicemapMappingOptional.isPresent()) {
						ServicemapMappingVO currServicemapMapping = currServicemapMappingOptional.get();

						// serivemapGroupSeq 가 같지 않을 시
						if ( !reqServicemapGroupSeq.equals(currServicemapMapping.getServicemapGroup().getServicemapGroupSeq()) ) {
							servicemapDao.updateServicemapGroupChange(
									serviceSeq
									, servicemapSeq
									, currServicemapMapping.getServicemapGroup().getServicemapGroupSeq()
									, reqServicemapGroupSeq
									, updater
							);
						}
					}
				}

				needSortInit = true;
			}

			if (CollectionUtils.isNotEmpty(addServicemapSeqs)) {
				// add service_servicemap_mapping
				servicemapDao.addServiceServicemapMappings(serviceSeq, addServicemapSeqs, updater);
				// add servicemapgroup_servicemap_mapping
				for (Integer servicemapSeq : addServicemapSeqs) {
					ServicemapGroupMappingVO addServicemapGroupMapping = servicemapsReqMap.get(servicemapSeq);
					servicemapDao.addServicemapgroupServicemapMapping(addServicemapGroupMapping);
				}

				needSortInit = true;
			}

			// 서비스맵그룹 sort 초기화 처리
			if (CollectionUtils.isNotEmpty(currServicemapGroupSeqs) && needSortInit) {
				for (Integer servicemapGroupSeq : currServicemapGroupSeqs) {
					servicemapDao.updateServicemapInitSortOrder(servicemapGroupSeq);
				}
			}

		} else {
			String errMsg = "The workspace not found.";
			throw new CocktailException(errMsg, ExceptionType.ServiceNotFound, errMsg);
		}
	}


	@Transactional(transactionManager = "transactionManager")
	public void updateBuildserversOfService(Integer serviceSeq, List<Integer> buildserverSeqs, Integer updater) throws Exception {

		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

		ServiceDetailVO serviceDetail = serviceDao.getService(serviceSeq);

		if (serviceDetail != null) {

			if (CollectionUtils.isEmpty(buildserverSeqs)) {
				buildserverSeqs = new ArrayList<>();
			}


			// 클러스터 존재여부 체크
			if (CollectionUtils.isNotEmpty(buildserverSeqs)) {
				IBuildServerMapper bsDao = sqlSession.getMapper(IBuildServerMapper.class);

				List<BuildServerVO> allBuildservers = Optional.ofNullable(bsDao.getBuildServerList(ContextHolder.exeContext().getUserAccountSeq(), null)).orElseGet(Lists::newArrayList);
				Set<Integer> allBuildserverSeqSet = allBuildservers.stream().map(BuildServerVO::getBuildServerSeq).collect(Collectors.toSet());
				for (Integer bs : buildserverSeqs) {
					if (!allBuildserverSeqSet.contains(bs)) {
						throw new CocktailException("Some of the requested build-servers do not exist.", ExceptionType.InvalidParameter, "Some of the requested build-servers do not exist.");
					}
				}
			}

			List<Integer> currBuildserverSeqs = Optional.ofNullable(serviceDetail.getBuildServers()).orElseGet(Lists::newArrayList).stream().map(BuildServerVO::getBuildServerSeq).collect(Collectors.toList());
			@SuppressWarnings("unchecked")
			List<Integer> deleteBuildserverSeqs = ListUtils.subtract(currBuildserverSeqs, buildserverSeqs);
			@SuppressWarnings("unchecked")
			List<Integer> addBuildserverSeqs = ListUtils.subtract(buildserverSeqs, currBuildserverSeqs);

			if (CollectionUtils.isNotEmpty(deleteBuildserverSeqs)) {
				serviceDao.deleteBuildserversOfService(serviceSeq, deleteBuildserverSeqs);
			}

			if (CollectionUtils.isNotEmpty(addBuildserverSeqs)) {
				serviceDao.addBuildserversOfService(serviceSeq, addBuildserverSeqs, updater);
			}
		} else {
			String errMsg = "workspace not found.";
			throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, errMsg);
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateProjectsOfService(Integer serviceSeq, List<ServiceRegistryVO> reqServiceRegistry, Integer updater, String apiVersion) throws Exception {
		if (CollectionUtils.isEmpty(reqServiceRegistry)) {
			reqServiceRegistry = new ArrayList<>();
		}

		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		List<ServiceRegistryVO> currServiceRegistry = Optional.ofNullable(dao.getServiceRegistries(serviceSeq, null, null, null)).orElseGet(() ->Lists.newArrayList());

		List<Integer> currProjectIds = Optional.ofNullable(currServiceRegistry).orElseGet(() ->Lists.newArrayList()).stream().map(ServiceRegistryVO::getProjectId).collect(Collectors.toList());
		List<Integer> reqProjectIds = reqServiceRegistry.stream().map(ServiceRegistryVO::getProjectId).collect(Collectors.toList());

		@SuppressWarnings("unchecked")
		List<Integer> deleteProjetIds = ListUtils.subtract(currProjectIds, reqProjectIds);
		@SuppressWarnings("unchecked")
		List<Integer> addProjectIds = ListUtils.subtract(reqProjectIds, currProjectIds);

		if (CollectionUtils.isNotEmpty(deleteProjetIds)) {
			// SERVICE 유형의 project는 삭제 불가 => R4.4.0 이전만 가능 (2020.06.11)
			if(ApiVersionType.V1.getType().equalsIgnoreCase(apiVersion)) {
				List<Integer> projectIdList = currServiceRegistry.stream().filter(cp -> (cp.getProjectType() == ServiceRegistryType.SERVICE)).map(ServiceRegistryVO::getProjectId).collect(Collectors.toList());
				if (CollectionUtils.isNotEmpty(ListUtils.intersection(deleteProjetIds, projectIdList))) {
					throw new CocktailException("Only one registry can be created.", ExceptionType.OnlyOneRegistryCanBeCreated);
				}
			}
			// 워크스페이스에서 해당 레지스트리를 사용하는 지 확인 => 이전 로직에서 이미 확인 (2020.06.11)
//			boolean isExistsPipeline = pipelineFlowService.checkPipelineUsingServiceSeqAndRegistryIds(serviceSeq, deleteProjetIds);
//
//			if (isExistsPipeline) {
//				throw new CocktailException("이 워크스페이스의 이미지 레지스트리를 사용하는 워크로드가 존재하여 삭제될 수 없습니다. 워크로드를 먼저 중지 후, 삭제하거나 이미지를 다른 이미지로 교체해 주세요.", ExceptionType.WorkloadUsingImageRegistryExists);
//			}
			dao.deleteProjectsOfService(serviceSeq, deleteProjetIds);
		}

		if (CollectionUtils.isNotEmpty(addProjectIds)) {
			List<ServiceRegistryVO> addServiceRegistry = reqServiceRegistry.stream().filter(sr -> (addProjectIds.contains(sr.getProjectId()))).collect(Collectors.toList());
			dao.addProjectsOfService(serviceSeq, addServiceRegistry, updater);
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public void deleteProjectsOfService(Integer serviceSeq, List<Integer> deleteProjetIds) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		dao.deleteProjectsOfService(serviceSeq, deleteProjetIds);
	}

	/**
	 * account 와 projectId 에 속한 서비스 리스트 가져오기
	 *
	 * @since 2019-09-15
	 * @param accountSeq
	 * @param projectId
	 * @return
	 */
	public List<Integer> getServiceSeqsOfProject(Integer accountSeq, Integer projectId) {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);

		return dao.getServiceSeqsOfProject(accountSeq, projectId);
	}

    public int changeRegistryUserPassword(ServiceAddVO service) {
        IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		service.setRegistryUserPassword(CryptoUtils.encryptAES(service.getRegistryUserPassword()));
        return dao.changeRegistryUserPassword(service);
    }

	public List<Integer> getServiceSeqsOfUser(Integer userSeq) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceSeqsOfUser(userSeq);
	}

	public List<Integer> getServiceSeqsOfSystem(Integer userSeq) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceSeqsOfSystem(userSeq);
	}

	public List<ServiceDetailVO> getServiceOfAccount(Integer accountSeq, String projectType) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceOfAccount(accountSeq, projectType);
	}

	public ServiceRegistryVO getServiceRegistry(Integer serviceSeq, String projectType, Integer projectId, String projectName) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceRegistry(serviceSeq, projectType, projectId, projectName);
	}

	public List<ServiceRegistryVO> getServiceRegistryOfAccount(Integer accountSeq, String projectType, Integer projectId) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceRegistryOfAccount(accountSeq, null, projectType, projectId, null);
	}

	public List<ServiceRegistryVO> getServiceRegistryOfAccount(Integer accountSeq, String projectType, Integer projectId, String serviceType) throws Exception {
		return this.getServiceRegistryOfAccount(accountSeq, null, projectType, projectId, serviceType);
	}
	public List<ServiceRegistryVO> getServiceRegistryOfAccount(Integer accountSeq, Integer serviceSeq, String projectType, Integer projectId, String serviceType) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceRegistryOfAccount(accountSeq, serviceSeq, projectType, projectId, serviceType);
	}

	public ServiceRelationVO getServiceRelation(Map<String, Object> params) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceRelation(params);
	}

	public Integer getServiceSeqByProjectId(Integer accountSeq, Integer projectId) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServiceSeqByProjectId(accountSeq, projectId);
	}

	public List<ServiceServicempGroupListVO> getServicesWithServicemapGroupByCluster(Integer clusterSeq) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		return dao.getServicesWithServicemapGroupByCluster(clusterSeq);
	}

	public ServiceAddVO createPlatformWorkspace(Integer accountSeq) throws Exception {
		return this.createDefaultWorkspace(accountSeq, ServiceType.PLATFORM, ClusterTenancy.SOFT, "Platform", "Platform Workspace", "darkgray");
	}

	public ServiceAddVO createSingleTenancyWorkspace(Integer accountSeq) throws Exception {
		return this.createDefaultWorkspace(accountSeq, ServiceType.NORMAL, ClusterTenancy.HARD, "Single Tenancy", "Single Tenancy Workspace", "default");
	}

	public ServiceAddVO createSingleTenancyWorkspace(Integer accountSeq, String serviceName, String description) throws Exception {
		return this.createDefaultWorkspace(accountSeq, ServiceType.NORMAL, ClusterTenancy.HARD, serviceName, description, "default");
	}

	/**
	 * 플랫폼(account) 생성시 기본
	 *
	 * @param accountSeq
	 * @throws Exception
	 */
	public ServiceAddVO createDefaultWorkspace(Integer accountSeq, ServiceType serviceType, ClusterTenancy clusterTenancy, String serviceName, String description, String color) throws Exception {
		ServiceAddVO service = new ServiceAddVO();
		service.setAccountSeq(accountSeq);
		service.setClusterTenancy(clusterTenancy);
		service.setServiceType(serviceType);
		service.setServiceName(serviceName);
		service.setDescription(description);
		service.setColorCode(color);

		/** Harbor Registry 사용자 정보 구성 **/
		// 생성되는 워크스페이스 (서비스)에 생성한 사용자 정보 Setting => DB Insert시에는 암호화 처리해서 넣음.
		service.setRegistryUserId(ResourceUtil.makeRegistryUserId());
		service.setRegistryUserPassword(ResourceUtil.makeRegistryUserPassword());

		/**
		 * 플랫폼 레지스트리 등록여부 체크
		 *
		 * 2021.01.05 chj
		 * 플랫폼(온라인:CCO) 등록시 레지스트리 정보를 필수 등록이나
		 * 디지털 서비스 신청과 같이
		 * 플랫폼 등록시 레지스트리를 별도로 등록할 경우에는 실제 harbor에 사용자 등록을 하지 않도록 하고
		 * 추후, 레지스트리 정보를 추가할 시에 harbor에 등록하도록 함.
		 */
		boolean addUser = true;
		AccountVO account = accountService.getAccount(accountSeq);
		if (account != null) {
			// 온라인 유형이고
			if (account.getAccountType().isOnline()) {
				AccountRegistryVO accountRegistry = accountRegistryService.getAccountRegistry(accountSeq);
				// 등록된 레지스트리 정보가 없다면 addUser = false
				if (accountRegistry == null) {
					addUser = false;
				}
			}
		}

		if (addUser) {
			// 해당 워크스페이스의 registry 사용자 등록 정보
			HarborUserReqVO registryUser = new HarborUserReqVO();
			registryUser.setUsername(service.getRegistryUserId());
			registryUser.setPassword(service.getRegistryUserPassword());
			registryUser.setRealname("workspace user");

			// get Harbor API Client
			IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(accountSeq);

			// registry 사용자 추가
			harborRegistryService.addUser(registryUser);
		}

		// 생성
		return this.addService(service);
	}

	/**
	 * 서비스 레지스트리 등록
	 * @param service
	 * @param serviceRegistries
	 */
	public void serviceRegistriesManaging(Integer accountSeq, ServiceDetailVO service, List<ServiceRegistryVO> serviceRegistries) throws Exception {

		IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
		AccountVO account = accountDao.getAccount(accountSeq);

		// harbor api client
		IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(accountSeq);

		if (account != null && service != null && harborRegistryService != null) {

			// 플랫폼 레지스트리 pull 사용자 (harbor)
			HarborUserRespVO registryPullUser = harborRegistryService.getUser(CryptoUtils.decryptAES(account.getRegistryDownloadUserId()));

			// 워크스페이스 레지스트리 사용자 (harbor)
			HarborUserRespVO registryUser = harborRegistryService.getUser(CryptoUtils.decryptAES(service.getRegistryUserId()));

			if (registryPullUser != null && registryUser != null) {

				// 플랫폼 레지스트리 pull 사용자 - harbor 모델 생성
				HarborProjectMemberVO projectPullMember = new HarborProjectMemberVO();
				projectPullMember.setEntityName(registryPullUser.getUsername());
				projectPullMember.setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());

				//워크스페이스 레지스트리 사용자 - harbor 모델 생성
				HarborProjectMemberVO projectMember = new HarborProjectMemberVO();
				projectMember.setEntityName(registryUser.getUsername());
				projectMember.setRoleId(HarborRegistryProjectMemberRole.DEVELOPER.getValue());

				IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

				if (CollectionUtils.isNotEmpty(serviceRegistries)) {
					for (ServiceRegistryVO srRow : serviceRegistries) {
						switch (srRow.getModifyType()) {
							/** Harbor에 Project생성 / 기존에 생성된 사용자를 Project Member로 등록 **/
							case C: {
								RegistryProjectVO addProject = this.addIfNotExistsProject(srRow.getProjectName(), harborRegistryService);

								if (addProject != null) { // 등록 성공
									srRow.setProjectId(addProject.getProjectId());

									// 플랫폼 레지스트리 pull 사용자 => add member to project ( role : guest )
									HarborProjectMemberVO currPullMem = harborRegistryService.getMemberOfProject(addProject.getProjectId(), projectPullMember.getEntityName());
									if (currPullMem == null) {
										harborRegistryService.addMemberToProject(addProject.getProjectId(), projectPullMember, false);
									}

									// 워크스페이스 레지스트리 사용자 => add member to project ( role : developer )
									HarborProjectMemberVO currPjtMem = harborRegistryService.getMemberOfProject(addProject.getProjectId(), projectMember.getEntityName());
									if (currPjtMem == null) {
										harborRegistryService.addMemberToProject(addProject.getProjectId(), projectMember, false);
									}

									// DB Insert
									serviceDao.addProjectsOfService(service.getServiceSeq(), Collections.singletonList(srRow), ContextHolder.exeContext().getUserSeq());

									/**
									 * Single Tenancy 플랫폼은 Single Tenancy 워크스페이스에 레지스트리를 즉시 연결 해야 함
									 **/
									// 생성은 반드시 Platform 워크스페이스에서 이루어져야 함 (당연)
									if(service.getServiceType() == ServiceType.PLATFORM) {
										// is Single Tenancy 이면 워크스페이스에 레지스트리 연결
										if(this.isSingleTenancy(service.getAccount().getAccountGrade())) {
											// Single Tenancy 워크스페이스 조회
											ServiceDetailVO singleWorkspace = serviceDao.getServiceByType(service.getAccount().getAccountSeq(), null, ServiceType.NORMAL.getCode(), "Y");
											// DB Insert (플랫폼에 연결시 SERVICE유형으로 연결되며, 이후 다른 워크스페이스에는 SHARE Type으로 연결)
											srRow.setProjectType(ServiceRegistryType.SHARE);
											serviceDao.addProjectsOfService(singleWorkspace.getServiceSeq(), Collections.singletonList(srRow), ContextHolder.exeContext().getUserSeq());

											/** Single Tenancy 서비스의 사용자를 Project에 멤버로 등록 필요 **/
											HarborUserRespVO singleTenancyRegistryUser = harborRegistryService.getUser(CryptoUtils.decryptAES(singleWorkspace.getRegistryUserId()));
											HarborProjectMemberVO singleTenancyProjectMember = new HarborProjectMemberVO();
											singleTenancyProjectMember.setEntityName(singleTenancyRegistryUser.getUsername());

											// add member ( role : developer )
											singleTenancyProjectMember.setRoleId(HarborRegistryProjectMemberRole.DEVELOPER.getValue());
											harborRegistryService.addMemberToProject(addProject.getProjectId(), singleTenancyProjectMember, false);
										}
									}
								} else {
									throw new CocktailException("fail addProjectsOfService!", ExceptionType.InvalidInputData);
								}

							}
							break;
							case U: {
								/** 수정은 Description 수정 밖에 없음... **/
								serviceDao.updateServiceRegistryDescription(srRow);
							}
							break;
							case D: {

								try {
									// 공유된 service registry도 모두 제거해줌
									List<ServiceRegistryVO> sharedRegistryInfos = this.getServiceRegistryOfAccount(service.getAccount().getAccountSeq(), ServiceRegistryType.SHARE.getCode(), srRow.getProjectId());
									if (CollectionUtils.isNotEmpty(sharedRegistryInfos)) {
										for (ServiceRegistryVO serviceRegistryRow : sharedRegistryInfos) {
											this.deleteProjectsOfService(serviceRegistryRow.getServiceSeq(), Collections.singletonList(srRow.getProjectId()));
											log.debug("Shared Project DB Deleted");
										}
									}
									// service registry DB 삭제
									this.deleteProjectsOfService(service.getServiceSeq(), Collections.singletonList(srRow.getProjectId()));
									log.debug("Project DB Deleted");
								}catch (Exception eo) {
									throw new CocktailException("Registry Delete Fail!.", eo, ExceptionType.RegistryDeleteProjectFail);
								}

								try {
									// registry server에서 project 삭제, 프로젝트내에 이미지가 존재하면 삭제 되지 않을수 있음
									harborRegistryService.deleteProject(srRow.getProjectId());
									log.debug("Project Deleted");
								}catch (Exception eo) {
									log.error(eo.getMessage(), eo); // 오류시 에러 로깅 후 진행.
								}

							}
							break;
						}
					}
				}
			}

		} else {
			throw new CocktailException("service is null", ExceptionType.InvalidInputData);
		}

	}

	public RegistryProjectVO addIfNotExistsProject(String projectName, IHarborRegistryService harborRegistryService) throws Exception {
		if (StringUtils.isNotBlank(projectName) && harborRegistryService != null) {
			RegistryProjectVO addProject = harborRegistryService.getProject(projectName);

			if (addProject == null) {
				HarborProjectReqVO projectReq = new HarborProjectReqVO();
				projectReq.setPublic(false);
				projectReq.setProjectName(projectName);
				addProject = harborRegistryService.addProject(projectReq);
			}

			return addProject;
		} else {
			return null;
		}
	}

	/**
	 * Single Tenancy 플랜일 경우만 사용..
	 * @param accountSeq
	 * @param serviceType
	 * @throws Exception
	 */
	public void removeWorkspaceByServiceType(Integer accountSeq, ServiceType serviceType) throws Exception {
		String serviceTypeStr = (serviceType != null) ? serviceType.getCode() : null;
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

		String baseLogFormat = String.format("############################### REMOVE_WORKSPACE ##### - account: [%d], updater: [%d, %s]", accountSeq, ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserRole());
		int result = 0;
		result = serviceDao.removeComponentsByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removeComponents", result);

		result = serviceDao.deleteServicemapGroupsMappingByAccount(accountSeq, serviceTypeStr);
		log.info("{}, {}: {}", baseLogFormat, "deleteServicemapGroupsMapping", result);

		result = serviceDao.removeWorkloadGroupsByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removeWorkloadGroups", result);

		result = serviceDao.removeServicemapGroupsByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removeServicemapGroups", result);

		result = serviceDao.removeServicemapsByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removeServicemaps", result);

		result = serviceDao.removePipelineRunByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removePipelineRun", result);

		result = serviceDao.removePipelineContainerByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removePipelineContainer", result);

		result = serviceDao.removePipelineWorkloadByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removePipelineWorkload", result);

		result = serviceDao.deleteServiceServicemapMappingByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "deleteServiceServicemap", result);

		boolean isDeletePlatform = false;
		boolean isDeleteNormal = false;
		if(serviceType != null) {
			if(serviceType == ServiceType.PLATFORM) {
				isDeletePlatform = true;
			}
			if(serviceType == ServiceType.NORMAL) {
				isDeleteNormal = true;
			}
		}
		else {
			isDeletePlatform = isDeleteNormal = true;
		}

		if(isDeleteNormal) {
			List<ServiceDetailVO> normalServices = this.getNormalServices(accountSeq, null, "Y");
			if (CollectionUtils.isNotEmpty(normalServices)) {
				for (ServiceDetailVO normalService : normalServices) {
					result = serviceDao.deleteAccountServiceMapping(accountSeq, normalService.getServiceSeq());
					log.info("{}, {}: {}", baseLogFormat, "deleteAccountServiceMapping (Normal Service)", result);

					result = serviceDao.removeService(normalService.getServiceSeq(), ContextHolder.exeContext().getUserSeq());
					log.info("{}, {}: {}", baseLogFormat, "remove Normal Service", result);
				}
			}
		}

		if(isDeletePlatform) {
			ServiceDetailVO platformService = this.getPlatformService(accountSeq, null);
			if(platformService != null) {
				result = serviceDao.deleteAccountServiceMapping(accountSeq, platformService.getServiceSeq());
				log.info("{}, {}: {}", baseLogFormat, "deleteAccountServiceMapping (Platform Service)", result);

				result = serviceDao.removeService(platformService.getServiceSeq(), ContextHolder.exeContext().getUserSeq());
				log.info("{}, {}: {}", baseLogFormat, "remove Platform Service", result);
			}
		}
	}

	public void deleteWorkspaceByServiceType(Integer accountSeq, ServiceType serviceType) throws Exception {
		String serviceTypeStr = (serviceType != null) ? serviceType.getCode() : null;
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
		IExternalRegistryMapper erDao = sqlSession.getMapper(IExternalRegistryMapper.class);

		String baseLogFormat = String.format("############################### REMOVE_WORKSPACE ##### - account: [%d], updater: [%d, %s]", accountSeq, ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserRole());
		int result = 0;

		result = serviceDao.deleteServicemapGroupsMappingByAccount(accountSeq, serviceTypeStr);
		log.info("{}, {}: {}", baseLogFormat, "deleteServicemapGroupsMapping", result);

		result = serviceDao.deleteServicemapGroupsByAccount(accountSeq, serviceTypeStr);
		log.info("{}, {}: {}", baseLogFormat, "deleteServicemapGroups", result);

		result = serviceDao.deleteServiceProjectsByAccount(accountSeq, serviceTypeStr);
		log.info("{}, {}: {}", baseLogFormat, "deleteServiceProjects", result);

		result = buildDao.deleteBuildStepRunByAccount(accountSeq);
		log.info("{}, {}: {}", baseLogFormat, "deleteBuildStepRun", result);

		result = buildDao.deleteBuildRunByAccount(accountSeq);
		log.info("{}, {}: {}", baseLogFormat, "deleteBuildRun", result);

		result = buildDao.deleteBuildStepByAccount(accountSeq);
		log.info("{}, {}: {}", baseLogFormat, "deleteBuildStep", result);

		result = buildDao.deleteBuildByAccount(accountSeq);
		log.info("{}, {}: {}", baseLogFormat, "deleteBuild", result);

		List<Integer> erSeqs = erDao.getExternalRegistrySeqsOfAccountMapping(accountSeq);
		if (CollectionUtils.isNotEmpty(erSeqs)) {
			result = erDao.deleteExternalRegistryAccountMapping(accountSeq, erSeqs);
			log.info("{}, {}: {}", baseLogFormat, "deleteExternalRegistryAccountMapping", result);

			result = erDao.deleteExternalRegistryServiceMappings(erSeqs);
			log.info("{}, {}: {}", baseLogFormat, "deleteExternalRegistryServiceMappings", result);

			result = erDao.deleteExternalRegistry(erSeqs);
			log.info("{}, {}: {}", baseLogFormat, "deleteExternalRegistry", result);
		}

		result = serviceDao.deleteServiceServicemapMappingByAccount(accountSeq, serviceTypeStr, ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "deleteServiceServicemap", result);

		boolean isDeletePlatform = false;
		boolean isDeleteNormal = false;
		if(serviceType != null) {
			if(serviceType == ServiceType.PLATFORM) {
				isDeletePlatform = true;
			}
			if(serviceType == ServiceType.NORMAL) {
				isDeleteNormal = true;
			}
		}
		else {
			isDeletePlatform = isDeleteNormal = true;
		}

		if(isDeleteNormal) {
			List<ServiceDetailVO> normalServices = this.getNormalServices(accountSeq, null, null);
			if (CollectionUtils.isNotEmpty(normalServices)) {
				for (ServiceDetailVO normalService : normalServices) {
					result = serviceDao.deleteAccountServiceMapping(accountSeq, normalService.getServiceSeq());
					log.info("{}, {}: {}", baseLogFormat, "deleteAccountServiceMapping (Normal Service)", result);

					result = serviceDao.deleteService(normalService.getServiceSeq());
					log.info("{}, {}: {}", baseLogFormat, "remove Normal Service", result);
				}
			}
		}

		if(isDeletePlatform) {
			ServiceDetailVO platformService = this.getPlatformService(accountSeq, null, null);
			if(platformService != null) {
				result = serviceDao.deleteAccountServiceMapping(accountSeq, platformService.getServiceSeq());
				log.info("{}, {}: {}", baseLogFormat, "deleteAccountServiceMapping (Platform Service)", result);

				result = serviceDao.deleteService(platformService.getServiceSeq());
				log.info("{}, {}: {}", baseLogFormat, "remove Platform Service", result);
			}
		}
	}

	public void removePlatformWorkspace(Integer accountSeq) throws Exception {
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        String baseLogFormat = String.format("############################### REMOVE_PLATFORM_WORKSPACE ##### - account: [%d], updater: [%d, %s]", accountSeq, ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserRole());
        int result = 0;
        result = serviceDao.removeComponentsByAccount(accountSeq, ServiceType.PLATFORM.getCode(), ContextHolder.exeContext().getUserSeq());
        log.info("{}, {}: {}", baseLogFormat, "removeComponents", result);

        result = serviceDao.removeWorkloadGroupsByAccount(accountSeq, ServiceType.PLATFORM.getCode(), ContextHolder.exeContext().getUserSeq());
        log.info("{}, {}: {}", baseLogFormat, "removeWorkloadGroups", result);

		result = serviceDao.deleteServicemapGroupsMappingByAccount(accountSeq, ServiceType.PLATFORM.getCode());
		log.info("{}, {}: {}", baseLogFormat, "deleteServicemapGroupsMapping", result);

        result = serviceDao.removeServicemapGroupsByAccount(accountSeq, ServiceType.PLATFORM.getCode(), ContextHolder.exeContext().getUserSeq());
        log.info("{}, {}: {}", baseLogFormat, "removeServicemapGroups", result);

        result = serviceDao.removeServicemapsByAccount(accountSeq, ServiceType.PLATFORM.getCode(), ContextHolder.exeContext().getUserSeq());
        log.info("{}, {}: {}", baseLogFormat, "removeServicemaps", result);

        result = serviceDao.removePipelineRunByAccount(accountSeq, ServiceType.PLATFORM.getCode(), ContextHolder.exeContext().getUserSeq());
        log.info("{}, {}: {}", baseLogFormat, "removePipelineRun", result);

        result = serviceDao.removePipelineContainerByAccount(accountSeq, ServiceType.PLATFORM.getCode(), ContextHolder.exeContext().getUserSeq());
        log.info("{}, {}: {}", baseLogFormat, "removePipelineContainer", result);

        result = serviceDao.removePipelineWorkloadByAccount(accountSeq, ServiceType.PLATFORM.getCode(), ContextHolder.exeContext().getUserSeq());
        log.info("{}, {}: {}", baseLogFormat, "removePipelineWorkload", result);

        result = serviceDao.deleteServiceServicemapMappingByAccount(accountSeq, ServiceType.PLATFORM.getCode(), ContextHolder.exeContext().getUserSeq());
        log.info("{}, {}: {}", baseLogFormat, "deleteServiceServicemap", result);

        ServiceVO platformService = this.getPlatformService(accountSeq, null);
		result = serviceDao.deleteAccountServiceMapping(accountSeq, platformService.getServiceSeq());
		log.info("{}, {}: {}", baseLogFormat, "removeAccountServiceMapping", result);

		result = serviceDao.removeService(platformService.getServiceSeq(), ContextHolder.exeContext().getUserSeq());
		log.info("{}, {}: {}", baseLogFormat, "removeServices", result);
    }

    private void addServicemapCommon(Integer serviceSeq, ServicemapAddVO servicemap) throws Exception {

		ExceptionMessageUtils.checkParameterRequired("servicemapGroupSeq", servicemap.getServicemapGroupSeq());
		ExceptionMessageUtils.checkParameterRequired("serviceSeq", servicemap.getServiceSeq());

		ServiceDetailVO service = this.getService(serviceSeq);

		boolean needMapping = false;
		if (CollectionUtils.isNotEmpty(service.getClusters())) {
			// 생성할 서비스맵의 클러스터가 워크스페이스에 존재하지 않는다면 맵핑하여 줌.
			Optional<ClusterVO> clusterOptional = service.getClusters().stream().filter(c -> (c.getClusterSeq().equals(servicemap.getClusterSeq()))).findFirst();
			if (!clusterOptional.isPresent()) {
				needMapping = true;
			}
		}
		else {
			needMapping = true;
		}

		if (needMapping) {
			throw new CocktailException("Do not have access to the cluster", ExceptionType.NotAuthorizedToResource);
		}
	}

	/**
	 * <pre>
	 * 워크스페이스 사용자(manager)용 서비스맵 생성
	 *
	 * - clusterTenancy Deprecated
	 * <strike>AS-IS : 'SOFT' 유형의 워크스페이스에서 존재하는 네임스페이스 서비스맵 생성</strike>
	 * <strike>TO-BE : 'SOFT' / 'HARD' 유형의 워크스페이스에서 존재하는 네임스페이스 서비스맵 생성 (addAppmapCommon 메서드에서 SOFT, HARD에 대한 처리를 분기하도록 함)</strike>
	 * </pre>
	 *
	 * @param serviceSeq
	 * @param servicemapAdd
	 * @return
	 * @throws Exception
	 */
    public ServicemapVO addServicemapOfService(Integer serviceSeq, ServicemapAddVO servicemapAdd) throws Exception {

    	this.addServicemapCommon(serviceSeq, servicemapAdd);

		return servicemapService.addServicemap(servicemapAdd, ContextHolder.exeContext());
	}

	/**
	 * <pre>
	 * 워크스페이스 사용자(manager)용 서비스맵 생성
	 *
	 * - clusterTenancy Deprecated
	 * <strike>AS-IS : 'SOFT' 유형의 워크스페이스에서 존재하는 네임스페이스 서비스맵 생성</strike>
	 * <strike>TO-BE : 'SOFT' / 'HARD' 유형의 워크스페이스에서 존재하는 네임스페이스 서비스맵 생성 (addAppmapCommon 메서드에서 SOFT, HARD에 대한 처리를 분기하도록 함)</strike>
	 * </pre>
	 *
	 * @param serviceSeq
	 * @param servicemapAdd
	 * @return
	 * @throws Exception
	 */
    public ServicemapVO addServicemapExistNamespaceOfService(Integer serviceSeq, ServicemapAddVO servicemapAdd) throws Exception {

    	this.addServicemapCommon(serviceSeq, servicemapAdd);

		return servicemapService.addExistServicemap(servicemapAdd, null, ContextHolder.exeContext());
	}

    /**
     * 워크스페이스 > 서비스맵 수정
	 *
     * @param serviceSeq
     * @param servicemap
     * @throws Exception
     */
	@Transactional(transactionManager = "transactionManager")
	public void updateServicemapOfService(Integer serviceSeq, ServicemapModVO servicemap) throws Exception {

		ExceptionMessageUtils.checkParameterRequired("servicemapGroupSeq", servicemap.getServicemapGroupSeq());
		ExceptionMessageUtils.checkParameterRequired("serviceSeq", servicemap.getServiceSeq());

		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		ServiceDetailVO currService = serviceDao.getService(serviceSeq);

		if (currService != null) {
			// 현재 워크스페아스 > 서비스맵
			List<ServicemapDetailResourceVO> currServicemaps = Optional.ofNullable(currService.getServicemaps()).orElseGet(Lists::newArrayList);
			Set<Integer> currServicemapsSet = currServicemaps.stream().map(ServicemapDetailResourceVO::getServicemapSeq).collect(Collectors.toSet());

			// 해당 워크스페이스에 서비스맵이 존재하는 지 체크
			if (currServicemapsSet != null && currServicemapsSet.contains(servicemap.getServicemapSeq())) {
				servicemapService.updateServicemap(servicemap, ContextHolder.exeContext());
            } else {
				String errMsg = "The requested servicemap do not exist.";
				throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
			}
		} else {
			String errMsg = "The workspace not found.";
			throw new CocktailException(errMsg, ExceptionType.ServiceNotFound, errMsg);
		}
	}

	/**
	 * 워크스페이스 > 네임스페이스(서비스맵) 수정
	 *
	 * @param serviceSeq
	 * @param clusterSeq
	 * @param namespaceName
	 * @param deployYaml
	 * @throws Exception
	 */
	public void udpateNamespaceOfServicemapByYaml(Integer serviceSeq, Integer clusterSeq, String namespaceName, K8sDeployYamlVO deployYaml) throws Exception {
		ServiceDetailVO currService = this.getService(serviceSeq);

		if (currService != null) {
			// 현재 워크스페아스 > 서비스맵
			List<ServicemapDetailResourceVO> currServicemaps = Optional.ofNullable(currService.getServicemaps()).orElseGet(Lists::newArrayList);

			// Map<clusterSeq, Set<namespaceName>>
			Map<Integer, Set<String>> namespaceOfServicemap = Maps.newHashMap();
			for (ServicemapDetailResourceVO sm : currServicemaps) {
				if (MapUtils.getObject(namespaceOfServicemap, sm.getClusterSeq(), null) == null) {
					namespaceOfServicemap.put(sm.getClusterSeq(), Sets.newHashSet());
				}

				namespaceOfServicemap.get(sm.getClusterSeq()).add(sm.getNamespaceName());
			}

			// 해당 워크스페이스에 서비스맵이 존재하는 지 체크
			if (MapUtils.isNotEmpty(namespaceOfServicemap)
					&& MapUtils.getObject(namespaceOfServicemap, clusterSeq, null) != null
					&& namespaceOfServicemap.get(clusterSeq).contains(namespaceName)
			) {
				servicemapService.udpateNamespaceOfServicemapByYaml(clusterSeq, namespaceName, deployYaml);
			} else {
				String errMsg = "The requested servicemap do not exist.";
				throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
			}
		} else {
			String errMsg = "The workspace not found.";
			throw new CocktailException(errMsg, ExceptionType.ServiceNotFound, errMsg);
		}
	}

	/**
	 * 워크스페이스 > 서비스맵 맵핑 제거
	 * 워크스페이스에서 삭제시에는 맵핑만 삭제함
	 *
	 * @param serviceSeq
	 * @param servicemapSeq
	 * @param context
	 * @throws Exception
	 */
	@Transactional(transactionManager = "transactionManager")
	public ServicemapVO removeServicemapMappingOfService(Integer serviceSeq, Integer servicemapSeq, ExecutingContextVO context) throws Exception {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		ServicemapVO servicemap = servicemapDao.getServicemap(servicemapSeq, serviceSeq);

		// 맵핑 삭제 및 서비스맵그룹 sort 초기화 처리
		if (CollectionUtils.isNotEmpty(servicemap.getServicemapMappings())) {
			for (ServicemapMappingVO smmRow : servicemap.getServicemapMappings()) {
				if (serviceSeq.equals(smmRow.getServiceSeq())) {
					if (smmRow.getServicemapGroup() != null) {
						// servicemapgroup, servicemap 맵핑 삭제
						servicemapDao.deleteServicemapgroupServicemapMappings(smmRow.getServicemapGroup().getServicemapGroupSeq(), Collections.singletonList(servicemapSeq));

						// 서비스맵그룹 sort 초기화 처리
						servicemapDao.updateServicemapInitSortOrder(smmRow.getServicemapGroup().getServicemapGroupSeq());
					}

					// service, servicemap 맵핑 제거
					servicemapDao.deleteServiceServicemapMapping(smmRow.getServiceSeq(), servicemapSeq);

					try {
						// 서비스 이벤트 처리
						this.eventService.getInstance().sendServices(smmRow.getServiceSeq(), servicemapSeq, context);
					} catch (Exception e) {
						log.error("Error! on removeServicemapMappingOfService, event sendServices", e);
					}
				}
			}
		}

		return servicemap;
	}

	/**
	 * is Single Tenancy Plan
	 * @param accountGrade
	 * @return
	 * @throws Exception
	 */
	public boolean isSingleTenancy(AccountGradeVO accountGrade) throws Exception {
		if(accountGrade != null) {
			if(StringUtils.isNotBlank(accountGrade.getGradeName())) {
				return this.isSingleTenancy(accountGrade.getGradeName());
			}
		}

		return false;
	}

	/**
	 * is Single Tenancy Plan
	 * @param gradeName
	 * @return
	 * @throws Exception
	 */
	public boolean isSingleTenancy(String gradeName) throws Exception {
		if(StringUtils.isNotBlank(gradeName)) {
			return gradeName.startsWith("STP");
		}

		return false;
	}

}
