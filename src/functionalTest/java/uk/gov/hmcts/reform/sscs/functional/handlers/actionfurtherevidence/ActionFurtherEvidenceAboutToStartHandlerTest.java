package uk.gov.hmcts.reform.sscs.functional.handlers.actionfurtherevidence;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import junitparams.JUnitParamsRunner;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class ActionFurtherEvidenceAboutToStartHandlerTest extends BaseHandler {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Test
    public void givenAboutToStartCallback_shouldSetItemsInFurtherActionDropdownMenu() throws Exception {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest(
                "handlers/actionfurtherevidence/actionFurtherEvidenceAboutToStartCallback.json");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToStart")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.furtherEvidenceAction.value.code", equalTo(""))
                .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "issueFurtherEvidence")))
                .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "otherDocumentManual")))
                .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "informationReceivedForInterlocJudge")))
                .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "informationReceivedForInterlocTcw")))
                .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "sendToInterlocReviewByJudge")))
                .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "sendToInterlocReviewByTcw")))
                .assertThat().body("data.furtherEvidenceAction.list_items.size()", greaterThanOrEqualTo(6));

    }
}
