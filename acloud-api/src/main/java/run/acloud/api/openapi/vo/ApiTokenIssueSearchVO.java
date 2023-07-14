package run.acloud.api.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.PagingVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "API 토큰 발급 검색 모델")
public class ApiTokenIssueSearchVO implements Serializable {

    private static final long serialVersionUID = -1296095906003496803L;

    @Schema(title = "startDate")
    private String startDate;

    @Schema(title = "endDate")
    private String endDate;

    @Schema(title = "searchColumn")
    private String searchColumn;

    @Schema(title = "searchKeyword")
    private String searchKeyword;

    @Schema(title = "historyState")
    private String historyState;

    @Schema(title = "resultCode", allowableValues = {"SUCCESS", "FAILURE"})
    private String resultCode;

    @Schema(title = "accountSeq")
    private Integer accountSeq;

    @Schema(title = "withApi", hidden = true)
    private boolean withApi = false;

    @Schema(title = "systemUserSeq", hidden = true)
    private Integer systemUserSeq;

    @Schema(title = "userTimezone", hidden = true)
    private String userTimezone;

    @Schema(title = "paging")
    private PagingVO paging;

}
