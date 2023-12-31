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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DEPRECATED 1.9 - This group version of NetworkPolicySpec is deprecated by
 * networking/v1/NetworkPolicySpec.
 */
@Schema(
    description =
        "DEPRECATED 1.9 - This group version of NetworkPolicySpec is deprecated by networking/v1/NetworkPolicySpec.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2020-06-19T10:47:33.387Z[Etc/UTC]")
public class V1beta1NetworkPolicySpec {
  public static final String SERIALIZED_NAME_EGRESS = "egress";

  @SerializedName(SERIALIZED_NAME_EGRESS)
  private List<V1beta1NetworkPolicyEgressRule> egress = null;

  public static final String SERIALIZED_NAME_INGRESS = "ingress";

  @SerializedName(SERIALIZED_NAME_INGRESS)
  private List<V1beta1NetworkPolicyIngressRule> ingress = null;

  public static final String SERIALIZED_NAME_POD_SELECTOR = "podSelector";

  @SerializedName(SERIALIZED_NAME_POD_SELECTOR)
  private V1LabelSelector podSelector;

  public static final String SERIALIZED_NAME_POLICY_TYPES = "policyTypes";

  @SerializedName(SERIALIZED_NAME_POLICY_TYPES)
  private List<String> policyTypes = null;

  public V1beta1NetworkPolicySpec egress(List<V1beta1NetworkPolicyEgressRule> egress) {

    this.egress = egress;
    return this;
  }

  public V1beta1NetworkPolicySpec addEgressItem(V1beta1NetworkPolicyEgressRule egressItem) {
    if (this.egress == null) {
      this.egress = new ArrayList<V1beta1NetworkPolicyEgressRule>();
    }
    this.egress.add(egressItem);
    return this;
  }

  /**
   * List of egress rules to be applied to the selected pods. Outgoing traffic is allowed if there
   * are no NetworkPolicies selecting the pod (and cluster policy otherwise allows the traffic), OR
   * if the traffic matches at least one egress rule across all of the NetworkPolicy objects whose
   * podSelector matches the pod. If this field is empty then this NetworkPolicy limits all outgoing
   * traffic (and serves solely to ensure that the pods it selects are isolated by default). This
   * field is beta-level in 1.8
   *
   * @return egress
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "List of egress rules to be applied to the selected pods. Outgoing traffic is allowed if there are no NetworkPolicies selecting the pod (and cluster policy otherwise allows the traffic), OR if the traffic matches at least one egress rule across all of the NetworkPolicy objects whose podSelector matches the pod. If this field is empty then this NetworkPolicy limits all outgoing traffic (and serves solely to ensure that the pods it selects are isolated by default). This field is beta-level in 1.8")
  public List<V1beta1NetworkPolicyEgressRule> getEgress() {
    return egress;
  }

  public void setEgress(List<V1beta1NetworkPolicyEgressRule> egress) {
    this.egress = egress;
  }

  public V1beta1NetworkPolicySpec ingress(List<V1beta1NetworkPolicyIngressRule> ingress) {

    this.ingress = ingress;
    return this;
  }

  public V1beta1NetworkPolicySpec addIngressItem(V1beta1NetworkPolicyIngressRule ingressItem) {
    if (this.ingress == null) {
      this.ingress = new ArrayList<V1beta1NetworkPolicyIngressRule>();
    }
    this.ingress.add(ingressItem);
    return this;
  }

  /**
   * List of ingress rules to be applied to the selected pods. Traffic is allowed to a pod if there
   * are no NetworkPolicies selecting the pod OR if the traffic source is the pod&#39;s local node,
   * OR if the traffic matches at least one ingress rule across all of the NetworkPolicy objects
   * whose podSelector matches the pod. If this field is empty then this NetworkPolicy does not
   * allow any traffic (and serves solely to ensure that the pods it selects are isolated by
   * default).
   *
   * @return ingress
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "List of ingress rules to be applied to the selected pods. Traffic is allowed to a pod if there are no NetworkPolicies selecting the pod OR if the traffic source is the pod's local node, OR if the traffic matches at least one ingress rule across all of the NetworkPolicy objects whose podSelector matches the pod. If this field is empty then this NetworkPolicy does not allow any traffic (and serves solely to ensure that the pods it selects are isolated by default).")
  public List<V1beta1NetworkPolicyIngressRule> getIngress() {
    return ingress;
  }

  public void setIngress(List<V1beta1NetworkPolicyIngressRule> ingress) {
    this.ingress = ingress;
  }

  public V1beta1NetworkPolicySpec podSelector(V1LabelSelector podSelector) {

    this.podSelector = podSelector;
    return this;
  }

  /**
   * Get podSelector
   *
   * @return podSelector
   */
  @Schema(required = true, description = "")
  public V1LabelSelector getPodSelector() {
    return podSelector;
  }

