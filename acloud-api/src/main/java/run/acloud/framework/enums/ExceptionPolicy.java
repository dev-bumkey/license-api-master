package run.acloud.framework.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public enum ExceptionPolicy {

	/**
	 * ExceptionCategory.COMMON
	 */
//	POLICY_COMMON_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "1" , "0", "구체적 오류 번호를 밝힐 수 없음.", ""),
//	POLICY_COMMON_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "2" , "0", "오류를 알 수 없음.", ""),
//	POLICY_COMMON_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "3" , "0", "분류할 수 없는 오류.", ""),
	POLICY_COMMON_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "4" , "0", "잘못된 함수 호출 인자.", ""),
	POLICY_COMMON_004_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "4" , "1", "날짜 형식이 맞지 않습니다.", "v2.5.4"),
	POLICY_COMMON_004_002(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "4" , "2", "{} 항목은 {}자 이상 입력하실 수 없습니다.", "v3.1.0"),
	POLICY_COMMON_004_003(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "4" , "3", "{} 항목은 빈 값을 입력하실 수 없습니다.", "v3.1.0"),
//	POLICY_COMMON_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "5" , "0", "인증되지 않은 요청", ""),
	POLICY_COMMON_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "6" , "0", "외부(보통 사용자환경)에서 API를 호출하면서 전달한 정보가 올바르지 않음.", ""),
	POLICY_COMMON_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "7" , "0", "서버, 작업 등이 올바르지 않은 상태, 혹은 요청을 처리할 수 없는 상태.", ""),
	POLICY_COMMON_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "8" , "0", "권한이 없음.", ""),
	POLICY_COMMON_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "9" , "0", "요청에 대한 권한이 없습니다. 다시 로그인 해 주세요.", "v3.1.1"),
//	POLICY_COMMON_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "10", "0", "설정 정보가 잘못되어 있음", ""),
//	POLICY_COMMON_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "11", "0", "지원하지 않는 기능 또는 요청", ""),
//	POLICY_COMMON_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "12", "0", "작업을 위해 호출한 URL을 찾을 수 없음", ""),
	POLICY_COMMON_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "13", "0", "요청한 자원을 찾을 수 없음", ""),
//	POLICY_COMMON_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "14", "0", "포함되지 않은 객체", ""),
//	POLICY_COMMON_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "15", "0", "클라우드 프로바이더가 일치 하지 않음", ""),
//	POLICY_COMMON_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "16", "0", "클라우드 레전이 일치 하지 않음", ""),
//	POLICY_COMMON_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "17", "0", "인스턴스가 공급되지 않음", ""),
//	POLICY_COMMON_018_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "18", "0", "작업 실패", ""),
	POLICY_COMMON_019_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "19", "0", "시스템 내부 오류", ""),
	POLICY_COMMON_020_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "20", "0", "외부 호출 API가 오류를 반환 또는 호출 실패", ""),
	POLICY_COMMON_020_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.MONITORING_API,   ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "20", "1", "Monitoring API가 오류를 반환 또는 호출 실패", "v2.5.4"),
	POLICY_COMMON_020_002(ExceptionSystem.COCKTAIL_API, ExceptionSystem.METERING_API,	  ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "20", "2", "Metering API가 오류를 반환 또는 호출 실패", "v3.1.0"),
	POLICY_COMMON_020_003(ExceptionSystem.COCKTAIL_API, ExceptionSystem.CLUSTER_API,	  ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "20", "3", "Cluster API가 오류를 반환 또는 호출 실패", "v4.1.0"),
    POLICY_COMMON_020_004(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,	      ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "20", "4", "Package 서비스에 연결 실패", "v4.5.0"),
	POLICY_COMMON_020_005(ExceptionSystem.COCKTAIL_API, ExceptionSystem.AUTH_SERVER,	  ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "20", "5", "Auth API가 오류를 반환 또는 호출 실패", "v4.5.0"),
	POLICY_COMMON_020_006(ExceptionSystem.COCKTAIL_API, ExceptionSystem.GATEWAY_SERVER,	  ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "20", "6", "Gateway API가 오류를 반환 또는 호출 실패", "v4.6.7 - serverless"),
//	POLICY_COMMON_021_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "21", "0", "사용자 role이 맞지 않음", ""),
//	POLICY_COMMON_022_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "22", "0", "추가하려는 정보가 이미 존재하고 있음", ""),
	POLICY_COMMON_023_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "23", "0", "수행할 작업이 없음", ""),
	POLICY_COMMON_024_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "24", "0", "추가하려는 자원이 이미 등록되어 있음", ""),
