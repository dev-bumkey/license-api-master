package run.acloud.api.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "API 게이트웨이 모델")
public class ApiGatewaysVO extends HasUseYnVO {
    @Schema(title = "API 순번")
    private Integer apiSeq;

    @Schema(title = "API 그룹 순번")
    private Integer apiGroupSeq;

    @Schema(title = "API 그룹 코드")
    private String apiGroupCode;

    @Schema(title = "API 그룹 명")
    private String apiGroupName;

    @Schema(title = "API 이름")
    private String apiName;

    @Schema(title = "API 설명")
    private String apiDescription;

    @Schema(title = "API 컨텐츠 유형")
    private String apiContentType;

    @Schema(title = "API URL")
    private String apiUrl;

    @Schema(title = "API 메소드")
    private String apiMethod;

    @Schema(title = "API 백엔드 호스트")
    private String apiBackendHost;

    @Schema(title = "API 백엔드 URL")
    private String apiBackendUrl;

    @Schema(title = "API 백엔드 확장설정")
    private String apiBackendExtraConfig;

    @Schema(title = "정렬 순서")
    private int sortOrder;

}
