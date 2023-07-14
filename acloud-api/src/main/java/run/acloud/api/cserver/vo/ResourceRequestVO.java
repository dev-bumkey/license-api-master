package run.acloud.api.cserver.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 11. 16.
 */
@Getter
@Setter
public class ResourceRequestVO {
    private Double cpuMax = 0.0;

    private Double memoryMax = 0.0;

    private Double totalCpu = 0.0;

    private Double totalMemory = 0.0;

    private Integer totalPod = 0;
}
