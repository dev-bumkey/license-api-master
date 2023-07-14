package run.acloud.api.openapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IPAddressStringParameters;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.ibatis.session.ResultHandler;
import org.joda.time.format.DateTimeFormat;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwx.HeaderParameterNames;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.code.service.CodeService;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.openapi.dao.IOpenapiMapper;
import run.acloud.api.openapi.enums.TokenState;
import run.acloud.api.openapi.handler.ApiTokenAuditLogCsvResultHandler;
import run.acloud.api.openapi.handler.ApiTokenAuditLogExcelResultHandler;
import run.acloud.api.openapi.handler.ApiTokenIssuesHistoryResultHandler;
import run.acloud.api.openapi.handler.ApiTokenIssuesResultHandler;
import run.acloud.api.openapi.util.OpenapiUtil;
import run.acloud.api.openapi.vo.*;
import run.acloud.commons.util.*;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.commons.vo.PagingVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailLicenseProperties;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class OpenapiService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private CodeService codeService;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    @Autowired
    private CocktailLicenseProperties licenseProperties;

    private static final String JWK_GROUP_ID = "JSON_WEB_KEY";
    private static final String JWK_CODE_ID = "JWK";

    private static final String DEFAULT_DATE_FORMAT = "yyyyMMdd";
    private static final String DEFAULT_DISPLAY_DATE_FORMAT = "yyyy-MM-dd";



    /**
     * jwk 생성
     *
     * @param checkReadiness readiness 체크 여부
     * @param isRegenerate 재생성 여부
     * @param isThrow exception throw 여부
     * @throws Exception
     */
    public void generateJwk(boolean checkReadiness, boolean isRegenerate, boolean isThrow) throws Exception {
        // readiness 체크 (database is ready?)
        int errorCnt = 0;
        if (checkReadiness) {
            try {
                CodeVO code = codeService.getCode("PROVIDER", "AWS");
                if (code == null) {
                    errorCnt += 1;
                }
            } catch (Exception e) {
                errorCnt += 1;
                CocktailException ce = new CocktailException("An error occurred while checking the license.(api-server is not ready))", e, ExceptionType.CommonCreateFail);
                if (isThrow) {
                    throw ce;
                } else {
                    log.error(ce.getMessage(), ce);
                }
            }
        }

        // readiness is valid
        if (errorCnt == 0) {
            // 발급된 jwks 존재여부 확인
            CodeVO codeKey = this.getCodeForJWK();
            if (codeKey == null) {
                try {
                    codeKey = new CodeVO();
                    codeKey.setGroupId(JWK_GROUP_ID);
                    codeKey.setCode(JWK_CODE_ID);
                    codeKey.setValue(DateTimeUtils.getUtcTime());
                    codeKey.setDescription(CryptoUtils.encryptAES(OpenapiUtil.generateJwk()));
                    int result = codeService.addCode(codeKey);
                    if (result > 0) {
                        log.info("JWK Created Success.");
                    } else {
                        log.warn("JWK Created Fail.");
                    }
                } catch (Exception e) {
                    CocktailException ce = new CocktailException("An error occurred while generating the jwk.", e, ExceptionType.CommonCreateFail);
                    if (isThrow) {
                        throw ce;
                    } else {
                        log.error(ce.getMessage(), ce);
                    }
                }
            } else {
                if (isRegenerate) {
                    try {
                        codeKey.setValue(DateTimeUtils.getUtcTime());
                        codeKey.setDescription(CryptoUtils.encryptAES(OpenapiUtil.generateJwk()));
                        int result = codeService.editCodeForTrialLicense(codeKey);
                        if (result > 0) {
                            log.info("JWK Update Success.");
                        } else {
                            log.warn("JWK Update Fail.");
                        }
                    } catch (Exception e) {
                        CocktailException ce = new CocktailException("An error occurred while generating the jwk.", e, ExceptionType.CommonCreateFail);
                        if (isThrow) {
                            throw ce;
                        } else {
                            log.error(ce.getMessage(), ce);
                        }
                    }
                }
            }
        }
    }


    /**
     * 저장된 jwk 조회
     *
     * @return
     */
    @Cacheable(value="jsonWebKey")
    public CodeVO getCodeForJWK() {
        return codeService.getCode(JWK_GROUP_ID, JWK_CODE_ID);
    }

    /**
     * 저장된 jwk를 조회하여 jwks로 변환하여 조회
     *
     * @return
     * @throws Exception
     */
    @Cacheable(value="jsonWebKeySet")
    public String getJwks() throws Exception {
        String jwksJson = null;
        CodeVO result = this.getCodeForJWK();
        if (result != null && StringUtils.isNotBlank(result.getDescription())) {
            // 저장된 jwk는 private key가 포홤되어 jwks 생성시 private key를 제거하고 생성함
            // 저장된 jwk json deserialize
            RsaJsonWebKey rsaJsonWebKeyIncludePrivate = new RsaJsonWebKey(ObjectMapperUtils.getMapper().readValue(CryptoUtils.decryptAES(result.getDescription()), new TypeReference<Map<String, Object>>(){}));
            // deserialize 된 jwk를 private key 제외하고 다시 deserialize
            RsaJsonWebKey rsaJsonWebKey = new RsaJsonWebKey(ObjectMapperUtils.getMapper().readValue(rsaJsonWebKeyIncludePrivate.toJson(), new TypeReference<Map<String, Object>>(){}));

            // jwks 생성
            JsonWebKeySet jwks = new JsonWebKeySet();
            jwks.addJsonWebKey(rsaJsonWebKey);
            jwksJson = jwks.toJson();
        }

        return jwksJson;
    }

    /**
     * api gateway group 조회
     *
     * @return
     */
    public List<ApiGatewayGroupsVO> getApiGatewayGroups(boolean withApi) {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        if (withApi) {
            return dao.getApiGatewayGroupsWithApi();
        } else {
            return dao.getApiGatewayGroups();
        }
    }

    /**
     * api gateway 조회
     *
     * @return
     */
    public List<ApiGatewaysVO> getApiGateways(List<Integer> apiSeqs) {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        return dao.getApiGateways(apiSeqs);
    }

    /**
     * api token 발급
     *
     * @param apiTokenIssueAdd
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void issueApiToken(ApiTokenIssueAddVO apiTokenIssueAdd) throws Exception {
        if (apiTokenIssueAdd != null) {
            IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
            IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);

            /** validate param **/
            ExceptionMessageUtils.checkParameterRequired("accountSeq", apiTokenIssueAdd.getAccountSeq());
            // check token name
            this.checkApiTokenName(apiTokenIssueAdd.getAccountSeq(), apiTokenIssueAdd.getApiTokenName(), null, dao);

            ExceptionMessageUtils.checkParameter("apiTokenDescription", apiTokenIssueAdd.getApiTokenDescription(), 200, false);

            // check api 권한 범위
            this.checkApiTokenPermissionsScopes(apiTokenIssueAdd.getPermissionsScopes(), dao);

            if (StringUtils.isNotBlank(apiTokenIssueAdd.getExpirationDatetime()) && !StringUtils.equals("0", apiTokenIssueAdd.getExpirationDatetime())) {
                // 만료일 체크
                this.checkExpirationDate(apiTokenIssueAdd.getExpirationDatetime());
            }

            // ip 체크
            InetAddressValidator ipValidator = InetAddressValidator.getInstance();
            // - WhiteIpList 중복제거
            if (CollectionUtils.isNotEmpty(apiTokenIssueAdd.getWhiteIpList())) {
                apiTokenIssueAdd.setWhiteIpList(this.makeDedupeIpList(apiTokenIssueAdd.getWhiteIpList()));
                this.checkIp(apiTokenIssueAdd.getWhiteIpList(), ipValidator);
            }
            // - BlackIpList 중복제거
            if (CollectionUtils.isNotEmpty(apiTokenIssueAdd.getBlackIpList())) {
                apiTokenIssueAdd.setBlackIpList(this.makeDedupeIpList(apiTokenIssueAdd.getBlackIpList()));
                this.checkIp(apiTokenIssueAdd.getBlackIpList(), ipValidator);
            }

            // jwt 생성
            String issuer = "Cocktail";
            if (ContextHolder.exeContext().getUserSeq() != null) {
                UserVO user = userDao.getUser(ContextHolder.exeContext().getUserSeq());
                if (user != null && StringUtils.isNotBlank(user.getUserId())) {
                    issuer = user.getUserId();
                }
            }
            String audience = "Gateway";
            if (ContextHolder.exeContext().getUserAccount() != null) {
                if (StringUtils.isNotBlank(ContextHolder.exeContext().getUserAccount().getAccountCode())) {
                    audience = ContextHolder.exeContext().getUserAccount().getAccountCode();
                }
            }
            this.generateJwt(apiTokenIssueAdd, issuer, audience);

            // 토큰 발급 등록
            int result = dao.addApiTokenIssue(apiTokenIssueAdd);
            if (result > 0) {
                // 토큰 권한 범위 등록
                result = dao.addApiTokenPermissionsScopes(apiTokenIssueAdd.getApiTokenIssueSeq(), apiTokenIssueAdd.getPermissionsScopes(), ContextHolder.exeContext().getUserSeq());

                if (result > 0) {
                    // 토큰 발급 이력 등록
                    this.addApiTokenIssueHistoryInfo(apiTokenIssueAdd.getApiTokenIssueSeq(), TokenState.GRANT.getCode(), null, dao);
                } else {
                    String errMsg = "Permission scope is invalid.";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }
        } else {
            throw new CocktailException("apiTokenIssueAdd is null.", ExceptionType.InvalidParameter_Empty);
        }
    }

    /**
     * api token 정보 수정
     *
     * @param apiTokenIssueEdit
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editApiTokenIssue(ApiTokenIssueEditVO apiTokenIssueEdit) throws Exception {
        if (apiTokenIssueEdit != null) {
            IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);

            /** validate param **/
            ExceptionMessageUtils.checkParameterRequired("apiTokenIssueSeq", apiTokenIssueEdit.getApiTokenIssueSeq());
            ExceptionMessageUtils.checkParameterRequired("accountSeq", apiTokenIssueEdit.getAccountSeq());
            // 존재하는 지 체크
            if (this.getApiTokenIssue(apiTokenIssueEdit.getAccountSeq(), apiTokenIssueEdit.getApiTokenIssueSeq(), false) == null) {
                throw new CocktailException("apiTokenIssue is null.", ExceptionType.InvalidParameter, "apiTokenIssue not found.");
            }
            // check token name
            this.checkApiTokenName(apiTokenIssueEdit.getAccountSeq(), apiTokenIssueEdit.getApiTokenName(), apiTokenIssueEdit.getApiTokenIssueSeq(), dao);

            ExceptionMessageUtils.checkParameter("apiTokenDescription", apiTokenIssueEdit.getApiTokenDescription(), 200, false);

            // check api 권한 범위
            this.checkApiTokenPermissionsScopes(apiTokenIssueEdit.getPermissionsScopes(), dao);

            // ip 체크
            InetAddressValidator ipValidator = InetAddressValidator.getInstance();
            // - WhiteIpList 중복제거
            if (CollectionUtils.isNotEmpty(apiTokenIssueEdit.getWhiteIpList())) {
                apiTokenIssueEdit.setWhiteIpList(this.makeDedupeIpList(apiTokenIssueEdit.getWhiteIpList()));
                this.checkIp(apiTokenIssueEdit.getWhiteIpList(), ipValidator);
            }
            // - BlackIpList 중복제거
            if (CollectionUtils.isNotEmpty(apiTokenIssueEdit.getBlackIpList())) {
                apiTokenIssueEdit.setBlackIpList(this.makeDedupeIpList(apiTokenIssueEdit.getBlackIpList()));
                this.checkIp(apiTokenIssueEdit.getBlackIpList(), ipValidator);
            }

            // 토큰 발급 수정
            int result = dao.editApiTokenIssue(apiTokenIssueEdit);
            if (result > 0) {
                // 토큰 권한 범위 삭제
                dao.deleteApiTokenPermissionsScopes(apiTokenIssueEdit.getApiTokenIssueSeq());
                // 토큰 권한 범위 등록
                result = dao.addApiTokenPermissionsScopes(apiTokenIssueEdit.getApiTokenIssueSeq(), apiTokenIssueEdit.getPermissionsScopes(), ContextHolder.exeContext().getUserSeq());

                if (result > 0) {
                    // 토큰 발급 이력 등록
                    this.addApiTokenIssueHistoryInfo(apiTokenIssueEdit.getApiTokenIssueSeq(), TokenState.CHANGE.getCode(), null, dao);
                } else {
                    String errMsg = "Permission scope is invalid.";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }

        } else {
            throw new CocktailException("apiTokenIssueEdit is null.", ExceptionType.InvalidParameter_Empty);
        }
    }

    /**
     * token 회수
     *
     * @param apiTokenIssueSeq
     * @param accountSeq
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public ApiTokenIssueDetailVO revokeApiTokenIssue(Integer apiTokenIssueSeq, Integer accountSeq) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);

        /** validate param **/
        ExceptionMessageUtils.checkParameterRequired("apiTokenIssueSeq", apiTokenIssueSeq);
        ExceptionMessageUtils.checkParameterRequired("accountSeq", accountSeq);

        ApiTokenIssueDetailVO detail = this.getApiTokenIssue(accountSeq, apiTokenIssueSeq, false);

        // 존재하는 지 체크
        if (detail == null) {
            throw new CocktailException("apiTokenIssue is null.", ExceptionType.InvalidParameter, "apiTokenIssue not found.");
        }

        // 1. 이력 먼저 저장
        // 토큰 발급 이력 등록
        this.addApiTokenIssueHistoryInfo(apiTokenIssueSeq, TokenState.REVOKE.getCode(), null, dao);

        // 2. 토큰 삭제(회수)
        dao.deleteApiTokenIssue(apiTokenIssueSeq, accountSeq);

        return detail;
    }

    /**
     * 토큰 발급 정보 이력 등록
     *
     * @param apiTokenIssueSeq
     * @param tokenState
     * @param historyMessage
     * @param dao
     * @throws Exception
     */
    public void addApiTokenIssueHistoryInfo(Integer apiTokenIssueSeq, String tokenState, String historyMessage, IOpenapiMapper dao) throws Exception {
        if (dao == null) {
            dao = sqlSession.getMapper(IOpenapiMapper.class);
        }

        // 토큰 발급 이력 등록
        ApiTokenIssueHistoryVO issueHis = new ApiTokenIssueHistoryVO();
        issueHis.setApiTokenIssueSeq(apiTokenIssueSeq);
        issueHis.setUpdateUserSeq(ContextHolder.exeContext().getUserSeq());
        issueHis.setHistoryState(tokenState);
        issueHis.setHistoryMessage(historyMessage);
        int result = dao.addApiTokenIssueHistory(issueHis);

        if (result > 0) {
            // 토큰 권한 범위 이력 등록
            ApiTokenPermissionsScopeHistoryVO scopeHis = new ApiTokenPermissionsScopeHistoryVO();
            scopeHis.setApiTokenIssueHistorySeq(issueHis.getApiTokenIssueHistorySeq());
            dao.addApiTokenPermissionsScopeHistory(scopeHis);
        }
    }

    /**
     * token 명 체크
     *
     * @param accountSeq
     * @param apiTokenName
     * @param excludeApiTokenIssueSeq
     * @param dao
     * @throws Exception
     */
    private void checkApiTokenName(Integer accountSeq, String apiTokenName, Integer excludeApiTokenIssueSeq, IOpenapiMapper dao) throws Exception {
        if (dao == null) {
            dao = sqlSession.getMapper(IOpenapiMapper.class);
        }
        ExceptionMessageUtils.checkParameter("apiTokenName", apiTokenName, 30, true);
        // token name 중복체크
        if (CollectionUtils.isNotEmpty(dao.getApiTokenNames(accountSeq, apiTokenName, excludeApiTokenIssueSeq))) {
            String errMsg = "Token name already exists.";
            throw new CocktailException(errMsg
                    , ExceptionType.ResourceAlreadyExists
                    , String.format("[%s] - %s", apiTokenName, errMsg));
        }
    }

    /**
     * token api 권한 범위 체크
     *
     * @param permissionsScopes
     * @param dao
     * @throws Exception
     */
    private void checkApiTokenPermissionsScopes(List<Integer> permissionsScopes, IOpenapiMapper dao) throws Exception {
        if (CollectionUtils.isEmpty(permissionsScopes)) {
            String errMsg = "Permission scope is required.";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter_Empty, errMsg);
        } else {
            if (dao == null) {
                dao = sqlSession.getMapper(IOpenapiMapper.class);
            }
            // 요청한 API 권한 범위로 API 조회
            List<ApiGatewaysVO> apis = dao.getApiGateways(permissionsScopes);
            // 요청한 API 권한 범위와 조회한 API의 갯수가 다르다면 validate 오류
            if (CollectionUtils.isEmpty(apis)
                    || (CollectionUtils.isNotEmpty(apis) && apis.size() != permissionsScopes.size())
            ) {
                String errMsg = "Permission scope is invalid.";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        }
    }

    /**
     * 만료일 체크
     *
     * @param chkExpDate
     * @throws Exception
     */
    private void checkExpirationDate(String chkExpDate) throws Exception {

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate toDate = LocalDate.now();
        LocalDate expirationDate = null; // 만료일
        String errMsg = "";

        // 날짜 포맷 체크 및 만료일 LocalDate로 변환
        try {
            expirationDate = LocalDate.parse(chkExpDate);
        } catch (Exception e) {
            throw new CocktailException("expirationDatetime format is invalid."
                    , ExceptionType.InvalidParameter_DateFormat
                    , String.format("The format of the expiration date[%s] is invalid.", chkExpDate));
        }

        // 만료 기간이 오늘 이전인지 체크
        if (expirationDate.isBefore(toDate)) {
            errMsg = "The expiration date cannot be set before today.";
            throw new CocktailException(errMsg
                    , ExceptionType.InvalidParameter
                    , String.format("%s Expiration date : %s", errMsg, expirationDate.format(dateFormat)));
        }
    }

    /**
     * ip list 공백(Trim)/중복제거
     *
     * @param ipList
     * @return
     * @throws Exception
     */
    private List<String> makeDedupeIpList(List<String> ipList) throws Exception {
        if (CollectionUtils.isNotEmpty(ipList)) {
            List<String> trimIpList = Lists.newArrayList();
            // trim
            for (String ip : ipList) {
                trimIpList.add(StringUtils.trim(ip));
            }
            // 중복제거
            Set<String> dedupeIpSet = new HashSet<>(trimIpList);
            return new ArrayList<>(dedupeIpSet);
        }

        return new ArrayList<>();
    }

    /**
     * IP 체크
     * - 1.2.0.0/16
     * - 1.2.0.0
     *
     * @param ipList
     * @param ipValidator
     * @throws Exception
     */
    private void checkIp(List<String> ipList, InetAddressValidator ipValidator) throws Exception {
        if (CollectionUtils.isNotEmpty(ipList)) {

            /**
             * [2 step] ip 체크
             * 아래와 같은 2가지 형태의 ip만 allow
             * - 1.2.0.0/16
             * - 1.2.0.0
             * 1. allow 범위가 넓은 validator로 먼저 수행
             * 2. 1번이 isValid == true이면, IP 만 따로 별도 체크 수행 (CIDR format은 prefix는 제외하고 ip만 체크)
             *
             */
            if (ipValidator == null) {
                ipValidator = InetAddressValidator.getInstance();
            }

            IPAddressStringParameters params = new IPAddressStringParameters.Builder().allowMask(false).allowWildcardedSeparator(false).allow_inet_aton(false).toParams();
            for (String ip : ipList) {

                /** [STEP 1] ipValidator2.isValid() **/
                // https://seancfoley.github.io/IPAddress/ipaddress.html
                IPAddressString ipValidator2 = new IPAddressString(ip, params);
                boolean isValid = ipValidator2.isValid();

                String ipAddr = ip;

                if (isValid) {
                    log.debug("[check ip] - [{}] is valid {}.", ip, isValid);

                    /** [STEP 2] ipValidator.isValid(ipAddr) **/
                    // CIDR format 여부
                    if (ipValidator2.isPrefixed()) {
                        // IP 정보만 추출
                        String cidrIP[] = ip.split("/");
                        ipAddr = cidrIP[0];
                    }
                    isValid = ipValidator.isValid(ipAddr);
                    if (isValid) {
                        log.debug("[check only ip] - [{}] is valid {}.", ipAddr, isValid);
                    } else {
                        log.warn("[check only ip] - [{}] is valid {}.", ipAddr, isValid);
                        throw new CocktailException("IP is invalid."
                                , ExceptionType.InvalidParameter_DateFormat
                                , String.format("The format of the IP[%s] is invalid.", ip));
                    }
                } else {
                    log.warn("[check ip] - [{}] is valid {}.", ip, isValid);
                    throw new CocktailException("IP is invalid."
                            , ExceptionType.InvalidParameter_DateFormat
                            , String.format("The format of the IP[%s] is invalid.", ip));
                }
            }
        }
    }

    /**
     * jwt 생성
     *
     * @param apiTokenIssueAdd
     * @param issuer
     * @param audience
     * @throws Exception
     */
    private void generateJwt(ApiTokenIssueAddVO apiTokenIssueAdd, String issuer, String audience) throws Exception {
        /**
         * Get jwk
         */
        CodeVO code = this.getCodeForJWK();
        if (code == null) {
            this.generateJwk(false, false, true);
            code = this.getCodeForJWK();
        }
        RsaJsonWebKey rsaJsonWebKey = new RsaJsonWebKey(ObjectMapperUtils.getMapper().readValue(CryptoUtils.decryptAES(code.getDescription()), new TypeReference<Map<String, Object>>(){}));

        /**
         * jwt 생성
         */
        org.joda.time.format.DateTimeFormatter baseDayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        String expDateStr = StringUtils.isNotBlank(apiTokenIssueAdd.getExpirationDatetime()) ? apiTokenIssueAdd.getExpirationDatetime() : "9999-12-31";
        Date expDate = DateTimeUtils.initMaxTime(baseDayFmt.parseDateTime(expDateStr).toDate());

        // Create the Claims, which will be the content of the JWT
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);  // who creates the token and signs it
        claims.setAudience(audience); // to whom the token is intended to be sent
        claims.setExpirationTime(NumericDate.fromMilliseconds(expDate.getTime()));
