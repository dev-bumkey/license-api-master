package run.acloud.api.openapi.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API 토큰 권한 범위 이력 그룹 엑셀 모델")
public class ApiTokenPermissionsScopeHistoryGroupExcelVO {

    @Schema(title = "API 토큰 발급 이력 순번", hidden = true)
    @JsonIgnore
    private Integer apiTokenIssueHistorySeq;

    @Schema(title = "API 그룹 순번", hidden = true)
    @JsonIgnore
    private Integer apiGroupSeq;

    @Schema(title = "API 그룹 이름")
    private String groupName;

    @Schema(title = "API 권한 범위 목록")
    private List<ApiTokenPermissionsScopeHistoryExcelVO> apis;
}
