package run.acloud.api.auth.enums;

import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 24.
 */
public enum UserRole implements EnumCode {
    ADMIN("Administrator", "System Administrator", "Y"),
    SYSTEM("System Manager", "System Manager", "Y"),
    SYSUSER("System User", "System User", "Y"),
    APPUSER("Application User", "Application User", "Y"),
    SYSDEMO("System Demo", "System Demo", "Y"), // 모든 메뉴 viewer(시스템관리 포함)
    DEVOPS("DevOps", "DevOps", "Y");

    @Getter
    private String value;

    @Getter
    private String description;

    @Getter
    private String useYn;

    UserRole(
            String value
            , String description
            , String useYn
    ){
        this.value = value;
        this.description = description;
        this.useYn = useYn;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public boolean isDevops() {
        return UserRole.DEVOPS == this;
    }

    public boolean isDevopsNSysuser() {
        return EnumSet.of(SYSUSER, SYSDEMO, DEVOPS).contains(this);
    }

    public boolean isSysuser() {
        return UserRole.SYSUSER == this;
    }

    public boolean isSystem() {
        return UserRole.SYSTEM == this;
    }

    public boolean isAccountRole() {
        return EnumSet.of(SYSUSER, SYSTEM, DEVOPS, SYSDEMO).contains(this);
    }

    public boolean isAdmin() {
        return UserRole.ADMIN == this;
    }

    public boolean isAdminNSystem() {
        return EnumSet.of(ADMIN, SYSTEM).contains(this);
    }

    public boolean isSysdemo() {
        return UserRole.SYSDEMO == this;
    }

    public boolean isUserOfSystem() {
        return EnumSet.of(SYSUSER, SYSTEM, SYSDEMO).contains(this);
    }

    public static CodeVO getCodeVO(String code) {
        if (StringUtils.isNotBlank(code)) {
            Optional<CodeVO> codeVOOptional = Arrays.asList(UserRole.values()).stream()
                    .filter(ur -> (BooleanUtils.toBoolean(ur.getUseYn()) && StringUtils.equalsIgnoreCase(code, ur.getCode())))
                    .map(vp -> {
                        CodeVO codeVO = new CodeVO();
                        codeVO.setGroupId("USER_ROLE");
                        codeVO.setCode(vp.getCode());
                        codeVO.setValue(vp.getValue());
                        codeVO.setDescription(vp.getDescription());
                        return codeVO;
                    })
                    .findFirst();

            if (codeVOOptional.isPresent()) {
                return codeVOOptional.get();
            }
        }

        return null;
    }

    public static List<CodeVO> getCodeVOs() {
        return Arrays.asList(UserRole.values()).stream()
                    .filter(ur -> (BooleanUtils.toBoolean(ur.getUseYn())))
                    .map(vp -> {
                        CodeVO codeVO = new CodeVO();
                        codeVO.setGroupId("USER_ROLE");
                        codeVO.setCode(vp.getCode());
                        codeVO.setValue(vp.getValue());
                        codeVO.setDescription(vp.getDescription());
                        return codeVO;
                    })
                    .collect(Collectors.toList());
    }
}
