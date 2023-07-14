package run.acloud.api.log.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.jose4j.jwa.AlgorithmConstraints;
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
import org.springframework.stereotype.Service;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.openapi.service.OpenapiService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.DateTimeUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class LogAgentTokenService {
    private final OpenapiService openapiService;

    public LogAgentTokenService(OpenapiService openapiService) {
        this.openapiService = openapiService;
    }

    // 로그 에이전트, 애드온 로그 에이전트 토큰 발행 로직
    public String issueToken() throws Exception {
        // jwt 생성
        String issuer = "Cocktail";
        String audience = "LogAgent";

        // jwk 조회
        CodeVO code = openapiService.getCodeForJWK();
        if (code == null) {
            throw new CocktailException("JWK load Failure", ExceptionType.CommonInquireFail, ExceptionBiz.LOG);
        }
        RsaJsonWebKey rsaJsonWebKey = new RsaJsonWebKey(ObjectMapperUtils.getMapper().readValue(CryptoUtils.decryptAES(code.getDescription()), new TypeReference<Map<String, Object>>(){}));

        // jwt 생성 로직 시작
        // 만료일 설정
        String expDateStr = "9999-12-31";
        Date expDate = getExpireDate(expDateStr);

        // jwt 안에 포함될 claims 생성 작업
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);  // who creates the token and signs it
        claims.setAudience(audience); // to whom the token is intended to be sent
        claims.setExpirationTime(NumericDate.fromMilliseconds(expDate.getTime()));
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setClaim("accountSeq", ContextHolder.exeContext().getUserAccountSeq()); // additional claims/attributes about the subject can be added

        // claims을 rsa 키로 암호화하여 jwt를 생성한다.
        String jwt = signingTokenByKey(claims, rsaJsonWebKey);
        log.debug("jwt : {}", jwt);

        // 발행된 토큰 검증
        verifySignedToken(jwt, issuer, audience, rsaJsonWebKey);

        return jwt;
    }

    private Date getExpireDate(String expDateStr) {
        org.joda.time.format.DateTimeFormatter baseDayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        return DateTimeUtils.initMaxTime(baseDayFmt.parseDateTime(expDateStr).toDate());
    }

    private String signingTokenByKey(JwtClaims claims, RsaJsonWebKey rsaJsonWebKey) throws Exception {
        JsonWebSignature jws = new JsonWebSignature();

        jws.setHeader(HeaderParameterNames.TYPE, "jwt");
        jws.setPayload(claims.toJson());
        jws.setKey(rsaJsonWebKey.getPrivateKey());
        jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        return jws.getCompactSerialization();
    }

    private void verifySignedToken(String token, String issuer, String audience, RsaJsonWebKey rsaJsonWebKey) throws Exception {
        // jwt 검증
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime() // the JWT must have an expiration time
                .setExpectedIssuer(issuer) // whom the JWT needs to have been issued by
                .setExpectedAudience(audience) // to whom the JWT is intended for
                .setVerificationKey(rsaJsonWebKey.getKey()) // verify the signature with the public key
                .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
                        AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256) // which is only RS256 here
                .build(); // create the JwtConsumer instance

        try {
            //  Validate the JWT and process it to the Claims
            JwtClaims jwtClaims = jwtConsumer.processToClaims(token);
            log.debug("JWT validation succeeded! {}", jwtClaims);
            log.debug("JWT validation succeeded! exp {}", jwtClaims.getExpirationTime().toString());
        } catch (InvalidJwtException e) {
            String errMsg = e.getMessage();
            String additionalMsg = "";

            log.debug("Invalid JWT", e);

            if (e.hasExpired()) {
                additionalMsg = String.format("JWT expired at %s", e.getJwtContext().getJwtClaims().getExpirationTime());
                log.debug(additionalMsg);
            }

            if (e.hasErrorCode(ErrorCodes.AUDIENCE_INVALID)) {
                additionalMsg = String.format("JWT had wrong audience: %s", e.getJwtContext().getJwtClaims().getAudience());
                log.debug(additionalMsg);
            }
            throw new CocktailException(errMsg, additionalMsg, e, ExceptionType.CommonCreateFail, ExceptionBiz.OPENAPI);
        }
    }
}
