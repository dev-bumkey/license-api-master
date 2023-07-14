package run.acloud.api.configuration.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.VersionInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.build.service.BuildServerService;
import run.acloud.api.catalog.service.PackageInfoService;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.dao.IProviderAccountMapper;
import run.acloud.api.configuration.enums.*;
import run.acloud.api.configuration.util.ClusterUtils;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.service.ServerStateService;
import run.acloud.api.cserver.service.ServiceValidService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.ServicemapDetailVO;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.task.K8sTokenGenerator;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.client.ClusterApiClient;
import run.acloud.commons.provider.K8sClient;
import run.acloud.commons.util.*;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailAddonProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClusterService {

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private ClusterVolumeService clusterVolumeService;

	@Autowired
	private ServiceValidService serviceValidService;

	@Autowired
	private K8sResourceService k8sResourceService;

	@Autowired
	private CRDResourceService crdResourceService;

	@Autowired
	private IngressSpecService ingressSpecService;

	@Autowired
	private ServiceSpecService serviceSpecService;

	@Autowired
	private ClusterStateService clusterStateService;

	@Autowired
	private ProviderAccountService providerAccountService;

	@Autowired
	private AccountGradeService accountGradeService;

	@Autowired
	private UserClusterRoleIssueService userClusterRoleIssueService;

	@Autowired
	private K8sClient k8sClient;

	@Autowired
	private SignatureUtils signatureUtils;

	@Autowired
	private K8sTokenGenerator k8sTokenGenerator;

	@Autowired
	private AddonService addonService;

	@Autowired
	private ServicemapService servicemapService;

	@Autowired
	private ServerStateService serverStateService;

	@Autowired
	private PackageInfoService packageInfoService;

	@Autowired
	private UserService userService;

	@Autowired
	private ConfigMapService configMapService;

	@Autowired
	private SecretService secretService;

	@Autowired
	private NamespaceService namespaceService;

	@Autowired
	private WorkloadResourceService workloadResourceService;

	@Autowired
	private GradePlanService gradePlanService;

	@Autowired
	private CocktailAddonProperties cocktailAddonProperties;

	@Autowired
	private ClusterApiClient clusterApiClient;

	@Autowired
	private BuildServerService buildServerService;

	@Transactional(transactionManager = "transactionManager")
	public void addCluster(ClusterAddVO cluster) throws Exception {
		if (cluster != null) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
			IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

			List<ProviderVO> providers = ProviderCode.getProviderList();

			/** Single Tenancy 유형인데 Workspace가 여러개일 경우 처리 종 **/
			AccountGradeVO paramAccountGrade = new AccountGradeVO();
			paramAccountGrade.setAccountSeq(cluster.getAccountSeq()); //account seq & use_yn Y 인 1건 조회
			AccountGradeVO accountGrade = accountGradeService.getAccountGrade(paramAccountGrade);

			if(gradePlanService.isSingleTenancy(accountGrade)) {
				int count = serviceDao.getServicesCountByType(cluster.getAccountSeq(), null, ServiceType.NORMAL.getCode());
				if(count != 1) {
					// Single Tenancy가 워크스페이스를 여러개 가지고 있거나 없다면 오류 -> 아래에서 어느 워크스페이스에 연결해야 할지 판단 어려움. 사전 체크.
					throw new CocktailException("Invalid Account - Invalid Single Tenancy Platform", ExceptionType.InvalidState);
				}
			}

			this.convertToBase64(cluster);

			// Token 인증방식 cluster의 token정보 발급 및 token정보 셋팅
			k8sTokenGenerator.initClusterToken(cluster);

			this.validCluster(cluster, clusterDao);

			ProviderAccountVO billingProviderAccount = null;
			if(cluster.getBillingProviderAccountSeq() != null){
				billingProviderAccount = providerAccountService.getProviderAccount(cluster.getBillingProviderAccountSeq());
			}

			if(cluster.getProviderAccount() != null){
				cluster.getProviderAccount().setAccountSeq(cluster.getAccountSeq());
				cluster.getProviderAccount().setProviderName(cluster.getClusterName());
				if(billingProviderAccount != null && cluster.getProviderAccount().getProviderCode().canMetering()){
					cluster.getProviderAccount().setConfig(billingProviderAccount.getConfig());
				}
				/**
				 * add provider_account
				 */
				providerAccountService.addProviderAccount(cluster.getProviderAccount());
				cluster.setProviderAccountSeq(cluster.getProviderAccount().getProviderAccountSeq());

				/**
				 * add account & provider_account mapping
				 */
				List<Integer> providerSeqs = Collections.singletonList(cluster.getProviderAccount().getProviderAccountSeq());
				if(CollectionUtils.isNotEmpty(providerSeqs)) {
					IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
					dao.addProviderOfAccount(cluster.getProviderAccount().getAccountSeq(), providerSeqs, cluster.getProviderAccount().getCreator());
				}

				// supported 정보 셋팅
				for (ProviderVO p : providers) {
					if (p.getProviderCode() == cluster.getProviderAccount().getProviderCode()) {
						if (CollectionUtils.isNotEmpty(p.getSupportCubeTypes())) {
							for (CubeTypeVO ct : p.getSupportCubeTypes()) {
								if (ct.getCubeType() == cluster.getCubeType()) {
									cluster.setIngressSupported(ct.getIngressSupported());
									cluster.setLoadbalancerSupported(ct.getLoadbalancerSupported());
									cluster.setNodePortSupported(ct.getNodePortSupported());
									cluster.setPersistentVolumeSupported(ct.getPersistentVolumeSupported());
									break;
								}
							}
						}
					}
				}
			}

			cluster.setClusterState(ClusterState.RUNNING.getCode());
			cluster.setUseYn("Y");
			clusterDao.addCluster(cluster);

			/**
			 * 생성된 Cluster를 워크스페이스에 연결.
			 */
			ServiceVO targetService;
			if(!gradePlanService.isSingleTenancy(accountGrade)) {
				/** is not Single Tenancy : Platform Workspace에 연결 **/
				targetService = serviceDao.getServiceByType(cluster.getAccountSeq(), null, ServiceType.PLATFORM.getCode(), "Y");
			}
			else {
				/** is Single Tenancy : Single Tenancy Workspace에 연결 **/
				targetService = serviceDao.getServiceByType(cluster.getAccountSeq(), null, ServiceType.NORMAL.getCode(), "Y");
			}
			// 서비스-클러스터 모델에 셋팅
			List<ServiceClusterVO> serviceClusters = Lists.newArrayList();
			ServiceClusterVO serviceCluster = new ServiceClusterVO();
			serviceCluster.setClusterSeq(cluster.getClusterSeq());
			serviceClusters.add(serviceCluster);
			// 등록
			serviceDao.addClustersOfService(targetService.getServiceSeq(), serviceClusters, cluster.getCreator());

			/**
			 * 기본 PSP 생성
			 * EKS는 버전에 따라 라벨만 생성하거나 지원안됨
			 *
			 * - hjchoi.20201020
			 * 없다면 화면에서 생성토록 함. 자동생성 주석
			 */
//		podSecurityPolicyService.createDefaultPodSecurityPolicyWithRBAC(JsonUtils.fromGson(JsonUtils.toGson(cluster), ClusterVO.class));

			/**
			 * 해당 클러스터에 기존에 배포된 빌드서버가 존재한다면 DB에 등록처리
			 */
			ClusterVO currCluster = clusterDao.getCluster(cluster.getClusterSeq());
			buildServerService.addBuildServerIfExists(currCluster, cluster.getAccountSeq());

			if(ContextHolder.isTest()) {
				throw new CocktailException("for testing........", ExceptionType.CommonFail);
			}
		} else {
			throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
		}
	}

