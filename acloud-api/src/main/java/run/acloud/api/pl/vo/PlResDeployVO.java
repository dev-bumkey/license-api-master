package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.pl.enums.PlResType;
import run.acloud.commons.vo.HasUpserterVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "파이프라인 배포 리소스 모델")
public class PlResDeployVO extends HasUpserterVO {

    @Schema(title = "파이프라인 리소스 배포 번호")
    private Integer plResDeploySeq;

    @Schema(title = "파이프라인 번호")
    private Integer plSeq;

    @Schema(title = "리소스 타입 : REPLICA_SERVER, STATEFUL_SET_SERVER, DAEMON_SET_SERVER, JOB_SERVER, CRON_JOB_SERVER, SVC, CM, SC, PV, PVC, IG")
    private PlResType resType;

    @Schema(title = "리소스 상세 유형", description = "조회용도")
    private String resDetailType;

    @Schema(title = "리소스 이름")
    private String resName;

    @Schema(title = "리소스 컨텐츠")
    private String resCont;

    @Schema(title = "실행 순서")
    private int runOrder;

    @Schema(title = "실행 여부")
    private String runYn;

    @Schema(title = "빌드 목록")
    private List<PlResBuildVO> plResBuilds;

    @Schema(title = "클러스터 번호")
    private Integer clusterSeq;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "워크로드 GUI 모델", description = "상세 조회용")
    private ServerGuiVO workloadConfig;
}
