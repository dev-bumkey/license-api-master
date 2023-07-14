package run.acloud.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClientUtil {
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 5000;
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 5000;

    // thread-safe
    private static CloseableHttpClient httpClient;
    private static PoolingHttpClientConnectionManager cm;
    private static volatile SchemaRegistryProvider schemaRegistryProvider;
    private static volatile HttpClientBuilder httpClientBuilder;

    static {
        init();
        closeExpiredConnectionsPeriodTask(60);
    }

    static void init() {
        resetHttpClientBuilder();
        cm = new PoolingHttpClientConnectionManager(schemaRegistryProvider.getSchemaRegistry());
        // max connections
        cm.setMaxTotal(200);
        // max connections per route
        cm.setDefaultMaxPerRoute(20);
        // set max connections for a specified route
        cm.setMaxPerRoute(new HttpRoute(new HttpHost("localhost", 8080)), 50);

        final RequestConfig requestConfig = RequestConfig.custom()
                // https://sarc.io/index.php/development/1172-unirest-invalid-expires-attribute-warning
//                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                // https://stackoverflow.com/questions/36473478/fixing-httpclient-warning-invalid-expires-attribute-using-fluent-api
                .setCookieSpec(CookieSpecs.STANDARD)
                // the socket timeout (SO_TIMEOUT) in milliseconds
                .setSocketTimeout(DEFAULT_CONNECT_TIMEOUT)
                // the timeout in milliseconds until a connection is
                // established.
                .setConnectTimeout(DEFAULT_SOCKET_TIMEOUT)
                // the timeout in milliseconds used when requesting a connection
                // from the connection pool.
                .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        httpClient = httpClientBuilder.setConnectionManager(cm).build();
    }

    public static void resetHttpClientBuilder() {
        schemaRegistryProvider = new DefaultSchemaRegistryProvider();
        httpClientBuilder = HttpClientBuilder.create();
    }

    public static abstract class SchemaRegistryProvider {
        /**
         * Must be non-null
         */
        public abstract Registry<ConnectionSocketFactory> getSchemaRegistry();
    }

    private static final class DefaultSchemaRegistryProvider extends SchemaRegistryProvider {
        @Override
        public Registry<ConnectionSocketFactory> getSchemaRegistry() {
            // this mimics PoolingHttpClientConnectionManager's default behavior,
            // except that we explicitly use SSLConnectionSocketFactory.getSystemSocketFactory()
            // to pick up the system level default SSLContext (where javax.net.ssl.* properties
            // related to keystore & truststore are specified)

            TrustStrategy trustStrategy = (x509Certificates, s) -> true;
            SSLContext sslContext = null;
            try {
                sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
            } catch (Exception e) {
                log.error("HttpClientUtils.loadTrustMaterial error", e);
            }
            httpClientBuilder.setSSLContext(sslContext);

            HostnameVerifier hostnameVerifier = (s, sslSession) -> true;
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            RegistryBuilder<ConnectionSocketFactory> builder = RegistryBuilder.create();
            builder.register("http", PlainConnectionSocketFactory.getSocketFactory());
            builder.register("https", SSLConnectionSocketFactory.getSocketFactory());
            return builder.build();
        }
    }

    /**
     * GET
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static String doGet(URI url, List<Header> headers) throws Exception {
        CloseableHttpResponse response = null;
        String result = "";
        try {
            log.debug("[BEGIN] doGet");

            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpGet.setHeader(HttpHeaders.ACCEPT, "application/json");
            if (CollectionUtils.isNotEmpty(headers)) {
                for (Header headerRow : headers) {
                    httpGet.setHeader(headerRow.getName(), headerRow.getValue());
                }
                log.debug("{}, headers : {}", url, JsonUtils.toGson(httpGet.getAllHeaders()));
            }

            response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() != 200) {
                log.error(response.toString());
                throw new CocktailException(String.format("doGet Failed : [URL: %s] HTTP error code : %d Response : [%s]", url, response.getStatusLine().getStatusCode(), response), ExceptionType.ExternalApiFail);
            }

            HttpEntity retEntity = response.getEntity();
            result = EntityUtils.toString(retEntity);
        } catch (Exception e) {
            HttpClientUtils.closeQuietly(response);
            throw new CocktailException("doGet : [URL: " + url + "]", e, ExceptionType.ExternalApiFail);
        } finally {
            HttpClientUtils.closeQuietly(response);

            log.debug("[END  ] doGet");
        }

        return result;
    }

    public static String doGet(String url, List<Header> headers) throws Exception {
        return doGet(URI.create(url), headers);
    }

    public static String doGet(URI url) throws Exception {
        return doGet(url, null);
    }

    public static String doGet(String url) throws Exception {
        return doGet(URI.create(url));
    }

    public static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String urlEncodeUTF8(Map<?,?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?,?> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    urlEncodeUTF8(entry.getKey().toString()),
                    urlEncodeUTF8(entry.getValue().toString())
            ));
        }
        return sb.toString();
    }

    /**
     * POST
     *
     * @param url
     * @param jsonParam
     * @return
     * @throws Exception
     */
    public static String doPost(URI url, List<Header> headers, String jsonParam) throws Exception {
        CloseableHttpResponse response = null;
        String result = "";
        try {
            log.debug("[BEGIN] {}.doPost");

            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
            if (CollectionUtils.isNotEmpty(headers)) {
                for (Header headerRow : headers) {
                    httpPost.setHeader(headerRow.getName(), headerRow.getValue());
                }
                log.debug("{}, headers : {}", url, JsonUtils.toGson(httpPost.getAllHeaders()));
            }

            if (StringUtils.isNotBlank(jsonParam)) {
                StringEntity entity = new StringEntity(jsonParam, StandardCharsets.UTF_8);
                entity.setContentType(ContentType.APPLICATION_JSON.toString());
                httpPost.setEntity(entity);

                log.debug("{}, params : {}", url, jsonParam);
            }

            response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() != 200) {
                log.error(response.toString());
                throw new CocktailException(String.format("doPost Failed : [URL: %s] HTTP error code : %d Response : [%s]", url, response.getStatusLine().getStatusCode(), response), ExceptionType.ExternalApiFail);
            }

            HttpEntity retEntity = response.getEntity();
            result = EntityUtils.toString(retEntity);
        } catch (Exception e) {
            HttpClientUtils.closeQuietly(response);
            throw new CocktailException("doPost : [URL: " + url + "] [json:" + jsonParam + "]", e, ExceptionType.ExternalApiFail);
        } finally {
            HttpClientUtils.closeQuietly(response);

            log.debug("[END  ] {}.doPost");
        }

        return result;
    }

    public static String doPost(String url, List<Header> headers, String jsonParam) throws Exception {
        return doPost(URI.create(url), headers, jsonParam);
    }

    public static String doPost(URI url, String jsonParam) throws Exception {
        return doPost(url, null, jsonParam);
    }

    public static String doPost(String url, String jsonParam) throws Exception {
        return doPost(URI.create(url), jsonParam);
    }


    /**
     * PUT
     *
     * @param url
     * @param jsonParam
     * @return
     * @throws Exception
     */
    public static String doPut(URI url, List<Header> headers, String jsonParam) throws Exception {
        CloseableHttpResponse response = null;
        String result = "";
        try {
            log.debug("[BEGIN] {}.doPut");

            HttpPut httpPut = new HttpPut(url);

            httpPut.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPut.setHeader(HttpHeaders.ACCEPT, "application/json");
            if (CollectionUtils.isNotEmpty(headers)) {
                for (Header headerRow : headers) {
                    httpPut.setHeader(headerRow.getName(), headerRow.getValue());
                }
                log.debug("{}, headers : {}", url, JsonUtils.toGson(httpPut.getAllHeaders()));
            }

            if (StringUtils.isNotBlank(jsonParam)) {
                StringEntity entity = new StringEntity(jsonParam, StandardCharsets.UTF_8);
                entity.setContentType(ContentType.APPLICATION_JSON.toString());
                httpPut.setEntity(entity);

                log.debug("{}, params : {}", url, jsonParam);
            }

            response = httpClient.execute(httpPut);

            if (response.getStatusLine().getStatusCode() != 200) {
                log.error(response.toString());
                throw new CocktailException(String.format("doPost Failed : [URL: %s] HTTP error code : %d Response : [%s]", url, response.getStatusLine().getStatusCode(), response), ExceptionType.ExternalApiFail);
            }

            HttpEntity retEntity = response.getEntity();
            result = EntityUtils.toString(retEntity);
        } catch (Exception e) {
            HttpClientUtils.closeQuietly(response);
            throw new CocktailException(String.format("doPost : [URL: %s] [headers: %s] [json: %s]", url, JsonUtils.toGson(headers), jsonParam), e, ExceptionType.ExternalApiFail);
        } finally {
            HttpClientUtils.closeQuietly(response);

            log.debug("[END  ] {}.doPut");
        }

        return result;
    }

    public static String doPut(String url, List<Header> headers, String jsonParam) throws Exception {
        return doPut(URI.create(url), headers, jsonParam);
    }

    public static String doPut(URI url, String jsonParam) throws Exception {
        return doPut(url, null, jsonParam);
    }

    public static String doPut(String url, String jsonParam) throws Exception {
        return doPut(URI.create(url), jsonParam);
    }


    /**
     * DELETE
     *
     * @param url
     * @param headers
     * @return
     * @throws Exception
     */
    public static String doDelete(URI url, List<Header> headers) throws Exception {
        CloseableHttpResponse response = null;
        String result = "";
        try {
            log.debug("[BEGIN] doDelete");

            HttpDelete httpDelete = new HttpDelete(url);

            httpDelete.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpDelete.setHeader(HttpHeaders.ACCEPT, "application/json");
            if (CollectionUtils.isNotEmpty(headers)) {
                for (Header headerRow : headers) {
                    httpDelete.setHeader(headerRow.getName(), headerRow.getValue());
                }
                log.debug("{}, headers : {}", url, JsonUtils.toGson(httpDelete.getAllHeaders()));
            }

            response = httpClient.execute(httpDelete);

            if (response.getStatusLine().getStatusCode() != 200) {
                log.error(response.toString());
                throw new CocktailException(String.format("doDelete Failed : [URL: %s] HTTP error code : %d Response : [%s]", url, response.getStatusLine().getStatusCode(), response), ExceptionType.ExternalApiFail);
            }

            HttpEntity retEntity = response.getEntity();
            result = EntityUtils.toString(retEntity);
        } catch (Exception e) {
            HttpClientUtils.closeQuietly(response);
            throw new CocktailException("doDelete : [URL: " + url + "]", e, ExceptionType.ExternalApiFail);
        } finally {
            HttpClientUtils.closeQuietly(response);

            log.debug("[END  ] doDelete");
        }

        return result;
    }

    public static String doDelete(String url, List<Header> headers) throws Exception {
        return doDelete(URI.create(url), headers);
    }

    public static String doDelete(URI url) throws Exception {
        return doDelete(url, null);
    }

    public static String doDelete(String url) throws Exception {
        return doDelete(URI.create(url));
    }

    private static void closeExpiredConnectionsPeriodTask(
            final int timeUnitBySecond) {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.SECONDS.sleep(timeUnitBySecond);
                } catch (InterruptedException e) {
                    log.error("Closing expired connections Interrupted.", e);
                }
                cm.closeExpiredConnections();
            }

        }).start();
    }
}