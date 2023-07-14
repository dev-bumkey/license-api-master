package run.acloud.api.build.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.AutoTagSeqType;
import run.acloud.api.build.enums.BuildAction;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.enums.RunType;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Schema(description = "빌드 실행 모델")
public class BuildRunVO extends HasUseYnVO {

    // BuildVO 정보
    private String buildName;
    private String buildDesc;
    private String registryName;
    private String imageName;
    private Integer accountSeq;
    private Integer registryProjectId;

    private Integer externalRegistrySeq;

    // 지정 빌드 서버 정보
    private String buildServerHost;
    private String buildServerTlsVerify;
    private String buildServerCacrt;
    private String buildServerClientCert;
    private String buildServerClientKey;


    private Integer buildRunSeq;
    private Integer buildSeq;
    private Integer buildNo;
    private RunType runType;
    private String runDesc;
    private RunState runState;

    // 자동태그 정보
    private String autotagUseYn;   // autotag_use_yn
    private String autotagPrefix;  // autotag_prefix
    private AutoTagSeqType autotagSeqType; // autotag_seq_type

    private String tagName;
    private String callbackUrl;
    private String imageUrl;
    private Long imageSize;
    private String imageDigest;

    // Pipeline 정보
    private Integer pipelineSeq=0;

    // pipeline 연관 정보
    @JsonIgnore
    private String relatedPipeline;
    private RelatedPipelineInfoVO relatedPipelineInfo;

    // 실행시간
    private Date beginTime;
    private Date endTime;
    private Integer runTimeBySec; // task 작업시간(단위:초) : endTime - beginTime

    private List<BuildStepRunVO> buildStepRuns;

    private List<BuildAction> possibleActions;

    public void setRunState(RunState runState){
        this.possibleActions = runState.possibleActions();
        this.runState = runState;
    }

    @JsonIgnore
    private Integer prevBuildRunSeq;

}
