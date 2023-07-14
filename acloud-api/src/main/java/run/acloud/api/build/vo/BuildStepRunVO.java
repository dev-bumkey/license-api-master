package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.StepState;
import run.acloud.api.build.enums.StepType;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "빌드단계 실행 모델")
public class BuildStepRunVO extends HasUseYnVO {

    private Integer buildStepRunSeq;

    private Integer buildStepSeq;

    private Integer buildRunSeq;
    
    private StepType stepType;
    
    private String stepConfig;
    
    private StepState stepState;
    
    private Integer stepOrder;

    private String logId;

    private String log;

    private String beginTime;
    private String endTime;
    private Integer runTimeBySec; // 작업시간(단위:초) : endTime - beginTime

    private boolean useFlag;

    private BuildStepAddVO buildStepConfig;

}
