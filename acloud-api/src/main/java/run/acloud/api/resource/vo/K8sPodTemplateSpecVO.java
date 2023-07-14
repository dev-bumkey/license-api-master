package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import run.acloud.commons.vo.BaseVO;

import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "Pod Template Spec 모델")
public class K8sPodTemplateSpecVO extends BaseVO{

    @Schema(title = "Pod Template Spec 명")
    private String name;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "ownerReferences")
    private List<K8sOwnerReferenceVO> ownerReferences;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private DateTime creationTime;

    @Schema(title = "spec")
    private K8sPodVO spec;

}
