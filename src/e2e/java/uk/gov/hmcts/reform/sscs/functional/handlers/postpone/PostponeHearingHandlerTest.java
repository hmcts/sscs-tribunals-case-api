package uk.gov.hmcts.reform.sscs.functional.handlers.postpone;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;




@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
public class PostponeHearingHandlerTest {

    @Value("${test-url}")
    private String testUrl;

    @Test
    public void hitCallback() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();

        String body = POSTPONE_HEARING_CALLBACK_CCD.getSerializedMessage();

        String response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .expect()
                .statusCode(201)
                .when()
                .post("/ccdAboutToSubmit/")
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().jsonPath().get(".data.appeal.dwpState");
        assertEquals("hearingPostponed", response);
    }
}