//            claims.setExpirationTimeMinutesInTheFuture(10); // time when the token will expire (10 minutes from now)
//            claims.setJwtId("jwt");
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
//            claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
//        claims.setSubject("subject"); // the subject/principal is whom the token is about
            claims.setClaim("accountSeq", apiTokenIssueAdd.getAccountSeq()); // additional claims/attributes about the subject can be added
//            List<String> groups = Arrays.asList("group-one", "other-group", "group-three");
//            claims.setStringListClaim("groups", groups); // multi-valued claims work too and will end up as a JSON array

        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS so we create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        jws.setHeader(HeaderParameterNames.TYPE, "jwt");

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        // The JWT is signed using the private key
        jws.setKey(rsaJsonWebKey.getPrivateKey());

        // Set the Key ID (kid) header because it's just the polite thing to do.
        // We only have one key in this example but a using a Key ID helps
        // facilitate a smooth key rollover process
        jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        // Sign the JWS and produce the compact serialization or the complete JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        // If you wanted to encrypt it, you can simply set this jwt as the payload
        // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
        String jwt = jws.getCompactSerialization();

//        log.debug("jwt : {}", jwt);

        /**
         * jwt 검증
         */
        // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
        // be used to validate and process the JWT.
        // The specific validation requirements for a JWT are context dependent, however,
        // it is typically advisable to require a (reasonable) expiration time, a trusted issuer, and
        // an audience that identifies your system as the intended recipient.
        // If the JWT is encrypted too, you need only provide a decryption key or
        // decryption key resolver to the builder.
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime() // the JWT must have an expiration time
//                    .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
//                    .setRequireSubject() // the JWT must have a subject claim
                .setExpectedIssuer(issuer) // whom the JWT needs to have been issued by
                .setExpectedAudience(audience) // to whom the JWT is intended for
                .setVerificationKey(rsaJsonWebKey.getKey()) // verify the signature with the public key
                .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
                        AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256) // which is only RS256 here
                .build(); // create the JwtConsumer instance

        try
        {
            //  Validate the JWT and process it to the Claims
            JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
            log.debug("JWT validation succeeded! {}", jwtClaims);
            log.debug("JWT validation succeeded! exp {}", jwtClaims.getExpirationTime().toString());
        }
        catch (InvalidJwtException e) {
            String errMsg = e.getMessage();
            String additionalMsg = "";
            // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
            // Hopefully with meaningful explanations(s) about what went wrong.
            log.debug("Invalid JWT", e);

            // Programmatic access to (some) specific reasons for JWT invalidity is also possible
            // should you want different error handling behavior for certain conditions.

            // Whether or not the JWT has expired being one common reason for invalidity
            if (e.hasExpired()) {
                additionalMsg = String.format("JWT expired at %s", e.getJwtContext().getJwtClaims().getExpirationTime());
                log.debug(additionalMsg);
            }

            // Or maybe the audience was invalid
            if (e.hasErrorCode(ErrorCodes.AUDIENCE_INVALID)) {
                additionalMsg = String.format("JWT had wrong audience: %s", e.getJwtContext().getJwtClaims().getAudience());
                log.debug(additionalMsg);
            }
            throw new CocktailException(errMsg, additionalMsg, e, ExceptionType.CommonCreateFail, ExceptionBiz.OPENAPI);
        }

        /**
         * Set jwt
         */
        apiTokenIssueAdd.setToken(CryptoUtils.encryptAES(jwt));
    }

    /**
     * 목록 조회 검색 조건
     *
     * @param accountSeq
     * @param withApi
     * @param historyState
     * @param searchColumn
     * @param searchKeyword
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public ApiTokenIssueSearchVO setApiTokenIssueCommonParams(Integer accountSeq, boolean withApi, String historyState, String resultCode, String searchColumn, String searchKeyword, String startDate, String endDate) throws Exception {
        ApiTokenIssueSearchVO search = new ApiTokenIssueSearchVO();

        search.setWithApi(withApi);

//        if(StringUtils.isBlank(startDate) || StringUtils.isBlank(endDate)) { // 날짜 없으면 Default로 현재부터 일주일 설정
//            Date dEndDate = new Date();
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(dEndDate);
//            calendar.add(Calendar.DATE, -7);
//
//            Date dStartDate = calendar.getTime();
//            startDate = DateTimeUtils.getUtcTimeString(dStartDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
//            endDate = DateTimeUtils.getUtcTimeString(dEndDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
//        }
        search.setStartDate(startDate);
        search.setEndDate(endDate);

        if (StringUtils.isNotBlank(resultCode)) {
            search.setResultCode(resultCode);
        }

        // 검색조건 있으면 추가
        if(StringUtils.isNotBlank(searchColumn) && StringUtils.isNotBlank(searchKeyword)) {
            search.setSearchColumn(searchColumn);
            search.setSearchKeyword(searchKeyword);
        }

        // 발급상태 입력이 있으면 설정..
        if(StringUtils.isNotBlank(historyState)) {
            search.setHistoryState(historyState);
        }

        if (accountSeq != null && accountSeq > 0) {
            search.setAccountSeq(accountSeq);
        }

        // 사용자 정보 timezone 셀정
        search.setUserTimezone(ContextHolder.exeContext().getUserTimezone());

        // System 사용자는 권한이 있는 System에 해당하는 이력만 조회 가능.
        if (UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isUserOfSystem()) {
            search.setSystemUserSeq(ContextHolder.exeContext().getUserSeq());
        }

        return search;
    }

    /**
     * api token issue 목록 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<ApiTokenIssueVO> getApiTokenIssueList(ApiTokenIssueSearchVO params) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        return dao.getApiTokenIssuesList(params);
    }

    /**
     * api token issue 갯수 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getApiTokenIssueCount(ApiTokenIssueSearchVO params) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        return dao.getApiTokenIssuesCount(params);
    }

    /**
     * api token issue 목록 조회 with paging
     *
     * @param accountSeq
     * @param withApi
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public ApiTokenIssueListVO getApiTokenIssueList(Integer accountSeq, boolean withApi, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String startDate, String endDate) throws Exception {
        ApiTokenIssueListVO list = new ApiTokenIssueListVO();
        try {
            ApiTokenIssueSearchVO params = this.setApiTokenIssueCommonParams(accountSeq, withApi, null, null, searchColumn, searchKeyword, startDate, endDate);
            ListCountVO listCount = this.getApiTokenIssueCount(params);
            PagingVO paging = PagingUtils.setPagingParams(null, null, nextPage, itemPerPage, null, listCount);
            params.setPaging(paging);
            /** Params Setting Completed **/

            List<ApiTokenIssueVO> items = this.getApiTokenIssueList(params);

            list.setItems(items);
            list.setTotalCount(params.getPaging().getListCount().getCnt());
            list.setCurrentPage(nextPage);
        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("Api Token Issue List Inquire Failure", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }

        return list;
    }

    /**
     * 내부조회용
     *
     * @param accountSeq
     * @return
     */
    public List<ApiTokenIssueRelationVO> getApiTokenIssuesRelation(Integer accountSeq) {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        return dao.getApiTokenIssuesRelation(accountSeq);
    }

    /**
     * Excel 다운로드를 위해 resultHandler 를 이용한 ApiTokenIssues 조회
     *
     * @param params
     * @param resultHandler
     * @throws Exception
     */
    public void getApiTokenIssuesForExcel(ApiTokenIssueSearchVO params, ResultHandler<ApiTokenIssueExcelVO> resultHandler) throws Exception {
        sqlSession.select(String.format("%s.%s", IOpenapiMapper.class.getName(), "getApiTokenIssuesForExcel"), params, resultHandler);
    }


    /**
     * api token issue excel download
     *
     * @param response
     * @param params
     * @throws Exception
     */
    public void downloadExcelApiTokenIssues(HttpServletResponse response, ApiTokenIssueSearchVO params) throws Exception {
        try{
            // excel header 컬럼 순서대로 셋팅
            List<Pair<String, Integer>> headers = Lists.newArrayList();
            headers.add(Pair.of("Token Name", 30));
            headers.add(Pair.of("Expiration Date", 21));
            headers.add(Pair.of("Issuer", 40));
            headers.add(Pair.of("Issue Date", 25));
            headers.add(Pair.of("Last Request Date", 25));
            headers.add(Pair.of("White Ip", 40));
            headers.add(Pair.of("Black Ip", 40));
            if (params != null && params.isWithApi()) {
                headers.add(Pair.of("Api Permissions Scope", 100));
            }
            // resultHandler
            ApiTokenIssuesResultHandler<ApiTokenIssueExcelVO> resultHandler = new ApiTokenIssuesResultHandler(response, "cocktail-api-token-issue.xlsx", "list", headers);
            // 조회
            this.getApiTokenIssuesForExcel(params, resultHandler);
            // excel 생성 및 종료
            ExcelUtils.closeAfterWrite(resultHandler.getResponse(), resultHandler.getWorkbook());
        } catch (Exception e) {
            throw new CocktailException("An error occurred while downloading Excel.[ApiTokenIssues]", e, ExceptionType.ExcelDownloadFail);
        }
    }

    /**
     * api token issue 상세 조회
     *
     * @param accountSeq
     * @param apiTokenIssueSeq
     * @param withToken
     * @return
     * @throws Exception
     */
    public ApiTokenIssueDetailVO getApiTokenIssue(Integer accountSeq, Integer apiTokenIssueSeq, boolean withToken) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);

        // System 사용자는 권한이 있는 System에 해당하는 이력만 조회 가능.
        Integer systemUserSeq = UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isUserOfSystem() ? ContextHolder.exeContext().getUserSeq() : null;

        return dao.getApiTokenIssue(accountSeq, apiTokenIssueSeq, withToken, systemUserSeq, ContextHolder.exeContext().getUserTimezone());
    }


    /**
     * api token issue 상세 조회 by token
     *
     * @param accountSeq
     * @param token
     * @return
     * @throws Exception
     */
    @Cacheable(value="openapiTokenInfo")
    public ApiTokenIssueDetailVO getApiTokenIssueByToken(Integer accountSeq, String token) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);

        // 플랫폼에 속한 토큰 목록 조회
        List<ApiTokenIssueDetailVO> tokens = dao.getApiTokens(accountSeq);

        // 요청한 토큰 검색
        Optional<ApiTokenIssueDetailVO> matchTokenOptional = Optional.ofNullable(tokens).orElseGet(() -> Lists.newArrayList())
                .stream()
                .filter(t -> (StringUtils.equals(t.getToken(), token))).findFirst();

        // 요청한 토큰에 맞는 토큰 상세 정보를 조회하여 리턴
        ApiTokenIssueDetailVO matchToken = null;
        if (matchTokenOptional.isPresent()) {
            matchToken = dao.getApiTokenIssue(accountSeq, matchTokenOptional.get().getApiTokenIssueSeq(), true, null, null);
        }

        return matchToken;
    }


    /**
     * api token issue 이력 목록 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<ApiTokenIssueHistoryVO> getApiTokenIssueHistoryList(ApiTokenIssueSearchVO params) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        return dao.getApiTokenIssuesHistoryList(params);
    }

    /**
     * api token issue 이력 갯수 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getApiTokenIssueHistoryCount(ApiTokenIssueSearchVO params) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        return dao.getApiTokenIssuesHistoryCount(params);
    }

    /**
     * api token issue 이력 목록 조회 with paging
     *
     * @param accountSeq
     * @param withApi
     * @param historyState
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public ApiTokenIssueHistoryListVO getApiTokenIssueHistoryList(Integer accountSeq, boolean withApi, String historyState, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String startDate, String endDate) throws Exception {
        ApiTokenIssueHistoryListVO list = new ApiTokenIssueHistoryListVO();
        try {
            ApiTokenIssueSearchVO params = this.setApiTokenIssueCommonParams(accountSeq, withApi, historyState, null, searchColumn, searchKeyword, startDate, endDate);
            ListCountVO listCount = this.getApiTokenIssueHistoryCount(params);
            PagingVO paging = PagingUtils.setPagingParams(null, null, nextPage, itemPerPage, null, listCount);
            params.setPaging(paging);
            /** Params Setting Completed **/

            List<ApiTokenIssueHistoryVO> items = this.getApiTokenIssueHistoryList(params);

            list.setItems(items);
            list.setTotalCount(params.getPaging().getListCount().getCnt());
            list.setCurrentPage(nextPage);
        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("Api Token Issue History List Inquire Failure", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }

        return list;
    }

    /**
     * Excel 다운로드를 위해 resultHandler 를 이용한 ApiTokenIssuesHistory 조회
     *
     * @param params
     * @param resultHandler
     * @throws Exception
     */
    public void getApiTokenIssuesHistoryForExcel(ApiTokenIssueSearchVO params, ResultHandler<ApiTokenIssueHistoryExcelVO> resultHandler) throws Exception {
        sqlSession.select(String.format("%s.%s", IOpenapiMapper.class.getName(), "getApiTokenIssuesHistoryForExcel"), params, resultHandler);
    }


    /**
     * api token issue History excel download
     *
     * @param response
     * @param params
     * @throws Exception
     */
    public void downloadExcelApiTokenIssuesHistory(HttpServletResponse response, ApiTokenIssueSearchVO params) throws Exception {
        try{
            // excel header 컬럼 순서대로 셋팅
            List<Pair<String, Integer>> headers = Lists.newArrayList();
            headers.add(Pair.of("History Date", 25));
            headers.add(Pair.of("History State", 25));
            headers.add(Pair.of("Updater", 40));
            headers.add(Pair.of("Token Name", 30));
            headers.add(Pair.of("Expiration Date", 21));
            headers.add(Pair.of("White Ip", 40));
            headers.add(Pair.of("Black Ip", 40));
            if (params != null && params.isWithApi()) {
                headers.add(Pair.of("Api Permissions Scope", 100));
            }
            // resultHandler
            ApiTokenIssuesHistoryResultHandler<ApiTokenIssueHistoryExcelVO> resultHandler = new ApiTokenIssuesHistoryResultHandler(response, "cocktail-api-token-issue-history.xlsx", "list", headers);
            // 조회
            this.getApiTokenIssuesHistoryForExcel(params, resultHandler);
            // excel 생성 및 종료
            ExcelUtils.closeAfterWrite(resultHandler.getResponse(), resultHandler.getWorkbook());
        } catch (Exception e) {
            throw new CocktailException("An error occurred while downloading Excel.[ApiTokenIssuesHistory]", e, ExceptionType.ExcelDownloadFail);
        }
    }

    /**
     * api token audit log 등록 with 호출 건수
     *
     * @param apiTokenAuditLogAdd
     * @throws Exception
     */
    public void addApiTokenAuditLog(ApiTokenAuditLogAddVO apiTokenAuditLogAdd) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        // audit log 등록
        dao.addApiTokenAuditLog(apiTokenAuditLogAdd);
    }

    /**
     * token을 이용한 호출 건수 증가
     *
     * @param apiTokenIssueSeq
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public ApiTokenRequestCountVO editApiTokenRequestCount(Integer apiTokenIssueSeq, Integer accountSeq) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        // 호출 건수 증가
        dao.editRequestCountByToken(apiTokenIssueSeq, accountSeq);

        // 호출 건수 조회
        return dao.getApiTokenRequestCount(apiTokenIssueSeq, accountSeq);
    }


    /**
     * api token 감사 로그 목록 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<ApiTokenAuditLogVO> getApiTokenAuditLogList(ApiTokenIssueSearchVO params) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        return dao.getApiTokenAuditLogList(params);
    }

    /**
     * api token 감사 로그 갯수 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getApiTokenAuditLogCount(ApiTokenIssueSearchVO params) throws Exception {
        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);
        return dao.getApiTokenAuditLogCount(params);
    }

    /**
     * api token 감사 로그 목록 조회 with paging
     *
     * @param accountSeq
     * @param withTCnt
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public ApiTokenAuditLogListVO getApiTokenAuditLogList(Integer accountSeq, boolean withTCnt, Integer nextPage, Integer itemPerPage, String resultCode, String searchColumn, String searchKeyword, String startDate, String endDate) throws Exception {
        ApiTokenAuditLogListVO list = new ApiTokenAuditLogListVO();
        try {
            ApiTokenIssueSearchVO params = this.setApiTokenIssueCommonParams(accountSeq, false, null, resultCode, searchColumn, searchKeyword, startDate, endDate);
            ListCountVO listCount = null;
            if (withTCnt) {
                listCount = this.getApiTokenAuditLogCount(params);
            }
            PagingVO paging = PagingUtils.setPagingParams(null, null, nextPage, itemPerPage, null, listCount);
            params.setPaging(paging);
            /** Params Setting Completed **/

            List<ApiTokenAuditLogVO> items = this.getApiTokenAuditLogList(params);

            list.setItems(items);
            if (withTCnt) {
                list.setTotalCount(params.getPaging().getListCount().getCnt());
            }
            list.setCurrentPage(nextPage);
        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("Api Token Audit Log List Inquire Failure", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }

        return list;
    }

    /**
     * api token 감사 로그 목록 총 카운트
     *
     * @param accountSeq
     * @param searchColumn
     * @param searchKeyword
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public ListCountVO getApiTokenAuditLogListTotalCount(Integer accountSeq, String resultCode, String searchColumn, String searchKeyword, String startDate, String endDate) throws Exception {
        ListCountVO listCount;
        try {
            ApiTokenIssueSearchVO params = this.setApiTokenIssueCommonParams(accountSeq, false, null, resultCode, searchColumn, searchKeyword, startDate, endDate);
            listCount = this.getApiTokenAuditLogCount(params);
        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("Api Token Audit Log List Inquire Failure", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }

        return listCount;
    }


    /**
     * Excel 다운로드를 위해 resultHandler 를 이용한 ApiTokenAuditLog 조회
     *
     * @param params
     * @param resultHandler
     * @throws Exception
     */
    public void getApiTokenAuditLogForDownload(ApiTokenIssueSearchVO params, ResultHandler<ApiTokenAuditLogVO> resultHandler) throws Exception {
        sqlSession.select(String.format("%s.%s", IOpenapiMapper.class.getName(), "getApiTokenAuditLogForExcel"), params, resultHandler);
    }

    /**
     * api token audit log excel download
     *
     * @param response
     * @param params
     * @throws Exception
     */
    public void downloadExcelApiTokenAuditLog(HttpServletResponse response, ApiTokenIssueSearchVO params) throws Exception {
        try{
            // excel header 컬럼 순서대로 셋팅
            List<Pair<String, Integer>> headers = Lists.newArrayList();
            headers.add(Pair.of("Log Date", 25));
            headers.add(Pair.of("Api Name", 30));
            headers.add(Pair.of("Http Method", 15));
            headers.add(Pair.of("Url", 100));
            headers.add(Pair.of("Referer", 50));
            headers.add(Pair.of("User Agent", 30));
            headers.add(Pair.of("Token Name", 30));
            headers.add(Pair.of("Result", 25));
            headers.add(Pair.of("Client IP", 25));
            headers.add(Pair.of("Processing Time(sec)", 21));
            headers.add(Pair.of("Request", 100));
            headers.add(Pair.of("Response", 100));

            // resultHandler
            ApiTokenAuditLogExcelResultHandler<ApiTokenAuditLogVO> resultHandler = new ApiTokenAuditLogExcelResultHandler(response, "cocktail-api-token-audit-log.xlsx", "list", headers);
            // 조회
            this.getApiTokenAuditLogForDownload(params, resultHandler);
            // excel 생성 및 종료
            ExcelUtils.closeAfterWrite(resultHandler.getResponse(), resultHandler.getWorkbook());
        } catch (Exception e) {
            throw new CocktailException("An error occurred while downloading Excel.[ApiTokenAuditLog]", e, ExceptionType.ExcelDownloadFail);
        }
    }

    /**
     * api token audit log excel download
     *
     * @param response
     * @param params
     * @throws Exception
     */
    public void downloadCsvApiTokenAuditLog(HttpServletResponse response, ApiTokenIssueSearchVO params) throws Exception {
        try{

            // excel header 컬럼 순서대로 셋팅
            List<String> headers = Lists.newArrayList();
            headers.add("Log Date");
            headers.add("Api Name");
            headers.add("Http Method");
            headers.add("Url");
            headers.add("Referer");
            headers.add("User Agent");
            headers.add("Token Name");
            headers.add("Result");
            headers.add("Client IP");
            headers.add("Processing Time(sec)");
            headers.add("Request");
            headers.add("Response");

            // resultHandler
            ApiTokenAuditLogCsvResultHandler<ApiTokenAuditLogVO> resultHandler = new ApiTokenAuditLogCsvResultHandler(response, headers);
            // 조회
            this.getApiTokenAuditLogForDownload(params, resultHandler);
            // csv 생성 및 종료
            resultHandler.closeAfterWrite("cocktail-api-token-audit-log", true);
        } catch (Exception e) {
            throw new CocktailException("An error occurred while downloading Csv.[ApiTokenAuditLog]", e, ExceptionType.ExcelDownloadFail);
        }
    }
}
