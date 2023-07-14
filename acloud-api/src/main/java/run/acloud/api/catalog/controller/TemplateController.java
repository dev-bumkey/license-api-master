package run.acloud.api.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import run.acloud.api.catalog.enums.LaunchType;
import run.acloud.api.catalog.enums.TemplateDeploymentType;
import run.acloud.api.catalog.enums.TemplateShareType;
import run.acloud.api.catalog.enums.TemplateType;
import run.acloud.api.catalog.service.TemplateService;
import run.acloud.api.catalog.vo.*;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.cserver.vo.ServicemapVO;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.DataType;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CompressUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Tag(name = "Template", description = "템플릿 관련 기능을 제공한다.")
@Slf4j
@Controller
@RequestMapping(value = "/api/template")
public class TemplateController {

	@Autowired
	private CocktailServiceProperties cocktailServiceProperties;

	@Autowired
	private TemplateService templateService;

	@Autowired
	private ClusterStateService clusterStateService;

	@PostMapping(value = "/{apiVersion}")
	@Operation(summary = "템플릿 생성 (apiVersion v1 = 기존 JSON 형태, apiVersion v2 = YAML 형태로 생성)", description = "배치작업을 템플릿으로 저장한다.")
	@ResponseBody
	public TemplateAddVO addTemplate (
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1","v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(description = "템플릿 생성 모델", required = true) @RequestBody @Validated TemplateAddVO templateAdd
	) throws Exception {
		log.debug("[BEGIN] addTemplate");

		// 신규 등록시 템플릿 이름은 필수
		if(templateAdd.isNew() && StringUtils.isBlank(templateAdd.getTemplateName())){
			throw new CocktailException(this.genFieldRequiredMsg("templateName"), ExceptionType.TemplateNameInvalid);
		}
		// 기존 템플릿에 버전 추가 시 템플릿 번호는 필수
		if(!templateAdd.isNew() && (templateAdd.getTemplateSeq() == null || templateAdd.getTemplateSeq() < 1)){
			throw new CocktailException(this.genFieldRequiredMsg("templateSeq"), ExceptionType.TemplateSequenceEmpty);
		}
		templateAdd.setCreator(ContextHolder.exeContext().getUserSeq());

		// 템플릿 등록
		templateService.addTemplate(apiVersion, templateAdd);

		log.debug("[END  ] addTemplate");
		return templateAdd;
	}

	@GetMapping(value = "")
	@Operation(summary = "템플릿 목록", description = "템플릿 목록을 반환한다.")
	@ResponseBody
	public List<TemplateListVO> getTemplates(
			@RequestHeader(name = "user-id" ) Integer userSeq,
			@RequestHeader(name = "user-role" ) String userRole,
			@Parameter(description = "템플릿 유형", schema = @Schema(allowableValues = {"PACKAGE","BUILD_PACK","SERVICE"})) @RequestParam(defaultValue = "SERVICE") String templateType,
			@Parameter(description = "템플릿 공유 유형", schema = @Schema(allowableValues = {"SYSTEM_SHARE","WORKSPACE_SHARE"})) @RequestParam(defaultValue = "", required = false) String templateShareType,
			@Parameter(description = "계정고유번호(accountSeq)") @RequestParam(required = false) Integer accountSeq,
			@Parameter(description = "워크스페이스고유번호(serviceSeq)") @RequestParam(required = false) Integer serviceSeq
	) throws Exception {
		log.debug("[BEGIN] getTemplates");

		if("COCKTAIL".equals(templateType)) {
			serviceSeq = null;
		}
		List<TemplateListVO> result = templateService.getTemplateList(templateType, templateShareType, accountSeq, serviceSeq);

		log.debug("[END  ] getTemplates");
		return result;
	}

