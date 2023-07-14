package run.acloud.api.cserver.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.cserver.vo.WorkloadGroupAddVO;
import run.acloud.api.cserver.vo.WorkloadGroupVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 2. 1.
 */
public interface IWorkloadGroupMapper {
	WorkloadGroupVO getWorkloadGroup(@Param("workloadGroupSeq") Integer workloadGroupSeq);

	List<WorkloadGroupVO> getWorkloadGroupsOfServicemap(@Param("servicemapSeq") Integer servicemapSeq);

	List<WorkloadGroupVO> getWorkloadGroupsOfNamespace(
		@Param("clusterSeq") Integer clusterSeq,
		@Param("namespaceName") String namespaceName
	);

	Integer addWorkloadGroup(WorkloadGroupAddVO workloadGroupAdd);
	
	Integer updateWorkloadGroup(WorkloadGroupVO workloadGroup);

	Integer removeWorkloadGroup(@Param("workloadGroupSeq") Integer workloadGroupSeq);
	
	void updateWorkloadGroupSortOrder(
			@Param("servicemapSeq") Integer servicemapSeq,
			@Param("fromOrder") Integer fromOrder,
			@Param("toOrder") Integer toOrder,
			@Param("increment") int increment);

	String getNamespaceOfWorkloadGroup(@Param("workloadGroupSeq") Integer workloadGroupSeq);
}
