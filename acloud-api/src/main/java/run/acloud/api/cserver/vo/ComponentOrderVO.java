package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "컴포넌트순서 모델")
@Getter
@Setter
public class ComponentOrderVO {
	
	@Schema(title = "워크로드 그룹 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(0)
	private Integer workloadGroupSeq;
	
	@Schema(title = "정렬 순서", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(0)
	private int sortOrder;
	
}
