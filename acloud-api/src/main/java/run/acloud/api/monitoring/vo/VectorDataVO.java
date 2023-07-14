package run.acloud.api.monitoring.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: wschoi@bettertomorrow.com
 * Created on 2018. 3. 14.
 */
@Getter
@Setter
public class VectorDataVO {
    public VectorDataVO() {
        this.metrics = new HashMap<>();
    }

    private Map<String, String> metrics;
    private float value;
}