	@GetMapping(value = "/{apiVersion}/{templateSeq}")
	@Operation(summary = "템플릿 상세정보", description = "템플릿의 상세정보를 가져온다.")
	@ResponseBody
	public TemplateDetailVO getTemplateDetail(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1","v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(description = "템플릿 번호", required = true) @PathVariable Integer templateSeq,
		@Parameter(description = "템플릿버전 번호") @RequestParam(required = false) Integer templateVersionSeq,
		@Parameter(description = "템플릿배치 정보 Display 여부", schema = @Schema(allowableValues = {"false","true"})) @RequestParam(required = false, defaultValue = "true") Boolean showDeploy
	) throws Exception {
		log.debug("[BEGIN] getTemplateDetail");

		TemplateDetailVO template = templateService.getTemplateDetail(apiVersion, templateSeq, templateVersionSeq, showDeploy);

		log.debug("[END  ] getTemplateDetail");
		return template;
	}

	@DeleteMapping(value = "/{templateSeq}")
	@Operation(summary = "템플릿 삭제", description = "템플릿을 삭제한다.")
	@ResponseBody
	public ResultVO removeTemplate(
            @RequestHeader(name = "user-id" ) Integer userSeq,
			@Parameter(description = "템플릿 번호", required = true) @PathVariable Integer templateSeq,
    		@Parameter(description = "템플릿버전 번호", required = true) @RequestParam Integer templateVersionSeq
			) throws Exception {
		log.debug("[BEGIN] removeTemplates");

		templateService.removeTemplate(templateSeq, templateVersionSeq, userSeq);

		log.debug("[END  ] removeTemplates");
		return new ResultVO();
	}

	@PutMapping(value = "/{templateSeq}/{templateVersionSeq}")
	@Operation(summary = "템플릿 편집", description = "템플릿을 편집한다.")
	@ResponseBody
	public ResultVO editTemplate(
            @RequestHeader(name = "user-id" ) Integer userSeq,
			@Parameter(description = "템플릿 번호", required = true) @PathVariable Integer templateSeq,
    		@Parameter(description = "템플릿버전 번호", required = true) @PathVariable Integer templateVersionSeq,
			@Parameter(description = "템플릿 편집 모델", required = true) @RequestBody @Validated TemplateEditVO templateEdit
			) throws Exception {
		log.debug("[BEGIN] editTemplate");

		templateEdit.setCreator(userSeq);

		templateService.editTemplate(templateEdit);

		log.debug("[END  ] editTemplate");
		return new ResultVO();
	}

