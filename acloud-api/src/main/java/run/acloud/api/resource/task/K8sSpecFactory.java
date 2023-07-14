package run.acloud.api.resource.task;

import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.QuantityFormatter;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ClusterVolumeParamterVO;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.enums.PortType;
import run.acloud.api.cserver.enums.VolumePlugIn;
import run.acloud.api.cserver.enums.VolumeType;
import run.acloud.api.cserver.enums.WorkloadType;
import run.acloud.api.k8sextended.models.*;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.UnsupportedEncodingException;
import java.util.*;

@Slf4j
public class K8sSpecFactory {

    public static Map<String, String> buildDefaultPrefixLabel() {
        Map<String, String> labels = new HashMap<>();
//        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
        return labels;
    }

    public static Map<String, String> buildDefaultPrefixLabel(String label) {
        Map<String, String> labels = new HashMap<>();
//        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, StringUtils.defaultString(label, ResourceUtil.getResourcePrefix()));
        return labels;
    }

    public static Map<String, String> buildNamespaceLabel(Map<String, String> labels) {
//        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
        // istio injection default 처리
        if (MapUtils.isEmpty(labels)) {
            labels = Maps.newHashMap();
        }
        if (MapUtils.getString(labels, KubeConstants.LABELS_ISTIO_INJECTION_KEY, null) == null) {
            labels.put(KubeConstants.LABELS_ISTIO_INJECTION_KEY, KubeConstants.LABELS_ISTIO_INJECTION_VALUE_DISABLED);
        }
        return labels;
    }

    public static V1Secret buildSecretV1(SecretGuiVO secretParam) {
        V1Secret secret = new V1Secret();
        secret.setApiVersion(K8sApiType.V1.getValue());
        secret.setKind(K8sApiKindType.SECRET.getValue());

	    // meta
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(secretParam.getName());
        if (StringUtils.isNotBlank(secretParam.getNamespace())) {
            meta.setNamespace(secretParam.getNamespace());
        }
        if (secretParam.getType() == null) {
            secretParam.setType(SecretType.Generic);
        }

        /** R3.5 : Parameter로 넘어온 Labels를 기본으로 사용.. **/
        Map<String, String> label = Optional.ofNullable(secretParam.getLabels()).orElseGet(() ->new HashMap<>());
        label.put(KubeConstants.LABELS_SECRET, secretParam.getType().name());
//        meta.putLabelsItem(KubeConstants.LABELS_SECRET, secretParam.getType().name());
        if(secretParam.isMakeCocktail()) {
//            meta.putLabelsItem(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
//            label.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
        }
        meta.setLabels(label);

        /** R3.5 : Parameter로 넘어온 Annotation를 기본으로 사용.. **/
        Map<String, String> annotations = Optional.ofNullable(secretParam.getAnnotations()).orElseGet(() ->new HashMap<>());
        try {
            annotations.put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Base64Utils.encodeToString(StringUtils.defaultString(secretParam.getDescription()).getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new CocktailException("description do not set.", ExceptionType.K8sSecretDescInvalid);
        }
        meta.setAnnotations(annotations);
        secret.setMetadata(meta);

        secret.setType(secretParam.getType().getValue());

        try {
            if(MapUtils.isNotEmpty(secretParam.getData())){
                for(Map.Entry<String, String> dataEntry : secretParam.getData().entrySet()){
                    if(StringUtils.isNotBlank(dataEntry.getValue())){
                        secret.putDataItem(dataEntry.getKey(), dataEntry.getValue().getBytes("UTF-8"));
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new CocktailException("data do not set.", ExceptionType.SecretDataInvalid);
        }
//        secret.setStringData(secretParam.getData());

        return secret;
    }

    public static V1ConfigMap buildConfigMapV1(ConfigMapGuiVO configMapParam) throws Exception{
        V1ConfigMap configMap = new V1ConfigMap();
        configMap.setApiVersion(K8sApiType.V1.getValue());
        configMap.setKind(K8sApiKindType.CONFIG_MAP.getValue());

        /**
         * meta
         */
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(configMapParam.getName());
        if (StringUtils.isNotBlank(configMapParam.getNamespace())) {
            meta.setNamespace(configMapParam.getNamespace());
        }

        // label
        Map<String, String> label = Optional.ofNullable(configMapParam.getLabels()).orElseGet(() ->new HashMap<>()); // R3.5 : Parameter로 넘어온 Labels를 기본으로 사용..
        meta.setLabels(label);
        // annotations
        Map<String, String> annotations = Optional.ofNullable(configMapParam.getAnnotations()).orElseGet(() ->new HashMap<>()); // Parameter로 넘어온 Annotation을 기본으로 사용..
        // Description을 annotation에 KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION 키로 저장한다.
        annotations.put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(configMapParam.getDescription()))));
        meta.setAnnotations(annotations);
        configMap.setMetadata(meta);

        /**
         * data
         */
        configMap.setData(configMapParam.getData());

        return configMap;
    }

    public static V1PersistentVolume buildPersistentVolumeV1(PersistentVolumeVO persistentVolumeParam) throws Exception {
        V1PersistentVolume pv = new V1PersistentVolume();
        pv.setApiVersion(K8sApiType.V1.getValue());
        pv.setKind(K8sApiKindType.PERSISTENT_VOLUME.getValue());

        // meta
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(persistentVolumeParam.getName());
        // labels
        Map<String, String> labels = K8sSpecFactory.buildDefaultPrefixLabel(ResourceUtil.getFormattedPVLabelName());
        if (MapUtils.isNotEmpty(persistentVolumeParam.getLabels())) {
            labels.putAll(persistentVolumeParam.getLabels());
        }
        labels.put(KubeConstants.CUSTOM_VOLUME_STORAGE, persistentVolumeParam.getStorageVolumeName());
        labels.put(KubeConstants.CUSTOM_PERSISTENT_VOLUME_TYPE, persistentVolumeParam.getAccessMode().getPersistentVolumeType().name());
        labels.put(KubeConstants.CUSTOM_VOLUME_TYPE, persistentVolumeParam.getVolumeType().name());
        meta.setLabels(labels);
        // annotations
        if (MapUtils.isNotEmpty(persistentVolumeParam.getAnnotations())) {
            meta.setAnnotations(persistentVolumeParam.getAnnotations());
        }
        pv.setMetadata(meta);

        // spec
        V1PersistentVolumeSpec spec = new V1PersistentVolumeSpec();

        Map<String, Quantity> cap = new HashMap<>();
        cap.put(KubeConstants.VOLUMES_STORAGE, new Quantity(String.format("%dGi", persistentVolumeParam.getCapacity())));
        spec.setCapacity(cap);

        List<String> accessMode = new ArrayList<>();
        accessMode.add(persistentVolumeParam.getAccessMode().getValue());
        spec.setAccessModes(accessMode);

        spec.setPersistentVolumeReclaimPolicy(persistentVolumeParam.getReclaimPolicy().getValue());

        VolumePlugIn plugIn = persistentVolumeParam.getPlugin();
        if (plugIn == VolumePlugIn.NFSDYNAMIC || plugIn == VolumePlugIn.NFSSTATIC) {
            V1NFSVolumeSource src = new V1NFSVolumeSource();
            for(PersistentVolumeParamterVO p : persistentVolumeParam.getParameters()) {
                if (p.getName().compareTo("server") == 0) {
                    src.setServer(p.getValue());
                } else if (p.getName().compareTo("path") == 0) {
                    src.setPath(p.getValue());
                }
            }
            spec.setNfs(src);
        } else if (plugIn == VolumePlugIn.AWSEBS) {
            V1AWSElasticBlockStoreVolumeSource src = new V1AWSElasticBlockStoreVolumeSource();
            for(PersistentVolumeParamterVO p : persistentVolumeParam.getParameters()) {
                if (StringUtils.equalsIgnoreCase("fsType", p.getName())) {
                    src.setFsType(p.getValue());
                } else if(StringUtils.equalsIgnoreCase("partition", p.getName())){
                    src.setPartition(Integer.parseInt(p.getValue()));
                } else if(StringUtils.equalsIgnoreCase("readOnly", p.getName())){
                    src.setReadOnly(BooleanUtils.toBoolean(p.getValue()));
                } else if(StringUtils.equalsIgnoreCase("volumeID", p.getName())){
                    src.setVolumeID(p.getValue());
                }
            }
            spec.setAwsElasticBlockStore(src);
        } else if (plugIn == VolumePlugIn.GCE) {
            V1GCEPersistentDiskVolumeSource src = new V1GCEPersistentDiskVolumeSource();
            for(PersistentVolumeParamterVO p : persistentVolumeParam.getParameters()) {
                if (StringUtils.equalsIgnoreCase("fsType", p.getName())) {
                    src.setFsType(p.getValue());
                } else if(StringUtils.equalsIgnoreCase("partition", p.getName())){
                    src.setPartition(Integer.parseInt(p.getValue()));
                } else if(StringUtils.equalsIgnoreCase("readOnly", p.getName())){
                    src.setReadOnly(BooleanUtils.toBoolean(p.getValue()));
                } else if(StringUtils.equalsIgnoreCase("pdName", p.getName())){
                    src.setPdName(p.getValue());
                }
            }
            spec.setGcePersistentDisk(src);
        } else if (plugIn == VolumePlugIn.AZUREDISK) {
            V1AzureDiskVolumeSource src = new V1AzureDiskVolumeSource();
            for(PersistentVolumeParamterVO p : persistentVolumeParam.getParameters()) {
                if (StringUtils.equalsIgnoreCase("fsType", p.getName())) {
                    src.setFsType(p.getValue());
                } else if(StringUtils.equalsIgnoreCase("cachingMode", p.getName())){
                    src.setCachingMode(p.getValue());
                } else if(StringUtils.equalsIgnoreCase("readOnly", p.getName())){
                    src.setReadOnly(BooleanUtils.toBoolean(p.getValue()));
                } else if(StringUtils.equalsIgnoreCase("diskName", p.getName())){
                    src.setDiskName(p.getValue());
                } else if(StringUtils.equalsIgnoreCase("diskURI", p.getName())){
                    src.setDiskURI(p.getValue());
                } else if(StringUtils.equalsIgnoreCase("kind", p.getName())){
                    src.setKind(p.getValue());
                }
            }
            spec.setAzureDisk(src);
        } else {
            throw new CocktailException(String.format("Not Supported Volume plug-in: %s", persistentVolumeParam.getPlugin().getCode()),
                    ExceptionType.NotSupportedVolumePlugIn);
        }

        spec.setStorageClassName(persistentVolumeParam.getStorageClassName());

        pv.setSpec(spec);

        return pv;
    }


    public static V1PersistentVolumeClaim buildPersistentVolumeClaimV1(PersistentVolumeClaimGuiVO persistentVolumeClaimParam) {
        V1PersistentVolumeClaim pvc = new V1PersistentVolumeClaim();
        pvc.setApiVersion(K8sApiType.V1.getValue());
        pvc.setKind(K8sApiKindType.PERSISTENT_VOLUME_CLAIM.getValue());

        // meta
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(persistentVolumeClaimParam.getName());
        // labels
        Map<String, String> labels = K8sSpecFactory.buildDefaultPrefixLabel(ResourceUtil.getFormattedPVLabelName());
        if (MapUtils.isNotEmpty(persistentVolumeClaimParam.getLabels())) {
            labels.putAll(persistentVolumeClaimParam.getLabels());
        }
        labels.put(KubeConstants.CUSTOM_VOLUME_STORAGE, persistentVolumeClaimParam.getStorageVolumeName());
        labels.put(KubeConstants.CUSTOM_PERSISTENT_VOLUME_TYPE, persistentVolumeClaimParam.getAccessMode().getPersistentVolumeType().name());
        labels.put(KubeConstants.CUSTOM_VOLUME_TYPE, persistentVolumeClaimParam.getVolumeType().name());
        meta.setLabels(labels);
        // annotations
        if (MapUtils.isNotEmpty(persistentVolumeClaimParam.getAnnotations())) {
            meta.setAnnotations(persistentVolumeClaimParam.getAnnotations());
        }
        pvc.setMetadata(meta);

        // spec
        V1PersistentVolumeClaimSpec spec = new V1PersistentVolumeClaimSpec();
        List<String> accessMode = new ArrayList<>();
        accessMode.add(persistentVolumeClaimParam.getAccessMode().getValue());
        spec.setAccessModes(accessMode);

        V1ResourceRequirements rr = new V1ResourceRequirements();
        Map<String, Quantity> r = new HashMap<>();
        r.put(KubeConstants.VOLUMES_STORAGE, new Quantity(String.format("%dGi", persistentVolumeClaimParam.getCapacity().intValue())));
        rr.setRequests(r);
        spec.setResources(rr);

        if (EnumSet.of(VolumeType.PERSISTENT_VOLUME, VolumeType.PERSISTENT_VOLUME_LINKED).contains(persistentVolumeClaimParam.getVolumeType())) {
            spec.setStorageClassName(persistentVolumeClaimParam.getStorageClassName());
        } else {
            spec.setVolumeName(persistentVolumeClaimParam.getVolumeName());
            spec.setStorageClassName("");
        }
        pvc.setSpec(spec);

        return pvc;
    }

    public static V1Service buildServiceV1(ServiceSpecGuiVO serviceSpec, String namespace, String appName, boolean isWorkload, ClusterVO cluster) throws Exception {

        PortType type = PortType.valueOf(serviceSpec.getServiceType());

        Map<String, String> labels = Optional.ofNullable(serviceSpec.getLabels()).orElseGet(() ->new HashMap<>()); // R3.5 : Parameter로 넘어온 Labels를 기본으로 사용..
        boolean isExistsAppLabel = false;
        if (labels.containsKey(KubeConstants.LABELS_KEY)) {
            isExistsAppLabel = true;
        }
        if (!isExistsAppLabel) {
            labels.put(KubeConstants.LABELS_KEY, serviceSpec.getName());
        }

        Map<String, String> annotations = Optional.ofNullable(serviceSpec.getAnnotations()).orElseGet(() ->new HashMap<>()); // Parameter로 넘어온 Annotation을 기본으로 사용..

        V1ObjectMeta metaData = new V1ObjectMeta();
        metaData.setName(serviceSpec.getName());
        metaData.setNamespace(namespace);
        metaData.setLabels(labels);

        List<V1ServicePort> servicePorts = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(serviceSpec.getServicePorts())) {
            for (ServicePortVO p : serviceSpec.getServicePorts()) {
                V1ServicePort servicePort = new V1ServicePort();
                servicePort.setName(p.getName());
                servicePort.setPort(p.getPort());
                servicePort.setProtocol(p.getProtocol());
                /** 2020.01.10 : Redion : targetPort가 규격을 준수하는지 체크 : 상세 내용은 호출 메서드 참고. **/
                ResourceUtil.isValidPortRule(p.getTargetPort());
                // 숫자로만 이루어져 있으면 Integer로, 아니면 String으로 입력..
                if(NumberUtils.isDigits(p.getTargetPort())) {
                    servicePort.setTargetPort(new IntOrString(Integer.parseInt(p.getTargetPort())));
                }
                else {
                    servicePort.setTargetPort(new IntOrString(p.getTargetPort()));
                }

                if (type == PortType.NODE_PORT && p.getNodePort() != null) {
                    servicePort.setNodePort(p.getNodePort());
                }
                servicePorts.add(servicePort);
            }
        }

        Map<String, String> selector = new HashMap<>();
        // R3.5.0, labelselecor이 존재하면 labelselector 사용하도록 수정, 2019/12/09, coolingi
        // TODO 3.5.0 이후 else 로직은 필요 없을 수 있음.
        if(MapUtils.isNotEmpty(serviceSpec.getLabelSelector())){
            selector = serviceSpec.getLabelSelector();
        } else {
            if(StringUtils.isNotBlank(appName)) {
                selector.put(KubeConstants.LABELS_KEY, appName);
            }
        }

        V1ServiceSpec spec = new V1ServiceSpec();
        spec.setSelector(selector);
        spec.setPorts(servicePorts);

        // Set service type
        switch (type) {
            case NODE_PORT:
                spec.setType(KubeServiceTypes.NodePort.name());
                break;
            case LOADBALANCER:
                spec.setType(KubeServiceTypes.LoadBalancer.name());
                if(serviceSpec.getInternalLBFlag() != null && serviceSpec.getInternalLBFlag().booleanValue()){
                    if(cluster != null && cluster.getProviderAccount().getProviderCode().canInternalLB()){
                        switch (cluster.getProviderAccount().getProviderCode()) {
                            case AWS:
                                // 1.15 부터 value가 변경됨
                                if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_15)) {
                                    annotations.put(KubeConstants.META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_KEY, KubeConstants.META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_VALUE_AFTER_1_15);
                                } else {
                                    annotations.put(KubeConstants.META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_KEY, KubeConstants.META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_VALUE);
                                }
                                break;
                            case GCP:
                                    annotations.put(KubeConstants.META_ANNOTATIONS_GCP_INTERNAL_LOADBALANCER_KEY, KubeConstants.META_ANNOTATIONS_GCP_INTERNAL_LOADBALANCER_VALUE);
                                break;
                            case AZR:
                                    annotations.put(KubeConstants.META_ANNOTATIONS_AZR_INTERNAL_LOADBALANCER_KEY, KubeConstants.META_ANNOTATIONS_AZR_INTERNAL_LOADBALANCER_VALUE);
                                break;
                            case NCP:
                                    annotations.put(KubeConstants.META_ANNOTATIONS_NCP_INTERNAL_LOADBALANCER_KEY, KubeConstants.META_ANNOTATIONS_NCP_INTERNAL_LOADBALANCER_VALUE);
                                break;
                        }
                    }
                }
                break;
            case HEADLESS:
            case CLUSTER_IP:
                spec.setType(KubeServiceTypes.ClusterIP.name());
                // Set headless service
                if (serviceSpec.getHeadlessFlag()) {
                    spec.setClusterIP("None"); // CLUSTERIP 타입에 ClusterIP가 'None' 면 headless service
                }
                break;
            case EXTERNAL_NAME:
                spec.setExternalName(serviceSpec.getExternalName());
                break;
        }

        // Set session affinity
        if (serviceSpec.getStickySessionFlag() != null && serviceSpec.getStickySessionFlag().booleanValue()) {
            log.debug("stickySession : [{}], [{}]", serviceSpec.getStickySessionFlag().booleanValue(), serviceSpec.getStickySessionTimeoutSeconds());
            spec.setSessionAffinity("ClientIP");
            // set timeoutSeconds
            V1SessionAffinityConfig sessionAffinityConfig = new V1SessionAffinityConfig();
            V1ClientIPConfig clientIPConfig = new V1ClientIPConfig();
            clientIPConfig.setTimeoutSeconds(serviceSpec.getStickySessionTimeoutSeconds());
            sessionAffinityConfig.setClientIP(clientIPConfig);
            spec.sessionAffinityConfig(sessionAffinityConfig);
        }else{
            spec.setSessionAffinity("None");
        }

        // Set annotation
        if (MapUtils.isNotEmpty(serviceSpec.getAnnotations())) {
            annotations.putAll(serviceSpec.getAnnotations());
        }
        metaData.setAnnotations(annotations);

        // Create service object
        V1Service service = new V1Service();
        service.setApiVersion(K8sApiType.V1.getValue());
        service.setKind(K8sApiKindType.SERVICE.getValue());
        service.setMetadata(metaData);
        service.setSpec(spec);
        return service;
    }

    public static V1ObjectMeta buildIngressV1ObjectMeta(IngressSpecGuiVO ingressSpec, String namespace) {
        Map<String, String> labels = Optional.ofNullable(ingressSpec.getLabels()).orElseGet(() ->new HashMap<>()); // R3.5 : Parameter로 넘어온 Labels를 기본으로 사용..
        boolean isExistsAppLabel = false;
        if (labels.containsKey(KubeConstants.LABELS_KEY)) {
            isExistsAppLabel = true;
        }
        if (!isExistsAppLabel) {
            labels.put(KubeConstants.LABELS_KEY, ingressSpec.getName());
        }
//        if(StringUtils.isNotBlank(componentId)){
//            labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getUniqueName(componentId));
//        }else {
//            labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
//        }

        Map<String, String> annotations = Optional.ofNullable(ingressSpec.getAnnotations()).orElseGet(() ->new HashMap<>()); // Parameter로 넘어온 Annotation을 기본으로 사용..
        if (StringUtils.isNotBlank(ingressSpec.getIngressControllerName())) {
            annotations.put(KubeConstants.META_ANNOTATIONS_INGRESSCLASS, ingressSpec.getIngressControllerName());
        }
        annotations.put(KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT, BooleanUtils.toString(ingressSpec.getUseSslRedirect(), "true", "false", "false"));
        // annotation에서 사용자가 직접 넣도록 함
//        annotations.put(KubeConstants.META_ANNOTATIONS_REWRITE_TARGET, "/");

        V1ObjectMeta metaData = new V1ObjectMeta();
        metaData.setName(ingressSpec.getName());
        metaData.setNamespace(namespace);
        metaData.setLabels(labels);
        metaData.setAnnotations(annotations);

        return metaData;
    }

    public static NetworkingV1beta1Ingress buildIngressNetworkingV1beta1(IngressSpecGuiVO ingressSpec, String namespace, String componentId) throws Exception {

        NetworkingV1beta1IngressSpec spec = new NetworkingV1beta1IngressSpec();
        if(CollectionUtils.isNotEmpty(ingressSpec.getIngressTLSs())){
            for (IngressTLSVO ingressTLS : ingressSpec.getIngressTLSs()) {
                NetworkingV1beta1IngressTLS tls = new NetworkingV1beta1IngressTLS();
                tls.setHosts(ingressTLS.getHosts());
                tls.setSecretName(ingressTLS.getSecretName());
                spec.addTlsItem(tls);
            }
        }
        if(CollectionUtils.isNotEmpty(ingressSpec.getIngressRules())){
            for (IngressRuleVO ingressRule : ingressSpec.getIngressRules()){
                NetworkingV1beta1IngressRule rule = new NetworkingV1beta1IngressRule();
                if(StringUtils.isNotBlank(ingressRule.getHostName())){
                    rule.setHost(ingressRule.getHostName());
                }

                NetworkingV1beta1HTTPIngressRuleValue httpIngressRuleValue = new NetworkingV1beta1HTTPIngressRuleValue();
                for (IngressHttpPathVO ingressHttpPath : ingressRule.getIngressHttpPaths()){
                    NetworkingV1beta1HTTPIngressPath path = new NetworkingV1beta1HTTPIngressPath();
                    // path
                    if(StringUtils.isNotBlank(ingressHttpPath.getPath())){
                        if(!StringUtils.startsWith(ingressHttpPath.getPath(), "/")){
                            ingressHttpPath.setPath(String.format("/%s", ingressHttpPath.getPath()));
                        }
                        path.setPath(ingressHttpPath.getPath());
                    }
                    // pathType
                    path.setPathType(ingressHttpPath.getPathType());
                    // backend
                    if(StringUtils.isNotBlank(ingressHttpPath.getServiceName()) && ingressHttpPath.getServicePort() != null){
                        NetworkingV1beta1IngressBackend backend = new NetworkingV1beta1IngressBackend();
                        backend.setServiceName(ingressHttpPath.getServiceName());

                        ResourceUtil.isValidPortRule(ingressHttpPath.getServicePort());
                        if (NumberUtils.isDigits(ingressHttpPath.getServicePort())) {
                            backend.setServicePort(new IntOrString(Integer.valueOf(Optional.ofNullable(ingressHttpPath.getServicePort()).orElseGet(() ->"0"))));
                        } else {
                            backend.setServicePort(new IntOrString(ingressHttpPath.getServicePort()));
                        }
                        path.setBackend(backend);
                    }
                    httpIngressRuleValue.addPathsItem(path);
                }
                rule.setHttp(httpIngressRuleValue);
                spec.addRulesItem(rule);
            }
        }

        NetworkingV1beta1Ingress ingress = new NetworkingV1beta1Ingress();
        ingress.setApiVersion(String.format("%s/%s", K8sApiGroupType.NETWORKING.getValue(), K8sApiType.V1BETA1.getValue()));
        ingress.setKind(K8sApiKindType.INGRESS.getValue());
        ingress.setMetadata(buildIngressV1ObjectMeta(ingressSpec, namespace));
        ingress.setSpec(spec);

        return ingress;
    }

    public static V1Ingress buildIngressNetworkingV1(IngressSpecGuiVO ingressSpec, String namespace, String componentId) throws Exception {

        V1IngressSpec spec = new V1IngressSpec();
        if(CollectionUtils.isNotEmpty(ingressSpec.getIngressTLSs())){
            for (IngressTLSVO ingressTLS : ingressSpec.getIngressTLSs()) {
                V1IngressTLS tls = new V1IngressTLS();
                tls.setHosts(ingressTLS.getHosts());
                tls.setSecretName(ingressTLS.getSecretName());
                spec.addTlsItem(tls);
            }
        }
        if(CollectionUtils.isNotEmpty(ingressSpec.getIngressRules())){
            for (IngressRuleVO ingressRule : ingressSpec.getIngressRules()){
                V1IngressRule rule = new V1IngressRule();
                if(StringUtils.isNotBlank(ingressRule.getHostName())){
                    rule.setHost(ingressRule.getHostName());
                }

                V1HTTPIngressRuleValue httpIngressRuleValue = new V1HTTPIngressRuleValue();
                for (IngressHttpPathVO ingressHttpPath : ingressRule.getIngressHttpPaths()){
                    V1HTTPIngressPath path = new V1HTTPIngressPath();
                    // path
                    if(StringUtils.isNotBlank(ingressHttpPath.getPath())){
                        if(!StringUtils.startsWith(ingressHttpPath.getPath(), "/")){
                            ingressHttpPath.setPath(String.format("/%s", ingressHttpPath.getPath()));
                        }
                        path.setPath(ingressHttpPath.getPath());
                    }
                    // pathType
                    if (StringUtils.isBlank(ingressHttpPath.getPathType())) {
                        ingressHttpPath.setPathType(PathType.ImplementationSpecific.getCode());
                    }
                    path.setPathType(ingressHttpPath.getPathType());
                    // backend
                    if(StringUtils.isNotBlank(ingressHttpPath.getServiceName()) && ingressHttpPath.getServicePort() != null){
                        V1IngressBackend backend = new V1IngressBackend();

                        V1IngressServiceBackend service = new V1IngressServiceBackend();
                        service.setName(ingressHttpPath.getServiceName());

                        V1ServiceBackendPort port = new V1ServiceBackendPort();
                        ResourceUtil.isValidPortRule(ingressHttpPath.getServicePort());
                        if (NumberUtils.isDigits(ingressHttpPath.getServicePort())) {
                            port.setNumber(Integer.valueOf(Optional.ofNullable(ingressHttpPath.getServicePort()).orElseGet(() ->"0")));
                        } else {
                            port.setName(ingressHttpPath.getServicePort());
                        }
                        service.setPort(port);

                        backend.setService(service);
                        path.setBackend(backend);
                    }
                    httpIngressRuleValue.addPathsItem(path);
                }
                rule.setHttp(httpIngressRuleValue);
                spec.addRulesItem(rule);
            }
        }

        V1Ingress ingress = new V1Ingress();
        ingress.setApiVersion(String.format("%s/%s", K8sApiGroupType.NETWORKING.getValue(), K8sApiType.V1.getValue()));
        ingress.setKind(K8sApiKindType.INGRESS.getValue());
        ingress.setMetadata(buildIngressV1ObjectMeta(ingressSpec, namespace));
        ingress.setSpec(spec);

        return ingress;
    }

    public static V1HorizontalPodAutoscaler buildHpaV1(HpaGuiVO hpa, String ns, String componentName, String name, K8sApiVerKindType k8sApiVerKindType) {

        Map<String, String> labels = new HashMap<>();
        labels.put(KubeConstants.LABELS_KEY, name);

        V1ObjectMeta metaData = new V1ObjectMeta();
        metaData.setName(name);
        metaData.setNamespace(ns);
        metaData.setLabels(labels);

        V1HorizontalPodAutoscalerSpec spec = new V1HorizontalPodAutoscalerSpec();
        spec.setMaxReplicas(hpa.getMaxReplicas());
        spec.setMinReplicas(hpa.getMinReplicas());
        spec.setTargetCPUUtilizationPercentage(hpa.getTargetCPUUtilizationPercentage());

        V1CrossVersionObjectReference crossVersionObjectReference = new V1CrossVersionObjectReference();
        crossVersionObjectReference.setApiVersion(String.format("%s/%s", K8sApiGroupType.EXTENSIONS.getValue(), K8sApiType.V1BETA1.getValue()));

        crossVersionObjectReference.setKind(k8sApiVerKindType.getKindType().getValue());
        crossVersionObjectReference.setName(componentName);
        spec.setScaleTargetRef(crossVersionObjectReference);

        V1HorizontalPodAutoscaler horizontalPodAutoscaler = new V1HorizontalPodAutoscaler();
        horizontalPodAutoscaler.setApiVersion(String.format("%s/%s", K8sApiGroupType.AUTOSCALING.getValue(), K8sApiType.V1.getValue()));
        horizontalPodAutoscaler.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
        horizontalPodAutoscaler.setMetadata(metaData);
        horizontalPodAutoscaler.setSpec(spec);

        return horizontalPodAutoscaler;
    }

