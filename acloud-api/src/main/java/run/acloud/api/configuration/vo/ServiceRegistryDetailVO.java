package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Schema(description = "워크스페이스 레지스트리 모델")
public class ServiceRegistryDetailVO extends ServiceRegistryVO {

    @Schema(title = "서비스가 사용하는 레지스트리 사용자 id")
    @JsonIgnore
    private String registryUserId;

    @Schema(title = "서비스가 사용하는 레지스트리 사용자 암호")
    @JsonIgnore
    private String registryUserPassword;

}
