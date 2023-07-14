package run.acloud.api.log.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "LogAgentVO", title= "LogAgentVO", description = "로그 에이전트 모델")
public class LogAgentVO extends LogAgentViewVO {
    @Schema(title = "에이전트 배포 설정 Yaml")
    @JsonIgnore
    private String deployConfig;
    @Schema(title = "클러스터 id")
    private String clusterId;
    @Schema(title = "컨트롤러 명")
    private String controllerName;
    @Schema(title = "토큰")
    @JsonIgnore
    private String token;
    @Schema(title = "애드온 로그 에이전트 여부")
    @JsonIgnore
    private String addonLogAgentYN;
}
