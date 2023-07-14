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
import org.threeten.bp.OffsetDateTime;

import java.util.Objects;

/**
 * Registration represents a named configuration for invoking a scanner via its adapter. 
 */
@Schema(description = "Registration represents a named configuration for invoking a scanner via its adapter. ")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class ScannerRegistration {
  @SerializedName("uuid")
  private String uuid = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("description")
  private String description = null;

  @SerializedName("url")
  private String url = null;

  @SerializedName("disabled")
  private Boolean disabled = false;

  @SerializedName("is_default")
  private Boolean isDefault = false;

  @SerializedName("auth")
  private String auth = "";

  @SerializedName("access_credential")
  private String accessCredential = null;

  @SerializedName("skip_certVerify")
  private Boolean skipCertVerify = false;

  @SerializedName("use_internal_addr")
  private Boolean useInternalAddr = false;

  @SerializedName("create_time")
  private OffsetDateTime createTime = null;

  @SerializedName("update_time")
  private OffsetDateTime updateTime = null;

  @SerializedName("adapter")
  private String adapter = null;

  @SerializedName("vendor")
  private String vendor = null;

  @SerializedName("version")
  private String version = null;

  @SerializedName("health")
  private String health = "";

  public ScannerRegistration uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

   /**
   * The unique identifier of this registration.
   * @return uuid
  **/
  @Schema(description = "The unique identifier of this registration.")
  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public ScannerRegistration name(String name) {
    this.name = name;
    return this;
  }

   /**
   * The name of this registration.
   * @return name
  **/
  @Schema(example = "Trivy", description = "The name of this registration.")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ScannerRegistration description(String description) {
    this.description = description;
    return this;
  }

   /**
   * An optional description of this registration.
   * @return description
  **/
  @Schema(example = "A free-to-use tool that scans container images for package vulnerabilities. ", description = "An optional description of this registration.")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ScannerRegistration url(String url) {
    this.url = url;
    return this;
  }

   /**
   * A base URL of the scanner adapter
   * @return url
  **/
  @Schema(example = "http://harbor-scanner-trivy:8080", description = "A base URL of the scanner adapter")
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public ScannerRegistration disabled(Boolean disabled) {
    this.disabled = disabled;
    return this;
  }

   /**
   * Indicate whether the registration is enabled or not
   * @return disabled
  **/
  @Schema(description = "Indicate whether the registration is enabled or not")
  public Boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }

  public ScannerRegistration isDefault(Boolean isDefault) {
    this.isDefault = isDefault;
    return this;
  }

   /**
   * Indicate if the registration is set as the system default one
   * @return isDefault
  **/
  @Schema(description = "Indicate if the registration is set as the system default one")
  public Boolean isIsDefault() {
    return isDefault;
  }

  public void setIsDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }

  public ScannerRegistration auth(String auth) {
    this.auth = auth;
    return this;
  }

   /**
   * Specify what authentication approach is adopted for the HTTP communications. Supported types Basic\&quot;, \&quot;Bearer\&quot; and api key header \&quot;X-ScannerAdapter-API-Key\&quot; 
   * @return auth
  **/
  @Schema(example = "Bearer", description = "Specify what authentication approach is adopted for the HTTP communications. Supported types Basic', 'Bearer' and api key header 'X-ScannerAdapter-API-Key' ")
  public String getAuth() {
    return auth;
  }

  public void setAuth(String auth) {
    this.auth = auth;
  }

  public ScannerRegistration accessCredential(String accessCredential) {
    this.accessCredential = accessCredential;
    return this;
  }

   /**
   * An optional value of the HTTP Authorization header sent with each request to the Scanner Adapter API. 
   * @return accessCredential
  **/
  @Schema(example = "Bearer: JWTTOKENGOESHERE", description = "An optional value of the HTTP Authorization header sent with each request to the Scanner Adapter API. ")
  public String getAccessCredential() {
    return accessCredential;
  }

  public void setAccessCredential(String accessCredential) {
    this.accessCredential = accessCredential;
  }

  public ScannerRegistration skipCertVerify(Boolean skipCertVerify) {
    this.skipCertVerify = skipCertVerify;
    return this;
  }

   /**
   * Indicate if skip the certificate verification when sending HTTP requests
   * @return skipCertVerify
  **/
  @Schema(description = "Indicate if skip the certificate verification when sending HTTP requests")
  public Boolean isSkipCertVerify() {
    return skipCertVerify;
  }

  public void setSkipCertVerify(Boolean skipCertVerify) {
    this.skipCertVerify = skipCertVerify;
  }

  public ScannerRegistration useInternalAddr(Boolean useInternalAddr) {
    this.useInternalAddr = useInternalAddr;
    return this;
  }

   /**
   * Indicate whether use internal registry addr for the scanner to pull content or not
   * @return useInternalAddr
  **/
  @Schema(description = "Indicate whether use internal registry addr for the scanner to pull content or not")
  public Boolean isUseInternalAddr() {
    return useInternalAddr;
  }

  public void setUseInternalAddr(Boolean useInternalAddr) {
    this.useInternalAddr = useInternalAddr;
  }

  public ScannerRegistration createTime(OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

   /**
   * The creation time of this registration
   * @return createTime
  **/
  @Schema(description = "The creation time of this registration")
  public OffsetDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public ScannerRegistration updateTime(OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

   /**
   * The update time of this registration
   * @return updateTime
  **/
  @Schema(description = "The update time of this registration")
  public OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public ScannerRegistration adapter(String adapter) {
    this.adapter = adapter;
    return this;
  }

   /**
   * Optional property to describe the name of the scanner registration
   * @return adapter
  **/
  @Schema(example = "Trivy", description = "Optional property to describe the name of the scanner registration")
  public String getAdapter() {
    return adapter;
  }

  public void setAdapter(String adapter) {
    this.adapter = adapter;
  }

  public ScannerRegistration vendor(String vendor) {
    this.vendor = vendor;
    return this;
  }

   /**
   * Optional property to describe the vendor of the scanner registration
   * @return vendor
  **/
  @Schema(example = "CentOS", description = "Optional property to describe the vendor of the scanner registration")
  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public ScannerRegistration version(String version) {
    this.version = version;
    return this;
  }

   /**
   * Optional property to describe the version of the scanner registration
   * @return version
  **/
  @Schema(example = "1.0.1", description = "Optional property to describe the version of the scanner registration")
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public ScannerRegistration health(String health) {
    this.health = health;
    return this;
  }

   /**
   * Indicate the healthy of the registration
   * @return health
  **/
  @Schema(example = "healthy", description = "Indicate the healthy of the registration")
  public String getHealth() {
    return health;
  }

  public void setHealth(String health) {
    this.health = health;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScannerRegistration scannerRegistration = (ScannerRegistration) o;
    return Objects.equals(this.uuid, scannerRegistration.uuid) &&
        Objects.equals(this.name, scannerRegistration.name) &&
        Objects.equals(this.description, scannerRegistration.description) &&
        Objects.equals(this.url, scannerRegistration.url) &&
        Objects.equals(this.disabled, scannerRegistration.disabled) &&
        Objects.equals(this.isDefault, scannerRegistration.isDefault) &&
        Objects.equals(this.auth, scannerRegistration.auth) &&
        Objects.equals(this.accessCredential, scannerRegistration.accessCredential) &&
        Objects.equals(this.skipCertVerify, scannerRegistration.skipCertVerify) &&
        Objects.equals(this.useInternalAddr, scannerRegistration.useInternalAddr) &&
        Objects.equals(this.createTime, scannerRegistration.createTime) &&
        Objects.equals(this.updateTime, scannerRegistration.updateTime) &&
        Objects.equals(this.adapter, scannerRegistration.adapter) &&
        Objects.equals(this.vendor, scannerRegistration.vendor) &&
        Objects.equals(this.version, scannerRegistration.version) &&
        Objects.equals(this.health, scannerRegistration.health);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, name, description, url, disabled, isDefault, auth, accessCredential, skipCertVerify, useInternalAddr, createTime, updateTime, adapter, vendor, version, health);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScannerRegistration {\n");
    
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    disabled: ").append(toIndentedString(disabled)).append("\n");
    sb.append("    isDefault: ").append(toIndentedString(isDefault)).append("\n");
    sb.append("    auth: ").append(toIndentedString(auth)).append("\n");
    sb.append("    accessCredential: ").append(toIndentedString(accessCredential)).append("\n");
    sb.append("    skipCertVerify: ").append(toIndentedString(skipCertVerify)).append("\n");
    sb.append("    useInternalAddr: ").append(toIndentedString(useInternalAddr)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    adapter: ").append(toIndentedString(adapter)).append("\n");
    sb.append("    vendor: ").append(toIndentedString(vendor)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    health: ").append(toIndentedString(health)).append("\n");
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
