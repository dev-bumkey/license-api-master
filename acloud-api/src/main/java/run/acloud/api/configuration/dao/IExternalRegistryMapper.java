package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.configuration.vo.ExternalRegistryDetailVO;
import run.acloud.api.configuration.vo.ExternalRegistryVO;

import java.util.List;

/**
 * Created by wschoi@acornsoft.io on 2017. 1. 12.
 */
public interface IExternalRegistryMapper {

    // 조회
    List<ExternalRegistryVO> getExternalRegistries(
            @Param("accountSeq") Integer accountSeq,
            @Param("serviceSeq") Integer serviceSeq,
            @Param("servicemapSeq") Integer servicemapSeq,
            @Param("name") String name,
            @Param("endpointUrl") String endpointUrl,
            @Param("registryName") String registryName
    );
    ExternalRegistryDetailVO getExternalRegistry(
            @Param("externalRegistrySeq") Integer externalRegistrySeq,
            @Param("serviceType") String serviceType
    );
    List<Integer> getExternalRegistrySeqsOfAccountMapping(
            @Param("accountSeq") Integer accountSeq
    );
    List<Integer> getExternalRegistrySeqsOfServiceMappingByAccount(
            @Param("accountSeq") Integer accountSeq
    );



    // 등록
    int insertExternalRegistry(ExternalRegistryVO externalRegistry);
    int insertExternalRegistryServiceMapping(
            @Param("serviceSeq") Integer serviceSeq,
            @Param("externalRegistrySeq") Integer externalRegistrySeq,
            @Param("creator") Integer creator
    );
    int insertExternalRegistryServiceMappings(
            @Param("serviceSeq") Integer serviceSeq,
            @Param("externalRegistrySeqs") List<Integer> externalRegistrySeqs,
            @Param("creator") Integer creator
    );
    int insertExternalRegistryAccountMapping(
            @Param("accountSeq") Integer accountSeq,
            @Param("externalRegistrySeq") Integer externalRegistrySeq,
            @Param("creator") Integer creator
    );


    // 수정
    int updateExternalRegistry(ExternalRegistryVO externalRegistry);



    // 삭제
    int removeExternalRegistry(
            @Param("externalRegistrySeq") Integer externalRegistrySeq,
            @Param("updater") Integer updater
    );
    int deleteExternalRegistry(
            @Param("externalRegistrySeqs") List<Integer> externalRegistrySeqs
    );
    int deleteExternalRegistryAccountMapping(
            @Param("accountSeq") Integer accountSeq,
            @Param("externalRegistrySeqs") List<Integer> externalRegistrySeqs
    );
    int deleteExternalRegistryServiceMappingOfAccount(
            @Param("accountSeq") Integer accountSeq
    );
    int deleteExternalRegistryServiceMappingByService(
            @Param("serviceSeq") Integer serviceSeq,
            @Param("externalRegistrySeqs") List<Integer> externalRegistrySeqs
    );
    int deleteExternalRegistryServiceMappings(
            @Param("externalRegistrySeqs") List<Integer> externalRegistrySeqs
    );
    int deleteExternalRegistryServiceMappingOfService(
            @Param("serviceSeq") Integer serviceSeq
    );
    int deleteExternalRegistryServiceMapping(
            @Param("externalRegistrySeq") Integer externalRegistrySeq,
            @Param("serviceSeqs") List<Integer> serviceSeqs
    );
}