//	POLICY_COMMON_025_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "25", "0", "자원이 부족한 상태임", ""),
	POLICY_COMMON_026_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "26", "0", "오류가 발생하였습니다.", ""),
	POLICY_COMMON_027_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "27", "0", "등록 중 오류가 발생하였습니다.", ""),
	POLICY_COMMON_028_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "28", "0", "수정 중 오류가 발생하였습니다.", ""),
	POLICY_COMMON_029_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "29", "0", "삭제 중 오류가 발생하였습니다.", ""),
	POLICY_COMMON_030_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "30", "0", "조회 중 오류가 발생하였습니다.", ""),
	POLICY_COMMON_031_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_DB,      ExceptionCategory.COMMON, ExceptionSeverityLevel.FATAL,   "31", "0", "데이터 베이스 작업 중 오류가 발생했습니다. 잠시 후 다시 시도 해 주세요. 문제가 지속될 경우 관리자에게 문의 해 주세요.", "v2.5.4"),
	POLICY_COMMON_032_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_DB,      ExceptionCategory.COMMON, ExceptionSeverityLevel.FATAL,   "32", "0", "데이터 베이스 연결 중 오류가 발생했습니다. 잠시 후 다시 시도 해 주세요. 가능한 경우 데이터 베이스 연결 상태를 점검하거나 관리자에게 문의 해 주세요.", "v2.5.4"),
	POLICY_COMMON_033_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.MESSAGING_SERVER, ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "33", "0", "메시징 서버에 연결중 오류가 발생하였습니다", "v3.0.X"),
	POLICY_COMMON_034_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.MESSAGING_SERVER, ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "34", "0", "메시징 서버의 연결 상태를 조회할 수 없습니다", "v3.0.X"),
	POLICY_COMMON_035_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.MESSAGING_SERVER, ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "35", "0", "메시징 서버와의 연결을 종료중 오류가 발생하였습니다", "v3.0.X"),
	POLICY_COMMON_036_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.MESSAGING_SERVER, ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "36", "0", "메시지를 퍼블리싱 할 수 없습니다", "v3.0.X"),
	POLICY_COMMON_037_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.MESSAGING_SERVER, ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "37", "0", "메시지를 Request를 할 수 없습니다", "v3.0.X"),
	POLICY_COMMON_038_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.MESSAGING_SERVER, ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "38", "0", "메시지에 대한 응답 데이터가 NULL 입니다", "v3.0.X"),
	POLICY_COMMON_039_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "39", "0", "Yaml 형식이 올바르지 않음.", "v3.4.1"),
	POLICY_COMMON_040_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          	  ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "40", "0", "GPU를 지원하지 않음", "v3.4.1"),
	POLICY_COMMON_041_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          	  ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "41", "0", "SCTP를 지원하지 않음.", "v3.4.1"),
	POLICY_COMMON_042_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          	  ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "42", "0", "Multi-nic를 지원하지 않음.", "v3.4.1"),
	POLICY_COMMON_043_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          	  ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "43", "0", "Sriov를 지원하지 않음.", "v3.4.1"),
	POLICY_COMMON_044_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "44" ,"0", "리소스에 대한 권한이 없습니다.", "v3.4.1"),
	POLICY_COMMON_045_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          	  ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "45", "0", "TTLAfterFinished를 지원하지 않음.", "v3.4.1"),
	POLICY_COMMON_046_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.ERROR,   "46", "0", "엑셀 다운로드 중 오류가 발생하였습니다.", "v4.3.0"),
	POLICY_COMMON_047_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "47", "0", "Toml 형식이 올바르지 않음.", "v4.5.12"),
	POLICY_COMMON_048_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "48", "0", "존재하지 않는 리소스입니다.", "v4.6.3"),
	POLICY_COMMON_049_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "49", "0", "지원하지 않는 리소스입니다.", "v4.6.3"),
	POLICY_COMMON_050_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "50", "0", "File을 찾을 수 없습니다.", "v4.7.0"),
	POLICY_COMMON_051_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "51", "0", "File을 읽는 중 오류가 발생하였습니다.", "v4.7.0"),
	POLICY_COMMON_990_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "990", "0", "License가 올바르지 않습니다.", "v4.7.0"),
	POLICY_COMMON_991_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,     ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "991", "0", "License가 만료되었습니다.", "v4.7.0"),


	/**
	 * ExceptionCategory.SYSTEM
	 */
	POLICY_SYSTEM_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "1", "0", "Cube Cluster API 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SYSTEM_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,  "2", "0", "Cube Cluster의 버전이 Cocktail에서 지원하지 않습니다.", "v2.5.4"),
	POLICY_SYSTEM_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "3", "0", "암호화 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SYSTEM_003_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "3", "1", "AES 복호화 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SYSTEM_003_002(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "3", "2", "AES 암호화 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SYSTEM_003_003(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "3", "3", "RSA 복호화 중 오류가 발생하였습니다.", "v4.6.2.8"),
	POLICY_SYSTEM_003_004(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "3", "4", "RSA 암호화 중 오류가 발생하였습니다.", "v4.6.2.8"),
	POLICY_SYSTEM_003_005(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "3", "5", "RSA 공개키 조회 중 오류가 발생하였습니다.", "v4.6.2.8"),
	POLICY_SYSTEM_003_006(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "3", "6", "SHA 암호화 중 오류가 발생하였습니다.", "v4.6.2.8"),
	POLICY_SYSTEM_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "4", "0", "압축 중 오류가 발생하였습니다.", "v3.0.4"),
	POLICY_SYSTEM_004_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "4", "1", "Zip 중 오류가 발생하였습니다.", "v3.0.4"),
	POLICY_SYSTEM_004_002(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,    "4", "2", "Unzip 중 오류가 발생하였습니다.", "v3.0.4"),
	POLICY_SYSTEM_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,  "5", "0", "Cube Cluster API 요청이 실패하였습니다.", "v4.0.0"),
	POLICY_SYSTEM_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.AWS_API,      ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR, 	 "7", "0", "AWS API 오류가 발생하였습니다.", "v4.2.0"),
	POLICY_SYSTEM_007_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.AWS_API,      ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR, 	 "7", "1", "AWS IAM API 오류가 발생하였습니다.", "v4.2.0"),
	POLICY_SYSTEM_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,  "8", "0", "Cluster 시스템 자원은 편집할 수 없습니다", "v4.5.22"),
	POLICY_SYSTEM_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,  "9", "0", "Cluster 시스템 자원은 삭제할 수 없습니다.", "v4.5.22"),
	POLICY_SYSTEM_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.MAIL_SERVER,  ExceptionCategory.SYSTEM, ExceptionSeverityLevel.ERROR,   "10", "0", "메일 발송 중 오류가 발생하였습니다.", "v4.6.2"),
	POLICY_SYSTEM_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING, "11", "0", "신뢰할 수 있는 이미지가 아닙니다.", "v4.5.30"),
	POLICY_SYSTEM_400_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"400", "0", "Cube Cluster API - 잘못된 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_401_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"401", "0", "Cube Cluster API - 비인증된 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_403_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"403", "0", "Cube Cluster API - 미승인된 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_404_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"404", "0", "Cube Cluster API - 찾을 수 없는 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_405_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"405", "0", "Cube Cluster API - 허용되지 않는 method 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_406_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"406", "0", "Cube Cluster API - 승인이 금지된 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_407_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"407", "0", "Cube Cluster API - proxy 인증이 필요한 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_408_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"408", "0", "Cube Cluster API - 요청 시간이 초과한 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_409_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"409", "0", "Cube Cluster API - 충동요소가 발생할 수 있는 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_410_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"410", "0", "Cube Cluster API - 삭제된 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_411_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"411", "0", "Cube Cluster API - content-length가 필요한 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_412_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"412", "0", "Cube Cluster API - header에 있는 precondition이 적절하지 않은 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_413_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"413", "0", "Cube Cluster API - payload가 너무 큰 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_414_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"414", "0", "Cube Cluster API - URI가 너무 긴 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_415_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"415", "0", "Cube Cluster API - 지원하지 않는 media 유형의 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_416_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"416", "0", "Cube Cluster API - Request-header Range 필드의 범위를 충족할 수 없는 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_417_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"417", "0", "Cube Cluster API - Request-header Expect 필드는 적당하지 않은 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_422_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"422", "0", "Cube Cluster API - 요청값이 spec에 맞지 않습니다.", "v4.5.30"),
	POLICY_SYSTEM_423_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"423", "0", "Cube Cluster API - 해당 리소스는 접근하는 것이 잠겨있습니다.", "v4.5.30"),
	POLICY_SYSTEM_424_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"424", "0", "Cube Cluster API - 이전 요청이 실해하여 현재 요청도 실해하였습니다.", "v4.5.30"),
	POLICY_SYSTEM_426_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"426", "0", "Cube Cluster API - 다른 프로토콜로 업그레이드하여 처리하시기 바랍니다.", "v4.5.30"),
	POLICY_SYSTEM_428_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"428", "0", "Cube Cluster API - Request-header의 precondition이 필수인 요청입니다.", "v4.5.30"),
	POLICY_SYSTEM_429_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"429", "0", "Cube Cluster API - 너무 많은 요청을 보냈습니다.", "v4.5.30"),
	POLICY_SYSTEM_431_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"431", "0", "Cube Cluster API - Request-header가 너무 큽니다.", "v4.5.30"),
	POLICY_SYSTEM_451_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SYSTEM, ExceptionSeverityLevel.WARNING,"451", "0", "Cube Cluster API - 법적 이유로 사용할 수 없는 리소스입니다.", "v4.5.30"),

	/**
	 * ExceptionCategory.ACCOUNT
	 */
	POLICY_ACCOUNT_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"1", "0", "계정을 찾을 수 없습니다.", "v3.1.0"),
	POLICY_ACCOUNT_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"2", "0", "계정이 이미 존재합니다.", "v3.1.0"),
	POLICY_ACCOUNT_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"3", "0", "계정 이름은 반드시 입력하여야 합니다..", "v3.1.0"),
	POLICY_ACCOUNT_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"4", "0", "계정에 사용자가 존재합니다.", "v3.1.0"),
	POLICY_ACCOUNT_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"5", "0", "계정에 프로바이더 계정이 존재합니다.", "v3.1.0"),
	POLICY_ACCOUNT_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"6", "0", "시스템의 사용기간이 만료 되었습니다.", "v3.3.0"),
	POLICY_ACCOUNT_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"7", "0", "시스템이 워크스페이스를 포함하고 있습니다.", "v3.5.0"),
	POLICY_ACCOUNT_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"8", "0", "이 플랫폼은 레지스트리를 가질 수 없습니다.", "R4.4.0"),
	POLICY_ACCOUNT_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"9", "0", "시스템이 클러스터를 포함하고 있습니다.", "R4.4.0"),
	POLICY_ACCOUNT_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT, ExceptionSeverityLevel.WARNING,	"10", "0", "싱글테넌시 서비스로 다운그레이드는 지원하지 않습니다.", "R4.6.0"),

	/**
	 * ExceptionCategory.ACCOUNT_APPLICATION
	 */
	POLICY_ACCOUNT_APPLICATION_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT_APPLICATION, ExceptionSeverityLevel.WARNING,	"1", "0", "신청현황을 찾을 수 없습니다.", "v4.6.5"),
	POLICY_ACCOUNT_APPLICATION_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT_APPLICATION, ExceptionSeverityLevel.WARNING,	"2", "0", "신청현황이 이미 존재합니다.", "v4.6.5"),
	POLICY_ACCOUNT_APPLICATION_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ACCOUNT_APPLICATION, ExceptionSeverityLevel.WARNING,	"3", "0", "플랫폼이 등록되어 삭제할 수 없습니다.", "v4.6.5"),

	/**
	 * ExceptionCategory.USER
	 */
	POLICY_USER_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "1", "0", "사용자 Id가 등록되어 있지 않음", ""),
	POLICY_USER_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "2", "0", "사용자 암호가 맞지 않음", ""),
	POLICY_USER_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "3", "0", "로그인 시 지정한 사용자 role이 사용자에게 할당되어 있지 않음", ""),
	POLICY_USER_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "4", "0", "추가하려는 사용자가 이미 등록되어 있음", ""),
	POLICY_USER_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "5", "0", "사용 중지된 사용자 계정으로 접속", ""),
	POLICY_USER_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "6", "0", "Root Admin 사용자는 조작할 수 없음", ""),
	POLICY_USER_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "7", "0", "사용자 권한은 하나만 부여할 수 있습니다.", "v3.1.0"),
	POLICY_USER_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "8", "0", "사용자 권한이 올바르지 않습니다.", "v3.1.0"),
	POLICY_USER_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING,  "9", "0", "허용된 Workspace가 없습니다", "v3.1.0"),
	POLICY_USER_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING, "10", "0", "사용자가 워크스페이스에 속해 있습니다.", "v3.1.0"),
	POLICY_USER_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING, "11", "0", "사용자가 어카운트에 속해 있습니다.", "v3.1.0"),
	POLICY_USER_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING, "12", "0", "Account ID가 올바르지 않음.", "v3.1.0"),
	POLICY_USER_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING, "13", "0", "나 자신은 삭제가 불가능합니다.", "v3.1.1"),
	POLICY_USER_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING, "14", "0", "사용자 암호가 올바르지 않음.", "v4.6.2"),
	POLICY_USER_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING, "15", "0", "자신의 상태는 수정이 불가능합니다.", "v4.6.2"),
	POLICY_USER_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING, "16", "0", "사용자 인증이 실패하였습니다.", "v4.6.2"),
	POLICY_USER_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.USER, ExceptionSeverityLevel.WARNING, "17", "0", "사용자 암호가 올바르지 않습니다.", "v4.5.36-security"),

	/**
	 * ExceptionCategory.PROVIDER_ACCOUNT
	 */
	POLICY_PROVIDER_ACCOUNT_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "1", "0", "프로바이더 credentail의 형식이 올바르지 않음", ""),
	POLICY_PROVIDER_ACCOUNT_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "2", "0", "프로바이더 credentail이 없음.", ""),
	POLICY_PROVIDER_ACCOUNT_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "3", "0", "프로바이더 access key 또는 secret key가 올바르지 않음", ""),
