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
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.Objects;

/** WebhookClientConfig contains the information to make a TLS connection with the webhook. */
@Schema(
    description =
        "WebhookClientConfig contains the information to make a TLS connection with the webhook.")
@jakarta.annotation.Generated(
        value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-01-04T09:55:14.976Z[Etc/UTC]")
public class ApiextensionsV1beta1WebhookClientConfig {
  public static final String SERIALIZED_NAME_CA_BUNDLE = "caBundle";

  @SerializedName(SERIALIZED_NAME_CA_BUNDLE)
  private byte[] caBundle;

  public static final String SERIALIZED_NAME_SERVICE = "service";

  @SerializedName(SERIALIZED_NAME_SERVICE)
  private ApiextensionsV1beta1ServiceReference service;

  public static final String SERIALIZED_NAME_URL = "url";

  @SerializedName(SERIALIZED_NAME_URL)
  private String url;

  public ApiextensionsV1beta1WebhookClientConfig caBundle(byte[] caBundle) {

    this.caBundle = caBundle;
    return this;
  }

  /**
   * caBundle is a PEM encoded CA bundle which will be used to validate the webhook&#39;s server
   * certificate. If unspecified, system trust roots on the apiserver are used.
   *
   * @return caBundle
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "caBundle is a PEM encoded CA bundle which will be used to validate the webhook's server certificate. If unspecified, system trust roots on the apiserver are used.")
  public byte[] getCaBundle() {
    return caBundle;
  }

  public void setCaBundle(byte[] caBundle) {
    this.caBundle = caBundle;
  }

  public ApiextensionsV1beta1WebhookClientConfig service(
      ApiextensionsV1beta1ServiceReference service) {

    this.service = service;
    return this;
  }

  /**
   * Get service
   *
   * @return service
   */
  @jakarta.annotation.Nullable
  @Schema(description = "")
  public ApiextensionsV1beta1ServiceReference getService() {
    return service;
  }

  public void setService(ApiextensionsV1beta1ServiceReference service) {
    this.service = service;
  }

  public ApiextensionsV1beta1WebhookClientConfig url(String url) {

    this.url = url;
    return this;
  }

  /**
   * url gives the location of the webhook, in standard URL form
   * (&#x60;scheme://host:port/path&#x60;). Exactly one of &#x60;url&#x60; or &#x60;service&#x60;
   * must be specified. The &#x60;host&#x60; should not refer to a service running in the cluster;
   * use the &#x60;service&#x60; field instead. The host might be resolved via external DNS in some
   * apiservers (e.g., &#x60;kube-apiserver&#x60; cannot resolve in-cluster DNS as that would be a
   * layering violation). &#x60;host&#x60; may also be an IP address. Please note that using
   * &#x60;localhost&#x60; or &#x60;127.0.0.1&#x60; as a &#x60;host&#x60; is risky unless you take
   * great care to run this webhook on all hosts which run an apiserver which might need to make
   * calls to this webhook. Such installs are likely to be non-portable, i.e., not easy to turn up
   * in a new cluster. The scheme must be \&quot;https\&quot;; the URL must begin with
   * \&quot;https://\&quot;. A path is optional, and if present may be any string permissible in a
   * URL. You may use the path to pass an arbitrary string to the webhook, for example, a cluster
   * identifier. Attempting to use a user or basic auth e.g. \&quot;user:password@\&quot; is not
   * allowed. Fragments (\&quot;#...\&quot;) and query parameters (\&quot;?...\&quot;) are not
   * allowed, either.
   *
   * @return url
   */
  @jakarta.annotation.Nullable
  @Schema(
      description =
          "url gives the location of the webhook, in standard URL form (`scheme://host:port/path`). Exactly one of `url` or `service` must be specified.  The `host` should not refer to a service running in the cluster; use the `service` field instead. The host might be resolved via external DNS in some apiservers (e.g., `kube-apiserver` cannot resolve in-cluster DNS as that would be a layering violation). `host` may also be an IP address.  Please note that using `localhost` or `127.0.0.1` as a `host` is risky unless you take great care to run this webhook on all hosts which run an apiserver which might need to make calls to this webhook. Such installs are likely to be non-portable, i.e., not easy to turn up in a new cluster.  The scheme must be 'https'; the URL must begin with 'https://'.  A path is optional, and if present may be any string permissible in a URL. You may use the path to pass an arbitrary string to the webhook, for example, a cluster identifier.  Attempting to use a user or basic auth e.g. 'user:password@' is not allowed. Fragments ('#...') and query parameters ('?...') are not allowed, either.")
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiextensionsV1beta1WebhookClientConfig apiextensionsV1beta1WebhookClientConfig =
        (ApiextensionsV1beta1WebhookClientConfig) o;
    return Arrays.equals(this.caBundle, apiextensionsV1beta1WebhookClientConfig.caBundle)
        && Objects.equals(this.service, apiextensionsV1beta1WebhookClientConfig.service)
        && Objects.equals(this.url, apiextensionsV1beta1WebhookClientConfig.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(caBundle), service, url);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiextensionsV1beta1WebhookClientConfig {\n");
    sb.append("    caBundle: ").append(toIndentedString(caBundle)).append("\n");
    sb.append("    service: ").append(toIndentedString(service)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
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
