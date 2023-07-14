package run.acloud.api.cserver.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.cserver.enums.WorkloadType;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 19.
 */
@Getter
@Setter
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "deployType",
		visible = true
)
@Schema(name = "ServerYamlVO",
		title = "ServerYamlVO",
		description = "서버 배포 YAML 모델",
		allOf = {ServerIntegrateVO.class}
)
public class ServerYamlVO extends ServerIntegrateVO {

	@Schema(title = "지원하는 워크로드 유형"
			, allowableValues = {WorkloadType.Names.SINGLE_SERVER, WorkloadType.Names.REPLICA_SERVER, WorkloadType.Names.STATEFUL_SET_SERVER, WorkloadType.Names.DAEMON_SET_SERVER, WorkloadType.Names.JOB_SERVER, WorkloadType.Names.CRON_JOB_SERVER}
	)
	@NotBlank
	private String workloadType;

	@Schema(title = "지원하는 워크로드 버전", allowableValues = {"V1"})
	@NotBlank
	private String workloadVersion;

	@Schema(title = "워크로드명")
	@NotBlank
	private String workloadName;

	@Schema(title = "워크로드 설명")
	private String description;

	@Schema(title = "네임스페이스명")
	@NotBlank
	private String namespaceName;

	@Schema(title = "cluster 번호")
	@NotNull
	private Integer clusterSeq;

	@Schema(title = "워크로드 상태")
	private String stateCode;

	@Schema(title = "배포 yaml 문자열")
	@NotBlank
	private String yaml;

	/** 4.1.0 적용시 추가 : workloadGroupSeq, workloadGroupName **/
	// internal use only
	@Schema(title = "Workload Group Sequence")
	@JsonIgnore
	private Integer workloadGroupSeq; // 신규 워크로드를 Yaml로 배포시 Group 정보 설정을 위해 사용.. (현재 Template에서 사용)

	// internal use only
	@Schema(title = "Workload Group명")
	@JsonIgnore
	private String workloadGroupName; // 신규 워크로드를 Yaml로 배포시 Group 정보 설정을 위해 사용.. (현재 Template에서 사용)

}
