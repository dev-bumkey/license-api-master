package run.acloud.api.external.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.external.vo.KeycloakUserVO;
import run.acloud.commons.vo.ListCountVO;

import java.util.List;
import java.util.Map;

public interface IKeycloakUserMapper {

    List<KeycloakUserVO> getUsers(
        Map<String, Object> params
    ) throws Exception;

    KeycloakUserVO getUser(
        @Param("accountCode") String accountCode,
        @Param("userId") String userId,
        @Param("roleCode") String roleCode
    ) throws Exception;

    ListCountVO getUserCountAndMaxId(
        Map<String, Object> params
    ) throws Exception;
}
