package run.acloud.api.pl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;
import run.acloud.commons.vo.PagingVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "파이프라인 실행 목록 검색 모델")
public class PlRunListSearchVO extends BaseVO {

    @Schema(title = "startDate")
    private String startDate;

    @Schema(title = "endDate")
    private String endDate;

    @Schema(title = "searchColumn")
    private String searchColumn;

    @Schema(title = "searchKeyword")
    private String searchKeyword;

    @Schema(title = "plSeq")
    private Integer plSeq;

    @Schema(title = "exceptRunningStatus", description = "제외할 파이프라인 실행 상태")
    private List<String> exceptRunningStatus;

    @Schema(title = "paging")
    private PagingVO paging;

}
