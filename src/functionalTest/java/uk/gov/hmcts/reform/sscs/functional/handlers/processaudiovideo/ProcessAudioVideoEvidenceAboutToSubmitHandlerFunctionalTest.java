package uk.gov.hmcts.reform.sscs.functional.handlers.processaudiovideo;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import junitparams.JUnitParamsRunner;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class ProcessAudioVideoEvidenceAboutToSubmitHandlerFunctionalTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallback_shouldSendSelectedEvidenceToJudge() throws Exception {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest(
                "handlers/processaudiovideo/processAudioVideoEvidenceAboutToSubmitCallback.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.interlocReviewState", equalTo("reviewByJudge"))
                .assertThat().body("data.audioVideoEvidence[0].value.processedAction", equalTo("sentToJudge"));
    }
}
