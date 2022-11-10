package uk.gov.hmcts.reform.sscs.functional.handlers.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
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

        assertThat(result.getAdjournment().getAdditionalDirections())
            .hasSize(1)
            .extracting(CollectionItem::getValue)
            .containsOnly("Nothing else");
        assertThat(result.getAdjournment().getCanCaseBeListedRightAway()).isEqualTo(YES);
        assertThat(result.getAdjournment().getDisabilityQualifiedPanelMemberName()).isEqualTo("Bob Smith");
        assertThat(result.getAdjournment().getGenerateNotice()).isEqualTo(YES);
        assertThat(result.getAdjournment().getInterpreterLanguage().getValue().getCode()).isEqualTo("spanish");
        assertThat(result.getAdjournment().getInterpreterRequired()).isEqualTo(YES);
        assertThat(result.getAdjournment().getMedicallyQualifiedPanelMemberName()).isEqualTo("Wendy Rowe");
        assertThat(result.getAdjournment().getNextHearingDateType()).isEqualTo(AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED);
        assertThat(result.getAdjournment().getNextHearingListingDuration()).isEqualTo(12);
        assertThat(result.getAdjournment().getNextHearingListingDurationType()).isEqualTo(AdjournCaseNextHearingDurationType.STANDARD);
        assertThat(result.getAdjournment().getNextHearingListingDurationUnits()).isEqualTo(AdjournCaseNextHearingDurationUnits.MINUTES);
        assertThat(result.getAdjournment().getTime().getAdjournCaseNextHearingSpecificTime()).isEqualTo("am");
        assertThat(result.getAdjournment().getNextHearingVenue()).isEqualTo(AdjournCaseNextHearingVenue.SOMEWHERE_ELSE);
        assertThat(result.getAdjournment().getNextHearingVenueSelected().getValue().getCode()).isEqualTo("1256");
        assertThat(result.getAdjournment().getOtherPanelMemberName()).isEqualTo("The Doctor");
        assertThat(result.getAdjournment().getPanelMembersExcluded()).isEqualTo(AdjournCasePanelMembersExcluded.YES);
        assertThat(result.getAdjournment().getTypeOfHearing()).isEqualTo(AdjournCaseTypeOfHearing.FACE_TO_FACE);
        assertThat(result.getAdjournment().getTypeOfNextHearing()).isEqualTo(AdjournCaseTypeOfHearing.FACE_TO_FACE);
        assertThat(result.getAdjournment().getReasons())
            .hasSize(1)
            .extracting(CollectionItem::getValue)
            .containsOnly("Testing reason");
        assertThat(result.getAdjournment().getPreviewDocument().getDocumentUrl()).isNotNull();
        assertThat(result.getAppeal().getHearingOptions().getLanguages()).isEqualTo("spanish");
        assertThat(result.getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo("Yes");
    }
}
