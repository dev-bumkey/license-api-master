package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.resource.service.CRDResourceService;
import run.acloud.api.resource.vo.K8sCRDResultVO;
import run.acloud.api.resource.vo.K8sCRDVersionVO;
import run.acloud.api.resource.vo.K8sCRDYamlVO;
import run.acloud.api.resource.vo.K8sCustomObjectVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Kubernetes CRD Management", description = "Kubernetes Custom Resource Definition을 관리 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/crds")
public class CRDController {

	@Autowired
	private CRDResourceService crdService;

	@Autowired
	private ClusterStateService clusterStateService;

	/**
	 * 클러스터에 등록된 Custom Resource Definition 목록을 반환한다.
	 * @param clusterSeq
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}")
	@Operation(summary = "CRD 목록", description = "CRD 목록을 반환한다.")
	public List<K8sCRDResultVO> listCustomResourceDefinition(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq
	) throws Exception {
		log.debug("[BEGIN] listCustomResourceDefinition");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, "");

		List<K8sCRDResultVO> results = null;
		results = crdService.getCustomResourceDefinitions(cluster, null, null);

		log.debug("[END  ] listCustomResourceDefinition");
		return results;
	}

	/**
	 * 클러스터에 Custom Resource Definition를 등록한다.
	 * @param clusterSeq
	 * @param yaml
	 * @return
	 * @throws Exception
	 */
	@PostMapping(value = "/cluster/{clusterSeq}")
	@Operation(summary = "CRD 등록", description = "CRD를 등록한다.")
	public Object createCustomResourceDefinition(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "yamlData", description = "yaml data", required = true) @RequestBody K8sCRDYamlVO yaml
	) throws Exception {
		log.debug("[BEGIN] createCustomResourceDefinition");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, "");

		Object result = null;
		result = crdService.createCustomResourceDefinition(cluster, yaml);

		log.debug("[END  ] createCustomResourceDefinition");
		return result;
	}

	/**
	 * 클러스터에 등록된 Custom Resource Definition 상세 정보를 반환한다.
	 * @param clusterSeq
	 * @param name
	 * @return K8sCRDResultVO
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/{name:.+}")
	@Operation(summary = "CRD 상세 정보", description = "CRD 상세 정보를 반환한다.")
	public K8sCRDResultVO readCustomResourceDefinition(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name
	) throws Exception {
		log.debug("[BEGIN] readCustomResourceDefinition");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, "");

		// PathVariable로 하니, instances.config.istio.io 도메인은 값은 가공되서 전달됨.
		// ex) PathVariable로 받았을 땐 "instances.config.istio"와 같은 식으로 변수에 할당됨.
		// 일단 Header로 Custom Resource Definition 이름을 받고, 이후에 한번 찾아봐야할듯.
		K8sCRDResultVO crd = null;
		crd = crdService.readCustomResourceDefinition(cluster, name);

		if(crd != null) {
			List<?> crds = crdService.readCustomResourceDefinitionRaw(cluster, name);
			if (crds != null && crds.size() > 0) {
				crd.setYaml(crdService.dumpAsMap(crds.get(0)));
			}
		}

		log.debug("[END  ] readCustomResourceDefinition");
		return crd;
	}

	/**
	 * 클러스터에 등록된 Cutom Resource Definition을 삭제한다.
	 * @param clusterSeq
	 * @param name
	 * @return
	 * @throws Exception
	 */
	@DeleteMapping(value = "/cluster/{clusterSeq}/{name:.+}")
	@Operation(summary = "CRD 삭제", description = "CRD를 삭제한다.")
	public void deleteCustomResourceDefinition(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name
	) throws Exception {
		log.debug("[BEGIN] deleteCustomResourceDefinition");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, "");

		// PathVariable로 하니, instances.config.istio.io 도메인은 값은 가공되서 전달됨.
		// ex) PathVariable로 받았을 땐 "instances.config.istio"와 같은 식으로 변수에 할당됨.
		// 일단 Header로 Custom Resource Definition 이름을 받고, 이후에 한번 찾아봐야할듯.
		Object status = null;
		status = crdService.deleteCustomResourceDefinition(cluster, name);

		log.debug("[END  ] deleteCustomResourceDefinition");
