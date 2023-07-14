package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum DockerFileType implements EnumCode {
	CONTENTS, FILE_PATH;

	public static class Names{
		public static final String CONTENTS = "CONTENTS";
		public static final String FILE_PATH = "FILE_PATH";
	}

	@Override
	public String getCode() {
		return this.name();
	}
	
	public static DockerFileType codeOf(String code) {
		return DockerFileType.valueOf(code);
	}
}
