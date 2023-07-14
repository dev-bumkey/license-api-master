package run.acloud.api.catalog.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Helm Status Request", description = "Helm Status Request Model")
public class HelmInstallRequestVO extends HelmRequestBaseVO {
    private static final long serialVersionUID = -1647141797818357028L;

    @Schema(title = "repo")
    private String repo;

    @Schema(title = "chartName")
    private String chartName;

    @Schema(title = "version")
    private String version;

    @Schema(title = "releaseName")
    private String releaseName;

    @Schema(title = "values")
    private String values;

    /** Appmap 신규 생성을 위한 변수 추가 **/
    @Schema(title = "실행 종류", description = "N : 신규 servicemap을 생성, A : 기존 servicemap에 추가")
    private String launchType = "A";

    @Schema(title = "서비스 번호")
    private Integer serviceSeq;

    @Schema(title = "서비스맵 이름")
    @SerializedName(value="servicemapName", alternate = {"appmapName"})
    private String servicemapName;

    @Schema(title = "서비스맵 그룹 번호")
    @SerializedName(value="servicemapGroupSeq", alternate = {"appmapGroupSeq"})
    private Integer servicemapGroupSeq;
}
