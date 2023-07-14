package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "파이프라인 마스터 모델")
public class PlMasterVO extends HasUseYnVO {

    @Schema(title = "파이프라인 번호")
    private Integer plSeq;

    @Schema(title = "파이프라인 이름")
    private String name;

    @Schema(title = "클러스터 번호")
    private Integer clusterSeq;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "파이프라인 버전")
    private String ver;

    @Schema(title = "파이프라인 실행 번호")
    private Integer plRunSeq;

    @Schema(title = "파이프라인 상태") // 실행건 있을 경우, '파이프라인 실행 번호'의 실행 상태
    private PlStatus status;

    @Schema(title = "파이프라인 릴리즈 버전") // 정상 배포 되었던 실행건의 최종 버전
    private String releaseVer;

    @Schema(title = "파이프라인 릴리즈 버전의 실행 시퀀스") // 정상 배포 되었던 실행건의 실행시퀀스
    private Integer releasePlRunSeq;

    @Schema(title = "파이프라인 릴리즈 버전의 실행 Note") // 정상 배포 되었던 실행건의 실행 Note
    private String releaseRunNote;

    @Schema(title = "파이프라인 빌드 리소스 목록")
    private List<PlResBuildVO> plResBuilds;

    @Schema(title = "파이프라인 배포 리소스 목록")
    private List<PlResDeployVO> plResDeploys;
}
