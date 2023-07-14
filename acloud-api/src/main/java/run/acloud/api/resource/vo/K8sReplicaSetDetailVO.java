package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "Replica Set 상세 모델")
public class K8sReplicaSetDetailVO extends BaseVO{

    @Schema(title = "Replica Set 명")
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
    private OffsetDateTime creationTime;

    @Schema(title = "Selector")
    private K8sLabelSelectorVO selector;

    @Schema(title = "이미지 목록")
    private List<String> images;

    @Schema(title = "Pods")
    private Map<String, Integer> pods;

    @Schema(title = "Pods Status")
    private Map<String, Integer> podsStatus;

    @Schema(title = "podTemplate")
    private K8sPodTemplateSpecVO podTemplate;
}
