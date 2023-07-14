package run.acloud.api.pl.enums;

import run.acloud.commons.enums.EnumCode;

public enum PlRunType implements EnumCode {

	  LATEST // 일반 실행(실행여부에 따름)
	, BUILD  // 모든 빌드만 실행
	, DEPLOY // 모든 배포만 실행
	, ALL    // 모든 빌드/배포 실행
	;

	public static class Names{
		public static final String LATEST = "LATEST";
		public static final String BUILD = "BUILD";
		public static final String DEPLOY = "DEPLOY";
		public static final String ALL = "ALL";
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
