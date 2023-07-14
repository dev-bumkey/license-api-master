package run.acloud.api.openapi.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.ObjectMapperUtils;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@Schema(description = "API 토큰 발급 이력 모델")
public class ApiTokenIssueHistoryVO {

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

    @Schema(title = "토큰")
    private String token;

    public String getToken() {
        if (StringUtils.isNotBlank(this.getTokenEnc())) {
            this.token = CryptoUtils.decryptAES(this.getTokenEnc());
        } else {
            this.token = null;
        }

        return this.token;
    }

    @Schema(title = "암호화된 토큰", hidden = true)
    @JsonIgnore
    private String tokenEnc;

    @Schema(title = "화이트 IP 목록")
    private List<String> whiteIpList;

    public List<String> getWhiteIpList() {
        if (StringUtils.isNotBlank(this.getWhiteIpListJson())) {
            try {
                this.whiteIpList = ObjectMapperUtils.getMapper().readValue(this.getWhiteIpListJson(), new TypeReference<List<String>>(){});
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.whiteIpList = null;
        }

        return this.whiteIpList;
    }

    @Schema(title = "화이트 IP 목록 json", hidden = true)
    @JsonIgnore
    private String whiteIpListJson;

    @Schema(title = "블랙 IP 목록")
    private List<String> blackIpList;

    public List<String> getBlackIpList() {
        if (StringUtils.isNotBlank(this.getBlackIpListJson())) {
            try {
                this.blackIpList = ObjectMapperUtils.getMapper().readValue(this.getBlackIpListJson(), new TypeReference<List<String>>(){});
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.blackIpList = null;
        }

        return this.blackIpList;
    }

    @Schema(title = "블랙 IP 목록 json", hidden = true)
    @JsonIgnore
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

    @Schema(title = "총 호출 건수")
    private BigInteger totalRequestCount;

    @Schema(title = "이력 상태", allowableValues = {"GRANT","CHANGE","REVOKE","EXPIRED"})
    private String historyState;

    @Schema(title = "이력 일시")
    private String historyDatetime;

    @Schema(title = "이력 메시지")
    private String historyMessage;

    @Schema(title = "API 권한 범위")
    private List<ApiTokenPermissionsScopeHistoryVO> permissionsScopes;

}
