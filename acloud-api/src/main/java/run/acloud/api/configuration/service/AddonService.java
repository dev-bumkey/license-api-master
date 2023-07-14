package run.acloud.api.configuration.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Secret;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.parser.ParserException;
import run.acloud.api.catalog.service.PackageCommonService;
import run.acloud.api.catalog.service.PackageK8sService;
import run.acloud.api.catalog.service.PackageValidService;
import run.acloud.api.catalog.vo.ChartInfoBaseVO;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.catalog.vo.HelmResourcesVO;
import run.acloud.api.configuration.constants.AddonConstants;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.enums.AddonDynamicValueType;
import run.acloud.api.configuration.enums.AddonKeyItem;
import run.acloud.api.configuration.enums.ClusterAddonType;
import run.acloud.api.configuration.util.AddonUtils;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.log.service.LogAgentService;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.ImagePullPolicyType;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.CRUDCommand;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CertificateUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailAddonProperties;
import run.acloud.framework.properties.CocktailRegistryProperties;

import javax.security.auth.x500.X500PrivateCredential;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AddonService {
	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private K8sResourceService k8sResourceService;

	@Autowired
	private PackageCommonService packageCommonService;

	@Autowired
	private PackageValidService packageValidService;

	@Autowired
	private PackageK8sService packageK8sService;

	@Autowired
	private IngressSpecService ingressSpecService;

	@Autowired
	private ServiceSpecService serviceSpecService;

	@Autowired
	private ConfigMapService configMapService;

	@Autowired
	private NamespaceService namespaceService;

	@Autowired
	private SecretService secretService;

	@Autowired
	private PersistentVolumeService persistentVolumeService;

	@Autowired
	private StorageClassService storageClassService;

	@Autowired
	private WorkloadResourceService workloadResourceService;

	@Autowired
	private ClusterAccessAuthService clusterAccessAuthService;

	@Autowired
	private ClusterStateService clusterStateService;

	@Autowired
	private CocktailAddonProperties cocktailAddonProperties;

	@Autowired
	private CocktailRegistryProperties cocktailRegistryProperties;

	@Autowired
	private AddonCommonService addonCommonService;

	@Autowired
	private K8sPatchSpecFactory k8sPatchSpecFactory;

	@Autowired
	private LogAgentService logAgentService;

	/**
	 * Cluster 설치시 기본으로 설치되는 Addon을 설치 한다...
	 * - addon-manager
	 * @param addCluster
	 * @param releaseName
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public List<ConfigMapGuiVO> installDefaultAddon(ClusterAddVO addCluster, String releaseName) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getCluster(addCluster.getClusterSeq());

		/** 01. Latest Addon Manager를 조회 **/
		String addonManagerVersion = cocktailAddonProperties.getAddonManagerChartVersion();
		if(StringUtils.equalsIgnoreCase(addonManagerVersion, "latest")) {
			ChartInfoBaseVO chartVersionInfo = packageCommonService.getLastestChartVersion(cocktailAddonProperties.getAddonChartRepoProjectName(), cocktailAddonProperties.getAddonManagerChartName());
			addonManagerVersion = chartVersionInfo.getVersion();
		}

		/** 02. Addon Manager를 설치한다. **/
		ChartInfoBaseVO chartInfo = packageCommonService.getChart(cocktailAddonProperties.getAddonChartRepoProjectName(), cocktailAddonProperties.getAddonManagerChartName(), addonManagerVersion);

		AddonInstallVO addonInstall = new AddonInstallVO();
		BeanUtils.copyProperties(chartInfo, addonInstall);
		addonInstall.setReleaseName(releaseName);
		addonInstall.setRepo(chartInfo.getRepo());
		addonInstall.setName(chartInfo.getName());
		addonInstall.setVersion(chartInfo.getVersion());

		try {
			return this.installAddon(cluster, addonInstall, clusterDao, true);
		}
		catch (CocktailException ce) {
			if(ce.getType() == ExceptionType.AddonAlreadyInstalled) {
				this.registerCocktailToAddonManager(cluster);
			}
			else {
				throw ce;
			}
		}
		catch (Exception ex) {
			throw ex;
		}

		return null;
//		return this.installAddon(cluster, addonInstall, clusterDao,false);
	}

	/**
	 * Addon Manager에 Cocktail 등록.
	 * @param cluster
	 * @throws Exception
	 */
	protected void registerCocktailToAddonManager(ClusterVO cluster) throws Exception {
		String addonManagerName = cocktailAddonProperties.getAddonManagerConfigmapPrefix();
		ConfigMapGuiVO configMap = this.getAddonConfigMap(cluster, addonManagerName, true);
		if (configMap == null) {
			return;
		}

		String registeredCocktails = Optional.ofNullable(configMap.getData()).orElseGet(() ->Maps.newHashMap()).get(AddonConstants.ADDON_DATA_REGISTERED_COCKTAILS);
		List<JsonObject> patchBody = new ArrayList<>();
		Map<String, Object> registeredCocktailMap = new HashMap<>();
		registeredCocktailMap.put("path", String.format("/data/%s", AddonConstants.ADDON_DATA_REGISTERED_COCKTAILS));

		boolean doInit = false; // 초기화 여부 (데이터가 올바르지 않으면 초기화)
		String value = null; // registeredCocktail value init

		/** 01. RegisteredCocktails에 값이 없는 경우... => 초기화 필요 **/
		if (StringUtils.isBlank(registeredCocktails)) {
			if (Optional.ofNullable(configMap.getData()).orElseGet(() ->Maps.newHashMap()).containsKey(AddonConstants.ADDON_DATA_REGISTERED_COCKTAILS)) {
				registeredCocktailMap.put("op", JsonPatchOp.REPLACE.getValue());
			}
			else {
				registeredCocktailMap.put("op", JsonPatchOp.ADD.getValue());
			}

			doInit = true; // do Initialize
		}
		/** 02. RegisteredCocktails에 값이 있는 경우... */
		else {
			/** 02-01. Read registeredCocktails... **/
			List<String> cocktails = null;
			try {
				cocktails = JsonUtils.fromGson(Optional.ofNullable(configMap.getData()).orElseGet(() ->Maps.newHashMap()).get(AddonConstants.ADDON_DATA_REGISTERED_COCKTAILS), new TypeToken<List<String>>(){}.getType());
			}
			catch (Exception e) {
				// 변환중 오류 = data format이 이상함... => 리셋!
				doInit = true; // do Initialize
			}

			/** 02-02. Read 오류가 없으면 현재 System이 List에 있는지 확인하여 1. 있으면 현상 유지 / 2. 없으면 현재 System을 List에 추가 **/
			if(!doInit) {
				/** RegisteredCocktails에 현재 System이 존재하면 Skip / 존재하지 않으면 추가 해줌... **/
				if(CollectionUtils.isNotEmpty(cocktails)) {
					boolean foundCurrentCocktail = false;
					for (String key : cocktails) {
						if(StringUtils.equals(key, ResourceUtil.getResourcePrefix())) {
							foundCurrentCocktail = true;
							break;
						}
					}

					if(!foundCurrentCocktail) {
						/** 현재 System이 존재하지 않으면 추가 **/
						cocktails.add(ResourceUtil.getResourcePrefix());
					}

					value = JsonUtils.toGson(cocktails); // value값 설정...
				}
				else {
					doInit = true; // do Initialize
				}
			}
		}

		/** 03. 초기화가 필요한 상태이면.. = registeredCocktail 데이터가 invalid 하면 => value값을 현재 System으로 초기화... */
		if(doInit) { // value 초기화...
			List<String> valueArr = new ArrayList<>();
			valueArr.add(ResourceUtil.getResourcePrefix());
			value = JsonUtils.toGson(valueArr);
		}

		/** 04. Data Update : RegisteredCocktails에 값이 있으므로 무조건 Replace **/
		registeredCocktailMap.put("op", JsonPatchOp.REPLACE.getValue());
		registeredCocktailMap.put("value", value);
		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(registeredCocktailMap), JsonElement.class)).getAsJsonObject());

		if (CollectionUtils.isNotEmpty(patchBody)) {
			configMapService.patchConfigMap(cluster.getClusterSeq(), this.getCocktailAddonNamespace(), addonManagerName, patchBody);
		}
	}

	/**
	 * Addon Install 전, 이미 Fix 되어 있는 AddonName을 사용하는 경우 오류 응답... (Unusable Addon Name...)
	 * @param cluster
	 * @param addonInstall
	 * @throws Exception
	 */
	private void checkAddonName(ClusterVO cluster, AddonInstallVO addonInstall) throws Exception {
		if (!StringUtils.equalsIgnoreCase(addonInstall.getName(), "addon-manager")) {
			if(StringUtils.equalsIgnoreCase(addonInstall.getReleaseName(), "addon-manager")) {
				throw new CocktailException("Unusable AddonName : " + addonInstall.getReleaseName(), ExceptionType.InvalidAddonName_FixedName);
			}
		}

//		if (!StringUtils.equalsIgnoreCase(addonInstall.getName(), "istio-init")) {
//			if(StringUtils.equalsIgnoreCase(addonInstall.getReleaseName(), "istio-init")) {
//				throw new CocktailException("Unusable AddonName : " + addonInstall.getReleaseName(), ExceptionType.InvalidAddonName_FixedName);
//			}
//		}
//
//		if (!StringUtils.equalsIgnoreCase(addonInstall.getName(), "istio")) {
//			if(StringUtils.equalsIgnoreCase(addonInstall.getReleaseName(), "istio")) {
//				throw new CocktailException("Unusable AddonName : " + addonInstall.getReleaseName(), ExceptionType.InvalidAddonName_FixedName);
//			}
//		}
//
//		if (!StringUtils.equalsIgnoreCase(addonInstall.getName(), "istiod")) {
//			if(StringUtils.equalsIgnoreCase(addonInstall.getReleaseName(), "istiod")) {
//				throw new CocktailException("Unusable AddonName : " + addonInstall.getReleaseName(), ExceptionType.InvalidAddonName_FixedName);
//			}
//		}
//
//		if (!StringUtils.equalsIgnoreCase(addonInstall.getName(), "prometheus-operator")) {
//			if(StringUtils.equalsIgnoreCase(addonInstall.getReleaseName(), "monitoring")) {
//				throw new CocktailException("Unusable AddonName : " + addonInstall.getReleaseName(), ExceptionType.InvalidAddonName_FixedName);
//			}
//		}

		if (!StringUtils.equalsIgnoreCase(addonInstall.getName(), "monitoring-agent")) {
			String agentReleaseName = String.format("%s-%s-%d", cocktailAddonProperties.getMonitoringAgentConfigmapPrefix(), ResourceUtil.getResourcePrefix(), cluster.getClusterSeq());
			if(StringUtils.equalsIgnoreCase(addonInstall.getReleaseName(), agentReleaseName)) {
				throw new CocktailException("Unusable AddonName : " + addonInstall.getReleaseName(), ExceptionType.InvalidAddonName_FixedName);
			}
		}

	}

	/**
	 * 공통 Addon Install
	 * @param clusterSeq cluster sequence
	 * @param addonInstall addon installation information
	 * @param apiVersion api version
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> installAddon(Integer clusterSeq, AddonInstallVO addonInstall, String apiVersion) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getCluster(clusterSeq);

		switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
			case V2:
				return this.installAddon(cluster, addonInstall, clusterDao,true);
			case V3:
				return this.installAddon(cluster, addonInstall, clusterDao,true, ApiVersionType.V3);
			default:
				throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
		}
	}

	/**
	 * 공통 Addon Install (default version = v2)
	 * @param clusterSeq cluster sequence
	 * @param addonInstall addon installation information
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> installAddon(Integer clusterSeq, AddonInstallVO addonInstall) throws Exception {
		return this.installAddon(clusterSeq, addonInstall, "v2");
	}

	/**
	 * Addon Install Processing
	 * @param cluster cluster Information
	 * @param addonInstall addon installation information
	 * @param clusterDao clusterDao
	 * @param isThrow Addon이 이미 설치되어 있을때 throw 할지 무시할지 여부... (이미 설치되어 있다면 무시하고 싶을때 사용한다...)
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> installAddon(ClusterVO cluster, AddonInstallVO addonInstall, IClusterMapper clusterDao, boolean isThrow) throws Exception {
		return this.installAddon(cluster, addonInstall, clusterDao, isThrow, ApiVersionType.V2);
	}

	/**
	 * Addon Install Processing
	 * @param cluster cluster Information
	 * @param addonInstall addon installation information
	 * @param clusterDao clusterDao
	 * @param isThrow Addon이 이미 설치되어 있을때 throw 할지 무시할지 여부... (이미 설치되어 있다면 무시하고 싶을때 사용한다...)
	 * @param apiVersionType API Version
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> installAddon(ClusterVO cluster, AddonInstallVO addonInstall, IClusterMapper clusterDao, boolean isThrow, ApiVersionType apiVersionType) throws Exception {
		this.checkAddonName(cluster, addonInstall);
		String releaseName = addonInstall.getReleaseName();
		/** Specific case : Monitoring Aget 설치일때는 releaseName을 api-server에서 자동으로 생성한다. Rule : agent-{ResourcePrefix}-{clusterSeq} **/
		if (StringUtils.equalsIgnoreCase(addonInstall.getName(), "monitoring-agent")) {
			releaseName = String.format("%s-%s-%d", cocktailAddonProperties.getMonitoringAgentConfigmapPrefix(), ResourceUtil.getResourcePrefix(), cluster.getClusterSeq());
		}
		/** Specific case : Monitoring Agent 설치일때 API-Server에서 생성한 releaseName으로 addonToml 내용을 Update **/
		addonInstall.setAddonToml(StringUtils.replace(addonInstall.getAddonToml(), "The_system_creates_it_automatically", releaseName));

		String chartName = addonInstall.getName();
		String chartVersion = addonInstall.getVersion();

		if(StringUtils.isBlank(addonInstall.getRepo())) {
			addonInstall.setRepo(cocktailAddonProperties.getAddonChartRepoProjectName());
		}

		List<ConfigMapGuiVO> configMapInstallationInfo = new ArrayList<>();
		List<HelmReleaseBaseVO> packageInstallationInfo = new ArrayList<>();

		/**
		 * Add-on Installation.
		 */
		try {
			//TODO : Front에서 넘어오는 Toml Format 오류 Patch.. 향후 보완 필요.
			addonInstall.setAddonToml(StringUtils.replace(addonInstall.getAddonToml(), "\"undefined\"", "undefined"));
			addonInstall.setAddonToml(StringUtils.replace(addonInstall.getAddonToml(), "undefined", "\"undefined\""));

			String lbType = null;
			/** 01. Chart 정보 조회 **/
			// getConfigToml & 설정정보 Loading.
			ChartInfoBaseVO chartInfo = packageCommonService.getChart(addonInstall.getRepo(), chartName, chartVersion);
//			if(ContextHolder.isTest()) {
//				chartInfo.setConfigToml(this.getTestTomlData());
//			}
			if (StringUtils.isBlank(chartInfo.getConfigToml())) {
				throw new CocktailException("addon installation is canceled.. because could not found addon chart..", ExceptionType.InvalidInputData, "addon installation is canceled.. because could not found addon chart..");
			}

			/** 02. Read configToml **/
			Toml configToml = new Toml().read(chartInfo.getConfigToml());
			Toml common = configToml.getTable(AddonConstants.ADDON_CONFIG_COMMON);
			Toml dependency = configToml.getTable(AddonConstants.ADDON_CONFIG_DEPENDENCY);

			// Namespace 설정
			String addonBaseNamespace = common.getString(KubeConstants.META_NAMESPACE);
			addonBaseNamespace = StringUtils.isNotBlank(addonBaseNamespace) ? addonBaseNamespace : this.getCocktailAddonNamespace();
			cluster.setNamespaceName(addonBaseNamespace);

			String releaseNamespace = null;

			/** Namespace가 존재하지 않으면 생성 **/
			this.createNamespaceIfNotExist(cluster, addonBaseNamespace);

			/** Addon의 Configmap이 이미 존재하는지 확인... **/
			ConfigMapGuiVO existConfigmap = configMapService.getConfigMap(cluster.getClusterSeq(), addonBaseNamespace, releaseName);
			if(existConfigmap != null) {
				if(isThrow) {
					throw new CocktailException(releaseName + " already installed...", ExceptionType.AddonAlreadyInstalled);
				}
				else {
					log.warn("=====# : Skip " + releaseName + " Installation. because of already installed...");
					return null;
				}
			}

			/** release Name이 이미 사용중인지 확인... **/
			if(packageValidService.isUsingReleaseName(cluster.getClusterSeq(), addonBaseNamespace, releaseName, false)) {
				if(isThrow) {
					throw new CocktailException(releaseName + " already installed...", ExceptionType.AddonAlreadyInstalled);
				}
				else {
					log.warn("=====# : Skip " + releaseName + " Installation. because of already installed...");
					return null;
				}
			}
			String fixedReleaseName = common.getString(AddonConstants.ADDON_CONFIG_COMMON_FIXED_RELEASE_NAME);
			if(StringUtils.isNotBlank(fixedReleaseName)) {
				if(!StringUtils.equals(fixedReleaseName, releaseName)) {
					throw new CocktailException("This addon can only be installed with the releaseName " + fixedReleaseName, ExceptionType.InvalidAddonName_FixedName);
				}
			}

			/** 03. Max Installation 설정에 따라 설치 여부 판단 **/
			Long maxInstallation = common.getLong(AddonConstants.ADDON_CONFIG_COMMON_MAX_INSTALLATION);
			if(maxInstallation != null) {
				/** 설치된 Addon 갯수를 조회하고, maxInstallation 갯수를 초과시 설치할 수 없도록 처리함 **/
				List<ConfigMapGuiVO> addonList = addonCommonService.getAddonConfigMaps(cluster.getClusterSeq(), null);
				List<ConfigMapGuiVO> currentAddonList = addonList.stream().filter(ao -> StringUtils.equalsIgnoreCase(Optional.ofNullable(ao.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_CHART_KEY), chartName)).collect(Collectors.toList());
				if(CollectionUtils.isNotEmpty(currentAddonList)) {
					if(maxInstallation < currentAddonList.size()) {
						throw new CocktailException("The number of installable add-ons has been exceeded. : " + maxInstallation + " : " + currentAddonList.size(), ExceptionType.AddonCanNoLonngerBeInstalled);
					}
				}
			}

			/** 04. Dependency를 판단하여 pre/post 설치가 가능할 수 있도록 설정 **/
			List<Toml> preInstallationList = null;
			List<Toml> postInstallationList = null;
			if(dependency != null) {
				preInstallationList = dependency.getTables(AddonConstants.ADDON_CONFIG_DEPENDENCY_PREINSTALLATION);
				postInstallationList = dependency.getTables(AddonConstants.ADDON_CONFIG_DEPENDENCY_POSTINSTALLATION);
			}

			/** Start Dynamic Value 설정. ======================= **/
			Map<String, String> dynamicValues = this.setUpDynamicValues(null, releaseName, fixedReleaseName, chartInfo, cluster);
			/** Ended Dynamic Value 설정. ======================= **/

			/** Specific case : Istio 설치 요청시 **/ /** 2021.04.07 istio Addon 설치시 인증서 생성등 Specific case로 처리하던 내용 삭제 **/
			//TODO : To be deleted
			if (false && StringUtils.equalsIgnoreCase(addonInstall.getName(), "istio")) {
				/** 01. Istio-init 설치가 되어있는지 확인한다. **/
				ConfigMapGuiVO istioInit = configMapService.getConfigMap(cluster.getClusterSeq(), addonBaseNamespace, "istio-init");
				if (istioInit == null) {
					throw new CocktailException("You need to install istio-init first.", ExceptionType.AddonIstioInitRequired);
				}

				/** 02. istio addon일 경우 인증서 추가 작업 필요...**/
				// TODO : addon.toml에서 처리하려 하였으나 애매한 케이스라 소스코드에 우선 구현....
				Toml addonToml = new Toml().read(addonInstall.getAddonToml());
				lbType = addonToml.getString(AddonConstants.ADDON_VALUE_DETAIL + ".gateways_istio-ingressgateway_type.value");
				String address = addonToml.getString(AddonConstants.ADDON_VALUE_DETAIL + ".gateways_istio-ingressgateway_ip.value");
				if(KubeConstants.SPEC_TYPE_VALUE_NODE_PORT.equals(lbType) && StringUtils.isBlank(address)) {
					throw new CocktailException("Ingress Gateway is required.", ExceptionType.InvalidParameter_Empty, "Ingress Gateway is required.");
				}
				String domain = null;
				String ipAddress = null;
				if (Utils.checkIPv4Address(address)) {
					ipAddress = address;
				}
				else {
					domain = address;
				}
				Long portNumber = addonToml.getLong(AddonConstants.ADDON_VALUE_DETAIL + ".gateways_istio-ingressgateway_ports_httpsNodePort.value");

				/** Create Certificate **/
				X500PrivateCredential serverCred = CertificateUtils.createRootCredential(true);

				/** 인증서 생성 및 DynamicValues에 입력 **/
				dynamicValues = addonCommonService.setCertificateToDynamicValue(serverCred, dynamicValues, domain, ipAddress);

				if(StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_NODE_PORT, lbType)) {
					dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_ADDRESS_LIST.getValue(), address + ":" + portNumber.toString()); //101.55.69.106
				}
				else if(StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_LOADBALANCER, lbType)) {
					dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_ADDRESS_LIST.getValue(), cluster.getNodePortUrl() + ":" + portNumber.toString());

					List<Toml> configmapTomlList = configToml.getTables(AddonConstants.ADDON_CONFIG_CONFIGMAP);
					if(CollectionUtils.isNotEmpty(configmapTomlList)) {
						for(Toml configmap : configmapTomlList) {
							/** istio configmap을 찾아서 releaseNamespace를 설정 **/
							Toml configmapLabels = configmap.getTable(AddonConstants.ADDON_CONFIG_CONFIGMAP_LABELS);
							String labelChartName = configmapLabels.getString(KubeConstants.LABELS_ADDON_CHART_KEY);
							labelChartName = addonCommonService.convertDynamicValues(labelChartName, dynamicValues);

							Toml configmapDatas = configmap.getTable(AddonConstants.ADDON_CONFIG_CONFIGMAP_DATA);
							if(StringUtils.equalsIgnoreCase(labelChartName, "istio")) {
								releaseNamespace = configmapDatas.getString(KubeConstants.ADDON_NAMESPACE_KEY);
								releaseNamespace = StringUtils.isNotBlank(releaseNamespace) ? releaseNamespace : addonBaseNamespace;
								/** Namespace가 존재하지 않으면 생성 **/
								this.createNamespaceIfNotExist(cluster, releaseNamespace);
								break;
							}
						}
					}
				}
			}

			/** 05. Installation Step에 따라 설치를 진행 **/
			List<String> installationSteps = common.getList(AddonConstants.ADDON_CONFIG_COMMON_INSTALLATION_STEP);
			if(CollectionUtils.isNotEmpty(installationSteps)) {
				// TODO : Pre Installation
				if(CollectionUtils.isNotEmpty(preInstallationList)) {
					for(Toml pre : preInstallationList) {
						/**
						 * Pre Installation이 있다면 함께 처리하기 위한 자리...
						 */
						log.debug("=====# : preInstallation : " + pre.getString("name"));
						String preType = pre.getString("type");

						/** chart type일 경우 **/
						if(StringUtils.equalsIgnoreCase(preType, "Chart")) {

							// 1. Chart 정보 설정
							String preChartRepo = pre.getString("repo");
							String preChartName = pre.getString("name");
							String preChartVersion = pre.getString("version");
							String preReleaseName = null;
							Long delay = pre.getLong("delay");

							// 2. Chart 정보 조회
							ChartInfoBaseVO preChartInfo = packageCommonService.getChart(preChartRepo, preChartName, preChartVersion);

							// 3. Chart Toml 정보를 Parsing 하여 releaseName을 얻는다.
							Toml preAddonToml = new Toml().read(preChartInfo.getAddonToml());
							Toml preRelease = preAddonToml.getTable(AddonConstants.ADDON_VALUE_RELEASE);
							if(preRelease != null) {
								Toml preReleaseInfo = preRelease.getTable(AddonConstants.ADDON_VALUE_RELEASE_NAME);
								preReleaseName = preReleaseInfo.getString("default"); // default release name 조회
							}

							// 4. Pre Installation
							if(StringUtils.isNotBlank(preReleaseName)) {
								AddonInstallVO preAddonInstall = new AddonInstallVO();
								preAddonInstall.setReleaseName(preReleaseName);
								preAddonInstall.setRepo(preChartInfo.getRepo());
								preAddonInstall.setName(preChartInfo.getName());
								preAddonInstall.setVersion(preChartInfo.getVersion());
								preAddonInstall.setDefaultValueYaml(preChartInfo.getDefaultValueYaml());
								preAddonInstall.setAddonToml(preChartInfo.getAddonToml());
								preAddonInstall.setAddonYaml(preChartInfo.getAddonYaml());
								preAddonInstall.setConfigToml(preChartInfo.getConfigToml());

								// Recursive Call..!
								// isThrow를 false로 하여 이미 해당 Addon이 설치되어 있다면 Skip 하도록 한다...
								this.installAddon(cluster, preAddonInstall, clusterDao, false);
							}

							// 5. Delay 설정이 있으면 해당 시간동안 대기...
							if(delay != null) {
								try {
									long lDelay = 1000 * delay;
									Thread.sleep(lDelay);
								}
								catch (Exception ex) {
									log.warn("=====# : preInstallation delay exception : " + pre.getString("name"));
								}
							}
						}
					}
				}

				/**
				 * Installation Step 따라 설치 진행
				 * Type : ConfigMap, Chart
				 **/
				boolean chartInstalled = false; // InstallationStep에 Chart(Package) Install을 여러번 시도하여도 한번만 Install 함...
				for(String step : installationSteps) {
					/** ConfigMap 설치 **/
					if(StringUtils.equalsIgnoreCase(step, AddonConstants.ADDON_CONFIG_COMMON_INSTALLATION_TYPE_CONFIGMAP)) {
						List<Toml> configmapTomlList = configToml.getTables(AddonConstants.ADDON_CONFIG_CONFIGMAP);
						if(CollectionUtils.isEmpty(configmapTomlList)) {
							continue;
						}

						for(Toml configmap : configmapTomlList) {
							// ConfigMap 설치
							String addonNamespace = configmap.getString(KubeConstants.META_NAMESPACE);
							String configmapName = configmap.getString(KubeConstants.NAME);
							configmapName = addonCommonService.convertDynamicValues(configmapName, dynamicValues);

							cluster.setNamespaceName(addonNamespace);
							V1ConfigMap addonConfigMap = configMapService.getConfigMapV1(cluster, addonNamespace, configmapName);
							/** 이미 Configmap 설정이 존재하면 ConfigMap을 생성하지 않고 다음 처리로 이동 **/
							if(addonConfigMap != null) {
								log.info("Addon configmap already exists: {}", addonBaseNamespace);
								continue;
							}

							ConfigMapGuiVO configMap = new ConfigMapGuiVO();
							configMap.setNamespace(addonNamespace);
							configMap.setName(configmapName);

							/** Data 설정 **/
							Map<String, String> data = AddonUtils.newAddonDataMap("", addonBaseNamespace);

							/** value.yaml 편집 : addon.toml 내용과 병합하여 설치함.**/
							String valuesYaml = addonCommonService.generateValuesYaml(addonInstall, cluster, dynamicValues, CRUDCommand.C);

							/**
							 * API Version V3 는 yaml 파일을 이용한 배포 기능 추가
							 * - 위에서 addon.toml과 병합되어 생성된 value.yaml 내용에 사용자가 입력한 yaml 데이터를 덮어씌움
							 **/
//							if(apiVersionType == ApiVersionType.V3 && StringUtils.isNotBlank(addonInstall.getAddonYaml())) {
							if(StringUtils.isNotBlank(addonInstall.getAddonYaml())) {
								valuesYaml = k8sPatchSpecFactory.mergeYamlToString(valuesYaml, addonInstall.getAddonYaml());
								data.put(AddonKeyItem.ADDON_YAML.getValue(), addonInstall.getAddonYaml());
							}

							data.put(AddonKeyItem.VALUE_YAML.getValue(), valuesYaml);
							Toml configmapDatas = configmap.getTable(AddonConstants.ADDON_CONFIG_CONFIGMAP_DATA);
                            addonCommonService.setMapData(data, configmapDatas, dynamicValues);

							/** Data에 Toml 배포 정보 추가 **/
							if(StringUtils.isNotBlank(addonInstall.getAddonToml())) {
								data.put(AddonKeyItem.ADDON_TOML.getValue(), Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(addonInstall.getAddonToml()))));
							}
							configMap.setData(data);

							/** Label 설정 **/
							Map<String, String> additionalLabels = new HashMap<>();
							Toml configmapLabels = configmap.getTable(AddonConstants.ADDON_CONFIG_CONFIGMAP_LABELS);
                            addonCommonService.setMapData(additionalLabels, configmapLabels, dynamicValues);

							configMapService.createConfigMap(cluster.getClusterSeq(), addonNamespace, configMap, additionalLabels);
							configMapInstallationInfo.add(configMap);
						}
					}

					/** Chart(Package) 설치 **/
					if(StringUtils.equalsIgnoreCase(step, AddonConstants.ADDON_CONFIG_COMMON_INSTALLATION_TYPE_CHART) && !chartInstalled) {
						HelmReleaseBaseVO release = this.installAddonChart(cluster, chartInfo, addonBaseNamespace, releaseName);
						packageInstallationInfo.add(release);
						chartInstalled = true;

					}
				}

				// TODO : Post Installation
				if(CollectionUtils.isNotEmpty(postInstallationList)) {
					for(Toml post : postInstallationList) {
						// Install
						log.debug("=====# : postInstallation : " + post.getString("name"));
					}
				}
			}

		}
		catch (Exception e) {
			if(log.isDebugEnabled()) log.debug("trace log ", e);
			if(clusterDao == null) {
				clusterDao = sqlSession.getMapper(IClusterMapper.class);
			}
			/** 오류 발생시 생성한 모든 Addon에 대해 Rollback 처리 **/
			if (CollectionUtils.isNotEmpty(configMapInstallationInfo)) {
				for (ConfigMapGuiVO configmap : configMapInstallationInfo) {
					ClusterVO addCluster = clusterDao.getCluster(cluster.getClusterSeq());
					addCluster.setNamespaceName(configmap.getNamespace());

					ConfigMapGuiVO c1 = configMapService.getConfigMap(cluster.getClusterSeq(), configmap.getNamespace(), configmap.getName());
					if(c1 != null) {
						configMapService.deleteConfigMap(addCluster, configmap.getNamespace(), configmap.getName());
						log.info(String.format("Rollback the addon installation - configmap name : %s", configmap.getName()), e);
					}
					else {
						log.info(String.format("Rollback the addon installation - configmap not found : %s", configmap.getName()), e);
					}
				}
			}
			if (CollectionUtils.isNotEmpty(packageInstallationInfo)) {
				for (HelmReleaseBaseVO release : packageInstallationInfo) {
					ClusterVO addCluster = clusterDao.getCluster(cluster.getClusterSeq());
					addCluster.setNamespaceName(release.getNamespace());

					if(packageValidService.isUsingReleaseName(cluster.getClusterSeq(), release.getNamespace(), release.getName(), false)) {
						packageCommonService.unInstallPackage(cluster.getClusterSeq(), release.getNamespace(), release.getName());
						log.info(String.format("Rollback the addon installation - addon name : %s", release.getName()), e);
					}
					else {
						log.info(String.format("Rollback the addon installation - addon not found : %s", release.getName()), e);
					}
				}
			}
			if(e instanceof IllegalStateException) {
				throw new CocktailException("addon installation is canceled.. because configuration file format invalid..", e, ExceptionType.ClusterRegistrationFail);
			}
			if(e instanceof CocktailException) {
				throw e;
			}
			if(e instanceof ParserException) {
				throw new CocktailException("Invalid Yaml Format", e, ExceptionType.InvalidYamlData, "Invalid Yaml Format");
			}
			else {
				throw new CocktailException("addon installation is canceled..", e, ExceptionType.CommonCreateFail, "addon installation is canceled..");
			}
		}

		return configMapInstallationInfo;
	}

    /**
     * Dynamic Value Generator
     * @param dynamicValues
     * @param releaseName
     * @param fixedReleaseName
     * @param chartInfo
     * @param cluster
     * @return
     * @throws Exception
     */
    private Map<String, String> setUpDynamicValues(Map<String, String> dynamicValues, String releaseName, String fixedReleaseName, ChartInfoBaseVO chartInfo, ClusterVO cluster) throws Exception {
        if(dynamicValues == null) {
            dynamicValues = new HashMap<>();
        }

        dynamicValues.put(AddonDynamicValueType.SYSTEM_RESOURCE_PREFIX.getValue(), ResourceUtil.getResourcePrefix());
        dynamicValues.put(AddonDynamicValueType.PACKAGE_CHART_NAME.getValue(), chartInfo.getName());
        dynamicValues.put(AddonDynamicValueType.PACKAGE_CHART_VERSION.getValue(), chartInfo.getVersion());
        dynamicValues.put(AddonDynamicValueType.PACKAGE_RELEASE_NAME.getValue(), releaseName);
        dynamicValues.put(AddonDynamicValueType.PACKAGE_FIXED_RELEASE_NAME.getValue(), fixedReleaseName);
        dynamicValues.put(AddonDynamicValueType.SYSTEM_CLUSTER_ID.getValue(), cluster.getClusterId());
        dynamicValues.put(AddonDynamicValueType.SYSTEM_CLUSTER_SEQ.getValue(), cluster.getClusterSeq().toString());
        dynamicValues.put(AddonDynamicValueType.PACKAGE_APP_VERSION.getValue(), chartInfo.getAppVersion());
        dynamicValues.put(AddonDynamicValueType.SYSTEM_CHART_REPO_URL.getValue(), cocktailAddonProperties.getAddonChartRepoUrl());
        dynamicValues.put(AddonDynamicValueType.SYSTEM_CHART_REPO_PROJECT_NAME.getValue(), cocktailAddonProperties.getAddonChartRepoProjectName());
        dynamicValues.put(AddonDynamicValueType.SYSTEM_CHART_REPO_USER.getValue(), cocktailAddonProperties.getAddonChartRepoUser());
        dynamicValues.put(AddonDynamicValueType.SYSTEM_CHART_REPO_PASSWORD.getValue(), cocktailAddonProperties.getAddonChartRepoPassword());
	    dynamicValues.put(AddonDynamicValueType.SYSTEM_CHART_REPO_CERT.getValue(), cocktailRegistryProperties.getPrivateCertificate());
        dynamicValues.put(AddonDynamicValueType.SYSTEM_IMAGE_PULL_POLICY_ALWAYS.getValue(), ImagePullPolicyType.Always.getCode());
        dynamicValues.put(AddonDynamicValueType.SYSTEM_IMAGE_BASE_URL.getValue(), cocktailAddonProperties.getAddonImageBaseUrl());
        dynamicValues.put(AddonDynamicValueType.SYSTEM_CLUSTER_TYPE.getValue(), cluster.getCubeType().isKaas() ? "kaas" : "small");
        dynamicValues.put(AddonDynamicValueType.SYSTEM_BASE64_CLUSTER_ID.getValue(), Base64Utils.encodeToString(cluster.getClusterId().getBytes(StandardCharsets.UTF_8)));
        dynamicValues.put(AddonDynamicValueType.SYSTEM_BASE64_CLUSTER_SEQ.getValue(), Base64Utils.encodeToString(String.valueOf(cluster.getClusterSeq().intValue()).getBytes(StandardCharsets.UTF_8)));
	    String agentReleaseName = String.format("%s-%s-%d", cocktailAddonProperties.getMonitoringAgentConfigmapPrefix(), ResourceUtil.getResourcePrefix(), cluster.getClusterSeq());
	    dynamicValues.put(AddonDynamicValueType.PACKAGE_AGENT_RELEASE_NAME.getValue(), agentReleaseName);

        /* 아래 dynamicValues들은 Monitoring Aget 설치일때만  설정함. */
        if(StringUtils.equalsIgnoreCase(chartInfo.getName(), "monitoring-agent")) {
            String authKey = null;
            try {
                authKey = clusterAccessAuthService.createClusterAccessSecret(cluster.getClusterSeq(), ClusterAddonType.MONITORING.getCode());
            }
            catch (Exception e) {
                if (e instanceof CocktailException) {
                    if (((CocktailException) e).getType() == ExceptionType.ClusterAccessKeyAlreadyExists) {
                        ClusterAccessAuthVO clusterAccessAuth = clusterAccessAuthService.getClusterAccessSecret(cluster.getClusterSeq(), ClusterAddonType.MONITORING.getCode());
                        authKey = clusterAccessAuth.getAuthKey();
                    }
                    else {
                        throw e;
                    }
                }
                else {
                    throw e;
                }
            }

            dynamicValues.put(AddonDynamicValueType.SYSTEM_PROMETHEUS_URL.getValue(), AddonUtils.getAutoAddonPrometheusUrl("monitoring"));
            dynamicValues.put(AddonDynamicValueType.SYSTEM_ALERT_MANAGER_URL.getValue(), AddonUtils.getAutoAddonAlertmanagerUrl("monitoring"));
            dynamicValues.put(AddonDynamicValueType.SYSTEM_COLLECTOR_SERVER_URL.getValue(), cocktailAddonProperties.getMonitoringCollectorUrlProxy());
            dynamicValues.put(AddonDynamicValueType.SYSTEM_MONITOR_API_URL.getValue(), cocktailAddonProperties.getMonitoringApiUrlProxy());
            dynamicValues.put(AddonDynamicValueType.SYSTEM_BASE64_MONITORING_SECRET.getValue(), Base64Utils.encodeToString(authKey.getBytes(StandardCharsets.UTF_8)));
        }

        return dynamicValues;
    }


	/**
	 * Install Addon Chart
	 * @param cluster
	 * @param chartInfo
	 * @param namespaceName
	 * @param releaseName
	 * @return
	 * @throws Exception
	 */
    private HelmReleaseBaseVO installAddonChart(ClusterVO cluster, ChartInfoBaseVO chartInfo, String namespaceName, String releaseName) throws Exception {
		return this.installAndUpgradeAddonChart(cluster, chartInfo, namespaceName, releaseName, null, null, false);
	}


	/**
	 * Upgrade Addon Chart
	 * @param cluster
	 * @param chartInfo
	 * @param namespaceName
	 * @param releaseName
	 * @return
	 * @throws Exception
	 */
	private HelmReleaseBaseVO upgradeAddonChart(ClusterVO cluster, ChartInfoBaseVO chartInfo, String namespaceName, String releaseName) throws Exception {
		return this.installAndUpgradeAddonChart(cluster, chartInfo, namespaceName, releaseName, null, null, true);
	}

	/**
	 * Install & Upgrade Addon Package
	 * - 2020.06.30 : R4.4.0 : 먼저 설치된 ConfigMap 의 values.yaml이 있으면 해당 Yaml을 이용하여 배포하도록 수정함..
	 * @param cluster
	 * @throws Exception
	 */
	private HelmReleaseBaseVO installAndUpgradeAddonChart(ClusterVO cluster, ChartInfoBaseVO chartInfo, String namespaceName, String releaseName, String chartName, String chartVersion, boolean isUpgrade) throws Exception {
		try {
			/**
			 * Chart 조회
			 */
			if(chartInfo == null) {
				if(StringUtils.isBlank(chartName) || StringUtils.isBlank(chartVersion)) {
					throw new CocktailException("addon installation failure!! (chart information is null)", ExceptionType.ClusterRegistrationFail);
				}
				chartInfo = packageCommonService.getChart(cocktailAddonProperties.getAddonChartRepoProjectName(), chartName, chartVersion);
			}

			/**
			 * Yaml에 값 설정..
			 */
			Map<String, Object> valuesMap = Yaml.getSnakeYaml().load(chartInfo.getValues());
			/**
			 * TODO : 현재는 Addon Manager만을 위해 동작하도록 간단히 구성되어 있음.
			 * 	- Addon을 Configmap이 아닌 Chart로 배포하는 시점에는 values 파일과 입력받은 데이터 Merge 등 추가기능 개발이 필요함.
			 **/
			chartInfo.setValues(Yaml.getSnakeYaml().dump(valuesMap));

			/**
			 * 2020.06.30 : R4.4.0 : 배포된 Configmap이 있으면 해당 Configmap의 values.yaml을 이용하여 배포하도록 함.
			 * - 현재 Addon-manager만 해당...
			 */
			cluster.setNamespaceName(namespaceName);
			ConfigMapGuiVO currAddon = configMapService.getConfigMap(cluster.getClusterSeq(), cluster.getNamespaceName(), releaseName);
			if(currAddon != null
				&& MapUtils.isNotEmpty(currAddon.getData())
				&& StringUtils.isNotBlank(currAddon.getData().get(AddonKeyItem.VALUE_YAML.getValue()))
			) {
				chartInfo.setValues(currAddon.getData().get(AddonKeyItem.VALUE_YAML.getValue()));
			}

			/**
			 * Package 배포 정보 구성 및 배포.
			 */
			HelmInstallRequestVO helmInstallRequest = new HelmInstallRequestVO();
			helmInstallRequest.setRepo(cocktailAddonProperties.getAddonChartRepoProjectName());
			helmInstallRequest.setChartName(chartInfo.getName());
			helmInstallRequest.setVersion(chartInfo.getVersion());
			helmInstallRequest.setReleaseName(releaseName);
			helmInstallRequest.setValues(chartInfo.getValues());
			helmInstallRequest.setLaunchType("A");
			helmInstallRequest.setNamespace(namespaceName);

			if(isUpgrade) {
				return packageCommonService.upgradePackage(cluster.getClusterSeq(), helmInstallRequest.getNamespace(), helmInstallRequest.getReleaseName(), helmInstallRequest);
			}
			else {
				return packageCommonService.installPackage(cluster.getClusterSeq(), helmInstallRequest.getNamespace(), helmInstallRequest, ContextHolder.exeContext());
			}
		} catch (CocktailException ce) {
			if(ce.getType() == ExceptionType.PackageNameAlreadyExists) {
				/** 중복된 패키지면 설치하지 않음... Log 남기고 정상 종료... **/
				log.info(String.format("Addon-manager package already exists: %s", namespaceName), ce);
			}
			else {
				try {
					// 오류 발생시 Package Uninstall... (사용자 권한이 불충분하여 리소스 생성 실패하는 경우가 있음 : clusterrolebinding 생성 오류등...) **/
					packageCommonService.unInstallPackage(cluster.getClusterSeq(), namespaceName, cocktailAddonProperties.getAddonManagerConfigmapPrefix());
				}
				catch (Exception ex) {
					// uninstall 오류는 무시한다...
				}
				log.info("Rollback the cluster installation - addon uninstalled");
				throw ce;
			}
		} catch (Exception e) {
			try {
				// 오류 발생시 Package Uninstall... (사용자 권한이 불충분하여 리소스 생성 실패하는 경우가 있음 : clusterrolebinding 생성 오류등...) **/
				packageCommonService.unInstallPackage(cluster.getClusterSeq(), namespaceName, cocktailAddonProperties.getAddonManagerConfigmapPrefix());
			}
			catch (Exception ex) {
				// uninstall 오류는 무시한다...
			}
			log.info("Rollback the cluster installation - addon uninstalled");
			throw new CocktailException("addon installation failure!!", e, ExceptionType.ClusterRegistrationFail);
		}

		return null;
	}

	/**
	 * 공통 Addon Upgrade
	 * @param clusterSeq
	 * @param addonUpgrade
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> upgradeAddon(Integer clusterSeq, AddonInstallVO addonUpgrade) throws Exception {
		return this.upgradeAddon(clusterSeq, addonUpgrade, "v2");
	}

	/**
	 * 공통 Addon Upgrade
	 * @param clusterSeq
	 * @param addonUpgrade
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> upgradeAddon(Integer clusterSeq, AddonInstallVO addonUpgrade, String apiVersion) throws Exception {
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getCluster(clusterSeq);

		switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
			case V2:
				return this.upgradeAddon(cluster, addonUpgrade, ApiVersionType.V2);
			case V3:
				return this.upgradeAddon(cluster, addonUpgrade, ApiVersionType.V3);
			default:
				throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
		}
	}

	/**
	 * 공통 Addon Upgrade 처리
	 * @param cluster
	 * @param addonUpgrade
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> upgradeAddon(ClusterVO cluster, AddonInstallVO addonUpgrade, ApiVersionType apiVersionType) throws Exception {
		/**
		 * 2020.10.13 : 이전버전으로 재배포 시 Version이 달라질 수 있으므로,
		 * 요청된 Chart Version에 해당하는 defaultValue와 configToml을 조회하여 재설정 해 주도록 함 */
		ChartInfoBaseVO chartDetail = packageCommonService.getChart(addonUpgrade.getRepo(), addonUpgrade.getName(), addonUpgrade.getVersion());
		if(chartDetail != null) { // 값이 있으면 덮어씀...
			if (!StringUtils.isBlank(chartDetail.getConfigToml())) {
				addonUpgrade.setConfigToml(chartDetail.getConfigToml());
			}
			if (!StringUtils.isBlank(chartDetail.getDefaultValueYaml())) {
				addonUpgrade.setDefaultValueYaml(chartDetail.getDefaultValueYaml());
			}
			if (!StringUtils.isBlank(chartDetail.getAppVersion())) {
				addonUpgrade.setAppVersion(chartDetail.getAppVersion());
			}
		}
		/** --- **/

		cluster.setNamespaceName(this.getCocktailAddonNamespace());
		// 자동 생성되는 releaseName인 케이스일때는 생성된 이름으로 Toml의 값을 변경 해줌...
		// Addon 생성시에 이미 처리하고 있어 처리할 필요가 없음.. (2020.04.27)
		// 본 케이스는 Addon 생성에서 본 처리를 구현하기전 (2020.04.27 이전 배포)에 대해 수정이 필요하여 넣음...
		if(StringUtils.equalsIgnoreCase(addonUpgrade.getReleaseName(), "The_system_creates_it_automatically")) {
			if (StringUtils.equalsIgnoreCase(addonUpgrade.getName(), "monitoring-agent")) {
				addonUpgrade.setReleaseName(String.format("%s-%s-%d", cocktailAddonProperties.getMonitoringAgentConfigmapPrefix(), ResourceUtil.getResourcePrefix(), cluster.getClusterSeq()));
			}
			addonUpgrade.setAddonToml(StringUtils.replace(addonUpgrade.getAddonToml(), "The_system_creates_it_automatically", addonUpgrade.getReleaseName()));
		}

		ConfigMapGuiVO currAddon = configMapService.getConfigMap(cluster.getClusterSeq(), cluster.getNamespaceName(), addonUpgrade.getReleaseName());

		//TODO : Front에서 넘어오는 Toml Format 오류 Patch.. 향후 보완 필요.
		addonUpgrade.setAddonToml(StringUtils.replace(addonUpgrade.getAddonToml(), "\"undefined\"", "undefined"));
		addonUpgrade.setAddonToml(StringUtils.replace(addonUpgrade.getAddonToml(), "undefined", "\"undefined\""));

		/** Read configToml **/
		Toml upgradeConfigToml = new Toml().read(addonUpgrade.getConfigToml());
		Toml common = upgradeConfigToml.getTable(AddonConstants.ADDON_CONFIG_COMMON);
		Toml dependency = upgradeConfigToml.getTable(AddonConstants.ADDON_CONFIG_DEPENDENCY);

		/** Dependency를 판단하여 pre/post 설치가 가능할 수 있도록 설정 **/
		List<Toml> preInstallationList = null;
		List<Toml> postInstallationList = null;
		if(dependency != null) {
			preInstallationList = dependency.getTables(AddonConstants.ADDON_CONFIG_DEPENDENCY_PREINSTALLATION);
			postInstallationList = dependency.getTables(AddonConstants.ADDON_CONFIG_DEPENDENCY_POSTINSTALLATION);
		}

		/** Install Step **/
		List<String> installationSteps = common.getList(AddonConstants.ADDON_CONFIG_COMMON_INSTALLATION_STEP);

		String lbType = null;
		String releaseNamespace = null;
		if (currAddon != null) { // 현재 설치된 Addon이 있어야 Upgrade가 가능함.
			if(CollectionUtils.isNotEmpty(installationSteps)) {
				// TODO : Pre Installation
				if(CollectionUtils.isNotEmpty(preInstallationList)) { // preInstallationList..
					for (Toml pre : preInstallationList) {
						// Install
						log.debug("=====# : preInstallation : " + pre.getString("name"));
					}
				}

				/**
				 * Installation Step 따라 Upgrade 진행
				 * Type : ConfigMap, Chart
				 **/
				boolean chartInstalled = false; // InstallationStep에 Chart(Package) Install을 여러번 시도하여도 한번만 Install 함...
				for(String step : installationSteps) {
					/** ConfigMap 설치 **/
					if (StringUtils.equalsIgnoreCase(step, AddonConstants.ADDON_CONFIG_COMMON_INSTALLATION_TYPE_CONFIGMAP)) {
						List<JsonObject> patchBody = new ArrayList<>();

						ChartInfoBaseVO chartInfo = new ChartInfoBaseVO();
						BeanUtils.copyProperties(addonUpgrade, chartInfo);
						Map<String, String> dynamicValues = this.setUpDynamicValues(null, addonUpgrade.getReleaseName(), addonUpgrade.getReleaseName(), chartInfo, cluster);

						/** Specific case : Istio Upgrade 요청시 Istio-init 설치가 되어있는지 확인한다. **/ /** 2021.04.07 Istio Specific case 제거 **/
						//TODO : To be deleted
						if (false && StringUtils.equalsIgnoreCase(addonUpgrade.getName(), "istio")) {
							/** istio addon일 경우 인증서 추가 작업 필요...**/
							// TODO : addon.toml에서 처리하려 하였으나 애매한 케이스라 우선 소스코드에 구현.... (생성 / 수정 공통 사용 => 모듈화)
							Toml addonToml = new Toml().read(addonUpgrade.getAddonToml());
							Toml configToml = new Toml().read(chartInfo.getConfigToml());
							lbType = addonToml.getString(AddonConstants.ADDON_VALUE_DETAIL + ".gateways_istio-ingressgateway_type.value");
							String address = addonToml.getString(AddonConstants.ADDON_VALUE_DETAIL + ".gateways_istio-ingressgateway_ip.value");
							String domain = null;
							String ipAddress = null;
							if (KubeConstants.SPEC_TYPE_VALUE_NODE_PORT.equals(lbType) && StringUtils.isBlank(address)) {
								throw new CocktailException("Ingress Gateway is required.", ExceptionType.InvalidParameter_Empty, "Ingress Gateway is required.");
							}
							if (Utils.checkIPv4Address(address)) {
								ipAddress = address;
							}
							else {
								domain = address;
							}
							Long portNumber = addonToml.getLong(AddonConstants.ADDON_VALUE_DETAIL + ".gateways_istio-ingressgateway_ports_httpsNodePort.value");

							/** Create Certificate **/
							X500PrivateCredential serverCred = null;
							/** 이미 Ca 인증서가 있으면 기존 인증서를 사용하여 Leaf 인증서를 생성한다.. (dashboard에서 Kiali 접속시 Cache된 인증서를 계속 사용할 수 있도록 하기 위함...) **/
							serverCred = addonCommonService.getServerCredFromExistInfo(currAddon);
							/** 인증서 생성 및 DynamicValues에 입력 **/
							dynamicValues = addonCommonService.setCertificateToDynamicValue(serverCred, dynamicValues, domain, ipAddress);

							if (StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_NODE_PORT, lbType)) {
								dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_ADDRESS_LIST.getValue(), address + ":" + portNumber.toString()); //101.55.69.106
							}
							else if (StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_LOADBALANCER, lbType)) {
								dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_ADDRESS_LIST.getValue(), cluster.getNodePortUrl() + ":" + portNumber.toString());

								List<Toml> configmapTomlList = configToml.getTables(AddonConstants.ADDON_CONFIG_CONFIGMAP);
								if (CollectionUtils.isNotEmpty(configmapTomlList)) {
									for (Toml configmap : configmapTomlList) {
										/** istio configmap을 찾아서 releaseNamespace를 설정 **/
										Toml configmapLabels = configmap.getTable(AddonConstants.ADDON_CONFIG_CONFIGMAP_LABELS);
										Toml configmapDatas = configmap.getTable(AddonConstants.ADDON_CONFIG_CONFIGMAP_DATA);

										String labelChartName = configmapLabels.getString(KubeConstants.LABELS_ADDON_CHART_KEY);
										labelChartName = addonCommonService.convertDynamicValues(labelChartName, dynamicValues);
										if (StringUtils.equalsIgnoreCase(labelChartName, "istio")) {
											releaseNamespace = configmapDatas.getString(KubeConstants.ADDON_NAMESPACE_KEY);
											releaseNamespace = StringUtils.isNotBlank(releaseNamespace) ? releaseNamespace : cluster.getNamespaceName();
											/** Namespace가 존재하지 않으면 생성 **/
											this.createNamespaceIfNotExist(cluster, releaseNamespace);
											break;
										}
									}
								}
							}
						}
						/** 2020.09.08 : Specific case : Monitoring Addon일 경우 Addon.toml 데이터에서 Etcd 인증서 정보 입력이 없으면, 기존 정보를 사용하는 것으로 판단 하여 처리. **/
						else if(StringUtils.equalsIgnoreCase(Optional.ofNullable(currAddon.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_CHART_KEY), "prometheus-operator")) {
							// current Toml Data가 존재하면 처리..
							String currToml = Optional.ofNullable(currAddon.getData()).orElseGet(() ->Maps.newHashMap()).get(AddonKeyItem.ADDON_TOML.getValue());
							currToml = this.validCurrToml(currToml);

							/** 검증결과 currToml이 유효할 경우에만 인증서 정보를 교체할 수 있음 **/
							if(StringUtils.isNotBlank(currToml)) {
								/** 인증서 입력이 없으면 이전 (source) 인증서 정보로 Replace 처리... **/
								String mergedTomlData = this.mergeMonitoringTomlData(addonUpgrade.getAddonToml(), currToml);
								addonUpgrade.setAddonToml(mergedTomlData);
							}
						}
						/** 2020.09.08 : Specific case : istiod Addon일 경우 Addon.toml 데이터에서 kiali url, username, password, 인증서 정보 입력이 없으면, 기존 정보를 사용하는 것으로 판단 하여 처리. **/
						else if(StringUtils.equalsIgnoreCase(Optional.ofNullable(currAddon.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_CHART_KEY), "istiod")) {
							// current Toml Data가 존재하면 처리..
							String currToml = Optional.ofNullable(currAddon.getData()).orElseGet(() ->Maps.newHashMap()).get(AddonKeyItem.ADDON_TOML.getValue());
							currToml = this.validCurrToml(currToml);

							/** 검증결과 currToml이 유효할 경우에만 정보를 교체할 수 있음 **/
							if(StringUtils.isNotBlank(currToml)) {
								/** 인증서 입력이 없으면 이전 (source) 정보로 Replace 처리... **/
								String mergedTomlData = this.mergeIstioTomlData(addonUpgrade.getAddonToml(), currToml);
								addonUpgrade.setAddonToml(mergedTomlData);
							}
						}

						/** 01. generate Values.yaml **/
						String valuesYaml = addonCommonService.generateValuesYaml(addonUpgrade, cluster, dynamicValues, CRUDCommand.U);
						/**
						 * API Version V3 는 yaml 파일을 이용한 배포 기능 추가
						 * - 위에서 addon.toml과 병합되어 생성된 value.yaml 내용에 사용자가 입력한 yaml 데이터를 덮어씌움
						 **/
