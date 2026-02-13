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
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class ActionFurtherEvidenceSubmittedCallbackHandlerTest extends BaseHandler {

    protected static final String CASE_ID_TO_BE_REPLACED = "12345678";

    @ParameterizedTest
    @CsvSource({
        "NON_COMPLIANT, informationReceivedForInterlocJudge, interlocutoryReviewState, reviewByJudge",
        "CREATE_WITH_DWP_TEST_CASE, sendToInterlocReviewByJudge, withDwp, reviewByJudge",
        "CREATE_WITH_DWP_TEST_CASE, sendToInterlocReviewByTcw, withDwp, reviewByTcw"
    })
    public void givenSubmittedCallbackForActionFurtherEvidence_shouldUpdateFieldAndTriggerEvent(
        String eventTypeName,
        String furtherEvidenceActionSelectedOption,
        String expectedState,
        String expectedReviewedBy) throws Exception {

        EventType eventType = EventType.valueOf(eventTypeName);
        Callback<SscsCaseData> callback = getSscsCaseDataCallback(furtherEvidenceActionSelectedOption);
        Long caseId = createCaseTriggeringGivenEvent(eventType, callback.getCaseDetails().getCaseData()).getId();
        callback = addCaseIdtoCallback(callback, caseId.toString());
        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(callback)
            .post("/ccdSubmittedEvent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.interlocReviewState", equalTo(expectedReviewedBy));

        assertEquals(expectedState, ccdService.getByCaseId(caseId, idamTokens).getState());
    }

    private SscsCaseDetails createCaseTriggeringGivenEvent(EventType eventType, SscsCaseData caseData) {
        SscsCaseData sscsCaseData = buildSscsCaseDataForTesting("Bowie", "AB 44 88 12 Y", caseData);
        return ccdService.createCase(sscsCaseData,
            eventType.getCcdType(), CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    private Callback<SscsCaseData> getSscsCaseDataCallback(String furtherEvidenceActionSelectedOption) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("handlers/actionfurtherevidence/actionFurtherEvidenceSubmittedCallback.json")).getFile();
        String jsonCallback = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        Callback<SscsCaseData> sscsCaseDataCallback = deserializer.deserialize(jsonCallback);
        sscsCaseDataCallback.getCaseDetails().getCaseData().getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionSelectedOption, "Information received for Interloc - send to Judge"));
        return sscsCaseDataCallback;
    }


    private Callback<SscsCaseData> addCaseIdtoCallback(Callback<SscsCaseData> sscsCaseDataCallback, String id) {
        String jsonCallback = serializeSscsCallback(sscsCaseDataCallback);
        jsonCallback = jsonCallback.replace(CASE_ID_TO_BE_REPLACED, id);
        return deserializer.deserialize(jsonCallback);
    }
}