//    public static V2beta1HorizontalPodAutoscaler buildHpaV1(HorizontalPodAutoscalerVO hpa, String ns, String componentId, String name) {
//
//        boolean useExtension = (hpa.getMetrics() != null && hpa.getMetrics().size() > 0);
//        Map<String, String> labels = new HashMap<>();
//        labels.put(KubeConstants.LABELS_KEY, name);
//        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, getUniqueName(componentId));
//
//        V1ObjectMeta metaData = new V1ObjectMeta();
//        metaData.setName(name);
//        metaData.setNamespace(ns);
//        metaData.setLabels(labels);
//
//        V2beta1HorizontalPodAutoscalerSpec spec = new V2beta1HorizontalPodAutoscalerSpec();
//        spec.setMaxReplicas(hpa.getMaxReplicas());
//        spec.setMinReplicas(hpa.getMinReplicas());
//        if (useExtension) {
//            List<Map<String, Object>> metrics = new ArrayList<>();
//            for (MetricVO metric : hpa.getMetrics()) {
//                switch (metric.getType()) {
//                    case Resource:
//                        Map<String, Object> resMeric = new HashMap<>();
//                        resMeric.put(KubeConstants.TYPE, metric.getType().getCode());
//                        resMeric.put(KubeConstants.NAME, metric.getResourceName());
//                        // resMeric.put(KubeConstants.HPA_TARGET_AVERAGE_VALUE, metric.getPodsTargetAverageValue());
//                        resMeric.put(KubeConstants.HPA_TARGET_AVERAGE_UTILIZATION, metric.getPodsTargetAverageValue());
//                        metrics.add(resMeric);
//                        break;
//                    case Pods:
//                        Map<String, Object> podMetric = new HashMap<>();
//                        podMetric.put(KubeConstants.TYPE, metric.getType().getCode());
//                        podMetric.put(KubeConstants.HPA_METRIC_NAME, metric.getPodsMetricName());
//                        podMetric.put(KubeConstants.HPA_TARGET_AVERAGE_VALUE, metric.getPodsTargetAverageValue());
//                        metrics.add(podMetric);
//                        break;
//                    case Object:
//                        Map<String, Object> target = new HashMap<>();
//                        target.put(KubeConstants.APIVSERION, metric.getObjectTargetApiVerion());
//                        target.put(KubeConstants.KIND, metric.getObjectTargetKind());
//                        target.put(KubeConstants.NAME, metric.getObjectTargetName());
//
//                        Map<String, Object> objMetric = new HashMap<>();
//                        objMetric.put(KubeConstants.TYPE, metric.getType().getCode());
//                        objMetric.put(KubeConstants.HPA_METRIC_NAME, metric.getObjectMetricName());
//                        objMetric.put(KubeConstants.HPA_TARGET_VALUE, metric.getObjectTargetValue());
//                        objMetric.put("target", target);
//                        metrics.add(objMetric);
//                        break;
//                }
//            }
//            spec.setAdditionalProperty(KubeConstants.HPA_METRICS, metrics);
//        } else {
//            spec.setTargetCPUUtilizationPercentage(hpa.getTargetCPUUtilizationPercentage());
//            V2beta1CrossVersionObjectReference crossVersionObjectReference = new V2beta1CrossVersionObjectReference();
//            crossVersionObjectReference.setApiVersion(KubeConstants.VERSION_APPS_V1);
//            crossVersionObjectReference.setKind(KubeResourceTypes.Deployment.name());
//            crossVersionObjectReference.setName(name);
//            spec.setScaleTargetRef(crossVersionObjectReference);
//        }
//
//        V1HorizontalPodAutoscaler horizontalPodAutoscaler = new V1HorizontalPodAutoscaler();
//        horizontalPodAutoscaler.setApiVersion((useExtension? KubeConstants.VERSION_AUTOSCALING_V2BETA1 : KubeConstants.VERSION_AUTOSCALING_V1));
//        horizontalPodAutoscaler.setKind(KubeResourceTypes.HorizontalPodAutoscaler.name());
//        horizontalPodAutoscaler.setMetadata(metaData);
//        horizontalPodAutoscaler.setSpec(spec);
//        return horizontalPodAutoscaler;
//    }

    private static V1ObjectMeta buildServerMetaV1(ServerDetailVO server, String namespace) {
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(ResourceUtil.getUniqueName(server.getComponent().getComponentName()));
        meta.setNamespace(namespace);
        meta.setLabels(buildServerDefaultLabel(server, false));
        if (MapUtils.isNotEmpty(server.getServer().getLabels())) {
            meta.getLabels().putAll(server.getServer().getLabels());
        }
        if (MapUtils.isNotEmpty(server.getServer().getAnnotations())) {
            meta.setAnnotations(server.getServer().getAnnotations());
        }
        if (server.getComponent().getDescription() != null) {
            meta.putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(server.getComponent().getDescription()))));
        }
        if (server.getComponent().getWorkloadGroupSeq() != null) {
            if (StringUtils.isNumeric(server.getComponent().getWorkloadGroupSeq().toString())) {
                meta.putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, server.getComponent().getWorkloadGroupSeq().toString());
            }
        } else {
            meta.getAnnotations().remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
        }

        return meta;
    }

    private static Map<String, String> buildServerDefaultLabel(ServerDetailVO server, boolean isSelector) {
        Map<String, String> labels = new HashMap<>();
        boolean isExistsAppLabel = false;
        if (MapUtils.isNotEmpty(server.getServer().getLabels())) {
            labels.putAll(server.getServer().getLabels());
            if (labels.containsKey(KubeConstants.LABELS_KEY)) {
                isExistsAppLabel = true;
            }
        }
        if (!isExistsAppLabel) {
            labels.put(KubeConstants.LABELS_KEY, ResourceUtil.getUniqueName(server.getComponent().getComponentName()));
        }

//        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getUniqueName(server.getComponent().getComponentId()));
//        if (WorkloadType.valueOf(server.getServer().getWorkloadType()) == WorkloadType.STATEFUL_SET_SERVER) {
//            labels.put(KubeConstants.CUSTOM_VOLUME_TYPE, VolumeType.PERSISTENT_VOLUME_LINKED.name());
//        }
//        if(!isSelector){
//            if(CollectionUtils.isNotEmpty(server.getVolumes())){
//                for (ContainerVolumeVO volume : server.getVolumes()){
//                    if(VolumeType.PERSISTENT_VOLUME_LINKED == volume.getVolumeType()){
//                        labels.put(volume.getPersistentVolumeClaimName(), KubeConstants.LABELS_VALUE_STORAGE);
//                    }
//                }
//            }
//        }

        return labels;
    }
