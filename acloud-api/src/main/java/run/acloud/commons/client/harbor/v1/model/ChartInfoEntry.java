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


@Schema(description = "The object contains basic chart information")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class ChartInfoEntry {
  @SerializedName("name")
  private String name = null;

  @SerializedName("total_versions")
  private Integer totalVersions = null;

  @SerializedName("latest_version")
  private String latestVersion = null;

  @SerializedName("created")
  private String created = null;

  @SerializedName("updated")
  private String updated = null;

  @SerializedName("icon")
  private String icon = null;

  @SerializedName("home")
  private String home = null;

  @SerializedName("deprecated")
  private Boolean deprecated = null;

  public ChartInfoEntry name(String name) {
    this.name = name;
    return this;
  }


  @Schema(required = true, description = "Name of chart")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ChartInfoEntry totalVersions(Integer totalVersions) {
    this.totalVersions = totalVersions;
    return this;
  }


  @Schema(required = true, description = "Total count of chart versions")
  public Integer getTotalVersions() {
    return totalVersions;
  }

  public void setTotalVersions(Integer totalVersions) {
    this.totalVersions = totalVersions;
  }

  public ChartInfoEntry latestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
    return this;
  }


  @Schema(description = "latest version of chart")
  public String getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
  }

  public ChartInfoEntry created(String created) {
    this.created = created;
    return this;
  }


  @Schema(required = true, description = "The created time of chart")
  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public ChartInfoEntry updated(String updated) {
    this.updated = updated;
    return this;
  }


  @Schema(description = "The created time of chart")
  public String getUpdated() {
    return updated;
  }

  public void setUpdated(String updated) {
    this.updated = updated;
  }

  public ChartInfoEntry icon(String icon) {
    this.icon = icon;
    return this;
  }


  @Schema(description = "The icon path of chart")
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public ChartInfoEntry home(String home) {
    this.home = home;
    return this;
  }


  @Schema(description = "The home website of chart")
  public String getHome() {
    return home;
  }

  public void setHome(String home) {
    this.home = home;
  }

  public ChartInfoEntry deprecated(Boolean deprecated) {
    this.deprecated = deprecated;
    return this;
  }


  @Schema(description = "Flag to indicate if the chart is deprecated")
  public Boolean isDeprecated() {
    return deprecated;
  }

  public void setDeprecated(Boolean deprecated) {
    this.deprecated = deprecated;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChartInfoEntry chartInfoEntry = (ChartInfoEntry) o;
    return Objects.equals(this.name, chartInfoEntry.name) &&
        Objects.equals(this.totalVersions, chartInfoEntry.totalVersions) &&
        Objects.equals(this.latestVersion, chartInfoEntry.latestVersion) &&
        Objects.equals(this.created, chartInfoEntry.created) &&
        Objects.equals(this.updated, chartInfoEntry.updated) &&
        Objects.equals(this.icon, chartInfoEntry.icon) &&
        Objects.equals(this.home, chartInfoEntry.home) &&
        Objects.equals(this.deprecated, chartInfoEntry.deprecated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, totalVersions, latestVersion, created, updated, icon, home, deprecated);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChartInfoEntry {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    totalVersions: ").append(toIndentedString(totalVersions)).append("\n");
    sb.append("    latestVersion: ").append(toIndentedString(latestVersion)).append("\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    updated: ").append(toIndentedString(updated)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    home: ").append(toIndentedString(home)).append("\n");
    sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n");
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

