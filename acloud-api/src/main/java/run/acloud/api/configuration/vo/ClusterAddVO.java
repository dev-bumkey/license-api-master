package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.AuthType;
import run.acloud.api.resource.enums.ClusterType;
import run.acloud.api.resource.enums.CubeType;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "클러스터 생성 모델")
public class ClusterAddVO extends HasUseYnVO {

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private Integer clusterSeq;

	@Schema(title = "프로바이더 계정 번호")
	private Integer providerAccountSeq;

	@Schema(title = "프로바이더 계정 정보", requiredMode = Schema.RequiredMode.REQUIRED)
	private ProviderAccountVO providerAccount;

	@Schema(title = "빌링 프로바이더 계정 번호")
	private Integer billingProviderAccountSeq;

	@Schema(title = "계정 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer accountSeq;

	@Schema(title = "서비스 번호")
	private Integer serviceSeq;

	@Schema(title = "클러스터 타입", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private ClusterType clusterType = ClusterType.CUBE;

	@Schema(title = "클러스터 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String clusterName;

	@Schema(title = "클러스터 설명", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String description;

	@Schema(title = "리젼 코드 : 프로바이더의 리젼목록이 null인 경우 사용자입력", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String regionCode;

	@Schema(title = "클러스터 상태")
	private String clusterState;

	@Schema(title = "클러스터 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String clusterId;

	@Schema(title = "DockerProject ID : providerCode가 GCP일 경우 입력")
	private String billingGroupId;

	@Schema(title = "큐브 클러스터 타입 : 클러스터 타입이 CUBE인 경우 입력")
	private CubeType cubeType;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private AuthType authType = AuthType.CERT;

	@Schema(title = "Master URL : 클러스터 타입이 CUBE인 경우 입력")
	private String apiUrl;

	@Schema(title = "User id : 설치 유형이 CERT일 경우 입력")
	private String apiKey;

	@Schema(title = "User password | Barer token : 설치 유형이 TOKEN일 경우 barer token, CERT일 경우 user password")
	private String apiSecret;

	@Schema(title = "Certificate authority data : 설치 유형이 CERT일 경우 입력. 보통 client.crt")
	private String clientAuthData;

    @Schema(title = "Certificate authority data : 설치 유형이 CERT일 경우 입력. 보통 client.key")
	private String clientKeyData;

	@Schema(title = "클러스터 CA 인증서 : 설치 유형이 CERT일 경우 입력")
	private String serverAuthData;

	@Schema(title = "큐브 클러스터에 서버생성 시 Ingress Path를 지정하면, Ingress Host + Path로 외부에서 접근가능.")
	@JsonIgnore
	@Deprecated
	private String ingressHost;

	@Schema(title = "Ingress Controller 접근 호스트 정보. chartType: nginx-ingress configMap의 annotation에 acornsoft.io/ingress-url 키에 저장, key : ingress controller class name, value : 호스트 주소(protocol 제외)")
	private List<Map<String, String>> ingressHosts;

	@Schema(title = "Ingress 지원 여부")
	private String ingressSupported;

	@Schema(title = "LoadBalancer 지원 여부")
	private String loadbalancerSupported;

	@Schema(title = "Persistent Volume 지원 여부")
	private String persistentVolumeSupported;

	@Schema(title = "NodePort 지원 여부")
	private String nodePortSupported;

	@Schema(title = "NodePort URL", description = "NodePort를 지원할 경우 필수")
	private String nodePortUrl;

	@Schema(title = "NodePort 대역대", description = "NodePort를 지원할 경우 필수", example = "30000-32767")
	private String nodePortRange;

	@Schema(title = "Kubernetes 버전")
	private String k8sVersion;

}

