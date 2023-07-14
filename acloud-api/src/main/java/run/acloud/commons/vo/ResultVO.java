package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "응답 결과 모델")
public class ResultVO {
    @Schema(description = "처리 상태. ok | error", allowableValues = {"ok","error"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String status = "ok";
    @Schema(description = "처리 상태 코드", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code = "200";
    @Schema(description = "Biz 코드")
    private String biz;
    @Schema(description = "처리 결과에 관련된 메시지")
    private String message;
    @Schema(description = "추가로 전달되는 메시지 (Detail Message)")
    private String additionalMessage;
    @Schema(description = "데이터")
    private Object data;
    @Schema(description = "request info")
    private Object requestInfo;
    @Schema(description = "상세 결과")
    private Object result;
    @Schema(description = "http 상태 코드")
    private Integer httpStatusCode;

    @SuppressWarnings("unchecked")
    public void putKeyValue(String key, Object value) {
        if(StringUtils.isEmpty(key)) {
            return;
        }

        if(this.result == null) {
            this.result = new HashMap<Object, Object>();

        }

        if(this.result instanceof Map) {
            ((Map<Object, Object>)this.result).put(key, value);
        }
    }
}
