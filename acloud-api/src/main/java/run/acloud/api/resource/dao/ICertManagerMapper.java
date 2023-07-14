package run.acloud.api.resource.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.resource.vo.PublicCertificateAddVO;
import run.acloud.api.resource.vo.PublicCertificateDetailVO;
import run.acloud.api.resource.vo.PublicCertificateVO;

import java.util.List;

/**
 * Created by wschoi@acornsoft.io on 2017. 1. 12.
 */
public interface ICertManagerMapper {

    // 조회
    List<PublicCertificateVO> getPublicCertificates(
            @Param("accountSeq") Integer accountSeq
    );
    PublicCertificateDetailVO getPublicCertificate(
            @Param("accountSeq") Integer accountSeq,
            @Param("publicCertificateSeq") Integer publicCertificateSeq
    );



    // 등록
    int insertPublicCertificate(PublicCertificateAddVO PublicCertificateAdd);
    int insertPublicCertificateAccountMapping(
            @Param("accountSeq") Integer accountSeq,
            @Param("publicCertificateSeq") Integer publicCertificateSeq,
            @Param("creator") Integer creator
    );


    // 수정
    int updatePublicCertificate(PublicCertificateAddVO PublicCertificateAdd);



    // 삭제
    int deletePublicCertificate(
            @Param("publicCertificateSeq") Integer publicCertificateSeq
    );
    int deletePublicCertificateAccountMapping(
            @Param("accountSeq") Integer accountSeq,
            @Param("publicCertificateSeq") Integer publicCertificateSeq
    );
}
