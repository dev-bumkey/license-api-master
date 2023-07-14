package run.acloud.api.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(name = "LogAgentAccountMappingAddVO", title = "LogAgentAccountMappingAddVO", description = "에이전트, 시스템 계정 매핑 모델")
public class LogAgentAccountMappingVO extends HasUseYnVO {
    @Schema(title = "계정 번호", description = "시스템의 계정번호")
    private Integer accountSeq;

    @Schema(title = "에이전트 순번")
    private Integer logAgentSeq;
}
