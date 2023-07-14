package run.acloud.api.resource.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.googleapis.apache.GoogleApacheHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.JsonSyntaxException;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterAddVO;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.OAuthTokenVO;
import run.acloud.api.resource.enums.AuthType;
import run.acloud.api.resource.util.K8sMapperUtils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.ExecUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Component
public class K8sTokenGenerator {

    private final int diffSec = 60; //원래 만료일 보다 1분 차이나게 하기 위해 차감할 초

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    public void initClusterToken(ClusterAddVO clusterAdd){

        String token = "";
        String resultToken = null;
        OAuthTokenVO authToken = null;

        // TOKEN 방식일 때만 처리
        if(clusterAdd.getAuthType() == AuthType.TOKEN) {
            switch (clusterAdd.getCubeType()) {
                case EKS -> {
                    ClusterVO cluster = new ClusterVO();
                    BeanUtils.copyProperties(clusterAdd, cluster);
                    ResultVO result = this.executeAwsIamAuthenticator(cluster);
                    if(StringUtils.equals("200", result.getCode())){
                        token = CryptoUtils.encryptAES(result.getMessage());
                        clusterAdd.setApiSecret(token);
                    }
                }
                case NCPKS -> {
                    ClusterVO cluster = new ClusterVO();
                    BeanUtils.copyProperties(clusterAdd, cluster);
                    ResultVO result = this.executeNcpIamAuthenticator(cluster);
                    if(StringUtils.equals("200", result.getCode())){
                        token = CryptoUtils.encryptAES(result.getMessage());
                        clusterAdd.setApiSecret(token);
                    }
                }
                // refresh token을 이용해 token 정보 재생성한 뒤에 값 셋팅
                case GKE -> {
                    resultToken = CryptoUtils.decryptAES(clusterAdd.getApiSecret());
                    authToken = JsonUtils.fromGson(resultToken, OAuthTokenVO.class);
                    try {
                        authToken = getTokenForGKE(authToken);
                        token = CryptoUtils.encryptAES(JsonUtils.toPrettyString(authToken));
                        clusterAdd.setApiSecret(token);
                    }
                    catch (GeneralSecurityException gse) {
                        log.error("GKE refresh token failed.", gse);
                    }
                    catch (IOException ie) {
                        log.error("GKE refresh token failed.", ie);
                    }
                    catch (CocktailException ce) {
                        log.error("GKE refresh token failed.", ce);
                    }
                    catch (Exception e) {
                        log.error("GKE refresh token failed.", e);
                    }
                }
                // refresh token을 이용해 token 정보 재생성한 뒤에 값 셋팅
                case AKS -> {
                    resultToken = CryptoUtils.decryptAES(clusterAdd.getApiSecret());
                    // 2021-09-06, coolingi, access token 만 존재할 경우( admin credential ), OAuthTokenVO object 임의 생성해 리턴, AKS 는 이후 모두 admin credential 사용함.
                    authToken = getAuthTokenFromJsonTokenForAKS(resultToken);

                    try {
                        authToken = getTokenForAKS(authToken);
                        token = CryptoUtils.encryptAES(JsonUtils.toPrettyString(authToken));
                        clusterAdd.setApiSecret(token);
                    }
                    catch (InterruptedException | ExecutionException | MalformedURLException e) {
                        log.error("AKS refresh token failed.", e);
                    }
                    catch (CocktailException ce) {
                        log.error("AKS refresh token failed.", ce);
                    }
                    catch (Exception e) {
                        log.error("AKS refresh token failed.", e);
                    }
                }
            }
        }

    }


