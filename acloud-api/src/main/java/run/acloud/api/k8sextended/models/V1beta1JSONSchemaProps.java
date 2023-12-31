/*
Copyright 2021 The Kubernetes Authors.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package run.acloud.api.k8sextended.models;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;

/** JSONSchemaProps is a JSON-Schema following Specification Draft 4 (http://json-schema.org/). */
@Schema(
    description =
        "JSONSchemaProps is a JSON-Schema following Specification Draft 4 (http://json-schema.org/).")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-01-04T09:55:14.976Z[Etc/UTC]")
public class V1beta1JSONSchemaProps {
  public static final String SERIALIZED_NAME_$_REF = "$ref";

  @SerializedName(SERIALIZED_NAME_$_REF)
  private String $ref;

  public static final String SERIALIZED_NAME_$_SCHEMA = "$schema";

  @SerializedName(SERIALIZED_NAME_$_SCHEMA)
  private String $schema;

  public static final String SERIALIZED_NAME_ADDITIONAL_ITEMS = "additionalItems";

  @SerializedName(SERIALIZED_NAME_ADDITIONAL_ITEMS)
  private Object additionalItems;

  public static final String SERIALIZED_NAME_ADDITIONAL_PROPERTIES = "additionalProperties";

  @SerializedName(SERIALIZED_NAME_ADDITIONAL_PROPERTIES)
  private Object additionalProperties;

  public static final String SERIALIZED_NAME_ALL_OF = "allOf";

  @SerializedName(SERIALIZED_NAME_ALL_OF)
  private List<V1beta1JSONSchemaProps> allOf = null;

  public static final String SERIALIZED_NAME_ANY_OF = "anyOf";

  @SerializedName(SERIALIZED_NAME_ANY_OF)
  private List<V1beta1JSONSchemaProps> anyOf = null;

  public static final String SERIALIZED_NAME_DEFAULT = "default";

  @SerializedName(SERIALIZED_NAME_DEFAULT)
  private Object _default;

  public static final String SERIALIZED_NAME_DEFINITIONS = "definitions";

  @SerializedName(SERIALIZED_NAME_DEFINITIONS)
  private Map<String, V1beta1JSONSchemaProps> definitions = null;

  public static final String SERIALIZED_NAME_DEPENDENCIES = "dependencies";

  @SerializedName(SERIALIZED_NAME_DEPENDENCIES)
  private Map<String, Object> dependencies = null;

  public static final String SERIALIZED_NAME_DESCRIPTION = "description";

  @SerializedName(SERIALIZED_NAME_DESCRIPTION)
  private String description;

  public static final String SERIALIZED_NAME_ENUM = "enum";

  @SerializedName(SERIALIZED_NAME_ENUM)
  private List<Object> _enum = null;

  public static final String SERIALIZED_NAME_EXAMPLE = "example";

  @SerializedName(SERIALIZED_NAME_EXAMPLE)
  private Object example;

  public static final String SERIALIZED_NAME_EXCLUSIVE_MAXIMUM = "exclusiveMaximum";

  @SerializedName(SERIALIZED_NAME_EXCLUSIVE_MAXIMUM)
  private Boolean exclusiveMaximum;

  public static final String SERIALIZED_NAME_EXCLUSIVE_MINIMUM = "exclusiveMinimum";

  @SerializedName(SERIALIZED_NAME_EXCLUSIVE_MINIMUM)
  private Boolean exclusiveMinimum;

  public static final String SERIALIZED_NAME_EXTERNAL_DOCS = "externalDocs";

  @SerializedName(SERIALIZED_NAME_EXTERNAL_DOCS)
  private V1beta1ExternalDocumentation externalDocs;

  public static final String SERIALIZED_NAME_FORMAT = "format";

  @SerializedName(SERIALIZED_NAME_FORMAT)
  private String format;

  public static final String SERIALIZED_NAME_ID = "id";

  @SerializedName(SERIALIZED_NAME_ID)
  private String id;

  public static final String SERIALIZED_NAME_ITEMS = "items";

  @SerializedName(SERIALIZED_NAME_ITEMS)
  private Object items;

  public static final String SERIALIZED_NAME_MAX_ITEMS = "maxItems";

  @SerializedName(SERIALIZED_NAME_MAX_ITEMS)
  private Long maxItems;

  public static final String SERIALIZED_NAME_MAX_LENGTH = "maxLength";

  @SerializedName(SERIALIZED_NAME_MAX_LENGTH)
  private Long maxLength;

  public static final String SERIALIZED_NAME_MAX_PROPERTIES = "maxProperties";

  @SerializedName(SERIALIZED_NAME_MAX_PROPERTIES)
  private Long maxProperties;

  public static final String SERIALIZED_NAME_MAXIMUM = "maximum";

  @SerializedName(SERIALIZED_NAME_MAXIMUM)
  private Double maximum;

  public static final String SERIALIZED_NAME_MIN_ITEMS = "minItems";

  @SerializedName(SERIALIZED_NAME_MIN_ITEMS)
  private Long minItems;

  public static final String SERIALIZED_NAME_MIN_LENGTH = "minLength";

  @SerializedName(SERIALIZED_NAME_MIN_LENGTH)
  private Long minLength;

  public static final String SERIALIZED_NAME_MIN_PROPERTIES = "minProperties";

  @SerializedName(SERIALIZED_NAME_MIN_PROPERTIES)
  private Long minProperties;

  public static final String SERIALIZED_NAME_MINIMUM = "minimum";

  @SerializedName(SERIALIZED_NAME_MINIMUM)
  private Double minimum;

  public static final String SERIALIZED_NAME_MULTIPLE_OF = "multipleOf";

  @SerializedName(SERIALIZED_NAME_MULTIPLE_OF)
  private Double multipleOf;

  public static final String SERIALIZED_NAME_NOT = "not";

