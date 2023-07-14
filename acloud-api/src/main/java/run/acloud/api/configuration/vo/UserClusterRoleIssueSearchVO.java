package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.PagingVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "사용자 클러스터 권한 발급 검색 모델")
public class UserClusterRoleIssueSearchVO implements Serializable {

    private static final long serialVersionUID = 2104545411175608766L;

    @Schema(title = "startDate")
    private String startDate;

    @Schema(title = "endDate")
    private String endDate;

    @Schema(title = "searchColumn")
    private String searchColumn;

    @Schema(title = "searchKeyword")
    private String searchKeyword;

    @Schema(title = "issueType")
    private String issueType;

    @Schema(title = "historyState")
    private String historyState;

    @Schema(title = "clusterSeq")
    private Integer clusterSeq;

    @Schema(title = "accountSeq")
    private Integer accountSeq;

    @Schema(title = "userSeq")
    private Integer userSeq;

    @Schema(title = "systemUserSeq")
    private Integer systemUserSeq;

    @Schema(title = "userTimezone")
    private String userTimezone;

    @Schema(title = "isUserBased")
    private Boolean isUserBased;

    @Schema(title = "paging")
    private PagingVO paging;

}
