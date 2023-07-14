package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum PublicCloudAccessType implements EnumCode {
	AWS
	,AWSIAM
	,GCP
	,AZR
	;

	@Override
	public String getCode() {
		return this.name();
	}
}
