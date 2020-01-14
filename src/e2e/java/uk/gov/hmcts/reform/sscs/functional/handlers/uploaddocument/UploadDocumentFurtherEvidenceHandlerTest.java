package uk.gov.hmcts.reform.sscs.functional.handlers.uploaddocument;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
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
public class UploadDocumentFurtherEvidenceHandlerTest extends BaseHandler {

    @Test
    public void givenUploadDocumentEventIsTriggered_shouldUploadDocument() throws IOException {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(BaseHandler.getJsonCallbackForTest("handlers/uploaddocument/uploadDocumentFECallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.dwpState", equalTo("feReceived"));
    }

}

