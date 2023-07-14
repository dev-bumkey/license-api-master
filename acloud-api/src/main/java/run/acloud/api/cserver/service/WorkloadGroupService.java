package run.acloud.api.cserver.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.cserver.dao.IWorkloadGroupMapper;
import run.acloud.api.cserver.vo.WorkloadGroupAddVO;
import run.acloud.api.cserver.vo.WorkloadGroupVO;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 2. 2.
 */
@Service
@Slf4j
public class WorkloadGroupService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    public WorkloadGroupVO getWorkloadGroup(Integer workloadGroupSeq) throws Exception {
        IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);
        return workloadGroupDao.getWorkloadGroup(workloadGroupSeq);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addWorkloadGroup(WorkloadGroupAddVO workloadGroupAdd) throws Exception {
        if (workloadGroupAdd.getCreator() == null) {
            workloadGroupAdd.setCreator(1);
        }
        if (workloadGroupAdd.getColumnCount() == null) {
            workloadGroupAdd.setColumnCount(1);
        }
        workloadGroupAdd.setUseYn("Y");
    	
    	IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);
        List<WorkloadGroupVO> workloadGroups = workloadGroupDao.getWorkloadGroupsOfServicemap(workloadGroupAdd.getServicemapSeq());
        workloadGroupAdd.setWorkloadGroupName(StringUtils.trim(workloadGroupAdd.getWorkloadGroupName()));
        for (WorkloadGroupVO g : workloadGroups) {
            if (g.getWorkloadGroupName().equals(workloadGroupAdd.getWorkloadGroupName())) {
                throw new CocktailException(String.format("WorkloadGroup '%s' already exists", workloadGroupAdd.getWorkloadGroupName()),
                        ExceptionType.GroupNameAlreadyExists);
            }
        }

        workloadGroupDao.updateWorkloadGroupSortOrder(workloadGroupAdd.getServicemapSeq(), workloadGroupAdd.getSortOrder(), null, 1);
        workloadGroupDao.addWorkloadGroup(workloadGroupAdd);
    }

    @Transactional(transactionManager = "transactionManager")
    public void updateWorkloadGroup(WorkloadGroupVO workloadGroup) throws Exception {
        workloadGroup.setUpdater(1);
    	
    	Integer nextSortOrder = workloadGroup.getSortOrder();

    	IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);

        WorkloadGroupVO groupData = workloadGroupDao.getWorkloadGroup(workloadGroup.getWorkloadGroupSeq());
        if (groupData == null) {
            throw new CocktailException("Workload Group not found.", ExceptionType.CommonNotFound, ExceptionBiz.WORKLOAD_GROUP);
        }
    	Integer servicemapSeq = groupData.getServicemapSeq();
        workloadGroup.setServicemapSeq(servicemapSeq);

        List<WorkloadGroupVO> groups = workloadGroupDao.getWorkloadGroupsOfServicemap(groupData.getServicemapSeq());
        workloadGroup.setWorkloadGroupName(StringUtils.trim(workloadGroup.getWorkloadGroupName()));
        for (WorkloadGroupVO g : groups) {
            if (!g.getWorkloadGroupSeq().equals(workloadGroup.getWorkloadGroupSeq()) && g.getWorkloadGroupName().equals(workloadGroup.getWorkloadGroupName())) {
                throw new CocktailException(String.format("WorkloadGroup '%s' already exists", workloadGroup.getWorkloadGroupName()),
                        ExceptionType.GroupNameAlreadyExists);
            }
        }

        if (nextSortOrder != null) {
    		
    		int currSortOrder = groupData.getSortOrder();
    		
    		if (currSortOrder < nextSortOrder) {
                workloadGroupDao.updateWorkloadGroupSortOrder(servicemapSeq, currSortOrder + 1, nextSortOrder, -1);
    		} else if (currSortOrder > nextSortOrder) {
                workloadGroupDao.updateWorkloadGroupSortOrder(servicemapSeq, nextSortOrder, currSortOrder - 1, 1);
    		}
    	}

        workloadGroupDao.updateWorkloadGroup(workloadGroup);
    }
    
    @Transactional(transactionManager = "transactionManager")
    public void removeWorkloadGroup(Integer workloadGroupSeq) throws Exception {
        IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);

        WorkloadGroupVO workloadGroup = workloadGroupDao.getWorkloadGroup(workloadGroupSeq);
    	Integer servicemapSeq = workloadGroup.getServicemapSeq();

        workloadGroupDao.removeWorkloadGroup(workloadGroupSeq);
        workloadGroupDao.updateWorkloadGroupSortOrder(servicemapSeq, workloadGroup.getSortOrder() + 1, null, -1);
    }

    public String getNamespaceOfWorkloadGroup(Integer workloadGroupSeq) throws Exception {
        IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);
        return workloadGroupDao.getNamespaceOfWorkloadGroup(workloadGroupSeq);
    }
}
