package run.acloud.api.cserver.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Schema(description = "서비스맵그룹 등록 모델")
public class ServicemapGroupAddVO extends HasUseYnVO {

	@Schema(title = "서비스맵그룹순번", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	private Integer servicemapGroupSeq;

	@Schema(title = "서비스순번(워크스페이스)")
	@NotNull
	private Integer serviceSeq;

	@Schema(title = "서비스맵그룹명")
	@Size(min = 1)
	private String servicemapGroupName;

	@Schema(title = "Color Code")
	@NotNull
	private String colorCode;

	@Schema(title = "정렬순서")
	@Min(1)
	private Integer sortOrder;
}
