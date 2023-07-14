package run.acloud.api.cserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.BaseExponent;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.QuantityFormatter;
import io.kubernetes.client.custom.SuffixFormatter;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.*;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.cserver.vo.ServerYamlVO;
import run.acloud.api.k8sextended.models.V1beta1CronJob;
import run.acloud.api.k8sextended.models.V2beta1HorizontalPodAutoscaler;
import run.acloud.api.k8sextended.models.V2beta1MetricSpec;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.commons.util.Utils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServerConversionService {

    @Autowired
    private ServerValidService serverValidService;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    public ServiceSpecGuiVO convertYamlToServiceSpec(ClusterVO cluster, String yamlStr) throws Exception{
        ServiceSpecGuiVO serviceSpec = new ServiceSpecGuiVO();
        serviceSpec.setDeployType(DeployType.GUI.getCode());

//        V1Service service = this.convertYamlToV1Service(cluster, yamlStr);
        V1Service service = ServerUtils.unmarshalYaml(yamlStr);
        if(service == null) {
            throw new CocktailException("Can not found Service spec (Invalid YAML)", ExceptionType.K8sServiceNotFound);
        }

        serviceSpec = this.convertService(serviceSpec, service);
        return serviceSpec;
    }

    public ServerGuiVO convertYamlToGui(ClusterVO cluster, String namespace, String workloadType, String workloadVersion, String workloadDesc, ComponentVO component, String yamlStr) throws Exception{
        List<Object> objs = ServerUtils.getYamlObjects(yamlStr);
        return this.convertYamlToGui(cluster, namespace, workloadType, workloadVersion, workloadDesc, component, objs);
    }

    public ServerGuiVO convertYamlToGui(ClusterVO cluster, String namespace, String workloadType, String workloadVersion, String workloadDesc, ComponentVO component, List<Object> objs) throws Exception{

        JSON k8sJson = new JSON();

        ServerGuiVO serverAdd = new ServerGuiVO();
        serverAdd.setDeployType(DeployType.GUI.getCode());
        serverAdd.setServer(new ServerVO());
        serverAdd.setInitContainers(new ArrayList<>());
        serverAdd.setContainers(new ArrayList<>());
        if (component == null) {
            serverAdd.setComponent(new ComponentVO());
            if(cluster != null) {
                serverAdd.getComponent().setClusterSeq(cluster.getClusterSeq());
            }
            if(StringUtils.isNotBlank(namespace)) {
                serverAdd.getComponent().setNamespaceName(namespace);
            }
        } else {
            serverAdd.setComponent(component);
        }

        if (CollectionUtils.isNotEmpty(objs)) {
            StringBuffer yamlStr = new StringBuffer();
            // set description
            if(StringUtils.isNotBlank(workloadDesc)) {
                serverAdd.getComponent().setDescription(workloadDesc);
            }
            int cnt = 0;
            for (Object obj : objs) {
                if (cnt > 0) {
                    yamlStr.append("---\n");
                }
                yamlStr.append(ServerUtils.marshalYaml(obj));

                Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                switch (kind) {
                    case DEPLOYMENT:
                        if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                            V1Deployment deployment = (V1Deployment) obj;

                            serverAdd.getServer().setWorkloadType(WorkloadType.REPLICA_SERVER.getCode());
                            this.convertDeployment(workloadType, workloadVersion, serverAdd, deployment);
                        }
                        break;
                    case STATEFUL_SET:
                        if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                            V1StatefulSet statefulSet = (V1StatefulSet) obj;

                            this.convertStatefulSet(workloadType, workloadVersion, serverAdd, statefulSet);
                        }
                        break;
                    case DAEMON_SET:
                        if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                            V1DaemonSet daemonSet = (V1DaemonSet) obj;

                            this.convertDaemonSet(workloadType, workloadVersion, serverAdd, daemonSet);
                        }
                        break;
                    case JOB:
                        if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                            V1Job job = (V1Job) obj;

                            this.convertJob(workloadType, workloadVersion, serverAdd, job);
                        }
                        break;
                    case CRON_JOB:
                        if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                            V1beta1CronJob cronJob = (V1beta1CronJob) obj;

                            this.convertCronJob(workloadType, workloadVersion, serverAdd, cronJob);
                        } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                            V1CronJob cronJob = (V1CronJob) obj;

                            this.convertCronJob(workloadType, workloadVersion, serverAdd, cronJob);
                        }
                        break;
                    case HORIZONTAL_POD_AUTOSCALER:
                        if (apiGroupType == K8sApiGroupType.AUTOSCALING) {
                            if (apiType == K8sApiType.V2) {
                                V2HorizontalPodAutoscaler hpa = (V2HorizontalPodAutoscaler)obj;
                                this.convertHorizontalPodAutoscaler(serverAdd, hpa);
                            } else if (apiType == K8sApiType.V2BETA2) {
                                V2beta2HorizontalPodAutoscaler hpa = (V2beta2HorizontalPodAutoscaler)obj;
                                this.convertHorizontalPodAutoscaler(serverAdd, hpa);
                            } else if (apiType == K8sApiType.V2BETA1) {
                                V2beta1HorizontalPodAutoscaler hpa = (V2beta1HorizontalPodAutoscaler)obj;
                                this.convertHorizontalPodAutoscaler(serverAdd, hpa);
                            }
                        }
                        break;
                }
                log.debug(run.acloud.api.k8sextended.util.Yaml.dump(obj));
//                if (cnt > 0) {
//                    yamlStr.append("---\n");
//                }
//                /** HPA 유형이면 Name이 null이 아닌지 확인 후 null이면 ComponentName으로 입력 해 줌 **/
//                if(kind == K8sApiKindType.HORIZONTAL_POD_AUTOSCALER) {
//                    Map<String, Object> meta = (Map<String, Object>) MapUtils.getMap(k8sObjectToMap, KubeConstants.META, new HashMap<>());
//                    if(MapUtils.isNotEmpty(meta)) {
//                        String name = MapUtils.getString(meta, KubeConstants.NAME);
//                        if(StringUtils.isBlank(name)) {
//                            meta.put(KubeConstants.NAME, serverAdd.getComponent().getComponentName());
//                        }
//                    }
//                    yamlStr.append(Yaml.getSnakeYaml().dumpAsMap(k8sObjectToMap));
//                }
//                else {
//                    yamlStr.append(ServerUtils.marshalYaml(obj));
//                }
                cnt++;
            }

            serverAdd.getComponent().setWorkloadType(serverAdd.getServer().getWorkloadType());
            serverAdd.getComponent().setWorkloadManifest(yamlStr.toString());

            // set MultiNic
            serverValidService.setMultiNic(serverAdd, cluster);
        }

        return serverAdd;
    }

