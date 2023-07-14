package run.acloud.commons.util;

import org.springframework.util.AntPathMatcher;

public class AntPathMatcherUtils {

    public AntPathMatcherUtils() {
    }

    private static class SingletonHolder {
        private static final AntPathMatcher INSTANCE = new AntPathMatcher();
    }
    public static AntPathMatcher getPathMatcher() {
        return SingletonHolder.INSTANCE;
    }
}
