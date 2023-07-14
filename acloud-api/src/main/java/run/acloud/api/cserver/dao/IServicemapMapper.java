package run.acloud.api.cserver.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.monitoring.vo.ServiceMonitoringVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 1. 12.
 */
public interface IServicemapMapper {
	List<ServicemapVO> getServicemapByClusterForRef(@Param("clusterSeq") Integer clusterSeq);
	List<ServicemapDetailVO> getServicemapDetailByClusterForRef(@Param("clusterSeq") Integer clusterSeq);

	/**
	 * 서비스맵 조회
	 *
	 * @param servicemapSeq - required
	 * @param serviceSeq - optional, 단일 서비스 정보만 조회시 사용
	 * @return
	 */
	ServicemapVO getServicemap(@Param("servicemapSeq") Integer servicemapSeq, @Param("serviceSeq") Integer serviceSeq);

	List<ServicemapVO> getServicemapsBySeqs(@Param("servicemapSeqs") List<Integer> servicemapSeqs);

	ServicemapDetailVO getServicemapDetail(@Param("servicemapSeq") Integer servicemapSeq, @Param("serviceSeq") Integer serviceSeq);

	ServicemapVO getServicemapByClusterAndName(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName);

	List<ServicemapVO> getServicemapsByServiceClusters(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("clusterSeqs") List<Integer> clusterSeqs,
			@Param("serviceType") String serviceType
	);

	List<ServicemapDetailResourceVO> getServicemapsByServiceClustersForResource(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("clusterSeqs") List<Integer> clusterSeqs,
			@Param("serviceType") String serviceType
	);

	List<ServicemapSummaryVO> getServicemapSummaries(
			@Param("accountSeq") Integer accountSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("serviceSeqs") List<Integer> serviceSeqs,
			@Param("servicemapSeq") Integer servicemapSeq
	);

	ServicemapSummaryVO getServicemapSummary(
			@Param("clusterSeq") Integer clusterSeq,
			@Param("namespaceName") String namespaceName,
			@Param("servicemapSeq") Integer servicemapSeq
	);

	// 제거
	// getServicemapSummaries 로 대체 clusterSeq
//	List<AppmapSummaryVO> getAppmapRelationSummaries(
//		@Param("clusterSeq") Integer clusterSeq,
//		@Param("serviceSeq") Integer serviceSeq,
//		@Param("useYn") String useYn);

	int addServicemap(ServicemapAddVO servicemapAdd);
	
	int addServiceServicemapMapping(ServicemapMappingVO servicemapMapping);

	int addServiceServicemapMappings(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("servicemapSeqs") List<Integer> servicemapSeqs,
			@Param("creator") Integer creator
	);

	// 신규
	int addServicemapgroupServicemapMapping(ServicemapGroupMappingVO servicemapGroupMapping);

	int deleteServiceServicemapMapping(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("servicemapSeq") Integer servicemapSeq
	);

	int deleteServiceServicemapMappings(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("servicemapSeqs") List<Integer> servicemapSeqs
	);

	// 신규
	int deleteServicemapgroupServicemapMapping(
			@Param("servicemapGroupSeq") Integer servicemapGroupSeq,
			@Param("servicemapSeq") Integer servicemapSeq
	);

	int deleteServicemapgroupServicemapMappings(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("servicemapSeqs") List<Integer> servicemapSeqs
	);

	int removeServicemap(ServicemapVO servicemap);
	
	int updateServicemap(ServicemapVO servicemap);

	// 제거
	// updateServicemap 로 대체 (서비스맵명만 수정)
//	int renameAppmap(AppmapVO appmap);

	// 제거
	// 서비스맵 이동 기능 제거
//	int moveAppmapToTargetService(
//			@Param("appmapSeq") Integer appmapSeq,
//			@Param("targetServiceSeq") Integer targetServiceSeq,
//			@Param("appmapGroupSeq") Integer appmapGroupSeq,
//			@Param("updater") Integer updater
//	);

	void updateServicemapInitSortOrder(
			@Param("servicemapGroupSeq") Integer servicemapGroupSeq
	);

	void updateServicemapSortOrder(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("servicemapGroupSeq") Integer servicemapGroupSeq,
			@Param("fromOrder") Integer fromOrder,
			@Param("toOrder") Integer toOrder,
			@Param("increment") int increment
	);

	int updateServicemapSelfSortOrder(
			@Param("nextServicemapGroupSeq") Integer nextServicemapGroupSeq,
			@Param("servicemapGroupMapping") ServicemapGroupMappingVO servicemapGroupMapping
	);

	int updateServicemapGroupChange(
			@Param("serviceSeq") Integer serviceSeq,
			@Param("servicemapSeq") Integer servicemapSeq,
			@Param("beforeServicemapGroupSeq") Integer beforeServicemapGroupSeq,
			@Param("afterServicemapGroupSeq") Integer afterServicemapGroupSeq,
			@Param("updater") Integer updater
	);

	String getNamespaceName(Integer appmapSeq);

	Integer getServicemapSeqByNamespace(
		@Param("clusterSeq") Integer clusterSeq,
		@Param("namespaceName") String namespaceName
	);

//	AppmapVO getAppmapInfoList(
//			@Param("serviceSeq") Integer serviceSeq,
//			@Param("appmapSeq") Integer appmapSeq,
//			@Param("clusterSeq") Integer clusterSeq,
//			@Param("jobType") String jobType
//	);

	Integer getClusterSeq(@Param("servicemapSeq") Integer servicemapSeq);

//	 제거
//	 서비스맵 이동 기능 제거
//	Integer getServiceSeq(@Param("appmapSeq") Integer appmapSeq);

	List<Integer> getServicemapSeqs(
		@Param("serviceSeq") Integer serviceSeq,
		@Param("clusterSeq") Integer clusterSeq
	);

	int updateServiceAppmapClusterToTargetService(
			@Param("sourceServiceSeq") Integer sourceServiceSeq,
			@Param("appmapSeq") Integer appmapSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("targetServiceSeq") Integer targetServiceSeq
	);

	// 제거
	// 서비스맵 이동 기능 제거

	// 제거
	// 서비스맵 이동 기능 제거
//	List<Integer> getAppmapSeqsOfCluster(@Param("clusterSeq") Integer clusterSeq);

	List<String> getNamespaceListOfCluster(@Param("clusterSeq") Integer clusterSeq);

	List<ServiceMonitoringVO> getServicemapListOfService(@Param("serviceSeq") Integer serviceSeq);

	int removeComponentsByServicemap(
			@Param("servicemapSeq") Integer servicemapSeq,
			@Param("updater") Integer updater
	);
	int removeWorkloadGroupsByServicemap(
			@Param("servicemapSeq") Integer servicemapSeq,
			@Param("updater") Integer updater
	);
	int removePipelineRunByServicemap(
			@Param("servicemapSeq") Integer servicemapSeq,
			@Param("updater") Integer updater
	);
	int removePipelineContainerByServicemap(
			@Param("servicemapSeq") Integer servicemapSeq,
			@Param("updater") Integer updater
	);
	int removePipelineWorkloadByServicemap(
			@Param("servicemapSeq") Integer servicemapSeq,
			@Param("updater") Integer updater
	);

}
