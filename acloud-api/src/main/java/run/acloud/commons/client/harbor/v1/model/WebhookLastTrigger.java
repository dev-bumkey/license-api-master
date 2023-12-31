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


@Schema(description = "The webhook policy and last trigger time group by event type.")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class WebhookLastTrigger {
  @SerializedName("event_type")
  private String eventType = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("creation_time")
  private String creationTime = null;

  @SerializedName("last_trigger_time")
  private String lastTriggerTime = null;

  public WebhookLastTrigger eventType(String eventType) {
    this.eventType = eventType;
    return this;
  }


  @Schema(description = "The webhook event type.")
  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public WebhookLastTrigger enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }


  @Schema(description = "Whether or not the webhook policy enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public WebhookLastTrigger creationTime(String creationTime) {
    this.creationTime = creationTime;
    return this;
  }


  @Schema(description = "The creation time of webhook policy.")
  public String getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }

  public WebhookLastTrigger lastTriggerTime(String lastTriggerTime) {
    this.lastTriggerTime = lastTriggerTime;
    return this;
  }


  @Schema(description = "The last trigger time of webhook policy.")
  public String getLastTriggerTime() {
    return lastTriggerTime;
  }

  public void setLastTriggerTime(String lastTriggerTime) {
    this.lastTriggerTime = lastTriggerTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookLastTrigger webhookLastTrigger = (WebhookLastTrigger) o;
    return Objects.equals(this.eventType, webhookLastTrigger.eventType) &&
        Objects.equals(this.enabled, webhookLastTrigger.enabled) &&
        Objects.equals(this.creationTime, webhookLastTrigger.creationTime) &&
        Objects.equals(this.lastTriggerTime, webhookLastTrigger.lastTriggerTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventType, enabled, creationTime, lastTriggerTime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebhookLastTrigger {\n");
    
    sb.append("    eventType: ").append(toIndentedString(eventType)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    lastTriggerTime: ").append(toIndentedString(lastTriggerTime)).append("\n");
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

