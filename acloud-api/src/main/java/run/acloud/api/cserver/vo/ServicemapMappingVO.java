package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.commons.vo.HasCreatorVO;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스맵 맵핑 모델")
public class ServicemapMappingVO extends HasCreatorVO {

	@Schema(title = "서비스맵순번")
	private Integer servicemapSeq;

	@Schema(title = "서비스순번(워크스페이스)")
	private Integer serviceSeq;

	@Schema(title = "서비스명(워크스페이스)")
	private String serviceName;

	@Schema(title = "서비스유형(워크스페이스)", allowableValues = {ServiceType.Names.NORMAL, ServiceType.Names.PLATFORM})
	private String serviceType;

	@Schema(title = "서비스맵그룹")
	private ServicemapGroupMappingVO servicemapGroup;

}