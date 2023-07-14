package run.acloud.api.resource.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.version.Version;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.k8sextended.apis.*;
import run.acloud.api.k8sextended.models.*;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.PodLogVO;
import run.acloud.commons.provider.K8sClient;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class K8sWorker {

    @Autowired
	private K8sClient k8sClient;

    @Autowired
    private K8sTokenGenerator k8sTokenGenerator;

    @Autowired
    private ClusterStateService clusterStateService;

    private final JSON k8sJson = new JSON();

    /**
     * ApiClient 생성
     *
     * @param cluster
     * @return
     * @throws Exception
     */
	private ApiClient makeConnection(ClusterVO cluster) throws Exception {

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        boolean isUpdated = k8sTokenGenerator.refreshClusterToken(cluster);

        ApiClient apiClient;

        if (this.k8sSupported(cluster)) {
            apiClient = (ApiClient) this.k8sClient.create(
                    cluster.getAuthType(),
                    cluster.getCubeType(),
                    cluster.getApiUrl(),
                    cluster.getApiKey(),
                    cluster.getApiSecret(),
                    cluster.getClientAuthData(),
                    cluster.getClientKeyData(),
                    cluster.getServerAuthData());

            if(EnumSet.of(CubeType.EKS, CubeType.NCPKS).contains(cluster.getCubeType())){
                log.debug("{} apiClient : {}, isUpdated : {}", cluster.getCubeType().getCode(), apiClient, isUpdated);
            }
        } else {
            throw new CocktailException(String.format("Cube Cluster version[%s] is not support.", cluster.getK8sVersion()), ExceptionType.K8sNotSupported);
        }

        return apiClient;
	}

    /**
     * Pod Collection 삭제
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public V1Status deleteCollectionNamespacedPodV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            return apiInstance.deleteCollectionNamespacedPod(namespace, null, null, null, fieldSelector, 0, labelSelector, null, null, null, null, null, null, new V1DeleteOptions());
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Pod Collection 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param gracePeriodSeconds
     * @param force
     * @return
     * @throws Exception
     */
    public V1Pod deleteNamespacedPodV1(ClusterVO cluster, String namespace, String name, Integer gracePeriodSeconds, boolean force) throws Exception{
        if(this.k8sSupported(cluster)){
            CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
            try {
                /*
                    // kubectl delete 로직 참조 (1.22)
                    // https://github.com/kubernetes/kubectl/blob/master/pkg/cmd/delete/delete_flags.go
                	if f.Force != nil {
                        options.ForceDeletion = *f.Force
                    }
                    if f.GracePeriod != nil {
                        options.GracePeriod = *f.GracePeriod
                    }
                    if f.Now != nil {
                        options.DeleteNow = *f.Now
                    }

                    // https://github.com/kubernetes/kubectl/blob/master/pkg/cmd/delete/delete.go
                    if o.DeleteNow {
                        if o.GracePeriod != -1 {
                            return fmt.Errorf("--now and --grace-period cannot be specified together")
                        }
                        o.GracePeriod = 1
                    }
                    if o.GracePeriod == 0 && !o.ForceDeletion {
                        // To preserve backwards compatibility, but prevent accidental data loss, we convert --grace-period=0
                        // into --grace-period=1. Users may provide --force to bypass this conversion.
                        o.GracePeriod = 1
                    }
                    if o.ForceDeletion && o.GracePeriod < 0 {
                        o.GracePeriod = 0
                    }
                 */
                if (gracePeriodSeconds != null) {
                    if (gracePeriodSeconds.longValue() == 0 && !force) {
                        gracePeriodSeconds = 1;
                    }
                    if (force && gracePeriodSeconds.longValue() < 0) {
                        gracePeriodSeconds = 0;
                    }
                } else {
                    if (force) {
                        gracePeriodSeconds = 0;
                    }
                }
                return apiInstance.deleteNamespacedPod(name, namespace, null, null, gracePeriodSeconds, null, null, new V1DeleteOptions());
            } catch (Exception e) {
                this.exceptionHandle(e);
            }
        }

        return null;
    }

    /**
     * Pod 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Pod> getPodV1WithLabel(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        return this.getPodV1WithLabel(cluster, namespace, fieldSelector, labelSelector, null);
    }

    public List<V1Pod> getPodV1WithLabel(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector, Integer limit) throws Exception{
        if(StringUtils.isNotBlank(namespace)){
            CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

            try {
                V1PodList v1PodList = apiInstance.listNamespacedPod(namespace, null, null, null, fieldSelector, labelSelector, limit, null, null, null, null);

                if(v1PodList != null){
                    if(CollectionUtils.isNotEmpty(v1PodList.getItems())){
                        for(V1Pod v1Pod : v1PodList.getItems()) {
                            v1Pod.setKind(K8sApiKindType.POD.getValue());
                            v1Pod.setApiVersion(v1PodList.getApiVersion());
                        }
                        return v1PodList.getItems();
                    }
                }
            } catch (Exception e) {
                this.exceptionHandle(e);
            }
        }

        return new ArrayList<>();
    }

    /**
     * Pod Status 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Pod getPodV1WithName(ClusterVO cluster, String namespace, String name) throws Exception{
        if(StringUtils.isNotBlank(namespace)){
            CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
            try {
                V1Pod v1PodStatus = apiInstance.readNamespacedPodStatus(name, namespace, null);

                if(v1PodStatus != null){
                    return v1PodStatus;
                }
            } catch (Exception e) {
                this.exceptionHandle(e);
            }
        }

        return null;
    }

    /**
     * Pod 목록 조회 - AllNamespace
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Pod> getPodV1AllNamespace(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        return this.getPodV1AllNamespace(cluster, fieldSelector, labelSelector, null);
    }

    public List<V1Pod> getPodV1AllNamespace(ClusterVO cluster, String fieldSelector, String labelSelector, Integer limit) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1PodList v1PodList = apiInstance.listPodForAllNamespaces(null, null, fieldSelector, labelSelector, limit, null, null, null, null, null);

            if(v1PodList != null){
                if(CollectionUtils.isNotEmpty(v1PodList.getItems())){
                    for(V1Pod v1Pod : v1PodList.getItems()) {
                        v1Pod.setKind(K8sApiKindType.POD.getValue());
                        v1Pod.setApiVersion(v1PodList.getApiVersion());
                    }
                    return v1PodList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Pod 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1Pod> getPodV1(ClusterVO cluster, String namespace, String name, String field, String label) throws Exception{
        return this.getPodV1(cluster, namespace, name, field, label, null);
    }

    public List<V1Pod> getPodV1(ClusterVO cluster, String namespace, String name, String field, String label, Integer limit) throws Exception{
        List<V1Pod> pods = new ArrayList<>();
        if (StringUtils.isNotBlank(namespace)){
            if (StringUtils.isNotBlank(name)) {
                V1Pod v1Pod = this.getPodV1WithName(cluster, namespace, name);
                if(v1Pod != null){
                    pods.add(v1Pod);
                }
            } else {
                pods.addAll(this.getPodV1WithLabel(cluster, namespace, field, label, limit));
            }
        }else{
            pods.addAll(this.getPodV1AllNamespace(cluster, field, label, limit));
        }

        return pods;
    }

    /**
     * Pod > Container Log 조회
     *
     * @param cluster
     * @param namespace
     * @param name - String | name of the Pod
     * @param container - String | The container for which to stream logs. Defaults to only container if there is one container in the pod.
     * @param follow - Boolean | Follow the log stream of the pod. Defaults to false.
     * @param limitBytes - Integer | If set, the number of bytes to read from the server before terminating the log output. This may not display a complete final line of logging, and may return slightly more or slightly less than the specified limit.
     * @param pretty - String | If 'true', then the output is pretty printed.
     * @param previous - Boolean | Return previous terminated container logs. Defaults to false.
     * @param sinceSeconds - Integer | A relative time in seconds before the current time from which to show logs. If this value precedes the time a pod was started, only logs since the pod start will be returned. If this value is in the future, no logs will be returned. Only one of sinceSeconds or sinceTime may be specified.
     * @param tailLines - Integer | If set, the number of lines from the end of the logs to show. If not specified, logs are shown from the creation of the container or sinceSeconds or sinceTime
     * @param timestamps - Boolean | If true, add an RFC3339 or RFC3339Nano timestamp at the beginning of every line of log output. Defaults to false.
     * @return
     * @throws Exception
     */
    public List<PodLogVO> getContainerLogsInPodV1(ClusterVO cluster, String namespace, String name, String container, Boolean follow, Integer limitBytes, String pretty, Boolean previous, Integer sinceSeconds, Integer tailLines, Boolean timestamps) throws Exception{

        List<PodLogVO> containerLogs = new ArrayList<>();
        if (StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){
            CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

            try {
                if (StringUtils.isNotBlank(container)) {
                    PodLogVO podLog = new PodLogVO();
                    podLog.setPodName(name);
                    podLog.setContainerName(container);
                    podLog.setLog(apiInstance.readNamespacedPodLog(name, namespace, container, follow, null, limitBytes, pretty, previous, sinceSeconds, tailLines, timestamps));
                    containerLogs.add(podLog);
                } else {
                    V1Pod v1Pod = this.getPodV1WithName(cluster, namespace, name);
                    if(v1Pod != null && v1Pod.getSpec() != null){
                        if(CollectionUtils.isNotEmpty(v1Pod.getSpec().getContainers())){
                            for(V1Container v1ContainerRow : v1Pod.getSpec().getContainers()){
                                PodLogVO podLog = new PodLogVO();
                                podLog.setPodName(name);
                                podLog.setContainerName(v1ContainerRow.getName());
                                podLog.setLog(apiInstance.readNamespacedPodLog(name, namespace, v1ContainerRow.getName(), follow, null, limitBytes, pretty, previous, sinceSeconds, tailLines, timestamps));
                                containerLogs.add(podLog);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                this.exceptionHandle(e);
            }
        }else{
            throw new CocktailException("Both namespace & podName is null", ExceptionType.InvalidParameter);
        }

        return CollectionUtils.isNotEmpty(containerLogs) ? containerLogs : this.getEmpytLogs();
    }

    private List<PodLogVO> getEmpytLogs(){
        List<PodLogVO> emptyLog = new ArrayList<>();
        PodLogVO log = new PodLogVO();
        log.setPodName("no-name");
        log.setContainerName("no-name");
        log.setLog("<Pod not exists>");
        emptyLog.add(log);

        return emptyLog;
    }

    /**
     * Event 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<CoreV1Event> getEventV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            CoreV1EventList v1EventList;

            if(StringUtils.isNotBlank(namespace)){
                v1EventList = apiInstance.listNamespacedEvent(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1EventList = apiInstance.listEventForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1EventList != null){
                if(CollectionUtils.isNotEmpty(v1EventList.getItems())){
                    return v1EventList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Service 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1Service createServiceV1(ClusterVO cluster, String namespace, V1Service param, boolean dryRun) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1Service result = apiInstance.createNamespacedService(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createServiceV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'Service': %s", Optional.ofNullable(param).map(V1Service::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sServiceCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Service Patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Service patchServiceV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {

            V1Service result = apiInstance.patchNamespacedService(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchServiceV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'Service': %s", name),
                        ExceptionType.K8sServiceCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Service Replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1Service replaceServiceV1(ClusterVO cluster, String namespace, String name, V1Service param) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {

            V1Service result = apiInstance.replaceNamespacedService(name, namespace, param, null, null, null, null);
            log.debug("replaceServiceV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to Replace 'Service': %s", name),
                        ExceptionType.K8sServiceCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Service 삭제 with Name
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteServiceV1WithName(ClusterVO cluster, String namespace, String name) throws Exception {

        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Service result = apiInstance.deleteNamespacedService(name, namespace, null, null, null, null, null, new V1DeleteOptions());
            log.debug("deleteServiceV1WithName result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

    }

    /**
     * Service 삭제 with Label
     *
     * @param cluster
     * @param namespace
     * @param label
     * @throws Exception
     */
    public void deleteServiceV1WithLabel(ClusterVO cluster, String namespace, String label) throws Exception {

        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            List<V1Service> services = this.getServicesV1(cluster, namespace, null, label);
            if(CollectionUtils.isNotEmpty(services)){
                for (V1Service serviceRow : services) {
                    if (serviceRow != null && serviceRow.getMetadata() != null) {
                        apiInstance.deleteNamespacedService(serviceRow.getMetadata().getName(), namespace, null, null, null, null, null, new V1DeleteOptions());
                    }
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

    }

    /**
     * Service 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Service> getServicesV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1ServiceList v1ServiceList;

            if(StringUtils.isNotBlank(namespace)){
                v1ServiceList = apiInstance.listNamespacedService(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1ServiceList = apiInstance.listServiceForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1ServiceList != null){
                if(CollectionUtils.isNotEmpty(v1ServiceList.getItems())){
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(V1Service v1Service : v1ServiceList.getItems()) {
                        v1Service.setKind(K8sApiKindType.SERVICE.getValue());
                        v1Service.setApiVersion(v1ServiceList.getApiVersion());
                    }
                    return v1ServiceList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Service 조회
     *
     * @param cluster
     * @param namespace
     * @return
     * @throws Exception
     */
    public V1Service getServiceV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            return apiInstance.readNamespacedServiceStatus(name, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
        return null;
    }

    /**
     * Endpoint 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Endpoints> getEndpointV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1EndpointsList v1EndpointsList;

            if(StringUtils.isNotBlank(namespace)){
                v1EndpointsList = apiInstance.listNamespacedEndpoints(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1EndpointsList = apiInstance.listEndpointsForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1EndpointsList != null){
                if(CollectionUtils.isNotEmpty(v1EndpointsList.getItems())){
                    return v1EndpointsList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * PersistentVolumeClaim 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public void createPersistentVolumeClaimV1(ClusterVO cluster, String namespace, V1PersistentVolumeClaim param) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1PersistentVolumeClaim result = apiInstance.createNamespacedPersistentVolumeClaim(namespace, param, null, null, null, null);
            log.debug("createPersistentVolumeClaimV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'PersistentVolumeClaim': %s", Optional.ofNullable(param).map(V1PersistentVolumeClaim::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sVolumeClaimCreationFail);
            }

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

    }

    /**
     * PersistentVolumeClaim patch
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1PersistentVolumeClaim patchPersistentVolumeClaimV1(ClusterVO cluster, String namespace, V1PersistentVolumeClaim param, Object body) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1PersistentVolumeClaim result = null;
            String name = Optional.ofNullable(param).map(V1PersistentVolumeClaim::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"");
            if (StringUtils.isNotBlank(name)) {
                result = apiInstance.patchNamespacedPersistentVolumeClaim(name, namespace, new V1Patch(JsonUtils.toGson(body)), "true", null, null, null, null);
            }
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'PersistentVolumeClaim': %s", name),
                        ExceptionType.K8sVolumeClaimUpdateFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * PersistentVolumeClaim Replace
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1PersistentVolumeClaim replacePersistentVolumeClaimV1(ClusterVO cluster, String namespace, String name, V1PersistentVolumeClaim param) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1PersistentVolumeClaim result = apiInstance.replaceNamespacedPersistentVolumeClaim(name, namespace, param, null, null, null, null);
            log.debug("replacePersistentVolumeClaimV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'PersistentVolumeClaim': %s", name), ExceptionType.K8sVolumeClaimUpdateFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }


    /**
     * PersistentVolumeClaim 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deletePersistentVolumeClaimV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1PersistentVolumeClaim result = apiInstance.deleteNamespacedPersistentVolumeClaim(name, namespace, null, null, null, null, null, new V1DeleteOptions());
            log.debug("deletePersistentVolumeClaimV1 result: {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * PersistentVolumeClaim 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1PersistentVolumeClaim getPersistentVolumeClaimV1WithName(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1PersistentVolumeClaim v1PersistentVolumeClaimStatus = apiInstance.readNamespacedPersistentVolumeClaimStatus(name, namespace, null);

            if(v1PersistentVolumeClaimStatus != null){
                return v1PersistentVolumeClaimStatus;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * PersistentVolumeClaim 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1PersistentVolumeClaim> getPersistentVolumeClaimsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1PersistentVolumeClaimList v1PersistentVolumeClaimList;

            if(StringUtils.isNotBlank(namespace)){
                v1PersistentVolumeClaimList = apiInstance.listNamespacedPersistentVolumeClaim(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1PersistentVolumeClaimList = apiInstance.listPersistentVolumeClaimForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1PersistentVolumeClaimList != null){
                if(CollectionUtils.isNotEmpty(v1PersistentVolumeClaimList.getItems())){
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(V1PersistentVolumeClaim v1PersistentVolumeClaim : v1PersistentVolumeClaimList.getItems()) {
                        v1PersistentVolumeClaim.setKind(K8sApiKindType.PERSISTENT_VOLUME_CLAIM.getValue());
                        v1PersistentVolumeClaim.setApiVersion(v1PersistentVolumeClaimList.getApiVersion());
                    }
                    return v1PersistentVolumeClaimList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * PersistentVolume 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public void createPersistentVolumeV1(ClusterVO cluster, V1PersistentVolume param) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1PersistentVolume result = null;
            String name = Optional.ofNullable(param).map(V1PersistentVolume::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"");
            if (StringUtils.isNotBlank(name)) {
                result = apiInstance.createPersistentVolume(param, null, null, null, null);
            }
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'PersistentVolume': %s", name), ExceptionType.K8sVolumeCreationFail);
            }

        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * PersistentVolume Patch
     *
     * @param cluster cluster 정보
     * @param param 기존 조회된 PV 정보
     * @param body 변경할 PV body 정보
     * @return
     * @throws Exception
     */
    public V1PersistentVolume patchPersistentVolumeV1(ClusterVO cluster, V1PersistentVolume param, Object body) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {

            V1PersistentVolume result = null;
            String name = Optional.ofNullable(param).map(V1PersistentVolume::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"");
            if (StringUtils.isNotBlank(name)) {
                result = apiInstance.patchPersistentVolume(name, new V1Patch(JsonUtils.toGson(body)), "true", null, null, null, null);
            }
            if(result == null) {
                throw new CocktailException(String.format("Fail to update 'PersistentVolume': %s", name), ExceptionType.K8sVolumeUpdateFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * PersistentVolume Replace
     *
     * @param cluster Cluster 정보
     * @param name 변경할 PV Name
     * @param param 변경할 PV Body 정보
     * @return
     * @throws Exception
     */
    public V1PersistentVolume replacePersistentVolumeV1(ClusterVO cluster, String name, V1PersistentVolume param) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {

            V1PersistentVolume result = apiInstance.replacePersistentVolume(name, param, null, null, null, null);
            log.debug("replacePersistentVolumeV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to update 'PersistentVolume': %s", name), ExceptionType.K8sVolumeUpdateFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * PersistentVolume 삭제
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public void deletePersistentVolumeV1(ClusterVO cluster, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1PersistentVolume result = apiInstance.deletePersistentVolume(name, null, null, null, null, null, new V1DeleteOptions());
            log.debug("deletePersistentVolumeV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }


    /**
     * PersistentVolume 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1PersistentVolume getPersistentVolumeV1WithName(ClusterVO cluster, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1PersistentVolume v1PersistentVolumeStatus = apiInstance.readPersistentVolumeStatus(name, null);

            if(v1PersistentVolumeStatus != null){
                return v1PersistentVolumeStatus;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * PersistentVolume 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1PersistentVolume> getPersistentVolumesV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1PersistentVolumeList v1PersistentVolumeList = apiInstance.listPersistentVolume(null, null, null, fieldSelector, labelSelector, null, null, null, 56, null);

            if(v1PersistentVolumeList != null){
                if(CollectionUtils.isNotEmpty(v1PersistentVolumeList.getItems())){
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(V1PersistentVolume v1PersistentVolume : v1PersistentVolumeList.getItems()) {
                        v1PersistentVolume.setKind(K8sApiKindType.PERSISTENT_VOLUME.getValue());
                        v1PersistentVolume.setApiVersion(v1PersistentVolumeList.getApiVersion());
                    }
                    return v1PersistentVolumeList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * StorageClass 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public void createStorageClassV1(ClusterVO cluster, V1StorageClass param) throws Exception{
        StorageV1Api apiInstance = new StorageV1Api(this.makeConnection(cluster));

        try {
            V1StorageClass result = apiInstance.createStorageClass(param, null, null, null, null);
            log.debug("createStorageClassV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'StorageClass': %s", Optional.ofNullable(param).map(V1StorageClass::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->""))
                        , ExceptionType.K8sStorageClassCreationFail);
            }

        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * StorageClass V1 patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1StorageClass patchStorageClassV1(ClusterVO cluster, String name, List<JsonObject> patchBody) throws Exception {
        StorageV1Api apiInstance = new StorageV1Api(this.makeConnection(cluster));

        try {
            log.debug("Patch StorageClass content: {}", JsonUtils.toGson(patchBody));
            V1StorageClass result = apiInstance.patchStorageClass(name, new V1Patch(JsonUtils.toGson(patchBody)), null, null, null, null, null);
            log.debug("patchStorageClassV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'StorageClass': %s", name), ExceptionType.K8sStorageClassCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * StorageClass 목록 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1StorageClass> getStorageClassesV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        StorageV1Api apiInstance = new StorageV1Api(this.makeConnection(cluster));
        try {
            V1StorageClassList v1StorageClassList = apiInstance.listStorageClass(null, null, null, fieldSelector, labelSelector, null, null, null, 56, null);

            if(v1StorageClassList != null){
                if(CollectionUtils.isNotEmpty(v1StorageClassList.getItems())){
                    for (V1StorageClass v1StorageClass : v1StorageClassList.getItems()) {
                        v1StorageClass.setApiVersion(String.format("%s/%s", K8sApiGroupType.STORAGE.getValue(), K8sApiType.V1.getValue()));
                        v1StorageClass.setKind(K8sApiKindType.STORAGE_CLASS.getValue());
                    }
                    return v1StorageClassList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * StorageClass 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1StorageClass getStorageClassV1(ClusterVO cluster, String name) throws Exception{
        StorageV1Api apiInstance = new StorageV1Api(this.makeConnection(cluster));

        try {

            return apiInstance.readStorageClass(name, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * StorageClass 삭제
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1StorageClass deleteStorageClassV1(ClusterVO cluster, String name) throws Exception{
        StorageV1Api apiInstance = new StorageV1Api(this.makeConnection(cluster));
        try {
            V1StorageClass result = apiInstance.deleteStorageClass(name, null, null, 0, null, null, new V1DeleteOptions());
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Deployment V1 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1Deployment createDeploymentV1(ClusterVO cluster, String namespace, V1Deployment param, boolean dryRun) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1Deployment result = apiInstance.createNamespacedDeployment(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createDeploymentV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'Deployment': %s", Optional.ofNullable(param).map(V1Deployment::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sDeploymentCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Deployment V1 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1Deployment replaceDeploymentV1(ClusterVO cluster, String namespace, String name, V1Deployment param) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1Deployment result = apiInstance.replaceNamespacedDeployment(name, namespace, param, null, null, null, null);
            log.debug("replaceDeploymentV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'Deployment': %s", Optional.ofNullable(param).map(V1Deployment::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sDeploymentCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Deployment 삭제
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteDeploymentV1(ClusterVO cluster, String namespace, String name) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1Status result = apiInstance.deleteNamespacedDeployment(name, namespace, null, null, null, null, null, new V1DeleteOptions());
            if (result != null) {
                log.debug("deleteDeploymentV1 result: {}", k8sJson.serialize(result));
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Deployment V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Deployment patchDeploymentV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1Deployment result = apiInstance.patchNamespacedDeployment(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchDeploymentV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'Deployment': %s", name), ExceptionType.K8sDeploymentCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Deployment Scale V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Scale patchDeploymentScaleV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));
        try {
            V1Scale result = apiInstance.patchNamespacedDeploymentScale(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Deployment V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<AppsV1beta1Deployment> getDeploymentsV1beta1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1beta1Api apiInstance = new AppsV1beta1Api(this.makeConnection(cluster));
        try {
            AppsV1beta1DeploymentList v1beta1DeploymentList;

            if(StringUtils.isNotBlank(namespace)){
                v1beta1DeploymentList = apiInstance.listNamespacedDeployment(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null);
            }else{
                v1beta1DeploymentList = apiInstance.listDeploymentForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, 56, null);
            }

            if(v1beta1DeploymentList != null){
                if(CollectionUtils.isNotEmpty(v1beta1DeploymentList.getItems())){
                    return v1beta1DeploymentList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Deployment V1beta2 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta2Deployment> getDeploymentsV1beta2(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1beta2Api apiInstance = new AppsV1beta2Api(this.makeConnection(cluster));

        try {
            V1beta2DeploymentList v1beta2DeploymentList;

            if(StringUtils.isNotBlank(namespace)){
                v1beta2DeploymentList = apiInstance.listNamespacedDeployment(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null);
            }else{
                v1beta2DeploymentList = apiInstance.listDeploymentForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, 56, null);
            }

            if(v1beta2DeploymentList != null){
                if(CollectionUtils.isNotEmpty(v1beta2DeploymentList.getItems())){
                    return v1beta2DeploymentList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Deployment V1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Deployment> getDeploymentsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1DeploymentList v1DeploymentList;

            if(StringUtils.isNotBlank(namespace)){
                v1DeploymentList = apiInstance.listNamespacedDeployment(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1DeploymentList = apiInstance.listDeploymentForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1DeploymentList != null){
                if(CollectionUtils.isNotEmpty(v1DeploymentList.getItems())){
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(V1Deployment v1Deployment : v1DeploymentList.getItems()) {
                        v1Deployment.setKind(K8sApiKindType.DEPLOYMENT.getValue());
                        v1Deployment.setApiVersion(v1DeploymentList.getApiVersion());
                    }
                    return v1DeploymentList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Deployment V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Deployment getDeploymentV1(ClusterVO cluster, String namespace, String name) throws Exception{
        return this.getDeploymentV1(cluster, namespace, name, false);
    }

    /**
     * Deployment V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Deployment getDeploymentV1(ClusterVO cluster, String namespace, String name, Boolean getStatus) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1Deployment v1Deployment;
            if(BooleanUtils.isTrue(getStatus)){
                v1Deployment = apiInstance.readNamespacedDeploymentStatus(name, namespace, null);
            }else{
                v1Deployment = apiInstance.readNamespacedDeployment(name, namespace, null);
            }

            if(v1Deployment != null){
                return v1Deployment;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }


    /**
     * StatefulSet V1 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1StatefulSet createStatefulSetV1(ClusterVO cluster, String namespace, V1StatefulSet param, boolean dryRun) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1StatefulSet result = apiInstance.createNamespacedStatefulSet(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("New StatefulSet content: {}", k8sJson.serialize(param));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'StatefulSet': %s", Optional.ofNullable(param).map(V1StatefulSet::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sStatefulSetCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * StatefulSet V1 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1StatefulSet replaceStatefulSetV1(ClusterVO cluster, String namespace, String name, V1StatefulSet param) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1StatefulSet result = apiInstance.replaceNamespacedStatefulSet(name, namespace, param, null, null, null, null);
            log.debug("Replace StatefulSet content: {}", k8sJson.serialize(param));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'StatefulSet': %s", Optional.ofNullable(param).map(V1StatefulSet::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sStatefulSetCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * StatefulSet 삭제
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteStatefulSetV1(ClusterVO cluster, String namespace, String name) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1Status v1Status = apiInstance.deleteNamespacedStatefulSet(name, namespace, null, null, null, null, null, new V1DeleteOptions());
            if (v1Status != null) {
                log.debug("### V1Status.getStatus() : " + v1Status.getStatus());
                return v1Status;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * StatefulSet V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1StatefulSet patchStatefulSetV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1StatefulSet result = apiInstance.patchNamespacedStatefulSet(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("Patch StatefulSet content: {}", JsonUtils.toGson(patchBody));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'StatefulSet': %s", name), ExceptionType.K8sStatefulSetCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * StatefulSet Scale V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Scale patchStatefulSetScaleV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));
        try {
            V1Scale result = apiInstance.patchNamespacedStatefulSetScale(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * StatefulSet V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta1StatefulSet> getStatefulSetsV1beta1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1beta1Api apiInstance = new AppsV1beta1Api(this.makeConnection(cluster));
        try {
            V1beta1StatefulSetList v1beta1StatefulSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1beta1StatefulSetList = apiInstance.listNamespacedStatefulSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null);
            }else{
                v1beta1StatefulSetList = apiInstance.listStatefulSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, 56, null);
            }

            if(v1beta1StatefulSetList != null){
                if(CollectionUtils.isNotEmpty(v1beta1StatefulSetList.getItems())){
                    return v1beta1StatefulSetList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * StatefulSet V1beta2 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta2StatefulSet> getStatefulSetsV1beta2(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1beta2Api apiInstance = new AppsV1beta2Api(this.makeConnection(cluster));

        try {
            V1beta2StatefulSetList v1beta2StatefulSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1beta2StatefulSetList = apiInstance.listNamespacedStatefulSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null);
            }else{
                v1beta2StatefulSetList = apiInstance.listStatefulSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, 56, null);
            }

            if(v1beta2StatefulSetList != null){
                if(CollectionUtils.isNotEmpty(v1beta2StatefulSetList.getItems())){
                    return v1beta2StatefulSetList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * StatefulSet V1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1StatefulSet> getStatefulSetsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1StatefulSetList v1StatefulSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1StatefulSetList = apiInstance.listNamespacedStatefulSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1StatefulSetList = apiInstance.listStatefulSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1StatefulSetList != null){
                if(CollectionUtils.isNotEmpty(v1StatefulSetList.getItems())){
                    for(V1StatefulSet v1StatefulSet : v1StatefulSetList.getItems()) {
                        v1StatefulSet.setKind(K8sApiKindType.STATEFUL_SET.getValue());
                        v1StatefulSet.setApiVersion(v1StatefulSetList.getApiVersion());
                    }
                    return v1StatefulSetList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * StatefulSet V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1StatefulSet getStatefulSetV1(ClusterVO cluster, String namespace, String name) throws Exception{
        return this.getStatefulSetV1(cluster, namespace, name, false);
    }

    /**
     * StatefulSet V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1StatefulSet getStatefulSetV1(ClusterVO cluster, String namespace, String name, Boolean getStatus) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1StatefulSet v1StatefulSet;
            if(BooleanUtils.isTrue(getStatus)){
                v1StatefulSet = apiInstance.readNamespacedStatefulSetStatus(name, namespace, null);
            }else{
                v1StatefulSet = apiInstance.readNamespacedStatefulSet(name, namespace, null);
            }

            if(v1StatefulSet != null){
                return v1StatefulSet;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * DaemonSet V1 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1DaemonSet createDaemonSetV1(ClusterVO cluster, String namespace, V1DaemonSet param, boolean dryRun) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1DaemonSet result = apiInstance.createNamespacedDaemonSet(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("New DaemonSet content: {}", k8sJson.serialize(param));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'DaemonSet': %s", Optional.ofNullable(param).map(V1DaemonSet::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sDaemonSetCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * DaemonSet V1 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1DaemonSet replaceDaemonSetV1(ClusterVO cluster, String namespace, String name, V1DaemonSet param) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1DaemonSet result = apiInstance.replaceNamespacedDaemonSet(name, namespace, param, null, null, null, null);
            log.debug("Replace DaemonSet content: {}", k8sJson.serialize(param));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'DaemonSet': %s", Optional.ofNullable(param).map(V1DaemonSet::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sDaemonSetCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * DaemonSet 삭제
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteDaemonSetV1(ClusterVO cluster, String namespace, String name) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1Status v1Status = apiInstance.deleteNamespacedDaemonSet(name, namespace, null, null, null, null, null, new V1DeleteOptions());
            if (v1Status != null) {
                log.debug("### V1Status.getStatus() : " + v1Status.getStatus());
                return v1Status;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * DaemonSet V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1DaemonSet patchDaemonSetV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1DaemonSet result = apiInstance.patchNamespacedDaemonSet(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("Patch DaemonSet content: {}", JsonUtils.toGson(patchBody));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'DaemonSet': %s", name), ExceptionType.K8sDaemonSetCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * DaemonSet V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta1DaemonSet> getDaemonSetsV1beta1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(this.makeConnection(cluster));
        try {
            V1beta1DaemonSetList v1beta1DaemonSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1beta1DaemonSetList = apiInstance.listNamespacedDaemonSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null);
            }else{
                v1beta1DaemonSetList = apiInstance.listDaemonSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, 56, null);
            }

            if(v1beta1DaemonSetList != null){
                if(CollectionUtils.isNotEmpty(v1beta1DaemonSetList.getItems())){
                    return v1beta1DaemonSetList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * DaemonSet V1beta2 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta2DaemonSet> getDaemonSetsV1beta2(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1beta2Api apiInstance = new AppsV1beta2Api(this.makeConnection(cluster));

        try {
            V1beta2DaemonSetList v1beta2DaemonSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1beta2DaemonSetList = apiInstance.listNamespacedDaemonSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null);
            }else{
                v1beta2DaemonSetList = apiInstance.listDaemonSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, 56, null);
            }

            if(v1beta2DaemonSetList != null){
                if(CollectionUtils.isNotEmpty(v1beta2DaemonSetList.getItems())){
                    return v1beta2DaemonSetList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * DaemonSet V1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1DaemonSet> getDaemonSetsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1DaemonSetList v1DaemonSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1DaemonSetList = apiInstance.listNamespacedDaemonSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1DaemonSetList = apiInstance.listDaemonSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1DaemonSetList != null){
                if(CollectionUtils.isNotEmpty(v1DaemonSetList.getItems())){
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(V1DaemonSet v1DaemonSet : v1DaemonSetList.getItems()) {
                        v1DaemonSet.setKind(K8sApiKindType.DAEMON_SET.getValue());
                        v1DaemonSet.setApiVersion(v1DaemonSetList.getApiVersion());
                    }
                    return v1DaemonSetList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * DaemonSet V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1DaemonSet getDaemonSetV1(ClusterVO cluster, String namespace, String name) throws Exception{
        return this.getDaemonSetV1(cluster, namespace, name, false);
    }

    /**
     * DaemonSet V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1DaemonSet getDaemonSetV1(ClusterVO cluster, String namespace, String name, Boolean getStatus) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1DaemonSet v1DaemonSet;
            if(BooleanUtils.isTrue(getStatus)){
                v1DaemonSet = apiInstance.readNamespacedDaemonSetStatus(name, namespace, null);
            }else{
                v1DaemonSet = apiInstance.readNamespacedDaemonSet(name, namespace, null);
            }

            if(v1DaemonSet != null){
                return v1DaemonSet;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Job V1 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1Job createJobV1(ClusterVO cluster, String namespace, V1Job param, boolean dryRun) throws Exception {
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1Job result = apiInstance.createNamespacedJob(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("New Job content: {}", k8sJson.serialize(param));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'Job': %s", Optional.ofNullable(param).map(V1Job::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Job V1 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1Job replaceJobV1(ClusterVO cluster, String namespace, String name, V1Job param) throws Exception {
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1Job result = apiInstance.replaceNamespacedJob(name, namespace, param, null, null, null, null);
            log.debug("Replace Job content: {}", k8sJson.serialize(param));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'Job': %s", Optional.ofNullable(param).map(V1Job::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Job 삭제
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteJobV1(ClusterVO cluster, String namespace, String name) throws Exception {
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1DeleteOptions v1DeleteOptions = new V1DeleteOptions();
            v1DeleteOptions.setGracePeriodSeconds(0L);
            v1DeleteOptions.setPropagationPolicy("Foreground");
            V1Status v1Status = apiInstance.deleteNamespacedJob(name, namespace, null, null, 0, null, "Foreground", v1DeleteOptions);
            if (v1Status != null) {
                log.debug("### Job V1Status.getStatus() : " + v1Status.getStatus());
                return v1Status;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Job V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Job patchJobV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1Job result = apiInstance.patchNamespacedJob(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("Patch Job content: {}", JsonUtils.toGson(patchBody));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'Job': %s", name), ExceptionType.K8sJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Job V1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Job> getJobsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1JobList v1JobList;

            if(StringUtils.isNotBlank(namespace)){
                v1JobList = apiInstance.listNamespacedJob(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1JobList = apiInstance.listJobForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1JobList != null){
                if(CollectionUtils.isNotEmpty(v1JobList.getItems())){
                    for(V1Job v1Job : v1JobList.getItems()) {
                        v1Job.setKind(K8sApiKindType.JOB.getValue());
                        v1Job.setApiVersion(v1JobList.getApiVersion());
                    }
                    return v1JobList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Job V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Job getJobV1(ClusterVO cluster, String namespace, String name) throws Exception{
        return this.getJobV1(cluster, namespace, name, false);
    }

    /**
     * Job V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Job getJobV1(ClusterVO cluster, String namespace, String name, Boolean getStatus) throws Exception{
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1Job v1Job;
            if(BooleanUtils.isTrue(getStatus)){
                v1Job = apiInstance.readNamespacedJobStatus(name, namespace, null);
            }else{
                v1Job = apiInstance.readNamespacedJob(name, namespace, null);
            }

            if(v1Job != null){
                return v1Job;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1beta1 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1beta1CronJob createCronJobV1beta1(ClusterVO cluster, String namespace, V1beta1CronJob param, boolean dryRun) throws Exception {
        BatchV1beta1Api apiInstance = new BatchV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CronJob result = apiInstance.createNamespacedCronJob(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createCronJobV1beta1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'CronJob': %s", Optional.ofNullable(param).map(V1beta1CronJob::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sCronJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1beta1 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1beta1CronJob replaceCronJobV1beta1(ClusterVO cluster, String namespace, String name, V1beta1CronJob param) throws Exception {
        BatchV1beta1Api apiInstance = new BatchV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CronJob result = apiInstance.replaceNamespacedCronJob(name, namespace, param, null, null, null, null);
            log.debug("replaceCronJobV1beta1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'CronJob': %s", Optional.ofNullable(param).map(V1beta1CronJob::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sCronJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1beta1 삭제
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteCronJobV1beta1(ClusterVO cluster, String namespace, String name) throws Exception {
        BatchV1beta1Api apiInstance = new BatchV1beta1Api(this.makeConnection(cluster));

        try {
            V1DeleteOptions v1DeleteOptions = new V1DeleteOptions();
            v1DeleteOptions.setPropagationPolicy("Foreground");
            V1Status result = apiInstance.deleteNamespacedCronJob(name, namespace, null, null, null, null, "Foreground", v1DeleteOptions);
            if (result != null) {
                log.debug("deleteCronJobV1beta1 result: {}", k8sJson.serialize(result));
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1beta1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1beta1CronJob patchCronJobV1beta1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        BatchV1beta1Api apiInstance = new BatchV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CronJob result = apiInstance.patchNamespacedCronJob(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchCronJobV1beta1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'CronJob': %s", name), ExceptionType.K8sCronJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta1CronJob> getCronJobsV1beta1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        BatchV1beta1Api apiInstance = new BatchV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CronJobList results;

            if(StringUtils.isNotBlank(namespace)){
                results = apiInstance.listNamespacedCronJob(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                results = apiInstance.listCronJobForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(results != null){
                if(CollectionUtils.isNotEmpty(results.getItems())){
                    for(V1beta1CronJob v1beta1CronJob : results.getItems()) {
                        v1beta1CronJob.setKind(K8sApiKindType.CRON_JOB.getValue());
                        v1beta1CronJob.setApiVersion(results.getApiVersion());
                    }
                    return results.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * CronJob V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1beta1CronJob getCronJobV1beta1(ClusterVO cluster, String namespace, String name) throws Exception{
        return this.getCronJobV1beta1(cluster, namespace, name, false);
    }

    /**
     * CronJob V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1beta1CronJob getCronJobV1beta1(ClusterVO cluster, String namespace, String name, Boolean getStatus) throws Exception{
        BatchV1beta1Api apiInstance = new BatchV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CronJob result;
            if(BooleanUtils.isTrue(getStatus)){
                result = apiInstance.readNamespacedCronJobStatus(name, namespace, null);
            }else{
                result = apiInstance.readNamespacedCronJob(name, namespace, null);
            }

            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1CronJob createCronJobV1(ClusterVO cluster, String namespace, V1CronJob param, boolean dryRun) throws Exception {
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1CronJob result = apiInstance.createNamespacedCronJob(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createCronJobV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'CronJob': %s", Optional.ofNullable(param).map(V1CronJob::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sCronJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1CronJob replaceCronJobV1(ClusterVO cluster, String namespace, String name, V1CronJob param) throws Exception {
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1CronJob result = apiInstance.replaceNamespacedCronJob(name, namespace, param, null, null, null, null);
            log.debug("replaceCronJobV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'CronJob': %s", Optional.ofNullable(param).map(V1CronJob::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sCronJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1 삭제
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteCronJobV1(ClusterVO cluster, String namespace, String name) throws Exception {
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1DeleteOptions v1DeleteOptions = new V1DeleteOptions();
            v1DeleteOptions.setPropagationPolicy("Foreground");
            V1Status result = apiInstance.deleteNamespacedCronJob(name, namespace, null, null, null, null, "Foreground", v1DeleteOptions);
            log.debug("deleteCronJobV1 result: {}", k8sJson.serialize(result));
            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1CronJob patchCronJobV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1CronJob result = apiInstance.patchNamespacedCronJob(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchCronJobV1 result: {}", JsonUtils.toGson(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'CronJob': %s", name), ExceptionType.K8sCronJobCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CronJob V1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1CronJob> getCronJobsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1CronJobList results;

            if(StringUtils.isNotBlank(namespace)){
                results = apiInstance.listNamespacedCronJob(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                results = apiInstance.listCronJobForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(results != null){
                if(CollectionUtils.isNotEmpty(results.getItems())){
                    for(V1CronJob v1CronJob : results.getItems()) {
                        v1CronJob.setKind(K8sApiKindType.CRON_JOB.getValue());
                        v1CronJob.setApiVersion(results.getApiVersion());
                    }
                    return results.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * CronJob V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1CronJob getCronJobV1(ClusterVO cluster, String namespace, String name) throws Exception{
        return this.getCronJobV1(cluster, namespace, name, false);
    }

    /**
     * CronJob V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1CronJob getCronJobV1(ClusterVO cluster, String namespace, String name, Boolean getStatus) throws Exception{
        BatchV1Api apiInstance = new BatchV1Api(this.makeConnection(cluster));

        try {
            V1CronJob result;
            if(BooleanUtils.isTrue(getStatus)){
                result = apiInstance.readNamespacedCronJobStatus(name, namespace, null);
            }else{
                result = apiInstance.readNamespacedCronJob(name, namespace, null);
            }

            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ReplidaSet 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteReplicaSetV1(ClusterVO cluster, String namespace, String name) throws Exception {
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));
        try {
            V1Status v1Status = apiInstance.deleteNamespacedReplicaSet(name, namespace, null, null, null, null, null, new V1DeleteOptions());
            log.debug("### deleteNamespacedReplicaSet : V1Status : " + v1Status);
            if (v1Status != null) {
                log.debug("#### V1Status.getStatus() : " + v1Status.getStatus());
                return v1Status;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
        return null;
    }

    /**
     * ReplicaSet V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta1ReplicaSet> getReplicaSetsV1beta1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1ReplicaSetList v1beta1ReplicaSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1beta1ReplicaSetList = apiInstance.listNamespacedReplicaSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null);
            }else{
                v1beta1ReplicaSetList = apiInstance.listReplicaSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, 56, null);
            }

            if(v1beta1ReplicaSetList != null){
                if(CollectionUtils.isNotEmpty(v1beta1ReplicaSetList.getItems())){
                    return v1beta1ReplicaSetList.getItems().stream()
                            .sorted((r1, r2) ->
                                    Optional.ofNullable(r2).map(V1beta1ReplicaSet::getMetadata).map(V1ObjectMeta::getCreationTimestamp).orElseGet(() ->OffsetDateTime.MIN)
                                            .compareTo(Optional.ofNullable(r1).map(V1beta1ReplicaSet::getMetadata).map(V1ObjectMeta::getCreationTimestamp).orElseGet(() ->OffsetDateTime.MIN)))
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * ReplicaSet V1beta2 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta2ReplicaSet> getReplicaSetsV1beta2(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1beta2Api apiInstance = new AppsV1beta2Api(this.makeConnection(cluster));

        try {
            V1beta2ReplicaSetList v1beta2ReplicaSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1beta2ReplicaSetList = apiInstance.listNamespacedReplicaSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null);
            }else{
                v1beta2ReplicaSetList = apiInstance.listReplicaSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, 56, null);
            }

            if(v1beta2ReplicaSetList != null){
                if(CollectionUtils.isNotEmpty(v1beta2ReplicaSetList.getItems())){
                    return v1beta2ReplicaSetList.getItems().stream()
                            .sorted((r1, r2) ->
                                    Optional.ofNullable(r2).map(V1beta2ReplicaSet::getMetadata).map(V1ObjectMeta::getCreationTimestamp).orElseGet(() ->OffsetDateTime.MIN)
                                            .compareTo(Optional.ofNullable(r1).map(V1beta2ReplicaSet::getMetadata).map(V1ObjectMeta::getCreationTimestamp).orElseGet(() ->OffsetDateTime.MIN)))
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * ReplicaSet V1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1ReplicaSet> getReplicaSetsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AppsV1Api apiInstance = new AppsV1Api(this.makeConnection(cluster));

        try {
            V1ReplicaSetList v1ReplicaSetList;

            if(StringUtils.isNotBlank(namespace)){
                v1ReplicaSetList = apiInstance.listNamespacedReplicaSet(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1ReplicaSetList = apiInstance.listReplicaSetForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1ReplicaSetList != null){
                if(CollectionUtils.isNotEmpty(v1ReplicaSetList.getItems())){
                    return v1ReplicaSetList.getItems().stream()
                            .sorted((r1, r2) ->
                                    Optional.ofNullable(r2).map(V1ReplicaSet::getMetadata).map(V1ObjectMeta::getCreationTimestamp).orElseGet(() ->OffsetDateTime.MIN)
                                            .compareTo(Optional.ofNullable(r1).map(V1ReplicaSet::getMetadata).map(V1ObjectMeta::getCreationTimestamp).orElseGet(() ->OffsetDateTime.MIN)))
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * HorizontalPodAutoscaler 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V2HorizontalPodAutoscaler createHorizontalPodAutoscalerV2(ClusterVO cluster, String namespace, V2HorizontalPodAutoscaler param, boolean dryRun) throws Exception {
        AutoscalingV2Api apiInstance = new AutoscalingV2Api(this.makeConnection(cluster));

        try {
            V2HorizontalPodAutoscaler result = apiInstance.createNamespacedHorizontalPodAutoscaler(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createHorizontalPodAutoscalerV2 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'V2HorizontalPodAutoscaler': %s", Optional.ofNullable(param).map(V2HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V2beta2HorizontalPodAutoscaler createHorizontalPodAutoscalerV2beta2(ClusterVO cluster, String namespace, V2beta2HorizontalPodAutoscaler param, boolean dryRun) throws Exception {
        AutoscalingV2beta2Api apiInstance = new AutoscalingV2beta2Api(this.makeConnection(cluster));

        try {
            V2beta2HorizontalPodAutoscaler result = apiInstance.createNamespacedHorizontalPodAutoscaler(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createHorizontalPodAutoscalerV2beta2 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'V2beta2HorizontalPodAutoscaler': %s", Optional.ofNullable(param).map(V2beta2HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V2beta1HorizontalPodAutoscaler createHorizontalPodAutoscalerV2beta1(ClusterVO cluster, String namespace, V2beta1HorizontalPodAutoscaler param, boolean dryRun) throws Exception {
        AutoscalingV2beta1Api apiInstance = new AutoscalingV2beta1Api(this.makeConnection(cluster));

        try {
            V2beta1HorizontalPodAutoscaler result = apiInstance.createNamespacedHorizontalPodAutoscaler(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createHorizontalPodAutoscalerV2beta1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'V2beta1HorizontalPodAutoscaler': %s", Optional.ofNullable(param).map(V2beta1HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1HorizontalPodAutoscaler createHorizontalPodAutoscalerV1(ClusterVO cluster, String namespace, V1HorizontalPodAutoscaler param, boolean dryRun) throws Exception {
        AutoscalingV1Api apiInstance = new AutoscalingV1Api(this.makeConnection(cluster));

        try {
            V1HorizontalPodAutoscaler result = apiInstance.createNamespacedHorizontalPodAutoscaler(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createHorizontalPodAutoscalerV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'HorizontalPodAutoscaler': %s", Optional.ofNullable(param).map(V1HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param hpa
     * @param dryRun
     * @return
     * @throws Exception
     */
    public V2HorizontalPodAutoscaler replaceHorizontalPodAutoscaleV2(ClusterVO cluster, String namespace, String name, V2HorizontalPodAutoscaler hpa, boolean dryRun) throws Exception {
        AutoscalingV2Api apiInstance = new AutoscalingV2Api(this.makeConnection(cluster));

        try {
            V2HorizontalPodAutoscaler result = apiInstance.replaceNamespacedHorizontalPodAutoscaler(name, namespace, hpa, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("replaceHorizontalPodAutoscaleV2 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'V2HorizontalPodAutoscaler': %s", name), ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2beta2 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param hpa
     * @param dryRun
     * @return
     * @throws Exception
     */
    public V2beta2HorizontalPodAutoscaler replaceHorizontalPodAutoscaleV2beta2(ClusterVO cluster, String namespace, String name, V2beta2HorizontalPodAutoscaler hpa, boolean dryRun) throws Exception {
        AutoscalingV2beta2Api apiInstance = new AutoscalingV2beta2Api(this.makeConnection(cluster));

        try {
            V2beta2HorizontalPodAutoscaler result = apiInstance.replaceNamespacedHorizontalPodAutoscaler(name, namespace, hpa, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("replaceHorizontalPodAutoscaleV2beta2 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'V2beta2HorizontalPodAutoscaler': %s", name), ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2beta1 replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param hpa
     * @param dryRun
     * @return
     * @throws Exception
     */
    public V2beta1HorizontalPodAutoscaler replaceHorizontalPodAutoscaleV2beta1(ClusterVO cluster, String namespace, String name, V2beta1HorizontalPodAutoscaler hpa, boolean dryRun) throws Exception {
        AutoscalingV2beta1Api apiInstance = new AutoscalingV2beta1Api(this.makeConnection(cluster));

        try {
            V2beta1HorizontalPodAutoscaler result = apiInstance.replaceNamespacedHorizontalPodAutoscaler(name, namespace, hpa, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("replaceHorizontalPodAutoscaleV2beta1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to replace 'V2beta1HorizontalPodAutoscaler': %s", name), ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V2HorizontalPodAutoscaler patchHorizontalPodAutoscaleV2(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        AutoscalingV2Api apiInstance = new AutoscalingV2Api(this.makeConnection(cluster));

        try {
            V2HorizontalPodAutoscaler result = apiInstance.patchNamespacedHorizontalPodAutoscaler(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchHorizontalPodAutoscaleV2 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'V2HorizontalPodAutoscaler': %s", name), ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2beta2 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V2beta2HorizontalPodAutoscaler patchHorizontalPodAutoscaleV2beta2(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        AutoscalingV2beta2Api apiInstance = new AutoscalingV2beta2Api(this.makeConnection(cluster));

        try {
            V2beta2HorizontalPodAutoscaler result = apiInstance.patchNamespacedHorizontalPodAutoscaler(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchHorizontalPodAutoscaleV2beta2 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'V2beta2HorizontalPodAutoscaler': %s", name), ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2beta1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V2beta1HorizontalPodAutoscaler patchHorizontalPodAutoscaleV2beta1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        AutoscalingV2beta1Api apiInstance = new AutoscalingV2beta1Api(this.makeConnection(cluster));

        try {
            V2beta1HorizontalPodAutoscaler result = apiInstance.patchNamespacedHorizontalPodAutoscaler(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchHorizontalPodAutoscaleV2beta1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'V2beta1HorizontalPodAutoscaler': %s", name), ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1HorizontalPodAutoscaler patchHorizontalPodAutoscaleV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        AutoscalingV1Api apiInstance = new AutoscalingV1Api(this.makeConnection(cluster));

        try {
            V1HorizontalPodAutoscaler result = apiInstance.patchNamespacedHorizontalPodAutoscaler(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchHorizontalPodAutoscaleV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'V1HorizontalPodAutoscaler': %s", name), ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }


    /**
     * HorizontalPodAutoscaler 삭제 with Name
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteHorizontalPodAutoscalerV2WithName(ClusterVO cluster, String namespace, String name) throws Exception {
        AutoscalingV2Api apiInstance = new AutoscalingV2Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedHorizontalPodAutoscaler(name, namespace, null, null, 0, null, null, new V1DeleteOptions());
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler 삭제 with Name
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteHorizontalPodAutoscalerV2beta2WithName(ClusterVO cluster, String namespace, String name) throws Exception {
        AutoscalingV2beta2Api apiInstance = new AutoscalingV2beta2Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedHorizontalPodAutoscaler(name, namespace, null, null, 0, null, null, new V1DeleteOptions());
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler 삭제 with Name
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteHorizontalPodAutoscalerV2beta1WithName(ClusterVO cluster, String namespace, String name) throws Exception {
        AutoscalingV2beta1Api apiInstance = new AutoscalingV2beta1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedHorizontalPodAutoscaler(name, namespace, null, null, 0, null, null, new V1DeleteOptions());
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler 삭제 with Name
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteHorizontalPodAutoscalerV1WithName(ClusterVO cluster, String namespace, String name) throws Exception {

        AutoscalingV1Api apiInstance = new AutoscalingV1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedHorizontalPodAutoscaler(name, namespace, null, null, 0, null, null, new V1DeleteOptions());
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2 조회
     *
     * @param cluster
     * @param namespace
     * @return
     * @throws Exception
     */
    public V2HorizontalPodAutoscaler getHorizontalPodAutoscalerV2(ClusterVO cluster, String namespace, String name) throws Exception{
        AutoscalingV2Api apiInstance = new AutoscalingV2Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedHorizontalPodAutoscalerStatus(name, namespace, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2beta2 조회
     *
     * @param cluster
     * @param namespace
     * @return
     * @throws Exception
     */
    public V2beta2HorizontalPodAutoscaler getHorizontalPodAutoscalerV2beta2(ClusterVO cluster, String namespace, String name) throws Exception{
        AutoscalingV2beta2Api apiInstance = new AutoscalingV2beta2Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedHorizontalPodAutoscalerStatus(name, namespace, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler V2beta1 조회
     *
     * @param cluster
     * @param namespace
     * @return
     * @throws Exception
     */
    public V2beta1HorizontalPodAutoscaler getHorizontalPodAutoscalerV2beta1(ClusterVO cluster, String namespace, String name) throws Exception{
        AutoscalingV2beta1Api apiInstance = new AutoscalingV2beta1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedHorizontalPodAutoscalerStatus(name, namespace, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * HorizontalPodAutoscaler 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1HorizontalPodAutoscaler> getHorizontalPodAutoscalersV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AutoscalingV1Api apiInstance = new AutoscalingV1Api(this.makeConnection(cluster));
        try {
            V1HorizontalPodAutoscalerList v1HorizontalPodAutoscalerList;

            if(StringUtils.isNotBlank(namespace)){
                v1HorizontalPodAutoscalerList = apiInstance.listNamespacedHorizontalPodAutoscaler(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1HorizontalPodAutoscalerList = apiInstance.listHorizontalPodAutoscalerForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, 56, null);
            }

            if(v1HorizontalPodAutoscalerList != null){
                if(CollectionUtils.isNotEmpty(v1HorizontalPodAutoscalerList.getItems())){
                    for(V1HorizontalPodAutoscaler hpaRow : v1HorizontalPodAutoscalerList.getItems()) {
                        hpaRow.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
                        hpaRow.setApiVersion(v1HorizontalPodAutoscalerList.getApiVersion());
                    }
                    return v1HorizontalPodAutoscalerList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * HorizontalPodAutoscaler V2 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V2HorizontalPodAutoscaler> getHorizontalPodAutoscalersV2(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AutoscalingV2Api apiInstance = new AutoscalingV2Api(this.makeConnection(cluster));
        try {
            V2HorizontalPodAutoscalerList results;

            if(StringUtils.isNotBlank(namespace)){
                results = apiInstance.listNamespacedHorizontalPodAutoscaler(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                results = apiInstance.listHorizontalPodAutoscalerForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null,  null, 56, null);
            }

            if(results != null){
                if(CollectionUtils.isNotEmpty(results.getItems())){
                    for(V2HorizontalPodAutoscaler hpaRow : results.getItems()) {
                        hpaRow.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
                        hpaRow.setApiVersion(results.getApiVersion());
                    }
                    return results.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * HorizontalPodAutoscaler V2beta2 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V2beta2HorizontalPodAutoscaler> getHorizontalPodAutoscalersV2beta2(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AutoscalingV2beta2Api apiInstance = new AutoscalingV2beta2Api(this.makeConnection(cluster));
        try {
            V2beta2HorizontalPodAutoscalerList results;

            if(StringUtils.isNotBlank(namespace)){
                results = apiInstance.listNamespacedHorizontalPodAutoscaler(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                results = apiInstance.listHorizontalPodAutoscalerForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null,  null, 56, null);
            }

            if(results != null){
                if(CollectionUtils.isNotEmpty(results.getItems())){
                    for(V2beta2HorizontalPodAutoscaler hpaRow : results.getItems()) {
                        hpaRow.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
                        hpaRow.setApiVersion(results.getApiVersion());
                    }
                    return results.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * HorizontalPodAutoscaler V2beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V2beta1HorizontalPodAutoscaler> getHorizontalPodAutoscalersV2beta1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        AutoscalingV2beta1Api apiInstance = new AutoscalingV2beta1Api(this.makeConnection(cluster));
        try {
            V2beta1HorizontalPodAutoscalerList results;

            if(StringUtils.isNotBlank(namespace)){
                results = apiInstance.listNamespacedHorizontalPodAutoscaler(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                results = apiInstance.listHorizontalPodAutoscalerForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null,  null, 56, null);
            }

            if(results != null){
                if(CollectionUtils.isNotEmpty(results.getItems())){
                    for(V2beta1HorizontalPodAutoscaler hpaRow : results.getItems()) {
                        hpaRow.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
                        hpaRow.setApiVersion(results.getApiVersion());
                    }
                    return results.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * HorizontalPodAutoscaler V1 조회
     *
     * @param cluster
     * @param namespace
     * @return
     * @throws Exception
     */
    public V1HorizontalPodAutoscaler getHorizontalPodAutoscalerV1(ClusterVO cluster, String namespace, String name) throws Exception{
        AutoscalingV1Api apiInstance = new AutoscalingV1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedHorizontalPodAutoscalerStatus(name, namespace, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Node 목록 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Node> getNodesV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1NodeList v1NodeList = apiInstance.listNode(null, null, null, fieldSelector, labelSelector, null, null, null, null, null);

            if(v1NodeList != null){
                if(CollectionUtils.isNotEmpty(v1NodeList.getItems())){
                    return v1NodeList.getItems().stream()
                            .sorted(Comparator.comparing(n -> Optional.ofNullable(n).map(V1Node::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")))
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Node 조회
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1Node getNodeV1(ClusterVO cluster, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Node v1Node = apiInstance.readNode(name, null);
            return Optional.ofNullable(v1Node).orElseGet(() ->null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Node 수정
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Node patchNodeV1(ClusterVO cluster, String name, List<JsonObject> patchBody) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {

            V1Node result = apiInstance.patchNode(name, new V1Patch(JsonUtils.toGson(patchBody)), null, null, null, null, null);
            log.debug("patchNodeV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'Node': %s", name),
                        ExceptionType.NodeUpdateFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Namespace 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Namespace> getNamespacesV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1NamespaceList v1NamespaceList = apiInstance.listNamespace(null, null, null, fieldSelector, labelSelector, null, null, null, null, null);

            if(v1NamespaceList != null){
                if(CollectionUtils.isNotEmpty(v1NamespaceList.getItems())){
                    List<V1Namespace> v1Namespaces = v1NamespaceList.getItems().stream()
                            .sorted(Comparator.comparing(n -> Optional.ofNullable(n).map(V1Namespace::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")))
                            .collect(Collectors.toList());

                    for (V1Namespace v1Namespace : v1Namespaces) {
                        v1Namespace.setApiVersion(K8sApiType.V1.getValue());
                        v1Namespace.setKind(K8sApiKindType.NAMESPACE.getValue());
                    }
                    return v1Namespaces;
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    public V1Namespace getNamespaceV1(ClusterVO cluster, String name) throws Exception{
        return this.getNamespaceV1(cluster, name, false);
    }

    /**
     * Namespace 조회
     *
     * @param cluster
     * @param name
     * @param getStatus
     * @return
     * @throws Exception
     */
    public V1Namespace getNamespaceV1(ClusterVO cluster, String name, boolean getStatus) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Namespace v1Namespace;

            if (getStatus) {
                v1Namespace = apiInstance.readNamespaceStatus(name, null);
            } else {
                v1Namespace = apiInstance.readNamespace(name, null);
            }

            if(v1Namespace != null){
                return v1Namespace;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Namespace 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1Namespace createNamespaceV1(ClusterVO cluster, V1Namespace param) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1Namespace result = apiInstance.createNamespace(param, null, null, null, null);
            log.debug("createNamespaceV1 result: {}", k8sJson.serialize(result));
            if(result != null) {
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Namespace Patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Namespace patchNamespaceV1(ClusterVO cluster, String name, List<JsonObject> patchBody) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1Namespace result = apiInstance.patchNamespace(name, new V1Patch(JsonUtils.toGson(patchBody)), null, null, null, null, null);
            log.debug("patchNamespaceV1 result: {}", k8sJson.serialize(result));
            if(result != null) {
                return result;
            }

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Namespace 삭제
     *
     * @param cluster
     * @param namespace
     * @return
     * @throws Exception
     */
    public V1Status deleteNamespaceV1(ClusterVO cluster, String namespace) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespace(namespace, null, null, null, null, null, new V1DeleteOptions());
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking IngressClass V1 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1IngressClass> getIngressClassNetworkingV1(ClusterVO cluster, String fieldSelector, String labelSelector, Integer limit) throws Exception {
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));
        try {
            V1IngressClassList results = apiInstance.listIngressClass(null, null, null, fieldSelector, labelSelector, limit, null, null, null, null);

            if (results != null) {
                if (CollectionUtils.isNotEmpty(results.getItems())) {
                    List<V1IngressClass> v1IngressClasss = results.getItems().stream()
                            .sorted(Comparator.comparing(n -> Optional.ofNullable(n).map(V1IngressClass::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")))
                            .collect(Collectors.toList());

                    for (V1IngressClass v1IngressClass : v1IngressClasss) {
                        v1IngressClass.setKind(K8sApiKindType.INGRESS_CLASS.getValue());
                        v1IngressClass.setApiVersion(results.getApiVersion());
                    }
                    return v1IngressClasss;
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }


    /**
     * Networking Ingress V1beta1 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public NetworkingV1beta1Ingress createIngressNetworkingV1beat1(ClusterVO cluster, String namespace, NetworkingV1beta1Ingress param, boolean dryRun) throws Exception {
        NetworkingV1beta1Api apiInstance = new NetworkingV1beta1Api(this.makeConnection(cluster));

        try {
            NetworkingV1beta1Ingress result = apiInstance.createNamespacedIngress(namespace, param, null, ResourceUtil.getDryRun(dryRun), null);
            log.debug("createIngressNetworkingV1beat1 result: {}", JsonUtils.toGson(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'Ingress': %s", Optional.ofNullable(param).map(NetworkingV1beta1Ingress::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sIngressCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking Ingress V1beta1 Patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public NetworkingV1beta1Ingress patchIngressNetworkingV1beat1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        NetworkingV1beta1Api apiInstance = new NetworkingV1beta1Api(this.makeConnection(cluster));

        try {
            NetworkingV1beta1Ingress result = apiInstance.patchNamespacedIngress(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("patchIngressNetworkingV1beat1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'Ingress': %s", name), ExceptionType.K8sIngressCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking Ingress V1beta1 Replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public NetworkingV1beta1Ingress replaceIngressNetworkingV1beat1(ClusterVO cluster, String namespace, String name, NetworkingV1beta1Ingress param) throws Exception {
        NetworkingV1beta1Api apiInstance = new NetworkingV1beta1Api(this.makeConnection(cluster));

        try {
            NetworkingV1beta1Ingress result = apiInstance.replaceNamespacedIngress(name, namespace, param, null, null, null);
            log.debug("replaceIngressNetworkingV1beat1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to Replace 'Ingress': %s", name), ExceptionType.K8sIngressCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking Ingress V1beta1 조회 (Cluster 전체)
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<NetworkingV1beta1Ingress> getIngressesNetworkingV1Beta1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        return this.getIngressesNetworkingV1Beta1(cluster, null, fieldSelector, labelSelector);
    }

    /**
     * Networking Ingress V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<NetworkingV1beta1Ingress> getIngressesNetworkingV1Beta1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        NetworkingV1beta1Api apiInstance = new NetworkingV1beta1Api(this.makeConnection(cluster));
        try {
            NetworkingV1beta1IngressList v1beta1IngressList;
            if(StringUtils.isNotBlank(namespace)) {
                v1beta1IngressList = apiInstance.listNamespacedIngress(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }
            else {
                v1beta1IngressList = apiInstance.listIngressForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, null, null);
            }

            if(v1beta1IngressList != null){
                if(CollectionUtils.isNotEmpty(v1beta1IngressList.getItems())){
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(NetworkingV1beta1Ingress v1beta1Ingress : v1beta1IngressList.getItems()) {
                        v1beta1Ingress.setKind(K8sApiKindType.INGRESS.getValue());
                        v1beta1Ingress.setApiVersion(v1beta1IngressList.getApiVersion());
                    }
                    return v1beta1IngressList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return Lists.newArrayList();
    }

    /**
     * Networking Ingress V1beta1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public NetworkingV1beta1Ingress getIngressNetworkingV1beta1(ClusterVO cluster, String namespace, String name) throws Exception{
        NetworkingV1beta1Api apiInstance = new NetworkingV1beta1Api(this.makeConnection(cluster));

        try {

            return apiInstance.readNamespacedIngressStatus(name, namespace, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking Ingress V1beta1 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteIngressNetworkingV1beta1(ClusterVO cluster, String namespace, String name) throws Exception{
        NetworkingV1beta1Api apiInstance = new NetworkingV1beta1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedIngress(name, namespace, null, null, null, null, null, new V1DeleteOptions());
            log.debug("deleteIngressNetworkingV1beta1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * Networking Ingress V1 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1Ingress createIngressNetworkingV1(ClusterVO cluster, String namespace, V1Ingress param, boolean dryRun) throws Exception {
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));

        try {
            V1Ingress result = apiInstance.createNamespacedIngress(namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createIngressNetworkingV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'Ingress': %s", Optional.ofNullable(param).map(V1Ingress::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")), ExceptionType.K8sIngressCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking Ingress V1 Patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Ingress patchIngressNetworkingV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));

        try {
            V1Ingress result = apiInstance.patchNamespacedIngress(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchIngressNetworkingV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to patch 'Ingress': %s", name), ExceptionType.K8sIngressCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking Ingress V1 Replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1Ingress replaceIngressNetworkingV1(ClusterVO cluster, String namespace, String name, V1Ingress param) throws Exception {
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));

        try {
            V1Ingress result = apiInstance.replaceNamespacedIngress(name, namespace, param, null, null, null, null);
            log.debug("replaceIngressNetworkingV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to Replace 'Ingress': %s", name), ExceptionType.K8sIngressCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking Ingress V1 조회 (Cluster 전체)
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Ingress> getIngressesNetworkingV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        return this.getIngressesNetworkingV1(cluster, null, fieldSelector, labelSelector);
    }

    /**
     * Networking Ingress V1 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Ingress> getIngressesNetworkingV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));
        try {
            V1IngressList v1IngressList;
            if(StringUtils.isNotBlank(namespace)) {
                v1IngressList = apiInstance.listNamespacedIngress(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }
            else {
                v1IngressList = apiInstance.listIngressForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, null, null);
            }

            if(v1IngressList != null){
                if(CollectionUtils.isNotEmpty(v1IngressList.getItems())){
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(V1Ingress v1Ingress : v1IngressList.getItems()) {
                        v1Ingress.setKind(K8sApiKindType.INGRESS.getValue());
                        v1Ingress.setApiVersion(v1IngressList.getApiVersion());
                    }
                    return v1IngressList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Networking Ingress V1 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Ingress getIngressNetworkingV1(ClusterVO cluster, String namespace, String name) throws Exception{
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));

        try {

            return apiInstance.readNamespacedIngressStatus(name, namespace, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Networking Ingress V1 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteIngressNetworkingV1(ClusterVO cluster, String namespace, String name) throws Exception{
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedIngress(name, namespace, null, null, null, null, null, new V1DeleteOptions());
            log.debug("deleteIngressNetworkingV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * Secret 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1Secret createSecretV1(ClusterVO cluster, String namespace, V1Secret param, boolean dryRun) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1Secret result = apiInstance.createNamespacedSecret(
                    (
                            StringUtils.isNotBlank(namespace) ? namespace : KubeConstants.DEFAULT_NAMESPACE
                    ),
                    param,
                    null, ResourceUtil.getDryRun(dryRun), null, null);

            log.debug("createSecretV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'Secret': %s", Optional.ofNullable(param).map(V1Secret::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sSecretCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }


    /**
     * Secret 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Secret getSecretV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            return apiInstance.readNamespacedSecret(name, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Secret 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Secret> getSecretsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1SecretList results;

            if(StringUtils.isNotBlank(namespace)){
                results = apiInstance.listNamespacedSecret(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                results = apiInstance.listSecretForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null,  null, null, null);
            }
            if(results != null){
                if(CollectionUtils.isNotEmpty(results.getItems())){
                    List<V1Secret> v1Secrets = results.getItems().stream()
                            .sorted(Comparator.comparing(n -> Optional.ofNullable(n).map(V1Secret::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")))
                            .collect(Collectors.toList());
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(V1Secret v1Secret : v1Secrets) {
                        v1Secret.setKind(K8sApiKindType.SECRET.getValue());
                        v1Secret.setApiVersion(results.getApiVersion());
                    }
                    return v1Secrets;
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Secret patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Secret patchSecretV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Secret result = apiInstance.patchNamespacedSecret(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Secret replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1Secret replaceSecretV1(ClusterVO cluster, String namespace, String name, V1Secret param, boolean dryRun) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Secret result = apiInstance.replaceNamespacedSecret(name, namespace, param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("replaceSecretV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to Replace 'Secret': %s", name), ExceptionType.K8sSecretCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Secret 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteSecretV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedSecret(name, namespace, null, null, 0, null, null, new V1DeleteOptions());
            log.debug("deleteSecretV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * ConfigMap 생성
     *
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public void createConfigMapV1(ClusterVO cluster, String namespace, V1ConfigMap param, boolean dryRun) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1ConfigMap result = apiInstance.createNamespacedConfigMap(
                    (
                            StringUtils.isNotBlank(namespace) ? namespace : KubeConstants.DEFAULT_NAMESPACE
                    ),
                    param,
                    null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createConfigMapV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'ConfigMap': %s", Optional.ofNullable(param).map(V1ConfigMap::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sConfigMapCreationFail);
            }

        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * ConfigMap 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1ConfigMap getConfigMapV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            return apiInstance.readNamespacedConfigMap(name, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ConfigMap 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1ConfigMap> getConfigMapsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1ConfigMapList results;

            if(StringUtils.isNotBlank(namespace)){
                results = apiInstance.listNamespacedConfigMap(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                results = apiInstance.listConfigMapForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null,  null, 56, null);
            }

            if(results != null){
                if(CollectionUtils.isNotEmpty(results.getItems())){
                    List<V1ConfigMap> v1ConfigMaps = results.getItems().stream()
                            .sorted(Comparator.comparing(n -> Optional.ofNullable(n).map(V1ConfigMap::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")))
                            .collect(Collectors.toList());
                    /**
                     * 2019.06.20 : List로 조회시 List 내부의 Item에는 apiVersion과 kind가 누락되게 됨. 하여 채워주는 처리 필요. (gun.kim)
                     */
                    for(V1ConfigMap v1ConfigMap : v1ConfigMaps) {
                        v1ConfigMap.setKind(K8sApiKindType.CONFIG_MAP.getValue());
                        v1ConfigMap.setApiVersion(results.getApiVersion());
                    }
                    return v1ConfigMaps;
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * ConfigMap patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1ConfigMap patchConfigMapV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1ConfigMap result = apiInstance.patchNamespacedConfigMap(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ConfigMap replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1ConfigMap replaceConfigMapV1(ClusterVO cluster, String namespace, String name, V1ConfigMap param) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1ConfigMap result = apiInstance.replaceNamespacedConfigMap(name, namespace, param, null, null, null, null);
            log.debug("replaceConfigMapV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to Replace 'ConfigMap': %s", name), ExceptionType.K8sConfigMapCreationFail);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ConfigMap 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @throws Exception
     */
    public void deleteConfigMapV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedConfigMap(name, namespace, null, null, 0, null, null, new V1DeleteOptions());
            log.debug("deleteConfigMapV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * CustomObject 생성
     *
     * @param cluster
     * @param namespace
     * @param group
     * @param version
     * @param plural
     * @param config
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createCustomObjectV1(ClusterVO cluster, String namespace, String group, String version, String plural, Map<String, Object> config) throws Exception {
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));

        try {
            Object result;
            if (StringUtils.isNotBlank(namespace)) {
                result = apiInstance.createNamespacedCustomObject(group, version, namespace, plural, config, null, null, null);
            } else {
                result = apiInstance.createClusterCustomObject(group, version, plural, config, null, null, null);
            }
            log.debug("createCustomObjectV1 result: {}", k8sJson.serialize(result));

            if(result != null){
                return (Map<String, Object>)result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public Map<String, Object> createCustomObjectV1Call(ClusterVO cluster, String namespace, String group, String version, String plural, Map<String, Object> config) throws Exception {
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));

        try {
            Call call;
            if (StringUtils.isNotBlank(namespace)) {
                call = apiInstance.createNamespacedCustomObjectCall(group, version, namespace, plural, config, null, null, null, null);
            } else {
                call = apiInstance.createClusterCustomObjectCall(group, version, plural, config, null, null, null, null);
            }
            log.debug("createCustomObjectV1Call content: {}", k8sJson.serialize(config));

            return this.callAndExecute(apiInstance.getApiClient(), call);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CustomObject patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param group
     * @param version
     * @param plural
     * @param config
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> patchCustomObjectV1(ClusterVO cluster, String namespace, String name, String group, String version, String plural, Map<String, Object> config) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Object result;
            if (StringUtils.isNotBlank(namespace)) {
                result = apiInstance.patchNamespacedCustomObject(group, version, namespace, plural, name, config, null, null, null);
            } else {
                result = apiInstance.patchClusterCustomObject(group, version, plural, name, config, null, null, null);
            }
            if(result != null){
                return (Map<String, Object>)result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public Map<String, Object> patchCustomObjectV1Call(ClusterVO cluster, String namespace, String name, String group, String version, String plural, Map<String, Object> config) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Call call;
            if (StringUtils.isNotBlank(namespace)) {
                call = apiInstance.patchNamespacedCustomObjectCall(group, version, namespace, plural, name, config, null, null, null, null);
            } else {
                call = apiInstance.patchClusterCustomObjectCall(group, version, plural, name, config, null, null, null, null);
            }

            return this.callAndExecute(apiInstance.getApiClient(), call);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CustomObject replace
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param group
     * @param version
     * @param plural
     * @param config
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> replaceCustomObjectV1(ClusterVO cluster, String namespace, String name, String group, String version, String plural, Map<String, Object> config) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Object result;
            if (StringUtils.isNotBlank(namespace)) {
                result = apiInstance.replaceNamespacedCustomObject(group, version, namespace, plural, name, config, null, null);
            } else {
                result = apiInstance.replaceClusterCustomObject(group, version, plural, name, config, null, null);
            }
            log.debug("replaceCustomObjectV1 content: {}", k8sJson.serialize(config));

            if (result != null) {
                return (Map<String, Object>)result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public Map<String, Object> replaceCustomObjectV1Call(ClusterVO cluster, String namespace, String name, String group, String version, String plural, Map<String, Object> config) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Call call;
            if (StringUtils.isNotBlank(namespace)) {
                call = apiInstance.replaceNamespacedCustomObjectCall(group, version, namespace, plural, name, config, null, null, null);
            } else {
                call = apiInstance.replaceClusterCustomObjectCall(group, version, plural, name, config, null, null, null);
            }
            log.debug("replaceCustomObjectV1Call content: {}", k8sJson.serialize(config));

            return this.callAndExecute(apiInstance.getApiClient(), call);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CustomObject 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCustomObjectV1(ClusterVO cluster, String namespace, String name, String group, String version, String plural) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Object result;
            if (StringUtils.isNotBlank(namespace)) {
                result = apiInstance.getNamespacedCustomObject(group, version, namespace, plural, name);
            } else {
                result = apiInstance.getClusterCustomObject(group, version, plural, name);
            }
            if (result != null) {
                return (Map<String, Object>)result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public Map<String, Object> getCustomObjectV1Call(ClusterVO cluster, String namespace, String name, String group, String version, String plural) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Call call;
            if (StringUtils.isNotBlank(namespace)) {
                call = apiInstance.getNamespacedCustomObjectCall(group, version, namespace, plural, name, null);
            } else {
                call = apiInstance.getClusterCustomObjectCall(group, version, plural, name, null);
            }

            return this.callAndExecute(apiInstance.getApiClient(), call);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * CustomObject 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param labelSelector
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCustomObjectsV1(ClusterVO cluster, String namespace, String group, String version, String plural, String labelSelector) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Object result;
            if (StringUtils.isNotBlank(namespace)) {
                result = apiInstance.listNamespacedCustomObject(group, version, namespace, plural, null, null, null, null, labelSelector, null, null, null,  null, null);
            } else {
                result = apiInstance.listClusterCustomObject(group, version, plural, null, null,  null, null, labelSelector, null, null, null,  null, null);
            }

            if(result != null){
                Map<String, Object> resultMap = (Map<String, Object>)result;
                if(MapUtils.getObject(resultMap, "items", null) != null && CollectionUtils.isNotEmpty((List<Object>)resultMap.get("items"))){
                    return (List<Map<String, Object>>)resultMap.get("items");
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCustomObjectsV1Call(ClusterVO cluster, String namespace, String group, String version, String plural, String labelSelector) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Call call;
            if (StringUtils.isNotBlank(namespace)) {
                call = apiInstance.listNamespacedCustomObjectCall(group, version, namespace, plural, null, null,  null, null, labelSelector, null, null, null,  null, null, null);
            } else {
                call = apiInstance.listClusterCustomObjectCall(group, version, plural, null, null,  null, null, labelSelector, null, null, null,  null, null, null);
            }

            Map<String, Object> result = this.callAndExecute(apiInstance.getApiClient(), call);
            if(result != null && MapUtils.getObject(result, "items", null) != null && CollectionUtils.isNotEmpty((List<Object>)result.get("items"))){
                return (List<Map<String, Object>>)result.get("items");
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * CustomObject 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param group
     * @param version
     * @param plural
     * @return
     * @throws Exception
     */
    public void deleteCustomObjectV1(ClusterVO cluster, String namespace, String name, String group, String version, String plural) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Object result;
            if (StringUtils.isNotBlank(namespace)) {
                result = apiInstance.deleteNamespacedCustomObject(group, version, namespace, plural, name, null, null, null, null, new V1DeleteOptions());
            } else {
                result = apiInstance.deleteClusterCustomObject(group, version, plural, name, null, null, null, null, new V1DeleteOptions());
            }
            log.debug("deleteCustomObjectV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public Map<String, Object> deleteCustomObjectV1Call(ClusterVO cluster, String namespace, String name, String group, String version, String plural) throws Exception{
        CustomObjectsApi apiInstance = new CustomObjectsApi(this.makeConnection(cluster));
        try {
            Call call;
            if (StringUtils.isNotBlank(namespace)) {
                call = apiInstance.deleteNamespacedCustomObjectCall(group, version, namespace, plural, name, null, null, null, null, new V1DeleteOptions(), null);
            } else {
                call = apiInstance.deleteClusterCustomObjectCall(group, version, plural, name, null, null, null, null, new V1DeleteOptions(), null);
            }

            return this.callAndExecute(apiInstance.getApiClient(), call);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    private Map<String, Object> callAndExecute(ApiClient apiClient, Call call) throws Exception {
        // Type returnType = new TypeToken<Map<String, Object>>() {}.getType();
        Type returnType = new TypeToken<String>() {}.getType();
        ApiResponse<String> response = apiClient.execute(call, returnType);

        Map<String, Object> result = null;
        if(response != null && response.getData() != null && !"".equals(response.getData())) {
            result = new ObjectMapper().readValue(response.getData(), new TypeReference<Map<String, Object>>() { });
        }
        return result;
    }

    public List<V1beta1CustomResourceDefinition> getCustomResourceDefinitionV1beta1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        ApiextensionsV1beta1Api apiInstance = new ApiextensionsV1beta1Api(this.makeConnection(cluster));

        try {

            V1beta1CustomResourceDefinitionList v1beta1CustomResourceDefinitionList =
                    apiInstance.listCustomResourceDefinition(null, null, null, fieldSelector, labelSelector, null, null, null, null, null);

            if(v1beta1CustomResourceDefinitionList != null){
                if(CollectionUtils.isNotEmpty(v1beta1CustomResourceDefinitionList.getItems())){
                    for(V1beta1CustomResourceDefinition v1beta1CustomResourceDefinition : v1beta1CustomResourceDefinitionList.getItems()) {
                        v1beta1CustomResourceDefinition.setKind(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION.getValue());
                        v1beta1CustomResourceDefinition.setApiVersion(v1beta1CustomResourceDefinitionList.getApiVersion());
                    }
                    return v1beta1CustomResourceDefinitionList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    public List<V1CustomResourceDefinition> getCustomResourceDefinitionV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        ApiextensionsV1Api apiInstance = new ApiextensionsV1Api(this.makeConnection(cluster));

        try {

            V1CustomResourceDefinitionList v1CustomResourceDefinitionList =
                    apiInstance.listCustomResourceDefinition(null, null, null, fieldSelector, labelSelector, null, null, null, null, null);

            if(v1CustomResourceDefinitionList != null){
                if(CollectionUtils.isNotEmpty(v1CustomResourceDefinitionList.getItems())){
                    for(V1CustomResourceDefinition v1CustomResourceDefinition : v1CustomResourceDefinitionList.getItems()) {
                        v1CustomResourceDefinition.setKind(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION.getValue());
                        v1CustomResourceDefinition.setApiVersion(v1CustomResourceDefinitionList.getApiVersion());
                    }
                    return v1CustomResourceDefinitionList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * V1beta1 Custom Resource Definition을 생성한다.
     * Created by 2021-06-15
     * @param cluster
     * @param body
     * @param fieldManager
     * @return
     * @throws Exception
     */
    public V1beta1CustomResourceDefinition createCustomResourceDefinitionV1beta1(ClusterVO cluster, V1beta1CustomResourceDefinition body, String fieldManager) throws Exception{
        ApiextensionsV1beta1Api apiInstance = new ApiextensionsV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CustomResourceDefinition v1beta1CustomResourceDefinition =
                    apiInstance.createCustomResourceDefinition(body, null, null, fieldManager);

            if(v1beta1CustomResourceDefinition != null) {
                return v1beta1CustomResourceDefinition;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1beta1CustomResourceDefinition();
    }

    /**
     * V1 Custom Resource Definition을 생성한다.
     * Created by 2021-06-15
     * @param cluster
     * @param body
     * @param fieldManager
     * @return
     * @throws Exception
     */
    public V1CustomResourceDefinition createCustomResourceDefinitionV1(ClusterVO cluster, V1CustomResourceDefinition body, String fieldManager) throws Exception{
        ApiextensionsV1Api apiInstance = new ApiextensionsV1Api(this.makeConnection(cluster));

        try {
            V1CustomResourceDefinition v1CustomResourceDefinition =
                    apiInstance.createCustomResourceDefinition(body, null, null, fieldManager, null);

            if(v1CustomResourceDefinition != null) {
                return v1CustomResourceDefinition;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1CustomResourceDefinition();
    }

    /**
     * V1beta1 Custom Resource Definition을 수정한다.
	 * Created by 2021-06-15
	 * @param cluster
     * @param name
     * @param body
     * @param fieldManager
     * @return
     * @throws Exception
     */
    public V1beta1CustomResourceDefinition replaceCustomResourceDefinitionV1beta1(ClusterVO cluster, String name, V1beta1CustomResourceDefinition body, String fieldManager) throws Exception{
        ApiextensionsV1beta1Api apiInstance = new ApiextensionsV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CustomResourceDefinition v1beta1CustomResourceDefinition =
                    apiInstance.replaceCustomResourceDefinition(name, body, null, null, fieldManager);

            if(v1beta1CustomResourceDefinition != null) {
                return v1beta1CustomResourceDefinition;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1beta1CustomResourceDefinition();
    }

    /**
     * V1 Custom Resource Definition을 수정한다.
     * Created by 2021-06-15
     * @param cluster
     * @param name
     * @param body
     * @param fieldManager
     * @return
     * @throws Exception
     */
    public V1CustomResourceDefinition replaceCustomResourceDefinitionV1(ClusterVO cluster, String name, V1CustomResourceDefinition body, String fieldManager) throws Exception{
        ApiextensionsV1Api apiInstance = new ApiextensionsV1Api(this.makeConnection(cluster));

        try {
            V1CustomResourceDefinition v1CustomResourceDefinition =
                    apiInstance.replaceCustomResourceDefinition(name, body, null, null, fieldManager, null);

            if(v1CustomResourceDefinition != null) {
                return v1CustomResourceDefinition;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1CustomResourceDefinition();
    }

    /**
     * V1beta1 Custom Resource Definition을 삭제한다.
     * Created by 2021-06-15
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteCustomResourceDefinitionV1beta1(ClusterVO cluster, String name) throws Exception{
        ApiextensionsV1beta1Api apiInstance = new ApiextensionsV1beta1Api(this.makeConnection(cluster));

        try {
            V1Status v1beta1Status =
                    apiInstance.deleteCustomResourceDefinition(name, null, null, 30, false, null, null);

            if(v1beta1Status != null) {
                return v1beta1Status;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1Status();
    }

    /**
     * V1 Custom Resource Definition을 삭제한다.
     * Created by 2021-06-15
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteCustomResourceDefinitionV1(ClusterVO cluster, String name) throws Exception{
        ApiextensionsV1Api apiInstance = new ApiextensionsV1Api(this.makeConnection(cluster));

        try {
            V1Status v1Status =
                    apiInstance.deleteCustomResourceDefinition(name, null, null, 30, false, null, null);

            if(v1Status != null) {
                return v1Status;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1Status();
    }

    /**
     * V1Beta1 커스텀 리소스 오브젝트 정보를 읽어들인다.
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1beta1CustomResourceDefinition readCustomResourceDefinitionV1beta1(ClusterVO cluster, String name) throws Exception{
        ApiextensionsV1beta1Api apiInstance = new ApiextensionsV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CustomResourceDefinition v1beta1CustomResourceDef =
                    apiInstance.readCustomResourceDefinition(name, null, null, null);

            if(v1beta1CustomResourceDef != null) {
                return v1beta1CustomResourceDef;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1beta1CustomResourceDefinition();
    }

    /**
     * V1 커스텀 리소스 오브젝트 정보를 읽어들인다.
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1CustomResourceDefinition readCustomResourceDefinitionV1(ClusterVO cluster, String name) throws Exception{
        ApiextensionsV1Api apiInstance = new ApiextensionsV1Api(this.makeConnection(cluster));

        try {
            V1CustomResourceDefinition v1CustomResourceDef =
                    apiInstance.readCustomResourceDefinition(name, null);

            if(v1CustomResourceDef != null) {
                return v1CustomResourceDef;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1CustomResourceDefinition();
    }

    /**
     * V1Beta1 커스텀 리소스 오브젝트의 상태 정보를 읽어들인다.
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1beta1CustomResourceDefinition readCustomResourceDefinitionV1beta1Status(ClusterVO cluster, String name) throws Exception{
        ApiextensionsV1beta1Api apiInstance = new ApiextensionsV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1CustomResourceDefinition v1beta1CustomResourceDef =
                    apiInstance.readCustomResourceDefinitionStatus(name, null);

            if(v1beta1CustomResourceDef != null) {
                return v1beta1CustomResourceDef;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1beta1CustomResourceDefinition();
    }

    /**
     * V1 커스텀 리소스 오브젝트의 상태 정보를 읽어들인다.
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1CustomResourceDefinition readCustomResourceDefinitionV1Status(ClusterVO cluster, String name) throws Exception{
        ApiextensionsV1Api apiInstance = new ApiextensionsV1Api(this.makeConnection(cluster));

        try {
            V1CustomResourceDefinition v1CustomResourceDef =
                    apiInstance.readCustomResourceDefinitionStatus(name, null);

            if(v1CustomResourceDef != null) {
                return v1CustomResourceDef;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new V1CustomResourceDefinition();
    }

    public List<V1ServiceAccount> getServiceAccountsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1ServiceAccountList v1ServiceAccountList;

            if(StringUtils.isNotBlank(namespace)){
                v1ServiceAccountList = apiInstance.listNamespacedServiceAccount(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1ServiceAccountList = apiInstance.listServiceAccountForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, null, null);
            }

            if(v1ServiceAccountList != null){
                if(CollectionUtils.isNotEmpty(v1ServiceAccountList.getItems())){
                    for(V1ServiceAccount v1ServiceAccount : v1ServiceAccountList.getItems()) {
                        v1ServiceAccount.setKind(K8sApiKindType.SERVICE_ACCOUNT.getValue());
                        v1ServiceAccount.setApiVersion(v1ServiceAccountList.getApiVersion());
                    }
                    return v1ServiceAccountList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Service Account 조회
     * @param cluster
     * @param namespace
     * @param serviceAccountName
     * @return
     * @throws Exception
     */
    public V1ServiceAccount getServiceAccountV1(ClusterVO cluster, String namespace, String serviceAccountName) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedServiceAccount(serviceAccountName, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Service Account 생성
     * @param cluster
     * @param namespace
     * @param param
     * @return
     * @throws Exception
     */
    public V1ServiceAccount createServiceAccountV1(ClusterVO cluster, String namespace, V1ServiceAccount param) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1ServiceAccount result = apiInstance.createNamespacedServiceAccount(namespace, param, null, null, null, null);
            log.debug("createServiceAccountV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'Service Account': %s", Optional.ofNullable(param).map(V1ServiceAccount::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sServiceAccountCreationFail);
            }
            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ClusterRole 생성
     *
     * @param cluster
     * @param param
     * @param dryRun
     * @return
     * @throws Exception
     */
    public V1ClusterRole createClusterRoleV1(ClusterVO cluster, V1ClusterRole param, boolean dryRun) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1ClusterRole result = apiInstance.createClusterRole(param, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createClusterRoleV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'ClusterRole': %s", Optional.ofNullable(param).map(V1ClusterRole::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sClusterRoleCreationFail);
            }
            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ClusterRole 목록 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1ClusterRole> getClusterRolesV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1ClusterRoleList list = apiInstance.listClusterRole(null, null, null, fieldSelector, labelSelector, null, null, null, null, null);

            if(list != null){
                if(CollectionUtils.isNotEmpty(list.getItems())){
                    for(V1ClusterRole item : list.getItems()) {
                        item.setKind(K8sApiKindType.CLUSTER_ROLE.getValue());
                        item.setApiVersion(list.getApiVersion());
                    }
                    return list.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * ClusterRole 상세 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1ClusterRole getClusterRoleV1(ClusterVO cluster, String name) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readClusterRole(name, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ClusterRole V1 patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1ClusterRole patchClusterRoleV1(ClusterVO cluster, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));
        try {
            V1ClusterRole result = apiInstance.patchClusterRole(name, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ClusterRole V1 삭제
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteClusterRoleV1(ClusterVO cluster, String name) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {

            return apiInstance.deleteClusterRole(name, null, null, 0, null, null, new V1DeleteOptions());
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ClusterRoleBinding 생성
     *
     * @param cluster
     * @param clusterRoleBinding
     * @return
     * @throws Exception
     */
    public V1ClusterRoleBinding createClusterRoleBindingV1(ClusterVO cluster, V1ClusterRoleBinding clusterRoleBinding, boolean dryRun) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1ClusterRoleBinding result = apiInstance.createClusterRoleBinding(clusterRoleBinding, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createClusterRoleBindingV1 result: {}", k8sJson.serialize(result));
            if(result == null) {

                String errMsg = String.format("Fail to create 'ClusterRoleBinding': %s", Optional.ofNullable(clusterRoleBinding).map(V1ClusterRoleBinding::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"null"));
                throw new CocktailException(errMsg, ExceptionType.K8sClusterRoleBindingCreationFail, errMsg);
            }
            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ClusterRoleBinding 목록 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1ClusterRoleBinding> getClusterRoleBindingsV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1ClusterRoleBindingList results = apiInstance.listClusterRoleBinding(null, null, null, fieldSelector, labelSelector, null, null, null, null, null);

            if(results != null){
                if(CollectionUtils.isNotEmpty(results.getItems())){
                    for(V1ClusterRoleBinding resultRow : results.getItems()) {
                        resultRow.setKind(K8sApiKindType.CLUSTER_ROLE_BINDING.getValue());
                        resultRow.setApiVersion(results.getApiVersion());
                    }
                    return results.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * ClusterRoleBinding 상세조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1ClusterRoleBinding getClusterRoleBindingV1(ClusterVO cluster, String name) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {

            return apiInstance.readClusterRoleBinding(name, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ClusterRoleBinding V1 patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1ClusterRoleBinding patchClusterRoleBindingV1(ClusterVO cluster, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));
        try {
            V1ClusterRoleBinding result = apiInstance.patchClusterRoleBinding(name, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ClusterRoleBinding V1 삭제
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteClusterRoleBindingV1(ClusterVO cluster, String name) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1Status result = apiInstance.deleteClusterRoleBinding(name, null, null, 0, null, null, new V1DeleteOptions());
            log.debug("deleteClusterRoleBindingV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * Role 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1Role> getRolesV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1RoleList v1RoleList;

            if(StringUtils.isNotBlank(namespace)){
                v1RoleList = apiInstance.listNamespacedRole(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1RoleList = apiInstance.listRoleForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, null, null);
            }

            if(v1RoleList != null){
                if(CollectionUtils.isNotEmpty(v1RoleList.getItems())){
                    for(V1Role v1Role : v1RoleList.getItems()) {
                        v1Role.setKind(K8sApiKindType.ROLE.getValue());
                        v1Role.setApiVersion(v1RoleList.getApiVersion());
                    }
                    return v1RoleList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Role 상세 조회
     *
     * @param cluster
     * @param namespace
     * @param roleName
     * @return
     * @throws Exception
     */
    public V1Role getRoleV1(ClusterVO cluster, String namespace, String roleName) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedRole(roleName, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Role 생성
     * @param cluster
     * @param namespace
     * @param role
     * @return
     * @throws Exception
     */
    public V1Role createRoleV1(ClusterVO cluster, String namespace, V1Role role, boolean dryRun) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1Role result = apiInstance.createNamespacedRole(namespace, role, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createRoleV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'Role': %s", Optional.ofNullable(role).map(V1Role::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"null")), ExceptionType.K8sRoleCreationFail);
            }
            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Role V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1Role patchRoleV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));
        try {
            V1Role result = apiInstance.patchNamespacedRole(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Role V1 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteRoleV1(ClusterVO cluster, String namespace, String name) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1Status result = apiInstance.deleteNamespacedRole(name, namespace, null, null, 0, null, null, new V1DeleteOptions());
            log.debug("deleteRoleV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * RoleBinding 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1RoleBinding> getRoleBindingsV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1RoleBindingList v1RoleBindingList;

            if(StringUtils.isNotBlank(namespace)){
                v1RoleBindingList = apiInstance.listNamespacedRoleBinding(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                v1RoleBindingList = apiInstance.listRoleBindingForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, null, null);
            }

            if(v1RoleBindingList != null){
                if(CollectionUtils.isNotEmpty(v1RoleBindingList.getItems())){
                    for(V1RoleBinding v1RoleBinding : v1RoleBindingList.getItems()) {
                        v1RoleBinding.setKind(K8sApiKindType.ROLE_BINDING.getValue());
                        v1RoleBinding.setApiVersion(v1RoleBindingList.getApiVersion());
                    }
                    return v1RoleBindingList.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * RoleBinding 조회
     * @param cluster
     * @param namespace
     * @param roleBindingName
     * @return
     * @throws Exception
     */
    public V1RoleBinding getRoleBindingV1(ClusterVO cluster, String namespace, String roleBindingName) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedRoleBinding(roleBindingName, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * RoleBinding 생성
     * @param cluster
     * @param namespace
     * @param roleBinding
     * @return
     * @throws Exception
     */
    public V1RoleBinding createRoleBindingV1(ClusterVO cluster, String namespace, V1RoleBinding roleBinding, boolean dryRun) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1RoleBinding result = apiInstance.createNamespacedRoleBinding(namespace, roleBinding, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createRoleBindingV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'RoleBinding': %s", Optional.ofNullable(roleBinding).map(V1RoleBinding::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"null")), ExceptionType.K8sRoleBindingCreationFail);
            }
            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * RoleBinding V1 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteRoleBindingV1(ClusterVO cluster, String namespace, String name) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));

        try {
            V1Status result = apiInstance.deleteNamespacedRoleBinding(name, namespace, null, null, 0, null, null, new V1DeleteOptions());
            log.debug("deleteRoleBindingV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * RoleBinding V1 patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1RoleBinding patchRoleBindingV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception{
        RbacAuthorizationV1Api apiInstance = new RbacAuthorizationV1Api(this.makeConnection(cluster));
        try {
            V1RoleBinding result = apiInstance.patchNamespacedRoleBinding(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            if(result != null){
                log.debug("patchRoleBindingV1 result : {}", k8sJson.serialize(result));
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }


    /**
     * Extensions PodSecurityPolicy 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public ExtensionsV1beta1PodSecurityPolicy createPodSecurityPolicyExtensionsV1beat1(ClusterVO cluster, ExtensionsV1beta1PodSecurityPolicy param) throws Exception {
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(this.makeConnection(cluster));

        try {
            ExtensionsV1beta1PodSecurityPolicy result = apiInstance.createPodSecurityPolicy(param, null, null, null);
            log.debug("createPodSecurityPolicyExtensionsV1beat1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to create 'PodSecurityPolicy': %s", Optional.ofNullable(param).map(ExtensionsV1beta1PodSecurityPolicy::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->""));
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Extensions PodSecurityPolicy Patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public ExtensionsV1beta1PodSecurityPolicy patchPodSecurityPolicyExtensionsV1beat1(ClusterVO cluster, String name, List<JsonObject> patchBody) throws Exception {
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(this.makeConnection(cluster));

        try {
            ExtensionsV1beta1PodSecurityPolicy result = apiInstance.patchPodSecurityPolicy(name, new V1Patch(JsonUtils.toGson(patchBody)), null, null, null, null);
            log.debug("patchPodSecurityPolicyExtensionsV1beat1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to patch 'PodSecurityPolicy': %s", name);
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Extensions PodSecurityPolicy Replace
     *
     * @param cluster
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public ExtensionsV1beta1PodSecurityPolicy replacePodSecurityPolicyExtensionsV1beat1(ClusterVO cluster, String name, ExtensionsV1beta1PodSecurityPolicy param) throws Exception {
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(this.makeConnection(cluster));

        try {
            ExtensionsV1beta1PodSecurityPolicy result = apiInstance.replacePodSecurityPolicy(name, param, null, null, null);
            log.debug("replacePodSecurityPolicyExtensionsV1beat1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to Replace 'PodSecurityPolicy': %s", name);
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }


    /**
     * Extensions PodSecurityPolicy 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<ExtensionsV1beta1PodSecurityPolicy> getPodSecurityPoliciesExtensionsV1Beta1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(this.makeConnection(cluster));
        try {
            ExtensionsV1beta1PodSecurityPolicyList list = apiInstance.listPodSecurityPolicy( null, null, null, fieldSelector, labelSelector, null, null, null, null);

            if(list != null){
                if(CollectionUtils.isNotEmpty(list.getItems())){
                    for(ExtensionsV1beta1PodSecurityPolicy item : list.getItems()) {
                        item.setKind(K8sApiKindType.POD_SECURITY_POLICY.getValue());
                        item.setApiVersion(list.getApiVersion());
                    }
                    return list.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Extensions PodSecurityPolicy 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public ExtensionsV1beta1PodSecurityPolicy getPodSecurityPolicyExtensionsV1beta1(ClusterVO cluster, String name) throws Exception{
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(this.makeConnection(cluster));

        try {

            return apiInstance.readPodSecurityPolicy(name, null, null, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Extensions PodSecurityPolicy 삭제
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public void deletePodSecurityPolicyExtensionsV1beta1(ClusterVO cluster, String name) throws Exception{
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deletePodSecurityPolicy(name, null, null, null, null, null, new V1DeleteOptions());
            log.debug("deletePodSecurityPolicyExtensionsV1beta1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * Policy PodSecurityPolicy 생성
     *
     * @param cluster
     * @param param
     * @return
     * @throws Exception
     */
    public V1beta1PodSecurityPolicy createPodSecurityPolicyPolicyV1beat1(ClusterVO cluster, V1beta1PodSecurityPolicy param) throws Exception {
        PolicyV1beta1Api apiInstance = new PolicyV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1PodSecurityPolicy result = apiInstance.createPodSecurityPolicy(param, null, null, null, null);
            log.debug("createPodSecurityPolicyPolicyV1beat1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to create 'PodSecurityPolicy': %s", Optional.ofNullable(param).map(V1beta1PodSecurityPolicy::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->""));
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Policy PodSecurityPolicy Patch
     *
     * @param cluster
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1beta1PodSecurityPolicy patchPodSecurityPolicyPolicyV1beat1(ClusterVO cluster, String name, List<JsonObject> patchBody) throws Exception {
        PolicyV1beta1Api apiInstance = new PolicyV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1PodSecurityPolicy result = apiInstance.patchPodSecurityPolicy(name, new V1Patch(JsonUtils.toGson(patchBody)), null, null, null, null, null);
            log.debug("patchPodSecurityPolicyPolicyV1beat1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to patch 'PodSecurityPolicy': %s", name);
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Policy PodSecurityPolicy Replace
     *
     * @param cluster
     * @param name
     * @param param
     * @return
     * @throws Exception
     */
    public V1beta1PodSecurityPolicy replacePodSecurityPolicyPolicyV1beat1(ClusterVO cluster, String name, V1beta1PodSecurityPolicy param) throws Exception {
        PolicyV1beta1Api apiInstance = new PolicyV1beta1Api(this.makeConnection(cluster));

        try {
            V1beta1PodSecurityPolicy result = apiInstance.replacePodSecurityPolicy(name, param, null, null, null, null);
            log.debug("replacePodSecurityPolicyPolicyV1beat1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to Replace 'PodSecurityPolicy': %s", name);
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }


    /**
     * Policy PodSecurityPolicy 조회
     *
     * @param cluster
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1beta1PodSecurityPolicy> getPodSecurityPoliciesPolicyV1Beta1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        PolicyV1beta1Api apiInstance = new PolicyV1beta1Api(this.makeConnection(cluster));
        try {
            V1beta1PodSecurityPolicyList list = apiInstance.listPodSecurityPolicy( null, null, null, fieldSelector, labelSelector, null, null, null, null, null);

            if(list != null){
                if(CollectionUtils.isNotEmpty(list.getItems())){
                    for(V1beta1PodSecurityPolicy item : list.getItems()) {
                        item.setKind(K8sApiKindType.POD_SECURITY_POLICY.getValue());
                        item.setApiVersion(list.getApiVersion());
                    }
                    return list.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * Policy PodSecurityPolicy 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1beta1PodSecurityPolicy getPodSecurityPolicyPolicyV1beta1(ClusterVO cluster, String name) throws Exception{
        PolicyV1beta1Api apiInstance = new PolicyV1beta1Api(this.makeConnection(cluster));

        try {

            return apiInstance.readPodSecurityPolicy(name, null);

        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * Policy PodSecurityPolicy 삭제
     *
     * @param cluster
     * @param name
     * @throws Exception
     */
    public void deletePodSecurityPolicyPolicyV1beta1(ClusterVO cluster, String name) throws Exception{
        PolicyV1beta1Api apiInstance = new PolicyV1beta1Api(this.makeConnection(cluster));
        try {
            V1beta1PodSecurityPolicy result = apiInstance.deletePodSecurityPolicy(name, null, null, null, null, null, new V1DeleteOptions());
            log.debug("deletePodSecurityPolicyPolicyV1beta1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }


    /**
     * LimitRange 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1LimitRange> getLimitRangesV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1LimitRangeList list;

            if(StringUtils.isNotBlank(namespace)){
                list = apiInstance.listNamespacedLimitRange(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                list = apiInstance.listLimitRangeForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, null, null);
            }

            if(list != null){
                if(CollectionUtils.isNotEmpty(list.getItems())){
                    for(V1LimitRange item : list.getItems()) {
                        item.setKind(K8sApiKindType.LIMIT_RANGE.getValue());
                        item.setApiVersion(list.getApiVersion());
                    }
                    return list.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * LimitRange 상세 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1LimitRange getLimitRangeV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedLimitRange(name, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * LimitRange 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Status deleteLimitRangeV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedLimitRange(name, namespace,  null, null, null, null, null, new V1DeleteOptions());
            if(result != null){
                return result;
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * LimitRange 생성
     *
     * @param cluster
     * @param namespace
     * @param limitRange
     * @throws Exception
     */
    public void createLimitRangeV1(ClusterVO cluster, String namespace, V1LimitRange limitRange, boolean dryRun) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1LimitRange result = apiInstance.createNamespacedLimitRange(namespace, limitRange, null, ResourceUtil.getDryRun(dryRun) , null, null);
            log.debug("createLimitRangeV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'LimitRange': %s", Optional.ofNullable(limitRange).map(V1LimitRange::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"null")), ExceptionType.K8sLimitRangeCreationFail);
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * LimitRange Patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1LimitRange patchLimitRangeV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1LimitRange result = apiInstance.patchNamespacedLimitRange(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchLimitRangeV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to patch 'LimitRange': %s", name);
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ResourceQuota 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1ResourceQuota> getResourceQuotasV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1ResourceQuotaList list;

            if(StringUtils.isNotBlank(namespace)){
                list = apiInstance.listNamespacedResourceQuota(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                list = apiInstance.listResourceQuotaForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, null, null);
            }

            if(list != null){
                if(CollectionUtils.isNotEmpty(list.getItems())){
                    for(V1ResourceQuota item : list.getItems()) {
                        item.setKind(K8sApiKindType.RESOURCE_QUOTA.getValue());
                        item.setApiVersion(list.getApiVersion());
                    }
                    return list.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * ResourceQuota 상세 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1ResourceQuota getResourceQuotaV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedResourceQuota(name, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * ResourceQuota 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @throws Exception
     */
    public void deleteResourceQuotaV1(ClusterVO cluster, String namespace, String name) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));
        try {
            V1ResourceQuota result = apiInstance.deleteNamespacedResourceQuota(name, namespace,  null, null, null, null, null, new V1DeleteOptions());
            log.debug("deleteResourceQuotaV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * ResourceQuota 생성
     *
     * @param cluster
     * @param namespace
     * @param resourceQuota
     * @throws Exception
     */
    public void createResourceQuotaV1(ClusterVO cluster, String namespace, V1ResourceQuota resourceQuota, boolean dryRun) throws Exception{
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1ResourceQuota result = apiInstance.createNamespacedResourceQuota(namespace, resourceQuota, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createResourceQuotaV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'ResourceQuota': %s", Optional.ofNullable(resourceQuota).map(V1ResourceQuota::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sResourceQuotaCreationFail);
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * ResourceQuota Patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1ResourceQuota patchResourceQuotaV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        CoreV1Api apiInstance = new CoreV1Api(this.makeConnection(cluster));

        try {
            V1ResourceQuota result = apiInstance.patchNamespacedResourceQuota(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchResourceQuotaV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to patch 'ResourceQuota': %s", name);
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * NetworkPolicy 목록 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1NetworkPolicy> getNetworkPoliciesV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));

            try {
                V1NetworkPolicyList list;

            if(StringUtils.isNotBlank(namespace)){
                list = apiInstance.listNamespacedNetworkPolicy(namespace, null, null, null, fieldSelector, labelSelector, null, null, null, null, null);
            }else{
                list = apiInstance.listNetworkPolicyForAllNamespaces(null, null, fieldSelector, labelSelector, null, null, null, null, null, null);
            }

            if(list != null){
                if(CollectionUtils.isNotEmpty(list.getItems())){
                    for(V1NetworkPolicy item : list.getItems()) {
                        item.setKind(K8sApiKindType.NETWORK_POLICY.getValue());
                        item.setApiVersion(list.getApiVersion());
                    }
                    return list.getItems();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * NetworkPolicy 상세 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1NetworkPolicy getNetworkPolicyV1(ClusterVO cluster, String namespace, String name) throws Exception{
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));

        try {
            return apiInstance.readNamespacedNetworkPolicy(name, namespace, null);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * NetworkPolicy 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @throws Exception
     */
    public void deleteNetworkPolicyV1(ClusterVO cluster, String namespace, String name) throws Exception{
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));
        try {
            V1Status result = apiInstance.deleteNamespacedNetworkPolicy(name, namespace,  null, null, null, null, null, new V1DeleteOptions());
            log.debug("deleteNetworkPolicyV1 result : {}", k8sJson.serialize(result));
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * NetworkPolicy 생성
     *
     * @param cluster
     * @param namespace
     * @param networkPolicy
     * @throws Exception
     */
    public void createNetworkPolicyV1(ClusterVO cluster, String namespace, V1NetworkPolicy networkPolicy, boolean dryRun) throws Exception{
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));

        try {
            V1NetworkPolicy result = apiInstance.createNamespacedNetworkPolicy(namespace, networkPolicy, null, ResourceUtil.getDryRun(dryRun), null, null);
            log.debug("createNetworkPolicyV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                throw new CocktailException(String.format("Fail to create 'NetworkPolicy': %s", Optional.ofNullable(networkPolicy).map(V1NetworkPolicy::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->"")),
                        ExceptionType.K8sNetworkPolicyCreationFail);
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * ResourceQuota Patch
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param patchBody
     * @return
     * @throws Exception
     */
    public V1NetworkPolicy patchNetworkPolicyV1(ClusterVO cluster, String namespace, String name, List<JsonObject> patchBody, boolean dryRun) throws Exception {
        NetworkingV1Api apiInstance = new NetworkingV1Api(this.makeConnection(cluster));

        try {
            V1NetworkPolicy result = apiInstance.patchNamespacedNetworkPolicy(name, namespace, new V1Patch(JsonUtils.toGson(patchBody)), null, ResourceUtil.getDryRun(dryRun), null, null, null);
            log.debug("patchNetworkPolicyV1 result: {}", k8sJson.serialize(result));
            if(result == null) {
                String errMsg = String.format("Fail to patch 'NetworkPolicy': %s", name);
                throw new CocktailException(errMsg, ExceptionType.K8sApiFail, errMsg);
            }

            return result;
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * 클러스터 버전 정보 조회
     *
     * @param cluster
     * @return
     * @throws Exception
     */
    public VersionInfo getVersion(ClusterVO cluster) throws Exception {
        Version version = new Version(this.makeConnection(cluster));
        try {
            return version.getVersion();
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }


    /**
     * @param v1APIResourceList
     * @param kind
     * @return
     * @throws Exception
     */
    public boolean isServerSupportsResource(V1APIResourceList v1APIResourceList, String kind) throws Exception {

        try {
            List<String> kinds = extractGroupKinds(v1APIResourceList.getResources());
            Set<String> serverKinds = new HashSet<>(kinds);

            if (serverKinds.contains(kind)) {
                return true;
            }

        } catch (Exception e) {
            log.info(String.format("server does not support API version %s", kind));
            this.exceptionHandle(e);
        }

        return false;

    }


    /**
     * @param cluster
     * @param groupVersion "ex: group/version"
     * @return
     * @throws Exception
     */
    public boolean isServerSupportsVersion(ClusterVO cluster, String groupVersion) throws Exception {

        ApisApi apiInstance = new ApisApi(this.makeConnection(cluster));
        V1APIGroupList result = apiInstance.getAPIVersions();

        if (result != null) {
            try {
                List<String> versions = extractGroupVersions(result.getGroups());
                Set<String> serverVersions = new HashSet<>(versions);

                if (serverVersions.contains(groupVersion)) {
                    return true;
                }

            } catch (Exception e) {
                log.info(String.format("server does not support API version %s", groupVersion));
                this.exceptionHandle(e);
            }
        }

        return false;

    }

    public List<String> extractGroupVersions(List<V1APIGroup> apiGroups) {
        List<String> groupVersions = new ArrayList<>();
        for (V1APIGroup v1APIGroup : apiGroups) {
            if (v1APIGroup != null && v1APIGroup.getPreferredVersion() != null) {
                groupVersions.add(v1APIGroup.getPreferredVersion().getGroupVersion());
            }
        }
        return groupVersions;
    }

    public List<String> extractGroupKinds(List<V1APIResource> v1APIResources) {
        List<String> groupVersions = new ArrayList<>();
        for (V1APIResource v1APIResource : v1APIResources) {
            groupVersions.add(v1APIResource.getKind());
        }
        return groupVersions;
    }

    private boolean k8sSupported(ClusterVO cluster) throws Exception{
        if(cluster != null && StringUtils.isNotBlank(cluster.getK8sVersion())) {
            List<String> apiVersionList = K8sApiVerType.getAllApiVersions();
            return ResourceUtil.getK8sSupported(cluster.getK8sVersion(), apiVersionList);
        }else{
            throw new CocktailException("k8s version is null.", ExceptionType.InternalError);
        }
    }

    public Map<String, String> convertReasourceMap(Map<String, Quantity> resourceMap) {
        Map<String, String> resultMap = new HashMap<>();
        if(resourceMap != null){
            for(Map.Entry<String, Quantity> re : resourceMap.entrySet()){
                if(re.getValue() != null){
                    resultMap.put(re.getKey(), re.getValue().toSuffixedString());
                }
            }
        }

        return resultMap;
    }

    public long convertQuantityToLong(Quantity quantity){
        long result = 0L;
        if(quantity != null){
//            log.debug("quantity - toString()[{}], getNumber()[{}], toSuffixedString[{}], getNumber().longValue()[{}]", quantity.toString(), quantity.getNumber().scale(), quantity.toSuffixedString(), quantity.getNumber().longValue());
            if (quantity.getFormat() == Quantity.Format.DECIMAL_SI && quantity.getNumber().scale() > 0){
                result = (long)(quantity.getNumber().doubleValue()*(Math.pow(10, quantity.getNumber().scale())));
//                log.debug("result [{}], getNumber().doubleValue()[{}], Math.pow(10, quantity.getNumber().scale())[{}]", result, quantity.getNumber().doubleValue(), Math.pow(10, quantity.getNumber().scale()));
            }else {
                result = quantity.getNumber().longValue();
            }
        }

        return result;
    }

    public long convertQuantityToLongForCpuMilliCore(Quantity quantity){
        long result = 0L;
        if(quantity != null){
//            log.debug("quantity - toString()[{}], getNumber()[{}], toSuffixedString[{}], getNumber().longValue()[{}]", quantity.toString(), quantity.getNumber().scale(), quantity.toSuffixedString(), quantity.getNumber().longValue());
            if (quantity.getFormat() == Quantity.Format.DECIMAL_SI){
                if (quantity.getNumber().scale() > 0) {
                    result = (long)(quantity.getNumber().doubleValue()*(Math.pow(10, quantity.getNumber().scale())));
//                log.debug("result [{}], getNumber().doubleValue()[{}], Math.pow(10, quantity.getNumber().scale())[{}]", result, quantity.getNumber().doubleValue(), Math.pow(10, quantity.getNumber().scale()));
                } else {
                    result = quantity.getNumber().longValue()*1000;
//                    log.debug("result [{}], getNumber().longValue()[{}], quantity.getNumber().longValue()*1000[{}]", result, quantity.getNumber().longValue(), quantity.getNumber().longValue()*1000);
                }
            }else {
                result = quantity.getNumber().longValue();
            }
        }

        return result;
    }

    private void exceptionHandle(Exception e) throws Exception{
        this.exceptionHandle(e, true);
    }

    private void exceptionHandle(Exception e, boolean isThrow) throws Exception{
        CocktailException ce = new CocktailException(e.getMessage(), e, ExceptionType.K8sApiFail);

        if(e instanceof JsonSyntaxException){
            if (e.getCause() instanceof IllegalStateException) {
                IllegalStateException ise = (IllegalStateException) e.getCause();
                if (ise.getMessage() != null && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
                    isThrow = false;
                }
            }
        }else if(e instanceof ApiException){
            if(StringUtils.equalsIgnoreCase("Not Found", e.getMessage()) || ((ApiException) e).getCode() == HttpStatus.NOT_FOUND.value()){
                isThrow = false;
            }
            // ApiException 오류가 Tag mismatch 인 경우 재시도
            else if(StringUtils.equalsIgnoreCase("javax.net.ssl.SSLException: Tag mismatch!", e.getMessage())){
                if (isThrow){
                    throw e;
                }
            }
            // ApiException 오류가 Unsupported record version Unknown 인 경우 재시도
            else if(StringUtils.contains(e.getMessage(), "javax.net.ssl.SSLException: Unsupported record version Unknown")){
                if (isThrow){
                    throw e;
                }
            }
            // ApiException 오류가 timeout인 경우 rootCause가 Socket closed면 재시도
            else if(StringUtils.contains(e.getMessage(), "java.net.SocketTimeoutException: timeout")){
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                String rootCauseMsg = ExceptionMessageUtils.getExceptionName(rootCause);
                if (StringUtils.contains(rootCauseMsg, "java.net.SocketException: Socket closed")){
                    if (isThrow){
                        throw e;
                    }
                }
            }
            // ApiException 오류가 connect timed out 인 경우 재시도
            else if(StringUtils.contains(e.getMessage(), "java.net.SocketTimeoutException: connect timed out")){
                if (isThrow){
                    throw e;
                }
            }
            else{
                ExceptionType exceptionType = ExceptionType.K8sApiFail;
                int responseCode = ((ApiException) e).getCode();
                String additionalMsg = ((ApiException) e).getResponseBody();
                if (responseCode >= 400 && responseCode < 500) {
                    exceptionType = ExceptionType.K8sApiWarning;

                    /**
                     * 참고 : https://developer.mozilla.org/ko/docs/Web/HTTP/Status
                     */
                    /**
                     * 400 Bad Request
                     * 이 응답은 잘못된 문법으로 인하여 서버가 요청을 이해할 수 없음을 의미합니다.
                     */
                    if (responseCode == HttpStatus.BAD_REQUEST.value()){
                        exceptionType = ExceptionType.K8sApiStatus400;
                    }
                    /**
                     * 401 Unauthorized
                     * 비록 HTTP 표준에서는 "미승인(unauthorized)"를 명확히 하고 있지만, 의미상 이 응답은 "비인증(unauthenticated)"을 의미합니다.
                     * 클라이언트는 요청한 응답을 받기 위해서는 반드시 스스로를 인증해야 합니다.
                     */
                    else if (responseCode == HttpStatus.UNAUTHORIZED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus401;
                    }
                    /**
                     * 402 Payment Required
                     * 이 응답 코드는 나중에 사용될 것을 대비해 예약되었습니다. 첫 목표로는 디지털 결제 시스템에 사용하기 위하여 만들어졌지만 지금 사용되고 있지는 않습니다.
                     */
//                    else if (responseCode == HttpStatus.PAYMENT_REQUIRED.value()) {
//
//                    }
                    /**
                     * 403 Forbidden
                     * 클라이언트는 콘텐츠에 접근할 권리를 가지고 있지 않습니다. 예를들어 그들은 미승인이어서 서버는 거절을 위한 적절한 응답을 보냅니다.
                     * 401과 다른 점은 서버가 클라이언트가 누구인지 알고 있습니다.
                     */
                    else if (responseCode == HttpStatus.FORBIDDEN.value()) {
                        exceptionType = ExceptionType.K8sApiStatus403;
                    }
                    /**
                     * 404 Not Found
                     * 서버는 요청받은 리소스를 찾을 수 없습니다. 브라우저에서는 알려지지 않은 URL을 의미합니다.
                     * 이것은 API에서 종점은 적절하지만 리소스 자체는 존재하지 않음을 의미할 수도 있습니다.
                     * 서버들은 인증받지 않은 클라이언트로부터 리소스를 숨기기 위하여 이 응답을 403 대신에 전송할 수도 있습니다.
                     * 이 응답 코드는 웹에서 반복적으로 발생하기 때문에 가장 유명할지도 모릅니다.
                     */
                    else if (responseCode == HttpStatus.NOT_FOUND.value()) {
                        exceptionType = ExceptionType.K8sApiStatus404;
                    }
                    /**
                     * 405 Method Not Allowed
                     * 요청한 메소드는 서버에서 알고 있지만, 제거되었고 사용할 수 없습니다. 예를 들어, 어떤 API에서 리소스를 삭제하는 것을 금지할 수 있습니다.
                     * 필수적인 메소드인 GET과 HEAD는 제거될 수 없으며 이 에러 코드를 리턴할 수 없습니다.
                     */
                    else if (responseCode == HttpStatus.METHOD_NOT_ALLOWED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus405;
                    }
                    /**
                     * 406 Not Acceptable
                     * 이 응답은 서버가 서버 주도 콘텐츠 협상 을 수행한 이후, 사용자 에이전트에서 정해준 규격에 따른 어떠한 콘텐츠도 찾지 않았을 때, 웹서버가 보냅니다.
                     */
                    else if (responseCode == HttpStatus.NOT_ACCEPTABLE.value()) {
                        exceptionType = ExceptionType.K8sApiStatus406;
                    }
                    /**
                     * 407 Proxy Authentication Required
                     * 이것은 401과 비슷하지만 프록시에 의해 완료된 인증이 필요합니다.
                     */
                    else if (responseCode == HttpStatus.PROXY_AUTHENTICATION_REQUIRED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus407;
                    }
                    /**
                     * 408 Request Timeout
                     * 이 응답은 요청을 한지 시간이 오래된 연결에 일부 서버가 전송하며, 어떨 때에는 이전에 클라이언트로부터 어떠한 요청이 없었다고 하더라도 보내지기도 합니다.
                     * 이것은 서버가 사용되지 않는 연결을 끊고 싶어한다는 것을 의미합니다.
                     * 이 응답은 특정 몇몇 브라우저에서 빈번하게 보이는데, Chrome, Firefox 27+, 또는 IE9와 같은 웹서핑 속도를 올리기 위해 HTTP 사전 연결 메카니즘을 사용하는 브라우저들이 해당됩니다.
                     * 또한 일부 서버는 이 메시지를 보내지 않고 연결을 끊어버리기도 합니다.
                     */
                    else if (responseCode == HttpStatus.REQUEST_TIMEOUT.value()) {
                        exceptionType = ExceptionType.K8sApiStatus408;
                    }
                    /**
                     * 409 Conflict
                     * 이 응답은 요청이 현재 서버의 상태와 충돌될 때 보냅니다.
                     */
                    else if (responseCode == HttpStatus.CONFLICT.value()) {
                        exceptionType = ExceptionType.K8sApiStatus409;
                    }
                    /**
                     * 410 Gone
                     * 이 응답은 요청한 콘텐츠가 서버에서 영구적으로 삭제되었으며, 전달해 줄 수 있는 주소 역시 존재하지 않을 때 보냅니다.
                     * 클라이언트가 그들의 캐쉬와 리소스에 대한 링크를 지우기를 기대합니다.
                     * HTTP 기술 사양은 이 상태 코드가 "일시적인, 홍보용 서비스"에 사용되기를 기대합니다.
                     * API는 알려진 리소스가 이 상태 코드와 함께 삭제되었다고 강요해서는 안된다.
                     */
                    else if (responseCode == HttpStatus.GONE.value()) {
                        exceptionType = ExceptionType.K8sApiStatus410;
                    }
                    /**
                     * 411 Length Required
                     * 서버에서 필요로 하는 Content-Length 헤더 필드가 정의되지 않은 요청이 들어왔기 때문에 서버가 요청을 거절합니다.
                     */
                    else if (responseCode == HttpStatus.LENGTH_REQUIRED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus411;
                    }
                    /**
                     * 412 Precondition Failed
                     * 클라이언트의 헤더에 있는 전제조건은 서버의 전제조건에 적절하지 않습니다.
                     */
                    else if (responseCode == HttpStatus.PRECONDITION_FAILED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus412;
                    }
                    /**
                     * 413 Payload Too Large
                     * 요청 엔티티는 서버에서 정의한 한계보다 큽니다; 서버는 연결을 끊거나 혹은 Retry-After 헤더 필드로 돌려보낼 것이다.
                     */
                    else if (responseCode == HttpStatus.PAYLOAD_TOO_LARGE.value()) {
                        exceptionType = ExceptionType.K8sApiStatus413;
                    }
                    /**
                     * 414 URI Too Long
                     * 클라이언트가 요청한 URI는 서버에서 처리하지 않기로 한 길이보다 깁니다.
                     */
                    else if (responseCode == HttpStatus.URI_TOO_LONG.value()) {
                        exceptionType = ExceptionType.K8sApiStatus414;
                    }
                    /**
                     * 415 Unsupported Media Type
                     * 요청한 미디어 포맷은 서버에서 지원하지 않습니다, 서버는 해당 요청을 거절할 것입니다.
                     */
                    else if (responseCode == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
                        exceptionType = ExceptionType.K8sApiStatus415;
                    }
                    /**
                     * 416 Requested Range Not Satisfiable
                     * Range 헤더 필드에 요청한 지정 범위를 만족시킬 수 없습니다; 범위가 타겟 URI 데이터의 크기를 벗어났을 가능성이 있습니다.
                     */
                    else if (responseCode == HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value()) {
                        exceptionType = ExceptionType.K8sApiStatus416;
                    }
                    /**
                     * 417 Expectation Failed
                     * 이 응답 코드는 Expect 요청 헤더 필드로 요청한 예상이 서버에서는 적당하지 않음을 알려줍니다.
                     */
                    else if (responseCode == HttpStatus.EXPECTATION_FAILED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus417;
                    }
                    /**
                     * 418 I'm a teapot
                     * 서버는 커피를 찻 주전자에 끓이는 것을 거절합니다.
                     */
//                    else if (responseCode == HttpStatus.I_AM_A_TEAPOT.value()) {
//
//                    }
                    /**
                     * 421 Misdirected Request
                     * 서버로 유도된 요청은 응답을 생성할 수 없습니다. 이것은 서버에서 요청 URI와 연결된 스킴과 권한을 구성하여 응답을 생성할 수 없을 때 보내집니다.
                     */
//                    else if (responseCode == HttpStatus.DESTINATION_LOCKED.value()) {
//
//                    }
                    /**
                     * 422 Unprocessable Entity (WebDAV)
                     * 요청은 잘 만들어졌지만, 문법 오류로 인하여 따를 수 없습니다.
                     */
                    else if (responseCode == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                        exceptionType = ExceptionType.K8sApiStatus422;
                    }
                    /**
                     * 423 Locked (WebDAV)
                     * 리소스는 접근하는 것이 잠겨있습니다.
                     */
                    else if (responseCode == HttpStatus.LOCKED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus423;
                    }
                    /**
                     * 424 Failed Dependency (WebDAV)
                     * 이전 요청이 실패하였기 때문에 지금의 요청도 실패하였습니다.
                     */
                    else if (responseCode == HttpStatus.FAILED_DEPENDENCY.value()) {
                        exceptionType = ExceptionType.K8sApiStatus424;
                    }
                    /**
                     * 426 Upgrade Required
                     * 서버는 지금의 프로토콜을 사용하여 요청을 처리하는 것을 거절하였지만, 클라이언트가 다른 프로토콜로 업그레이드를 하면 처리를 할지도 모릅니다.
                     * 서버는 Upgrade 헤더와 필요로 하는 프로토콜을 알려주기 위해 426 응답에 보냅니다.
                     */
                    else if (responseCode == HttpStatus.UPGRADE_REQUIRED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus426;
                    }
                    /**
                     * 428 Precondition Required
                     * 오리진 서버는 요청이 조건적이어야 합니다. 클라이언트가 리소스를 GET해서, 수정하고,
                     * 그리고 PUT으로 서버에 돌려놓는 동안 서드파티가 서버의 상태를 수정하여 발생하는 충돌인 '업데이트 상실'을 예방하기 위한 목적입니다.
                     */
                    else if (responseCode == HttpStatus.PRECONDITION_REQUIRED.value()) {
                        exceptionType = ExceptionType.K8sApiStatus428;
                    }
                    /**
                     * 429 Too Many Requests
                     * 사용자가 지정된 시간에 너무 많은 요청을 보냈습니다("rate limiting").
                     */
                    else if (responseCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
                        exceptionType = ExceptionType.K8sApiStatus429;
                    }
                    /**
                     * 431 Request Header Fields Too Large
                     * 요청한 헤더 필드가 너무 크기 때문에 서버는 요청을 처리하지 않을 것입니다. 요청은 크기를 줄인 다음에 다시 전송해야 합니다.
                     */
                    else if (responseCode == HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.value()) {
                        exceptionType = ExceptionType.K8sApiStatus431;
                    }
                    /**
                     * 451 Unavailable For Legal Reasons
                     */
                    else if (responseCode == HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value()) {
                        exceptionType = ExceptionType.K8sApiStatus451;
                    }

                }

                try {
                    JSON k8sJson = new JSON();
                    V1Status status = k8sJson.getGson().fromJson(additionalMsg, V1Status.class);
                    if (status.getDetails() != null) {
                        additionalMsg = k8sJson.serialize(status.getDetails());
                    } else {
                        additionalMsg = k8sJson.serialize(status.getMessage());
                    }

                } catch (JsonSyntaxException e1) {
                    log.debug("K8s apiException response is not V1Status!!");
                }

                // ApiException 오류가 이미지 무결성 관련일 경우 별도 처리
                if(StringUtils.containsAny(additionalMsg
                        , "connaisseur-svc."
                        , "does not match expected digest pattern"
                        , "Could not retrieve valid and unambiguous digest from data received by Cosign"
                        , "non-json signature data from Cosign"
                        , "no matching signatures ailed to verify signature"
                        , "Failed to verify signature of trust data"
                        , "fetching signatures: getting signature manifest"
                        , "No trust data for image"
                        , "Unexpected Cosign exception for image"
                        , "Could not extract any digest from data received by Cosign"
                        , "Unable to find signed digest for image"
                )
                ){
                    exceptionType = ExceptionType.K8sImageContentTrustFail;
                }

                ce = new CocktailException(e.getMessage(), e, exceptionType, additionalMsg, responseCode);
            }
        }

        if(isThrow){
            log.error(ce.getMessage(), ce);
            throw ce;
        }
    }
}
