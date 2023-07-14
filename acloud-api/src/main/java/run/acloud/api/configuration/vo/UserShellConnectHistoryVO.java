package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "사용자 쉘 접속 이력 모델")
public class UserShellConnectHistoryVO extends BaseVO {
	
	@Schema(name = "접속 번호")
	@JsonIgnore
	private Integer connectSeq;

	@Schema(name = "사용자 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer userSeq;

	@Schema(name = "사용자 아이디")
	private String userId;

	@Schema(name = "사용자 이름")
	private String userName;

	@Schema(name = "클러스터 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer clusterSeq;

	@Schema(name = "발급권한")
	private String issueRole;

	@Schema(name = "접속한 serviceAccount(shell) 명")
	private String connectAccountName;

	@Schema(name = "클러스터 ID")
	private String clusterId;

	@Schema(name = "클러스터 명")
	private String clusterName;

	@Schema(name = "접속상태", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"OPEN","CLOSE","FAIL"})
	private String connectState;

	@Schema(name = "접속일시")
	private String connectDatetime;

	@Schema(name = "접속상태", description = "로직상 이슈가 있을 시 정보 입력")
	private String connectMessage;

	@Schema(name = "발급 사용자 번호", description = "조회용도")
	private Integer issueUserSeq;

	@Schema(name = "발급 사용자 아이디", description = "조회용도")
	private String issueUserId;

	@Schema(name = "발급 사용자 이름", description = "조회용도")
	private String issueUserName;

	@Schema(name = "이력 번호", description = "조회용도")
	@JsonIgnore
	private Integer historySeq;

	@Schema(name = "이력상태", description = "조회용도", allowableValues = {"GRANT","CHANGE","REVOKE","EXPIRED"})
	private String historyState;
}
