package run.acloud.api.catalog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.kubernetes.client.openapi.models.*;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.catalog.dao.ITemplateMapper;
import run.acloud.api.catalog.enums.LaunchType;
import run.acloud.api.catalog.enums.TemplateDeploymentType;
import run.acloud.api.catalog.vo.*;
import run.acloud.api.configuration.constants.AddonConstants;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.AddonCommonService;
import run.acloud.api.configuration.service.ClusterVolumeService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.configuration.vo.RegistryProjectVO;
import run.acloud.api.configuration.vo.ServiceDetailVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.dao.IWorkloadGroupMapper;
import run.acloud.api.cserver.enums.*;
import run.acloud.api.cserver.service.*;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.k8sextended.models.*;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.dao.IComponentMapper;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.util.K8sJsonUtils;
import run.acloud.api.resource.util.K8sMapperUtils;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.DataType;
import run.acloud.commons.enums.HarborRegistryProjectMemberRole;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.HarborProjectMemberVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io Created on 2017. 1. 10.
 */
@Slf4j
@Service
public class TemplateService {

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private ServicemapService servicemapService;

	@Autowired
	private ServerService serverService;

	@Autowired
	private ServerValidService serverValidService;

	@Autowired
	private ServerConversionService serverConversionService;

	@Autowired
	private ClusterVolumeService clusterVolumeService;

	@Autowired
	private CRDResourceService crdResourceService;

	@Autowired
	private K8sResourceService k8sResourceService;

	@Autowired
	private IngressSpecService ingressSpecService;

	@Autowired
	private ServiceSpecService serviceSpecService;

	@Autowired
	private HarborRegistryFactoryService harborRegistryFactory;

	@Autowired
	private RegistryPropertyService registryPropertyService;

	@Autowired
	private PersistentVolumeService persistentVolumeService;

	@Autowired
	private AddonCommonService addonCommonService;

	@Autowired
	private PackageService packageService;

	@Autowired
	private PackageInfoService packageInfoService;

	@Autowired
	private PackageValidService packageValidService;

	@Autowired
	private PackageAsyncService packageAsyncService;

	@Autowired
	private WorkloadGroupService workloadGroupService;

	@Autowired
	private ConfigMapService configMapService;

	@Autowired
	private SecretService secretService;

	@Autowired
	private RBACResourceService rbacResourceService;

	@Autowired
	private WorkloadResourceService workloadResourceService;

	/** ================================================================================================================
	 * Start : Change template format to YAML type
	 =============================================================================================================== **/

	/**
	 * masking Unusable values in metadata
	 * @param objectMeta
	 * @throws Exception
	 */
	private void maskingUnusableMetadata(V1ObjectMeta objectMeta) {
		if (objectMeta != null) {
			this.maskingUnusableValues(objectMeta);
			this.maskingUnusableLabelsAndAnnotations(objectMeta);
		}
	}

	private void maskingUnusableMetadata(Map<String, Object> objectMetaMap) {
		if (MapUtils.isNotEmpty(objectMetaMap)) {
			this.maskingUnusableValues(objectMetaMap);
			this.maskingUnusableLabelsAndAnnotations(objectMetaMap);
		}
	}

	/**
	 * masking Unusable Values : for V1ObjectMeta
	 * @param objectMeta
	 * @throws Exception
	 */
	private void maskingUnusableValues(V1ObjectMeta objectMeta) {
		if(objectMeta == null) return;

		objectMeta.setNamespace(null);
		objectMeta.setCreationTimestamp(null);
		objectMeta.setResourceVersion(null);
		objectMeta.setUid(null);
		objectMeta.setSelfLink(null);

		objectMeta.setFinalizers(null);
	}

	/**
	 * masking Unusable Values : for Map<String, Object> type Metadata
	 * @param objectMetaMap
	 * @throws Exception
	 */
	private void maskingUnusableValues(Map<String, Object> objectMetaMap) {
		if(MapUtils.isEmpty(objectMetaMap)) return;

		objectMetaMap.remove("namespace");
		objectMetaMap.remove("creationTimestamp");
		objectMetaMap.remove("resourceVersion");
		objectMetaMap.remove("setUid");
		objectMetaMap.remove("selfLink");

		// for CustomObject
		objectMetaMap.remove("uid");
		objectMetaMap.remove("generation");
	}

	/**
	 * masking Unusable Values : for Workload Labels and Annotations
	 * @param objectMeta
	 * @throws Exception
	 */
	private void maskingUnusableLabelsAndAnnotations(V1ObjectMeta objectMeta) {
		if(objectMeta == null) return;

		Map<String, String> labels = objectMeta.getLabels();
		Map<String, String> annotations = objectMeta.getAnnotations();

		this.maskingUnusableLabelsAndAnnotations(labels, annotations);
	}

