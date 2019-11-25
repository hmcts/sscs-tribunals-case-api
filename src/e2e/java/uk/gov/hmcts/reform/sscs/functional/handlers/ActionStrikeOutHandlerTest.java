package uk.gov.hmcts.reform.sscs.functional.handlers;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class ActionStrikeOutHandlerTest extends BaseHandler {

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Test
    @Parameters({
        "strikeOut, strikeOutActioned",
        ", struckOut"
    })
    public void givenAboutToSubmitCallback_shouldSetDwpStateField(String decisionType, String expectedDwpState)
        throws Exception {
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
