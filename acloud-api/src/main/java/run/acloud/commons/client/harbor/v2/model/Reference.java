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
 * Reference
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class Reference {
  @SerializedName("parent_id")
  private Long parentId = null;

  @SerializedName("child_id")
  private Long childId = null;

  @SerializedName("child_digest")
  private String childDigest = null;

  @SerializedName("platform")
  private Platform platform = null;

  @SerializedName("annotations")
  private Annotations annotations = null;

  @SerializedName("urls")
  private List<String> urls = null;

  public Reference parentId(Long parentId) {
    this.parentId = parentId;
    return this;
  }

   /**
   * The parent ID of the reference
   * @return parentId
  **/
  @Schema(description = "The parent ID of the reference")
  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Reference childId(Long childId) {
    this.childId = childId;
    return this;
  }

   /**
   * The child ID of the reference
   * @return childId
  **/
  @Schema(description = "The child ID of the reference")
  public Long getChildId() {
    return childId;
  }

  public void setChildId(Long childId) {
    this.childId = childId;
  }

  public Reference childDigest(String childDigest) {
    this.childDigest = childDigest;
    return this;
  }

   /**
   * The digest of the child artifact
   * @return childDigest
  **/
  @Schema(description = "The digest of the child artifact")
  public String getChildDigest() {
    return childDigest;
  }

  public void setChildDigest(String childDigest) {
    this.childDigest = childDigest;
  }

  public Reference platform(Platform platform) {
    this.platform = platform;
    return this;
  }

   /**
   * Get platform
   * @return platform
  **/
  @Schema(description = "")
  public Platform getPlatform() {
    return platform;
  }

  public void setPlatform(Platform platform) {
    this.platform = platform;
  }

  public Reference annotations(Annotations annotations) {
    this.annotations = annotations;
    return this;
  }

   /**
   * Get annotations
   * @return annotations
  **/
  @Schema(description = "")
  public Annotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Annotations annotations) {
    this.annotations = annotations;
  }

  public Reference urls(List<String> urls) {
    this.urls = urls;
    return this;
  }

  public Reference addUrlsItem(String urlsItem) {
    if (this.urls == null) {
      this.urls = new ArrayList<String>();
    }
    this.urls.add(urlsItem);
    return this;
  }

   /**
   * The download URLs
   * @return urls
  **/
  @Schema(description = "The download URLs")
  public List<String> getUrls() {
    return urls;
  }

  public void setUrls(List<String> urls) {
    this.urls = urls;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Reference reference = (Reference) o;
    return Objects.equals(this.parentId, reference.parentId) &&
        Objects.equals(this.childId, reference.childId) &&
        Objects.equals(this.childDigest, reference.childDigest) &&
        Objects.equals(this.platform, reference.platform) &&
        Objects.equals(this.annotations, reference.annotations) &&
        Objects.equals(this.urls, reference.urls);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentId, childId, childDigest, platform, annotations, urls);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Reference {\n");
    
    sb.append("    parentId: ").append(toIndentedString(parentId)).append("\n");
    sb.append("    childId: ").append(toIndentedString(childId)).append("\n");
    sb.append("    childDigest: ").append(toIndentedString(childDigest)).append("\n");
    sb.append("    platform: ").append(toIndentedString(platform)).append("\n");
    sb.append("    annotations: ").append(toIndentedString(annotations)).append("\n");
    sb.append("    urls: ").append(toIndentedString(urls)).append("\n");
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

