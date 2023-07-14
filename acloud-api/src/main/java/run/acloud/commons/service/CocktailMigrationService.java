package run.acloud.commons.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.audit.dao.IAuditAccessLogMapper;
import run.acloud.api.build.service.PipelineBuildRunService;
import run.acloud.api.build.service.PipelineBuildService;
import run.acloud.api.catalog.dao.ITemplateMapper;
import run.acloud.api.catalog.service.TemplateService;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.dao.IProviderAccountMapper;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.api.configuration.vo.ClusterDetailConditionVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.service.ServerConversionService;
import run.acloud.api.cserver.service.ServerService;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.vo.ServicemapDetailVO;
import run.acloud.api.resource.dao.IComponentMapper;
import run.acloud.api.resource.enums.SecretType;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.service.SecretService;
import run.acloud.api.resource.service.StorageClassService;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.vo.DockerRegistrySecretVO;
import run.acloud.commons.client.HarborApiClient;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.context.ContextHolder;

import java.util.List;

@Slf4j
@Service
public class CocktailMigrationService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate cocktailSqlSession;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private HarborRegistryFactoryService harborRegistryFactory;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    @Autowired
    private PipelineBuildService pipelineBuildService;

    @Autowired
    private PipelineBuildRunService pipelineBuildRunService;

    @Autowired
    private StorageClassService storageClassService;

    @Autowired
    private HarborApiClient harborApiClient;

    @Autowired
    private RegistryPropertyService registryProperties;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ServerConversionService serverConversionService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private SecretService secretService;


    @Transactional(transactionManager = "transactionManager")
    public void migrationOfCocktail(String version) throws Exception {
        IClusterMapper clusterDao = cocktailSqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = cocktailSqlSession.getMapper(IServiceMapper.class);
        IProviderAccountMapper providerAccountDao = cocktailSqlSession.getMapper(IProviderAccountMapper.class);
        ITemplateMapper templateDao = cocktailSqlSession.getMapper(ITemplateMapper.class);
        IAccountMapper accountDao = cocktailSqlSession.getMapper(IAccountMapper.class);
        IComponentMapper componentDao = cocktailSqlSession.getMapper(IComponentMapper.class);


        if (StringUtils.equals("4.6.7", version)) {
            /**
             * #########################################################################################################################################################
             * create registry pull user of platform
             * #########################################################################################################################################################
             * hjchoi, 2022.03.21 - init
             */
            List<AccountVO> accounts = accountDao.getAccounts(null);

            if (CollectionUtils.isNotEmpty(accounts)) {
                String registryPullUserId, registryPullUserPassword;
                for (AccountVO aRow : accounts) {
                    /**
                     * 플랫폼 레지스트리 pull 전용 사용자 생성
                     * cf) 사용자가 없을 시에만 생성
                     */
                    accountService.createAccountRegistryPullUser(aRow.getAccountSeq());

                    AccountVO account = accountDao.getAccount(aRow.getAccountSeq());

                    registryPullUserId = CryptoUtils.decryptAES(account.getRegistryDownloadUserId());
                    registryPullUserPassword = CryptoUtils.decryptAES(account.getRegistryDownloadUserPassword());

                    List<ClusterDetailConditionVO> clusters = clusterService.getClusterCondition(aRow.getAccountSeq(), null, false, false, false, false, false, true, false, false, true, ContextHolder.exeContext());

                    if (CollectionUtils.isNotEmpty(clusters)) {
                        for (ClusterDetailConditionVO cluster : clusters) {
                            if (clusterStateService.isClusterRunning(cluster)) {
                                if (CollectionUtils.isNotEmpty(cluster.getServicemaps())) {
                                    for (ServicemapDetailVO smdRow : cluster.getServicemaps()) {
                                        if (smdRow.getK8sResourceExists()) {
                                            // secret이 없을 시에만 생성
                                            if (secretService.getSecret(cluster, smdRow.getNamespaceName(), registryPullUserId) == null) {
                                                DockerRegistrySecretVO secret = new DockerRegistrySecretVO();
                                                secret.setMakeCocktail(false);
                                                secret.setType(SecretType.DockerRegistry);
                                                secret.setName(registryPullUserId);
                                                secret.setUserName(registryPullUserId);
                                                secret.setPassword(registryPullUserPassword);
                                                secret.setEmail(String.format("%s@%s", registryPullUserId, CommonConstants.DEFAULT_USER_DOMAIN));
                                                secret.setServerUrl(registryProperties.getUrl(account.getAccountSeq()));
                                                secretService.createDockerRegistrySecret(cluster, smdRow.getNamespaceName(), secret);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * audit_logs -> audit_access_logs 로 마이그레이션
     *
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void migrationAuditLogsToAuditAccessLog() throws Exception {
        IAuditAccessLogMapper auditAccessLogDao = cocktailSqlSession.getMapper(IAuditAccessLogMapper.class);
        int maxSeq = auditAccessLogDao.getMaxSeq();

        // 데이터가 없을때만 migration
        if (maxSeq == 0) {
            auditAccessLogDao.migrationAuditLogsToAuditAccessLog();
        }
    }
}
