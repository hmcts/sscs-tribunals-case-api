package uk.gov.hmcts.reform.sscs.functional.handlers.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class AdjournCaseAboutToSubmitHandlerTest extends BaseHandler {

    @Autowired
    private ObjectMapper mapper;

    @DisplayName("Given about to submit callback for Gaps event, should set fields")
    @Test
    public void testGaps() throws IOException {
        String response = RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/adjourncase/adjournCaseGapsCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true).extract().body().asString();

        JsonNode root = mapper.readTree(response);
        SscsCaseData result = mapper.readValue(root.path("data").toPrettyString(), new TypeReference<>(){});

        assertThat(result.getAdjournCaseAdditionalDirections())
            .hasSize(1)
            .extracting(CollectionItem::getValue)
            .containsOnly("Nothing else");
        assertThat(result.getAdjournCaseCanCaseBeListedRightAway()).isEqualTo("Yes");
        assertThat(result.getAdjournCaseDisabilityQualifiedPanelMemberName()).isEqualTo("Bob Smith");
        assertThat(result.getAdjournCaseGenerateNotice()).isEqualTo("Yes");
        assertThat(result.getAdjournCaseInterpreterLanguage()).isEqualTo("spanish");
        assertThat(result.getAdjournCaseInterpreterRequired()).isEqualTo("Yes");
        assertThat(result.getAdjournCaseMedicallyQualifiedPanelMemberName()).isEqualTo("Wendy Rowe");
        assertThat(result.getAdjournCaseNextHearingDateType()).isEqualTo("dateToBeFixed");
        assertThat(result.getAdjournCaseNextHearingListingDuration()).isEqualTo("12");
        assertThat(result.getAdjournCaseNextHearingListingDurationType()).isEqualTo("setTime");
        assertThat(result.getAdjournCaseNextHearingListingDurationUnits()).isEqualTo("hours");
        assertThat(result.getAdjournCaseTime().getAdjournCaseNextHearingSpecificTime()).isEqualTo("am");
        assertThat(result.getAdjournCaseNextHearingVenue()).isEqualTo("somewhereElse");
        assertThat(result.getAdjournCaseNextHearingVenueSelected().getValue().getCode()).isEqualTo("1256");
        assertThat(result.getAdjournCaseOtherPanelMemberName()).isEqualTo("The Doctor");
        assertThat(result.getAdjournCasePanelMembersExcluded()).isEqualTo("Yes");
        assertThat(result.getAdjournCaseTypeOfHearing()).isEqualTo("faceToFace");
        assertThat(result.getAdjournCaseTypeOfNextHearing()).isEqualTo("faceToFace");
        assertThat(result.getAdjournCaseReasons())
            .hasSize(1)
            .extracting(CollectionItem::getValue)
            .containsOnly("Testing reason");
        assertThat(result.getAdjournCasePreviewDocument().getDocumentUrl()).isNotNull();
        assertThat(result.getAppeal().getHearingOptions().getLanguages()).isEqualTo("spanish");
        assertThat(result.getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo("Yes");
    }
}
