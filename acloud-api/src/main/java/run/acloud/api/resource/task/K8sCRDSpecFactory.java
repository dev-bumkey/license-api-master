package run.acloud.api.resource.task;

import com.google.api.client.util.Maps;
import com.google.common.collect.Lists;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class K8sCRDSpecFactory {

    public static Map<String, String> buildDefaultPrefixLabel() {
        Map<String, String> labels = new HashMap<>();
        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
        return labels;
    }

    public static Map<String, String> buildDefaultPrefixLabel(Map<String, String> labels) {
        if(labels == null)
            labels = new HashMap<>();

        labels.put(KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
        return labels;
    }

    public static Map<String, String> buildDefaultPrefixAnnotations(Map<String, String> annotations, String type) {
        if(annotations == null)
            annotations = new HashMap<>();

        Map<String, String> defaultMap = K8sApiCniType.valueOf(type).getAnnotations();
        annotations.putAll(defaultMap);

        return annotations;
    }

    public static Map<String, Object> buildNetworkAttachmentDefinition(K8sCRDNetAttachDefGuiVO netAttachDef) {
        Map<String, Object> crd = new HashMap<>();
        crd.put(KubeConstants.APIVSERION, String.format("%s/%s", K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue()));
        crd.put(KubeConstants.KIND, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION.getValue());

        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName(netAttachDef.getName());
        objectMeta.setNamespace(netAttachDef.getNamespace());
        objectMeta.setLabels(buildDefaultPrefixLabel(netAttachDef.getLabels()));
        objectMeta.putLabelsItem(KubeConstants.LABELS_CRD_NET_ATTACH_DEF, netAttachDef.getType());
        objectMeta.setAnnotations(buildDefaultPrefixAnnotations(netAttachDef.getAnnotations(), netAttachDef.getType()));
//        objectMeta.setAnnotations(MapUtils.isEmpty(netAttachDef.getAnnotations()) ? K8sApiCniType.valueOf(netAttachDef.getType()).getAnnotations() : netAttachDef.getAnnotations());
        crd.put(KubeConstants.META, objectMeta);

        Map<String, String> configMap = Maps.newHashMap();
        configMap.put("config", netAttachDef.getConfig());
        crd.put(KubeConstants.SPEC, configMap);

        return crd;
    }

    public static Map<String, Object> buildIssuer(K8sCRDIssuerGuiVO issuer) throws Exception {
        Map<String, Object> crd = Maps.newLinkedHashMap();
        crd.put(KubeConstants.APIVSERION, String.format("%s/%s", K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue()));
        crd.put(KubeConstants.KIND, CertIssuerScope.valueOf(issuer.getScope()).getKind().getValue());

        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName(issuer.getName());
        if (CertIssuerScope.valueOf(issuer.getScope()) == CertIssuerScope.NAMESPACED) {
            objectMeta.setNamespace(issuer.getNamespace());
        }
        objectMeta.setLabels(issuer.getLabels());
        objectMeta.setAnnotations(issuer.getAnnotations());
        crd.put(KubeConstants.META, objectMeta);

        CertIssueType issueType = CertIssueType.valueOf(issuer.getIssueType());
        if (CertIssueType.selfSigned == issueType) {
            if (issuer.getSelfSigned() != null) {
                Map<String, Object> configMap = Maps.newHashMap();
                configMap.put(issuer.getIssueType(), ServerUtils.getK8sObjectToMap(issuer.getSelfSigned(), new JSON()));
                crd.put(KubeConstants.SPEC, configMap);
            } else {
                Map<String, Object> configMap = Maps.newHashMap();
                configMap.put(issuer.getIssueType(), Maps.newHashMap());
                crd.put(KubeConstants.SPEC, configMap);
            }
        } else if (CertIssueType.ca == issueType) {
            if (issuer.getCa() != null) {
                Map<String, Object> configMap = Maps.newHashMap();
                configMap.put(issuer.getIssueType(), ServerUtils.getK8sObjectToMap(issuer.getCa(), new JSON()));
                crd.put(KubeConstants.SPEC, configMap);
            }
        }

        return crd;
    }

    public static Map<String, Object> buildIssuer(K8sCRDIssuerYamlVO issuer) throws Exception {
        Map<String, Object> crd = Maps.newLinkedHashMap();
        crd.put(KubeConstants.APIVSERION, String.format("%s/%s", K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue()));
        crd.put(KubeConstants.KIND, CertIssuerScope.valueOf(issuer.getScope()).getKind().getValue());

        JSON k8sJson = new JSON();
        Map<String, Object> objMap = Yaml.getSnakeYaml().load(issuer.getYaml());

        V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(objMap, k8sJson);
        objectMeta.setName(issuer.getName());
        if (CertIssuerScope.valueOf(issuer.getScope()) == CertIssuerScope.NAMESPACED) {
            objectMeta.setNamespace(issuer.getNamespace());
        }
        objectMeta.setManagedFields(null);
        crd.put(KubeConstants.META, objectMeta);

        crd.put(KubeConstants.SPEC, objMap.get(KubeConstants.SPEC));

        return crd;
    }


    public static Map<String, Object> buildCertificate(K8sCRDCertificateGuiVO cert) throws Exception {
        Map<String, Object> crd = Maps.newLinkedHashMap();
        crd.put(KubeConstants.APIVSERION, String.format("%s/%s", K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue()));
        crd.put(KubeConstants.KIND, K8sApiKindType.CERTIFICATE.getValue());

        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName(cert.getName());
        objectMeta.setNamespace(cert.getNamespace());
        objectMeta.setLabels(cert.getLabels());
        objectMeta.setAnnotations(cert.getAnnotations());
        crd.put(KubeConstants.META, objectMeta);

        JSON k8sJson = new JSON();
        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put(CertCertificateGUIItem.isCA.getCode(), BooleanUtils.toBoolean(cert.getIsCA()));
        if (StringUtils.isNotBlank(cert.getCommonName())) {
            configMap.put(CertCertificateGUIItem.commonName.getCode(), cert.getCommonName());
        }
        if (CollectionUtils.isNotEmpty(cert.getDnsNames())) {
            configMap.put(CertCertificateGUIItem.dnsNames.getCode(), cert.getDnsNames());
        }
        if (CollectionUtils.isNotEmpty(cert.getUris())) {
            configMap.put(CertCertificateGUIItem.uris.getCode(), cert.getUris());
        }
        if (CollectionUtils.isNotEmpty(cert.getIpAddresses())) {
            configMap.put(CertCertificateGUIItem.ipAddresses.getCode(), cert.getIpAddresses());
        }
        if (CollectionUtils.isNotEmpty(cert.getEmailAddresses())) {
            configMap.put(CertCertificateGUIItem.emailAddresses.getCode(), cert.getEmailAddresses());
        }
        if (StringUtils.isNotBlank(cert.getSecretName())) {
            configMap.put(CertCertificateGUIItem.secretName.getCode(), cert.getSecretName());
        }
        if (cert.getIssuerRef() != null) {
            String strJson = k8sJson.serialize(cert.getIssuerRef());
            configMap.put(CertCertificateGUIItem.issuerRef.getCode(), k8sJson.getGson().fromJson(strJson, K8sCRDIssuerRefVO.class));
        }
        if (StringUtils.isNotBlank(cert.getDuration())) {
            configMap.put(CertCertificateGUIItem.duration.getCode(), cert.getDuration());
        }
        if (StringUtils.isNotBlank(cert.getRenewBefore())) {
            configMap.put(CertCertificateGUIItem.renewBefore.getCode(), cert.getRenewBefore());
        }
        if (cert.getRevisionHistoryLimit() != null) {
            configMap.put(CertCertificateGUIItem.revisionHistoryLimit.getCode(), cert.getRevisionHistoryLimit());
        }
        if (CollectionUtils.isNotEmpty(cert.getUsages())) {
            List<String> usages = Lists.newArrayList();
            // Map<Code, Value>
            Map<String, String> usageMap = Arrays.stream(CertUsages.values()).collect(Collectors.toMap(CertUsages::getCode, CertUsages::getValue));
            cert.getUsages().stream().filter(u -> (usageMap.containsKey(u))).forEach(u -> usages.add(usageMap.get(u)));
            cert.setUsages(usages);
            configMap.put(CertCertificateGUIItem.usages.getCode(), cert.getUsages());
        }

        crd.put(KubeConstants.SPEC, configMap);

        return crd;
    }

    public static Map<String, Object> buildCertificate(K8sCRDCertificateYamlVO cert) throws Exception {
        Map<String, Object> crd = Maps.newLinkedHashMap();
        crd.put(KubeConstants.APIVSERION, String.format("%s/%s", K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue()));
        crd.put(KubeConstants.KIND, K8sApiKindType.CERTIFICATE.getValue());

        JSON k8sJson = new JSON();
        Map<String, Object> objMap = Yaml.getSnakeYaml().load(cert.getYaml());

        V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(objMap, k8sJson);
        objectMeta.setName(cert.getName());
        objectMeta.setNamespace(cert.getNamespace());
        objectMeta.setManagedFields(null);
        crd.put(KubeConstants.META, objectMeta);

        crd.put(KubeConstants.SPEC, objMap.get(KubeConstants.SPEC));

        return crd;
    }
}
