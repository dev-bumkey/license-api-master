package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TolerationEffects implements EnumCode {

	NO_SCHEDULE(KubeConstants.TOLERATION_EFFECT_NO_SCHEDULE),
	PREFER_NO_SCHEDULE(KubeConstants.TOLERATION_EFFECT_PREFER_NO_SCHEDULE),
	NO_EXECUTE(KubeConstants.TOLERATION_EFFECT_NO_EXECUTE)
	;

	@Getter
	private String value;

	TolerationEffects(String value){
		this.value = value;
	}

	public static List<String> getValueList(){
		return Arrays.asList(TolerationEffects.values()).stream().map(s -> s.getValue()).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
