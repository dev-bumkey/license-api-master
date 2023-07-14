package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created on 2021. 06. 18.
 */
@Schema(title = "K8s CustomResourceDefinition Column 모델")
@Data
public class K8sCRDColumnVO {

	public static final String SERIALIZED_NAME_DESCRIPTION = "description";
	@Schema(title = "description is a human readable description of this column.")
	@SerializedName(SERIALIZED_NAME_DESCRIPTION)
	private String description;

	public K8sCRDColumnVO description(String description) {

		this.description = description;
		return this;
	}

	public static final String SERIALIZED_NAME_FORMAT = "format";
	@Schema(title = "format is an optional OpenAPI type definition for this column. The 'name' format is applied to the primary identifier column to assist in clients identifying column is the resource name. See https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#data-types for details.")
	@SerializedName(SERIALIZED_NAME_FORMAT)
	private String format;

	public K8sCRDColumnVO format(String format) {

		this.format = format;
		return this;
	}

	public static final String SERIALIZED_NAME_JSON_PATH = "jsonPath";
	@Schema(
			requiredMode = Schema.RequiredMode.REQUIRED,
			description =
					"jsonPath is a simple JSON path (i.e. with array notation) which is evaluated against each custom resource to produce the value for this column.")
	@SerializedName(value = SERIALIZED_NAME_JSON_PATH, alternate = "JSONPath")
	private String jsonPath;

	public K8sCRDColumnVO jsonPath(String jsonPath) {

		this.jsonPath = jsonPath;
		return this;
	}

	public static final String SERIALIZED_NAME_NAME = "name";
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "name is a human readable name for the column.")
	@SerializedName(SERIALIZED_NAME_NAME)
	private String name;

	public K8sCRDColumnVO name(String name) {

		this.name = name;
		return this;
	}

	public static final String SERIALIZED_NAME_PRIORITY = "priority";
	@Schema(
			description =
					"priority is an integer defining the relative importance of this column compared to others. Lower numbers are considered higher priority. Columns that may be omitted in limited space scenarios should be given a priority greater than 0.")
	@SerializedName(SERIALIZED_NAME_PRIORITY)
	private Integer priority;

	public K8sCRDColumnVO priority(Integer priority) {

		this.priority = priority;
		return this;
	}

	public static final String SERIALIZED_NAME_TYPE = "type";
	@Schema(
			requiredMode = Schema.RequiredMode.REQUIRED,
			description =
					"type is an OpenAPI type definition for this column. See https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#data-types for details.")
	@SerializedName(SERIALIZED_NAME_TYPE)
	private String type;

	public K8sCRDColumnVO type(String type) {

		this.type = type;
		return this;
	}

}
