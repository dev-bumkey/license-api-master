package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author dy79@acornsoft.io on 2017. 2. 1.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "워크로드그룹 등록 모델")
public class WorkloadGroupAddVO extends WorkloadGroupVO {

}
