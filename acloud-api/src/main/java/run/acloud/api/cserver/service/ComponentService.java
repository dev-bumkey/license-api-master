package run.acloud.api.cserver.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.cserver.enums.StateCode;
import run.acloud.api.event.service.EventService;
import run.acloud.api.resource.dao.IComponentMapper;
import run.acloud.api.resource.vo.ComponentDetailsVO;
import run.acloud.api.resource.vo.ComponentFilterVO;
import run.acloud.api.resource.vo.ComponentVO;
import run.acloud.commons.vo.ExecutingContextVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 1. 12.
 */
@Slf4j
@Service
public class ComponentService {
	
	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
    private EventService eventService;

	public ComponentVO getComponent(Integer componentSeq) throws Exception{
        IComponentMapper componentDao = sqlSession.getMapper(IComponentMapper.class);
        return componentDao.getComponent(componentSeq);
    }

    @Transactional(transactionManager = "transactionManager")
    public void removeComponent(ComponentVO component) throws Exception{
    	IComponentMapper componentDao = sqlSession.getMapper(IComponentMapper.class);

    	// 아래 코드로 인해 DB DeadLock 발생. 필요 조건이 아닌 것 같아 주석처리함.
    	componentDao.updateComponentSortOrder(component.getWorkloadGroupSeq(), component.getSortOrder() + 1, null, -1);
    	componentDao.removeComponent(component);
    }
    
    public void updateComponentOrder(ComponentVO component, ExecutingContextVO context) throws Exception{

    	int componentSeq = component.getComponentSeq();
    	int nextWorkloadGroupSeq = component.getWorkloadGroupSeq();
    	int nextSortOrder = component.getSortOrder();
    	
    	IComponentMapper componentDao = sqlSession.getMapper(IComponentMapper.class);

		ComponentVO currCompo = componentDao.getComponent(componentSeq);
		int currWorkgroupSeq = currCompo.getWorkloadGroupSeq();
		int currSortOrder = currCompo.getSortOrder();

		// init current group sort order
		componentDao.updateComponentInitSortOrder(currWorkgroupSeq);

		if (currWorkgroupSeq != nextWorkloadGroupSeq) {
			// init next group sort order
			componentDao.updateComponentInitSortOrder(nextWorkloadGroupSeq);

			componentDao.updateComponentSortOrder(currWorkgroupSeq, currSortOrder + 1, null, -1);
    		componentDao.updateComponentSortOrder(nextWorkloadGroupSeq, nextSortOrder, null, 1);
			componentDao.updateComponentSelfSortOrder(component);
    	} else {
    		if (currSortOrder < nextSortOrder) {
    			componentDao.updateComponentSortOrder(currWorkgroupSeq, currSortOrder + 1, nextSortOrder, -1);
    		} else if (currSortOrder > nextSortOrder) {
    			componentDao.updateComponentSortOrder(currWorkgroupSeq, nextSortOrder, currSortOrder - 1, 1);
    		}
    		componentDao.updateComponentSelfSortOrder(component);
    	}

    }

	@Transactional(transactionManager = "transactionManager")
	public ComponentVO addServerComponent(ComponentVO component, ExecutingContextVO context) throws Exception {
		IComponentMapper dao = sqlSession.getMapper(IComponentMapper.class);
		dao.addComponent(component);

		return component;
	}

    public void updateComponent(ComponentVO component) throws Exception{
        IComponentMapper componentDao = sqlSession.getMapper(IComponentMapper.class);
        componentDao.updateComponent(component);
    }

	public void updateComponentState(ExecutingContextVO context, Integer clusterSeq, String namespaceName, String componentName, StateCode code, IComponentMapper cm) {
		this.updateComponentState(context, clusterSeq, namespaceName, componentName, code, null, cm);
	}

