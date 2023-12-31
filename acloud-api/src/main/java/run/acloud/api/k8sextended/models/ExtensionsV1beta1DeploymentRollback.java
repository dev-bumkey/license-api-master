/*
Copyright 2020 The Kubernetes Authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** DEPRECATED. DeploymentRollback stores the information required to rollback a deployment. */
@Schema(
    description =
        "DEPRECATED. DeploymentRollback stores the information required to rollback a deployment.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class ExtensionsV1beta1DeploymentRollback {
  public static final String SERIALIZED_NAME_API_VERSION = "apiVersion";

  @SerializedName(SERIALIZED_NAME_API_VERSION)
  private String apiVersion;

  public static final String SERIALIZED_NAME_KIND = "kind";

  @SerializedName(SERIALIZED_NAME_KIND)
  private String kind;

  public static final String SERIALIZED_NAME_NAME = "name";

  @SerializedName(SERIALIZED_NAME_NAME)
  private String name;

  public static final String SERIALIZED_NAME_ROLLBACK_TO = "rollbackTo";

  @SerializedName(SERIALIZED_NAME_ROLLBACK_TO)
  private ExtensionsV1beta1RollbackConfig rollbackTo;

  public static final String SERIALIZED_NAME_UPDATED_ANNOTATIONS = "updatedAnnotations";

  @SerializedName(SERIALIZED_NAME_UPDATED_ANNOTATIONS)
  private Map<String, String> updatedAnnotations = null;

  public ExtensionsV1beta1DeploymentRollback apiVersion(String apiVersion) {

    this.apiVersion = apiVersion;
    return this;
  }

  /**
   * APIVersion defines the versioned schema of this representation of an object. Servers should
   * convert recognized schemas to the latest internal value, and may reject unrecognized values.
   * More info:
   * https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources
   *
   * @return apiVersion
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest internal value, and may reject unrecognized values. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources")
  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public ExtensionsV1beta1DeploymentRollback kind(String kind) {

    this.kind = kind;
    return this;
  }

  /**
   * Kind is a string value representing the REST resource this object represents. Servers may infer
   * this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More
   * info:
   * https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds
   *
   * @return kind
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds")
  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public ExtensionsV1beta1DeploymentRollback name(String name) {

    this.name = name;
    return this;
  }

  /**
   * Required: This must match the Name of a deployment.
   *
   * @return name
   */
  @Schema(required = true, description = "Required: This must match the Name of a deployment.")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ExtensionsV1beta1DeploymentRollback rollbackTo(
      ExtensionsV1beta1RollbackConfig rollbackTo) {

    this.rollbackTo = rollbackTo;
    return this;
  }

  /**
   * Get rollbackTo
   *
   * @return rollbackTo
   */
  @Schema(required = true, description = "")
  public ExtensionsV1beta1RollbackConfig getRollbackTo() {
    return rollbackTo;
  }

  public void setRollbackTo(ExtensionsV1beta1RollbackConfig rollbackTo) {
    this.rollbackTo = rollbackTo;
  }

  public ExtensionsV1beta1DeploymentRollback updatedAnnotations(
      Map<String, String> updatedAnnotations) {

    this.updatedAnnotations = updatedAnnotations;
    return this;
  }

  public ExtensionsV1beta1DeploymentRollback putUpdatedAnnotationsItem(
      String key, String updatedAnnotationsItem) {
    if (this.updatedAnnotations == null) {
      this.updatedAnnotations = new HashMap<String, String>();
    }
    this.updatedAnnotations.put(key, updatedAnnotationsItem);
    return this;
  }

  /**
   * The annotations to be updated to a deployment
   *
   * @return updatedAnnotations
   */
  @jakarta.annotation.Nullable
  @Schema(description = "The annotations to be updated to a deployment")
  public Map<String, String> getUpdatedAnnotations() {
    return updatedAnnotations;
  }

  public void setUpdatedAnnotations(Map<String, String> updatedAnnotations) {
    this.updatedAnnotations = updatedAnnotations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExtensionsV1beta1DeploymentRollback extensionsV1beta1DeploymentRollback =
        (ExtensionsV1beta1DeploymentRollback) o;
    return Objects.equals(this.apiVersion, extensionsV1beta1DeploymentRollback.apiVersion)
        && Objects.equals(this.kind, extensionsV1beta1DeploymentRollback.kind)
        && Objects.equals(this.name, extensionsV1beta1DeploymentRollback.name)
        && Objects.equals(this.rollbackTo, extensionsV1beta1DeploymentRollback.rollbackTo)
        && Objects.equals(
            this.updatedAnnotations, extensionsV1beta1DeploymentRollback.updatedAnnotations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiVersion, kind, name, rollbackTo, updatedAnnotations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExtensionsV1beta1DeploymentRollback {\n");
    sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    rollbackTo: ").append(toIndentedString(rollbackTo)).append("\n");
    sb.append("    updatedAnnotations: ").append(toIndentedString(updatedAnnotations)).append("\n");
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
