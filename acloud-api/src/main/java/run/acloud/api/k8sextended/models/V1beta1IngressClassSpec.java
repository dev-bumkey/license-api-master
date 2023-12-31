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
import io.kubernetes.client.openapi.models.V1TypedLocalObjectReference;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/** IngressClassSpec provides information about the class of an Ingress. */
@Schema(description = "IngressClassSpec provides information about the class of an Ingress.")
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-01-04T09:55:14.976Z[Etc/UTC]")
public class V1beta1IngressClassSpec {
  public static final String SERIALIZED_NAME_CONTROLLER = "controller";

  @SerializedName(SERIALIZED_NAME_CONTROLLER)
  private String controller;

  public static final String SERIALIZED_NAME_PARAMETERS = "parameters";

  @SerializedName(SERIALIZED_NAME_PARAMETERS)
  private V1TypedLocalObjectReference parameters;

  public V1beta1IngressClassSpec controller(String controller) {

    this.controller = controller;
    return this;
  }

  /**
   * Controller refers to the name of the controller that should handle this class. This allows for
   * different \&quot;flavors\&quot; that are controlled by the same controller. For example, you
   * may have different Parameters for the same implementing controller. This should be specified as
   * a domain-prefixed path no more than 250 characters in length, e.g.
   * \&quot;acme.io/ingress-controller\&quot;. This field is immutable.
   *
   * @return controller
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "Controller refers to the name of the controller that should handle this class. This allows for different 'flavors' that are controlled by the same controller. For example, you may have different Parameters for the same implementing controller. This should be specified as a domain-prefixed path no more than 250 characters in length, e.g. 'acme.io/ingress-controller'. This field is immutable.")
  public String getController() {
    return controller;
  }

  public void setController(String controller) {
    this.controller = controller;
  }

  public V1beta1IngressClassSpec parameters(V1TypedLocalObjectReference parameters) {

    this.parameters = parameters;
    return this;
  }

  /**
   * Get parameters
   *
   * @return parameters
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public V1TypedLocalObjectReference getParameters() {
    return parameters;
  }

  public void setParameters(V1TypedLocalObjectReference parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1beta1IngressClassSpec v1beta1IngressClassSpec = (V1beta1IngressClassSpec) o;
    return Objects.equals(this.controller, v1beta1IngressClassSpec.controller)
        && Objects.equals(this.parameters, v1beta1IngressClassSpec.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(controller, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1beta1IngressClassSpec {\n");
    sb.append("    controller: ").append(toIndentedString(controller)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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
