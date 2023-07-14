package run.acloud.api.configuration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.service.ProviderAccountService;
import run.acloud.api.configuration.vo.ProviderAccountVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

@InHouse // InHouse 처리해도 될 것으로 생각된다.. 필요시 오픈..
@Tag(name = "Cluster Provider Account", description = "클러스터 Provider의 접속 정보 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/provideraccount")
@RestController
@Validated
public class ProviderAccountController {

    private final ProviderAccountService providerAccountService;

    @Autowired
    public ProviderAccountController(ProviderAccountService service) {
        this.providerAccountService = service;
    }

    @Autowired
    private AccountService accountService;

    @Operation(summary = "Provider Account의 사용 유형 조회", description = "Provider Account의 사용 유형을 조회한다 (USER, METERING)")
    @GetMapping(value = "/useTypes")
    public List<CodeVO> getProviderAccountUseTypes() throws Exception {
        return this.providerAccountService.getProviderAccountUseTypes();
    }

    @Operation(summary = "Provider Account 목록 조회", description = "Provider Account의 목록을 조회한다.")
    @GetMapping(value = "/list")
    public List<ProviderAccountVO> getProviderAccounts(
            @Parameter(description = "프로바이더 유형", schema = @Schema(allowableValues = {"USER","METERING"})) @RequestParam(required = false) String accountUseType,
            @Parameter(description = "계정 번호") @RequestParam(required = false) Integer accountSeq,
            @Parameter(description = "서비스 번호") @RequestParam(required = false) Integer serviceSeq
     ) throws Exception {
        log.debug("[BEGIN] getProviderAccounts");

        List<ProviderAccountVO> results = providerAccountService.getProviderAccounts(null, accountUseType, accountSeq, serviceSeq, "Y");
        if (CollectionUtils.isNotEmpty(results)) {
            for (ProviderAccountVO pa : results) {
                if (pa != null) {
                    //            pa.setApiAccountId("");
                    pa.setApiAccountPassword("");
                    pa.setCredential(null);
                }
            }
        }

        log.debug("[END  ] getProviderAccounts");

        return results;
    }

//    @PostMapping(value = "")
//    public ProviderAccountVO addProviderAccount(@RequestParam String providerCode, @RequestParam String providerName, @RequestParam String accountUseType, @RequestParam String description, @RequestParam String credential) throws Exception {
//        JSONObject json = null;
//        if (credential != null && !"".equals(credential.trim())) {
//            try {
//                json = new JSONObject(credential);
//            } catch (Exception e) {
//                throw new CocktailException("Credential format is invalid", ExceptionType.InvalidParameter);
//            }
//        }
//
//        if (json == null && ("AWS".equals(providerCode) || "GCP".equals(providerCode) || "AZR".equals(providerCode))) {
//            throw new CocktailException("Credential is empty", ExceptionType.InvalidParameter);
//        }
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("providerAccountSeq", -1);
//        params.put("providerCode", providerCode);
//        params.put("providerName", providerName);
//        params.put("accountUseType", accountUseType);
//        params.put("description", description);
//        params.put("status", "Y");
//
//        ProviderCode prvCode = ProviderCode.valueOf(providerCode);
//        if (json == null) {
//            params.put("accountGroupId", "");
//            params.put("apiAccountId", "");
//            params.put("apiAccountPassword", "");
//        } else {
//            fillCredentialParameters(params, prvCode, json, credential);
//        }
//        this.providerAccountService.addProviderAccount(params);
//
//        List<ProviderAccountVO> accounts = this.providerAccountService.getProviderAccounts(params);
//        accounts.get(0).setApiAccountId("");
//        accounts.get(0).setApiAccountPassword("");
//        accounts.get(0).setCredential("");
//        return accounts.get(0);
//    }

//    private void fillCredentialParameters(Map<String, Object> params, ProviderCode prvCode, JSONObject json, String credential) throws Exception {
//        switch (prvCode) {
//            case AWS:
//                String accessKey = json.getString("accessKey");
//                String secretKey = json.getString("secretKey");
//                String accountGroupId = checkAwsCredential(accessKey, secretKey);
//                if (accountGroupId == null) {
//                    throw new CocktailException("AccessKey or SecretKey is invalid", ExceptionType.ProviderAccessKeyOrSecretKeyInvalid);
//                }
//                params.put("accountGroupId",accountGroupId);
//                params.put("apiAccountId", accessKey);
//                params.put("apiAccountPassword", secretKey);
//                break;
//            case GCP:
//                String projectId = json.getString("project_id");
//                params.put("accountGroupId",projectId);
//                params.put("apiAccountId", projectId);
//                params.put("apiAccountPassword", credential);
//                break;
//            case AZR:
//                String subscriptionId = json.getString("subscription_id");
//                String tenantDomain = json.getString("tenant_domain");
//                params.put("accountGroupId", subscriptionId);
//                params.put("apiAccountId", tenantDomain);
//                params.put("apiAccountPassword", credential);
//                break;
//            default:
//                params.put("accountGroupId", "");
//                params.put("apiAccountId", "");
//                params.put("apiAccountPassword", "");
//        }
//    }

//    @Operation(summary = "클러스터 계정 등록", description = "클러스터 계정을 수정한다.")
//    @PostMapping(value = "")
//    public ProviderAccountVO addProviderAccount(@RequestHeader(name = "user-id" ) Integer userSeq,
//                                                @RequestBody ProviderAccountVO providerAccount) throws Exception {
//
////        providerAccount.setCreator(userSeq);
////        providerAccount.setUpdater(userSeq);
//        providerAccount.setUseYn("Y");
//
//        /**
//         * add provider_account
//         */
//        this.providerAccountService.addProviderAccount(providerAccount);
//
//        /**
//         * add account & provider_account mapping
//         */
//        accountService.addProviderOfAccount(providerAccount.getAccountSeq(), Arrays.asList(providerAccount.getProviderAccountSeq()), providerAccount.getCreator());
//
//        providerAccount = this.providerAccountService.getProviderAccount(providerAccount.getProviderAccountSeq());
//        providerAccount.setApiAccountId("");
//        providerAccount.setApiAccountPassword("");
//        providerAccount.setCredential(null);
//        return providerAccount;
//    }

//        providerAccount.setApiAccountId("");
//    @PostMapping(value = "/{providerAccountSequence}")
//    public ProviderAccountVO editProviderAccount(@PathVariable int providerAccountSequence, @RequestParam String providerName, @RequestParam String accountUseType, @RequestParam(required = false) String description, @RequestParam String status, @RequestParam(required = false) String credential) throws Exception {
//        Map<String, Object> params = new HashMap<>();
//        params.put("providerAccountSeq", providerAccountSequence);
//        List<ProviderAccountVO> accounts = this.providerAccountService.getProviderAccounts(params);
//        if (accounts.size() == 0) {
//            throw new CocktailException("Provider not found", ExceptionType.DataNotFound);
//        }
//
//        JSONObject json = null;
//        if (credential != null && !"".equals(credential.trim())) {
//            try {
//                json = new JSONObject(credential);
//            } catch (Exception e) {
//                throw new CocktailException("Credential format is invalid", ExceptionType.InvalidInputData);
//            }
//        }
//
//        params.put("providerName", providerName);
//        params.put("accountUseType", accountUseType);
//        params.put("description", description);
//        params.put("status", status);
//
//        if (json != null) {
//            fillCredentialParameters(params, accounts.get(0).getProviderCode(), json, credential);
//            String oldAccountGroupId = accounts.get(0).getAccountGroupId();
//            if (!oldAccountGroupId.equals(params.get("accountGroupId").toString())) {
//                throw new CocktailException("Cloud Provider Key is not matched", ExceptionType.CloudProviderNotMatch);
//            }
//        }
//        this.providerAccountService.editProviderAccount(params);
//
//        accounts = this.providerAccountService.getProviderAccounts(params);
//        accounts.get(0).setApiAccountId("");
//        accounts.get(0).setApiAccountPassword("");
//        accounts.get(0).setCredential("");
//        return accounts.get(0);
//    }

//    @PutMapping(value = "/{providerAccountSequence}")
//    public ProviderAccountVO editProviderAccount(@RequestHeader(name = "user-id" ) Integer userSeq,
//                                                 @PathVariable int providerAccountSequence,
//                                                 @RequestBody ProviderAccountVO providerAccount) throws Exception {
//        ProviderAccountVO pvCurr = this.providerAccountService.getProviderAccount(providerAccountSequence);
//        if (pvCurr == null) {
//            throw new CocktailException("Provider not found", ExceptionType.ProviderNotFound);
//        }else{
//            // cluster에서 사용 중인 account 수정 불가 항목 체크
//            boolean checkCluster = false;
//            if(pvCurr.getProviderCode() == ProviderCode.AWS || pvCurr.getProviderCode() == ProviderCode.GCP || pvCurr.getProviderCode() == ProviderCode.AZR){
//                if(pvCurr.getAccountUseType() != providerAccount.getAccountUseType()
//                        || !StringUtils.equals(pvCurr.getAccountGroupId(), providerAccount.getAccountGroupId())
//                        || (pvCurr.getProviderCode() == ProviderCode.GCP
//                                && providerAccount.getAccountUseType() == ProviderAccountType.USER
//                                && !StringUtils.equals(pvCurr.getApiAccountId(), providerAccount.getProjectId()))
//                        || (providerAccount.getAccountUseType() == ProviderAccountType.METERING
//                                && StringUtils.isNotBlank(providerAccount.getCredential()) && StringUtils.isNotBlank(providerAccount.getConfig()))
//                        ){
//                    checkCluster = true;
//                }
//            }else{
//                // if(pvCurr.getProviderCode() == ProviderCode.OPM || pvCurr.getProviderCode() == ProviderCode.RVS){
//                if(pvCurr.getAccountUseType() != providerAccount.getAccountUseType()){
//                    checkCluster = true;
//                }
//            }
//
//            if(checkCluster){
//                List<String> clusters = this.providerAccountService.getClustersUsingAccount(pvCurr.getProviderAccountSeq());
//                if (clusters.size() > 0) {
//                    throw new CocktailException(String.format("Provider account used by cluster(s): %s", clusters),
//                            ExceptionType.ProviderUsedByCluster);
//                }
//            }
//        }
//
//        ExceptionMessageUtils.checkParameter("providerName", providerAccount.getProviderName(), 50, true);
//
//        providerAccount.setProviderAccountSeq(providerAccountSequence);
////        providerAccount.setUpdater(userSeq);
//
//        this.providerAccountService.editProviderAccount(providerAccount);
//
//        pvCurr = this.providerAccountService.getProviderAccount(providerAccountSequence);
//        pvCurr.setApiAccountId("");
//        pvCurr.setApiAccountPassword("");
//        pvCurr.setCredential("");
//
//        return pvCurr;
//    }

//    @DeleteMapping(value = "/{providerAccountSequence}")
//    public ResultVO removeProviderAccount(@RequestHeader(name = "user-id" ) Integer userSeq,
//                                          @PathVariable int providerAccountSequence) throws Exception {
//
//        ProviderAccountVO pvCurr = this.providerAccountService.getProviderAccount(providerAccountSequence);
//        if (pvCurr == null) {
//            throw new CocktailException("Provider not found", ExceptionType.ProviderNotFound);
//        }
//        List<String> clusters = this.providerAccountService.getClustersUsingAccount(pvCurr.getProviderAccountSeq());
//        if (CollectionUtils.isNotEmpty(clusters)) {
//            throw new CocktailException(String.format("Provider account used by cluster(s): %s", clusters),
//                    ExceptionType.ProviderUsedByCluster);
//        }
//
//        pvCurr.setProviderAccountSeq(providerAccountSequence);
//        pvCurr.setUpdater(userSeq);
//        this.providerAccountService.removeProviderAccount(pvCurr);
//
//        return new ResultVO();
//    }

