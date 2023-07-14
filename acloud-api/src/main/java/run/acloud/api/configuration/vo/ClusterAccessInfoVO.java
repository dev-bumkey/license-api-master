package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.JsonSyntaxException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.AuthType;
import run.acloud.api.resource.enums.CubeType;
import run.acloud.api.resource.util.K8sMapperUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.BaseVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;


@Getter
@Setter
@Schema(description = "클러스터 접근 정보")
public class ClusterAccessInfoVO extends BaseVO {
    private static final long serialVersionUID = 8303185289711001522L;

    @Schema(title = "클러스터 Sequence")
    @JsonIgnore
    private Integer clusterSeq;

    @Schema(title = "클러스터 ID")
    @JsonProperty("cluster_name")
    private String clusterId;

    @Schema(title = "클러스터 endpoint")
    @JsonProperty("endpoint")
    private String apiUrl;

    @Schema(title = "클러스터 CA 인증서 : 설치 유형이 CERT일 경우 입력")
    @JsonProperty("certificate_authority")
    private String serverAuthData;

    @Schema(title = "Certificate authority data : 설치 유형이 CERT일 경우 입력. 보통 client.crt")
    @JsonProperty("client_certificate")
    private String clientAuthData;

    @Schema(title = "Certificate authority data : 설치 유형이 CERT일 경우 입력. 보통 client.key")
    @JsonProperty("client_key")
    private String clientKeyData;

    @Schema(title = "user name")
    @JsonProperty("username")
    public String getUsername() {
        if (AuthType.CERT != this.authType) {
            return this.apiKey;
        }
        return null;
    }

    @Schema(title = "password")
    @JsonProperty("password")
    public String getPassword() {
        return this.apiSecret;
    }

    @Schema(title = "인증 토큰")
    @JsonProperty("token")
    public String getToken() {
        try {
            if (AuthType.TOKEN == this.authType) {
                String accessToken = "";
                if (EnumSet.of(CubeType.EKS, CubeType.NCPKS).contains(this.cubeType)) {
                    Map<String, Object> authMap = K8sMapperUtils.getMapper().readValue(this.getPassword(), new TypeReference<Map<String, Object>>() {});
                    accessToken = (String) ((Map<String, Object>) authMap.get("status")).get("token");
                }
                else if (EnumSet.of(CubeType.GKE, CubeType.AKS).contains(this.cubeType)) {
                    OAuthTokenVO authToken = JsonUtils.fromGson(this.getPassword(), OAuthTokenVO.class);
                    accessToken = authToken.getAccessToken();
                }
                return accessToken;
            }
        }
        catch (JsonParseException | JsonMappingException je) {
            throw new CocktailException("Unable to parse cluster access information", je, ExceptionType.InvalidClusterCertification);
        }
        catch (JsonSyntaxException je) {
            throw new CocktailException("Unable to parse cluster access information", je, ExceptionType.InvalidClusterCertification);
        }
        catch (IOException ie) {
            throw new CocktailException("Unable to parse cluster access information", ie, ExceptionType.InvalidClusterCertification);
        }
        catch (Exception ex) {
            throw new CocktailException("Unable to parse cluster access information", ex, ExceptionType.InvalidClusterCertification);
        }
        return null;
    }

    @Schema(title = "User password | Barer token : 설치 유형이 TOKEN일 경우 barer token, CERT일 경우 user password")
    @JsonIgnore
    private String apiSecret;

    @Schema(title = "User id : 설치 유형이 CERT일 경우 입력")
    @JsonIgnore
    private String apiKey;

    @Schema(title = "설치 유형 (CERT / TOKEN / PLAIN)")
    @JsonIgnore
    private AuthType authType;

    @Schema(title = "큐브 클러스터 타입 : 클러스터 타입이 CUBE인 경우 입력")
    @JsonIgnore
    private CubeType cubeType;
}
