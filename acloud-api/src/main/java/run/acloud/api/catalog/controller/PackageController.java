package run.acloud.api.catalog.controller;

import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.catalog.service.PackageCommonService;
import run.acloud.api.catalog.service.PackageInfoService;
import run.acloud.api.catalog.service.PackageService;
import run.acloud.api.catalog.service.PackageValidService;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.catalog.vo.HelmReleaseInfoVO;
import run.acloud.api.catalog.vo.HelmResourcesVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;
import java.util.Optional;

/**
 * @author gun@acornsoft.io
 * Created on 2019. 1. 29.
 */
@Tag(name = "Package", description = "Package 관련 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/package")
public class PackageController {

	/**
	 * Reference : Helm Chart Release Status list...
	 * https://github.com/helm/helm/blob/master/pkg/release/status.go
	 *
	 StatusUnknown Status = "unknown"
	 // StatusDeployed indicates that the release has been pushed to Kubernetes.
	 StatusDeployed Status = "deployed"
	 // StatusUninstalled indicates that a release has been uninstalled from Kubermetes.
	 StatusUninstalled Status = "uninstalled"
	 // StatusSuperseded indicates that this release object is outdated and a newer one exists.
	 StatusSuperseded Status = "superseded"
	 // StatusFailed indicates that the release was not successfully deployed.
	 StatusFailed Status = "failed"
	 // StatusUninstalling indicates that a uninstall operation is underway.
	 StatusUninstalling Status = "uninstalling"
	 // StatusPendingInstall indicates that an install operation is underway.
	 StatusPendingInstall Status = "pending-install"
	 // StatusPendingUpgrade indicates that an upgrade operation is underway.
	 StatusPendingUpgrade Status = "pending-upgrade"
	 // StatusPendingRollback indicates that an rollback operation is underway.
	 StatusPendingRollback Status = "pending-rollback"
	 */

	@Autowired
    private PackageService packageService;

	@Autowired
    private PackageCommonService packageCommonService;

	@Autowired
    private PackageInfoService packageInfoService;

	@Autowired
    private PackageValidService packageValidService;

	@Operation(summary = "Print Cluster Access Info")
	@GetMapping("/{apiVersion}/cluster/{clusterSeq}/info")
	public void printClusterAccessInfo(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq
	) throws Exception {
		log.debug("[BEGIN] printClusterAccessInfo");
		packageService.printClusterAccessInfo(clusterSeq);
		log.debug("[END  ] printClusterAccessInfo");
	}

