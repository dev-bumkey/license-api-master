/*
 * Harbor API
 * These APIs provide services for manipulating Harbor project.
 *
 * OpenAPI spec version: 2.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package run.acloud.commons.client.harbor.v2.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * AuthproxySetting
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class AuthproxySetting {
  @SerializedName("endpoint")
  private String endpoint = null;

  @SerializedName("tokenreivew_endpoint")
  private String tokenreivewEndpoint = null;

  @SerializedName("skip_search")
  private Boolean skipSearch = null;

  @SerializedName("verify_cert")
  private Boolean verifyCert = null;

  @SerializedName("server_certificate")
  private String serverCertificate = null;

  public AuthproxySetting endpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

   /**
   * The fully qualified URI of login endpoint of authproxy, such as &#39;https://192.168.1.2:8443/login&#39;
   * @return endpoint
  **/
  @Schema(description = "The fully qualified URI of login endpoint of authproxy, such as 'https://192.168.1.2:8443/login'")
  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public AuthproxySetting tokenreivewEndpoint(String tokenreivewEndpoint) {
    this.tokenreivewEndpoint = tokenreivewEndpoint;
    return this;
  }

   /**
   * The fully qualified URI of token review endpoint of authproxy, such as &#39;https://192.168.1.2:8443/tokenreview&#39;
   * @return tokenreivewEndpoint
  **/
  @Schema(description = "The fully qualified URI of token review endpoint of authproxy, such as 'https://192.168.1.2:8443/tokenreview'")
  public String getTokenreivewEndpoint() {
    return tokenreivewEndpoint;
  }

  public void setTokenreivewEndpoint(String tokenreivewEndpoint) {
    this.tokenreivewEndpoint = tokenreivewEndpoint;
  }

  public AuthproxySetting skipSearch(Boolean skipSearch) {
    this.skipSearch = skipSearch;
    return this;
  }

   /**
   * The flag to determine whether Harbor can skip search the user/group when adding him as a member.
   * @return skipSearch
  **/
  @Schema(description = "The flag to determine whether Harbor can skip search the user/group when adding him as a member.")
  public Boolean isSkipSearch() {
    return skipSearch;
  }

  public void setSkipSearch(Boolean skipSearch) {
    this.skipSearch = skipSearch;
  }

  public AuthproxySetting verifyCert(Boolean verifyCert) {
    this.verifyCert = verifyCert;
    return this;
  }

   /**
   * The flag to determine whether Harbor should verify the certificate when connecting to the auth proxy.
   * @return verifyCert
  **/
  @Schema(description = "The flag to determine whether Harbor should verify the certificate when connecting to the auth proxy.")
  public Boolean isVerifyCert() {
    return verifyCert;
  }

  public void setVerifyCert(Boolean verifyCert) {
    this.verifyCert = verifyCert;
  }

  public AuthproxySetting serverCertificate(String serverCertificate) {
    this.serverCertificate = serverCertificate;
    return this;
  }

   /**
   * The certificate to be pinned when connecting auth proxy.
   * @return serverCertificate
  **/
  @Schema(description = "The certificate to be pinned when connecting auth proxy.")
  public String getServerCertificate() {
    return serverCertificate;
  }

  public void setServerCertificate(String serverCertificate) {
    this.serverCertificate = serverCertificate;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthproxySetting authproxySetting = (AuthproxySetting) o;
    return Objects.equals(this.endpoint, authproxySetting.endpoint) &&
        Objects.equals(this.tokenreivewEndpoint, authproxySetting.tokenreivewEndpoint) &&
        Objects.equals(this.skipSearch, authproxySetting.skipSearch) &&
        Objects.equals(this.verifyCert, authproxySetting.verifyCert) &&
        Objects.equals(this.serverCertificate, authproxySetting.serverCertificate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpoint, tokenreivewEndpoint, skipSearch, verifyCert, serverCertificate);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthproxySetting {\n");
    
    sb.append("    endpoint: ").append(toIndentedString(endpoint)).append("\n");
    sb.append("    tokenreivewEndpoint: ").append(toIndentedString(tokenreivewEndpoint)).append("\n");
    sb.append("    skipSearch: ").append(toIndentedString(skipSearch)).append("\n");
    sb.append("    verifyCert: ").append(toIndentedString(verifyCert)).append("\n");
    sb.append("    serverCertificate: ").append(toIndentedString(serverCertificate)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

