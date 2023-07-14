package run.acloud.api.code.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.code.service.CodeService;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.code.vo.SubCodeVO;
import run.acloud.api.configuration.enums.*;
import run.acloud.api.cserver.enums.NetworkPolicyCreationType;
import run.acloud.api.cserver.enums.VolumePlugIn;
import run.acloud.api.pl.enums.PlResType;
import run.acloud.api.resource.enums.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
@Tag(name = "Code", description = "칵테일 코드에 대한 관리 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/code")
public class CodeController {

    private CodeService codeService;

	@Autowired
    public CodeController(CodeService codeService) {
	    this.codeService = codeService;
    }

    @Operation(summary = "그룹 아이디에 해당하는 코드 리스트 조회", description = "그룹 아이디에 해당하는 코드 리스트 조회한다.")
    @GetMapping("/{groupId}")
    public List<CodeVO> getCodes(@PathVariable String groupId) throws Exception {
    	log.debug("[BEGIN] getCodes");

        List<CodeVO> result = codeService.getCodes(groupId);
    	
        log.debug("[END  ] getCodes");
        return result;
    }

    @Operation(summary = "그룹 아이디/서브그룹 아이디에 해당하는 서브 코드 리스트 조회", description = "그룹 아이디/서브그룹 아이디에 해당하는 서브 코드 리스트를 조회한다.")
    @GetMapping("/{groupId}/{subGroupId}")
    public List<SubCodeVO> getSubCodes(@PathVariable String groupId, @PathVariable String subGroupId) throws Exception {
    	log.debug("[BEGIN] getSubCodes");

        List<SubCodeVO> result = codeService.getSubCodes(groupId, subGroupId);
    	
        log.debug("[END  ] getSubCodes");
        return result;
    }

    @Operation(summary = "Enum명 으로 enum code 목록 조회")
    @GetMapping("/group/{groupName}")
    public Object getCodesByEnum(
            @Parameter(name = "groupName"
                    , schema = @Schema(allowableValues = {"PROJECT_TYPE",
                                        "TAINT_EFFECTS",
                                        "TOLERATION_EFFECTS",
                                        "TOLERATION_OPERATORS",
                                        "METRIC_TARGET_TYPE",
                                        "ISSUE_TYPE",
                                        "ISSUE_ROLE",
                                        "HISTORY_STATE",
                                        "USER_ROLE",
                                        "USER_GRANT",
                                        "USER_AUTH_TYPE",
                                        "SECRET_TYPE",
                                        "FS_GROUP_RULE",
                                        "SE_LINUX_RULE",
                                        "RUN_AS_USER_RULE",
                                        "RUN_AS_GROUP_RULE",
                                        "SUPPLEMENTAL_GROUPS_RULE",
                                        "NODE_SELECTOR_OPERATORS",
                                        "LABEL_SELECTOR_OPERATORS",
                                        "SCOPE_SELECTOR_OPERATORS",
                                        "LIMIT_RANGE_TYPE",
                                        "NETWORK_POLICY_CREATION_TYPE",
                                        "K8S_VOLUME_TYPE",
                                        "PL_RES_TYPE",
                                        "EXTERNAL_REGISTRY_TYPE",
                                        "HPA_SCALING_POLICY_TYPE",
                                        "HPA_SELECT_POLICY",
                                        "CERT_ISSUER_SCOPE",
                                        "CERT_USAGES",
                                        "CERT_ISSUE_TYPE",
                                        "VOLUME_PLUGIN"})
                    , required = true) @PathVariable String groupName
    ) throws Exception {

        switch (groupName) {
            case "PROJECT_TYPE":
                return Optional.ofNullable(Arrays.asList(ServiceRegistryType.values())).orElseGet(() ->Lists.newArrayList());
            case "TAINT_EFFECT_TYPE":
                return Optional.ofNullable(TaintEffects.getTaintEffectList()).orElseGet(() ->Lists.newArrayList());
            case "TOLERATION_EFFECTS":
                return Optional.ofNullable(TolerationEffects.getValueList()).orElseGet(() ->Lists.newArrayList());
            case "TOLERATION_OPERATORS":
                return Optional.ofNullable(TolerationOperators.getValueList()).orElseGet(() ->Lists.newArrayList());
            case "METRIC_TARGET_TYPE":
                return Optional.ofNullable(MetricTargetType.getList()).orElseGet(() ->Lists.newArrayList());
            case "ISSUE_TYPE":
                return Optional.ofNullable(Arrays.asList(IssueType.values())).orElseGet(() ->Lists.newArrayList());
            case "ISSUE_ROLE":
                return Optional.ofNullable(IssueRole.toMap()).orElseGet(() ->Maps.newHashMap());
            case "HISTORY_STATE":
                return Optional.ofNullable(Arrays.asList(HistoryState.values())).orElseGet(() ->Lists.newArrayList());
            case "USER_ROLE":
                return Optional.ofNullable(Arrays.asList(UserRole.values())).orElseGet(() ->Lists.newArrayList());
            case "USER_GRANT":
                return Optional.ofNullable(UserGrant.getList()).orElseGet(() ->Lists.newArrayList());
            case "USER_AUTH_TYPE":
                return Optional.ofNullable(UserAuthType.getValueList()).orElseGet(() ->Lists.newArrayList());
            case "SECRET_TYPE":
                return Optional.ofNullable(SecretType.getSupportedSecretTypes()).orElseGet(() ->Lists.newArrayList());
            case "FS_GROUP_RULE":
                return Optional.ofNullable(FSGroupRule.getList()).orElseGet(() ->Lists.newArrayList());
            case "SE_LINUX_RULE":
                return Optional.ofNullable(SELinuxRule.getList()).orElseGet(() ->Lists.newArrayList());
            case "RUN_AS_USER_RULE":
                return Optional.ofNullable(RunAsUserRule.getList()).orElseGet(() ->Lists.newArrayList());
            case "RUN_AS_GROUP_RULE":
                return Optional.ofNullable(RunAsGroupRule.getList()).orElseGet(() ->Lists.newArrayList());
            case "SUPPLEMENTAL_GROUPS_RULE":
                return Optional.ofNullable(SupplementalGroupsRule.getList()).orElseGet(() ->Lists.newArrayList());
            case "NODE_SELECTOR_OPERATORS":
                return Optional.ofNullable(NodeSelectorOperators.getValueList()).orElseGet(() ->Lists.newArrayList());
            case "LABEL_SELECTOR_OPERATORS":
                return Optional.ofNullable(LabelSelectorOperators.getValueList()).orElseGet(() ->Lists.newArrayList());
            case "SCOPE_SELECTOR_OPERATORS":
                return Optional.ofNullable(ScopeSelectorOperators.getValueList()).orElseGet(() ->Lists.newArrayList());
            case "LIMIT_RANGE_TYPE":
                return Optional.ofNullable(LimitRangeType.getList()).orElseGet(() ->Lists.newArrayList());
            case "NETWORK_POLICY_CREATION_TYPE":
                return Optional.ofNullable(NetworkPolicyCreationType.getList()).orElseGet(() ->Lists.newArrayList());
            case "K8S_VOLUME_TYPE":
                return Optional.ofNullable(K8sVolumeType.getList()).orElseGet(() ->Lists.newArrayList());
            case "PL_RES_TYPE":
                return Optional.ofNullable(PlResType.getList()).orElseGet(() ->Lists.newArrayList());
            case "EXTERNAL_REGISTRY_TYPE":
                return Optional.ofNullable(ImageRegistryType.getList()).orElseGet(() ->Lists.newArrayList());
            case "HPA_SCALING_POLICY_TYPE":
                return Optional.ofNullable(HpaScalingPolicyType.getCodeList()).orElseGet(() ->Lists.newArrayList());
            case "HPA_SELECT_POLICY":
                return Optional.ofNullable(HpaSelectPolicy.getCodeList()).orElseGet(() ->Lists.newArrayList());
            case "CERT_ISSUER_SCOPE":
                return Optional.ofNullable(CertIssuerScope.getCodeList()).orElseGet(() ->Lists.newArrayList());
            case "CERT_USAGES":
                return Optional.ofNullable(CertUsages.getCodeList()).orElseGet(() ->Lists.newArrayList());
            case "CERT_ISSUE_TYPE":
                return Optional.ofNullable(CertIssueType.getCodeList()).orElseGet(() ->Lists.newArrayList());
            case "VOLUME_PLUGIN":
                return Optional.ofNullable(VolumePlugIn.getVolumePlugInCodeList()).orElseGet(() ->Lists.newArrayList());
            default:
                return Lists.newArrayList();
        }

    }

