package uk.gov.hmcts.reform.sscs.functional.handlers.requesthearingrecording;

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
public class RequestHearingRecordingAboutToSubmitHandlerFunctionalTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/requesthearingrecording/requestHearingRecordingAboutToSubmitCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .rootPath("data")
            .assertThat().body("requestableHearingDetails.list_items[0].code", equalTo("44445566"))
            .assertThat()
            .body("requestableHearingDetails.list_items[0].label", equalTo("Prudential House 12:00 04 Apr 2021"))
            .assertThat().body("requestableHearingDetails.list_items[1].code", equalTo("33445566"))
            .assertThat()
            .body("requestableHearingDetails.list_items[1].label", equalTo("Prudential House 23:00 06 Jun 2021"))
            .assertThat()
            .body("requestedHearings[0].value.sscsHearingRecording.hearingId", equalTo("11445566"))
            .assertThat()
            .body("requestedHearings[1].value.sscsHearingRecording.hearingId", equalTo("33445566"))
            .assertThat()
            .body("hearingRecordingRequestOutstanding", equalTo("Yes"));



    }
}
