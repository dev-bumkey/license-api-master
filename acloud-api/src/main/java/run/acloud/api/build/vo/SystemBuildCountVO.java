package run.acloud.api.build.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "빌드 갯수 모델")
@JsonPropertyOrder({"accountSeq", "buildCount"})
public class SystemBuildCountVO extends BaseVO {

	@Schema(title = "계정 번호")
	@JsonProperty("accountSeq")
	private Integer accountSeq;

	@Schema(title = "빌드 갯수")
	private int buildCount;

}