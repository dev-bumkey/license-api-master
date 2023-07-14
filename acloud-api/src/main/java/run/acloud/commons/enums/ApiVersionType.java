package run.acloud.commons.enums;

public enum ApiVersionType {
    BOTH("BOTH"),
    V1("V1"),
    V2("V2"),
	V3("V3");

	ApiVersionType(String type) {
		this.type = type;
	}

	private String type;

	public String getType(){
		return this.type;
	}

}
