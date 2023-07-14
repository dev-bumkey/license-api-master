package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.StatefulSetStrategyType;

import java.io.Serializable;

@Getter
@Setter
public class StatefulSetStrategyVO implements Serializable {

    private StatefulSetStrategyType type;

    private Integer partition;

}
