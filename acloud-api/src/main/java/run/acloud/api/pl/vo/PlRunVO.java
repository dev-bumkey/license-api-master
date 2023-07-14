package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "파이프라인 실행 모델")
public class PlRunVO extends HasUseYnVO {

    @Schema(title = "파이프라인 실행 번호")
    private Integer plRunSeq;

    @Schema(title = "파이프라인 번호")
    private Integer plSeq;

    @Schema(title = "파이프라인 이름")
    private String plName;

    @Schema(title = "파이프라인 실행 노트")
    private String runNote;

    @Schema(title = "파이프라인 버전")
    private String ver;

    @Schema(title = "파이프라인 실행 상태 - CREATED, RUNNING, ERROR, DONE", description = "PlRunState 참조")
    private PlStatus runStatus;

    @Schema(title = "callbackUrl", description = "실행결과 화면전달용 receive URL")
    private String callbackUrl;

    @Schema(title = "시작 시간")
    private Date beginTime;

    @Schema(title = "종료 시간")
    private Date endTime;

    @Schema(title = "작업시간(단위:초) : endTime - beginTime")
    private Integer runTimeBySec;

    @Schema(title = "Pl 실행 상태 publish subject")
    private String pubSubject;

    @Schema(title = "파이프라인 실행 빌드 목록")
    private List<PlRunBuildVO> plRunBuilds;

    @Schema(title = "파이프라인 실행 배포 목록")
    private List<PlRunDeployVO> plRunDeploys;

    @Schema(title = "실행취소 가능여부")
    private boolean canCancel;

}
