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

/** DeploymentStatus is the most recently observed status of the Deployment. */
@Schema(description = "DeploymentStatus is the most recently observed status of the Deployment.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta2DeploymentStatus {
  public static final String SERIALIZED_NAME_AVAILABLE_REPLICAS = "availableReplicas";

  @SerializedName(SERIALIZED_NAME_AVAILABLE_REPLICAS)
  private Integer availableReplicas;

  public static final String SERIALIZED_NAME_COLLISION_COUNT = "collisionCount";

  @SerializedName(SERIALIZED_NAME_COLLISION_COUNT)
  private Integer collisionCount;

  public static final String SERIALIZED_NAME_CONDITIONS = "conditions";

  @SerializedName(SERIALIZED_NAME_CONDITIONS)
  private List<V1beta2DeploymentCondition> conditions = null;

  public static final String SERIALIZED_NAME_OBSERVED_GENERATION = "observedGeneration";

  @SerializedName(SERIALIZED_NAME_OBSERVED_GENERATION)
  private Long observedGeneration;

  public static final String SERIALIZED_NAME_READY_REPLICAS = "readyReplicas";

  @SerializedName(SERIALIZED_NAME_READY_REPLICAS)
  private Integer readyReplicas;

  public static final String SERIALIZED_NAME_REPLICAS = "replicas";

  @SerializedName(SERIALIZED_NAME_REPLICAS)
  private Integer replicas;

  public static final String SERIALIZED_NAME_UNAVAILABLE_REPLICAS = "unavailableReplicas";

  @SerializedName(SERIALIZED_NAME_UNAVAILABLE_REPLICAS)
  private Integer unavailableReplicas;

  public static final String SERIALIZED_NAME_UPDATED_REPLICAS = "updatedReplicas";

  @SerializedName(SERIALIZED_NAME_UPDATED_REPLICAS)
  private Integer updatedReplicas;

  public V1beta2DeploymentStatus availableReplicas(Integer availableReplicas) {

    this.availableReplicas = availableReplicas;
    return this;
  }

  /**
   * Total number of available pods (ready for at least minReadySeconds) targeted by this
   * deployment.
   *
   * @return availableReplicas
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Total number of available pods (ready for at least minReadySeconds) targeted by this deployment.")
  public Integer getAvailableReplicas() {
    return availableReplicas;
  }

  public void setAvailableReplicas(Integer availableReplicas) {
    this.availableReplicas = availableReplicas;
  }

  public V1beta2DeploymentStatus collisionCount(Integer collisionCount) {

    this.collisionCount = collisionCount;
    return this;
  }

  /**
   * Count of hash collisions for the Deployment. The Deployment controller uses this field as a
   * collision avoidance mechanism when it needs to create the name for the newest ReplicaSet.
   *
   * @return collisionCount
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Count of hash collisions for the Deployment. The Deployment controller uses this field as a collision avoidance mechanism when it needs to create the name for the newest ReplicaSet.")
  public Integer getCollisionCount() {
    return collisionCount;
  }

  public void setCollisionCount(Integer collisionCount) {
    this.collisionCount = collisionCount;
  }

  public V1beta2DeploymentStatus conditions(List<V1beta2DeploymentCondition> conditions) {

    this.conditions = conditions;
    return this;
  }

  public V1beta2DeploymentStatus addConditionsItem(V1beta2DeploymentCondition conditionsItem) {
    if (this.conditions == null) {
      this.conditions = new ArrayList<V1beta2DeploymentCondition>();
    }
    this.conditions.add(conditionsItem);
    return this;
  }

  /**
   * Represents the latest available observations of a deployment&#39;s current state.
   *
   * @return conditions
   */
  @jakarta.annotation.Nullable
  @Schema(
      description = "Represents the latest available observations of a deployment's current state.")
  public List<V1beta2DeploymentCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<V1beta2DeploymentCondition> conditions) {
    this.conditions = conditions;
  }

  public V1beta2DeploymentStatus observedGeneration(Long observedGeneration) {

    this.observedGeneration = observedGeneration;
    return this;
  }

  /**
   * The generation observed by the deployment controller.
   *
   * @return observedGeneration
   */
  @jakarta.annotation.Nullable
  @Schema(description = "The generation observed by the deployment controller.")
  public Long getObservedGeneration() {
    return observedGeneration;
  }

  public void setObservedGeneration(Long observedGeneration) {
    this.observedGeneration = observedGeneration;
  }

  public V1beta2DeploymentStatus readyReplicas(Integer readyReplicas) {

    this.readyReplicas = readyReplicas;
    return this;
  }

  /**
   * Total number of ready pods targeted by this deployment.
   *
   * @return readyReplicas
   */
  @jakarta.annotation.Nullable
  @Schema(description = "Total number of ready pods targeted by this deployment.")
  public Integer getReadyReplicas() {
    return readyReplicas;
  }

  public void setReadyReplicas(Integer readyReplicas) {
    this.readyReplicas = readyReplicas;
  }

  public V1beta2DeploymentStatus replicas(Integer replicas) {

    this.replicas = replicas;
    return this;
  }

  /**
   * Total number of non-terminated pods targeted by this deployment (their labels match the
   * selector).
   *
   * @return replicas
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Total number of non-terminated pods targeted by this deployment (their labels match the selector).")
  public Integer getReplicas() {
    return replicas;
  }

  public void setReplicas(Integer replicas) {
    this.replicas = replicas;
  }

  public V1beta2DeploymentStatus unavailableReplicas(Integer unavailableReplicas) {

    this.unavailableReplicas = unavailableReplicas;
    return this;
  }

  /**
   * Total number of unavailable pods targeted by this deployment. This is the total number of pods
   * that are still required for the deployment to have 100% available capacity. They may either be
   * pods that are running but not yet available or pods that still have not been created.
   *
   * @return unavailableReplicas
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Total number of unavailable pods targeted by this deployment. This is the total number of pods that are still required for the deployment to have 100% available capacity. They may either be pods that are running but not yet available or pods that still have not been created.")
  public Integer getUnavailableReplicas() {
    return unavailableReplicas;
  }

  public void setUnavailableReplicas(Integer unavailableReplicas) {
    this.unavailableReplicas = unavailableReplicas;
  }

  public V1beta2DeploymentStatus updatedReplicas(Integer updatedReplicas) {

    this.updatedReplicas = updatedReplicas;
    return this;
  }

  /**
   * Total number of non-terminated pods targeted by this deployment that have the desired template
   * spec.
   *
   * @return updatedReplicas
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Total number of non-terminated pods targeted by this deployment that have the desired template spec.")
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
    V1beta2DeploymentStatus v1beta2DeploymentStatus = (V1beta2DeploymentStatus) o;
    return Objects.equals(this.availableReplicas, v1beta2DeploymentStatus.availableReplicas)
        && Objects.equals(this.collisionCount, v1beta2DeploymentStatus.collisionCount)
        && Objects.equals(this.conditions, v1beta2DeploymentStatus.conditions)
        && Objects.equals(this.observedGeneration, v1beta2DeploymentStatus.observedGeneration)
        && Objects.equals(this.readyReplicas, v1beta2DeploymentStatus.readyReplicas)
        && Objects.equals(this.replicas, v1beta2DeploymentStatus.replicas)
        && Objects.equals(this.unavailableReplicas, v1beta2DeploymentStatus.unavailableReplicas)
        && Objects.equals(this.updatedReplicas, v1beta2DeploymentStatus.updatedReplicas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        availableReplicas,
        collisionCount,
        conditions,
        observedGeneration,
        readyReplicas,
        replicas,
        unavailableReplicas,
        updatedReplicas);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta2DeploymentStatus {\n");
    sb.append("    availableReplicas: ").append(toIndentedString(availableReplicas)).append("\n");
    sb.append("    collisionCount: ").append(toIndentedString(collisionCount)).append("\n");
    sb.append("    conditions: ").append(toIndentedString(conditions)).append("\n");
    sb.append("    observedGeneration: ").append(toIndentedString(observedGeneration)).append("\n");
    sb.append("    readyReplicas: ").append(toIndentedString(readyReplicas)).append("\n");
    sb.append("    replicas: ").append(toIndentedString(replicas)).append("\n");
    sb.append("    unavailableReplicas: ")
        .append(toIndentedString(unavailableReplicas))
        .append("\n");
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
