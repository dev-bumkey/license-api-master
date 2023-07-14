package run.acloud.api.configuration.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.configuration.constants.AddonConstants;
import run.acloud.api.configuration.enums.AddonKeyItem;
import run.acloud.api.configuration.enums.ClusterState;
import run.acloud.api.configuration.enums.PublicCloudAccessType;
import run.acloud.api.configuration.service.*;
import run.acloud.api.configuration.util.AddonUtils;
import run.acloud.api.configuration.util.ClusterUtils;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.enums.VolumePlugIn;
import run.acloud.api.cserver.enums.VolumePlugInParams;
import run.acloud.api.cserver.enums.VolumeType;
import run.acloud.api.cserver.service.ServerValidService;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.service.NamespaceService;
import run.acloud.api.resource.service.PersistentVolumeService;
import run.acloud.api.resource.service.RBACResourceService;
import run.acloud.api.resource.util.K8sMapperUtils;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Cluster", description = "클러스터 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api")
@RestController
@Validated
public class ClusterController {

	@Autowired
	private AccountService accountService;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private ClusterStateService clusterStateService;

	@Autowired
	private ServerValidService serverValidService;

	@Autowired
	private ServiceService serviceService;

	@Autowired
	private ServicemapService servicemapService;

	@Autowired
	private AddonService addonService;

	@Autowired
	private K8sResourceService k8sResourceService;

	@Autowired
	private ClusterVolumeService clusterVolumeService;

	@Autowired
	private PersistentVolumeService pvService;

	@Autowired
	private NamespaceService namespaceService;

	@Autowired
	private RBACResourceService rbacResourceService;

