package run.acloud.commons.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;
import run.acloud.api.resource.util.JavaTimeModuleUtils;
import run.acloud.api.resource.util.PatchObjectMapper;
import run.acloud.commons.jackson.DateTimeSerializer;

public class ObjectMapperUtils {

    public ObjectMapperUtils() {
    }

    private static class SingletonHolder {
        private static final ObjectMapper INSTANCE = new ObjectMapper();
    }
    public static ObjectMapper getMapper() {
        return SingletonHolder.INSTANCE;
    }

    private static class PatchSingletonHolder {
        private static final PatchObjectMapper INSTANCE = new PatchObjectMapper();
    }
    public static PatchObjectMapper getPatchMapper() {
        return PatchSingletonHolder.INSTANCE;
    }

    private static class JodaSingletonHolder {
        private static final ObjectMapper INSTANCE = new ObjectMapper()
                .registerModule(new JodaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new SimpleModule().addSerializer(DateTime.class, new DateTimeSerializer()));
    }
    public static ObjectMapper getJodaMapper() {
        return JodaSingletonHolder.INSTANCE;
    }

    private static class JavaTimeSingletonHolder {
        private static final ObjectMapper INSTANCE = new ObjectMapper()
                .registerModule(new JodaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModuleUtils.getModule());
    }
    public static ObjectMapper getJavaTimeMapper() {
        return JavaTimeSingletonHolder.INSTANCE;
    }

    private static class DateTimeSingletonHolder {
        private static final ObjectMapper INSTANCE = new ObjectMapper()
                .registerModule(new JodaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new SimpleModule().addSerializer(DateTime.class, new DateTimeSerializer()))
                .registerModule(JavaTimeModuleUtils.getModule());
    }
    public static ObjectMapper getDateTimeMapper() {
        return DateTimeSingletonHolder.INSTANCE;
    }

    public static <T> T copyObject(Object orig, TypeReference<T> type) throws Exception {
        String str = getMapper().writeValueAsString(orig);
        return getMapper().readValue(str, type);
    }
}
