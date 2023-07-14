package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterVolumeService;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.enums.PersistentVolumeType;
import run.acloud.api.cserver.enums.PortType;
import run.acloud.api.cserver.enums.VolumeType;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.DynamicLabelType;
import run.acloud.api.resource.vo.AdditionalLabelVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Acloud Label Management", description = "쿠버네티스 Ingress에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/label")
@RestController
@Validated
public class LabelController {

    @Autowired
    private ClusterVolumeService clusterVolumeService;

    @PostMapping("/{apiVersion}/dynamiclabel")
    @Operation(summary = "요청한 kind의 Resource에 붙여줄 Label을 생성하여 Map 형태로 응답", description = "Controller에서 Label 정보가 필요시 요청하여 사용")
    public AdditionalLabelVO makeDynamicLabel(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "LabelRequest", description = "LabelRequest", required = true) @RequestBody Map<String,String> params
    ) throws Exception {

        log.debug("[BEGIN] makeDynamicLabel");

        String reqKind = params.get(KubeConstants.LABELS_DYNAMIC_LABEL_KIND);
        String reqValue = params.get(KubeConstants.LABELS_DYNAMIC_LABEL_VALUE);
        String accessMode = params.get(KubeConstants.LABELS_DYNAMIC_LABEL_ACCESS_MODES);

        DynamicLabelType dynamicLabelType = DynamicLabelType.findDynamicLabelType(StringUtils.upperCase(reqKind));
        if (dynamicLabelType == null) {
            throw new CocktailException("Unsupported Type", ExceptionType.InvalidParameter);
        }

        // Response Object
        AdditionalLabelVO additionalLabelVO = new AdditionalLabelVO();
        switch (dynamicLabelType) {
            case PVC: { // @spec.storageClassName
                String clusterSeq = params.get(KubeConstants.LABELS_CLUSTERSEQ);
                String storageClassName = reqValue;

                if(StringUtils.isEmpty(storageClassName) || StringUtils.isEmpty(clusterSeq)) {
                    throw new CocktailException("Insufficient parameters: clusterSeq and StorageClassName is required.", ExceptionType.InvalidParameter);
                }

                ClusterVolumeVO clusterVolumeVO = clusterVolumeService.getStorageVolume(Integer.valueOf(clusterSeq), reqValue);
                if(clusterVolumeVO == null) {
                    throw new CocktailException("Storage Class.", ExceptionType.ClusterVolumeNotFound);
                }

                Map<String, String> map = new HashMap<>();
                for(String key : params.keySet()) {
                    if(!key.equalsIgnoreCase(KubeConstants.LABELS_DYNAMIC_LABEL_KIND) &&
                       !key.equalsIgnoreCase(KubeConstants.LABELS_CLUSTERID) &&
                        !key.equalsIgnoreCase(KubeConstants.LABELS_CLUSTERSEQ) &&
                        !key.equalsIgnoreCase(KubeConstants.LABELS_DYNAMIC_LABEL_VALUE) &&
                        !key.equalsIgnoreCase(KubeConstants.LABELS_DYNAMIC_LABEL_ACCESS_MODES)) {
                        map.put(key, params.get(key));
                    }
                }

                /**
                 * 2019.04.23:PersistentVolumeType을 실제 PVC의 AccessModes 정보를 기준으로 설정하도록 함.
                 */

                PersistentVolumeType persistentVolumeType = PersistentVolumeType.SHARED;
                if (accessMode != null && accessMode.endsWith("Once")) {
                    persistentVolumeType = PersistentVolumeType.SINGLE;
                }

                map.put(KubeConstants.CUSTOM_PERSISTENT_VOLUME_TYPE, persistentVolumeType.getCode());
                map.put(KubeConstants.CUSTOM_VOLUME_STORAGE, clusterVolumeVO.getVolumeSeq().toString());
                map.put(KubeConstants.CUSTOM_VOLUME_TYPE, VolumeType.PERSISTENT_VOLUME_LINKED.getCode());

                additionalLabelVO.setKind(reqKind);
                additionalLabelVO.setLabel(map);

                break;
            }
            case SERVICE: { // @spec.type
                PortType portType = PortType.findPortType(reqValue);
                if(portType != null) {
                    Map<String, String> map = new HashMap<>();
                    for(String key : params.keySet()) {
                        if(!key.equalsIgnoreCase(KubeConstants.LABELS_DYNAMIC_LABEL_KIND) &&
                            !key.equalsIgnoreCase(KubeConstants.LABELS_DYNAMIC_LABEL_VALUE)) {
                            map.put(key, params.get(key));
                        }
                    }
                    map.put(KubeConstants.LABELS_COCKTAIL_SERVICE_TYPE, portType.getCode());

                    additionalLabelVO.setKind(reqKind);
                    additionalLabelVO.setLabel(map);
                }
                break;
            }
        }

        log.debug("[END  ] makeDynamicLabel");

        return additionalLabelVO;
    }

}
