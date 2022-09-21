package uk.gov.hmcts.reform.sscs;

import java.io.IOException;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.ControlPlane;

@Slf4j
@Component
public class AdditionalComposeServiceRunner {

    public static final String BASE_COMPOSE_PATH = "./src/cftlib/resources/docker/";

    @PostConstruct
    public void startComposeServices() throws IOException, InterruptedException {
        ControlPlane.waitForDB();
        ControlPlane.waitForAuthServer();

        log.info("Starting additional services...");

        String[] additionalFiles = Optional.ofNullable(System.getenv("ADDITIONAL_COMPOSE_FILES"))
            .map(files -> files.split(","))
            .orElse(new String[] {""});

        if (additionalFiles[0].isBlank()) {
            log.info("No additional services requested during startup");
            return;
        }

        for (String additionalService : additionalFiles) {
            String path = BASE_COMPOSE_PATH + additionalService;

            ProcessBuilder processBuilder = new ProcessBuilder("docker", "compose", "-f", path, "up", "-d")
                .inheritIO();

            Process process = processBuilder.start();

            int code =  process.waitFor();

            if (code != 0) {
                log.error("****** Failed to start additional services in {} ******", additionalService);
                log.info("Exit value: {}", code);
            } else {
                log.info("Successfully started additional services in {}", additionalService);
            }
        }
    }
}
