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


@Schema(description = "The quota object")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class Quota {
  @SerializedName("id")
  private Integer id = null;

  @SerializedName("ref")
  private QuotaRefObject ref = null;

  @SerializedName("hard")
  private ResourceList hard = null;

  @SerializedName("used")
  private ResourceList used = null;

  @SerializedName("creation_time")
  private String creationTime = null;

  @SerializedName("update_time")
  private String updateTime = null;

  public Quota id(Integer id) {
    this.id = id;
    return this;
  }


  @Schema(description = "ID of the quota")
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Quota ref(QuotaRefObject ref) {
    this.ref = ref;
    return this;
  }


  @Schema(description = "The reference object of the quota")
  public QuotaRefObject getRef() {
    return ref;
  }

  public void setRef(QuotaRefObject ref) {
    this.ref = ref;
  }

  public Quota hard(ResourceList hard) {
    this.hard = hard;
    return this;
  }


  @Schema(description = "The hard limits of the quota")
  public ResourceList getHard() {
    return hard;
  }

  public void setHard(ResourceList hard) {
    this.hard = hard;
  }

  public Quota used(ResourceList used) {
    this.used = used;
    return this;
  }


  @Schema(description = "The used status of the quota")
  public ResourceList getUsed() {
    return used;
  }

  public void setUsed(ResourceList used) {
    this.used = used;
  }

  public Quota creationTime(String creationTime) {
    this.creationTime = creationTime;
    return this;
  }


  @Schema(description = "the creation time of the quota")
  public String getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }

  public Quota updateTime(String updateTime) {
    this.updateTime = updateTime;
    return this;
  }


  @Schema(description = "the update time of the quota")
  public String getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(String updateTime) {
    this.updateTime = updateTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Quota quota = (Quota) o;
    return Objects.equals(this.id, quota.id) &&
        Objects.equals(this.ref, quota.ref) &&
        Objects.equals(this.hard, quota.hard) &&
        Objects.equals(this.used, quota.used) &&
        Objects.equals(this.creationTime, quota.creationTime) &&
        Objects.equals(this.updateTime, quota.updateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, ref, hard, used, creationTime, updateTime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Quota {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    ref: ").append(toIndentedString(ref)).append("\n");
    sb.append("    hard: ").append(toIndentedString(hard)).append("\n");
    sb.append("    used: ").append(toIndentedString(used)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
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

