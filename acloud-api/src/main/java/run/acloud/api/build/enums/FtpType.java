package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum FtpType implements EnumCode {
	FTP, SFTP;

	public static class Names{
		public static final String FTP = "FTP";
		public static final String SFTP = "SFTP";
	}

	@Override
	public String getCode() {
		return this.name();
	}
	
	public static FtpType codeOf(String code) {
		return FtpType.valueOf(code);
	}
}
