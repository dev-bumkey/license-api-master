package run.acloud.framework.enums;

public enum AuditAdditionalType {

	LAUNCH_TEMPLATE,
	PIPELINE_DEPLOY
	;

	public String getCode() {
		return this.name();
	}
}
