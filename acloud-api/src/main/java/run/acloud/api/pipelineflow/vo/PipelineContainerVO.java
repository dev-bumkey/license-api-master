package run.acloud.api.pipelineflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.pipelineflow.enums.PipelineContainerAction;
import run.acloud.api.pipelineflow.enums.PipelineContainerState;
import run.acloud.api.pipelineflow.enums.PipelineRunState;
import run.acloud.api.pipelineflow.enums.PipelineType;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

/**
 * @author: coolingi@acornsoft.io
 * Created on 2019. 9. 17.
 */
@Data
@Schema(description = "파이프라인 Container 모델")
public class PipelineContainerVO extends HasUseYnVO {

    @Schema(title = "파이프라인 Container 번호", description = "생성시에는 존재 안함, 파이프라인 Container 번호", example = "1")
    private Integer pipelineContainerSeq;

    @Schema(title = "파이프라인 workload 번호", description = "생성시에는 존재 안함, 수정시 해당 파이프라인 workload 번호", example = "1")
    private Integer pipelineWorkloadSeq;

    @Schema(title = "파이프라인 업무 종류", allowableValues = {"BUILD_DEPLOY"," PUBLIC_DEPLOY"}, example = "BUILD_DEPLOY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private PipelineType pipelineType;

    @Schema(title = "container 이름", example = "mysql-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String containerName;

    @Schema(title = "빌드 번호", description = "선택된 이미지가 빌드되는 buildSeq", example = "167")
    private Integer buildSeq;

    @Schema(title = "빌드 실행 번호", description = "빌드가 실행되는 buildRunSeq", example = "1")
    private Integer buildRunSeq;

    @Schema(title = "빌드 실행 comments", description = "빌드가 실행시 입력한 comments, run desc", example = "desc")
    private String buildRunDesc;

    @Schema(title = "빌드 Tag Name", description = "빌드가 실행시 사용한 TagName, build run tag name", example = "0.0.1")
    private String buildTagName;

    @Schema(title = "빌드 상태", description = "빌드가 실행되는 동안의 상태", example = "CREATED")
    private PipelineRunState buildState;

    @Schema(title = "빌드 레지스트리 번호", description = "e.g) harbor projectId", example = "1")
    private Integer buildRegistrySeq;

    @Schema(title = "빌드 레지스트리 이름", description = "e.g) harbor projectName", example = "library")
    private String buildRegistryName;

    @Schema(title = "빌드 이미지명", example = "pipeline-build-test")
    private String buildImageName;

    @Schema(title = "빌드 이미지 태그", example = "2.1.6-test.B000001")
    private String buildImageTag;

    @Schema(title = "빌드 이미지 URL", example = "BUILD_DEPLOY 일때만 존재, regi.acloud.run/cocktail-dev-qa/build-nginx:1.B000021")
    private String buildImageUrl;

    @Schema(title = "배포 이미지의 빌드 실행 번호", description = "배포된 이미지의 빌드실행 번호", example = "1")
    private Integer deployBuildRunSeq;

    @Schema(title = "빌드 실행 comments", description = "빌드가 실행시 입력한 comments, run desc", example = "desc")
    private String deployBuildRunDesc;

    @Schema(title = "배포 상태", description = "배포가 실행되는 동안의 상태", example = "CREATED")
    private PipelineRunState deployState;

    @Schema(title = "배포 레지스트리 번호", description = "e.g) harbor projectId", example = "1")
    private Integer deployRegistrySeq;

    @Schema(title = "배포 레지스트리 이름", description = "e.g) harbor projectName", example = "library")
    private String deployRegistryName;

    @Schema(title = "배포 이미지명", example = "pipeline-build-test", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String deployImageName;

    @Schema(title = "배포 이미지 태그", example = "2.1.6-test.B000001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String deployImageTag;

    @Schema(title = "배포 이미지 URL", example = "BUILD_DEPLOY 일때만 존재, regi.acloud.run/cocktail-dev-qa/build-nginx:1.B000021")
    private String deployImageUrl;

    @Schema(title = "컨테이너 상태")
    private PipelineContainerState containerState;

    // 아래 세개의 필드는 동일한 이미지URL의 배포된 빌드와 최신 빌드가 다를 경우만 셋팅된다.
    @Schema(title = "이미지 업데이터 대상여부")
    private boolean isUpdateTarget=false;   // 이미지 업데이터 대상여부, true : 이미지 update 대상, false : 이미지 update 대상아님
    private BuildRunVO deployBuildRun;      // 배포된 빌드 Run 정보
    private BuildRunVO latestBuildRun;      // 배포된 빌드와 동일한 이미지URL의 최근 빌드 정보

    private List<PipelineContainerAction> possibleActions;

    public void setContainerState(PipelineContainerState containerState){
        if(containerState != null) {
            this.possibleActions = containerState.possibleActions(getPipelineType());
            this.containerState = containerState;
        }
    }

}