//	POLICY_PROVIDER_ACCOUNT_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "4", "0", "새로 입력하려는 프로바이더 키 값이 이전과 같지 않음", ""),
	POLICY_PROVIDER_ACCOUNT_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "5", "0", "프로바이더를 찾을 수 없음", ""),
	POLICY_PROVIDER_ACCOUNT_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "6", "0", "해당 프로바이더를 사용하는 클러스터가 있음", ""),
	POLICY_PROVIDER_ACCOUNT_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "7", "0", "프로바이더 접속 계정이 올바르지 않음", "v3.1.0"),
	POLICY_PROVIDER_ACCOUNT_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "8", "0", "해당 accountID, billing Account ID로 등록된 프로바이더가 이미 존재함", "v3.1.0"),
	POLICY_PROVIDER_ACCOUNT_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PROVIDER_ACCOUNT, ExceptionSeverityLevel.WARNING, "9", "0", "해당 클러스터에 발급된 사용자 계정이 존재합니다. 권한 회수 후 다시 시도해주세요.", "v4.2.0"),

	/**
	 * ExceptionCategory.CLUSTER
	 */
	POLICY_CLUSTER_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "1", "0", "클러스터 접속 계정이 올바르지 않음", ""),
	POLICY_CLUSTER_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "2", "0", "해당 마스터 URL로 등록된 클러스터가 이미 존재", ""),
	POLICY_CLUSTER_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "3", "0", "클러스터 인증서가 올바르지 않음", ""),
