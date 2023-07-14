package run.acloud.commons.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.api.cserver.enums.WorkloadType;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiVerKindType;
import run.acloud.commons.enums.ApiVersionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 31.
 */
@Getter
@Setter
public class ExecutingContextVO {
    public ExecutingContextVO() {
        this.params = new HashMap<>();
        this.workloadVersionSetMap = new HashMap<>();
    }

    private ApiVersionType apiVersionType;

    private String pipelineYn = "N";

    private String catalogYn = "N";

    private Integer userSeq;
    private String userRole;
    private String userTimezone;
    private Integer userAccountSeq;
    private Integer userServiceSeq;
    private String userGrant;
    private AccountVO userAccount;
    private List<ServiceRelationVO> userRelations;

    private Integer componentSeq;

    private Map<String, Object> params;

    private WorkloadType workloadType;

    private Map<K8sApiKindType, K8sApiVerKindType> workloadVersionSetMap;

    private ResultVO result;

    //파이프라인 시퀀스 추가, 파이프라인 실행시에 셋팅, 2021.02.26
    private Integer plSeq;
    private Integer plRunSeq;
    private Integer plRunBuildSeq;
}
