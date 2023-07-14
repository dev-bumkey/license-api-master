package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 7.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "외부 레지스트리 모델")
public class ExternalRegistryDetailVO extends ExternalRegistryVO {

    @Schema(description = "워크스페이스 목록")
    private List<ServiceVO> services;
}
