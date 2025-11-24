package uk.gov.hmcts.reform.sscs;

import static java.lang.Boolean.parseBoolean;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

@Slf4j
@Component
public class CftlibConfig implements CFTLibConfigurer {

    @Value("${idam.api.url}")
    private String idamHost;

    @Override
    public void configure(CFTLib lib) throws Exception {

        createIdamUser(
            "system.update@hmcts.net",
            "Service",
            "Account",
            List.of(
                "caseworker",
                "caseworker-sscs",
                "caseworker-sscs-superuser",
                "caseworker-sscs-clerk",
                "caseworker-sscs-systemupdate",
                "caseworker-sscs-judge",
                "caseworker-sscs-dwpresponsewriter",
                "caseworker-sscs-registrar",
                "caseworker-caa"));

        lib.createIdamUser("local.test@example.com", "caseworker", "caseworker-sscs");
        lib.createIdamUser(
            "super-user@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-superuser",
            "hearing-manager");
        lib.createIdamUser("sscs-citizen2@hmcts.net", "citizen");
        lib.createIdamUser("sscs-citizen-khan@hmcts.net", "citizen");
        lib.createIdamUser(
            "judge@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-judge",
            "caseworker-sscs-judge-salaried");
        lib.createIdamUser(
            "clerk@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-clerk",
            "hearing-manager");
        lib.createIdamUser(
            "registrar@example.com", "caseworker", "caseworker-sscs", "caseworker-sscs-registrar");
        lib.createIdamUser(
            "dwpuser@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-dwpresponsewriter");
        lib.createIdamUser(
            "hmrcuser@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-hmrcresponsewriter");
        lib.createIdamUser(
            "ibcauser@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-ibcaresponsewriter");
        lib.createIdamUser("ctsc-administrator@hmcts.net", "caseworker", "caseworker-sscs");
        lib.createIdamUser("regional-centre-admin@fake.hmcts.net", "caseworker", "caseworker-sscs");
        lib.createIdamUser(
            "data.store.idam.system.user@gmail.com", "ccd-import", "manage-user", "caseworker");
        lib.createIdamUser(
            "wa-system-user@fake.hmcts.net", "caseworker-wa", "caseworker-wa-task-configuration");
        lib.createIdamUser("tribunal-member-1@fake.hmcts.net", "caseworker", "caseworker-sscs");
        lib.createIdamUser("tribunal-member-2@fake.hmcts.net", "caseworker", "caseworker-sscs");
        lib.createIdamUser("tribunal-member-3@fake.hmcts.net", "caseworker", "caseworker-sscs");
        lib.createIdamUser(
            "judge-feepaid@example.com", "caseworker", "caseworker-sscs", "caseworker-sscs-judge");
        lib.createRoles(
            "caseworker-sscs-superuser",
            "caseworker-sscs-clerk",
            "caseworker-sscs-systemupdate",
            "caseworker-sscs-judge",
            "caseworker-sscs-judge-salaried",
            "caseworker-sscs-dwpresponsewriter",
            "caseworker-sscs-hmrcresponsewriter",
            "caseworker-sscs-ibcaresponsewriter",
            "caseworker-sscs-registrar",
            "caseworker-sscs-callagent",
            "caseworker-sscs-teamleader",
            "caseworker-sscs-panelmember",
            "caseworker-sscs-bulkscan",
            "caseworker-sscs-anonymouscitizen",
            "caseworker-sscs-pcqextractor",
            "citizen",
            "caseworker-sscs",
            "caseworker",
            "hearing-manager",
            "hearing-viewer",
            "caseworker-wa",
            "caseworker-wa-task-configuration",
            "caseworker-ras-validation",
            "GS_profile");
        var def = Files.readAllBytes(Path.of("./definitions/benefit/CCD_SSCSDefinition_LOCAL.xlsx"));
        lib.importDefinition(def);

        var roleAssignments =
            Resources.toString(
                Resources.getResource("am-role-assignments.json"), StandardCharsets.UTF_8);
        lib.configureRoleAssignments(roleAssignments);

        if (parseBoolean(System.getenv("ENABLE_WORK_ALLOCATION"))) {
            loadCamundaFiles();
        }
    }

    @SneakyThrows
    private void loadCamundaFiles() {
        int code =
            new ProcessBuilder("./src/cftlib/resources/scripts/camunda-deployment.sh")
                .inheritIO()
                .start()
                .waitFor();

        if (code != 0) {
            log.error("****** Camunda deployment failed ******");
            log.info("Exit value: {}", code);
        } else {
            log.info("Camunda deployment successful");
        }
    }

    @SneakyThrows
    private void createIdamUser(String email, String forename, String account, List<String> roles) {
        final String json =
            new ObjectMapper()
                .writeValueAsString(
                    Map.of("email", email, "forename", forename, "surname", account, "roles", roles.stream().map(r -> Map.of("code", r)).toList()));

        var request =
            HttpRequest.newBuilder()
                .uri(URI.create("%s/testing-support/accounts".formatted(idamHost)))
                .header("content-type", "application/json")
                .POST(ofString(json))
                .build();

        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (isNotOk(response)) {
                throw new RuntimeException("Failed to create idam account" + response.statusCode());
            }
        }
    }

    private static boolean isNotOk(HttpResponse<String> response) {
        return response.statusCode() != 200;
    }

}
