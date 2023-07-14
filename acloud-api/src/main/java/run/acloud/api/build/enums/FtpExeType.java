package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum FtpExeType implements EnumCode {
	FTP_UPLOAD, FTP_DOWN, DIR_FTP_UPLOAD, DIR_FTP_DOWN;


	public static class Names{
		public static final String FTP_UPLOAD = "FTP_UPLOAD";
		public static final String FTP_DOWN = "FTP_DOWN";
		public static final String DIR_FTP_UPLOAD = "DIR_FTP_UPLOAD";
		public static final String DIR_FTP_DOWN = "DIR_FTP_DOWN";
	}

	@Override
	public String getCode() {
		return this.name();
	}
	
	public static FtpExeType codeOf(String code) {
		return FtpExeType.valueOf(code);
	}
}
