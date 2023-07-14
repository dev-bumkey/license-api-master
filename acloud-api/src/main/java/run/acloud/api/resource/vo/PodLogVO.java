package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
public class PodLogVO {
    private String podName;

    private String containerName;

    private String log;
}
