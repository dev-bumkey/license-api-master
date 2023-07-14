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
import run.acloud.commons.vo.ADVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.io.IOException;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 25.
 */
@Slf4j
@Component
public class ADApiClient {


    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    public ResultVO authenticateAD(ADVO auth) throws CocktailException{
        String url = String.format("%s%s", StringUtils.removeEnd(cocktailServiceProperties.getAuthServerHost(), "/"), cocktailServiceProperties.getAuthCheckUrl());

        ResultVO result;
        try {
            result = this.convertResponseToResult(HttpClientUtil.doPost(url, ObjectMapperUtils.getMapper().writeValueAsString(auth)), "authenticateAD");

        } catch (IOException ioe) {
            throw new CocktailException("fail to call AD API - [authenticateAD]!!", ioe, ExceptionType.ExternalApiFail_ADApi);
        } catch (Exception e) {
            throw new CocktailException("fail to call AD API - [authenticateAD]!!", e, ExceptionType.ExternalApiFail_ADApi);
        }

        return result;
    }

    private ResultVO convertResponseToResult(String response, String callMethodName) throws IOException {
        ResultVO result = ObjectMapperUtils.getMapper().readValue(response, new TypeReference<ResultVO>(){});

        log.info("ADApiClient.{} => {}",callMethodName, JsonUtils.toGson(result));

        return result;
    }

    private String genGetUrlWithParam(String url, Map<String, Object> paramMap, boolean isOptionalParam) throws Exception{
        if(!isOptionalParam && MapUtils.isEmpty(paramMap)){
            throw new CocktailException("Invalid parameter!!", ExceptionType.ExternalApiFail_ADApi);
        }else{
            StringBuffer paramStr = new StringBuffer();
            for(Map.Entry entryRow : paramMap.entrySet()){
                paramStr.append(String.format("%s=%s&", entryRow.getKey(), entryRow.getValue()));
            }
            return String.format("%s?%s", url, StringUtils.substringBeforeLast(paramStr.toString(), "&"));
        }
    }

}
