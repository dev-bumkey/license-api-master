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
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * DEPRECATED - This group version of ReplicaSet is deprecated by apps/v1/ReplicaSet. See the
 * release notes for more information. ReplicaSet ensures that a specified number of pod replicas
 * are running at any given time.
 */
@Schema(
    description =
        "DEPRECATED - This group version of ReplicaSet is deprecated by apps/v1/ReplicaSet. See the release notes for more information. ReplicaSet ensures that a specified number of pod replicas are running at any given time.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta2ReplicaSet implements io.kubernetes.client.common.KubernetesObject {
  public static final String SERIALIZED_NAME_API_VERSION = "apiVersion";

  @SerializedName(SERIALIZED_NAME_API_VERSION)
  private String apiVersion;

  public static final String SERIALIZED_NAME_KIND = "kind";

  @SerializedName(SERIALIZED_NAME_KIND)
  private String kind;

  public static final String SERIALIZED_NAME_METADATA = "metadata";

  @SerializedName(SERIALIZED_NAME_METADATA)
  private V1ObjectMeta metadata;

  public static final String SERIALIZED_NAME_SPEC = "spec";

  @SerializedName(SERIALIZED_NAME_SPEC)
  private V1beta2ReplicaSetSpec spec;

  public static final String SERIALIZED_NAME_STATUS = "status";

  @SerializedName(SERIALIZED_NAME_STATUS)
  private V1beta2ReplicaSetStatus status;

  public V1beta2ReplicaSet apiVersion(String apiVersion) {

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

  public V1beta2ReplicaSet kind(String kind) {

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

  public V1beta2ReplicaSet metadata(V1ObjectMeta metadata) {

    this.metadata = metadata;
    return this;
  }

  /**
   * Get metadata
   *
   * @return metadata
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1ObjectMeta getMetadata() {
    return metadata;
  }

  public void setMetadata(V1ObjectMeta metadata) {
    this.metadata = metadata;
  }

  public V1beta2ReplicaSet spec(V1beta2ReplicaSetSpec spec) {

    this.spec = spec;
    return this;
  }

  /**
   * Get spec
   *
   * @return spec
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1beta2ReplicaSetSpec getSpec() {
    return spec;
  }

  public void setSpec(V1beta2ReplicaSetSpec spec) {
    this.spec = spec;
  }

  public V1beta2ReplicaSet status(V1beta2ReplicaSetStatus status) {

    this.status = status;
    return this;
  }

  /**
   * Get status
   *
   * @return status
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1beta2ReplicaSetStatus getStatus() {
    return status;
  }

  public void setStatus(V1beta2ReplicaSetStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta2ReplicaSet v1beta2ReplicaSet = (V1beta2ReplicaSet) o;
    return Objects.equals(this.apiVersion, v1beta2ReplicaSet.apiVersion)
        && Objects.equals(this.kind, v1beta2ReplicaSet.kind)
        && Objects.equals(this.metadata, v1beta2ReplicaSet.metadata)
        && Objects.equals(this.spec, v1beta2ReplicaSet.spec)
        && Objects.equals(this.status, v1beta2ReplicaSet.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiVersion, kind, metadata, spec, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta2ReplicaSet {\n");
    sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    spec: ").append(toIndentedString(spec)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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