//    public void convertHorizontalPodAutoscaler(ServerGuiVO serverAdd, V2beta2HorizontalPodAutoscaler v2Hpa) throws Exception {
//        JSON k8sJson = new JSON();
//        serverAdd.getServer().setHpa(new HorizontalPodAutoscalerVO());
//
//        serverAdd.getServer().getHpa().setMaxReplicas(v2Hpa.getSpec().getMaxReplicas());
//        serverAdd.getServer().getHpa().setMinReplicas(v2Hpa.getSpec().getMinReplicas());
//
//        if (CollectionUtils.isNotEmpty(v2Hpa.getSpec().getMetrics())) {
//            serverAdd.getServer().getHpa().setMetrics(new ArrayList<>());
//
//            for (V2beta2MetricSpec metricSpecRow : v2Hpa.getSpec().getMetrics()) {
//                MetricVO metric = new MetricVO();
//
//                if (metricSpecRow.getResource() != null) {
//                    metric.setType(MetricType.Resource);
//                    metric.setResourceName(metricSpecRow.getResource().getName());
//                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getResource().getTarget());
//                    serverAdd.getServer().getHpa().getMetrics().add(metric);
//                } else if (metricSpecRow.getPods() != null) {
//                    metric.setType(MetricType.Pods);
//                    metric.setPodsMetricName(metricSpecRow.getPods().getMetric().getName());
//                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getPods().getTarget());
//                    if (metricSpecRow.getPods().getMetric().getSelector() != null) {
//                        String selectorJson = k8sJson.serialize(metricSpecRow.getPods().getMetric().getSelector());
//                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
//                    }
//
//                } else if (metricSpecRow.getObject() != null) {
//                    metric.setType(MetricType.Object);
//                    metric.setObjectMetricName(metricSpecRow.getObject().getMetric().getName());
//                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getObject().getTarget());
//                    if (metricSpecRow.getObject().getMetric().getSelector() != null) {
//                        String selectorJson = k8sJson.serialize(metricSpecRow.getObject().getMetric().getSelector());
//                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
//                    }
//                    if (metricSpecRow.getObject().getDescribedObject() != null) {
//                        metric.setObjectTargetApiVerion(metricSpecRow.getObject().getDescribedObject().getApiVersion());
//                        metric.setObjectTargetKind(metricSpecRow.getObject().getDescribedObject().getKind());
//                        metric.setObjectTargetName(metricSpecRow.getObject().getDescribedObject().getName());
//                    }
//
//                } else if (metricSpecRow.getExternal() != null) {
//                    metric.setType(MetricType.External);
//                    metric.setExternalMetricName(metricSpecRow.getExternal().getMetric().getName());
//                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getExternal().getTarget());
//                    if (metricSpecRow.getExternal().getMetric().getSelector() != null) {
//                        String selectorJson = k8sJson.serialize(metricSpecRow.getExternal().getMetric().getSelector());
//                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
//                    }
//                }
//
//            }
//        }
//
//    }
//
//    private void convertMetricTargetV2beta2(MetricVO metric, V2beta2MetricTarget target) throws Exception {
//        metric.setTargetType(MetricTargetType.valueOf(target.getType()));
//        if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.AverageValue.getCode())) {
//            if (metric.getType() == MetricType.Resource) {
//                if (target.getAverageValue() != null) {
//                    if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_CPU, metric.getResourceName())) {
//                        metric.setTargetAverageValue(String.valueOf((long)(target.getAverageValue().getNumber().doubleValue()*1000)));
//                    } else if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_MEMORY, metric.getResourceName())) {
//                        metric.setTargetAverageValue(String.valueOf(target.getAverageValue().getNumber().intValue()/1024/1024));
//                    } else {
//                        metric.setTargetAverageValue(target.getAverageValue().toSuffixedString());
//                    }
//                }
//            } else {
//                metric.setTargetAverageValue(target.getAverageValue().toSuffixedString());
//            }
//        } else if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.Value.getCode())) {
//            metric.setTargetValue(target.getValue().toSuffixedString());
//        } else {
//            metric.setTargetAverageUtilization(target.getAverageUtilization());
//        }
//    }

    public void convertHorizontalPodAutoscaler(ServerGuiVO serverAdd, V2HorizontalPodAutoscaler v2Hpa) throws Exception {
        JSON k8sJson = new JSON();
        serverAdd.getServer().setHpa(new HpaGuiVO());

        serverAdd.getServer().getHpa().setMaxReplicas(v2Hpa.getSpec().getMaxReplicas());
        serverAdd.getServer().getHpa().setMinReplicas(v2Hpa.getSpec().getMinReplicas());

        /** 2020.10.16 : Horizontal Pod Autoscaler의 이름을 ServerGuiVO Model에 추가... **/
        serverAdd.getServer().getHpa().setName(Optional.ofNullable(v2Hpa).map(V2HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->null));

        // spec.metrics
        if (CollectionUtils.isNotEmpty(v2Hpa.getSpec().getMetrics())) {
            serverAdd.getServer().getHpa().setMetrics(new ArrayList<>());

            for (V2MetricSpec metricSpecRow : v2Hpa.getSpec().getMetrics()) {
                MetricVO metric = new MetricVO();

                if (metricSpecRow.getResource() != null) {
                    metric.setType(MetricType.Resource);
                    metric.setResourceName(metricSpecRow.getResource().getName());
                    this.convertMetricTargetV2(metric, metricSpecRow.getResource().getTarget());
                    serverAdd.getServer().getHpa().getMetrics().add(metric);
                } else if (metricSpecRow.getPods() != null) {
                    metric.setType(MetricType.Pods);
                    metric.setPodsMetricName(metricSpecRow.getPods().getMetric().getName());
                    this.convertMetricTargetV2(metric, metricSpecRow.getPods().getTarget());
                    if (metricSpecRow.getPods().getMetric().getSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getPods().getMetric().getSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }

                } else if (metricSpecRow.getObject() != null) {
                    metric.setType(MetricType.Object);
                    metric.setObjectMetricName(metricSpecRow.getObject().getMetric().getName());
                    this.convertMetricTargetV2(metric, metricSpecRow.getObject().getTarget());
                    if (metricSpecRow.getObject().getMetric().getSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getObject().getMetric().getSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }
                    if (metricSpecRow.getObject().getDescribedObject() != null) {
                        metric.setObjectTargetApiVerion(metricSpecRow.getObject().getDescribedObject().getApiVersion());
                        metric.setObjectTargetKind(metricSpecRow.getObject().getDescribedObject().getKind());
                        metric.setObjectTargetName(metricSpecRow.getObject().getDescribedObject().getName());
                    }

                } else if (metricSpecRow.getExternal() != null) {
                    metric.setType(MetricType.External);
                    metric.setExternalMetricName(metricSpecRow.getExternal().getMetric().getName());
                    this.convertMetricTargetV2(metric, metricSpecRow.getExternal().getTarget());
                    if (metricSpecRow.getExternal().getMetric().getSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getExternal().getMetric().getSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }
                } else if (metricSpecRow.getContainerResource() != null) {
                    metric.setType(MetricType.ContainerResource);
                    metric.setContainerResourceName(metricSpecRow.getContainerResource().getName());
                    metric.setContainerName(metricSpecRow.getContainerResource().getContainer());
                    this.convertMetricTargetV2(metric, metricSpecRow.getContainerResource().getTarget());
                }

            }
        }

        // spec.behavior
        if (v2Hpa.getSpec().getBehavior() != null) {
            if (v2Hpa.getSpec().getBehavior().getScaleDown() != null) {
                String scaleDownJson = JsonUtils.toGson(v2Hpa.getSpec().getBehavior().getScaleDown());
                HpaScalingRulesVO scaleDown = JsonUtils.fromGson(scaleDownJson, HpaScalingRulesVO.class);
                serverAdd.getServer().getHpa().setScaleDown(scaleDown);
            }
            if (v2Hpa.getSpec().getBehavior().getScaleUp() != null) {
                String scaleUpJson = JsonUtils.toGson(v2Hpa.getSpec().getBehavior().getScaleUp());
                HpaScalingRulesVO scaleUp = JsonUtils.fromGson(scaleUpJson, HpaScalingRulesVO.class);
                serverAdd.getServer().getHpa().setScaleUp(scaleUp);
            }
        }
    }

    private void convertMetricTargetV2(MetricVO metric, V2MetricTarget target) throws Exception {
        metric.setTargetType(MetricTargetType.valueOf(target.getType()));
        if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.AverageValue.getCode())) {
            if (metric.getType() == MetricType.Resource) {
                if (target.getAverageValue() != null) {
                    if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_CPU, metric.getResourceName())) {
                        metric.setTargetAverageValue(String.valueOf((long)(target.getAverageValue().getNumber().doubleValue()*1000)));
                    } else if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_MEMORY, metric.getResourceName())) {
                        metric.setTargetAverageValue(String.valueOf(target.getAverageValue().getNumber().longValue()/1024/1024));
                    } else {
                        metric.setTargetAverageValue(target.getAverageValue().toSuffixedString());
                    }
                }
            } else {
                metric.setTargetAverageValue(target.getAverageValue().toSuffixedString());
            }
        } else if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.Value.getCode())) {
            metric.setTargetValue(target.getValue().toSuffixedString());
        } else {
            metric.setTargetAverageUtilization(target.getAverageUtilization());
        }
    }

    public void convertHorizontalPodAutoscaler(ServerGuiVO serverAdd, V2beta2HorizontalPodAutoscaler v2Hpa) throws Exception {
        JSON k8sJson = new JSON();
        serverAdd.getServer().setHpa(new HpaGuiVO());

        serverAdd.getServer().getHpa().setMaxReplicas(v2Hpa.getSpec().getMaxReplicas());
        serverAdd.getServer().getHpa().setMinReplicas(v2Hpa.getSpec().getMinReplicas());

        /** 2020.10.16 : Horizontal Pod Autoscaler의 이름을 ServerGuiVO Model에 추가... **/
        serverAdd.getServer().getHpa().setName(Optional.ofNullable(v2Hpa).map(V2beta2HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->null));

        // spec.metrics
        if (CollectionUtils.isNotEmpty(v2Hpa.getSpec().getMetrics())) {
            serverAdd.getServer().getHpa().setMetrics(new ArrayList<>());

            for (V2beta2MetricSpec metricSpecRow : v2Hpa.getSpec().getMetrics()) {
                MetricVO metric = new MetricVO();

                if (metricSpecRow.getResource() != null) {
                    metric.setType(MetricType.Resource);
                    metric.setResourceName(metricSpecRow.getResource().getName());
                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getResource().getTarget());
                    serverAdd.getServer().getHpa().getMetrics().add(metric);
                } else if (metricSpecRow.getPods() != null) {
                    metric.setType(MetricType.Pods);
                    metric.setPodsMetricName(metricSpecRow.getPods().getMetric().getName());
                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getPods().getTarget());
                    if (metricSpecRow.getPods().getMetric().getSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getPods().getMetric().getSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }

                } else if (metricSpecRow.getObject() != null) {
                    metric.setType(MetricType.Object);
                    metric.setObjectMetricName(metricSpecRow.getObject().getMetric().getName());
                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getObject().getTarget());
                    if (metricSpecRow.getObject().getMetric().getSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getObject().getMetric().getSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }
                    if (metricSpecRow.getObject().getDescribedObject() != null) {
                        metric.setObjectTargetApiVerion(metricSpecRow.getObject().getDescribedObject().getApiVersion());
                        metric.setObjectTargetKind(metricSpecRow.getObject().getDescribedObject().getKind());
                        metric.setObjectTargetName(metricSpecRow.getObject().getDescribedObject().getName());
                    }

                } else if (metricSpecRow.getExternal() != null) {
                    metric.setType(MetricType.External);
                    metric.setExternalMetricName(metricSpecRow.getExternal().getMetric().getName());
                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getExternal().getTarget());
                    if (metricSpecRow.getExternal().getMetric().getSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getExternal().getMetric().getSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }
                } else if (metricSpecRow.getContainerResource() != null) {
                    metric.setType(MetricType.ContainerResource);
                    metric.setContainerResourceName(metricSpecRow.getContainerResource().getName());
                    metric.setContainerName(metricSpecRow.getContainerResource().getContainer());
                    this.convertMetricTargetV2beta2(metric, metricSpecRow.getContainerResource().getTarget());
                }

            }
        }

        // spec.behavior
        if (v2Hpa.getSpec().getBehavior() != null) {
            if (v2Hpa.getSpec().getBehavior().getScaleDown() != null) {
                String scaleDownJson = JsonUtils.toGson(v2Hpa.getSpec().getBehavior().getScaleDown());
                HpaScalingRulesVO scaleDown = JsonUtils.fromGson(scaleDownJson, HpaScalingRulesVO.class);
                serverAdd.getServer().getHpa().setScaleDown(scaleDown);
            }
            if (v2Hpa.getSpec().getBehavior().getScaleUp() != null) {
                String scaleUpJson = JsonUtils.toGson(v2Hpa.getSpec().getBehavior().getScaleUp());
                HpaScalingRulesVO scaleUp = JsonUtils.fromGson(scaleUpJson, HpaScalingRulesVO.class);
                serverAdd.getServer().getHpa().setScaleUp(scaleUp);
            }
        }
    }

    private void convertMetricTargetV2beta2(MetricVO metric, V2beta2MetricTarget target) throws Exception {
        metric.setTargetType(MetricTargetType.valueOf(target.getType()));
        if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.AverageValue.getCode())) {
            if (metric.getType() == MetricType.Resource) {
                if (target.getAverageValue() != null) {
                    if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_CPU, metric.getResourceName())) {
                        metric.setTargetAverageValue(String.valueOf((long)(target.getAverageValue().getNumber().doubleValue()*1000)));
                    } else if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_MEMORY, metric.getResourceName())) {
                        metric.setTargetAverageValue(String.valueOf(target.getAverageValue().getNumber().intValue()/1024/1024));
                    } else {
                        metric.setTargetAverageValue(target.getAverageValue().toSuffixedString());
                    }
                }
            } else {
                metric.setTargetAverageValue(target.getAverageValue().toSuffixedString());
            }
        } else if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.Value.getCode())) {
            metric.setTargetValue(target.getValue().toSuffixedString());
        } else {
            metric.setTargetAverageUtilization(target.getAverageUtilization());
        }
    }

    public void convertHorizontalPodAutoscaler(ServerGuiVO serverAdd, V2beta1HorizontalPodAutoscaler v2Hpa) throws Exception {
        JSON k8sJson = new JSON();
        serverAdd.getServer().setHpa(new HpaGuiVO());

        serverAdd.getServer().getHpa().setMaxReplicas(v2Hpa.getSpec().getMaxReplicas());
        serverAdd.getServer().getHpa().setMinReplicas(v2Hpa.getSpec().getMinReplicas());
        /** 2020.10.16 : Horizontal Pod Autoscaler의 이름을 ServerGuiVO Model에 추가... **/
        serverAdd.getServer().getHpa().setName(Optional.ofNullable(v2Hpa).map(V2beta1HorizontalPodAutoscaler::getMetadata).map(V1ObjectMeta::getName).orElseGet(() ->null));

        if (CollectionUtils.isNotEmpty(v2Hpa.getSpec().getMetrics())) {
            serverAdd.getServer().getHpa().setMetrics(new ArrayList<>());

            for (V2beta1MetricSpec metricSpecRow : v2Hpa.getSpec().getMetrics()) {
                MetricVO metric = new MetricVO();

                if (metricSpecRow.getResource() != null) {
                    metric.setType(MetricType.Resource);
                    metric.setResourceName(metricSpecRow.getResource().getName());
                    if (metricSpecRow.getResource().getTargetAverageValue() != null) {
                        metric.setTargetType(MetricTargetType.AverageValue);
                        if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_CPU, metric.getResourceName())) {
                            metric.setTargetAverageValue(String.valueOf((long)(metricSpecRow.getResource().getTargetAverageValue().getNumber().doubleValue()*1000)));
                        } else if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_MEMORY, metric.getResourceName())) {
                            metric.setTargetAverageValue(String.valueOf(metricSpecRow.getResource().getTargetAverageValue().getNumber().intValue()/1024/1024));
                        } else {
                            metric.setTargetAverageValue(metricSpecRow.getResource().getTargetAverageValue().toSuffixedString());
                        }
                    } else {
                        metric.setTargetType(MetricTargetType.Utilization);
                        metric.setTargetAverageUtilization(metricSpecRow.getResource().getTargetAverageUtilization());
                    }
                    serverAdd.getServer().getHpa().getMetrics().add(metric);
                } else if (metricSpecRow.getPods() != null) {
                    metric.setType(MetricType.Pods);
                    metric.setPodsMetricName(metricSpecRow.getPods().getMetricName());
                    if (metricSpecRow.getPods().getTargetAverageValue() != null) {
                        metric.setTargetAverageValue(metricSpecRow.getPods().getTargetAverageValue().toSuffixedString());
                    }
                    if (metricSpecRow.getPods().getSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getPods().getSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }

                } else if (metricSpecRow.getObject() != null) {
                    metric.setType(MetricType.Object);
                    metric.setObjectMetricName(metricSpecRow.getObject().getMetricName());
                    if (metricSpecRow.getObject().getAverageValue() != null) {
                        metric.setTargetAverageValue(metricSpecRow.getObject().getAverageValue().toSuffixedString());
                    } else if (metricSpecRow.getObject().getTargetValue() != null) {
                        metric.setTargetValue(metricSpecRow.getObject().getTargetValue().toSuffixedString());
                    }
                    if (metricSpecRow.getObject().getSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getObject().getSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }
                    if (metricSpecRow.getObject().getTarget() != null) {
                        metric.setObjectTargetApiVerion(metricSpecRow.getObject().getTarget().getApiVersion());
                        metric.setObjectTargetKind(metricSpecRow.getObject().getTarget().getKind());
                        metric.setObjectTargetName(metricSpecRow.getObject().getTarget().getName());
                    }

                } else if (metricSpecRow.getExternal() != null) {
                    metric.setType(MetricType.External);
                    metric.setExternalMetricName(metricSpecRow.getExternal().getMetricName());
                    if (metricSpecRow.getExternal().getTargetAverageValue() != null) {
                        metric.setTargetAverageValue(metricSpecRow.getExternal().getTargetAverageValue().toSuffixedString());
                    } else if (metricSpecRow.getExternal().getTargetValue() != null) {
                        metric.setTargetValue(metricSpecRow.getExternal().getTargetValue().toSuffixedString());
                    }
                    if (metricSpecRow.getExternal().getMetricSelector() != null) {
                        String selectorJson = k8sJson.serialize(metricSpecRow.getExternal().getMetricSelector());
                        metric.setMetricLabelSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                    }
                }

            }
        }

    }

    /**
     * V1Service 모델을 ServiceSpecGuiVO 모델로 Convert 한 후 ServiceGuiVO에 입력..
     * @param serverAdd
     * @param v1Service
     * @throws Exception
     */
    public void convertService(ServerGuiVO serverAdd, V1Service v1Service) throws Exception {

        if (CollectionUtils.isEmpty(serverAdd.getServices())) {
            serverAdd.setServices(new ArrayList<>());
        }

        ServiceSpecGuiVO serviceSpec = new ServiceSpecGuiVO();
        serviceSpec.setDeployType(serverAdd.getDeployType());
        serviceSpec = this.convertService(serviceSpec, v1Service);

        serverAdd.getServices().add(serviceSpec);
    }

    /**
     * V1Service 모델을 ServiceSpecGuiVO 모델로 Convert
     * @param serviceSpec
     * @param v1Service
     * @throws Exception
     */
    public ServiceSpecGuiVO convertService(ServiceSpecGuiVO serviceSpec, V1Service v1Service) throws Exception {
        if(serviceSpec == null) {
            serviceSpec = new ServiceSpecGuiVO();
            serviceSpec.setDeployType(DeployType.GUI.getCode());
        }

        serviceSpec.setComponentSeq(this.getComponentSeqByLabels(v1Service.getMetadata().getLabels()));
        serviceSpec.setName(v1Service.getMetadata().getName());
        serviceSpec.setNamespaceName(v1Service.getMetadata().getNamespace());
        serviceSpec.setLabels(v1Service.getMetadata().getLabels());
        serviceSpec.setLabelSelector(v1Service.getSpec().getSelector()); /** 2019.11.08 : Redion : LabelSelector도 Set 하도록 추가.. **/
        serviceSpec.setAnnotations(v1Service.getMetadata().getAnnotations());
        serviceSpec.setClusterIp(v1Service.getSpec().getClusterIP());

        // ServiceType 셋팅
        if (StringUtils.equalsIgnoreCase(v1Service.getSpec().getClusterIP(), "None")) {
            serviceSpec.setServiceType(PortType.HEADLESS.getCode());
            serviceSpec.setHeadlessFlag(Boolean.TRUE);
        } else {
            if (StringUtils.isNotBlank(v1Service.getSpec().getType())) {
                serviceSpec.setServiceType(PortType.findPortType(v1Service.getSpec().getType()).getCode());
                serviceSpec.setHeadlessFlag(Boolean.FALSE);
            } else {
                serviceSpec.setServiceType(PortType.CLUSTER_IP.getCode());
                serviceSpec.setHeadlessFlag(Boolean.FALSE);
            }
        }

        if (PortType.valueOf(serviceSpec.getServiceType()) == PortType.EXTERNAL_NAME) {
            serviceSpec.setExternalName(v1Service.getSpec().getExternalName());
        }

        if (MapUtils.isNotEmpty(v1Service.getMetadata().getAnnotations())) {
            if (PortType.valueOf(serviceSpec.getServiceType()) == PortType.LOADBALANCER) {
                boolean internalLBFlag = false;
                if (v1Service.getMetadata().getAnnotations().containsKey(KubeConstants.META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_KEY)
                    || v1Service.getMetadata().getAnnotations().containsKey(KubeConstants.META_ANNOTATIONS_GCP_INTERNAL_LOADBALANCER_KEY)
                    || v1Service.getMetadata().getAnnotations().containsKey(KubeConstants.META_ANNOTATIONS_AZR_INTERNAL_LOADBALANCER_KEY)
                    || v1Service.getMetadata().getAnnotations().containsKey(KubeConstants.META_ANNOTATIONS_NCP_INTERNAL_LOADBALANCER_KEY)
                    ) {
                    internalLBFlag = true;
                }

                serviceSpec.setInternalLBFlag(internalLBFlag);
            }
        }

        if (StringUtils.equals(v1Service.getSpec().getSessionAffinity(), "ClientIP")) {
            serviceSpec.setStickySessionFlag(Boolean.TRUE);
            if (v1Service.getSpec().getSessionAffinityConfig() != null && v1Service.getSpec().getSessionAffinityConfig().getClientIP() != null) {
                serviceSpec.setStickySessionTimeoutSeconds(v1Service.getSpec().getSessionAffinityConfig().getClientIP().getTimeoutSeconds());
            }
        } else {
            serviceSpec.setStickySessionFlag(Boolean.FALSE);
            serviceSpec.setStickySessionTimeoutSeconds(10800);
        }

        if (CollectionUtils.isNotEmpty(v1Service.getSpec().getPorts())) {
            serviceSpec.setServicePorts(new ArrayList<>());

            for (V1ServicePort servicePortRow : v1Service.getSpec().getPorts()) {
                ServicePortVO servicePort = new ServicePortVO();
                servicePort.setName(servicePortRow.getName());
                servicePort.setProtocol(servicePortRow.getProtocol());
                servicePort.setPort(servicePortRow.getPort());
                if(servicePortRow.getTargetPort() != null) {
                    servicePort.setTargetPort(servicePortRow.getTargetPort().toString());
                }
                servicePort.setNodePort(servicePortRow.getNodePort());

                serviceSpec.getServicePorts().add(servicePort);
            }
        }

        return serviceSpec;
    }


    public void convertDeployment(String workloadType, String workloadVersion, ServerGuiVO serverAdd, V1Deployment deployment) throws Exception {
        this.convertObjectMeta(serverAdd, deployment.getMetadata());
        this.convertPodTemplate(serverAdd, deployment.getSpec().getTemplate());

        /**
         * server
         */
        // ApplicationVersion
        serverAdd.getServer().setApplicationVersion("1.0");

        // WorkloadType, ServerType
        serverAdd.getServer().setWorkloadType(WorkloadType.REPLICA_SERVER.getCode());
        serverAdd.getServer().setServerType(ServerType.MULTI.getCode());

        // WorkloadVersion
        if (StringUtils.isBlank(workloadVersion)) {
            serverAdd.getServer().setWorkloadVersion(WorkloadVersion.V1.getCode());
        } else {
            serverAdd.getServer().setWorkloadVersion(workloadVersion);
        }

        // computeTotal
        if (deployment.getSpec().getReplicas() != null) {
            serverAdd.getServer().setComputeTotal(deployment.getSpec().getReplicas());
        }
        // 2022.09.22 hjchoi - 아래 이슈로 HPA가 존재한다면 없을 수도 있으므로 없다면 셋팅하지 않음
        // HPA가 활성화되어 있으면, 디플로이먼트, 스테이트풀셋 모두 또는 둘 중 하나의 매니페스트에서 spec.replicas의 값을 삭제하는 것이 바람직하다
        // https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#%EB%94%94%ED%94%8C%EB%A1%9C%EC%9D%B4%EB%A8%BC%ED%8A%B8%EC%99%80-%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%ED%92%80%EC%85%8B%EC%9D%84-horizontal-autoscaling%EC%9C%BC%EB%A1%9C-%EC%A0%84%ED%99%98%ED%95%98%EA%B8%B0
//        else {
//            serverAdd.getServer().setComputeTotal(1);
//        }

        // strategy
        serverAdd.getServer().setStrategy(new DeploymentStrategyVO());
        serverAdd.getServer().getStrategy().setType(DeploymentStrategyType.valueOf(deployment.getSpec().getStrategy().getType()));
        if (DeploymentStrategyType.valueOf(deployment.getSpec().getStrategy().getType()) == DeploymentStrategyType.RollingUpdate) {
            if (deployment.getSpec().getStrategy().getRollingUpdate().getMaxSurge() != null) {
                if (!deployment.getSpec().getStrategy().getRollingUpdate().getMaxSurge().isInteger()) {
                    serverAdd.getServer().getStrategy().setMaxSurge(deployment.getSpec().getStrategy().getRollingUpdate().getMaxSurge().getStrValue());
                } else {
                    serverAdd.getServer().getStrategy().setMaxSurge(String.valueOf(deployment.getSpec().getStrategy().getRollingUpdate().getMaxSurge().getIntValue()));
                }
            }
            if (deployment.getSpec().getStrategy().getRollingUpdate().getMaxUnavailable() != null) {
                if (!deployment.getSpec().getStrategy().getRollingUpdate().getMaxUnavailable().isInteger()) {
                    serverAdd.getServer().getStrategy().setMaxUnavailable(deployment.getSpec().getStrategy().getRollingUpdate().getMaxUnavailable().getStrValue());
                } else {
                    serverAdd.getServer().getStrategy().setMaxUnavailable(String.valueOf(deployment.getSpec().getStrategy().getRollingUpdate().getMaxUnavailable().getIntValue()));
                }
            }
        }

        // minReadySeconds
        if (deployment.getSpec().getMinReadySeconds() != null) {
            serverAdd.getServer().setMinReadySeconds(deployment.getSpec().getMinReadySeconds());
        }
    }

    public void convertStatefulSet(String workloadType, String workloadVersion, ServerGuiVO serverAdd, V1StatefulSet statefulSet) throws Exception {
        this.convertObjectMeta(serverAdd, statefulSet.getMetadata());

        /**
         * server
         */
        // ApplicationVersion
        serverAdd.getServer().setApplicationVersion("1.0");

        // WorkloadType, ServerType
        serverAdd.getServer().setWorkloadType(WorkloadType.STATEFUL_SET_SERVER.getCode());
        serverAdd.getServer().setServerType(ServerType.STATEFUL_SET.getCode());

        // WorkloadVersion
        if (StringUtils.isBlank(workloadVersion)) {
            serverAdd.getServer().setWorkloadVersion(WorkloadVersion.V1.getCode());
        } else {
            serverAdd.getServer().setWorkloadVersion(workloadVersion);
        }

        // computeTotal
        // 2022.09.22 hjchoi - 아래 이슈로 HPA가 존재한다면 없을 수도 있으므로 없다면 셋팅하지 않음
        // HPA가 활성화되어 있으면, 디플로이먼트, 스테이트풀셋 모두 또는 둘 중 하나의 매니페스트에서 spec.replicas의 값을 삭제하는 것이 바람직하다
        // https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#%EB%94%94%ED%94%8C%EB%A1%9C%EC%9D%B4%EB%A8%BC%ED%8A%B8%EC%99%80-%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%ED%92%80%EC%85%8B%EC%9D%84-horizontal-autoscaling%EC%9C%BC%EB%A1%9C-%EC%A0%84%ED%99%98%ED%95%98%EA%B8%B0
//        serverAdd.getServer().setComputeTotal(Optional.ofNullable(statefulSet.getSpec().getReplicas()).orElseGet(() ->0));
        if (statefulSet.getSpec().getReplicas() != null) {
            serverAdd.getServer().setComputeTotal(statefulSet.getSpec().getReplicas());
        }

        serverAdd.getServer().setPodManagementPolicy(statefulSet.getSpec().getPodManagementPolicy());
        serverAdd.getServer().setServiceName(statefulSet.getSpec().getServiceName());

        // strategy
        serverAdd.getServer().setStatefulSetStrategy(new StatefulSetStrategyVO());
        serverAdd.getServer().getStatefulSetStrategy().setType(StatefulSetStrategyType.valueOf(statefulSet.getSpec().getUpdateStrategy().getType()));
        if (StatefulSetStrategyType.valueOf(statefulSet.getSpec().getUpdateStrategy().getType()) == StatefulSetStrategyType.RollingUpdate) {
            if (statefulSet.getSpec().getUpdateStrategy().getRollingUpdate() != null && statefulSet.getSpec().getUpdateStrategy().getRollingUpdate().getPartition() != null) {
                serverAdd.getServer().getStatefulSetStrategy().setPartition(statefulSet.getSpec().getUpdateStrategy().getRollingUpdate().getPartition());
            }
        }

        // VolumeClaimTemplates
        if (CollectionUtils.isNotEmpty(statefulSet.getSpec().getVolumeClaimTemplates())) {
            serverAdd.setVolumeTemplates(new ArrayList<>());
            QuantityFormatter quantityFormatter = new QuantityFormatter();
            for (V1PersistentVolumeClaim pvcRow : statefulSet.getSpec().getVolumeClaimTemplates()) {
                PersistentVolumeClaimGuiVO pvc = new PersistentVolumeClaimGuiVO();
                AccessMode accessMode = null;
                if(pvcRow.getSpec().getAccessModes() != null) {
                    accessMode = AccessMode.getAccessMode(pvcRow.getSpec().getAccessModes().get(0));
                    if (accessMode == AccessMode.RWO) {
                        pvc.setPersistentVolumeType(PersistentVolumeType.SINGLE);
                    } else {
                        pvc.setPersistentVolumeType(PersistentVolumeType.SHARED);
                    }
                }
                pvc.setLabels(pvcRow.getMetadata().getLabels());
                pvc.setAnnotations(pvcRow.getMetadata().getAnnotations());
                pvc.setName(pvcRow.getMetadata().getName());
                pvc.setStorageClassName(pvcRow.getSpec().getStorageClassName());
                pvc.setAccessMode(accessMode);

                String PARTS_RE = "[eEinumkKMGTP]+";
                String value = pvcRow.getSpec().getResources().getRequests().get(KubeConstants.VOLUMES_STORAGE).toSuffixedString();
                String[] parts = value.split(PARTS_RE);
                BigDecimal numericValue = new BigDecimal(parts[0]);
                String suffix = value.substring(parts[0].length());
                BaseExponent baseExponent = new SuffixFormatter().parse(suffix);
                Integer capacity = Integer.valueOf(parts[0]);
                switch(pvcRow.getSpec().getResources().getRequests().get(KubeConstants.VOLUMES_STORAGE).getFormat()) {
                    case DECIMAL_SI:
                    case DECIMAL_EXPONENT:
                        break;
                    case BINARY_SI:
                        // Gi를 2^30 기준으로 처리
                        BigDecimal unitMultiplier = BigDecimal.valueOf(baseExponent.getBase()).pow(baseExponent.getExponent(), MathContext.DECIMAL64);
                        // 2^30 보다 크다면 큰 만큼 나눠줌
                        if (baseExponent.getExponent() > 30) {
                            BigDecimal unitDivisor = BigDecimal.valueOf(baseExponent.getBase()).pow(baseExponent.getExponent() - 30, MathContext.DECIMAL64);
                            BigDecimal unitlessValue = numericValue.multiply(unitMultiplier).divide(unitDivisor,  MathContext.DECIMAL64);
                            capacity = unitlessValue.intValue();
                        }
                        // 2^30 보다 작다면 작은 만큼 곱해줌
                        else if (baseExponent.getExponent() < 30) {
                            BigDecimal unitMultiplier2 = BigDecimal.valueOf(baseExponent.getBase()).pow(30 - baseExponent.getExponent(), MathContext.DECIMAL64);
                            BigDecimal unitlessValue = numericValue.multiply(unitMultiplier).multiply(unitMultiplier2,  MathContext.DECIMAL64);
                            capacity = unitlessValue.intValue();
                        }
                        break;
                }

                pvc.setCapacity(capacity);

                serverAdd.getVolumeTemplates().add(pvc);
            }
        }

        this.convertPodTemplate(serverAdd, statefulSet.getSpec().getTemplate());
    }

    public void convertDaemonSet(String workloadType, String workloadVersion, ServerGuiVO serverAdd, V1DaemonSet daemonSet) throws Exception {
        this.convertObjectMeta(serverAdd, daemonSet.getMetadata());
        this.convertPodTemplate(serverAdd, daemonSet.getSpec().getTemplate());

        /**
         * server
         */
        // ApplicationVersion
        serverAdd.getServer().setApplicationVersion("1.0");

        // WorkloadType, ServerType
        serverAdd.getServer().setWorkloadType(WorkloadType.DAEMON_SET_SERVER.getCode());
        serverAdd.getServer().setServerType(ServerType.DAEMON_SET.getCode());

        // WorkloadVersion
        if (StringUtils.isBlank(workloadVersion)) {
            serverAdd.getServer().setWorkloadVersion(WorkloadVersion.V1.getCode());
        } else {
            serverAdd.getServer().setWorkloadVersion(workloadVersion);
        }

        // computeTotal
        serverAdd.getServer().setComputeTotal(0);

        // DaemonSetStrategy
        serverAdd.getServer().setDaemonSetStrategy(new DaemonSetStrategyVO());
        serverAdd.getServer().getDaemonSetStrategy().setType(DaemonSetStrategyType.valueOf(daemonSet.getSpec().getUpdateStrategy().getType()));
        if (DaemonSetStrategyType.valueOf(daemonSet.getSpec().getUpdateStrategy().getType()) == DaemonSetStrategyType.RollingUpdate) {
            if (daemonSet.getSpec().getUpdateStrategy().getRollingUpdate().getMaxUnavailable() != null) {
                if (daemonSet.getSpec().getUpdateStrategy().getRollingUpdate().getMaxUnavailable().isInteger()) {
                    serverAdd.getServer().getDaemonSetStrategy().setMaxUnavailable(String.valueOf(daemonSet.getSpec().getUpdateStrategy().getRollingUpdate().getMaxUnavailable().getIntValue()));
                } else {
                    serverAdd.getServer().getDaemonSetStrategy().setMaxUnavailable(daemonSet.getSpec().getUpdateStrategy().getRollingUpdate().getMaxUnavailable().getStrValue());
                }
            }
        }

        // minReadySeconds
        if (daemonSet.getSpec().getMinReadySeconds() != null) {
            serverAdd.getServer().setMinReadySeconds(daemonSet.getSpec().getMinReadySeconds());
        }
    }

    public void convertJob(String workloadType, String workloadVersion, ServerGuiVO serverAdd, V1Job job) throws Exception {
        this.convertObjectMeta(serverAdd, job.getMetadata());
        this.convertPodTemplate(serverAdd, job.getSpec().getTemplate());

        /**
         * server
         */
        // ApplicationVersion
        serverAdd.getServer().setApplicationVersion("1.0");

        // WorkloadType, ServerType
        serverAdd.getServer().setWorkloadType(WorkloadType.JOB_SERVER.getCode());
        serverAdd.getServer().setServerType(ServerType.JOB.getCode());

        // WorkloadVersion
        if (StringUtils.isBlank(workloadVersion)) {
            serverAdd.getServer().setWorkloadVersion(WorkloadVersion.V1.getCode());
        } else {
            serverAdd.getServer().setWorkloadVersion(workloadVersion);
        }

        // computeTotal
        serverAdd.getServer().setComputeTotal(Optional.ofNullable(job.getSpec().getCompletions()).orElseGet(() ->Optional.ofNullable(job.getSpec().getParallelism()).orElseGet(() ->1)));
        serverAdd.getServer().setParallelism(Optional.ofNullable(job.getSpec().getParallelism()).orElseGet(() ->1));
        serverAdd.getServer().setActiveDeadlineSeconds(job.getSpec().getActiveDeadlineSeconds());
        serverAdd.getServer().setBackoffLimit(job.getSpec().getBackoffLimit());
        serverAdd.getServer().setTtlSecondsAfterFinished(job.getSpec().getTtlSecondsAfterFinished());
    }

    public void convertCronJob(String workloadType, String workloadVersion, ServerGuiVO serverAdd, V1beta1CronJob cronJob) throws Exception {
        this.convertObjectMeta(serverAdd, cronJob.getMetadata());
        this.convertPodTemplate(serverAdd, cronJob.getSpec().getJobTemplate().getSpec().getTemplate());

        /**
         * server
         */
        // ApplicationVersion
        serverAdd.getServer().setApplicationVersion("1.0");

        // WorkloadType, ServerType
        serverAdd.getServer().setWorkloadType(WorkloadType.CRON_JOB_SERVER.getCode());
        serverAdd.getServer().setServerType(ServerType.CRON_JOB.getCode());

        // WorkloadVersion
        if (StringUtils.isBlank(workloadVersion)) {
            serverAdd.getServer().setWorkloadVersion(WorkloadVersion.V1.getCode());
        } else {
            serverAdd.getServer().setWorkloadVersion(workloadVersion);
        }

        // CronJob
        serverAdd.getServer().setConcurrencyPolicy(ConcurrencyPolicy.valueOf(cronJob.getSpec().getConcurrencyPolicy()));
        serverAdd.getServer().setSchedule(cronJob.getSpec().getSchedule());
        serverAdd.getServer().setStartingDeadlineSeconds(cronJob.getSpec().getStartingDeadlineSeconds());
        serverAdd.getServer().setSuccessfulJobsHistoryLimit(cronJob.getSpec().getSuccessfulJobsHistoryLimit());
        serverAdd.getServer().setFailedJobsHistoryLimit(cronJob.getSpec().getFailedJobsHistoryLimit());
        serverAdd.getServer().setSuspend(cronJob.getSpec().getSuspend());

        // Job
        // computeTotal
        serverAdd.getServer().setComputeTotal(Optional.ofNullable(cronJob.getSpec().getJobTemplate().getSpec().getCompletions()).orElseGet(() ->Optional.ofNullable(cronJob.getSpec().getJobTemplate().getSpec().getParallelism()).orElseGet(() ->1)));
        serverAdd.getServer().setParallelism(Optional.ofNullable(cronJob.getSpec().getJobTemplate().getSpec().getParallelism()).orElseGet(() ->1));
        serverAdd.getServer().setActiveDeadlineSeconds(cronJob.getSpec().getJobTemplate().getSpec().getActiveDeadlineSeconds());
        serverAdd.getServer().setBackoffLimit(cronJob.getSpec().getJobTemplate().getSpec().getBackoffLimit());
        serverAdd.getServer().setTtlSecondsAfterFinished(cronJob.getSpec().getJobTemplate().getSpec().getTtlSecondsAfterFinished());
    }

    public void convertCronJob(String workloadType, String workloadVersion, ServerGuiVO serverAdd, V1CronJob cronJob) throws Exception {
        this.convertObjectMeta(serverAdd, cronJob.getMetadata());
        this.convertPodTemplate(serverAdd, cronJob.getSpec().getJobTemplate().getSpec().getTemplate());

        /**
         * server
         */
        // ApplicationVersion
        serverAdd.getServer().setApplicationVersion("1.0");

        // WorkloadType, ServerType
        serverAdd.getServer().setWorkloadType(WorkloadType.CRON_JOB_SERVER.getCode());
        serverAdd.getServer().setServerType(ServerType.CRON_JOB.getCode());

        // WorkloadVersion
        if (StringUtils.isBlank(workloadVersion)) {
            serverAdd.getServer().setWorkloadVersion(WorkloadVersion.V1.getCode());
        } else {
            serverAdd.getServer().setWorkloadVersion(workloadVersion);
        }

        // CronJob
        serverAdd.getServer().setConcurrencyPolicy(ConcurrencyPolicy.valueOf(cronJob.getSpec().getConcurrencyPolicy()));
        serverAdd.getServer().setSchedule(cronJob.getSpec().getSchedule());
        serverAdd.getServer().setStartingDeadlineSeconds(cronJob.getSpec().getStartingDeadlineSeconds());
        serverAdd.getServer().setSuccessfulJobsHistoryLimit(cronJob.getSpec().getSuccessfulJobsHistoryLimit());
        serverAdd.getServer().setFailedJobsHistoryLimit(cronJob.getSpec().getFailedJobsHistoryLimit());
        serverAdd.getServer().setSuspend(cronJob.getSpec().getSuspend());

        // Job
        // computeTotal
        serverAdd.getServer().setComputeTotal(Optional.ofNullable(cronJob.getSpec().getJobTemplate().getSpec().getCompletions()).orElseGet(() ->Optional.ofNullable(cronJob.getSpec().getJobTemplate().getSpec().getParallelism()).orElseGet(() ->1)));
        serverAdd.getServer().setParallelism(Optional.ofNullable(cronJob.getSpec().getJobTemplate().getSpec().getParallelism()).orElseGet(() ->1));
        serverAdd.getServer().setActiveDeadlineSeconds(cronJob.getSpec().getJobTemplate().getSpec().getActiveDeadlineSeconds());
        serverAdd.getServer().setBackoffLimit(cronJob.getSpec().getJobTemplate().getSpec().getBackoffLimit());
        serverAdd.getServer().setTtlSecondsAfterFinished(cronJob.getSpec().getJobTemplate().getSpec().getTtlSecondsAfterFinished());
    }

    public int getComponentSeqByLabels(Map<String, String> labels) throws Exception {
        int componentSeq = 0;
        if (MapUtils.isNotEmpty(labels) && labels.containsKey(KubeConstants.LABELS_COCKTAIL_KEY)) {
            componentSeq = StringUtils.contains(labels.get(KubeConstants.LABELS_COCKTAIL_KEY), "-")
                    ? Integer.parseInt(StringUtils.substringAfterLast(labels.get(KubeConstants.LABELS_COCKTAIL_KEY), "-"))
                    : 0;
        }

        return componentSeq;
    }

    private void convertObjectMeta(ServerGuiVO serverAdd, V1ObjectMeta objectMeta) throws Exception {

        // name
        serverAdd.getComponent().setComponentName(objectMeta.getName());

        serverAdd.getComponent().setComponentType(ComponentType.CSERVER.getCode());

        // Labels
        if (MapUtils.isNotEmpty(objectMeta.getLabels())) {
            serverAdd.getServer().setLabels(objectMeta.getLabels());
//            serverAdd.getComponent().setComponentSeq(this.getComponentSeqByLabels(objectMeta.getLabels()));
        }

        // Annotations
        if (MapUtils.isNotEmpty(objectMeta.getAnnotations())) {
            serverAdd.getServer().setAnnotations(objectMeta.getAnnotations());
            // Multi-nic
            if (MapUtils.getString(objectMeta.getAnnotations(), KubeConstants.META_ANNOTATIONS_CNI_NETWORKS, null) != null) {
                serverAdd.getServer().setPodNetworks(ObjectMapperUtils.getMapper().readValue(objectMeta.getAnnotations().get(KubeConstants.META_ANNOTATIONS_CNI_NETWORKS), new TypeReference<List<Map<String, String>>>(){}));
            }
            // group seq
            Integer workloadGroupSeq = MapUtils.getInteger(objectMeta.getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, null);
            if (workloadGroupSeq != null) {
                serverAdd.getComponent().setWorkloadGroupSeq(workloadGroupSeq);
            }
        }

    }

    private void convertPodTemplate(ServerGuiVO serverAdd, V1PodTemplateSpec podTemplateSpec) throws Exception {

        JSON k8sJson = new JSON();

        // restartPolicy
        serverAdd.getServer().setRestartPolicy(RestartPolicyType.valueOf(podTemplateSpec.getSpec().getRestartPolicy()));

        // terminationGracePeriodSeconds
        serverAdd.getServer().setTerminationGracePeriodSeconds(podTemplateSpec.getSpec().getTerminationGracePeriodSeconds());

        // serviceAccountName
        if (StringUtils.isNotBlank(podTemplateSpec.getSpec().getServiceAccountName())) {
            serverAdd.getServer().setServiceAccountName(podTemplateSpec.getSpec().getServiceAccountName());
        }

        // nodeSelectorKey, nodeSelectorValue
        if (MapUtils.isNotEmpty(podTemplateSpec.getSpec().getNodeSelector())) {
            for (Map.Entry<String, String> entryRow : podTemplateSpec.getSpec().getNodeSelector().entrySet()) {
                serverAdd.getServer().setNodeSelectorKey(entryRow.getKey());
                serverAdd.getServer().setNodeSelectorValue(entryRow.getValue());
                break;
            }
        }

        // tolerations
        if (CollectionUtils.isNotEmpty(podTemplateSpec.getSpec().getTolerations())) {
            serverAdd.getServer().setTolerations(Lists.newArrayList());
            for (V1Toleration tolerationRow : podTemplateSpec.getSpec().getTolerations()) {
                serverAdd.getServer().getTolerations().add(k8sJson.deserialize(k8sJson.serialize(tolerationRow), TolerationVO.class));
            }
        }

        // Affinity
        if (podTemplateSpec.getSpec().getAffinity() != null) {
            AffinityVO affinity = new AffinityVO();
            // Node Affinity
            if (podTemplateSpec.getSpec().getAffinity().getNodeAffinity() != null) {
                affinity.setNodeAffinity(k8sJson.deserialize(k8sJson.serialize(podTemplateSpec.getSpec().getAffinity().getNodeAffinity()), NodeAffinityVO.class));
            }
            // Pod Affinity
            if (podTemplateSpec.getSpec().getAffinity().getPodAffinity() != null) {
                affinity.setPodAffinity(k8sJson.deserialize(k8sJson.serialize(podTemplateSpec.getSpec().getAffinity().getPodAffinity()), PodAffinityVO.class));
            }
            // Pod Anti Affinity
            if (podTemplateSpec.getSpec().getAffinity().getPodAntiAffinity() != null) {
                affinity.setPodAntiAffinity(k8sJson.deserialize(k8sJson.serialize(podTemplateSpec.getSpec().getAffinity().getPodAntiAffinity()), PodAntiAffinityVO.class));
            }
            serverAdd.getServer().setAffinity(affinity);
        }

        // ImagePullSecret
        if (CollectionUtils.isNotEmpty(podTemplateSpec.getSpec().getImagePullSecrets())) {
//            serverAdd.getServer().setImageSecret(podTemplateSpec.getSpec().getImagePullSecrets().get(0).getName());
            serverAdd.getServer().setImagePullSecrets(k8sJson.deserialize(k8sJson.serialize(podTemplateSpec.getSpec().getImagePullSecrets()), new TypeToken<List<LocalObjectReferenceVO>>(){}.getType()));
        }

        // hostname
        if (StringUtils.isNotBlank(podTemplateSpec.getSpec().getHostname())) {
            serverAdd.getServer().setHostname(podTemplateSpec.getSpec().getHostname());
        }

        // Volume
        Map<String, V1Volume> volumeMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(podTemplateSpec.getSpec().getVolumes())) {
            serverAdd.setVolumes(new ArrayList<>());
            for (V1Volume volumeRow : podTemplateSpec.getSpec().getVolumes()) {

                ContainerVolumeVO containerVolume = new ContainerVolumeVO();
                containerVolume.setVolumeName(volumeRow.getName());
                containerVolume.setUseMemoryYn("N");

                if (volumeRow.getEmptyDir() != null) {
                    containerVolume.setVolumeType(VolumeType.EMPTY_DIR);
                    if (StringUtils.isNotBlank(volumeRow.getEmptyDir().getMedium())) {
                        if (StringUtils.equals(volumeRow.getEmptyDir().getMedium(), "Memory")) {
                            containerVolume.setUseMemoryYn("Y");
                        }
                        containerVolume.setEmptyDirMedium(volumeRow.getEmptyDir().getMedium());
                    }
                    if (volumeRow.getEmptyDir().getSizeLimit() != null) {
                        containerVolume.setSizeLimit(volumeRow.getEmptyDir().getSizeLimit().toSuffixedString());
                    }

                } else if (volumeRow.getHostPath() != null) {

                    containerVolume.setVolumeType(VolumeType.HOST_PATH);
                    containerVolume.setHostPath(volumeRow.getHostPath().getPath());
                    containerVolume.setHostPathType(volumeRow.getHostPath().getType());

                } else if (volumeRow.getConfigMap() != null) {

                    containerVolume.setVolumeType(VolumeType.CONFIG_MAP);
                    containerVolume.setConfigMapName(volumeRow.getConfigMap().getName());
                    if (volumeRow.getConfigMap().getDefaultMode() != null) {
                        containerVolume.setDefaultMode(Integer.parseInt(Integer.toOctalString(volumeRow.getConfigMap().getDefaultMode()))); // 10진수 -> 8진수
                    } else {
                        // 기본값 : 420
                        containerVolume.setDefaultMode(Integer.parseInt(Integer.toOctalString(420))); // 10진수 -> 8진수
                    }
                    containerVolume.setOptional(volumeRow.getConfigMap().getOptional());
                    if (CollectionUtils.isNotEmpty(volumeRow.getConfigMap().getItems())) {
                        containerVolume.setItems(new ArrayList<>());
                        for (V1KeyToPath keyToPathRow : volumeRow.getConfigMap().getItems()) {
                            ContainerVolumeKeyToPathVO keyToPath = new ContainerVolumeKeyToPathVO();
                            if (keyToPathRow.getMode() != null) {
                                keyToPath.setMode(Integer.parseInt(Integer.toOctalString(keyToPathRow.getMode()))); // 10진수 -> 8진수
                            }
                            keyToPath.setKey(keyToPathRow.getKey());
                            keyToPath.setPath(keyToPathRow.getPath());
                            containerVolume.getItems().add(keyToPath);
                        }
                    }

                } else if (volumeRow.getSecret() != null) {

                    containerVolume.setVolumeType(VolumeType.SECRET);
                    containerVolume.setSecretName(volumeRow.getSecret().getSecretName());
                    if (volumeRow.getSecret().getDefaultMode() != null) {
                        containerVolume.setDefaultMode(Integer.parseInt(Integer.toOctalString(volumeRow.getSecret().getDefaultMode()))); // 10진수 -> 8진수
                    } else {
                        // 기본값 : 420
                        containerVolume.setDefaultMode(Integer.parseInt(Integer.toOctalString(420))); // 10진수 -> 8진수
                    }
                    containerVolume.setOptional(volumeRow.getSecret().getOptional());
                    if (CollectionUtils.isNotEmpty(volumeRow.getSecret().getItems())) {
                        containerVolume.setItems(new ArrayList<>());
                        for (V1KeyToPath keyToPathRow : volumeRow.getSecret().getItems()) {
                            ContainerVolumeKeyToPathVO keyToPath = new ContainerVolumeKeyToPathVO();
                            if (keyToPathRow.getMode() != null) {
                                keyToPath.setMode(Integer.parseInt(Integer.toOctalString(keyToPathRow.getMode()))); // 10진수 -> 8진수
                            }
                            keyToPath.setKey(keyToPathRow.getKey());
                            keyToPath.setPath(keyToPathRow.getPath());
                            containerVolume.getItems().add(keyToPath);
                        }
                    }

                } else if (volumeRow.getPersistentVolumeClaim() != null) {

                    containerVolume.setVolumeType(VolumeType.PERSISTENT_VOLUME_LINKED);
                    containerVolume.setPersistentVolumeClaimName(volumeRow.getPersistentVolumeClaim().getClaimName());
                    containerVolume.setReadOnly(volumeRow.getPersistentVolumeClaim().getReadOnly());

                }

                // cocktail GUI에서 지원하는 타입만 셋팅
                if (containerVolume.getVolumeType() != null) {
                    serverAdd.getVolumes().add(containerVolume);
                    volumeMap.put(volumeRow.getName(), volumeRow);
                }
            }
        }

        // VolumeTemplate - statefulSet
        Map<String, PersistentVolumeClaimGuiVO> volumeTemplateMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(serverAdd.getVolumeTemplates())) {
            for (PersistentVolumeClaimGuiVO volumeTemplateRow : serverAdd.getVolumeTemplates()) {
                volumeTemplateMap.put(volumeTemplateRow.getName(), volumeTemplateRow);
            }
        }

        // InitContainer
        if (CollectionUtils.isNotEmpty(podTemplateSpec.getSpec().getInitContainers())) {
            serverAdd.setInitContainers(this.convertContainer(volumeMap, volumeTemplateMap, podTemplateSpec.getSpec().getInitContainers(), true, k8sJson));
        }

        // Container
        if (CollectionUtils.isNotEmpty(podTemplateSpec.getSpec().getContainers())) {
            serverAdd.setContainers(this.convertContainer(volumeMap, volumeTemplateMap, podTemplateSpec.getSpec().getContainers(), false, k8sJson));
        }
    }

    private List<ContainerVO> convertContainer(Map<String, V1Volume> volumeMap, Map<String, PersistentVolumeClaimGuiVO> volumeTemplateMap, List<V1Container> v1Containers, boolean isInitContainer, JSON k8sJson) throws Exception {

        List<ContainerVO> containers = new ArrayList<>();
        // registry url
        String registryUrl = ResourceUtil.getRegistryUrl();
//        String registryUrl = "regi.acloud.run"; // for test

        for (V1Container containerRow : v1Containers) {
            ContainerVO container = new ContainerVO();
            container.setContainerName(containerRow.getName());
            if (isInitContainer) {
                container.setInitContainerYn("Y");
            }

            // image 정보 셋팅
            // - 콜론이 없다면 latest로 셋팅
            if (StringUtils.indexOf(containerRow.getImage(), ":") > 0) {
                container.setFullImageName(containerRow.getImage());
            } else {
                container.setFullImageName(String.format("%s:latest", containerRow.getImage()));
            }

            // image pull policy
            container.setImagePullPolicy(containerRow.getImagePullPolicy());

            // registry 사용시 PrivateRegistryYn 셋팅
            if (StringUtils.startsWith(containerRow.getImage(), registryUrl)) {
                container.setPrivateRegistryYn("Y");
            } else {
                container.setPrivateRegistryYn("N");
            }

            // Cmds
            if (CollectionUtils.isNotEmpty(containerRow.getCommand())) {
                container.setCmds(containerRow.getCommand());
            }
            // Args
            if (CollectionUtils.isNotEmpty(containerRow.getArgs())) {
                container.setArgs(containerRow.getArgs());
            }

            // ports
            if (CollectionUtils.isNotEmpty(containerRow.getPorts())) {
                container.setContainerPorts(Lists.newArrayList());

                for (V1ContainerPort containerPortRow : containerRow.getPorts()) {
                    ContainerPortVO containerPort = new ContainerPortVO();
                    containerPort.setName(containerPortRow.getName());
                    containerPort.setContainerPort(containerPortRow.getContainerPort());
                    containerPort.setHostPort(containerPortRow.getHostPort());
                    containerPort.setProtocol(containerPortRow.getProtocol());
                    containerPort.setHostIP(containerPortRow.getHostIP());
                    container.getContainerPorts().add(containerPort);
                }
            }

            // Env
            if (CollectionUtils.isNotEmpty(containerRow.getEnv())) {
                container.setContainerEnvVars(Lists.newArrayList());

                for (V1EnvVar envVarRow : containerRow.getEnv()) {
                    ContainerEnvVarVO envVar = new ContainerEnvVarVO();
                    envVar.setKey(envVarRow.getName());

                    if (StringUtils.isNotBlank(envVarRow.getValue())) {
                        envVar.setValue(envVarRow.getValue());
                    } else {
                        if (envVarRow.getValueFrom() != null) {
                            if (envVarRow.getValueFrom().getConfigMapKeyRef() != null) {
                                envVar.setConfigMapName(envVarRow.getValueFrom().getConfigMapKeyRef().getName());
                                envVar.setConfigMapKey(envVarRow.getValueFrom().getConfigMapKeyRef().getKey());
                            } else if (envVarRow.getValueFrom().getSecretKeyRef() != null) {
                                envVar.setSecretName(envVarRow.getValueFrom().getSecretKeyRef().getName());
                                envVar.setSecretKey(envVarRow.getValueFrom().getSecretKeyRef().getKey());
                            } else if (envVarRow.getValueFrom().getFieldRef() != null) {
                                K8sObjectFieldSelectorVO fieldRef = new K8sObjectFieldSelectorVO();
                                fieldRef.setApiVersion(envVarRow.getValueFrom().getFieldRef().getApiVersion());
                                fieldRef.setFieldPath(envVarRow.getValueFrom().getFieldRef().getFieldPath());
                                envVar.setFieldRef(fieldRef);
                            } else if (envVarRow.getValueFrom().getResourceFieldRef() != null) {
                                K8sResourceFieldSelectorVO resourceFieldRef = new K8sResourceFieldSelectorVO();
                                if (StringUtils.isNotBlank(envVarRow.getValueFrom().getResourceFieldRef().getContainerName())) {
                                    resourceFieldRef.setContainerName(envVarRow.getValueFrom().getResourceFieldRef().getContainerName());
                                }
                                if (envVarRow.getValueFrom().getResourceFieldRef().getDivisor() != null) {
                                    resourceFieldRef.setDivisor(envVarRow.getValueFrom().getResourceFieldRef().getDivisor().toSuffixedString());
                                }
                                resourceFieldRef.setResource(envVarRow.getValueFrom().getResourceFieldRef().getResource());
                                envVar.setResourceFieldRef(resourceFieldRef);
                            }
                        }
                    }
                    container.getContainerEnvVars().add(envVar);
                }
            }

            // VolumeMount
            if (CollectionUtils.isNotEmpty(containerRow.getVolumeMounts())) {
                container.setVolumeMounts(new ArrayList<>());
                boolean isExists = false;
                for (V1VolumeMount volumeMountRow : containerRow.getVolumeMounts()) {
                    isExists = false;

                    if (MapUtils.isNotEmpty(volumeMap) && MapUtils.getObject(volumeMap, volumeMountRow.getName(), null) != null) {
                        V1Volume volume = volumeMap.get(volumeMountRow.getName());
                        if (volume.getPersistentVolumeClaim() != null
                                || volume.getEmptyDir() != null
                                || volume.getHostPath() != null
                                || volume.getSecret() != null
                                || volume.getConfigMap() != null) {
                            isExists = true;
                        }
                    } else if (MapUtils.isNotEmpty(volumeTemplateMap) && MapUtils.getObject(volumeTemplateMap, volumeMountRow.getName(), null) != null) {
                        isExists = true;
                    }

                    if (isExists) {
                        VolumeMountVO volumeMount = new VolumeMountVO();
                        volumeMount.setVolumeName(volumeMountRow.getName());
                        volumeMount.setContainerPath(volumeMountRow.getMountPath());
                        if (StringUtils.isNotBlank(volumeMountRow.getMountPropagation())) {
                            volumeMount.setMountPropagation(volumeMountRow.getMountPropagation());
                        }
                        if (StringUtils.isNotBlank(volumeMountRow.getSubPath())) {
                            volumeMount.setSubPath(volumeMountRow.getSubPath());
                        }
                        if (StringUtils.isNotBlank(volumeMountRow.getSubPathExpr())) {
                            volumeMount.setSubPathExpr(volumeMountRow.getSubPathExpr());
                        }

                        if (BooleanUtils.toBoolean(volumeMountRow.getReadOnly())) {
                            volumeMount.setReadOnlyYn("Y");
                        } else {
                            volumeMount.setReadOnlyYn("N");
                        }
                        container.getVolumeMounts().add(volumeMount);
                    }
                }
            }

            // Container SecurityContext
            if (containerRow.getSecurityContext() != null) {
                container.setSecurityContext(new SecurityContextVO());
                container.getSecurityContext().setAllowPrivilegeEscalation(containerRow.getSecurityContext().getAllowPrivilegeEscalation());
                if (containerRow.getSecurityContext().getCapabilities() != null) {
                    CapabilitiesVO capabilities = new CapabilitiesVO();
                    capabilities.setAdd(containerRow.getSecurityContext().getCapabilities().getAdd());
                    capabilities.setDrop(containerRow.getSecurityContext().getCapabilities().getDrop());
                    container.getSecurityContext().setCapabilities(capabilities);
                }
                container.getSecurityContext().setPrivileged(containerRow.getSecurityContext().getPrivileged());
                container.getSecurityContext().setProcMount(containerRow.getSecurityContext().getProcMount());
                container.getSecurityContext().setReadOnlyRootFilesystem(containerRow.getSecurityContext().getReadOnlyRootFilesystem());
                container.getSecurityContext().setRunAsGroup(containerRow.getSecurityContext().getRunAsGroup());
                container.getSecurityContext().setRunAsNonRoot(containerRow.getSecurityContext().getRunAsNonRoot());
                container.getSecurityContext().setRunAsUser(containerRow.getSecurityContext().getRunAsUser());
                if (containerRow.getSecurityContext().getSeLinuxOptions() != null) {
                    SELinuxOptionsVO seLinuxOptions = new SELinuxOptionsVO();
                    seLinuxOptions.setLevel(containerRow.getSecurityContext().getSeLinuxOptions().getLevel());
                    seLinuxOptions.setType(containerRow.getSecurityContext().getSeLinuxOptions().getType());
                    seLinuxOptions.setRole(containerRow.getSecurityContext().getSeLinuxOptions().getRole());
                    seLinuxOptions.setUser(containerRow.getSecurityContext().getSeLinuxOptions().getUser());
                    container.getSecurityContext().setSeLinuxOptions(seLinuxOptions);
                }
                if (containerRow.getSecurityContext().getWindowsOptions() != null) {
                    WindowsOptionsVO windowsOptions = new WindowsOptionsVO();
                    windowsOptions.setGmsaCredentialSpec(containerRow.getSecurityContext().getWindowsOptions().getGmsaCredentialSpec());
                    windowsOptions.setGmsaCredentialSpecName(containerRow.getSecurityContext().getWindowsOptions().getGmsaCredentialSpecName());
                    windowsOptions.setRunAsUserName(containerRow.getSecurityContext().getWindowsOptions().getRunAsUserName());
                    container.getSecurityContext().setWindowsOptions(windowsOptions);
                }
            }

            // Resources
            if (containerRow.getResources() != null) {
                container.setResources(new ContainerResourcesVO());

                // requests
                if (MapUtils.isNotEmpty(containerRow.getResources().getRequests())) {
                    container.getResources().setRequests(new ResourceVO());

                    for (Map.Entry<String, Quantity> reqEntryRow : containerRow.getResources().getRequests().entrySet()) {
                        log.debug(reqEntryRow.toString());

                        switch (reqEntryRow.getKey()) {
                            case KubeConstants.RESOURCES_CPU:
                                container.getResources().getRequests().setCpu(reqEntryRow.getValue().getNumber().doubleValue()*1000);
                                break;
                            case KubeConstants.RESOURCES_MEMORY:
                                container.getResources().getRequests().setMemory(reqEntryRow.getValue().getNumber().doubleValue()/1024/1024);
                                break;
                            case KubeConstants.RESOURCES_GPU:
                                container.getResources().getRequests().setGpu(reqEntryRow.getValue().getNumber().doubleValue());
                                container.getResources().setUseGpu(Boolean.TRUE);
                                break;
                            default:
                                if (MapUtils.isEmpty(container.getResources().getRequests().getNetwork())) {
                                    container.getResources().getRequests().setNetwork(new HashMap<>());
                                }
                                container.getResources().getRequests().getNetwork().put(reqEntryRow.getKey(), reqEntryRow.getValue().toSuffixedString());
                                break;
                        }
                    }
                }

                // limits
                if (MapUtils.isNotEmpty(containerRow.getResources().getLimits())) {
                    container.getResources().setLimits(new ResourceVO());

                    for (Map.Entry<String, Quantity> reqEntryRow : containerRow.getResources().getLimits().entrySet()) {
                        log.debug(reqEntryRow.toString());

                        switch (reqEntryRow.getKey()) {
                            case KubeConstants.RESOURCES_CPU:
                                container.getResources().getLimits().setCpu(reqEntryRow.getValue().getNumber().doubleValue()*1000);
                                break;
                            case KubeConstants.RESOURCES_MEMORY:
                                container.getResources().getLimits().setMemory(reqEntryRow.getValue().getNumber().doubleValue()/1024/1024);
                                break;
                            case KubeConstants.RESOURCES_GPU:
                                container.getResources().getLimits().setGpu(reqEntryRow.getValue().getNumber().doubleValue());
                                container.getResources().setUseGpu(Boolean.TRUE);
                                break;
                            default:
                                if (MapUtils.isEmpty(container.getResources().getLimits().getNetwork())) {
                                    container.getResources().getLimits().setNetwork(new HashMap<>());
                                }
                                container.getResources().getLimits().getNetwork().put(reqEntryRow.getKey(), reqEntryRow.getValue().toSuffixedString());
                                break;
                        }
                    }
                }
            }

            // LivenessProbe
            if (containerRow.getLivenessProbe() != null) {
                container.setLivenessProbe(this.convertProbe(containerRow.getLivenessProbe()));
            }

            // ReadinessProbe
            if (!isInitContainer && containerRow.getReadinessProbe() != null) {
                container.setReadinessProbe(this.convertProbe(containerRow.getReadinessProbe()));
            }

            // Lifecycle
            if (containerRow.getLifecycle() != null) {
                container.setLifecycle(k8sJson.deserialize(k8sJson.serialize(containerRow.getLifecycle()), ContainerLifecycleVO.class));
            }

            containers.add(container);
        }

        return containers;
    }

    private ContainerProbeVO convertProbe(V1Probe v1Probe) throws Exception {
        ContainerProbeVO probe = new ContainerProbeVO();

        if (v1Probe != null) {
            if (v1Probe.getHttpGet() != null) {
                probe.setType(ProbeType.HTTPGET);
                probe.setHttpGetScheme(v1Probe.getHttpGet().getScheme());
                if (StringUtils.isNotBlank(v1Probe.getHttpGet().getHost())) {
                    probe.setHttpGetHost(v1Probe.getHttpGet().getHost());
                }
                if (v1Probe.getHttpGet().getPort() != null) {
                    if (v1Probe.getHttpGet().getPort().isInteger()) {
                        probe.setHttpGetPort(String.valueOf(v1Probe.getHttpGet().getPort().getIntValue().intValue()));
                    } else {
                        probe.setHttpGetPort(v1Probe.getHttpGet().getPort().getStrValue());
                    }
                }
                if (StringUtils.isNotBlank(v1Probe.getHttpGet().getPath())) {
                    probe.setHttpGetPath(v1Probe.getHttpGet().getPath());
                }
                if (CollectionUtils.isNotEmpty(v1Probe.getHttpGet().getHttpHeaders())) {
                    List<HTTPHeaderVO> httpHeaders = Lists.newArrayList();
                    for (V1HTTPHeader httpHeaderRow : v1Probe.getHttpGet().getHttpHeaders()) {
                        HTTPHeaderVO httpHeader = new HTTPHeaderVO();
                        httpHeader.setName(httpHeaderRow.getName());
                        httpHeader.setValue(httpHeaderRow.getValue());
                        httpHeaders.add(httpHeader);
                    }
                    probe.setHttpGetHeaders(httpHeaders);
                }
            } else if (v1Probe.getTcpSocket() != null) {
                probe.setType(ProbeType.TCPSOCKET);
                if (v1Probe.getTcpSocket().getPort() != null) {
                    if (v1Probe.getTcpSocket().getPort().isInteger()) {
                        probe.setTcpSocketPort(String.valueOf(v1Probe.getTcpSocket().getPort().getIntValue().intValue()));
                    } else {
                        probe.setTcpSocketPort(v1Probe.getTcpSocket().getPort().getStrValue());
                    }
                }
            } else if (v1Probe.getExec() != null) {
                probe.setType(ProbeType.EXEC);
                if (CollectionUtils.isNotEmpty(v1Probe.getExec().getCommand())) {
                    List<String> replaceNewlineCmd = v1Probe.getExec().getCommand().stream().map(c -> (StringUtils.replaceAll(c, "\n", " "))).collect(Collectors.toList());
                    probe.setExecCommand(StringUtils.join(replaceNewlineCmd, '\n'));
                    probe.setExecCmds(v1Probe.getExec().getCommand());
                }
            }

            probe.setInitialDelaySeconds(v1Probe.getInitialDelaySeconds());
            probe.setPeriodSeconds(v1Probe.getPeriodSeconds());
            probe.setSuccessThreshold(v1Probe.getSuccessThreshold());
            probe.setTimeoutSeconds(v1Probe.getTimeoutSeconds());
            probe.setFailureThreshold(v1Probe.getFailureThreshold());
        }

        return probe;
    }

    public ServerDetailVO convertToServerDetail(ServerGuiVO serverParam) {
        serverParam.getServer().setActiveCount(Optional.ofNullable(serverParam.getServer().getComputeTotal()).orElseGet(() ->0));
        serverParam.getServer().setInitContainers(serverParam.getInitContainers());
        serverParam.getServer().setContainers(serverParam.getContainers());

        ServerDetailVO serverData = new ServerDetailVO();
        serverData.setComponentSeq(serverParam.getComponent().getComponentSeq());
        ComponentVO component = serverParam.getComponent();
        component.setComponentType(ComponentType.CSERVER.getCode());
        component.setDescription(component.getDescription());

        serverData.setComponent(component);
        serverData.setServer(serverParam.getServer());
        serverData.setVolumes(serverParam.getVolumes());
        serverData.setServices(serverParam.getServices());
        serverData.setVolumeTemplates(serverParam.getVolumeTemplates());

        return serverData;
    }

    public ServerDetailVO convertToServerDetailOld(ServerGuiVO serverParam) {
        serverParam.getServer().setActiveCount(Optional.ofNullable(serverParam.getServer().getComputeTotal()).orElseGet(() ->0));
        serverParam.getServer().setInitContainers(serverParam.getInitContainers());
        serverParam.getServer().setContainers(serverParam.getContainers());

        ServerDetailVO serverData = new ServerDetailVO();
        serverData.setComponentSeq(serverParam.getComponent().getComponentSeq());
        ComponentVO component = serverParam.getComponent();
//        if (StringUtils.isNoneEmpty(component.getComponentName())) {
//            component.setComponentName(component.getComponentName());
//        }
        component.setComponentType(ComponentType.CSERVER.getCode());
//        component.setComponentSeq(serverParam.getComponent().getComponentSeq());
//        component.setDescription(component.getDescription());

        serverData.setComponent(component);
        serverData.setServer(serverParam.getServer());
        serverData.setVolumes(serverParam.getVolumes());
        serverData.setServices(serverParam.getServices());
        serverData.setVolumeTemplates(serverParam.getVolumeTemplates());

        return serverData;
    }

    /**
     * 현재 배포된 리소스에 gui에서 수정된 것을 merge함
     *
     * @param cluster
     * @param namespace
     * @param serverGui
     * @return
     * @throws Exception
     */
    public String mergeWorkload(ClusterVO cluster, String namespace, ServerGuiVO serverGui, ServerYamlVO serverYaml) throws Exception {

        StringBuffer yamlStr = new StringBuffer();

//        V1Service currService = null;
        V1Deployment currDeployment = null;
        V2HorizontalPodAutoscaler currHpaV2 = null;
        V2beta2HorizontalPodAutoscaler currHpaV2beta2 = null;
        V2beta1HorizontalPodAutoscaler currHpaV2beta1 = null;
        V1HorizontalPodAutoscaler currHpaV1 = null;
        V1DaemonSet currDaemonSet = null;
        V1Job currJob = null;
        V1beta1CronJob currCronJobV1beta1 = null;
        V1CronJob currCronJobV1 = null;
        V1StatefulSet currStatefulSet = null;

        K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);

        if (serverYaml != null && StringUtils.isNotBlank(serverYaml.getYaml())) {
            JSON k8sJson = new JSON();
            List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());

            if (CollectionUtils.isNotEmpty(objs)) {
                for (Object obj : objs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                    switch (kind) {
                        case DEPLOYMENT:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                currDeployment = (V1Deployment) obj;
                            }
                            break;
                        case HORIZONTAL_POD_AUTOSCALER:
                            if (K8sApiType.V2 == hpaType.getApiType()) {
                                currHpaV2 = (V2HorizontalPodAutoscaler) obj;
                            } else if (K8sApiType.V2BETA2 == hpaType.getApiType()) {
                                currHpaV2beta2 = (V2beta2HorizontalPodAutoscaler) obj;
                            } else if (K8sApiType.V2BETA1 == hpaType.getApiType()) {
                                currHpaV2beta1 = (V2beta1HorizontalPodAutoscaler) obj;
                            } else if (K8sApiType.V1 == hpaType.getApiType()) {
                                currHpaV1 = (V1HorizontalPodAutoscaler) obj;
                            }
                            break;
                        case DAEMON_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                currDaemonSet = (V1DaemonSet) obj;
                            }
                            break;
                        case JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                currJob = (V1Job) obj;
                            }
                            break;
                        case CRON_JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                currCronJobV1beta1 = (V1beta1CronJob) obj;
                            } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                currCronJobV1 = (V1CronJob) obj;
                            }
                            break;
                        case STATEFUL_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                currStatefulSet = (V1StatefulSet) obj;
                            }
                            break;
                    }
                }
            }

        }


        if (StringUtils.isNotBlank(serverGui.getServer().getWorkloadType())
                && StringUtils.isNotBlank(serverGui.getServer().getWorkloadVersion())) {

            WorkloadType workloadType = WorkloadType.valueOf(serverGui.getServer().getWorkloadType());
            WorkloadVersion workloadVersion = WorkloadVersion.valueOf(serverGui.getServer().getWorkloadVersion());

            ServerDetailVO serverDetail = this.convertToServerDetail(serverGui);
            K8sApiVerKindType k8sApiVerKindType = null;

            /**
             * Deployment
             */
            if (workloadType == WorkloadType.SINGLE_SERVER || workloadType == WorkloadType.REPLICA_SERVER) {
                k8sApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);

                // Deployment
                if (k8sApiVerKindType != null) {
                    if (k8sApiVerKindType.getGroupType() == K8sApiGroupType.APPS && k8sApiVerKindType.getApiType() == K8sApiType.V1) {
                        if (currDeployment != null) {
                            V1Deployment updatedDeployment = K8sSpecFactory.buildDeploymentV1(serverDetail, namespace);

                            if (updatedDeployment != null) {
                                /**
                                 * metadata
                                 */
                                if (currDeployment.getMetadata() != null && updatedDeployment.getMetadata() != null) {
                                    currDeployment.getMetadata().setAnnotations(updatedDeployment.getMetadata().getAnnotations());
                                    currDeployment.getMetadata().setLabels(updatedDeployment.getMetadata().getLabels());
                                }

                                /**
                                 * spec
                                 */
                                if (currDeployment.getSpec() != null && currDeployment.getSpec() != null) {
                                    currDeployment.getSpec().setStrategy(updatedDeployment.getSpec().getStrategy());
                                    currDeployment.getSpec().setMinReadySeconds(updatedDeployment.getSpec().getMinReadySeconds());
                                    currDeployment.getSpec().setReplicas(updatedDeployment.getSpec().getReplicas());

                                    /**
                                     * spec.podTemplate
                                     */
                                    this.mergePodTemplate(currDeployment.getSpec().getTemplate(), updatedDeployment.getSpec().getTemplate());
                                }
                            }

                            currDeployment.setStatus(null);
                            yamlStr.append(ServerUtils.marshalYaml(currDeployment));
                        }
                    }
                }

            }
            /**
             * StatefulSet
             */
            else if (workloadType == WorkloadType.STATEFUL_SET_SERVER) {
                k8sApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);

                // StatefulSet
                if (k8sApiVerKindType != null) {
                    if (k8sApiVerKindType.getGroupType() == K8sApiGroupType.APPS && k8sApiVerKindType.getApiType() == K8sApiType.V1) {
                        if (currStatefulSet != null) {
                            V1StatefulSet updatedStatefulSet = K8sSpecFactory.buildStatefulSetV1(serverDetail, namespace);

                            if (updatedStatefulSet != null) {
                                /**
                                 * metadata
                                 */
                                if (currStatefulSet.getMetadata() != null && updatedStatefulSet.getMetadata() != null) {
                                    currStatefulSet.getMetadata().setLabels(updatedStatefulSet.getMetadata().getLabels());
                                    currStatefulSet.getMetadata().setAnnotations(updatedStatefulSet.getMetadata().getAnnotations());
                                }

                                /**
                                 * spec
                                 */
                                if (currStatefulSet.getSpec() != null && updatedStatefulSet.getSpec() != null) {
                                    currStatefulSet.getSpec().setPodManagementPolicy(updatedStatefulSet.getSpec().getPodManagementPolicy());
                                    currStatefulSet.getSpec().setReplicas(updatedStatefulSet.getSpec().getReplicas());
                                    currStatefulSet.getSpec().setUpdateStrategy(updatedStatefulSet.getSpec().getUpdateStrategy());
                                    currStatefulSet.getSpec().setVolumeClaimTemplates(updatedStatefulSet.getSpec().getVolumeClaimTemplates());

                                    /**
                                     * spec.podTemplate
                                     */
                                    this.mergePodTemplate(currStatefulSet.getSpec().getTemplate(), updatedStatefulSet.getSpec().getTemplate(), currStatefulSet.getSpec().getVolumeClaimTemplates());
                                }
                            }

                            currStatefulSet.setStatus(null);
                            if (yamlStr != null && StringUtils.isNotBlank(yamlStr.toString())) {
                                yamlStr.append("---\n");
                            }
                            yamlStr.append(ServerUtils.marshalYaml(currStatefulSet));
                        }
                    }
                }

            }
            /**
             * DaemonSet
             */
            else if (workloadType == WorkloadType.DAEMON_SET_SERVER) {
                k8sApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);

                // DaemonSet
                if (k8sApiVerKindType != null) {
                    if (k8sApiVerKindType.getGroupType() == K8sApiGroupType.APPS && k8sApiVerKindType.getApiType() == K8sApiType.V1) {
                        if (currDaemonSet != null) {
                            V1DaemonSet updatedDaemonSet = K8sSpecFactory.buildDaemonSetV1(serverDetail, namespace);

                            if (updatedDaemonSet != null) {
                                /**
                                 * metadata
                                 */
                                if (currDaemonSet.getMetadata() != null && updatedDaemonSet.getMetadata() != null) {
                                    currDaemonSet.getMetadata().setAnnotations(updatedDaemonSet.getMetadata().getAnnotations());
                                    currDaemonSet.getMetadata().setLabels(updatedDaemonSet.getMetadata().getLabels());
                                }

                                /**
                                 * spec
                                 */
                                if (currDaemonSet.getSpec() != null && updatedDaemonSet.getSpec() != null) {
                                    currDaemonSet.getSpec().setMinReadySeconds(updatedDaemonSet.getSpec().getMinReadySeconds());
                                    currDaemonSet.getSpec().setRevisionHistoryLimit(updatedDaemonSet.getSpec().getRevisionHistoryLimit());
                                    currDaemonSet.getSpec().setUpdateStrategy(updatedDaemonSet.getSpec().getUpdateStrategy());

                                    /**
                                     * spec.podTemplate
                                     */
                                    this.mergePodTemplate(currDaemonSet.getSpec().getTemplate(), updatedDaemonSet.getSpec().getTemplate());
                                }
                            }

                            currDaemonSet.setStatus(null);
                            yamlStr.append(ServerUtils.marshalYaml(currDaemonSet));
                        }
                    }
                }
            }
            /**
             * Job
             */
            else if (workloadType == WorkloadType.JOB_SERVER) {
                k8sApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);

                // Job
                if (k8sApiVerKindType != null) {
                    if (k8sApiVerKindType.getGroupType() == K8sApiGroupType.BATCH && k8sApiVerKindType.getApiType() == K8sApiType.V1) {
                        if (currJob != null) {
                            V1Job updatedJob = K8sSpecFactory.buildJobV1(serverDetail, namespace);

                            if (updatedJob != null) {
                                /**
                                 * metadata
                                 */
                                if (currJob.getMetadata() != null && updatedJob.getMetadata() != null) {
                                    currJob.getMetadata().setAnnotations(updatedJob.getMetadata().getAnnotations());
                                    currJob.getMetadata().setLabels(updatedJob.getMetadata().getLabels());
                                }

                                /**
                                 * spec
                                 */
                                if (currJob.getSpec() != null && updatedJob.getSpec() != null) {
                                    currJob.getSpec().setActiveDeadlineSeconds(updatedJob.getSpec().getActiveDeadlineSeconds());
                                    currJob.getSpec().setBackoffLimit(updatedJob.getSpec().getBackoffLimit());
                                    currJob.getSpec().setCompletions(updatedJob.getSpec().getCompletions());
                                    currJob.getSpec().setParallelism(updatedJob.getSpec().getParallelism());
                                    currJob.getSpec().setTtlSecondsAfterFinished(updatedJob.getSpec().getTtlSecondsAfterFinished());

                                    /**
                                     * spec.podTemplate
                                     */
                                    this.mergePodTemplate(currJob.getSpec().getTemplate(), updatedJob.getSpec().getTemplate());
                                }
                            }

                            currJob.setStatus(null);
                            yamlStr.append(ServerUtils.marshalYaml(currJob));
                        }
                    }
                }

            }
            /**
             * CronJob
             */
            else if (workloadType == WorkloadType.CRON_JOB_SERVER) {
                k8sApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

                // CronJob
                if (k8sApiVerKindType != null) {
                    if (k8sApiVerKindType.getGroupType() == K8sApiGroupType.BATCH && k8sApiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                        if (currCronJobV1beta1 != null) {
                            V1beta1CronJob updatedCronJob = K8sSpecFactory.buildCronJobV1beta1(serverDetail, namespace);

                            if (updatedCronJob != null) {
                                /**
                                 * metadata
                                 */
                                if (currCronJobV1beta1.getMetadata() != null && updatedCronJob.getMetadata() != null) {
                                    currCronJobV1beta1.getMetadata().setAnnotations(updatedCronJob.getMetadata().getAnnotations());
                                    currCronJobV1beta1.getMetadata().setLabels(updatedCronJob.getMetadata().getLabels());
                                }

                                /**
                                 * spec
                                 */
                                if (currCronJobV1beta1.getSpec() != null && updatedCronJob.getSpec() != null) {
                                    currCronJobV1beta1.getSpec().setConcurrencyPolicy(updatedCronJob.getSpec().getConcurrencyPolicy());
                                    currCronJobV1beta1.getSpec().setFailedJobsHistoryLimit(updatedCronJob.getSpec().getFailedJobsHistoryLimit());
                                    currCronJobV1beta1.getSpec().setSchedule(updatedCronJob.getSpec().getSchedule());
                                    currCronJobV1beta1.getSpec().setStartingDeadlineSeconds(updatedCronJob.getSpec().getStartingDeadlineSeconds());
                                    currCronJobV1beta1.getSpec().setSuccessfulJobsHistoryLimit(updatedCronJob.getSpec().getSuccessfulJobsHistoryLimit());
                                    currCronJobV1beta1.getSpec().setSuspend(updatedCronJob.getSpec().getSuspend());

                                    /**
                                     * spec.jobTemplate
                                     */
                                    if (
                                            currCronJobV1beta1.getSpec().getJobTemplate() != null && updatedCronJob.getSpec().getJobTemplate() != null
                                                    && currCronJobV1beta1.getSpec().getJobTemplate().getSpec() != null && updatedCronJob.getSpec().getJobTemplate().getSpec() != null
                                    ) {
                                        currCronJobV1beta1.getSpec().getJobTemplate().getSpec().setActiveDeadlineSeconds(updatedCronJob.getSpec().getJobTemplate().getSpec().getActiveDeadlineSeconds());
                                        currCronJobV1beta1.getSpec().getJobTemplate().getSpec().setBackoffLimit(updatedCronJob.getSpec().getJobTemplate().getSpec().getBackoffLimit());
                                        currCronJobV1beta1.getSpec().getJobTemplate().getSpec().setCompletions(updatedCronJob.getSpec().getJobTemplate().getSpec().getCompletions());
                                        currCronJobV1beta1.getSpec().getJobTemplate().getSpec().setParallelism(updatedCronJob.getSpec().getJobTemplate().getSpec().getParallelism());
                                        currCronJobV1beta1.getSpec().getJobTemplate().getSpec().setTtlSecondsAfterFinished(updatedCronJob.getSpec().getJobTemplate().getSpec().getTtlSecondsAfterFinished());

                                        /**
                                         * spec.jobTemplate.spec.podTemplate
                                         */
                                        this.mergePodTemplate(currCronJobV1beta1.getSpec().getJobTemplate().getSpec().getTemplate(), updatedCronJob.getSpec().getJobTemplate().getSpec().getTemplate());
                                    }
                                }
                            }

                            currCronJobV1beta1.setStatus(null);
                            yamlStr.append(ServerUtils.marshalYaml(currCronJobV1beta1));
                        }
                    } else {
                        if (currCronJobV1 != null) {
                            V1CronJob updatedCronJob = K8sSpecFactory.buildCronJobV1(serverDetail, namespace);

                            if (updatedCronJob != null) {
                                /**
                                 * metadata
                                 */
                                if (currCronJobV1.getMetadata() != null && updatedCronJob.getMetadata() != null) {
                                    currCronJobV1.getMetadata().setAnnotations(updatedCronJob.getMetadata().getAnnotations());
                                    currCronJobV1.getMetadata().setLabels(updatedCronJob.getMetadata().getLabels());
                                }

                                /**
                                 * spec
                                 */
                                if (currCronJobV1.getSpec() != null && updatedCronJob.getSpec() != null) {
                                    currCronJobV1.getSpec().setConcurrencyPolicy(updatedCronJob.getSpec().getConcurrencyPolicy());
                                    currCronJobV1.getSpec().setFailedJobsHistoryLimit(updatedCronJob.getSpec().getFailedJobsHistoryLimit());
                                    currCronJobV1.getSpec().setSchedule(updatedCronJob.getSpec().getSchedule());
                                    currCronJobV1.getSpec().setStartingDeadlineSeconds(updatedCronJob.getSpec().getStartingDeadlineSeconds());
                                    currCronJobV1.getSpec().setSuccessfulJobsHistoryLimit(updatedCronJob.getSpec().getSuccessfulJobsHistoryLimit());
                                    currCronJobV1.getSpec().setSuspend(updatedCronJob.getSpec().getSuspend());

                                    /**
                                     * spec.jobTemplate
                                     */
                                    if (
                                            currCronJobV1.getSpec().getJobTemplate() != null && updatedCronJob.getSpec().getJobTemplate() != null
                                                    && currCronJobV1.getSpec().getJobTemplate().getSpec() != null && updatedCronJob.getSpec().getJobTemplate().getSpec() != null
                                    ) {
                                        currCronJobV1.getSpec().getJobTemplate().getSpec().setActiveDeadlineSeconds(updatedCronJob.getSpec().getJobTemplate().getSpec().getActiveDeadlineSeconds());
                                        currCronJobV1.getSpec().getJobTemplate().getSpec().setBackoffLimit(updatedCronJob.getSpec().getJobTemplate().getSpec().getBackoffLimit());
                                        currCronJobV1.getSpec().getJobTemplate().getSpec().setCompletions(updatedCronJob.getSpec().getJobTemplate().getSpec().getCompletions());
                                        currCronJobV1.getSpec().getJobTemplate().getSpec().setParallelism(updatedCronJob.getSpec().getJobTemplate().getSpec().getParallelism());
                                        currCronJobV1.getSpec().getJobTemplate().getSpec().setTtlSecondsAfterFinished(updatedCronJob.getSpec().getJobTemplate().getSpec().getTtlSecondsAfterFinished());

                                        /**
                                         * spec.jobTemplate.spec.podTemplate
                                         */
                                        this.mergePodTemplate(currCronJobV1.getSpec().getJobTemplate().getSpec().getTemplate(), updatedCronJob.getSpec().getJobTemplate().getSpec().getTemplate());
                                    }
                                }
                            }

                            currCronJobV1.setStatus(null);
                            yamlStr.append(ServerUtils.marshalYaml(currCronJobV1));
                        }
                    }
                }


            }

            if (EnumSet.of(WorkloadType.SINGLE_SERVER, WorkloadType.REPLICA_SERVER, WorkloadType.STATEFUL_SET_SERVER).contains(workloadType)) {
                if (k8sApiVerKindType == null) {
                    if (EnumSet.of(WorkloadType.SINGLE_SERVER, WorkloadType.REPLICA_SERVER).contains(workloadType)) {
                        k8sApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);
                    } else {
                        k8sApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);
                    }
                }

                if (hpaType != null) {
                    // hpa
                    if (hpaType.getGroupType() == K8sApiGroupType.AUTOSCALING) {
                        if (hpaType.getApiType() == K8sApiType.V2) {
                            if (currHpaV2 != null) {
                                if (serverGui.getServer() != null && serverGui.getServer().getHpa() != null && CollectionUtils.isNotEmpty(serverGui.getServer().getHpa().getMetrics())) {
                                    V2HorizontalPodAutoscaler updatedHpa = K8sSpecFactory.buildHpaV2(serverGui.getServer().getHpa(), namespace, serverGui.getComponent().getComponentName(), serverGui.getServer().getHpa().getName(), k8sApiVerKindType);

                                    /**
                                     * merge
                                     */
                                    this.mergeHorizontalPodAutoscalerV2(currHpaV2, updatedHpa);
                                } else {
                                    currHpaV2 = null;
                                }
                            } else {
                                if (serverGui.getServer() != null && serverGui.getServer().getHpa() != null && CollectionUtils.isNotEmpty(serverGui.getServer().getHpa().getMetrics())) {
                                    currHpaV2 = K8sSpecFactory.buildHpaV2(serverGui.getServer().getHpa(), namespace, serverGui.getComponent().getComponentName(), serverGui.getServer().getHpa().getName(), k8sApiVerKindType);
                                }
                            }

                            // merge된 값이 있다면 yaml로 변환
                            if (currHpaV2 != null) {
                                currHpaV2.setStatus(null);
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpaV2));
                            }
                        } else if (hpaType.getApiType() == K8sApiType.V2BETA2) {
                            if (currHpaV2beta2 != null) {
                                if (serverGui.getServer() != null && serverGui.getServer().getHpa() != null && CollectionUtils.isNotEmpty(serverGui.getServer().getHpa().getMetrics())) {
                                    V2beta2HorizontalPodAutoscaler updatedHpa = K8sSpecFactory.buildHpaV2beta2(serverGui.getServer().getHpa(), namespace, serverGui.getComponent().getComponentName(), serverGui.getServer().getHpa().getName(), k8sApiVerKindType);

                                    /**
                                     * merge
                                     */
                                    this.mergeHorizontalPodAutoscalerV2beta2(currHpaV2beta2, updatedHpa);
                                } else {
                                    currHpaV2beta2 = null;
                                }
                            } else {
                                if (serverGui.getServer() != null && serverGui.getServer().getHpa() != null && CollectionUtils.isNotEmpty(serverGui.getServer().getHpa().getMetrics())) {
                                    currHpaV2beta2 = K8sSpecFactory.buildHpaV2beta2(serverGui.getServer().getHpa(), namespace, serverGui.getComponent().getComponentName(), serverGui.getServer().getHpa().getName(), k8sApiVerKindType);
                                }
                            }

                            // merge된 값이 있다면 yaml로 변환
                            if (currHpaV2beta2 != null) {
                                currHpaV2beta2.setStatus(null);
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpaV2beta2));
                            }
                        } else if (hpaType.getApiType() == K8sApiType.V2BETA1) {
                            if (currHpaV2beta1 != null) {
                                if (serverGui.getServer() != null && serverGui.getServer().getHpa() != null && CollectionUtils.isNotEmpty(serverGui.getServer().getHpa().getMetrics())) {
                                    V2beta1HorizontalPodAutoscaler updatedHpa = K8sSpecFactory.buildHpaV2beta1(serverGui.getServer().getHpa(), namespace, serverGui.getComponent().getComponentName(), serverGui.getServer().getHpa().getName(), k8sApiVerKindType);

                                    /**
                                     * merge
                                     */
                                    this.mergeHorizontalPodAutoscalerV2beta1(currHpaV2beta1, updatedHpa);
                                } else {
                                    currHpaV2beta1 = null;
                                }
                            } else {
                                if (serverGui.getServer() != null && serverGui.getServer().getHpa() != null && CollectionUtils.isNotEmpty(serverGui.getServer().getHpa().getMetrics())) {
                                    currHpaV2beta1 = K8sSpecFactory.buildHpaV2beta1(serverGui.getServer().getHpa(), namespace, serverGui.getComponent().getComponentName(), serverGui.getServer().getHpa().getName(), k8sApiVerKindType);
                                }
                            }

                            // merge된 값이 있다면 yaml로 변환
                            if (currHpaV2beta1 != null) {
                                currHpaV2beta1.setStatus(null);
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpaV2beta1));
                            }
                        }
                    }
                }
            }
        }

        log.debug("clusterSeq : [{}], namespace : [{}]\nServerGuiVO : [{}]\nYamlStr : [{}]", cluster.getClusterSeq(), namespace, JsonUtils.toGson(serverGui), yamlStr.toString());

        return yamlStr.toString();
    }

    private void mergePodTemplate(V1PodTemplateSpec currPodTemplate, V1PodTemplateSpec updatedPodTemplate) throws Exception {
        this.mergePodTemplate(currPodTemplate, updatedPodTemplate, null);
    }

    private void mergePodTemplate(V1PodTemplateSpec currPodTemplate, V1PodTemplateSpec updatedPodTemplate, List<V1PersistentVolumeClaim> currVolumeClaimTemplates) throws Exception {
        if (currPodTemplate != null && updatedPodTemplate != null) {
            // TerminationGracePeriodSeconds
            currPodTemplate.getSpec().setTerminationGracePeriodSeconds(updatedPodTemplate.getSpec().getTerminationGracePeriodSeconds());
            // hostname
            currPodTemplate.getSpec().setHostname(updatedPodTemplate.getSpec().getHostname());
            // ServiceAccountName
            currPodTemplate.getSpec().setServiceAccount(updatedPodTemplate.getSpec().getServiceAccount());
            currPodTemplate.getSpec().setServiceAccountName(updatedPodTemplate.getSpec().getServiceAccountName());
            // NodeSelector
            if (MapUtils.isNotEmpty(updatedPodTemplate.getSpec().getNodeSelector())) {
                if (MapUtils.isEmpty(currPodTemplate.getSpec().getNodeSelector())) {
                    currPodTemplate.getSpec().setNodeSelector(new HashMap<>());
                }
                currPodTemplate.getSpec().getNodeSelector().putAll(updatedPodTemplate.getSpec().getNodeSelector());
            } else {
                currPodTemplate.getSpec().setNodeSelector(updatedPodTemplate.getSpec().getNodeSelector());
            }
            // toleration
            if (CollectionUtils.isNotEmpty(currPodTemplate.getSpec().getTolerations())
                    || CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getTolerations())) {
                if (CollectionUtils.isNotEmpty(currPodTemplate.getSpec().getTolerations())
                        && CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getTolerations())) {
                    // 값이 다르다면 업데이트 값으로 셋팅
                    if (!CollectionUtils.isEqualCollection(currPodTemplate.getSpec().getTolerations(), updatedPodTemplate.getSpec().getTolerations())) {
                        if (CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getTolerations())) {
                            currPodTemplate.getSpec().setTolerations(updatedPodTemplate.getSpec().getTolerations());
                        }
                    }
                }
                else {
                    if (CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getTolerations())) {
                        currPodTemplate.getSpec().setTolerations(updatedPodTemplate.getSpec().getTolerations());
                    } else {
                        currPodTemplate.getSpec().setTolerations(null);
                    }
                }
            }
            // Affinity
            if (currPodTemplate.getSpec().getAffinity() != null || updatedPodTemplate.getSpec().getAffinity() != null) {
                if (currPodTemplate.getSpec().getAffinity() != null && updatedPodTemplate.getSpec().getAffinity() != null) {
                    // 값이 다르다면 업데이트 값만 추가하여 줌
                    if (!currPodTemplate.getSpec().getAffinity().equals(updatedPodTemplate.getSpec().getAffinity())) {
                        // Node Affinity
                        if (currPodTemplate.getSpec().getAffinity().getNodeAffinity() != null || updatedPodTemplate.getSpec().getAffinity().getNodeAffinity() != null) {
                            if (currPodTemplate.getSpec().getAffinity().getNodeAffinity() != null && updatedPodTemplate.getSpec().getAffinity().getNodeAffinity() != null) {
                                if (!currPodTemplate.getSpec().getAffinity().getNodeAffinity().equals(updatedPodTemplate.getSpec().getAffinity().getNodeAffinity())) {
                                    currPodTemplate.getSpec().getAffinity().getNodeAffinity().setRequiredDuringSchedulingIgnoredDuringExecution(updatedPodTemplate.getSpec().getAffinity().getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution());
                                }
                            } else {
                                if (updatedPodTemplate.getSpec().getAffinity().getNodeAffinity() != null) {
                                    currPodTemplate.getSpec().getAffinity().setNodeAffinity(updatedPodTemplate.getSpec().getAffinity().getNodeAffinity());
                                } else {
                                    currPodTemplate.getSpec().getAffinity().setNodeAffinity(null);
                                }
                            }
                        }
                    }
                }
                else {
                    if (updatedPodTemplate.getSpec().getAffinity() != null) {
                        currPodTemplate.getSpec().setAffinity(updatedPodTemplate.getSpec().getAffinity());
                    } else {
                        if (currPodTemplate.getSpec().getAffinity() != null) {
                            // Node Affinity
                            currPodTemplate.getSpec().getAffinity().setNodeAffinity(null);
                        }
                    }
                }
            }

            currPodTemplate.getSpec().setImagePullSecrets(updatedPodTemplate.getSpec().getImagePullSecrets());

            // volumes
            if (CollectionUtils.isNotEmpty(currPodTemplate.getSpec().getVolumes())
                    || CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getVolumes())) {
                if (CollectionUtils.isNotEmpty(currPodTemplate.getSpec().getVolumes())
                        && CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getVolumes())) {
                    // 값이 다르다면 업데이트 값만 추가하여 줌
                    if (!CollectionUtils.isEqualCollection(currPodTemplate.getSpec().getVolumes(), updatedPodTemplate.getSpec().getVolumes())) {
                        List<V1Volume> addVolumes = new ArrayList<>();
                        Map<String, V1Volume> currVolMap = currPodTemplate.getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, v1Volume -> v1Volume));
                        Map<String, V1Volume> updatedVolMap = updatedPodTemplate.getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, v1Volume -> v1Volume));

                        // 바뀌지 않은 것도 추가
                        MapDifference<String, V1Volume> diff = Maps.difference(currVolMap, updatedVolMap);
                        if (MapUtils.isNotEmpty(diff.entriesInCommon())) {
                            for (Map.Entry<String, V1Volume> volumeEntry : diff.entriesInCommon().entrySet()) {
                                addVolumes.add(volumeEntry.getValue());
                            }
                        }
                        // gui에서 온 volume에 새로운 것은 추가
                        if (MapUtils.isNotEmpty(diff.entriesOnlyOnRight())) {
                            for (Map.Entry<String, V1Volume> volumeEntry : diff.entriesOnlyOnRight().entrySet()) {
                                addVolumes.add(volumeEntry.getValue());
                            }
                        }
                        // 현재 volume 값 중에 gui에서 지원하는 source인
                        // pvc, emptyDir, hostPath, secret, configMap을 제외한 volume source가 있다면
                        // 추가 해주고 없다면 삭제했다는 것으로 가정하고 추가하지 않음
                        if (MapUtils.isNotEmpty(diff.entriesOnlyOnLeft())) {
                            for (Map.Entry<String, V1Volume> volumeEntry : diff.entriesOnlyOnLeft().entrySet()) {
                                if (!(volumeEntry.getValue().getPersistentVolumeClaim() != null
                                        || volumeEntry.getValue().getEmptyDir() != null
                                        || volumeEntry.getValue().getHostPath() != null
                                        || volumeEntry.getValue().getSecret() != null
                                        || volumeEntry.getValue().getConfigMap() != null)
                                ) {
                                    addVolumes.add(volumeEntry.getValue());
                                }
                            }
                        }
                        // gui에서 바뀐 값으로 추가
                        if (MapUtils.isNotEmpty(diff.entriesDiffering())) {
                            Map<String, MapDifference.ValueDifference<V1Volume>> differenceMap = diff.entriesDiffering();
                            for (Map.Entry<String, MapDifference.ValueDifference<V1Volume>> volumeDiffEntry : differenceMap.entrySet()) {
                                // emptyDir 일경우 예외처리
                                // UI에서 넘어올 수 있는 Memory일 경우에만 Merge, 아니면 기존값으로 Merge
                                if (volumeDiffEntry.getValue().leftValue().getEmptyDir() != null || volumeDiffEntry.getValue().rightValue().getEmptyDir() != null) {
                                    if (volumeDiffEntry.getValue().leftValue().getEmptyDir() != null && volumeDiffEntry.getValue().rightValue().getEmptyDir() != null) {
                                        if (!StringUtils.equals(volumeDiffEntry.getValue().rightValue().getEmptyDir().getMedium(), "Memory")) {
                                            volumeDiffEntry.getValue().rightValue().getEmptyDir().setMedium(volumeDiffEntry.getValue().leftValue().getEmptyDir().getMedium());
                                        }
                                        volumeDiffEntry.getValue().rightValue().getEmptyDir().setSizeLimit(volumeDiffEntry.getValue().leftValue().getEmptyDir().getSizeLimit());
                                    } else {
                                        if (volumeDiffEntry.getValue().rightValue().getEmptyDir() == null) {
                                            volumeDiffEntry.getValue().rightValue().emptyDir(volumeDiffEntry.getValue().leftValue().getEmptyDir());
                                        }
                                    }
                                }

                                addVolumes.add(volumeDiffEntry.getValue().rightValue());
                            }
                        }
                        currPodTemplate.getSpec().setVolumes(addVolumes);
                    }
                } else {
                    if (CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getVolumes())) {
                        currPodTemplate.getSpec().setVolumes(updatedPodTemplate.getSpec().getVolumes());
                    } else {
                        currPodTemplate.getSpec().setVolumes(null);
                    }
                }
            }

            // Init Container
            if (CollectionUtils.isNotEmpty(currPodTemplate.getSpec().getInitContainers())
                    || CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getInitContainers())
            ) {
                if (CollectionUtils.isNotEmpty(currPodTemplate.getSpec().getInitContainers())
                        && CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getInitContainers())
                ) {
                    if (!CollectionUtils.isEqualCollection(currPodTemplate.getSpec().getInitContainers(), updatedPodTemplate.getSpec().getInitContainers())) {
                        List<V1Container> addInitContainers = new ArrayList<>();

                        this.mergeContainer(currPodTemplate.getSpec().getVolumes(), updatedPodTemplate.getSpec().getVolumes(), currVolumeClaimTemplates, addInitContainers, currPodTemplate.getSpec().getInitContainers(), updatedPodTemplate.getSpec().getInitContainers(), true);

                        currPodTemplate.getSpec().setInitContainers(addInitContainers);
                    }

                } else {
                    if (CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getInitContainers())) {
                        currPodTemplate.getSpec().setInitContainers(updatedPodTemplate.getSpec().getInitContainers());
                    } else {
                        currPodTemplate.getSpec().setInitContainers(null);
                    }
                }
            }
            // Container
            if (CollectionUtils.isNotEmpty(currPodTemplate.getSpec().getContainers())
                    || CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getContainers())
            ) {
                if (CollectionUtils.isNotEmpty(currPodTemplate.getSpec().getContainers())
                        && CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getContainers())
                ) {
                    if (!CollectionUtils.isEqualCollection(currPodTemplate.getSpec().getContainers(), updatedPodTemplate.getSpec().getContainers())) {
                        List<V1Container> addContainers = new ArrayList<>();

                        this.mergeContainer(currPodTemplate.getSpec().getVolumes(), updatedPodTemplate.getSpec().getVolumes(), currVolumeClaimTemplates, addContainers, currPodTemplate.getSpec().getContainers(), updatedPodTemplate.getSpec().getContainers(), false);

                        currPodTemplate.getSpec().setContainers(addContainers);
                    }

                } else {
                    if (CollectionUtils.isNotEmpty(updatedPodTemplate.getSpec().getContainers())) {
                        currPodTemplate.getSpec().setContainers(updatedPodTemplate.getSpec().getContainers());
                    } else {
                        currPodTemplate.getSpec().setContainers(null);
                    }
                }
            }
        }
    }

