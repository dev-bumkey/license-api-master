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
 * The style of the resource filter
 */
@Schema(description = "The style of the resource filter")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class RegistryEndpoint {
  @SerializedName("key")
  private String key = null;

  @SerializedName("value")
  private String value = null;

  public RegistryEndpoint key(String key) {
    this.key = key;
    return this;
  }

   /**
   * The endpoint key
   * @return key
  **/
  @Schema(description = "The endpoint key")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public RegistryEndpoint value(String value) {
    this.value = value;
    return this;
  }

   /**
   * The endpoint value
   * @return value
  **/
  @Schema(description = "The endpoint value")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegistryEndpoint registryEndpoint = (RegistryEndpoint) o;
    return Objects.equals(this.key, registryEndpoint.key) &&
        Objects.equals(this.value, registryEndpoint.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegistryEndpoint {\n");
    
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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
