package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Schema(
    description =
        "HPAScalingRules configures the scaling behavior for one direction. These Rules are applied after calculating DesiredReplicas from metrics for the HPA. They can limit the scaling velocity by specifying scaling policies. They can prevent flapping by specifying the stabilization window, so that the number of replicas is not set instantly, instead, the safest value from the stabilization window is chosen.")
@Getter
@Setter
public class K8sHorizontalPodAutoscalerScalingRulesVO {
  public static final String SERIALIZED_NAME_POLICIES = "policies";

  /**
   * policies is a list of potential scaling polices which can be used during scaling. At least one
   * policy must be specified, otherwise the HPAScalingRules will be discarded as invalid
   *
   * @return policies
   */
  @jakarta.annotation.Nullable
  @Schema(
          description =
                  "policies is a list of potential scaling polices which can be used during scaling. At least one policy must be specified, otherwise the HPAScalingRules will be discarded as invalid")
  @SerializedName(SERIALIZED_NAME_POLICIES)
  private List<K8sHorizontalPodAutoscalerScalingPolicyVO> policies = null;

  public static final String SERIALIZED_NAME_SELECT_POLICY = "selectPolicy";

  /**
   * selectPolicy is used to specify which policy should be used. If not set, the default value Max
   * is used.
   *
   * @return selectPolicy
   */
  @jakarta.annotation.Nullable
  @Schema(
          description =
                  "selectPolicy is used to specify which policy should be used. If not set, the default value Max is used.")
  @SerializedName(SERIALIZED_NAME_SELECT_POLICY)
  private String selectPolicy;

  public static final String SERIALIZED_NAME_STABILIZATION_WINDOW_SECONDS = "stabilizationWindowSeconds";

  /**
   * StabilizationWindowSeconds is the number of seconds for which past recommendations should be
   * considered while scaling up or scaling down. StabilizationWindowSeconds must be greater than or
   * equal to zero and less than or equal to 3600 (one hour). If not set, use the default values: -
   * For scale up: 0 (i.e. no stabilization is done). - For scale down: 300 (i.e. the stabilization
   * window is 300 seconds long).
   *
   * @return stabilizationWindowSeconds
   */
  @jakarta.annotation.Nullable
  @Schema(
          description =
                  "StabilizationWindowSeconds is the number of seconds for which past recommendations should be considered while scaling up or scaling down. StabilizationWindowSeconds must be greater than or equal to zero and less than or equal to 3600 (one hour). If not set, use the default values: - For scale up: 0 (i.e. no stabilization is done). - For scale down: 300 (i.e. the stabilization window is 300 seconds long).")
  @SerializedName(SERIALIZED_NAME_STABILIZATION_WINDOW_SECONDS)
  private Integer stabilizationWindowSeconds;

  public K8sHorizontalPodAutoscalerScalingRulesVO policies(List<K8sHorizontalPodAutoscalerScalingPolicyVO> policies) {

    this.policies = policies;
    return this;
  }

  public K8sHorizontalPodAutoscalerScalingRulesVO addPoliciesItem(K8sHorizontalPodAutoscalerScalingPolicyVO policiesItem) {
    if (this.policies == null) {
      this.policies = new ArrayList<>();
    }
    this.policies.add(policiesItem);
    return this;
  }

  public K8sHorizontalPodAutoscalerScalingRulesVO selectPolicy(String selectPolicy) {

    this.selectPolicy = selectPolicy;
    return this;
  }

  public K8sHorizontalPodAutoscalerScalingRulesVO stabilizationWindowSeconds(Integer stabilizationWindowSeconds) {

    this.stabilizationWindowSeconds = stabilizationWindowSeconds;
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
    K8sHorizontalPodAutoscalerScalingRulesVO v2HPAScalingRules = (K8sHorizontalPodAutoscalerScalingRulesVO) o;
    return Objects.equals(this.policies, v2HPAScalingRules.policies)
        && Objects.equals(this.selectPolicy, v2HPAScalingRules.selectPolicy)
        && Objects.equals(
            this.stabilizationWindowSeconds, v2HPAScalingRules.stabilizationWindowSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(policies, selectPolicy, stabilizationWindowSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V2HPAScalingRules {\n");
    sb.append("    policies: ").append(toIndentedString(policies)).append("\n");
    sb.append("    selectPolicy: ").append(toIndentedString(selectPolicy)).append("\n");
    sb.append("    stabilizationWindowSeconds: ")
        .append(toIndentedString(stabilizationWindowSeconds))
        .append("\n");
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
