package uk.gov.hmcts.reform.sscs.functional.handlers.dwpuploadresponse;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class DwpUploadResponseAboutToStartHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToStartCallback_shouldReturnDwpFurtherInfo() throws Exception {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("callback/dwpUploadResponse.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToStart")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat()
                .body("data.dwpFurtherInfo", equalTo("Yes"))
                .body("data.appeal.benefitType.code", equalTo("BENEFIT_CODE_PLACEHOLDER"))
                .body("data.dwpResponseDocument.documentLink.document_url", equalTo("http://dm-store:5005/documents/6a7cc2b4-2a32-468d-a3e9-d394b0503a26"))
                .body("data.dwpEvidenceBundleDocument.documentLink.document_url", equalTo("http://dm-store:5005/documents/6a7cc2b4-2a32-468d-a3e9-d394b0503a23"))
                .body("data.dynamicDwpState.value.code", equalTo("withdrawalReceived"))
                .body("data.sscsDocument[0].value.documentType", equalTo("appellantEvidence"));
    }
}