//    private Optional<V1NodeSelectorRequirement> getGpuNodeAffinityOptional(V1Affinity v1Affinity) throws Exception {
//        return Optional.of(v1Affinity)
//                .map(V1Affinity::getNodeAffinity)
//                .map(V1NodeAffinity::getRequiredDuringSchedulingIgnoredDuringExecution)
//                .map(V1NodeSelector::getNodeSelectorTerms)
//                .flatMap(rdside -> rdside.stream()
//                        .filter(nst -> (CollectionUtils.isNotEmpty(nst.getMatchExpressions())) )
//                        .flatMap(nst -> nst.getMatchExpressions().stream().filter(
//                                nsr -> (StringUtils.equals(nsr.getKey(), KubeConstants.RESOURCES_GPU)
//                                        && StringUtils.equals(nsr.getOperator(), KubeConstants.TOLERATION_OPERATOR_EXISTS)
//                                        && CollectionUtils.isEmpty(nsr.getValues())))
//                        ).findFirst()
//                );
//    }

    private void mergeContainer(List<V1Volume> currVolumes, List<V1Volume> updatedVolumes, List<V1PersistentVolumeClaim> currVolumeClaimTemplates, List<V1Container> addContainers, List<V1Container> currContainer, List<V1Container> updatedContainer, boolean isInitContainer) throws Exception {
        Map<String, V1Container> currContainerMap = currContainer.stream().collect(Collectors.toMap(V1Container::getName, v1Container -> v1Container));
        Map<String, V1Container> updatedContainerMap = updatedContainer.stream().collect(Collectors.toMap(V1Container::getName, v1Container -> v1Container));

        // 바뀌지 않은 것도 추가
        MapDifference<String, V1Container> diff = Maps.difference(currContainerMap, updatedContainerMap);
        if (MapUtils.isNotEmpty(diff.entriesInCommon())) {
            for (Map.Entry<String, V1Container> containerEntry : diff.entriesInCommon().entrySet()) {
                addContainers.add(containerEntry.getValue());
            }
        }
        // gui에서 온 container에 새로운 것은 추가
        if (MapUtils.isNotEmpty(diff.entriesOnlyOnRight())) {
            for (Map.Entry<String, V1Container> containerEntry : diff.entriesOnlyOnRight().entrySet()) {
                addContainers.add(containerEntry.getValue());
            }
        }
        // 현재 container는 삭제
//        if (MapUtils.isNotEmpty(diff.entriesOnlyOnLeft())) {
//            for (Map.Entry<String, V1Container> containerEntry : diff.entriesOnlyOnLeft().entrySet()) {
//                addContainers.add(containerEntry.getValue());
//            }
//        }
        if (MapUtils.isNotEmpty(diff.entriesDiffering())) {
            Map<String, MapDifference.ValueDifference<V1Container>> differenceMap = diff.entriesDiffering();
            for (Map.Entry<String, MapDifference.ValueDifference<V1Container>> valueDifferenceEntry : differenceMap.entrySet()) {
                // args
                valueDifferenceEntry.getValue().leftValue().setArgs(valueDifferenceEntry.getValue().rightValue().getArgs());
                // command
                valueDifferenceEntry.getValue().leftValue().setCommand(valueDifferenceEntry.getValue().rightValue().getCommand());
                // image
                valueDifferenceEntry.getValue().leftValue().setImage(valueDifferenceEntry.getValue().rightValue().getImage());
                // imagePullPolicy
                if (StringUtils.isNotBlank(valueDifferenceEntry.getValue().rightValue().getImagePullPolicy())) {
                    valueDifferenceEntry.getValue().leftValue().setImagePullPolicy(valueDifferenceEntry.getValue().rightValue().getImagePullPolicy());
                }
                // port
                if (CollectionUtils.isNotEmpty(valueDifferenceEntry.getValue().rightValue().getPorts())) {
                    valueDifferenceEntry.getValue().leftValue().setPorts(valueDifferenceEntry.getValue().rightValue().getPorts());
                } else {
                    valueDifferenceEntry.getValue().leftValue().setPorts(null);
                }

                // env
                if (CollectionUtils.isNotEmpty(valueDifferenceEntry.getValue().leftValue().getEnv())
                        || CollectionUtils.isNotEmpty(valueDifferenceEntry.getValue().rightValue().getEnv())
                ) {
                    if (CollectionUtils.isNotEmpty(valueDifferenceEntry.getValue().leftValue().getEnv())
                            && CollectionUtils.isNotEmpty(valueDifferenceEntry.getValue().rightValue().getEnv())
                    ) {
                        if (!CollectionUtils.isEqualCollection(valueDifferenceEntry.getValue().leftValue().getEnv(), valueDifferenceEntry.getValue().rightValue().getEnv())) {
                            List<V1EnvVar> addEnvVars = new ArrayList<>();
                            Map<String, V1EnvVar> currEnvVarMap = valueDifferenceEntry.getValue().leftValue().getEnv().stream().collect(Collectors.toMap(V1EnvVar::getName, v1EnvVar -> v1EnvVar));
                            Map<String, V1EnvVar> updatedEnvVarMap = valueDifferenceEntry.getValue().rightValue().getEnv().stream().collect(Collectors.toMap(V1EnvVar::getName, v1EnvVar -> v1EnvVar));

                            // 바뀌지 않은 것도 추가
                            MapDifference<String, V1EnvVar> diffEnvVar = Maps.difference(currEnvVarMap, updatedEnvVarMap);
                            if (MapUtils.isNotEmpty(diffEnvVar.entriesInCommon())) {
                                for (Map.Entry<String, V1EnvVar> envVarEntry : diffEnvVar.entriesInCommon().entrySet()) {
                                    addEnvVars.add(envVarEntry.getValue());
                                }
                            }
                            // gui에서 온 container에 새로운 것은 추가
                            if (MapUtils.isNotEmpty(diffEnvVar.entriesOnlyOnRight())) {
                                for (Map.Entry<String, V1EnvVar> envVarEntry : diffEnvVar.entriesOnlyOnRight().entrySet()) {
                                    addEnvVars.add(envVarEntry.getValue());
                                }
                            }
                            // 현재 container는 그대로 추가해 줌
//                            if (MapUtils.isNotEmpty(diffEnvVar.entriesOnlyOnLeft())) {
//                                for (Map.Entry<String, V1EnvVar> envVarEntry : diffEnvVar.entriesOnlyOnLeft().entrySet()) {
//                                    addEnvVars.add(envVarEntry.getValue());
//                                }
//                            }
                            if (MapUtils.isNotEmpty(diffEnvVar.entriesDiffering())) {
                                Map<String, MapDifference.ValueDifference<V1EnvVar>> differenceEnvVarMap = diffEnvVar.entriesDiffering();
                                for (Map.Entry<String, MapDifference.ValueDifference<V1EnvVar>> valueDifferenceEnvVarEntry : differenceEnvVarMap.entrySet()) {
                                    addEnvVars.add(valueDifferenceEnvVarEntry.getValue().rightValue());
                                }
                            }

                            valueDifferenceEntry.getValue().leftValue().setEnv(addEnvVars);
                        }

                    } else {
                        if (CollectionUtils.isNotEmpty(valueDifferenceEntry.getValue().rightValue().getEnv())) {
                            valueDifferenceEntry.getValue().leftValue().setEnv(valueDifferenceEntry.getValue().rightValue().getEnv());
                        } else {
                            valueDifferenceEntry.getValue().leftValue().setEnv(null);
                        }
                    }
                }

                // lifecycle
                if (valueDifferenceEntry.getValue().leftValue().getLifecycle() != null
                        || valueDifferenceEntry.getValue().rightValue().getLifecycle() != null
                ) {
                    valueDifferenceEntry.getValue().leftValue().setLifecycle(valueDifferenceEntry.getValue().rightValue().getLifecycle());
                }

                // livenessProbe
                if (valueDifferenceEntry.getValue().leftValue().getLivenessProbe() != null
                        || valueDifferenceEntry.getValue().rightValue().getLivenessProbe() != null
                ) {
                    valueDifferenceEntry.getValue().leftValue().setLivenessProbe(valueDifferenceEntry.getValue().rightValue().getLivenessProbe());
                }

                // readinessProbe
                if (!isInitContainer) {
                    if (valueDifferenceEntry.getValue().leftValue().getReadinessProbe() != null
                            || valueDifferenceEntry.getValue().rightValue().getReadinessProbe() != null
                    ) {
                        valueDifferenceEntry.getValue().leftValue().setReadinessProbe(valueDifferenceEntry.getValue().rightValue().getReadinessProbe());
                    }
                }

                // resources
                if (valueDifferenceEntry.getValue().leftValue().getResources() != null
                        || valueDifferenceEntry.getValue().rightValue().getResources() != null
                ) {
                    if (valueDifferenceEntry.getValue().leftValue().getResources() != null
                            && valueDifferenceEntry.getValue().rightValue().getResources() != null
                    ) {
                        // request
                        if (MapUtils.isNotEmpty(valueDifferenceEntry.getValue().rightValue().getResources().getRequests())) {
                            for (Map.Entry<String, Quantity> requestEntry : valueDifferenceEntry.getValue().rightValue().getResources().getRequests().entrySet()) {
                                valueDifferenceEntry.getValue().leftValue().getResources().putRequestsItem(
                                        requestEntry.getKey(),
                                        requestEntry.getValue()
                                );
                            }
                        }
                        // limit
                        if (MapUtils.isNotEmpty(valueDifferenceEntry.getValue().rightValue().getResources().getLimits())) {
                            for (Map.Entry<String, Quantity> limitEntry : valueDifferenceEntry.getValue().rightValue().getResources().getLimits().entrySet()) {
                                valueDifferenceEntry.getValue().leftValue().getResources().putLimitsItem(
                                        limitEntry.getKey(),
                                        limitEntry.getValue()
                                );
                            }
                        }
                    } else {
                        if (valueDifferenceEntry.getValue().rightValue().getResources() != null) {
                            valueDifferenceEntry.getValue().leftValue().setResources(valueDifferenceEntry.getValue().rightValue().getResources());
                        }
                    }
                }

                // securityContext
                if (valueDifferenceEntry.getValue().leftValue().getSecurityContext() != null
                        || valueDifferenceEntry.getValue().rightValue().getSecurityContext() != null
                ) {
                    valueDifferenceEntry.getValue().leftValue().setSecurityContext(valueDifferenceEntry.getValue().rightValue().getSecurityContext());
                }

                // volumeMounts
                // Volume 이름으로 map 생성
                Map<String, V1Volume> currVolMap = Optional.ofNullable(currVolumes).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(V1Volume::getName, v1Volume -> v1Volume));
                Map<String, V1Volume> updatedVolMap = Optional.ofNullable(updatedVolumes).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(V1Volume::getName, v1Volume -> v1Volume));
                Map<String, V1PersistentVolumeClaim> currVolTmpltMap = Optional.ofNullable(currVolumeClaimTemplates).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(vt -> vt.getMetadata().getName(), vt -> vt));

                // 현재 volumeMount를 volume 이름으로  Map<String, List<V1VolumeMount>> 생성
                Map<String, List<V1VolumeMount>> currVolMntMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(valueDifferenceEntry.getValue().leftValue().getVolumeMounts())) {
                    for (V1VolumeMount volumeMountRow : valueDifferenceEntry.getValue().leftValue().getVolumeMounts()) {
                        if ((MapUtils.isNotEmpty(currVolMap) && currVolMap.containsKey(volumeMountRow.getName()))
                                || (MapUtils.isNotEmpty(currVolTmpltMap) && currVolTmpltMap.containsKey(volumeMountRow.getName()))
                        ) {
                            if (!currVolMntMap.containsKey(volumeMountRow.getName())) {
                                currVolMntMap.put(volumeMountRow.getName(), new ArrayList<>());
                            }

                            currVolMntMap.get(volumeMountRow.getName()).add(volumeMountRow);
                        }
                    }
                }
                // 수정한 volumeMount를 volume 이름으로 Map<String, List<V1VolumeMount>> 생성
                Map<String, List<V1VolumeMount>> updatedVolMntMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(valueDifferenceEntry.getValue().rightValue().getVolumeMounts())) {
                    for (V1VolumeMount volumeMountRow : valueDifferenceEntry.getValue().rightValue().getVolumeMounts()) {
                        if ((MapUtils.isNotEmpty(updatedVolMap) && updatedVolMap.containsKey(volumeMountRow.getName()))
                                || (MapUtils.isNotEmpty(currVolTmpltMap) && currVolTmpltMap.containsKey(volumeMountRow.getName()))
                        ) {
                            if (!updatedVolMntMap.containsKey(volumeMountRow.getName())) {
                                updatedVolMntMap.put(volumeMountRow.getName(), new ArrayList<>());
                            }

                            updatedVolMntMap.get(volumeMountRow.getName()).add(volumeMountRow);
                        }
                    }
                }

                if (!CollectionUtils.isEqualCollection(
                        Optional.ofNullable(valueDifferenceEntry.getValue().leftValue().getVolumeMounts()).orElseGet(() ->Lists.newArrayList()),
                        Optional.ofNullable(valueDifferenceEntry.getValue().rightValue().getVolumeMounts()).orElseGet(() ->Lists.newArrayList())
                    )
                ) {
                    List<V1VolumeMount> addVolumeMounts = new ArrayList<>();

                    MapDifference<String, List<V1VolumeMount>> diffVolMnt = Maps.difference(currVolMntMap, updatedVolMntMap);
                    // 바뀌지 않은 것도 추가
                    if (MapUtils.isNotEmpty(diffVolMnt.entriesInCommon())) {
                        for (Map.Entry<String, List<V1VolumeMount>> volMntEntry : diffVolMnt.entriesInCommon().entrySet()) {
                            addVolumeMounts.addAll(volMntEntry.getValue());
                        }
                    }
                    // gui에서 온 container에 새로운 것은 추가
                    if (MapUtils.isNotEmpty(diffVolMnt.entriesOnlyOnRight())) {
                        for (Map.Entry<String, List<V1VolumeMount>> volMntEntry : diffVolMnt.entriesOnlyOnRight().entrySet()) {
                            addVolumeMounts.addAll(volMntEntry.getValue());
                        }
                    }
                    // 현재 container는 그대로 추가해 줌
//                                if (MapUtils.isNotEmpty(diffVolMnt.entriesOnlyOnLeft())) {
//                                    for (Map.Entry<String, List<V1VolumeMount>> volMntEntry : diffVolMnt.entriesOnlyOnLeft().entrySet()) {
//                                        addVolumeMounts.addAll(volMntEntry.getValue());
//                                    }
//                                }
                    if (MapUtils.isNotEmpty(diffVolMnt.entriesDiffering())) {
                        Map<String, MapDifference.ValueDifference<List<V1VolumeMount>>> differenceVolMntMap = diffVolMnt.entriesDiffering();
                        for (Map.Entry<String, MapDifference.ValueDifference<List<V1VolumeMount>>> valueDifferenceVolMntEntry : differenceVolMntMap.entrySet()) {
                            addVolumeMounts.addAll(valueDifferenceVolMntEntry.getValue().rightValue());
                        }
                    }

                    valueDifferenceEntry.getValue().leftValue().setVolumeMounts(addVolumeMounts);
                }

                addContainers.add(valueDifferenceEntry.getValue().leftValue());
            }
        }
    }

    private void mergeService(ClusterVO cluster, String namespace, WorkloadType workloadType, PortType serviceType, ServerGuiVO serverGui, V1Service currService, StringBuffer yamlStr) throws Exception {

        if (CollectionUtils.isNotEmpty(serverGui.getServices())) {
            if (serviceType == PortType.HEADLESS) {
                // Workload(StatefulSet, DaemonSet)에서 생성되는 service는 Headless만 생성됨
                // 그래서 update service 값이 잘못 넘어오더라도 Headless로 변경하여 초기화 시켜줌
                serverGui.getServices().get(0).setHeadlessFlag(Boolean.TRUE);
                serverGui.getServices().get(0).setServiceType(PortType.HEADLESS.getCode());
                serverGui.getServices().get(0).setClusterIp(null);
            } else {
                // Workload에서 생성되는 service는 ClusterIP만 생성됨
                // 그래서 update service 값이 잘못 넘어오더라도 ClusterIP로 변경하여 초기화 시켜줌
                serverGui.getServices().get(0).setHeadlessFlag(Boolean.FALSE);
                serverGui.getServices().get(0).setServiceType(PortType.CLUSTER_IP.getCode());
                serverGui.getServices().get(0).setClusterIp(null);
            }
        }

        // 현재 서비스가 생성되어 있고
        if (currService != null) {
            // 수정하는 서비스의 값이 존재한다면 merge 처리
            if (CollectionUtils.isNotEmpty(serverGui.getServices())) {
                V1Service updatedService = K8sSpecFactory.buildServiceV1(serverGui.getServices().get(0), namespace, serverGui.getComponent().getComponentName(), true, cluster);

                /**
                 * merge
                 */
                // metadata
//                currService.getMetadata().setAnnotations(updatedService.getMetadata().getAnnotations());
//                currService.getMetadata().setLabels(updatedService.getMetadata().getLabels());
                // spec
//                currService.getSpec().setSelector(updatedService.getSpec().getSelector());
                currService.getSpec().setSessionAffinity(updatedService.getSpec().getSessionAffinity());
                currService.getSpec().setSessionAffinityConfig(updatedService.getSpec().getSessionAffinityConfig());
                currService.getSpec().setPorts(updatedService.getSpec().getPorts());
                if (serviceType == PortType.EXTERNAL_NAME) {
                    currService.getSpec().setExternalName(updatedService.getSpec().getExternalName());
                }

            } else {
                currService = null;
            }
        }
        // 현재 생성된 서비스가 없고
        else {
            // 수정하는 서비스의 값이 존재한다면 merge 없이 currService에 셋팅
            if (CollectionUtils.isNotEmpty(serverGui.getServices())) {
                currService = K8sSpecFactory.buildServiceV1(serverGui.getServices().get(0), namespace, serverGui.getComponent().getComponentName(), true, cluster);
            }
        }

        // merge된 값이 있다면 yaml로 변환
        if (currService != null) {
            currService.setStatus(null);
            if (workloadType != WorkloadType.STATEFUL_SET_SERVER) {
                yamlStr.append("---\n");
            }
            yamlStr.append(ServerUtils.marshalYaml(currService));
        }



    }

    /**
     * 현재 배포된 리소스에 gui에서 수정된 내용을 merge함
     * @param cluster
     * @param namespace
     * @param appName
     * @param serviceSpec
     * @param currService
     * @param yamlStr
     * @throws Exception
     */
    private V1Service mergeService(ClusterVO cluster, String namespace, String appName, ServiceSpecGuiVO serviceSpec, V1Service currService, StringBuffer yamlStr) throws Exception {

        V1Service currCopyService = null;
        // 현재 서비스가 생성되어 있고
        if (currService != null) {
            // 수정하는 서비스의 값이 존재한다면 merge 처리
            if (serviceSpec != null) {
                V1Service updatedService = K8sSpecFactory.buildServiceV1(serviceSpec, namespace, appName, false, cluster);
                currCopyService = k8sPatchSpecFactory.copyObject(currService, new TypeReference<V1Service>() {});

                /**
                 * merge
                 */
                // metadata
                currCopyService.getMetadata().setAnnotations(updatedService.getMetadata().getAnnotations());
                currCopyService.getMetadata().setLabels(updatedService.getMetadata().getLabels());
                // spec
                currCopyService.getSpec().setSelector(updatedService.getSpec().getSelector());
                currCopyService.getSpec().setSessionAffinity(updatedService.getSpec().getSessionAffinity());
                currCopyService.getSpec().setSessionAffinityConfig(updatedService.getSpec().getSessionAffinityConfig());
                currCopyService.getSpec().setPorts(updatedService.getSpec().getPorts());
                if (PortType.valueOf(serviceSpec.getServiceType()) == PortType.EXTERNAL_NAME) {
                    currCopyService.getSpec().setExternalName(updatedService.getSpec().getExternalName());
                }

            }
        }
        // 현재 생성된 서비스가 없고
        else {
            // 수정하는 서비스의 값이 존재한다면 merge 없이 currService에 셋팅
            if (serviceSpec != null) {
                currCopyService = K8sSpecFactory.buildServiceV1(serviceSpec, namespace, appName, false, cluster);
            }
        }
        // merge된 값이 있다면 yaml로 변환
        if (currCopyService != null) {
            currCopyService.setStatus(null);
            yamlStr.append(ServerUtils.marshalYaml(currCopyService));
        }

        return currCopyService;
    }

