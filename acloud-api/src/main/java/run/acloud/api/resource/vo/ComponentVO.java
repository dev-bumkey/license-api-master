package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.commons.vo.HasStateVO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(title = "컴포넌트 모델")
public class ComponentVO extends HasStateVO {

	@Schema(title = "컴포넌트 순번")
	private Integer componentSeq;

	@Schema(title = "워크로드 그룹 순번")
	private Integer workloadGroupSeq;

	@Schema(title = "컴포넌트명")
	@NotNull
	@Size(min = 1, max = 50)
	private String componentName;

	@Schema(title = "컴포넌트유형")
	private String componentType;

	@Schema(title = "컴포넌트설명")
	private String description;

	@Schema(title = "워크로드 유형")
	private String workloadType;

	@Schema(title = "워크로드 버전")
	private String workloadVersion;

	@Schema(title = "워크로드 설정")
	private String workloadManifest;

	@Schema(title = "오류 메세지")
	private String errorMessage;

	@Deprecated
	private int sortOrder;

    private Boolean dryRunYn = false;

    private String stateDetail;


	/** 3.5.0 : from ServerVO **/
	private int computeTotal;
	private int activeCount;
	private List<ServiceSpecGuiVO> services;
	private List<ServerUrlVO> serverUrls;

	/** 3.5.0 : Add  **/
	@Schema(title = "containers", example = "[]")
	private List<ContainerVO> containers; // 워크로드에서 사용중인 이미지 정보를 담기 위함..
	private String qos;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private OffsetDateTime creationTimestamp; // 실제 K8s에서 조회한 creationTimestamp 시간을 담기 위함..

	@Schema(title = "pods", example = "[]")
	private List<K8sPodVO> pods; // 워크로드의 Pod 목록
	private Boolean isK8sResourceExist;

	@Schema(title = "클러스터순번")
	private Integer clusterSeq;
	@Schema(title = "클러스터명")
	private String clusterName;
	@Schema(title = "namespace 명")
	private String namespaceName;

	@Schema(title = "서비스맵 정보")
	private ServicemapSummaryVO servicemapInfo;

//	@Schema(title = "워크스페이스순번")
//	private Integer serviceSeq;
//	@Schema(title = "워크스페이스명")
//	private String serviceName;
//	@Schema(title = "서비스맵 순번")
//	private Integer servicemapSeq;
//	@Schema(title = "서비스맵 명")
//	private String servicemapName;
//	@Schema(title = "워크로드 그룹 명")
//	private String workloadGroupName;
//	@Schema(title = "워크로드 그룹 정렬 순서")
//	private Integer groupSortOrder;

	/** 4.0.0 : Add **/
	@JsonIgnore
	private Map<String, String> templateLabels; // Service selector와 label match 하기 위한 라벨값
	/** 4.0.1 : Add **/
	@JsonIgnore
	private Map<String, String> annotations; // Annotation에서 GroupSeq label을 조회
}
