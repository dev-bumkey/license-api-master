package run.acloud.api.monitoring.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.monitoring.dao.IAlertUserMapper;
import run.acloud.api.monitoring.vo.AlertUserListVO;
import run.acloud.api.monitoring.vo.AlertUserSearchVO;
import run.acloud.api.monitoring.vo.AlertUserVO;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.PagingUtils;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.commons.vo.PagingVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;


@Slf4j
@Service
public class AlertUserService {
	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	/**
	 * Alert User List 조회.
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public List<AlertUserVO> getAlertUsers(AlertUserSearchVO params) throws Exception {
		IAlertUserMapper dao = sqlSession.getMapper(IAlertUserMapper.class);

		// get Lists
		List<AlertUserVO> alertUsers = dao.getAlertUsers(params);

		return alertUsers;
	}

	/**
	 * Alert User List 조회
	 *
	 * @param accountSeq
	 * @param searchColumn
	 * @param searchKeyword
	 * @param order
	 * @param orderColumn
	 * @param nextPage
	 * @param itemPerPage
	 * @param maxId
	 * @return
	 * @throws Exception
	 */
	public AlertUserListVO getAlertUsers(Integer accountSeq, String searchColumn, String searchKeyword, String order, String orderColumn, Integer nextPage, Integer itemPerPage, String maxId) throws Exception {
		/**
		 * DevOps 권한의 사용자는 접근이 불가함..
		 */
		AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

		AlertUserListVO list = new AlertUserListVO();
		try {
			AlertUserSearchVO params = this.setAlertUserCommonParams(accountSeq, searchColumn, searchKeyword);
			ListCountVO listCount = this.getAlertUserCountAndMaxId(params);
			PagingVO paging = PagingUtils.setPagingParams(orderColumn, order, nextPage, itemPerPage, maxId, listCount);
			params.setPaging(paging);
			/** Params Setting Completed **/

			List<AlertUserVO> alertUsers = this.getAlertUsers(params);

			list.setItems(alertUsers);
			list.setTotalCount(params.getPaging().getListCount().getCnt());
			list.setMaxId(params.getPaging().getMaxId());
			list.setCurrentPage(nextPage);

			}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception ex) {
			throw new CocktailException("Alert User List Inquire Failure", ex, ExceptionType.CommonInquireFail);
		}

		return list;
	}

	/**
	 * Set alert user 관련 params
	 *
	 * @param accountSeq
	 * @param searchColumn
	 * @param searchKeyword
	 * @return
	 * @throws Exception
	 */
	public AlertUserSearchVO setAlertUserCommonParams(Integer accountSeq, String searchColumn, String searchKeyword) throws Exception {
		AlertUserSearchVO params = new AlertUserSearchVO();

		params.setAccountSeq(accountSeq);

		// 검색조건 있으면 추가
		if(StringUtils.isNotBlank(searchColumn) && StringUtils.isNotBlank(searchKeyword)) {
			params.setSearchColumn(searchColumn);
			params.setSearchKeyword(searchKeyword);
		}

//		if (StringUtils.isNotBlank(userName)) {
//			params.setUserName(userName);
//		}
//
//		if (StringUtils.isNotBlank(phoneNumber)) {
//			params.setPhoneNumber(phoneNumber);
//		}
//
//		if (StringUtils.isNotBlank(kakaoId)) {
//			params.setKakaoId(kakaoId);
//		}
//
//		if (StringUtils.isNotBlank(email)) {
//			params.setEmail(email);
//		}

		return params;
	}

	/**
	 * Alert User 갯수 & user_seq 기준 maxId 조회
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public ListCountVO getAlertUserCountAndMaxId(AlertUserSearchVO params) throws Exception {
		IAlertUserMapper dao = sqlSession.getMapper(IAlertUserMapper.class);
		return dao.getAlertUserCountAndMaxId(params);
	}


	/**
	 * Alert User 상세 정보 조회
	 * @param alertUserSeq
	 * @return
	 * @throws Exception
	 */
	public AlertUserVO getAlertUser(Integer alertUserSeq) throws Exception {
		IAlertUserMapper dao = sqlSession.getMapper(IAlertUserMapper.class);
		return dao.getAlertUser(alertUserSeq);
	}

	@Transactional(transactionManager = "transactionManager")
	public void addAlertUser(AlertUserVO alertUser) throws Exception {
		IAlertUserMapper dao = sqlSession.getMapper(IAlertUserMapper.class);
		dao.addAlertUser(alertUser);
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateAlertUser(AlertUserVO alertUser) throws Exception {
		IAlertUserMapper dao = sqlSession.getMapper(IAlertUserMapper.class);
		dao.updateAlertUser(alertUser);
	}

	@Transactional(transactionManager = "transactionManager")
	public void removeAlertUser(AlertUserVO alertUser) throws Exception {
		IAlertUserMapper dao = sqlSession.getMapper(IAlertUserMapper.class);
		dao.removeAlertUser(alertUser);
	}
}
