package run.acloud.api.code.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

/**
 * Created by dy79@acornsoft.io on 2017. 3. 13.
 */
@Getter
@Setter
public class CodeVO extends BaseVO {
	String groupId;
	
	String code;
	
	String value;
	
	String description;

	String useYn;
}