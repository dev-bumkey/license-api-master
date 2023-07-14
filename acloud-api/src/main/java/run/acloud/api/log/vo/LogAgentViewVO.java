package run.acloud.api.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "LogAgentViewVO", title= "LogAgentViewVO", description = "로그 에이전트 뷰 모델")
public class LogAgentViewVO extends HasUseYnVO {
    @Schema(title = "계정 번호", description = "시스템의 계정번호")
    private Integer accountSeq;
    @Schema(title = "에이전트 순번")
    private Integer agentSeq;
    @Schema(title = "에이전트 이름")
    private String agentName;
    @Schema(title = "에이전트 설명", description = "로그 에이전트 설명으로 필수값 아님")
    private String agentDescription;
    @Schema(title = "클러스터 순번", description = "로그 에이전트가 배포될 cluster")
    private Integer clusterSeq;
    @Schema(title = "네임스페이스 명", description = "네임스페이스명")
    private String namespace;
    @NotBlank
    @Schema(title = "어플리케이션 이름")
    private String applicationName;
    // deploy configs...
    @Schema(title = "log level", description = "로그 에이전트 log level")
    private String logLevel;
    @Schema(title = "cpu request", description = "cpu request")
    private Integer cpuRequest;
    @Schema(title = "cpu limit", description = "cpu limit")
    private Integer cpuLimit;
    @Schema(title = "memory request", description = "memory request")
    private Integer memoryRequest;
    @Schema(title = "memory limit", description = "memory limit")
    private Integer memoryLimit;
    @Schema(title = "affinity", description = "affinity")
    private String affinity;
    @Schema(title = "tolerations", description = "tolerations")
    private String tolerations;
    @Schema(title = "node selector", description = "node selector")
    private String nodeSelector;
    @NotBlank
    @Schema(title = "host path", description = "host path")
    private String hostPath;
    @NotBlank
    @Schema(title = "agent config", description = "agent config")
    private String agentConfig;
    @Schema(title = "parser config", description = "parser config")
    private String parserConfig;
    @Schema(title = "log labels", description = "log에 추가 될 레이블 목록")
    private Map<String, String> lokiCustomLabels;
}
