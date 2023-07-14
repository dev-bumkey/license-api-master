package run.acloud.api.auth.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.auth.Util.ServiceUtil;
import run.acloud.api.auth.enums.LanguageType;
import run.acloud.api.auth.enums.ServiceMode;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.api.configuration.vo.ClusterDetailNamespaceVO;
import run.acloud.api.configuration.vo.ServiceVO;
import run.acloud.api.configuration.vo.UserClusterRoleIssueVO;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.commons.vo.CocktailLicenseValidVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class UserVO extends HasUseYnVO {

	@Schema(title = "사용자 일련번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer userSeq;

	@Schema(title = "사용자 ID(email 형식)")
	private String userId;

	@Schema(title = "사용자 이름")
	private String userName;

    @Schema(title = "사용자 언어")
	private LanguageType userLanguage;

    @Schema(title = "사용자 시간대")
	private String userTimezone;

	@Schema(title = "사용자 암호")
	private String password;

	@Schema(title = "Hash Salt")
	@JsonIgnore
	private String hashSalt;

	@Schema(title = "마지막 로그인 시각")
	private String lastLogin;

	@Schema(title = "활성화 일시")
	private String activeDatetime;

	@Schema(title = "활성화 일시")
	@JsonIgnore
	private int sleepPeriod;

	@Schema(title = "휴면 여부", allowableValues = {"Y","N"})
	private String sleepYn;

	@Schema(title = "사용자 role. 사용자는 하나의 role만 가질 수 있다.", allowableValues = {"ADMIN","SYSUSER","SYSTEM","DEVOPS"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private List<String> roles;

	private String token;

	private String userRole;

	private String userGrant;

    @Schema(title = "API ServiceMode", allowableValues = {"PRD","HYDCARD","SECURITY_ONLINE"})
    private ServiceMode serviceMode = ServiceUtil.getServiceMode();

    @Schema(title = "변경할 비밀번호")
    private String newPassword;

	@Schema(title = "비밀번호 초기화 처리 여부, 폅집시 사용 => 기존 내부 처리용", allowableValues = {"Y","N"})
	private String initPasswordYn = "N";

	@Schema(title = "비밀번호 초기화 여부, 비밀번호 초기화시 필요 => 신규 처리", allowableValues = {"Y","N"})
	private String resetPasswordYn = "N";

	@Schema(title = "사용자 비활성 여부", allowableValues = {"Y","N"})
	private String inactiveYn = "N";

	@Schema(title = "로그인 실패 횟수")
	private int loginFailCount = 0;

	private boolean passwordChangeRequired = false;

	private boolean passwordPeriodExpired = false;

    @Schema(title = "OTP 사용 여부", allowableValues = {"Y","N"})
    private String otpUseYn;

	@Schema(title = "사용자 직책")
	private String userJob;

	@Schema(title = "사용자 부서")
	private String userDepartment;

	@Schema(title = "전화번호")
	private String phoneNumber;

	@Schema(title = "카카오아이디")
	private String kakaoId;

	@Schema(title = "이메일")
	private String email;

	@Schema(title = "설명")
	private String description;

	@Schema(title = "추가컬럼1")
	private String additionalColumn1;

	@Schema(title = "추가컬럼2")
	private String additionalColumn2;

	@Schema(title = "추가컬럼3")
	private String additionalColumn3;

	@Schema(title = "마지막 서비스(워크스페이스) seq")
	private String lastServiceSeq;

	@Schema(title = "마지막 서비스(워크스페이스)")
	private ServiceVO lastService;

	@Schema(title = "총 서비스 수, ADMIN은 전체 서비스")
	private Integer serviceTotalCount;

	@Schema(title = "account 정보", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	private AccountVO account;

	@Schema(title = "계정 요금 미납 여부")
	private boolean accountUnpaid = false;

	@Schema(title = "클러스터 shell 권한 발급 목록")
	private List<UserClusterRoleIssueVO> shellRoles;

	@Schema(title = "클러스터 kubeconfig 권한 발급 목록")
	private List<UserClusterRoleIssueVO> kubeconfigRoles;

	@Schema(title = "사용자가 사용 가능한 cluster", example = "[]")
	private List<ClusterDetailNamespaceVO> clusters;

	@Schema(title = "사용자 relation")
	private List<ServiceRelationVO> userRelations;

	@Schema(title = "신청서 로그인", accessMode = Schema.AccessMode.READ_ONLY)
	private boolean applicationLogin = false;

	@Schema(title = "신청서 상태", accessMode = Schema.AccessMode.READ_ONLY)
	private String applicationStatus = null;

	@Schema(title = "License valid")
	private CocktailLicenseValidVO licenseValid;


	@JsonIgnore
	private Date passwordExpirationBeginTime;
	@JsonIgnore
	private Date passwordExpirationEndTime;

	@JsonIgnore
	private Integer passwordInterval;

	@JsonIgnore
	private String issueTypeShell;

	@JsonIgnore
	private String issueTypeKubeconfig;

	@JsonIgnore
	private Integer accountSeq;

}
