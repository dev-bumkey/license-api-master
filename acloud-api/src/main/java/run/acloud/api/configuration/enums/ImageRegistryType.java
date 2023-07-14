package run.acloud.api.configuration.enums;

import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(name = "ImageRegistryType", description = "ImageRegistryType")
public enum ImageRegistryType implements EnumCode {
	@Schema(name = "HARBOR", description = "harbor")
	HARBOR("harbor", "Y"),
	@Schema(name = "DOCKER_HUB", description = "docker-hub")
	DOCKER_HUB("docker-hub", "N");

	@Getter
	private String value;

	@Getter
	private String useYn;

	ImageRegistryType(String value, String useYn) {
		this.value = value;
		this.useYn = useYn;
	}

	public Map<String, String> toMap() {
		Map<String, String> valueMap = Maps.newHashMap();
		valueMap.put("code", this.getCode());
		valueMap.put("value", this.getValue());
		return valueMap;
	}

	public static List<Map<String, String>> getList() {
		return Arrays.stream(ImageRegistryType.values()).filter(e -> (BooleanUtils.toBoolean(e.getUseYn()))).map(ImageRegistryType::toMap).collect(Collectors.toList());
	}

	public static List<String> getValueList() {
		return Arrays.stream(ImageRegistryType.values()).filter(e -> (BooleanUtils.toBoolean(e.getUseYn()))).map(ImageRegistryType::getValue).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
