package helper;

import org.springframework.test.annotation.ProfileValueSource;
import org.springframework.test.annotation.SystemProfileValueSource;

public class EnvironmentProfileValueSource implements ProfileValueSource {

    private final SystemProfileValueSource systemProfileValueSource = SystemProfileValueSource.getInstance();

    public String get(String key) {

        if ("environment.shared-ccd".equals(key)) {
            return isPreviewOrAatEnv() ? "true" : "false";
        }

        return systemProfileValueSource.get(key);
    }

    private boolean isPreviewOrAatEnv() {
        final String testUrl = getEnvOrEmpty("TEST_URL");
        return testUrl.contains("preview.internal") || testUrl.contains("aat.internal");
    }

    public static String getEnvOrEmpty(String name) {
        String value = System.getenv(name);
        if (value == null) {
            return "";
        }

        return value;
    }

}
