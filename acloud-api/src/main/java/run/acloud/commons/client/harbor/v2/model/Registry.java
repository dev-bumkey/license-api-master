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
 * Registry
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class Registry {
  @SerializedName("id")
  private Long id = null;

  @SerializedName("url")
  private String url = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("credential")
  private RegistryCredential credential = null;

  @SerializedName("type")
  private String type = null;

  @SerializedName("insecure")
  private Boolean insecure = null;

  @SerializedName("description")
  private String description = null;

  @SerializedName("status")
  private String status = null;

  @SerializedName("creation_time")
  private OffsetDateTime creationTime = null;

  @SerializedName("update_time")
  private OffsetDateTime updateTime = null;

  public Registry id(Long id) {
    this.id = id;
    return this;
  }

   /**
   * The registry ID.
   * @return id
  **/
  @Schema(description = "The registry ID.")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Registry url(String url) {
    this.url = url;
    return this;
  }

   /**
   * The registry URL string.
   * @return url
  **/
  @Schema(description = "The registry URL string.")
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Registry name(String name) {
    this.name = name;
    return this;
  }

   /**
   * The registry name.
   * @return name
  **/
  @Schema(description = "The registry name.")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Registry credential(RegistryCredential credential) {
    this.credential = credential;
    return this;
  }

   /**
   * Get credential
   * @return credential
  **/
  @Schema(description = "")
  public RegistryCredential getCredential() {
    return credential;
  }

  public void setCredential(RegistryCredential credential) {
    this.credential = credential;
  }

  public Registry type(String type) {
    this.type = type;
    return this;
  }

   /**
   * Type of the registry, e.g. &#39;harbor&#39;.
   * @return type
  **/
  @Schema(description = "Type of the registry, e.g. 'harbor'.")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Registry insecure(Boolean insecure) {
    this.insecure = insecure;
    return this;
  }

   /**
   * Whether or not the certificate will be verified when Harbor tries to access the server.
   * @return insecure
  **/
  @Schema(description = "Whether or not the certificate will be verified when Harbor tries to access the server.")
  public Boolean isInsecure() {
    return insecure;
  }

  public void setInsecure(Boolean insecure) {
    this.insecure = insecure;
  }

  public Registry description(String description) {
    this.description = description;
    return this;
  }

   /**
   * Description of the registry.
   * @return description
  **/
  @Schema(description = "Description of the registry.")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Registry status(String status) {
    this.status = status;
    return this;
  }

   /**
   * Health status of the registry.
   * @return status
  **/
  @Schema(description = "Health status of the registry.")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Registry creationTime(OffsetDateTime creationTime) {
    this.creationTime = creationTime;
    return this;
  }

   /**
   * The create time of the policy.
   * @return creationTime
  **/
  @Schema(description = "The create time of the policy.")
  public OffsetDateTime getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(OffsetDateTime creationTime) {
    this.creationTime = creationTime;
  }

  public Registry updateTime(OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

   /**
   * The update time of the policy.
   * @return updateTime
  **/
  @Schema(description = "The update time of the policy.")
  public OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Registry registry = (Registry) o;
    return Objects.equals(this.id, registry.id) &&
        Objects.equals(this.url, registry.url) &&
        Objects.equals(this.name, registry.name) &&
        Objects.equals(this.credential, registry.credential) &&
        Objects.equals(this.type, registry.type) &&
        Objects.equals(this.insecure, registry.insecure) &&
        Objects.equals(this.description, registry.description) &&
        Objects.equals(this.status, registry.status) &&
        Objects.equals(this.creationTime, registry.creationTime) &&
        Objects.equals(this.updateTime, registry.updateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, url, name, credential, type, insecure, description, status, creationTime, updateTime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Registry {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    credential: ").append(toIndentedString(credential)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    insecure: ").append(toIndentedString(insecure)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
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

