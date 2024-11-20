package uk.gov.hmcts.reform.sscs.functional.handlers.updatewelshpreference;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import junitparams.JUnitParamsRunner;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class UpdateWelshPreferenceAboutToSubmitHandlerFunctionalTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackAndIterlocReviewStateIsWelshTranslation_ShouldAddNote() throws Exception {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest(
                "handlers/updatewelshpreference/updateWelshPreferenceAboutToSubmitCallback.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.interlocReviewState", equalTo("awaitingAdminAction"))
                .assertThat().body("data.appealNotePad.notesCollection[0].value.noteDetail", equalTo("Assigned to admin - Case no longer Welsh. Please cancel any Welsh translations"));
    }
}
