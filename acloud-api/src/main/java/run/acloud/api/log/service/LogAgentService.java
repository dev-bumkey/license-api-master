package run.acloud.api.log.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1Affinity;
import io.kubernetes.client.openapi.models.V1Toleration;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.log.dao.IAddonLogAgentMapper;
import run.acloud.api.log.dao.ILogAgentMapper;
import run.acloud.api.log.util.LogAgentUtils;
import run.acloud.api.log.vo.LogAgentAccountMappingVO;
import run.acloud.api.log.vo.LogAgentVO;
import run.acloud.api.log.vo.LogAgentViewVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailLogAgentProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class LogAgentService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    private final LogAgentTokenService logAgentTokenService;
    private final LogAgentDeployService logAgentDeployService;
    private final ClusterStateService clusterStateService;
    private final CocktailLogAgentProperties cocktailLogAgentProperties;

    public LogAgentService(
            LogAgentTokenService logAgentTokenService,
            LogAgentDeployService logAgentDeployService,
            ClusterStateService clusterStateService,
            CocktailLogAgentProperties cocktailLogAgentProperties) {
        this.logAgentTokenService = logAgentTokenService;
        this.logAgentDeployService = logAgentDeployService;
        this.clusterStateService = clusterStateService;
        this.cocktailLogAgentProperties = cocktailLogAgentProperties;
    }

    public List<LogAgentVO> getLogAgentList() throws Exception {
        ILogAgentMapper logAgentDao = sqlSession.getMapper(ILogAgentMapper.class);
        return Optional.ofNullable(logAgentDao.getLogAgentList()).orElseGet(Lists::newArrayList);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addLogAgent(LogAgentViewVO logAgentView) throws Exception {
        ILogAgentMapper logAgentDao = sqlSession.getMapper(ILogAgentMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        // admin이 아닌경우 request body로 받은 accountSeq 대신 세션에 있는 accountSeq를 넣어준다.
        if (!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
            logAgentView.setAccountSeq(ContextHolder.exeContext().getUserAccountSeq());
        }

        // 파라메터 유효성 검사
        validLogAgentViewVO(logAgentView, false);

        LogAgentVO logAgent = LogAgentUtils.copyLogAgentVO(logAgentView);

        // 클러스터 상태 체크
        ClusterVO cluster = clusterDao.getCluster(logAgent.getClusterSeq());
        clusterStateService.checkClusterState(cluster);

        // logAgent에 클러스터 id, 암호화된 토큰, yaml값 추가
        logAgent.setClusterId(cluster.getClusterId());
        logAgent.setToken(CryptoUtils.encryptDefaultAES(logAgentTokenService.issueToken()));

        String deployConfigYAML = LogAgentUtils.logAgentAddVOToDeployConfigYAML(logAgent, cocktailLogAgentProperties);
        if (StringUtils.isNotBlank(deployConfigYAML)) {
            logAgent.setDeployConfig(deployConfigYAML);
        }

        // 로그 에이전트 차트 배포
        logAgentDeployService.installLogAgent(logAgent);

        // 배포 후 컨트롤러 이름이 logAgent에 세팅되면 DB에 저장한다.
        int resultCount = logAgentDao.addLogAgent(logAgent);
        if (resultCount != 1) {
            throw new CocktailException("Log Agent Create Failure", ExceptionType.CommonCreateFail, ExceptionBiz.LOG);
        }

        // logAgent <-> account 매핑 저장
        LogAgentAccountMappingVO agentAccountMapping = LogAgentUtils.getAgentAccountMapping(logAgent);
        resultCount = logAgentDao.addLogAgentAccountMapping(agentAccountMapping);
        if (resultCount != 1) {
            throw new CocktailException("Log Agent Account Mapping Create Failure", ExceptionType.CommonCreateFail, ExceptionBiz.LOG);
        }
    }

    private void validLogAgentViewVO(LogAgentViewVO view, boolean isEdit) throws Exception {
        if (view == null) return;

        JSON k8sJson = new JSON();
        if (!isEdit) {
            ExceptionMessageUtils.checkParameter("agentName", view.getAgentName(), 50, true);
            if (!view.getAgentName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("agent name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("agent name is invalid"));
            }
            ExceptionMessageUtils.checkParameterRequired("accountSeq", view.getAccountSeq());
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", view.getClusterSeq());
            ExceptionMessageUtils.checkParameter("namespace", view.getNamespace(), 256, true);
            if (!view.getNamespace().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("namespace is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("namespace is invalid"));
            }
        }

        ExceptionMessageUtils.checkParameter("agentDescription", view.getAgentDescription(), 256, false);
        ExceptionMessageUtils.checkParameter("applicationName", view.getApplicationName(), 50, true);
        if (!view.getApplicationName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("application name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("application is invalid"));
        }

        ExceptionMessageUtils.checkParameterRequired("cpuRequest", view.getCpuRequest());
        ExceptionMessageUtils.checkParameterRequired("cpuLimit", view.getCpuLimit());
        if (view.getCpuRequest() > view.getCpuLimit()) {
            String errMsg = "cpu request value cannot be greater than cpu limit value!!";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
        }
        ExceptionMessageUtils.checkParameterRequired("memoryRequest", view.getMemoryRequest());
        ExceptionMessageUtils.checkParameterRequired("memoryLimit", view.getMemoryLimit());
        if (view.getMemoryRequest() > view.getMemoryLimit()) {
            String errMsg = "memory request value cannot be greater than memory limit value!!";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
        }

        if (StringUtils.isNotBlank(view.getNodeSelector())) {
            try {
                // spec 체크
                LogAgentUtils.getYamlValue(view.getNodeSelector(), new TypeReference<Map<String, String>>(){}, k8sJson);
            } catch (Exception e) {
                String errMsg = String.format("Invalid node selector!! - %s", e.getMessage());
                throw new CocktailException(errMsg, e, ExceptionType.InvalidParameter, errMsg);
            }
        }
        if (StringUtils.isNotBlank(view.getTolerations())) {
            try {
                // spec 체크
                // k8s model로 체크
                LogAgentUtils.getYamlValue(view.getTolerations(), new TypeReference<List<V1Toleration>>(){}, k8sJson);
            } catch (Exception e) {
                String errMsg = String.format("Invalid Tolerations!! - %s", e.getMessage());
                throw new CocktailException(errMsg, e, ExceptionType.InvalidParameter, errMsg);
            }
        }
        if (StringUtils.isNotBlank(view.getAffinity())) {
            try {
                // spec 체크
                // k8s model로 체크
                LogAgentUtils.getYamlValue(view.getAffinity(), new TypeReference<V1Affinity>(){}, k8sJson);
            } catch (Exception e) {
                String errMsg = String.format("Invalid Affinity!! - %s", e.getMessage());
                throw new CocktailException(errMsg, e, ExceptionType.InvalidParameter, errMsg);
            }
        }

        ExceptionMessageUtils.checkParameterRequired("hostPath", view.getHostPath());
        ExceptionMessageUtils.checkParameterRequired("agentConfig", view.getAgentConfig());
    }

    public LogAgentVO getLogAgent(Integer agentSeq) throws Exception {
        ILogAgentMapper logAgentDao = sqlSession.getMapper(ILogAgentMapper.class);
        return Optional.ofNullable(logAgentDao.getLogAgent(agentSeq))
                .orElseThrow(() -> new CocktailException("Log Agent Inquire Failure", ExceptionType.CommonInquireFail, ExceptionBiz.LOG));
    }

    public LogAgentViewVO getLogAgentView(Integer agentSeq) throws Exception {
        return LogAgentUtils.deployConfigYamlToLogAgentVO(getLogAgent(agentSeq));
    }

    public void editLogAgent(Integer agentSeq, LogAgentViewVO logAgentView) throws Exception {

        validLogAgentViewVO(logAgentView, true);

        try {
            ILogAgentMapper logAgentDao = sqlSession.getMapper(ILogAgentMapper.class);
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            // 로그 에이전트 상세 조회
            LogAgentVO logAgent = getLogAgent(agentSeq);

            // 클러스터 상태 체크
            ClusterVO cluster = clusterDao.getCluster(logAgent.getClusterSeq());
            clusterStateService.checkClusterState(cluster);

            // 파라메터로 넘겨받은 값들을 로그 에이전트에 셋팅
            logAgent.setAgentDescription(logAgentView.getAgentDescription());
            logAgent.setAgentConfig(logAgentView.getAgentConfig());
            logAgent.setParserConfig(logAgentView.getParserConfig());
            logAgent.setApplicationName(logAgentView.getApplicationName());
            logAgent.setHostPath(logAgentView.getHostPath());
            logAgent.setLogLevel(logAgentView.getLogLevel());
            logAgent.setLokiCustomLabels(logAgentView.getLokiCustomLabels());
            logAgent.setCpuRequest(logAgentView.getCpuRequest());
            logAgent.setCpuLimit(logAgentView.getCpuLimit());
            logAgent.setMemoryRequest(logAgentView.getMemoryRequest());
            logAgent.setMemoryLimit(logAgentView.getMemoryLimit());
            logAgent.setAffinity(logAgentView.getAffinity());
            logAgent.setTolerations(logAgentView.getTolerations());
            logAgent.setNodeSelector(logAgentView.getNodeSelector());

            // 받은 정보를 토대로 yaml 데이터를 생성해서 값 추가
            String deployConfigYAML = LogAgentUtils.logAgentAddVOToDeployConfigYAML(logAgent, cocktailLogAgentProperties);
            if (StringUtils.isNotBlank(deployConfigYAML)) {
                logAgent.setDeployConfig(deployConfigYAML);
            }

            // 변경사항을 DB에 저장 후 릴리즈 업그레이드 작업
            int updateCount = logAgentDao.editLogAgent(logAgent);
            if (updateCount != 1) {
                throw new CocktailException("Log Agent Update Failure", ExceptionType.CommonUpdateFail, ExceptionBiz.LOG);
            }

            logAgentDeployService.updateLogAgent(logAgent);
        } catch(CocktailException e) {
            throw e;
        } catch(Exception e) {
            throw new CocktailException("Log Agent Update Failure", e, ExceptionType.CommonUpdateFail, ExceptionBiz.LOG);
        }
    }

    public LogAgentVO removeLogAgent(Integer agentSeq) throws Exception {
        try {
            ILogAgentMapper logAgentDao = sqlSession.getMapper(ILogAgentMapper.class);
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

            // 로그 에이전트 상세 조회
            LogAgentVO logAgent = getLogAgent(agentSeq);

            // 클러스터 상태 체크
            ClusterVO cluster = clusterDao.getCluster(logAgent.getClusterSeq());
            clusterStateService.checkClusterState(cluster);

            // DB에서 로그 에이전트를 제거하고 릴리즈 제거 작업
            int removeCount = logAgentDao.removeLogAgent(agentSeq);
            if (removeCount != 1) {
                throw new CocktailException("Log Agent Delete Failure", ExceptionType.CommonDeleteFail, ExceptionBiz.LOG);
            }

            logAgentDeployService.deleteLogAgent(logAgent);

            return logAgent;
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("Log Agent Delete Failure", e, ExceptionType.CommonDeleteFail, ExceptionBiz.LOG);
        }
    }

    public String addAddonLogAgentToken(String releaseName, ClusterVO cluster) throws Exception {
        IAddonLogAgentMapper addonLogAgentDao = sqlSession.getMapper(IAddonLogAgentMapper.class);

        // 추가하려는 애드온 로그 에이전트 토큰 정보가 이미 존재하는지 검사한다.
        Optional<Integer> addonLogAgentSeq = Optional.ofNullable(addonLogAgentDao.getAddonLogAgentSeq(releaseName, cluster));
        if (addonLogAgentSeq.isPresent()) throw new CocktailException("already has installed chart", ExceptionType.CommonCreateFail, ExceptionBiz.LOG);

        // 애드온 로그 에이전트 토큰 발급 작업 시작
        // 사용하지 않지만 필수값인 경우 addon으로 넣어줌
        String token = logAgentTokenService.issueToken();
        LogAgentVO logAgent = new LogAgentVO();
        logAgent.setAgentName(releaseName);
        logAgent.setDeployConfig("addon");
        logAgent.setClusterSeq(cluster.getClusterSeq());
        logAgent.setClusterId(cluster.getClusterId());
        logAgent.setNamespace(cluster.getNamespaceName());
        logAgent.setControllerName("addon");
        logAgent.setApplicationName("addon");
        logAgent.setToken(CryptoUtils.encryptDefaultAES(token));
        logAgent.setAddonLogAgentYN("Y");

        // DB에 토큰 발급 정보를 저장한다.
        int addCount = addonLogAgentDao.addAddonLogAgent(logAgent);
        if (addCount != 1) {
            throw new CocktailException("Log Agent Create Failure", ExceptionType.CommonCreateFail, ExceptionBiz.LOG);
        }

        return token;
    }

    public String getAddonLogAgentToken(String releaseName, ClusterVO cluster) throws Exception {
        IAddonLogAgentMapper addonLogAgentDao = sqlSession.getMapper(IAddonLogAgentMapper.class);
        Optional<String> addonLogAgentToken = Optional.ofNullable(addonLogAgentDao.getAddonLogAgentToken(releaseName, cluster));

        return CryptoUtils.decryptDefaultAES(addonLogAgentToken
                .orElseThrow(() -> new CocktailException("Addon Log Agent Token Inquire Failure", ExceptionType.CommonInquireFail, ExceptionBiz.LOG)));
    }

    public void removeAddonLogAgentToken(String releaseName, ClusterVO cluster) throws Exception {
        IAddonLogAgentMapper addonLogAgentDao = sqlSession.getMapper(IAddonLogAgentMapper.class);
        // 애드온 로그 에이전트 토큰 정보가 존재하는지 확인
        Integer addonLogAgentSeq = Optional.ofNullable(addonLogAgentDao.getAddonLogAgentSeq(releaseName, cluster))
                .orElseThrow(() -> new CocktailException("Addon Log Agent Inquire Failure", ExceptionType.CommonDeleteFail, ExceptionBiz.LOG));

        // DB에서 토큰 발급 정보를 제거한다.
        int deleteCount = addonLogAgentDao.removeAddonLogAgent(addonLogAgentSeq);
        if (deleteCount != 1) {
            throw new CocktailException("Addon Log Agent Delete Failure", ExceptionType.CommonDeleteFail, ExceptionBiz.LOG);
        }
    }

    public List<String> getTokenList() throws Exception {
        ILogAgentMapper logAgentDao = sqlSession.getMapper(ILogAgentMapper.class);
        // 로그 에이전트, 애드온 로그 에이전트 토큰 정보를 복호화해서 반환
        return Optional.ofNullable(logAgentDao.getTokenList()).orElseGet(Lists::newArrayList).stream()
                .map(CryptoUtils::decryptDefaultAES)
                .toList();
    }

    public List<String> getApplicationList() throws Exception {
        ILogAgentMapper logAgentDao = sqlSession.getMapper(ILogAgentMapper.class);
        return Optional.ofNullable(logAgentDao.getApplicationList()).orElseGet(Lists::newArrayList);
    }
}
