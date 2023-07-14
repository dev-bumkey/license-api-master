package run.acloud.api.auth.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 24.
 */
@Schema(name = "LanguageType", description = "LanguageType")
public enum LanguageType implements EnumCode {
    @Schema(name = "ko", description = "Asia/Seoul")
    ko("Asia/Seoul"),
    @Schema(name = "en", description = "America/New_York")
    en("America/New_York"),
    @Schema(name = "ja", description = "Asia/Tokyo")
    ja("Asia/Tokyo"),
    @Schema(name = "zh", description = "Asia/Shanghai")
    zh("Asia/Shanghai");

    @Getter
    private String timezone;

    LanguageType(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
