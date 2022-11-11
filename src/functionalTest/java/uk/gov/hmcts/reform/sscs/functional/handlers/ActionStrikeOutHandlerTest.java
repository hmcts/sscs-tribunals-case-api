package uk.gov.hmcts.reform.sscs.functional.handlers;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class ActionStrikeOutHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallback_shouldSetDwpStateFieldToStrikeOutActioned()
        throws Exception {
        String decisionType = "strikeOut";
        String expectedDwpState = "strikeOutActioned";
        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("handlers/actionStrikeOutCallback.json");
        jsonCallbackForTest = jsonCallbackForTest.replace("DECISION_TYPE_PLACEHOLDER", decisionType);
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(jsonCallbackForTest)
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .assertThat().body("data.dwpState", equalTo(expectedDwpState));
    }

    @Test
    public void givenAboutToSubmitCallback_shouldSetDwpStateFieldToStruckOut()
            throws Exception {
        String decisionType = "";
        String expectedDwpState = "struckOut";
        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("handlers/actionStrikeOutCallback.json");
        jsonCallbackForTest = jsonCallbackForTest.replace("DECISION_TYPE_PLACEHOLDER", decisionType);
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body("data.dwpState", equalTo(expectedDwpState));
    }

}