//	POLICY_CLUSTER_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "4", "0", "클러스터 접속이 원활하지 않음.", ""),
	POLICY_CLUSTER_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "5", "0", "클러스터가 서버를 포함하고 있어 클러스터를 수정 또는 삭제할 수 없음", ""),
	POLICY_CLUSTER_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.ERROR,   "6", "0", "클러스터 현황 조회 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_CLUSTER_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.ERROR,   "7", "0", "어플리케이션 현황 조회 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_CLUSTER_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.ERROR,   "8", "0", "볼륨 현황 조회 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_CLUSTER_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "9", "0", "클러스터가 기동 중이 아닙니다. 클러스터 상태를 확인해주세요.", "v3.0.2"),
	POLICY_CLUSTER_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "10", "0", "클러스터가 Access Key가 이미 존재합니다.", "R3.3.0-Package"),
	POLICY_CLUSTER_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "11", "0", "클러스터가 Access Key가 없습니다.", "R3.3.0-Package"),
	POLICY_CLUSTER_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "12", "0", "클러스터가 Access Key가 너무 많습니다. [Data Error]", "R3.3.0-Package"),
	POLICY_CLUSTER_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, 	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "13", "0", "Rollback을 위한 이전 addon 정보가 존재하지 않음.", "R3.4.1"),
	POLICY_CLUSTER_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S, 			ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "14", "0", "노드를 수정할 수 없습니다.", "v3.5"),
	POLICY_CLUSTER_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S, 			ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "15", "0", "클러스터를 찾을 수 없습니다.", "v3.5"),
	POLICY_CLUSTER_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S, 			ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "16", "0", "노드를 찾을 수 없습니다.", "v3.5"),
	POLICY_CLUSTER_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S, 			ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "17", "0", "Taint에 Effect가 필요합니다..", "v3.5"),
	POLICY_CLUSTER_018_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S, 			ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "18", "0", "Taint의 Effect가 올바르지 않습니다.", "v3.5"),
	POLICY_CLUSTER_019_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S, 			ExceptionCategory.CLUSTER, ExceptionSeverityLevel.WARNING, "19", "0", "Taint에 Key가 필요합니다.", "v3.5"),
	POLICY_CLUSTER_020_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,	ExceptionCategory.CLUSTER, ExceptionSeverityLevel.ERROR, "20", "0", "Cluster를 추가할 수 없습니다.", "R4.2.1"),

	/**
	 * ExceptionCategory.APPMAP_GROUP
	 */
	POLICY_APPMAP_GROUP_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICEMAP_GROUP, ExceptionSeverityLevel.WARNING, "1", "0", "그룹이 어플리케이션맵를 포함하고 있어 그룹을 삭제할 수 없음", "v3.1.0"),
	POLICY_APPMAP_GROUP_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICEMAP_GROUP, ExceptionSeverityLevel.WARNING, "2", "0", "그룹명이 이미 존재함", "v3.1.0"),

	/**
	 * ExceptionCategory.GROUP
	 */
	POLICY_GROUP_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.WORKLOAD_GROUP, ExceptionSeverityLevel.WARNING, "1", "0", "그룹이 서버를 포함하고 있어 삭제할 수 없음", ""),
	POLICY_GROUP_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.WORKLOAD_GROUP, ExceptionSeverityLevel.WARNING, "2", "0", "그룹명이 이미 존재함", ""),

	/**
	 * ExceptionCategory.SERVICE
	 */
	POLICY_SERVICE_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "1", "0", "서비스가 클러스터에 등록되어 있음", ""),
	POLICY_SERVICE_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "2", "0", "앱맵이 클러스터를 사용 중", ""),
	POLICY_SERVICE_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "3", "0", "계정에 허용된 Workspace갯수를 초과 했습니다.", "v3.3.0"),
	POLICY_SERVICE_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "4", "0", "이 워크스페이스는 빌드를 포함하고 있어 삭제될 수 없습니다. 빌드를 먼저 삭제해 주세요.", "v3.3.0"),
	POLICY_SERVICE_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "5", "0", "이 워크스페이스의 이미지 레지스트리를 사용하는 워크로드가 존재하여 삭제될 수 없습니다. 워크로드를 먼저 중지 후, 삭제하거나 이미지를 다른 이미지로 교체해 주세요.", "v3.3.0"),
	POLICY_SERVICE_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "6", "0", "레지스트리는 적어도 1개는 등록하여야 함", "v3.5.0"),
	POLICY_SERVICE_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "7", "0", "레지스트리 생성은 1개만 가능함", "v3.5.0"),
	POLICY_SERVICE_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "8", "0", "공유할 레지스트리가 시스템에 존재하지 않음", "v3.5.0"),
	POLICY_SERVICE_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "9", "0", "테넌시를 서비스맵으로 변경하기 위해서는 사용중인 클러스터를 모두 등록 해제하여야 합니다.", "R4.4.0"),
	POLICY_SERVICE_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "10", "0", "테넌시를 클러스터로 변경하기 위해서는 사용중인 서비스맵을 모두 등록 해제하여야 합니다.", "R4.4.0"),
	POLICY_SERVICE_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "11", "0", "클러스터 테넌시는 HARD 와 SOFT 만 가능합니다.", "R4.4.0"),
	POLICY_SERVICE_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "12", "0", "클러스터 테넌시가 올바르지 않습니다. 현재 처리는 서비스맵 유형만 가능합니다.", "R4.4.0"),
	POLICY_SERVICE_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "13", "0", "클러스터 테넌시가 올바르지 않습니다. 현재 처리는 클러스터 유형만 가능합니다.", "R4.4.0"),
	POLICY_SERVICE_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "14", "0", "워크스페이스에서 사용중인 클러스터가 있습니다. 먼저, 클러스터 연결을 해제 해 주세요.", "R4.4.0"),
	POLICY_SERVICE_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "15", "0", "워크스페이스에서 사용중인 서비스맵이 있습니다. 먼저, 서비스맵 연결을 해제 해 주세요.", "R4.4.0"),
	POLICY_SERVICE_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE, ExceptionSeverityLevel.WARNING, "16", "0", "워크스페이스명이 이미 존재 합니다.", "R4.6.2"),

	/**
	 * ExceptionCategory.BUILD
	 */
	POLICY_BUILD_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "1" , "0", "빌드 Job 생성 오류", ""),
	POLICY_BUILD_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "2" , "0", "빌드 Job 삭제 오류", ""),
	POLICY_BUILD_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "3" , "0", "Registry에 이미지명이 이미 존재함", ""),
	POLICY_BUILD_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "4" , "0", "해당 이미지명이 이미 존재함", ""),
	POLICY_BUILD_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "5" , "0", "빌드 서버 접속이 원활하지 않음.", "v2.5.4"),
	POLICY_BUILD_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "6" , "0", "빌드 조회 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_BUILD_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "7" , "0", "빌드 생성 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_BUILD_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "8" , "0", "빌드 수정 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_BUILD_009_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_DB,  ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "9" , "1", "빌드 DB 정보 삭제시 오류가 발생하였습니다.(Running 중 인 빌드는 삭제 불가)", "v2.5.4"),
	POLICY_BUILD_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "10", "0", "빌드 히스토리 조회 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_BUILD_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "11", "0", "빌드 Create Image Step은 하나만 존재해야 합니다", "v3.2.1"),
	POLICY_BUILD_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "12", "0", "계정에 허용된 Build 갯수를 초과 했습니다.", "v3.3.0"),
	POLICY_BUILD_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "13", "0", "허용된 동시 Build수를 초과 했습니다.", "v3.3.0"),
	POLICY_BUILD_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "14", "0", "실행중인 빌드의 히스토리는 삭제할 수 없습니다.", "v3.3.0"),
	POLICY_BUILD_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "15", "0", "파이프라인에서 사용중인 빌드의 히스토리는 삭제할 수 없습니다.", "v4.0.0"),
    POLICY_BUILD_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "16", "0", "빌드 Export 중 오류 발생", "v3.4.21,v4.2.2"),
    POLICY_BUILD_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.ERROR,   "17", "0", "빌드 Import 중 오류 발생", "v3.4.21,v4.2.2"),
    POLICY_BUILD_018_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "18", "0", "빌드 Import파일이 올바르지 않음.", "v3.4.21,v4.2.2"),
    POLICY_BUILD_018_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "18", "1", "빌드 Import파일이 올바르지 않음.(Invalid extension).", "v3.4.21,v4.2.2"),
    POLICY_BUILD_018_002(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "18", "2", "빌드 Import파일이 올바르지 않음.(Invalid Mime-Type).", "v3.4.21,v4.2.2"),
    POLICY_BUILD_019_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "19", "0", "이 빌드 파일은 해당 버전에서 import가 지원되지 않습니다.", "v3.4.21,v4.2.2"),
    POLICY_BUILD_020_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BUILD, ExceptionSeverityLevel.WARNING, "20", "0", "레지스트리가 존재하지 않습니다.", "v3.4.21,v4.2.2"),

	/**
	 * ExceptionCategory.CLUSTER_VOLUME
	 */
	POLICY_CLUSTER_VOLUME_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "1" , "0", "클러스터 볼륨의 이름이 없거나 올바르지 않음", ""),
	POLICY_CLUSTER_VOLUME_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "2" , "0", "클러스터 볼륨의 스토리지 클래스 이름이 없거나 올바르지 않음", ""),
	POLICY_CLUSTER_VOLUME_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "3" , "0", "클러스터 볼륨의 플러그인 이름이 없음", ""),
	POLICY_CLUSTER_VOLUME_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "4" , "0", "클러스터 볼륨의 리크레임 정책 이름이 없음", ""),
	POLICY_CLUSTER_VOLUME_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "5" , "0", "클러스터 볼륨의 클러스터 일련 번호가 없음", ""),
	POLICY_CLUSTER_VOLUME_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "6" , "0", "클러스터 볼륨을 찾을 수 없음", ""),
	POLICY_CLUSTER_VOLUME_006_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "6" , "1", "클러스터 볼륨의 총용량이 이전값 보다 작음", ""),
	POLICY_CLUSTER_VOLUME_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "7" , "0", "이미 생성된 클러스터 볼륨의 스토리지 클래스가 있음", ""),
	POLICY_CLUSTER_VOLUME_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "8" , "0", "클러스터 볼륨 유형을 찾을 수 없음", ""),
	POLICY_CLUSTER_VOLUME_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "9" , "0", "해당 클러스터 볼륨을 사용하고 있음", ""),
	POLICY_CLUSTER_VOLUME_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "10", "0", "고정 클러스터 볼륨은 파라미터가 필요함", ""),
	POLICY_CLUSTER_VOLUME_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "11", "0", "이미 공유된 클러스터 볼륨이 존재함", ""),
	POLICY_CLUSTER_VOLUME_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "12", "0", "해당 클러스터 볼륨은 고정 클러스터 볼륨이 아님", ""),
	POLICY_CLUSTER_VOLUME_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "13", "0", "클러스터 스토리지 유형을 찾을 수 없음", "v3.0.0"),
	POLICY_CLUSTER_VOLUME_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S, 		  ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.ERROR,   "14", "0", "K8S StorageClass 생성 실패", "v3.0.0"),
	POLICY_CLUSTER_VOLUME_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "15", "0", "추가하려는 StorageClass의 이름이 존재함", "v3.0.0"),
	POLICY_CLUSTER_VOLUME_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "16", "0", "클러스터 볼륨의 프로비저너 이름이 없거나 올바르지 않음", "v3.0.0"),
	POLICY_CLUSTER_VOLUME_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "17", "0", "클러스터 볼륨의 프로비저너가 사용 중 입니다.", "v3.0.0"),
	POLICY_CLUSTER_VOLUME_018_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "18", "0", "추가하려는 프로비저너가 존재하지 않습니다.", "v3.0.0"),
	POLICY_CLUSTER_VOLUME_019_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "19", "0", "기본 스토리지는 삭제할 수 없습니다. 다른 스토리지로 기본 지정을 변경 후 다시 시도해주세요.", "v3.4.0"),
	POLICY_CLUSTER_VOLUME_020_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_VOLUME, ExceptionSeverityLevel.WARNING, "20", "0", "기본 스토리지로 지정만 가능합니다.", "v3.5.0"),

	/**
	 * ExceptionCategory.CONFIG_MAP
	 */
	POLICY_CONFIG_MAP_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S ConfigMap 생성 실패", ""),
	POLICY_CONFIG_MAP_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S ConfigMap을 찾을 수 없음", ""),
	POLICY_CONFIG_MAP_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.WARNING, "3" , "0", "컨피그맵의 이름이 없거나 올바르지 않음", ""),
	POLICY_CONFIG_MAP_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.WARNING, "4" , "0", "컨피그맵의 키가 올바르지 않음", ""),
	POLICY_CONFIG_MAP_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.WARNING, "5" , "0", "추가하려는 컨피그맵의 이름이 이미 있음", ""),
	POLICY_CONFIG_MAP_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.WARNING, "6" , "0", "컨피그맵 Data가 올바르지 않음", ""),
	POLICY_CONFIG_MAP_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.WARNING, "7" , "0", "컨피그맵을 사용하는 서버가 존재함", ""),
	POLICY_CONFIG_MAP_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.WARNING, "8" , "0", "컨피그맵 설명이 올바르지 않습니다.", "v3.0.0"),
	POLICY_CONFIG_MAP_008_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CONFIG_MAP, ExceptionSeverityLevel.WARNING, "8" , "1", "컨피그맵 설명의 길이가 초과하였습니다.", "v3.0.0"),

	/**
	 * ExceptionCategory.SECRET
	 */
	POLICY_SECRET_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SECRET, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S Secret 생성 실패", ""),
	POLICY_SECRET_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SECRET, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S Secret을 찾을 수 없음", ""),
	POLICY_SECRET_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SECRET, ExceptionSeverityLevel.WARNING, "3" , "0", "시크릿의 이름이 없거나 올바르지 않음", ""),
	POLICY_SECRET_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SECRET, ExceptionSeverityLevel.WARNING, "4" , "0", "시크릿 Data가 올바르지 않음", ""),
	POLICY_SECRET_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SECRET, ExceptionSeverityLevel.WARNING, "5" , "0", "추가하려는 시크릿의 이름이 존재함", ""),
	POLICY_SECRET_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SECRET, ExceptionSeverityLevel.WARNING, "6" , "0", "시크릿을 사용하는 서버가 존재함", ""),
	POLICY_SECRET_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SECRET, ExceptionSeverityLevel.WARNING, "7" , "0", "시크릿 설명이 올바르지 않습니다.", "v3.0.0"),
	POLICY_SECRET_007_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SECRET, ExceptionSeverityLevel.WARNING, "7" , "1", "시크릿 설명의 길이가 초과하였습니다.", "v3.0.0"),

	/**
	 * ExceptionCategory.NET_ATTACH_DEF
	 */
	POLICY_NET_ATTACH_DEF_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.NET_ATTACH_DEF, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S NetAttachDefC 생성 실패", "v3.4.1"),
	POLICY_NET_ATTACH_DEF_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.NET_ATTACH_DEF, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S NetAttachDefC을 찾을 수 없음", "v3.4.1"),
	POLICY_NET_ATTACH_DEF_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.NET_ATTACH_DEF, ExceptionSeverityLevel.WARNING, "3" , "0", "NetAttachDefC의 이름이 없거나 올바르지 않음", "v3.4.1"),
	POLICY_NET_ATTACH_DEF_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.NET_ATTACH_DEF, ExceptionSeverityLevel.WARNING, "4" , "0", "NetAttachDefC Data가 올바르지 않음", "v3.4.1"),
	POLICY_NET_ATTACH_DEF_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.NET_ATTACH_DEF, ExceptionSeverityLevel.WARNING, "5" , "0", "추가하려는 NetAttachDefC의 이름이 이미 있음", "v3.4.1"),
	POLICY_NET_ATTACH_DEF_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.NET_ATTACH_DEF, ExceptionSeverityLevel.WARNING, "6" , "0", "NetAttachDefC을 사용하는 서버가 존재함", "v3.4.1"),

	/**
	 * ExceptionCategory.SERVICE_SPEC
	 */
	POLICY_SERVICE_SPEC_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE_SPEC, ExceptionSeverityLevel.WARNING, "1" , "0", "Service의 이름이 없거나 올바르지 않음", "v3.2.0"),
	POLICY_SERVICE_SPEC_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE_SPEC, ExceptionSeverityLevel.WARNING, "2" , "0", "추가하려는 Service의 이름이 존재함", "v3.2.0"),
	POLICY_SERVICE_SPEC_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE_SPEC, ExceptionSeverityLevel.WARNING, "3" , "0", "지정한 K8S Service을 찾을 수 없음", "v3.2.0"),
	POLICY_SERVICE_SPEC_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE_SPEC, ExceptionSeverityLevel.WARNING, "4" , "0", "워크로드명과 같은 이름의 ClusterIP 유형의 서비스는 워크로드에서 생성하세요.", "v3.2.0"),
	POLICY_SERVICE_SPEC_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE_SPEC, ExceptionSeverityLevel.WARNING, "5" , "0", "지원하지 않는 프로토콜 입니다.", "v3.4.1"),

	/**
	 * ExceptionCategory.INGRESS_SPEC
	 */
	POLICY_INGRESS_SPEC_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.INGRESS_SPEC, ExceptionSeverityLevel.WARNING, "1" , "0", "Ingress의 이름이 없거나 올바르지 않음", "v3.2.0"),
	POLICY_INGRESS_SPEC_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.INGRESS_SPEC, ExceptionSeverityLevel.WARNING, "2" , "0", "추가하려는 Ingress의 이름이 존재함", "v3.2.0"),
	POLICY_INGRESS_SPEC_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.INGRESS_SPEC, ExceptionSeverityLevel.WARNING, "3" , "0", "지정한 K8S Ingress을 찾을 수 없음", "v3.2.0"),
	POLICY_INGRESS_SPEC_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.INGRESS_SPEC, ExceptionSeverityLevel.WARNING, "4" , "0", "지정한 K8S IngressClass을 찾을 수 없음", "v3.2.0"),

	/**
	 * ExceptionCategory.CATALOG
	 */
	POLICY_CATALOG_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "1" , "0", "작업 대상 카탈로그의 일련 번호가 없음", ""),
	POLICY_CATALOG_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "2" , "0", "카탈로그를 적용할 기존 앱맵의 일련 번호가 없음", ""),
	POLICY_CATALOG_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "3" , "0", "등록하려는 카탈로그의 이름이 이미 있음", ""),
	POLICY_CATALOG_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "4" , "0", "등록하려는 카탈로그의 버전이 이미 있음", ""),
	POLICY_CATALOG_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "5" , "0", "카탈로그 등록 중 오류 발생", ""),
	POLICY_CATALOG_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "6" , "0", "카탈로그 삭제 중 오류 발생", ""),
	POLICY_CATALOG_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "7" , "0", "카탈로그 수정 중 오류 발생", ""),
	POLICY_CATALOG_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "8" , "0", "카탈로그 배포 중 오류 발생", ""),
	POLICY_CATALOG_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "9" , "0", "카탈로그가 사용하는 클러스터를 찾을 수 없음", ""),