	@Operation(summary = "클러스터 생성", description = "클러스터를 생성한다.")
	@PostMapping(value = "/cluster")
	public ClusterAddVO addCluster(
			@Parameter(description = "클러스터생성 모델", required = true) @RequestBody ClusterAddVO cluster
			) throws Exception {
        if("guntest".equals(cluster.getClusterId())) {
            throw new CocktailException("for testing........", ExceptionType.CommonFail);
        }

		/**
		 * DevOps 권한의 사용자는 수정이 불가능함.
		 */
		AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

		log.debug("[BEGIN] addCluster");

		ExceptionMessageUtils.checkParameterRequired("accountSeq", cluster.getAccountSeq());
		ExceptionMessageUtils.checkParameter("Cluster name", cluster.getClusterName(), 50, true);

		// accountSeq 유효성 체크
		UserRole userRole = UserRole.valueOf(ContextHolder.exeContext().getUserRole());
		// ADMIN : 등록된 플랫폼이 존재하는지 체크
		if (userRole == UserRole.ADMIN) {
			AccountVO account = accountService.getAccount(cluster.getAccountSeq(), false);
			if (account == null) {
				throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter);
			}
		}
		// SYSTEM : 요청한 사용자의 플랫폼 정보와 요청하는 플랫폼 정보가 같은지 체크
		else if (userRole == UserRole.SYSTEM) {
			if (!cluster.getAccountSeq().equals(ContextHolder.exeContext().getUserAccountSeq())) {
				throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter);
			}
		}

		cluster.setCreator(ContextHolder.exeContext().getUserSeq());

		clusterService.addCluster(cluster);
		ClusterUtils.setNullClusterInfo(cluster);

		log.debug("[END  ] addCluster");

		return cluster;
	}

	@Operation(summary = "클러스터 수정", description = "클러스터를 수정한다.")
	@PutMapping(value = "/cluster/{clusterSeq}")
	public ClusterAddVO updateCluster(
			@Parameter(name = "clusterSeq", description = "clusterSeq") @PathVariable Integer clusterSeq,
			@Parameter(description = "클러스터생성 모델", required = true) @RequestBody ClusterAddVO cluster
	) throws Exception {
		log.debug("[BEGIN] updateCluster");

		ExceptionMessageUtils.checkParameter("Cluster name", cluster.getClusterName(), 50, true);

		this.clusterService.convertToBase64(cluster);

		cluster.setClusterSeq(clusterSeq);
		cluster.setUpdater(ContextHolder.exeContext().getUserSeq());

//		ClusterVO currCluster = clusterService.getCluster(clusterSeq);

		clusterService.updateCluster(cluster);
		ClusterUtils.setNullClusterInfo(cluster);

		log.debug("[END  ] updateCluster");
		return cluster;
    }
	
	@Operation(summary = "클러스터 삭제", description = "클러스터를 삭제한다.")
	@DeleteMapping(value = "/cluster/{clusterSeq}")
	public ResultVO removeCluster(
			@Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "cascade", description = "종속된 정보(DB) 모두 삭제", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "cascade", required = false, defaultValue = "false") boolean cascade
	) throws Exception {
		log.debug("[BEGIN] removeCluster");

		/**
		 * header 정보로 요청 사용자 권한 체크
		 */
		AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

		ClusterVO currCluster = clusterService.getCluster(clusterSeq);

		/**
		 * Remove cluster
		 */
		clusterService.removeCluster(currCluster, cascade, ContextHolder.exeContext().getUserSeq());

		log.debug("[END  ] removeCluster");
        return new ResultVO();
    }

	@Operation(summary = "클러스터 목록", description = "모든 클러스터를 가져온다.")
	@GetMapping(value = "/cluster")
	public List<ClusterVO> getClusters(
			@Parameter(name = "accountSeq", description = "계정 번호") @RequestParam(name = "accountSeq", required = false) Integer accountSeq,
			@Parameter(name = "serviceSeq", description = "서비스 번호") @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq,
			@RequestParam(name = "useOnly", defaultValue = "n") String useOnly) throws Exception {
		log.debug("[BEGIN] getClusters");

		List<ClusterVO> clusters = clusterService.getClusters(accountSeq, serviceSeq, null);
		try {
			if (StringUtils.isNotBlank(useOnly) && useOnly.equalsIgnoreCase("y")) {
				clusters.removeIf(p -> p.getUseYn().equals("N"));
			}

			clusters = clusters.stream().map(c -> {
				ClusterUtils.setNullClusterInfo(c);
				return c;
			}).collect(Collectors.toList());

			log.debug("[END  ] getClusters");
		}
		catch (CocktailException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new CocktailException("getClusters Fail.", ex, ExceptionType.CommonInquireFail);
		}

		return clusters;
	}
	
	@Operation(summary = "클러스터 상세", description = "특정 클러스터의 정보를 가져온다.")
	@GetMapping(value = "/cluster/{clusterSeq}")
	@SuppressWarnings("unchecked")
	public ClusterDetailVO getCluster(@PathVariable Integer clusterSeq) throws Exception {
		log.debug("[BEGIN] getCluster");

		ClusterDetailVO cluster = clusterService.getClusterDetail(clusterSeq);

		if (clusterStateService.isClusterRunning(cluster)) {
			/**
			 * R3.5 : 2019.11.11 : 클러스터 내의 리소스 갯수 정보 추가
			 * node / Namespace / storage-class / pvc
			 **/
			List<K8sNodeVO> nodes = k8sResourceService.getNodes(clusterSeq, false, false, ContextHolder.exeContext());
			List<K8sNamespaceVO> namespaces = namespaceService.getNamespaces(clusterSeq,null,null,	true,true, ContextHolder.exeContext());
			List<ClusterVolumeVO> clusterVolumes = this.clusterVolumeService.getStorageVolumes(null, null, clusterSeq, null, null, false, false);
			List<K8sPersistentVolumeClaimVO> pvcs = pvService.getStorageVolumesInCluster(cluster, null, null, ContextHolder.exeContext());

			// STATIC STORAGE는 configMap으로 관리가 되어 조회된 스토리지(clusterVolumes) 중에 PERSISTENT_VOLUME_STATIC 볼륨유형에서
			// 생성된 pv 정보중 volumeSource정보가 'nfs'이고 parameter의 정보 server, path가 일치하는지 비교하여 카운트하도록 수정
			ObjectMapper mapper = K8sMapperUtils.getMapper();
			CharSequence[] nfsSrcCharSequence = VolumePlugIn.NFSSTATIC.getVolumePlugInParams().stream().map(VolumePlugInParams::getKeyName).toArray(CharSequence[]::new);
			long staticVolumeCount = pvcs.stream().filter(pvc -> (StringUtils.isBlank(pvc.getStorageClassName())))
					.filter( pvc -> {
//							pvc -> (pvc.getPersistentVolume() != null && pvc.getPersistentVolume().getDetail().getVolumeSource().contains("/storage/static"))
						if (CollectionUtils.isNotEmpty(clusterVolumes)
								&& pvc.getPersistentVolume() != null
								&& StringUtils.isNotBlank(pvc.getPersistentVolume().getDetail().getVolumeSource())
								&& StringUtils.containsAny(pvc.getPersistentVolume().getDetail().getVolumeSource(), nfsSrcCharSequence )
						) {
							// storage에서
							for (ClusterVolumeVO cv : clusterVolumes) {
								// static storage 이고 파라미터 정보가 있다면
								if (cv.getType() == VolumeType.PERSISTENT_VOLUME_STATIC && CollectionUtils.isNotEmpty(cv.getParameters())) {
									try {
										// pv의 volumeSource 정보와 static storage의 파라미터 정보(server, path)를 비교하여 카운트함.
										Map<String, Object> volumeSourceMap = mapper.readValue(pvc.getPersistentVolume().getDetail().getVolumeSource(), new TypeReference<Map<String, Object>>(){});
										if (MapUtils.isNotEmpty(volumeSourceMap) && MapUtils.getObject(volumeSourceMap, "nfs", null) != null) {
											Map<String, String> nfsSrcMap = (Map<String, String>)volumeSourceMap.get("nfs");
											Map<String, String> staticVolParamMap = cv.getParameters().stream().collect(Collectors.toMap(ClusterVolumeParamterVO::getName, ClusterVolumeParamterVO::getValue));

											if (StringUtils.equals(nfsSrcMap.get("server"), staticVolParamMap.get("server"))
													&& StringUtils.startsWith(nfsSrcMap.get("path"), staticVolParamMap.get("path"))
											) {
												return true;
											}
										}
									} catch (JsonParseException | JsonMappingException jpe) {
										log.error("error occurred in pvcs filtering.", jpe);
									} catch (IOException ioe) {
										log.error("error occurred in pvcs filtering.", ioe);
									}
								}
							}
						}
						return false;
					})
					.count();

			cluster.setCountOfNode(Optional.ofNullable(nodes).map(List::size).orElseGet(() ->0));
			cluster.setCountOfNamespace(Optional.ofNullable(namespaces).map(List::size).orElseGet(() ->0));
			cluster.setCountOfStorageClass(Optional.ofNullable(clusterVolumes).map(List::size).orElseGet(() ->0));
			cluster.setCountOfVolume(Optional.ofNullable(pvcs).map(List::size).orElseGet(() ->0));
			cluster.setCountOfStaticVolume((int)staticVolumeCount);
		}

		ClusterUtils.setNullClusterInfo(cluster);
		log.debug("[END  ] getCluster");
		return cluster;
	}

	@InHouse
	@Operation(summary = "클러스터 상세 For Pipeline Server", description = "특정 클러스터의 정보를 가져온다.")
	@GetMapping(value = "/cluster/pipeline/{clusterSeq}")
	public ClusterVO getClusterForPipeline(
			@Parameter(name = "clusterSeq", description = "클러스터 번호") @PathVariable Integer clusterSeq,
			@Parameter(name = "accessType", description = "AccessKey 요청", schema = @Schema(allowableValues = {"AWS","AWSIAM","GCP","AZR"})) @RequestParam(value = "accessType", required = false) String accessType,
			@Parameter(name = "issueAccountName", description = "AccessKey 요청에 따른 issue account name") @RequestParam(name = "issueAccountName", required = false) String issueAccountName,
			@RequestParam(name = "token", required = false) String token
	) throws Exception {
		log.debug("[BEGIN] getClusterForPipeline");

//		String jd = "{\"userName\":\"cocltailcloud-acloud@acornsoft.io-20200313\", \"arn\":\"arn:aws:iam::741408023868:user/cocltailcloud-acloud@acornsoft.io-20200313\"}";
//		String encrypted = CryptoUtils.encryptAES(jd);
//		log.debug(encrypted);

		ClusterVO cluster = null;
		if(StringUtils.isNotBlank(token) && StringUtils.equals(KubeConstants.PIPELINE_SERVER_NAME, token)){
			PublicCloudAccessType publicCloudAccessType = null;
			if(StringUtils.isNotBlank(accessType)) {
				try {
					publicCloudAccessType = PublicCloudAccessType.valueOf(accessType);
				}
				catch (Exception ex) {
					throw new CocktailException(String.format("Invalid accessType : %s", accessType), ExceptionType.InvalidParameter);
				}
			}

			cluster = clusterService.getClusterForPipeline(clusterSeq, publicCloudAccessType, issueAccountName);
		}

		log.debug("[END  ] getClusterForPipeline");
		return cluster;
	}

	@InHouse
	@Operation(summary = "Public Cloud의 AccessKey가 연결된 클러스터의 목록 조회", description = "접근 가능한 (AccessKey가 연결된) 클러스터 목록")
	@GetMapping(value = "/cluster/accessable")
	public List<ClusterVO> getAccessableClusters(
		@Parameter(name = "provider", description = "provider", schema = @Schema(allowableValues = {"AWS","GCP","AZR"})) @RequestParam(value = "provider", required = false) String provider,
        @Parameter(name = "canIam", description = "iam 권한 확인 여부(AWS)", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "canIam", required = false, defaultValue = "false") boolean canIam,
        @Parameter(name = "canLog", description = "log 권한 확인 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "canLog", required = false, defaultValue = "true") boolean canLog,
		@RequestParam(name = "token", required = false) String token
	) throws Exception {
		log.debug("[BEGIN] getAccessableClusters");

		List<ClusterVO> clusters = null;
		if(StringUtils.isNotBlank(token) && StringUtils.equals(KubeConstants.PIPELINE_SERVER_NAME, token)){
			ProviderCode providerCode = null;
			if(StringUtils.isNotBlank(provider)) {
				try {
					providerCode = ProviderCode.valueOf(provider);
				}
				catch (Exception ex) {
					throw new CocktailException(String.format("Invalid Provider Type : %s", provider), ExceptionType.InvalidParameter);
				}
			}

			clusters = clusterService.getAccessableClusters(providerCode, canIam, canLog);
		}

		log.debug("[END  ] getAccessableClusters");
		return clusters;
	}

	@Operation(summary = "클러스터 계정 검사", description = "K8S 접속 정보를 검사힌다.")
    @PostMapping(value = "/cluster/check")
	public ResultVO checkClusterAuthentication(@RequestBody ClusterAddVO cluster) throws Exception {
	    this.clusterService.convertToBase64(cluster);
        ResultVO r = new ResultVO();
        r.setResult(this.clusterService.checkClusterAuthentication(cluster, false));
        return r;
    }

	@Operation(summary = "클러스터 현황 목록", description = "모든 클러스터의 현황을 가져온다.")
	@GetMapping(value = "/cluster/v2/conditions")
	public List<ClusterDetailConditionVO> getClustersCondition(
			@Parameter(name = "accountSeq", description = "계정 번호") @RequestParam(name = "accountSeq", required = false) Integer accountSeq,
			@Parameter(name = "serviceSeq", description = "서비스 번호") @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq,
			@Parameter(name = "useK8s", description = "K8S 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useK8s", required = false, defaultValue = "false") boolean useK8s,
			@Parameter(name = "useSvc", description = "Service 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useSvc", required = false, defaultValue = "false") boolean useSvc,
			@Parameter(name = "usePvc", description = "PVC 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "usePvc", required = false, defaultValue = "false") boolean usePvc,
			@Parameter(name = "useAddon", description = "Addon 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useAddon", required = false, defaultValue = "false") boolean useAddon,
			@Parameter(name = "useFeatureGates", description = "featureGates 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useFeatureGates", required = false, defaultValue = "false") boolean useFeatureGates,
			@Parameter(name = "useNamespace", description = "Namespace 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useNamespace", required = false, defaultValue = "false") boolean useNamespace,
			@Parameter(name = "useWorkload", description = "Workload 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useWorkload", required = false, defaultValue = "false") boolean useWorkload,
			@Parameter(name = "usePod", description = "Pod 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "usePod", required = false, defaultValue = "false") boolean usePod
	) throws Exception {
		log.debug("[BEGIN] getClustersCondition");

		List<ClusterDetailConditionVO> clusters;
		try {
			ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);
			clusters = clusterService.getClusterCondition(accountSeq, serviceSeq, useK8s, useSvc, usePvc, useAddon, useFeatureGates, useNamespace, useWorkload, usePod, false, ContextHolder.exeContext());
		} catch (Exception e) {
			if(e instanceof CocktailException){
				throw e;
			}else{
				throw new CocktailException("get cluster condition fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
			}
		}

		log.debug("[END  ] getClustersCondition");
		return clusters;
	}

	@Operation(summary = "클러스터내의 Pod 목록", description = "클러스터 내의 Pod 목록을 조회한다.")
	@GetMapping(value = "/cluster/v2/{clusterSeq}/pods")
	public List<K8sPodVO> getPodsInCluster(
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq
	) throws Exception {
		log.debug("[BEGIN] getPodsInCluster");

		ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(clusterSeq);

		List<K8sPodVO> pods = clusterService.getPods(clusterSeq, null, ContextHolder.exeContext());

		log.debug("[END  ] getPodsInCluster");

		return pods;
	}

	@Operation(summary = "클러스터내의 패키지 목록", description = "클러스터 내의 패키지 목록을 조회한다.")
	@GetMapping(value = "/cluster/{apiVersion}/{clusterSeq}/packages")
	public List<HelmReleaseBaseVO> getPackagesInCluster(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "useAddon", description = "Addon도 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useAddon", required = false, defaultValue = "false") boolean useAddon
	) throws Exception {
		log.debug("[BEGIN] getPackagesInCluster");

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(clusterSeq);

		ContextHolder.exeContext().setApiVersionType(ApiVersionType.valueOf(StringUtils.upperCase(apiVersion)));

		List<HelmReleaseBaseVO> packages = clusterService.getPackages(clusterSeq, null, useAddon, ContextHolder.exeContext());

		log.debug("[END  ] getPackagesInCluster");

		return packages;
	}

	@Operation(summary = "클러스터내의 워크로드 목록", description = "클러스터 내의 워크로드 목록을 조회한다.")
	@GetMapping(value = "/cluster/{apiVersion}/{clusterSeq}/workloads")
	public List<ComponentVO> getWorkloadsInCluster(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "useExistingWorkload", description = "클러스터에 실제 리소스가 존재하는 경우만 응답", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "useExistingWorkload", required = false, defaultValue = "true") boolean useExistingWorkload
	) throws Exception {
		log.debug("[BEGIN] getWorkloadsInCluster");

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(clusterSeq);

		ContextHolder.exeContext().setApiVersionType(ApiVersionType.valueOf(StringUtils.upperCase(apiVersion)));

		List<ComponentVO> components = clusterService.getWorkloads(clusterSeq, null, true, false, useExistingWorkload, ContextHolder.exeContext());

		log.debug("[END  ] getWorkloadsInCluster");

		return components;
	}

	@Operation(summary = "클러스터내의 서비스노출 목록", description = "클러스터 내의 서비스노출 목록을 조회한다.")
	@GetMapping(value = "/cluster/{apiVersion}/{clusterSeq}/servicespecs")
	public List<K8sServiceInfoVO> getServiceSpecsInCluster(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq
	) throws Exception {
		log.debug("[BEGIN] getServiceSpecsInCluster");

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(clusterSeq);
		ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
		ContextHolder.exeContext().setApiVersionType(apiVersionType);

		List<K8sServiceInfoVO> services = clusterService.getServiceSpecs(clusterSeq, null, ContextHolder.exeContext());

		log.debug("[END  ] getServiceSpecsInCluster");

		return services;
	}

	@Operation(summary = "클러스터내의 인그레스 목록", description = "클러스터 내의 인그레스 목록을 조회한다.")
	@GetMapping(value = "/cluster/{apiVersion}/{clusterSeq}/ingressspecs")
	public List<K8sIngressInfoVO> getIngressSpecsInCluster(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq
	) throws Exception {
		log.debug("[BEGIN] getIngressSpecsInCluster");

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(clusterSeq);
		ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
		ContextHolder.exeContext().setApiVersionType(apiVersionType);

		List<K8sIngressInfoVO> ingress = clusterService.getIngressSpecs(clusterSeq, null, ContextHolder.exeContext());

		log.debug("[END  ] getIngressSpecsInCluster");

		return ingress;
	}

	@Operation(summary = "Namespace가 속한 클러스터, 워크스페이스, 서비스맵 정보 조회", description = "Namespace가 속한 클러스터, 워크스페이스, 서비스맵 정보 조회")
	@GetMapping(value = "/cluster/{apiVersion}/{clusterSeq}/namespace/{namespaceName}/relation")
	public ServicemapSummaryVO getClusterServiceServicemapInfo(
			@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
			@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName
	) throws Exception {
		log.debug("[BEGIN] getClusterServiceServicemapInfo");

		ServicemapSummaryVO result = servicemapService.getServicemapSummary(clusterSeq, namespaceName, null);

		log.debug("[END  ] getClusterServiceServicemapInfo");

		return result;
	}

	@Operation(summary = "클러스터내의 설정정보 목록", description = "클러스터 내의 설정정보 목록을 조회한다.")
	@GetMapping(value = "/cluster/{apiVersion}/{clusterSeq}/setinfos")
	public List<SettingInformationVO> getSettingInfomationsInCluster(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq
	) throws Exception {
		log.debug("[BEGIN] getSettingInfomationsInCluster");

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(clusterSeq);
		ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
		ContextHolder.exeContext().setApiVersionType(apiVersionType);

		List<SettingInformationVO> setInfos = clusterService.getSettingInfomations(clusterSeq, null, ContextHolder.exeContext());


		log.debug("[END  ] getSettingInfomationsInCluster");

		return setInfos;
	}

	@Operation(summary = "클러스터 현황 목록", description = "모든 클러스터의 현황을 가져온다.")
	@GetMapping(value = "/cluster/v2/{clusterSeq}/conditions")
	public ClusterDetailConditionVO getClusterCondition(
			@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "useK8s", description = "K8S 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useK8s", required = false, defaultValue = "false") boolean useK8s,
			@Parameter(name = "useSvc", description = "Service 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useSvc", required = false, defaultValue = "false") boolean useSvc,
			@Parameter(name = "usePvc", description = "PVC 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "usePvc", required = false, defaultValue = "false") boolean usePvc,
			@Parameter(name = "useAddon", description = "Addon 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useAddon", required = false, defaultValue = "false") boolean useAddon,
			@Parameter(name = "useFeatureGates", description = "featureGates 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useFeatureGates", required = false, defaultValue = "false") boolean useFeatureGates,
			@Parameter(name = "useNamespace", description = "Namespace 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useNamespace", required = false, defaultValue = "false") boolean useNamespace,
			@Parameter(name = "useWorkload", description = "Workload 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useWorkload", required = false, defaultValue = "false") boolean useWorkload,
			@Parameter(name = "usePod", description = "Pod 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "usePod", required = false, defaultValue = "false") boolean usePod
	) throws Exception {
		log.debug("[BEGIN] getClusterCondition");

		ClusterDetailConditionVO cluster = null;
		try {
			ExecutingContextVO ctx = new ExecutingContextVO();
			ctx.setApiVersionType(ApiVersionType.V2);

			List<ClusterDetailConditionVO> clusters = clusterService.getClusterCondition(clusterSeq, useK8s, useSvc, usePvc, useAddon, useFeatureGates, useNamespace, useWorkload, usePod, false, ctx);

			if (CollectionUtils.isNotEmpty(clusters)) {
				cluster = clusters.get(0);
			}
		} catch (Exception e) {
			if(e instanceof CocktailException){
				throw e;
			}else{
				throw new CocktailException("get cluster condition fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
			}
		}

		log.debug("[END  ] getClusterCondition");
		return cluster;
	}

	@InHouse
	@GetMapping("/cluster/v2/{clusterSeq}/nodepools")
	@Operation(summary = "Node pool 목록", description = "Node pool 목록을 반환한다.")
	public List<ClusterNodePoolVO> getNodePoolsInCluster(
			@Parameter(name = "clusterSeq", description = "클러스터 번호") @PathVariable Integer clusterSeq,
			@Parameter(name = "serviceSeq", description = "워크스페이스 번호") @RequestParam(required = false) Integer serviceSeq,
			@Parameter(name = "usePod", description = "Pod 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false"))
				@RequestParam(value = "usePod", required = false, defaultValue = "false") boolean usePod
	) throws Exception {
		List<ClusterNodePoolVO> nodePools;
		try {
			log.debug("[BEGIN] getNodePools");

			/**
			 * cluster 상태 체크
			 */
			clusterStateService.checkClusterState(clusterSeq);

			ExecutingContextVO ctx = new ExecutingContextVO();
			ctx.setApiVersionType(ApiVersionType.V2);

			nodePools = clusterService.getClusterNodePools(clusterSeq, serviceSeq, BooleanUtils.toBooleanDefaultIfNull(usePod, false), ctx);

		}
		catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.CommonInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.NODE);
		}finally {
			log.debug("[END  ] getNodePools");
		}

		return nodePools;
	}

	@InHouse
	@GetMapping("/cluster/v2/{clusterSeq}/gateways")
	@Operation(summary = "GateWay Info 목록", description = "GateWay Info 목록을 반환한다.")
	public List<ClusterGateWayVO> getGateWaysInCluster(
			@Parameter(name = "clusterSeq", description = "cluster seq", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "serviceSeq", description = "serviceSeq") @RequestParam(required = false) Integer serviceSeq
	) throws Exception {
		log.debug("[BEGIN] getGateWaysInCluster");

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(clusterSeq);

		ExecutingContextVO ctx = new ExecutingContextVO();
		ctx.setApiVersionType(ApiVersionType.V2);

		List<ClusterGateWayVO> gateWays = clusterService.getClusterGateWays(clusterSeq, serviceSeq, ctx);

		log.debug("[END  ] getGateWaysInCluster");

		return gateWays;
	}

	@InHouse
	@GetMapping("/cluster/v2/{clusterSeq}/clusterrolenames")
	@Operation(summary = "Cluster Role 이름 목록", description = "ClusterRole 이름 목록을 반환한다.")
	public List<String> ClusterRoleNames(
		@Parameter(name = "clusterSeq", description = "cluster seq", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "includeManaged", description = "Cocktail에서 Managed하는 Role만 조회", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "includeManaged", defaultValue = "true", required = false) Boolean includeManaged
	) throws Exception {
		log.debug("[BEGIN] ClusterRoleNames");

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(clusterSeq);

		ExecutingContextVO ctx = new ExecutingContextVO();
		ctx.setApiVersionType(ApiVersionType.V2);

		List<V1ClusterRole> v1ClusterRoles = rbacResourceService.getClusterRolesV1(clusterSeq, null, null);
		List<String> clusterRoles = new ArrayList<>();
		if(includeManaged) {
			for (V1ClusterRole vr : Optional.ofNullable(v1ClusterRoles).orElseGet(() ->Lists.newArrayList())) {
				if (vr != null && vr.getMetadata() != null) {
					String isCocktailClusterRole = MapUtils.getString(Optional.ofNullable(vr.getMetadata().getLabels()).orElseGet(() ->Maps.newHashMap()), KubeConstants.LABELS_ACORNSOFT_CLUSTER_ROLE, "false");
					if (
							StringUtils.equalsAnyIgnoreCase(vr.getMetadata().getName(), "cluster-admin", "admin", "edit", "view") ||
									"true".equalsIgnoreCase(isCocktailClusterRole)
					) {
						clusterRoles.add(vr.getMetadata().getName());
					}
				}
			}
		}
		else {
			clusterRoles = Optional.ofNullable(v1ClusterRoles).orElseGet(() ->Lists.newArrayList()).stream()
					.filter(cr -> cr != null && cr.getMetadata() != null)
					.map(cr -> cr.getMetadata().getName())
					.collect(Collectors.toList());
		}

		log.debug("[END  ] ClusterRoleNames");

		return clusterRoles;
	}

	@InHouse
	@Operation(summary = "클러스터 상태 수정(by clusterSeq)", description = "클러스터 상태를 수정한다.")
	@PutMapping(value = "/cluster/{clusterSeq}/{clusterState}")
	public void updateClusterStateBySeq(
			@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "clusterState", description = "cluster state", schema = @Schema(allowableValues = {"RUNNING","STOPPED"}), required = true) @PathVariable String clusterState) throws Exception {
		log.debug("[BEGIN] updateClusterStateBySeq");

		if(clusterSeq != null
				&& (StringUtils.isNotBlank(clusterState) && StringUtils.equalsAny(clusterState, Arrays.stream(ClusterState.values()).map(ClusterState::getCode).toArray(String[]::new)))){
			/**
			 * clusterSeq로 상태 수정
			 */
			clusterService.updateClusterStateBySeq(clusterSeq, clusterState);

			Thread.sleep(100);
		}else{
			throw new CocktailException("clusterSeq, clusterState is null.", ExceptionType.InvalidParameter);
		}

		log.debug("[END  ] updateClusterStateBySeq");
	}

	@InHouse
	@Operation(summary = "클러스터 상태 수정(by ClusterID)", description = "클러스터 상태를 수정한다.")
	@PutMapping(value = "/cluster/id/{clusterId}/{clusterState}")
	public void updateClusterStateById(
			@Parameter(name = "clusterId", description = "cluster ID", required = true) @PathVariable String clusterId,
			@Parameter(name = "clusterState", description = "cluster state", schema = @Schema(allowableValues = {"RUNNING","STOPPED"}), required = true) @PathVariable String clusterState) throws Exception {
		log.debug("[BEGIN] updateClusterStateById");

		if(StringUtils.isNotBlank(clusterId)
				&& (StringUtils.isNotBlank(clusterState) && StringUtils.equalsAny(clusterState, Arrays.stream(ClusterState.values()).map(ClusterState::getCode).toArray(String[]::new)))){
			/**
			 * clusterId로 상태 수정
			 */
			clusterService.updateClusterStateById(clusterId, clusterState);
		}else{
			throw new CocktailException("clusterId, clusterState is null.", ExceptionType.InvalidParameter);
		}

		log.debug("[END  ] updateClusterStateById");
	}

	@InHouse
	@Operation(summary = "클러스터 버전 동기화", description = "현재 등록된 클러스터의 버전을 조회하여 동기화 처리합니다.")
	@PutMapping(value = "/cluster/version/sync")
	public void syncClusterVersion(
			@Parameter(name = "accountSeq", description = "account sequence") @RequestParam(required = false) Integer accountSeq,
			@Parameter(name = "clusterSeq", description = "cluster sequence") @RequestParam(required = false) Integer clusterSeq
	) throws Exception {
		log.debug("[BEGIN] syncClusterVersion");

		/**
		 * header 정보로 요청 사용자 권한 체크
		 */
		AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

		clusterService.syncClusterVersion(accountSeq, clusterSeq);

		log.debug("[END  ] syncClusterVersion");
	}

	@Operation(summary = "서비스맵이 속한 클러스터 상세", description = "서비스맵이 속한 클러스터 상세")
	@GetMapping(value = "/cluster/servicemap/{servicemapSeq}")
	public ClusterVO getClusterOfServicemap(
			@Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq
	) throws Exception {
		log.debug("[BEGIN] getClusterOfServicemap");

		ClusterVO cluster = clusterService.getClusterOfServicemap(servicemapSeq);

		ClusterUtils.setNullClusterInfo(cluster);

		log.debug("[END  ] getClusterOfServicemap");

		return cluster;
	}

	@Operation(summary = "Server 이름 사용 여부 확인 Cluster에서 Unique 여부", description = "추가하려는 Server의 이름이 cluster에서 이미 사용하고 있는 것인지 검사한다 (삭제된 서버의 이름은 사용할 수 있다).")
	@GetMapping("/cluster/{clusterSeq}/servername/{serverName}/checkwithincluster")
	@Deprecated
	public ResultVO isServerNameUsedInCluster(@PathVariable Integer clusterSeq, @PathVariable String serverName) throws Exception {
		ResultVO r = new ResultVO();
		try {
			// 서버명 중복 체크시 Pod의 Label을 기준으로 체크하는데 K8s Label의 Max Length가 63글자여서 에러 발생. => 예외 처리함. => UI에서는 63글자 넘어가면 호출하지 않는 것으로 처리..
			if (StringUtils.isBlank(serverName) || serverName.length() > 63) {
				throw new CocktailException("Package Workload Name is null or empty or more than 20 characters", ExceptionType.InvalidParameter);
			}

			r.putKeyValue("exists", serverValidService.checkServerNameIfExistsInCluster(serverName, clusterSeq, true, false));
		}
		catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException("Error during data query.", e, ExceptionType.CommonInquireFail, ExceptionBiz.SERVER);
		}
		return r;
	}

	@Operation(summary = "addon configMap 목록 조회(by 클러스터 번호)", description = "클러스터 번호에 의한 addon configMap 목록 조회")
	@GetMapping("/cluster/{clusterSeq}/addons")
	public List<ConfigMapGuiVO> getAddonsInCluster(
			@Parameter(name = "clusterSeq", description = "클러스터 번", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "chartType", description = "chartType", example = "nginx-ingress") @RequestParam(required = false) String chartType,
			@Parameter(name = "useResource", description = "useResource") @RequestParam(value = "useResource", required = false, defaultValue = "false") boolean useResource,
			@Parameter(name = "useIngress", description = "useIngress") @RequestParam(value = "useIngress", required = false, defaultValue = "false") boolean useIngress,
			@Parameter(name = "useYn", description = "useYn", schema = @Schema(allowableValues = {"Y","N"})) @RequestParam(required = false) String useYn
	) throws Exception {

		log.debug("[BEGIN] getAddonsInCluster");

		List<ConfigMapGuiVO> addons = addonService.getAddonConfigMapsByChartType(clusterSeq, chartType, useYn, useResource, useIngress);

		log.debug("[END  ] getAddonsInCluster");

		return addons;
	}

	@Operation(summary = "addon configMap 목록 조회(by 클러스터 아이디)", description = "클러스터 아이디에 의한 addon configMap 목록 조회")
	@GetMapping("/cluster/id/{clusterId}/addons")
	public List<ConfigMapGuiVO> getAddonsInClusterById(
			@Parameter(name = "clusterId", description = "cluster Id", required = true) @PathVariable String clusterId,
			@Parameter(name = "chartType", description = "chartType", example = "nginx-ingress") @RequestParam(required = false) String chartType,
			@Parameter(name = "useYn", description = "useYn", schema = @Schema(allowableValues = {"Y","N"})) @RequestParam(required = false) String useYn
	) throws Exception {

		log.debug("[BEGIN] getAddonsInClusterById");

		String label = ResourceUtil.commonAddonSearchLabel(chartType);
		List<ConfigMapGuiVO> addons = addonService.getAddonConfigMaps(clusterId, label, useYn);

		log.debug("[END  ] getAddonsInClusterById");

		return addons;
	}

	@Operation(summary = "addon configMap 상세 정보 조회(by 클러스터 번호)", description = "addon configMap 정보(by 클러스터 번호)")
	@GetMapping("/cluster/{clusterSeq}/addon/{addonName}")
	public ConfigMapGuiVO getAddonInCluster(
			@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName,
			@Parameter(name = "showValueYaml", description = "Yaml 정보 표시 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "showValueYaml", required = false, defaultValue = "false") boolean showValueYaml,
			@Parameter(name = "showKiali", description = "Kiali 정보 표시 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "showKiali", required = false, defaultValue = "false") boolean showKiali
	) throws Exception {

		log.debug("[BEGIN] getAddonInCluster");

		ConfigMapGuiVO addon = addonService.getAddonConfigMap(clusterSeq, addonName, showValueYaml, showKiali);

		log.debug("[END  ] getAddonInCluster");

		return addon;
	}

	@Operation(summary = "addon configMap 상세 정보 조회(by 클러스터 아이디)", description = "addon configMap 정보(by 클러스터 아이디)")
	@GetMapping("/cluster/id/{clusterId}/addon/{addonName}")
	public ConfigMapGuiVO getAddonInClusterById(
			@Parameter(name = "clusterId", description = "cluster Id", required = true) @PathVariable String clusterId,
			@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName,
			@Parameter(name = "showValueYaml", description = "Yaml 정보 표시 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "showValueYaml", required = false, defaultValue = "false") boolean showValueYaml,
			@Parameter(name = "showKiali", description = "Kiali 정보 표시 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "showKiali", required = false, defaultValue = "false") boolean showKiali
	) throws Exception {

		log.debug("[BEGIN] getAddonInClusterById");

		ConfigMapGuiVO addon = addonService.getAddonConfigMap(clusterId, addonName, showValueYaml, showKiali);

		log.debug("[END  ] getAddonInClusterById");

		return addon;
	}

	@Operation(summary = "addon resource 정보", description = "addon resource 정보")
	@GetMapping("/cluster/{clusterSeq}/addon/{addonName}/resource")
	public ClusterAddonVO getAddonResourceInCluster(
			@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName
	) throws Exception {

		log.debug("[BEGIN] getAddonResourceInCluster");

		ClusterAddonVO addon = addonService.getClusterAddonResourceByManifest(clusterSeq, addonName);

		log.debug("[END  ] getAddonResourceInCluster");

		return addon;
	}

	@Deprecated
	@Operation(summary = "addon 수정", description = "addon 수정")
	@PutMapping("/cluster/{clusterSeq}/addon/{addonName}")
	public ConfigMapGuiVO updateAddonInCluster(
			@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName,
			@Parameter(name = "addon configMap 모델", description = "addon configMap 모델", required = true) @RequestBody ConfigMapGuiVO addon
	) throws Exception {

		log.debug("[BEGIN] updateAddonInCluster");

		if ( addon == null
				|| MapUtils.isEmpty(addon.getData())
				|| !addon.getData().containsKey(AddonKeyItem.VALUE_YAML.getValue())
				|| !addon.getData().containsKey(AddonKeyItem.AUTO_UPDATE.getValue())
		) {
			throw new CocktailException("Invalid Parameter!!", ExceptionType.InvalidParameter, addon);
		} else {
			if (!AddonUtils.isValidYaml(MapUtils.getString(addon.getData(), AddonKeyItem.VALUE_YAML.getValue()))) {
				throw new CocktailException("Invalid Parameter(yaml)!!", ExceptionType.InvalidParameter, addon);
			}
		}

		ConfigMapGuiVO result = addonService.updateAddon(clusterSeq, addon);

		log.debug("[END  ] updateAddonInCluster");

		return result;
	}

	@Deprecated
	@Operation(summary = "addon 삭제", description = "addon 삭제")
	@DeleteMapping("/cluster/{clusterSeq}/addon/{addonName}")
	public void deleteAddonInCluster(
			@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName
	) throws Exception {

		log.debug("[BEGIN] deleteAddonInCluster");

		if (StringUtils.equals(addonName, AddonConstants.CHART_NAME_ADDON_MANAGER)) {
			throw new CocktailException("addon-manager does not delete!!", ExceptionType.InvalidInputData);
		}
		addonService.deleteAddon(clusterSeq, addonName);

		log.debug("[END  ] deleteAddonInCluster");

	}

	@Operation(summary = "addon rollback", description = "addon rollback")
	@PutMapping("/cluster/{clusterSeq}/addon/{addonName}/rollback")
	public void rollbackAddonInCluster(
			@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName
	) throws Exception {

		log.debug("[BEGIN] rollbackAddonInCluster");

		addonService.rollbackAddon(clusterSeq, addonName);

		log.debug("[END  ] rollbackAddonInCluster");

	}

	@Operation(summary = "addon 재배포", description = "addon 재배포")
	@PutMapping("/cluster/{clusterSeq}/addon/{addonName}/redeploy")
	public void redeployAddonInCluster(
			@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName
	) throws Exception {

		log.debug("[BEGIN] redeployAddonInCluster");

		if (StringUtils.equals(addonName, AddonConstants.CHART_NAME_ADDON_MANAGER)) {
			throw new CocktailException("addon-manager does not redeploy!!", ExceptionType.InvalidInputData);
		}
		addonService.redeployAddon(clusterSeq, addonName);

		log.debug("[END  ] redeployAddonInCluster");

	}

	@Operation(summary = "클러스터에 속한 워크스페이스 목록", description = "클러스터에 속한 워크스페이스 목록 (servicemap group 포함)")
	@PostMapping("/cluster/{clusterSeq}/services")
	public List<ServiceServicempGroupListVO> getServicesWithServicemapGroupByCluster(
			@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq
	) throws Exception {

		log.debug("[BEGIN] getServicesWithServicemapGroupByCluster");

		List<ServiceServicempGroupListVO> services = serviceService.getServicesWithServicemapGroupByCluster(clusterSeq);

		log.debug("[END  ] getServicesWithServicemapGroupByCluster");

		return services;
	}

	@Operation(summary = "설치 가능한 전체 Addon 목록", description = "설치 가능한 전체 Addon 목록")
	@GetMapping("/cluster/{clusterSeq}/addons/installable")
	public List<AddonInfoBaseVO> getInstallableAddons(
		@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq
	) throws Exception {

		log.debug("[BEGIN] getInstallableAddons");

		String label = String.format("%s!=%s", KubeConstants.LABELS_ADDON_CHART_KEY, "cocktail");
		List<AddonInfoBaseVO> addons = addonService.getInstallableAddons(clusterSeq, label);

		log.debug("[END  ] getInstallableAddons");

		return addons;
	}

	@Operation(summary = "설치 가능한 전체 Addon 목록", description = "설치 가능한 전체 Addon 목록")
	@GetMapping("/cluster/id/{clusterId}/addons/installable")
	public List<AddonInfoBaseVO> getInstallableAddonsById(
		@Parameter(name = "clusterId", description = "cluster Id", required = true) @PathVariable String clusterId
	) throws Exception {

		log.debug("[BEGIN] getInstallableAddonsById");

		String label = String.format("%s!=%s", KubeConstants.LABELS_ADDON_CHART_KEY, "cocktail");
		List<AddonInfoBaseVO> addons = addonService.getInstallableAddons(clusterId, label);

		log.debug("[END  ] getInstallableAddonsById");

		return addons;
	}

	@Operation(summary = "addon 생성", description = "addon 생성")
	@PostMapping("/cluster/{apiVersion}/{clusterSeq}/addon/{addonName}")
	public List<ConfigMapGuiVO> addonInstallInCluster(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2","v3"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName,
		@Parameter(name = "addon configMap 모델", description = "addon configMap 모델", required = true) @RequestBody AddonInstallVO addonInstall
	) throws Exception {

		log.debug("[BEGIN] addonInstallInCluster");
		addonService.addonDefaultValidation(addonInstall, addonName, ApiVersionType.valueOf(apiVersion.toUpperCase()));
		List<ConfigMapGuiVO> result = addonService.installAddon(clusterSeq, addonInstall, apiVersion);
		log.debug("[END  ] addonInstallInCluster");

		return result;
	}

	@Operation(summary = "addon 수정", description = "addon 수정")
	@PutMapping("/cluster/{apiVersion}/{clusterSeq}/addon/{addonName}")
	public List<ConfigMapGuiVO> upgradeAddonInCluster(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2","v3"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName,
		@Parameter(name = "addon configMap 모델", description = "addon configMap 모델", required = true) @RequestBody AddonInstallVO addonUpgrade
	) throws Exception {

		log.debug("[BEGIN] upgradeAddonInCluster");
		addonService.addonDefaultValidation(addonUpgrade, addonName, ApiVersionType.valueOf(apiVersion.toUpperCase()));
		List<ConfigMapGuiVO> result = addonService.upgradeAddon(clusterSeq, addonUpgrade, apiVersion);
		log.debug("[END  ] upgradeAddonInCluster");

		return result;
	}

	@Operation(summary = "addon 삭제", description = "addon 삭제")
	@DeleteMapping("/cluster/{apiVersion}/{clusterSeq}/addon/{addonName}")
	public void deleteAddonInCluster(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "addonName", description = "addon Name", required = true) @PathVariable String addonName
	) throws Exception {

		log.debug("[BEGIN] deleteAddonInCluster");

//		if (StringUtils.equals(addonName, AddonConstants.CHART_NAME_ADDON_MANAGER)) {
//			throw new CocktailException("addon-manager does not delete!!", ExceptionType.InvalidInputData);
//		}

		addonService.deleteAddon(clusterSeq, addonName, apiVersion);

		log.debug("[END  ] deleteAddonInCluster");

	}
}
