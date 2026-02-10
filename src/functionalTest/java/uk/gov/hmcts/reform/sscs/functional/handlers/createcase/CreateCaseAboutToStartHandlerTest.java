package uk.gov.hmcts.reform.sscs.functional.handlers.createcase;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class CreateCaseAboutToStartHandlerTest extends BaseHandler {
    @Test
    public void givenAboutToStartCallback_shouldCreateAppeal() throws Exception {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest(
                "validAppealCreatedCallback.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToStart")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat()
                .body("data.appeal.benefitType.code", equalTo("PIP"))
                .body("data.appeal.benefitType.description", equalTo("Personal Independence Payment"))
                .body("data.appeal.appellant.name.lastName", equalTo("Test"))
                .body("data.appeal.appellant().address.line1", equalTo("2 Drake Close"));
    }
}
