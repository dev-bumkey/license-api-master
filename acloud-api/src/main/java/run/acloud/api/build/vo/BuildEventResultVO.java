package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Schema(description = "빌드 결과 모델")
public class BuildEventResultVO<T> {

    private String event;
    private Map<String, Object> target;

    private T data;
}

