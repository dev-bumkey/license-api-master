package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.EnumSet;

/**
 * Created by dy79 on 17. 4. 12.
 */
public enum CubeType implements EnumCode {
    ECS(EnumSet.of(AuthType.CERT)),
    EKS(EnumSet.of(AuthType.TOKEN)),
    ACS(EnumSet.of(AuthType.CERT)),
    AKS(EnumSet.of(AuthType.TOKEN)),
    GKE(EnumSet.of(AuthType.TOKEN)),
    TKE(EnumSet.of(AuthType.CERT)),
    OCP(EnumSet.of(AuthType.CERT)), // RedHat OpenShift
    OKE(EnumSet.of(AuthType.CERT)), // RedHat OpenShift
    NCPKS(EnumSet.of(AuthType.TOKEN)), // Naver Cloud
    MANAGED(EnumSet.of(AuthType.CERT)),
    PROVIDER(EnumSet.of(AuthType.CERT));

	private EnumSet<AuthType> supportAuthTypes;
	
	CubeType(EnumSet<AuthType> supportAuthTypes) {
		this.supportAuthTypes = supportAuthTypes;
	}
	
	public EnumSet<AuthType> getSupportAuthTypes() {
		return supportAuthTypes;
	}

	public boolean isKaas() {
	    return !EnumSet.of(MANAGED, PROVIDER).contains(this);
    }
	
	public boolean isOpenShift() {
	    return EnumSet.of(OCP, OKE).contains(this);
    }

    /**
     * kubeconfig 계정 기능 지원 가능하지 않는 여부
     *
     * @return
     */
    public boolean isKubeconfigNotSupported() {
        return EnumSet.of(NCPKS).contains(this);
    }

    /**
     * Shell 계정 기능 지원 가능하지 않는 여부
     *
     * @return
     */
    public boolean isShellNotSupported() {
        return EnumSet.of(NCPKS).contains(this);
    }

    public static CubeType codeOf(String code) {
        return CubeType.valueOf(code);
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
