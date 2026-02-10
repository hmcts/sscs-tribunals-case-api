package uk.gov.hmcts.reform.sscs.functional.handlers.createcase;

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
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class CreateCaseAboutToStartHandlerTest extends BaseHandler {
    @Test
    public void givenAboutToStartCallback_shouldCreateAppeal() throws Exception {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest(
                "handlers/validappeal/validAppealCreatedCallback.json");

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
                .body("data.appeal.appellant.name.title", equalTo("Mr"))
                .body("data.appeal.appellant.name.firstName", equalTo("Joe"))
                .body("data.appeal.appellant.name.lastName", equalTo("Bloggs"))
                .body("data.appeal.appellant.address.line1", equalTo("123 Hairy Lane"))
                .body("data.appeal.appellant.address.line2", equalTo("Off Hairy Park"))
                .body("data.appeal.appellant.address.county", equalTo("Kent"))
                .body("data.appeal.appellant.address.postcode", equalTo("TN32 6PL"));
    }
}
