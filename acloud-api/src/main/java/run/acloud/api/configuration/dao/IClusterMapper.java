package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.configuration.enums.ClusterState;
import run.acloud.api.configuration.vo.ClusterAddVO;
import run.acloud.api.configuration.vo.ClusterDetailConditionVO;
import run.acloud.api.configuration.vo.ClusterDetailVO;
import run.acloud.api.configuration.vo.ClusterVO;

import java.util.List;

/**
 * @author wschoi@acornsoft.io on 2017. 5. 19.
 */
public interface IClusterMapper {

    // @TODO servicemap에 맞게 변경
    ClusterVO getClusterByServicemap(@Param("servicemapSeq") Integer servicemapSeq);

    ClusterVO getClusterByNamespace(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("namespaceName") String namespaceName
    );

    int addCluster(ClusterAddVO cluster);
    
    List<ClusterVO> getClusters(
            @Param("accountSeq") Integer accountSeq,
            @Param("serviceSeq") Integer serviceSeq,
            @Param("clusterSeq") Integer clusterSeq,
            @Param("serviceSeqs") List<Integer> serviceSeqs,
            @Param("clusterSeqs") List<Integer> clusterSeqs,
            @Param("useYn") String useYn
    );
    List<ClusterVO> getClustersWithoutAuth(
            @Param("accountSeq") Integer accountSeq,
            @Param("serviceSeq") Integer serviceSeq,
            @Param("clusterSeq") Integer clusterSeq,
            @Param("serviceSeqs") List<Integer> serviceSeqs,
            @Param("clusterSeqs") List<Integer> clusterSeqs,
            @Param("useYn") String useYn
    );

    List<ClusterVO> getAssignableClustersOfAccountForTenancy(
            @Param("accountSeq") Integer accountSeq,
            @Param("serviceSeq") Integer serviceSeq,
            @Param("clusterTenancy") String clusterTenancy
    );

    ClusterVO getCluster(
            @Param("clusterSeq") Integer clusterSeq
    );

    ClusterVO getClusterWithoutAuth(
            @Param("clusterSeq") Integer clusterSeq
    );

    ClusterDetailVO getClusterDetail(
            @Param("clusterSeq") Integer clusterSeq
    );

    ClusterVO getClusterByUseYn(
        @Param("clusterSeq") Integer clusterSeq,
        @Param("useYn") String useYn);

    ClusterVO getClusterByClusterId(
            @Param("clusterId") String clusterId,
            @Param("useYn") String useYn);

    int hasComponents(@Param("clusterSeq") Integer clusterSeq);
    
    int updateCluster(ClusterAddVO cluster);

    int updateClusterForSecurity(ClusterAddVO cluster);

    int updateClusterState(ClusterAddVO cluster);
    int updateClusterVersion(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("k8sVersion") String k8sVersion,
            @Param("updater") Integer updater);

    int removeClusterEmpty(
    		@Param("clusterSeq") Integer clusterSeq,
    		@Param("clusterState") ClusterState clusterState,
    		@Param("updater") Integer updater);


    int deleteCluster(@Param("clusterSeq") Integer clusterSeq);

    List<ClusterDetailConditionVO> getClusterCondition(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("accountSeq") Integer accountSeq,
            @Param("serviceSeq") Integer serviceSeq
    );

    List<ClusterVO> getDuplicationCluster(
            @Param("apiUrl") String apiUrl,
            @Param("clusterId") String clusterId,
            @Param("clusterSeq") Integer clusterSeq
    );

    int removeComponentsByCluster(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("updater") Integer updater
    );
    int deleteComponentsByCluster(@Param("clusterSeq") Integer clusterSeq);
    int removeWorkloadGroupsByCluster(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("updater") Integer updater
    );
    int deleteWorkloadGroupsByCluster(@Param("clusterSeq") Integer clusterSeq);
    int deletePlRunBuildDeployMappingByCluster(@Param("clusterSeq") Integer clusterSeq);
    int deletePlRunDeployByCluster(@Param("clusterSeq") Integer clusterSeq);
    int deletePlRunBuildByCluster(@Param("clusterSeq") Integer clusterSeq);
    int removePlRunByCluster(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("updater") Integer updater
    );
    int deletePlRunByCluster(@Param("clusterSeq") Integer clusterSeq);
    int deletePlResBuildDeployMappingByCluster(@Param("clusterSeq") Integer clusterSeq);
    int deletePlResDeployByCluster(@Param("clusterSeq") Integer clusterSeq);
    int deletePlResBuildByCluster(@Param("clusterSeq") Integer clusterSeq);
    int removePlMasterByCluster(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("updater") Integer updater
    );
    int deletePlMasterByCluster(@Param("clusterSeq") Integer clusterSeq);
    int removeServicemapsByCluster(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("updater") Integer updater
    );
    int deleteServicemapsByCluster(@Param("clusterSeq") Integer clusterSeq);
    int removePipelineRunByCluster(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("updater") Integer updater
    );
    int deletePipelineRunByCluster(@Param("clusterSeq") Integer clusterSeq);
    int removePipelineContainerByCluster(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("updater") Integer updater
    );
    int deletePipelineContainerByCluster(@Param("clusterSeq") Integer clusterSeq);
    int removePipelineWorkloadByCluster(
            @Param("clusterSeq") Integer clusterSeq,
            @Param("updater") Integer updater
    );
    int deletePipelineWorkloadByCluster(@Param("clusterSeq") Integer clusterSeq);
    int deleteBuildServerByCluster(@Param("clusterSeq") Integer clusterSeq);
}

