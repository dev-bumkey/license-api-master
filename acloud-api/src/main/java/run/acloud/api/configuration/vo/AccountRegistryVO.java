package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.ImageRegistryType;
import run.acloud.commons.vo.HasUseYnVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "플랫폼 레지스트리 모델")
public class AccountRegistryVO extends HasUseYnVO implements Serializable {
    private static final long serialVersionUID = 8008192952557238153L;

    @Schema(title = "플랫폼 레지스트리 번호")
    private Integer accountRegistrySeq;

    @Schema(title = "플랫폼 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer accountSeq;

    @Schema(description = "레지스트리 타입", allowableValues = {"HARBOR"}, example = "HARBOR")
    private ImageRegistryType provider;

    @Schema(description = "레지스트리 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "레지스트리 설명")
    private String description;

    @Schema(description = "레지스트리 URL", example = "http(s)://192.168.1.1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registryUrl;

    @Schema(description = "접근 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessId;

    @Schema(description = "접근 Secret", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessSecret;

    @Schema(description = "사설 CA 인증서 사용 여부", allowableValues = {"N","Y"})
    private String privateCertificateUseYn;

    @Schema(description = "사설 인증서")
    private String privateCertificate;

    @Schema(description = "insecure 여부, Verify Remote Cert : N - 수행, Y - 미수행", allowableValues = {"N","Y"})
    private String insecureYn = "Y";

    @Schema(description = "상태, Y - 정상, N - 이상", allowableValues = {"Y","N"})
    private String status;

}