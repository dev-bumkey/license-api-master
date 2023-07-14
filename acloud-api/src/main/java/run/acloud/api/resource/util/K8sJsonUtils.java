package run.acloud.api.resource.util;

import io.kubernetes.client.openapi.JSON;

public class K8sJsonUtils {

    public K8sJsonUtils() {
    }

    private static class SingletonHolder {
        private static final JSON INSTANCE = new JSON();
    }

    public static JSON getJson() {
        return SingletonHolder.INSTANCE;
    }
}