	@PostMapping(value = "/{apiVersion}/{templateSeq}/deploy")
	@Operation(summary = "템플릿 배포", description = "템플릿 배포")
	@ResponseBody
	public ServicemapVO deployTemplate(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(description = "템플릿 번호", required = true) @PathVariable Integer templateSeq,
		@Parameter(description = "템플릿 실행 모델", required = true) @RequestBody @Validated TemplateLaunchVO templateLaunch
	) throws Exception {
		log.debug("[BEGIN] deployTemplate");

		/**
		 * cluster 상태 체크
		 */
		clusterStateService.checkClusterState(templateLaunch.getClusterSeq());

		// servicemap_group_seq는 필수 입력.
		// 2022.08.03 hjchoi 서비스맵 등록시 값이 없으면 조회하여 첫번째 servicemapGroup의 seq가 셋팅되도록 수정
//		if(templateLaunch.getServicemapGroupSeq() == null || templateLaunch.getServicemapGroupSeq() < 1) {
//			throw new CocktailException(this.genFieldRequiredMsg("ServicemapGroupSeq"), ExceptionType.AppmapNameInvalid);
//		}

		// 신규 appmap 생성할 시 서비스맵 이름 필수
		if(StringUtils.equals(LaunchType.NEW.getType(), templateLaunch.getLaunchType())){
			if(StringUtils.isNotBlank(templateLaunch.getNamespaceName())) {
				if(!ResourceUtil.validNamespaceName(templateLaunch.getNamespaceName())){
					throw new CocktailException("Invalid namespaceName!!", ExceptionType.NamespaceNameInvalid);
				}
			}
			if(StringUtils.isBlank(templateLaunch.getServicemapName())){
				throw new CocktailException(this.genFieldRequiredMsg("servicemapName"), ExceptionType.AppmapNameInvalid);
			}
		}
		// 기존 servicemap에 추가할 시 서비스맵 번호 필수
		if(StringUtils.equals(LaunchType.ADD.getType(), templateLaunch.getLaunchType())){
			if (templateLaunch.getServicemapSeq() == null || templateLaunch.getServicemapSeq() < 1) {
				throw new CocktailException(this.genFieldRequiredMsg("servicemapSeq"), ExceptionType.InvalidParameter);
			}
			if(templateLaunch.getServicemapGroupSeq() == null || templateLaunch.getServicemapGroupSeq() < 1) {
				throw new CocktailException(this.genFieldRequiredMsg("ServicemapGroupSeq"), ExceptionType.AppmapNameInvalid);
			}
		}

		ExecutingContextVO ctx = ContextHolder.exeContext();
		ctx.setApiVersionType(ApiVersionType.valueOf(apiVersion.toUpperCase()));
		templateLaunch.setCreator(ctx.getUserSeq());

		ServicemapVO servicemap = null;
		try {
			switch (ApiVersionType.valueOf(apiVersion.toUpperCase())) {
				case V2:
					servicemap = templateService.deployTemplateV2(templateLaunch, ctx);
					break;
				default:
					throw new CocktailException("Invalid apiVersion", ExceptionType.InvalidParameter);
			}
		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception ex) {
			throw new CocktailException(String.format("An error occurred during template deployment. [%s]", ex.getMessage()), ex, ExceptionType.TemplateDeploymentFail);
		}

		log.debug("[END  ] deployTemplate");
		return servicemap;
	}

	@PostMapping(value = "/{apiVersion}/{templateSeq}/valid")
	@Operation(summary = "템플릿 유효성 체크", description = "템플릿의 유효성을 체크한다.")
	@ResponseBody
	public List<TemplateValidResponseVO> validTemplate(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1","v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(description = "템플릿 번호", required = true) @PathVariable Integer templateSeq,
		@Parameter(description = "템플릿 유효성 요청 모델", required = true) @RequestBody @Validated TemplateValidRequestVO templateValidRequest
	) throws Exception {

		log.debug("[BEGIN] validTemplate");

		ExecutingContextVO ctx = new ExecutingContextVO();
		ctx.setApiVersionType(ApiVersionType.valueOf(apiVersion.toUpperCase()));

		List<TemplateValidResponseVO> validList = templateService.validTemplate(apiVersion, templateValidRequest, ctx);

		log.debug("[END  ] validTemplate");

		return validList;
	}

