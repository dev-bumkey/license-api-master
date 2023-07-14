package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasStateVO;

@Getter
@Setter
public class ComponentFilterVO extends HasStateVO {

	Integer servicemapSeq;

	Integer exceptComponentSeq;

	boolean includeDeleted = false;

	Integer componentSeq;

	Integer serviceSeq;

	Integer clusterSeq;

	String namespaceName;

	String componentName;
}
