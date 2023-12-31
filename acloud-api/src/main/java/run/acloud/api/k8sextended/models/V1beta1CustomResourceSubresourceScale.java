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

import java.util.Objects;

/**
 * CustomResourceSubresourceScale defines how to serve the scale subresource for CustomResources.
 */
@Schema(
    description =
        "CustomResourceSubresourceScale defines how to serve the scale subresource for CustomResources.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-01-04T09:55:14.976Z[Etc/UTC]")
public class V1beta1CustomResourceSubresourceScale {
  public static final String SERIALIZED_NAME_LABEL_SELECTOR_PATH = "labelSelectorPath";

  @SerializedName(SERIALIZED_NAME_LABEL_SELECTOR_PATH)
  private String labelSelectorPath;

  public static final String SERIALIZED_NAME_SPEC_REPLICAS_PATH = "specReplicasPath";

  @SerializedName(SERIALIZED_NAME_SPEC_REPLICAS_PATH)
  private String specReplicasPath;

  public static final String SERIALIZED_NAME_STATUS_REPLICAS_PATH = "statusReplicasPath";

  @SerializedName(SERIALIZED_NAME_STATUS_REPLICAS_PATH)
  private String statusReplicasPath;

  public V1beta1CustomResourceSubresourceScale labelSelectorPath(String labelSelectorPath) {

    this.labelSelectorPath = labelSelectorPath;
    return this;
  }

  /**
   * labelSelectorPath defines the JSON path inside of a custom resource that corresponds to Scale
   * &#x60;status.selector&#x60;. Only JSON paths without the array notation are allowed. Must be a
   * JSON Path under &#x60;.status&#x60; or &#x60;.spec&#x60;. Must be set to work with
   * HorizontalPodAutoscaler. The field pointed by this JSON path must be a string field (not a
   * complex selector struct) which contains a serialized label selector in string form. More info:
   * https://kubernetes.io/docs/tasks/access-kubernetes-api/custom-resources/custom-resource-definitions#scale-subresource
   * If there is no value under the given path in the custom resource, the
   * &#x60;status.selector&#x60; value in the &#x60;/scale&#x60; subresource will default to the
   * empty string.
   *
   * @return labelSelectorPath
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "labelSelectorPath defines the JSON path inside of a custom resource that corresponds to Scale `status.selector`. Only JSON paths without the array notation are allowed. Must be a JSON Path under `.status` or `.spec`. Must be set to work with HorizontalPodAutoscaler. The field pointed by this JSON path must be a string field (not a complex selector struct) which contains a serialized label selector in string form. More info: https://kubernetes.io/docs/tasks/access-kubernetes-api/custom-resources/custom-resource-definitions#scale-subresource If there is no value under the given path in the custom resource, the `status.selector` value in the `/scale` subresource will default to the empty string.")
  public String getLabelSelectorPath() {
    return labelSelectorPath;
  }

  public void setLabelSelectorPath(String labelSelectorPath) {
    this.labelSelectorPath = labelSelectorPath;
  }

  public V1beta1CustomResourceSubresourceScale specReplicasPath(String specReplicasPath) {

    this.specReplicasPath = specReplicasPath;
    return this;
  }

  /**
   * specReplicasPath defines the JSON path inside of a custom resource that corresponds to Scale
   * &#x60;spec.replicas&#x60;. Only JSON paths without the array notation are allowed. Must be a
   * JSON Path under &#x60;.spec&#x60;. If there is no value under the given path in the custom
   * resource, the &#x60;/scale&#x60; subresource will return an error on GET.
   *
   * @return specReplicasPath
   */
  @Schema(
      required = true,
      description =
          "specReplicasPath defines the JSON path inside of a custom resource that corresponds to Scale `spec.replicas`. Only JSON paths without the array notation are allowed. Must be a JSON Path under `.spec`. If there is no value under the given path in the custom resource, the `/scale` subresource will return an error on GET.")
  public String getSpecReplicasPath() {
    return specReplicasPath;
  }

  public void setSpecReplicasPath(String specReplicasPath) {
    this.specReplicasPath = specReplicasPath;
  }

  public V1beta1CustomResourceSubresourceScale statusReplicasPath(String statusReplicasPath) {

    this.statusReplicasPath = statusReplicasPath;
    return this;
  }

  /**
   * statusReplicasPath defines the JSON path inside of a custom resource that corresponds to Scale
   * &#x60;status.replicas&#x60;. Only JSON paths without the array notation are allowed. Must be a
   * JSON Path under &#x60;.status&#x60;. If there is no value under the given path in the custom
   * resource, the &#x60;status.replicas&#x60; value in the &#x60;/scale&#x60; subresource will
   * default to 0.
   *
   * @return statusReplicasPath
   */
  @Schema(
      required = true,
      description =
          "statusReplicasPath defines the JSON path inside of a custom resource that corresponds to Scale `status.replicas`. Only JSON paths without the array notation are allowed. Must be a JSON Path under `.status`. If there is no value under the given path in the custom resource, the `status.replicas` value in the `/scale` subresource will default to 0.")
  public String getStatusReplicasPath() {
    return statusReplicasPath;
  }

  public void setStatusReplicasPath(String statusReplicasPath) {
    this.statusReplicasPath = statusReplicasPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta1CustomResourceSubresourceScale v1beta1CustomResourceSubresourceScale =
        (V1beta1CustomResourceSubresourceScale) o;
    return Objects.equals(
            this.labelSelectorPath, v1beta1CustomResourceSubresourceScale.labelSelectorPath)
        && Objects.equals(
            this.specReplicasPath, v1beta1CustomResourceSubresourceScale.specReplicasPath)
        && Objects.equals(
            this.statusReplicasPath, v1beta1CustomResourceSubresourceScale.statusReplicasPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(labelSelectorPath, specReplicasPath, statusReplicasPath);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta1CustomResourceSubresourceScale {\n");
    sb.append("    labelSelectorPath: ").append(toIndentedString(labelSelectorPath)).append("\n");
    sb.append("    specReplicasPath: ").append(toIndentedString(specReplicasPath)).append("\n");
    sb.append("    statusReplicasPath: ").append(toIndentedString(statusReplicasPath)).append("\n");
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
