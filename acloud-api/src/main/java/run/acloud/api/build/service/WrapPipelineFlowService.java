package run.acloud.api.build.service;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import run.acloud.api.pipelineflow.dao.IPipelineFlowMapper;
import run.acloud.api.pipelineflow.vo.PipelineCountVO;
import run.acloud.api.pipelineflow.vo.PipelineRelatedInfoVO;

import java.util.List;

@Slf4j
@Service
public class WrapPipelineFlowService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;


    public List<PipelineCountVO> getPipelineContainerCountByBuild(String namespaceName, Integer accountSeq){
        IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

        // buildSeq 별 count 조회
        List<PipelineCountVO> pipelineCounts = pipelineDao.getPipelineContainerCountByBuild(namespaceName, accountSeq);

        return pipelineCounts;
    }

    /**
     * buildSeq, buildRunSeq, imageUrl를 사용하는 pipeline이 있는지 체크하는 메서드, 존재시 true
     *
     * @param buildSeq
     * @param buildRunSeq
     * @param imageUrl
     * @return
     */
    public boolean checkPipelineUsingBuild(Integer buildSeq, Integer buildRunSeq, String imageUrl){
        IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
        int cnt = pipelineDao.getPipelineContainersUsingBuild(buildSeq, buildRunSeq, imageUrl);

        return (cnt > 0);
    }

    // buildSeq pipeline workload 관련 정보 조회
    public List<PipelineRelatedInfoVO> getPipelineRelatedInfoListUsingBuild(Integer buildSeq){
        IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

        List<PipelineRelatedInfoVO> pipelineRelatedInfos = pipelineDao.getPipelineRelatedInfoUsingBuild(buildSeq);

        return pipelineRelatedInfos;
    }

    // pipeline seq 로  workload 관련 정보 조회
    public PipelineRelatedInfoVO getPipelineRelatedInfoByContainer(Integer pipelineContainerSeq){
        IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

        PipelineRelatedInfoVO pipelineRelatedInfo = pipelineDao.getPipelineRelatedInfoByContainer(pipelineContainerSeq);

        return pipelineRelatedInfo;
    }

}