//						if(apiVersionType == ApiVersionType.V3 && StringUtils.isNotBlank(addonUpgrade.getAddonYaml())) {
						if(StringUtils.isNotBlank(addonUpgrade.getAddonYaml())) {
							valuesYaml = k8sPatchSpecFactory.mergeYamlToString(valuesYaml, addonUpgrade.getAddonYaml());
						}

						/** 02. generate Patch Body **/
						patchBody = addonCommonService.generateAddonPatchData(patchBody, addonUpgrade, currAddon, dynamicValues, valuesYaml);

						/** 03. appVersion이 다르면 PatchBody 구성 **/
						if(!StringUtils.equalsIgnoreCase(addonUpgrade.getAppVersion(), currAddon.getData().get(AddonKeyItem.APP_VERSION.getValue()))) {
							AddonUtils.setAddonAppVersion(patchBody, addonUpgrade.getAppVersion());
						}

						/** 04. Addon Status 및 Update 시간 수정 **/
						if (CollectionUtils.isNotEmpty(patchBody)) {
							AddonUtils.setAddonStatus(patchBody, KubeConstants.ADDON_STATUS_PENDING_UPGRADE);
							AddonUtils.setAddonUpdateAt(patchBody);
							configMapService.patchConfigMap(cluster.getClusterSeq(), this.getCocktailAddonNamespace(), currAddon.getName(), patchBody);
						}

					} // if step is configmap

					/** Chart Type일 경우 직접 Chart Upgrade를 진행 **/
					if(StringUtils.equalsIgnoreCase(step, AddonConstants.ADDON_CONFIG_COMMON_INSTALLATION_TYPE_CHART) && !chartInstalled) {
						List<Toml> configmapTomlList = upgradeConfigToml.getTables(AddonConstants.ADDON_CONFIG_CONFIGMAP);
						if (CollectionUtils.isNotEmpty(configmapTomlList)) {
							for (Toml configmap : configmapTomlList) {
								Toml configmapDatas = configmap.getTable(AddonConstants.ADDON_CONFIG_CONFIGMAP_DATA);
								releaseNamespace = configmapDatas.getString(KubeConstants.ADDON_NAMESPACE_KEY);
								releaseNamespace = StringUtils.isNotBlank(releaseNamespace) ? releaseNamespace : cluster.getNamespaceName();
								/** Namespace가 존재하지 않으면 생성 **/
								this.createNamespaceIfNotExist(cluster, releaseNamespace);
								break;
							}
						}

						ChartInfoBaseVO chartInfo = packageCommonService.getChart(addonUpgrade.getRepo(), addonUpgrade.getName(), addonUpgrade.getVersion());
						HelmReleaseBaseVO release = this.upgradeAddonChart(cluster, chartInfo, releaseNamespace, addonUpgrade.getReleaseName());
						chartInstalled = true;
					}

				} // for(String step : installationSteps) {

				// TODO : Post Installation
				if(CollectionUtils.isNotEmpty(postInstallationList)) {
					for(Toml post : postInstallationList) { // postInstallationList
						// Install
						log.debug("=====# : postInstallation : " + post.getString("name"));
					}
				}

			} // if(CollectionUtils.isNotEmpty(installationSteps)) {

		} // currAddon is not null
		else {
			throw new CocktailException("addon configMap does not exist !!", ExceptionType.K8sConfigMapNotFound);
		}

		currAddon = configMapService.getConfigMap(cluster.getClusterSeq(), cluster.getNamespaceName(), addonUpgrade.getReleaseName());
		List<ConfigMapGuiVO> cms = new ArrayList<>();
		cms.add(currAddon);

		return cms;
	}

	private String validCurrToml(String currToml) throws Exception {
		if(StringUtils.isNotBlank(currToml)) {
			// Base64 Encode 되어 있으면 decode해서 넣음.
			if(Utils.isBase64Encoded(currToml)) {
				String decodedTomlString = new String(Base64Utils.decodeFromString(currToml), StandardCharsets.UTF_8);
				if(this.isValidToml(decodedTomlString)) {
					currToml = decodedTomlString;
				}
				else {
					currToml = null;
				}
			}
			// Base64 Encode되지 않은 문자열..
			else {
				// invalid하면 null
				if(!this.isValidToml(currToml)) {
					currToml = null;
				}
			}
		}

		return currToml;
	}

	/**
	 * 클러스터에 설치된 전체 Addon을 제거한다...
	 * 2022.07.29 : 클러스터 제거시, Addon 전체가 아닌 Monitoring Agent만 제거하도록 함.
	 * @param cluster 클러스터 정보
	 * @throws Exception
	 */
	@Deprecated
	public void removeAllAddons(ClusterVO cluster) throws Exception {
		/** ===========================================================================================================
		 * 2020.05.08 : Addon을 제거할지 여부 판단 로직 추가.
		 * - 여러 Cocktail에서 사용중일 경우가 있음
		 * - 다른 Cocktail에서도 사용중이면 개별 Cocktail에 종속적인 Addon만 제거
		 *   1. 개별 Cocktail에 종속성을 갖는 Addon은 labels.cocktail key가 존재하며, value는 Cocktail별로 Unique한 ResourcePrefix값을 가짐
		 *   2. 아래의 this.getAddonConfigMaps(cluster.getClusterSeq(), label)로 조회시 다른 cocktail의 Addon은 미리 걸러지고 조회됨
		 *   3. 따라서 lebels.cocktail key가 존재하면 제거하면 됨
		 * - 현재 Cocktail에서만 사용중이면 전체 Addon 제거.
		 **/
		String addonNamespace = this.getCocktailAddonNamespace();
		String addonManagerName = cocktailAddonProperties.getAddonManagerConfigmapPrefix();

		boolean isLast = false; // 현재 Cocktail에서만 사용중인지 판단... default false
		ConfigMapGuiVO configMap = this.getAddonConfigMap(cluster, addonManagerName, true);
		if (configMap != null) {
			String registeredCocktails = Optional.ofNullable(configMap.getData()).orElseGet(() ->Maps.newHashMap()).get(AddonConstants.ADDON_DATA_REGISTERED_COCKTAILS);
			if (StringUtils.isNotBlank(registeredCocktails)) {
				try {
					List<String> cocktails = JsonUtils.fromGson(Optional.ofNullable(configMap.getData()).orElseGet(() ->Maps.newHashMap()).get(AddonConstants.ADDON_DATA_REGISTERED_COCKTAILS), new TypeToken<List<String>>(){}.getType());
					boolean foundCurrentCocktail = false;
					if(CollectionUtils.isNotEmpty(cocktails)) {
						for (String key : cocktails) {
							if (StringUtils.equals(key, ResourceUtil.getResourcePrefix())) {
								foundCurrentCocktail = true;
								break;
							}
						}

						if(foundCurrentCocktail) {
							if(cocktails.size() <= 1) {
								// 현재 Cocktail을 찾았고, List가 1개면 => 현재 Cocktail에서만 사용중으로 판단...
								isLast = true;
							}
							else {
								// 현재 Cocktail외에 다른 Cocktail에서도 사용중 => AddonManager의 registeredCocktail 값을 Update (현재 Cocktail 제거)
								cocktails.remove(ResourceUtil.getResourcePrefix());

								// patchBody 구성
								List<JsonObject> patchBody = new ArrayList<>();
								Map<String, Object> registeredCocktailMap = new HashMap<>();
								registeredCocktailMap.put("path", String.format("/data/%s", AddonConstants.ADDON_DATA_REGISTERED_COCKTAILS));
								registeredCocktailMap.put("op", JsonPatchOp.REPLACE.getValue());
								registeredCocktailMap.put("value", JsonUtils.toGson(cocktails));
								patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(registeredCocktailMap), JsonElement.class)).getAsJsonObject());

								// 현재 Cocktail을 제거하여 Update.
								configMapService.patchConfigMap(cluster.getClusterSeq(), this.getCocktailAddonNamespace(), addonManagerName, patchBody);
								isLast = false; // 현재 Cocktail을 AddonManager Configmap에서 제거 => 개별 Cocktail에 종속적 Addon만 제거
							}
						}
						else {
							isLast = false; // 현재 Cocktail을 찾지 못했다면 = 다른 Cocktail에서도 사용중 = 개별 Cocktail에 종속적 Addon만 제거
						}
					}
					else {
						// 비어 있다면 => 현재 Cocktail에서만 사용중으로 판단.
						isLast = true;
					}
				}
				catch (Exception e) {
					isLast = false; // 변환중 오류 => 알 수 없는 오류... 우선 다른 Cocktail에서도 사용중으로 설정. (개별 Cocktail에 종속적 Addon만 제거)
				}
			}
			else {
				// 비어 있다면 => 현재 Cocktail에서만 사용중으로 판단.
				isLast = true;
			}
		}
		/** =========================================================================================================== **/

		String label = String.format("%s!=%s", KubeConstants.LABELS_ADDON_CHART_KEY, KubeConstants.LABELS_ADDON_CHART_VALUE_COCKTAIL);
		List<ConfigMapGuiVO> addons = addonCommonService.getAddonConfigMaps(cluster.getClusterSeq(), label);

		/** 현재 Cocktail에서만 사용중인 클러스터 라면 전체 Addon 제거 **/
		if(isLast) {
			/** Addon Manager 외에 모든 Addon 제거 **/
			for (ConfigMapGuiVO addonRow : addons) {
				String releaseName = Optional.ofNullable(addonRow.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_HELM_CHART_KEY);
				/** Addon-manager가 아니면 모두 제거하자. **/
				if (!StringUtils.equalsIgnoreCase(cocktailAddonProperties.getAddonManagerConfigmapPrefix(), releaseName)) {
					this.deleteAddonV2(cluster.getClusterSeq(), releaseName);
					log.info("##### Addon removal ended successfully. : " + releaseName);
				}
			}

			/** Addon Manager 제거 **/
			this.deleteAddonManager(cluster, addonNamespace, addonManagerName);
		}
		/** 현재 Cocktail외에도 사용중인 클러스터가 있다면 : Cocktail 종속적인 Addon만 제거 **/
		else {
			for (ConfigMapGuiVO addonRow : addons) {
				Map<String, String> labels = Optional.ofNullable(addonRow.getLabels()).orElseGet(() ->Maps.newHashMap());
				String releaseName = labels.get(KubeConstants.LABELS_HELM_CHART_KEY);
				/** Cocktail 종속적 Addon 판단 **/
				if (labels.containsKey(KubeConstants.LABELS_COCKTAIL_KEY)) {
					// 현재 Cocktail에서 생성한 Addon일 경우만 제거.
					if(StringUtils.equals(labels.get(KubeConstants.LABELS_COCKTAIL_KEY), ResourceUtil.getResourcePrefix())) {
						this.deleteAddonV2(cluster.getClusterSeq(), releaseName);
						log.info("##### Addon removal ended successfully. : " + releaseName);
					}
				}
			}
		}
	}

	/**
	 * Addon Manager 삭
	 * @param cluster
	 * @param addonNamespace
	 * @param addonManagerName
	 * @throws Exception
	 */
	public void deleteAddonManager(ClusterVO cluster, String addonNamespace, String addonManagerName) throws Exception {
		/** Addon Manager 제거 **/
		/// 배포된 Addon-Manager가 있을때만 UnInstall
		if (packageValidService.isUsingReleaseName(cluster.getClusterSeq(), addonNamespace, addonManagerName, false)) {
			packageCommonService.unInstallPackage(cluster.getClusterSeq(), addonNamespace, addonManagerName);
			log.info("##### Addon-Manager removal ended successfully. : " + addonManagerName);
		}
		else {
			log.warn("##### Addon-Manager not found. : " + addonManagerName);
		}

		cluster.setNamespaceName(addonNamespace);

		ConfigMapGuiVO c1 = configMapService.getConfigMap(cluster.getClusterSeq(), addonNamespace, addonManagerName);
		if (c1 != null) {
			configMapService.deleteConfigMap(cluster, addonNamespace, addonManagerName);
			log.info("##### Addon-Manager Configmap removal ended successfully. : " + addonManagerName);
		}
		else {
			log.warn("##### Addon-Manager Configmap not found. : " + addonManagerName);
		}

		/** Addon List 제거 **/
		ConfigMapGuiVO c2 = configMapService.getConfigMap(cluster.getClusterSeq(), addonNamespace, AddonConstants.ADDON_LIST_CONFIGMAP_NAME);
		if (c2 != null) {
			configMapService.deleteConfigMap(cluster, addonNamespace, AddonConstants.ADDON_LIST_CONFIGMAP_NAME);
			log.info("##### Addon-List Configmap removal ended successfully. : " + AddonConstants.ADDON_LIST_CONFIGMAP_NAME);
		}
		else {
			log.warn("##### Addon-List Configmap not found. : " + AddonConstants.ADDON_LIST_CONFIGMAP_NAME);
		}
	}

	public List<AddonInfoBaseVO> getInstallableAddons(Integer clusterSeq, String labels) throws Exception {
		if(clusterSeq == null) {
			throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
		}
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster =  clusterDao.getCluster(clusterSeq);
		if(cluster == null) {
			throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
		}
		return this.getInstallableAddons(cluster, labels);
	}

	public List<AddonInfoBaseVO> getInstallableAddons(String clusterId, String labels) throws Exception {
		if(StringUtils.isBlank(clusterId)) {
			throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
		}
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster =  clusterDao.getClusterByClusterId(clusterId, "Y");
		if(cluster == null) {
			throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
		}
		return this.getInstallableAddons(cluster, labels);

	}

	private List<AddonInfoBaseVO> getInstallableAddons(ClusterVO cluster, String labels) throws Exception {
		cluster.setNamespaceName(this.getCocktailAddonNamespace());
		List<ConfigMapGuiVO> installedAddons = addonCommonService.getAddonConfigMaps(cluster, labels);

		ConfigMapGuiVO addonCm = configMapService.getConfigMap(cluster, this.getCocktailAddonNamespace(), AddonConstants.ADDON_LIST_CONFIGMAP_NAME);

		String addonListStr = null;
		if(addonCm != null) {
			addonListStr = Optional.ofNullable(addonCm.getData()).orElseGet(() ->Maps.newHashMap()).get("items");
		}

		if(StringUtils.isBlank(addonListStr)) {
			/**
			 * Addon 목록을 조회할 수 없음 => Addon-Manager의 이상 상태라 고 판단됨...
			 * - Addon-Manager의 설치가 필요하므로 Addon-Manager를 기본으로 응답하도록 처리.
			 */
			// Latest Addon Manager를 조회
			/** 01. Latest Addon Manager를 조회 **/
			String addonManagerVersion = cocktailAddonProperties.getAddonManagerChartVersion();
			if(StringUtils.equalsIgnoreCase(addonManagerVersion, "latest")) {
				ChartInfoBaseVO chartVersionInfo = packageCommonService.getLastestChartVersion(cocktailAddonProperties.getAddonChartRepoProjectName(), cocktailAddonProperties.getAddonManagerChartName());
				addonManagerVersion = chartVersionInfo.getVersion();
			}

			// Latest Addon Manager Chart의 상세 정보 조회.
			ChartInfoBaseVO chartInfo = packageCommonService.getChart(cocktailAddonProperties.getAddonChartRepoProjectName(), cocktailAddonProperties.getAddonManagerChartName(), addonManagerVersion);
			List<AddonInfoBaseVO> addonManager = new ArrayList<>();
			AddonInfoBaseVO am = new AddonInfoBaseVO();

			BeanUtils.copyProperties(chartInfo, am);
			am.setRepo(cocktailAddonProperties.getAddonChartRepoProjectName());
			am.setCurrentInstallation(0);
			am.setMaxInstallation(1);
			am.setMultipleInstallable(false);
			am.setInstalled(false);

			addonManager.add(am);

			return addonManager;
		}

		List<AddonInfoBaseVO> addons = JsonUtils.fromGson(addonListStr, new TypeToken<List<AddonInfoBaseVO>>(){}.getType());
		addons = addons.stream()
			.filter(chart ->
				(
					StringUtils.isNotBlank(chart.getName()) &&
					!StringUtils.endsWithIgnoreCase(chart.getName(), "/"+KubeConstants.LABELS_ADDON_CHART_VALUE_COCKTAIL)
				)
			)
			.collect(Collectors.toList());
		for(AddonInfoBaseVO chart : addons) {
			int lastIndex = chart.getName().lastIndexOf("/");
			if(lastIndex >= 1) {
				chart.setRepo(chart.getName().substring(0, lastIndex));
				chart.setName(chart.getName().substring(lastIndex+1));
			}

			Long maxInstallation = 1L;
			Toml configToml = new Toml().read(chart.getConfigToml());
			if(configToml != null) {
				Toml common = configToml.getTable(AddonConstants.ADDON_CONFIG_COMMON);
				if(common != null) {
					maxInstallation = common.getLong("maxInstallation"); // 최대 설치 갯수
				}
			}
			if(maxInstallation != null) {
				if( maxInstallation > 1 ) {
					chart.setMultipleInstallable(true);
				}
				chart.setMaxInstallation(maxInstallation.intValue());
			}
			int foundInstallation = 0;
			for(ConfigMapGuiVO cm : installedAddons) {
				if(StringUtils.equalsIgnoreCase(Optional.of(cm.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_CHART_KEY), chart.getName())) {
					chart.setInstalled(true);
					foundInstallation++;
				}
			}
			chart.setCurrentInstallation(foundInstallation);

		}

		return addons;
	}

	/**
	 * addon configMap 조회
	 *
	 * @param clusterSeq
	 * @return
	 * @throws Exception
	 */
	public ConfigMapGuiVO getAddonConfigMap(Integer clusterSeq, String addonName, boolean showValueYaml, boolean showKiali) throws Exception {

		ConfigMapGuiVO addon = new ConfigMapGuiVO();

		if (clusterSeq != null) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
			ClusterVO cluster =  clusterDao.getCluster(clusterSeq);

			if (cluster != null) {
				addon = this.getAddonConfigMap(cluster, addonName, showValueYaml, showKiali);
			}
		}

		return addon;
	}

	/**
	 * addon configMap 조회
	 *
	 * @param clusterId
	 * @param addonName
	 * @param showValueYaml
	 * @return
	 * @throws Exception
	 */
	public ConfigMapGuiVO getAddonConfigMap(String clusterId, String addonName, boolean showValueYaml, boolean showKiali) throws Exception {

		ConfigMapGuiVO addon = new ConfigMapGuiVO();

		if (StringUtils.isNotBlank(clusterId)) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
			ClusterVO cluster =  clusterDao.getClusterByClusterId(clusterId, "Y");

			if (cluster != null) {
				addon = this.getAddonConfigMap(cluster, addonName, showValueYaml, showKiali);
			}
		}

		return addon;
	}

	public ConfigMapGuiVO getAddonConfigMap(Integer servicemapSeq, String addonName, boolean showValueYaml) throws Exception {

		ConfigMapGuiVO addon = new ConfigMapGuiVO();

		if (servicemapSeq != null) {
			ClusterVO cluster =  k8sResourceService.setupCluster(servicemapSeq);

			if (cluster != null) {
				addon = this.getAddonConfigMap(cluster, addonName, showValueYaml);
			}
		}

		return addon;
	}

	public ConfigMapGuiVO getAddonConfigMap(ClusterVO cluster, String addonName, boolean showValueYaml) throws Exception {
		return this.getAddonConfigMap(cluster, addonName, showValueYaml, false);
	}

	public ConfigMapGuiVO getAddonConfigMap(ClusterVO cluster, String addonName, boolean showValueYaml, boolean showKiali) throws Exception {

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(cluster);

		ConfigMapGuiVO currAddon = configMapService.getConfigMap(cluster.getClusterSeq(), this.getCocktailAddonNamespace(), addonName);

		if (currAddon != null) {
			Map<String, String> currAddonLabels = Optional.ofNullable(currAddon.getLabels()).orElseGet(() ->Maps.newHashMap());

			currAddon.getData().remove(AddonKeyItem.MANIFEST.getValue());

			if (!showValueYaml) {
				currAddon.getData().remove(AddonKeyItem.VALUE_YAML.getValue());
				currAddon.getData().remove(AddonKeyItem.VALUE_YAML_PREV.getValue());
//				currAddon.getData().remove(AddonKeyItem.ADDON_TOML.getValue());
//				currAddon.getData().remove(AddonKeyItem.ADDON_TOML_PREV.getValue());
//				currAddon.getData().remove(AddonKeyItem.ADDON_YAML.getValue());
//				currAddon.getData().remove(AddonKeyItem.ADDON_YAML_PREV.getValue());
				currAddon.setDeployment(null);
				currAddon.setDeploymentYaml(null);
			}
			if( true ) { /** 2021.04.07. Istio에서 Kiali 인증서를 더이상 생성하지 않음 **/ //TODO : To be deleted
				currAddon.getData().remove(AddonKeyItem.KIALI_CA_PUBLIC.getValue());
				currAddon.getData().remove(AddonKeyItem.KIALI_CA_PRVATE.getValue());
				currAddon.getData().remove(AddonKeyItem.KIALI_CA_CRT.getValue());
				currAddon.getData().remove(AddonKeyItem.KIALI_ADDRESS_LIST.getValue());
			}
			if (showKiali) {
				V1Secret kialiSecretV1 = secretService.getSecretV1(cluster, KubeConstants.ISTIO_SYSTEM_NAMESPACE, KubeConstants.KIALI_USER_SECRET_NAME);
				SecretGuiVO kialiSecret = secretService.convertSecretData(kialiSecretV1, true);

				if(kialiSecret != null && MapUtils.isNotEmpty(kialiSecret.getData())) {
					String kUrl = MapUtils.getString(kialiSecret.getData(), "url", "");
					String kUsername = MapUtils.getString(kialiSecret.getData(), "username", "");
					String kPassword = MapUtils.getString(kialiSecret.getData(), "password", "");
					if(Utils.isBase64Encoded(kUrl)) {
						kUrl = new String(Base64Utils.decodeFromString(MapUtils.getString(kialiSecret.getData(), "url", "")));
					}
					if(Utils.isBase64Encoded(kUsername)) {
						kUsername = new String(Base64Utils.decodeFromString(MapUtils.getString(kialiSecret.getData(), "username", "")));
					}
					if(Utils.isBase64Encoded(kPassword)) {
						kPassword = new String(Base64Utils.decodeFromString(MapUtils.getString(kialiSecret.getData(), "password", "")));
					}
					currAddon.getData().put(AddonKeyItem.KIALI_URL.getValue(), kUrl);
					currAddon.getData().put(AddonKeyItem.KIALI_USER.getValue(), kUsername);
					currAddon.getData().put(AddonKeyItem.KIALI_PASSWORD.getValue(), kPassword);
				}
				// Kiali Url이 설정되지 않았으면 Default 값을 설정 해 준다.
				if(StringUtils.isBlank(currAddon.getData().get(AddonKeyItem.KIALI_URL.getValue()))) {
					currAddon.getData().put(AddonKeyItem.KIALI_URL.getValue(), cocktailAddonProperties.getKialiUrl());
				}
			}

			if(MapUtils.isEmpty(currAddon.getData())) {
				return currAddon;
			}

			for (String key : currAddon.getData().keySet()) {
				// addon.toml Data가 있으면 Base64 Decode 해서 넣는다.
				if (StringUtils.equalsIgnoreCase(key, AddonKeyItem.ADDON_TOML.getValue())) {
					// Base64 Encode 되어 있으면 decode해서 넣음.
					if(Utils.isBase64Encoded(MapUtils.getString(currAddon.getData(), key, ""))) {
						String decodeString = new String(Base64Utils.decodeFromString(MapUtils.getString(currAddon.getData(), key, "")), StandardCharsets.UTF_8);

						if(this.isValidToml(decodeString)) {
							currAddon.getData().put(key, decodeString);
						}
						else {
							// Toml 변환이 되지 않으면 올바르지 않은 데이터 => null
							currAddon.getData().put(key, null);
						}
					}
					// Base64 Encode 데이터가 아니면..
					else {
						if(this.isValidToml(MapUtils.getString(currAddon.getData(), key, ""))) {
							currAddon.getData().put(key, MapUtils.getString(currAddon.getData(), key, ""));
						}
						else {
							// Toml 변환이 되지 않으면 올바르지 않은 데이터 => null
							currAddon.getData().put(key, null);
						}
					}

					/**
					 * 2020.09.03 Hotfix
					 * Monitoring Addon일 경우 Addon.toml 데이터에서 Etcd 인증서 정보를 제거하고 mandatory=false로 설정하여 UI로 응답하도록 한다.
					 * UI에서는 Mandatory=false인 필드는 필수 입력값 체크를 하지 않도록 구현 하고,
					 * 이후 Update 처리시 Etcd 인증서 정보가 없으면 기존 인증서 정보를 읽어서 설정할 수 있어야 함.
					 */
					// Monitoring Addon 일 경우.
					if(StringUtils.equalsIgnoreCase(currAddonLabels.get(KubeConstants.LABELS_ADDON_CHART_KEY), "prometheus-operator")) {
						String maskedString = this.mergeMonitoringTomlData(currAddon.getData().get(AddonKeyItem.ADDON_TOML.getValue()), null);
						currAddon.getData().put(AddonKeyItem.ADDON_TOML.getValue(), maskedString);
					}
					// istio Addon 일 경우.
					else if(StringUtils.equalsIgnoreCase(currAddonLabels.get(KubeConstants.LABELS_ADDON_CHART_KEY), "istiod")) {
						String maskedString = this.mergeIstioTomlData(currAddon.getData().get(AddonKeyItem.ADDON_TOML.getValue()), null);
						currAddon.getData().put(AddonKeyItem.ADDON_TOML.getValue(), maskedString);
					}
				}

				/** Kiali **/
				/** 2021.04.07. Istio에서 Kiali 인증서를 더이상 생성하지 않음 **/ //TODO : To be deleted
//				if (false && StringUtils.equalsIgnoreCase(key, AddonKeyItem.KIALI_CA_CRT.getValue())) {
//					currAddon.getData().put(key, CryptoUtils.decryptDefaultAES(currAddon.getData().get(key)));
//				}
			}
		}
		else {
			return null;
		}

		return currAddon;
	}

	/**
	 * Monitoring Addon일 경우에만 배포정보 조회시 인증서 정보를 숨기는 Specific 처리가 필요.
	 * 수정시에는 인증서 정보가 없을 경우 기존에 입력된 인증서 정보로 배포될 수 있도록 처리 해 주어야 함.
	 *
	 * @param targetString
	 * @param sourceString
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private String mergeMonitoringTomlData(String targetString, String sourceString) throws Exception {
		if (StringUtils.isNotBlank(targetString)) {

			final String ETCD_ENDPOINTS_MAP_KEY = "kubeEtcd_endpoints";
			final String ETCD_CA_CRT_MAP_KEY = "kubeEtcd_caCrt";
			final String ETCD_CLIENT_CRT_MAP_KEY = "kubeEtcd_healthcheckClientCrt";
			final String ETCD_CLIENT_KEY_MAP_KEY = "kubeEtcd_healthcheckClientKey";

			/** get TomlData **/
			Toml addonToml = new Toml().read(targetString);
			Map<String, Object> addonTomlMap = addonToml.toMap();
			Map<String, Object> releaseTomlMap = (Map<String, Object>) addonTomlMap.get(AddonConstants.ADDON_VALUE_RELEASE);
			Map<String, Object> detailTomlMap = (Map<String, Object>) addonTomlMap.get(AddonConstants.ADDON_VALUE_DETAIL);
			Map<String, Object> setupTomlMap = (Map<String, Object>) addonTomlMap.get(AddonConstants.ADDON_VALUE_SETUP);
			Map<String, Object> specificTomlMap = (Map<String, Object>) addonTomlMap.get(AddonConstants.ADDON_VALUE_SPECIFIC);

			Map<String, Object> endpoints = (Map<String, Object>) detailTomlMap.get(ETCD_ENDPOINTS_MAP_KEY);
			Map<String, Object> caCrt = (Map<String, Object>) detailTomlMap.get(ETCD_CA_CRT_MAP_KEY);
			Map<String, Object> clientCrt = (Map<String, Object>) detailTomlMap.get(ETCD_CLIENT_CRT_MAP_KEY);
			Map<String, Object> clientKey = (Map<String, Object>) detailTomlMap.get(ETCD_CLIENT_KEY_MAP_KEY);

			/** sourceString이 Blank이면 masking 처리 **/
			if(StringUtils.isBlank(sourceString)) {
				/** ETCD 데이터 초기화 **/
				if (MapUtils.isNotEmpty(endpoints)) {
					endpoints.put("value", "[]");
					endpoints.put("mandatory", false);
				}
				if (MapUtils.isNotEmpty(caCrt)) {
					caCrt.put("value", "");
					caCrt.put("mandatory", false);
				}
				if (MapUtils.isNotEmpty(clientCrt)) {
					clientCrt.put("value", "");
					clientCrt.put("mandatory", false);
				}
				if (MapUtils.isNotEmpty(clientKey)) {
					clientKey.put("value", "");
					clientKey.put("mandatory", false);
				}
			}
			/** sourceString Data가 존재하면 source Data로 replace **/
			else {
				try {
					Toml srcAddonToml = new Toml().read(sourceString);
					Map<String, Object> srcAddonTomlMap = srcAddonToml.toMap();
					Map<String, Object> srcDetailTomlMap = (Map<String, Object>) srcAddonTomlMap.get(AddonConstants.ADDON_VALUE_DETAIL);

					Map<String, Object> srcEndpoints = (Map<String, Object>) srcDetailTomlMap.get(ETCD_ENDPOINTS_MAP_KEY);
					Map<String, Object> srcCaCrt = (Map<String, Object>) srcDetailTomlMap.get(ETCD_CA_CRT_MAP_KEY);
					Map<String, Object> srcClientCrt = (Map<String, Object>) srcDetailTomlMap.get(ETCD_CLIENT_CRT_MAP_KEY);
					Map<String, Object> srcClientKey = (Map<String, Object>) srcDetailTomlMap.get(ETCD_CLIENT_KEY_MAP_KEY);

					/** Target Data **/
					String sEndpoints = MapUtils.getString(endpoints, "value", null);
					String sCaCrt = MapUtils.getString(caCrt, "value", null);
					String sClientCrt = MapUtils.getString(clientCrt, "value", null);
					String sClientKey = MapUtils.getString(clientKey, "value", null);

					/** Source Data **/
					String sSrcEndpoints = MapUtils.getString(srcEndpoints, "value", null);
					String sSrcCaCrt = MapUtils.getString(srcCaCrt, "value", null);
					String sSrcClientCrt = MapUtils.getString(srcClientCrt, "value", null);
					String sSrcClientKey = MapUtils.getString(srcClientKey, "value", null);

					/** 입력 값이 없는 경우가 하나라도 있으면 source에서 데이터를 읽도록 함 **/
					if (StringUtils.isBlank(sEndpoints) || StringUtils.equals(sEndpoints, "[]") ||
						StringUtils.isBlank(sCaCrt) || StringUtils.isBlank(sClientCrt) || StringUtils.isBlank(sClientKey)) {

						/** if endpoint 입력이 없으면 replace **/
						if ( (StringUtils.isBlank(sEndpoints) || StringUtils.equals(sEndpoints, "[]"))
								&& StringUtils.isNotBlank(sSrcEndpoints) && endpoints != null)
						{
							endpoints.put("value", sSrcEndpoints);
						}
						/** if caCrt 입력이 없으면 replace **/
						if (StringUtils.isBlank(sCaCrt) && StringUtils.isNotBlank(sSrcCaCrt) && caCrt != null ) {
							caCrt.put("value", sSrcCaCrt);
						}
						/** if clientCrt 입력이 없으면 replace **/
						if (StringUtils.isBlank(sClientCrt) && StringUtils.isNotBlank(sSrcClientCrt) && clientCrt != null ) {
							clientCrt.put("value", sSrcClientCrt);
						}
						/** if clientKey 입력이 없으면 replace **/
						if (StringUtils.isBlank(sClientKey) && StringUtils.isNotBlank(sSrcClientKey) && clientKey != null ) {
							clientKey.put("value", sSrcClientKey);
						}
					}
				}
				catch (Exception ex) {
					log.error("Monitoring Addon : Invalid source datas : " + ex.getMessage());
					throw new CocktailException("Invalid Toml Data !!", ExceptionType.InvalidTomlData);
				}
			}

			/** detail 데이터 순서 정렬 (UI에서 표시되는 순서) : Toml Object로 변환되는 순간 순서가 없어지는 문제로 인함... **/
			String detailPrefix = "";
			Integer num = 10000;
			List<Pair<String, Integer>> detailList = new ArrayList<>();

			// Detail 데이터를 읽어 순번을 가져옴
			for (String akey : detailTomlMap.keySet()) {
				if (StringUtils.equals(akey, "visible")) {
//					detailPrefix = detailPrefix + akey + " = " + detailTomlMap.get(akey) + "\n";
					detailPrefix = String.format("%s%s = %s\n", detailPrefix, akey, detailTomlMap.get(akey));
					continue;
				}
				else if (StringUtils.equals(akey, "display")) {
//					detailPrefix = detailPrefix + akey + " = \"" + detailTomlMap.get(akey) + "\"\n";
					detailPrefix = String.format("%s%s = \"%s\"\n", detailPrefix, akey, detailTomlMap.get(akey));
					continue;
				}

				Map<String, Object> orderMap = (Map<String, Object>) detailTomlMap.get(akey);
				Integer order = MapUtils.getInteger(orderMap, "order", null);
				if (order != null) {
					detailList.add(Pair.of(akey, order));
				}
				else {
					// order가 없으면 현재 객체 순서대로 number 채번...
					detailList.add(Pair.of(akey, num++));
				}
//							log.debug(akey + " : " + order);
			}

			/** Order대로 Sorting...... **/
			List<Pair<String, Integer>> sortedKeyList = detailList.stream().sorted(Comparator.comparingInt(Pair::getRight)).collect(Collectors.toList());

			/** Detail 데이터를 LinkedHashMap을 이용하여 정의된 순서대로 정렬 **/
			LinkedHashMap<String, Object> detailLinkedMap = new LinkedHashMap<>();
			for (Pair<String, Integer> akey : sortedKeyList) {
				Map<String, Object> tomlData = (Map<String, Object>) MapUtils.getObject(detailTomlMap, akey.getLeft(), null);
				detailLinkedMap.put(akey.getLeft(), tomlData);
			}

			/** Toml -> Map으로 변환시 상위 객체가 사라짐 -> Wrapper로 생성하여 넣어줌 **/
			Map<String, Object> relaseTomlMapWrapper = new HashMap<>();
			Map<String, Object> setupTomlMapWrapper = new HashMap<>();
			Map<String, Object> detailTomlMapWrapper = new HashMap<>();
			Map<String, Object> specificTomlMapWrapper = new HashMap<>();

			TomlWriter tomlWriter = new TomlWriter();
			relaseTomlMapWrapper.put(AddonConstants.ADDON_VALUE_RELEASE, releaseTomlMap);
			String releaseString = tomlWriter.write(relaseTomlMapWrapper);
			setupTomlMapWrapper.put(AddonConstants.ADDON_VALUE_SETUP, setupTomlMap);
			String setupString = tomlWriter.write(setupTomlMapWrapper);
			detailTomlMapWrapper.put(AddonConstants.ADDON_VALUE_DETAIL, detailLinkedMap);
			String detailString = tomlWriter.write(detailTomlMapWrapper);
			specificTomlMapWrapper.put(AddonConstants.ADDON_VALUE_SPECIFIC, specificTomlMap);
			String specificString = tomlWriter.write(specificTomlMapWrapper);

			/** Title String은 별도 처리 해주고 **/
			String titleString = String.format("%s = \"%s\"", AddonConstants.ADDON_VALUE_TITLE, addonToml.getString(AddonConstants.ADDON_VALUE_TITLE));

			/** 만들어진 문자열을 Merge하여 Toml 생성 **/
			StringBuilder finalTomlString = new StringBuilder();
			finalTomlString.append(titleString)
					.append("\n")
					.append(releaseString)
					.append("\n")
					.append(setupString)
					.append("\n[detail]\n")
					.append(detailPrefix)
					.append("\n")
					.append(detailString)
					.append("\n")
					.append(specificString)
					.append("\n");

			return finalTomlString.toString();
		}

		return targetString;
	}

	/**
	 * Istio Addon일 경우에만 배포정보 조회시 kiali url, username, password, 인증서 정보를 숨기는 Specific 처리가 필요.
	 * 수정시에는 kiali url, username, password, 인증서 정보가 없을 경우
	 * 기존에 입력된 kiali url, username, password, 인증서 정보로 배포될 수 있도록 처리 해 주어야 함.
	 *
	 * @param targetString
	 * @param sourceString
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private String mergeIstioTomlData(String targetString, String sourceString) throws Exception {
		if (StringUtils.isNotBlank(targetString)) {

			final String KIALI_URL_MAP_KEY = "kiali_url";
			final String KIALI_USERNAME_MAP_KEY = "kiali_username";
			final String KIALI_PASSWORD_MAP_KEY = "kiali_password";
			final String KIALI_TLS_CERT_MAP_KEY = "kiali_tls-cert";
			final String KIALI_TLS_KEY_MAP_KEY = "kiali_tls-key";

			/** get TomlData **/
			Toml addonToml = new Toml().read(targetString);
			Map<String, Object> addonTomlMap = addonToml.toMap();
			Map<String, Object> releaseTomlMap = (Map<String, Object>) addonTomlMap.get(AddonConstants.ADDON_VALUE_RELEASE);
			Map<String, Object> detailTomlMap = (Map<String, Object>) addonTomlMap.get(AddonConstants.ADDON_VALUE_DETAIL);
			Map<String, Object> setupTomlMap = (Map<String, Object>) addonTomlMap.get(AddonConstants.ADDON_VALUE_SETUP);
			Map<String, Object> specificTomlMap = (Map<String, Object>) addonTomlMap.get(AddonConstants.ADDON_VALUE_SPECIFIC);

			Map<String, Object> tgtDetailKialiUrlMap = (Map<String, Object>) detailTomlMap.get(KIALI_URL_MAP_KEY);
			Map<String, Object> tgtDetailKialiUsernameMap = (Map<String, Object>) detailTomlMap.get(KIALI_USERNAME_MAP_KEY);
			Map<String, Object> tgtDetailKialiPasswordMap = (Map<String, Object>) detailTomlMap.get(KIALI_PASSWORD_MAP_KEY);
			Map<String, Object> tgtDetailKialiTlsCertMap = (Map<String, Object>) detailTomlMap.get(KIALI_TLS_CERT_MAP_KEY);
			Map<String, Object> tgtDetailKialiTlsKeyMap = (Map<String, Object>) detailTomlMap.get(KIALI_TLS_KEY_MAP_KEY);

			/** sourceString이 Blank이면 masking 처리 **/
			if(StringUtils.isBlank(sourceString)) {
				/** 데이터 초기화 **/
				if (MapUtils.isNotEmpty(tgtDetailKialiUrlMap)) {
					tgtDetailKialiUrlMap.put("value", "");
					tgtDetailKialiUrlMap.put("mandatory", false);
				}
				if (MapUtils.isNotEmpty(tgtDetailKialiUsernameMap)) {
					tgtDetailKialiUsernameMap.put("value", "");
					tgtDetailKialiUsernameMap.put("mandatory", false);
				}
				if (MapUtils.isNotEmpty(tgtDetailKialiPasswordMap)) {
					tgtDetailKialiPasswordMap.put("value", "");
					tgtDetailKialiPasswordMap.put("mandatory", false);
				}
				if (MapUtils.isNotEmpty(tgtDetailKialiTlsCertMap)) {
					tgtDetailKialiTlsCertMap.put("value", "");
					tgtDetailKialiTlsCertMap.put("mandatory", false);
				}
				if (MapUtils.isNotEmpty(tgtDetailKialiTlsKeyMap)) {
					tgtDetailKialiTlsKeyMap.put("value", "");
					tgtDetailKialiTlsKeyMap.put("mandatory", false);
				}
			}
			/** sourceString Data가 존재하면 source Data로 replace **/
			else {
				try {
					Toml srcAddonToml = new Toml().read(sourceString);
					Map<String, Object> srcAddonTomlMap = srcAddonToml.toMap();
					Map<String, Object> srcDetailTomlMap = (Map<String, Object>) srcAddonTomlMap.get(AddonConstants.ADDON_VALUE_DETAIL);

					Map<String, Object> srcDetailKialiUrlMap = (Map<String, Object>) srcDetailTomlMap.get(KIALI_URL_MAP_KEY);
					Map<String, Object> srcDetailKialiUsernameMap = (Map<String, Object>) srcDetailTomlMap.get(KIALI_USERNAME_MAP_KEY);
					Map<String, Object> srcDetailKialiPasswordMap = (Map<String, Object>) srcDetailTomlMap.get(KIALI_PASSWORD_MAP_KEY);
					Map<String, Object> srcDetailKialiTlsCertMap = (Map<String, Object>) srcDetailTomlMap.get(KIALI_TLS_CERT_MAP_KEY);
					Map<String, Object> srcDetailKialiTlsKeyMap = (Map<String, Object>) srcDetailTomlMap.get(KIALI_TLS_KEY_MAP_KEY);

					/** Target Data **/
					String vTgtKialiUrl = MapUtils.getString(tgtDetailKialiUrlMap, "value", null);
					String vTgtKialiUsername = MapUtils.getString(tgtDetailKialiUsernameMap, "value", null);
					String vTgtKialiPassword = MapUtils.getString(tgtDetailKialiPasswordMap, "value", null);
					String vTgtKialiTlsCert = MapUtils.getString(tgtDetailKialiTlsCertMap, "value", null);
					String vTgtKialiTlsKey = MapUtils.getString(tgtDetailKialiTlsKeyMap, "value", null);

					/** Source Data **/
					String vSrcKialiUrl = MapUtils.getString(srcDetailKialiUrlMap, "value", null);
					String vSrcKialiUsername = MapUtils.getString(srcDetailKialiUsernameMap, "value", null);
					String vSrcKialiPassword = MapUtils.getString(srcDetailKialiPasswordMap, "value", null);
					String vSrcKialiTlsCert = MapUtils.getString(srcDetailKialiTlsCertMap, "value", null);
					String vSrcKialiTlsKey = MapUtils.getString(srcDetailKialiTlsKeyMap, "value", null);

					/** 입력 값이 없는 경우가 하나라도 있으면 source에서 데이터를 읽도록 함 **/
					if (StringUtils.isBlank(vTgtKialiUrl) || StringUtils.isBlank(vTgtKialiUsername) || StringUtils.isBlank(vTgtKialiPassword) ||
							StringUtils.isBlank(vTgtKialiTlsCert) || StringUtils.isBlank(vTgtKialiTlsKey)) {

						/** if kiali_url 입력이 없으면 replace **/
						if (StringUtils.isBlank(vTgtKialiUrl) && StringUtils.isNotBlank(vSrcKialiUrl) && tgtDetailKialiUrlMap != null) {
							tgtDetailKialiUrlMap.put("value", vSrcKialiUrl);
						}
						/** if kiali_username 입력이 없으면 replace **/
						if (StringUtils.isBlank(vTgtKialiUsername) && StringUtils.isNotBlank(vSrcKialiUsername) && tgtDetailKialiUsernameMap != null) {
							tgtDetailKialiUsernameMap.put("value", vSrcKialiUsername);
						}
						/** if kiali_password 입력이 없으면 replace **/
						if (StringUtils.isBlank(vTgtKialiPassword) && StringUtils.isNotBlank(vSrcKialiPassword) && tgtDetailKialiPasswordMap != null) {
							tgtDetailKialiPasswordMap.put("value", vSrcKialiPassword);
						}
						/** if kiali_tls-cert 입력이 없으면 replace **/
						if (StringUtils.isBlank(vTgtKialiTlsCert) && StringUtils.isNotBlank(vSrcKialiTlsCert) && tgtDetailKialiTlsCertMap != null) {
							tgtDetailKialiTlsCertMap.put("value", vSrcKialiTlsCert);
						}
						/** if clientKey 입력이 없으면 replace **/
						if (StringUtils.isBlank(vTgtKialiTlsKey) && StringUtils.isNotBlank(vSrcKialiTlsKey) && tgtDetailKialiTlsKeyMap != null) {
							tgtDetailKialiTlsKeyMap.put("value", vSrcKialiTlsKey);
						}
					}
				}
				catch (Exception ex) {
					log.error("Monitoring Addon : Invalid source datas : " + ex.getMessage());
					throw new CocktailException("Invalid Toml Data !!", ExceptionType.InvalidTomlData);
				}
			}

			/** detail 데이터 순서 정렬 (UI에서 표시되는 순서) : Toml Object로 변환되는 순간 순서가 없어지는 문제로 인함... **/
			String detailPrefix = "";
			Integer num = 10000;
			List<Pair<String, Integer>> detailList = new ArrayList<>();

			// Detail 데이터를 읽어 순번을 가져옴
			for (String akey : detailTomlMap.keySet()) {
				if (StringUtils.equals(akey, "visible")) {
//					detailPrefix = detailPrefix + akey + " = " + detailTomlMap.get(akey) + "\n";
					detailPrefix = String.format("%s%s = %s\n", detailPrefix, akey, detailTomlMap.get(akey));
					continue;
				}
				else if (StringUtils.equals(akey, "display")) {
//					detailPrefix = detailPrefix + akey + " = \"" + detailTomlMap.get(akey) + "\"\n";
					detailPrefix = String.format("%s%s = \"%s\"\n", detailPrefix, akey, detailTomlMap.get(akey));
					continue;
				}

				Map<String, Object> orderMap = (Map<String, Object>) detailTomlMap.get(akey);
				Integer order = MapUtils.getInteger(orderMap, "order", null);
				if (order != null) {
					detailList.add(Pair.of(akey, order));
				}
				else {
					// order가 없으면 현재 객체 순서대로 number 채번...
					detailList.add(Pair.of(akey, num++));
				}
//							log.debug(akey + " : " + order);
			}

			/** Order대로 Sorting...... **/
			List<Pair<String, Integer>> sortedKeyList = detailList.stream().sorted(Comparator.comparingInt(Pair::getRight)).collect(Collectors.toList());

			/** Detail 데이터를 LinkedHashMap을 이용하여 정의된 순서대로 정렬 **/
			LinkedHashMap<String, Object> detailLinkedMap = new LinkedHashMap<>();
			for (Pair<String, Integer> akey : sortedKeyList) {
				Map<String, Object> tomlData = (Map<String, Object>) MapUtils.getObject(detailTomlMap, akey.getLeft(), null);
				detailLinkedMap.put(akey.getLeft(), tomlData);
			}

			/** Toml -> Map으로 변환시 상위 객체가 사라짐 -> Wrapper로 생성하여 넣어줌 **/
			Map<String, Object> relaseTomlMapWrapper = new HashMap<>();
			Map<String, Object> setupTomlMapWrapper = new HashMap<>();
			Map<String, Object> detailTomlMapWrapper = new HashMap<>();
			Map<String, Object> specificTomlMapWrapper = new HashMap<>();

			TomlWriter tomlWriter = new TomlWriter();
			relaseTomlMapWrapper.put(AddonConstants.ADDON_VALUE_RELEASE, releaseTomlMap);
			String releaseString = tomlWriter.write(relaseTomlMapWrapper);
			setupTomlMapWrapper.put(AddonConstants.ADDON_VALUE_SETUP, setupTomlMap);
			String setupString = tomlWriter.write(setupTomlMapWrapper);
			detailTomlMapWrapper.put(AddonConstants.ADDON_VALUE_DETAIL, detailLinkedMap);
			String detailString = tomlWriter.write(detailTomlMapWrapper);
			specificTomlMapWrapper.put(AddonConstants.ADDON_VALUE_SPECIFIC, specificTomlMap);
			String specificString = tomlWriter.write(specificTomlMapWrapper);

			/** Title String은 별도 처리 해주고 **/
			String titleString = String.format("%s = \"%s\"", AddonConstants.ADDON_VALUE_TITLE, addonToml.getString(AddonConstants.ADDON_VALUE_TITLE));

			/** 만들어진 문자열을 Merge하여 Toml 생성 **/
			StringBuilder finalTomlString = new StringBuilder();
			finalTomlString.append(titleString)
					.append("\n")
					.append(releaseString)
					.append("\n")
					.append(setupString)
					.append("\n[detail]\n")
					.append(detailPrefix)
					.append("\n")
					.append(detailString)
					.append("\n")
					.append(specificString)
					.append("\n");

			return finalTomlString.toString();
		}

		return targetString;
	}

	@Deprecated
	public ConfigMapGuiVO updateAddon(Integer clusterSeq, ConfigMapGuiVO updtAddon) throws Exception {

		List<String> updateKeyItem = Arrays.asList(AddonKeyItem.VALUE_YAML.getValue(), AddonKeyItem.AUTO_UPDATE.getValue());

		ConfigMapGuiVO currAddon = configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), updtAddon.getName());

		if (currAddon != null) {
			List<JsonObject> patchBody = new ArrayList<>();

			for (String itemKey : updateKeyItem) {
				if (StringUtils.isNotBlank(updtAddon.getData().get(itemKey))) {
					if (!StringUtils.equals(StringUtils.trim(updtAddon.getData().get(itemKey)), currAddon.getData().get(itemKey))) {
						Map<String, Object> patchMap = new HashMap<>();
						patchMap.put("op", JsonPatchOp.REPLACE.getValue());
						patchMap.put("path", String.format("/data/%s", itemKey));
						patchMap.put("value", StringUtils.trim(updtAddon.getData().get(itemKey)));
						patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());

						if (StringUtils.equals(AddonKeyItem.VALUE_YAML.getValue(), itemKey)) {

							String patchOp = JsonPatchOp.REPLACE.getValue();
							if (!currAddon.getData().containsKey(AddonKeyItem.VALUE_YAML_PREV.getValue())) {
								patchOp = JsonPatchOp.ADD.getValue();
							}

							patchMap = new HashMap<>();
							patchMap.put("op", patchOp);
							patchMap.put("path", String.format("/data/%s", AddonKeyItem.VALUE_YAML_PREV.getValue()));
							patchMap.put("value", currAddon.getData().get(itemKey));

							patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
						}
					}
				}
			}

			if (CollectionUtils.isNotEmpty(patchBody)) {
				AddonUtils.setAddonStatus(patchBody, KubeConstants.ADDON_STATUS_PENDING_UPGRADE);
				AddonUtils.setAddonUpdateAt(patchBody);
				configMapService.patchConfigMap(clusterSeq, this.getCocktailAddonNamespace(), currAddon.getName(), patchBody);
			}
		} else {
			throw new CocktailException("addon configMap does not exist !!", ExceptionType.K8sConfigMapNotFound);
		}

		return configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), updtAddon.getName());
	}

	public void deleteAddon(Integer clusterSeq, String addonName, String apiVersion) throws Exception {
		try {
			switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
				case V1:
					this.deleteAddonV1(clusterSeq, addonName);
					break;
				case V2:
					this.deleteAddonV2(clusterSeq, addonName);
					break;
				default:
					throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
			}
		}
		catch (Exception ex) {
			if(log.isDebugEnabled()) log.debug("trace log ", ex);
			throw new CocktailException(String.format("An error occurred while deleting an add-on. [%s]", ex.getMessage()), ex, ExceptionType.CommonDeleteFail);
		}
	}

	public void deleteAddon(Integer clusterSeq, String addonName) throws Exception {
		this.deleteAddonV1(clusterSeq, addonName);
	}

	/**
	 * addon 삭제는 useYn=N로 변경하면 삭제가 되고
	 * 배포되었던 리소스는 삭제됨
	 *
	 * @param clusterSeq
	 * @param addonName
	 * @throws Exception
	 */
	public void deleteAddonV2(Integer clusterSeq, String addonName) throws Exception {
		ClusterVO cluster = k8sResourceService.setupCluster(clusterSeq, this.getCocktailAddonNamespace());

		/** Addon Manager 삭제는 별도 처리 필요. **/
		if (StringUtils.equalsIgnoreCase(cocktailAddonProperties.getAddonManagerConfigmapPrefix(), addonName)) {
			/** Addon Manager 제거 **/
			this.deleteAddonManager(cluster, this.getCocktailAddonNamespace(), addonName);
			/** 제거 후 종료 **/
			return;
		}

		/** ConfigMap의 useYn Flag를 N으로 변경 **/
		ConfigMapGuiVO currAddon = this.deleteAddonV1(clusterSeq, addonName);

		String releaseName = Optional.ofNullable(currAddon.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_HELM_CHART_KEY);

		/** Addon이 제거되었는지 확인 (대기) 후 제거가 완료되면 Configmap도 제거한다 **/
		if(StringUtils.isNotBlank(releaseName)) {
			int waitSecond = 40; // Package Uninstall 시간이 오래 걸리는 케이스가 있어(monitoring) 최장 2분까지 대기로 변경함... TODO : 나중에 처리 로직을 변경해야 할 필요 있음... UnInstall 후 제거하던지...
			for (int i = 0; i < waitSecond; i++) { //
				log.debug(String.format("Waiting for Addon-Manager to complete the addon removal process... %ds", waitSecond - i));
  				Thread.sleep(3000);
				// package가 uninstall 되었는지 확인..
				boolean isInstalledPackage = packageValidService.isUsingReleaseName(clusterSeq, this.getCocktailAddonNamespace(), releaseName, false);
				// addon-configmap의 상태도 uninstalled로 변경되었는지 확인...
				ConfigMapGuiVO addonStatusCm = configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), addonName);
				boolean isUninstallable = false;
				if (addonStatusCm != null) {
					String addonStatus = Optional.ofNullable(addonStatusCm.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_STATUS);
					isUninstallable = StringUtils.equalsIgnoreCase(KubeConstants.ADDON_STATUS_UNINSTALLED, addonStatus);
				}

				// 모두 uninstall 되었으면 제거 시작...
				if (!isInstalledPackage && isUninstallable) {
					// monitoring, monitoringAgent가 모두 삭제되면 Addon-Manager를 제거함...
					break;
				}
			}
		}
		else {
			int waitSecond = 10; // 최대 30초만 대기하고, 30초 내에도 처리가 안되면 이후 프로세스를 진행하도록 함...
			for (int i = 0; i < waitSecond; i++) { //
				log.debug(String.format("Waiting for Addon-Manager to complete the addon removal process... %ds", waitSecond - i));
				Thread.sleep(3000);
				// release 정보가 없으면 configmap만 제거함...
				ConfigMapGuiVO addonStatusCm = configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), addonName);
				boolean isUninstallable = false;
				if (addonStatusCm != null) {
					String addonStatus = Optional.ofNullable(addonStatusCm.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_STATUS);
					isUninstallable = StringUtils.equalsIgnoreCase(KubeConstants.ADDON_STATUS_UNINSTALLED, addonStatus);
				}

				// 모두 uninstall 되었으면 제거 시작...
				if (isUninstallable) {
					// monitoring, monitoringAgent가 모두 삭제되면 Addon-Manager를 제거함...
					break;
				}
			}
		}

		configMapService.deleteConfigMap(cluster, this.getCocktailAddonNamespace(), addonName);

		/**
		 * prometheus-operator Addon의 경우 uninstall시에 kube-system에 설치된 서비스를 삭제 해 주어야 함...
		 */
		if(StringUtils.equalsIgnoreCase(releaseName, "monitoring")) {
			cluster.setNamespaceName(KubeConstants.KUBE_SYSTEM_NAMESPACE);
			String targetServiceName = AddonUtils.getAutoAddonMonitoringServiceName(releaseName);

			K8sServiceVO s1 = serviceSpecService.getService(cluster, KubeConstants.KUBE_SYSTEM_NAMESPACE, targetServiceName, new ExecutingContextVO());
			if(s1 != null) {
				serviceSpecService.deleteService(cluster, targetServiceName, null, new ExecutingContextVO());
				log.debug("##### Monitoring service removal ended successfully. : " + targetServiceName);
			}
			else {
				log.debug("##### Monitoring service not found. : " + targetServiceName);
			}
		}

		/**
		 * Monitoring Agent Addon의 경우 uninstall시에 Cluster 인증 키 expire 처리 추가 필요.
		 */
		String agentReleaseName = String.format("%s-%s-%d", cocktailAddonProperties.getMonitoringAgentConfigmapPrefix(), ResourceUtil.getResourcePrefix(), cluster.getClusterSeq());
		if(StringUtils.equalsIgnoreCase(releaseName, agentReleaseName)) {
			this.expireClusterAccessSecret(null, cluster.getClusterSeq(), ClusterAddonType.MONITORING.getCode(), false);
		}

		/**
		 * nginx-ingress Addon의 경우 uninstall시에 cocktail-addon에 설치된 Configmap을 삭제 해 주어야 함...
		 */
		if(StringUtils.equalsIgnoreCase("nginx-ingress", Optional.ofNullable(currAddon.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_CHART_KEY))) {
			String targetConfigmapName = String.format("ingress-controller-leader-%s", addonName);
			ConfigMapGuiVO targetCm = configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), targetConfigmapName);
			if(targetCm != null) {
				cluster.setNamespaceName(this.getCocktailAddonNamespace());
				configMapService.deleteConfigMap(cluster, this.getCocktailAddonNamespace(), targetConfigmapName);
				log.debug("##### nginx ingress configmap removal ended successfully. : " + targetConfigmapName);
			}
			else {
				log.debug("##### nginx ingress configmap not found. : " + targetConfigmapName);
			}
		}

		/**
		 * logs-promtail Addon의 경우 uninstall시에 발급된 토큰 제거 처리 추가 필요
		 */
		if(StringUtils.equalsIgnoreCase("logs-promtail", Optional.ofNullable(currAddon.getLabels()).orElseGet(Maps::newHashMap).get(KubeConstants.LABELS_ADDON_CHART_KEY))) {
			logAgentService.removeAddonLogAgentToken(addonName, cluster);
		}

		/**
		 * istio Addon의 경우 uninstall시에 istio-system에 설치된 Secret을 삭제 해 주어야 함...
		 */