  @SerializedName(SERIALIZED_NAME_NOT)
  private V1beta1JSONSchemaProps not;

  public static final String SERIALIZED_NAME_NULLABLE = "nullable";

  @SerializedName(SERIALIZED_NAME_NULLABLE)
  private Boolean nullable;

  public static final String SERIALIZED_NAME_ONE_OF = "oneOf";

  @SerializedName(SERIALIZED_NAME_ONE_OF)
  private List<V1beta1JSONSchemaProps> oneOf = null;

  public static final String SERIALIZED_NAME_PATTERN = "pattern";

  @SerializedName(SERIALIZED_NAME_PATTERN)
  private String pattern;

  public static final String SERIALIZED_NAME_PATTERN_PROPERTIES = "patternProperties";

  @SerializedName(SERIALIZED_NAME_PATTERN_PROPERTIES)
  private Map<String, V1beta1JSONSchemaProps> patternProperties = null;

  public static final String SERIALIZED_NAME_PROPERTIES = "properties";

  @SerializedName(SERIALIZED_NAME_PROPERTIES)
  private Map<String, V1beta1JSONSchemaProps> properties = null;

  public static final String SERIALIZED_NAME_REQUIRED = "required";

  @SerializedName(SERIALIZED_NAME_REQUIRED)
  private List<String> required = null;

  public static final String SERIALIZED_NAME_TITLE = "title";

  @SerializedName(SERIALIZED_NAME_TITLE)
  private String title;

  public static final String SERIALIZED_NAME_TYPE = "type";

  @SerializedName(SERIALIZED_NAME_TYPE)
  private String type;

  public static final String SERIALIZED_NAME_UNIQUE_ITEMS = "uniqueItems";

  @SerializedName(SERIALIZED_NAME_UNIQUE_ITEMS)
  private Boolean uniqueItems;

  public static final String SERIALIZED_NAME_X_KUBERNETES_EMBEDDED_RESOURCE =
      "x-kubernetes-embedded-resource";

  @SerializedName(SERIALIZED_NAME_X_KUBERNETES_EMBEDDED_RESOURCE)
  private Boolean xKubernetesEmbeddedResource;

  public static final String SERIALIZED_NAME_X_KUBERNETES_INT_OR_STRING =
      "x-kubernetes-int-or-string";

  @SerializedName(SERIALIZED_NAME_X_KUBERNETES_INT_OR_STRING)
  private Boolean xKubernetesIntOrString;

  public static final String SERIALIZED_NAME_X_KUBERNETES_LIST_MAP_KEYS =
      "x-kubernetes-list-map-keys";

  @SerializedName(SERIALIZED_NAME_X_KUBERNETES_LIST_MAP_KEYS)
  private List<String> xKubernetesListMapKeys = null;

  public static final String SERIALIZED_NAME_X_KUBERNETES_LIST_TYPE = "x-kubernetes-list-type";

  @SerializedName(SERIALIZED_NAME_X_KUBERNETES_LIST_TYPE)
  private String xKubernetesListType;

  public static final String SERIALIZED_NAME_X_KUBERNETES_MAP_TYPE = "x-kubernetes-map-type";

  @SerializedName(SERIALIZED_NAME_X_KUBERNETES_MAP_TYPE)
  private String xKubernetesMapType;

  public static final String SERIALIZED_NAME_X_KUBERNETES_PRESERVE_UNKNOWN_FIELDS =
      "x-kubernetes-preserve-unknown-fields";

  @SerializedName(SERIALIZED_NAME_X_KUBERNETES_PRESERVE_UNKNOWN_FIELDS)
  private Boolean xKubernetesPreserveUnknownFields;

  public V1beta1JSONSchemaProps $ref(String $ref) {

    this.$ref = $ref;
    return this;
  }

