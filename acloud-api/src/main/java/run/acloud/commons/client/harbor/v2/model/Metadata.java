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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Metadata
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class Metadata {
  @SerializedName("id")
  private String id = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("icon")
  private String icon = null;

  @SerializedName("maintainers")
  private List<String> maintainers = null;

  @SerializedName("version")
  private String version = null;

  @SerializedName("source")
  private String source = null;

  public Metadata id(String id) {
    this.id = id;
    return this;
  }

   /**
   * id
   * @return id
  **/
  @Schema(description = "id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Metadata name(String name) {
    this.name = name;
    return this;
  }

   /**
   * name
   * @return name
  **/
  @Schema(description = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Metadata icon(String icon) {
    this.icon = icon;
    return this;
  }

   /**
   * icon
   * @return icon
  **/
  @Schema(description = "icon")
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public Metadata maintainers(List<String> maintainers) {
    this.maintainers = maintainers;
    return this;
  }

  public Metadata addMaintainersItem(String maintainersItem) {
    if (this.maintainers == null) {
      this.maintainers = new ArrayList<String>();
    }
    this.maintainers.add(maintainersItem);
    return this;
  }

   /**
   * maintainers
   * @return maintainers
  **/
  @Schema(description = "maintainers")
  public List<String> getMaintainers() {
    return maintainers;
  }

  public void setMaintainers(List<String> maintainers) {
    this.maintainers = maintainers;
  }

  public Metadata version(String version) {
    this.version = version;
    return this;
  }

   /**
   * version
   * @return version
  **/
  @Schema(description = "version")
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Metadata source(String source) {
    this.source = source;
    return this;
  }

   /**
   * source
   * @return source
  **/
  @Schema(description = "source")
  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Metadata metadata = (Metadata) o;
    return Objects.equals(this.id, metadata.id) &&
        Objects.equals(this.name, metadata.name) &&
        Objects.equals(this.icon, metadata.icon) &&
        Objects.equals(this.maintainers, metadata.maintainers) &&
        Objects.equals(this.version, metadata.version) &&
        Objects.equals(this.source, metadata.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, icon, maintainers, version, source);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Metadata {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    maintainers: ").append(toIndentedString(maintainers)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
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

