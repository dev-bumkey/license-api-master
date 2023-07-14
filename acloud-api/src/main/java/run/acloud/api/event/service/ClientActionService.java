package run.acloud.api.event.service;

/**
 * @author dy79@acornsoft.io Created on 2017. 5. 1.
 */
//@Slf4j
//@Service
public class ClientActionService {

//	@Autowired
//	private SimpMessageSendingOperations messaging;
//
//	private static final String DEFAULT_DATE_FORMAT1 = "yyyyMMddhhmmsss";
//
//	private static String getUtcTime() {
//		DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT1);
//		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//		Date date = new Date();
//
//		return dateFormat.format(date);
//	}
//
//	public <T> void  sendMessage(IClientAction<T> action) throws Exception {
//		sendMessage(action.getDestination(), action.getType(), action.getResult());
//	}
//
//	public <T> void sendMessage(String destination, String type, T result) {
//	    log.debug(">>>> sendMessage 1 - destination: {}, type: {}", destination, type);
//		ActionResponseVO<T> response = new ActionResponseVO<>();
//		response.setResult(result);
//
//		ActionMessageVO<T> action = new ActionMessageVO<>();
//		action.setType(type);
//		action.setResponse(response);
//		action.setCreated(getUtcTime());
//
//		sendMessage(destination, action);
//	}
//
//	public <T> void sendMessage(String destination, ActionMessageVO<T> payload) {
//        log.debug(">>>> sendMessage 2 - destination: {}", destination);
//		ClientMessageVO<ActionMessageVO<T>> clientMessage = new ClientMessageVO<>();
//		clientMessage.setDestination(destination);
//		clientMessage.setPayload(payload);
//
//		sendMessage(clientMessage);
//	}
//
//	public <T> void sendMessage(ClientMessageVO<T> clientMessage) {
////		log.debug(JsonUtils.toPrettyString(clientMessage));
//        log.debug(">>>> sendMessage 3 - destination: {}", clientMessage.getDestination());
//		messaging.convertAndSend(clientMessage.getDestination(), clientMessage.getPayload());
//	}
}
