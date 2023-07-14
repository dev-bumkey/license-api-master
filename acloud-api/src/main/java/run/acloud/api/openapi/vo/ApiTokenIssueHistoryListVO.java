package run.acloud.api.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API 토큰 발급 이력 목록 모델")
public class ApiTokenIssueHistoryListVO implements Serializable {

	private static final long serialVersionUID = 1745559282732778190L;

	@Schema(title = "API 토큰 발급 이력 목록")
	private List<ApiTokenIssueHistoryVO> items;

	@Schema(title = "전체 데이터 갯수")
	private Integer totalCount;

	@Schema(title = "현재 페이지")
	private Integer currentPage;

}
