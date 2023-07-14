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
 * AuditLog
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class AuditLog {
  @SerializedName("id")
  private Integer id = null;

  @SerializedName("username")
  private String username = null;

  @SerializedName("resource")
  private String resource = null;

  @SerializedName("resource_type")
  private String resourceType = null;

  @SerializedName("operation")
  private String operation = null;

  @SerializedName("op_time")
  private OffsetDateTime opTime = null;

  public AuditLog id(Integer id) {
    this.id = id;
    return this;
  }

   /**
   * The ID of the audit log entry.
   * @return id
  **/
  @Schema(description = "The ID of the audit log entry.")
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public AuditLog username(String username) {
    this.username = username;
    return this;
  }

   /**
   * Username of the user in this log entry.
   * @return username
  **/
  @Schema(description = "Username of the user in this log entry.")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public AuditLog resource(String resource) {
    this.resource = resource;
    return this;
  }

   /**
   * Name of the repository in this log entry.
   * @return resource
  **/
  @Schema(description = "Name of the repository in this log entry.")
  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public AuditLog resourceType(String resourceType) {
    this.resourceType = resourceType;
    return this;
  }

   /**
   * Tag of the repository in this log entry.
   * @return resourceType
  **/
  @Schema(description = "Tag of the repository in this log entry.")
  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public AuditLog operation(String operation) {
    this.operation = operation;
    return this;
  }

   /**
   * The operation against the repository in this log entry.
   * @return operation
  **/
  @Schema(description = "The operation against the repository in this log entry.")
  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public AuditLog opTime(OffsetDateTime opTime) {
    this.opTime = opTime;
    return this;
  }

   /**
   * The time when this operation is triggered.
   * @return opTime
  **/
  @Schema(example = "2006-01-02T15:04:05Z", description = "The time when this operation is triggered.")
  public OffsetDateTime getOpTime() {
    return opTime;
  }

  public void setOpTime(OffsetDateTime opTime) {
    this.opTime = opTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuditLog auditLog = (AuditLog) o;
    return Objects.equals(this.id, auditLog.id) &&
        Objects.equals(this.username, auditLog.username) &&
        Objects.equals(this.resource, auditLog.resource) &&
        Objects.equals(this.resourceType, auditLog.resourceType) &&
        Objects.equals(this.operation, auditLog.operation) &&
        Objects.equals(this.opTime, auditLog.opTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, resource, resourceType, operation, opTime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuditLog {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    resource: ").append(toIndentedString(resource)).append("\n");
    sb.append("    resourceType: ").append(toIndentedString(resourceType)).append("\n");
    sb.append("    operation: ").append(toIndentedString(operation)).append("\n");
    sb.append("    opTime: ").append(toIndentedString(opTime)).append("\n");
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
