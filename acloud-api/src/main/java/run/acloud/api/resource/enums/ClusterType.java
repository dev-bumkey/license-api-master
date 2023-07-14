package run.acloud.api.resource.enums;

import run.acloud.api.cserver.enums.StateCode;
import run.acloud.commons.enums.EnumCode;

import java.util.EnumSet;

/**
 * @author vancheju on 17. 4. 12.
 */
public enum ClusterType implements EnumCode {
	COCKTAIL, CUBE;

	private final static EnumSet<ClusterType> CAN_DELETE_SERVER_SET = EnumSet.of(CUBE);
//	private final static EnumSet<ClusterType> CAN_START_SERVER_SET = EnumSet.of();
//	private final static EnumSet<ClusterType> CAN_STOP_SERVER_SET = EnumSet.of();
	private final static EnumSet<ClusterType> CAN_SCALE_INOUT_SERVER_SET = EnumSet.of(CUBE);
//	private final static EnumSet<ClusterType> CAN_SCALE_UPDOWN_SERVER_SET = EnumSet.of();
	private final static EnumSet<ClusterType> CAN_DEPLOY_SERVER_SET = EnumSet.of(CUBE);

    public static ClusterType codeOf(String code) {
        return ClusterType.valueOf(code);
    }

    @Override
    public String getCode() {
        return this.name();
    }
    
    public boolean canDoServerAction(StateCode stateCode) {
		boolean result = false;
		
		switch (stateCode) {
		case DELETING:
			result = CAN_DELETE_SERVER_SET.contains(this);
			break;
//		case STARTING:
//			result = CAN_START_SERVER_SET.contains(this);
//			break;
//		case STOPPING:
//			result = CAN_STOP_SERVER_SET.contains(this);
//			break;
//		case IN_OUT_SCALING:
//			result = CAN_SCALE_INOUT_SERVER_SET.contains(this);
//			break;
//		case UP_DOWN_SCALING:
//			result = CAN_SCALE_UPDOWN_SERVER_SET.contains(this);
//			break;
		case DEPLOYING:
			result = CAN_DEPLOY_SERVER_SET.contains(this);
			break;

		default:
			break;
		}
		
		return result;
	}
}
