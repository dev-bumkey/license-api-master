package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 7.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "외부 레지스트리 모델")
public class ExternalRegistryAddVO extends ExternalRegistryVO {

    @Schema(description = "접근 Secret")
    @JsonProperty("accessSecret")
    private String accessSecret;

    @Schema(description = "사설 인증서")
    @JsonProperty("privateCertificate")
    private String privateCertificate;

}
