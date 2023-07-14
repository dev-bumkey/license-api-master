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
public class RetentionPolicyScope {
  @SerializedName("level")
  private String level = null;

  @SerializedName("ref")
  private Integer ref = null;

  public RetentionPolicyScope level(String level) {
    this.level = level;
    return this;
  }


  @Schema(description = "")
  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public RetentionPolicyScope ref(Integer ref) {
    this.ref = ref;
    return this;
  }


  @Schema(description = "")
  public Integer getRef() {
    return ref;
  }

  public void setRef(Integer ref) {
    this.ref = ref;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RetentionPolicyScope retentionPolicyScope = (RetentionPolicyScope) o;
    return Objects.equals(this.level, retentionPolicyScope.level) &&
        Objects.equals(this.ref, retentionPolicyScope.ref);
  }

  @Override
  public int hashCode() {
    return Objects.hash(level, ref);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RetentionPolicyScope {\n");
    
    sb.append("    level: ").append(toIndentedString(level)).append("\n");
    sb.append("    ref: ").append(toIndentedString(ref)).append("\n");
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

