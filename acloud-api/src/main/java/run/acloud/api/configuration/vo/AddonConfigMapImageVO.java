package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(description = "Addon configMap Image 모델")
public class AddonConfigMapImageVO extends BaseVO {

    private String registry_url;

}