    /**
     * Token 방식 cluster일 경우 Token의 만료 체크 후 token update 처리
     *
     * @param cluster
     * @return
     */
    public boolean refreshClusterToken(ClusterVO cluster){

        boolean isUpdated = false;

        // TOKEN 방식일 때만 처리
        if(cluster.getAuthType() == AuthType.TOKEN) {
            switch (cluster.getCubeType()) {
                case EKS:
                    isUpdated = updateAWSToken(cluster);
                    break;
                case NCPKS:
                    isUpdated = updateNCPToken(cluster);
                    break;
                case GKE:
                    isUpdated = updateTokenForGKE(cluster);
                    break;
                case AKS:
                    isUpdated = updateTokenForAKS(cluster);
                    break;
            }
        }

        return isUpdated;
    }

///////////////////// For EKS /////////////////////
    boolean updateAWSToken(ClusterVO cluster){
        boolean isUpdated = false;
        try {
            // 현재 등록된 token 정보
            boolean needRenewToken = false;
            String resultToken = CryptoUtils.decryptAES(cluster.getApiSecret());
            if(StringUtils.isNotBlank(resultToken)){
                Map<String, Object> resultTokenMap = K8sMapperUtils.getMapper().readValue(resultToken, new TypeReference<Map<String, Object>>(){});
                // get ExpirationTime
                DateTime expirationTimestamp = DateTime.parse((String)((Map<String, Object>)resultTokenMap.get("status")).get("expirationTimestamp"));

                // ExpirationTime 에서 5분전 시간과 현재 시간을 비교하여 현재 보다 작으면 token을 재발행한다.
                DateTime expirationTimestampMinusMinutes = expirationTimestamp.minusMinutes(5);
                DateTime currentDateTime = DateTime.now(expirationTimestampMinusMinutes.getZone());
                log.debug("currentDateTime : {}, expirationTime : {}, expirationTimestamp : {}, {} > {} = {}", currentDateTime, expirationTimestampMinusMinutes, expirationTimestamp, currentDateTime.getMillis(), expirationTimestampMinusMinutes.getMillis(), currentDateTime.getMillis() > expirationTimestampMinusMinutes.getMillis());

                // 만료시간 체크
                if(currentDateTime.getMillis() > expirationTimestampMinusMinutes.getMillis()){
                    needRenewToken = true;
                }

                // 토큰 재발행이 필요하면 재발행 처리
                if(needRenewToken){
                    // token 재발행
                    ResultVO result = this.executeAwsIamAuthenticator(cluster);
                    if(StringUtils.equals("200", result.getCode())){
                        cluster.setApiSecret(CryptoUtils.encryptAES(result.getMessage()));
                        cluster.setUpdater(ContextHolder.exeContext().getUserSeq());
                        this.updateClusterToken(cluster);
                        isUpdated = true;
                    }
                }
            }
        }
        catch (CocktailException ce) {
            log.error("renew Aws EKS Token error.", ce);
        }
        catch (Exception e) {
            log.error("renew Aws EKS Token error.", e);
        }
        return isUpdated;
    }

    ResultVO executeAwsIamAuthenticator(ClusterVO cluster){
        String cmd = String.format("export AWS_ACCESS_KEY_ID=%s&& \\export AWS_SECRET_ACCESS_KEY=%s&& \\export AWS_DEFAULT_REGION=%s&& \\aws-iam-authenticator token -i %s"
                , CryptoUtils.decryptAES(cluster.getClientAuthData()), CryptoUtils.decryptAES(cluster.getClientKeyData()), cluster.getRegionCode(), cluster.getBillingGroupId());
        // get Token
        return ExecUtils.execute(cmd);
    }

///////////////////// For NCPKS /////////////////////
    boolean updateNCPToken(ClusterVO cluster){
        boolean isUpdated = false;
        try {
            // 현재 등록된 token 정보
            boolean needRenewToken = false;
            String resultToken = CryptoUtils.decryptAES(cluster.getApiSecret());
            if(StringUtils.isNotBlank(resultToken)){
                Map<String, Object> resultTokenMap = K8sMapperUtils.getMapper().readValue(resultToken, new TypeReference<Map<String, Object>>(){});

                Map<String, Object> status = (Map<String, Object>)resultTokenMap.get("status");
                if (MapUtils.isNotEmpty(status)) {
                    String expirationTimestampStr = MapUtils.getString(status, "expirationTimestamp", null);

                    if (StringUtils.isNotBlank(expirationTimestampStr)) {
                        // get ExpirationTime
                        DateTime expirationTimestamp = DateTime.parse(expirationTimestampStr);

                        // ExpirationTime 에서 5분전 시간과 현재 시간을 비교하여 현재 보다 작으면 token을 재발행한다.
                        DateTime expirationTimestampMinusMinutes = expirationTimestamp.minusMinutes(5);
                        DateTime currentDateTime = DateTime.now(expirationTimestampMinusMinutes.getZone());
                        log.debug("currentDateTime : {}, expirationTime : {}, expirationTimestamp : {}, {} > {} = {}", currentDateTime, expirationTimestampMinusMinutes, expirationTimestamp, currentDateTime.getMillis(), expirationTimestampMinusMinutes.getMillis(), currentDateTime.getMillis() > expirationTimestampMinusMinutes.getMillis());

                        // 만료시간 체크
                        if (currentDateTime.getMillis() > expirationTimestampMinusMinutes.getMillis()) {
                            needRenewToken = true;
                        }
                    } else {
                        needRenewToken = true;
                    }
                } else {
                    needRenewToken = true;
                }

                // 토큰 재발행이 필요하면 재발행 처리
                if (needRenewToken) {
                    // token 재발행
                    ResultVO result = this.executeNcpIamAuthenticator(cluster);
                    if (StringUtils.equals("200", result.getCode())) {
                        cluster.setApiSecret(CryptoUtils.encryptAES(result.getMessage()));
                        cluster.setUpdater(ContextHolder.exeContext().getUserSeq());
                        this.updateClusterToken(cluster);
                        isUpdated = true;
                    }
                }
            }
        }
        catch (CocktailException ce) {
            log.error("renew Ncp KS Token error.", ce);
        }
        catch (Exception e) {
            log.error("renew Ncp KS Token error.", e);
        }
        return isUpdated;
    }

