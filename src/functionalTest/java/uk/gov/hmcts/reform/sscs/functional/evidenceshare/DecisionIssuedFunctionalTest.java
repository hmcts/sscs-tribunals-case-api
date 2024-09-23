package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.io.IOException;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class DecisionIssuedFunctionalTest extends AbstractFunctionalTest{

    @Autowired
    private IdamService idamService;
    private IdamTokens idamTokens;
    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";
    @Value("${test-url}")
    protected String testUrl;

    @Autowired
    private ObjectMapper mapper;


    public DecisionIssuedFunctionalTest() {
        super();
    }

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
    }

    @Test
    public void processAnIssueDirectionEvent_ifPastOrFutureHearingExcludedDatesAreOnCaseDetails() throws IOException {
        idamTokens = idamService.getIdamTokens();
        String json = BaseHandler.getJsonCallbackForTest("handlers/decisionissued/decisionIssuedAboutToSubmitCallback.json");
        json = uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, "EVIDENCE_DOCUMENT", json);

        String response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(json)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .extract().body().asString();

        JsonNode root = mapper.readTree(response);
        SscsCaseData result = mapper.readValue(root.path("data").toPrettyString(), new TypeReference<>(){});
        assertThat(result.getInterlocReferralReason()).isNull();
        assertThat(result.getDirectionDueDate()).isNull();
        assertThat(result.getPostponement()).isNull();
        assertThat(result.getPostponementRequest()).isNull();
    }

}
