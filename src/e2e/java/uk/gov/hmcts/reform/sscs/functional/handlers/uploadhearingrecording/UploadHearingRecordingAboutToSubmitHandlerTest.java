package uk.gov.hmcts.reform.sscs.functional.handlers.uploadhearingrecording;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

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
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class UploadHearingRecordingAboutToSubmitHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/uploadhearingrecording/hearingRecordingsCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .rootPath("data")

            .assertThat().body("sscsHearingRecordings[0].value.hearingType", equalTo("adjourned"))
            .assertThat().body("sscsHearingRecordings[0].value.hearingDate", equalTo("06-06-2021 10:00:20 PM"))
            .assertThat().body("sscsHearingRecordings[0].value", hasKey("uploadDate"))

            .assertThat().body("sscsHearingRecordings[1].value.hearingType", equalTo("adjourned"))
            .assertThat().body("sscsHearingRecordings[1].value.hearingDate", equalTo("06-06-2021 11:00:00 PM"))
            .assertThat().body("sscsHearingRecordings[1].value", hasKey("uploadDate"))

            .assertThat().body("sscsHearingRecordings[1].value.recordings[0].value.document_filename",
            equalTo("Adjourned Prudential House 06 Jun 2021.MP4"))
            .assertThat().body("sscsHearingRecordings[1].value.recordings[0].value", hasKey(("document_url")))
            .assertThat().body("sscsHearingRecordings[1].value.recordings[0].value", hasKey(("document_binary_url")))

            .assertThat().body("sscsHearingRecordings[1].value.recordings[1].value.document_filename",
            equalTo("Adjourned Prudential House 06 Jun 2021 (2).mp4"))
            .assertThat().body("sscsHearingRecordings[1].value.recordings[1].value", hasKey(("document_url")))
            .assertThat().body("sscsHearingRecordings[1].value.recordings[1].value", hasKey(("document_binary_url")))

            .assertThat().body("sscsHearingRecordings[1].value.recordings[2].value.document_filename",
            equalTo("Adjourned Prudential House 06 Jun 2021 (3).mp3"))
            .assertThat().body("sscsHearingRecordings[1].value.recordings[2].value", hasKey(("document_url")))
            .assertThat().body("sscsHearingRecordings[1].value.recordings[2].value", hasKey(("document_binary_url")));

    }
}
