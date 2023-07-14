package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.commons.vo.HasUpserterVO;

@Getter
@Setter
@Schema(title = "서비스 사용자 모델")
@EqualsAndHashCode
public class ServiceUserVO extends HasUpserterVO {

    @Schema(title = "서비스 번호 (워크스페이스)")
    private Integer serviceSeq;

    @Schema(title = "사용자 번호")
    private Integer userSeq;

    @Schema(title = "사용자 아이디")
    private String userId;

    @Schema(title = "사용자 이름")
    private String userName;

    @Schema(title = "사용자 워크스페이스 권한")
    private UserGrant userGrant;


}