    @Operation(summary = "Provider Account의 상세 정보", description = "Provider Account의 상세 정보를 응답한다.")
    @GetMapping(value = "/{providerAccountSeq}")
    public ProviderAccountVO getProviderAccount(@PathVariable int providerAccountSeq) throws Exception {
        ProviderAccountVO providerAccount = this.providerAccountService.getProviderAccount(providerAccountSeq);
        if (providerAccount == null) {
            throw new CocktailException("Provider account not found", ExceptionType.ProviderNotFound);
        }else{
            List<String> clusters = providerAccountService.getClusterUsingProviderAccount(providerAccountSeq, providerAccount.getAccountUseType());
            if (CollectionUtils.isNotEmpty(clusters)) {
                providerAccount.setClusterNames(clusters);
            }
        }
//        accounts.get(0).setApiAccountId("");
        providerAccount.setApiAccountPassword("");
        providerAccount.setCredential("");

        return providerAccount;
    }

//    private String checkAwsCredential(String accessKey, String secretKey) {
//        log.debug("accessKey="+accessKey);
//        log.debug("secretKey="+secretKey);
//        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
//        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(awsCreds);
//        AmazonIdentityManagement aim = null;
//        try {
//            aim = AmazonIdentityManagementClientBuilder.standard()
//                    .withCredentials(credentialsProvider)
//                    .withRegion(Regions.DEFAULT_REGION)
//                    .build();
//
//            GetUserResult result = aim.getUser();
//            /* ARN Format => arn:aws:iam::123456789012:/user/Bob */
//            String[] temp = result.getUser().getArn().split(":");
//            log.debug("******** [BillingAccountId=" + temp[4] + "] ********");
//            return temp[4];
//        } catch (Exception e) {
//            log.debug(e.getMessage());
//            e.printStackTrace();
//            return null;
//        } finally {
//            if (aim != null) {
//                aim.shutdown();
//            }
//        }
//    }

//    private boolean checkGoogleCredential(String projectId, String credential) {
//        InputStream credentialsStream = null;
//        try {
//            credentialsStream = new ByteArrayInputStream(credential.getBytes());
//            ServiceAccountCredentials serviceAccountCredentials = ServiceAccountCredentials.fromStream(credentialsStream);
//
//            Storage storage = StorageOptions.newBuilder()
//                    .setProjectId(projectId)
//                    .setCredentials(serviceAccountCredentials)
//                    .build()
//                    .getService();
//            return (storage == null);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        } finally {
//            if (credentialsStream != null) {
//                try {
//                    credentialsStream.close();
//                } catch (Exception ignore) {
//                    // do nothing
//                }
//            }
//        }
//        return true;
//    }

//    private boolean checkAzureCredential(JSONObject json) {
//        return true;
//    }
}
