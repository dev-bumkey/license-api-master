package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.AccountType;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "계정 서비스 모델")
public class AccountServiceVO extends BaseVO {

    @Schema(title = "계정 번호")
    private Integer accountSeq;

    @Schema(title = "계정 유형")
    private AccountType accountType = AccountType.CCO;

    @Schema(title = "계정 이름")
    private String accountName;

    @Schema(title = "서비스 목록")
    private List<ServiceVO> services;
}