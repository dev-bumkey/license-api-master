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

/**
 * IDRange provides a min/max of an allowed range of IDs. Deprecated: use IDRange from policy API
 * Group instead.
 */
@Schema(
    description =
        "IDRange provides a min/max of an allowed range of IDs. Deprecated: use IDRange from policy API Group instead.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class ExtensionsV1beta1IDRange {
  public static final String SERIALIZED_NAME_MAX = "max";

  @SerializedName(SERIALIZED_NAME_MAX)
  private Long max;

  public static final String SERIALIZED_NAME_MIN = "min";

  @SerializedName(SERIALIZED_NAME_MIN)
  private Long min;

  public ExtensionsV1beta1IDRange max(Long max) {

    this.max = max;
    return this;
  }

  /**
   * max is the end of the range, inclusive.
   *
   * @return max
   */
  @Schema(required = true, description = "max is the end of the range, inclusive.")
  public Long getMax() {
    return max;
  }

  public void setMax(Long max) {
    this.max = max;
  }

  public ExtensionsV1beta1IDRange min(Long min) {

    this.min = min;
    return this;
  }

  /**
   * min is the start of the range, inclusive.
   *
   * @return min
   */
  @Schema(required = true, description = "min is the start of the range, inclusive.")
  public Long getMin() {
    return min;
  }

  public void setMin(Long min) {
    this.min = min;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExtensionsV1beta1IDRange extensionsV1beta1IDRange = (ExtensionsV1beta1IDRange) o;
    return Objects.equals(this.max, extensionsV1beta1IDRange.max)
        && Objects.equals(this.min, extensionsV1beta1IDRange.min);
  }

  @Override
  public int hashCode() {
    return Objects.hash(max, min);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExtensionsV1beta1IDRange {\n");
    sb.append("    max: ").append(toIndentedString(max)).append("\n");
    sb.append("    min: ").append(toIndentedString(min)).append("\n");
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
