package run.acloud.api.resource.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class K8sMapperUtils {

    public K8sMapperUtils() {
    }

    private static class K8sSingletonHolder {
        private static final ObjectMapper INSTANCE = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JodaModule())
                .registerModule(JavaTimeModuleUtils.getModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    public static ObjectMapper getMapper() {
        return K8sSingletonHolder.INSTANCE;
    }
}
