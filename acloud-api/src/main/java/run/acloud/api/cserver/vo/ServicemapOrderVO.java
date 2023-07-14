package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "서비스맵 순서 모델")
@Getter
@Setter
public class 	ServicemapOrderVO {
	
	@Schema(title = "서비스맵순번", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(1)
	private Integer servicemapSeq;

	@Schema(title = "서비스순번(워크스페이스)", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(1)
	private Integer serviceSeq;

	@Schema(title = "서비스맵그룹순번", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(1)
	private Integer servicemapGroupSeq;

	@Schema(title = "정렬 순서", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(1)
	private int sortOrder;
	
}
