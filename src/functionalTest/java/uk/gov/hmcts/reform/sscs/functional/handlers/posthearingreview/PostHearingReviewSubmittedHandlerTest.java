package uk.gov.hmcts.reform.sscs.functional.handlers.posthearingreview;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.SET_ASIDE_GRANTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import java.time.LocalDate;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correction;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class PostHearingReviewSubmittedHandlerTest extends BaseHandler {
    protected static final String CASE_ID_TO_BE_REPLACED = "12345656789";

    @Autowired
    private ObjectMapper mapper;

    @DisplayName("Given about to submit callback for state of reasons, should set fields")
    @Test
    public void testStatementOfReasonsSubmitted() throws IOException {
        String jsonCallback = getJsonCallbackForTest("callback/postHearingReview.json");
        jsonCallback = jsonCallback.replaceFirst("invoking_event", "postHearingReview");

        Callback<SscsCaseData> callback = deserializer.deserialize(jsonCallback);
        SscsCaseDetails caseDetails = createCaseWithAdditionalSetting(sscsCaseData -> {
            sscsCaseData.setPostHearing(PostHearing.builder()
                    .correction(
                            Correction.builder().isCorrectionFinalDecisionInProgress(NO)
                                    .build())
                    .build());
            sscsCaseData.setDocumentGeneration(DocumentGeneration.builder()
                    .generateNotice(YES)
                    .build());
            sscsCaseData.setDocumentStaging(DocumentStaging.builder()
                    .dateAdded(LocalDate.now())
                    .build());
        });
        callback = replaceCallbackCaseId(callback, caseDetails.getId().toString(), CASE_ID_TO_BE_REPLACED);

        String response = RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(callback)
                .post("/ccdSubmittedEvent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true).extract().body().asString();

        JsonNode root = mapper.readTree(response);
        SscsCaseData result = mapper.readValue(root.path("data").toPrettyString(), new TypeReference<>(){});
        assertThat(result.getPostHearing().getCorrection().getIsCorrectionFinalDecisionInProgress()).as(result.getCcdCaseId())
                .isEqualTo(NO);
        assertThat(result.getDocumentGeneration().getGenerateNotice()).as(result.getCcdCaseId())
                .isEqualTo(YES);
        assertThat(result.getDocumentStaging().getDateAdded()).as(result.getCcdCaseId())
                .isEqualTo(LocalDate.now());
        assertThat(result.getDwpState()).as(result.getCcdCaseId())
                .isEqualTo(SET_ASIDE_GRANTED);
        assertThat(result.getInterlocReferralReason()).as(result.getCcdCaseId())
                .isNull();
        assertThat(result.getInterlocReviewState()).as(result.getCcdCaseId())
                .isEqualTo(AWAITING_ADMIN_ACTION);
    }
}
