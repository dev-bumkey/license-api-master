package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Created by gun@acornsoft.io on 2020.05.13
 */
@Getter
@Setter
@Schema(description = "인증서 모델")
public class CertificateVO implements Serializable {
    private static final long serialVersionUID = -2437520465963247378L;

    private String publicKey;

    private String privateKey;

    private String certificate;

}
