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
import io.kubernetes.client.custom.IntOrString;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/** Spec to control the desired behavior of daemon set rolling update. */
@Schema(description = "Spec to control the desired behavior of daemon set rolling update.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta2RollingUpdateDaemonSet {
  public static final String SERIALIZED_NAME_MAX_UNAVAILABLE = "maxUnavailable";

  @SerializedName(SERIALIZED_NAME_MAX_UNAVAILABLE)
  private IntOrString maxUnavailable;

  public V1beta2RollingUpdateDaemonSet maxUnavailable(IntOrString maxUnavailable) {

    this.maxUnavailable = maxUnavailable;
    return this;
  }

  /**
   * IntOrString is a type that can hold an int32 or a string. When used in JSON or YAML marshalling
   * and unmarshalling, it produces or consumes the inner type. This allows you to have, for
   * example, a JSON field that can accept a name or number.
   *
   * @return maxUnavailable
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "IntOrString is a type that can hold an int32 or a string.  When used in JSON or YAML marshalling and unmarshalling, it produces or consumes the inner type.  This allows you to have, for example, a JSON field that can accept a name or number.")
  public IntOrString getMaxUnavailable() {
    return maxUnavailable;
  }

  public void setMaxUnavailable(IntOrString maxUnavailable) {
    this.maxUnavailable = maxUnavailable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta2RollingUpdateDaemonSet v1beta2RollingUpdateDaemonSet = (V1beta2RollingUpdateDaemonSet) o;
    return Objects.equals(this.maxUnavailable, v1beta2RollingUpdateDaemonSet.maxUnavailable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxUnavailable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta2RollingUpdateDaemonSet {\n");
    sb.append("    maxUnavailable: ").append(toIndentedString(maxUnavailable)).append("\n");
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
