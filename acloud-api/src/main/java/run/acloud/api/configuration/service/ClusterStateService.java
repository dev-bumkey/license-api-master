package run.acloud.api.configuration.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.enums.ClusterState;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

@Slf4j
@Component
public class ClusterStateService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    public boolean isClusterRunning(String clusterId) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByClusterId(clusterId, "Y");

        return this.isClusterRunning(cluster);
    }

    public boolean isClusterRunning(Integer clusterSeq) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.isClusterRunning(cluster);
    }

    public boolean isClusterRunning(ClusterVO cluster) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        if(cluster != null){
            if (StringUtils.isBlank(cluster.getUseYn())) {
                return this.isClusterRunning(cluster.getClusterSeq());
            } else {
                if(BooleanUtils.toBoolean(cluster.getUseYn()) && StringUtils.equalsIgnoreCase(cluster.getClusterState(), ClusterState.RUNNING.getCode())){
                    return true;
                }
            }
        }

        return false;
    }

    public void checkClusterState(String clusterId) throws Exception{
        this.checkClusterState(clusterId, null);
    }

    public void checkClusterState(String clusterId, CocktailException e) throws Exception{
        if(!this.isClusterRunning(clusterId)){
            this.raiseClusterException(e);
        }
    }

    public void checkClusterState(Integer clusterSeq) throws Exception{
        this.checkClusterState(clusterSeq, null);
    }

    public void checkClusterState(Integer clusterSeq, CocktailException e) throws Exception{
        if(!this.isClusterRunning(clusterSeq)){
            this.raiseClusterException(e);
        }
    }

    public void checkClusterState(ClusterVO cluster) throws Exception{
        this.checkClusterState(cluster, null);
    }

    public void checkClusterState(ClusterVO cluster, CocktailException e) throws Exception{
        if(!this.isClusterRunning(cluster)){
            this.raiseClusterException(e);
        }
    }

    public boolean isClusterRunningByServicemap(Integer servicemapSeq) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);

        if (cluster == null) {
            throw new CocktailException("Not found", ExceptionType.ResourceNotFound);
        }

        return this.isClusterRunning(cluster);
    }

    public void checkClusterStateByServicemap(Integer servicemapSeq) throws Exception{
        this.checkClusterStateByServicemap(servicemapSeq, null);
    }

    public void checkClusterStateByServicemap(Integer servicemapSeq, CocktailException e) throws Exception{
        if(!this.isClusterRunningByServicemap(servicemapSeq)){
            this.raiseClusterException(e);
        }
    }

    public boolean isClusterRunningByNamespace(Integer clusterSeq, String namespace) throws Exception{
        IClusterMapper clusterMapper = this.sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterMapper.getClusterByNamespace(clusterSeq, namespace);

        return this.isClusterRunning(cluster);
    }

    public void checkClusterStateByNamespace(Integer clusterSeq, String namespace) throws Exception{
        this.checkClusterStateByNamespace(clusterSeq, namespace, null);
    }

    public void checkClusterStateByNamespace(Integer clusterSeq, String namespace, CocktailException e) throws Exception{
        if(!this.isClusterRunningByNamespace(clusterSeq, namespace)){
            this.raiseClusterException(e);
        }
    }

    private void raiseClusterException(CocktailException e) throws Exception{
        if(e != null){
            throw e;
        }else{
            throw new CocktailException("cluster is not running.", ExceptionType.ClusterIsNotRunning);
        }
    }

}
