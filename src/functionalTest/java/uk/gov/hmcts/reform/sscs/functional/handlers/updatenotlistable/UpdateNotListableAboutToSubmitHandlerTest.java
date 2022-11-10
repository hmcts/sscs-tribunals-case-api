package uk.gov.hmcts.reform.sscs.functional.handlers.updatenotlistable;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class UpdateNotListableAboutToSubmitHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/updatenotlistable/updateNotListableCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .rootPath("data")
            .assertThat().body("directionDueDate", nullValue())
            .assertThat().body("updateNotListableDirectionsFulfilled", nullValue())
            .assertThat().body("updateNotListableInterlocReview", nullValue())
            .assertThat().body("updateNotListableWhoReviewsCase", nullValue())
            .assertThat().body("interlocReferralDate", equalTo(LocalDate.now().toString()))
            .assertThat().body("interlocReviewState", equalTo("reviewByJudge"));
    }
}
