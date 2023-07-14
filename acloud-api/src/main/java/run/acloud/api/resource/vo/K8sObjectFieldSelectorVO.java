package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "Object Field Selector 모델")
public class K8sObjectFieldSelectorVO extends BaseVO {

	@SerializedName("apiVersion")
	@Schema(name = "apiVersion", description = "Version of the schema the FieldPath is written in terms of, defaults to v1.")
	private String apiVersion;

	@SerializedName("fieldPath")
	@Schema(name = "fieldPath", requiredMode = Schema.RequiredMode.REQUIRED, description = "Path of the field to select in the specified API version.")
	private String fieldPath;
}
