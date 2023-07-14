package run.acloud.api.openapi.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.commons.vo.HasUpdaterVO;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@Schema(description = "API 토큰 발급 수정 모델")
public class ApiTokenIssueEditVO extends HasUpdaterVO {

    @Schema(title = "API 토큰 발급 순번", hidden = true)
    private Integer apiTokenIssueSeq;

    @Schema(title = "플랫폼 순번", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer accountSeq;

    @Schema(title = "API 토큰 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String apiTokenName;

    @Schema(title = "API 토큰 설명")
    private String apiTokenDescription;

    @Schema(title = "api 권한 범위", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 1)
    private List<Integer> permissionsScopes;

    @Schema(title = "화이트 IP 목록")
    private List<String> whiteIpList;

    @Schema(title = "화이트 IP 목록 json", hidden = true)
    @JsonIgnore
    private String whiteIpListJson;

    public String getWhiteIpListJson() throws Exception {
        if (CollectionUtils.isNotEmpty(this.getWhiteIpList())) {
            this.whiteIpListJson = ObjectMapperUtils.getMapper().writeValueAsString(this.getWhiteIpList());
        } else {
            this.whiteIpListJson = null;
        }

        return this.whiteIpListJson;
    }

    @Schema(title = "블랙 IP 목록")
    private List<String> blackIpList;

    @Schema(title = "블랙 IP 목록 json", hidden = true)
    @JsonIgnore
    private String blackIpListJson;

    public String getBlackIpListJson() throws Exception {
        if (CollectionUtils.isNotEmpty(this.getBlackIpList())) {
            this.blackIpListJson = ObjectMapperUtils.getMapper().writeValueAsString(this.getBlackIpList());
        } else {
            this.blackIpListJson = null;
        }

        return this.blackIpListJson;
    }

    @Schema(title = "총 호출 건수")
    private BigInteger totalRequestCount;
}