//	POLICY_CATALOG_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "10", "0", "카탈로그가 사용하는 레지스트리를 찾을 수 없음", ""),
	POLICY_CATALOG_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "11", "0", "카탈로그가 사용하는 클러스터 볼륨을 찾을 수 없음", ""),
	POLICY_CATALOG_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "12", "0", "등록할 카탈로그의 이름이 없거나 올바르지 않음", ""),
	POLICY_CATALOG_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "13", "0", "카탈로그를 적용할 새 앱맵의 이름이 없거나 올바르지 않음", ""),
//	POLICY_CATALOG_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "14", "0", "Static PersistentVolume을 사용하고 있으면 카탈로그를 등록할 수 없음", ""),
	POLICY_CATALOG_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "15", "0", "카탈로그를 적용할 새 네임스페이스의 이름이 없거나 올바르지 않음", ""),
	POLICY_CATALOG_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "16", "0", "카탈로그 Export 중 오류 발생", "v3.0.4"),
	POLICY_CATALOG_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.ERROR,   "17", "0", "카탈로그 Import 중 오류 발생", "v3.0.4"),
	POLICY_CATALOG_018_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "18", "0", "카탈로그 Import파일이 올바르지 않음.", "v3.0.4"),
	POLICY_CATALOG_018_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "18", "1", "카탈로그 Import파일이 올바르지 않음.(Invalid extension).", "v3.0.4"),
	POLICY_CATALOG_018_002(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CATALOG, ExceptionSeverityLevel.WARNING, "18", "2", "카탈로그 Import파일이 올바르지 않음.(Invalid Mime-Type).", "v3.0.4"),

	/**
	 * ExceptionCategory.SERVER
	 */
	POLICY_SERVER_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "1" , "0", "실행하려는 job type이 올바르지 않음", ""),
	POLICY_SERVER_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "2" , "0", "task가 실행 중이라 새로운 task를 실행할 수 없음", ""),
	POLICY_SERVER_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "3" , "0", "요청한 동작을 수행 할 수 없습니다", ""),
	POLICY_SERVER_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "4" , "0", "현재 상태에서 실행할 수 없는 액션임", ""),
//	POLICY_SERVER_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "5" , "0", "작업이 실행 중", ""),
	POLICY_SERVER_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "6" , "0", "실행할 업무(task)가 없음", ""),
	POLICY_SERVER_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "7" , "0", "생성될 서버(들)가 요청한 메모리의 총합이 클러스터에 남은 메모리 양을 초과", ""),
	POLICY_SERVER_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "8" , "0", "생성될 서버(들)가 요청한 CPU의 총합이 클러스터에 남은 CPU 양을 초과", ""),
	POLICY_SERVER_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "9" , "0", "생성될 서버(들)가 요청한 Pod의 총합이 클러스터에 남은 생성 가능 Pod 수를 초과", ""),
	POLICY_SERVER_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "10", "0", "서버 생성 준비 중 오류 발생", ""),
	POLICY_SERVER_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "11", "0", "서버를 수정할 수 없는 상태", ""),
	POLICY_SERVER_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "12", "0", "서버 수정 준비 중 오류 발생", ""),
	POLICY_SERVER_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "13", "0", "서버 종료 준비 중 오류 발생", ""),
	POLICY_SERVER_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "14", "0", "서버 재시작 준비 중 오류 발생", ""),
	POLICY_SERVER_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "15", "0", "생성될 서버의 이름이 이미 사용 중", ""),
	POLICY_SERVER_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "16", "0", "조회하려는 로그의 갯수가 없음", ""),
	POLICY_SERVER_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "17", "0", "알 수 없는 리소스 타입", ""),
	POLICY_SERVER_018_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "18", "0", "지원하지 않는 서버 형식", ""),
	POLICY_SERVER_019_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "19", "0", "지원하지 않는 볼륨 플러그인", ""),
	POLICY_SERVER_020_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "20", "0", "K8S Deployment 생성을 기다라던 중 timeout(대기 시간은 칵테일에서 설정)", ""),
	POLICY_SERVER_021_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "21", "0", "K8S Pod 생성을 기다라던 중 timeout(대기 시간은 칵테일에서 설정)", ""),
	POLICY_SERVER_022_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "22", "0", "K8S Deployment 삭제 실패", ""),
	POLICY_SERVER_023_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "23", "0", "K8S Replicatset 삭제 실패", ""),
	POLICY_SERVER_024_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "24", "0", "K8S Service(or Load Balancer) 생성을 기다라던 중 timeout(대기 시간은 칵테일에서 설정)", ""),
	POLICY_SERVER_025_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "25", "0", "K8S Pod를 찾을 수 없음", ""),
	POLICY_SERVER_026_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "26", "0", "K8S Object 상태 조회 결과에 status가 없음", ""),
	POLICY_SERVER_027_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "27", "0", "K8S PersistentVolume 생성 실패", ""),
	POLICY_SERVER_027_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "27", "1", "K8S PersistentVolume 수정 실패", ""),
	POLICY_SERVER_028_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "28", "0", "K8S PersistentVolume을 찾을 수 없음", ""),
	POLICY_SERVER_029_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "29", "0", "K8S PersistentVolumeClaim 생성 실패", ""),
	POLICY_SERVER_029_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "29", "1", "K8S PersistentVolumeClaim 수정 실패", ""),
	POLICY_SERVER_030_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "30", "0", "K8S PersistentVolumeClaim 생성 중 timeout(대기 시간은 칵테일에서 설정)", ""),
	POLICY_SERVER_031_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "31", "0", "지정한 K8S Namespace를 찾을 수 없음", ""),
	POLICY_SERVER_032_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "32", "0", "K8S Deployment 생성 실패", ""),
	POLICY_SERVER_033_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "33", "0", "K8S Service 생성 실패", ""),
	POLICY_SERVER_034_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "34", "0", "K8S Ingress 생성 실패", ""),
	POLICY_SERVER_035_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "35", "0", "K8S HorizontalPodAutoscaler 생성 실패", ""),
	POLICY_SERVER_036_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "36", "0", "생성하려는 이름을 가진 볼륨이 이미 있음", ""),
	POLICY_SERVER_037_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "37", "0", "지정한 서버를 찾을 수 없음", ""),
	POLICY_SERVER_038_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "38", "0", "서버가 Cube type이 아님", ""),
	POLICY_SERVER_039_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "39", "0", "서버 설정 수정에 실패", ""),
	POLICY_SERVER_040_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "40", "0", "서버 생성/수정 전 k8s 자원 검사에 실패", ""),