//	@Transactional(transactionManager = "transactionManager")
//	@Deprecated
//	public void addStorage(ClusterAddVO cluster) throws Exception {
//		// k8s storageClass 조회
//		ExecutingContextVO context = new ExecutingContextVO();
//		List<K8sStorageClassVO> k8sStorageClasses = storageClassService.getStorageClasses(cluster.getClusterSeq(), null, KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE, context);
//
//		if (CollectionUtils.isNotEmpty(k8sStorageClasses)) {
//			for (K8sStorageClassVO k8sStorageClassRow : k8sStorageClasses) {
//
//				String storageTypeLabel = MapUtils.getString(k8sStorageClassRow.getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE, "");
//				String provisionerTypeLabel = MapUtils.getString(k8sStorageClassRow.getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_PROVISIONER_TYPE, "");
//
//				if (StringUtils.isNotBlank(provisionerTypeLabel)) {
//
//					Optional<VolumePlugIn> volumePlugInOptional = Arrays.stream(VolumePlugIn.values()).filter(v -> (v == VolumePlugIn.valueOf(provisionerTypeLabel))).findFirst();
//
//					if (volumePlugInOptional.isPresent()) {
//						ClusterVolumeVO clusterVolume = new ClusterVolumeVO();
//						clusterVolume.setCreator(cluster.getCreator());
//						clusterVolume.setClusterSeq(cluster.getClusterSeq());
//						clusterVolume.setName(k8sStorageClassRow.getName());
//						clusterVolume.setLabels(k8sStorageClassRow.getDetail().getLabels());
//						clusterVolume.setAnnotations(k8sStorageClassRow.getDetail().getAnnotations());
//						clusterVolume.setDescription(clusterVolume.getName());
//						clusterVolume.setStorageType(volumePlugInOptional.get().getStorageType());
//						clusterVolume.setBaseStorageYn(
//								( BooleanUtils.toBoolean(MapUtils.getString(k8sStorageClassRow.getDetail().getAnnotations(), KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT, "false"))
//										|| BooleanUtils.toBoolean(MapUtils.getString(k8sStorageClassRow.getDetail().getAnnotations(), KubeConstants.META_ANNOTATIONS_RELEASE_STORAGE_CLASS_IS_DEFAULT, "false")) )
//								? "Y" : "N"
//						);
//						clusterVolume.setType(VolumeType.PERSISTENT_VOLUME);
//						clusterVolume.setPlugin(volumePlugInOptional.get());
//						clusterVolume.setReclaimPolicy(ReclaimPolicy.getReclaimPolicyOfValue(k8sStorageClassRow.getDetail().getReclaimPolicy()));
//						clusterVolume.setStorageClassName(k8sStorageClassRow.getName());
//						clusterVolume.setProvisionerName(k8sStorageClassRow.getProvisioner());
//						clusterVolume.setCapacity(0);
//						// NFS 총용량 셋팅
//						if (volumePlugInOptional.get().haveTotalCapacity()) {
//							String totalCapacityLabel = MapUtils.getString(k8sStorageClassRow.getLabels(), KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_TOTAL_CAPACITY, "");
//							if (StringUtils.isNotBlank(totalCapacityLabel)) {
//								clusterVolume.setTotalCapacity(Integer.parseInt(totalCapacityLabel));
//							} else {
//								clusterVolume.setTotalCapacity(0);
//							}
//						}
//
//						if (MapUtils.isNotEmpty(k8sStorageClassRow.getParameters())) {
//							List<ClusterVolumeParamterVO> clusterVolumeParamters = new ArrayList<>();
//							for (Map.Entry<String, String> paramRow : k8sStorageClassRow.getParameters().entrySet()) {
//								ClusterVolumeParamterVO parameter = new ClusterVolumeParamterVO();
//								parameter.setName(paramRow.getKey());
//								parameter.setValue(paramRow.getValue());
//								clusterVolumeParamters.add(parameter);
//							}
//							clusterVolume.setParameters(clusterVolumeParamters);
//						}
//
//						// storage DB 등록
//						clusterVolumeService.addClusterVolume(clusterVolume, false);
//					}
//				}
//			}
//		}
//	}

	public void convertToBase64(ClusterAddVO cluster) throws Exception {

		if (StringUtils.isNotBlank(cluster.getApiUrl())) {
			cluster.setApiUrl(StringUtils.removeEnd(cluster.getApiUrl().trim(), "/"));
		}
		if (StringUtils.isNotBlank(cluster.getApiSecret())) {
			cluster.setApiSecret(CryptoUtils.encryptAES(cluster.getApiSecret()));
		}

		if (cluster.getAuthType() == AuthType.CERT) {
			/**
			 * Set CA Certification
			 */
			this.setServerAuthData(cluster);
			/**
			 * Set Client Certificate
			 */
			if (StringUtils.isNotBlank(cluster.getClientAuthData())) /*&& !Base64.isBase64(cluster.getClientAuthData()))*/ {
				if (StringUtils.contains(cluster.getClientAuthData(), "-----BEGIN ")) {
					cluster.setClientAuthData(Base64Utils.encodeToString(cluster.getClientAuthData().getBytes()));
				} else {
					try {
						String decodedStr = new String(Base64Utils.decodeFromString(cluster.getClientAuthData()), StandardCharsets.UTF_8);
						if (!StringUtils.contains(decodedStr, "-----BEGIN ")) {
							throw new CocktailException("Invalid Cluster - Client Certificate Data!!", ExceptionType.InvalidClusterCertification);
						}
					} catch (CocktailException e) {
						throw new CocktailException("Invalid Cluster - Client Certificate Data - bad base64 encoding!!", e, ExceptionType.InvalidClusterCertification);
					}
				}
				cluster.setClientAuthData(CryptoUtils.encryptAES(cluster.getClientAuthData()));
//                log.debug("Client cert: {}", cluster.getClientAuthData());
			}
			/**
			 * Set Client Key
			 */
			if (StringUtils.isNotBlank(cluster.getClientKeyData())) /*&& !Base64.isBase64(cluster.getClientKeyData()))*/ {
				if (StringUtils.contains(cluster.getClientKeyData(), "-----BEGIN ")) {
					cluster.setClientKeyData(Base64Utils.encodeToString(cluster.getClientKeyData().getBytes()));
				} else {
					try {
						String decodedStr = new String(Base64Utils.decodeFromString(cluster.getClientKeyData()), StandardCharsets.UTF_8);
						if (!StringUtils.contains(decodedStr, "-----BEGIN ")) {
							throw new CocktailException("Invalid Cluster - Client Key Data!!", ExceptionType.InvalidClusterCertification);
						}
					} catch (CocktailException e) {
						throw new CocktailException("Invalid Cluster - Client Key Data - bad base64 encoding!!", e, ExceptionType.InvalidClusterCertification);
					}
				}
				cluster.setClientKeyData(CryptoUtils.encryptAES(cluster.getClientKeyData()));
//                log.debug("Client key: {}", cluster.getClientKeyData());
			}
		} else if(cluster.getAuthType() == AuthType.TOKEN){
			/**
			 * Set CA Certification
			 */
			this.setServerAuthData(cluster);

			// 2021-09-06, coolingi, AKS도 admin credential을 사용하면, client token 값 존재함. AKS 조건도 추가
			if(EnumSet.of(CubeType.EKS, CubeType.AKS, CubeType.NCPKS).contains(cluster.getCubeType())){

				if (StringUtils.isNotBlank(cluster.getClientAuthData())) {
					cluster.setClientAuthData(CryptoUtils.encryptAES(cluster.getClientAuthData()));
				}

				if (StringUtils.isNotBlank(cluster.getClientKeyData())) {
					cluster.setClientKeyData(CryptoUtils.encryptAES(cluster.getClientKeyData()));
				}
			}
		}
	}

	private void setServerAuthData(ClusterAddVO cluster) throws Exception{
		if (StringUtils.isNotBlank(cluster.getServerAuthData())) {
			if (StringUtils.contains(cluster.getServerAuthData(), "-----BEGIN ")) {
				cluster.setServerAuthData(Base64Utils.encodeToString(cluster.getServerAuthData().getBytes()));
			} else {
				try {
					String decodedStr = new String(Base64Utils.decodeFromString(cluster.getServerAuthData()), StandardCharsets.UTF_8);
					if (!StringUtils.contains(decodedStr, "-----BEGIN ")) {
						throw new CocktailException("Invalid Cluster - CA Certification!!", ExceptionType.InvalidClusterCertification);
					}
				} catch (Exception e) {
					throw new CocktailException("Invalid Cluster - CA Certification - bad base64 encoding!!", ExceptionType.InvalidClusterCertification);
				}
			}
			cluster.setServerAuthData(CryptoUtils.encryptAES(cluster.getServerAuthData()));
		}
	}

	private String toBase64(String source) {
		Base64 base64 = new Base64();
		return base64.encodeToString(source.getBytes());
	}

	public List<ClusterVO> getClusters() throws Exception {
		return this.getClusters(null, null);
	}

	/**
	 * Service(워크스페이스)에 해당하는 Cluster 목록 조회.
	 *
	 * @return
	 */
	public List<ClusterVO> getClusters(Integer accountSeq, Integer serviceSeq) throws Exception {
		return this.getClusters(accountSeq, serviceSeq, "Y");
	}
	public List<ClusterVO> getClusters(Integer accountSeq, Integer serviceSeq, String useYn) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		return clusterDao.getClusters(accountSeq, serviceSeq, null, null, null, Utils.getUseYn(useYn));
	}

	public ClusterVO getCluster(Integer clusterSeq) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getCluster(clusterSeq);

		return cluster;
	}

	public ClusterDetailVO getClusterDetail(Integer clusterSeq) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterDetailVO cluster = clusterDao.getClusterDetail(clusterSeq);
		if (cluster != null) {
			cluster.setIngressHosts(this.getIngressHosts(cluster));
		}

		return cluster;
	}

	public ClusterVO getClusterByClusterId(String clusterId) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		return clusterDao.getClusterByClusterId(clusterId, "Y");
	}

	public ClusterVO getClusterForPipeline(Integer clusterSeq) throws Exception {
		return this.getClusterForPipeline(clusterSeq, null, null);
	}

	public ClusterVO getClusterForPipeline(Integer clusterSeq, PublicCloudAccessType accessType, String issueAccountName) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

		ClusterVO cluster = clusterDao.getClusterByUseYn(clusterSeq, "Y");

		if (cluster != null) {
			k8sTokenGenerator.refreshClusterToken(cluster);
			cluster.setApiSecret(CryptoUtils.decryptAES(cluster.getApiSecret()));
			cluster.setClientAuthData(CryptoUtils.decryptAES(cluster.getClientAuthData()));
			cluster.setClientKeyData(CryptoUtils.decryptAES(cluster.getClientKeyData()));
			cluster.setServerAuthData(CryptoUtils.decryptAES(cluster.getServerAuthData()));

			if(accessType != null) {
				switch (accessType) {
					case AWSIAM:
					case AWS:
						PublicCloudAccessInfoVO accessInfo = new PublicCloudAccessInfoVO();
						ProviderAccountVO providerAccount = providerAccountService.getProviderAccountByClusterSeq(clusterSeq, ProviderCode.AWS, ProviderAccountType.ACCESS_KEY);
						if(providerAccount != null) {
							if (StringUtils.isNotBlank(issueAccountName)) {
								UserClusterRoleIssueVO userClusterRoleIssue = userClusterRoleIssueService.getUserClusterRoleIssue(ContextHolder.exeContext().getUserSeq(), clusterSeq, IssueType.KUBECONFIG.getCode(), issueAccountName);
								if(userClusterRoleIssue == null || StringUtils.isBlank(userClusterRoleIssue.getIssueConfig())) {
									// Kubeconfig 권한이 없거나 접속 정보가 올바르지 않으면 그대로 종료.. publicCloudAccessInfo를 응답하지 않음...
									break;
								}
								String userIamInfo = CryptoUtils.decryptAES(userClusterRoleIssue.getIssueConfig());
								IssueConfigAWSVO issueConfigAWS = JsonUtils.fromGson(userIamInfo, IssueConfigAWSVO.class);
								accessInfo.setAwsUser(issueConfigAWS);

								String awsAccessInfo = CryptoUtils.decryptAES(providerAccount.getApiAccountPassword());
								if (StringUtils.isNotBlank(awsAccessInfo)) {
									JsonElement element = new JsonParser().parse(awsAccessInfo);
									String accessKeyId = element.getAsJsonObject().get("access_key_id").getAsString();
									String secretAccessKey = element.getAsJsonObject().get("secret_access_key").getAsString();

									accessInfo.setPublicCloudAccessType(accessType);
									accessInfo.setAwsAccessKey(accessKeyId);
									accessInfo.setAwsSecret(secretAccessKey);

									cluster.setPublicCloudAccessInfo(accessInfo);
								}

							}
						}
						break;
					default:
						break;
				}
			}
		}

		return cluster;
	}

	/**
	 * Public Cloud의 접속 계정이 연결된 클러스터 목록 및 해당 접속 정보 응답
	 * @param providerType
	 * @return
	 * @throws Exception
	 */
	public List<ClusterVO> getAccessableClusters(ProviderCode providerType, boolean canIam, boolean canLog) throws Exception {
		List<ClusterVO> clusters = new ArrayList<>();
		List<ClusterVO> clusterList = this.getClusters();

		HttpServletRequest request = Utils.getCurrentRequest();

		for(ClusterVO cluster : Optional.ofNullable(clusterList).orElseGet(() ->Lists.newArrayList())) {
            if (cluster != null) {
                k8sTokenGenerator.refreshClusterToken(cluster);
                cluster.setApiSecret(CryptoUtils.decryptAES(cluster.getApiSecret()));
                cluster.setClientAuthData(CryptoUtils.decryptAES(cluster.getClientAuthData()));
                cluster.setClientKeyData(CryptoUtils.decryptAES(cluster.getClientKeyData()));
                cluster.setServerAuthData(CryptoUtils.decryptAES(cluster.getServerAuthData()));

                if(cluster.getCloudProviderAccount() == null) {
                    /** Cloud Provider Account가 연결된 Cluster만 응답.. **/
                    continue;
                }
                if(cluster.getCloudProviderAccount().getProviderCode() == null) {
                    /** Cloud Provider Account가 연결된 Cluster만 응답.. **/
                    continue;
                }
                if (providerType != null && providerType != cluster.getCloudProviderAccount().getProviderCode()) {
                    /** Provider Type 입력이 있으면 매칭되는 Provider에 대해서만 응답...**/
                    continue;
                }

                PublicCloudAccessInfoVO accessInfo = new PublicCloudAccessInfoVO();
                JsonElement element;
                if (cluster.getCloudProviderAccount() != null) {

                    JSONObject config = null;
                    try {
                        config = new JSONObject(cluster.getCloudProviderAccount().getConfig());
                    } catch (Exception e) {
                        CocktailException ce = new CocktailException("config format is invalid", e, ExceptionType.ProviderCredentialFormatInvalid);
                        log.warn(ExceptionMessageUtils.setCommonResult(request, null, ce, false));
                    }

                    if (cluster.getCloudProviderAccount().getProviderCode() != null) {
                        switch (cluster.getCloudProviderAccount().getProviderCode()) {
                            case AWS:
                                if (config != null
                                        && (
                                        (canIam && config.getBoolean("aws_iam_permission_check"))
                                                || (canLog && config.getBoolean("aws_cloudwatch_permission_check"))
                                )
                                ) {
                                    String awsAccessInfo = CryptoUtils.decryptAES(cluster.getCloudProviderAccount().getApiAccountPassword());
                                    if (StringUtils.isNotBlank(awsAccessInfo)) {
                                        element = new JsonParser().parse(awsAccessInfo);
                                        if (element != null && element.getAsJsonObject() != null
                                                && element.getAsJsonObject().get("access_key_id") != null
                                                && element.getAsJsonObject().get("secret_access_key") != null
                                        ) {
                                            accessInfo.setAwsAccessKey(element.getAsJsonObject().get("access_key_id").getAsString());
                                            accessInfo.setAwsSecret(element.getAsJsonObject().get("secret_access_key").getAsString());
                                            accessInfo.setPublicCloudAccessType(PublicCloudAccessType.AWS);
                                        } else {
                                            CocktailException ce = new CocktailException("config format is invalid(AWS)", ExceptionType.ProviderCredentialFormatInvalid);
                                            log.warn(ExceptionMessageUtils.setCommonResult(request, null, ce, false));
                                        }
                                    } else {
                                        CocktailException ce = new CocktailException("config format is invalid(AWS)", ExceptionType.ProviderCredentialFormatInvalid);
                                        log.warn(ExceptionMessageUtils.setCommonResult(request, null, ce, false));
                                    }
                                    break;
                                } else {
                                    continue;
                                }
                            case AZR:
                                if (config != null
                                        && (canLog && config.getBoolean("azure_log_analytics_permission_check"))
                                ) {
                                    String azureAccessInfo = CryptoUtils.decryptAES(cluster.getCloudProviderAccount().getApiAccountPassword());
                                    if (StringUtils.isNotBlank(azureAccessInfo)) {
                                        element = new JsonParser().parse(azureAccessInfo);
                                        if (element != null && element.getAsJsonObject() != null
                                                && element.getAsJsonObject().get("tenant_id") != null
                                                && element.getAsJsonObject().get("workspace_id") != null
                                                && element.getAsJsonObject().get("client_id") != null
                                                && element.getAsJsonObject().get("client_secret") != null
                                        ) {
                                            accessInfo.setAzureTenantId(element.getAsJsonObject().get("tenant_id").getAsString());
                                            accessInfo.setAzureWorkspaceId(element.getAsJsonObject().get("workspace_id").getAsString());
                                            accessInfo.setAzureClientId(element.getAsJsonObject().get("client_id").getAsString());
                                            accessInfo.setAzureClientSecret(element.getAsJsonObject().get("client_secret").getAsString());
                                            accessInfo.setPublicCloudAccessType(PublicCloudAccessType.AZR);
                                        } else {
                                            CocktailException ce = new CocktailException("config format is invalid(AZR)", ExceptionType.ProviderCredentialFormatInvalid);
                                            log.warn(ExceptionMessageUtils.setCommonResult(request, null, ce, false));
                                        }
                                    } else {
                                        CocktailException ce = new CocktailException("config format is invalid(AZR)", ExceptionType.ProviderCredentialFormatInvalid);
                                        log.warn(ExceptionMessageUtils.setCommonResult(request, null, ce, false));
                                    }
                                    break;
                                } else {
                                    continue;
                                }
                            case GCP:
                                if (config != null
                                        && (canLog && config.getBoolean("gcp_logs_viewer_permission_check"))
                                ) {
                                    accessInfo.setGcpJsonKey(CryptoUtils.decryptAES(cluster.getCloudProviderAccount().getApiAccountPassword()));
                                    accessInfo.setPublicCloudAccessType(PublicCloudAccessType.GCP);
                                    break;
                                } else {
                                    continue;
                                }
                            default:
                                continue;
                        }
                    }

                    /** Result에 추가 **/
                    cluster.setPublicCloudAccessInfo(accessInfo);
                    clusters.add(cluster);
                }
                else {
                    log.warn("could not found cloud provider account : " + cluster.getClusterSeq());
                }
            }

		}

		return clusters;
	}

	public void updateCluster(ClusterAddVO cluster) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

		if (cluster != null) {
			Integer clusterSeq = cluster.getClusterSeq();
			ClusterVO currCluster = this.getCluster(clusterSeq);

			String useYn = cluster.getUseYn();
			if ("N".equalsIgnoreCase(useYn)) {
				this.removeCluster(currCluster, true, cluster.getUpdater());
			}

			if (clusterDao.hasComponents(clusterSeq) == 1) {

				Integer providerAccountSeq = cluster.getProviderAccountSeq();
				if (providerAccountSeq != null && !providerAccountSeq.equals(currCluster.getProviderAccountSeq())) {
					throw new CocktailException("해당 클러스터는 컴포넌트를 갖고 있어, 프로바이더계정을 수정 할 수 없습니다.", ExceptionType.ClusterHasComponent);
				}

				ClusterType clusterType = cluster.getClusterType();
				if (clusterType != null && clusterType != currCluster.getClusterType()) {
					throw new CocktailException("해당 클러스터는 컴포넌트를 갖고 있어, 클러스터타입을 수정 할 수 없습니다.", ExceptionType.ClusterHasComponent);
				}

				CubeType cubeType = cluster.getCubeType();
				if (cubeType != null && cubeType != currCluster.getCubeType()) {
					throw new CocktailException("해당 클러스터는 컴포넌트를 갖고 있어, 큐브타입을 수정 할 수 없습니다.", ExceptionType.ClusterHasComponent);
				}

			}

			// api url // cluster id 중복 체크
			this.checkDuplicationCluster(cluster, clusterDao);

			if (StringUtils.isNotBlank(cluster.getApiUrl()) || StringUtils.isNotBlank(cluster.getApiKey()) ||
					StringUtils.isNotBlank(cluster.getApiSecret()) || StringUtils.isNotBlank(cluster.getClientAuthData()) ||
					StringUtils.isNotBlank(cluster.getServerAuthData()) || StringUtils.isNotBlank(cluster.getClientKeyData())) {

				ClusterAddVO check = new ClusterAddVO();
				check.setClusterSeq(clusterSeq);
				check.setAuthType(cluster.getAuthType());
				check.setCubeType(currCluster.getCubeType());
				check.setRegionCode(StringUtils.isBlank(cluster.getRegionCode()) ? currCluster.getRegionCode() : cluster.getRegionCode());
				check.setBillingGroupId(StringUtils.isBlank(cluster.getBillingGroupId()) ? currCluster.getBillingGroupId() : cluster.getBillingGroupId());
				check.setApiUrl(StringUtils.isBlank(cluster.getApiUrl()) ? currCluster.getApiUrl() : cluster.getApiUrl());
				check.setApiKey(StringUtils.isBlank(cluster.getApiKey()) ? currCluster.getApiKey() : cluster.getApiKey());
				check.setApiSecret(StringUtils.isBlank(cluster.getApiSecret()) ? currCluster.getApiSecret() : cluster.getApiSecret());

				// GKE 일 경우 CERT 에서 TOKEN 방식으로 변경하는 경우가 있을 수 있고,
				// TOKEN 방식에서 사용하지 않는 ClientAuthData 와 ClientKeyData 값을 지움
				if(cluster.getCubeType() == CubeType.GKE
						&& currCluster.getAuthType() == AuthType.CERT
						&& cluster.getAuthType() == AuthType.TOKEN)
				{
					// token을 다시 발급후 셋팅 한다.
					k8sTokenGenerator.initClusterToken(check);
					cluster.setApiSecret(check.getApiSecret());

					// 기존에 데이터가 존재하는 경우, 값을 지우기 위해 공백 셋팅
					if(StringUtils.isNotBlank(currCluster.getClientAuthData())){
						check.setClientAuthData(" ");
						cluster.setClientAuthData(" ");
					}

					// 기존에 데이터가 존재하는 경우, 값을 지우기 위해 공백 셋팅
					if(StringUtils.isNotBlank(currCluster.getClientAuthData())){
						check.setClientKeyData(" ");
						cluster.setClientKeyData(" ");
					}
				} else {
					if (EnumSet.of(CubeType.GKE, CubeType.AKS).contains(cluster.getCubeType())
							&& cluster.getAuthType() == AuthType.TOKEN
					) {
						// token을 다시 발급후 셋팅 한다.
						k8sTokenGenerator.initClusterToken(check);
						cluster.setApiSecret(check.getApiSecret());

						// 2021-09-06, coolingi, AKS 일때 client 값이 있으면 셋팅한다.
						if (CubeType.AKS == cluster.getCubeType()) {
							check.setClientAuthData(StringUtils.isBlank(cluster.getClientAuthData()) ? currCluster.getClientAuthData() : cluster.getClientAuthData());
							check.setClientKeyData(StringUtils.isBlank(cluster.getClientKeyData()) ? currCluster.getClientKeyData() : cluster.getClientKeyData());
						}
					} else {
						check.setClientAuthData(StringUtils.isBlank(cluster.getClientAuthData()) ? currCluster.getClientAuthData() : cluster.getClientAuthData());
						check.setClientKeyData(StringUtils.isBlank(cluster.getClientKeyData()) ? currCluster.getClientKeyData() : cluster.getClientKeyData());
					}
				}

				check.setServerAuthData(StringUtils.isBlank(cluster.getServerAuthData()) ? currCluster.getServerAuthData() : cluster.getServerAuthData());

				this.validCluster(check, clusterDao);
			}

			boolean isUpdate = false;
			if(cluster.getBillingProviderAccountSeq() != null){
				if(currCluster.getBillingProviderAccountSeq() != null){
					if(!currCluster.getBillingProviderAccountSeq().equals(cluster.getBillingProviderAccountSeq())){
						isUpdate = true;
					}
				}else{
					isUpdate = true;
				}

				if(isUpdate && currCluster.getProviderAccount().getProviderCode().canMetering()){
					ProviderAccountVO billingProviderAccount = providerAccountService.getProviderAccount(cluster.getBillingProviderAccountSeq());
					if(billingProviderAccount != null){
						cluster.setProviderAccount(currCluster.getProviderAccount());
						cluster.getProviderAccount().setUpdater(ContextHolder.exeContext().getUserSeq());
						cluster.getProviderAccount().setConfig(billingProviderAccount.getConfig());
						providerAccountService.editProviderAccount(cluster.getProviderAccount());
					}
				}
			}else{
				if(currCluster.getBillingProviderAccountSeq() != null){
					cluster.setProviderAccount(currCluster.getProviderAccount());
					cluster.getProviderAccount().setUpdater(ContextHolder.exeContext().getUserSeq());
					cluster.getProviderAccount().setConfig("");
					providerAccountService.editProviderAccount(cluster.getProviderAccount());
				}
			}

			clusterDao.updateCluster(cluster);

			/** 2020.05.08 : Addon-Manager가 설치되어 있으면 현재 Cocktail을 등록 해줌 **/
			try {
				addonService.registerCocktailToAddonManager(currCluster);
			}
			catch (Exception ex) {
				/** error 발생시 무시 **/
				log.warn("addon-manager update failure : by Cluster Update");
			}

			/**
			 * Ingress Controller 의 접근 호스트 정보 셋팅
			 * - 해당 ingress class의 configMap에 annotation으로 설정
			 */
			try {
				if (CollectionUtils.isNotEmpty(cluster.getIngressHosts())) {
					// chart-type : ingress-nginx addon configMap 조회
					List<ConfigMapGuiVO> ingressAddons = addonService.getAddonConfigMapsByChartType(clusterSeq, "ingress-nginx", "Y", false, false);
					// chart-type 변경으로 인한 재조회 추가 ( nginx-ingress -> ingress-nginx ), hjchoi.20210825
					if (CollectionUtils.isEmpty(ingressAddons)) {
						// chart-type : nginx-ingress addon configMap 조회
						ingressAddons = addonService.getAddonConfigMapsByChartType(clusterSeq, "nginx-ingress", "Y", false, false);
					}
					if (CollectionUtils.isNotEmpty(ingressAddons)) {
						String targetPath = "/metadata/annotations";

						// addon configMap의 annotation에 LB 정보를 저장
						for (ConfigMapGuiVO addonRow : ingressAddons) {
							for (Map<String, String> hostRow : cluster.getIngressHosts()) {
								if (hostRow.containsKey(addonRow.getName())) {
									List<JsonObject> patchBody = new ArrayList<>();
									Map<String, Object> patchMap = new HashMap<>();
									Map<String, String> annotations = Optional.ofNullable(addonRow.getAnnotations()).orElseGet(() ->Maps.newHashMap());
									if (MapUtils.isEmpty(annotations)) {
										patchMap.put("op", JsonPatchOp.ADD.getValue());
									}
									else {
										patchMap.put("op", JsonPatchOp.REPLACE.getValue());
									}
									// 아래와 같은 키로 annotation에 저장
									annotations.put(KubeConstants.LABELS_ACORNSOFT_INGRESS_URL, StringUtils.defaultString(hostRow.get(addonRow.getName())));
									patchMap.put("path", targetPath);
									patchMap.put("value", annotations);
									patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
									configMapService.patchConfigMap(clusterSeq, addonRow.getNamespace(), addonRow.getName(), patchBody);
									break;
								}
							}
						}
					}
				}
			} catch (Exception e) {
				/** error 발생시 무시 **/
				log.warn("ingress controller addon update failure : by Cluster Update");
			}
		} else {
			throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
		}
	}

	public List<Map<String, String>> getIngressHosts(Integer clusterSeq) throws Exception {
		ClusterVO cluster = this.getCluster(clusterSeq);
		return this.getIngressHosts(cluster);
	}

    /**
     * 인그레스 컨트롤러의 LB 정보 조회
     *
     * @param cluster
     * @return
     * @throws Exception
     */
	public List<Map<String, String>> getIngressHosts(ClusterVO cluster) throws Exception {
		List<Map<String, String>> hosts = Lists.newArrayList();

		if (clusterStateService.isClusterRunning(cluster)) {
			// chart-type : ingress-nginx addon configMap 조회
			List<ConfigMapGuiVO> ingressAddons = addonService.getAddonConfigMapsByChartType(cluster, "ingress-nginx", "Y", false, false);
			// chart-type 변경으로 인한 재조회 추가 ( nginx-ingress -> ingress-nginx ), hjchoi.20210825
			if (CollectionUtils.isEmpty(ingressAddons)) {
				// chart-type : nginx-ingress addon configMap 조회
				ingressAddons = addonService.getAddonConfigMapsByChartType(cluster, "nginx-ingress", "Y", false, false);
			}
			ingressAddons = Optional.ofNullable(ingressAddons).orElseGet(() ->Lists.newArrayList());

			for (ConfigMapGuiVO addonRow : ingressAddons) {
				Map<String, String> hostMap = Maps.newHashMap();
				hostMap.put(addonRow.getName()
						, StringUtils.defaultString(Optional.ofNullable(addonRow.getAnnotations()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ACORNSOFT_INGRESS_URL)));
				hosts.add(hostMap);
			}
		}

		return hosts;
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateClusterStateBySeq(Integer clusterSeq, String clusterState) throws Exception {
		ClusterAddVO cluster = new ClusterAddVO();
		cluster.setClusterSeq(clusterSeq);
		cluster.setClusterState(clusterState);

		this.updateClusterState(cluster);
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateClusterStateById(String clusterId, String clusterState) throws Exception {
		ClusterAddVO cluster = new ClusterAddVO();
		cluster.setClusterId(clusterId);
		cluster.setClusterState(clusterState);

		this.updateClusterState(cluster);
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateClusterState(ClusterAddVO cluster) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		clusterDao.updateClusterState(cluster);
	}

	/**
	 * 클러스터 버전 동기화
	 *
	 * @param accountSeq
	 * @param clusterSeq
	 * @throws Exception
	 */
	public void syncClusterVersion(Integer accountSeq, Integer clusterSeq) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		List<ClusterVO> clusters = Optional.ofNullable(clusterDao.getClusters(accountSeq, null, clusterSeq, null, null, "Y")).orElseGet(() ->Lists.newArrayList());

		Integer updater = null;
		if (ContextHolder.exeContext() != null) {
			updater = ContextHolder.exeContext().getUserSeq();
		}

		for (ClusterVO cluster : clusters) {
			if (clusterStateService.isClusterRunning(cluster)) {
				VersionInfo versionInfo = k8sResourceService.getClusterVersionInfo(cluster);
				if (versionInfo != null) {
					this.updateClusterVersionForSync(cluster.getClusterSeq(), cluster.getK8sVersion(), versionInfo.getGitVersion(), clusterDao, updater);
				}
			}
		}
	}

	/**
	 * 칵테일에 등록된 클러스터 버전과 k8s 클러스터 버전을 비교하여 변경처리
	 *
	 * @param clusterSeq
	 * @param cocktailClusterVersion - 칵테일에 등록된 클러스터 버전
	 * @param k8sClusterVersion - k8s 클러스터 버전
	 * @param clusterDao
	 * @param updater
	 * @throws Exception
	 */
	public void updateClusterVersionForSync(Integer clusterSeq, String cocktailClusterVersion, String k8sClusterVersion, IClusterMapper clusterDao, Integer updater) throws Exception {
		if (StringUtils.isNotBlank(cocktailClusterVersion) && StringUtils.isNotBlank(k8sClusterVersion)) {
			String currK8sVersion = ResourceUtil.getMatchVersion(k8sClusterVersion);
			if (!StringUtils.equals(currK8sVersion, cocktailClusterVersion)) {
				if (clusterDao == null) {
					clusterDao = sqlSession.getMapper(IClusterMapper.class);
				}
				clusterDao.updateClusterVersion(clusterSeq, currK8sVersion, updater);
			}
		}
	}

    @Transactional(transactionManager = "transactionManager")
	public void removeCluster(Integer clusterSeq, boolean cascade, Integer updater) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO clusterVO = clusterDao.getCluster(clusterSeq);
		this.removeCluster(clusterVO, cascade, updater);
	}

	@Transactional(transactionManager = "transactionManager")
	public void removeCluster(ClusterVO cluster, boolean cascade, Integer updater) throws Exception {
		this.removeCluster(cluster, cascade, updater, ContextHolder.exeContext().getUserRole());
	}

    @Transactional(transactionManager = "transactionManager")
	public void removeCluster(ClusterVO cluster, boolean cascade, Integer updater, String userRole) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
		IProviderAccountMapper providerAccountDao = sqlSession.getMapper(IProviderAccountMapper.class);
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        if (cluster != null) {
            /**
             * cluster provision 로직
             */
            // 2018.11.21 AWS는 자동 생성 하도록 변경
//		ClusterVO clusterVO = clusterDao.getCluster(clusterSeq);
//		if (CubeType.PROVIDER.equals(clusterVO.getCubeType())
//				&& (clusterVO.getInventory() != null && clusterVO.getInventory().length() > 0)) {
//			ProviderAccountVO providerAccountVO = dao.getProviderAccount(clusterVO.getProviderAccountSeq());
//			if (providerAccountVO != null && ProviderCode.AWS.equals(providerAccountVO.getProviderCode())) {
//				ClusterAddVO clusterAddVO = new ClusterAddVO();
//				clusterAddVO.setClusterSeq(clusterVO.getClusterSeq());
//				clusterAddVO.setClusterUuid(clusterVO.getClusterUuid());
//				clusterAddVO.setClusterName(clusterVO.getClusterName());
//				clusterAddVO.setUpdater(updater);
//				clusterAddVO.setClusterState(ClusterState.DELETING.getCode());
//				removeClusterProvider(providerAccountVO, clusterAddVO);
//				return;
//			}
//		}

            String baseLogFormat = String.format("############################### REMOVE_CLUSTER ##### - cluster: [%d, %s], updater: [%d, %s]", cluster.getClusterSeq(), cluster.getClusterId(), updater, userRole);

            /**
             * cascade 옵션 적용시에는 클러스터에 관계된 정보 삭제(use_yn = 'N') 처리
             */
            if (!cascade) {
                serviceValidService.canDeleteServiceClusters(null, Collections.singletonList(cluster.getClusterSeq()), ServiceType.Names.NORMAL);
            }

            /**
             * remove addon configMap
             * STOPPED 일 경우 Addon이 삭제되지 않는다.
             */
            try {
//                /** 1. 전체 Addon 제거 **/
//                addonService.removeAllAddons(cluster);
				/** 1. Agent만 제거 **/
	            String agentReleaseName = String.format("%s-%s-%d", cocktailAddonProperties.getMonitoringAgentConfigmapPrefix(), ResourceUtil.getResourcePrefix(), cluster.getClusterSeq());
	            ConfigMapGuiVO agentCm = configMapService.getConfigMap(cluster.getClusterSeq(), addonService.getCocktailAddonNamespace(), agentReleaseName);
	            if (agentCm != null) {
		            addonService.deleteAddonV2(cluster.getClusterSeq(), agentReleaseName);
	            }
            } catch (Exception e) {
                if (clusterStateService.isClusterRunning(cluster)) {
                    throw e;
                } else {
                    log.warn(String.format("%s - %s", baseLogFormat, "Failure to delete add-ons because cluster is not up."), e);

                    if(cascade) {
                        /** Cluster가 Running 중이 아닐때 Cluster를 제거하게 되면 Monitoring Agent Addon의 인증 키를 expire 처리 함 **/
                        boolean res = addonService.expireClusterAccessSecret(null, cluster.getClusterSeq(), ClusterAddonType.MONITORING.getCode(), false);
                        log.info("{}, {}: {}", baseLogFormat, "expireClusterAccessSecret", res);
                    }
                }
            }

            /**
             * 해당 클러스터에 shell, kubeconfig 권한이 부여된 사용자 권한 삭제
             */
            List<ClusterVO> clusters = clusterDao.getClusters(null, null, cluster.getClusterSeq(), null, null, "Y");
			if (CollectionUtils.isNotEmpty(clusters)) {
				userClusterRoleIssueService.removeUserClusterRoleIssues(cluster, clusters.get(0).getAccount().getAccountSeq(), updater, userRole);
			}
			userClusterRoleIssueService.deleteAllUserClusterRoleIssueInfoByCluster(cluster.getClusterSeq());

            // delete 처리
			this.deleteClusterCascade(cluster, baseLogFormat, clusterDao);

            // cluster - service mapping 삭제
            serviceDao.deleteServiceCluster(cluster.getClusterSeq(), null);

            // provider_account 삭제
            ProviderAccountVO pvCurr = providerAccountDao.getProviderAccount(cluster.getProviderAccountSeq());
            accountDao.deleteProviderAccount(pvCurr.getProviderAccountSeq());
            providerAccountDao.deleteProviderAccount(pvCurr);

            // cluster 삭제
            clusterDao.removeClusterEmpty(cluster.getClusterSeq(), ClusterState.DELETED, updater);
            // FOREIGN_KEY_CHECKS 해제하고 삭제처리
			clusterDao.deleteCluster(cluster.getClusterSeq());
        }
    }

