package run.acloud.api.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "API 게이트 그룹 모델")
public class ApiGatewayGroupsVO extends HasUseYnVO {

    @Schema(title = "API 그룹 순번")
    private Integer apiGroupSeq;

    @Schema(title = "API 그룹 코드")
    private String apiGroupCode;

    @Schema(title = "API 그룹 이름")
    private String apiGroupName;

    @Schema(title = "정렬 순서")
    private int sortOrder;

    @Schema(title = "API 목록")
    private List<ApiGatewaysVO> apis;
}
