package uk.gov.hmcts.reform.sscs.functional.handlers.issuehearingenquiryform;

import static io.restassured.RestAssured.given;
import static java.time.LocalDate.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.HEF_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
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

        final SscsCaseData caseData = callAboutToSubmitEndpoint(sscsCaseDataCallback);

        assertThat(caseData.getInterlocReviewState()).isEqualTo(HEF_ISSUED);
        assertThat(caseData.getDirectionDueDate()).isEqualTo(now().plusDays(21).toString());
    }

    @Test
    void shouldResetDirectionDueDateTo21DaysInFuture() throws IOException {

        final Callback<SscsCaseData> sscsCaseDataCallback = deserializeCallbackData(
            "handlers/issuehearingenquiryform/issueHearingEnquiryFormCallback.json");

        sscsCaseDataCallback.getCaseDetails().getCaseData().setDirectionDueDate("2025-05-24");
        sscsCaseDataCallback.getCaseDetails().getCaseData().setInterlocReviewState(REVIEW_BY_JUDGE);

        final SscsCaseData caseData = callAboutToSubmitEndpoint(sscsCaseDataCallback);

        assertThat(caseData.getInterlocReviewState()).isEqualTo(HEF_ISSUED);
        assertThat(caseData.getDirectionDueDate()).isEqualTo(now().plusDays(21).toString());
    }


    private SscsCaseData callAboutToSubmitEndpoint(Callback<SscsCaseData> sscsCaseDataCallback) {
        return given().contentType(ContentType.JSON)
                      .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                      .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                      .body(serializeSscsCallback(sscsCaseDataCallback))
                      .post("/ccdAboutToSubmit")
                      .then()
                      .log()
                      .body()
                      .statusCode(HttpStatus.SC_OK)
                      .and()
                      .extract()
                      .body()
                      .jsonPath()
                      .getObject("data", SscsCaseData.class);
    }

}
