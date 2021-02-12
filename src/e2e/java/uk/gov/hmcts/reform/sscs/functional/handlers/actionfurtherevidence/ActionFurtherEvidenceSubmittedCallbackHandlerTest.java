package uk.gov.hmcts.reform.sscs.functional.handlers.actionfurtherevidence;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class ActionFurtherEvidenceSubmittedCallbackHandlerTest extends BaseHandler {

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Test
    @Parameters({
        "NON_COMPLIANT, informationReceivedForInterlocJudge, interlocutoryReviewState, awaitingAdminAction",
        "VALID_APPEAL_CREATED, sendToInterlocReviewByJudge, validAppeal, reviewByJudge",
        "VALID_APPEAL_CREATED, sendToInterlocReviewByTcw, validAppeal, reviewByTcw"
    })
    public void givenSubmittedCallbackForActionFurtherEvidence_shouldUpdateFieldAndTriggerEvent(
        EventType eventType,
        String furtherEvidenceActionSelectedOption,
        String expectedState,
        String expectedReviewedBy) throws Exception {

        Long caseId = createCaseTriggeringGivenEvent(eventType).getId();

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest(caseId, furtherEvidenceActionSelectedOption))
            .post("/ccdSubmittedEvent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.interlocReviewState", equalTo(expectedReviewedBy));

        assertEquals(expectedState, ccdService.getByCaseId(caseId, idamTokens).getState());
    }

    private SscsCaseDetails createCaseTriggeringGivenEvent(EventType eventType) {
        return ccdService.createCase(buildSscsCaseDataForTesting("Bowie", "AB 44 88 12 Y"),
            eventType.getCcdType(), CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    private String getJsonCallbackForTest(Long caseId, String furtherEvidenceActionSelectedOption) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("handlers/actionfurtherevidence/actionFurtherEvidenceSubmittedCallback.json")).getFile();
        String jsonCallback = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        jsonCallback = jsonCallback.replace("FURTHER_EVIDENCE_ACTION_SELECTED_OPTION",
            furtherEvidenceActionSelectedOption);
        return jsonCallback.replace("CASE_ID_TO_BE_REPLACED", caseId.toString());
    }
}

