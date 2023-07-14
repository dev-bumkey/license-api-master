package run.acloud.api.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;


@Schema(name="AuthModel", description="사용자 인증 정보")
@Getter
public class AuthVO //implements UserDetails
{

	private static final long serialVersionUID = -2451103088660657044L;

    @Schema(name = "User Id", description = "UserDetails method. 사용자 ID로 email 주소 형식",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "admin@acornsoft.io")
    @Setter
    private String username;

    @Schema(name = "User Password", description = "UserDetails method. 사용자 암호, 생성 및 로그인에 사용하며 조회 시에는 반환되지 않는다.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Setter
    private String password;

    @Setter
    private String hashSalt;

    @Schema(name = "Account ID", description = "DEVOPS 유형으로 로그인시 account id 필수")
    @Setter
    private String accountId;

    @Schema(name = "certified", description = "2 factor 인증시 사용( 인증 성공 : S, 실패 : F ), 기본값은 null")
    @Setter
    private String certified = null;

    @Schema(description = "UserDetails method", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean isAccountNonExpired = true;

    @Schema(description = "UserDetails method", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean isAccountNonLocked = true;

    @Schema(description = "UserDetails method", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean isCredentialsNonExpired = true;

    @Schema(description = "계정의 활성 여부", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean isEnabled;
    
//    @Schema(description = "UserDetails method. 사용자 권한. 로그인 할 때는 이 필드는 없어야 한다", accessMode = Schema.AccessMode.READ_ONLY)
//    @Setter
//    private Collection<? extends GrantedAuthority> authorities;

    @Schema(description = "사용자 권한", allowableValues = {"ADMIN","SYSTEM","DEVOPS"},
    		requiredMode = Schema.RequiredMode.REQUIRED, example="ADMIN")
    @Setter
    private String role;
}
