package run.acloud.api.monitoring.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author: wschoi@bettertomorrow.com
 * Created on 2018. 3. 19.
 */
@Getter
@Setter
public class ServerPodVO {
    private String serverName;

    private List<String> podNames;
}
