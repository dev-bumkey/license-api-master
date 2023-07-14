package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "서비스맵그룹 모델")
public class ServicemapGroupVO extends HasUseYnVO {

	@Schema(title = "서비스맵그룹순번")
	private Integer servicemapGroupSeq;

	@Schema(title = "서비스순번(워크스페이스)")
	private Integer serviceSeq;

	@Schema(title = "서비스맵그룹명")
	private String servicemapGroupName;

	@Schema(title = "Color Code")
	private String colorCode;

	@Schema(title = "정렬순서")
	private Integer sortOrder;
}
