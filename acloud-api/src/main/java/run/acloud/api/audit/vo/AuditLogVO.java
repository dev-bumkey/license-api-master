package run.acloud.api.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "수정 이력 모델")
public class AuditLogVO implements Serializable {

    private static final long serialVersionUID = 1230204406352043463L;

    @Schema(title ="수정이력번호")
    private Integer auditLogSeq;

    @Schema(title ="수정이력코드")
    private String auditLogCode;

    @Schema(title ="HTTP 메소드")
    private String httpMethod;

    @Schema(title ="Request URI")
    private String uri;

    @Schema(title ="컨트롤러 이름")
    private String controllerName;

    @Schema(title ="메소드 이름")
    private String methodName;

    @Schema(title ="어카운트번호 (시스템)")
    private Integer accountSeq;

    @Schema(title ="서비스번호 (워크스페이스)")
    private Integer serviceSeq;

    @Schema(title ="서비스맵그룹번호 (서비스맵 그룹)")
    private Integer servicemapGroupSeq;

    @Schema(title ="서비스맵번호 (서비스맵)")
    private Integer servicemapSeq;

    @Schema(title ="클러스터 번호")
    private Integer clusterSeq;

    @Schema(title ="워크로드그룹 번호")
    private Integer workloadGroupSeq;

    @Schema(title ="사용자 번호")
    private Integer userSeq;

    @Schema(title ="사용자 계정")
    private String userId;

    @Schema(title ="결과 코드")
    private String resultCode;

    @Schema(title ="처리 시간")
    private double duration;

    @Schema(title ="요청 데이터")
    private String requestData;

    @Schema(title ="응답 데이터")
    private String responseData;

    @Schema(title ="리소스별 데이터")
    private String resourceData;

    @Schema(title ="리소스명")
    private String resourceName;

    @Schema(title ="생성일시")
    private String created;

    @Schema(title ="수정이력 로그 그룹")
    private String auditLogGroup; // WORKLOAD, SYSTEM, INGRESS, SERVICE 등등.

    @Schema(title ="수정이력 퍼미션")
    private String auditLogPermission; // DEVOPS, SYSTEM, ADMIN

    @Schema(title ="어카운트이름 (시스템)")
    private String accountName;

    @Schema(title ="서비스이름 (워크스페이스)")
    private String serviceName;

    @Schema(title ="서비스맵그룹이름 (서비스맵 그룹)")
    private String servicemapGroupName;

    @Schema(title ="서비스맵이름 (서비스맵)")
    private String servicemapName;

    @Schema(title ="클러스터 이름")
    private String clusterName;

    @Schema(title ="워크로드그룹 이름")
    private String workloadGroupName;

}