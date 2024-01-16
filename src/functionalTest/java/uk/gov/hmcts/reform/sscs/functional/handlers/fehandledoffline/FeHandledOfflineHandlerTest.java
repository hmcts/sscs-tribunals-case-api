package uk.gov.hmcts.reform.sscs.functional.handlers.fehandledoffline;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class FeHandledOfflineHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallback_shouldClearHmctsDwpStateAndSetEvidenceIssuedToYes() throws Exception {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(BaseHandler.getJsonCallbackForTest("handlers/fehandledoffline/feHandledOfflineAboutToSubmitCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().body(true)
            .assertThat().body("data.hmctsDwpState", is(nullValue()))
            .assertThat().body("data.sscsDocument[0].value.documentType", equalTo("Other document"))
            .assertThat().body("data.sscsDocument[0].value.evidenceIssued", is("Yes"))
            .assertThat().body("data.sscsDocument[1].value.documentType", equalTo("representativeEvidence"))
            .assertThat().body("data.sscsDocument[1].value.evidenceIssued", is("Yes"))
            .assertThat().body("data.sscsDocument[2].value.documentType", equalTo("appellantEvidence"))
            .assertThat().body("data.sscsDocument[2].value.evidenceIssued", is("Yes"))
            .assertThat().body("data.sscsDocument[3].value.documentType", equalTo("dl16"))
            .assertThat().body("data.sscsDocument[3].value.evidenceIssued", is(nullValue()));
    }

}
