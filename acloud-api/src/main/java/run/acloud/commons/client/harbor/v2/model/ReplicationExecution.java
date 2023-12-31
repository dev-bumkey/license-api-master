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
 * The replication execution
 */
@Schema(description = "The replication execution")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class ReplicationExecution {
  @SerializedName("id")
  private Integer id = null;

  @SerializedName("policy_id")
  private Integer policyId = null;

  @SerializedName("status")
  private String status = null;

  @SerializedName("trigger")
  private String trigger = null;

  @SerializedName("start_time")
  private OffsetDateTime startTime = null;

  @SerializedName("end_time")
  private OffsetDateTime endTime = null;

  @SerializedName("status_text")
  private String statusText = null;

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

  public ReplicationExecution id(Integer id) {
    this.id = id;
    return this;
  }

   /**
   * The ID of the execution
   * @return id
  **/
  @Schema(description = "The ID of the execution")
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

   /**
   * The ID if the policy that the execution belongs to
   * @return policyId
  **/
  @Schema(description = "The ID if the policy that the execution belongs to")
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

   /**
   * The status of the execution
   * @return status
  **/
  @Schema(description = "The status of the execution")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public ReplicationExecution trigger(String trigger) {
    this.trigger = trigger;
    return this;
  }

   /**
   * The trigger mode
   * @return trigger
  **/
  @Schema(description = "The trigger mode")
  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public ReplicationExecution startTime(OffsetDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

   /**
   * The start time
   * @return startTime
  **/
  @Schema(description = "The start time")
  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(OffsetDateTime startTime) {
    this.startTime = startTime;
  }

  public ReplicationExecution endTime(OffsetDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

   /**
   * The end time
   * @return endTime
  **/
  @Schema(description = "The end time")
  public OffsetDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(OffsetDateTime endTime) {
    this.endTime = endTime;
  }

  public ReplicationExecution statusText(String statusText) {
    this.statusText = statusText;
    return this;
  }

   /**
   * The status text
   * @return statusText
  **/
  @Schema(description = "The status text")
  public String getStatusText() {
    return statusText;
  }

  public void setStatusText(String statusText) {
    this.statusText = statusText;
  }

  public ReplicationExecution total(Integer total) {
    this.total = total;
    return this;
  }

   /**
   * The total count of all executions
   * @return total
  **/
  @Schema(description = "The total count of all executions")
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

   /**
   * The count of failed executions
   * @return failed
  **/
  @Schema(description = "The count of failed executions")
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

   /**
   * The count of succeed executions
   * @return succeed
  **/
  @Schema(description = "The count of succeed executions")
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

   /**
   * The count of in_progress executions
   * @return inProgress
  **/
  @Schema(description = "The count of in_progress executions")
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

   /**
   * The count of stopped executions
   * @return stopped
  **/
  @Schema(description = "The count of stopped executions")
  public Integer getStopped() {
    return stopped;
  }

  public void setStopped(Integer stopped) {
    this.stopped = stopped;
  }


  @Override
  public boolean equals(java.lang.Object o) {
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
        Objects.equals(this.trigger, replicationExecution.trigger) &&
        Objects.equals(this.startTime, replicationExecution.startTime) &&
        Objects.equals(this.endTime, replicationExecution.endTime) &&
        Objects.equals(this.statusText, replicationExecution.statusText) &&
        Objects.equals(this.total, replicationExecution.total) &&
        Objects.equals(this.failed, replicationExecution.failed) &&
        Objects.equals(this.succeed, replicationExecution.succeed) &&
        Objects.equals(this.inProgress, replicationExecution.inProgress) &&
        Objects.equals(this.stopped, replicationExecution.stopped);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, policyId, status, trigger, startTime, endTime, statusText, total, failed, succeed, inProgress, stopped);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReplicationExecution {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    policyId: ").append(toIndentedString(policyId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    trigger: ").append(toIndentedString(trigger)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    statusText: ").append(toIndentedString(statusText)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    failed: ").append(toIndentedString(failed)).append("\n");
    sb.append("    succeed: ").append(toIndentedString(succeed)).append("\n");
    sb.append("    inProgress: ").append(toIndentedString(inProgress)).append("\n");
    sb.append("    stopped: ").append(toIndentedString(stopped)).append("\n");
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

