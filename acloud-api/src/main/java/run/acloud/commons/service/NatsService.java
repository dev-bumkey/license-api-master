package run.acloud.commons.service;

import lombok.extern.slf4j.Slf4j;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.PemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.framework.properties.CocktailBuilderProperties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class NatsService {

    @Autowired
    private CocktailBuilderProperties cocktailBuilderProperties;

    /**
     * nats 연동을 위한 인증서 읽어 SSLContext 객체 생성 하는 메서드</br>
     * </br>
     * nats 인증서 secret를 /etc/certs/nats 디렉토리 mount함.</br>
     * 디렉토리 하위에 tls.crt, tls.key, ca.crt 파일이 존재함.</br>
     *
     * @return SSLContext
     * @throws IOException
     */
    private SSLContext makeSSLConetext() throws IOException {
        // 인증서디렉토리
        String certDir = cocktailBuilderProperties.getBuildQueueClientCertDir();

        // 인증서 경로 설정
        Path certPath = Paths.get(certDir+"/tls.crt");
        Path keyPath = Paths.get(certDir+"/tls.key");
        Path caPath = Paths.get(certDir+"/ca.crt");

        // 파일이 하나라도 존재 하지 않으면 null 리턴
        boolean fileExists = Files.exists(certPath) && Files.exists(keyPath) && Files.exists(caPath);
        if (!fileExists) {
            return null;
        }

        // 인증서 파일 읽기
        byte[] certData={};
        byte[] keyData={};
        byte[] caData={};

        certData = Files.readAllBytes(certPath);
        keyData = Files.readAllBytes(keyPath);
        caData = Files.readAllBytes(caPath);

        X509ExtendedKeyManager keyManager = PemUtils.loadIdentityMaterial(new ByteArrayInputStream(certData), new ByteArrayInputStream(keyData));
        X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(new ByteArrayInputStream(caData));
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(keyManager)
                .withTrustMaterial(trustManager)
                .build();

        return sslFactory.getSslContext();

    }

    /**
     * Nats client Connection 생성을 위한 option 생성 메서드
     *
     * @return
     * @throws IOException
     */
    public io.nats.client.Options getNatsClientOption() throws IOException {
        // ssl 연결을 위한 context 생성
        SSLContext ctx = makeSSLConetext();

        // 연결 옵션 설정
        io.nats.client.Options options = new io.nats.client.Options.Builder()
                .server(cocktailBuilderProperties.getBuildQueueUrl())
                .userInfo(cocktailBuilderProperties.getBuildQueueUser(), cocktailBuilderProperties.getBuildQueuePasswd())
                .sslContext(ctx)
                .build();

        return options;
    }
}
