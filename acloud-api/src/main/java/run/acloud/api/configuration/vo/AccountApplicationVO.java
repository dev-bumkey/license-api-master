package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "디지털 서비스 신청 모델")
@Deprecated
public class AccountApplicationVO implements Serializable {
	private static final long serialVersionUID = -7952370884217014957L;

	@Schema(title = "계정 신청 번호")
	private Integer accountApplicationSeq;

	@Schema(title = "제품구분", allowableValues = {"TEAM","ENTERPRISE","CUSTOM"}, example="ENTERPRISE")
	private String prdType;

	@Schema(title = "플랫폼ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String accountCode;

	@Schema(title = "신청자명")
	private String userName;

	@Schema(title = "신청자 Email", requiredMode = Schema.RequiredMode.REQUIRED)
	private String userEmail;

	@Schema(title = "신청자 패스워드", requiredMode = Schema.RequiredMode.REQUIRED)
	private String userPassword;

	@Schema(title = "패스워드 hash salt")
	private String hashSalt;

	@Schema(title = "회사명")
	private String customerName;

	@Schema(title = "회사주소")
	private String customerAddress;

	@Schema(title = "회사연락처", example = "02-123-4567")
	private String customerPhoneNumber;

	@Schema(title = "가입경로", allowableValues = {"검색사이트(Naver, Google 등)","언론기사","관련커뮤니티","세미나 및 컨퍼런스","지인소개","직접가입","기타"}, example="검색사이트(Naver, Google 등)")
	private String registrationPath;

	@Schema(title = "소속기관명", allowableValues="공공,교육,언론미디어,연구,기타", example = "공공")
	private String organizationName;

	@Schema(title = "개인정보수집 및 이용동의", allowableValues = {"Y","N"}, example="Y")
	private String agreePersonalInfoYn;

	@Schema(title = "개인정보수집 및 이용동의 시간")
	private String agreePersonalInfoTime;

	@Schema(title = "마케팅 활용 동의", allowableValues = {"Y","N"}, example="N")
	private String agreeMarketingYn;

	@Schema(title = "마케팅 활용 동의 시간")
	private String agreeMarketingTime;

	@Schema(title = "상태", allowableValues = {"A","R","C"}, example="A")
	private String status;

	@Schema(title = "생성일시")
	private String created;

	@Schema(title = "수정일시")
	private String updated;

	@Schema(title = "수정자")
	private Integer updater;

	@Schema(title = "수정자명")
	private String updaterName;
}