    ResultVO executeNcpIamAuthenticator(ClusterVO cluster){
        String cmd = String.format("export NCLOUD_ACCESS_KEY=%s&& \\export NCLOUD_SECRET_KEY=%s&& \\export NCLOUD_API_GW=%s&& \\ncp-iam-authenticator token --clusterUuid %s --region %s"
                , CryptoUtils.decryptAES(cluster.getClientAuthData()), CryptoUtils.decryptAES(cluster.getClientKeyData()), cocktailServiceProperties.getNcloudApiGw(), cluster.getBillingGroupId(), cluster.getRegionCode());
        // get Token
        return ExecUtils.execute(cmd);
    }


///////////////////// For GKE /////////////////////
    boolean updateTokenForGKE(ClusterVO cluster){
        boolean isUpdated = false;
        String resultToken = CryptoUtils.decryptAES(cluster.getApiSecret());
        OAuthTokenVO authToken = JsonUtils.fromGson(resultToken, OAuthTokenVO.class);

        // token이 만료 되었을때만 재발급후 DB 저장
        if(authToken.isExpired()){
            try {
                // access token 재발급
                getTokenForGKE(authToken);

                // db update
                cluster.setApiSecret(CryptoUtils.encryptAES(JsonUtils.toGson(authToken)));
                cluster.setUpdater(ContextHolder.exeContext().getUserSeq());
                this.updateClusterToken(cluster);

                isUpdated = true;
            }
            catch (GeneralSecurityException gse) {
                log.error("Fail to get the accessToken.", gse);
            }
            catch (IOException ie) {
                log.error("Fail to get the accessToken.", ie);
            }
            catch (Exception e) {
                log.error("Fail to get the accessToken.", e);
            }
        }

        return isUpdated;
    }

    OAuthTokenVO getTokenForGKE(OAuthTokenVO authToken) throws GeneralSecurityException, IOException {
        String clientId = cocktailServiceProperties.getGkeClientId();
        String clientSecret = cocktailServiceProperties.getGkeClientSecret();
        HttpTransport httpTransport = GoogleApacheHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        GoogleRefreshTokenRequest tokenRequest = new GoogleRefreshTokenRequest(httpTransport, jsonFactory, authToken.getRefreshToken(), clientId, clientSecret);
        GoogleTokenResponse result = tokenRequest.execute();
        log.debug("========================================================");
        log.debug(result.toPrettyString());
        log.debug("========================================================");

        // 결과 셋팅
        if (result != null){
            authToken.setAccessToken(result.getAccessToken());

            // 만료시간 재계산, UTC time base
            int expiresIn = result.getExpiresInSeconds().intValue() - diffSec;
            DateTime tmpDateTime = new DateTime(DateTimeZone.UTC);
            tmpDateTime = tmpDateTime.plusSeconds(expiresIn);

            log.debug("The computed expire date : "+tmpDateTime.toString("yyyy-MM-dd HH:mm:ss.SSSZ") );

            authToken.setExpiresIn(expiresIn);
            authToken.setExpiresOnDate(tmpDateTime.getMillis()); //만료시간을 밀리세컨으로 저장
        }

        return authToken;
    }

///////////////////// For AKS /////////////////////
    boolean updateTokenForAKS(ClusterVO cluster){
        boolean isUpdated = false;
        String resultToken = CryptoUtils.decryptAES(cluster.getApiSecret());
        OAuthTokenVO authToken = getAuthTokenFromJsonTokenForAKS(resultToken);
        if(authToken.isExpired()){
            try {
                // access token 재발급
                getTokenForAKS(authToken);

                // db update
                cluster.setApiSecret(CryptoUtils.encryptAES(JsonUtils.toPrettyString(authToken)));
                cluster.setUpdater(ContextHolder.exeContext().getUserSeq());
                this.updateClusterToken(cluster);

                isUpdated = true;
            }
            catch (InterruptedException | ExecutionException | MalformedURLException e) {
                log.error("Fail to get the accessToken.", e);
            }
            catch (Exception e) {
                log.error("Fail to get the accessToken.", e);
            }
        }
        return isUpdated;
    }

