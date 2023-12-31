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


@Schema(description = "The item in CVE whitelist")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class CVEWhitelistItem {
  @SerializedName("cve_id")
  private String cveId = null;

  public CVEWhitelistItem cveId(String cveId) {
    this.cveId = cveId;
    return this;
  }


  @Schema(description = "The ID of the CVE, such as 'CVE-2019-10164'")
  public String getCveId() {
    return cveId;
  }

  public void setCveId(String cveId) {
    this.cveId = cveId;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CVEWhitelistItem cvEWhitelistItem = (CVEWhitelistItem) o;
    return Objects.equals(this.cveId, cvEWhitelistItem.cveId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cveId);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CVEWhitelistItem {\n");
    
    sb.append("    cveId: ").append(toIndentedString(cveId)).append("\n");
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

