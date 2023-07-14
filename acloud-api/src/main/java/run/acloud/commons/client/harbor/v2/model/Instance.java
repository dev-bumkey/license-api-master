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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Instance
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class Instance {
  @SerializedName("id")
  private Integer id = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("description")
  private String description = null;

  @SerializedName("vendor")
  private String vendor = null;

  @SerializedName("endpoint")
  private String endpoint = null;

  @SerializedName("auth_mode")
  private String authMode = null;

  @SerializedName("auth_info")
  private Map<String, String> authInfo = null;

  @SerializedName("status")
  private String status = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("default")
  private Boolean _default = null;

  @SerializedName("insecure")
  private Boolean insecure = null;

  @SerializedName("setup_timestamp")
  private Long setupTimestamp = null;

  public Instance id(Integer id) {
    this.id = id;
    return this;
  }

   /**
   * Unique ID
   * @return id
  **/
  @Schema(description = "Unique ID")
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Instance name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Instance name
   * @return name
  **/
  @Schema(description = "Instance name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Instance description(String description) {
    this.description = description;
    return this;
  }

   /**
   * Description of instance
   * @return description
  **/
  @Schema(description = "Description of instance")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Instance vendor(String vendor) {
    this.vendor = vendor;
    return this;
  }

   /**
   * Based on which driver, identified by ID
   * @return vendor
  **/
  @Schema(description = "Based on which driver, identified by ID")
  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public Instance endpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

   /**
   * The service endpoint of this instance
   * @return endpoint
  **/
  @Schema(description = "The service endpoint of this instance")
  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Instance authMode(String authMode) {
    this.authMode = authMode;
    return this;
  }

   /**
   * The authentication way supported
   * @return authMode
  **/
  @Schema(description = "The authentication way supported")
  public String getAuthMode() {
    return authMode;
  }

  public void setAuthMode(String authMode) {
    this.authMode = authMode;
  }

  public Instance authInfo(Map<String, String> authInfo) {
    this.authInfo = authInfo;
    return this;
  }

  public Instance putAuthInfoItem(String key, String authInfoItem) {
    if (this.authInfo == null) {
      this.authInfo = new HashMap<String, String>();
    }
    this.authInfo.put(key, authInfoItem);
    return this;
  }

   /**
   * The auth credential data if exists
   * @return authInfo
  **/
  @Schema(description = "The auth credential data if exists")
  public Map<String, String> getAuthInfo() {
    return authInfo;
  }

  public void setAuthInfo(Map<String, String> authInfo) {
    this.authInfo = authInfo;
  }

  public Instance status(String status) {
    this.status = status;
    return this;
  }

   /**
   * The health status
   * @return status
  **/
  @Schema(description = "The health status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instance enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Whether the instance is activated or not
   * @return enabled
  **/
  @Schema(description = "Whether the instance is activated or not")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Instance _default(Boolean _default) {
    this._default = _default;
    return this;
  }

   /**
   * Whether the instance is default or not
   * @return _default
  **/
  @Schema(description = "Whether the instance is default or not")
  public Boolean isDefault() {
    return _default;
  }

  public void setDefault(Boolean _default) {
    this._default = _default;
  }

  public Instance insecure(Boolean insecure) {
    this.insecure = insecure;
    return this;
  }

   /**
   * Whether the instance endpoint is insecure or not
   * @return insecure
  **/
  @Schema(description = "Whether the instance endpoint is insecure or not")
  public Boolean isInsecure() {
    return insecure;
  }

  public void setInsecure(Boolean insecure) {
    this.insecure = insecure;
  }

  public Instance setupTimestamp(Long setupTimestamp) {
    this.setupTimestamp = setupTimestamp;
    return this;
  }

   /**
   * The timestamp of instance setting up
   * @return setupTimestamp
  **/
  @Schema(description = "The timestamp of instance setting up")
  public Long getSetupTimestamp() {
    return setupTimestamp;
  }

  public void setSetupTimestamp(Long setupTimestamp) {
    this.setupTimestamp = setupTimestamp;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Instance instance = (Instance) o;
    return Objects.equals(this.id, instance.id) &&
        Objects.equals(this.name, instance.name) &&
        Objects.equals(this.description, instance.description) &&
        Objects.equals(this.vendor, instance.vendor) &&
        Objects.equals(this.endpoint, instance.endpoint) &&
        Objects.equals(this.authMode, instance.authMode) &&
        Objects.equals(this.authInfo, instance.authInfo) &&
        Objects.equals(this.status, instance.status) &&
        Objects.equals(this.enabled, instance.enabled) &&
        Objects.equals(this._default, instance._default) &&
        Objects.equals(this.insecure, instance.insecure) &&
        Objects.equals(this.setupTimestamp, instance.setupTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, vendor, endpoint, authMode, authInfo, status, enabled, _default, insecure, setupTimestamp);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Instance {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    vendor: ").append(toIndentedString(vendor)).append("\n");
    sb.append("    endpoint: ").append(toIndentedString(endpoint)).append("\n");
    sb.append("    authMode: ").append(toIndentedString(authMode)).append("\n");
    sb.append("    authInfo: ").append(toIndentedString(authInfo)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    _default: ").append(toIndentedString(_default)).append("\n");
    sb.append("    insecure: ").append(toIndentedString(insecure)).append("\n");
    sb.append("    setupTimestamp: ").append(toIndentedString(setupTimestamp)).append("\n");
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
