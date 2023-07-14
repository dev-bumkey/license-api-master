package run.acloud.commons.vo;

import org.apache.commons.lang3.StringUtils;
import run.acloud.api.cserver.enums.DeployAction;
import run.acloud.api.cserver.enums.StateCode;

import java.util.List;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 18.
 */

public class HasStateVO extends HasUseYnVO {
	
	private String stateCode;

	private List<DeployAction> possibleActions;

	public String getStateCode() {
	    return this.stateCode;
    }

//    public void setStateCode(String code) {
//	    stateCode = code;
//	    if (StringUtils.isNotBlank(code)) {
//	        if (code.equals("AVAILABLE")) {
//	            stateCode = "RUNNING";
//            } else if (code.equals("NOT_AVAILABLE")) {
//	            stateCode = "INITIALIZING";
//            }
//        }
//    }

	public List<DeployAction> getPossibleActions() {
		return this.possibleActions;
	}

    public void setStateCode(String state) {
		this.stateCode = state;
		if (StringUtils.isNotBlank(this.stateCode)) {
			StateCode stateCode = StateCode.valueOf(this.stateCode);
			this.possibleActions = stateCode.possibleActions();
		}

    }
}
