package uk.gov.hmcts.reform.sscs.bulkscan;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.OK;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

@SpringBootTest
@TestPropertySource(locations = "classpath:application_e2e.yaml")
@RunWith(JUnitParamsRunner.class)
@Slf4j
public class ValidateSscsCaseFunctionalTest extends BaseFunctionalTest {

    @Value("${document_management.url}")
    private String documentManagementUrl;

    @Test
    public void validate_nino_normalised() throws IOException {
        createCase();
        String json = getJson("validationsscs/validate_sscs_case_request.json");
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);

        validateRecordEndpointRequest(json, OK.value());

        SscsCaseDetails caseDetails = ccdService.getByCaseId(Long.valueOf(ccdCaseId), idamTokens);

        assertEquals("AB225566B", caseDetails.getData().getAppeal().getAppellant().getIdentity().getNino());
    }

    @Test
    public void validate_and_update_incomplete_case_to_appeal_created_case() throws IOException {
        String json = getJson("validationsscs/validate_sscs_case_request.json");

        Response response = validateRecordEndpointRequest(json, OK.value());

        JsonPath validationResponse = response.getBody().jsonPath();

        assertSoftly(softly -> {
            softly.assertThat(validationResponse.getList("warnings")).isEmpty();
            softly.assertThat(validationResponse.getList("errors")).isEmpty();

            softly.assertAll();
        });
    }
}
