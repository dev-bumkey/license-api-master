package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "사용자 설정 다운로드 이력 모델")
public class UserConfigDownloadHistoryVO extends BaseVO {
	
	@Schema(name = "다운로드 번호")
	@JsonIgnore
	private Integer downloadSeq;

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

	@Schema(name = "다운로드한 userAccount(kubeconfig) 명")
	private String downloadAccountName;

	@Schema(name = "클러스터 ID")
	private String clusterId;

	@Schema(name = "클러스터 명")
	private String clusterName;

	@Schema(name = "다운로드상태", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"SUCCESS","FAIL"})
	private String downloadState;

	@Schema(name = "다운로드일시")
	private String downloadDatetime;

	@Schema(name = "다운로드상태", description = "로직상 이슈가 있을 시 정보 입력")
	private String downloadMessage;

	@Schema(name = "만료일시", description = "yyyy-MM-dd(조회용도)")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private String expirationDatetime;

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
