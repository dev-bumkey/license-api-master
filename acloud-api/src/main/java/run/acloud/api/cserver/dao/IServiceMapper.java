package run.acloud.api.cserver.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.api.cserver.vo.ServiceSummaryVO;
import run.acloud.api.cserver.vo.ServiceUserVO;

import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 1. 10.
 */
@Repository
public interface IServiceMapper {
	List<ServiceSummaryVO> getServiceSummaries(Map<String, Object> parameters);
	
	int addService(ServiceAddVO service);
	
	int updateService(ServiceAddVO service);
	
	int removeService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("updater") Integer updater);
	int deleteService(@Param("serviceSeq") Integer serviceSeq);

	List<ServiceListVO> getServices(
            @Param("accountSeq") Integer accountSeq,
            @Param("userSeq") Integer userSeq,
            @Param("userRole") String userRole,
			@Param("useYn") String useYn,
            @Param("projectId") Integer projectId
	);

	List<ServiceCountVO> getServicesInRegistry(
		@Param("accountSeq") Integer accountSeq,
		@Param("useYn") String useYn,
		@Param("projectId") Integer projectId
	);

	ServiceDetailVO getService(
			@Param("serviceSeq") Integer serviceSeq
	);

	List<UserVO> getServiceUsersForRef(
			@Param("serviceSeq") Integer serviceSeq
	);

	ServiceDetailVO getServiceByType(
		@Param("accountSeq") Integer accountSeq,
		@Param("serviceSeq") Integer serviceSeq,
		@Param("serviceType") String serviceType,
		@Param("useYn") String useYn
	);
	List<ServiceDetailVO> getServicesByType(
		@Param("accountSeq") Integer accountSeq,
		@Param("serviceSeq") Integer serviceSeq,
		@Param("serviceType") String serviceType,
		@Param("useYn") String useYn
	);

	int getServicesCountByType (
		@Param("accountSeq") Integer accountSeq,
		@Param("serviceSeq") Integer serviceSeq,
		@Param("serviceType") String serviceType
	);

	List<Integer> getServiceUserSeqsForRef(
			@Param("serviceSeq") Integer serviceSeq
	);

	List<ServiceUserVO> getServiceUsers(
			@Param("serviceSeq") Integer serviceSeq
	);

	List<Integer> getUserSeqsOfService(Integer serviceSeq);

	int deleteUsersOfService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("userSeqs") List<Integer> userSeqs) throws Exception;

	int deleteUserOfService(
		@Param("userSeq") Integer userSeq) throws Exception;

	int addUsersOfService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("serviceUsers") List<ServiceUserVO> serviceUsers,
			@Param("creator") Integer creator
	) throws Exception;

	int updateUsersOfService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("userSeq") Integer userSeq,
			@Param("userGrant") String userGrant,
			@Param("updater") Integer updater
	) throws Exception;

	List<Integer> getClusterSeqsOfService(Integer serviceSeq);

	List<ServiceClusterVO> getClusterOfService(
			@Param("serviceSeq") Integer serviceSeq
	);

	List<ServiceClusterVO> getServiceCluster(
		@Param("serviceSeq") Integer serviceSeq,
		@Param("clusterSeq") Integer clusterSeq
	);

	List<Integer> getServiceSeqByCluster(
		@Param("clusterSeq") Integer clusterSeq,
		@Param("clusterTenancy") String clusterTenancy
	);

	int deleteServiceCluster(
		@Param("clusterSeq") Integer clusterSeq,
		@Param("serviceSeq") Integer serviceSeq
		);

	void deleteClustersOfService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("clusterSeqs") List<Integer> clusterSeqs);

	void deleteBuildserversOfService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("buildserverSeqs") List<Integer> buildserverSeqs);

	int addClustersOfService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("serviceClusters") List<ServiceClusterVO> serviceClusters,
			@Param("creator") Integer creator
	);

	int addClustersOfServiceV2(
			@Param("serviceClusters") List<ServiceClusterVO> serviceClusters,
			@Param("creator") Integer creator
	);

	int addClustersOfServiceV3(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("clusterSeqs") List<Integer> clusterSeqs,
			@Param("creator") Integer creator
	);

	int addBuildserversOfService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("buildserverSeqs") List<Integer> buildserverSeqs,
			@Param("creator") Integer creator
	);

	List<Integer> getProjectIdsOfService(@Param("serviceSeq") Integer serviceSeq);

	List<Integer> getProjectIdsOfAccount(@Param("accountSeq") Integer accountSeq);

	List<Integer> getServiceSeqsOfProject( @Param("accountSeq") Integer accountSeq, @Param("projectId") Integer projectId );

	int deleteProjectsOfService(
            @Param("serviceSeq") Integer serviceSeq,
            @Param("projectIds") List<Integer> projectIds) throws Exception;

    int addProjectsOfService(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("projects") List<ServiceRegistryVO> projects,
            @Param("creator") Integer creator
	);

    int updateServiceRegistryName(
            ServiceRegistryVO project
	);

    int updateServiceRegistryType(
            ServiceRegistryVO project
	);

	int updateServiceRegistryDescription(
			ServiceRegistryVO project
	);

    int deleteUnusedServiceRegistry();

    List<Integer> getProjectsOfUser(@Param("userSeq") Integer userSeq) throws Exception;

    int changeRegistryUserPassword(ServiceAddVO service);
    int updateRegistryUser(ServiceAddVO service);

	List<ServiceRegistryUserVO> getRegistryUserByNamespace(
			@Param("clusterSeq") Integer clusterSeq,
			@Param("namespaceName") String namespaceName
	);

    int getProjectOfServiceCount(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("projectId") Integer projectId);

	int addAccountServiceMapping(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq,
			@Param("creator") Integer creator
	);
	int deleteAccountServiceMapping(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq
	);

    List<Integer> getServiceSeqsOfUser(Integer userSeq) throws Exception;

	List<Integer> getServiceSeqsOfSystem(Integer userSeq) throws Exception;

	List<ServiceDetailVO> getServiceOfAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("projectType") String projectType
	) throws Exception;

	List<String> getRegistryUserIds();

	int getAccessibleResourcesCount(Map<String, Object> params);

	int getAccessibleServicesCount(Map<String, Object> params);

	ServiceRelationVO getServiceRelation(Map<String, Object> params);

	Integer getServiceSeqByProjectId(@Param("accountSeq") Integer accountSeq, @Param("projectId") Integer projectId);

	ServiceRegistryVO getServiceRegistry(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("projectType") String projectType,
			@Param("projectId") Integer projectId,
			@Param("projectName") String projectName
	) throws Exception;

	List<ServiceRegistryVO> getServiceRegistries(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("projectId") Integer projectId,
			@Param("projectType") String projectType,
			@Param("projectName") String projectName
	) throws Exception;

	List<ServiceRegistryDetailVO> getServiceRegistriesWithPipelineFlow(
		@Param("serviceSeq") Integer serviceSeq
	) throws Exception;

	List<ServiceRegistryVO> getServiceRegistryOfAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq,
			@Param("projectType") String projectType,
			@Param("projectId") Integer projectId,
			@Param("serviceType") String serviceType
			);

	List<ServiceServicempGroupListVO> getServicesWithServicemapGroupByCluster(
			@Param("clusterSeq") Integer clusterSeq
	) throws Exception;

	int removeComponentsByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType,
			@Param("updater") Integer updater
	) throws Exception;

	int removeWorkloadGroupsByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType,
			@Param("updater") Integer updater
	) throws Exception;

	int deleteServicemapGroupsMappingByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType
	) throws Exception;

	int removeServicemapGroupsByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType,
			@Param("updater") Integer updater
	) throws Exception;

	int deleteServicemapGroupsByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType
	) throws Exception;

	int removeServicemapsByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType,
			@Param("updater") Integer updater
	) throws Exception;

	int removePipelineRunByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType,
			@Param("updater") Integer updater
	) throws Exception;

	int removePipelineContainerByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType,
			@Param("updater") Integer updater
	) throws Exception;

	int removePipelineWorkloadByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType,
			@Param("updater") Integer updater
	) throws Exception;

	int deleteServiceServicemapMappingByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType,
			@Param("updater") Integer updater
	) throws Exception;

	int deleteServiceProjectsByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceType") String serviceType
	);

	int deleteServicemapGroupsMappingByService(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq
	);

	int deleteServicemapGroupsByService(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq
	);

	int deleteServiceServicemapMappingByService(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq
	);

}
