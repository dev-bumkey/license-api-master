package run.acloud.api.terminal.coders;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.acloud.api.terminal.vo.TerminalMessage;
import run.acloud.commons.util.JsonUtils;

@Slf4j
public class TerminalMessageEncoder implements Encoder.Text<TerminalMessage>{

    private static Logger terminalLogger = LoggerFactory.getLogger("terminal.logger");

    @Override
    public String encode(TerminalMessage message) throws EncodeException {
        return JsonUtils.toGson(message);
    }

    @Override
    public void init(EndpointConfig ec) {
        terminalLogger.debug("MessageEncoder - init method called");
    }

    @Override
    public void destroy() {
        terminalLogger.debug("MessageEncoder - destroy method called");
    }

}
