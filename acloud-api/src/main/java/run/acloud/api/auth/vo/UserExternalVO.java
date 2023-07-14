package run.acloud.api.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@EqualsAndHashCode
public class UserExternalVO extends BaseVO {

	@Schema(title = "사용자 일련번호")
	private Integer userSeq;

	@Schema(title = "사용자 ID(email 형식)")
	private String userId;

	@Schema(title = "사용자 이름")
	private String userName;

	@Schema(title = "플랫폼 일련번호")
	private Integer accountSeq;

	@Schema(title = "생성 일시")
	protected String created;

	@Schema(title = "수정 일시")
	protected String updated;

}
