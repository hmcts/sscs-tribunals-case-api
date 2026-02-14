package uk.gov.hmcts.reform.sscs.functional.handlers.createcase;

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
public class CreateCaseAboutToSubmitHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {
        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("handlers/validappeal/validAppealCreatedCallback.json");

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat()
                .body("data.poAttendanceConfirmed", equalTo("No"))
                .body("data.tribunalDirectPoToAttend", equalTo("No"))
                .body("data.dwpIsOfficerAttending", equalTo("No"))
                .body("data.sscsDocument.size()", equalTo(1))
                .body("data.sscsDocument[0].value.documentType", equalTo("sscs1"))
                .body("data.issueCode", equalTo("DD"))
                .body("data.caseCode", equalTo("002DD"))
                .body("data.isScottishCase", equalTo("No"));
    }


    @Test
    public void givenAboutToSubmitCallbackForEventonIbcaCase_shouldSetFields() throws Exception {
        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("handlers/validappeal/validAppealCreatedIbcaCallback.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat()
                .body("data.poAttendanceConfirmed", equalTo("No"))
                .body("data.tribunalDirectPoToAttend", equalTo("No"))
                .body("data.dwpIsOfficerAttending", equalTo("No"))
                .body("data.appeal.mrnDetails.dwpIssuingOffice", equalTo("IBCA"))
                .body("data.regionalProcessingCenter.hearingRoute", equalTo("listAssist"))
                .body("data.sscsDocument.size()", equalTo(1))
                .body("data.sscsDocument[0].value.documentType", equalTo("sscs8"))
                .body("data.issueCode", equalTo("DD"))
                .body("data.caseCode", equalTo("093DD"))
                .body("data.isScottishCase", equalTo("No"));
    }
}


