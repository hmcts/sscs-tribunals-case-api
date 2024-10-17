package uk.gov.hmcts.reform.sscs;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class ResourceLoader {

    private ResourceLoader() {
    }

    public static String loadJson(final String filePath) throws Exception {
        return new String(loadResource(filePath), StandardCharsets.UTF_8);
    }

    private static byte[] loadResource(final String filePath) throws Exception {
        URL url = ResourceLoader.class.getClassLoader().getResource(filePath);

        if (url == null) {
            throw new IllegalArgumentException(String.format("Could not find resource in path %s", filePath));
        }

        return Files.readAllBytes(Paths.get(url.toURI()));
    }
}
