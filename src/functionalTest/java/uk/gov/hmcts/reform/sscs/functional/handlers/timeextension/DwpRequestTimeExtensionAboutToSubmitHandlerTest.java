package uk.gov.hmcts.reform.sscs.functional.handlers.timeextension;

import static org.hamcrest.Matchers.*;

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
public class DwpRequestTimeExtensionAboutToSubmitHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFieldsAndTl1Form() throws Exception {
        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/timeextension/dwpRequestTimeExtensionAboutToSubmitCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .rootPath("data")
            .assertThat().body("interlocReviewState", equalTo("reviewByTcw"))
            .assertThat().body("dwpState", equalTo("extensionRequested"))
            .assertThat().body("interlocReferralReason", equalTo("timeExtension"))
            .assertThat().body("dwpDocuments", notNullValue())
            .assertThat().body("tl1Form", nullValue());
    }

}
