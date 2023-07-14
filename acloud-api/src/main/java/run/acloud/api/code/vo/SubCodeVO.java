package run.acloud.api.code.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

/**
 * Created by dy79@acornsoft.io on 2017. 3. 13.
 */
@Getter
@Setter
public class SubCodeVO extends BaseVO {
	String groupId;
	
	String subGroupId;
	
	String subCodeGroupId;
	
	String code;
	
	String value;
	
	String description;
}