package uk.gov.hmcts.reform.sscs.functional.handlers.postpone;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.POSTPONE_HEARING_CALLBACK_CCD;

import io.restassured.http.ContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class PostponeHearingHandlerTest extends BaseHandler {

    @Test
    public void hitCallback() {

        String body = POSTPONE_HEARING_CALLBACK_CCD.getSerializedMessage();

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
                .extract().body().jsonPath().get("data.dwpState");

        assertEquals("hearingPostponed", response);
    }
}