    @Operation(summary = "다중 Enum명 으로 enum code 목록 조회", description = "다중 Enum명 으로 enum code 목록 조회.")
    @GetMapping("/group/codes/enum")
    public Map<String, Object> getMultiCodesByEnum(
            @Parameter(name = "groupNames"
                    , description = "PROJECT_TYPE, " +
                                "TAINT_EFFECTS, " +
                                "TOLERATION_EFFECTS, " +
                                "TOLERATION_OPERATORS, " +
                                "METRIC_TARGET_TYPE, " +
                                "ISSUE_TYPE, " +
                                "ISSUE_ROLE, " +
                                "HISTORY_STATE, " +
                                "USER_ROLE, " +
                                "USER_GRANT, " +
                                "USER_AUTH_TYPE, " +
                                "SECRET_TYPE, " +
                                "FS_GROUP_RULE, " +
                                "SE_LINUX_RULE, " +
                                "RUN_AS_USER_RULE, " +
                                "RUN_AS_GROUP_RULE, " +
                                "SUPPLEMENTAL_GROUPS_RULE, " +
                                "NODE_SELECTOR_OPERATORS, " +
                                "LABEL_SELECTOR_OPERATORS, " +
                                "SCOPE_SELECTOR_OPERATORS, " +
                                "LIMIT_RANGE_TYPE, " +
                                "NETWORK_POLICY_CREATION_TYPE, " +
                                "K8S_VOLUME_TYPE, " +
                                "PL_RES_TYPE, " +
                                "EXTERNAL_REGISTRY_TYPE, " +
                                "HPA_SCALING_POLICY_TYPE, " +
                                "HPA_SELECT_POLICY, " +
                                "CERT_ISSUER_SCOPE, " +
                                "CERT_USAGES, " +
                                "CERT_ISSUE_TYPE, " +
                                "VOLUME_PLUGIN"
                    , required = true) @RequestParam List<String> groupNames
    ) throws Exception {

        Map<String, Object> codeMap = Maps.newHashMap();

	    if (CollectionUtils.isNotEmpty(groupNames)) {
	        for (String groupNmae : groupNames) {
                codeMap.put(groupNmae, this.getCodesByEnum(groupNmae));
            }
        }

	    return codeMap;

    }
}
