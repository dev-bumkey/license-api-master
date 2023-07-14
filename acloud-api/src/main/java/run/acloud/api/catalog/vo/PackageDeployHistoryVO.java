package run.acloud.api.catalog.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.enums.ExecutionResultCode;
import run.acloud.framework.context.ContextHolder;

import java.io.Serializable;
import java.util.Optional;

@Getter
@Setter
@Schema(title = "Package Deploy History", description = "Package Deploy History Model")
public class PackageDeployHistoryVO implements Serializable {
    private static final long serialVersionUID = -8335205979115048706L;

    @Schema(title = "packageDeployHistorySeq")
    private Integer packageDeployHistorySeq;

    @Schema(title = "clusterSeq")
    private Integer clusterSeq;

    @Schema(title = "namespaceName")
    private String namespaceName;

    @Schema(title = "releaseName")
    private String releaseName;

    @Schema(title = "chartName")
    private String chartName;

    @Schema(title = "chartVersion")
    private String chartVersion;

    @Schema(title = "revision")
    private String revision;

    @Schema(title = "repository")
    private String repository;

    @Schema(title = "chartValues")
    private String chartValues;

    @Schema(title = "manifest")
    private String packageManifest;

    @Schema(title = "command")
    private String command;

    @Schema(title = "executionResult")
    private String executionResult = ExecutionResultCode.UNKNOWN.getCode();

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    protected String created;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JsonIgnore
    protected Integer creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
}
