package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class IssueDirectionFunctionalTest extends AbstractFunctionalTest {

    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";

    @Value("${test-url}")
    protected String testUrl;

    @Autowired
    private IdamService idamService;
    private IdamTokens idamTokens;


    IssueDirectionFunctionalTest() {
        super();
    }

    @BeforeEach
    void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
    }

    // Need tribunals running to pass this functional test
    @Test
    void processAnIssueDirectionEvent_shouldUpdateInterlocReviewState() throws IOException {

        createDigitalCaseWithEvent(NON_COMPLIANT);

        String json = getJson(DIRECTION_ISSUED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        assertNull(caseData.getDirectionTypeDl());
    }

    @Test
    void processAnIssueDirectionEvent_ifPastOrFutureHearingExcludedDatesAreOnCaseDetails() throws IOException {
        idamTokens = idamService.getIdamTokens();
        String json = BaseHandler.getJsonCallbackForTest(
            "handlers/directionissued/directionIssuedAboutToSubmitCallback.json");
        json = uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, "EVIDENCE_DOCUMENT", json);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(json)
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.dwpState", equalTo("directionActionRequired"));
    }

    @Nested
    @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    class CmDirectionTypeToggleOn {
        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin,Yes", "confidentialityRefusedSendToAdmin,No"})
        void processAnIssueDirectionEvent_updateAppellantConfidentiality_whenCmFlagIsEnabledAndTheSelectedConfidentialityPartyIsAppellant(String directionType, String expectedConfidentiality) throws IOException {
            idamTokens = idamService.getIdamTokens();

            String json = BaseHandler.getJsonCallbackForTestAndReplace(
                "handlers/directionissued/directionIssuedAboutToSubmitCallbackAppellantConfidentialityRequired.json",
                List.of("[DIRECTION_TYPE_CODE]"),
                List.of(directionType));

            json = uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, "EVIDENCE_DOCUMENT", json);

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(json)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.appeal.appellant.confidentialityRequired", equalTo(expectedConfidentiality))
                .assertThat().body("data.appeal.appellant.confidentialityRequiredChangedDate", notNullValue());
        }

        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin,Yes", "confidentialityRefusedSendToAdmin,No"})
        void processAnIssueDirectionEvent_updateOtherPartyConfidentiality_whenCmFlagIsEnabledAndTheSelectedConfidentialityPartyIsOtherParty(String directionType, String expectedConfidentiality) throws IOException {
            idamTokens = idamService.getIdamTokens();

            String json = BaseHandler.getJsonCallbackForTestAndReplace(
                "handlers/directionissued/directionIssuedAboutToSubmitCallbackOtherPartyConfidentialityRequired.json",
                List.of("[DIRECTION_TYPE_CODE]"),
                List.of(directionType));

            json = uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, "EVIDENCE_DOCUMENT", json);

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(json)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log()
                .all(true)
                .assertThat().body("data.appeal.appellant.confidentialityRequired", nullValue())
                .assertThat().body("data.appeal.appellant.confidentialityRequiredChangedDate", nullValue())
                .assertThat().body("data.otherParties[0].value.confidentialityRequired", equalTo(expectedConfidentiality))
                .assertThat().body("data.otherParties[0].value.confidentialityRequiredChangedDate", notNullValue())
                .assertThat().body("data.otherParties[1].value.confidentialityRequired", nullValue());
        }
    }

    @Nested
    @DisabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    class CmDirectionTypeToggleOff {
        @ParameterizedTest
        @ValueSource(strings = {"confidentialityGrantedSendToAdmin", "confidentialityRefusedSendToAdmin"})
        void processAnIssueDirectionEvent_doNotUpdateAppellantConfidentiality_whenCmFlagIsNotEnabledAndTheSelectedConfidentialityPartyIsAppellant(String directionType) throws IOException {
            idamTokens = idamService.getIdamTokens();

            String json = BaseHandler.getJsonCallbackForTestAndReplace(
                "handlers/directionissued/directionIssuedAboutToSubmitCallbackAppellantConfidentialityRequired.json",
                List.of("[DIRECTION_TYPE_CODE]"),
                List.of(directionType));

            json = uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, "EVIDENCE_DOCUMENT", json);

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(json)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.appeal.appellant.confidentialityRequired", nullValue())
                .assertThat().body("data.appeal.appellant.confidentialityRequiredChangedDate", nullValue());
        }

        @ParameterizedTest
        @ValueSource(strings = {"confidentialityGrantedSendToAdmin", "confidentialityRefusedSendToAdmin"})
        void processAnIssueDirectionEvent_doNotUpdateOtherPartyConfidentiality_whenCmFlagIsNotEnabledAndTheSelectedConfidentialityPartyIsOtherParty(String directionType) throws IOException {
            idamTokens = idamService.getIdamTokens();

            String json = BaseHandler.getJsonCallbackForTestAndReplace(
                "handlers/directionissued/directionIssuedAboutToSubmitCallbackOtherPartyConfidentialityRequired.json",
                List.of("[DIRECTION_TYPE_CODE]"),
                List.of(directionType));

            json = uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, "EVIDENCE_DOCUMENT", json);

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(json)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log()
                .all(true)
                .assertThat().body("data.appeal.appellant.confidentialityRequired", nullValue())
                .assertThat().body("data.appeal.appellant.confidentialityRequiredChangedDate", nullValue())
                .assertThat().body("data.otherParties[0].value.confidentialityRequired", nullValue())
                .assertThat().body("data.otherParties[0].value.confidentialityRequiredChangedDate", nullValue())
                .assertThat().body("data.otherParties[1].value.confidentialityRequired", nullValue())
                .assertThat().body("data.otherParties[1].value.confidentialityRequiredChangedDate", nullValue());
        }
    }
}
