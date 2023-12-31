/*
Copyright 2021 The Kubernetes Authors.
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
import io.kubernetes.client.openapi.models.V1LoadBalancerStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/** IngressStatus describe the current state of the Ingress. */
@Schema(description = "IngressStatus describe the current state of the Ingress.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-01-04T09:55:14.976Z[Etc/UTC]")
public class NetworkingV1beta1IngressStatus {
  public static final String SERIALIZED_NAME_LOAD_BALANCER = "loadBalancer";

  @SerializedName(SERIALIZED_NAME_LOAD_BALANCER)
  private V1LoadBalancerStatus loadBalancer;

  public NetworkingV1beta1IngressStatus loadBalancer(V1LoadBalancerStatus loadBalancer) {

    this.loadBalancer = loadBalancer;
    return this;
  }

  /**
   * Get loadBalancer
   *
   * @return loadBalancer
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1LoadBalancerStatus getLoadBalancer() {
    return loadBalancer;
  }

  public void setLoadBalancer(V1LoadBalancerStatus loadBalancer) {
    this.loadBalancer = loadBalancer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NetworkingV1beta1IngressStatus networkingV1beta1IngressStatus =
        (NetworkingV1beta1IngressStatus) o;
    return Objects.equals(this.loadBalancer, networkingV1beta1IngressStatus.loadBalancer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(loadBalancer);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NetworkingV1beta1IngressStatus {\n");
    sb.append("    loadBalancer: ").append(toIndentedString(loadBalancer)).append("\n");
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
