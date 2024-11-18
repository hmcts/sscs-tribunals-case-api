package uk.gov.hmcts.reform.sscs.functional.handlers.uploadhearingrecording;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;

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
public class UploadHearingRecordingAboutToStartHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/uploadhearingrecording/hearingsAttendedCallback.json"))
            .post("/ccdAboutToStart")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .rootPath("data")
            .assertThat().body("selectHearingDetails.list_items[0].code", equalTo("33445566"))
            .assertThat()
            .body("selectHearingDetails.list_items[0].label", equalTo("Prudential House 23:00:00 06 Jun 2021"));
    }

    @Test
    public void givenCancelledHearingInCallback_ShouldFilterFromList() throws Exception {

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(getJsonCallbackForTest("handlers/uploadhearingrecording/hearingCancelledCallback.json"))
                .post("/ccdAboutToStart")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .rootPath("data")
                .assertThat().body("hearings.size()", equalTo(3))
                .assertThat().body("selectHearingDetails.list_items.size()", equalTo(2))
                .assertThat().body("selectHearingDetails.list_items", hasItem(hasEntry("label", "Chester Magistrate's Court 23:00:00 17 Jul 2021")))
                .assertThat().body("selectHearingDetails.list_items", hasItem(hasEntry("label", "Prudential House 23:00:00 06 Jun 2021")));
    }
}