//
//    public static List<JsonObject> buildPatchDeploymentV1(V1Deployment currentDeployment, V1Deployment updatedDeployment) throws Exception {
//        JsonNode diff = JsonDiff.asJson(PatchUtils.patchMapper().valueToTree(currentDeployment), PatchUtils.patchMapper().valueToTree(updatedDeployment));
//        String diffStr = PatchUtils.patchMapper().writeValueAsString(diff);
//        List<Map<String, Object>> diffMaps = PatchUtils.patchMapper().readValue(diffStr, new TypeReference<List<Map<String, Object>>>(){});
//
//        List<JsonObject> patchBody = new ArrayList<>();
//        if(CollectionUtils.isNotEmpty(diffMaps)){
//            for (Map<String, Object> diffMapRow : diffMaps){
//                patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(diffMapRow), JsonElement.class)).getAsJsonObject());
//            }
//        }
//
//        return patchBody;
//    }

    public static V1Deployment buildDeploymentV1(ServerDetailVO server, String namespace) throws Exception {
        V1Deployment deployment = new V1Deployment();

        deployment.setApiVersion(String.format("%s/%s", K8sApiGroupType.APPS.getValue(), K8sApiType.V1.getValue()));
        deployment.setKind(K8sApiKindType.DEPLOYMENT.getValue());

        deployment.setMetadata(buildServerMetaV1(server, namespace));

        deployment.setSpec(buildDeploymentSpecV1(server, namespace));

        return deployment;
    }

    private static V1DeploymentSpec buildDeploymentSpecV1(ServerDetailVO server, String namespace) throws Exception {
        V1DeploymentSpec spec = new V1DeploymentSpec();

        V1LabelSelector labelSelector = new V1LabelSelector();
        labelSelector.setMatchLabels(buildServerDefaultLabel(server, true));
        spec.setSelector(labelSelector);

        WorkloadType workloadType = WorkloadType.valueOf(server.getServer().getWorkloadType());
        V1DeploymentStrategy stg;
        if (workloadType == WorkloadType.REPLICA_SERVER) {
            spec.setReplicas(server.getServer().getComputeTotal());
            stg = buildDeploymentStrategyV1(server.getServer());
        } else if (workloadType == WorkloadType.SINGLE_SERVER) {
            stg = buildDeploymentStrategyV1(server.getServer());
        } else {
            throw new CocktailException(String.format("Not supported server type: %s", workloadType.getCode()),
                    ExceptionType.NotSupportedServerType);
        }

        if (stg != null) {
            spec.setStrategy(stg);
        }

        spec.setMinReadySeconds(server.getServer().getMinReadySeconds());

        spec.setTemplate(buildPodTemplateSpecV1(server, namespace));

        return spec;
    }

    private static V1DeploymentStrategy buildDeploymentStrategyV1(ServerVO server) {
        DeploymentStrategyVO stgVo = server.getStrategy();
        if (stgVo == null) {
            return null;
        }

        V1DeploymentStrategy stg = new V1DeploymentStrategy();
        if (stgVo.getType() == DeploymentStrategyType.Recreate) {
            stg.setType(KubeConstants.SPEC_STRATEGY_RECREATE);
        } else { // Default: KubeConstants.SPEC_STRATEGY_ROLLINGUPDATE
            stg.setType(KubeConstants.SPEC_STRATEGY_ROLLINGUPDATE);

            V1RollingUpdateDeployment rud = new V1RollingUpdateDeployment();
            if (StringUtils.isNotBlank(stgVo.getMaxUnavailable())) {
                if (StringUtils.isNumeric(stgVo.getMaxUnavailable())) {
                    rud.setMaxUnavailable(new IntOrString(Integer.valueOf(stgVo.getMaxUnavailable()).intValue()));
                } else {
                    rud.setMaxUnavailable(new IntOrString(stgVo.getMaxUnavailable()));
                }
            }
            if (StringUtils.isNotBlank(stgVo.getMaxSurge())) {
                if (StringUtils.isNumeric(stgVo.getMaxSurge())) {
                    rud.setMaxSurge(new IntOrString(Integer.valueOf(stgVo.getMaxSurge()).intValue()));
                } else {
                    rud.setMaxSurge(new IntOrString(stgVo.getMaxSurge()));
                }
            }

            stg.setRollingUpdate(rud);
        }
        return stg;
    }

    public static V1StatefulSet buildStatefulSetV1(ServerDetailVO server, String namespace) throws Exception {
        V1StatefulSet statefulSet = new V1StatefulSet();

        statefulSet.setApiVersion(String.format("%s/%s", K8sApiGroupType.APPS.getValue(), K8sApiType.V1.getValue()));
        statefulSet.setKind(K8sApiKindType.STATEFUL_SET.getValue());

        statefulSet.setMetadata(buildServerMetaV1(server, namespace));

        statefulSet.setSpec(buildStatefulSetSpecV1(server, namespace));

        return statefulSet;
    }

    private static V1StatefulSetSpec buildStatefulSetSpecV1(ServerDetailVO server, String namespace) throws Exception {
        V1StatefulSetSpec spec = new V1StatefulSetSpec();

        V1LabelSelector labelSelector = new V1LabelSelector();
        labelSelector.setMatchLabels(buildServerDefaultLabel(server, true));
        spec.setSelector(labelSelector);

        if (server.getServer().getStatefulSetStrategy() != null) {
            V1StatefulSetUpdateStrategy stg = new V1StatefulSetUpdateStrategy();

            // default - RollingUpdate
            if (server.getServer().getStatefulSetStrategy().getType() == null) {
                server.getServer().getStatefulSetStrategy().setType(StatefulSetStrategyType.RollingUpdate);
            }
            stg.setType(server.getServer().getStatefulSetStrategy().getType().getCode());
            if (server.getServer().getStatefulSetStrategy().getType() == StatefulSetStrategyType.RollingUpdate && server.getServer().getStatefulSetStrategy().getPartition() != null) {
                V1RollingUpdateStatefulSetStrategy rud = new V1RollingUpdateStatefulSetStrategy();
                stg.rollingUpdate(rud.partition(server.getServer().getStatefulSetStrategy().getPartition()));
            }

            spec.setUpdateStrategy(stg);
        }
        spec.setPodManagementPolicy(server.getServer().getPodManagementPolicy());
        spec.setReplicas(server.getServer().getComputeTotal());
        spec.setServiceName(server.getServer().getServiceName());

        if (CollectionUtils.isNotEmpty(server.getVolumeTemplates())) {
            for (PersistentVolumeClaimGuiVO pvcRow : server.getVolumeTemplates()) {
                V1PersistentVolumeClaim pvc = new V1PersistentVolumeClaim();

                V1ObjectMeta meta = new V1ObjectMeta();
                meta.setLabels(pvcRow.getLabels());
                meta.setAnnotations(pvcRow.getAnnotations());
                meta.setName(pvcRow.getName());

                V1PersistentVolumeClaimSpec pvcSpec = new V1PersistentVolumeClaimSpec();
                if(StringUtils.isNotBlank(pvcRow.getStorageClassName())
                    && pvcRow.getAccessMode() != null) {
                    pvcSpec.storageClassName(pvcRow.getStorageClassName())
                        .addAccessModesItem(pvcRow.getAccessMode().getValue());
                }

                V1ResourceRequirements pvcResource = new V1ResourceRequirements();
                pvcResource.putRequestsItem(KubeConstants.VOLUMES_STORAGE, new Quantity(String.format("%dGi", pvcRow.getCapacity().intValue())));
                pvcSpec.resources(pvcResource);

                pvc.metadata(meta).setSpec(pvcSpec);

                spec.addVolumeClaimTemplatesItem(pvc);
            }
        }

        spec.setTemplate(buildPodTemplateSpecV1(server, namespace));

        return spec;
    }

    public static V1DaemonSet buildDaemonSetV1(ServerDetailVO server, String namespace) throws Exception {
        V1DaemonSet daemonSet = new V1DaemonSet();

        daemonSet.setApiVersion(String.format("%s/%s", K8sApiGroupType.APPS.getValue(), K8sApiType.V1.getValue()));
        daemonSet.setKind(K8sApiKindType.DAEMON_SET.getValue());

        daemonSet.setMetadata(buildServerMetaV1(server, namespace));

        daemonSet.setSpec(buildDaemonSetSpecV1(server, namespace));

        return daemonSet;
    }

    private static V1DaemonSetSpec buildDaemonSetSpecV1(ServerDetailVO server, String namespace) throws Exception {
        V1DaemonSetSpec spec = new V1DaemonSetSpec();

        V1LabelSelector labelSelector = new V1LabelSelector();
        labelSelector.setMatchLabels(buildServerDefaultLabel(server, true));
        spec.setSelector(labelSelector);

        if (server.getServer().getDaemonSetStrategy() != null) {
            V1DaemonSetUpdateStrategy stg = new V1DaemonSetUpdateStrategy();

            if (server.getServer().getDaemonSetStrategy().getType() == null) {
                stg.setType(DaemonSetStrategyType.RollingUpdate.getCode());
            }
            stg.setType(server.getServer().getDaemonSetStrategy().getType().getCode());
            if (server.getServer().getDaemonSetStrategy().getType() == DaemonSetStrategyType.RollingUpdate && StringUtils.isNotBlank(server.getServer().getDaemonSetStrategy().getMaxUnavailable())) {
                V1RollingUpdateDaemonSet rud = new V1RollingUpdateDaemonSet();
                if (StringUtils.isNumeric(server.getServer().getDaemonSetStrategy().getMaxUnavailable())) {
                    stg.rollingUpdate(rud.maxUnavailable(new IntOrString(Integer.valueOf(server.getServer().getDaemonSetStrategy().getMaxUnavailable()).intValue())));
                } else {
                    stg.rollingUpdate(rud.maxUnavailable(new IntOrString(server.getServer().getDaemonSetStrategy().getMaxUnavailable())));
                }
            }

            spec.setUpdateStrategy(stg);
        }

        spec.setMinReadySeconds(server.getServer().getMinReadySeconds());

        spec.setTemplate(buildPodTemplateSpecV1(server, namespace));

        return spec;
    }

    public static V1beta1CronJob buildCronJobV1beta1(ServerDetailVO server, String namespace) throws Exception {
        V1beta1CronJob cronJob = new V1beta1CronJob();

        cronJob.setApiVersion(String.format("%s/%s", K8sApiGroupType.BATCH.getValue(), K8sApiType.V1BETA1.getValue()));
        cronJob.setKind(K8sApiKindType.CRON_JOB.getValue());

        cronJob.setMetadata(buildServerMetaV1(server, namespace));

        cronJob.setSpec(buildCronJobSpecV1beta1(server, namespace));

        return cronJob;
    }

    private static V1beta1CronJobSpec buildCronJobSpecV1beta1(ServerDetailVO server, String namespace) throws Exception {
        V1beta1CronJobSpec spec = new V1beta1CronJobSpec();

        spec.setConcurrencyPolicy(server.getServer().getConcurrencyPolicy().getCode());
        spec.setSchedule(server.getServer().getSchedule());
        spec.setStartingDeadlineSeconds(server.getServer().getStartingDeadlineSeconds());
        spec.setSuccessfulJobsHistoryLimit(server.getServer().getSuccessfulJobsHistoryLimit());
        spec.setFailedJobsHistoryLimit(server.getServer().getFailedJobsHistoryLimit());
        spec.setSuspend(server.getServer().getSuspend());

        spec.setJobTemplate(buildJobTemplateSpecV1beta1(server, namespace));

        return spec;
    }

    private static V1beta1JobTemplateSpec buildJobTemplateSpecV1beta1(ServerDetailVO server, String namespace) throws Exception {
        V1beta1JobTemplateSpec template = new V1beta1JobTemplateSpec();

        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setLabels(buildServerDefaultLabel(server, false));
        template.setMetadata(meta);

        template.setSpec(buildJobSpecV1(server, namespace, true));

        return template;
    }

    public static V1CronJob buildCronJobV1(ServerDetailVO server, String namespace) throws Exception {
        V1CronJob cronJob = new V1CronJob();

        cronJob.setApiVersion(String.format("%s/%s", K8sApiGroupType.BATCH.getValue(), K8sApiType.V1.getValue()));
        cronJob.setKind(K8sApiKindType.CRON_JOB.getValue());

        cronJob.setMetadata(buildServerMetaV1(server, namespace));

        cronJob.setSpec(buildCronJobSpecV1(server, namespace));

        return cronJob;
    }

    private static V1CronJobSpec buildCronJobSpecV1(ServerDetailVO server, String namespace) throws Exception {
        V1CronJobSpec spec = new V1CronJobSpec();

        spec.setConcurrencyPolicy(server.getServer().getConcurrencyPolicy().getCode());
        spec.setSchedule(server.getServer().getSchedule());
        spec.setStartingDeadlineSeconds(server.getServer().getStartingDeadlineSeconds());
        spec.setSuccessfulJobsHistoryLimit(server.getServer().getSuccessfulJobsHistoryLimit());
        spec.setFailedJobsHistoryLimit(server.getServer().getFailedJobsHistoryLimit());
        spec.setSuspend(server.getServer().getSuspend());

        spec.setJobTemplate(buildJobTemplateSpecV1(server, namespace));

        return spec;
    }

    private static V1JobTemplateSpec buildJobTemplateSpecV1(ServerDetailVO server, String namespace) throws Exception {
        V1JobTemplateSpec template = new V1JobTemplateSpec();

        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setLabels(buildServerDefaultLabel(server, false));
        template.setMetadata(meta);

        template.setSpec(buildJobSpecV1(server, namespace, true));

        return template;
    }

    public static V1Job buildJobV1(ServerDetailVO server, String namespace) throws Exception {
        V1Job job = new V1Job();

        job.setApiVersion(String.format("%s/%s", K8sApiGroupType.BATCH.getValue(), K8sApiType.V1.getValue()));
        job.setKind(K8sApiKindType.JOB.getValue());

        job.setMetadata(buildServerMetaV1(server, namespace));

        job.setSpec(buildJobSpecV1(server, namespace, false));

        return job;
    }

    private static V1JobSpec buildJobSpecV1(ServerDetailVO server, String namespace, boolean isCronJob) throws Exception {
        V1JobSpec spec = new V1JobSpec();

        if (!isCronJob) {
            spec.manualSelector(Boolean.TRUE);
            V1LabelSelector labelSelector = new V1LabelSelector();
            labelSelector.setMatchLabels(buildServerDefaultLabel(server, true));
            spec.setSelector(labelSelector);
        }

        spec.setCompletions(server.getServer().getComputeTotal());
        spec.setParallelism(server.getServer().getParallelism());
        spec.setActiveDeadlineSeconds(server.getServer().getActiveDeadlineSeconds());
        spec.setBackoffLimit(server.getServer().getBackoffLimit());
        spec.setTtlSecondsAfterFinished(server.getServer().getTtlSecondsAfterFinished());

        spec.setTemplate(buildPodTemplateSpecV1(server, namespace));

        return spec;
    }

    private static V1PodTemplateSpec buildPodTemplateSpecV1(ServerDetailVO serverParam, String namespace) throws Exception {

        JSON k8sJson = new JSON();
        V1PodTemplateSpec template = new V1PodTemplateSpec();

        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setLabels(buildServerDefaultLabel(serverParam, false));
        if (MapUtils.isNotEmpty(serverParam.getServer().getAnnotations())) {
            for (Map.Entry<String, String> annoEntryRow : serverParam.getServer().getAnnotations().entrySet()) {
                if (!StringUtils.equalsAny(annoEntryRow.getKey(), KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO)) {
                    meta.putAnnotationsItem(annoEntryRow.getKey(), annoEntryRow.getValue());
                }
            }
        }
        // Multi-nic
        if (CollectionUtils.isNotEmpty(serverParam.getServer().getPodNetworks())) {
            meta.putAnnotationsItem(KubeConstants.META_ANNOTATIONS_CNI_NETWORKS, JsonUtils.toGson(serverParam.getServer().getPodNetworks()));
        }
        template.setMetadata(meta);

        V1PodSpec spec = new V1PodSpec();
        /**
         * JOB RestartPolicy : OnFailure 를 기본으로 셋팅
         */
        if (EnumSet.of(WorkloadType.JOB_SERVER, WorkloadType.CRON_JOB_SERVER).contains(WorkloadType.valueOf(serverParam.getServer().getWorkloadType()))) {
            spec.setRestartPolicy(RestartPolicyType.OnFailure.getCode());
        } else {
            spec.setRestartPolicy(RestartPolicyType.Always.getCode());
        }

        spec.setTerminationGracePeriodSeconds(serverParam.getServer().getTerminationGracePeriodSeconds());
        if(StringUtils.isNotBlank(serverParam.getServer().getHostname())){
            spec.setHostname(serverParam.getServer().getHostname());
        }
        if(StringUtils.isNotBlank(serverParam.getServer().getServiceAccountName())){
            spec.setServiceAccount(serverParam.getServer().getServiceAccountName());
            spec.setServiceAccountName(serverParam.getServer().getServiceAccountName());
        }
        if(StringUtils.isNoneBlank(serverParam.getServer().getNodeSelectorKey(), serverParam.getServer().getNodeSelectorValue())){
            // TODO: 여러개 넣을 수 있도록 수정 필요
            Map<String, String> nodeSelector = Maps.newHashMap();
            nodeSelector.put(serverParam.getServer().getNodeSelectorKey(), serverParam.getServer().getNodeSelectorValue());
            spec.setNodeSelector(nodeSelector);
//            spec.putNodeSelectorItem(serverParam.getServer().getNodeSelectorKey(), serverParam.getServer().getNodeSelectorValue());
        } else {
            spec.setNodeSelector(null);
        }
        // container들중 GPU 사용 하는게 하나라도 있으면 GPU 관련 nodeAffinity 셋팅
        boolean useGpu = serverParam.getServer().getContainers().stream().anyMatch( c -> Boolean.TRUE.equals(c.getResources().getUseGpu()) );

        // affinity
        if (serverParam.getServer().getAffinity() != null) {
            // affinity 정보 deserialize
            V1Affinity affinity = k8sJson.deserialize(k8sJson.serialize(serverParam.getServer().getAffinity()), V1Affinity.class);

            // GPU 사용 nodeAffinity 로 추가
            if (useGpu) {
                // GPU 관련 affinity 객체를 단계별로 생성
                V1NodeSelector nodeSelector = new V1NodeSelector();
                V1NodeSelectorTerm nodeSelectorTerm = new V1NodeSelectorTerm();
                V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
                nodeSelectorRequirement = nodeSelectorRequirement.key(KubeConstants.RESOURCES_GPU).operator(KubeConstants.TOLERATION_OPERATOR_EXISTS);
                nodeSelectorTerm = nodeSelectorTerm.addMatchExpressionsItem(nodeSelectorRequirement);
                nodeSelector = nodeSelector.addNodeSelectorTermsItem(nodeSelectorTerm);

                // nodeAffinity 가 존재한다면
                if (affinity.getNodeAffinity() != null) {
                    // RequiredDuringSchedulingIgnoredDuringExecution 와 NodeSelectorTerms 가 존재한다면
                    if (affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution() != null
                            && CollectionUtils.isNotEmpty(affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms())
                    ) {
                        // 설정된 정보에 GPU 관련한 정보가 존재하는 지 체크하여
                        boolean isExist = false;
                        for (V1NodeSelectorTerm termRow : affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms()) {
                            if (CollectionUtils.isNotEmpty(termRow.getMatchExpressions())) {
                                Optional<V1NodeSelectorRequirement> requirementOptional = termRow.getMatchExpressions().stream()
                                        .filter(me -> (
                                                StringUtils.equals(me.getKey(), KubeConstants.RESOURCES_GPU) && StringUtils.equals(me.getOperator(), KubeConstants.TOLERATION_OPERATOR_EXISTS)
                                        )).findFirst();
                                if (requirementOptional.isPresent()) {
                                    isExist = true;
                                    break;
                                }
                            }
                        }
                        // 없을 시에 GPU 관련 정보 셋팅
                        if (!isExist) {
                            affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().addNodeSelectorTermsItem(nodeSelectorTerm);
                        }
                    }
                    // RequiredDuringSchedulingIgnoredDuringExecution 와 NodeSelectorTerms 가 존재하지 않는다면 RequiredDuringSchedulingIgnoredDuringExecution 값 셋팅
                    else {
                        affinity.getNodeAffinity().setRequiredDuringSchedulingIgnoredDuringExecution(nodeSelector);
                    }
                }
                // nodeAffinity 가 존재하지 않는다면 새로 생성하여 셋팅
                else {
                    V1NodeAffinity nodeAffinity = new V1NodeAffinity();
                    affinity.nodeAffinity(nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution(nodeSelector));
                }
            } else {
                // GPU 설정시 추가되는 nodeAffinity가 있다면 제거
                if (affinity.getNodeAffinity() != null) {
                    if (affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution() != null
                            && CollectionUtils.isNotEmpty(affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms())
                    ) {
                        int nodeSelectorCnt = affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().size();
                        if (nodeSelectorCnt == 1) {
                            if ( affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().get(0).getMatchExpressions().stream().anyMatch(me -> (
                                    StringUtils.equals(me.getKey(), KubeConstants.RESOURCES_GPU) && StringUtils.equals(me.getOperator(), KubeConstants.TOLERATION_OPERATOR_EXISTS)
                            )) ) {
                                affinity.getNodeAffinity().setRequiredDuringSchedulingIgnoredDuringExecution(null);
                            }
                        } else if (nodeSelectorCnt > 1) {
                            for (V1NodeSelectorTerm termRow : affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms()) {
                                if (CollectionUtils.isNotEmpty(termRow.getMatchExpressions())) {
                                    termRow.getMatchExpressions()
                                            .removeIf(me -> (
                                                    StringUtils.equals(me.getKey(), KubeConstants.RESOURCES_GPU) && StringUtils.equals(me.getOperator(), KubeConstants.TOLERATION_OPERATOR_EXISTS)
                                            ));
                                }
                            }

                            if (CollectionUtils.isNotEmpty(affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms())) {
                                affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms()
                                        .removeIf(nst -> (CollectionUtils.isEmpty(nst.getMatchExpressions()) && CollectionUtils.isEmpty(nst.getMatchFields())));
                            }
                        }
                    }
                }
            }
            spec.setAffinity(affinity);
        } else {
            if(useGpu) {
//                spec.addTolerationsItem(new V1Toleration().key(KubeConstants.RESOURCES_GPU).operator(KubeConstants.TOLERATION_OPERATOR_EXISTS).effect(KubeConstants.TOLERATION_EFFECT_NO_SCHEDULE));
                V1Affinity affinity = new V1Affinity();
                V1NodeAffinity nodeAffinity = new V1NodeAffinity();
                V1NodeSelector nodeSelector = new V1NodeSelector();
                V1NodeSelectorTerm nodeSelectorTerm = new V1NodeSelectorTerm();
                V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
                affinity.nodeAffinity(
                        nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution(
                                nodeSelector.addNodeSelectorTermsItem(
                                        nodeSelectorTerm.addMatchExpressionsItem(
                                                nodeSelectorRequirement.key(KubeConstants.RESOURCES_GPU).operator(KubeConstants.TOLERATION_OPERATOR_EXISTS)))));
                spec.setAffinity(affinity);
            }
        }
        // toleration
        if (CollectionUtils.isNotEmpty(serverParam.getServer().getTolerations())) {
            for (TolerationVO tolerationRow : serverParam.getServer().getTolerations()) {
                spec.addTolerationsItem(k8sJson.deserialize(k8sJson.serialize(tolerationRow), V1Toleration.class));
            }
        }

        // volume
        List<V1Volume> volumes = buildVolumesV1(serverParam);
        if (volumes != null) {
            spec.setVolumes(volumes);
        }


        // init container
        if (CollectionUtils.isNotEmpty(serverParam.getServer().getInitContainers())) {
            List<V1Container> initContainers = buildContainersV1(serverParam, true); // init container
            if (CollectionUtils.isNotEmpty(initContainers)) {
                spec.setInitContainers(initContainers);
            }
        }

        // container
        List<V1Container> containers = buildContainersV1(serverParam, false); // regular container
        if (CollectionUtils.isNotEmpty(containers)) {
            spec.setContainers(containers);
        }

        // registry url
        String registryUrl = ResourceUtil.getRegistryUrl();

        boolean usePrivate = false;
        List<ContainerVO> allContainers = new ArrayList<>();
        ResourceUtil.mergeContainer(allContainers, serverParam.getServer().getInitContainers(), serverParam.getServer().getContainers());
        for (ContainerVO c : allContainers) {
            if (StringUtils.equalsIgnoreCase("Y", c.getPrivateRegistryYn())
                    || StringUtils.startsWith(c.getImageName(), registryUrl)
                    || StringUtils.startsWith(c.getFullImageName(), registryUrl)
            ) {
                usePrivate = true;
                break;
            }
        }
        if (usePrivate) {
            V1LocalObjectReference ref = new V1LocalObjectReference();
            ref.setName(CryptoUtils.decryptAES(serverParam.getServer().getImageSecret()));
            if (CollectionUtils.isNotEmpty(spec.getImagePullSecrets())) {
                if (!spec.getImagePullSecrets().contains(ref)) {
                    spec.addImagePullSecretsItem(ref);
                }
            } else {
                spec.addImagePullSecretsItem(ref);
            }
        }

        // ImagePullSecrets 처리
        // 시스템에서 추가한 것과 별개로 사용자가 추가한 것이 있다면 추가
        if (CollectionUtils.isNotEmpty(serverParam.getServer().getImagePullSecrets())) {
            List<V1LocalObjectReference> updateImagePullSecrets = k8sJson.deserialize(k8sJson.serialize(serverParam.getServer().getImagePullSecrets()), new TypeToken<List<V1LocalObjectReference>>(){}.getType());
            if (CollectionUtils.isNotEmpty(spec.getImagePullSecrets())) {
                for (V1LocalObjectReference lorRow : updateImagePullSecrets) {
                    if (!spec.getImagePullSecrets().contains(lorRow)) {
                        spec.addImagePullSecretsItem(lorRow);
                    }
                }
            } else {
                spec.setImagePullSecrets(updateImagePullSecrets);
            }
        }

        template.setSpec(spec);

        return template;
    }

    private static List<V1Volume> buildVolumesV1(ServerDetailVO serverParam) {
        if (serverParam.getVolumes() == null || serverParam.getVolumes().size() == 0) {
            return null;
        }

        List<V1Volume> volumes = new ArrayList<>();
        QuantityFormatter quantityFormatter = new QuantityFormatter();
        for (ContainerVolumeVO cv : serverParam.getVolumes()) {
            V1Volume v = new V1Volume();
            v.setName(cv.getVolumeName());
            switch (cv.getVolumeType()) {
                case EMPTY_DIR: {
                    V1EmptyDirVolumeSource s = new V1EmptyDirVolumeSource();
                    if (cv.getUseMemoryYn() != null && cv.getUseMemoryYn().compareToIgnoreCase("y") == 0) {
                        cv.setEmptyDirMedium("Memory");
                        s.setMedium("Memory");
                    }
                    if (StringUtils.isNotBlank(cv.getSizeLimit())) {
                        s.setSizeLimit(new Quantity(String.format("%sGi", cv.getSizeLimit())));
                    }
                    v.setEmptyDir(s);
                }
                break;

                case HOST_PATH: {
                    V1HostPathVolumeSource s = new V1HostPathVolumeSource();
                    s.setPath(cv.getHostPath());
                    if (StringUtils.isNotBlank(cv.getHostPathType())) {
                        s.setType(cv.getHostPathType());
                    }
                    v.setHostPath(s);
                }
                break;

                case CONFIG_MAP: {
                    V1ConfigMapVolumeSource s = new V1ConfigMapVolumeSource();
                    s.setName(cv.getConfigMapName());
                    if(cv.getDefaultMode() != null){
                        s.setDefaultMode(Integer.parseInt(cv.getDefaultMode().toString(), 8)); // 8진수 -> 10진수
                    }
                    s.setOptional(cv.getOptional());
                    if (CollectionUtils.isNotEmpty(cv.getItems())) {
                        for (ContainerVolumeKeyToPathVO keyToPathRow : cv.getItems()) {
                            V1KeyToPath keyToPath = new V1KeyToPath();
                            if (keyToPathRow.getMode() != null) {
                                keyToPath.setMode(Integer.parseInt(keyToPathRow.getMode().toString(), 8)); // 8진수 -> 10진수
                            }
                            keyToPath.setKey(keyToPathRow.getKey());
                            keyToPath.setPath(keyToPathRow.getPath());
                            s.addItemsItem(keyToPath);
                        }
                    }
                    v.setConfigMap(s);
                }
                break;

                case SECRET: {
                    V1SecretVolumeSource s = new V1SecretVolumeSource();
                    s.setSecretName(cv.getSecretName());
                    if(cv.getDefaultMode() != null){
                        s.setDefaultMode(Integer.parseInt(cv.getDefaultMode().toString(), 8)); // 8진수 -> 10진수
                    }
                    s.setOptional(cv.getOptional());
                    if (CollectionUtils.isNotEmpty(cv.getItems())) {
                        for (ContainerVolumeKeyToPathVO keyToPathRow : cv.getItems()) {
                            V1KeyToPath keyToPath = new V1KeyToPath();
                            if (keyToPathRow.getMode() != null) {
                                keyToPath.setMode(Integer.parseInt(keyToPathRow.getMode().toString(), 8)); // 8진수 -> 10진수
                            }
                            keyToPath.setKey(keyToPathRow.getKey());
                            keyToPath.setPath(keyToPathRow.getPath());
                            s.addItemsItem(keyToPath);
                        }
                    }
                    v.setSecret(s);
                }
                break;

                case PERSISTENT_VOLUME_LINKED:
                case PERSISTENT_VOLUME_STATIC:
                case PERSISTENT_VOLUME: { // bind dynamic persistent volume
                    if (StringUtils.isNotBlank(cv.getPersistentVolumeClaimName())) {
                        V1PersistentVolumeClaimVolumeSource s = new V1PersistentVolumeClaimVolumeSource();
                        s.setClaimName(cv.getPersistentVolumeClaimName());
                        s.setReadOnly(cv.getReadOnly());
                        v.setPersistentVolumeClaim(s);
                    } else {
                        continue;
                    }
                }
                break;

                default: {
                    log.warn("Unsupported volume type: {}", cv.getVolumeType());
                    continue;
                }
            }

            volumes.add(v);
        }
        return volumes;
    }

    private static List<V1Container> buildContainersV1(ServerDetailVO serverParam, boolean isInitContainer) throws Exception {
        List<V1Container> containers = new ArrayList<>();
        String baseName = ResourceUtil.getUniqueName(serverParam.getComponent().getComponentName());
        List<ContainerVO> containerVOS;
        if (isInitContainer) {
            containerVOS = serverParam.getServer().getInitContainers();
        }else {
            containerVOS = serverParam.getServer().getContainers();
        }
        int index = 1;
        for (ContainerVO cv : containerVOS) {

            V1Container c = new V1Container();
            c.setName(ResourceUtil.getFormattedName(baseName, index++));
            if(StringUtils.isNotBlank(cv.getContainerName())){
                c.setName(cv.getContainerName());
            }

            /**
             * 2021.01.15
             * JPN Region 마이그레이션 진행중 이슈 발생하여 수정
             * - 현재 ImageName, ImageTag는 Deprecated 되고 fullImageName만 사용중인데
             *   JPN Region의 기존 스냅샷중 fullImageName이 존재하지 않는 스냅샷이 있음 -> 마이그레이션중 ImageTag가 Null로 입려되는 문제 발생
             * - fullImageName이 없을 경우 ImageName + ImageTag 조합을 사용하도록 함.
             */
            if(StringUtils.isNotBlank(cv.getFullImageName())) {
                c.setImage(cv.getFullImageName());
            }
            else {
                if(StringUtils.isNotBlank(cv.getImageName()) && StringUtils.isNotBlank(cv.getImageTag())) {
                    c.setImage(cv.getImageName() + ":" + cv.getImageTag());
                }
                else {
                    c.setImage(cv.getFullImageName());
                }
            }

            // Image pull policy. One of Always, Never, IfNotPresent. Defaults to Always if :latest tag is specified, or IfNotPresent otherwise. Cannot be updated. More info: https://kubernetes.io/docs/concepts/containers/images#updating-images
            if (StringUtils.isNotBlank(cv.getImagePullPolicy())) {
                c.setImagePullPolicy(ImagePullPolicyType.valueOf(cv.getImagePullPolicy()).getCode());
            }

            // process command & args
//            if (StringUtils.isNotBlank(cv.getCommand())) {
//                c.setCommand(K8sSpecFactory.setTrimCommand(cv.getCommand()));
//            }
//            if (StringUtils.isNotBlank(cv.getArguments())) {
//                c.setArgs(K8sSpecFactory.setTrimCommand(cv.getArguments()));
//            }
            if (CollectionUtils.isNotEmpty(cv.getCmds())) {
                c.setCommand(cv.getCmds());
            }
            if (CollectionUtils.isNotEmpty(cv.getArgs())) {
                c.setArgs(cv.getArgs());
            }

            // ports
            if (!CollectionUtils.isEmpty(cv.getContainerPorts())) {
                c.setPorts(buildContainerPortsV1(cv.getContainerPorts()));
            }
//            if (!CollectionUtils.isEmpty(cv.getServicePorts())) {
//                c.setPorts(buildContainerPortsV1(cv.getServicePorts()));
//            }

            // envs
            if (!CollectionUtils.isEmpty(cv.getContainerEnvVars())) {
                c.setEnv(buildContainerEnvsV1(cv.getContainerEnvVars()));
            }

            // volumeMounts
            if (!CollectionUtils.isEmpty(cv.getVolumeMounts())) {
                c.setVolumeMounts(buildVolumeMountsV1(cv.getVolumeMounts()));
            }

            // security context
            if (cv.getSecurityContext() != null) {
                c.setSecurityContext(buildSecurityContextV1(cv.getSecurityContext()));
            }

            // resources
            if (cv.getResources() != null) {
                c.setResources(buildResourceRequirementsV1(cv.getResources()));
            }

            // liveness probes
            if (cv.getLivenessProbe() != null) {
                c.setLivenessProbe(buildContainerProveV1(cv.getLivenessProbe()));
            }

            // readness probe
            if (!isInitContainer) {
                if (cv.getReadinessProbe() != null) {
                    c.setReadinessProbe(buildContainerProveV1(cv.getReadinessProbe()));
                }
            }

            // lifecycle, added 2019.04.17, coolingi
            if (cv.getLifecycle() != null){
                c.setLifecycle(buildContainerLifecycleV1(cv.getLifecycle()));
            }

            containers.add(c);
        }
        return containers;
    }

    private static List<V1ContainerPort> buildContainerPortsV1(List<ContainerPortVO> portsParams) {
        if (CollectionUtils.isEmpty(portsParams)) {
            return null;
        }
        List<Integer> used = new ArrayList<>();
        List<V1ContainerPort> ports = new ArrayList<>();
        for (ContainerPortVO cp : portsParams) {
            if (cp.getHostPort() != null) {
                if (used.contains(cp.getHostPort())) {
                    log.warn("skip used port: {}", cp.getHostPort());
                    continue;
                } else {
                    used.add(cp.getHostPort());
                }
            }

            V1ContainerPort p = new V1ContainerPort();
            p.setName(cp.getName());
            p.setContainerPort(cp.getContainerPort());
            p.setHostPort(cp.getHostPort());
            p.setProtocol(cp.getProtocol());
            p.setHostIP(cp.getHostIP());
            ports.add(p);
        }
        return ports;
    }

    public static List<V1EnvVar> buildContainerEnvsV1(List<ContainerEnvVarVO> envVarParams) {
        if (CollectionUtils.isEmpty(envVarParams)) {
            return null;
        }
        List<V1EnvVar> envVars = new ArrayList<>();
        for (ContainerEnvVarVO envVarParam : envVarParams) {
            V1EnvVar ev = new V1EnvVar();
            ev.setName(envVarParam.getKey());
            if (StringUtils.isNotBlank(envVarParam.getConfigMapName())) {
                V1ConfigMapKeySelector selector = new V1ConfigMapKeySelector();
                selector.setName(envVarParam.getConfigMapName());
                selector.setKey(envVarParam.getConfigMapKey());

                V1EnvVarSource src = new V1EnvVarSource();
                src.setConfigMapKeyRef(selector);

                ev.setValueFrom(src);
            } else if (StringUtils.isNotBlank(envVarParam.getSecretName())) {
                V1SecretKeySelector selector = new V1SecretKeySelector();
                selector.setName(envVarParam.getSecretName());
                selector.setKey(envVarParam.getSecretKey());

                V1EnvVarSource src = new V1EnvVarSource();
                src.setSecretKeyRef(selector);

                ev.setValueFrom(src);
            } else if (envVarParam.getFieldRef() != null) {
                V1ObjectFieldSelector selector = new V1ObjectFieldSelector();
                if (StringUtils.isNotBlank(envVarParam.getFieldRef().getApiVersion())) {
                    selector.setApiVersion(envVarParam.getFieldRef().getApiVersion());
                }
                selector.setFieldPath(envVarParam.getFieldRef().getFieldPath());

                V1EnvVarSource src = new V1EnvVarSource();
                src.setFieldRef(selector);

                ev.setValueFrom(src);
            } else if (envVarParam.getResourceFieldRef() != null) {
                V1ResourceFieldSelector selector = new V1ResourceFieldSelector();
                if (StringUtils.isNotBlank(envVarParam.getResourceFieldRef().getContainerName())) {
                    selector.setContainerName(envVarParam.getResourceFieldRef().getContainerName());
                }
                if (StringUtils.isNotBlank(envVarParam.getResourceFieldRef().getDivisor())) {
                    selector.setDivisor(new Quantity(envVarParam.getResourceFieldRef().getDivisor()));
                }
                selector.setResource(envVarParam.getResourceFieldRef().getResource());

                V1EnvVarSource src = new V1EnvVarSource();
                src.setResourceFieldRef(selector);

                ev.setValueFrom(src);
            } else {
                ev.setValue(envVarParam.getValue());
            }
            envVars.add(ev);
        }
        return envVars;
    }

    private static List<V1VolumeMount> buildVolumeMountsV1(List<VolumeMountVO> vmParams) {
        if (CollectionUtils.isEmpty(vmParams)) {
            return null;
        }
        List<V1VolumeMount> mounts = new ArrayList<>();
        for (VolumeMountVO vm : vmParams) {
            V1VolumeMount v = new V1VolumeMount();
            v.setName(vm.getVolumeName());
            v.setMountPath(vm.getContainerPath());
            if (StringUtils.isNotBlank(vm.getSubPath())) {
                v.setSubPath(vm.getSubPath());
            }
            if (StringUtils.isNotBlank(vm.getSubPathExpr())) {
                v.setSubPathExpr(vm.getSubPathExpr());
            }
            if (StringUtils.isNotBlank(vm.getReadOnlyYn()) && StringUtils.equalsIgnoreCase("Y", vm.getReadOnlyYn())) {
                v.setReadOnly(true);
            }
            mounts.add(v);
        }
        return mounts;
    }

    private static V1SecurityContext buildSecurityContextV1(SecurityContextVO scv) {
        if (scv == null) {
            return null;
        }
        V1SecurityContext sc = new V1SecurityContext();
        sc.setAllowPrivilegeEscalation(scv.getAllowPrivilegeEscalation());
        if (scv.getCapabilities() != null) {
            CapabilitiesVO cv = scv.getCapabilities();
            V1Capabilities c = new V1Capabilities();
            c.setAdd(cv.getAdd());
            c.setDrop(cv.getDrop());
            sc.setCapabilities(c);
        }
        sc.setPrivileged(scv.getPrivileged());
        sc.setProcMount(scv.getProcMount());
        sc.setReadOnlyRootFilesystem(scv.getReadOnlyRootFilesystem());
        sc.setRunAsGroup(scv.getRunAsGroup());
        sc.setRunAsNonRoot(scv.getRunAsNonRoot());
        sc.setRunAsUser(scv.getRunAsUser());
        if (scv.getSeLinuxOptions() != null) {
            SELinuxOptionsVO slov = scv.getSeLinuxOptions();
            V1SELinuxOptions slo = new V1SELinuxOptions();
            slo.setLevel(slov.getLevel());
            slo.setType(slov.getType());
            slo.setRole(slov.getRole());
            slo.setUser(slov.getUser());
            sc.setSeLinuxOptions(slo);
        }
        if (scv.getWindowsOptions() != null) {
            sc.setWindowsOptions(new V1WindowsSecurityContextOptions());
            sc.getWindowsOptions().setGmsaCredentialSpec(scv.getWindowsOptions().getGmsaCredentialSpec());
            sc.getWindowsOptions().setGmsaCredentialSpecName(scv.getWindowsOptions().getGmsaCredentialSpecName());
            sc.getWindowsOptions().setRunAsUserName(scv.getWindowsOptions().getRunAsUserName());
        }

        return sc;
    }

    public static V1ResourceRequirements buildResourceRequirementsV1(ContainerResourcesVO resources) {
        if (resources == null) {
            return null;
        }

        V1ResourceRequirements rr = new V1ResourceRequirements();

        Map<String, Quantity> requests = new HashMap<>();
        QuantityFormatter quantityFormatter = new QuantityFormatter();
        if (resources.getRequests() != null) {
            if (resources.getRequests().getCpu() != null) {
                requests.put(KubeConstants.RESOURCES_CPU, quantityFormatter.parse(String.format("%fm", resources.getRequests().getCpu())));
            }
            if (resources.getRequests().getMemory() != null) {
                requests.put(KubeConstants.RESOURCES_MEMORY, quantityFormatter.parse(String.format("%fMi", resources.getRequests().getMemory())));
            }
            if (resources.getRequests().getGpu() != null) {
                requests.put(KubeConstants.RESOURCES_GPU, quantityFormatter.parse(String.format("%f", resources.getRequests().getGpu())));
            }
            if (MapUtils.isNotEmpty(resources.getRequests().getNetwork())) {
                for (Map.Entry<String, String> networkRow : resources.getRequests().getNetwork().entrySet()) {
                    requests.put(networkRow.getKey(), quantityFormatter.parse(networkRow.getValue()));
                }
            }
        }

        if (requests.size() > 0) {
            rr.setRequests(requests);
        }

        Map<String, Quantity> limits = new HashMap<>();
        if (resources.getLimits() != null) {
            if (resources.getLimits().getCpu() != null) {
                limits.put(KubeConstants.RESOURCES_CPU, quantityFormatter.parse(String.format("%fm", resources.getLimits().getCpu())));
            }
            if (resources.getLimits().getMemory() != null) {
                limits.put(KubeConstants.RESOURCES_MEMORY, quantityFormatter.parse(String.format("%fMi", resources.getLimits().getMemory())));
            }
            if (resources.getLimits().getGpu() != null) {
                limits.put(KubeConstants.RESOURCES_GPU, quantityFormatter.parse(String.format("%f", resources.getLimits().getGpu())));
            }
            if (MapUtils.isNotEmpty(resources.getLimits().getNetwork())) {
                for (Map.Entry<String, String> networkRow : resources.getLimits().getNetwork().entrySet()) {
                    limits.put(networkRow.getKey(), quantityFormatter.parse(networkRow.getValue()));
                }
            }
        }
        if (limits.size() > 0) {
            rr.setLimits(limits);
        }

        return rr;
    }

    private static V1Probe buildContainerProveV1(ContainerProbeVO cpv) throws Exception {
        if (cpv == null) {
            return null;
        }

        V1Probe p = new V1Probe();
        String value;
        if (cpv.getType() == ProbeType.HTTPGET) {
            V1HTTPGetAction action = new V1HTTPGetAction();

            value = cpv.getHttpGetScheme();
            action.setScheme(StringUtils.isBlank(value) ? "HTTP" :  StringUtils.upperCase(cpv.getHttpGetScheme()));

            value = cpv.getHttpGetHost();
            if (StringUtils.isNotBlank(value)) {
                action.setHost(value);
            }

            /** 2020.01.10 : Redion : targetPort가 규격을 준수하는지 체크 : 상세 내용은 호출 메서드 참고. **/
            if (StringUtils.isNotBlank(cpv.getHttpGetPort())) {
                ResourceUtil.isValidPortRule(cpv.getHttpGetPort());
                // 숫자로만 이루어져 있으면 Integer로, 아니면 String으로 입력..
                if(NumberUtils.isDigits(cpv.getHttpGetPort())) {
                    action.setPort(new IntOrString(Integer.parseInt(cpv.getHttpGetPort())));
                }
                else {
                    action.setPort(new IntOrString(cpv.getHttpGetPort()));
                }
            }


            value = cpv.getHttpGetPath();
            if (StringUtils.isNotBlank(value)) {
                action.setPath(value);
            }

            // Http header
            if (CollectionUtils.isNotEmpty(cpv.getHttpGetHeaders())) {
                for (HTTPHeaderVO item : cpv.getHttpGetHeaders()) {
                    action.addHttpHeadersItem(new V1HTTPHeader().name(item.getName()).value(item.getValue()));
                }
            }

            p.setHttpGet(action);
        } else if(cpv.getType() == ProbeType.TCPSOCKET) {
            V1TCPSocketAction action = new V1TCPSocketAction();
            /** 2020.01.10 : Redion : targetPort가 규격을 준수하는지 체크 : 상세 내용은 호출 메서드 참고. **/
            ResourceUtil.isValidPortRule(cpv.getTcpSocketPort());
            // 숫자로만 이루어져 있으면 Integer로, 아니면 String으로 입력..
            if(NumberUtils.isDigits(cpv.getHttpGetPort())) {
                action.setPort(new IntOrString(Integer.parseInt(cpv.getTcpSocketPort())));
            }
            else {
                action.setPort(new IntOrString(cpv.getTcpSocketPort()));
            }
            p.setTcpSocket(action);
        } else if (cpv.getType() == ProbeType.EXEC) {
            V1ExecAction action = new V1ExecAction();
            if (CollectionUtils.isNotEmpty(cpv.getExecCmds())) {
                action.setCommand(cpv.getExecCmds());
            } else {
                value = cpv.getExecCommand();
                if (StringUtils.isNotBlank(value)) {
                    action.setCommand(K8sSpecFactory.setTrimCommand(value));
                }
            }
            p.setExec(action);
        } else {
            log.warn("Unsupported probe type: {}", cpv.getType());
            return null;
        }

        p.setInitialDelaySeconds(cpv.getInitialDelaySeconds());
        p.setPeriodSeconds(cpv.getPeriodSeconds());
        p.setSuccessThreshold(cpv.getSuccessThreshold());
        p.setTimeoutSeconds(cpv.getTimeoutSeconds());
        p.setFailureThreshold(cpv.getFailureThreshold());

        return p;
    }

    private static V1Lifecycle buildContainerLifecycleV1(ContainerLifecycleVO clv) throws Exception {
        if (clv == null) {
            return null;
        }

        String lifecycleJson = JsonUtils.toGson(clv);
        V1Lifecycle lifecycle = JsonUtils.fromGson(lifecycleJson, V1Lifecycle.class);

        /** 2020.01.10 : Redion : HTTPGet / TCP port가 K8s 규격을 준수하는지 체크 로직 추가 **/
        if(lifecycle.getPostStart() != null) {
            lifecycle.setPostStart(buildContainerLifecycleHandlerV1(lifecycle.getPostStart()));
        }

        if(lifecycle.getPreStop() != null) {
            lifecycle.setPreStop(buildContainerLifecycleHandlerV1(lifecycle.getPreStop()));
        }

        return lifecycle;
    }

    /**
     * LifeCycle Hook의 port에 대한 Validation 체크 : 2020.01.10 : Redion
     * @param v1LifecycleHandler
     * @return
     * @throws Exception
     */
    private static V1LifecycleHandler buildContainerLifecycleHandlerV1(V1LifecycleHandler v1LifecycleHandler) throws Exception {
        if(v1LifecycleHandler.getExec() != null) {
            // 그대로 유지..
        }

        if(v1LifecycleHandler.getHttpGet() != null) {
            /** 2020.01.10 : Redion : targetPort가 규격을 준수하는지 체크 : 상세 내용은 호출 메서드 참고. **/
            ResourceUtil.isValidPortRule(v1LifecycleHandler.getHttpGet().getPort().toString());
            // 숫자로만 이루어져 있으면 Integer로, 아니면 String으로 입력..
            if(NumberUtils.isDigits(v1LifecycleHandler.getHttpGet().getPort().toString())) {
                v1LifecycleHandler.getHttpGet().setPort(new IntOrString(Integer.parseInt(v1LifecycleHandler.getHttpGet().getPort().toString())));
            }
            else {
                v1LifecycleHandler.getHttpGet().setPort(new IntOrString(v1LifecycleHandler.getHttpGet().getPort().toString()));
            }
        }

        if(v1LifecycleHandler.getTcpSocket() != null) {
            /** 2020.01.10 : Redion : targetPort가 규격을 준수하는지 체크 : 상세 내용은 호출 메서드 참고. **/
            ResourceUtil.isValidPortRule(v1LifecycleHandler.getTcpSocket().getPort().toString());
            // 숫자로만 이루어져 있으면 Integer로, 아니면 String으로 입력..
            if(NumberUtils.isDigits(v1LifecycleHandler.getTcpSocket().getPort().toString())) {
                v1LifecycleHandler.getTcpSocket().setPort(new IntOrString(Integer.parseInt(v1LifecycleHandler.getTcpSocket().getPort().toString())));
            }
            else {
                v1LifecycleHandler.getTcpSocket().setPort(new IntOrString(v1LifecycleHandler.getTcpSocket().getPort().toString()));
            }
        }

        return v1LifecycleHandler;
    }

    /**
     * HorizontalPodAutoscaler Spec Builder (V2beta1)
     *
     * @param hpa
     * @param ns
     * @param componentName
     * @param name
     * @param k8sApiVerKindType
     * @return
     * @throws Exception
     */
    public static V2beta1HorizontalPodAutoscaler buildHpaV2beta1(HpaGuiVO hpa, String ns, String componentName, String name, K8sApiVerKindType k8sApiVerKindType) throws Exception {
        boolean useExtensionMetric = (hpa.getMetrics() != null && hpa.getMetrics().size() > 0); // V2Beta1 Spec 인지 확인

        V2beta1HorizontalPodAutoscalerSpec spec = new V2beta1HorizontalPodAutoscalerSpec();
        spec.setMaxReplicas(hpa.getMaxReplicas());
        spec.setMinReplicas(hpa.getMinReplicas());
        /**
         * TODO: Redion: CrossVersionObjectReference 정보를 넣을때에 Deployment의 ApiVersion을 확인하여 올바르게 정보를 넣어줄 필요가 있음
         * 현재는 extension/v1beta1으로만 Deployment 하고 있으므로 괜찮지만 추후 Deployment 버전이 올라갈 경우 Side effect 예상됨
         * 현재로서는 딱히 정보를 알아올 방법이 없어 이전과 같이 extension/v1beta1으로 고정하여 처리함
         * redion (2018.08.08)
         */
        V2beta1CrossVersionObjectReference targetRef = new V2beta1CrossVersionObjectReference();
        targetRef.setApiVersion(String.format("%s/%s", k8sApiVerKindType.getGroupType().getValue(), k8sApiVerKindType.getApiType().getValue()));
        targetRef.setKind(k8sApiVerKindType.getKindType().getValue());
        targetRef.setName(componentName);

        spec.setScaleTargetRef(targetRef);

        if (useExtensionMetric) {
            QuantityFormatter quantityFormatter = new QuantityFormatter();
            JSON k8sJson = new JSON();
            List<V2beta1MetricSpec> metrics = new ArrayList<>();

            for (MetricVO metric : hpa.getMetrics()) {
                V2beta1MetricSpec metricSpec = new V2beta1MetricSpec();
                metricSpec.setType(metric.getType().getCode());

                switch (metric.getType()) {
                    case Resource:
                        V2beta1ResourceMetricSource resMetric = new V2beta1ResourceMetricSource();
                        resMetric.setName(metric.getResourceName());
                        if (metric.getTargetType() == MetricTargetType.AverageValue) {
                            if (metric.getType() == MetricType.Resource) {
                                if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_CPU, metric.getResourceName())) {
                                    resMetric.setTargetAverageValue(quantityFormatter.parse(String.format("%sm", metric.getTargetAverageValue())));
                                } else if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_MEMORY, metric.getResourceName())) {
                                    resMetric.setTargetAverageValue(quantityFormatter.parse(String.format("%sMi", metric.getTargetAverageValue())));
                                } else {
                                    resMetric.setTargetAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
                                }
                            }
                        } else {
                            if (metric.getTargetAverageUtilization() != null) {
                                resMetric.setTargetAverageUtilization(metric.getTargetAverageUtilization());
                            } else {
                                resMetric.setTargetAverageUtilization(metric.getResourceTargetAverageUtilization());
                            }
                        }
                        metricSpec.setResource(resMetric);

                        break;
                    case Pods:
                        V2beta1PodsMetricSource podMetric = new V2beta1PodsMetricSource();
                        podMetric.setMetricName(metric.getPodsMetricName());
                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            podMetric.setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        if (metric.getTargetType() == MetricTargetType.AverageValue) {
                            podMetric.setTargetAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
                        }

                        metricSpec.setPods(podMetric);

                        break;
                    case Object:
                        V2beta1CrossVersionObjectReference objMetricTarget = new V2beta1CrossVersionObjectReference();
                        objMetricTarget.setApiVersion(metric.getObjectTargetApiVerion());
                        objMetricTarget.setKind(metric.getObjectTargetKind());
                        objMetricTarget.setName(metric.getObjectTargetName());

                        V2beta1ObjectMetricSource objMetric = new V2beta1ObjectMetricSource();
                        objMetric.setMetricName(metric.getObjectMetricName());
                        if (metric.getTargetType() == MetricTargetType.AverageValue) {
                            objMetric.setAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
                        } else if (metric.getTargetType() == MetricTargetType.Value) {
                            objMetric.setTargetValue(quantityFormatter.parse(metric.getTargetValue()));
                        }
                        objMetric.setTarget(objMetricTarget);

                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            objMetric.setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        metricSpec.setObject(objMetric);

                        break;
                    case External:
                        V2beta1ExternalMetricSource extMetric = new V2beta1ExternalMetricSource();
                        extMetric.setMetricName(metric.getExternalMetricName());
                        if (metric.getTargetType() == MetricTargetType.AverageValue) {
                            extMetric.setTargetAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
                        } else if (metric.getTargetType() == MetricTargetType.Value) {
                            extMetric.setTargetValue(quantityFormatter.parse(metric.getTargetValue()));
                        }

                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            extMetric.setMetricSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        metricSpec.setExternal(extMetric);

                        break;
                }
                metrics.add(metricSpec);
            }
            spec.setMetrics(metrics);
        } else {
            CocktailException ce = new CocktailException("Not supported in HorizontalPodAutoscaler V2beta1", ExceptionType.K8sNotSupported, hpa);
            log.error(ce.getMessage(), ce);
            throw ce;
        }

        V2beta1HorizontalPodAutoscaler v2beta1Hpa = new V2beta1HorizontalPodAutoscaler();

        Map<String, String> labels = new HashMap<>();
        labels.put(KubeConstants.LABELS_KEY, name);
