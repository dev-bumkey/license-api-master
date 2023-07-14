package run.acloud.api.event.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionMessageVO<T> {
	
	private String type;
	
	private ActionResponseVO<T> response;
	
	private String created;
}
