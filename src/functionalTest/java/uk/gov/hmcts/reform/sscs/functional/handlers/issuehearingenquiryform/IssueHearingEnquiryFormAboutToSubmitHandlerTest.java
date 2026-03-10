package uk.gov.hmcts.reform.sscs.functional.handlers.issuehearingenquiryform;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import java.time.LocalDate;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
class IssueHearingEnquiryFormAboutToSubmitHandlerTest extends BaseHandler {

    @Test
    void shouldHandleIssueHearingEnquiryFormAboutToSubmit() throws IOException {

        final Callback<SscsCaseData> sscsCaseDataCallback = deserializeCallbackData(
            "handlers/issuehearingenquiryform/issueHearingEnquiryFormCallback.json");

        assertThat(sscsCaseDataCallback.getCaseDetails().getCaseData().getDirectionDueDate()).isNull();
        assertThat(sscsCaseDataCallback.getCaseDetails().getCaseData().getInterlocReviewState()).isNull();

        assertThatCaseDataUpdated(sscsCaseDataCallback);
    }

    @Test
    void shouldResetDirectionDueDateTo21DaysInFuture() throws IOException {

        final Callback<SscsCaseData> sscsCaseDataCallback = deserializeCallbackData(
            "handlers/issuehearingenquiryform/issueHearingEnquiryFormCallback.json");

        sscsCaseDataCallback.getCaseDetails().getCaseData().setDirectionDueDate("2025-05-24");
        sscsCaseDataCallback.getCaseDetails().getCaseData().setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);

        assertThatCaseDataUpdated(sscsCaseDataCallback);
    }

    private void assertThatCaseDataUpdated(Callback<SscsCaseData> sscsCaseDataCallback) {
        given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON).header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(serializeSscsCallback(sscsCaseDataCallback)).post("/ccdAboutToSubmit").then().statusCode(HttpStatus.SC_OK).log()
            .all(true).rootPath("data").assertThat().body("interlocReviewState", equalTo("hefIssued")).assertThat()
            .body("directionDueDate", equalTo(LocalDate.now().plusDays(21).toString()));
    }

}
