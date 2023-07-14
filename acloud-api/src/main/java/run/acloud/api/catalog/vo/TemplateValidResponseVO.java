package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "템플릿 유효성 응답 모델")
public class TemplateValidResponseVO extends HasUseYnVO {
	
	@Schema(title = "템플릿배치 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer templateDeploymentSeq;

	@Schema(title = "유효성 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	private boolean isValid;
	
	@Schema(title = "유효성 메세지")
	private String validMsg;

	@Schema(title = "서버이름 유효성 여부")
	private boolean serverNameValid;

	@Schema(title = "ConfigMap이름 유효성 여부")
	private boolean configMapNameValid;

	@Schema(title = "Secret이름 유효성 여부")
	private boolean secretNameValid;

	@Schema(title = "NetAttachDef이름 유효성 여부")
	private boolean netAttachDefNameValid;

	@Schema(title = "PV 설정 여부")
	private boolean isPVConfig;

	@Schema(title = "cluster 지원 여부")
	private boolean clusterSupported = true;

	@Schema(title = "gpu 유효성 여부")
	private boolean gpuValid;

	@Schema(title = "stcp 유효성 여부")
	private boolean sctpValid;

	@Schema(title = "Multi-nic 유효성 여부")
	private boolean multiNicValid;

	@Schema(title = "Sriov 유효성 여부")
	private boolean sriovValid;

	@Schema(title = "TTLAfterFinished 유효성 여부")
	private boolean ttlAfterFinishedValid;

	/** 4.0.2 신규 추가 리소스 유효성 체크 **/
	@Schema(title = "Service 이름 유효성 여부")
	private boolean serviceNameValid;

	@Schema(title = "Ingress 이름 유효성 여부")
	private boolean ingressNameValid;

	@Schema(title = "Persistent Volume Claim 이름 유효성 여부")
	private boolean pvcNameValid;

	@Schema(title = "Service Account 이름 유효성 여부")
	private boolean serviceAccountNameValid;

	@Schema(title = "Role 이름 유효성 여부")
	private boolean roleNameValid;

	@Schema(title = "RoleBinding 이름 유효성 여부")
	private boolean roleBindingNameValid;

	@Schema(title = "CustomObject 이름 유효성 여부")
	private boolean customObjectNameValid;

	@Schema(title = "Package 이름 유효성 여부")
	private boolean packageNameValid;

	@Schema(title = "ConfigMap 데이터 유효성 여부")
	private boolean configMapDataValid;

	@Schema(title = "Secret 데이터 유효성 여부")
	private boolean secretDataValid;

	@Schema(title = "Ingress HostPath 사용중 여부")
	private boolean ingressHostPathValid;

	@Schema(title = "Ingress Host 사용중 여부")
	private boolean ingressHostValid;

	@Schema(title = "Ingress HostPath 길이 유효성 여부")
	private boolean ingressPathLongValid;

	@Schema(title = "Ingress Host 길이 유효성 여부")
	private boolean ingressHostLongValid;

	@Schema(title = "Ingress 이름규칙 유효성 여부")
	private boolean ingressNameSpecValid;

	@Schema(title = "HPA 이름규칙 유효성 여부")
	private boolean hpaNameValid;


}
