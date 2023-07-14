package run.acloud.api.pl.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.cserver.vo.ServerIntegrateVO;
import run.acloud.api.cserver.vo.ServerYamlVO;
import run.acloud.api.pl.enums.PlRunType;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.api.pl.service.PlEventService;
import run.acloud.api.pl.service.PlRunDeployService;
import run.acloud.api.pl.service.PlService;
import run.acloud.api.pl.vo.*;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.PagingUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@Tag(name = "Pipeline", description = "파이프라인 관련 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/pl")
public class PlController {

	@Autowired
    private PlService plService;

	@Autowired
    private PlRunDeployService plRunDeployService;

	@PostMapping(value = "/servicemap/{servicemapSeq}")
	@Operation(summary = "파이프라인 생성(appmap)", description = "파이프라인을 생성한다.")
	public PlMasterVO addPipelineByServicemap(
			@Parameter(name = "appmapSeq", description = "appmap 번호", required = true) @PathVariable Integer servicemapSeq,
			@Parameter(name = "파이프라인 생성 모델", description = "파이프라인 생성 모델", required = true) @RequestBody PlMasterVO plMaster
	) throws Exception {
		log.debug("[BEGIN] addPipeline");

		plMaster = plService.addPipeline(servicemapSeq, plMaster);

		log.debug("[END  ] addPipeline");

		return plMaster;
	}

	@PostMapping(value = "/cluster/{clusterSeq}/namespace/{namespaceName}")
	@Operation(summary = "파이프라인 생성", description = "파이프라인을 생성한다.")
	public PlMasterVO addPipelineByNamespace(
			@Parameter(name = "clusterSeq", description = "cluster 번호", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "namespaceName", description = "namespace 명", required = true) @PathVariable String namespaceName,
			@Parameter(name = "파이프라인 생성 모델", description = "파이프라인 생성 모델", required = true) @RequestBody PlMasterVO plMaster
	) throws Exception {
		log.debug("[BEGIN] addPipeline");

		plMaster = plService.addPipeline(clusterSeq, namespaceName, plMaster);

		log.debug("[END  ] addPipeline");

		return plMaster;
	}

	@GetMapping(value = "/list/account/{accountSeq}")
	@Operation(summary = "파이프라인 목록 조회", description = "파이프라인을 목록을 조회 한다.")
	public List<PlMasterListVO> getPlListByAccount(
			@Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable Integer accountSeq
	) throws Exception {
		log.debug("[BEGIN] getPlListByAccount");

		List<PlMasterListVO> list = plService.getPlList(accountSeq, null);

		log.debug("[END  ] getPlListByAccount");
		return list;
	}

	@GetMapping(value = "/list/service/{serviceSeq}")
	@Operation(summary = "파이프라인 목록 조회", description = "서비스 seq 이용한 파이프라인을 목록을 조회 한다.")
	public List<PlMasterListVO> getPlListByService(
			@Parameter(name = "serviceSeq", description = "workspace 번호", required = true) @PathVariable Integer serviceSeq
	) throws Exception {
		log.debug("[BEGIN] getPlListByService");

		List<PlMasterListVO> list = plService.getPlList(null, serviceSeq);

		log.debug("[END  ] getPlListByService");

		return list;
	}

