package run.acloud.api.configuration.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.configuration.dao.IClusterAccessAuthMapper;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.enums.ClusterAccessAuthType;
import run.acloud.api.configuration.enums.ClusterAddonType;
import run.acloud.api.configuration.vo.ClusterAccessAuthVO;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.DateTimeUtils;
import run.acloud.commons.util.SignatureUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ClusterAccessAuthService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private SignatureUtils signatureUtils;

    /**
     * Cluster Access Authorization Secret 생성.
     * @param clusterSeq
     * @param ownerType
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public String createClusterAccessSecret(Integer clusterSeq, String ownerType) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        /**
         * 유효한 Cluster에 대한 요청인지 확인..
         */
        ClusterVO cluster = clusterDao.getClusterByUseYn(clusterSeq, "Y");
        if(cluster == null) {
            throw new CocktailException("cluster not found", ExceptionType.InvalidParameter);
        }

        // ownerType(addonType)에 대한 유효성 체크 후 Secret 조회.
        ownerType = StringUtils.upperCase(ownerType);
        List<ClusterAccessAuthVO> clusterAccess = findClusterAccessSecret(clusterSeq, ownerType);

        if(clusterAccess != null && clusterAccess.size() >= 1) {
            throw new CocktailException("cluster AccessKey Already Exists", ExceptionType.ClusterAccessKeyAlreadyExists);
        }

        // Key 생성.
        String genKey = this.generateSecret(cluster, ownerType);

        ClusterAccessAuthVO clusterAuth = new ClusterAccessAuthVO();

        clusterAuth.setClusterSeq(clusterSeq);
        clusterAuth.setAuthType(ClusterAccessAuthType.SECRET.toString());
        clusterAuth.setOwnerType(ownerType);
        clusterAuth.setAuthKey(genKey);
        clusterAuth.setUseYn("Y");

        // Default Expired Date = 10년 설정.
        Date date = new Date();
        date = DateTimeUtils.addDate(date, CommonConstants.CLUSTER_SECRET_DEFAULT_EXPIRED_YEAR, 0,0);
        String formattedDate = DateTimeUtils.getUtcTimeString(date, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
        clusterAuth.setExpired(formattedDate);

        int result = dao.addClusterAccessAuth(clusterAuth);

        if(result != 1) {
            throw new CocktailException("cluster AccessKey Insert Failure : " + result, ExceptionType.CommonCreateFail);
        }

        return genKey;
    }

    /**
     * cluster에 생성된 모든 Access Secret을 조회함.
     * @param clusterSeq
     * @return
     * @throws Exception
     */
    public List<ClusterAccessAuthVO> getClusterAccessSecrets(Integer clusterSeq) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        /**
         * 유효한 Cluster에 대한 요청인지 확인..
         */
        ClusterVO cluster = clusterDao.getClusterByUseYn(clusterSeq, "Y");
        if(cluster == null) {
            throw new CocktailException("cluster not found", ExceptionType.InvalidParameter);
        }

        List<ClusterAccessAuthVO> clusterAccess = findClusterAccessSecrets(clusterSeq);

        for(ClusterAccessAuthVO ca : clusterAccess) {
            String sub = ca.getAuthKey().substring(0,20);
            for(int i=0;i < 54;i++) { sub += "*"; }
            ca.setAuthKey(sub);
        }

        return clusterAccess;

    }

    /**
     * Cluster Access Authorization Secret 조회.
     *
     * @param clusterSeq
     * @param ownerType
     * @return
     * @throws Exception
     */
    public ClusterAccessAuthVO getClusterAccessSecret(Integer clusterSeq, String ownerType) throws Exception {
        return getClusterAccessSecret(null, clusterSeq, ownerType, true);
    }

    private ClusterAccessAuthVO getClusterAccessSecret(Integer clusterAuthSeq, Integer clusterSeq, String ownerType, boolean checkCluster) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        // ownerType에 대한 유효성 체크 후 Secret 조회.
        List<ClusterAccessAuthVO> clusterAccess;
        if(clusterAuthSeq != null) {
            clusterAccess = findClusterAccessSecret(clusterAuthSeq, clusterSeq, ownerType);
        }
        else {
            ownerType = StringUtils.upperCase(ownerType);
            clusterAccess = findClusterAccessSecret(clusterSeq, ownerType);
        }

        if(clusterAccess == null || clusterAccess.size() < 1) {
            throw new CocktailException("cluster AccessKey Not Found", ExceptionType.ClusterAccessKeyNotFound);
        }
        else if(clusterAccess.size() > 1) {
            throw new CocktailException("Too many cluster access key", ExceptionType.TooManyClusterAccessKey);
        }

        /**
         * 유효한 Cluster에 대한 요청인지 확인..
         */
        if (checkCluster) {
            ClusterVO cluster = clusterDao.getClusterByUseYn(clusterAccess.get(0).getClusterSeq(), "Y");
            if(cluster == null) {
                throw new CocktailException("cluster not found", ExceptionType.InvalidParameter);
            }
        }

        return clusterAccess.get(0);
    }

    /**
     * Cluster Access Authorization Secret 만료.
     * @param clusterSeq
     * @param ownerType
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public boolean expireClusterAccessSecret(Integer clusterAuthSeq, Integer clusterSeq, String ownerType) throws Exception {
        return this.expireClusterAccessSecret(clusterAuthSeq, clusterSeq, ownerType, true);
    }

    /**
     * Cluster Access Authorization Secret 만료.
     * @param clusterSeq
     * @param ownerType
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public boolean expireClusterAccessSecret(Integer clusterAuthSeq, Integer clusterSeq, String ownerType, boolean checkCluster) throws Exception {
        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        /**
         * Cluster Access Secret 조회 및 유효성 체크.
         */
        ClusterAccessAuthVO clusterAccessAuth = this.getClusterAccessSecret(clusterAuthSeq, clusterSeq, ownerType, checkCluster);

        /**
         * 조회된 데이터를 기반으로 만료 처리..
         */
        ClusterAccessAuthVO clusterAuth = new ClusterAccessAuthVO();
        clusterAuth.setClusterAuthSeq(clusterAccessAuth.getClusterAuthSeq());
        clusterAuth.setUseYn("N");
        int result = dao.editClusterAccessAuth(clusterAuth);

        if(result != 1) {
            throw new CocktailException("cluster AccessKey Update Failure : " + result, ExceptionType.CommonCreateFail);
        }

        return true;
    }

    /**
     * Hash Based Secret 생성.
     * @param cluster
     * @param ownerType
     * @return
     * @throws Exception
     */
    private String generateSecret(ClusterVO cluster, String ownerType) throws Exception {
        if(cluster == null) {
            throw new CocktailException("cluster not found", ExceptionType.InvalidParameter);
        }

        String payload = cluster.getClusterId() + CommonConstants.CLUSTER_ACCESSKEY_DELIMITER
            + cluster.getProviderAccountSeq().toString() + CommonConstants.CLUSTER_ACCESSKEY_DELIMITER
            + ownerType;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        payload += timestamp.getTime();

        return signatureUtils.getSignedHmacKey(payload);
    }

    /**
     * Cluster에 등록된 전체 Access Secret을 조회.
     * @param clusterSeq
     * @return
     * @throws Exception
     */
    private List<ClusterAccessAuthVO> findClusterAccessSecrets(Integer clusterSeq) throws Exception {
        return findClusterAccessAuths(clusterSeq, ClusterAccessAuthType.SECRET);
    }

    private List<ClusterAccessAuthVO> findClusterAccessAuths(Integer clusterSeq, ClusterAccessAuthType clusterAccessAuthType) throws Exception {
        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        Map<String, Object> map = new HashMap<>();
        if(clusterSeq != null) {
            map.put("clusterSeq", clusterSeq);
        }
        if(clusterAccessAuthType != null) {
            map.put("authType", clusterAccessAuthType.toString());
        }
        map.put("useYn", "Y");

        return dao.getClusterAccessAuthList(map);
    }

    /**
     * ownerType(addonType) 에 대한 유효성 확인 후 ClusterAccessAuth 데이터 조회.
     * @param clusterAuthSeq
     * @return
     * @throws Exception
     */
    private List<ClusterAccessAuthVO> findClusterAccessSecret(Integer clusterAuthSeq) throws Exception {
        return findClusterAccessSecret(clusterAuthSeq, null, null);
    }

    private List<ClusterAccessAuthVO> findClusterAccessSecret(Integer clusterSeq, String ownerType) throws Exception {
        return findClusterAccessSecret(null, clusterSeq, ownerType);
    }

    private List<ClusterAccessAuthVO> findClusterAccessSecret(Integer clusterAuthSeq, Integer clusterSeq, String ownerType) throws Exception {
        return findClusterAccessAuth(clusterAuthSeq, clusterSeq, ownerType, ClusterAccessAuthType.SECRET);
    }

    private List<ClusterAccessAuthVO> findClusterAccessAuth(Integer clusterAuthSeq, Integer clusterSeq, String ownerType, ClusterAccessAuthType clusterAccessAuthType) throws Exception {
        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        // ClusterAuthSeq가 없을 경우 clusterSeq와 ownerType(addonType)은 반드시 Pair여야함..
        if(clusterAuthSeq == null) {
            if(StringUtils.isEmpty(ownerType) || clusterSeq == null) {
                throw new CocktailException("clusterSeq and ownerType are required as pair.", ExceptionType.InvalidParameter);
            }
        }

        // 유효한 ownerType인지 확인
        if(ownerType != null) {
            ownerType = StringUtils.upperCase(ownerType);
            if (ClusterAddonType.findClusterAddonType(ownerType) == null) {
                throw new CocktailException("Invalid Cluster Addon Type : " + ownerType, ExceptionType.InvalidParameter);
            }
        }

        Map<String, Object> map = new HashMap<>();
        if(clusterAuthSeq != null) {
            map.put("clusterAuthSeq", clusterAuthSeq);
        }
        if(clusterSeq != null) {
            map.put("clusterSeq", clusterSeq);
        }
        if(ownerType != null) {
            map.put("ownerType", ownerType);
        }
        if(clusterAccessAuthType != null) {
            map.put("authType", clusterAccessAuthType.toString());
        }
        map.put("useYn", "Y");

        return dao.getClusterAccessAuthList(map);
    }


    /**
     * Signature 생성.
     * @param method
     * @param clusterSeqMasking
     * @param clusterIdMd5
     * @param ownerType
     * @param timestamp
     * @param sigAlg
     * @return
     * @throws Exception
     */
    public String makeSignature(String method, String clusterSeqMasking, String clusterIdMd5, String ownerType, String timestamp, String sigAlg, String secret) throws Exception {
        String payload =
            method +
                CommonConstants.CLUSTER_ACCESS_SIGNATURE_DELIMITER +
            ownerType +
                CommonConstants.CLUSTER_ACCESS_SIGNATURE_DELIMITER +
            clusterIdMd5 +
                CommonConstants.CLUSTER_ACCESS_SIGNATURE_DELIMITER +
            clusterSeqMasking +
                CommonConstants.CLUSTER_ACCESS_SIGNATURE_DELIMITER +
            ownerType +
                CommonConstants.CLUSTER_ACCESS_SIGNATURE_DELIMITER +
            timestamp +
                CommonConstants.CLUSTER_ACCESS_SIGNATURE_DELIMITER +
            sigAlg;

//        log.debug("payload : " + payload);
        payload = URLEncoder.encode(payload, "UTF-8");
//        log.debug("payload url encode : " + payload);

        String signature = signatureUtils.getSignedHmacKey(secret, payload, sigAlg);
//        log.debug("signature : " + signature);

        return signature;
    }
