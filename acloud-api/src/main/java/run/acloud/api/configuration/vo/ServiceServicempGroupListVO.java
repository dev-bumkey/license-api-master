package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapGroupVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "워크스페이스 서비스맵그룹 목록 모델")
public class ServiceServicempGroupListVO extends ServiceVO {
	
	@Schema(title = "워크스페이스의 서비스맵 Group 목록")
	private List<ServicemapGroupVO> servicemapGroups;

}
