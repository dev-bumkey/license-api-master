package run.acloud.api.catalog.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.EnumSet;

public enum TemplateDeploymentType implements EnumCode {
    CONFIG_MAP
    ,SECRET
    ,NET_ATTACH_DEF
	,SERVICE
	,INGRESS
	,DEPLOYMENT
	,STATEFUL_SET
	,DAEMON_SET
	,JOB
	,CRON_JOB
	,HORIZONTAL_POD_AUTOSCALER
	,PERSISTENT_VOLUME_CLAIM
	,SERVICE_ACCOUNT
	,ROLE
	,ROLE_BINDING
	,CUSTOM_OBJECT
	,PACKAGE
	;

	@Override
	public String getCode() {
		return this.name();
	}

	public boolean isWorkload(){
		return EnumSet.of(DEPLOYMENT, STATEFUL_SET, DAEMON_SET, JOB, CRON_JOB).contains(this);
	}
}
