package run.acloud.api.monitoring.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@bettertomorrow.com
 * Created on 2018. 3. 6.
 */
@Getter
@Setter
public class MatrixDataVO {
    public MatrixDataVO() {
        this.metrics = new HashMap<>();
        this.samples = new ArrayList<>();
    }

    private Map<String, String> metrics;
    private List<SampleVO> samples;
}
