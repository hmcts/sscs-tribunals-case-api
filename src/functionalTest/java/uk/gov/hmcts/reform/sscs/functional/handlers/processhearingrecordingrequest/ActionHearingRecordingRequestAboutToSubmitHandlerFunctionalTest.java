package uk.gov.hmcts.reform.sscs.functional.handlers.processhearingrecordingrequest;

import static org.hamcrest.Matchers.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;


@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class ActionHearingRecordingRequestAboutToSubmitHandlerFunctionalTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/actionhearingrecordingrequest/actionHearingRecordingRequestAboutToSubmitCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .rootPath("data")
            .assertThat()
            .body("requestedHearings", is(empty()))
            .assertThat()
            .body("dwpReleasedHearings[0].value.sscsHearingRecording.hearingId", equalTo("12345566767"))
            .assertThat()
            .body("hearingRecordingRequestOutstanding", equalTo("No"));



    }
}
