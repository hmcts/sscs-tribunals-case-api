package uk.gov.hmcts.reform.sscs.functional.handlers.uploaddocument;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import java.time.LocalDate;
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
            .assertThat().body("data", not(hasKey("draftSscsFurtherEvidenceDocument")))
            .assertThat().body("data.sscsDocument", hasSize(2))
            .assertThat().body("data.scannedDocuments", hasSize(2))
            .assertThat().body("data.scannedDocuments[0].value.type", is("other"))
            .assertThat().body("data.scannedDocuments[0].value.fileName", is("appellant-some-name.pdf"))
            .assertThat().body("data.scannedDocuments[0].value.scannedDate", is(LocalDate.now().atStartOfDay().toString()))
            .assertThat().body("data.scannedDocuments[1].value.type", is("other"))
            .assertThat().body("data.scannedDocuments[1].value.fileName", is("reps-some-name.pdf"))
            .assertThat().body("data.scannedDocuments[1].value.scannedDate", is(LocalDate.now().atStartOfDay().toString()));
    }

}

