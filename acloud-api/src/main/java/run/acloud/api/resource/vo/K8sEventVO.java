package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
public class K8sEventVO extends BaseVO{

    @Schema(title = "메세지")
    private String message;

    @Schema(title = "소스")
    private String source;

    @Schema(title = "서브오브젝트")
    private String subObject;

    @Schema(title = "횟수")
    private Integer count;

    @Schema(title = "처음 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime firstTime;

    @Schema(title = "마지막 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime lastTime;

    @Schema(title = "kind")
    private String kind;

    @Schema(title = "name")
    private String name;

    @Schema(title = "namespace")
    private String namespace;

    @Schema(title = "involvedObject")
    private K8sObjectReferenceVO involvedObject;

    @Schema(title = "related")
    private K8sObjectReferenceVO related;

    @Schema(title = "Event 배포 정보")
    private String deployment;

    @Schema(title = "Event 배포 정보 (Yaml)")
    private String deploymentYaml;
}
