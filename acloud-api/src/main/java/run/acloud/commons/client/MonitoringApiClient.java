package run.acloud.commons.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.commons.util.HttpClientUtil;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailMonitoringProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 25.
 */
@Slf4j
@Component
public class MonitoringApiClient {

    @Autowired
    private CocktailMonitoringProperties monitoringProperties;


    /**
     * Pod - Cpu Usage
     *
     * @param componentSeq
     * @return
     * @throws Exception
     */
    public Map<String, Object> getPodCpuUsage(Integer componentSeq, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/cpu/components/{component-seq}/pods");
        url = StringUtils.replace(url, "{component-seq}", componentSeq.toString());

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);
        paramMap.put("result", "transition");

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getPodCpuUsage");
    }

    /**
     * Pod - Memory Usage
     *
     * @param componentSeq
     * @return
     * @throws Exception
     */
    public Map<String, Object> getPodMemoryUsage(Integer componentSeq, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/memory/components/{component-seq}/pods");
        url = StringUtils.replace(url, "{component-seq}", componentSeq.toString());

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);
        paramMap.put("result", "transition");

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getPodMemoryUsage");
    }

    /**
     * 클러스터 노드별 자원할당량
     *
     * @param clusterSeq
     * @param duration -
     *                 <pre>
     *                     - 값을 산출하기 위해 합산 등의 작업을 하는 시간 범위
     *                            - type: integer, 분(minute)
     *                            - min/max는 질의에 따라 다르다.
     *                            - min/max는 질의에 따라 다르다.
     *                            - ex: 30m
     *                 </pre>
     * @param current -
     *                 <pre>
     *                     - 질의를 산출하는 시간. 값을 주지 않으면 현재 시간을 사용한다.
     *                            - type: long(unix time, utc)
     *                            - duration, start, end와 함께 사용하지 않는다.
     *                            - 자원 할당량과 같은 시간에 따른 사용량이 아닌 값에 대한 질의에 사용한다.
     *                 </pre>
     * @return
     * @throws Exception
     */
    public Map<String, Object> getNodesInClusterResourceQuota(Integer clusterSeq, String duration, Long current) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/clusters/{cluster-seq}/all-nodes/resource");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());

        Map<String, Object> paramMap = this.setParamByResourceQuota(duration, current);

        url = this.genGetUrlWithParam(url, paramMap, true);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getNodesInClusterResourceQuota");
    }

    /**
     * 클러스터 자원할당량 Resource Quota(Limit/Request)
     *
     * @param clusterSeq
     * @return
     * @throws Exception
     */
    public Map<String, Object> getClusterResourceQuota(Integer clusterSeq) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/quota/clusters/{cluster-seq}");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());


        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("result", "current");

        url = this.genGetUrlWithParam(url, paramMap, true);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getClusterResourceQuota");
    }

    /**
     * Node > Pod - Cpu Usage
     *
     * @param clusterSeq
     * @param nodeName
     * @return
     * @throws Exception
     */
    public Map<String, Object> getPodInNodeCpuUsage(Integer clusterSeq, String nodeName, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/cpu/clusters/{cluster-seq}/nodes/{node-name}/pods");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());
        url = StringUtils.replace(url, "{node-name}", nodeName);

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);
        paramMap.put("result", "transition");

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getPodInNodeCpuUsage");
    }

    /**
     * Node > Pod - Memory Usage
     *
     * @param clusterSeq
     * @param nodeName
     * @return
     * @throws Exception
     */
    public Map<String, Object> getPodInNodeMemoryUsage(Integer clusterSeq, String nodeName, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/memory/clusters/{cluster-seq}/nodes/{node-name}/pods");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());
        url = StringUtils.replace(url, "{node-name}", nodeName);

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);
        paramMap.put("result", "transition");

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getPodInNodeMemoryUsage");
    }

    /**
     * Namespace 자원할당량 Resource Quota(Limit/Request)
     *
     * @param clusterSeq
     * @return
     * @throws Exception
     */
    public Map<String, Object> getNamespaceResourceQuota(Integer clusterSeq) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/quota/clusters/{cluster-seq}/appmaps");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("result", "current");

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getNamespaceResourceQuota");
    }

    /**
     * Namespace - Cpu Usage
     *
     * @param clusterSeq
     * @param span -
     *                 <pre>
     *                     - 현재 시점을 기준으로 몇 분 전까지의 측정값을 가져 올 것인지 지정한다. 이 값을 지정하지 않으면 start, end를 사용해야 한다.
     *                            - type: integer, 분(minute)
     *                            - min: 5(분)
     *                            - max: 60(분)
     *                            - ex: 30m
     *                 </pre>
     * @param start
     * @param end -
     *                 <pre>
     *                     - 값을 읽을 시간 범위를 지정한다. 이 값을 지정하지 않으면 span을 사용해야 한다.
     *                            - type: long(unix time, utc)
     *                 </pre>
     * @param duration -
     *                 <pre>
     *                     - 값을 산출하기 위해 합산 등의 작업을 하는 시간 범위
     *                            - type: integer, 분(minute)
     *                            - min/max는 질의에 따라 다르다.
     *                            - min/max는 질의에 따라 다르다.
     *                            - ex: 30m
     *                 </pre>
     * @param step -
     *                 <pre>
     *                     - 현재 시점을 기준으로 몇 분 전까지의 측정값을 가져 올 것인지 지정한다. 이 값을 지정하지 않으면 start, end를 사용해야 한다.
     *                            - type: integer, 분(minute)
     *                            - min: 5(분)
     *                            - max: 60(분)
     *                            - ex: 30m
     *                 </pre>
     * @return
     * @throws Exception
     */
    public Map<String, Object> getNamespaceCpuUsage(Integer clusterSeq, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/clusters/{cluster-seq}/appmaps/cpu");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getNamespaceCpuUsage");
    }

    /**
     * Namespace - Memory Usage
     *
     * @param clusterSeq
     * @param span -
     *                 <pre>
     *                     - 현재 시점을 기준으로 몇 분 전까지의 측정값을 가져 올 것인지 지정한다. 이 값을 지정하지 않으면 start, end를 사용해야 한다.
     *                            - type: integer, 분(minute)
     *                            - min: 5(분)
     *                            - max: 60(분)
     *                            - ex: 30m
     *                 </pre>
     * @param start
     * @param end -
     *                 <pre>
     *                     - 값을 읽을 시간 범위를 지정한다. 이 값을 지정하지 않으면 span을 사용해야 한다.
     *                            - type: long(unix time, utc)
     *                 </pre>
     * @param duration -
     *                 <pre>
     *                     - 값을 산출하기 위해 합산 등의 작업을 하는 시간 범위
     *                            - type: integer, 분(minute)
     *                            - min/max는 질의에 따라 다르다.
     *                            - min/max는 질의에 따라 다르다.
     *                            - ex: 30m
     *                 </pre>
     * @param step -
     *                 <pre>
     *                     - 현재 시점을 기준으로 몇 분 전까지의 측정값을 가져 올 것인지 지정한다. 이 값을 지정하지 않으면 start, end를 사용해야 한다.
     *                            - type: integer, 분(minute)
     *                            - min: 5(분)
     *                            - max: 60(분)
     *                            - ex: 30m
     *                 </pre>
     * @return
     * @throws Exception
     */
    public Map<String, Object> getNamespaceMemoryUsage(Integer clusterSeq, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/clusters/{cluster-seq}/appmaps/memory");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getNamespaceMemoryUsage");
    }

    /**
     * Namespace > pod - Cpu Usage
     *
     * @param clusterSeq
     * @param namespace
     * @return
     * @throws Exception
     */
    public Map<String, Object> getPodInNamespaceCpuUsage(Integer clusterSeq, String namespace, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/cpu/clusters/{cluster-seq}/namespaces/{namespace-name}/pods");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());
        url = StringUtils.replace(url, "{namespace-name}", namespace);

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);
        paramMap.put("result", "transition");

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getPodInNamespaceCpuUsage");
    }

    /**
     * Namespace > pod - Memory Usage
     *
     * @param clusterSeq
     * @param namespace
     * @return
     * @throws Exception
     */
    public Map<String, Object> getPodInNamespaceMemoryUsage(Integer clusterSeq, String namespace, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/memory/clusters/{cluster-seq}/namespaces/{namespace-name}/pods");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());
        url = StringUtils.replace(url, "{namespace-name}", namespace);

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);
        paramMap.put("result", "transition");

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getPodInNamespaceMemoryUsage");
    }


    /**
     * Cluster > node - Cpu, Memory Usage
     *
     * @param clusterSeq
     * @param span -
     *                 <pre>
     *                     - 현재 시점을 기준으로 몇 분 전까지의 측정값을 가져 올 것인지 지정한다. 이 값을 지정하지 않으면 start, end를 사용해야 한다.
     *                            - type: integer, 분(minute)
     *                            - min: 5(분)
     *                            - max: 60(분)
     *                            - ex: 30m
     *                 </pre>
     * @param start
     * @param end -
     *                 <pre>
     *                     - 값을 읽을 시간 범위를 지정한다. 이 값을 지정하지 않으면 span을 사용해야 한다.
     *                            - type: long(unix time, utc)
     *                 </pre>
     * @param duration -
     *                 <pre>
     *                     - 값을 산출하기 위해 합산 등의 작업을 하는 시간 범위
     *                            - type: integer, 분(minute)
     *                            - min/max는 질의에 따라 다르다.
     *                            - min/max는 질의에 따라 다르다.
     *                            - ex: 30m
     *                 </pre>
     * @param step -
     *                 <pre>
     *                     - 현재 시점을 기준으로 몇 분 전까지의 측정값을 가져 올 것인지 지정한다. 이 값을 지정하지 않으면 start, end를 사용해야 한다.
     *                            - type: integer, 분(minute)
     *                            - min: 5(분)
     *                            - max: 60(분)
     *                            - ex: 30m
     *                 </pre>
     * @return
     * @throws Exception
     */
    public Map<String, Object> getNodeInClusterUsage(Integer clusterSeq, String span, Long start, Long end, String duration, Integer step) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/usage/clusters/{cluster-seq}/nodes");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());

        Map<String, Object> paramMap = this.setParamByUsage(span, start, end, duration, step);
        paramMap.put("result", "current");

        url = this.genGetUrlWithParam(url, paramMap, false);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getNodeInClusterUsage");
    }

    /**
     * alarm 정보 조회
     *
     * @param clusterSeq
     * @return
     * @throws Exception
     */
    public Map<String, Object> getAlarmsByCluster(Integer clusterSeq) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/alert/clusters/{cluster-seq}");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getAlarmsByCluster");
    }

    /**
     * cluster capacity 조회
     *
     * @param clusterSeq
     * @return
     * @throws Exception
     */
    public Map<String, Object> getCapacityOfCluster(Integer clusterSeq) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v1/metric/capacity/clusters/{cluster-seq}");
        url = StringUtils.replace(url, "{cluster-seq}", clusterSeq.toString());

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("result", "current");

        url = this.genGetUrlWithParam(url, paramMap, true);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getCapacityOfCluster");
    }

    /**
     * cluster capacity 조회
     * monitoring server 에서 api 수정됨에 따라 cluster로 조회하는 부분 새로 생성 
     *
     * @param clusterId
     * @return
     * @throws Exception
     */
    public Map<String, Object> getCapacityOfClusterByClusterId(String clusterId) throws Exception {
        String url = String.format("%s%s", StringUtils.removeEnd(monitoringProperties.getMonitoringHost(), "/"), "/v2/target/clusters/metric/capacity/result/current");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("cluster-ids", clusterId);

        url = this.genGetUrlWithParam(url, paramMap, true);

        String resp = HttpClientUtil.doGet(url);

        return this.convertResponseToMap(resp, "getCapacityOfClusterByClusterId");
    }

    private Map<String, Object> convertResponseToMap(String response, String callMethodName) throws Exception{
        Map<String, Object> result = ObjectMapperUtils.getMapper().readValue(response, new TypeReference<Map<String, Object>>(){});

        String status = result.get("status") != null ? result.get("status").toString() : "";
        String code = result.get("code") != null ? result.get("code").toString() : "";
        if (StringUtils.isBlank(status) || status.compareToIgnoreCase("ok") != 0) {
            throw new CocktailException(String.format("fail to call Monitoring API - [%s] : [status : %s][code : %s]", callMethodName, status, code), ExceptionType.ExternalApiFail_Monitoring);
        }

        return result;
    }

    private String genGetUrlWithParam(String url, Map<String, Object> paramMap, boolean isOptionalParam) throws Exception{
        if(!isOptionalParam && MapUtils.isEmpty(paramMap)){
            throw new CocktailException("Invalid parameter!!", ExceptionType.ExternalApiFail_Monitoring);
        }else{
            StringBuffer paramStr = new StringBuffer();
            for(Map.Entry entryRow : paramMap.entrySet()){
                paramStr.append(String.format("%s=%s&", entryRow.getKey(), entryRow.getValue()));
            }
            return String.format("%s?%s", url, StringUtils.substringBeforeLast(paramStr.toString(), "&"));
        }
    }

    private Map<String, Object> setParamByResourceQuota(String duration, Long current) throws Exception{
        Map<String, Object> paramMap = new HashMap<>();
        if(StringUtils.isNotBlank(duration)){
            paramMap.put("duration", duration);
        }
        if(current != null){
            paramMap.put("current", current.longValue());
        }

        return paramMap;
    }

    private Map<String, Object> setParamByUsage(String span, Long start, Long end, String duration, Integer step) throws Exception{
        Map<String, Object> paramMap = new HashMap<>();
        if(StringUtils.isNotBlank(span)){
            paramMap.put("span", span);
        }else if(start != null && end != null){
            paramMap.put("start", start.longValue());
            paramMap.put("end", end.longValue());
        }else{
            throw new CocktailException("Invalid parameter!!([span] or [start, end])", ExceptionType.ExternalApiFail_Monitoring);
        }
        if(StringUtils.isNotBlank(duration)){
            paramMap.put("duration", duration);
        }
        if(step != null){
            paramMap.put("step", step);
        }

        return paramMap;
    }
}
