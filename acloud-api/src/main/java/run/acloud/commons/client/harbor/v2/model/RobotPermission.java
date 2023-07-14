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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RobotPermission
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class RobotPermission {
  @SerializedName("kind")
  private String kind = null;

  @SerializedName("namespace")
  private String namespace = null;

  @SerializedName("access")
  private List<Access> access = null;

  public RobotPermission kind(String kind) {
    this.kind = kind;
    return this;
  }

   /**
   * The kind of the permission
   * @return kind
  **/
  @Schema(description = "The kind of the permission")
  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public RobotPermission namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

   /**
   * The namespace of the permission
   * @return namespace
  **/
  @Schema(description = "The namespace of the permission")
  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public RobotPermission access(List<Access> access) {
    this.access = access;
    return this;
  }

  public RobotPermission addAccessItem(Access accessItem) {
    if (this.access == null) {
      this.access = new ArrayList<Access>();
    }
    this.access.add(accessItem);
    return this;
  }

   /**
   * Get access
   * @return access
  **/
  @Schema(description = "")
  public List<Access> getAccess() {
    return access;
  }

  public void setAccess(List<Access> access) {
    this.access = access;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RobotPermission robotPermission = (RobotPermission) o;
    return Objects.equals(this.kind, robotPermission.kind) &&
        Objects.equals(this.namespace, robotPermission.namespace) &&
        Objects.equals(this.access, robotPermission.access);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, namespace, access);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RobotPermission {\n");
    
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
    sb.append("    access: ").append(toIndentedString(access)).append("\n");
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