//		return status;
	}

	/**
	 * 지정된 CRD 오브젝트 목록(Namespaced)을 반환한다.
	 * @param clusterSeq
	 * @param namespaceName
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/objects/istio/namespaced/{namespaceName}")
	@Operation(summary = "지정된 CRD 오브젝트 목록 (Namespaced)", description = "지정된 CRD 오브젝트 목록(Namespaced)을 반환한다.")
	public List<Map<String, Object>> listNamespacedCustomObjectsIstioObjects(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
	) throws Exception {
		log.debug("[BEGIN] listNamespacedCustomObjectsIstioObjects");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, namespaceName);

		List<K8sCRDResultVO> tmpDefs = crdService.getCustomResourceDefinitions(cluster, "", "");
		if(tmpDefs == null || tmpDefs.size() <= 0) {
			return new ArrayList<>();
		}
		List<K8sCRDResultVO> defs = tmpDefs.stream()
			.filter((s) -> (s.getGroup().contains("networking.istio.io")))
			.collect(Collectors.toList());
		if(defs == null || defs.size() <= 0) {
			return new ArrayList<>();
		}

		List<Map<String, Object>> tmpResults = new ArrayList<>();

		for(K8sCRDResultVO def : defs) {
			Map<String, Object> tmp = new HashMap<>();
			tmp.put("name", def.getFullName());
			tmp.put("group", def.getGroup());
			tmp.put("kind", def.getAcceptedNames().getKind());
			tmp.put("group", def.getGroup());
			tmp.put("plural", def.getAcceptedNames().getPlural());
			tmp.put("storedVersion", def.getStoredVersion());
			List<String> versions = new ArrayList<>();
			for(K8sCRDVersionVO version : def.getVersions()) {
				versions.add(version.getName());
			}
			tmp.put("versions", versions);
			tmpResults.add(tmp);
		}

		List<Map<String, Object>> results = null;
		results = crdService.getCustomObjectsBySingle(cluster, tmpResults);

		log.debug("[END  ] listNamespacedCustomObjectsIstioObjects");
		return results;
	}

	/**
	 * 지정된 CRD 오브젝트 목록(Cluster)을 반환한다.
	 * @param clusterSeq
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/objects/istio/cluster")
	@Operation(summary = "지정된 CRD 오브젝트 목록 (Cluster)", description = "지정된 CRD 오브젝트 목록(Cluster)을 반환한다.")
	public List<Map<String, Object>> listClusterCustomObjectsIstioObjects(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq
	) throws Exception {
		return this.listNamespacedCustomObjectsIstioObjects(clusterSeq, "");
	}

	/**
	 * CRD 오브젝트 생성 유형 목록을 반환한다.
	 * @param clusterSeq
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/objects/istio/types")
	@Operation(summary = "CRD 오브젝트 생성 유형 목록", description = "CRD 오브젝트 생성 유형 목록을 반환한다.")
	public List<Map<String, Object>> listIstioObjectTypes(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq
	) throws Exception {
		log.debug("[BEGIN] listIstioObjectTypes");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, "");

		List<Map<String, Object>> results = new ArrayList<>();

		List<K8sCRDResultVO> tmpDefs = crdService.getCustomResourceDefinitions(cluster, "", "");
		if(tmpDefs == null || tmpDefs.size() <= 0) {
			return new ArrayList<>();
		}
		List<K8sCRDResultVO> defs = tmpDefs.stream()
			.filter((s) -> (s.getGroup().contains("networking.istio.io")))
			.collect(Collectors.toList());
		if(defs == null || defs.size() <= 0) {
			return new ArrayList<>();
		}

		for(K8sCRDResultVO def : defs) {
			Map<String, Object> tmp = new HashMap<>();
			tmp.put("name", def.getFullName());
			tmp.put("group", def.getGroup());
			tmp.put("kind", def.getAcceptedNames().getKind());
			tmp.put("group", def.getGroup());
			tmp.put("plural", def.getAcceptedNames().getPlural());
			tmp.put("storedVersion", def.getStoredVersion());
			List<String> versions = new ArrayList<>();
			for(K8sCRDVersionVO version : def.getVersions()) {
				versions.add(version.getName());
			}
			tmp.put("versions", versions);
			results.add(tmp);
		}

		log.debug("[END  ] listIstioObjectTypes");
		return results;
	}

	/**
	 * CRD 목록(Namespaced)을 반환한다.
	 * @param clusterSeq
	 * @param namespaceName
	 * @param group
	 * @param version
	 * @param plural
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/objects/namespaced/{namespaceName:.+}/{group:.+}/{version}/{plural}")
	@Operation(summary = "CRD 오브젝트 목록 (Namespaced)", description = "CRD 목록(Namespaced)을 반환한다.")
	public List<K8sCustomObjectVO> listNamespacedCustomObjects(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural
	) throws Exception {
		log.debug("[BEGIN] listCustomObjects");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, namespaceName);

		List<K8sCustomObjectVO> results = new ArrayList<>();

		List<Map<String, Object>> tmpObjects = crdService.getCustomObjects(cluster, group, version, plural, null);
		if(tmpObjects != null && tmpObjects.size() > 0) {
			for (Map<String, Object> tmpObject : tmpObjects) {
				K8sCustomObjectVO tmp = crdService.createCustomObjectVO(tmpObject);
//				tmp.put("yaml", crdService.dumpAsMap(tmpObject));
				results.add(tmp);
			}
		}

		log.debug("[END  ] listCustomObjects");
		return results;
	}

	/**
	 * CRD 오브젝트 목록을 반환한다.
	 * @param clusterSeq
	 * @param group
	 * @param version
	 * @param plural
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/objects/cluster/{group:.+}/{version}/{plural}")
	@Operation(summary = "CRD 오브젝트 목록 (Cluster)", description = "CRD 오브젝트 목록(Cluster)을 반환한다.")
	public List<K8sCustomObjectVO> listClusterCustomObjects(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural
	) throws Exception {
		return this.listNamespacedCustomObjects(clusterSeq, "", group, version, plural);
	}

	/**
	 * CRD 오브젝트(Namespaced)를 등록한다.
	 * @param clusterSeq
	 * @param namespaceName
	 * @param group
	 * @param version
	 * @param plural
	 * @param input
	 * @return
	 * @throws Exception
	 */
	@PostMapping(value = "/cluster/{clusterSeq}/objects/namespaced/{namespaceName:.+}/{group:.+}/{version}/{plural}")
	@Operation(summary = "CRD 오브젝트 등록 (Namespaced)", description = "CRD 오브젝트(Namespaced)를 등록한다.")
	public K8sCustomObjectVO createNamespacedCustomObject(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural,
		@Parameter(name = "yamlData", description = "crd object yaml data", required = true) @RequestBody K8sCRDYamlVO input
	) throws Exception {
		log.debug("[BEGIN] createCustomObject");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, namespaceName);

		Map<String, Object> tmp = crdService.createCustomObject(cluster, group, version, plural, input);

		K8sCustomObjectVO result = null;
		if(tmp != null) {
			result = crdService.createCustomObjectVO(tmp);
			result.setYaml(crdService.dumpAsMap(tmp));
		}

		log.debug("[END  ] createCustomObject");
		return result;
	}

	/**
	 * CRD 오브젝트(Cluster)를 등록한다.
	 * @param clusterSeq
	 * @param group
	 * @param version
	 * @param plural
	 * @param input
	 * @return
	 * @throws Exception
	 */
	@PostMapping(value = "/cluster/{clusterSeq}/objects/cluster/{group:.+}/{version}/{plural}")
	@Operation(summary = "CRD 오브젝트 등록 (Cluster)", description = "CRD 오브젝트(Cluster)를 등록한다.")
	public K8sCustomObjectVO createClusterCustomObject(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural,
		@Parameter(name = "yamlData", description = "crd object yaml data", required = true) @RequestBody K8sCRDYamlVO input
	) throws Exception {
		return this.createNamespacedCustomObject(clusterSeq, "", group, version, plural, input);
	}

	/**
	 * CRD 오브젝트(Namespaced)의 상세정보를 반환한다.
	 * @param clusterSeq
	 * @param namespaceName
	 * @param name
	 * @param group
	 * @param version
	 * @param plural
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/objects/namespaced/{namespaceName:.+}/{group:.+}/{version}/{plural}/{name:.+}")
	@Operation(summary = "CRD 오브젝트 상세정보 (Namespaced)", description = "CRD 오브젝트(Namespaced)의 상세정보를 반환한다.")
	public K8sCustomObjectVO readNamespacedCustomObject(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural
	) throws Exception {
		log.debug("[BEGIN] readCustomObject");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, namespaceName);

		Map<String, Object> tmp = crdService.getCustomObject(cluster, name, group, version, plural);

		K8sCustomObjectVO result = null;
		if(tmp != null) {
			result = crdService.createCustomObjectVO(tmp);
			result.setYaml(crdService.dumpAsMap(tmp));
		}

		log.debug("[END  ] readCustomObject");
		return result;
	}

	/**
	 * CRD 오브젝트(Cluster)의 상세정보를 반환한다.
	 * @param clusterSeq
	 * @param name
	 * @param group
	 * @param version
	 * @param plural
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/objects/cluster/{group:.+}/{version}/{plural}/{name:.+}")
	@Operation(summary = "CRD 오브젝트 상세정보 (Cluster)", description = "CRD 오브젝트(Cluster)의 상세정보를 반환한다.")
	public K8sCustomObjectVO readClusterCustomObject(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural
	) throws Exception {
		return this.readNamespacedCustomObject(clusterSeq, "", name, group, version, plural);
	}

	/**
	 * CRD 오브젝트(Namespaced)를 수정한다.
	 * @param clusterSeq
	 * @param namespaceName
	 * @param name
	 * @param group
	 * @param version
	 * @param plural
	 * @param input
	 * @return
	 * @throws Exception
	 */
	@PutMapping(value = "/cluster/{clusterSeq}/objects/namespaced/{namespaceName:.+}/{group:.+}/{version}/{plural}/{name:.+}")
	@Operation(summary = "CRD 오브젝트 수정 (Namespaced)", description = "CRD 오브젝트(Namespaced)를 수정한다.")
	public K8sCustomObjectVO replaceNamespacedCustomObject(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural,
		@Parameter(name = "yamlData", description = "crd object yaml data", required = true) @RequestBody K8sCRDYamlVO input
	) throws Exception {
		log.debug("[BEGIN] replaceCustomObject");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, namespaceName);

		Map<String, Object> tmp = crdService.replaceCustomObject(cluster, name, group, version, plural, input);

		K8sCustomObjectVO result = null;
		if(tmp != null) {
			result = crdService.createCustomObjectVO(tmp);
		}

		log.debug("[END  ] replaceCustomObject");
		return result;
	}

	/**
	 * CRD 오브젝트(Cluster)를 수정한다.
	 * @param clusterSeq
	 * @param name
	 * @param group
	 * @param version
	 * @param plural
	 * @param input
	 * @return
	 * @throws Exception
	 */
	@PutMapping(value = "/cluster/{clusterSeq}/objects/cluster/{group:.+}/{version}/{plural}/{name:.+}")
	@Operation(summary = "CRD 오브젝트 수정 (Cluster)", description = "CRD 오브젝트(Cluster)를 수정한다.")
	public K8sCustomObjectVO replaceClusterCustomObject(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural,
		@Parameter(name = "yamlData", description = "crd object yaml data", required = true) @RequestBody K8sCRDYamlVO input
	) throws Exception {
		return this.replaceNamespacedCustomObject(clusterSeq, "", name, group, version, plural, input);
	}

	/**
	 * CRD 오브젝트(Namespaced)를 삭제한다.
	 * @param clusterSeq
	 * @param namespaceName
	 * @param name
	 * @param group
	 * @param version
	 * @param plural
	 * @throws Exception
	 */
	@DeleteMapping(value = "/cluster/{clusterSeq}/objects/namespaced/{namespaceName:.+}/{group:.+}/{version}/{plural}/{name:.+}")
	@Operation(summary = "CRD 오브젝트 삭제 (Namespaced)", description = "CRD 오브젝트(Namespaced)를 삭제한다.")
	public void deleteNamespacedCustomObject(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural
	) throws Exception {
		log.debug("[BEGIN] deleteCustomObject");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, namespaceName);

		crdService.deleteCustomObject(cluster, name, group, version, plural);

		log.debug("[END  ] deleteCustomObject");
	}

	/**
	 * CRD 오브젝트(Cluster)를 삭제한다.
	 * @param clusterSeq
	 * @param name
	 * @param group
	 * @param version
	 * @param plural
	 * @throws Exception
	 */
	@DeleteMapping(value = "/cluster/{clusterSeq}/objects/cluster/{group:.+}/{version}/{plural}/{name:.+}")
	@Operation(summary = "CRD 오브젝트 삭제 (Cluster)", description = "CRD 오브젝트(Cluster)를 삭제한다.")
	public void deleteClusterCustomObject(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name,
		@Parameter(name = "group", description = "crd group", required = true) @PathVariable String group,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version,
		@Parameter(name = "plural", description = "crd plural", required = true) @PathVariable String plural
	) throws Exception {
		this.deleteNamespacedCustomObject(clusterSeq, "", name, group, version, plural);
	}

	/**
	 * YAML (Namespaced) 템플릿 문자열을 반환한다.
	 * @param clusterSeq
	 * @param namespaceName
	 * @param name
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/templates/namespaced/{namespaceName:.+}/{name:.+}/{version}")
	@Operation(summary = "CRD 오브젝트 템플릿 생성 (Namespaced)", description = "CRD 오브젝트(Namespaced)의 템플릿 생성한다.")
	public Map<String, Object> genNamespacedTemplate(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version
	) throws Exception {
		log.debug("[BEGIN] genNamespacedTemplate");

		/** cluster 상태 체크 */
		clusterStateService.checkClusterState(clusterSeq);
		ClusterVO cluster = crdService.setupCluster(clusterSeq, namespaceName);

		Map<String, Object> result = new HashMap<>();

		StringBuilder sb = new StringBuilder();

		List<Map<String, Object>> tmpls = crdService.genTemplate(cluster, namespaceName, name, version);
		if(tmpls == null) {
			result.put("yaml", "");
		}
		else {
			for (Map<String, Object> tmpl : tmpls) {
				sb.append(crdService.dumpAsMap(tmpl));
			}
			result.put("yaml", sb.toString());
		}

		log.debug("[END  ] genNamespacedTemplate");
		return result;
	}

	/**
	 * YAML (Cluster) 템플릿 문자열을 반환한다.
	 * @param clusterSeq
	 * @param name
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/cluster/{clusterSeq}/templates/cluster/{name:.+}/{version}")
	@Operation(summary = "CRD 오브젝트 템플릿 생성", description = "CRD 오브젝트의 템플릿 생성한다.")
	public Map<String, Object> genClusterTemplate(
		@Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "name", description = "crd name", required = true) @PathVariable String name,
		@Parameter(name = "version", description = "crd version", required = true) @PathVariable String version
	) throws Exception {
		return this.genNamespacedTemplate(clusterSeq, "", name, version);
	}

	/**
	 * YAML 템플릿 문자열을 반환한다. (Default)
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/templates")
	@Operation(summary = "CRD 오브젝트 템플릿 생성 (Default)", description = "CRD 오브젝트의 템플릿 생성한다. (Default)")
	public Map<String, Object> genTemplate() throws Exception {
		log.debug("[BEGIN] genTemplate");

		Map<String, Object> result = new HashMap<>();

		Map<String, Object> templateObj = crdService.genTemplateCommon("", "", "");
		if(templateObj == null) {
			result.put("yaml", "");
		}
		else {
			result.put("yaml", crdService.dumpAsMap(templateObj));
		}

		log.debug("[END  ] genTemplate");
		return result;
	}

}
