package run.acloud.api.cserver.enums;

import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 9.
 */
public enum PortType  implements EnumCode {
    INTERNAL("Internal"), // 서비스 노출 안함
    EXTERNAL_NAME(KubeConstants.SPEC_TYPE_VALUE_EXTERNAL_NAME),
    CLUSTER_IP(KubeConstants.SPEC_TYPE_VALUE_CLUSTER_IP),
    NODE_PORT(KubeConstants.SPEC_TYPE_VALUE_NODE_PORT),
    LOADBALANCER(KubeConstants.SPEC_TYPE_VALUE_LOADBALANCER),
    HEADLESS(KubeConstants.SPEC_TYPE_VALUE_CLUSTER_IP)
//    EXTERNAL_NAME,
//    INGRESS
    ;

    private String type;

    PortType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }


    @Override
    public String getCode() {
        return this.name();
    }

    public static PortType findPortType(String findType){
        return Arrays.stream(PortType.values()).filter(vk -> (vk.getType().equals(findType)))
            .findFirst()
            .orElseGet(() ->null);
    }

    public static PortType findPortName(String findName){
        return Arrays.stream(PortType.values()).filter(vk -> (vk.getCode().equals(findName)))
            .findFirst()
            .orElseGet(() ->null);
    }
}
