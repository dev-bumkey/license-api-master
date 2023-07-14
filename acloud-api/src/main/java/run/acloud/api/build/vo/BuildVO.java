package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.AutoTagSeqType;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "빌드 모델")
public class BuildVO extends HasUseYnVO {

    private Integer buildSeq;
    private Integer buildNo;
    private String buildName;
    private String buildDesc;
    private Integer accountSeq;
    private Integer registryProjectId;
    private String registryName;
    private String imageName;

    // 자동태그 정보
    private String autotagUseYn;
    private String autotagPrefix;
    private AutoTagSeqType autotagSeqType;

    private String tagName;

    // 외부 레지스트리 순번
    private Integer externalRegistrySeq;

    // 빌드 실행 서버 & TLS 정보
    private String buildServerHost;
    private String buildServerTlsVerify;
    private String buildServerCacrt;
    private String buildServerClientCert;
    private String buildServerClientKey;

    private List<BuildStepVO> buildSteps;

    // 빌드리스트를 위한 변수
    private String hostUrl; // 이미지 host
    private Integer buildCount; // 총빌드수
    private Integer runningCount; // 실행중인 빌드수
    private Integer doneCount;  // 빌드완료 건수
    private Integer errorCount; // 빌드에러 건수
    private Integer pipelineCount; // 파이프라인 수
    private Integer tagCount; // 태그수

    private Integer buildTimeAvg;   // 평균 빌드시간, 빌드 성공인것만 계산
    private Long imgSizeAvg;     // 평균 이미지 크기, 빌드 성공인것만 계산

    private Integer latestBuildTime;   // 최근 빌드시간, 빌드 성공인것만
    private Long latestImgSize;     // 최근 이미지 크기, 빌드 성공인것만

}