//  ====================================================================================================================

    /**
     * Cluster Access Authorization List
     * @param params
     * @return
     * @throws Exception
     */
    public List<ClusterAccessAuthVO> getClusterAccessAuthList(Map<String, Object> params) throws Exception {
        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        // get Lists
        List<ClusterAccessAuthVO> clusterAccessList = dao.getClusterAccessAuthList(params);

        return clusterAccessList;
    }

    /**
     * Cluster Access Authorization 조회
     * @param clusterAuthSeq
     * @return
     * @throws Exception
     */
    public ClusterAccessAuthVO getClusterAccessAuth(Integer clusterAuthSeq) throws Exception {
        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        // get Account
        ClusterAccessAuthVO clusterAccessAuth = dao.getClusterAccessAuth(clusterAuthSeq);

        return clusterAccessAuth;
    }

    /**
     * Cluster Access Authorization 추가
     *
     * @param clusterAccessAuth
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addClusterAccessAuth(ClusterAccessAuthVO clusterAccessAuth) throws Exception {

        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        dao.addClusterAccessAuth(clusterAccessAuth);
    }

    /**
     * Cluster Access Authorization 수정
     *
     * @param clusterAccessAuth
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editClusterAccessAuth(ClusterAccessAuthVO clusterAccessAuth) throws Exception {

        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        dao.editClusterAccessAuth(clusterAccessAuth);
    }


    /**
     * Cluster Access Authorization 삭제
     *
     * @param clusterAuthSeq
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void removeClusterAccessAuth(Integer clusterAuthSeq) throws Exception {

        IClusterAccessAuthMapper dao = sqlSession.getMapper(IClusterAccessAuthMapper.class);

        dao.removeClusterAccessAuth(clusterAuthSeq);
    }

}
