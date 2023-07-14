package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.RunState;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "빌드단계 실행 모델")
public class BuildRunLogVO extends HasUseYnVO {
    private Integer buildRunSeq;
    private RunState runState;
    private Integer buildSeq;
    private Integer registryProjectId;
    private Integer externalRegistrySeq;
    private String  buildServerHost;


    private boolean dbLog = false;
    private List<BuildStepRunVO> buildRunLogs;

}

