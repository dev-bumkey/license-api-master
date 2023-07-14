package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

/**
 * @author dy79@acornsoft.io on 2017. 2. 1.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "워크로드그룹 모델")
public class WorkloadGroupVO extends HasUseYnVO {

	@Schema(title = "워크로드그룹순번")
	private Integer workloadGroupSeq;

	@Schema(title = "서비스맵순번", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer servicemapSeq;

	@Schema(title = "워크로드그룹명", requiredMode = Schema.RequiredMode.REQUIRED)
	@Size(min = 1)
	private String workloadGroupName;

	@Schema(title = "컬럼수", example = "1")
	private Integer columnCount;

	@Schema(title = "정렬순서")
	@Min(1)
	private Integer sortOrder;
}