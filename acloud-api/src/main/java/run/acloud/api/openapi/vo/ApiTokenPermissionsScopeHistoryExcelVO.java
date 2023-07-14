package run.acloud.api.openapi.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API 토큰 권한 범위 이력 엑셀 모델")
public class ApiTokenPermissionsScopeHistoryExcelVO {

    @Schema(title = "API 토큰 권한 범위 이력 순번", hidden = true)
    @JsonIgnore
    private Integer apiTokenPermissionsScopeHistorySeq;

    @Schema(title = "API 토큰 발급 이력 순번", hidden = true)
    @JsonIgnore
    private Integer apiTokenIssueHistorySeq;

    @Schema(title = "API 그룹 순번", hidden = true)
    @JsonIgnore
    private Integer apiGroupSeq;

    @Schema(title = "API 순번", hidden = true)
    @JsonIgnore
    private Integer apiSeq;

    @Schema(title = "API 이름")
    private String name;

    @Schema(title = "API 메소드")
    private String method;

    @Schema(title = "API URL")
    private String url;

}
