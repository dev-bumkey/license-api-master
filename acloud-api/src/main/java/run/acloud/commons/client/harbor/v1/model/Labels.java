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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Objects;


@Schema(description = "A list of label")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class Labels extends ArrayList<Label> {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Labels {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
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

