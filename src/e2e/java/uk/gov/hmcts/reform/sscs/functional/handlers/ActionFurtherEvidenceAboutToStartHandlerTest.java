package uk.gov.hmcts.reform.sscs.functional.handlers;

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

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class ActionFurtherEvidenceAboutToStartHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToStartCallback_shouldSetItemsInFurtherActionDropdownMenu() throws Exception {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(BaseHandler.getJsonCallbackForTest("actionFurtherEvidenceAboutToStartCallback.json"))
            .post("/ccdAboutToStart")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.furtherEvidenceAction.value.code", equalTo("otherDocumentManual"))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "otherDocumentManual")))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "informationReceivedForInterlocJudge")))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "informationReceivedForInterlocTcw")))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "sendToInterlocReviewByJudge")))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "sendToInterlocReviewByTcw")));

    }
}
