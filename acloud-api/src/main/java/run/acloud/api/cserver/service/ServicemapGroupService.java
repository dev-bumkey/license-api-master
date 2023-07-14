package run.acloud.api.cserver.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.cserver.dao.IServicemapGroupMapper;
import run.acloud.api.cserver.vo.ServicemapGroupAddVO;
import run.acloud.api.cserver.vo.ServicemapGroupVO;
import run.acloud.api.event.service.EventService;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 2. 2.
 */
@Service
@Slf4j
public class ServicemapGroupService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private EventService eventService;

    @Transactional(transactionManager = "transactionManager")
    public void addServicemapGroup(ServicemapGroupAddVO servicemapGroupAdd) throws Exception {

    	IServicemapGroupMapper servicemapGroupDao = sqlSession.getMapper(IServicemapGroupMapper.class);
    	List<ServicemapGroupVO> servicemapGroups = servicemapGroupDao.getServicemapGroupsOfService(servicemapGroupAdd.getServiceSeq());
        servicemapGroupAdd.setServicemapGroupName(StringUtils.trim(servicemapGroupAdd.getServicemapGroupName()));

        if(CollectionUtils.isNotEmpty(servicemapGroups)){
            List<String> servicemapGroupNames = servicemapGroups.stream().map(ag -> ag.getServicemapGroupName()).collect(Collectors.toList());
            if(servicemapGroupNames.contains(servicemapGroupAdd.getServicemapGroupName())){
                throw new CocktailException(String.format("Servicemap Group '%s' already exists", servicemapGroupAdd.getServicemapGroupName()),
                        ExceptionType.AppmapGroupNameAlreadyExists);
            }
        }
        servicemapGroupDao.updateServicemapGroupSortOrder(servicemapGroupAdd.getServiceSeq(), servicemapGroupAdd.getSortOrder(), null, 1, servicemapGroupAdd.getUpdater());
        servicemapGroupDao.addServicemapGroup(servicemapGroupAdd);

        // send event
        eventService.getInstance().sendServicemapGroups(servicemapGroupAdd.getServiceSeq(), ContextHolder.exeContext());
    }
    
    @Transactional(transactionManager = "transactionManager")
    public void updateServicemapGroup(ServicemapGroupVO servicemapGroup) throws Exception {

    	Integer nextSortOrder = servicemapGroup.getSortOrder();

        IServicemapGroupMapper servicemapGroupDao = sqlSession.getMapper(IServicemapGroupMapper.class);

        ServicemapGroupVO servicemapGroupData = this.getServicemapGroup(servicemapGroup.getServicemapGroupSeq());
    	Integer serviceSeq = servicemapGroupData.getServiceSeq();
        servicemapGroup.setServiceSeq(serviceSeq);

        List<ServicemapGroupVO> servicemapGroups = servicemapGroupDao.getServicemapGroupsOfService(servicemapGroupData.getServiceSeq());
        servicemapGroup.setServicemapGroupName(StringUtils.trim(servicemapGroup.getServicemapGroupName()));

        if(CollectionUtils.isNotEmpty(servicemapGroups)){
            List<String> servicemapGroupNames = servicemapGroups.stream()
                    .filter(ag -> (!servicemapGroup.getServicemapGroupSeq().equals(ag.getServicemapGroupSeq())))
                    .map(ag -> ag.getServicemapGroupName())
                    .collect(Collectors.toList());
            if(servicemapGroupNames.contains(servicemapGroup.getServicemapGroupName())){
                throw new CocktailException(String.format("Servicemap Group '%s' already exists", servicemapGroup.getServicemapGroupName()),
                        ExceptionType.AppmapGroupNameAlreadyExists);
            }
        }

        if (nextSortOrder != null) {

    		int currSortOrder = servicemapGroupData.getSortOrder();

    		if (currSortOrder < nextSortOrder) {
                servicemapGroupDao.updateServicemapGroupSortOrder(serviceSeq, currSortOrder + 1, nextSortOrder, -1, servicemapGroup.getUpdater());
    		} else if (currSortOrder > nextSortOrder) {
                servicemapGroupDao.updateServicemapGroupSortOrder(serviceSeq, nextSortOrder, currSortOrder - 1, 1, servicemapGroup.getUpdater());
    		}
    	}

        servicemapGroupDao.updateServicemapGroup(servicemapGroup);

        // send event
        eventService.getInstance().sendServicemapGroups(servicemapGroup.getServiceSeq(), ContextHolder.exeContext());
    }

    @Transactional(transactionManager = "transactionManager")
    public void removeServicemapGroup(Integer servicemapGroupSeq, Integer updater) throws Exception {

        IServicemapGroupMapper servicemapGroupDao = sqlSession.getMapper(IServicemapGroupMapper.class);
    	boolean hasServicemaps = servicemapGroupDao.hasServicemaps(servicemapGroupSeq) == 1;

    	if (hasServicemaps) {
    		throw new CocktailException("해당 그룹은 서비스맵을 갖고 있습니다.", ExceptionType.AppmapGroupHasAppmap);
    	}

        ServicemapGroupVO servicemapGroup = this.getServicemapGroup(servicemapGroupSeq);
        Integer serviceSeq = servicemapGroup.getServiceSeq();

        servicemapGroupDao.removeServicemapGroup(servicemapGroupSeq, updater);
        servicemapGroupDao.updateServicemapGroupSortOrder(serviceSeq, servicemapGroup.getSortOrder() + 1, null, -1, updater);

        // send event
        eventService.getInstance().sendServicemapGroups(servicemapGroup.getServiceSeq(), ContextHolder.exeContext());
    }

    /**
     * ServicemapGroup List
     * @param serviceSeq
     * @return
     */
    public List<ServicemapGroupVO> getServicemapGroupsOfService(Integer serviceSeq) throws Exception {
        IServicemapGroupMapper servicemapGroupDao = sqlSession.getMapper(IServicemapGroupMapper.class);

        return servicemapGroupDao.getServicemapGroupsOfService(serviceSeq);
    }

    /**
     * ServicemapGroup
     * @param servicemapGroupSeq
     * @return
     */
    public ServicemapGroupVO getServicemapGroup(Integer servicemapGroupSeq) {
        return this.getServicemapGroup(servicemapGroupSeq, null);
    }

    /**
     * ServicemapGroup
     * @param servicemapGroupSeq
     * @return
     */
    public ServicemapGroupVO getServicemapGroup(Integer servicemapGroupSeq, Integer serviceSeq) {
        IServicemapGroupMapper servicemapGroupDao = sqlSession.getMapper(IServicemapGroupMapper.class);

        return servicemapGroupDao.getServicemapGroup(servicemapGroupSeq, serviceSeq);
    }
}
