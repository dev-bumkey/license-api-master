package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.DeploymentStrategyType;

import java.io.Serializable;

@Getter
@Setter
public class DeploymentStrategyVO implements Serializable {

    private DeploymentStrategyType type;

    private String maxUnavailable;

    private String maxSurge;

}
