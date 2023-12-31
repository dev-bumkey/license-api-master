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


@Schema(description = "The CVE Whitelist for system or project")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class CVEWhitelist {
  @SerializedName("id")
  private Integer id = null;

  @SerializedName("project_id")
  private Integer projectId = null;

  @SerializedName("expires_at")
  private Integer expiresAt = null;

  @SerializedName("items")
  private List<CVEWhitelistItem> items = null;

  public CVEWhitelist id(Integer id) {
    this.id = id;
    return this;
  }


  @Schema(description = "ID of the whitelist")
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public CVEWhitelist projectId(Integer projectId) {
    this.projectId = projectId;
    return this;
  }


  @Schema(description = "ID of the project which the whitelist belongs to.  For system level whitelist this attribute is zero.")
  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public CVEWhitelist expiresAt(Integer expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }


  @Schema(description = "the time for expiration of the whitelist, in the form of seconds since epoch.  This is an optional attribute, if it's not set the CVE whitelist does not expire.")
  public Integer getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Integer expiresAt) {
    this.expiresAt = expiresAt;
  }

  public CVEWhitelist items(List<CVEWhitelistItem> items) {
    this.items = items;
    return this;
  }

  public CVEWhitelist addItemsItem(CVEWhitelistItem itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<CVEWhitelistItem>();
    }
    this.items.add(itemsItem);
    return this;
  }


  @Schema(description = "")
  public List<CVEWhitelistItem> getItems() {
    return items;
  }

  public void setItems(List<CVEWhitelistItem> items) {
    this.items = items;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CVEWhitelist cvEWhitelist = (CVEWhitelist) o;
    return Objects.equals(this.id, cvEWhitelist.id) &&
        Objects.equals(this.projectId, cvEWhitelist.projectId) &&
        Objects.equals(this.expiresAt, cvEWhitelist.expiresAt) &&
        Objects.equals(this.items, cvEWhitelist.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, projectId, expiresAt, items);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CVEWhitelist {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    expiresAt: ").append(toIndentedString(expiresAt)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

