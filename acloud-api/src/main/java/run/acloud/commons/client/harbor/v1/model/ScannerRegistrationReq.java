/*
 * Harbor API
 * These APIs provide services for manipulating Harbor project.
 *
 * OpenAPI spec version: 1.10.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package run.acloud.commons.client.harbor.v1.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;


@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class ScannerRegistrationReq {
  @SerializedName("name")
  private String name = null;

  @SerializedName("description")
  private String description = null;

  @SerializedName("url")
  private String url = null;

  @SerializedName("auth")
  private String auth = "";

  @SerializedName("access_credential")
  private String accessCredential = null;

  @SerializedName("skip_certVerify")
  private Boolean skipCertVerify = false;

  @SerializedName("use_internal_addr")
  private Boolean useInternalAddr = false;

  @SerializedName("disabled")
  private Boolean disabled = false;

  public ScannerRegistrationReq name(String name) {
    this.name = name;
    return this;
  }


  @Schema(example = "Clair", description = "The name of this registration")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ScannerRegistrationReq description(String description) {
    this.description = description;
    return this;
  }


  @Schema(example = "A free-to-use tool that scans container images for package vulnerabilities. ", description = "An optional description of this registration.")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ScannerRegistrationReq url(String url) {
    this.url = url;
    return this;
  }


  @Schema(example = "http://harbor-scanner-clair:8080", description = "A base URL of the scanner adapter.")
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public ScannerRegistrationReq auth(String auth) {
    this.auth = auth;
    return this;
  }


  @Schema(example = "Bearer", description = "Specify what authentication approach is adopted for the HTTP communications. Supported types Basic', 'Bearer' and api key header 'X-ScannerAdapter-API-Key' ")
  public String getAuth() {
    return auth;
  }

  public void setAuth(String auth) {
    this.auth = auth;
  }

  public ScannerRegistrationReq accessCredential(String accessCredential) {
    this.accessCredential = accessCredential;
    return this;
  }


  @Schema(example = "Bearer: JWTTOKENGOESHERE", description = "An optional value of the HTTP Authorization header sent with each request to the Scanner Adapter API. ")
  public String getAccessCredential() {
    return accessCredential;
  }

  public void setAccessCredential(String accessCredential) {
    this.accessCredential = accessCredential;
  }

  public ScannerRegistrationReq skipCertVerify(Boolean skipCertVerify) {
    this.skipCertVerify = skipCertVerify;
    return this;
  }


  @Schema(description = "Indicate if skip the certificate verification when sending HTTP requests")
  public Boolean isSkipCertVerify() {
    return skipCertVerify;
  }

  public void setSkipCertVerify(Boolean skipCertVerify) {
    this.skipCertVerify = skipCertVerify;
  }

  public ScannerRegistrationReq useInternalAddr(Boolean useInternalAddr) {
    this.useInternalAddr = useInternalAddr;
    return this;
  }


  @Schema(description = "Indicate whether use internal registry addr for the scanner to pull content or not")
  public Boolean isUseInternalAddr() {
    return useInternalAddr;
  }

  public void setUseInternalAddr(Boolean useInternalAddr) {
    this.useInternalAddr = useInternalAddr;
  }

  public ScannerRegistrationReq disabled(Boolean disabled) {
    this.disabled = disabled;
    return this;
  }


  @Schema(description = "Indicate whether the registration is enabled or not")
  public Boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScannerRegistrationReq scannerRegistrationReq = (ScannerRegistrationReq) o;
    return Objects.equals(this.name, scannerRegistrationReq.name) &&
        Objects.equals(this.description, scannerRegistrationReq.description) &&
        Objects.equals(this.url, scannerRegistrationReq.url) &&
        Objects.equals(this.auth, scannerRegistrationReq.auth) &&
        Objects.equals(this.accessCredential, scannerRegistrationReq.accessCredential) &&
        Objects.equals(this.skipCertVerify, scannerRegistrationReq.skipCertVerify) &&
        Objects.equals(this.useInternalAddr, scannerRegistrationReq.useInternalAddr) &&
        Objects.equals(this.disabled, scannerRegistrationReq.disabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, url, auth, accessCredential, skipCertVerify, useInternalAddr, disabled);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScannerRegistrationReq {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    auth: ").append(toIndentedString(auth)).append("\n");
    sb.append("    accessCredential: ").append(toIndentedString(accessCredential)).append("\n");
    sb.append("    skipCertVerify: ").append(toIndentedString(skipCertVerify)).append("\n");
    sb.append("    useInternalAddr: ").append(toIndentedString(useInternalAddr)).append("\n");
    sb.append("    disabled: ").append(toIndentedString(disabled)).append("\n");
    sb.append("}");
    return sb.toString();
  }


  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
