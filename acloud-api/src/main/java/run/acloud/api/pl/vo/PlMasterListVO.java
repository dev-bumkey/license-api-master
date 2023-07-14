package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.commons.vo.BaseVO;

import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "파이프라인 마스터 리스트 모델")
public class PlMasterListVO extends BaseVO {

    @Schema(title = "파이프라인 번호")
    private Integer plSeq;

    @Schema(title = "클러스터 번호")
    private Integer clusterSeq;

    @Schema(title = "클러스터 모델")
    private ClusterVO cluster;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "파이프라인 이름")
    private String name;

    @Schema(title = "파이프라인 버전")
    private String ver;

    @Schema(title = "파이프라인 상태")
    private PlStatus status;

    @Schema(title = "파이프라인 최근 실행일시")
    private Date lastRunTime;

    @Schema(title = "파이프라인 릴리즈 버전")
    private String releaseVer;

    @Schema(title = "파이프라인 랄리즈 리소스 갯수")
    private Integer releaseResCount;

    @Schema(title = "파이프라인 릴리즈 실행일시")
    private Date releaseTime;

    @Schema(title = "파이프라인 수정 리소스 갯수")
    private Integer updateResCount;
}
