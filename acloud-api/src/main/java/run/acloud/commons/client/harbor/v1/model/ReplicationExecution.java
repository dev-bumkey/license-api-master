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


@Schema(description = "The replication execution")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class ReplicationExecution {
  @SerializedName("id")
  private Integer id = null;

  @SerializedName("policy_id")
  private Integer policyId = null;

  @SerializedName("status")
  private String status = null;

  @SerializedName("status_text")
  private String statusText = null;

  @SerializedName("trigger")
  private String trigger = null;

  @SerializedName("total")
  private Integer total = null;

  @SerializedName("failed")
  private Integer failed = null;

  @SerializedName("succeed")
  private Integer succeed = null;

  @SerializedName("in_progress")
  private Integer inProgress = null;

  @SerializedName("stopped")
  private Integer stopped = null;

  @SerializedName("start_time")
  private String startTime = null;

  @SerializedName("end_time")
  private String endTime = null;

  public ReplicationExecution id(Integer id) {
    this.id = id;
    return this;
  }


  @Schema(description = "The ID")
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ReplicationExecution policyId(Integer policyId) {
    this.policyId = policyId;
    return this;
  }


  @Schema(description = "The policy ID")
  public Integer getPolicyId() {
    return policyId;
  }

  public void setPolicyId(Integer policyId) {
    this.policyId = policyId;
  }

  public ReplicationExecution status(String status) {
    this.status = status;
    return this;
  }


  @Schema(description = "The status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public ReplicationExecution statusText(String statusText) {
    this.statusText = statusText;
    return this;
  }


  @Schema(description = "The status text")
  public String getStatusText() {
    return statusText;
  }

  public void setStatusText(String statusText) {
    this.statusText = statusText;
  }

  public ReplicationExecution trigger(String trigger) {
    this.trigger = trigger;
    return this;
  }


  @Schema(description = "The trigger mode")
  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public ReplicationExecution total(Integer total) {
    this.total = total;
    return this;
  }


  @Schema(description = "The total count of all tasks")
  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public ReplicationExecution failed(Integer failed) {
    this.failed = failed;
    return this;
  }


  @Schema(description = "The count of failed tasks")
  public Integer getFailed() {
    return failed;
  }

  public void setFailed(Integer failed) {
    this.failed = failed;
  }

  public ReplicationExecution succeed(Integer succeed) {
    this.succeed = succeed;
    return this;
  }


  @Schema(description = "The count of succeed tasks")
  public Integer getSucceed() {
    return succeed;
  }

  public void setSucceed(Integer succeed) {
    this.succeed = succeed;
  }

  public ReplicationExecution inProgress(Integer inProgress) {
    this.inProgress = inProgress;
    return this;
  }


  @Schema(description = "The count of in_progress tasks")
  public Integer getInProgress() {
    return inProgress;
  }

  public void setInProgress(Integer inProgress) {
    this.inProgress = inProgress;
  }

  public ReplicationExecution stopped(Integer stopped) {
    this.stopped = stopped;
    return this;
  }


  @Schema(description = "The count of stopped tasks")
  public Integer getStopped() {
    return stopped;
  }

  public void setStopped(Integer stopped) {
    this.stopped = stopped;
  }

  public ReplicationExecution startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }


  @Schema(description = "The start time")
  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ReplicationExecution endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }


  @Schema(description = "The end time")
  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReplicationExecution replicationExecution = (ReplicationExecution) o;
    return Objects.equals(this.id, replicationExecution.id) &&
        Objects.equals(this.policyId, replicationExecution.policyId) &&
        Objects.equals(this.status, replicationExecution.status) &&
        Objects.equals(this.statusText, replicationExecution.statusText) &&
        Objects.equals(this.trigger, replicationExecution.trigger) &&
        Objects.equals(this.total, replicationExecution.total) &&
        Objects.equals(this.failed, replicationExecution.failed) &&
        Objects.equals(this.succeed, replicationExecution.succeed) &&
        Objects.equals(this.inProgress, replicationExecution.inProgress) &&
        Objects.equals(this.stopped, replicationExecution.stopped) &&
        Objects.equals(this.startTime, replicationExecution.startTime) &&
        Objects.equals(this.endTime, replicationExecution.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, policyId, status, statusText, trigger, total, failed, succeed, inProgress, stopped, startTime, endTime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReplicationExecution {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    policyId: ").append(toIndentedString(policyId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    statusText: ").append(toIndentedString(statusText)).append("\n");
    sb.append("    trigger: ").append(toIndentedString(trigger)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    failed: ").append(toIndentedString(failed)).append("\n");
    sb.append("    succeed: ").append(toIndentedString(succeed)).append("\n");
    sb.append("    inProgress: ").append(toIndentedString(inProgress)).append("\n");
    sb.append("    stopped: ").append(toIndentedString(stopped)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
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

