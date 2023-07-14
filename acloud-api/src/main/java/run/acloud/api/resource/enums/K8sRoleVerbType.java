package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum K8sRoleVerbType implements EnumCode {

	  get
	, list
	, watch
	, create
	, delete
	, deletecollection
	, patch
	, update
	, impersonate
	, use

	;

	@Override
	public String getCode() {
		return this.name();
	}
}
