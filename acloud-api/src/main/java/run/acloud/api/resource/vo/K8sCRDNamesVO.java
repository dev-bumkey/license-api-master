package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2021. 06. 18.
 */
@Schema(title = "K8s CustomResourceDefinition Names 모델")
@Data
public class K8sCRDNamesVO {

	public static final String SERIALIZED_NAME_CATEGORIES = "categories";
	@Schema(title = "categories is a list of grouped resources this custom resource belongs to (e.g. 'all'). This is published in API discovery documents, and used by clients to support invocations like `kubectl get all`.")
	@SerializedName(SERIALIZED_NAME_CATEGORIES)
	private List<String> categories = null;

	public K8sCRDNamesVO categories(List<String> categories) {

		this.categories = categories;
		return this;
	}

	public K8sCRDNamesVO addCategoriesItem(String categoriesItem) {
		if (this.categories == null) {
			this.categories = new ArrayList<>();
		}
		this.categories.add(categoriesItem);
		return this;
	}

	public static final String SERIALIZED_NAME_KIND = "kind";
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "kind is the serialized kind of the resource. It is normally CamelCase and singular. Custom resource instances will use this value as the `kind` attribute in API calls.")
	@SerializedName(SERIALIZED_NAME_KIND)
	private String kind;

	public K8sCRDNamesVO kind(String kind) {

		this.kind = kind;
		return this;
	}

	public static final String SERIALIZED_NAME_LIST_KIND = "listKind";
	@Schema(title = "listKind is the serialized kind of the list for this resource. Defaults to `kind`List.")
	@SerializedName(SERIALIZED_NAME_LIST_KIND)
	private String listKind;

	public K8sCRDNamesVO listKind(String listKind) {

		this.listKind = listKind;
		return this;
	}

	public static final String SERIALIZED_NAME_PLURAL = "plural";
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "plural is the plural name of the resource to serve. The custom resources are served under `/apis/<group>/<version>/.../<plural>`. Must match the name of the CustomResourceDefinition (in the form `<names.plural>.<group>`). Must be all lowercase.")
	@SerializedName(SERIALIZED_NAME_PLURAL)
	private String plural;

	public K8sCRDNamesVO plural(String plural) {

		this.plural = plural;
		return this;
	}

	public static final String SERIALIZED_NAME_SHORT_NAMES = "shortNames";
	@Schema(title = "shortNames are short names for the resource, exposed in API discovery documents, and used by clients to support invocations like `kubectl get <shortname>`. It must be all lowercase.")
	@SerializedName(SERIALIZED_NAME_SHORT_NAMES)
	private List<String> shortNames = null;

	public K8sCRDNamesVO shortNames(List<String> shortNames) {

		this.shortNames = shortNames;
		return this;
	}

	public K8sCRDNamesVO addShortNamesItem(String shortNamesItem) {
		if (this.shortNames == null) {
			this.shortNames = new ArrayList<>();
		}
		this.shortNames.add(shortNamesItem);
		return this;
	}

	public static final String SERIALIZED_NAME_SINGULAR = "singular";
	@Schema(title = "singular is the singular name of the resource. It must be all lowercase. Defaults to lowercased `kind`.")
	@SerializedName(SERIALIZED_NAME_SINGULAR)
	private String singular;

	public K8sCRDNamesVO singular(String singular) {

		this.singular = singular;
		return this;
	}

}
