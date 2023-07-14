package run.acloud.api.openapi.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "API 토큰 발급 이력 엑셀 모델")
public class ApiTokenIssueHistoryExcelVO {

    @Schema(title = "API 토큰 발급 이력 순번")
    private Integer apiTokenIssueHistorySeq;

    @Schema(title = "API 토큰 발급 순번")
    private Integer apiTokenIssueSeq;

    @Schema(title = "플랫폼 순번")
    private Integer accountSeq;

    @Schema(title = "API 토큰 이름")
    private String apiTokenName;

    @Schema(title = "API 토큰 설명")
    private String apiTokenDescription;

    @Schema(title = "화이트 IP 목록 json")
    private String whiteIpListJson;

    @Schema(title = "블랙 IP 목록 json")
    private String blackIpListJson;

    @Schema(title = "수정 사용자 순번")
    private Integer updateUserSeq;

    @Schema(title = "수정 사용자 ID")
    private String updateUserId;

    @Schema(title = "수정 사용자 이름")
    private String updateUserName;

    @Schema(title = "만료일시", description = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String expirationDatetime;

    @Schema(title = "이력 상태", allowableValues = {"GRANT","CHANGE","REVOKE","EXPIRED"})
    private String historyState;

    @Schema(title = "이력 일시")
    private String historyDatetime;

    @Schema(title = "이력 메시지")
    private String historyMessage;

    @Schema(title = "API 권한 범위 조회 여부", hidden = true)
    @JsonIgnore
    private Boolean withApi;

    @Schema(title = "API 권한 범위")
    private List<ApiTokenPermissionsScopeHistoryGroupExcelVO> permissionsScopes;

}
