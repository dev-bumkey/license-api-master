package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(title = "Helm Release Base", description = "Helm Release Base Model")
public class HelmReleaseBaseVO implements Serializable {
    private static final long serialVersionUID = -6197017640205046869L;

    @Schema(title = "namespace")
    private String namespace;

    @Schema(title = "name")
    private String name;

    @Schema(title = "chart")
    private String chart;

    @Schema(title = "revision")
    private String revision;

    @Schema(title = "repo")
    private String repo;

    @Schema(title = "values")
    private String values;

    @Schema(title = "manifest")
    private String manifest;

    @Schema(title = "chartNameAndVersion")
    private String chartNameAndVersion;

    @Schema(title = "chartName")
    private String chartName;

    @Schema(title = "chartVersion")
    private String chartVersion;

    @Schema(title = "appVersion")
    private String appVersion;

    @Schema(title = "info")
    private HelmReleaseBaseInfoVO info;

    @Schema(title = "클러스터순번")
    private Integer clusterSeq;

    @Schema(title = "클러스터이름")
    private String clusterName;

    @Schema(title = "서비스맵 정보")
    private ServicemapSummaryVO servicemapInfo;
}