package run.acloud.api.external.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeycloakUserVO {

	private Integer userSeq;

	private String userId;

	private String userName;

	private String userLanguage;

	private String userTimezone;

	private String lastLogin;

	private String userRole;

	private String userJob;

	private String phoneNumber;

	private String kakaoId;

	private String email;

	private String description;

	private String password;

	private String hashSalt;

	private String accountCode;
}