	@GetMapping(value = "/{apiVersion}/{templateSeq}/{templateVersionSeq}/export")
	@Operation(summary = "템플릿 Export", description = "템플릿을 Export 한다.")
	public void templateFileExport(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1","v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(description = "템플릿 번호", required = true) @PathVariable Integer templateSeq,
		@Parameter(description = "템플릿버전 번호", required = true) @PathVariable Integer templateVersionSeq,
		@Parameter(name = "dataType", description = "Export Data Type", schema = @Schema(allowableValues = {"SNAPSHOT","YAML"}, defaultValue = "SNAPSHOT")) @RequestParam(value = "dataType", required = false, defaultValue = "SNAPSHOT") String dataType,
		@Parameter(name = "splitCompression", description = "Resource 분할 압축 사용여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "splitCompression", required = false, defaultValue = "false") boolean splitCompression,
		HttpServletRequest request,
		HttpServletResponse response
	) throws Exception {

		log.debug("[BEGIN] templateFileExport");

		try {
			byte[] templateDataByte = null;
			byte[] zip = null;
			String zipFileName = null;
			if(ApiVersionType.valueOf(apiVersion.toUpperCase()) == ApiVersionType.V2) {
				// 압축할 파일명 ReleaseVersion을 포함하여 생성 - releaseVersion에 따라 import 제약을 주기 위해 추가
				String entryname = String.format("snapshot-%s.yaml", cocktailServiceProperties.getReleaseVersion());
				// export할 template 조회
				TemplateDetailVO template = templateService.getTemplateDetail(apiVersion, templateSeq, templateVersionSeq, true);

				if (template != null) {
					// Zip 파일명 생성
					String templateName = StringUtils.replaceAll(template.getTemplateName(), "[:\\\\/%*?:|\"<>]", "");
					zipFileName = URLEncoder.encode(String.format("%s-%s.zip", templateName, template.getVersion()), "UTF-8");

					if(DataType.valueOf(dataType) == DataType.SNAPSHOT) {
						for (TemplateDeploymentVO deployment : template.getTemplateDeployments()) {
							if(deployment.getTemplateDeploymentType() != TemplateDeploymentType.PACKAGE) {
								deployment.setTemplateContent(deployment.getTemplateContentYaml());
							}
							deployment.setTemplateContentYaml(null);
							deployment.setTemplateContentJson(null);
						}
						// template data Base64로 인코딩하여 생성
						templateDataByte = Base64Utils.encode(JsonUtils.toGson(template).getBytes(StandardCharsets.UTF_8));
						zip = CompressUtils.zipFileToByte(entryname, templateDataByte);
					}
					else if(DataType.valueOf(dataType) == DataType.YAML) {
						zipFileName = URLEncoder.encode(String.format("%s-%s-Yaml.zip", templateName, template.getVersion()), "UTF-8");
						StringBuffer templateData = null;
						if(splitCompression) {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ZipOutputStream zos = new ZipOutputStream(baos);
							try {
								int i = 1;
								for (TemplateDeploymentVO deployment : template.getTemplateDeployments()) {
									if (StringUtils.isNotBlank(deployment.getTemplateContentYaml())) {
										if(deployment.getTemplateDeploymentType() == TemplateDeploymentType.PACKAGE) { // Package는 YAML로 저장되지 않으므로 별도 처리..
											HelmInstallRequestVO helmInstallRequest = JsonUtils.fromGson(deployment.getTemplateContent(), HelmInstallRequestVO.class);
											String entryName = String.format("%s-%s.yaml", deployment.getTemplateDeploymentType(), helmInstallRequest.getReleaseName());
											zos.putNextEntry(new ZipEntry(entryName));
											zos.write(deployment.getTemplateContentYaml().getBytes());
										}
										else {
											Map<String, Object> yamlMap = Yaml.getSnakeYaml().load(deployment.getTemplateContentYaml());
											Map<String, Object> meta = (Map<String, Object>) MapUtils.getMap(yamlMap, KubeConstants.META, null);
											String entryName = String.format("%s-%s.yaml", deployment.getTemplateDeploymentType(), MapUtils.getString(meta, KubeConstants.NAME, "" + i++));
											zos.putNextEntry(new ZipEntry(entryName));
											zos.write(deployment.getTemplateContentYaml().getBytes());
										}
									}
								}
								zos.closeEntry();
							} catch (IOException ie) {
								log.error(ie.getMessage(), ie);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							} finally {
								zos.flush();
								zos.finish();
								baos.flush();
								zos.close();
								baos.close();
							}
							zip = baos.toByteArray();
						}
						else {
							for (TemplateDeploymentVO deployment : template.getTemplateDeployments()) {
								if (StringUtils.isNotBlank(deployment.getTemplateContentYaml())) {
									if (templateData == null) {
										templateData = new StringBuffer();
									}
									else {
										templateData.append("---\n");
									}
									templateData.append(deployment.getTemplateContentYaml());
									templateData.append("\n");
								}
							}
							if (templateData != null && StringUtils.isNotBlank(templateData.toString())) {
								templateDataByte = templateData.toString().getBytes(StandardCharsets.UTF_8); // YAML 포멧 그대로 Export.
								zip = CompressUtils.zipFileToByte(entryname, templateDataByte);
							}
							else {
								response.setStatus(HttpServletResponse.SC_NOT_FOUND);
							}
						}
					}
				}
				else {
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				}
			}

			if(zip != null) {
				// Zip 파일로 response 셋팅
				try (ServletOutputStream sos = response.getOutputStream()) {
					response.setStatus(HttpServletResponse.SC_OK);
					response.setContentType("application/zip; UTF-8");
					response.setHeader("Accept-Ranges", "bytes");
					response.setHeader("Content-Disposition", String.format("attachment; filename=%s; filename*=UTF-8''%s", zipFileName, zipFileName));
					response.setHeader("Content-Transfer-Encoding", "binary");
					response.setContentLength(zip.length);

					sos.write(zip);
					sos.flush();
					response.flushBuffer();
				} catch (IOException ie) {
					throw ie;
				} catch (Exception e) {
					throw e;
				}
			}
			else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		}
		catch (IllegalArgumentException iae) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw new CocktailException("Template Export fail.", iae, ExceptionType.TemplateExportFail);
		}
		catch (IOException ie) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw new CocktailException("Template Export fail.", ie, ExceptionType.TemplateExportFail);
		}
		catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw new CocktailException("Template Export fail.", e, ExceptionType.TemplateExportFail);
		}
		log.debug("[END  ] templateFileExport");
	}

	@PostMapping(value = "/{apiVersion}/import", consumes = { "multipart/form-data" })
	@Operation(summary = "템플릿 import", description = "템플릿을 import 한다.")
	@ResponseBody
	public void templateFileImport(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1","v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(description = "신규생성여부", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam Boolean isNew,
		@Parameter(description = "템플릿 번호") @RequestParam(required = false) Integer templateSeq,
		@Parameter(description = "템플릿명") @RequestParam(required = false) String templateName,
		@Parameter(description = "템플릿버전") @RequestParam(required = false) String templateVersion,
		@Parameter(description = "템플릿공유유형") @RequestParam(required = false) String templateShareType,
		@Parameter(description = "워크스페이스 번호") @RequestParam(required = false) Integer serviceSeq,
		@Parameter(description = "Exported Template file", required = true) @RequestPart("dataFile") MultipartFile multipartFile,
		HttpServletResponse response
	) throws Exception {

		log.debug("[BEGIN] templateFileImport");

		if(multipartFile.isEmpty()) {
			throw new CocktailException("File empty.", ExceptionType.TemplateImportFileInvalid);
		}

		// 확장자 체크
		if(!FilenameUtils.isExtension(multipartFile.getOriginalFilename(), "zip")){
			throw new CocktailException("Invalid File extension.", ExceptionType.TemplateImportFileInvalid_Extension);
		}
		// mime type 체크
		Tika defaultTika = new Tika();
		if(!StringUtils.equalsIgnoreCase("application/zip", defaultTika.detect(multipartFile.getBytes()))){
			throw new CocktailException("Invalid File mime type.", ExceptionType.TemplateImportFileInvalid_MimeType);
		}

		/** Start 파라미터 체크 =========================== **/
		// 신규 등록시 템플릿 이름은 필수
		if(isNew && StringUtils.isBlank(templateName)){
			throw new CocktailException(this.genFieldRequiredMsg("templateName"), ExceptionType.TemplateNameInvalid);
		}
		// 신규 등록시 템플릿 공유유형은 필수 :2019.08.08
		if(isNew && StringUtils.isBlank(templateShareType)){
			throw new CocktailException(this.genFieldRequiredMsg("templateShareType is missing"), ExceptionType.InvalidParameter);
		}
		if(isNew && !TemplateShareType.getTemplateShareTypeListToString().contains(templateShareType)){
			throw new CocktailException(this.genFieldRequiredMsg("templateShareType is mismatch (It is only possible for WORKSPACE_SHARE and SYSTEM_SHARE)"), ExceptionType.InvalidParameter);
		}
		// 기존 템플릿에 버전 추가 시 템플릿 번호는 필수
		if(!isNew){
			if(templateSeq == null || templateSeq < 1){
				throw new CocktailException(this.genFieldRequiredMsg("templateSeq"), ExceptionType.TemplateSequenceEmpty);
			}
			if(StringUtils.isBlank(templateVersion)){
				templateVersion = "1.0";
			}
		}
		/** End 파라미터 체크 =========================== **/

		File f = null;
		try {
			f = CompressUtils.unzipFile(multipartFile);
			if (f != null) {
				String templateStr = new String(Base64Utils.decode(Files.readAllBytes(Paths.get(f.getPath()))), StandardCharsets.UTF_8.name());
				TemplateDetailVO templateDetail = JsonUtils.fromGson(templateStr, TemplateDetailVO.class);

				log.debug("templateFileImport.dataFile : {}", JsonUtils.toGson(templateDetail));

				// template 등록 모델 셋팅
				TemplateAddVO templateAdd = new TemplateAddVO();
				templateAdd.setNew(isNew);
				templateAdd.setTemplateType(templateDetail.getTemplateType());
				if (templateDetail.getTemplateType() == TemplateType.SERVICE) {
					if (templateDetail.getTemplateShareType() != null) {
						templateAdd.setTemplateShareType(templateDetail.getTemplateShareType());
					}else {
						templateAdd.setTemplateShareType(TemplateShareType.WORKSPACE_SHARE);
					}
				}
				templateAdd.setSummary(templateDetail.getSummary());
				templateAdd.setDescription(templateDetail.getDescription());
				templateAdd.setAccountSeq(ContextHolder.exeContext().getUserAccountSeq());

				/**
				 * 신규 등록시 템플릿 공유유형은 사용자가 입력한 값으로 최종 셋팅함 : 2019.08.08
				 */
				if(isNew && TemplateShareType.getTemplateShareTypeListToString().contains(templateShareType)){
					templateAdd.setTemplateShareType(TemplateShareType.valueOf(templateShareType));
				}

				if(isNew){
					templateAdd.setTemplateName(templateName);
					templateAdd.setVersion(templateDetail.getVersion());
					if (templateAdd.getTemplateShareType() == TemplateShareType.WORKSPACE_SHARE) {
						if (serviceSeq != null) {
							templateAdd.setServiceSeq(serviceSeq);
						} else {
							throw new CocktailException("serviceSeq is null.", ExceptionType.InvalidParameter);
						}
					}
				}else{
					templateAdd.setTemplateSeq(templateSeq);
					templateAdd.setVersion(templateVersion);
				}

				templateService.addTemplateByImport(apiVersion, templateAdd, templateDetail);
			} else {
				throw new CocktailException("File empty.(null)", ExceptionType.TemplateImportFileInvalid);
			}

			// json으로 response 셋팅
			try (ServletOutputStream sos = response.getOutputStream()) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("application/json");

				ResultVO result = new ResultVO();

				sos.write(JsonUtils.toGson(result).getBytes());
				sos.flush();
				response.flushBuffer();
			} catch (IOException ie) {
				throw ie;
			} catch (Exception e) {
				throw e;
			}
		}
		catch (IOException ie) {
			throw new CocktailException("Template Import fail.", ie, ExceptionType.TemplateImportFail);
		}
		catch (CocktailException ce) {
			throw new CocktailException("Template Import fail.", ce, ExceptionType.TemplateImportFail);
		}
		catch (Exception e) {
			throw new CocktailException("Template Import fail.", e, ExceptionType.TemplateImportFail);
		}
		log.debug("[END  ] templateFileImport");
	}

	private String genFieldRequiredMsg(String fieldName){
		String invalidParamMsg = "\'%s\' field required.";

		return String.format(invalidParamMsg, fieldName);
	}



}