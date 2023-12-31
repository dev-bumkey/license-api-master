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

/**
 * DEPRECATED 1.9 - This group version of IPBlock is deprecated by networking/v1/IPBlock. IPBlock
 * describes a particular CIDR (Ex. \&quot;192.168.1.1/24\&quot;) that is allowed to the pods
 * matched by a NetworkPolicySpec&#39;s podSelector. The except entry describes CIDRs that should
 * not be included within this rule.
 */
@Schema(
    description =
        "DEPRECATED 1.9 - This group version of IPBlock is deprecated by networking/v1/IPBlock. IPBlock describes a particular CIDR (Ex. '192.168.1.1/24') that is allowed to the pods matched by a NetworkPolicySpec's podSelector. The except entry describes CIDRs that should not be included within this rule.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta1IPBlock {
  public static final String SERIALIZED_NAME_CIDR = "cidr";

  @SerializedName(SERIALIZED_NAME_CIDR)
  private String cidr;

  public static final String SERIALIZED_NAME_EXCEPT = "except";

  @SerializedName(SERIALIZED_NAME_EXCEPT)
  private List<String> except = null;

  public V1beta1IPBlock cidr(String cidr) {

    this.cidr = cidr;
    return this;
  }

  /**
   * CIDR is a string representing the IP Block Valid examples are \&quot;192.168.1.1/24\&quot;
   *
   * @return cidr
   */
  @Schema(
      required = true,
      description = "CIDR is a string representing the IP Block Valid examples are '192.168.1.1/24'")
  public String getCidr() {
    return cidr;
  }

  public void setCidr(String cidr) {
    this.cidr = cidr;
  }

  public V1beta1IPBlock except(List<String> except) {

    this.except = except;
    return this;
  }

  public V1beta1IPBlock addExceptItem(String exceptItem) {
    if (this.except == null) {
      this.except = new ArrayList<String>();
    }
    this.except.add(exceptItem);
    return this;
  }

  /**
   * Except is a slice of CIDRs that should not be included within an IP Block Valid examples are
   * \&quot;192.168.1.1/24\&quot; Except values will be rejected if they are outside the CIDR range
   *
   * @return except
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Except is a slice of CIDRs that should not be included within an IP Block Valid examples are '192.168.1.1/24' Except values will be rejected if they are outside the CIDR range")
  public List<String> getExcept() {
    return except;
  }

  public void setExcept(List<String> except) {
    this.except = except;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta1IPBlock v1beta1IPBlock = (V1beta1IPBlock) o;
    return Objects.equals(this.cidr, v1beta1IPBlock.cidr)
        && Objects.equals(this.except, v1beta1IPBlock.except);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cidr, except);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta1IPBlock {\n");
    sb.append("    cidr: ").append(toIndentedString(cidr)).append("\n");
    sb.append("    except: ").append(toIndentedString(except)).append("\n");
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