//	POLICY_SERVER_041_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "41", "0", "", ""),
	POLICY_SERVER_042_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "42", "0", "K8S Deployment 정보를 찾을 수 없습니다", ""),
	POLICY_SERVER_043_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "43", "0", "서버타입 변경불가", ""),
	POLICY_SERVER_044_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "44", "0", "서버컨테이너 없음", ""),
	POLICY_SERVER_045_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "45", "0", "호스트 포트 중복", ""),
	POLICY_SERVER_046_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "46", "0", "인그레스 경로가 이미 사용중임", ""),
	POLICY_SERVER_047_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "47", "0", "8S의 로그 타입이 아님", ""),
	POLICY_SERVER_048_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "48", "0", "서버 삭제가 실패하였습니다", ""),
//	POLICY_SERVER_049_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "49", "0", "서버 중지가 실패하였습니다", ""),
	POLICY_SERVER_050_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "50", "0", "노드포트 지정 범위를 벗어남", ""),
	POLICY_SERVER_051_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "51", "0", "노드포트 중복", ""),
//	POLICY_SERVER_052_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "52", "0", "볼륨 삭제 실패", ""),
	POLICY_SERVER_053_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "53", "0", "해당 볼륨이 사용가능한 상태가 아님", ""),
	POLICY_SERVER_054_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "54", "0", "컨테이너명 중복", ""),
	POLICY_SERVER_055_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "55", "0", "namespace명 중복", ""),
	POLICY_SERVER_056_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "56", "0", "서버 중지 가능한 상태가 아닙니다.", ""),
	POLICY_SERVER_057_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "57", "0", "서버 시작 가능한 상태가 아닙니다.", ""),
	POLICY_SERVER_058_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "58", "0", "서버 재시작 가능한 상태가 아닙니다.", ""),
	POLICY_SERVER_059_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "59", "0", "서버 삭제 가능한 상태가 아닙니다.", ""),
	POLICY_SERVER_060_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "60", "0", "해당 PV가 Mount중 입니다.", ""),
//	POLICY_SERVER_061_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "61", "0", "지원하지 않는 클러스터 버전", ""),
	POLICY_SERVER_062_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "62", "0", "Cube Cluster 정보 조회 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SERVER_063_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "63", "0", "Cube Cluster 정보 생성 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SERVER_064_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "64", "0", "Cube Cluster 정보 수정 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SERVER_065_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "65", "0", "Cube Cluster 정보 삭제 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SERVER_066_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "66", "0", "서버 포트가 올바르지 않습니다.( 범위 : 1 ~ 65535 ).", "v2.5.4"),
	POLICY_SERVER_067_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "67", "0", "서버 포트 범위가 올바르지 않습니다.", "v2.5.4"),
	POLICY_SERVER_068_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "68", "0", "서버 포트 범위 형식이 올바르지 않습니다.( e.g. 8080-8082 )", "v2.5.4"),
	POLICY_SERVER_069_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "69", "0", "서버 타켓 포트와 포트의 범위가 동일하지 않습니다.( e.g. 8080-8082, 9090-9092 )", "v2.5.4"),
	POLICY_SERVER_070_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "70", "0", "Cube Cluster의 클러스터 리소스 제한량 조회 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SERVER_071_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "71", "0", "서비스가 존재하지 않습니다.", "v2.5.4"),
	POLICY_SERVER_072_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "72", "0", "서버 실행 중 오류가 발생하였습니다.", "v2.5.4"),
	POLICY_SERVER_073_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "73", "0", "서버 볼륨 설정이 올바르지 않습니다. 다시 확인 해주세요.", "v2.5.4"),
	POLICY_SERVER_073_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "73", "1", "서버 볼륨명은 빈값으로 설정할 수 없습니다.", "v2.5.4"),
    POLICY_SERVER_073_002(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "73", "2", "서버 마운트의 볼륨명은 빈값으로 설정할 수 없습니다.", "v2.5.4"),
    POLICY_SERVER_073_003(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "73", "3", "서버 마운트의 볼륨명이 볼륨에 존재하지 않습니다.", "v2.5.4"),
    POLICY_SERVER_073_004(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "73", "4", "서버 볼륨의 Linked 유형의 PVC 항목은 빈값으로 설정할 수 없습니다.", "v3.0.0"),
    POLICY_SERVER_074_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "74", "0", "Port Range는 지정 노드 포트를 설정하실 수 없습니다.", "v2.5.4"),
    POLICY_SERVER_075_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "75", "0", "지정 노드포트 값이 없습니다.", "v2.5.4"),
	POLICY_SERVER_076_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "76", "0", "서버 제거 준비 중 오류 발생.", "v2.5.4"),
	POLICY_SERVER_077_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "77", "0", "공유 볼륨이 이미 존재합니다.", "v3.0.0"),
	POLICY_SERVER_077_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "77", "1", "PVC가 이미 존재합니다.", "v3.0.0"),
	POLICY_SERVER_077_002(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "77", "2", "PV가 이미 존재합니다.", "v3.0.0"),
	POLICY_SERVER_077_003(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "77", "3", "PVC가 존재하지 않습니다.", "v3.0.0"),
	POLICY_SERVER_078_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "78", "0", "Sticky Session을 지정하였다면, timeout 값을 지정하여야 함.", "v3.1.0"),
	POLICY_SERVER_079_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "79", "0", "Sticky Session의 timeout 값이 올바르지 않습니다.(1초 ~ 86400초)", "v3.1.0"),
	POLICY_SERVER_080_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "80", "0", "Ingress Path가 너무 깁니다.(200자 이하)", "v3.1.1"),
	POLICY_SERVER_081_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "81", "0", "Ingress Host가 너무 깁니다.(200자 이하)", "v3.2.1"),
	POLICY_SERVER_082_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "82", "0", "인그레스 Host가 이미 사용중임", "v3.2.1"),
	POLICY_SERVER_083_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "83", "0", "K8S DaemonSet 생성 실패", "v3.4.0"),
	POLICY_SERVER_084_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "84", "0", "K8S DaemonSet 삭제 실패", "v3.4.0"),
	POLICY_SERVER_085_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "85", "0", "K8S Job 생성 실패", "v3.4.0"),
	POLICY_SERVER_086_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "86", "0", "K8S Job 삭제 실패", "v3.4.0"),
	POLICY_SERVER_087_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "87", "0", "K8S CronJob 생성 실패", "v3.4.0"),
	POLICY_SERVER_088_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "88", "0", "K8S CronJob 삭제 실패", "v3.4.0"),
	POLICY_SERVER_089_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "89", "0", "K8S StatefulSet 생성 실패", "v3.4.0"),
	POLICY_SERVER_090_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.ERROR,   "90", "0", "K8S StatefulSet 삭제 실패", "v3.4.0"),
	POLICY_SERVER_091_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "91", "0", "서버가 정지된 상태에서만 수정 가능합니다.", "v3.5.0"),
	POLICY_SERVER_092_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "92", "0", "StatefulSet 워크로드는 반드시 Headless 서비스가 존재하여야 합니다.", "v3.5.0"),
	POLICY_SERVER_093_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "93", "0", "Horizontal Pod Autoscaler Name is null", "v4.5.21"),
	POLICY_SERVER_094_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "94", "0", "Horizontal Pod Autoscaler Name 은 변경할 수 없습니다.", "v4.5.21"),
	POLICY_SERVER_095_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVER, ExceptionSeverityLevel.WARNING, "95", "0", "이미 사용중인 Horizontal Pod Autoscaler Name 입니다.", "v4.5.21"),

	/**
	 * ExceptionCategory.PIPELINE
	 */
	POLICY_PIPELINE_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.ERROR,   "1", "0", "Pipeline 생성 실패", ""),
    POLICY_PIPELINE_001_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.WARNING, "1", "1", "Pipeline 생성 파라미터가 올바르지 않습니다.", "v2.5.4"),
    POLICY_PIPELINE_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.ERROR,   "2", "0", "Pipeline 수정 실패", ""),
    POLICY_PIPELINE_002_001(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.WARNING, "2", "1", "Pipeline 수정 파라미터가 올바르지 않습니다.", "v2.5.4"),
    POLICY_PIPELINE_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.ERROR,   "3", "0", "지정한 Pipeline을 찾을 수 없음", ""),
    POLICY_PIPELINE_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.WARNING, "4", "0", "서버가 실행 중인 상태가 아니어서 pipeline을 실행할 수 없음.", ""),
    POLICY_PIPELINE_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.WARNING, "5", "0", "Pipeline이 실행 중인 상태여서 다른 동작을 수행할 수 없음.", ""),
    POLICY_PIPELINE_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.WARNING, "6", "0", "파이프라인 실행 중 동일한 빌드 작업이 존재하여 실행할 수 없습니다. 동일한 빌드를 포함하는 파이프라인은 각각 실행하여 주세요.", ""),
    POLICY_PIPELINE_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PIPELINE, ExceptionSeverityLevel.ERROR,   "7", "0", "파이프라인 실행 중 오류가 발생하였습니다.", "v2.5.4"),

	/**
	 * ExceptionCategory.REGISTRY
	 */
	POLICY_REGISTRY_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "1" , "0", "레지스트리에서 이미지 목록을 받을 수 없음", ""),
	POLICY_REGISTRY_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "2" , "0", "레지스트리에 로그인 할 수 없음", ""),
	POLICY_REGISTRY_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "3" , "0", "레지스트리에서 이미지 태그 목록을 받을 수 없음", ""),
	POLICY_REGISTRY_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "4" , "0", "레지스트리에 사용자를 등록할 수 없음", ""),
	POLICY_REGISTRY_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "5" , "0", "레지스트리에 로그인하려는 사용자가 등록되어 있지 않음", ""),
	POLICY_REGISTRY_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "6" , "0", "레지스트리 사용자의 암호를 수정할 수 없음", ""),
