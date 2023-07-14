package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.configuration.vo.AccountRegistryVO;

import java.util.List;

public interface IAccountRegistryMapper {

    // 조회
    List<AccountRegistryVO> getAccountRegistries(
            @Param("accountSeq") Integer accountSeq,
            @Param("name") String name,
            @Param("registryUrl") String registryUrl
    );

    AccountRegistryVO getAccountRegistry(
            @Param("accountSeq") Integer accountSeq,
            @Param("accountRegistrySeq") Integer accountRegistrySeq
    );


    // 등록
    int insertAccountRegistry(AccountRegistryVO accountRegistry);

    // 수정
    int updateAccountRegistry(AccountRegistryVO accountRegistry);

    // 삭제
    int removeAccountRegistry(
            @Param("accountRegistrySeq") Integer accountRegistrySeq,
            @Param("updater") Integer updater
    );
    int deleteAccountRegistry(
            @Param("accountSeq") Integer accountSeq,
            @Param("accountRegistrySeq") Integer accountRegistrySeq
    );
}
