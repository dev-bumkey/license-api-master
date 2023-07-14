package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;


@Getter
@Setter
@Schema(name = "BuildServerAddVO", title= "BuildServerAddVO", description = "빌드 서버 생성 모델")
public class BuildServerAddVO extends HasUseYnVO {

    @Schema(title = "빌드 서버 이름", description = "편집 종류가 신규시 필수")
    private String buildServerName;

    @Schema(title = "빌드 서버 설명", description = "빌드에 대한 설명으로 필수값 아님")
    private String buildServerDesc;

    @Schema(title = "Apps 이벤트 서버")
    private String appsEventServer;

    @Schema(title = "빌드 서버 번호", description = "편집 종류가 편집시 필수")
    private Integer buildServerSeq;

    @Schema(title = "계정 번호", description = "시스템의 계정번호")
    private Integer accountSeq;

    @Schema(title = "클러스터 순번", description = "빌드 이미지 저장할 레지스트리 프로젝트 ID")
    private Integer clusterSeq;

    @Schema(title = "클러스터 아이디", description = "빌드 이미지 저장할 레지스트리 프로젝트 ID")
    private String clusterId;

    @Schema(title = "네임스페이스 명", description = "네임스페이스명")
    private String namespace;

    @Schema(title = "cpu request", description = "cpu request")
    private Integer cpuRequest;

    @Schema(title = "cpu limit", description = "cpu limit")
    private Integer cpuLimit;

    @Schema(title = "memory request", description = "memory request")
    private Integer memoryRequest;

    @Schema(title = "memory limit", description = "memory limit")
    private Integer memoryLimit;

    @Schema(title = "node selector", description = "node selector")
    private String nodeSelector;

    @Schema(title = "tolerations", description = "tolerations")
    private String tolerations;

    @Schema(title = "affinity", description = "affinity")
    private String affinity;

    @Schema(title = "persistence Enabled", description = "persistence Enabled, persistence.enabled", allowableValues = {"true", "false"})
    private Boolean persistenceEnabled;

    @Schema(title = "storageClass 명", description = "storageClass, persistence.workspace.storageClass")
    private String storageClass;

    @Schema(title = "pvc 명", description = "pvc 명, persistence.workspace.existingClaim")
    private String pvcName;

    @Schema(title = "pvc 용량", description = "pvc 용량, persistence.workspace.size")
    private String pvcSize;

    @Schema(title = "Insecure Registries")
    private List<String> insecureRegistries;

    @Schema(title = "controller name", description = "워크로드 이름")
    private String controllerName;

}
