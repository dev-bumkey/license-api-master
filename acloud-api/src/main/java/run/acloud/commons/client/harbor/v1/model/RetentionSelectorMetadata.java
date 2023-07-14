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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Schema(description = "retention selector")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class RetentionSelectorMetadata {
  @SerializedName("display_text")
  private String displayText = null;

  @SerializedName("kind")
  private String kind = null;

  @SerializedName("decorations")
  private List<String> decorations = null;

  public RetentionSelectorMetadata displayText(String displayText) {
    this.displayText = displayText;
    return this;
  }


  @Schema(description = "")
  public String getDisplayText() {
    return displayText;
  }

  public void setDisplayText(String displayText) {
    this.displayText = displayText;
  }

  public RetentionSelectorMetadata kind(String kind) {
    this.kind = kind;
    return this;
  }


  @Schema(description = "")
  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public RetentionSelectorMetadata decorations(List<String> decorations) {
    this.decorations = decorations;
    return this;
  }

  public RetentionSelectorMetadata addDecorationsItem(String decorationsItem) {
    if (this.decorations == null) {
      this.decorations = new ArrayList<String>();
    }
    this.decorations.add(decorationsItem);
    return this;
  }


  @Schema(description = "")
  public List<String> getDecorations() {
    return decorations;
  }

  public void setDecorations(List<String> decorations) {
    this.decorations = decorations;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RetentionSelectorMetadata retentionSelectorMetadata = (RetentionSelectorMetadata) o;
    return Objects.equals(this.displayText, retentionSelectorMetadata.displayText) &&
        Objects.equals(this.kind, retentionSelectorMetadata.kind) &&
        Objects.equals(this.decorations, retentionSelectorMetadata.decorations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayText, kind, decorations);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RetentionSelectorMetadata {\n");
    
    sb.append("    displayText: ").append(toIndentedString(displayText)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    decorations: ").append(toIndentedString(decorations)).append("\n");
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