//        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getUniqueName(componentId));

        V1ObjectMeta metaData = new V1ObjectMeta();
        metaData.setName(name);
        metaData.setNamespace(ns);
        metaData.setLabels(labels);

        v2beta1Hpa.setMetadata(metaData);
        v2beta1Hpa.setApiVersion(String.format("%s/%s", K8sApiGroupType.AUTOSCALING.getValue(), K8sApiType.V2BETA1.getValue())); // V1 Spec은 사전 처리에서 걸러짐 무조건 V2Beta1 Spec만 처리
        v2beta1Hpa.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
        v2beta1Hpa.setSpec(spec);

        return v2beta1Hpa;
    }

//    /**
//     * HorizontalPodAutoscaler Spec Builder (V2beta2)
//     *
//     * @param hpa
//     * @param namespace
//     * @param componentId
//     * @param name
//     * @return
//     * @throws Exception
//     */
//    public static V2beta2HorizontalPodAutoscaler buildHpaV2beta2(HorizontalPodAutoscalerVO hpa, String namespace, String componentId, String name) throws Exception {
//        boolean useExtensionMetric = (hpa.getMetrics() != null && hpa.getMetrics().size() > 0); // V2Beta2 Spec 인지 확인
//
//        V2beta2HorizontalPodAutoscalerSpec spec = new V2beta2HorizontalPodAutoscalerSpec();
//        spec.setMaxReplicas(hpa.getMaxReplicas());
//        spec.setMinReplicas(hpa.getMinReplicas());
//        /**
//         * TODO: Redion: CrossVersionObjectReference 정보를 넣을때에 Deployment의 ApiVersion을 확인하여 올바르게 정보를 넣어줄 필요가 있음
//         * 현재는 extension/v1beta1으로만 Deployment 하고 있으므로 괜찮지만 추후 Deployment 버전이 올라갈 경우 Side effect 예상됨
//         * 현재로서는 딱히 정보를 알아올 방법이 없어 이전과 같이 extension/v1beta1으로 고정하여 처리함
//         * redion (2018.08.08)
//         */
//        V2beta2CrossVersionObjectReference targetRef = new V2beta2CrossVersionObjectReference();
//        targetRef.setApiVersion(String.format("%s/%s", K8sApiGroupType.APPS.getValue(), K8sApiType.V1.getValue()));
//        targetRef.setKind(K8sApiKindType.DEPLOYMENT.getValue());
//        targetRef.setName(name);
//
//        spec.setScaleTargetRef(targetRef);
//
//        if (useExtensionMetric) {
//            QuantityFormatter quantityFormatter = new QuantityFormatter();
//            JSON k8sJson = new JSON();
//            for (MetricVO metric : hpa.getMetrics()) {
//                V2beta2MetricSpec metricSpec = new V2beta2MetricSpec();
//                metricSpec.setType(metric.getType().getCode());
//
//                V2beta2MetricTarget target = new V2beta2MetricTarget();
//                target.setType(metric.getTargetType().getCode());
//                if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.AverageValue.getCode())) {
//                    if (metric.getType() == MetricType.Resource) {
//                        if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_CPU, metric.getResourceName())) {
//                            target.setAverageValue(quantityFormatter.parse(String.format("%sm", metric.getTargetAverageValue())));
//                        } else if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_MEMORY, metric.getResourceName())) {
//                            target.setAverageValue(quantityFormatter.parse(String.format("%sMi", metric.getTargetAverageValue())));
//                        } else {
//                            target.setAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
//                        }
//                    } else {
//                        target.setAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
//                    }
//                } else if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.Value.getCode())) {
//                    target.setValue(quantityFormatter.parse(metric.getTargetValue()));
//                } else {
//                    if (metric.getTargetAverageUtilization() != null) {
//                        target.setAverageUtilization(metric.getTargetAverageUtilization());
//                    } else {
//                        target.setAverageUtilization(metric.getResourceTargetAverageUtilization());
//                    }
//                }
//
//                switch (metric.getType()) {
//                    case Resource:
//                        V2beta2ResourceMetricSource resMetric = new V2beta2ResourceMetricSource();
//                        resMetric.setName(StringUtils.lowerCase(metric.getResourceName()));
//                        resMetric.setTarget(target);
//                        metricSpec.setResource(resMetric);
//
//                        break;
//                    case Pods:
//                        V2beta2PodsMetricSource podMetric = new V2beta2PodsMetricSource();
//
//                        podMetric.metric(new V2beta2MetricIdentifier()).setTarget(target);
//                        podMetric.getMetric().setName(metric.getPodsMetricName());
//                        if (metric.getMetricLabelSelector() != null) {
//                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
//                            podMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
//                        }
//
//                        metricSpec.setPods(podMetric);
//
//                        break;
//                    case Object:
//                        V2beta2ObjectMetricSource objMetric = new V2beta2ObjectMetricSource();
//                        objMetric.describedObject(new V2beta2CrossVersionObjectReference()).metric(new V2beta2MetricIdentifier()).setTarget(target);
//
//                        objMetric.getDescribedObject().setApiVersion(metric.getObjectTargetApiVerion());
//                        objMetric.getDescribedObject().setKind(metric.getObjectTargetKind());
//                        objMetric.getDescribedObject().setName(metric.getObjectTargetName());
//
//                        objMetric.getMetric().setName(metric.getObjectMetricName());
//                        if (metric.getMetricLabelSelector() != null) {
//                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
//                            objMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
//                        }
//
//                        metricSpec.setObject(objMetric);
//
//                        break;
//                    case External:
//                        V2beta2ExternalMetricSource extMetric = new V2beta2ExternalMetricSource();
//
//                        extMetric.metric(new V2beta2MetricIdentifier()).setTarget(target);
//                        extMetric.getMetric().setName(metric.getExternalMetricName());
//                        if (metric.getMetricLabelSelector() != null) {
//                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
//                            extMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
//                        }
//
//                        metricSpec.setExternal(extMetric);
//
//                        break;
//
//                }
//                spec.addMetricsItem(metricSpec);
//            }
//        } else {
//            CocktailException ce = new CocktailException("Not supported in HorizontalPodAutoscaler V2beta2", ExceptionType.K8sNotSupported, hpa);
//            log.error(ce.getMessage(), ce);
//            throw ce;
//        }
//
//        V2beta2HorizontalPodAutoscaler v2beta2Hpa = new V2beta2HorizontalPodAutoscaler();
//
//        Map<String, String> labels = new HashMap<>();
//        labels.put(KubeConstants.LABELS_KEY, name);
////        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getUniqueName(componentId));
//
//        V1ObjectMeta metaData = new V1ObjectMeta();
//        metaData.setName(name);
//        metaData.setNamespace(namespace);
//        metaData.setLabels(labels);
//
//        v2beta2Hpa.setMetadata(metaData);
//        v2beta2Hpa.setApiVersion(String.format("%s/%s", K8sApiGroupType.AUTOSCALING.getValue(), K8sApiType.V2BETA2.getValue()));
//        v2beta2Hpa.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
//        v2beta2Hpa.setSpec(spec);
//
//        return v2beta2Hpa;
//    }

    /**
     * HorizontalPodAutoscaler Spec Builder (V2beta2)
     * - k8s 1.18 부터 behavior spec 추가
     *
     * @param hpa
     * @param namespace
     * @param componentName
     * @param name
     * @param k8sApiVerKindType
     * @return
     * @throws Exception
     */
    public static V2beta2HorizontalPodAutoscaler buildHpaV2beta2(HpaGuiVO hpa, String namespace, String componentName, String name, K8sApiVerKindType k8sApiVerKindType) throws Exception {
        boolean useExtensionMetric = (hpa.getMetrics() != null && hpa.getMetrics().size() > 0); // V2Beta2 Spec 인지 확인

        V2beta2HorizontalPodAutoscalerSpec spec = new V2beta2HorizontalPodAutoscalerSpec();
        spec.setMaxReplicas(hpa.getMaxReplicas());
        spec.setMinReplicas(hpa.getMinReplicas());
        /**
         * TODO: Redion: CrossVersionObjectReference 정보를 넣을때에 Deployment의 ApiVersion을 확인하여 올바르게 정보를 넣어줄 필요가 있음
         * 현재는 extension/v1beta1으로만 Deployment 하고 있으므로 괜찮지만 추후 Deployment 버전이 올라갈 경우 Side effect 예상됨
         * 현재로서는 딱히 정보를 알아올 방법이 없어 이전과 같이 extension/v1beta1으로 고정하여 처리함
         * redion (2018.08.08)
         */
        V2beta2CrossVersionObjectReference targetRef = new V2beta2CrossVersionObjectReference();
        targetRef.setApiVersion(String.format("%s/%s", k8sApiVerKindType.getGroupType().getValue(), k8sApiVerKindType.getApiType().getValue()));
        targetRef.setKind(k8sApiVerKindType.getKindType().getValue());
        targetRef.setName(componentName);

        spec.setScaleTargetRef(targetRef);

        if (useExtensionMetric) {
            QuantityFormatter quantityFormatter = new QuantityFormatter();
            JSON k8sJson = new JSON();

            // spec.metrics
            for (MetricVO metric : hpa.getMetrics()) {
                V2beta2MetricSpec metricSpec = new V2beta2MetricSpec();
                metricSpec.setType(metric.getType().getCode());

                V2beta2MetricTarget target = new V2beta2MetricTarget();
                target.setType(metric.getTargetType().getCode());
                if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.AverageValue.getCode())) {
                    if (metric.getType() == MetricType.Resource) {
                        if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_CPU, metric.getResourceName())) {
                            target.setAverageValue(quantityFormatter.parse(String.format("%sm", metric.getTargetAverageValue())));
                        } else if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_MEMORY, metric.getResourceName())) {
                            target.setAverageValue(quantityFormatter.parse(String.format("%sMi", metric.getTargetAverageValue())));
                        } else {
                            target.setAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
                        }
                    } else {
                        target.setAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
                    }
                } else if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.Value.getCode())) {
                    target.setValue(quantityFormatter.parse(metric.getTargetValue()));
                } else {
                    if (metric.getTargetAverageUtilization() != null) {
                        target.setAverageUtilization(metric.getTargetAverageUtilization());
                    } else {
                        target.setAverageUtilization(metric.getResourceTargetAverageUtilization());
                    }
                }

                switch (metric.getType()) {
                    case Resource:
                        V2beta2ResourceMetricSource resMetric = new V2beta2ResourceMetricSource();
                        resMetric.setName(StringUtils.lowerCase(metric.getResourceName()));
                        resMetric.setTarget(target);
                        metricSpec.setResource(resMetric);

                        break;
                    case Pods:
                        V2beta2PodsMetricSource podMetric = new V2beta2PodsMetricSource();

                        podMetric.metric(new V2beta2MetricIdentifier()).setTarget(target);
                        podMetric.getMetric().setName(metric.getPodsMetricName());
                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            podMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        metricSpec.setPods(podMetric);

                        break;
                    case Object:
                        V2beta2ObjectMetricSource objMetric = new V2beta2ObjectMetricSource();
                        objMetric.describedObject(new V2beta2CrossVersionObjectReference()).metric(new V2beta2MetricIdentifier()).setTarget(target);

                        objMetric.getDescribedObject().setApiVersion(metric.getObjectTargetApiVerion());
                        objMetric.getDescribedObject().setKind(metric.getObjectTargetKind());
                        objMetric.getDescribedObject().setName(metric.getObjectTargetName());

                        objMetric.getMetric().setName(metric.getObjectMetricName());
                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            objMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        metricSpec.setObject(objMetric);

                        break;
                    case External:
                        V2beta2ExternalMetricSource extMetric = new V2beta2ExternalMetricSource();

                        extMetric.metric(new V2beta2MetricIdentifier()).setTarget(target);
                        extMetric.getMetric().setName(metric.getExternalMetricName());
                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            extMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        metricSpec.setExternal(extMetric);

                        break;

                }
                spec.addMetricsItem(metricSpec);
            }

            // spec.behavior
            if (hpa.getScaleDown() != null || hpa.getScaleUp() != null) {
                V2beta2HorizontalPodAutoscalerBehavior behavior = new V2beta2HorizontalPodAutoscalerBehavior();
                if (hpa.getScaleDown() != null) {
                    V2beta2HPAScalingRules scaleDown = new V2beta2HPAScalingRules();

                    // selectPolicy - Disabled
                    if (StringUtils.equalsIgnoreCase(hpa.getScaleDown().getSelectPolicy(), HpaSelectPolicy.Names.Disabled)) {
                        scaleDown.setSelectPolicy(HpaSelectPolicy.Names.Disabled);
                    }
                    // selectPolicy - Min, Max
                    else {
                        scaleDown.setSelectPolicy(hpa.getScaleDown().getSelectPolicy());

                        if (hpa.getScaleDown().getStabilizationWindowSeconds() != null) {
                            // Max 3600 - less than or equal to 3600 (one hour)
                            scaleDown.setStabilizationWindowSeconds(hpa.getScaleDown().getStabilizationWindowSeconds());
                        } else {
                            scaleDown.setStabilizationWindowSeconds(300); // the default values: For scale down: 300
                        }

                        if (CollectionUtils.isNotEmpty(hpa.getScaleDown().getPolicies())) {
                            String toJson = k8sJson.serialize(hpa.getScaleDown().getPolicies());
                            scaleDown.setPolicies(k8sJson.getGson().fromJson(toJson, new TypeToken<List<V2beta2HPAScalingPolicy>>(){}.getType()));
                        }
                    }
                    behavior.setScaleDown(scaleDown);
                }
                if (hpa.getScaleUp() != null) {
                    V2beta2HPAScalingRules scaleUp = new V2beta2HPAScalingRules();

                    // selectPolicy - Disabled
                    if (StringUtils.equalsIgnoreCase(hpa.getScaleUp().getSelectPolicy(), HpaSelectPolicy.Names.Disabled)) {
                        scaleUp.setSelectPolicy(HpaSelectPolicy.Names.Disabled);
                    }
                    // selectPolicy - Min, Max
                    else {
                        scaleUp.setSelectPolicy(hpa.getScaleUp().getSelectPolicy());

                        if (hpa.getScaleUp().getStabilizationWindowSeconds() != null) {
                            // Max 3600 - less than or equal to 3600 (one hour)
                            scaleUp.setStabilizationWindowSeconds(hpa.getScaleUp().getStabilizationWindowSeconds());
                        } else {
                            scaleUp.setStabilizationWindowSeconds(0); // the default values: For scale up: 0 (i.e. no stabilization is done)
                        }

                        if (CollectionUtils.isNotEmpty(hpa.getScaleUp().getPolicies())) {
                            String toJson = k8sJson.serialize(hpa.getScaleUp().getPolicies());
                            scaleUp.setPolicies(k8sJson.getGson().fromJson(toJson, new TypeToken<List<V2beta2HPAScalingPolicy>>(){}.getType()));
                        }
                    }
                    behavior.setScaleUp(scaleUp);
                }
                spec.setBehavior(behavior);
            }
        } else {
            CocktailException ce = new CocktailException("Not supported in HorizontalPodAutoscaler V2beta2", ExceptionType.K8sNotSupported, hpa);
            log.error(ce.getMessage(), ce);
            throw ce;
        }

        V2beta2HorizontalPodAutoscaler v2beta2Hpa = new V2beta2HorizontalPodAutoscaler();

        Map<String, String> labels = new HashMap<>();
        labels.put(KubeConstants.LABELS_KEY, name);
