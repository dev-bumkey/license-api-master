package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum CertCertificateGUIItem implements EnumCode {
	  isCA
	, commonName
	, dnsNames
	, uris
	, ipAddresses
	, emailAddresses
	, secretName
	, issuerRef
	, duration
	, renewBefore
	, revisionHistoryLimit
	, usages
	;

	public static CertCertificateGUIItem codeOf(String name) {
		return CertCertificateGUIItem.valueOf(name);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
