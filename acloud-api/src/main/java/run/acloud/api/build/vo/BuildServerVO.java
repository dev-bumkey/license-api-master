package run.acloud.api.build.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "빌드 서버 모델")
public class BuildServerVO extends BuildServerAddVO {

    @Schema(title = "빌드 서버 배포 설정 Yaml", description = "내부 조회용")
    @JsonIgnore
    private String deployConfig;

}
