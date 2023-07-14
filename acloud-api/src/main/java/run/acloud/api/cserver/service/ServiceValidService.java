package run.acloud.api.cserver.service;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.build.dao.IBuildMapper;
import run.acloud.api.build.vo.BuildVO;
import run.acloud.api.configuration.enums.ServiceRegistryType;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.vo.ServicemapVO;
import run.acloud.commons.enums.CRUDCommand;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.vo.HarborUserRespVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io Created on 2017. 1. 10.
 */
@Slf4j
@Service
public class ServiceValidService {

	private final static String DEFAULT_APPMAP_GROUP_NAME = "Default";

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private HarborRegistryFactoryService harborRegistryFactory;

	@Autowired
	private RegistryPropertyService registryProperties;


	public boolean canDeleteServiceClusters(Integer serviceSeq, List<Integer> clusterSeqs, String serviceType) throws Exception {
		IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
		List<ServicemapVO> servicemaps = servicemapDao.getServicemapsByServiceClusters(serviceSeq, clusterSeqs, serviceType);
		
		if (CollectionUtils.isNotEmpty(servicemaps)) {
			String servicemapNames = servicemaps.stream()
				.map(ServicemapVO::getServicemapName)
				.collect(Collectors.joining(", "))
				;
			
			throw new CocktailException(
					String.format("해당 클러스터는 다음 서비스맵에서 사용하고 있습니다. [%s]", servicemapNames),
					ExceptionType.AppmapUseCluster);
		}
		
		return true;
	}

	public void checkParameter(ServiceAddVO serviceAdd) throws Exception {
		ExceptionMessageUtils.checkParameter("serviceName", serviceAdd.getServiceName(), 50, true);
//		if (CollectionUtils.isNotEmpty(serviceAdd.getServiceClusters())) {
//			for (ServiceClusterVO serviceClusterRow : serviceAdd.getServiceClusters()) {
//				if (serviceClusterRow.getClusterSeq() == null) {
//					throw new CocktailException("serviceCluster > clusterSeq is null.", ExceptionType.InvalidParameter_Empty);
//				}
//			}
//		}
	}

	public void checkWorkspaceNameUsed(ServiceAddVO serviceAdd) throws CocktailException {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		List<ServiceListVO> services = dao.getServices(serviceAdd.getAccountSeq(), null, null, "Y", null);
		long usedServiceNameCnt = services.stream().filter(svc -> StringUtils.equalsIgnoreCase(svc.getServiceName(), serviceAdd.getServiceName())).count();
		if (usedServiceNameCnt > 0){
			throw new CocktailException("Workspace name exists already.", ExceptionType.WorkspaceNameAlreadyExists);
		}
	}


