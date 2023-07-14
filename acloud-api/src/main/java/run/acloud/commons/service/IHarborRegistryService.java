package run.acloud.commons.service;

import run.acloud.api.configuration.vo.AccountRegistryVO;
import run.acloud.api.configuration.vo.ImageRepositoryTagVO;
import run.acloud.api.configuration.vo.RegistryProjectVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.vo.*;

import java.util.List;

public interface IHarborRegistryService {

    public ImageRepositoryTagVO getImageTagInfo(String projectName, String imageName , String tag) throws Exception;
    public RegistryProjectVO addProject(HarborProjectReqVO projectReq) throws Exception;
    public void deleteProject(Integer projectId) throws Exception;
    public List<RegistryProjectVO> getProjectList(String projectName) throws Exception;
    public RegistryProjectVO getProject(String projectName) throws Exception;
    public RegistryProjectVO getProject(Integer projectId) throws Exception;
    public HarborUserRespVO addUser(HarborUserReqVO user) throws Exception;
    public void deleteUser(Integer userId) throws Exception;
    public void deleteUser(String username) throws Exception;
    public HarborUserRespVO getUser(Integer userId) throws Exception;
    public HarborUserRespVO getUser(String username) throws Exception;
    public List<HarborPermissionVO> getCurrentUserPermissions(String scope, Boolean relative) throws Exception;
    public ResultVO getCurrentUserPermissionsStatus(String scope, Boolean relative) throws Exception;
    public HarborProjectMemberVO getMemberOfProject(Integer projectId, String userName) throws Exception;
    public void addMembersToProjects(List<Integer> projects, List<HarborProjectMemberVO> users, Boolean ignoreException) throws Exception;
    public void addMemberToProjects(List<Integer> projects, HarborProjectMemberVO user, Boolean ignoreException) throws Exception;
    public void addMemberToProject(Integer projectId, HarborProjectMemberVO user, Boolean ignoreException) throws Exception;
    public void updateMemberOfProject(Integer projectId, Integer mid, Integer roleId) throws Exception;
    public void deleteMemberOfProject(Integer projectId, Integer mid) throws Exception;
    public boolean isRegistryImagesCheck(String projectName , String imageName);
    public boolean deleteImagesFromProjects(String projectName, String imageName);
    public boolean deleteImagesFromProjects(String projectName, String imageName, String tag);
    public int registriesPing(HarborRegistryPingVO registryPingReq) throws Exception;
    public HarborGeneralInfoVO getSystemGeneralInfo();
    public HarborGeneralInfoVO getSystemGeneralInfo(Integer accountSeq);
    public HarborGeneralInfoVO getSystemGeneralInfo(AccountRegistryVO accountRegistry);
    public ApiVersionType getApiVersion();
    public Object getClient();
}