	public void updateComponentState(ExecutingContextVO context, Integer clusterSeq, String namespaceName, String componentName, StateCode code, String errorMessage, IComponentMapper cm) {
		ComponentVO component = new ComponentVO();
		component.setClusterSeq(clusterSeq);
		component.setNamespaceName(namespaceName);
		component.setComponentName(componentName);

		this.updateComponentState(context, component, code, errorMessage, cm);
	}

	public void updateComponentState(ExecutingContextVO context, ComponentVO component, StateCode code, IComponentMapper cm) {
		this.updateComponentState(context, component, code, null, cm);
	}

	public void updateComponentState(ExecutingContextVO context, ComponentVO component, StateCode code, String errorMessage, IComponentMapper cm) {
		if (cm == null) {
			cm = this.sqlSession.getMapper(IComponentMapper.class);
		}

		ComponentVO currComponent = cm.getComponentByClusterAndNames(component.getClusterSeq(), component.getNamespaceName(), component.getComponentName());

		if (currComponent != null) {
			component.setComponentSeq(currComponent.getComponentSeq());
		} else {
			component.setStateCode(code.getCode());
			cm.addComponent(component);
		}

		component.setErrorMessage(null);
		component.setUpdater(context.getUserSeq());
		component.setStateCode(code.getCode());

		if (component.getComponentSeq() != null && component.getComponentSeq().intValue() > 0) {
			if (code == StateCode.DELETED) {
				if(context != null) {
					cm.updateComponentSortOrder(component.getWorkloadGroupSeq(), component.getSortOrder() + 1, null, -1);
					component.setUseYn("N");
				}
				cm.removeComponent(component);
			} else if (code == StateCode.ERROR) {
				component.setErrorMessage(errorMessage);
				cm.updateComponent(component);
			} else {
				cm.updateComponent(component);
			}
		}
	}

	public void updateComponentState(ComponentVO component, StateCode state, String detail, IComponentMapper compoDao) {
    	if (component.getComponentSeq() != null && component.getComponentSeq().intValue() > 0) {
			component.setStateCode(state.getCode());
			compoDao.updateComponentState(component); // 3.5.0 : 2019.09.25 : Status를 Update 할때는 Updater와 Updated를 수정하지 않는 updateComponentState를 이용..
			component.setStateDetail(detail);
		}
	}

	public List<ComponentDetailsVO> getComponentDetails(ExecutingContextVO context) throws Exception {
		return this.getComponentDetails(new ComponentFilterVO(), context);
	}

	public List<ComponentDetailsVO> getComponentDetails(ComponentFilterVO compoFilter, ExecutingContextVO context) throws Exception {

    	if(compoFilter == null){
    		compoFilter = new ComponentFilterVO();
		}

		IComponentMapper compoDao = sqlSession.getMapper(IComponentMapper.class);

		List<ComponentDetailsVO> componentDetails = compoDao.getComponentDetails(compoFilter);
//		List<DeploymentVO> deployments = deployDao.getAllDeployments();


//		if (CollectionUtils.isNotEmpty(componentDetails)) {
//
//			for(ComponentDetailsVO componentDetailsRow : componentDetails){
//
//				if(CollectionUtils.isNotEmpty(deployments)){
//					if(CollectionUtils.isNotEmpty(componentDetailsRow.getComponents())){
//						for(ComponentVO componentRow : componentDetailsRow.getComponents()){
//
//							Optional<DeploymentVO> deploymentVOOptional = deployments.stream().filter(d -> (d.getDeploy() != null && d.getDeploy().getComponentSeq() != null && componentRow.getComponentSeq().equals(d.getDeploy().getComponentSeq()))).findFirst();
//							if(deploymentVOOptional.isPresent()){
//								componentRow.setDeployment(deploymentVOOptional.get());
//								componentRow.getDeployment().setServerAdd(JsonUtils.fromGson(deploymentVOOptional.get().getContent(), ServerGuiVO.class));
//							}
//						}
//					}
//				}
//			}
//
//		}

		return componentDetails;
	}

}
