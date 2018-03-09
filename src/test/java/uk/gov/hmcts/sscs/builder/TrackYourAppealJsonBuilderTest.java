package uk.gov.hmcts.sscs.builder;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.sscs.util.SerializeJsonMessageManager.APPEAL_RECEIVED;
import static uk.gov.hmcts.sscs.util.SerializeJsonMessageManager.APPEAL_RECEIVED_CCD;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;

import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;

public class TrackYourAppealJsonBuilderTest {

    @Test
    public void appealReceivedJsonTest() {
        CaseData caseData = APPEAL_RECEIVED_CCD.getDeserializeMessage();
        ObjectNode objectNode = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), objectNode);
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

