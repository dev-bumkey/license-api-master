package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "계정 구성 모델")
public class AccountConfigVO extends BaseVO {

    @Schema(title = "내부 빌드서버 사용 여부", allowableValues = {"Y", "N"}, defaultValue = "Y")
    private String internalBuildServerUseYn;

    public String getInternalBuildServerUseYn() {
        if (StringUtils.isBlank(this.internalBuildServerUseYn)) {
            return this.internalBuildServerUseYn = "Y";
        } else {
            return this.internalBuildServerUseYn;
        }
    }
}