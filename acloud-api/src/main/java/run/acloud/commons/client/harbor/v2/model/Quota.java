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
import org.threeten.bp.OffsetDateTime;

import java.util.Objects;

/**
 * The quota object
 */
@Schema(description = "The quota object")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
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
  private OffsetDateTime creationTime = null;

  @SerializedName("update_time")
  private OffsetDateTime updateTime = null;

  public Quota id(Integer id) {
    this.id = id;
    return this;
  }

   /**
   * ID of the quota
   * @return id
  **/
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

   /**
   * The reference object of the quota
   * @return ref
  **/
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

   /**
   * The hard limits of the quota
   * @return hard
  **/
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

   /**
   * The used status of the quota
   * @return used
  **/
  @Schema(description = "The used status of the quota")
  public ResourceList getUsed() {
    return used;
  }

  public void setUsed(ResourceList used) {
    this.used = used;
  }

  public Quota creationTime(OffsetDateTime creationTime) {
    this.creationTime = creationTime;
    return this;
  }

   /**
   * the creation time of the quota
   * @return creationTime
  **/
  @Schema(description = "the creation time of the quota")
  public OffsetDateTime getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(OffsetDateTime creationTime) {
    this.creationTime = creationTime;
  }

  public Quota updateTime(OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

   /**
   * the update time of the quota
   * @return updateTime
  **/
  @Schema(description = "the update time of the quota")
  public OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
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

