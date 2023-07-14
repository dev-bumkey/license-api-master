package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.EnumSet;


public enum StepType implements EnumCode {
	INIT,
	CODE_DOWN,
	USER_TASK,
	FTP,
	HTTP,
	SHELL,
	CREATE_IMAGE,
	CANCEL,
	DELETE,
	DEPLOY;


	public static class Names{
		public static final String INIT = "INIT";
		public static final String CODE_DOWN = "CODE_DOWN";
		public static final String USER_TASK = "USER_TASK";
		public static final String FTP = "FTP";
		public static final String HTTP = "HTTP";
		public static final String SHELL = "SHELL";
		public static final String CREATE_IMAGE = "CREATE_IMAGE";
		public static final String CANCEL = "CANCEL";
		public static final String DELETE = "DELETE";
		public static final String DEPLOY = "DEPLOY";
	}

	// 입력 config 를 가지고 있는 타입
	private final static EnumSet<StepType> CAN_CONTAIN_CONFIG = EnumSet.of(CODE_DOWN, USER_TASK, FTP, HTTP, SHELL, CREATE_IMAGE);

	public boolean canContainConfig(){
		return CAN_CONTAIN_CONFIG.contains(this);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
