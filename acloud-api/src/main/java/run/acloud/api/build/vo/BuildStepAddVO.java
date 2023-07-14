package run.acloud.api.build.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.build.enums.StepType;

@Getter
@Setter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "stepType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StepInitVO.class, name = StepType.Names.INIT),
        @JsonSubTypes.Type(value = StepCodeDownVO.class, name = StepType.Names.CODE_DOWN),
        @JsonSubTypes.Type(value = StepUserTaskVO.class, name = StepType.Names.USER_TASK),
        @JsonSubTypes.Type(value = StepFtpVO.class, name = StepType.Names.FTP),
        @JsonSubTypes.Type(value = StepHttpVO.class, name = StepType.Names.HTTP),
        @JsonSubTypes.Type(value = StepShellVO.class, name = StepType.Names.SHELL),
        @JsonSubTypes.Type(value = StepCreateImageVO.class, name = StepType.Names.CREATE_IMAGE),
        @JsonSubTypes.Type(value = StepCancelVO.class, name = StepType.Names.CANCEL)
})
@Schema(name = "BuildStepAddVO",
        title = "BuildStepAddVO",
        description = "빌드단계 생성 모델",
        discriminatorProperty = "stepType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = StepType.Names.INIT, schema = StepInitVO.class),
                @DiscriminatorMapping(value = StepType.Names.CODE_DOWN, schema = StepCodeDownVO.class),
                @DiscriminatorMapping(value = StepType.Names.USER_TASK, schema = StepUserTaskVO.class),
                @DiscriminatorMapping(value = StepType.Names.FTP, schema = StepFtpVO.class),
                @DiscriminatorMapping(value = StepType.Names.HTTP, schema = StepHttpVO.class),
                @DiscriminatorMapping(value = StepType.Names.SHELL, schema = StepShellVO.class),
                @DiscriminatorMapping(value = StepType.Names.CREATE_IMAGE, schema = StepCreateImageVO.class),
                @DiscriminatorMapping(value = StepType.Names.CANCEL, schema = StepCancelVO.class)
        },
        subTypes = {StepInitVO.class, StepCodeDownVO.class, StepUserTaskVO.class, StepFtpVO.class, StepHttpVO.class, StepShellVO.class, StepCreateImageVO.class, StepCancelVO.class}
)
public class BuildStepAddVO{

    @Schema(title = "빌드 단계 번호")
    private Integer buildStepSeq;

    @Schema(title = "빌드 단계 종류", allowableValues = { StepType.Names.INIT, StepType.Names.CODE_DOWN, StepType.Names.USER_TASK, StepType.Names.FTP, StepType.Names.HTTP, StepType.Names.SHELL, StepType.Names.CREATE_IMAGE, StepType.Names.CANCEL}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String stepType;

    @Schema(title = "빌드 단계 순서", required = false)
    @JsonIgnore
    private Integer stepOrder;

    @Schema(title = "빌드 단계 작업명", required = false)
    private String stepTitle;

    @Schema(title = "빌드 단계 작업설명", required = false)
    private String stepDesc;
}
