package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Schema(title="PackageEvent", description="패키지 이벤트 정보 모델.")
public class PackageEventVO {

	private String clusterId;

	private String kind;

	private String name;

	private String namespace;

	private Map<String, String> labels;

	private String type;

	private int count;

	private String firstTime;

	private String lastTime;

	private String reason;

	private String note;

	private String source;

	private String deleted;

	private String json;
}
