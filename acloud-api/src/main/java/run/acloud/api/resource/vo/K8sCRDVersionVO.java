package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2021. 06. 18.
 */
@Schema(title = "K8s CustomResourceDefinition Version 모델")
@Data
public class K8sCRDVersionVO {

	public static final String SERIALIZED_NAME_ADDITIONAL_PRINTER_COLUMNS = "additionalPrinterColumns";
	@Schema(
			description =
					"additionalPrinterColumns specifies additional columns returned in Table output. See https://kubernetes.io/docs/reference/using-api/api-concepts/#receiving-resources-as-tables for details. If no columns are specified, a single column displaying the age of the custom resource is used.")
	@SerializedName(SERIALIZED_NAME_ADDITIONAL_PRINTER_COLUMNS)
	private List<K8sCRDColumnVO> additionalPrinterColumns = null;

	public K8sCRDVersionVO additionalPrinterColumns(List<K8sCRDColumnVO> additionalPrinterColumns) {
		this.additionalPrinterColumns = additionalPrinterColumns;
		return this;
	}

	public K8sCRDVersionVO addAdditionalPrinterColumnsItem(K8sCRDColumnVO additionalPrinterColumnsItem) {
		if (this.additionalPrinterColumns == null) {
			this.additionalPrinterColumns = new ArrayList<>();
		}
		this.additionalPrinterColumns.add(additionalPrinterColumnsItem);
		return this;
	}

	public static final String SERIALIZED_NAME_DEPRECATED = "deprecated";
	@Schema(
			description =
					"deprecated indicates this version of the custom resource API is deprecated. When set to true, API requests to this version receive a warning header in the server response. Defaults to false.")
	@SerializedName(SERIALIZED_NAME_DEPRECATED)
	private Boolean deprecated;

	public K8sCRDVersionVO deprecated(Boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public static final String SERIALIZED_NAME_DEPRECATION_WARNING = "deprecationWarning";
	@Schema(
			description =
					"deprecationWarning overrides the default warning returned to API clients. May only be set when `deprecated` is true. The default warning indicates this version is deprecated and recommends use of the newest served version of equal or greater stability, if one exists.")
	@SerializedName(SERIALIZED_NAME_DEPRECATION_WARNING)
	private String deprecationWarning;

	public K8sCRDVersionVO deprecationWarning(String deprecationWarning) {
		this.deprecationWarning = deprecationWarning;
		return this;
	}

	public static final String SERIALIZED_NAME_NAME = "name";
	@Schema(
			requiredMode = Schema.RequiredMode.REQUIRED,
			description =
					"name is the version name, e.g. “v1”, “v2beta1”, etc. The custom resources are served under this version at `/apis/<group>/<version>/...` if `served` is true.")
	@SerializedName(SERIALIZED_NAME_NAME)
	private String name;

	public K8sCRDVersionVO name(String name) {
		this.name = name;
		return this;
	}

	public static final String SERIALIZED_NAME_SERVED = "served";
	@Schema(
			requiredMode = Schema.RequiredMode.REQUIRED,
			description =  "served is a flag enabling/disabling this version from being served via REST APIs")
	@SerializedName(SERIALIZED_NAME_SERVED)
	private Boolean served;

	public K8sCRDVersionVO served(Boolean served) {

		this.served = served;
		return this;
	}

	public static final String SERIALIZED_NAME_STORAGE = "storage";
	@Schema(
			requiredMode = Schema.RequiredMode.REQUIRED,
			description =
					"storage indicates this version should be used when persisting custom resources to storage. There must be exactly one version with storage=true.")
	@SerializedName(SERIALIZED_NAME_STORAGE)
	private Boolean storage;

	public K8sCRDVersionVO storage(Boolean storage) {
		this.storage = storage;
		return this;
	}
}
