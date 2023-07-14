package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Schema(
    description =
        "HPAScalingPolicy is a single policy which must hold true for a specified past interval.")
@Getter
@Setter
public class K8sHorizontalPodAutoscalerScalingPolicyVO {
  public static final String SERIALIZED_NAME_PERIOD_SECONDS = "periodSeconds";

  /**
   * PeriodSeconds specifies the window of time for which the policy should hold true. PeriodSeconds
   * must be greater than zero and less than or equal to 1800 (30 min).
   *
   * @return periodSeconds
   */
  @Schema(
          requiredMode = Schema.RequiredMode.REQUIRED,
          description =
                  "PeriodSeconds specifies the window of time for which the policy should hold true. PeriodSeconds must be greater than zero and less than or equal to 1800 (30 min).")
  @SerializedName(SERIALIZED_NAME_PERIOD_SECONDS)
  private Integer periodSeconds;

  public static final String SERIALIZED_NAME_TYPE = "type";

  /**
   * Type is used to specify the scaling policy.
   *
   * @return type
   */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type is used to specify the scaling policy.")
  @SerializedName(SERIALIZED_NAME_TYPE)
  private String type;

  public static final String SERIALIZED_NAME_VALUE = "value";

  /**
   * Value contains the amount of change which is permitted by the policy. It must be greater than
   * zero
   *
   * @return value
   */
  @Schema(
          requiredMode = Schema.RequiredMode.REQUIRED,
          description =
                  "Value contains the amount of change which is permitted by the policy. It must be greater than zero")
  @SerializedName(SERIALIZED_NAME_VALUE)
  private Integer value;

  public K8sHorizontalPodAutoscalerScalingPolicyVO periodSeconds(Integer periodSeconds) {

    this.periodSeconds = periodSeconds;
    return this;
  }

  public K8sHorizontalPodAutoscalerScalingPolicyVO type(String type) {

    this.type = type;
    return this;
  }

  public K8sHorizontalPodAutoscalerScalingPolicyVO value(Integer value) {

    this.value = value;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    K8sHorizontalPodAutoscalerScalingPolicyVO v2HPAScalingPolicy = (K8sHorizontalPodAutoscalerScalingPolicyVO) o;
    return Objects.equals(this.periodSeconds, v2HPAScalingPolicy.periodSeconds)
        && Objects.equals(this.type, v2HPAScalingPolicy.type)
        && Objects.equals(this.value, v2HPAScalingPolicy.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(periodSeconds, type, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V2HPAScalingPolicy {\n");
    sb.append("    periodSeconds: ").append(toIndentedString(periodSeconds)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
