package run.acloud.api.configuration.service;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.dao.IProviderAccountMapper;
import run.acloud.api.configuration.vo.ProviderAccountVO;
import run.acloud.api.resource.enums.ProviderAccountType;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.commons.client.ClusterApiClient;
import run.acloud.commons.service.AWSResourceService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProviderAccountService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private ClusterApiClient clusterApiClient;

	@Autowired
    private AWSResourceService awsResourceService;

//	public ProviderAccountVO getProviderAccountOfAppmap(Integer appmapSeq) {
//		IProviderAccountMapper dao = this.sqlSession.getMapper(IProviderAccountMapper.class);
//        return dao.getProviderAccountOfAppmap(appmapSeq);
//    }

//	public ProviderAccountVO getProviderAccountOfGroup(Integer groupSeq) {
//		IProviderAccountMapper dao = this.sqlSession.getMapper(IProviderAccountMapper.class);
//        return dao.getProviderAccountOfGroup(groupSeq);
//    }

//	public ProviderAccountVO getProviderAccountOfComponent(Integer componentSeq) {
//		IProviderAccountMapper dao = this.sqlSession.getMapper(IProviderAccountMapper.class);
//        return dao.getProviderAccountOfComponent(componentSeq);
//    }

    public List<CodeVO> getProviderAccountUseTypes() throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        return dao.getProviderAccountUseTypes();
    }

    public List<ProviderAccountVO> getProviderAccounts(Integer providerAccountSeq, String accountUseType, Integer accountSeq, Integer serviceSeq, String useYn) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        return dao.getProviderAccounts(providerAccountSeq, accountUseType, null, accountSeq, serviceSeq, useYn);
    }

    public List<ProviderAccountVO> getProviderAccounts(Integer providerAccountSeq, List<String> accountUseTypes, Integer accountSeq, Integer serviceSeq, String useYn) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        return dao.getProviderAccounts(providerAccountSeq, null, accountUseTypes, accountSeq, serviceSeq, useYn);
    }

    public ProviderAccountVO getProviderAccount(Integer providerAccountSeq) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        return dao.getProviderAccount(providerAccountSeq);
    }

    public ProviderAccountVO getProviderAccountByClusterSeq(Integer clusterSeq, ProviderCode providerCode, ProviderAccountType providerAccountType) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        return dao.getProviderAccountByClusterSeq(clusterSeq, providerCode.getCode(), providerAccountType.getCode());
    }

    @Transactional(transactionManager = "transactionManager")
    public void addProviderAccount(Map<String, Object> params) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        dao.addProviderAccount(params);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addProviderAccount(ProviderAccountVO providerAccount) throws Exception {
        this.addProviderAccountProcess(providerAccount);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addProviderAccountProcess(ProviderAccountVO providerAccount) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);

        ExceptionMessageUtils.checkParameter("providerName", providerAccount.getProviderName(), 50, true);

        this.setProviderAccount(providerAccount, false);

        dao.addProviderAccount2(providerAccount);
    }

    @Transactional(transactionManager = "transactionManager")
    public void editProviderAccount(Map<String, Object> params) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        dao.editProviderAccount(params);
    }

    @Transactional(transactionManager = "transactionManager")
    public void editProviderAccount(ProviderAccountVO providerAccount) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);

        this.setProviderAccount(providerAccount, true);

        dao.editProviderAccount2(providerAccount);
    }

    /**
     * Provider Account 수정 ( Account 수정에서 사용. Transaction은 Account에서 관리.)
     * @param providerAccount
     * @throws Exception
     */
    public void editProviderAccountFromAccount(ProviderAccountVO providerAccount) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        dao.editProviderAccount2(providerAccount);
    }

    /**
     * Provider Account 삭제 ( Account 수정에서 사용. Transaction은 Account에서 관리.)
     * @param providerAccount
     * @throws Exception
     */
    public void removeProviderAccountFromAccount(ProviderAccountVO providerAccount) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        // 삭제해도 되는지 Validation 체크.
        ProviderAccountVO pvCurr = this.getProviderAccount(providerAccount.getProviderAccountSeq());
        if (pvCurr == null) {
            throw new CocktailException("Provider not found", ExceptionType.ProviderNotFound);
        }
        List<String> clusters = this.getClustersUsingAccount(providerAccount.getProviderAccountSeq());
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(clusters)) {
            throw new CocktailException(String.format("Provider account used by cluster(s): %s", clusters),
                    ExceptionType.ProviderUsedByCluster);
        }

        // 삭제 처리 (useYn ="N")
        dao.removeProviderAccount(providerAccount);
    }

    @Transactional(transactionManager = "transactionManager")
    public void removeProviderAccount(ProviderAccountVO providerAccount) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        dao.removeProviderAccount(providerAccount);
    }

    @Transactional(transactionManager = "transactionManager")
    public void deleteProviderAccount(ProviderAccountVO providerAccount) throws Exception {

        // account_provider_account_mapping 삭제
        IAccountMapper aDao = sqlSession.getMapper(IAccountMapper.class);
        aDao.deleteProviderAccount(providerAccount.getProviderAccountSeq());

        // PROVIDER_ACCOUNT 삭제
        IProviderAccountMapper paDao = sqlSession.getMapper(IProviderAccountMapper.class);
        paDao.deleteProviderAccount(providerAccount);
    }

    public List<String> getClustersUsingAccount(Integer providerAccountSeq) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        return dao.selectClustersUsingAccount(providerAccountSeq);
    }

    public List<String> getClustersUsingMeteringAccount(Integer providerAccountSeq) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        return dao.selectClustersUsingMeteringAccount(providerAccountSeq);
    }

    public List<String> getClustersUsingAccessKeyAccount(Integer providerAccountSeq) throws Exception {
        IProviderAccountMapper dao = sqlSession.getMapper(IProviderAccountMapper.class);
        return dao.selectClustersUsingAccessKeyAccount(providerAccountSeq);
    }

    /**
     *
     *
     * @param providerAccount
     * @param json
     * @throws Exception
     */
    private void setCredentialParameters(ProviderAccountVO providerAccount, JSONObject json) throws Exception {
        boolean isValid = true;
        switch (providerAccount.getProviderCode()) {
            case AWS:
                /**
                 * access_key_id   : Amazon API Key Id
                 * secret_access_key   : Amazon API Key Secret
                 */
                if(!json.has("access_key_id") || !json.has("secret_access_key")){
                    isValid = false;
                }else{
                    if(StringUtils.isBlank(json.getString("access_key_id")) || StringUtils.isBlank(json.getString("secret_access_key"))){
                        isValid = false;
                    }
                }
                if(!isValid){
                    throw new CocktailException("CredentialFormat[AWS] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                }else{
                    providerAccount.setApiAccountPassword(CryptoUtils.encryptAES(providerAccount.getCredential()));
                }

                break;
            case GCP:
                /**
                 * credential_json : Google Storage의 접속 Credentail(json)
                 */
                if(!json.has("type") || !json.has("project_id")
                        || !json.has("private_key_id") || !json.has("private_key")
                        || !json.has("client_email") || !json.has("client_id")
                        || !json.has("auth_uri") || !json.has("token_uri")
                        || !json.has("auth_provider_x509_cert_url") || !json.has("client_x509_cert_url")){
                    isValid = false;
                }else{
                    if(StringUtils.isBlank(json.getString("type")) || StringUtils.isBlank(json.getString("project_id"))
                            || StringUtils.isBlank(json.getString("private_key_id")) || StringUtils.isBlank(json.getString("private_key"))
                            || StringUtils.isBlank(json.getString("client_email")) || StringUtils.isBlank(json.getString("client_id"))
                            || StringUtils.isBlank(json.getString("auth_uri")) || StringUtils.isBlank(json.getString("token_uri"))
                            || StringUtils.isBlank(json.getString("auth_provider_x509_cert_url")) || StringUtils.isBlank(json.getString("client_x509_cert_url"))){
                        isValid = false;
                    }
                }
                if(!isValid){
                    throw new CocktailException("CredentialFormat[GCP] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                }else{
                    providerAccount.setApiAccountPassword(CryptoUtils.encryptAES(providerAccount.getCredential()));
                }

                break;
            case AZR:
                /**
                 * client_id       : Azure API의 접속 계정
                 * client_secret   : Azure API의 접속 암호
                 */
                if(!json.has("client_id") || !json.has("client_secret")){
                    isValid = false;
                }else{
                    if(StringUtils.isBlank(json.getString("client_id")) || StringUtils.isBlank(json.getString("client_secret"))){
                        isValid = false;
                    }
                }

                /** AccessKey 유형일 경우 tenant_id와 workspace_id를 추가로 체크 함 **/
                if(providerAccount.getAccountUseType() == ProviderAccountType.ACCESS_KEY) {
                    if (!json.has("tenant_id") || !json.has("workspace_id")) {
                        isValid = false;
                    }
                    else {
                        if (StringUtils.isBlank(json.getString("tenant_id")) || StringUtils.isBlank(json.getString("workspace_id"))) {
                            isValid = false;
                        }
                    }
                }
                if(!isValid){
                    throw new CocktailException("CredentialFormat[AZR] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                }else{
                    providerAccount.setApiAccountPassword(CryptoUtils.encryptAES(providerAccount.getCredential()));
                }

                break;
            default:
                providerAccount.setAccountGroupId("");
                providerAccount.setApiAccountId("");
                providerAccount.setApiAccountPassword("");
                break;
        }
    }

    private void setConfigParameters(ProviderAccountVO providerAccount, JSONObject json) throws Exception {
        /** Metering Data 입력시 configJson 설정 **/
        if(providerAccount.getAccountUseType() == ProviderAccountType.METERING || providerAccount.getAccountUseType() == ProviderAccountType.USER) {
            boolean isValid = true;
            switch (providerAccount.getProviderCode()) {
                case AWS:
                    /**
                     * billing_account_id  : 결제 계정 ID
                     * account_id : 계정 ID
                     */
                    if (!json.has("billing_account_id") || !json.has("account_id")) {
                        isValid = false;
                    }
                    else {
                        if (StringUtils.isBlank(json.getString("billing_account_id")) || StringUtils.isBlank(json.getString("account_id"))) {
                            isValid = false;
                        }
                    }
                    if (providerAccount.getAccountUseType() == ProviderAccountType.METERING) {
                        /**
                         * region_name : Amazon S3의 리전 코드
                         * bucket_name : Amazon S3의 버킷 이름
                         */
                        if (!json.has("region_name") || !json.has("bucket_name")) {
                            isValid = false;
                        }
                        else {
                            if (StringUtils.isBlank(json.getString("region_name")) || StringUtils.isBlank(json.getString("bucket_name"))) {
                                isValid = false;
                            }
                        }
                    }

                    if (!isValid) {
                        throw new CocktailException("ConfigFormat[AWS] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                    }
                    else {
//                String accountGroupId = checkAwsCredential(accessKey, secretKey);
//                if (accountGroupId == null) {
//                    throw new CocktailException("AccessKey or SecretKey is invalid", ExceptionType.ProviderAccessKeyOrSecretKeyInvalid);
//                }
                        providerAccount.setAccountGroupId(json.getString("billing_account_id"));
                        providerAccount.setApiAccountId(json.getString("account_id"));
                        providerAccount.setProviderAccountConfig(providerAccount.getConfig());
                    }

                    break;
                case GCP:
                    /**
                     * billing_account_id  : 결제 계정 ID
                     * account_id : 계정 ID
                     */
                    if (!json.has("billing_account_id") || !json.has("account_id")) {
                        isValid = false;
                    }
                    else {
                        if (StringUtils.isBlank(json.getString("billing_account_id")) || StringUtils.isBlank(json.getString("account_id"))) {
                            isValid = false;
                        }
                    }
                    if (providerAccount.getAccountUseType() == ProviderAccountType.METERING) {
                        /**
                         * bucket_name : Google Storage의 버킷 이름
                         * blob_prefix : 버킷 내 Billing 파일의 접두어
                         */
                        if (!json.has("bucket_name") || !json.has("blob_prefix")) {
                            isValid = false;
                        }
                        else {
                            if (StringUtils.isBlank(json.getString("bucket_name")) || StringUtils.isBlank(json.getString("blob_prefix"))) {
                                isValid = false;
                            }
                        }
                    }

                    if (!isValid) {
                        throw new CocktailException("ConfigFormat[GCP] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                    }
                    else {
                        providerAccount.setAccountGroupId(json.getString("billing_account_id"));
                        providerAccount.setApiAccountId(json.getString("account_id"));
                        providerAccount.setProviderAccountConfig(providerAccount.getConfig());
                    }

                    break;
                case AZR:
//                /**
//                 * billing_account_id => subscription_id : Azure 구독 ID
//                 * account_id => tenant_id       : Azure Active Directory ID
//                 */
//                if(!json.has("billing_account_id") || !json.has("account_id")){
//                    isValid = false;
//                }else{
//                    if(StringUtils.isBlank(json.getString("billing_account_id")) || StringUtils.isBlank(json.getString("account_id"))){
//                        isValid = false;
//                    }
//                }
                    if (providerAccount.getAccountUseType() == ProviderAccountType.METERING) {
//                    /**
//                     * tenant_id       : Azure Active Directory ID
//                     */
//                    if(!json.has("tenant_id")){
//                        isValid = false;
//                    }else{
//                        if(StringUtils.isBlank(json.getString("tenant_id"))){
//                            isValid = false;
//                        }
//                    }
                        /**
                         * offer_type      : 구독 종류. 종량제(PayAsYouGo) // 미지원. CSP(클라우드 서비스 공급자), EA(기업계약 - Enterprose Agreement)
                         * offer_id        : Azure 구독 제안 번호. 종량제(MS-AZR-0003P, MS-AZR-0023P) // 미지원. CSP(MS-AZR-0145P), EA(MS-AZR-0017P, MS-AZR-0148P)
                         * currency        : 통화코드(ex: USD, KRW, JPN, ...)
                         * locale      : 로케일(ex: ko-KR, ...)
                         */
                        if (!json.has("offer_type") || !json.has("offer_id")
                            || !json.has("currency") || !json.has("locale")) {
                            isValid = false;
                        }
                        else {
                            if (StringUtils.isBlank(json.getString("offer_type")) || StringUtils.isBlank(json.getString("offer_id"))
                                || StringUtils.isBlank(json.getString("currency")) || StringUtils.isBlank(json.getString("locale"))) {
                                isValid = false;
                            }
                        }
                    }

                    if (!isValid) {
                        throw new CocktailException("ConfigFormat[AZR] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                    }
                    else {
                        providerAccount.setAccountGroupId(json.getString("billing_account_id"));
                        providerAccount.setApiAccountId(json.getString("account_id"));
                        providerAccount.setProviderAccountConfig(providerAccount.getConfig());
                    }

                    break;
                default:
                    providerAccount.setAccountGroupId("");
                    providerAccount.setApiAccountId("");
                    providerAccount.setApiAccountPassword("");
                    break;
            }
        }
        else if(providerAccount.getAccountUseType() == ProviderAccountType.ACCESS_KEY) {
            switch (providerAccount.getProviderCode()) {
                case AWS:
                    /**
                     * aws_iam_permission_check,
                     * aws_cloudwatch_permission_check
                     */
                    if (!json.has("aws_iam_permission_check") || !json.has("aws_cloudwatch_permission_check")) {
                        throw new CocktailException("ConfigFormat[AWS] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                    }
                    else {
                        providerAccount.setProviderAccountConfig(providerAccount.getConfig());
                    }

                    break;
                case GCP:
                    /**
                     * gcp_logs_viewer_permission_check
                     */
                    if (!json.has("gcp_logs_viewer_permission_check")) {
                        throw new CocktailException("ConfigFormat[GCP] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                    }
                    else {
                        providerAccount.setProviderAccountConfig(providerAccount.getConfig());
                    }

                    break;
                case AZR:
                    /**
                     * azure_log_analytics_permission_check
                     */
                    if (!json.has("azure_log_analytics_permission_check")) {
                        throw new CocktailException("ConfigFormat[Azure] is invalid.", ExceptionType.ProviderCredentialFormatInvalid);
                    }
                    else {
                        providerAccount.setProviderAccountConfig(providerAccount.getConfig());
                    }

                    break;
                default:
                    providerAccount.setAccountGroupId("");
                    providerAccount.setApiAccountId("");
                    providerAccount.setApiAccountPassword("");
                    break;
            }
        }
    }

    public void setProviderAccount(ProviderAccountVO providerAccount, boolean isEdit) throws Exception {
//        if(credentialJson != null
//                && providerAccount.getAccountUseType() == ProviderAccountType.METERING
//                && providerAccount.getProviderCode() != ProviderCode.OPM){
//            this.setCredentialParameters(providerAccount, credentialJson);
//        }else{
//            providerAccount.setAccountGroupId(providerAccount.getAccountGroupId() != null ? providerAccount.getAccountGroupId() : "");
//            providerAccount.setApiAccountId(providerAccount.getApiAccountId() != null ? providerAccount.getApiAccountId() : "");
//            providerAccount.setApiAccountPassword(providerAccount.getApiAccountPassword() != null ? providerAccount.getApiAccountPassword() : "");
//        }
        JSONObject credentialJson = this.convertProviderJsonStringToObject(providerAccount.getCredential(), "Credential format is invalid");

        JSONObject configJson = this.convertProviderJsonStringToObject(providerAccount.getConfig(), "config format is invalid");

        if ( providerAccount.getProviderCode() == ProviderCode.AWS
                || providerAccount.getProviderCode() == ProviderCode.GCP
                || providerAccount.getProviderCode() == ProviderCode.AZR) {
            // GCP // USER // ProjectId required
//            if (providerAccount.getProviderCode() == ProviderCode.GCP
//                    && providerAccount.getAccountUseType() == ProviderAccountType.USER
//                    && StringUtils.isBlank(providerAccount.getProjectId())){
//                throw new CocktailException("ProjectId is empty", ExceptionType.InvalidParameter);
//            }else{
//                providerAccount.setApiAccountId(providerAccount.getProjectId());
//            }

            // (!isEdit == CREATE) AWS/GCP/AZR // METERING // Credential/Config required
            if(providerAccount.getAccountUseType() == ProviderAccountType.METERING && (!isEdit && (credentialJson == null || configJson == null))){
                throw new CocktailException("Credential is empty", ExceptionType.ProviderCredentialEmpty);
            }

            // (!isEdit == CREATE) AWS/GCP/AZR // ACCESS_KEY // Credential required
            if(providerAccount.getAccountUseType() == ProviderAccountType.ACCESS_KEY && (!isEdit && credentialJson == null)){
                throw new CocktailException("Credential is empty", ExceptionType.ProviderCredentialEmpty);
            }

//            if(StringUtils.isBlank(providerAccount.getAccountGroupId())){
//                throw new CocktailException("GroupId is empty", ExceptionType.InvalidParameter);
//            }else{
//                if(providerAccount.getAccountGroupId().length() > 50){
//                    throw new CocktailException("GroupId is more than 50 characters",
//                            ExceptionType.InvalidParameter);
//                }else{
//                    providerAccount.setAccountGroupId(providerAccount.getAccountGroupId().trim());
//                }
//            }
        }

        if(!providerAccount.getProviderCode().isNotCloud()){
            // 접속 키 정보
            if(credentialJson != null){
                this.setCredentialParameters(providerAccount, credentialJson);
            }else{
                if(!isEdit){
                    providerAccount.setApiAccountPassword("");
                }
            }
            // 추가 항목
            if(configJson != null){
                if(providerAccount.getAccountUseType() == ProviderAccountType.METERING || providerAccount.getAccountUseType() == ProviderAccountType.USER) {
                    this.setConfigParameters(providerAccount, configJson);
                }
                else if(providerAccount.getAccountUseType() == ProviderAccountType.ACCESS_KEY) {
                    this.setConfigParameters(providerAccount, configJson);
                }
            }else{
                providerAccount.setProviderAccountConfig("");
            }

            // default 처리
            if(StringUtils.isBlank(providerAccount.getAccountGroupId())){
                providerAccount.setAccountGroupId("");
            }
            if(StringUtils.isBlank(providerAccount.getApiAccountId())){
                providerAccount.setApiAccountId("");
            }

            if(providerAccount.getAccountUseType() == ProviderAccountType.ACCESS_KEY) {
                /**
                 * AccessKey 계정의 인증 체크
                 */
                if(providerAccount.getProviderCode() == ProviderCode.AWS
                    || providerAccount.getProviderCode() == ProviderCode.GCP
                    || providerAccount.getProviderCode() == ProviderCode.AZR){
                    boolean needValidated = true;
                    if(isEdit) { // 수정일때만...
                        IProviderAccountMapper providerAccountDao = sqlSession.getMapper(IProviderAccountMapper.class);
                        ProviderAccountVO currProviderAccount = providerAccountDao.getProviderAccount(providerAccount.getProviderAccountSeq());
                        String credentialDecrypted = CryptoUtils.decryptAES(currProviderAccount.getApiAccountPassword());
                        if(credentialJson == null){
                            try {
                                credentialJson = new JSONObject(credentialDecrypted);
                                providerAccount.setCredential(credentialDecrypted);
                            }
                            catch (JSONException je) {
                                throw new CocktailException("Credential format is invalid by Authentication", je, ExceptionType.ProviderCredentialFormatInvalid);
                            }
                            catch (Exception e) {
                                throw new CocktailException("Credential format is invalid by Authentication", e, ExceptionType.ProviderCredentialFormatInvalid);
                            }
                        }

                        // credential / config 값이 모두 동일하면 needValidated = false
                        // ( config안에 체크 여부만 바뀌는 경우도 있으므로 config도 체크하며, needValidated가 true라도 config안의 체크 여부에 따라 체크할지는 추가 판단함...)
                        if(StringUtils.equals(credentialDecrypted, providerAccount.getCredential()) &&
                           StringUtils.equals(currProviderAccount.getConfig(), providerAccount.getConfig()))
                        {
                            needValidated = false;
                        }
                    }

                    /**
                     * Validation Check!!
                     */
                    if(credentialJson != null && needValidated){
                        switch (providerAccount.getProviderCode()) {
                            case AWS: {
                                boolean isCheckIam = false;
                                boolean isCheckCW = false;
                                if(configJson == null) {
                                    // 4.3.3 이전 버전에서는 AWS AccessKey 입력시 config 정보가 없었기에.. 4.3.3 이전 버전에서 Check하던 IAM Check만 Default로 넣어줌...
                                    isCheckIam = true;
                                    throw new CocktailException("ConfigFormat[AWS] is null.", ExceptionType.ProviderCredentialFormatInvalid);
                                }
                                else {
                                    isCheckIam = configJson.getBoolean("aws_iam_permission_check");
                                    isCheckCW = configJson.getBoolean("aws_cloudwatch_permission_check");
                                }

                                if(isCheckIam) {
                                    AmazonIdentityManagement iamClient = awsResourceService.getIAMClient(credentialJson.getString("access_key_id"), credentialJson.getString("secret_access_key"));
                                    if (iamClient == null) {
                                        throw new CocktailException("AWS IAM Authentication information is not valid!!", ExceptionType.ProviderAuthenticationIsInvalid);
                                    }
                                    try {
                                        awsResourceService.getUsersTest(iamClient);
                                    }
                                    catch (CocktailException ce) {
                                        throw new CocktailException("AWS IAM Authentication information is not valid!!", ce, ExceptionType.ProviderAuthenticationIsInvalid);
                                    }
                                    catch (Exception ex) {
                                        throw new CocktailException("AWS IAM Authentication information is not valid!!", ex, ExceptionType.ProviderAuthenticationIsInvalid);
                                    }
                                }
                                else {
                                    log.warn("Skip checking AWS IAM Access permissions.");
                                }

                                if(isCheckCW) {
//                                    //TODO : CloudWatch Access Permission Check!!
//                                    Map<String, Object> result = clusterApiClient.accessKeyValidator(providerAccount.getProviderCode(), providerAccount.getCredential());
//                                    if(MapUtils.isNotEmpty(result)) {
//                                        if(!StringUtils.equalsIgnoreCase("200", MapUtils.getString(result, "status", "failed"))) {
//                                            throw new CocktailException("AWS CloudWatch Authentication information is not valid!! : " + MapUtils.getString(result, "status", "failed"), ExceptionType.ProviderAuthenticationIsInvalid);
//                                        }
//                                    }
//                                    else {
//                                        throw new CocktailException("AWS CloudWatch Authentication information is not valid!!", ExceptionType.ProviderAuthenticationIsInvalid);
//                                    }
                                }
                                else {
                                    log.warn("Skip checking AWS CloudWatch Access permissions.");
                                }
                            }
                                break;
                            case GCP: {
                                if(configJson == null) {
                                    throw new CocktailException("ConfigFormat[GCP] is null.", ExceptionType.ProviderCredentialFormatInvalid);
                                }
                                else {
                                    if(configJson.getBoolean("gcp_logs_viewer_permission_check")) {
//                                        //TODO : Logs Viewer Permission Check!!
//                                        Map<String, Object> result = clusterApiClient.accessKeyValidator(providerAccount.getProviderCode(), providerAccount.getCredential());
//                                        if(MapUtils.isNotEmpty(result)) {
//                                            if(!StringUtils.equalsIgnoreCase("200", MapUtils.getString(result, "status", "failed"))) {
//                                                throw new CocktailException("GCP Logs Viewer Authentication information is not valid!! : " + MapUtils.getString(result, "status", "failed"), ExceptionType.ProviderAuthenticationIsInvalid);
//                                            }
//                                        }
//                                        else {
//                                            throw new CocktailException("GCP Logs Viewer Authentication information is not valid!!", ExceptionType.ProviderAuthenticationIsInvalid);
//                                        }
                                    }
                                    else {
                                        log.warn("Skip checking GCP Logs Viewer Access permissions.");
                                    }
                                }
                            }
                                break;
                            case AZR: {
                                if(configJson == null) {
                                    throw new CocktailException("ConfigFormat[Azure] is null.", ExceptionType.ProviderCredentialFormatInvalid);
                                }
                                else {
                                    if(configJson.getBoolean("azure_log_analytics_permission_check")) {
//                                        //TODO : Log Analytics Access Permission Check!!
//                                        Map<String, Object> result = clusterApiClient.accessKeyValidator(providerAccount.getProviderCode(), providerAccount.getCredential());
//                                        if(MapUtils.isNotEmpty(result)) {
//                                            if(!StringUtils.equalsIgnoreCase("200", MapUtils.getString(result, "status", "failed"))) {
//                                                throw new CocktailException("Azure Log Analytics Authentication information is not valid!! : " + MapUtils.getString(result, "status", "failed"), ExceptionType.ProviderAuthenticationIsInvalid);
//                                            }
//                                        }
//                                        else {
//                                            throw new CocktailException("Azure Log Analytics Authentication information is not valid!!", ExceptionType.ProviderAuthenticationIsInvalid);
//                                        }
                                    }
                                    else {
                                        log.warn("Skip checking Azure Log Analytics Access permissions.");
                                    }
                                }
                            }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        else {
            providerAccount.setAccountGroupId(providerAccount.getAccountGroupId() != null ? providerAccount.getAccountGroupId() : "");
            providerAccount.setApiAccountId(providerAccount.getApiAccountId() != null ? providerAccount.getApiAccountId() : "");
            providerAccount.setApiAccountPassword(providerAccount.getApiAccountPassword() != null ? providerAccount.getApiAccountPassword() : "");
        }
    }

    public JSONObject convertProviderJsonStringToObject(String strJson, String errMsg) throws Exception {
        JSONObject jsonObj = null;
        if (StringUtils.isNotBlank(strJson)) {
            try {
                jsonObj = new JSONObject(strJson);
            }
            catch (JSONException je) {
                throw new CocktailException(errMsg, je, ExceptionType.ProviderCredentialFormatInvalid);
            }
            catch (Exception e) {
                throw new CocktailException(errMsg, e, ExceptionType.ProviderCredentialFormatInvalid);
            }
        }

        return jsonObj;
    }

    /**
     * AWS IAM permission만 별도 체크시 사용
     *
     * @param providerAccount
     * @return
     * @throws Exception
     */
    public boolean validAwsIAMPermission(ProviderAccountVO providerAccount) throws Exception {
        boolean isValid = false;
        if (providerAccount != null) {
            if (providerAccount.getAccountUseType() == ProviderAccountType.ACCESS_KEY && providerAccount.getProviderCode() == ProviderCode.AWS) {
                JSONObject configJson = this.convertProviderJsonStringToObject(providerAccount.getConfig(), "config format is invalid");
                if (configJson != null) {
                    boolean isCheckIam = configJson.getBoolean("aws_iam_permission_check");
                    if (isCheckIam) {
                        AmazonIdentityManagement iamClient = null;
                        try {
                            iamClient = awsResourceService.getIAMClientUsingEncryptedAuth(null, CryptoUtils.decryptAES(providerAccount.getCredential()));
                            if (iamClient == null) {
                                throw new CocktailException("AWS IAM Authentication information is not valid!!", ExceptionType.ProviderAuthenticationIsInvalid);
                            }
                            try {
                                awsResourceService.getUsersTest(iamClient);
                                isValid = true;
                            }
                            catch (CocktailException ce) {
                                throw new CocktailException("AWS IAM Authentication information is not valid!!", ce, ExceptionType.ProviderAuthenticationIsInvalid);
                            }
                            catch (Exception ex) {
                                throw new CocktailException("AWS IAM Authentication information is not valid!!", ex, ExceptionType.ProviderAuthenticationIsInvalid);
                            }
                        } catch (CocktailException e) {
                            log.warn("AWS IAM permission is invalid.", e);
                        } finally {
                            if (iamClient != null) {
                                iamClient.shutdown();
                            }
                        }

                    }
                }
            }
        } else {
            throw new CocktailException("The providerAccount is empty.", ExceptionType.ProviderNotFound);
        }

        return isValid;
    }

    /**
     * Provider Account 수정전 유효성 체크
     * @param providerAccount
     * @throws Exception
     */
    public void checkProviderAccountValidation(ProviderAccountVO providerAccount) throws Exception {
        ProviderAccountVO pvCurr = this.getProviderAccount(providerAccount.getProviderAccountSeq());
        if (pvCurr == null) {
            throw new CocktailException("Provider not found (modifyType error)", ExceptionType.ProviderNotFound);
        }else{
            // cluster에서 사용 중인 account 수정 불가 항목 체크
            boolean checkCluster = false;
            if(pvCurr.getProviderCode() == ProviderCode.AWS || pvCurr.getProviderCode() == ProviderCode.GCP || pvCurr.getProviderCode() == ProviderCode.AZR){
                if(pvCurr.getAccountUseType() != providerAccount.getAccountUseType()
                        || (providerAccount.getAccountUseType() == ProviderAccountType.METERING
                                && (StringUtils.isNotBlank(providerAccount.getCredential()) || !StringUtils.equals(pvCurr.getConfig(), providerAccount.getConfig())))
                        ){
                    checkCluster = true;
                }
            }
            else {
                // if(pvCurr.getProviderCode() == ProviderCode.OPM || pvCurr.getProviderCode() == ProviderCode.RVS){
                if(pvCurr.getAccountUseType() != providerAccount.getAccountUseType()){
                    checkCluster = true;
                }
            }

            if(checkCluster){
                List<String> clusters = this.getClusterUsingProviderAccount(pvCurr.getProviderAccountSeq(), providerAccount.getAccountUseType());
                if (clusters.size() > 0) {
                    throw new CocktailException(String.format("Provider account used by cluster(s): %s", clusters),
                            ExceptionType.ProviderUsedByCluster);
                }
            }

        }

        ExceptionMessageUtils.checkParameter("providerName", providerAccount.getProviderName(), 50, true);

//        this.setProviderAccount(providerAccount);
    }

    public List<String> getClusterUsingProviderAccount(Integer providerAccountSeq, ProviderAccountType providerAccountType) throws Exception{
        List<String> clusters;
        if(providerAccountType == ProviderAccountType.USER) {
            clusters = this.getClustersUsingAccount(providerAccountSeq);
        }
        else if(providerAccountType == ProviderAccountType.ACCESS_KEY) {
            clusters = this.getClustersUsingAccessKeyAccount(providerAccountSeq);
        }
        else if(providerAccountType == ProviderAccountType.METERING) {
            clusters = this.getClustersUsingMeteringAccount(providerAccountSeq);
        }
        else {
            throw new CocktailException("Invalid Provider Account Type", ExceptionType.InvalidInputData);
        }

        return clusters;
    }
}
