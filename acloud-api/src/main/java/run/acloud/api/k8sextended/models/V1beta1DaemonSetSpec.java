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
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/** DaemonSetSpec is the specification of a daemon set. */
@Schema(description = "DaemonSetSpec is the specification of a daemon set.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta1DaemonSetSpec {
  public static final String SERIALIZED_NAME_MIN_READY_SECONDS = "minReadySeconds";

  @SerializedName(SERIALIZED_NAME_MIN_READY_SECONDS)
  private Integer minReadySeconds;

  public static final String SERIALIZED_NAME_REVISION_HISTORY_LIMIT = "revisionHistoryLimit";

  @SerializedName(SERIALIZED_NAME_REVISION_HISTORY_LIMIT)
  private Integer revisionHistoryLimit;

  public static final String SERIALIZED_NAME_SELECTOR = "selector";

  @SerializedName(SERIALIZED_NAME_SELECTOR)
  private V1LabelSelector selector;

  public static final String SERIALIZED_NAME_TEMPLATE = "template";

  @SerializedName(SERIALIZED_NAME_TEMPLATE)
  private V1PodTemplateSpec template;

  public static final String SERIALIZED_NAME_TEMPLATE_GENERATION = "templateGeneration";

  @SerializedName(SERIALIZED_NAME_TEMPLATE_GENERATION)
  private Long templateGeneration;

  public static final String SERIALIZED_NAME_UPDATE_STRATEGY = "updateStrategy";

  @SerializedName(SERIALIZED_NAME_UPDATE_STRATEGY)
  private V1beta1DaemonSetUpdateStrategy updateStrategy;

  public V1beta1DaemonSetSpec minReadySeconds(Integer minReadySeconds) {

    this.minReadySeconds = minReadySeconds;
    return this;
  }

  /**
   * The minimum number of seconds for which a newly created DaemonSet pod should be ready without
   * any of its container crashing, for it to be considered available. Defaults to 0 (pod will be
   * considered available as soon as it is ready).
   *
   * @return minReadySeconds
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "The minimum number of seconds for which a newly created DaemonSet pod should be ready without any of its container crashing, for it to be considered available. Defaults to 0 (pod will be considered available as soon as it is ready).")
  public Integer getMinReadySeconds() {
    return minReadySeconds;
  }

  public void setMinReadySeconds(Integer minReadySeconds) {
    this.minReadySeconds = minReadySeconds;
  }

  public V1beta1DaemonSetSpec revisionHistoryLimit(Integer revisionHistoryLimit) {

    this.revisionHistoryLimit = revisionHistoryLimit;
    return this;
  }

  /**
   * The number of old history to retain to allow rollback. This is a pointer to distinguish between
   * explicit zero and not specified. Defaults to 10.
   *
   * @return revisionHistoryLimit
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "The number of old history to retain to allow rollback. This is a pointer to distinguish between explicit zero and not specified. Defaults to 10.")
  public Integer getRevisionHistoryLimit() {
    return revisionHistoryLimit;
  }

  public void setRevisionHistoryLimit(Integer revisionHistoryLimit) {
    this.revisionHistoryLimit = revisionHistoryLimit;
  }

  public V1beta1DaemonSetSpec selector(V1LabelSelector selector) {

    this.selector = selector;
    return this;
  }

  /**
   * Get selector
   *
   * @return selector
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1LabelSelector getSelector() {
    return selector;
  }

  public void setSelector(V1LabelSelector selector) {
    this.selector = selector;
  }

  public V1beta1DaemonSetSpec template(V1PodTemplateSpec template) {

    this.template = template;
    return this;
  }

  /**
   * Get template
   *
   * @return template
   */
  @Schema(required = true, description = "")
  public V1PodTemplateSpec getTemplate() {
    return template;
  }

  public void setTemplate(V1PodTemplateSpec template) {
    this.template = template;
  }

  public V1beta1DaemonSetSpec templateGeneration(Long templateGeneration) {

    this.templateGeneration = templateGeneration;
    return this;
  }

  /**
   * DEPRECATED. A sequence number representing a specific generation of the template. Populated by
   * the system. It can be set only during the creation.
   *
   * @return templateGeneration
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "DEPRECATED. A sequence number representing a specific generation of the template. Populated by the system. It can be set only during the creation.")
  public Long getTemplateGeneration() {
    return templateGeneration;
  }

  public void setTemplateGeneration(Long templateGeneration) {
    this.templateGeneration = templateGeneration;
  }

  public V1beta1DaemonSetSpec updateStrategy(V1beta1DaemonSetUpdateStrategy updateStrategy) {

    this.updateStrategy = updateStrategy;
    return this;
  }

  /**
   * Get updateStrategy
   *
   * @return updateStrategy
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1beta1DaemonSetUpdateStrategy getUpdateStrategy() {
    return updateStrategy;
  }

  public void setUpdateStrategy(V1beta1DaemonSetUpdateStrategy updateStrategy) {
    this.updateStrategy = updateStrategy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta1DaemonSetSpec v1beta1DaemonSetSpec = (V1beta1DaemonSetSpec) o;
    return Objects.equals(this.minReadySeconds, v1beta1DaemonSetSpec.minReadySeconds)
        && Objects.equals(this.revisionHistoryLimit, v1beta1DaemonSetSpec.revisionHistoryLimit)
        && Objects.equals(this.selector, v1beta1DaemonSetSpec.selector)
        && Objects.equals(this.template, v1beta1DaemonSetSpec.template)
        && Objects.equals(this.templateGeneration, v1beta1DaemonSetSpec.templateGeneration)
        && Objects.equals(this.updateStrategy, v1beta1DaemonSetSpec.updateStrategy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        minReadySeconds,
        revisionHistoryLimit,
        selector,
        template,
        templateGeneration,
        updateStrategy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta1DaemonSetSpec {\n");
    sb.append("    minReadySeconds: ").append(toIndentedString(minReadySeconds)).append("\n");
    sb.append("    revisionHistoryLimit: ")
        .append(toIndentedString(revisionHistoryLimit))
        .append("\n");
    sb.append("    selector: ").append(toIndentedString(selector)).append("\n");
    sb.append("    template: ").append(toIndentedString(template)).append("\n");
    sb.append("    templateGeneration: ").append(toIndentedString(templateGeneration)).append("\n");
    sb.append("    updateStrategy: ").append(toIndentedString(updateStrategy)).append("\n");
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
