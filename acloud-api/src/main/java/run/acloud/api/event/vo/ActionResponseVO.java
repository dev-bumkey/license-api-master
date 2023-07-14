package run.acloud.api.event.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionResponseVO<T> {
	
	private T result;
}
