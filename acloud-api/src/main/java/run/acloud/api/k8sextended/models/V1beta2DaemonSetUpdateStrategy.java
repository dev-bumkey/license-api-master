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

import java.util.Objects;

/** DaemonSetUpdateStrategy is a struct used to control the update strategy for a DaemonSet. */
@Schema(
    description =
        "DaemonSetUpdateStrategy is a struct used to control the update strategy for a DaemonSet.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta2DaemonSetUpdateStrategy {
  public static final String SERIALIZED_NAME_ROLLING_UPDATE = "rollingUpdate";

  @SerializedName(SERIALIZED_NAME_ROLLING_UPDATE)
  private V1beta2RollingUpdateDaemonSet rollingUpdate;

  public static final String SERIALIZED_NAME_TYPE = "type";

  @SerializedName(SERIALIZED_NAME_TYPE)
  private String type;

  public V1beta2DaemonSetUpdateStrategy rollingUpdate(V1beta2RollingUpdateDaemonSet rollingUpdate) {

    this.rollingUpdate = rollingUpdate;
    return this;
  }

  /**
   * Get rollingUpdate
   *
   * @return rollingUpdate
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1beta2RollingUpdateDaemonSet getRollingUpdate() {
    return rollingUpdate;
  }

  public void setRollingUpdate(V1beta2RollingUpdateDaemonSet rollingUpdate) {
    this.rollingUpdate = rollingUpdate;
  }

  public V1beta2DaemonSetUpdateStrategy type(String type) {

    this.type = type;
    return this;
  }

  /**
   * Type of daemon set update. Can be \&quot;RollingUpdate\&quot; or \&quot;OnDelete\&quot;.
   * Default is RollingUpdate.
   *
   * @return type
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Type of daemon set update. Can be 'RollingUpdate' or 'OnDelete'. Default is RollingUpdate.")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta2DaemonSetUpdateStrategy v1beta2DaemonSetUpdateStrategy =
        (V1beta2DaemonSetUpdateStrategy) o;
    return Objects.equals(this.rollingUpdate, v1beta2DaemonSetUpdateStrategy.rollingUpdate)
        && Objects.equals(this.type, v1beta2DaemonSetUpdateStrategy.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rollingUpdate, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta2DaemonSetUpdateStrategy {\n");
    sb.append("    rollingUpdate: ").append(toIndentedString(rollingUpdate)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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
