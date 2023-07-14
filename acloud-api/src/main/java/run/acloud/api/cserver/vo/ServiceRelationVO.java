package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.auth.enums.UserGrant;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "서비스 연결 모델")
public class ServiceRelationVO implements Serializable {

    private static final long serialVersionUID = 7117967418615940226L;

    @Schema(title = "어카운트 번호 (시스템)")
    private Integer accountSeq;

    @Schema(title = "어카운트 이름 (시스템)")
    private String accountName;

    @Schema(title = "서비스 번호 (워크스페이스)")
    private Integer serviceSeq;

    @Schema(title = "서비스 이름 (워크스페이스)")
    private String serviceName;

    @Schema(title = "클러스터 번호")
    private Integer clusterSeq;

    @Schema(title = "클러스터 이름")
    private String clusterName;

    @Schema(title = "서비스맵그룹 순번")
    private Integer servicemapGroupSeq;

    @Schema(title = "서비스맵그룹 이름")
    private String servicemapGroupName;

    @Schema(title = "서비스맵 순번")
    private Integer servicemapSeq;

    @Schema(title = "서비스맵 이름")
    private String servicemapName;

    @Schema(title = "Namespace")
    private String namespace;

    @Schema(title = "워크로드그룹 번호")
    private Integer workloadGroupSeq;

    @Schema(title = "워크로드그룹 이름")
    private String workloadGroupName;

    @Schema(title = "컴포넌트 번호 (워크로드)")
    private Integer componentSeq;

    @Schema(title = "컴포넌트 이름 (워크로드)")
    private String componentName;

    @Schema(title = "컴포넌트 타입 (워크로드)")
    private String componentType;

    @Schema(title = "사용자 번호")
    private Integer userSeq;

    @Schema(title = "사용자 워크스페이스 권한")
    private UserGrant userGrant;


}