	/**
	 * masking Unusable Values : for Workload Labels and Annotations
	 * @param objectMetaMap
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void maskingUnusableLabelsAndAnnotations(Map<String, Object> objectMetaMap) {
		if(MapUtils.isEmpty(objectMetaMap)) return;

		Map<String, String> labels = (Map<String, String>)objectMetaMap.get(KubeConstants.META_LABELS);
		Map<String, String> annotations = (Map<String, String>)objectMetaMap.get(KubeConstants.META_ANNOTATIONS);

		this.maskingUnusableLabelsAndAnnotations(labels, annotations);
	}

	private void maskingUnusableLabelsAndAnnotations(Map<String, String> labels, Map<String, String> annotations) {
		if(MapUtils.isNotEmpty(labels)) {
			labels.remove(KubeConstants.LABELS_COCKTAIL_KEY);
			labels.remove(KubeConstants.CUSTOM_VOLUME_TYPE);
		}
		if (MapUtils.isNotEmpty(annotations)) {
			annotations.remove(KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION);
			annotations.remove(KubeConstants.META_ANNOTATIONS_DEPRECATED_DAEMONSET_TEMPLATE_GENERATION);
			annotations.remove(KubeConstants.META_ANNOTATIONS_LAST_APPLIED_CONFIGURATION);
			annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
		}
	}

	/**
	 * Gate of add Template
	 * @param apiVersion
	 * @param templateAdd
	 * @return
	 * @throws Exception
	 */
	public int addTemplate(String apiVersion, TemplateAddVO templateAdd) throws Exception {
		try {
			switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
				case V2:
					return this.addTemplateV2(templateAdd);
				default:
					throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
			}
		}
		catch (Exception ex) {
			if(log.isDebugEnabled()) log.debug("trace log ", ex);
			throw new CocktailException(String.format("An error occurred during template registration. [%s]", ex.getMessage()), ex, ExceptionType.TemplateRegistrationFail);
		}
	}

	/**
	 * add Template with YAML
	 * @param templateAdd
	 * @return
	 * @throws Exception
	 */
	@Transactional(transactionManager = "transactionManager")
	public int addTemplateV2(TemplateAddVO templateAdd) throws Exception {
		/**
		 * Step 1 : Insert into DB (templates, template_version)
		 */
		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);
		if (this.addTemplateVersion(templateAdd, dao) < 1) {
			throw new CocktailException("An error occurred during template registration.", ExceptionType.TemplateRegistrationFail);
		}

		/**
		 * Step.2 : Insert into DB (template_deployment)
		 */

		/** 2-0 : Set Basic Information **/
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		IComponentMapper componentDao = sqlSession.getMapper(IComponentMapper.class);

		ServicemapDetailVO servicemapDetail = servicemapDao.getServicemapDetail(templateAdd.getServicemapSeq(), templateAdd.getServiceSeq());

        List<String> excludePackageResourceLogger = new ArrayList<>();

        if (servicemapDetail != null) {
            ServiceDetailVO service = serviceDao.getService(templateAdd.getServiceSeq());
            ClusterVO cluster = this._getClusterInfoByServicemap("템플릿 생성 중 오류가 발생하였습니다. %s", templateAdd.getServicemapSeq());
            templateAdd.setAccountSeq(cluster.getAccount().getAccountSeq());
            templateAdd.setServiceSeq(service.getServiceSeq());

            /** 2-1 : Package **/
            // Package 목록
            List<HelmReleaseBaseVO> packageList = packageInfoService.getPackages(cluster.getClusterSeq(), cluster.getNamespaceName(), null);
            // Package로 배포된 리소스는 스냅샷으로 저장하지 않기 위함.
            List<String> pkgWorkloads = new ArrayList<>();
            List<String> pkgServices = new ArrayList<>();
            List<String> pkgIngress = new ArrayList<>();
            List<String> pkgConfigMaps = new ArrayList<>();
            List<String> pkgSecrets = new ArrayList<>();
            List<String> pkgPvcs = new ArrayList<>();
            List<String> pkgServiceAccounts = new ArrayList<>();
            List<String> pkgRoles = new ArrayList<>();
            List<String> pkgRoleBindings = new ArrayList<>();
            List<Pair<String, String>> pkgCustomObjects = new ArrayList<>();
            // StatefulSet의 VolumeTemplate으로 배포된 PVC는 PVC Snapshot에 포함하지 않음.
            List<String> usedInTheVolumeTemplates = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(packageList)) {
                int sortOrder = 1;
                int runOrder = 1;
                for (HelmReleaseBaseVO pkg : packageList) {
                    // TODO : Manifest를 조회하기 위해 일일이 조회해야 함.. Package Service 담당자와 협의 후 리스트로 한번에 받을수 있도록 개선 필요함..
                    HelmReleaseBaseVO packageDetail = packageService.getPackageStatus(pkg.getClusterSeq(), pkg.getNamespace(), pkg.getName(), null);

                    // Manifest 내용을 Parsing 하여 Package에서 생성된 리소스들은 이후 처리에서 스냅샷으로 저장되지 않도록 함..
                    Iterable<Object> iterable = Yaml.getSnakeYaml().loadAll(packageDetail.getManifest());
                    List<Object> objs = Lists.newArrayList();
                    for (Object object : iterable) {
                        if (object != null) {
                            objs.add(object);
                        }
                    }
                    for (Object obj : objs) {
                        Map<String, Object> k8sObjMap = ServerUtils.getK8sObjectToMap(obj, null);
                        String kindString = MapUtils.getString(k8sObjMap, "kind");
                        if(StringUtils.isBlank(kindString)) {
                            // 이런 경우는 없을 것 같은데.. 일단 로그 남김..
                            log.error("Kind not found (Add Template) : " + JsonUtils.toGson(obj));
                            continue;
                        }
                        K8sApiKindType kind = K8sApiKindType.findKindTypeByValue(kindString);
                        if(kind == null) {
                            kind = K8sApiKindType.UNSUPPORTED_RESOURCE;
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> meta = (Map<String, Object>) MapUtils.getMap(k8sObjMap, KubeConstants.META, null);
                        String name = MapUtils.getString(meta, KubeConstants.NAME);
                        /* Resource List Logging.................... */
                        log.debug("====================================================\n{} : {} : {} : {}\n----------------------------------------------\n{}"
                            , MapUtils.getString(k8sObjMap, KubeConstants.APIVSERION)
                            , kind
                            , MapUtils.getString(meta, KubeConstants.NAME)
                            , MapUtils.getString(meta, KubeConstants.META_NAMESPACE)
                            , JsonUtils.toPrettyString(obj)
                        );
                        switch (kind) {
                            case DEPLOYMENT:
                            case STATEFUL_SET:
                            case DAEMON_SET:
                            case CRON_JOB:
                            case JOB:
                                pkgWorkloads.add(name);
                                break;
                            case SERVICE:
                                pkgServices.add(name);
                                break;
                            case INGRESS:
                                pkgIngress.add(name);
                                break;
                            case CONFIG_MAP:
                                pkgConfigMaps.add(name);
                                break;
                            case SECRET:
                                pkgSecrets.add(name);
                                break;
                            case PERSISTENT_VOLUME_CLAIM:
                                pkgPvcs.add(name);
                                break;
                            case SERVICE_ACCOUNT:
                                pkgServiceAccounts.add(name);
                                break;
                            case ROLE:
                                pkgRoles.add(name);
                                break;
                            case ROLE_BINDING:
                                pkgRoleBindings.add(name);
                                break;
                            case UNSUPPORTED_RESOURCE:          // Cocktail에서 아직 지원하지 않는 리소스 유형 -> Custom Resource로 판단하고 처리함..
                                pkgCustomObjects.add(Pair.of(kindString, name));
                                break;
                            case NETWORK_ATTACHMENT_DEFINITION: // Custom Resource (위 Unsupported Resource)에서 공통으로 처리..
                            case ENDPOINTS:                     // Cocktail에서 서비스만 구성 가능 함.
                            case POD:                           // Cocktail에서 단독으로 생성/수정 지원하지 않음.
                            case HORIZONTAL_POD_AUTOSCALER:     // 워크로드에 포함되어 걸러짐.
                            case REPLICA_SET:                   // 워크로드에 포함되어 걸러짐.
                            case CUSTOM_RESOURCE_DEFINITION:    // 스냅샷 미지원 : Custom Resource Definition 지원 안함.
                            case STORAGE_CLASS:                 // 스냅샷 미지원 : Namespace 종속적이지 않음.
                            case PERSISTENT_VOLUME:             // 스냅샷 미지원 : Namespace 종속적이지 않음.
                            case CLUSTER_ROLE:                  // 스냅샷 미지원 : Namespace 종속적이지 않음.
                            case CLUSTER_ROLE_BINDING:          // 스냅샷 미지원 : Namespace 종속적이지 않음.
                            default: {
                                /** TODO: Support 되지 않는 Type은 Error Logging 처리하여 향후 지원하도록 한다. **/
                                if(log.isDebugEnabled()) {
                                    log.warn("Unsupported Resources (Add Template) : " + kind.getValue());
                                }
                                break;
                            }
                        }
                    }

                    HelmInstallRequestVO helmInstallRequest = new HelmInstallRequestVO();
                    helmInstallRequest.setRepo(packageDetail.getRepo());
                    helmInstallRequest.setChartName(packageDetail.getChartName());
                    helmInstallRequest.setVersion(packageDetail.getChartVersion());
                    helmInstallRequest.setReleaseName(packageDetail.getName());
                    helmInstallRequest.setValues(packageDetail.getValues());
                    helmInstallRequest.setLaunchType(LaunchType.ADD.getType());

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.PACKAGE,
                        templateAdd.getTemplateVersionSeq(), JsonUtils.toGson(helmInstallRequest), sortOrder++, runOrder++);
                    // Insert role into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }

            /** 2-2 : Role **/
            List<V1Role> v1Roles = rbacResourceService.getRolesV1(cluster, cluster.getNamespaceName(), null, null);

            if (CollectionUtils.isNotEmpty(v1Roles)) {
                int sortOrder = 1;
                int runOrder = 1;
                for (V1Role roleRow : v1Roles) {
                    if (roleRow != null) {
                        String name = Optional.ofNullable(roleRow).map(V1Role::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"");
                        if(pkgRoles.contains(name)) {
                            excludePackageResourceLogger.add(StringUtils.defaultString(roleRow.getKind()) + " : " + name);
                            continue; // Package에서 생성한 리소스이면 Skip
                        }
                        this.maskingUnusableMetadata(roleRow.getMetadata());

                        TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.ROLE,
                                templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(roleRow), sortOrder++, runOrder++);
                        // Insert role into DB (template_deployment) with YAML
                        dao.addTemplateDeployment(templateDeployment);
                    }
                }
            }

            /** 2-3 : RoleBinding **/
            List<V1RoleBinding> v1RoleBindings = rbacResourceService.getRoleBindingsV1(cluster, cluster.getNamespaceName(), null, null);

            if (CollectionUtils.isNotEmpty(v1RoleBindings)) {
                int sortOrder = 1;
                int runOrder = 1;
                for (V1RoleBinding roleBindingRow : v1RoleBindings) {
                    if(pkgRoleBindings.contains(Optional.ofNullable(roleBindingRow).map(V1RoleBinding::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->""))) {
                        excludePackageResourceLogger.add(roleBindingRow.getKind() + " : " + roleBindingRow.getMetadata().getName());
                        continue; // Package에서 생성한 리소스이면 Skip
                    }
                    this.maskingUnusableMetadata(roleBindingRow.getMetadata());

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.ROLE_BINDING,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(roleBindingRow), sortOrder++, runOrder++);
                    // Insert role binding into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }

            /** 2-4 : ServiceAccount **/
            List<V1ServiceAccount> v1ServiceAccounts = rbacResourceService.getServiceAccountsV1(cluster, cluster.getNamespaceName(), null, null);

            if (CollectionUtils.isNotEmpty(v1RoleBindings)) {
                int sortOrder = 1;
                int runOrder = 1;
                for (V1ServiceAccount serviceAccountRow : v1ServiceAccounts) {
                    if(pkgServiceAccounts.contains(serviceAccountRow.getMetadata().getName())) {
                        excludePackageResourceLogger.add(serviceAccountRow.getKind() + " : " + serviceAccountRow.getMetadata().getName());
                        continue; // Package에서 생성한 리소스이면 Skip
                    }
                    // Default Service Account Filtering..
                    if(StringUtils.equals(serviceAccountRow.getMetadata().getName(), KubeConstants.DEFAULT_SERVICE_ACCOUNT)) {
                        continue;
                    }
                    this.maskingUnusableMetadata(serviceAccountRow.getMetadata());

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.SERVICE_ACCOUNT,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(serviceAccountRow), sortOrder++, runOrder++);
                    // Insert service account into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }

            /** 2-5 : configmap **/
            List<V1ConfigMap> configMaps = configMapService.getConfigMapsV1(cluster, cluster.getNamespaceName(), null, null);

            if (CollectionUtils.isNotEmpty(configMaps)) {
                int sortOrder = 1;
                int runOrder = 1;
                for (V1ConfigMap configMapRow : configMaps) {
                    if(pkgConfigMaps.contains(configMapRow.getMetadata().getName())) {
                        excludePackageResourceLogger.add(configMapRow.getKind() + " : " + configMapRow.getMetadata().getName());
                        continue; // Package에서 생성한 리소스이면 Skip
                    }
                    this.maskingUnusableValues(configMapRow.getMetadata());
                    configMapRow.getMetadata().setLabels(null);
                    configMapRow.getMetadata().setAnnotations(null);

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.CONFIG_MAP,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(configMapRow), sortOrder++, runOrder++);
                    // Insert configmap into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }

            /** 2-6 : Secret **/
            List<V1Secret> secrets = secretService.getSecretsV1(cluster, cluster.getNamespaceName(), null, null, true);

            if (CollectionUtils.isNotEmpty(secrets)) {
                int sortOrder = 1;
                int runOrder = 1;
                for (V1Secret secretRow : secrets) {
                    if(pkgSecrets.contains(secretRow.getMetadata().getName())) {
                        excludePackageResourceLogger.add(secretRow.getKind() + " : " + secretRow.getMetadata().getName());
                        continue; // Package에서 생성한 리소스이면 Skip
                    }
                    /** R3.5 : Secret Data List 구성시 Cocktail에서 Support하는 Type에 대해서만 조회하도록 처리하였음..
                     * TODO : Secret 조회 범위 확정 후 처리 필요. **/
                    boolean isSkip = true;
                    Set<String> supportedSecretType = SecretType.getSupportedSecretTypesValue();
                    if (secretRow.getMetadata().getLabels() != null && secretRow.getMetadata().getLabels().containsKey(KubeConstants.LABELS_SECRET)) {
                        if(supportedSecretType.contains(secretRow.getType())) {
                            isSkip = false;
                        }
                    }
                    if(supportedSecretType.contains(secretRow.getType())) {
                        isSkip = false;
                    }
    //				Cocktail에서 Support하던 아니던 일단 다 넣는 걸로 진행함.. 2020.02.17
    //				if(isSkip) {
    //					continue;
    //				}

                    /**
                     * 2020.03.03 : ServiceAccount 생성시 자동으로 구성되는 Token : "kubernetes.io/service-account-token" Type은 제외하도록 한다.
                     * - Package에서 생성한 Manifest 정보에 없어 Snapshot으로 저장되면 패키지 배포시 오류를 유발..
                     * - And... ServiceAccount에 종속적이므로 필요 없을 것으로 판단됨..
                     * **/
                    if(StringUtils.equalsIgnoreCase(secretRow.getType(), KubeConstants.SECRET_TYPE_SERVICE_ACCOUNT_TOKEN)) {
                        continue;
                    }

                    this.maskingUnusableValues(secretRow.getMetadata());
                    secretRow.getMetadata().setLabels(null);
                    secretRow.getMetadata().setAnnotations(null);

                    /** Secret 저장시 데이터는 삭제 **/
                    if(!templateAdd.isIsd()) {
                        Map<String, byte[]> dataMap = new HashMap<>();
                        if (MapUtils.isNotEmpty(secretRow.getData())) {
                            for (Map.Entry<String, byte[]> dataEntry : secretRow.getData().entrySet()) {
                                dataMap.put(dataEntry.getKey(), "".getBytes());
                            }
                        }
                        secretRow.setData(dataMap);
                    }

                    /** Secret 저장 **/
                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.SECRET,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(secretRow), sortOrder++, runOrder++);
                    // Insert Secret into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }

            /** 2-7 : Custom Resource Definition **/
            // Map<CRD name, Map<CRD version, List<CustomObject Map>>>
            Map<String, Map<String, List<Map<String, Object>>>> customObjectMap = this.getCustomObjectOfCRD(cluster, cluster.getNamespaceName());
            if (MapUtils.isNotEmpty(customObjectMap)) {
                int sortOrder = 1;
                int runOrder = 1;
                int networkSortOrder = 1;
                int networkRunOrder = 1;
                for (Map.Entry<String, Map<String, List<Map<String, Object>>>> crdEntry : customObjectMap.entrySet()) {
                    for (Map.Entry<String, List<Map<String, Object>>> coEntry : crdEntry.getValue().entrySet()) {
                        for(Map<String, Object> customObjectRow : Optional.ofNullable(coEntry.getValue()).orElseGet(() ->Lists.newArrayList())) {
                            boolean generatedInThePackage = false;
                            for(Pair<String, String> pr : pkgCustomObjects) {
                                Map<String, Object> meta = (Map<String, Object>)customObjectRow.get(KubeConstants.META);
                                if(MapUtils.getString(customObjectRow, KubeConstants.KIND, "").equals(pr.getLeft()) &&
                                        MapUtils.getString(meta, KubeConstants.NAME, "").equals(pr.getRight())) {
                                    generatedInThePackage = true;
                                    excludePackageResourceLogger.add(pr.getLeft() + " : " + pr.getRight());
                                    break; // Package에서 생성한 리소스이면 Skip
                                }
                            }
                            if(generatedInThePackage) {
                                continue; // Package에서 생성한 리소스이면 Skip
                            }

                            Map<String, Object> metadata = (Map<String, Object>)customObjectRow.get(KubeConstants.META);
                            this.maskingUnusableMetadata(metadata);

                            if(StringUtils.equals(MapUtils.getString(customObjectRow, KubeConstants.KIND, ""), K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION.getValue())) {
                                TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.NET_ATTACH_DEF,
                                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(customObjectRow), networkSortOrder++, networkRunOrder++);
                                // Insert [Network Attachment Definition] into DB (template_deployment) with YAML
                                dao.addTemplateDeployment(templateDeployment);
                            }
                            else {
                                TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.CUSTOM_OBJECT,
                                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(customObjectRow), sortOrder++, runOrder++);
                                // Insert [Custom Object] into DB (template_deployment) with YAML
                                dao.addTemplateDeployment(templateDeployment);
                            }
                        }
                    }
                }
            }


//          위 Custom Resource Definition 조회에서 함께 처리하도록 함..
//    		/** 2-6 : Network Attachment Definition **/
//    		List<Map<String, Object>> netAttachDefs = k8sResourceService.getCustomObjects(cluster, appmap.getNamespaceName(), K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, null);
//
//    		if (CollectionUtils.isNotEmpty(netAttachDefs)) {
//    			int sortOrder = 1;
//    			int runOrder = 1;
//    			for (Map<String, Object> netAttachDefRow : netAttachDefs) {
//    				Map<String, Object> metadata = (Map<String, Object>)netAttachDefRow.get(KubeConstants.META);
//    				this.maskingUnusableValues(metadata);
//    				metadata.put(KubeConstants.META_LABELS, null);
//    				// metadata.put("annotations", null); // Annotation은 유지..
//
//    				TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.NET_ATTACH_DEF,
//    					templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(netAttachDefRow), sortOrder++, runOrder++);
//    				// Insert [Network Attachment Definition] into DB (template_deployment) with YAML
//    				dao.addTemplateDeployment(templateDeployment);
//    			}
//    		}

            /** 2-8 : Horizontal Pod Autoscaler for Workloads**/
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
            List<V1HorizontalPodAutoscaler> v1HorizontalPodAutoscalers = null;
            List<V2beta1HorizontalPodAutoscaler> v2beta1HorizontalPodAutoscalers = null;
            List<V2beta2HorizontalPodAutoscaler> v2beta2HorizontalPodAutoscalers = null;
            List<V2HorizontalPodAutoscaler> v2HorizontalPodAutoscalers = null;
            if (apiVerKindType != null) {
                if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V1){
                    v1HorizontalPodAutoscalers = workloadResourceService.getHorizontalPodAutoscalersV1(cluster, cluster.getNamespaceName(), null, null);
                    for (V1HorizontalPodAutoscaler hpaRow : v1HorizontalPodAutoscalers) {
                        this.maskingUnusableMetadata(hpaRow.getMetadata());
                        hpaRow.setStatus(null);
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA1) {
                    v2beta1HorizontalPodAutoscalers = workloadResourceService.getHorizontalPodAutoscalersV2beta1(cluster, cluster.getNamespaceName(), null, null);
                    for (V2beta1HorizontalPodAutoscaler hpaRow : v2beta1HorizontalPodAutoscalers) {
                        this.maskingUnusableMetadata(hpaRow.getMetadata());
                        hpaRow.setStatus(null);
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA2) {
                    v2beta2HorizontalPodAutoscalers = workloadResourceService.getHorizontalPodAutoscalersV2beta2(cluster, cluster.getNamespaceName(), null, null);
                    for (V2beta2HorizontalPodAutoscaler hpaRow : v2beta2HorizontalPodAutoscalers) {
                        this.maskingUnusableMetadata(hpaRow.getMetadata());
                        hpaRow.setStatus(null);
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2) {
                    v2HorizontalPodAutoscalers = workloadResourceService.getHorizontalPodAutoscalersV2(cluster, cluster.getNamespaceName(), null, null);
                    for (V2HorizontalPodAutoscaler hpaRow : v2HorizontalPodAutoscalers) {
                        this.maskingUnusableMetadata(hpaRow.getMetadata());
                        hpaRow.setStatus(null);
                    }
                }
            }
            Map<String, List<? extends Object>>  hpas = new HashMap<>();
            hpas.put("v1", v1HorizontalPodAutoscalers);
            hpas.put("v2beta1", v2beta1HorizontalPodAutoscalers);
            hpas.put("v2beta2", v2beta2HorizontalPodAutoscalers);
            hpas.put("v2", v2HorizontalPodAutoscalers);

            List<V1Service> services = new ArrayList<>(); // Stop된 워크로드에 존재하는 서비스를 찾기 위해 먼저 선언함..
            /** 2-9 : Workloads **/
            { // 2-9 Workloads Block.
                String registryUrl = ResourceUtil.getRegistryUrl();
                templateAdd.setTemplateDeploymentType(TemplateDeploymentType.DEPLOYMENT);

                Map<String, Map<String, ?>> k8sResourceMap = workloadResourceService.getWorkloadResource(
                    cluster, cluster.getNamespaceName(),
                    null, null,
                    Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);

                int runOrder = 1;
                Set<String> workloadNames = Sets.newHashSet();
                /** 2-9-1 : k8s에서 조회한 workload를 저장 **/
                if (MapUtils.isNotEmpty(k8sResourceMap)) {
                    for (Map.Entry<String, Map<String, ?>> k8sKindEntryRow : k8sResourceMap.entrySet()) {
                        K8sApiKindType kindType = K8sApiKindType.findKindTypeByValue(k8sKindEntryRow.getKey());
                        if (kindType != null && kindType.isWorkload() && MapUtils.isNotEmpty(k8sKindEntryRow.getValue())) {
                            switch (kindType) {
                                case DEPLOYMENT:
                                    templateAdd.setTemplateDeploymentType(TemplateDeploymentType.DEPLOYMENT);
                                    break;
                                case STATEFUL_SET:
                                    templateAdd.setTemplateDeploymentType(TemplateDeploymentType.STATEFUL_SET);
                                    break;
                                case DAEMON_SET:
                                    templateAdd.setTemplateDeploymentType(TemplateDeploymentType.DAEMON_SET);
                                    break;
                                case JOB:
                                    templateAdd.setTemplateDeploymentType(TemplateDeploymentType.JOB);
                                    break;
                                case CRON_JOB:
                                    templateAdd.setTemplateDeploymentType(TemplateDeploymentType.CRON_JOB);
                                    break;
                                default:
                                    continue;
                            }

                            for (Map.Entry<String, ?> k8sWorklodEntryRow : k8sKindEntryRow.getValue().entrySet()) {
                                if(pkgWorkloads.contains(k8sWorklodEntryRow.getKey())) {
                                    excludePackageResourceLogger.add(templateAdd.getTemplateDeploymentType() + " : " + k8sWorklodEntryRow.getKey());
                                    continue; // Package에서 생성한 리소스이면 Skip
                                }
                                TemplateDeploymentVO templateDeployment = new TemplateDeploymentVO();
                                templateDeployment.setTemplateVersionSeq(templateAdd.getTemplateVersionSeq());
                                templateDeployment.setTemplateDeploymentType(templateAdd.getTemplateDeploymentType());

                                // addTemplateWorkload
                                this.addTemplateWorkloadProcess(templateDeployment, templateAdd.getTemplateDeploymentType(), k8sWorklodEntryRow.getValue(), runOrder++, hpas, dao, usedInTheVolumeTemplates);

                                // 중복 체크용..
                                workloadNames.add(k8sWorklodEntryRow.getKey());
                            }
                        }
                    }
                }

                /** 2-9-2 : stateCode in (STOPPED, ERROR) 인 경우 DB에서 조회하여 workload를 저장 **/
                List<ComponentVO> components = componentDao.getComponentsInAppmapByClusterAndNames(cluster.getClusterSeq(), cluster.getNamespaceName(), null);
                if (CollectionUtils.isNotEmpty(components)) {
                    for (ComponentVO componentRow : components) {
                        if(pkgWorkloads.contains(componentRow.getComponentName())) {
                            excludePackageResourceLogger.add(componentRow.getWorkloadType() + " : " + componentRow.getComponentName());
                            continue; // Package에서 생성한 리소스이면 Skip
                        }
                        if (StringUtils.isBlank(componentRow.getWorkloadManifest())) {
                            continue;
                        }
                        if (!workloadNames.contains(componentRow.getComponentName()) && StringUtils.isNotBlank(componentRow.getStateCode())
                            && (StateCode.valueOf(componentRow.getStateCode()) == StateCode.STOPPED || StateCode.valueOf(componentRow.getStateCode()) == StateCode.ERROR)) {
                            TemplateDeploymentVO templateDeployment = new TemplateDeploymentVO();
                            templateDeployment.setTemplateVersionSeq(templateAdd.getTemplateVersionSeq());

                            Object k8sObj = null;

                            /** Workload 설정 **/
                            switch (WorkloadType.valueOf(componentRow.getWorkloadType())) {
                                case SINGLE_SERVER:
                                case REPLICA_SERVER:
                                    templateDeployment.setTemplateDeploymentType(TemplateDeploymentType.DEPLOYMENT);
                                    k8sObj = ServerUtils.unmarshalYaml(componentRow.getWorkloadManifest(), K8sApiKindType.DEPLOYMENT);
                                    break;
                                case STATEFUL_SET_SERVER:
                                    templateDeployment.setTemplateDeploymentType(TemplateDeploymentType.STATEFUL_SET);
                                    k8sObj = ServerUtils.unmarshalYaml(componentRow.getWorkloadManifest(), K8sApiKindType.STATEFUL_SET);
                                    break;
                                case DAEMON_SET_SERVER:
                                    templateDeployment.setTemplateDeploymentType(TemplateDeploymentType.DAEMON_SET);
                                    k8sObj = ServerUtils.unmarshalYaml(componentRow.getWorkloadManifest(), K8sApiKindType.DAEMON_SET);
                                    break;
                                case JOB_SERVER:
                                    templateDeployment.setTemplateDeploymentType(TemplateDeploymentType.JOB);
                                    k8sObj = ServerUtils.unmarshalYaml(componentRow.getWorkloadManifest(), K8sApiKindType.JOB);
                                    break;
                                case CRON_JOB_SERVER:
                                    templateDeployment.setTemplateDeploymentType(TemplateDeploymentType.CRON_JOB);
                                    k8sObj = ServerUtils.unmarshalYaml(componentRow.getWorkloadManifest(), K8sApiKindType.CRON_JOB);
                                    break;
                                default:
                                    continue;
                            }

                            /** 관리되는 유형의 워크로드가 존재하면 Autoscaler 설정 (없을시 위 switch 문에서 continue 시킴... **/
                            switch (WorkloadType.valueOf(componentRow.getWorkloadType())) {
                                case SINGLE_SERVER:
                                case REPLICA_SERVER:
                                case STATEFUL_SET_SERVER:
                                    Object hpaObj = ServerUtils.unmarshalYaml(componentRow.getWorkloadManifest(), K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
                                    if(hpaObj != null) { // Autoscaler가 존재하면 설정..
                                        if(hpaObj instanceof V1HorizontalPodAutoscaler) {
                                            V1HorizontalPodAutoscaler hpaRow = (V1HorizontalPodAutoscaler) hpaObj;
                                            this.maskingUnusableMetadata(hpaRow.getMetadata());
                                            hpaRow.setStatus(null);

                                            List<V1HorizontalPodAutoscaler> hpaList = null;
                                            if(hpas.get("v1") == null) {
                                                hpaList = new ArrayList<>();
                                            }
                                            else {
                                                hpaList = (List<V1HorizontalPodAutoscaler>)hpas.get("v1");
                                            }
                                            hpaList.add(hpaRow);
                                            hpas.put("v1", hpaList);
                                        }
                                        if(hpaObj instanceof V2beta1HorizontalPodAutoscaler) {
                                            V2beta1HorizontalPodAutoscaler hpaRow = (V2beta1HorizontalPodAutoscaler) hpaObj;
                                            this.maskingUnusableMetadata(hpaRow.getMetadata());
                                            hpaRow.setStatus(null);

                                            List<V2beta1HorizontalPodAutoscaler> hpaList = null;
                                            if(hpas.get("v2beta1") == null) {
                                                hpaList = new ArrayList<>();
                                            }
                                            else {
                                                hpaList = (List<V2beta1HorizontalPodAutoscaler>)hpas.get("v2beta1");
                                            }
                                            hpaList.add(hpaRow);
                                            hpas.put("v2beta1", hpaList);
                                        }
                                        if(hpaObj instanceof V2beta2HorizontalPodAutoscaler) {
                                            V2beta2HorizontalPodAutoscaler hpaRow = (V2beta2HorizontalPodAutoscaler) hpaObj;
                                            this.maskingUnusableMetadata(hpaRow.getMetadata());
                                            hpaRow.setStatus(null);

                                            List<V2beta2HorizontalPodAutoscaler> hpaList = null;
                                            if(hpas.get("v2beta2") == null) {
                                                hpaList = new ArrayList<>();
                                            }
                                            else {
                                                hpaList = (List<V2beta2HorizontalPodAutoscaler>)hpas.get("v2beta2");
                                            }
                                            hpaList.add(hpaRow);
                                            hpas.put("v2beta2", hpaList);
                                        }
                                        if(hpaObj instanceof V2HorizontalPodAutoscaler) {
                                            V2HorizontalPodAutoscaler hpaRow = (V2HorizontalPodAutoscaler) hpaObj;
                                            this.maskingUnusableMetadata(hpaRow.getMetadata());
                                            hpaRow.setStatus(null);

                                            List<V2HorizontalPodAutoscaler> hpaList = null;
                                            if(hpas.get("v2") == null) {
                                                hpaList = new ArrayList<>();
                                            }
                                            else {
                                                hpaList = (List<V2HorizontalPodAutoscaler>)hpas.get("v2");
                                            }
                                            hpaList.add(hpaRow);
                                            hpas.put("v2", hpaList);
                                        }
                                    }
                                    break;
                                default:
                                    break;
                            }

                            /** 워크로드 안에 서비스가 있으면 서비스 리스트에 추가하여 아래 2-6 서비스 등록시 함께 추가될 수 있도록 한다. **/
                            V1Service serviceInWorkload = ServerUtils.unmarshalYaml(componentRow.getWorkloadManifest(), K8sApiKindType.SERVICE);
                            if(serviceInWorkload != null) {
                                services.add(serviceInWorkload);
                            }
                            // addTemplateWorkload
                            if (k8sObj != null) {
                                this.addTemplateWorkloadProcess(templateDeployment, templateDeployment.getTemplateDeploymentType(), k8sObj, runOrder++, hpas, dao, usedInTheVolumeTemplates);
                            }

                            // 중복 체크용..
                            workloadNames.add(componentRow.getComponentName());
                        }
                    }
                }
            }

            /** 2-? : Horizontal Pod Autoscaler
             * // Deployment Workload에 포함되어 저장되도록 수정..
             *---------------------------------------------
            K8sApiVerKindType apiVerKindType = k8sResourceService.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
            if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V1){
                List<V1HorizontalPodAutoscaler> v1HorizontalPodAutoscalers = k8sResourceService.getHorizontalPodAutoscalersV1(cluster, appmap.getNamespaceName(), null, null);
                int sortOrder = 1;
                int runOrder = 1;
                for (V1HorizontalPodAutoscaler hpaRow : v1HorizontalPodAutoscalers) {
                    this.maskingUnusableMetadata(hpaRow.getMetadata());
                    hpaRow.setStatus(null);

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.HORIZONTAL_POD_AUTOSCALER,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(hpaRow), sortOrder++, runOrder++);
                    // Insert Horizontal Pod Autoscaler into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }
            else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA1) {
                List<V2beta1HorizontalPodAutoscaler> v2beta1HorizontalPodAutoscalers = k8sResourceService.getHorizontalPodAutoscalersV2beta1(cluster, appmap.getNamespaceName(), null, null);
                int sortOrder = 1;
                int runOrder = 1;
                for (V2beta1HorizontalPodAutoscaler hpaRow : v2beta1HorizontalPodAutoscalers) {
                    this.maskingUnusableMetadata(hpaRow.getMetadata());
                    hpaRow.setStatus(null);

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.HORIZONTAL_POD_AUTOSCALER,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(hpaRow), sortOrder++, runOrder++);
                    // Insert Horizontal Pod Autoscaler into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }
            else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA2) {
                List<V2beta2HorizontalPodAutoscaler> v2beta2HorizontalPodAutoscalers = k8sResourceService.getHorizontalPodAutoscalersV2beta2(cluster, appmap.getNamespaceName(), null, null);
                int sortOrder = 1;
                int runOrder = 1;
                for (V2beta2HorizontalPodAutoscaler hpaRow : v2beta2HorizontalPodAutoscalers) {
                    this.maskingUnusableMetadata(hpaRow.getMetadata());
                    hpaRow.setStatus(null);

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.HORIZONTAL_POD_AUTOSCALER,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(hpaRow), sortOrder++, runOrder++);
                    // Insert Horizontal Pod Autoscaler into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }
            **/

            /** 2-10 : Persistent Volume Claim **/
            List<V1PersistentVolumeClaim> pvcs = persistentVolumeService.getPersistentVolumeClaimsV1(cluster, cluster.getNamespaceName(), null, null);

            if (CollectionUtils.isNotEmpty(pvcs)) {
                int sortOrder = 1;
                int runOrder = 1;
                for (V1PersistentVolumeClaim pvcRow : pvcs) {
                    String pvcName = pvcRow.getMetadata().getName();
                    if(pkgPvcs.contains(pvcName)) {
                        excludePackageResourceLogger.add(pvcRow.getKind() + " : " + pvcName);
                        continue; // Package에서 생성한 리소스이면 Skip
                    }

                    String pvcNameWithoutSuffix = String.format("%s-", StringUtils.substringBeforeLast(pvcRow.getMetadata().getName(), "-"));
                    if(usedInTheVolumeTemplates.contains(pvcNameWithoutSuffix)) {
                        continue; // StatefulSet의 volumeClaimTemplate으로 생성한 리소스이면 Skip
                    }

                    pvcRow.getSpec().setVolumeName(null);       // volumeName은 저장하지 않음.
                    pvcRow.getSpec().setVolumeMode(null);       // volumeMode는 저장하지 않음.
                    pvcRow.setStatus(null);                     // status는 저장하지 않음.
                    pvcRow.getMetadata().setAnnotations(null);  // Annotation은 저장하지 않음.
                    this.maskingUnusableMetadata(pvcRow.getMetadata());

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.PERSISTENT_VOLUME_CLAIM,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(pvcRow), sortOrder++, runOrder++);
                    // Insert Persistent Volume Claim into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }


            /** 2-11 : Service **/
            services.addAll(serviceSpecService.getServicesV1(cluster, cluster.getNamespaceName(), null, null, ContextHolder.exeContext()));

            if (CollectionUtils.isNotEmpty(services)) {
                int sortOrder = 1;
                int runOrder = 1;
                for (V1Service serviceRow : services) {
                    if(pkgServices.contains(serviceRow.getMetadata().getName())) {
                        excludePackageResourceLogger.add(serviceRow.getKind() + " : " + serviceRow.getMetadata().getName());
                        continue; // Package에서 생성한 리소스이면 Skip
                    }
                    this.maskingUnusableMetadata(serviceRow.getMetadata());
                    if(!(StringUtils.isNotBlank(serviceRow.getSpec().getClusterIP()) && "None".equalsIgnoreCase(serviceRow.getSpec().getClusterIP()))) {
                        // Headless 서비스가 아니면 ClusterIP Null
                        serviceRow.getSpec().setClusterIP(null);
                    }
                    // Node 포트 이면 NodePort 정보 삭제..
                    if(serviceRow.getSpec() != null && KubeConstants.SPEC_TYPE_VALUE_NODE_PORT.equals(serviceRow.getSpec().getType())) {
                        if (CollectionUtils.isNotEmpty(serviceRow.getSpec().getPorts())){
                            for (V1ServicePort port : serviceRow.getSpec().getPorts()) {
                                port.setNodePort(null);
                            }
                        }
                    }
                    serviceRow.setStatus(null);

                    TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.SERVICE,
                        templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(serviceRow), sortOrder++, runOrder++);
                    // Insert Service into DB (template_deployment) with YAML
                    dao.addTemplateDeployment(templateDeployment);
                }
            }

            /** 2-12 : Ingress **/
            K8sApiVerKindType ingressType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);

            if (ingressType != null) {
                if (ingressType.getGroupType() == K8sApiGroupType.NETWORKING && ingressType.getApiType() == K8sApiType.V1) {
                    List<V1Ingress> networkingIngresses = ingressSpecService.getIngressesNetworkingV1(cluster, cluster.getNamespaceName(), null, null);
                    if (CollectionUtils.isNotEmpty(networkingIngresses)) {
                        int sortOrder = 1;
                        int runOrder = 1;
                        for (V1Ingress ingressRow : networkingIngresses) {
                            if (pkgIngress.contains(ingressRow.getMetadata().getName())) {
                                excludePackageResourceLogger.add(ingressRow.getKind() + " : " + ingressRow.getMetadata().getName());
                                continue; // Package에서 생성한 리소스이면 Skip
                            }
                            this.maskingUnusableMetadata(ingressRow.getMetadata());
                            ingressRow.setStatus(null);

                            TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.INGRESS, templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(ingressRow), sortOrder++, runOrder++);
                            // Insert Service into DB (template_deployment) with YAML
                            dao.addTemplateDeployment(templateDeployment);
                        }
                    }
                } else if (ingressType.getGroupType() == K8sApiGroupType.NETWORKING && ingressType.getApiType() == K8sApiType.V1BETA1) {
                    List<NetworkingV1beta1Ingress> networkingIngresses = ingressSpecService.getIngressesNetworkingV1Beta1(cluster, cluster.getNamespaceName(), null, null);
                    if (CollectionUtils.isNotEmpty(networkingIngresses)) {
                        int sortOrder = 1;
                        int runOrder = 1;
                        for (NetworkingV1beta1Ingress ingressRow : networkingIngresses) {
                            if (pkgIngress.contains(ingressRow.getMetadata().getName())) {
                                excludePackageResourceLogger.add(ingressRow.getKind() + " : " + ingressRow.getMetadata().getName());
                                continue; // Package에서 생성한 리소스이면 Skip
                            }
                            this.maskingUnusableMetadata(ingressRow.getMetadata());
                            ingressRow.setStatus(null);

                            TemplateDeploymentVO templateDeployment = this.buildTemplateDeployment(TemplateDeploymentType.INGRESS, templateAdd.getTemplateVersionSeq(), ServerUtils.marshalYaml(ingressRow), sortOrder++, runOrder++);
                            // Insert Service into DB (template_deployment) with YAML
                            dao.addTemplateDeployment(templateDeployment);
                        }
                    }
                }
            }
        }

		if(false) {
			throw new CocktailException("for testing........", ExceptionType.CommonFail);
		}
		else {
			log.debug("=================================================================");
			log.debug("Excluded Package Resources.. ------------------------------------");
			log.debug(JsonUtils.toPrettyString(excludePackageResourceLogger));
			log.debug("=================================================================");
		}
		return 1;
	}

	/**
	 * 공통 TemplateDeployment builder
	 * @param templateDeploymentType
	 * @param templateVersionSeq
	 * @param content
	 * @param sortOrder
	 * @param runOrder
	 * @return
	 * @throws Exception
	 */
	private TemplateDeploymentVO buildTemplateDeployment(TemplateDeploymentType templateDeploymentType,
	                                                     Integer templateVersionSeq,
	                                                     String content,
	                                                     int sortOrder,
	                                                     int runOrder) throws Exception
	{
		return this.buildTemplateDeployment(new TemplateDeploymentVO(), templateDeploymentType, templateVersionSeq, content, sortOrder, runOrder);
	}

	/**
	 * 공통 TemplateDeployment builder
	 * @param templateDeployment
	 * @param templateDeploymentType
	 * @param templateVersionSeq
	 * @param content
	 * @param sortOrder
	 * @param runOrder
	 * @return
	 * @throws Exception
	 */
	private TemplateDeploymentVO buildTemplateDeployment(TemplateDeploymentVO templateDeployment,
	                                                     TemplateDeploymentType templateDeploymentType,
	                                                     Integer templateVersionSeq,
	                                                     String content,
	                                                     int sortOrder,
	                                                     int runOrder) throws Exception
	{
		if(templateDeployment == null) {
			templateDeployment = new TemplateDeploymentVO();
		}
		templateDeployment.setTemplateVersionSeq(templateVersionSeq);
		templateDeployment.setTemplateDeploymentType(templateDeploymentType);
		templateDeployment.setTemplateContent(content);
		templateDeployment.setWorkloadGroupName(templateDeploymentType.getCode());
		templateDeployment.setSortOrder(sortOrder);
		templateDeployment.setRunOrder(runOrder);

		return templateDeployment;
	}

	/**
	 * Workload에 대한 TemplateDeployment 구성..
	 * @param templateDeployment
	 * @param templateDeploymentType
	 * @param k8sObj
	 * @param runOrder
	 * @param hpas
	 * @param dao
	 * @throws Exception
	 */
	private void addTemplateWorkloadProcess(TemplateDeploymentVO templateDeployment, TemplateDeploymentType templateDeploymentType, Object k8sObj, int runOrder, Map<String, List<? extends Object>> hpas, ITemplateMapper dao) throws Exception {
		this.addTemplateWorkloadProcess(templateDeployment, templateDeploymentType, k8sObj, runOrder, hpas, dao, null);
	}

	/**
	 * Workload에 대한 TemplateDeployment 구성..
	 *
	 * @param templateDeployment
	 * @param templateDeploymentType
	 * @param k8sObj
	 * @param runOrder
	 * @param hpas
	 * @param dao
	 * @param usedInTheVolumeTemplates
	 * @throws Exception
	 */
	private void addTemplateWorkloadProcess(TemplateDeploymentVO templateDeployment, TemplateDeploymentType templateDeploymentType, Object k8sObj, int runOrder, Map<String, List<? extends Object>> hpas, ITemplateMapper dao, List<String> usedInTheVolumeTemplates) throws Exception {
		if (k8sObj == null) {
			return;
		}
		if (dao == null) {
			dao = sqlSession.getMapper(ITemplateMapper.class);
		}

		V1Deployment deployment = null;
		V1StatefulSet statefulSet = null;
		V1DaemonSet daemonSet = null;
		V1Job job = null;
		V1beta1CronJob cronJobV1beta1 = null;
		V1CronJob cronJobV1 = null;

		V1ObjectMeta objectMeta = null;
		V1PodTemplateSpec podTemplateSpec = null;

		Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(k8sObj, K8sJsonUtils.getJson());
		K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
		K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

		switch(templateDeploymentType) {
			case DEPLOYMENT:
				deployment = (V1Deployment) k8sObj;
				objectMeta = deployment.getMetadata();
				podTemplateSpec = deployment.getSpec().getTemplate();
				break;
			case STATEFUL_SET:
				statefulSet = (V1StatefulSet) k8sObj;
				objectMeta = statefulSet.getMetadata();
				podTemplateSpec = statefulSet.getSpec().getTemplate();
				break;
			case DAEMON_SET:
				daemonSet = (V1DaemonSet) k8sObj;
				objectMeta = daemonSet.getMetadata();
				podTemplateSpec = daemonSet.getSpec().getTemplate();
				break;
			case JOB:
				job = (V1Job) k8sObj;
				objectMeta = job.getMetadata();
				if(objectMeta.getOwnerReferences() != null) {
					for(V1OwnerReference ownerReference : objectMeta.getOwnerReferences()) {
						if("CronJob".equalsIgnoreCase(ownerReference.getKind())) {
							// CronJob에서 생성된 Job일 경우 저장하지 않는다..
							return;
						}
					}
				}
				podTemplateSpec = job.getSpec().getTemplate();
				break;
			case CRON_JOB:
				if (K8sApiType.V1BETA1 == apiType) {
					cronJobV1beta1 = (V1beta1CronJob) k8sObj;
					objectMeta = cronJobV1beta1.getMetadata();
					podTemplateSpec = cronJobV1beta1.getSpec().getJobTemplate().getSpec().getTemplate();
				} else {
					cronJobV1 = (V1CronJob) k8sObj;
					objectMeta = cronJobV1.getMetadata();
					podTemplateSpec = cronJobV1.getSpec().getJobTemplate().getSpec().getTemplate();
				}
				break;
		}

		/** Set GroupName **/
		String workloadGroupName = CommonConstants.DEFAULT_GROUP_NAME;
		Map<String, String> annotations = Optional.ofNullable(objectMeta).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
		if(MapUtils.isNotEmpty(annotations)) {
			Integer groupNo = Utils.getInteger(annotations.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
			WorkloadGroupVO workloadGroup = workloadGroupService.getWorkloadGroup(groupNo);
			if(workloadGroup != null) {
				workloadGroupName = workloadGroup.getWorkloadGroupName();
			}
		}

		templateDeployment.setWorkloadGroupName(workloadGroupName);

		/** Masking Unusable Values and Labels and Annotations **/
		this.maskingUnusableMetadata(objectMeta);

		/** Persistent Volume Claims는 카탈로그로 등록 금지 => EMPTY_DIR로 변환.. **/
//		this.maskingPodTemplateSpec(podTemplateSpec, objectMeta); // 2020.02.18 : PVC도 Snapshot에 포함.

		/** Container and Init container Initialize (해당 없음) **/
		/** Service명이 워크로드명과 상이하면 워크로드명으로 저장 (워크로드에서 생성하는 Service는 사라질 예정이므로 처리하지 않음..) **/

		/** 완료된 워크로드를 Template Contents에 설정 **/
		if(deployment != null) {
			deployment.setStatus(null);
			// 연결된 HPA가 존재하면 추가..
			StringBuffer yamlStr = new StringBuffer();
			yamlStr.append(ServerUtils.marshalYaml(deployment));
			if(hpas.get("v1") != null && CollectionUtils.isNotEmpty(hpas.get("v1"))) {
				for (V1HorizontalPodAutoscaler hpaRow : (List<V1HorizontalPodAutoscaler>)hpas.get("v1")) {
					if (hpaRow.getSpec() != null && hpaRow.getSpec().getScaleTargetRef() != null && hpaRow.getSpec().getScaleTargetRef().getName() != null) {
						if (StringUtils.equals(hpaRow.getSpec().getScaleTargetRef().getName(), deployment.getMetadata().getName())) {
							yamlStr.append("---\n");
							yamlStr.append(ServerUtils.marshalYaml(hpaRow));
						}
					}
				}
			}
			if(hpas.get("v2beta1") != null && CollectionUtils.isNotEmpty(hpas.get("v2beta1"))) {
				for (V2beta1HorizontalPodAutoscaler hpaRow : (List<V2beta1HorizontalPodAutoscaler>)hpas.get("v2beta1")) {
					if (hpaRow.getSpec() != null && hpaRow.getSpec().getScaleTargetRef() != null && hpaRow.getSpec().getScaleTargetRef().getName() != null) {
						if (StringUtils.equals(hpaRow.getSpec().getScaleTargetRef().getName(), deployment.getMetadata().getName())) {
							yamlStr.append("---\n");
							yamlStr.append(ServerUtils.marshalYaml(hpaRow));
						}
					}
				}
			}
			if(hpas.get("v2beta2") != null && CollectionUtils.isNotEmpty(hpas.get("v2beta2"))) {
				for (V2beta2HorizontalPodAutoscaler hpaRow : (List<V2beta2HorizontalPodAutoscaler>)hpas.get("v2beta2")) {
					if (hpaRow.getSpec() != null && hpaRow.getSpec().getScaleTargetRef() != null && hpaRow.getSpec().getScaleTargetRef().getName() != null) {
						if (StringUtils.equals(hpaRow.getSpec().getScaleTargetRef().getName(), deployment.getMetadata().getName())) {
							yamlStr.append("---\n");
							yamlStr.append(ServerUtils.marshalYaml(hpaRow));
						}
					}
				}
			}
			if(hpas.get("v2") != null && CollectionUtils.isNotEmpty(hpas.get("v2"))) {
				for (V2HorizontalPodAutoscaler hpaRow : (List<V2HorizontalPodAutoscaler>)hpas.get("v2")) {
					if (hpaRow.getSpec() != null && hpaRow.getSpec().getScaleTargetRef() != null && hpaRow.getSpec().getScaleTargetRef().getName() != null) {
						if (StringUtils.equals(hpaRow.getSpec().getScaleTargetRef().getName(), deployment.getMetadata().getName())) {
							yamlStr.append("---\n");
							yamlStr.append(ServerUtils.marshalYaml(hpaRow));
						}
					}
				}
			}

			templateDeployment.setTemplateContent(yamlStr.toString());
		}
		else if(statefulSet != null) {
			statefulSet.setStatus(null);
			// 연결된 HPA가 존재하면 추가..
			StringBuffer yamlStr = new StringBuffer();
			yamlStr.append(ServerUtils.marshalYaml(statefulSet));
			if(hpas.get("v1") != null && CollectionUtils.isNotEmpty(hpas.get("v1"))) {
				for (V1HorizontalPodAutoscaler hpaRow : (List<V1HorizontalPodAutoscaler>)hpas.get("v1")) {
					if (hpaRow.getSpec() != null && hpaRow.getSpec().getScaleTargetRef() != null && hpaRow.getSpec().getScaleTargetRef().getName() != null) {
						if (StringUtils.equals(hpaRow.getSpec().getScaleTargetRef().getName(), statefulSet.getMetadata().getName())) {
							yamlStr.append("---\n");
							yamlStr.append(ServerUtils.marshalYaml(hpaRow));
						}
					}
				}
			}
			if(hpas.get("v2beta1") != null && CollectionUtils.isNotEmpty(hpas.get("v2beta1"))) {
				for (V2beta1HorizontalPodAutoscaler hpaRow : (List<V2beta1HorizontalPodAutoscaler>)hpas.get("v2beta1")) {
					if (hpaRow.getSpec() != null && hpaRow.getSpec().getScaleTargetRef() != null && hpaRow.getSpec().getScaleTargetRef().getName() != null) {
						if (StringUtils.equals(hpaRow.getSpec().getScaleTargetRef().getName(), statefulSet.getMetadata().getName())) {
							yamlStr.append("---\n");
							yamlStr.append(ServerUtils.marshalYaml(hpaRow));
						}
					}
				}
			}
			if(hpas.get("v2beta2") != null && CollectionUtils.isNotEmpty(hpas.get("v2beta2"))) {
				for (V2beta2HorizontalPodAutoscaler hpaRow : (List<V2beta2HorizontalPodAutoscaler>)hpas.get("v2beta2")) {
					if (hpaRow.getSpec() != null && hpaRow.getSpec().getScaleTargetRef() != null && hpaRow.getSpec().getScaleTargetRef().getName() != null) {
						if (StringUtils.equals(hpaRow.getSpec().getScaleTargetRef().getName(), statefulSet.getMetadata().getName())) {
							yamlStr.append("---\n");
							yamlStr.append(ServerUtils.marshalYaml(hpaRow));
						}
					}
				}
			}
			if(hpas.get("v2") != null && CollectionUtils.isNotEmpty(hpas.get("v2"))) {
				for (V2HorizontalPodAutoscaler hpaRow : (List<V2HorizontalPodAutoscaler>)hpas.get("v2")) {
					if (hpaRow.getSpec() != null && hpaRow.getSpec().getScaleTargetRef() != null && hpaRow.getSpec().getScaleTargetRef().getName() != null) {
						if (StringUtils.equals(hpaRow.getSpec().getScaleTargetRef().getName(), statefulSet.getMetadata().getName())) {
							yamlStr.append("---\n");
							yamlStr.append(ServerUtils.marshalYaml(hpaRow));
						}
					}
				}
			}
			templateDeployment.setTemplateContent(yamlStr.toString());

			if(CollectionUtils.isNotEmpty(statefulSet.getSpec().getVolumeClaimTemplates())) {
				for(V1PersistentVolumeClaim vct : statefulSet.getSpec().getVolumeClaimTemplates()) {
					if(vct.getMetadata() != null && StringUtils.isNotBlank(vct.getMetadata().getName())) {
						// Statefulset에서 생성한 PVC를 거를 수 있어야 한다.
						String volumeTemplatePrefixName = String.format("%s-%s-", vct.getMetadata().getName(), statefulSet.getMetadata().getName());
						usedInTheVolumeTemplates.add(volumeTemplatePrefixName);
					}
				}
			}
		}
		else if(daemonSet != null) {
			daemonSet.setStatus(null);
			templateDeployment.setTemplateContent(ServerUtils.marshalYaml(daemonSet));
		}
		else if(job != null) {
			job.setStatus(null);
			templateDeployment.setTemplateContent(ServerUtils.marshalYaml(job));
		}
		else if(cronJobV1beta1 != null) {
			cronJobV1beta1.setStatus(null);
			templateDeployment.setTemplateContent(ServerUtils.marshalYaml(cronJobV1beta1));
		}
		else if(cronJobV1 != null) {
			cronJobV1.setStatus(null);
			templateDeployment.setTemplateContent(ServerUtils.marshalYaml(cronJobV1));
		}
		else {
			throw new CocktailException("An error occurred during template registration.[addTemplateWorkloadProcess]", ExceptionType.TemplateRegistrationFail);
		}

		templateDeployment.setSortOrder(runOrder);
		templateDeployment.setRunOrder(runOrder);

		/** Insert Workload into DB (template_deployment) with YAML **/
		int result = dao.addTemplateDeployment(templateDeployment);

		/** if template deployment Fail **/
		if (result < 1) {
			throw new CocktailException("An error occurred during template registration.[addTemplateDeployment result is null]", ExceptionType.TemplateRegistrationFail);
		}
	}

	/**
	 * V1PodTemplateSpec의 Volume 설정 (PVC -> EmptyDir로..)
	 * @param podTemplateSpec
	 * @param objectMeta
	 * @throws Exception
	 */
	private void maskingPodTemplateSpec(V1PodTemplateSpec podTemplateSpec, V1ObjectMeta objectMeta) throws Exception {
		/**
		 * Persistent Volume Claims는 카탈로그로 등록 금지 => EMPTY_DIR로 변환..
		 */
		List<V1Volume> volumes = podTemplateSpec.getSpec().getVolumes();
		if(CollectionUtils.isNotEmpty(volumes)) {
			for (V1Volume volume : volumes) {
				// static Persistent Volume Claims는 카탈로그로 등록 금지 => EMPTY_DIR로 변환..
				if(volume.getPersistentVolumeClaim() != null) {
					// Label에서 Persistent Volume Claim 참조 제거..
					if(StringUtils.isNotEmpty(volume.getPersistentVolumeClaim().getClaimName())) {
						if(objectMeta.getLabels() != null && objectMeta.getLabels().containsKey(volume.getPersistentVolumeClaim().getClaimName())) {
							objectMeta.getLabels().remove(volume.getPersistentVolumeClaim().getClaimName());
						}
					}
					// PVC 지우고 EmptyDir 추가
					volume.setPersistentVolumeClaim(null);
					volume.setEmptyDir(new V1EmptyDirVolumeSource());
				}
			}
		}

		/**
		 * Volume Claim Template Initialize (해당 없음)
		 */
	}


	/**
	 * Gate of Template Detail Inquiry
	 * @param apiVersion
	 * @param templateSeq
	 * @param templateVersionSeq
	 * @param showDeploy
	 * @return
	 */
	public TemplateDetailVO getTemplateDetail(String apiVersion, Integer templateSeq, Integer templateVersionSeq, Boolean showDeploy) {
		try {
			switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
				case V2:
					return this.getTemplateDetail(templateSeq, templateVersionSeq, showDeploy, DataType.YAML);
				default:
					throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
			}
		}
		catch (Exception ex) {
			if(log.isDebugEnabled()) log.debug("trace log ", ex);
			throw new CocktailException(String.format("An error occurred during template detail inquiry. [%s]", ex.getMessage()), ex, ExceptionType.CommonInquireFail);
		}

	}

	public TemplateDetailVO getTemplateDetail(Integer templateSeq, Integer templateVersionSeq, Boolean showDeploy, DataType dataType) throws Exception {
		if(dataType == DataType.JSON) { // JSON Type은 기존 처리 이용.
			return this.getTemplateDetail(templateSeq, templateVersionSeq, showDeploy);
		}
		if(dataType != DataType.YAML) { // YAML & JSON만 지원
			throw new CocktailException(String.format("An error occurred during template detail inquiry. Unsupported data type. [%s]", dataType), ExceptionType.CommonInquireFail);
		}

		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);
		TemplateDetailVO templateDetail = dao.getTemplateDetail(templateSeq, templateVersionSeq);

		// 변환할 DeploymentContents가 없으면 즉시 응답.
		if(templateDetail == null || showDeploy == null || showDeploy.booleanValue() == false) {
			if(templateDetail != null) {
				templateDetail.setTemplateDeployments(null);
			}
			return templateDetail;
		}
		if(CollectionUtils.isEmpty(templateDetail.getTemplateDeployments())){
			return templateDetail;
		}

		if(this.isV1Template(templateDetail)) { // AS-IS 유형 템플릿은 기존 로직 활용..
			return this.getTemplateDetail(templateSeq, templateVersionSeq, showDeploy);
		}


		WorkloadType workloadType = null;
		// DeploymentContents 컨버트.. YAML to GUI
		for(TemplateDeploymentVO templateDeploymentRow : templateDetail.getTemplateDeployments()) {
			if (StringUtils.isBlank(templateDeploymentRow.getTemplateContent())) { // Skip..
				continue;
			}

			if(templateDeploymentRow.getTemplateDeploymentType() == TemplateDeploymentType.PACKAGE) { // PACKAGE는 Contents가 JSON Type으로 저장됨..
				templateDeploymentRow.setTemplateContentJson(templateDeploymentRow.getTemplateContent());
			}
			else {
				templateDeploymentRow.setTemplateContentYaml(templateDeploymentRow.getTemplateContent());
//				templateDeploymentRow.setTemplateContentJson(ServerUtils.convertYamlListToJsonList(templateDeploymentRow.getTemplateContent()));
			}
			workloadType = null;
			switch (templateDeploymentRow.getTemplateDeploymentType()) {
				case DEPLOYMENT:
					if(workloadType == null) workloadType = WorkloadType.REPLICA_SERVER;
				case STATEFUL_SET:
					if(workloadType == null) workloadType = WorkloadType.STATEFUL_SET_SERVER;
				case DAEMON_SET:
					if(workloadType == null) workloadType = WorkloadType.DAEMON_SET_SERVER;
				case CRON_JOB:
					if(workloadType == null) workloadType = WorkloadType.CRON_JOB_SERVER;
				case JOB: {
					if(workloadType == null) workloadType = WorkloadType.JOB_SERVER;

//					try {
						List<Object> objs = ServerUtils.getYamlObjects(templateDeploymentRow.getTemplateContent());
						ServerGuiVO serverAddKubesRow = serverConversionService.convertYamlToGui(null, null, workloadType.getCode(), null, null, null, objs);
						this.setContainerNameForDeployment(serverAddKubesRow);
						this.convertServerInfoByUpgrade(serverAddKubesRow);

						templateDeploymentRow.setTemplateContent(JsonUtils.toGson(serverAddKubesRow));
						templateDeploymentRow.setDeployType(DeployType.GUI.getCode());
//					}
//					catch(Exception ex) {
//						if(log.isDebugEnabled()) ex.printStackTrace();
//					}
					break;
				}
				case SECRET: {
					V1Secret secret = ServerUtils.unmarshalYaml(templateDeploymentRow.getTemplateContent(), K8sApiKindType.SECRET);
					SecretGuiVO secretGui = secretService.convertSecretData(secret, true);
					/** GUI 변환시 Data 값을 지워줌
					 *  - YAML 저장시에는 Data를 지워주지 않는 것이 편의성이 좋아 보이는데...
					 * => YAML 배포시에는 그대로 유지
					 * => Default를 YAML 배포로 하여, Snapshot을 그대로 배포하였을때 문제가 없도록 함.
					 * -------------------------
					 * YAML 저장시에 값을 지우고 저장하는 것으로 변경.. (옵션 처리하여 Data 저장 가능하도록 변경..)
					 * => GUI에만 값을 지워줘서 YAML 배포시 기존 값 그대로 배포 가능하도록 처리..
					 * **/
					if(secretGui != null && MapUtils.isNotEmpty(secretGui.getData())) {
						for(String key : secretGui.getData().keySet()) {
							boolean isBase64 = Utils.isBase64Encoded(MapUtils.getString(secretGui.getData(), key, ""));
							if(isBase64) {
								secretGui.getData().put(key, new String(Base64Utils.decodeFromString(MapUtils.getString(secretGui.getData(), key, "")), "UTF-8"));
							}
							else {
								// base64가 아니면 기존 값 그대로 유지..
								secretGui.getData().put(key, null);
							}
						}
					}
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(secretGui));
					templateDeploymentRow.setDeployType(DeployType.GUI.getCode());
					break;
				}
				case CONFIG_MAP: {
					V1ConfigMap configmap = ServerUtils.unmarshalYaml(templateDeploymentRow.getTemplateContent(), K8sApiKindType.CONFIG_MAP);
					ConfigMapGuiVO configMapGui = configMapService.convertConfigMapData(configmap);
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(configMapGui));
					templateDeploymentRow.setDeployType(DeployType.GUI.getCode());
					break;
				}
				case NET_ATTACH_DEF: {
					Map<String, Object> yamlMap = Yaml.getSnakeYaml().load(templateDeploymentRow.getTemplateContent());
					K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
					crdResourceService.convertNetAttachDef(yamlMap, netAttachDef);
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(netAttachDef));
					templateDeploymentRow.setDeployType(DeployType.GUI.getCode());
					break;
				}
				case SERVICE: {
					V1Service service = ServerUtils.unmarshalYaml(templateDeploymentRow.getTemplateContent(), K8sApiKindType.SERVICE);
					K8sServiceVO k8sService = serviceSpecService.genServiceData(null, null, service, null);
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(k8sService));
					templateDeploymentRow.setDeployType(DeployType.YAML.getCode());
					break;
				}
				case INGRESS: {
					K8sIngressVO k8sIngress = new K8sIngressVO();

					Object obj = ServerUtils.unmarshalYaml(templateDeploymentRow.getTemplateContent(), K8sApiKindType.INGRESS);
					if(obj instanceof V1Ingress) {
						V1Ingress networkingIngresses = (V1Ingress)obj;
						k8sIngress = ingressSpecService.convertIngressData(k8sIngress, networkingIngresses, null);

					}
					else if(obj instanceof NetworkingV1beta1Ingress) {
						NetworkingV1beta1Ingress networkingIngresses = (NetworkingV1beta1Ingress)obj;
						k8sIngress = ingressSpecService.convertIngressData(k8sIngress, networkingIngresses, null);

					}
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(k8sIngress));
					templateDeploymentRow.setDeployType(DeployType.YAML.getCode());
					break;
				}
				case PERSISTENT_VOLUME_CLAIM:
					V1PersistentVolumeClaim persistentVolumeClaim = ServerUtils.unmarshalYaml(templateDeploymentRow.getTemplateContent(), K8sApiKindType.PERSISTENT_VOLUME_CLAIM);
					K8sPersistentVolumeClaimVO k8sPersistentVolumeClaim = persistentVolumeService.convertPersistentVolumeClaim(persistentVolumeClaim);
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(k8sPersistentVolumeClaim));
					templateDeploymentRow.setDeployType(DeployType.YAML.getCode());
					break;
				case PACKAGE:
					// 저장된 Contents 그대로 사용..
					templateDeploymentRow.setDeployType(DeployType.GUI.getCode());
					break;
				case CUSTOM_OBJECT:
					Map<String, Object> customObject = Yaml.getSnakeYaml().load(templateDeploymentRow.getTemplateContent());
					String kind = MapUtils.getString(customObject, "kind", "");
					if(kind.equals(K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION.getValue())) {
						templateDeploymentRow.setTemplateDeploymentType(TemplateDeploymentType.NET_ATTACH_DEF);

						K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
						crdResourceService.convertNetAttachDef(customObject, netAttachDef);
						templateDeploymentRow.setTemplateContent(JsonUtils.toGson(netAttachDef));
					}
					else {
						templateDeploymentRow.setTemplateContent(JsonUtils.toGson(customObject));
					}
					templateDeploymentRow.setDeployType(DeployType.YAML.getCode());
					break;
				case SERVICE_ACCOUNT:
					V1ServiceAccount serviceAccount = ServerUtils.unmarshalYaml(templateDeploymentRow.getTemplateContent(), K8sApiKindType.SERVICE_ACCOUNT);
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(serviceAccount));
					templateDeploymentRow.setDeployType(DeployType.YAML.getCode());
					break;
				case ROLE:
					V1Role role = ServerUtils.unmarshalYaml(templateDeploymentRow.getTemplateContent(), K8sApiKindType.ROLE);
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(role));
					templateDeploymentRow.setDeployType(DeployType.YAML.getCode());
					break;
				case ROLE_BINDING:
					V1RoleBinding roleBinding = ServerUtils.unmarshalYaml(templateDeploymentRow.getTemplateContent(), K8sApiKindType.ROLE_BINDING);
					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(roleBinding));
					templateDeploymentRow.setDeployType(DeployType.YAML.getCode());
					break;
				default:
					log.error(String.format("Unsupported Template Deployment Type : getTemplateDetail : [%s]", templateDeploymentRow.getTemplateDeploymentType()));
					templateDeploymentRow.setTemplateContent(null);
					templateDeploymentRow.setDeployType(DeployType.YAML.getCode());

