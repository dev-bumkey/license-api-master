package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.ServiceRegistryType;
import run.acloud.commons.enums.CRUDCommand;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 7.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스 레지스트리 모델")
public class ServiceRegistryVO extends BaseVO {

    @Schema(description = "서비스 번호")
    private Integer serviceSeq;

    @Schema(description = "레지스트리 프로젝트 ID")
    private Integer projectId;

    @Schema(description = "레지스트리 프로젝트 유형")
    private ServiceRegistryType projectType;

    @Schema(description = "레지스트리 프로젝트 이름")
    private String projectName;

    /** R4.4.0 : 클러스터 테넌시 : 레지스트리 통합관리 기능을 위해 필드 추가 : 2020.06.18 **/
    @Schema(description = "레지스트리 프로젝트 설명")
    private String description;

    @Schema(description = "레지스트리 프로젝트 생성일")
    private String created;

    @Schema(description = "레지스트리 프로젝트를 사용중인 빌드(이미지) 갯수")
    private Integer buildCount;

    @Schema(description = "레지스트리를 사용중인 워크스페이스(서비스) 목록")
    private List<ServiceCountVO> services;

    // 데이터 수정시 생성/수정/삭제 등의 변경 유형 판단을 위해 추가
    // C(Create), U(Update), D(Delete), N(No Change : UI에서 No Change일때 값을 넘기지 않고 있음.. R4.1.0 => null도 No Change로 간주..)
    private CRUDCommand modifyType;
}