//		if(StringUtils.equalsIgnoreCase("istio", Optional.ofNullable(currAddon.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_CHART_KEY))) {
//			String targetSecretName = "istio-ingressgateway-certs";
//			//TODO : istio-system Namespace 이름을 설정 파일로 이동 필요..
//			SecretGuiVO targetSecret = k8sResourceService.getSecret(clusterSeq, "istio-system", targetSecretName);
//			if(targetSecret != null) {
//				cluster.setNamespaceName(cocktailAddonProperties.getAddonNamespace());
//				k8sResourceService.deleteSecret(clusterSeq, "istio-system", targetSecretName);
//				log.debug("##### istio-ingressgateway-certs secret removal ended successfully. : " + targetSecretName);
//			}
//			else {
//				log.debug("##### istio-ingressgateway-certs secret not found. : " + targetSecretName);
//			}
//		}

	}

	/**
	 * Cluster Access Authorization Secret 만료
	 * @param clusterAuthSeq
	 * @param clusterSeq
	 * @param ownerType
	 * @param checkCluster
	 * @return
	 * @throws Exception
	 */
	public boolean expireClusterAccessSecret(Integer clusterAuthSeq, Integer clusterSeq, String ownerType, boolean checkCluster) throws Exception {
		try {
			// expire auth key
			clusterAccessAuthService.expireClusterAccessSecret(clusterAuthSeq, clusterSeq, ownerType, checkCluster);
		}
		catch (Exception e) {
			log.warn("fail expire authkKey");
			return false;
		}

		return true;
	}

	/**
	 * addon 삭제는 useYn=N로 변경하면 삭제가 되고
	 * 배포되었던 리소스는 삭제됨
	 *
	 * @param clusterSeq
	 * @param addonName
	 * @throws Exception
	 */
	public ConfigMapGuiVO deleteAddonV1(Integer clusterSeq, String addonName) throws Exception {

		ConfigMapGuiVO currAddon = configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), addonName);

		if (currAddon != null) {
			List<JsonObject> patchBody = new ArrayList<>();

			Map<String, Object> patchMap = new HashMap<>();
			patchMap.put("op", JsonPatchOp.REPLACE.getValue());
			patchMap.put("path", String.format("/data/%s", AddonKeyItem.USE_YN.getValue()));
			patchMap.put("value", "N");
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());

			AddonUtils.setAddonStatus(patchBody, "DELETED");
			AddonUtils.setAddonUpdateAt(patchBody);

			configMapService.patchConfigMap(clusterSeq, this.getCocktailAddonNamespace(), currAddon.getName(), patchBody);
		} else {
			throw new CocktailException("addon configMap does not exist !!", ExceptionType.K8sConfigMapNotFound);
		}

		return currAddon;
	}

	public void rollbackAddon(Integer clusterSeq, String addonName) throws Exception {

		ConfigMapGuiVO currAddon = configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), addonName);

		if (currAddon != null) {

			if (currAddon.getData().containsKey(AddonKeyItem.VALUE_YAML_PREV.getValue())) {
				List<JsonObject> patchBody = new ArrayList<>();

				Map<String, Object> patchMap = new HashMap<>();
				patchMap.put("op", JsonPatchOp.REPLACE.getValue());
				patchMap.put("path", String.format("/data/%s", AddonKeyItem.VALUE_YAML.getValue()));
				patchMap.put("value", currAddon.getData().get(AddonKeyItem.VALUE_YAML_PREV.getValue()));
				patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());

				patchMap = new HashMap<>();
				patchMap.put("op", JsonPatchOp.REMOVE.getValue());
				patchMap.put("path", String.format("/data/%s", AddonKeyItem.VALUE_YAML_PREV.getValue()));
				patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());

				AddonUtils.setAddonStatus(patchBody, KubeConstants.ADDON_STATUS_PENDING_ROLLBACK);
				AddonUtils.setAddonUpdateAt(patchBody);

				configMapService.patchConfigMap(clusterSeq, this.getCocktailAddonNamespace(), currAddon.getName(), patchBody);
			} else {
				throw new CocktailException("The old addon information for rollback does not exist!!", ExceptionType.DoesNotExistsTheOldAddonInfoForRollback);
			}
		} else {
			throw new CocktailException("addon configMap does not exist !!", ExceptionType.K8sConfigMapNotFound);
		}
	}

	@Async
	public void redeployAddon(Integer clusterSeq, String addonName) throws Exception {

		ConfigMapGuiVO currAddon = configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), addonName);

		if (currAddon != null) {
			/**
			 * useYn = N 처리 후 5 sec 기다린 후,
			 * 다시 useYn = Y 로 변경하여 재생성하도록 함.
			 */
			List<JsonObject> patchBody = new ArrayList<>();
			Map<String, Object> patchMap = new HashMap<>();
			if (MapUtils.isNotEmpty(currAddon.getData())
					&& BooleanUtils.toBoolean(currAddon.getData().get(AddonKeyItem.USE_YN.getValue()))) {
				patchMap.put("op", JsonPatchOp.REPLACE.getValue());
				patchMap.put("path", String.format("/data/%s", AddonKeyItem.USE_YN.getValue()));
				patchMap.put("value", "N");
				patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
				AddonUtils.setAddonStatus(patchBody, "DELETED");
				AddonUtils.setAddonUpdateAt(patchBody);
				configMapService.patchConfigMap(clusterSeq, this.getCocktailAddonNamespace(), currAddon.getName(), patchBody);
				Thread.sleep(5000);
			}

			patchBody = new ArrayList<>();
			patchMap = new HashMap<>();
			patchMap.put("op", JsonPatchOp.REPLACE.getValue());
			patchMap.put("path", String.format("/data/%s", AddonKeyItem.USE_YN.getValue()));
			patchMap.put("value", "Y");
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
			AddonUtils.setAddonStatus(patchBody, KubeConstants.ADDON_STATUS_PENDING_UPGRADE);
			AddonUtils.setAddonUpdateAt(patchBody);
			configMapService.patchConfigMap(clusterSeq, this.getCocktailAddonNamespace(), currAddon.getName(), patchBody);
		} else {
			throw new CocktailException("addon configMap does not exist !!", ExceptionType.K8sConfigMapNotFound);
		}
	}


	/**
	 * Get Cluster Addon의 Manifest String.
	 * @param clusterSeq
	 * @param addonName
	 * @return
	 * @throws Exception
	 */
	public ConfigMapGuiVO getClusterAddonConfig(Integer clusterSeq, String addonName) throws Exception {
		if (clusterSeq != null) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
			ClusterVO cluster = clusterDao.getCluster(clusterSeq);

			if (cluster != null) {
				return configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), addonName);
			}
		}

		return null;
	}

	public String getCocktailAddonNamespace() throws Exception {
		return this.addonCommonService.getCocktailAddonNamespace();
	}

	/**
	 * Cluster Addon의 리소스 상세 정보 조회
	 * @param clusterSeq
	 * @param addonName
	 * @return
	 * @throws Exception
	 */
	public ClusterAddonVO getClusterAddonResourceByManifest(Integer clusterSeq, String addonName) throws Exception {
		ClusterAddonVO addon = new ClusterAddonVO();
		ConfigMapGuiVO config = this.getClusterAddonConfig(clusterSeq, addonName);
		if (config != null) {
			String namespace = Optional.ofNullable(config.getData()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.ADDON_NAMESPACE_KEY); // 실제 설치 namespace 는 releaseNamespace에 있음
			if (StringUtils.isBlank(namespace)) {
				namespace = this.getCocktailAddonNamespace();
			}

			String base64EncodedString = Optional.ofNullable(config.getData()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.ADDON_MANIFEST_KEY);
			String manifestString = Utils.decompressGZipFromString(base64EncodedString, true, "UTF-8"); // Base64 Encode + Gzip Data를 Manifest String으로 변환.

			if(manifestString == null) {
				if(log.isDebugEnabled()) {
					log.info("==============================================");
					log.info("get Addon Resources From Manifest failure!!111");
					log.info("==============================================");
				}
				/** Manifest 정보가 없으면 기존 로직을 태워서 리소스 정보를 조회 함 **/
				return this.getClusterAddonResource(clusterSeq, addonName);
			}

			HelmResourcesVO helmResources = packageK8sService.getHelmResourcesFromManifest(manifestString, clusterSeq, null, namespace, null);

			if(helmResources == null) {
				if(log.isDebugEnabled()) {
					log.info("==============================================");
					log.info("get Addon Resources From Manifest failure!!222");
					log.info("==============================================");
				}
				/** Manifest로 조회에 실패하면 기존 로직을 태워서 다시 한번 조회 함 **/
				return this.getClusterAddonResource(clusterSeq, addonName);
			}

			if(log.isDebugEnabled()) {
				log.debug("==============================================");
				log.debug("get Addon Resources From Manifest succeed!!!!!");
				log.debug("==============================================");
			}

			/**
			 * 각 Resource별로 셋팅
			 */
			ClusterAddonControllerVO addonController = new ClusterAddonControllerVO();

			if(helmResources.getControllers() != null) {
				addonController.setDeployments(helmResources.getControllers().getDeployments());
				addonController.setReplicaSets(helmResources.getControllers().getReplicaSets());
				addonController.setStatefulSets(helmResources.getControllers().getStatefulSets());
				addonController.setDaemonSets(helmResources.getControllers().getDaemonSets());
				addonController.setCronJobs(helmResources.getControllers().getCronJobs());
				addonController.setJobs(helmResources.getControllers().getJobs());
			}
			addon.setController(addonController);

			ClusterAddonServiceVO addonService = new ClusterAddonServiceVO();
			addonService.setServices(helmResources.getServices());
			addonService.setIngresses(helmResources.getIngresses());
			addon.setService(addonService);

			ClusterAddonConfigVO addonConfig = new ClusterAddonConfigVO();
			addonConfig.setConfigMaps(helmResources.getConfigMaps());
			addonConfig.setSecrets(helmResources.getSecrets());
			addon.setConfig(addonConfig);

			addon.setPods(helmResources.getPods());
			addon.setVolumes(helmResources.getVolumes());
		} else {
			log.info("===============================================================");
			log.info("get Addon Resources From Manifest failure!!(not found config)");
			log.info("===============================================================");
		}

		return addon;

	}

	/**
	 * Cluster Addon의 Resources 조회
	 * @param clusterSeq
	 * @param addonName
	 * @return
	 * @throws Exception
	 */
	public ClusterAddonVO getClusterAddonResource(Integer clusterSeq, String addonName) throws Exception {

		ClusterAddonVO clusterAddon = new ClusterAddonVO();

		if (clusterSeq != null) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
			ClusterVO cluster =  clusterDao.getCluster(clusterSeq);

			if (cluster != null) {
				ConfigMapGuiVO addon = configMapService.getConfigMap(clusterSeq, this.getCocktailAddonNamespace(), addonName);

				if (addon != null) {
					String namespace = addon.getData().get(KubeConstants.ADDON_NAMESPACE_KEY); // 실제 설치 namespace 는 releaseNamespace에 있음

					if (StringUtils.isBlank(namespace)) {
						namespace = this.getCocktailAddonNamespace();
					}

					String label = String.format("%s=%s,%s=%s", KubeConstants.LABELS_ADDON_INSTANCE_KEY, addon.getName(), KubeConstants.LABELS_ADDON_NAME_KEY, addon.getName());

					/** Event **/
					List<K8sEventVO> events = k8sResourceService.getEventByCluster(clusterSeq, namespace, null ,null, ContextHolder.exeContext());
					Map<String, Map<String, List<K8sEventVO>>> eventMap = new HashMap<>();
					if (CollectionUtils.isNotEmpty(events)) {
						for (K8sEventVO eventRow : events) {
							if (!eventMap.containsKey(eventRow.getKind())) {
								eventMap.put(eventRow.getKind(), Maps.newHashMap());
							}
							if (!eventMap.get(eventRow.getKind()).containsKey(eventRow.getName())) {
								eventMap.get(eventRow.getKind()).put(eventRow.getName(), Lists.newArrayList());
							}

							eventMap.get(eventRow.getKind()).get(eventRow.getName()).add(eventRow);
						}
					}

					/** Pod **/
					List<K8sPodVO> pods = new ArrayList<>();
					List<K8sPodVO> allPods = workloadResourceService.getPods(clusterSeq, null, namespace, null, ContextHolder.exeContext());
					Map<String, List<K8sPodVO>> podMap = new HashMap<>();
					addonCommonService.getPodToMap(allPods, podMap, MapUtils.getObject(eventMap, K8sApiKindType.POD.getValue(), Maps.newHashMap()));

					/** ReplicaSet **/
					List<K8sReplicaSetVO> replicaSets = new ArrayList<>();
					List<K8sReplicaSetVO> allRelicaSets = workloadResourceService.convertReplicaSetDataList(cluster, namespace, null, null);
					Map<String, List<K8sReplicaSetVO>> replicaSetMap = new HashMap<>();
					addonCommonService.getReplicaSetToMap(allRelicaSets, replicaSetMap, MapUtils.getObject(eventMap, K8sApiKindType.REPLICA_SET.getValue(), Maps.newHashMap()));

					/** Deployment **/
					List<K8sDeploymentVO> deployments = new ArrayList<>();
					List<K8sDeploymentVO> allDeployments = workloadResourceService.getDeployments(cluster, namespace, null, null, ContextHolder.exeContext());
					this.setDeployment(allDeployments, deployments, MapUtils.getObject(eventMap, K8sApiKindType.DEPLOYMENT.getValue(), Maps.newHashMap()), addon.getName());
					/** Set replicaSet owner pod **/
					for (K8sDeploymentVO k8sDeploymentRow : deployments) {
						if (CollectionUtils.isNotEmpty(k8sDeploymentRow.getNewReplicaSets())) {
							addonCommonService.addPod(pods, podMap, k8sDeploymentRow.getNewReplicaSets().get(0).getName());
						}
						if (CollectionUtils.isNotEmpty(k8sDeploymentRow.getOldReplicaSets())) {
							addonCommonService.addPod(pods, podMap, k8sDeploymentRow.getOldReplicaSets().get(0).getName());
						}
						// Set replicaSet
						replicaSets.addAll(replicaSetMap.get(k8sDeploymentRow.getName()));
					}

					/** DaemonSet **/
					List<K8sDaemonSetVO> daemonSets = workloadResourceService.getDaemonSets(cluster, namespace, null, label, ContextHolder.exeContext());
					for (K8sDaemonSetVO k8sDaemonSetRow : daemonSets) {
						k8sDaemonSetRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.DAEMON_SET.getValue(), Maps.newHashMap()).get(k8sDaemonSetRow.getName()));
						addonCommonService.addPod(pods, podMap, k8sDaemonSetRow.getName());
					}

					/** StatefulSet **/
					List<K8sStatefulSetVO> statefulSets = workloadResourceService.getStatefulSets(cluster, namespace, null, label, ContextHolder.exeContext());
					for (K8sStatefulSetVO k8sStatefulSetRow : statefulSets) {
						k8sStatefulSetRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.STATEFUL_SET.getValue(), Maps.newHashMap()).get(k8sStatefulSetRow.getName()));
						addonCommonService.addPod(pods, podMap, k8sStatefulSetRow.getName());
					}

					/** CronJob **/
					List<K8sCronJobVO> cronJobs = workloadResourceService.getCronJobs(cluster, namespace, null, label, ContextHolder.exeContext());

					/** Job **/
					List<K8sJobVO> jobs = new ArrayList<>();
					List<K8sJobVO> allJobs = workloadResourceService.getJobs(cluster, namespace, null, CollectionUtils.isNotEmpty(cronJobs) ? null : label, ContextHolder.exeContext());

					if (CollectionUtils.isNotEmpty(cronJobs)) {
						Map<String, List<K8sJobVO>> jobMap = new HashMap<>();
						// 기본 라벨로 조회되는 job은 jobs에 셋팅
						addonCommonService.getJobToMap(allJobs, jobs, jobMap, MapUtils.getObject(eventMap, K8sApiKindType.JOB.getValue(), Maps.newHashMap()), addon.getName());

						// CronJob이 owner인 job을 jobs에 셋팅
						for (K8sCronJobVO k8sCronJobRow : cronJobs) {
							k8sCronJobRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.CRON_JOB.getValue(), Maps.newHashMap()).get(k8sCronJobRow.getName()));
							addonCommonService.addJob(jobs, jobMap, k8sCronJobRow.getName());
						}
					} else {
						jobs.addAll(allJobs);
					}
					for (K8sJobVO k8sJobRow : jobs) {
						addonCommonService.addPod(pods, podMap, k8sJobRow.getName());
					}

					/** Service **/
					List<K8sServiceVO> services = serviceSpecService.getServices(cluster, namespace, null, label, ContextHolder.exeContext());

					/** Ingress **/
					String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.META_NAMESPACE, namespace);
					List<K8sIngressVO> ingresses = ingressSpecService.getIngresses(cluster, field, label, ContextHolder.exeContext());

					/** ConfigMap **/
					List<ConfigMapGuiVO> configMaps = configMapService.getConfigMaps(cluster, namespace, null, label);

					/** Secret **/
					List<SecretGuiVO> secrets = secretService.getSecrets(cluster, namespace, null, label, true);

					/** PVC **/
					List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(cluster, namespace, null, label, ContextHolder.exeContext());
					/** PV **/
					Map<String, K8sPersistentVolumeVO> persistentVolumesMap = persistentVolumeService.getPersistentVolumesMap(cluster, namespace, null, null, ContextHolder.exeContext());
					/** StorageClass **/
					Map<String, K8sStorageClassVO> storageClassMap = storageClassService.convertStorageClassDataMap(cluster, null, null, ContextHolder.exeContext());

					if (CollectionUtils.isNotEmpty(persistentVolumeClaims)) {
						for (K8sPersistentVolumeClaimVO k8sPersistentVolumeClaimRow : persistentVolumeClaims) {
							// Set PersistentVolume
							k8sPersistentVolumeClaimRow.setPersistentVolume(persistentVolumesMap.get(k8sPersistentVolumeClaimRow.getVolumeName()));
							// Set StorageClass
							k8sPersistentVolumeClaimRow.setStorageClass(storageClassMap.get(k8sPersistentVolumeClaimRow.getStorageClassName()));
						}
					}

					/**
					 * 각 Resource별로 셋팅
					 */
					ClusterAddonControllerVO addonController = new ClusterAddonControllerVO();
					addonController.setDeployments(deployments);
					addonController.setReplicaSets(replicaSets);
					addonController.setStatefulSets(statefulSets);
					addonController.setDaemonSets(daemonSets);
					addonController.setCronJobs(cronJobs);
					addonController.setJobs(jobs);
					clusterAddon.setController(addonController);

					ClusterAddonServiceVO addonService = new ClusterAddonServiceVO();
					addonService.setServices(services);
					addonService.setIngresses(ingresses);
					clusterAddon.setService(addonService);

					ClusterAddonConfigVO addonConfig = new ClusterAddonConfigVO();
					addonConfig.setConfigMaps(configMaps);
					addonConfig.setSecrets(secrets);
					clusterAddon.setConfig(addonConfig);

					clusterAddon.setPods(pods);
					clusterAddon.setVolumes(persistentVolumeClaims);
				}
			}
		}

		return clusterAddon;
	}


	protected void setDeployment(List<K8sDeploymentVO> allDeployments, List<K8sDeploymentVO> deployments, Map<String, List<K8sEventVO>> eventMap, String addonName){
		if(CollectionUtils.isNotEmpty(allDeployments)){
			for(K8sDeploymentVO deploymentRow : allDeployments){
				if (StringUtils.equals(MapUtils.getString(deploymentRow.getLabels(), KubeConstants.LABELS_ADDON_INSTANCE_KEY, ""), addonName)
						&& StringUtils.equals(MapUtils.getString(deploymentRow.getLabels(), KubeConstants.LABELS_ADDON_NAME_KEY, ""), addonName) ) {
					deploymentRow.setEvents(eventMap.get(deploymentRow.getName()));
					deployments.add(deploymentRow);
				}
			}
		}
	}

	public List<String> getAddonNamesByServicemap(Integer servicemapSeq, String labels) throws Exception {

		ClusterVO cluster =  k8sResourceService.setupCluster(servicemapSeq);
		return this.getAddonNames(cluster, labels);

	}
	public List<String> getAddonNames(Integer clusterSeq, String labels) throws Exception {

		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster =  clusterDao.getCluster(clusterSeq);
		return this.getAddonNames(cluster, labels);

	}

	public List<String> getAddonNames(ClusterVO cluster, String labels) throws Exception {
		List<ConfigMapGuiVO> addons = addonCommonService.getAddonConfigMaps(cluster, labels);

		if (CollectionUtils.isNotEmpty(addons)) {
			return addons.stream().filter(a -> (BooleanUtils.toBoolean(a.getData().get(AddonKeyItem.USE_YN.getValue())))).map(a -> (a.getLabels().get(KubeConstants.LABELS_ADDON_CHART_KEY))).collect(Collectors.toList());
		}

		return null;
	}

