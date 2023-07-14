package run.acloud.api.configuration.service;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.build.dao.IBuildMapper;
import run.acloud.api.build.vo.BuildVO;
import run.acloud.api.configuration.dao.IExternalRegistryMapper;
import run.acloud.api.configuration.enums.ImageRegistryType;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.vo.AccountRegistryVO;
import run.acloud.api.configuration.vo.ExternalRegistryDetailVO;
import run.acloud.api.configuration.vo.ExternalRegistryVO;
import run.acloud.api.configuration.vo.ServiceVO;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.HarborRegistryPingVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExternalRegistryService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

	@Autowired
	private HarborRegistryFactoryService harborRegistryFactory;

    @Autowired
    private RegistryPropertyService registryProperties;

	@Autowired
	private ServiceService serviceService;

    @Autowired
    private AccountRegistryService accountRegistryService;


    @Transactional(transactionManager = "transactionManager")
    public ExternalRegistryDetailVO addExternalRegistry(ExternalRegistryVO externalRegistry) throws Exception {
        IExternalRegistryMapper erDao = sqlSession.getMapper(IExternalRegistryMapper.class);

        // check validation
        this.checkParameterValidation(externalRegistry, true);

        // set parameter
        this.setParameter(externalRegistry);

        // convert to base64
        this.convertToBase64(externalRegistry, true, true);

        // provider에 따른 인증정보 체크 - url connect test
        ResultVO checkResult = this.checkConnection(externalRegistry, true, true);
        if (checkResult != null && checkResult.getHttpStatusCode() != HttpStatus.SC_OK) {
            throw new CocktailException(checkResult.getMessage(), ExceptionType.ExternalRegistryConnectionFail, checkResult.getMessage());
        }
        // 암호화
        this.encryptCertInfo(externalRegistry);

        // 외부 레지스트리 등록
        erDao.insertExternalRegistry(externalRegistry);
        // 외부 레지스트리 플랫폼 맵핑 등록
        erDao.insertExternalRegistryAccountMapping(externalRegistry.getAccountSeq(), externalRegistry.getExternalRegistrySeq(), externalRegistry.getCreator());
        // 플랫폼 워크스페이스 추가
        ServiceVO platformService = serviceService.getPlatformService(externalRegistry.getAccountSeq(), null);
        erDao.insertExternalRegistryServiceMapping(platformService.getServiceSeq(), externalRegistry.getExternalRegistrySeq(), ContextHolder.exeContext().getUserSeq());

        return erDao.getExternalRegistry(externalRegistry.getExternalRegistrySeq(), ServiceType.NORMAL.getCode());
    }

    @Transactional(transactionManager = "transactionManager")
    public ExternalRegistryDetailVO editExternalRegistry(ExternalRegistryVO externalRegistry) throws Exception {
        IExternalRegistryMapper erDao = sqlSession.getMapper(IExternalRegistryMapper.class);

        ExternalRegistryDetailVO currExternalRegistry = erDao.getExternalRegistry(externalRegistry.getExternalRegistrySeq(), ServiceType.NORMAL.getCode());
        if (currExternalRegistry == null) {
            throw new CocktailException("It is not exists the external registry.", ExceptionType.CommonUpdateFail, ExceptionBiz.EXTERNAL_REGISTRY, "the external registry not found.");
        } else {
            // check validation
            this.checkParameterValidation(externalRegistry, false);

            // set parameter
            this.setParameter(externalRegistry);

            // convert to base64
            this.convertToBase64(externalRegistry, false, true);

            // 복호화
            this.decryptCertInfo(currExternalRegistry);

            // provider에 따른 인증정보 체크 - url connect test
            if (!StringUtils.equals(currExternalRegistry.getEndpointUrl(), externalRegistry.getEndpointUrl()) // url 상이할 경우
                    || !StringUtils.isAllBlank(externalRegistry.getAccessId(), externalRegistry.getAccessSecret()) // id, pw가 빈값이 아닐 경우
                    || !StringUtils.equals(currExternalRegistry.getPrivateCertificateUseYn(), externalRegistry.getPrivateCertificateUseYn()) // 사설인증서 사용 유무 값이 상이할 경우
                    || (BooleanUtils.toBoolean(externalRegistry.getPrivateCertificateUseYn()) && StringUtils.isNotBlank(externalRegistry.getPrivateCertificate())) // 사설인증서 사용 유무 값이 'Y'이고, 사설인증서 값이 빈값이 아닐 경우
                    || !StringUtils.equals(currExternalRegistry.getInsecureYn(), externalRegistry.getInsecureYn()) // insecure 사용 유무 값이 상이할 경우
            ) {
                // if empty, set current value
                if (StringUtils.isBlank(externalRegistry.getAccessId())) {
                    externalRegistry.setAccessId(currExternalRegistry.getAccessId());
                }
                if (StringUtils.isBlank(externalRegistry.getAccessSecret())) {
                    externalRegistry.setAccessSecret(currExternalRegistry.getAccessSecret());
                }
                if (BooleanUtils.toBoolean(externalRegistry.getPrivateCertificateUseYn())) {
                    if (StringUtils.isNotBlank(externalRegistry.getPrivateCertificate())) {
                        externalRegistry.setPrivateCertificate(currExternalRegistry.getPrivateCertificate());
                    }
                } else {
                    externalRegistry.setPrivateCertificate(null);
                }

                // check connection
                ResultVO checkResult = this.checkConnection(externalRegistry, false, true);
                if (checkResult != null && Optional.ofNullable(checkResult.getHttpStatusCode()).orElseGet(() ->Integer.valueOf("0")) != HttpStatus.SC_OK) {
                    throw new CocktailException(checkResult.getMessage(), ExceptionType.ExternalRegistryConnectionFail, checkResult.getMessage());
                }
            }

            // 암호화
            this.encryptCertInfo(externalRegistry);

            // 외부 레지스트리 수정
            erDao.updateExternalRegistry(externalRegistry);
        }

        return currExternalRegistry;
    }

    public void getConnectionStatus(ExternalRegistryVO externalRegistry, boolean decrypt) throws Exception {
        if (decrypt) {
            // 복호화
            this.decryptCertInfo(externalRegistry);
        }

        // check connection
        ResultVO checkResult = this.checkConnection(externalRegistry, false, false);
        if (checkResult != null && Optional.ofNullable(checkResult.getHttpStatusCode()).orElseGet(() ->Integer.valueOf("0")).intValue() == HttpStatus.SC_OK) {
            externalRegistry.setStatus("Y");
        } else {
            externalRegistry.setStatus("N");
        }
    }

    public ResultVO checkConnection(ExternalRegistryVO externalRegistry, boolean isAdd, boolean isThrow) throws Exception {

        ResultVO result = new ResultVO();

        if (externalRegistry != null) {
            // TODO: provider에 따른 인증정보 체크
            // TODO: url connect test
//            // 사설 인증서
//            if (BooleanUtils.toBoolean(externalRegistry.getPrivateCertificateUseYn())) {
//                if (StringUtils.isNotBlank(externalRegistry.getPrivateCertificate())) {
//                    // TODO: 사설인증서일 경우 처리
//                }
//            }
//            // 보통
//            else {
//                result = this.checkConnectionWithHarbor(externalRegistry);
//            }

//            result = this.checkConnectionWithHarbor(externalRegistry);

            if (externalRegistry.getProvider() == ImageRegistryType.HARBOR) {
                AccountRegistryVO accountRegistry = new AccountRegistryVO();
                accountRegistry.setProvider(externalRegistry.getProvider());
                accountRegistry.setRegistryUrl(externalRegistry.getEndpointUrl());
                accountRegistry.setAccessId(externalRegistry.getAccessId());
                accountRegistry.setAccessSecret(externalRegistry.getAccessSecret());
                accountRegistry.setPrivateCertificateUseYn(externalRegistry.getPrivateCertificateUseYn());
                accountRegistry.setPrivateCertificate(externalRegistry.getPrivateCertificate());
                accountRegistry.setInsecureYn(externalRegistry.getInsecureYn());
                result = accountRegistryService.checkConnection(accountRegistry);
            } else {
                throw new CocktailException("Provider is not supported.", ExceptionType.CommonNotSupported, externalRegistry.getProvider());
            }
        }

        return result;
    }


    public ResultVO checkConnectionWithHarbor(ExternalRegistryVO externalRegistry) throws Exception {

        ResultVO result;

        /**
         * 칵테일 설치시 제공하는 harbor의 check api를 사용하여 인증정보 체크
         * harbor ~1.7 과 1.8~ 의 api가 상이하고 harbor 버전을 알 수가 없어 아래와 같이 처리
         */
        // harbor 1.8이상
        IHarborRegistryService harborRegistryService = harborRegistryFactory.getService();
        HarborRegistryPingVO pingRegistry = new HarborRegistryPingVO();
        pingRegistry.setAccessKey(externalRegistry.getAccessId());
        pingRegistry.setAccessSecret(externalRegistry.getAccessSecret());

        pingRegistry.setType(externalRegistry.getProvider().getValue());
        pingRegistry.setUrl(externalRegistry.getEndpointUrl());
        pingRegistry.setInsecure(BooleanUtils.toBoolean(externalRegistry.getInsecureYn()));

        result = this.registriesPing(harborRegistryService, pingRegistry);

        if (result.getHttpStatusCode() != HttpStatus.SC_OK) {

            // http status는 200 정상이나 api가 존재하지 않아 오류가 발생할 시 0 으로 리턴
            // 이 경우에 다시 1.7 버전의 api를 호출하여 줌 (harbor 만 체크 가능)
            if (externalRegistry.getProvider() == ImageRegistryType.HARBOR && result.getHttpStatusCode() == 0) {
                // harbor 1.7
                result = this.targetPing(externalRegistry.getEndpointUrl(), externalRegistry.getAccessId(), externalRegistry.getAccessSecret(), pingRegistry.isInsecure().booleanValue());
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

    public ExternalRegistryDetailVO getExternalRegistry(Integer externalRegistrySeq, String serviceType) {
        IExternalRegistryMapper erDao = sqlSession.getMapper(IExternalRegistryMapper.class);
        return erDao.getExternalRegistry(externalRegistrySeq, serviceType);
    }

    public ExternalRegistryDetailVO getExternalRegistry(Integer externalRegistrySeq, String serviceType, boolean useStatus) throws Exception {
        ExternalRegistryDetailVO externalRegistry = this.getExternalRegistry(externalRegistrySeq, serviceType);

        if (useStatus && externalRegistry != null) {
            this.getConnectionStatus(externalRegistry, true);
        }

        return externalRegistry;
    }

    public List<ExternalRegistryVO> getExternalRegistries(Integer accountSeq, Integer serviceSeq, Integer servicemapSeq, String name, String endpointUrl, String registryName) throws Exception {
        IExternalRegistryMapper erDao = sqlSession.getMapper(IExternalRegistryMapper.class);
        return erDao.getExternalRegistries(accountSeq, serviceSeq, servicemapSeq, name, endpointUrl, registryName);
    }

    public List<ExternalRegistryVO> getExternalRegistriesWithStatus(Integer accountSeq, Integer serviceSeq, Integer servicemapSeq, String name, String endpointUrl, String registryName, boolean useStatus) throws Exception {
        List<ExternalRegistryVO> externalRegistries = this.getExternalRegistries(accountSeq, serviceSeq, servicemapSeq, name, endpointUrl, registryName);

        // 접속 상태 체크
        if (useStatus && CollectionUtils.isNotEmpty(externalRegistries)) {
            for (ExternalRegistryVO er : externalRegistries) {
                this.getConnectionStatus(er, true);
            }
        }

        return externalRegistries;
    }

    @Transactional(transactionManager = "transactionManager")
    public ExternalRegistryDetailVO deleteExternalRegistry(Integer externalRegistrySeq, boolean cascade) throws Exception {
        IExternalRegistryMapper erDao = sqlSession.getMapper(IExternalRegistryMapper.class);

        ExternalRegistryDetailVO currExternalRegistry = erDao.getExternalRegistry(externalRegistrySeq, null);
        if (currExternalRegistry == null) {
            throw new CocktailException("It is not exists the external registry.", ExceptionType.CommonUpdateFail, ExceptionBiz.EXTERNAL_REGISTRY, "the external registry not found.");
        } else {
            List<ServiceVO> currServicesWithoutPlatform = Optional.ofNullable(currExternalRegistry.getServices()).orElseGet(() ->Lists.newArrayList()).stream().filter(s -> (s.getServiceType() != ServiceType.PLATFORM)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(currServicesWithoutPlatform) && !cascade) {
                throw new CocktailException("It cannot be deleted because it is in use in the workspace."
                        , ExceptionType.CannotDeleteExternalRegistryUsingWorkspace
                        , String.format("Using workspace : %s", currServicesWithoutPlatform.stream().map(ServiceVO::getServiceName).collect(Collectors.joining(","))));
            } else {
                IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);

                List<BuildVO> builds = buildDao.getBuildList(currExternalRegistry.getAccountSeq(), null, null, Collections.singletonList(currExternalRegistry.getExternalRegistrySeq()), registryProperties.getUrl());

                if (CollectionUtils.isNotEmpty(builds)) {
                    throw new CocktailException("레지스트리가 빌드를 포함하고 있어 삭제될 수 없습니다. 빌드를 먼저 삭제해 주세요.", ExceptionType.RegistryContainsBuild, currExternalRegistry.getRegistryName());
                } else {
                    // delete service mapping
                    erDao.deleteExternalRegistryServiceMapping(externalRegistrySeq, currExternalRegistry.getServices().stream().map(ServiceVO::getServiceSeq).collect(Collectors.toList()));

                    // delete account mapping
//                erDao.deleteExternalRegistryAccountMapping(currExternalRegistry.getAccountSeq(), Arrays.asList(externalRegistrySeq));

                    // delete external registry (use_yn = 'N')
                    erDao.removeExternalRegistry(externalRegistrySeq, ContextHolder.exeContext().getUserSeq());
                }
            }
        }

        return currExternalRegistry;
    }

    @Transactional(transactionManager = "transactionManager")
    public int deleteExternalRegistryServiceMappingOfService(Integer serviceSeq) throws Exception {
        IExternalRegistryMapper erDao = sqlSession.getMapper(IExternalRegistryMapper.class);
        return erDao.deleteExternalRegistryServiceMappingOfService(serviceSeq);
    }

    @Transactional(transactionManager = "transactionManager")
    public void updateExternalRegistriesOfService(Integer serviceSeq, List<ExternalRegistryVO> reqExternalRegistries, Integer updater) throws Exception {
        if (CollectionUtils.isEmpty(reqExternalRegistries)) {
            reqExternalRegistries = new ArrayList<>();
        }

        IExternalRegistryMapper erDao = sqlSession.getMapper(IExternalRegistryMapper.class);

        List<ExternalRegistryVO> currExternalRegistries = erDao.getExternalRegistries(null, serviceSeq, null, null, null, null);

        List<Integer> currErSeqs = Optional.ofNullable(currExternalRegistries).orElseGet(() ->Lists.newArrayList()).stream().map(ExternalRegistryVO::getExternalRegistrySeq).collect(Collectors.toList());
        List<Integer> reqErSeqs = reqExternalRegistries.stream().map(ExternalRegistryVO::getExternalRegistrySeq).collect(Collectors.toList());

        List<Integer> deleteErSeqs = ListUtils.subtract(currErSeqs, reqErSeqs);
        List<Integer> addErSeqs = ListUtils.subtract(reqErSeqs, currErSeqs);

        if (CollectionUtils.isNotEmpty(deleteErSeqs)) {
            erDao.deleteExternalRegistryServiceMappingByService(serviceSeq, deleteErSeqs);
        }

        if (CollectionUtils.isNotEmpty(addErSeqs)) {
            erDao.insertExternalRegistryServiceMappings(serviceSeq, addErSeqs, updater);
        }
    }

    // 외부 레지스트리 정보의 PrivateCertificate 값 체크 및 base64 encoding 체크
    private void convertToBase64(ExternalRegistryVO externalRegistry, boolean isAdd, boolean isThrow) throws Exception {
        String errMsg = "";
        if (BooleanUtils.toBoolean(externalRegistry.getPrivateCertificateUseYn())) {
            if (StringUtils.isNotBlank(externalRegistry.getPrivateCertificate())) {
                // convert to base64
                if (StringUtils.contains(externalRegistry.getPrivateCertificate(), "-----BEGIN ")) {
                    externalRegistry.setPrivateCertificate(Base64Utils.encodeToString(externalRegistry.getPrivateCertificate().trim().getBytes()));
                } else {
                    try {
                        String decodedStr = new String(Base64Utils.decodeFromString(externalRegistry.getPrivateCertificate()), StandardCharsets.UTF_8);
                        if (!StringUtils.contains(decodedStr, "-----BEGIN ")) {
                            errMsg = "Invalid Private Certificate Data!!";
                            if (isThrow) {
                                throw new CocktailException(errMsg, ExceptionType.InvalidExternalRegistryCertification, errMsg);
                            }
                        }
                    } catch (CocktailException e) {
                        errMsg = "Invalid Private Certificate Data - bad base64 encoding!!";
                        if (isThrow) {
                            throw new CocktailException(errMsg, ExceptionType.InvalidExternalRegistryCertification, errMsg);
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

    public void checkParameterValidation(ExternalRegistryVO externalRegistry, boolean isAdd) throws Exception {
        if (externalRegistry != null) {
            if (isAdd) {
                externalRegistry.setExternalRegistrySeq(null);
            } else {
                ExceptionMessageUtils.checkParameterRequired("externalRegistrySeq", externalRegistry.getExternalRegistrySeq());
            }
            ExceptionMessageUtils.checkParameterRequired("accountSeq", externalRegistry.getAccountSeq());
            ExceptionMessageUtils.checkParameterRequired("provider", externalRegistry.getProvider());
            ExceptionMessageUtils.checkParameter("name", externalRegistry.getName(), 50, true);
            ExceptionMessageUtils.checkParameter("description", externalRegistry.getDescription(), 300, false);
            this.checkParamterUrlValidation(externalRegistry.getEndpointUrl());
            ExceptionMessageUtils.checkParameter("registryName", externalRegistry.getRegistryName(), 255, true);
        } else {
            throw new CocktailException("ExternalRegistry is null!!", ExceptionType.InvalidParameter, ExceptionBiz.EXTERNAL_REGISTRY);
        }
    }

    public void checkParamterUrlValidation(String endpointUrl) throws Exception {
        ExceptionMessageUtils.checkParameter("endpointUrl", endpointUrl, 255, true);
        if (!Utils.isValidUrlHttp(endpointUrl)) {
            throw new CocktailException("Invalid endpoint url!!", ExceptionType.InvalidParameter, ExceptionBiz.EXTERNAL_REGISTRY);
        }
    }

    public void setParameter(ExternalRegistryVO externalRegistry) throws Exception {
        externalRegistry.setEndpointUrl(StringUtils.removeEnd(StringUtils.trim(externalRegistry.getEndpointUrl()), "/"));
        externalRegistry.setRegistryName(StringUtils.trim(externalRegistry.getRegistryName()));
        externalRegistry.setAccessId(StringUtils.trim(externalRegistry.getAccessId()));
        externalRegistry.setAccessSecret(StringUtils.trim(externalRegistry.getAccessSecret()));
        externalRegistry.setPrivateCertificateUseYn(StringUtils.defaultString(externalRegistry.getPrivateCertificateUseYn(), "N"));
        if (!BooleanUtils.toBoolean(externalRegistry.getPrivateCertificateUseYn())) {
            externalRegistry.setPrivateCertificate(null);
        } else {
            externalRegistry.setPrivateCertificate(StringUtils.trim(externalRegistry.getPrivateCertificate()));
        }
        externalRegistry.setInsecureYn(StringUtils.defaultString(externalRegistry.getInsecureYn(), "N"));
    }

    // 암호화
    private void encryptCertInfo(ExternalRegistryVO externalRegistry) throws Exception {
        externalRegistry.setAccessId(CryptoUtils.encryptAES(externalRegistry.getAccessId()));
        externalRegistry.setAccessSecret(CryptoUtils.encryptAES(externalRegistry.getAccessSecret()));
        if (StringUtils.isNotBlank(externalRegistry.getPrivateCertificate())) {
            externalRegistry.setPrivateCertificate(CryptoUtils.encryptAES(externalRegistry.getPrivateCertificate()));
        }
    }

    // 복호화
    private void decryptCertInfo(ExternalRegistryVO externalRegistry) throws Exception {
        externalRegistry.setAccessId(CryptoUtils.decryptAES(externalRegistry.getAccessId()));
        externalRegistry.setAccessSecret(CryptoUtils.decryptAES(externalRegistry.getAccessSecret()));
        if (StringUtils.isNotBlank(externalRegistry.getPrivateCertificate())) {
            externalRegistry.setPrivateCertificate(CryptoUtils.decryptAES(externalRegistry.getPrivateCertificate()));
        }
    }

    public void clearCertInfo(ExternalRegistryVO externalRegistry) {
        if (externalRegistry != null) {
            externalRegistry.setAccessId(Optional.ofNullable(CryptoUtils.decryptAES(externalRegistry.getAccessId())).orElseGet(() ->externalRegistry.getAccessId()));
            externalRegistry.setAccessSecret(null);
            externalRegistry.setPrivateCertificate(null);
        }
    }
}
