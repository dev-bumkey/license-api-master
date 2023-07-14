package run.acloud.api.terminal.coders;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.acloud.api.terminal.vo.TerminalMessage;
import run.acloud.commons.util.JsonUtils;

@Slf4j
public class TerminalMessageDecoder implements Decoder.Text<TerminalMessage> {

    private static Logger terminalLogger = LoggerFactory.getLogger("terminal.logger");

    @Override
    public TerminalMessage decode(String message) throws DecodeException {
        terminalLogger.debug("TerminalMessage receiveMessage : "+message);
        return JsonUtils.fromGson(message, TerminalMessage.class);
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }

    @Override
    public void init(EndpointConfig endpointConfig) {}

    @Override
    public void destroy() {}

}