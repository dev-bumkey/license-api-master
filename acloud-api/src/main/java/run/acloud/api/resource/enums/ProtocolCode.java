package run.acloud.api.resource.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

@AllArgsConstructor(access=AccessLevel.PRIVATE)
public enum ProtocolCode implements EnumCode {
	ALL("-1"), TCP("tcp"), UDP("udp"), ICMP("icmp"), HTTP("http"), HTTPS("https"), SSL("ssl");
	
	@Getter
	String awsCode;

	public static ProtocolCode codeOf(String code) {
		return ProtocolCode.valueOf(code);
	}
	
	@Override
	public String getCode() {
		return this.name();
	}

}
