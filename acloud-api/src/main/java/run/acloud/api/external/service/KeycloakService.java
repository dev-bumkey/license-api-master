package run.acloud.api.external.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import run.acloud.api.external.dao.IKeycloakUserMapper;
import run.acloud.api.external.vo.KeycloakUserListVO;
import run.acloud.api.external.vo.KeycloakUserVO;
import run.acloud.commons.util.DateTimeUtils;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;

@Slf4j
@Service
public class KeycloakService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    /**
     * Audit Log List 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<KeycloakUserVO> getKeycloakUsers(Map<String, Object> params) throws Exception {
        IKeycloakUserMapper dao = sqlSession.getMapper(IKeycloakUserMapper.class);

        // get Lists
        List<KeycloakUserVO> keycloakUsers = dao.getUsers(params);

        return keycloakUsers;
    }

    /**
     * Audit Log List 조회
     * @param order
     * @param orderColumn
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @return
     */
    public KeycloakUserListVO getKeycloakUsers(String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String maxId, String newId, String startDate, String endDate) throws Exception {
        KeycloakUserListVO keycloakUserList = new KeycloakUserListVO();
        try {
            Map<String, Object> params = new HashMap<>();

            Integer limitNextPage = (nextPage - 1) * itemPerPage; // nextPage 값을 LIMIT Query 형태에 맞도록 변환

            if(StringUtils.isBlank(startDate) || StringUtils.isBlank(endDate)) { // 날짜 없으면 Default로 현재부터 일주일 설정
                Date dEndDate = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dEndDate);
                calendar.add(Calendar.DATE, -7);

                Date dStartDate = calendar.getTime();
                startDate = DateTimeUtils.getUtcTimeString(dStartDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
                endDate = DateTimeUtils.getUtcTimeString(dEndDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
            }

            if(StringUtils.isNotBlank(searchColumn) && StringUtils.isNotBlank(searchKeyword)) {
                params.put(searchColumn, searchKeyword);
            }

            params.put("order", order);
            params.put("orderColumn", orderColumn);
            params.put("nextPage", limitNextPage);
            params.put("itemPerPage", itemPerPage);
            params.put("startDate", startDate);
            params.put("endDate", endDate);

            params.put("roleCode", "ADMIN"); // 2020.08.24 : ADMIN 계정만 Federation하도록 함.

            ListCountVO keycloakUserCount = getKeycloakUserCountAndMaxId(params);
            Integer totalCount = keycloakUserCount.getCnt(); // this.getKeycloakUserCount(params);
            if(StringUtils.isBlank(maxId) && keycloakUserCount.getMaxId() != null) {
                maxId = keycloakUserCount.getMaxId().toString();
            }

            params.put("maxId", maxId);
            /** Params Setting Completed **/

            List<KeycloakUserVO> keycloakUsers = this.getKeycloakUsers(params);

            keycloakUserList.setUsers(keycloakUsers);
            keycloakUserList.setTotalCount(totalCount);
            keycloakUserList.setMaxId(maxId);
            keycloakUserList.setCurrentPage(nextPage);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception ex) {
            throw new CocktailException("Keycloak User List Inquire Failure", ex, ExceptionType.CommonInquireFail);
        }

        return keycloakUserList;
    }

    /**
     * Audit Log 갯수 & audit_log_seq 기준 maxId 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getKeycloakUserCountAndMaxId(Map<String, Object> params) throws Exception {
        IKeycloakUserMapper dao = sqlSession.getMapper(IKeycloakUserMapper.class);

        // get Audit Log Count
        return dao.getUserCountAndMaxId(params);
    }

    public KeycloakUserVO getKeycloakUser(String accountCode, String userId, String roleCode) throws Exception {
        IKeycloakUserMapper dao = sqlSession.getMapper(IKeycloakUserMapper.class);
        // get Audit Log Count
        return dao.getUser(accountCode, userId, roleCode);
    }

}
