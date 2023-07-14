package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 21.
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailBuilderProperties.PREFIX)
public class CocktailBuilderProperties {
    public static final String PREFIX = "cocktail.builder";

    private Boolean needRegister;

    private String buildApiUrl; // build개선,, 추가

    private int buildApiPort; // build개선,, 추가

    // build개선, 2019/01/4, 추가
    private String buildQueueUrl;   // BUILD_QUEUE_URL
    private String buildQueueExternalUrl;   // BUILD_QUEUE_EXTERNAL_URL => 외부 URL
    private String buildQueueCid;   // BUILD_QUEUE_CID, build-queue의 nats streaming server cluster ID
    private String buildQueueUser;  // NATS_USERNAME, secret 사용
    private String buildQueuePasswd;// NATS_PASSWORD, secret 사용
    private String buildQueueClientCertDir;   //20230530, coolingi, 추가, nats client 인증서 secret mount 디렉토리 path

    private boolean buildLogDbSaveEnabled; // Build log DB 저장 여부, true : 저장, false : 저장안함
    private int defaultParallelBuildCnt; //DEFAULT_PARALLEL_BUILD_CNT, default 동시 빌드 서버 갯수, system(account)별 갯수

    private String buildServerChartRepo;    // cocktail-app
    private String buildServerChartName;    // cocktail-app/build-server
    private String buildServerChartVersion; // 1.0.1
}
