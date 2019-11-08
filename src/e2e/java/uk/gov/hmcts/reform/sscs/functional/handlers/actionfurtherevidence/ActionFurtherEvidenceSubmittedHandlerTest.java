package uk.gov.hmcts.reform.sscs.functional.handlers.actionfurtherevidence;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.functional.ccd.UpdateCaseInCcdTest.buildSscsCaseDataForTestingWithValidMobileNumbers;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class ActionFurtherEvidenceSubmittedHandlerTest extends BaseHandler {

    private Long ccdCaseId;

    @Before
    public void setUp() {
        super.setUp();
        ccdCaseId = createCaseInCcdInterlocutoryReviewState().getId();
    }

    private SscsCaseDetails createCaseInCcdInterlocutoryReviewState() {

        return ccdService.createCase(buildSscsCaseDataForTestingWithValidMobileNumbers(),
            EventType.NON_COMPLIANT.getCcdType(), "non compliant created test case",
            "non compliant created test case", idamTokens);
    }

    @Test
    public void givenSubmittedCallbackForActionFurtherEvidence_shouldUpdateFieldAndTriggerEvent() throws Exception {
        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest())
            .post("/ccdSubmittedEvent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.interlocReviewState", equalTo("reviewByJudge"));

        assertEquals("interlocutoryReviewState",
            ccdService.getByCaseId(ccdCaseId, idamTokens).getState());
    }

    private String getJsonCallbackForTest() throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("handlers/actionfurtherevidence/actionFurtherEvidenceSubmittedCallback.json")).getFile();
        String jsonCallback = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        return jsonCallback.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId.toString());
    }
}