//	public void removeClusterCascade(ClusterVO cluster, String baseLogFormat, Integer updater, IClusterMapper clusterDao) throws Exception {
//
//		if (clusterDao == null) {
//			clusterDao = sqlSession.getMapper(IClusterMapper.class);
//		}
//
//		if (cluster != null) {
//			int result;
//
//			result = clusterDao.removeComponentsByCluster(cluster.getClusterSeq(), updater);
//			log.info("{}, {}: {}", baseLogFormat, "removeComponents", result);
//
//			result = clusterDao.removeWorkloadGroupsByCluster(cluster.getClusterSeq(), updater);
//			log.info("{}, {}: {}", baseLogFormat, "removeWorkloadGroups", result);
//
//			result = clusterDao.removePlRunByCluster(cluster.getClusterSeq(), updater);
//			log.info("{}, {}: {}", baseLogFormat, "removePlRun", result);
//
//			result = clusterDao.removePlMasterByCluster(cluster.getClusterSeq(), updater);
//			log.info("{}, {}: {}", baseLogFormat, "removePlMaster", result);
//
//			result = clusterDao.removeServicemapsByCluster(cluster.getClusterSeq(), updater);
//			log.info("{}, {}: {}", baseLogFormat, "removeServicemaps", result);
//
//			result = clusterDao.removePipelineRunByCluster(cluster.getClusterSeq(), updater);
//			log.info("{}, {}: {}", baseLogFormat, "removePipelineRun", result);
//
//			result = clusterDao.removePipelineContainerByCluster(cluster.getClusterSeq(), updater);
//			log.info("{}, {}: {}", baseLogFormat, "removePipelineContainer", result);
//
//			result = clusterDao.removePipelineWorkloadByCluster(cluster.getClusterSeq(), updater);
//			log.info("{}, {}: {}", baseLogFormat, "removePipelineWorkload", result);
//		}
//
//	}

	public void deleteClusterCascade(ClusterVO cluster, String baseLogFormat, IClusterMapper clusterDao) throws Exception {

		if (clusterDao == null) {
			clusterDao = sqlSession.getMapper(IClusterMapper.class);
		}

		if (cluster != null) {
			int result;

			result = clusterDao.deleteComponentsByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deleteComponents", result);

			result = clusterDao.deletePlRunBuildDeployMappingByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePlRunBuildDeployMapping", result);

			result = clusterDao.deletePlRunDeployByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePlRunDeploy", result);

			result = clusterDao.deletePlRunBuildByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePlRunBuild", result);

			result = clusterDao.deletePlRunByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePlRun", result);

			result = clusterDao.deletePlResBuildDeployMappingByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePlResBuildDeployMapping", result);

			result = clusterDao.deletePlResDeployByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePlResDeploy", result);

			result = clusterDao.deletePlResBuildByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePlResBuild", result);

			result = clusterDao.deletePlMasterByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePlMaster", result);

			result = clusterDao.deleteWorkloadGroupsByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deleteWorkloadGroups", result);

			result = clusterDao.deleteServicemapsByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deleteServicemaps", result);

			result = clusterDao.deletePipelineRunByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePipelineRun", result);

			result = clusterDao.deletePipelineContainerByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePipelineContainer", result);

			result = clusterDao.deletePipelineWorkloadByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deletePipelineWorkload", result);

			result = clusterDao.deleteBuildServerByCluster(cluster.getClusterSeq());
			log.info("{}, {}: {}", baseLogFormat, "deleteBuildServer", result);
		}

	}