//    private void mergeHorizontalPodAutoscalerV2beta2(V2beta2HorizontalPodAutoscaler currHpa, V2beta2HorizontalPodAutoscaler updatedHpa) throws Exception {
//        /**
//         * metadata
//         */
//        currHpa.getMetadata().setAnnotations(updatedHpa.getMetadata().getAnnotations());
//        currHpa.getMetadata().setLabels(updatedHpa.getMetadata().getLabels());
//
//        /**
//         * spec
//         */
//        currHpa.getSpec().setMaxReplicas(updatedHpa.getSpec().getMaxReplicas());
//        currHpa.getSpec().setMinReplicas(updatedHpa.getSpec().getMinReplicas());
//
//        if (CollectionUtils.isNotEmpty(updatedHpa.getSpec().getMetrics())) {
//            List<V2beta2MetricSpec> addMetrics = Lists.newArrayList();
//            // MetricSpec List -> Map , key = metricName
//            Map<String, V2beta2MetricSpec> currMetricMap = Maps.newHashMap();
//            Map<String, V2beta2MetricSpec> updatedMetricMap = Maps.newHashMap();
//            this.convertMetricSpecMapV2beta2(currHpa.getSpec().getMetrics(), currMetricMap);
//            this.convertMetricSpecMapV2beta2(updatedHpa.getSpec().getMetrics(), updatedMetricMap);
//
//            // 바뀌지 않은 것도 추가
//            MapDifference<String, V2beta2MetricSpec> diff = Maps.difference(currMetricMap, updatedMetricMap);
//            if (MapUtils.isNotEmpty(diff.entriesInCommon())) {
//                for (Map.Entry<String, V2beta2MetricSpec> metricSpecEntry : diff.entriesInCommon().entrySet()) {
//                    addMetrics.add(metricSpecEntry.getValue());
//                }
//            }
//            // gui에서 온 metric spec에 새로운 것은 추가
//            if (MapUtils.isNotEmpty(diff.entriesOnlyOnRight())) {
//                for (Map.Entry<String, V2beta2MetricSpec> metricSpecEntry : diff.entriesOnlyOnRight().entrySet()) {
//                    addMetrics.add(metricSpecEntry.getValue());
//                }
//            }
//            // 현재 metric spec는 그대로 추가해 줌
////            if (MapUtils.isNotEmpty(diff.entriesOnlyOnLeft())) {
////                for (Map.Entry<String, V2beta2MetricSpec> metricSpecEntry : diff.entriesOnlyOnLeft().entrySet()) {
////                    addMetrics.add(metricSpecEntry.getValue());
////                }
////            }
//            if (MapUtils.isNotEmpty(diff.entriesDiffering())) {
//                Map<String, MapDifference.ValueDifference<V2beta2MetricSpec>> differenceMap = diff.entriesDiffering();
//                for (Map.Entry<String, MapDifference.ValueDifference<V2beta2MetricSpec>> valueDifferenceEntry : differenceMap.entrySet()) {
//                    addMetrics.add(valueDifferenceEntry.getValue().rightValue());
//                }
//            }
//
//            currHpa.getSpec().setMetrics(addMetrics);
//        } else {
//            currHpa = null;
//        }
//    }

    private void mergeHorizontalPodAutoscalerV2(V2HorizontalPodAutoscaler currHpa, V2HorizontalPodAutoscaler updatedHpa) throws Exception {
        /**
         * metadata
         */
        currHpa.getMetadata().setAnnotations(updatedHpa.getMetadata().getAnnotations());
        currHpa.getMetadata().setLabels(updatedHpa.getMetadata().getLabels());

        /**
         * spec
         */
        currHpa.getSpec().setMaxReplicas(updatedHpa.getSpec().getMaxReplicas());
        currHpa.getSpec().setMinReplicas(updatedHpa.getSpec().getMinReplicas());

        if (updatedHpa == null || updatedHpa.getSpec() == null || updatedHpa.getSpec().getMaxReplicas() == null) {
            currHpa = null;
        } else {
            if (CollectionUtils.isNotEmpty(updatedHpa.getSpec().getMetrics())) {
                List<V2MetricSpec> addMetrics = Lists.newArrayList();
                // MetricSpec List -> Map , key = metricName
                Map<String, V2MetricSpec> currMetricMap = Maps.newHashMap();
                Map<String, V2MetricSpec> updatedMetricMap = Maps.newHashMap();
                this.convertMetricSpecMapV2(currHpa.getSpec().getMetrics(), currMetricMap);
                this.convertMetricSpecMapV2(updatedHpa.getSpec().getMetrics(), updatedMetricMap);

                // 바뀌지 않은 것도 추가
                MapDifference<String, V2MetricSpec> diff = Maps.difference(currMetricMap, updatedMetricMap);
                if (MapUtils.isNotEmpty(diff.entriesInCommon())) {
                    for (Map.Entry<String, V2MetricSpec> metricSpecEntry : diff.entriesInCommon().entrySet()) {
                        addMetrics.add(metricSpecEntry.getValue());
                    }
                }
                // gui에서 온 metric spec에 새로운 것은 추가
                if (MapUtils.isNotEmpty(diff.entriesOnlyOnRight())) {
                    for (Map.Entry<String, V2MetricSpec> metricSpecEntry : diff.entriesOnlyOnRight().entrySet()) {
                        addMetrics.add(metricSpecEntry.getValue());
                    }
                }
                // 현재 metric spec는 그대로 추가해 줌
//            if (MapUtils.isNotEmpty(diff.entriesOnlyOnLeft())) {
//                for (Map.Entry<String, V2beta2MetricSpec> metricSpecEntry : diff.entriesOnlyOnLeft().entrySet()) {
//                    addMetrics.add(metricSpecEntry.getValue());
//                }
//            }
                if (MapUtils.isNotEmpty(diff.entriesDiffering())) {
                    Map<String, MapDifference.ValueDifference<V2MetricSpec>> differenceMap = diff.entriesDiffering();
                    for (Map.Entry<String, MapDifference.ValueDifference<V2MetricSpec>> valueDifferenceEntry : differenceMap.entrySet()) {
                        addMetrics.add(valueDifferenceEntry.getValue().rightValue());
                    }
                }

                currHpa.getSpec().setMetrics(addMetrics);
            }

            // spec.behavior
            if (updatedHpa.getSpec().getBehavior() != null) {
                if (currHpa.getSpec().getBehavior() == null) {
                    currHpa.getSpec().setBehavior(new V2HorizontalPodAutoscalerBehavior());
                }

                JSON k8sJson = new JSON();
                // scale down
                if (updatedHpa.getSpec().getBehavior().getScaleDown() != null) {
                    String toJson = k8sJson.serialize(updatedHpa.getSpec().getBehavior().getScaleDown());
                    currHpa.getSpec().getBehavior().setScaleDown(k8sJson.getGson().fromJson(toJson, new TypeToken<V2HPAScalingRules>(){}.getType()));
                } else {
                    currHpa.getSpec().getBehavior().setScaleDown(null);
                }
                // scale up
                if (updatedHpa.getSpec().getBehavior().getScaleUp() != null) {
                    String toJson = k8sJson.serialize(updatedHpa.getSpec().getBehavior().getScaleUp());
                    currHpa.getSpec().getBehavior().setScaleUp(k8sJson.getGson().fromJson(toJson, new TypeToken<V2HPAScalingRules>(){}.getType()));
                } else {
                    currHpa.getSpec().getBehavior().setScaleUp(null);
                }
            } else {
                currHpa.getSpec().setBehavior(null);
            }
        }
    }

    private void mergeHorizontalPodAutoscalerV2beta2(V2beta2HorizontalPodAutoscaler currHpa, V2beta2HorizontalPodAutoscaler updatedHpa) throws Exception {
        /**
         * metadata
         */
        currHpa.getMetadata().setAnnotations(updatedHpa.getMetadata().getAnnotations());
        currHpa.getMetadata().setLabels(updatedHpa.getMetadata().getLabels());

        /**
         * spec
         */
        currHpa.getSpec().setMaxReplicas(updatedHpa.getSpec().getMaxReplicas());
        currHpa.getSpec().setMinReplicas(updatedHpa.getSpec().getMinReplicas());

        if (updatedHpa == null || updatedHpa.getSpec() == null || updatedHpa.getSpec().getMaxReplicas() == null) {
            currHpa = null;
        } else {
            if (CollectionUtils.isNotEmpty(updatedHpa.getSpec().getMetrics())) {
                List<V2beta2MetricSpec> addMetrics = Lists.newArrayList();
                // MetricSpec List -> Map , key = metricName
                Map<String, V2beta2MetricSpec> currMetricMap = Maps.newHashMap();
                Map<String, V2beta2MetricSpec> updatedMetricMap = Maps.newHashMap();
                this.convertMetricSpecMapV2beta2(currHpa.getSpec().getMetrics(), currMetricMap);
                this.convertMetricSpecMapV2beta2(updatedHpa.getSpec().getMetrics(), updatedMetricMap);

                // 바뀌지 않은 것도 추가
                MapDifference<String, V2beta2MetricSpec> diff = Maps.difference(currMetricMap, updatedMetricMap);
                if (MapUtils.isNotEmpty(diff.entriesInCommon())) {
                    for (Map.Entry<String, V2beta2MetricSpec> metricSpecEntry : diff.entriesInCommon().entrySet()) {
                        addMetrics.add(metricSpecEntry.getValue());
                    }
                }
                // gui에서 온 metric spec에 새로운 것은 추가
                if (MapUtils.isNotEmpty(diff.entriesOnlyOnRight())) {
                    for (Map.Entry<String, V2beta2MetricSpec> metricSpecEntry : diff.entriesOnlyOnRight().entrySet()) {
                        addMetrics.add(metricSpecEntry.getValue());
                    }
                }
                // 현재 metric spec는 그대로 추가해 줌
//            if (MapUtils.isNotEmpty(diff.entriesOnlyOnLeft())) {
//                for (Map.Entry<String, V2beta2MetricSpec> metricSpecEntry : diff.entriesOnlyOnLeft().entrySet()) {
//                    addMetrics.add(metricSpecEntry.getValue());
//                }
//            }
                if (MapUtils.isNotEmpty(diff.entriesDiffering())) {
                    Map<String, MapDifference.ValueDifference<V2beta2MetricSpec>> differenceMap = diff.entriesDiffering();
                    for (Map.Entry<String, MapDifference.ValueDifference<V2beta2MetricSpec>> valueDifferenceEntry : differenceMap.entrySet()) {
                        addMetrics.add(valueDifferenceEntry.getValue().rightValue());
                    }
                }

                currHpa.getSpec().setMetrics(addMetrics);
            }

            // spec.behavior
            if (updatedHpa.getSpec().getBehavior() != null) {
                if (currHpa.getSpec().getBehavior() == null) {
                    currHpa.getSpec().setBehavior(new V2beta2HorizontalPodAutoscalerBehavior());
                }

                JSON k8sJson = new JSON();
                // scale down
                if (updatedHpa.getSpec().getBehavior().getScaleDown() != null) {
                    String toJson = k8sJson.serialize(updatedHpa.getSpec().getBehavior().getScaleDown());
                    currHpa.getSpec().getBehavior().setScaleDown(k8sJson.getGson().fromJson(toJson, new TypeToken<V2beta2HPAScalingRules>(){}.getType()));
                } else {
                    currHpa.getSpec().getBehavior().setScaleDown(null);
                }
                // scale up
                if (updatedHpa.getSpec().getBehavior().getScaleUp() != null) {
                    String toJson = k8sJson.serialize(updatedHpa.getSpec().getBehavior().getScaleUp());
                    currHpa.getSpec().getBehavior().setScaleUp(k8sJson.getGson().fromJson(toJson, new TypeToken<V2beta2HPAScalingRules>(){}.getType()));
                } else {
                    currHpa.getSpec().getBehavior().setScaleUp(null);
                }
            } else {
                currHpa.getSpec().setBehavior(null);
            }
        }
    }

    private void mergeHorizontalPodAutoscalerV2beta1(V2beta1HorizontalPodAutoscaler currHpa, V2beta1HorizontalPodAutoscaler updatedHpa) throws Exception {
        /**
         * metadata
         */
        currHpa.getMetadata().setAnnotations(updatedHpa.getMetadata().getAnnotations());
        currHpa.getMetadata().setLabels(updatedHpa.getMetadata().getLabels());

        /**
         * spec
         */
        currHpa.getSpec().setMaxReplicas(updatedHpa.getSpec().getMaxReplicas());
        currHpa.getSpec().setMinReplicas(updatedHpa.getSpec().getMinReplicas());

        if (CollectionUtils.isNotEmpty(updatedHpa.getSpec().getMetrics())) {
            List<V2beta1MetricSpec> addMetrics = Lists.newArrayList();
            // MetricSpec List -> Map , key = metricName
            Map<String, V2beta1MetricSpec> currMetricMap = Maps.newHashMap();
            Map<String, V2beta1MetricSpec> updatedMetricMap = Maps.newHashMap();
            this.convertMetricSpecMapV2beta1(currHpa.getSpec().getMetrics(), currMetricMap);
            this.convertMetricSpecMapV2beta1(updatedHpa.getSpec().getMetrics(), updatedMetricMap);

            // 바뀌지 않은 것도 추가
            MapDifference<String, V2beta1MetricSpec> diff = Maps.difference(currMetricMap, updatedMetricMap);
            if (MapUtils.isNotEmpty(diff.entriesInCommon())) {
                for (Map.Entry<String, V2beta1MetricSpec> metricSpecEntry : diff.entriesInCommon().entrySet()) {
                    addMetrics.add(metricSpecEntry.getValue());
                }
            }
            // gui에서 온 metric spec에 새로운 것은 추가
            if (MapUtils.isNotEmpty(diff.entriesOnlyOnRight())) {
                for (Map.Entry<String, V2beta1MetricSpec> metricSpecEntry : diff.entriesOnlyOnRight().entrySet()) {
                    addMetrics.add(metricSpecEntry.getValue());
                }
            }
            // 현재 metric spec는 그대로 추가해 줌
//            if (MapUtils.isNotEmpty(diff.entriesOnlyOnLeft())) {
//                for (Map.Entry<String, V2beta2MetricSpec> metricSpecEntry : diff.entriesOnlyOnLeft().entrySet()) {
//                    addMetrics.add(metricSpecEntry.getValue());
//                }
//            }
            if (MapUtils.isNotEmpty(diff.entriesDiffering())) {
                Map<String, MapDifference.ValueDifference<V2beta1MetricSpec>> differenceMap = diff.entriesDiffering();
                for (Map.Entry<String, MapDifference.ValueDifference<V2beta1MetricSpec>> valueDifferenceEntry : differenceMap.entrySet()) {
                    addMetrics.add(valueDifferenceEntry.getValue().rightValue());
                }
            }

            currHpa.getSpec().setMetrics(addMetrics);
        } else {
            currHpa = null;
        }
    }

