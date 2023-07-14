package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.serverless.enums.ServerlessType;

@Schema(description="Gateway Token 요청 Model by serverless")
@Setter
@Getter
public class RequestGatewayTokenVO {

    @Schema(name = "Cluster Id")
    private String clusterId;

    @Schema(name = "Project Name")
    private String projectName;

    @Schema(name = "User Id")
    private String userId;

    @Schema(name = "serverless 유형", allowableValues = {ServerlessType.Names.BAAS , ServerlessType.Names.FAAS})
    private String serverlessType;

    @Schema(name = "functionn name")
    private String functionName;
}
