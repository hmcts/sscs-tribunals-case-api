package uk.gov.hmcts.reform.sscs.functional.handlers.addhearingoutcome;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;


@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.procperties")
@SpringBootTest
public class AddHearingOutcomeAboutToStartTest extends BaseHandler {


    @Value("${idam.oauth2.user.email}")
    private String idamSystemUpdateUser;

    @Value("${idam.oauth2.user.password}")
    private String idamSystemUpdatePassword;

    private final IdamClient idamClient;

    public AddHearingOutcomeAboutToStartTest(IdamClient idamClient) {
        this.idamClient = idamClient;
    }


    //based on aat case 1708512409621510
    @Test
    public void givenAboutToStartCallback_shouldPopulateDropdown() throws IOException {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest(
                "handlers/addhearingoutcome/AddHearingOutcomeRequestAboutToStart.json");

        String userAuthToken = idamClient.getAccessToken(idamSystemUpdateUser,  idamSystemUpdatePassword);


        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", userAuthToken))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToStart")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.HearingOutcomeValue.completedHearings.list_items.size()", greaterThanOrEqualTo(1));

    }

}
