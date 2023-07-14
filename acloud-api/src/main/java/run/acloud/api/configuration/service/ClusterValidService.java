package run.acloud.api.configuration.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.service.WorkloadResourceService;
import run.acloud.commons.provider.K8sClient;

@Slf4j
@Component
public class ClusterValidService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private Environment environment;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private K8sClient k8sClient;

    public boolean isCocktailCluster(String clusterId) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByClusterId(clusterId, "Y");

        return this.isCocktailCluster(cluster);
    }

    public boolean isCocktailCluster(Integer clusterSeq) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.isCocktailCluster(cluster);
    }

    public boolean isCocktailCluster(ClusterVO cluster) throws Exception{

        if(cluster != null){
            if (StringUtils.isBlank(cluster.getUseYn())) {
                return this.isCocktailCluster(cluster.getClusterSeq());
            } else {
                String namespace = environment.getProperty("MY_POD_NAMESPACE");
                String podName = environment.getProperty("MY_POD_NAME");

                if (StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(podName)) {
                    String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, podName);
                    return CollectionUtils.isNotEmpty(workloadResourceService.getPods(cluster, namespace, field, null, 1));
                } else {
                    return false;
                }
            }
        }

        return false;
    }

//    public void inclustertest() throws Exception {
//        String host = environment.getProperty(Config.ENV_SERVICE_HOST);
//        String port = environment.getProperty(Config.ENV_SERVICE_PORT);
//        String token =
//                new String(
//                        Files.readAllBytes(Paths.get(Config.SERVICEACCOUNT_TOKEN_PATH)), Charset.defaultCharset());
//        String ca = Base64Utils.encodeToString(Files.readAllBytes(Paths.get(Config.SERVICEACCOUNT_CA_PATH)));
//        String ns = new String(
//                Files.readAllBytes(Paths.get(Config.SERVICEACCOUNT_NAMESPACE_PATH)), Charset.defaultCharset());
//        String basePath = "";
//        try {
//            Integer iPort = Integer.valueOf(port);
//            URI uri = new URI("https", null, host, iPort, null, null, null);
//            basePath = uri.toString();
//        } catch (NumberFormatException | URISyntaxException e) {
//            throw new IllegalStateException(e);
//        }
////        ApiClient client = (ApiClient)k8sClient.create(AuthType.TOKEN, CubeType.MANAGED, basePath, null, CryptoUtils.encryptAES(token), null, null, CryptoUtils.encryptAES(ca));
//
//        log.info("\nhost: {}\nport: {}\ntoken\n{}\nca\n{}\nns: {}\nbasePath: {}\n", host, port, token, ca, ns, basePath);
//        ApiClient client = ClientBuilder.cluster().build();
//        CoreV1Api apiInstance = new CoreV1Api(client);
//        try {
//            V1Namespace v1Namespace = apiInstance.readNamespaceStatus("kube-system", null);
//            log.error("v1Namespace: \n{}", Yaml.getSnakeYaml().dumpAsMap(v1Namespace));
//        } catch (ApiException e) {
//            log.error(e.getResponseBody(), e);
//        }
//
//        try {
//            SecretGuiVO secretGui = new SecretGuiVO();
//            secretGui.setNamespace(ns);
//            secretGui.setName("incluster-secret");
//            secretGui.putDataItem("license", "Ic5OXgAAACIAAAACAAAABwAAAA9jb21wYW55SFlVTkRBSUNBUElUQUwxAAAAGgAAAAsAAAAKZXhwaXJ5RGF0ZQAA5nfM+YAAAAAAGQAAAAsAAAAJaXNzdWVEYXRlAAABgz5x+AAAAAAWAAAAAgAAAAYAAAAEaXNzdWVyTkFNVQAAACEAAAAMAAAACWxpY2Vuc2VJZL27bFf2qWdFOsGuKCooTA8AAACcAAAAAQAAABAAAACAbGljZW5zZVNpZ25hdHVyZXvRS7VLWXJpGcr4MwEU0JBit1KYoUkbIMTQKbGw1R/oTc+rEeIeE5zv0TJMYT18UrmcfSxVjIU/09RAWVfKuV8RVAKdpp5XFckfysm42dj219Slibg9TWoCn1iOItzDOJH7BXNV6ZIpDpnwRQ1GAwWXXBYKM6oI5ulvCE73ttixAAAAHQAAAAIAAAAHAAAACnB1cnBvc2VFTlRFUlBSSVNFAAAAFAAAAAIAAAAGAAAAAnJlZ2lvbktSAAAAIgAAAAIAAAAPAAAAB3NpZ25hdHVyZURpZ2VzdFNIQS01MTIAAAAUAAAAAgAAAAQAAAAEdHlwZUZVTEw=");
//            V1Secret secret = K8sSpecFactory.buildSecretV1(secretGui);
//            apiInstance.createNamespacedSecret(ns, secret, null, null, null, null);
//            Thread.sleep(2000);
//            V1Secret result = apiInstance.readNamespacedSecret("incluster-secret", ns, null);
//            log.error("V1Secret: \n{}", Yaml.getSnakeYaml().dumpAsMap(result));
//        } catch (ApiException e) {
//            log.error(e.getResponseBody(), e);
//        } catch (InterruptedException e) {
//            log.error(e.getMessage(), e);
//        }
//    }
}
