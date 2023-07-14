package run.acloud.api.build.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.build.enums.AutoTagSeqType;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(name = "BuildAddVO", title= "BuildAddVO", description = "빌드 생성 모델")
public class BuildAddVO extends HasUseYnVO {
    @Schema(title = "편집 종류", allowableValues = {"N-신규","U-편집"}, description = "신규 : N, 편집 : U", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String editType;

    @Schema(title = "빌드 이름", description = "편집 종류가 신규시 필수")
    private String buildName;

    @Schema(title = "빌드 설명", description = "빌드에 대한 설명으로 필수값 아님", required = false)
    private String buildDesc;

    @Schema(title = "빌드 번호", description = "편집 종류가 편집시 필수")
    private Integer buildSeq;

    @Schema(title = "계정 번호", description = "시스템의 계정번호")
    private Integer accountSeq;

    @Schema(title = "레지스트리 프로젝트 아이디", description = "빌드 이미지 저장할 레지스트리 프로젝트 ID")
    private Integer registryProjectId = 0;

    @Schema(title = "레지스트리 명", description = "빌드 이미지 저장할 레지스트리명")
    private String registryName;

    @Schema(title = "이미지 명", description = "빌드될 이미지명")
    private String imageName;

    // 자동태그 정보
    @Schema(title = "자동태그 사용여부", description = "사용: Y, 사용안함: N")
    private String autotagUseYn;   // autotag_use_yn

    @Schema(title = "자동태그 prefix", description = "자동 태그 prefix")
    private String autotagPrefix;  // autotag_prefix

    @Schema(title = "자동태그 순번 유형", description = "자동태그 순번 유형, 'DATETIME' or 'SEQUENCE' ")
    private AutoTagSeqType autotagSeqType; // autotag_seq_type


    @Schema(title = "이미지 tag명", description = "빌드될 이미지 tag명")
    private String tagName;

    @Schema(title = "이미지 tag명", description = "빌드될 이미지 tag명")
    private Integer tags;

    @Schema(title = "레지스트리 번호", description = "빌드 이미지 저장할 레지스트리 번호")
    private Integer externalRegistrySeq;

    @Schema(title = "빌드 실행할 서버의 Host 정보", description = "실제 빌드 스텝을 실행하고 이미지 생성할 서버 Host 정보")
    private String buildServerHost;

    @Schema(title = "빌드 실행 서버의 tls 사용여부", description = "실제 빌드 실행할 서버의 tls 사용여부, Y/N")
    private String buildServerTlsVerify;

    @Schema(title = "빌드 실행 서버의 tls CA", description = "실제 빌드 실행할 서버의 tls 연동 위한 CA 값")
    private String buildServerCacrt;

    @Schema(title = "빌드 실행 서버의 Client Cert", description = "실제 빌드 실행할 서버의 tls 연동 위한 Client Cert")
    private String buildServerClientCert;

    @Schema(title = "빌드 실행 서버의 Client Key", description = "실제 빌드 실행할 서버의 tls 연동 위한 Client Key")
    private String buildServerClientKey;

    @Schema(title = "단계별 빌드 설정 정보 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Size(min = 1)
    @JsonAlias("buildStepRuns")
    // @SerializedName(value="buildSteps", alternate = {"buildStepRuns"})
    private List<BuildStepVO> buildSteps;
}
