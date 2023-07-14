package run.acloud.api.pipelineflow.vo;

import lombok.Data;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.enums.StepState;
import run.acloud.api.build.enums.StepType;
import run.acloud.api.build.vo.BuildStepRunVO;

import java.util.List;

@Data
public class PipelineCommandVO {

	// 요청시 필요한 필드들
	private String fromType; // 요청유형, PIPELINE|BUILD|PL
	private String pipelineNamespace;
	private Integer accountSeq;
	private Integer buildAbleCount;
	private Integer buildSeq;
	private Integer buildRunSeq;
	private Integer pipelineSeq;
	private RunState runState;
	private String callback="";

	// build server & build server TLS infos
	private String buildServerHost;
	private boolean buildServerTlsVerify = false;
	private String buildServerCacrt;
	private String buildServerClientCert;
	private String buildServerClientKey;

	// registry id/pw for to push image
	// certificate 필드 추가, coolingi, 2021.11.25
	private String registryUserId;
	private String registryUserPassword;
	private String privateCertificate;

	// log server 정보
	private String logClusterId;
	private String logUser;
	private String logPass;
	private String logServerUrl;

	private List<BuildStepRunVO> buildList;

	// Deploy시 필요한 정보들
	private Integer clusterSeq;
	private String clusterUrl; //클러스터 조회 URL
	private Integer pipelineRunSeq; // deploy pipeline run seq
	private String deployContent;

	// 응답시 필요한 필드들
	private Integer buildStepRunSeq;
	private StepType stepType;
	private StepState stepState;
	private String imageUrl;
	private Integer imageSize;

	// PL 정보들
	private Integer plSeq;
	private Integer plRunSeq;
	private Integer plRunBuildSeq;

}