	@Operation(summary = "Cluster 전체 Package 목록 조회")
	@GetMapping("/{apiVersion}/cluster/{clusterSeq}")
	public List<HelmReleaseBaseVO> getPackages(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "filter", description = "filter") @RequestParam(value = "filter", required = false) String filter
	) throws Exception {
		log.debug("[BEGIN] getPackages");

		List<HelmReleaseBaseVO> result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageInfoService.getPackages(clusterSeq, null, filter);
			log.debug("result count : " + Optional.ofNullable(result).orElseGet(() ->Lists.newArrayList()).size());
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageListInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageListInquireFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] getPackages");

		return result;
	}

	@Operation(summary = "Namespace 안의 Package 목록 조회")
	@GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
	public List<HelmReleaseBaseVO> getPackagesInNamespace(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
		@Parameter(name = "filter", description = "filter") @RequestParam(value = "filter", required = false) String filter
	) throws Exception {
		log.debug("[BEGIN] getPackagesInNamespace");

		List<HelmReleaseBaseVO> result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageInfoService.getPackages(clusterSeq, namespaceName, filter);
			log.debug("result count : " + Optional.ofNullable(result).orElseGet(() ->Lists.newArrayList()).size());
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageListInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageListInquireFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] getPackagesInNamespace");

		return result;
	}

	@Operation(summary = "Package 상태 조회")
	@GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/{releaseName:.+}")
	public HelmReleaseBaseVO getPackageStatus(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
		@Parameter(name = "releaseName", description = "releaseName", required = true)  @PathVariable String releaseName,
		@Parameter(name = "revision", description = "revision") @RequestParam(value = "revision", required = false) String revision
	) throws Exception {
		log.debug("[BEGIN] getPackageStatus");

		HelmReleaseBaseVO result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageService.getPackageStatus(clusterSeq, namespaceName, releaseName, revision);
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageStatusInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageStatusInquireFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] getPackageStatus");

		return result;
	}

	@Operation(summary = "Package Revision 정보 조회 ")
	@GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/{releaseName}/revisions")
	public List<HelmReleaseInfoVO> getPackageRevisions(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
		@Parameter(name = "releaseName", description = "releaseName", required = true)  @PathVariable String releaseName
	) throws Exception {
		log.debug("[BEGIN] getPackageRevisions");

		List<HelmReleaseInfoVO> result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageService.getPackageRevisions(clusterSeq, namespaceName, releaseName);
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageHistoryInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageHistoryInquireFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] getPackageRevisions");

		return result;
	}

	@Operation(summary = "Package Install")
	@PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}")
	public HelmReleaseBaseVO installPackage(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
        @Parameter(name = "helmInstallRequest", description = "Install Chart 정보", required = true) @RequestBody HelmInstallRequestVO helmInstallRequest
	) throws Exception {
		log.debug("[BEGIN] installPackage");

		HelmReleaseBaseVO result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageCommonService.installPackage(clusterSeq, namespaceName, helmInstallRequest, ContextHolder.exeContext());
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageInstallFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageInstallFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] installPackage");

		return result;
	}

	@Operation(summary = "Package Upgrage")
	@PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/{releaseName:.+}")
	public HelmReleaseBaseVO upgradePackage(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
		@Parameter(name = "releaseName", description = "releaseName", required = true)  @PathVariable String releaseName,
		@Parameter(name = "helmUpgradelRequest", description = "Upgrage Chart 정보", required = true) @RequestBody HelmInstallRequestVO helmUpgradelRequest
	) throws Exception {
		log.debug("[BEGIN] upgradePackage");

		HelmReleaseBaseVO result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageCommonService.upgradePackage(clusterSeq, namespaceName, releaseName, helmUpgradelRequest);
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageUpgradeFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageUpgradeFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] upgradePackage");

		return result;
	}

	@Operation(summary = "Package Rollback")
	@PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/{releaseName}/{revision:.+}")
	public HelmReleaseBaseVO rollbackPackage(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
		@Parameter(name = "releaseName", description = "releaseName", required = true)  @PathVariable String releaseName,
		@Parameter(name = "revision", description = "revision", required = true) @PathVariable String revision
	) throws Exception {
		log.debug("[BEGIN] rollbackPackage");

		HelmReleaseBaseVO result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageService.rollbackPackage(clusterSeq, namespaceName, releaseName, revision);
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageRollbackFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageRollbackFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] rollbackPackage");

		return result;
	}

	@Operation(summary = "Package Uninstall")
	@DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/{releaseName:.+}")
	public HelmReleaseBaseVO unInstallPackage(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
		@Parameter(name = "releaseName", description = "releaseName", required = true)  @PathVariable String releaseName
	) throws Exception {
		log.debug("[BEGIN] unInstallPackage");

		HelmReleaseBaseVO result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageCommonService.unInstallPackage(clusterSeq, namespaceName, releaseName);
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageUninstallFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageUninstallFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] unInstallPackage");

		return result;
	}

	@Operation(summary = "Package 상세 정보 조회 (Resource 상태 정보 포함)")
	@GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/{releaseName}/detail")
	public HelmResourcesVO getPackageResources(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
		@Parameter(name = "releaseName", description = "releaseName", required = true)  @PathVariable String releaseName
	) throws Exception {
		log.debug("[BEGIN] getPackageResources");

		HelmResourcesVO result;
		try {
			ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
			ContextHolder.exeContext().setApiVersionType(apiVersionType);

			result = packageService.getPackageResources(clusterSeq, namespaceName, releaseName);
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException(ExceptionType.PackageStateInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.PackageStateInquireFail, ExceptionBiz.PACKAGE_SERVER);
		}

		log.debug("[END  ] getPackageResources");

		return result;
	}

	@Operation(summary = "Release 이름 사용 여부 확인 Namespace에서 Unique 여부", description = "추가하려는 패키지의 이름이 해당 Namespace에서 이미 사용하고 있는 것인지 검사.")
	@GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/{releaseName}/check")
	public ResultVO isUsingReleaseName(
		@Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
		@Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "namespaceName", description = "namespaceName", required = true)  @PathVariable String namespaceName,
		@Parameter(name = "releaseName", description = "releaseName", required = true)  @PathVariable String releaseName,
		@Parameter(name = "isCheckServerName", description = "워크로드명과 중복 체크 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "isCheckServerName", required = false, defaultValue = "false") boolean isCheckServerName
	) throws Exception {
		ResultVO r = new ResultVO();
		try {
			r.putKeyValue("exists", packageValidService.isUsingReleaseName(clusterSeq, namespaceName, releaseName, isCheckServerName));
		}
		catch (DataAccessException de) {
			throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
		}
		catch (CocktailException ce) {
			throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
		}
		catch (Exception e) {
			throw new CocktailException("Error during data query.", e, ExceptionType.CommonInquireFail, ExceptionBiz.SERVER);
		}
		return r;
	}
}
