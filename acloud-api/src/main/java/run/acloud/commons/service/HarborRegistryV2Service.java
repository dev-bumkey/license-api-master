package run.acloud.commons.service;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.vo.AccountRegistryVO;
import run.acloud.api.configuration.vo.ImageRepositoryTagVO;
import run.acloud.api.configuration.vo.RegistryProjectVO;
import run.acloud.commons.client.HarborApiClient;
import run.acloud.commons.client.harbor.v2.ApiException;
import run.acloud.commons.client.harbor.v2.ApiResponse;
import run.acloud.commons.client.harbor.v2.HarborApiClientV2;
import run.acloud.commons.client.harbor.v2.api.*;
import run.acloud.commons.client.harbor.v2.model.*;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.HarborRegistryProjectMemberRole;
import run.acloud.commons.vo.*;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
public class HarborRegistryV2Service implements IHarborRegistryService {

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

    private HarborApiClientV2 harborApiClientV2;

    @Override
    public ImageRepositoryTagVO getImageTagInfo(String projectName , String imageName , String tag) throws Exception {

        ArtifactApi artifactApi = this.getArtifactApi();
        Artifact artifact = this.getArtifact(artifactApi, projectName, imageName, tag, null, null, true, null, null, null, null, null);

        if (artifact != null) {
            ImageRepositoryTagVO tagImage = new ImageRepositoryTagVO();
            tagImage.setImageName(String.format(HARBOR_IMAGE, projectName, imageName));

            List<Tag> tags = Optional.ofNullable(artifact.getTags()).orElseGet(Lists::newArrayList);

            // data set
            tagImage.setTagName(tags.get(0).getName());
            tagImage.setDigest(artifact.getDigest());
            tagImage.setArchitecture((String)artifact.getExtraAttrs().get("architecture"));
            tagImage.setDockerVersion(null); // 2.5.x 버전에서 값이 조회되지 않아 사용안함.
            tagImage.setCreated((String)artifact.getExtraAttrs().get("created"));
            tagImage.setSize(artifact.getSize());

            return tagImage;
        }

        return null;

    }

    @Override
    public RegistryProjectVO addProject(HarborProjectReqVO projectReq) throws Exception {
        ProjectApi projectApi = this.getProjectApi();
        ProjectReq req = new ProjectReq();
        ProjectMetadata pMeta = new ProjectMetadata();
        pMeta.setPublic(projectReq.isPublic().toString());
        req.metadata(pMeta).setProjectName(projectReq.getProjectName());
        Project addProject = this.addProject(projectApi, req);

        return this.setProject(addProject);
    }


    @Override
    public void deleteProject(Integer projectId) throws Exception {
        if (projectId == null) {
            return;
        }
        ProjectApi projectApi = this.getProjectApi();
        this.deleteProject(projectApi, projectId);
    }

    @Override
    public List<RegistryProjectVO> getProjectList(String projectName) throws Exception {

        ProjectApi projectApi = this.getProjectApi();
        List<Project> projects = this.getProjects(projectApi, null, null, null, null, projectName, null, null, true);

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

        ProjectApi projectApi = this.getProjectApi();
        Project p = this.getProject(projectApi, projectName);

        return this.setProject(p);
    }

    @Override
    public RegistryProjectVO getProject(Integer projectId) throws Exception {

        ProjectApi projectApi = this.getProjectApi();
        Project p = this.getProject(projectApi, projectId);

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

        UserCreationReq reqUser = new UserCreationReq();
        reqUser.setUsername(userReq.getUsername());
        if (userReq.getUsername().matches(HarborRegistryV2Service.MAIL_REGEX)) {
            reqUser.setEmail(userReq.getUsername());
        } else {
            reqUser.setEmail(String.format("%s%s", userReq.getUsername(), HarborRegistryV2Service.DEFAULT_EMAIL));
        }
        reqUser.setPassword(userReq.getPassword());
        reqUser.setRealname(userReq.getUsername());

        UserApi userApi = this.getUserApi();
        UserResp user = this.addUser(userApi, reqUser);

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
        UserApi userApi = this.getUserApi();
        this.deleteUser(userApi, userId);
    }

    @Override
    public void deleteUser(String username) throws Exception {
        UserApi userApi = this.getUserApi();
        this.deleteUser(userApi, username);
    }

    @Override
    public HarborUserRespVO getUser(Integer userId) throws Exception {
        UserApi userApi = this.getUserApi();
        UserResp user = this.getUser(userApi, userId);

        return this.setUser(user);
    }

    @Override
    public HarborUserRespVO getUser(String username) throws Exception {
        UserApi userApi = this.getUserApi();
        UserResp user = this.getUser(userApi, username);

        return this.setUser(user);
    }

    private HarborUserRespVO setUser(UserResp user) {
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
        UserApi userApi = this.getUserApi();
        List<Permission> curr = this.getCurrentUserPermissions(userApi, scope, relative);

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

        UserApi userApi = this.getUserApi();
        int status =  this.getCurrentUserPermissionsStatus(userApi, scope, relative);

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

        MemberApi memberApi = this.getMemberApi();
        ProjectMemberEntity projectMemberEntity = this.getProjectMember(memberApi, projectId, userName);

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

        MemberApi memberApi = this.getMemberApi();
        for (HarborProjectMemberVO user : users) {
            this.addMemberToProjectsCommon(memberApi, projects, user, ignoreException);
        }
    }

    @Override
    public void addMemberToProjects(List<Integer> projects, HarborProjectMemberVO user, Boolean ignoreException) throws Exception {
        if (CollectionUtils.isEmpty(projects)) {
            return;
        }

        MemberApi memberApi = this.getMemberApi();
        this.addMemberToProjectsCommon(memberApi, projects, user, ignoreException);
    }

