package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.enums.AccessMode;
import run.acloud.api.cserver.enums.VolumeType;
import run.acloud.commons.vo.BaseVO;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "컨테이너에서 사용할 volume의 정의")
public class ContainerVolumeVO extends BaseVO implements Serializable {
    @Schema(title = "Volume 이름",
            description = "이 이름을 K8S spec.volume.name, spec.container.volumeMount.name에 사용", requiredMode = Schema.RequiredMode.REQUIRED)
    private String volumeName;

    @Schema(title = "Volume type", requiredMode = Schema.RequiredMode.REQUIRED)
    private VolumeType volumeType;

    @Schema(title = "Volume type이 PERSISTENT_VOLUME일 때 Cluster Volume 일련번호",
            description = "Persistent Volume 생성에 사용한다")
    private Integer clusterVolumeSeq;

    @Schema(title = "Volume type이 CONFIG_MAP일 때 config map 이름")
    private String configMapName;

    @Schema(title = "Volume type이 SECRET일 때 secret 이름")
    private String secretName;

    @Schema(title = "Volume type이 SECRET일 때 defaultMode")
    private Integer defaultMode;

    @Schema(title = "Volume type이 HOST_PATH일 때 host path")
    private String hostPath;

    // https://kubernetes.io/docs/concepts/storage/volumes/#hostpath
    private String hostPathType;

    @Schema(title = "Persisent Volume의 크기", description = "단위는 GB")
    private Integer capacity;

    @Schema(title = "Persisent Volume의 access 방식")
    private AccessMode accessMode;

    @Schema(title = "Volume type이 EMPTY_DIR일 때 메모리 사용 여부", description = "기본 값은 'N'")
    private String useMemoryYn = "N";

    @Schema(title = "Volume type이 EMPTY_DIR일 때 medium")
    private String emptyDirMedium;

    @Schema(title = "Persistent Volume Claim의 이름. 정적 PV/PVC binding에서 사용.",
            description = "내부 처리용. 클라이언트에서 입력하지 않음. 자동 생성")
    private String persistentVolumeClaimName;

    @Schema(title = "Volume type이 PERSISTENT_VOLUME일 때 Volume 설정 Object",
            description = "내부 처리용. 클라이언트에서 입력하지 않음")
    private ClusterVolumeVO clusterVolume;

	private Integer containerVolumeSeq;

	private Integer containerVolumeIndex; // 다중 컨테이너 상황에서 volume reference를 위해 사용
	
	private Integer containerSeq;

	private String podName;

	private String containerName;

	@Size(min = 1)
	private String containerPath; // hostPath, emptyDir에서 사용, Persistent Volume은 VolumeMountPath에 저장

	private String subPath; // hostPath, emptyDir에서 사용, Persistent Volume은 VolumeMountPath에 저장
    private String subPathExpr;

    // Persistent Volume Claim 생성에 사용
    private Integer persistentVolumeSeq; // to db

    // Persistent Volume Claim 생성에 사용
    private Integer persistentVolumeClaimSeq; // to db

    // Persistent Volume의 이름. 사용자가 입력할 수도 있지만, 현재는 자동 생성
	private String persistentVolumeName;


    // Persistent Claim을 mount할 때 사용하는 이름. Pod의 volumes.name으로 spec.containers.volumeMounts.name과 같으며
    // host path, emptyDir에서는 사용하지 않는다
    private String mountName; // mountInfos로 이동

    private List<VolumeMountVO> paths;

    private Boolean readOnly = null;

    // emptyDir source
    private String sizeLimit;

    // configMap, secret
    private Boolean optional;
    private List<ContainerVolumeKeyToPathVO> items;
}
