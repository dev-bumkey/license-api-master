package run.acloud.api.event.enums;

public enum EventTypeEnum {
    URL("URL"),
    MESSAGE("MESSAGE");

	public static class Names{
		public static final String URL = "URL";
		public static final String MESSAGE = "MESSAGE";
	}

	EventTypeEnum(String type) {
		this.type = type;
	}

	private String type;

	public String getType(){
		return this.type;
	}
}