//    private void convertMetricSpecMapV2beta2(List<V2beta2MetricSpec> metricSpecs, Map<String, V2beta2MetricSpec> metricMap) throws Exception {
//        for (V2beta2MetricSpec spec : metricSpecs) {
//            MetricType metricType = MetricType.valueOf(spec.getType());
//            switch (metricType) {
//                case Resource:
//                    if (spec.getResource() != null) {
//                        metricMap.put(spec.getResource().getName(), spec);
//                    }
//                    break;
//                case Pods:
//                    if (spec.getPods() != null) {
//                        metricMap.put(spec.getPods().getMetric().getName(), spec);
//                    }
//                    break;
//                case Object:
//                    if (spec.getObject() != null) {
//                        metricMap.put(spec.getObject().getMetric().getName(), spec);
//                    }
//                    break;
//                case External:
//                    if (spec.getExternal() != null) {
//                        metricMap.put(spec.getExternal().getMetric().getName(), spec);
//                    }
//                    break;
//            }
//        }
//    }

    private void convertMetricSpecMapV2(List<V2MetricSpec> metricSpecs, Map<String, V2MetricSpec> metricMap) throws Exception {
        for (V2MetricSpec spec : metricSpecs) {
            MetricType metricType = MetricType.valueOf(spec.getType());
            switch (metricType) {
                case Resource:
                    if (spec.getResource() != null) {
                        metricMap.put(spec.getResource().getName(), spec);
                    }
                    break;
                case Pods:
                    if (spec.getPods() != null) {
                        metricMap.put(spec.getPods().getMetric().getName(), spec);
                    }
                    break;
                case Object:
                    if (spec.getObject() != null) {
                        metricMap.put(spec.getObject().getMetric().getName(), spec);
                    }
                    break;
                case External:
                    if (spec.getExternal() != null) {
                        metricMap.put(spec.getExternal().getMetric().getName(), spec);
                    }
                    break;
            }
        }
    }

    private void convertMetricSpecMapV2beta2(List<V2beta2MetricSpec> metricSpecs, Map<String, V2beta2MetricSpec> metricMap) throws Exception {
        for (V2beta2MetricSpec spec : metricSpecs) {
            MetricType metricType = MetricType.valueOf(spec.getType());
            switch (metricType) {
                case Resource:
                    if (spec.getResource() != null) {
                        metricMap.put(spec.getResource().getName(), spec);
                    }
                    break;
                case Pods:
                    if (spec.getPods() != null) {
                        metricMap.put(spec.getPods().getMetric().getName(), spec);
                    }
                    break;
                case Object:
                    if (spec.getObject() != null) {
                        metricMap.put(spec.getObject().getMetric().getName(), spec);
                    }
                    break;
                case External:
                    if (spec.getExternal() != null) {
                        metricMap.put(spec.getExternal().getMetric().getName(), spec);
                    }
                    break;
            }
        }
    }

    private void convertMetricSpecMapV2beta1(List<V2beta1MetricSpec> metricSpecs, Map<String, V2beta1MetricSpec> metricMap) throws Exception {
        for (V2beta1MetricSpec spec : metricSpecs) {
            MetricType metricType = MetricType.valueOf(spec.getType());
            switch (metricType) {
                case Resource:
                    if (spec.getResource() != null) {
                        metricMap.put(spec.getResource().getName(), spec);
                    }
                    break;
                case Pods:
                    if (spec.getPods() != null) {
                        metricMap.put(spec.getPods().getMetricName(), spec);
                    }
                    break;
                case Object:
                    if (spec.getObject() != null) {
                        metricMap.put(spec.getObject().getMetricName(), spec);
                    }
                    break;
                case External:
                    if (spec.getExternal() != null) {
                        metricMap.put(spec.getExternal().getMetricName(), spec);
                    }
                    break;
            }
        }
    }

    public String getDescription(Map<String, String> annotaions) throws Exception {
        String desc = null;
        boolean isBase64 = Utils.isBase64Encoded(MapUtils.getString(annotaions, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, ""));
        if(isBase64) {
            desc = new String(Base64Utils.decodeFromString(MapUtils.getString(annotaions, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "")), "UTF-8");
        }
        else {
            desc = "";
        }
        return desc;
    }
}
