package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.configuration.vo.ProviderAccountVO;

import java.util.List;
import java.util.Map;

/**
 * Created by wschoi@acornsoft.io on 2017. 1. 12.
 */
public interface IProviderAccountMapper {

    List<ProviderAccountVO> getGcpBillingAccounts();

    List<ProviderAccountVO> getProviderAccountByAccountId(ProviderAccountVO providerAccount) throws Exception;

    List<CodeVO> getProviderAccountUseTypes() throws Exception;

    List<ProviderAccountVO> getProviderAccounts(
            @Param("providerAccountSeq") Integer providerAccountSeq,
            @Param("accountUseType") String accountUseType,
            @Param("accountUseTypes") List<String> accountUseTypes,
            @Param("accountSeq") Integer accountSeq,
            @Param("serviceSeq") Integer serviceSeq,
            @Param("useYn") String useYn
    ) throws Exception;

    ProviderAccountVO getProviderAccount(@Param("providerAccountSeq") Integer providerAccountSeq) throws Exception;

    ProviderAccountVO getProviderAccountByClusterSeq(@Param("clusterSeq") Integer clusterSeq,
                                                     @Param("providerCode") String providerCode,
                                                     @Param("accountUseType") String accountUseType) throws Exception;

    void addProviderAccount(Map<String, Object> params) throws Exception;

    void addProviderAccount2(ProviderAccountVO providerAccount) throws Exception;

    void editProviderAccount(Map<String, Object> params) throws Exception;

    void editProviderAccount2(ProviderAccountVO providerAccount) throws Exception;

    void removeProviderAccount(ProviderAccountVO providerAccount) throws Exception;
    void deleteProviderAccount(ProviderAccountVO providerAccount) throws Exception;

    List<String> selectClustersUsingAccount(@Param("providerAccountSeq") Integer providerAccountSeq) throws Exception;
    List<String> selectClustersUsingMeteringAccount(@Param("providerAccountSeq") Integer providerAccountSeq) throws Exception;
    List<String> selectClustersUsingAccessKeyAccount(@Param("providerAccountSeq") Integer providerAccountSeq) throws Exception;
}
