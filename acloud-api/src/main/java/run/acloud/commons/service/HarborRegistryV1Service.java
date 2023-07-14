package run.acloud.commons.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.vo.AccountRegistryVO;
import run.acloud.api.configuration.vo.ImageRepositoryTagVO;
import run.acloud.api.configuration.vo.RegistryProjectVO;
import run.acloud.commons.client.HarborApiClient;
import run.acloud.commons.client.harbor.v1.ApiException;
import run.acloud.commons.client.harbor.v1.ApiResponse;
import run.acloud.commons.client.harbor.v1.HarborApiClientV1;
import run.acloud.commons.client.harbor.v1.api.ProductsApi;
import run.acloud.commons.client.harbor.v1.model.*;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.HarborRegistryProjectMemberRole;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.*;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 6.
 */
@Slf4j
@Service
public class HarborRegistryV1Service implements IHarborRegistryService {

    public static final String DEFAULT_AUTH_TYPE = "basic";

    private static final String DEFAULT_EMAIL = "@acornsoft.io";
    private static final String MAIL_REGEX = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
    private static final String HARBOR_IMAGE = "%s/%s";
    private static final String LIST_URL = "%s/api/repositories?project_id=%s";
    private static final String SEARCH_URL = "%s/api/search?q=%s";
    private static final String TAGS_URL = "%s/api/repositories/%s/tags"; // harbor version up, the detail parameter don't be used
    private static final String PROJECTS_URL = "%s/api/projects";
    private static final String PROJECT_URL = "%s/api/projects/%d";
    private static final String USER_URL = "%s/api/users";
    private static final String PASSWORD_USER = "%s/api/users/%d/password";
    private static final String USER_EDIT_URL = "%s/api/users/%d";
    private static final String MEMBERS_URL = "%s/api/projects/%d/members";
    private static final String MEMBER_URL = "%s/api/projects/%d/members/%d";

    private static final String TAG_URL = "%s/api/repositories/%s/tags/%s";
    private static final String IMG_URL = "%s/api/repositories/%s";
    private static final String TARGET_PING = "%s/api/targets/ping";

    @Autowired
    private RegistryPropertyService registry;

    @Autowired
    private HarborApiClient harborApiClient;

    private HarborApiClientV1 harborApiClientV1;

    @Override
    public ImageRepositoryTagVO getImageTagInfo(String projectName , String imageName , String tag) throws Exception {

        ImageRepositoryTagVO tagImage = new ImageRepositoryTagVO();
        tagImage.setImageName(String.format(HARBOR_IMAGE, projectName, imageName));

        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }
        DetailedTag detailedTag = this.getTag(productsApi, tagImage.getImageName(), tag);

        if (detailedTag != null) {
            // data set
            tagImage.setTagName(detailedTag.getName());
            tagImage.setDigest(detailedTag.getDigest());
            tagImage.setArchitecture(detailedTag.getArchitecture());
            tagImage.setDockerVersion(detailedTag.getDockerVersion()); // 2.5.x 버전에서 값이 조회되지 않아 사용안함.
            tagImage.setCreated(detailedTag.getCreated());
            tagImage.setSize(detailedTag.getSize());

            return tagImage;
        }

