package run.acloud.framework.enums;

import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum AuthExcludePathPattern {

	EP_001_000("/api/cluster/id/*/*", "Y", ""),
	EP_002_000("/api/cluster/pipeline/*", "Y", ""),
	EP_003_000("/api/cluster/accessable" , "Y", "called API from Cluster-API"),
	EP_004_000("/api/cluster/signature/**", "Y", ""),
	EP_005_000("/api/label/*/dynamiclabel", "Y", ""),
	EP_006_000("/api/cube/component/*/pod", "N", ""),
	EP_007_000("/api/appmap/v2/*/pods", "N", ""),
	EP_008_000("/api/appmap/v2/*/server-pods", "N", ""),
	EP_009_000("/api/appmap/v2/*/server-names", "N", ""),
	EP_010_000("/api/appmap/summaries", "N", ""),
	EP_011_000("/api/monitoring/**", "Y", ""),
	EP_012_001("/api/auth/login", "Y", ""),
	EP_012_002("/api/auth/ad/login", "Y", ""),
	EP_012_003("/api/auth/platform/admin/login", "Y", "For CSAP"),
	EP_012_004("/api/auth/platform/user/login", "Y", "For CSAP"),
	EP_012_005("/api/auth/admin/login", "Y", "For CSAP"),
	EP_012_006("/api/auth/publickey", "Y", "For GS"),
	EP_013_000("/api/check/**", "Y", ""),
	EP_014_000("/api/file/**", "Y", ""),
	EP_015_000("/api/internal/cube/**", "Y", ""),
	EP_016_000("/api/batch/**", "Y", "called API from batch"),
	// EP_017_000("/builder/task/*/RESULT", "Y", "build result receive API"),
	EP_018_000("/api/build/run/result/**", "Y", "pipeline result receive API"),
	EP_019_000("/api/pipelineflow/run/result/**", "Y", "pipeline result receive API"),
	EP_020_000("/api/pl/*/run/*/build/*/result", "Y", "pl build result receive API"),
	EP_021_000("/api/alert/*/ruleid/*/user", "Y", "AlertRuleID를 수신받을 사용자 목록 조회 : System(AlarmCollector) to System(api-server)"),
	EP_022_000("/api/keycloak/**", "Y", "Keycloak 연동 API"),
	EP_023_000("/internal/**", "Y", "called API from internal service"),
	EP_024_000("/v2/api-docs/**", "Y", ""),
	EP_025_000("/api/redoc/**", "Y", ""),


	DUMMY("", "N", "")
	;

	@Getter
	private String excludePath;

	@Getter
	private String useYn;

	@Getter
	private String desc;

	AuthExcludePathPattern(String excludePath, String useYn, String desc) {
		this.excludePath = excludePath;
		this.useYn = useYn;
		this.desc = desc;
	}

	public static String[] toArray() {
		return Arrays.stream(AuthExcludePathPattern.values()).filter(a -> (BooleanUtils.toBoolean(a.getUseYn()))).map(AuthExcludePathPattern::getExcludePath).toArray(String[]::new);
	}

	public static List<String> toList() {
		return Arrays.stream(AuthExcludePathPattern.values()).filter(a -> (BooleanUtils.toBoolean(a.getUseYn()))).map(AuthExcludePathPattern::getExcludePath).collect(Collectors.toList());
	}

	public String getCode() {
		return this.name();
	}
}
