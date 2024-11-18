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
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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
        Adjournment adjournment = result.getAdjournment();

        assertThat(adjournment.getAdditionalDirections())
            .hasSize(1)
            .extracting(CollectionItem::getValue)
            .containsOnly("Nothing else");
        assertThat(adjournment.getCanCaseBeListedRightAway()).isEqualTo(YES);
        assertThat(adjournment.getPanelMember1().getIdamId()).isEqualTo("123");
        assertThat(adjournment.getGenerateNotice()).isEqualTo(YES);
        assertThat(adjournment.getInterpreterLanguage().getValue().getCode()).isEqualTo("spanish");
        assertThat(adjournment.getInterpreterRequired()).isEqualTo(YES);
        assertThat(adjournment.getPanelMember2().getIdamId()).isEqualTo("1234");
        assertThat(adjournment.getNextHearingDateType()).isEqualTo(AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED);
        assertThat(adjournment.getNextHearingListingDuration()).isEqualTo(720);
        assertThat(adjournment.getNextHearingListingDurationType()).isEqualTo(AdjournCaseNextHearingDurationType.STANDARD);
        assertThat(adjournment.getNextHearingListingDurationUnits()).isEqualTo(AdjournCaseNextHearingDurationUnits.MINUTES);
        assertThat(adjournment.getTime().getAdjournCaseNextHearingSpecificTime()).isEqualTo("am");
        assertThat(adjournment.getNextHearingVenue()).isEqualTo(AdjournCaseNextHearingVenue.SOMEWHERE_ELSE);
        assertThat(adjournment.getNextHearingVenueSelected().getValue().getCode()).isEqualTo("1256");
        assertThat(adjournment.getPanelMember3().getIdamId()).isEqualTo("12345");
        assertThat(adjournment.getPanelMembersExcluded()).isEqualTo(AdjournCasePanelMembersExcluded.YES);
        assertThat(adjournment.getTypeOfHearing()).isEqualTo(AdjournCaseTypeOfHearing.FACE_TO_FACE);
        assertThat(adjournment.getTypeOfNextHearing()).isEqualTo(AdjournCaseTypeOfHearing.FACE_TO_FACE);
        assertThat(adjournment.getReasons())
            .hasSize(1)
            .extracting(CollectionItem::getValue)
            .containsOnly("Testing reason");
        assertThat(adjournment.getPreviewDocument().getDocumentUrl()).isNotNull();
        assertThat(adjournment.getAdjournmentInProgress()).isEqualTo(YES);
    }
}
