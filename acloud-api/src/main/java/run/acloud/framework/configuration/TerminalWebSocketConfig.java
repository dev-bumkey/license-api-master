package run.acloud.framework.configuration;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import run.acloud.api.terminal.endpoint.TerminalEndPoint;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.properties.CocktailTerminalProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
@EnableConfigurationProperties({CocktailTerminalProperties.class})
public class TerminalWebSocketConfig extends ServerEndpointConfig.Configurator {

    public static final String SESSION_HEADER_KEY = "REQUEST_HEADERS";

    private static Logger terminalLogger = LoggerFactory.getLogger("terminal.logger");

    // 객체를 하나만 유지하기 위해 static로 미리 생성함
    private static final TerminalEndPoint ENDPOINT = new TerminalEndPoint();

    private static Map<String, Map<String, List<String>>> HEADER_MAP = new ConcurrentHashMap<String, Map<String, List<String>>>();

    public static Map<String, Map<String, List<String>>> getHeaderMap(){
        return HEADER_MAP;
    }

    @Autowired
    private CocktailTerminalProperties cocktailTerminalProperties;

    // 이 메서드가 없으면 Web Socket URL 맵핑이 안되어 websocket 접속시 찾지를 못함
    @Bean
    public TerminalEndPoint createEndPoint(){
        return ENDPOINT;
    }

    // createEndPoint()를 사용해서 ServerEndPoint 클래스를 등록하고 ServlertContainer에 생성된 ServerEndPoint 객체를 등록한다.
    @Bean
    public ServerEndpointExporter endpointExporter() {
        return new ServerEndpointExporter();
    }

    // getEndpointInstance 는 해당 EndPoint로 접근할때 마다 호출되는 메서드 이다.
    // 아래 처럼 객체 생성을 하지 않는다면 요청시마다 새로운 객체를 생성하여 사용하게 된다.
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        if (TerminalEndPoint.class.equals(endpointClass)) {
            log.debug("=========== getEndpointInstance");
            return (T) ENDPOINT;
        } else {
            throw new InstantiationException();
        }
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        String dilimiter = "==================================================================";
        terminalLogger.debug("{}\n=========== header in modifyHandshake : {}\n{}", dilimiter, JsonUtils.toGson(request.getHeaders()), dilimiter);
        terminalLogger.debug("=========== TXID1 : "+Thread.currentThread().getId());

        TerminalWebSocketConfig.getHeaderMap().put(String.valueOf(Thread.currentThread().getId()), request.getHeaders());
    }

    @Bean
    @Qualifier("terminalExecutor")
    public TaskExecutor terminalExecutor() {
        return new ConcurrentTaskExecutor( Executors.newFixedThreadPool(this.cocktailTerminalProperties.getTerminalMaxConnection()) );
    }
}
