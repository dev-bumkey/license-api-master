package run.acloud.api.openapi.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API 토큰 발급 엑셀 모델")
public class ApiTokenIssueExcelVO extends HasUseYnVO {

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

    @Schema(title = "발급일시")
    private String issueDatetime;

    @Schema(title = "만료일시", description = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String expirationDatetime;

    @Schema(title = "현재 호출 건수")
    private BigInteger currRequestCount;

    @Schema(title = "총 호출 건수")
    private BigInteger totalRequestCount;

    @Schema(title = "마지막 호출 일시")
    private String lastRequestDatetime;

    @Schema(title = "발급자 번호")
    private Integer issueUserSeq;

    @Schema(title = "발급자 ID")
    private String issueUserId;

    @Schema(title = "발급자 명")
    private String issueUserName;

    @Schema(title = "API 권한 범위 조회 여부", hidden = true)
    @JsonIgnore
    private Boolean withApi;

    @Schema(title = "API 권한 범위")
    private List<ApiTokenPermissionsScopeGroupExcelVO> permissionsScopes;


}
