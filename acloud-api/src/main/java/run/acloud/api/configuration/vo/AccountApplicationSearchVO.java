package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.PagingVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "플랫폼 신청 검색 모델")
@Deprecated
public class AccountApplicationSearchVO implements Serializable {

	private static final long serialVersionUID = -5412480946863569380L;

	@Schema(title = "defaultTimezone")
	private String defaultTimezone;

	@Schema(title = "paging")
	private PagingVO paging;

}