    @Override
    public void addMemberToProject(Integer projectId, HarborProjectMemberVO user, Boolean ignoreException) throws Exception {
        if (projectId == null) {
            return;
        }

        MemberApi memberApi = this.getMemberApi();
        this.addMemberToProjectsCommon(memberApi, Collections.singletonList(projectId), user, ignoreException);
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

    private void addMemberToProjectsCommon(MemberApi memberApi, List<Integer> projects, HarborProjectMemberVO user, Boolean ignoreException) throws Exception {
        if (StringUtils.isBlank(user.getEntityName())) {
            log.warn("user id is empty");
            return;
        }

        if (memberApi == null) {
            memberApi = this.getMemberApiWithAdminAuth();
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
                this.addProjectMember(memberApi, projectId, projectMember);
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
        MemberApi memberApi = this.getMemberApi();
        this.updateProjectMember(memberApi, projectId, mid, new RoleRequest().roleId(roleId));
    }

    @Override
    public void deleteMemberOfProject(Integer projectId, Integer mid) throws Exception {
        MemberApi memberApi = this.getMemberApi();
        this.deleteProjectMember(memberApi, projectId, mid);
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
            ArtifactApi artifactApi = this.getArtifactApi();
            List<Artifact> artifacts = this.getArtifacts(artifactApi, projectName, imageName, null, 1L, 5L, false, false, false, false, false, false);

            if(CollectionUtils.isNotEmpty(artifacts)){
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
    public boolean deleteImagesFromProjects(String projectName, String imageName){
        boolean registryImageCheck = false;

        try {
            RepositoryApi repositoryApi = this.getRepositoryApi();
            this.deleteRepository(repositoryApi, projectName, imageName);
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
    public boolean deleteImagesFromProjects(String projectName, String imageName, String tag){
        boolean registryImageCheck = false;

        try {
            ArtifactApi artifactApi = this.getArtifactApi();
            this.deleteArtifactTag(artifactApi, projectName, imageName, tag);
            registryImageCheck = true;
        }catch (Exception e){
            log.warn(e.getMessage(), e);
        }

        return registryImageCheck;
    }

    @Override
    public int registriesPing(HarborRegistryPingVO registryPingReq) throws Exception {
        RegistryApi registryApi = null;
        if (this.getClient() != null) {
            registryApi = this.getRegistryApi(this.getClient());
        }
        RegistryPing registryPing = new RegistryPing();
        if (registryPingReq != null) {
            registryPing.setAccessKey(registryPingReq.getAccessKey());
            registryPing.setAccessSecret(registryPingReq.getAccessSecret());

            registryPing.setType(registryPingReq.getType());
            registryPing.setUrl(registryPingReq.getUrl());
            registryPing.setInsecure(registryPingReq.isInsecure());
        }
        return this.registriesPing(registryApi, registryPing);
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
            GeneralInfo generalInfo = this.getSysteminfo(this.getSysteminfoApiByAccountSeq(accountSeq));
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
            GeneralInfo generalInfo = this.getSysteminfo(this.getSysteminfoApiWithRegistry(accountRegistry));
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
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * Harbor client 2.x
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */
    public ApiVersionType getApiVersion() {
        return ApiVersionType.V2;
    }
    public Object getClient() {
        return this.harborApiClientV2;
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
    public HarborApiClientV2 getClientWithAuth(String registryUrl, String decryptedUsername, String decryptedPassword, String privateCertificateUseYn, String privateCertificate) {
        this.harborApiClientV2 = (HarborApiClientV2)harborApiClient.create(ApiVersionType.V2, registryUrl, decryptedUsername, decryptedPassword, privateCertificateUseYn, privateCertificate);
        return this.harborApiClientV2;
    }



    /**
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * UserApi
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    /**
     * UserApi 생성
     *
     * @param apiClient HarborApiClientV2
     * @return
     * @throws Exception
     */
    public UserApi getUserApi(HarborApiClientV2 apiClient) throws Exception {
        return new UserApi(apiClient);
    }

    /**
     * UserApi 생성
     *
     * @param apiClient Object
     * @return
     * @throws Exception
     */
    public UserApi getUserApi(Object apiClient) throws Exception {
        return this.getUserApi((HarborApiClientV2)apiClient);
    }

    /**
     * UserApi 생성
     *
     * @return
     * @throws Exception
     */
    public UserApi getUserApi() throws Exception {
        UserApi userApi = null;
        if (this.getClient() != null) {
            userApi = this.getUserApi(this.getClient());
        }
        return userApi;
    }

    /**
     * UserApi 생성 with admin
     *
     * @return
     * @throws Exception
     */
    public UserApi getUserApiWithAdminAuth() throws Exception {
//        return new ProductsApi(this.getClientWithEncryptedAuth(registry.getId(), registry.getPassword()));
        return new UserApi(this.getClientWithAuth(registry.getUrl(), registry.getId(), registry.getPassword(), registry.getPrivateCertificateUseYn(), registry.getPrivateCertificate()));
    }

    /**
     * UserApi 생성 with 플랫폼 레지스트리
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public UserApi getUserApiByAccountSeq(Integer accountSeq) throws Exception {
        return new UserApi(this.getClientWithAuth(registry.getUrl(accountSeq), registry.getId(accountSeq), registry.getPassword(accountSeq), registry.getPrivateCertificateUseYn(accountSeq), registry.getPrivateCertificate(accountSeq)));
    }

    /**
     * UserApi 생성 with 플랫폼 레지스트리 정보
     *
     * @param accountRegistry
     * @return
     * @throws Exception
     */
    public UserApi getUserApiWithRegistry(AccountRegistryVO accountRegistry) throws Exception {
        return new UserApi(this.getClientWithAuth(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), accountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificate()));
    }

    /**
     * 사용자 생성
     *
     * @param userApi
     * @param userReq
     * @return
     * @throws Exception
     */
    public UserResp addUser(UserApi userApi, UserCreationReq userReq) throws Exception {
        try {
            if (userApi == null) {
                userApi = this.getUserApiWithAdminAuth();
            }

            if (userReq.getUsername().matches(HarborRegistryV2Service.MAIL_REGEX)) {
                userReq.setEmail(userReq.getUsername());
            } else {
                userReq.setEmail(String.format("%s%s", userReq.getUsername(), HarborRegistryV2Service.DEFAULT_EMAIL));
            }

            ApiResponse<Void> response = userApi.createUserWithHttpInfo(userReq, null);

            this.getResponse(response, "Fail to create new user", ExceptionType.RegistryAddUserFail);

            return this.getUser(userApi, userReq.getUsername());
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * 사용자 목록
     *
     * @param userApi
     * @param username
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     */
    public List<UserResp> getUsers(UserApi userApi, String username, Long page, Long pageSize) throws Exception {
        try {
            if (userApi == null) {
                userApi = this.getUserApiWithAdminAuth();
            }

            if (StringUtils.isNotBlank(username)) {
                username = String.format("username=%s", username);
            }

            ApiResponse<List<UserResp>> response = userApi.listUsersWithHttpInfo(null, username, null, page, pageSize);

            return this.getResponse(response, "Fail to get users", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * 사용자 상세 by username
     *
     * @param userApi
     * @param username
     * @return
     * @throws Exception
     */
    public UserResp getUser(UserApi userApi, String username) throws Exception {
        try {
            if (userApi == null) {
                userApi = this.getUserApiWithAdminAuth();
            }

            List<UserResp> users = this.getUsers(userApi, username, null, null);
            if (CollectionUtils.isNotEmpty(users)) {
                Optional<UserResp> userOptional = users.stream().filter(u -> (StringUtils.equals(u.getUsername(), username))).findFirst();
                if (userOptional.isPresent()) {
                    return userOptional.get();
                }
            }
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * 사용자 상세 by userId
     *
     * @param userApi
     * @param userId
     * @return
     * @throws Exception
     */
    public UserResp getUser(UserApi userApi, Integer userId) throws Exception {
        try {
            if (userApi == null) {
                userApi = this.getUserApiWithAdminAuth();
            }

            ApiResponse<UserResp> response = userApi.getUserWithHttpInfo(userId, null);

            return this.getResponse(response, "Fail to get user", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * 사용자 삭제 by userId
     *
     * @param userApi
     * @param userId
     * @throws Exception
     */
    public void deleteUser(UserApi userApi, Integer userId) throws Exception {
        try {
            if (userApi == null) {
                userApi = this.getUserApiWithAdminAuth();
            }

            ApiResponse<Void> response = userApi.deleteUserWithHttpInfo(userId, null);

            this.getResponse(response, "Fail to delete user from registry", ExceptionType.RegistryDeleteUserToProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * 사용자 삭제 by username
     *
     * @param userApi
     * @param username
     * @throws Exception
     */
    public void deleteUser(UserApi userApi, String username) throws Exception {
        try {
            if (userApi == null) {
                userApi = this.getUserApiWithAdminAuth();
            }

            UserResp user = this.getUser(userApi, username);

            if (user == null) {
                log.warn("User not found from registry: {}", username);
                return;
            }

            this.deleteUser(userApi, user.getUserId());

        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }


    /**
     * 현재 사용자 권한 조회
     *
     * @param userApi
     * @param scope
     * @param relative
     * @return
     * @throws Exception
     */
    public List<Permission> getCurrentUserPermissions(UserApi userApi, String scope, Boolean relative) throws Exception {
        try {
            if (userApi == null) {
                userApi = this.getUserApiWithAdminAuth();
            }

            ApiResponse<List<Permission>> response = userApi.getCurrentUserPermissionsWithHttpInfo(null, scope, relative);

            return this.getResponse(response, "Fail to Current User Permissions", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    public int getCurrentUserPermissionsStatus(UserApi userApi, String scope, Boolean relative) throws Exception {
        try {
            if (userApi == null) {
                userApi = this.getUserApiWithAdminAuth();
            }

            ApiResponse<List<Permission>> response = userApi.getCurrentUserPermissionsWithHttpInfo(null, scope, relative);

            return response.getStatusCode();
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return 0;
    }

    /**
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * ProjectApi
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    /**
     * ProjectApi 생성
     *
     * @param apiClient HarborApiClientV2
     * @return
     * @throws Exception
     */
    public ProjectApi getProjectApi(HarborApiClientV2 apiClient) throws Exception {
        return new ProjectApi(apiClient);
    }

    /**
     * ProjectApi 생성
     *
     * @param apiClient Object
     * @return
     * @throws Exception
     */
    public ProjectApi getProjectApi(Object apiClient) throws Exception {
        return this.getProjectApi((HarborApiClientV2)apiClient);
    }

    /**
     * ProjectApi 생성
     *
     * @return
     * @throws Exception
     */
    public ProjectApi getProjectApi() throws Exception {
        ProjectApi projectApi = null;
        if (this.getClient() != null) {
            projectApi = this.getProjectApi(this.getClient());
        }
        return projectApi;
    }

    /**
     * ProjectApi 생성 with admin
     *
     * @return
     * @throws Exception
     */
    public ProjectApi getProjectApiWithAdminAuth() throws Exception {
//        return new ProjectApi(this.getClientWithEncryptedAuth(registry.getId(), registry.getPassword()));
        return new ProjectApi(this.getClientWithAuth(registry.getUrl(), registry.getId(), registry.getPassword(), registry.getPrivateCertificateUseYn(), registry.getPrivateCertificate()));
    }

    /**
     * ProjectApi 생성 with 플랫폼 레지스트리
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public ProjectApi getProjectApiByAccountSeq(Integer accountSeq) throws Exception {
        return new ProjectApi(this.getClientWithAuth(registry.getUrl(accountSeq), registry.getId(accountSeq), registry.getPassword(accountSeq), registry.getPrivateCertificateUseYn(accountSeq), registry.getPrivateCertificate(accountSeq)));
    }

    /**
     * ProjectApi 생성 with 플랫폼 레지스트리 정보
     *
     * @param accountRegistry
     * @return
     * @throws Exception
     */
    public ProjectApi getProjectApiWithRegistry(AccountRegistryVO accountRegistry) throws Exception {
        return new ProjectApi(this.getClientWithAuth(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), accountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificate()));
    }

    /**
     * project 생성
     *
     * @param projectApi
     * @param projectReq
     * @return
     * @throws Exception
     */
    public Project addProject(ProjectApi projectApi, ProjectReq projectReq) throws Exception {
        try {
            if (projectApi == null) {
                projectApi = this.getProjectApiWithAdminAuth();
            }

            ApiResponse<Void> response = projectApi.createProjectWithHttpInfo(projectReq, null, null);

            this.getResponse(response, "Fail to create new project", ExceptionType.RegistryAddProjectFail);

            return this.getProject(projectApi, projectReq.getProjectName());
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * project 삭제 by projectId
     *
     * @param projectApi
     * @param projectId
     * @throws Exception
     */
    public void deleteProject(ProjectApi projectApi, Integer projectId) throws Exception {
        try {
            if (projectApi == null) {
                projectApi = this.getProjectApiWithAdminAuth();
            }

            ApiResponse<Void> response = projectApi.deleteProjectWithHttpInfo(Optional.ofNullable(projectId).map(pId -> (pId.toString())).orElseGet(() ->null), null, null);

            this.getResponse(response, "Fail to delete project from registry", ExceptionType.RegistryDeleteProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * project 삭제 by projectName
     *
     * @param projectApi
     * @param projectName
     * @throws Exception
     */
    public void deleteProject(ProjectApi projectApi, String projectName) throws Exception {
        try {
            if (projectApi == null) {
                projectApi = this.getProjectApiWithAdminAuth();
            }

            ApiResponse<Void> response = projectApi.deleteProjectWithHttpInfo(projectName, null, null);

            this.getResponse(response, "Fail to delete project from registry", ExceptionType.RegistryDeleteProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * project 목록
     *
     * @param projectApi
     * @param q
     * @param page
     * @param pageSize
     * @param sort
     * @param name
     * @param _public
     * @param owner
     * @param withDetail
     * @return
     * @throws Exception
     */
    public List<Project> getProjects(ProjectApi projectApi, String q, Long page, Long pageSize, String sort, String name, Boolean _public, String owner, Boolean withDetail) throws Exception {
        try {
            if (projectApi == null) {
                projectApi = this.getProjectApiWithAdminAuth();
            }

            ApiResponse<List<Project>> response = projectApi.listProjectsWithHttpInfo(null, q, page, pageSize, sort, name, _public, owner, withDetail);

            return this.getResponse(response, "fail to get project list", ExceptionType.RegistryProjectListingFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * project 상세 by projectName
     *
     * @param projectApi
     * @param projectName
     * @return
     * @throws Exception
     */
    public Project getProject(ProjectApi projectApi, String projectName) throws Exception {
        try {
            if (projectApi == null) {
                projectApi = this.getProjectApiWithAdminAuth();
            }

            ApiResponse<Project> response = projectApi.getProjectWithHttpInfo(projectName, null, null);

            return this.getResponse(response, "fail to get project", ExceptionType.RegistryProjectListingFail);
        } catch (Exception e) {
            this.exceptionHandleForForbidden(e);
        }

        return null;
    }

    /**
     * project 상세 by projectId
     *
     * @param projectApi
     * @param projectId
     * @return
     * @throws Exception
     */
    public Project getProject(ProjectApi projectApi, Integer projectId) throws Exception {
        try {
            if (projectApi == null) {
                projectApi = this.getProjectApiWithAdminAuth();
            }

            ApiResponse<Project> response = projectApi.getProjectWithHttpInfo(Optional.ofNullable(projectId).map(pId -> (pId.toString())).orElseGet(() ->null), null, null);

            return this.getResponse(response, "fail to get project", ExceptionType.RegistryProjectListingFail);
        } catch (Exception e) {
            this.exceptionHandleForForbidden(e);
        }

        return null;
    }




    /**
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * MemberApi
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */


    /**
     * MemberApi 생성
     *
     * @param apiClient HarborApiClientV2
     * @return
     * @throws Exception
     */
    public MemberApi getMemberApi(HarborApiClientV2 apiClient) throws Exception {
        return new MemberApi(apiClient);
    }

    /**
     * MemberApi 생성
     *
     * @param apiClient Object
     * @return
     * @throws Exception
     */
    public MemberApi getMemberApi(Object apiClient) throws Exception {
        return this.getMemberApi((HarborApiClientV2)apiClient);
    }

    /**
     * MemberApi 생성
     *
     * @return
     * @throws Exception
     */
    public MemberApi getMemberApi() throws Exception {
        MemberApi memberApi = null;
        if (this.getClient() != null) {
            memberApi = this.getMemberApi(this.getClient());
        }
        return memberApi;
    }

    /**
     * MemberApi 생성 with admin
     *
     * @return
     * @throws Exception
     */
    public MemberApi getMemberApiWithAdminAuth() throws Exception {
//        return new MemberApi(this.getClientWithEncryptedAuth(registry.getId(), registry.getPassword()));
        return new MemberApi(this.getClientWithAuth(registry.getUrl(), registry.getId(), registry.getPassword(), registry.getPrivateCertificateUseYn(), registry.getPrivateCertificate()));
    }

    /**
     * MemberApi 생성 with 플랫폼 레지스트리
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public MemberApi getMemberApiByAccountSeq(Integer accountSeq) throws Exception {
        return new MemberApi(this.getClientWithAuth(registry.getUrl(accountSeq), registry.getId(accountSeq), registry.getPassword(accountSeq), registry.getPrivateCertificateUseYn(accountSeq), registry.getPrivateCertificate(accountSeq)));
    }

    /**
     * MemberApi 생성 with 플랫폼 레지스트리 정보
     *
     * @param accountRegistry
     * @return
     * @throws Exception
     */
    public MemberApi getMemberApiWithRegistry(AccountRegistryVO accountRegistry) throws Exception {
        return new MemberApi(this.getClientWithAuth(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), accountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificate()));
    }

    /**
     * project member 추가 by projectName
     *
     * @param memberApi
     * @param projectName
     * @param projectMember
     * @throws Exception
     */
    public void addProjectMember(MemberApi memberApi, String projectName, ProjectMember projectMember) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<Void> response = memberApi.createProjectMemberWithHttpInfo(projectName, null, null, projectMember);

            this.getResponse(response, "Fail to add member to project", ExceptionType.RegistryAddUserToProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * project member 추가 by projectId
     *
     * @param memberApi
     * @param projectId
     * @param projectMember
     * @throws Exception
     */
    public void addProjectMember(MemberApi memberApi, Integer projectId, ProjectMember projectMember) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<Void> response = memberApi.createProjectMemberWithHttpInfo(
                    Optional.ofNullable(projectId).map(pId -> (pId.toString())).orElseGet(() ->null)
                    , null
                    , null
                    , projectMember);

            this.getResponse(response, "Fail to add member to project", ExceptionType.RegistryAddUserToProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * project member 수정 by projectName
     *
     * @param memberApi
     * @param projectName
     * @param mid
     * @param role
     * @throws Exception
     */
    public void updateProjectMember(MemberApi memberApi, String projectName, Integer mid, RoleRequest role) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<Void> response = memberApi.updateProjectMemberWithHttpInfo(
                    projectName, Optional.ofNullable(mid).map(Integer::longValue).orElseGet(() ->null)
                    , null
                    , null
                    , role);

            this.getResponse(response, "Fail to update member role in project", ExceptionType.RegistryAddUserToProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     *  project member 수정 by projectId
     *
     * @param memberApi
     * @param projectId
     * @param mid
     * @param role
     * @throws Exception
     */
    public void updateProjectMember(MemberApi memberApi, Integer projectId, Integer mid, RoleRequest role) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<Void> response = memberApi.updateProjectMemberWithHttpInfo(
                    Optional.ofNullable(projectId).map(pId -> (pId.toString())).orElseGet(() ->null)
                    , Optional.ofNullable(mid).map(Integer::longValue).orElseGet(() ->null)
                    , null
                    , null
                    , role);

            this.getResponse(response, "Fail to update member role in project", ExceptionType.RegistryAddUserToProjectFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     *  project member 삭제 by projectId
     *
     * @param memberApi
     * @param projectId
     * @param mid
     * @throws Exception
     */
    public void deleteProjectMember(MemberApi memberApi, Integer projectId, Integer mid) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<Void> response = memberApi.deleteProjectMemberWithHttpInfo(
                    Optional.ofNullable(projectId).map(pId -> (pId.toString())).orElseGet(() ->null)
                    , Optional.ofNullable(mid).map(Integer::longValue).orElseGet(() ->null)
                    , null
                    , null);

            this.getResponse(response, "Fail to delete member in project", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * project member 목록 by projectName, searchMemberName
     *
     * @param memberApi
     * @param projectName
     * @param searchMemberName
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     */
    public List<ProjectMemberEntity> getProjectMembers(MemberApi memberApi, String projectName, String searchMemberName, Long page, Long pageSize) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<List<ProjectMemberEntity>> response = memberApi.listProjectMembersWithHttpInfo(
                    projectName
                    , null
                    , null
                    , page
                    , pageSize
                    , searchMemberName);

            return this.getResponse(response, "Fail to get user in project", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * project member 목록 by projectId, searchMemberName
     *
     * @param memberApi
     * @param projectId
     * @param searchMemberName
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     */
    public List<ProjectMemberEntity> getProjectMembers(MemberApi memberApi, Integer projectId, String searchMemberName, Long page, Long pageSize) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<List<ProjectMemberEntity>> response = memberApi.listProjectMembersWithHttpInfo(
                    Optional.ofNullable(projectId).map(pId -> (pId.toString())).orElseGet(() ->null)
                    , null
                    , null
                    , page
                    , pageSize
                    , searchMemberName);

            return this.getResponse(response, "Fail to get user in project", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return new ArrayList<>();
    }

    /**
     * project member 상세 by projectName, searchMemberName
     *
     * @param memberApi
     * @param projectName
     * @param searchMemberName
     * @return
     * @throws Exception
     */
    public ProjectMemberEntity getProjectMember(MemberApi memberApi, String projectName, String searchMemberName) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            List<ProjectMemberEntity> projectMembers = this.getProjectMembers(memberApi, projectName, searchMemberName, null, null);
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

    /**
     * project member 상세 by projectId, searchMemberName
     *
     * @param memberApi
     * @param projectId
     * @param searchMemberName
     * @return
     * @throws Exception
     */
    public ProjectMemberEntity getProjectMember(MemberApi memberApi, Integer projectId, String searchMemberName) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            List<ProjectMemberEntity> projectMembers = this.getProjectMembers(memberApi, projectId, searchMemberName, null, null);
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

    /**
     * project member 상세 by projectName, userId
     *
     * @param memberApi
     * @param projectName
     * @param userId
     * @return
     * @throws Exception
     */
    public ProjectMemberEntity getProjectMember(MemberApi memberApi, String projectName, Integer userId) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<ProjectMemberEntity> response = memberApi.getProjectMemberWithHttpInfo(
                    projectName
                    , Optional.ofNullable(userId).map(Integer::longValue).orElseGet(() ->null)
                    , null
                    , null);

            return this.getResponse(response, "Fail to get user in project", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * project member 상세 by projectId, userId
     *
     * @param memberApi
     * @param projectId
     * @param userId
     * @return
     * @throws Exception
     */
    public ProjectMemberEntity getProjectMember(MemberApi memberApi, Integer projectId, Integer userId) throws Exception {
        try {
            if (memberApi == null) {
                memberApi = this.getMemberApiWithAdminAuth();
            }

            ApiResponse<ProjectMemberEntity> response = memberApi.getProjectMemberWithHttpInfo(
                    Optional.ofNullable(projectId).map(pId -> (pId.toString())).orElseGet(() ->null)
                    , Optional.ofNullable(userId).map(Integer::longValue).orElseGet(() ->null)
                    , null
                    , null);

            return this.getResponse(response, "Fail to get user in project", ExceptionType.RegistryUserNotFound);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }




    /**
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * RepositoryApi
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    /**
     * RepositoryApi 생성
     *
     * @param apiClient
     * @return
     * @throws Exception
     */
    public RepositoryApi getRepositoryApi(HarborApiClientV2 apiClient) throws Exception {
        return new RepositoryApi(apiClient);
    }

    /**
     * RepositoryApi 생성
     *
     * @param apiClient
     * @return
     * @throws Exception
     */
    public RepositoryApi getRepositoryApi(Object apiClient) throws Exception {
        return this.getRepositoryApi((HarborApiClientV2)apiClient);
    }

    /**
     * RepositoryApi 생성
     *
     * @return
     * @throws Exception
     */
    public RepositoryApi getRepositoryApi() throws Exception {
        RepositoryApi repositoryApi = null;
        if (this.getClient() != null) {
            repositoryApi = this.getRepositoryApi(this.getClient());
        }
        return repositoryApi;
    }

    /**
     * RepositoryApi 생성 with admin
     *
     * @return
     * @throws Exception
     */
    public RepositoryApi getRepositoryApiWithAdminAuth() throws Exception {
//        return new RepositoryApi(this.getClientWithEncryptedAuth(registry.getId(), registry.getPassword()));
        return new RepositoryApi(this.getClientWithAuth(registry.getUrl(), registry.getId(), registry.getPassword(), registry.getPrivateCertificateUseYn(), registry.getPrivateCertificate()));
    }

    /**
     * RepositoryApi 생성 with 플랫폼 레지스트리
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public RepositoryApi getRepositoryApiByAccountSeq(Integer accountSeq) throws Exception {
        return new RepositoryApi(this.getClientWithAuth(registry.getUrl(accountSeq), registry.getId(accountSeq), registry.getPassword(accountSeq), registry.getPrivateCertificateUseYn(accountSeq), registry.getPrivateCertificate(accountSeq)));
    }

    /**
     * RepositoryApi 생성 with 플랫폼 레지스트리 정보
     *
     * @param accountRegistry
     * @return
     * @throws Exception
     */
    public RepositoryApi getRepositoryApiWithRegistry(AccountRegistryVO accountRegistry) throws Exception {
        return new RepositoryApi(this.getClientWithAuth(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), accountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificate()));
    }


    /**
     * project > image 삭제 (all tag)
     *
     * @param repositoryApi
     * @param projectName
     * @param repositoryName
     * @throws Exception
     */
    public void deleteRepository(RepositoryApi repositoryApi, String projectName, String repositoryName) throws Exception {
        try {
            if (repositoryApi == null) {
                repositoryApi = this.getRepositoryApiWithAdminAuth();
            }

            if (StringUtils.isNotBlank(repositoryName)) {
                repositoryName = URLEncoder.encode(repositoryName, StandardCharsets.UTF_8.toString());
            }

            ApiResponse<Void> response = repositoryApi.deleteRepositoryWithHttpInfo(projectName, repositoryName, null);

            this.getResponse(response, "Fail to delete image(repository).", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }




    /**
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * ArtifactApi
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    /**
     * ArtifactApi 생성
     *
     * @param apiClient
     * @return
     * @throws Exception
     */
    public ArtifactApi getArtifactApi(HarborApiClientV2 apiClient) throws Exception {
        return new ArtifactApi(apiClient);
    }

    /**
     * ArtifactApi 생성
     *
     * @param apiClient
     * @return
     * @throws Exception
     */
    public ArtifactApi getArtifactApi(Object apiClient) throws Exception {
        return this.getArtifactApi((HarborApiClientV2)apiClient);
    }

    /**
     * ArtifactApi 생성
     *
     * @return
     * @throws Exception
     */
    public ArtifactApi getArtifactApi() throws Exception {
        ArtifactApi artifactApi = null;
        if (this.getClient() != null) {
            artifactApi = this.getArtifactApi(this.getClient());
        }
        return artifactApi;
    }

    /**
     * ArtifactApi 생성 with admin
     *
     * @return
     * @throws Exception
     */
    public ArtifactApi getArtifactApiWithAdminAuth() throws Exception {
//        return new ArtifactApi(this.getClientWithEncryptedAuth(registry.getId(), registry.getPassword()));
        return new ArtifactApi(this.getClientWithAuth(registry.getUrl(), registry.getId(), registry.getPassword(), registry.getPrivateCertificateUseYn(), registry.getPrivateCertificate()));
    }

    /**
     * ArtifactApi 생성 with 플랫폼 레지스트리
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public ArtifactApi getArtifactApiByAccountSeq(Integer accountSeq) throws Exception {
        return new ArtifactApi(this.getClientWithAuth(registry.getUrl(accountSeq), registry.getId(accountSeq), registry.getPassword(accountSeq), registry.getPrivateCertificateUseYn(accountSeq), registry.getPrivateCertificate(accountSeq)));
    }

    /**
     * ArtifactApi 생성 with 플랫폼 레지스트리 정보
     *
     * @param accountRegistry
     * @return
     * @throws Exception
     */
    public ArtifactApi getArtifactApiWithRegistry(AccountRegistryVO accountRegistry) throws Exception {
        return new ArtifactApi(this.getClientWithAuth(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), accountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificate()));
    }


    /**
     * project > image tag 삭제
     *
     * @param artifactApi
     * @param projectName
     * @param repositoryName
     * @param reference The reference of the artifact, can be digest or tag (required)
     * @throws Exception
     */
    public void deleteArtifactTag(ArtifactApi artifactApi, String projectName, String repositoryName, String reference) throws Exception {
        try {
            if (artifactApi == null) {
                artifactApi = this.getArtifactApiWithAdminAuth();
            }

            if (StringUtils.isNotBlank(repositoryName)) {
                repositoryName = URLEncoder.encode(repositoryName, StandardCharsets.UTF_8.toString());
            }

            ApiResponse<Void> response = artifactApi.deleteArtifactWithHttpInfo(projectName, repositoryName, reference, null);

            this.getResponse(response, "Fail to delete image tag", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }
    }

    /**
     * 이미지 tag 목록 by projectName
     *
     * @param artifactApi
     * @param projectName
     * @param repositoryName
     * @param q Query string to query resources. Supported query patterns are \&quot;exact match(k&#x3D;v)\&quot;, \&quot;fuzzy match(k&#x3D;~v)\&quot;, \&quot;range(k&#x3D;[min~max])\&quot;, \&quot;list with union releationship(k&#x3D;{v1 v2 v3})\&quot; and \&quot;list with intersetion relationship(k&#x3D;(v1 v2 v3))\&quot;. The value of range and list can be string(enclosed by \&quot; or &#39;), integer or time(in format \&quot;2020-04-09 02:36:00\&quot;). All of these query patterns should be put in the query string \&quot;q&#x3D;xxx\&quot; and splitted by \&quot;,\&quot;. e.g. q&#x3D;k1&#x3D;v1,k2&#x3D;~v2,k3&#x3D;[min~max] (optional)
     * @param page
     * @param pageSize
     * @param withTag
     * @param withLabel
     * @param withScanOverview
     * @param withSignature
     * @param withImmutableStatus
     * @param withAccessory
     * @return
     * @throws Exception
     */
    public List<Artifact> getArtifacts(ArtifactApi artifactApi, String projectName, String repositoryName, String q, Long page, Long pageSize, Boolean withTag, Boolean withLabel, Boolean withScanOverview, Boolean withSignature, Boolean withImmutableStatus, Boolean withAccessory) throws Exception {
        try {
            if (artifactApi == null) {
                artifactApi = this.getArtifactApiWithAdminAuth();
            }

            if (StringUtils.isNotBlank(repositoryName)) {
                repositoryName = URLEncoder.encode(repositoryName, StandardCharsets.UTF_8.toString());
            }

            ApiResponse<List<Artifact>> response = artifactApi.listArtifactsWithHttpInfo(projectName, repositoryName, null, q, null, page, pageSize, null, withTag, withLabel, withScanOverview, withSignature, withImmutableStatus, withAccessory);

            return this.getResponse(response, "fail to get artifact list", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }

    /**
     * 이미지 상세 by projectName
     *
     * @param artifactApi
     * @param projectName
     * @param repositoryName
     * @param reference The reference of the artifact, can be digest or tag (required)
     * @param page
     * @param pageSize
     * @param withTag
     * @param withLabel
     * @param withScanOverview
     * @param withAccessory
     * @param withSignature
     * @param withImmutableStatus
     * @return
     * @throws Exception
     */
    public Artifact getArtifact(ArtifactApi artifactApi, String projectName, String repositoryName, String reference, Long page, Long pageSize, Boolean withTag, Boolean withLabel, Boolean withScanOverview, Boolean withAccessory, Boolean withSignature, Boolean withImmutableStatus) throws Exception {
        try {
            if (artifactApi == null) {
                artifactApi = this.getArtifactApiWithAdminAuth();
            }

            if (StringUtils.isNotBlank(repositoryName)) {
                repositoryName = URLEncoder.encode(repositoryName, StandardCharsets.UTF_8.toString());
            }

            ApiResponse<Artifact> response = artifactApi.getArtifactWithHttpInfo(projectName, repositoryName, reference, null, page, pageSize, null, withTag, withLabel, withScanOverview, withAccessory, withSignature, withImmutableStatus);

            return this.getResponse(response, "fail to get artifact", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e);
        }

        return null;
    }



    /**
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * RegistryApi
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    /**
     * RegistryApi 생성
     *
     * @param apiClient
     * @return
     * @throws Exception
     */
    public RegistryApi getRegistryApi(HarborApiClientV2 apiClient) throws Exception {
        return new RegistryApi(apiClient);
    }

    /**
     * RegistryApi 생성
     *
     * @param apiClient
     * @return
     * @throws Exception
     */
    public RegistryApi getRegistryApi(Object apiClient) throws Exception {
        return this.getRegistryApi((HarborApiClientV2)apiClient);
    }

    /**
     * RegistryApi 생성
     *
     * @return
     * @throws Exception
     */
    public RegistryApi getRegistryApi() throws Exception {
        RegistryApi registryApi = null;
        if (this.getClient() != null) {
            registryApi = this.getRegistryApi(this.getClient());
        }
        return registryApi;
    }

    /**
     * RegistryApi 생성 with admin
     *
     * @return
     * @throws Exception
     */
    public RegistryApi getRegistryApiWithAdminAuth() throws Exception {
//        return new RegistryApi(this.getClientWithEncryptedAuth(registry.getId(), registry.getPassword()));
        return new RegistryApi(this.getClientWithAuth(registry.getUrl(), registry.getId(), registry.getPassword(), registry.getPrivateCertificateUseYn(), registry.getPrivateCertificate()));
    }

    /**
     * RegistryApi 생성 with 플랫폼 레지스트리
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public RegistryApi getRegistryApiByAccountSeq(Integer accountSeq) throws Exception {
        return new RegistryApi(this.getClientWithAuth(registry.getUrl(accountSeq), registry.getId(accountSeq), registry.getPassword(accountSeq), registry.getPrivateCertificateUseYn(accountSeq), registry.getPrivateCertificate(accountSeq)));
    }

    /**
     * RegistryApi 생성 with 플랫폼 레지스트리 정보
     *
     * @param accountRegistry
     * @return
     * @throws Exception
     */
    public RegistryApi getRegistryApiWithRegistry(AccountRegistryVO accountRegistry) throws Exception {
        return new RegistryApi(this.getClientWithAuth(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), accountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificate()));
    }


    /**
     * 레지스트리 접속 확인
     *
     * @param registryApi
     * @param registry
     * @return
     * @throws Exception
     */
    public int registriesPing(RegistryApi registryApi, RegistryPing registry) throws Exception {
        try {
            if (registryApi == null) {
                registryApi = this.getRegistryApiWithAdminAuth();
            }

            ApiResponse<Void> response = registryApi.pingRegistryWithHttpInfo(registry, null);

            return response.getStatusCode();
        } catch (Exception e) {
            this.exceptionHandle(e, false);
        }

        return 0;
    }




    /**
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * SysteminfoApi
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    /**
     * SysteminfoApi 생성
     *
     * @param apiClient
     * @return
     * @throws Exception
     */
    public SysteminfoApi getSysteminfoApi(HarborApiClientV2 apiClient) throws Exception {
        return new SysteminfoApi(apiClient);
    }

    /**
     * SysteminfoApi 생성
     *
     * @param apiClient
     * @return
     * @throws Exception
     */
    public SysteminfoApi getSysteminfoApi(Object apiClient) throws Exception {
        return this.getSysteminfoApi((HarborApiClientV2)apiClient);
    }

    /**
     * SysteminfoApi 생성
     *
     * @return
     * @throws Exception
     */
    public SysteminfoApi getSysteminfoApi() throws Exception {
        SysteminfoApi systeminfoApi = null;
        if (this.getClient() != null) {
            systeminfoApi = this.getSysteminfoApi(this.getClient());
        }
        return systeminfoApi;
    }

    /**
     * SysteminfoApi 생성 with admin
     *
     * @return
     * @throws Exception
     */
    public SysteminfoApi getSysteminfoApiWithAdminAuth() throws Exception {
//        return new SysteminfoApi(this.getClientWithEncryptedAuth(registry.getId(), registry.getPassword()));
        return new SysteminfoApi(this.getClientWithAuth(registry.getUrl(), registry.getId(), registry.getPassword(), registry.getPrivateCertificateUseYn(), registry.getPrivateCertificate()));
    }

    /**
     * SysteminfoApi 생성 with 플랫폼 레지스트리
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public SysteminfoApi getSysteminfoApiByAccountSeq(Integer accountSeq) throws Exception {
        return new SysteminfoApi(this.getClientWithAuth(registry.getUrl(accountSeq), registry.getId(accountSeq), registry.getPassword(accountSeq), registry.getPrivateCertificateUseYn(accountSeq), registry.getPrivateCertificate(accountSeq)));
    }

    /**
     * SysteminfoApi 생성 with 플랫폼 레지스트리 정보
     *
     * @param accountRegistry
     * @return
     * @throws Exception
     */
    public SysteminfoApi getSysteminfoApiWithRegistry(AccountRegistryVO accountRegistry) throws Exception {
        return new SysteminfoApi(this.getClientWithAuth(accountRegistry.getRegistryUrl(), accountRegistry.getAccessId(), accountRegistry.getAccessSecret(), accountRegistry.getPrivateCertificateUseYn(), accountRegistry.getPrivateCertificate()));
    }


    /**
     * Harbor systeminfo
     *
     * @param systeminfoApi
     * @return
     * @throws Exception
     */
    public GeneralInfo getSysteminfo(SysteminfoApi systeminfoApi) throws Exception {
        try {
            if (systeminfoApi == null) {
                systeminfoApi = this.getSysteminfoApiWithAdminAuth();
            }

            ApiResponse<GeneralInfo> response = systeminfoApi.getSystemInfoWithHttpInfo(null);

            return this.getResponse(response, "fail to get systeminfo.", ExceptionType.RegistryApiFail);
        } catch (Exception e) {
            this.exceptionHandle(e, false);
        }

        return null;
    }








    /**
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * ============================================================================================================================================
     * Common
     * ============================================================================================================================================
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */
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

    private void exceptionHandleForForbidden(Exception e) throws Exception {
        boolean isThrow = true;
        if(e instanceof ApiException){
            if(StringUtils.equalsIgnoreCase("Forbidden", e.getMessage())){
                isThrow = false;
            }
        }
        this.exceptionHandle(e, isThrow);
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
            String errMsg = "";
            if (StringUtils.isNotBlank(e.getMessage())) {
                if (StringUtils.equalsIgnoreCase("Not Found", e.getMessage())) {
                    isThrow = false;
                }
                // ApiException 오류가 Tag mismatch 인 경우 재시도
                else if (StringUtils.equalsIgnoreCase("javax.net.ssl.SSLException: Tag mismatch!", e.getMessage())) {
                    if (isThrow) {
                        throw e;
                    }
                } else {
                    errMsg = String.format("%s, %s", e.getMessage(), ((ApiException) e).getResponseBody());
                }
            } else {
                // 2.7.1 에서는 e.getMessage() 값이 없음
                // Not Found
                /**
                 * {
                 *   "errors": [
                 *     {
                 *       "code": "NOT_FOUND",
                 *       "message": "project test not found"
                 *     }
                 *   ]
                 * }
                 */
                if ( ((ApiException) e).getCode() == HttpStatus.SC_NOT_FOUND) {
                    isThrow = false;
                } else {
                    errMsg = ((ApiException) e).getResponseBody();
                }
            }

            ce = new CocktailException(errMsg, e, ExceptionType.RegistryApiFail);
        }

        log.warn(ce.getMessage());

        if(isThrow){
            throw ce;
        }
    }
}
