package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.AuthType;
import run.acloud.api.resource.enums.CubeType;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.EnumSet;

@Getter
@Setter
@Schema(title="CubeTypeModel", description="큐브 클러스터 타입 모델")
public class CubeTypeVO extends HasUseYnVO {

	@Schema(title = "큐브 클러스터 타입", requiredMode = Schema.RequiredMode.REQUIRED)
	private CubeType cubeType;

	@Schema(title = "ingress 지원여부", requiredMode = Schema.RequiredMode.REQUIRED)
	private String ingressSupported = "Y";

	@Schema(title = "Load Balancer 지원여부", requiredMode = Schema.RequiredMode.REQUIRED)
	private String loadbalancerSupported = "Y";

	@Schema(title = "Node Port 지원여부", requiredMode = Schema.RequiredMode.REQUIRED)
	private String nodePortSupported = "Y";

	@Schema(title = "PersistentVolume 지원여부", requiredMode = Schema.RequiredMode.REQUIRED)
	private String persistentVolumeSupported = "Y";

	@Schema(title = "지원하는 인증 타입", requiredMode = Schema.RequiredMode.REQUIRED)
	public EnumSet<AuthType> getSupportAuthTypes() {
		if (cubeType == null) {
			return null;
		} else {
			return cubeType.getSupportAuthTypes();
		}
	}

	public void setSupported(ProviderCode providerCode){
		if(this.cubeType == CubeType.MANAGED){
			this.setLoadbalancerSupported("N");
		}
		if (providerCode != null) {
			// bare-metal에서 설치된 OpenShift일 경우 LB 사용 불가
			if (providerCode.isBaremetal() && this.cubeType.isOpenShift()) {
				this.setLoadbalancerSupported("N");
			}
		}
		// NodePort 는 무조건 지원하도록 주석 처리
//		if(this.cubeType != CubeType.MANAGED){
//			this.setNodePortSupported("N");
//		}
	}
}