	@GetMapping(value = "/{plSeq}")
	@Operation(summary = "파이프라인 상세 조회", description = "파이프라인 seq 이용한 파이프라인을 상세정보 조회 한다.")
	public PlMasterVO getPlDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq
	) throws Exception {
		log.debug("[BEGIN] getPlDetail");

		PlMasterVO detail = plService.getPlDetail(plSeq, true);

		log.debug("[END  ] getPlDetail");

		return detail;
	}

	@PutMapping(value = "/{plSeq}/name/{name}")
	@Operation(summary = "파이프라인 이름 수정", description = "파이프라인 이름을 수정 한다.")
	public void editPlMasterName(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "name", description = "파이프라인 이름", required = true) @PathVariable String name
	) throws Exception {
		log.debug("[BEGIN] editPlMasterName");

		plService.editPlMasterName(plSeq, name, ContextHolder.exeContext());

		log.debug("[END  ] editPlMasterName");

	}

	@DeleteMapping(value = "/{plSeq}")
	@Operation(summary = "파이프라인 삭제", description = "파이프라인 seq 이용한 파이프라인을 삭제 한다.")
	public PlMasterVO deletePlMaster(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq
	) throws Exception {
		log.debug("[BEGIN] deletePlMaster");

		PlMasterVO detail = plService.deletePlMaster(plSeq, ContextHolder.exeContext());

		log.debug("[END  ] deletePlMaster");

		return detail;
	}

	@GetMapping(value = "/{plSeq}/current")
	@Operation(summary = "파이프라인 배포현황 조회", description = "파이프라인 seq 이용한 파이프라인을 배포현황을 조회 한다.")
	public CurrentDeployVO getPlCurrentResources(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq
	) throws Exception {
		log.debug("[BEGIN] getPlCurrentResources");

		CurrentDeployVO detail = plService.getCurrentResources(plSeq);

		log.debug("[END  ] getPlCurrentResources");

		return detail;
	}

	@GetMapping(value = "{plSeq}/build/{plResBuildSeq}")
	@Operation(summary = "파이프라인 빌드 상세 조회", description = "파이프라인 빌드 상세정보 조회 한다.")
	public PlResBuildVO getPlResBuild(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResBuildSeq", description = "파이프라인 빌드 번호", required = true) @PathVariable Integer plResBuildSeq
	) throws Exception {
		log.debug("[BEGIN] getPlResBuild");

		PlResBuildVO detail = null;
		detail = plService.getPlResBuild(plSeq, plResBuildSeq);

		log.debug("[END  ] getPlResBuild");

		return detail;
	}



	@Operation(summary = "파이프라인 리소스 상세조회 - 워크로드", description = "파이프라인 리소스 상세조회 - 워크로드")
	@GetMapping("/{plSeq}/res/{plResDeploySeq}/deploy/{deployType}/workload")
	public ServerIntegrateVO getWorkloadDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq,
			@Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI, DeployType.Names.YAML}), required = true) @PathVariable String deployType
	) throws Exception {

		log.debug("[BEGIN] getWorkloadDeployResourceDetail");

		ServerIntegrateVO result = plService.getWorkloadDeployResourceDetail(plSeq, plResDeploySeq, deployType);

		log.debug("[END  ] getWorkloadDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 리소스 상세조회 - 컨피그맵", description = "파이프라인 리소스 상세조회 - 컨피그맵")
	@GetMapping("/{plSeq}/res/{plResDeploySeq}/configmap")
	public ConfigMapGuiVO getConfigMapDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getConfigMapDeployResourceDetail");

		ConfigMapGuiVO result = plService.getConfigMapDeployResourceDetail(plSeq, plResDeploySeq);

		log.debug("[END  ] getConfigMapDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 리소스 상세조회 - 시크릿", description = "파이프라인 리소스 상세조회 - 시크릿")
	@GetMapping("/{plSeq}/res/{plResDeploySeq}/secret")
	public SecretGuiVO getSecretDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getSecretDeployResourceDetail");

		SecretGuiVO result = plService.getSecretDeployResourceDetail(plSeq, plResDeploySeq);

		log.debug("[END  ] getSecretDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 리소스 상세조회 - 서비스", description = "파이프라인 리소스 상세조회 - 서비스")
	@GetMapping("/{plSeq}/res/{plResDeploySeq}/service")
	public K8sServiceVO getServiceDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getServiceDeployResourceDetail");

		K8sServiceVO result = plService.getServiceDeployResourceDetail(plSeq, plResDeploySeq);

		log.debug("[END  ] getServiceDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 리소스 상세조회 - 인그레스", description = "파이프라인 리소스 상세조회 - 인그레스")
	@GetMapping("/{plSeq}/res/{plResDeploySeq}/ingress")
	public K8sIngressVO getIngressDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getIngressDeployResourceDetail");

		K8sIngressVO result = plService.getIngressDeployResourceDetail(plSeq, plResDeploySeq);

		log.debug("[END  ] getIngressDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 리소스 상세조회 - PVC", description = "파이프라인 리소스 상세조회 - PVC")
	@GetMapping("/{plSeq}/res/{plResDeploySeq}/pvc")
	public K8sPersistentVolumeClaimVO getPvcDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getPvcDeployResourceDetail");

		K8sPersistentVolumeClaimVO result = plService.getPvcDeployResourceDetail(plSeq, plResDeploySeq);

		log.debug("[END  ] getPvcDeployResourceDetail");

		return result;
	}

    @Operation(summary = "파이프라인 워크로드 리소스 정보 수정", description = "파이프라인 워크로드 리소스 정보 수정")
    @PutMapping("/{plSeq}/res/{plResDeploySeq}/deploy/{deployType}/workload/{workloadName}/edit")
    public void editWorkloadDeployResource(
            @Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq,
            @Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI, DeployType.Names.YAML}), required = true) @PathVariable String deployType,
            @Parameter(name = "workloadName", description = "workloadName", required = true) @PathVariable String workloadName,
            @Parameter(name = "serverParam", description = "serverParam", required = true) @RequestBody @Validated ServerIntegrateVO serverParam
    ) throws Exception {

        try {
            log.debug("[BEGIN] editWorkloadDeployResource");

            ExecutingContextVO ctx = ContextHolder.exeContext();
            HttpServletRequest request = Utils.getCurrentRequest();
            ResultVO result = new ResultVO();
            ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
            ctx.setResult(result);

            log.info("[PARAM] IN - \n{}", JsonUtils.toGson(serverParam));

            if (DeployType.valueOf(deployType) == DeployType.GUI) {
                ServerGuiVO serverGui = (ServerGuiVO)serverParam;

                if (!workloadName.equals(serverGui.getComponent().getComponentName())) {
                    throw new CocktailException("cluster, namespace, workloadName are different from serverParam.", ExceptionType.InvalidParameter);
                }
            } else {
                ServerYamlVO serverYaml = (ServerYamlVO)serverParam;

                if (!workloadName.equals(serverYaml.getWorkloadName())) {
                    throw new CocktailException("cluster, namespace, workloadName are different from serverParam.", ExceptionType.InvalidParameter);
                }
            }

            /**
             * server 수정
             */
            plService.editWorkloadDeployResource(plSeq, plResDeploySeq, serverParam, ctx);

            log.info("[PARAM] OUT - \n{}", JsonUtils.toGson(serverParam));

        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
        }finally {
            log.debug("[END  ] editWorkloadDeployResource");
        }

    }

	@Operation(summary = "파이프라인 컨피그맵 리소스 정보 수정", description = "파이프라인 컨피그맵 리소스 정보 수정")
	@PutMapping("/{plSeq}/res/{plResDeploySeq}/deploy/{deployType}/configmap/{configMapName}/edit")
	public void editConfigMapDeployResource(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq,
			@Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI, DeployType.Names.YAML}), required = true) @PathVariable String deployType,
			@Parameter(name = "configMapName", description = "configMapName", required = true) @PathVariable String configMapName,
			@Parameter(name = "configMap", description = "수정하려는 configMap", required = true) @RequestBody ConfigMapIntegrateVO configMapParam
	) throws Exception {
		try {
			log.debug("[BEGIN] editConfigMapDeployResource");

			ExecutingContextVO ctx = ContextHolder.exeContext();
			HttpServletRequest request = Utils.getCurrentRequest();
			ResultVO result = new ResultVO();
			ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
			ctx.setResult(result);

			log.info("[PARAM] IN - \n{}", JsonUtils.toGson(configMapParam));

			/**
			 * configMap 수정
			 */
			plService.editConfigMapDeployResource(plSeq, plResDeploySeq, deployType, configMapName, configMapParam, ContextHolder.exeContext());

			log.info("[PARAM] OUT - \n{}", JsonUtils.toGson(configMapParam));

		}
		catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
		}finally {
			log.debug("[END  ] editConfigMapDeployResource");
		}

	}

	@Operation(summary = "파이프라인 시크릿 리소스 정보 수정", description = "파이프라인 시크릿 리소스 정보 수정")
	@PutMapping("/{plSeq}/res/{plResDeploySeq}/deploy/{deployType}/secret/{secretName}/edit")
	public void editSecretDeployResource(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq,
			@Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI, DeployType.Names.YAML}), required = true) @PathVariable String deployType,
			@Parameter(name = "secretName", description = "secretName", required = true) @PathVariable String secretName,
			@Parameter(name = "secret", description = "수정하려는 secret", required = true) @RequestBody SecretIntegrateVO secretParam
	) throws Exception {
		try {
			log.debug("[BEGIN] editSecretDeployResource");

			ExecutingContextVO ctx = ContextHolder.exeContext();
			HttpServletRequest request = Utils.getCurrentRequest();
			ResultVO result = new ResultVO();
			ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
			ctx.setResult(result);

			log.info("[PARAM] IN - \n{}", JsonUtils.toGson(secretParam));

			/**
			 * configMap 수정
			 */
			plService.editSecretDeployResource(plSeq, plResDeploySeq, deployType, secretName, secretParam, ContextHolder.exeContext());

			log.info("[PARAM] OUT - \n{}", JsonUtils.toGson(secretParam));

		}
		catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
		}finally {
			log.debug("[END  ] editSecretDeployResource");
		}

	}

	@Operation(summary = "파이프라인 서비스 리소스 정보 수정", description = "파이프라인 서비스 리소스 정보 수정")
	@PutMapping("/{plSeq}/res/{plResDeploySeq}/deploy/{deployType}/service/{serviceName}/edit")
	public void editServiceDeployResource(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq,
			@Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI, DeployType.Names.YAML}), required = true) @PathVariable String deployType,
			@Parameter(name = "serviceName", description = "serviceName", required = true) @PathVariable String serviceName,
			@Parameter(name = "service", description = "수정하려는 service", required = true) @RequestBody ServiceSpecIntegrateVO serviceParam
	) throws Exception {
		try {
			log.debug("[BEGIN] editServiceDeployResource");

			ExecutingContextVO ctx = ContextHolder.exeContext();
			HttpServletRequest request = Utils.getCurrentRequest();
			ResultVO result = new ResultVO();
			ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
			ctx.setResult(result);

			log.info("[PARAM] IN - \n{}", JsonUtils.toGson(serviceParam));

			/**
			 * configMap 수정
			 */
			plService.editServiceDeployResource(plSeq, plResDeploySeq, deployType, serviceName, serviceParam, ContextHolder.exeContext());

			log.info("[PARAM] OUT - \n{}", JsonUtils.toGson(serviceParam));

		}
		catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
		}finally {
			log.debug("[END  ] editServiceDeployResource");
		}

	}

	@Operation(summary = "파이프라인 인그레스 리소스 정보 수정", description = "파이프라인 인그레스 리소스 정보 수정")
	@PutMapping("/{plSeq}/res/{plResDeploySeq}/deploy/{deployType}/ingress/{ingressName}/edit")
	public void editIngressDeployResource(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 리소스 배포 번호", required = true) @PathVariable Integer plResDeploySeq,
			@Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI, DeployType.Names.YAML}), required = true) @PathVariable String deployType,
			@Parameter(name = "ingressName", description = "ingressName", required = true) @PathVariable String ingressName,
			@Parameter(name = "ingress", description = "수정하려는 ingress", required = true) @RequestBody IngressSpecIntegrateVO ingressParam
	) throws Exception {
		try {
			log.debug("[BEGIN] editIngressDeployResource");

			ExecutingContextVO ctx = ContextHolder.exeContext();
			HttpServletRequest request = Utils.getCurrentRequest();
			ResultVO result = new ResultVO();
			ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
			ctx.setResult(result);

			log.info("[PARAM] IN - \n{}", JsonUtils.toGson(ingressParam));

			/**
			 * configMap 수정
			 */
			plService.editIngressDeployResource(plSeq, plResDeploySeq, deployType, ingressName, ingressParam, ContextHolder.exeContext());

			log.info("[PARAM] OUT - \n{}", JsonUtils.toGson(ingressParam));

		}
		catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
		}finally {
			log.debug("[END  ] editIngressDeployResource");
		}

	}

	@DeleteMapping(value = "/{plSeq}/deploy/{plResDeploySeq}")
	@Operation(summary = "파이프라인 배포 리소스 삭제 ", description = "파이프라인 배 리소스 정보 삭제 한다. 워크로드와의 맵핑정보 존재시 맵핑정보도 삭제된다.")
	public PlResDeployVO deletePlResDeploy(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 배포 리소스 번호", required = true) @PathVariable Integer plResDeploySeq
	) throws Exception {
		try {
			log.debug("[BEGIN] deletePlResDeploy");

			ExecutingContextVO ctx = ContextHolder.exeContext();

			return plService.deletePlResDeploy(plSeq, plResDeploySeq, ctx);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonDeleteFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] deletePlResDeploy");
		}
	}

	@PostMapping(value = "/{plSeq}/deploy")
	@Operation(summary = "파이프라인 배포 리소스 추가 ", description = "파이프라인 배 리소스 정보 추가 한다. 워크로드와의 맵핑정보 존재시 맵핑정보도 추가된다.")
	public void addPlResDeploy(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploy", description = "파이프라인 배포 리소스 모델", required = true) @RequestBody PlResDeployVO plResDeploy
	) throws Exception {
		try {
			log.debug("[BEGIN] addPlResDeploy");

			plService.addPlResDeploy(plSeq, plResDeploy);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] addPlResDeploy");
		}
	}

	@PostMapping(value = "/{plSeq}/deploies")
	@Operation(summary = "파이프라인 배포 리소스 추가 ", description = "파이프라인 배 리소스 정보 추가 한다. 워크로드와의 맵핑정보 존재시 맵핑정보도 추가된다.")
	public void addPlResDeploies(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploy", description = "파이프라인 배포 리소스 모델", required = true) @RequestBody List<PlResDeployVO> plResDeploies
	) throws Exception {
		try {
			log.debug("[BEGIN] addPlResDeploies");

			plService.addPlResDeploies(plSeq, plResDeploies);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] addPlResDeploy");
		}
	}

	@PostMapping(value = "/{plSeq}/build")
	@Operation(summary = "파이프라인 빌드 리소스 추가 ", description = "파이프라인 빌드 리소스 정보트 리스트을 추가 한다.")
	public void addPlResBuilds(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResBuilds", description = "파이프라인 빌드 리소스 모델 리스트", required = true) @RequestBody List<PlResBuildVO> addResBuilds
	) throws Exception {
		try {
			log.debug("[BEGIN] addPlResBuilds");

			ExecutingContextVO ctx = ContextHolder.exeContext();

			plService.addPlResBuilds(plSeq, addResBuilds);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] addPlResBuilds");
		}
	}

	@PutMapping(value = "/{plSeq}/build/{plResBuildSeq}")
	@Operation(summary = "파이프라인 빌드 리소스 상세 수정", description = "파이프라인 빌드스 리소스 정보 수정 한다.")
	public PlResBuildVO editPlResBuild(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResBuildSeq", description = "파이프라인 빌드 리소스 번호", required = false) @PathVariable Integer plResBuildSeq,
			@Parameter(name = "파이프라인 빌드 정보 모델", description = "파이프라인 빌드 정보 모델", required = true) @RequestBody PlResBuildVO plResBuildVO
	) throws Exception {
		try {
			log.debug("[BEGIN] editPlResBuild");
			ExecutingContextVO ctx = ContextHolder.exeContext();

			PlResBuildVO detail = plService.editPlResBuild(plSeq, plResBuildSeq, plResBuildVO, ctx);

			return detail;
		}
		catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
		}finally {
			log.debug("[END  ] editPlResBuild");
		}
	}

	@DeleteMapping(value = "/{plSeq}/build/{plResBuildSeq}")
	@Operation(summary = "파이프라인 빌드 리소스 삭제 ", description = "파이프라인 빌드 리소스 정보 삭제 한다. 워크로드와의 맵핑정보 존재시 맵핑정보도 삭제된다.")
	public String deletePlResBuild(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResBuildSeq", description = "파이프라인 빌드 리소스 번호", required = true) @PathVariable Integer plResBuildSeq
	) throws Exception {
		try {
			log.debug("[BEGIN] deletePlResBuild");
			ExecutingContextVO ctx = ContextHolder.exeContext();

			return plService.deletePlResBuild(plSeq, plResBuildSeq, ctx);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonDeleteFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] deletePlResBuild");
		}
	}

	@PutMapping(value = "/{plSeq}/build/{plResBuildSeq}/runorder/{updateRunOrder}")
	@Operation(summary = "파이프라인 빌드 리소스 실행순서 변경 ", description = "파이프라인 빌드 리소스 실행순서 변경 (실행 순서 변경은 1 단위로만 가능)")
	public String editPlResBuildRunOrder(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResBuildSeq", description = "파이프라인 빌 리소스 번호", required = true) @PathVariable Integer plResBuildSeq,
			@Parameter(name = "updateRunOrder", description = "변경할 runOrder", required = true) @PathVariable Integer updateRunOrder
	) throws Exception {
		try {
			log.debug("[BEGIN] editPlResBuildRunOrder");

			ExecutingContextVO ctx = ContextHolder.exeContext();

			return plService.editPlResBuildRunOrder(plSeq, plResBuildSeq, updateRunOrder, ctx);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonDeleteFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] editPlResBuildRunOrder");
		}
	}

	@PutMapping(value = "/{plSeq}/build/{plResBuildSeq}/runYn/{runYn}")
	@Operation(summary = "파이프라인 빌드 리소스 실행단계 변경 ", description = "파이프라인 빌드 리소스 실행단계 변경")
	public String editPlResBuildRunYn(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResBuildSeq", description = "파이프라인 빌드 리소스 번호", required = true) @PathVariable Integer plResBuildSeq,
			@Parameter(name = "runYn", description = "변경할 runYn", schema = @Schema(allowableValues = {"Y","N"}), required = true) @PathVariable String runYn
	) throws Exception {
		try {
			log.debug("[BEGIN] editPlResBuildRunYn");

			ExecutingContextVO ctx = ContextHolder.exeContext();

			return plService.editPlResBuildRunYn(plSeq, plResBuildSeq, runYn, ctx);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] editPlResBuildRunYn");
		}
	}

    @PutMapping(value = "/{plSeq}/deploy/{plResDeploySeq}/workload/runorder/{updateRunOrder}")
    @Operation(summary = "파이프라인 워크로드 배포 리소스 실행순서 변경 ", description = "파이프라인 워크로드 배포 리소스 실행순서 변경 (실행 순서 변경은 1 단위 위아래로만 가능)")
    public PlResDeployVO editPlResDeployWorkloadRunOrder(
            @Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
            @Parameter(name = "plResDeploySeq", description = "파이프라인 배포 리소스 번호", required = true) @PathVariable Integer plResDeploySeq,
            @Parameter(name = "updateRunOrder", description = "변경할 runOrder", required = true) @PathVariable Integer updateRunOrder
    ) throws Exception {
        try {
            log.debug("[BEGIN] editPlResDeployWorkloadRunOrder");

            ExecutingContextVO ctx = ContextHolder.exeContext();

            return plService.editPlResDeployWorkloadRunOrder(plSeq, plResDeploySeq, updateRunOrder, ctx);

        } catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        } catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        } catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
        } finally {
            log.debug("[END  ] editPlResDeployWorkloadRunOrder");
        }
    }

	@PutMapping(value = "/{plSeq}/deploy/{plResDeploySeq}/runYn/{runYn}")
	@Operation(summary = "파이프라인 빌드 리소스 실행순서 변경 ", description = "파이프라인 빌드 리소스 실행순서 변경 (실행 순서 변경은 1 단위로만 가능)")
	public PlResDeployVO editPlResDeployRunYn(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plResDeploySeq", description = "파이프라인 배포 리소스 번호", required = true) @PathVariable Integer plResDeploySeq,
			@Parameter(name = "runYn", description = "변경할 runYn", schema = @Schema(allowableValues = {"Y","N"}), required = true) @PathVariable String runYn
	) throws Exception {
		try {
			log.debug("[BEGIN] editPlResDeployRunYn");

			ExecutingContextVO ctx = ContextHolder.exeContext();

			return plService.editPlResDeployRunYn(plSeq, plResDeploySeq, runYn, ctx);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] editPlResDeployRunYn");
		}
	}

	@PostMapping(value = "/{plSeq}/run/build")
	@Operation(summary = "파이프라인 빌드 실행", description = "파이프라인을 빌드를 실행 한다.")
	public PlRunVO runBuildPlMaster(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "runNote", description = "파이프라인 실행 노트", required = true) @RequestParam String runNote,
			@Parameter(name = "callbackUrl", description = "파이프라인 callbackUrl", required = true) @RequestParam String callbackUrl
	) throws Exception {
		try {
			log.debug("[BEGIN] runPlMaster");

			// 실행할 수 있는지 체크
			plService.checkRunValidation(plSeq);

			// run 정보 생성, 여기서의 plRunVO는 pl_run_seq 포함한 pl_run 필수값만 셋팅되어 있는 상태임, build 와 deploy 정보는 없음
			PlRunVO plRunVO = plService.createPlRun(plService.getPlDetail(plSeq), runNote, callbackUrl);

			// 파이프라인 마스터 테이블에 실핼 시퀀스 update
			plService.updatePlMasterForPlRunSeqAndVersion(plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), null);

			// 파이프라인 실행
			plRunVO = plService.runPl(plRunVO);

			// Pl 실행상태 publish subject 설정
			String subject = String.format(PlEventService.PREFIX_PUB_SUBJECT_FMT, plRunVO.getPlRunSeq());
			plRunVO.setPubSubject(subject);


			return plRunVO;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] runPlMaster");
		}
	}

	@PostMapping(value = "/{plSeq}/run")
	@Operation(summary = "파이프라인 실행", description = "파이프라인을 실행 한다.")
	public PlRunVO runPlMaster(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "runNote", description = "파이프라인 실행 노트", required = true) @RequestParam String runNote,
			@Parameter(name = "callbackUrl", description = "파이프라인 callbackUrl", required = true) @RequestParam String callbackUrl
	) throws Exception {
		try {
			log.debug("[BEGIN] runPlMaster");

			PlMasterVO detail = plService.getPlDetail(plSeq);

			/**
			 * 2021.07.08, hjchoi - 현대카드 모드일 경우 워크스페이스 권한이 DEV는 배포 금지 처리
			 * 2022.04.21, hjchoi - 위 권한 처리를 기본 기능으로 처리하도록 변경
			 */
			if (UserRole.valueOf(ContextHolder.exeContext().getUserRole()) == UserRole.DEVOPS) {
				UserGrant userGrant = plService.getUserGrant(detail);
				if (userGrant != null) {
					if (EnumSet.of(UserGrant.DEVOPS, UserGrant.DEV, UserGrant.VIEWER).contains(userGrant)) {
						AuthUtils.isValid(false);
					}
				} else {
					AuthUtils.isValid(false);
				}
			}

			// 실행할 수 있는지 체크
			plService.checkRunValidation(plSeq);

			// run 정보 생성, 여기서의 plRunVO는 pl_run_seq 포함한 pl_run 필수값만 셋팅되어 있는 상태임, build 와 deploy 정보는 없음
			PlRunVO plRunVO = plService.createPlRun(detail, runNote, callbackUrl);

			// 파이프라인 마스터 테이블에 실핼 시퀀스 update
			plService.updatePlMasterForPlRunSeqAndVersion(plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), null);

			// 파이프라인 실행
			plRunVO = plService.runPl(plRunVO);

			// Pl 실행상태 publish subject 설정
			String subject = String.format(PlEventService.PREFIX_PUB_SUBJECT_FMT, plRunVO.getPlRunSeq());
			plRunVO.setPubSubject(subject);


			return plRunVO;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] runPlMaster");
		}
	}

	@PostMapping(value = "/{plSeq}/run/external")
	@Operation(summary = "파이프라인 외부 실행", description = "파이프라인을 외부 호출에 의해 실행 한다. 버전이 자동생성 되면서 실행된다.")
	public PlRunVO runPlMasterFromExternal(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "runType", description = "파이프라인 실행 유형", schema = @Schema(allowableValues = {PlRunType.Names.LATEST, PlRunType.Names.BUILD, PlRunType.Names.DEPLOY, PlRunType.Names.ALL}, defaultValue = PlRunType.Names.LATEST), required = true) @RequestParam(defaultValue = PlRunType.Names.LATEST) String runType,
			@Parameter(name = "runNote", description = "파이프라인 실행 노트", required = true) @RequestParam String runNote,
			@Parameter(name = "callbackUrl", description = "파이프라인 callbackUrl") @RequestParam(required = false) String callbackUrl
	) throws Exception {
		try {
			log.debug("[BEGIN] runPlMasterFromExternal");

			if (callbackUrl == null){
				callbackUrl = "http://dashboard:3000/callback/";
			}

			// 실행할 수 있는지 체크
			plService.checkRunValidation(plSeq);

			// 파이프라인 정보 조회
			PlMasterVO plMasterVO = plService.getPlDetail(plSeq);

			/** Done 상태일 경우 version 다시 생성, 그 외(ERROR, CANCELED, CREATED) 상태는 그냥 실행시킴. */
			if (plMasterVO.getStatus() == PlStatus.DONE){
				String version = plService.newPlversion();

				PlMasterVO existVO = plService.existPlVersion(plSeq, version);
				// 자동생성한 version이 존재할 경우, 다시 버전 정보 발급
				if (existVO != null){
					version = plService.newPlversion();
				}

				// 특정 실행 버전의 값으로 master data update
				plService.rollbackFromPlRunToPlMaster(plSeq, plMasterVO.getReleasePlRunSeq(), version);
			}

			// run 정보 생성, 여기서의 plRunVO는 pl_run_seq 포함한 pl_run 필수값만 셋팅되어 있는 상태임, build 와 deploy 정보는 없음
			PlRunVO plRunVO = plService.createPlRun(plService.getPlDetail(plSeq), runNote, callbackUrl, runType);

			// 파이프라인 마스터 테이블에 실핼 시퀀스 update
			plService.updatePlMasterForPlRunSeqAndVersion(plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), null);

			// 파이프라인 실행
			plRunVO = plService.runPl(plRunVO);

			// Pl 실행상태 publish subject 설정
			String subject = String.format(PlEventService.PREFIX_PUB_SUBJECT_FMT, plRunVO.getPlRunSeq());
			plRunVO.setPubSubject(subject);


			return plRunVO;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] runPlMasterFromExternal");
		}
	}

	@PostMapping(value = "/{plSeq}/run/{plRunSeq}/cancel")
	@Operation(summary = "파이프라인 취소", description = "파이프라인 실행을 취소 한다.")
	public PlRunVO cancelPlMaster(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq
	) throws Exception {
		try {
			log.debug("[BEGIN] cancelPlMaster");

			// 취소 실행할 수 있는지 체크
			PlRunBuildVO runBuildVO = plService.checkCancelValidation(plSeq, plRunSeq);

			// plRun 상태 변경 및 빌드 cancel 처리
			PlRunVO plRunVO = plService.cancelPlRun(plRunSeq, runBuildVO);

			return plRunVO;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] cancelPlMaster");
		}
	}

	@PostMapping(value = "/{plSeq}/run/{plRunSeq}/cancel/build")
	@Operation(summary = "파이프라인 취소", description = "파이프라인 실행을 취소 한다.")
	public PlRunVO cancelBuildPlMaster(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq
	) throws Exception {
		try {
			log.debug("[BEGIN] cancelBuildPlMaster");

			// 취소 실행할 수 있는지 체크
			PlRunBuildVO runBuildVO = plService.checkCancelValidation(plSeq, plRunSeq);

			// plRun 상태 변경 및 빌드 cancel 처리
			PlRunVO plRunVO = plService.cancelPlRun(plRunSeq, runBuildVO);

			return plRunVO;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] cancelBuildPlMaster");
		}
	}


	@GetMapping(value = "/{plSeq}/ver")
	@Operation(summary = "파이프라인 버전 목록 조회", description = "파이프라인을 버전 목록을 조회한다.")
	public List<PlRunVO> getPlVerList(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq
	) throws Exception {
		try {
			log.debug("[BEGIN] getPlVerList");

			// 파이프라인 실행 이력 목록 조회
			List<PlRunVO> results = plService.getPlVerList(plSeq);

			return results;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] getPlVerList");
		}
	}

	@GetMapping(value = "/{plSeq}/run")
	@Operation(summary = "파이프라인 실행 이력 목록", description = "파이프라인을 실행 이력 목록을 조회한다.")
	public List<PlRunVO> getPlRuns(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "useExceptRunningStatus", description = "실행중,취소중 상태를 제외하고 검색 사용", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "useExceptRunningStatus", required = false, defaultValue = "true") boolean useExceptRunningStatus
	) throws Exception {
		try {
			log.debug("[BEGIN] getPlRuns");

			List<String> exceptRunningStatus = null;
			if (useExceptRunningStatus) {
				exceptRunningStatus = Arrays.asList(PlStatus.RUNNING.getCode(), PlStatus.CANCEL.getCode());
			}

			// 파이프라인 실행 이력 목록 조회
			PlRunListSearchVO params = new PlRunListSearchVO();
			params.setPlSeq(plSeq);
			params.setExceptRunningStatus(exceptRunningStatus);
			List<PlRunVO> results = plService.getPlRunList(params);

			return results;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] getPlRuns");
		}
	}

	@GetMapping(value = "/{plSeq}/run/list")
	@Operation(summary = "파이프라인 실행 이력 목록", description = "파이프라인을 실행 이력 목록을 조회한다.")
	public PlRunListVO getPlRunList(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "useExceptRunningStatus", description = "실행중,취소중 상태를 제외하고 검색 사용", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "useExceptRunningStatus", required = false, defaultValue = "true") boolean useExceptRunningStatus,
