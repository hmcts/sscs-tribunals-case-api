package uk.gov.hmcts.reform.sscs.functional.handlers.interloc;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

public class SendToAdminTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForSendToAdmin_shouldSetInterlocReviewStateField() throws Exception {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(BaseHandler.getJsonCallbackForTest("handlers/interloc/sendToAdminCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .assertThat().body("data.interlocReviewState", equalTo("awaitingAdminAction"));
    }


}