  /**
   * Get $ref
   *
   * @return $ref
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public String get$Ref() {
    return $ref;
  }

  public void set$Ref(String $ref) {
    this.$ref = $ref;
  }

  public V1beta1JSONSchemaProps $schema(String $schema) {

    this.$schema = $schema;
    return this;
  }

  /**
   * Get $schema
   *
   * @return $schema
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public String get$Schema() {
    return $schema;
  }

  public void set$Schema(String $schema) {
    this.$schema = $schema;
  }

  public V1beta1JSONSchemaProps additionalItems(Object additionalItems) {

    this.additionalItems = additionalItems;
    return this;
  }

  /**
   * JSONSchemaPropsOrBool represents JSONSchemaProps or a boolean value. Defaults to true for the
   * boolean property.
   *
   * @return additionalItems
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "JSONSchemaPropsOrBool represents JSONSchemaProps or a boolean value. Defaults to true for the boolean property.")
  public Object getAdditionalItems() {
    return additionalItems;
  }

  public void setAdditionalItems(Object additionalItems) {
    this.additionalItems = additionalItems;
  }

  public V1beta1JSONSchemaProps additionalProperties(Object additionalProperties) {

    this.additionalProperties = additionalProperties;
    return this;
  }

  /**
   * JSONSchemaPropsOrBool represents JSONSchemaProps or a boolean value. Defaults to true for the
   * boolean property.
   *
   * @return additionalProperties
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "JSONSchemaPropsOrBool represents JSONSchemaProps or a boolean value. Defaults to true for the boolean property.")
  public Object getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(Object additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public V1beta1JSONSchemaProps allOf(List<V1beta1JSONSchemaProps> allOf) {

    this.allOf = allOf;
    return this;
  }

  public V1beta1JSONSchemaProps addAllOfItem(V1beta1JSONSchemaProps allOfItem) {
    if (this.allOf == null) {
      this.allOf = new ArrayList<>();
    }
    this.allOf.add(allOfItem);
    return this;
  }

  /**
   * Get allOf
   *
   * @return allOf
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public List<V1beta1JSONSchemaProps> getAllOf() {
    return allOf;
  }

  public void setAllOf(List<V1beta1JSONSchemaProps> allOf) {
    this.allOf = allOf;
  }

  public V1beta1JSONSchemaProps anyOf(List<V1beta1JSONSchemaProps> anyOf) {

    this.anyOf = anyOf;
    return this;
  }

  public V1beta1JSONSchemaProps addAnyOfItem(V1beta1JSONSchemaProps anyOfItem) {
    if (this.anyOf == null) {
      this.anyOf = new ArrayList<>();
    }
    this.anyOf.add(anyOfItem);
    return this;
  }

  /**
   * Get anyOf
   *
   * @return anyOf
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public List<V1beta1JSONSchemaProps> getAnyOf() {
    return anyOf;
  }

  public void setAnyOf(List<V1beta1JSONSchemaProps> anyOf) {
    this.anyOf = anyOf;
  }

  public V1beta1JSONSchemaProps _default(Object _default) {

    this._default = _default;
    return this;
  }

  /**
   * default is a default value for undefined object fields. Defaulting is a beta feature under the
   * CustomResourceDefaulting feature gate. CustomResourceDefinitions with defaults must be created
   * using the v1 (or newer) CustomResourceDefinition API.
   *
   * @return _default
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "default is a default value for undefined object fields. Defaulting is a beta feature under the CustomResourceDefaulting feature gate. CustomResourceDefinitions with defaults must be created using the v1 (or newer) CustomResourceDefinition API.")
  public Object getDefault() {
    return _default;
  }

  public void setDefault(Object _default) {
    this._default = _default;
  }

  public V1beta1JSONSchemaProps definitions(Map<String, V1beta1JSONSchemaProps> definitions) {

    this.definitions = definitions;
    return this;
  }

  public V1beta1JSONSchemaProps putDefinitionsItem(
      String key, V1beta1JSONSchemaProps definitionsItem) {
    if (this.definitions == null) {
      this.definitions = new HashMap<>();
    }
    this.definitions.put(key, definitionsItem);
    return this;
  }

  /**
   * Get definitions
   *
   * @return definitions
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Map<String, V1beta1JSONSchemaProps> getDefinitions() {
    return definitions;
  }

  public void setDefinitions(Map<String, V1beta1JSONSchemaProps> definitions) {
    this.definitions = definitions;
  }

  public V1beta1JSONSchemaProps dependencies(Map<String, Object> dependencies) {

    this.dependencies = dependencies;
    return this;
  }

  public V1beta1JSONSchemaProps putDependenciesItem(String key, Object dependenciesItem) {
    if (this.dependencies == null) {
      this.dependencies = new HashMap<>();
    }
    this.dependencies.put(key, dependenciesItem);
    return this;
  }

  /**
   * Get dependencies
   *
   * @return dependencies
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Map<String, Object> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Map<String, Object> dependencies) {
    this.dependencies = dependencies;
  }

  public V1beta1JSONSchemaProps description(String description) {

    this.description = description;
    return this;
  }

  /**
   * Get description
   *
   * @return description
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public V1beta1JSONSchemaProps _enum(List<Object> _enum) {

    this._enum = _enum;
    return this;
  }

  public V1beta1JSONSchemaProps addEnumItem(Object _enumItem) {
    if (this._enum == null) {
      this._enum = new ArrayList<>();
    }
    this._enum.add(_enumItem);
    return this;
  }

  /**
   * Get _enum
   *
   * @return _enum
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public List<Object> getEnum() {
    return _enum;
  }

  public void setEnum(List<Object> _enum) {
    this._enum = _enum;
  }

  public V1beta1JSONSchemaProps example(Object example) {

    this.example = example;
    return this;
  }

  /**
   * JSON represents any valid JSON value. These types are supported: bool, int64, float64, string,
   * []interface{}, map[string]interface{} and nil.
   *
   * @return example
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "JSON represents any valid JSON value. These types are supported: bool, int64, float64, string, []interface{}, map[string]interface{} and nil.")
  public Object getExample() {
    return example;
  }

  public void setExample(Object example) {
    this.example = example;
  }

  public V1beta1JSONSchemaProps exclusiveMaximum(Boolean exclusiveMaximum) {

    this.exclusiveMaximum = exclusiveMaximum;
    return this;
  }

  /**
   * Get exclusiveMaximum
   *
   * @return exclusiveMaximum
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Boolean getExclusiveMaximum() {
    return exclusiveMaximum;
  }

  public void setExclusiveMaximum(Boolean exclusiveMaximum) {
    this.exclusiveMaximum = exclusiveMaximum;
  }

  public V1beta1JSONSchemaProps exclusiveMinimum(Boolean exclusiveMinimum) {

    this.exclusiveMinimum = exclusiveMinimum;
    return this;
  }

  /**
   * Get exclusiveMinimum
   *
   * @return exclusiveMinimum
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Boolean getExclusiveMinimum() {
    return exclusiveMinimum;
  }

  public void setExclusiveMinimum(Boolean exclusiveMinimum) {
    this.exclusiveMinimum = exclusiveMinimum;
  }

  public V1beta1JSONSchemaProps externalDocs(V1beta1ExternalDocumentation externalDocs) {

    this.externalDocs = externalDocs;
    return this;
  }

  /**
   * Get externalDocs
   *
   * @return externalDocs
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1beta1ExternalDocumentation getExternalDocs() {
    return externalDocs;
  }

  public void setExternalDocs(V1beta1ExternalDocumentation externalDocs) {
    this.externalDocs = externalDocs;
  }

  public V1beta1JSONSchemaProps format(String format) {

    this.format = format;
    return this;
  }

  /**
   * format is an OpenAPI v3 format string. Unknown formats are ignored. The following formats are
   * validated: - bsonobjectid: a bson object ID, i.e. a 24 characters hex string - uri: an URI as
   * parsed by Golang net/url.ParseRequestURI - email: an email address as parsed by Golang
   * net/mail.ParseAddress - hostname: a valid representation for an Internet host name, as defined
   * by RFC 1034, section 3.1 [RFC1034]. - ipv4: an IPv4 IP as parsed by Golang net.ParseIP - ipv6:
   * an IPv6 IP as parsed by Golang net.ParseIP - cidr: a CIDR as parsed by Golang net.ParseCIDR -
   * mac: a MAC address as parsed by Golang net.ParseMAC - uuid: an UUID that allows uppercase
   * defined by the regex (?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$ -
   * uuid3: an UUID3 that allows uppercase defined by the regex
   * (?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?3[0-9a-f]{3}-?[0-9a-f]{4}-?[0-9a-f]{12}$ - uuid4: an UUID4 that
   * allows uppercase defined by the regex
   * (?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?4[0-9a-f]{3}-?[89ab][0-9a-f]{3}-?[0-9a-f]{12}$ - uuid5: an UUID5
   * that allows uppercase defined by the regex
   * (?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?5[0-9a-f]{3}-?[89ab][0-9a-f]{3}-?[0-9a-f]{12}$ - isbn: an ISBN10
   * or ISBN13 number string like \&quot;0321751043\&quot; or \&quot;978-0321751041\&quot; - isbn10:
   * an ISBN10 number string like \&quot;0321751043\&quot; - isbn13: an ISBN13 number string like
   * \&quot;978-0321751041\&quot; - creditcard: a credit card number defined by the regex
   * ^(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11})$
   * with any non digit characters mixed in - ssn: a U.S. social security number following the regex
   * ^\\d{3}[- ]?\\d{2}[- ]?\\d{4}$ - hexcolor: an hexadecimal color code like \&quot;#FFFFFF:
   * following the regex ^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$ - rgbcolor: an RGB color code like rgb
   * like \&quot;rgb(255,255,2559\&quot; - byte: base64 encoded binary data - password: any kind of
   * string - date: a date string like \&quot;2006-01-02\&quot; as defined by full-date in RFC3339 -
   * duration: a duration string like \&quot;22 ns\&quot; as parsed by Golang time.ParseDuration or
   * compatible with Scala duration format - datetime: a date time string like
   * \&quot;2014-12-15T19:30:20.000Z\&quot; as defined by date-time in RFC3339.
   *
   * @return format
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "format is an OpenAPI v3 format string. Unknown formats are ignored. The following formats are validated:  - bsonobjectid: a bson object ID, i.e. a 24 characters hex string - uri: an URI as parsed by Golang net/url.ParseRequestURI - email: an email address as parsed by Golang net/mail.ParseAddress - hostname: a valid representation for an Internet host name, as defined by RFC 1034, section 3.1 [RFC1034]. - ipv4: an IPv4 IP as parsed by Golang net.ParseIP - ipv6: an IPv6 IP as parsed by Golang net.ParseIP - cidr: a CIDR as parsed by Golang net.ParseCIDR - mac: a MAC address as parsed by Golang net.ParseMAC - uuid: an UUID that allows uppercase defined by the regex (?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$ - uuid3: an UUID3 that allows uppercase defined by the regex (?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?3[0-9a-f]{3}-?[0-9a-f]{4}-?[0-9a-f]{12}$ - uuid4: an UUID4 that allows uppercase defined by the regex (?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?4[0-9a-f]{3}-?[89ab][0-9a-f]{3}-?[0-9a-f]{12}$ - uuid5: an UUID5 that allows uppercase defined by the regex (?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?5[0-9a-f]{3}-?[89ab][0-9a-f]{3}-?[0-9a-f]{12}$ - isbn: an ISBN10 or ISBN13 number string like '0321751043' or '978-0321751041' - isbn10: an ISBN10 number string like '0321751043' - isbn13: an ISBN13 number string like '978-0321751041' - creditcard: a credit card number defined by the regex ^(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11})$ with any non digit characters mixed in - ssn: a U.S. social security number following the regex ^\\d{3}[- ]?\\d{2}[- ]?\\d{4}$ - hexcolor: an hexadecimal color code like '#FFFFFF: following the regex ^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$ - rgbcolor: an RGB color code like rgb like 'rgb(255,255,2559' - byte: base64 encoded binary data - password: any kind of string - date: a date string like '2006-01-02' as defined by full-date in RFC3339 - duration: a duration string like '22 ns' as parsed by Golang time.ParseDuration or compatible with Scala duration format - datetime: a date time string like '2014-12-15T19:30:20.000Z' as defined by date-time in RFC3339.")
  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public V1beta1JSONSchemaProps id(String id) {

    this.id = id;
    return this;
  }

  /**
   * Get id
   *
   * @return id
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public V1beta1JSONSchemaProps items(Object items) {

    this.items = items;
    return this;
  }

  /**
   * JSONSchemaPropsOrArray represents a value that can either be a JSONSchemaProps or an array of
   * JSONSchemaProps. Mainly here for serialization purposes.
   *
   * @return items
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "JSONSchemaPropsOrArray represents a value that can either be a JSONSchemaProps or an array of JSONSchemaProps. Mainly here for serialization purposes.")
  public Object getItems() {
    return items;
  }

  public void setItems(Object items) {
    this.items = items;
  }

  public V1beta1JSONSchemaProps maxItems(Long maxItems) {

    this.maxItems = maxItems;
    return this;
  }

  /**
   * Get maxItems
   *
   * @return maxItems
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Long getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(Long maxItems) {
    this.maxItems = maxItems;
  }

  public V1beta1JSONSchemaProps maxLength(Long maxLength) {

    this.maxLength = maxLength;
    return this;
  }

  /**
   * Get maxLength
   *
   * @return maxLength
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Long getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(Long maxLength) {
    this.maxLength = maxLength;
  }

  public V1beta1JSONSchemaProps maxProperties(Long maxProperties) {

    this.maxProperties = maxProperties;
    return this;
  }

  /**
   * Get maxProperties
   *
   * @return maxProperties
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Long getMaxProperties() {
    return maxProperties;
  }

  public void setMaxProperties(Long maxProperties) {
    this.maxProperties = maxProperties;
  }

  public V1beta1JSONSchemaProps maximum(Double maximum) {

    this.maximum = maximum;
    return this;
  }

  /**
   * Get maximum
   *
   * @return maximum
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Double getMaximum() {
    return maximum;
  }

  public void setMaximum(Double maximum) {
    this.maximum = maximum;
  }

  public V1beta1JSONSchemaProps minItems(Long minItems) {

    this.minItems = minItems;
    return this;
  }

  /**
   * Get minItems
   *
   * @return minItems
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Long getMinItems() {
    return minItems;
  }

  public void setMinItems(Long minItems) {
    this.minItems = minItems;
  }

  public V1beta1JSONSchemaProps minLength(Long minLength) {

    this.minLength = minLength;
    return this;
  }

  /**
   * Get minLength
   *
   * @return minLength
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Long getMinLength() {
    return minLength;
  }

  public void setMinLength(Long minLength) {
    this.minLength = minLength;
  }

  public V1beta1JSONSchemaProps minProperties(Long minProperties) {

    this.minProperties = minProperties;
    return this;
  }

  /**
   * Get minProperties
   *
   * @return minProperties
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Long getMinProperties() {
    return minProperties;
  }

  public void setMinProperties(Long minProperties) {
    this.minProperties = minProperties;
  }

  public V1beta1JSONSchemaProps minimum(Double minimum) {

    this.minimum = minimum;
    return this;
  }

  /**
   * Get minimum
   *
   * @return minimum
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Double getMinimum() {
    return minimum;
  }

  public void setMinimum(Double minimum) {
    this.minimum = minimum;
  }

  public V1beta1JSONSchemaProps multipleOf(Double multipleOf) {

    this.multipleOf = multipleOf;
    return this;
  }

  /**
   * Get multipleOf
   *
   * @return multipleOf
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Double getMultipleOf() {
    return multipleOf;
  }

  public void setMultipleOf(Double multipleOf) {
    this.multipleOf = multipleOf;
  }

  public V1beta1JSONSchemaProps not(V1beta1JSONSchemaProps not) {

    this.not = not;
    return this;
  }

  /**
   * Get not
   *
   * @return not
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1beta1JSONSchemaProps getNot() {
    return not;
  }

  public void setNot(V1beta1JSONSchemaProps not) {
    this.not = not;
  }

  public V1beta1JSONSchemaProps nullable(Boolean nullable) {

    this.nullable = nullable;
    return this;
  }

  /**
   * Get nullable
   *
   * @return nullable
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Boolean getNullable() {
    return nullable;
  }

  public void setNullable(Boolean nullable) {
    this.nullable = nullable;
  }

  public V1beta1JSONSchemaProps oneOf(List<V1beta1JSONSchemaProps> oneOf) {

    this.oneOf = oneOf;
    return this;
  }

  public V1beta1JSONSchemaProps addOneOfItem(V1beta1JSONSchemaProps oneOfItem) {
    if (this.oneOf == null) {
      this.oneOf = new ArrayList<>();
    }
    this.oneOf.add(oneOfItem);
    return this;
  }

  /**
   * Get oneOf
   *
   * @return oneOf
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public List<V1beta1JSONSchemaProps> getOneOf() {
    return oneOf;
  }

  public void setOneOf(List<V1beta1JSONSchemaProps> oneOf) {
    this.oneOf = oneOf;
  }

  public V1beta1JSONSchemaProps pattern(String pattern) {

    this.pattern = pattern;
    return this;
  }

  /**
   * Get pattern
   *
   * @return pattern
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public V1beta1JSONSchemaProps patternProperties(
      Map<String, V1beta1JSONSchemaProps> patternProperties) {

    this.patternProperties = patternProperties;
    return this;
  }

  public V1beta1JSONSchemaProps putPatternPropertiesItem(
      String key, V1beta1JSONSchemaProps patternPropertiesItem) {
    if (this.patternProperties == null) {
      this.patternProperties = new HashMap<>();
    }
    this.patternProperties.put(key, patternPropertiesItem);
    return this;
  }

  /**
   * Get patternProperties
   *
   * @return patternProperties
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Map<String, V1beta1JSONSchemaProps> getPatternProperties() {
    return patternProperties;
  }

  public void setPatternProperties(Map<String, V1beta1JSONSchemaProps> patternProperties) {
    this.patternProperties = patternProperties;
  }

  public V1beta1JSONSchemaProps properties(Map<String, V1beta1JSONSchemaProps> properties) {

    this.properties = properties;
    return this;
  }

  public V1beta1JSONSchemaProps putPropertiesItem(
      String key, V1beta1JSONSchemaProps propertiesItem) {
    if (this.properties == null) {
      this.properties = new HashMap<>();
    }
    this.properties.put(key, propertiesItem);
    return this;
  }

  /**
   * Get properties
   *
   * @return properties
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Map<String, V1beta1JSONSchemaProps> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, V1beta1JSONSchemaProps> properties) {
    this.properties = properties;
  }

  public V1beta1JSONSchemaProps required(List<String> required) {

    this.required = required;
    return this;
  }

  public V1beta1JSONSchemaProps addRequiredItem(String requiredItem) {
    if (this.required == null) {
      this.required = new ArrayList<>();
    }
    this.required.add(requiredItem);
    return this;
  }

  /**
   * Get required
   *
   * @return required
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public List<String> getRequired() {
    return required;
  }

  public void setRequired(List<String> required) {
    this.required = required;
  }

  public V1beta1JSONSchemaProps title(String title) {

    this.title = title;
    return this;
  }

  /**
   * Get title
   *
   * @return title
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public V1beta1JSONSchemaProps type(String type) {

    this.type = type;
    return this;
  }

  /**
   * Get type
   *
   * @return type
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public V1beta1JSONSchemaProps uniqueItems(Boolean uniqueItems) {

    this.uniqueItems = uniqueItems;
    return this;
  }

  /**
   * Get uniqueItems
   *
   * @return uniqueItems
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public Boolean getUniqueItems() {
    return uniqueItems;
  }

  public void setUniqueItems(Boolean uniqueItems) {
    this.uniqueItems = uniqueItems;
  }

  public V1beta1JSONSchemaProps xKubernetesEmbeddedResource(Boolean xKubernetesEmbeddedResource) {

    this.xKubernetesEmbeddedResource = xKubernetesEmbeddedResource;
    return this;
  }

  /**
   * x-kubernetes-embedded-resource defines that the value is an embedded Kubernetes runtime.Object,
   * with TypeMeta and ObjectMeta. The type must be object. It is allowed to further restrict the
   * embedded object. kind, apiVersion and metadata are validated automatically.
   * x-kubernetes-preserve-unknown-fields is allowed to be true, but does not have to be if the
   * object is fully specified (up to kind, apiVersion, metadata).
   *
   * @return xKubernetesEmbeddedResource
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "x-kubernetes-embedded-resource defines that the value is an embedded Kubernetes runtime.Object, with TypeMeta and ObjectMeta. The type must be object. It is allowed to further restrict the embedded object. kind, apiVersion and metadata are validated automatically. x-kubernetes-preserve-unknown-fields is allowed to be true, but does not have to be if the object is fully specified (up to kind, apiVersion, metadata).")
  public Boolean getxKubernetesEmbeddedResource() {
    return xKubernetesEmbeddedResource;
  }

  public void setxKubernetesEmbeddedResource(Boolean xKubernetesEmbeddedResource) {
    this.xKubernetesEmbeddedResource = xKubernetesEmbeddedResource;
  }

  public V1beta1JSONSchemaProps xKubernetesIntOrString(Boolean xKubernetesIntOrString) {

    this.xKubernetesIntOrString = xKubernetesIntOrString;
    return this;
  }

  /**
   * x-kubernetes-int-or-string specifies that this value is either an integer or a string. If this
   * is true, an empty type is allowed and type as child of anyOf is permitted if following one of
   * the following patterns: 1) anyOf: - type: integer - type: string 2) allOf: - anyOf: - type:
   * integer - type: string - ... zero or more
   *
   * @return xKubernetesIntOrString
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "x-kubernetes-int-or-string specifies that this value is either an integer or a string. If this is true, an empty type is allowed and type as child of anyOf is permitted if following one of the following patterns:  1) anyOf:    - type: integer    - type: string 2) allOf:    - anyOf:      - type: integer      - type: string    - ... zero or more")
  public Boolean getxKubernetesIntOrString() {
    return xKubernetesIntOrString;
  }

  public void setxKubernetesIntOrString(Boolean xKubernetesIntOrString) {
    this.xKubernetesIntOrString = xKubernetesIntOrString;
  }

  public V1beta1JSONSchemaProps xKubernetesListMapKeys(List<String> xKubernetesListMapKeys) {

    this.xKubernetesListMapKeys = xKubernetesListMapKeys;
    return this;
  }

  public V1beta1JSONSchemaProps addXKubernetesListMapKeysItem(String xKubernetesListMapKeysItem) {
    if (this.xKubernetesListMapKeys == null) {
      this.xKubernetesListMapKeys = new ArrayList<>();
    }
    this.xKubernetesListMapKeys.add(xKubernetesListMapKeysItem);
    return this;
  }

  /**
   * x-kubernetes-list-map-keys annotates an array with the x-kubernetes-list-type &#x60;map&#x60;
   * by specifying the keys used as the index of the map. This tag MUST only be used on lists that
   * have the \&quot;x-kubernetes-list-type\&quot; extension set to \&quot;map\&quot;. Also, the
   * values specified for this attribute must be a scalar typed field of the child structure (no
   * nesting is supported). The properties specified must either be required or have a default
   * value, to ensure those properties are present for all list items.
   *
   * @return xKubernetesListMapKeys
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "x-kubernetes-list-map-keys annotates an array with the x-kubernetes-list-type `map` by specifying the keys used as the index of the map.  This tag MUST only be used on lists that have the 'x-kubernetes-list-type' extension set to 'map'. Also, the values specified for this attribute must be a scalar typed field of the child structure (no nesting is supported).  The properties specified must either be required or have a default value, to ensure those properties are present for all list items.")
  public List<String> getxKubernetesListMapKeys() {
    return xKubernetesListMapKeys;
  }

  public void setxKubernetesListMapKeys(List<String> xKubernetesListMapKeys) {
    this.xKubernetesListMapKeys = xKubernetesListMapKeys;
  }

  public V1beta1JSONSchemaProps xKubernetesListType(String xKubernetesListType) {

    this.xKubernetesListType = xKubernetesListType;
    return this;
  }

  /**
   * x-kubernetes-list-type annotates an array to further describe its topology. This extension must
   * only be used on lists and may have 3 possible values: 1) &#x60;atomic&#x60;: the list is
   * treated as a single entity, like a scalar. Atomic lists will be entirely replaced when updated.
   * This extension may be used on any type of list (struct, scalar, ...). 2) &#x60;set&#x60;: Sets
   * are lists that must not have multiple items with the same value. Each value must be a scalar,
   * an object with x-kubernetes-map-type &#x60;atomic&#x60; or an array with x-kubernetes-list-type
   * &#x60;atomic&#x60;. 3) &#x60;map&#x60;: These lists are like maps in that their elements have a
   * non-index key used to identify them. Order is preserved upon merge. The map tag must only be
   * used on a list with elements of type object. Defaults to atomic for arrays.
   *
   * @return xKubernetesListType
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "x-kubernetes-list-type annotates an array to further describe its topology. This extension must only be used on lists and may have 3 possible values:  1) `atomic`: the list is treated as a single entity, like a scalar.      Atomic lists will be entirely replaced when updated. This extension      may be used on any type of list (struct, scalar, ...). 2) `set`:      Sets are lists that must not have multiple items with the same value. Each      value must be a scalar, an object with x-kubernetes-map-type `atomic` or an      array with x-kubernetes-list-type `atomic`. 3) `map`:      These lists are like maps in that their elements have a non-index key      used to identify them. Order is preserved upon merge. The map tag      must only be used on a list with elements of type object. Defaults to atomic for arrays.")
  public String getxKubernetesListType() {
    return xKubernetesListType;
  }

  public void setxKubernetesListType(String xKubernetesListType) {
    this.xKubernetesListType = xKubernetesListType;
  }

  public V1beta1JSONSchemaProps xKubernetesMapType(String xKubernetesMapType) {

    this.xKubernetesMapType = xKubernetesMapType;
    return this;
  }

  /**
   * x-kubernetes-map-type annotates an object to further describe its topology. This extension must
   * only be used when type is object and may have 2 possible values: 1) &#x60;granular&#x60;: These
   * maps are actual maps (key-value pairs) and each fields are independent from each other (they
   * can each be manipulated by separate actors). This is the default behaviour for all maps. 2)
   * &#x60;atomic&#x60;: the list is treated as a single entity, like a scalar. Atomic maps will be
   * entirely replaced when updated.
   *
   * @return xKubernetesMapType
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "x-kubernetes-map-type annotates an object to further describe its topology. This extension must only be used when type is object and may have 2 possible values:  1) `granular`:      These maps are actual maps (key-value pairs) and each fields are independent      from each other (they can each be manipulated by separate actors). This is      the default behaviour for all maps. 2) `atomic`: the list is treated as a single entity, like a scalar.      Atomic maps will be entirely replaced when updated.")
  public String getxKubernetesMapType() {
    return xKubernetesMapType;
  }

  public void setxKubernetesMapType(String xKubernetesMapType) {
    this.xKubernetesMapType = xKubernetesMapType;
  }

  public V1beta1JSONSchemaProps xKubernetesPreserveUnknownFields(
      Boolean xKubernetesPreserveUnknownFields) {

    this.xKubernetesPreserveUnknownFields = xKubernetesPreserveUnknownFields;
    return this;
  }

  /**
   * x-kubernetes-preserve-unknown-fields stops the API server decoding step from pruning fields
   * which are not specified in the validation schema. This affects fields recursively, but switches
   * back to normal pruning behaviour if nested properties or additionalProperties are specified in
   * the schema. This can either be true or undefined. False is forbidden.
   *
   * @return xKubernetesPreserveUnknownFields
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "x-kubernetes-preserve-unknown-fields stops the API server decoding step from pruning fields which are not specified in the validation schema. This affects fields recursively, but switches back to normal pruning behaviour if nested properties or additionalProperties are specified in the schema. This can either be true or undefined. False is forbidden.")
  public Boolean getxKubernetesPreserveUnknownFields() {
    return xKubernetesPreserveUnknownFields;
  }

  public void setxKubernetesPreserveUnknownFields(Boolean xKubernetesPreserveUnknownFields) {
    this.xKubernetesPreserveUnknownFields = xKubernetesPreserveUnknownFields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta1JSONSchemaProps v1beta1JSONSchemaProps = (V1beta1JSONSchemaProps) o;
    return Objects.equals(this.$ref, v1beta1JSONSchemaProps.$ref)
        && Objects.equals(this.$schema, v1beta1JSONSchemaProps.$schema)
        && Objects.equals(this.additionalItems, v1beta1JSONSchemaProps.additionalItems)
        && Objects.equals(this.additionalProperties, v1beta1JSONSchemaProps.additionalProperties)
        && Objects.equals(this.allOf, v1beta1JSONSchemaProps.allOf)
        && Objects.equals(this.anyOf, v1beta1JSONSchemaProps.anyOf)
        && Objects.equals(this._default, v1beta1JSONSchemaProps._default)
        && Objects.equals(this.definitions, v1beta1JSONSchemaProps.definitions)
        && Objects.equals(this.dependencies, v1beta1JSONSchemaProps.dependencies)
        && Objects.equals(this.description, v1beta1JSONSchemaProps.description)
        && Objects.equals(this._enum, v1beta1JSONSchemaProps._enum)
        && Objects.equals(this.example, v1beta1JSONSchemaProps.example)
        && Objects.equals(this.exclusiveMaximum, v1beta1JSONSchemaProps.exclusiveMaximum)
        && Objects.equals(this.exclusiveMinimum, v1beta1JSONSchemaProps.exclusiveMinimum)
        && Objects.equals(this.externalDocs, v1beta1JSONSchemaProps.externalDocs)
        && Objects.equals(this.format, v1beta1JSONSchemaProps.format)
        && Objects.equals(this.id, v1beta1JSONSchemaProps.id)
        && Objects.equals(this.items, v1beta1JSONSchemaProps.items)
        && Objects.equals(this.maxItems, v1beta1JSONSchemaProps.maxItems)
        && Objects.equals(this.maxLength, v1beta1JSONSchemaProps.maxLength)
        && Objects.equals(this.maxProperties, v1beta1JSONSchemaProps.maxProperties)
        && Objects.equals(this.maximum, v1beta1JSONSchemaProps.maximum)
        && Objects.equals(this.minItems, v1beta1JSONSchemaProps.minItems)
        && Objects.equals(this.minLength, v1beta1JSONSchemaProps.minLength)
        && Objects.equals(this.minProperties, v1beta1JSONSchemaProps.minProperties)
        && Objects.equals(this.minimum, v1beta1JSONSchemaProps.minimum)
        && Objects.equals(this.multipleOf, v1beta1JSONSchemaProps.multipleOf)
        && Objects.equals(this.not, v1beta1JSONSchemaProps.not)
        && Objects.equals(this.nullable, v1beta1JSONSchemaProps.nullable)
        && Objects.equals(this.oneOf, v1beta1JSONSchemaProps.oneOf)
        && Objects.equals(this.pattern, v1beta1JSONSchemaProps.pattern)
        && Objects.equals(this.patternProperties, v1beta1JSONSchemaProps.patternProperties)
        && Objects.equals(this.properties, v1beta1JSONSchemaProps.properties)
        && Objects.equals(this.required, v1beta1JSONSchemaProps.required)
        && Objects.equals(this.title, v1beta1JSONSchemaProps.title)
        && Objects.equals(this.type, v1beta1JSONSchemaProps.type)
        && Objects.equals(this.uniqueItems, v1beta1JSONSchemaProps.uniqueItems)
        && Objects.equals(
            this.xKubernetesEmbeddedResource, v1beta1JSONSchemaProps.xKubernetesEmbeddedResource)
        && Objects.equals(
            this.xKubernetesIntOrString, v1beta1JSONSchemaProps.xKubernetesIntOrString)
        && Objects.equals(
            this.xKubernetesListMapKeys, v1beta1JSONSchemaProps.xKubernetesListMapKeys)
        && Objects.equals(this.xKubernetesListType, v1beta1JSONSchemaProps.xKubernetesListType)
        && Objects.equals(this.xKubernetesMapType, v1beta1JSONSchemaProps.xKubernetesMapType)
        && Objects.equals(
            this.xKubernetesPreserveUnknownFields,
            v1beta1JSONSchemaProps.xKubernetesPreserveUnknownFields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        $ref,
        $schema,
        additionalItems,
        additionalProperties,
        allOf,
        anyOf,
        _default,
        definitions,
        dependencies,
        description,
        _enum,
        example,
        exclusiveMaximum,
        exclusiveMinimum,
        externalDocs,
        format,
        id,
        items,
        maxItems,
        maxLength,
        maxProperties,
        maximum,
        minItems,
        minLength,
        minProperties,
        minimum,
        multipleOf,
        not,
        nullable,
        oneOf,
        pattern,
        patternProperties,
        properties,
        required,
        title,
        type,
        uniqueItems,
        xKubernetesEmbeddedResource,
        xKubernetesIntOrString,
        xKubernetesListMapKeys,
        xKubernetesListType,
        xKubernetesMapType,
        xKubernetesPreserveUnknownFields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta1JSONSchemaProps {\n");
    sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
    sb.append("    $schema: ").append(toIndentedString($schema)).append("\n");
    sb.append("    additionalItems: ").append(toIndentedString(additionalItems)).append("\n");
    sb.append("    additionalProperties: ")
        .append(toIndentedString(additionalProperties))
        .append("\n");
    sb.append("    allOf: ").append(toIndentedString(allOf)).append("\n");
    sb.append("    anyOf: ").append(toIndentedString(anyOf)).append("\n");
    sb.append("    _default: ").append(toIndentedString(_default)).append("\n");
    sb.append("    definitions: ").append(toIndentedString(definitions)).append("\n");
    sb.append("    dependencies: ").append(toIndentedString(dependencies)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    _enum: ").append(toIndentedString(_enum)).append("\n");
    sb.append("    example: ").append(toIndentedString(example)).append("\n");
    sb.append("    exclusiveMaximum: ").append(toIndentedString(exclusiveMaximum)).append("\n");
    sb.append("    exclusiveMinimum: ").append(toIndentedString(exclusiveMinimum)).append("\n");
    sb.append("    externalDocs: ").append(toIndentedString(externalDocs)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    maxItems: ").append(toIndentedString(maxItems)).append("\n");
    sb.append("    maxLength: ").append(toIndentedString(maxLength)).append("\n");
    sb.append("    maxProperties: ").append(toIndentedString(maxProperties)).append("\n");
    sb.append("    maximum: ").append(toIndentedString(maximum)).append("\n");
    sb.append("    minItems: ").append(toIndentedString(minItems)).append("\n");
    sb.append("    minLength: ").append(toIndentedString(minLength)).append("\n");
    sb.append("    minProperties: ").append(toIndentedString(minProperties)).append("\n");
    sb.append("    minimum: ").append(toIndentedString(minimum)).append("\n");
    sb.append("    multipleOf: ").append(toIndentedString(multipleOf)).append("\n");
    sb.append("    not: ").append(toIndentedString(not)).append("\n");
    sb.append("    nullable: ").append(toIndentedString(nullable)).append("\n");
    sb.append("    oneOf: ").append(toIndentedString(oneOf)).append("\n");
    sb.append("    pattern: ").append(toIndentedString(pattern)).append("\n");
    sb.append("    patternProperties: ").append(toIndentedString(patternProperties)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    uniqueItems: ").append(toIndentedString(uniqueItems)).append("\n");
    sb.append("    xKubernetesEmbeddedResource: ")
        .append(toIndentedString(xKubernetesEmbeddedResource))
        .append("\n");
    sb.append("    xKubernetesIntOrString: ")
        .append(toIndentedString(xKubernetesIntOrString))
        .append("\n");
    sb.append("    xKubernetesListMapKeys: ")
        .append(toIndentedString(xKubernetesListMapKeys))
        .append("\n");
    sb.append("    xKubernetesListType: ")
        .append(toIndentedString(xKubernetesListType))
        .append("\n");
    sb.append("    xKubernetesMapType: ").append(toIndentedString(xKubernetesMapType)).append("\n");
    sb.append("    xKubernetesPreserveUnknownFields: ")
        .append(toIndentedString(xKubernetesPreserveUnknownFields))
        .append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
