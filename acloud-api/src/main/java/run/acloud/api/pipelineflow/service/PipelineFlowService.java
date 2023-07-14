package run.acloud.api.pipelineflow.service;

import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.build.enums.BuildAction;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.enums.StepType;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployMode;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.service.ServerService;
import run.acloud.api.cserver.service.ServerValidService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.cserver.vo.ServerIntegrateVO;
import run.acloud.api.cserver.vo.ServerStateVO;
import run.acloud.api.pipelineflow.constants.PipelineFlowConstant;
import run.acloud.api.pipelineflow.dao.IPipelineFlowMapper;
import run.acloud.api.pipelineflow.enums.*;
import run.acloud.api.pipelineflow.util.PipelineTypeConverter;
import run.acloud.api.pipelineflow.vo.PipelineCommandVO;
import run.acloud.api.pipelineflow.vo.PipelineContainerVO;
import run.acloud.api.pipelineflow.vo.PipelineRunVO;
import run.acloud.api.pipelineflow.vo.PipelineWorkloadVO;
import run.acloud.api.resource.enums.ImagePullPolicyType;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.ComponentVO;
import run.acloud.api.resource.vo.ContainerVO;
import run.acloud.api.resource.vo.ServerDetailVO;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailUIProperties;
import run.acloud.protobuf.pipeline.PipelineAPIServiceProto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PipelineFlowService {

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private CocktailUIProperties cocktailUIProperties;

	@Autowired
	private Environment environment;

	@Autowired
	private ServerService serverService;

	@Autowired
	private ServerValidService serverValidService;

	@Autowired
	private ServicemapService servicemapService;


	/** R3.5.0 빌드 서비스 **/
	@Autowired
	private WrappedBuildService buildService;

	@Autowired
    private PipelineAPIService pipelineAPIService;

	/**
	 * 파이프라인 Flow 생성
	 * (Api 호출 용)
	 *
	 * @param pipelineAdd
	 * @param context
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public PipelineWorkloadVO addPipeline(PipelineWorkloadVO pipelineAdd, ExecutingContextVO context) throws Exception {
		log.debug("[BEGIN] addPipeline(PipelineWorkloadVO pipelineAdd, ExecutingContextVO context), pipelineAdd : [{}]", pipelineAdd);

		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

		try {

			if(CollectionUtils.isEmpty(pipelineAdd.getPipelineContainers())){
				throw new CocktailException("파이프라이 생성 중 오류가 발생하였습니다. 파이프라인 정보가 존재하지 않습니다.", ExceptionType.PipelineCreationFail, pipelineAdd);
			}

			/**
             * 파라미터 validation 체크
             */
			List<String> invalidParams = new ArrayList<>();
			for(PipelineContainerVO pipelineContainer : pipelineAdd.getPipelineContainers()){
                if(PipelineType.BUILD_DEPLOY == pipelineContainer.getPipelineType()){
                    this.checkPipelineContainerParam(pipelineContainer, invalidParams);
                }
                if(CollectionUtils.isNotEmpty(invalidParams)){
                    throw new CocktailException(this.genFieldRequiredMsg(StringUtils.join(invalidParams.toArray(), ", ")), ExceptionType.PipelineCreationFail_ParameterInvalid, pipelineAdd);
                }
            }

			/**
			 * 파이프라인 워크로드 등록
			 */
			pipelineDao.addPipelineWorkload(pipelineAdd);

			/**
             * 파이프라인 컨테이너 등록
             */
			for(PipelineContainerVO pipelineContainer : pipelineAdd.getPipelineContainers()){
				pipelineContainer.setPipelineWorkloadSeq(pipelineAdd.getPipelineWorkloadSeq());
				pipelineContainer.setBuildState(PipelineRunState.CREATED);
				pipelineContainer.setDeployState(PipelineRunState.CREATED);
				pipelineContainer.setCreator(context.getUserSeq());
				pipelineDao.addPipelineContainer(pipelineContainer);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			if(e instanceof CocktailException){
				throw e;
			}else{
				throw new CocktailException("파이프라이 생성 중 오류가 발생하였습니다.", e, ExceptionType.PipelineCreationFail, pipelineAdd);
			}

		}
		log.debug("[END] addPipeline(PipelineWorkloadVO pipelineAdd, ExecutingContextVO context)");
		return pipelineDao.getPipelineWorkloadByWorkload(pipelineAdd.getClusterSeq(), pipelineAdd.getNamespaceName(), pipelineAdd.getWorkloadName());

	}

	/**
	 * 파이프라인 생성
	 * (서버 생성시 사용)
	 *
	 * @param serverParam
	 * @param serviceSeq - 워크스페이스 시퀀스, service_seq
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public PipelineWorkloadVO addPipeline(ServerGuiVO serverParam, Integer serviceSeq, ExecutingContextVO context) throws Exception{
		log.debug("[BEGIN] addPipeline(ServerGuiVO serverParam, ExecutingContextVO context), serverParam : [{}]",serverParam);

		PipelineWorkloadVO pipelineAdd = new PipelineWorkloadVO();

		try {
			pipelineAdd.setCreator(context.getUserSeq());
			List<PipelineContainerVO> pipelineContainers = new ArrayList<>();

			// clusterSeq, namespaceName, workloadName 값 셋팅 및 validation 체크
			Integer clusterSeq = serverParam.getComponent().getClusterSeq();
			String namespaceName = serverParam.getComponent().getNamespaceName();
			String worklaodName = serverParam.getComponent().getComponentName();

			// clusterSeq, componentSeq, namespaceName, workloadName 셋팅
			pipelineAdd.setClusterSeq(clusterSeq);
			pipelineAdd.setNamespaceName(namespaceName);
			pipelineAdd.setWorkloadName(worklaodName);

			// 필수값들임 하나라도 없으면 오류
			if(clusterSeq == null || StringUtils.isBlank(namespaceName) || StringUtils.isBlank(worklaodName)){
				throw new CocktailException("파이프라이 생성 중 오류가 발생하였습니다.", ExceptionType.PipelineCreationFail, pipelineAdd);
			}

			// init container 정보 셋팅
			if(CollectionUtils.isNotEmpty(serverParam.getInitContainers())){

				PipelineContainerVO pipelineContainer = null;
                List<ContainerVO> initContainers = serverParam.getInitContainers();

                for(int i = 0, ie = initContainers.size(); i < ie; i++){
					pipelineContainer = new PipelineContainerVO();
                    this.genPipelineContainer(pipelineContainer, initContainers.get(i), serviceSeq); // PipelineContainerVO 데이터 셋팅
					pipelineContainer.setCreator(context.getUserSeq());
					pipelineContainers.add(pipelineContainer);
                }
            }

			// container 정보 셋팅
			if(CollectionUtils.isNotEmpty(serverParam.getContainers())){

				PipelineContainerVO pipelineContainer = null;
                List<ContainerVO> containers = serverParam.getContainers();

                for(int i = 0, ie = containers.size(); i < ie; i++){
					pipelineContainer = new PipelineContainerVO();
					this.genPipelineContainer(pipelineContainer, containers.get(i), serviceSeq);
					pipelineContainer.setCreator(context.getUserSeq());
					pipelineContainers.add(pipelineContainer);
                }
            }
			pipelineAdd.setPipelineContainers(pipelineContainers);

		} catch (Exception e) {
			log.error(e.getMessage(), e);

			if(e instanceof CocktailException){
				throw e;
			}else{
				throw new CocktailException("파이프라이 생성 중 오류가 발생하였습니다.", e, ExceptionType.PipelineCreationFail, serverParam);
			}
		}

		log.debug("[END] addPipeline(ServerGuiVO serverParam, ExecutingContextVO context)");
		return this.addPipeline(pipelineAdd, context);
	}

	/**
	 * 파이프라인 수정
	 * (실행)
	 *
	 * @param pipelineContainerSeq
	 * @param context
	 * @return
	 * @throws Exception
	 */
	@Transactional(transactionManager = "transactionManager")
	public PipelineContainerVO editPipelinesByRun(Integer pipelineContainerSeq, BuildRunVO buildRun, ExecutingContextVO context) throws Exception{
		PipelineContainerVO pipelineContainer = null;
		try {
			IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

			// pipeline 조회
			pipelineContainer = pipelineDao.getPipelineContainer(pipelineContainerSeq);

			// buildRun 정보로 pipelineContainer 정보 셋팅
			pipelineContainer.setBuildSeq(buildRun.getBuildSeq());
			pipelineContainer.setBuildRunSeq(buildRun.getBuildRunSeq());
			pipelineContainer.setBuildRegistryName(buildRun.getRegistryName());
			pipelineContainer.setBuildRegistrySeq(buildRun.getRegistryProjectId());
			pipelineContainer.setBuildImageName(buildRun.getImageName());
			pipelineContainer.setBuildImageTag(StringUtils.substringAfterLast(buildRun.getImageUrl(),":")); //1.0.B000009
			pipelineContainer.setBuildImageUrl(buildRun.getImageUrl());
			pipelineContainer.setBuildState(PipelineRunState.WAIT);
			pipelineContainer.setUpdater(context.getUserSeq());

			// pipeline 실행 정보 생성
			this.addPipelineRun(pipelineContainer, true);

			// pipeline container 정보 update
			pipelineDao.updatePipelineContainer(pipelineContainer);

			return pipelineContainer;
		} catch (Exception e) {
			log.error(e.getMessage(), e);

			if(e instanceof CocktailException){
				throw e;
			}else{
				throw new CocktailException("파이프라이 실행 중 오류가 발생하였습니다.", e, ExceptionType.PipelineRunningFail, pipelineContainer);
			}
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public PipelineContainerVO runChangeImage(Integer pipelineContainerSeq, Integer buildRunSeq, String tagName, ExecutingContextVO context) throws Exception{
		PipelineContainerVO pipelineContainer = null;
		try {
			IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

			// pipeline 조회
			pipelineContainer = pipelineDao.getPipelineContainer(pipelineContainerSeq);

			if(pipelineContainer.getPipelineType() == PipelineType.BUILD_DEPLOY){
				BuildRunVO buildRun = buildService.getBuildRun(buildRunSeq);

				// buildRun 정보로 pipelineContainer 정보 셋팅
				pipelineContainer.setBuildSeq(buildRun.getBuildSeq());
				pipelineContainer.setBuildRunSeq(buildRun.getBuildRunSeq());
				pipelineContainer.setBuildRegistryName(buildRun.getRegistryName());
				pipelineContainer.setBuildRegistrySeq(buildRun.getRegistryProjectId());
				pipelineContainer.setBuildImageName(buildRun.getImageName());
				pipelineContainer.setBuildImageTag(StringUtils.substringAfterLast(buildRun.getImageUrl(),":"));
				pipelineContainer.setBuildImageUrl(buildRun.getImageUrl());
				pipelineContainer.setBuildState(PipelineRunState.DONE);

				pipelineContainer.setDeployBuildRunSeq(buildRun.getBuildRunSeq());
				pipelineContainer.setDeployRegistryName(buildRun.getRegistryName());
				pipelineContainer.setDeployRegistrySeq(buildRun.getRegistryProjectId());
				pipelineContainer.setDeployImageName(buildRun.getImageName());
				pipelineContainer.setDeployImageTag(StringUtils.substringAfterLast(buildRun.getImageUrl(),":"));
				pipelineContainer.setDeployImageUrl(buildRun.getImageUrl());
				pipelineContainer.setDeployState(PipelineRunState.WAIT);
				pipelineContainer.setUpdater(context.getUserSeq());

			} else if(pipelineContainer.getPipelineType() == PipelineType.PUBLIC_DEPLOY){
				pipelineContainer.setDeployImageTag(tagName);
				pipelineContainer.setDeployImageUrl(pipelineContainer.getDeployImageName() +":"+pipelineContainer.getDeployImageTag());
				pipelineContainer.setDeployState(PipelineRunState.WAIT);
				pipelineContainer.setUpdater(context.getUserSeq());
			}

			// pipeline 실행 정보 생성
			this.addPipelineRun(pipelineContainer, false);

			// pipeline container 정보 update
			pipelineDao.updatePipelineContainer(pipelineContainer);

			return pipelineContainer;
		} catch (Exception e) {
			log.error(e.getMessage(), e);

			if(e instanceof CocktailException){
				throw e;
			}else{
				throw new CocktailException("파이프라이 실행 중 오류가 발생하였습니다.", e, ExceptionType.PipelineRunningFail, pipelineContainer);
			}
		}
	}

	/**
	 * 빌드 오류시 기존 배포정보로 빌드 정보 롤백하는 메서드
	 *
	 * @param pipelineContainerSeq
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public PipelineContainerVO rollbackBuildImage(Integer pipelineContainerSeq, ExecutingContextVO context) throws Exception{
		PipelineContainerVO pipelineContainer = null;
		try {
			IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

			// pipeline 조회
			pipelineContainer = pipelineDao.getPipelineContainer(pipelineContainerSeq);

			// BUILD_DEPLOY 인 경우만 처리함
			if(pipelineContainer.getPipelineType() == PipelineType.BUILD_DEPLOY){
				pipelineContainer.setBuildRunSeq(pipelineContainer.getDeployBuildRunSeq());
				pipelineContainer.setBuildRegistryName(pipelineContainer.getDeployRegistryName());
				pipelineContainer.setBuildRegistrySeq(pipelineContainer.getDeployRegistrySeq());
				pipelineContainer.setBuildImageName(pipelineContainer.getDeployImageName());
				pipelineContainer.setBuildImageTag(pipelineContainer.getDeployImageTag());
				pipelineContainer.setBuildImageUrl(pipelineContainer.getDeployImageUrl());
				pipelineContainer.setBuildState(pipelineContainer.getDeployState());
				pipelineContainer.setUpdater(context.getUserSeq());
			}

			// pipeline container 정보 update
			pipelineDao.updatePipelineContainer(pipelineContainer);

			return pipelineContainer;

		} catch (Exception e) {
			log.error(e.getMessage(), e);

			if(e instanceof CocktailException){
				throw e;
			}else{
				throw new CocktailException("파이프라이 실행 중 오류가 발생하였습니다.", e, ExceptionType.PipelineRunningFail, pipelineContainer);
			}
		}
	}

	public String genPipelineCallbackUrl(ExecutingContextVO context){
		return genPipelineCallbackUrl(context, false);
	}

	public String genPipelineCallbackUrl(ExecutingContextVO context, boolean withPipelineListener){
		String callback = cocktailUIProperties.getCallbackBuilderUrl();

		// pipeline listener url 추가할 때만 처리
		if(withPipelineListener) {
			String servicePipelineListener = String.format(PipelineFlowConstant.PIPELINE_CALLBACK_URL
					, "api-server"
					, environment.getProperty("local.server.port")
					, context.getUserSeq()
					, context.getUserRole()
					, context.getUserServiceSeq());

			callback = String.format("%s|%s", servicePipelineListener, callback);
		}

		return callback;
	}

	public int addPipelineRun(PipelineContainerVO pipelineContainer, boolean containBuild){
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

		int updateCnt = 0;
		if(pipelineContainer.getPipelineType() == PipelineType.PUBLIC_DEPLOY || !containBuild){ // Deploy만 필요한 경우
			PipelineRunVO deployRunVO = new PipelineRunVO();
			deployRunVO.setPipelineContainerSeq(pipelineContainer.getPipelineContainerSeq());
			deployRunVO.setRunType(PipelineRunType.DEPLOY);
			deployRunVO.setRunState(PipelineRunState.WAIT);
			deployRunVO.setUseYn("Y");
			deployRunVO.setDeployContent(JsonUtils.toGson(pipelineContainer));

			updateCnt += pipelineDao.addPipelineRun(deployRunVO);
		}else{ // Build & Deploy 필요한 경우
			PipelineRunVO buildRunVO = new PipelineRunVO();
			buildRunVO.setPipelineContainerSeq(pipelineContainer.getPipelineContainerSeq());
			buildRunVO.setRunType(PipelineRunType.BUILD);
			buildRunVO.setRunState(PipelineRunState.WAIT);
			buildRunVO.setBuildSeq(pipelineContainer.getBuildSeq());
			buildRunVO.setBuildRunSeq(pipelineContainer.getBuildRunSeq());
			buildRunVO.setBuildRegistrySeq(pipelineContainer.getBuildRegistrySeq());
			buildRunVO.setBuildRegistryName(pipelineContainer.getBuildRegistryName());
			buildRunVO.setBuildImageName(pipelineContainer.getBuildImageName());
			buildRunVO.setBuildImageTag(pipelineContainer.getBuildImageTag());
			buildRunVO.setBuildImageUrl(pipelineContainer.getBuildImageUrl());
			buildRunVO.setUseYn("Y");
			updateCnt += pipelineDao.addPipelineRun(buildRunVO);

			PipelineRunVO deployRunVO = new PipelineRunVO();
			deployRunVO.setPipelineContainerSeq(pipelineContainer.getPipelineContainerSeq());
			deployRunVO.setRunType(PipelineRunType.DEPLOY);
			deployRunVO.setRunState(PipelineRunState.WAIT);
			deployRunVO.setUseYn("Y");
			deployRunVO.setDeployContent(JsonUtils.toGson(pipelineContainer));

			updateCnt += pipelineDao.addPipelineRun(deployRunVO);
		}

		return updateCnt;
	}

	/**
	 * 워크로드에서 수정되었을때 기존 파이프라인 정보와 비교하여 데이터 처리를 하는 메서드임.
	 *
	 * @param serverAdd
	 * @param serviceSeq - 워크스페이스 번호, service_seq
	 * @param context
	 * @throws Exception
	 */
	public void mergePipeline(ServerGuiVO serverAdd, Integer serviceSeq, ExecutingContextVO context) throws Exception {
		log.debug("[BEGIN] mergePipeline, severAdd : {}",serverAdd);
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

        if(CollectionUtils.isNotEmpty(serverAdd.getContainers())){
			// 조회키 추출
			Integer clusterSeq = serverAdd.getComponent().getClusterSeq();
			String namespaceName = serverAdd.getComponent().getNamespaceName();
			String worklaodName = serverAdd.getComponent().getComponentName();

            PipelineWorkloadVO oldPipeline = pipelineDao.getPipelineWorkloadByWorkload(clusterSeq, namespaceName, worklaodName);

			// 기존 pipeline 정보가 존재 할 경우
			if (oldPipeline != null ){
				
            	List<PipelineContainerVO> oldPipelineContainers = oldPipeline.getPipelineContainers();

				// 처리로직에서의 검색을 위해 기존 PipelineContainer 정보를 map에 저장
				Map<String, PipelineContainerVO> oldPipelineContainerMap = new HashMap<>();
				if( CollectionUtils.isNotEmpty(oldPipelineContainers) ){
					oldPipelineContainerMap = oldPipeline.getPipelineContainers().stream().collect(Collectors.toMap(PipelineContainerVO::getContainerName, Function.identity()));
				}

				// 우선 기존 pipelineContainer 삭제(use_yn : 'N')
				pipelineDao.deletePipelineContainerByWorkload(clusterSeq, namespaceName, worklaodName, context.getUserSeq());
                oldPipelineContainers.stream().forEach(container-> container.setUseYn("N"));// 조회한 컨테이너들도 우선 use_yn = 'N' 로 셋팅

				PipelineContainerVO pipelineContainer = null;
                PipelineContainerVO tmpPipelineContainer = null;

                // 서버의 모든 container를 하나로 합침.
				List<ContainerVO> containers = new ArrayList<>();
				ResourceUtil.mergeContainer(containers, serverAdd.getInitContainers(), serverAdd.getContainers());

				for(ContainerVO serverContainer : containers) {
					// 기존 조회된 Pipeline 중에 현재 서버정보의 컨테이너에 해당하는 정보가 있는지 조회
					pipelineContainer = oldPipelineContainerMap.get(serverContainer.getContainerName());

					// pipeline 정보에 기존 container 정보가 존재하면
					if (pipelineContainer != null) {

                        pipelineContainer.setUseYn("Y");
                        pipelineContainer.setUpdater(context.getUserSeq());

                        // 새 Object에 내용 copy
                        tmpPipelineContainer = new PipelineContainerVO();
                        BeanUtils.copyProperties(pipelineContainer, tmpPipelineContainer);

						this.genPipelineContainer(tmpPipelineContainer, serverContainer, serviceSeq); // 서버 container로 PipelineContainer 정보 update

                        // 기존에는 BUILD_DEPLOY => PUBLIC_DEPLOY로 변경 되었을 경우는 pipeline server의 instance 삭제 요청한다.
                        if(pipelineContainer.getPipelineType() == PipelineType.BUILD_DEPLOY && tmpPipelineContainer.getPipelineType() == PipelineType.PUBLIC_DEPLOY){
                            pipelineAPIService.terminatePipelineBuild(pipelineContainer);
                        }

                        // pipeline container update
						pipelineDao.updatePipelineContainer(tmpPipelineContainer);

					} else {
                        tmpPipelineContainer = new PipelineContainerVO();
                        tmpPipelineContainer.setPipelineWorkloadSeq(oldPipeline.getPipelineWorkloadSeq());
                        tmpPipelineContainer.setBuildState(PipelineRunState.CREATED);
                        tmpPipelineContainer.setDeployState(PipelineRunState.CREATED);
						this.genPipelineContainer(tmpPipelineContainer, serverContainer, serviceSeq);
                        tmpPipelineContainer.setCreator(context.getUserSeq());
						pipelineDao.addPipelineContainer(tmpPipelineContainer);
					}
				}

				// 기존에 존재 했으나 수정하면서 없어진 BUILD_DEPLOY TYPE 의 pipelineContainer건에 대해 pipeline server의 instance 삭제 요청한다.
                for(PipelineContainerVO oldPipelineContainer : oldPipelineContainers){
                    if(oldPipelineContainer.getPipelineType() == PipelineType.BUILD_DEPLOY && StringUtils.equals(oldPipelineContainer.getUseYn(), "N")){
                        pipelineAPIService.terminatePipelineBuild(oldPipelineContainer);
                    }
                }
			}else{ // 기존 내역이 존재하지 않을 경우 다시 등록
                this.addPipeline(serverAdd, serviceSeq, context);
			}
		}
		log.debug("[END] mergePipeline");
	}

	private void genPipelineContainer(PipelineContainerVO pipelineContainer, ContainerVO container, Integer serviceSeq) throws Exception {

		// build seq 조회
		Integer buildSeq = container.getBuildSeq();
		if(buildSeq == null) buildSeq = container.getBuildTaskSeq();

		String imageUrl = container.getFullImageName();

		// image로 BuildRun 정보조회
		BuildRunVO buildRun;
		if(buildSeq != null){
			buildRun = buildService.getBuildRunsByImageUrl(buildSeq, serviceSeq, imageUrl);
		} else {
			buildRun = buildService.getBuildRunsByImageUrl(null, serviceSeq, imageUrl);
		}

		// registry private 여부
		boolean isPrivate = false;
		if( buildRun != null){
			isPrivate = true;
		}

		if ( isPrivate ) {
			pipelineContainer.setPipelineType(PipelineType.BUILD_DEPLOY);

			pipelineContainer.setBuildSeq(buildRun.getBuildSeq());
			pipelineContainer.setBuildRunSeq(buildRun.getBuildRunSeq());
			pipelineContainer.setBuildState(PipelineRunState.valueOf(buildRun.getRunState().getCode()));
			pipelineContainer.setBuildRegistrySeq(buildRun.getRegistryProjectId());
			pipelineContainer.setBuildRegistryName(buildRun.getRegistryName());
			pipelineContainer.setBuildImageName(buildRun.getImageName());
			pipelineContainer.setBuildImageTag(StringUtils.substringAfterLast(imageUrl, ":"));
			pipelineContainer.setBuildImageUrl(buildRun.getImageUrl());

			// deploy info
			pipelineContainer.setDeployBuildRunSeq(buildRun.getBuildRunSeq());
			pipelineContainer.setDeployRegistrySeq(buildRun.getRegistryProjectId());
			pipelineContainer.setDeployRegistryName(buildRun.getRegistryName());
			pipelineContainer.setDeployImageName(buildRun.getImageName());
			pipelineContainer.setDeployImageTag(StringUtils.substringAfterLast(imageUrl, ":"));
			pipelineContainer.setDeployImageUrl(buildRun.getImageUrl());

		} else {
			pipelineContainer.setPipelineType(PipelineType.PUBLIC_DEPLOY);
			pipelineContainer.setDeployImageName(StringUtils.substringBefore(imageUrl, ":"));
			pipelineContainer.setDeployImageTag(StringUtils.substringAfterLast(imageUrl, ":"));
			pipelineContainer.setDeployImageUrl(imageUrl);
		}

		pipelineContainer.setContainerName(container.getContainerName());
	}

	private String genFieldRequiredMsg(String fieldName){
		String invalidParamMsg = "\'%s\' field required.";

		return String.format(invalidParamMsg, fieldName);
	}

	private void checkPipelineContainerParam(PipelineContainerVO pipelineFlow, List<String> invalidParams){
		// 빌드 번호
		if(pipelineFlow.getBuildSeq() == null || pipelineFlow.getBuildSeq().intValue() < 1){
			invalidParams.add("buildSeq");
		}

		// 빌드 실행 번호
		if(pipelineFlow.getBuildRunSeq() == null || pipelineFlow.getBuildRunSeq().intValue() < 1){
			invalidParams.add("buildRunSeq");
		}

		// 빌드 레지스트리 번호
		if(pipelineFlow.getBuildRegistrySeq() == null){
			invalidParams.add("buildRegistrySeq");
		}
		// 빌드 레지스트리명
		if(StringUtils.isBlank(pipelineFlow.getBuildRegistryName())){
			invalidParams.add("buildRegistryName");
		}
		// 빌드 이미지명
		if(StringUtils.isBlank(pipelineFlow.getBuildImageName())){
			invalidParams.add("buildImageName");
		}
		// 빌드 이미지 태그명
		if(StringUtils.isBlank(pipelineFlow.getBuildImageTag())){
			invalidParams.add("buildImageTag");
		}

		// 배포 레지스트리명
		if(StringUtils.isBlank(pipelineFlow.getDeployRegistryName())){
			invalidParams.add("deployRegistryName");
		}

	}

	/**
	 * 파이프라인 목록 조회
	 * (appmapSeq)
	 *
	 * @param servicemapSeq
	 * @return
	 * @throws Exception
	 */
	public List<PipelineWorkloadVO> getPipelineWorkloadsByServicemap(Integer servicemapSeq, ExecutingContextVO context) throws Exception{
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		IClusterMapper clusterDao = this.sqlSession.getMapper(IClusterMapper.class);

		// 클러스터 정보 조회
		ClusterVO clusterVO = clusterDao.getClusterByServicemap(servicemapSeq);

		// namespace 조회
		String namespaceName = clusterVO.getNamespaceName();

		// namespace의 서버들의 정보 조회
		ServerStateVO serverState = servicemapService.getWorkloadsInNamespace(servicemapSeq, context);
		List<ComponentVO> components = serverState.getComponents();

		// componentSeq 별 Map 저장
		Map<String, ComponentVO> componentMap = new HashMap<>();
		if(CollectionUtils.isNotEmpty(components)) {
			componentMap = components.stream().collect(Collectors.toMap(ComponentVO::getComponentName, Function.identity()));
		}

		// namespace 에 해당하는 pipeline 목록 조회
		List<PipelineWorkloadVO> pipelineWorkloads = pipelineDao.getPipelineWorkloads(clusterVO.getClusterSeq(), namespaceName);

		List<PipelineWorkloadVO> returnWorkloads = new ArrayList<>(); //return할 워크로드 저장 list

		// 각 워크로드의 워크로드 타입과 상태 조회
		if(CollectionUtils.isNotEmpty(pipelineWorkloads) && CollectionUtils.isNotEmpty(components)){
			ComponentVO tmpComponent = null;
			for(PipelineWorkloadVO workload : pipelineWorkloads){
				// 워크로드 상태 셋팅
				tmpComponent = componentMap.get(workload.getWorkloadName());

				// 조회한 component가 존재하지 않거나, 상태가 Stopped인 workload는 제외 시킨다.
				if(tmpComponent == null){
					continue;
				} else {
					if(StringUtils.equals(tmpComponent.getStateCode(), "STOPPED")){
						this.removePipelineByComponent(tmpComponent, context);
						continue;
					}
				}

				returnWorkloads.add(workload);
				workload.setWorkloadType(tmpComponent.getWorkloadType());
				workload.setWorkloadStateCode(tmpComponent.getStateCode());

				// 컨테이너별 빌드 정보 셋팅
				BuildRunVO buildRun = null;
				BuildRunVO latestBuildRun = null;
				List<PipelineContainerVO> containers = workload.getPipelineContainers();
				if(CollectionUtils.isNotEmpty(containers)){

					for(PipelineContainerVO container: containers){

						// container 상태 체크
						container.setContainerState(PipelineContainerState.getState(container.getPipelineType(), container.getBuildState(), container.getDeployState()));

						// 파이프라인이 빌드 유형일 경우, 빌드 태그 & 빌드 실행 설명 정보 셋팅
						if(container.getPipelineType() == PipelineType.BUILD_DEPLOY) {
							// 빌드실행 tagName & runDesc 셋팅
							buildRun = buildService.getBuildRun(container.getBuildRunSeq());

							if (buildRun != null) {
								container.setBuildRunDesc(buildRun.getRunDesc());
								container.setBuildTagName(buildRun.getTagName());

								/**
								 * 빌드취소 액션 관련 로직 추가, coolingi, 2022-03-07
								 * 빌드실행 정보조회에 CANCEL Action이 없다면, container의 Action 정보에서도 제거한다.
								 */
								if (container.getContainerState() == PipelineContainerState.BUILDING
										&& container.getPossibleActions().contains(PipelineContainerAction.BUILD_CANCEL)
										&& !buildRun.getPossibleActions().contains(BuildAction.CANCEL)){
									container.getPossibleActions().remove(PipelineContainerAction.BUILD_CANCEL);
								}

								// 빌드 deploy 정보에 빌드실행설명 추가
								if (container.getBuildRunSeq().equals(container.getDeployBuildRunSeq())) {
									container.setDeployBuildRunDesc(buildRun.getRunDesc());
								}else{
									buildRun = buildService.getBuildRun(container.getDeployBuildRunSeq());
									container.setDeployBuildRunDesc(buildRun.getRunDesc());
								}

								/**
								 * 파이프라인의 이미지 URL과 동일한 빌드 이력중,
								 * 파이프라인에 설정된 이미지보다 더 최신의 빌드된 이미지가 존재하면 최신의 이미지 정보를 설정한다.
								 */
								// imageURL 동일한 최신 빌드성공 이력 조회
								latestBuildRun = buildService.getBuildRunsByImageUrl(buildRun.getBuildSeq(), buildRun.getImageUrl());

								// 현재 파이프라인에 설정된 빌드 이력 정보가 최신이 아니면 최신빌드이력 pipeline container에 셋팅
								if(latestBuildRun != null && !latestBuildRun.getBuildRunSeq().equals(buildRun.getBuildRunSeq())){
									container.setUpdateTarget(true);
									container.setDeployBuildRun(buildRun);
									container.setLatestBuildRun(latestBuildRun);
								}
							}
						}

					} // end for

				}
			}
		}

		return returnWorkloads;
	}


	/**
	 * Pipeline workload 정보 조회
	 *
	 * @param pipelineWorkloadSeq
	 * @return
	 */
	public PipelineWorkloadVO getPipelineWorkload(Integer pipelineWorkloadSeq){
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		return pipelineDao.getPipelineWorkload(pipelineWorkloadSeq);
	}

	/**
	 * ComponentSeq 에 의한 Pipeline workload 정보 조회
	 *
	 * @param component
	 * @return
	 */
	public PipelineWorkloadVO getPipelineWorkloadByComponent(ComponentVO component){
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		return pipelineDao.getPipelineWorkloadByWorkload(component.getClusterSeq(), component.getNamespaceName(), component.getComponentName());
	}

	/**
	 * Pipeline Container Seq로 해당하는 accountSeq 정보 조회
	 *
	 * @param pipelineContainerSeq
	 * @return
	 */
	public Integer getAccountSeqByPipelineContainerSeq(Integer pipelineContainerSeq){
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		return pipelineDao.getAccountSeqByPipelineContainerSeq(pipelineContainerSeq);
	}

	/**
	 * Pipeline Container 정보 조회
	 *
	 * @param pipelineContainerSeq
	 * @return
	 */
	public PipelineContainerVO getPipelineContainer(Integer pipelineContainerSeq){
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		return pipelineDao.getPipelineContainer(pipelineContainerSeq);
	}

	public List<PipelineRunVO> getPipelineRun(Integer pipelineContainerSeq){
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		return pipelineDao.getPipelineRunOnRunning(pipelineContainerSeq);
	}

	/**
	 * pipeline 실행중인 정보중 RunType에 따른 필터링
	 *
	 * @param pipelineContainerSeq
	 * @param runType
	 * @return
	 */
	public PipelineRunVO getPipelineRun(Integer pipelineContainerSeq, PipelineRunType runType){
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		PipelineRunVO rtnVO = null;
		List<PipelineRunVO> pipelineRuns = pipelineDao.getPipelineRunOnRunning(pipelineContainerSeq);

		if(CollectionUtils.isNotEmpty(pipelineRuns)){
			for(PipelineRunVO pipelineRun : pipelineRuns){
				if(runType == pipelineRun.getRunType()){
					rtnVO = pipelineRun;
					break;
				}
			}
		}
		return rtnVO;
	}

	/**
	 * 서버 삭제시 파이프라인 삭제 처리
	 * (component_seq 이용)
	 *
	 * @param serverParam
	 * @param context
	 * @throws Exception
	 */
	public void removePipelineByComponent(ServerDetailVO serverParam, ExecutingContextVO context) throws Exception {

		if(serverParam.getComponent() != null && serverParam.getComponent().getComponentSeq() != null){
            // 파이프라인 실행중인지 체크
            if(this.checkPipelineOnRunning(serverParam.getComponent())){
                throw new CocktailException("Pipeline is running!!", ExceptionType.PipelineRunning);
            }

			this.removePipelineByComponent(serverParam.getComponent(), context);
		}
	}

//	@Transactional
	public void removePipelineByComponent(ComponentVO component, ExecutingContextVO context) throws Exception{
		log.debug("[BEGIN] removePipelineByComponent, component : {}", component);

		this.removePipeline(component.getClusterSeq(), component.getNamespaceName(), component.getComponentName(), context);

		log.debug("[END] removePipelineByComponent");

	}

	public void removePipeline(Integer clusterSeq, String namespaceName, String workloadName, ExecutingContextVO context) throws Exception{
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

		PipelineWorkloadVO pipelineWorkload = pipelineDao.getPipelineWorkloadByWorkload(clusterSeq, namespaceName, workloadName);

        if(pipelineWorkload != null){
            List<PipelineContainerVO> pipelineContainers = pipelineWorkload.getPipelineContainers();

            // pipeline server의 해당 instance 삭제
            for(PipelineContainerVO container : pipelineContainers){
                if(container.getPipelineType() == PipelineType.BUILD_DEPLOY){
                    PipelineRunState runState = pipelineAPIService.terminatePipelineBuild(container);
                    if(runState == PipelineRunState.ERROR){
                        log.error("Deleting a pipeline-build-instance is Failed.{}", container);
                    }
                }
            }

			int result = 0;
            // pipeline container 정보가 존재하면 삭제
            if(CollectionUtils.isNotEmpty(pipelineWorkload.getPipelineContainers())){
                pipelineDao.deletePipelineRunByWorkload(clusterSeq, namespaceName, workloadName, context.getUserSeq()); // 실행정보 삭제
                pipelineDao.deletePipelineContainerByWorkload(clusterSeq, namespaceName, workloadName, context.getUserSeq()); // container 정보 삭제
            }

            result += pipelineDao.deletePipelineWorkloadByWorkload(clusterSeq, namespaceName, workloadName, context.getUserSeq());
			log.debug("pipeline 정보 삭제 완료 : clusterSeq[{}], namespaceName[{}], workloadName[{}], pipeline seq[{}], delete count[{}]", clusterSeq, namespaceName, workloadName, pipelineWorkload.getPipelineWorkloadSeq(), result);
        }

	}

	/**
	 * 동시빌드수 체크하여 pipeline 실행이 가능한지 체크하는 로직
	 *
	 * @param accountSeq
	 * @return 성공이면 true 리턴
	 * @throws Exception 빌드 불가능할시 Exception 발생
	 */
	public boolean checkPossibleBuildBySystem(Integer accountSeq) throws Exception{

		// 빌드 가능한지 체크, 불가능 하면 Exception 발생
		buildService.checkPossibleRunBuildBySystem(accountSeq);

		return true;
	}

	/**
	 * 현재 실행중인 빌드가 있는지 체크하여 빌드실행 가능한지 체크하는 로직
	 *
	 * @param buildSeq
	 * @param pipelineWorkloadSeq
	 * @throws Exception
	 */
	public void checkPossibleRunBuildByBuild(Integer buildSeq, Integer pipelineWorkloadSeq) throws Exception {
		// 현재 실행중인 빌드가 있는지 체크, 불가능 하면 Exception 발생
		buildService.checkPossibleRunBuildByBuild(buildSeq, pipelineWorkloadSeq);
	}

	public PipelineContainerVO checkBuildRunPipelineValidation(Integer pipelineContainerSeq) throws Exception {
		/***************** Validation Check Start *****************/
		// pipeline 정보조회
		PipelineContainerVO pipelineContainer = this.getPipelineContainer(pipelineContainerSeq);

		// validation, pipeline 정보가 없거나 빌드 가능한 파이프라인이 아니면 Exception 처리
		if(pipelineContainer == null || pipelineContainer.getPipelineType() != PipelineType.BUILD_DEPLOY){
			throw new CocktailException("실행할 수 없는 pipeline 상태 입니다.", ExceptionType.InvalidState);
		}

		PipelineWorkloadVO pipelineWorkload = this.getPipelineWorkload(pipelineContainer.getPipelineWorkloadSeq());

		// component 상태 체크(workload 존재하는 지 체크)
		if (!serverValidService.checkServerIfExists(pipelineWorkload.getClusterSeq(), pipelineWorkload.getNamespaceName(), pipelineWorkload.getWorkloadName(), true, false)) {
			throw new CocktailException("does not Exists workload!!!", ExceptionType.ServerNotFound, pipelineWorkload);
		}

		// 동시빌드 갯수 체크
		Integer accountSeq = this.getAccountSeqByPipelineContainerSeq(pipelineContainerSeq);
		this.checkPossibleBuildBySystem(accountSeq); // 빌드 불가시 Exception 발생

		// 빌드 실행 가능여부 체크
		this.checkPossibleRunBuildByBuild(pipelineContainer.getBuildSeq(), pipelineContainer.getPipelineWorkloadSeq());

		// 파이프라인 실행중인지 체크
		if(this.checkPipelineOnRunning(pipelineWorkload)){
			throw new CocktailException("Pipeline is running!!", ExceptionType.PipelineRunning);
		}
		/***************** Validation Check End *****************/

		return pipelineContainer;
	}

	public PipelineContainerVO checkCancelPipelineValidation(Integer pipelineContainerSeq) throws Exception {
		/***************** Validation Check Start *****************/
		// pipeline 정보조회
		PipelineContainerVO pipelineContainer = this.getPipelineContainer(pipelineContainerSeq);

		// pipeline 정보가 없거나
		// 빌드 가능한 파이프라인이 아니거나
		// 파이프라인의 빌드가 실행중이 아니면 Exception 처리
		if(pipelineContainer == null || pipelineContainer.getPipelineType() != PipelineType.BUILD_DEPLOY || pipelineContainer.getBuildState() != PipelineRunState.RUNNING){
			throw new CocktailException("실행할 수 없는 pipeline 상태 입니다.", ExceptionType.InvalidState);
		}

		PipelineWorkloadVO pipelineWorkload = this.getPipelineWorkload(pipelineContainer.getPipelineWorkloadSeq());

		// component 상태 체크(workload 존재하는 지 체크)
		if (!serverValidService.checkServerIfExists(pipelineWorkload.getClusterSeq(), pipelineWorkload.getNamespaceName(), pipelineWorkload.getWorkloadName(), true, false)) {
			throw new CocktailException("does not Exists workload!!!", ExceptionType.ServerNotFound, pipelineWorkload);
		}

		/***************** Validation Check End *****************/

		return pipelineContainer;
	}

	/**
	 *
	 * pipeline workload를 배포하는 메서드, pipelineContainer의 이미지를 교체한다.<br/>
	 * ** 현재 파이프라인은 cluster에 존재하는 workload만 데이터가 존재함. **
	 *
	 * @param workload
	 * @param pipelineContainer
	 * @param ctx
	 * @return
	 */
	public PipelineContainerVO deployByPipeline(PipelineWorkloadVO workload, PipelineContainerVO pipelineContainer, ExecutingContextVO ctx) {

		try {

			// K8s에서 워크로드 조회
			ServerIntegrateVO serverCurrent = serverService.getWorkloadDetailByNamespace(DeployType.GUI, workload.getClusterSeq(), workload.getNamespaceName(), workload.getWorkloadName());
			ServerGuiVO serverGuiVO = (ServerGuiVO)serverCurrent;
			ComponentVO componentVO = serverGuiVO.getComponent();

			/**
			 * api version 체크
			 */
			serverValidService.checkServerApiVersion(serverGuiVO.getServer().getWorkloadType(), serverGuiVO.getServer().getWorkloadVersion(), serverGuiVO.getComponent().getClusterSeq(), ctx);

			List<ContainerVO> containers = new ArrayList<>();
			ResourceUtil.mergeContainer(containers, serverGuiVO.getInitContainers(), serverGuiVO.getContainers());

			if(CollectionUtils.isNotEmpty(containers)){
				for(ContainerVO containerVO:containers){
					if(StringUtils.equals(containerVO.getContainerName(), pipelineContainer.getContainerName())){
						containerVO.setFullImageName(pipelineContainer.getDeployImageUrl());
						containerVO.setImagePullPolicy(ImagePullPolicyType.Always.getCode());

						if(pipelineContainer.getPipelineType() == PipelineType.BUILD_DEPLOY) {
							containerVO.setBuildSeq(pipelineContainer.getBuildSeq());
						}
						break;
					}
				}
			}

			// 파이프라인 실행 여부 셋팅
			ctx.setPipelineYn("Y");
			serverService.updateWorkload(componentVO.getClusterSeq(), componentVO.getNamespaceName(), componentVO, serverGuiVO, DeployMode.ASYNC, ctx);
			pipelineContainer.setDeployState(PipelineRunState.DONE);

		} catch (Exception e) {
			log.error("Pipeline deploying Error!!.", e);
			pipelineContainer.setDeployState(PipelineRunState.ERROR);
		}

		return pipelineContainer;
	}

	/**
	 * 빌드 시작시 pipeline run & pipeline container Build 상태 변경 메서드
	 *
	 * @param pipelineContainerVO
	 * @return
	 */
	@Transactional(transactionManager = "transactionManager")
	public int updatePipelineBuildState(PipelineContainerVO pipelineContainerVO){
		int cnt = 0;
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		PipelineRunVO pipelineRunVO = this.getPipelineRun(pipelineContainerVO.getPipelineContainerSeq(), PipelineRunType.BUILD);

		if(pipelineRunVO != null) {
			pipelineRunVO.setRunState(pipelineContainerVO.getBuildState());
			cnt += pipelineDao.updatePipelineRunState(pipelineRunVO);
			cnt += pipelineDao.updatePipelineContainerBuildState(pipelineRunVO);
		}

		return cnt;
	}

	/**
	 * 빌드 시작시 pipeline run & pipeline container Deploy 상태 변경 메서드
	 *
	 * @param pipelineContainerVO
	 * @return
	 */
	@Transactional(transactionManager = "transactionManager")
	public int updatePipelineDeployState(PipelineContainerVO pipelineContainerVO){
		int cnt = 0;
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		PipelineRunVO pipelineRunVO = this.getPipelineRun(pipelineContainerVO.getPipelineContainerSeq(), PipelineRunType.DEPLOY);

		if(pipelineRunVO != null) {
			pipelineRunVO.setRunState(pipelineContainerVO.getDeployState());
			cnt += pipelineDao.updatePipelineRunState(pipelineRunVO);
			cnt += pipelineDao.updatePipelineContainerDeployState(pipelineRunVO);
		}

		return cnt;
	}

	@Transactional(transactionManager = "transactionManager")
	public int updatePipelineDeploy(PipelineContainerVO pipelineContainer){
		int cnt = 0;
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

		cnt += pipelineDao.updatePipelineContainerDeploy(pipelineContainer);

		return cnt;
	}

	/**
     * component로 파이프라인이 실행중인지 판단하여 true/false 리턴
     *
     * @param component
     * @return
     * @throws Exception
     */
	public boolean checkPipelineOnRunning(ComponentVO component) throws Exception{
		Integer clusterSeq = component.getClusterSeq();
		String namespaceName = component.getNamespaceName();
		String workloadName = component.getComponentName();

        return this.checkPipelineOnRunning(clusterSeq, namespaceName, workloadName);
    }

	/**
	 * workload로 파이프라인이 실행중인지 판단하여 true/false 리턴
	 *
	 * @param workload
	 * @return
	 * @throws Exception
	 */
	public boolean checkPipelineOnRunning(PipelineWorkloadVO workload) throws Exception{
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

		Integer clusterSeq = workload.getClusterSeq();
		String namespaceName = workload.getNamespaceName();
		String workloadName = workload.getWorkloadName();

		return this.checkPipelineOnRunning(clusterSeq, namespaceName, workloadName);

	}

	/**
	 * cluster, namespace, workload에 해당하는 pipeline이 실행 중인지 체크
	 *
	 * @param clusterSeq
	 * @param namespaceName
	 * @param workloadName
	 * @return
	 * @throws Exception
	 */
	public boolean checkPipelineOnRunning(Integer clusterSeq, String namespaceName, String workloadName) throws Exception {
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

		PipelineRunVO pipelineRun = pipelineDao.getPipelineOnRunningByWorkload(clusterSeq, namespaceName, workloadName, Arrays.asList("WAIT", "RUNNING"));

		if (pipelineRun != null) {
			// BUILD_DEPLOY 이고 빌드에서 오류가 발생한 경우 build-api가 정상적이지 않을 시 상태가 ERROR가 아닐 수 있으므로 buildState를 확인하여 체크하여 줌
			if (StringUtils.isNotBlank(pipelineRun.getPipelineType()) && PipelineType.valueOf(pipelineRun.getPipelineType()) == PipelineType.BUILD_DEPLOY) {
				// pipeline_container.build_state가 'ERROR'이면
				if (StringUtils.isNotBlank(pipelineRun.getBuildState()) && PipelineRunState.valueOf(pipelineRun.getBuildState()) == PipelineRunState.ERROR) {
					// 빌드가 오류이면 deploy는 DONE 처리해줌
					pipelineRun.setRunState(PipelineRunState.DONE);
					pipelineDao.updatePipelineRunState(pipelineRun);
					return false;
				}
				// pipeline_run.run_state가 'WAIT'이면
				if (pipelineRun.getRunState() == PipelineRunState.WAIT) {
					if (pipelineRun.getRunType() == PipelineRunType.DEPLOY && StringUtils.isNotBlank(pipelineRun.getDeployContent())) {
						// 파이프라인 실행할 때 입력된 정보로 build 상태를 체크하여 'ERROR' 라면 실행 중으로 간주하지 않고 'DONE'으로 변경
						PipelineRunVO pipelineRunDeploy = JsonUtils.fromGson(pipelineRun.getDeployContent(), PipelineRunVO.class);
						if (pipelineRunDeploy.getPipelineContainerSeq() != null && pipelineRunDeploy.getBuildSeq() != null && pipelineRunDeploy.getBuildRunSeq() != null) {
							PipelineRunVO pipelineRunByError = pipelineDao.getPipelineOnRunningByBuildRunSeq(pipelineRunDeploy.getPipelineContainerSeq(), pipelineRunDeploy.getBuildSeq(), pipelineRunDeploy.getBuildRunSeq(), Arrays.asList("ERROR"));
							if (pipelineRunByError != null && pipelineRunByError.getRunState() == PipelineRunState.ERROR) {
								// 빌드가 오류이면 deploy는 DONE 처리해줌
								pipelineRun.setRunState(PipelineRunState.DONE);
								pipelineDao.updatePipelineRunState(pipelineRun);
								return false;
							} else {
								return true;
							}
						} else {
							return true;
						}
					} else {
						return true;
					}
				} else {
					return false;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	/**
	 * serviceSeq & registrySeq로 사용하는 pipeline이 있는지 체크
	 *
	 * @param serviceSeq
	 * @param registrySeqs
	 * @return
	 * @throws Exception
	 */
	public boolean checkPipelineUsingServiceSeqAndRegistryIds(Integer serviceSeq, List<Integer> registrySeqs, List<Integer> externalRegistrySeqs) throws Exception{
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		int cnt = pipelineDao.getPipelineCountByServiceSeqAndRegistryIds(serviceSeq, registrySeqs, externalRegistrySeqs);

		return (cnt > 0);
	}

	/**
	 * 이전 빌드 연결된 파이프라인 정보 조회
	 *
	 * @param workloadName
	 * @param containerName
	 * @param deployImageUrl
	 * @return 이전 pipeline 정보가 빌드 연결된 파이프라인 정보이면 PipelineContainerVO 리턴, 아니면 null 리턴
	 */
	public PipelineContainerVO getPreviousPipelineContainer(String workloadName, String containerName, String deployImageUrl){
		IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);
		PipelineContainerVO pipelineContainer = null;

		// 이전 pipeline container seq 조회,
		Integer pipelineContainerSeq = pipelineDao.getPreviousPipelineContainerSeqByImageUrlAndNames(workloadName, containerName, deployImageUrl);
		if(pipelineContainerSeq > 0){
			PipelineContainerVO tmpPipelineContainer = this.getPipelineContainer(pipelineContainerSeq);
			if(tmpPipelineContainer.getPipelineType() == PipelineType.BUILD_DEPLOY){
				pipelineContainer = tmpPipelineContainer;
			}
		}
		return pipelineContainer;
	}



	/**
	 * pipeline server로 부터 전달받은 응답처리.
	 * Build & Deploy 일때 호출된다.
	 *
	 * @param pipelineContainerSeq
	 * @param pipelineResult
	 * @param ctx
	 * @throws InvalidProtocolBufferException
	 */
	public void handleResult(Integer pipelineContainerSeq, String pipelineResult, ExecutingContextVO ctx) throws Exception {
		// pipeline response 생성
		PipelineAPIServiceProto.Pipeline pipeline = PipelineTypeConverter.convertVO(pipelineResult, PipelineAPIServiceProto.Pipeline.newBuilder()).build();

		// pipeline command 생성
		PipelineCommandVO pipelineCommandVO = PipelineTypeConverter.convertToPipelineCommandVO(pipeline);

		// pipeline 조회
		PipelineContainerVO pipelineContainer = this.getPipelineContainer(pipelineContainerSeq);

		// updater 셋팅, parameter context와 혅재 실행중인 context 두개다 셋팅
		ctx.setUserSeq(pipelineContainer.getUpdater());
		ContextHolder.exeContext().setUserSeq(pipelineContainer.getUpdater()); // 여기를 셋팅하지 않으면 deploy시 workload update 하다가 exception 발생함. bug-fixed

		// pipeline workload 조회
		PipelineWorkloadVO pipelineWorkload = this.getPipelineWorkload(pipelineContainer.getPipelineWorkloadSeq());
		log.info("handleResult - pipelineContainerSeq : {}, BuildRunSeq : {}, pipelineResult : \n{}", pipelineContainer.getPipelineContainerSeq(), pipelineContainer.getBuildRunSeq(), pipelineResult);

		// Deploy가 아니고 추가 완료 call 이 아니면 build 처리 호출
		// RunState 가 DONE 일때는 pipeline으로 부터 finishAt 값을 받았을때임, finishAt은 모든 task를 다 처리하고 마지막에 한번 더 호출될때 값이 들어옴.
		if(pipelineCommandVO.getStepType() != StepType.DEPLOY){
			// build 관련해서는 handleBuildResult 메서드에서 처리됨.
			buildService.handleBuildResult(pipelineContainer.getBuildRunSeq(), pipelineResult);
		}

		if(pipelineCommandVO.getRunState() == RunState.ERROR) {
			pipelineContainer.setBuildState(PipelineRunState.ERROR);
			this.updatePipelineBuildState(pipelineContainer);
			pipelineContainer.setDeployState(PipelineRunState.DONE); // deploy 상태는 DONE으로 처리
			this.updatePipelineDeployState(pipelineContainer);

		}else if(pipelineCommandVO.getRunState() == RunState.DONE){	// pipeline server 작업이 완료 되었을때는 workload 배포 처리

			PipelineContainerVO deployContainer = new PipelineContainerVO();

			try {
				// build 상태 update
				pipelineContainer.setBuildState(PipelineRunState.DONE);
				this.updatePipelineBuildState(pipelineContainer);

				// deploy 상태 update
				pipelineContainer.setDeployState(PipelineRunState.RUNNING);
				this.updatePipelineDeployState(pipelineContainer);

				// deploy 정보 셋팅
				BeanUtils.copyProperties(pipelineContainer, deployContainer);

				// 배포정보 설정
				deployContainer.setDeployBuildRunSeq(pipelineContainer.getBuildRunSeq());
				deployContainer.setDeployImageName(pipelineContainer.getBuildImageName());
				deployContainer.setDeployImageTag(pipelineContainer.getBuildImageTag());
				deployContainer.setDeployImageUrl(pipelineContainer.getBuildImageUrl());
				deployContainer.setDeployRegistryName(pipelineContainer.getBuildRegistryName());
				deployContainer.setDeployRegistrySeq(pipelineContainer.getBuildRegistrySeq());

				// workload deploy
				deployByPipeline(pipelineWorkload, deployContainer, ctx);

				// 성공시 배포 정보 update
				if(deployContainer.getDeployState() == PipelineRunState.DONE){
					pipelineContainer.setDeployBuildRunSeq(deployContainer.getDeployBuildRunSeq());
					pipelineContainer.setDeployImageName(deployContainer.getDeployImageName());
					pipelineContainer.setDeployImageTag(deployContainer.getDeployImageTag());
					pipelineContainer.setDeployImageUrl(deployContainer.getDeployImageUrl());
					pipelineContainer.setDeployRegistryName(deployContainer.getDeployRegistryName());
					pipelineContainer.setDeployRegistrySeq(deployContainer.getDeployRegistrySeq());
				}

			}catch(Exception e){
				log.error("Pipeline deploying Error!!.", e);
				deployContainer.setDeployState(PipelineRunState.ERROR);
			}

			pipelineContainer.setDeployState(deployContainer.getDeployState());

			// deploy 정보 update
			this.updatePipelineDeploy(pipelineContainer);

			// 배포 상태 update
			this.updatePipelineDeployState(pipelineContainer);
		}

		// pipeline 조회
		pipelineContainer = this.getPipelineContainer(pipelineContainer.getPipelineContainerSeq());
		log.debug("The pipeline container data after processing.\n"+pipelineContainer);
	}

}