	public boolean checkServiceRegistryUsed(Integer accountSeq, String name, boolean withHarbor) throws Exception {
		IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
		List<ServiceRegistryVO> currentServiceRegistries = dao.getServiceRegistryOfAccount(accountSeq, null, ServiceRegistryType.SERVICE.getCode(), null, ServiceType.PLATFORM.getCode());

		List<String> projectNames = Optional.ofNullable(currentServiceRegistries).orElseGet(() ->Lists.newArrayList()).stream().map(ServiceRegistryVO::getProjectName).collect(Collectors.toList());

		// ProjectName명 중복 체크 (DB)
		if (projectNames.contains(name)) {
			return true;
		}

		if (withHarbor) {
			// harbor api client
			IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(accountSeq);

			// projectName명 중복 체크 (Harbor)
			List<RegistryProjectVO> harborProjects = harborRegistryService.getProjectList(name);
			Optional<RegistryProjectVO> harborProjectsOptional = Optional.ofNullable(harborProjects).orElseGet(() -> Lists.newArrayList()).stream().filter(hp -> (StringUtils.equals(hp.getName(), name))).findFirst();
			if (harborProjectsOptional.isPresent()) {
				return true;
			}
		}

		return false;
	}
	/**
	 * 서비스 레지스트리 생성 / 수정 / 삭제 유효성 체크
	 * @param service
	 * @param receivedServiceRegistries
	 * @param currentServiceRegistries
	 * @param currentServiceRegistriesMap
	 */
	public void checkServiceRegistryCommonValidation(Integer accountSeq, ServiceDetailVO service, List<ServiceRegistryVO> receivedServiceRegistries, List<ServiceRegistryVO> currentServiceRegistries, Map<Integer, ServiceRegistryVO> currentServiceRegistriesMap) throws Exception {
		IHarborRegistryService harborRegistryService = null;
		List<String> projectNames = null;
		IBuildMapper buildDao = null;

		for(ServiceRegistryVO srRow : receivedServiceRegistries) {

			/** 생성이 아닐때 **/
			if (srRow.getModifyType() != CRUDCommand.C) {
				if (srRow.getProjectId() == null) {
					throw new CocktailException("The projectid does not exist in registry", ExceptionType.InvalidInputData, srRow.getProjectId());
				}

				if (!currentServiceRegistriesMap.containsKey(srRow.getProjectId())) {
					throw new CocktailException("The registry does not exist", ExceptionType.RegistryProjectNotFound, srRow.getProjectName());
				}
			}

			/** 삭제 일때 **/
			if (srRow.getModifyType() == CRUDCommand.D) {
				if(buildDao == null) {
					buildDao = sqlSession.getMapper(IBuildMapper.class);
				}

				List<BuildVO> builds = buildDao.getBuildList(service.getAccount().getAccountSeq(), null, Arrays.asList(srRow.getProjectId()), null, registryProperties.getUrl());

				if (CollectionUtils.isNotEmpty(builds)) {
					throw new CocktailException("레지스트리가 빌드를 포함하고 있어 삭제될 수 없습니다. 빌드를 먼저 삭제해 주세요.", ExceptionType.RegistryContainsBuild, srRow.getProjectName());
				}
			}

			/** 생성 일때 **/
			if (srRow.getModifyType() == CRUDCommand.C) {
				if(harborRegistryService == null) {
					// harbor api client
					harborRegistryService = harborRegistryFactory.getService(accountSeq);
				}

				// ProjectName명 유효성 체크
				ExceptionMessageUtils.checkParameter("projectName", srRow.getProjectName(), 255, true);

				if(projectNames == null) {
					projectNames = Optional.ofNullable(Optional.ofNullable(currentServiceRegistries).orElseGet(() ->Lists.newArrayList()).stream().map(ServiceRegistryVO::getProjectName).collect(Collectors.toList())).orElseGet(() ->Lists.newArrayList());
				}
				// ProjectName명 중복 체크 (DB)
				if (projectNames.contains(srRow.getProjectName())) {
					throw new CocktailException("Already exists registry project!!", ExceptionType.RegistryProjectAlreadyExists);
				}

				// projectName명 중복 체크 (Harbor)
				boolean withHarbor = false;
				if (withHarbor) {
					List<RegistryProjectVO> harborProjects = harborRegistryService.getProjectList(srRow.getProjectName());
					Optional<RegistryProjectVO> harborProjectsOptional = Optional.ofNullable(harborProjects).orElseGet(() -> Lists.newArrayList()).stream().filter(hp -> (StringUtils.equals(hp.getName(), srRow.getProjectName()))).findFirst();
					if (harborProjectsOptional.isPresent()) {
						throw new CocktailException("Already exists registry project!!", ExceptionType.RegistryProjectAlreadyExists);
					}
				}
			}

		}

		if(harborRegistryService == null) {
			harborRegistryService = harborRegistryFactory.getService(accountSeq);
		}

		HarborUserRespVO registryUser = harborRegistryService.getUser(CryptoUtils.decryptAES(service.getRegistryUserId()));
		if(registryUser == null) {
			throw new CocktailException("Could not found registry user", ExceptionType.RegistryUserNotFound, CryptoUtils.decryptAES(service.getRegistryUserId()));
		}
	}

}