        return null;
    }

    @Override
    public RegistryProjectVO addProject(HarborProjectReqVO projectReq) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        ProjectReq req = new ProjectReq();
        ProjectMetadata pMeta = new ProjectMetadata();
        pMeta.setPublic(projectReq.isPublic().toString());
        req.metadata(pMeta).setProjectName(projectReq.getProjectName());
        Project addProject = this.addProject(productsApi, req);

        return this.setProject(addProject);
    }

    @Override
    public void deleteProject(Integer projectId) throws Exception {
        if (projectId == null) {
            return;
        }
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        this.deleteProject(productsApi, projectId);
    }

    @Override
    public List<RegistryProjectVO> getProjectList(String projectName) throws Exception {

        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }
        List<Project> projects = this.getProjects(productsApi, projectName, null, null, null, null);

        List<RegistryProjectVO> registryProjects = new ArrayList<>();

        if(CollectionUtils.isNotEmpty(projects)) {
            for (Project p : projects) {
                registryProjects.add(this.setProject(p));
            }
        }
        return registryProjects;
    }

    @Override
    public RegistryProjectVO getProject(String projectName) throws Exception {

        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }
        Project p = this.getProject(productsApi, projectName);

        return this.setProject(p);
    }

    @Override
    public RegistryProjectVO getProject(Integer projectId) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        Project p = this.getProject(productsApi, projectId);

        return this.setProject(p);
    }

    private RegistryProjectVO setProject(Project p) {
        if (p != null) {
            RegistryProjectVO rp = new RegistryProjectVO();

            rp.setProjectId(p.getProjectId());
            rp.setName(p.getName());
            rp.setPublic(BooleanUtils.toBoolean(p.getMetadata().getPublic())); // harbor version up으로 인해 public 를 metadata내에서 가져오게 변경
            rp.setOwnerId(p.getOwnerId());
            rp.setOwenrName(p.getOwnerName());

            return rp;
        }

        return null;
    }

    @Override
    public HarborUserRespVO addUser(HarborUserReqVO userReq) throws Exception {

        User reqUser = new User();
        reqUser.setUsername(userReq.getUsername());
        if (userReq.getUsername().matches(HarborRegistryV1Service.MAIL_REGEX)) {
            reqUser.setEmail(userReq.getUsername());
        } else {
            reqUser.setEmail(String.format("%s%s", userReq.getUsername(), HarborRegistryV1Service.DEFAULT_EMAIL));
        }
        reqUser.setPassword(userReq.getPassword());
        reqUser.setRealname(userReq.getUsername());

        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }
        User user = this.addUser(productsApi, reqUser);

        if (user != null) {
            HarborUserRespVO resp = new HarborUserRespVO();
            resp.setEmail(user.getEmail());
            resp.setRealname(user.getRealname());
            resp.setUserId(user.getUserId());
            resp.setUsername(user.getUsername());
            return resp;
        }

        return null;
    }

    @Override
    public void deleteUser(Integer userId) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        this.deleteUser(productsApi, userId);
    }

    @Override
    public void deleteUser(String username) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        this.deleteUser(productsApi, username);
    }

    @Override
    public HarborUserRespVO getUser(Integer userId) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        User user = this.getUser(productsApi, userId);

        return this.setUser(user);
    }

    @Override
    public HarborUserRespVO getUser(String username) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        User user = this.getUser(productsApi, username);

        return this.setUser(user);
    }

    private HarborUserRespVO setUser(User user) {
        if (user != null) {
            HarborUserRespVO resp = new HarborUserRespVO();
            resp.setEmail(user.getEmail());
            resp.setRealname(user.getRealname());
            resp.setUserId(user.getUserId());
            resp.setUsername(user.getUsername());
            return resp;
        }

        return null;
    }

    @Override
    public List<HarborPermissionVO> getCurrentUserPermissions(String scope, Boolean relative) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        List<Permission> curr = this.getCurrentUserPermissions(productsApi, scope, relative);

        List<HarborPermissionVO> permissions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(curr)) {
            for (Permission p : curr) {
                HarborPermissionVO h = new HarborPermissionVO();
                h.setResource(p.getResource());
                h.setAction(p.getAction());
                permissions.add(h);
            }
        }

        return permissions;
    }

    @Override
    public ResultVO getCurrentUserPermissionsStatus(String scope, Boolean relative) throws Exception {

        ResultVO result = new ResultVO();

        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        int status = this.getCurrentUserPermissionsStatus(productsApi, scope, relative);

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

    @Override
    public HarborProjectMemberVO getMemberOfProject(Integer projectId, String userName) throws Exception{
        if (projectId == null || StringUtils.isBlank(userName)) {
            log.warn("projectId or userName is empty");
            return null;
        }

        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        ProjectMemberEntity projectMemberEntity = this.getProjectMember(productsApi, projectId, userName);

        if (projectMemberEntity != null) {
            HarborProjectMemberVO member = new HarborProjectMemberVO();
            member.setId(projectMemberEntity.getId());
            member.setProjectId(projectMemberEntity.getProjectId());
            member.setEntityName(projectMemberEntity.getEntityName());
            member.setRoleName(projectMemberEntity.getRoleName());
            member.setRoleId(projectMemberEntity.getRoleId());
            member.setEntityId(projectMemberEntity.getEntityId());
            member.setEntityType(projectMemberEntity.getEntityType());
            //harbor version up 으로 인해 변경, username -> entityname
//            user.setUserSeq(projectMemberEntity.getId());
//            user.setUserId(projectMemberEntity.getEntityName());
//            user.setUserRole(projectMemberEntity.getRoleId().toString());
            return member;
        }

        return null;
    }

    @Override
    public void addMembersToProjects(List<Integer> projects, List<HarborProjectMemberVO> users, Boolean ignoreException) throws Exception {
        if (!isValidUsersAndProjects(users, projects)) {
            return;
        }

        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        for (HarborProjectMemberVO user : users) {
            this.addMemberToProjectsCommon(productsApi, projects, user, ignoreException);
        }
    }

    @Override
    public void addMemberToProjects(List<Integer> projects, HarborProjectMemberVO user, Boolean ignoreException) throws Exception {
        if (CollectionUtils.isEmpty(projects)) {
            return;
        }

        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        this.addMemberToProjectsCommon(productsApi, projects, user, ignoreException);
    }

    @Override
    public void addMemberToProject(Integer projectId, HarborProjectMemberVO user, Boolean ignoreException) throws Exception {
        if (projectId == null) {
            return;
        }
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        this.addMemberToProjectsCommon(productsApi, Collections.singletonList(projectId), user, ignoreException);
    }

    private Boolean isValidUsersAndProjects(List<HarborProjectMemberVO> users, List<Integer> projects) {
        if (users == null || users.isEmpty()) {
            log.warn("users empty.");
            return false;
        } else if (projects == null || projects.isEmpty()) {
            log.warn("projects empty");
            return false;
        }
        return true;
    }

    private void addMemberToProjectsCommon(ProductsApi productsApi, List<Integer> projects, HarborProjectMemberVO user, Boolean ignoreException) throws Exception {
        if (StringUtils.isBlank(user.getEntityName())) {
            log.warn("user id is empty");
            return;
        }

        if (productsApi == null) {
            productsApi = this.getProductsApiWithAdminAuth();
        }

        /**
        // 기존 harbor 포맷
         {
         "roles": [
         0
         ],
         "username": "string"
         }

        // 변경된 harbor 포맷
        {
            "role_id": 0,
            "member_user": {
                "user_id": 0,
                "username": "string"
            },
            "member_group": {
                "id": 0,
                "group_name": "string",
                "group_type": 0,
                "ldap_group_dn": "string"
            }
        }
        */

        /**
         * Harbor Registry Role
         *  - Project Admin : 1
         *  - Developer : 2
         *  - Guest : 3
         */
        ProjectMember projectMember = new ProjectMember();
        UserEntity memberUser = new UserEntity();
        memberUser.setUsername(user.getEntityName());
        if (user.getRoleId() != null) {
            projectMember.memberUser(memberUser).setRoleId(user.getRoleId());
        } else {
            projectMember.memberUser(memberUser).setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());
        }

        try {
            for (Integer projectId : projects) {
                this.addProjectMember(productsApi, projectId, projectMember);
            }
        } catch (Exception e) {
            if (ignoreException) {
                log.warn("Ignore not ok status: {}", e);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void updateMemberOfProject(Integer projectId, Integer mid, Integer roleId) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }

        this.updateProjectMember(productsApi, projectId, mid, new RoleRequest().roleId(roleId));
    }

    @Override
    public void deleteMemberOfProject(Integer projectId, Integer mid) throws Exception {
        ProductsApi productsApi = this.getProductsApi();
        this.deleteProjectMember(productsApi, projectId, mid);
    }

//    public void deleteUsersFromProjects(List<UserVO> users, List<Integer> projects, Boolean ignoreException) throws Exception {
//        if (!isValidUsersAndProjects(users, projects)) {
//            return;
//        }
//
//        HttpClient httpClient = Utils.makeHttpClient(this.registry.getUrl().startsWith("https://"));
//
//        for (UserVO user : users) {
//            this.deleteUserFromProjects(user, projects, ignoreException, httpClient);
//        }
//    }
//
//    private void deleteUserFromProjects(UserVO user, List<Integer> projects, Boolean ignoreException, HttpClient httpClient)
//        throws Exception {
//        Integer userId = this.getUserId(httpClient, user.getUserId());
//        if (userId == null) {
//            log.warn("User not found from registry: %{}", user.getUserId());
//            return;
//        }
//
//        for (Integer projectId : projects) {
//            String q = String.format(HarborRegistryService.MEMBER_URL, registry.getUrl(), projectId, userId);
//            HttpDelete delete = new HttpDelete(q);
//            setHttpHeaderAuth(delete); // Base Auth 셋팅
//            HttpResponse response = httpClient.execute(delete);
//
//            if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 400) {
//                if (ignoreException) {
//                    log.warn("Ignore not ok status: {}", response.getStatusLine().getStatusCode());
//                } else {
//                    throw new CocktailException("Fail to delete user from project", ExceptionType.RegistryDeleteUserToProjectFail, JsonUtils.toGson(response.getStatusLine()));
//                }
//            }
//
//            log.debug("delete user from project: ok");
//        }
//    }


    /**
     * tag를 제외한 이미지명 체크
     * cocktail
     *
     * @param projectName
     * @param imageName
     * @return
     * @throws Exception
     */
    @Override
    public boolean isRegistryImagesCheck(String projectName , String imageName)  {

        boolean registryImageCheck = false;

        try {

            ProductsApi productsApi = null;
            if (this.getClient() != null) {
                productsApi = this.getProductsApi(this.getClient());
            }
            List<DetailedTag> tags = this.getTags(productsApi, String.format(HARBOR_IMAGE,projectName , imageName), null, false);

            if(CollectionUtils.isNotEmpty(tags)){
                registryImageCheck = true;
            }
        }catch (Exception e){
            log.warn(e.getMessage(), e);
        }

        return registryImageCheck;

    }

    /**
     * registry 에서 이미지 삭제하는 메서드, repository(image)의 모든 이미지 삭제(모든 tag)
     *
     * @param projectName
     * @param imageName
     * @return
     */
    @Override
    public boolean deleteImagesFromProjects(String projectName , String imageName){

        boolean registryImageCheck = false;

        try {
            ProductsApi productsApi = null;
            if (this.getClient() != null) {
                productsApi = this.getProductsApi(this.getClient());
            }
            String deleteImageName = String.format(HARBOR_IMAGE, projectName, imageName);
            this.deleteRepository(productsApi, deleteImageName);
            registryImageCheck = true;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }


        return registryImageCheck;
    }

    /**
     * image tag 삭제
     *
     * @param projectName
     * @param imageName
     * @param tag
     * @return
     */
    @Override
    public boolean deleteImagesFromProjects(String projectName , String imageName, String tag){
        boolean registryImageCheck = false;

        try {
            ProductsApi productsApi = null;
            if (this.getClient() != null) {
                productsApi = this.getProductsApi(this.getClient());
            }
            String deleteImageName = String.format(HARBOR_IMAGE, projectName, imageName);
            this.deleteTag(productsApi, deleteImageName, tag);
            registryImageCheck = true;
        }catch (Exception e){
            log.warn(e.getMessage(), e);
        }

        return registryImageCheck;
    }

    @Override
    public int registriesPing(HarborRegistryPingVO registryPingReq) throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }
        PingRegistry pingRegistry = new PingRegistry();
        if (registryPingReq != null) {
            pingRegistry.setAccessKey(registryPingReq.getAccessKey());
            pingRegistry.setAccessSecret(registryPingReq.getAccessSecret());

            pingRegistry.setType(registryPingReq.getType());
            pingRegistry.setUrl(registryPingReq.getUrl());
            pingRegistry.setInsecure(registryPingReq.isInsecure());
        }
        return this.registriesPing(productsApi, pingRegistry);
    }

    /**
     * 외부 레지스트리(harbor) 접속 테스트
     * harbor 1.7 일경우 url로 처리
     *
     * @param url
     * @param username
     * @param password
     * @throws Exception
     */
    public int targetPing(String url, String username, String password, boolean insecure) throws Exception {
        HttpClient httpClient = Utils.makeHttpClient(this.registry.getUrl().startsWith("https://"));

        // harbor version up으로 인해 request json 변경
        // 기존 { project_name : "", public : 0 } ==> 변경 { project_name : "", metadata : { public : "false" } }
        JsonObject joReq = new JsonObject();
        joReq.addProperty("type", 0);
        joReq.addProperty("name", "");
        joReq.addProperty("endpoint", url);
        joReq.addProperty("username", username);
        joReq.addProperty("password", password);
        joReq.addProperty("insecure", insecure);

        String q = String.format(HarborRegistryV1Service.TARGET_PING, registry.getUrl());
        HttpPost post = new HttpPost(q);
        setHttpHeaderAuth(post); // Base Auth 셋팅
        StringEntity entity = new StringEntity(joReq.toString());
        post.setEntity(entity);
        post.setHeader("Content-type", "application/json");
        log.debug("targetPing request: \n{}", post);
        HttpResponse response = httpClient.execute(post);
        log.debug("targetPing response: \n{}", response.toString());

        return response.getStatusLine().getStatusCode();

    }

    @Override
    public HarborGeneralInfoVO getSystemGeneralInfo() {
        try {
            GeneralInfo generalInfo = this.getSysteminfo(null);
            if (generalInfo != null) {
                return this.setSystemGeneralInfo(generalInfo);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public HarborGeneralInfoVO getSystemGeneralInfo(Integer accountSeq) {
        try {
            GeneralInfo generalInfo = this.getSysteminfo(this.getProductsApiByAccountSeq(accountSeq));
            if (generalInfo != null) {
                return this.setSystemGeneralInfo(generalInfo);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public HarborGeneralInfoVO getSystemGeneralInfo(AccountRegistryVO accountRegistry) {
        try {
            GeneralInfo generalInfo = this.getSysteminfo(this.getProductsApiWithRegistry(accountRegistry));
            if (generalInfo != null) {
                return this.setSystemGeneralInfo(generalInfo);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }

        return null;
    }

    private HarborGeneralInfoVO setSystemGeneralInfo(GeneralInfo generalInfo) {
        if (generalInfo != null) {
            HarborGeneralInfoVO info = new HarborGeneralInfoVO();
            info.setWithNotary(generalInfo.isWithNotary());
            info.setRegistryUrl(generalInfo.getRegistryUrl());
            info.setExternalUrl(generalInfo.getExternalUrl());
            info.setAuthMode(generalInfo.getAuthMode());
            info.setProjectCreationRestriction(generalInfo.getProjectCreationRestriction());
            info.setSelfRegistration(generalInfo.isSelfRegistration());
            info.setHasCaRoot(generalInfo.isHasCaRoot());
            info.setHarborVersion(generalInfo.getHarborVersion());

            return info;
        }

        return null;
    }

    /**
     * registry 요청 request Header에 Basic Auth(ID & Password) 셋팅
     *
     * @author coolingi
     * @since 2019/01/02
     * @param httpRequest
     * @param <T>
     */
    private <T extends HttpRequestBase> void setHttpHeaderAuth(T httpRequest){
        httpRequest.setHeader("Authorization", okhttp3.Credentials.basic(registry.getId(), registry.getPassword()));
    }

    /**
     * registry 요청 request Header에 parameter로 넘어온 userId와 userPasswd를 이용해 Basic Auth 셋팅
     *
     * @author coolingi
     * @since 2019/01/02
     * @param httpRequest
     * @param userId
     * @param userPasswd
     * @param <T>
     */
    private <T extends HttpRequestBase> void setHttpHeaderAuth(T httpRequest, String userId, String userPasswd){
        httpRequest.setHeader("Authorization", okhttp3.Credentials.basic(userId, userPasswd));
    }

    /**
     * ============================================================================================================================================
     * Harbor client 1.x
     * ============================================================================================================================================
     */

    /**
     * harbor client 1.x 생성
     *
     * @param decryptedUsername
     * @param decryptedPassword
     * @return
     */
    public HarborApiClientV1 getClientWithEncryptedAuth(String decryptedUsername, String decryptedPassword) {
        String username = CryptoUtils.encryptAES(decryptedUsername);
        String password = CryptoUtils.encryptAES(decryptedPassword);

        return (HarborApiClientV1)harborApiClient.create(username, password);
    }

    public ApiVersionType getApiVersion() {
        return ApiVersionType.V1;
    }
    public Object getClient() {
        return this.harborApiClientV1;
    }

    /**
     * private CA 값을 설정할 수 있는 harbor client 생성 메서드.
     *
     * @param registryUrl
     * @param decryptedUsername
     * @param decryptedPassword
     * @param privateCertificateUseYn
     * @param privateCertificate
     * @return
     */
    public HarborApiClientV1 getClientWithAuth(String registryUrl, String decryptedUsername, String decryptedPassword, String privateCertificateUseYn, String privateCertificate) {
        this.harborApiClientV1 = (HarborApiClientV1)harborApiClient.create(ApiVersionType.V1, registryUrl, decryptedUsername, decryptedPassword, privateCertificateUseYn, privateCertificate);
        return this.harborApiClientV1;
    }

    public ProductsApi getProductsApi(HarborApiClientV1 apiClient) throws Exception {
        return new ProductsApi(apiClient);
    }

    public ProductsApi getProductsApi(Object apiClient) throws Exception {
        return this.getProductsApi((HarborApiClientV1)apiClient);
    }

    public ProductsApi getProductsApi() throws Exception {
        ProductsApi productsApi = null;
        if (this.getClient() != null) {
            productsApi = this.getProductsApi(this.getClient());
        }
        return productsApi;
    }

    public ProductsApi getProductsApiWithAdminAuth() throws Exception {
//        return new ProductsApi(this.getClientWithEncryptedAuth(registry.getId(), registry.getPassword()));
        return new ProductsApi(this.getClientWithAuth(registry.getUrl(), registry.getId(), registry.getPassword(), registry.getPrivateCertificateUseYn(), registry.getPrivateCertificate()));
    }

    public ProductsApi getProductsApiByAccountSeq(Integer accountSeq) throws Exception {
        return new ProductsApi(this.getClientWithAuth(registry.getUrl(accountSeq), registry.getId(accountSeq), registry.getPassword(accountSeq), registry.getPrivateCertificateUseYn(accountSeq), registry.getPrivateCertificate(accountSeq)));
    }

    public ProductsApi getProductsApiWithRegistry(AccountRegistryVO accountRegistry) throws Exception {
        return new ProductsApi(this.getClientWithAuth(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), accountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificate()));
    }

    public User addUser(ProductsApi productsApi, User user) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            if (user.getUsername().matches(HarborRegistryV1Service.MAIL_REGEX)) {
                user.setEmail(user.getUsername());
            } else {
                user.setEmail(String.format("%s%s", user.getUsername(), HarborRegistryV1Service.DEFAULT_EMAIL));
            }

            ApiResponse<Void> response = productsApi.usersPostWithHttpInfo(user);

            this.getResponse(response, "Fail to create new user", ExceptionType.RegistryAddUserFail);

            return this.getUser(productsApi, user.getUsername());
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public List<User> getUsers(ProductsApi productsApi, String username, String email, Integer page, Integer pageSize) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<List<User>> response = productsApi.usersGetWithHttpInfo(username, email, page, pageSize);

            return this.getResponse(response, "Fail to get users", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    public User getUser(ProductsApi productsApi, String username) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            List<User> users = this.getUsers(productsApi, username, null, null, null);
            if (CollectionUtils.isNotEmpty(users)) {
                Optional<User> userOptional = users.stream().filter(u -> (StringUtils.equals(u.getUsername(), username))).findFirst();
                if (userOptional.isPresent()) {
                    return userOptional.get();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public User getUser(ProductsApi productsApi, Integer userId) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<User> response = productsApi.usersUserIdGetWithHttpInfo(userId);

            return this.getResponse(response, "Fail to get user", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public void deleteUser(ProductsApi productsApi, Integer userId) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.usersUserIdDeleteWithHttpInfo(userId);

            this.getResponse(response, "Fail to delete user from registry", ExceptionType.RegistryDeleteUserToProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public void deleteUser(ProductsApi productsApi, String username) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            User user = this.getUser(productsApi, username);

            if (user == null) {
                log.warn("User not found from registry: {}", username);
                return;
            }

            this.deleteUser(productsApi, user.getUserId());

        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public List<Permission> getCurrentUserPermissions(ProductsApi productsApi, String scope, Boolean relative) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<List<Permission>> response = productsApi.usersCurrentPermissionsGetWithHttpInfo(scope, relative);

            return this.getResponse(response, "Fail to Current User Permissions", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public int getCurrentUserPermissionsStatus(ProductsApi productsApi, String scope, Boolean relative) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<List<Permission>> response = productsApi.usersCurrentPermissionsGetWithHttpInfo(scope, relative);

            return response.getStatusCode();
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return 0;
    }

    public Project addProject(ProductsApi productsApi, ProjectReq projectReq) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.projectsPostWithHttpInfo(projectReq);

            this.getResponse(response, "Fail to create new project", ExceptionType.RegistryAddProjectFail);

            List<Project> projects = this.getProjects(productsApi, projectReq.getProjectName(), null, null, null, null);
            if (CollectionUtils.isNotEmpty(projects)) {
                Optional<Project> projectsOptional = projects.stream().filter(hp -> (StringUtils.equals(hp.getName(), projectReq.getProjectName()))).findFirst();
                if (projectsOptional.isPresent()) {
                    return projectsOptional.get();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public void deleteProject(ProductsApi productsApi, Integer projectId) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.projectsProjectIdDeleteWithHttpInfo(Optional.ofNullable(projectId).map(Integer::longValue).orElseGet(() ->null));

            this.getResponse(response, "Fail to delete project from registry", ExceptionType.RegistryDeleteProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public List<Project> getProjects(ProductsApi productsApi, String name, Boolean _public, String owner, Integer page, Integer pageSize) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<List<Project>> response = productsApi.projectsGetWithHttpInfo(name, _public, owner, page, pageSize);

            return this.getResponse(response, "fail to get project list", ExceptionType.RegistryProjectListingFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    public Project getProject(ProductsApi productsApi, String projectName) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            List<Project> projects = this.getProjects(productsApi, projectName, null, null, null, null);
            if (CollectionUtils.isNotEmpty(projects)) {
                Optional<Project> projectsOptional = projects.stream().filter(hp -> (StringUtils.equals(hp.getName(), projectName))).findFirst();
                if (projectsOptional.isPresent()) {
                    return projectsOptional.get();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public Project getProject(ProductsApi productsApi, Integer projectId) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Project> response = productsApi.projectsProjectIdGetWithHttpInfo(Optional.ofNullable(projectId).map(Integer::longValue).orElseGet(() ->null));

            return this.getResponse(response, "fail to get project", ExceptionType.RegistryProjectListingFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public void addProjectMember(ProductsApi productsApi, String projectName, ProjectMember projectMember) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            boolean isExistsProject = false;
            List<Project> projects = this.getProjects(productsApi, projectName, null, null, null, null);
            if (CollectionUtils.isNotEmpty(projects)) {
                Optional<Project> projectsOptional = projects.stream().filter(hp -> (StringUtils.equals(hp.getName(), projectName))).findFirst();
                if (projectsOptional.isPresent()) {
                    isExistsProject = true;
                    this.addProjectMember(productsApi, projectsOptional.get().getProjectId(), projectMember);
                }
            }

            if (!isExistsProject) {
                throw new CocktailException("Project not found!", ExceptionType.RegistryProjectNotFound, projectName);
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public void addProjectMember(ProductsApi productsApi, Integer projectId, ProjectMember projectMember) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.projectsProjectIdMembersPostWithHttpInfo(Optional.ofNullable(projectId).map(Integer::longValue).orElseGet(() ->null), projectMember);

            this.getResponse(response, "Fail to add member to project", ExceptionType.RegistryAddUserToProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public void updateProjectMember(ProductsApi productsApi, Integer projectId, Integer mid, RoleRequest role) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.projectsProjectIdMembersMidPutWithHttpInfo(Optional.ofNullable(projectId).map(Integer::longValue).orElseGet(() ->null), mid.longValue(), role);

            this.getResponse(response, "Fail to update member role in project", ExceptionType.RegistryAddUserToProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public void deleteProjectMember(ProductsApi productsApi, Integer projectId, Integer mid) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.projectsProjectIdMembersMidDeleteWithHttpInfo(Optional.ofNullable(projectId).map(Integer::longValue).orElseGet(() ->null), mid.longValue());

            this.getResponse(response, "Fail to delete member in project", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public List<ProjectMemberEntity> getProjectMembers(ProductsApi productsApi, Integer projectId, String searchMemberName) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<List<ProjectMemberEntity>> response = productsApi.projectsProjectIdMembersGetWithHttpInfo(Optional.ofNullable(projectId).map(Integer::longValue).orElseGet(() ->null), searchMemberName);

            return this.getResponse(response, "Fail to get user in project", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    public ProjectMemberEntity getProjectMember(ProductsApi productsApi, Integer projectId, String searchMemberName) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            List<ProjectMemberEntity> projectMembers = this.getProjectMembers(productsApi, projectId, searchMemberName);
            if (CollectionUtils.isNotEmpty(projectMembers)) {
                Optional<ProjectMemberEntity> projectMembersOptional = projectMembers.stream().filter(pm -> (StringUtils.equals(pm.getEntityName(), searchMemberName))).findFirst();
                if (projectMembersOptional.isPresent()) {
                    return projectMembersOptional.get();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public ProjectMemberEntity getProjectMember(ProductsApi productsApi, Integer projectId, Integer userId) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<ProjectMemberEntity> response = productsApi.projectsProjectIdMembersMidGetWithHttpInfo(Optional.ofNullable(projectId).map(Integer::longValue).orElseGet(() ->null), userId.longValue());

            return this.getResponse(response, "Fail to get user in project", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public void deleteRepository(ProductsApi productsApi, String repoName) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.repositoriesRepoNameDeleteWithHttpInfo(repoName);

            this.getResponse(response, "Fail to delete registry", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public void deleteTag(ProductsApi productsApi, String repoName, String tag) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.repositoriesRepoNameTagsTagDeleteWithHttpInfo(repoName, tag);

            this.getResponse(response, "Fail to delete image tag", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    public List<DetailedTag> getTags(ProductsApi productsApi, String repoName, String labelId, Boolean detail) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<List<DetailedTag>> response = productsApi.repositoriesRepoNameTagsGetWithHttpInfo(repoName, labelId, detail);

            return this.getResponse(response, "Fail to get tag in project", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public DetailedTag getTag(ProductsApi productsApi, String repoName, String tag) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<DetailedTag> response = productsApi.repositoriesRepoNameTagsTagGetWithHttpInfo(repoName, tag);

            return this.getResponse(response, "Fail to get tag in project", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public int registriesPing(ProductsApi productsApi, PingRegistry pingRegistry) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<Void> response = productsApi.registriesPingPostWithHttpInfo(pingRegistry);

            return response.getStatusCode();
        } catch (Exception e) {
            this.exceptionHandle(e, false);
        }

        return 0;
    }

    public GeneralInfo getSysteminfo(ProductsApi productsApi) throws Exception {
        try {
            if (productsApi == null) {
                productsApi = this.getProductsApiWithAdminAuth();
            }

            ApiResponse<GeneralInfo> response = productsApi.systeminfoGetWithHttpInfo();

            return this.getResponse(response, "Fail to get systeminfo", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e, false);
        }

        return null;
    }

    private <T> T getResponse(ApiResponse<T> response, String message, ExceptionType exceptionType) throws Exception {
        boolean isSuccess = false;
        int statusCode = 0;
        if (response != null) {
            statusCode = response.getStatusCode();
            if (response.getStatusCode() == HttpStatus.SC_OK || response.getStatusCode() == HttpStatus.SC_CREATED) {
                return response.getData();
            } else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            } else {
                message = String.format("%s: %d", message, response.getStatusCode());
            }
        }

        if (!isSuccess) {
            throw new CocktailException(message, exceptionType, statusCode);
        }

        return null;
    }


    private void exceptionHandle(Exception e) throws Exception{
        this.exceptionHandle(e, true);
    }

    private void exceptionHandle(Exception e, boolean isThrow) throws Exception{
        CocktailException ce = new CocktailException(e.getMessage(), e, ExceptionType.RegistryApiFail);

        if(e instanceof JsonSyntaxException){
            if (e.getCause() instanceof IllegalStateException) {
                IllegalStateException ise = (IllegalStateException) e.getCause();
                if (ise.getMessage() != null && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
                    isThrow = false;
                }
            }
        }else if(e instanceof ApiException){
            if(StringUtils.equalsIgnoreCase("Not Found", e.getMessage())){
                isThrow = false;
            }
            // ApiException 오류가 Tag mismatch 인 경우 재시도
            else if(StringUtils.equalsIgnoreCase("javax.net.ssl.SSLException: Tag mismatch!", e.getMessage())){
                if (isThrow){
                    throw e;
                }
            }
            else{
                ce = new CocktailException(String.format("%s, %s", e.getMessage(), ((ApiException) e).getResponseBody()), e, ExceptionType.RegistryApiFail);
            }
        }

        log.warn(ce.getMessage());

        if(isThrow){
            throw ce;
        }
    }

}
