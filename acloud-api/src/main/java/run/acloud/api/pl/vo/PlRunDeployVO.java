package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.commons.vo.HasUpserterVO;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "파이프라인 실행 배포 모델")
public class PlRunDeployVO extends HasUpserterVO {

    @Schema(title = "파이프라인 실행 배포 번호")
    private Integer plRunDeploySeq;

    @Schema(title = "파이프라인 실행 번호")
    private Integer plRunSeq;

    @Schema(title = "파이프라인 리소스 배포 번호")
    private Integer plResDeploySeq;

    @Schema(title = "리소스 타입 : REPLICA_SERVER, STATEFUL_SET_SERVER, DAEMON_SET_SERVER, JOB_SERVER, CRON_JOB_SERVER, SVC, CM, SC, PV, PVC, IG")
    private String resType;

    @Schema(title = "리소스 상세 유형", description = "조회용도")
    private String resDetailType;

    @Schema(title = "리소스 이름")
    private String resName;

    @Schema(title = "리소스 컨텐츠")
    private String resCont;

    @Schema(title = "실행 순서")
    private Integer runOrder;

    @Schema(title = "실행 여부")
    private String runYn;

    @Schema(title = "실행 상태 - WAIT, RUNNING, FAILED, DONE")
    private PlStatus runStatus;

    @Schema(title = "실행 로그")
    private String runLog;

    @Schema(title = "시작 시간")
    private Date beginTime;

    @Schema(title = "종료 시간")
    private Date endTime;

    @Schema(title = "작업시간(단위:초) : endTime - beginTime")
    private Integer runTimeBySec;

    @Schema(title = "빌드 실행 목록")
    private List<PlRunBuildVO> plRunBuilds;

    @Schema(title = "workload 배포 실행시 저장용")
    private List<Object> workloadObjects;

    @Schema(title = "워크로드 GUI 모델", description = "상세 조회용")
    private ServerGuiVO workloadConfig;
}
