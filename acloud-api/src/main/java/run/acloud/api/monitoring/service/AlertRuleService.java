package run.acloud.api.monitoring.service;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.monitoring.dao.IAlertRuleMapper;
import run.acloud.api.monitoring.dao.IAlertUserMapper;
import run.acloud.api.monitoring.handler.AlertRuleResultHandler;
import run.acloud.api.monitoring.vo.AlertRuleListVO;
import run.acloud.api.monitoring.vo.AlertRuleVO;
import run.acloud.api.monitoring.vo.AlertUserVO;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.ExcelUtils;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class AlertRuleService {
	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	/**
	 * Audit Log List 조회.
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public List<AlertRuleVO> getAlertRules(Map<String, Object> params) throws Exception {
		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);

		// get Lists
		List<AlertRuleVO> alertRules = dao.getAlertRules(params);

		return alertRules;
	}

	/**
	 * Alert Rule List 조회
	 * @param order
	 * @param orderColumn
	 * @param nextPage
	 * @param itemPerPage
	 * @param searchColumn
	 * @param searchKeyword
	 * @param maxId
	 * @param newId
	 * @return
	 * @throws Exception
	 */
	public AlertRuleListVO getAlertRules(String alertState, String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String maxId, String newId) throws Exception {
		/**
		 * DevOps 권한의 사용자는 접근이 불가함..
		 */
		AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

		AlertRuleListVO alertRuleList = new AlertRuleListVO();
		try {
			Map<String, Object> params = this.setParams(alertState, searchColumn, searchKeyword);

			Integer limitNextPage = (nextPage - 1) * itemPerPage; // nextPage 값을 LIMIT Query 형태에 맞도록 변환

			params.put("order", order);
			params.put("orderColumn", orderColumn);
			params.put("nextPage", limitNextPage);
			params.put("itemPerPage", itemPerPage);

			ListCountVO alertRuleCount = getAlertRuleCountAndMaxId(params);
			Integer totalCount = alertRuleCount.getCnt(); // this.getAlertRuleCount(params);
			if(StringUtils.isBlank(maxId) && alertRuleCount.getMaxId() != null) {
				maxId = alertRuleCount.getMaxId().toString();
			}

			params.put("maxId", maxId);
			/** Params Setting Completed **/

			List<AlertRuleVO> alertRules = this.getAlertRules(params);
			alertRuleList.setAlertRules(alertRules);
			alertRuleList.setTotalCount(totalCount);
			alertRuleList.setMaxId(maxId);
			alertRuleList.setCurrentPage(nextPage);
		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception ex) {
			throw new CocktailException("Alert Rule List Inquire Failure", ex, ExceptionType.CommonInquireFail);
		}

		return alertRuleList;
	}

	public void downloadExcelAlertRules(HttpServletResponse response, String alertState, String searchColumn, String searchKeyword) throws Exception {
		// excel header 컬럼 순서대로 셋팅
		List<Pair<String, Integer>> headers = Lists.newArrayList();
		headers.add(Pair.of("Notification Name", 40));
		headers.add(Pair.of("Status", 20));
		headers.add(Pair.of("Message", 40));
		headers.add(Pair.of("Processing time", 20));
		headers.add(Pair.of("Description", 40));
		headers.add(Pair.of("Target Cluster", 40));
		headers.add(Pair.of("Recipients", 40));

		// parameter 셋팅
		Map<String, Object> params = this.setParams(alertState, searchColumn, searchKeyword);

		// resultHandler
		AlertRuleResultHandler<AlertRuleVO> resultHandler = new AlertRuleResultHandler(response, "alert-rules.xlsx", "list", headers);

		// 조회
		sqlSession.select(String.format("%s.%s", IAlertRuleMapper.class.getName(), "getAlertRulesForExcel"), params, resultHandler);

		// excel 생성 및 종료
		ExcelUtils.closeAfterWrite(resultHandler.getResponse(), resultHandler.getWorkbook());
	}

	public Map<String, Object> setParams(String alertState, String searchColumn, String searchKeyword) throws Exception {
		Map<String, Object> params = new HashMap<>();
		if(StringUtils.isNotBlank(searchColumn) && StringUtils.isNotBlank(searchKeyword)) {
			params.put("searchColumn", searchColumn);
			params.put("searchKeyword", searchKeyword);
		}
		if (StringUtils.isNotBlank(alertState)) {
			params.put("alertState", alertState);
		}
		return params;
	}

	/**
	 * Alert Rule 갯수 & alert_rule_seq 기준 maxId 조회
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public ListCountVO getAlertRuleCountAndMaxId(Map<String, Object> params) throws Exception {
		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);

		// get Audit Log Count
		return dao.getAlertRuleCountAndMaxId(params);
	}


	/**
	 * Alert Rule 상세 정보 조회
	 * @param alertRuleSeq
	 * @return
	 * @throws Exception
	 */
	public AlertRuleVO getAlertRule(Integer alertRuleSeq) throws Exception {
		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);
		IAlertUserMapper alertUserDao = sqlSession.getMapper(IAlertUserMapper.class);

		// get Alert Rule
		AlertRuleVO alertRule = dao.getAlertRule(alertRuleSeq);

		if(alertRule == null) {
			return alertRule;
		}

		// get mapped user
//		List<AlertUserVO> alertReceivers = alertUserDao.getAlertUsersOfRule(ContextHolder.exeContext().getUserAccountSeq(), alertRuleSeq, null);
//
//		if(CollectionUtils.isNotEmpty(alertReceivers)) {
//			alertRule.setAlertReceivers(alertReceivers);
//		}

		return alertRule;
	}

	/**
	 * Alert Rule Id로 Alert을 수신할 사용자 목록을 조회
	 *
	 * @param alertRuleId
	 * @param clusterId
	 * @return
	 * @throws Exception
	 */
	public List<AlertUserVO> getUsersByAlertRuleId(String alertRuleId, String clusterId) throws Exception {
		IAlertUserMapper alertUserDao = sqlSession.getMapper(IAlertUserMapper.class);

		// get mapped user
		List<AlertUserVO> alertReceivers = alertUserDao.getAlertUsersOfRule(ContextHolder.exeContext().getUserAccountSeq(), null, clusterId, null, alertRuleId);

		return alertReceivers;
	}

	@Transactional(transactionManager = "transactionManager")
	public AlertRuleVO addAlertRule(AlertRuleVO alertRule) throws Exception {
		/** 생성 전 Alert Rule ID 중복 체크 **/
		if(this.checkRuleIdIfExists(alertRule.getAlertRuleId())) {
			throw new CocktailException("AlertRuleID Already Exists", ExceptionType.AlertRuleIdAlreadyExists);
		}

		/** 생성 **/
		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);
		dao.addAlertRule(alertRule);

		return alertRule;
	}

	@Transactional(transactionManager = "transactionManager")
	public AlertRuleVO updateAlertRule(AlertRuleVO alertRule) throws Exception {
		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);
		dao.updateAlertRule(alertRule);

		return alertRule;
	}

	@Transactional(transactionManager = "transactionManager")
	public AlertRuleVO removeAlertRule(AlertRuleVO alertRule) throws Exception {
		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);
		dao.removeAlertRule(alertRule);

		return alertRule;
	}

	public boolean checkRuleIdIfExists(String alertRuleId) throws Exception {
		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);

		Map<String, Object> params = this.setParams(null, null, null);
		params.put("alertRuleId", alertRuleId);

		List<AlertRuleVO> alertRules = dao.getAlertRules(params);

		if(CollectionUtils.isNotEmpty(alertRules)) {
			return true; // 사용중
		}
		else {
			return false; // 미사용
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public void updateUsersOfAlertRule(Integer alertRuleSeq, List<Integer> reqUserSeqs, Integer updater) throws Exception {
		if (reqUserSeqs == null) {
			reqUserSeqs = new ArrayList<>();
		}

		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);

		List<Integer> currUserSeqs = dao.getUserSeqsOfAlertRule(alertRuleSeq);
		@SuppressWarnings("unchecked")
		List<Integer> deleteUserSeqs = ListUtils.subtract(currUserSeqs, reqUserSeqs);
		@SuppressWarnings("unchecked")
		List<Integer> addUserSeqs = ListUtils.subtract(reqUserSeqs, currUserSeqs);

		// 사용자 삭제
		if (CollectionUtils.isNotEmpty(deleteUserSeqs)) {
			dao.deleteAlertRuleUserMappingByAlertRule(alertRuleSeq, deleteUserSeqs);
		}
		// 사용자 추가
		if (CollectionUtils.isNotEmpty(addUserSeqs)) {
			dao.addAlertRuleUserMappingByAlertRule(alertRuleSeq, addUserSeqs, updater);
		}

	}

	@Transactional(transactionManager = "transactionManager")
	public void updateClustersOfAlertRule(Integer alertRuleSeq, List<Integer> reqClusterSeqs, Integer updater) throws Exception {
		if (reqClusterSeqs == null) {
			reqClusterSeqs = new ArrayList<>();
		}

		IAlertRuleMapper dao = sqlSession.getMapper(IAlertRuleMapper.class);

		List<Integer> currClusterSeqs = dao.getClusterSeqsOfAlertRule(alertRuleSeq);
		@SuppressWarnings("unchecked")
		List<Integer> deleteClusterSeqs = ListUtils.subtract(currClusterSeqs, reqClusterSeqs);
		@SuppressWarnings("unchecked")
		List<Integer> addClusterSeqs = ListUtils.subtract(reqClusterSeqs, currClusterSeqs);

		// 사용자 삭제
		if (CollectionUtils.isNotEmpty(deleteClusterSeqs)) {
			dao.deleteAlertRuleClusterMappingByAlertRule(alertRuleSeq, deleteClusterSeqs);
		}
		// 사용자 추가
		if (CollectionUtils.isNotEmpty(addClusterSeqs)) {
			dao.addAlertRuleClusterMappingByAlertRule(alertRuleSeq, addClusterSeqs, updater);
		}

	}
}
