package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;

import java.time.OffsetDateTime;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 8. 29.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(title = "설정정보 모델")
public class SettingInformationVO {

    @Schema(title = "설정정보 이름")
    private String name;

    @Schema(title = "설정정보 유형")
    private String type;        // K8sApiKindType

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "클러스터순번")
    private Integer clusterSeq;

    @Schema(title = "클러스터이름")
    private String clusterName;

    @Schema(title = "네임스페이스이름")
    private String namespaceName;

    @Schema(title = "서비스맵 정보")
    private ServicemapSummaryVO servicemapInfo;
}