//        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getUniqueName(componentId));

        V1ObjectMeta metaData = new V1ObjectMeta();
        metaData.setName(name);
        metaData.setNamespace(namespace);
        metaData.setLabels(labels);

        v2beta2Hpa.setMetadata(metaData);
        v2beta2Hpa.setApiVersion(String.format("%s/%s", K8sApiGroupType.AUTOSCALING.getValue(), K8sApiType.V2BETA2.getValue()));
        v2beta2Hpa.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
        v2beta2Hpa.setSpec(spec);

        return v2beta2Hpa;
    }


    /**
     * HorizontalPodAutoscaler Spec Builder (V2)
     * - k8s 1.18 부터 behavior spec 추가
     * - k8s 1.23 GA
     *
     * @param hpa
     * @param namespace
     * @param componentName
     * @param name
     * @param k8sApiVerKindType
     * @return
     * @throws Exception
     */
    public static V2HorizontalPodAutoscaler buildHpaV2(HpaGuiVO hpa, String namespace, String componentName, String name, K8sApiVerKindType k8sApiVerKindType) throws Exception {
        boolean useExtensionMetric = (hpa.getMetrics() != null && hpa.getMetrics().size() > 0); // V2Beta2 Spec 인지 확인

        V2HorizontalPodAutoscalerSpec spec = new V2HorizontalPodAutoscalerSpec();
        spec.setMaxReplicas(hpa.getMaxReplicas());
        spec.setMinReplicas(hpa.getMinReplicas());
        /**
         * TODO: Redion: CrossVersionObjectReference 정보를 넣을때에 Deployment의 ApiVersion을 확인하여 올바르게 정보를 넣어줄 필요가 있음
         * 현재는 extension/v1beta1으로만 Deployment 하고 있으므로 괜찮지만 추후 Deployment 버전이 올라갈 경우 Side effect 예상됨
         * 현재로서는 딱히 정보를 알아올 방법이 없어 이전과 같이 extension/v1beta1으로 고정하여 처리함
         * redion (2018.08.08)
         */
        V2CrossVersionObjectReference targetRef = new V2CrossVersionObjectReference();
        targetRef.setApiVersion(String.format("%s/%s", k8sApiVerKindType.getGroupType().getValue(), k8sApiVerKindType.getApiType().getValue()));
        targetRef.setKind(k8sApiVerKindType.getKindType().getValue());
        targetRef.setName(componentName);

        spec.setScaleTargetRef(targetRef);

        if (useExtensionMetric) {
            QuantityFormatter quantityFormatter = new QuantityFormatter();
            JSON k8sJson = new JSON();

            // spec.metrics
            for (MetricVO metric : hpa.getMetrics()) {
                V2MetricSpec metricSpec = new V2MetricSpec();
                metricSpec.setType(metric.getType().getCode());

                V2MetricTarget target = new V2MetricTarget();
                target.setType(metric.getTargetType().getCode());
                if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.AverageValue.getCode())) {
                    if (metric.getType() == MetricType.Resource) {
                        if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_CPU, metric.getResourceName())) {
                            target.setAverageValue(quantityFormatter.parse(String.format("%sm", metric.getTargetAverageValue())));
                        } else if (StringUtils.equalsIgnoreCase(KubeConstants.RESOURCES_MEMORY, metric.getResourceName())) {
                            target.setAverageValue(quantityFormatter.parse(String.format("%sMi", metric.getTargetAverageValue())));
                        } else {
                            target.setAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
                        }
                    } else {
                        target.setAverageValue(quantityFormatter.parse(metric.getTargetAverageValue()));
                    }
                } else if (StringUtils.equalsIgnoreCase(target.getType(), MetricTargetType.Value.getCode())) {
                    target.setValue(quantityFormatter.parse(metric.getTargetValue()));
                } else {
                    if (metric.getTargetAverageUtilization() != null) {
                        target.setAverageUtilization(metric.getTargetAverageUtilization());
                    } else {
                        target.setAverageUtilization(metric.getResourceTargetAverageUtilization());
                    }
                }

                switch (metric.getType()) {
                    case Resource:
                        V2ResourceMetricSource resMetric = new V2ResourceMetricSource();
                        resMetric.setName(StringUtils.lowerCase(metric.getResourceName()));
                        resMetric.setTarget(target);
                        metricSpec.setResource(resMetric);

                        break;
                    case Pods:
                        V2PodsMetricSource podMetric = new V2PodsMetricSource();

                        podMetric.metric(new V2MetricIdentifier()).setTarget(target);
                        podMetric.getMetric().setName(metric.getPodsMetricName());
                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            podMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        metricSpec.setPods(podMetric);

                        break;
                    case Object:
                        V2ObjectMetricSource objMetric = new V2ObjectMetricSource();
                        objMetric.describedObject(new V2CrossVersionObjectReference()).metric(new V2MetricIdentifier()).setTarget(target);

                        objMetric.getDescribedObject().setApiVersion(metric.getObjectTargetApiVerion());
                        objMetric.getDescribedObject().setKind(metric.getObjectTargetKind());
                        objMetric.getDescribedObject().setName(metric.getObjectTargetName());

                        objMetric.getMetric().setName(metric.getObjectMetricName());
                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            objMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        metricSpec.setObject(objMetric);

                        break;
                    case External:
                        V2ExternalMetricSource extMetric = new V2ExternalMetricSource();

                        extMetric.metric(new V2MetricIdentifier()).setTarget(target);
                        extMetric.getMetric().setName(metric.getExternalMetricName());
                        if (metric.getMetricLabelSelector() != null) {
                            String selectorJson = k8sJson.serialize(metric.getMetricLabelSelector());
                            extMetric.getMetric().setSelector(k8sJson.getGson().fromJson(selectorJson, V1LabelSelector.class));
                        }

                        metricSpec.setExternal(extMetric);

                        break;

                }
                spec.addMetricsItem(metricSpec);
            }

            // spec.behavior
            if (hpa.getScaleDown() != null || hpa.getScaleUp() != null) {
                V2HorizontalPodAutoscalerBehavior behavior = new V2HorizontalPodAutoscalerBehavior();
                if (hpa.getScaleDown() != null) {
                    V2HPAScalingRules scaleDown = new V2HPAScalingRules();

                    // selectPolicy - Disabled
                    if (StringUtils.equalsIgnoreCase(hpa.getScaleDown().getSelectPolicy(), HpaSelectPolicy.Names.Disabled)) {
                        scaleDown.setSelectPolicy(HpaSelectPolicy.Names.Disabled);
                    }
                    // selectPolicy - Min, Max
                    else {
                        scaleDown.setSelectPolicy(hpa.getScaleDown().getSelectPolicy());

                        if (hpa.getScaleDown().getStabilizationWindowSeconds() != null) {
                            // Max 3600 - less than or equal to 3600 (one hour)
                            scaleDown.setStabilizationWindowSeconds(hpa.getScaleDown().getStabilizationWindowSeconds());
                        } else {
                            scaleDown.setStabilizationWindowSeconds(300); // the default values: For scale down: 300
                        }

                        if (CollectionUtils.isNotEmpty(hpa.getScaleDown().getPolicies())) {
                            String toJson = k8sJson.serialize(hpa.getScaleDown().getPolicies());
                            scaleDown.setPolicies(k8sJson.getGson().fromJson(toJson, new TypeToken<List<V2HPAScalingPolicy>>(){}.getType()));
                        }
                    }
                    behavior.setScaleDown(scaleDown);
                }
                if (hpa.getScaleUp() != null) {
                    V2HPAScalingRules scaleUp = new V2HPAScalingRules();

                    // selectPolicy - Disabled
                    if (StringUtils.equalsIgnoreCase(hpa.getScaleUp().getSelectPolicy(), HpaSelectPolicy.Names.Disabled)) {
                        scaleUp.setSelectPolicy(HpaSelectPolicy.Names.Disabled);
                    }
                    // selectPolicy - Min, Max
                    else {
                        scaleUp.setSelectPolicy(hpa.getScaleUp().getSelectPolicy());

                        if (hpa.getScaleUp().getStabilizationWindowSeconds() != null) {
                            // Max 3600 - less than or equal to 3600 (one hour)
                            scaleUp.setStabilizationWindowSeconds(hpa.getScaleUp().getStabilizationWindowSeconds());
                        } else {
                            scaleUp.setStabilizationWindowSeconds(0); // the default values: For scale up: 0 (i.e. no stabilization is done)
                        }

                        if (CollectionUtils.isNotEmpty(hpa.getScaleUp().getPolicies())) {
                            String toJson = k8sJson.serialize(hpa.getScaleUp().getPolicies());
                            scaleUp.setPolicies(k8sJson.getGson().fromJson(toJson, new TypeToken<List<V2HPAScalingPolicy>>(){}.getType()));
                        }
                    }
                    behavior.setScaleUp(scaleUp);
                }
                spec.setBehavior(behavior);
            }
        } else {
            CocktailException ce = new CocktailException("Not supported in HorizontalPodAutoscaler V2", ExceptionType.K8sNotSupported, hpa);
            log.error(ce.getMessage(), ce);
            throw ce;
        }

        V2HorizontalPodAutoscaler v2Hpa = new V2HorizontalPodAutoscaler();

        Map<String, String> labels = new HashMap<>();
        labels.put(KubeConstants.LABELS_KEY, name);
