package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class DecisionIssuedFunctionalTest extends AbstractFunctionalTest {

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

    @BeforeEach
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
    }

    @Test
    public void processAnIssueDecisionEvent() throws IOException {
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
        assertThat(result.getPostponement().getUnprocessedPostponement()).isNull();
        assertThat(result.getPostponement().getPostponementEvent()).isNull();
        assertThat(result.getPostponementRequest().getActionPostponementRequestSelected()).isNull();
        assertThat(result.getDwpState()).isEqualTo(DwpState.STRUCK_OUT);
        assertThat(result.getState()).isEqualTo(State.DORMANT_APPEAL_STATE);
    }

}