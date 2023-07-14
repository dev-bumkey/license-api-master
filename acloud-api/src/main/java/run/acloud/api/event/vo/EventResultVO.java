package run.acloud.api.event.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author: skyikho@acornsoft.io
 * Created on 2017. 8. 3.
 */
@Getter
@Setter
@Schema(description = "이벤트 결과 모델")
public class EventResultVO<T> {

    private String event;
    private Map<String, Object> target;

    private T data;
}

