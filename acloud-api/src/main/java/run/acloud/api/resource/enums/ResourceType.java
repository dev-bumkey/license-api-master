package run.acloud.api.resource.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import run.acloud.commons.enums.EnumCode;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ResourceType implements EnumCode {
	PROVIDER("PVC"), NETWORK("NET"), SERVICE("SVC"), SERVICEMAP("MAP"),
	/*SECURITY_GROUP("SEG"), LOADBALANCER("ELB"), COMPUTE("INS"), VOLUME("EBS"), */SERVER("SVR"), /*RDB("RDS"), STORAGE("S3S"),*/
	CONTAINER("CTN")
	;
	
	@Getter
	@NonNull
	String code;
}
