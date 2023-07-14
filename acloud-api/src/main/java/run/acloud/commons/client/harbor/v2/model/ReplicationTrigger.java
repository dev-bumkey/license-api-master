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
 * ReplicationTrigger
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class ReplicationTrigger {
  @SerializedName("type")
  private String type = null;

  @SerializedName("trigger_settings")
  private ReplicationTriggerSettings triggerSettings = null;

  public ReplicationTrigger type(String type) {
    this.type = type;
    return this;
  }

   /**
   * The replication policy trigger type. The valid values are manual, event_based and scheduled.
   * @return type
  **/
  @Schema(description = "The replication policy trigger type. The valid values are manual, event_based and scheduled.")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ReplicationTrigger triggerSettings(ReplicationTriggerSettings triggerSettings) {
    this.triggerSettings = triggerSettings;
    return this;
  }

   /**
   * Get triggerSettings
   * @return triggerSettings
  **/
  @Schema(description = "")
  public ReplicationTriggerSettings getTriggerSettings() {
    return triggerSettings;
  }

  public void setTriggerSettings(ReplicationTriggerSettings triggerSettings) {
    this.triggerSettings = triggerSettings;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReplicationTrigger replicationTrigger = (ReplicationTrigger) o;
    return Objects.equals(this.type, replicationTrigger.type) &&
        Objects.equals(this.triggerSettings, replicationTrigger.triggerSettings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, triggerSettings);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReplicationTrigger {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    triggerSettings: ").append(toIndentedString(triggerSettings)).append("\n");
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
