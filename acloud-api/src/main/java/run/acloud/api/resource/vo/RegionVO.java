package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(title="RegionModel", description="리젼 모델")
public class RegionVO extends BaseVO {

	@Schema(title = "리젼코드", requiredMode = Schema.RequiredMode.REQUIRED)
	private String regionCode;
	
	@Schema(title = "리젼명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String regionName;
	
	@Schema(title = "설명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;
	
}
