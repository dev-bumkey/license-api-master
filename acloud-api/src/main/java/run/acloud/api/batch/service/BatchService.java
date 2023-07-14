package run.acloud.api.batch.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.kubernetes.client.openapi.models.V1Node;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.audit.dao.IAuditAccessLogMapper;
import run.acloud.api.audit.dao.IAuditLogMapper;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.enums.ServiceMode;
import run.acloud.api.billing.service.AccountBillService;
import run.acloud.api.billing.vo.AccountBillDailyChargeVO;
import run.acloud.api.billing.vo.AccountBillProductVO;
import run.acloud.api.billing.vo.AccountBillVO;
import run.acloud.api.build.dao.IBuildMapper;
import run.acloud.api.build.vo.SystemBuildCountVO;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.dao.IGradePlanChargeBaseMapper;
import run.acloud.api.configuration.enums.HistoryState;
import run.acloud.api.configuration.enums.IssueType;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.service.AccountGradeService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.service.ProviderAccountService;
import run.acloud.api.configuration.service.UserClusterRoleIssueService;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.openapi.dao.IOpenapiMapper;
import run.acloud.api.openapi.enums.TokenState;
import run.acloud.api.openapi.vo.ApiTokenIssueHistoryVO;
import run.acloud.api.openapi.vo.ApiTokenIssueVO;
import run.acloud.api.openapi.vo.ApiTokenPermissionsScopeHistoryVO;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.commons.client.ClusterApiClient;
import run.acloud.commons.client.MonitoringApiClient;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BatchService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private UserClusterRoleIssueService userClusterRoleIssueService;

    @Autowired
    private AccountGradeService accountGradeService;

    @Autowired
    private ProviderAccountService providerAccountService;

    @Autowired
    private MonitoringApiClient monitoringApiClient;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private AccountBillService accountBillService;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ClusterApiClient clusterApiClient;

    /**
     * 현재 Grade 적용중인 계정에 대한 현재 적용중인 동시빌드수, 총 빌드수, workspace수, core수 등의 정보를 시간별 미터링 테이블에 저장한다.<br/>
     * <br/>
     * Update, 2019/06/25, node_cnt 추가
     *
     * @param request
     * @return
     * @throws Exception
     * @since  Create(2019/02/12):coolingi, Update(2019/06/25):coolingi
     */
    public int createAccountHourMetering(HttpServletRequest request) throws Exception{

        int resultCount = 0;
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);

        // 미터링 대상 Account 정보 조회, accountGrade 테이블 조회, 사용중인 모든건 조회(use_yn = 'Y')
        AccountGradeVO srchVO = new AccountGradeVO();
        srchVO.setAccountSeq(null);
        srchVO.setApplyState(null);
        List<AccountGradeVO> targetAccounts = accountGradeService.getAccountGrades(srchVO);

        // loop 계정별 처리, 필요한 정보 수집
        for (AccountGradeVO accountGradeVO : targetAccounts) {
            AccountMeteringVO meteringVO = new AccountMeteringVO();

            meteringVO.setAccountSeq(accountGradeVO.getAccountSeq());
            meteringVO.setGradeSeq(accountGradeVO.getGradeSeq());
            meteringVO.setParallelBuildCnt(accountGradeVO.getParallelBuildCnt());

            int workspaceCount = 0;
            // Account의 서비스 조회
            List<ServiceDetailVO> serviceVOList = serviceDao.getServiceOfAccount(accountGradeVO.getAccountSeq(), null);
            if(serviceVOList != null){
                workspaceCount = (int) serviceVOList.stream().filter(vo -> vo.getServiceType() != ServiceType.PLATFORM).count();
            }
            // workspace 수 셋팅
            meteringVO.setWorkspaceCnt(workspaceCount);

            // total build count 조회 & 셋팅
            int totalBuildCount = 0;
            SystemBuildCountVO buildCount = buildDao.getSystemBuildCount(accountGradeVO.getAccountSeq());
            if(buildCount != null) {
                totalBuildCount = buildCount.getBuildCount();
            }
            meteringVO.setTotalBuildCnt(totalBuildCount);

            // cluster 리스트 조회
//            List<ClusterVO> clusters = clusterService.getClusterCondition(accountGradeVO.getAccountSeq(), null, false, false, false, null);
            List<ClusterVO> clusters = Optional.ofNullable(clusterDao.getClusters(accountGradeVO.getAccountSeq(), null, null, null, null, "Y")).orElseGet(() ->Lists.newArrayList()).stream().filter(vo -> StringUtils.equals("Y",vo.getUseYn())).collect(Collectors.toList());

            // node 수, core 수 조회 (monitoring api 연동) 및 합계
            int totalNodeCnt = 0;
            float totalCoreCnt = 0.0F;

            for(ClusterVO cluster : clusters){
                if(clusterStateService.isClusterRunning(cluster)) {
                    totalNodeCnt += getClusterNodeCount(cluster);
                    totalCoreCnt += getClusterCoreCount(request, cluster);
                }
            }

            // node수 셋팅
            meteringVO.setNodeCnt(totalNodeCnt);

            // core수 셋팅
            meteringVO.setCoreCnt(Math.round(totalCoreCnt));

            // account의 metering 데이터 저장
            accountGradeService.addAccountHourMetering(meteringVO);
            resultCount++;
        }

        return resultCount;
    }

    private int getClusterNodeCount(ClusterVO cluster){
        int nodeCount = 0;

        try {
            List<V1Node> v1Nodes = k8sWorker.getNodesV1(cluster, null, null);
            if(CollectionUtils.isNotEmpty(v1Nodes)){
                nodeCount = v1Nodes.size();
            }
        } catch (Exception e) {
            log.error("cluster 의 노드 조회 실패", e);
        }
        return nodeCount;
    }

    private float getClusterCoreCount(HttpServletRequest request, ClusterVO cluster){
        float clusterCoreCnt = 0.0F;

        try {
            // 클러스터가 baremetal 타입인지 확인, 2019/06/25
            ProviderAccountVO providerAccountVO = providerAccountService.getProviderAccount(cluster.getProviderAccountSeq());

            boolean bereMetal = false;
            if( providerAccountVO.getProviderCode().isBaremetal() ){
                bereMetal = true;
            }else{
                bereMetal = false;
            }

            // Core 정보 조회 및 Core수 셋팅
            Map<String, Object> clusterCapacityMap = monitoringApiClient.getCapacityOfClusterByClusterId(cluster.getClusterId());

            if(clusterCapacityMap != null && !clusterCapacityMap.isEmpty() && clusterCapacityMap.get("result") != null){

                List<Map<String, Object>> resultList = (List<Map<String, Object>>)clusterCapacityMap.get("result");
                if(CollectionUtils.isNotEmpty(resultList)){
                    float tmpCoreCnt = 0.0F;
                    for(Map<String, Object> mapRow : resultList){
                        if(mapRow.get("metrics") != null){
                            Map<String, String> metric = (Map<String, String>)mapRow.get("metrics");
                            if(StringUtils.equalsIgnoreCase("capacity-cpu", metric.get("measure"))){ // CPU 정보만 합산

                                tmpCoreCnt = MapUtils.getFloat(mapRow, "value", 0.0F);
                                if(bereMetal) {
                                    tmpCoreCnt = tmpCoreCnt * 2; // bearmetal일 경우는 core 갯수 * 2
                                }

                                clusterCoreCnt += tmpCoreCnt; //최종 core수에 더함
                                break; //cpu 찾았을땐 다른정보는 skip 한다.
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CocktailException ce = new CocktailException(String.format("getCapacityOfCluster - [%s] fail resource calculation", cluster.getClusterName()), e, ExceptionType.ExternalApiFail_Monitoring);
            log.error(ExceptionMessageUtils.setCommonResult(request, null, ce, false), e);
        }

        return clusterCoreCnt;
    }

    public int createAccountBillDailyCharge(HttpServletRequest request, String paramDate) throws Exception{
        int resultCount = 0;

        String regionTimeZone = cocktailServiceProperties.getRegionTimeZone();
        DateTimeFormatter baseDayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        // 조회 날짜 결정, paramDate없을 때는 현재 날짜 기준으로 계산
        String baseDate = paramDate;
        if( baseDate == null || "NONE".equals(baseDate) ) { // 기준일자가 없을 경우는 현재시간의 하루전 날짜를 기준으로 함, timeZone 값 사용
            baseDate = DateTime.now(DateTimeZone.forID(regionTimeZone)).minusDays(1).toString(baseDayFmt);
        }

        String nextDate = baseDayFmt.parseDateTime(baseDate).plusDays(1).toString(baseDayFmt); // 기준일 다음날짜 구함

        // hour metering 정보에서 account_seq 가져오게 했음.
        List<Integer> accountSeqList = accountGradeService.getAccountSeqListInHourMeteringByBaseDate(baseDate);

        // 전체 grade 과금 기본정보 조회 및 gradeSeq 별 Map 저장
        Map<Integer, List<GradePlanChargeBaseVO>> gradePlanChargeBaseMap = this.getGradePlanChargeBaseMap();

        List<AccountBillDailyChargeVO> dailyChargeList = new ArrayList<>();
        for (Integer accountSeq : accountSeqList) {
            log.debug("Start daily charge process [{}, {}]", accountSeq, baseDate);

            // 동일한 날짜의 데이터가 있는지 조회
            List<AccountBillDailyChargeVO> preDailyChargeList = accountBillService.getAccountBillDailyCharges(accountSeq, baseDate, nextDate);

            // 해당날짜에 데이터가 존재하면 처리하지 않고 skip 한다.
            if(CollectionUtils.isNotEmpty(preDailyChargeList)) {
                log.debug("The daily charge data exists already!! [{}, {}]", accountSeq, baseDate);
                continue;
            }

            List<AccountMeteringVO> accountMeteringList = accountGradeService.getAccountHourMeterings(accountSeq, null, null, baseDate, null, null);

            // 최종 일별 과금 저장할 VO 객체 생성 및 기본값 셋팅
            AccountBillDailyChargeVO dailyChargeVO = new AccountBillDailyChargeVO();
            dailyChargeVO.setAccountSeq(accountSeq);
            dailyChargeVO.setChargeBaseDate(baseDate);

            Integer tmpGradeSeq = 0;
            List<GradePlanChargeBaseVO> tmpChargeBaseList = null;

            // 시간별 미터링 정보를 looping 처리
            for(AccountMeteringVO hourMeteringVO : accountMeteringList){
                // 시간별 미터링에 대한 과금 금액 계산, by grade 과금 기본정보
                tmpGradeSeq = hourMeteringVO.getGradeSeq();
                tmpChargeBaseList = gradePlanChargeBaseMap.get(tmpGradeSeq); // 등급에 해당하는 과금 기본정보 조회

                // 과금 기본정보에 맞게 과금 계산 및 합계
                if(CollectionUtils.isNotEmpty(tmpChargeBaseList)) {
                    for (GradePlanChargeBaseVO chargeBaseVO : tmpChargeBaseList) {
                        switch (chargeBaseVO.getChargeType()) {
                            case ADD_CONCURRENT_BUILD:
                                int addConcurrentCnt = hourMeteringVO.getParallelBuildCnt() - chargeBaseVO.getBaseQty();
                                if (addConcurrentCnt > 0) { //기본 수량보다 사용하는 수량 보다 클 경우만 금액계산
                                    BigDecimal conBuildAmt = chargeBaseVO.getBasePricePerHour().multiply(BigDecimal.valueOf(addConcurrentCnt)); // 시간당 추가 계산값
                                    dailyChargeVO.setAddParallBuildAmt(dailyChargeVO.getAddParallBuildAmt().add(conBuildAmt));
                                }
                                break;
                            case ADD_BUILD: // 빌드수는 100개단위 체크, 1~100, 101~200...
                                int addBuildCnt = (hourMeteringVO.getTotalBuildCnt() - 1) / chargeBaseVO.getBaseQty(); // 100개 단위로 처리해야 하므로 나눗셈 처리
                                if (addBuildCnt > 0) { //기본 수량보다 사용하는 수량 보다 큰 경우만 금액계산
                                    BigDecimal addBuildAmt = chargeBaseVO.getBasePricePerHour().multiply(BigDecimal.valueOf(addBuildCnt)); // 시간당 추가 계산값
                                    dailyChargeVO.setAddBuildAmt(dailyChargeVO.getAddBuildAmt().add(addBuildAmt));
                                }
                                break;
                            case ADD_WORKSPACE: // 추가 워크스페이스 수 처리
                                int addWorkspaceCnt = hourMeteringVO.getWorkspaceCnt() - chargeBaseVO.getBaseQty();
                                if (addWorkspaceCnt > 0) { //기본 수량보다 사용하는 수량이 큰 경우만 금액계산
                                    BigDecimal addWorkspaceAmt = chargeBaseVO.getBasePricePerHour().multiply(BigDecimal.valueOf(addWorkspaceCnt)); // 시간당 추가 계산값
                                    dailyChargeVO.setAddWorkspaceAmt(dailyChargeVO.getAddWorkspaceAmt().add(addWorkspaceAmt)); // 일별 과금 VO에 금액 추가
                                }
                                break;
                            case CORE:
                                int coreCnt = hourMeteringVO.getCoreCnt();
                                if (coreCnt > 0) {
                                    BigDecimal coreCntAmt = chargeBaseVO.getBasePricePerHour().multiply(BigDecimal.valueOf(coreCnt)); // 시간당 추가 계산값
                                    dailyChargeVO.setCoreAmt(dailyChargeVO.getCoreAmt().add(coreCntAmt)); // 일별 과금 VO에 금액 추가
                                }
                                break;
                            case NODE:
                                int nodeCnt = hourMeteringVO.getNodeCnt();
                                if (nodeCnt > 0) {
                                    BigDecimal nodeCntAmt = chargeBaseVO.getBasePricePerHour().multiply(BigDecimal.valueOf(nodeCnt)); // 시간당 추가 계산값
                                    dailyChargeVO.setNodeAmt(dailyChargeVO.getNodeAmt().add(nodeCntAmt)); // 일별 과금 VO에 금액 추가
                                }
                                break;
                        }
                    }
                }
            }
            log.debug("End daily charge process [{}, {}]", accountSeq, baseDate);

            dailyChargeList.add(dailyChargeVO);

            resultCount++;
        }

        // dailyCharge 정보 저장
        if(CollectionUtils.isNotEmpty(dailyChargeList)) {
            accountBillService.addAccountBillDailyChargeList(dailyChargeList);
        }

        return resultCount;
    }

    // 청구서 데이터 등록
    public int createAccountBillingData(HttpServletRequest request, String paramMonth) throws Exception{
        int resultCount = 0;
        int TAX_RATE = 10; // 부가세율

        String regionTimeZone = cocktailServiceProperties.getRegionTimeZone();

        // 기준월에 따른 필요 날짜 계산
        DateTimeFormatter monthFmt = DateTimeFormat.forPattern("yyyy-MM");
        DateTimeFormatter dayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        String baseMonth = paramMonth;
        if(baseMonth == null || "NONE".equals(baseMonth) ){ // paramMonth가 없는 경우
            baseMonth = DateTime.now(DateTimeZone.forID(regionTimeZone)).minusMonths(1).toString(monthFmt);
        }
        DateTime baseMonthDateTime = monthFmt.parseDateTime(baseMonth);

        String baseDate = baseMonthDateTime.toString(dayFmt); // 기준일자, 월첫일
        String billDate = DateTime.now(DateTimeZone.forID(regionTimeZone)).toString(dayFmt); // 청구일자, 현재일 셋팅
        String dueDate = DateTime.now(DateTimeZone.forID(regionTimeZone)).toLocalDate().dayOfMonth().withMaximumValue().toString(dayFmt); // 납기일자, 현재월의 말일 셋팅

        // 전체 grade 과금 기본정보 조회 및 gradeSeq 별 Map 저장
        Map<Integer, List<GradePlanChargeBaseVO>> gradePlanChargeBaseMap = this.getGradePlanChargeBaseMap();

        // account별 dailyChargeSum 정보 조회, accountSeq로 group by
        List<AccountBillDailyChargeVO> accountSumDailyChargesList = accountBillService.getSumDailyCharges(null, baseDate);

        // loop 계정별 처리, 필요한 정보 수집 및 청구서 리스트 생성
        List<AccountBillVO> accountBillList = new ArrayList<>();
        for (AccountBillDailyChargeVO sumDailyCharge : accountSumDailyChargesList) {

            // 등급 정보 조회
            AccountGradeVO accountGradeVO = accountGradeService.getLastAccountGrade(sumDailyCharge.getAccountSeq());

            // 등급별 과금 기본정보 조회
            List<GradePlanChargeBaseVO> gradePlanChargeBases = gradePlanChargeBaseMap.get(accountGradeVO.getGradeSeq());

            // 계정정보 조회 : 2020.01.09 : 사용하지 않아 주석 처리함..
//            AccountVO accountVO = accountService.getAccount(sumDailyCharge.getAccountSeq());

            // 기본 청구서 데이터 생성
            DateTimeFormatter monthDBFmt = DateTimeFormat.forPattern("yyyyMM"); // DB에 저장될 사용월 포맷
            AccountBillVO accountBillVO = new AccountBillVO();
            accountBillVO.setAccountSeq(sumDailyCharge.getAccountSeq()); // account seq
            accountBillVO.setUsedMonth(baseMonthDateTime.toString(monthDBFmt));   // 사용 월
            accountBillVO.setUsedStartDate(sumDailyCharge.getUsedStartDate());  // 사용 시작일
            accountBillVO.setUsedEndDate(sumDailyCharge.getUsedEndDate());  // 사용 종료일
            accountBillVO.setBillDate(billDate); // 청구일자
            accountBillVO.setDueDate(dueDate); // 납부일자

            // 금액 초기화
            accountBillVO.setBillAmt(BigDecimal.valueOf(0)); // 청구금액 0
            accountBillVO.setAdditionalTaxRate(TAX_RATE); // 부가세율, 10%
            accountBillVO.setAdditionalTax(BigDecimal.valueOf(0)); // 부가세 금액 0
            accountBillVO.setFinalBillAmt(BigDecimal.valueOf(0)); // 최종 청구 금액 0

            // 청구서 상품 데이터 생성
            List<AccountBillProductVO> billProducts = new ArrayList<>();
            AccountBillProductVO productVO = null;

            for(GradePlanChargeBaseVO chargeBaseVO : gradePlanChargeBases){
                // 상품 기본정보 설정
                productVO = new AccountBillProductVO();
                productVO.setSvcNm(chargeBaseVO.getChargeAreaName());
                productVO.setPrdNm(chargeBaseVO.getChargeName());
                productVO.setPrdCurrency(chargeBaseVO.getBaseCurrency());
                productVO.setDiscountRate(0);
                productVO.setDescription(chargeBaseVO.getDescription());

                switch (chargeBaseVO.getChargeType()){

                    case DEFAULT:
                        accountBillVO.setCurrency(chargeBaseVO.getBaseCurrency()); // 청구서의 기본 통화코드 셋팅

                        productVO.setPrdAmt(chargeBaseVO.getBasePrice());
                        productVO.setPrdBillAmt(chargeBaseVO.getBasePrice());
                        billProducts.add(productVO);
                        break;
                    case ADD_CONCURRENT_BUILD: // 추가 동시빌드 금액이 존재 할때만 처리,
                        if(sumDailyCharge.getAddParallBuildAmt().compareTo(BigDecimal.valueOf(0)) > 0) {
                            productVO.setPrdAmt(sumDailyCharge.getAddParallBuildAmt());
                            productVO.setPrdBillAmt(sumDailyCharge.getAddParallBuildAmt());
                            billProducts.add(productVO);
                        }
                        break;
                    case ADD_BUILD: // 추가 빌드수 금액이 존재 할때만 처리
                        if(sumDailyCharge.getAddBuildAmt().compareTo(BigDecimal.valueOf(0)) > 0) {
                            productVO.setPrdAmt(sumDailyCharge.getAddBuildAmt());
                            productVO.setPrdBillAmt(sumDailyCharge.getAddBuildAmt());
                            billProducts.add(productVO);
                        }
                        break;
                    case ADD_WORKSPACE: // 추가 워크스페이스 금액이 존재 할때만 처리
                        if(sumDailyCharge.getAddWorkspaceAmt().compareTo(BigDecimal.valueOf(0)) > 0) {
                            productVO.setPrdAmt(sumDailyCharge.getAddWorkspaceAmt());
                            productVO.setPrdBillAmt(sumDailyCharge.getAddWorkspaceAmt());
                            billProducts.add(productVO);
                        }
                        break;
                    case CORE: // 금액이 존재 할때만 처리
                        if(sumDailyCharge.getCoreAmt().compareTo(BigDecimal.valueOf(0)) > 0) {
                            productVO.setPrdAmt(sumDailyCharge.getCoreAmt());
                            productVO.setPrdBillAmt(sumDailyCharge.getCoreAmt());
                            billProducts.add(productVO);
                        }
                        break;
                    case NODE: // 금액이 존재 할때만 처리
                        if(sumDailyCharge.getNodeAmt().compareTo(BigDecimal.valueOf(0)) > 0) {
                            productVO.setPrdAmt(sumDailyCharge.getNodeAmt());
                            productVO.setPrdBillAmt(sumDailyCharge.getNodeAmt());
                            billProducts.add(productVO);
                        }
                        break;
                }

            }

            // billProducts 설정
            if(CollectionUtils.isNotEmpty(billProducts)){

                // 금액 계산
                BigDecimal totalBillAmount = billProducts.stream().map(AccountBillProductVO::getPrdBillAmt).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal taxAmount = totalBillAmount.multiply(BigDecimal.valueOf(TAX_RATE/100.0)).setScale(2, BigDecimal.ROUND_HALF_UP);
                BigDecimal finalBillAmt = totalBillAmount.add(taxAmount);

                // 금액 셋팅
                accountBillVO.setBillAmt(totalBillAmount); // 청구금액
                accountBillVO.setAdditionalTax(taxAmount); // 부가세 금액
                accountBillVO.setFinalBillAmt(finalBillAmt); // 최종 청구 금액

                // 상품리스트 셋팅
                accountBillVO.setBillProducts(billProducts);

            }

            // 청구서 데이터 추가, 환율데이터는 저장하지 않는다.
            accountBillList.add(accountBillVO);
        }

        // 청구서 데이터 일괄 등록
        resultCount = accountBillService.addAccountBillList(accountBillList);

        return resultCount;
    }

    /**
     * 전체 grade 과금 기본정보 조회 및 gradeSeq 별 Map 생성 후 리턴
     *
     * @return
     * @throws Exception
     */
    private Map<Integer, List<GradePlanChargeBaseVO>> getGradePlanChargeBaseMap() throws Exception {
        IGradePlanChargeBaseMapper gradePlanChargeBaseDao = sqlSession.getMapper(IGradePlanChargeBaseMapper.class);

        Map<Integer, List<GradePlanChargeBaseVO>> gradePlanChargeBaseMap = new HashMap<>();

        // 전체 과금 기본정보 조회
        List<GradePlanChargeBaseVO> gradePlanChargeBaseList = gradePlanChargeBaseDao.getGradePlanChargeBases(null);

        for(GradePlanChargeBaseVO vo : gradePlanChargeBaseList){

            if (!gradePlanChargeBaseMap.containsKey(vo.getGradeSeq())) {
                gradePlanChargeBaseMap.put(vo.getGradeSeq(), new ArrayList<>());
            }

            gradePlanChargeBaseMap.get(vo.getGradeSeq()).add(vo);

        }

        return gradePlanChargeBaseMap;
    }

    /**
     * 클러스터 kubeconfig 권한 만료 처리
     *
     * @param request
     * @param paramDate
     * @return
     * @throws Exception
     */
    public int expireUserAccountForKubeconfig(HttpServletRequest request, String paramDate) throws Exception{

        IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);

        int resultCount = 0;

        String regionTimeZone = cocktailServiceProperties.getRegionTimeZone();
        DateTimeFormatter baseDayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        // 조회 날짜 결정, paramDate없을 때는 현재 날짜 기준으로 계산
        String baseDate = paramDate;
        if( baseDate == null || "NONE".equals(baseDate) ) { // 기준일자가 없을 경우는 현재시간 날짜를 기준으로 함, timeZone 값 사용
            baseDate = DateTime.now(DateTimeZone.forID(regionTimeZone)).minusDays(1).toString(baseDayFmt);
        }

        String expireDate = baseDayFmt.parseDateTime(baseDate).toString(baseDayFmt); // 기준일 구함

        // 만료할 user-account 조회
        List<UserClusterRoleIssueVO> expireTargets = userDao.getUserClusterRoleIssues(null, null, Lists.newArrayList(), null, IssueType.KUBECONFIG.getCode(), expireDate, null, true);

        if (CollectionUtils.isNotEmpty(expireTargets)) {

            /**  DB 삭제 처리 */
            expireTargets = userClusterRoleIssueService.removeUserClusterRoleIssues(null, null, IssueType.KUBECONFIG, expireTargets, HistoryState.EXPIRED, ContextHolder.exeContext().getUserSeq());
            log.info("## expire User Account For Kubeconfig remove baseDate[{}], expireTargets[{}]", baseDate, expireTargets.size());

            /**  Cluster-api 삭제 처리 요청 */
            // account별 user-account으로 map 셋팅하여 cluster-api를 accountSeq별로 호출하여 처리
            Map<Integer, List<UserClusterRoleIssueVO>> expireTargetMapByAccount = Maps.newHashMap();
            for (UserClusterRoleIssueVO issueRow : expireTargets) {
                if (MapUtils.getObject(expireTargetMapByAccount, issueRow.getAccountSeq(), null) == null) {
                    expireTargetMapByAccount.put(issueRow.getAccountSeq(), Lists.newArrayList());
                }
                expireTargetMapByAccount.get(issueRow.getAccountSeq()).add(issueRow);
            }
            // cluster-api에서 header를 SYSTEM, SYSUSER 권한만 허용하여 role만 변경하여 호출함.
            ContextHolder.exeContext().setUserRole("SYSTEM");
            for (Map.Entry<Integer, List<UserClusterRoleIssueVO>> entryIssueRow : expireTargetMapByAccount.entrySet()) {
                // cluster-api 권한 삭제 요청
                log.info("## expire User Account For Kubeconfig remove - call - clusterApi - baseDate[{}], accountSeq[{}], expireTargets[{}]", baseDate, entryIssueRow.getKey(), entryIssueRow.getValue().size());
                clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), ContextHolder.exeContext().getUserRole(), entryIssueRow.getKey(), null, IssueType.KUBECONFIG, null, null, entryIssueRow.getValue());
            }

            resultCount = expireTargets.size();
        }

        return resultCount;
    }

    /**
     * api token 만료 처리
     *
     * @param request
     * @param paramDate
     * @return
     * @throws Exception
     */
    public int expireApiToken(HttpServletRequest request, String paramDate) throws Exception{

        IOpenapiMapper dao = sqlSession.getMapper(IOpenapiMapper.class);

        int resultCount = 0;

        String regionTimeZone = cocktailServiceProperties.getRegionTimeZone();
        DateTimeFormatter baseDayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        // 조회 날짜 결정, paramDate없을 때는 현재 날짜 기준으로 계산
        String baseDate = paramDate;
        if( baseDate == null || "NONE".equals(baseDate) ) { // 기준일자가 없을 경우는 현재시간 날짜를 기준으로 함, timeZone 값 사용
            baseDate = DateTime.now(DateTimeZone.forID(regionTimeZone)).minusDays(1).toString(baseDayFmt);
        }

        String expireDate = baseDayFmt.parseDateTime(baseDate).toString(baseDayFmt); // 기준일 구함

        // 만료할 user-account 조회
        List<ApiTokenIssueVO> expireTargets = dao.getApiTokenIssues(null, null, null, false, expireDate);

        if (CollectionUtils.isNotEmpty(expireTargets)) {
            log.info("## Start api token expired baseDate[{}], expireTargets[{}]", expireDate, expireTargets.size());
            for (ApiTokenIssueVO targetRow : expireTargets) {
                // 1. 이력 먼저 저장
                // 토큰 발급 이력 등록
                ApiTokenIssueHistoryVO issueHis = new ApiTokenIssueHistoryVO();
                issueHis.setApiTokenIssueSeq(targetRow.getApiTokenIssueSeq());
                issueHis.setUpdateUserSeq(0);
                issueHis.setHistoryState(TokenState.EXPIRED.getCode());
                issueHis.setHistoryMessage(null);
                int result = dao.addApiTokenIssueHistory(issueHis);

                if (result > 0) {
                    // 토큰 권한 범위 이력 등록
                    ApiTokenPermissionsScopeHistoryVO scopeHis = new ApiTokenPermissionsScopeHistoryVO();
                    scopeHis.setApiTokenIssueHistorySeq(issueHis.getApiTokenIssueHistorySeq());
                    dao.addApiTokenPermissionsScopeHistory(scopeHis);
                }

                // 2. 토큰 삭제(회수)
                resultCount += dao.deleteApiTokenIssue(targetRow.getApiTokenIssueSeq(), targetRow.getAccountSeq());
            }
            log.info("## End api token expired baseDate[{}], expireTargets[{}]", expireDate, resultCount);

        }

        return resultCount;
    }

    /**
     * 칵테일 audit log 삭제 (기준일 - 보관 주기(월 단위) 이전 log)
     *
     * @param request
     * @param paramDate
     * @return
     * @throws Exception
     */
    public int deleteCocktailAuditLog(HttpServletRequest request, String paramDate) throws Exception{

        IAuditLogMapper aDao = sqlSession.getMapper(IAuditLogMapper.class);

        int resultCount = 0;

        String regionTimeZone = cocktailServiceProperties.getRegionTimeZone();
        DateTimeFormatter baseDayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        // 조회 날짜 결정, paramDate없을 때는 현재 날짜 기준으로 계산
        String baseDate = paramDate;
        if( baseDate == null || "NONE".equals(baseDate) ) { // 기준일자가 없을 경우는 현재시간 날짜를 기준으로 함, timeZone 값 사용
            baseDate = DateTime.now(DateTimeZone.forID(regionTimeZone)).minusMonths(cocktailServiceProperties.getAuditLogPeriodMonth()).toString(baseDayFmt);
        }

        String deleteBaseDate = baseDayFmt.parseDateTime(baseDate).toString(baseDayFmt); // 기준일 구함

        // 보관 주기 기준에 해당하는 건수 조회
        int targetCnt = aDao.getAuditLogCountForBatch(deleteBaseDate);

        if (targetCnt > 0) {

            log.info("## Start Audit Log delete baseDate[{}], expireTargets[{}]", deleteBaseDate, targetCnt);
            /**  DB 삭제 처리 */
            resultCount = aDao.deleteAuditLogForBatch(deleteBaseDate);
            log.info("## End Audit Log delete baseDate[{}], expireTargets[{}]", deleteBaseDate, resultCount);
        }

        return resultCount;
    }

    /**
     * audit_access_log 테이블의 partition drop 및 추가.</br>
     * 이 메서드는 배치를 통해 매일 실행되며, 내부적으로 매월 1일 & 보안모드 & audit_access_logs 테이블이 존재 할때만 파티션 처리 로직이 실행 된다.</br>
     * 파라메터로 값이 넘어오지 않으면, 현재 시간 (UTC 기준)으로 판단한다.
     *
     * @param paramDate
     * @return
     * @throws Exception
     */
    public int dropAndCreateAuditAccessLogPartition(String paramDate) throws Exception{

        IAuditAccessLogMapper aDao = sqlSession.getMapper(IAuditAccessLogMapper.class);

        int resultCount = 0;

        DateTimeFormatter baseMonthFmt = DateTimeFormat.forPattern("yyyyMM");
        DateTimeFormatter baseDayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        // 조회 날짜 결정, paramDate없을 때는 현재 날짜 기준으로 계산
        String baseDate = paramDate;
        if( baseDate == null || "NONE".equals(baseDate) ) { // 기준일자가 없을 경우는 현재시간 날짜를 기준으로 함, timeZone 값 사용
            baseDate = DateTime.now(DateTimeZone.UTC).toString(baseDayFmt);
        }

        /** 매월 1일 & 보안모드 & audit_access_logs 테이블이 존재 할때만 실행한다. */
        boolean firstDayValid = baseDate.matches("\\d{4}\\-\\d{2}-01"); // 매월 첫번째 날인지 계산
        ServiceMode serviceMode = ServiceMode.valueOf(cocktailServiceProperties.getMode());
        boolean tableExist = aDao.existAuditAccessLogsTable();

        if (firstDayValid && serviceMode == ServiceMode.SECURITY_ONLINE && tableExist){
            String dropBaseDate = baseDayFmt.parseDateTime(baseDate).minusMonths(cocktailServiceProperties.getAuditLogPeriodMonth()+1).toString(baseDayFmt);

            /** 기간 지난 데이터 해당하는 partition drop */
            // 삭제 파티션명 구함
            String dropPartitionName = "p_"+baseDayFmt.parseDateTime(dropBaseDate).toString(baseMonthFmt);

            // 삭제할 파티션 존재 하는지 조회
            boolean existPartition = aDao.existAuditAccessLogsPartition(dropPartitionName);

            if (existPartition) {
                log.info("## Start Audit Log delete baseDate[{}], dropBaseDate[{}], dropPartitionName[{}]", baseDate, dropBaseDate, dropPartitionName);
                resultCount = aDao.dropPartitionAuditAccessLogForBatch(dropPartitionName);
                log.info("## End Audit Log delete baseDate[{}], dropBaseDate[{}], dropPartitionName[{}]", baseDate, dropBaseDate, dropPartitionName);
            }

            /** 사용할 partition 생성, 날짜 기준 다음달, 다다음달 파티션 생성, 존재하면 처리 안함. */
            // 생성할 파티션이름 생성
            Map<String,String> createPartitionConditions = new HashMap();
            createPartitionConditions.put(baseDayFmt.parseDateTime(baseDate).plusMonths(1).toString(baseMonthFmt), baseDayFmt.parseDateTime(baseDate).plusMonths(2).toString(baseDayFmt));
            createPartitionConditions.put(baseDayFmt.parseDateTime(baseDate).plusMonths(2).toString(baseMonthFmt), baseDayFmt.parseDateTime(baseDate).plusMonths(3).toString(baseDayFmt));

            String partitionName;
            String partitionConditionDate;
            for (Map.Entry<String, String> partition : createPartitionConditions.entrySet()) {
                partitionName = "p_"+partition.getKey();
                partitionConditionDate = partition.getValue();

                existPartition = aDao.existAuditAccessLogsPartition(partitionName);

                // 존재하지 않으면, 파티션 생성
                if(!existPartition){
                    log.info("## Start Audit Log create partition baseDate[{}], dropBaseDate[{}], dropPartitionName[{}]", baseDate, dropBaseDate, dropPartitionName);
                    aDao.addPartitionToAuditAccessLogForBatch(partitionName, partitionConditionDate);
                    log.info("## End Audit Log create partition baseDate[{}], dropBaseDate[{}], dropPartitionName[{}]", baseDate, dropBaseDate, dropPartitionName);
                }
            }
        }

        return resultCount;
    }

    /**
     * api token audit log 삭제 (기준일 - 보관 주기(월 단위) 이전 log)
     *
     * @param request
     * @param paramDate
     * @return
     * @throws Exception
     */
    public int deleteApiTokenAuditLog(HttpServletRequest request, String paramDate) throws Exception{

        IOpenapiMapper aDao = sqlSession.getMapper(IOpenapiMapper.class);

        int resultCount = 0;

        String regionTimeZone = cocktailServiceProperties.getRegionTimeZone();
        DateTimeFormatter baseDayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        // 조회 날짜 결정, paramDate없을 때는 현재 날짜 기준으로 계산
        String baseDate = paramDate;
        if( baseDate == null || "NONE".equals(baseDate) ) { // 기준일자가 없을 경우는 현재시간 날짜를 기준으로 함, timeZone 값 사용
            baseDate = DateTime.now(DateTimeZone.forID(regionTimeZone)).minusMonths(cocktailServiceProperties.getAuditLogPeriodMonth()).toString(baseDayFmt);
        }

        String deleteBaseDate = baseDayFmt.parseDateTime(baseDate).toString(baseDayFmt); // 기준일 구함

        // 보관 주기 기준에 해당하는 건수 조회
        int targetCnt = aDao.getApiTokenAuditLogCountForBatch(deleteBaseDate);

        if (targetCnt > 0) {

            log.info("## Start API Token Audit Log delete baseDate[{}], expireTargets[{}]", deleteBaseDate, targetCnt);
            /**  DB 삭제 처리 */
            resultCount = aDao.deleteApiTokenAuditLogForBatch(deleteBaseDate);
            log.info("## End API Token Audit Log delete baseDate[{}], expireTargets[{}]", deleteBaseDate, resultCount);
        }

        return resultCount;
    }
}
