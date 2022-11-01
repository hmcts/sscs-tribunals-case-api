package uk.gov.hmcts.reform.sscs.functional.handlers.adjourncase;

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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class AdjournCaseAboutToSubmitHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/adjourncase/adjournCaseCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .rootPath("data")
            .assertThat().body("adjournCaseAdditionalDirections[0].value", equalTo("Nothing else"))
            .assertThat().body("adjournCaseCanCaseBeListedRightAway", equalTo("Yes"))
            .assertThat().body("adjournCaseDisabilityQualifiedPanelMemberName", equalTo("Bob Smith"))
            .assertThat().body("adjournCaseGenerateNotice", equalTo("Yes"))
            .assertThat().body("adjournCaseInterpreterLanguageList.value.code", equalTo("spanish"))
            .assertThat().body("adjournCaseInterpreterRequired", equalTo("Yes"))
            .assertThat().body("adjournCaseMedicallyQualifiedPanelMemberName", equalTo("Wendy Rowe"))
            .assertThat().body("adjournCaseNextHearingDateType", equalTo("dateToBeFixed"))
            .assertThat().body("adjournCaseNextHearingListingDuration", equalTo("12"))
            .assertThat().body("adjournCaseNextHearingListingDurationType", equalTo("setTime"))
            .assertThat().body("adjournCaseNextHearingListingDurationUnits", equalTo("hours"))
            .assertThat().body("adjournCaseTime.adjournCaseNextHearingSpecificTime", equalTo("am"))
            .assertThat().body("adjournCaseNextHearingVenue", equalTo("somewhereElse"))
            .assertThat().body("adjournCaseNextHearingVenueSelected.value.code", equalTo("1256"))
            .assertThat().body("adjournCaseOtherPanelMemberName", equalTo("The Doctor"))
            .assertThat().body("adjournCasePanelMembersExcluded", equalTo("Yes"))
            .assertThat().body("adjournCaseTypeOfHearing", equalTo("faceToFace"))
            .assertThat().body("adjournCaseTypeOfNextHearing", equalTo("faceToFace"))
            .assertThat().body("adjournCaseReasons[0].value", equalTo("Testing reason"))
            .assertThat().body("adjournCasePreviewDocument.document_url", notNullValue());
    }
}
