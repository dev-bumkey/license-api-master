package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import run.acloud.api.configuration.vo.CubeTypeVO;
import run.acloud.api.configuration.vo.ProviderAccountVO;
import run.acloud.api.resource.enums.ClusterType;
import run.acloud.api.resource.enums.ProviderAccountType;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.commons.vo.BaseVO;

import java.util.EnumSet;
import java.util.List;

@Getter
@Setter
@Schema(title="ProviderModel", description="프로바이더 모델")
public class ProviderVO extends BaseVO {

	@Schema(title = "프로바이더 코드", requiredMode = Schema.RequiredMode.REQUIRED)
	private ProviderCode providerCode;
	
	@Schema(title = "프로바이더명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String providerName;
	
	@Schema(title = "설명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;
	
	@Schema(title = "리젼목록")
	private List<RegionVO> regions;
	
	@Schema(title = "지원하는 프로바이더 계정 타입", requiredMode = Schema.RequiredMode.REQUIRED)
	public EnumSet<ProviderAccountType> getSupportProviderAccountTypes() {
		if (providerCode == null) {
			return null;
		} else {
			return providerCode.getSupportProviderAccountTypes();
		}
	}
	
	@Schema(title = "지원하는 클러스터 타입", requiredMode = Schema.RequiredMode.REQUIRED)
	public EnumSet<ClusterType> getSupportClusterTypes() {
		if (providerCode == null) {
			return null;
		} else {
			return providerCode.getSupportClusterTypes();
		}
	}
	
	@Schema(title = "지원하는 큐브 클러스터 타입", requiredMode = Schema.RequiredMode.REQUIRED)
	public List<CubeTypeVO> getSupportCubeTypes() {
		if (providerCode == null) {
			return null;
		} else {
			return providerCode.getSupportCubeTypes();
		}
	}


	public ProviderVO copyProperties(ProviderAccountVO providerAccount){
		BeanUtils.copyProperties(providerAccount, this);
		return this;
	}
}
