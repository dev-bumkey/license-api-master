package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.ImageRegistryType;
import run.acloud.commons.vo.HasUseYnVO;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 7.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "외부 레지스트리 모델")
public class ExternalRegistryVO extends HasUseYnVO {

    @Schema(description = "플랫폼 번호")
    private Integer accountSeq;

    @Schema(description = "외부 레지스트리 프로바이더", allowableValues = {"HARBOR"}, example = "HARBOR")
    private ImageRegistryType provider;

    @Schema(description = "외부 레지스트리 번호")
    private Integer externalRegistrySeq;

    @Schema(description = "외부 레지스트리 이름")
    private String name;

    @Schema(description = "외부 레지스트리 설명")
    private String description;

    @Schema(description = "엔드포인트 URL", example = "http(s)://192.168.1.1")
    private String endpointUrl;

    @Schema(description = "레지스트리 이름")
    private String registryName;

    @Schema(description = "접근 ID")
    private String accessId;

    @Schema(description = "접근 Secret")
    @JsonIgnore
    private String accessSecret;

    @Schema(description = "사설 CA 인증서 사용 여부", allowableValues = {"N","Y"})
    private String privateCertificateUseYn = "N";

    @Schema(description = "사설 인증서")
    @JsonIgnore
    private String privateCertificate;

    @Schema(description = "insecure 여부, Verify Remote Cert : N - 수행, Y - 미수행", allowableValues = {"N","Y"})
    private String insecureYn = "N";

    @Schema(description = "상태, Y - 정상, N - 이상", allowableValues = {"Y","N"})
    private String status;

}
