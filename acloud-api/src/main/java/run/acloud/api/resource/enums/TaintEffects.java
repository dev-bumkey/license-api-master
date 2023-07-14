package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TaintEffects implements EnumCode {

	NO_SCHEDULE(KubeConstants.TAINT_EFFECT_NO_SCHEDULE),
	PREFER_NO_SCHEDULE(KubeConstants.TAINT_EFFECT_PREFER_NO_SCHEDULE),
	NO_EXECUTE(KubeConstants.TAINT_EFFECT_NO_EXECUTE)
	;

	@Getter
	private String value;

	TaintEffects(String value){
		this.value = value;
	}

	public static List<String> getTaintEffectList(){
		return Arrays.asList(TaintEffects.values()).stream().map(s -> s.getValue()).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