//					ServerGuiVO serverAddKubesRow = JsonUtils.fromGson(templateDeploymentRow.getTemplateContent(), ServerGuiVO.class);
//					deployService.setContainerNameForDeployment(serverAddKubesRow);
//					this.convertServerInfoByUpgrade(serverAddKubesRow);
//					templateDeploymentRow.setTemplateContent(JsonUtils.toGson(serverAddKubesRow));
					break;
			}
		}

		return templateDetail;
	}

	/**
	 * Gate of Deploy Template
	 * @param templateLaunch
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public ServicemapVO deployTemplate(String apiVersion, Integer templateSeq, TemplateLaunchVO templateLaunch, ExecutingContextVO context) throws Exception {
		try {
			switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
				case V2:
					return this.deployTemplateV2(templateLaunch, context);
				default:
					throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
			}
		}
		catch (Exception ex) {
			if(log.isDebugEnabled()) log.debug("trace log ", ex);
			if(ex instanceof CocktailException) {
				throw ex;
			}
			throw new CocktailException(String.format("An error occurred during template deployment. [%s]", ex.getMessage()), ex, ExceptionType.TemplateDeploymentFail);
		}
	}

	/**
	 * Template 배포.
	 * @param templateLaunch
	 * @param context
	 * @return
	 * @throws Exception
	 */
	@Transactional(transactionManager = "transactionManager")
	public ServicemapVO deployTemplateV2(TemplateLaunchVO templateLaunch, ExecutingContextVO context) throws Exception {
		IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);

		templateLaunch.setCreator(context.getUserSeq());
		templateLaunch.setUseYn("Y");

		ServicemapVO servicemap = null;

		// servicemap 셋팅
		ServicemapAddVO servicemapAdd = new ServicemapAddVO();
		BeanUtils.copyProperties(templateLaunch, servicemapAdd);

		// 클러스터 조회
		ClusterVO cluster = null;
		if(StringUtils.equals(LaunchType.ADD.getType(), templateLaunch.getLaunchType())){
			cluster = this._getClusterInfoByServicemap("템플릿 배포 중 오류가 발생하였습니다. %s", templateLaunch.getServicemapSeq());
			servicemapAdd.setNamespaceName(cluster.getNamespaceName());
			templateLaunch.setNamespaceName(cluster.getNamespaceName());
		}
		else {
			cluster = this._getClusterInfo("템플릿 배포 중 오류가 발생하였습니다. %s", templateLaunch.getClusterSeq());
			cluster.setNamespaceName(templateLaunch.getNamespaceName());
			servicemapAdd.setNamespaceName(templateLaunch.getNamespaceName());
		}

		// 클러스터 볼륨 조회
		List<ClusterVolumeVO> clusterDynamicVolumes = this._getClusterVolumeInfo("템플릿 배포 중 오류가 발생하였습니다. %s", templateLaunch.getClusterSeq(), null, VolumeType.PERSISTENT_VOLUME);
		ClusterVolumeVO defaultClusterVolume = clusterDynamicVolumes.stream().filter(c -> (c != null && BooleanUtils.toBoolean(c.getBaseStorageYn()))).findFirst().orElseGet(() ->null);
		List<ClusterVolumeVO> clusterStaticVolumes = this._getClusterVolumeInfo("템플릿 배포 중 오류가 발생하였습니다. %s", templateLaunch.getClusterSeq(), null, VolumeType.PERSISTENT_VOLUME_STATIC);

		List<ConfigMapIntegrateVO> configMaps = new ArrayList<>();
		List<SecretIntegrateVO> secrets = new ArrayList<>();
		List<K8sCRDNetAttachDefIntegrateVO> netAttachDefs = new ArrayList<>();
		List<ServerIntegrateVO> serverAddKubes = new ArrayList<>();
		List<ServiceSpecIntegrateVO> services = new ArrayList<>();
		List<IngressSpecIntegrateVO> ingresses = new ArrayList<>();
		List<PersistentVolumeClaimIntegrateVO> pvcs = new ArrayList<>();
		List<CommonYamlVO> roles = new ArrayList<>();
		List<CommonYamlVO> roleBindings = new ArrayList<>();
		List<CommonYamlVO> serviceAccounts = new ArrayList<>();
		List<CommonYamlVO> customObjects = new ArrayList<>();
		List<HelmInstallRequestVO> packages = new ArrayList<>();

		Set<String> workloadGroupNameSet = new HashSet<>();     // 워크로드그룹명 중복제거
		Map<Integer, String> workloadGroupNameMap = new HashMap<>();    // 워크로드그룹번호로 그룹명 저장
		Set<String> configMapNameSet = new HashSet<>();         // configMap명 중복제거
		Set<String> secretNameSet = new HashSet<>();            // secret명 중복제거
		Set<String> netAttachDefNameSet = new HashSet<>();      // netAttachDef명 중복제거
		Set<String> componentNameSet = new HashSet<>();         // 컴포넌트명 중복제거
		Set<String> hpaNameSet = new HashSet<>();               // HPA명 중복제거
		Set<String> serviceNameSet = new HashSet<>();           // 서비스명 중복제거
		Set<String> ingressNameSet = new HashSet<>();           // 인그레스명 중복 제거
		Set<String> pvcNameSet = new HashSet<>();               // pvc명 중복제거
		Set<String> serviceAccountNameSet = new HashSet<>();    // serviceAccount명 중복제거
		Set<String> roleNameSet = new HashSet<>();              // role명 중복제거
		Set<String> roleBindingNameSet = new HashSet<>();       // roleBinding명 중복제거
		Set<Pair> customObjectNameSet = new HashSet<>();        // customObject명 중복제거
		Set<String> packageNameSet = new HashSet<>();           // Package 중복 제거
		int groupTempSeq = 0; // 그룹이름별 임시 채번

		// 정렬 후 시작..
		templateLaunch.setTemplateDeployments(templateLaunch.getTemplateDeployments().stream().sorted(Comparator.comparingInt(TemplateDeploymentVO::getSortOrder)).collect(Collectors.toList()));

		// Custom Resource Definition 조회 후 시작.. (한번만 조회하여 사용하기 위함..)
