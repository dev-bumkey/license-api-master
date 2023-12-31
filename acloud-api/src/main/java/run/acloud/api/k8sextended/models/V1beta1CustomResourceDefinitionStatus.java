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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** CustomResourceDefinitionStatus indicates the state of the CustomResourceDefinition */
@Schema(
    description =
        "CustomResourceDefinitionStatus indicates the state of the CustomResourceDefinition")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-01-04T09:55:14.976Z[Etc/UTC]")
public class V1beta1CustomResourceDefinitionStatus {
  public static final String SERIALIZED_NAME_ACCEPTED_NAMES = "acceptedNames";

  @SerializedName(SERIALIZED_NAME_ACCEPTED_NAMES)
  private V1beta1CustomResourceDefinitionNames acceptedNames;

  public static final String SERIALIZED_NAME_CONDITIONS = "conditions";

  @SerializedName(SERIALIZED_NAME_CONDITIONS)
  private List<V1beta1CustomResourceDefinitionCondition> conditions = null;

  public static final String SERIALIZED_NAME_STORED_VERSIONS = "storedVersions";

  @SerializedName(SERIALIZED_NAME_STORED_VERSIONS)
  private List<String> storedVersions = null;

  public V1beta1CustomResourceDefinitionStatus acceptedNames(
      V1beta1CustomResourceDefinitionNames acceptedNames) {

    this.acceptedNames = acceptedNames;
    return this;
  }

  /**
   * Get acceptedNames
   *
   * @return acceptedNames
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1beta1CustomResourceDefinitionNames getAcceptedNames() {
    return acceptedNames;
  }

  public void setAcceptedNames(V1beta1CustomResourceDefinitionNames acceptedNames) {
    this.acceptedNames = acceptedNames;
  }

  public V1beta1CustomResourceDefinitionStatus conditions(
      List<V1beta1CustomResourceDefinitionCondition> conditions) {

    this.conditions = conditions;
    return this;
  }

  public V1beta1CustomResourceDefinitionStatus addConditionsItem(
      V1beta1CustomResourceDefinitionCondition conditionsItem) {
    if (this.conditions == null) {
      this.conditions = new ArrayList<>();
    }
    this.conditions.add(conditionsItem);
    return this;
  }

  /**
   * conditions indicate state for particular aspects of a CustomResourceDefinition
   *
   * @return conditions
   */
  @jakarta.annotation.Nullable
  @Schema(
      description = "conditions indicate state for particular aspects of a CustomResourceDefinition")
  public List<V1beta1CustomResourceDefinitionCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<V1beta1CustomResourceDefinitionCondition> conditions) {
    this.conditions = conditions;
  }

  public V1beta1CustomResourceDefinitionStatus storedVersions(List<String> storedVersions) {

    this.storedVersions = storedVersions;
    return this;
  }

  public V1beta1CustomResourceDefinitionStatus addStoredVersionsItem(String storedVersionsItem) {
    if (this.storedVersions == null) {
      this.storedVersions = new ArrayList<>();
    }
    this.storedVersions.add(storedVersionsItem);
    return this;
  }

  /**
   * storedVersions lists all versions of CustomResources that were ever persisted. Tracking these
   * versions allows a migration path for stored versions in etcd. The field is mutable so a
   * migration controller can finish a migration to another version (ensuring no old objects are
   * left in storage), and then remove the rest of the versions from this list. Versions may not be
   * removed from &#x60;spec.versions&#x60; while they exist in this list.
   *
   * @return storedVersions
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "storedVersions lists all versions of CustomResources that were ever persisted. Tracking these versions allows a migration path for stored versions in etcd. The field is mutable so a migration controller can finish a migration to another version (ensuring no old objects are left in storage), and then remove the rest of the versions from this list. Versions may not be removed from `spec.versions` while they exist in this list.")
  public List<String> getStoredVersions() {
    return storedVersions;
  }

  public void setStoredVersions(List<String> storedVersions) {
    this.storedVersions = storedVersions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta1CustomResourceDefinitionStatus v1beta1CustomResourceDefinitionStatus =
        (V1beta1CustomResourceDefinitionStatus) o;
    return Objects.equals(this.acceptedNames, v1beta1CustomResourceDefinitionStatus.acceptedNames)
        && Objects.equals(this.conditions, v1beta1CustomResourceDefinitionStatus.conditions)
        && Objects.equals(
            this.storedVersions, v1beta1CustomResourceDefinitionStatus.storedVersions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(acceptedNames, conditions, storedVersions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta1CustomResourceDefinitionStatus {\n");
    sb.append("    acceptedNames: ").append(toIndentedString(acceptedNames)).append("\n");
    sb.append("    conditions: ").append(toIndentedString(conditions)).append("\n");
    sb.append("    storedVersions: ").append(toIndentedString(storedVersions)).append("\n");
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