//        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getUniqueName(componentId));

        V1ObjectMeta metaData = new V1ObjectMeta();
        metaData.setName(name);
        metaData.setNamespace(namespace);
        metaData.setLabels(labels);

        v2Hpa.setMetadata(metaData);
        v2Hpa.setApiVersion(String.format("%s/%s", K8sApiGroupType.AUTOSCALING.getValue(), K8sApiType.V2.getValue()));
        v2Hpa.setKind(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
        v2Hpa.setSpec(spec);

        return v2Hpa;
    }

    public static Map<String, String> buildStorageClassLabel(ClusterVolumeVO clusterVolume) {
        Map<String, String> labels = new HashMap<>();
        if (MapUtils.isNotEmpty(clusterVolume.getLabels())) {
            labels.putAll(clusterVolume.getLabels());
        }
        labels.put(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_VOLUME_TYPE, clusterVolume.getType().getCode());
        labels.put(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE, clusterVolume.getStorageType().getCode());
        labels.put(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_PROVISIONER_TYPE, clusterVolume.getPlugin().getCode());
        if (clusterVolume.getPlugin().haveTotalCapacity()) {
            Integer totalCapacity = 100;
            if (clusterVolume.getTotalCapacity() != null) {
                totalCapacity = clusterVolume.getTotalCapacity();
            }
            labels.put(KubeConstants.LABELS_ACORNSOFT_STORAGE_CLASS_TOTAL_CAPACITY, String.valueOf(totalCapacity.intValue()));
        }

        return labels;
    }

    public static Map<String, String> buildStorageClassAnno(ClusterVolumeVO clusterVolume) {
        Map<String, String> annotations = new HashMap<>();

        if (MapUtils.isNotEmpty(clusterVolume.getAnnotations())) {
            annotations.putAll(clusterVolume.getAnnotations());
        }
        if (clusterVolume.getType() == VolumeType.PERSISTENT_VOLUME) {
            String annoKey = KubeConstants.META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT;

            if (clusterVolume != null && clusterVolume.getCluster() != null && StringUtils.isNotBlank(clusterVolume.getCluster().getK8sVersion())) {
                String[] k8sVersion = StringUtils.split(ResourceUtil.getMatchVersion(clusterVolume.getCluster().getK8sVersion()), ".");
                try {
                    if (Integer.parseInt(k8sVersion[0]) == 1 && Integer.parseInt(k8sVersion[1]) > 12) {
                        annoKey = KubeConstants.META_ANNOTATIONS_RELEASE_STORAGE_CLASS_IS_DEFAULT;
                    }
                } catch (NumberFormatException e) {
                    log.error("K8sSpecFactory.buildStorageClassAnno() baseVersion parseInt error!!", e);
                }
            }

            annotations.put(annoKey, BooleanUtils.toStringTrueFalse(BooleanUtils.toBoolean(clusterVolume.getBaseStorageYn())));
        }

        // Description을 annotation에 KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION 키로 저장한다.
        annotations.put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(clusterVolume.getDescription()))));

        return annotations;
    }

    public static V1StorageClass buildStorageClassV1(ClusterVolumeVO clusterVolume) throws Exception{
        V1StorageClass storageClass = new V1StorageClass();
        storageClass.setApiVersion(String.format("%s/%s", K8sApiGroupType.STORAGE.getValue(), K8sApiType.V1.getValue()));
        storageClass.setKind(K8sApiKindType.STORAGE_CLASS.getValue());

        /**
         * meta
         */
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(clusterVolume.getName());
        // label
        meta.setLabels(buildStorageClassLabel(clusterVolume));
        // annotations
        Map<String, String> annotations = new HashMap<>();
        if (MapUtils.isNotEmpty(clusterVolume.getAnnotations())) {
            annotations.putAll(clusterVolume.getAnnotations());
        }
        annotations.putAll(K8sSpecFactory.buildStorageClassAnno(clusterVolume));
        meta.setAnnotations(annotations);
        storageClass.setMetadata(meta);

        storageClass.setProvisioner(clusterVolume.getProvisionerName());
        storageClass.setReclaimPolicy(clusterVolume.getReclaimPolicy().getValue());
        storageClass.setVolumeBindingMode(clusterVolume.getVolumeBindingMode().getValue());

        if(CollectionUtils.isNotEmpty(clusterVolume.getParameters())){
            Map<String, String> parameters = new HashMap<>();
            for (ClusterVolumeParamterVO clusterVolumeParamterRow : clusterVolume.getParameters()){
                if (StringUtils.isNotBlank(clusterVolumeParamterRow.getValue())) {
                    parameters.put(clusterVolumeParamterRow.getName(), clusterVolumeParamterRow.getValue());
                }
            }
            storageClass.setParameters(parameters);
        }

        if (CollectionUtils.isNotEmpty(clusterVolume.getMountOptions())) {
            storageClass.setMountOptions(clusterVolume.getMountOptions());
        }

        // Following Types can expand volume size.
        // gcePersistentDisk, awsElasticBlockStore, Azure File, Azure Disk, Portworx, FlexVolumes, Cinder, glusterfs, rbd
        // 볼륨사이즈를 동적으로 확장 가능한 plugin 인 경우는 Storage class의 allowVolumeExpansion 값을 설정한다. NFS Dynamic 도 추가
        if(clusterVolume.getPlugin().canVolumeExpansion()){
            storageClass.setAllowVolumeExpansion(Boolean.TRUE);
        }

        return storageClass;
    }

    public static V1Namespace buildNamespaceV1(String namespaceName, Map<String, String> labels, Map<String, String> annotations) throws Exception{
        V1Namespace namespace = new V1Namespace();
        namespace.setApiVersion(K8sApiType.V1.getValue());
        namespace.setKind(K8sApiKindType.NAMESPACE.getValue());

        /**
         * meta
         */
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(namespaceName);
        // label
        meta.setLabels(K8sSpecFactory.buildNamespaceLabel(labels));
        meta.setAnnotations(annotations);

        namespace.setMetadata(meta);

        return namespace;
    }