//		List<V1beta1CustomResourceDefinition> v1beta1Crds = Lists.newArrayList();
//		List<V1CustomResourceDefinition> v1Crds = Lists.newArrayList();
//		if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_16)) {
//			v1Crds.addAll(crdResourceService.getCustomResourceDefinitionV1(cluster, null, null));
//		} else {
//			v1beta1Crds.addAll(crdResourceService.getCustomResourceDefinitionV1beta1(cluster, null, null));
//		}
		// 2022.04.12, hjchoi - 필요한 정보가 아래와 같이 조회하여도 충분하여 변경처리
		List<K8sCRDResultVO> crds = crdResourceService.getCustomResourceDefinitions(cluster, null, null);

		/**
		 * 01. Input Data Parsing and validation Check!!
		 */
		for(TemplateDeploymentVO deployment : templateLaunch.getTemplateDeployments()){
			DeployType deployType = DeployType.valueOf(deployment.getDeployType());
			if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.CONFIG_MAP) {
				if(deployType == DeployType.GUI) {
					ConfigMapIntegrateVO configMapIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<ConfigMapIntegrateVO>() {});
					if (configMapIntegrate == null) {
						throw new CocktailException("ConfigMap is null!!", ExceptionType.TemplateDeploymentFail);
					}
					ConfigMapGuiVO configMapRow = (ConfigMapGuiVO)configMapIntegrate;
					if (MapUtils.isNotEmpty(configMapRow.getData())) {
						for (Map.Entry<String, String> dataEntry : configMapRow.getData().entrySet()) {
							if (StringUtils.isBlank(dataEntry.getKey()) || StringUtils.isBlank(dataEntry.getValue())) {
								throw new CocktailException("Config Map data invalid!!", ExceptionType.K8sConfigMapDataInvalid);
							}
						}
					}
					this.addNameSet(configMapRow.getName(), configMapNameSet, "ConfigMap");
					configMapRow.setServiceSeq(null);
					configMapRow.setServicemapSeq(null);
					configMapRow.setDeployType(deployType.getCode());
					configMaps.add(configMapRow);
				}
				else if(deployType == DeployType.YAML) {
//					ConfigMapIntegrateVO configMapIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<ConfigMapIntegrateVO>() {});
//					if (configMapIntegrate == null) {
//						throw new CocktailException("ConfigMap is null!!", ExceptionType.TemplateDeploymentFail);
//					}
//					ConfigMapYamlVO configMapYaml = (ConfigMapYamlVO)configMapIntegrate;
					V1ConfigMap v1Configmap = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.CONFIG_MAP);
					if (v1Configmap == null) {
						throw new CocktailException("ConfigMap is null!!", ExceptionType.TemplateDeploymentFail);
					}
					ConfigMapYamlVO configMapYaml = new ConfigMapYamlVO();
					configMapYaml.setYaml(deployment.getTemplateContentYaml());
					configMapYaml.setDeployType(deployType.getCode());
					configMapYaml.setNamespace(templateLaunch.getNamespaceName());
					configMapYaml.setName(this.getNameFromMetadata(v1Configmap.getMetadata()));
					this.addNameSet(v1Configmap.getMetadata().getName(), configMapNameSet, "ConfigMap");

					configMaps.add(configMapYaml);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.SECRET){
				if(deployType == DeployType.GUI) {
					SecretIntegrateVO secretIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<SecretIntegrateVO>() {});
					if (secretIntegrate == null) {
						throw new CocktailException("Secret is null!!", ExceptionType.TemplateDeploymentFail);
					}
					SecretGuiVO secretRow = (SecretGuiVO)secretIntegrate;
					if (MapUtils.isNotEmpty(secretRow.getData())) {
						for (Map.Entry<String, String> dataEntry : secretRow.getData().entrySet()) {
							if (StringUtils.isBlank(dataEntry.getKey()) || StringUtils.isBlank(dataEntry.getValue())) {
								throw new CocktailException("Secret data invalid!!", ExceptionType.SecretDataInvalid);
							}
						}
					}
					this.addNameSet(secretRow.getName(), secretNameSet, "Secret");
					secretRow.setDeployType(deployType.getCode());
					secrets.add(secretRow);
				}
				else if(deployType == DeployType.YAML) {
//					SecretIntegrateVO secretIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<SecretIntegrateVO>() {});
//					if (secretIntegrate == null) {
//						throw new CocktailException("Secret is null!!", ExceptionType.TemplateDeploymentFail);
//					}
//					SecretYamlVO secretYaml = (SecretYamlVO)secretIntegrate;

					V1Secret v1Secret = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.SECRET);
					if (v1Secret == null) {
						throw new CocktailException("Secret is null!!", ExceptionType.TemplateDeploymentFail);
					}
					SecretYamlVO secretYaml = new SecretYamlVO();
					secretYaml.setYaml(deployment.getTemplateContentYaml());
					secretYaml.setDeployType(deployType.getCode());
					secretYaml.setNamespace(templateLaunch.getNamespaceName());
					secretYaml.setName(this.getNameFromMetadata(v1Secret.getMetadata()));
					this.addNameSet(v1Secret.getMetadata().getName(), secretNameSet, "Secret");

					secrets.add(secretYaml);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.NET_ATTACH_DEF){
				if(deployType == DeployType.GUI) {
					K8sCRDNetAttachDefIntegrateVO netAttachDefRowIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<K8sCRDNetAttachDefIntegrateVO>() {});
					if (netAttachDefRowIntegrate == null) {
						throw new CocktailException("Network Attachment Definition is null!!", ExceptionType.TemplateDeploymentFail);
					}
					K8sCRDNetAttachDefGuiVO netAttachDefRow = (K8sCRDNetAttachDefGuiVO)netAttachDefRowIntegrate;
					crdResourceService.validNetAttachDefConfig(netAttachDefRow);
					this.addNameSet(netAttachDefRow.getName(), netAttachDefNameSet, "Network Attachment Definition");
					netAttachDefRow.setDeployType(deployType.getCode());
					netAttachDefs.add(netAttachDefRow);
				}
				else if(deployType == DeployType.YAML) {
//					K8sCRDNetAttachDefIntegrateVO netAttachDefRowIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<K8sCRDNetAttachDefIntegrateVO>() {});
//					if (netAttachDefRowIntegrate == null) {
//						throw new CocktailException("Network Attachment Definition is null!!", ExceptionType.TemplateDeploymentFail);
//					}
//					K8sCRDNetAttachDefYamlVO k8sCRDNetAttachDefYaml = (K8sCRDNetAttachDefYamlVO)netAttachDefRowIntegrate;
					Map<String, Object> netAttachYaml = Yaml.getSnakeYaml().load(deployment.getTemplateContentYaml());
					if (netAttachYaml == null) {
						throw new CocktailException("Network Attachment Definition is null!!", ExceptionType.TemplateDeploymentFail);
					}
					Map<String, Object> meta = (Map<String, Object>) MapUtils.getMap(netAttachYaml, KubeConstants.META, null);
					K8sCRDNetAttachDefYamlVO k8sCRDNetAttachDefYaml = new K8sCRDNetAttachDefYamlVO();
					k8sCRDNetAttachDefYaml.setYaml(deployment.getTemplateContentYaml());
					k8sCRDNetAttachDefYaml.setDeployType(deployType.getCode());
					k8sCRDNetAttachDefYaml.setNamespace(templateLaunch.getNamespaceName());
					if(StringUtils.isBlank(MapUtils.getString(meta, KubeConstants.NAME, ""))) {
						throw new CocktailException("Invalid YAML Format (Resource name is null)", ExceptionType.InvalidYamlData);
					}
					k8sCRDNetAttachDefYaml.setName(MapUtils.getString(meta, KubeConstants.NAME, ""));
					this.addNameSet(MapUtils.getString(meta, KubeConstants.NAME), netAttachDefNameSet, "Network Attachment Definition");
					netAttachDefs.add(k8sCRDNetAttachDefYaml);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType().isWorkload()) {
				ServerGuiVO serverGuiAddRow = null;
				ServerYamlVO serverYaml = new ServerYamlVO();
				if(deployType == DeployType.GUI) {
					ServerIntegrateVO serverIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<ServerIntegrateVO>() {});
					serverGuiAddRow = (ServerGuiVO) serverIntegrate;
					if (serverIntegrate == null) {
						throw new CocktailException("Workload is null!!", ExceptionType.TemplateDeploymentFail);
					}
				}
				else if(deployType == DeployType.YAML) {
//					ServerIntegrateVO serverIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<ServerIntegrateVO>() {});
//					if (serverIntegrate == null) {
//						throw new CocktailException("Workload is null!!", ExceptionType.TemplateDeploymentFail);
//					}
//					ServerYamlVO serverYaml = (ServerYamlVO) serverIntegrate;
					Object k8sObj = null;
					V1ObjectMeta objectMeta = null;
					String modifiedYaml = null;
					WorkloadType workloadType = this.getWorkloadTypeAsTemplateDeploymentType(deployment.getTemplateDeploymentType());

					switch(workloadType) {
						case SINGLE_SERVER:
						case REPLICA_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.DEPLOYMENT);
							if (k8sObj != null) {
								V1Deployment v1Deployment = (V1Deployment) k8sObj;
								objectMeta = v1Deployment.getMetadata();
								objectMeta.setNamespace(templateLaunch.getNamespaceName());
								modifiedYaml = ServerUtils.marshalYaml(v1Deployment);
							}
							break;
						case STATEFUL_SET_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.STATEFUL_SET);
							if (k8sObj != null) {
								V1StatefulSet statefulSet = (V1StatefulSet) k8sObj;
								objectMeta = statefulSet.getMetadata();
								objectMeta.setNamespace(templateLaunch.getNamespaceName());
								modifiedYaml = ServerUtils.marshalYaml(statefulSet);
							}
							break;
						case DAEMON_SET_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.DAEMON_SET);
							if (k8sObj != null) {
								V1DaemonSet daemonSet = (V1DaemonSet) k8sObj;
								objectMeta = daemonSet.getMetadata();
								objectMeta.setNamespace(templateLaunch.getNamespaceName());
								modifiedYaml = ServerUtils.marshalYaml(daemonSet);
							}
							break;
						case JOB_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.JOB);
							if (k8sObj != null) {
								V1Job job = (V1Job) k8sObj;
								objectMeta = job.getMetadata();
								objectMeta.setNamespace(templateLaunch.getNamespaceName());
								modifiedYaml = ServerUtils.marshalYaml(job);
							}
							break;
						case CRON_JOB_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.CRON_JOB);
							if (k8sObj != null) {
								Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(k8sObj, K8sJsonUtils.getJson());
								K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

								if (K8sApiType.V1BETA1 == apiType) {
									V1beta1CronJob cronJob = (V1beta1CronJob) k8sObj;
									objectMeta = cronJob.getMetadata();
									objectMeta.setNamespace(templateLaunch.getNamespaceName());
									modifiedYaml = ServerUtils.marshalYaml(cronJob);
								} else {
									V1CronJob cronJob = (V1CronJob) k8sObj;
									objectMeta = cronJob.getMetadata();
									objectMeta.setNamespace(templateLaunch.getNamespaceName());
									modifiedYaml = ServerUtils.marshalYaml(cronJob);
								}

							}
							break;
					}
					if (k8sObj == null) {
						throw new CocktailException("Workload is null!!", ExceptionType.TemplateDeploymentFail);
					}

					serverYaml.setYaml(modifiedYaml);
					serverYaml.setNamespaceName(templateLaunch.getNamespaceName());
					serverYaml.setWorkloadVersion("V1");
					serverYaml.setWorkloadType(workloadType.getCode());
					serverYaml.setClusterSeq(templateLaunch.getClusterSeq());
					serverYaml.setWorkloadName(this.getNameFromMetadata(objectMeta));
					serverYaml.setDeployType(deployType.getCode());
					serverYaml.setWorkloadGroupName(deployment.getWorkloadGroupName());

					List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());
					serverGuiAddRow = serverConversionService.convertYamlToGui(null, null, workloadType.getCode(), null, null, null, objs);
					serverGuiAddRow.setDeploymentYaml(serverYaml.getYaml());
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				/**
				 * check supported cluster version!!
				 */
				serverValidService.checkServerApiVersion(serverGuiAddRow.getServer().getWorkloadType(), serverGuiAddRow.getServer().getWorkloadVersion(), cluster, context);

				/**
				 * 서버명 규칙 체크
				 */
				ServerUtils.checkServerNameRule(serverGuiAddRow.getComponent().getComponentName());

				/**
				 * HPA 이름 규칙 체크. 2020.10.19
				 */
				if (serverGuiAddRow.getServer() != null
					&& serverGuiAddRow.getServer().getHpa() != null
					&& CollectionUtils.isNotEmpty(serverGuiAddRow.getServer().getHpa().getMetrics())) {
					ServerUtils.checkHpaNameRule(serverGuiAddRow.getServer().getHpa().getName());
				}

				// 컨테이너 지원여부 체크
				this._checkContainerSupported(serverGuiAddRow, cluster, templateLaunch.getServiceSeq(), false);

				// persistent volume일 경우 해당 클러스터의 볼륨 존재여부 체크
				if (CollectionUtils.isNotEmpty(serverGuiAddRow.getVolumes())) {
					for (int vi = 0, vie = serverGuiAddRow.getVolumes().size(); vi < vie; vi++) {
						if (serverGuiAddRow.getVolumes().get(vi).getVolumeType() == VolumeType.PERSISTENT_VOLUME) {
							if (CollectionUtils.isEmpty(clusterDynamicVolumes)) {
								throw new CocktailException(String.format("템플릿 배포 중 오류가 발생하였습니다. %s", "[클러스터 볼륨 없음]"), ExceptionType.TemplateClusterVolumeNotExists);
							}
						}
						if (serverGuiAddRow.getVolumes().get(vi).getVolumeType() == VolumeType.PERSISTENT_VOLUME_STATIC) {
							if (CollectionUtils.isEmpty(clusterStaticVolumes)) {
								throw new CocktailException(String.format("템플릿 배포 중 오류가 발생하였습니다. %s", "[클러스터 볼륨 없음]"), ExceptionType.TemplateClusterVolumeNotExists);
							}
						}
					}
				}

				// StatefulSet에서 volumeClaimTemplate 처리시 봄륨을 설정하지 않았다면 기본 스토리지로 설정하여 줌
				if (WorkloadType.valueOf(serverGuiAddRow.getServer().getWorkloadType()) == WorkloadType.STATEFUL_SET_SERVER) {
					if (CollectionUtils.isNotEmpty(serverGuiAddRow.getVolumeTemplates())) {
						for (PersistentVolumeClaimGuiVO pvcRow : serverGuiAddRow.getVolumeTemplates()) {
							if (StringUtils.isBlank(pvcRow.getStorageClassName())) {
								if (defaultClusterVolume != null) {
									pvcRow.setStorageClassName(defaultClusterVolume.getName());
								}
								else {
									// 기본 스토리지가 없을시 방어로직
									if (CollectionUtils.isNotEmpty(clusterDynamicVolumes)) {
										pvcRow.setStorageClassName(clusterDynamicVolumes.get(0).getName());
									}
									else {
										throw new CocktailException(String.format("템플릿 배포 중 오류가 발생하였습니다. %s", "[기본 클러스터 볼륨 없음]"), ExceptionType.TemplateClusterVolumeNotExists);
									}
								}
							}
						}
					}
				}

				// HPA V1 -> V2 spec 으로 변경
				if (WorkloadType.valueOf(serverGuiAddRow.getServer().getWorkloadType()).isPossibleAutoscaling()) {
					if (serverGuiAddRow.getServer().getHpa() != null) {
						if (StringUtils.isNotBlank(serverGuiAddRow.getServer().getHpa().getType())) {
							if (StringUtils.equalsIgnoreCase(KubeConstants.HPA_TYPE_CPU, serverGuiAddRow.getServer().getHpa().getType())) {
								// type : CPU -> METRIC
								serverGuiAddRow.getServer().getHpa().setType(KubeConstants.HPA_TYPE_METRIC.toUpperCase());
								// Metrics : Resource > CPU 로 변환하여 셋팅
								List<MetricVO> metrics = new ArrayList<>();
								MetricVO metric = new MetricVO();
								metric.setType(MetricType.Resource);
								metric.setTargetType(MetricTargetType.AverageUtilization);
								metric.setResourceName(KubeConstants.RESOURCES_CPU);
								metric.setTargetAverageUtilization(serverGuiAddRow.getServer().getHpa().getTargetCPUUtilizationPercentage());
								metrics.add(metric);
								serverGuiAddRow.getServer().getHpa().setMetrics(metrics);
							}
						}
						else if (CollectionUtils.isNotEmpty(serverGuiAddRow.getServer().getHpa().getMetrics())) {
							for (MetricVO metric : serverGuiAddRow.getServer().getHpa().getMetrics()) {
								metric.setTargetType(MetricTargetType.Utilization);
								//2020.03.03. metrics에 값이 있다고 V1인게 아님.. => V2일 경우 아래와 같이 처리하면 기존 값이 삭제됨.. => Null일 경우만 값 입력하도록 수정..
//								metric.setTargetAverageUtilization(metric.getResourceTargetAverageUtilization());
								if(metric.getTargetAverageUtilization() == null) {
									metric.setTargetAverageUtilization(metric.getResourceTargetAverageUtilization());
								}
							}
						}
					}
				}

				// 해당 클러스터에서 지원하는 포트로 셋팅
				if (CollectionUtils.isNotEmpty(serverGuiAddRow.getContainers())) {
					String portType = "";
					int cIdx = 1;
					String baseName = ResourceUtil.getUniqueName(serverGuiAddRow.getComponent().getComponentName());

					for (int ci = 0, cie = serverGuiAddRow.getContainers().size(); ci < cie; ci++) {
						// Container Name 가 없다면 생성하여 셋팅
						if (StringUtils.isBlank(serverGuiAddRow.getContainers().get(ci).getContainerName())) {
							serverGuiAddRow.getContainers().get(ci).setContainerName(ResourceUtil.getFormattedName(baseName, cIdx++));
						}

						if (StringUtils.isNotBlank(serverGuiAddRow.getContainers().get(ci).getImageName()) || StringUtils.isNotBlank(serverGuiAddRow.getContainers().get(ci).getImageTag())) {
							serverGuiAddRow.getContainers().get(ci).setFullImageName(String.format("%s:%s", serverGuiAddRow.getContainers().get(ci).getImageName(), serverGuiAddRow.getContainers().get(ci).getImageTag()));
							serverGuiAddRow.getContainers().get(ci).setImageName(null);
							serverGuiAddRow.getContainers().get(ci).setImageTag(null);
						}
					}
				}

				serverGuiAddRow.getComponent().setSortOrder(deployment.getSortOrder()); // 순서 셋팅

				// Add componentNameSet
				this.addNameSet(serverGuiAddRow.getComponent().getComponentName(), componentNameSet, "Workload");

				// Add hpaNameSet : 2020.10.19
				if (serverGuiAddRow.getServer() != null
					&& serverGuiAddRow.getServer().getHpa() != null
					&& CollectionUtils.isNotEmpty(serverGuiAddRow.getServer().getHpa().getMetrics())) {
					this.addNameSet(serverGuiAddRow.getServer().getHpa().getName(), hpaNameSet, "Horizontal Pod Autoscaler");
				}

                /** 워크로드 그룹 셋팅 시작 **/
                // WorkloadGroupName이 존재할때만 처리한다.
                if (StringUtils.isEmpty(deployment.getWorkloadGroupName())) {
                    deployment.setWorkloadGroupName(CommonConstants.DEFAULT_GROUP_NAME);
                }

                // 그룹이름별로 그룹번호 임시 채번하여 셋팅
                if (!workloadGroupNameSet.contains(deployment.getWorkloadGroupName())) {
                    if (deployType == DeployType.GUI) {
                        serverGuiAddRow.getComponent().setWorkloadGroupSeq(groupTempSeq++);
                    } else {
                        serverYaml.setWorkloadGroupSeq(groupTempSeq++);
                    }
                } else {
                    for (Map.Entry<Integer, String> entry : workloadGroupNameMap.entrySet()) {
                        if (StringUtils.equals(deployment.getWorkloadGroupName(), entry.getValue())) {
                            if (deployType == DeployType.GUI) {
                                serverGuiAddRow.getComponent().setWorkloadGroupSeq(Integer.parseInt(entry.getKey().toString()));
                            } else {
                                serverYaml.setWorkloadGroupSeq(Integer.parseInt(entry.getKey().toString()));
                            }
                            break;
                        }
                    }
                }

                // add groupNameSet
                workloadGroupNameSet.add(deployment.getWorkloadGroupName());
                /** 워크로드 그룹 셋팅 시작 **/

				// 컴포넌트 정보 목록에 추가
				if(deployType == DeployType.GUI) {
					// 그룹 관련 변수도 셋팅
					workloadGroupNameMap.put(serverGuiAddRow.getComponent().getWorkloadGroupSeq(), deployment.getWorkloadGroupName());
					serverAddKubes.add(serverGuiAddRow);
				}
				else if(deployType == DeployType.YAML) {
					// 그룹 관련 변수도 셋팅
					workloadGroupNameMap.put(serverYaml.getWorkloadGroupSeq(), deployment.getWorkloadGroupName());
					serverAddKubes.add(serverYaml);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.SERVICE) {
				if(deployType == DeployType.GUI) {
					ServiceSpecIntegrateVO serviceIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<ServiceSpecIntegrateVO>() {});
					if(serviceIntegrate == null) {
						throw new CocktailException("Service is null!!", ExceptionType.TemplateDeploymentFail);
					}
					ServiceSpecGuiVO serviceRow = (ServiceSpecGuiVO)serviceIntegrate;
					this.serviceSpecValidationCheck(cluster, templateLaunch.getNamespaceName(), serviceRow);
					this.addNameSet(serviceRow.getName(), serviceNameSet, "Service");
					serviceRow.setAppmapSeq(null);
					services.add(serviceRow);
				}
				else if(deployType == DeployType.YAML) {
//					ServiceSpecIntegrateVO serviceIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<ServiceSpecIntegrateVO>() {});
//					if(serviceIntegrate == null) {
//						throw new CocktailException("Service is null!!", ExceptionType.TemplateDeploymentFail);
//					}
//					ServiceSpecYamlVO serviceSpecYaml = (ServiceSpecYamlVO)serviceIntegrate;

					V1Service v1Service = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.SERVICE);
					if(v1Service == null) {
						throw new CocktailException("Service is null!!", ExceptionType.TemplateDeploymentFail);
					}
					ServiceSpecYamlVO serviceSpecYaml = new ServiceSpecYamlVO();
					serviceSpecYaml.setYaml(deployment.getTemplateContentYaml());
					serviceSpecYaml.setDeployType(deployType.getCode());
					serviceSpecYaml.setServicemapSeq(null);
					serviceSpecYaml.setNamespaceName(templateLaunch.getNamespaceName());
					serviceSpecYaml.setName(this.getNameFromMetadata(v1Service.getMetadata()));

					ServiceSpecGuiVO tempService = serverConversionService.convertYamlToServiceSpec(cluster, serviceSpecYaml.getYaml());
					this.serviceSpecValidationCheck(cluster, templateLaunch.getNamespaceName(), tempService);
					this.addNameSet(tempService.getName(), serviceNameSet, "Service");

					services.add(serviceSpecYaml);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.INGRESS) {
				if(deployType == DeployType.GUI) {
					IngressSpecIntegrateVO ingressIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<IngressSpecIntegrateVO>() {});
					if(ingressIntegrate == null) {
						throw new CocktailException("Ingress is null!!", ExceptionType.TemplateDeploymentFail);
					}
					IngressSpecGuiVO ingressRow = (IngressSpecGuiVO)ingressIntegrate;
					ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), true, ingressRow);
					this.addNameSet(ingressRow.getName(), ingressNameSet, "Ingress");
					ingresses.add(ingressRow);
				}
				else if(deployType == DeployType.YAML) {
//					IngressSpecIntegrateVO ingressIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<IngressSpecIntegrateVO>() {});
//					if(ingressIntegrate == null) {
//						throw new CocktailException("Ingress is null!!", ExceptionType.TemplateDeploymentFail);
//					}
//					IngressSpecYamlVO ingressYaml = (IngressSpecYamlVO)ingressIntegrate;

					Object obj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.INGRESS);
					if(obj == null) {
						throw new CocktailException("Ingress is null!!", ExceptionType.TemplateDeploymentFail);
					}
					IngressSpecYamlVO ingressYaml = new IngressSpecYamlVO();
					ingressYaml.setYaml(deployment.getTemplateContentYaml());
					ingressYaml.setDeployType(deployType.getCode());
					ingressYaml.setClusterSeq(cluster.getClusterSeq());
					ingressYaml.setNamespaceName(templateLaunch.getNamespaceName());
					if(obj instanceof NetworkingV1beta1Ingress) {
						NetworkingV1beta1Ingress networkingIngresses = (NetworkingV1beta1Ingress)obj;
						ingressYaml.setName(this.getNameFromMetadata(networkingIngresses.getMetadata()));
					}
					else if(obj instanceof V1Ingress) {
						V1Ingress networkingIngresses = (V1Ingress)obj;
						ingressYaml.setName(this.getNameFromMetadata(networkingIngresses.getMetadata()));
					}

					IngressSpecGuiVO tempIngress = ingressSpecService.convertIngressSpecYamlToGui(cluster, null, templateLaunch.getNamespaceName(), ingressYaml.getYaml());
					ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), true, tempIngress);
					this.addNameSet(tempIngress.getName(), ingressNameSet, "Ingress");

					ingresses.add(ingressYaml);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.PERSISTENT_VOLUME_CLAIM) {
				if(deployType == DeployType.GUI) {
					PersistentVolumeClaimIntegrateVO persistentVolumeClaimIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<PersistentVolumeClaimIntegrateVO>() {});
					if(persistentVolumeClaimIntegrate == null) {
						throw new CocktailException("Persistent Volume Claim is null!!", ExceptionType.TemplateDeploymentFail);
					}
					PersistentVolumeClaimGuiVO pvcRow = (PersistentVolumeClaimGuiVO)persistentVolumeClaimIntegrate;
					this.addNameSet(pvcRow.getName(), pvcNameSet, "Persistent Volume Claim");

					pvcs.add(pvcRow);
				}
				else if(deployType == DeployType.YAML) {
//					PersistentVolumeClaimIntegrateVO persistentVolumeClaimIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<PersistentVolumeClaimIntegrateVO>() {});
//					if(persistentVolumeClaimIntegrate == null) {
//						throw new CocktailException("Persistent Volume Claim is null!!", ExceptionType.TemplateDeploymentFail);
//					}
//					PersistentVolumeClaimYamlVO pvcYaml = (PersistentVolumeClaimYamlVO)persistentVolumeClaimIntegrate;

					V1PersistentVolumeClaim v1Pvc = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.PERSISTENT_VOLUME_CLAIM);
					if(v1Pvc == null) {
						throw new CocktailException("Persistent Volume Claim is null!!", ExceptionType.TemplateDeploymentFail);
					}
					PersistentVolumeClaimYamlVO pvcYaml = new PersistentVolumeClaimYamlVO();
					pvcYaml.setYaml(deployment.getTemplateContentYaml());
					pvcYaml.setDeployType(deployType.getCode());
					pvcYaml.setNamespace(templateLaunch.getNamespaceName());
					pvcYaml.setName(this.getNameFromMetadata(v1Pvc.getMetadata()));

					this.addNameSet(v1Pvc.getMetadata().getName(), pvcNameSet, "Persistent Volume Claim");
					pvcs.add(pvcYaml);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.CUSTOM_OBJECT) {
				CommonYamlVO customObjectYaml = new CommonYamlVO();
				if(deployType == DeployType.GUI) {
					throw new CocktailException("The CustomObject only supports YAML deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else if(deployType == DeployType.YAML) {
//					customObjectYaml = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<CommonYamlVO>() {});
					customObjectYaml.setYaml(deployment.getTemplateContentYaml());
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				Map<String, Object> customObject = Yaml.getSnakeYaml().load(customObjectYaml.getYaml());
				if(customObject == null) {
					throw new CocktailException("CUSTOM_OBJECT is null!!", ExceptionType.TemplateDeploymentFail);
				}
				Map<String, Object> meta = (Map<String, Object>) MapUtils.getMap(customObject, KubeConstants.META, null);

				if(StringUtils.isBlank(MapUtils.getString(meta, KubeConstants.NAME))) {
					throw new CocktailException("Custom Object Name is Required!!", ExceptionType.TemplateDeploymentFail);
				}
				for(Pair pr : customObjectNameSet) {
					if(StringUtils.equals(pr.getLeft().toString(), MapUtils.getString(customObject, KubeConstants.KIND)) &&
						StringUtils.equals(pr.getRight().toString(), MapUtils.getString(meta, KubeConstants.NAME))) {
						throw new CocktailException("Duplicate Custom Object Name!!", ExceptionType.TemplateDeploymentFail);
					}
				}
				customObjectNameSet.add(Pair.of(MapUtils.getString(customObject, KubeConstants.KIND, ""), MapUtils.getString(meta, KubeConstants.NAME)));
				customObjectYaml.setName(MapUtils.getString(meta, KubeConstants.NAME));
				customObjectYaml.setDeployType(deployType.getCode());
				customObjectYaml.setNamespace(templateLaunch.getNamespaceName());
				customObjectYaml.setK8sApiKindType(K8sApiKindType.CUSTOM_OBJECT);
				customObjectYaml.setCustomObjectKind(MapUtils.getString(customObject, KubeConstants.KIND));

				// 매칭되는 CustomObject의 Definition을 찾음..
				this.setCustomObjectInfo(crds, customObject, customObjectYaml);

				if(StringUtils.isBlank(customObjectYaml.getCustomObjectGroup())) {
					log.error(String.format("Could not found matched Custom Resource Definition. [Kind : %s]", customObjectYaml.getCustomObjectKind()));
				}

				customObjects.add(customObjectYaml);
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.SERVICE_ACCOUNT) {
				CommonYamlVO saYaml = new CommonYamlVO();
				if(deployType == DeployType.GUI) {
					throw new CocktailException("The ServiceAccount only supports YAML deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else if(deployType == DeployType.YAML) {
//					saYaml = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<CommonYamlVO>() {});
					saYaml.setYaml(deployment.getTemplateContentYaml());
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				V1ServiceAccount serviceAccount = ServerUtils.unmarshalYaml(saYaml.getYaml(), K8sApiKindType.SERVICE_ACCOUNT);
				if(serviceAccount == null) {
					throw new CocktailException("SERVICE_ACCOUNT is null!!", ExceptionType.TemplateDeploymentFail);
				}
				this.addNameSet(serviceAccount.getMetadata().getName(), serviceAccountNameSet, "Service Account");

				saYaml.setName(serviceAccount.getMetadata().getName());
				saYaml.setDeployType(deployType.getCode());
				saYaml.setNamespace(templateLaunch.getNamespaceName());
				saYaml.setK8sApiKindType(K8sApiKindType.SERVICE_ACCOUNT);
				serviceAccounts.add(saYaml);
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.ROLE) {
				CommonYamlVO roleYaml = new CommonYamlVO();
				if(deployType == DeployType.GUI) {
					throw new CocktailException("The Role only supports YAML deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else if(deployType == DeployType.YAML) {
//					roleYaml = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<CommonYamlVO>() {});
					roleYaml.setYaml(deployment.getTemplateContentYaml());
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				V1Role role = ServerUtils.unmarshalYaml(roleYaml.getYaml(), K8sApiKindType.ROLE);
				if(role == null) {
					throw new CocktailException("ROLE is null!!", ExceptionType.TemplateDeploymentFail);
				}
				this.addNameSet(role.getMetadata().getName(), roleNameSet, "Role");

				roleYaml.setName(role.getMetadata().getName());
				roleYaml.setDeployType(deployType.getCode());
				roleYaml.setNamespace(templateLaunch.getNamespaceName());
				roleYaml.setK8sApiKindType(K8sApiKindType.ROLE);
				roles.add(roleYaml);
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.ROLE_BINDING) {
				CommonYamlVO roleBindingYaml = new CommonYamlVO();
				if(deployType == DeployType.GUI) {
					throw new CocktailException("The RoleBinding only supports YAML deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else if(deployType == DeployType.YAML) {
//					roleBindingYaml = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<CommonYamlVO>() {});
					roleBindingYaml.setYaml(deployment.getTemplateContentYaml());
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				V1RoleBinding roleBinding = ServerUtils.unmarshalYaml(roleBindingYaml.getYaml(), K8sApiKindType.ROLE_BINDING);
				if(roleBinding == null) {
					throw new CocktailException("ROLE_BINDING is null!!", ExceptionType.TemplateDeploymentFail);
				}
				this.addNameSet(roleBinding.getMetadata().getName(), roleBindingNameSet, "RoleBinding");

				roleBindingYaml.setName(roleBinding.getMetadata().getName());
				roleBindingYaml.setDeployType(deployType.getCode());
				roleBindingYaml.setNamespace(templateLaunch.getNamespaceName());
				roleBindingYaml.setK8sApiKindType(K8sApiKindType.ROLE);
				roleBindings.add(roleBindingYaml);
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.PACKAGE) {
				if(deployType == DeployType.GUI) {
					HelmInstallRequestVO helmInstallRequest = JsonUtils.fromGson(deployment.getTemplateContent(), HelmInstallRequestVO.class);
					helmInstallRequest.setNamespace(templateLaunch.getNamespaceName());
					helmInstallRequest.setServicemapName(templateLaunch.getServicemapName());
					/** 규칙 체크 */
					ServerUtils.checkServerNameRule(helmInstallRequest.getReleaseName());
					/** 중복 체크 */
					if(packageValidService.isUsingReleaseName(cluster, templateLaunch.getNamespaceName(), helmInstallRequest.getReleaseName(), false)) {
						throw new CocktailException("release name already exists", ExceptionType.PackageNameAlreadyExists);
					}
					packageValidService.packageInstallValidation(helmInstallRequest, templateLaunch.getNamespaceName(), null);
					this.addNameSet(helmInstallRequest.getReleaseName(), packageNameSet, "Package");
					packages.add(helmInstallRequest);
				}
				else if(deployType == DeployType.YAML) {
					throw new CocktailException("The Package only supports GUI deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else {
					throw new CocktailException("Invalid DeployType!!", ExceptionType.TemplateDeploymentFail);
				}
			}
			else {
				log.error(String.format("Unsupported Template Deployment Type : deployTemplate : [%s]", deployment.getTemplateDeploymentType()));
			}
		}

		/**
		 * 02. Basic Resource Creation and validation check.
		 */
		List<String> workloadGroupNames = new ArrayList<>(workloadGroupNameSet);

		ServiceDetailVO service = serviceDao.getService(servicemapAdd.getServiceSeq());
		// registry url
		String registryUrl = registryPropertyService.getUrl(cluster.getAccount().getAccountSeq());
		String registryPullUserId = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId());
		String registryPullUserPassword = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserPassword());

		// service registry user
		List<HarborProjectMemberVO> users = new ArrayList<>();
		HarborProjectMemberVO user = new HarborProjectMemberVO();
		user.setEntityName(service.getRegistryUserId());
		user.setEntityPassword(service.getRegistryUserPassword());
		user.setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());
		users.add(user);

		Set<Integer> projectSet = new HashSet<>(); // 다른 service registry project

		// harbor api client
		IHarborRegistryService harborRegistryService = harborRegistryFactory.getService();

		// service registry name 조회
		String registryProjectName = null;
		if(service != null && CollectionUtils.isNotEmpty(service.getProjects())) {
			RegistryProjectVO serviceRegistryProject = harborRegistryService.getProject(service.getProjects().get(0).getProjectId());
			if(serviceRegistryProject != null){
				registryProjectName = serviceRegistryProject.getName();
			}else{
				log.info("Template deployment : service registry not exists");
//				throw new CocktailException("템플릿 배포 중 오류가 발생하였습니다.[service registry not exists]", ExceptionType.TemplateDeploymentFail);
			}
		}else{
			log.info("Template deployment : service registry not exists");
//			throw new CocktailException("템플릿 배포 중 오류가 발생하였습니다.[service not exists]", ExceptionType.TemplateDeploymentFail);
		}

		/** 신규 Namespace 배포 **/
		if(StringUtils.equals(LaunchType.NEW.getType(), templateLaunch.getLaunchType())){
			/** Add Servicemap and WorkloadGroups **/
			servicemap = servicemapService.addServicemap(servicemapAdd, workloadGroupNames, context);
			cluster.setNamespaceName(servicemap.getNamespaceName()); // 생성된 Servicemap의 Namespace를 설정
			/**
			 * 그룹명으로 생성된 그룹번호 셋팅
			 **/
			for (ServerIntegrateVO serverIntegrate : serverAddKubes) {
				ServerYamlVO serverYaml = null;
				ServerGuiVO serverAddKube = null;
				String workloadGroupName = "";
				if (serverIntegrate != null && DeployType.valueOf(serverIntegrate.getDeployType()) == DeployType.GUI) {
					serverAddKube = (ServerGuiVO) serverIntegrate;
					workloadGroupName = workloadGroupNameMap.get(serverAddKube.getComponent().getWorkloadGroupSeq());
				}
				else if (serverIntegrate != null && DeployType.valueOf(serverIntegrate.getDeployType()) == DeployType.YAML) {
					serverYaml = (ServerYamlVO) serverIntegrate;
					List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());
					serverAddKube = serverConversionService.convertYamlToGui(null, null, serverYaml.getWorkloadType(), null, null, null, objs);

					workloadGroupName = workloadGroupNameMap.get(serverYaml.getWorkloadGroupSeq());
				}

				// 그룹번호 셋팅
				for (WorkloadGroupVO workloadGroupRow : servicemap.getWorkloadGroups()) {
					if (StringUtils.equals(workloadGroupName, workloadGroupRow.getWorkloadGroupName())) {
						serverAddKube.getComponent().setWorkloadGroupSeq(workloadGroupRow.getWorkloadGroupSeq());
						/** 템플릿 배포시 워크로드에 Group 정보 Annotation 추가 : 2020.02.04 : redion **/
						Map<String, String> annotations = Optional.ofNullable(serverAddKube.getServer()).map(ServerVO::getAnnotations).orElseGet(() ->Maps.newHashMap());
						annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, workloadGroupRow.getWorkloadGroupSeq().toString());
						serverAddKube.getServer().setAnnotations(annotations);
						// YAML 배포일때 Group 정보 설정 추가 : 2020.03.05 : redion
						if (serverIntegrate != null && DeployType.valueOf(serverIntegrate.getDeployType()) == DeployType.YAML) {
							this.setGroupToWorkloadYaml(serverYaml, workloadGroupRow.getWorkloadGroupSeq().toString());
						}
						break;
					}
				}
				// Servicemap 정보 셋팅 추가 (WorkloadGroup번호 대신 Audit에서 사용)
                ServicemapSummaryVO smsTarget = new ServicemapSummaryVO();
                BeanUtils.copyProperties(servicemap, smsTarget);
				serverAddKube.getComponent().setServicemapInfo(smsTarget);
				// clusterSeq 셋팅
				serverAddKube.getComponent().setClusterSeq(servicemap.getClusterSeq());
				// namespace 셋팅
				serverAddKube.getComponent().setNamespaceName(servicemap.getNamespaceName());

				// hjchoi 2022.04.12 플랫폼 레지스트리 pull 사용자로 사용
//				if (CollectionUtils.isNotEmpty(serverAddKube.getContainers())) {
//					// 해당 서비스의 service registry(project)와 container image registry(project)가 다른 project를 projectSet에 셋팅
//					this.setMemberToProject(users, projectSet, registryProjectName, registryUrl, serverAddKube.getContainers());
//				}
			}
		}
		/** 기존 Namespace 배포 **/
		else if(StringUtils.equals(LaunchType.ADD.getType(), templateLaunch.getLaunchType())) {

			IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);

			List<String> componentNames = new ArrayList<>(componentNameSet);

			ServicemapDetailVO servicemapDetail = servicemapDao.getServicemapDetail(templateLaunch.getServicemapSeq(), templateLaunch.getServiceSeq());
			servicemap = servicemapDetail;

			if(servicemapDetail != null) {
				// 컴포넌트명 중복 체크
				List<String> hasComponentNameList = new ArrayList<>();
				if(CollectionUtils.isNotEmpty(servicemapDetail.getComponents())){
					for(ComponentVO componentRow : servicemapDetail.getComponents()){
						if(componentNames.contains(componentRow.getComponentName())){
							hasComponentNameList.add(componentRow.getComponentName());
						}
					}
				}
				if(CollectionUtils.isNotEmpty(hasComponentNameList)){
					throw new CocktailException(String.format("Workload [%s] already exists.",
						hasComponentNameList.stream().collect(Collectors.joining(","))),
						ExceptionType.ServerNameAlreadyExists);
				}
/**
 *  Stop된 워크로드 안에 있는 HPA 이름의 중복 체크까지 처리 여부 (주석 처리 : Stop된 워크로드를 재시작할때 알림을 주는 방향으로 정리함)
 *
				// HPA명 중복 체크
				List<String> hpaNames = new ArrayList<>(hpaNameSet);
				List<String> hasHpaNameList = new ArrayList<>();
				// 조회한 해당 앱맵의 Autoscaler명 셋팅 (STOP되어 있는 Snapshot 형태의 정보도 비교하기 위함)
				if(appmap != null) {
					if (CollectionUtils.isNotEmpty(appmap.getComponents())) {
						for (ComponentVO comp4Hpa : appmap.getComponents()) {
							// 각 Component(워크로드의 Manifest 파일을 parsing 하여 HPA 정보를 조회한 후 Name set을 구성)
							List<Object> objs = ServerUtils.getYamlObjects(comp4Hpa.getWorkloadManifest());
							ServerGuiVO serverGui4Hpa = serverConversionService.convertYamlToGui(null, null, comp4Hpa.getWorkloadType(), null, null, null, objs);
							if (serverGui4Hpa.getServer() != null
								&& serverGui4Hpa.getServer().getHpa() != null
								&& CollectionUtils.isNotEmpty(serverGui4Hpa.getServer().getHpa().getMetrics())) {
								if(hpaNames.contains(serverGui4Hpa.getServer().getHpa().getName())) {
									hasHpaNameList.add(serverGui4Hpa.getServer().getHpa().getName());
								}
							}
						}
					}
				}
				if(CollectionUtils.isNotEmpty(hasHpaNameList)){
					throw new CocktailException(String.format("Horizontal Pod Autoscaler [%s] already exists.",
						hasHpaNameList.stream().collect(Collectors.joining(","))),
						ExceptionType.HorizontalPodAutoscalerNameAlreadyExists);
				}
*/

				//TODO : 일단 하나하나 체크.. 나중에 목록 조회해서 체크하도록 리팩토링..
				/** Namespace안에 워크로드가 존재하는지 Cluster를 직접 체크 추가 **/
				for(String componentName : componentNameSet) {
					serverValidService.checkServerNameIfExists(cluster, servicemap.getNamespaceName(), componentName, true, null);
				}
				/** Namespace안에 HPA가 존재하는지 Cluster를 직접 체크 추가 **/
				for(String hpaName : hpaNameSet) {
					if(serverValidService.checkHpaNameIfExists(cluster, servicemap.getNamespaceName(), hpaName)) {
						throw new CocktailException("HPA Name Already Exists", ExceptionType.HorizontalPodAutoscalerNameAlreadyExists);
					}
				}

				/**
				 * 그룹 처리
 				 */
				List<WorkloadGroupVO> hasWorkloadGroupList = new ArrayList<>(); // 기존에 존재하는 그룹
				List<WorkloadGroupVO> hasNotWorkloadGroupList = new ArrayList<>(); // 기존에 존재하지 않는 그룹
				GROUP_ADD_LOOP :
				for(String workloadGroupName : workloadGroupNames){
					/** 존재하는 그룹이면 다음 탐색.. **/
					for(WorkloadGroupVO groupRow : servicemap.getWorkloadGroups()){
						if(StringUtils.equals(workloadGroupName, groupRow.getWorkloadGroupName())){
							hasWorkloadGroupList.add(groupRow);
							continue GROUP_ADD_LOOP;
						}
					}

					/** 존재하지 않는 그룹이면 신규로 그룹 등록 **/
					WorkloadGroupAddVO groupRow = new WorkloadGroupAddVO();
					groupRow.setServicemapSeq(templateLaunch.getServicemapSeq());
					groupRow.setWorkloadGroupName(workloadGroupName);
					groupRow.setColumnCount(1);
					groupRow.setSortOrder(servicemap.getWorkloadGroups().get(servicemap.getWorkloadGroups().size()-1).getSortOrder()+1);
					groupRow.setUseYn("Y");
					groupRow.setCreator(templateLaunch.getCreator());
					// 그룹 생성
					workloadGroupDao.addWorkloadGroup(groupRow);

					hasNotWorkloadGroupList.add(groupRow);
					servicemap.getWorkloadGroups().add(groupRow);
				}
				/** 전체 그룹을 하나로 Merge **/
				List<WorkloadGroupVO> workloadGroupList = new ArrayList<>();
				workloadGroupList.addAll(hasWorkloadGroupList);
				workloadGroupList.addAll(hasNotWorkloadGroupList);

				/** 서버에 그룹 정보 설정 **/
				for (ServerIntegrateVO serverIntegrate : serverAddKubes) {
					ServerYamlVO serverYaml = null;
					ServerGuiVO serverAddKube = null;
					String workloadGroupName = "";
					if (serverIntegrate != null && DeployType.valueOf(serverIntegrate.getDeployType()) == DeployType.GUI) {
						serverAddKube = (ServerGuiVO) serverIntegrate;
						workloadGroupName = workloadGroupNameMap.get(serverAddKube.getComponent().getWorkloadGroupSeq());
					}
					else if (serverIntegrate != null && DeployType.valueOf(serverIntegrate.getDeployType()) == DeployType.YAML) {
						serverYaml = (ServerYamlVO) serverIntegrate;
						List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());
						serverAddKube = serverConversionService.convertYamlToGui(null, null, serverYaml.getWorkloadType(), null, null, null, objs);

						workloadGroupName = workloadGroupNameMap.get(serverYaml.getWorkloadGroupSeq());
					}

					// 그룹번호 셋팅
					for (WorkloadGroupVO groupRow : workloadGroupList) {
						if (StringUtils.equals(workloadGroupName, groupRow.getWorkloadGroupName())) {
							serverAddKube.getComponent().setWorkloadGroupSeq(groupRow.getWorkloadGroupSeq());
							/** 템플릿 배포시 워크로드에 Group 정보 Annotation 추가 : 2020.02.04 : redion **/
							Map<String, String> annotations = Optional.ofNullable(serverAddKube.getServer()).map(ServerVO::getAnnotations).orElseGet(() ->Maps.newHashMap());
							annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, groupRow.getWorkloadGroupSeq().toString());
							serverAddKube.getServer().setAnnotations(annotations);
							// YAML 배포일때 Group 정보 설정 추가 : 2020.03.05 : redion
							if (serverIntegrate != null && DeployType.valueOf(serverIntegrate.getDeployType()) == DeployType.YAML) {
								this.setGroupToWorkloadYaml(serverYaml, groupRow.getWorkloadGroupSeq().toString());
							}
							break;
						}
					}
					// Servicemap 정보 셋팅 추가 (Workload Group번호 대신 Audit에서 사용)
                    ServicemapSummaryVO smsTarget = new ServicemapSummaryVO();
                    BeanUtils.copyProperties(servicemap, smsTarget);
					serverAddKube.getComponent().setServicemapInfo(smsTarget);
					// clusterSeq 셋팅
					serverAddKube.getComponent().setClusterSeq(cluster.getClusterSeq());
					// namespace 셋팅
					serverAddKube.getComponent().setNamespaceName(servicemap.getNamespaceName());

					// hjchoi 2022.04.12 플랫폼 레지스트리 pull 사용자로 사용
//					// 해당 서비스의 service registry(project)와 container image registry(project)가 다른 project를 projectSet에 셋팅
//					if(CollectionUtils.isNotEmpty(serverAddKube.getContainers())){
//						this.setMemberToProject(harborRegistryService, users, projectSet, registryProjectName, registryUrl, serverAddKube.getContainers());
//					}
				}
			}
			else{
				throw new CocktailException("Could not found exist Appmap", ExceptionType.TemplateDeploymentFail);
			}

			Set<String> k8sConfigMapNameSet = new HashSet<>();         // configMap명 중복제거
			Set<String> k8sSecretNameSet = new HashSet<>();            // secret명 중복제거
			Set<String> k8sNetAttachDefNameSet = new HashSet<>();      // netAttachDef명 중복제거
			/** Config-Map, Secret, NetAttachDef 조회 및 중복 체크 **/
			this.getConfigMapNSecretNetAttachDef(servicemap.getServicemapSeq(), k8sConfigMapNameSet, k8sSecretNameSet, k8sNetAttachDefNameSet);

			// Config-Map 중복 체크
			if(CollectionUtils.isNotEmpty(configMapNameSet) && CollectionUtils.isNotEmpty(configMaps)){
				for(ConfigMapIntegrateVO configMapIntegrate : configMaps){
					String name = null;
					if (DeployType.valueOf(configMapIntegrate.getDeployType()) == DeployType.GUI) {
						name = ((ConfigMapGuiVO) configMapIntegrate).getName();
						((ConfigMapGuiVO) configMapIntegrate).setServicemapSeq(servicemap.getServicemapSeq());
					}
					if (DeployType.valueOf(configMapIntegrate.getDeployType()) == DeployType.YAML) {
						name = ((ConfigMapYamlVO) configMapIntegrate).getName();
						((ConfigMapYamlVO) configMapIntegrate).setServicemapSeq(servicemap.getServicemapSeq());
					}
					if (k8sConfigMapNameSet.contains(name)) {
						throw new CocktailException(String.format("ConfigMap [%s] already exists.", name), ExceptionType.K8sConfigMapAlreadyExists);
					}
				}
			}
			// Secret 중복 체크
			if(CollectionUtils.isNotEmpty(secretNameSet) && CollectionUtils.isNotEmpty(secrets)){
				for(SecretIntegrateVO secretIntegrate : secrets){
					String name = null;
					if (DeployType.valueOf(secretIntegrate.getDeployType()) == DeployType.GUI) {
						name = ((SecretGuiVO) secretIntegrate).getName();
					}
					if (DeployType.valueOf(secretIntegrate.getDeployType()) == DeployType.YAML) {
						name = ((SecretYamlVO) secretIntegrate).getName();
					}
					if(k8sSecretNameSet.contains(name)){
						throw new CocktailException(String.format("Secret [%s] already exists.", name), ExceptionType.SecretNameAlreadyExists);
					}
				}
			}
			// NetAttachDef 중복 체크
			if(CollectionUtils.isNotEmpty(netAttachDefNameSet) && CollectionUtils.isNotEmpty(netAttachDefs)){
				for(K8sCRDNetAttachDefIntegrateVO newAttachDefRowIntegrate : netAttachDefs){
					String name = null;
					if (DeployType.valueOf(newAttachDefRowIntegrate.getDeployType()) == DeployType.GUI) {
						name = ((K8sCRDNetAttachDefGuiVO) newAttachDefRowIntegrate).getName();
					}
					if (DeployType.valueOf(newAttachDefRowIntegrate.getDeployType()) == DeployType.YAML) {
						name = ((K8sCRDNetAttachDefYamlVO) newAttachDefRowIntegrate).getName();
					}
					if(k8sNetAttachDefNameSet.contains(name)){
						throw new CocktailException(String.format("Network Attachment Definition [%s] already exists.", name), ExceptionType.K8sNetAttachDefAlreadyExists);
					}
				}
			}

			/** 신규 리소스들에 대한 중복 체크 시작
			 *  01. 이름 중복 체크.
			 **/
			// Service 중복 체크
			List<V1Service> v1Services = serviceSpecService.getServicesV1(cluster, servicemap.getNamespaceName(), null, null, ContextHolder.exeContext());
			if (CollectionUtils.isNotEmpty(v1Services)) {
				for (V1Service serviceRow : v1Services) {
					if(serviceNameSet.contains(serviceRow.getMetadata().getName())) {
						throw new CocktailException(String.format("Service [%s] already exists.", serviceRow.getMetadata().getName()), ExceptionType.ServiceNameAlreadyExists);
					}
				}
			}

			// Ingress 중복 체크
			List<NetworkingV1beta1Ingress> networkingIngressesV1beta1 = ingressSpecService.getIngressesNetworkingV1Beta1(cluster, servicemap.getNamespaceName(), null, null);
			if (CollectionUtils.isNotEmpty(networkingIngressesV1beta1)) {
				for (NetworkingV1beta1Ingress ingressRow : networkingIngressesV1beta1) {
					if(ingressNameSet.contains(ingressRow.getMetadata().getName())) {
						throw new CocktailException(String.format("Ingress [%s] already exists.", ingressRow.getMetadata().getName()), ExceptionType.IngressNameAlreadyExists);
					}
				}
			}
			List<V1Ingress> networkingIngressesV1 = ingressSpecService.getIngressesNetworkingV1(cluster, servicemap.getNamespaceName(), null, null);
			if (CollectionUtils.isNotEmpty(networkingIngressesV1)) {
				for (V1Ingress ingressRow : networkingIngressesV1) {
					if(ingressNameSet.contains(ingressRow.getMetadata().getName())) {
						throw new CocktailException(String.format("Ingress [%s] already exists.", ingressRow.getMetadata().getName()), ExceptionType.IngressNameAlreadyExists);
					}
				}
			}

			// Persistent Volume Claim 중복 체크.
			List<V1PersistentVolumeClaim> v1Pvcs = persistentVolumeService.getPersistentVolumeClaimsV1(cluster, servicemap.getNamespaceName(), null, null);
			if (CollectionUtils.isNotEmpty(v1Pvcs)) {
				for (V1PersistentVolumeClaim pvcRow : v1Pvcs) {
					if(pvcNameSet.contains(pvcRow.getMetadata().getName())) {
						throw new CocktailException(String.format("Persistent Volume Claim [%s] already exists.", pvcRow.getMetadata().getName()), ExceptionType.IngressNameAlreadyExists);
					}
				}
			}

			// Role 중복 체크.
			List<V1Role> v1Roles = rbacResourceService.getRolesV1(cluster, servicemap.getNamespaceName(), null, null);
			if (CollectionUtils.isNotEmpty(v1Roles)) {
				for (V1Role roleRow : v1Roles) {
					if(roleNameSet.contains(roleRow.getMetadata().getName())) {
						throw new CocktailException(String.format("Role [%s] already exists.", roleRow.getMetadata().getName()), ExceptionType.TemplateDeploymentFail);
					}
				}

			}
			// Role Binding 중복 체크.
			List<V1RoleBinding> v1RoleBindings = rbacResourceService.getRoleBindingsV1(cluster, servicemap.getNamespaceName(), null, null);
			if (CollectionUtils.isNotEmpty(v1RoleBindings)) {
				for (V1RoleBinding roleBindingRow : v1RoleBindings) {
					if(roleBindingNameSet.contains(roleBindingRow.getMetadata().getName())) {
						throw new CocktailException(String.format("RoleBinding [%s] already exists.", roleBindingRow.getMetadata().getName()), ExceptionType.TemplateDeploymentFail);
					}
				}
			}

			// Service Account 중복 체크
			List<V1ServiceAccount> v1ServiceAccounts = rbacResourceService.getServiceAccountsV1(cluster, servicemap.getNamespaceName(), null, null);
			if (CollectionUtils.isNotEmpty(v1RoleBindings)) {
				for (V1ServiceAccount serviceAccountRow : v1ServiceAccounts) {
					if(serviceAccountNameSet.contains(serviceAccountRow.getMetadata().getName())) {
						throw new CocktailException(String.format("Service Account [%s] already exists.", serviceAccountRow.getMetadata().getName()), ExceptionType.TemplateDeploymentFail);
					}
				}
			}

			// Custom Object 중복 체크.
/** Custom Object 마다 목록 조회를 해야해서 비효율적.. -> Refactoring... ====================
			List<V1beta1CustomResourceDefinition> crds = k8sResourceService.getCustomResourceDefinitionV1beta1(cluster, appmap.getNamespaceName(), null, null);
			for(CommonYamlVO customObj : customObjects) {
				// Custom Resource Definition에서 매칭되는 리소스를 찾음 (Custom Object 조회를 위해 해당 정보가 필요함)
				for(V1beta1CustomResourceDefinition crd : Optional.ofNullable(crds).orElseGet(() ->Lists.newArrayList())) {
					V1beta1CustomResourceDefinitionSpec v1Spec = crd.getSpec();
					if(StringUtils.isBlank(v1Spec.getGroup()) ||
						StringUtils.isBlank(v1Spec.getVersion()) ||
						v1Spec.getNames() == null || StringUtils.isBlank(v1Spec.getNames().getPlural())) {
						// Parameter 누락시 Skip..
						continue;
					}
					// 매칭되는 리소스를 찾으면 해당 CustomObject 목록을 조회하여 중복 체크.
					if(StringUtils.equals(customObj.getCustomObjectKind(), crd.getSpec().getNames().getKind())) {
						// 배포시 사용을 위해 Custom Resource Definition 정보를 보관
						customObj.setCustomObjectGroup(v1Spec.getGroup());
						customObj.setCustomObjectVersion(v1Spec.getVersion());
						customObj.setCustomObjectPlural(v1Spec.getNames().getPlural());
						// Custom Object 목록을 조회 후 중복 체크.
						List<Map<String, Object>> customObjects = k8sResourceService.getCustomObjects(cluster,
							appmap.getNamespaceName(),
							v1Spec.getGroup(),
							v1Spec.getVersion(),
							v1Spec.getNames().getPlural(),
							null);

						Set<String> currentNameSet = customObjects.stream().map(co -> ((Map<String,Object)co.get(KubeConstants.META)).get(KubeConstants.NAME)).collect(Collectors.toSet());

						if(currentNameSet.contains(customObj.getName())) {
							throw new CocktailException(String.format("Custom Object [%s] already exists.", customObj.getName()), ExceptionType.TemplateDeploymentFail);
						}
					}
				}
			}
*/
			if (CollectionUtils.isNotEmpty(crds)) {
				for (K8sCRDResultVO crd : crds) {
					if (!StringUtils.equals("Namespaced", crd.getScope())) {
						// Scope가 Namespace가 아니면 Skip..
						continue;
					}
					if(StringUtils.isBlank(crd.getGroup()) ||
							CollectionUtils.isNotEmpty(crd.getVersions()) ||
							crd.getAcceptedNames() == null ||
							(crd.getAcceptedNames() != null && StringUtils.isBlank(crd.getAcceptedNames().getPlural()))) {
						// Parameter 누락시 Skip..
						continue;
					}
					// 매칭되는 Custom Object가 있으면 조회하여 확인..
					for (CommonYamlVO customObj : customObjects) {
						if (StringUtils.equals(customObj.getCustomObjectKind(), crd.getAcceptedNames().getKind())) {
							// Custom Object 목록을 조회 후 중복 체크.
							List<Map<String, Object>> currentCustomObject = crdResourceService.getCustomObjects(
									cluster
									, servicemap.getNamespaceName()
									, crd.getGroup()
									, crd.getStoredVersion()
									, crd.getAcceptedNames().getPlural()
									, null);

							// 중복 체크를 위해 현재 Custom Object Type에 매칭되는 이름셋만 따로 추출..
							Set<String> currentNameSet = customObjectNameSet.stream().filter(co -> co.getLeft().equals(customObj.getCustomObjectKind())).map(co -> co.getRight().toString()).collect(Collectors.toSet());
							for (Map<String, Object> customObjectRow : Optional.ofNullable(currentCustomObject).orElseGet(() ->Lists.newArrayList())) {
								Map<String, Object> meta = (Map<String, Object>) customObjectRow.get(KubeConstants.META);
								if (currentNameSet.contains(MapUtils.getString(meta, KubeConstants.NAME, ""))) {
									throw new CocktailException(String.format("Custom Object [%s] already exists.", MapUtils.getString(meta, KubeConstants.NAME, "")), ExceptionType.TemplateDeploymentFail);
								}
							}

							// 배포시 사용을 위해 Custom Resource Definition 정보를 보관
							customObj.setCustomObjectGroup(crd.getGroup());
							customObj.setCustomObjectVersion(crd.getStoredVersion());
							customObj.setCustomObjectPlural(crd.getAcceptedNames().getPlural());
						}
					}
				}
			}

			// Package 중복 체크.
			List<HelmReleaseBaseVO> packageList = packageInfoService.getPackages(cluster.getClusterSeq(), servicemap.getNamespaceName(), null);
			if (CollectionUtils.isNotEmpty(packageList)) {
				for (HelmReleaseBaseVO pkg : packageList) {
					if(packageNameSet.contains(pkg.getName())) {
						throw new CocktailException(String.format("Service Account [%s] already exists.", pkg.getName()), ExceptionType.PackageNameAlreadyExists);
					}
				}
			}
		}
		else{
			throw new CocktailException("Invalid Launch Type ", ExceptionType.TemplateDeploymentFail);
		}

		/**
		 * 03. Deployment.
		 */
		if (servicemap == null) {
			if(StringUtils.equals(LaunchType.ADD.getType(), templateLaunch.getLaunchType())) {
				throw new CocktailException("Could not found exist Servciemap", ExceptionType.TemplateDeploymentFail);
			}
			else {
				throw new CocktailException("Could not found target Servicemap", ExceptionType.TemplateDeploymentFail);
			}
		}
		// hjchoi 2022.04.12 플랫폼 레지스트리 pull 사용자로 사용
