package uk.gov.hmcts.reform.sscs.functional.handlers.addhearingoutcome;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;




@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class AddHearingOutcomeAboutToStartTest extends BaseHandler {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    //based on aat case 1708512409621510
    @Test
    public void givenAboutToStartCallback_shouldPopulateDropdown() throws IOException {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest(
                "handlers/addhearingoutcome/AddHearingOutcomeRequestAboutToStart.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToStart")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.HearingOutcomeValue.completedHearings.list_items.size()", greaterThanOrEqualTo(1));

    }

}
