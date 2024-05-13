package uk.gov.hmcts.reform.sscs.functional.handlers.writefinaldecision;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

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
public class WriteFinalDecisionAboutToSubmitHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/writefinaldecision/writeFinalDecisionCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .rootPath("data")
            .assertThat().body("pipWriteFinalDecisionComparedToDWPMobilityQuestion", equalTo("same"))
            .assertThat().body("pipWriteFinalDecisionDailyLivingActivitiesQuestion[0]", equalTo("preparingFood"))
            .assertThat().body("pipWriteFinalDecisionDailyLivingActivitiesQuestion[1]", equalTo("takingNutrition"))
            .assertThat().body("pipWriteFinalDecisionDailyLivingQuestion", equalTo("standardRate"))
            .assertThat().body("pipWriteFinalDecisionMobilityActivitiesQuestion[0]", equalTo("planningAndFollowing"))
            .assertThat().body("pipWriteFinalDecisionMobilityQuestion", equalTo("standardRate"))
            .assertThat().body("pipWriteFinalDecisionPlanningAndFollowingQuestion", equalTo("planningAndFollowing11d"))
            .assertThat().body("pipWriteFinalDecisionPreparingFoodQuestion", equalTo("preparingFood1f"))
            .assertThat().body("pipWriteFinalDecisionTakingNutritionQuestion", equalTo("takingNutrition2b"))
            .assertThat().body("writeFinalDecisionDateOfDecision", equalTo("2020-06-01"))
            .assertThat().body("writeFinalDecisionDisabilityQualifiedPanelMemberName", equalTo("Fred"))
            .assertThat().body("writeFinalDecisionEndDate", equalTo("2020-10-10"))
            .assertThat().body("writeFinalDecisionEndDateType", equalTo("setEndDate"))
            .assertThat().body("writeFinalDecisionMedicallyQualifiedPanelMemberName", equalTo("Ted"))
            .assertThat().body("writeFinalDecisionPageSectionReference", equalTo("B2"))
            .assertThat().body("writeFinalDecisionAppellantAttendedQuestion", equalTo("Yes"))
            .assertThat().body("writeFinalDecisionAppointeeAttendedQuestion", equalTo("Yes"))
            .assertThat().body("writeFinalDecisionPresentingOfficerAttendedQuestion", equalTo("Yes"))
            .assertThat().body("writeFinalDecisionReasons[0].value", equalTo("Because appellant has trouble walking"))
            .assertThat().body("writeFinalDecisionDetailsOfDecision", equalTo("The details of the decision."))
            .assertThat().body("writeFinalDecisionAnythingElse", equalTo("Something else."))
            .assertThat().body("writeFinalDecisionStartDate", equalTo("2019-10-10"))
            .assertThat().body("writeFinalDecisionTypeOfHearing", equalTo("telephone"))
            .assertThat().body("writeFinalDecisionPreviewDocument.document_url", notNullValue());
    }
}
