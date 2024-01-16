package uk.gov.hmcts.reform.sscs.functional.handlers.dwplapse;

import static io.restassured.RestAssured.*;
import static org.junit.Assert.assertEquals;

import io.restassured.http.ContentType;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class DwpLapseCaseHandlerTest {
    @Value("${test-url}")
    private String testUrl;
    private IdamTokens idamTokens;
    @Autowired
    private IdamService idamService;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        idamTokens = idamService.getIdamTokens();
    }

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