//		// 해당 서비스의 service registry(project)와 container image registry(project)가 다른 project가 있다면
//		// project 별로 member 추가
//		if(CollectionUtils.isNotEmpty(projectSet)){
//			List<Integer> projects = new ArrayList<>(projectSet);
//			harborRegistryService.addMembersToProjects(projects, users, false);
//		}
		// catalog에서 배포 flag 셋팅
		context.setCatalogYn("Y");

		cluster.setNamespaceName(servicemap.getNamespaceName());
		/**
		 * Service Account 생성
		 */
		if(CollectionUtils.isNotEmpty(services)){
			/** @Sync **/
			rbacResourceService.createMultipleServiceAccount(cluster, servicemap.getNamespaceName(), serviceAccounts);
		}

		/**
		 * Role 생성
		 */
		if(CollectionUtils.isNotEmpty(roles)){
			/** @Sync **/
			rbacResourceService.createMultipleRole(cluster, servicemap.getNamespaceName(), roles);
		}

		/**
		 * Role Binding 생성
		 */
		if(CollectionUtils.isNotEmpty(roleBindings)){
			/** @Sync **/
			rbacResourceService.createMultipleRoleBinding(cluster, servicemap.getNamespaceName(), roleBindings);
		}

		/**
		 * Persistent Volume Claim 생성
		 */
		if(CollectionUtils.isNotEmpty(pvcs)){
			/** @Sync **/
			persistentVolumeService.createMultiplePersistentVolumeClaim(cluster, pvcs, context);
		}

		/**
		 * ConfigMap 생성
		 */
		if(CollectionUtils.isNotEmpty(configMaps)){
			/** @Sync **/
			configMapService.createMultipleConfigMap(servicemap.getServicemapSeq(), configMaps);
		}

		/**
		 * Secret 생성
		 */
		if(CollectionUtils.isNotEmpty(secrets)){
			/** @Sync **/
			secretService.createMultipleSecret(servicemap.getServicemapSeq(), secrets);
		}

		/**
		 * NetAttachDef 생성
		 */
		if(CollectionUtils.isNotEmpty(netAttachDefs)){
			/** @Sync **/
			crdResourceService.createMultipleNetworkAttachmentDefinition(servicemap.getServicemapSeq(), K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, netAttachDefs);
		}

		/**
		 * Custom Object 생성
		 */
		if(CollectionUtils.isNotEmpty(customObjects)){
			/** @Sync **/
			crdResourceService.createMultipleCustomObject(servicemap.getServicemapSeq(), customObjects);
		}

		/**
		 * Service 생성
		 */
		if(CollectionUtils.isNotEmpty(services)){
			/** @Sync (Ingress 생성 전 생성이 완료되어야 하므로 Sync로 처리 **/
			this.createMultipleServices(cluster, servicemap.getNamespaceName(), context, services);
		}

		/**
		 * Ingress 생성
		 */
		if(CollectionUtils.isNotEmpty(ingresses)){
			/** @Sync **/
			this.createMultipleIngresses(cluster, servicemap.getNamespaceName(), context, ingresses);
		}

		/** =====================================================================================================
		 * for Audit Logging
		 * - 카탈로그로 배포된 리소스들에 대한 개별 Audit Log 적재를 위해 목록을 구성
		 * - Exception 없이 본 단계로 진입시 Success로 간주 (TODO : 배포중 실패한 케이스에 대해 처리방안 고민 필요..)
		 * - Workload와 Package는 Async 처리이므로 별도 처리가 필요함)
		 */
		try {
			Map<String, Object> auditAdditionalDatas = new HashMap<>();
			/** Sync로 생성된 리소스들은 auditAdditionalDatas에 입력하여 Audit Logger의 Additional Logging 시 기록 되도록 한다. **/
			if(StringUtils.equals(LaunchType.NEW.getType(), templateLaunch.getLaunchType())){
				auditAdditionalDatas.put(ResourceType.SERVICEMAP.getCode(), servicemap);
			}
			if (CollectionUtils.isNotEmpty(configMaps)) {
				log.debug("======================== SET SERVICE_ACCOUNT ==============================");
				auditAdditionalDatas.put(K8sApiKindType.SERVICE_ACCOUNT.getCode(), serviceAccounts);
			}
			if (CollectionUtils.isNotEmpty(configMaps)) {
				log.debug("======================== SET ROLE =========================================");
				auditAdditionalDatas.put(K8sApiKindType.ROLE.getCode(), roles);
			}
			if (CollectionUtils.isNotEmpty(configMaps)) {
				log.debug("======================== SET ROLE_BINDING =================================");
				auditAdditionalDatas.put(K8sApiKindType.ROLE_BINDING.getCode(), roleBindings);
			}
			if (CollectionUtils.isNotEmpty(configMaps)) {
				log.debug("======================== SET PERSISTENT_VOLUME_CLAIM ======================");
				auditAdditionalDatas.put(K8sApiKindType.PERSISTENT_VOLUME_CLAIM.getCode(), pvcs);
			}
			if (CollectionUtils.isNotEmpty(configMaps)) {
				log.debug("======================== SET CONFIG_MAP ===================================");
				auditAdditionalDatas.put(K8sApiKindType.CONFIG_MAP.getCode(), configMaps);
			}
			if (CollectionUtils.isNotEmpty(secrets)) {
				log.debug("======================== SET SECRET =======================================");
				auditAdditionalDatas.put(K8sApiKindType.SECRET.getCode(), secrets);
			}
			if (CollectionUtils.isNotEmpty(netAttachDefs)) {
				log.debug("======================== SET NETWORK_ATTACHMENT_DEFINITION ================");
				auditAdditionalDatas.put(K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION.getCode(), netAttachDefs);
			}
			if (CollectionUtils.isNotEmpty(configMaps)) {
				log.debug("======================== SET CUSTOM_OBJECT ================================");
				auditAdditionalDatas.put(K8sApiKindType.CUSTOM_OBJECT.getCode(), customObjects);
			}
			if (CollectionUtils.isNotEmpty(configMaps)) {
				log.debug("======================== SET SERVICE ======================================");
				auditAdditionalDatas.put(K8sApiKindType.SERVICE.getCode(), services);
			}
			if (CollectionUtils.isNotEmpty(configMaps)) {
				log.debug("======================== SET INGRESS ======================================");
				auditAdditionalDatas.put(K8sApiKindType.INGRESS.getCode(), ingresses);
			}
			ContextHolder.auditProcessingDatas().put(CommonConstants.AUDIT_ADDITIONAL_DATAS, auditAdditionalDatas);

			/** Workload 및 Package에 대한 Audit Logging은 별도 처리.. **/
			// ContextHolder의 Lifecycle이 Requet객체와 같아 Async 처리중 객체가 유실되므로 Context에 데이터를 복제하여 Audit 로깅에 사용..
			context.getParams().put(CommonConstants.AUDIT_REQUEST_URI, Utils.getCurrentRequest().getRequestURI());
			context.getParams().put(CommonConstants.AUDIT_HTTP_METHOD, Utils.getCurrentRequest().getMethod());
			context.getParams().put(CommonConstants.AUDIT_REQUEST_REFERER, Utils.getCurrentRequest().getHeader(CommonConstants.AUDIT_REQUEST_REFERER));
			context.getParams().put(CommonConstants.AUDIT_REQUEST_CLIENT_IP, Utils.getClientIp());
			context.getParams().put(CommonConstants.AUDIT_CLASS_NAME, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_CLASS_NAME));
			context.getParams().put(CommonConstants.AUDIT_METHOD_NAME, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_METHOD_NAME));
			context.getParams().put(CommonConstants.AUDIT_USER_SEQ, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_USER_SEQ));
			context.getParams().put(CommonConstants.AUDIT_USER_SERVICE_SEQ, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_USER_SERVICE_SEQ));
			context.getParams().put(CommonConstants.AUDIT_USER_ROLE, ContextHolder.auditProcessingDatas().get(CommonConstants.AUDIT_USER_ROLE));
			log.debug("====== Finished setting Audit Data for Resources processed by Async. ======");
		}
		catch (Exception ex) {
			// Audit Logging 관련 처리중 오류 발생시 서비스에 영향도 없도록 로그 남기고 계속..
			if(log.isDebugEnabled()) {
				log.debug("trace log ", ex);
				log.debug("@@CATCH : Audit Logging Failed (deployTemplate)");
			}
			log.error("@@CATCH : Audit Logging Failed (deployTemplate)", ex);
		}
		/** ==================================================================================================**/

		/**
		 * Workload 생성
		 */
		Thread.sleep(200);
		if(CollectionUtils.isNotEmpty(serverAddKubes)) {
			/** @Async **/
			serverService.addMultipleWorkload(serverAddKubes, cluster, context);
		}

		/**
		 * Package 생성
		 */
		if(CollectionUtils.isNotEmpty(packages)){
			/** @Async **/
			packageAsyncService.installMultiplePackage(cluster.getClusterSeq(), templateLaunch.getServiceSeq(), servicemap.getServicemapName(), cluster.getNamespaceName(), context, packages);
		}

		return servicemap;
	}

	/**
	 * ServerYamlVO의 Yaml String 안에 Group 정보를 추가로 설정..
	 * @param serverYaml
	 * @param groupNo
	 * @throws Exception
	 */
	private void setGroupToWorkloadYaml(ServerYamlVO serverYaml, String groupNo) throws Exception {
		if(serverYaml == null) {
			throw new CocktailException("Workload is null!!", ExceptionType.TemplateDeploymentFail);
		}
		switch(WorkloadType.valueOf(serverYaml.getWorkloadType())) {
			case SINGLE_SERVER:
			case REPLICA_SERVER: {
				Object k8sObj = ServerUtils.unmarshalYaml(serverYaml.getYaml(), K8sApiKindType.DEPLOYMENT);
				V1Deployment v1Deployment = (V1Deployment) k8sObj;
				Map<String, String> annotations = Optional.ofNullable(v1Deployment.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
				annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, groupNo);
				v1Deployment.getMetadata().setAnnotations(annotations);

				serverYaml.setYaml(ServerUtils.marshalYaml(v1Deployment));
				break;
			}
			case STATEFUL_SET_SERVER: {
				Object k8sObj = ServerUtils.unmarshalYaml(serverYaml.getYaml(), K8sApiKindType.STATEFUL_SET);
				V1StatefulSet statefulSet = (V1StatefulSet) k8sObj;
				Map<String, String> annotations = Optional.ofNullable(statefulSet.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
				annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, groupNo);
				statefulSet.getMetadata().setAnnotations(annotations);

				serverYaml.setYaml(ServerUtils.marshalYaml(statefulSet));
				break;
			}
			case DAEMON_SET_SERVER: {
				Object k8sObj = ServerUtils.unmarshalYaml(serverYaml.getYaml(), K8sApiKindType.DAEMON_SET);
				V1DaemonSet daemonSet = (V1DaemonSet) k8sObj;
				Map<String, String> annotations = Optional.ofNullable(daemonSet.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
				annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, groupNo);
				daemonSet.getMetadata().setAnnotations(annotations);

				serverYaml.setYaml(ServerUtils.marshalYaml(daemonSet));
				break;
			}
			case JOB_SERVER: {
				Object k8sObj = ServerUtils.unmarshalYaml(serverYaml.getYaml(), K8sApiKindType.JOB);
				V1Job job = (V1Job) k8sObj;
				Map<String, String> annotations = Optional.ofNullable(job.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
				annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, groupNo);
				job.getMetadata().setAnnotations(annotations);

				serverYaml.setYaml(ServerUtils.marshalYaml(job));
				break;
			}
			case CRON_JOB_SERVER: {
				Object k8sObj = ServerUtils.unmarshalYaml(serverYaml.getYaml(), K8sApiKindType.CRON_JOB);

				Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(k8sObj, K8sJsonUtils.getJson());
				K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

				if (K8sApiType.V1BETA1 == apiType) {
					V1beta1CronJob cronJob = (V1beta1CronJob) k8sObj;
					Map<String, String> annotations = Optional.ofNullable(cronJob.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
					annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, groupNo);
					cronJob.getMetadata().setAnnotations(annotations);

					serverYaml.setYaml(ServerUtils.marshalYaml(cronJob));
				} else {
					V1CronJob cronJob = (V1CronJob) k8sObj;
					Map<String, String> annotations = Optional.ofNullable(cronJob.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
					annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, groupNo);
					cronJob.getMetadata().setAnnotations(annotations);

					serverYaml.setYaml(ServerUtils.marshalYaml(cronJob));
				}
				break;
			}
		}
	}

	/**
	 * Create Multiple Services (Invoke from Snapshot Deployment)
	 * @param services
	 * @throws Exception
	 */
	private void createMultipleServices(ClusterVO cluster, String namespace, ExecutingContextVO context, List<ServiceSpecIntegrateVO> services) throws Exception {
		/** Validation시 순환참조 우려가 있어 K8sResourceService에 Business Logic을 구현하지 않고 이곳에서 처리.. **/
		for(ServiceSpecIntegrateVO serviceRow : services){
			if(DeployType.valueOf(serviceRow.getDeployType()) == DeployType.GUI) {
				ServiceSpecGuiVO serviceSpecGui = null;
				try {
					serviceSpecGui = (ServiceSpecGuiVO) serviceRow;
					this.serviceSpecValidationCheck(cluster, namespace, serviceSpecGui);
					serviceSpecService.createService(cluster, namespace, serviceSpecGui, context);
					Thread.sleep(100);
				}
				catch (Exception ex) {
					// 실패시 Log를 남기고 다음 처리를 계속한다..
					log.error(String.format("Service Deployment Failure : deployTemplate : %s\n%s", ex.getMessage(), ex, JsonUtils.toGson(serviceSpecGui)));
				}
			}
			else if(DeployType.valueOf(serviceRow.getDeployType()) == DeployType.YAML) {
				ServiceSpecYamlVO serviceSpecYaml = null;
				try {
					serviceSpecYaml = (ServiceSpecYamlVO) serviceRow;
					/** GUI로 변환하여 Validation 체크만 진행. **/
					ServiceSpecGuiVO tempService = serverConversionService.convertYamlToServiceSpec(cluster, serviceSpecYaml.getYaml());
					this.serviceSpecValidationCheck(cluster, namespace, tempService);
					/** Service 생성.. **/
					V1Service v1Service = ServerUtils.unmarshalYaml(serviceSpecYaml.getYaml(), K8sApiKindType.SERVICE);
					serviceSpecService.createServiceV1(cluster, namespace, v1Service, context);
					Thread.sleep(100);
				}
				catch (Exception ex) {
					// 실패시 Log를 남기고 다음 처리를 계속한다..
					log.error(String.format("Service Deployment Failure : deployTemplate : %s\n%s", ex.getMessage(), ex, JsonUtils.toGson(serviceSpecYaml)));
				}
			}
			else {
				log.error(String.format("Invalid DeployType : deployTemplate : %s", JsonUtils.toGson(serviceRow)));
			}
		}
	}

	/**
	 * Create Multiple Ingresses (Invoke from Snapshot Deployment)
	 * @param ingresses
	 * @throws Exception
	 */
	private void createMultipleIngresses(ClusterVO cluster, String namespace, ExecutingContextVO context, List<IngressSpecIntegrateVO> ingresses) {
		/** Validation시 순환참조 우려가 있어 K8sResourceService에 Business Logic을 구현하지 않고 이곳에서 처리.. **/
		for(IngressSpecIntegrateVO ingress : ingresses){
			if(DeployType.valueOf(ingress.getDeployType()) == DeployType.GUI) {
				IngressSpecGuiVO ingressSpecGui = null;

				try {
					ingressSpecGui = (IngressSpecGuiVO) ingress;
					ingressSpecService.checkIngress(cluster, namespace, true, ingressSpecGui);
					ingressSpecService.createIngress(cluster, namespace, ingressSpecGui, null, context);
					Thread.sleep(100);

				} catch (Exception ex) {
					// 실패시 Log를 남기고 다음 처리를 계속한다..
					log.error(String.format("Ingress Deployment Failure : deployTemplate : %s\n%s", ex.getMessage(), ex, JsonUtils.toGson(ingressSpecGui)));
				}
			}
			else if(DeployType.valueOf(ingress.getDeployType()) == DeployType.YAML) {
				IngressSpecYamlVO ingressSpecYaml = null;

				try {
					ingressSpecYaml = (IngressSpecYamlVO) ingress;
					/** GUI로 변환하여 Validation 체크만 진행. **/
					IngressSpecGuiVO tempIngress = ingressSpecService.convertIngressSpecYamlToGui(cluster, null, namespace, ingressSpecYaml.getYaml());

                    if (tempIngress == null) {
                        continue;
                    }

					ingressSpecService.checkIngress(cluster, namespace, true, tempIngress);
					/** Ingress 생성.. **/
					ingressSpecService.createIngress(cluster, namespace, ingressSpecYaml.getYaml(), context);
					Thread.sleep(100);
				} catch (Exception ex) {
					// 실패시 Log를 남기고 다음 처리를 계속한다..
					log.error(String.format("Ingress Deployment Failure : deployTemplate : %s\n%s", ex.getMessage(), ex, JsonUtils.toGson(ingressSpecYaml)));
				}
			}
			else {
				log.error(String.format("Invalid DeployType : deployTemplate : %s", JsonUtils.toGson(ingress)));
			}
		}
	}

	/**
	 * Name 기본 Validation 체크 및 NameSet 저장.
	 * @param name
	 * @param nameSet
	 * @param type
	 * @throws Exception
	 */
	private void addNameSet(String name, Set<String> nameSet, String type) throws Exception {
		if(StringUtils.isBlank(name)) {
			throw new CocktailException(String.format("%s Name is Required!!", type), ExceptionType.TemplateDeploymentFail);
		}
		if(nameSet.contains(name)) {
			throw new CocktailException(String.format("Duplicate %s Name!!", type), ExceptionType.TemplateDeploymentFail);
		}
		nameSet.add(name);
	}

	/**
	 * Service Validation Check
	 * @param cluster
	 * @param namespaceName
	 * @param serviceRow
	 * @throws Exception
	 */
	private void serviceSpecValidationCheck(ClusterVO cluster, String namespaceName, ServiceSpecGuiVO serviceRow) throws Exception {
		if (serviceRow == null) {
			throw new CocktailException("Service is null!!", ExceptionType.TemplateDeploymentFail);
		}
		if (StringUtils.isBlank(serviceRow.getName()) || !serviceRow.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
			throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
		}
		K8sServiceVO k8sService = serviceSpecService.getService(cluster, namespaceName, serviceRow.getName(), ContextHolder.exeContext());
		if (k8sService != null) {
			throw new CocktailException("Service already exists!!", ExceptionType.ServiceNameAlreadyExists);
		}
		if (StringUtils.isBlank(serviceRow.getServiceType()) || PortType.findPortName(serviceRow.getServiceType()) == null) {
			throw new CocktailException("ServiceType is invalid!", ExceptionType.InvalidInputData);
		}
		Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, null, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
		List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
		serviceSpecs.add(serviceRow);
		serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

        if (!K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_19)) {
            // SCTP 프로토콜 사용여부 및 클러스터 지원여부 체크
            boolean useStcp = serviceRow.getServicePorts().stream().anyMatch(sp -> "SCTP".equalsIgnoreCase(sp.getProtocol()));

            if (useStcp) {
                Map<String, Boolean> featureGates = k8sResourceService.getFeatureGates(cluster);
                if (MapUtils.isEmpty(featureGates)) {
                    throw new CocktailException("SCTP Protocol Not Supported.", ExceptionType.ProtocolNotSupported);
                } else {
                    if (!MapUtils.getBooleanValue(featureGates, "SCTPSupport", false)) {
                        throw new CocktailException("SCTP Protocol Not Supported.", ExceptionType.ProtocolNotSupported);
                    }
                }
            }
        }
	}

	/**
	 * Gate of Template Validation Check
	 * @param apiVersion
	 * @param templateValidRequest
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	public List<TemplateValidResponseVO> validTemplate(String apiVersion, TemplateValidRequestVO templateValidRequest, ExecutingContextVO ctx) throws Exception {
		try {
			switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
				case V2:
					return this.validTemplateV2(templateValidRequest, ctx);
				default:
					throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
			}
		}
		catch (Exception ex) {
			if(log.isDebugEnabled()) log.debug("trace log ", ex);
			throw new CocktailException(String.format("An error occurred during template deployment. [%s]", ex.getMessage()), ex, ExceptionType.TemplateDeploymentFail);
		}
	}

	private String getNameFromMetadata(V1ObjectMeta objectMeta) throws Exception {
		if(objectMeta == null) {
			throw new CocktailException("Invalid YAML Format (Metadata is null)", ExceptionType.InvalidYamlData);
		}
		if(StringUtils.isBlank(objectMeta.getName())) {
			throw new CocktailException("Invalid YAML Format (Resource name is null)", ExceptionType.InvalidYamlData);
		}
		return objectMeta.getName();
	}

	@SuppressWarnings("unchecked")
	public List<TemplateValidResponseVO> validTemplateV2(TemplateValidRequestVO templateValidRequest, ExecutingContextVO ctx) throws Exception {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		ServicemapDetailVO servicemapDetail = null;
		Set<String> configMapNameSet = new HashSet<>();         // configMap명 중복제거
		Set<String> secretNameSet = new HashSet<>();            // secret명 중복제거
		Set<String> netAttachDefNameSet = new HashSet<>();      // netAttachDef명 중복제거
		Set<String> componentNameSet = new HashSet<>();         // 컴포넌트명 중복제거
		Set<String> hpaNameSet = new HashSet<>();               // HPA명 중복제거
		Set<String> serviceNameSet = new HashSet<>();           // 서비스명 중복제거
		Set<String> ingressNameSet = new HashSet<>();           // 인그레스명 중복 제거
		Set<String> pvcNameSet = new HashSet<>();               // pvc명 중복제거
		Set<String> serviceAccountNameSet = new HashSet<>();    // serviceAccount명 중복제거
		Set<String> roleNameSet = new HashSet<>();              // role명 중복제거
		Set<String> roleBindingNameSet = new HashSet<>();       // roleBinding명 중복제거
		Set<Pair> customObjectNameSet = new HashSet<>();        // customObject명 중복제거
		Set<String> packageNameSet = new HashSet<>();           // Package 중복 제거

		Set<String> persistentVolumeClaimRWOWithMountNameSet = new HashSet<>(); // PVC명 RWO accessMode & Mount
		Set<String> persistentVolumeClaimRWONameSet = new HashSet<>(); // PVC명 RWO accessMode
		Set<String> ContainerVolumeRWOSet = new HashSet<>(); // Container PVC명 RWO accessMode

		List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = new ArrayList<>();

		// 클러스터 조회
		ClusterVO cluster = this._getClusterInfo("%s", templateValidRequest.getClusterSeq());

		boolean gpuUseYn = addonCommonService.getSupported(cluster.getClusterSeq(), AddonConstants.CHART_NAME_GPU);
		boolean multiNicUseYn = addonCommonService.getSupported(cluster.getClusterSeq(), AddonConstants.CHART_NAME_MULTI_NIC);
		boolean sriovUseYn = addonCommonService.getSupported(cluster.getClusterSeq(), AddonConstants.CHART_NAME_SR_IOV);
		Map<String, Boolean> featureGates = k8sResourceService.getFeatureGates(cluster);

		/**
		 * 01. Servicemap 존재시 기 배포된 리소스를 조회하여 각각의 NameSet 구성
		 **/
		if(templateValidRequest.getServicemapSeq() != null && templateValidRequest.getServicemapSeq() > 0) {
			servicemapDetail = servicemapDao.getServicemapDetail(templateValidRequest.getServicemapSeq(), templateValidRequest.getServiceSeq());

			if(servicemapDetail != null) {
				cluster.setNamespaceName(servicemapDetail.getNamespaceName());
                // TODO: 2020.04.19, hjchoi 사용하지 않는 것으로 판단되어 주석처리 추후 필요하다면 수정필요
//				cluster.setServicemapSeq(servicemapDetail.getServicemapSeq());

				/**
				 * DEPLOYMENT
				 */
				if(CollectionUtils.isNotEmpty(servicemapDetail.getComponents())) {
					// 조회한 해당 앱맵의 컴포넌트명 셋팅
					componentNameSet = servicemapDetail.getComponents().stream().map(ComponentVO::getComponentName).collect(Collectors.toSet());
				}
				// Cluster로 부터 조회한 워크로드명 추가 셋팅.
				componentNameSet.addAll(Optional.ofNullable(this.getWorkloadNameSet(cluster)).orElseGet(() ->new HashSet<>()));
			}

/**
 *  Stop된 워크로드 안에 있는 HPA 이름의 중복 체크까지 처리 여부 (주석 처리 : Stop된 워크로드를 재시작할때 알림을 주는 방향으로 정리함)
 *

			// 조회한 해당 앱맵의 Autoscaler명 셋팅 (STOP되어 있는 Snapshot 형태의 정보도 비교하기 위함)
			if(appmap != null) {
				if (CollectionUtils.isNotEmpty(appmap.getComponents())) {
					for (ComponentVO comp4Hpa : appmap.getComponents()) {
						// 각 Component(워크로드의 Manifest 파일을 parsing 하여 HPA 정보를 조회한 후 Name set을 구성)
						List<Object> objs = ServerUtils.getYamlObjects(comp4Hpa.getWorkloadManifest());
						ServerGuiVO serverGui4Hpa = serverConversionService.convertYamlToGui(null, null, comp4Hpa.getWorkloadType(), null, null, null, objs);
						if (serverGui4Hpa.getServer() != null
							&& serverGui4Hpa.getServer().getHpa() != null
							&& CollectionUtils.isNotEmpty(serverGui4Hpa.getServer().getHpa().getMetrics())) {
							hpaNameSet.add(serverGui4Hpa.getServer().getHpa().getName());
						}
					}
				}
			}
*/
			/**
			 * Horizontal Pod Autoscaler 중복 체크를 위한 로직 추가 : 2020.10.19
			 */
			// Cluster-namespace에 해당하는 전체 hpa name set을 생성..
			hpaNameSet.addAll(Optional.ofNullable(this.getHpaNameSet(cluster)).orElseGet(() ->new HashSet<>()));

			/**
			 * CONFIG_MAP
			 */
			List<ConfigMapGuiVO> configMaps = configMapService.getConfigMaps(templateValidRequest.getServicemapSeq());
			if(CollectionUtils.isNotEmpty(configMaps)){
				configMapNameSet = configMaps.stream().map(ConfigMapGuiVO::getName).collect(Collectors.toSet());
			}

			/**
			 * SECRET
			 */
			List<SecretGuiVO> secrets = secretService.getSecrets(templateValidRequest.getServicemapSeq(), true);
			if(CollectionUtils.isNotEmpty(secrets)){
				secretNameSet = secrets.stream().map(SecretGuiVO::getName).collect(Collectors.toSet());
			}

			/**
			 * Network Attachment Definition
			 */
			List<Map<String, Object>> results = crdResourceService.getCustomObjects(templateValidRequest.getServicemapSeq(), K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, null);
			List<K8sCRDNetAttachDefGuiVO> netAttachDefs = crdResourceService.getConvertNetAttachDefList(results);
			if(CollectionUtils.isNotEmpty(netAttachDefs)){
				netAttachDefNameSet = netAttachDefs.stream().map(K8sCRDNetAttachDefGuiVO::getName).collect(Collectors.toSet());
			}

			/**
			 * Service
			 */
			List<V1Service> v1Services = serviceSpecService.getServicesV1(cluster, cluster.getNamespaceName(), null, null, ContextHolder.exeContext());
			if(CollectionUtils.isNotEmpty(v1Services)){
				serviceNameSet = v1Services.stream().map(V1Service::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toSet());
			}

			/**
			 * Ingress
			 */
			List<NetworkingV1beta1Ingress> networkingIngressesV1beta1 = ingressSpecService.getIngressesNetworkingV1Beta1(cluster, cluster.getNamespaceName(), null, null);
			if(CollectionUtils.isNotEmpty(networkingIngressesV1beta1)){
				ingressNameSet = networkingIngressesV1beta1.stream().map(NetworkingV1beta1Ingress::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toSet());
			}

			List<V1Ingress> networkingIngressesV1 = ingressSpecService.getIngressesNetworkingV1(cluster, cluster.getNamespaceName(), null, null);
			if(CollectionUtils.isNotEmpty(networkingIngressesV1)){
				ingressNameSet = networkingIngressesV1.stream().map(V1Ingress::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toSet());
			}

			/**
			 * Persistent Volume Claim
			 */
			List<V1PersistentVolumeClaim> v1Pvcs = persistentVolumeService.getPersistentVolumeClaimsV1(cluster, cluster.getNamespaceName(), null, null);
			if(CollectionUtils.isNotEmpty(v1Pvcs)){
				pvcNameSet = v1Pvcs.stream().map(V1PersistentVolumeClaim::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toSet());
			}

			/**
			 * Role
			 */
			List<V1Role> v1Roles = rbacResourceService.getRolesV1(cluster, cluster.getNamespaceName(), null, null);
			if(CollectionUtils.isNotEmpty(v1Roles)){
				roleNameSet = v1Roles.stream().map(V1Role::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toSet());
			}

			/**
			 * Role Binding
			 */
			List<V1RoleBinding> v1RoleBindings = rbacResourceService.getRoleBindingsV1(cluster, cluster.getNamespaceName(), null, null);
			if(CollectionUtils.isNotEmpty(v1RoleBindings)){
				roleBindingNameSet = v1RoleBindings.stream().map(V1RoleBinding::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toSet());
			}

			/**
			 * Service Account
			 */
			List<V1ServiceAccount> v1ServiceAccounts = rbacResourceService.getServiceAccountsV1(cluster, cluster.getNamespaceName(), null, null);
			if(CollectionUtils.isNotEmpty(v1ServiceAccounts)){
				serviceAccountNameSet = v1ServiceAccounts.stream().map(V1ServiceAccount::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toSet());
			}

			/**
			 * Custom Object
			 */
			Map<String, Map<String, List<Map<String, Object>>>> customObjectMap = this.getCustomObjectOfCRD(cluster, cluster.getNamespaceName());
			if (MapUtils.isNotEmpty(customObjectMap)) {
				for (Map.Entry<String, Map<String, List<Map<String, Object>>>> crdEntry : customObjectMap.entrySet()) {
					for (Map.Entry<String, List<Map<String, Object>>> coEntry : crdEntry.getValue().entrySet()) {
						// Custom Object Nameset 구성..
						for(Map<String, Object> customObjectRow : Optional.ofNullable(coEntry.getValue()).orElseGet(() ->Lists.newArrayList())) {
							Map<String, Object> meta = (Map<String, Object>)customObjectRow.get(KubeConstants.META);
							customObjectNameSet.add(Pair.of(MapUtils.getString(customObjectRow, KubeConstants.KIND, ""), MapUtils.getString(meta, KubeConstants.NAME, "")));
						}
					}
				}
			}

			/**
			 * Package
			 */
			List<HelmReleaseBaseVO> packageList = packageInfoService.getPackages(cluster.getClusterSeq(), cluster.getNamespaceName(), null);
			if(CollectionUtils.isNotEmpty(packageList)){
				packageNameSet = packageList.stream().map(HelmReleaseBaseVO::getName).collect(Collectors.toSet());
			}

			/**
			 * PERSISTENT_VOLUME_LINKED VolumeType PV
			 */
			String labelSelector = String.format("%s,%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, KubeConstants.CUSTOM_VOLUME_TYPE, VolumeType.PERSISTENT_VOLUME_LINKED.getCode());
			persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaimsInServicemap(templateValidRequest.getServicemapSeq(), null, labelSelector, ctx);
			if(CollectionUtils.isNotEmpty(persistentVolumeClaims)){
				persistentVolumeClaimRWOWithMountNameSet = persistentVolumeClaims.stream().filter(pvc -> (StringUtils.equals(AccessMode.RWO.getValue(), pvc.getAccessModes().stream().findFirst().orElseGet(() ->null))) && CollectionUtils.isNotEmpty(pvc.getServerParams())).map(K8sPersistentVolumeClaimVO::getName).collect(Collectors.toSet());
				persistentVolumeClaimRWONameSet = persistentVolumeClaims.stream().filter(pvc -> (StringUtils.equals(AccessMode.RWO.getValue(), pvc.getAccessModes().stream().findFirst().orElseGet(() ->null)))).map(K8sPersistentVolumeClaimVO::getName).collect(Collectors.toSet());
			}
		}

		/**
		 * 02. Start Validation Check
		 **/
		List<TemplateValidResponseVO> validList = new ArrayList<>();
		StringBuilder errMsg = new StringBuilder();
		boolean serverNameValid;
		boolean isPVConfig;
		boolean configMapNameValid;
		boolean secretNameValid;
		boolean netAttachDefNameValid;
		/** 4.0.2 신규 리소스 관련 : 여기부터 **/
		boolean serviceNameValid;
		boolean ingressNameValid;
		boolean pvcNameValid;
		boolean serviceAccountNameValid;
		boolean roleNameValid;
		boolean roleBindingNameValid;
		boolean customObjectNameValid;
		boolean packageNameValid;
		boolean configMapDataValid;
		boolean secretDataValid;
		boolean ingressHostPathValid;
		boolean ingressHostValid;
		boolean ingressPathLongValid;
		boolean ingressHostLongValid;
		boolean ingressNameSpecValid;
		/** 4.0.2 신규 리소스 관련 : 여기까지 **/
		boolean clusterSupported;
		boolean gpuValid;
		boolean sctpValid;
		boolean multiNicValid;
		boolean sriovValid;
		boolean ttlAfterFinishedValid;
		boolean hpaNameValid;
		for (TemplateDeploymentVO deployment : templateValidRequest.getTemplateDeployments()) {
			// 초기화
			serverNameValid = true;
			isPVConfig = true;
			configMapNameValid = true;
			secretNameValid = true;
			netAttachDefNameValid = true;
			/** 4.0.2 신규 리소스 관련 : 여기부터 **/
			serviceNameValid = true;
			ingressNameValid = true;
			pvcNameValid = true;
			serviceAccountNameValid = true;
			roleNameValid = true;
			roleBindingNameValid = true;
			customObjectNameValid = true;
			packageNameValid = true;
			configMapDataValid = true;
			secretDataValid = true;
			ingressHostPathValid = true;
			ingressHostValid = true;
			ingressPathLongValid = true;
			ingressHostLongValid = true;
			ingressNameSpecValid = true;
			/** 4.0.2 신규 리소스 관련 : 여기까지 **/
			clusterSupported = true;
			gpuValid = true;
			sctpValid = true;
			multiNicValid = true;
			sriovValid = true;
			ttlAfterFinishedValid = true;
			hpaNameValid = true;

			errMsg.delete(0, errMsg.capacity());
			errMsg.setLength(0);

			DeployType deployType = DeployType.valueOf(deployment.getDeployType());
			if (deployment.getTemplateDeploymentType() == TemplateDeploymentType.CONFIG_MAP) {
				ConfigMapIntegrateVO configMapIntegrate = null;
				ConfigMapGuiVO configMapRow = null;
				if(deployType == DeployType.GUI) {
					configMapIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<ConfigMapIntegrateVO>() {});
					configMapRow = (ConfigMapGuiVO)configMapIntegrate;
				}
				else if(deployType == DeployType.YAML) {
//					configMapIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<ConfigMapIntegrateVO>() {});
//					ConfigMapYamlVO configMapYaml = (ConfigMapYamlVO)configMapIntegrate;
					V1ConfigMap v1Configmap = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.CONFIG_MAP);
					ConfigMapYamlVO configMapYaml = new ConfigMapYamlVO();
					configMapYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						configMapYaml.setNamespace(servicemapDetail.getNamespaceName());
						configMapYaml.setServicemapSeq(servicemapDetail.getServicemapSeq());
					}
					configMapYaml.setName(this.getNameFromMetadata(v1Configmap.getMetadata()));
					configMapRow = configMapService.convertYamlToConfigMap(cluster, configMapYaml);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				if (configMapRow != null) {
					if (MapUtils.isNotEmpty(configMapRow.getData())) {
						for (Map.Entry<String, String> dataEntry : configMapRow.getData().entrySet()) {
							if (StringUtils.isBlank(dataEntry.getKey()) || StringUtils.isBlank(dataEntry.getValue())) {
								configMapDataValid = false;
							}
						}
					}
					// configMap명 중복 체크
					if (configMapNameSet.contains(configMapRow.getName())) {
						configMapNameValid = false;
					}
					configMapNameSet.add(configMapRow.getName());
				}
				else {
					throw new CocktailException("ConfigMap is null!!", ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.SECRET){
				SecretIntegrateVO secretIntegrate = null;
				SecretGuiVO secretRow = null;
				if(deployType == DeployType.GUI) {
					secretIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<SecretIntegrateVO>() {});
					secretRow = (SecretGuiVO)secretIntegrate;
				}
				else if(deployType == DeployType.YAML) {
//					secretIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<SecretIntegrateVO>() {});
//					SecretYamlVO secretYaml = (SecretYamlVO)secretIntegrate;
					V1Secret v1Secret = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.SECRET);
					SecretYamlVO secretYaml = new SecretYamlVO();
					secretYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						secretYaml.setNamespace(servicemapDetail.getNamespaceName());
					}
					secretYaml.setName(this.getNameFromMetadata(v1Secret.getMetadata()));
					secretRow = secretService.convertYamlToSecret(null, cluster, secretYaml, true);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
				if (secretRow != null) {
					if (MapUtils.isNotEmpty(secretRow.getData())) {
						for (Map.Entry<String, String> dataEntry : secretRow.getData().entrySet()) {
							if (StringUtils.isBlank(dataEntry.getKey()) || StringUtils.isBlank(dataEntry.getValue())) {
								secretDataValid = false;
							}
						}
					}
					// secret명 중복 체크
					if(secretNameSet.contains(secretRow.getName())){
						secretNameValid = false;
					}
					secretNameSet.add(secretRow.getName());
				}
				else {
					throw new CocktailException("Secret is null!!", ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.NET_ATTACH_DEF) {
				K8sCRDNetAttachDefIntegrateVO netAttachDefRowIntegrate = null;
				K8sCRDNetAttachDefGuiVO netAttachDefRow = null;
				if(deployType == DeployType.GUI) {
					netAttachDefRowIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<K8sCRDNetAttachDefIntegrateVO>() {});
					netAttachDefRow = (K8sCRDNetAttachDefGuiVO)netAttachDefRowIntegrate;
				}
				else if(deployType == DeployType.YAML) {
//					netAttachDefRowIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<K8sCRDNetAttachDefIntegrateVO>() {});
//					K8sCRDNetAttachDefYamlVO k8sCRDNetAttachDefYaml = (K8sCRDNetAttachDefYamlVO) netAttachDefRowIntegrate;
					Map<String, Object> yamlMap = Yaml.getSnakeYaml().load(deployment.getTemplateContentYaml());
					K8sCRDNetAttachDefYamlVO k8sCRDNetAttachDefYaml = new K8sCRDNetAttachDefYamlVO();
					k8sCRDNetAttachDefYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						k8sCRDNetAttachDefYaml.setNamespace(servicemapDetail.getNamespaceName());
					}
					Map<String, Object> meta = (Map<String, Object>) MapUtils.getMap(yamlMap, KubeConstants.META, null);
					if(StringUtils.isBlank(MapUtils.getString(meta, KubeConstants.NAME, ""))) {
						throw new CocktailException("Invalid YAML Format (Resource name is null)", ExceptionType.InvalidYamlData);
					}
					k8sCRDNetAttachDefYaml.setName(MapUtils.getString(meta, KubeConstants.NAME, ""));

					netAttachDefRow = new K8sCRDNetAttachDefGuiVO();
					Map<String, Object> netAttachDefMap = Yaml.getSnakeYaml().load(k8sCRDNetAttachDefYaml.getYaml());
					if (MapUtils.isNotEmpty(netAttachDefMap)) {
						crdResourceService.convertNetAttachDef(netAttachDefMap, netAttachDefRow);
					}
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				if (netAttachDefRow != null) {
					if(netAttachDefNameSet.contains(netAttachDefRow.getName())){
						netAttachDefNameValid = false;
					}
					netAttachDefNameSet.add(netAttachDefRow.getName());
				}
				else {
					throw new CocktailException("Network Attachment Definition is null!!", ExceptionType.TemplateDeploymentFail);
				}

				multiNicValid = multiNicUseYn;
				sriovValid = sriovUseYn;
			}
			else if(deployment.getTemplateDeploymentType().isWorkload()){
				ServerIntegrateVO serverIntegrate = null;
				ServerGuiVO serverAddKubesRow = null;
				if(deployType == DeployType.GUI) {
					serverIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<ServerIntegrateVO>() {});
					serverAddKubesRow = (ServerGuiVO)serverIntegrate;
				}
				else if(deployType == DeployType.YAML) {
//					serverIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<ServerIntegrateVO>() {});
//					ServerYamlVO serverYaml = (ServerYamlVO) serverIntegrate;

					Object k8sObj = null;
					V1ObjectMeta objectMeta = null;
					WorkloadType workloadType = this.getWorkloadTypeAsTemplateDeploymentType(deployment.getTemplateDeploymentType());
					switch(workloadType) {
						case SINGLE_SERVER:
						case REPLICA_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.DEPLOYMENT);
							V1Deployment v1Deployment = (V1Deployment) k8sObj;
							objectMeta = v1Deployment.getMetadata();
							break;
						case STATEFUL_SET_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.STATEFUL_SET);
							V1StatefulSet statefulSet = (V1StatefulSet) k8sObj;
							objectMeta = statefulSet.getMetadata();
							break;
						case DAEMON_SET_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.DAEMON_SET);
							V1DaemonSet daemonSet = (V1DaemonSet) k8sObj;
							objectMeta = daemonSet.getMetadata();
							break;
						case JOB_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.JOB);
							V1Job job = (V1Job) k8sObj;
							objectMeta = job.getMetadata();
							break;
						case CRON_JOB_SERVER:
							k8sObj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.CRON_JOB);
							V1beta1CronJob cronJob = (V1beta1CronJob) k8sObj;
							objectMeta = cronJob.getMetadata();
							break;
					}

					ServerYamlVO serverYaml = new ServerYamlVO();
					serverYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						serverYaml.setNamespaceName(servicemapDetail.getNamespaceName());
					}
					serverYaml.setWorkloadVersion("V1");
					serverYaml.setWorkloadType(workloadType.getCode());
					serverYaml.setClusterSeq(templateValidRequest.getClusterSeq());
					serverYaml.setWorkloadName(this.getNameFromMetadata(objectMeta));

					List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());
					serverAddKubesRow = serverConversionService.convertYamlToGui(null, null, workloadType.getCode(), null, null, null, objs);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				if(serverAddKubesRow == null) {
					throw new CocktailException("Workload Data is null!!", ExceptionType.TemplateDeploymentFail);
				}

				// 컴포넌트명 중복 체크
				if (componentNameSet.contains(serverAddKubesRow.getComponent().getComponentName())) {
					serverNameValid = false;
				}
				componentNameSet.add(serverAddKubesRow.getComponent().getComponentName());

				// HPA명 중복 체크
				if (serverAddKubesRow.getServer() != null
					&& serverAddKubesRow.getServer().getHpa() != null
					&& CollectionUtils.isNotEmpty(serverAddKubesRow.getServer().getHpa().getMetrics())) {
					// HPA명 중복 체크
					if (hpaNameSet.contains(serverAddKubesRow.getServer().getHpa().getName())) {
						hpaNameValid = false;
					}
					hpaNameSet.add(serverAddKubesRow.getServer().getHpa().getName()); // check한 이름도 nameset에 넣어 현재 배포되는 HPA내에서도 중복 체크가 가능하도록 함.
				}

				// workloadType별 클러스터 지원 여부 체크
				try {
					serverValidService.checkServerApiVersion(serverAddKubesRow.getServer().getWorkloadType(), serverAddKubesRow.getServer().getWorkloadVersion(), cluster, ctx);
				}catch (CocktailException e){
					if(e.getType() == ExceptionType.K8sNotSupported){
						clusterSupported = false;
					}else{
						throw e;
					}
				}catch (Exception e){
					throw e;
				}

				// PV 설정 확인
				if(CollectionUtils.isNotEmpty(serverAddKubesRow.getVolumes())){
					for(ContainerVolumeVO containerVolumeRow : serverAddKubesRow.getVolumes()){
						if(containerVolumeRow.getVolumeType() == VolumeType.PERSISTENT_VOLUME
							|| containerVolumeRow.getVolumeType() == VolumeType.PERSISTENT_VOLUME_STATIC){
							if(containerVolumeRow.getClusterVolumeSeq() == null){
								isPVConfig = false;
								break;
							}
						}else if(containerVolumeRow.getVolumeType() == VolumeType.PERSISTENT_VOLUME_LINKED){
							if(persistentVolumeClaimRWOWithMountNameSet.contains(containerVolumeRow.getPersistentVolumeClaimName())){
								isPVConfig = false;
								break;
							}
							if(persistentVolumeClaimRWONameSet.contains(containerVolumeRow.getPersistentVolumeClaimName())){
								if(ContainerVolumeRWOSet.contains(containerVolumeRow.getPersistentVolumeClaimName())){
									isPVConfig = false;
									break;
								}else{
									ContainerVolumeRWOSet.add(containerVolumeRow.getPersistentVolumeClaimName());
								}
							}
						}
					}
				}

				// Multi-nic, Sriov 지원여부 체크
				if (CollectionUtils.isNotEmpty(serverAddKubesRow.getServer().getPodNetworks())) {
					multiNicValid = multiNicUseYn;
					sriovValid = sriovUseYn;
				}

				// GPU 지원여부 체크
				boolean useGpu = false;
				if (CollectionUtils.isNotEmpty(serverAddKubesRow.getContainers())) { // GPU 사용여부
					useGpu = serverAddKubesRow.getContainers().stream().anyMatch(c -> Boolean.TRUE.equals(c.getResources().getUseGpu()));
				}
				if(useGpu){ // 카탈로그에 gpu 데이터를 사용할 경우만 체크
					// 카탈로그는 GPU를 사용하는데, 클러스터가 GPU addon이 없다면 유효성 실패로 셋팅
					gpuValid = gpuUseYn;
				}

				// SCTP 지원여부 체크
				boolean useSctp = false;
                if (!K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_19)) {
                    if (CollectionUtils.isNotEmpty(serverAddKubesRow.getServices())) { // SCTP 사용여부
                        useSctp = serverAddKubesRow.getServices().stream().anyMatch(sp -> Optional.ofNullable(sp.getServicePorts()).orElseGet(() -> Lists.newArrayList()).stream().anyMatch(port -> "SCTP".equalsIgnoreCase(port.getProtocol())));
                    }
                    if (useSctp) {
                        if (MapUtils.isEmpty(featureGates)) {
                            sctpValid = false;
                        } else {
                            if (!MapUtils.getBooleanValue(featureGates, "SCTPSupport", false)) {
                                sctpValid = false;
                            }
                        }
                    }
                } else {
                    sctpValid = true;
                }

				if(WorkloadType.JOB_SERVER == WorkloadType.valueOf(serverAddKubesRow.getServer().getWorkloadType())
					|| WorkloadType.CRON_JOB_SERVER == WorkloadType.valueOf(serverAddKubesRow.getServer().getWorkloadType())) {
					if (serverAddKubesRow.getServer().getTtlSecondsAfterFinished() != null) {
						if (!K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_21)) {
							if (MapUtils.isEmpty(featureGates)) {
								ttlAfterFinishedValid = false;
							} else {
								if (!MapUtils.getBooleanValue(featureGates, "TTLAfterFinished", false)) {
									ttlAfterFinishedValid = false;
								}
							}
						}
					}
				}

				// 컨테이너 지원여부 체크, registry 체크
				errMsg.append(this._checkContainerSupported(serverAddKubesRow, cluster, templateValidRequest.getServiceSeq(), true));
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.SERVICE) {
				ServiceSpecIntegrateVO serviceIntegrate;
				ServiceSpecGuiVO serviceRow;
				if(deployType == DeployType.GUI) {
					serviceIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<ServiceSpecIntegrateVO>() {});
					serviceRow = (ServiceSpecGuiVO)serviceIntegrate;
				}
				else if(deployType == DeployType.YAML) {
//					serviceIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<ServiceSpecIntegrateVO>() {});
//					ServiceSpecYamlVO serviceSpecYaml = (ServiceSpecYamlVO)serviceIntegrate;

					V1Service v1Service = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.SERVICE);
					ServiceSpecYamlVO serviceSpecYaml = new ServiceSpecYamlVO();
					serviceSpecYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						serviceSpecYaml.setNamespaceName(servicemapDetail.getNamespaceName());
						serviceSpecYaml.setServicemapSeq(servicemapDetail.getServicemapSeq());
					}
					serviceSpecYaml.setName(this.getNameFromMetadata(v1Service.getMetadata()));

					serviceRow = serverConversionService.convertYamlToServiceSpec(cluster, serviceSpecYaml.getYaml());
				}
				else {
					throw new CocktailException("Invalid DeployType!!", ExceptionType.TemplateDeploymentFail);
				}

				if(serviceRow == null) {
					throw new CocktailException("Service is null!!", ExceptionType.TemplateDeploymentFail);
				}
				if(serviceNameSet.contains(serviceRow.getName())){
					serviceNameValid = false;
				}
				serviceNameSet.add(serviceRow.getName());
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.INGRESS) {
				IngressSpecIntegrateVO ingressIntegrate = null;
				IngressSpecGuiVO ingressRow = null;
				if(deployType == DeployType.GUI) {
					ingressIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<IngressSpecIntegrateVO>() {});
					ingressRow = (IngressSpecGuiVO)ingressIntegrate;
				}
				else if(deployType == DeployType.YAML) {
//					ingressIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<ServiceSpecIntegrateVO>() {});
//					IngressSpecYamlVO ingressSpecYaml = (IngressSpecYamlVO)ingressIntegrate;

					Object obj = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.INGRESS);
					IngressSpecYamlVO ingressSpecYaml = new IngressSpecYamlVO();
					ingressSpecYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						ingressSpecYaml.setNamespaceName(servicemapDetail.getNamespaceName());
					}
					ingressSpecYaml.setClusterSeq(templateValidRequest.getClusterSeq());
					if(obj instanceof NetworkingV1beta1Ingress) {
						NetworkingV1beta1Ingress networkingIngressesV1beta1 = (NetworkingV1beta1Ingress)obj;
						ingressSpecYaml.setName(this.getNameFromMetadata(networkingIngressesV1beta1.getMetadata()));
					}
					else if(obj instanceof V1Ingress) {
						V1Ingress networkingIngressesV1 = (V1Ingress)obj;
						ingressSpecYaml.setName(this.getNameFromMetadata(networkingIngressesV1.getMetadata()));
					}

					ingressRow = ingressSpecService.convertIngressSpecYamlToGui(cluster, Optional.ofNullable(servicemapDetail).map(ServicemapDetailVO::getServicemapSeq).orElse(null), cluster.getNamespaceName(), ingressSpecYaml.getYaml());
					try {
						ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), true, ingressRow);
					}
					catch (Exception ex) {
						if(ex instanceof CocktailException) {
							CocktailException ce = (CocktailException)ex;
							if(ce.getType() == ExceptionType.IngressPathUsed) {
								ingressHostPathValid = false;
							}
							if(ce.getType() == ExceptionType.IngressHostUsed) {
								ingressHostValid = false;
							}
							if(ce.getType() == ExceptionType.IngressPathTooLong) {
								ingressPathLongValid = false;
							}
							if(ce.getType() == ExceptionType.IngressHostTooLong) {
								ingressHostLongValid = false;
							}
							if(ce.getType() == ExceptionType.K8sIngressNameInvalid) {
								ingressNameSpecValid = false;
							}
						}
					}

				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				if(ingressRow == null) {
					throw new CocktailException("Ingress is null!!", ExceptionType.TemplateDeploymentFail);
				}
				if(ingressNameSet.contains(ingressRow.getName())){
					ingressNameValid = false;
				}
				ingressNameSet.add(ingressRow.getName());
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.PERSISTENT_VOLUME_CLAIM) {
				PersistentVolumeClaimIntegrateVO persistentVolumeClaimIntegrate = null;
				String pvcName = null;
				if(deployType == DeployType.GUI) {
					persistentVolumeClaimIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContent(), new TypeReference<PersistentVolumeClaimIntegrateVO>() {});
					PersistentVolumeClaimGuiVO pvcRow = (PersistentVolumeClaimGuiVO)persistentVolumeClaimIntegrate;
					pvcName = pvcRow.getName();
				}
				else if(deployType == DeployType.YAML) {
//					persistentVolumeClaimIntegrate = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<PersistentVolumeClaimIntegrateVO>() {});
//					PersistentVolumeClaimYamlVO pvcYaml = (PersistentVolumeClaimYamlVO)persistentVolumeClaimIntegrate;

					V1PersistentVolumeClaim v1Pvc = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.PERSISTENT_VOLUME_CLAIM);
					PersistentVolumeClaimYamlVO pvcYaml = new PersistentVolumeClaimYamlVO();
					pvcYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						pvcYaml.setNamespace(servicemapDetail.getNamespaceName());
					}
					pvcName = this.getNameFromMetadata(v1Pvc.getMetadata());
					pvcYaml.setName(pvcName);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				if(StringUtils.isBlank(pvcName)) {
					throw new CocktailException("PVC name is required!!", ExceptionType.TemplateDeploymentFail);
				}
				if(pvcNameSet.contains(pvcName)){
					pvcNameValid = false;
				}
				pvcNameSet.add(pvcName);
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.CUSTOM_OBJECT) {
				CommonYamlVO customObjectYaml = new CommonYamlVO();
				Map<String, Object> customObject = null;
				Map<String, Object> meta = null;
				if(deployType == DeployType.YAML) {
//					customObjectYaml = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<CommonYamlVO>() {});
//					if(customObjectYaml == null) {
//						throw new CocktailException("CUSTOM_OBJECT is null!!", ExceptionType.TemplateDeploymentFail);
//					}
					customObject = Yaml.getSnakeYaml().load(deployment.getTemplateContentYaml());
					customObjectYaml.setYaml(deployment.getTemplateContentYaml());
					customObjectYaml.setK8sApiKindType(K8sApiKindType.CUSTOM_OBJECT);
					if(servicemapDetail != null) {
						customObjectYaml.setNamespace(servicemapDetail.getNamespaceName());
					}
					meta = (Map<String, Object>)customObject.get(KubeConstants.META);
					if(StringUtils.isBlank(MapUtils.getString(meta, KubeConstants.NAME, ""))) {
						throw new CocktailException("Custom Object Name is Required!!", ExceptionType.InvalidYamlData);
					}
					customObjectYaml.setName(MapUtils.getString(meta, KubeConstants.NAME, ""));
					if(customObject == null) {
						throw new CocktailException("CUSTOM_OBJECT is null!!", ExceptionType.TemplateDeploymentFail);
					}
				}
				else if(deployType == DeployType.GUI) {
					throw new CocktailException("The CustomObject only supports YAML deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}

				for(Pair pr : customObjectNameSet) {
					if(StringUtils.equals(pr.getLeft().toString(), MapUtils.getString(customObject, KubeConstants.KIND)) &&
						StringUtils.equals(pr.getRight().toString(), MapUtils.getString(meta, KubeConstants.NAME))) {
						customObjectNameValid = false;
					}
				}
				customObjectNameSet.add(Pair.of(MapUtils.getString(customObject, KubeConstants.KIND), MapUtils.getString(meta, KubeConstants.NAME)));
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.SERVICE_ACCOUNT) {
				if(deployType == DeployType.YAML) {
//					CommonYamlVO saYaml = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<CommonYamlVO>() {});
					CommonYamlVO saYaml = new CommonYamlVO();
					V1ServiceAccount serviceAccount = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.SERVICE_ACCOUNT);
					if(serviceAccount == null) {
						throw new CocktailException("SERVICE_ACCOUNT is null!!", ExceptionType.TemplateDeploymentFail);
					}
					saYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						saYaml.setNamespace(servicemapDetail.getNamespaceName());
					}
					saYaml.setK8sApiKindType(K8sApiKindType.SERVICE_ACCOUNT);
					saYaml.setName(this.getNameFromMetadata(serviceAccount.getMetadata()));
					if(serviceAccountNameSet.contains(serviceAccount.getMetadata().getName())) {
						serviceAccountNameValid = false;
					}
					serviceAccountNameSet.add(serviceAccount.getMetadata().getName());
				}
				else if(deployType == DeployType.GUI) {
					throw new CocktailException("The ServiceAccount only supports YAML deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.ROLE) {
				if(deployType == DeployType.YAML) {
//					CommonYamlVO roleYaml = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<CommonYamlVO>() {});
					CommonYamlVO roleYaml = new CommonYamlVO();
					V1Role role = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.ROLE);
					if(role == null) {
						throw new CocktailException("ROLE is null!!", ExceptionType.TemplateDeploymentFail);
					}
					roleYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						roleYaml.setNamespace(servicemapDetail.getNamespaceName());
					}
					roleYaml.setK8sApiKindType(K8sApiKindType.ROLE);
					roleYaml.setName(this.getNameFromMetadata(role.getMetadata()));

					if(roleNameSet.contains(role.getMetadata().getName())) {
						roleNameValid = false;
					}
					roleNameSet.add(role.getMetadata().getName());
				}
				else if(deployType == DeployType.GUI) {
					throw new CocktailException("The Role only supports YAML deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.ROLE_BINDING) {
				if(deployType == DeployType.YAML) {
//					CommonYamlVO roleBindingYaml = this.getObjectMapper().readValue(deployment.getTemplateContentYaml(), new TypeReference<CommonYamlVO>() {});
					CommonYamlVO roleBindingYaml = new CommonYamlVO();
					V1RoleBinding roleBinding = ServerUtils.unmarshalYaml(deployment.getTemplateContentYaml(), K8sApiKindType.ROLE_BINDING);
					if(roleBinding == null) {
						throw new CocktailException("Role Binding is null!!", ExceptionType.TemplateDeploymentFail);
					}
					roleBindingYaml.setYaml(deployment.getTemplateContentYaml());
					if(servicemapDetail != null) {
						roleBindingYaml.setNamespace(servicemapDetail.getNamespaceName());
					}
					roleBindingYaml.setK8sApiKindType(K8sApiKindType.ROLE);
					roleBindingYaml.setName(this.getNameFromMetadata(roleBinding.getMetadata()));

					if(roleBindingNameSet.contains(roleBinding.getMetadata().getName())) {
						roleBindingNameValid = false;
					}
					roleBindingNameSet.add(roleBinding.getMetadata().getName());
				}
				else if(deployType == DeployType.GUI) {
					throw new CocktailException("The Role only supports YAML deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}
			else if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.PACKAGE) {
				if(deployType == DeployType.GUI) {
					HelmInstallRequestVO helmInstallRequest = JsonUtils.fromGson(deployment.getTemplateContent(), HelmInstallRequestVO.class);
					if(packageNameSet.contains(helmInstallRequest.getReleaseName())) {
						packageNameValid = false;
					}
					packageNameSet.add(helmInstallRequest.getReleaseName());
				}
				else if(deployType == DeployType.YAML) {
					throw new CocktailException("The Role only supports GUI deployment.", ExceptionType.TemplateDeploymentFail);
				}
				else {
					throw new CocktailException(String.format("Invalid DeployType!! : [%s]", deployType.getCode()), ExceptionType.TemplateDeploymentFail);
				}
			}

			TemplateValidResponseVO validRow = new TemplateValidResponseVO();
			validRow.setTemplateDeploymentSeq(deployment.getTemplateDeploymentSeq());
			validRow.setValid(errMsg.length() == 0);
			validRow.setServerNameValid(serverNameValid);
			validRow.setPVConfig(isPVConfig);
			validRow.setConfigMapNameValid(configMapNameValid);
			validRow.setSecretNameValid(secretNameValid);
			validRow.setNetAttachDefNameValid(netAttachDefNameValid);
			validRow.setClusterSupported(clusterSupported);
			validRow.setGpuValid(gpuValid);
			validRow.setSctpValid(sctpValid);
			validRow.setMultiNicValid(multiNicValid);
			validRow.setSriovValid(sriovValid);
			validRow.setTtlAfterFinishedValid(ttlAfterFinishedValid);
			validRow.setValidMsg(errMsg.toString());
			/** 4.0.2 신규 추가 리소스 유효성 체크 : 여기부터 **/
			validRow.setServiceNameValid(serviceNameValid);
			validRow.setIngressNameValid(ingressNameValid);
			validRow.setPvcNameValid(pvcNameValid);
			validRow.setServiceAccountNameValid(serviceAccountNameValid);
			validRow.setRoleNameValid(roleNameValid);
			validRow.setRoleBindingNameValid(roleBindingNameValid);
			validRow.setCustomObjectNameValid(customObjectNameValid);
			validRow.setPackageNameValid(packageNameValid);
			validRow.setConfigMapDataValid(configMapDataValid);
			validRow.setSecretDataValid(secretDataValid);
			validRow.setIngressHostLongValid(ingressHostLongValid);
			validRow.setIngressPathLongValid(ingressPathLongValid);
			validRow.setIngressNameSpecValid(ingressNameSpecValid);
			validRow.setIngressHostValid(ingressHostValid);
			validRow.setIngressHostPathValid(ingressHostPathValid);
			/** 4.0.2 신규 추가 리소스 유효성 체크 : 여기까지 **/
			validRow.setHpaNameValid(hpaNameValid);

			validList.add(validRow);
		}

		return validList;
	}

	private Set<String> getWorkloadNameSet(ClusterVO cluster) throws Exception {
		Set<String> workloadNames = Sets.newHashSet();
		try {
			Map<String, Map<String, ?>> k8sResourceMap = workloadResourceService.getWorkloadResource(
				cluster, cluster.getNamespaceName(),
				null, null,
				Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);

			/** 2-9-1 : k8s에서 조회한 workload를 저장 **/
			if (MapUtils.isNotEmpty(k8sResourceMap)) {
				for (Map.Entry<String, Map<String, ?>> k8sKindEntryRow : k8sResourceMap.entrySet()) {
					K8sApiKindType kindType = K8sApiKindType.findKindTypeByValue(k8sKindEntryRow.getKey());
					if (kindType != null && kindType.isWorkload() && MapUtils.isNotEmpty(k8sKindEntryRow.getValue())) {
						for (Map.Entry<String, ?> k8sWorklodEntryRow : k8sKindEntryRow.getValue().entrySet()) {
							workloadNames.add(k8sWorklodEntryRow.getKey()); // getKey = workloadName
						}
					}
				}
			}
		}
		catch (Exception ex) {
			log.error("get Workload Name Set Error : " + ex.getMessage(), ex);
			return null;
		}

		return workloadNames;
	}

	private Set<String> getHpaNameSet(ClusterVO cluster) throws Exception {
		Set<String> hpaNameSet = Sets.newHashSet();
		try {
			// HPA 목록 조회.
			K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
			if (apiVerKindType != null) {
				if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V1){
					List<V1HorizontalPodAutoscaler> v1HorizontalPodAutoscalers = workloadResourceService.getHorizontalPodAutoscalersV1(cluster, cluster.getNamespaceName(), null, null);
					// 조회한 HPA Name을 Nameset에 추가.
					if(CollectionUtils.isNotEmpty(v1HorizontalPodAutoscalers)) {
	//					hpaNameSet.addAll(new HashSet<>(v1HorizontalPodAutoscalers.stream().map(V1HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toList())));
						for(V1HorizontalPodAutoscaler hpa : Optional.ofNullable(v1HorizontalPodAutoscalers).orElseGet(() ->Lists.newArrayList())) {
							if(hpa.getMetadata() != null && StringUtils.isNotBlank(hpa.getMetadata().getName())) {
								hpaNameSet.add(hpa.getMetadata().getName());
							}
						}
					}
				}
				else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA1) {
					List<V2beta1HorizontalPodAutoscaler> v2beta1HorizontalPodAutoscalers = workloadResourceService.getHorizontalPodAutoscalersV2beta1(cluster, cluster.getNamespaceName(), null, null);
					// 조회한 HPA Name을 Nameset에 추가.
					if(CollectionUtils.isNotEmpty(v2beta1HorizontalPodAutoscalers)) {
	//					hpaNameSet.addAll(new HashSet<>(v2beta1HorizontalPodAutoscalers.stream().map(V2beta1HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toList())));
						for(V2beta1HorizontalPodAutoscaler hpa : Optional.ofNullable(v2beta1HorizontalPodAutoscalers).orElseGet(() ->Lists.newArrayList())) {
							if(hpa.getMetadata() != null && StringUtils.isNotBlank(hpa.getMetadata().getName())) {
								hpaNameSet.add(hpa.getMetadata().getName());
							}
						}
					}
				}
				else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA2) {
					List<V2beta2HorizontalPodAutoscaler> v2beta2HorizontalPodAutoscalers = workloadResourceService.getHorizontalPodAutoscalersV2beta2(cluster, cluster.getNamespaceName(), null, null);
					// 조회한 HPA Name을 Nameset에 추가.
					if(CollectionUtils.isNotEmpty(v2beta2HorizontalPodAutoscalers)) {
	//					hpaNameSet.addAll(new HashSet<>(v2beta2HorizontalPodAutoscalers.stream().map(V2beta2HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toList())));
						for(V2beta2HorizontalPodAutoscaler hpa : Optional.ofNullable(v2beta2HorizontalPodAutoscalers).orElseGet(() ->Lists.newArrayList())) {
							if(hpa.getMetadata() != null && StringUtils.isNotBlank(hpa.getMetadata().getName())) {
								hpaNameSet.add(hpa.getMetadata().getName());
							}
						}
					}
				}
				else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2) {
					List<V2HorizontalPodAutoscaler> v2HorizontalPodAutoscalers = workloadResourceService.getHorizontalPodAutoscalersV2(cluster, cluster.getNamespaceName(), null, null);
					// 조회한 HPA Name을 Nameset에 추가.
					if(CollectionUtils.isNotEmpty(v2HorizontalPodAutoscalers)) {
	//					hpaNameSet.addAll(new HashSet<>(v2beta2HorizontalPodAutoscalers.stream().map(V2beta2HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).collect(Collectors.toList())));
						for(V2HorizontalPodAutoscaler hpa : Optional.ofNullable(v2HorizontalPodAutoscalers).orElseGet(() ->Lists.newArrayList())) {
							if(hpa.getMetadata() != null && StringUtils.isNotBlank(hpa.getMetadata().getName())) {
								hpaNameSet.add(hpa.getMetadata().getName());
							}
						}
					}
				}
			}
		}
		catch (Exception ex) {
			log.error("get HPA Name Set Error : " + ex.getMessage(), ex);
			return null;
		}

		return hpaNameSet;
	}

	/**
	 * Template Deployment Type에 해당하는 WorkloadType을 응답.
	 * @param templateDeploymentType
	 * @return
	 * @throws Exception
	 */
	private WorkloadType getWorkloadTypeAsTemplateDeploymentType(TemplateDeploymentType templateDeploymentType) throws Exception {
		WorkloadType workloadType = null;
		switch (templateDeploymentType) {
			case DEPLOYMENT:
				workloadType = WorkloadType.REPLICA_SERVER;
				break;
			case STATEFUL_SET:
				workloadType = WorkloadType.STATEFUL_SET_SERVER;
				break;
			case DAEMON_SET:
				workloadType = WorkloadType.DAEMON_SET_SERVER;
				break;
			case CRON_JOB:
				workloadType = WorkloadType.CRON_JOB_SERVER;
				break;
			case JOB:
				workloadType = WorkloadType.JOB_SERVER;
				break;
			default:
				throw new CocktailException(String.format("Invalid Workload Template Deployment Type!! : [%s]", templateDeploymentType), ExceptionType.TemplateDeploymentFail);
		}

		return workloadType;
	}

	/**
	 * Gate of add Template by Import
	 * @param apiVersion
	 * @param templateAdd
	 * @param templateDetail
	 * @return
	 * @throws Exception
	 */
	@Transactional(transactionManager = "transactionManager")
	public int addTemplateByImport(String apiVersion, TemplateAddVO templateAdd, TemplateDetailVO templateDetail) throws Exception {


		try {
			switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
				case V2:
					return this.addTemplateByImportV2(templateAdd, templateDetail);
				default:
					throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
			}
		}
		catch (Exception ex) {
			if(log.isDebugEnabled()) log.debug("trace log ", ex);
			throw new CocktailException(String.format("An error occurred during template registration by Import. [%s]", ex.getMessage()), ex, ExceptionType.TemplateRegistrationFail);
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public int addTemplateByImportV2(TemplateAddVO templateAdd, TemplateDetailVO templateDetail) throws Exception {
		int result = 0;

		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);
		// step.1 insert template
		// step.2 insert template version
		result = this.addTemplateVersion(templateAdd, dao);

		// step.3 insert template deployment
		if(result < 1) {
			throw new CocktailException("템플릿 등록 중 오류가 발생하였습니다.[2]", ExceptionType.TemplateRegistrationFail);
		}

		for(TemplateDeploymentVO deploymentRow : templateDetail.getTemplateDeployments()) {
			deploymentRow.setTemplateVersionSeq(templateAdd.getTemplateVersionSeq());
			deploymentRow.setCreator(ContextHolder.exeContext().getUserSeq());
			deploymentRow.setUpdater(ContextHolder.exeContext().getUserSeq());
			// 템플릿 배치 정보 등록
			result = dao.addTemplateDeployment(deploymentRow);
		}

		return result;
	}

	/**
	 * OLD (V1) 유형 템플릿인지 판단..
	 * @param templateDetail
	 * @return
	 * @throws Exception
	 */
	public boolean isV1Template(TemplateDetailVO templateDetail) throws Exception {
		// AS-IS (v1) Snapshot 유형인지 확인 후 v1 Type Snapshot이면 기존 로직으로 처리..
		for(TemplateDeploymentVO templateDeploymentRow : templateDetail.getTemplateDeployments()) {
			if(templateDeploymentRow.getTemplateDeploymentType() != TemplateDeploymentType.PACKAGE) {
				if(StringUtils.isNotBlank(templateDeploymentRow.getTemplateContent()) &&
					StringUtils.startsWithAny(templateDeploymentRow.getTemplateContent(), "{", "[")) {
					// JSON Format으로 저장되어 있으면 AS-IS Type으로 판단.
					return true;
				}
			}
		}

		return false;
	}

	/** ================================================================================================================
	 * End : Change template format to YAML type
	 =============================================================================================================== **/


	private int addTemplateVersion(TemplateAddVO templateAdd, ITemplateMapper dao) throws Exception {
		int result = 0;

		// step.1 insert template
		// 신규 등록일 경우
		if(templateAdd.isNew()){
			// 템플릿명 중복 체크
			if(dao.checkDuplTemplateName(templateAdd) > 0){
				throw new CocktailException(String.format("[%s] is already registered template.", templateAdd.getTemplateName()),
						ExceptionType.TemplateNameAlreadyExists);
			}

			result = dao.addTemplate(templateAdd);
		}
		// 버전업일 경우
		else{
			// 버전 중복 체크
			if(dao.checkDuplTemplateVersion(templateAdd) > 0){
				throw new CocktailException(String.format("[%s] is already registered version.", templateAdd.getVersion()),
						ExceptionType.TemplateVersionAlreadyExists);
			}
		}

		// step.2 insert template version
		// 신규 등록일 경우 템플릿 저장이 성공이거나
		// 버전업일 경우에 버전정보를 저장함
		if((templateAdd.isNew() && (result > 0))
				|| (!templateAdd.isNew() && (templateAdd.getTemplateSeq() != null && templateAdd.getTemplateSeq() > 0))){
			if(!templateAdd.isNew()){
				// 템플릿 버전 등록전 기존 버전 최신여부 'N'로 초기화 처리
				dao.updateTemplateVersionForNonLatest(templateAdd);
			}

			// 템플릿 버전 등록
			result = dao.addTemplateVersion(templateAdd);
		}
		else {
			throw new CocktailException("An error occurred during template registration. [during templates & template_version insert]", ExceptionType.TemplateRegistrationFail);
		}

		return result;
	}

	@Transactional(transactionManager = "transactionManager")
	public int addTemplateByImport(TemplateAddVO templateAdd, TemplateDetailVO templateDetail) throws Exception {
		int result = 0;

		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);

		// step.1 insert template
		// step.2 insert template version
		result = this.addTemplateVersion(templateAdd, dao);

		// step.3 insert template deployment
		if(result > 0){

			for(TemplateDeploymentVO deploymentRow : templateDetail.getTemplateDeployments()){
				deploymentRow.setTemplateVersionSeq(templateAdd.getTemplateVersionSeq());
				deploymentRow.setCreator(ContextHolder.exeContext().getUserSeq());
				deploymentRow.setUpdater(ContextHolder.exeContext().getUserSeq());

				if (TemplateDeploymentType.DEPLOYMENT == deploymentRow.getTemplateDeploymentType()) {
					// json to ServerAddVO
					ServerGuiVO serverAddKubesRow = JsonUtils.fromGson(deploymentRow.getTemplateContent(), ServerGuiVO.class);
					this.convertServerInfoByUpgrade(serverAddKubesRow);
					deploymentRow.setTemplateContent(JsonUtils.toGson(serverAddKubesRow));
				}


				// 템플릿 배치 정보 등록
				result = dao.addTemplateDeployment(deploymentRow);
			}

		}else{
			throw new CocktailException("템플릿 등록 중 오류가 발생하였습니다.[2]", ExceptionType.TemplateRegistrationFail);
		}


		return result;
	}

	/**
	 * Template 목록 조회. (serviceSeq에 해당하는 Template 목록만 조회합니다.)
	 * @param templateType
	 * @param serviceSeq
	 * @return
	 */
	public List<TemplateListVO> getTemplateList(String templateType, String templateShareType, Integer accountSeq, Integer serviceSeq){
		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);

		return dao.getTemplateList(templateType, templateShareType, accountSeq, serviceSeq);
	}

	/**
	 * Template Version Sequence List 조회.
	 * @param templateSeq
	 * @return
	 */
	public List<Integer> getTemplateVersionList(Integer templateSeq){
		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);
		return dao.getTemplateVersionList(templateSeq);
	}

	/**
	 * Insert Template Deployment
	 * @param templateDeployment
	 * @return
	 */
	public int addTemplateDeployment(TemplateDeploymentVO templateDeployment){
		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);
		return dao.addTemplateDeployment(templateDeployment);
	}

	/**
	 * Update Template Deployment
	 * @param templateDeployment
	 * @return
	 */
	public int updateTemplateDeployment(TemplateDeploymentVO templateDeployment){
		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);
		return dao.updateTemplateDeployment(templateDeployment);
	}

	/**
	 * Update Template Deployment And Type
	 * @param templateDeployment
	 * @return
	 */
	public int updateTemplateDeploymentAndType(TemplateDeploymentVO templateDeployment){
		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);
		return dao.updateTemplateDeploymentAndType(templateDeployment);
	}

	public TemplateDetailVO getTemplateDetail(Integer templateSeq, Integer templateVersionSeq) {
		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);
		return dao.getTemplateDetail(templateSeq, templateVersionSeq);
	}

	public TemplateDetailVO getTemplateDetail(Integer templateSeq, Integer templateVersionSeq, Boolean showDeploy) {
		
		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);

		TemplateDetailVO templateDetail = dao.getTemplateDetail(templateSeq, templateVersionSeq);

		if(templateDetail != null){
			if(showDeploy != null && showDeploy.booleanValue() == true){
				if(CollectionUtils.isNotEmpty(templateDetail.getTemplateDeployments())){
					for(TemplateDeploymentVO templateDeploymentRow : templateDetail.getTemplateDeployments()){
						if(StringUtils.isNotBlank(templateDeploymentRow.getTemplateContent())){
							if(templateDeploymentRow.getTemplateDeploymentType() == TemplateDeploymentType.DEPLOYMENT){
								ServerGuiVO serverAddKubesRow = JsonUtils.fromGson(templateDeploymentRow.getTemplateContent(), ServerGuiVO.class);

								this.setContainerNameForDeployment(serverAddKubesRow);
//								deployService.convertServiceContent(serverAddKubesRow, true);

								this.convertServerInfoByUpgrade(serverAddKubesRow);

								templateDeploymentRow.setTemplateContent(JsonUtils.toGson(serverAddKubesRow));
							}
						}
						templateDeploymentRow.setDeployType(DeployType.GUI.getCode()); // 기존 스냅샷은 GUI만 지원 : 2020.02.27 필드 추가.
					}
				}
			}else{
				templateDetail.setTemplateDeployments(null);
			}
		}

		return templateDetail;
	}

	@Transactional(transactionManager = "transactionManager")
	public int removeTemplate(Integer templateSeq, Integer templateVersionSeq, Integer creator) throws Exception {
		int result;

		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);

		// step.1 템플릿 버전 삭제 정보 조회
		TemplateVersionDelVO tempVerDelInfo = dao.getTemplateVersionForDel(templateSeq, templateVersionSeq);

		// step.2 템플릿 버전 삭제
		result = dao.deleteTemplateVersion(tempVerDelInfo.getTemplateSeq(), tempVerDelInfo.getTemplateVersionSeqForDel(), creator);

		if(result > 0){
			// step.3 템플릿 버전 총 수에 따라 분기 처리
			if(tempVerDelInfo.getVersionTotalCount() > 0){
				// step.3-1 템플릿의 버전이 있고 삭제된 버전이 최신이었다면 다른 버전으로 최신여부를 재처리
				if(StringUtils.equals("Y", tempVerDelInfo.getLatestYnForDel())){
					result = dao.updateTemplateVersionForLatest(tempVerDelInfo.getTemplateSeq(), tempVerDelInfo.getLatestTemplateVersionSeq(), creator);
				}
			}else{
				// step.3-2 템플릿의 버전이 없다면 Master 삭제 처리(use_yn = 'N')
				result = dao.deleteTemplateByNoVersion(tempVerDelInfo.getTemplateSeq(), creator);
			}
		}else{
			throw new CocktailException("템플릿(버전) 삭제 중 오류가 발생하였습니다.", ExceptionType.TemplateDeletionFail);
		}

		return result;
	}

	@Transactional(transactionManager = "transactionManager")
	public int removeTemplateByService(Integer accountSeq, Integer serviceSeq, Integer creator) throws Exception {
		int result;

		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);

		// step.1 템플릿 버전 삭제
		result = dao.deleteTemplateVersionByService(accountSeq, serviceSeq, creator);

		// step.2 템플릿 버전 삭제
		result = dao.deleteTemplateByService(accountSeq, serviceSeq, creator);

		return result;
	}

	@Transactional(transactionManager = "transactionManager")
	public int editTemplate(TemplateEditVO templateEdit) throws Exception {
		int result;

		ITemplateMapper dao = sqlSession.getMapper(ITemplateMapper.class);

		// step.1 템플릿 버전 정보 수정
		result = dao.updateTemplateVersion(templateEdit);

		// step.2 템플릿 배치 정보 수정
		if(result > 0){
			if(CollectionUtils.isNotEmpty(templateEdit.getTemplateDeployments())){
				for(TemplateDeploymentVO tempDeployRow : templateEdit.getTemplateDeployments()){
					result = dao.updateTemplateDeployment(tempDeployRow);
				}
			}else{
				throw new CocktailException("템플릿 수정 중 오류가 발생하였습니다.", ExceptionType.TemplateUpdateFail);
			}
		}

		return result;
	}

	private String _checkContainerSupported(ServerGuiVO serverAddKubesRow, ClusterVO cluster, Integer serviceSeq, boolean isResponseMsg) throws Exception{
		StringBuffer errMsgSb = new StringBuffer();
		String errMsgTmpl;
		if(isResponseMsg){
			errMsgTmpl = "[%s]";
		} else {
			StringBuilder sb = new StringBuilder();
			errMsgTmpl = sb.append(String.format("템플릿 배포 중 오류가 발생하였습니다.[%s",
                    serverAddKubesRow.getComponent().getComponentName())).append(" : %s]").toString();
		}

		if(serverAddKubesRow.getInitContainers() != null) {
			for (ContainerVO containerRow : serverAddKubesRow.getInitContainers()) {
				// 포트지원여부 체크
	//			if(CollectionUtils.isNotEmpty(containerRow.getContainerPorts())){
	//				this._getErrMsgByExisted(
	//						this._checkPortTypeSupported(errMsgTmpl, containerRow.getContainerPorts(), cluster, isResponseMsg)
	//						, errMsgSb);
	//			}

//				if (StringUtils.equals("Y", containerRow.getPrivateRegistryYn())) {
//					// 서비스에 해당 Registry(project)가 존재하는 지 체크
//					errMsgSb.append(this._checkProjectOfService(errMsgTmpl, serviceSeq, containerRow.getProjectId(), isResponseMsg));
//				}
			}
		}
		if(serverAddKubesRow.getContainers() != null) {
			for (ContainerVO containerRow : serverAddKubesRow.getContainers()) {
				// 포트지원여부 체크
	//			if(CollectionUtils.isNotEmpty(containerRow.getContainerPorts())){
	//				this._getErrMsgByExisted(
	//						this._checkPortTypeSupported(errMsgTmpl, containerRow.getContainerPorts(), cluster, isResponseMsg)
	//						, errMsgSb);
	//			}

//				if (StringUtils.equals("Y", containerRow.getPrivateRegistryYn())) {
//					// 서비스에 해당 Registry(project)가 존재하는 지 체크
//					errMsgSb.append(this._checkProjectOfService(errMsgTmpl, serviceSeq, containerRow.getProjectId(), isResponseMsg));
//				}
			}
		}

        // 볼륨지원여부 체크
		if(CollectionUtils.isNotEmpty(serverAddKubesRow.getVolumes())){
			this._getErrMsgByExisted(
					this._checkPersistentVolumeSupported(errMsgTmpl, serverAddKubesRow.getVolumes(), cluster, isResponseMsg),
					errMsgSb);
		}

		// exception 처리
		if(!isResponseMsg && errMsgSb.length() > 0){
//			this._genErrMsg(false, errMsgSb.toString(), errMsgSb);
			throw new CocktailException(errMsgSb.toString(), ExceptionType.TemplateDeploymentFail);
		}
		return errMsgSb.toString();
	}


	private String _checkPersistentVolumeSupported(String errMsgTmpl, List<ContainerVolumeVO> volumes, ClusterVO cluster, boolean isResponseMsg) throws Exception{
		StringBuffer errMsgSb = new StringBuffer();

		for (ContainerVolumeVO v : volumes) {
			if(VolumeType.PERSISTENT_VOLUME == v.getVolumeType()
					|| VolumeType.PERSISTENT_VOLUME_STATIC == v.getVolumeType()){
				if(StringUtils.equals(cluster.getPersistentVolumeSupported(), "N")){
					this._genErrMsg(String.format(errMsgTmpl, "퍼시스턴트 볼륨 미지원"), errMsgSb);
					break;
				}
			}
		}

		return errMsgSb.toString();
	}

	private ClusterVO _getClusterInfo(String errMsgTmpl, Integer clusterSeq) throws Exception{
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ClusterVO cluster = clusterDao.getCluster(clusterSeq);

		if(cluster == null){
			throw new CocktailException(String.format(errMsgTmpl, "[클러스터 없음]"), ExceptionType.TemplateClusterNotExists);
		}

		return cluster;
	}

	/**
	 * 클러스터 정보 조회 (by servicemap)
	 * clusterDao.getCluster 조회시 플랫폼 정보를 조회하여 pull 사용자 정보를 사용함.
	 *
	 * @param errMsgTmpl
	 * @param servicemapSeq
	 * @return
	 * @throws Exception
	 */
	public ClusterVO _getClusterInfoByServicemap(String errMsgTmpl, Integer servicemapSeq) throws Exception {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
		ServicemapVO servicemap = servicemapDao.getServicemap(servicemapSeq, null);
		if (servicemap == null) {
			throw new CocktailException(String.format(errMsgTmpl, "[서비스맵 없음]"), ExceptionType.TemplateClusterNotExists);
		}

		ClusterVO cluster = clusterDao.getCluster(servicemap.getClusterSeq());
		if(cluster == null){
			throw new CocktailException(String.format(errMsgTmpl, "[클러스터 없음]"), ExceptionType.TemplateClusterNotExists);
		}
		cluster.setNamespaceName(servicemap.getNamespaceName());

		return cluster;
	}

	private List<ClusterVolumeVO> _getClusterVolumeInfo(String errMsgTmpl, Integer clusterSeq, StorageType storageType, VolumeType volumeType) throws Exception{
//		if (volumeType == VolumeType.PERSISTENT_VOLUME_STATIC) {
//			IClusterVolumeMapper clusterDao = sqlSession.getMapper(IClusterVolumeMapper.class);
//			return clusterDao.getClusterVolumes(null, null, clusterSeq, null, volumeType.getCode(), "Y");
//		} else {
//			return clusterVolumeService.getClusterVolumes(null, null, clusterSeq, null, volumeType.getCode(), "Y", false, false);
//		}
		return clusterVolumeService.getStorageVolumes(null, null, clusterSeq, null, volumeType.getCode(), false, false);
	}

	private void _getErrMsgByExisted(String errMsg, StringBuffer errMsgSb){
		if(StringUtils.isNotBlank(errMsg)){
			errMsgSb.append(errMsg);
		}
	}

	private void _genErrMsg(String errMsg, StringBuffer errMsgSb) throws Exception{
		errMsgSb.append(errMsg).append("\n");
//		this._genErrMsg(true, errMsg, errMsgSb);
	}

	private Set<Integer> setMemberToProject(IHarborRegistryService harborRegistryService, List<HarborProjectMemberVO> users, Set<Integer> projects, String registryProjectName, String registryUrl, List<ContainerVO> containers) throws Exception{
		if (CollectionUtils.isNotEmpty(containers)) {
			for(ContainerVO containerRow : containers){
				if (containerRow != null && StringUtils.isNotBlank(containerRow.getFullImageName()) && CollectionUtils.isNotEmpty(users)) {
					// regi.acornsoft.io/cocktail-common/test-imagename:1.0.B000001
					// 해당 registry 이미지 인지 체크
					if(StringUtils.startsWith(containerRow.getFullImageName(), registryUrl)){
						// project가 존재하는 구조인지 체크
						if(StringUtils.countMatches(containerRow.getFullImageName(), "/") >= 2){
							// 해당 project 정보 조회
							RegistryProjectVO registryProject = harborRegistryService.getProject(StringUtils.split(containerRow.getFullImageName(), "/")[1]);
							if (registryProject != null) {
								// 배포하려는 서비스의 project와 해당 이미지의 project가 같지 않다면
								if(!StringUtils.equals(registryProjectName, registryProject.getName())){
									// 배포하려는 서비스의 project > member인지를 체크
									HarborProjectMemberVO member = harborRegistryService.getMemberOfProject(registryProject.getProjectId(), users.get(0).getEntityName());
									if (projects == null) {
										projects = Sets.newHashSet();
									}
									// 없다면 user를 project member로 GUEST권한으로 추가
									if(member == null){
										projects.add(registryProject.getProjectId());
									}
								}
							}
						}
					}
				}
			}
		}

		return projects;
	}

	private void getConfigMapNSecretNetAttachDef(Integer servicemapSeq, Set<String> configMapNameSet, Set<String> secretNameSet, Set<String> netAttachDefNameSet) throws Exception{
		List<ConfigMapGuiVO> configMapsOfAppmap = configMapService.getConfigMaps(servicemapSeq);
		if(CollectionUtils.isNotEmpty(configMapsOfAppmap)){
			configMapNameSet.addAll(configMapsOfAppmap.stream().map(ConfigMapGuiVO::getName).collect(Collectors.toList()));
		}
		List<SecretGuiVO> secretsOfAppmap = secretService.getSecrets(servicemapSeq, null, null, true);
		if(CollectionUtils.isNotEmpty(secretsOfAppmap)){
			secretNameSet.addAll(secretsOfAppmap.stream().map(SecretGuiVO::getName).collect(Collectors.toList()));
		}
		List<Map<String, Object>> results = crdResourceService.getCustomObjects(servicemapSeq, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, null);
		List<K8sCRDNetAttachDefGuiVO> netAttachDefs = crdResourceService.getConvertNetAttachDefList(results);
		if(CollectionUtils.isNotEmpty(netAttachDefs)){
			netAttachDefNameSet.addAll(netAttachDefs.stream().map(K8sCRDNetAttachDefGuiVO::getName).collect(Collectors.toList()));
		}

	}

	public void convertServerInfoByUpgrade(ServerGuiVO serverAddKubesRow) {
		if (CollectionUtils.isNotEmpty(serverAddKubesRow.getInitContainers())) {
			for (ContainerVO containerRow : serverAddKubesRow.getInitContainers()) {
				if (StringUtils.isNotBlank(containerRow.getCommand())) {
					containerRow.setCmds(K8sSpecFactory.setTrimCommand(containerRow.getCommand()));
					containerRow.setCommand(null);
				}
				if (StringUtils.isNotBlank(containerRow.getArguments())) {
					containerRow.setArgs(K8sSpecFactory.setTrimCommand(containerRow.getArguments()));
					containerRow.setArguments(null);
				}
				if (StringUtils.isNotBlank(containerRow.getImageName()) && StringUtils.isNotBlank(containerRow.getImageTag())) {
					containerRow.setFullImageName(String.format("%s:%s", containerRow.getImageName(), containerRow.getImageTag()));
					containerRow.setImageName(null);
					containerRow.setImageTag(null);
				}
			}
		}
		if (CollectionUtils.isNotEmpty(serverAddKubesRow.getContainers())) {
			for (ContainerVO containerRow : serverAddKubesRow.getContainers()) {
				if (StringUtils.isNotBlank(containerRow.getCommand())) {
					containerRow.setCmds(K8sSpecFactory.setTrimCommand(containerRow.getCommand()));
					containerRow.setCommand(null);
				}
				if (StringUtils.isNotBlank(containerRow.getArguments())) {
					containerRow.setArgs(K8sSpecFactory.setTrimCommand(containerRow.getArguments()));
					containerRow.setArguments(null);
				}
				if (StringUtils.isNotBlank(containerRow.getImageName()) && StringUtils.isNotBlank(containerRow.getImageTag())) {
					containerRow.setFullImageName(String.format("%s:%s", containerRow.getImageName(), containerRow.getImageTag()));
					containerRow.setImageName(null);
					containerRow.setImageTag(null);
				}
			}
		}
		if (ServerType.valueOf(serverAddKubesRow.getServer().getServerType()) == ServerType.SINGLE) {
			serverAddKubesRow.getServer().setServerType(ServerType.MULTI.getCode());
		}
		if (WorkloadType.valueOf(serverAddKubesRow.getServer().getWorkloadType()) == WorkloadType.SINGLE_SERVER) {
			serverAddKubesRow.getServer().setWorkloadType(WorkloadType.REPLICA_SERVER.getCode());
		}
		if (serverAddKubesRow.getServer().getHpa() != null && CollectionUtils.isNotEmpty(serverAddKubesRow.getServer().getHpa().getMetrics())) {
			for (MetricVO metric : serverAddKubesRow.getServer().getHpa().getMetrics()) {
			    if (metric.getTargetAverageUtilization() == null && metric.getResourceTargetAverageUtilization() != null) {
                    metric.setTargetType(MetricTargetType.Utilization);
                    metric.setTargetAverageUtilization(metric.getResourceTargetAverageUtilization());
                }
			}
		}

		if (MapUtils.isNotEmpty(serverAddKubesRow.getServer().getLabels())) {
			serverAddKubesRow.getServer().getLabels().remove(KubeConstants.LABELS_COCKTAIL_KEY);
			serverAddKubesRow.getServer().getLabels().remove(KubeConstants.CUSTOM_VOLUME_TYPE);
		}
		if (MapUtils.isNotEmpty(serverAddKubesRow.getServer().getAnnotations())) {
			serverAddKubesRow.getServer().getAnnotations().remove(KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION);
			serverAddKubesRow.getServer().getAnnotations().remove(KubeConstants.META_ANNOTATIONS_DEPRECATED_DAEMONSET_TEMPLATE_GENERATION);
			serverAddKubesRow.getServer().getAnnotations().remove(KubeConstants.META_ANNOTATIONS_LAST_APPLIED_CONFIGURATION);
		}

		/**
		 * Volume 정보 초기화
		 * - PV는 키값 초기화
		 */
		if(CollectionUtils.isNotEmpty(serverAddKubesRow.getVolumes())){
			for(ContainerVolumeVO containerVolume : serverAddKubesRow.getVolumes()){
				// static PV(VolumeType.PERSISTENT_VOLUME_STATIC)는 카탈로그로 등록 금지
				if(containerVolume.getVolumeType() == VolumeType.PERSISTENT_VOLUME
						|| containerVolume.getVolumeType() == VolumeType.PERSISTENT_VOLUME_STATIC
						|| containerVolume.getVolumeType() == VolumeType.PERSISTENT_VOLUME_LINKED){
					if (StringUtils.isNotBlank(containerVolume.getPersistentVolumeClaimName())) {
						if (MapUtils.isNotEmpty(serverAddKubesRow.getServer().getLabels()) && serverAddKubesRow.getServer().getLabels().containsKey(containerVolume.getPersistentVolumeClaimName())) {
							serverAddKubesRow.getServer().getLabels().remove(containerVolume.getPersistentVolumeClaimName());
						}
					}

				}
			}
		}
	}

	/**
	 * Template 정보 조회
	 * @param templateSeq
	 * @return
	 */
	public TemplateVO getTemplate(Integer templateSeq, String useYn) {
		ITemplateMapper templateDao = sqlSession.getMapper(ITemplateMapper.class);
		return templateDao.getTemplate(templateSeq, useYn);
	}

	public ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = K8sMapperUtils.getMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper;
    }

	/**
	 * Namespace CRD에 해당하는 모든 CustomObject 조
	 *
	 * @param cluster
	 * @param namespaceName
	 * @return회 Map<String, Map<String, List<Map<String, Object>>>> => Map<CRD name, Map<CRD version, List<CustomObject Map>>>
	 * @throws Exception
	 */
    private Map<String, Map<String, List<Map<String, Object>>>> getCustomObjectOfCRD(ClusterVO cluster, String namespaceName) throws Exception {

		Map<String, Map<String, List<Map<String, Object>>>> customObjectMap = Maps.newHashMap();

        K8sApiVerKindType crdType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CUSTOM_RESOURCE_DEFINITION);

        if (crdType != null) {
            if (crdType.getGroupType() == K8sApiGroupType.API_EXTENSIONS && crdType.getApiType() == K8sApiType.V1) {
                List<V1CustomResourceDefinition> crds = crdResourceService.getCustomResourceDefinitionV1(cluster, null, null);
                for (V1CustomResourceDefinition crd : Optional.ofNullable(crds).orElseGet(() ->Lists.newArrayList())) {
                    V1CustomResourceDefinitionSpec spec = crd.getSpec();
                    if (!StringUtils.equals("Namespaced", spec.getScope())) {
                        // Scope가 Namespace가 아니면 Skip..
                        continue;
                    }
                    if (StringUtils.isBlank(spec.getGroup()) ||
                            CollectionUtils.isNotEmpty(spec.getVersions()) ||
                            spec.getNames() == null || StringUtils.isBlank(spec.getNames().getPlural())) {
                        // Parameter 누락시 Skip..
                        continue;
                    }

                    if (CollectionUtils.isNotEmpty(spec.getVersions())) {
                        for (V1CustomResourceDefinitionVersion version : spec.getVersions()) {
                            List<Map<String, Object>> customObjects = crdResourceService.getCustomObjects(cluster,
                                    namespaceName,
                                    spec.getGroup(),
                                    version.getName(),
                                    spec.getNames().getPlural(),
                                    null);

                            if (CollectionUtils.isNotEmpty(customObjects)) {
                                if (MapUtils.getObject(customObjectMap, crd.getMetadata().getName(), null) == null) {
                                    customObjectMap.put(crd.getMetadata().getName(), Maps.newHashMap());
                                }

                                customObjectMap.get(crd.getMetadata().getName()).put(version.getName(), customObjects);
                            }
                        }
                    }
                }
            } else if (crdType.getGroupType() == K8sApiGroupType.API_EXTENSIONS && crdType.getApiType() == K8sApiType.V1BETA1) {
                List<V1beta1CustomResourceDefinition> crds = crdResourceService.getCustomResourceDefinitionV1beta1(cluster, null, null);
                for (V1beta1CustomResourceDefinition crd : Optional.ofNullable(crds).orElseGet(() ->Lists.newArrayList())) {
                    V1beta1CustomResourceDefinitionSpec spec = crd.getSpec();
                    if (!StringUtils.equals("Namespaced", spec.getScope())) {
                        // Scope가 Namespace가 아니면 Skip..
                        continue;
                    }
                    if (StringUtils.isBlank(spec.getGroup()) ||
                            StringUtils.isBlank(spec.getVersion()) ||
                            spec.getNames() == null || StringUtils.isBlank(spec.getNames().getPlural())) {
                        // Parameter 누락시 Skip..
                        continue;
                    }

                    if (CollectionUtils.isNotEmpty(spec.getVersions())) {
                        for (V1beta1CustomResourceDefinitionVersion version : spec.getVersions()) {
                            List<Map<String, Object>> customObjects = crdResourceService.getCustomObjects(cluster,
                                    namespaceName,
                                    spec.getGroup(),
                                    version.getName(),
                                    spec.getNames().getPlural(),
                                    null);

                            if (CollectionUtils.isNotEmpty(customObjects)) {
                                if (MapUtils.getObject(customObjectMap, crd.getMetadata().getName(), null) == null) {
                                    customObjectMap.put(crd.getMetadata().getName(), Maps.newHashMap());
                                }

                                customObjectMap.get(crd.getMetadata().getName()).put(version.getName(), customObjects);
                            }
                        }
                    }
                }
            }
        }

		return customObjectMap;
	}

	private void setCustomObjectInfo(List<K8sCRDResultVO> crds, Map<String, Object> customObject, CommonYamlVO customObjectYaml) throws Exception {
		String apiVersionStr = MapUtils.getString(customObject, KubeConstants.APIVSERION);
		String versionStr = "";
		String groupStr = "";
		if (StringUtils.indexOf(apiVersionStr, "/") < 1) {
			versionStr = apiVersionStr;
		} else {
			groupStr = StringUtils.upperCase(StringUtils.split(apiVersionStr, "/")[0]);
			versionStr = StringUtils.upperCase(StringUtils.split(apiVersionStr, "/")[1]);
		}

		if (CollectionUtils.isNotEmpty(crds)) {
			CRD_LOOP:
			for(K8sCRDResultVO crd : crds) {
				if(!StringUtils.equalsIgnoreCase("Namespaced", crd.getScope())) {
					// Scope가 Namespace가 아니면 Skip..
					continue CRD_LOOP;
				}
				if(StringUtils.isBlank(crd.getGroup()) ||
						CollectionUtils.isNotEmpty(crd.getVersions()) ||
						crd.getAcceptedNames() == null ||
						(crd.getAcceptedNames() != null && StringUtils.isBlank(crd.getAcceptedNames().getPlural()))) {
					// Parameter 누락시 Skip..
					continue CRD_LOOP;
				}

				// 매칭되는 CustomObject의 Definition을 찾음..
				if(StringUtils.equals(customObjectYaml.getCustomObjectKind(), crd.getAcceptedNames().getKind())
						&& StringUtils.equals(crd.getGroup(), groupStr)
				) {
					for (K8sCRDVersionVO version : crd.getVersions()) {
						if (StringUtils.equals(version.getName(), versionStr)) {
							// 배포시 사용을 위해 Custom Resource Definition 정보를 보관
							customObjectYaml.setCustomObjectGroup(crd.getGroup());
							customObjectYaml.setCustomObjectVersion(version.getName());
							customObjectYaml.setCustomObjectPlural(crd.getAcceptedNames().getPlural());
							break CRD_LOOP;
						}
					}
				}
			}
		}
	}

    public void setContainerNameForDeployment(ServerGuiVO serverAddKubesRow){
        if(CollectionUtils.isNotEmpty(serverAddKubesRow.getContainers())){
            int cIdx = 1;
            String baseName = ResourceUtil.getUniqueName(serverAddKubesRow.getComponent().getComponentName());
            for(ContainerVO containerRow : serverAddKubesRow.getContainers()){
                if(StringUtils.isBlank(containerRow.getContainerName())){
                    containerRow.setContainerName(ResourceUtil.getFormattedName(baseName, cIdx++));
                }
            }
        }
    }
}
