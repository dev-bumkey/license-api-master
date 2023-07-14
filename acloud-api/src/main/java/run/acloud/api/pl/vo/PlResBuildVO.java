package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.commons.vo.HasUpserterVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "파이프라인 빌드 리소스 모델")
public class PlResBuildVO extends HasUpserterVO {

    @Schema(title = "파이프라인 리소스 빌드 번호")
    private Integer plResBuildSeq;

    @Schema(title = "파이프라인 번호")
    private Integer plSeq;

    @Schema(title = "이미지 URL")
    private String imgUrl;

    @Schema(title = "빌드 번호")
    private Integer buildSeq;

    @Schema(title = "빌드 실행 번호")
    private Integer buildRunSeq;

    @Schema(title = "빌드 태그")
    private String buildTag;

    @Schema(title = "빌드 컨테츠", accessMode = Schema.AccessMode.READ_ONLY)
    private String buildCont;

    @Schema(title = "빌드 설정 정보, buildCont 문자열을 VO로 변환한 필드, 기존 실행했던 BuildRun 정보나 이후 수정 및 추가한 빌드 설정 정보")
    private BuildRunVO buildConfig;

    @Schema(title = "실행 여부")
    private String runYn;

    @Schema(title = "실행 순서")
    private Integer runOrder;

    @Schema(title = "Build-워크로드 리소스 간의 맵핑 정보")
    private List<PlResBuildDeployMappingVO> buildDeployMapping;

}
