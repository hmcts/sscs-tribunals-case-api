package uk.gov.hmcts.reform.sscs.functional.handlers.dwplapse;

import static io.restassured.RestAssured.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.http.ContentType;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class DwpLapseCaseHandlerTest extends BaseHandler {

    @Test
    public void hitCallback() throws IOException {

        String body = BaseHandler.getJsonCallbackForTest("handlers/dwplapse/dwpLapseCallback.json");

        String response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", idamTokens.getIdamOauth2Token())
                .header("ServiceAuthorization", idamTokens.getServiceAuthorization())
                .body(body)
                .expect()
                .statusCode(200)
                .when()
                .post("/ccdAboutToSubmit/")
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().jsonPath().get("data.interlocReviewState");

        assertEquals("awaitingAdminAction", response);
    }
}
