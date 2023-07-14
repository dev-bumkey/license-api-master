package run.acloud.api.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasCreatorVO;

@Getter
@Setter
@Schema(description = "API 토큰 권한 범위 이력 모델")
public class ApiTokenPermissionsScopeHistoryVO extends HasCreatorVO {

    @Schema(title = "API 토큰 권한 범위 이력 순번")
    private Integer apiTokenPermissionsScopeHistorySeq;

    @Schema(title = "API 토큰 발급 이력 순번")
    private Integer apiTokenIssueHistorySeq;

    @Schema(title = "API 순번")
    private Integer apiSeq;

    @Schema(title = "API 이름")
    private String apiName;

    @Schema(title = "API 그룹 순번")
    private Integer apiGroupSeq;

    @Schema(title = "API 그룹 이름")
    private String apiGroupName;

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
}
