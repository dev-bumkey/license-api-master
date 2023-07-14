package run.acloud.api.pipelineflow.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.pipelineflow.vo.*;

import java.util.List;

public interface IPipelineFlowMapper {
    // 등록
    int addPipelineWorkload(PipelineWorkloadVO pipelineWorkload);
    int addPipelineContainer(PipelineContainerVO pipelineContainer);
    int addPipelineRun(PipelineRunVO pipelineRun);

    // 수정
    int updatePipelineContainer(PipelineContainerVO pipelineContainer);

    // componentSeq 로 삭제
    int deletePipelineRunByWorkload(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName, @Param("workloadName") String workloadName, @Param("updater") Integer updater);
    int deletePipelineContainerByWorkload(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName, @Param("workloadName") String workloadName, @Param("updater") Integer updater);
    int deletePipelineWorkloadByWorkload(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName, @Param("workloadName") String workloadName, @Param("updater") Integer updater);

    // pipelineFlow로 PipelineFlow 삭제
    int deletePipelineRun(@Param("pipelineRunSeq") Integer pipelineRunSeq, @Param("updater") Integer updater);
    int deletePipelineContainer(@Param("pipelineContainerSeq") Integer pipelineContainerSeq, @Param("updater") Integer updater);
    int deletePipelineWorkload(@Param("pipelineWorkloadSeq") Integer pipelineWorkloadSeq, @Param("updater") Integer updater);

    // 상세조회
    PipelineWorkloadVO getPipelineWorkload(@Param("pipelineWorkloadSeq") Integer pipelineWorkloadSeq);
    PipelineWorkloadVO getPipelineWorkloadByWorkload(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName, @Param("workloadName") String workloadName);
    PipelineContainerVO getPipelineContainer(@Param("pipelineContainerSeq") Integer pipelineContainerSeq);
    PipelineRunVO getPipelineRun(@Param("pipelineRunSeq") Integer pipelineRunSeq);
    PipelineRunVO getPipelineOnRunningByWorkload(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName, @Param("workloadName") String workloadName, @Param("runStates") List<String> runStates);
    PipelineRunVO getPipelineOnRunningByBuildRunSeq(@Param("pipelineContainerSeq") Integer pipelineContainerSeq, @Param("buildSeq") Integer buildSeq, @Param("buildRunSeq") Integer buildRunSeq, @Param("runStates") List<String> runStates);

    // 리스트 조회
    List<PipelineWorkloadVO> getPipelineWorkloads(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName);
    List<PipelineRelatedInfoVO> getPipelineRelatedInfoUsingBuild(@Param("buildSeq") Integer buildSeq);
    PipelineRelatedInfoVO getPipelineRelatedInfoByContainer(@Param("pipelineContainerSeq") Integer pipelineContainerSeq);

    List<PipelineContainerVO> getPipelineContainers(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName, @Param("workloadName") String workloadName);
    List<PipelineCountVO> getPipelineContainerCountByBuild(@Param("namespaceName") String namespaceName, @Param("accountSeq") Integer accountSeq);
    int getPipelineCountByServiceSeqAndRegistryIds(@Param("serviceSeq") Integer serviceSeq, @Param("registrySeqs") List<Integer> registrySeqs, @Param("externalRegistrySeqs") List<Integer> externalRegistrySeqs);
    List<PipelineRunVO> getPipelineRuns(@Param("pipelineContainerSeq") Integer pipelineContainerSeq);
    List<PipelineRunVO> getPipelineRunOnRunning(@Param("pipelineContainerSeq") Integer pipelineContainerSeq);
    int getPipelineContainersUsingBuild(@Param("buildSeq") Integer buildSeq, @Param("buildRunSeq") Integer buildRunSeq, @Param("imageUrl") String imageUrl);

    // cluster_seq, namespace 의 워크로드들이 사용하는 registry 정보 조회
    List<PipelineContainerVO> getUsedRegistryInfoByClusterAndNamespace(@Param("clusterSeq") Integer clusterSeq, @Param("namespaceName") String namespaceName);

    // update by run
    int updatePipelineContainerBuildState(PipelineRunVO pipelineRun);
    int updatePipelineContainerDeployState(PipelineRunVO pipelineRun);
    int updatePipelineContainerDeploy(PipelineContainerVO pipelineContainer);
    int updatePipelineRunState(PipelineRunVO pipelineRun);
    int updatePipelineRunDeployContent(PipelineRunVO pipelineRun);

    // account table 조회
    Integer getAccountSeqByPipelineContainerSeq(@Param("pipelineContainerSeq") Integer pipelineContainerSeq);

    /**
     * workload name, container name, deployImageUrl로 이전 파이프 라인의 pipeline_container_seq 정보 조회, 없으면 0
     *
     * @param workloadName
     * @param containerName
     * @param deployImageUrl
     * @return
     */
    Integer getPreviousPipelineContainerSeqByImageUrlAndNames(@Param("workloadName") String workloadName, @Param("containerName") String containerName, @Param("deployImageUrl") String deployImageUrl);

}
