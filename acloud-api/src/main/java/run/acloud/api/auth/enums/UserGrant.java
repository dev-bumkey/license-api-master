package run.acloud.api.auth.enums;

import lombok.Getter;
import run.acloud.api.auth.Util.ServiceUtil;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 24.
 */
public enum UserGrant implements EnumCode {
    MANAGER(EnumSet.of(ServiceMode.PRD, ServiceMode.SECURITY_ONLINE, ServiceMode.HYDCARD)), // 워크스페이스 관리자 권한
    USER(EnumSet.of(ServiceMode.PRD, ServiceMode.SECURITY_ONLINE, ServiceMode.HYDCARD)),    // 워크스페이스 사용자 권한
    DEVOPS(EnumSet.of(ServiceMode.PRD, ServiceMode.SECURITY_ONLINE, ServiceMode.HYDCARD)),  // 파이프라인 배포 권한(파이프라인만 접근 가능)
    DEV(EnumSet.of(ServiceMode.PRD, ServiceMode.SECURITY_ONLINE, ServiceMode.HYDCARD)),     // 파이프라인 빌드 권한(파이프라인만 접근 가능)
    VIEWER(EnumSet.of(ServiceMode.PRD, ServiceMode.SECURITY_ONLINE, ServiceMode.HYDCARD))   // 워크스페이스 뷰어 권한(CUD 불가능)
    ;

    @Getter
    private EnumSet<ServiceMode> supportServiceMode;

    UserGrant(EnumSet<ServiceMode> supportServiceMode) {
        this.supportServiceMode = supportServiceMode;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public boolean isManager() {
        return UserGrant.MANAGER == this;
    }

    public boolean isUser() {
        return UserGrant.USER == this;
    }

    public boolean isViewer() {
        return UserGrant.VIEWER == this;
    }

    public boolean isPipeline() {
        return EnumSet.of(UserGrant.DEVOPS, UserGrant.DEV).contains(this);
    }

    public static List<String> getList() {
        return Arrays.stream(UserGrant.values()).filter(ug -> (ug.getSupportServiceMode().contains(ServiceUtil.getServiceMode()))).map(UserGrant::getCode).collect(Collectors.toList());
    }
}
