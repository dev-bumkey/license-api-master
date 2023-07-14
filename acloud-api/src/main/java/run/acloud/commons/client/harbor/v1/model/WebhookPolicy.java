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


@Schema(description = "The webhook policy object")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class WebhookPolicy {
  @SerializedName("id")
  private Long id = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("description")
  private String description = null;

  @SerializedName("project_id")
  private Integer projectId = null;

  @SerializedName("targets")
  private List<WebhookTargetObject> targets = null;

  @SerializedName("event_types")
  private List<String> eventTypes = null;

  @SerializedName("creator")
  private String creator = null;

  @SerializedName("creation_time")
  private String creationTime = null;

  @SerializedName("update_time")
  private String updateTime = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  public WebhookPolicy id(Long id) {
    this.id = id;
    return this;
  }


  @Schema(description = "The webhook policy ID.")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public WebhookPolicy name(String name) {
    this.name = name;
    return this;
  }


  @Schema(description = "The name of webhook policy.")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WebhookPolicy description(String description) {
    this.description = description;
    return this;
  }


  @Schema(description = "The description of webhook policy.")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public WebhookPolicy projectId(Integer projectId) {
    this.projectId = projectId;
    return this;
  }


  @Schema(description = "The project ID of webhook policy.")
  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public WebhookPolicy targets(List<WebhookTargetObject> targets) {
    this.targets = targets;
    return this;
  }

  public WebhookPolicy addTargetsItem(WebhookTargetObject targetsItem) {
    if (this.targets == null) {
      this.targets = new ArrayList<WebhookTargetObject>();
    }
    this.targets.add(targetsItem);
    return this;
  }


  @Schema(description = "")
  public List<WebhookTargetObject> getTargets() {
    return targets;
  }

  public void setTargets(List<WebhookTargetObject> targets) {
    this.targets = targets;
  }

  public WebhookPolicy eventTypes(List<String> eventTypes) {
    this.eventTypes = eventTypes;
    return this;
  }

  public WebhookPolicy addEventTypesItem(String eventTypesItem) {
    if (this.eventTypes == null) {
      this.eventTypes = new ArrayList<String>();
    }
    this.eventTypes.add(eventTypesItem);
    return this;
  }


  @Schema(description = "")
  public List<String> getEventTypes() {
    return eventTypes;
  }

  public void setEventTypes(List<String> eventTypes) {
    this.eventTypes = eventTypes;
  }

  public WebhookPolicy creator(String creator) {
    this.creator = creator;
    return this;
  }


  @Schema(description = "The creator of the webhook policy.")
  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public WebhookPolicy creationTime(String creationTime) {
    this.creationTime = creationTime;
    return this;
  }


  @Schema(description = "The create time of the webhook policy.")
  public String getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }

  public WebhookPolicy updateTime(String updateTime) {
    this.updateTime = updateTime;
    return this;
  }


  @Schema(description = "The update time of the webhook policy.")
  public String getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(String updateTime) {
    this.updateTime = updateTime;
  }

  public WebhookPolicy enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }


  @Schema(description = "Whether the webhook policy is enabled or not.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookPolicy webhookPolicy = (WebhookPolicy) o;
    return Objects.equals(this.id, webhookPolicy.id) &&
        Objects.equals(this.name, webhookPolicy.name) &&
        Objects.equals(this.description, webhookPolicy.description) &&
        Objects.equals(this.projectId, webhookPolicy.projectId) &&
        Objects.equals(this.targets, webhookPolicy.targets) &&
        Objects.equals(this.eventTypes, webhookPolicy.eventTypes) &&
        Objects.equals(this.creator, webhookPolicy.creator) &&
        Objects.equals(this.creationTime, webhookPolicy.creationTime) &&
        Objects.equals(this.updateTime, webhookPolicy.updateTime) &&
        Objects.equals(this.enabled, webhookPolicy.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, projectId, targets, eventTypes, creator, creationTime, updateTime, enabled);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebhookPolicy {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    targets: ").append(toIndentedString(targets)).append("\n");
    sb.append("    eventTypes: ").append(toIndentedString(eventTypes)).append("\n");
    sb.append("    creator: ").append(toIndentedString(creator)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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
