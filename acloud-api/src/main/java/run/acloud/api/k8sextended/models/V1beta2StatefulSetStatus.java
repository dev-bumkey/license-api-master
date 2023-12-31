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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** StatefulSetStatus represents the current state of a StatefulSet. */
@Schema(description = "StatefulSetStatus represents the current state of a StatefulSet.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta2StatefulSetStatus {
  public static final String SERIALIZED_NAME_COLLISION_COUNT = "collisionCount";

  @SerializedName(SERIALIZED_NAME_COLLISION_COUNT)
  private Integer collisionCount;

  public static final String SERIALIZED_NAME_CONDITIONS = "conditions";

  @SerializedName(SERIALIZED_NAME_CONDITIONS)
  private List<V1beta2StatefulSetCondition> conditions = null;

  public static final String SERIALIZED_NAME_CURRENT_REPLICAS = "currentReplicas";

  @SerializedName(SERIALIZED_NAME_CURRENT_REPLICAS)
  private Integer currentReplicas;

  public static final String SERIALIZED_NAME_CURRENT_REVISION = "currentRevision";

  @SerializedName(SERIALIZED_NAME_CURRENT_REVISION)
  private String currentRevision;

  public static final String SERIALIZED_NAME_OBSERVED_GENERATION = "observedGeneration";

  @SerializedName(SERIALIZED_NAME_OBSERVED_GENERATION)
  private Long observedGeneration;

  public static final String SERIALIZED_NAME_READY_REPLICAS = "readyReplicas";

  @SerializedName(SERIALIZED_NAME_READY_REPLICAS)
  private Integer readyReplicas;

  public static final String SERIALIZED_NAME_REPLICAS = "replicas";

  @SerializedName(SERIALIZED_NAME_REPLICAS)
  private Integer replicas;

  public static final String SERIALIZED_NAME_UPDATE_REVISION = "updateRevision";

  @SerializedName(SERIALIZED_NAME_UPDATE_REVISION)
  private String updateRevision;

  public static final String SERIALIZED_NAME_UPDATED_REPLICAS = "updatedReplicas";

  @SerializedName(SERIALIZED_NAME_UPDATED_REPLICAS)
  private Integer updatedReplicas;

  public V1beta2StatefulSetStatus collisionCount(Integer collisionCount) {

    this.collisionCount = collisionCount;
    return this;
  }

  /**
   * collisionCount is the count of hash collisions for the StatefulSet. The StatefulSet controller
   * uses this field as a collision avoidance mechanism when it needs to create the name for the
   * newest ControllerRevision.
   *
   * @return collisionCount
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "collisionCount is the count of hash collisions for the StatefulSet. The StatefulSet controller uses this field as a collision avoidance mechanism when it needs to create the name for the newest ControllerRevision.")
  public Integer getCollisionCount() {
    return collisionCount;
  }

  public void setCollisionCount(Integer collisionCount) {
    this.collisionCount = collisionCount;
  }

  public V1beta2StatefulSetStatus conditions(List<V1beta2StatefulSetCondition> conditions) {

    this.conditions = conditions;
    return this;
  }

  public V1beta2StatefulSetStatus addConditionsItem(V1beta2StatefulSetCondition conditionsItem) {
    if (this.conditions == null) {
      this.conditions = new ArrayList<V1beta2StatefulSetCondition>();
    }
    this.conditions.add(conditionsItem);
    return this;
  }

  /**
   * Represents the latest available observations of a statefulset&#39;s current state.
   *
   * @return conditions
   */
  @jakarta.annotation.Nullable
  @Schema(
      description = "Represents the latest available observations of a statefulset's current state.")
  public List<V1beta2StatefulSetCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<V1beta2StatefulSetCondition> conditions) {
    this.conditions = conditions;
  }

  public V1beta2StatefulSetStatus currentReplicas(Integer currentReplicas) {

    this.currentReplicas = currentReplicas;
    return this;
  }

  /**
   * currentReplicas is the number of Pods created by the StatefulSet controller from the
   * StatefulSet version indicated by currentRevision.
   *
   * @return currentReplicas
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "currentReplicas is the number of Pods created by the StatefulSet controller from the StatefulSet version indicated by currentRevision.")
  public Integer getCurrentReplicas() {
    return currentReplicas;
  }

  public void setCurrentReplicas(Integer currentReplicas) {
    this.currentReplicas = currentReplicas;
  }

  public V1beta2StatefulSetStatus currentRevision(String currentRevision) {

    this.currentRevision = currentRevision;
    return this;
  }

  /**
   * currentRevision, if not empty, indicates the version of the StatefulSet used to generate Pods
   * in the sequence [0,currentReplicas).
   *
   * @return currentRevision
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "currentRevision, if not empty, indicates the version of the StatefulSet used to generate Pods in the sequence [0,currentReplicas).")
  public String getCurrentRevision() {
    return currentRevision;
  }

  public void setCurrentRevision(String currentRevision) {
    this.currentRevision = currentRevision;
  }

  public V1beta2StatefulSetStatus observedGeneration(Long observedGeneration) {

    this.observedGeneration = observedGeneration;
    return this;
  }

  /**
   * observedGeneration is the most recent generation observed for this StatefulSet. It corresponds
   * to the StatefulSet&#39;s generation, which is updated on mutation by the API Server.
   *
   * @return observedGeneration
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "observedGeneration is the most recent generation observed for this StatefulSet. It corresponds to the StatefulSet's generation, which is updated on mutation by the API Server.")
  public Long getObservedGeneration() {
    return observedGeneration;
  }

  public void setObservedGeneration(Long observedGeneration) {
    this.observedGeneration = observedGeneration;
  }

  public V1beta2StatefulSetStatus readyReplicas(Integer readyReplicas) {

    this.readyReplicas = readyReplicas;
    return this;
  }

  /**
   * readyReplicas is the number of Pods created by the StatefulSet controller that have a Ready
   * Condition.
   *
   * @return readyReplicas
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "readyReplicas is the number of Pods created by the StatefulSet controller that have a Ready Condition.")
  public Integer getReadyReplicas() {
    return readyReplicas;
  }

  public void setReadyReplicas(Integer readyReplicas) {
    this.readyReplicas = readyReplicas;
  }

  public V1beta2StatefulSetStatus replicas(Integer replicas) {

    this.replicas = replicas;
    return this;
  }

  /**
   * replicas is the number of Pods created by the StatefulSet controller.
   *
   * @return replicas
   */
  @Schema(
      required = true,
      description = "replicas is the number of Pods created by the StatefulSet controller.")
  public Integer getReplicas() {
    return replicas;
  }

  public void setReplicas(Integer replicas) {
    this.replicas = replicas;
  }

  public V1beta2StatefulSetStatus updateRevision(String updateRevision) {

    this.updateRevision = updateRevision;
    return this;
  }

  /**
   * updateRevision, if not empty, indicates the version of the StatefulSet used to generate Pods in
   * the sequence [replicas-updatedReplicas,replicas)
   *
   * @return updateRevision
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "updateRevision, if not empty, indicates the version of the StatefulSet used to generate Pods in the sequence [replicas-updatedReplicas,replicas)")
  public String getUpdateRevision() {
    return updateRevision;
  }

  public void setUpdateRevision(String updateRevision) {
    this.updateRevision = updateRevision;
  }

  public V1beta2StatefulSetStatus updatedReplicas(Integer updatedReplicas) {

    this.updatedReplicas = updatedReplicas;
    return this;
  }

  /**
   * updatedReplicas is the number of Pods created by the StatefulSet controller from the
   * StatefulSet version indicated by updateRevision.
   *
   * @return updatedReplicas
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "updatedReplicas is the number of Pods created by the StatefulSet controller from the StatefulSet version indicated by updateRevision.")
  public Integer getUpdatedReplicas() {
    return updatedReplicas;
  }

  public void setUpdatedReplicas(Integer updatedReplicas) {
    this.updatedReplicas = updatedReplicas;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta2StatefulSetStatus v1beta2StatefulSetStatus = (V1beta2StatefulSetStatus) o;
    return Objects.equals(this.collisionCount, v1beta2StatefulSetStatus.collisionCount)
        && Objects.equals(this.conditions, v1beta2StatefulSetStatus.conditions)
        && Objects.equals(this.currentReplicas, v1beta2StatefulSetStatus.currentReplicas)
        && Objects.equals(this.currentRevision, v1beta2StatefulSetStatus.currentRevision)
        && Objects.equals(this.observedGeneration, v1beta2StatefulSetStatus.observedGeneration)
        && Objects.equals(this.readyReplicas, v1beta2StatefulSetStatus.readyReplicas)
        && Objects.equals(this.replicas, v1beta2StatefulSetStatus.replicas)
        && Objects.equals(this.updateRevision, v1beta2StatefulSetStatus.updateRevision)
        && Objects.equals(this.updatedReplicas, v1beta2StatefulSetStatus.updatedReplicas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        collisionCount,
        conditions,
        currentReplicas,
        currentRevision,
        observedGeneration,
        readyReplicas,
        replicas,
        updateRevision,
        updatedReplicas);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta2StatefulSetStatus {\n");
    sb.append("    collisionCount: ").append(toIndentedString(collisionCount)).append("\n");
    sb.append("    conditions: ").append(toIndentedString(conditions)).append("\n");
    sb.append("    currentReplicas: ").append(toIndentedString(currentReplicas)).append("\n");
    sb.append("    currentRevision: ").append(toIndentedString(currentRevision)).append("\n");
    sb.append("    observedGeneration: ").append(toIndentedString(observedGeneration)).append("\n");
    sb.append("    readyReplicas: ").append(toIndentedString(readyReplicas)).append("\n");
    sb.append("    replicas: ").append(toIndentedString(replicas)).append("\n");
    sb.append("    updateRevision: ").append(toIndentedString(updateRevision)).append("\n");
    sb.append("    updatedReplicas: ").append(toIndentedString(updatedReplicas)).append("\n");
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
