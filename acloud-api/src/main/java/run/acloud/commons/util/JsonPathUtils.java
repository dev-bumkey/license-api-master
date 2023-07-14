package run.acloud.commons.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class JsonPathUtils {
    public JsonPathUtils() {
    }

    private static class SingletonHolder {
        private static final ParseContext INSTANCE = JsonPath.using(Configuration.builder().jsonProvider(new JacksonJsonProvider(ObjectMapperUtils.getPatchMapper())).mappingProvider(new JacksonMappingProvider(ObjectMapperUtils.getPatchMapper())).build());
    }
    public static ParseContext getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
