
package run.acloud.api.resource.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import run.acloud.api.resource.vo.ComponentDetailsVO;
import run.acloud.api.resource.vo.ComponentFilterVO;
import run.acloud.api.resource.vo.ComponentVO;

import java.util.List;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 24.
 */
@Repository
public interface IComponentMapper {
	ComponentVO getComponent(Integer componentSeq);

	ComponentVO getComponentByClusterAndNames(
			@Param("clusterSeq") Integer clusterSeq,
			@Param("namespaceName") String namespaceName,
			@Param("componentName") String componentName
	);

	List<ComponentVO> getComponentsInAppmapByClusterAndNames(
			@Param("clusterSeq") Integer clusterSeq,
			@Param("namespaceName") String namespaceName,
			@Param("componentName") String componentName
	);

	List<ComponentDetailsVO> getComponentDetails(ComponentFilterVO componentFilter);

	List<ComponentDetailsVO> getServerDetails(ComponentFilterVO componentFilter);

	int addComponent(ComponentVO component);
	
	int updateComponent(ComponentVO component);

	int updateComponentState(ComponentVO component);

	int removeComponent(ComponentVO component);
	
	int updateComponentSortOrder(
			@Param("workloadGroupSeq") Integer workloadGroupSeq,
			@Param("fromOrder") Integer fromOrder,
			@Param("toOrder") Integer toOrder,
			@Param("increment") int increment);

	int updateComponentInitSortOrder(
			@Param("workloadGroupSeq") Integer workloadGroupSeq
	);

	int updateComponentSelfSortOrder(ComponentVO component);

	List<ComponentVO> getComponentsInServicemapByName(@Param("name") String name,
												  @Param("servicemapSeq") Integer servicemapSeq);

	List<ComponentVO> getComponentListInClusterByName(@Param("name") String name,
	                                               @Param("clusterSeq") Integer clusterSeq);

	int updateComponentManifestAndGroupByNamespace(
		@Param("clusterSeq") Integer clusterSeq,
		@Param("namespaceName") String namespaceName,
		@Param("workloadGroupSeq") Integer workloadGroupSeq,
		@Param("componentName") String componentName,
		@Param("workloadManifest") String workloadManifest,
		@Param("updater") Integer updater
	);

	int updateComponentManifestByNamespace(
			@Param("clusterSeq") Integer clusterSeq,
			@Param("namespaceName") String namespaceName,
			@Param("componentName") String componentName,
			@Param("workloadManifest") String workloadManifest,
			@Param("updater") Integer updater
	);

	int updateComponentManiDescGrpByNamespace(
			@Param("clusterSeq") Integer clusterSeq,
			@Param("namespaceName") String namespaceName,
			@Param("componentName") String componentName,
			@Param("workloadGroupSeq") Integer workloadGroupSeq,
			@Param("workloadManifest") String workloadManifest,
			@Param("description") String description,
			@Param("updater") Integer updater
	);

}
