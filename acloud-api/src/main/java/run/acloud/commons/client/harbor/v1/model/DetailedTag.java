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


@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class DetailedTag {
  @SerializedName("digest")
  private String digest = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("size")
  private Long size = null;

  @SerializedName("architecture")
  private String architecture = null;

  @SerializedName("os")
  private String os = null;

  @SerializedName("docker_version")
  private String dockerVersion = null;

  @SerializedName("author")
  private String author = null;

  @SerializedName("created")
  private String created = null;

  @SerializedName("signature")
  private Object signature = null;

  @SerializedName("scan_overview")
  private ScanOverview scanOverview = null;

  @SerializedName("labels")
  private List<Label> labels = null;

  public DetailedTag digest(String digest) {
    this.digest = digest;
    return this;
  }


  @Schema(description = "The digest of the tag.")
  public String getDigest() {
    return digest;
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  public DetailedTag name(String name) {
    this.name = name;
    return this;
  }


  @Schema(description = "The name of the tag.")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DetailedTag size(Long size) {
    this.size = size;
    return this;
  }


  @Schema(description = "The size of the image.")
  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public DetailedTag architecture(String architecture) {
    this.architecture = architecture;
    return this;
  }


  @Schema(description = "The architecture of the image.")
  public String getArchitecture() {
    return architecture;
  }

  public void setArchitecture(String architecture) {
    this.architecture = architecture;
  }

  public DetailedTag os(String os) {
    this.os = os;
    return this;
  }


  @Schema(description = "The os of the image.")
  public String getOs() {
    return os;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public DetailedTag dockerVersion(String dockerVersion) {
    this.dockerVersion = dockerVersion;
    return this;
  }


  @Schema(description = "The version of docker which builds the image.")
  public String getDockerVersion() {
    return dockerVersion;
  }

  public void setDockerVersion(String dockerVersion) {
    this.dockerVersion = dockerVersion;
  }

  public DetailedTag author(String author) {
    this.author = author;
    return this;
  }


  @Schema(description = "The author of the image.")
  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public DetailedTag created(String created) {
    this.created = created;
    return this;
  }


  @Schema(description = "The build time of the image.")
  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public DetailedTag signature(Object signature) {
    this.signature = signature;
    return this;
  }


  @Schema(description = "The signature of image, defined by RepoSignature. If it is null, the image is unsigned.")
  public Object getSignature() {
    return signature;
  }

  public void setSignature(Object signature) {
    this.signature = signature;
  }

  public DetailedTag scanOverview(ScanOverview scanOverview) {
    this.scanOverview = scanOverview;
    return this;
  }


  @Schema(description = "The overview of the scan result.")
  public ScanOverview getScanOverview() {
    return scanOverview;
  }

  public void setScanOverview(ScanOverview scanOverview) {
    this.scanOverview = scanOverview;
  }

  public DetailedTag labels(List<Label> labels) {
    this.labels = labels;
    return this;
  }

  public DetailedTag addLabelsItem(Label labelsItem) {
    if (this.labels == null) {
      this.labels = new ArrayList<Label>();
    }
    this.labels.add(labelsItem);
    return this;
  }


  @Schema(description = "The label list.")
  public List<Label> getLabels() {
    return labels;
  }

  public void setLabels(List<Label> labels) {
    this.labels = labels;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DetailedTag detailedTag = (DetailedTag) o;
    return Objects.equals(this.digest, detailedTag.digest) &&
        Objects.equals(this.name, detailedTag.name) &&
        Objects.equals(this.size, detailedTag.size) &&
        Objects.equals(this.architecture, detailedTag.architecture) &&
        Objects.equals(this.os, detailedTag.os) &&
        Objects.equals(this.dockerVersion, detailedTag.dockerVersion) &&
        Objects.equals(this.author, detailedTag.author) &&
        Objects.equals(this.created, detailedTag.created) &&
        Objects.equals(this.signature, detailedTag.signature) &&
        Objects.equals(this.scanOverview, detailedTag.scanOverview) &&
        Objects.equals(this.labels, detailedTag.labels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(digest, name, size, architecture, os, dockerVersion, author, created, signature, scanOverview, labels);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DetailedTag {\n");
    
    sb.append("    digest: ").append(toIndentedString(digest)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    architecture: ").append(toIndentedString(architecture)).append("\n");
    sb.append("    os: ").append(toIndentedString(os)).append("\n");
    sb.append("    dockerVersion: ").append(toIndentedString(dockerVersion)).append("\n");
    sb.append("    author: ").append(toIndentedString(author)).append("\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    signature: ").append(toIndentedString(signature)).append("\n");
    sb.append("    scanOverview: ").append(toIndentedString(scanOverview)).append("\n");
    sb.append("    labels: ").append(toIndentedString(labels)).append("\n");
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

