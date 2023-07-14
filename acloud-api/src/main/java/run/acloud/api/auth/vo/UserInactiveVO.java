package run.acloud.api.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInactiveVO {

	@Schema(title = "사용자 일련번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer userSeq;

	@Schema(title = "사용자 비활성 여부", allowableValues = {"Y","N"})
	private String inactiveYn = "N";

}
