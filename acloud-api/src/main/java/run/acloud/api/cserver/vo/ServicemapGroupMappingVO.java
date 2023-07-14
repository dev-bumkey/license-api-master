package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUpserterVO;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스맵 그룹 맵핑 모델")
public class ServicemapGroupMappingVO extends HasUpserterVO {

	@Schema(title = "서비스순번(워크스페이스)")
	private Integer serviceSeq;

	@Schema(title = "서비스맵순번")
	private Integer servicemapSeq;

	@Schema(title = "서비스맵그룹순번")
	private Integer servicemapGroupSeq;

	@Schema(title = "서비스맵그룹명")
	private String servicemapGroupName;

	@Schema(title = "서비스맵정렬순서")
	private Integer sortOrder;


}