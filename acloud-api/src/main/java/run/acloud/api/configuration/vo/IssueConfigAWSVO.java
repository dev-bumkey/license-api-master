package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Issue Config 필드 모델")
public class IssueConfigAWSVO extends BaseVO {

    private static final long serialVersionUID = -8586168483143706168L;

    @Schema(description = "사용자 이름")
    private String userName;

    @Schema(description = "Amazon Resource Number")
    private String arn;

    @Schema(description = "path")
    private String path;

    @Schema(description = "Amazon Region")
    private String region;
}
