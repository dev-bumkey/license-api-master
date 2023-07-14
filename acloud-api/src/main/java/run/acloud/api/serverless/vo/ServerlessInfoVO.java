package run.acloud.api.serverless.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.serverless.enums.ServerlessType;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "서비리스 정보 모델")
public class ServerlessInfoVO extends HasUseYnVO {
	
	@Schema(title = "서버리스 정보 순번")
	private Integer serverlessInfoSeq;

	@Schema(title = "서버리스 순번", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer serverlessSeq;

	@Schema(title = "서비리스 유형", requiredMode = Schema.RequiredMode.REQUIRED)
	private ServerlessType serverlessType;
	@Schema(title = "function 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String functionName;

	@Schema(title = "token", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	private String token;

}
