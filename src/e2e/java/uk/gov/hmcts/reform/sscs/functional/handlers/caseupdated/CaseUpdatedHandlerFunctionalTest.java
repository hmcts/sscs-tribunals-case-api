package uk.gov.hmcts.reform.sscs.functional.handlers.caseupdated;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class CaseUpdatedHandlerFunctionalTest extends BaseHandler {

    @Test
    public void givenSubmittedCallbackForCaseUpdated_shouldAddJointParty() throws Exception {

        SscsCaseDetails sscsCaseDetails = createCaseInWithDwpState();

        Long caseId = sscsCaseDetails.getId();

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(getJsonCallbackForTest(caseId))
                .post("/ccdSubmittedEvent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.jointParty", equalTo("Yes"));
    }

    private String getJsonCallbackForTest(Long caseId) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("handlers/caseUpdated/caseUpdatedSubmittedCallback.json")).getFile();
        String body = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        return body.replace("CASE_ID_TO_BE_REPLACED", caseId.toString());
    }
}
