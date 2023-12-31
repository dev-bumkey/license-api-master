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
 * ReplicationFilter
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class ReplicationFilter {
  @SerializedName("type")
  private String type = null;

  @SerializedName("value")
  private Object value = null;

  @SerializedName("decoration")
  private String decoration = null;

  public ReplicationFilter type(String type) {
    this.type = type;
    return this;
  }

   /**
   * The replication policy filter type.
   * @return type
  **/
  @Schema(description = "The replication policy filter type.")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ReplicationFilter value(Object value) {
    this.value = value;
    return this;
  }

   /**
   * The value of replication policy filter.
   * @return value
  **/
  @Schema(description = "The value of replication policy filter.")
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public ReplicationFilter decoration(String decoration) {
    this.decoration = decoration;
    return this;
  }

   /**
   * matches or excludes the result
   * @return decoration
  **/
  @Schema(description = "matches or excludes the result")
  public String getDecoration() {
    return decoration;
  }

  public void setDecoration(String decoration) {
    this.decoration = decoration;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReplicationFilter replicationFilter = (ReplicationFilter) o;
    return Objects.equals(this.type, replicationFilter.type) &&
        Objects.equals(this.value, replicationFilter.value) &&
        Objects.equals(this.decoration, replicationFilter.decoration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, value, decoration);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReplicationFilter {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    decoration: ").append(toIndentedString(decoration)).append("\n");
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

