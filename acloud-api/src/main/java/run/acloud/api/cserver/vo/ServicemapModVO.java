package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.Map;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스맵 등록 모델")
public class ServicemapModVO extends HasUseYnVO {

	@Schema(title="서비스맵순번")
	@NotNull
	private Integer servicemapSeq;

	@Schema(title="서비스맵명")
	@Valid
	@NotNull
    @Size(min = 1, max = 50)
	private String servicemapName;

	@Schema(title="수정할 서비스맵그룹순번", description = "서비스맵그룹 수정시에만 사용")
	private Integer servicemapGroupSeq;

	@Schema(title="서비스맵그룹을 수정할 서비스순번(워크스페이스)", description = "서비스맵그룹 수정시에만 사용")
	private Integer serviceSeq;

	@Schema(name = "labels", description = "k8s namespace labels")
	private Map<String, String> labels;
	@Schema(name = "annotations", description = "k8s namespace annotations")
	private Map<String, String> annotations;

}