    /**
     * <p>
     * token 문자열을 이용해 OAuthTokenVO Object 를 생성하는 메서드.</br>
     * admin credential 통해 얻은 정보는 access token 값 넘어오고, 기존 OAuth token json은 json 형태로 넘어온다.</br>
     * access token 값이 넘어왔을 경우는 json parsing 오류가 발생되고, 이때는 OAuthTokenVO Object 를 임의로 생성하고, access token 값만 셋팅하고, expire 관련값을 -1 로 셋팅한다.
     * </p>
     *
     * @since 2021-09-06
     * @param resultToken
     * @return
     */
    OAuthTokenVO getAuthTokenFromJsonTokenForAKS(String resultToken){
        OAuthTokenVO authToken;
        try {
            authToken = JsonUtils.fromGson(resultToken, OAuthTokenVO.class);
        } catch (JsonSyntaxException jse){
            log.debug("resultToken : {}", resultToken);
            log.debug("apiSecret json parsing exception : {}", jse.getMessage());

            // 특정 parsing exception 일때만 token Object를 만든다.
            if (jse.getMessage().indexOf("Expected BEGIN_OBJECT but was STRING at line 1 column 1") != -1) {
                authToken = new OAuthTokenVO();
                authToken.setTokenType("Bearer");
                authToken.setAccessToken(resultToken);
                authToken.setExpiresIn(-1);
                authToken.setExpiresOnDate(-1);
            } else {
                throw jse;
            }
        }
        return authToken;
    }

    /**
     *
     * refresh token을 이용해 OAuth token을 조회해 OAuthTokenVO object에 셋팅후 리턴하는 메서드.
     *
     *
     * @param authToken
     * @return
     * @update 2021-09-06
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MalformedURLException
     */
    OAuthTokenVO getTokenForAKS(OAuthTokenVO authToken) throws InterruptedException, ExecutionException, MalformedURLException {

        String authority = "https://login.microsoftonline.com/";

        String clientId = cocktailServiceProperties.getAksClientId();
        String clientSecret = cocktailServiceProperties.getAksClientSecret();

        AuthenticationContext context;
        AuthenticationResult result = null;
        ExecutorService service = null;

        //TODO 만료시간이나 refreshToken 이 없는 경우는 토큰 조회 처리를 하지 않는다.
        //TODO getAzureClusterAdminCredential 통해 값을 얻은 경우는 refreshToken과 만료일이 없음.
        if(authToken.getRefreshToken() == null || authToken.getExpiresOnDate() == -1){
            return authToken;
        }

        try {

            // 2021-09-03, coolingi, 아래 로직을 남겨놓는 이유는, 이전 OAuth을 이용해 등록된 AKS 가 존재할 경우 기존과 동일한 방식으로 처리 하기 위해 남겨놓은 것임.
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(authority + authToken.getTenantId() + "/", true, service);
            Future<AuthenticationResult> future = context
                    .acquireTokenByRefreshToken(authToken.getRefreshToken(), new ClientCredential(clientId, clientSecret), null, null);
            result = future.get();

            log.debug("========================================================");
            log.debug(JsonUtils.toPrettyString(result));
            log.debug("========================================================");

            // 결과 셋팅
            if (result != null){
                authToken.setTenantId(result.getUserInfo().getTenantId());
                authToken.setAccessToken(result.getAccessToken());
                authToken.setRefreshToken(result.getRefreshToken());

                // 만료시간 재계산, UTC time base
                int expiresIn = (int)result.getExpiresAfter() - diffSec;
                DateTime tmpDateTime = new DateTime(DateTimeZone.UTC);
                tmpDateTime = tmpDateTime.plusSeconds(expiresIn);
                log.debug("The computed expire date : "+tmpDateTime.toString("yyyy-MM-dd HH:mm:ss.SSSZ") );

                authToken.setExpiresIn(expiresIn);
                authToken.setExpiresOnDate(tmpDateTime.getMillis()); //만료시간을 밀리세컨으로 저장
            }
        }
        catch (InterruptedException | ExecutionException | MalformedURLException e) {
            throw e;
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (service != null) {
                service.shutdown();
            }
        }

        if (result == null) {
            throw new RuntimeException("authentication result was null");
        }

        return authToken;
    }

    /**
     * 순환 참조 문제로 ClusterService에 구현하지 않고 직접 구현...
     * TODO : Refactoring... 적당한 위치로 이동 시키자....
     * @param cluster
     * @throws Exception
     */
    public void updateClusterToken(ClusterVO cluster) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterAddVO clusterAdd = new ClusterAddVO();
        clusterAdd.setClusterSeq(cluster.getClusterSeq());
        clusterAdd.setApiSecret(cluster.getApiSecret());
        clusterDao.updateClusterForSecurity(clusterAdd);
    }
}
