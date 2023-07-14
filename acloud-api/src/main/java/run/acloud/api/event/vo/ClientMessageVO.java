package run.acloud.api.event.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientMessageVO<T> {
	
	private String destination;
	
	private T payload;
}