  public void setPodSelector(V1LabelSelector podSelector) {
    this.podSelector = podSelector;
  }

  public V1beta1NetworkPolicySpec policyTypes(List<String> policyTypes) {

    this.policyTypes = policyTypes;
    return this;
  }

  public V1beta1NetworkPolicySpec addPolicyTypesItem(String policyTypesItem) {
    if (this.policyTypes == null) {
      this.policyTypes = new ArrayList<String>();
    }
    this.policyTypes.add(policyTypesItem);
    return this;
  }

  /**
   * List of rule types that the NetworkPolicy relates to. Valid options are \&quot;Ingress\&quot;,
   * \&quot;Egress\&quot;, or \&quot;Ingress,Egress\&quot;. If this field is not specified, it will
   * default based on the existence of Ingress or Egress rules; policies that contain an Egress
   * section are assumed to affect Egress, and all policies (whether or not they contain an Ingress
   * section) are assumed to affect Ingress. If you want to write an egress-only policy, you must
   * explicitly specify policyTypes [ \&quot;Egress\&quot; ]. Likewise, if you want to write a
   * policy that specifies that no egress is allowed, you must specify a policyTypes value that
   * include \&quot;Egress\&quot; (since such a policy would not include an Egress section and would
   * otherwise default to just [ \&quot;Ingress\&quot; ]). This field is beta-level in 1.8
   *
   * @return policyTypes
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "List of rule types that the NetworkPolicy relates to. Valid options are 'Ingress', 'Egress', or 'Ingress,Egress'. If this field is not specified, it will default based on the existence of Ingress or Egress rules; policies that contain an Egress section are assumed to affect Egress, and all policies (whether or not they contain an Ingress section) are assumed to affect Ingress. If you want to write an egress-only policy, you must explicitly specify policyTypes [ 'Egress' ]. Likewise, if you want to write a policy that specifies that no egress is allowed, you must specify a policyTypes value that include 'Egress' (since such a policy would not include an Egress section and would otherwise default to just [ 'Ingress' ]). This field is beta-level in 1.8")
  public List<String> getPolicyTypes() {
    return policyTypes;
  }

  public void setPolicyTypes(List<String> policyTypes) {
    this.policyTypes = policyTypes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta1NetworkPolicySpec v1beta1NetworkPolicySpec = (V1beta1NetworkPolicySpec) o;
    return Objects.equals(this.egress, v1beta1NetworkPolicySpec.egress)
        && Objects.equals(this.ingress, v1beta1NetworkPolicySpec.ingress)
        && Objects.equals(this.podSelector, v1beta1NetworkPolicySpec.podSelector)
        && Objects.equals(this.policyTypes, v1beta1NetworkPolicySpec.policyTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(egress, ingress, podSelector, policyTypes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta1NetworkPolicySpec {\n");
    sb.append("    egress: ").append(toIndentedString(egress)).append("\n");
    sb.append("    ingress: ").append(toIndentedString(ingress)).append("\n");
    sb.append("    podSelector: ").append(toIndentedString(podSelector)).append("\n");
    sb.append("    policyTypes: ").append(toIndentedString(policyTypes)).append("\n");
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
