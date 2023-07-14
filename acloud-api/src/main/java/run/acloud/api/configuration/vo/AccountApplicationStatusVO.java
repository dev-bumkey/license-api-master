package run.acloud.api.configuration.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "디지털 서비스 신청 모델")
@Deprecated
public class AccountApplicationStatusVO implements Serializable {

	private static final long serialVersionUID = 3448188677412647185L;

	@Schema(title = "상태")
	private String status;

}
