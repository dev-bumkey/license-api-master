package run.acloud.api.monitoring.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;
import run.acloud.commons.vo.PagingVO;

@Getter
@Setter
@Schema(description = "알림 사용자 검색 모델")
public class AlertUserSearchVO extends BaseVO {

    @Schema(title = "accountSeq")
    private Integer accountSeq;

    @Schema(title = "searchColumn")
    private String searchColumn;

    @Schema(title = "searchKeyword")
    private String searchKeyword;

    @Schema(title = "userName")
    private String userName;

    @Schema(title = "phoneNumber")
    private String phoneNumber;

    @Schema(title = "kakaoId")
    private String kakaoId;

    @Schema(title = "email")
    private String email;

    @Schema(title = "paging")
    private PagingVO paging;

}
