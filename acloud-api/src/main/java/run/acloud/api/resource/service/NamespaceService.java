package run.acloud.api.resource.service;

import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1Namespace;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.SystemNamespaces;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.vo.K8sDeployYamlVO;
import run.acloud.api.resource.vo.K8sNamespaceDetailVO;
import run.acloud.api.resource.vo.K8sNamespaceVO;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;

@Slf4j
@Service
public class NamespaceService {
    private static final Integer MAX_TAIL_COUNT = 10000;
//    private static final String COCKTAIL_REGISTRY_SECRET = "cocktail-registry-secret";

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private ServicemapService servicemapService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    @Autowired
    private K8sResourceService k8sResourceService;


    /**
     * K8S Namespace 정보 조회
     *
     * @param clusterSeq
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public Map<String, K8sNamespaceVO> getNamespacesToMap(Integer clusterSeq, String field, String label, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.convertNamespaceDataMap(cluster, field, label, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getNamespacesToMap fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Namespace 정보 조회 후,
     *
     * V1Namespace -> K8sNamespaceVO 변환
     *
     * @param cluster
     * @param context
     * @return
     * @throws Exception
     */
    public Map<String, K8sNamespaceVO> convertNamespaceDataMap(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception{
        return this.convertNamespaceDataMap(cluster, null, field, label, context);
    }
    public Map<String, K8sNamespaceVO> convertNamespaceDataMap(ClusterVO cluster, Integer serviceSeq, String field, String label, ExecutingContextVO context) throws Exception{

        Map<String, K8sNamespaceVO> namespaceMap = new HashMap<>();

        try {
            if(cluster != null){
                this.genNamespaceData(null, namespaceMap, cluster, serviceSeq, field, label, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("convertNamespaceDataMap fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return namespaceMap;
    }

    /**
     * K8S Namespace 목록 조회
     * @param clusterSeq
     * @param field
     * @param label
     * @param includeSystem
     * @param includeManaged
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sNamespaceVO> getNamespaces(Integer clusterSeq, String field, String label, boolean includeSystem, boolean includeManaged, ExecutingContextVO context) throws Exception{
        return this.getNamespaces(clusterSeq, field, label, includeSystem, includeManaged, false, context);
    }
    /**
     * K8S Namespace 목록 조회
     * @param clusterSeq
     * @param field
     * @param label
     * @param includeSystem
     * @param includeManaged
     * @param useOnlyName
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sNamespaceVO> getNamespaces(Integer clusterSeq, String field, String label, boolean includeSystem, boolean includeManaged, boolean useOnlyName, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.genNamespacesToList(cluster, field, label, includeSystem, includeManaged, useOnlyName, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getNamespacesToList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Namespace 정보 조회 후,
     * V1Namespace -> K8sNamespaceVO 변환
     * @param cluster
     * @param field
     * @param label
     * @param includeSystem
     * @param includeManaged
     * @param context
     * @return
     * @throws Exception
     */
    private List<K8sNamespaceVO> genNamespacesToList(ClusterVO cluster, String field, String label, boolean includeSystem, boolean includeManaged, boolean useOnlyName, ExecutingContextVO context) throws Exception {

        List<K8sNamespaceVO> namespaces = new ArrayList<>();

        if(cluster != null){
            // Namespace 조회
            List<V1Namespace> v1Namespaces = k8sWorker.getNamespacesV1(cluster, field, label);

            if (CollectionUtils.isNotEmpty(v1Namespaces)) {
                // Acloud에서 관리되고 있는 Namespace 목록 조회.
                Map<String, ServicemapSummaryVO> servicemapInfoMap = servicemapService.getServicemapSummaryMap(cluster.getClusterSeq());

                K8sNamespaceVO namespace;

                for (V1Namespace v1NamespaceRow : v1Namespaces) {
                    namespace = new K8sNamespaceVO();
                    JSON k8sJson = new JSON();

                    // Namespace 기본정보
                    namespace.setName(v1NamespaceRow.getMetadata().getName());
                    namespace.setLabels(v1NamespaceRow.getMetadata().getLabels());
                    namespace.setAnnotations(v1NamespaceRow.getMetadata().getAnnotations());
                    namespace.setStatus(v1NamespaceRow.getStatus().getPhase());
                    namespace.setCreationTimestamp(v1NamespaceRow.getMetadata().getCreationTimestamp());
                    namespace.setClusterSeq(cluster.getClusterSeq());
                    namespace.setClusterName(cluster.getClusterName());
                    if(field != null) { // 필드 셀렉터로 조회 = 상세 조회 일때만 배포 정보를 포함함..
                        namespace.setDeployment(k8sJson.serialize(v1NamespaceRow));
                        namespace.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1NamespaceRow));
                    }

                    // namespace에 해당하는 서비스 연결 정보를 찾아서 셋팅
                    if (MapUtils.isNotEmpty(servicemapInfoMap) && servicemapInfoMap.containsKey(namespace.getName())) {
                        namespace.setServicemapInfo(servicemapInfoMap.get(namespace.getName()));
                    }

                    // Namespace 상세
                    K8sNamespaceDetailVO namespaceDetail = new K8sNamespaceDetailVO();
                    namespaceDetail.setName(v1NamespaceRow.getMetadata().getName());
                    namespaceDetail.setLabels(v1NamespaceRow.getMetadata().getLabels());
                    namespaceDetail.setAnnotations(v1NamespaceRow.getMetadata().getAnnotations());
                    namespaceDetail.setCreationTime(v1NamespaceRow.getMetadata().getCreationTimestamp());
                    namespaceDetail.setStatus(v1NamespaceRow.getStatus().getPhase());
                    namespace.setDetail(namespaceDetail);

                    // 조회 조건 확인 (System Namespace를 포함할 것인지)
                    boolean isAdd = true;
                    if(SystemNamespaces.getSystemNamespaces().contains(namespace.getName()) && !includeSystem) {
                        isAdd = false;
                    }

                    if(isAdd && namespaces != null) {
                        if(useOnlyName) { // System Namespace 체크등 namespace 객체를 이용하는 정보가 있으므로 신규 객체 생성하여 사용함..
                            K8sNamespaceVO returnNamespace = new K8sNamespaceVO();
                            returnNamespace.setName(namespace.getName());
                            namespaces.add(returnNamespace);
                        }
                        else {
                            namespaces.add(namespace);
                        }
                    }
                }
            }
        }
        else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        return namespaces;
    }

    /**
     * K8S Namespace 정보 목록 조회
     *
     * @param clusterSeq
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sNamespaceVO> getNamespacesToList(Integer clusterSeq, String field, String label, ExecutingContextVO context) throws Exception{
        return this.getNamespacesToList(clusterSeq, null, field, label, context);
    }

    public List<K8sNamespaceVO> getNamespacesToList(Integer clusterSeq, Integer serviceSeq, String field, String label, ExecutingContextVO context) throws Exception{

        if(clusterSeq != null){
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

            ClusterVO cluster = clusterDao.getCluster(clusterSeq);

            if(cluster != null){
                return this.getNamespacesToList(cluster, serviceSeq, field, label, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        }else{
            throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
        }
    }

    public List<K8sNamespaceVO> getNamespacesToList(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception{
        return this.getNamespacesToList(cluster, null, field, label, context);
    }

    public List<K8sNamespaceVO> getNamespacesToList(ClusterVO cluster, Integer serviceSeq, String field, String label, ExecutingContextVO context) throws Exception{
        try {
            return this.convertNamespaceDataList(cluster, serviceSeq, field, label, context);
        } catch (Exception e) {
            throw new CocktailException("getNamespacesToList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * V1Namespace 정보 조회
     *
     * @param cluster
     * @param name
     * @return
     * @throws Exception
     */
    public V1Namespace getV1Namespace(ClusterVO cluster, String name) throws Exception{
        try {
            return k8sWorker.getNamespaceV1(cluster, name);
        } catch (Exception e) {
            throw new CocktailException("getNamespace fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Namespace 정보 목록 조회
     *
     * @param cluster
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sNamespaceVO getNamespace(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception{

        try {

            String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);

            List<K8sNamespaceVO> results = this.convertNamespaceDataList(cluster, null, field, null, context);

            if(CollectionUtils.isNotEmpty(results)){
                return results.get(0);
            }else{
                return null;
            }

        } catch (Exception e) {
            throw new CocktailException("getNamespace fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Namespace 정보 조회 후,
     *
     * V1Namespace -> K8sNamespaceVO 변환
     *
     * @param cluster
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sNamespaceVO> convertNamespaceDataList(ClusterVO cluster, Integer serviceSeq, String field, String label, ExecutingContextVO context) throws Exception{

        List<K8sNamespaceVO> namespaces = new ArrayList<>();

        try {
            if(cluster != null){
                this.genNamespaceData(namespaces, null, cluster, serviceSeq, field, label, context);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("convertNamespaceDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return namespaces;
    }

    /**
     * K8S Namespace 정보 조회 후,
     *
     * V1Namespace -> K8sNamespaceVO 변환
     *
     * @param namespaces
     * @param namespaceMap
     * @param cluster
     * @param field
     * @param label
     * @param context
     * @throws Exception
     */
    private void genNamespaceData(List<K8sNamespaceVO> namespaces, Map<String, K8sNamespaceVO> namespaceMap, ClusterVO cluster, Integer serviceSeq, String field, String label, ExecutingContextVO context) throws Exception {

        if(cluster != null){

            // Namespace 조회
            List<V1Namespace> v1Namespaces = k8sWorker.getNamespacesV1(cluster, field, label);

            if (CollectionUtils.isNotEmpty(v1Namespaces)) {

                JSON k8sJson = new JSON();

                K8sNamespaceVO namespace;

                Set<String> appmapNamespaces = k8sResourceService.getServicemapNamespacesByService(serviceSeq);

                for (V1Namespace v1NamespaceRow : v1Namespaces) {

                    if (serviceSeq != null) {
                        if ( !(CollectionUtils.isNotEmpty(appmapNamespaces) && appmapNamespaces.contains(v1NamespaceRow.getMetadata().getName())) ) {
                            continue;
                        }
                    }

                    namespace = new K8sNamespaceVO();

                    // Namespace 목록
                    namespace.setName(v1NamespaceRow.getMetadata().getName());
                    namespace.setLabels(v1NamespaceRow.getMetadata().getLabels());
                    namespace.setAnnotations(v1NamespaceRow.getMetadata().getAnnotations());
                    namespace.setStatus(v1NamespaceRow.getStatus().getPhase());
                    namespace.setCreationTimestamp(v1NamespaceRow.getMetadata().getCreationTimestamp());
                    namespace.setDeployment(k8sJson.serialize(v1NamespaceRow));
                    namespace.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1NamespaceRow));

                    K8sNamespaceDetailVO namespaceDetail = new K8sNamespaceDetailVO();
                    namespaceDetail.setName(v1NamespaceRow.getMetadata().getName());
                    namespaceDetail.setLabels(v1NamespaceRow.getMetadata().getLabels());
                    namespaceDetail.setAnnotations(v1NamespaceRow.getMetadata().getAnnotations());
                    namespaceDetail.setCreationTime(v1NamespaceRow.getMetadata().getCreationTimestamp());
                    namespaceDetail.setStatus(v1NamespaceRow.getStatus().getPhase());
                    namespace.setDetail(namespaceDetail);

                    if(namespaceMap != null){
                        namespaceMap.put(v1NamespaceRow.getMetadata().getName(), namespace);
                    }
                    if(namespaces != null){
                        namespaces.add(namespace);
                    }
                }
            }
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    /**
     * K8S Namespace 생성
     *
     * @param cluster
     * @param namespace
     * @param context
     * @return
     * @throws Exception
     */
    public K8sNamespaceVO createNamespace(ClusterVO cluster, String namespace, ExecutingContextVO context) throws Exception {

        return this.createNamespace(cluster, namespace, null, null, context);
    }

    public K8sNamespaceVO createNamespace(ClusterVO cluster, String namespace, Map<String, String> labels, Map<String, String> annotations, ExecutingContextVO context) throws Exception {

        K8sNamespaceVO k8sNamespace = null;

        // Namespace 생성
        V1Namespace v1Namespace = K8sSpecFactory.buildNamespaceV1(namespace, labels, annotations);

        v1Namespace = k8sWorker.createNamespaceV1(cluster, v1Namespace);
        Thread.sleep(100);

        String fieldSelector = String.format("metadata.name=%s", namespace);
        List<K8sNamespaceVO> namespaces = this.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, context);
        if(CollectionUtils.isNotEmpty(namespaces)){
            k8sNamespace = namespaces.get(0);
        }

        return k8sNamespace;
    }

    public K8sNamespaceVO patchNamespace(Integer clusterSeq, String name, Map<String, String> labels, Map<String, String> annotations, ExecutingContextVO context) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.patchNamespace(cluster, name, labels, annotations, context);
    }

    /**
     * K8S Namespace Patch
     *
     * @param cluster
     * @param name
     * @param labels
     * @param annotations
     * @param context
     * @return
     * @throws Exception
     */
    public K8sNamespaceVO patchNamespace(ClusterVO cluster, String name, Map<String, String> labels, Map<String, String> annotations, ExecutingContextVO context) throws Exception {

        K8sNamespaceVO k8sNamespace = null;

        V1Namespace currentNamespace = k8sWorker.getNamespaceV1(cluster, name);
        currentNamespace.setStatus(null);
        // Patch Namespace
        V1Namespace updatedNamespace = K8sSpecFactory.buildNamespaceV1(name, labels, annotations);;

        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentNamespace, updatedNamespace);

        updatedNamespace = k8sWorker.patchNamespaceV1(cluster, name, patchBody);
        Thread.sleep(100);

        String fieldSelector = String.format("metadata.name=%s", name);
        List<K8sNamespaceVO> namespaces = this.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, context);
        if(CollectionUtils.isNotEmpty(namespaces)){
            k8sNamespace = namespaces.get(0);
        }

        return k8sNamespace;
    }

    public void patchNamespaceByYaml(ClusterVO cluster, K8sDeployYamlVO deployYaml) throws Exception {

        if (deployYaml != null) {
            List<Object> objs = ServerUtils.getYamlObjects(deployYaml.getYaml());
            if (CollectionUtils.isNotEmpty(objs) && objs.size() == 1) {
                K8sApiKindType k8sApiKindType = ServerUtils.getK8sKindInObject(objs.get(0), new JSON());
                if (k8sApiKindType == K8sApiKindType.NAMESPACE) {
                    // update namespace
                    V1Namespace updateNamespace = (V1Namespace) objs.get(0);

                    // check yaml name
                    if (StringUtils.equals(deployYaml.getName(), updateNamespace.getMetadata().getName())) {
                        V1Namespace currentNamespace = k8sWorker.getNamespaceV1(cluster, updateNamespace.getMetadata().getName());

                        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentNamespace, updateNamespace);
                        // patch
                        k8sWorker.patchNamespaceV1(cluster, updateNamespace.getMetadata().getName(), patchBody);
                    } else {
                        throw new CocktailException("name parameter is invalid.", ExceptionType.InvalidYamlData);
                    }
                } else {
                    throw new CocktailException("Yaml is invalid.(it is not Namespace).", ExceptionType.InvalidYamlData);
                }
            } else {
                throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
            }
        } else {
            throw new CocktailException("Namespace parameter is null.", ExceptionType.InvalidParameter);
        }
    }

    /**
     * K8S Ingress 삭제
     *
     * @param cluster
     * @param namespace
     * @param context
     * @return
     * @throws Exception
     */
    public void deleteNamespace(ClusterVO cluster, String namespace, ExecutingContextVO context) throws Exception {

        String fieldSelector = String.format("metadata.name=%s", namespace);
        List<K8sNamespaceVO> namespaces = this.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, context);
        if(CollectionUtils.isNotEmpty(namespaces)){
            k8sWorker.deleteNamespaceV1(cluster, namespace);
            Thread.sleep(100);
        }

    }

    public ClusterVO setupCluster(Integer clusterSeq, String namespaceName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        return cluster;
    }

}
