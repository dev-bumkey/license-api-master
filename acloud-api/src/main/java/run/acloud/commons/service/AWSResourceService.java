package run.acloud.commons.service;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.commons.provider.AWSClient;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;
import java.util.Optional;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 6.
 */
@Slf4j
@Service
public class AWSResourceService {

    public static final String USER_PATH_COCKTAIL = "/cocktail/eks/";

    @Autowired
    private AWSClient awsClient;


    /** ******************************************************************************* **/

    public AmazonIdentityManagement getIAMClientUsingEncryptedAuth(String regionCode, String encryptedAuth) throws CocktailException {
        try {
            JSONObject credentialJson = null;

            if(StringUtils.isNotBlank(encryptedAuth)){
                String credentialDecrypted = CryptoUtils.decryptAES(encryptedAuth);
                try {
                    credentialJson = new JSONObject(credentialDecrypted);
                } catch (JSONException e) {
                    throw new CocktailException("Credential format is invalid by Authentication", ExceptionType.AWSIAMApiFail);
                }
            } else {
                throw new CocktailException("Credential format is null by Authentication", ExceptionType.AWSIAMApiFail);
            }
            credentialJson = Optional.ofNullable(credentialJson).orElseGet(() ->new JSONObject());
            String decryptedAccessKey = credentialJson.getString("access_key_id");
            String decryptedSecretKey = credentialJson.getString("secret_access_key");

            if (StringUtils.isNotBlank(regionCode)) {
                return this.getIAMClient(regionCode, decryptedAccessKey, decryptedSecretKey);
            } else {
                return this.getIAMClient(decryptedAccessKey, decryptedSecretKey);
            }
        } catch (JSONException e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public AmazonIdentityManagement getIAMClient(String accessKey, String secretKey) {
        return awsClient.createIAM(accessKey, secretKey);
    }

    public AmazonIdentityManagement getIAMClient(String regionCode, String accessKey, String secretKey) {
        return awsClient.createIAM(regionCode, accessKey, secretKey);
    }

    public CreateUserResult createUserUsingCocktailPath(AmazonIdentityManagement iam, String userName) {
        return this.createUser(iam, userName, USER_PATH_COCKTAIL);
    }

    public CreateUserResult createUser(AmazonIdentityManagement iam, String userName, String path) {

        if (StringUtils.isNotBlank(userName)) {
            CreateUserRequest request = new CreateUserRequest().withUserName(userName);
            if (StringUtils.isNotBlank(path)) {
                request.setPath(path);
            }
            CreateUserResult response = iam.createUser(request);
            log.debug("Created user: {}", JsonUtils.toPrettyString(response));

            return response;

        }

        return null;
    }

    public DeleteUserResult deleteUser(AmazonIdentityManagement iam, String userName) {

        DeleteUserRequest request = new DeleteUserRequest().withUserName(userName);
        DeleteUserResult response = iam.deleteUser(request);
        log.debug("deleted user: {}", JsonUtils.toPrettyString(response));

        return response;

    }

    public DeleteUserResult deleteUserWithAccessKey(AmazonIdentityManagement iam, String userName)  {
        /**
         * User 삭제시 AccessKey를 먼저 삭제하여야 함.
         */
        // User AccessKey 삭제
        boolean done = false;
        ListAccessKeysRequest listAccessKeysRequest = new ListAccessKeysRequest().withUserName(userName);
        DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest().withUserName(userName);

        while (!done) {

            ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

            for (AccessKeyMetadata metadata : listAccessKeysResult.getAccessKeyMetadata()) {
                log.debug("Retrieved access key {}", JsonUtils.toPrettyString(metadata));
                deleteAccessKeyRequest.setAccessKeyId(metadata.getAccessKeyId());
                DeleteAccessKeyResult deleteAccessKeyResult = iam.deleteAccessKey(deleteAccessKeyRequest);
                log.debug("Deleted access key {}", JsonUtils.toPrettyString(deleteAccessKeyResult));
            }

            listAccessKeysRequest.setMarker(listAccessKeysResult.getMarker());

            if (!listAccessKeysResult.getIsTruncated()) {
                done = true;
            }
        }


        // User 삭제
        return this.deleteUser(iam, userName);
    }

//    public List<User> getUsersWithCocktailPathPrefix(AmazonIdentityManagement iam) throws Exception {
//        return this.getUsers(iam, USER_PATH_COCKTAIL);
//    }

//    public List<User> getUsers(AmazonIdentityManagement iam, String pathPrefix) throws Exception {
//        List<User> users = Lists.newArrayList();
//
//        try {
//            boolean done = false;
//            ListUsersRequest request = new ListUsersRequest();
//            if (StringUtils.isNotBlank(pathPrefix)) {
//                request.setPathPrefix(pathPrefix);
//            }
//
//            while(!done) {
//                ListUsersResult response = iam.listUsers(request);
//
//                for(User user : response.getUsers()) {
//                    users.add(user);
//                }
//
//                request.setMarker(response.getMarker());
//
//                if(!response.getIsTruncated()) {
//                    done = true;
//                }
//            }
//        } catch (Exception e) {
//            this.exceptionHandle(e);
//        }
//
//        return users;
//    }

    public List<User> getUsersTest(AmazonIdentityManagement iam) {
        List<User> users = Lists.newArrayList();

        ListUsersRequest request = new ListUsersRequest();
        request.setMaxItems(1);
        ListUsersResult response = iam.listUsers(request);
        for(User user : response.getUsers()) {
            users.add(user);
        }

        return users;
    }

//    public User getUser(AmazonIdentityManagement iam, String userName) throws Exception {
//        try {
//            GetUserRequest request = new GetUserRequest();
//            request.setUserName(userName);
//
//            GetUserResult result = iam.getUser(request);
//
//            return result.getUser();
//        } catch (Exception e) {
//            this.exceptionHandle(e);
//        }
//
//        return null;
//    }

//    public DeleteAccessKeyResult deleteAccessKey(AmazonIdentityManagement iam, String userName, String accessKey) throws Exception {
//        try {
//            DeleteAccessKeyRequest request = new DeleteAccessKeyRequest()
//                    .withAccessKeyId(accessKey)
//                    .withUserName(userName);
//
//            DeleteAccessKeyResult response = iam.deleteAccessKey(request);
//            log.debug("deleted user: {}", JsonUtils.toPrettyString(response));
//
//            return response;
//        } catch (Exception e) {
//            this.exceptionHandle(e);
//        }
//
//        return null;
//    }


    private void exceptionHandle(Exception e) throws CocktailException{
        this.exceptionHandle(e, true);
    }

    private void exceptionHandle(Exception e, boolean isThrow) throws CocktailException{

        String errMsg = "";
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause != null) {
            errMsg = rootCause.getMessage();
        }
        if (StringUtils.isBlank(errMsg)) {
            for(Throwable throwableRow : ExceptionUtils.getThrowableList(e)){
                errMsg = throwableRow.getMessage();
                break;
            }
        }

        CocktailException ce = new CocktailException(e.getMessage(), e, ExceptionType.AWSIAMApiFail, errMsg);

        if(isThrow){
            log.error(ce.getMessage(), ce);
            throw ce;
        }
    }
}
