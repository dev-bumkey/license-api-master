package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "파이프라인 리소스 빌드와 리소스 배포 간의 맵핑 테이블")
public class PlResBuildDeployMappingVO extends BaseVO {

    @Schema(title = "파이프라인 리소스 빌드 번호")
    private Integer plResBuildSeq;

    @Schema(title = "파이프라인 리소스 배포 번호")
    private Integer plResDeploySeq;

    @Schema(title = "리소스 타입")
    private String resType;

    @Schema(title = "리소스 이름")
    private String resName;

    @Schema(title = "컨테이너 이름")
    private String containerName;
}