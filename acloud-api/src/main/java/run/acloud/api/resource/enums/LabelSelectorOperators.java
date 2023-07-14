package run.acloud.api.resource.enums;

import com.google.common.collect.Maps;
import lombok.Getter;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum LabelSelectorOperators implements EnumCode {

	IN(KubeConstants.NODE_AFFINITY_OPERATOR_IN, SelectorOperatorsValidType.NOT_NULL),
	NOT_IN(KubeConstants.NODE_AFFINITY_OPERATOR_NOT_IN, SelectorOperatorsValidType.NOT_NULL),
	EXISTS(KubeConstants.NODE_AFFINITY_OPERATOR_EXISTS, SelectorOperatorsValidType.NULL),
	DOES_NOT_EXIST(KubeConstants.NODE_AFFINITY_OPERATOR_DOES_NOT_EXIST, SelectorOperatorsValidType.NULL)
	;

	@Getter
	private String value;

	@Getter
	private SelectorOperatorsValidType validType;

	LabelSelectorOperators(String value, SelectorOperatorsValidType validType){
		this.value = value;
		this.validType = validType;
	}

	public Map<String, String> toMap() {
		Map<String, String> valueMap = Maps.newHashMap();
		valueMap.put("value", this.getValue());
		valueMap.put("validType", this.getValidType().getCode());

		return valueMap;
	}

	public static List<Map<String, String>> getValueList(){
		return Arrays.asList(LabelSelectorOperators.values()).stream().map(s -> s.toMap()).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