//          @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"})) @RequestParam(name = "order", required = false) String order,
//        	@Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","audit_log_seq"})) @RequestParam(name = "orderColumn", required = false) String orderColumn,
			@Parameter(name = "nextPage", description = "요청페이지", schema = @Schema(defaultValue = "1"), required = true) @RequestParam(name = "nextPage", defaultValue = "1") Integer nextPage,
			@Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", schema = @Schema(defaultValue = "99999"), required = true) @RequestParam(name = "itemPerPage", defaultValue = "99999") Integer itemPerPage,
//			@Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
//			@Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
			@Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
			@Parameter(name = "endDate", description = "검색 종료 일자", example = "20201228000000") @RequestParam(name = "endDate", required = false) String endDate
	) throws Exception {
		PlRunListVO list;
		try {
			log.debug("[BEGIN] getPlRunList");

			List<String> exceptRunningStatus = null;
			if (useExceptRunningStatus) {
				exceptRunningStatus = Arrays.asList(PlStatus.RUNNING.getCode(), PlStatus.CANCEL.getCode());
			}
			PagingUtils.validatePagingParams(nextPage, itemPerPage);

			// 파이프라인 실행 이력 목록 조회
			list = plService.getPlRunList(plSeq, exceptRunningStatus, null, null, nextPage, itemPerPage, null, null, startDate, endDate);

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] getPlRunList");
		}

		return list;
	}

	@GetMapping("/{plSeq}/run/{plRunSeq}")
	@Operation(summary = "파이프라인 실행 정보 조회", description = "파이프라인 실행 상세 정보 조회")
	public PlRunVO getPlRunDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq
	) throws Exception {

		log.debug("[BEGIN] getPlRunDetail");

		PlRunVO result = plService.getPlRunDetail(plRunSeq, null, true);

		// 취소 실행할 수 있는지 체크후 셋팅
		boolean canCancel = false;
		try {
			PlRunBuildVO runBuildVO = plService.checkCancelValidation(plSeq, plRunSeq);
			canCancel = true;
		}
		catch (CocktailException ce) {
			// log.debug("fail checkCancelValidation getPlRunDetail.", ce);
		}
		catch (Exception e){
			// log.debug("fail checkCancelValidation getPlRunDetail.", e);
		}
		result.setCanCancel(canCancel);

		log.debug("[END  ] getPlRunDetail");

		return result;
	}

	@GetMapping(value = "/{plSeq}/run/{plRunSeq}/build/{plRunBuildSeq}")
	@Operation(summary = "파이프라인 빌드 실행 상세 조회", description = "파이프라인 빌드 실행 상세 조회")
	public PlRunBuildVO getPlRunBuildDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "plRunBuildSeq", description = "파이프라인 실행 빌드 번호", required = true) @PathVariable Integer plRunBuildSeq
	) throws Exception {
		log.debug("[BEGIN] getPlRunBuildDetail");

		PlRunBuildVO runBuild = plService.getPlRunBuildDetail(plSeq, plRunSeq, plRunBuildSeq);

		log.debug("[END  ] getPlRunBuildDetail");
		return runBuild;
	}

	@GetMapping("/{plSeq}/run/{plRunSeq}/log")
	@Operation(summary = "파이프라인 실행 로그 조회", description = "파이프라인 실행 로그 조회")
	public String getPlRunLog(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq
	) throws Exception {

		try {
			log.debug("[BEGIN] getPlRunLog");

			String result = plService.getPlRunLog(plRunSeq);

			return result;
		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] getPlRunLog");
		}
	}


	@PostMapping(value = "/{plSeq}/run/{plRunSeq}/build/{plRunBuildSeq}/result")
	@Operation(summary = "빌드 서버로 부터 빌드 실행 결과를 받아 처리.", description = "서버로 부터 빌드 실행 결과를 받아 처리.")
	public void handleBuildResult(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "plRunBuildSeq", description = "파이프라인 실행 빌드 번호", required = true) @PathVariable Integer plRunBuildSeq,
			HttpServletRequest request
	) throws Exception {
		try {
			log.debug("[BEGIN] handleBuildResult");

			JsonObject jsonObject = new Gson().fromJson(request.getReader(), JsonObject.class);
			String buildResult = jsonObject.toString();

			log.debug("the call data from build server : \n {}", jsonObject);

			// 기본 사용자 정보 조회 및 셋팅, builder server 에서 호출되기 때문에 user_seq, user_role, service_seq 등이 존재 하지 않는다.
			plService.setUserInfosToExcuteContext(plRunSeq);

			// pipeline handle 처리
			plService.handleBuildResult(plSeq, plRunSeq, plRunBuildSeq, buildResult, ContextHolder.exeContext());

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] handleBuildResult");
		}

	}

	@PostMapping(value = "/{plSeq}/rollback/{plRunSeq}")
	@Operation(summary = "파이프라인 롤백 실행", description = "파이프라인의 정보를 특정 실행시점으로 롤백하고 실행 한다.")
	public PlRunVO rollbackPlMaster(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "롤백할 파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "runNote", description = "파이프라인 실행 노트", required = true) @RequestParam String runNote,
			@Parameter(name = "callbackUrl", description = "파이프라인 callbackUrl", required = true) @RequestParam String callbackUrl
	) throws Exception {
		try {
			log.debug("[BEGIN] rollbackPlMaster");

			// 특정 실행 버전의 값으로 master data update
			plService.rollbackFromPlRunToPlMaster(plSeq, plRunSeq, null);

			// rollback 일 경우는 build는 필요 없이 deploy만 실행하기 때문에 build 정보의 run_yn을 모두 'N' 처리
			plService.editPlResBuildsForRunYn(plSeq, "N");

			// 실행할 수 있는지 체크
			plService.checkRunValidation(plSeq);

			// pl run 정보 생성, 여기서의 plRunVO는 pl_run_seq 포함한 pl_run 필수값만 셋팅되어 있는 상태임, build 와 deploy 정보는 없음
			PlRunVO plRunVO = plService.createPlRun(plService.getPlDetail(plSeq), runNote, callbackUrl);

			// 파이프라인 마스터 테이블에 실핼 시퀀스 update
			plService.updatePlMasterForPlRunSeqAndVersion(plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), null);

			// 파이프라인 실행
			plRunVO = plService.runPl(plRunVO);

			return plRunVO;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] rollbackPlMaster");
		}
	}

	@Operation(summary = "파이프라인 실행 리소스 상세조회 - 워크로드", description = "파이프라인 실행 리소스 상세조회 - 워크로드")
	@GetMapping("/{plSeq}/run/{plRunSeq}/res/{plRunDeploySeq}/deploy/{deployType}/workload")
	public ServerIntegrateVO getWorkloadRunDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "plRunDeploySeq", description = "파이프라인 실행 리소스 배포 번호", required = true) @PathVariable Integer plRunDeploySeq,
			@Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI, DeployType.Names.YAML}), required = true) @PathVariable String deployType
	) throws Exception {

		log.debug("[BEGIN] getWorkloadRunDeployResourceDetail");

		ServerIntegrateVO result = plRunDeployService.getWorkloadRunDeployResourceDetail(plSeq, plRunSeq, plRunDeploySeq, deployType);

		log.debug("[END  ] getWorkloadRunDeployResourceDetail");

		return result;
	}
	@Operation(summary = "파이프라인 실행 리소스 상세조회 - 컨피그맵", description = "파이프라인 실행 리소스 상세조회 - 컨피그맵")
	@GetMapping("/{plSeq}/run/{plRunSeq}/res/{plRunDeploySeq}/deploy/configmap")
	public ConfigMapGuiVO getConfigMapRunDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "plRunDeploySeq", description = "파이프라인 실행 리소스 배포 번호", required = true) @PathVariable Integer plRunDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getConfigMapRunDeployResourceDetail");

		ConfigMapGuiVO result = plRunDeployService.getConfigMapRunDeployResourceDetail(plSeq, plRunSeq, plRunDeploySeq);

		log.debug("[END  ] getConfigMapRunDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 실행 리소스 상세조회 - 시크릿", description = "파이프라인 실행 리소스 상세조회 - 시크릿")
	@GetMapping("/{plSeq}/run/{plRunSeq}/res/{plRunDeploySeq}/deploy/secret")
	public SecretGuiVO getSecretRunDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "plRunDeploySeq", description = "파이프라인 실행 리소스 배포 번호", required = true) @PathVariable Integer plRunDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getSecretRunDeployResourceDetail");

		SecretGuiVO result = plRunDeployService.getSecretRunDeployResourceDetail(plSeq, plRunSeq, plRunDeploySeq);

		log.debug("[END  ] getSecretRunDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 실행 리소스 상세조회 - 서비스", description = "파이프라인 실행 리소스 상세조회 - 서비스")
	@GetMapping("/{plSeq}/run/{plRunSeq}/res/{plRunDeploySeq}/deploy/service")
	public K8sServiceVO getServiceRunDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "plRunDeploySeq", description = "파이프라인 실행 리소스 배포 번호", required = true) @PathVariable Integer plRunDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getServiceRunDeployResourceDetail");

		K8sServiceVO result = plRunDeployService.getServiceRunDeployResourceDetail(plSeq, plRunSeq, plRunDeploySeq);

		log.debug("[END  ] getServiceRunDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 실행 리소스 상세조회 - 인그레스", description = "파이프라인 실행 리소스 상세조회 - 인그레스")
	@GetMapping("/{plSeq}/run/{plRunSeq}/res/{plRunDeploySeq}/deploy/ingress")
	public K8sIngressVO getIngressRunDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "plRunDeploySeq", description = "파이프라인 실행 리소스 배포 번호", required = true) @PathVariable Integer plRunDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getIngressRunDeployResourceDetail");

		K8sIngressVO result = plRunDeployService.getIngressRunDeployResourceDetail(plSeq, plRunSeq, plRunDeploySeq);

		log.debug("[END  ] getIngressRunDeployResourceDetail");

		return result;
	}

	@Operation(summary = "파이프라인 실행 리소스 상세조회 - PVC", description = "파이프라인 실행 리소스 상세조회 - PVC")
	@GetMapping("/{plSeq}/run/{plRunSeq}/res/{plRunDeploySeq}/deploy/pvc")
	public K8sPersistentVolumeClaimVO getPvcRunDeployResourceDetail(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "plRunSeq", description = "파이프라인 실행 번호", required = true) @PathVariable Integer plRunSeq,
			@Parameter(name = "plRunDeploySeq", description = "파이프라인 실행 리소스 배포 번호", required = true) @PathVariable Integer plRunDeploySeq
	) throws Exception {

		log.debug("[BEGIN] getPvcRunDeployResourceDetail");

		K8sPersistentVolumeClaimVO result = plRunDeployService.getPvcRunDeployResourceDetail(plSeq, plRunSeq, plRunDeploySeq);

		log.debug("[END  ] getPvcRunDeployResourceDetail");

		return result;
	}

	@PostMapping(value = "/{plSeq}/ver")
	@Operation(summary = "파이프라인 버전 생성", description = "파이프라인의 버전을 생성 한다.")
	public PlMasterVO createVersion(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq,
			@Parameter(name = "version", description = "파이프라인 version", required = true) @RequestParam String version
	) throws Exception {
		try {
			log.debug("[BEGIN] createVersion");

			// master 정보 조회
			PlMasterVO plMaster = plService.getPlDetail(plSeq);

			// 파이프라인 정보가 없거나 release 버전이 존재 하지 않으면 오류??
			if (plMaster == null ){
				String errMsg = "It is not exists the pipeline.";
				throw new CocktailException(errMsg, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET, errMsg);
			} else if (plMaster.getReleaseVer() == null){
				String errMsg = "It is not exists a release version of the pipeline.";
				throw new CocktailException(errMsg, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET, errMsg);
			}

			// 버전 정보 체크, 버전정보가 존재할 경우 Exception 처리
			PlMasterVO existVO = plService.existPlVersion(plSeq, version);
			if (existVO != null){
				String errMsg = String.format("It is exists the pipeline version.(%s)", version);
				throw new CocktailException(errMsg, ExceptionType.ResourceAlreadyExists, ExceptionBiz.PIPELINE_SET, errMsg);
			}

			// 특정 실행 버전의 값으로 master data update
			plService.rollbackFromPlRunToPlMaster(plSeq, plMaster.getReleasePlRunSeq(), version);

			// 파이프라인 정보 다시 조회
			plMaster = plService.getPlDetail(plSeq);

			return plMaster;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] createVersion");
		}
	}

	@DeleteMapping(value = "/{plSeq}/ver")
	@Operation(summary = "파이프라인 버전 삭제", description = "파이프라인의 버전을 삭제 한다.")
	public PlMasterVO deleteVersion(
			@Parameter(name = "plSeq", description = "파이프라인 번호", required = true) @PathVariable Integer plSeq
	) throws Exception {
		try {
			log.debug("[BEGIN] deleteVersion");

			// master 정보 조회
			PlMasterVO plMaster = plService.getPlDetail(plSeq);

			// 파이프라인 정보가 없거나 release 버전이 존재 하지 않으면 오류??
			if (plMaster == null ){
				throw new CocktailException("It is not exists the pipeline.", ExceptionType.CommonDeleteFail, ExceptionBiz.PIPELINE_SET);
			} else if (plMaster.getReleaseVer() == null){
				throw new CocktailException("It is not exists a release version of the pipeline.", ExceptionType.CommonDeleteFail, ExceptionBiz.PIPELINE_SET);
			}

			// 특정 실행 버전의 값으로 master data update
			plService.rollbackFromPlRunToPlMaster(plSeq, plMaster.getReleasePlRunSeq(), null);


			// 파이프라인 정보 다시 조회
			plMaster = plService.getPlDetail(plSeq);

			return plMaster;

		} catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		} catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		} catch (Exception e) {
			throw new CocktailException(e.getMessage(), e, ExceptionType.CommonDeleteFail, ExceptionBiz.PIPELINE_SET);
		} finally {
			log.debug("[END  ] deleteVersion");
		}
	}

}
