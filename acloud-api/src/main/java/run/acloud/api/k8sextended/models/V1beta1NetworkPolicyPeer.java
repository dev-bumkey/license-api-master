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
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * DEPRECATED 1.9 - This group version of NetworkPolicyPeer is deprecated by
 * networking/v1/NetworkPolicyPeer.
 */
@Schema(
    description =
        "DEPRECATED 1.9 - This group version of NetworkPolicyPeer is deprecated by networking/v1/NetworkPolicyPeer.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta1NetworkPolicyPeer {
  public static final String SERIALIZED_NAME_IP_BLOCK = "ipBlock";

  @SerializedName(SERIALIZED_NAME_IP_BLOCK)
  private V1beta1IPBlock ipBlock;

  public static final String SERIALIZED_NAME_NAMESPACE_SELECTOR = "namespaceSelector";

  @SerializedName(SERIALIZED_NAME_NAMESPACE_SELECTOR)
  private V1LabelSelector namespaceSelector;

  public static final String SERIALIZED_NAME_POD_SELECTOR = "podSelector";

  @SerializedName(SERIALIZED_NAME_POD_SELECTOR)
  private V1LabelSelector podSelector;

  public V1beta1NetworkPolicyPeer ipBlock(V1beta1IPBlock ipBlock) {

    this.ipBlock = ipBlock;
    return this;
  }

  /**
   * Get ipBlock
   *
   * @return ipBlock
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1beta1IPBlock getIpBlock() {
    return ipBlock;
  }

  public void setIpBlock(V1beta1IPBlock ipBlock) {
    this.ipBlock = ipBlock;
  }

  public V1beta1NetworkPolicyPeer namespaceSelector(V1LabelSelector namespaceSelector) {

    this.namespaceSelector = namespaceSelector;
    return this;
  }

  /**
   * Get namespaceSelector
   *
   * @return namespaceSelector
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1LabelSelector getNamespaceSelector() {
    return namespaceSelector;
  }

  public void setNamespaceSelector(V1LabelSelector namespaceSelector) {
    this.namespaceSelector = namespaceSelector;
  }

  public V1beta1NetworkPolicyPeer podSelector(V1LabelSelector podSelector) {

    this.podSelector = podSelector;
    return this;
  }

  /**
   * Get podSelector
   *
   * @return podSelector
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1LabelSelector getPodSelector() {
    return podSelector;
  }

  public void setPodSelector(V1LabelSelector podSelector) {
    this.podSelector = podSelector;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta1NetworkPolicyPeer v1beta1NetworkPolicyPeer = (V1beta1NetworkPolicyPeer) o;
    return Objects.equals(this.ipBlock, v1beta1NetworkPolicyPeer.ipBlock)
        && Objects.equals(this.namespaceSelector, v1beta1NetworkPolicyPeer.namespaceSelector)
        && Objects.equals(this.podSelector, v1beta1NetworkPolicyPeer.podSelector);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ipBlock, namespaceSelector, podSelector);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta1NetworkPolicyPeer {\n");
    sb.append("    ipBlock: ").append(toIndentedString(ipBlock)).append("\n");
    sb.append("    namespaceSelector: ").append(toIndentedString(namespaceSelector)).append("\n");
    sb.append("    podSelector: ").append(toIndentedString(podSelector)).append("\n");
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
