package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스맵 그룹 맵핑 생성 모델")
public class ServicemapGroupMappingAddVO {

	@Schema(title = "서비스맵순번")
	private Integer servicemapSeq;

	@Schema(title = "서비스맵그룹순번")
	private Integer servicemapGroupSeq;

}