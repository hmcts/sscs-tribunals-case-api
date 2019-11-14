package uk.gov.hmcts.reform.sscs.functional.handlers;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class DwpChallengeValidityHandlerTest {

    @Value("${test-url}")
    private String testUrl;
    @Autowired
    private IdamService idamService;
    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void givenAboutToSubmitCallbackForSendToAdmin_shouldSetInterlocReviewStateField() throws Exception {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(BaseHandlerTest.getJsonCallbackForTest("handlers/interloc/dwpChallengeValidityCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.interlocReviewState", equalTo("reviewByJudge"));
    }


}