//    private static V1ObjectMeta buildPspV1ObjectMeta(PodSecurityPolicyGuiVO pspGui) {
//        /**
//         * meta
//         */
//        V1ObjectMeta meta = new V1ObjectMeta();
//        meta.setName(pspGui.getName());
//
//        // labels
//        Map<String, String> labels = Optional.ofNullable(pspGui.getLabels()).orElseGet(() ->new HashMap<>());
//        if (pspGui.isDefault()) {
//            labels.put(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
//        }
//        meta.setLabels(labels);
//
//        // annotations
//        Map<String, String> annotations = Optional.ofNullable(pspGui.getAnnotations()).orElseGet(() ->new HashMap<>()); // Parameter로 넘어온 Annotation을 기본으로 사용..
//        // Description을 annotation에 KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION 키로 저장한다.
//        annotations.put(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(pspGui.getDescription()))));
//        meta.setAnnotations(annotations);
//
//        return meta;
//    }


    private static V1ObjectMeta buildV1ObjectMeta(String namespace, String name, String description, boolean isDefault, Map<String, String> labels, Map<String, String> annotaions) {

        return buildV1ObjectMeta(namespace, name, description, isDefault, false, labels, annotaions);
    }

    private static V1ObjectMeta buildV1ObjectMeta(String namespace, String name, String description, boolean isDefault, boolean isDisplayDefault, Map<String, String> labels, Map<String, String> annotaions) {
        /**
         * meta
         */
        V1ObjectMeta meta = new V1ObjectMeta();
        if (StringUtils.isNotBlank(namespace)) {
            meta.setNamespace(namespace);
        }
        meta.setName(name);

        // labels
        meta.setLabels(Optional.ofNullable(labels).orElseGet(() ->new HashMap<>()));
        if (isDefault) {
            meta.putLabelsItem(KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
        }
        if (isDisplayDefault) {
            meta.putLabelsItem(KubeConstants.LABELS_ACORNSOFT_PSP_DISPLAY_DEFALUT, KubeConstants.LABELS_COCKTAIL_KEY);
        }

        // annotations
        meta.setAnnotations(Optional.ofNullable(annotaions).orElseGet(() ->new HashMap<>())); // Parameter로 넘어온 Annotation을 기본으로 사용..
        // Description을 annotation에 KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION 키로 저장한다.
        meta.putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(description))));

        return meta;
    }

    public static ExtensionsV1beta1PodSecurityPolicy buildPspExtensionsV1beta1(PodSecurityPolicyGuiVO pspGui) throws Exception{
        ExtensionsV1beta1PodSecurityPolicy psp = new ExtensionsV1beta1PodSecurityPolicy();
        psp.setApiVersion(String.format("%s/%s", K8sApiGroupType.EXTENSIONS.getValue(), K8sApiType.V1BETA1.getValue()));
        psp.setKind(K8sApiKindType.POD_SECURITY_POLICY.getValue());

        /**
         * meta
         */
        psp.setMetadata(buildV1ObjectMeta(null, pspGui.getName(), pspGui.getDescription(), false, pspGui.isDisplayDefault(), pspGui.getLabels(), pspGui.getAnnotations()));

        /**
         * spec
         */
        JSON k8sJson = new JSON();
        psp.setSpec(JsonUtils.fromGson(k8sJson.serialize(pspGui), ExtensionsV1beta1PodSecurityPolicySpec.class));

        return psp;
    }

    public static V1beta1PodSecurityPolicy buildPspPolicyV1beta1(PodSecurityPolicyGuiVO pspGui) throws Exception{
        V1beta1PodSecurityPolicy psp = new V1beta1PodSecurityPolicy();
        psp.setApiVersion(String.format("%s/%s", K8sApiGroupType.POLICY.getValue(), K8sApiType.V1BETA1.getValue()));
        psp.setKind(K8sApiKindType.POD_SECURITY_POLICY.getValue());

        /**
         * meta
         */
        psp.setMetadata(buildV1ObjectMeta(null, pspGui.getName(), pspGui.getDescription(), false, pspGui.isDisplayDefault(), pspGui.getLabels(), pspGui.getAnnotations()));

        /**
         * spec
         */
        JSON k8sJson = new JSON();
        psp.setSpec(JsonUtils.fromGson(k8sJson.serialize(pspGui), V1beta1PodSecurityPolicySpec.class));

        return psp;
    }

    public static V1LimitRange buildLimitRangeV1(LimitRangeGuiVO limitRangeGui) throws Exception{
        V1LimitRange limitRange = new V1LimitRange();
        limitRange.setApiVersion(String.format("%s", K8sApiType.V1.getValue()));
        limitRange.setKind(K8sApiKindType.LIMIT_RANGE.getValue());

        /**
         * meta
         */
        limitRange.setMetadata(buildV1ObjectMeta(limitRangeGui.getNamespace(), limitRangeGui.getName(), limitRangeGui.getDescription(), limitRangeGui.isDefault(), limitRangeGui.getLabels(), limitRangeGui.getAnnotations()));

        /**
         * spec
         */
        if (limitRangeGui != null) {
            if (CollectionUtils.isNotEmpty(limitRangeGui.getLimits())) {
                limitRange.setSpec(new V1LimitRangeSpec());
                QuantityFormatter quantityFormatter = new QuantityFormatter();
                for (LimitRangeItemVO item : limitRangeGui.getLimits()) {
                    V1LimitRangeItem limitRangeItem = new V1LimitRangeItem();
                    // type
                    limitRangeItem.setType(item.getType());
                    // default
                    if (MapUtils.isNotEmpty(item.get_default())) {
                        limitRangeItem.setDefault(convertResourceMap(item.get_default(), quantityFormatter));
                    }
                    // defaultRequest
                    if (MapUtils.isNotEmpty(item.getDefaultRequest())) {
                        limitRangeItem.setDefaultRequest(convertResourceMap(item.getDefaultRequest(), quantityFormatter));
                    }
                    // max
                    if (MapUtils.isNotEmpty(item.getMax())) {
                        limitRangeItem.setMax(convertResourceMap(item.getMax(), quantityFormatter));
                    }
                    // maxLimitRequestRatio
                    if (MapUtils.isNotEmpty(item.getMaxLimitRequestRatio())) {
                        limitRangeItem.setMaxLimitRequestRatio(convertResourceMap(item.getMaxLimitRequestRatio(), quantityFormatter));
                    }
                    // min
                    if (MapUtils.isNotEmpty(item.getMin())) {
                        limitRangeItem.setMin(convertResourceMap(item.getMin(), quantityFormatter));
                    }
                    limitRange.getSpec().addLimitsItem(limitRangeItem);
                }
            }
        }
//        JSON k8sJson = new JSON();
//        limitRange.setSpec(JsonUtils.fromGson(k8sJson.serialize(limitRangeGui), V1LimitRangeSpec.class));

        return limitRange;
    }

    public static V1ResourceQuota buildResourceQuotaV1(ResourceQuotaGuiVO resourceQuotaGui) throws Exception{
        V1ResourceQuota resourceQuota = new V1ResourceQuota();
        resourceQuota.setApiVersion(String.format("%s", K8sApiType.V1.getValue()));
        resourceQuota.setKind(K8sApiKindType.RESOURCE_QUOTA.getValue());

        /**
         * meta
         */
        resourceQuota.setMetadata(buildV1ObjectMeta(resourceQuotaGui.getNamespace(), resourceQuotaGui.getName(), resourceQuotaGui.getDescription(), resourceQuotaGui.isDefault(), resourceQuotaGui.getLabels(), resourceQuotaGui.getAnnotations()));

        /**
         * spec
         */
        if (resourceQuotaGui != null) {
            resourceQuota.setSpec(new V1ResourceQuotaSpec());

            JSON k8sJson = new JSON();
            QuantityFormatter quantityFormatter = new QuantityFormatter();

            // hard
            if (MapUtils.isNotEmpty(resourceQuotaGui.getHard())) {
                resourceQuota.getSpec().setHard(convertResourceMap(resourceQuotaGui.getHard(), quantityFormatter));
            }

            // scopeSelector
            resourceQuota.getSpec().setScopeSelector(JsonUtils.fromGson(k8sJson.serialize(resourceQuotaGui.getScopeSelector()), V1ScopeSelector.class));

            // scopes
            resourceQuota.getSpec().setScopes(resourceQuotaGui.getScopes());
        }

        return resourceQuota;
    }

    public static V1NetworkPolicy buildNetworkPolicyV1(NetworkPolicyGuiVO networkPolicyGui) throws Exception{
        V1NetworkPolicy networkPolicy = new V1NetworkPolicy();
        networkPolicy.setApiVersion(String.format("%s/%s", K8sApiGroupType.NETWORKING.getValue(), K8sApiType.V1.getValue()));
        networkPolicy.setKind(K8sApiKindType.NETWORK_POLICY.getValue());

        /**
         * meta
         */
        networkPolicy.setMetadata(buildV1ObjectMeta(networkPolicyGui.getNamespace(), networkPolicyGui.getName(), networkPolicyGui.getDescription(), networkPolicyGui.isDefault(), networkPolicyGui.getLabels(), networkPolicyGui.getAnnotations()));

        /**
         * spec
         */
        if (networkPolicyGui != null) {
            networkPolicy.setSpec(new V1NetworkPolicySpec());

            JSON k8sJson = new JSON();

            // egress
            if (CollectionUtils.isNotEmpty(networkPolicyGui.getEgress())) {
                for (NetworkPolicyEgressRuleVO ruleRow : networkPolicyGui.getEgress()) {
                    V1NetworkPolicyEgressRule rule = new V1NetworkPolicyEgressRule();

                    // egress.ports
                    if (CollectionUtils.isNotEmpty(ruleRow.getPorts())) {
                        for (NetworkPolicyPortVO portRow : ruleRow.getPorts()) {
                            V1NetworkPolicyPort port = new V1NetworkPolicyPort();
                            port.setPort(new IntOrString(portRow.getPort()));
                            port.setProtocol(portRow.getProtocol());
                            rule.addPortsItem(port);
                        }
                    }

                    // egress.to
                    String toJson = k8sJson.serialize(ruleRow.getTo());
                    rule.setTo(k8sJson.getGson().fromJson(toJson, new TypeToken<List<V1NetworkPolicyPeer>>(){}.getType()));

                    networkPolicy.getSpec().addEgressItem(rule);
                }
            }

            // ingress
            if (CollectionUtils.isNotEmpty(networkPolicyGui.getIngress())) {
                for (NetworkPolicyIngressRuleVO ruleRow : networkPolicyGui.getIngress()) {
                    V1NetworkPolicyIngressRule rule = new V1NetworkPolicyIngressRule();

                    // ingress.ports
                    if (CollectionUtils.isNotEmpty(ruleRow.getPorts())) {
                        for (NetworkPolicyPortVO portRow : ruleRow.getPorts()) {
                            V1NetworkPolicyPort port = new V1NetworkPolicyPort();
                            port.setPort(new IntOrString(portRow.getPort()));
                            port.setProtocol(portRow.getProtocol());
                            rule.addPortsItem(port);
                        }
                    }

                    // ingress.from
                    String fromJson = k8sJson.serialize(ruleRow.getFrom());
                    rule.setFrom(k8sJson.getGson().fromJson(fromJson, new TypeToken<List<V1NetworkPolicyPeer>>(){}.getType()));

                    networkPolicy.getSpec().addIngressItem(rule);
                }
            }

            // podSelector
            V1LabelSelector v1LabelSelector = Optional.ofNullable(JsonUtils.fromGson(k8sJson.serialize(networkPolicyGui.getPodSelector()), V1LabelSelector.class)).orElseGet(() ->new V1LabelSelector());
            networkPolicy.getSpec().setPodSelector(v1LabelSelector);

            // policyTypes
            networkPolicy.getSpec().setPolicyTypes(networkPolicyGui.getPolicyTypes());
        }

        return networkPolicy;
    }

    public static V1ClusterRole buildClusterRoleV1(ClusterRoleGuiVO clusterRoleGui) throws Exception{

        JSON k8sJson = new JSON();

        V1ClusterRole clusterRole = new V1ClusterRole();
        clusterRole.setApiVersion(String.format("%s/%s", K8sApiGroupType.RBAC_AUTHORIZATION.getValue(), K8sApiType.V1.getValue()));
        clusterRole.setKind(K8sApiKindType.CLUSTER_ROLE.getValue());

        /**
         * meta
         */
        clusterRole.setMetadata(buildV1ObjectMeta(null, clusterRoleGui.getName(), clusterRoleGui.getDescription(), false, clusterRoleGui.getLabels(), clusterRoleGui.getAnnotations()));

        /**
         * AggregationRule
         */
        if (clusterRoleGui.getAggregationRule() != null) {

            clusterRole.setAggregationRule(JsonUtils.fromGson(k8sJson.serialize(clusterRoleGui.getAggregationRule()), V1AggregationRule.class));

        }

        /**
         * Rules
         */
        if (CollectionUtils.isNotEmpty(clusterRoleGui.getRules())) {

            String fromJson = k8sJson.serialize(clusterRoleGui.getRules());
            clusterRole.setRules(k8sJson.getGson().fromJson(fromJson, new TypeToken<List<V1PolicyRule>>(){}.getType()));

        }

        return clusterRole;
    }

    public static V1Role buildRoleV1(RoleGuiVO gui) throws Exception{

        JSON k8sJson = new JSON();

        V1Role role = new V1Role();
        role.setApiVersion(String.format("%s/%s", K8sApiGroupType.RBAC_AUTHORIZATION.getValue(), K8sApiType.V1.getValue()));
        role.setKind(K8sApiKindType.ROLE.getValue());

        /**
         * meta
         */
        role.setMetadata(buildV1ObjectMeta(gui.getNamespace(), gui.getName(), gui.getDescription(), false, gui.getLabels(), gui.getAnnotations()));

        /**
         * Rules
         */
        if (CollectionUtils.isNotEmpty(gui.getRules())) {

            String fromJson = k8sJson.serialize(gui.getRules());
            role.setRules(k8sJson.getGson().fromJson(fromJson, new TypeToken<List<V1PolicyRule>>(){}.getType()));

        }

        return role;
    }

    public static V1ClusterRoleBinding buildClusterRoleBindingV1(ClusterRoleBindingGuiVO gui) throws Exception{

        JSON k8sJson = new JSON();

        V1ClusterRoleBinding clusterRoleBinding = new V1ClusterRoleBinding();
        clusterRoleBinding.setApiVersion(String.format("%s/%s", K8sApiGroupType.RBAC_AUTHORIZATION.getValue(), K8sApiType.V1.getValue()));
        clusterRoleBinding.setKind(K8sApiKindType.CLUSTER_ROLE_BINDING.getValue());

        /**
         * meta
         */
        clusterRoleBinding.setMetadata(buildV1ObjectMeta(null, gui.getName(), gui.getDescription(), false, gui.getLabels(), gui.getAnnotations()));

        /**
         * roleRef
         */
        if (gui.getRoleRef() != null) {
            clusterRoleBinding.setRoleRef(new V1RoleRef());
            clusterRoleBinding.getRoleRef().setApiGroup(K8sApiGroupType.RBAC_AUTHORIZATION.getValue());
            clusterRoleBinding.getRoleRef().setKind(gui.getRoleRef().getKind());
            clusterRoleBinding.getRoleRef().setName(gui.getRoleRef().getName());
        }

        /**
         * Subjects
         */
        if (CollectionUtils.isNotEmpty(gui.getSubjects())) {
            String fromJson = k8sJson.serialize(gui.getSubjects());
            clusterRoleBinding.setSubjects(k8sJson.getGson().fromJson(fromJson, new TypeToken<List<V1Subject>>(){}.getType()));

        }

        return clusterRoleBinding;
    }

    public static V1RoleBinding buildRoleBindingV1(RoleBindingGuiVO gui) throws Exception{

        JSON k8sJson = new JSON();

        V1RoleBinding roleBinding = new V1RoleBinding();
        roleBinding.setApiVersion(String.format("%s/%s", K8sApiGroupType.RBAC_AUTHORIZATION.getValue(), K8sApiType.V1.getValue()));
        roleBinding.setKind(K8sApiKindType.ROLE_BINDING.getValue());

        /**
         * meta
         */
        roleBinding.setMetadata(buildV1ObjectMeta(gui.getNamespace(), gui.getName(), gui.getDescription(), false, gui.getLabels(), gui.getAnnotations()));

        /**
         * roleRef
         */
        if (gui.getRoleRef() != null) {
            roleBinding.setRoleRef(new V1RoleRef());
            roleBinding.getRoleRef().setApiGroup(K8sApiGroupType.RBAC_AUTHORIZATION.getValue());
            roleBinding.getRoleRef().setKind(gui.getRoleRef().getKind());
            roleBinding.getRoleRef().setName(gui.getRoleRef().getName());
        }

        /**
         * Subjects
         */
        if (CollectionUtils.isNotEmpty(gui.getSubjects())) {
            String fromJson = k8sJson.serialize(gui.getSubjects());
            roleBinding.setSubjects(k8sJson.getGson().fromJson(fromJson, new TypeToken<List<V1Subject>>(){}.getType()));

        }

        return roleBinding;
    }


    public static Map<String, Quantity> convertResourceMap(Map<String, String> resourceMap, QuantityFormatter quantityFormatter) {
        Map<String, Quantity> convertMap = Maps.newHashMap();
        if (MapUtils.isNotEmpty(resourceMap)) {
            for (Map.Entry<String, String> entry : resourceMap.entrySet()) {
                convertMap.put(entry.getKey(), quantityFormatter.parse(entry.getValue()));
//                if (StringUtils.isNotBlank(getQuantityResourceFormat(entry.getKey()))) {
//                    convertMap.put(entry.getKey(), quantityFormatter.parse(String.format(getQuantityResourceFormat(entry.getKey()), entry.getValue())));
//                }
            }
        }

        return convertMap;
    }

    public static String getQuantityResourceFormat(String resourceName) {
        if (StringUtils.equals(resourceName, KubeConstants.RESOURCES_CPU)
                || StringUtils.endsWith(resourceName, String.format(".%s", KubeConstants.RESOURCES_CPU))
        ) {
            return "%sm";
        }
        if (StringUtils.equals(resourceName, KubeConstants.RESOURCES_MEMORY)
                || StringUtils.endsWith(resourceName, String.format(".%s", KubeConstants.RESOURCES_CPU))
        ) {
            return "%sMi";
        }
        if (StringUtils.equals(resourceName, KubeConstants.RESOURCES_GPU)
                || StringUtils.endsWith(resourceName, String.format(".%s", KubeConstants.RESOURCES_CPU))
        ) {
            return "%s";
        }
        if (StringUtils.equals(resourceName, KubeConstants.RESOURCES_STORAGE)
                || StringUtils.endsWith(resourceName, String.format(".%s", KubeConstants.RESOURCES_CPU))
        ) {
            return "%sGi";
        }
        if (StringUtils.equals(resourceName, KubeConstants.RESOURCES_EPHEMERAL_STORAGE)
                || StringUtils.endsWith(resourceName, String.format(".%s", KubeConstants.RESOURCES_CPU))
        ) {
            return "%sGi";
        }
        if (StringUtils.equals(resourceName, KubeConstants.RESOURCES_PODS)) {
            return "%s";
        }

        return null;
    }

    public static List<String> setTrimCommand(String command) {
        String[] cmdArr = StringUtils.split(command, "\n");
        List<String> cmds = new ArrayList<>();
        for (String cmd : cmdArr) {
            cmds.add(StringUtils.trim(cmd));
        }

        return cmds;
    }
}
