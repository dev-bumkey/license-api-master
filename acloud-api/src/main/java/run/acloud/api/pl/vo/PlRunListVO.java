package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "파이프라인 실행 목록 모델")
public class PlRunListVO extends BaseVO {

	@Schema(title = "파이프라인 실행 목록")
	private List<PlRunVO> items;

	@Schema(title = "더보기 중복 방지를 위한 데이터셋 위치 지정")
	private String maxId;

	@Schema(title = "신규 데이터 추가를 위한 데이터셋 위치 지정")
	private String newId;

	@Schema(title = "전체 데이터 갯수")
	private Integer totalCount;

	@Schema(title = "현재 페이지")
	private Integer currentPage;

}
