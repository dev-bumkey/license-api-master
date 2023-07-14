package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "빌드와 관련된 pipeline 정보 모델")
public class RelatedPipelineInfoVO extends BaseVO {
    // key 정보
    private Integer pipelineContainerSeq;
    private Integer pipelineWorkloadSeq;

    // build 화면 노출정보
    private String namespaceName;
    private String workloadName;
    private String containerName;
    private Integer componentSeq;

    private String useYn;

    // URL 연동정보
    private Integer accountSeq;
    private Integer serviceSeq;
    private Integer appmapSeq;
    private String clusterId;

}
