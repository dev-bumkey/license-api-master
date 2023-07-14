package run.acloud.api.resource.util;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import run.acloud.commons.jackson.OffsetDateTimeDeserializer;

import java.time.OffsetDateTime;

public class JavaTimeModuleUtils {

    public JavaTimeModuleUtils() {
    }

    private static class SingletonHolder {
        private static final JavaTimeModule INSTANCE = (JavaTimeModule)new JavaTimeModule().addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer());;
    }

    public static JavaTimeModule getModule() {
        return SingletonHolder.INSTANCE;
    }
}
