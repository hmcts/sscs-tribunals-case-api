package uk.gov.hmcts.reform.sscs;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResourceLoader {

    private ResourceLoader() {
    }

    public static String loadJson(final String filePath) throws Exception {
        return new String(loadResource(filePath), StandardCharsets.UTF_8);
    }

    private static byte[] loadResource(final String filePath) throws Exception {
        URL url = ResourceLoader.class.getClassLoader().getResource(filePath);

        if (url == null) {
            throw new IllegalArgumentException("Could not find resource in path %s".formatted(filePath));
        }

        return Files.readAllBytes(Path.of(url.toURI()));
    }
}
