package run.acloud.api.configuration.service;

import com.google.api.client.util.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.vo.ChartInfoBaseVO;
import run.acloud.api.configuration.enums.AddonDynamicValueType;
import run.acloud.api.configuration.enums.AddonKeyItem;
import run.acloud.api.configuration.util.AddonUtils;
import run.acloud.api.configuration.vo.AddonInstallVO;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.api.resource.service.ConfigMapService;
import run.acloud.api.resource.service.ServiceSpecService;
import run.acloud.api.resource.service.WorkloadResourceService;
import run.acloud.api.resource.vo.ConfigMapGuiVO;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.exception.CocktailException;

import javax.security.auth.x500.X500PrivateCredential;
import java.util.*;

@Deprecated
@Slf4j
@Service
public class AddonAsyncService {

    @Autowired
    private ConfigMapService configMapService;

    @Autowired
    private ServiceSpecService serviceSpecService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private AddonCommonService addonCommonService;

    @Async
    public void postAsyncProcessor(String type, ClusterVO cluster, String releaseNamespace, String addonName, String addonBaseNamespace, AddonInstallVO addonInstall, Map<String, String> dynamicValues) throws Exception {
        switch (type) {
            case "istio-loadbalancer": {
                List<JsonObject> patchBody = new ArrayList<>();

                /** 1. istio-ingressgateway 서비스를 조회 하여 LoadBalancer 구성이 완료되었는지 확인 (Loop...) **/
                String lb = null;
                String domain = null;
                String ipAddress = null;
                int waitSecond = 40; // 최대 2분간 대기하고, 처리가 안되면 이후 프로세스를 진행하도록 함...

                for (int i = 0; i < waitSecond; i++) {
                    V1Service v1Service = serviceSpecService.getServiceV1(cluster, releaseNamespace, "istio-ingressgateway", null);

                    if (v1Service == null) {
                        Thread.sleep(3000);
                        continue;
                    }
                    if (CollectionUtils.isNotEmpty(v1Service.getStatus().getLoadBalancer().getIngress())) {
                        String ip = v1Service.getStatus().getLoadBalancer().getIngress().get(0).getIp();
                        String hostname = v1Service.getStatus().getLoadBalancer().getIngress().get(0).getHostname();
                        if (StringUtils.isEmpty(ip)) {
                            lb = domain = hostname;
                        }
                        else {
                            lb = ipAddress = ip;
                        }
                    }

                    if (StringUtils.isNotBlank(lb)) {
                        break;
                    }
                    Thread.sleep(3000);
                }

                /** 2. 설정된 Host Address를 이용하여 인증서 새로 구성 **/
                ConfigMapGuiVO currAddon = configMapService.getConfigMap(cluster.getClusterSeq(), addonBaseNamespace, addonName);
                for (int i = 0; i < waitSecond; i++) {
                    if (currAddon == null) {
                        Thread.sleep(3000);
                        continue;
                    }
                    else {
                        break;
                    }
                }
                if (currAddon == null) {
                    return;
                }

                /** chart base info 설정 **/
                ChartInfoBaseVO chartInfo = new ChartInfoBaseVO();
                BeanUtils.copyProperties(addonInstall, chartInfo);

                /** Create Certificate **/
                X500PrivateCredential serverCred = null;
                /** 이미 Ca 인증서가 있으면 기존 인증서를 사용하여 Leaf 인증서를 생성한다.. (dashboard에서 Kiali 접속시 Cache된 인증서를 계속 사용할 수 있도록 하기 위함...) **/
                serverCred = addonCommonService.getServerCredFromExistInfo(currAddon);
                /** 인증서 생성 및 DynamicValues에 입력 **/
                dynamicValues = addonCommonService.setCertificateToDynamicValue(serverCred, dynamicValues, domain, ipAddress);
                dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_ADDRESS_LIST.getValue(), cluster.getNodePortUrl());


                /** Generate Patch Body **/
                patchBody = addonCommonService.generateAddonPatchData(patchBody, cluster, addonInstall, currAddon, dynamicValues);

                /**
                 * 3. istio configmap에 Host Address와 인증서 정보 Update (deployed 상태가 아니면 deployed 상태가 될때까지 대기...)
                 *  - 인증서, host 정보 모두 수정하여 addon upgrade 요청 (수정 후 또 재설치 됨..)
                 **/
                currAddon = configMapService.getConfigMap(cluster.getClusterSeq(), addonBaseNamespace, addonName);
                for (int i = 0; i < waitSecond; i++) {
                    if (currAddon == null) {
                        Thread.sleep(3000);
                        continue;
                    }

                    // deployed 상태가 될때까지 대기 후 update 처리
                    String addonStatus = Optional.ofNullable(currAddon.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_STATUS);
                    if (StringUtils.equalsIgnoreCase(KubeConstants.ADDON_STATUS_DEPLOYED, addonStatus)) {
                        break;
                    }
                    Thread.sleep(3000);
                    currAddon = configMapService.getConfigMap(cluster.getClusterSeq(), addonBaseNamespace, addonName);
                }

                if (StringUtils.isNotBlank(lb)) {
                    Map<String, Object> pmCaAddress = new HashMap<>();
                    pmCaAddress.put("op", JsonPatchOp.REPLACE.getValue());
                    if (!currAddon.getData().containsKey(AddonKeyItem.KIALI_ADDRESS_LIST.getValue())) {
                        pmCaAddress.put("op", JsonPatchOp.ADD.getValue());
                    }
                    pmCaAddress.put("path", String.format("/data/%s", AddonKeyItem.KIALI_ADDRESS_LIST.getValue()));
                    pmCaAddress.put("value", lb);
                    patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmCaAddress), JsonElement.class)).getAsJsonObject());
                }

                // Addon 수정 : Configmap의 host 정보만 수정...
                if (CollectionUtils.isNotEmpty(patchBody)) {
                    AddonUtils.setAddonStatus(patchBody, KubeConstants.ADDON_STATUS_PENDING_UPGRADE);
                    AddonUtils.setAddonUpdateAt(patchBody);
                    configMapService.patchConfigMap(cluster.getClusterSeq(), addonBaseNamespace, currAddon.getName(), patchBody);
                }

                waitSecond = 15; // Deploy가 완료될때까지 최대 45초 대기..
                for (int i = 0; i < waitSecond; i++) {
                    ConfigMapGuiVO addonStatusCm = configMapService.getConfigMap(cluster.getClusterSeq(), addonBaseNamespace, addonName);
                    String addonStatus = Optional.ofNullable(addonStatusCm.getLabels()).orElseGet(() ->com.google.common.collect.Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_STATUS);
                    if (StringUtils.equalsIgnoreCase(KubeConstants.ADDON_STATUS_DEPLOYED, addonStatus)) {
                        break;
                    }
                    Thread.sleep(3000);
                }
                Thread.sleep(3000); // deployed로 변경 이후 (혹은 waitSecond가 지난 후) 3초간 더 대기...
                try {
                    if(StringUtils.isBlank(releaseNamespace)) {
                        releaseNamespace = KubeConstants.ISTIO_SYSTEM_NAMESPACE;
                    }
                    workloadResourceService.rolloutRestartDeployment(cluster, releaseNamespace, "istio-ingressgateway");
                }
                catch (CocktailException ce) {
                    // 오류시 throw 하지 않고 warning 남기고 종료...
                    log.warn("istio-ingressgateway rollout restart failed... : {} : {}", cluster.getClusterSeq(), cluster.getClusterId());
                }
                catch (Exception ex) {
                    // 오류시 throw 하지 않고 warning 남기고 종료...
                    log.warn("istio-ingressgateway rollout restart failed... : {} : {}", cluster.getClusterSeq(), cluster.getClusterId());
                }
            }
            break;
            default:
                break;
        }
    }
}
