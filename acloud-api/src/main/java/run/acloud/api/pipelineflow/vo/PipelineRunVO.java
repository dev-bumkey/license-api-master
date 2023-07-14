package run.acloud.api.pipelineflow.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.pipelineflow.enums.PipelineRunState;
import run.acloud.api.pipelineflow.enums.PipelineRunType;
import run.acloud.commons.vo.HasUseYnVO;

/**
 * @author: coolingi@acornsoft.io
 * Created on 2019. 9. 26.
 */
@Getter
@Setter
@Schema(description = "pipeline 실행 모델")
public class PipelineRunVO extends HasUseYnVO {

    @Schema(title = "pipeline 실행 번호", description = "파이프라인 실행 번호", example = "1")
    private Integer pipelineRunSeq;

    @Schema(title = "pipeline Container 번호", description = "생성시에는 수정시 해당 파이프라인 Container 번호", example = "1")
    private Integer pipelineContainerSeq;

    @Schema(title = "pipeline 실행 종류", allowableValues = {"BUILD"," DEPLOY"}, example = "DEPLOY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private PipelineRunType runType;

    @Schema(title = "pipeline 실행 상태", description = "빌드가 실행되는 동안의 상태", example = "CREATED")
    private PipelineRunState runState;

    @Schema(title = "빌드 번호", description = "선택된 이미지가 빌드되는 taskSeq", example = "167")
    private Integer buildSeq;

    @Schema(title = "빌드 실행 번호", description = "빌드가 실행되는 taskRunSeq", example = "1")
    private Integer buildRunSeq;

    @Schema(title = "빌드 상태", description = "빌드가 실행되는 동안의 상태", example = "CREATED")
    private String buildState;

    @Schema(title = "파이프라인 업무 종류", allowableValues = {"BUILD_DEPLOY"," PUBLIC_DEPLOY"}, example = "BUILD_DEPLOY")
    @JsonIgnore
    private String pipelineType;

    @Schema(title = "빌드 레지스트리 번호", description = "e.g) harbor projectId", example = "1")
    private Integer buildRegistrySeq;

    @Schema(title = "빌드 레지스트리 이름", description = "e.g) harbor projectName", example = "library")
    private String buildRegistryName;

    @Schema(title = "빌드 이미지명", example = "pipeline-build-test")
    private String buildImageName;

    @Schema(title = "빌드 이미지 태그", example = "2.1.6-test.B000001")
    private String buildImageTag;

    @Schema(title = "빌드 이미지 URL", example = "regi.acloud.run/cocktail-dev-qa/build-nginx:1.B000021")
    private String buildImageUrl;

    private String deployContent;
}

