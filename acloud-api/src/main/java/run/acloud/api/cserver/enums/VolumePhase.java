package run.acloud.api.cserver.enums;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 30.
 */
public enum VolumePhase {
    UNKNOWN,
    AVAILABLE, // Dynamic은 항상 이 상태.
    BOUND,
    RELEASED,
    FAILED,
    DELETED; // K8S 상태값이 아닌 Cocktail 관리용 상태.

    public static VolumePhase ofCode(String value) {
        value = value.toLowerCase();
        switch (value) {
            case "available":
                return AVAILABLE;
            case "bound":
                return BOUND;
            case "released":
                return RELEASED;
            case "fail":
                return FAILED;
            case "deleted":
                return DELETED;
            default:
                return UNKNOWN;
        }
    }
}
