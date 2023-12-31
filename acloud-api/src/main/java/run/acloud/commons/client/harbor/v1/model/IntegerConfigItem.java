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
public class IntegerConfigItem {
  @SerializedName("value")
  private Integer value = null;

  @SerializedName("editable")
  private Boolean editable = null;

  public IntegerConfigItem value(Integer value) {
    this.value = value;
    return this;
  }


  @Schema(description = "The integer value of current config item")
  public Integer getValue() {
    return value;
  }

  public void setValue(Integer value) {
    this.value = value;
  }

  public IntegerConfigItem editable(Boolean editable) {
    this.editable = editable;
    return this;
  }


  @Schema(description = "The configure item can be updated or not")
  public Boolean isEditable() {
    return editable;
  }

  public void setEditable(Boolean editable) {
    this.editable = editable;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegerConfigItem integerConfigItem = (IntegerConfigItem) o;
    return Objects.equals(this.value, integerConfigItem.value) &&
        Objects.equals(this.editable, integerConfigItem.editable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, editable);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegerConfigItem {\n");
    
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    editable: ").append(toIndentedString(editable)).append("\n");
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