//	POLICY_REGISTRY_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "7" , "0", "레지스트리에서 등록된 사용자를 삭제할 수 없음", ""),
	POLICY_REGISTRY_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "8" , "0", "레지스트리에 등록된 사용자를 프로젝트에 할당할 수 없음", ""),
	POLICY_REGISTRY_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "9" , "0", "레지스트리에 등록된 사용자를 프로젝트에서 제외할 수 없음", ""),
	POLICY_REGISTRY_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "10", "0", "레지스트리에 프로젝트를 등록할 수 없음", ""),
	POLICY_REGISTRY_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "11", "0", "레지스트리에 프로젝트가 이미 있음", ""),
	POLICY_REGISTRY_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "12", "0", "레지스트리에 프로젝트를 삭제할 수 없음", ""),
	POLICY_REGISTRY_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "13", "0", "레지스트리 프로젝트 목록을 받을 수 없음", "v3.5.0"),
	POLICY_REGISTRY_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "14", "0", "레지스트리 프로젝트가 등록되어 있지 않음", "v3.5.0"),
	POLICY_REGISTRY_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "15", "0", "레지스트리 동작 중 오류가 발생했습니다.", "v3.5.0"),
	POLICY_REGISTRY_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "16", "0", "이 레지스트리는 빌드를 포함하고 있어 삭제될 수 없습니다. 빌드를 먼저 삭제해 주세요.", "R4.4.0"),

	POLICY_REGISTRY_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "17" , "0", "레지스트리 접속 정보가 올바르지 않음,", "R4.6.5"),
	POLICY_REGISTRY_018_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,     ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "18" , "0", "레지스트리 사설 인증서 정보가 올바르지 않음,", "R4.6.5"),
	POLICY_REGISTRY_019_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.REGISTRY, ExceptionSeverityLevel.WARNING, "19" , "0", "플랫폼에 레지스트리가 이미 존재 합니다.", "R4.6.5"),

	/**
	 * ExceptionCategory.PACKAGE_SERVER
	 */
	POLICY_PACKAGE_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,    "1" , "0", "Package 워크로드의 Event 조회중 오류 발생.", "R3.3.0-Package"),
	POLICY_PACKAGE_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,    "2" , "0", "Package 워크로드의 Resource 조회중 오류 발생.", "R3.3.0-Package"),
	POLICY_PACKAGE_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,    "3" , "0", "Package 워크로드의 상세 정보 이벤트 조회중 오류 발생.", "R3.3.0-Package"),
	POLICY_PACKAGE_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,    "4" , "0", "StorageClass 조회중 오류 발생.", "R3.3.0-Package"),
	POLICY_PACKAGE_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,    "5" , "0", "Package 워크로드의 상태 조회중 오류 발생.", "R3.3.0-Package"),
	POLICY_PACKAGE_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING,  "6" , "0", "클러스터 컨트롤러와의 연결이 유효하지 않습니다.", "R3.3.0-Package"),
	POLICY_PACKAGE_007_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,    "7" , "0", "Package 배포 중 오류 발생", "R4.0"),
	POLICY_PACKAGE_008_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,    "8" , "0", "Package 업그레이드 중 오류 발생", "R4.0"),
	POLICY_PACKAGE_009_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,    "9" , "0", "Package 롤백 중 오류 발생", "R4.0"),
	POLICY_PACKAGE_010_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,   "10" , "0", "Package 제거 중 오류 발생", "R4.0"),
	POLICY_PACKAGE_011_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,   "11" , "0", "Package 목록 조회 중 오류 발생", "R4.0"),
	POLICY_PACKAGE_012_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,   "12" , "0", "Package 상태 조회 중 오류 발생", "R4.0"),
	POLICY_PACKAGE_013_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.ERROR,   "13" , "0", "Package 아력 조회 중 오류 발생", "R4.0"),
	POLICY_PACKAGE_014_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "14" , "0", "이미 사용중인 Release Name 입니다.", "R4.0"),
	POLICY_PACKAGE_015_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "15" , "0", "Chart 목록 조회 중 오류 발생", "R4.2.1"),
	POLICY_PACKAGE_016_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "16" , "0", "Add-on Name이 이미 사용중입니다.", "R4.3.0"),
	POLICY_PACKAGE_017_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "17" , "0", "이미 설치된 Add-on 입니다.", "R4.3.0"),
	POLICY_PACKAGE_018_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "18" , "0", "Add-on 설치 정보가 이미 존재합니다.", "R4.3.0"),
	POLICY_PACKAGE_019_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "19" , "0", "Add-on이 설치 가능한 갯수를 넘었습니다. 더 이상 설치할 수 없습니다. ", "R4.3.0"),
	POLICY_PACKAGE_020_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "20" , "0", "Add-on 설치를 위해 Release Name이 필요합니다.", "R4.3.0"),
	POLICY_PACKAGE_021_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "21" , "0", "Add-on 설치를 위해 Chart Name이 필요합니다.", "R4.3.0"),
	POLICY_PACKAGE_022_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "22" , "0", "Add-on 설치를 위해 Chart Version이 필요합니다.", "R4.3.0"),
	POLICY_PACKAGE_023_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "23" , "0", "Add-on 설정 정보 파일이 올바르지 않습니다.", "R4.3.0"),
	POLICY_PACKAGE_024_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "24" , "0", "Add-on 구성 정보가 잘 못 되었습니다.", "R4.3.0"),
	POLICY_PACKAGE_025_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "25" , "0", "Add-on 이름이 잘 못 되었습니다.", "R4.3.0"),
	POLICY_PACKAGE_026_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.PACKAGE,      ExceptionCategory.PACKAGE, ExceptionSeverityLevel.WARNING, "26" , "0", "Istio-init Add-on 을 먼저 설치하여야 합니다.", "R4.3.0"),

	/**
	 * ExceptionCategory.BILLING
	 */
	POLICY_BILLING_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BILLING, ExceptionSeverityLevel.WARNING, "1" , "0", "청구서 시퀀스가 적합하지 않습니다.", "R3.3.0-Package"),
	POLICY_BILLING_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.BILLING, ExceptionSeverityLevel.WARNING, "2" , "0", "청구서 정보가 존재하지 않습니다.", "R3.3.0-Package"),

	/**
	 * ExceptionCategory.CLUSTER_ROLE
	 */
	POLICY_CLUSTER_ROLE_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CLUSTER_ROLE, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S ClusterRole 생성 실패", "R4.3.0"),
	POLICY_CLUSTER_ROLE_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CLUSTER_ROLE, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S ClusterRole을 찾을 수 없음", "R4.3.0"),
	POLICY_CLUSTER_ROLE_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_ROLE, ExceptionSeverityLevel.WARNING, "3" , "0", "ClusterRole 이름이 없거나 올바르지 않음", "R4.3.0"),
	POLICY_CLUSTER_ROLE_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_ROLE, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 ClusterRole의 이름이 존재함", "R4.3.0"),
	POLICY_CLUSTER_ROLE_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_ROLE, ExceptionSeverityLevel.WARNING, "5" , "0", "해당 ClusterRole을 사용하는 binding이 존재합니다. 먼저 binding을 삭제해주세요.", "R4.5.25"),

	/**
	 * ExceptionCategory.CLUSTER_ROLE_BINDING
	 */
	POLICY_CLUSTER_ROLE_BINDING_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CLUSTER_ROLE_BINDING, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S ClusterRoleBinding 생성 실패", "R4.3.0"),
	POLICY_CLUSTER_ROLE_BINDING_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CLUSTER_ROLE_BINDING, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S ClusterRoleBinding을 찾을 수 없음", "R4.3.0"),
	POLICY_CLUSTER_ROLE_BINDING_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_ROLE_BINDING, ExceptionSeverityLevel.WARNING, "3" , "0", "ClusterRoleBinding 이름이 없거나 올바르지 않음", "R4.3.0"),
	POLICY_CLUSTER_ROLE_BINDING_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CLUSTER_ROLE_BINDING, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 ClusterRoleBinding의 이름이 존재함", "R4.3.0"),

	/**
	 * ExceptionCategory.ROLE
	 */
	POLICY_ROLE_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.ROLE, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S Role 생성 실패", "R4.2.0"),
	POLICY_ROLE_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.ROLE, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S Role을 찾을 수 없음", "R4.2.0"),
	POLICY_ROLE_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ROLE, ExceptionSeverityLevel.WARNING, "3" , "0", "Role 이름이 없거나 올바르지 않음", "R4.2.0"),
	POLICY_ROLE_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ROLE, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 Role의 이름이 존재함", "R4.2.0"),
	POLICY_ROLE_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ROLE, ExceptionSeverityLevel.WARNING, "5" , "0", "해당 Role을 사용하는 binding이 존재합니다. 먼저 binding을 삭제해주세요.", "R4.5.25"),

	/**
	 * ExceptionCategory.ROLE_BINDING
	 */
	POLICY_ROLE_BINDING_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.ROLE_BINDING, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S RoleBinding 생성 실패", "R4.2.0"),
	POLICY_ROLE_BINDING_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.ROLE_BINDING, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S RoleBinding을 찾을 수 없음", "R4.2.0"),
	POLICY_ROLE_BINDING_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ROLE_BINDING, ExceptionSeverityLevel.WARNING, "3" , "0", "RoleBinding 이름이 없거나 올바르지 않음", "R4.2.0"),
	POLICY_ROLE_BINDING_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.ROLE_BINDING, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 RoleBinding의 이름이 존재함", "R4.2.0"),

	/**
	 * ExceptionCategory.SERVICE_ACCOUNT
	 */
	POLICY_SERVICE_ACCOUNT_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVICE_ACCOUNT, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S ServiceAccount 생성 실패", "R4.2.0"),
	POLICY_SERVICE_ACCOUNT_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.SERVICE_ACCOUNT, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S ServiceAccount을 찾을 수 없음", "R4.2.0"),
	POLICY_SERVICE_ACCOUNT_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE_ACCOUNT, ExceptionSeverityLevel.WARNING, "3" , "0", "ServiceAccount 이름이 없거나 올바르지 않음", "R4.2.0"),
	POLICY_SERVICE_ACCOUNT_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.SERVICE_ACCOUNT, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 ServiceAccount의 이름이 존재함", "R4.2.0"),

	/**
	 * ExceptionCategory.CUSTOM_OBJECT
	 */
	POLICY_CUSTOM_OBJECT_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CUSTOM_OBJECT, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S CustomObject 생성 실패", "R4.2.0"),
	POLICY_CUSTOM_OBJECT_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CUSTOM_OBJECT, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S CustomObject을 찾을 수 없음", "R4.2.0"),
	POLICY_CUSTOM_OBJECT_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CUSTOM_OBJECT, ExceptionSeverityLevel.WARNING, "3" , "0", "CustomObject 이름이 없거나 올바르지 않음", "R4.2.0"),
	POLICY_CUSTOM_OBJECT_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CUSTOM_OBJECT, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 CustomObject의 이름이 존재함", "R4.2.0"),
	POLICY_CUSTOM_OBJECT_005_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.CUSTOM_OBJECT, ExceptionSeverityLevel.ERROR,   "5" , "0", "K8S CustomObject 수정 실패", "R4.6.1"),
	POLICY_CUSTOM_OBJECT_006_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.CUSTOM_OBJECT, ExceptionSeverityLevel.WARNING, "6" , "0", "CustomObject의 이름은 변경할 수 없음", "R4.6.1"),

	/**
	 * ExceptionCategory.POD_SECURITY_POLICY
	 */
	POLICY_POD_SECURITY_POLICY_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.POD_SECURITY_POLICY, ExceptionSeverityLevel.WARNING, "1" , "0", "Pod Security Policy의 이름이 없거나 올바르지 않음", "v4.3.0"),
	POLICY_POD_SECURITY_POLICY_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.POD_SECURITY_POLICY, ExceptionSeverityLevel.WARNING, "2" , "0", "추가하려는 Pod Security Policy의 이름이 존재함", "v4.3.0"),
	POLICY_POD_SECURITY_POLICY_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.POD_SECURITY_POLICY, ExceptionSeverityLevel.ERROR, "3" , "0", "지정한 K8S Pod Security Policy을 찾을 수 없음", "v4.3.0"),
	POLICY_POD_SECURITY_POLICY_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.POD_SECURITY_POLICY, ExceptionSeverityLevel.WARNING, "4" , "0", "해당 파드 보안 설정을 바인딩하는 리소스가 있어 삭제할 수 없습니다.", "v4.5.23"),

	/**
	 * ExceptionCategory.LIMIT_RANGE
	 */
	POLICY_LIMIT_RANGE_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.LIMIT_RANGE, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S LimitRange 생성 실패", "R4.3.0"),
	POLICY_LIMIT_RANGE_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.LIMIT_RANGE, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S LimitRange을 찾을 수 없음", "R4.3.0"),
	POLICY_LIMIT_RANGE_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.LIMIT_RANGE, ExceptionSeverityLevel.WARNING, "3" , "0", "LimitRange 이름이 없거나 올바르지 않음", "R4.3.0"),
	POLICY_LIMIT_RANGE_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.LIMIT_RANGE, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 LimitRange의 이름이 존재함", "R4.3.0"),

	/**
	 * ExceptionCategory.RESOURCE_QUOTA
	 */
	POLICY_RESOURCE_QUOTA_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.RESOURCE_QUOTA, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S ResourceQuota 생성 실패", "R4.3.0"),
	POLICY_RESOURCE_QUOTA_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.RESOURCE_QUOTA, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S ResourceQuota을 찾을 수 없음", "R4.3.0"),
	POLICY_RESOURCE_QUOTA_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.RESOURCE_QUOTA, ExceptionSeverityLevel.WARNING, "3" , "0", "ResourceQuota 이름이 없거나 올바르지 않음", "R4.3.0"),
	POLICY_RESOURCE_QUOTA_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.RESOURCE_QUOTA, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 ResourceQuota의 이름이 존재함", "R4.3.0"),

	/**
	 * ExceptionCategory.NETWORK_POLICY
	 */
	POLICY_NETWORK_POLICY_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.NETWORK_POLICY, ExceptionSeverityLevel.ERROR,   "1" , "0", "K8S NetworkPolicy 생성 실패", "R4.3.0"),
	POLICY_NETWORK_POLICY_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.K8S,          ExceptionCategory.NETWORK_POLICY, ExceptionSeverityLevel.ERROR,   "2" , "0", "지정한 K8S NetworkPolicy을 찾을 수 없음", "R4.3.0"),
	POLICY_NETWORK_POLICY_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.NETWORK_POLICY, ExceptionSeverityLevel.WARNING, "3" , "0", "NetworkPolicy 이름이 없거나 올바르지 않음", "R4.3.0"),
	POLICY_NETWORK_POLICY_004_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.NETWORK_POLICY, ExceptionSeverityLevel.WARNING, "4" , "0", "추가하려는 NetworkPolicy의 이름이 존재함", "R4.3.0"),

	/**
	 * ExceptionCategory.ALERT_RULE
	 */
	POLICY_ALERT_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_DB,          ExceptionCategory.NETWORK_POLICY, ExceptionSeverityLevel.WARNING,   "1" , "0", "AlertRuleID Already Exists", "R4.5.21"),

	/**
	 * ExceptionCategory.EXTERNAL_REGISTRY
	 */
	POLICY_EXTERNAL_REGISTRY_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.REGISTRY,          ExceptionCategory.EXTERNAL_REGISTRY, ExceptionSeverityLevel.WARNING,   "1" , "0", "외부 레지스트리 접속 정보가 올바르지 않음,", "R4.7.0"),
	POLICY_EXTERNAL_REGISTRY_002_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,      ExceptionCategory.EXTERNAL_REGISTRY, ExceptionSeverityLevel.WARNING,   "2" , "0", "외부 레지스트리 사설 인증서 정보가 올바르지 않음,", "R4.7.0"),
	POLICY_EXTERNAL_REGISTRY_003_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API,      ExceptionCategory.EXTERNAL_REGISTRY, ExceptionSeverityLevel.WARNING,   "3" , "0", "워크스페이스에서 사용 중이어서 삭제할 수 없습니다.", "R4.7.0"),

	/**
	 * "," 누락으로 인한 오류 방지용 Dummy Code
	 */
	POLICY_DUMMY_001_000(ExceptionSystem.COCKTAIL_API, ExceptionSystem.COCKTAIL_API, ExceptionCategory.COMMON, ExceptionSeverityLevel.WARNING, "1" , "0", "Dummy.", "")
	;

	@Getter
	private ExceptionSystem system;

	@Getter
	private ExceptionSystem section;

	@Getter
	private ExceptionCategory category;

	@Getter
	private ExceptionSeverityLevel severityLevel;

	@Getter
	private String majorCode;

	@Getter
	private String detailCode;

	@Getter
	private String message;

	@Getter
	private String etc;

	ExceptionPolicy(ExceptionSystem system, ExceptionSystem section, ExceptionCategory category, ExceptionSeverityLevel severityLevel, String majorCode, String detailCode, String message, String etc) {
		this.system = system;
		this.section = section;
		this.category = category;
		this.severityLevel = severityLevel;
		this.majorCode = majorCode;
		this.detailCode = detailCode;
		this.message = message;
		this.etc = etc;
	}

	public String getErrorCode(){
		return String.format("%1s%1s%2s%1s%3s%3s",
				this.system.getSystemCode(),
				this.section.getSystemCode(),
				this.category.getCategoryCode(),
				this.severityLevel.getSeverityLevel(),
				StringUtils.leftPad(this.majorCode, 3, "0"),
				StringUtils.leftPad(this.detailCode, 3, "0"));
	}

	public String getCode() {
		return this.name();
	}
}