//	@Async
//	public void removeClusterAsync(ClusterVO cluster, boolean cascade, Integer updater, String userRole) throws Exception {
//		this.removeCluster(cluster, cascade, updater, userRole);
//	}

    public Boolean checkClusterAuthentication(ClusterAddVO cluster, boolean isThrow) throws Exception {
        log.debug("check cluster connection");
        try {

        	ApiClient client = (ApiClient) k8sClient.createNoCache(
                    cluster.getAuthType(),
                    cluster.getCubeType(),
                    cluster.getApiUrl(),
                    cluster.getApiKey(),
                    cluster.getApiSecret(),
                    cluster.getClientAuthData(),
                    cluster.getClientKeyData(),
                    cluster.getServerAuthData());

            if(client != null){
                CoreV1Api apiInstance = new CoreV1Api(client);
                V1NamespaceList v1NamespaceList = apiInstance.listNamespace(null, null, null, null, null, 10, null, null, null, null);

                if(v1NamespaceList != null && CollectionUtils.isNotEmpty(v1NamespaceList.getItems())){
                    log.debug("check cluster connection - get namespace");
                    return true;
                }else{
                    log.warn("check cluster connection - Can't get namespace list");
                    return false;
                }
            }else{
                log.warn("check cluster connection - not set config");
                return false;
            }

        } catch (Exception eo) {
			if(log.isDebugEnabled()) log.debug("trace log ", eo);
			String errMsg = null;
			if (eo instanceof ApiException ae) {
				errMsg = ae.getMessage();
			}
            CocktailException ce = new CocktailException("Can't connect to cluster", eo, ExceptionType.InvalidClusterCertification, errMsg);
			if (isThrow) {
				throw ce;
			}
            log.warn("Can't connect to cluster", ce);
            return false;
        }
    }

	public void validCluster(ClusterAddVO cluster, IClusterMapper clusterDao) throws Exception {
		if (clusterDao == null) {
			clusterDao = sqlSession.getMapper(IClusterMapper.class);
		}
		try {
			if (!this.checkClusterAuthentication(cluster, true)) {
				throw new CocktailException("Invalid cluster connection information", ExceptionType.ClusterAccountInvalid);
			}
		} catch (CocktailException e) {
			throw e;
		} catch (Exception e) {
			throw new CocktailException("Can't connect to cluster", e, ExceptionType.InvalidClusterCertification, e.getMessage());
		}
		this.checkDuplicationCluster(cluster, clusterDao);
	}

	public void checkDuplicationCluster(ClusterAddVO cluster, IClusterMapper clusterDao) throws Exception {
        if (clusterDao == null) {
            clusterDao = sqlSession.getMapper(IClusterMapper.class);
        }
        if (CollectionUtils.isNotEmpty(clusterDao.getDuplicationCluster(cluster.getApiUrl(), cluster.getClusterId(), cluster.getClusterSeq()))) {
            throw new CocktailException(String.format("Cluster ApiUrl '%s' already exists", cluster.getApiUrl()), ExceptionType.ClusterApiUrlAlreadyExists);
        }
    }

	public List<ClusterDetailConditionVO> getClusterCondition(Integer clusterSeq
			, boolean useK8s, boolean useSvc, boolean usePvc, boolean useAddon, boolean useFeatureGates, boolean useNamespace, boolean useWorkload, boolean usePod, boolean showAuth
			, ExecutingContextVO context
	) throws Exception {
		return getClusterCondition(clusterSeq, null, null, useK8s, useSvc, usePvc, useAddon, useFeatureGates, useNamespace, useWorkload, usePod, showAuth, context);
	}

	public List<ClusterDetailConditionVO> getClusterCondition(Integer accountSeq, Integer serviceSeq
			, boolean useK8s, boolean useSvc, boolean usePvc, boolean useAddon, boolean useFeatureGates, boolean useNamespace, boolean useWorkload, boolean usePod, boolean showAuth
			, ExecutingContextVO context
	) throws Exception {
		return getClusterCondition(null, accountSeq, serviceSeq, useK8s, useSvc, usePvc, useAddon, useFeatureGates, useNamespace, useWorkload, usePod, showAuth, context);
	}

	/**
	 * 클러스터 현황 조회.
	 * @param clusterSeq : 조회할 클러스터의 Sequence / if null이면 아래 accountSeq를 이용하여 조회...
	 * @param accountSeq : 조회할 클러스터가 속해있는 account(시스템계정)의 Sequence
	 * @param serviceSeq : 조회할 클러스터가 속해있는 service(워크스페이스)의 Sequence
	 * @param useK8s : k8s 조회 여부
	 * @param useSvc : service 노출 조회 여부
	 * @param usePvc : pvc 조회 여부
	 * @param useAddon : addon 조회 여부
	 * @param useFeatureGates : FeatureGates 조회 여부
	 * @param useNamespace : namespace 조회 여부
	 * @param useWorkload : workload 조회 여부
	 * @param usePod : pod 조회 여부
	 * @param showAuth : auth 정보
	 * @param context : Executing Context
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<ClusterDetailConditionVO> getClusterCondition(Integer clusterSeq, Integer accountSeq, Integer serviceSeq
			, boolean useK8s, boolean useSvc, boolean usePvc, boolean useAddon, boolean useFeatureGates, boolean useNamespace, boolean useWorkload, boolean usePod, boolean showAuth
			, ExecutingContextVO context
	) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		List<ClusterDetailConditionVO> clusters;
		if (clusterSeq != null) {
			clusters = clusterDao.getClusterCondition(clusterSeq, null, null);
		} else {
			clusters = clusterDao.getClusterCondition(null, accountSeq, serviceSeq);
		}

		if (CollectionUtils.isNotEmpty(clusters)) {
			// cluster shell / kubeconfig 권한 조회
			Map<Integer, List<UserClusterRoleIssueVO>> shellRoleMap = Maps.newHashMap();
			Map<Integer, List<UserClusterRoleIssueVO>> kubeconfigRoleMap = Maps.newHashMap();
			if (ContextHolder.exeContext() != null && ContextHolder.exeContext().getUserSeq() != null) {
				UserVO userCurr = userService.getByUserSeq(ContextHolder.exeContext().getUserSeq());
				for (UserClusterRoleIssueVO issueRow : Optional.ofNullable(userCurr.getShellRoles()).orElseGet(() ->Lists.newArrayList())) {
					if (!shellRoleMap.containsKey(issueRow.getClusterSeq())) {
						shellRoleMap.put(issueRow.getClusterSeq(), Lists.newArrayList());
					}

					issueRow.setCluster(null); // 클러스터 조회이므로 null 처리
					shellRoleMap.get(issueRow.getClusterSeq()).add(issueRow);
				}
				for (UserClusterRoleIssueVO issueRow : Optional.ofNullable(userCurr.getKubeconfigRoles()).orElseGet(() ->Lists.newArrayList())) {
					if (!kubeconfigRoleMap.containsKey(issueRow.getClusterSeq())) {
						kubeconfigRoleMap.put(issueRow.getClusterSeq(), Lists.newArrayList());
					}

					issueRow.setCluster(null); // 클러스터 조회이므로 null 처리
					kubeconfigRoleMap.get(issueRow.getClusterSeq()).add(issueRow);
				}
			}

			long readyNodeCount;
			long desiredNodeCount;
			long gpuNodeCount;
			long cpuCapacity;
			long memoryCapacity;
			int gpuCapacity;
			Integer loadBalancerCount = 0;
			Integer volumeRequestCapacity = 0;

			// serviceSeqPerCluster : 개별 클러스터의 현황 조회시 QUOTA 적용을 위한 변수. (if null 이면 Cluster 전체의 리소스 사용정보를 조회)
			Integer serviceSeqPerCluster = serviceSeq;
            List<Integer> serviceSeqs = null;
            if (serviceSeqPerCluster != null) {
                serviceSeqs = Collections.singletonList(serviceSeqPerCluster);
            }
            boolean useAddonPerCluster = useAddon;
			for (ClusterDetailConditionVO cluster : clusters) {
				if (cluster == null) {
					continue;
				} else {
					if (cluster.getCondition() == null) {
						cluster.setCondition(new ClusterConditionVO());
					}
				}

				serviceSeqPerCluster = null; // null이면 클러스터 전체 조회
				useAddonPerCluster = useAddon;

				// cluster shell / kubeconfig 권한 셋팅
				cluster.setShellRoles(Optional.ofNullable(shellRoleMap).orElseGet(() ->Maps.newHashMap()).get(cluster.getClusterSeq()));
				cluster.setKubeconfigRoles(Optional.ofNullable(kubeconfigRoleMap).orElseGet(() ->Maps.newHashMap()).get(cluster.getClusterSeq()));

				readyNodeCount = 0L;
				desiredNodeCount = 0L;
				gpuNodeCount = 0L;
				cpuCapacity = 0L;
				memoryCapacity = 0L;
				gpuCapacity = 0;
				loadBalancerCount = 0;
				volumeRequestCapacity = 0;

				if ((useK8s || useSvc || usePvc || useAddonPerCluster || useFeatureGates || useNamespace || useWorkload || usePod) && clusterStateService.isClusterRunning(cluster)) {
					if (useK8s) {
						try {
							List<K8sNodeVO> nodes = k8sResourceService.convertNodeDataList(cluster, serviceSeqPerCluster, false, false, context);

							if (CollectionUtils.isNotEmpty(nodes)) {
								desiredNodeCount = nodes.size();
								for (K8sNodeVO nodeRow : nodes) {
									if (StringUtils.equalsIgnoreCase("True", nodeRow.getReady())) {
										readyNodeCount++;
									}
									cpuCapacity += nodeRow.getCpuCapacity();
									memoryCapacity += nodeRow.getMemoryCapacity();
									gpuCapacity += nodeRow.getGpuCapacity();

									// gpu node count
									if (nodeRow.getGpuCapacity() > 0
											|| (MapUtils.isNotEmpty(nodeRow.getLabels()) && nodeRow.getLabels().containsKey(KubeConstants.RESOURCES_GPU))) {
										gpuNodeCount += 1;
									}
								}
							}
							cluster.getCondition().setReadyNodeCount(readyNodeCount);
							cluster.getCondition().setDesiredNodeCount(desiredNodeCount);
							cluster.getCondition().setGpuNodeCount(gpuNodeCount);
							cluster.getCondition().setTotalCpuUsage(cpuCapacity / 1000);
							cluster.getCondition().setTotalMemUsage(memoryCapacity);
							cluster.getCondition().setTotalGpuUsage(gpuCapacity);

							if(cluster.getCubeType().isOpenShift()) {
								cluster.getCondition().setOpenShiftUsage(true);
								Map<String, Object> result = clusterApiClient.openshiftRoutes("istio-system",
																			cluster.getClusterSeq(),
																			ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserAccountSeq());
								if(MapUtils.isNotEmpty(result) && result.containsKey("result")) {
									List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("result");
									if(CollectionUtils.isNotEmpty(items)) {
										for (Map<String, Object> item : items) {
											if (MapUtils.isNotEmpty(item)) {
												if ( (item.get("namespace") != null && "istio-system".equalsIgnoreCase(item.get("namespace").toString()))
														&& (item.get("name") != null && "kiali".equalsIgnoreCase(item.get("name").toString()))
												) {
													if(item.containsKey("host")) {
														cluster.getCondition().setKialiUrl(MapUtils.getString(item, "host", null));
													}
												}
											}
										}
									}
								}
							}
						} catch (Exception e) {
							log.warn("fail k8s convertNodeDataList!!", e);
						}
					}
					if (useNamespace) {
						try {
							List<K8sNamespaceVO> namespaces = Optional.ofNullable(namespaceService.getNamespacesToList(cluster, serviceSeqPerCluster, null, null, context)).orElseGet(() ->Lists.newArrayList());

							cluster.getCondition().setNamespaceCount(namespaces.size());
							cluster.setNamespaces(namespaces);
						} catch (Exception e) {
							log.warn("fail k8s getNamespacesToList!!", e);
						}

						// Whether the namespace exists or not
						if (CollectionUtils.isNotEmpty(cluster.getServicemaps()) && CollectionUtils.isNotEmpty(cluster.getNamespaces())) {
							Map<String, K8sNamespaceVO> namespaceMap = cluster.getNamespaces().stream().collect(Collectors.toMap(K8sNamespaceVO::getName, Function.identity()));
							for (ServicemapDetailVO smdRow : cluster.getServicemaps()) {
								boolean isExists = namespaceMap.containsKey(smdRow.getNamespaceName());
								smdRow.setK8sResourceExists(isExists);
								if (isExists) {
									smdRow.setLabels(namespaceMap.get(smdRow.getNamespaceName()).getLabels());
									smdRow.setAnnotations(namespaceMap.get(smdRow.getNamespaceName()).getAnnotations());
									smdRow.setK8sNamespace(namespaceMap.get(smdRow.getNamespaceName()));
								}
							}
						}
					}
					if (useWorkload || usePod) {
						try {
							Map<String, Map<String, ?>> k8sResourceMap = workloadResourceService.getWorkloadResource(
									cluster, serviceSeqPerCluster, null,
									null, null,
									useWorkload, useWorkload, useWorkload, useWorkload, useWorkload, Boolean.FALSE, Boolean.FALSE, usePod);

							if (MapUtils.isNotEmpty(k8sResourceMap)) {
								int workloadCnt = 0;
								int podCnt = 0;
								for (Map.Entry<String, Map<String, ?>> k8sEntryRow : k8sResourceMap.entrySet()) {
									if (K8sApiKindType.findKindTypeByValue(k8sEntryRow.getKey()).isWorkload()) {
										workloadCnt += k8sEntryRow.getValue().size();
									} else if (K8sApiKindType.findKindTypeByValue(k8sEntryRow.getKey()) == K8sApiKindType.POD) {
										podCnt += k8sEntryRow.getValue().size();
									}
								}
								if (useWorkload) {
									cluster.getCondition().setServerCount(workloadCnt);
								}
								if (usePod) {
									cluster.getCondition().setPodCount(podCnt);
								}
							} else {
								if (useWorkload) {
									cluster.getCondition().setServerCount(0);
								}
								if (usePod) {
									cluster.getCondition().setPodCount(0);
								}
							}
						} catch (Exception e) {
							log.warn("fail k8s getWorkloadResource!!", e);
						}

					}
					if (useSvc) {
						try {
							// Load Balancer 유형의 Service Count
							List<ClusterGateWayVO> clusterGateWays = this.getClusterGateWays(cluster, serviceSeqPerCluster, context);

							if (CollectionUtils.isNotEmpty(clusterGateWays)) {
								for (ClusterGateWayVO clusterGateWayRow : clusterGateWays) {
									loadBalancerCount += Integer.parseInt(String.valueOf(clusterGateWayRow.getGateWayCount()));
								}
							}
							cluster.getCondition().setLoadBalancerCount(loadBalancerCount);
						} catch (Exception e) {
							log.warn("fail k8s getServices!!", e);
						}
					}
					if (usePvc) {
						try {
                            // PVC 요청량
							List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, serviceSeqs, cluster.getClusterSeq(), null ,null, false, true);

							if (CollectionUtils.isNotEmpty(clusterVolumes)) {
								for (ClusterVolumeVO clusterVolumeRow : clusterVolumes) {
									volumeRequestCapacity += ( clusterVolumeRow.getTotalRequest() != null ? clusterVolumeRow.getTotalRequest() : 0 );
								}
							}

							cluster.getCondition().setVolumeRequestCapacity(volumeRequestCapacity);
						} catch (Exception e) {
							log.warn("fail k8s PVC request capacity!!", e);
						}
					}
					if (useAddonPerCluster) {
						try {
							// addon 정보
							cluster.setAddons(addonService.getAddonConfigMaps(cluster));

						} catch (Exception e) {
							log.warn("fail addon Inquiry!!", e);
						}
					}

					if (useFeatureGates) {
						try {
							// featureGates 정보 조회
							cluster.setFeatureGates(k8sResourceService.getFeatureGates(cluster));
						} catch (Exception e) {
							log.warn("fail featureGates Inquiry!!", e);
						}
					}
				} else {
					cluster.getCondition().setReadyNodeCount(readyNodeCount);
					cluster.getCondition().setDesiredNodeCount(desiredNodeCount);
					cluster.getCondition().setTotalCpuUsage(cpuCapacity);
					cluster.getCondition().setTotalMemUsage(memoryCapacity);
					cluster.getCondition().setTotalGpuUsage(gpuCapacity);
					cluster.getCondition().setLoadBalancerCount(loadBalancerCount);
				}

				if (!showAuth) {
					cluster.setApiSecret(null);
					cluster.setServerAuthData(null);
					cluster.setClientAuthData(null);
					cluster.setClientKeyData(null);
				}

			}

		}

		return clusters;
	}

	public List<ClusterNodePoolVO> getClusterNodePools(Integer clusterSeq, Integer serviceSeq, boolean usePod, ExecutingContextVO context) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getCluster(clusterSeq);

		List<ClusterNodePoolVO> clusterNodePools = new ArrayList<>();


		if (cluster != null && clusterStateService.isClusterRunning(cluster)) {
			long readyNodeCount = 0L;
			long desiredNodeCount = 0L;
			int gpuRequests = 0;
			int gpuCapacity = 0;
			long cpuRequests = 0L;
			long cpuCapacity = 0L;
			long memoryRequests = 0L;
			long memoryCapacity = 0L;

			List<K8sNodeVO> nodes = k8sResourceService.getNodes(clusterSeq, serviceSeq, usePod, false, context);

			if (CollectionUtils.isNotEmpty(nodes)) {
				ClusterNodePoolVO clusterNodePool = new ClusterNodePoolVO();
				clusterNodePool.setClusterSeq(clusterSeq);
				clusterNodePool.setNodePoolName("Default");
				clusterNodePool.setClusterState(cluster.getClusterState());
				clusterNodePool.setResources(Maps.newHashMap());

				desiredNodeCount = nodes.size();
				for (K8sNodeVO nodeRow : nodes) {
					if (StringUtils.equalsIgnoreCase("True", nodeRow.getReady())) {
						readyNodeCount++;
					}
					if (MapUtils.isNotEmpty(nodeRow.getCapacity())) {
						for (Map.Entry<String, Quantity> capacityEntryRow : nodeRow.getCapacity().entrySet()) {
							if (MapUtils.getObject(clusterNodePool.getResources(), capacityEntryRow.getKey(), null) == null) {
								clusterNodePool.getResources().put(capacityEntryRow.getKey(), new ClusterNodePoolResourceVO());
							}
							if (StringUtils.equals(capacityEntryRow.getKey(), KubeConstants.RESOURCES_GPU)) {
								clusterNodePool.setGpuResourceName(capacityEntryRow.getKey());
							}
							clusterNodePool.getResources().get(capacityEntryRow.getKey()).setCapacity(clusterNodePool.getResources().get(capacityEntryRow.getKey()).getCapacity() + capacityEntryRow.getValue().getNumber().longValue());
						}
					}
					if (MapUtils.isNotEmpty(nodeRow.getAllocatable())) {
						for (Map.Entry<String, Quantity> allocatableEntryRow : nodeRow.getAllocatable().entrySet()) {
							if (MapUtils.getObject(clusterNodePool.getResources(), allocatableEntryRow.getKey(), null) == null) {
								clusterNodePool.getResources().put(allocatableEntryRow.getKey(), new ClusterNodePoolResourceVO());
							}

							clusterNodePool.getResources().get(allocatableEntryRow.getKey()).setAllocatable(clusterNodePool.getResources().get(allocatableEntryRow.getKey()).getAllocatable() + allocatableEntryRow.getValue().getNumber().longValue());
						}
					}
					gpuRequests += nodeRow.getGpuRequests();
					gpuCapacity += nodeRow.getGpuCapacity();
					cpuRequests += nodeRow.getCpuRequests();
					cpuCapacity += nodeRow.getCpuCapacity();
					memoryRequests += nodeRow.getMemoryRequests();
					memoryCapacity += nodeRow.getMemoryCapacity();
				}


				clusterNodePool.setReadyNodeCount(readyNodeCount);
				clusterNodePool.setDesiredNodeCount(desiredNodeCount);
				clusterNodePool.setGpuRequests(gpuRequests);
				clusterNodePool.setGpuCapacity(gpuCapacity);
				clusterNodePool.setCpuRequests(cpuRequests);
				clusterNodePool.setCpuCapacity(cpuCapacity * 1000);
				clusterNodePool.setMemoryRequests(memoryRequests);
				clusterNodePool.setMemoryCapacity(memoryCapacity);

				if (MapUtils.getObject(clusterNodePool.getResources(), KubeConstants.RESOURCES_CPU, null) != null) {
					clusterNodePool.getResources().get(KubeConstants.RESOURCES_CPU).setRequest(cpuRequests);
					clusterNodePool.getResources().get(KubeConstants.RESOURCES_CPU).setAllocatable(clusterNodePool.getResources().get("cpu").getAllocatable() * 1000);
					clusterNodePool.getResources().get(KubeConstants.RESOURCES_CPU).setCapacity(clusterNodePool.getResources().get("cpu").getCapacity() * 1000);
				}
				if (MapUtils.getObject(clusterNodePool.getResources(), KubeConstants.RESOURCES_MEMORY, null) != null) {
					clusterNodePool.getResources().get(KubeConstants.RESOURCES_MEMORY).setRequest(memoryRequests);
				}
				if (MapUtils.getObject(clusterNodePool.getResources(), KubeConstants.RESOURCES_GPU, null) != null) {
					clusterNodePool.getResources().get(KubeConstants.RESOURCES_GPU).setRequest(gpuRequests);
				}

				clusterNodePools.add(clusterNodePool);

			}
		}


		return clusterNodePools;
	}

	public List<ClusterGateWayVO> getClusterGateWays(Integer clusterSeq, Integer serviceSeq, ExecutingContextVO context) throws Exception {

		// cluster 조회
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getCluster(clusterSeq);

		return this.getClusterGateWays(cluster, serviceSeq, context);
	}

	public List<ClusterGateWayVO> getClusterGateWays(ClusterVO cluster, Integer serviceSeq, ExecutingContextVO context) throws Exception {
		// 객체 생성  및 초기화
		List<ClusterGateWayVO> clusterGateWays = new ArrayList<>();

		ClusterGateWayVO ingress = new ClusterGateWayVO();
		ingress.setClusterSeq(cluster.getClusterSeq());
		ingress.setGateWayType(GateWayNameType.INGRESS);
		ingress.setGateWayState("Ready");
//		ingress.setGateWayDesc( String.format("Host : %s", cluster.getIngressHost()) );

		ClusterGateWayVO nodeport = new ClusterGateWayVO();
		nodeport.setClusterSeq(cluster.getClusterSeq());
		nodeport.setGateWayType(GateWayNameType.NODE_PORT);
		nodeport.setGateWayState("Ready");
		nodeport.setGateWayDesc( String.format("Host : %s, Port-Range : %s", cluster.getNodePortUrl(), cluster.getNodePortRange()) );

		ClusterGateWayVO loadBalancer = new ClusterGateWayVO();
		loadBalancer.setClusterSeq(cluster.getClusterSeq());
		loadBalancer.setGateWayState("Ready");
		loadBalancer.setGateWayType(GateWayNameType.LOAD_BALANCER);

		int ingressCnt = 0;
		int nodeportCnt = 0;
		int loadBalancerCnt = 0;

		// count 조회
		if(cluster != null && clusterStateService.isClusterRunning(cluster)) {
			Set<String> namespaces = new HashSet<>();
			if (serviceSeq != null) {
				namespaces = servicemapService.getNamespaceNamesByServiceInCluster(serviceSeq, cluster.getClusterSeq());
			}

			// Ingress 조회
			List<K8sIngressVO> ingressList = ingressSpecService.getIngresses(cluster, null, null, context);

			if(CollectionUtils.isNotEmpty(ingressList)){
				if (serviceSeq != null) {
					for(K8sIngressVO ingressVO: ingressList){
						if (CollectionUtils.isNotEmpty(namespaces) && namespaces.contains(ingressVO.getNamespace())) {
							ingressCnt++;
						}
					}
				} else {
					ingressCnt += ingressList.size();
				}
			}

			// Node Port, LoadBalancer 조회
			List<K8sServiceVO> k8sServices = serviceSpecService.getServices(cluster.getClusterSeq(), null, null, null, context);

			if(CollectionUtils.isNotEmpty(k8sServices)){

				for(K8sServiceVO serviceVO : k8sServices) {
					K8sServiceDetailVO detailVO = serviceVO.getDetail();
					if(detailVO != null) {

						if( StringUtils.equals(detailVO.getType(), KubeServiceTypes.NodePort.name()) ){ // Node Port
							if( org.apache.commons.collections4.CollectionUtils.isNotEmpty(serviceVO.getServicePorts()) ) {
								if (serviceSeq != null) {
									if (CollectionUtils.isNotEmpty(namespaces) && namespaces.contains(serviceVO.getNamespace())) {
										nodeportCnt += serviceVO.getServicePorts().size();
									}
								} else {
									nodeportCnt += serviceVO.getServicePorts().size();
								}
							}
						} else if( StringUtils.equalsIgnoreCase(detailVO.getType(), KubeServiceTypes.LoadBalancer.name()) ){ // LoadBalancer
							if (serviceSeq != null) {
								if (CollectionUtils.isNotEmpty(namespaces) && namespaces.contains(serviceVO.getNamespace())) {
									loadBalancerCnt++;
								}
							} else {
								loadBalancerCnt++;
							}
						}
					}
				}
			}
		}

		// 데이터 셋팅
		ingress.setGateWayCount(ingressCnt);
		clusterGateWays.add(ingress);
		if(nodeportCnt > 0 ){
			nodeport.setGateWayCount(nodeportCnt);
			clusterGateWays.add(nodeport);
		}
		if(loadBalancerCnt > 0 ){
			loadBalancer.setGateWayCount(loadBalancerCnt);
			clusterGateWays.add(loadBalancer);
		}

		return clusterGateWays;
	}

	public ClusterVO getClusterOfServicemap(Integer servicemapSeq) throws Exception {
		IClusterMapper clusterMapper = this.sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterMapper.getClusterByServicemap(servicemapSeq);

		if (cluster != null) {
			return cluster;
		} else {
			return new ClusterVO();
		}
	}

	//2018.11.21 ohs aws cube online
//	public ClusterAddVO addClusterProvider(ClusterAddVO clusterAddVO) throws Exception {
//		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
//		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
//		IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
//		ICubeClusterMapper iCubeClusterMapper = sqlSession.getMapper(ICubeClusterMapper.class);
//
//
//		ProviderAccountVO providerAccountVO = dao.getProviderAccount(clusterAddVO.getProviderAccountSeq());
//		if (providerAccountVO == null) {
//			throw new CocktailException("Unregistered Provider Account.", ExceptionType.InvalidParameter);
//		}
//		ProviderCode providerCode = providerAccountVO.getProviderCode();
//		if (!ProviderCode.AWS.equals(providerCode)) {
//			throw new CocktailException("Provider Code supports AWS only.", ExceptionType.InvalidParameter);
//		}
//
//
//		int clusterIdCnt = iCubeClusterMapper.checkDuplClusterId(clusterAddVO.getClusterId());
//		if (clusterIdCnt > 0) {
//			throw new CocktailException("Registered clusterId.", ExceptionType.InvalidParameter);
//		}
//
//		//K8S VERSION CHECK
//		CodeVO codeVO = codeService.getCode("K8S_VERSION", clusterAddVO.getK8sVersion());
//		if (codeVO == null) {
//			throw new CocktailException("Unregistered K8S Version.", ExceptionType.InvalidParameter);
//		}
//
//		//Region Check
//		codeVO = codeService.getCode("AWS_REGION", clusterAddVO.getRegionCode());
//		if (codeVO == null) {
//			throw new CocktailException("Unregistered AWS Region.", ExceptionType.InvalidParameter);
//		}
//
//		codeVO = codeService.getCode(clusterAddVO.getRegionCode(), clusterAddVO.getAvailabilityZone());
//		if (codeVO == null) {
//			throw new CocktailException("Unregistered AWS Availability Zone.", ExceptionType.InvalidParameter);
//		}
//
//		//master node
//		codeVO = codeService.getCode("AWS_EC_TYPE", clusterAddVO.getMasterVmSize());
//		if (codeVO == null) {
//			throw new CocktailException("Unregistered AWS instance-types.", ExceptionType.InvalidParameter);
//		}
//
//		if (!ckeckStorageSize(clusterAddVO.getMasterBootStorageType(), clusterAddVO.getMasterBootStorageSize())) {
//			throw new CocktailException("Check storage type or size", ExceptionType.InvalidParameter);
//		}
//
//		if (!ckeckStorageSize(clusterAddVO.getMasterDataStorageType(), clusterAddVO.getMasterDataStorageSize())) {
//			throw new CocktailException("Check storage type or size", ExceptionType.InvalidParameter);
//		}
//
//
//		//worker node
//		codeVO = codeService.getCode("AWS_EC_TYPE", clusterAddVO.getWorkerVmSize());
//		if (codeVO == null) {
//			throw new CocktailException("Unregistered AWS instance-types.", ExceptionType.InvalidParameter);
//		}
//
//
//		if (!ckeckStorageSize(clusterAddVO.getWorkerBootStorageType(), clusterAddVO.getWorkerBootStorageSize())) {
//			throw new CocktailException("Check storage type or size", ExceptionType.InvalidParameter);
//		}
//
//		if (!ckeckStorageSize(clusterAddVO.getWorkerDataStorageType(), clusterAddVO.getWorkerDataStorageSize())) {
//			throw new CocktailException("Check storage type or size", ExceptionType.InvalidParameter);
//		}
//
//
//		if (!ckeckStorageSize("gp2", clusterAddVO.getNfsStorageSize())) {
//			throw new CocktailException("Check storage size", ExceptionType.InvalidParameter);
//		}
//
//		//ssh key reading
//		String privateKey = getFileText(clusterAddVO.getPrivateKeyPath());
//		if (privateKey.length() == 0) {
//			throw new CocktailException("Check Private Key", ExceptionType.InvalidParameter);
//		}
//
//		String publicKey = getFileText(clusterAddVO.getKeyPath());
//		if (publicKey.length() == 0) {
//			throw new CocktailException("Check Public Key", ExceptionType.InvalidParameter);
//		}
//
//		Base64 base64 = new Base64();
//		String base64PrivateKey = base64.encodeToString(privateKey.getBytes());
//		String base64PublicKey = base64.encodeToString(publicKey.getBytes());
//
//		clusterAddVO.setSshPrivateKey(base64PrivateKey);
//		clusterAddVO.setSshPublicKey(base64PublicKey);
//
//		String clusterUuid = UUIDGenerator.uuid();
//		clusterAddVO.setClusterUuid(clusterUuid);
//
//		String yamlData = CubeYamlUtils.createAwsYaml(clusterAddVO);
//		yamlData = CryptoUtils.encryptAES(yamlData);
//		clusterAddVO.setCubeYamlData(yamlData);
//
//		clusterAddVO.setAuthType(AuthType.CERT);
//		clusterAddVO.setClusterState(ClusterState.CREATING.getCode());
//		clusterAddVO.setUseYn("Y");
//
//		//추가 사항
//		String aesPrivateKey = CryptoUtils.encryptAES(privateKey);
//		String aesPublicKey = CryptoUtils.encryptAES(publicKey);
//		clusterAddVO.setSshPrivateKey(aesPrivateKey);
//		clusterAddVO.setSshPublicKey(aesPublicKey);
//
//
//		//클러스트 등록
//		clusterDao.addCluster(clusterAddVO);
//
//
//		/**
//		 * add Cluster - Service
//		 */
//		List<Integer> clusterSeqs = Lists.newArrayList();
//		clusterSeqs.add(clusterAddVO.getClusterSeq());
//		serviceDao.addClustersOfService(clusterAddVO.getServiceSeq(), clusterSeqs, clusterAddVO.getCreator());
//
//		clusterAddVO.setClusterState(ClusterState.CREATING.getCode());
//		//cube cluster cloud 호출
//		cubeClusterService.callBuildCubeCluster(providerAccountVO, clusterAddVO);
//
//		// Response Data에서 사용하지 않는 데이터 제거
//		clusterAddVO.setSshPrivateKey(null);
//		clusterAddVO.setSshPublicKey(null);
//		clusterAddVO.setClusterUuid(null);
//		clusterAddVO.setCubeYamlData(null);
//
//		return clusterAddVO;
//	}
//
//	/**
//	 * cluster provision 로직
//	 */
//	//2018.11.26 ohs aws cube online remove
//	private void removeClusterProvider(ProviderAccountVO providerAccountVO, ClusterAddVO clusterAddVO) throws Exception {
//		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
//		serviceService.canDeleteServiceClusters(null, Arrays.asList(clusterAddVO.getClusterSeq()));
//		clusterDao.updateClusterState(clusterAddVO);
//		cubeClusterService.callBuildCubeCluster(providerAccountVO, clusterAddVO);
//	}
//
//	/**
//	 * cluster provision 로직
//	 */
//	//2018.11.21 파일 사이즈 체크
//	private boolean ckeckStorageSize(String storageType, int size) {
//		boolean result = false;
//
//		switch (storageType.toLowerCase()) {
//			case "gp2":
//				if (size < 2047)
//					result = true;
//				break;
//			case "standard":
//				if (size < 1024)
//					result = true;
//				break;
//		}
//		return result;
//	}
//
//	/**
//	 * cluster provision 로직
//	 */
//	//2018.11.21 파일에서 텍스트 추출
//	public String getFileText(String fileName) {
//
//		String result = "";
//
//		String filePath = "./tmp/data/files/" + fileName;
//
//		StringBuilder stringBuilder;
//		FileReader fileReader = null;
//		BufferedReader bufferedReader = null;
//		try {
//			stringBuilder = new StringBuilder();
//			fileReader = new FileReader(filePath);
//			bufferedReader = new BufferedReader(fileReader);
//			String line;
//			while ((line = bufferedReader.readLine()) != null)
//				stringBuilder.append(line).append('\n');
//
//			result = stringBuilder.toString();
//		} catch (IOException e) {
//
//		} finally {
//			if (bufferedReader != null) {
//				try {
//					bufferedReader.close();
//				} catch (Exception ex) { /* Do Nothing */ }
//			}
//			if (fileReader != null) {
//
//				try {
//					fileReader.close();
//				} catch (Exception ex) { /* Do Nothing */ }
//			}
//		}
//		return result;
//	}

	public String genControllerAuthKey(ClusterVO clusterVO) throws Exception {
		if(clusterVO == null) {
			throw new CocktailException("cluster not found", ExceptionType.InvalidParameter);
		}

		String payload = clusterVO.getClusterId() + clusterVO.getProviderAccountSeq().toString();
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		payload = payload + timestamp.getTime();

		return signatureUtils.getSignedHmacKey(payload);
	}

	/**
	 * CLUSTER 할당 유형에 따른 Pod 목록 조회..
	 * @param clusterSeq : Pod가 속해있는 Cluster의 Sequence
	 * @param serviceSeq : 현재 사용중인 serviceSeq(워크스페이스 Sequence)
	 * @param context : ExecutingContextVO
	 * @return
	 * @throws Exception
	 */
	public List<K8sPodVO> getPods(Integer clusterSeq, Integer serviceSeq, ExecutingContextVO context) throws Exception{
		return workloadResourceService.getPods(clusterSeq, null, serviceSeq, context);
	}

	public List<K8sPodVO> getPods(Integer clusterSeq, ExecutingContextVO context) throws Exception{
		String nodeName = null;
		String namespace = null;
		return workloadResourceService.getPods(clusterSeq, nodeName, namespace, context);
	}


	/**
	 * 클러스터의 할당 유형 및 상태 체크 이후 정상일 경우 Cluster 정보를 응답.
	 * @param clusterSeq
	 * @param accountSeq
	 * @param serviceSeq
	 * @return
	 * @throws Exception
	 */
	public ClusterVO getClusterInfoWithStateCheck(Integer clusterSeq, Integer accountSeq, Integer serviceSeq) throws Exception {
		/** service_cluster의 할당 유형 조회 **/
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		if (clusterSeq == null) {
			throw new CocktailException("Missing the required parameter 'clusterSeq' when calling getWorkloads", ExceptionType.InvalidParameter);
		}

		List<ClusterVO> clusters = clusterDao.getClusters(null, serviceSeq, clusterSeq, null, null, "Y");
		if(serviceSeq != null) {
			if (clusters == null || clusters.size() < 1) {
				throw new CocktailException("The cluster is not assigned to a current service..", ExceptionType.InvalidParameter);
			}
			if (clusters.size() > 1) {
				throw new CocktailException("The cluster is multiple assigned to a current service..", ExceptionType.InvalidParameter);
			}
		}

		// clusters.size == 1
		if(clusters == null || clusters.size() < 1) {
			return null;
		}

		ClusterVO cluster = clusters.get(0);

		/** cluster 상태 체크 **/
		clusterStateService.isClusterRunning(cluster);

		return cluster;
	}

	/**
	 * CLUSTER 할당 유형에 따른 워크로드 목록 조회
	 * @param clusterSeq : 워크로드가 속해있는 Cluster의 Sequence
	 * @param serviceSeq : 현재 사용중인 serviceSeq (워크스페이스 Sequence)
	 * @param canActualizeState : Kubernetes의 실시간 상태 적용 여부
	 * @param useAdditionalInfo : 추가 정보 (서비스 매핑, 컨테이너 목록, QoS 등) 조회 여부
	 * @param context : ExecutingContextVO
	 * @return
	 * @throws Exception
	 */
	public List<ComponentVO> getWorkloads(Integer clusterSeq,
	                                      Integer serviceSeq,
	                                      boolean canActualizeState,
	                                      boolean useAdditionalInfo,
	                                      boolean useExistingWorkload,
	                                      ExecutingContextVO context) throws Exception
	{
		/** Component 응답 객체 생성 **/
		List<ComponentVO> components = new ArrayList<>();

		try {
			ClusterVO cluster = this.getClusterInfoWithStateCheck(clusterSeq, null, serviceSeq);
			if (cluster == null) {
				return new ArrayList<>();
			}

			/** Cluster 내의 Component 상세 정보 및 Actualize State 조회 **/
			ComponentDetailsVO componentDetails = serverStateService.getComponentDetails(cluster, null, null, canActualizeState, useAdditionalInfo, context);

			if (componentDetails != null) {
				components.addAll(componentDetails.getComponents());
				ClusterUtils.setNullClusterInfo(cluster);
			}


			// K8s에 존재하는 워크로드만 조회요청일때 필터링..
			if (useExistingWorkload) {
				components = components.stream().filter(cp -> (BooleanUtils.isTrue(cp.getIsK8sResourceExist()))).collect(Collectors.toList());
			}

			try {
				components.sort(Comparator.comparing(ComponentVO::getCreationTimestamp));
				Collections.reverse(components);
			}
			catch (NullPointerException ex) {
				log.debug("Can't Sort Workload (Retry with Created) : ClusterService.getWorkloads()");
				components.sort(Comparator.comparing(s -> Optional.ofNullable(s.getCreated()).orElseGet(() ->"0")));
				Collections.reverse(components);
			}
			catch (Exception ex) {
				log.debug("Can't Sort Workload : ClusterService.getWorkloads()");
			}
			log.debug("::::::::::::::::::::::::::::::::::::::::::::: " + Optional.ofNullable(components).map(List::size).orElseGet(() ->0));
		}
		catch (Exception ex) {
			if(log.isDebugEnabled()) log.debug("trace log ", ex);
			throw new CocktailException(ExceptionType.CommonInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.CommonInquireFail, ExceptionBiz.SERVER);
		}

		return components;
	}

	/**
	 * CLUSTER 할당 유형에 따른 서비스노출 목록 조회
	 * @param clusterSeq : 서비스노출이 속해있는 Cluster의 Sequence
	 * @param serviceSeq : 현재 사용중인 serviceSeq (워크스페이스 Sequence)
	 * @param context : ExecutingContextVO
	 * @return
	 * @throws Exception
	 */
	public List<K8sServiceInfoVO> getServiceSpecs(Integer clusterSeq, Integer serviceSeq, ExecutingContextVO context) throws Exception {
		ClusterVO cluster = this.getClusterInfoWithStateCheck(clusterSeq, null, serviceSeq);
		if(cluster == null) {
			return new ArrayList<>();
		}

		/** Service 응답 객체 생성 **/
		List<K8sServiceInfoVO> services = new ArrayList<>();

		/** Cluster 내의 ServiceSpec List 조회 **/
		List<K8sServiceVO> k8sServices = serviceSpecService.getServices(cluster, null, null, null, context);

		if (CollectionUtils.isNotEmpty(k8sServices)) {
			/** 클러스터 내의 전체 Servicemap 연결 정보를 조회 (할당 유형 CLUSTER Type을 처리하기 위함)**/
			Map<String, ServicemapSummaryVO> servicemapInfoMap = servicemapService.getServicemapSummaryMap(clusterSeq);

			for(K8sServiceVO serviceSpec : k8sServices) {
				// K8sServiceVO -> K8sServiceInfoVO
				K8sServiceInfoVO info = new K8sServiceInfoVO();
				BeanUtils.copyProperties(serviceSpec, info);

				// namespace에 해당하는 서비스 연결 정보를 찾아서 셋팅
				if (MapUtils.isNotEmpty(servicemapInfoMap) && servicemapInfoMap.containsKey(serviceSpec.getNamespace())) {
					info.setServicemapInfo(servicemapInfoMap.get(serviceSpec.getNamespace()));
				}
				services.add(info);
			}
		}

        services.sort(Comparator.comparing(K8sServiceInfoVO::getCreationTimestamp));
        Collections.reverse(services);

		log.debug("::::::::::::::::::::::::::::::::::::::::::::: {}", Optional.ofNullable(services).map(List::size).orElseGet(() ->0));
		return services;
	}

	/**
	 * CLUSTER 할당 유형에 따른 인그레스 목록 조회
	 * @param clusterSeq : 인그레스가 속해있는 Cluster의 Sequence
	 * @param serviceSeq : 현재 사용중인 serviceSeq (워크스페이스 Sequence)
	 * @param context : ExecutingContextVO
	 * @return
	 * @throws Exception
	 */
	public List<K8sIngressInfoVO> getIngressSpecs(Integer clusterSeq, Integer serviceSeq, ExecutingContextVO context) throws Exception {
		ClusterVO cluster = this.getClusterInfoWithStateCheck(clusterSeq, null, serviceSeq);
		if(cluster == null) {
			return new ArrayList<>();
		}

		/** Ingress 응답 객체 생성 **/
		List<K8sIngressInfoVO> ingressSpecs = new ArrayList<>();

		/** Cluster 내의 Ingress List 조회 **/
		List<K8sIngressVO> k8sIngress = ingressSpecService.getIngresses(cluster, null, null, context);

		String resourcePrefixSource = ResourceUtil.getResourcePrefix();
		if (CollectionUtils.isNotEmpty(k8sIngress)) {
			/** 클러스터 내의 전체 Servicemap 연결 정보를 조회 (할당 유형 CLUSTER Type을 처리하기 위함)**/
			Map<String, ServicemapSummaryVO> servicemapInfoMap = servicemapService.getServicemapSummaryMap(clusterSeq);

			for(K8sIngressVO ingressSpec : k8sIngress) {
				// K8sIngressVO -> K8sIngressInfoVO
				K8sIngressInfoVO info = new K8sIngressInfoVO();
				BeanUtils.copyProperties(ingressSpec, info);

				// namespace에 해당하는 서비스 연결 정보를 찾아서 셋팅
				if (MapUtils.isNotEmpty(servicemapInfoMap) && servicemapInfoMap.containsKey(ingressSpec.getNamespace())) {
					info.setServicemapInfo(servicemapInfoMap.get(ingressSpec.getNamespace()));
				}
				ingressSpecs.add(info);
			}
		}
        ingressSpecs.sort(Comparator.comparing(K8sIngressInfoVO::getCreationTimestamp));
        Collections.reverse(ingressSpecs);

		log.debug("::::::::::::::::::::::::::::::::::::::::::::: {}", Optional.ofNullable(ingressSpecs).map(List::size).orElseGet(() ->0));
		return ingressSpecs;
	}

	/**
	 * CLUSTER 할당 유형에 따른 설정정보 목록 조회
	 * @param clusterSeq : 설정정보가 속해있는 Cluster의 Sequence
	 * @param serviceSeq : 현재 사용중인 serviceSeq (워크스페이스 Sequence)
	 * @param context : ExecutingContextVO
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<SettingInformationVO> getSettingInfomations(Integer clusterSeq, Integer serviceSeq, ExecutingContextVO context) throws Exception {
		ClusterVO cluster = this.getClusterInfoWithStateCheck(clusterSeq, null, serviceSeq);
		if (cluster == null) {
			return new ArrayList<>();
		}

		/** Ingress 응답 객체 생성 **/
		List<SettingInformationVO> setInfos = new ArrayList<>();

		/** 클러스터 내의 전체 Servicemap 연결 정보를 조회 (할당 유형 CLUSTER Type을 처리하기 위함)**/
		Map<String, ServicemapSummaryVO> servicemapInfoMap = servicemapService.getServicemapSummaryMap(clusterSeq);

		/**
		 * Cluster 내의 설정정보 List 조회 및 입력
		 */
		/** CONFIG MAP 입력 **/
		List<ConfigMapGuiVO> configMapList = configMapService.getConfigMaps(cluster, null, null, null);
		if (CollectionUtils.isNotEmpty(configMapList)) {
			for (ConfigMapGuiVO config : configMapList) {
				/** 설정정보에 입력 **/
				SettingInformationVO setInfo = new SettingInformationVO();
				setInfo.setName(config.getName());
				setInfo.setType(K8sApiKindType.CONFIG_MAP.getValue());
				setInfo.setCreationTimestamp(config.getCreationTimestamp());
				setInfo.setClusterSeq(cluster.getClusterSeq());
				setInfo.setClusterName(cluster.getClusterName());
				setInfo.setNamespaceName(config.getNamespace());
				// namespace에 해당하는 서비스 연결 정보를 찾아서 셋팅
				if (MapUtils.isNotEmpty(servicemapInfoMap) && servicemapInfoMap.containsKey(config.getNamespace())) {
					setInfo.setServicemapInfo(servicemapInfoMap.get(config.getNamespace()));
				}

				setInfos.add(setInfo);
			}
		}

		/** SECRET 입력 **/
		List<SecretGuiVO> secretList = secretService.getSecrets(cluster, null, null, null, true);
		if (CollectionUtils.isNotEmpty(secretList)) {
			for (SecretGuiVO secret : secretList) {
				/** 설정정보에 입력 **/
				SettingInformationVO setInfo = new SettingInformationVO();
				setInfo.setName(secret.getName());
				setInfo.setType(K8sApiKindType.SECRET.getValue());
				setInfo.setCreationTimestamp(secret.getCreationTimestamp());
				setInfo.setClusterSeq(cluster.getClusterSeq());
				setInfo.setClusterName(cluster.getClusterName());
				setInfo.setNamespaceName(secret.getNamespace());
				// namespace에 해당하는 서비스 연결 정보를 찾아서 셋팅
				if (MapUtils.isNotEmpty(servicemapInfoMap) && servicemapInfoMap.containsKey(secret.getNamespace())) {
					setInfo.setServicemapInfo(servicemapInfoMap.get(secret.getNamespace()));
				}

				setInfos.add(setInfo);
			}
		}

		/** NETWORK 입력 **/
		List<Map<String, Object>> customObjects = crdResourceService.getCustomObjects(cluster, null, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, null);
		if (CollectionUtils.isNotEmpty(customObjects)) {
			for (Map<String, Object> resultRow : customObjects) {
				Map<String, Object> metadata = (Map<String, Object>)resultRow.get(KubeConstants.META);
				SettingInformationVO setInfo = new SettingInformationVO();
				setInfo.setName((String)metadata.get("name"));
				setInfo.setType(K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION.getValue());
				setInfo.setCreationTimestamp(OffsetDateTime.parse((String)metadata.get("creationTimestamp")));
				setInfo.setClusterSeq(cluster.getClusterSeq());
				setInfo.setClusterName(cluster.getClusterName());
				setInfo.setNamespaceName((String)metadata.get("namespace"));
				// namespace에 해당하는 서비스 연결 정보를 찾아서 셋팅
				if (MapUtils.isNotEmpty(servicemapInfoMap) && servicemapInfoMap.containsKey(setInfo.getNamespaceName())) {
					setInfo.setServicemapInfo(servicemapInfoMap.get(setInfo.getNamespaceName()));
				}
				// Cocktail에서 Management하는 리소스 판단..
//				Map<String, String> labelMap = metadata.get("labels") != null ? (Map<String, String>)metadata.get(KubeConstants.META_LABELS) : Maps.newHashMap();

				setInfos.add(setInfo);
			}
		}

		setInfos.sort(Comparator.comparing(SettingInformationVO::getCreationTimestamp));
		Collections.reverse(setInfos);

		if (log.isDebugEnabled() || log.isTraceEnabled()) {
//			SimpleDateFormat format = new SimpleDateFormat("YYYY MM dd HH:mm:ss", Locale.UK);
			int i = 1;
			for(SettingInformationVO set : setInfos) {
				log.debug(String.format("##### %d : %s : %s : %s : %s : %s", i++, set.getCreationTimestamp().toString(), set.getName(), set.getNamespaceName(), set.getType(), JsonUtils.toGson(set.getServicemapInfo())));
			}
			log.debug("::::::::::::::::::::::::::::::::::::::::::::: {}", Optional.ofNullable(setInfos).map(List::size).orElseGet(() ->0));
		}

		return setInfos;
	}

	/**
	 * CLUSTER 할당 유형에 따른 패키지 목록 조회

	 * @param clusterSeq : 워크로드가 속해있는 Cluster의 Sequence
	 * @param serviceSeq : 현재 사용중인 serviceSeq (워크스페이스 Sequence)
	 * @param useAddon
	 * @param context : ExecutingContextVO
	 * @return
	 * @throws Exception
	 */
	public List<HelmReleaseBaseVO> getPackages(Integer clusterSeq,
	                                           Integer serviceSeq,
	                                           boolean useAddon,
	                                           ExecutingContextVO context) throws Exception
	{
		/** Packages 응답 객체 생성 **/
		List<HelmReleaseBaseVO> packages = new ArrayList<>();

		try {
			ClusterVO cluster = this.getClusterInfoWithStateCheck(clusterSeq, null, serviceSeq);
			if (cluster == null) {
				return new ArrayList<>();
			}

			/** Cluster 내의 Package목록 조회 **/
			List<HelmReleaseBaseVO> packageList = packageInfoService.getPackages(clusterSeq, null, null);

			List<ConfigMapGuiVO> addons = null;
			if(!useAddon) {
				/** Addon은 제거하기 위해 Addon 목록 조회.. **/
				// 1. Addon 목록을 조회
				String label1 = String.format("%s!=%s", KubeConstants.LABELS_ADDON_CHART_KEY, KubeConstants.LABELS_ADDON_CHART_VALUE_COCKTAIL);
				String label2 = String.format("%s=%s,%s,%s", KubeConstants.LABELS_ADDON_AGENT_KEY, KubeConstants.LABELS_ADDON_AGENT_KEY, KubeConstants.LABELS_ADDON_CHART_KEY, KubeConstants.LABELS_HELM_CHART_KEY);
				String labelSelectors = String.format("%s,%s", label1, label2);
				addons = configMapService.getConfigMaps(cluster, cocktailAddonProperties.getAddonNamespace(), null, labelSelectors);
			}

			for (HelmReleaseBaseVO pack : Optional.ofNullable(packageList).orElseGet(() ->Lists.newArrayList())) {
				/** useAddon이 false 이면 Addon은 목록에서 제거한다. **/
				if(!useAddon && CollectionUtils.isNotEmpty(addons)) {
					boolean isSkipAddon = false;
					for(ConfigMapGuiVO ao : addons) {
						if(StringUtils.equalsIgnoreCase(Optional.ofNullable(ao.getData()).orElseGet(() ->Maps.newHashMap()).get("releaseNamespace"), pack.getNamespace()) &&
							StringUtils.equalsIgnoreCase(Optional.ofNullable(ao.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_HELM_CHART_KEY), pack.getName()))
						{
							isSkipAddon = true;
							break;
						}
					}
					if(isSkipAddon) continue;
				}

				/** 서비스 연결 정보 설정 후 List에 추가 : 서비스 연결 정보는 Package List에서 처리하므로 제거..**/
				packages.add(pack);
			}

			try {
				packages.sort(Comparator.comparing(s -> s.getInfo().getLastDeployed()));
				Collections.reverse(packages);
			}
			catch (NullPointerException ex) {
				log.debug("Can't Sort Package (Retry with PackageName ASC) : ClusterService.getPackages()");
				packages.sort(Comparator.comparing(s -> Optional.ofNullable(s.getName()).orElseGet(() ->"")));
			}
			catch (Exception ex) {
				log.debug("Can't Sort Package : ClusterService.getPackages()");
			}
			log.debug("::::::::::::::::::::::::::::::::::::::::::::: " + Optional.ofNullable(packages).map(List::size).orElseGet(() ->0));
		}
		catch (Exception ex) {
			if(log.isDebugEnabled()) log.debug("trace log ", ex);
			throw new CocktailException(ExceptionType.PackageListInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageListInquireFail, ExceptionBiz.PACKAGE_SERVER);
		}

		return packages;
	}

}