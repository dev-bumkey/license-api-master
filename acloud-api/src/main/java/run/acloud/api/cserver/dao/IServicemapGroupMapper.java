package run.acloud.api.cserver.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.cserver.vo.ServicemapGroupAddVO;
import run.acloud.api.cserver.vo.ServicemapGroupVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 2. 1.
 */
public interface IServicemapGroupMapper {
	ServicemapGroupVO getServicemapGroup(
			@Param("servicemapGroupSeq") Integer servicemapGroupSeq,
			@Param("serviceSeq") Integer serviceSeq
	);

	List<ServicemapGroupVO> getServicemapGroupsOfService(
			@Param("serviceSeq") Integer serviceSeq
	);

	// 제거 - 사용안함
//	List<AppmapGroupSummaryVO> getAppmapGroupSummaries(
//			@Param("serviceSeq") Integer serviceSeq
//	);

	Integer addServicemapGroup(ServicemapGroupAddVO servicemapGroupAdd);

	Integer updateServicemapGroup(ServicemapGroupVO servicemapGroup);

	Integer removeServicemapGroup(
			@Param("servicemapGroupSeq") Integer servicemapGroupSeq,
			@Param("updater") Integer updater
	);

	int hasServicemaps(
			@Param("servicemapGroupSeq") Integer servicemapGroupSeq
	);

	void updateServicemapGroupSortOrder(
            @Param("serviceSeq") Integer serviceSeq,
            @Param("fromOrder") Integer fromOrder,
            @Param("toOrder") Integer toOrder,
            @Param("increment") int increment,
			@Param("updater") Integer updater
	);

}
