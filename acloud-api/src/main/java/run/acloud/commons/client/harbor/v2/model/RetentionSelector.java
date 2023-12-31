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
 * RetentionSelector
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class RetentionSelector {
  @SerializedName("kind")
  private String kind = null;

  @SerializedName("decoration")
  private String decoration = null;

  @SerializedName("pattern")
  private String pattern = null;

  @SerializedName("extras")
  private String extras = null;

  public RetentionSelector kind(String kind) {
    this.kind = kind;
    return this;
  }

   /**
   * Get kind
   * @return kind
  **/
  @Schema(description = "")
  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public RetentionSelector decoration(String decoration) {
    this.decoration = decoration;
    return this;
  }

   /**
   * Get decoration
   * @return decoration
  **/
  @Schema(description = "")
  public String getDecoration() {
    return decoration;
  }

  public void setDecoration(String decoration) {
    this.decoration = decoration;
  }

  public RetentionSelector pattern(String pattern) {
    this.pattern = pattern;
    return this;
  }

   /**
   * Get pattern
   * @return pattern
  **/
  @Schema(description = "")
  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public RetentionSelector extras(String extras) {
    this.extras = extras;
    return this;
  }

   /**
   * Get extras
   * @return extras
  **/
  @Schema(description = "")
  public String getExtras() {
    return extras;
  }

  public void setExtras(String extras) {
    this.extras = extras;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RetentionSelector retentionSelector = (RetentionSelector) o;
    return Objects.equals(this.kind, retentionSelector.kind) &&
        Objects.equals(this.decoration, retentionSelector.decoration) &&
        Objects.equals(this.pattern, retentionSelector.pattern) &&
        Objects.equals(this.extras, retentionSelector.extras);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, decoration, pattern, extras);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RetentionSelector {\n");
    
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    decoration: ").append(toIndentedString(decoration)).append("\n");
    sb.append("    pattern: ").append(toIndentedString(pattern)).append("\n");
    sb.append("    extras: ").append(toIndentedString(extras)).append("\n");
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