//	/**
//	 * Monitoring Agent의 Data로 사용되는 AddonConfigMapVO 생성
//	 * @param updateCluster
//	 * @param cluster
//	 * @param monitoringReleaseId
//	 * @param authKey
//	 * @return
//	 * @throws Exception
//	 */
//	private AddonConfigMapVO generateMonitoringAddonConfigMap(ClusterAddVO updateCluster, ClusterVO cluster, String monitoringReleaseId, String authKey) throws Exception {
//		AddonConfigMapVO valueObj = new AddonConfigMapVO();
//		// Prometheus / Alertmanager url suffix 정보를 addon configMap release-id로 얻음
//		List<V1ConfigMap> addonConfigMaps = k8sWorker.getConfigMapsV1(cluster, this.getCocktailAddonNamespace(), null, AddonUtils.addonLabel());
//		if (CollectionUtils.isNotEmpty(addonConfigMaps)) {
//			/** AS-IS 처리 : addon ConfigMap이 존재하면 AS-IS 처리...**/
//			V1ConfigMap addonConfigMap = addonConfigMaps.get(0);
//			valueObj.setPrometheus_url(AddonUtils.getAddonPrometheusUrl(addonConfigMap.getMetadata().getLabels().get(KubeConstants.LABELS_HELM_CHART_KEY)));
//			valueObj.setAlertmanager_url(AddonUtils.getAddonAlertmanagerUrl(addonConfigMap.getMetadata().getLabels().get(KubeConstants.LABELS_HELM_CHART_KEY)));
//		}
//		else {
//			/** TO-BE 처리 : Addon 자동 설치 **/
//			if(!this.isAddonAutoInstallation(updateCluster)) { // Addon 자동설치가 아니면.. : AS-IS 케이스는 Exception 처리함...
//				throw new CocktailException("Has not addon configMap!", ExceptionType.K8sConfigMapCreationFail);
//			}
//			valueObj.setPrometheus_url(AddonUtils.getAutoAddonPrometheusUrl(monitoringReleaseId));
//			valueObj.setAlertmanager_url(AddonUtils.getAutoAddonAlertmanagerUrl(monitoringReleaseId));
//		}
//
//		valueObj.setCluster_type(updateCluster.getCubeType().isKaas() ? "kaas" : "small");
//		valueObj.setCollector_server_url(cocktailAddonProperties.getMonitoringCollectorUrlProxy());
//		valueObj.setMonitor_api_url(cocktailAddonProperties.getMonitoringApiUrlProxy());
//		valueObj.setBase64_monitoring_secret(Base64Utils.encodeToString(authKey.getBytes("UTF-8")));
//		valueObj.setBase64_cluster_id(Base64Utils.encodeToString(updateCluster.getClusterId().getBytes("UTF-8")));
//		valueObj.setBase64_cluster_seq(Base64Utils.encodeToString(String.valueOf(updateCluster.getClusterSeq().intValue()).getBytes("UTF-8")));
//		AddonConfigMapImageVO valueImageObj = new AddonConfigMapImageVO();
//		valueImageObj.setRegistry_url(cocktailAddonProperties.getAcloudRegistryUrl());
//		valueObj.setImage(valueImageObj);
//
//		return valueObj;
//	}

	public List<ConfigMapGuiVO> getAddonConfigMaps(ClusterVO cluster) throws Exception {
		return this.addonCommonService.getAddonConfigMaps(cluster);
	}
	public List<ConfigMapGuiVO> getAddonConfigMaps(String clusterId, String labels, String useYn) throws Exception {
		return this.addonCommonService.getAddonConfigMaps(clusterId, labels, useYn);
	}
	public List<ConfigMapGuiVO> getAddonConfigMapsByChartType(Integer clusterSeq, String chartType, String useYn, boolean useResource, boolean useIngress) throws Exception {
		String labels = ResourceUtil.commonAddonSearchLabel(chartType);
		return this.getAddonConfigMaps(clusterSeq, labels, useYn, useResource, useIngress);
	}
	public List<ConfigMapGuiVO> getAddonConfigMapsByChartType(ClusterVO cluster, String chartType, String useYn, boolean useResource, boolean useIngress) throws Exception {
		String labels = ResourceUtil.commonAddonSearchLabel(chartType);
		return this.getAddonConfigMaps(cluster, labels, useYn, useResource, useIngress);
	}
	public List<ConfigMapGuiVO> getAddonConfigMaps(Integer clusterSeq, String labels, String useYn, boolean useResource, boolean useIngress) throws Exception {
		return this.addonCommonService.getAddonConfigMaps(clusterSeq, labels, useYn, useResource, useIngress);
	}
	public List<ConfigMapGuiVO> getAddonConfigMaps(ClusterVO cluster, String labels, String useYn, boolean useResource, boolean useIngress) throws Exception {
		return this.addonCommonService.getAddonConfigMaps(cluster, labels, useYn, useResource, useIngress);
	}

	/**
	 * Create Namespace (없으면...)
	 * @param cluster
	 * @param namespace
	 * @throws Exception
	 */
	private void createNamespaceIfNotExist(ClusterVO cluster, String namespace) throws Exception {
		/** Namespace가 존재하지 않으면 생성 **/
		V1Namespace v1Namespace = namespaceService.getV1Namespace(cluster, namespace);

		if(v1Namespace == null) {
			namespaceService.createNamespace(cluster, namespace, null);
		}
	}


	/**
	 * Addon 기본 정보에 대한 Validation Check.
	 * @param addonInstall  Addon Installation VO
	 * @param addonName     Addon 이름 (Helm의 Release name)
	 * @throws Exception
	 */
	public void addonDefaultValidation(AddonInstallVO addonInstall, String addonName) throws Exception {
		this.addonDefaultValidation(addonInstall, addonName, ApiVersionType.V2);
	}

	/**
	 * Addon 기본 정보에 대한 Validation Check.
	 * @param addonInstall      Addon Installation VO
	 * @param addonName         Addon 이름 (Helm의 Release name)
	 * @param  apiVersionType   Api Version
	 * @throws Exception
	 */
	public void addonDefaultValidation(AddonInstallVO addonInstall, String addonName, ApiVersionType apiVersionType) throws Exception {
		if (addonInstall == null) {
			throw new CocktailException("Request Body is null!!", ExceptionType.InvalidParameter_Empty);
		}
		else if (StringUtils.isBlank(addonInstall.getReleaseName())) {
			throw new CocktailException("release Name is required", ExceptionType.AddonReleaseNameMissing);
		}
		else if (StringUtils.isBlank(addonInstall.getName())) {
			throw new CocktailException("chart Name is required", ExceptionType.AddonChartNameMissing);
		}
		else if (StringUtils.isBlank(addonInstall.getVersion())) {
			throw new CocktailException("chart Version is required", ExceptionType.AddonChartVersionMissing);
		}
		else if (!StringUtils.equals(addonInstall.getReleaseName(), addonName)) {
			throw new CocktailException("Invalid release Name (The releaseName entered in RequestBody is different.)", ExceptionType.InvalidAddonName_FixedName);
		}
		else if (StringUtils.isNotBlank(addonInstall.getAddonToml())) {
			if(!this.isValidToml(addonInstall.getAddonToml())) {
				throw new CocktailException("Invalid Toml", ExceptionType.InvalidTomlData);
			}
		}
//		else if (apiVersionType == ApiVersionType.V3 && StringUtils.isNotBlank(addonInstall.getAddonYaml())) {
		else if (StringUtils.isNotBlank(addonInstall.getAddonYaml())) {
			if(!this.isValidYaml(addonInstall.getAddonYaml())) {
				throw new CocktailException("Invalid Yaml", ExceptionType.InvalidYamlData);
			}
		}
	}

	/**
	 * Toml 유효성 체크.
	 * @param toml toml string
	 * @return
	 * @throws Exception
	 */
	public boolean isValidToml(String toml) {
		try {
			new Toml().read(toml);
			return true;
		}
		catch (Exception ex) {
			// invalid toml
			return false;
		}
	}

	/**
	 * Yaml 유효성 체크.
	 * @param yaml yaml string
	 * @return
	 * @throws Exception
	 */
	public boolean isValidYaml(String yaml) {
		try {
			Yaml.getSnakeYaml(null).load(yaml);
			return true;
		}
		catch (Exception ex) {
			// invalid yaml
			return false;
		}
	}

}