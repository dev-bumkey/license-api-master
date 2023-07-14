package run.acloud.api.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.acloud.api.configuration.enums.UserAuthType;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
@Tag(name = "Internal Platform", description = "내부호출용 플랫폼 관련 기능을 제공한다.")
@InHouse
@Slf4j
@RestController
@RequestMapping(value = "/internal/account")
public class InternalAccountController {

    @Autowired
    private AccountService accountService;


    /**
     * Keycloak 연동일 경우 USER_AUTH_TYPE=AD 강제 update 하는 api
     *
     * @return
     * @throws Exception
     */
    @Operation(summary = "플랫폼 AD 용으로 변경")
    @PutMapping(value = "/{accountSeq}/AD")
    public void editAccountInfoForAD(
            @Parameter(name = "accountSeq", description = "Account Seq", required = true) @PathVariable Integer accountSeq
    ) throws Exception {

        log.debug("[BEGIN] editAccountInfoForAD");


        AccountVO account = new AccountVO();
        if (accountSeq == null) {
            account.setAccountSeq(1); // Default : account_seq = 1
        } else {
            account.setAccountSeq(accountSeq);
        }
//        account.setAccountCode("PAAS");
        account.setUserAuthType(UserAuthType.AD);
        account.setCreator(1);
        account.setUpdater(1);


        AccountVO result = accountService.getAccount(accountSeq);
        if (result == null) {
            throw new CocktailException("Platform not found.", ExceptionType.CommonNotFound);
        }else{
            accountService.editAccountInfoForAD(account);

        }

        log.debug("[END  ] editAccountInfoForAD");
    }

}
