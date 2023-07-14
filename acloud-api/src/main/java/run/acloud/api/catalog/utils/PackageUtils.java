package run.acloud.api.catalog.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import run.acloud.api.catalog.vo.ChartInfoBaseVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseInfoVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.protobuf.chart.Package;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public final class PackageUtils {


	/**
	 * Package.ChartVersion 모델을 ChartInfoBaseVO 모델로 컨버
	 *
	 * @param chartVersion
	 * @return
	 * @throws Exception
	 */
	public static ChartInfoBaseVO convertChartVersion(Package.ChartVersion chartVersion) throws Exception {
		ChartInfoBaseVO chartInfoBase = new ChartInfoBaseVO();
		Optional.ofNullable(chartVersion.getName()).ifPresent(rl -> chartInfoBase.setName(rl));
		Optional.ofNullable(chartVersion.getVersion()).ifPresent(rl -> chartInfoBase.setVersion(rl));
		Optional.ofNullable(chartVersion.getDescription()).ifPresent(rl -> chartInfoBase.setDescription(rl));
		Optional.ofNullable(chartVersion.getAppVersion()).ifPresent(rl -> chartInfoBase.setAppVersion(rl));
		Optional.ofNullable(chartVersion.getCreated()).ifPresent(rl -> chartInfoBase.setCreated(rl)); // Chart Data
		Optional.ofNullable(chartVersion.getDigest()).ifPresent(rl -> chartInfoBase.setDigest(rl));
		/** chartVersion 정보에 Url 정보도 있으나 사용하지 않으므로 Skip... **/

		return chartInfoBase;
	}

	/**
	 * Package.ChartVersionDetails 모델을 ChartInfoBaseVO 모델로 컨버트
	 * @param chart
	 * @return
	 */
	public static ChartInfoBaseVO convertChart(Package.ChartVersionDetails chart) throws Exception {
		ChartInfoBaseVO chartInfoBase = new ChartInfoBaseVO();
		if(chart != null) {
			Optional.ofNullable(chart.getName()).ifPresent(rl -> chartInfoBase.setName(rl));
			Optional.ofNullable(chart.getVersion()).ifPresent(rl -> chartInfoBase.setVersion(rl));
			Optional.ofNullable(chart.getCreated()).ifPresent(rl -> chartInfoBase.setCreated(rl)); // Chart Data
			Optional.ofNullable(chart.getDigest()).ifPresent(rl -> chartInfoBase.setDigest(rl));
			Optional.ofNullable(chart.getReadme()).ifPresent(rl -> chartInfoBase.setReadme(rl));
			Optional.ofNullable(chart.getValues()).ifPresent(rl -> chartInfoBase.setValues(rl));
			Optional.ofNullable(chart.getDescription()).ifPresent(rl -> chartInfoBase.setDescription(rl));
			Optional.ofNullable(chart.getIcon()).ifPresent(rl -> chartInfoBase.setIcon(rl));
			Optional.ofNullable(chart.getAppVersion()).ifPresent(rl -> chartInfoBase.setAppVersion(rl));
			Optional.ofNullable(chart.getAddonToml()).ifPresent(rl -> chartInfoBase.setAddonToml(rl));
			Optional.ofNullable(chart.getAddonYaml()).ifPresent(rl -> chartInfoBase.setAddonYaml(rl));
			Optional.ofNullable(chart.getConfigToml()).ifPresent(rl -> chartInfoBase.setConfigToml(rl));
			Optional.ofNullable(chart.getDefaultValueYaml()).ifPresent(rl -> chartInfoBase.setDefaultValueYaml(rl));
		}

		return chartInfoBase;
	}

	/**
	 * Package.Release 모델을 HelmReleaseBaseVO 모델로 컨버트
	 * @param release
	 * @return
	 */
	public static HelmReleaseBaseVO convertRelease(Package.Release release) throws Exception {
		HelmReleaseBaseVO helmRelease = new HelmReleaseBaseVO();
		if(release != null) {
			Optional.ofNullable(release.getName()).ifPresent(rl -> helmRelease.setName(rl));
			Optional.ofNullable(release.getNamespace()).ifPresent(rl -> helmRelease.setNamespace(rl));
			Optional.ofNullable(release.getChart()).ifPresent(rl -> helmRelease.setChart(rl.toStringUtf8())); // Chart Data
			Optional.ofNullable(release.getManifest()).ifPresent(rl -> helmRelease.setManifest(rl));
			Optional.ofNullable(release.getVersion()).ifPresent(rl -> helmRelease.setRevision(rl));
			Optional.ofNullable(release.getAppVersion()).ifPresent(rl -> helmRelease.setAppVersion(rl));
			Optional.ofNullable(release.getChartName()).ifPresent(rl -> helmRelease.setChartNameAndVersion(rl));
			Optional.ofNullable(release.getMetadataName()).ifPresent(rl -> helmRelease.setChartName(rl));
			Optional.ofNullable(release.getMetadataVersion()).ifPresent(rl -> helmRelease.setChartVersion(rl));

			HelmReleaseBaseInfoVO helmInfo = new HelmReleaseBaseInfoVO();
			Optional.ofNullable(release.getInfo()).map(Package.Info::getFirstDeployed).ifPresent(info -> helmInfo.setFirstDeployed(info));
			Optional.ofNullable(release.getInfo()).map(Package.Info::getLastDeployed).ifPresent(info -> helmInfo.setLastDeployed(info));
			Optional.ofNullable(release.getInfo()).map(Package.Info::getDeleted).ifPresent(info -> helmInfo.setDeleted(info));
			Optional.ofNullable(release.getInfo()).map(Package.Info::getDescription).ifPresent(info -> helmInfo.setDescription(info));
			Optional.ofNullable(release.getInfo()).map(Package.Info::getNotes).ifPresent(info -> helmInfo.setNotes(info));
			Optional.ofNullable(release.getInfo()).map(Package.Info::getReadme).ifPresent(info -> helmInfo.setReadme(info));
			Optional.ofNullable(release.getInfo()).map(Package.Info::getIcon).ifPresent(info -> helmInfo.setIcon(info));
			Optional.ofNullable(release.getInfo()).map(Package.Info::getStatus).map(Package.Status::getStatus).ifPresent(info -> helmInfo.setStatus(info));
			helmRelease.setInfo(helmInfo);
		}

		return helmRelease;
	}


	/**
	 * Package.ChartVersionsResponse 모델을 List<ChartInfoBaseVO> 모델로 컨버트
	 * @param chart
	 * @return
	 * @throws Exception
	 */
	public static List<ChartInfoBaseVO> convertChartVersions(Package.ChartVersionsResponse chart) throws Exception {
		List<ChartInfoBaseVO> chartInfoBases = new ArrayList<>();

		for(Package.ChartVersion chartVersion : chart.getResultList()) {
			chartInfoBases.add(PackageUtils.convertChartVersion(chartVersion));
		}

		return chartInfoBases;
	}


	/**
	 * 서비스 연결정보를 조회하여 HelmReleaseBaseVO에 설정
	 * @param helmRelease
	 * @param servicemapSummary
	 * @return
	 * @throws Exception
	 */
	public static HelmReleaseBaseVO fillRelation(HelmReleaseBaseVO helmRelease, ServicemapSummaryVO servicemapSummary) throws Exception {
		if(helmRelease == null) {
			helmRelease = new HelmReleaseBaseVO();
		}
		if(servicemapSummary == null) {
			return helmRelease;
		}

		if(servicemapSummary.getClusterSeq() != null) helmRelease.setClusterSeq(servicemapSummary.getClusterSeq());
		if(StringUtils.isNotEmpty(servicemapSummary.getClusterName())) helmRelease.setClusterName(servicemapSummary.getClusterName());
		helmRelease.setServicemapInfo(servicemapSummary);

		return helmRelease;
	}

}
