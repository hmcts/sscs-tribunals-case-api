package uk.gov.hmcts.sscs.builder;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.sscs.util.SerializeJsonMessageManager.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;

import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;

public class TrackYourAppealJsonBuilderTest {

    @Test
    public void appealReceivedTest() {
        CaseData caseData = APPEAL_RECEIVED_CCD.getDeserializeMessage();
        ObjectNode objectNode = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dwpRespondTest() {
        CaseData caseData = DWP_RESPOND_CCD.getDeserializeMessage();
        ObjectNode objectNode = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(DWP_RESPOND.getSerializedMessage(), objectNode);
    }

    @Test
    public void hearingBookedTest() {
        CaseData caseData = HEARING_BOOKED_CCD.getDeserializeMessage();
        ObjectNode objectNode = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(HEARING_BOOKED.getSerializedMessage(), objectNode);
    }

    @Test
    public void adjournedTest() {
        CaseData caseData = ADJOURNED_CCD.getDeserializeMessage();
        ObjectNode objectNode = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(ADJOURNED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dormantTest() {
        CaseData caseData = DORMANT_CCD.getDeserializeMessage();
        ObjectNode objectNode = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(DORMANT.getSerializedMessage(), objectNode);
    }

    private RegionalProcessingCenter populateRegionalProcessingCenter() {
        RegionalProcessingCenter regionalProcessingCenter = new RegionalProcessingCenter();
        regionalProcessingCenter.setName("LIVERPOOL");
        regionalProcessingCenter.setAddress1("HM Courts & Tribunals Service");
        regionalProcessingCenter.setAddress2("Social Security & Child Support Appeals");
        regionalProcessingCenter.setAddress3("Prudential Buildings");
        regionalProcessingCenter.setAddress4("36 Dale Street");
        regionalProcessingCenter.setCity("LIVERPOOL");
        regionalProcessingCenter.setPostcode("L2 5UZ");
        regionalProcessingCenter.setPhoneNumber("0300 123 1142");
        regionalProcessingCenter.setFaxNumber("0870 324 0109");
        return regionalProcessingCenter;
    }
}

