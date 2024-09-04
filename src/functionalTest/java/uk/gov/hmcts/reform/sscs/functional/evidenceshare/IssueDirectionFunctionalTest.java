package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import java.time.LocalDate;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class IssueDirectionFunctionalTest extends AbstractFunctionalTest {

    @Autowired
    private IdamService idamService;
    private IdamTokens idamTokens;
    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";
    @Value("${test-url}")
    protected String testUrl;


    public IssueDirectionFunctionalTest() {
        super();
    }

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
    }

    // Need tribunals running to pass this functional test
    @Test
    public void processAnIssueDirectionEvent_shouldUpdateInterlocReviewState() throws IOException {

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
    public void processAnIssueDirectionEvent_ifPastHearingExcludedDatesAreOnCaseDetails() throws IOException {
        idamTokens = idamService.getIdamTokens();
        String json = BaseHandler.getJsonCallbackForTest("handlers/directionissued/directionIssuedAboutToSubmitCallback.json");
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
}
