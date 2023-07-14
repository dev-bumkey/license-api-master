package run.acloud.commons.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.commons.util.HttpClientUtil;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.commons.vo.GatewayTokenVO;
import run.acloud.commons.vo.RequestGatewayTokenVO;
import run.acloud.commons.vo.ResponseGatewayTokenVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailGatewayProperties;

import java.io.IOException;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 25.
 */
@Slf4j
@Component
public class GatewayApiClient {


    @Autowired
    private CocktailGatewayProperties cocktailGatewayProperties;

    /**
     * Token 요청 For BAAS
     *
     * @param requestToken
     * @return
     * @throws CocktailException
     */
    public ResponseGatewayTokenVO<GatewayTokenVO> requestBaasToken(RequestGatewayTokenVO requestToken) throws CocktailException{
        String url = String.format("%s%s", StringUtils.removeEnd(cocktailGatewayProperties.getGatewayHost(), "/"), "/auth/a/token");

        ResponseGatewayTokenVO<GatewayTokenVO> result;
        try {
            String response = HttpClientUtil.doPost(url, ObjectMapperUtils.getMapper().writeValueAsString(requestToken));
            result = ObjectMapperUtils.getMapper().readValue(response, new TypeReference<ResponseGatewayTokenVO<GatewayTokenVO>>(){});
            log.debug("GatewayApiClient.{} => {}", "requestBaasToken", JsonUtils.toGson(result));

        } catch (IOException ioe) {
            throw new CocktailException("fail to call Gateway API - [requestBaasToken]!!", ioe, ExceptionType.ExternalApiFail_GatewayApi);
        } catch (Exception e) {
            throw new CocktailException("fail to call Gateway API - [requestBaasToken]!!", e, ExceptionType.ExternalApiFail_GatewayApi);
        }

        return result;
    }

    /**
     * Token 요청 For FAAS
     *
     * @param requestToken
     * @return
     * @throws CocktailException
     */
    public ResponseGatewayTokenVO<GatewayTokenVO> requestFaasToken(RequestGatewayTokenVO requestToken) throws CocktailException{
        String url = String.format("%s%s", StringUtils.removeEnd(cocktailGatewayProperties.getGatewayHost(), "/"), "/auth/b/token");

        ResponseGatewayTokenVO<GatewayTokenVO> result;
        try {
            String response = HttpClientUtil.doPost(url, ObjectMapperUtils.getMapper().writeValueAsString(requestToken));
            result = ObjectMapperUtils.getMapper().readValue(response, new TypeReference<ResponseGatewayTokenVO<GatewayTokenVO>>(){});
            log.debug("GatewayApiClient.{} => {}", "requestFaasToken", JsonUtils.toGson(result));

        } catch (IOException ioe) {
            throw new CocktailException("fail to call Gateway API - [requestFaasToken]!!", ioe, ExceptionType.ExternalApiFail_GatewayApi);
        } catch (Exception e) {
            throw new CocktailException("fail to call Gateway API - [requestFaasToken]!!", e, ExceptionType.ExternalApiFail_GatewayApi);
        }

        return result;
    }

    private ResultVO convertResponseToResult(String response, String callMethodName) throws IOException {
        ResultVO result = ObjectMapperUtils.getMapper().readValue(response, new TypeReference<ResultVO>(){});

        log.info("GatewayApiClient.{} => {}", callMethodName, JsonUtils.toGson(result));

        return result;
    }

    private String genGetUrlWithParam(String url, Map<String, Object> paramMap, boolean isOptionalParam) throws Exception{
        if(!isOptionalParam && MapUtils.isEmpty(paramMap)){
            throw new CocktailException("Invalid parameter!!", ExceptionType.ExternalApiFail_GatewayApi);
        }else{
            StringBuffer paramStr = new StringBuffer();
            for(Map.Entry entryRow : paramMap.entrySet()){
                paramStr.append(String.format("%s=%s&", entryRow.getKey(), entryRow.getValue()));
            }
            return String.format("%s?%s", url, StringUtils.substringBeforeLast(paramStr.toString(), "&"));
        }
    }

}
