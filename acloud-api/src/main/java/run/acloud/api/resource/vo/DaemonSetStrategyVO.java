package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.DaemonSetStrategyType;

import java.io.Serializable;

@Getter
@Setter
public class DaemonSetStrategyVO implements Serializable {

    private DaemonSetStrategyType type;

    private String maxUnavailable;

}
