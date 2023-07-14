package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.PublicCloudAccessType;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Public Cloud AccessInfo 필드 모델")
public class PublicCloudAccessInfoVO extends BaseVO {
    @Schema(description = "Access Type (AWS, AWSIAM, GCP, AZR")
    private PublicCloudAccessType publicCloudAccessType;

    /**
     * For AWS
     **/
    @Schema(description = "AWS Access Key")
    private String awsAccessKey;

    @Schema(description = "AWS Access Secret")
    private String awsSecret;

    @Schema(description = "userInfo")
    private IssueConfigAWSVO awsUser;

    /**
     * For Azure
     **/
    @Schema(description = "Azure Tenant Id")
    private String azureTenantId;

    @Schema(description = "Azure Workspace Id")
    private String azureWorkspaceId;

    @Schema(description = "Azure Application (Client) Id")
    private String azureClientId;

    @Schema(description = "Azure Client Secret")
    private String azureClientSecret;

    /**
     * For GCP
     **/
    @Schema(description = "GCP Json Key")
    private String gcpJsonKey;

}
