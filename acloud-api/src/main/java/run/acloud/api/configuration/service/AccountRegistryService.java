package run.acloud.api.configuration.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.build.dao.IBuildMapper;
import run.acloud.api.build.vo.BuildVO;
import run.acloud.api.configuration.dao.IAccountRegistryMapper;
import run.acloud.api.configuration.enums.ImageRegistryType;
import run.acloud.api.configuration.vo.AccountRegistryVO;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.HarborRegistryPingVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AccountRegistryService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

	@Autowired
	private HarborRegistryFactoryService harborRegistryFactory;

    @Transactional(transactionManager = "transactionManager")
    public AccountRegistryVO addAccountRegistry(AccountRegistryVO accountRegistry) throws Exception {
        IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);

        /**
         * 기존 플랫폼에 등록되어 있는 레지스트리가 존재하는지 체크.
         */
        AccountRegistryVO currAccountRegistry = this.getAccountRegistry(accountRegistry.getAccountSeq());
        if (currAccountRegistry != null){
            throw new CocktailException("플랫폼에 이미 레지스트리가 존재 합니다.", ExceptionType.ExistsAlreadyRegistry, ExceptionBiz.ACCOUNT_REGISTRY);
        }

        // check validation
        this.checkParameterValidation(accountRegistry, true);

        // set parameter
        this.setParameter(accountRegistry);

        // convert to base64
        this.convertToBase64(accountRegistry, true, true);

        // provider에 따른 인증정보 체크 - url connect test
        ResultVO checkResult = this.checkConnection(accountRegistry, true, true);
        if (checkResult != null && checkResult.getHttpStatusCode() != HttpStatus.SC_OK) {
            throw new CocktailException(checkResult.getMessage(), ExceptionType.RegistryConnectionFail, checkResult.getMessage());
        }
        // 암호화
        this.encryptCertInfo(accountRegistry);

        // 플랫폼 레지스트리 등록
        dao.insertAccountRegistry(accountRegistry);

        // 등록된 정보 조회 하여 리턴
        AccountRegistryVO resultAccountRegistry = this.getAccountRegistry(null, accountRegistry.getAccountRegistrySeq(), false);

        return resultAccountRegistry;
    }

    @Transactional(transactionManager = "transactionManager")
    public AccountRegistryVO editAccountRegistry(AccountRegistryVO accountRegistry) throws Exception {
        IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);

        AccountRegistryVO currAccountRegistry = dao.getAccountRegistry(accountRegistry.getAccountSeq(), accountRegistry.getAccountRegistrySeq());
        if (currAccountRegistry == null) {
            throw new CocktailException("It is not exists the account registry.", ExceptionType.CommonUpdateFail, ExceptionBiz.ACCOUNT_REGISTRY, "the registry not found.");
        } else {
            // check validation
            this.checkParameterValidation(accountRegistry, false);

            // set parameter
            this.setParameter(accountRegistry);

            // convert to base64
            this.convertToBase64(accountRegistry, false, true);

            // 복호화
            this.decryptCertInfo(currAccountRegistry);

            // provider에 따른 인증정보 체크 - url connect test
            if (!StringUtils.equals(accountRegistry.getRegistryUrl(), currAccountRegistry.getRegistryUrl()) // url 상이할 경우
                    || !StringUtils.isAllBlank(accountRegistry.getAccessId(), accountRegistry.getAccessSecret()) // id, pw가 빈값이 아닐 경우
                    || !StringUtils.equals(currAccountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificateUseYn()) // 사설인증서 사용 유무 값이 상이할 경우
                    || (BooleanUtils.toBoolean(accountRegistry.getPrivateCertificateUseYn()) && StringUtils.isNotBlank(accountRegistry.getPrivateCertificate())) // 사설인증서 사용 유무 값이 'Y'이고, 사설인증서 값이 빈값이 아닐 경우
                    || !StringUtils.equals(currAccountRegistry.getInsecureYn(), accountRegistry.getInsecureYn()) // insecure 사용 유무 값이 상이할 경우
            ) {
                // if empty, set current value
                if (StringUtils.isBlank(accountRegistry.getAccessId())) {
                    accountRegistry.setAccessId(currAccountRegistry.getAccessId());
                }
                if (StringUtils.isBlank(accountRegistry.getAccessSecret())) {
                    accountRegistry.setAccessSecret(currAccountRegistry.getAccessSecret());
                }
                if (BooleanUtils.toBoolean(accountRegistry.getPrivateCertificateUseYn())) {
                    if (StringUtils.isNotBlank(accountRegistry.getPrivateCertificate())) {
                        accountRegistry.setPrivateCertificate(currAccountRegistry.getPrivateCertificate());
                    }
                } else {
                    accountRegistry.setPrivateCertificate(null);
                }

                // check connection
                ResultVO checkResult = this.checkConnection(accountRegistry, false, true);
                if (checkResult != null && Optional.ofNullable(checkResult.getHttpStatusCode()).orElseGet(() ->Integer.valueOf("0")) != HttpStatus.SC_OK) {
                    throw new CocktailException(checkResult.getMessage(), ExceptionType.RegistryConnectionFail, checkResult.getMessage());
                }
            }

            // 암호화
            this.encryptCertInfo(accountRegistry);

            // 외부 레지스트리 수정
            dao.updateAccountRegistry(accountRegistry);
        }

        return currAccountRegistry;
    }

    public void getConnectionStatus(AccountRegistryVO accountRegistry, boolean decrypt) throws Exception {

        if (accountRegistry == null) {
            throw new CocktailException("accountRegistry is null.", ExceptionType.CommonInquireFail, ExceptionBiz.ACCOUNT_REGISTRY);
        }

        if (decrypt) {
            // 복호화
            this.decryptCertInfo(accountRegistry);
        }

        // check connection
        ResultVO checkResult = this.checkConnection(accountRegistry, false, false);
        if (checkResult != null && Optional.ofNullable(checkResult.getHttpStatusCode()).orElseGet(() ->Integer.valueOf("0")).intValue() == HttpStatus.SC_OK) {
            accountRegistry.setStatus("Y");
        } else {
            accountRegistry.setStatus("N");
        }
    }

    public ResultVO checkConnection(AccountRegistryVO accountRegistry, boolean isAdd, boolean isThrow) throws Exception {

        ResultVO result = new ResultVO();

        if (accountRegistry != null) {
//            result = this.checkConnectionWithHarbor(accountRegistry);
            if (accountRegistry.getProvider() == ImageRegistryType.HARBOR) {
                result = this.checkConnection(accountRegistry);
            } else {
                throw new CocktailException("Provider is not supported.", ExceptionType.CommonNotSupported, accountRegistry.getProvider());
            }
        }

        return result;
    }

    public ResultVO checkConnection(AccountRegistryVO accountRegistry) throws Exception {

        IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(accountRegistry);
        return harborRegistryService.getCurrentUserPermissionsStatus(null, null);

    }

    public ResultVO checkConnectionWithHarbor(AccountRegistryVO accountRegistry) throws Exception {

        ResultVO result;

        /**
         * 칵테일 설치시 제공하는 harbor의 check api를 사용하여 인증정보 체크
         * harbor ~1.7 과 1.8~ 의 api가 상이하고 harbor 버전을 알 수가 없어 아래와 같이 처리
         */
        // harbor 1.8이상
        // harbor api client
        IHarborRegistryService harborRegistryService = harborRegistryFactory.getService();
        HarborRegistryPingVO pingRegistry = new HarborRegistryPingVO();
        pingRegistry.setAccessKey(accountRegistry.getAccessId());
        pingRegistry.setAccessSecret(accountRegistry.getAccessSecret());

        pingRegistry.setType(accountRegistry.getProvider().getValue());
        pingRegistry.setUrl(accountRegistry.getRegistryUrl());
        pingRegistry.setInsecure(BooleanUtils.toBoolean(accountRegistry.getInsecureYn()));

        result = this.registriesPing(harborRegistryService, pingRegistry);

        if (result.getHttpStatusCode() != HttpStatus.SC_OK) {

            // http status는 200 정상이나 api가 존재하지 않아 오류가 발생할 시 0 으로 리턴
            // 이 경우에 다시 1.7 버전의 api를 호출하여 줌 (harbor 만 체크 가능)
            if (accountRegistry.getProvider() == ImageRegistryType.HARBOR && result.getHttpStatusCode() == 0) {
                // harbor 1.7
                result = this.targetPing(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), pingRegistry.isInsecure().booleanValue());

            }
        }

        return result;
    }

    /**
     * Harbor 1.8 ~ 에서 사용하는 레파지토리 접속 상태 체크 api
     *
     * @param harborRegistryService
     * @param registryPingReq
     * @return
     * @throws Exception
     */
    public ResultVO registriesPing(IHarborRegistryService harborRegistryService, HarborRegistryPingVO registryPingReq) throws Exception {
        ResultVO result = new ResultVO();
        int status = harborRegistryService.registriesPing(registryPingReq);
        result.setHttpStatusCode(status);
        switch (status) {
            case HttpStatus.SC_OK:
                result.setMessage("success");
                break;
            case HttpStatus.SC_BAD_REQUEST:
                result.setMessage("No proper registry information provided.");
                break;
            case HttpStatus.SC_UNAUTHORIZED:
                result.setMessage("Registry not found (when registry is provided by ID).");
                break;
            case HttpStatus.SC_NOT_FOUND:
                result.setMessage("Target not found.");
                break;
            case HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE:
                result.setMessage("The Media Type of the request is not supported, it has to be \"application/json\"");
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                result.setMessage("Unexpected internal errors.");
                break;
            default:
                result.setMessage("Unexpected internal errors.");
                break;
        }

        return result;
    }

    /**
     * Harbor ~ 1.7 에서 에서 사용하는 레파지토리 접속 상태 체크 api
     * - Harbor만 지원
     *
     * @param url
     * @param username
     * @param password
     * @param insecure
     * @return
     * @throws Exception
     */
    public ResultVO targetPing(String url, String username, String password, boolean insecure) throws Exception {

        ResultVO result = new ResultVO();
        int status = harborRegistryFactory.getV1().targetPing(url, username, password, insecure);
        result.setHttpStatusCode(status);
        switch (status) {
            case HttpStatus.SC_OK:
                result.setMessage("success");
                break;
            case HttpStatus.SC_BAD_REQUEST:
                result.setMessage("Target id is invalid/ endpoint is needed/ invaild URL/ network issue.");
                break;
            case HttpStatus.SC_UNAUTHORIZED:
                result.setMessage("User need to log in first or wrong username/password for remote target.");
                break;
            case HttpStatus.SC_NOT_FOUND:
                result.setMessage("Target not found.");
                break;
            case HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE:
                result.setMessage("The Media Type of the request is not supported, it has to be \"application/json\"");
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                result.setMessage("Unexpected internal errors.");
                break;
            default:
                result.setMessage("Unexpected internal errors.");
                break;
        }

        return result;
    }

    /**
     * 플랫폼에 등록된 레지스트리 정보를 가져오는 메서드.</br>
     * 기본적으로 cert 값은 포함되지 않는다.</br>
     * cert 포함을 위해선 getAccountRegistry( Integer accountSeq, Integer accountRegistrySeq, boolean containCert )  메서드 사용.
     *
     * @param accountSeq 플랫폼 아이디
     * @return
     * @throws Exception
     */
    public AccountRegistryVO getAccountRegistry(Integer accountSeq) {
        return this.getAccountRegistry(accountSeq, null, false);
    }

    public AccountRegistryVO getAccountRegistry( Integer accountSeq, Integer accountRegistrySeq, boolean containCert ) {
        IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);
        AccountRegistryVO accountRegistry = dao.getAccountRegistry(accountSeq, accountRegistrySeq);

        // cert값 포함시에는 복호화 하여 셋팅, 포함 안할때는 값 제거
        if (containCert){
            this.decryptCertInfo(accountRegistry);
        }else{
            this.clearCertInfo(accountRegistry);
        }
        return accountRegistry;
    }

    @Transactional(transactionManager = "transactionManager")
    public AccountRegistryVO deleteAccountRegistry(Integer accountRegistrySeq) throws CocktailException {
        IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);

        AccountRegistryVO currAccountRegistry = dao.getAccountRegistry(null, accountRegistrySeq);
        if (currAccountRegistry == null) {
            throw new CocktailException("It is not exists the account registry.", ExceptionType.CommonUpdateFail, ExceptionBiz.ACCOUNT_REGISTRY, "the external registry not found.");
        } else {

            IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
            //TODO, registry에 해당하는 build 리스트 조회??
            List<BuildVO> builds = buildDao.getBuildList(currAccountRegistry.getAccountSeq(), null, null, null, currAccountRegistry.getRegistryUrl());

            if (CollectionUtils.isNotEmpty(builds)) {
                throw new CocktailException("레지스트리가 빌드를 포함하고 있어 삭제될 수 없습니다. 빌드를 먼저 삭제해 주세요.", ExceptionType.RegistryContainsBuild, currAccountRegistry.getName());
            } else {
                // delete account registry (use_yn = 'N')
                dao.deleteAccountRegistry(null, accountRegistrySeq);
            }
        }

        return currAccountRegistry;
    }

    private void convertToBase64(AccountRegistryVO accountRegistry, boolean isAdd, boolean isThrow) throws CocktailException {
        String errMsg = "";
        if (BooleanUtils.toBoolean(accountRegistry.getPrivateCertificateUseYn())) {
            if (StringUtils.isNotBlank(accountRegistry.getPrivateCertificate())) {
                // convert to base64
                if (StringUtils.contains(accountRegistry.getPrivateCertificate(), "-----BEGIN ")) {
                    accountRegistry.setPrivateCertificate(Base64Utils.encodeToString(accountRegistry.getPrivateCertificate().getBytes()));
                } else {
                    try {
                        String decodedStr = new String(Base64Utils.decodeFromString(accountRegistry.getPrivateCertificate()), StandardCharsets.UTF_8);
                        if (!StringUtils.contains(decodedStr, "-----BEGIN ")) {
                            errMsg = "Invalid Private Certificate Data!!";
                            if (isThrow) {
                                throw new CocktailException(errMsg, ExceptionType.InvalidRegistryCertification, errMsg);
                            }
                        }
                    } catch (CocktailException e) {
                        errMsg = "Invalid Private Certificate Data - bad base64 encoding!!";
                        if (isThrow) {
                            throw new CocktailException(errMsg, ExceptionType.InvalidRegistryCertification, errMsg);
                        }
                    }
                }
            } else {
                if (isAdd) {
                    errMsg = "If you have checked whether to use a private certificate, please enter your private certificate.";
                    if (isThrow) {
                        throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                    }
                }
            }
        }
    }

    private void checkParameterValidation(AccountRegistryVO accountRegistry, boolean isAdd) throws CocktailException {
        if (accountRegistry != null) {
            if (isAdd) {
                accountRegistry.setAccountRegistrySeq(null);
            } else {
                ExceptionMessageUtils.checkParameterRequired("accountRegistrySeq", accountRegistry.getAccountRegistrySeq());
            }
            ExceptionMessageUtils.checkParameterRequired("accountSeq", accountRegistry.getAccountSeq());
            ExceptionMessageUtils.checkParameterRequired("provider", accountRegistry.getProvider());
            ExceptionMessageUtils.checkParameter("name", accountRegistry.getName(), 50, true);
            ExceptionMessageUtils.checkParameter("description", accountRegistry.getDescription(), 300, false);
            this.checkParamterUrlValidation(accountRegistry.getRegistryUrl());
            ExceptionMessageUtils.checkParameter("name", accountRegistry.getName(), 255, true);
        } else {
            throw new CocktailException("AccountRegistry is null!!", ExceptionType.InvalidParameter, ExceptionBiz.ACCOUNT_REGISTRY);
        }
    }

    public void checkParamterUrlValidation(String registryUrl) throws CocktailException {
        ExceptionMessageUtils.checkParameter("registryUrl", registryUrl, 255, true);
        if (!Utils.isValidUrlHttp(registryUrl)) {
            throw new CocktailException("Invalid endpoint url!!", ExceptionType.InvalidParameter, ExceptionBiz.ACCOUNT_REGISTRY);
        }
    }

    public void setParameter(AccountRegistryVO accountRegistry) {
        if (accountRegistry != null) {
            accountRegistry.setRegistryUrl(StringUtils.removeEnd(StringUtils.trim(accountRegistry.getRegistryUrl()), "/"));
            accountRegistry.setName(StringUtils.trim(accountRegistry.getName()));
            accountRegistry.setAccessId(StringUtils.trim(accountRegistry.getAccessId()));
            accountRegistry.setAccessSecret(StringUtils.trim(accountRegistry.getAccessSecret()));
            accountRegistry.setPrivateCertificateUseYn(StringUtils.defaultString(accountRegistry.getPrivateCertificateUseYn(), "N"));
            if (!BooleanUtils.toBoolean(accountRegistry.getPrivateCertificateUseYn())) {
                accountRegistry.setPrivateCertificate(null);
            } else {
                accountRegistry.setPrivateCertificate(StringUtils.trim(accountRegistry.getPrivateCertificate()));
            }
            accountRegistry.setInsecureYn(StringUtils.defaultString(accountRegistry.getInsecureYn(), "N"));
        }
    }

    // 암호화
    private void encryptCertInfo(AccountRegistryVO accountRegistry) {
        if (accountRegistry != null) {
            accountRegistry.setAccessId(CryptoUtils.encryptAES(accountRegistry.getAccessId()));
            accountRegistry.setAccessSecret(CryptoUtils.encryptAES(accountRegistry.getAccessSecret()));
            if (StringUtils.isNotBlank(accountRegistry.getPrivateCertificate())) {
                accountRegistry.setPrivateCertificate(CryptoUtils.encryptAES(accountRegistry.getPrivateCertificate()));
            }
        }
    }

    // 복호화
    private void decryptCertInfo(AccountRegistryVO accountRegistry) {
        if (accountRegistry != null) {
            accountRegistry.setAccessId(CryptoUtils.decryptAES(accountRegistry.getAccessId()));
            accountRegistry.setAccessSecret(CryptoUtils.decryptAES(accountRegistry.getAccessSecret()));
            if (StringUtils.isNotBlank(accountRegistry.getPrivateCertificate())) {
                accountRegistry.setPrivateCertificate(CryptoUtils.decryptAES(accountRegistry.getPrivateCertificate()));
            }
        }
    }

    public void clearCertInfo(AccountRegistryVO accountRegistry) {
        if (accountRegistry != null) {
            accountRegistry.setAccessId(Optional.ofNullable(CryptoUtils.decryptAES(accountRegistry.getAccessId())).orElseGet(() ->accountRegistry.getAccessId()));
            accountRegistry.setAccessSecret(null);
            accountRegistry.setPrivateCertificate(null);
        }
    }
}
