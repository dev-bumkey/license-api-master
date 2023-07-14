package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class K8sCustomObjectVO {

	@Schema(title = "name")
	private String name;

	@Schema(title = "group")
	private String group;

	@Schema(title = "namespace")
	private String namespace;

	@Schema(title = "labels")
	private Map<String, String> labels = new HashMap<>();

	@Schema(title = "annotations")
	private Map<String, String> annotations = new HashMap<>();

	@Schema(title = "spec")
	private Map<String, Object> spec = new HashMap<>();

	@Schema(title = "생성시간")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private OffsetDateTime creationTimestamp;

	@Schema(title = "배포 정보 (yaml)")
	private String yaml;

}
